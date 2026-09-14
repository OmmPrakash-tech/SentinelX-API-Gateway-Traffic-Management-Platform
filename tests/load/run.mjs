import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {performance} from 'node:perf_hooks';
import {execFileSync} from 'node:child_process';
import os from 'node:os';
const base=process.env.BASE_URL||'http://localhost:8080';
const counts=(process.env.LOAD_COUNTS||'100,1000,5000,10000').split(',').map(Number);
const concurrency=Number(process.env.LOAD_CONCURRENCY||20);
const unthrottled=process.argv.includes('--unthrottled');
const password=process.env.DEMO_PASSWORD||'SentinelX-Local-2026!';
async function api(path,token,method='GET',body){const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined});const data=await r.json();if(!r.ok)throw new Error(`${r.status}: ${data.message}`);return data;}
const login=await api('/api/auth/login',null,'POST',{email:unthrottled?'admin@sentinelx.local':'developer@sentinelx.local',password});
const credential=await api('/api/me/keys',login.token,'POST',{name:'Load test '+new Date().toISOString(),days:1});
let policies=[];
const applyPolicy=p=>api('/api/admin/rate-limits/'+p.id,login.token,'PUT',{scope:p.scope,capacity:p.capacity,refillPerSecond:p.refill_per_second,enabled:p.enabled});
let gatewayPid;try{gatewayPid=JSON.parse(await readFile('work/processes.json','utf8')).find(x=>x.name==='gateway').id;}catch{}
function resources(){if(!gatewayPid||process.platform!=='win32')return null;try{return JSON.parse(execFileSync('powershell.exe',['-NoProfile','-Command',`Get-Process -Id ${Number(gatewayPid)} | Select-Object CPU,WorkingSet64,PeakWorkingSet64,HandleCount | ConvertTo-Json -Compress`],{encoding:'utf8',windowsHide:true}));}catch{return null;}}
const report={timestamp:new Date().toISOString(),base,scenario:unthrottled?'Rate policies temporarily disabled; authentication, persistence and gateway enabled':'Configured rate policies enabled',node:process.version,os:os.type()+' '+os.release(),cpu:os.cpus()[0]?.model,logicalCpus:os.cpus().length,concurrency,results:[]};
try{
 if(unthrottled){policies=await api('/api/ops/rate-limits',login.token);for(const p of policies)await applyPolicy({...p,enabled:false});}
 for(const count of counts){
  const latencies=[],statuses={};let next=0;const before=resources(),start=performance.now();
  await Promise.all(Array.from({length:concurrency},async()=>{while(next++<count){const began=performance.now();try{const r=await fetch(base+'/gateway/users',{headers:{'X-API-Key':credential.secret}});await r.arrayBuffer();statuses[r.status]=(statuses[r.status]||0)+1;}catch{statuses.NETWORK_ERROR=(statuses.NETWORK_ERROR||0)+1;}latencies.push(performance.now()-began);}}));
  const duration=performance.now()-start,after=resources();latencies.sort((a,b)=>a-b);const percentile=p=>latencies[Math.max(0,Math.ceil(latencies.length*p)-1)];const errors=Object.entries(statuses).filter(([s])=>Number(s)>=400||s==='NETWORK_ERROR').reduce((n,[,v])=>n+v,0);
  const result={requests:count,durationMs:duration,requestsPerSecond:count/(duration/1000),p50Ms:percentile(.5),p95Ms:percentile(.95),p99Ms:percentile(.99),errors,errorRate:errors/count,statuses,gatewayBefore:before,gatewayAfter:after,gatewayCpuSeconds:before&&after?after.CPU-before.CPU:null};report.results.push(result);console.log(JSON.stringify(result));
 }
}finally{
 for(const p of policies)await applyPolicy(p);
 await api('/api/me/keys/'+credential.id,login.token,'DELETE');
 await api('/api/auth/logout',login.token,'POST');
 await mkdir('docs/performance',{recursive:true});const filename=`docs/performance/${report.timestamp.replace(/[:.]/g,'-')}-${unthrottled?'unthrottled':'policies-enabled'}.json`;await writeFile(filename,JSON.stringify(report,null,2));console.log('Results saved: '+filename);
}

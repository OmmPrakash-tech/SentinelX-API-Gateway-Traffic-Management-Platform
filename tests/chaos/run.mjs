import {readFile,writeFile,mkdir} from 'node:fs/promises';
import assert from 'node:assert/strict';
const base=process.env.BASE_URL||'http://localhost:8080',password=process.env.DEMO_PASSWORD||'SentinelX-Local-2026!';
const control=(await readFile('work/control-token.txt','utf8')).trim();
const results=[];
async function api(path,token,method='GET',body){const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined});const data=await r.json();if(!r.ok)throw new Error(`${r.status}: ${data.message}`);return data;}
async function fault(port,body){const r=await fetch(`http://localhost:${port}/__control`,{method:'POST',headers:{'Content-Type':'application/json','X-Control-Token':control},body:JSON.stringify(body)});assert.equal(r.status,200);return r.json();}
const login=await api('/api/auth/login',null,'POST',{email:'admin@sentinelx.local',password});const token=login.token;
const routes=await api('/api/ops/routes',token),product=routes.find(r=>r.path_prefix==='/products');
const config=r=>({pathPrefix:r.path_prefix,methods:r.methods,serviceId:r.service_id,enabled:r.enabled,timeoutMs:r.timeout_ms,retries:r.retries,backoffMs:r.backoff_ms,failureThreshold:r.failure_threshold,recoveryMs:r.recovery_ms,connectTimeoutMs:r.connect_timeout_ms||1000,readTimeoutMs:r.read_timeout_ms||2000});
async function request(path,method='GET'){return fetch(base+'/gateway'+path,{method,headers:{Authorization:'Bearer '+token,'X-Request-ID':'chaos-'+Date.now()}});}
async function check(name,fn){await fn();results.push({name,status:'PASS'});console.log('PASS '+name);}
try{
 await check('User, product and order demo services proxy',async()=>{for(const path of ['/users','/products','/orders'])assert.equal((await request(path)).status,200);});
 await check('Round robin distributes to two user instances',async()=>{const seen=new Set();for(let i=0;i<6;i++)seen.add((await (await request('/users')).json()).instance);assert.equal(seen.size,2);});
 await check('Health failure excludes instance; recovery restores it',async()=>{await fault(9101,{unhealthy:true});await api('/api/ops/check-health',token,'POST');for(let i=0;i<5;i++)assert.equal((await (await request('/users')).json()).instance,'user-9104');await fault(9101,{});await api('/api/ops/check-health',token,'POST');const instances=await api('/api/ops/instances',token);assert.equal(instances.find(i=>i.base_url.endsWith(':9101')).status,'HEALTHY');});
 await check('Transient GET failure retries successfully',async()=>{await fault(9102,{failures:1});const r=await request('/products');assert.equal(r.status,200);assert.equal(r.headers.get('X-SentinelX-Retries'),'1');});
 await check('POST does not retry',async()=>{await fault(9102,{failures:1});const r=await request('/products','POST');assert.equal(r.status,503);assert.equal(r.headers.get('X-SentinelX-Retries'),'0');});
 await api('/api/admin/routes/'+product.id,token,'PUT',{...config(product),failureThreshold:2,recoveryMs:500,retries:0,timeoutMs:300,readTimeoutMs:200});
 await check('Timeout returns 504',async()=>{await fault(9102,{latencyMs:1000});assert.equal((await request('/products')).status,504);await fault(9102,{});});
 await check('Circuit opens, blocks calls, probes and recovers',async()=>{await fault(9102,{alwaysFail:true});await request('/products');await request('/products');const before=(await fault(9102,{alwaysFail:true})).attempts;assert.equal((await request('/products')).status,503);assert.equal((await fault(9102,{alwaysFail:true})).attempts,before);const circuits=await api('/api/ops/circuits',token);assert.ok(circuits.some(c=>c.id.startsWith(product.id)&&c.state==='OPEN'));await fault(9102,{});await new Promise(r=>setTimeout(r,600));assert.equal((await request('/products')).status,200);assert.ok((await api('/api/ops/circuits',token)).some(c=>c.id.startsWith(product.id)&&c.state==='CLOSED'));});
}finally{
 for(const port of [9101,9102,9103,9104])await fault(port,{});
 await api('/api/admin/routes/'+product.id,token,'PUT',config(product));await api('/api/ops/check-health',token,'POST');await api('/api/auth/logout',token,'POST');
 await mkdir('docs/performance',{recursive:true});await writeFile('docs/performance/chaos-results.json',JSON.stringify({timestamp:new Date().toISOString(),results},null,2));
}

import http from 'node:http';
import {randomUUID,timingSafeEqual} from 'node:crypto';
const service=process.env.SERVICE_NAME||process.argv[2]||'user';
const port=Number(process.env.PORT||process.argv[3]||9101);
const instance=process.env.INSTANCE_ID||`${service}-${port}`;
const resource=service+'s';
const records=new Map();
let latency=0,failures=0,alwaysFail=false,unhealthy=false,attempts=0;
const control=process.env.DEMO_CONTROL_TOKEN;
function reply(res,status,data,id){res.writeHead(status,{'Content-Type':'application/json','X-Instance-ID':instance,'X-Request-ID':id});res.end(JSON.stringify(data));}
async function body(req){const parts=[];let length=0;for await(const chunk of req){length+=chunk.length;if(length>1048576)throw new Error('Body too large');parts.push(chunk);}const raw=Buffer.concat(parts).toString();return raw?JSON.parse(raw):{};}
const server=http.createServer(async(req,res)=>{
 const id=req.headers['x-request-id']||randomUUID();const path=new URL(req.url,'http://localhost').pathname;
 try{
  if(path==='/health'){reply(res,unhealthy?503:200,{status:unhealthy?'DOWN':'UP',service,instance},id);return;}
  if(path==='/__control'){
   const supplied=req.headers['x-control-token']||'';
   if(process.env.NODE_ENV!=='development'||!control||supplied.length!==control.length||!timingSafeEqual(Buffer.from(supplied),Buffer.from(control))){reply(res,404,{error:'NOT_FOUND'},id);return;}
   if(req.method==='POST'){const b=await body(req);latency=Math.max(0,Math.min(10000,Number(b.latencyMs)||0));failures=Math.max(0,Math.min(10000,Number(b.failures)||0));alwaysFail=b.alwaysFail===true;unhealthy=b.unhealthy===true;}
   reply(res,200,{latencyMs:latency,failures,alwaysFail,unhealthy,attempts,instance},id);return;
  }
  attempts++;
  if(latency)await new Promise(resolve=>setTimeout(resolve,latency));
  if(alwaysFail||failures>0){if(failures>0)failures--;reply(res,503,{error:'CONTROLLED_FAILURE',instance},id);return;}
  if(path===`/${resource}`&&req.method==='GET'){reply(res,200,{service,instance,items:[...records.values()],requestId:id},id);return;}
  if(path===`/${resource}`&&req.method==='POST'){const data=await body(req);const item={id:randomUUID(),name:String(data.name||'Demo record').slice(0,100),createdAt:new Date().toISOString()};records.set(item.id,item);reply(res,201,{...item,instance},id);return;}
  if(path.startsWith(`/${resource}/`)&&req.method==='GET'){const item=records.get(path.slice(resource.length+2));reply(res,item?200:404,item?{...item,instance}:{error:'NOT_FOUND',instance},id);return;}
  reply(res,404,{error:'NOT_FOUND',service,instance},id);
 }catch{reply(res,400,{error:'INVALID_REQUEST'},id);}
});
server.listen(port,process.env.HOST||'127.0.0.1',()=>console.log(JSON.stringify({event:'service_started',service,instance,port})));
process.on('SIGTERM',()=>server.close(()=>process.exit(0)));

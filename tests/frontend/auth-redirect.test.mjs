import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFileSync} from 'node:fs';
// Exercise the production session transition without a browser dependency.
const source=readFileSync(new URL('../../frontend/app.js',import.meta.url),'utf8');
const transition=source.split('\n').find(line=>line.startsWith('function setSession('));
for(const role of ['ADMIN','OPERATOR','DEVELOPER','USER']){
 const target=['ADMIN','OPERATOR'].includes(role)?'#ops/overview':'#dev/overview';
 test(role+' transitions from login and renders an already-matching dashboard link',()=>{
  for(const initial of ['#login',target]){
   let renders=0;const storage=new Map();const location={hash:initial};
   const context=vm.createContext({location,sessionStorage:{setItem:(k,v)=>storage.set(k,v)},navigate:()=>{renders++;}});
   vm.runInContext('let token,me;'+transition,context);
   context.setSession({token:'test-session',user:{role,name:'Test'}});
   assert.equal(storage.get('sx-session'),'test-session');
   assert.equal(location.hash.replace(/^#/,''),target.slice(1));
   assert.equal(renders,initial===target?1:0,'An unchanged URL must explicitly render the authenticated workspace');
  }
 });
}

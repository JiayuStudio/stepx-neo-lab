import test from 'node:test';
import assert from 'node:assert/strict';
import {MatrixPlayer,validateFrame,checkReply,makeBuddy} from './matrix-player.mjs';
const delay=ms=>new Promise(r=>setTimeout(r,ms));

test('reject malformed device payloads, including fractional and boolean cells',()=>{
  for(const f of [[],Array(391).fill(256),Array(391).fill(.5),Array(391).fill(false)])
    assert.throws(()=>validateFrame(f));
  for(let t=0;t<10;t+=.13)assert.equal(validateFrame(makeBuddy(t)).length,391);
});
test('surface nested MCP errors instead of treating transport success as hardware success',()=>{
  assert.throws(()=>checkReply({result:{content:[{type:'text',text:'{"success":false}'}]}}));
  assert.throws(()=>checkReply({data:{isError:true}}));
  assert.throws(()=>checkReply(undefined));
  checkReply({content:[{type:'text',text:'{"success":true}'}]});
});
test('stop waits for the in-flight frame, clears last, and prevents later frames',async()=>{
  const calls=[];let unblock,entered;const first=new Promise(r=>entered=r);
  const mcp={connect:async()=>({success:true}),callTool:async(name)=>{
    calls.push(name+':begin');
    if(name==='set_back_screen_frame'){entered();await new Promise(r=>unblock=r);}
    calls.push(name+':end');return {success:true};
  }};
  const p=new MatrixPlayer({mcp});await p.start({device:true});await first;
  const stopping=p.stop();await delay(10);
  assert.equal(calls.some(x=>x.startsWith('clear')),false);
  unblock();await stopping;await delay(30);
  assert.deepEqual(calls,['set_back_screen_frame:begin','set_back_screen_frame:end','clear_back_screen_frame:begin','clear_back_screen_frame:end']);
});
test('stop during connection never starts a later frame',async()=>{
  let unlock,entered;const entering=new Promise(r=>entered=r),calls=[];
  const p=new MatrixPlayer({mcp:{connect:async()=>{entered();await new Promise(r=>unlock=r);},callTool:async(n)=>{calls.push(n);return {success:true};}}});
  const starting=p.start({device:true});await entering;const stopping=p.stop();unlock();
  await starting;await stopping;assert.deepEqual(calls,[]);
});
test('failed tool reply stops playback and preserves cleanup until explicit stop',async()=>{
  const calls=[];let failure;const failed=new Promise(r=>failure=r);
  const p=new MatrixPlayer({status:s=>{if(s.startsWith('播放失败'))failure();},mcp:{connect:async()=>{},callTool:async(n)=>{calls.push(n);return n==='set_back_screen_frame'?{content:[{text:'{"isError":true}'}]}:{success:true};}}});
  await p.start({device:true});await failed;await p.stop();
  assert.deepEqual(calls,['set_back_screen_frame','clear_back_screen_frame']);
});
test('preview never touches hardware or changes global settings',async()=>{
  const calls=[],frames=[];
  const p=new MatrixPlayer({mcp:{connect:async()=>calls.push('connect'),callTool:async(n)=>calls.push(n)},preview:f=>frames.push(f)});
  await p.start();await delay(20);await p.stop();assert.deepEqual(calls,[]);assert.ok(frames.length>=2);
});

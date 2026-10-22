import {albumColumns,albumPageSize,mediaGridColumns} from '../src/composables/albumLayout.ts'
import {readViewState,writeViewState,invalidateViewData,clearViewStates} from '../src/composables/viewState.ts'
import {gatherParticles} from '../src/composables/fragmentGather.ts'
import {fragmentPageWindow} from '../src/composables/fragmentPaging.ts'
import {wakeOffset,wakeProjection,wakeLifetime} from '../src/three/meteorWake.ts'
import {fragmentActivation,fragmentMeteorGeometry,fragmentMeteorPoint} from '../src/composables/fragmentLink.ts'
import {overviewMagnification,calendarMagnification,contentCameraDistance} from '../src/three/cameraFraming.ts'
import {calendarBranch} from '../src/three/starLabels.ts'
import {seaRippleOffset} from '../src/three/seaRipple.ts'
import test from 'node:test'
import assert from 'node:assert/strict'
import {memoryReceipt,capsuleReceipt,receiptMatches,resolveCreationIndex,flightPosition,creationDetail} from '../src/composables/creationReceipt.ts'
import {deliveredFragments,fragmentContains,branchContext,interactionFocus,matchesUniverseNode,universeFilter,warmStarColor} from '../src/three/branchContext.ts'
import {buildSync} from 'esbuild'
import {Color,Vector3,Group} from 'three'
import {useCreationEvents} from '../src/composables/useCreationEvents.ts'

const memory={id:7,familyId:2,title:'去年的生日',memoryDate:'2025-08-10',content:'private content'}
const receipt=memoryReceipt(memory,{x:10,y:20})
const index={indexId:50,sourceType:'MEMORY',sourceId:7,nodeDate:'2025-08-10',title:'去年的生日'}
const page=(items,total=items.length)=>({items,total,page:1,size:24,hasMore:total>24})
test('memory uses its historical date, receipt contains no body or media',()=>{
  assert.equal(receipt.nodeDate,'2025-08-10');assert.equal(receipt.familyId,2)
  assert.equal('content' in receipt,false);assert.equal(creationDetail(receipt),'/memories/7')
})
test('capsule uses sealing date, not future opening date or content',()=>{
  const result=capsuleReceipt({id:7,title:'未来的信',createdAt:'2026-10-21T09:00:00',openDate:'2030-01-01',content:'sealed'},2)
  assert.equal(result.nodeDate,'2026-10-21');assert.equal(result.sourceType,'CAPSULE');assert.equal('content' in result,false)
  assert.equal(receiptMatches(index,result),false);assert.equal(creationDetail(result),'/capsules/7')
})
test('loaded index is reused without making any network reads',async()=>{
  const fail=()=>{throw Error('unexpected fetch')}
  assert.equal(await resolveCreationIndex(receipt,fail,fail,[index]),index)
})
test('new item on the last day page is found without scanning the full year',async()=>{
  const calls=[]
  const result=await resolveCreationIndex(receipt,async p=>{calls.push(p);return p===1?page([],241):page([index],241)},()=>{throw Error('search is unnecessary')})
  assert.deepEqual(calls,[1,11]);assert.equal(result,index)
})
test('source types never collide and missing index never creates a fake star',async()=>{
  const wrong={...index,sourceType:'CAPSULE'}
  let calls=0
  assert.equal(await resolveCreationIndex(receipt,async()=>{calls++;return page([wrong])},async()=>{calls++;return page([wrong])}),null)
  assert.equal(calls,2)
})
test('read failures reject, so a successful save can show refresh failure separately',async()=>{
  await assert.rejects(resolveCreationIndex(receipt,async()=>{throw Error('offline')},async()=>page([])),/offline/)
})
function node(key,kind,parent,count=1){return {key,kind,parent,count,year:2026,position:[0,0,0],ids:[],memoryCount:kind==='capsule'?0:count,capsuleCount:kind==='capsule'?count:0,color:'#fff'}}
const tree=[node('year:2026','year'),node('month:2026:10','month','year:2026'),node('day:2026:10:21','day','month:2026:10'),node('memory:7','memory','day:2026:10:21'),node('capsule:7','capsule','day:2026:10:21'),node('year:2025','year'),node('month:2026:11','month','year:2026',0)]
test('hover/click path contains every year/month/day ancestor and both child types',()=>{
  const result=branchContext(tree,'day:2026:10:21')
  assert.deepEqual(result.path.map(n=>n.key),['year:2026','month:2026:10','day:2026:10:21'])
  assert.deepEqual(result.children.map(n=>n.key),['memory:7','capsule:7'])
  assert.deepEqual(branchContext(tree,'year:2026').children.map(n=>n.key),['month:2026:10'])
  assert.deepEqual(branchContext(tree,'capsule:7').path.map(n=>n.kind),['year','month','day','capsule'])
})
test('malformed ancestry cannot cause an infinite traversal',()=>{
  const cyclic=[node('a','year','b'),node('b','month','a')]
  assert.equal(branchContext(cyclic,'a').path.length,2)
})
test('flight lands on the exact projected position, warm stars are stable',()=>{
  const start={x:12,y:70},end={x:180,y:20}
  assert.deepEqual(flightPosition(start,end,0),start)
  assert.ok(Math.abs(flightPosition(start,end,1).y-end.y)<1e-9)
  assert.equal(flightPosition(start,end,1).x,end.x)
  assert.equal(warmStarColor(tree[3]),warmStarColor(tree[3]))
  assert.equal(warmStarColor(tree[4]),warmStarColor(tree[4]))
})
test('success events stay in their family and are consumed exactly once',()=>{
  const events=useCreationEvents();events.publish(receipt)
  assert.equal(events.take(99),null);assert.equal(events.take(2),receipt);assert.equal(events.take(2),null)
})
test('old success events do not replay after a delayed visit',()=>{
  const events=useCreationEvents(),original=Date.now
  try{Date.now=()=>100;events.publish(receipt);Date.now=()=>30200;assert.equal(events.take(2),null)}finally{Date.now=original}
})
test('same month and same day share identities independently of year and parent',()=>{
  assert.equal(warmStarColor({key:'month:2026:2',kind:'month',month:2}),warmStarColor({key:'month:2023:2',kind:'month',month:2}))
  assert.equal(warmStarColor({key:'day:2026:2:2',kind:'day',day:2}),warmStarColor({key:'day:2024:11:2',kind:'day',day:2}))
  assert.notEqual(warmStarColor({key:'month:2026:2',kind:'month',month:2}),warmStarColor({key:'month:2026:3',kind:'month',month:3}))
})
test('memory and capsule colors are individually seeded in a warm hue range',()=>{
  const hue=hex=>{const c=[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16)/255),max=Math.max(...c),min=Math.min(...c),d=max-min;if(!d)return 0;let h=max===c[0]?(c[1]-c[2])/d:max===c[1]?2+(c[2]-c[0])/d:4+(c[0]-c[1])/d;return (h*60+360)%360}
  const colors=new Set()
  for(const kind of ['memory','capsule'])for(let id=1;id<=80;id++){
    const color=warmStarColor({key:`${kind}:${id}`,kind});const h=hue(color)
    assert.ok(h<=57||h>=337,`${color}: ${h}`);colors.add(color)
  }
  assert.ok(colors.size>100)
  assert.notEqual(warmStarColor({key:'memory:7',kind:'memory'}),warmStarColor({key:'capsule:7',kind:'capsule'}))
})
test('pointer leave clears transient focus but keeps only an explicit selection',()=>{
  assert.deepEqual(interactionFocus('day',null,null),{displayKey:'day',scopeKey:'day',transientKey:'day'})
  assert.deepEqual(interactionFocus(null,null,null),{displayKey:null,scopeKey:null,transientKey:null})
  assert.deepEqual(interactionFocus('day',null,'year'),{displayKey:'day',scopeKey:'day',transientKey:'day'})
  assert.deepEqual(interactionFocus(null,null,'year'),{displayKey:'year',scopeKey:'year',transientKey:null})
  assert.deepEqual(interactionFocus('year','memory:7','day'),{displayKey:'memory:7',scopeKey:'year',transientKey:'memory:7'})
})
const rendererModule=buildSync({entryPoints:[new URL('../src/three/ConstellationLinks.ts',import.meta.url).pathname.replace(/^\/(\w:)/,'$1')],bundle:true,format:'esm',platform:'node',write:false})
const {ConstellationLinks}=await import('data:text/javascript;base64,'+Buffer.from(rendererModule.outputFiles[0].text).toString('base64'))
function linkTree(){return tree.map((item,i)=>({...item,position:[i*10,(i%3)*7,i*2]}))}
test('actual line model reveals ancestors and descendants, then clears all hover edges',()=>{
  const links=new ConstellationLinks(linkTree())
  links.hover('day:2026:10:21')
  assert.deepEqual(new Set(links.edges.map(e=>e.key)),new Set(['year:2026>month:2026:10','month:2026:10>day:2026:10:21','day:2026:10:21>memory:7','day:2026:10:21>capsule:7']))
  links.hover(null);assert.equal(links.edges.length,0);assert.equal(links.revealedNodes().hovered.length,0)
  links.dispose()
})
test('actual line model removes a temporary branch and retains the pinned branch only',()=>{
  const links=new ConstellationLinks(linkTree())
  links.select('day:2026:10:21');const pinned=links.edges.map(e=>e.key).sort()
  links.hover('year:2025');links.hover(null)
  assert.deepEqual(links.edges.map(e=>e.key).sort(),pinned)
  assert.deepEqual(links.revealedNodes().ancestors.map(n=>n.key),['month:2026:10','year:2026'])
  links.select(null);assert.equal(links.edges.length,0);links.dispose()
})
test('rendered connection endpoints inherit their actual parent and child star colors',()=>{
  const colored=linkTree().map(item=>({...item,color:warmStarColor(item)})),links=new ConstellationLinks(colored)
  links.hover('day:2026:10:21')
  const edge=links.edges[0],geometry=links.active.geometry,color=geometry.getAttribute('color'),progress=geometry.getAttribute('aProgress')
  const end=[...progress.array].findIndex(value=>value===1)
  const normalized=values=>{const max=Math.max(...values);return values.map(v=>v/max)}
  for(const [index,hex] of [[0,edge.from.color],[end,edge.to.color]]){
    const actual=normalized([color.getX(index),color.getY(index),color.getZ(index)]),expectedColor=new Color(hex),expected=normalized([expectedColor.r,expectedColor.g,expectedColor.b])
    actual.forEach((value,i)=>assert.ok(Math.abs(value-expected[i])<1e-6))
  }
  links.dispose()
})
test('source type filtering drives time nodes, star visibility and line edges together',()=>{
  const typed=linkTree().map(item=>({...item,ids:item.kind==='memory'?[7]:item.kind==='capsule'?[70]:[],memoryCount:item.kind==='capsule'?0:1,capsuleCount:item.year===2026&&item.kind!=='memory'&&item.key!=='year:2025'?1:0}))
  const capsule=typed.find(n=>n.kind==='capsule'),memory=typed.find(n=>n.kind==='memory')
  assert.equal(matchesUniverseNode(capsule,'capsule',null,null),true)
  assert.equal(matchesUniverseNode(memory,'capsule',null,null),false)
  const filter=universeFilter(typed,'capsule',null,null)
  assert.ok(filter.ids.has(70));assert.equal(filter.ids.has(7),false)
  const links=new ConstellationLinks(typed);links.setMatches(filter.ids,filter.keys);links.select('day:2026:10:21')
  assert.ok(links.edges.some(e=>e.to.kind==='capsule'));assert.equal(links.edges.some(e=>e.to.kind==='memory'),false)
  links.dispose()
})


test('content hover and selection isolate one fragment, date scopes retain both types',()=>{
  const day={...node('day:2026:8:10','day','month:2026:8'),month:8,day:10}
  const a={...node('memory:12','memory',day.key),month:8,day:10}
  const b={...node('capsule:12','capsule',day.key),month:8,day:10}
  const c={...node('memory:13','memory',day.key),month:8,day:11}
  assert.deepEqual([a,b,c].filter(n=>fragmentContains(n,a)).map(n=>n.key),[a.key])
  assert.deepEqual([a,b,c].filter(n=>fragmentContains(n,day)).map(n=>n.key),[a.key,b.key])
  assert.equal(fragmentContains(b,undefined),true)
  const focus=interactionFocus(null,b.key,a.key)
  assert.equal(focus.scopeKey,a.key)
  assert.deepEqual([a,b].filter(n=>fragmentContains(n,focus.scopeKey===a.key?a:b)).map(n=>n.key),[a.key])
})


test('year context labels every descendant month/day and preserves a pinned branch',()=>{
 const y=node('year:2023','year'),m=node('month:2023:2','month',y.key),m2=node('month:2023:8','month',y.key)
 const d=node('day:2023:2:7','day',m.key),d2=node('day:2023:8:10','day',m2.key),leaf=node('memory:5','memory',d.key)
 const other=node('year:2026','year'),all=[y,m,m2,d,d2,leaf,other]
 assert.deepEqual([...calendarBranch(all,y.key)].sort(),[y.key,m.key,m2.key,d.key,d2.key].sort())
 assert.deepEqual([...calendarBranch(all,leaf.key)].sort(),[y.key,m.key,d.key].sort())
 assert.equal(calendarBranch(all,other.key,y.key).has(d2.key),true)
 assert.equal(calendarBranch(all,null).size,0)
})
test('sea wave displaces geometry, travels outward and returns exactly to rest',()=>{
 const center=[0,0,0],point=[65,0,0]
 assert.deepEqual(seaRippleOffset(point,center,0),[0,0,0])
 const wave=seaRippleOffset(point,center,.7)
 assert.ok(Math.abs(wave[2])>1);assert.ok(Math.abs(wave[0])>.1)
 assert.notDeepEqual(wave,seaRippleOffset(point,center,1.2))
 assert.ok(Math.abs(seaRippleOffset([400,0,0],center,.7)[2])<1e-10)
 assert.deepEqual(seaRippleOffset(point,center,4.8),[0,0,0])
})


test('calendar annotations stay on their actual connection while hover changes elsewhere',()=>{
 const links=new ConstellationLinks(linkTree());links.select('year:2026')
 const before=links.labelAnchor('month:2026:10').point.toArray()
 for(let i=0;i<10;i++)assert.deepEqual(links.labelAnchor('month:2026:10').point.toArray(),before)
 links.hover('year:2025')
 assert.deepEqual(links.labelAnchor('month:2026:10').point.toArray(),before)
 const edge=links.edges.find(e=>e.to.key==='month:2026:10'),t=.64
 const expected=edge.from.position.map((value,i)=>(1-t)**2*value+2*(1-t)*t*edge.control.getComponent(i)+t*t*edge.to.position[i])
 before.forEach((value,i)=>assert.ok(Math.abs(value-expected[i])<1e-8))
 links.dispose()
})
test('content clicks approach visibly, repeated close views never zoom out',()=>{
 assert.equal(contentCameraDistance(200,60),164)
 assert.equal(contentCameraDistance(60,60),60)
 assert.equal(contentCameraDistance(100,10),82)
 assert.equal(contentCameraDistance(80,10),70)
 assert.equal(contentCameraDistance(12,60),12)
 assert.equal(overviewMagnification,1.25)
 assert.ok(calendarMagnification.day>calendarMagnification.month&&calendarMagnification.month>calendarMagnification.year)
})


test('meteor has a visible moving tail, exact endpoints and bounded geometry for short paths',()=>{
 const from={x:180,y:280},to={x:690,y:180}
 assert.deepEqual(fragmentMeteorPoint(from,to,0),from)
 const end=fragmentMeteorPoint(from,to,1)
 assert.ok(Math.abs(end.x-to.x)<1e-8&&Math.abs(end.y-to.y)<1e-8)
 const mid=fragmentMeteorGeometry(from,to,.5)
 assert.ok(Math.hypot(mid.head.x-mid.tail[0].x,mid.head.y-mid.tail[0].y)>100)
 assert.equal(mid.opacity,1);assert.equal(mid.tail.length,22)
 assert.equal(fragmentMeteorGeometry(from,to,0).opacity,0)
 assert.equal(fragmentMeteorGeometry(from,to,1).opacity,0)
 const reverse=fragmentMeteorGeometry(to,from,.5)
 assert.ok(reverse.tail[0].x>reverse.head.x)
 const stationary=fragmentMeteorGeometry(from,from,.5)
 assert.ok(stationary.tail.every(p=>Number.isFinite(p.x)&&Number.isFinite(p.y)))
})

test('delivered calendar scope contains a collection without selecting individual records',()=>{
 const y={...node('year:2026','year'),year:2026},m={...node('month:2026:8','month',y.key),month:8}
 const day={...node('day:2026:8:10','day',m.key),month:8,day:10}
 const a={...node('memory:12','memory',day.key),month:8,day:10},b={...node('capsule:12','capsule',day.key),month:8,day:10}
 const other={...node('memory:13','memory','day:2026:10:21'),month:10,day:21}
 for(const scope of [m,day]){
  const records=[a,b,other].filter(n=>fragmentContains(n,scope))
  assert.equal(records.length,2);assert.equal(records.filter(n=>n.key===scope.key).length,0)
 }
 assert.equal([a,b].filter(n=>fragmentContains(n,a)).length,1)
})


test('cached content stays absent until a delivery or explicit search opens the panel scope',()=>{
 const day={...node('day:2026:8:10','day'),month:8,day:10}
 const a={...node('memory:1','memory',day.key),month:8,day:10,indexId:1,ids:[1]},b={...node('capsule:2','capsule',day.key),month:8,day:10,indexId:2,ids:[2]}
 assert.deepEqual(deliveredFragments([a,b],undefined,'all',null,null,false),[])
 assert.deepEqual(deliveredFragments([a,b],a,'all',null,null,true),[a])
 assert.equal(deliveredFragments([a,b],day,'all',null,null,true).length,2)
 assert.deepEqual(deliveredFragments([a,b],day,'capsule',null,null,true),[b])
})
test('meteor wake moves the sampled sea locally and projection follows the same refraction',()=>{
 const wakes=[{x:.4,y:.5,age:.3},{x:.46,y:.52,age:.18}],w=1200,h=800,original={x:530,y:390}
 const displayed=wakeProjection(original.x,original.y,w,h,wakes)
 const displacement=wakeOffset(displayed.x/w,1-displayed.y/h,w/h,wakes)
 assert.ok(Math.abs((displayed.x/w+displacement.x)*w-original.x)<1)
 assert.ok(Math.abs((1-(1-displayed.y/h+displacement.y))*h-original.y)<1)
 assert.ok(Math.hypot(displayed.x-original.x,displayed.y-original.y)>.01)
 assert.deepEqual(wakeProjection(original.x,original.y,w,h,[]),original)
 assert.deepEqual(wakeOffset(.45,.5,w/h,[{x:.4,y:.5,age:wakeLifetime}]),{x:0,y:0})
})


test('compact fragment pages show six rows and clamp correctly after filtering',()=>{
 assert.deepEqual(fragmentPageWindow(13,0),{pages:3,page:0,start:0,end:6})
 assert.deepEqual(fragmentPageWindow(13,1),{pages:3,page:1,start:6,end:12})
 assert.deepEqual(fragmentPageWindow(13,99),{pages:3,page:2,start:12,end:13})
 assert.deepEqual(fragmentPageWindow(2,2),{pages:1,page:0,start:0,end:2})
 assert.deepEqual(fragmentPageWindow(0,4),{pages:0,page:0,start:0,end:0})
 assert.deepEqual(fragmentPageWindow(8,-1),{pages:2,page:0,start:0,end:6})
})


test('click pins delivered content or promotes its existing flight without replay',()=>{
 assert.equal(fragmentActivation('memory:1','memory:1',null),'attached')
 assert.equal(fragmentActivation('day:2026:8:10',null,{key:'day:2026:8:10',reverse:false}),'in-flight')
 assert.equal(fragmentActivation('memory:2','memory:1',{key:'memory:3',reverse:false}),'start')
 assert.equal(fragmentActivation('memory:1','memory:1',{key:'memory:2',reverse:false}),'attached')
 assert.equal(fragmentActivation('memory:2','day:2026:8:10',{key:'memory:2',reverse:true}),'start')
})


test('receiver starlets ripple out, gather onto actual rows and disappear cleanly',()=>{
 const origin={x:140,y:90},rows=[{x:10,y:18,width:260,height:46},{x:10,y:72,width:260,height:46}]
 const start=gatherParticles(origin,280,180,0,rows),early=gatherParticles(origin,280,180,.12,rows),later=gatherParticles(origin,280,180,.32,rows)
 assert.equal(start.length,84);assert.ok(start.every(p=>p.opacity===0))
 assert.ok(Math.hypot(later[0].x-origin.x,later[0].y-origin.y)>Math.hypot(early[0].x-origin.x,early[0].y-origin.y))
 const mid=gatherParticles(origin,280,180,.5,rows),near=gatherParticles(origin,280,180,.85,rows),end=gatherParticles(origin,280,180,1,rows)
 for(let i=28;i<end.length;i++){
  const row=rows[(i-28)%rows.length],p=end[i]
  assert.ok(p.x>=row.x&&p.x<=row.x+row.width&&p.y>=row.y&&p.y<=row.y+row.height)
  assert.ok(Math.hypot(near[i].x-p.x,near[i].y-p.y)<Math.hypot(mid[i].x-p.x,mid[i].y-p.y))
 }
 assert.ok(end.every(p=>Math.abs(p.opacity)<1e-8))
 assert.ok(gatherParticles(origin,280,180,.4).every(p=>Number.isFinite(p.x)&&Number.isFinite(p.y)))
})


test('view state stays scoped to a family, invalidates data without losing filters and clears per session',()=>{
 clearViewStates();writeViewState('album:1',{keyword:'夏天',page:2,items:[1]});writeViewState('album:2',{keyword:'生日',page:1});writeViewState('vault:1',{filter:'pending'})
 invalidateViewData('album:')
 assert.equal(readViewState('album:1').keyword,'夏天');assert.equal(readViewState('album:1').stale,true)
 assert.equal(readViewState('album:2').keyword,'生日');assert.equal(readViewState('vault:1').stale,undefined)
 clearViewStates();assert.equal(readViewState('album:1'),undefined);assert.equal(readViewState('vault:1'),undefined)
})


const sceneModule=buildSync({entryPoints:[new URL('../src/three/UniverseScene.ts',import.meta.url).pathname.replace(/^\/(\w:)/,'$1')],bundle:true,format:'esm',platform:'node',write:false})
const {UniverseScene}=await import('data:text/javascript;base64,'+Buffer.from(sceneModule.outputFiles[0].text).toString('base64'))
test('star camera return restores pose, target and rotation without creating a flight',()=>{
 const source={camera:{position:new Vector3(11,22,77)},controls:{target:new Vector3(3,4,5)},group:new Group(),focusPosition:new Vector3(7,8,9),focusRadius:40,overview:false}
 source.group.rotation.set(.1,.2,.3)
 const saved=UniverseScene.prototype.captureView.call(source)
 let projected=false
 const target={camera:{position:new Vector3()},controls:{target:new Vector3(),enableDamping:false,update(){}},group:new Group(),focusPosition:new Vector3(),focusRadius:0,overview:true,reduced:false,finishFlight(){},project(){projected=true}}
 UniverseScene.prototype.restoreView.call(target,saved)
 assert.deepEqual(target.camera.position.toArray(),[11,22,77]);assert.deepEqual(target.controls.target.toArray(),[3,4,5])
 assert.equal(target.group.rotation.y,.2);assert.equal(target.focusRadius,40);assert.equal(target.tween,undefined);assert.equal(projected,true)
 assert.equal(target.overview,false)
})


test('wide album pagination follows available columns and media grids preserve usable tile sizes',()=>{
 assert.equal(albumColumns(1856),6);assert.equal(albumPageSize(1856),18)
 assert.equal(albumColumns(1376),4);assert.equal(albumPageSize(1376),12)
 assert.equal(albumColumns(960),3);assert.equal(albumColumns(343),1)
 assert.deepEqual([1,2,3,4,5,6,9].map(mediaGridColumns),[1,2,3,2,4,3,3])
})

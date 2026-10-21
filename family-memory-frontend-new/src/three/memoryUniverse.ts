import type {UniverseOverview,UniverseItem} from '@/types'

export type UniverseLevel='universe'|'year'|'month'|'day'
export type UniverseNode={
  key:string;kind:'year'|'month'|'day'|'memory'|'capsule';year:number;month?:number;day?:number
  memoryId?:number;capsuleId?:number;indexId?:number;title?:string;parent?:string
  count:number;memoryCount:number;capsuleCount:number;ids:number[];position:[number,number,number];color:string
}
export type UniverseLocation={level:UniverseLevel;year?:number;month?:number;day?:number}

export function hashKey(value: string) {
  let hash = 2166136261
  for (let i = 0; i < value.length; i++) hash = Math.imul(hash ^ value.charCodeAt(i), 16777619)
  return hash >>> 0
}
export function randomFromKey(value: string) {
  let seed = hashKey(value)
  return () => {
    seed += 0x6D2B79F5
    let n = Math.imul(seed ^ seed >>> 15, seed | 1)
    n ^= n + Math.imul(n ^ n >>> 7, n | 61)
    return ((n ^ n >>> 14) >>> 0) / 4294967296
  }
}

function colorSeed(key:string){const rng=randomFromKey(`hue:${key}`);return {h:rng()*360,s:.62+rng()*.25,l:.62+rng()*.12}}
function hslColor(h:number,s:number,l:number){
  const a=s*Math.min(l,1-l)
  const channel=(n:number)=>{const k=(n+h/30)%12;return Math.round(255*(l-a*Math.max(-1,Math.min(k-3,9-k,1)))).toString(16).padStart(2,'0')}
  return `#${channel(0)}${channel(8)}${channel(4)}`
}
export function universeColor(key: string) {const {h,s,l}=colorSeed(key);return hslColor(h,s,l)}


/** 仅从年月日计数构造概要节点，不为内容数量制造逐条假节点。 */
export function buildOverviewUniverse(overview:UniverseOverview):UniverseNode[]{
  const nodes:UniverseNode[]=[]
  const add=(kind:UniverseNode['kind'],year:number,memoryCount:number,capsuleCount:number,month?:number,day?:number,parent?:string)=>{
    const key=kind==='year'?`year:${year}`:kind==='month'?`month:${year}:${month}`:`day:${year}:${month}:${day}`
    nodes.push({key,kind,year,month,day,parent,count:memoryCount+capsuleCount,memoryCount,capsuleCount,ids:[],position:[0,0,0],color:universeColor(key)})
  }
  for(const year of overview.years){
    add('year',year.year,year.memoryCount,year.capsuleCount)
    for(const month of year.months){
      add('month',year.year,month.memoryCount,month.capsuleCount,month.month,undefined,`year:${year.year}`)
      for(const day of month.days)add('day',year.year,day.memoryCount,day.capsuleCount,month.month,day.day,`month:${year.year}:${month.month}`)
    }
  }
  return layoutOverview(nodes)
}
function layoutOverview(nodes:UniverseNode[]):UniverseNode[]{
  // Stable colors span the whole hue wheel rather than repeating a small palette.
  const siblings=new Map<string,UniverseNode[]>()
  for(const node of nodes){const key=node.parent||'universe';const group=siblings.get(key)||[];group.push(node);siblings.set(key,group)}
  // Each occupied subtree owns a sector. Descendants turn clockwise within it,
  // keeping sibling branches apart without inventing extra memory nodes.
  const leaves=new Map<string,number>()
  const weight=(node:UniverseNode):number=>{
    if(leaves.has(node.key))return leaves.get(node.key)!
    const children=(siblings.get(node.key)||[]).filter(child=>child.count)
    const value=children.length?children.reduce((sum,child)=>sum+weight(child),0):Math.sqrt(Math.max(1,node.count))
    leaves.set(node.key,value);return value
  }
  const radii=[0,37,62,82]
  const arrange=(parent:UniverseNode,root:UniverseNode,depth:number,high:number,low:number)=>{
    const children=(siblings.get(parent.key)||[]).filter(child=>child.count).sort((a,b)=>(a.month||0)-(b.month||0)||(a.day||0)-(b.day||0)||(a.memoryId||a.capsuleId||0)-(b.memoryId||b.capsuleId||0))
    const total=children.reduce((sum,node)=>sum+weight(node),0)
    let cursor=high
    for(const child of children){
      const span=(high-low)*weight(child)/Math.max(1,total),mid=cursor-span*.5
      const rng=randomFromKey(`branch:${child.key}`)
      const theta=mid-(depth-1)*.12+(rng()-.5)*Math.min(.035,span*.08)
      const radius=(radii[depth]||82)+(rng()-.5)*2+Math.max(0,Math.sqrt(children.length)-5)*(depth===3?3:1)
      const depthWave=Math.sin(mid*2.25+.7)*radius*.55+Math.sin(depth*.85+mid)*radius*.12
      child.position=[root.position[0]+Math.cos(theta)*radius,root.position[1]+Math.sin(theta)*radius,root.position[2]+depthWave+(rng()-.5)*4]
      arrange(child,root,depth+1,cursor-span*.06,cursor-span*.94)
      cursor-=span
    }
  }
  for(const root of nodes.filter(node=>node.kind==='year'))arrange(root,root,1,2.95,.22)
  // Pack whole years with breathing room, rather than packing their roots alone.
  // Local branches retain their shapes, while each year's occupied volume stays separate.
  const clusters:Array<{x:number;y:number;r:number}>=[]
  for(const root of nodes.filter(node=>node.kind==='year')){
    const members=nodes.filter(node=>node.year===root.year),occupied=members.filter(node=>node.count)
    if(!occupied.length)occupied.push(root)
    const rotation=(randomFromKey(`cluster:${root.year}`)()-.5)*.65
    const cos=Math.cos(rotation),sin=Math.sin(rotation)
    for(const node of members){const x=node.position[0]-root.position[0],y=node.position[1]-root.position[1];if(node!==root){node.position[0]=root.position[0]+x*cos-y*sin;node.position[1]=root.position[1]+x*sin+y*cos}}
    const cx=(Math.min(...occupied.map(node=>node.position[0]))+Math.max(...occupied.map(node=>node.position[0])))/2
    const cy=(Math.min(...occupied.map(node=>node.position[1]))+Math.max(...occupied.map(node=>node.position[1])))/2
    const radius=Math.max(45,...occupied.map(node=>Math.hypot(node.position[0]-cx,node.position[1]-cy)))+16
    let x=0,y=0
    if(clusters.length)for(let step=1;step<=20000;step++){
      const angle=step*2.3999632297+.25,distance=Math.sqrt(step)*32
      x=Math.cos(angle)*distance;y=Math.sin(angle)*distance*.82
      if(clusters.every(other=>Math.hypot(x-other.x,y-other.y)>=radius+other.r+38))break
    }
    for(const node of members){node.position[0]+=x-cx;node.position[1]+=y-cy}
    clusters.push({x,y,r:radius})
  }
  if(clusters.length){
    const cx=(Math.min(...clusters.map(c=>c.x-c.r))+Math.max(...clusters.map(c=>c.x+c.r)))/2
    const cy=(Math.min(...clusters.map(c=>c.y-c.r))+Math.max(...clusters.map(c=>c.y+c.r)))/2
    for(const node of nodes){node.position[0]-=cx;node.position[1]-=cy}
  }
  const assignedHues=new Map<string,number>()
  const hueDistance=(a:number,b:number)=>Math.min(Math.abs(a-b),360-Math.abs(a-b))
  for(const [parent,group] of siblings){
    group.sort((a,b)=>Number(b.count>0)-Number(a.count>0)||a.key.localeCompare(b.key))
    let previous=assignedHues.get(parent)
    for(const node of group){
      const seed=colorSeed(node.key),parentHue=assignedHues.get(parent)
      let hue=seed.h
      for(let i=0;i<12;i++){
        if((previous===undefined||hueDistance(hue,previous)>35)&&(parentHue===undefined||hueDistance(hue,parentHue)>30))break
        hue=(hue+137.508)%360
      }
      node.color=hslColor(hue,seed.s,seed.l);assignedHues.set(node.key,hue);previous=hue
    }
  }
  return nodes
}

/** 页面分批加入概要，网络和渲染中途取消时不会再补入旧数据。 */
export async function buildOverviewProgressively(overview:UniverseOverview,onBatch:(nodes:UniverseNode[],all:UniverseNode[])=>void,cancelled:()=>boolean){
  if(cancelled())return
  const all=buildOverviewUniverse(overview),rank={year:0,month:1,day:2,memory:3,capsule:3}
  const ordered=[...all].sort((a,b)=>rank[a.kind]-rank[b.kind])
  let loaded:UniverseNode[]=[]
  for(let start=0;start<ordered.length;start+=120){
    if(cancelled())return
    loaded=[...loaded,...ordered.slice(start,start+120)];onBatch(loaded,all)
    if(start+120<ordered.length)await new Promise<void>(resolve=>requestAnimationFrame(()=>resolve()))
  }
  if(!ordered.length)onBatch([],all)
}

/** 仅为当前日期页或搜索页建立真实内容节点，已有日期位置不变。 */
export function buildContentNodes(items:UniverseItem[],overviewNodes:UniverseNode[]):UniverseNode[]{
  const parents=new Map(overviewNodes.map(node=>[node.key,node]))
  return items.flatMap(item=>{
    const [year,month,day]=item.nodeDate.split('-').map(Number),parent=`day:${year}:${month}:${day}`,anchor=parents.get(parent)
    if(!anchor)return []
    const kind=item.sourceType==='MEMORY'?'memory':'capsule',key=`${kind}:${item.sourceId}`,rng=randomFromKey(`leaf:${key}`)
    const angle=rng()*Math.PI*2,radius=6+rng()*11
    return [{key,kind,year:year!,month,day,parent,indexId:item.indexId,title:item.title,memoryId:kind==='memory'?item.sourceId:undefined,capsuleId:kind==='capsule'?item.sourceId:undefined,count:1,memoryCount:kind==='memory'?1:0,capsuleCount:kind==='capsule'?1:0,ids:[item.indexId],position:[anchor.position[0]+Math.cos(angle)*radius,anchor.position[1]+Math.sin(angle)*radius*.8,anchor.position[2]+(rng()-.5)*12] as [number,number,number],color:universeColor(key)} satisfies UniverseNode]
  })
}
export function itemPathKeys(items:UniverseItem[]):Set<string>{
  const keys=new Set<string>()
  for(const item of items){const [year,month,day]=item.nodeDate.split('-').map(Number);keys.add(`year:${year}`);keys.add(`month:${year}:${month}`);keys.add(`day:${year}:${month}:${day}`);keys.add(`${item.sourceType==='MEMORY'?'memory':'capsule'}:${item.sourceId}`)}
  return keys
}
export function locationKey(location:UniverseLocation){return location.level==='universe'?'':location.level==='year'?`year:${location.year}`:location.level==='month'?`month:${location.year}:${location.month}`:`day:${location.year}:${location.month}:${location.day}`}
export function levelNodes(nodes:UniverseNode[],location:UniverseLocation){const kind={universe:'year',year:'month',month:'day',day:'memory'}[location.level],parent=locationKey(location);return nodes.filter(node=>(node.kind===kind||(kind==='memory'&&node.kind==='capsule'))&&(!parent||node.parent===parent))}

import type {UniverseNode} from './memoryUniverse'

/** The complete ancestry and direct children are kept separate from content previews. */
export function branchContext(nodes:UniverseNode[],key:string|null) {
  const map=new Map(nodes.map(node=>[node.key,node])),path:UniverseNode[]=[],seen=new Set<string>()
  const focus=key?map.get(key):undefined
  let node=focus
  while(node&&!seen.has(node.key)){seen.add(node.key);path.unshift(node);node=node.parent?map.get(node.parent):undefined}
  const children=focus?nodes.filter(item=>item.parent===focus.key&&item.count>0):nodes.filter(item=>item.kind==='year'&&item.count>0)
  return {focus,path,children}
}

export function warmStarColor(node:Pick<UniverseNode,'key'|'kind'>&Partial<Pick<UniverseNode,'year'|'month'|'day'>>) {
  // Calendar identities are shared across years/months; content identities include their source type.
  const parts=node.key.split(':')
  const key=node.kind==='month'?`month:${node.month??Number(parts[2])}`:node.kind==='day'?`day:${node.day??Number(parts[3])}`:node.key
  let seed=2166136261;for(const char of key)seed=Math.imul(seed^char.charCodeAt(0),16777619)>>>0
  const content=node.kind==='memory'||node.kind==='capsule'
  const hue=content?(seed%5===0?338+seed%22:8+seed%48):seed%360
  const saturation=content ? .64+(seed%17)/100 : .59+(seed%22)/100,lightness=.65+(seed%12)/100
  const a=saturation*Math.min(lightness,1-lightness)
  const channel=(n:number)=>{const k=(n+hue/30)%12;return Math.round(255*(lightness-a*Math.max(-1,Math.min(k-3,9-k,1)))).toString(16).padStart(2,'0')}
  return `#${channel(0)}${channel(8)}${channel(4)}`
}

export function fragmentContains(node:UniverseNode,scope:UniverseNode|undefined) {
  if(!scope)return true
  if(scope.kind==='memory'||scope.kind==='capsule')return node.key===scope.key
  return node.year===scope.year&&(!scope.month||node.month===scope.month)&&(!scope.day||node.day===scope.day)
}

export function interactionFocus(hovered:string|null,panelPreview:string|null,selected:string|null) {
  return {displayKey:panelPreview||hovered||selected,scopeKey:hovered||selected,transientKey:panelPreview||hovered}
}

export function matchesUniverseNode(node:UniverseNode,type:'all'|'memory'|'capsule',ids:Set<number>|null,keys:Set<string>|null) {
  const count=type==='memory'?node.memoryCount:type==='capsule'?node.capsuleCount:node.count
  return count>0&&(ids===null||Boolean(keys?.has(node.key))||node.ids.some(id=>ids.has(id)))
}

export function universeFilter(nodes:UniverseNode[],type:'all'|'memory'|'capsule',ids:Set<number>|null,keys:Set<string>|null) {
  if(type==='all'&&ids===null)return {ids:null,keys:null}
  const eligible=nodes.filter(node=>matchesUniverseNode(node,type,ids,keys))
  return {ids:new Set(eligible.flatMap(node=>node.ids)),keys:new Set(eligible.map(node=>node.key))}
}


export function deliveredFragments(nodes:UniverseNode[],scope:UniverseNode|undefined,type:'all'|'memory'|'capsule',ids:Set<number>|null,keys:Set<string>|null,ready:boolean){
 if(!ready)return []
 return nodes.filter(n=>(n.kind==='memory'||n.kind==='capsule')&&fragmentContains(n,scope)&&matchesUniverseNode(n,type,ids,keys)).sort((a,b)=>(b.year-a.year)||((b.month||0)-(a.month||0))||((b.day||0)-(a.day||0))||((a.indexId||0)-(b.indexId||0)))
}

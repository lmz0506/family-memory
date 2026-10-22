import type {UniverseNode} from './memoryUniverse'
export function calendarBranch(nodes:UniverseNode[],...keys:(string|null)[]) {
  const map=new Map(nodes.map(n=>[n.key,n])),children=new Map<string,UniverseNode[]>()
  for(const n of nodes)if(n.parent){const list=children.get(n.parent)||[];list.push(n);children.set(n.parent,list)}
  const result=new Set<string>()
  for(const key of keys){
    let n=key?map.get(key):undefined;const seen=new Set<string>(),pending=n?[n]:[]
    while(n&&!seen.has(n.key)){seen.add(n.key);if(n.kind!=='memory'&&n.kind!=='capsule')result.add(n.key);n=n.parent?map.get(n.parent):undefined}
    seen.clear()
    while(pending.length){const current=pending.pop()!;if(seen.has(current.key))continue;seen.add(current.key)
      if(current.kind!=='memory'&&current.kind!=='capsule'&&current.count){result.add(current.key);pending.push(...(children.get(current.key)||[]))}
    }
  }
  return result
}

import type {Capsule, Memory, UniverseItem, UniversePage, UniverseSourceType} from '@/types'

export interface CreationReceipt {
  familyId: number
  sourceType: UniverseSourceType
  sourceId: number
  title: string
  nodeDate: string
  origin?: {x:number;y:number;width?:number;height?:number}
}

export function memoryReceipt(memory: Memory, origin?: CreationReceipt['origin']): CreationReceipt {
  return {familyId:memory.familyId,sourceType:'MEMORY',sourceId:memory.id,title:memory.title,nodeDate:memory.memoryDate.slice(0,10),origin}
}

export function capsuleReceipt(capsule: Capsule, familyId:number, origin?: CreationReceipt['origin']): CreationReceipt {
  // The index archives a capsule on its sealing date, never on its future opening date.
  return {familyId,sourceType:'CAPSULE',sourceId:capsule.id,title:capsule.title,nodeDate:capsule.createdAt.slice(0,10),origin}
}

export function receiptMatches(item:UniverseItem, receipt:CreationReceipt) {
  return item.sourceType===receipt.sourceType && item.sourceId===receipt.sourceId
}

export function creationDetail(receipt:CreationReceipt) {
  return receipt.sourceType==='MEMORY'?`/memories/${receipt.sourceId}`:`/capsules/${receipt.sourceId}`
}

export function flightPosition(start:{x:number;y:number}, end:{x:number;y:number}, progress:number) {
  const t=Math.max(0,Math.min(1,progress)),bend=Math.min(150,Math.hypot(end.x-start.x,end.y-start.y)*.25)
  return {x:start.x+(end.x-start.x)*t,y:start.y+(end.y-start.y)*t-Math.sin(Math.PI*t)*bend}
}

/** At most three bounded reads: first day page, newest day page, title search. */
export async function resolveCreationIndex(receipt:CreationReceipt, day:(page:number)=>Promise<UniversePage>, search:()=>Promise<UniversePage>, cached:Iterable<UniverseItem>=[]) {
  for(const item of cached)if(receiptMatches(item,receipt))return item
  const first=await day(1)
  let found=first.items.find(item=>receiptMatches(item,receipt))
  if(found)return found
  const lastPage=Math.ceil(first.total/Math.max(1,first.size))
  if(lastPage>1){found=(await day(lastPage)).items.find(item=>receiptMatches(item,receipt));if(found)return found}
  return (await search()).items.find(item=>receiptMatches(item,receipt))||null
}

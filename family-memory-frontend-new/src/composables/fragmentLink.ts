export type LinkPoint={x:number;y:number}
export const fragmentMeteorDuration=780
export function fragmentMeteorPoint(from:LinkPoint,to:LinkPoint,t:number):LinkPoint{
 const progress=Math.max(0,Math.min(1,t)),bend=Math.min(90,Math.hypot(to.x-from.x,to.y-from.y)*.16)
 return {x:from.x+(to.x-from.x)*progress,y:from.y+(to.y-from.y)*progress-Math.sin(Math.PI*progress)*bend}
}
export function fragmentMeteorGeometry(from:LinkPoint,to:LinkPoint,progress:number){
 const t=Math.max(0,Math.min(1,progress)),head=fragmentMeteorPoint(from,to,t)
 const length=Math.max(1,Math.hypot(to.x-from.x,to.y-from.y)),tailFraction=Math.min(.42,140/length)
 const tail=Array.from({length:22},(_,i)=>fragmentMeteorPoint(from,to,Math.max(0,t-tailFraction*(1-i/21))))
 return {head,tail,opacity:Math.min(1,t/.08,(1-t)/.06),points:tail.map(p=>`${p.x},${p.y}`).join(' ')}
}


export function fragmentActivation(key:string,delivered:string|null,pending:{key:string;reverse:boolean}|null){
 if(key===delivered)return 'attached'
 if(pending?.key===key&&!pending.reverse)return 'in-flight'
 return 'start'
}

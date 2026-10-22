export type GatherTarget={x:number;y:number;width:number;height:number}
export const gatherDuration=1400,gatherDeliveryDelay=220
export function gatherParticles(origin:{x:number;y:number},width:number,height:number,progress:number,targets:GatherTarget[]=[]){
 const t=Math.max(0,Math.min(1,progress))
 return Array.from({length:84},(_,i)=>{
  const angle=i*2.39996323,radius=32+(i*37%67),wave=i<28
  if(wave){const age=Math.max(0,t-(i%2)*.07),r=age*Math.max(width,height)*.85*(1+.05*Math.sin(angle*3));return {x:origin.x+Math.cos(angle)*r,y:origin.y+Math.sin(angle)*r*.65,opacity:Math.max(0,Math.sin(Math.min(1,age/.5)*Math.PI))*.6,size:1+(i%3)*.4,wave:true}}
  const burst={x:origin.x+Math.cos(angle)*radius,y:origin.y+Math.sin(angle)*radius*.65}
  const row=targets.length?targets[(i-28)%targets.length]:{x:width*.13,y:height*.28,width:width*.74,height:Math.max(38,height*.3)}
  const target={x:row!.x+row!.width*(.08+(i*17%85)/100),y:row!.y+row!.height*(.2+(i*11%61)/100)}
  const expand=Math.min(1,t/.25),settle=Math.max(0,Math.min(1,(t-.28)/.67)),ease=settle*settle*(3-2*settle)
  return {x:(origin.x+(burst.x-origin.x)*expand)*(1-ease)+target.x*ease,y:(origin.y+(burst.y-origin.y)*expand)*(1-ease)+target.y*ease,opacity:Math.min(1,t/.1)*Math.max(0,1-Math.pow(settle,2))*.8,size:1+(i%4)*.3,wave:false}
 })
}

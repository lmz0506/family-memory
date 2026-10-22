export type MeteorWake={x:number;y:number;age:number}
export const wakeLifetime=1.8,wakeLimit=16
export function wakeOffset(x:number,y:number,aspect:number,wakes:MeteorWake[]){
 let ox=0,oy=0
 for(const w of wakes){if(w.age<=0||w.age>=wakeLifetime)continue
  const dx=(x-w.x)*aspect,dy=y-w.y,d=Math.hypot(dx,dy),front=d-w.age*.11
  const amount=Math.sin(front*140)*Math.exp(-Math.pow(front/.035,2))*Math.pow(1-w.age/wakeLifetime,2)*Math.min(1,w.age/.12)*.0035
  ox+=dx/Math.max(d,.0001)*amount/aspect;oy+=dy/Math.max(d,.0001)*amount
 }
 return {x:Math.max(-.012,Math.min(.012,ox)),y:Math.max(-.012,Math.min(.012,oy))}
}
export function wakeProjection(x:number,y:number,width:number,height:number,wakes:MeteorWake[]){
 if(!wakes.length)return {x,y}
 const u=x/width,v=1-y/height;let qx=u,qy=v
 for(let i=0;i<5;i++){const offset=wakeOffset(qx,qy,width/height,wakes);qx=u-offset.x;qy=v-offset.y}
 return {x:qx*width,y:(1-qy)*height}
}
export const meteorWakeShader={
 uniforms:{tDiffuse:{value:null},uAspect:{value:1},uWakes:{value:[]},uCount:{value:0}},
 vertexShader:`varying vec2 vUv;void main(){vUv=uv;gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0);}`,
 fragmentShader:`uniform sampler2D tDiffuse;uniform float uAspect;uniform vec4 uWakes[16];uniform int uCount;varying vec2 vUv;
 void main(){vec2 offset=vec2(0.0);for(int i=0;i<16;i++){if(i>=uCount)break;vec4 w=uWakes[i];if(w.z<=0.0||w.z>=1.8)continue;
 vec2 delta=(vUv-w.xy)*vec2(uAspect,1.0);float d=length(delta),front=d-w.z*.11;
 float amount=sin(front*140.0)*exp(-pow(front/.035,2.0))*pow(1.0-w.z/1.8,2.0)*min(1.0,w.z/.12)*.0035;
 offset+=delta/max(d,.0001)*amount/vec2(uAspect,1.0);}
 gl_FragColor=texture2D(tDiffuse,vUv+clamp(offset,vec2(-.012),vec2(.012)));}`
}

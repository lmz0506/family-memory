// The same field drives GPU geometry and CPU hit/label projection.
export function seaRippleOffset(position:readonly number[],center:readonly number[],age:number){
 if(age<=0||age>=4.8)return [0,0,0] as const
 const dx=position[0]!-center[0]!,dy=position[1]!-center[1]!,dz=position[2]!-center[2]!,distance=Math.hypot(dx,dy,dz)
 const front=distance-age*82,envelope=Math.exp(-Math.pow(front/32,2))*Math.sin(Math.min(1,age/.2)*Math.PI/2)*Math.pow(1-age/4.8,2)
 const wave=Math.sin(front*.19)*envelope
 return [dx/Math.max(distance,1)*wave*2.4,dy/Math.max(distance,1)*wave*2.4,wave*9] as const
}
export const seaRippleGLSL=`
uniform vec3 uRippleCenter;uniform float uRippleAge;
vec3 seaRipple(vec3 p){
 if(uRippleAge<=0.0||uRippleAge>=4.8)return p;
 vec3 delta=p-uRippleCenter;float d=length(delta),front=d-uRippleAge*82.0;
 float envelope=exp(-pow(front/32.0,2.0))*sin(min(1.0,uRippleAge/.2)*1.5707963)*pow(1.0-uRippleAge/4.8,2.0);
 float wave=sin(front*.19)*envelope;
 return p+vec3(delta.xy/max(d,1.0)*wave*2.4,wave*9.0);
}`

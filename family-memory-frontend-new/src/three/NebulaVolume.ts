import * as THREE from 'three'
import {randomFromKey} from './memoryUniverse'

let cachedNoise: Uint8Array | undefined
const NOISE_SIZE = 128
function bakeNoise() {
  if (cachedNoise) return cachedNoise
  const rng=randomFromKey('volumetric-ocean-noise'),grid=new Float32Array(32*32*32)
  for(let i=0;i<grid.length;i++)grid[i]=rng()
  const sample=(x:number,y:number,z:number)=>{
    const ix=Math.floor(x),iy=Math.floor(y),iz=Math.floor(z)
    let fx=x-ix,fy=y-iy,fz=z-iz
    fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);fz=fz*fz*(3-2*fz)
    const at=(a:number,b:number,c:number)=>grid[(a&31)+((b&31)<<5)+((c&31)<<10)]!
    const mix=(a:number,b:number,t:number)=>a+(b-a)*t
    return mix(mix(mix(at(ix,iy,iz),at(ix+1,iy,iz),fx),mix(at(ix,iy+1,iz),at(ix+1,iy+1,iz),fx),fy),mix(mix(at(ix,iy,iz+1),at(ix+1,iy,iz+1),fx),mix(at(ix,iy+1,iz+1),at(ix+1,iy+1,iz+1),fx),fy),fz)
  }
  const data=new Uint8Array(NOISE_SIZE**3*4)
  for(let z=0;z<NOISE_SIZE;z++)for(let y=0;y<NOISE_SIZE;y++)for(let x=0;x<NOISE_SIZE;x++){
    const index=(x+y*NOISE_SIZE+z*NOISE_SIZE*NOISE_SIZE)*4
    const a=x/8,b=y/8,c=z/8
    const density=sample(a,b,c)*.52+sample(a*2+7,b*2+3,c*2+11)*.27+sample(a*4+13,b*4,c*4+5)*.14+sample(a*8,b*8+17,c*8)*.07
    data[index]=Math.round(density*255)
    data[index+2]=Math.round(sample(x/25+11,y/25+7,z/25+19)*255)
    data[index+3]=Math.floor(rng()*256)
  }
  for(let z=0;z<NOISE_SIZE;z++)for(let y=0;y<NOISE_SIZE;y++)for(let x=0;x<NOISE_SIZE;x++){
    const index=(x+y*NOISE_SIZE+z*NOISE_SIZE*NOISE_SIZE)*4
    const neighbor=(((x+126)%NOISE_SIZE)+((y+3)%NOISE_SIZE)*NOISE_SIZE+((z+1)%NOISE_SIZE)*NOISE_SIZE*NOISE_SIZE)*4
    data[index+1]=Math.round(Math.max(.12,Math.min(1,.5+(data[neighbor]!-data[index]!)/255*4))*255)
  }
  cachedNoise=data
  return data
}

// Ray-marched world-space gas. A small 3D noise texture gives fine detail without
// evaluating many procedural noise octaves for every ray sample on the CPU/GPU.
export class NebulaVolume {
  private target=new THREE.WebGLRenderTarget(1,1,{type:THREE.HalfFloatType,depthBuffer:false,stencilBuffer:false})
  private scene=new THREE.Scene()
  private camera=new THREE.OrthographicCamera(-1,1,1,-1,0,1)
  private noise:THREE.Data3DTexture
  private mesh:THREE.Mesh<THREE.PlaneGeometry,THREE.ShaderMaterial>
  private width=1
  private height=1
  private lastUpdate=0
  private lastTime=-1
  private lastCamera=new THREE.Matrix4()
  private lastWorld=new THREE.Matrix4()
  private dirty=true
  texture=this.target.texture
  constructor(private renderer:THREE.WebGLRenderer){
    this.noise=new THREE.Data3DTexture(bakeNoise(),NOISE_SIZE,NOISE_SIZE,NOISE_SIZE)
    this.noise.format=THREE.RGBAFormat
    this.noise.minFilter=this.noise.magFilter=THREE.LinearFilter
    this.noise.wrapS=this.noise.wrapT=this.noise.wrapR=THREE.RepeatWrapping
    this.noise.generateMipmaps=false;this.noise.unpackAlignment=1;this.noise.needsUpdate=true
    const material=new THREE.ShaderMaterial({
      uniforms:{uNoise:{value:this.noise},uTime:{value:0},uInverseProjection:{value:new THREE.Matrix4()},uCameraWorld:{value:new THREE.Matrix4()},uWorldInverse:{value:new THREE.Matrix4()},uSteps:{value:48}},
      depthTest:false,depthWrite:false,toneMapped:false,
      vertexShader:`varying vec2 vUv;void main(){vUv=uv;gl_Position=vec4(position.xy,0.0,1.0);}`,
      fragmentShader:`
        precision highp sampler3D;
        uniform sampler3D uNoise;uniform float uTime;uniform mat4 uInverseProjection;uniform mat4 uCameraWorld;uniform mat4 uWorldInverse;uniform int uSteps;varying vec2 vUv;
        vec4 noise4(vec3 p){vec3 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return texture(uNoise,(cell+f+.5)/128.0);}
        float hash21(vec2 p){vec3 q=fract(vec3(mod(p,128.0).xy,p.x+p.y)*.1031);q+=dot(q,q.yzx+33.33);return fract((q.x+q.y)*q.z);}
        vec2 boxIntersection(vec3 ro,vec3 rd){vec3 a=(vec3(-370.0,-190.0,-310.0)-ro)/rd;vec3 b=(vec3(370.0,190.0,110.0)-ro)/rd;vec3 lo=min(a,b),hi=max(a,b);return vec2(max(max(lo.x,lo.y),lo.z),min(min(hi.x,hi.y),hi.z));}
        vec4 medium(vec3 p){
          // Separated, feathered wisps leave most rays as clear deep space.
          vec3 a=(p-vec3(-180.0,60.0,-115.0))/vec3(112.0,25.0,70.0);
          vec3 b=(p-vec3(-35.0,-48.0,-175.0))/vec3(100.0,20.0,62.0);
          vec3 c=(p-vec3(155.0,48.0,-95.0))/vec3(125.0,24.0,72.0);
          vec3 d=(p-vec3(230.0,-70.0,10.0))/vec3(100.0,19.0,60.0);
          vec3 e=(p-vec3(-255.0,-65.0,-35.0))/vec3(90.0,20.0,58.0);
          a.y+=a.x*.28;c.y-=c.x*.32;b.y+=b.x*.22;
          float envelope=max(max(exp(-dot(a,a)*1.5),exp(-dot(b,b)*1.5)),max(exp(-dot(c,c)*1.5),max(exp(-dot(d,d)*1.6),exp(-dot(e,e)*1.6))));
          if(envelope<.004)return vec4(0.0);
          vec3 q=p*.18+vec3(uTime*.035,uTime*.016,0.0);
          vec4 base=noise4(q+vec3(41.0,57.0,29.0));
          float f=base.r;
          float gas=pow(smoothstep(.42,.79,f),1.65);
          float density=gas*envelope*.015;
          float edge=base.g;
          float hue=base.b;
          vec3 blue=vec3(.035,.105,.23),teal=vec3(.035,.19,.21),lilac=vec3(.18,.063,.19),copper=vec3(.3,.19,.085);
          float tint=smoothstep(-220.0,210.0,p.x)*.48+hue*.52;
          vec3 color=mix(teal,blue,smoothstep(.15,.43,tint));
          color=mix(color,lilac,smoothstep(.43,.7,tint));
          color=mix(color,copper,smoothstep(.72,.93,tint)*.5);
          float birth=exp(-dot((p-vec3(-90.0,-10.0,-80.0))/vec3(75.0,58.0,90.0),(p-vec3(-90.0,-10.0,-80.0))/vec3(75.0,58.0,90.0)));
          color=mix(color,copper,birth*.65);
          color*=.25+edge*2.05;
          return vec4(color,density);
        }
        void main(){
          vec4 view=uInverseProjection*vec4(vUv*2.0-1.0,1.0,1.0);
          vec3 direction=normalize(mat3(uWorldInverse)*mat3(uCameraWorld)*normalize(view.xyz/view.w));
          vec3 origin=(uWorldInverse*uCameraWorld*vec4(0,0,0,1)).xyz;
          vec2 bounds=boxIntersection(origin,direction);
          vec3 sky=vec3(.0007,.0012,.0035);
          if(bounds.y<=max(bounds.x,0.0)){gl_FragColor=vec4(sky,1);return;}
          float start=max(bounds.x,0.0),end=min(bounds.y,1400.0);
          float stepSize=(end-start)/float(uSteps);
          float distance=start+stepSize*hash21(gl_FragCoord.xy);
          float transmittance=1.0;vec3 light=vec3(0.0);
          for(int i=0;i<64;i++){
            if(i>=uSteps||transmittance<.025)break;
            vec4 fog=medium(origin+direction*distance);
            float alpha=1.0-exp(-fog.a*stepSize);
            light+=transmittance*fog.rgb*alpha;
            transmittance*=1.0-alpha;
            distance+=stepSize;
          }
          gl_FragColor=vec4(sky+light,1.0);
        }`
    })
    this.mesh=new THREE.Mesh(new THREE.PlaneGeometry(2,2),material)
    this.mesh.frustumCulled=false
    this.scene.add(this.mesh)
  }
  resize(width:number,height:number,low:boolean){
    const scale=low ? .4 : .6
    this.width=Math.max(1,Math.ceil(width*scale));this.height=Math.max(1,Math.ceil(height*scale))
    this.target.setSize(this.width,this.height)
    this.mesh.material.uniforms.uSteps.value=low?32:48
    this.dirty=true
  }
  render(camera:THREE.PerspectiveCamera,world:THREE.Matrix4,time:number,cameraMoving:boolean){
    camera.updateMatrixWorld(true)
    const now=performance.now()
    if(!this.dirty){
      if(time===this.lastTime&&this.lastCamera.equals(camera.matrixWorld)&&this.lastWorld.equals(world))return
      // The gas drifts subpixel distances between updates; camera gestures always get a fresh volume.
      if(!cameraMoving&&now-this.lastUpdate<66)return
    }
    const uniforms=this.mesh.material.uniforms
    uniforms.uTime.value=time
    uniforms.uInverseProjection.value.copy(camera.projectionMatrixInverse)
    uniforms.uCameraWorld.value.copy(camera.matrixWorld)
    uniforms.uWorldInverse.value.copy(world).invert()
    const previous=this.renderer.getRenderTarget()
    this.renderer.setRenderTarget(this.target)
    this.renderer.render(this.scene,this.camera)
    this.renderer.setRenderTarget(previous)
    this.lastCamera.copy(camera.matrixWorld);this.lastWorld.copy(world)
    this.lastTime=time;this.lastUpdate=now;this.dirty=false
  }
  dispose(){this.noise.dispose();this.mesh.geometry.dispose();this.mesh.material.dispose();this.target.dispose()}
}

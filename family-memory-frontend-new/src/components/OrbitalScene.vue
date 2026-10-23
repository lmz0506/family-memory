<script setup lang="ts">
import {onBeforeUnmount,onMounted,ref} from 'vue'
import {Pause,Play} from 'lucide-vue-next'
import StarChart from './StarChart.vue'
const props=withDefaults(defineProps<{variant?:'memory'|'capsule'}>(),{variant:'memory'})
const host=ref<HTMLElement>(),ready=ref(false),paused=ref(false),reduced=ref(false)
let cleanup=()=>{},sync=()=>{}
function toggle(){paused.value=!paused.value;sync()}
onMounted(async()=>{
  let destroyed=false
  if(!host.value)return
  const pendingHost=host.value
  let pendingSize:ResizeObserver|undefined,release=()=>{}
  // A mobile layout can hide the memory exhibit. Load WebGL only when it has a size.
  const sized=new Promise<void>(resolve=>{
    release=resolve
    if(pendingHost.clientWidth&&pendingHost.clientHeight)resolve()
    else{pendingSize=new ResizeObserver(()=>{if(pendingHost.clientWidth&&pendingHost.clientHeight){pendingSize?.disconnect();resolve()}});pendingSize.observe(pendingHost)}
  })
  cleanup=()=>{destroyed=true;pendingSize?.disconnect();release()}
  await sized
  if(destroyed)return
  const THREE=await import('three')
  if(destroyed||!host.value)return
  const container=host.value,scene=new THREE.Scene(),camera=new THREE.PerspectiveCamera(35,1,.1,40)
  camera.position.set(0,0,8.6)
  let renderer:InstanceType<typeof THREE.WebGLRenderer>
  try{renderer=new THREE.WebGLRenderer({alpha:true,antialias:true,powerPreference:'low-power'})}catch{return}
  renderer.setPixelRatio(Math.min(window.devicePixelRatio,1.5));renderer.setClearColor(0,0)
  renderer.domElement.setAttribute('aria-hidden','true');container.appendChild(renderer.domElement)
  const world=new THREE.Group();world.rotation.set(.28,0,-.34);scene.add(world)
  const palette=props.variant==='capsule'?[0xceb2e0,0xeacda4,0x91b9cf]:[0xe3c69d,0x9cb6d0,0xc4afd0]
  const geometries:InstanceType<typeof THREE.BufferGeometry>[]=[],materials:InstanceType<typeof THREE.Material>[]=[]
  const sprite=document.createElement('canvas');sprite.width=64;sprite.height=64
  const ctx=sprite.getContext('2d')!
  const gradient=ctx.createRadialGradient(32,32,0,32,32,32);gradient.addColorStop(0,'#fff8e7');gradient.addColorStop(.12,'#fff0cce0');gradient.addColorStop(.36,'#edbc7c60');gradient.addColorStop(1,'#edbc7c00');ctx.fillStyle=gradient;ctx.fillRect(0,0,64,64)
  const texture=new THREE.CanvasTexture(sprite)
  const movers:Array<{star:InstanceType<typeof THREE.Sprite>;radius:number;offset:number;speed:number;orbit:InstanceType<typeof THREE.Group>}>=[]
  for(let ring=0;ring<3;ring++){
    const radius=2.15+ring*.38,orbit=new THREE.Group();orbit.rotation.set(.68+ring*.31,ring*.44,ring*.37);world.add(orbit)
    const coords:number[]=[];for(let i=0;i<=240;i++){const a=i/240*Math.PI*2;coords.push(Math.cos(a)*radius,Math.sin(a)*radius,0)}
    const geometry=new THREE.BufferGeometry().setAttribute('position',new THREE.Float32BufferAttribute(coords,3));geometries.push(geometry)
    const material=new THREE.LineBasicMaterial({color:palette[ring],transparent:true,opacity:ring===0?.32:.15});materials.push(material)
    orbit.add(new THREE.LineLoop(geometry,material))
    for(let i=0;i<4;i++){
      const sm=new THREE.SpriteMaterial({map:texture,color:palette[ring],transparent:true,depthWrite:false,blending:THREE.AdditiveBlending});materials.push(sm)
      const star=new THREE.Sprite(sm);star.scale.setScalar(i===0?.26:.12);orbit.add(star)
      movers.push({star,radius,offset:i*Math.PI*.5+ring*.7,speed:(ring%2?-1:1)*(.1+ring*.025),orbit})
    }
  }
  const positions:number[]=[]
  for(let i=0;i<65;i++){const a=i*2.39996,r=1.7+(i%19)*.095;positions.push(Math.cos(a)*r,Math.sin(a)*r*.72,Math.sin(i*1.7)*1.5)}
  const dustGeo=new THREE.BufferGeometry().setAttribute('position',new THREE.Float32BufferAttribute(positions,3));geometries.push(dustGeo)
  const dustMat=new THREE.PointsMaterial({map:texture,color:0xe9d3b1,size:.07,transparent:true,opacity:.62,depthWrite:false,blending:THREE.AdditiveBlending});materials.push(dustMat)
  const dust=new THREE.Points(dustGeo,dustMat);world.add(dust)
  const timer=new THREE.Timer();timer.connect(document);let elapsed=0,visible=true,frameCount=0
  function render(advance=true){timer.update();if(advance)elapsed+=Math.min(timer.getDelta(),.05);world.rotation.y=Math.sin(elapsed*.075)*.16;world.rotation.z=-.34+Math.sin(elapsed*.09)*.06;dust.rotation.z=elapsed*.012;for(const m of movers){const a=m.offset+elapsed*m.speed;m.star.position.set(Math.cos(a)*m.radius,Math.sin(a)*m.radius,0)}renderer.render(scene,camera);frameCount++;renderer.domElement.dataset.frame=String(frameCount)}
  function resize(){const {width,height}=container.getBoundingClientRect();if(!width||!height)return;camera.aspect=width/height;camera.updateProjectionMatrix();world.scale.setScalar(Math.min(props.variant==='capsule'?1.35:1,camera.aspect*.95));renderer.setSize(width,height);render(false)}
  sync=()=>{renderer.setAnimationLoop(null);if(!paused.value&&!reduced.value&&visible&&!document.hidden)renderer.setAnimationLoop(()=>render());else render(false)}
  const media=window.matchMedia('(prefers-reduced-motion: reduce)')
  function preference(){reduced.value=media.matches;sync()}
  const resizeObserver=new ResizeObserver(resize);resizeObserver.observe(container)
  const intersection=new IntersectionObserver(entries=>{visible=Boolean(entries[0]?.isIntersecting);sync()});intersection.observe(container)
  media.addEventListener('change',preference);document.addEventListener('visibilitychange',sync)
  reduced.value=media.matches;resize();ready.value=true;sync()
  cleanup=()=>{destroyed=true;renderer.setAnimationLoop(null);resizeObserver.disconnect();intersection.disconnect();media.removeEventListener('change',preference);document.removeEventListener('visibilitychange',sync);timer.dispose();geometries.forEach(g=>g.dispose());materials.forEach(m=>m.dispose());texture.dispose();renderer.dispose();renderer.domElement.remove()}
})
onBeforeUnmount(()=>cleanup())
</script>
<template><div class="orbital-scene"><div ref="host" class="orbital-canvas" aria-hidden="true"></div><StarChart v-if="!ready" :variant="variant" class="orbital-fallback"/><button v-if="ready&&!reduced" class="orbit-motion-toggle" :aria-pressed="paused" @click="toggle"><Play v-if="paused" :size="11" aria-hidden="true"/><Pause v-else :size="11" aria-hidden="true"/>{{paused?'播放星轨':'暂停星轨'}}</button></div></template>
<style scoped>
.orbital-scene{position:relative;isolation:isolate;min-width:0;width:100%;height:100%;pointer-events:none}.orbital-canvas{position:absolute;inset:0}.orbital-canvas :deep(canvas){display:block;width:100%;height:100%}.orbital-fallback{position:absolute;inset:10%;width:80%;height:80%;opacity:.6}.orbit-motion-toggle{position:absolute;right:15px;bottom:12px;z-index:3;display:flex;gap:6px;align-items:center;padding:7px 10px;min-height:32px;color:#b3b5c5;font-size:10px;letter-spacing:1px;border:1px solid #cbb99229;border-radius:20px;background:#101927bb;pointer-events:auto}.orbit-motion-toggle:hover{color:#f0d6af;border-color:#cbb99277}.orbit-motion-toggle:focus-visible{outline:2px solid #e3c49b;outline-offset:3px}
</style>

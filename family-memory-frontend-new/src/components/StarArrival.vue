<script setup lang="ts">
import {onBeforeUnmount,onMounted,ref,watch} from 'vue'
import type {CreationReceipt} from '@/composables/creationReceipt'
import {flightPosition} from '@/composables/creationReceipt'
const props=defineProps<{receipt:CreationReceipt;target:{x:number;y:number}|null}>()
const emit=defineEmits<{landed:[];finished:[]}>()
const particle=ref<HTMLElement>(),fold=ref<HTMLElement>(),landed=ref(false)
let folded=false,flying=false
let animations:Animation[]=[],timer:ReturnType<typeof setTimeout>|undefined,stopped=false
function finish(){if(stopped)return;stopped=true;animations.forEach(a=>a.cancel());if(timer)clearTimeout(timer);emit('finished')}
function visibility(){if(document.hidden)finish()}
onMounted(async()=>{
  document.addEventListener('visibilitychange',visibility)
  if(window.matchMedia('(prefers-reduced-motion: reduce)').matches||document.hidden){finish();return}
  const start=props.receipt.origin||{x:window.innerWidth/2,y:window.innerHeight*.65}
  try{
    if(fold.value&&start.width&&start.height){
      const animation=fold.value.animate([{transform:'translate(-50%,-50%) scale(1)',opacity:1,borderRadius:'22px'},{transform:'translate(-50%,-50%) scale(.035)',opacity:.9,borderRadius:'80px'}],{duration:540,easing:'cubic-bezier(.65,0,.85,.4)',fill:'forwards'})
      animations.push(animation);await animation.finished
    }
    folded=true;void fly()
  }catch{/* Animation cancellation is handled by finish/unmount. */}
})
async function fly(){
  if(!folded||flying||stopped||!props.target)return
  flying=true
  if(fold.value)fold.value.style.visibility='hidden'
  const start=props.receipt.origin||{x:window.innerWidth/2,y:window.innerHeight*.65}
  try{
    if(stopped)return
    const frames=Array.from({length:41},(_,i)=>{const t=i/40,p=flightPosition(start,props.target!,t);return {transform:`translate(${p.x}px,${p.y}px) scale(${2-t})`,opacity:t<.06?t/.06:1,offset:t}})
    const animation=particle.value?.animate(frames,{duration:1250,easing:'cubic-bezier(.35,0,.35,1)',fill:'forwards'})
    if(!animation){finish();return}animations.push(animation);await animation.finished
    if(stopped)return;landed.value=true;if(particle.value){const fade=particle.value.animate([{opacity:1},{opacity:0}],{duration:180,fill:'forwards'});animations.push(fade)}emit('landed');timer=setTimeout(finish,1650)
  }catch{}
}
watch(()=>props.target,()=>{void fly()})
onBeforeUnmount(()=>{stopped=true;animations.forEach(a=>a.cancel());if(timer)clearTimeout(timer);document.removeEventListener('visibilitychange',visibility)})
</script>
<template>
  <Teleport to="body"><div class="star-arrival-layer" aria-hidden="true"><div v-if="receipt.origin?.width" ref="fold" class="arrival-editor-fold" :style="{left:`${receipt.origin.x}px`,top:`${receipt.origin.y}px`,width:`${receipt.origin.width}px`,height:`${receipt.origin.height}px`}"><span>✦</span><p>把这一刻，安放进星海</p></div><span ref="particle" class="arrival-particle" :class="{'arrival-capsule':receipt.sourceType==='CAPSULE'}"></span></div><button class="arrival-skip" @click="finish">跳过入星动画</button></Teleport>
</template>

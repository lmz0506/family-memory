<!-- Lucian UX/UI Prompt #228 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#228 -->
<script setup lang="ts">
import {onBeforeUnmount,onMounted,ref} from 'vue'
import '@/styles/lucian-228.css'
import {albumColumns} from '@/composables/albumLayout'
const root=ref<HTMLElement>()
let pointer={x:-500,y:-500},opacity=0,target=0,raf=0,last=0
function apply(){
  if(!root.value)return
  const rb=root.value.getBoundingClientRect(),k=root.value.offsetWidth/(rb.width||1)
  // Read every card first, then write, so the collection causes one layout pass.
  const cards=Array.from(root.value.querySelectorAll<HTMLElement>('.u228-c')).map(el=>({el,r:el.getBoundingClientRect()}))
  for(const {el,r} of cards){const x=pointer.x-(r.left-rb.left)*k,y=pointer.y-(r.top-rb.top)*k;el.style.setProperty('--cx',`${x.toFixed(1)}px`);el.style.setProperty('--cy',`${y.toFixed(1)}px`);el.style.setProperty('--o',opacity.toFixed(3));const inner=el.querySelector<HTMLElement>('.u228-in');inner?.style.setProperty('--cx',`${x-1.5}px`);inner?.style.setProperty('--cy',`${y-1.5}px`)}
}
function frame(now:number){raf=0;const dt=Math.min(.05,Math.max(0,(now-last)/1000));last=now;opacity+=(target-opacity)*Math.min(1,dt*(target?8:2.5));apply();if(Math.abs(target-opacity)>.005)raf=requestAnimationFrame(frame)}
function kick(){if(!raf){last=performance.now();raf=requestAnimationFrame(frame)}}
function move(e:PointerEvent){if(e.pointerType==='touch'||!root.value)return;const b=root.value.getBoundingClientRect(),k=root.value.offsetWidth/(b.width||1);pointer={x:(e.clientX-b.left)*k,y:(e.clientY-b.top)*k};target=1;if(window.matchMedia('(prefers-reduced-motion: reduce)').matches){opacity=1;apply()}else{apply();kick()}}
function leave(){target=0;if(window.matchMedia('(prefers-reduced-motion: reduce)').matches){opacity=0;apply()}else kick()}
let resize:ResizeObserver|undefined
onMounted(()=>{resize=new ResizeObserver(()=>{if(root.value)root.value.style.setProperty('--album-columns',String(albumColumns(root.value.clientWidth)))});if(root.value)resize.observe(root.value)})
onBeforeUnmount(()=>{cancelAnimationFrame(raf);resize?.disconnect()})
</script>
<template><div ref="root" class="ux u228 glow-collection" @pointermove="move" @pointerleave="leave"><TransitionGroup name="starlight" tag="div" class="u228-gd"><slot/></TransitionGroup></div></template>
<style>
.u228.glow-collection{width:100%;height:auto;max-width:100%;padding:0;overflow:visible;border-radius:0;background:none;box-shadow:none;--o:0}
.glow-collection .u228-gd{grid-template-columns:repeat(var(--album-columns,3),minmax(0,1fr));grid-template-rows:none;height:auto;gap:24px;align-items:stretch}
.glow-collection .u228-c{min-width:0;--cx:-500px;--cy:-500px;--o:0}
.glow-collection .u228-in{padding:0;gap:0;background:radial-gradient(220px circle at var(--cx) var(--cy),rgb(202 176 138 / calc(var(--o)*.14)),transparent 60%),#172131}
.glow-collection .u228-c{background:radial-gradient(160px circle at var(--cx) var(--cy),rgb(223 190 145 / var(--o)),rgb(133 168 201 / calc(var(--o)*.5)) 40%,transparent 70%),#b4bcd52b}
@media(max-width:580px){.glow-collection .u228-gd{gap:18px}}
/* Lucian #039 row motion, applied to the real filtered collection. */
.starlight-enter-active{transition:transform .55s var(--ux-spring),opacity .35s var(--ux-ease);transition-delay:var(--item-delay,0ms)}
.starlight-leave-active{transition:transform .55s var(--ux-spring),opacity .35s var(--ux-ease);position:absolute;pointer-events:none}
.starlight-enter-from{transform:translateX(70%);opacity:0}.starlight-leave-to{transform:translateX(-60%);opacity:0}.starlight-move{transition:transform .55s var(--ux-spring)}
</style>

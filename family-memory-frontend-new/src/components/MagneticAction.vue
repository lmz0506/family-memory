<!-- Lucian UX/UI Prompt #002 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#002 -->
<script setup lang="ts">
import {onBeforeUnmount,ref} from 'vue'
import '@/styles/lucian-002.css'
defineProps<{label:string}>()
const emit=defineEmits<{activate:[]}>()
const root=ref<HTMLElement>(),button=ref<HTMLButtonElement>(),state=ref('idle')
let timer:ReturnType<typeof setTimeout>|undefined
function move(event:PointerEvent){
  if(event.pointerType==='touch'||window.matchMedia('(prefers-reduced-motion: reduce)').matches||!button.value)return
  const r=button.value.getBoundingClientRect(),dx=event.clientX-(r.left+r.width/2),dy=event.clientY-(r.top+r.height/2),p=Math.max(0,1-Math.hypot(dx,dy)/240)
  const values=[['.u002-mag',.12,.22],['.u002-txt',.05,.08],['.u002-orb',.1,.14]] as const
  for(const [selector,x,y] of values){const el=root.value?.querySelector<HTMLElement>(selector);if(el)el.style.translate=`${(dx*x*p).toFixed(1)}px ${(dy*y*p).toFixed(1)}px`}
}
function reset(){root.value?.querySelectorAll<HTMLElement>('.u002-mag,.u002-txt,.u002-orb').forEach(el=>el.style.translate='0px 0px')}
function activate(){if(timer)clearTimeout(timer);state.value='go';emit('activate');timer=setTimeout(()=>state.value='idle',1900)}
onBeforeUnmount(()=>{if(timer)clearTimeout(timer)})
</script>
<template><div ref="root" class="ux u002 sky-magnetic" :data-state="state" @pointermove="move" @pointerleave="reset"><span class="u002-glow" aria-hidden="true"></span><div class="u002-mag"><button ref="button" class="u002-btn" type="button" @click="activate"><span class="u002-fill" aria-hidden="true"></span><span class="u002-txt"><span>{{label}}</span><span aria-hidden="true">{{label}}</span></span><span class="u002-orb" aria-hidden="true"><svg v-for="n in 2" :key="n" viewBox="0 0 24 24"><path d="M5 12h13M13 6l6 6-6 6"/></svg></span></button></div></div></template>
<style>
html:root .u002.sky-magnetic{max-width:100%;--bg:#eadcc5;--fg:#202633;--blob:linear-gradient(135deg,#b78755,#be9869 60%,#7e98ad)}
@media(prefers-reduced-motion:reduce){.u002.sky-magnetic :is(.u002-mag,.u002-txt,.u002-orb){translate:none!important}}
</style>

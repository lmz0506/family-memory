<script setup lang="ts">
import {computed,onMounted,onBeforeUnmount,ref} from 'vue'
import {gatherParticles,gatherDuration,type GatherTarget} from '@/composables/fragmentGather'
const props=defineProps<{id:number;x:number;y:number;width:number;height:number;color:string;targets:GatherTarget[]}>()
const emit=defineEmits<{finished:[id:number];measure:[id:number]}>()
const progress=ref(0),particles=computed(()=>gatherParticles({x:props.x,y:props.y},props.width,props.height,progress.value,props.targets))
let frame=0,start=0,measured=false,stopped=false
function tick(now:number){if(stopped)return;if(!start)start=now;progress.value=Math.min(1,(now-start)/gatherDuration);if(!measured&&progress.value>.38){measured=true;emit('measure',props.id)}if(progress.value<1)frame=requestAnimationFrame(tick);else emit('finished',props.id)}
onMounted(()=>{frame=requestAnimationFrame(tick)})
onBeforeUnmount(()=>{stopped=true;cancelAnimationFrame(frame)})
</script>
<template><svg class="fragment-gather" :width="width" :height="height" aria-hidden="true" :style="{'--gather-color':color}"><g v-for="(p,i) in particles" :key="i" :opacity="p.opacity"><path v-if="!p.wave && i%5===0" :d="`M${p.x-p.size*2},${p.y} h${p.size*4} M${p.x},${p.y-p.size*2} v${p.size*4}`" class="fragment-gather-spark"/><circle v-else :cx="p.x" :cy="p.y" :r="p.size" :class="{'fragment-gather-wave':p.wave}"/></g></svg></template>
<style scoped>
.fragment-gather{position:absolute;left:0;top:0;z-index:8;overflow:visible;pointer-events:none;fill:var(--gather-color);filter:drop-shadow(0 0 3px #f1c89955)}.fragment-gather-wave{fill:#ffe7c4}.fragment-gather-spark{fill:none;stroke:#ffe7c4;stroke-width:.8;stroke-linecap:round}
@media(prefers-reduced-motion:reduce){.fragment-gather{display:none}}
</style>

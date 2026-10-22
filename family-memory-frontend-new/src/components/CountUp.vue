<script setup lang="ts">
import {ref,watch,onBeforeUnmount} from 'vue'
const props=withDefaults(defineProps<{value:number;replay?:number}>(),{replay:0})
const displayed=ref(props.value)
let frame=0,start=0
function tick(now:number){if(!start)start=now;const t=Math.min(1,(now-start)/650);displayed.value=Math.round(props.value*(1-Math.pow(1-t,3)));if(t<1)frame=requestAnimationFrame(tick)}
watch(()=>props.replay,()=>{cancelAnimationFrame(frame);if(window.matchMedia('(prefers-reduced-motion: reduce)').matches||document.hidden){displayed.value=props.value;return}displayed.value=0;start=0;frame=requestAnimationFrame(tick)})
watch(()=>props.value,value=>displayed.value=value)
onBeforeUnmount(()=>cancelAnimationFrame(frame))
</script>
<template><span class="count-up"><span aria-hidden="true">{{displayed}}</span><span class="sr-only">{{value}}</span></span></template>
<style scoped>.count-up{font-variant-numeric:tabular-nums}</style>

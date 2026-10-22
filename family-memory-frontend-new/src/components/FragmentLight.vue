<script setup lang="ts">
import {onMounted,onBeforeUnmount,ref,computed} from 'vue'
import {fragmentMeteorGeometry,fragmentMeteorDuration,type LinkPoint} from '@/composables/fragmentLink'
const props=defineProps<{id:number;from:LinkPoint;to:LinkPoint;color:string}>()
const emit=defineEmits<{arrived:[id:number];travel:[id:number,x:number,y:number]}>()
const dissolving=ref(false),progress=ref(0),geometry=computed(()=>fragmentMeteorGeometry(props.from,props.to,progress.value))
const gradientId=`fragment-meteor-${props.id}`
let frame=0,disposed=false,start=0,lastTravel=-Infinity,finishTimer:ReturnType<typeof setTimeout>|undefined
function tick(now:number){
 if(disposed)return
 if(!start)start=now
 const t=Math.min(1,(now-start)/fragmentMeteorDuration)
 progress.value=t*t*(3-2*t)
 if(now-lastTravel>=55&&t<.98){lastTravel=now;const p=geometry.value.head;emit('travel',props.id,p.x,p.y)}
 if(t<1)frame=requestAnimationFrame(tick)
 else{dissolving.value=true;finishTimer=setTimeout(()=>{if(!disposed)emit('arrived',props.id)},160)}
}
onMounted(()=>{frame=requestAnimationFrame(tick)})
onBeforeUnmount(()=>{disposed=true;cancelAnimationFrame(frame);if(finishTimer)clearTimeout(finishTimer)})
</script>
<template><svg class="fragment-meteor-layer" aria-hidden="true" :style="{'--link-color':color}"><defs><linearGradient :id="gradientId" gradientUnits="userSpaceOnUse" :x1="geometry.tail[0]?.x" :y1="geometry.tail[0]?.y" :x2="geometry.head.x" :y2="geometry.head.y"><stop offset="0" :stop-color="color" stop-opacity="0"/><stop offset=".55" :stop-color="color" stop-opacity=".45"/><stop offset="1" stop-color="#fff0d5"/></linearGradient></defs><g :opacity="geometry.opacity"><polyline class="fragment-meteor-haze" :points="geometry.points" :stroke="`url(#${gradientId})`"/><polyline class="fragment-meteor-tail" :points="geometry.points" :stroke="`url(#${gradientId})`"/></g><g :opacity="Math.min(1,progress/.08)" :class="{'fragment-meteor-dissolve':dissolving}" :style="{transformOrigin:`${to.x}px ${to.y}px`}"><circle class="fragment-meteor-halo" :cx="geometry.head.x" :cy="geometry.head.y" r="9"/><circle class="fragment-meteor-head" :cx="geometry.head.x" :cy="geometry.head.y" r="3.5"/></g></svg></template>

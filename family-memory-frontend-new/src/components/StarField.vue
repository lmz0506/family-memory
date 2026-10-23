<script setup lang="ts">
// Fixed coordinates keep the sky stable across routes and avoid a render loop.
const stars=Array.from({length:74},(_,i)=>({x:(i*37.73+3)%100,y:(i*23.17+7)%100,size:i%11===0?2.4:i%3===0?1.6:1,opacity:.18+(i%5)*.12}))
</script>
<template><div class="reading-starfield" aria-hidden="true"><i v-for="(star,i) in stars" :key="i" :style="{left:`${star.x}%`,top:`${star.y}%`,width:`${star.size}px`,height:`${star.size}px`,opacity:star.opacity}"></i><div class="reading-nebula"></div></div></template>
<style scoped>
.reading-starfield{position:absolute;inset:0;overflow:hidden;pointer-events:none;background:radial-gradient(ellipse at 78% 24%,#6b58752b,transparent 48%),radial-gradient(ellipse at 20% 72%,#334e6e25,transparent 55%),#0e1422}.reading-starfield i{position:absolute;border-radius:50%;background:#f4dfc4;box-shadow:0 0 6px #efcf9d55}.reading-nebula{position:absolute;inset:0;background:linear-gradient(118deg,transparent 24%,#9c7da908 45%,#a192be0c 49%,transparent 70%)}
</style>

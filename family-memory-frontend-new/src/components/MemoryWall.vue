<script setup lang="ts">
import {onMounted,onBeforeUnmount,ref} from 'vue'
import {albumColumns} from '@/composables/albumLayout'
const wall=ref<HTMLElement>()
let observer:ResizeObserver|undefined
function layout(){if(wall.value)wall.value.style.setProperty('--album-columns',String(albumColumns(wall.value.clientWidth)))}
onMounted(()=>{if(typeof ResizeObserver!=='undefined'){observer=new ResizeObserver(layout);if(wall.value)observer.observe(wall.value)}layout()})
onBeforeUnmount(()=>observer?.disconnect())
</script>
<template><div ref="wall" class="memory-wall memory-modules"><slot/></div></template>
<style scoped>
.memory-wall{display:grid;grid-template-columns:repeat(var(--album-columns,1),minmax(0,1fr));gap:24px;align-items:stretch;min-width:0}.memory-wall :deep(>*){min-width:0;width:100%;height:100%}
@media(min-width:768px){.memory-wall{--album-columns:2}}@media(min-width:1100px){.memory-wall{--album-columns:3}}@media(min-width:1400px){.memory-wall{--album-columns:4}}@media(min-width:1680px){.memory-wall{--album-columns:5}}@media(min-width:1920px){.memory-wall{--album-columns:6}}
</style>

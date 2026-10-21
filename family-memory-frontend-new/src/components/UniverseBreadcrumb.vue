<!-- Lucian UX/UI Prompt #103 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#103 -->
<script setup lang="ts">
import {Home} from 'lucide-vue-next'
import type {UniverseLocation} from '@/three/memoryUniverse'
defineProps<{path:Array<{label:string; location:UniverseLocation}>}>()
const emit=defineEmits<{navigate:[location:UniverseLocation]}>()
</script>
<template>
  <div class="ux u103" :class="{'deep-path':path.length>3}">
    <nav aria-label="星海位置"><template v-for="(part,index) in path" :key="part.location.level">
      <button type="button" :data-level="part.location.level" :aria-label="index===0?'返回宇宙全景':part.label" :aria-current="index===path.length-1?'page':undefined" @click="emit('navigate',part.location)"><Home v-if="index===0" :size="15"/><span :class="{'root-label':index===0}">{{part.label}}</span></button>
      <span v-if="index<path.length-1" aria-hidden="true" :data-after="part.location.level">/</span>
    </template></nav>
  </div>
</template>
<style scoped>
.u103{width:100%;max-width:380px;min-width:0;font-size:13px;padding:16px 0}
.u103 button{font:inherit;color:inherit;cursor:pointer;border:0;background:transparent;min-height:44px;padding:8px 10px;border-radius:5px;display:flex;align-items:center;gap:6px}
.u103 nav{display:flex;align-items:center;flex-wrap:wrap;gap:3px;border-bottom:1px solid var(--ux-line-2);padding-bottom:10px}
.u103 nav button[aria-current=page]{font-weight:700;color:var(--ux-accent)}.u103 nav>span{color:var(--ux-ink-3)}
@media(max-width:639px){.u103{padding:0;flex:1}.u103 nav{overflow-x:auto;scrollbar-width:none;flex-wrap:nowrap;padding-bottom:0;border-bottom:0;gap:1px}.u103 button{padding:8px;flex-shrink:0}.u103 .root-label{display:none}}
</style>

<script setup lang="ts">
import {RefreshCw,Sparkles} from 'lucide-vue-next'
defineEmits<{retry:[]}>()
</script>
<template>
  <div class="memory-media-placeholder" role="group" aria-label="影像暂未显现">
    <span class="memory-placeholder-star" aria-hidden="true"><Sparkles :size="24"/></span>
    <span class="memory-placeholder-caption">影像暂未显现</span>
    <button type="button" class="memory-placeholder-retry" aria-label="重新加载这段记忆的影像" title="重新加载影像" @click="$emit('retry')"><RefreshCw :size="14"/></button>
  </div>
</template>
<style scoped>
.memory-media-placeholder{position:absolute;inset:0;overflow:hidden;background-color:#201c30;background-image:radial-gradient(circle,#d4ad6a28 .7px,transparent 1px),radial-gradient(ellipse at 65% 15%,#372344b3,transparent 75%);background-size:18px 18px,100% 100%;color:#c7bad2}
.memory-placeholder-star{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);display:grid;place-items:center;width:44px;height:44px;border:1px solid #d1adc62b;border-radius:50%;background:#e2b87309;color:#d8b979;box-shadow:0 0 25px #d2a4670b}
.memory-placeholder-caption{position:absolute;top:calc(50% + 31px);left:38px;right:38px;text-align:center;font-size:11px;line-height:1.5;color:#bdb0ca}
.memory-placeholder-retry{position:absolute;right:5px;bottom:5px;display:grid;place-items:center;width:32px;height:32px;border:0;border-radius:8px;color:#a99ab8;background:transparent;transition:color .2s,background .2s}.memory-placeholder-retry:hover{color:#e2c79b;background:#e2b8730c}.memory-placeholder-retry:focus-visible{outline:1px solid #d8b979;outline-offset:-2px}
@media(max-width:767px){.memory-placeholder-retry{width:44px;height:44px;right:0;bottom:0}}
@media(prefers-reduced-motion:reduce){.memory-placeholder-retry{transition:none}}
</style>

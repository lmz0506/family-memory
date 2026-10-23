<!-- Lucian UX/UI Prompt #258 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#258 -->
<script setup lang="ts">
import {ref, watch} from 'vue'
import {ImageOff, RotateCcw} from 'lucide-vue-next'
const props = withDefaults(defineProps<{src: string; alt: string; fit?: 'cover' | 'contain'; eager?: boolean;compact?:boolean;width?:number;height?:number}>(), {fit: 'cover', eager: false,compact:false})
const emit=defineEmits<{dimensions:[ratio:number];failed:[source:string]}>()
function onError(){failed.value=true;emit('failed',props.src)}
function onLoad(event:Event){loaded.value=true;const img=event.target as HTMLImageElement;if(img.naturalHeight)emit('dimensions',img.naturalWidth/img.naturalHeight)}
const loaded = ref(false)
const failed = ref(false)
const attempt = ref(0)
watch(() => props.src, () => {loaded.value = false; failed.value = false; attempt.value = 0})
function retry() { loaded.value = false; failed.value = false; attempt.value++ }
</script>
<template>
  <span class="ux u258-tl" :class="{'is-lq': !loaded && !failed, 'is-failed':failed,'is-compact':compact}" :aria-busy="!loaded && !failed">
    <img v-if="src && !failed" :key="`${src}-${attempt}`" :src="src" :alt="alt" :loading="eager ? 'eager' : 'lazy'" :fetchpriority="eager?'high':undefined" :width="width" :height="height" decoding="async"
         :style="{objectFit:fit}" @load="onLoad" @error="onError"/>
    <span v-if="failed || !src" class="u258-fallback" role="img" :aria-label="`${alt}：照片暂时无法加载`"><ImageOff :size="compact?18:28"/><span v-if="!compact">影像暂未显现</span><small v-if="!compact">可以稍后重试</small></span>
    <button v-if="failed" type="button" class="u258-retry" aria-label="重新加载照片" @click.stop="retry"><RotateCcw :size="14"/><span v-if="!compact">重试</span></button>
    <span v-if="!loaded && !failed" class="sr-only">正在加载照片</span>
  </span>
</template>
<style scoped>
.u258-tl{position:relative;display:block;width:100%;height:100%;overflow:hidden;background:var(--ux-surface-2)}
.u258-tl img{display:block;width:100%;height:100%;transform:scale(1);filter:none;transition:opacity .3s ease,transform .3s ease;opacity:1}
.u258-tl.is-lq img{filter:none;transform:scale(1.02);opacity:0}
.u258-tl::after{content:"";position:absolute;inset:0;background:linear-gradient(105deg,transparent 35%,rgba(220,200,170,.1) 50%,transparent 65%);background-size:250% 100%;opacity:0;pointer-events:none}
.u258-tl.is-lq::after{opacity:1;animation:u258-sh 1.4s linear infinite}
@keyframes u258-sh{from{background-position:130% 0}to{background-position:-30% 0}}
.u258-fallback{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;background:radial-gradient(ellipse at 70% 20%,#e2b8730c,transparent 70%),repeating-linear-gradient(135deg,#ffffff02 0px,#ffffff02 1px,transparent 1px,transparent 9px),#1a1c20;color:var(--ux-ink-2);font-size:14px}
.u258-fallback small{font-size:12px;color:var(--ux-ink-3)}
.u258-retry{position:absolute;z-index:20;top:50%;left:50%;margin-top:64px;transform:translateX(-50%);display:flex;align-items:center;gap:5px;padding:5px 12px;border:1px solid var(--ux-line);border-radius:999px;background:var(--ux-surface);color:var(--ux-ink-2);font-size:12px}
.is-compact .u258-retry{top:auto;bottom:7px;left:auto;right:7px;margin:0;transform:none;min-width:30px;min-height:30px;padding:7px;border-radius:8px}.is-compact .u258-fallback{gap:0}
</style>

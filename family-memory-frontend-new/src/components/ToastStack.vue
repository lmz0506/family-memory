<!-- Lucian UX/UI Prompt #070 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#070 -->
<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {Check, Info, TriangleAlert, X} from 'lucide-vue-next'
import {toasts, dismissToast} from '@/composables/useToast'
const expanded = ref(false)
let tick: ReturnType<typeof setInterval> | undefined
let dragX = 0
onMounted(() => {
  tick = setInterval(() => {
    if (expanded.value || document.hidden) return
    const front = toasts.value[0]
    if (front) {front.remaining -= 100; if (front.remaining <= 0) dismissToast(front.id)}
  },100)
})
onBeforeUnmount(() => clearInterval(tick))
</script>
<template>
  <Teleport to="body">
    <div class="ux u070-zone" role="region" aria-label="操作提示" @pointerenter="expanded=true" @pointerleave="expanded=false" @focusin="expanded=true" @focusout="expanded=false">
      <TransitionGroup name="toast">
        <div v-for="(toast,index) in toasts" :key="toast.id" class="u070-t" :class="{back:!expanded && index>0}"
             :role="toast.kind==='error'?'alert':'status'" :inert="!expanded && index>0"
             :style="{'--y':`${-index*(expanded?84:14)}px`,'--s':expanded?1:1-index*.06,zIndex:50-index,'--c':toast.kind==='error'?'var(--ux-danger)':toast.kind==='success'?'var(--ux-mint)':'var(--ux-accent)'}"
             @pointerdown="dragX=$event.clientX" @pointerup="Math.abs($event.clientX-dragX)>90 && dismissToast(toast.id)">
          <span class="u070-ic"><component :is="toast.kind==='error'?TriangleAlert:toast.kind==='success'?Check:Info" :size="18"/></span>
          <span class="u070-tx"><b>{{ toast.message }}</b></span>
          <button type="button" class="u070-x" aria-label="关闭提示" @click="dismissToast(toast.id)"><X :size="16"/></button>
          <span class="u070-bar" :style="{transform:`scaleX(${toast.remaining/4500})`}" aria-hidden="true"></span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
<style scoped>
.u070-zone{position:fixed;z-index:200;right:24px;bottom:24px;width:350px;max-width:calc(100vw - 32px);pointer-events:none}
.u070-t{position:absolute;left:0;right:0;bottom:0;display:flex;align-items:center;gap:12px;min-height:68px;padding:12px 12px 12px 14px;border-radius:20px;background:var(--ux-surface);box-shadow:0 0 0 1px var(--ux-line),0 16px 30px -18px var(--ux-shadow-strong);transform-origin:50% 100%;transform:translateY(var(--y,0px)) scale(var(--s,1));transition:transform .5s var(--ux-spring),opacity .35s;touch-action:pan-y;pointer-events:auto}
.u070-t>*{transition:opacity .25s}.u070-t.back>*{opacity:0}
.u070-ic{width:36px;height:36px;flex:none;border-radius:12px;display:grid;place-items:center;background:color-mix(in srgb,var(--c) 15%,transparent);color:var(--c)}
.u070-tx{flex:1;min-width:0}.u070-tx b{display:block;font-size:14px;font-weight:780;letter-spacing:-.02em;overflow-wrap:anywhere}
.u070-x{width:30px;height:30px;flex:none;border:0;padding:0;border-radius:50%;display:grid;place-items:center;background:none;color:var(--ux-ink-3)}
.u070-x:hover{background:var(--ux-surface-2);color:var(--ux-ink)}
.u070-bar{position:absolute;left:16px;right:16px;bottom:0;height:2px;border-radius:2px;background:var(--c);opacity:.5;transform-origin:0 50%}
.toast-enter-from{transform:translateY(90px) scale(.9);opacity:0}.toast-leave-to{opacity:0;transform:translateX(100%)}.toast-leave-active{position:absolute;pointer-events:none}
@media(max-width:1023px){.u070-zone{right:16px;bottom:24px}}
</style>

<!-- Lucian UX/UI Prompt #039 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#039 -->
<script setup lang="ts">
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
const props = defineProps<{modelValue: string; options: Array<{value: string; label: string}>; label: string}>()
const emit = defineEmits<{'update:modelValue': [value: string]}>()
const root = ref<HTMLElement>()
const pill = ref({transform: 'translateX(5px)', width: '0px'})
let observer: ResizeObserver | undefined
function measure() {
  const active = root.value?.querySelector<HTMLElement>('[aria-pressed="true"]')
  if (active) pill.value = {transform: `translateX(${active.offsetLeft}px)`, width: `${active.offsetWidth}px`}
}
function navigate(event: KeyboardEvent, index: number) {
  const delta = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0
  if (!delta && event.key !== 'Home' && event.key !== 'End') return
  event.preventDefault()
  const target = event.key === 'Home' ? 0 : event.key === 'End' ? props.options.length - 1 : (index + delta + props.options.length) % props.options.length
  emit('update:modelValue', props.options[target]!.value)
  root.value?.querySelectorAll('button')[target]?.focus()
}
watch(() => [props.modelValue, props.options], async () => { await nextTick(); measure() }, {deep: true})
onMounted(() => { observer = new ResizeObserver(measure); if (root.value) observer.observe(root.value); measure() })
onBeforeUnmount(() => observer?.disconnect())
</script>
<template>
  <div ref="root" class="ux u039-tabs" role="group" :aria-label="label">
    <span class="u039-pill" :style="pill" aria-hidden="true"></span>
    <button v-for="(option,index) in options" :key="option.value" type="button" class="u039-tab"
            :aria-pressed="modelValue===option.value" :tabindex="modelValue===option.value ? 0 : -1"
            @click="emit('update:modelValue',option.value)" @keydown="navigate($event,index)">{{ option.label }}</button>
  </div>
</template>
<style scoped>
.u039-tabs{position:relative;display:flex;padding:5px;border-radius:999px;background:var(--ux-surface);box-shadow:0 0 0 1px var(--ux-line);max-width:100%;isolation:isolate}
.u039-pill{position:absolute;left:0;top:5px;bottom:5px;border-radius:999px;background:var(--ux-accent-soft);box-shadow:0 8px 16px -10px var(--ux-shadow-strong);transition:transform .5s var(--ux-spring),width .5s var(--ux-spring);pointer-events:none}
.u039-tab{position:relative;flex:1;min-width:0;height:42px;border:0;padding:0 16px;border-radius:999px;background:none;font-size:14px;font-weight:450;color:var(--ux-ink-2);white-space:nowrap;transition:color .35s var(--ux-ease)}
.u039-tab[aria-pressed="true"]{color:var(--ux-accent)}
.u039-tab:focus-visible{outline:2px solid var(--ux-accent);outline-offset:2px}
</style>

<style>
/* Shared reveal: one entrance per item, never a loop. */
.will-reveal{opacity:0;transform:translateY(22px);transition:transform .8s cubic-bezier(.2,.8,.2,1),opacity .65s cubic-bezier(.2,.8,.2,1);transition-delay:var(--item-delay,0ms)}
.will-reveal.is-seen{opacity:1;transform:none}
@media(prefers-reduced-motion:reduce){.will-reveal{opacity:1;transform:none;transition:none}}
</style>

<script setup lang="ts">
import {Images, Orbit} from 'lucide-vue-next'
import {nextTick} from 'vue'
const props=withDefaults(defineProps<{modelValue:string;theme?:'dark'|'light'}>(),{theme:'light'})
const emit=defineEmits<{'update:modelValue':[value:string]}>()
const options=[{value:'stars',label:'星海',icon:Orbit},{value:'classic',label:'相册',icon:Images}]
async function navigate(event:KeyboardEvent,index:number){
  if(!['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return
  event.preventDefault()
  const next=event.key==='Home'?0:event.key==='End'?1:1-index
  emit('update:modelValue',options[next]!.value)
  await nextTick()
  document.querySelector<HTMLButtonElement>('.timeline-mode-switch button[aria-pressed="true"]')?.focus()
}
</script>
<template>
  <div class="timeline-mode-switch" :data-theme="props.theme" role="group" aria-label="时光轴浏览方式">
    <button v-for="(option,index) in options" :key="option.value" :aria-pressed="modelValue===option.value" :tabindex="modelValue===option.value?0:-1" @click="emit('update:modelValue',option.value)" @keydown="navigate($event,index)">
      <component :is="option.icon" :size="14" aria-hidden="true"/><span>{{option.label}}</span>
    </button>
  </div>
</template>
<style scoped>
.timeline-mode-switch{display:flex;gap:3px;padding:3px;border-radius:11px;background:#ede7df;color:#766b60}
button{display:flex;align-items:center;justify-content:center;gap:6px;min-width:72px;height:33px;padding:0 12px;border:1px solid transparent;border-radius:8px;font-size:12px;font-weight:550;transition:background .18s,color .18s,border-color .18s}
button[aria-pressed=true]{background:#fff;color:#46382b;border-color:#d8cabb;box-shadow:0 1px 4px #49321d0a}
button:focus-visible{outline:2px solid #c79b64;outline-offset:2px}
[data-theme=dark]{background:transparent;color:#a5b5cd}
[data-theme=dark] button[aria-pressed=true]{background:#24364d;color:#eef5ff;border-color:#92b4dd4d;box-shadow:none}
[data-theme=dark] button:hover{color:#fff;background:#25374a80}
@media(prefers-reduced-motion:reduce){button{transition:none}}
</style>

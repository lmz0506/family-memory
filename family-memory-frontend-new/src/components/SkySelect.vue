<!-- Lucian UX/UI Prompt #031 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#031 -->
<script setup lang="ts">
import {computed,nextTick,onMounted,onBeforeUnmount,ref,watch} from 'vue'
import {Check,ChevronDown} from 'lucide-vue-next'
const props=defineProps<{id:string;label:string;modelValue:string;options:{value:string;label:string}[]}>()
const emit=defineEmits<{'update:modelValue':[value:string]}>()
const root=ref<HTMLElement>(),trigger=ref<HTMLButtonElement>(),list=ref<HTMLElement>(),open=ref(false),active=ref(0)
const chosen=computed(()=>props.options.find(o=>o.value===props.modelValue)||props.options[0])
let typed='',typingTimer:ReturnType<typeof setTimeout>|undefined
async function show(){active.value=Math.max(0,props.options.findIndex(o=>o.value===props.modelValue));open.value=true;await nextTick();list.value?.focus({preventScroll:true});scrollActive()}
function close(restore=false){open.value=false;if(restore)trigger.value?.focus({preventScroll:true})}
function choose(index:number){const option=props.options[index];if(!option)return;emit('update:modelValue',option.value);close(true)}
function scrollActive(){list.value?.querySelector<HTMLElement>(`[data-option-index="${active.value}"]`)?.scrollIntoView({block:'nearest',inline:'nearest'})}
function keys(event:KeyboardEvent){
 if(['ArrowDown','ArrowUp','Home','End','Enter',' ','Escape'].includes(event.key)){event.preventDefault();event.stopPropagation()}
 if(event.key==='ArrowDown')active.value=Math.min(props.options.length-1,active.value+1)
 else if(event.key==='ArrowUp')active.value=Math.max(0,active.value-1)
 else if(event.key==='Home')active.value=0
 else if(event.key==='End')active.value=props.options.length-1
 else if(event.key==='Enter'||event.key===' ')choose(active.value)
 else if(event.key==='Escape')close(true)
 else if(event.key==='Tab')close()
 else if(event.key.length===1){typed+=event.key.toLowerCase();if(typingTimer)clearTimeout(typingTimer);typingTimer=setTimeout(()=>typed='',600);const hit=props.options.findIndex(o=>o.label.toLowerCase().startsWith(typed));if(hit>=0)active.value=hit}
 scrollActive()
}
function outside(event:PointerEvent){if(!root.value?.contains(event.target as Node))close()}
function blur(event:FocusEvent){if(!root.value?.contains(event.relatedTarget as Node|null))close()}
watch(()=>props.options,()=>{active.value=Math.max(0,Math.min(active.value,props.options.length-1))})
onMounted(()=>document.addEventListener('pointerdown',outside))
onBeforeUnmount(()=>{document.removeEventListener('pointerdown',outside);if(typingTimer)clearTimeout(typingTimer)})
</script>
<template><div ref="root" class="u031-f" :class="{'is-open':open}" @focusout="blur"><span :id="`${id}-label`" class="u031-lab">{{label}}</span><button ref="trigger" type="button" class="u031-btn" :aria-labelledby="`${id}-label ${id}-value`" aria-haspopup="listbox" :aria-expanded="open" :aria-controls="`${id}-list`" @click="open?close(true):show()" @keydown.down.prevent="show" @keydown.up.prevent="show"><span class="u031-v"><b :id="`${id}-value`">{{chosen?.label||'请选择'}}</b></span><ChevronDown :size="14" class="u031-car"/></button><ul :aria-hidden="!open" :id="`${id}-list`" ref="list" class="u031-list" :class="{'is-open':open}" role="listbox" tabindex="-1" :aria-label="label" :aria-activedescendant="`${id}-option-${active}`" @keydown="keys"><li class="u031-hl" role="presentation" aria-hidden="true" :style="{'--i':active}"></li><li v-for="(option,i) in options" :id="`${id}-option-${i}`" :key="option.value" :data-option-index="i" class="u031-o" role="option" :aria-selected="option.value===modelValue" @pointermove="active=i" @pointerdown.prevent @click="choose(i)">{{option.label}}<Check :size="16" class="u031-ck"/></li></ul></div></template>
<style scoped>
.u031-f{position:relative;min-width:0;--ux-surface:#24222d;--ux-surface-2:#edc69c12;--ux-line-2:#dac09c30;--ux-ink:#eddbc3;--ux-ink-3:#b8a38b;--ux-accent:#dfb98d;--ux-ease:cubic-bezier(.2,.7,.2,1);--ux-spring:cubic-bezier(.34,1.45,.64,1)}
.u031-lab{display:block;margin:0 2px 7px;font-size:11px;font-weight:400;color:var(--ux-ink-3)}
.u031-btn{display:flex;align-items:center;gap:12px;width:100%;height:40px;padding:0 12px;border:0;border-radius:10px;background:var(--ux-surface-2);color:var(--ux-ink);text-align:left;box-shadow:inset 0 0 0 1px #dac09c20;transition:box-shadow .25s var(--ux-ease),background-color .25s}
.u031-btn:hover{box-shadow:inset 0 0 0 1px var(--ux-line-2)}.u031-btn:focus-visible,.is-open .u031-btn{outline:0;background:#edc69c0d;box-shadow:inset 0 0 0 1px #dfb98d80}.u031-v{flex:1;min-width:0}.u031-v b{display:block;font-size:13px;font-weight:400;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.u031-car{color:var(--ux-ink-3);flex-shrink:0;transition:transform .45s var(--ux-spring)}.u031-btn[aria-expanded=true] .u031-car{transform:rotate(180deg)}
.u031-list{position:absolute;z-index:90;left:0;right:0;top:calc(100% + 8px);margin:0;padding:6px;list-style:none;border-radius:12px;background:var(--ux-surface);box-shadow:0 0 0 1px var(--ux-line-2),0 20px 35px -16px #000b;transform-origin:50% 0;outline:none;opacity:0;transform:translateY(-6px) scaleY(.92);visibility:hidden;pointer-events:none;transition:opacity .2s var(--ux-ease),transform .4s var(--ux-spring),visibility 0s .4s;max-height:220px;overflow-y:auto;scrollbar-width:none;overscroll-behavior:contain}.u031-list::-webkit-scrollbar{display:none}.u031-list.is-open{pointer-events:auto;opacity:1;transform:none;visibility:visible;transition:opacity .2s var(--ux-ease),transform .45s var(--ux-spring)}
.u031-hl{position:absolute;left:6px;right:6px;top:6px;height:46px;border-radius:8px;background:var(--ux-surface-2);transform:translateY(calc(var(--i,0)*46px));transition:transform .35s var(--ux-spring);pointer-events:none}.u031-o{position:relative;display:flex;align-items:center;gap:12px;height:46px;padding:0 10px;border-radius:8px;cursor:pointer;font-size:13px;font-weight:400;color:var(--ux-ink)}.u031-ck{margin-left:auto;color:var(--ux-accent);opacity:0;transform:scale(.4);transition:opacity .2s,transform .4s var(--ux-spring)}.u031-o[aria-selected=true] .u031-ck{opacity:1;transform:none}
@media(prefers-reduced-motion:reduce){.u031-btn,.u031-car,.u031-list,.u031-hl,.u031-ck{transition:none!important}}
</style>

<script setup lang="ts">
import {ref,toRef} from 'vue'
import {X} from 'lucide-vue-next'
import {useDialog} from '@/composables/useDialog'
const props=defineProps<{modelValue:boolean;title:string;description?:string}>()
const emit=defineEmits<{'update:modelValue':[value:boolean]}>()
const panel=ref<HTMLElement>()
function close(){emit('update:modelValue',false)}
useDialog(toRef(props,'modelValue'),panel,close)
</script>
<template><Teleport to="body"><Transition name="drawer"><div v-if="modelValue" class="sky-drawer-backdrop" @click.self="close"><section ref="panel" class="sky-drawer" role="dialog" aria-modal="true" :aria-label="title" tabindex="-1"><header><div><h2>{{title}}</h2><p v-if="description">{{description}}</p></div><button aria-label="关闭详情" @click="close"><X :size="20"/></button></header><div class="sky-drawer-content"><slot/></div></section></div></Transition></Teleport></template>
<style scoped>
.sky-drawer-backdrop{position:fixed;inset:0;z-index:100;background:#05060880;display:flex;justify-content:flex-end}.sky-drawer{width:min(460px,100%);height:100%;padding:28px;background:#1a1c20;color:var(--ux-ink);border-left:1px solid #e2b87330;outline:none;overflow-y:auto}.sky-drawer header{display:flex;justify-content:space-between;gap:16px;padding-bottom:22px;border-bottom:1px solid var(--ux-line)}.sky-drawer h2{font-size:23px;font-weight:500}.sky-drawer header p{font-size:13px;color:var(--ux-ink-2);margin-top:8px}.sky-drawer header button{width:44px;height:44px;display:grid;place-items:center;flex-shrink:0}.sky-drawer-content{padding-top:20px}.drawer-enter-active,.drawer-leave-active{transition:opacity .22s}.drawer-enter-active .sky-drawer,.drawer-leave-active .sky-drawer{transition:transform .26s ease}.drawer-enter-from,.drawer-leave-to{opacity:0}.drawer-enter-from .sky-drawer,.drawer-leave-to .sky-drawer{transform:translateX(40px)}
@media(max-width:639px){.sky-drawer-backdrop{align-items:flex-end}.sky-drawer{height:auto;max-height:88dvh;border-radius:20px 20px 0 0;padding:22px 20px calc(22px + env(safe-area-inset-bottom));border:1px solid #e2b87325}.drawer-enter-from .sky-drawer,.drawer-leave-to .sky-drawer{transform:translateY(30px)}}
@media(prefers-reduced-motion:reduce){.drawer-enter-active,.drawer-leave-active,.drawer-enter-active .sky-drawer,.drawer-leave-active .sky-drawer{transition:none}}
</style>

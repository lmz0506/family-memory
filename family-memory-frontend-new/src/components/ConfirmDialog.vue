<script setup lang="ts">
import {ref,toRef} from 'vue'
import {TriangleAlert} from 'lucide-vue-next'
import {useDialog} from '@/composables/useDialog'
const props=defineProps<{open:boolean; title:string; description:string; busy?:boolean}>()
const emit=defineEmits<{'update:open':[value:boolean]; confirm:[]}>()
const panel=ref<HTMLElement>()
function close(){if(!props.busy)emit('update:open',false)}
useDialog(toRef(props,'open'),panel,close)
</script>
<template><Teleport to="body"><Transition name="fade"><div v-if="open" class="fixed inset-0 z-[100] grid place-items-center bg-black/60 p-5 backdrop-blur-sm" @click.self="close"><section ref="panel" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-description" tabindex="-1" class="sky-dialog w-full max-w-sm rounded-2xl bg-night-surface p-6 shadow-soft-lg"><TriangleAlert :size="28" class="mb-4 text-night-danger"/><h2 id="confirm-title" class="text-lg font-bold">{{title}}</h2><p id="confirm-description" class="mt-3 text-sm leading-7 text-night-secondary">{{description}}</p><div class="mt-6 flex gap-3"><button autofocus :disabled="busy" @click="close" class="min-h-11 flex-1 rounded-full border border-night-line text-sm font-semibold">保留</button><button :disabled="busy" @click="emit('confirm')" class="min-h-11 flex-1 rounded-full bg-rose-600 text-sm font-semibold text-white disabled:opacity-50">{{busy?'正在删除…':'确认删除'}}</button></div></section></div></Transition></Teleport></template>

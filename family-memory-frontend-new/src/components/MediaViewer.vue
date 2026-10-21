<script setup lang="ts">
import {computed, ref, toRef} from 'vue'
import {ChevronLeft, ChevronRight, X} from 'lucide-vue-next'
import MemoryImage from './MemoryImage.vue'
import {useDialog} from '@/composables/useDialog'
const props = defineProps<{open: boolean; media: string[]; index: number; title: string}>()
const emit = defineEmits<{'update:open':[value:boolean]; 'update:index':[value:number]}>()
const panel = ref<HTMLElement>()
const url = computed(() => props.media[props.index] || '')
const isVideo = computed(() => /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url.value))
function close() {emit('update:open', false)}
function move(delta: number) {emit('update:index',(props.index+delta+props.media.length)%props.media.length)}
useDialog(toRef(props,'open'), panel, close)
</script>
<template>
  <Teleport to="body"><Transition name="fade">
    <div v-if="open" ref="panel" role="dialog" aria-modal="true" :aria-label="title" tabindex="-1" class="media-viewer" @click.self="close" @keydown.left.prevent="move(-1)" @keydown.right.prevent="move(1)">
      <div class="viewer-top"><span>{{ index+1 }} / {{ media.length }}</span><button aria-label="关闭照片浏览" @click="close"><X :size="22"/></button></div>
      <video v-if="isVideo" :key="url" :src="url" controls autoplay playsinline class="viewer-video"></video>
      <MemoryImage v-else :key="url" :src="url" :alt="title" fit="contain" eager class="viewer-image"/>
      <template v-if="media.length>1"><button class="viewer-prev" aria-label="上一张" @click="move(-1)"><ChevronLeft/></button><button class="viewer-next" aria-label="下一张" @click="move(1)"><ChevronRight/></button></template>
      <p class="viewer-caption">{{ title }}</p>
    </div>
  </Transition></Teleport>
</template>
<style scoped>
.media-viewer{position:fixed;inset:0;z-index:100;display:flex;align-items:center;justify-content:center;background:#111117f5;padding:80px 64px;color:white;outline:none}
.viewer-top{position:absolute;inset:16px 20px auto;display:flex;align-items:center;justify-content:space-between;font-size:14px}
.media-viewer button{display:grid;place-items:center;width:44px;height:44px;border-radius:50%;background:#ffffff1a;color:white;border:1px solid #ffffff20}
.viewer-image{height:100%;max-height:100%;background:transparent;max-width:1440px}.viewer-video{max-height:100%;max-width:100%}
.viewer-prev,.viewer-next{position:absolute;top:50%;transform:translateY(-50%)}.viewer-prev{left:12px}.viewer-next{right:12px}
.viewer-caption{position:absolute;bottom:24px;left:24px;right:24px;text-align:center;font-size:14px;color:#ffffffba}
@media(max-width:640px){.media-viewer{padding:72px 8px}.viewer-prev,.viewer-next{top:auto;bottom:16px;transform:none}.viewer-caption{left:64px;right:64px;bottom:18px;max-height:42px;overflow:hidden}}
</style>

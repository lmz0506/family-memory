<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {ChevronLeft, ChevronRight, Heart, Layers, MapPin, MessageCircle, Play, X} from 'lucide-vue-next'
import type {Memory} from '@/types'
import {memoryApi} from '@/api'

const props = defineProps<{ memory: Memory }>()
const emit = defineEmits<{ changed: [] }>()
const busy = ref(false)
const activeIndex = ref(0)
const viewerIndex = ref(0)
const viewerOpen = ref(false)
let timer: ReturnType<typeof setInterval> | undefined
let touchStartX = 0

const media = computed(() => Array.from(new Set(
    [...(props.memory.mediaUrls || []), props.memory.coverUrl].filter((url): url is string => Boolean(url))
)))
const category = computed(() => props.memory.tags?.[0] || (props.memory.type === 'photo' ? '照片' : props.memory.type === 'audio' ? '声音' : '日常'))
const relativeTime = computed(() => {
  const value = new Date(props.memory.createdAt).getTime()
  if (Number.isNaN(value)) return props.memory.memoryDate
  const days = Math.max(0, Math.floor((Date.now() - value) / 86400000))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return props.memory.memoryDate
})

function isVideo(url: string) {
  return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url)
}
function stopAutoplay() { if (timer) clearInterval(timer); timer = undefined }
function startAutoplay() {
  stopAutoplay()
  if (media.value.length > 1 && !viewerOpen.value) timer = setInterval(() => goTo(activeIndex.value + 1, false), 5000)
}
function goTo(index: number, restart = true) {
  if (!media.value.length) return
  activeIndex.value = (index + media.value.length) % media.value.length
  if (restart) startAutoplay()
}
function onTouchStart(event: TouchEvent) { touchStartX = event.touches[0]?.clientX || 0; stopAutoplay() }
function onTouchEnd(event: TouchEvent) {
  const distance = (event.changedTouches[0]?.clientX || 0) - touchStartX
  if (Math.abs(distance) > 40) goTo(activeIndex.value + (distance < 0 ? 1 : -1)); else startAutoplay()
}
function openViewer() { viewerIndex.value = activeIndex.value; viewerOpen.value = true; stopAutoplay(); document.body.style.overflow = 'hidden' }
function closeViewer() { viewerOpen.value = false; document.body.style.overflow = ''; startAutoplay() }
function moveViewer(direction: number) { viewerIndex.value = (viewerIndex.value + direction + media.value.length) % media.value.length }
function handleKeydown(event: KeyboardEvent) {
  if (!viewerOpen.value) return
  if (event.key === 'Escape') closeViewer()
  if (event.key === 'ArrowLeft' && media.value.length > 1) moveViewer(-1)
  if (event.key === 'ArrowRight' && media.value.length > 1) moveViewer(1)
}
async function like(event: Event) {
  event.preventDefault(); event.stopPropagation()
  if (busy.value) return
  busy.value = true
  try {
    const state = await memoryApi.like(props.memory.id)
    props.memory.liked = state.liked; props.memory.likesCount = state.likesCount; emit('changed')
  } finally { busy.value = false }
}
onMounted(() => { startAutoplay(); document.addEventListener('keydown', handleKeydown) })
onBeforeUnmount(() => { stopAutoplay(); document.removeEventListener('keydown', handleKeydown); if (viewerOpen.value) document.body.style.overflow = '' })
</script>

<template>
  <article class="group flex cursor-pointer flex-col overflow-hidden rounded-3xl border border-warm-100 bg-white shadow-soft transition-all duration-300 hover:shadow-soft-lg">
    <div v-if="media.length" class="relative aspect-[4/3] overflow-hidden bg-warm-100" @mouseenter="stopAutoplay" @mouseleave="startAutoplay" @touchstart.passive="onTouchStart" @touchend.passive="onTouchEnd">
      <Transition name="media-fade" mode="out-in">
        <button :key="media[activeIndex]" type="button" class="block h-full w-full" @click="openViewer">
          <video v-if="isVideo(media[activeIndex])" :src="media[activeIndex]" muted playsinline preload="metadata" class="h-full w-full object-cover"></video>
          <img v-else :src="media[activeIndex]" :alt="memory.title" class="h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-105"/>
          <span v-if="isVideo(media[activeIndex])" class="absolute left-1/2 top-1/2 grid h-12 w-12 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-black/40 text-white backdrop-blur"><Play :size="20" fill="currentColor"/></span>
        </button>
      </Transition>

      <div class="pointer-events-none absolute left-3 top-3 flex items-center gap-2">
        <span class="flex items-center gap-1 rounded-full bg-black/40 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur-md"><Layers :size="12"/>{{ media.length }}个媒体</span>
      </div>
      <div class="pointer-events-none absolute right-3 top-3"><span class="rounded-full bg-brand-coral/90 px-2.5 py-1 text-[11px] font-semibold text-white backdrop-blur-sm">{{ category }}</span></div>
      <div class="pointer-events-none absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-black/50 to-transparent"></div>
      <div class="pointer-events-none absolute bottom-3 left-3 flex items-center gap-1.5 text-xs font-medium text-white"><MapPin :size="14" class="text-brand-peach"/><span>{{ memory.location || '家庭记忆' }} · {{ memory.memoryDate }}</span></div>

      <template v-if="media.length > 1">
        <button @click.stop="goTo(activeIndex-1)" class="absolute left-3 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-full bg-white/80 text-warm-800 opacity-0 shadow transition group-hover:opacity-100"><ChevronLeft :size="16"/></button>
        <button @click.stop="goTo(activeIndex+1)" class="absolute right-3 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-full bg-white/80 text-warm-800 opacity-0 shadow transition group-hover:opacity-100"><ChevronRight :size="16"/></button>
      </template>
    </div>
    <RouterLink v-else :to="`/memories/${memory.id}`" class="grid aspect-[4/3] place-items-center bg-warm-100 font-serif text-warm-400">一段文字记忆</RouterLink>

    <div class="flex flex-1 flex-col justify-between p-5">
      <div class="space-y-2">
        <div class="flex items-center justify-between text-xs text-warm-400">
          <div class="flex items-center gap-2">
            <div class="grid h-6 w-6 place-items-center rounded-full bg-brand-coral/10 text-[10px] font-bold text-brand-coral">{{ memory.authorName.slice(0,1) }}</div>
            <span class="font-medium text-warm-800">{{ memory.authorName }} 记录</span>
          </div>
          <span>{{ relativeTime }}</span>
        </div>
        <RouterLink :to="`/memories/${memory.id}`" class="block">
          <h2 class="text-lg font-bold text-warm-900 transition-colors group-hover:text-brand-coral">{{ memory.title }}</h2>
          <p class="mt-2 line-clamp-2 text-xs leading-relaxed text-warm-500">{{ memory.content || '这一刻，被家人认真收藏。把每一个普通又特别的日子保留下来。' }}</p>
        </RouterLink>
      </div>
      <div class="mt-4 flex items-center justify-between border-t border-warm-100 pt-4 text-xs text-warm-400">
        <div class="flex items-center gap-4">
          <button @click="like" class="flex items-center gap-1 transition hover:text-brand-coral" :class="memory.liked?'text-brand-coral':''"><Heart :size="16" :fill="memory.liked?'currentColor':'none'"/>{{ memory.likesCount }}</button>
          <RouterLink :to="`/memories/${memory.id}`" class="flex items-center gap-1 transition hover:text-brand-coral"><MessageCircle :size="16"/>{{ memory.commentsCount }} 条回应</RouterLink>
        </div>
        <RouterLink :to="`/memories/${memory.id}`" class="flex items-center gap-0.5 text-xs font-semibold text-brand-coral transition-transform group-hover:translate-x-1">查看详情 <ChevronRight :size="16"/></RouterLink>
      </div>
    </div>

    <Teleport to="body"><Transition name="viewer-fade"><div v-if="viewerOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/95 p-3 sm:p-8" @click.self="closeViewer">
      <button @click="closeViewer" class="absolute right-4 top-4 z-10 grid h-11 w-11 place-items-center rounded-full bg-white/10 text-white backdrop-blur"><X :size="22"/></button>
      <div class="absolute left-4 top-5 rounded-full bg-white/10 px-3 py-1.5 text-xs text-white/80">{{ viewerIndex+1 }} / {{ media.length }}</div>
      <video v-if="isVideo(media[viewerIndex])" :src="media[viewerIndex]" controls autoplay class="max-h-full max-w-full object-contain"></video>
      <img v-else :src="media[viewerIndex]" :alt="memory.title" class="max-h-full max-w-full select-none object-contain"/>
      <template v-if="media.length>1"><button @click="moveViewer(-1)" class="absolute left-3 top-1/2 grid h-11 w-11 -translate-y-1/2 place-items-center rounded-full bg-white/10 text-white"><ChevronLeft :size="24"/></button><button @click="moveViewer(1)" class="absolute right-3 top-1/2 grid h-11 w-11 -translate-y-1/2 place-items-center rounded-full bg-white/10 text-white"><ChevronRight :size="24"/></button></template>
    </div></Transition></Teleport>
  </article>
</template>

<style scoped>
.media-fade-enter-active,.media-fade-leave-active,.viewer-fade-enter-active,.viewer-fade-leave-active{transition:opacity .22s ease}.media-fade-enter-from,.media-fade-leave-to,.viewer-fade-enter-from,.viewer-fade-leave-to{opacity:0}
</style>

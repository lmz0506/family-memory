<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ArrowLeft, ChevronLeft, ChevronRight, Heart, LoaderCircle, MessageSquare, Play, Send, X} from 'lucide-vue-next'
import {memoryApi} from '@/api'
import MemoryImage from '@/components/MemoryImage.vue'
import MediaViewer from '@/components/MediaViewer.vue'
import {notify} from '@/composables/useToast'
import type {Memory} from '@/types'

const route = useRoute()
const router = useRouter()
const memory = ref<Memory | null>(null)
const comment = ref('')
const sending = ref(false)
const liking = ref(false)
const loadError = ref('')
const mediaRatio = ref(1.5)
async function load(){loadError.value='';try {memory.value=await memoryApi.detail(Number(route.params.id))}catch(e){loadError.value=(e as Error).message}}
function goBack(){window.history.state?.back ? router.back() : router.push({name:'home'})}
const activeIndex = ref(0)
const viewerIndex = ref(0)
const viewerOpen = ref(false)
let touchStartX = 0
let touchStartY = 0

const media = computed(() => {
  if (!memory.value) return []
  return Array.from(new Set([...(memory.value.mediaUrls || []), memory.value.coverUrl].filter((url): url is string => Boolean(url))))
})
function isVideo(url: string) { return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url) }
function selectMedia(index: number) { activeIndex.value = index }
function moveMedia(direction: number) { if (media.value.length) activeIndex.value = (activeIndex.value + direction + media.value.length) % media.value.length }
function onTouchStart(event: TouchEvent) { touchStartX = event.touches[0]?.clientX || 0; touchStartY = event.touches[0]?.clientY || 0 }
function onTouchEnd(event: TouchEvent) { const distance = (event.changedTouches[0]?.clientX || 0) - touchStartX; if (Math.abs(distance) > 45 && Math.abs(distance)>Math.abs((event.changedTouches[0]?.clientY||0)-touchStartY)) moveMedia(distance < 0 ? 1 : -1) }
function openViewer() { viewerIndex.value = activeIndex.value; viewerOpen.value = true }
async function like() {
  if (!memory.value || liking.value) return
  liking.value=true
  try {const state=await memoryApi.like(memory.value.id); memory.value.liked=state.liked;memory.value.likesCount=state.likesCount}
  catch(e){notify((e as Error).message,'error')}finally{liking.value=false}
}
async function send() {
  if (!memory.value || !comment.value.trim()) return
  sending.value = true
  try {
    const item = await memoryApi.comment(memory.value.id, comment.value.trim())
    memory.value.comments = [...(memory.value.comments || []), item]
    memory.value.commentsCount++
    comment.value = ''
    notify('你的回应，家人会看到')
  } catch(e){notify((e as Error).message,'error')} finally { sending.value = false }
}
onMounted(load)
</script>

<template>
  <div v-if="loadError" class="empty-state" role="alert"><p>{{loadError}}</p><button @click="load" class="rounded-full bg-warm-900 px-5 py-3 text-white">重新加载</button></div>
  <div v-else-if="!memory" class="grid min-h-[70vh] place-items-center"><LoaderCircle class="animate-spin text-brand-coral"/></div>

  <section v-else class="page-shell page-reveal space-y-6">
    <div class="relative">
      <div class="mb-4 flex items-center justify-between px-1">
        <button @click="goBack" class="group inline-flex items-center gap-2 text-xs font-semibold text-warm-700 transition hover:text-brand-coral">
          <span class="grid h-7 w-7 place-items-center rounded-lg border border-warm-200/60 bg-white shadow-sm transition group-hover:border-brand-coral/40 group-hover:bg-brand-coral-light"><ArrowLeft :size="14" class="transition-transform group-hover:-translate-x-0.5"/></span>
          <span>返回上一页</span>
        </button>
        <span class="text-xs font-medium text-warm-400">回忆详情</span>
      </div>

      <template v-if="media.length">
        <div class="detail-media group relative overflow-hidden rounded-3xl bg-warm-100/60" :style="{aspectRatio:mediaRatio}"
             @touchstart.passive="onTouchStart" @touchend.passive="onTouchEnd">
          <video v-if="isVideo(media[activeIndex])" :src="media[activeIndex]" muted playsinline preload="metadata" class="h-full w-full object-contain"></video>
          <MemoryImage v-else :src="media[activeIndex]!" :alt="memory.title" fit="contain" eager @dimensions="mediaRatio=$event"/>
          <button type="button" @click="openViewer" :aria-label="`全屏浏览：${memory.title}`" class="absolute inset-0"><span v-if="isVideo(media[activeIndex])" class="absolute left-1/2 top-1/2 grid h-12 w-12 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-black/40 text-white"><Play :size="20" fill="currentColor"/></span></button>

          <template v-if="media.length > 1">
            <button @click="moveMedia(-1)" aria-label="上一张照片" class="absolute left-6 top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full border border-warm-200/80 bg-white/80 text-warm-800 shadow-md transition hover:bg-white active:scale-95"><ChevronLeft :size="20"/></button>
            <button @click="moveMedia(1)" aria-label="下一张照片" class="absolute right-6 top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full border border-warm-200/80 bg-white/80 text-warm-800 shadow-md transition hover:bg-white active:scale-95"><ChevronRight :size="20"/></button>
          </template>
          <div class="absolute left-6 top-4 rounded-full border border-warm-200/60 bg-white/90 px-3 py-1 text-xs font-semibold text-warm-700 shadow-sm backdrop-blur-md">{{ activeIndex + 1 }} / {{ media.length }} 媒体文件</div>
        </div>

        <div v-if="media.length > 1" class="no-scrollbar mt-3 flex items-center justify-center gap-3 overflow-x-auto rounded-2xl bg-warm-50/50 p-3">
          <button v-for="(url,index) in media" :key="url" @click="selectMedia(index)" class="relative h-14 w-14 shrink-0 overflow-hidden rounded-xl border-2 border-white shadow-sm ring-2 transition-all hover:scale-105" :class="index===activeIndex?'ring-brand-coral opacity-100':'ring-transparent opacity-60 hover:opacity-100'">
            <video v-if="isVideo(url)" :src="url" muted playsinline preload="metadata" class="h-full w-full object-cover"></video>
            <img v-else :src="url" :alt="`缩略图${index+1}`" class="h-full w-full object-cover"/>
            <span v-if="isVideo(url)" class="absolute inset-0 grid place-items-center bg-black/20 text-white"><Play :size="14" fill="currentColor"/></span>
          </button>
        </div>
      </template>

      <div class="mx-auto mt-6 w-full max-w-3xl space-y-6 px-1 pb-8">
        <div class="flex flex-col justify-between gap-5 md:flex-row md:items-center">
          <div class="space-y-2">
            <div class="flex flex-wrap items-center gap-2">
              <span v-for="tag in memory.tags" :key="tag" class="rounded-full bg-brand-coral-light px-2.5 py-0.5 text-xs font-semibold text-brand-coral"># {{ tag }}</span>
              <span class="text-xs text-warm-400">发生在 {{ memory.memoryDate }}</span>
            </div>
            <h1 class="break-words font-serif text-2xl font-bold text-warm-900 md:text-3xl">{{ memory.title }}</h1>
          </div>

          <div class="flex items-center gap-4 self-start md:self-auto">
            <div class="flex items-center gap-3 border-r border-warm-100 pr-4">
              <div class="grid h-10 w-10 place-items-center rounded-full bg-brand-coral/10 text-sm font-bold text-brand-coral ring-2 ring-brand-coral-light">{{ memory.authorName.slice(0,1) }}</div>
              <div><p class="text-xs font-bold text-warm-900">{{ memory.authorName }}</p><p class="text-[10px] text-warm-400">发布于 {{ memory.createdAt }}</p></div>
            </div>
            <button @click="like" :disabled="liking" :aria-pressed="memory.liked" :aria-label="memory.liked?'取消喜欢':'喜欢这段记忆'" class="flex items-center gap-2 rounded-2xl border px-4 py-2.5 transition-all active:scale-95" :class="memory.liked?'border-rose-200 bg-rose-50 text-brand-coral':'border-warm-200/80 bg-warm-50 text-warm-700 hover:border-rose-200 hover:bg-rose-50'">
              <Heart :size="16" :fill="memory.liked?'currentColor':'none'"/><span class="text-xs font-bold">{{ memory.likesCount }}</span>
            </button>
          </div>
        </div>

        <div class="w-full space-y-3 text-base font-normal leading-8 text-warm-800">
          <p class="rounded-r-xl border-l-2 border-brand-coral bg-brand-coral-light/30 py-1 pl-4 font-serif text-base italic text-warm-900">“这一刻，被家人认真收藏。”</p>
          <p class="whitespace-pre-wrap break-words">{{ memory.content || '把每一个普通又特别的日子保留下来，留给未来慢慢翻阅。' }}</p>
          <audio v-if="memory.audioUrl" :src="memory.audioUrl" controls class="mt-4 w-full"></audio>
        </div>

        <div class="space-y-6 pt-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2"><MessageSquare :size="20" class="text-brand-coral"/><h2 class="text-base font-bold text-warm-900">家人的回应</h2><span class="rounded-full bg-warm-100 px-2 py-0.5 text-xs font-semibold text-warm-500">{{ memory.commentsCount }} 条回应</span></div>
            <span class="hidden text-xs text-warm-400 sm:inline">每一句回应，都让记忆更温暖</span>
          </div>

          <form @submit.prevent="send" class="space-y-3 rounded-2xl border border-warm-100 bg-warm-50/80 p-4">
            <div class="flex items-start gap-3">
              <div class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-brand-coral text-xs font-bold text-white shadow-sm">我</div>
              <textarea v-model="comment" rows="2" maxlength="500" placeholder="写下一句想对家人说的话…" class="w-full resize-none rounded-xl border border-warm-200/80 bg-white p-3 text-xs text-warm-900 outline-none transition placeholder:text-warm-400 focus:border-brand-coral focus:ring-2 focus:ring-brand-coral/30"></textarea>
            </div>
            <div class="flex items-center justify-end pt-1">
              <button :disabled="sending || !comment.trim()" class="flex items-center gap-2 rounded-xl bg-brand-coral px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-brand-coral-hover active:scale-95 disabled:opacity-40"><LoaderCircle v-if="sending" :size="14" class="animate-spin"/><Send v-else :size="14"/>发送回应</button>
            </div>
          </form>

          <div v-if="memory.comments?.length" class="space-y-4">
            <div v-for="item in memory.comments" :key="item.id" class="flex items-start gap-3 rounded-2xl border border-warm-100/60 bg-warm-50/40 p-4 transition hover:bg-warm-50/80">
              <div class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-amber-100 text-xs font-bold text-amber-800">{{ item.userName.slice(0,1) }}</div>
              <div class="flex-1 space-y-1"><div class="flex items-center justify-between"><span class="text-xs font-bold text-warm-900">{{ item.userName }}</span><span class="text-[10px] text-warm-400">{{ item.createdAt }}</span></div><p class="break-words text-xs leading-relaxed text-warm-700">{{ item.content }}</p></div>
            </div>
          </div>
          <div v-else class="rounded-2xl border border-warm-100/60 bg-warm-50/40 p-5 text-center text-xs text-warm-400">还没有回应，来说第一句话吧。</div>
        </div>
      </div>
    </div>

    <MediaViewer v-model:open="viewerOpen" v-model:index="viewerIndex" :media="media" :title="memory.title"/>
  </section>
</template>

<style scoped>.detail-media{width:100%;max-height:65dvh;min-height:180px}@media(min-width:1024px){.detail-media{max-height:580px}}.viewer-fade-enter-active,.viewer-fade-leave-active{transition:opacity .2s ease}.viewer-fade-enter-from,.viewer-fade-leave-to{opacity:0}</style>

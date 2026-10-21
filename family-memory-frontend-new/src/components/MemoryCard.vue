<script setup lang="ts">
import {computed,ref} from 'vue'
import {ChevronLeft,ChevronRight,Heart,Layers,MapPin,MessageCircle,Play} from 'lucide-vue-next'
import type {Memory} from '@/types'
import {memoryApi} from '@/api'
import MemoryImage from './MemoryImage.vue'
import MediaViewer from './MediaViewer.vue'
import {notify} from '@/composables/useToast'
const props=defineProps<{memory:Memory}>()
const emit=defineEmits<{changed:[]}>()
const busy=ref(false)
const activeIndex=ref(0)
const viewerIndex=ref(0)
const viewerOpen=ref(false)
let touchStartX=0,touchStartY=0
const media=computed(()=>Array.from(new Set([...(props.memory.mediaUrls||[]),props.memory.coverUrl].filter((url):url is string=>Boolean(url)))))
const category=computed(()=>props.memory.tags?.[0]||(props.memory.type==='photo'?'照片':props.memory.type==='audio'?'声音':'日常'))
const relativeTime=computed(()=>{
  const value=new Date(props.memory.createdAt).getTime()
  if(Number.isNaN(value))return props.memory.memoryDate
  const days=Math.max(0,Math.floor((Date.now()-value)/86400000))
  return days===0?'今天':days===1?'昨天':days<7?`${days}天前`:days<30?`${Math.floor(days/7)}周前`:props.memory.memoryDate
})
function isVideo(url:string){return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url)}
function goTo(index:number){if(media.value.length)activeIndex.value=(index+media.value.length)%media.value.length}
function onTouchStart(event:TouchEvent){touchStartX=event.touches[0]?.clientX||0;touchStartY=event.touches[0]?.clientY||0}
function onTouchEnd(event:TouchEvent){const dx=(event.changedTouches[0]?.clientX||0)-touchStartX,dy=(event.changedTouches[0]?.clientY||0)-touchStartY;if(Math.abs(dx)>40&&Math.abs(dx)>Math.abs(dy))goTo(activeIndex.value+(dx<0?1:-1))}
function openViewer(){viewerIndex.value=activeIndex.value;viewerOpen.value=true}
async function like(event:Event){
  event.preventDefault();event.stopPropagation();if(busy.value)return;busy.value=true
  try{const state=await memoryApi.like(props.memory.id);props.memory.liked=state.liked;props.memory.likesCount=state.likesCount;emit('changed')}
  catch(e){notify((e as Error).message,'error')}finally{busy.value=false}
}
</script>

<template>
  <article class="group flex flex-col overflow-hidden rounded-3xl border border-warm-100 bg-white shadow-soft transition-all duration-300 hover:shadow-soft-lg">
    <div v-if="media.length" class="relative aspect-[4/3] overflow-hidden bg-warm-100" @touchstart.passive="onTouchStart" @touchend.passive="onTouchEnd">
      <Transition name="media-fade" mode="out-in">
        <div :key="media[activeIndex]" class="absolute inset-0">
          <video v-if="isVideo(media[activeIndex])" :src="media[activeIndex]" muted playsinline preload="metadata" class="h-full w-full object-cover"></video>
          <MemoryImage v-else :src="media[activeIndex]!" :alt="memory.title"/>
        </div>
      </Transition>
      <button type="button" :aria-label="`浏览照片：${memory.title}`" class="absolute inset-0" @click="openViewer"><span v-if="isVideo(media[activeIndex])" class="absolute left-1/2 top-1/2 grid h-12 w-12 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-black/40 text-white"><Play :size="20" fill="currentColor"/></span></button>

      <div class="pointer-events-none absolute left-3 top-3 flex items-center gap-2">
        <span class="flex items-center gap-1 rounded-full bg-black/40 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur-md"><Layers :size="12"/>{{ media.length }}个媒体</span>
      </div>
      <div class="pointer-events-none absolute right-3 top-3"><span class="rounded-full bg-brand-coral/90 px-2.5 py-1 text-[11px] font-semibold text-white backdrop-blur-sm">{{ category }}</span></div>
      <div class="pointer-events-none absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-black/50 to-transparent"></div>
      <div class="pointer-events-none absolute bottom-3 left-3 flex items-center gap-1.5 text-xs font-medium text-white"><MapPin :size="14" class="text-brand-peach"/><span>{{ memory.location || '家庭记忆' }} · {{ memory.memoryDate }}</span></div>

      <template v-if="media.length > 1">
        <button @click.stop="goTo(activeIndex-1)" aria-label="上一张照片" class="absolute left-3 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-full bg-white/80 text-warm-800 opacity-100 shadow transition sm:opacity-0 sm:group-hover:opacity-100 focus:opacity-100"><ChevronLeft :size="16"/></button>
        <button @click.stop="goTo(activeIndex+1)" aria-label="下一张照片" class="absolute right-3 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-full bg-white/80 text-warm-800 opacity-100 shadow transition sm:opacity-0 sm:group-hover:opacity-100 focus:opacity-100"><ChevronRight :size="16"/></button>
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
          <h2 class="line-clamp-2 text-lg font-bold leading-7 text-warm-900 transition-colors group-hover:text-brand-coral">{{ memory.title }}</h2>
          <p class="mt-2 line-clamp-2 text-sm leading-6 text-warm-500">{{ memory.content || '这一刻，被家人认真收藏。把每一个普通又特别的日子保留下来。' }}</p>
        </RouterLink>
      </div>
      <div class="mt-4 flex items-center justify-between border-t border-warm-100 pt-4 text-xs text-warm-400">
        <div class="flex items-center gap-4">
          <button @click="like" :disabled="busy" :aria-label="memory.liked ? '取消喜欢' : '喜欢这段记忆'" :aria-pressed="memory.liked" class="flex items-center gap-1 transition hover:text-brand-coral" :class="memory.liked?'text-brand-coral':''"><Heart :size="16" :fill="memory.liked?'currentColor':'none'"/>{{ memory.likesCount }}</button>
          <RouterLink :to="`/memories/${memory.id}`" class="flex items-center gap-1 transition hover:text-brand-coral"><MessageCircle :size="16"/>{{ memory.commentsCount }} 条回应</RouterLink>
        </div>
        <RouterLink :to="`/memories/${memory.id}`" class="flex items-center gap-0.5 text-xs font-semibold text-brand-coral transition-transform group-hover:translate-x-1">翻开故事 <ChevronRight :size="16"/></RouterLink>
      </div>
    </div>

    <MediaViewer v-model:open="viewerOpen" v-model:index="viewerIndex" :media="media" :title="memory.title"/>
  </article>
</template>

<style scoped>
.media-fade-enter-active,.media-fade-leave-active,.viewer-fade-enter-active,.viewer-fade-leave-active{transition:opacity .22s ease}.media-fade-enter-from,.media-fade-leave-to,.viewer-fade-enter-from,.viewer-fade-leave-to{opacity:0}
</style>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {MapPin, Search, Sparkles, UserRound} from 'lucide-vue-next'
import {useFamilyStore} from '@/stores/family'
import {useMemoryStore} from '@/stores/memories'
import StarTimeline from '@/components/StarTimeline.vue'
import type {Memory} from '@/types'

type TimelineMode = 'stars' | 'classic'
const route = useRoute()
const family = useFamilyStore()
const store = useMemoryStore()
const selectedYear = ref<number | null>(null)
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const savedMode = localStorage.getItem('family-memory-timeline-mode')
const mode = ref<TimelineMode>(keyword.value ? 'classic' : savedMode === 'classic' ? 'classic' : 'stars')
const normalLoaded = ref(false)

const groups = computed(() => {
  const map = new Map<string, Memory[]>()
  for (const item of store.items) {
    const key = item.memoryDate.slice(0, 7)
    map.set(key, [...(map.get(key) || []), item])
  }
  return [...map.entries()].map(([key, items]) => ({
    key,
    year: key.slice(0, 4),
    month: Number(key.slice(5)),
    items
  }))
})

function mediaUrl(item: Memory) { return item.coverUrl || item.mediaUrls?.[0] || '' }
function isVideo(url: string) { return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url) }

async function loadNormal() {
  if (!family.current) return
  await store.load(family.current.id, {
    page: 1,
    size: 50,
    year: selectedYear.value || undefined,
    keyword: keyword.value.trim() || undefined
  })
  normalLoaded.value = true
}

async function setMode(next: TimelineMode) {
  mode.value = next
  localStorage.setItem('family-memory-timeline-mode', next)
  if (next === 'classic' && !normalLoaded.value) await loadNormal()
}

async function selectYear(year: number | null) {
  if (selectedYear.value === year) return
  selectedYear.value = year
  await loadNormal()
}

onMounted(async () => {
  if (!family.current) await family.load()
  if (!family.current) return
  await store.loadHighlights(family.current.id)
  if (mode.value === 'classic') await loadNormal()
})

watch(() => route.query.keyword, async value => {
  keyword.value = typeof value === 'string' ? value : ''
  if (keyword.value) await setMode('classic')
  await loadNormal()
})
</script>

<template>
  <section :class="mode === 'stars' ? '-m-4 sm:-m-6 md:-m-8' : 'w-full space-y-6'">
    <header v-if="mode === 'classic'" class="flex flex-col justify-between gap-5 rounded-3xl border border-warm-100 bg-gradient-to-r from-warm-50 via-white to-warm-50/50 p-6 shadow-soft md:flex-row md:items-center md:p-8">
      <div class="space-y-1">
        <p class="text-xs font-bold uppercase tracking-widest text-brand-coral">OUR STORY</p>
        <h1 class="font-serif text-3xl font-bold text-warm-900">时光轴</h1>
        <p class="text-xs text-warm-400">{{ family.current?.memoryCount || store.total || 0 }} 个日子，被家人认真收藏。</p>
      </div>
      <div class="flex items-center rounded-2xl border border-warm-200 bg-warm-100/70 p-1">
        <button type="button" @click="setMode('stars')" class="flex items-center gap-1.5 rounded-xl px-4 py-2 text-xs font-semibold text-warm-500 transition hover:text-warm-900"><Sparkles :size="14"/>星海模式</button>
        <button type="button" class="rounded-xl bg-white px-4 py-2 text-xs font-semibold text-warm-900 shadow-sm">普通模式</button>
      </div>
    </header>

    <div v-if="mode === 'stars' && family.current" class="relative min-h-[calc(100vh-72px)] overflow-hidden bg-[#030611]">
      <div class="pointer-events-none absolute left-5 top-5 z-50 text-white sm:left-7 sm:top-6">
        <p class="text-[10px] font-bold uppercase tracking-[.2em] text-brand-peach">OUR STORY</p>
        <h1 class="mt-1 font-serif text-2xl font-bold drop-shadow-lg sm:text-3xl">时光轴</h1>
        <p class="mt-1 text-[10px] text-white/50">{{ family.current.memoryCount || 0 }} 个日子，化作星光留在这里。</p>
      </div>
      <div class="absolute right-5 top-5 z-50 flex items-center rounded-2xl border border-white/15 bg-black/35 p-1 shadow-xl backdrop-blur-md sm:right-7 sm:top-6">
        <button type="button" class="flex items-center gap-1.5 rounded-xl bg-white/12 px-4 py-2 text-xs font-semibold text-brand-peach"><Sparkles :size="14"/>星海模式</button>
        <button type="button" @click="setMode('classic')" class="rounded-xl px-4 py-2 text-xs font-semibold text-white/60 transition hover:bg-white/10 hover:text-white">普通模式</button>
      </div>
      <StarTimeline :key="family.current.id" :family-id="family.current.id"/>
    </div>

    <template v-else>
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <form @submit.prevent="loadNormal" class="relative min-w-[220px]">
          <Search :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-warm-400"/>
          <input v-model="keyword" class="w-full rounded-full border border-warm-200/80 bg-white py-2.5 pl-9 pr-3 text-xs text-warm-800 outline-none transition placeholder:text-warm-400 focus:ring-2 focus:ring-brand-coral/30" placeholder="寻找回忆...">
        </form>
        <div class="no-scrollbar flex max-w-full items-center gap-1.5 overflow-x-auto rounded-full border border-warm-200/50 bg-warm-100/60 p-1">
          <button @click="selectYear(null)" class="shrink-0 rounded-full px-3.5 py-1.5 text-xs font-semibold transition-all" :class="selectedYear === null ? 'bg-warm-900 text-white shadow-sm' : 'text-warm-700 hover:bg-white'">全部</button>
          <button v-for="year in store.years" :key="year" @click="selectYear(year)" class="shrink-0 rounded-full px-3.5 py-1.5 text-xs font-medium transition-all" :class="selectedYear === year ? 'bg-warm-900 text-white shadow-sm' : 'text-warm-700 hover:bg-white'">{{ year }}</button>
        </div>
      </div>

      <div v-if="store.loading" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <div v-for="n in 10" :key="n" class="aspect-[4/3] animate-pulse rounded-2xl bg-warm-100"></div>
      </div>

      <div v-else-if="groups.length" class="relative space-y-10">
        <section v-for="group in groups" :key="group.key" class="space-y-4">
          <div class="flex items-baseline gap-2.5 border-b border-warm-200/60 pb-3">
            <h2 class="font-serif text-2xl font-bold text-warm-900">{{ group.month }}月</h2>
            <span class="text-sm font-medium text-warm-400">{{ group.year }}</span>
          </div>
          <div class="grid grid-cols-1 gap-4 pt-2 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            <RouterLink v-for="item in group.items" :key="item.id" :to="`/memories/${item.id}`" class="group relative aspect-[4/3] cursor-pointer overflow-hidden rounded-2xl bg-warm-200 shadow-soft transition-all duration-300 hover:-translate-y-1 hover:shadow-soft-lg">
              <video v-if="mediaUrl(item) && isVideo(mediaUrl(item))" :src="mediaUrl(item)" muted playsinline preload="metadata" class="h-full w-full object-cover transition duration-700 group-hover:scale-105"></video>
              <img v-else-if="mediaUrl(item)" :src="mediaUrl(item)" :alt="item.title" class="h-full w-full object-cover transition duration-700 group-hover:scale-105">
              <div v-else class="grid h-full place-items-center bg-gradient-to-br from-warm-800 to-warm-900 px-5 text-center font-serif text-sm text-warm-300">一段文字记忆</div>
              <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/15 to-transparent"></div>
              <div class="absolute left-3 top-3 rounded-xl border border-white/60 bg-white/90 px-2.5 py-1.5 text-center shadow-md backdrop-blur-md"><div class="text-sm font-bold leading-none text-warm-900">{{ item.memoryDate.slice(8) }}</div><div class="mt-0.5 text-[9px] font-semibold text-warm-500">{{ item.memoryDate.slice(5, 7) }}月</div></div>
              <div class="absolute bottom-3 left-3 right-3 text-white">
                <span v-if="item.tags?.length" class="mb-1 inline-block rounded-md bg-brand-coral/85 px-2 py-0.5 text-[10px] font-semibold">{{ item.tags[0] }}</span>
                <h3 class="line-clamp-2 text-sm font-bold leading-snug transition-colors group-hover:text-brand-peach">{{ item.title }}</h3>
                <div class="mt-2 space-y-1 border-t border-white/20 pt-2 text-[10px] text-white/80"><div class="flex items-center gap-1"><MapPin :size="11" class="text-brand-peach"/><span class="truncate">{{ item.location || '未记录地点' }}</span></div><div class="flex items-center gap-1"><UserRound :size="11" class="text-brand-peach"/><span class="truncate">{{ item.authorName }} 发布</span></div></div>
              </div>
            </RouterLink>
          </div>
        </section>
      </div>

      <div v-else class="rounded-3xl border border-dashed border-warm-200 bg-white/60 py-24 text-center text-sm text-warm-400">没有找到符合条件的记忆</div>
    </template>
  </section>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from 'vue'
import {ChevronLeft, ChevronRight, Image, LoaderCircle, Plus, Sun} from 'lucide-vue-next'
import {useFamilyStore} from '@/stores/family'
import {useMemoryStore} from '@/stores/memories'
import MemoryCard from '@/components/MemoryCard.vue'

const family = useFamilyStore()
const memories = useMemoryStore()
const mobilePageSize = 4
type DisplayMode = 'large' | 'medium' | 'small'
const displaySettings: Record<DisplayMode, {label: string; columns: number; pageSize: number; gridClass: string}> = {
  large: {label: '大', columns: 3, pageSize: 6, gridClass: 'md:grid-cols-3'},
  medium: {label: '中', columns: 4, pageSize: 8, gridClass: 'md:grid-cols-4'},
  small: {label: '小', columns: 5, pageSize: 10, gridClass: 'md:grid-cols-5'}
}
const savedDisplayMode = localStorage.getItem('family-memory-home-display')
const displayMode = ref<DisplayMode>(savedDisplayMode === 'medium' || savedDisplayMode === 'small' ? savedDisplayMode : 'large')
const page = ref(1)
const mobilePage = ref(1)
const loadingMore = ref(false)
const loadMoreTrigger = ref<HTMLElement | null>(null)
const isDesktop = ref(false)
let mediaQuery: MediaQueryList | undefined
let observer: IntersectionObserver | undefined

const greeting = computed(() => new Date().getHours() < 12 ? '早上好' : new Date().getHours() < 18 ? '下午好' : '晚上好')
const desktopPageSize = computed(() => displaySettings[displayMode.value].pageSize)
const gridClass = computed(() => displaySettings[displayMode.value].gridClass)
const totalPages = computed(() => Math.max(1, Math.ceil(memories.total / desktopPageSize.value)))
const hasMore = computed(() => memories.items.length < memories.total)

async function loadFirstPage() {
  if (!family.current) return
  page.value = 1
  mobilePage.value = 1
  await memories.load(family.current.id, {page: 1, size: isDesktop.value ? desktopPageSize.value : mobilePageSize})
}

async function goToPage(target: number) {
  if (!family.current || target < 1 || target > totalPages.value || target === page.value) return
  page.value = target
  await memories.load(family.current.id, {page: target, size: desktopPageSize.value})
  document.querySelector('main > div:last-of-type')?.scrollTo({top: 0, behavior: 'smooth'})
}

async function changeDisplayMode(mode: string) {
  const nextMode = mode as DisplayMode
  if (displayMode.value === nextMode) return
  displayMode.value = nextMode
  localStorage.setItem('family-memory-home-display', nextMode)
  if (isDesktop.value) await loadFirstPage()
}

async function loadMore() {
  if (isDesktop.value || !family.current || !hasMore.value || loadingMore.value) return
  loadingMore.value = true
  mobilePage.value++
  try {
    await memories.load(family.current.id, {page: mobilePage.value, size: mobilePageSize}, true)
  } finally {
    loadingMore.value = false
  }
}

function observeLoadMore() {
  observer?.disconnect()
  if (!loadMoreTrigger.value) return
  observer = new IntersectionObserver(entries => {
    if (entries[0]?.isIntersecting) loadMore()
  }, {rootMargin: '300px 0px'})
  observer.observe(loadMoreTrigger.value)
}

async function handleBreakpoint(event: MediaQueryListEvent) {
  isDesktop.value = event.matches
  await loadFirstPage()
  await nextTick()
  observeLoadMore()
}

onMounted(async () => {
  mediaQuery = window.matchMedia('(min-width: 768px)')
  isDesktop.value = mediaQuery.matches
  mediaQuery.addEventListener('change', handleBreakpoint)
  if (!family.current) await family.load()
  await loadFirstPage()
  window.addEventListener('family-memory-created', loadFirstPage)
  await nextTick()
  observeLoadMore()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  mediaQuery?.removeEventListener('change', handleBreakpoint)
  window.removeEventListener('family-memory-created', loadFirstPage)
})
</script>

<template>
  <section class="w-full space-y-8">
    <div class="relative overflow-hidden rounded-3xl border border-brand-coral/10 bg-gradient-to-r from-[#fff9f5] via-[#fff3ec] to-[#fdf8f3] p-8 shadow-soft md:p-10">
      <div class="pointer-events-none absolute -bottom-10 -right-10 h-72 w-72 rounded-full bg-brand-peach/10 blur-3xl"></div>
      <div class="relative z-10 flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
        <div class="max-w-2xl space-y-3">
          <div class="inline-flex items-center gap-2 rounded-full bg-brand-coral-light px-3 py-1 text-xs font-semibold text-brand-coral">
            <Sun :size="14"/><span>{{ greeting }}，欢迎回家</span>
          </div>
          <h1 class="font-serif text-3xl font-bold leading-tight text-warm-900 md:text-4xl">把日子，一页页留住。</h1>
          <p class="text-sm leading-relaxed text-warm-500">这里收集家人的照片、故事，也收集每一个普通却珍贵的瞬间。每一个记录，都是献给未来的情书。</p>
        </div>
        <div class="flex self-stretch items-center justify-between gap-3 rounded-2xl border border-white/80 bg-white/90 px-5 py-3.5 shadow-soft backdrop-blur-md md:self-auto md:justify-start">
          <div class="rounded-xl bg-brand-coral-light p-3 text-brand-coral"><Image :size="24"/></div>
          <div>
            <div class="flex items-baseline gap-1"><span class="text-2xl font-bold text-warm-900">{{ family.current?.photoCount || 0 }}</span><span class="text-xs text-warm-400">个</span></div>
            <p class="text-xs font-medium text-warm-500">照片与回忆视频</p>
          </div>
        </div>
      </div>
    </div>

    <div class="hidden items-center justify-end gap-2 md:flex">
      <span class="mr-1 text-xs font-medium text-warm-400">卡片显示</span>
      <div class="flex rounded-xl border border-warm-200 bg-white p-1 shadow-sm" aria-label="首页卡片显示模式">
        <button v-for="(setting, mode) in displaySettings" :key="mode" type="button"
                :aria-pressed="displayMode === mode" @click="changeDisplayMode(mode)"
                class="rounded-lg px-3 py-1.5 text-xs font-semibold transition"
                :class="displayMode === mode ? 'bg-warm-900 text-white shadow-sm' : 'text-warm-500 hover:bg-warm-50 hover:text-warm-900'">
          {{ setting.label }}<span class="ml-1 text-[9px] opacity-65">{{ setting.columns }}列</span>
        </button>
      </div>
    </div>

    <div v-if="memories.loading && !memories.items.length" class="grid grid-cols-1 gap-6" :class="gridClass">
      <div v-for="n in (isDesktop ? desktopPageSize : mobilePageSize)" :key="n" class="h-[440px] animate-pulse rounded-3xl bg-warm-100"></div>
    </div>

    <div v-else-if="memories.items.length" class="grid grid-cols-1 gap-6" :class="gridClass">
      <MemoryCard v-for="item in memories.items" :key="item.id" :memory="item"/>
    </div>

    <div v-else class="rounded-3xl border border-dashed border-warm-300 bg-white/70 px-6 py-16 text-center">
      <div class="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-brand-coral-light text-brand-coral"><Plus :size="20"/></div>
      <h2 class="mt-4 font-serif text-xl font-bold text-warm-900">这个家还没有记忆</h2>
      <p class="mt-2 text-xs text-warm-400">从一件今天发生的小事开始吧。</p>
    </div>

    <nav v-if="isDesktop && totalPages > 1" class="flex items-center justify-center gap-2" aria-label="首页分页">
      <button :disabled="page===1 || memories.loading" @click="goToPage(page-1)" class="grid h-10 w-10 place-items-center rounded-xl border border-warm-200 bg-white text-warm-500 transition hover:border-brand-coral/40 hover:text-brand-coral disabled:opacity-35"><ChevronLeft :size="17"/></button>
      <button v-for="number in totalPages" :key="number" @click="goToPage(number)" class="grid h-10 min-w-10 place-items-center rounded-xl px-2 text-xs font-semibold transition" :class="number===page?'bg-warm-900 text-white shadow-sm':'border border-warm-200 bg-white text-warm-800 hover:border-brand-coral/40'">{{ number }}</button>
      <button :disabled="page===totalPages || memories.loading" @click="goToPage(page+1)" class="grid h-10 w-10 place-items-center rounded-xl border border-warm-200 bg-white text-warm-500 transition hover:border-brand-coral/40 hover:text-brand-coral disabled:opacity-35"><ChevronRight :size="17"/></button>
    </nav>

    <div v-if="!isDesktop" ref="loadMoreTrigger" class="flex h-20 items-center justify-center text-xs text-warm-400">
      <span v-if="loadingMore" class="flex items-center gap-2"><LoaderCircle :size="16" class="animate-spin"/>正在翻出更多记忆</span>
      <span v-else-if="!hasMore && memories.items.length">已经看到全部记忆了</span>
    </div>
  </section>
</template>

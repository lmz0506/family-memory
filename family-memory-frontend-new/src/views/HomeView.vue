<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from 'vue'
import {ChevronLeft, ChevronRight, Image, LoaderCircle, Plus, Sun} from 'lucide-vue-next'
import {useFamilyStore} from '@/stores/family'
import {useMemoryStore} from '@/stores/memories'
import MemoryCard from '@/components/MemoryCard.vue'
import SegmentedControl from '@/components/SegmentedControl.vue'
import {notify} from '@/composables/useToast'

const family = useFamilyStore()
const memories = useMemoryStore()
const mobilePageSize = 4
type DisplayMode = 'large' | 'medium' | 'small'
const displaySettings: Record<DisplayMode, {label: string; columns: number; pageSize: number; gridClass: string}> = {
  large: {label: '大', columns: 3, pageSize: 6, gridClass: 'memory-grid-large'},
  medium: {label: '中', columns: 4, pageSize: 8, gridClass: 'memory-grid-medium'},
  small: {label: '小', columns: 5, pageSize: 10, gridClass: 'memory-grid-small'}
}
const savedDisplayMode = localStorage.getItem('family-memory-home-display')
const displayMode = ref<DisplayMode>(savedDisplayMode === 'medium' || savedDisplayMode === 'small' ? savedDisplayMode : 'large')
const page = ref(1)
const mobilePage = ref(1)
const loadingMore = ref(false)
const loadError = ref('')
const displayOptions = [{value:'large',label:'舒展'}, {value:'medium',label:'标准'}, {value:'small',label:'紧凑'}]
function openComposer() { window.dispatchEvent(new Event('family-memory-compose')) }
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
  loadError.value = ''
  try { await memories.load(family.current.id, {page: 1, size: isDesktop.value ? desktopPageSize.value : mobilePageSize}) }
  catch (e) {loadError.value = (e as Error).message}
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
  try {
    await memories.load(family.current.id, {page: mobilePage.value + 1, size: mobilePageSize}, true)
    mobilePage.value++
  } catch (e) {notify((e as Error).message, 'error'); observer?.disconnect()} finally {
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
  <section class="page-shell page-reveal space-y-6">
    <header class="home-welcome">
      <div>
        <p class="page-eyebrow flex items-center gap-2"><Sun :size="15"/>{{ greeting }} · {{ family.current?.name || '欢迎回家' }}</p>
        <h1 class="page-title mt-2">把日子，一页页留住。</h1>
        <p class="mt-2 max-w-xl text-sm leading-7 text-warm-500">照片里的笑，日常里的爱，都在这里慢慢积攒。</p>
      </div>
      <div class="home-welcome-aside">
        <span class="flex items-center gap-2 text-sm text-warm-500"><Image :size="18" class="text-brand-coral"/><b class="text-lg text-warm-900">{{ family.current?.photoCount || 0 }}</b>个影像瞬间</span>
        <button @click="openComposer" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-full bg-brand-coral px-5 text-sm font-semibold text-white shadow-soft transition hover:-translate-y-0.5 hover:bg-brand-coral-hover active:scale-95"><Plus :size="18"/>记录这一刻</button>
      </div>
    </header>
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div><p class="page-eyebrow">LITTLE MOMENTS, BIG LOVE</p><h2 class="mt-1 font-serif text-xl font-bold text-warm-900">家里的故事 <span class="ml-2 font-sans text-sm font-normal text-warm-500">{{ memories.total }} 段回忆</span></h2></div>
      <div class="hidden md:block"><SegmentedControl :model-value="displayMode" :options="displayOptions" label="记忆卡片密度" @update:model-value="changeDisplayMode"/></div>
    </div>
    <div v-if="loadError" class="empty-state" role="alert"><p>暂时没能翻开这些记忆</p><p class="text-sm">{{ loadError }}</p><button class="rounded-full bg-warm-900 px-5 py-2.5 text-sm text-white" @click="loadFirstPage">重新加载</button></div>
    <div v-else-if="memories.loading && !memories.items.length" class="memory-grid" :class="gridClass">
      <div v-for="n in (isDesktop ? desktopPageSize : mobilePageSize)" :key="n" class="h-[440px] animate-pulse rounded-3xl bg-warm-100"></div>
    </div>

    <div v-else-if="memories.items.length" class="memory-grid" :class="gridClass">
      <MemoryCard v-for="item in memories.items" :key="item.id" :memory="item"/>
    </div>

    <div v-else class="rounded-3xl border border-dashed border-warm-300 bg-white/70 px-6 py-16 text-center">
      <div class="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-brand-coral-light text-brand-coral"><Plus :size="20"/></div>
      <h2 class="mt-4 font-serif text-xl font-bold text-warm-900">从第一段故事开始</h2>
      <p class="mt-2 text-xs text-warm-400">从一件今天发生的小事开始吧。</p><button @click="openComposer" class="mt-5 rounded-full bg-brand-coral px-5 py-3 text-sm font-semibold text-white">记录第一段记忆</button>
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

<style scoped>
.home-welcome{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:28px 32px;border:1px solid #efdfd2;border-radius:24px;background:radial-gradient(ellipse at top right,#fbe7d5,transparent 60%),#fffaf5}
.home-welcome-aside{display:flex;flex-direction:column;align-items:flex-end;gap:14px;flex-shrink:0}
.memory-grid{display:grid;gap:24px;grid-template-columns:repeat(auto-fit,minmax(min(100%,290px),1fr))}
.memory-grid-medium{grid-template-columns:repeat(auto-fit,minmax(min(100%,250px),1fr))}
.memory-grid-small{grid-template-columns:repeat(auto-fit,minmax(min(100%,220px),1fr))}
@media(min-width:1650px){.memory-grid-large{grid-template-columns:repeat(3,minmax(0,1fr))}.memory-grid-medium{grid-template-columns:repeat(4,minmax(0,1fr))}.memory-grid-small{grid-template-columns:repeat(5,minmax(0,1fr))}}
@media(max-width:767px){.home-welcome{padding:20px;display:block}.home-welcome-aside{margin-top:16px;flex-direction:row;align-items:center;justify-content:space-between;gap:8px}.home-welcome-aside>span{font-size:12px}.home-welcome-aside>button{padding-inline:14px;font-size:13px}.memory-grid{grid-template-columns:minmax(0,1fr);gap:20px}}
</style>

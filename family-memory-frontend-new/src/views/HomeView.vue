<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {ChevronLeft, ChevronRight, LoaderCircle, Plus, Search, X} from 'lucide-vue-next'
import {useRoute,useRouter} from 'vue-router'
import SegmentedControl from '@/components/SegmentedControl.vue'
import type {FamilyMember,Memory} from '@/types'
import {useFamilyStore} from '@/stores/family'
import {useMemoryStore} from '@/stores/memories'
import MemoryCard from '@/components/MemoryCard.vue'
import MemoryWall from '@/components/MemoryWall.vue'
import {familyApi,memoryApi} from '@/api'
import SkyPageHeader from '@/components/SkyPageHeader.vue'
import SkySelect from '@/components/SkySelect.vue'
import {readViewState,writeViewState} from '@/composables/viewState'
import {albumPageSize} from '@/composables/albumLayout'

const route=useRoute(),router=useRouter()
const view=computed(()=>route.query.view==='timeline'?'timeline':'grid')
function switchView(value:string){void router.replace({query:{...route.query,view:value}})}
const family = useFamilyStore()
const memories = useMemoryStore()
const mobilePageSize = 4
const keyword=ref(typeof route.query.keyword==='string'?route.query.keyword:''),yearFilter=ref(''),yearOptions=ref<number[]>([]),authorFilter=ref(''),authorMembers=ref<FamilyMember[]>([...family.members]),authorError=ref('')
let searchTimer:ReturnType<typeof setTimeout>|undefined
function queueSearch(event:Event){if((event as InputEvent).isComposing)return;if(searchTimer)clearTimeout(searchTimer);searchTimer=setTimeout(()=>{void loadFirstPage()},350)}
async function loadAuthors(){if(!family.current)return;const familyId=family.current.id;authorError.value='';try{const members=await familyApi.members(familyId);if(alive&&family.current?.id===familyId)authorMembers.value=members}catch{if(alive)authorError.value='作者列表暂未载入'}}
function submitSearch(){if(searchTimer)clearTimeout(searchTimer);void loadFirstPage()}
const monthGroups=computed(()=>{const groups=new Map<string,Memory[]>();for(const item of memories.items){const key=item.memoryDate.slice(0,7);groups.set(key,[...(groups.get(key)||[]),item])}return Array.from(groups,([key,items])=>({key,year:key.slice(0,4),month:Number(key.slice(5)),items}))})
const filters=()=>({authorId:authorFilter.value?Number(authorFilter.value):undefined,keyword:keyword.value.trim()||undefined,year:yearFilter.value?Number(yearFilter.value):undefined})
const page = ref(1)
const mobilePage = ref(1)
const loadingMore = ref(false)
const loadError = ref(''),loadMoreError=ref('')
function openComposer() { window.dispatchEvent(new Event('family-memory-compose')) }
const loadMoreTrigger = ref<HTMLElement | null>(null)
const isDesktop = ref(false)
let mediaQuery: MediaQueryList | undefined
let observer: IntersectionObserver | undefined,alive=true,loadVersion=0

const desktopPageSize=ref(12)
let resizeObserver:ResizeObserver|undefined,resizeTimer:ReturnType<typeof setTimeout>|undefined
const albumRoot=ref<HTMLElement>()
function availableAlbumWidth(){const root=albumRoot.value;if(!root)return window.innerWidth-64;const css=window.getComputedStyle(root);return Math.max(0,root.clientWidth-(parseFloat(css.paddingLeft)||0)-(parseFloat(css.paddingRight)||0))}
function updateAlbumWidth(){
 const size=albumPageSize(availableAlbumWidth())
 if(size===desktopPageSize.value)return
 desktopPageSize.value=size
 if(isDesktop.value){if(resizeTimer)clearTimeout(resizeTimer);resizeTimer=setTimeout(()=>{void loadFirstPage()},200)}
}
const totalPages = computed(() => Math.max(1, Math.ceil(memories.total / desktopPageSize.value)))
const hasMore = computed(() => memories.items.length < memories.total)

async function loadFirstPage() {
  if (!family.current) return
  const version=++loadVersion
  page.value = 1
  mobilePage.value = 1
  loadError.value = '';loadMoreError.value=''
  try { await memories.load(family.current.id, {page: 1, size: isDesktop.value ? desktopPageSize.value : mobilePageSize,...filters()}) }
  catch (e) {if(version===loadVersion&&alive)loadError.value = (e as Error).message}
  await nextTick();if(version===loadVersion&&alive)observeLoadMore()
}

async function goToPage(target: number) {
  if (!family.current || target < 1 || target > totalPages.value || target === page.value) return
  loadError.value=''
  const version=++loadVersion
  try{await memories.load(family.current.id, {page: target, size: desktopPageSize.value,...filters()});if(version!==loadVersion||!alive)return;page.value=target}catch(e){if(version===loadVersion&&alive)loadError.value=(e as Error).message;return}
  document.querySelector('[data-app-scroll]')?.scrollTo({top: 0, behavior: 'smooth'})
}

async function loadMore() {
  if (isDesktop.value || !family.current || !hasMore.value || loadingMore.value || memories.loading) return
  loadingMore.value = true;loadMoreError.value=''
  const version=++loadVersion
  try {
    await memories.load(family.current.id, {page: mobilePage.value + 1, size: mobilePageSize,...filters()}, true)
    if(version===loadVersion&&alive)mobilePage.value++
  } catch (e) {if(version===loadVersion&&alive){loadMoreError.value=(e as Error).message;observer?.disconnect()}} finally {
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
  if(resizeTimer)clearTimeout(resizeTimer)
  desktopPageSize.value=albumPageSize(availableAlbumWidth())
  await loadFirstPage()
  await nextTick()
  observeLoadMore()
}

onMounted(async () => {
  mediaQuery = window.matchMedia('(min-width: 768px)')
  isDesktop.value = mediaQuery.matches
  desktopPageSize.value=albumPageSize(availableAlbumWidth())
  mediaQuery.addEventListener('change', handleBreakpoint)
  if (!family.current) await family.load()
  if(!family.current||!alive)return
  await Promise.all([memoryApi.years(family.current.id).then(years=>yearOptions.value=years).catch(()=>{}),loadAuthors()])
  if(!alive)return
  const saved=readViewState<{keyword:string;year:string;page:number;mobilePage:number;scroll:number;items?:import('@/types').Memory[];total:number;stale?:boolean;desktop:boolean;pageSize?:number;view?:string;authorId?:string}>(`album:${family.current.id}`)
  if(saved){if(route.query.keyword===undefined)keyword.value=saved.keyword;yearFilter.value=saved.year;authorFilter.value=saved.authorId||'';if(!route.query.view&&saved.view)switchView(saved.view);page.value=saved.page;mobilePage.value=saved.mobilePage
    if(!isDesktop.value&&saved.desktop===false&&saved.items&&!saved.stale&&(route.query.keyword===undefined||route.query.keyword===saved.keyword)){memories.items=saved.items;memories.total=saved.total}
    else if(saved.stale||route.query.keyword!==undefined&&route.query.keyword!==saved.keyword||isDesktop.value&&saved.pageSize!==desktopPageSize.value||!isDesktop.value&&saved.desktop!==false){await loadFirstPage()}else await memories.load(family.current.id,{page:saved.page,size:isDesktop.value?desktopPageSize.value:mobilePageSize,...filters()})
    await nextTick();document.querySelector('[data-app-scroll]')?.scrollTo({top:saved.stale||route.query.keyword!==undefined&&route.query.keyword!==saved.keyword?0:saved.scroll})
  }else await loadFirstPage()
  if(!alive)return
  window.addEventListener('family-memory-created', handleCreated)
  await nextTick()
  observeLoadMore();resizeObserver=new ResizeObserver(updateAlbumWidth);if(albumRoot.value)resizeObserver.observe(albumRoot.value)
})

onBeforeUnmount(() => {
  alive=false;loadVersion++
  if(family.current)writeViewState(`album:${family.current.id}`,{keyword:keyword.value,year:yearFilter.value,page:page.value,mobilePage:mobilePage.value,scroll:document.querySelector('[data-app-scroll]')?.scrollTop||0,items:[...memories.items],total:memories.total,desktop:isDesktop.value,pageSize:desktopPageSize.value,view:view.value,authorId:authorFilter.value})
  observer?.disconnect();resizeObserver?.disconnect();if(resizeTimer)clearTimeout(resizeTimer)
  if(searchTimer)clearTimeout(searchTimer)
  mediaQuery?.removeEventListener('change', handleBreakpoint)
  window.removeEventListener('family-memory-created', handleCreated)
})
function handleCreated(){void loadFirstPage()}
watch(()=>route.query.keyword,value=>{if(typeof value==='string'&&value!==keyword.value){keyword.value=value;void loadFirstPage()}})
</script>

<template>
  <section ref="albumRoot" class="album-page page-reveal space-y-8">
    <SkyPageHeader title="回忆" description="那些漫过岁月的欢喜与微光，都在这里被温柔记录。"><template #title-extra><span class="album-stat">{{family.current?.memoryCount??memories.total}} 段回忆 · {{family.current?.photoCount||0}} 个影像瞬间</span></template><button class="sky-primary" @click="openComposer"><Plus :size="17"/>记录这一刻</button></SkyPageHeader>
    <div class="album-toolbar">
      <div class="album-filters"><form class="album-search" @submit.prevent="submitSearch"><Search :size="15"/><input v-model="keyword" aria-label="搜索回忆" placeholder="搜索记忆标题…" @input="queueSearch" @compositionend="queueSearch"/><button v-if="keyword" type="button" aria-label="清除搜索" @click="keyword='';submitSearch()"><X :size="14"/></button><button v-else type="submit" aria-label="搜索"><Search :size="14"/></button></form>
      <div class="album-filter"><SkySelect id="album-year" label="年份" :model-value="yearFilter" :options="[{value:'',label:'所有年份'},...yearOptions.map(y=>({value:String(y),label:`${y}年`}))]" @update:model-value="yearFilter=$event;submitSearch()"/></div>
      <div class="album-filter"><SkySelect id="album-author" label="作者" :model-value="authorFilter" :options="[{value:'',label:'所有作者'},...authorMembers.map(member=>({value:String(member.userId),label:member.displayName}))]" @update:model-value="authorFilter=$event;submitSearch()"/></div></div>
      <SegmentedControl class="album-view-switch" :model-value="view" :options="[{value:'grid',label:'网格视图'},{value:'timeline',label:'时间轴'}]" label="回忆浏览方式" @update:model-value="switchView"/>
    </div>
    <p v-if="authorError" class="album-results" role="status">{{authorError}} <button @click="loadAuthors">重新载入</button></p>
    <p v-if="keyword||yearFilter||authorFilter" class="album-results" role="status">找到 {{memories.total}} 段回忆 <button @click="keyword='';yearFilter='';authorFilter='';submitSearch()">清除筛选</button></p>
    <div v-if="loadError" class="empty-state" role="alert"><p>暂时没能翻开这些记忆</p><p class="text-sm">{{ loadError }}</p><button class="sky-primary rounded-full bg-night-action px-5 py-2.5 text-sm text-night-action-ink" @click="loadFirstPage">重新加载</button></div>
    <div v-else-if="memories.loading && !memories.items.length" class="memory-grid">
      <div v-for="n in (isDesktop ? desktopPageSize : mobilePageSize)" :key="n" class="h-[440px] animate-pulse rounded-2xl bg-night-soft"></div>
    </div>

    <template v-else-if="memories.items.length">
      <MemoryWall v-if="view==='grid'"><MemoryCard v-for="item in memories.items" :key="item.id" :memory="item"/></MemoryWall>
      <div v-else class="album-timeline" aria-label="按月份浏览回忆"><section v-for="group in monthGroups" :key="group.key" class="album-month"><header><span class="album-month-node"></span><h2>{{group.year}} 年 <strong>{{group.month}} 月</strong></h2><span>{{group.items.length}} 段已显示</span></header><div class="album-timeline-cards"><MemoryCard v-for="item in group.items" :key="item.id" :memory="item"/></div></section></div>
    </template>

    <div v-else class="rounded-2xl border border-dashed border-night-line-strong bg-night-surface/70 px-6 py-16 text-center">
      <div class="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-night-accent/10 text-night-accent"><Plus :size="20"/></div>
      <h2 class="mt-4 font-serif text-xl font-bold text-night-ink">从第一段故事开始</h2>
      <p class="mt-2 text-xs text-night-muted">从一件今天发生的小事开始吧。</p><button @click="openComposer" class="sky-primary mt-5 rounded-full bg-night-action px-5 py-3 text-sm font-semibold text-night-action-ink">记录第一段记忆</button>
    </div>

    <nav v-if="isDesktop && totalPages > 1" class="flex items-center justify-center gap-2" aria-label="首页分页">
      <button :disabled="page===1 || memories.loading" @click="goToPage(page-1)" class="grid h-10 w-10 place-items-center rounded-xl border border-night-line bg-night-surface text-night-secondary transition hover:border-night-accent/40 hover:text-night-accent disabled:opacity-35"><ChevronLeft :size="17"/></button>
      <button v-for="number in totalPages" :key="number" :aria-current="number===page?'page':undefined" @click="goToPage(number)" class="grid h-10 min-w-10 place-items-center rounded-xl px-2 text-xs font-semibold transition" :class="number===page?'album-page-current':'border border-night-line bg-night-surface text-night-ink hover:border-night-accent/40'">{{ number }}</button>
      <button :disabled="page===totalPages || memories.loading" @click="goToPage(page+1)" class="grid h-10 w-10 place-items-center rounded-xl border border-night-line bg-night-surface text-night-secondary transition hover:border-night-accent/40 hover:text-night-accent disabled:opacity-35"><ChevronRight :size="17"/></button>
    </nav>

    <div v-if="!isDesktop" ref="loadMoreTrigger" class="flex h-20 items-center justify-center text-xs text-night-muted">
      <span v-if="loadingMore" class="flex items-center gap-2"><LoaderCircle :size="16" class="animate-spin"/>正在翻出更多记忆</span>
      <button v-else-if="loadMoreError" class="sky-secondary" @click="loadMore().then(()=>{if(!loadMoreError)observeLoadMore()})">加载失败，点击重试</button>
      <span v-else-if="!hasMore && memories.items.length">已经看到全部记忆了</span>
    </div>
  </section>
</template>

<style scoped>
.album-page{width:100%;max-width:1856px;margin-inline:auto}.album-page :deep(.sky-page-title-line){display:flex;align-items:center;gap:16px;flex-wrap:wrap}.album-page :deep(.page-title){font-family:Georgia,"Songti SC",serif;font-size:40px;font-weight:600;letter-spacing:.04em}.album-stat{font-size:11px;color:#e2b873;padding:5px 12px;border:1px solid #d3a55c33;border-radius:30px;background:#d6a65e0d;white-space:nowrap}.album-page :deep(.sky-page-header p){font-size:13px;color:#aaa3af}
.album-toolbar{display:flex;justify-content:space-between;align-items:center;gap:18px;padding:15px 16px;background:#20212b88;border:1px solid #c4b6ca1a;border-radius:15px}.album-filters{display:flex;align-items:center;gap:12px;flex:1;min-width:0;flex-wrap:wrap}.album-search{display:flex;align-items:center;gap:9px;min-width:200px;flex:1;max-width:450px;padding:0 11px;background:#171a2580;border:1px solid #c7bac721;border-radius:10px;color:#9993a1;height:38px}.album-search:focus-within{border-color:#d3af7459}.album-search input{background:none!important;border:0!important;box-shadow:none!important;outline:none!important;min-width:0;width:100%;padding:7px 0!important;font-size:12px!important}.album-search button{display:grid;place-items:center;min-width:28px;min-height:34px}.album-filter{width:118px}.album-filter :deep(.u031-lab){position:absolute;width:1px;height:1px;clip-path:inset(50%);overflow:hidden}.album-filter :deep(.u031-btn){height:38px;min-height:38px;background:#171a2580;box-shadow:inset 0 0 0 1px #c7bac721}.album-filter :deep(.u031-v b){font-size:12px}.album-view-switch{flex-shrink:0}.album-view-switch :deep(.u039-tab){height:32px;font-size:12px;padding-inline:16px}.album-view-switch :deep(.u039-pill){border:1px solid #c9974b40}
.album-page-current{background:#e2b873;color:#211c17;font-weight:650;border:1px solid #efd09e}.album-page-current:hover{background:#eac58c;color:#211c17}
.album-filter-notice,.album-results{font-size:12px;color:#b6a48e}.album-filter-notice button,.album-results button{margin-left:10px;color:#dfbd90;min-height:32px}
.memory-grid{display:grid;gap:24px;grid-template-columns:repeat(auto-fill,minmax(min(100%,265px),1fr));align-items:stretch}
.album-timeline{position:relative;border-left:1px solid #d8b7822b;margin-left:12px;padding-left:30px}.album-month{position:relative;margin-bottom:36px}.album-month>header{display:flex;gap:16px;align-items:center;margin-bottom:20px}.album-month h2{font-size:13px;color:#a49ba9}.album-month h2 strong{font-size:22px;font-weight:500;color:#eee3d2;margin-left:10px}.album-month>header>span:last-child{font-size:11px;color:#9b91a1}.album-month-node{position:absolute;left:-35px;width:9px;height:9px;border-radius:50%;background:#d6b27c;box-shadow:0 0 0 5px #d5b8850c}.album-timeline-cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,310px),1fr));gap:24px;align-items:stretch}
@media(min-width:1280px){.album-page{padding-inline:32px}}
@media(max-width:1000px){.album-toolbar{flex-wrap:wrap}.album-view-switch{margin-left:auto}.album-filters{flex-basis:100%}.album-search{max-width:none}}
@media(max-width:767px){.album-page :deep(.page-title){font-size:30px}.album-stat{font-size:10px}.album-page :deep(.sky-page-header){align-items:flex-start;flex-wrap:wrap}.album-toolbar{padding:12px;gap:12px}.album-search{flex-basis:100%;min-width:0}.album-filter{flex:1;min-width:0}.album-view-switch{width:100%}.album-timeline{margin-left:5px;padding-left:19px}.album-month-node{left:-24px}.album-timeline-cards{gap:18px}}
</style>

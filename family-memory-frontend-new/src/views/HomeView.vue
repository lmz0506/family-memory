<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {ArrowUpRight, ChevronLeft, ChevronRight, LoaderCircle, Plus, Search, X} from 'lucide-vue-next'
import {useRoute,useRouter} from 'vue-router'
import SegmentedControl from '@/components/SegmentedControl.vue'
import type {FamilyMember,Memory} from '@/types'
import {useFamilyStore} from '@/stores/family'
import {useMemoryStore} from '@/stores/memories'
import MemoryCard from '@/components/MemoryCard.vue'
import GlowCollection from '@/components/GlowCollection.vue'
import SpotlightStory from '@/components/SpotlightStory.vue'
import OrbitalScene from '@/components/OrbitalScene.vue'
import MagneticAction from '@/components/MagneticAction.vue'
import {usePageMotion} from '@/composables/usePageMotion'
import {familyApi,memoryApi} from '@/api'
import MemoryImage from '@/components/MemoryImage.vue'
import SkySelect from '@/components/SkySelect.vue'
import {readViewState,writeViewState} from '@/composables/viewState'
import {albumPageSize} from '@/composables/albumLayout'

const route=useRoute(),router=useRouter()
const view=computed(()=>route.query.view==='timeline'?'timeline':'grid')
function switchView(value:string){void router.replace({query:{...route.query,view:value}})}
const family = useFamilyStore()
const memories = useMemoryStore()
// The opening spread uses an actual story from the loaded page, never demo content.
const openingMemories=ref<Memory[]>([])
const coverStory = computed(() => {
  const source=openingMemories.value.length?openingMemories.value:memories.items
  const candidate=source.find(item => item.content?.trim() && (item.coverUrl || item.mediaUrls?.[0]) && !/\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(item.coverUrl || item.mediaUrls?.[0] || ''))
  return candidate?(memories.items.find(item=>item.id===candidate.id)||candidate):undefined
})
const featuredId = computed(() => coverStory.value?.id ?? memories.items.find(item => item.coverUrl || item.mediaUrls?.length)?.id)
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
usePageMotion(albumRoot)
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
  await Promise.all([memoryApi.years(family.current.id).then(years=>yearOptions.value=years).catch(()=>{}),loadAuthors(),memoryApi.list(family.current.id,{page:1,size:12}).then(result=>{if(alive)openingMemories.value=result.list}).catch(()=>{})])
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
    <header class="memory-hero">
      <div class="memory-hero-copy"><p class="observatory-kicker"><span></span> THE MEMORY OBSERVATORY <i>01 / 回忆</i></p><h1><span>把平凡的日子，</span><br><em>连成我们的星海。</em></h1><p class="memory-hero-description">那些被认真记住的瞬间，<br>会在漫长岁月里，继续发光。</p><div class="memory-hero-action"><MagneticAction label="点亮一段记忆" @activate="openComposer"/></div><div class="memory-hero-coordinates"><span><b>{{family.current?.memoryCount??memories.total}}</b> 颗回忆之星</span><span><b>{{family.current?.recordDays||0}}</b> 天共同轨迹</span><span class="coordinate-line"></span></div></div>
      <div class="memory-hero-exhibit"><OrbitalScene/><div v-if="coverStory&&!loadError" class="memory-featured-orbit"><SpotlightStory :memory="coverStory"/><span class="exhibit-coordinate">✦ 从一颗星，重返那一天</span></div><div v-else class="memory-exhibit-empty"><span>✦</span><p>每一段故事，<br>都有自己的光。</p></div><span class="exhibit-annotation annotation-top">{{family.current?.name||'我们的家'}} / 私人星图</span><span class="exhibit-annotation annotation-side">A MOMENT. A STAR.</span></div>
    </header>
    <div class="album-chapter" data-reveal><div><span class="chapter-number">01 /</span><h2>回忆星图 <small>THE MOMENTS WE KEEP</small></h2></div><span>{{family.current?.memoryCount??memories.total}} 段回忆 <i>·</i> {{family.current?.photoCount||0}} 个影像瞬间</span></div>
    <div class="album-toolbar" data-reveal>
      <div class="album-filters"><form class="album-search" @submit.prevent="submitSearch"><Search aria-hidden="true" :size="15"/><input v-model="keyword" name="memory-search" autocomplete="off" aria-label="搜索回忆" placeholder="搜索记忆标题…" @input="queueSearch" @compositionend="queueSearch"/><button v-if="keyword" type="button" aria-label="清除搜索" @click="keyword='';submitSearch()"><X aria-hidden="true" :size="14"/></button><button v-else type="submit" aria-label="搜索"><Search aria-hidden="true" :size="14"/></button></form>
      <div class="album-filter"><SkySelect id="album-year" label="年份" :model-value="yearFilter" :options="[{value:'',label:'所有年份'},...yearOptions.map(y=>({value:String(y),label:`${y}年`}))]" @update:model-value="yearFilter=$event;submitSearch()"/></div>
      <div class="album-filter"><SkySelect id="album-author" label="作者" :model-value="authorFilter" :options="[{value:'',label:'所有作者'},...authorMembers.map(member=>({value:String(member.userId),label:member.displayName}))]" @update:model-value="authorFilter=$event;submitSearch()"/></div></div>
      <SegmentedControl class="album-view-switch" :model-value="view" :options="[{value:'grid',label:'故事画廊'},{value:'timeline',label:'时间轴'}]" label="回忆浏览方式" @update:model-value="switchView"/>
    </div>
    <p v-if="authorError" class="album-results" role="status">{{authorError}} <button @click="loadAuthors">重新载入</button></p>
    <p v-if="keyword||yearFilter||authorFilter" class="album-results" role="status">找到 {{memories.total}} 段回忆 <button @click="keyword='';yearFilter='';authorFilter='';submitSearch()">清除筛选</button></p>
    <div v-if="loadError" class="empty-state" role="alert"><p>暂时没能翻开这些记忆</p><p class="text-sm">{{ loadError }}</p><button class="sky-primary rounded-full bg-night-action px-5 py-2.5 text-sm text-night-action-ink" @click="loadFirstPage">重新加载</button></div>
    <div v-else-if="memories.loading && !memories.items.length" class="memory-grid">
      <div v-for="n in (isDesktop ? desktopPageSize : mobilePageSize)" :key="n" class="h-[440px] animate-pulse rounded-2xl bg-night-soft"></div>
    </div>

    <template v-else-if="memories.items.length">
      <GlowCollection v-if="view==='grid'" class="editorial-wall"><MemoryCard v-for="(item,index) in memories.items" :key="item.id" :memory="item" :featured="item.id===featuredId" :data-edition="String((page-1)*desktopPageSize+index+1).padStart(2,'0')" data-reveal :style="{'--item-delay':`${Math.min(index,5)*45}ms`}"/></GlowCollection>
      <div v-else class="album-timeline" aria-label="按月份浏览回忆"><section v-for="group in monthGroups" :key="group.key" class="album-month" data-reveal><header><span class="album-month-node"></span><h2>{{group.year}} 年 <strong>{{group.month}} 月</strong></h2><span>{{group.items.length}} 段已显示</span></header><div class="album-timeline-cards"><MemoryCard v-for="item in group.items" :key="item.id" :memory="item"/></div></section></div>
    </template>

    <div v-else class="rounded-2xl border border-dashed border-night-line-strong bg-night-surface/70 px-6 py-16 text-center">
      <div class="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-night-accent/10 text-night-accent"><Plus aria-hidden="true" :size="20"/></div>
      <h2 class="mt-4 font-serif text-xl font-bold text-night-ink">从第一段故事开始</h2>
      <p class="mt-2 text-xs text-night-muted">从一件今天发生的小事开始吧。</p><button @click="openComposer" class="sky-primary mt-5 rounded-full bg-night-action px-5 py-3 text-sm font-semibold text-night-action-ink">记录第一段记忆</button>
    </div>

    <nav v-if="isDesktop && totalPages > 1" class="flex items-center justify-center gap-2" aria-label="首页分页">
      <button aria-label="上一页回忆" :disabled="page===1 || memories.loading" @click="goToPage(page-1)" class="grid h-10 w-10 place-items-center rounded-xl border border-night-line bg-night-surface text-night-secondary transition hover:border-night-accent/40 hover:text-night-accent disabled:opacity-35"><ChevronLeft aria-hidden="true" :size="17"/></button>
      <button v-for="number in totalPages" :key="number" :aria-current="number===page?'page':undefined" @click="goToPage(number)" class="grid h-10 min-w-10 place-items-center rounded-xl px-2 text-xs font-semibold transition" :class="number===page?'album-page-current':'border border-night-line bg-night-surface text-night-ink hover:border-night-accent/40'">{{ number }}</button>
      <button aria-label="下一页回忆" :disabled="page===totalPages || memories.loading" @click="goToPage(page+1)" class="grid h-10 w-10 place-items-center rounded-xl border border-night-line bg-night-surface text-night-secondary transition hover:border-night-accent/40 hover:text-night-accent disabled:opacity-35"><ChevronRight aria-hidden="true" :size="17"/></button>
    </nav>

    <div v-if="!isDesktop" ref="loadMoreTrigger" class="flex h-20 items-center justify-center text-xs text-night-muted">
      <span v-if="loadingMore" class="flex items-center gap-2"><LoaderCircle aria-hidden="true" :size="16" class="animate-spin"/>正在翻出更多记忆</span>
      <button v-else-if="loadMoreError" class="sky-secondary" @click="loadMore().then(()=>{if(!loadMoreError)observeLoadMore()})">加载失败，点击重试</button>
      <span v-else-if="!hasMore && memories.items.length">已经看到全部记忆了</span>
    </div>
  </section>
</template>

<style scoped>
.album-page{width:100%;max-width:1480px;margin:auto;padding-bottom:35px}
.memory-hero{display:grid;grid-template-columns:1fr 1fr;align-items:center;min-height:510px;gap:30px;position:relative;margin-top:-20px;border-bottom:1px solid #e2cead21;padding-bottom:24px}
.memory-hero-copy{position:relative;z-index:1;padding:38px 0 22px;min-width:0}
.observatory-kicker{display:flex;align-items:center;gap:10px;color:#c2b8a8;font-size:9px;letter-spacing:2.4px}.observatory-kicker>span{width:5px;height:5px;border-radius:50%;background:#e9c89d;box-shadow:0 0 12px #e9c89d88}.observatory-kicker i{font-style:normal;font-size:10px;margin-left:12px;color:#a0a8b9;letter-spacing:1px}
.memory-hero h1{font:400 clamp(34px,3.1vw,58px)/1.6 'Songti SC','STSong','SimSun',serif;letter-spacing:.025em;margin:26px 0 18px;color:#eee2d0;text-wrap:balance}.memory-hero h1 em{font-style:normal;background:linear-gradient(110deg,#ead6b4,#b6c5db);background-clip:text;-webkit-background-clip:text;color:transparent}
.memory-hero-description{font-size:13px;line-height:2.1;color:#b2b5c1;letter-spacing:1px}.memory-hero-action{height:112px;margin-left:-25px;display:flex;align-items:center;overflow:visible}.memory-hero-action>.u002{margin-block:-44px}
.memory-hero-coordinates{display:flex;align-items:center;gap:26px;font-size:10px;color:#989faf;letter-spacing:1px}.memory-hero-coordinates b{font-family:Georgia,serif;font-weight:400;font-size:22px;color:#cfbaa0;letter-spacing:0;margin-right:6px}.coordinate-line{height:1px;background:linear-gradient(90deg,#dcc09555,transparent);flex:1;max-width:100px}
.memory-hero-exhibit{position:relative;height:510px;min-width:0;isolation:isolate}.memory-hero-exhibit>.orbital-scene{position:absolute;inset:0;width:100%;height:100%;z-index:0}.memory-featured-orbit{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:18px;pointer-events:none}.memory-featured-orbit>.spotlight-story{pointer-events:auto;transform:rotate(3deg);animation:memory-card-arrival 1.25s var(--ux-ease) both}.exhibit-coordinate{font-size:9px;letter-spacing:2px;color:#b4ac9f}.exhibit-annotation{position:absolute;font-size:8px;letter-spacing:2px;color:#9197a8;pointer-events:none}.annotation-top{left:28px;top:10px}.annotation-side{right:-4px;top:50%;writing-mode:vertical-rl}.memory-exhibit-empty{position:absolute;inset:0;display:grid;place-content:center;text-align:center;color:#d9c5a7}.memory-exhibit-empty>span{font-size:50px;filter:drop-shadow(0 0 20px #eec59355)}.memory-exhibit-empty>p{font:24px/1.8 'Songti SC',serif;margin-top:20px}
.album-chapter{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:6px 0;margin-top:27px!important}.album-chapter>div{display:flex;align-items:center;gap:19px}.chapter-number{font:italic 15px Georgia,serif;color:#b5a185}.album-chapter h2{font:400 25px/1.5 'Songti SC',serif;letter-spacing:2px;color:#e4d8c6}.album-chapter h2 small{font:8px sans-serif;letter-spacing:2px;color:#9197a8;margin-left:17px}.album-chapter>span{font-size:10px;color:#9fa6b5}.album-chapter i{font-style:normal;padding:0 8px;color:#d0b58b}
.album-toolbar{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:16px;padding:15px 0 21px;border-bottom:1px solid #c7c4d820;margin-top:12px!important}.album-filters{display:flex;flex:1;align-items:center;gap:12px;min-width:0;flex-wrap:wrap}.album-search{display:flex;align-items:center;gap:9px;max-width:390px;min-width:200px;flex:1;height:48px;border:1px solid #bdc3d626;border-radius:14px;padding:0 12px;background:#1c263580;color:#a3afc2;transition:border-color .2s}.album-search:focus-within{border-color:#dec29b88}.album-search input{width:100%;min-width:0;background:transparent!important;border:0;outline:none;font-size:13px!important;box-shadow:none;padding:10px 0}.album-search button{display:grid;place-items:center;min-height:36px;min-width:30px;border-radius:50%}.album-search button:hover{color:#edd1a9;background:#e1c39812}.album-filter{width:140px}.album-filter :deep(.u031-btn){border-radius:13px;background:#1c263580;height:48px}.album-view-switch{flex-shrink:0}.album-results{font-size:12px;color:#c8c3bb;margin-top:15px!important}.album-results button{margin-left:14px;min-height:36px;color:#e6c69b;border-bottom:1px solid #e6c69b33}
.editorial-wall{margin-top:25px!important;overflow:hidden}.editorial-wall :deep(.memory-post){min-height:310px}.editorial-wall :deep(.memory-post.is-featured){grid-column:span 2}.editorial-wall :deep(.is-featured .memory-media-frame){aspect-ratio:2.4}.editorial-wall :deep(.memory-post-copy){padding:24px}.editorial-wall :deep(.memory-card-header){display:block}.editorial-wall :deep(.memory-author-name){display:block;margin-top:8px;max-width:100%;font-size:10px;color:#c1b19d}.editorial-wall :deep(.memory-story-link h2){white-space:normal;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}.editorial-wall :deep(.memory-excerpt){font-size:12px;line-height:1.95}.editorial-wall :deep(.is-note .memory-note-top){justify-content:flex-start;margin-bottom:25px}.editorial-wall :deep(.is-note .memory-note-top time){background:none;border:0;letter-spacing:2px;padding:0;color:#a7aabd}.editorial-wall :deep(.is-note .memory-card-surface){position:relative}.editorial-wall :deep(.memory-post.is-note::after){content:attr(data-edition);position:absolute;right:20px;bottom:55px;font:italic 100px Georgia,serif;color:#dcc7ad06;pointer-events:none}.album-page-current{background:#d9bf971c;border:1px solid #d9bf974d;color:#e5cba6}
.album-timeline{margin-top:30px!important;padding-left:27px;border-left:1px solid #d9bf9733}.album-month{position:relative;margin-bottom:36px}.album-month>header{display:flex;align-items:center;gap:14px;margin-bottom:22px}.album-month h2{font-size:16px;color:#c1c3cc}.album-month h2 strong{font:400 25px Georgia,serif;color:#e8c99f;padding-left:8px}.album-month>header>span:last-child{margin-left:auto;font-size:11px;color:#969ead}.album-month-node{position:absolute;width:7px;height:7px;border-radius:50%;background:#d7bb8e;box-shadow:0 0 13px #d7bb8e66;left:-31px;top:13px}.album-timeline-cards{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:22px}.album-timeline-cards :deep(.memory-post){padding:1.5px;border-radius:18px;background:#c8bfd52b}.album-timeline-cards :deep(.u228-in){border-radius:16.5px;background:#172131}
@keyframes memory-card-arrival{from{opacity:0;transform:translateY(24px) rotate(7deg)}to{opacity:1;transform:rotate(3deg)}}
@media(max-width:1100px){.memory-hero{grid-template-columns:1fr 1fr;gap:0;min-height:480px}.memory-hero h1{font-size:36px}.memory-hero-exhibit{height:480px}.memory-featured-orbit{transform:scale(.9)}.observatory-kicker{font-size:8px;letter-spacing:1.5px}.observatory-kicker i{display:none}.album-timeline-cards{grid-template-columns:repeat(2,minmax(0,1fr))}.album-toolbar{gap:16px}.album-filter{width:125px}}
@media(max-width:767px){.memory-hero{grid-template-columns:minmax(0,1fr);padding-top:8px;min-height:0;margin-top:0}.memory-hero-copy{padding:20px 0 15px}.memory-hero h1{font-size:clamp(32px,6vw,43px);margin-top:22px}.memory-hero-exhibit{display:none}.memory-hero-description{font-size:12px}.memory-hero-action{margin-left:-15px;height:110px}.memory-hero-coordinates{gap:20px;margin-top:4px}.album-chapter{margin-top:24px!important;flex-wrap:wrap;gap:10px}.album-chapter h2{font-size:23px}.album-chapter h2 small{display:none}.album-chapter>span{font-size:10px}.album-toolbar{margin-top:8px!important;padding-bottom:20px}.album-filters{width:100%;flex:none;gap:10px}.album-search{max-width:none;min-width:100%;height:48px}.album-filter{flex:1;width:auto}.album-view-switch{margin-left:auto}.album-timeline-cards{grid-template-columns:minmax(0,1fr)}.album-month h2{font-size:14px}.album-month h2 strong{font-size:21px}}
@media(max-width:700px){.editorial-wall :deep(.memory-post.is-featured){grid-column:auto}.editorial-wall :deep(.is-featured .memory-media-frame){aspect-ratio:16/9}}
@media(prefers-reduced-motion:reduce){.memory-featured-orbit>.spotlight-story{animation:none}.album-search{transition:none}}
</style>

<script setup lang="ts">
import {computed, nextTick, defineAsyncComponent, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {MapPin, Search, Sparkles, UserRound} from 'lucide-vue-next'
import {useFamilyStore} from '@/stores/family'
import {useMemoryStore} from '@/stores/memories'
import SkyPageHeader from '@/components/SkyPageHeader.vue'
import SkySelect from '@/components/SkySelect.vue'
import {readViewState,writeViewState} from '@/composables/viewState'
import TimelineModeSwitch from '@/components/TimelineModeSwitch.vue'
import MemoryImage from '@/components/MemoryImage.vue'
import type {Memory} from '@/types'

type TimelineMode = 'stars' | 'classic'
const StarTimeline = defineAsyncComponent(() => import('@/components/StarTimeline.vue'))
const route = useRoute()
const family = useFamilyStore()
const store = useMemoryStore()
const selectedYear = ref<number | null>(null)
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const savedMode = localStorage.getItem('family-memory-timeline-mode')
const mode = ref<TimelineMode>(savedMode === 'stars' ? 'stars' : 'classic')
const timelineRoot=ref<HTMLElement>(),activeMonth=ref(''),revealedMonths=ref(new Set<string>()),timelineMotion=ref(false)
let monthObserver:IntersectionObserver|undefined,scrollHost:HTMLElement|null=null,scrollFrame=0
function updateActiveMonth(){cancelAnimationFrame(scrollFrame);scrollFrame=requestAnimationFrame(()=>{const sections=Array.from(timelineRoot.value?.querySelectorAll<HTMLElement>('[data-month]')||[]),anchor=(scrollHost?.getBoundingClientRect().top||0)+155;let active=sections[0]?.dataset.month||'';for(const section of sections)if(section.getBoundingClientRect().top<=anchor)active=section.dataset.month||active;activeMonth.value=active})}
async function observeMonths(){await nextTick();monthObserver?.disconnect();scrollHost?.removeEventListener('scroll',updateActiveMonth);if(mode.value!=='classic')return;scrollHost=document.querySelector<HTMLElement>('[data-app-scroll]');scrollHost?.addEventListener('scroll',updateActiveMonth,{passive:true});const sections=Array.from(timelineRoot.value?.querySelectorAll<HTMLElement>('[data-month]')||[]);if(typeof IntersectionObserver==='undefined'){revealedMonths.value=new Set(sections.map(n=>n.dataset.month!));return}monthObserver=new IntersectionObserver(entries=>{for(const entry of entries)if(entry.isIntersecting){revealedMonths.value=new Set([...revealedMonths.value,(entry.target as HTMLElement).dataset.month!]);monthObserver?.unobserve(entry.target)}},{root:scrollHost,rootMargin:'0px 0px -30px 0px',threshold:.02});sections.forEach(section=>monthObserver?.observe(section));updateActiveMonth()}
function goMonth(key:string){const section=Array.from(timelineRoot.value?.querySelectorAll<HTMLElement>('[data-month]')||[]).find(n=>n.dataset.month===key);if(!section||!scrollHost)return;const top=section.getBoundingClientRect().top-scrollHost.getBoundingClientRect().top+scrollHost.scrollTop-140;scrollHost.scrollTo({top,behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'})}
const normalLoaded = ref(false),normalPage=ref(1),normalError=ref('')
const normalPages=computed(()=>Math.ceil(store.total/50))
async function changeNormalPage(page:number){if(page<1||page>normalPages.value)return;normalPage.value=page;await loadNormal();document.querySelector('[data-app-scroll]')?.scrollTo({top:0})}
function searchNormal(){normalPage.value=1;void loadNormal()}
function changeMode(value:string){setMode(value as TimelineMode)}

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
  normalError.value=''
  try{await store.load(family.current.id, {
    page: normalPage.value,
    size: 50,
    year: selectedYear.value || undefined,
    keyword: keyword.value.trim() || undefined
  })
  normalLoaded.value = true;void observeMonths()}catch(e){normalError.value=(e as Error).message}
}

async function setMode(next: TimelineMode) {
  mode.value = next
  localStorage.setItem('family-memory-timeline-mode', next)
  if (next === 'classic' && !normalLoaded.value && family.current) await Promise.all([loadNormal(),store.loadHighlights(family.current!.id)])
  else await observeMonths()
}

async function selectYear(year: number | null) {
  if (selectedYear.value === year) return
  selectedYear.value = year;normalPage.value=1
  await loadNormal()
}

onMounted(async () => {
  timelineMotion.value=!window.matchMedia('(prefers-reduced-motion: reduce)').matches
  window.addEventListener('family-memory-created',handleCreated)
  if (!family.current) await family.load()
  if (!family.current) return
  const saved=readViewState<{year:number|null;keyword:string;page:number;scroll:number}>(`timeline:${family.current.id}`);if(saved){selectedYear.value=saved.year;keyword.value=saved.keyword;normalPage.value=saved.page}
  if (mode.value === 'classic') await Promise.all([loadNormal(),store.loadHighlights(family.current.id)])
  if(saved)requestAnimationFrame(()=>document.querySelector('[data-app-scroll]')?.scrollTo({top:saved.scroll}))
})
function handleCreated(){if(mode.value==='classic')void loadNormal()}
onBeforeUnmount(()=>{monthObserver?.disconnect();scrollHost?.removeEventListener('scroll',updateActiveMonth);cancelAnimationFrame(scrollFrame);if(family.current)writeViewState(`timeline:${family.current.id}`,{year:selectedYear.value,keyword:keyword.value,page:normalPage.value,scroll:document.querySelector('[data-app-scroll]')?.scrollTop||0});window.removeEventListener('family-memory-created',handleCreated)})

watch(() => route.query.keyword, async value => {
  keyword.value = typeof value === 'string' ? value : '';normalPage.value=1
  if (mode.value==='classic') await loadNormal()
})
</script>

<template>
  <section ref="timelineRoot" :class="mode==='stars'?'timeline-stars':'timeline-page page-reveal'">
    <SkyPageHeader v-if="mode==='classic'" title="时光轴" description="沿着光与时间，回看家里的故事。"><div class="timeline-heading-tools"><form class="sky-toolbar-search" @submit.prevent="searchNormal"><Search :size="15"/><input v-model="keyword" aria-label="搜索时间轴记忆" placeholder="寻找一段回忆…"/><button aria-label="搜索"><Search :size="15"/></button></form><div class="sky-year-filter"><SkySelect id="timeline-year" label="年份" :model-value="selectedYear===null?'':String(selectedYear)" :options="[{value:'',label:'所有年份'},...store.years.map(y=>({value:String(y),label:`${y}年`}))]" @update:model-value="selectYear($event?Number($event):null)"/></div><TimelineModeSwitch :model-value="mode" @update:model-value="changeMode"/></div></SkyPageHeader>
    <div v-if="mode==='stars'&&family.current" class="relative h-full overflow-hidden bg-[#030611]"><StarTimeline :key="family.current.id" :family-id="family.current.id" :initial-keyword="keyword"><template #mode><TimelineModeSwitch theme="dark" :model-value="mode" @update:model-value="changeMode"/></template></StarTimeline></div>
    <template v-else>
      <nav v-if="groups.length" class="timeline-month-nav" aria-label="当前月份与月份定位"><span><i></i>{{activeMonth?`${activeMonth.slice(0,4)}年 ${Number(activeMonth.slice(5))}月`:'时光正在展开'}}</span><div><button v-for="group in groups" :key="group.key" :aria-current="activeMonth===group.key?'date':undefined" @click="goMonth(group.key)">{{group.year}} · {{group.month}}月</button></div></nav>
      <div v-if="normalError" class="empty-state" role="alert"><p>{{normalError}}</p><button class="sky-secondary" @click="loadNormal">重新加载</button></div>
      <div v-else-if="store.loading" class="timeline-records timeline-skeletons"><div v-for="n in 4" :key="n" class="h-44 animate-pulse rounded-2xl bg-night-surface"></div></div>
      <div v-else-if="groups.length" class="timeline-groups" :class="{'timeline-motion':timelineMotion}">
        <section v-for="group in groups" :key="group.key" class="timeline-month" :data-month="group.key" :class="{'is-current':activeMonth===group.key,'is-revealed':revealedMonths.has(group.key)}"><header><h2>{{group.month}}月</h2><span>{{group.year}}</span><small>{{group.items.length}} 段回忆</small></header><div class="timeline-records">
          <RouterLink v-for="(item,index) in group.items" :key="item.id" :to="{path:`/memories/${item.id}`,query:{from:'timeline'}}" class="timeline-record" :class="{'has-media':mediaUrl(item)}" :style="{'--record-delay':`${Math.min(index,5)*55}ms`}"><time class="timeline-day" :datetime="item.memoryDate"><strong>{{Number(item.memoryDate.slice(8))}}</strong><span>{{Number(item.memoryDate.slice(5,7))}}月</span></time><div v-if="mediaUrl(item)" class="timeline-thumbnail"><video v-if="isVideo(mediaUrl(item))" :src="mediaUrl(item)" muted playsinline preload="metadata"></video><MemoryImage v-else :src="mediaUrl(item)" :alt="item.title" compact/></div><div class="timeline-story"><span v-if="item.tags?.length" class="timeline-tag">{{item.tags[0]}}</span><h3>{{item.title}}</h3><p v-if="item.content&&item.content!==item.title">{{item.content}}</p><footer><span><UserRound :size="12"/>{{item.authorName}}</span><span v-if="item.location"><MapPin :size="12"/>{{item.location}}</span></footer></div></RouterLink>
        </div></section>
      </div>
      <div v-else class="empty-state">没有找到符合条件的记忆，可以调整关键词或年份。</div>
      <nav v-if="normalPages>1" class="sky-timeline-pages" aria-label="时光轴分页"><button class="sky-secondary" :disabled="normalPage<=1||store.loading" @click="changeNormalPage(normalPage-1)">上一页</button><span>{{normalPage}} / {{normalPages}} · {{store.total}} 段回忆</span><button class="sky-secondary" :disabled="normalPage>=normalPages||store.loading" @click="changeNormalPage(normalPage+1)">下一页</button></nav>
    </template>
  </section>
</template>
<style scoped>
.timeline-stars{height:100%;min-height:0}.timeline-page{width:100%;max-width:none}.timeline-heading-tools{display:flex;align-items:center;justify-content:flex-end;flex-wrap:wrap;gap:12px}.timeline-month-nav{position:relative;z-index:1;display:flex;align-items:center;gap:22px;padding:10px 14px;margin:12px 0 25px;border-radius:10px;background:linear-gradient(110deg,#29252df7,#202230f7);border-bottom:1px solid #e2b87320}.timeline-month-nav>span{display:flex;align-items:center;gap:9px;font-size:13px;color:#e2c497;white-space:nowrap}.timeline-month-nav i{width:6px;height:6px;border-radius:50%;background:#e2b873;box-shadow:0 0 10px #e2b87355}.timeline-month-nav>div{display:flex;gap:6px;overflow-x:auto;scrollbar-width:none}.timeline-month-nav button{padding:7px 12px;white-space:nowrap;font-size:12px;border-radius:8px;color:#a69f94}.timeline-month-nav button[aria-current]{background:#e2b87310;color:#e2b873}.timeline-groups{position:relative;padding-left:35px}.timeline-groups:before{content:"";position:absolute;top:12px;bottom:20px;left:7px;width:1px;background:linear-gradient(#e2b87350,#e2b87312)}.timeline-month{position:relative;margin-bottom:36px}.timeline-month>header{display:flex;align-items:baseline;gap:11px;margin-bottom:18px}.timeline-month>header:before{content:"";position:absolute;left:-32px;top:10px;width:9px;height:9px;border-radius:50%;background:#29272a;border:1px solid #e2b87370;transition:background .2s,box-shadow .2s}.timeline-month.is-current>header:before{background:#e2b873;box-shadow:0 0 15px #e2b87345}.timeline-month h2{font-size:23px;font-weight:500;color:#eee5d8}.timeline-month>header>span{color:#a59b8c;font-size:13px}.timeline-month>header>small{margin-left:auto;font-size:11px;color:#948d82}.timeline-records{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:17px;align-items:start}.timeline-record{display:grid;grid-template-columns:52px minmax(0,1fr);gap:15px;min-width:0;min-height:172px;align-content:start;align-items:start;padding:17px;border:1px solid #e2b87316;border-radius:13px;background:linear-gradient(140deg,#2b2b36,#20232e);box-shadow:inset 0 1px 0 #ffffff08,0 6px 18px #080a121f;transition:border-color .2s,transform .2s}.timeline-record.has-media{grid-template-columns:48px minmax(100px,160px) minmax(0,1fr)}.timeline-record:hover{border-color:#e2b87355;transform:translateY(-2px)}.timeline-day{align-self:start;text-align:center;padding-top:3px}.timeline-day strong{display:block;font-size:32px;line-height:1.1;font-weight:400;color:#d4b483;font-variant-numeric:tabular-nums}.timeline-day>span{display:block;margin-top:6px;color:#a39c90;font-size:11px}.timeline-thumbnail{position:relative;min-width:0;height:136px;overflow:hidden;border-radius:8px;background:#16171b}.timeline-thumbnail :deep(.u258-tl){position:absolute;inset:0;height:100%}.timeline-thumbnail video{width:100%;height:100%;object-fit:cover}.timeline-story{min-width:0;min-height:136px;display:flex;flex-direction:column;align-items:flex-start}.timeline-tag{font-size:10px;color:#c3ad87;margin-bottom:6px}.timeline-story h3{font-size:17px;font-weight:550;color:#eee5d9;line-height:1.6;overflow-wrap:anywhere;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.timeline-story>p{font-size:13px;color:#b0aba3;line-height:1.8;margin-top:7px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.timeline-story footer{margin-top:auto;padding-top:12px;display:flex;flex-wrap:wrap;gap:8px 14px;color:#a0988c;font-size:11px}.timeline-story footer>span{display:flex;align-items:center;gap:4px;max-width:100%;overflow:hidden;text-overflow:ellipsis}.sky-timeline-pages{display:flex;align-items:center;justify-content:center;gap:16px;font-size:12px;color:var(--ux-ink-2);margin:24px 0}.timeline-motion .timeline-month.is-revealed .timeline-record{animation:timeline-card-enter .5s ease-out both;animation-delay:var(--record-delay)}.timeline-motion .timeline-month:not(.is-revealed) .timeline-record{opacity:0;transform:translateY(20px)}@keyframes timeline-card-enter{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}
@media(max-width:1050px){.timeline-record.has-media{grid-template-columns:40px 90px minmax(0,1fr);gap:10px;padding:13px}}@media(max-width:767px){.timeline-records{grid-template-columns:1fr}.timeline-heading-tools{justify-content:flex-start;width:100%}.timeline-month-nav{gap:12px;flex-wrap:wrap}.timeline-month-nav>div{min-width:0;max-width:100%}.timeline-groups{padding-left:25px}.timeline-groups:before{left:3px}.timeline-month>header:before{left:-25px}.timeline-day strong{font-size:28px}.timeline-record{min-height:155px}.timeline-thumbnail{height:116px}.timeline-story{min-height:116px}.timeline-record.has-media{grid-template-columns:34px 85px minmax(0,1fr)}}
@media(prefers-reduced-motion:reduce){.timeline-motion .timeline-month .timeline-record{animation:none;opacity:1;transform:none;transition:none}}
</style>

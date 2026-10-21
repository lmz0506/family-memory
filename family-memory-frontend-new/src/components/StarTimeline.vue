<script setup lang="ts">
import {computed,nextTick,onBeforeUnmount,onMounted,ref,shallowRef,watch} from 'vue'
import {ArrowLeft,ArrowUpRight,ChevronLeft,ChevronRight,Eye,EyeOff,Home,LoaderCircle,MapPin,Pause,Play,RotateCcw,Search,Sparkles,X,ZoomIn,ZoomOut,PanelRightOpen} from 'lucide-vue-next'
import {useRouter,useRoute} from 'vue-router'
import {memoryApi,universeApi} from '@/api'
import type {Memory,UniverseOverview,UniverseItem,UniversePage,UniverseSourceType} from '@/types'
import MemoryImage from './MemoryImage.vue'
import UniverseBreadcrumb from './UniverseBreadcrumb.vue'
import {useDialog} from '@/composables/useDialog'
import {UniverseScene,type ProjectedStar} from '@/three/UniverseScene'
import {UniversePreloader} from '@/three/UniversePreloader'
import {buildOverviewProgressively,buildContentNodes,itemPathKeys,levelNodes,locationKey,type UniverseLocation,type UniverseNode} from '@/three/memoryUniverse'
import '@/styles/universe.css'

const props=defineProps<{familyId:number;initialKeyword?:string}>(),router=useRouter(),route=useRoute(),PAGE_SIZE=24
const canvasHost=ref<HTMLElement>(),stage=ref<HTMLElement>(),readerPanel=ref<HTMLElement>()
const loading=ref(true),overviewReady=ref(false),error=ref('')
const overview=shallowRef<UniverseOverview>({memoryCount:0,capsuleCount:0,years:[]})
const nodes=shallowRef<UniverseNode[]>([]),projected=shallowRef<ProjectedStar[]>([])
const searchPage=shallowRef<UniversePage|null>(null)
const location=ref<UniverseLocation>({level:'universe'}),hovered=ref<string|null>(null),selectedStar=ref<string|null>(null)
const paused=ref(false),reduced=ref(false),immersive=ref(false),lowQuality=ref(false),filterOpen=ref(false),navigationOpen=ref(true)
const keyword=ref(props.initialKeyword||''),mediaType=ref('all'),filtering=ref(false),filterError=ref(''),appliedFilter=ref('')
const matches=shallowRef<Set<number>|null>(null),matchingKeys=shallowRef<Set<string>|null>(null)
const monthLoading=ref(false),monthError=ref(''),memoryPage=ref(0)
const selectedMemoryId=ref<number|null>(null),detailLoading=ref(false),detailError=ref(''),photoIndex=ref(0)
const detailCache=shallowRef(new Map<number,Memory>())
const motionQuery=window.matchMedia('(prefers-reduced-motion: reduce)')
let world:UniverseScene|undefined,destroyed=false,initVersion=0,filterVersion=0,detailVersion=0,appWasInert=false
let allOverviewNodes:UniverseNode[]=[],stagedKeys=new Set<string>(),initAbort:AbortController|undefined,searchAbort:AbortController|undefined
let appliedQuery:{keyword:string;sourceType?:UniverseSourceType}={keyword:''}
let preloader:UniversePreloader|undefined,sceneFrame=0

const memoryTotal=computed(()=>overview.value.memoryCount),capsuleTotal=computed(()=>overview.value.capsuleCount),total=computed(()=>memoryTotal.value+capsuleTotal.value)
const currentKey=computed(()=>locationKey(location.value)),candidates=computed(()=>levelNodes(nodes.value,location.value))
const breadcrumb=computed(()=>{const v=location.value,path:Array<{label:string;location:UniverseLocation}>=[{label:'全景',location:{level:'universe'}}];if(v.year)path.push({label:`${v.year}年`,location:{level:'year',year:v.year}});if(v.month)path.push({label:`${v.month}月`,location:{level:'month',year:v.year,month:v.month}});if(v.day)path.push({label:`${v.day}日`,location:{...v}});return path})
const visibleLabels=computed(()=>projected.value.filter(item=>item.visible&&item.node.count&&isMatch(item.node)))
const nodeMap=computed(()=>new Map(nodes.value.map(node=>[node.key,node])))
const contextLabelKeys=computed(()=>{const keys=new Set<string>();for(const key of [hovered.value,selectedStar.value]){let node=key?nodeMap.value.get(key):undefined;const seen=new Set<string>();while(node&&!seen.has(node.key)){seen.add(node.key);if(node.kind!=='memory'&&node.kind!=='capsule')keys.add(node.key);node=node.parent?nodeMap.value.get(node.parent):undefined}}return keys})
const datePlacements=computed(()=>{
 const placements=new Map<string,{left:string;top:string}>(),occupied:Array<{x:number;y:number;w:number;h:number}>=[]
 const width=stage.value?.clientWidth||375,height=stage.value?.clientHeight||600
 for(const item of visibleLabels.value.filter(item=>contextLabelKeys.value.has(item.node.key))){const w=dateLabel(item.node).length*13+10,h=24;let best={dx:0,dy:13,score:Infinity,rect:{x:0,y:0,w,h}}
  for(const [dx,dy] of [[0,13],[0,-30],[40,5],[-40,5],[0,38],[0,-54]]){const x=Math.max(5,Math.min(width-w-5,item.x+dx!-w/2)),y=Math.max(8,Math.min(height-h-8,item.y+dy!)),rect={x,y,w,h},collision=occupied.some(b=>x<b.x+b.w+3&&x+w+3>b.x&&y<b.y+b.h+3&&y+h+3>b.y),score=(collision?1000:0)+Math.abs(dx!)+Math.abs(dy!-13);if(score<best.score)best={dx:x+w/2-item.x,dy:y-item.y,score,rect}}
  occupied.push(best.rect);placements.set(item.node.key,{left:`calc(50% + ${best.dx}px)`,top:`calc(50% + ${best.dy}px)`})
 }return placements
})
const hoveredMemoryNode=computed(()=>visibleLabels.value.find(item=>item.node.key===hovered.value&&(item.node.kind==='memory'||item.node.kind==='capsule')))
const memoryPreviewStyle=computed(()=>{const item=hoveredMemoryNode.value;return {left:`${Math.max(12,Math.min((stage.value?.clientWidth||375)-240,(item?.x||0)-105))}px`,top:`${Math.max(175,Math.min((stage.value?.clientHeight||600)-125,(item?.y||0)>310?(item?.y||0)-125:(item?.y||0)+40))}px`}})
const selectedLeaf=computed(()=>nodes.value.find(node=>node.memoryId===selectedMemoryId.value&&node.kind==='memory'))
const readerNode=computed(()=>selectedLeaf.value)
const dayNodes=computed(()=>nodes.value.filter(node=>node.kind==='memory'&&node.year===selectedLeaf.value?.year&&node.month===selectedLeaf.value?.month&&node.day===selectedLeaf.value?.day))
const reading=computed(()=>selectedMemoryId.value!==null),memory=computed(()=>selectedMemoryId.value===null?undefined:detailCache.value.get(selectedMemoryId.value))
const media=computed(()=>Array.from(new Set([...(memory.value?.mediaUrls||[]),memory.value?.coverUrl].filter((value):value is string=>Boolean(value)))))
const chosenIndex=computed(()=>dayNodes.value.findIndex(node=>node.memoryId===selectedMemoryId.value)),readerChoices=computed(()=>dayNodes.value.slice(Math.floor(Math.max(0,chosenIndex.value)/8)*8,Math.floor(Math.max(0,chosenIndex.value)/8)*8+8))
const matchingCandidates=computed(()=>candidates.value.filter(node=>node.count&&isMatch(node)))
const railNodes=computed(()=>location.value.level==='day'?matchingCandidates.value.slice(memoryPage.value*PAGE_SIZE,(memoryPage.value+1)*PAGE_SIZE):matchingCandidates.value)
const memoryPages=computed(()=>location.value.level==='day'?Math.ceil(matchingCandidates.value.length/PAGE_SIZE):0)
const levelTitle=computed(()=>({universe:'选择年份，走近一片记忆',year:`${location.value.year}年 · 选择月份`,month:`${location.value.year}年${location.value.month}月 · 选择日期`,day:`${location.value.month}月${location.value.day}日 · 共${nodeMap.value.get(currentKey.value)?.count||0}项内容`})[location.value.level])
const searchPages=computed(()=>Math.ceil((searchPage.value?.total||0)/PAGE_SIZE))
function isMatch(node:UniverseNode){return matches.value===null||Boolean(matchingKeys.value?.has(node.key))||node.ids.some(id=>matches.value!.has(id))}
function dateLabel(node:UniverseNode){return node.kind==='year'?`${node.year}年`:node.kind==='month'?`${node.month}月`:`${node.day}日`}
function contentCountLabel(node:UniverseNode){return [node.memoryCount?`${node.memoryCount}段记忆`:'',node.capsuleCount?`${node.capsuleCount}份珍藏`:''].filter(Boolean).join('、')||'暂无内容'}
function label(node:UniverseNode){return node.kind==='year'?`${node.year}`:node.kind==='month'?`${node.month}月`:node.kind==='day'?`${node.day}日`:node.title||(node.kind==='capsule'?'一份珍藏':'一段记忆')}
function accessibleLabel(node:UniverseNode){return node.kind==='year'?`探索${node.year}年，${contentCountLabel(node)}`:node.kind==='month'?`探索${node.month}月，${contentCountLabel(node)}`:node.kind==='day'?`打开${node.month}月${node.day}日，${contentCountLabel(node)}`:node.kind==='capsule'?`查看珍藏：${label(node)}`:`阅读记忆：${label(node)}`}
function isVideo(url:string){return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url)}
function thumbnail(node:UniverseNode){const item=detailCache.value.get(node.memoryId!);const url=item?.coverUrl||item?.mediaUrls?.[0];return url&&!isVideo(url)?url:''}
function hideBrokenThumbnail(event:Event){(event.target as HTMLImageElement).style.display='none'}
function closeReader(){const id=selectedMemoryId.value;selectedMemoryId.value=null;detailVersion++;detailLoading.value=false;detailError.value='';if(id!==null)nextTick(()=>stage.value?.querySelector<HTMLButtonElement>(`[data-node-key="memory:${id}"]`)?.focus())}
useDialog(reading,readerPanel,closeReader)
watch(reading,value=>world?.setReading(value));watch(selectedMemoryId,value=>world?.selectMemory(value));watch(paused,value=>world?.setPaused(value));watch(lowQuality,value=>world?.setLowQuality(value));watch(hovered,value=>{world?.highlight(value);const node=value?nodeMap.value.get(value):undefined;if(node)preloader?.prioritize(node.year)})
function closeNavigation(){navigationOpen.value=false;nextTick(()=>stage.value?.querySelector<HTMLButtonElement>('[data-navigation-toggle]')?.focus())}
function toggleNavigation(){navigationOpen.value=!navigationOpen.value}
function toggleFilter(){filterOpen.value=!filterOpen.value}
function clearStarSelection(){hovered.value=null;selectedStar.value=null;world?.clearSelection()}
function focusStar(event:FocusEvent,key:string){if((event.target as HTMLElement).matches(':focus-visible')){world?.holdHover();hovered.value=key}}
function hoverAt(event:PointerEvent,node:UniverseNode){world?.requestHover(world.nodeAt(event)?.key||node.key)}
function activateAt(event:MouseEvent,node:UniverseNode){void activate(event.detail?world?.nodeAt(event)||node:node)}
watch(()=>props.initialKeyword,value=>{keyword.value=value||'';if(overviewReady.value){if(value?.trim())void applyFilters();else if(matches.value)clearFilters()}})
watch(immersive,async value=>{const app=document.getElementById('app');if(!value){if(app)app.inert=appWasInert;return}appWasInert=Boolean(app?.inert);await nextTick();if(!immersive.value||destroyed)return;if(app)app.inert=true;stage.value?.querySelector<HTMLButtonElement>('.immersive-exit')?.focus()})

/** 将后台已缓存星点加入固定概要布局，切换日期不会删除其他星点。 */
function syncScene(){
 const items=new Map<number,UniverseItem>(preloader?.items)
 for(const item of searchPage.value?.items||[])items.set(item.indexId,item)
 const leafItems=[...items.values()],paths=itemPathKeys(leafItems)
 for(const key of paths)stagedKeys.add(key)
 const base=allOverviewNodes.filter(node=>stagedKeys.has(node.key))
 nodes.value=[...base,...buildContentNodes(leafItems,allOverviewNodes)]
 world?.setNodes(nodes.value);world?.setMatches(matches.value,matchingKeys.value)
}
/** 合并同一帧的数据补入，不移动镜头。 */
function scheduleScene(){if(sceneFrame||destroyed)return;sceneFrame=requestAnimationFrame(()=>{sceneFrame=0;if(!destroyed){syncScene();updateLoadingState()}})}
function updateLoadingState(){
 const year=location.value.year
 monthError.value=year?preloader?.errorFor(year)||'':''
 const day=nodeMap.value.get(currentKey.value),loaded=location.value.level==='day'?levelNodes(nodes.value,location.value).length:0
 monthLoading.value=Boolean(location.value.level==='day'&&year&&!preloader?.isComplete(year)&&!monthError.value&&loaded<(day?.count||0))
}
async function initialize(){
 const version=++initVersion;initAbort?.abort();initAbort=new AbortController();loading.value=true;overviewReady.value=false;error.value=''
 preloader?.dispose();preloader=undefined;cancelAnimationFrame(sceneFrame);sceneFrame=0
 world?.dispose();world=undefined;nodes.value=[];allOverviewNodes=[];stagedKeys=new Set()
 await nextTick();if(!canvasHost.value||destroyed)return
 try{
  world=new UniverseScene(canvasHost.value,[],{project:value=>projected.value=value,hover:value=>hovered.value=value,pick:node=>{void activate(node)},blank:clearStarSelection,failed:()=>error.value='图形渲染暂时中断，请重新打开星海。'})
  world.setReduced(reduced.value);world.setPaused(paused.value);world.setLowQuality(lowQuality.value)
  const response=await universeApi.overview(props.familyId,initAbort.signal)
  if(destroyed||version!==initVersion)return
  overview.value=response;overviewReady.value=true;let framed=false
  await buildOverviewProgressively(response,(batch,all)=>{
   allOverviewNodes=all;for(const node of batch)stagedKeys.add(node.key)
   syncScene()
   if(!framed){world?.setOverviewBounds(all);world?.setLevel(levelNodes(nodes.value,{level:'universe'}),undefined,!world?.hasUserGesture());framed=true}
  },()=>destroyed||version!==initVersion)
  if(destroyed||version!==initVersion)return
  preloader=new UniversePreloader((year,afterId,signal)=>universeApi.year(props.familyId,year,afterId,120,signal),()=>{if(version===initVersion)scheduleScene()})
  preloader.start(response.years.map(item=>item.year))
  if(location.value.year)preloader.prioritize(location.value.year)
  if(props.initialKeyword?.trim() && version===initVersion && !destroyed){keyword.value=props.initialKeyword;await applyFilters()}
 }catch(e){if(!destroyed&&version===initVersion)error.value=(e as Error).message}
 finally{if(version===initVersion)loading.value=false}
}
async function navigate(next:UniverseLocation,select=false){
 closeReader();filterOpen.value=false;hovered.value=null;location.value=next;memoryPage.value=0
 if(next.year)preloader?.prioritize(next.year)
 syncScene();updateLoadingState()
 const target=nodeMap.value.get(locationKey(next));selectedStar.value=select?target?.key||null:null
 world?.setLevel(levelNodes(nodes.value,next),target);world?.setMatches(matches.value,matchingKeys.value);if(select&&target)world?.selectNode(target.key)
}
function changeDayPage(index:number){if(index>=0&&index<memoryPages.value)memoryPage.value=index}
async function activate(node:UniverseNode){
 if(!node.count||!isMatch(node))return
 if(node.kind==='capsule'){await router.push({path:`/capsules/${node.capsuleId}`,query:{from:'universe'}});return}
 if(node.kind==='memory'){if(node.memoryId)await openMemory(node.memoryId);return}
 await navigate({level:node.kind,year:node.year,month:node.month,day:node.day},true)
}
function back(){if(reading.value){closeReader();return}const v=location.value;void navigate(v.level==='day'?{level:'month',year:v.year,month:v.month}:v.level==='month'?{level:'year',year:v.year}:{level:'universe'})}
async function openMemory(id:number){
 const version=++detailVersion;selectedStar.value=`memory:${id}`;selectedMemoryId.value=id;photoIndex.value=0;detailError.value='';detailLoading.value=!detailCache.value.has(id)
 if(!detailLoading.value)return
 try{const item=await memoryApi.detail(id);if(!destroyed){const cache=new Map(detailCache.value);cache.set(id,item);if(cache.size>64)cache.delete(cache.keys().next().value!);detailCache.value=cache}}
 catch(e){if(version===detailVersion)detailError.value=(e as Error).message}
 finally{if(version===detailVersion)detailLoading.value=false}
}
function stepMemory(delta:number){const items=dayNodes.value;if(!items.length)return;const node=items[(chosenIndex.value+delta+items.length)%items.length];if(node?.memoryId)void openMemory(node.memoryId)}
function shiftPhoto(delta:number){if(media.value.length)photoIndex.value=(photoIndex.value+delta+media.value.length)%media.value.length}
function updateMotion(){reduced.value=motionQuery.matches;world?.setReduced(reduced.value);if(reduced.value)paused.value=true}
function toggleImmersive(){closeReader();filterOpen.value=false;immersive.value=!immersive.value}
function keydown(event:KeyboardEvent){if(immersive.value&&event.key==='Tab'){event.preventDefault();stage.value?.querySelector<HTMLButtonElement>('.immersive-exit')?.focus();return}if(event.key==='Escape'){if(reading.value)return;if(filterOpen.value){filterOpen.value=false;return}if(immersive.value){immersive.value=false;return}if(navigationOpen.value){closeNavigation();return}if(location.value.level!=='universe')back()}}
async function applyFilters(){if(!overviewReady.value){filterError.value='时间概要仍在加载，请稍后重试';return}if(!keyword.value.trim()&&mediaType.value==='all'){clearFilters();return}appliedQuery={keyword:keyword.value.trim(),sourceType:mediaType.value==='all'?undefined:mediaType.value==='capsule'?'CAPSULE':'MEMORY'};await loadSearch(1)}
async function loadSearch(page:number){
 const version=++filterVersion;searchAbort?.abort();searchAbort=new AbortController();filtering.value=true;filterError.value=''
 try{
  const response=await universeApi.search(props.familyId,appliedQuery.keyword,appliedQuery.sourceType,page,PAGE_SIZE,searchAbort.signal)
  if(version!==filterVersion||destroyed)return
  searchPage.value=response;matches.value=new Set(response.items.map(item=>item.indexId));matchingKeys.value=itemPathKeys(response.items);appliedFilter.value=[appliedQuery.keyword,appliedQuery.sourceType==='CAPSULE'?'珍藏':appliedQuery.sourceType==='MEMORY'?'记忆':''].filter(Boolean).join(' · ')
  await navigate({level:'universe'});if(version!==filterVersion||destroyed)return
  selectedStar.value=response.items.length===1?`${response.items[0]!.sourceType==='MEMORY'?'memory':'capsule'}:${response.items[0]!.sourceId}`:null
  world?.focusMatches(matches.value!,matchingKeys.value);filterOpen.value=false
 }catch(e){if(version===filterVersion&&!destroyed)filterError.value=(e as Error).message}
 finally{if(version===filterVersion)filtering.value=false}
}
function refocusSearch(){if(matches.value)world?.focusMatches(matches.value,matchingKeys.value)}
function clearFilters(){if(route.query.keyword)void router.replace({query:{...route.query,keyword:undefined}});filterVersion++;searchAbort?.abort();searchPage.value=null;matches.value=null;matchingKeys.value=null;keyword.value='';mediaType.value='all';filtering.value=false;filterError.value='';appliedFilter.value='';selectedStar.value=null;hovered.value=null;world?.setMatches(null);void navigate({...location.value})}
function retryMonth(){if(location.value.year){preloader?.prioritize(location.value.year,true);updateLoadingState()}}
onMounted(()=>{lowQuality.value=window.innerWidth<768;updateMotion();motionQuery.addEventListener('change',updateMotion);document.addEventListener('keydown',keydown);void initialize()})
onBeforeUnmount(()=>{destroyed=true;initVersion++;filterVersion++;detailVersion++;initAbort?.abort();searchAbort?.abort();preloader?.dispose();cancelAnimationFrame(sceneFrame);if(immersive.value){const app=document.getElementById('app');if(app)app.inert=appWasInert}motionQuery.removeEventListener('change',updateMotion);document.removeEventListener('keydown',keydown);world?.dispose()})
</script>

<template>
  <Teleport to="body" :disabled="!immersive">
    <section ref="stage" class="universe-stage" :class="{immersive,reading,'nav-open':navigationOpen}" :data-level="location.level" aria-label="家庭记忆宇宙海">
      <div ref="canvasHost" class="universe-render"></div>
      <div class="universe-vignette" aria-hidden="true"></div>
      <header class="universe-header universe-ui">
        <div><div class="universe-title-row"><h1>记忆星海</h1><p class="universe-eyebrow">A UNIVERSE OF US</p></div><p class="universe-subtitle"><template v-if="capsuleTotal">{{memoryTotal}} 段记忆 · {{capsuleTotal}} 份珍藏，在星海中重逢。</template><template v-else>{{memoryTotal}} 段记忆，在星光中慢慢重逢。</template></p></div>
      </header>
      <div class="universe-control-dock universe-ui ambient-control">
        <div class="control-dock-bar"><div class="universe-mode"><slot name="mode"/></div><button class="control-dock-toggle" aria-label="搜索与筛选记忆" :aria-expanded="filterOpen" :class="{active:matches!==null}" @click="toggleFilter"><Search :size="17"/></button></div>
      </div>
      <div class="universe-camera universe-ui ambient-control" role="group" aria-label="视角与显示控制">
        <div class="camera-actions">
          <button v-if="location.level!=='universe'" aria-label="返回上一层" aria-describedby="camera-back-hint" @click="back"><ArrowLeft :size="17"/><span id="camera-back-hint" class="control-tooltip" role="tooltip">返回上一层</span></button>
          <button aria-label="重新居中" aria-describedby="camera-reset-hint" @click="world?.recenter()"><RotateCcw :size="17"/><span id="camera-reset-hint" class="control-tooltip" role="tooltip">复位当前视角</span></button>
          <button aria-label="放大星海" aria-describedby="camera-zoom-in-hint" @click="world?.zoom(.8)"><ZoomIn :size="18"/><span id="camera-zoom-in-hint" class="control-tooltip" role="tooltip">放大星海</span></button>
          <button aria-label="缩小星海" aria-describedby="camera-zoom-out-hint" @click="world?.zoom(1.25)"><ZoomOut :size="18"/><span id="camera-zoom-out-hint" class="control-tooltip" role="tooltip">缩小星海</span></button>
        </div>
        <div class="camera-actions camera-display">
          <button :aria-label="paused?'继续星海旋转':'暂停星海旋转'" :aria-pressed="paused" :disabled="reduced" aria-describedby="camera-motion-hint" @click="paused=!paused"><Play v-if="paused" :size="16"/><Pause v-else :size="16"/><span id="camera-motion-hint" class="control-tooltip" role="tooltip">{{reduced?'已启用系统减少动态效果':paused?'继续星海旋转':'暂停星海旋转'}}</span></button>
          <button aria-label="隐藏界面，进入沉浸模式" aria-describedby="camera-immersive-hint" @click="toggleImmersive"><EyeOff :size="16"/><span id="camera-immersive-hint" class="control-tooltip" role="tooltip">全屏沉浸 · 隐藏界面</span></button>
        </div>
      </div>
      <div class="universe-toolbar universe-ui ambient-control">
        <UniverseBreadcrumb :path="breadcrumb" @navigate="navigate"/>

      </div>
      <form v-if="filterOpen" class="universe-filter universe-ui" @submit.prevent="applyFilters">
        <div class="flex items-center justify-between"><h2>寻找一段记忆</h2><button type="button" aria-label="关闭筛选" @click="filterOpen=false"><X :size="17"/></button></div>
        <label>关键词<div class="filter-input"><Search :size="16"/><input v-model="keyword" placeholder="搜索记忆或珍藏的标题" aria-label="星海搜索关键词"/></div></label>
        <label>内容类型<select v-model="mediaType" aria-label="记忆类型"><option value="all">记忆与珍藏</option><option value="memory">记忆</option><option value="capsule">珍藏</option></select></label>
        <label class="quality-option"><input v-model="lowQuality" type="checkbox"/>轻量画质<span>适合手机与较慢的设备</span></label>
        <p v-if="filterError" role="alert" class="filter-error">{{filterError}}</p>
        <div class="filter-actions"><button type="button" @click="clearFilters">重置</button><button type="submit" :disabled="filtering"><LoaderCircle v-if="filtering" :size="15" class="animate-spin"/>{{filtering?'正在寻找…':'点亮匹配记忆'}}</button></div>
      </form>
      <div v-if="matches!==null" class="universe-filter-summary universe-ui" role="status"><Sparkles :size="13"/>{{searchPage?.total || 0}} 项匹配内容 · {{appliedFilter}}<span v-if="searchPages>1" class="search-result-pages"><button aria-label="上一页搜索结果" :disabled="filtering || (searchPage?.page||1)<=1" @click="loadSearch((searchPage?.page||1)-1)"><ChevronLeft :size="13"/></button>{{searchPage?.page}} / {{searchPages}}<button aria-label="下一页搜索结果" :disabled="filtering || !searchPage?.hasMore" @click="loadSearch((searchPage?.page||1)+1)"><ChevronRight :size="13"/></button></span><button aria-label="重新定位搜索结果" @click="refocusSearch"><ZoomIn :size="14"/></button><button aria-label="清除筛选" @click="clearFilters"><X :size="14"/></button></div>

      <div v-if="nodes.length" class="star-targets universe-ui">
        <button v-for="item in visibleLabels" :key="item.node.key" class="star-target" :class="{'star-empty':!item.node.count,'star-dim':!isMatch(item.node),'star-memory':item.node.kind==='memory','star-capsule':item.node.kind==='capsule','star-ancestor':item.ancestor,'star-selected':selectedStar===item.node.key}"
          :data-node-key="item.node.key" :aria-label="accessibleLabel(item.node)" :disabled="!item.node.count || !isMatch(item.node)"
          :style="{left:`${item.x}px`,top:`${item.y}px`,'--star-color':item.node.color}" @click="activateAt($event,item.node)" @pointerenter="hoverAt($event,item.node)" @pointermove="hoverAt($event,item.node)" @pointerleave="world?.requestHover(null)" @focus="focusStar($event,item.node.key)" @blur="world?.requestHover(null)">
          <span v-if="contextLabelKeys.has(item.node.key)" class="star-date" :style="datePlacements.get(item.node.key)" aria-hidden="true">{{dateLabel(item.node)}}</span>
        </button>
      </div>



      <div v-if="hoveredMemoryNode && !reading" class="memory-star-preview universe-ui" role="tooltip" :style="memoryPreviewStyle" @pointerenter="world?.holdHover()" @pointerleave="world?.requestHover(null)">
        <small>{{hoveredMemoryNode.node.year}}年{{hoveredMemoryNode.node.month}}月{{hoveredMemoryNode.node.day}}日 · {{hoveredMemoryNode.node.kind==='capsule'?'珍藏 · 封存':'记忆'}}</small>
        <strong>{{label(hoveredMemoryNode.node)}}</strong><span>{{hoveredMemoryNode.node.kind==='capsule'?'点击查看珍藏及开启状态':'点击星点，翻阅故事'}} <ArrowUpRight :size="11"/></span>
      </div>

      <div v-if="loading" class="universe-loading-note" role="status"><LoaderCircle :size="24" class="animate-spin"/><p>正在补全时间星图…</p></div>
      <div v-else-if="error" class="universe-loading-note universe-load-error" role="alert"><p>{{error}}</p><button @click="initialize">重新打开星海</button><p class="text-xs">也可以切换到时光相册继续浏览。</p></div>
      <div v-else-if="!total" class="universe-status"><Sparkles :size="28"/><h2>这里，等待第一颗记忆星</h2><p>记下今天的小事，让星海从这一刻开始。</p></div>
      <div v-if="monthError" class="universe-data-error universe-ui" role="alert"><span>{{monthError}}</span><button @click="retryMonth">重新载入</button></div>
      <div v-else-if="monthLoading" class="universe-data-error universe-ui" role="status"><LoaderCircle :size="14" class="animate-spin"/>正在补全这片星海…</div>

      <Transition name="navigation"><aside v-if="overviewReady && total && navigationOpen" id="universe-navigation" class="universe-rail universe-ui ambient-control" aria-label="时间导航面板">
        <header class="navigation-heading"><div class="time-navigation-actions"><h2>时间导航</h2><button v-if="location.level!=='universe'" class="rail-back" aria-label="时间导航：返回上一层" @click="back"><ChevronLeft :size="13"/>上一级</button><button v-if="location.level!=='universe'" class="rail-back" aria-label="时间导航：返回全景" @click="navigate({level:'universe'})"><Home :size="12"/>全景</button></div><button aria-label="收起时间导航" @click="closeNavigation"><X :size="15"/></button></header>
        <div class="rail-caption"><span>{{levelTitle}}</span></div>
        <div v-if="memoryPages>1" class="memory-pages"><button aria-label="上一页当天记忆" :disabled="memoryPage===0" @click="changeDayPage(memoryPage-1)"><ChevronLeft :size="14"/></button><span>{{memoryPage+1}} / {{memoryPages}}</span><button aria-label="下一页当天记忆" :disabled="memoryPage>=memoryPages-1" @click="changeDayPage(memoryPage+1)"><ChevronRight :size="14"/></button></div>
        <div class="rail-stops" :key="currentKey" role="group" aria-label="宇宙时间导航">
          <button v-for="node in railNodes" :key="node.key" :class="{'memory-stop':node.kind==='memory'||node.kind==='capsule'}" :disabled="!isMatch(node)" :aria-label="accessibleLabel(node)" :style="{'--star-color':node.color}" @click="activate(node)"><img v-if="thumbnail(node)" :src="thumbnail(node)" alt="" class="rail-thumbnail" @error="hideBrokenThumbnail"/><i v-else></i><span>{{label(node)}}</span><small v-if="node.kind!=='memory'&&node.kind!=='capsule'">{{contentCountLabel(node)}}</small></button>
        </div>

      </aside></Transition>
      <button v-if="overviewReady && total && !navigationOpen" class="time-navigation-toggle universe-ui ambient-control" data-navigation-toggle aria-label="展开时间导航" @click="toggleNavigation"><PanelRightOpen :size="15"/>时间导航</button>
      <p class="universe-instruction universe-ui">拖动探索 · 双指或滚轮缩放 · 轻点星光靠近</p>
      <button v-if="immersive" class="immersive-exit" @click="immersive=false" aria-label="退出沉浸模式"><Eye :size="17"/>显示界面<span>Esc</span></button>

      <Transition name="reader"><div v-if="reading" class="universe-reader-layer" @click.self="closeReader">
        <section ref="readerPanel" class="universe-reader" :class="{'reader-note':!media.length}" role="dialog" aria-modal="true" aria-label="当天的家庭记忆" tabindex="-1">
          <header class="reader-header"><span>{{readerNode?.year}}年{{readerNode?.month}}月{{readerNode?.day}}日<small>这一天，被好好珍藏</small></span><button aria-label="关闭记忆，返回星海" @click="closeReader"><X :size="21"/></button></header>
          <div v-if="detailLoading && !memory" class="reader-loading"><LoaderCircle class="animate-spin"/>正在唤醒这段记忆…</div>
          <div v-else-if="detailError" class="reader-loading" role="alert"><p>{{detailError}}</p><button @click="selectedMemoryId && openMemory(selectedMemoryId)">重新载入</button></div>
          <div v-else-if="memory" class="reader-content">
            <div v-if="media.length" class="reader-media"><video v-if="isVideo(media[photoIndex]!)" :key="media[photoIndex]" :src="media[photoIndex]" controls playsinline/><MemoryImage v-else :key="media[photoIndex]" :src="media[photoIndex]!" :alt="memory.title" fit="contain" eager/><div v-if="media.length>1" class="reader-photos"><button aria-label="上一张照片" @click="shiftPhoto(-1)"><ChevronLeft :size="18"/></button><span>{{photoIndex+1}} / {{media.length}}</span><button aria-label="下一张照片" @click="shiftPhoto(1)"><ChevronRight :size="18"/></button></div></div>
            <div class="reader-story"><p class="universe-eyebrow">A MOMENT WORTH KEEPING</p><h2>{{memory.title}}</h2><p class="reader-author">{{memory.authorName}} 记录<template v-if="memory.location"><span>·</span><MapPin :size="13"/>{{memory.location}}</template></p><div class="reader-prose">{{memory.content || '有些瞬间，无需言语，照片已经替我们记住。'}}</div><audio v-if="memory.audioUrl" :src="memory.audioUrl" controls/><div class="reader-tags"><span v-for="tag in memory.tags" :key="tag"># {{tag}}</span></div><RouterLink :to="`/memories/${memory.id}`" class="reader-link">翻开完整故事与家人回应<ArrowUpRight :size="16"/></RouterLink></div>
          </div>
          <div v-if="dayNodes.length>1" class="reader-picks" role="group" aria-label="当天记忆列表"><button v-for="node in readerChoices" :key="node.key" :aria-pressed="node.memoryId===selectedMemoryId" @click="node.memoryId && openMemory(node.memoryId)"><img v-if="thumbnail(node)" :src="thumbnail(node)" alt="" @error="hideBrokenThumbnail"/><span>{{label(node)}}</span></button></div>
          <footer class="reader-footer"><button :disabled="dayNodes.length<2" @click="stepMemory(-1)" aria-label="当天上一段记忆"><ChevronLeft :size="19"/></button><span>当前载入第 {{Math.max(0,chosenIndex)+1}} / {{dayNodes.length}} 段记忆</span><button :disabled="dayNodes.length<2" @click="stepMemory(1)" aria-label="当天下一段记忆"><ChevronRight :size="19"/></button></footer>
        </section>
      </div></Transition>
    </section>
  </Teleport>
</template>

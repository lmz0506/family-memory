<script setup lang="ts">
import {computed,nextTick,onBeforeUnmount,onMounted,ref,shallowRef,watch} from 'vue'
import {Archive,ArrowLeft,ArrowUpRight,ChevronDown,ChevronLeft,ChevronRight,Eye,EyeOff,Gauge,LoaderCircle,MapPin,Pause,Play,RotateCcw,Search,Sparkles,X,ZoomIn,ZoomOut} from 'lucide-vue-next'
import {useRouter,useRoute} from 'vue-router'
import {memoryApi,universeApi} from '@/api'
import type {Memory,UniverseOverview,UniverseItem,UniversePage,UniverseSourceType} from '@/types'
import MemoryImage from './MemoryImage.vue'
import {useDialog} from '@/composables/useDialog'
import {UniverseScene,type ProjectedStar} from '@/three/UniverseScene'
import {calendarBranch} from '@/three/starLabels'
import {UniversePreloader} from '@/three/UniversePreloader'
import {buildOverviewProgressively,buildContentNodes,itemPathKeys,levelNodes,locationKey,type UniverseLocation,type UniverseNode} from '@/three/memoryUniverse'
import '@/styles/universe.css'
import '@/styles/universe-workspace.css'
import StarArrival from './StarArrival.vue'
import FragmentLight from './FragmentLight.vue'
import FragmentGather from './FragmentGather.vue'
import {gatherDeliveryDelay,type GatherTarget} from '@/composables/fragmentGather'
import SkySelect from './SkySelect.vue'
import {fragmentPageWindow} from '@/composables/fragmentPaging'
import {fragmentActivation,type LinkPoint} from '@/composables/fragmentLink'
import {skyEditorOpen} from '@/composables/useSkyEditor'
import {deliveredFragments,interactionFocus,matchesUniverseNode,universeFilter,warmStarColor} from '@/three/branchContext'
import {readViewState,writeViewState} from '@/composables/viewState'
import {useCreationEvents} from '@/composables/useCreationEvents'
import {creationDetail,resolveCreationIndex,type CreationReceipt} from '@/composables/creationReceipt'

const props=defineProps<{familyId:number;initialKeyword?:string;home?:boolean}>(),router=useRouter(),route=useRoute(),PAGE_SIZE=24
type UniverseResume={view:ReturnType<UniverseScene['captureView']>;location:UniverseLocation;selected:string|null;delivered:string|null;type:'all'|'memory'|'capsule';keyword:string;page:number;open:boolean;paused:boolean}
const resumeKey=`universe:${props.familyId}:${props.home?'home':'timeline'}`
const savedUniverse=readViewState<UniverseResume>(resumeKey)
const resumeUniverse=savedUniverse&&(!props.initialKeyword||props.initialKeyword===savedUniverse.keyword)?savedUniverse:undefined
const canvasHost=ref<HTMLElement>(),stage=ref<HTMLElement>(),readerPanel=ref<HTMLElement>()
const loading=ref(true),overviewReady=ref(false),error=ref('')
const overview=shallowRef<UniverseOverview>({memoryCount:0,capsuleCount:0,years:[]})
const nodes=shallowRef<UniverseNode[]>([]),projected=shallowRef<ProjectedStar[]>([])
const searchPage=shallowRef<UniversePage|null>(null)
const location=ref<UniverseLocation>({level:'universe'}),hovered=ref<string|null>(null),selectedStar=ref<string|null>(null)
const paused=ref(false),reduced=ref(false),immersive=ref(false),lowQuality=ref(false)
const fragmentsOpen=ref(window.innerWidth>=768),fragmentPage=ref(0),fragmentType=ref<'all'|'memory'|'capsule'>('all')
const panelPreview=ref<string|null>(null),searchExpanded=ref(false),filtersExpanded=ref(false)
const filterArea=ref<HTMLElement>()
function filterOutside(event:PointerEvent){if(!filterArea.value?.contains(event.target as Node))filtersExpanded.value=false}
function filterFocusOut(event:FocusEvent){if(!filterArea.value?.contains(event.relatedTarget as Node|null))filtersExpanded.value=false}
function closeFragmentFilters(){filtersExpanded.value=false;nextTick(()=>filterArea.value?.querySelector<HTMLButtonElement>('.fragment-filter-trigger')?.focus())}
const fragmentLink=shallowRef<{id:number;key:string;from:LinkPoint;to:LinkPoint;color:string;reverse:boolean}|null>(null)
const fragmentPulse=ref<string|null>(null),deliveredKey=ref<string|null>(null),pinnedDeliveredKey=ref<string|null>(null)
let linkVersion=0,lastLinkKey:string|null=null,lastLinkReverse=false,openOnArrival=false,bridgePinned=false,pulseTimer:ReturnType<typeof setTimeout>|undefined
let cameraHoverLockUntil=0,pageBlurred=false,panelHovered=false,previewResetTimer:ReturnType<typeof setTimeout>|undefined,panelHoverTimer:ReturnType<typeof setTimeout>|undefined
function stopPreviewReset(){if(previewResetTimer)clearTimeout(previewResetTimer);previewResetTimer=undefined}
function enterFragmentPanel(){panelHovered=true;stopPreviewReset();world?.holdHover()}
function resetDeliveredAfterLeave(){
 stopPreviewReset();if(bridgePinned)return
 previewResetTimer=setTimeout(()=>{if(destroyed||panelHovered)return;if(fragmentGather.value?.key!==selectedStar.value)cancelGather();if(selectedStar.value && pinnedDeliveredKey.value!==selectedStar.value)void linkFragments(selectedStar.value,false,true,true);else deliveredKey.value=pinnedDeliveredKey.value},400)
}
const fragmentReveal=ref(0)
const fragmentGather=shallowRef<{id:number;key:string;open:boolean;x:number;y:number;width:number;height:number;color:string;targets:GatherTarget[]}|null>(null)
let gatherTimer:ReturnType<typeof setTimeout>|undefined
function cancelGather(complete=false){const current=fragmentGather.value;if(gatherTimer)clearTimeout(gatherTimer);gatherTimer=undefined;fragmentGather.value=null;if(complete&&current)deliverFragment(current.key,current.open,false)}
function measureGather(id:number){const gather=fragmentGather.value,body=stage.value?.querySelector<HTMLElement>('.fragment-body');if(!gather||gather.id!==id||!body)return;const bounds=body.getBoundingClientRect();fragmentGather.value={...gather,targets:Array.from(body.querySelectorAll<HTMLElement>('[data-fragment-key]')).map(row=>{const r=row.getBoundingClientRect();return {x:r.left-bounds.left,y:r.top-bounds.top,width:r.width,height:r.height}})}}
function finishGather(id:number){if(fragmentGather.value?.id===id)fragmentGather.value=null}
async function gatherFragments(key:string,open:boolean,id:number,point:LinkPoint,color:string){
 cancelGather();if(open)fragmentsOpen.value=true
 await nextTick();if(destroyed||id!==linkVersion)return
 const body=stage.value?.querySelector<HTMLElement>('.fragment-body'),stageBounds=stage.value?.getBoundingClientRect(),bounds=body?.getBoundingClientRect()
 if(!bounds||!stageBounds||paused.value||reduced.value){deliverFragment(key,open,false);return}
 fragmentGather.value={id,key,open,x:Math.max(8,Math.min(bounds.width-8,point.x+stageBounds.left-bounds.left)),y:Math.max(8,Math.min(bounds.height-8,point.y+stageBounds.top-bounds.top)),width:bounds.width,height:bounds.height,color,targets:[]}
 gatherTimer=setTimeout(()=>{gatherTimer=undefined;if(fragmentGather.value?.id===id&&!destroyed)deliverFragment(key,open,false)},gatherDeliveryDelay)
}
function cancelFragmentLink(){linkVersion++;fragmentLink.value=null;lastLinkKey=null;openOnArrival=false;bridgePinned=false;fragmentPulse.value=null;if(pulseTimer)clearTimeout(pulseTimer)}
async function pulseFragment(key:string){
 fragmentPulse.value=null;if(pulseTimer)clearTimeout(pulseTimer);if(paused.value||reduced.value)return;const version=linkVersion;await nextTick();if(destroyed||version!==linkVersion)return
 fragmentPulse.value=key;pulseTimer=setTimeout(()=>fragmentPulse.value=null,650)
}
function deliverFragment(key:string,open:boolean,reverse:boolean){
 if(!reverse){fragmentReveal.value++;deliveredKey.value=key;if(selectedStar.value===key)pinnedDeliveredKey.value=key}
 if(open)fragmentsOpen.value=true
 void pulseFragment(key)
}
function meteorTravel(id:number,x:number,y:number){if(fragmentLink.value?.id===id)world?.wakeAt(x,y)}
function finishFragmentLink(id:number){
 if(fragmentLink.value?.id!==id)return
 const {key,reverse,to,color}=fragmentLink.value,shouldOpen=openOnArrival
 fragmentLink.value=null;openOnArrival=false;bridgePinned=false
 if(reverse)deliverFragment(key,shouldOpen,true)
 else void gatherFragments(key,shouldOpen,id,to,color)
}
async function linkFragments(key:string,reverse=false,open=false,force=false){
 const node=nodeMap.value.get(key)
 if(!node||reading.value||skyEditorOpen.value||immersive.value||arrival.value||document.hidden||pageBlurred){cancelFragmentLink();return}
 if(!reverse&&deliveredKey.value===key){if(fragmentGather.value&&fragmentGather.value.key!==key)cancelGather();if(fragmentLink.value?.reverse)cancelFragmentLink();if(open)fragmentsOpen.value=true;return}
 if(!force&&lastLinkKey===key&&lastLinkReverse===reverse){
  if(fragmentLink.value){openOnArrival ||= open;bridgePinned ||= open}
  else if(open){fragmentsOpen.value=true}
  return
 }
 if(fragmentGather.value?.key!==key)cancelGather()
 const id=++linkVersion;fragmentLink.value=null;lastLinkKey=key;lastLinkReverse=reverse;openOnArrival=open;bridgePinned=open
 await nextTick();if(id!==linkVersion||destroyed)return
 const point=world?.screenPosition(key),bounds=stage.value?.getBoundingClientRect()
 const destination=(reverse?stage.value?.querySelector<HTMLElement>(`[data-fragment-key="${key}"]`):undefined)||stage.value?.querySelector<HTMLElement>('.fragment-toggle')||stage.value?.querySelector<HTMLElement>('.fragment-body')
 const target=destination?.getBoundingClientRect()
 if(!point?.visible||!bounds||!target||paused.value||reduced.value){bridgePinned=false;openOnArrival=false;deliverFragment(key,open,reverse);return}
 const star={x:point.x,y:point.y},card={x:target.left-bounds.left+(reverse?Math.min(target.width/2,30):target.width/2),y:target.top-bounds.top+target.height/2}
 fragmentLink.value={id,key,from:reverse?card:star,to:reverse?star:card,color:node.color,reverse}
}
watch([hovered,panelPreview],([hover,panel])=>{
 stopPreviewReset();const key=panel||hover
 if(key)void linkFragments(key,Boolean(panel))
 else if(!bridgePinned){cancelFragmentLink();if(!panelHovered)resetDeliveredAfterLeave()}
},{flush:'post'})
const searchInput=ref<HTMLInputElement>()
async function toggleSearch(){filtersExpanded.value=false;searchExpanded.value=!searchExpanded.value;if(searchExpanded.value){await nextTick();searchInput.value?.focus()}}
const creation=useCreationEvents(),recentCreation=ref<CreationReceipt|null>(null),creationUpdating=ref(false),creationError=ref('')
const arrival=ref<{receipt:CreationReceipt;target:{x:number;y:number}|null}|null>(null)
const createdItems=new Map<number,UniverseItem>()
let creationAbort:AbortController|undefined,creationVersion=0
const keyword=ref(props.initialKeyword||''),filtering=ref(false),filterError=ref(''),appliedFilter=ref('')
const matches=shallowRef<Set<number>|null>(null),matchingKeys=shallowRef<Set<string>|null>(null)
const monthLoading=ref(false),monthError=ref('')
const selectedMemoryId=ref<number|null>(null),detailLoading=ref(false),detailError=ref(''),photoIndex=ref(0)
const detailCache=shallowRef(new Map<number,Memory>())
const motionQuery=window.matchMedia('(prefers-reduced-motion: reduce)')
let world:UniverseScene|undefined,destroyed=false,initVersion=0,filterVersion=0,detailVersion=0,appWasInert=false
let allOverviewNodes:UniverseNode[]=[],stagedKeys=new Set<string>(),initAbort:AbortController|undefined,searchAbort:AbortController|undefined
let appliedQuery:{keyword:string;sourceType?:UniverseSourceType}={keyword:''}
let preloader:UniversePreloader|undefined,sceneFrame=0

const memoryTotal=computed(()=>overview.value.memoryCount),capsuleTotal=computed(()=>overview.value.capsuleCount),total=computed(()=>memoryTotal.value+capsuleTotal.value)
const currentKey=computed(()=>locationKey(location.value))
const visibleLabels=computed(()=>projected.value.filter(item=>item.visible&&item.node.count&&isMatch(item.node)))
const nodeMap=computed(()=>new Map(nodes.value.map(node=>[node.key,node])))
const focus=computed(()=>interactionFocus(hovered.value,panelPreview.value,selectedStar.value))
const contextLabelKeys=computed(()=>{const keys=calendarBranch(nodes.value.filter(isMatch),focus.value.displayKey,selectedStar.value);if(!focus.value.displayKey&&!selectedStar.value)for(const node of nodes.value)if(node.kind==='year'&&isMatch(node))keys.add(node.key);return keys})
const inspectedNode=computed(()=>deliveredKey.value?nodeMap.value.get(deliveredKey.value):undefined)
const fragmentScope=computed(()=>{const node=inspectedNode.value;return node&&(node.kind==='memory'||node.kind==='capsule')?nodeMap.value.get(node.parent!):node})
const fragmentPanelReady=computed(()=>Boolean(deliveredKey.value&&nodeMap.value.has(deliveredKey.value))||matches.value!==null)
const fragmentNodes=computed(()=>deliveredFragments(nodes.value,inspectedNode.value,fragmentType.value,matches.value,matchingKeys.value,fragmentPanelReady.value))
const fragmentPaging=computed(()=>fragmentPageWindow(fragmentNodes.value.length,fragmentPage.value))
const fragmentPages=computed(()=>fragmentPaging.value.pages)
watch(fragmentPages,()=>{fragmentPage.value=fragmentPaging.value.page})
const fragmentCount=computed(()=>!fragmentPanelReady.value?0:inspectedNode.value?(inspectedNode.value.kind==='memory'||inspectedNode.value.kind==='capsule'||matches.value!==null?fragmentNodes.value.length:fragmentType.value==='memory'?inspectedNode.value.memoryCount:fragmentType.value==='capsule'?inspectedNode.value.capsuleCount:inspectedNode.value.count):matches.value!==null?(searchPage.value?.total||0):fragmentType.value==='memory'?memoryTotal.value:fragmentType.value==='capsule'?capsuleTotal.value:total.value)
const shownFragments=computed(()=>fragmentNodes.value.slice(fragmentPaging.value.start,fragmentPaging.value.end))
const scopeLoading=computed(()=>{if(!fragmentPanelReady.value)return false;void nodes.value;const year=fragmentScope.value?.year;return year?Boolean(preloader&&!preloader.isComplete(year)&&!preloader.errorFor(year)):loading.value||Boolean(preloader&&overview.value.years.some(item=>!preloader!.isComplete(item.year)&&!preloader!.errorFor(item.year)))})
const scopeError=computed(()=>{if(!fragmentPanelReady.value)return '';void nodes.value;const year=fragmentScope.value?.year;return year?preloader?.errorFor(year)||'':overview.value.years.map(item=>preloader?.errorFor(item.year)).find(Boolean)||''})
watch(()=>inspectedNode.value?.key,()=>{fragmentPage.value=0})
watch(fragmentType,()=>{
 fragmentPage.value=0;cancelGather();stopPreviewReset();world?.holdHover();hovered.value=null;clearPanelPreview();world?.highlight(null)
 const node=inspectedNode.value
 if(node&&(node.kind==='memory'||node.kind==='capsule')&&!isMatch(node)){
  const parent=node.parent?nodeMap.value.get(node.parent):undefined
  if(parent){deliveredKey.value=parent.key;pinnedDeliveredKey.value=parent.key;selectedStar.value=parent.key}
 }
 updateSceneFilter()
})
watch([matches,matchingKeys],updateSceneFilter)
watch(fragmentsOpen,open=>{if(!open){cancelGather();cancelFragmentLink();clearTransient()}})
const datePlacements=computed(()=>{
 const placements=new Map<string,{left:string;top:string}>()
 for(const item of visibleLabels.value){
  if(!contextLabelKeys.value.has(item.node.key))continue
  // A fixed curve parameter follows the actual branch without searching for a new slot each frame.
  const point=item.label||{x:item.x+14,y:item.y-26}
  placements.set(item.node.key,{left:`calc(50% + ${point.x-item.x}px)`,top:`calc(50% + ${point.y-item.y}px)`})
 }
 return placements
})
const hoverHint=computed(()=>{if(!hovered.value||hovered.value===selectedStar.value||panelPreview.value)return undefined;const star=projected.value.find(p=>p.node.key===hovered.value&&p.visible);if(!star)return undefined;const width=stage.value?.clientWidth||375,height=stage.value?.clientHeight||600;return {x:Math.max(10,star.x+210<(fragmentsOpen.value&&width>=768?width-340:width)?star.x+20:star.x-195),y:Math.max(12,star.y+45<height?star.y+24:star.y-30),collection:star.node.kind!=='memory'&&star.node.kind!=='capsule'}})
const selectedLeaf=computed(()=>nodes.value.find(node=>node.memoryId===selectedMemoryId.value&&node.kind==='memory'))
const readerNode=computed(()=>selectedLeaf.value)
const dayNodes=computed(()=>nodes.value.filter(node=>node.kind==='memory'&&node.year===selectedLeaf.value?.year&&node.month===selectedLeaf.value?.month&&node.day===selectedLeaf.value?.day))
const reading=computed(()=>selectedMemoryId.value!==null),memory=computed(()=>selectedMemoryId.value===null?undefined:detailCache.value.get(selectedMemoryId.value))
const media=computed(()=>Array.from(new Set([...(memory.value?.mediaUrls||[]),memory.value?.coverUrl].filter((value):value is string=>Boolean(value)))))
const chosenIndex=computed(()=>dayNodes.value.findIndex(node=>node.memoryId===selectedMemoryId.value)),readerChoices=computed(()=>dayNodes.value.slice(Math.floor(Math.max(0,chosenIndex.value)/8)*8,Math.floor(Math.max(0,chosenIndex.value)/8)*8+8))
const fragmentYears=computed(()=>overview.value.years.filter(year=>{const node=nodeMap.value.get(`year:${year.year}`);return node&&isMatch(node)}))
const fragmentMonths=computed(()=>nodes.value.filter(n=>n.kind==='month'&&n.year===fragmentScope.value?.year&&isMatch(n)).sort((a,b)=>(a.month||0)-(b.month||0)))
const fragmentDays=computed(()=>nodes.value.filter(n=>n.kind==='day'&&n.year===fragmentScope.value?.year&&n.month===fragmentScope.value?.month&&isMatch(n)).sort((a,b)=>(a.day||0)-(b.day||0)))
function selectFragmentType(value:string){changeFragmentType(value as 'all'|'memory'|'capsule')}
function selectFragmentDate(kind:'year'|'month'|'day',choice:string){
 const value=Number(choice),scope=fragmentScope.value
 if(!value){const parent=kind==='year'?undefined:nodes.value.find(n=>kind==='month'?n.kind==='year'&&n.year===scope?.year:n.kind==='month'&&n.year===scope?.year&&n.month===scope?.month);if(parent)void inspectOrActivate(parent);else clearStarSelection();return}
 const node=nodes.value.find(n=>n.kind===kind&&(kind==='year'?n.year===value:n.year===scope?.year&&(kind==='month'?n.month===value:n.month===scope?.month&&n.day===value)))
 if(node)void inspectOrActivate(node)
}
const searchPages=computed(()=>Math.ceil((searchPage.value?.total||0)/PAGE_SIZE))
function isMatch(node:UniverseNode){return matchesUniverseNode(node,fragmentType.value,matches.value,matchingKeys.value)}
function updateSceneFilter(){const filter=universeFilter(nodes.value,fragmentType.value,matches.value,matchingKeys.value);world?.setMatches(filter.ids,filter.keys)}
function dateLabel(node:UniverseNode){return node.kind==='year'?`${node.year}年`:node.kind==='month'?`${node.month}月`:`${node.day}日`}
function contentCountLabel(node:UniverseNode){return [node.memoryCount?`${node.memoryCount}段记忆`:'',node.capsuleCount?`${node.capsuleCount}份珍藏`:''].filter(Boolean).join('、')||'暂无内容'}
function label(node:UniverseNode){return node.kind==='year'?`${node.year}`:node.kind==='month'?`${node.month}月`:node.kind==='day'?`${node.day}日`:node.title||(node.kind==='capsule'?'一份珍藏':'一段记忆')}
function accessibleLabel(node:UniverseNode){return node.kind==='year'?`探索${node.year}年，${contentCountLabel(node)}`:node.kind==='month'?`探索${node.month}月，${contentCountLabel(node)}`:node.kind==='day'?`打开${node.month}月${node.day}日，${contentCountLabel(node)}`:node.kind==='capsule'?`查看珍藏星点：${label(node)}`:`查看记忆星点：${label(node)}`}
function isVideo(url:string){return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url)}
function thumbnail(node:UniverseNode){const item=detailCache.value.get(node.memoryId!);const url=item?.coverUrl||item?.mediaUrls?.[0];return url&&!isVideo(url)?url:''}
function hideBrokenThumbnail(event:Event){(event.target as HTMLImageElement).style.display='none'}
function closeReader(){const id=selectedMemoryId.value;selectedMemoryId.value=null;detailVersion++;detailLoading.value=false;detailError.value='';if(id!==null)nextTick(()=>stage.value?.querySelector<HTMLButtonElement>(`[data-node-key="memory:${id}"]`)?.focus())}
useDialog(reading,readerPanel,closeReader)
watch([reading,skyEditorOpen,immersive,arrival,paused,reduced],()=>{if(reading.value||skyEditorOpen.value||immersive.value||arrival.value||paused.value||reduced.value){if((paused.value||reduced.value)&&fragmentLink.value)deliverFragment(fragmentLink.value.key,openOnArrival,fragmentLink.value.reverse);cancelFragmentLink();cancelGather(paused.value||reduced.value)}})
watch(skyEditorOpen,value=>{if(value)filtersExpanded.value=false})
watch(reduced,value=>{if(value)arrival.value=null})
watch(reading,value=>world?.setReading(value));watch(selectedMemoryId,value=>world?.selectMemory(value));watch([paused,arrival],()=>world?.setPaused(paused.value||Boolean(arrival.value)));watch(lowQuality,value=>world?.setLowQuality(value))
watch(()=>focus.value.transientKey,value=>{world?.highlight(value);const node=value?nodeMap.value.get(value):undefined;if(node)preloader?.prioritize(node.year)})
watch(selectedStar,key=>{world?.selectNode(key);if(!key){cancelFragmentLink();deliveredKey.value=null;pinnedDeliveredKey.value=null}})
function clearTransient(){panelHovered=false;world?.holdHover();hovered.value=null;clearPanelPreview();world?.highlight(null);resetDeliveredAfterLeave()}
function clearStarSelection(){cancelGather();stopPreviewReset();clearTransient();selectedStar.value=null;deliveredKey.value=null;pinnedDeliveredKey.value=null;location.value={level:'universe'};world?.setLevel(levelNodes(nodes.value,location.value),undefined,false);world?.clearSelection();updateLoadingState()}
function focusStar(event:FocusEvent,key:string){if((event.target as HTMLElement).matches(':focus-visible')){world?.holdHover();hovered.value=key}}
function hoverAt(event:PointerEvent,node:UniverseNode){
 if(performance.now()<cameraHoverLockUntil)return
 const label=(event.target as Element).closest('.star-date'),key=label?node.key:world?.nodeAt(event)?.key||node.key
 world?.requestHover(key)
}
function leaveStar(){world?.requestHover(null)}
function activateAt(event:MouseEvent,node:UniverseNode){const label=(event.target as Element).closest('.star-date');void inspectOrActivate(event.detail&&!label?world?.nodeAt(event)||node:node)}
async function inspectOrActivate(node:UniverseNode){
 if(!node.count||!isMatch(node))return
 const transfer=fragmentActivation(node.key,deliveredKey.value,fragmentLink.value||(fragmentGather.value?{key:fragmentGather.value.key,reverse:false}:null))
 cameraHoverLockUntil=performance.now()+1200;bridgePinned=true;clearTransient();selectedStar.value=node.key
 if(node.kind==='memory'||node.kind==='capsule'){world?.selectNode(node.key);world?.preview(node)}
 else{
  location.value={level:node.kind,year:node.year,month:node.month,day:node.day}
  preloader?.prioritize(node.year);syncScene();updateLoadingState()
  world?.setLevel(levelNodes(nodes.value,location.value),node);world?.selectNode(node.key)
 }
 if(transfer==='attached'){if(fragmentGather.value&&fragmentGather.value.key!==node.key)cancelGather();cancelFragmentLink();pinnedDeliveredKey.value=node.key;fragmentsOpen.value=true}
 else if(transfer==='in-flight'){openOnArrival=true;bridgePinned=true;if(fragmentGather.value)fragmentGather.value={...fragmentGather.value,open:true}}
 else void linkFragments(node.key,false,true)
}
watch(()=>props.initialKeyword,value=>{keyword.value=value||'';if(overviewReady.value){if(value?.trim())void applyFilters();else if(matches.value)clearFilters()}})
watch(immersive,async value=>{const app=document.getElementById('app');if(!value){if(app)app.inert=appWasInert;return}appWasInert=Boolean(app?.inert);await nextTick();if(!immersive.value||destroyed)return;if(app)app.inert=true;stage.value?.querySelector<HTMLButtonElement>('.immersive-exit')?.focus()})

/** 将后台已缓存星点加入固定概要布局，切换日期不会删除其他星点。 */
function syncScene(){
 const items=new Map<number,UniverseItem>(preloader?.items)
 for(const item of searchPage.value?.items||[])items.set(item.indexId,item)
 for(const item of createdItems.values())items.set(item.indexId,item)
 const leafItems=[...items.values()],paths=itemPathKeys(leafItems)
 for(const key of paths)stagedKeys.add(key)
 const base=allOverviewNodes.filter(node=>stagedKeys.has(node.key))
 nodes.value=[...base,...buildContentNodes(leafItems,allOverviewNodes)]
 nodes.value=nodes.value.map(node=>({...node,color:warmStarColor(node)}))
 world?.setNodes(nodes.value);updateSceneFilter()
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
  world=new UniverseScene(canvasHost.value,[],{project:value=>projected.value=value,hover:value=>hovered.value=value,pick:node=>{void inspectOrActivate(node)},blank:clearStarSelection,failed:()=>error.value='图形渲染暂时中断，请重新打开星海。'})
  world.setReduced(reduced.value);world.setPaused(paused.value);world.setLowQuality(lowQuality.value)
  world.setWarm(Boolean(props.home))
  const response=await universeApi.overview(props.familyId,initAbort.signal)
  if(destroyed||version!==initVersion)return
  overview.value=response;overviewReady.value=true;let framed=false
  await buildOverviewProgressively(response,(batch,all)=>{
   allOverviewNodes=all;for(const node of batch)stagedKeys.add(node.key)
   syncScene()
   if(!framed){world?.setOverviewBounds(all);world?.setLevel(levelNodes(nodes.value,{level:'universe'}),undefined,!resumeUniverse&&!world?.hasUserGesture());framed=true}
  },()=>destroyed||version!==initVersion)
  if(destroyed||version!==initVersion)return
  preloader=new UniversePreloader((year,afterId,signal)=>universeApi.year(props.familyId,year,afterId,120,signal),()=>{if(version===initVersion)scheduleScene()})
  preloader.start(response.years.map(item=>item.year))
  if(location.value.year)preloader.prioritize(location.value.year)
  if((props.initialKeyword?.trim()||resumeUniverse?.keyword) && version===initVersion && !destroyed){keyword.value=props.initialKeyword||resumeUniverse!.keyword;await applyFilters()}
  if(resumeUniverse&&!destroyed&&version===initVersion){const saved=resumeUniverse;location.value=saved.location;selectedStar.value=saved.selected;deliveredKey.value=saved.delivered;pinnedDeliveredKey.value=saved.delivered;fragmentPage.value=saved.page;fragmentsOpen.value=saved.open;world?.setLevel(levelNodes(nodes.value,saved.location),nodeMap.value.get(locationKey(saved.location)),false);world?.selectNode(saved.selected);world?.restoreView(saved.view)}
 }catch(e){if(!destroyed&&version===initVersion)error.value=(e as Error).message}
 finally{if(version===initVersion)loading.value=false}
}
async function navigate(next:UniverseLocation,select=true){
 closeReader();clearTransient();location.value=next
 if(next.year)preloader?.prioritize(next.year)
 syncScene();updateLoadingState()
 const target=nodeMap.value.get(locationKey(next));selectedStar.value=select?target?.key||null:null;deliveredKey.value=selectedStar.value;pinnedDeliveredKey.value=selectedStar.value
 world?.setLevel(levelNodes(nodes.value,next),target);updateSceneFilter();if(select&&target)world?.selectNode(target.key)
}
async function activate(node:UniverseNode){
 if(!node.count||!isMatch(node))return
 panelPreview.value=null
 clearTransient();fragmentsOpen.value=true
 if(node.kind==='capsule'){await router.push({path:`/capsules/${node.capsuleId}`,query:{from:props.home?'home':'universe'}});return}
 if(node.kind==='memory'){if(node.memoryId)await openMemory(node.memoryId);return}
 await inspectOrActivate(node)
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
function toggleImmersive(){closeReader();clearTransient();immersive.value=!immersive.value}
function keydown(event:KeyboardEvent){if(immersive.value&&event.key==='Tab'){event.preventDefault();stage.value?.querySelector<HTMLButtonElement>('.immersive-exit')?.focus();return}if(event.key==='Escape'){if(reading.value)return;if(immersive.value){immersive.value=false;return}clearStarSelection()}}
async function applyFilters(){if(!overviewReady.value){filterError.value='星图仍在加载，请稍后重试';return}fragmentsOpen.value=true;if(!keyword.value.trim()){clearFilters();return}appliedQuery={keyword:keyword.value.trim(),sourceType:fragmentType.value==='all'?undefined:fragmentType.value==='capsule'?'CAPSULE':'MEMORY'};await loadSearch(1)}
function changeFragmentType(type:'all'|'memory'|'capsule'){fragmentType.value=type;if(appliedQuery.keyword)void applyFilters()}
async function loadSearch(page:number){
 const version=++filterVersion;searchAbort?.abort();searchAbort=new AbortController();filtering.value=true;filterError.value=''
 try{
  const response=await universeApi.search(props.familyId,appliedQuery.keyword,appliedQuery.sourceType,page,PAGE_SIZE,searchAbort.signal)
  if(version!==filterVersion||destroyed)return
  searchPage.value=response;matches.value=new Set(response.items.map(item=>item.indexId));matchingKeys.value=itemPathKeys(response.items);appliedFilter.value=[appliedQuery.keyword,appliedQuery.sourceType==='CAPSULE'?'珍藏':appliedQuery.sourceType==='MEMORY'?'记忆':''].filter(Boolean).join(' · ')
  await navigate({level:'universe'});if(version!==filterVersion||destroyed)return
  selectedStar.value=response.items.length===1?`${response.items[0]!.sourceType==='MEMORY'?'memory':'capsule'}:${response.items[0]!.sourceId}`:null
  world?.focusMatches(matches.value!,matchingKeys.value)
 }catch(e){if(version===filterVersion&&!destroyed)filterError.value=(e as Error).message}
 finally{if(version===filterVersion)filtering.value=false}
}
function refocusSearch(){if(matches.value)world?.focusMatches(matches.value,matchingKeys.value)}
function clearFilters(){if(route.query.keyword)void router.replace({query:{...route.query,keyword:undefined}});filterVersion++;searchAbort?.abort();searchPage.value=null;matches.value=null;matchingKeys.value=null;keyword.value='';appliedQuery={keyword:''};filtering.value=false;filterError.value='';appliedFilter.value='';clearStarSelection();updateSceneFilter();world?.recenter()}
function retryMonth(){if(location.value.year){preloader?.prioritize(location.value.year,true);updateLoadingState()}}
function previewPanelNode(node:UniverseNode,immediate=false){
 world?.holdHover();if(panelHoverTimer)clearTimeout(panelHoverTimer)
 if(immediate){panelPreview.value=node.key;return}
 panelHoverTimer=setTimeout(()=>{panelHoverTimer=undefined;if(!destroyed)panelPreview.value=node.key},160)
}
function clearPanelPreview(){if(panelHoverTimer)clearTimeout(panelHoverTimer);panelHoverTimer=undefined;panelPreview.value=null}
async function changeFragmentPage(delta:number,focus:'first'|'last'|null=null){
 const next=fragmentPageWindow(fragmentNodes.value.length,fragmentPage.value+delta).page
 if(next===fragmentPage.value)return
 cancelGather();cancelFragmentLink();clearPanelPreview();fragmentPage.value=next
 await nextTick();const body=stage.value?.querySelector<HTMLElement>('.fragment-body');body?.scrollTo({top:0})
 if(focus){const rows=Array.from(body?.querySelectorAll<HTMLButtonElement>('[data-fragment-key]')||[]),row=focus==='last'?rows[rows.length-1]:rows[0];row?.focus({preventScroll:true});row?.scrollIntoView({block:'nearest',inline:'nearest'})}
}
function fragmentRowKey(event:KeyboardEvent,index:number){
 if(!['ArrowDown','ArrowUp','Home','End','PageDown','PageUp'].includes(event.key))return
 event.preventDefault();event.stopPropagation()
 if(event.key==='PageDown'){void changeFragmentPage(1,'first');return}
 if(event.key==='PageUp'){void changeFragmentPage(-1,'first');return}
 const rows=Array.from(stage.value?.querySelectorAll<HTMLButtonElement>('[data-fragment-key]')||[])
 if(event.key==='ArrowDown'&&index===rows.length-1&&fragmentPage.value<fragmentPages.value-1){void changeFragmentPage(1,'first');return}
 if(event.key==='ArrowUp'&&index===0&&fragmentPage.value>0){void changeFragmentPage(-1,'last');return}
 const next=event.key==='Home'?0:event.key==='End'?rows.length-1:Math.max(0,Math.min(rows.length-1,index+(event.key==='ArrowDown'?1:-1)))
 rows[next]?.focus({preventScroll:true});rows[next]?.scrollIntoView({block:'nearest',inline:'nearest'})
}
function leavePanelFocus(event:FocusEvent){clearPanelPreview();if(!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node|null))clearTransient()}
function workspaceResize(){cancelGather();cancelFragmentLink()}
function windowBlur(){cancelGather();pageBlurred=true;clearTransient();cancelFragmentLink()}
function windowFocus(){pageBlurred=false;if(selectedStar.value && deliveredKey.value!==selectedStar.value)void linkFragments(selectedStar.value,false,true,true)}
function visibilityChange(){if(document.hidden){cancelGather();clearTransient();cancelFragmentLink()}}
function retryFragments(){if(fragmentScope.value)preloader?.prioritize(fragmentScope.value.year,true);else for(const year of overview.value.years)if(preloader?.errorFor(year.year))preloader.prioritize(year.year,true)}
async function refreshCreation(receipt:CreationReceipt){
 const version=++creationVersion;creationAbort?.abort();creationAbort=new AbortController();const signal=creationAbort.signal
 recentCreation.value=receipt;creationUpdating.value=true;creationError.value='';arrival.value=!reduced.value&&!paused.value&&!document.hidden?{receipt,target:null}:null
 if(arrival.value)fragmentsOpen.value=false
 try{
  const cache=new Map<number,UniverseItem>(preloader?.items)
  for(const item of searchPage.value?.items||[])cache.set(item.indexId,item)
  for(const item of createdItems.values())cache.set(item.indexId,item)
  const [response,item]=await Promise.all([universeApi.overview(props.familyId,signal),resolveCreationIndex(receipt,page=>universeApi.day(props.familyId,receipt.nodeDate,page,PAGE_SIZE,signal),()=>universeApi.search(props.familyId,receipt.title,receipt.sourceType,1,PAGE_SIZE,signal),cache.values())])
  if(destroyed||version!==creationVersion)return
  overview.value=response
  await buildOverviewProgressively(response,(_batch,all)=>{allOverviewNodes=all;for(const node of all)stagedKeys.add(node.key)},()=>destroyed||version!==creationVersion)
  if(destroyed||version!==creationVersion)return
  if(item)createdItems.set(item.indexId,item)
  preloader?.start(response.years.map(year=>year.year));syncScene();world?.setOverviewBounds(allOverviewNodes)
  if(!item)throw Error('已经保存，星海还在更新。可以直接查看内容，或稍后重试。')
  const key=`${item.sourceType==='MEMORY'?'memory':'capsule'}:${item.sourceId}`,node=nodeMap.value.get(key)
  if(!node)throw Error('已经保存，时间星群仍在同步。请稍后更新星图。')
  if(!isMatch(node)){arrival.value=null;fragmentsOpen.value=true;creationError.value='已经保存；当前筛选未包含这颗星，可以直接查看内容。';return}
  clearTransient();selectedStar.value=key;world?.selectNode(key);deliveredKey.value=key;pinnedDeliveredKey.value=key
  if(reduced.value||paused.value||document.hidden){fragmentsOpen.value=true;return}
  world?.setPaused(true)
  let point=world?.screenPosition(key)
  if(!point?.visible){
   world?.preview(node)
   let timeout:ReturnType<typeof setTimeout>|undefined
   const arrived=await Promise.race([world?.settledFlight()||Promise.resolve(false),new Promise<false>(resolve=>{timeout=setTimeout(()=>resolve(false),2200)})])
   if(timeout)clearTimeout(timeout)
   if(!arrived||destroyed||version!==creationVersion){if(version===creationVersion){arrival.value=null;fragmentsOpen.value=true}return}
   point=world?.screenPosition(key)
  }
  if(point?.visible&&stage.value&&arrival.value){const bounds=stage.value.getBoundingClientRect();arrival.value={receipt,target:{x:bounds.left+point.x,y:bounds.top+point.y}}}else{arrival.value=null;fragmentsOpen.value=true}
 }catch(e){if(!destroyed&&version===creationVersion){arrival.value=null;fragmentsOpen.value=true;creationError.value=`内容已保存，${(e as Error).message.replace(/^已经保存，?/,'')}`}}
 finally{if(version===creationVersion){creationUpdating.value=false;if(!arrival.value)world?.setPaused(paused.value)}}
}
function landCreation(){world?.setPaused(paused.value);const receipt=arrival.value?.receipt;if(!receipt)return;world?.ripple(`${receipt.sourceType==='MEMORY'?'memory':'capsule'}:${receipt.sourceId}`)}
function finishCreation(){const receipt=arrival.value?.receipt,hasTarget=Boolean(arrival.value?.target);arrival.value=null;if(!receipt)return;const key=`${receipt.sourceType==='MEMORY'?'memory':'capsule'}:${receipt.sourceId}`,node=nodeMap.value.get(key);if(!node||destroyed)return;selectedStar.value=key;world?.selectNode(key);fragmentsOpen.value=true;if(hasTarget&&!reduced.value&&!paused.value&&!document.hidden)world?.preview(node)}
watch([creation.pending,overviewReady,loading],()=>{if(!overviewReady.value||loading.value||destroyed)return;const receipt=creation.take(props.familyId);if(receipt)void refreshCreation(receipt)},{flush:'post'})
onMounted(()=>{if(resumeUniverse){fragmentType.value=resumeUniverse.type;paused.value=resumeUniverse.paused}lowQuality.value=window.innerWidth<768;updateMotion();motionQuery.addEventListener('change',updateMotion);document.addEventListener('keydown',keydown);document.addEventListener('pointerdown',filterOutside);window.addEventListener('blur',windowBlur);window.addEventListener('focus',windowFocus);window.addEventListener('resize',workspaceResize);document.addEventListener('visibilitychange',visibilityChange);void initialize()})
onBeforeUnmount(()=>{if(world)writeViewState<UniverseResume>(resumeKey,{view:world.captureView(),location:location.value,selected:selectedStar.value,delivered:pinnedDeliveredKey.value,type:fragmentType.value,keyword:appliedQuery.keyword,page:fragmentPage.value,open:fragmentsOpen.value,paused:paused.value});destroyed=true;cancelGather();clearPanelPreview();stopPreviewReset();cancelFragmentLink();window.removeEventListener('resize',workspaceResize);initVersion++;filterVersion++;detailVersion++;creationVersion++;creationAbort?.abort();initAbort?.abort();searchAbort?.abort();preloader?.dispose();cancelAnimationFrame(sceneFrame);if(immersive.value){const app=document.getElementById('app');if(app)app.inert=appWasInert}motionQuery.removeEventListener('change',updateMotion);document.removeEventListener('keydown',keydown);document.removeEventListener('pointerdown',filterOutside);window.removeEventListener('blur',windowBlur);window.removeEventListener('focus',windowFocus);document.removeEventListener('visibilitychange',visibilityChange);world?.dispose()})
</script>

<template>
  <Teleport to="body" :disabled="!immersive">
    <section ref="stage" class="universe-stage" :class="{immersive,reading,'universe-home':home,'motion-paused':paused}" :data-level="location.level" aria-label="家庭记忆宇宙海" @pointerdown="arrival=null" @pointerleave="clearTransient" @pointercancel="clearTransient">
      <div ref="canvasHost" class="universe-render"></div>
      <div class="universe-vignette" aria-hidden="true"></div>
      <header class="universe-header universe-ui">
        <div><div class="universe-title-row"><h1>{{home?'一家人的星海':'记忆星海'}}</h1></div><p class="universe-subtitle"><template v-if="home">{{memoryTotal}} 段记忆 · {{capsuleTotal}} 份珍藏</template><template v-else-if="capsuleTotal">{{memoryTotal}} 段记忆 · {{capsuleTotal}} 份珍藏，在星海中重逢。</template><template v-else>{{memoryTotal}} 段记忆，在星光中慢慢重逢。</template></p></div>
      </header>
<div v-if="!home" class="universe-control-dock universe-ui ambient-control"><div class="universe-mode"><slot name="mode"/></div></div>
      <div class="universe-camera universe-ui ambient-control" role="group" aria-label="视角与显示控制">
        <div class="camera-actions">
          <button v-if="location.level!=='universe'" aria-label="返回上一层" aria-describedby="camera-back-hint" @click="back"><ArrowLeft :size="17"/><span id="camera-back-hint" class="control-tooltip" role="tooltip">返回上一层</span></button>
          <button aria-label="重新居中" aria-describedby="camera-reset-hint" @click="world?.recenter()"><RotateCcw :size="17"/><span id="camera-reset-hint" class="control-tooltip" role="tooltip">复位当前视角</span></button>
          <button aria-label="放大星海" aria-describedby="camera-zoom-in-hint" @click="world?.zoom(.8)"><ZoomIn :size="18"/><span id="camera-zoom-in-hint" class="control-tooltip" role="tooltip">放大星海</span></button>
          <button aria-label="缩小星海" aria-describedby="camera-zoom-out-hint" @click="world?.zoom(1.25)"><ZoomOut :size="18"/><span id="camera-zoom-out-hint" class="control-tooltip" role="tooltip">缩小星海</span></button>
        </div>
        <div class="camera-actions camera-display">
          <button aria-label="轻量画质" :aria-pressed="lowQuality" @click="lowQuality=!lowQuality"><Gauge :size="16"/><span class="control-tooltip" role="tooltip">轻量画质</span></button>
          <button :aria-label="paused?'继续星海旋转':'暂停星海旋转'" :aria-pressed="paused" :disabled="reduced" aria-describedby="camera-motion-hint" @click="paused=!paused"><Play v-if="paused" :size="16"/><Pause v-else :size="16"/><span id="camera-motion-hint" class="control-tooltip" role="tooltip">{{reduced?'已启用系统减少动态效果':paused?'继续星海旋转':'暂停星海旋转'}}</span></button>
          <button aria-label="隐藏界面，进入沉浸模式" aria-describedby="camera-immersive-hint" @click="toggleImmersive"><EyeOff :size="16"/><span id="camera-immersive-hint" class="control-tooltip" role="tooltip">全屏沉浸 · 隐藏界面</span></button>
        </div>
      </div>
      <div v-if="nodes.length" class="star-targets universe-ui">
        <button v-for="item in visibleLabels" :key="item.node.key" class="star-target" :class="{'star-empty':!item.node.count,'star-dim':!isMatch(item.node),'star-memory':item.node.kind==='memory','star-capsule':item.node.kind==='capsule','star-ancestor':item.ancestor,'star-selected':selectedStar===item.node.key,'fragment-star-received':fragmentPulse===item.node.key}"
          :data-node-key="item.node.key" :aria-label="accessibleLabel(item.node)" :disabled="!item.node.count || !isMatch(item.node)"
          :style="{left:`${item.x}px`,top:`${item.y}px`,'--star-color':item.node.color}" @click="activateAt($event,item.node)" @pointerenter="hoverAt($event,item.node)" @pointermove="hoverAt($event,item.node)" @pointerleave="leaveStar" @focus="focusStar($event,item.node.key)" @blur="world?.requestHover(null)">
          <span v-if="contextLabelKeys.has(item.node.key)" class="star-date" :style="datePlacements.get(item.node.key)" aria-hidden="true">{{dateLabel(item.node)}}</span>
        </button>
      </div>



      <span v-if="hoverHint && !reading && !immersive && !skyEditorOpen" class="star-pin-hint" role="status" :style="{left:`${hoverHint.x}px`,top:`${hoverHint.y}px`}">点击星光，固定这{{hoverHint.collection?'片':'段'}}时光</span>
      <FragmentLight v-if="fragmentLink" :key="fragmentLink.id" :id="fragmentLink.id" :from="fragmentLink.from" :to="fragmentLink.to" :color="fragmentLink.color" @arrived="finishFragmentLink" @travel="meteorTravel"/>
      <aside v-if="fragmentsOpen && !reading && !immersive && !skyEditorOpen" class="fragment-panel universe-ui" :class="{'fragment-collection':inspectedNode && inspectedNode.kind!=='memory' && inspectedNode.kind!=='capsule'}" :style="{'--collection-color':inspectedNode?.color}" aria-label="时光碎片统一查看面板" @pointerenter="enterFragmentPanel" @pointerleave="clearTransient" @focusout="leavePanelFocus">
        <header class="fragment-heading" :class="{'fragment-received':fragmentPulse && fragmentPulse===deliveredKey && inspectedNode?.kind!=='memory' && inspectedNode?.kind!=='capsule'}"><h2>时光碎片</h2><div class="fragment-heading-actions"><slot v-if="home" name="home-actions"/><button class="fragment-search-toggle" aria-label="展开或收起搜索" aria-controls="fragment-search-form" :aria-expanded="searchExpanded" @click="toggleSearch"><Search :size="18"/></button><button aria-label="收起时光碎片" @click="fragmentsOpen=false"><X :size="18"/></button></div></header>
        <p v-if="fragmentLink && !fragmentLink.reverse" class="fragment-delivery-note" role="status">星光正在送来{{nodeMap.get(fragmentLink.key)?.kind==='memory'||nodeMap.get(fragmentLink.key)?.kind==='capsule'?'这段时光':'这片时光'}}…</p>
        <div ref="filterArea" class="fragment-context" @focusout="filterFocusOut" @keydown.esc.stop.prevent="closeFragmentFilters">
          <div class="fragment-context-row"><button class="fragment-filter-trigger" :aria-expanded="filtersExpanded" aria-controls="fragment-filter-popover" aria-label="调整时间与内容筛选" @click="filtersExpanded=!filtersExpanded"><span>{{fragmentScope?`${fragmentScope.year}年${fragmentScope.month?`${fragmentScope.month}月`:''}${fragmentScope.day?`${fragmentScope.day}日`:''}`:fragmentPanelReady?'查找的时光':'等待星光抵达'}}{{fragmentType==='all'?'':fragmentType==='memory'?' · 记忆':' · 珍藏'}}</span><ChevronDown :size="13"/></button><span v-if="fragmentPanelReady" class="fragment-count">{{fragmentCount}} 份碎片</span></div>
          <div v-if="filtersExpanded" id="fragment-filter-popover" class="fragment-filter-popover"><p>寻找一段时光<button @click="clearFilters();fragmentType='all';filtersExpanded=false">重置</button></p><div class="fragment-selectors">
            <SkySelect id="fragment-kind" label="内容" :model-value="fragmentType" :options="[{value:'all',label:'所有类型'},{value:'memory',label:'记忆'},{value:'capsule',label:'珍藏'}]" @update:model-value="selectFragmentType"/>
            <SkySelect id="fragment-year" label="年份" :model-value="String(fragmentScope?.year||'')" :options="[{value:'',label:'选择年份'},...fragmentYears.map(y=>({value:String(y.year),label:`${y.year}年`}))]" @update:model-value="selectFragmentDate('year',$event)"/>
            <SkySelect v-if="fragmentScope?.year" id="fragment-month" label="月份" :model-value="String(fragmentScope?.month||'')" :options="[{value:'',label:'整年'},...fragmentMonths.map(n=>({value:String(n.month),label:`${n.month}月`}))]" @update:model-value="selectFragmentDate('month',$event)"/>
            <SkySelect v-if="fragmentScope?.month" id="fragment-day" label="日期" :model-value="String(fragmentScope?.day||'')" :options="[{value:'',label:'整月'},...fragmentDays.map(n=>({value:String(n.day),label:`${n.day}日`}))]" @update:model-value="selectFragmentDate('day',$event)"/>
          </div></div>
        </div>
        <form v-if="searchExpanded" id="fragment-search-form" class="fragment-search" @submit.prevent="applyFilters"><Search :size="16"/><input ref="searchInput" v-model="keyword" aria-label="搜索记忆或珍藏标题" placeholder="寻找记忆或珍藏…"/><button type="submit" :disabled="filtering" aria-label="查找时光碎片"><LoaderCircle v-if="filtering" :size="16" class="animate-spin"/><ArrowUpRight v-else :size="17"/></button></form>
        <p v-if="filterError" class="fragment-search-error" role="alert">{{filterError}}</p>
        <div class="fragment-body fragment-receiver" :style="{minHeight:fragmentGather?`${fragmentGather.height}px`:undefined}" :aria-busy="Boolean(fragmentLink && !fragmentLink.reverse || fragmentGather)">
          <FragmentGather v-if="fragmentGather" :key="fragmentGather.id" :id="fragmentGather.id" :x="fragmentGather.x" :y="fragmentGather.y" :width="fragmentGather.width" :height="fragmentGather.height" :color="fragmentGather.color" :targets="fragmentGather.targets" @measure="measureGather" @finished="finishGather"/>
          <div v-if="matches!==null" class="fragment-search-summary" role="status"><span>{{appliedFilter}} · {{searchPage?.total||0}} 项结果</span><button @click="clearFilters">清除</button><button @click="refocusSearch">定位</button><div v-if="searchPages>1"><button :disabled="filtering || (searchPage?.page||1)<=1" aria-label="上一页搜索结果" @click="loadSearch((searchPage?.page||1)-1)"><ChevronLeft :size="14"/></button><span>{{searchPage?.page}} / {{searchPages}}</span><button :disabled="filtering || !searchPage?.hasMore" aria-label="下一页搜索结果" @click="loadSearch((searchPage?.page||1)+1)"><ChevronRight :size="14"/></button></div></div>
          <p v-if="inspectedNode && inspectedNode.kind!=='memory' && inspectedNode.kind!=='capsule'" class="fragment-collection-note">整段时光 · 选择一份碎片继续翻阅</p>

          <Transition name="fragment-unfold" mode="out-in"><div v-if="fragmentPanelReady" :key="fragmentReveal" class="fragment-list"><button v-for="(node,index) in shownFragments" :key="node.key" class="fragment-item" :data-fragment-key="node.key" :aria-label="`${node.kind==='capsule'?'珍藏':'记忆'}，${label(node)}，${node.year}年${node.month}月${node.day}日，点击查看`" :class="{active:selectedStar===node.key&&deliveredKey===node.key,'fragment-dates-full':true,'fragment-linked':deliveredKey===node.key,'fragment-received':fragmentPulse===node.key}" :style="{'--fragment-color':node.color}" @click="activate(node)" @pointerenter="previewPanelNode(node)" @pointerleave="clearPanelPreview" @focus="previewPanelNode(node,true)" @keydown="fragmentRowKey($event,index)"><span class="fragment-kind" :class="{'fragment-kind-capsule':node.kind==='capsule'}"><Archive v-if="node.kind==='capsule'" :size="12" aria-hidden="true"/><Sparkles v-else :size="12" aria-hidden="true"/><span>{{node.kind==='capsule'?'珍藏':'记忆'}}</span></span><strong class="fragment-row-title">{{label(node)}}</strong><time class="fragment-row-date" :datetime="`${node.year}-${String(node.month).padStart(2,'0')}-${String(node.day).padStart(2,'0')}`">{{node.year}}/{{node.month}}/{{node.day}}</time></button></div><div v-else key="awaiting" class="fragment-awaiting"><span class="fragment-empty-mark"><Sparkles :size="26"/></span><strong>等一颗星，送来一段时光</strong><p>轻触或悬停星光<br/>让记忆在这里慢慢浮现</p></div></Transition>
          <p v-if="scopeLoading" class="fragment-empty" role="status">正在整理这段时光，已有内容可以先看。</p><p v-else-if="scopeError" class="fragment-empty" role="alert">{{scopeError}} <button @click="retryFragments">重试</button></p><p v-else-if="fragmentPanelReady && !shownFragments.length" class="fragment-empty">{{matches!==null?'当前筛选下暂无这些碎片。':`这段时光里暂无${fragmentType==='capsule'?'珍藏':fragmentType==='memory'?'记忆':'内容'}，可以调整筛选。`}}</p>
          <div v-if="fragmentPages>1" class="fragment-paging"><button aria-label="上一页时光碎片" :disabled="fragmentPage===0" @click="changeFragmentPage(-1)"><ChevronLeft :size="16"/></button><span aria-live="polite">{{fragmentPaging.start+1}}–{{fragmentPaging.end}} / {{fragmentNodes.length}} 条</span><button aria-label="下一页时光碎片" :disabled="fragmentPage>=fragmentPages-1" @click="changeFragmentPage(1)"><ChevronRight :size="16"/></button></div>
        </div>

      </aside>
      <button v-else-if="!reading && !immersive && !skyEditorOpen" class="fragment-toggle universe-ui" :class="{'fragment-received':fragmentPulse}" @click="fragmentsOpen=true"><Sparkles :size="16"/>时光碎片<span>查找与记录</span></button>
      <div v-if="recentCreation && (creationUpdating || creationError) && !immersive && !skyEditorOpen" class="creation-feedback" role="status"><p>{{creationError||(creationUpdating?'已经保存，正在安放这颗星…':recentCreation.sourceType==='CAPSULE'?'这封信，成为了星海里一份温柔的等待。':'这一刻，已成为我们的一颗星。')}}</p><div class="creation-feedback-actions"><RouterLink :to="{path:creationDetail(recentCreation),query:{from:home?'home':'universe'}}">查看刚刚的{{recentCreation.sourceType==='MEMORY'?'记忆':'珍藏'}}</RouterLink><button v-if="creationError" :disabled="creationUpdating" @click="refreshCreation(recentCreation)">更新星图</button><button @click="recentCreation=null;arrival=null">收起</button></div></div>
      <StarArrival v-if="arrival" :receipt="arrival.receipt" :target="arrival.target" @landed="landCreation" @finished="finishCreation"/>

      <div v-if="loading" class="universe-loading-note" role="status"><LoaderCircle :size="24" class="animate-spin"/><p>正在补全时间星图…</p></div>
      <div v-else-if="error" class="universe-loading-note universe-load-error" role="alert"><p>{{error}}</p><button @click="initialize">重新打开星海</button><p class="text-xs">也可以切换到时光相册继续浏览。</p></div>
      <div v-else-if="!total" class="universe-status"><Sparkles :size="28"/><h2>这里，等待第一颗记忆星</h2><p>记下今天的小事，让星海从这一刻开始。</p></div>
      <div v-if="monthError" class="universe-data-error universe-ui" role="alert"><span>{{monthError}}</span><button @click="retryMonth">重新载入</button></div>
      <div v-else-if="monthLoading" class="universe-data-error universe-ui" role="status"><LoaderCircle :size="14" class="animate-spin"/>正在补全这片星海…</div>


      <p class="universe-instruction universe-ui">拖动探索 · 双指或滚轮缩放 · 轻点星光靠近</p>
      <button v-if="immersive" class="immersive-exit" @click="immersive=false" aria-label="退出沉浸模式"><Eye :size="17"/>显示界面<span>Esc</span></button>

      <Transition name="reader"><div v-if="reading" class="universe-reader-layer" @click.self="closeReader">
        <section ref="readerPanel" class="universe-reader" :class="{'reader-note':!media.length}" role="dialog" aria-modal="true" aria-label="当天的家庭记忆" tabindex="-1">
          <header class="reader-header"><span>{{readerNode?.year}}年{{readerNode?.month}}月{{readerNode?.day}}日<small>这一天，被好好珍藏</small></span><button aria-label="关闭记忆，返回星海" @click="closeReader"><X :size="21"/></button></header>
          <div v-if="detailLoading && !memory" class="reader-loading"><LoaderCircle class="animate-spin"/>正在唤醒这段记忆…</div>
          <div v-else-if="detailError" class="reader-loading" role="alert"><p>{{detailError}}</p><button @click="selectedMemoryId && openMemory(selectedMemoryId)">重新载入</button></div>
          <div v-else-if="memory" class="reader-content">
            <div v-if="media.length" class="reader-media"><video v-if="isVideo(media[photoIndex]!)" :key="media[photoIndex]" :src="media[photoIndex]" controls playsinline/><MemoryImage v-else :key="media[photoIndex]" :src="media[photoIndex]!" :alt="memory.title" fit="contain" eager/><div v-if="media.length>1" class="reader-photos"><button aria-label="上一张照片" @click="shiftPhoto(-1)"><ChevronLeft :size="18"/></button><span>{{photoIndex+1}} / {{media.length}}</span><button aria-label="下一张照片" @click="shiftPhoto(1)"><ChevronRight :size="18"/></button></div></div>
            <div class="reader-story"><p class="universe-eyebrow">A MOMENT WORTH KEEPING</p><h2>{{memory.title}}</h2><p class="reader-author">{{memory.authorName}} 记录<template v-if="memory.location"><span>·</span><MapPin :size="13"/>{{memory.location}}</template></p><div class="reader-prose">{{memory.content || '有些瞬间，无需言语，照片已经替我们记住。'}}</div><audio v-if="memory.audioUrl" :src="memory.audioUrl" controls/><div class="reader-tags"><span v-for="tag in memory.tags" :key="tag"># {{tag}}</span></div><RouterLink :to="{path:`/memories/${memory.id}`,query:{from:home?'home':'timeline'}}" class="reader-link">翻开完整故事与家人回应<ArrowUpRight :size="16"/></RouterLink></div>
          </div>
          <div v-if="dayNodes.length>1" class="reader-picks" role="group" aria-label="当天记忆列表"><button v-for="node in readerChoices" :key="node.key" :aria-pressed="node.memoryId===selectedMemoryId" @click="node.memoryId && openMemory(node.memoryId)"><img v-if="thumbnail(node)" :src="thumbnail(node)" alt="" @error="hideBrokenThumbnail"/><span>{{label(node)}}</span></button></div>
          <footer class="reader-footer"><button :disabled="dayNodes.length<2" @click="stepMemory(-1)" aria-label="当天上一段记忆"><ChevronLeft :size="19"/></button><span>当前载入第 {{Math.max(0,chosenIndex)+1}} / {{dayNodes.length}} 段记忆</span><button :disabled="dayNodes.length<2" @click="stepMemory(1)" aria-label="当天下一段记忆"><ChevronRight :size="19"/></button></footer>
        </section>
      </div></Transition>
    </section>
  </Teleport>
</template>

<script setup lang="ts">
import {computed,nextTick,ref,toRef,watch} from 'vue'
import {ChevronLeft,ChevronRight,ImageOff,Play,X} from 'lucide-vue-next'
import MemoryImage from './MemoryImage.vue'
import {useDialog} from '@/composables/useDialog'
const props=defineProps<{open:boolean;media:string[];index:number;title:string}>()
const emit=defineEmits<{'update:open':[value:boolean];'update:index':[value:number]}>()
const panel=ref<HTMLElement>(),thumbnails=ref<HTMLElement>()
const url=computed(()=>props.media[props.index]||'')
function video(source:string){return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(source)}
const isVideo=computed(()=>video(url.value))
function hideFailedThumbnail(event:Event){(event.target as HTMLImageElement).style.visibility='hidden'}
function close(){emit('update:open',false)}
function move(delta:number){if(props.media.length)emit('update:index',(props.index+delta+props.media.length)%props.media.length)}
async function revealThumbnail(){
 await nextTick()
 const strip=thumbnails.value,active=strip?.querySelector<HTMLElement>('[aria-current="true"]')
 if(strip&&active)strip.scrollTo({left:active.offsetLeft-(strip.clientWidth-active.offsetWidth)/2,behavior:'auto'})
}
watch(()=>[props.open,props.index,props.media.length],revealThumbnail,{flush:'post'})
useDialog(toRef(props,'open'),panel,close)
</script>
<template>
 <Teleport to="body"><Transition name="fade">
  <div v-if="open" ref="panel" role="dialog" aria-modal="true" :aria-label="title" tabindex="-1" class="media-viewer" @click.self="close" @keydown.left.prevent="move(-1)" @keydown.right.prevent="move(1)">
   <header class="viewer-top"><span>{{index+1}} / {{media.length}}</span><button aria-label="关闭照片浏览" @click="close"><X :size="22"/></button></header>
   <div class="viewer-stage" @click.self="close">
    <video v-if="isVideo" :key="url" :src="url" controls autoplay playsinline class="viewer-video"></video>
    <MemoryImage v-else :key="url" :src="url" :alt="title" fit="contain" eager class="viewer-image"/>
    <template v-if="media.length>1"><button class="viewer-prev" aria-label="上一张" @click="move(-1)"><ChevronLeft/></button><button class="viewer-next" aria-label="下一张" @click="move(1)"><ChevronRight/></button></template>
   </div>
   <p class="viewer-caption" :title="title">{{title}}</p>
   <nav ref="thumbnails" class="viewer-thumbnails" aria-label="全部图片与视频缩略图">
    <button v-for="(source,i) in media" :key="`${source}-${i}`" class="viewer-thumbnail" :aria-label="`查看第${i+1}${video(source)?'个视频':'张照片'}`" :aria-current="i===index?'true':undefined" @click="emit('update:index',i)">
     <template v-if="video(source)"><video :src="source" muted playsinline preload="metadata" aria-hidden="true"></video><Play class="viewer-thumbnail-play" :size="17" fill="currentColor"/></template>
     <template v-else><ImageOff :size="18" aria-hidden="true"/><img :src="source" :alt="`第${i+1}张照片缩略图`" loading="lazy" decoding="async" @error="hideFailedThumbnail"/></template>
     <span class="viewer-thumbnail-number">{{i+1}}</span>
    </button>
   </nav>
  </div>
 </Transition></Teleport>
</template>
<style scoped>
.media-viewer{position:fixed;inset:0;z-index:100;display:grid;grid-template-rows:48px minmax(0,1fr) 24px 88px;gap:12px;background:#15151ef7;padding:16px 24px max(18px,env(safe-area-inset-bottom));color:#f4e9da;outline:none}
.viewer-top{display:flex;align-items:center;justify-content:space-between;font-size:13px;color:#c2b5a7;font-variant-numeric:tabular-nums}
.media-viewer button{display:grid;place-items:center;width:44px;height:44px;flex-shrink:0;border-radius:50%;background:#ffffff0f;color:#f4e9da;border:1px solid #ffffff20}
.media-viewer button:focus-visible{outline:2px solid #e2b873;outline-offset:3px}
.viewer-stage{position:relative;min-height:0;min-width:0;display:flex;align-items:center;justify-content:center;padding:0 48px}
.viewer-image{height:100%;min-height:0;max-width:1440px;background:transparent}.viewer-video{max-height:100%;max-width:100%;min-height:0}
.viewer-prev,.viewer-next{position:absolute;top:50%;transform:translateY(-50%)}.viewer-prev{left:0}.viewer-next{right:0}
.viewer-caption{text-align:center;font-size:13px;color:#c9beb5;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:24px}
.viewer-thumbnails{position:relative;display:flex;align-items:center;gap:10px;max-width:min(100%,1100px);width:max-content;justify-self:center;overflow-x:auto;overscroll-behavior-x:contain;scrollbar-width:none;padding:4px;min-width:0}.viewer-thumbnails::-webkit-scrollbar{display:none}
.media-viewer .viewer-thumbnail{position:relative;overflow:hidden;width:76px;height:76px;border-radius:9px;border:2px solid transparent;opacity:.55;transition:opacity .18s,border-color .18s;isolation:isolate}
.media-viewer .viewer-thumbnail:hover{opacity:.9}.media-viewer .viewer-thumbnail[aria-current=true]{opacity:1;border-color:#e2b873}
.viewer-thumbnail>img,.viewer-thumbnail>video{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}.viewer-thumbnail-play{z-index:1}
.viewer-thumbnail-number{position:absolute;right:3px;bottom:3px;font-size:10px;line-height:17px;min-width:17px;padding:0 3px;border-radius:3px;background:#080b1499;color:#fff}
.viewer-thumbnail-play{filter:drop-shadow(0 1px 3px #000)}
@media(max-width:640px){.media-viewer{padding:10px 10px max(12px,env(safe-area-inset-bottom));grid-template-rows:44px minmax(0,1fr) 24px 68px;gap:8px}.viewer-stage{padding:0 30px}.viewer-stage>button{width:32px;height:40px;border-radius:8px;background:#1719239c}.viewer-thumbnails{gap:7px}.media-viewer .viewer-thumbnail{width:58px;height:58px}}
@media(max-height:480px){.media-viewer{grid-template-rows:36px minmax(0,1fr) 20px 56px;gap:5px;padding-block:6px}.media-viewer .viewer-thumbnail{width:48px;height:48px}.viewer-caption{line-height:20px}}
@media(prefers-reduced-motion:reduce){.media-viewer .viewer-thumbnail{transition:none}}
</style>

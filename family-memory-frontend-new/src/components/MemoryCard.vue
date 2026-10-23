<!-- Lucian UX/UI Prompt #228 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#228 -->
<script setup lang="ts">
import {computed,ref,watch} from 'vue'
import {ArrowUpRight,Heart,MapPin,MessageCircle,Play} from 'lucide-vue-next'
import type {Memory} from '@/types'
import {memoryApi} from '@/api'
import {mediaGridColumns,mediaGridRows} from '@/composables/albumLayout'
import MemoryImage from './MemoryImage.vue'
import MemoryMediaPlaceholder from './MemoryMediaPlaceholder.vue'
import MediaViewer from './MediaViewer.vue'
import {notify} from '@/composables/useToast'
const props=defineProps<{memory:Memory;featured?:boolean}>()
const emit=defineEmits<{changed:[]}>()
const busy=ref(false),viewerIndex=ref(0),viewerOpen=ref(false)
const media=computed(()=>Array.from(new Set([...(props.memory.mediaUrls||[]),props.memory.coverUrl].filter((url):url is string=>Boolean(url)))))
const gridMedia=computed(()=>media.value.slice(0,9))
const excerpt=computed(()=>{const content=props.memory.content?.trim()||'';return content===props.memory.title.trim()?'':content})
const failedSources=ref(new Set<string>()),mediaAttempt=ref(0)
const mediaUnavailable=computed(()=>gridMedia.value.length>0&&gridMedia.value.every(url=>failedSources.value.has(url)))
function mediaFailed(source:string){failedSources.value=new Set([...failedSources.value,source])}
function retryMedia(){failedSources.value=new Set();mediaAttempt.value++}
watch(()=>props.memory.mediaUrls,()=>{failedSources.value=new Set()})
const tags=computed(()=>props.memory.tags?.filter(Boolean)||[])
function isVideo(url:string){return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url)}
function openViewer(index:number){viewerIndex.value=index;viewerOpen.value=true}
async function like(event:Event){event.preventDefault();event.stopPropagation();if(busy.value)return;busy.value=true;try{const state=await memoryApi.like(props.memory.id);props.memory.liked=state.liked;props.memory.likesCount=state.likesCount;emit('changed')}catch(e){notify((e as Error).message,'error')}finally{busy.value=false}}
</script>
<template>
 <article class="memory-post u228-c" :class="{'is-note':!media.length,'is-media-unavailable':mediaUnavailable,'is-featured':featured}">
  <div class="u228-in memory-card-surface">
  <div v-if="media.length" class="memory-media-frame">
   <MemoryMediaPlaceholder v-if="mediaUnavailable" @retry="retryMedia"/>
   <div v-else :key="mediaAttempt" class="memory-media-grid" :class="[`memory-collage-${Math.min(media.length,9)}`]" :style="{'--media-columns':mediaGridColumns(media.length),'--media-rows':mediaGridRows(media.length)}" :aria-label="`${memory.title}的图片与视频宫格`">
    <div v-for="(url,index) in gridMedia" :key="url" class="memory-media-tile"><video v-if="isVideo(url)" :src="url" muted playsinline preload="metadata" class="memory-tile-video" @error="mediaFailed(url)"></video><MemoryImage v-else :src="url" :alt="`${memory.title} · 第${index+1}张照片`" compact @failed="mediaFailed"/><button type="button" class="memory-media-open" :aria-label="`预览第${index+1}${isVideo(url)?'个视频':'张照片'}：${memory.title}`" @click="openViewer(index)"><span v-if="isVideo(url)" class="memory-video-play"><Play aria-hidden="true" :size="18" fill="currentColor"/></span><span v-if="index===8&&media.length>9" class="memory-media-more">+{{media.length-9}}</span></button></div>
    <span v-if="media.length>1" class="memory-media-count">{{media.length}} 个影像</span>
   </div>
   <time class="memory-photo-date" :datetime="memory.memoryDate">{{memory.memoryDate}}</time>
   <span v-if="memory.location&&!mediaUnavailable" class="memory-photo-location"><MapPin aria-hidden="true" :size="11"/>{{memory.location}}</span>
  </div>
  <div class="memory-post-copy">
   <div v-if="!media.length" class="memory-note-top"><time :datetime="memory.memoryDate">{{memory.memoryDate}}</time></div>
   <header class="memory-card-header"><RouterLink :to="`/memories/${memory.id}`" class="memory-story-link"><h2 :title="memory.title">{{memory.title}}</h2></RouterLink><span class="memory-author-name" :title="memory.authorName">{{memory.authorName}}</span></header>
   <RouterLink v-if="excerpt" :to="`/memories/${memory.id}`" class="memory-summary-link"><p class="memory-excerpt">{{excerpt}}</p></RouterLink>
   <audio v-if="memory.audioUrl" :src="memory.audioUrl" controls preload="none" class="memory-inline-audio" :aria-label="`${memory.title}的声音`"></audio>
   <p v-if="!media.length&&memory.location" class="memory-post-location"><MapPin aria-hidden="true" :size="11"/>{{memory.location}}</p>
   <div class="memory-card-spacer" aria-hidden="true"></div>
   <footer class="memory-post-footer">
    <div v-if="tags.length" class="memory-post-tags" aria-label="记忆标签"><span v-for="tag in tags.slice(0,3)" :key="tag" :title="tag">#{{tag}}</span><span v-if="tags.length>3" class="memory-tags-more">+{{tags.length-3}}</span></div><span v-else></span>
    <div class="memory-post-actions"><button :disabled="busy" :aria-label="memory.liked?'取消喜欢':'喜欢这段记忆'" :aria-pressed="memory.liked" @click="like"><Heart aria-hidden="true" :size="14" :fill="memory.liked?'currentColor':'none'"/><span>{{memory.likesCount}}</span></button><RouterLink :to="`/memories/${memory.id}`" :aria-label="`查看${memory.commentsCount}条回应`"><MessageCircle aria-hidden="true" :size="14"/><span>{{memory.commentsCount}}</span></RouterLink><RouterLink :to="`/memories/${memory.id}`" aria-label="翻阅完整记忆" class="memory-read-story"><ArrowUpRight aria-hidden="true" :size="14"/></RouterLink></div>
   </footer>
  </div>
  </div>
  <MediaViewer v-model:open="viewerOpen" v-model:index="viewerIndex" :media="media" :title="memory.title"/>
 </article>
</template>
<style scoped>
.memory-post{position:relative;min-width:0;min-height:300px;height:100%;border:0;overflow:hidden;transition:transform .55s var(--ux-spring)}
.memory-card-surface{display:flex;flex-direction:column;min-height:300px;width:100%;height:100%}
.memory-post.is-note .memory-card-surface{background:radial-gradient(ellipse at 95% 0,#b69bb817,transparent 65%),#182232}
.memory-post.is-note::before{content:'✦';position:absolute;right:22px;top:24px;font-size:17px;color:#dfc39987;z-index:1;pointer-events:none}
.memory-post-copy{display:flex;flex-direction:column;flex:1;min-width:0;padding:20px}
.memory-card-header{display:flex;align-items:baseline;gap:12px;min-width:0}.memory-story-link{display:block;flex:1;min-width:0;text-decoration:none}
.memory-story-link h2{font-family:Georgia,"Songti SC",serif;font-size:22px;line-height:1.5;font-weight:400;color:#f7f7f8;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;transition:color .2s}.memory-story-link:hover h2{color:#e2b873}
.memory-author-name{flex-shrink:0;max-width:70px;overflow:hidden;white-space:nowrap;text-overflow:ellipsis;color:#9996a2;font-size:10px}
.memory-summary-link{display:block;min-width:0;text-decoration:none}.memory-excerpt{margin:10px 0 0;font-size:12px;line-height:1.65;color:#9ca3af;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.memory-note-top{display:flex;align-items:center;justify-content:flex-end;margin:2px 0 20px}.memory-note-top time{font-size:10px;font-variant-numeric:tabular-nums;color:#a29ba5;background:#ffffff06;border:1px solid #ffffff0b;padding:4px 9px;border-radius:20px}
.is-note .memory-story-link h2{font-family:Georgia,"Songti SC",serif;font-size:24px}.is-note .memory-excerpt{margin-top:15px;line-height:1.95;font-style:normal;color:#b6b3be}
.memory-media-frame{position:relative;flex:none;aspect-ratio:16/9;overflow:hidden;background:#1b1d28}.memory-media-grid{position:absolute;inset:0;display:grid;grid-template-columns:repeat(var(--media-columns),minmax(0,1fr));grid-template-rows:repeat(var(--media-rows),minmax(0,1fr));gap:3px;overflow:hidden}.memory-media-tile{position:relative;min-width:0;min-height:0;overflow:hidden}
.memory-media-grid :deep(.u258-tl){position:absolute;inset:0;height:100%}.memory-tile-video,.memory-media-grid :deep(img){width:100%;height:100%;object-fit:cover}.memory-tile-video{position:absolute;inset:0}.memory-media-open{position:absolute;inset:0;z-index:2;border:0;background:transparent;display:grid;place-items:center}.memory-media-open:focus-visible{outline:2px solid #e2b873;outline-offset:-4px}
.memory-collage-3 .memory-media-tile:first-child,.memory-collage-5 .memory-media-tile:first-child{grid-column:span 2;grid-row:span 2}.memory-collage-7 .memory-media-tile:first-child{grid-column:span 3}.memory-collage-8 .memory-media-tile:first-child{grid-column:span 2}
.memory-media-grid:after{content:"";position:absolute;inset:0;pointer-events:none;background:linear-gradient(transparent 50%,#080a1338);z-index:1}
.memory-photo-date{position:absolute;right:12px;top:12px;padding:5px 9px;border-radius:20px;background:#171d25a6;color:#f0e7da;font-size:9px;font-weight:550;pointer-events:none;z-index:3;font-variant-numeric:tabular-nums}.memory-photo-location{position:absolute;bottom:10px;left:12px;display:flex;align-items:center;gap:4px;padding:3px 7px;border-radius:20px;background:#10131c99;color:#e0d5c8;font-size:10px;max-width:70%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;z-index:3;pointer-events:none}
.memory-media-count{position:absolute;right:9px;bottom:9px;padding:4px 7px;background:#151925a6;border-radius:5px;color:#e3c18c;font-size:10px;pointer-events:none;z-index:3}.memory-media-more{position:absolute;inset:0;display:grid;place-items:center;font-size:24px;color:#efd4a7;background:#1a1725b3}.memory-video-play{display:grid;place-items:center;width:32px;height:32px;border-radius:50%;background:#070b1690;color:#fff}

.memory-post-footer{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:auto;padding-top:12px;position:relative}.memory-post-footer:before{content:"";position:absolute;left:0;right:0;top:0;height:1px;background:#ffffff14}.memory-card-spacer{flex:1;min-height:16px}.is-note .memory-post-copy{padding:24px}
.memory-post-tags{display:flex;align-items:center;gap:5px;min-width:0;overflow:hidden}.memory-post-tags>span{min-width:0;max-width:110px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:9px;padding:4px 8px;border:1px solid #ffffff0a;border-radius:20px;background:#ffffff09;color:#c6bec3}.memory-post-tags .memory-tags-more{padding-inline:3px;background:transparent;border:0;flex-shrink:0}
.memory-post-actions{display:flex;align-items:center;gap:8px;flex-shrink:0}.memory-post-actions button,.memory-post-actions a{display:flex;align-items:center;justify-content:center;gap:4px;color:#9b96a3;min-height:36px;min-width:32px;font-size:10px;transition:color .2s}.memory-post-actions button:hover,.memory-post-actions a:hover{color:#e2b873}.memory-post-actions button[aria-pressed=true]{color:#df929a}.memory-post-actions .memory-read-story{min-width:32px;color:#bcaa90}
.memory-inline-audio{width:100%;height:36px;margin-top:14px;color-scheme:dark}.memory-post-location{display:flex;gap:4px;align-items:center;color:#aaa0aa;font-size:10px;margin-top:12px}
@media(hover:hover) and (pointer:fine){.memory-post:hover{transform:translateY(-5px)}}
@media(max-width:767px){.memory-media-frame{aspect-ratio:16/9}.memory-post{min-height:300px}.memory-post-actions button,.memory-post-actions a{min-height:44px;min-width:30px}}
@media(prefers-reduced-motion:reduce){.memory-post,.memory-story-link h2,.memory-post-actions button,.memory-post-actions a{transition:none}.memory-post:hover{transform:none}}
</style>

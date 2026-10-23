<!-- Lucian UX/UI Prompt #050 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#050 -->
<script setup lang="ts">
import {ref} from 'vue'
import {Heart} from 'lucide-vue-next'
import type {Memory} from '@/types'
import {memoryApi} from '@/api'
import {useMemoryStore} from '@/stores/memories'
import {notify} from '@/composables/useToast'
import MemoryImage from './MemoryImage.vue'
import '@/styles/lucian-050.css'
const props=defineProps<{memory:Memory}>()
const memoryStore=useMemoryStore()
const root=ref<HTMLElement>(),live=ref(false),busy=ref(false)
function move(e:PointerEvent){
  if(e.pointerType==='touch'||!root.value)return
  const r=root.value.getBoundingClientRect(),x=(e.clientX-r.left)/r.width,y=(e.clientY-r.top)/r.height
  live.value=true;root.value.style.setProperty('--mx',`${x*100}%`);root.value.style.setProperty('--my',`${y*100}%`)
  if(window.matchMedia('(prefers-reduced-motion: reduce)').matches)return
  root.value.style.setProperty('--ry',`${(x-.5)*14}deg`);root.value.style.setProperty('--rx',`${(.5-y)*10}deg`)
}
function leave(){live.value=false;root.value?.style.setProperty('--ry','0deg');root.value?.style.setProperty('--rx','0deg')}
async function like(){if(busy.value)return;busy.value=true;try{const state=await memoryApi.like(props.memory.id);Object.assign(props.memory,state);const cached=memoryStore.items.find(item=>item.id===props.memory.id);if(cached)Object.assign(cached,state)}catch(e){notify((e as Error).message,'error')}finally{busy.value=false}}
</script>
<template><article ref="root" class="ux u050 spotlight-story" :class="{'is-live':live}" @pointermove="move" @pointerleave="leave"><div class="u050-card" :class="{'is-read':memory.liked}"><span class="u050-glare" aria-hidden="true"></span><div class="u050-img story-image"><MemoryImage :src="memory.coverUrl||memory.mediaUrls?.[0]||''" :alt="memory.title" :width="340" :height="178" eager/><RouterLink class="story-photo-open" :to="`/memories/${memory.id}`" :aria-label="`重温：${memory.title}`"/><b>星海拾遗 / FEATURED</b></div><div class="u050-body"><div class="u050-src"><span class="story-avatar" aria-hidden="true">{{memory.authorName.slice(0,1)}}</span>{{memory.authorName}}<span class="u050-dot" role="img" :aria-label="memory.liked?'已喜欢':'一段闪光的回忆'"></span></div><RouterLink :to="`/memories/${memory.id}`"><h2 class="u050-h">{{memory.title}}</h2></RouterLink><p class="u050-p">{{memory.content}}</p></div><div class="u050-foot"><time :datetime="memory.memoryDate">{{memory.memoryDate.split('-').join(' . ')}}</time><button class="u050-btn" type="button" :aria-pressed="memory.liked" :disabled="busy" @click="like"><Heart :size="15" aria-hidden="true" :fill="memory.liked?'currentColor':'none'"/><span>{{memory.liked?'已喜欢':'喜欢'}} · {{memory.likesCount}}</span></button></div></div></article></template>
<style>
.spotlight-story.u050{max-width:100%;--ux-surface:#1c2635;--ux-ink:#f0e3cf;--ux-ink-2:#b8becb;--ux-ink-3:#aeb3bf;--ux-mint:#dfb986;--ux-accent:#e9c99d;--ux-line:#ebd4ae42}
.spotlight-story .story-image{display:block}.spotlight-story .story-photo-open{position:absolute;inset:0;z-index:1}.spotlight-story .story-image>b{pointer-events:none;z-index:2}.spotlight-story .story-image>div{height:100%;border-radius:0}.spotlight-story .story-image img{width:100%;height:100%;object-fit:cover;transition:transform .6s var(--ux-ease)}.spotlight-story:focus-within img{transform:scale(1.04)}.spotlight-story .story-avatar{display:grid;place-items:center;width:22px;height:22px;background:#e8cda91a;border-radius:50%;color:#e6cba8;font-size:10px}.spotlight-story .u050-h{font:400 24px/1.4 'Songti SC',serif;letter-spacing:.02em;display:-webkit-box;-webkit-line-clamp:1;-webkit-box-orient:vertical;overflow:hidden}.spotlight-story .u050-p{line-height:1.8;margin-top:7px}.spotlight-story .u050-btn svg{position:static;stroke-width:1.6}.spotlight-story a:focus-visible{outline-offset:-3px}.spotlight-story .u050-btn svg path{stroke-dasharray:none;stroke-dashoffset:0}
@media(prefers-reduced-motion:reduce){.spotlight-story .u050-card{transform:none!important}}
</style>

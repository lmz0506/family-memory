<script setup lang="ts">
import {computed, onMounted, onBeforeUnmount, ref} from 'vue'
import {Archive, CheckCircle, ChevronRight, Clock, Lock, LoaderCircle, Plus, Sparkles, X} from 'lucide-vue-next'
import {capsuleApi} from '@/api'
import {useFamilyStore} from '@/stores/family'
import type {Capsule} from '@/types'
import FlipNumber from '@/components/FlipNumber.vue'
import WaxSeal from '@/components/WaxSeal.vue'
import SkySelect from '@/components/SkySelect.vue'
import SkyPageHeader from '@/components/SkyPageHeader.vue'
import {readViewState,writeViewState} from '@/composables/viewState'



const family = useFamilyStore()
const capsules = ref<Capsule[]>([])
const filter = ref<'all' | 'pending' | 'opened'>('all')
const loading = ref(true)
const loadError = ref('')
const filterOptions=computed(()=>[{value:'all',label:'全部'},{value:'pending',label:`待开启 ${pendingCount.value}`},{value:'opened',label:`已开启 ${openedCount.value}`}])
function openCapsule(){window.dispatchEvent(new Event('family-capsule-compose'))}
function changeFilter(value:string){filter.value=value as typeof filter.value}
const next = computed(() => capsules.value.filter(item => !item.opened).sort((a,b) => a.daysRemaining - b.daysRemaining)[0])
const pendingCount = computed(() => capsules.value.filter(item => !item.opened).length)
const openedCount = computed(() => capsules.value.filter(item => item.opened).length)
const filteredCapsules = computed(() => filter.value === 'all' ? capsules.value : capsules.value.filter(item => filter.value === 'opened' ? item.opened : !item.opened))

async function load(){if(!family.current)return;loading.value=true;loadError.value='';try{capsules.value=await capsuleApi.list(family.current.id)}catch(e){loadError.value=(e as Error).message}finally{loading.value=false}}
onMounted(async () => { if (!family.current) await family.load(); const saved=readViewState<{filter:typeof filter.value;scroll:number}>(`vault:${family.current?.id}`);if(saved)filter.value=saved.filter;await load();if(saved)requestAnimationFrame(()=>document.querySelector('[data-app-scroll]')?.scrollTo({top:saved.scroll}));window.addEventListener('family-capsule-created',load) })
onBeforeUnmount(()=>{writeViewState(`vault:${family.current?.id}`,{filter:filter.value,scroll:document.querySelector('[data-app-scroll]')?.scrollTop||0});window.removeEventListener('family-capsule-created',load)})

</script>

<template>
  <section class="page-shell page-reveal space-y-6 pb-8">
    <SkyPageHeader title="珍藏" description="把今天的心意，留给以后的某一天。"><button class="sky-primary" @click="openCapsule"><Plus :size="17"/>封存一封信</button></SkyPageHeader>

    <div v-if="next" class="capsule-next relative overflow-hidden rounded-2xl border border-night-line-strong/50 bg-gradient-to-br from-night-raised to-night-page p-6 text-white shadow-soft md:p-8">
      <div class="pointer-events-none absolute right-0 top-0 h-64 w-64 rounded-full bg-amber-500/10 blur-2xl"></div>
      <div class="relative z-10 flex flex-col items-start justify-between gap-6 lg:flex-row lg:items-center">
        <div class="max-w-2xl space-y-4">
          <div class="flex flex-wrap items-center gap-3"><span class="flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/20 px-3 py-1 text-xs font-semibold text-amber-300"><Sparkles :size="14"/>{{next.daysRemaining<=30?'即将开启':'下一封来信'}}</span><span class="text-xs text-night-muted">寄语给：<strong class="text-white">{{ next.recipient || '未来的家人' }}</strong></span></div>
          <h2 class="font-serif text-2xl font-bold text-night-ink">{{ next.title }}</h2>
          <p class="line-clamp-2 text-sm leading-7 text-night-muted">{{ next.opened ? next.content || '这是一份交给时间保存的珍贵心意。' : '这封信，正在等待约定的日子。' }}</p>
          <div class="flex flex-wrap items-center gap-4 pt-1 text-xs text-night-muted"><span class="flex items-center gap-1"><Clock :size="14" class="text-night-accent"/>创建于 {{ next.createdAt.slice(0,10) }}</span><span class="flex items-center gap-1"><Lock :size="14" class="text-amber-400"/>解锁时间：{{ next.openDate }}</span></div>
        </div>
        <div class="capsule-countdown flex min-w-[220px] flex-col items-center justify-center rounded-2xl border border-night-line/10 bg-black/30 p-5 backdrop-blur-md"><span class="mb-2 text-xs font-semibold uppercase tracking-widest text-night-muted">距离约定的日子</span><div class="flex items-baseline gap-2"><FlipNumber class="capsule-days" :value="Math.max(0,next.daysRemaining)"/><span class="text-sm font-semibold text-night-secondary">天</span></div><span class="mt-2 text-xs text-night-muted">约定开启日 · {{ next.openDate }}</span></div>
      </div>
    </div>

    <div class="flex flex-col items-start justify-between gap-4 border-b border-night-line/60 pb-3 sm:flex-row sm:items-center">
      <div class="sky-year-filter"><SkySelect id="vault-state" :model-value="filter" :options="filterOptions" label="珍藏状态" @update:model-value="changeFilter"/></div>
      <span class="text-xs text-night-muted">共 {{ capsules.length }} 份珍藏</span>
    </div>

    <div v-if="loading" class="grid gap-6 sm:grid-cols-2"><div v-for="n in 2" :key="n" class="h-64 animate-pulse rounded-2xl bg-night-soft"></div></div>
    <div v-else-if="loadError" class="empty-state" role="alert"><p>{{loadError}}</p><button @click="load" class="sky-primary rounded-full bg-night-action px-5 py-3 text-night-action-ink">重新加载</button></div>
    <div v-else-if="filteredCapsules.length" class="capsule-grid">
      <button v-for="item in filteredCapsules" :key="item.id" @click="$router.push(`/capsules/${item.id}`)" class="capsule-card group flex flex-col justify-between space-y-4 rounded-2xl border p-6 text-left shadow-soft transition-all  hover:shadow-soft-lg" :class="item.opened?'is-opened':'border-night-line bg-night-surface'">
        <div class="capsule-art" aria-hidden="true"><WaxSeal :opened="item.opened"/></div>
        <div class="space-y-3"><div class="flex items-center justify-between gap-3"><span class="flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold" :class="item.opened?'bg-night-positive/10 text-night-positive':'bg-night-accent/10 text-night-accent'"><CheckCircle v-if="item.opened" :size="12"/><Lock v-else :size="12"/>{{ item.opened ? '已开启' : '待解锁' }}</span><span class="truncate text-xs font-medium text-night-muted">写给：{{ item.recipient || '未来的家人' }}</span></div><h3 class="text-base font-bold text-night-ink transition-colors group-hover:text-night-accent">{{ item.title }}</h3><p class="line-clamp-2 text-sm leading-7 text-night-secondary">{{ item.opened ? item.content || '一份交给未来的心意。' : '封存的心意，会在约定的日子与你重逢。' }}</p></div>
        <div class="flex items-center justify-between border-t pt-4 text-xs" :class="item.opened?'border-night-accent/10 text-night-accent':'border-night-line text-night-muted'"><span class="flex items-center gap-1"><Clock :size="14" class="text-night-accent"/>{{ item.openDate }} {{ item.opened?'已开启':'开启' }}</span><span v-if="item.opened" class="flex items-center gap-0.5 font-semibold transition-transform group-hover:translate-x-1">查看内容<ChevronRight :size="16"/></span><span v-else class="rounded-md bg-night-accent/5 px-2 py-0.5 font-bold text-night-accent">剩余 {{ item.daysRemaining }} 天</span></div>
      </button>
    </div>
    <div v-else class="rounded-2xl border border-dashed border-night-line bg-night-surface/60 py-20 text-center text-sm text-night-muted">没有符合当前条件的珍藏</div>
  </section>


</template>

<style scoped>
.capsule-grid{display:grid;gap:20px;grid-template-columns:repeat(auto-fill,minmax(min(100%,290px),1fr))}.capsule-card{background:var(--ux-surface);border-color:var(--ux-line);box-shadow:none;padding:22px}.capsule-card:hover{border-color:rgb(var(--sky-accent)/.35);box-shadow:none}.capsule-card h3{font-weight:500;font-size:17px}.capsule-art{width:42px;height:42px;border-radius:12px;background:var(--ux-accent-soft);display:grid;place-items:center;color:var(--ux-accent)}.capsule-next{padding:23px 26px;border-color:var(--ux-line-2);background:linear-gradient(120deg,#25232d,#171d29);box-shadow:none}.capsule-next h2{font-weight:500;font-size:23px}.capsule-countdown{min-width:170px;background:#0b101b50;border-color:var(--ux-line);backdrop-filter:none;padding:16px}.capsule-countdown .text-4xl{font-size:32px;color:var(--ux-accent)}
@media(max-width:767px){.capsule-next{padding:20px}.capsule-countdown{width:100%;align-items:flex-start;min-width:0}.capsule-grid{gap:16px}.capsule-card{padding:20px}}
.capsule-next{position:relative;min-height:225px;padding:30px 35px;background:radial-gradient(ellipse at 85% 100%,#79603618,transparent 65%),linear-gradient(115deg,#23202d,#171e28 65%,#24221d);border-color:#e2b87330;overflow:hidden}.capsule-next:after{content:"";position:absolute;inset:0;pointer-events:none;background:repeating-linear-gradient(125deg,transparent 0px,transparent 8px,#e2b87303 9px,transparent 10px)}.capsule-countdown{min-width:250px;background:transparent;border:0;padding:0 10px;align-items:center}.capsule-days{font-family:Georgia,"Songti SC",serif;font-size:clamp(65px,6vw,104px);line-height:1.05;letter-spacing:-.045em;font-weight:400}.capsule-card{position:relative;background:linear-gradient(140deg,#24212699,#1a1c20);border-color:#e2b8731c}.capsule-card:before{content:"";position:absolute;left:22px;right:22px;top:37px;height:1px;background:linear-gradient(90deg,transparent,#bba17c27,transparent);pointer-events:none}.capsule-card.is-opened{background:linear-gradient(140deg,#26292255,#1a1c20)}.capsule-art{width:64px;height:64px;border-radius:0;background:none;position:relative;z-index:1;margin-bottom:9px}.capsule-card:hover :deep(.wax-seal){transform:rotate(-4deg);transition:transform .25s}.capsule-next h2{font-size:27px}.capsule-grid{gap:22px}
@media(max-width:767px){.capsule-next{padding:24px}.capsule-countdown{min-width:0;align-items:flex-start}.capsule-days{font-size:70px}.capsule-next h2{font-size:23px}}@media(prefers-reduced-motion:reduce){.capsule-card:hover :deep(.wax-seal){transform:none;transition:none}}
</style>

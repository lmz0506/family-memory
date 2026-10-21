<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {Archive, CheckCircle, ChevronRight, Clock, Lock, LoaderCircle, Plus, Sparkles, X} from 'lucide-vue-next'
import {capsuleApi} from '@/api'
import {useFamilyStore} from '@/stores/family'
import type {Capsule} from '@/types'
import SegmentedControl from '@/components/SegmentedControl.vue'
import {useDialog} from '@/composables/useDialog'
import {notify} from '@/composables/useToast'

const family = useFamilyStore()
const capsules = ref<Capsule[]>([])
const filter = ref<'all' | 'pending' | 'opened'>('all')
const modal = ref(false)
const error = ref('')
const saving = ref(false)
const loading = ref(true)
const loadError = ref('')
const panel = ref<HTMLElement>()
const filterOptions=computed(()=>[{value:'all',label:'全部'},{value:'pending',label:`待开启 ${pendingCount.value}`},{value:'opened',label:`已开启 ${openedCount.value}`}])
useDialog(modal,panel,()=>{if(!saving.value)modal.value=false})
function close(){if(!saving.value)modal.value=false}
function minDate(){const date=new Date();return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`}
function changeFilter(value:string){filter.value=value as typeof filter.value}
const form = reactive({title: '', content: '', recipient: '', openDate: ''})
const next = computed(() => capsules.value.filter(item => !item.opened).sort((a,b) => a.daysRemaining - b.daysRemaining)[0])
const pendingCount = computed(() => capsules.value.filter(item => !item.opened).length)
const openedCount = computed(() => capsules.value.filter(item => item.opened).length)
const filteredCapsules = computed(() => filter.value === 'all' ? capsules.value : capsules.value.filter(item => filter.value === 'opened' ? item.opened : !item.opened))

async function load(){if(!family.current)return;loading.value=true;loadError.value='';try{capsules.value=await capsuleApi.list(family.current.id)}catch(e){loadError.value=(e as Error).message}finally{loading.value=false}}
onMounted(async () => { if (!family.current) await family.load(); await load() })

async function create() {
  if (!family.current || saving.value) return
  saving.value = true; error.value = ''
  try {
    await capsuleApi.create(family.current.id, form)
    modal.value = false
    Object.assign(form, {title: '', content: '', recipient: '', openDate: ''})
    notify('这封信，已经交给时间保管')
    await load()
  } catch (e) { error.value = (e as Error).message } finally { saving.value = false }
}
</script>

<template>
  <section class="page-shell page-reveal space-y-6 pb-8">
    <header class="flex flex-wrap items-center justify-between gap-5"><div><p class="page-eyebrow">LET TIME DELIVER THE LOVE</p><h1 class="page-title mt-2">写给未来的我们</h1><p class="mt-2 text-sm leading-7 text-warm-500">将此刻的心意封存，在约定的日子，与家人一起打开。</p></div><button @click="modal=true" class="flex min-h-12 items-center justify-center gap-2 rounded-full bg-brand-coral px-6 text-sm font-semibold text-white transition hover:-translate-y-0.5 hover:bg-brand-coral-hover"><Plus :size="18"/>封存一封信</button></header>

    <div v-if="next" class="capsule-next relative overflow-hidden rounded-3xl border border-warm-700/50 bg-gradient-to-br from-warm-800 to-warm-900 p-6 text-white shadow-soft md:p-8">
      <div class="pointer-events-none absolute right-0 top-0 h-64 w-64 rounded-full bg-amber-500/10 blur-2xl"></div>
      <div class="relative z-10 flex flex-col items-start justify-between gap-6 lg:flex-row lg:items-center">
        <div class="max-w-2xl space-y-4">
          <div class="flex flex-wrap items-center gap-3"><span class="flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/20 px-3 py-1 text-xs font-semibold text-amber-300"><Sparkles :size="14"/>{{next.daysRemaining<=30?'即将开启':'下一封来信'}}</span><span class="text-xs text-warm-300">寄语给：<strong class="text-white">{{ next.recipient || '未来的家人' }}</strong></span></div>
          <h2 class="font-serif text-2xl font-bold text-warm-50">{{ next.title }}</h2>
          <p class="line-clamp-2 text-sm leading-7 text-warm-300">{{ next.content || '这是一份交给时间保存的珍贵心意。' }}</p>
          <div class="flex flex-wrap items-center gap-4 pt-1 text-xs text-warm-400"><span class="flex items-center gap-1"><Clock :size="14" class="text-brand-coral"/>创建于 {{ next.createdAt.slice(0,10) }}</span><span class="flex items-center gap-1"><Lock :size="14" class="text-amber-400"/>解锁时间：{{ next.openDate }}</span></div>
        </div>
        <div class="capsule-countdown flex min-w-[220px] flex-col items-center justify-center rounded-2xl border border-white/10 bg-black/30 p-5 backdrop-blur-md"><span class="mb-2 text-xs font-semibold uppercase tracking-widest text-warm-300">距离约定的日子</span><div class="flex items-baseline gap-2"><span class="text-4xl font-extrabold tracking-tight text-amber-400">{{ next.daysRemaining }}</span><span class="text-sm font-semibold text-warm-200">天</span></div><span class="mt-2 text-xs text-warm-400">将在 {{ next.openDate }} 自动开启</span></div>
      </div>
    </div>

    <div class="flex flex-col items-start justify-between gap-4 border-b border-warm-200/60 pb-3 sm:flex-row sm:items-center">
      <SegmentedControl :model-value="filter" :options="filterOptions" label="筛选时光胶囊" @update:model-value="changeFilter"/>
      <span class="text-xs text-warm-400">共 {{ capsules.length }} 个时光胶囊</span>
    </div>

    <div v-if="loading" class="grid gap-6 sm:grid-cols-2"><div v-for="n in 2" :key="n" class="h-64 animate-pulse rounded-3xl bg-warm-100"></div></div>
    <div v-else-if="loadError" class="empty-state" role="alert"><p>{{loadError}}</p><button @click="load" class="rounded-full bg-warm-900 px-5 py-3 text-white">重新加载</button></div>
    <div v-else-if="filteredCapsules.length" class="capsule-grid">
      <button v-for="item in filteredCapsules" :key="item.id" @click="$router.push(`/capsules/${item.id}`)" class="capsule-card group flex flex-col justify-between space-y-4 rounded-3xl border p-6 text-left shadow-soft transition-all hover:-translate-y-0.5 hover:shadow-soft-lg" :class="item.opened?'border-brand-coral/20 bg-gradient-to-br from-brand-coral-light/60 to-white':'border-warm-100 bg-white'">
        <div class="capsule-art" aria-hidden="true"><span class="capsule-paper"></span><Lock v-if="!item.opened" :size="24"/><CheckCircle v-else :size="24"/></div>
        <div class="space-y-3"><div class="flex items-center justify-between gap-3"><span class="flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold" :class="item.opened?'bg-emerald-100 text-emerald-800':'bg-amber-100 text-amber-800'"><CheckCircle v-if="item.opened" :size="12"/><Lock v-else :size="12"/>{{ item.opened ? '已开启' : '待解锁' }}</span><span class="truncate text-xs font-medium text-warm-400">写给：{{ item.recipient || '未来的家人' }}</span></div><h3 class="text-base font-bold text-warm-900 transition-colors group-hover:text-brand-coral">{{ item.title }}</h3><p class="line-clamp-2 text-sm leading-7 text-warm-500">{{ item.content || '一份交给未来的心意。' }}</p></div>
        <div class="flex items-center justify-between border-t pt-4 text-xs" :class="item.opened?'border-brand-coral/10 text-brand-coral':'border-warm-100 text-warm-400'"><span class="flex items-center gap-1"><Clock :size="14" class="text-brand-coral"/>{{ item.openDate }} {{ item.opened?'已开启':'开启' }}</span><span v-if="item.opened" class="flex items-center gap-0.5 font-semibold transition-transform group-hover:translate-x-1">查看内容<ChevronRight :size="16"/></span><span v-else class="rounded-md bg-amber-50 px-2 py-0.5 font-bold text-amber-600">剩余 {{ item.daysRemaining }} 天</span></div>
      </button>
    </div>
    <div v-else class="rounded-3xl border border-dashed border-warm-200 bg-white/60 py-20 text-center text-sm text-warm-400">没有符合当前条件的时光胶囊</div>
  </section>

  <Teleport to="body"><Transition name="fade"><div v-if="modal" class="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-4 backdrop-blur-sm sm:items-center" @click.self="close"><form ref="panel" role="dialog" aria-modal="true" aria-label="封存一封信" tabindex="-1" @submit.prevent="create" class="max-h-[90dvh] overflow-y-auto w-full max-w-lg space-y-5 rounded-t-3xl border border-warm-100 bg-white p-6 shadow-2xl sm:rounded-3xl">
    <div class="flex items-center justify-between border-b border-warm-100 pb-3"><div class="flex items-center gap-2"><div class="rounded-xl bg-brand-coral-light p-2 text-brand-coral"><Archive :size="16"/></div><h2 class="text-base font-bold text-warm-900">封存一封信</h2></div><button type="button" @click="close" :disabled="saving" aria-label="关闭时光胶囊编辑" class="rounded-lg p-1 text-warm-400 hover:text-warm-800"><X :size="20"/></button></div>
    <div class="space-y-4 text-base"><input v-model="form.title" :disabled="saving" aria-label="胶囊标题" required class="w-full rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none focus:ring-2 focus:ring-brand-coral/30" placeholder="胶囊标题"/><input v-model="form.recipient" :disabled="saving" aria-label="收信人" class="w-full rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none focus:ring-2 focus:ring-brand-coral/30" placeholder="写给谁"/><textarea v-model="form.content" :disabled="saving" aria-label="写给未来的话" required rows="4" class="w-full resize-none rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none focus:ring-2 focus:ring-brand-coral/30" placeholder="想对未来说的话"></textarea><label class="block font-semibold text-warm-800">开启日期<input v-model="form.openDate" :disabled="saving" :min="minDate()" required type="date" class="mt-1 w-full rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none"/></label><p v-if="error" class="text-xs text-red-600">{{ error }}</p></div>
    <button :disabled="saving" class="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-coral py-3 text-xs font-bold text-white hover:bg-brand-coral-hover disabled:opacity-50"><LoaderCircle v-if="saving" :size="15" class="animate-spin"/>{{ saving?'正在封存...':'封存到未来' }}</button>
  </form></div></Transition></Teleport>
</template>

<style scoped>
.capsule-grid{display:grid;gap:24px;grid-template-columns:repeat(auto-fill,minmax(min(100%,290px),1fr))}
.capsule-art{position:relative;display:grid;place-items:center;height:110px;overflow:hidden;border-radius:16px;background:radial-gradient(ellipse at center,#f1dec1,#faf5ed);color:#92633d}
.capsule-paper{position:absolute;top:26px;left:50%;width:106px;height:80px;transform:translateX(-50%) rotate(-6deg);border:1px solid #e1c7a4;border-radius:6px;background:repeating-linear-gradient(#fffaf2 0px,#fffaf2 14px,#eedfca 15px,#fffaf2 16px);box-shadow:0 8px 20px #6f462516;transition:transform .5s cubic-bezier(.2,.8,.2,1)}
.capsule-art>svg{position:relative;padding:10px;width:46px;height:46px;border-radius:50%;background:#f6e7cf;box-shadow:0 4px 12px #6f46251a}.capsule-card:hover .capsule-paper{transform:translateX(-50%) translateY(-5px) rotate(0deg)}
@media(max-width:639px){.capsule-countdown{display:grid;grid-template-columns:1fr auto;gap:2px 16px;min-width:0;width:100%;padding:16px;text-align:left}.capsule-countdown>span:first-child{grid-column:1;margin:0}.capsule-countdown>div{grid-column:2;grid-row:1 / 3}.capsule-countdown>span:last-child{grid-column:1;grid-row:2;margin:0}.capsule-countdown>div>span:first-child{font-size:32px}}
</style>

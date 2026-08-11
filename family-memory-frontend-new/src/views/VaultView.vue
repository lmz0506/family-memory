<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {Archive, CheckCircle, ChevronRight, Clock, Lock, LoaderCircle, Plus, Sparkles, X} from 'lucide-vue-next'
import {capsuleApi} from '@/api'
import {useFamilyStore} from '@/stores/family'
import type {Capsule} from '@/types'

const family = useFamilyStore()
const capsules = ref<Capsule[]>([])
const filter = ref<'all' | 'pending' | 'opened'>('all')
const modal = ref(false)
const error = ref('')
const saving = ref(false)
const form = reactive({title: '', content: '', recipient: '', openDate: ''})
const next = computed(() => capsules.value.filter(item => !item.opened).sort((a,b) => a.daysRemaining - b.daysRemaining)[0])
const pendingCount = computed(() => capsules.value.filter(item => !item.opened).length)
const openedCount = computed(() => capsules.value.filter(item => item.opened).length)
const filteredCapsules = computed(() => filter.value === 'all' ? capsules.value : capsules.value.filter(item => filter.value === 'opened' ? item.opened : !item.opened))

async function load() { if (family.current) capsules.value = await capsuleApi.list(family.current.id) }
onMounted(async () => { if (!family.current) await family.load(); await load() })

async function create() {
  if (!family.current) return
  saving.value = true; error.value = ''
  try {
    await capsuleApi.create(family.current.id, form)
    modal.value = false
    Object.assign(form, {title: '', content: '', recipient: '', openDate: ''})
    await load()
  } catch (e) { error.value = (e as Error).message } finally { saving.value = false }
}
</script>

<template>
  <section class="w-full space-y-8 pb-8">
    <div class="relative overflow-hidden rounded-3xl bg-gradient-to-r from-warm-900 via-warm-800 to-warm-900 p-8 text-white shadow-soft-lg md:p-10">
      <div class="pointer-events-none absolute -bottom-10 -right-10 h-80 w-80 rounded-full bg-brand-coral/20 blur-3xl"></div>
      <div class="relative z-10 flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
        <div class="max-w-2xl space-y-3">
          <div class="inline-flex items-center gap-2 rounded-full bg-brand-coral/20 px-3 py-1 text-xs font-semibold text-brand-peach backdrop-blur-md"><Lock :size="14"/>TIME CAPSULE · 时光胶囊</div>
          <h1 class="font-serif text-3xl font-bold leading-tight md:text-4xl">写给未来的我们</h1>
          <p class="text-xs leading-relaxed text-warm-200 opacity-90 md:text-sm">把此刻的爱与感动封存，交给时间慢慢递送。无论是成年礼、纪念日，还是未来的某一天。</p>
        </div>
        <button @click="modal=true" class="flex self-stretch items-center justify-center gap-2 whitespace-nowrap rounded-2xl bg-brand-coral px-5 py-3 text-xs font-bold text-white shadow-glow transition-all hover:bg-brand-coral-hover active:scale-95 md:self-auto"><Archive :size="16"/>+ 封存此刻胶囊</button>
      </div>
    </div>

    <div v-if="next" class="relative overflow-hidden rounded-3xl border border-warm-700/50 bg-gradient-to-br from-warm-800 to-warm-900 p-6 text-white shadow-soft md:p-8">
      <div class="pointer-events-none absolute right-0 top-0 h-64 w-64 rounded-full bg-amber-500/10 blur-2xl"></div>
      <div class="relative z-10 flex flex-col items-start justify-between gap-6 lg:flex-row lg:items-center">
        <div class="max-w-2xl space-y-4">
          <div class="flex flex-wrap items-center gap-3"><span class="flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/20 px-3 py-1 text-xs font-semibold text-amber-300"><Sparkles :size="14"/>临近开启</span><span class="text-xs text-warm-300">寄语给：<strong class="text-white">{{ next.recipient || '未来的家人' }}</strong></span></div>
          <h2 class="font-serif text-2xl font-bold text-warm-50">{{ next.title }}</h2>
          <p class="line-clamp-2 text-xs leading-relaxed text-warm-300">{{ next.content || '这是一份交给时间保存的珍贵心意。' }}</p>
          <div class="flex flex-wrap items-center gap-4 pt-1 text-xs text-warm-400"><span class="flex items-center gap-1"><Clock :size="14" class="text-brand-coral"/>创建于 {{ next.createdAt }}</span><span class="flex items-center gap-1"><Lock :size="14" class="text-amber-400"/>解锁时间：{{ next.openDate }}</span></div>
        </div>
        <div class="flex min-w-[220px] flex-col items-center justify-center rounded-2xl border border-white/10 bg-black/30 p-5 backdrop-blur-md"><span class="mb-2 text-[11px] font-semibold uppercase tracking-widest text-warm-300">距离解锁仅剩</span><div class="flex items-baseline gap-2"><span class="text-4xl font-extrabold tracking-tight text-amber-400">{{ next.daysRemaining }}</span><span class="text-sm font-semibold text-warm-200">天</span></div><span class="mt-2 text-[10px] text-warm-400">将在 {{ next.openDate }} 自动开启</span></div>
      </div>
    </div>

    <div class="flex flex-col items-start justify-between gap-4 border-b border-warm-200/60 pb-3 sm:flex-row sm:items-center">
      <div class="no-scrollbar flex max-w-full items-center gap-2 overflow-x-auto">
        <button @click="filter='all'" class="whitespace-nowrap rounded-full px-4 py-2 text-xs font-semibold transition" :class="filter==='all'?'bg-warm-900 text-white shadow-sm':'border border-warm-200/60 bg-white text-warm-800 hover:bg-warm-50'">全部胶囊 & 珍藏</button>
        <button @click="filter='pending'" class="whitespace-nowrap rounded-full px-4 py-2 text-xs font-semibold transition" :class="filter==='pending'?'bg-warm-900 text-white shadow-sm':'border border-warm-200/60 bg-white text-warm-800 hover:bg-warm-50'">🔒 待开启胶囊 ({{ pendingCount }})</button>
        <button @click="filter='opened'" class="whitespace-nowrap rounded-full px-4 py-2 text-xs font-semibold transition" :class="filter==='opened'?'bg-warm-900 text-white shadow-sm':'border border-warm-200/60 bg-white text-warm-800 hover:bg-warm-50'">✨ 已开启回忆 ({{ openedCount }})</button>
      </div>
      <span class="text-xs text-warm-400">共 {{ capsules.length }} 个时光胶囊</span>
    </div>

    <div v-if="filteredCapsules.length" class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
      <button v-for="item in filteredCapsules" :key="item.id" @click="$router.push(`/capsules/${item.id}`)" class="group flex flex-col justify-between space-y-4 rounded-3xl border p-6 text-left shadow-soft transition-all hover:-translate-y-0.5 hover:shadow-soft-lg" :class="item.opened?'border-brand-coral/20 bg-gradient-to-br from-brand-coral-light/60 to-white':'border-warm-100 bg-white'">
        <div class="space-y-3"><div class="flex items-center justify-between gap-3"><span class="flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="item.opened?'bg-emerald-100 text-emerald-800':'bg-amber-100 text-amber-800'"><CheckCircle v-if="item.opened" :size="12"/><Lock v-else :size="12"/>{{ item.opened ? '已开启' : '待解锁' }}</span><span class="truncate text-xs font-medium text-warm-400">写给：{{ item.recipient || '未来的家人' }}</span></div><h3 class="text-base font-bold text-warm-900 transition-colors group-hover:text-brand-coral">{{ item.title }}</h3><p class="line-clamp-2 text-xs leading-relaxed text-warm-500">{{ item.content || '一份交给未来的心意。' }}</p></div>
        <div class="flex items-center justify-between border-t pt-4 text-xs" :class="item.opened?'border-brand-coral/10 text-brand-coral':'border-warm-100 text-warm-400'"><span class="flex items-center gap-1"><Clock :size="14" class="text-brand-coral"/>{{ item.openDate }} {{ item.opened?'已开启':'开启' }}</span><span v-if="item.opened" class="flex items-center gap-0.5 font-semibold transition-transform group-hover:translate-x-1">查看内容<ChevronRight :size="16"/></span><span v-else class="rounded-md bg-amber-50 px-2 py-0.5 font-bold text-amber-600">剩余 {{ item.daysRemaining }} 天</span></div>
      </button>
    </div>
    <div v-else class="rounded-3xl border border-dashed border-warm-200 bg-white/60 py-20 text-center text-sm text-warm-400">没有符合当前条件的时光胶囊</div>
  </section>

  <Transition name="fade"><div v-if="modal" class="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-4 backdrop-blur-sm sm:items-center" @click.self="modal=false"><form @submit.prevent="create" class="w-full max-w-lg space-y-5 rounded-t-3xl border border-warm-100 bg-white p-6 shadow-2xl sm:rounded-3xl">
    <div class="flex items-center justify-between border-b border-warm-100 pb-3"><div class="flex items-center gap-2"><div class="rounded-xl bg-brand-coral-light p-2 text-brand-coral"><Archive :size="16"/></div><h2 class="text-base font-bold text-warm-900">封存一封信</h2></div><button type="button" @click="modal=false" class="rounded-lg p-1 text-warm-400 hover:text-warm-800"><X :size="20"/></button></div>
    <div class="space-y-4 text-xs"><input v-model="form.title" required class="w-full rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none focus:ring-2 focus:ring-brand-coral/30" placeholder="胶囊标题"/><input v-model="form.recipient" class="w-full rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none focus:ring-2 focus:ring-brand-coral/30" placeholder="写给谁"/><textarea v-model="form.content" required rows="4" class="w-full resize-none rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none focus:ring-2 focus:ring-brand-coral/30" placeholder="想对未来说的话"></textarea><label class="block font-semibold text-warm-800">开启日期<input v-model="form.openDate" required type="date" class="mt-1 w-full rounded-xl border border-warm-200 bg-warm-50 p-3 outline-none"/></label><p v-if="error" class="text-xs text-red-600">{{ error }}</p></div>
    <button :disabled="saving" class="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-coral py-3 text-xs font-bold text-white hover:bg-brand-coral-hover disabled:opacity-50"><LoaderCircle v-if="saving" :size="15" class="animate-spin"/>{{ saving?'正在封存...':'封存到未来' }}</button>
  </form></div></Transition>
</template>

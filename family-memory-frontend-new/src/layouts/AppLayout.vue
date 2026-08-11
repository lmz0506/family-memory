<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  Bell, CalendarDays, Heart, Home, LockKeyhole, LogOut, PanelLeftClose, PanelLeftOpen,
  Plus, Search, Settings, Sparkles, Users
} from 'lucide-vue-next'
import {useAuthStore} from '@/stores/auth'
import {useFamilyStore} from '@/stores/family'
import {useMemoryStore} from '@/stores/memories'
import ComposerModal from '@/components/ComposerModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const family = useFamilyStore()
const memories = useMemoryStore()
const composer = ref(false)
const mobileMenu = ref(false)
const collapsed = ref(false)
const search = ref('')
const nav = [
  {name: 'home', label: '首页', icon: Home},
  {name: 'timeline', label: '时光轴', icon: CalendarDays},
  {name: 'vault', label: '珍藏', icon: LockKeyhole},
  {name: 'family', label: '家人', icon: Users}
]
const detailPage = computed(() => route.name === 'memory-detail' || route.name === 'capsule-detail')
const initial = computed(() => auth.user?.nickname?.slice(0, 1) || '家')
const currentDate = computed(() => new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
}).format(new Date()))

onMounted(async () => {
  await family.load()
  if (!family.current && route.name !== 'welcome') router.replace('/welcome')
})

function logout() {
  auth.logout()
  router.replace('/login')
}

function submitSearch() {
  if (!search.value.trim()) return
  router.push({name: 'timeline', query: {keyword: search.value.trim()}})
}

async function handleCreated() {
  await family.refresh()
  if (!family.current) return
  if (route.name === 'home') window.dispatchEvent(new Event('family-memory-created'))
  else await memories.load(family.current.id)
}
</script>

<template>
  <div class="flex h-screen w-full overflow-hidden bg-[#fafaf7] text-[#332e2b]">
    <aside class="relative z-30 hidden shrink-0 flex-col justify-between overflow-hidden border-r border-warm-100/80 bg-white/90 transition-all duration-300 lg:flex"
           :class="collapsed ? 'w-[76px]' : 'w-64'">
      <div>
        <div class="flex items-center gap-3.5 p-6">
          <div class="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-gradient-to-tr from-brand-coral to-brand-peach text-white shadow-md shadow-brand-coral/20">
            <Sparkles :size="20"/>
          </div>
          <div v-if="!collapsed" class="whitespace-nowrap">
            <h1 class="text-lg font-bold leading-none tracking-tight text-warm-900">拾光</h1>
            <span class="mt-1 block text-[10px] font-semibold uppercase tracking-wider text-warm-400">Family Memory</span>
          </div>
        </div>

        <nav class="mt-2 space-y-1 px-3">
          <RouterLink v-for="item in nav" :key="item.name" :to="{name:item.name}"
                      class="flex items-center gap-3.5 rounded-xl px-4 py-3 text-sm font-medium transition-all"
                      :title="collapsed ? item.label : undefined"
                      :class="route.name === item.name ? 'bg-brand-coral-light/80 text-brand-coral' : 'text-warm-800 hover:bg-warm-50 hover:text-brand-coral'">
            <component :is="item.icon" :size="20" class="shrink-0" :class="route.name === item.name ? '' : 'text-warm-400'"/>
            <span v-if="!collapsed" class="whitespace-nowrap">{{ item.label }}</span>
          </RouterLink>
        </nav>
      </div>

      <div class="space-y-3 p-4">
        <div v-if="!detailPage" class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-warm-900 to-warm-800 p-4 text-white shadow-lg shadow-warm-900/10">
          <div class="absolute -bottom-3 -right-3 h-20 w-20 rounded-full bg-brand-coral/20 blur-xl"></div>
          <div class="relative z-10">
            <div v-if="!collapsed">
              <div class="mb-1 flex items-center gap-2 text-xs font-medium text-brand-peach"><Heart :size="14" fill="currentColor"/>让记忆有迹可循</div>
              <p class="mb-3 text-xs leading-relaxed text-warm-200">今天也记录一件值得记住的小事吧。</p>
            </div>
            <button @click="composer=true" class="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-coral px-3 py-2.5 text-xs font-semibold text-white shadow-md transition hover:bg-brand-coral-hover active:scale-[.98]">
              <Plus :size="16"/><span v-if="!collapsed" class="whitespace-nowrap">添加记忆</span>
            </button>
          </div>
        </div>

        <div class="relative">
          <button @click="mobileMenu=!mobileMenu" class="flex w-full items-center justify-between rounded-xl p-2.5 text-left transition hover:bg-warm-100/50">
            <div class="flex min-w-0 items-center gap-3">
              <div class="relative grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-coral-light text-xs font-bold text-brand-coral ring-2 ring-warm-100">
                {{ initial }}<span class="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full bg-emerald-500 ring-2 ring-white"></span>
              </div>
              <div v-if="!collapsed" class="min-w-0 whitespace-nowrap">
                <p class="truncate text-xs font-semibold text-warm-900">{{ auth.user?.nickname || '家庭成员' }}</p>
                <p class="truncate text-[10px] text-warm-400">{{ family.current?.name || '温馨小家' }}</p>
              </div>
            </div>
            <Settings v-if="!collapsed" :size="16" class="text-warm-400"/>
          </button>
          <div v-if="mobileMenu" class="absolute bottom-full left-0 z-50 mb-2 w-full min-w-40 rounded-2xl border border-warm-100 bg-white/95 p-2 shadow-xl backdrop-blur-md">
            <button @click="logout" class="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-xs font-medium text-rose-600 hover:bg-rose-50"><LogOut :size="14"/>退出登录</button>
          </div>
        </div>
      </div>
    </aside>

    <main class="paper relative flex min-w-0 flex-1 flex-col overflow-hidden">
      <header class="z-20 flex h-16 shrink-0 items-center justify-between border-b border-warm-100/60 bg-white/60 px-4 backdrop-blur-md sm:px-6">
        <div class="flex min-w-0 flex-1 items-center gap-3 sm:gap-4">
          <button @click="collapsed=!collapsed" class="hidden rounded-xl p-2 text-warm-500 transition hover:bg-warm-100/60 hover:text-warm-800 lg:grid" title="展开/收起菜单">
            <PanelLeftOpen v-if="collapsed" :size="20"/><PanelLeftClose v-else :size="20"/>
          </button>
          <div class="flex items-center gap-2 lg:hidden">
            <div class="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-tr from-brand-coral to-brand-peach text-white"><Sparkles :size="18"/></div>
            <span class="hidden font-bold text-warm-900 sm:inline">拾光</span>
          </div>
          <form @submit.prevent="submitSearch" class="relative w-full max-w-xl">
            <Search :size="16" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-warm-400"/>
            <input v-model="search" class="w-full rounded-full border border-warm-100 bg-warm-50/80 py-2 pl-10 pr-4 text-xs text-warm-800 outline-none transition placeholder:text-warm-400 focus:border-brand-coral focus:ring-2 focus:ring-brand-coral/30"
                   placeholder="搜索珍贵回忆、照片标签、家人的留言……"/>
          </form>
        </div>
        <div class="ml-3 flex items-center gap-3">
          <button class="relative rounded-full p-2 text-warm-500 transition hover:bg-warm-100/60 hover:text-warm-800"><Bell :size="20"/><span class="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-brand-coral ring-2 ring-white"></span></button>
          <div class="hidden items-center gap-2 rounded-full bg-warm-100/50 px-3 py-1.5 text-xs font-medium text-warm-800 sm:flex"><CalendarDays :size="14" class="text-brand-coral"/>{{ currentDate }}</div>
        </div>
      </header>

      <div class="flex-1 overflow-y-auto p-4 pb-28 sm:p-6 sm:pb-28 md:p-8 lg:pb-8">
        <RouterView/>
      </div>
    </main>

    <button v-if="route.name === 'home'" aria-label="添加记忆" @click="composer=true"
            class="fixed bottom-[88px] right-5 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-brand-coral text-white shadow-glow transition hover:-translate-y-1 hover:bg-brand-coral-hover lg:hidden">
      <Plus :size="22"/>
    </button>

    <nav v-if="!detailPage" class="fixed inset-x-3 bottom-3 z-40 mx-auto flex h-[68px] max-w-md items-center justify-around rounded-[22px] border border-white/70 bg-white/90 px-2 shadow-2xl backdrop-blur-xl lg:hidden">
      <RouterLink v-for="item in nav" :key="item.name" :to="{name:item.name}" :aria-label="item.label"
                  class="flex min-w-[62px] flex-col items-center gap-1 text-[9px] font-semibold"
                  :class="route.name===item.name?'text-brand-coral':'text-warm-400'">
        <span class="grid h-8 w-10 place-items-center rounded-xl" :class="route.name===item.name?'bg-brand-coral-light':''"><component :is="item.icon" :size="19"/></span>{{ item.label }}
      </RouterLink>
    </nav>
    <ComposerModal v-if="family.current" v-model="composer" :family-id="family.current.id" @created="handleCreated"/>
  </div>
</template>

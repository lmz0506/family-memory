<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useAppStore, useMemoryStore, useAuthStore } from '@/stores'
import { IconHeart, IconCalendar, IconPlus, IconLock, IconUser, IconEdit, IconLogout } from '@/components/icons'
import AddMemoryModal from '@/components/AddMemoryModal.vue'
import YearPicker from '@/components/YearPicker.vue'
import CreateCapsuleModal from '@/components/CreateCapsuleModal.vue'
import InviteFamilyModal from '@/components/InviteFamilyModal.vue'
import JoinFamilyModal from '@/components/JoinFamilyModal.vue'
import ProfileEditModal from '@/components/ProfileEditModal.vue'
import HomeView from '@/views/HomeView.vue'
import TimelineView from '@/views/TimelineView.vue'
import FamilyView from '@/views/FamilyView.vue'
import VaultView from '@/views/VaultView.vue'

const appStore = useAppStore()
const memoryStore = useMemoryStore()
const authStore = useAuthStore()

// 用户菜单状态
const showUserMenu = ref(false)
const showProfileModal = ref(false)

// 当前日期信息
const today = new Date()
const currentYear = today.getFullYear()
const currentMonth = today.getMonth() + 1
const currentDay = today.getDate()
const currentWeekday = computed(() => {
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return weekdays[today.getDay()]
})

// 问候语库
const greetings = {
  home: [
    { sub: (name: string) => `亲爱的${name}`, main: '今天想留住什么？' },
    { sub: (name: string) => `嗨，${name}`, main: '记录一个温暖瞬间吧' },
    { sub: () => '阳光正好', main: '把美好定格在这里' },
    { sub: () => '时光温柔', main: '值得被珍藏的一天' },
    { sub: (name: string) => `${name}，早安`, main: '新的一天，新的故事' },
    { sub: () => '生活很甜', main: '每一刻都值得纪念' },
    { sub: () => '幸福满溢', main: '用心感受家的温度' },
    { sub: (name: string) => `欢迎回来，${name}`, main: '家人们都在等你' },
    { sub: () => '岁月静好', main: '愿所有美好如约而至' },
    { sub: () => '爱意满满', main: '记录属于我们的小确幸' }
  ],
  timeline: [
    { sub: '岁月如梭', main: '拾起散落的足迹' },
    { sub: '时光漫步', main: '每一步都是回忆' },
    { sub: '光阴故事', main: '翻阅过往的篇章' },
    { sub: '记忆长河', main: '那些闪闪发光的日子' },
    { sub: '往事如歌', main: '唱响岁月的旋律' },
    { sub: '流年似水', main: '留住最美的瞬间' },
    { sub: '时间印记', main: '探寻成长的轨迹' },
    { sub: '昨日重现', main: '与过去的自己相遇' },
    { sub: '岁月留声', main: '聆听时光的回响' },
    { sub: '年华似锦', main: '珍藏每一段旅程' }
  ],
  vault: [
    { sub: '绝对私密', main: '开启家庭档案馆' },
    { sub: '珍藏时刻', main: '最珍贵的都在这里' },
    { sub: '秘密花园', main: '只属于我们的宝藏' },
    { sub: '时光宝盒', main: '封存最美好的记忆' },
    { sub: '私享空间', main: '安放心底的柔软' },
    { sub: '密封回忆', main: '等待未来的自己' },
    { sub: '珍贵收藏', main: '每一件都是无价之宝' },
    { sub: '心灵港湾', main: '存放最真挚的情感' },
    { sub: '专属印记', main: '刻画家的模样' },
    { sub: '永恒珍藏', main: '时间带不走的温暖' }
  ],
  family: [
    { sub: '血脉相连', main: '最亲近的一家人' },
    { sub: '家人闲坐', main: '灯火可亲' },
    { sub: '温暖相伴', main: '有你们真好' },
    { sub: '爱的港湾', main: '家是最美的风景' },
    { sub: '亲情永恒', main: '彼此守护的力量' },
    { sub: '幸福家庭', main: '在一起就是最好' },
    { sub: '相亲相爱', main: '一家人整整齐齐' },
    { sub: '团圆时刻', main: '最幸福的样子' },
    { sub: '爱满全家', main: '每个人都很重要' },
    { sub: '携手同行', main: '一起走过的时光' }
  ]
}

// 用于存储每个 tab 当前选中的问候语索引
const greetingIndexes = ref<Record<string, number>>({
  home: Math.floor(Math.random() * greetings.home.length),
  timeline: Math.floor(Math.random() * greetings.timeline.length),
  vault: Math.floor(Math.random() * greetings.vault.length),
  family: Math.floor(Math.random() * greetings.family.length)
})

// 获取问候语
const topGreeting = computed(() => {
  const tab = appStore.activeTab
  const nickname = authStore.user?.nickname || '家人'
  const tabGreetings = greetings[tab as keyof typeof greetings]

  if (tabGreetings) {
    const index = greetingIndexes.value[tab] || 0
    const greeting = tabGreetings[index]
    return {
      sub: typeof greeting.sub === 'function' ? greeting.sub(nickname) : greeting.sub,
      main: greeting.main
    }
  }
  return { sub: '欢迎回来', main: '时光·家' }
})

// 切换 tab 时随机选择新的问候语
watch(() => appStore.activeTab, (tab) => {
  const tabGreetings = greetings[tab as keyof typeof greetings]
  if (tabGreetings) {
    greetingIndexes.value[tab] = Math.floor(Math.random() * tabGreetings.length)
  }
})

// 打开个人资料编辑
const openProfileEdit = () => {
  showUserMenu.value = false
  showProfileModal.value = true
}

// 退出登录
const handleLogout = () => {
  authStore.logout()
  memoryStore.reset()
  showUserMenu.value = false
}

// 监听 tab 切换，按需刷新数据
watch(() => appStore.activeTab, (tab) => {
  if (!memoryStore.currentFamilyId) return
  if (tab === 'home') {
    memoryStore.fetchMemories()
  } else if (tab === 'family') {
    memoryStore.fetchFamilyMembers()
    memoryStore.fetchStatistics()
  }
})

// 导航配置
const navItems = [
  { key: 'home', label: '温馨', color: 'var(--peach-main)' },
  { key: 'timeline', label: '时光', color: 'var(--timeline-green)' },
  { key: 'vault', label: '珍藏', color: 'var(--vault-gold)' },
  { key: 'family', label: '家人', color: 'var(--family-blue)' }
]

const getNavIcon = (key: string) => key
</script>

<template>
  <!-- 顶部 Header -->
  <div class="relative pt-8 pb-24 header-bg-fade px-6">
    <!-- 装饰性浮动元素 -->
    <div class="absolute inset-0 pointer-events-none">
      <!-- 浮动爱心 -->
      <div class="absolute top-6 left-[12%] w-4 h-4 text-white/50 animate-float-slow">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>
      <div class="absolute top-14 right-[18%] w-3 h-3 text-white/40 animate-float-medium">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>
      <div class="absolute top-20 left-[32%] w-2.5 h-2.5 text-white/35 animate-float-fast">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>
      <div class="absolute top-10 left-[55%] w-2 h-2 text-white/30 animate-float-medium">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>
      <div class="absolute top-24 right-[35%] w-3.5 h-3.5 text-white/45 animate-float-slow">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>

      <!-- 浮动星星 -->
      <div class="absolute top-8 right-[42%] w-3 h-3 text-white/50 animate-twinkle">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>
      <div class="absolute top-16 left-[6%] w-2.5 h-2.5 text-white/40 animate-twinkle-delay">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>
      <div class="absolute top-4 left-[42%] w-2 h-2 text-white/35 animate-twinkle">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>
      <div class="absolute top-20 right-[8%] w-2 h-2 text-white/45 animate-twinkle-delay">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>
      <div class="absolute top-12 left-[22%] w-1.5 h-1.5 text-white/30 animate-twinkle">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>

      <!-- 浮动圆点/气泡 -->
      <div class="absolute top-14 left-[48%] w-2 h-2 bg-white/35 rounded-full animate-bubble" />
      <div class="absolute top-5 left-[25%] w-1.5 h-1.5 bg-white/40 rounded-full animate-bubble-delay" />
      <div class="absolute top-18 right-[15%] w-2.5 h-2.5 bg-white/30 rounded-full animate-bubble-slow" />
      <div class="absolute top-22 left-[18%] w-1.5 h-1.5 bg-white/25 rounded-full animate-bubble" />

      <!-- 下半区爱心 -->
      <div class="absolute bottom-20 left-[10%] w-3 h-3 text-white/35 animate-float-medium">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>
      <div class="absolute bottom-14 right-[12%] w-2.5 h-2.5 text-white/30 animate-float-fast">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>
      <div class="absolute bottom-8 left-[45%] w-2 h-2 text-white/25 animate-float-slow">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </div>

      <!-- 下半区星星 -->
      <div class="absolute bottom-24 right-[30%] w-2 h-2 text-white/40 animate-twinkle">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>
      <div class="absolute bottom-10 left-[25%] w-1.5 h-1.5 text-white/35 animate-twinkle-delay">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>
      <div class="absolute bottom-16 right-[45%] w-2.5 h-2.5 text-white/30 animate-twinkle">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      </div>

      <!-- 下半区气泡 -->
      <div class="absolute bottom-12 left-[60%] w-1.5 h-1.5 bg-white/30 rounded-full animate-bubble-delay" />
      <div class="absolute bottom-6 right-[20%] w-2 h-2 bg-white/20 rounded-full animate-bubble-slow" />

      <!-- 大装饰圆 -->
      <div class="absolute -top-16 -right-16 w-40 h-40 bg-white/10 rounded-full" />
      <div class="absolute top-8 -left-12 w-28 h-28 bg-white/[0.08] rounded-full" />
      <div class="absolute bottom-4 right-4 w-20 h-20 bg-white/[0.06] rounded-full" />
    </div>

    <div class="relative flex justify-between items-start text-white mb-3">
      <Transition name="fade" mode="out-in">
        <div :key="appStore.activeTab" class="flex-1">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-sm opacity-90">{{ topGreeting.sub }}</span>
          </div>
          <h1 class="text-2xl font-serif-title font-black mb-2">{{ topGreeting.main }}</h1>
          <!-- 日期署名 -->
          <p class="text-xs text-white/60 italic">—— {{ currentYear }}年{{ currentMonth }}月{{ currentDay }}日 周{{ currentWeekday }}</p>
        </div>
      </Transition>

      <!-- 用户头像 -->
      <div class="relative">
        <div
          @click="showUserMenu = !showUserMenu"
          class="w-14 h-14 rounded-2xl overflow-hidden cursor-pointer active:scale-95 transition-transform shadow-lg ring-2 ring-white/30"
        >
          <img
            v-if="authStore.user?.avatar?.startsWith('http')"
            :src="authStore.user.avatar"
            class="w-full h-full object-cover"
            alt="头像"
          />
          <div
            v-else
            class="w-full h-full bg-white flex items-center justify-center text-2xl"
          >
            {{ authStore.user?.avatar || '👤' }}
          </div>
        </div>
        <!-- 在线状态指示点 -->
        <div class="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-green-400 rounded-full border-2 border-white shadow-sm" />

        <!-- 下拉菜单 -->
        <Transition name="fade">
          <div
            v-if="showUserMenu"
            class="absolute right-0 top-16 w-44 glass-card rounded-2xl py-2 z-20"
          >
            <div class="px-4 py-3 border-b border-white/30">
              <p class="text-sm font-bold text-stone-700 truncate">{{ authStore.user?.nickname }}</p>
              <p class="text-xs text-stone-400 truncate">{{ authStore.user?.username }}</p>
            </div>
            <button
              @click="openProfileEdit"
              class="w-full px-4 py-3 text-left text-sm text-stone-600 hover:bg-[#FFF0EE] transition-colors flex items-center"
            >
              <IconEdit :size="16" class="mr-2 text-stone-400" /> 编辑资料
            </button>
            <button
              @click="handleLogout"
              class="w-full px-4 py-3 text-left text-sm text-red-500 hover:bg-red-50/50 transition-colors flex items-center"
            >
              <IconLogout :size="16" class="mr-2" /> 退出登录
            </button>
          </div>
        </Transition>
        <!-- 点击外部关闭菜单 -->
        <div
          v-if="showUserMenu"
          class="fixed inset-0 z-10"
          @click="showUserMenu = false"
        />
      </div>
    </div>

    <!-- 里程碑卡片 -->
    <div class="absolute bottom-[-44px] left-5 right-5 glass-card rounded-[2rem] p-5 flex items-center justify-between z-10 shadow-xl shadow-[#FF8A80]/15 overflow-hidden">
      <!-- 背景装饰元素 -->
      <div class="absolute inset-0 pointer-events-none">
        <!-- 右侧装饰爱心 -->
        <div class="absolute -right-4 top-1/2 -translate-y-1/2 w-24 h-24 text-[#FF8A80]/[0.06]">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <!-- 左上小星星 -->
        <div class="absolute left-16 top-2 w-3 h-3 text-[#FFAB91]/20">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <!-- 右下小爱心 -->
        <div class="absolute right-20 bottom-2 w-2.5 h-2.5 text-[#FF8A80]/15">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <!-- 装饰圆点 -->
        <div class="absolute left-1/3 bottom-3 w-1.5 h-1.5 bg-[#FFAB91]/15 rounded-full" />
        <div class="absolute right-1/3 top-3 w-1 h-1 bg-[#FF8A80]/20 rounded-full" />
      </div>

      <div class="relative flex items-center gap-4">
        <div class="w-12 h-12 bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] rounded-2xl flex flex-col items-center justify-center text-white shadow-lg shadow-[#FF8A80]/30">
          <span class="text-[10px] leading-none opacity-80 uppercase">记录</span>
          <span class="text-lg font-bold leading-none">{{ memoryStore.statistics.memoryCount }}</span>
        </div>
        <div>
          <p class="text-[10px] text-stone-400 font-bold uppercase tracking-widest">时光里程碑</p>
          <p class="text-sm font-bold text-stone-700">并肩守护 {{ memoryStore.statistics.recordDays.toLocaleString() }} 天</p>
        </div>
      </div>
      <div class="relative flex -space-x-2">
        <div
          v-for="(member, i) in memoryStore.familyMembers.slice(0, 3)"
          :key="i"
          class="w-9 h-9 rounded-full border-2 border-white bg-white flex items-center justify-center text-sm shadow-sm overflow-hidden"
        >
          <img
            v-if="member.avatar?.startsWith('http')"
            :src="member.avatar"
            class="w-full h-full object-cover"
            alt=""
          />
          <span v-else>{{ member.avatar || '👤' }}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- 主内容区域 -->
  <main class="flex-1 overflow-y-auto no-scrollbar pt-12 pb-32 px-5">
    <HomeView v-if="appStore.activeTab === 'home'" />
    <TimelineView v-if="appStore.activeTab === 'timeline'" />
    <FamilyView v-if="appStore.activeTab === 'family'" />
    <VaultView v-if="appStore.activeTab === 'vault'" />
  </main>

  <!-- 底部导航栏 -->
  <div class="fixed bottom-4 left-1/2 -translate-x-1/2 w-[94%] max-w-[380px] bg-white/95 backdrop-blur-lg h-[72px] rounded-[2rem] flex justify-around items-center px-3 z-50 shadow-xl shadow-stone-200/50 border border-stone-100">
    <!-- Home -->
    <button
      @click="appStore.setActiveTab('home')"
      :class="[
        'flex flex-col items-center justify-center py-2 px-3 rounded-2xl transition-all duration-300 cursor-pointer hover:bg-stone-50',
        appStore.activeTab === 'home' ? 'text-[#FF8A80]' : 'text-stone-400'
      ]"
    >
      <IconHeart :size="22" :fill="appStore.activeTab === 'home'" />
      <span class="text-[10px] font-bold mt-1">温馨</span>
    </button>

    <!-- Timeline -->
    <button
      @click="appStore.setActiveTab('timeline')"
      :class="[
        'flex flex-col items-center justify-center py-2 px-3 rounded-2xl transition-all duration-300 cursor-pointer hover:bg-stone-50',
        appStore.activeTab === 'timeline' ? 'text-[#81C784]' : 'text-stone-400'
      ]"
    >
      <IconCalendar :size="22" :fill="appStore.activeTab === 'timeline'" />
      <span class="text-[10px] font-bold mt-1">时光</span>
    </button>

    <!-- Add Button -->
    <div class="relative -translate-y-4">
      <button
        @click="appStore.openAddModal()"
        class="w-14 h-14 btn-gradient text-white rounded-full flex items-center justify-center shadow-xl shadow-[#FF8A80]/40 border-4 border-white active:scale-90 transition-all cursor-pointer hover:shadow-2xl"
      >
        <IconPlus :size="28" />
      </button>
    </div>

    <!-- Vault -->
    <button
      @click="appStore.setActiveTab('vault')"
      :class="[
        'flex flex-col items-center justify-center py-2 px-3 rounded-2xl transition-all duration-300 cursor-pointer hover:bg-stone-50',
        appStore.activeTab === 'vault' ? 'text-[#FFD54F]' : 'text-stone-400'
      ]"
    >
      <IconLock :size="22" :fill="appStore.activeTab === 'vault'" />
      <span class="text-[10px] font-bold mt-1">珍藏</span>
    </button>

    <!-- Family -->
    <button
      @click="appStore.setActiveTab('family')"
      :class="[
        'flex flex-col items-center justify-center py-2 px-3 rounded-2xl transition-all duration-300 cursor-pointer hover:bg-stone-50',
        appStore.activeTab === 'family' ? 'text-[#64B5F6]' : 'text-stone-400'
      ]"
    >
      <IconUser :size="22" :fill="appStore.activeTab === 'family'" />
      <span class="text-[10px] font-bold mt-1">家人</span>
    </button>
  </div>

  <!-- 添加记忆模态框 -->
  <AddMemoryModal />

  <!-- 年份选择器 -->
  <YearPicker />

  <!-- 创建胶囊模态框 -->
  <CreateCapsuleModal />

  <!-- 邀请家人模态框 -->
  <InviteFamilyModal />

  <!-- 加入/创建家庭模态框 -->
  <JoinFamilyModal />

  <!-- 个人资料编辑模态框 -->
  <ProfileEditModal :show="showProfileModal" @close="showProfileModal = false" />
</template>

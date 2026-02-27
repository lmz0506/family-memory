<script setup lang="ts">
import { useAppStore } from '@/stores'
import type { TabType } from '@/types'
import { IconHeart, IconCalendar, IconPlus, IconLock, IconUser } from '@/components/icons'

const appStore = useAppStore()

const tabs: { key: TabType; label: string; icon: string }[] = [
  { key: 'home', label: '温馨', icon: 'heart' },
  { key: 'timeline', label: '时光轴', icon: 'calendar' },
  { key: 'vault', label: '珍藏', icon: 'lock' },
  { key: 'family', label: '家人', icon: 'user' }
]
</script>

<template>
  <div
    class="fixed bottom-0 left-0 right-0 bg-white/90 backdrop-blur-md border-t border-stone-200 pb-safe pt-2 px-6 shadow-[0_-5px_20px_rgba(0,0,0,0.03)] z-50 w-full max-w-md mx-auto"
  >
    <div class="flex justify-between items-center h-16">
      <!-- 温馨 -->
      <button
        class="flex flex-col items-center space-y-1 transition-colors"
        :class="appStore.activeTab === 'home' ? 'text-orange-600' : 'text-stone-400'"
        @click="appStore.setActiveTab('home')"
      >
        <IconHeart :size="24" :fill="appStore.activeTab === 'home'" />
        <span class="text-[10px] font-medium">温馨</span>
      </button>

      <!-- 时光轴 -->
      <button
        class="flex flex-col items-center space-y-1 transition-colors"
        :class="appStore.activeTab === 'timeline' ? 'text-orange-600' : 'text-stone-400'"
        @click="appStore.setActiveTab('timeline')"
      >
        <IconCalendar :size="24" />
        <span class="text-[10px] font-medium">时光轴</span>
      </button>

      <!-- 中间添加按钮 -->
      <div class="relative -top-5">
        <button
          class="bg-orange-500 hover:bg-orange-600 text-white rounded-full p-4 shadow-lg shadow-orange-200 transition-all hover:scale-110 active:scale-95"
          @click="appStore.openAddModal()"
        >
          <IconPlus :size="28" />
        </button>
      </div>

      <!-- 珍藏 -->
      <button
        class="flex flex-col items-center space-y-1 transition-colors"
        :class="appStore.activeTab === 'vault' ? 'text-orange-600' : 'text-stone-400'"
        @click="appStore.setActiveTab('vault')"
      >
        <IconLock :size="24" />
        <span class="text-[10px] font-medium">珍藏</span>
      </button>

      <!-- 家人 -->
      <button
        class="flex flex-col items-center space-y-1 transition-colors"
        :class="appStore.activeTab === 'family' ? 'text-orange-600' : 'text-stone-400'"
        @click="appStore.setActiveTab('family')"
      >
        <IconUser :size="24" />
        <span class="text-[10px] font-medium">家人</span>
      </button>
    </div>
  </div>
</template>

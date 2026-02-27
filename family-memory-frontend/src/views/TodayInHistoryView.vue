<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMemoryStore } from '@/stores'
import { IconChevronRight, IconMapPin, IconHeart, IconImage, IconDocument } from '@/components/icons'
import HeaderDecorations from '@/components/HeaderDecorations.vue'
import * as api from '@/api'
import type { MemoryResp } from '@/api'

const router = useRouter()
const memoryStore = useMemoryStore()

const today = new Date()
const todayMonth = today.getMonth() + 1
const todayDay = today.getDate()

const todayInHistoryMemories = ref<MemoryResp[]>([])
const loading = ref(false)

// 从API获取那年今日数据
onMounted(async () => {
  if (!memoryStore.currentFamilyId) return
  loading.value = true
  try {
    const res = await api.getTodayInHistory(memoryStore.currentFamilyId)
    todayInHistoryMemories.value = res.data
  } catch (e) {
    console.error('获取那年今日失败:', e)
  } finally {
    loading.value = false
  }
})

const getYearsAgo = (yearsAgo?: number) => {
  if (yearsAgo === undefined || yearsAgo === 0) return '今年'
  return `${yearsAgo}年前`
}

const goBack = () => {
  router.back()
}

const goToDetail = (id: number) => {
  router.push({ name: 'memory-detail', params: { id } })
}

const handleLike = async (id: number, event: Event) => {
  event.stopPropagation()
  try {
    const res = await api.likeMemory(id)
    const memory = todayInHistoryMemories.value.find(m => m.id === id)
    if (memory) {
      memory.likes = res.data.likes
    }
  } catch (e) {
    // 如果已点赞，尝试取消
    try {
      const res = await api.unlikeMemory(id)
      const memory = todayInHistoryMemories.value.find(m => m.id === id)
      if (memory) {
        memory.likes = res.data.likes
      }
    } catch (e2) {
      console.error('点赞操作失败:', e2)
    }
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#FFF9F8]">
    <!-- 顶部导航 - 与首页风格统一 -->
    <header class="relative pt-8 pb-20 header-bg-fade px-6 overflow-hidden">
      <!-- 装饰元素 -->
      <HeaderDecorations />

      <div class="relative flex items-start text-white">
        <button
          @click="goBack"
          class="w-10 h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center mr-4 hover:bg-white/30 transition-colors cursor-pointer active:scale-95 flex-shrink-0"
        >
          <IconChevronRight :size="20" class="rotate-180" />
        </button>
        <div class="flex-1">
          <p class="text-sm opacity-90 mb-1">时光倒流</p>
          <h1 class="text-2xl font-serif-title font-black mb-2">那年今日</h1>
          <p class="text-xs text-white/60 italic">—— {{ todayMonth }}月{{ todayDay }}日的回忆</p>
        </div>
      </div>
    </header>

    <!-- 记忆列表 - 时间轴布局 -->
    <main class="px-5 pb-8 pt-6">
      <!-- 加载状态 -->
      <div v-if="loading" class="text-center py-12">
        <div class="text-stone-400 text-sm">加载中...</div>
      </div>

      <div v-else-if="todayInHistoryMemories.length > 0" class="relative">
        <!-- 时间轴线 -->
        <div class="absolute left-[22px] top-0 bottom-0 w-0.5 bg-gradient-to-b from-[#FF8A80] via-[#FFAB91] to-[#FFE4E1]" />

        <!-- 时间轴卡片 -->
        <div class="space-y-6">
          <div
            v-for="(memory, index) in todayInHistoryMemories"
            :key="memory.id"
            class="relative flex gap-4"
          >
            <!-- 时间轴节点 -->
            <div class="relative flex-shrink-0 w-11 flex flex-col items-center">
              <!-- 年份圆点 -->
              <div
                class="w-11 h-11 rounded-full flex items-center justify-center text-[10px] font-bold shadow-md z-10 transition-transform hover:scale-110"
                :class="index === 0
                  ? 'bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] text-white'
                  : 'bg-white border-2 border-[#FFE4E1] text-[#FF8A80]'"
              >
                {{ getYearsAgo(memory.yearsAgo) }}
              </div>
            </div>

            <!-- 卡片内容 -->
            <div
              class="flex-1 min-w-0 cursor-pointer group"
              @click="goToDetail(memory.id)"
            >
              <!-- 有图片的卡片 - 图片突出展示 -->
              <div v-if="memory.images && memory.images.length > 0" class="bg-white rounded-2xl overflow-hidden shadow-md hover:shadow-xl transition-all duration-300 hover:-translate-y-0.5 border border-stone-100">
                <!-- 图片区域 -->
                <div class="relative aspect-[16/10] overflow-hidden">
                  <img
                    :src="memory.images[0]"
                    class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    alt=""
                  />
                  <!-- 渐变遮罩 -->
                  <div class="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent" />
                  <!-- 图片数量角标 -->
                  <div v-if="memory.images.length > 1" class="absolute top-3 right-3">
                    <span class="inline-flex items-center gap-1 px-2 py-1 bg-black/40 backdrop-blur-sm text-white text-[10px] rounded-full">
                      <IconImage :size="10" />
                      {{ memory.images.length }}
                    </span>
                  </div>
                  <!-- 底部标题 -->
                  <div class="absolute bottom-0 left-0 right-0 p-3.5">
                    <h3 class="font-bold text-white text-sm line-clamp-1">{{ memory.title }}</h3>
                  </div>
                </div>
                <div class="p-3.5 pt-2.5">
                  <p class="text-xs text-stone-500 line-clamp-2 mb-3 leading-relaxed break-words">{{ memory.description }}</p>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center text-[10px] text-stone-400">
                      <span class="font-medium text-stone-500">{{ memory.author }}</span>
                      <span v-if="memory.location" class="flex items-center ml-2">
                        <IconMapPin :size="10" class="mr-0.5" />{{ memory.location }}
                      </span>
                    </div>
                    <button
                      class="flex items-center text-[#FF8A80] text-xs space-x-1 hover:scale-110 transition-transform"
                      @click="handleLike(memory.id, $event)"
                    >
                      <IconHeart :size="14" :fill="true" />
                      <span>{{ memory.likes }}</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- 无图片的卡片 - 纯文字风格 -->
              <div v-else class="relative bg-white rounded-2xl p-4 shadow-md hover:shadow-xl transition-all duration-300 hover:-translate-y-0.5 border border-stone-100 overflow-hidden">
                <!-- 装饰背景 -->
                <div class="absolute top-2 right-2 opacity-5">
                  <IconDocument :size="48" class="text-[#FF8A80]" />
                </div>

                <h3 class="font-bold text-stone-700 text-sm mb-2 line-clamp-1 break-all">{{ memory.title }}</h3>
                <p class="text-xs text-stone-500 line-clamp-3 mb-3 leading-relaxed break-words">{{ memory.description }}</p>
                <div class="flex items-center justify-between">
                  <div class="flex items-center text-[10px] text-stone-400">
                    <span class="font-medium text-stone-500">{{ memory.author }}</span>
                    <span v-if="memory.location" class="flex items-center ml-2">
                      <IconMapPin :size="10" class="mr-0.5" />{{ memory.location }}
                    </span>
                  </div>
                  <button
                    class="flex items-center text-[#FF8A80] text-xs space-x-1 hover:scale-110 transition-transform"
                    @click="handleLike(memory.id, $event)"
                  >
                    <IconHeart :size="14" :fill="true" />
                    <span>{{ memory.likes }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 时间轴底部装饰 -->
        <div class="relative flex gap-4 mt-6">
          <div class="flex-shrink-0 w-11 flex justify-center">
            <div class="w-3 h-3 rounded-full bg-[#FFE4E1] border-2 border-white shadow-sm" />
          </div>
          <div class="flex-1 text-xs text-stone-400 pt-0.5">
            更多回忆等待记录...
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center h-[60vh] text-center">
        <div class="w-32 h-32 bg-[#FFF0EE] rounded-full flex items-center justify-center mb-6 shadow-inner">
          <span class="text-6xl">📅</span>
        </div>
        <h2 class="text-xl font-serif-title font-bold text-stone-700 mb-2">今日暂无历史记忆</h2>
        <p class="text-sm text-stone-500 leading-relaxed">
          快去记录今天的美好时刻吧！<br />
          明年的今天就能看到了
        </p>
      </div>
    </main>
  </div>
</template>

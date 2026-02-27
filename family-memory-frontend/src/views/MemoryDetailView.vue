<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { IconChevronRight, IconMapPin, IconMic, IconInbox } from '@/components/icons'
import HeaderDecorations from '@/components/HeaderDecorations.vue'
import ImageGrid from '@/components/ImageGrid.vue'
import * as api from '@/api'
import type { MemoryResp } from '@/api'

const route = useRoute()
const router = useRouter()

const memoryId = computed(() => Number(route.params.id))
const memory = ref<MemoryResp | null>(null)
const loading = ref(false)

// 从API获取记忆详情
onMounted(async () => {
  loading.value = true
  try {
    const res = await api.getMemoryDetail(memoryId.value)
    memory.value = res.data
  } catch (e) {
    console.error('获取记忆详情失败:', e)
  } finally {
    loading.value = false
  }
})

const goBack = () => {
  router.back()
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
          <p class="text-sm opacity-90 mb-1">时光深处</p>
          <h1 class="text-2xl font-serif-title font-black mb-2">记忆详情</h1>
          <p class="text-xs text-white/60 italic">—— 那些值得珍藏的瞬间</p>
        </div>
      </div>
    </header>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="px-5 pb-8 pt-4">
      <div class="glass-card rounded-[2rem] p-5 mb-5 animate-pulse">
        <div class="flex items-center space-x-4">
          <div class="w-14 h-14 rounded-2xl bg-stone-200"></div>
          <div class="flex-1">
            <div class="h-5 bg-stone-200 rounded w-24 mb-2"></div>
            <div class="h-3 bg-stone-100 rounded w-32"></div>
          </div>
        </div>
      </div>
      <div class="h-7 bg-stone-200 rounded w-3/4 mb-3 mx-1 animate-pulse"></div>
      <div class="h-4 bg-stone-100 rounded w-1/3 mb-5 mx-1 animate-pulse"></div>
      <div class="glass-card rounded-[2rem] p-6 mb-5 animate-pulse">
        <div class="space-y-3">
          <div class="h-4 bg-stone-200 rounded w-full"></div>
          <div class="h-4 bg-stone-200 rounded w-5/6"></div>
          <div class="h-4 bg-stone-200 rounded w-4/6"></div>
        </div>
      </div>
      <div class="aspect-video bg-stone-200 rounded-[2rem] mb-5 animate-pulse"></div>
    </div>

    <!-- 内容区域 -->
    <main class="px-5 pb-8 pt-4" v-else-if="memory">
      <!-- 作者信息卡片 -->
      <div class="glass-card rounded-[2rem] p-5 mb-5">
        <div class="flex items-center space-x-4">
          <div class="w-12 h-12 rounded-xl overflow-hidden shadow-sm">
            <img
              v-if="memory.avatar?.startsWith('http')"
              :src="memory.avatar"
              class="w-full h-full object-cover"
              alt=""
            />
            <div
              v-else
              class="w-full h-full bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] flex items-center justify-center text-xl text-white"
            >
              {{ memory.avatar || '👤' }}
            </div>
          </div>
          <div>
            <h3 class="font-bold text-stone-700">{{ memory.author }}</h3>
            <p class="text-xs text-stone-400">{{ memory.date }}</p>
          </div>
        </div>
      </div>

      <!-- 图片展示 - 移到标题前面 -->
      <div v-if="memory.images && memory.images.length === 1" class="rounded-[2rem] overflow-hidden mb-5 shadow-lg">
        <img
          :src="memory.images[0]"
          class="w-full max-h-[50vh] object-cover"
          alt=""
        />
      </div>
      <div v-else-if="memory.images && memory.images.length > 1" class="mb-5">
        <ImageGrid :images="memory.images" />
      </div>

      <!-- 标题 -->
      <h2 class="text-2xl font-serif-title font-bold text-stone-800 mb-3 px-1 break-all">{{ memory.title }}</h2>

      <!-- 位置信息 -->
      <div v-if="memory.location" class="flex items-center text-sm text-stone-500 mb-4 px-1">
        <IconMapPin :size="16" class="mr-2 text-[#FF8A80]" />
        <span>{{ memory.location }}</span>
      </div>

      <!-- 标签 - 移到描述前面 -->
      <div v-if="memory.tags && memory.tags.length > 0" class="flex flex-wrap gap-2 mb-4 px-1">
        <span
          v-for="tag in memory.tags"
          :key="tag"
          class="text-xs font-medium text-[#FF8A80] bg-[#FFF0EE] px-3 py-1.5 rounded-full"
        >
          #{{ tag }}
        </span>
      </div>

      <!-- 描述内容 -->
      <div class="glass-card rounded-[2rem] p-6 mb-5">
        <p class="text-stone-600 leading-relaxed text-[15px] whitespace-pre-wrap break-words">{{ memory.description }}</p>
      </div>

      <!-- 语音 -->
      <div v-if="memory.audioUrl" class="mb-5 glass-card rounded-[2rem] p-5">
        <div class="flex items-center gap-4 mb-3">
          <div class="w-12 h-12 bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] rounded-2xl flex items-center justify-center shadow-lg">
            <IconMic :size="22" class="text-white" />
          </div>
          <div>
            <span class="text-sm text-stone-700 font-bold">语音消息</span>
            <p class="text-xs text-stone-400">点击播放</p>
          </div>
        </div>
        <audio :src="memory.audioUrl" controls class="w-full h-10" />
      </div>
    </main>

    <!-- 记忆不存在 -->
    <div v-else class="flex flex-col items-center justify-center h-[60vh] text-center px-8">
      <div class="w-32 h-32 bg-[#FFF0EE] rounded-full flex items-center justify-center mb-6 shadow-inner">
        <IconInbox :size="56" class="text-[#FF8A80]" />
      </div>
      <h2 class="text-xl font-serif-title font-bold text-stone-700 mb-2">找不到这条记忆</h2>
      <p class="text-sm text-stone-500 mb-8">这条记忆可能已被删除或不存在</p>
      <button
        @click="goBack"
        class="px-8 py-4 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white rounded-full font-bold shadow-lg shadow-[#FF8A80]/30 cursor-pointer active:scale-95 transition-transform"
      >
        返回首页
      </button>
    </div>
  </div>
</template>

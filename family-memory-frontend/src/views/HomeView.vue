<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMemoryStore, useUIStore } from '@/stores'
import { IconChevronRight, IconAlarmClock, IconDocument, IconInbox, IconHeart, IconImage } from '@/components/icons'
import MemoryCard from '@/components/MemoryCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const router = useRouter()
const memoryStore = useMemoryStore()
const uiStore = useUIStore()

// 拍立得布局：首页最多展示3条
const displayMemories = computed(() => memoryStore.todayMemories.slice(0, 3))

// 根据记忆数量返回布局类名
const fragmentLayoutClass = computed(() => {
  const count = displayMemories.value.length
  if (count === 1) return 'fragments-single'
  if (count === 2) return 'fragments-double'
  return 'fragments-triple'
})

const goToTodayInHistory = () => {
  router.push({ name: 'today-in-history' })
}

const goToAllMemories = () => {
  router.push({ name: 'all-memories' })
}

const goToMemoryDetail = (id: number) => {
  router.push({ name: 'memory-detail', params: { id } })
}

const handleDeleteMemory = async (id: number) => {
  try {
    await memoryStore.deleteMemory(id)
    uiStore.toast.success('记忆已删除')
  } catch (e: any) {
    uiStore.toast.error(e.message || '删除失败')
  }
}
</script>

<template>
  <div>
    <!-- 那年今日 - 拍立得剪贴簿风格 -->
    <div v-if="displayMemories.length > 0" class="mb-8 mt-4">
      <div class="bg-gradient-to-br from-[#FFF5F3] to-[#FFF0EE] rounded-[1.5rem] p-4 relative overflow-hidden">
        <!-- 背景装饰 -->
        <div class="absolute top-2 right-2 w-16 h-16 text-[#FF8A80]/[0.08] pointer-events-none">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute bottom-3 left-6 w-8 h-8 text-[#FFAB91]/10 pointer-events-none">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>

        <!-- 模块标题 - 标签样式 -->
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <span class="inline-flex items-center gap-1.5 px-3 py-1 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white text-xs font-bold rounded-full shadow-sm">
              <IconAlarmClock :size="12" />
              那年今日
            </span>
          </div>
          <button
            @click="goToTodayInHistory"
            class="text-xs text-[#FF8A80] font-medium flex items-center gap-1 cursor-pointer hover:underline transition-colors"
          >
            查看全部
            <IconChevronRight :size="12" />
          </button>
        </div>

        <!-- 拍立得卡片布局 -->
        <div :class="['fragments-container', fragmentLayoutClass]">
          <!-- 单条记忆布局 -->
          <template v-if="displayMemories.length === 1">
            <div
              class="polaroid-card"
              @click="goToMemoryDetail(displayMemories[0].id)"
            >
              <!-- 装饰胶带 -->
              <div class="tape-decoration tape-top-left"></div>
              <div class="tape-decoration tape-top-right"></div>

              <!-- 照片区域 -->
              <div v-if="displayMemories[0].images?.length > 0" class="polaroid-photo">
                <img :src="displayMemories[0].images[0]" alt="" />
                <span class="years-ago-tag">
                  {{ displayMemories[0].yearsAgo === 0 ? '今日' : `${displayMemories[0].yearsAgo}年前` }}
                </span>
                <span v-if="displayMemories[0].images.length > 1" class="photo-count-tag">
                  <IconImage :size="10" />
                  {{ displayMemories[0].images.length }}
                </span>
              </div>
              <div v-else class="polaroid-photo polaroid-no-image" style="height: 140px;">
                <IconDocument :size="32" class="icon-placeholder" />
                <p>{{ displayMemories[0].description }}</p>
                <span class="years-ago-tag">
                  {{ displayMemories[0].yearsAgo === 0 ? '今日' : `${displayMemories[0].yearsAgo}年前` }}
                </span>
              </div>

              <!-- 底部文字 -->
              <div class="polaroid-caption">
                <h3>{{ displayMemories[0].title }}</h3>
                <div class="meta">{{ displayMemories[0].author }}</div>
              </div>
            </div>
          </template>

          <!-- 双条记忆布局 -->
          <template v-else-if="displayMemories.length === 2">
            <div
              v-for="(memory, index) in displayMemories"
              :key="memory.id"
              :class="['polaroid-card', `fragment-${index}`]"
              @click="goToMemoryDetail(memory.id)"
            >
              <!-- 装饰胶带 -->
              <div class="tape-decoration tape-top-left"></div>

              <!-- 照片区域 -->
              <div v-if="memory.images?.length > 0" class="polaroid-photo">
                <img :src="memory.images[0]" alt="" />
                <span class="years-ago-tag">
                  {{ memory.yearsAgo === 0 ? '今日' : `${memory.yearsAgo}年前` }}
                </span>
                <span v-if="memory.images.length > 1" class="photo-count-tag">
                  <IconImage :size="10" />
                  {{ memory.images.length }}
                </span>
              </div>
              <div v-else class="polaroid-photo polaroid-no-image" style="height: 110px;">
                <IconDocument :size="24" class="icon-placeholder" />
                <p>{{ memory.description }}</p>
                <span class="years-ago-tag">
                  {{ memory.yearsAgo === 0 ? '今日' : `${memory.yearsAgo}年前` }}
                </span>
              </div>

              <!-- 底部文字 -->
              <div class="polaroid-caption">
                <h3>{{ memory.title }}</h3>
                <div class="meta">{{ memory.author }}</div>
              </div>
            </div>
          </template>

          <!-- 三条记忆布局 -->
          <template v-else>
            <!-- 主卡片（左侧） -->
            <div
              class="polaroid-card fragment-main"
              @click="goToMemoryDetail(displayMemories[0].id)"
            >
              <!-- 装饰胶带 -->
              <div class="tape-decoration tape-top-left"></div>
              <div class="tape-decoration tape-top-right"></div>

              <!-- 照片区域 -->
              <div v-if="displayMemories[0].images?.length > 0" class="polaroid-photo">
                <img :src="displayMemories[0].images[0]" alt="" />
                <span class="years-ago-tag">
                  {{ displayMemories[0].yearsAgo === 0 ? '今日' : `${displayMemories[0].yearsAgo}年前` }}
                </span>
                <span v-if="displayMemories[0].images.length > 1" class="photo-count-tag">
                  <IconImage :size="10" />
                  {{ displayMemories[0].images.length }}
                </span>
              </div>
              <div v-else class="polaroid-photo polaroid-no-image" style="height: 130px;">
                <IconDocument :size="28" class="icon-placeholder" />
                <p>{{ displayMemories[0].description }}</p>
                <span class="years-ago-tag">
                  {{ displayMemories[0].yearsAgo === 0 ? '今日' : `${displayMemories[0].yearsAgo}年前` }}
                </span>
              </div>

              <!-- 底部文字 -->
              <div class="polaroid-caption">
                <h3>{{ displayMemories[0].title }}</h3>
                <div class="meta">{{ displayMemories[0].author }}</div>
              </div>
            </div>

            <!-- 次卡片堆叠（右侧） -->
            <div class="stacked-cards">
              <div
                v-for="(memory, index) in displayMemories.slice(1)"
                :key="memory.id"
                :class="['fragment-sub', `fragment-${index + 1}`]"
              >
                <div
                  class="polaroid-card"
                  @click="goToMemoryDetail(memory.id)"
                >
                  <!-- 装饰胶带 -->
                  <div class="tape-decoration tape-top-left" style="width: 35px; height: 14px;"></div>

                  <!-- 照片区域 -->
                  <div v-if="memory.images?.length > 0" class="polaroid-photo">
                    <img :src="memory.images[0]" alt="" />
                    <span class="years-ago-tag" style="font-size: 9px; padding: 3px 8px;">
                      {{ memory.yearsAgo === 0 ? '今日' : `${memory.yearsAgo}年前` }}
                    </span>
                  </div>
                  <div v-else class="polaroid-photo polaroid-no-image" style="height: 85px;">
                    <IconDocument :size="20" class="icon-placeholder" />
                    <p style="font-size: 10px; -webkit-line-clamp: 2;">{{ memory.description }}</p>
                    <span class="years-ago-tag" style="font-size: 9px; padding: 3px 8px;">
                      {{ memory.yearsAgo === 0 ? '今日' : `${memory.yearsAgo}年前` }}
                    </span>
                  </div>

                  <!-- 底部文字 -->
                  <div class="polaroid-caption">
                    <h3>{{ memory.title }}</h3>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- 时光印记 -->
    <div class="mb-4 mt-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-1.5">
          <!-- 时钟/胶片感图标 -->
          <div class="w-5 h-5 rounded-full border-2 border-[#FFAB91] flex items-center justify-center">
            <div class="w-1.5 h-1.5 bg-[#FF8A80] rounded-full"></div>
          </div>
          <h2 class="text-base font-bold text-[#8B6F60]">时光印记</h2>
          <span class="text-[10px] text-[#CCAB9A] italic ml-0.5">memories</span>
        </div>
        <button
          v-if="memoryStore.memories.length > 0"
          @click="goToAllMemories"
          class="text-xs text-[#CCAB9A] font-medium flex items-center gap-1 cursor-pointer hover:text-[#FF8A80] transition-colors"
        >
          查看全部
          <IconChevronRight :size="14" />
        </button>
      </div>
    </div>

    <!-- 记忆列表区域 -->
    <div class="space-y-5">
      <!-- 加载状态 -->
      <template v-if="memoryStore.loading">
        <SkeletonCard type="card" />
        <SkeletonCard type="card" />
      </template>

      <!-- 空状态 -->
      <div v-else-if="memoryStore.memories.length === 0" class="bg-white rounded-[2rem] p-8 text-center shadow-lg shadow-stone-200/50 border border-stone-100">
        <div class="w-16 h-16 mx-auto mb-3 rounded-full bg-[#FFF0EE] flex items-center justify-center">
          <IconInbox :size="32" class="text-[#FF8A80]" />
        </div>
        <div class="text-stone-500 text-sm">暂无记忆，快去创建第一条吧</div>
      </div>

      <!-- 记忆列表 -->
      <MemoryCard
        v-else
        v-for="memory in memoryStore.memories"
        :key="memory.id"
        :memory="memory"
        @click="goToMemoryDetail(memory.id)"
        @delete="handleDeleteMemory"
      />
    </div>
  </div>
</template>

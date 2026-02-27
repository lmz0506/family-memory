<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMemoryStore, useAppStore } from '@/stores'
import { IconChevronRight, IconMapPin, IconDocument, IconInbox } from '@/components/icons'
import SkeletonCard from '@/components/SkeletonCard.vue'

const router = useRouter()
const memoryStore = useMemoryStore()
const appStore = useAppStore()

// 筛选后的记忆（如果后端已经筛选过，这里直接用即可）
const filteredMemories = computed(() => {
  return memoryStore.memories.filter(m => m.year === appStore.selectedYear)
})

// 当选中年份变化时，重新获取数据
watch(() => appStore.selectedYear, async (year) => {
  if (memoryStore.currentFamilyId) {
    await memoryStore.fetchMemories(year)
  }
})

const goToDetail = (id: number) => {
  router.push({ name: 'memory-detail', params: { id } })
}
</script>

<template>
  <div class="py-4">
    <div class="flex justify-between items-end mb-6 px-2">
      <div>
        <h2 class="text-2xl font-serif-title font-bold text-stone-800">时光轨迹</h2>
        <p class="text-xs text-stone-400 mt-1">记录每一个闪光的日子</p>
      </div>
      <div
        @click="appStore.openYearPicker()"
        class="glass-card px-4 py-2 rounded-full text-xs font-medium text-stone-600 flex items-center cursor-pointer hover:shadow-md transition-all"
      >
        <span class="mr-1 font-bold text-[#FF8A80]">{{ appStore.selectedYear }}</span>
        <IconChevronRight :size="12" class="rotate-90 text-stone-400" />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="memoryStore.loading" class="border-l-2 border-dashed border-[#FF8A80]/30 ml-4 space-y-10 pb-10">
      <SkeletonCard type="timeline" />
      <SkeletonCard type="timeline" />
      <SkeletonCard type="timeline" />
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredMemories.length === 0" class="text-center py-12">
      <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-[#FFF0EE] flex items-center justify-center">
        <IconInbox :size="40" class="text-[#FF8A80]" />
      </div>
      <p class="text-stone-400 text-sm">{{ appStore.selectedYear }} 年暂无记忆</p>
    </div>

    <!-- 时间线 -->
    <div v-else class="border-l-2 border-dashed border-[#FF8A80]/30 ml-4 space-y-10 pb-10">
      <div v-for="memory in filteredMemories" :key="memory.id" class="relative pl-10">
        <!-- Dot -->
        <div
          class="absolute -left-[9px] top-1 w-4 h-4 rounded-full bg-white border-4 border-[#FF8A80] shadow-sm"
        />

        <div
          @click="goToDetail(memory.id)"
          class="glass-card rounded-3xl p-4 hover:shadow-lg transition-all cursor-pointer"
        >
          <span class="text-[10px] font-bold text-[#FF8A80] block mb-2">{{ memory.memoryTime }}</span>

          <div class="flex gap-4">
            <!-- 图片预览 -->
            <div v-if="memory.images && memory.images.length > 0" class="relative w-16 h-16 flex-shrink-0">
              <img :src="memory.images[0]" class="w-full h-full rounded-xl object-cover shadow-sm" alt="" />
              <div
                v-if="memory.images.length > 1"
                class="absolute bottom-0 right-0 bg-black/60 text-white text-[10px] px-1.5 py-0.5 rounded-tl-lg rounded-br-xl font-medium"
              >
                +{{ memory.images.length - 1 }}
              </div>
            </div>
            <div
              v-else
              class="w-16 h-16 rounded-xl bg-[#FFF0EE] flex items-center justify-center flex-shrink-0"
            >
              <IconDocument :size="24" class="text-[#FF8A80]" />
            </div>

            <!-- 文字内容 -->
            <div class="flex-1 min-w-0">
              <h3 class="font-bold text-stone-700 mb-1 line-clamp-1">{{ memory.title }}</h3>
              <p class="text-xs text-stone-500 line-clamp-2">{{ memory.description }}</p>
              <div class="mt-2 flex items-center text-[10px] text-stone-500">
                <span class="mr-3 font-medium">{{ memory.author }}</span>
                <span v-if="memory.location" class="flex items-center">
                  <IconMapPin :size="10" class="mr-1" /> {{ memory.location }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

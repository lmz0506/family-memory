<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore, useMemoryStore } from '@/stores'
import { IconX } from '@/components/icons'

const appStore = useAppStore()
const memoryStore = useMemoryStore()

// 使用store中的可用年份，如果为空则用当前年份
const availableYears = computed(() => {
  if (memoryStore.availableYears.length > 0) {
    return memoryStore.availableYears
  }
  // 回退：显示当前年份
  return [new Date().getFullYear().toString()]
})

const selectYear = async (year: string) => {
  appStore.setSelectedYear(year)
  appStore.closeYearPicker()
  // 选择年份后获取该年的记忆
  if (memoryStore.currentFamilyId) {
    await memoryStore.fetchMemories(year)
  }
}
</script>

<template>
  <Transition name="fade">
    <div
      v-if="appStore.showYearPicker"
      class="fixed inset-0 bg-black/50 z-[60]"
      @click="appStore.closeYearPicker()"
    />
  </Transition>

  <Transition name="slide-up">
    <div
      v-if="appStore.showYearPicker"
      class="fixed bottom-0 left-0 right-0 bg-white rounded-t-[2.5rem] shadow-2xl z-[61] max-h-[60vh]"
    >
      <!-- 拖拽指示器 -->
      <div class="w-10 h-1 bg-stone-300 rounded-full mx-auto mt-4 mb-2"></div>

      <!-- 标题栏 -->
      <div class="flex justify-between items-center px-6 py-4">
        <h2 class="text-lg font-serif-title font-bold text-stone-800">选择年份</h2>
        <button
          class="w-9 h-9 bg-stone-100 rounded-full flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors"
          @click="appStore.closeYearPicker()"
        >
          <IconX :size="18" />
        </button>
      </div>

      <!-- 年份列表 -->
      <div class="px-6 pb-10 overflow-y-auto max-h-[calc(60vh-100px)]">
        <div class="grid grid-cols-3 gap-3">
          <button
            v-for="year in availableYears"
            :key="year"
            class="py-5 rounded-2xl text-center font-bold transition-all"
            :class="[
              appStore.selectedYear === year
                ? 'bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white shadow-lg shadow-[#FF8A80]/30'
                : 'bg-stone-50 text-stone-600 hover:bg-stone-100 border border-stone-100'
            ]"
            @click="selectYear(year)"
          >
            {{ year }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}
</style>

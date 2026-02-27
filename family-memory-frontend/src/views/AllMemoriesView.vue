<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMemoryStore, useUIStore } from '@/stores'
import { IconChevronRight, IconSearch, IconX, IconCalendar, IconMapPin, IconUser, IconInbox } from '@/components/icons'
import MemoryCard from '@/components/MemoryCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'
import HeaderDecorations from '@/components/HeaderDecorations.vue'
import * as api from '@/api'
import type { MemoryResp, FamilyMemberResp } from '@/api'

const router = useRouter()
const memoryStore = useMemoryStore()
const uiStore = useUIStore()

// 数据
const memories = ref<MemoryResp[]>([])
const familyMembers = ref<FamilyMemberResp[]>([])
const loading = ref(false)
const loadingMore = ref(false)

// 分页
const page = ref(1)
const size = ref(10)
const total = ref(0)
const hasMore = computed(() => memories.value.length < total.value)

// 筛选条件
const showFilter = ref(false)
const filters = ref({
  keyword: '',
  authorId: '' as string | number,
  year: '',
  location: '',
  tag: ''
})

// 可用年份列表
const availableYears = computed(() => memoryStore.availableYears)

// 获取数据
const fetchMemories = async (append = false) => {
  if (!memoryStore.currentFamilyId) return

  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
    page.value = 1
  }

  try {
    const res = await api.getMemories(memoryStore.currentFamilyId, {
      page: page.value,
      size: size.value,
      year: filters.value.year || undefined,
      keyword: filters.value.keyword || undefined,
      authorId: filters.value.authorId ? Number(filters.value.authorId) : undefined,
      location: filters.value.location || undefined,
      tag: filters.value.tag || undefined
    })

    if (append) {
      memories.value.push(...res.data.list)
    } else {
      memories.value = res.data.list
    }
    total.value = res.data.total
  } catch (e) {
    console.error('获取记忆列表失败:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 获取家庭成员
const fetchMembers = async () => {
  if (!memoryStore.currentFamilyId) return
  try {
    const res = await api.getFamilyMembers(memoryStore.currentFamilyId)
    familyMembers.value = res.data
  } catch (e) {
    console.error('获取家庭成员失败:', e)
  }
}

// 加载更多
const loadMore = () => {
  if (loadingMore.value || !hasMore.value) return
  page.value++
  fetchMemories(true)
}

// 应用筛选
const applyFilter = () => {
  showFilter.value = false
  fetchMemories()
}

// 重置筛选
const resetFilter = () => {
  filters.value = {
    keyword: '',
    authorId: '',
    year: '',
    location: '',
    tag: ''
  }
  fetchMemories()
}

// 清除单个筛选条件
const clearFilter = (key: keyof typeof filters.value) => {
  filters.value[key] = ''
  fetchMemories()
}

// 是否有活跃的筛选条件
const hasActiveFilters = computed(() => {
  return Object.values(filters.value).some(v => v !== '')
})

// 获取活跃筛选条件列表
const activeFilters = computed(() => {
  const result: { key: keyof typeof filters.value; label: string }[] = []
  if (filters.value.keyword) result.push({ key: 'keyword', label: `关键字: ${filters.value.keyword}` })
  if (filters.value.authorId) {
    const member = familyMembers.value.find(m => m.userId === Number(filters.value.authorId))
    result.push({ key: 'authorId', label: `发布人: ${member?.nickname || '未知'}` })
  }
  if (filters.value.year) result.push({ key: 'year', label: `年份: ${filters.value.year}` })
  if (filters.value.location) result.push({ key: 'location', label: `地点: ${filters.value.location}` })
  if (filters.value.tag) result.push({ key: 'tag', label: `标签: ${filters.value.tag}` })
  return result
})

const goBack = () => {
  router.back()
}

const goToMemoryDetail = (id: number) => {
  router.push({ name: 'memory-detail', params: { id } })
}

const handleDeleteMemory = async (id: number) => {
  try {
    await api.deleteMemory(id)
    // 从本地列表中移除
    const index = memories.value.findIndex(m => m.id === id)
    if (index !== -1) {
      memories.value.splice(index, 1)
      total.value--
    }
    uiStore.toast.success('记忆已删除')
  } catch (e: any) {
    uiStore.toast.error(e.message || '删除失败')
  }
}

onMounted(() => {
  fetchMemories()
  fetchMembers()
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF9F8]">
    <!-- 顶部导航 -->
    <header class="relative pt-8 pb-20 header-bg-fade px-6 overflow-hidden">
      <!-- 装饰元素 -->
      <HeaderDecorations />

      <div class="relative flex items-center justify-between text-white mb-4">
        <div class="flex items-center">
          <button
            @click="goBack"
            class="w-10 h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center mr-3 hover:bg-white/30 transition-colors"
          >
            <IconChevronRight :size="20" class="rotate-180" />
          </button>
          <div>
            <h1 class="text-xl font-serif-title font-bold">全部动态</h1>
            <p class="text-xs opacity-80">共 {{ total }} 条记忆</p>
          </div>
        </div>
        <button
          @click="showFilter = !showFilter"
          class="px-4 py-2 rounded-full text-sm font-medium transition-all"
          :class="hasActiveFilters ? 'bg-white text-[#FF8A80]' : 'bg-white/20 text-white hover:bg-white/30'"
        >
          筛选
        </button>
      </div>

      <!-- 搜索框 -->
      <div class="relative">
        <input
          v-model="filters.keyword"
          type="text"
          placeholder="寻找那段温暖记忆..."
          class="w-full bg-white border-none rounded-2xl py-3.5 pl-11 pr-4 text-xs shadow-sm focus:ring-2 focus:ring-[#FF8A80]/20 outline-none"
          @keyup.enter="applyFilter"
        />
        <IconSearch class="absolute left-4 top-1/2 -translate-y-1/2 text-stone-300" :size="16" />
      </div>
    </header>

    <!-- 活跃的筛选条件标签 -->
    <div v-if="activeFilters.length > 0" class="px-5 py-3 flex flex-wrap gap-2">
      <span
        v-for="filter in activeFilters"
        :key="filter.key"
        class="inline-flex items-center px-3 py-1.5 bg-[#FFF0EE] text-[#FF8A80] text-xs rounded-full font-medium"
      >
        {{ filter.label }}
        <button @click="clearFilter(filter.key)" class="ml-1.5 hover:text-red-500">
          <IconX :size="12" />
        </button>
      </span>
      <button
        @click="resetFilter"
        class="text-xs text-stone-500 hover:text-[#FF8A80] underline"
      >
        清除全部
      </button>
    </div>

    <!-- 筛选面板 -->
    <Transition name="slide-down">
      <div
        v-if="showFilter"
        class="bg-white border-b border-stone-100 px-5 py-5 space-y-5"
      >
        <!-- 发布人 -->
        <div>
          <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">
            <IconUser :size="12" class="inline mr-1" />发布人
          </label>
          <select
            v-model="filters.authorId"
            class="w-full px-4 py-3 bg-[#FFF9F8] border-none rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-[#FF8A80]/20 shadow-inner"
          >
            <option value="">全部成员</option>
            <option v-for="member in familyMembers" :key="member.userId" :value="member.userId">
              {{ member.nickname }} ({{ member.role }})
            </option>
          </select>
        </div>

        <!-- 年份 -->
        <div>
          <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">
            <IconCalendar :size="12" class="inline mr-1" />年份
          </label>
          <select
            v-model="filters.year"
            class="w-full px-4 py-3 bg-[#FFF9F8] border-none rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-[#FF8A80]/20 shadow-inner"
          >
            <option value="">全部年份</option>
            <option v-for="year in availableYears" :key="year" :value="year">
              {{ year }}年
            </option>
          </select>
        </div>

        <!-- 地点 -->
        <div>
          <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">
            <IconMapPin :size="12" class="inline mr-1" />地点关键字
          </label>
          <input
            v-model="filters.location"
            type="text"
            placeholder="输入地点关键字..."
            class="w-full px-4 py-3 bg-[#FFF9F8] border-none rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-[#FF8A80]/20 shadow-inner"
          />
        </div>

        <!-- 标签 -->
        <div>
          <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">
            <span class="inline mr-1">#</span>标签
          </label>
          <input
            v-model="filters.tag"
            type="text"
            placeholder="输入标签..."
            class="w-full px-4 py-3 bg-[#FFF9F8] border-none rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-[#FF8A80]/20 shadow-inner"
          />
        </div>

        <!-- 按钮 -->
        <div class="flex gap-4 pt-2">
          <button
            @click="resetFilter"
            class="flex-1 py-3.5 bg-[#FFF9F8] text-stone-600 rounded-2xl text-sm font-bold hover:bg-stone-100 transition-colors"
          >
            重置
          </button>
          <button
            @click="applyFilter"
            class="flex-1 py-3.5 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white rounded-2xl text-sm font-bold shadow-lg shadow-[#FF8A80]/30 transition-all"
          >
            应用筛选
          </button>
        </div>
      </div>
    </Transition>

    <!-- 内容区域 -->
    <main class="px-5 pb-8 pt-4">
      <!-- 加载状态 -->
      <div v-if="loading" class="space-y-5">
        <SkeletonCard type="card" />
        <SkeletonCard type="card" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="memories.length === 0" class="flex flex-col items-center justify-center py-16">
        <div class="w-20 h-20 rounded-full bg-[#FFF0EE] flex items-center justify-center mb-4">
          <IconInbox :size="40" class="text-[#FF8A80]" />
        </div>
        <p class="text-stone-500 text-sm font-medium">
          {{ hasActiveFilters ? '没有找到符合条件的记忆' : '暂无记忆' }}
        </p>
        <button
          v-if="hasActiveFilters"
          @click="resetFilter"
          class="mt-6 px-6 py-3 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white rounded-full text-sm font-bold shadow-lg shadow-[#FF8A80]/30 cursor-pointer"
        >
          清除筛选条件
        </button>
      </div>

      <!-- 记忆列表 -->
      <template v-else>
        <div class="space-y-5">
          <MemoryCard
            v-for="memory in memories"
            :key="memory.id"
            :memory="memory as any"
            @click="goToMemoryDetail(memory.id)"
            @delete="handleDeleteMemory"
          />
        </div>

        <!-- 加载更多 -->
        <div class="text-center py-6">
          <button
            v-if="hasMore"
            @click="loadMore"
            :disabled="loadingMore"
            class="px-8 py-3 glass-card text-stone-600 rounded-full text-sm font-medium hover:shadow-md transition-all disabled:opacity-50 cursor-pointer"
          >
            {{ loadingMore ? '加载中...' : '加载更多' }}
          </button>
          <p v-else class="text-stone-400 text-xs font-bold uppercase tracking-widest">已经到底啦~</p>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>

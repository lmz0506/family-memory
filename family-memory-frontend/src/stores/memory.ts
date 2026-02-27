import { defineStore } from 'pinia'
import { ref} from 'vue'
import type { Memory, FamilyMember } from '@/types'
import * as api from '@/api'

export const useMemoryStore = defineStore('memory', () => {
  // State
  const memories = ref<Memory[]>([])
  const todayMemories = ref<api.MemoryResp[]>([])
  const todayCount = ref(0)
  const familyMembers = ref<FamilyMember[]>([])
  const loading = ref(false)
  const currentFamilyId = ref<number | null>(null)
  const availableYears = ref<string[]>([])
  const statistics = ref({
    recordDays: 0,
    photoCount: 0,
    memoryCount: 0,
    memberCount: 0,
    capsuleCount: 0
  })

  // 将API响应转换为Memory类型
  const convertToMemory = (resp: api.MemoryResp): Memory => ({
    id: resp.id,
    type: resp.type as Memory['type'],
    date: resp.date,
    year: resp.year,
    author: resp.author,
    authorId: resp.authorId,
    memoryTime: resp.memoryTime,
    avatar: resp.avatar,
    title: resp.title,
    description: resp.description,
    location: resp.location,
    images: resp.images,
    likes: resp.likes,
    tags: resp.tags
  })

  // 获取记忆列表
  const fetchMemories = async (year?: string) => {
    if (!currentFamilyId.value) return
    loading.value = true
    try {
      const res = await api.getMemories(currentFamilyId.value, { year, size: 10 })
      memories.value = res.data.list.map(convertToMemory)
    } catch (e) {
      console.error('获取记忆失败:', e)
    } finally {
      loading.value = false
    }
  }

  // 获取家庭成员
  const fetchFamilyMembers = async () => {
    if (!currentFamilyId.value) return
    try {
      const res = await api.getFamilyMembers(currentFamilyId.value)
      familyMembers.value = res.data.map(m => ({
        name: m.nickname,
        role: m.role,
        avatar: m.avatar,
        color: m.color
      }))
    } catch (e) {
      console.error('获取家庭成员失败:', e)
    }
  }

  // 获取统计数据
  const fetchStatistics = async () => {
    if (!currentFamilyId.value) return
    try {
      const res = await api.getFamilyStatistics(currentFamilyId.value)
      statistics.value = res.data
    } catch (e) {
      console.error('获取统计数据失败:', e)
    }
  }

  // 获取可用年份
  const fetchYears = async () => {
    if (!currentFamilyId.value) return
    try {
      const res = await api.getMemoryYears(currentFamilyId.value)
      availableYears.value = res.data
    } catch (e) {
      console.error('获取年份失败:', e)
    }
  }

  // 获取那年今日数据
  const fetchTodayMemories = async () => {
    if (!currentFamilyId.value) return
    try {
      const res = await api.getTodayInHistory(currentFamilyId.value)
      todayMemories.value = res.data
      todayCount.value = res.data.length
    } catch (e) {
      console.error('获取那年今日失败:', e)
    }
  }

  // 添加记忆
  const addMemory = async (memory: api.CreateMemoryParams) => {
    if (!currentFamilyId.value) return
    try {
      const res = await api.createMemory(currentFamilyId.value, memory)
      // 从服务器重新拉取数据确保一致性
      await Promise.all([fetchMemories(), fetchTodayMemories()])
      return res.data
    } catch (e) {
      console.error('创建记忆失败:', e)
      throw e
    }
  }

  // 点赞
  const likeMemory = async (id: number) => {
    try {
      const res = await api.likeMemory(id)
      const memory = memories.value.find(m => m.id === id)
      if (memory) {
        memory.likes = res.data.likes
      }
    } catch (e) {
      // 如果已点赞，尝试取消点赞
      try {
        const res = await api.unlikeMemory(id)
        const memory = memories.value.find(m => m.id === id)
        if (memory) {
          memory.likes = res.data.likes
        }
      } catch (e2) {
        console.error('点赞操作失败:', e2)
      }
    }
  }

  // 设置当前家庭ID
  const setCurrentFamilyId = (id: number) => {
    currentFamilyId.value = id
  }

  // 删除记忆
  const deleteMemory = async (id: number) => {
    try {
      await api.deleteMemory(id)
      // 从本地列表中移除
      const index = memories.value.findIndex(m => m.id === id)
      if (index !== -1) {
        memories.value.splice(index, 1)
      }
      // 同时刷新那年今日数据
      await fetchTodayMemories()
      return true
    } catch (e) {
      console.error('删除记忆失败:', e)
      throw e
    }
  }

  // 重置store状态
  const reset = () => {
    memories.value = []
    todayMemories.value = []
    todayCount.value = 0
    familyMembers.value = []
    loading.value = false
    currentFamilyId.value = null
    availableYears.value = []
    statistics.value = {
      recordDays: 0,
      photoCount: 0,
      memoryCount: 0,
      memberCount: 0,
      capsuleCount: 0
    }
  }

  // 初始化数据
  const initData = async () => {
    await Promise.all([
      fetchMemories(),
      fetchFamilyMembers(),
      fetchStatistics(),
      fetchYears(),
      fetchTodayMemories()
    ])
  }

  return {
    memories,
    todayMemories,
    todayCount,
    familyMembers,
    loading,
    currentFamilyId,
    availableYears,
    statistics,
    fetchMemories,
    fetchFamilyMembers,
    fetchStatistics,
    fetchYears,
    fetchTodayMemories,
    addMemory,
    likeMemory,
    deleteMemory,
    setCurrentFamilyId,
    reset,
    initData
  }
})

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useAppStore, useMemoryStore, useUIStore } from '@/stores'
import { IconClock, IconPlus, IconGift, IconSparkles, IconChevronRight } from '@/components/icons'
import * as api from '@/api'
import type { CapsuleResp } from '@/api'

const appStore = useAppStore()
const memoryStore = useMemoryStore()
const uiStore = useUIStore()

const pendingCount = ref(0)
const openedCount = ref(0)
const allCapsules = ref<CapsuleResp[]>([])
const openedCapsules = ref<CapsuleResp[]>([])
const loading = ref(true)

// 选中要查看的胶囊
const selectedCapsule = ref<CapsuleResp | null>(null)
const showCapsuleModal = ref(false)
const opening = ref(false)

// 显示已开启胶囊列表
const showOpenedList = ref(false)

// 计算今天可开启的胶囊
const todayOpenable = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return allCapsules.value.filter(c => c.openDate <= today && !c.isOpened)
})

// 获取胶囊数据
const fetchCapsules = async () => {
  if (!memoryStore.currentFamilyId) return
  loading.value = true
  try {
    // 获取待开启胶囊
    const pendingRes = await api.getCapsules(memoryStore.currentFamilyId, 'pending')
    pendingCount.value = pendingRes.data.pendingCount
    allCapsules.value = pendingRes.data.list || []

    // 获取已开启胶囊
    const openedRes = await api.getCapsules(memoryStore.currentFamilyId, 'opened')
    openedCapsules.value = openedRes.data.list || []
    openedCount.value = openedCapsules.value.length
  } catch (e) {
    console.error('获取胶囊数量失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchCapsules)

// 监听创建胶囊弹窗关闭，刷新数据
watch(() => appStore.showCreateCapsuleModal, (newVal, oldVal) => {
  if (oldVal && !newVal) {
    fetchCapsules()
  }
})

// 打开胶囊详情
const openCapsuleDetail = (capsule: CapsuleResp) => {
  selectedCapsule.value = capsule
  showCapsuleModal.value = true
}

// 开启胶囊
const openCapsule = async () => {
  if (!selectedCapsule.value) return
  opening.value = true
  try {
    const res = await api.getCapsuleDetail(selectedCapsule.value.id)
    selectedCapsule.value = res.data
    uiStore.toast.success('时光胶囊已开启!')

    // 更新本地数据
    const idx = allCapsules.value.findIndex(c => c.id === selectedCapsule.value?.id)
    if (idx > -1) {
      const openedItem = { ...allCapsules.value[idx], isOpened: true, content: res.data.content, openedAt: res.data.openedAt }
      allCapsules.value.splice(idx, 1)
      openedCapsules.value.unshift(openedItem)
      pendingCount.value = Math.max(0, pendingCount.value - 1)
      openedCount.value++
    }
  } catch (e: any) {
    uiStore.toast.error(e.message || '开启失败')
  } finally {
    opening.value = false
  }
}

// 关闭弹窗
const closeCapsuleModal = () => {
  showCapsuleModal.value = false
  selectedCapsule.value = null
}

// 打开已开启列表
const openOpenedList = () => {
  showOpenedList.value = true
}

// 关闭已开启列表
const closeOpenedList = () => {
  showOpenedList.value = false
}

// 格式化日期显示
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 今日可开启的胶囊区域 -->
    <div v-if="todayOpenable.length > 0" class="px-5 pt-6 pb-4 flex-shrink-0">
      <!-- 标题区域带闪烁动画 -->
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center">
          <div class="relative">
            <IconGift :size="24" class="text-[#FFD54F]" />
            <div class="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full animate-ping"></div>
            <div class="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full"></div>
          </div>
          <span class="ml-3 text-lg font-bold text-stone-700 font-serif-title">今日惊喜</span>
        </div>
        <span class="text-xs text-[#FFD54F] bg-[#FFFDE7] px-3 py-1 rounded-full font-medium">
          {{ todayOpenable.length }} 个胶囊等你开启
        </span>
      </div>

      <!-- 胶囊卡片列表 -->
      <div class="space-y-3">
        <div
          v-for="capsule in todayOpenable"
          :key="capsule.id"
          @click="openCapsuleDetail(capsule)"
          class="relative overflow-hidden bg-gradient-to-r from-[#FFD54F] via-[#FFE082] to-[#FFF8E1] rounded-2xl p-4 shadow-lg shadow-yellow-200/50 cursor-pointer active:scale-[0.98] transition-all group"
        >
          <!-- 装饰性光芒 -->
          <div class="absolute -top-10 -right-10 w-32 h-32 bg-white/30 rounded-full blur-2xl group-hover:scale-150 transition-transform duration-500"></div>
          <div class="absolute -bottom-8 -left-8 w-24 h-24 bg-yellow-300/30 rounded-full blur-xl"></div>

          <!-- 飘落的星星动画 -->
          <div class="absolute top-2 right-12 animate-bounce" style="animation-delay: 0.1s">
            <IconSparkles :size="12" class="text-white/60" />
          </div>
          <div class="absolute top-6 right-6 animate-bounce" style="animation-delay: 0.3s">
            <IconSparkles :size="10" class="text-white/40" />
          </div>
          <div class="absolute bottom-4 right-20 animate-bounce" style="animation-delay: 0.5s">
            <IconSparkles :size="8" class="text-white/50" />
          </div>

          <div class="relative flex items-center">
            <div class="w-14 h-14 bg-white/80 rounded-2xl flex items-center justify-center shadow-inner mr-4 flex-shrink-0">
              <div class="text-3xl animate-pulse">🎁</div>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-bold text-stone-800 truncate mb-1">{{ capsule.title }}</h3>
              <div class="flex items-center text-xs text-stone-600/80">
                <span>来自 {{ capsule.creatorName }}</span>
                <span class="mx-2">·</span>
                <span>封存于 {{ formatDate(capsule.createdAt) }}</span>
              </div>
            </div>
            <div class="flex-shrink-0 ml-3">
              <div class="w-10 h-10 bg-white rounded-full flex items-center justify-center shadow-md group-hover:shadow-lg group-hover:scale-110 transition-all">
                <IconChevronRight :size="20" class="text-[#FFD54F]" />
              </div>
            </div>
          </div>

          <div class="mt-3 pt-3 border-t border-white/30 text-center">
            <span class="text-xs font-medium text-stone-700/70">
              点击开启这份来自过去的礼物
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分隔线 -->
    <div v-if="todayOpenable.length > 0" class="mx-5 my-4 border-t border-dashed border-stone-200 flex-shrink-0"></div>

    <!-- 主内容区域 -->
    <div class="flex-1 flex flex-col items-center justify-center py-6 text-center pb-10">
      <!-- 时钟图标区域 - 点击查看已开启胶囊 -->
      <div
        class="w-36 h-36 bg-[#FFF0EE] rounded-full flex items-center justify-center mb-8 shadow-inner border-4 border-white relative cursor-pointer group"
        @click="openOpenedList"
      >
        <IconClock :size="56" class="text-[#FFD54F] group-hover:scale-110 transition-transform" />
        <!-- 待开启徽章 -->
        <div
          v-if="pendingCount > 0"
          class="absolute -bottom-2 -right-2 bg-[#FF8A80] text-white text-[10px] px-3 py-1 rounded-full border-2 border-white animate-bounce shadow-lg"
        >
          {{ pendingCount }}个待开启
        </div>
        <!-- 已开启徽章 -->
        <div
          v-if="openedCount > 0"
          class="absolute -bottom-2 -left-2 bg-emerald-500 text-white text-[10px] px-3 py-1 rounded-full border-2 border-white shadow-lg hover:bg-emerald-600 transition-colors"
        >
          {{ openedCount }}个已开启
        </div>
      </div>

      <h2 class="text-xl font-bold text-stone-700 mb-2 font-serif-title">私人档案馆</h2>
      <p class="text-sm text-stone-400 px-10 leading-relaxed mb-10">
        这里存放着属于家人的秘密空间，<br />
        给未来的自己，或者给长大的孩子写一封信。
      </p>
      <button
        @click="appStore.openCreateCapsuleModal()"
        class="px-10 py-4 bg-[#FFD54F] text-stone-700 font-bold rounded-full shadow-xl shadow-yellow-200 active:scale-95 transition-all flex items-center"
      >
        <IconPlus :size="18" class="mr-2" /> 创建新胶囊
      </button>
    </div>

    <!-- 胶囊详情弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showCapsuleModal"
          class="fixed inset-0 bg-black/60 z-[70] flex items-center justify-center p-6"
          @click="closeCapsuleModal"
        >
          <Transition name="scale">
            <div
              v-if="showCapsuleModal && selectedCapsule"
              class="w-full max-w-sm bg-white rounded-3xl overflow-hidden shadow-2xl"
              @click.stop
            >
              <!-- 顶部装饰 -->
              <div class="relative h-32 bg-gradient-to-br from-[#FFD54F] via-[#FFE082] to-[#FFF8E1] flex items-center justify-center overflow-hidden">
                <div class="absolute top-4 left-4 text-4xl animate-bounce" style="animation-delay: 0.1s">✨</div>
                <div class="absolute top-8 right-6 text-2xl animate-bounce" style="animation-delay: 0.3s">🌟</div>
                <div class="absolute bottom-4 left-8 text-2xl animate-bounce" style="animation-delay: 0.5s">💫</div>
                <div class="w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-xl">
                  <span class="text-5xl" :class="selectedCapsule.isOpened ? '' : 'animate-pulse'">
                    {{ selectedCapsule.isOpened ? '💌' : '🎁' }}
                  </span>
                </div>
              </div>

              <div class="p-6">
                <h3 class="text-xl font-bold text-stone-800 text-center mb-2 font-serif-title">
                  {{ selectedCapsule.title }}
                </h3>
                <p class="text-xs text-stone-400 text-center mb-4">
                  {{ selectedCapsule.creatorName }} · {{ formatDate(selectedCapsule.createdAt) }} 封存
                </p>

                <!-- 未开启状态 -->
                <div v-if="!selectedCapsule.isOpened" class="text-center">
                  <div class="bg-amber-50 rounded-2xl p-4 mb-4 border border-amber-100">
                    <p class="text-sm text-amber-700">
                      这是一份来自过去的礼物，<br/>
                      现在可以开启了！
                    </p>
                  </div>
                  <button
                    @click="openCapsule"
                    :disabled="opening"
                    class="w-full py-4 bg-gradient-to-r from-[#FFD54F] to-[#FFE082] text-stone-800 font-bold rounded-full shadow-lg shadow-yellow-200 active:scale-95 transition-all disabled:opacity-50"
                  >
                    {{ opening ? '正在开启...' : '开启时光胶囊' }}
                  </button>
                </div>

                <!-- 已开启状态 -->
                <div v-else>
                  <div class="bg-stone-50 rounded-2xl p-4 mb-4 max-h-60 overflow-y-auto">
                    <p class="text-stone-700 whitespace-pre-wrap leading-relaxed">
                      {{ selectedCapsule.content }}
                    </p>
                  </div>
                  <p class="text-xs text-stone-400 text-center">
                    开启于 {{ formatDate(selectedCapsule.openedAt || '') }}
                  </p>
                </div>
              </div>

              <button
                @click="closeCapsuleModal"
                class="w-full py-4 border-t border-stone-100 text-stone-500 font-medium hover:bg-stone-50 transition-colors"
              >
                关闭
              </button>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>

    <!-- 已开启胶囊列表 - 全屏 -->
    <Teleport to="body">
      <Transition name="slide-right">
        <div
          v-if="showOpenedList"
          class="fixed inset-0 bg-gradient-to-b from-[#FFFDE7] to-[#FFF8E1] z-[60] flex flex-col"
        >
          <!-- 头部导航栏 -->
          <div class="flex items-center justify-between px-5 py-4 bg-gradient-to-r from-[#FFD54F] to-[#FFE082] sticky top-0 z-10 shadow-md">
            <button
              @click="closeOpenedList"
              class="w-10 h-10 rounded-full flex items-center justify-center text-stone-700 bg-white/30 hover:bg-white/50 transition-colors"
            >
              <IconChevronRight :size="24" class="rotate-180" />
            </button>
            <div class="text-center">
              <h2 class="text-base font-bold text-stone-800 font-serif-title">时光信箱</h2>
              <p class="text-[10px] text-stone-600/80">{{ openedCount }} 封已开启的信</p>
            </div>
            <div class="w-10"></div>
          </div>

          <!-- 列表内容 -->
          <div class="flex-1 overflow-y-auto">
            <div v-if="openedCapsules.length === 0" class="py-20 text-center px-10">
              <div class="w-28 h-28 bg-white/60 rounded-full flex items-center justify-center mx-auto mb-5 shadow-inner border-4 border-white">
                <span class="text-5xl">📭</span>
              </div>
              <p class="text-stone-600 text-sm mb-2 font-medium">信箱空空如也</p>
              <p class="text-stone-400 text-xs leading-relaxed">开启的时光胶囊会变成信件<br/>保存在这里，随时可以重温</p>
            </div>

            <div v-else class="p-5 space-y-4">
              <!-- 信件卡片 - 与待开启胶囊同风格 -->
              <div
                v-for="capsule in openedCapsules"
                :key="capsule.id"
                @click="openCapsuleDetail(capsule)"
                class="relative overflow-hidden bg-gradient-to-r from-[#FFD54F] via-[#FFE082] to-[#FFF8E1] rounded-2xl p-4 shadow-lg shadow-yellow-200/50 cursor-pointer active:scale-[0.98] transition-all group"
              >
                <!-- 装饰性光芒 -->
                <div class="absolute -top-10 -right-10 w-32 h-32 bg-white/30 rounded-full blur-2xl group-hover:scale-150 transition-transform duration-500"></div>
                <div class="absolute -bottom-8 -left-8 w-24 h-24 bg-yellow-300/30 rounded-full blur-xl"></div>

                <!-- 飘落的星星动画 -->
                <div class="absolute top-2 right-12 animate-bounce" style="animation-delay: 0.1s">
                  <IconSparkles :size="12" class="text-white/60" />
                </div>
                <div class="absolute top-6 right-6 animate-bounce" style="animation-delay: 0.3s">
                  <IconSparkles :size="10" class="text-white/40" />
                </div>

                <div class="relative flex items-center">
                  <div class="w-14 h-14 bg-white/80 rounded-2xl flex items-center justify-center shadow-inner mr-4 flex-shrink-0">
                    <div class="text-3xl">💌</div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="font-bold text-stone-800 truncate mb-1">{{ capsule.title }}</h3>
                    <div class="flex items-center text-xs text-stone-600/80">
                      <span>来自 {{ capsule.creatorName }}</span>
                      <span class="mx-2">·</span>
                      <span>开启于 {{ formatDate(capsule.openedAt || '') }}</span>
                    </div>
                  </div>
                  <div class="flex-shrink-0 ml-3">
                    <div class="w-10 h-10 bg-white rounded-full flex items-center justify-center shadow-md group-hover:shadow-lg group-hover:scale-110 transition-all">
                      <IconChevronRight :size="20" class="text-[#FFD54F]" />
                    </div>
                  </div>
                </div>

                <div class="mt-3 pt-3 border-t border-white/30 text-center">
                  <span class="text-xs font-medium text-stone-700/70">
                    点击重温这份来自过去的礼物
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
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

.scale-enter-active,
.scale-leave-active {
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.scale-enter-from,
.scale-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-right-enter-from {
  transform: translateX(100%);
}
.slide-right-leave-to {
  transform: translateX(100%);
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

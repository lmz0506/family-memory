<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore, useMemoryStore, useUIStore } from '@/stores'
import { IconX } from '@/components/icons'
import DateTimePicker from '@/components/DateTimePicker.vue'
import * as api from '@/api'

const appStore = useAppStore()
const memoryStore = useMemoryStore()
const uiStore = useUIStore()

const title = ref('')
const content = ref('')
const openDate = ref('')
const submitting = ref(false)

// 获取最小日期（明天）
const minDate = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().slice(0, 10)
})

const handleSubmit = async () => {
  if (!title.value.trim() || !content.value.trim() || !openDate.value) {
    return
  }
  if (!memoryStore.currentFamilyId) {
    uiStore.toast.warning('请先加入一个家庭')
    return
  }

  submitting.value = true
  try {
    await api.createCapsule(memoryStore.currentFamilyId, {
      title: title.value,
      content: content.value,
      openDate: openDate.value
    })
    resetForm()
    appStore.closeCreateCapsuleModal()
    uiStore.toast.success('时光胶囊创建成功！')
  } catch (e: any) {
    uiStore.toast.error(e.message || '创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  title.value = ''
  content.value = ''
  openDate.value = ''
}

const handleClose = () => {
  resetForm()
  appStore.closeCreateCapsuleModal()
}
</script>

<template>
  <Transition name="fade">
    <div
      v-if="appStore.showCreateCapsuleModal"
      class="fixed inset-0 z-[60] flex items-center justify-center p-4 overflow-hidden"
      @click="handleClose"
    >
      <!-- 全屏渐变背景 -->
      <div
        class="absolute inset-0"
        style="background: linear-gradient(180deg, #FFD54F 0%, #FFCA28 20%, #FFB300 35%, #FFE082 55%, #FFF8E1 75%, #FFFDE7 100%);"
      />

      <!-- 装饰元素 -->
      <div class="absolute inset-0 pointer-events-none overflow-hidden">
        <!-- 上区 - 信封/星星装饰 -->
        <div class="absolute top-[5%] left-[12%] w-4 h-4 text-white/50 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[8%] right-[18%] w-3 h-3 text-white/40 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[3%] left-[55%] w-2.5 h-2.5 text-white/35 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[12%] right-[40%] w-3 h-3 text-white/45 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>

        <!-- 上区 - 爱心 -->
        <div class="absolute top-[6%] right-[25%] w-3 h-3 text-white/40 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[10%] left-[8%] w-2 h-2 text-white/30 animate-float-fast">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>

        <!-- 上区 - 气泡 -->
        <div class="absolute top-[7%] left-[35%] w-2 h-2 bg-white/35 rounded-full animate-bubble" />
        <div class="absolute top-[4%] right-[45%] w-1.5 h-1.5 bg-white/40 rounded-full animate-bubble-delay" />
        <div class="absolute top-[11%] right-[12%] w-2.5 h-2.5 bg-white/30 rounded-full animate-bubble-slow" />

        <!-- 中区左右侧 - 星星 -->
        <div class="absolute top-[30%] left-[4%] w-3 h-3 text-[#FFD54F]/25 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[45%] right-[5%] w-2.5 h-2.5 text-[#FFB300]/20 animate-twinkle-delay">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[55%] left-[6%] w-2 h-2 text-[#FFCA28]/20 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[38%] right-[3%] w-2 h-2 text-[#FFD54F]/15 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>

        <!-- 中区左右侧 - 爱心 -->
        <div class="absolute top-[40%] left-[3%] w-2.5 h-2.5 text-[#FFB300]/20 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[52%] right-[4%] w-2 h-2 text-[#FFCA28]/15 animate-float-fast">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>

        <!-- 中区 - 气泡 -->
        <div class="absolute top-[35%] left-[2%] w-1.5 h-1.5 bg-[#FFD54F]/15 rounded-full animate-bubble" />
        <div class="absolute top-[48%] right-[3%] w-2 h-2 bg-[#FFB300]/10 rounded-full animate-bubble-delay" />

        <!-- 下区 - 星星 -->
        <div class="absolute bottom-[18%] left-[10%] w-2.5 h-2.5 text-[#FFD54F]/20 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute bottom-[12%] right-[15%] w-2 h-2 text-[#FFB300]/15 animate-twinkle-delay">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute bottom-[8%] left-[40%] w-3 h-3 text-[#FFCA28]/15 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute bottom-[15%] right-[35%] w-2 h-2 text-[#FFD54F]/15 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>

        <!-- 下区 - 爱心 -->
        <div class="absolute bottom-[20%] right-[25%] w-2.5 h-2.5 text-[#FFB300]/15 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute bottom-[6%] left-[20%] w-2 h-2 text-[#FFCA28]/12 animate-float-fast">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>

        <!-- 下区 - 气泡 -->
        <div class="absolute bottom-[10%] left-[55%] w-1.5 h-1.5 bg-[#FFD54F]/15 rounded-full animate-bubble-delay" />
        <div class="absolute bottom-[4%] right-[30%] w-2 h-2 bg-[#FFB300]/10 rounded-full animate-bubble-slow" />

        <!-- 大装饰圆 -->
        <div class="absolute -top-16 -right-16 w-40 h-40 bg-white/10 rounded-full" />
        <div class="absolute top-[10%] -left-12 w-28 h-28 bg-white/[0.08] rounded-full" />
        <div class="absolute bottom-[15%] right-4 w-20 h-20 bg-[#FFD54F]/[0.06] rounded-full" />
        <div class="absolute bottom-[25%] -left-8 w-24 h-24 bg-[#FFB300]/[0.05] rounded-full" />
      </div>

      <!-- 模态框内容 -->
      <div
        class="relative z-10 w-full max-w-sm"
        @click.stop
      >
        <!-- 顶部图标区域 -->
        <div class="flex flex-col items-center text-white mb-6">
          <div class="w-16 h-16 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center mb-3 border border-white/30 shadow-lg">
            <span class="text-3xl">💌</span>
          </div>
          <h1 class="text-2xl font-serif-title font-black drop-shadow-sm">创建时光胶囊</h1>
          <p class="text-xs text-white/80 mt-1 tracking-wider">写给未来的一封信</p>
        </div>

        <!-- 表单卡片 -->
        <div class="bg-white/90 backdrop-blur-sm rounded-3xl p-5 shadow-xl max-h-[55vh] overflow-y-auto no-scrollbar">
          <!-- 关闭按钮 -->
          <button
            class="absolute top-2 right-2 w-8 h-8 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center text-white hover:bg-white/30 transition-colors"
            @click="handleClose"
          >
            <IconX :size="16" />
          </button>

          <!-- 表单内容 -->
          <div class="space-y-4">
            <!-- 标题输入 -->
            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-2">胶囊标题</label>
              <input
                v-model="title"
                type="text"
                placeholder="给这个胶囊起个名字..."
                class="w-full p-3.5 bg-[#FFFDE7] rounded-2xl text-stone-700 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-[#FFD54F]/50 transition-shadow border border-amber-100"
              />
            </div>

            <!-- 内容输入 -->
            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-2">写给未来的话</label>
              <textarea
                v-model="content"
                placeholder="在这里写下你想对未来说的话..."
                class="w-full h-28 p-3.5 bg-[#FFFDE7] rounded-2xl text-stone-700 placeholder:text-stone-400 resize-none focus:outline-none focus:ring-2 focus:ring-[#FFD54F]/50 transition-shadow border border-amber-100"
              />
            </div>

            <!-- 日期选择 -->
            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-2">开启日期</label>
              <DateTimePicker
                v-model="openDate"
                mode="date"
                theme="amber"
                label="开启日期"
                placeholder="选择开启日期"
                :min-date="minDate"
              />
            </div>

            <!-- 提示信息 -->
            <div class="bg-amber-50 rounded-2xl p-3 border border-amber-100">
              <p class="text-xs text-amber-600 text-center leading-relaxed">
                时光胶囊将在指定日期自动开启<br />在此之前，内容将被封存，无法查看
              </p>
            </div>
          </div>
        </div>

        <!-- 提交按钮 -->
        <button
          class="w-full py-4 bg-white text-[#FFB300] font-bold rounded-full mt-5 shadow-xl active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!title.trim() || !content.trim() || !openDate || submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '创建中...' : '封存胶囊' }}
        </button>

        <!-- 底部提示 -->
        <p class="text-center text-xs text-stone-500 mt-4">点击任意空白处关闭</p>
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
</style>

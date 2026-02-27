<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAppStore, useMemoryStore } from '@/stores'
import { IconX } from '@/components/icons'
import * as api from '@/api'

const appStore = useAppStore()
const memoryStore = useMemoryStore()

const inviteCode = ref('')
const expireAt = ref('')
const inviteLink = computed(() => `https://family-memory.app/join/${inviteCode.value}`)
const copied = ref(false)
const loading = ref(false)

// 获取邀请码
const fetchInviteCode = async () => {
  if (!memoryStore.currentFamilyId) return
  loading.value = true
  try {
    const res = await api.createInviteCode(memoryStore.currentFamilyId, { expireHours: 24, maxUses: 10 })
    inviteCode.value = res.data.code
    expireAt.value = res.data.expireAt
  } catch (e) {
    console.error('获取邀请码失败:', e)
  } finally {
    loading.value = false
  }
}

// 当模态框打开时获取邀请码
watch(() => appStore.showInviteFamilyModal, (show) => {
  if (show && !inviteCode.value) {
    fetchInviteCode()
  }
})

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}

const refreshCode = async () => {
  await fetchInviteCode()
}
</script>

<template>
  <Transition name="slide-up">
    <div
      v-if="appStore.showInviteFamilyModal"
      class="fixed inset-0 bg-black/50 z-[60] flex items-end"
      @click="appStore.closeInviteFamilyModal()"
    >
      <div
        class="bg-white w-full rounded-t-[2.5rem] p-6 pb-10 shadow-2xl relative"
        @click.stop
      >
        <!-- 拖拽指示器 -->
        <div class="w-10 h-1 bg-stone-300 rounded-full mx-auto mb-5"></div>

        <!-- 标题栏 -->
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-lg font-serif-title font-bold text-stone-800">邀请家人</h2>
          <button
            class="w-9 h-9 bg-stone-100 rounded-full flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors"
            @click="appStore.closeInviteFamilyModal()"
          >
            <IconX :size="18" />
          </button>
        </div>

        <!-- 邀请码展示 -->
        <div class="text-center mb-6">
          <p class="text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">分享邀请码给家人</p>
          <div class="bg-stone-50 rounded-2xl p-6 mb-3 border border-stone-100">
            <span v-if="loading" class="text-xl text-stone-400">加载中...</span>
            <span v-else class="text-4xl font-mono font-bold text-[#FF8A80] tracking-[0.3em]">
              {{ inviteCode || '------' }}
            </span>
          </div>
          <button
            @click="refreshCode"
            :disabled="loading"
            class="text-sm text-[#FF8A80] hover:text-[#FFAB91] transition-colors disabled:opacity-50 font-medium"
          >
            {{ loading ? '生成中...' : '刷新邀请码' }}
          </button>
        </div>

        <!-- 二维码区域 -->
        <div class="flex flex-col items-center mb-6">
          <div class="w-32 h-32 bg-stone-50 rounded-2xl flex items-center justify-center mb-3 border border-stone-100">
            <div class="text-center">
              <div class="text-4xl mb-1">📱</div>
              <div class="text-[10px] text-stone-400">扫码功能</div>
              <div class="text-[10px] text-stone-400">开发中...</div>
            </div>
          </div>
          <p class="text-xs text-stone-400">扫描二维码加入家庭</p>
        </div>

        <!-- 分享链接 -->
        <div class="mb-5">
          <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-2">或复制邀请链接</label>
          <div class="flex gap-2">
            <input
              :value="inviteLink"
              readonly
              class="flex-1 p-4 bg-stone-50 rounded-2xl text-sm text-stone-600 focus:outline-none border border-stone-100"
            />
            <button
              @click="copyLink"
              :disabled="!inviteCode"
              class="px-5 py-4 rounded-2xl text-sm font-bold transition-all disabled:opacity-50 shadow-lg"
              :class="[
                copied
                  ? 'bg-green-500 text-white shadow-green-200'
                  : 'bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white shadow-[#FF8A80]/30'
              ]"
            >
              {{ copied ? '已复制' : '复制' }}
            </button>
          </div>
        </div>

        <!-- 提示信息 -->
        <div class="bg-amber-50 rounded-2xl p-4 border border-amber-100">
          <p class="text-sm text-amber-700 text-center">邀请码24小时内有效。<br />家人加入后可以一起记录和查看家庭记忆。</p>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(100%);
}
</style>

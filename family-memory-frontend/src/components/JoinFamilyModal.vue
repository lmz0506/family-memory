<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useAuthStore, useMemoryStore } from '@/stores'
import { IconX, IconPlus, IconUser, IconHeart } from '@/components/icons'
import * as api from '@/api'

const authStore = useAuthStore()
const memoryStore = useMemoryStore()

// 当前模式
const mode = ref<'choose' | 'create' | 'join'>('choose')

// 创建家庭表单
const createForm = ref({
  name: '',
  description: ''
})

// 加入家庭表单
const joinForm = ref({
  inviteCode: '',
  nickname: '',
  role: ''
})

// 状态
const loading = ref(false)
const errorMsg = ref('')

// 是否显示模态框（当用户已登录但没有家庭时）
const showModal = computed(() => {
  return authStore.isLoggedIn && !authStore.currentFamilyId
})

// 重置表单
const resetForm = () => {
  mode.value = 'choose'
  createForm.value = { name: '', description: '' }
  joinForm.value = { inviteCode: '', nickname: authStore.user?.nickname || '', role: '' }
  errorMsg.value = ''
}

// 初始化加入表单中的昵称
watch(showModal, (show) => {
  if (show) {
    joinForm.value.nickname = authStore.user?.nickname || ''
  }
})

// 创建家庭
const handleCreate = async () => {
  if (!createForm.value.name.trim()) {
    errorMsg.value = '请输入家庭名称'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    await api.createFamily({
      name: createForm.value.name,
      description: createForm.value.description || undefined
    })

    // 重新获取用户信息以获取新的家庭ID
    const user = await authStore.fetchCurrentUser()
    if (user?.currentFamily) {
      memoryStore.setCurrentFamilyId(user.currentFamily.id)
      await memoryStore.initData()
    }

    resetForm()
  } catch (e: any) {
    errorMsg.value = e.message || '创建失败，请重试'
  } finally {
    loading.value = false
  }
}

// 加入家庭
const handleJoin = async () => {
  if (!joinForm.value.inviteCode.trim()) {
    errorMsg.value = '请输入邀请码'
    return
  }
  if (!joinForm.value.nickname.trim()) {
    errorMsg.value = '请输入昵称'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    await api.joinFamily({
      inviteCode: joinForm.value.inviteCode,
      nickname: joinForm.value.nickname,
      role: joinForm.value.role || undefined
    })

    // 重新获取用户信息以获取新的家庭ID
    const user = await authStore.fetchCurrentUser()
    if (user?.currentFamily) {
      memoryStore.setCurrentFamilyId(user.currentFamily.id)
      await memoryStore.initData()
    }

    resetForm()
  } catch (e: any) {
    errorMsg.value = e.message || '加入失败，请检查邀请码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Transition name="fade">
    <div
      v-if="showModal"
      class="fixed inset-0 bg-black/50 z-[70] flex items-center justify-center p-4"
    >
      <div
        class="bg-white w-full max-w-sm rounded-[2rem] shadow-2xl p-8"
        @click.stop
      >
        <!-- 选择模式 -->
        <template v-if="mode === 'choose'">
          <div class="text-center mb-8">
            <div class="w-20 h-20 bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] rounded-full flex items-center justify-center mx-auto mb-5 shadow-lg shadow-[#FF8A80]/30">
              <IconHeart :size="36" class="text-white" />
            </div>
            <h2 class="text-2xl font-serif-title font-bold text-stone-800 mb-2">欢迎来到时光·家</h2>
            <p class="text-sm text-stone-500">创建或加入一个家庭开始记录</p>
          </div>

          <div class="space-y-4">
            <button
              @click="mode = 'create'"
              class="w-full py-5 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white font-bold rounded-full shadow-lg shadow-[#FF8A80]/30 active:scale-95 transition-all flex items-center justify-center space-x-2"
            >
              <IconPlus :size="20" />
              <span>创建新家庭</span>
            </button>

            <button
              @click="mode = 'join'"
              class="w-full py-5 bg-[#FFF9F8] hover:bg-[#FFF0EE] text-stone-700 font-bold rounded-full active:scale-95 transition-all flex items-center justify-center space-x-2"
            >
              <IconUser :size="20" />
              <span>加入现有家庭</span>
            </button>
          </div>
        </template>

        <!-- 创建家庭 -->
        <template v-else-if="mode === 'create'">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-serif-title font-bold text-stone-800">创建家庭</h2>
            <button
              @click="resetForm"
              class="w-10 h-10 bg-stone-100 rounded-full flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors"
            >
              <IconX :size="18" />
            </button>
          </div>

          <div class="space-y-5">
            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">家庭名称</label>
              <input
                v-model="createForm.name"
                type="text"
                placeholder="例如：幸福之家"
                class="w-full px-5 py-4 rounded-2xl bg-stone-50 text-stone-800 placeholder:text-stone-400 focus:ring-2 focus:ring-[#FF8A80]/20 outline-none transition-all border border-stone-100"
              />
            </div>

            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">家庭简介（可选）</label>
              <textarea
                v-model="createForm.description"
                placeholder="描述一下你的家庭..."
                rows="3"
                class="w-full px-5 py-4 rounded-2xl bg-stone-50 text-stone-800 placeholder:text-stone-400 resize-none focus:ring-2 focus:ring-[#FF8A80]/20 outline-none transition-all border border-stone-100"
              />
            </div>

            <p v-if="errorMsg" class="text-red-500 text-sm text-center">{{ errorMsg }}</p>

            <button
              @click="handleCreate"
              :disabled="loading"
              class="w-full py-5 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white font-bold rounded-full shadow-lg shadow-[#FF8A80]/30 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ loading ? '创建中...' : '创建家庭' }}
            </button>
          </div>
        </template>

        <!-- 加入家庭 -->
        <template v-else-if="mode === 'join'">
          <div class="flex justify-between items-center mb-8">
            <h2 class="text-xl font-serif-title font-bold text-stone-800">加入家庭</h2>
            <button
              @click="resetForm"
              class="w-10 h-10 bg-stone-100 rounded-full flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors"
            >
              <IconX :size="18" />
            </button>
          </div>

          <div class="space-y-5">
            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">邀请码</label>
              <input
                v-model="joinForm.inviteCode"
                type="text"
                placeholder="输入6位邀请码"
                class="w-full px-5 py-5 rounded-2xl bg-stone-50 text-stone-800 placeholder:text-stone-400 focus:ring-2 focus:ring-[#FF8A80]/20 outline-none transition-all border border-stone-100 text-center text-xl tracking-[0.3em] font-mono"
              />
            </div>

            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">你的昵称</label>
              <input
                v-model="joinForm.nickname"
                type="text"
                placeholder="在家庭中显示的名字"
                class="w-full px-5 py-4 rounded-2xl bg-stone-50 text-stone-800 placeholder:text-stone-400 focus:ring-2 focus:ring-[#FF8A80]/20 outline-none transition-all border border-stone-100"
              />
            </div>

            <div>
              <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">家庭角色（可选）</label>
              <input
                v-model="joinForm.role"
                type="text"
                placeholder="例如：爸爸、妈妈、女儿..."
                class="w-full px-5 py-4 rounded-2xl bg-stone-50 text-stone-800 placeholder:text-stone-400 focus:ring-2 focus:ring-[#FF8A80]/20 outline-none transition-all border border-stone-100"
              />
            </div>

            <p v-if="errorMsg" class="text-red-500 text-sm text-center">{{ errorMsg }}</p>

            <button
              @click="handleJoin"
              :disabled="loading"
              class="w-full py-5 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white font-bold rounded-full shadow-lg shadow-[#FF8A80]/30 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ loading ? '加入中...' : '加入家庭' }}
            </button>
          </div>
        </template>
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

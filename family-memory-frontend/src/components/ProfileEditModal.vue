<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores'
import { IconX, IconImage } from '@/components/icons'
import * as api from '@/api'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const authStore = useAuthStore()

// 表单数据
const form = ref({
  nickname: '',
  avatar: ''
})

// 头像上传相关
const avatarInput = ref<HTMLInputElement | null>(null)
const avatarPreview = ref('')
const uploading = ref(false)

const loading = ref(false)
const errorMsg = ref('')

// 当模态框打开时，初始化表单
watch(() => props.show, (show) => {
  if (show && authStore.user) {
    form.value.nickname = authStore.user.nickname || ''
    form.value.avatar = authStore.user.avatar || ''
    avatarPreview.value = authStore.user.avatar || ''
    errorMsg.value = ''
  }
})

// 打开头像选择器
const openAvatarPicker = () => {
  avatarInput.value?.click()
}

// 处理头像选择
const handleAvatarSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 预览
  avatarPreview.value = URL.createObjectURL(file)

  // 上传
  uploading.value = true
  try {
    const res = await api.uploadFile(file, 'avatar')
    form.value.avatar = res.data.url // 使用相对路径传递给后端
    avatarPreview.value = res.data.fullUrl // 前端预览使用完整URL
  } catch (e: any) {
    errorMsg.value = '头像上传失败，请重试'
    avatarPreview.value = authStore.user?.avatar || ''
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const handleSubmit = async () => {
  if (!form.value.nickname.trim()) {
    errorMsg.value = '请输入昵称'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    await authStore.updateProfile({
      nickname: form.value.nickname,
      avatar: form.value.avatar
    })
    emit('close')
  } catch (e: any) {
    errorMsg.value = e.message || '保存失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Transition name="fade">
    <div
      v-if="show"
      class="fixed inset-0 bg-black/50 z-[70] flex items-center justify-center p-4"
      @click="emit('close')"
    >
      <div
        class="bg-white w-full max-w-sm rounded-[2rem] shadow-2xl p-8"
        @click.stop
      >
        <!-- 标题栏 -->
        <div class="flex justify-between items-center mb-8">
          <h2 class="text-xl font-serif-title font-bold text-stone-800">编辑个人资料</h2>
          <button
            class="w-10 h-10 bg-stone-100 rounded-full flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors"
            @click="emit('close')"
          >
            <IconX :size="18" />
          </button>
        </div>

        <div class="space-y-6">
          <!-- 头像上传 -->
          <div>
            <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-4">头像</label>
            <div class="flex flex-col items-center">
              <div
                @click="openAvatarPicker"
                class="relative w-28 h-28 rounded-full overflow-hidden cursor-pointer group shadow-lg"
              >
                <!-- 头像预览 -->
                <img
                  v-if="avatarPreview && (avatarPreview.startsWith('http') || avatarPreview.startsWith('blob:'))"
                  :src="avatarPreview"
                  class="w-full h-full object-cover"
                  alt="头像"
                />
                <div
                  v-else-if="avatarPreview"
                  class="w-full h-full bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] flex items-center justify-center text-5xl text-white"
                >
                  {{ avatarPreview }}
                </div>
                <div
                  v-else
                  class="w-full h-full bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] flex items-center justify-center"
                >
                  <IconImage :size="36" class="text-white" />
                </div>

                <!-- 上传遮罩 -->
                <div class="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <div v-if="uploading" class="text-white text-xs">上传中...</div>
                  <IconImage v-else :size="24" class="text-white" />
                </div>
              </div>

              <input
                ref="avatarInput"
                type="file"
                accept="image/*"
                @change="handleAvatarSelect"
                class="hidden"
              />

              <p class="text-xs text-stone-400 mt-3">点击更换头像</p>
            </div>
          </div>

          <!-- 昵称 -->
          <div>
            <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">昵称</label>
            <input
              v-model="form.nickname"
              type="text"
              placeholder="请输入昵称"
              class="w-full px-5 py-4 rounded-2xl border-none bg-[#FFF9F8] text-stone-800 placeholder:text-stone-400 focus:ring-2 focus:ring-[#FF8A80]/20 outline-none transition-all"
            />
          </div>

          <!-- 用户名（只读） -->
          <div>
            <label class="block text-xs font-bold text-stone-400 uppercase tracking-widest mb-3">用户名</label>
            <div class="px-5 py-4 rounded-2xl bg-stone-100 text-stone-500 text-sm">
              {{ authStore.user?.username }}
            </div>
          </div>

          <!-- 错误提示 -->
          <p v-if="errorMsg" class="text-red-500 text-sm text-center">{{ errorMsg }}</p>

          <!-- 保存按钮 -->
          <button
            @click="handleSubmit"
            :disabled="loading || uploading"
            class="w-full py-4 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white font-bold rounded-full shadow-lg shadow-[#FF8A80]/30 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? '保存中...' : '保存修改' }}
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
</style>

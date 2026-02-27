<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useAppStore, useMemoryStore, useUIStore } from '@/stores'
import { IconX, IconImage, IconMic, IconMapPin } from '@/components/icons'
import DateTimePicker from '@/components/DateTimePicker.vue'
import * as api from '@/api'

const appStore = useAppStore()
const memoryStore = useMemoryStore()
const uiStore = useUIStore()

// 表单数据
const title = ref('')
const content = ref('')
const location = ref('')
const tags = ref('')

// 文件选择相关
const fileInput = ref<HTMLInputElement | null>(null)
interface SelectedFile {
  file: File
  preview: string
  type: 'image' | 'video'
}
const selectedFiles = ref<SelectedFile[]>([])
const uploadedUrls = ref<string[]>([])

// 日期时间选择
// 获取当前时间，格式化为 datetime-local 需要的格式
const getNowDatetime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}
const selectedDate = ref(getNowDatetime())

// 语音录制相关
const isRecording = ref(false)
const recordingDuration = ref(0)
const audioBlob = ref<Blob | null>(null)
const audioUrl = ref<string | null>(null)
const uploadedAudioUrl = ref<string | null>(null)
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let recordingTimer: number | null = null

// 提交状态
const submitting = ref(false)

const formattedDuration = computed(() => {
  const mins = Math.floor(recordingDuration.value / 60)
  const secs = recordingDuration.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

// 打开文件选择器
const openFilePicker = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files) return

  Array.from(files).forEach(file => {
    const type = file.type.startsWith('image') ? 'image' : 'video'
    const preview = URL.createObjectURL(file)
    selectedFiles.value.push({ file, preview, type })
  })

  // 重置 input 以允许选择相同文件
  input.value = ''
}

// 移除已选文件
const removeFile = (index: number) => {
  const file = selectedFiles.value[index]
  URL.revokeObjectURL(file.preview)
  selectedFiles.value.splice(index, 1)
}

// 开始录音
const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) {
        audioChunks.push(e.data)
      }
    }

    mediaRecorder.onstop = () => {
      audioBlob.value = new Blob(audioChunks, { type: 'audio/webm' })
      audioUrl.value = URL.createObjectURL(audioBlob.value)
      stream.getTracks().forEach(track => track.stop())
    }

    mediaRecorder.start()
    isRecording.value = true
    recordingDuration.value = 0

    recordingTimer = window.setInterval(() => {
      recordingDuration.value++
    }, 1000)
  } catch (err) {
    console.error('无法访问麦克风:', err)
    uiStore.toast.error('无法访问麦克风，请检查权限设置')
  }
}

// 停止录音
const stopRecording = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
}

// 删除录音
const removeRecording = () => {
  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
  }
  audioBlob.value = null
  audioUrl.value = null
  uploadedAudioUrl.value = null
  recordingDuration.value = 0
}

// 切换录音状态
const toggleRecording = () => {
  if (isRecording.value) {
    stopRecording()
  } else if (audioUrl.value) {
    removeRecording()
  } else {
    startRecording()
  }
}

// 上传文件
const uploadFiles = async () => {
  if (selectedFiles.value.length === 0) return []

  const files = selectedFiles.value.map(f => f.file)
  const res = await api.uploadFiles(files, 'memory')
  return res.data.map(f => f.url)
}

// 上传音频
const uploadAudio = async () => {
  if (!audioBlob.value) return null

  const audioFile = new File([audioBlob.value], 'voice.webm', { type: 'audio/webm' })
  const res = await api.uploadFile(audioFile, 'audio')
  return res.data.url
}

// 提交表单
const handleSubmit = async () => {
  if (!content.value.trim()) {
    uiStore.toast.warning('请输入记忆内容')
    return
  }
  if (!memoryStore.currentFamilyId) {
    uiStore.toast.warning('请先加入一个家庭')
    return
  }

  submitting.value = true
  try {
    // 上传文件
    const mediaUrls = await uploadFiles()
    const audioFileUrl = await uploadAudio()

    // 解析标签
    const tagList = tags.value
      .split(/[,，、\s]+/)
      .map(t => t.trim())
      .filter(t => t.length > 0)

    // 创建记忆
    await memoryStore.addMemory({
      title: title.value.trim() || content.value.slice(0, 20),
      description: content.value,
      type: selectedFiles.value.some(f => f.type === 'video') ? 'video' :
            selectedFiles.value.length > 0 ? 'photo' : 'note',
      memoryDate: selectedDate.value,
      location: location.value || undefined,
      tags: tagList.length > 0 ? tagList : undefined,
      mediaUrls: mediaUrls.length > 0 ? mediaUrls : undefined,
      audioUrl: audioFileUrl || undefined,
      audioDuration: audioFileUrl ? recordingDuration.value : undefined
    })

    resetForm()
    appStore.closeAddModal()
    uiStore.toast.success('记忆创建成功！')
  } catch (e: any) {
    uiStore.toast.error(e.message || '创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  title.value = ''
  content.value = ''
  location.value = ''
  tags.value = ''
  selectedFiles.value.forEach(f => URL.revokeObjectURL(f.preview))
  selectedFiles.value = []
  uploadedUrls.value = []
  selectedDate.value = getNowDatetime()
  removeRecording()
}

// 关闭模态框
const handleClose = () => {
  if (isRecording.value) {
    stopRecording()
  }
  resetForm()
  appStore.closeAddModal()
}

// 组件卸载时清理
onUnmounted(() => {
  if (isRecording.value) {
    stopRecording()
  }
  selectedFiles.value.forEach(f => URL.revokeObjectURL(f.preview))
  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
  }
})
</script>

<template>
  <Transition name="slide-up">
    <div
      v-if="appStore.showAddModal"
      class="fixed inset-0 bg-black/50 z-[60] flex items-end"
      @click="handleClose"
    >
      <div
        class="bg-white w-full rounded-t-[2.5rem] p-6 pb-10 shadow-2xl relative max-h-[90vh] flex flex-col"
        @click.stop
      >
        <!-- 拖拽指示器 -->
        <div class="w-10 h-1 bg-stone-300 rounded-full mx-auto mb-5"></div>

        <!-- 标题栏 -->
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-lg font-serif-title font-bold text-stone-800">记下这份感动</h2>
          <button
            class="w-9 h-9 bg-stone-100 rounded-full flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors"
            @click="handleClose"
          >
            <IconX :size="18" />
          </button>
        </div>

        <!-- 表单内容 -->
        <div class="flex-1 space-y-4 overflow-y-auto no-scrollbar">
          <!-- 标题 -->
          <input
            v-model="title"
            type="text"
            placeholder="给这条记忆起个标题（可选）"
            class="w-full p-4 bg-stone-50 rounded-2xl text-stone-700 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-[#FF8A80]/20 transition-shadow border border-stone-100"
          />

          <!-- 内容 -->
          <textarea
            v-model="content"
            placeholder="此时此刻，你的心情是怎样的？"
            class="w-full h-32 p-4 bg-stone-50 rounded-2xl text-stone-700 placeholder:text-stone-400 resize-none focus:outline-none focus:ring-2 focus:ring-[#FF8A80]/20 transition-shadow border border-stone-100"
          />

          <!-- 已选文件预览 -->
          <div v-if="selectedFiles.length > 0" class="grid grid-cols-3 gap-2">
            <div
              v-for="(file, index) in selectedFiles"
              :key="index"
              class="relative aspect-square rounded-xl overflow-hidden bg-stone-100"
            >
              <img
                v-if="file.type === 'image'"
                :src="file.preview"
                class="w-full h-full object-cover"
                alt=""
              />
              <video
                v-else
                :src="file.preview"
                class="w-full h-full object-cover"
              />
              <button
                @click="removeFile(index)"
                class="absolute top-1.5 right-1.5 w-5 h-5 bg-black/50 rounded-full flex items-center justify-center text-white hover:bg-black/70 transition-colors"
              >
                <IconX :size="12" />
              </button>
              <div
                v-if="file.type === 'video'"
                class="absolute bottom-1.5 left-1.5 px-1.5 py-0.5 bg-black/50 rounded text-white text-[9px]"
              >
                视频
              </div>
            </div>
          </div>

          <!-- 已录制语音预览 -->
          <div v-if="audioUrl" class="bg-stone-50 rounded-2xl p-4 border border-stone-100">
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-3">
                <div class="w-10 h-10 bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] rounded-xl flex items-center justify-center">
                  <IconMic :size="18" class="text-white" />
                </div>
                <div>
                  <p class="text-sm font-medium text-stone-700">语音消息</p>
                  <p class="text-xs text-stone-400">{{ formattedDuration }}</p>
                </div>
              </div>
              <div class="flex items-center space-x-2">
                <audio :src="audioUrl" controls class="h-8 w-28" />
                <button
                  @click="removeRecording"
                  class="p-1.5 text-stone-400 hover:text-red-500 transition-colors"
                >
                  <IconX :size="14" />
                </button>
              </div>
            </div>
          </div>

          <div class="flex gap-3">
            <!-- 文件选择 -->
            <input
              ref="fileInput"
              type="file"
              accept="image/*,video/*"
              multiple
              @change="handleFileSelect"
              class="hidden"
            />
            <label
              @click="openFilePicker"
              class="flex-1 h-24 rounded-2xl border-2 border-dashed border-stone-200 bg-stone-50 flex flex-col items-center justify-center text-stone-400 cursor-pointer hover:border-[#FF8A80] hover:text-[#FF8A80] hover:bg-[#FFF9F8] transition-colors"
            >
              <IconImage :size="26" class="mb-1.5" />
              <span class="text-[10px] font-medium">加入照片</span>
              <span v-if="selectedFiles.length > 0" class="text-[9px] text-[#FF8A80] mt-0.5">
                已选 {{ selectedFiles.length }} 个
              </span>
            </label>

            <!-- 语音录制 -->
            <button
              @click="toggleRecording"
              class="flex-1 h-24 rounded-2xl border-2 border-dashed flex flex-col items-center justify-center transition-colors"
              :class="[
                isRecording
                  ? 'bg-red-50 border-red-300 text-red-500'
                  : audioUrl
                    ? 'bg-[#FFF9F8] border-[#FF8A80] text-[#FF8A80]'
                    : 'bg-stone-50 border-stone-200 text-stone-400 hover:border-[#FF8A80] hover:text-[#FF8A80] hover:bg-[#FFF9F8]'
              ]"
            >
              <div
                v-if="isRecording"
                class="w-6 h-6 mb-1.5 rounded-full bg-red-500 animate-pulse"
              />
              <IconMic v-else :size="26" class="mb-1.5" />
              <span class="text-[10px] font-medium">
                {{ isRecording ? '点击停止' : audioUrl ? '重新录制' : '录一段话' }}
              </span>
              <span v-if="isRecording" class="text-[9px] mt-0.5 font-mono">
                {{ formattedDuration }}
              </span>
            </button>
          </div>

          <div class="space-y-2.5">
            <!-- 位置 -->
            <div class="flex items-center p-3.5 bg-stone-50 rounded-xl border border-stone-100">
              <IconMapPin :size="18" class="mr-3 text-[#FF8A80] flex-shrink-0" />
              <input
                v-model="location"
                type="text"
                placeholder="在哪里发生的？"
                class="flex-1 bg-transparent text-sm text-stone-600 focus:outline-none placeholder:text-stone-400"
              />
            </div>

            <!-- 标签 -->
            <div class="flex items-center p-3.5 bg-stone-50 rounded-xl border border-stone-100">
              <span class="mr-3 text-[#FF8A80] text-base font-bold">#</span>
              <input
                v-model="tags"
                type="text"
                placeholder="添加标签（用逗号分隔）"
                class="flex-1 bg-transparent text-sm text-stone-600 focus:outline-none placeholder:text-stone-400"
              />
            </div>

            <!-- 日期时间选择 -->
            <DateTimePicker v-model="selectedDate" placeholder="选择记忆时间" />
          </div>
        </div>

        <!-- 提交按钮 -->
        <button
          class="w-full py-4 bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] text-white font-bold rounded-full mt-5 shadow-lg shadow-[#FF8A80]/30 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!content.trim() || submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '保存中...' : '封存进时光博物馆' }}
        </button>
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

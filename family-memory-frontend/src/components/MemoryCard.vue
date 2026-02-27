<script setup lang="ts">
import {ref} from 'vue'
import type {Memory} from '@/types'
import {useAuthStore, useUIStore} from '@/stores'
import {IconMoreHorizontal} from '@/components/icons'
import ImageGrid from '@/components/ImageGrid.vue'

const props = defineProps<{
  memory: Memory
}>()

const emit = defineEmits<{
  click: []
  edit: [id: number]
  delete: [id: number]
}>()

const authStore = useAuthStore()
const uiStore = useUIStore()

// 菜单状态
const showMenu = ref(false)

// 是否是作者本人
const isAuthor = (memory: Memory) => {
  const userId = authStore.user?.id ?? null
  const authorId = memory.authorId ?? null
  return userId === authorId
}

const handleCardClick = () => {
  emit('click')
}

const toggleMenu = (event: Event) => {
  event.stopPropagation()
  showMenu.value = !showMenu.value
}

const closeMenu = () => {
  showMenu.value = false
}

const handleEdit = (event: Event) => {
  event.stopPropagation()
  showMenu.value = false
  emit('edit', props.memory.id)
}

const handleDelete = async (event: Event) => {
  event.stopPropagation()
  showMenu.value = false

  const confirmed = await uiStore.showConfirm({
    title: '删除记忆',
    message: '确定要删除这条记忆吗？删除后无法恢复。',
    confirmText: '删除',
    cancelText: '取消',
    type: 'danger'
  })

  if (confirmed) {
    emit('delete', props.memory.id)
  }
}

const handleShare = (event: Event) => {
  event.stopPropagation()
  showMenu.value = false
  uiStore.toast.info('分享功能开发中...')
}
</script>

<template>
  <div
      @click="handleCardClick"
      class="glass-card rounded-[2rem] overflow-hidden group hover:shadow-xl transition-all duration-300 border border-white/40 cursor-pointer"
  >
    <div class="p-5">
      <!-- 头部: 作者信息 -->
      <div class="flex justify-between items-center mb-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full overflow-hidden shadow-sm">
            <img
                v-if="memory.avatar?.startsWith('http')"
                :src="memory.avatar"
                class="w-full h-full object-cover"
                alt=""
            />
            <div
                v-else
                class="w-full h-full bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] flex items-center justify-center text-lg text-white"
            >
              {{ memory.avatar || '👤' }}
            </div>
          </div>
          <div>
            <h4 class="text-sm font-bold text-stone-700">{{ memory.author }}</h4>
            <p class="text-[10px] text-stone-400">{{ memory.memoryTime }}<span
                v-if="memory.location"> · {{ memory.location }}</span></p>
          </div>
        </div>

        <!-- 更多菜单 -->
        <div class="relative">
          <button
              @click="toggleMenu"
              class="p-2 text-stone-400 hover:text-[#FF8A80] hover:bg-[#FFF0EE] rounded-xl transition-colors cursor-pointer"
          >
            <IconMoreHorizontal :size="20"/>
          </button>
          <!-- 下拉菜单 -->
          <Transition name="fade">
            <div
                v-if="showMenu"
                class="absolute right-0 top-10 w-32 glass-card rounded-2xl shadow-lg py-2 z-20"
            >
              <button
                  v-if="isAuthor(memory)"
                  @click="handleEdit"
                  class="w-full px-4 py-2.5 text-left text-sm text-stone-600 hover:bg-[#FFF0EE] hover:text-[#FF8A80] transition-colors"
              >
                编辑
              </button>
              <button
                  @click="handleShare"
                  class="w-full px-4 py-2.5 text-left text-sm text-stone-600 hover:bg-[#FFF0EE] hover:text-[#FF8A80] transition-colors"
              >
                分享
              </button>
              <button
                  v-if="isAuthor(memory)"
                  @click="handleDelete"
                  class="w-full px-4 py-2.5 text-left text-sm text-red-500 hover:bg-red-50 transition-colors"
              >
                删除
              </button>
            </div>
          </Transition>
          <!-- 点击外部关闭 -->
          <div
              v-if="showMenu"
              class="fixed inset-0 z-10"
              @click.stop="closeMenu"
          />
        </div>
      </div>

      <!-- 图片区域 -->
      <div v-if="memory.images.length === 1" class="mb-4 rounded-2xl overflow-hidden shadow-sm aspect-video">
        <img
            :src="memory.images[0]"
            class="w-full h-full object-cover transform group-hover:scale-105 transition-transform duration-500"
        />
      </div>
      <ImageGrid v-else-if="memory.images.length > 1" :images="memory.images" class="mb-4"/>

      <!-- 内容区域 -->
      <h3 class="text-base font-serif-title font-bold text-stone-800 mb-2 break-all">{{ memory.title }}</h3>
      <p class="text-[13px] text-stone-500 leading-relaxed line-clamp-3 break-words">
        {{ memory.description }}
      </p>

      <!-- 底部：标签 -->
      <div v-if="memory.tags && memory.tags.length > 0"
           class="mt-4 pt-4 border-t border-stone-100 flex gap-2 flex-wrap">
        <span
            v-for="tag in memory.tags"
            :key="tag"
            class="text-xs font-medium text-[#FF8A80] bg-[#FFF0EE] px-3 py-1.5 rounded-full"
        >
          #{{ tag }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

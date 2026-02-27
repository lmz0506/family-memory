<script setup lang="ts">
import { useUIStore } from '@/stores/ui'
import { IconX } from '@/components/icons'

const uiStore = useUIStore()

const getTypeStyles = (type: string) => {
  switch (type) {
    case 'success':
      return 'bg-green-50 border-green-200 text-green-700'
    case 'error':
      return 'bg-red-50 border-red-200 text-red-700'
    case 'warning':
      return 'bg-amber-50 border-amber-200 text-amber-700'
    default:
      return 'bg-[#FFF0EE] border-[#FF8A80]/30 text-stone-700'
  }
}

const getIconBg = (type: string) => {
  switch (type) {
    case 'success':
      return 'bg-green-100 text-green-600'
    case 'error':
      return 'bg-red-100 text-red-600'
    case 'warning':
      return 'bg-amber-100 text-amber-600'
    default:
      return 'bg-[#FF8A80]/20 text-[#FF8A80]'
  }
}

const getIcon = (type: string) => {
  switch (type) {
    case 'success':
      return '✓'
    case 'error':
      return '!'
    case 'warning':
      return '!'
    default:
      return 'i'
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-3 w-[90%] max-w-[360px] pointer-events-none">
      <TransitionGroup name="toast">
        <div
          v-for="toast in uiStore.toasts"
          :key="toast.id"
          :class="[
            'pointer-events-auto rounded-2xl border px-4 py-3 shadow-lg backdrop-blur-sm',
            'flex items-center gap-3 transition-all duration-300',
            getTypeStyles(toast.type || 'info')
          ]"
        >
          <div
            :class="[
              'w-7 h-7 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0',
              getIconBg(toast.type || 'info')
            ]"
          >
            {{ getIcon(toast.type || 'info') }}
          </div>
          <p class="flex-1 text-sm font-medium">{{ toast.message }}</p>
          <button
            @click="uiStore.removeToast(toast.id)"
            class="w-6 h-6 rounded-full flex items-center justify-center hover:bg-black/5 transition-colors flex-shrink-0"
          >
            <IconX :size="14" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-leave-active {
  transition: all 0.2s ease-out;
}

.toast-enter-from {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}

.toast-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

.toast-move {
  transition: transform 0.3s ease;
}
</style>

<script setup lang="ts">
import { useUIStore } from '@/stores/ui'
import { IconX } from '@/components/icons'

const uiStore = useUIStore()

const getTypeColor = (type: string) => {
  switch (type) {
    case 'danger':
      return {
        icon: 'bg-red-100 text-red-500',
        button: 'bg-red-500 hover:bg-red-600 active:bg-red-700'
      }
    case 'warning':
      return {
        icon: 'bg-amber-100 text-amber-500',
        button: 'bg-amber-500 hover:bg-amber-600 active:bg-amber-700'
      }
    default:
      return {
        icon: 'bg-[#FFF0EE] text-[#FF8A80]',
        button: 'bg-gradient-to-r from-[#FF8A80] to-[#FFAB91] hover:shadow-lg'
      }
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="uiStore.confirmVisible"
        class="fixed inset-0 bg-black/40 backdrop-blur-sm z-[110] flex items-center justify-center p-6"
        @click.self="uiStore.handleConfirm(false)"
      >
        <Transition name="scale" appear>
          <div
            v-if="uiStore.confirmVisible"
            class="bg-white rounded-[2rem] w-full max-w-[320px] overflow-hidden shadow-2xl"
          >
            <!-- Header -->
            <div class="relative px-6 pt-6 pb-4">
              <button
                @click="uiStore.handleConfirm(false)"
                class="absolute right-4 top-4 w-8 h-8 rounded-full bg-stone-100 flex items-center justify-center text-stone-400 hover:bg-stone-200 hover:text-stone-600 transition-colors"
              >
                <IconX :size="16" />
              </button>

              <!-- Icon -->
              <div class="flex justify-center mb-4">
                <div
                  :class="[
                    'w-16 h-16 rounded-full flex items-center justify-center',
                    getTypeColor(uiStore.confirmOptions.type || 'info').icon
                  ]"
                >
                  <span v-if="uiStore.confirmOptions.type === 'danger'" class="text-2xl font-bold">!</span>
                  <span v-else-if="uiStore.confirmOptions.type === 'warning'" class="text-2xl font-bold">?</span>
                  <span v-else class="text-2xl font-bold">i</span>
                </div>
              </div>

              <!-- Title -->
              <h3 class="text-lg font-bold text-stone-800 text-center mb-2">
                {{ uiStore.confirmOptions.title }}
              </h3>

              <!-- Message -->
              <p class="text-sm text-stone-500 text-center leading-relaxed">
                {{ uiStore.confirmOptions.message }}
              </p>
            </div>

            <!-- Actions -->
            <div class="px-6 pb-6 flex gap-3">
              <button
                @click="uiStore.handleConfirm(false)"
                class="flex-1 py-3 rounded-xl bg-stone-100 text-stone-600 font-medium text-sm hover:bg-stone-200 active:scale-95 transition-all"
              >
                {{ uiStore.confirmOptions.cancelText }}
              </button>
              <button
                @click="uiStore.handleConfirm(true)"
                :class="[
                  'flex-1 py-3 rounded-xl text-white font-medium text-sm active:scale-95 transition-all',
                  getTypeColor(uiStore.confirmOptions.type || 'info').button
                ]"
              >
                {{ uiStore.confirmOptions.confirmText }}
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
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

.scale-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.scale-leave-active {
  transition: all 0.15s ease-out;
}

.scale-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>

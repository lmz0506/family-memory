import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'

// Toast 类型
export interface ToastOptions {
  message: string
  type?: 'success' | 'error' | 'warning' | 'info'
  duration?: number
}

interface ToastItem extends ToastOptions {
  id: number
}

// Confirm 类型
export interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'danger' | 'warning' | 'info'
}

export const useUIStore = defineStore('ui', () => {
  // Toast 状态
  const toasts = ref<ToastItem[]>([])
  let toastId = 0

  // Confirm 状态
  const confirmVisible = ref(false)
  const confirmOptions = ref<ConfirmOptions>({
    message: ''
  })
  const confirmResolve = shallowRef<((value: boolean) => void) | null>(null)

  // 显示 Toast
  const showToast = (options: ToastOptions | string) => {
    const opts: ToastOptions = typeof options === 'string'
      ? { message: options }
      : options

    const id = ++toastId
    const toast: ToastItem = {
      id,
      message: opts.message,
      type: opts.type || 'info',
      duration: opts.duration ?? 3000
    }

    toasts.value.push(toast)

    if (toast.duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, toast.duration)
    }

    return id
  }

  // 快捷方法
  const toast = {
    success: (message: string, duration?: number) =>
      showToast({ message, type: 'success', duration }),
    error: (message: string, duration?: number) =>
      showToast({ message, type: 'error', duration }),
    warning: (message: string, duration?: number) =>
      showToast({ message, type: 'warning', duration }),
    info: (message: string, duration?: number) =>
      showToast({ message, type: 'info', duration })
  }

  // 移除 Toast
  const removeToast = (id: number) => {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index > -1) {
      toasts.value.splice(index, 1)
    }
  }

  // 显示确认框
  const showConfirm = (options: ConfirmOptions | string): Promise<boolean> => {
    const opts: ConfirmOptions = typeof options === 'string'
      ? { message: options }
      : options

    confirmOptions.value = {
      title: opts.title || '提示',
      message: opts.message,
      confirmText: opts.confirmText || '确定',
      cancelText: opts.cancelText || '取消',
      type: opts.type || 'info'
    }
    confirmVisible.value = true

    return new Promise((resolve) => {
      confirmResolve.value = resolve
    })
  }

  // 确认框响应
  const handleConfirm = (result: boolean) => {
    confirmVisible.value = false
    if (confirmResolve.value) {
      confirmResolve.value(result)
      confirmResolve.value = null
    }
  }

  return {
    // Toast
    toasts,
    showToast,
    toast,
    removeToast,
    // Confirm
    confirmVisible,
    confirmOptions,
    showConfirm,
    handleConfirm
  }
})

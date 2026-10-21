import {ref} from 'vue'
export type Toast = {id: number; message: string; kind: 'success' | 'error' | 'info'; remaining: number}
export const toasts = ref<Toast[]>([])
let sequence = 0
export function notify(message: string, kind: Toast['kind'] = 'success') {
  toasts.value = [{id: ++sequence, message, kind, remaining: 4500}, ...toasts.value].slice(0, 4)
}
export function dismissToast(id: number) {toasts.value = toasts.value.filter(item => item.id !== id)}

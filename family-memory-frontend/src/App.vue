<script setup lang="ts">
import LockScreen from '@/views/LockScreen.vue'
import Toast from '@/components/Toast.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useAuthStore } from '@/stores'

const authStore = useAuthStore()
</script>

<template>
  <div class="flex justify-center min-h-screen">
    <div
      class="bg-[#FFF9F8] min-h-screen w-full max-w-md shadow-2xl overflow-hidden relative flex flex-col"
    >
      <!-- 登录界面 -->
      <LockScreen />

      <!-- 主应用内容 -->
      <template v-if="authStore.isLoggedIn">
        <router-view />
      </template>
    </div>
  </div>

  <!-- 全局 Toast 通知 -->
  <Toast />

  <!-- 全局确认对话框 -->
  <ConfirmDialog />
</template>

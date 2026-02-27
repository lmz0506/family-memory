<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAppStore, useAuthStore, useMemoryStore } from '@/stores'
import { IconHeart, IconUser } from '@/components/icons'

const appStore = useAppStore()
const authStore = useAuthStore()
const memoryStore = useMemoryStore()

// 当前模式：login 或 register
const mode = ref<'login' | 'register'>('login')

// 表单数据
const loginForm = ref({
  username: '',
  password: ''
})

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

// 状态
const loading = ref(false)
const errorMsg = ref('')

// 是否显示登录界面
const showLogin = computed(() => !authStore.isLoggedIn)

// 切换模式
const switchMode = (newMode: 'login' | 'register') => {
  mode.value = newMode
  errorMsg.value = ''
}

// 登录
const handleLogin = async () => {
  if (!loginForm.value.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!loginForm.value.password) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const result = await authStore.login(loginForm.value.username, loginForm.value.password)

    // 登录成功，初始化数据
    if (result.user.currentFamily) {
      memoryStore.setCurrentFamilyId(result.user.currentFamily.id)
      await memoryStore.initData()
    }

    // 解锁应用
    appStore.unlock()
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!registerForm.value.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!registerForm.value.nickname.trim()) {
    errorMsg.value = '请输入昵称'
    return
  }
  if (!registerForm.value.password) {
    errorMsg.value = '请输入密码'
    return
  }
  if (registerForm.value.password.length < 6) {
    errorMsg.value = '密码至少6位'
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    errorMsg.value = '两次密码输入不一致'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    await authStore.register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      nickname: registerForm.value.nickname
    })

    // 注册成功，自动登录
    const result = await authStore.login(registerForm.value.username, registerForm.value.password)

    if (result.user.currentFamily) {
      memoryStore.setCurrentFamilyId(result.user.currentFamily.id)
      await memoryStore.initData()
    }

    appStore.unlock()
  } catch (e: any) {
    errorMsg.value = e.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 检查已有登录状态
onMounted(async () => {
  if (authStore.isLoggedIn) {
    // 已有 token，验证并初始化
    try {
      const user = await authStore.fetchCurrentUser()
      if (user && user.currentFamily) {
        memoryStore.setCurrentFamilyId(user.currentFamily.id)
        await memoryStore.initData()
      }
      appStore.unlock()
    } catch {
      // token 无效，保持登录界面
      authStore.logout()
    }
  }
})
</script>

<template>
  <Transition name="fade">
    <div
      v-if="showLogin"
      class="fixed inset-0 z-[100] flex flex-col items-center justify-center overflow-hidden"
      style="background: linear-gradient(180deg, #FF8A80 0%, #FF9A8C 20%, #FFAB91 35%, #FFD4C4 55%, #FFF0EE 75%, #FFF9F8 100%);"
    >
      <!-- 装饰元素覆盖整个页面 -->
      <div class="absolute inset-0 pointer-events-none overflow-hidden">
        <!-- 上区 - 爱心 -->
        <div class="absolute top-[5%] left-[12%] w-4 h-4 text-white/50 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[10%] right-[18%] w-3 h-3 text-white/40 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[3%] left-[55%] w-2 h-2 text-white/30 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[15%] right-[35%] w-3.5 h-3.5 text-white/45 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>

        <!-- 上区 - 星星 -->
        <div class="absolute top-[4%] right-[42%] w-3 h-3 text-white/50 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[12%] left-[6%] w-2.5 h-2.5 text-white/40 animate-twinkle-delay">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[8%] left-[22%] w-1.5 h-1.5 text-white/30 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>

        <!-- 上区 - 气泡 -->
        <div class="absolute top-[9%] left-[48%] w-2 h-2 bg-white/35 rounded-full animate-bubble" />
        <div class="absolute top-[3%] left-[25%] w-1.5 h-1.5 bg-white/40 rounded-full animate-bubble-delay" />
        <div class="absolute top-[14%] right-[15%] w-2.5 h-2.5 bg-white/30 rounded-full animate-bubble-slow" />

        <!-- 中区 - 爱心（表单两侧） -->
        <div class="absolute top-[35%] left-[5%] w-3.5 h-3.5 text-[#FF8A80]/25 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[45%] right-[6%] w-3 h-3 text-[#FF8A80]/20 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[55%] left-[8%] w-2 h-2 text-[#FFAB91]/25 animate-float-fast">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute top-[40%] right-[3%] w-2.5 h-2.5 text-[#FFAB91]/20 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>

        <!-- 中区 - 星星 -->
        <div class="absolute top-[38%] right-[10%] w-2 h-2 text-[#FF8A80]/20 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[50%] left-[4%] w-2.5 h-2.5 text-[#FFAB91]/20 animate-twinkle-delay">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute top-[60%] right-[8%] w-1.5 h-1.5 text-[#FF8A80]/15 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>

        <!-- 中区 - 气泡 -->
        <div class="absolute top-[42%] left-[3%] w-2 h-2 bg-[#FF8A80]/15 rounded-full animate-bubble" />
        <div class="absolute top-[52%] right-[5%] w-1.5 h-1.5 bg-[#FFAB91]/15 rounded-full animate-bubble-delay" />

        <!-- 下区 - 爱心 -->
        <div class="absolute bottom-[18%] left-[10%] w-3 h-3 text-[#FF8A80]/20 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute bottom-[10%] right-[12%] w-2.5 h-2.5 text-[#FF8A80]/15 animate-float-fast">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute bottom-[5%] left-[45%] w-2 h-2 text-[#FFAB91]/15 animate-float-slow">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>
        <div class="absolute bottom-[15%] right-[30%] w-3 h-3 text-[#FF8A80]/15 animate-float-medium">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
        </div>

        <!-- 下区 - 星星 -->
        <div class="absolute bottom-[20%] right-[25%] w-2 h-2 text-[#FF8A80]/20 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute bottom-[8%] left-[20%] w-1.5 h-1.5 text-[#FFAB91]/20 animate-twinkle-delay">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>
        <div class="absolute bottom-[12%] right-[40%] w-2.5 h-2.5 text-[#FF8A80]/15 animate-twinkle">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
        </div>

        <!-- 下区 - 气泡 -->
        <div class="absolute bottom-[8%] left-[60%] w-1.5 h-1.5 bg-[#FF8A80]/15 rounded-full animate-bubble-delay" />
        <div class="absolute bottom-[3%] right-[20%] w-2 h-2 bg-[#FFAB91]/10 rounded-full animate-bubble-slow" />

        <!-- 大装饰圆 -->
        <div class="absolute -top-16 -right-16 w-40 h-40 bg-white/10 rounded-full" />
        <div class="absolute top-[8%] -left-12 w-28 h-28 bg-white/[0.08] rounded-full" />
        <div class="absolute bottom-[15%] right-4 w-20 h-20 bg-[#FF8A80]/[0.06] rounded-full" />
        <div class="absolute bottom-[30%] -left-8 w-24 h-24 bg-[#FFAB91]/[0.05] rounded-full" />
      </div>

      <!-- 主内容区域 -->
      <div class="relative z-10 flex flex-col items-center w-full max-w-xs px-8">
        <!-- Logo 区域 -->
        <div class="flex flex-col items-center text-white mb-8">
          <div class="w-20 h-20 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center mb-4 border border-white/30 shadow-lg">
            <IconHeart :size="36" :fill="true" class="text-white" />
          </div>
          <h1 class="text-3xl font-serif-title font-black drop-shadow-sm">时光 · 家</h1>
          <p class="text-xs text-white/80 mt-2 tracking-[0.3em]">爱是唯一的密钥</p>
        </div>

        <!-- 登录表单 -->
        <div v-if="mode === 'login'" class="w-full space-y-4">
          <div>
            <input
              v-model="loginForm.username"
              type="text"
              placeholder="用户名"
              class="w-full px-5 py-3.5 rounded-2xl border-none bg-white/90 backdrop-blur-sm text-[#4A403A] placeholder:text-stone-400 focus:ring-4 focus:ring-white/30 outline-none transition-all shadow-lg"
              @keyup.enter="handleLogin"
            />
          </div>
          <div>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              class="w-full px-5 py-3.5 rounded-2xl border-none bg-white/90 backdrop-blur-sm text-[#4A403A] placeholder:text-stone-400 focus:ring-4 focus:ring-white/30 outline-none transition-all shadow-lg"
              @keyup.enter="handleLogin"
            />
          </div>

          <!-- 错误提示 -->
          <p v-if="errorMsg" class="text-white text-sm text-center bg-red-500/80 backdrop-blur-sm rounded-xl py-2 px-4">{{ errorMsg }}</p>

          <!-- 登录按钮 -->
          <button
            @click="handleLogin"
            :disabled="loading"
            class="w-full py-4 bg-white text-[#FF8A80] font-bold rounded-full shadow-xl active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </button>

          <!-- 切换到注册 -->
          <p class="text-center text-sm text-stone-500 pt-2">
            还没有账号？
            <button
              @click="switchMode('register')"
              class="text-[#FF8A80] font-bold hover:text-[#FFAB91]"
            >
              立即注册
            </button>
          </p>
        </div>

        <!-- 注册表单 -->
        <div v-else class="w-full space-y-4">
          <div>
            <input
              v-model="registerForm.username"
              type="text"
              placeholder="用户名"
              class="w-full px-5 py-3.5 rounded-2xl border-none bg-white/90 backdrop-blur-sm text-[#4A403A] placeholder:text-stone-400 focus:ring-4 focus:ring-white/30 outline-none transition-all shadow-lg"
            />
          </div>
          <div>
            <input
              v-model="registerForm.nickname"
              type="text"
              placeholder="昵称（显示名称）"
              class="w-full px-5 py-3.5 rounded-2xl border-none bg-white/90 backdrop-blur-sm text-[#4A403A] placeholder:text-stone-400 focus:ring-4 focus:ring-white/30 outline-none transition-all shadow-lg"
            />
          </div>
          <div>
            <input
              v-model="registerForm.password"
              type="password"
              placeholder="密码（至少6位）"
              class="w-full px-5 py-3.5 rounded-2xl border-none bg-white/90 backdrop-blur-sm text-[#4A403A] placeholder:text-stone-400 focus:ring-4 focus:ring-white/30 outline-none transition-all shadow-lg"
            />
          </div>
          <div>
            <input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              class="w-full px-5 py-3.5 rounded-2xl border-none bg-white/90 backdrop-blur-sm text-[#4A403A] placeholder:text-stone-400 focus:ring-4 focus:ring-white/30 outline-none transition-all shadow-lg"
              @keyup.enter="handleRegister"
            />
          </div>

          <!-- 错误提示 -->
          <p v-if="errorMsg" class="text-white text-sm text-center bg-red-500/80 backdrop-blur-sm rounded-xl py-2 px-4">{{ errorMsg }}</p>

          <!-- 注册按钮 -->
          <button
            @click="handleRegister"
            :disabled="loading"
            class="w-full py-4 bg-white text-[#FF8A80] font-bold rounded-full shadow-xl active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </button>

          <!-- 切换到登录 -->
          <p class="text-center text-sm text-stone-500 pt-2">
            已有账号？
            <button
              @click="switchMode('login')"
              class="text-[#FF8A80] font-bold hover:text-[#FFAB91]"
            >
              立即登录
            </button>
          </p>
        </div>

        <!-- 底部文字 -->
        <p class="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-8">爱让家更温暖</p>
      </div>
    </div>
  </Transition>
</template>

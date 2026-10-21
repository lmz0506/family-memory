<script setup lang="ts">
import {reactive, ref} from 'vue';
import {useRouter} from 'vue-router';
import {ArrowRight, Heart, LoaderCircle, Sparkles} from 'lucide-vue-next';
import {useAuthStore} from '@/stores/auth'

const auth = useAuthStore(), router = useRouter();
const mode = ref<'login' | 'register'>('login');
const form = reactive({username: 'mom', password: '123456', nickname: ''});
const error = ref('');
const loading = ref(false)

async function submit() {
  error.value = '';
  loading.value = true;
  try {
    if (mode.value === 'login') await auth.login(form.username, form.password); else await auth.register(form.username, form.password, form.nickname);
    router.replace(auth.user?.families?.length ? '/' : '/welcome')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <main class="grid min-h-screen bg-[#eee7df] lg:grid-cols-[1.1fr_.9fr]">
    <section class="relative hidden overflow-hidden lg:block"><img
        src="/assets/images/family-sunset.jpg"
        class="absolute inset-0 h-full w-full object-cover"/>
      <div class="absolute inset-0 bg-gradient-to-t from-black/75 via-black/10 to-black/20"></div>
      <div class="absolute left-12 top-10 flex items-center gap-3 text-white">
        <div class="grid h-11 w-11 place-items-center rounded-2xl bg-peach-500">
          <Sparkles :size="21"/>
        </div>
        <div><b class="font-display text-xl">拾光</b>
          <p class="text-[9px] tracking-[.24em] text-white/60">FAMILY MEMORY</p></div>
      </div>
      <div class="absolute bottom-14 left-12 max-w-xl text-white"><p
          class="font-display text-4xl font-black leading-[1.45]">把平凡日子里的相亲相爱，<br/>留给以后慢慢回忆。</p>
        <p class="mt-5 text-sm text-white/65">一个只属于家人的安静空间。</p></div>
    </section>
    <section class="paper flex min-h-screen items-center justify-center p-6">
      <div class="w-full max-w-sm">
        <div class="mb-10 lg:hidden">
          <div class="flex items-center gap-3">
            <div class="grid h-11 w-11 place-items-center rounded-2xl bg-peach-500 text-white">
              <Sparkles :size="21"/>
            </div>
            <b class="font-display text-2xl">拾光</b></div>
        </div>
        <p class="text-xs font-bold tracking-[.2em] text-peach-600">WELCOME HOME</p>
        <h1 class="mt-3 font-display text-3xl font-black">{{ mode === 'login' ? '欢迎回家' : '创建你的拾光账号' }}</h1>
        <p class="mt-3 text-sm text-stone-400">
          {{ mode === 'login' ? '登录后继续翻阅家里的故事。' : '从今天开始，认真收藏家人的故事。' }}</p>
        <form @submit.prevent="submit" class="mt-8 space-y-4"><label v-if="mode==='register'"
                                                                     class="block text-xs font-bold text-stone-500">昵称<input
            v-model="form.nickname" required
            class="mt-2 w-full rounded-2xl border border-stone-200 bg-white px-4 py-3.5 font-normal outline-none focus:border-peach-300"
            placeholder="家人怎么称呼你"/></label><label class="block text-xs font-bold text-stone-500">用户名<input
            v-model="form.username" autocomplete="username" required
            class="mt-2 w-full rounded-2xl border border-stone-200 bg-white px-4 py-3.5 font-normal outline-none focus:border-peach-300"/></label><label
            class="block text-xs font-bold text-stone-500">密码<input v-model="form.password" :autocomplete="mode==='login'?'current-password':'new-password'" required type="password"
                                                                      class="mt-2 w-full rounded-2xl border border-stone-200 bg-white px-4 py-3.5 font-normal outline-none focus:border-peach-300"/></label>
          <p v-if="error" class="rounded-xl bg-red-50 px-3 py-2 text-xs text-red-600">{{ error }}</p>
          <button :disabled="loading"
                  class="flex w-full items-center justify-center gap-2 rounded-2xl bg-peach-500 py-3.5 text-sm font-bold text-white shadow-float disabled:opacity-60">
            <LoaderCircle v-if="loading" class="animate-spin" :size="17"/>
            {{ mode === 'login' ? '登录' : '注册并继续' }}
            <ArrowRight v-if="!loading" :size="17"/>
          </button>
        </form>
        <button @click="mode=mode==='login'?'register':'login'" class="mt-6 w-full text-center text-xs text-stone-400">
          {{ mode === 'login' ? '还没有账号？现在注册' : '已经有账号？返回登录' }}
        </button>
        <div class="mt-10 flex items-center justify-center gap-2 text-[10px] text-stone-300">
          <Heart :size="12" fill="currentColor"/>
          仅与你邀请的家人分享
        </div>
      </div>
    </section>
  </main>
</template>

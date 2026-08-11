<script setup lang="ts">
import {reactive, ref} from 'vue';
import {useRouter} from 'vue-router';
import {ArrowRight, Home, KeyRound, LoaderCircle, Sparkles} from 'lucide-vue-next';
import {familyApi} from '@/api';
import {useAuthStore} from '@/stores/auth';
import {useFamilyStore} from '@/stores/family'

const router = useRouter(), auth = useAuthStore(), family = useFamilyStore();
const mode = ref<'choose' | 'create' | 'join'>('choose');
const form = reactive({name: '', description: '', inviteCode: '', displayName: auth.user?.nickname || ''});
const error = ref('');
const loading = ref(false)

async function submit() {
  loading.value = true;
  error.value = '';
  try {
    if (mode.value === 'create') await familyApi.create({
      name: form.name,
      description: form.description
    }); else await familyApi.join({inviteCode: form.inviteCode, displayName: form.displayName});
    await auth.loadProfile();
    await family.load();
    router.replace('/')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <main class="paper flex min-h-screen items-center justify-center p-5">
    <section
        class="w-full max-w-2xl rounded-[34px] border border-white bg-white/75 p-6 shadow-soft backdrop-blur-xl sm:p-10">
      <div class="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-peach-500 text-white shadow-float">
        <Sparkles :size="24"/>
      </div>
      <div v-if="mode==='choose'" class="text-center"><h1 class="mt-6 font-display text-3xl font-black">
        先找到你的家</h1>
        <p class="mt-3 text-sm text-stone-400">创建一个新的家庭空间，或者接受家人的邀请。</p>
        <div class="mt-8 grid gap-4 sm:grid-cols-2">
          <button @click="mode='create'"
                  class="group rounded-[26px] border border-stone-200 bg-white p-6 text-left transition hover:-translate-y-1 hover:border-peach-200 hover:shadow-soft">
            <div class="grid h-11 w-11 place-items-center rounded-2xl bg-peach-100 text-peach-600">
              <Home :size="20"/>
            </div>
            <h2 class="mt-5 font-display text-xl font-bold">创建家庭</h2>
            <p class="mt-2 text-xs leading-5 text-stone-400">成为家庭管理员，邀请其他家人加入。</p></button>
          <button @click="mode='join'"
                  class="group rounded-[26px] border border-stone-200 bg-white p-6 text-left transition hover:-translate-y-1 hover:border-sky-200 hover:shadow-soft">
            <div class="grid h-11 w-11 place-items-center rounded-2xl bg-sky-100 text-sky-600">
              <KeyRound :size="20"/>
            </div>
            <h2 class="mt-5 font-display text-xl font-bold">加入家庭</h2>
            <p class="mt-2 text-xs leading-5 text-stone-400">输入家人发给你的 8 位邀请码。</p></button>
        </div>
      </div>
      <form v-else @submit.prevent="submit" class="mx-auto mt-7 max-w-md">
        <button type="button" @click="mode='choose'" class="text-xs text-stone-400">← 返回选择</button>
        <h1 class="mt-5 font-display text-2xl font-black">
          {{ mode === 'create' ? '给这个家起个名字' : '接受家人的邀请' }}</h1>
        <div class="mt-6 space-y-4">
          <template v-if="mode==='create'"><input v-model="form.name" required
                                                  class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3.5 text-sm outline-none focus:border-peach-300"
                                                  placeholder="例如：林家小院"/><textarea v-model="form.description"
                                                                                         class="h-24 w-full resize-none rounded-2xl border border-stone-200 bg-white px-4 py-3.5 text-sm outline-none focus:border-peach-300"
                                                                                         placeholder="写一句属于这个家的话"></textarea>
          </template>
          <template v-else><input v-model="form.inviteCode" required maxlength="8"
                                  class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3.5 text-center text-xl font-bold uppercase tracking-[.25em] outline-none focus:border-sky-300"
                                  placeholder="XXXXXXXX"/><input v-model="form.displayName"
                                                                 class="w-full rounded-2xl border border-stone-200 bg-white px-4 py-3.5 text-sm outline-none"
                                                                 placeholder="家人怎么称呼你"/></template>
          <p v-if="error" class="rounded-xl bg-red-50 px-3 py-2 text-xs text-red-600">{{ error }}</p>
          <button :disabled="loading"
                  class="flex w-full items-center justify-center gap-2 rounded-2xl bg-peach-500 py-3.5 text-sm font-bold text-white">
            <LoaderCircle v-if="loading" class="animate-spin" :size="17"/>
            继续
            <ArrowRight v-if="!loading" :size="17"/>
          </button>
        </div>
      </form>
    </section>
  </main>
</template>

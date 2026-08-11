<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {CalendarDays, ChevronLeft, LoaderCircle, LockKeyhole, Send} from 'lucide-vue-next';
import {capsuleApi} from '@/api';
import type {Capsule} from '@/types'

const route = useRoute(), router = useRouter();
const capsule = ref<Capsule | null>(null);
onMounted(async () => capsule.value = await capsuleApi.detail(Number(route.params.id)))
</script>
<template>
  <div v-if="!capsule" class="grid min-h-screen place-items-center">
    <LoaderCircle class="animate-spin text-amber-600"/>
  </div>
  <div v-else class="min-h-screen bg-[#fffaf5] p-5 sm:p-10">
    <button @click="router.back()" class="grid h-11 w-11 place-items-center rounded-full bg-white shadow-soft">
      <ChevronLeft :size="20"/>
    </button>
    <article class="mx-auto mt-6 max-w-2xl overflow-hidden rounded-[34px] bg-white shadow-soft">
      <div class="relative bg-[#5a473b] px-7 py-12 text-center text-white sm:px-12">
        <div class="mx-auto grid h-16 w-16 place-items-center rounded-3xl bg-amber-300 text-[#5a473b]">
          <component :is="capsule.opened?Send:LockKeyhole" :size="27"/>
        </div>
        <p class="mt-6 text-[10px] font-bold tracking-[.2em] text-amber-200">
          {{ capsule.opened ? 'TIME HAS ARRIVED' : 'SEALED FOR THE FUTURE' }}</p>
        <h1 class="mt-3 font-display text-3xl font-black">{{ capsule.title }}</h1>
        <p class="mt-3 text-sm text-white/55">写给 {{ capsule.recipient || '未来的我们' }}</p></div>
      <div class="p-7 sm:p-10">
        <div class="flex items-center gap-2 text-xs text-stone-400">
          <CalendarDays :size="15"/>
          {{ capsule.openDate }} 开启
        </div>
        <div v-if="capsule.opened" class="mt-8 whitespace-pre-wrap font-display text-lg leading-9 text-stone-700">
          {{ capsule.content }}
        </div>
        <div v-else class="mt-8 rounded-[26px] bg-amber-50 p-8 text-center">
          <LockKeyhole class="mx-auto text-amber-500"/>
          <p class="mt-4 font-display text-xl font-bold">这封信还在时间里旅行</p>
          <p class="mt-2 text-xs text-stone-400">还有 {{ capsule.daysRemaining }} 天可以开启</p></div>
        <p class="mt-8 border-t border-stone-100 pt-5 text-[10px] text-stone-400">由 {{ capsule.creatorName }} 于
          {{ capsule.createdAt }} 封存</p></div>
    </article>
  </div>
</template>

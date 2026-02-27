<script setup lang="ts">
import { useMemoryStore, useAppStore } from '@/stores'
import { IconPlus, IconHeart } from '@/components/icons'

const memoryStore = useMemoryStore()
const appStore = useAppStore()
</script>

<template>
  <div class="py-4">
    <h2 class="text-xs font-black text-stone-400 uppercase tracking-[0.2em] mb-6 px-2">家庭成员</h2>

    <div class="space-y-4">
      <div
        v-for="(member, i) in memoryStore.familyMembers"
        :key="i"
        class="glass-card rounded-[2.2rem] p-5 flex items-center justify-between group hover:border-[#64B5F6]/30 transition-all cursor-pointer"
      >
        <div class="flex items-center gap-5">
          <div class="w-14 h-14 rounded-2xl shadow-sm overflow-hidden group-hover:rotate-3 transition-transform">
            <img
              v-if="member.avatar?.startsWith('http')"
              :src="member.avatar"
              class="w-full h-full object-cover"
              alt=""
            />
            <div
              v-else
              class="w-full h-full bg-gradient-to-br from-[#FF8A80] to-[#FFAB91] flex items-center justify-center text-2xl text-white"
            >
              {{ member.avatar || '👤' }}
            </div>
          </div>
          <div>
            <h3 class="font-bold text-stone-700 text-lg">{{ member.name }}</h3>
            <div class="flex items-center gap-2 mt-0.5">
              <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
              <p class="text-[10px] text-stone-400 font-medium">{{ member.role || '家庭成员' }}</p>
            </div>
          </div>
        </div>
        <button class="w-10 h-10 rounded-full bg-[#FFF9F8] flex items-center justify-center text-[#FF8A80] shadow-sm hover:scale-110 transition-transform">
          <IconHeart :size="18" />
        </button>
      </div>

      <!-- 邀请家人 -->
      <div
        @click="appStore.openInviteFamilyModal()"
        class="glass-card rounded-[2.2rem] p-5 flex items-center justify-center gap-4 cursor-pointer hover:border-[#FF8A80]/30 transition-all group border-2 border-dashed border-stone-200"
      >
        <div
          class="w-12 h-12 bg-[#FFF0EE] rounded-full flex items-center justify-center text-[#FF8A80] group-hover:scale-110 transition-transform"
        >
          <IconPlus :size="24" />
        </div>
        <span class="text-sm text-stone-500 font-medium group-hover:text-[#FF8A80] transition-colors">邀请家人加入</span>
      </div>
    </div>

    <!-- 家庭统计 -->
    <div class="mt-10 px-2">
      <h3 class="text-xs font-black text-stone-400 uppercase tracking-[0.2em] mb-4">家庭数据</h3>
      <div
        class="glass-card rounded-[2rem] p-6 flex justify-around text-center"
      >
        <div class="flex-1">
          <div class="text-2xl font-bold text-[#FF8A80]">
            {{ memoryStore.statistics.recordDays.toLocaleString() }}
          </div>
          <div class="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-1">记录天数</div>
        </div>
        <div class="w-px bg-stone-200/50"></div>
        <div class="flex-1">
          <div class="text-2xl font-bold text-[#FFAB91]">
            {{ memoryStore.statistics.photoCount.toLocaleString() }}
          </div>
          <div class="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-1">照片视频</div>
        </div>
        <div class="w-px bg-stone-200/50"></div>
        <div class="flex-1">
          <div class="text-2xl font-bold text-[#64B5F6]">
            {{ memoryStore.statistics.memoryCount.toLocaleString() }}
          </div>
          <div class="text-[10px] text-stone-400 font-bold uppercase tracking-widest mt-1">珍藏故事</div>
        </div>
      </div>
    </div>
  </div>
</template>

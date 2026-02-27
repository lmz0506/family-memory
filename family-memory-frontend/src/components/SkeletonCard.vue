<script setup lang="ts">
defineProps<{
  type?: 'card' | 'timeline' | 'today'
}>()
</script>

<template>
  <!-- 记忆卡片骨架屏 -->
  <div v-if="type === 'card' || !type" class="glass-card rounded-[2.5rem] p-5 animate-pulse">
    <!-- 头部 -->
    <div class="flex items-center gap-3 mb-4">
      <div class="w-11 h-11 rounded-full bg-stone-200"></div>
      <div class="flex-1">
        <div class="h-4 bg-stone-200 rounded-lg w-24 mb-2"></div>
        <div class="h-3 bg-stone-100 rounded w-32"></div>
      </div>
      <div class="w-8 h-8 rounded-full bg-stone-100"></div>
    </div>

    <!-- 图片区域 -->
    <div class="aspect-video bg-stone-200 rounded-[2rem] mb-4"></div>

    <!-- 内容 -->
    <div class="h-5 bg-stone-200 rounded-lg w-3/4 mb-3"></div>
    <div class="space-y-2">
      <div class="h-3 bg-stone-100 rounded w-full"></div>
      <div class="h-3 bg-stone-100 rounded w-5/6"></div>
    </div>

    <!-- 底部 -->
    <div class="mt-4 pt-4 border-t border-stone-100 flex justify-between items-center">
      <div class="flex gap-2">
        <div class="h-5 bg-stone-100 rounded-lg w-12"></div>
        <div class="h-5 bg-stone-100 rounded-lg w-14"></div>
      </div>
      <div class="w-6 h-6 rounded-full bg-stone-100"></div>
    </div>
  </div>

  <!-- 时间线骨架屏 -->
  <div v-else-if="type === 'timeline'" class="relative pl-10 animate-pulse">
    <!-- Dot -->
    <div class="absolute -left-[9px] top-1 w-4 h-4 rounded-full bg-stone-200"></div>

    <div class="glass-card rounded-3xl p-4">
      <div class="h-3 bg-stone-200 rounded w-20 mb-3"></div>

      <div class="flex gap-4">
        <div class="w-16 h-16 rounded-xl bg-stone-200 flex-shrink-0"></div>
        <div class="flex-1">
          <div class="h-4 bg-stone-200 rounded w-3/4 mb-2"></div>
          <div class="h-3 bg-stone-100 rounded w-full mb-1"></div>
          <div class="h-3 bg-stone-100 rounded w-2/3"></div>
          <div class="mt-2 flex gap-3">
            <div class="h-3 bg-stone-100 rounded w-16"></div>
            <div class="h-3 bg-stone-100 rounded w-20"></div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 那年今日骨架屏 -->
  <div v-else-if="type === 'today'" class="glass-card rounded-[2.5rem] p-4 flex gap-4 items-center animate-pulse">
    <div class="w-20 h-24 bg-stone-200 rounded-2xl flex-shrink-0"></div>
    <div class="flex-1">
      <div class="h-5 bg-stone-200 rounded-full w-24 mb-2"></div>
      <div class="h-4 bg-stone-200 rounded w-3/4 mb-2"></div>
      <div class="h-3 bg-stone-100 rounded w-full"></div>
      <div class="h-3 bg-stone-100 rounded w-2/3 mt-1"></div>
    </div>
  </div>
</template>

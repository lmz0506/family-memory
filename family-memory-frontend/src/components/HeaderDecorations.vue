<template>
  <div class="absolute inset-0 pointer-events-none">
    <!-- 浮动爱心 -->
    <div class="absolute top-6 left-[12%] w-4 h-4 text-white/50 animate-float-slow">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>
    <div class="absolute top-14 right-[18%] w-3 h-3 text-white/40 animate-float-medium">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>
    <div class="absolute top-20 left-[32%] w-2.5 h-2.5 text-white/35 animate-float-fast">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>
    <div class="absolute top-10 left-[55%] w-2 h-2 text-white/30 animate-float-medium">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>
    <div class="absolute top-24 right-[35%] w-3.5 h-3.5 text-white/45 animate-float-slow">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>

    <!-- 浮动星星 -->
    <div class="absolute top-8 right-[42%] w-3 h-3 text-white/50 animate-twinkle">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>
    <div class="absolute top-16 left-[6%] w-2.5 h-2.5 text-white/40 animate-twinkle-delay">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>
    <div class="absolute top-4 left-[42%] w-2 h-2 text-white/35 animate-twinkle">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>
    <div class="absolute top-20 right-[8%] w-2 h-2 text-white/45 animate-twinkle-delay">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>
    <div class="absolute top-12 left-[22%] w-1.5 h-1.5 text-white/30 animate-twinkle">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>

    <!-- 浮动圆点/气泡 -->
    <div class="absolute top-14 left-[48%] w-2 h-2 bg-white/35 rounded-full animate-bubble" />
    <div class="absolute top-5 left-[25%] w-1.5 h-1.5 bg-white/40 rounded-full animate-bubble-delay" />
    <div class="absolute top-18 right-[15%] w-2.5 h-2.5 bg-white/30 rounded-full animate-bubble-slow" />
    <div class="absolute top-22 left-[18%] w-1.5 h-1.5 bg-white/25 rounded-full animate-bubble" />

    <!-- 下半区爱心 -->
    <div class="absolute bottom-20 left-[10%] w-3 h-3 text-white/35 animate-float-medium">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>
    <div class="absolute bottom-14 right-[12%] w-2.5 h-2.5 text-white/30 animate-float-fast">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>
    <div class="absolute bottom-8 left-[45%] w-2 h-2 text-white/25 animate-float-slow">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
    </div>

    <!-- 下半区星星 -->
    <div class="absolute bottom-24 right-[30%] w-2 h-2 text-white/40 animate-twinkle">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>
    <div class="absolute bottom-10 left-[25%] w-1.5 h-1.5 text-white/35 animate-twinkle-delay">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>
    <div class="absolute bottom-16 right-[45%] w-2.5 h-2.5 text-white/30 animate-twinkle">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
    </div>

    <!-- 下半区气泡 -->
    <div class="absolute bottom-12 left-[60%] w-1.5 h-1.5 bg-white/30 rounded-full animate-bubble-delay" />
    <div class="absolute bottom-6 right-[20%] w-2 h-2 bg-white/20 rounded-full animate-bubble-slow" />

    <!-- 大装饰圆 -->
    <div class="absolute -top-16 -right-16 w-40 h-40 bg-white/10 rounded-full" />
    <div class="absolute top-8 -left-12 w-28 h-28 bg-white/[0.08] rounded-full" />
    <div class="absolute bottom-4 right-4 w-20 h-20 bg-white/[0.06] rounded-full" />
  </div>
</template>

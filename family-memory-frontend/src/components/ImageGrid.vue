<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  images: string[]
}>()

const gridClass = computed(() => {
  const c = props.images.length
  const base = 'grid gap-1.5 rounded-[2rem] overflow-hidden shadow-sm border border-white '
  if (c === 1) return 'rounded-[2rem] overflow-hidden shadow-sm border border-white relative group'
  if (c === 2) return base + 'grid-cols-2 h-48'
  if (c === 3) return base + 'grid-cols-3 h-32'
  if (c === 4) return base + 'grid-cols-2 h-64'
  return base + 'grid-cols-3 h-auto'
})

const displayImages = computed(() => {
  return props.images.length > 9 ? props.images.slice(0, 9) : props.images
})
</script>

<template>
  <div v-if="images && images.length > 0" :class="gridClass">
    <div
      v-for="(img, idx) in displayImages"
      :key="idx"
      class="relative w-full h-full overflow-hidden group bg-[#FFF0EE]"
    >
      <img
        :src="img"
        :alt="'Memory ' + idx"
        class="w-full h-full object-cover transition-transform duration-[1.5s] group-hover:scale-110"
      />
    </div>
  </div>
</template>

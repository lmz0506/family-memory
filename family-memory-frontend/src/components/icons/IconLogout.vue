<script setup lang="ts">
import IconBase from './IconBase.vue'

defineProps<{ size?: number | string; fill?: boolean }>()
</script>

<template>
  <IconBase :size="size" :fill="fill">
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
    <polyline points="16 17 21 12 16 7" />
    <line x1="21" x2="9" y1="12" y2="12" />
  </IconBase>
</template>

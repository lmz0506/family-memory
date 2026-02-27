<script setup lang="ts">
import IconBase from './IconBase.vue'

defineProps<{ size?: number | string; fill?: boolean }>()
</script>

<template>
  <IconBase :size="size" :fill="fill">
    <path d="M20 12v10H4V12" />
    <path d="M2 7h20v5H2z" />
    <path d="M12 22V7" />
    <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z" />
    <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z" />
  </IconBase>
</template>

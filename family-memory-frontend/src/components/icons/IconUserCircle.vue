<script setup lang="ts">
import IconBase from './IconBase.vue'

defineProps<{ size?: number | string; fill?: boolean }>()
</script>

<template>
  <IconBase :size="size" :fill="fill">
    <circle cx="12" cy="12" r="10" />
    <circle cx="12" cy="10" r="3" />
    <path d="M7 20.662V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.662" />
  </IconBase>
</template>

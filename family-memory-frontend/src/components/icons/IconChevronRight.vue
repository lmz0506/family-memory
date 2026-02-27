<script setup lang="ts">
import IconBase from './IconBase.vue'

defineProps<{ size?: number | string }>()
</script>

<template>
  <IconBase :size="size">
    <polyline points="9 18 15 12 9 6" />
  </IconBase>
</template>

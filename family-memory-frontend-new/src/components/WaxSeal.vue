<script setup lang="ts">
import {LockKeyhole,Check} from 'lucide-vue-next'
defineProps<{opened?:boolean}>()
</script>
<template><span class="wax-seal" :class="{'wax-opened':opened}" aria-hidden="true"><span><Check v-if="opened" :size="20"/><LockKeyhole v-else :size="20"/></span></span></template>
<style scoped>
.wax-seal{position:relative;display:inline-grid;place-items:center;flex-shrink:0;width:57px;height:57px;border-radius:46% 54% 48% 52%/55% 44% 56% 45%;color:#e6c8aa;background:radial-gradient(circle at 35% 25%,#a96f59,#704535 65%,#512d27);box-shadow:inset 0 2px 3px #edc4a340,inset 0 -3px 5px #26171b80,0 5px 10px #0004;border:1px solid #b8836555}.wax-seal>span{display:grid;place-items:center;width:40px;height:40px;border:1px solid #d7a98544;border-radius:50%;box-shadow:inset 0 1px 3px #2d171c40}.wax-opened{background:radial-gradient(circle at 35% 25%,#737b65,#454b42 70%,#333a34);color:#cdd3b2;border-color:#9ca78b44}
</style>

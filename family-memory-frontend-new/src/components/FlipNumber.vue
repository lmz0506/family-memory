<script setup lang="ts">
defineProps<{value:number}>()
</script>
<template><span class="flip-number" :aria-label="String(value)"><Transition name="number-turn" mode="out-in" appear><span :key="value" aria-hidden="true">{{value}}</span></Transition></span></template>
<style scoped>
.flip-number{display:inline-flex;perspective:400px;font-variant-numeric:tabular-nums}.flip-number>span{display:block;transform-origin:center 70%;background:linear-gradient(135deg,#fcdfaa 5%,#c3924e 38%,#f2d39a 52%,#b88b4b 85%);background-clip:text;-webkit-background-clip:text;color:transparent}.number-turn-enter-active,.number-turn-leave-active{transition:transform .5s ease-out,opacity .4s ease}.number-turn-enter-from{transform:rotateX(-35deg) translateY(6px);opacity:0}.number-turn-leave-to{transform:rotateX(25deg);opacity:0}@media(prefers-reduced-motion:reduce){.number-turn-enter-active,.number-turn-leave-active{transition:none}.number-turn-enter-from{transform:none}}
</style>

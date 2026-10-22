<script setup lang="ts">
defineProps<{title:string;description?:string}>()
</script>
<template><header class="sky-page-header"><div><div class="sky-page-title-line"><h1 class="page-title">{{title}}</h1><slot name="title-extra"/></div><p v-if="description">{{description}}</p></div><div v-if="$slots.default" class="sky-page-actions"><slot/></div></header></template>

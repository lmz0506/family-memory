<script setup lang="ts">
import {computed,defineAsyncComponent,ref,onMounted,onBeforeUnmount} from 'vue'
import {useRoute} from 'vue-router'
import {Archive,Plus,Sparkles} from 'lucide-vue-next'
import {useFamilyStore} from '@/stores/family'
const StarTimeline=defineAsyncComponent(()=>import('@/components/StarTimeline.vue'))
const family=useFamilyStore(),route=useRoute()
const createOpen=ref(false),createMenu=ref<HTMLElement>()
function enterCreate(event:PointerEvent){if(event.pointerType==='mouse')createOpen.value=true}
function leaveCreatePointer(){if(!createMenu.value?.contains(document.activeElement))createOpen.value=false}
function outsideCreate(event:PointerEvent){if(!createMenu.value?.contains(event.target as Node))createOpen.value=false}
onMounted(()=>document.addEventListener('pointerdown',outsideCreate))
onBeforeUnmount(()=>document.removeEventListener('pointerdown',outsideCreate))
function leaveCreate(event:FocusEvent){if(!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node|null))createOpen.value=false}
const keyword=computed(()=>typeof route.query.keyword==='string'?route.query.keyword:'')
function compose(){window.dispatchEvent(new Event('family-memory-compose'))}
function capsule(){window.dispatchEvent(new Event('family-capsule-compose'))}
</script>
<template>
  <section class="star-home" aria-label="一家人的记忆星海">
    <Suspense>
      <StarTimeline v-if="family.current" :key="family.current.id" :family-id="family.current.id" :initial-keyword="keyword" home>
        <template #home-actions><div ref="createMenu" class="sky-create-menu" @pointerenter="enterCreate" @pointerleave="leaveCreatePointer" @focusout="leaveCreate" @keydown.esc.stop="createOpen=false"><button class="sky-create-trigger" aria-label="新增记忆或珍藏" :aria-expanded="createOpen" aria-controls="sky-create-choices" @click="createOpen=true"><Plus :size="20"/></button><div v-if="createOpen" id="sky-create-choices" class="sky-create-choices"><button @click="createOpen=false;compose()"><Plus :size="17"/>新增记忆</button><button @click="createOpen=false;capsule()"><Archive :size="17"/>封存珍藏</button></div></div></template>
      </StarTimeline>
      <template #fallback><div class="sky-starting" role="status"><Sparkles :size="26"/><p>正在展开一家人的星图…</p><RouterLink :to="{name:'album'}">先翻阅回忆相册</RouterLink></div></template>
    </Suspense>
    <div v-if="!family.current" class="sky-starting" role="status"><Sparkles :size="26"/><p>正在寻找我们的小家…</p><RouterLink :to="{name:'album'}">回忆相册</RouterLink></div>
  </section>
</template>

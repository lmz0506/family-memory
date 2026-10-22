<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {CalendarDays, ChevronLeft, LoaderCircle, LockKeyhole, Send} from 'lucide-vue-next';
import CapsuleLetter from '@/components/CapsuleLetter.vue'
import {capsuleApi} from '@/api';
import type {Capsule} from '@/types'

const route = useRoute(), router = useRouter();
const capsule = ref<Capsule | null>(null);
const error=ref(''),checking=ref(false)
async function load(){if(checking.value)return;checking.value=true;error.value='';try{capsule.value=await capsuleApi.detail(Number(route.params.id))}catch(e){error.value=(e as Error).message}finally{checking.value=false}}
function goBack(){window.history.state?.back?router.back():router.push({name:route.query.from==='family'?'family':route.query.from==='home'||route.query.from==='universe'?'home':'vault'})}
onMounted(load)
</script>
<template>
  <div v-if="error" class="empty-state" role="alert"><p>{{error}}</p><button @click="load" class="sky-primary rounded-full bg-night-action px-5 py-3 text-night-action-ink">重新加载</button></div>
  <div v-else-if="!capsule" class="grid min-h-[60dvh] place-items-center">
    <LoaderCircle class="animate-spin text-night-accent"/>
  </div>
  <div v-else class="page-shell page-reveal">
    <button @click="goBack" :aria-label="route.query.from==='family'?'返回家人':route.query.from==='home'||route.query.from==='universe'?'返回星海':'返回珍藏'" class="grid h-11 w-11 place-items-center rounded-full bg-night-surface shadow-soft">
      <ChevronLeft :size="20"/>
    </button>
    <CapsuleLetter :capsule="capsule" :checking="checking" @refresh="load"/>

  </div>
</template>

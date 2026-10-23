<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {CalendarDays, ChevronLeft, LoaderCircle, LockKeyhole, Send} from 'lucide-vue-next';
import CapsuleLetter from '@/components/CapsuleLetter.vue'
import StarChart from '@/components/StarChart.vue'
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
  <section v-else class="capsule-observatory page-reveal">
    <StarChart variant="capsule"/>
    <div class="detail-back-row"><button @click="goBack" class="detail-back"><ChevronLeft :size="17"/>返回{{route.query.from==='family'?'家人':route.query.from==='home'||route.query.from==='universe'?'星海':'珍藏'}}</button><span>✧ LIGHT TO THE FUTURE / 星光珍藏</span></div>
    <div class="capsule-flight-meta"><span><Send :size="13"/>寄给 {{capsule.recipient||'未来的我们'}}</span><span><CalendarDays :size="13"/>{{capsule.openDate}} · 约定重逢</span><span><LockKeyhole :size="13"/>{{capsule.opened?'星光已抵达':'时间守护中'}}</span></div>
    <CapsuleLetter :capsule="capsule" :checking="checking" @refresh="load"/>
    <p class="capsule-endnote">星光走过漫长的路，只为在那一天与你相逢。</p>
  </section>
</template>

<script setup lang="ts">
import {reactive,ref,toRef} from 'vue'
import {Archive,LoaderCircle,X} from 'lucide-vue-next'
import {capsuleApi} from '@/api'
import {useDialog} from '@/composables/useDialog'
import {notify} from '@/composables/useToast'
import {useCreationEvents} from '@/composables/useCreationEvents'
import {capsuleReceipt} from '@/composables/creationReceipt'
const props=defineProps<{modelValue:boolean;familyId:number;embedded?:boolean}>()
const emit=defineEmits<{'update:modelValue':[value:boolean];created:[]}>()
const panel=ref<HTMLElement>(),saving=ref(false),error=ref('')
const form=reactive({title:'',recipient:'',content:'',openDate:''})
function minDate(){const date=new Date();return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`}
function close(){if(!saving.value)emit('update:modelValue',false)}
useDialog(toRef(props,'modelValue'),panel,close,()=>!props.embedded)
async function create(){
  if(saving.value)return
  saving.value=true;error.value=''
  try{
    const saved=await capsuleApi.create(props.familyId,{...form})
    const bounds=panel.value?.getBoundingClientRect()
    useCreationEvents().publish(capsuleReceipt(saved,props.familyId,bounds?{x:bounds.left+bounds.width/2,y:bounds.top+bounds.height/2,width:props.embedded?bounds.width:undefined,height:props.embedded?bounds.height:undefined}:undefined))
    Object.assign(form,{title:'',recipient:'',content:'',openDate:''})
    emit('update:modelValue',false);emit('created');notify('这封信，已经交给时间保管')
  }catch(e){error.value=(e as Error).message}finally{saving.value=false}
}
</script>
<template>
  <Teleport to="body"><Transition name="fade"><div v-if="modelValue" class="capsule-compose-backdrop" :class="{'sky-editor-host':embedded}" @click.self="close">
    <form ref="panel" role="dialog" :aria-modal="!embedded" aria-labelledby="capsule-compose-heading" tabindex="-1" class="capsule-compose-panel" :class="{'sky-editor':embedded}" @submit.prevent="create">
      <header class="capsule-compose-heading"><div><p class="page-eyebrow">写给未来的我们</p><h2 id="capsule-compose-heading"><Archive :size="22"/>封存一封信</h2></div><button type="button" :disabled="saving" aria-label="关闭时光胶囊编辑" @click="close"><X :size="21"/></button></header>
      <p class="capsule-compose-intro">把今天的心意，留给以后的某一天。</p>
      <fieldset :disabled="saving" class="capsule-compose-fields">
        <label>信的标题<input v-model="form.title" required placeholder="给未来留下一句话"/></label>
        <label>收信人（选填）<input v-model="form.recipient" placeholder="写给谁"/></label>
        <label>写给未来的话<textarea v-model="form.content" required rows="5" placeholder="想对那时的我们说些什么？"></textarea></label>
        <label>开启日期<input v-model="form.openDate" type="date" :min="minDate()" required/><small>在约定的日期，按照信件的开启状态查看内容。</small></label>
      </fieldset>
      <p v-if="error" role="alert" class="capsule-compose-error">{{error}}</p>
      <footer><button :disabled="saving" class="fm-primary"><LoaderCircle v-if="saving" :size="17" class="animate-spin"/>{{saving?'正在封存…':'封存到未来'}}</button></footer>
    </form>
  </div></Transition></Teleport>
</template>
<style scoped>
.capsule-compose-backdrop{position:fixed;inset:0;z-index:90;display:flex;align-items:center;justify-content:center;padding:24px;background:#241f2466}
.capsule-compose-panel{width:100%;max-width:600px;max-height:90dvh;overflow-y:auto;overscroll-behavior:contain;border-radius:25px;background:var(--ux-surface);color:var(--ux-ink);padding:28px;box-shadow:0 24px 80px #241c2933;outline:none}
.capsule-compose-heading{display:flex;justify-content:space-between;align-items:center;gap:15px}.capsule-compose-heading h2{display:flex;align-items:center;gap:9px;margin:7px 0;font-family:"Songti SC",serif;font-size:26px;font-weight:600}.capsule-compose-heading h2 svg{color:var(--ux-accent)}.capsule-compose-heading>button{width:44px;height:44px;display:grid;place-items:center;border-radius:50%;background:var(--ux-surface-2)}.capsule-compose-intro{font-size:14px;color:var(--ux-ink-3);margin:8px 0 23px}
.capsule-compose-fields{display:grid;gap:17px}.capsule-compose-fields label{font-size:14px;color:var(--ux-ink-2)}.capsule-compose-fields input,.capsule-compose-fields textarea{display:block;width:100%;padding:12px 14px;margin-top:7px;border:1px solid var(--ux-line-2);border-radius:12px;background:var(--ux-surface-2);color:var(--ux-ink);font-size:16px;outline:none;transition:border-color .16s,background .16s}.capsule-compose-fields input:focus,.capsule-compose-fields textarea:focus{border-color:#a58a75;background:var(--ux-surface-2)}.capsule-compose-fields small{display:block;color:var(--ux-ink-3);font-size:12px;margin-top:8px}.capsule-compose-error{color:var(--ux-danger);font-size:14px;margin-top:14px}.capsule-compose-panel footer{position:sticky;bottom:-28px;padding:21px 0 8px;background:var(--ux-surface)}.capsule-compose-panel footer>button{width:100%;justify-content:center}
@media(max-width:639px){.capsule-compose-backdrop{padding:0;align-items:end}.capsule-compose-panel{border-radius:24px 24px 0 0;padding:24px 20px;padding-bottom:max(20px,env(safe-area-inset-bottom));max-height:94dvh}}
</style>

<script setup lang="ts">
import {computed, onBeforeUnmount, reactive, ref, toRef, watch} from 'vue'
import {Image, LoaderCircle, Mic, X, Film, Trash2} from 'lucide-vue-next'
import {memoryApi, uploadApi} from '@/api'
import {useDialog} from '@/composables/useDialog'
import {notify} from '@/composables/useToast'
import UploadProgress, {type UploadItem} from './UploadProgress.vue'
import {memoryReceipt} from '@/composables/creationReceipt'
import {useCreationEvents} from '@/composables/useCreationEvents'

const props = defineProps<{modelValue:boolean; familyId:number;embedded?:boolean}>()
const emit = defineEmits<{'update:modelValue':[value:boolean]; created:[]}>()
const panel = ref<HTMLElement>()
const files = ref<File[]>([])
const audioFile = ref<File | null>(null)
const saving = ref(false)
const error = ref('')
const previews = ref<Array<{file:File; url:string}>>([])
const uploads = ref<UploadItem[]>([])
const uploaded = new Map<File,string>()
function localToday(){const date=new Date();return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`}
const form = reactive({title:'',content:'',memoryDate:localToday(),location:'',tags:''})
const allFiles = computed(()=>[...files.value,...(audioFile.value?[audioFile.value]:[])])
function clearPreviews(){previews.value.forEach(item=>URL.revokeObjectURL(item.url))}
watch(files,value=>{clearPreviews();previews.value=value.map(file=>({file,url:URL.createObjectURL(file)}))})
onBeforeUnmount(clearPreviews)
function choose(event:Event){const selected=Array.from((event.target as HTMLInputElement).files||[]),available=Math.max(0,9-files.value.length);files.value=[...files.value,...selected.slice(0,available)];(event.target as HTMLInputElement).value='';if(selected.length>available)notify(`最多珍藏 9 个照片或视频，本次有 ${selected.length-available} 个未添加`,'info')}
function chooseAudio(event:Event){audioFile.value=(event.target as HTMLInputElement).files?.[0]||null}
function close(){if(!saving.value)emit('update:modelValue',false)}
useDialog(toRef(props,'modelValue'),panel,close,()=>!props.embedded)
async function submit(){
  if(saving.value)return
  if(!form.title.trim()){error.value='给这段记忆起个名字吧';return}
  saving.value=true;error.value=''
  uploads.value=allFiles.value.map(file=>({name:file.name,progress:uploaded.has(file)?100:0,state:uploaded.has(file)?'done':'waiting'}))
  try{
    for(const [index,file] of allFiles.value.entries()){
      if(uploaded.has(file))continue
      const entry=uploads.value[index]!
      entry.state='uploading'
      try{const result=await uploadApi.file(file,file===audioFile.value?'audio':'memory',percent=>entry.progress=percent);uploaded.set(file,result.url);entry.progress=100;entry.state='done'}
      catch(e){entry.state='error';throw e}
    }
    const mediaUrls=files.value.map(file=>uploaded.get(file)!)
    const audioUrl=audioFile.value?uploaded.get(audioFile.value):''
    const saved=await memoryApi.create(props.familyId,{...form,title:form.title.trim(),type:files.value.length?'photo':audioUrl?'audio':'note',mediaUrls,audioUrl,tags:form.tags.split(/[,，]/).map(value=>value.trim()).filter(Boolean)})
    const bounds=panel.value?.getBoundingClientRect()
    useCreationEvents().publish(memoryReceipt(saved,bounds?{x:bounds.left+bounds.width/2,y:bounds.top+bounds.height/2,width:props.embedded?bounds.width:undefined,height:props.embedded?bounds.height:undefined}:undefined))
    Object.assign(form,{title:'',content:'',memoryDate:localToday(),location:'',tags:''});files.value=[];audioFile.value=null;uploads.value=[];uploaded.clear()
    emit('created');emit('update:modelValue',false);notify('这一刻，已经被好好珍藏')
  }catch(e){error.value=(e as Error).message;notify('暂时没有保存成功，内容已为你保留','error')}
  finally{saving.value=false}
}
</script>
<template>
  <Teleport to="body"><Transition name="fade"><div v-if="modelValue" class="composer-backdrop" :class="{'sky-editor-host':embedded}" @click.self="close">
    <form ref="panel" role="dialog" :aria-modal="!embedded" aria-labelledby="composer-title" tabindex="-1" class="composer-panel" :class="{'sky-editor':embedded}" @submit.prevent="submit">
      <header class="flex items-center justify-between"><div><p class="page-eyebrow">A MOMENT TO KEEP</p><h2 id="composer-title" class="mt-1 font-serif text-2xl font-bold">记录这一刻</h2></div><button type="button" :disabled="saving" aria-label="关闭记忆编辑" @click="close" class="grid h-11 w-11 place-items-center rounded-full bg-night-soft"><X :size="20"/></button></header>
      <p v-if="error" role="alert" class="mt-4 rounded-xl bg-night-danger/10 p-3 text-sm text-night-danger">{{error}}</p>
      <fieldset :disabled="saving" class="mt-5 space-y-4">
        <label class="block"><span class="sr-only">记忆标题</span><input v-model="form.title" required maxlength="120" class="w-full border-0 bg-transparent font-serif text-xl font-bold outline-none placeholder:text-night-muted" placeholder="给这段记忆起个名字"/></label>
        <label class="block"><span class="sr-only">记忆故事</span><textarea v-model="form.content" rows="4" class="w-full resize-y border-0 bg-transparent text-base leading-7 outline-none placeholder:text-night-muted" placeholder="今天发生了什么值得记住的事？"></textarea></label>
        <div v-if="previews.length" class="flex flex-wrap gap-3"><div v-for="(item,index) in previews" :key="item.url" class="relative h-24 w-24 overflow-hidden rounded-xl bg-night-soft"><video v-if="item.file.type.startsWith('video/')" :src="item.url" class="h-full w-full object-cover" muted preload="metadata"/><img v-else :src="item.url" :alt="item.file.name" class="h-full w-full object-cover"/><button type="button" :aria-label="`移除${item.file.name}`" class="absolute right-1 top-1 grid h-7 w-7 place-items-center rounded-full bg-black/60 text-white" @click="files=files.filter((_,i)=>i!==index)"><X :size="14"/></button></div></div>
        <div class="grid grid-cols-2 gap-3"><label class="relative flex cursor-pointer items-center justify-center gap-2 rounded-2xl border border-dashed border-night-line-strong bg-night-soft py-4 text-sm font-semibold text-night-ink"><Image :size="20"/>照片或视频<input type="file" multiple accept="image/*,video/*" class="absolute inset-0 w-full cursor-pointer opacity-0" aria-label="选择照片或视频，最多9个" @change="choose"/></label><label class="relative flex cursor-pointer items-center justify-center gap-2 rounded-2xl border border-dashed border-night-line-strong bg-night-soft py-4 text-sm font-semibold text-night-ink"><Mic :size="20"/>一段声音<input type="file" accept="audio/*" class="absolute inset-0 w-full cursor-pointer opacity-0" aria-label="选择声音文件" @change="chooseAudio"/></label></div>
        <div v-if="audioFile" class="flex items-center justify-between gap-2 rounded-xl bg-night-soft p-3 text-sm"><span class="truncate">{{audioFile.name}}</span><button type="button" aria-label="移除声音" @click="audioFile=null"><Trash2 :size="16"/></button></div>
        <p class="text-xs text-night-secondary">最多 9 个照片或视频，也可以只写一段故事。</p>
        <div class="grid gap-3 sm:grid-cols-2"><label class="text-sm text-night-secondary">发生日期<input v-model="form.memoryDate" required type="date" class="mt-1.5 w-full rounded-xl border border-night-line bg-night-surface p-3 text-base text-night-ink"/></label><label class="text-sm text-night-secondary">地点<input v-model="form.location" class="mt-1.5 w-full rounded-xl border border-night-line bg-night-surface p-3 text-base text-night-ink" placeholder="例如：东湖绿道"/></label></div>
        <label class="block text-sm text-night-secondary">标签<input v-model="form.tags" class="mt-1.5 w-full rounded-xl border border-night-line bg-night-surface p-3 text-base text-night-ink" placeholder="用逗号分隔，例如：生日，旅行"/></label>
      </fieldset>
      <UploadProgress v-if="uploads.length" :items="uploads" :busy="saving" class="mt-4" @retry="submit"/>
      <footer class="composer-footer"><button :disabled="saving" class="sky-primary flex min-h-12 w-full items-center justify-center gap-2 rounded-full bg-night-action text-base font-semibold text-night-action-ink transition hover:bg-night-action disabled:opacity-60"><LoaderCircle v-if="saving" :size="18" class="animate-spin"/>{{saving?'正在珍藏…':'珍藏这段记忆'}}</button></footer>
    </form>
  </div></Transition></Teleport>
</template>
<style scoped>
.composer-backdrop{position:fixed;inset:0;z-index:90;display:flex;align-items:center;justify-content:center;padding:24px;background:#03060c70;backdrop-filter:blur(6px)}
.composer-panel{width:100%;max-width:580px;max-height:90dvh;overflow-y:auto;overscroll-behavior:contain;background:var(--ux-surface);border-radius:28px;padding:28px;box-shadow:0 24px 80px #03060c30;outline:none}
.composer-footer{position:sticky;bottom:-28px;padding:20px 0 10px;background:linear-gradient(transparent,var(--ux-surface) 20%)}
@media(max-width:639px){.composer-backdrop{padding:0;align-items:flex-end}.composer-panel{max-height:94dvh;border-radius:26px 26px 0 0;padding:24px 20px;padding-bottom:max(20px,env(safe-area-inset-bottom))}}
</style>

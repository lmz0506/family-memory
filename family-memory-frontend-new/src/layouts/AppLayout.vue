<script setup lang="ts">
import {computed,nextTick,onBeforeUnmount,onMounted,ref,watch} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {Archive,CalendarDays,ChevronDown,Images,LockKeyhole,LogOut,Orbit,Plus,Sparkles,Users} from 'lucide-vue-next'
import {useAuthStore} from '@/stores/auth'
import {useFamilyStore} from '@/stores/family'
import ComposerModal from '@/components/ComposerModal.vue'
import CapsuleComposerModal from '@/components/CapsuleComposerModal.vue'
import {skyEditorOpen} from '@/composables/useSkyEditor'
import {invalidateViewData} from '@/composables/viewState'
import {notify} from '@/composables/useToast'
import '@/styles/star-home.css'
import '@/styles/universe-workspace.css'

const route=useRoute(),router=useRouter(),auth=useAuthStore(),family=useFamilyStore()
const composer=ref(false),capsuleComposer=ref(false),accountOpen=ref(false),familyError=ref('')
const menuTrigger=ref<HTMLButtonElement>()
async function openMenu(){accountOpen.value=true;await nextTick();document.querySelector<HTMLAnchorElement>('#sky-page-menu a')?.focus()}
function menuNavigate(event:KeyboardEvent){if(!['ArrowDown','ArrowUp','Home','End'].includes(event.key))return;event.preventDefault();const items=Array.from(document.querySelectorAll<HTMLElement>('#sky-page-menu a,#sky-page-menu button')),index=items.indexOf(document.activeElement as HTMLElement),next=event.key==='Home'?0:event.key==='End'?items.length-1:(index+(event.key==='ArrowDown'?1:-1)+items.length)%items.length;items[next]?.focus()}
const nav=[{name:'home',label:'星海',icon:Orbit},{name:'album',label:'回忆',icon:Images},{name:'vault',label:'珍藏',icon:LockKeyhole},{name:'family',label:'家人',icon:Users}]
const starHome=computed(()=>route.name==='home')
watch([starHome,composer,capsuleComposer],()=>{skyEditorOpen.value=composer.value||capsuleComposer.value},{immediate:true})
onBeforeUnmount(()=>{skyEditorOpen.value=false})
const activeName=computed(()=>route.name==='memory-detail'?(route.query.from==='home'?'home':'album'):route.name==='capsule-detail'?(route.query.from==='home'||route.query.from==='universe'?'home':'vault'):route.name)
function openComposer(){capsuleComposer.value=false;composer.value=true}
function openCapsule(){composer.value=false;capsuleComposer.value=true}
function logout(){auth.logout();router.replace('/login')}
function keydown(event:KeyboardEvent){if(event.key==='Escape'&&accountOpen.value){accountOpen.value=false;nextTick(()=>menuTrigger.value?.focus());event.preventDefault();event.stopImmediatePropagation()}}
async function loadFamily(){
  familyError.value=''
  try{await family.load();if(!family.current&&route.name!=='welcome')await router.replace('/welcome')}
  catch(e){familyError.value=(e as Error).message}
}
async function handleCreated(kind:'memory'|'capsule'){
  invalidateViewData(kind==='memory'?'album:':'vault:')
  window.dispatchEvent(new Event(kind==='memory'?'family-memory-created':'family-capsule-created'))
  try{await family.refresh()}catch{notify('内容已保存，家庭概览暂未更新','info')}
}
watch(()=>route.fullPath,()=>{accountOpen.value=false},{immediate:true})
onMounted(()=>{window.addEventListener('family-memory-compose',openComposer);window.addEventListener('family-capsule-compose',openCapsule);document.addEventListener('keydown',keydown,true);void loadFamily()})
onBeforeUnmount(()=>{window.removeEventListener('family-memory-compose',openComposer);window.removeEventListener('family-capsule-compose',openCapsule);document.removeEventListener('keydown',keydown,true)})
</script>
<template>
  <div class="fm-shell" :class="{'fm-shell-star':starHome,'fm-shell-content':!starHome}">
    <a href="#main-content" class="fm-skip">跳到主要内容</a>
    <div class="sky-app-menu">
      <button ref="menuTrigger" class="sky-menu-trigger" @keydown.down.prevent="openMenu" aria-label="拾光页面菜单" :aria-expanded="accountOpen" aria-controls="sky-page-menu" @click="accountOpen=!accountOpen"><Sparkles :size="23"/><span><b>拾光</b><small>{{family.current?.name||'一家人的星海'}}</small></span><ChevronDown :size="14"/></button>
      <template v-if="accountOpen"><button class="fm-account-dismiss" tabindex="-1" aria-label="关闭页面菜单" @click="accountOpen=false"></button><nav id="sky-page-menu" class="sky-page-menu" @keydown="menuNavigate" aria-label="页面与账号"><p>翻开我们的小世界</p><RouterLink v-for="item in nav" :key="item.name" :to="{name:item.name}" :aria-current="activeName===item.name?'page':undefined" @click="accountOpen=false"><component :is="item.icon" :size="17"/>{{item.label}}</RouterLink><div class="sky-menu-create"><button @click="accountOpen=false;openComposer()"><Plus :size="16"/>记录一段记忆</button><button @click="accountOpen=false;openCapsule()"><Archive :size="16"/>封存一份珍藏</button></div><div class="sky-menu-account"><span>{{auth.user?.nickname||'家庭成员'}}</span><button @click="logout"><LogOut :size="15"/>退出登录</button></div></nav></template>
    </div>
    <main id="main-content" data-app-scroll class="fm-content" :class="{'fm-content-sky':starHome,'sky-content':!starHome}" tabindex="-1">
      <div v-if="familyError" class="fm-family-error" role="alert"><span>{{familyError}}</span><button @click="loadFamily">重新加载家庭</button></div>
      <RouterView/>
    </main>
    <ComposerModal v-if="family.current" :key="`memory-${family.current.id}`" v-model="composer" :family-id="family.current.id" :embedded="true" @created="handleCreated('memory')"/>
    <CapsuleComposerModal v-if="family.current" :key="`capsule-${family.current.id}`" v-model="capsuleComposer" :family-id="family.current.id" :embedded="true" @created="handleCreated('capsule')"/>
  </div>
</template>

<style scoped>
.fm-shell-content{background:var(--sky-ambient-base)}.fm-shell-content>.sky-app-menu{position:absolute;top:20px;left:32px}.fm-shell-content .fm-content{padding:100px 32px 36px;background:var(--sky-ambient);background-color:var(--sky-ambient-base)}.fm-shell-content .sky-app-menu::before{content:"";position:fixed;left:0;right:0;top:0;height:84px;pointer-events:none;background:linear-gradient(#29252df2 25%,#24232cbb 65%,transparent);z-index:-1}.fm-shell-content .sky-menu-trigger{position:relative}.fm-shell-content .sky-page-menu{z-index:60}.fm-shell-content .fm-family-error{background:var(--ux-surface);color:var(--ux-ink-2)}
@media(max-width:767px){.fm-shell-content>.sky-app-menu{left:19px;top:18px}.fm-shell-content .fm-content{padding:94px 16px calc(24px + env(safe-area-inset-bottom))}}
</style>

<!-- Lucian UX/UI Prompt #037 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#037 -->
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
import '@/styles/lucian-037.css'
import '@/styles/star-home.css'
import '@/styles/universe-workspace.css'
import '@/styles/constellation-pages.css'
import StarField from '@/components/StarField.vue'

const route=useRoute(),router=useRouter(),auth=useAuthStore(),family=useFamilyStore()
const composer=ref(false),capsuleComposer=ref(false),accountOpen=ref(false),familyError=ref('')
const menuTrigger=ref<HTMLButtonElement>()
function highlightMenu(event:Event){const item=(event.target as HTMLElement).closest<HTMLElement>('.u037-it');if(!item)return;const list=item.closest<HTMLElement>('.u037-list');if(!list)return;list.style.setProperty('--y',`${item.parentElement!.offsetTop}px`);list.style.setProperty('--h',`${item.offsetHeight}px`);list.classList.add('is-hl')}
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
    <StarField v-if="!starHome"/>

    <a href="#main-content" class="fm-skip">跳到主要内容</a>
    <div class="sky-app-menu ux u037" :class="{'is-open':accountOpen}">
      <button ref="menuTrigger" class="u037-chip sky-menu-trigger" @keydown.down.prevent="openMenu" @keydown.up.prevent="openMenu" aria-label="拾光页面菜单" :aria-expanded="accountOpen" aria-controls="sky-page-menu" @click="accountOpen=!accountOpen"><span class="u037-ws menu-brand-star" aria-hidden="true"><Sparkles :size="24"/></span><span><b>拾光 <em>记忆星海</em></b><small>{{family.current?.name||'一家人的星海'}} <i>·</i> {{nav.find(item=>item.name===activeName)?.label||'星海'}}</small></span><ChevronDown class="u037-car" :size="17" aria-hidden="true"/></button>
      <button v-if="accountOpen" class="fm-account-dismiss" tabindex="-1" aria-label="关闭页面菜单" @click="accountOpen=false"></button>
      <nav id="sky-page-menu" class="u037-menu" :inert="!accountOpen" :aria-hidden="!accountOpen" @keydown="menuNavigate" aria-label="页面与账号">
        <p class="u037-sec">OUR LITTLE UNIVERSE <span>我们的小世界</span></p>
        <ul class="u037-list" @pointerover="highlightMenu" @focusin="highlightMenu" @pointerleave="($event.currentTarget as HTMLElement).classList.remove('is-hl')"><li class="u037-hl" aria-hidden="true"></li><li v-for="(item,index) in nav" :key="item.name"><RouterLink class="u037-it" :style="{'--i':index}" :to="{name:item.name}" :aria-current="activeName===item.name?'page':undefined" @click="accountOpen=false"><span class="u037-ws" :class="`nav-${item.name}`"><component :is="item.icon" :size="18" aria-hidden="true"/></span><span>{{item.label}}<small>{{['The universe','Our memories','Time capsules','Constellation'][index]}}</small></span><span v-if="activeName===item.name" class="menu-current-star" aria-hidden="true">✦</span></RouterLink></li></ul>
        <div class="u037-sep"></div>
        <ul class="u037-list menu-create-list" @pointerover="highlightMenu" @focusin="highlightMenu"><li class="u037-hl" aria-hidden="true"></li><li><button class="u037-it" style="--i:4" @click="accountOpen=false;openComposer()"><span class="u037-ico"><Plus :size="18" aria-hidden="true"/></span>记录一段记忆<span class="menu-item-hint">点亮</span></button></li><li><button class="u037-it" style="--i:5" @click="accountOpen=false;openCapsule()"><span class="u037-ico"><Archive :size="18" aria-hidden="true"/></span>封存一份珍藏<span class="menu-item-hint">寄往未来</span></button></li></ul>
        <div class="u037-sep"></div><div class="menu-account-row"><span class="menu-account-avatar">{{(auth.user?.nickname||'家').slice(0,1)}}</span><span>{{auth.user?.nickname||'家庭成员'}}<small>星海记录者</small></span><button @click="logout" aria-label="退出登录"><LogOut :size="16" aria-hidden="true"/></button></div>
      </nav>
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
.fm-shell-content{background:var(--sky-ambient-base)}.fm-shell-content .fm-family-error{background:var(--ux-surface);color:var(--ux-ink-2)}
.sky-app-menu.u037{position:absolute;top:20px;left:28px;width:320px;max-width:calc(100vw - 36px);height:68px;pointer-events:none;z-index:55;--ux-surface:#1a2232;--ux-surface-2:#d5bb8f10;--ux-ink:#f0dfc7;--ux-ink-3:#a8a9b7;--ux-line:#dac6a329;--ux-line-2:#dac6a355;--ux-accent:#e7c79e}
.sky-app-menu.u037 .u037-chip{position:absolute;top:0;bottom:auto;z-index:2;pointer-events:auto;padding:0 16px 0 10px;background:#152030a6;backdrop-filter:blur(18px);gap:12px;min-height:68px;box-shadow:0 0 0 1px #e7c99b18;letter-spacing:0}
.u037 .menu-brand-star{background:linear-gradient(145deg,#e3bc831a,#e3bc8304);border:1px solid #dbc19930;color:#eccb9d;box-shadow:inset 0 0 13px #f1c79609}
.u037 .u037-chip b{font-size:22px;font-weight:550;letter-spacing:2px;line-height:1.3}.u037-chip b em{font-size:9px;font-style:normal;color:#ac9f90;letter-spacing:2px;margin-left:10px;font-weight:400}.u037 .u037-chip small{font-size:10px;color:#ada9a6;max-width:205px;margin-top:4px}.u037-chip small i{font-style:normal;padding:0 6px;opacity:.4}.u037 .u037-car{margin-left:auto;color:#b4aa9a;flex-shrink:0}
.sky-app-menu.u037 .u037-menu{top:84px;bottom:auto;z-index:3;pointer-events:auto;max-height:calc(100dvh - 125px);overflow-y:auto;overscroll-behavior:contain;transform-origin:30px 0;backdrop-filter:blur(24px);background:linear-gradient(145deg,#212735f8,#131d2cf8)}
.u037 .u037-sec{font-size:9px;font-weight:400;letter-spacing:1.5px;display:flex;justify-content:space-between;margin:13px 12px 14px}.u037-sec span{font-size:10px;letter-spacing:0;color:#bba78c}.u037 .u037-list{list-style:none}.u037 .u037-it{font-size:14px;font-weight:450;text-decoration:none}.u037 .u037-it small{font-size:9px;font-weight:400;letter-spacing:.6px;margin-top:3px}.u037 .u037-it .u037-ws{background:#b9c3de0a;border:1px solid #b9c3de16;color:#b0bbd1}.u037 .u037-it[aria-current=page]{color:#efd1a7}.u037 .u037-it[aria-current=page] .u037-ws{color:#e8c69b;background:#e8c69b14;border-color:#e8c69b2b}.menu-current-star{margin-left:auto;color:#e1c199;font-size:13px}.menu-item-hint{font-size:10px;color:#9f9484;margin-left:auto}.u037 .menu-create-list .u037-it{font-size:12px}.menu-account-row{display:flex;align-items:center;gap:11px;padding:11px 12px 7px;color:#d5c3aa;font-size:12px}.menu-account-avatar{width:32px;height:32px;border-radius:50%;display:grid;place-items:center;border:1px solid #e6c99b38;background:#e6c99b0c}.menu-account-row small{display:block;font-size:9px;color:#9497a4;margin-top:3px}.menu-account-row button{margin-left:auto;display:grid;place-items:center;min-width:36px;min-height:36px;border-radius:50%;color:#a7a5aa}.menu-account-row button:hover{background:#ffffff09;color:#f0caa0}.sky-app-menu .fm-account-dismiss{z-index:1;pointer-events:auto}.u037 :is(a,button):focus-visible{outline:2px solid #e6c99b;outline-offset:-2px}
@media(max-width:639px){.sky-app-menu.u037{top:16px;left:18px;width:calc(100vw - 36px);max-width:340px}.u037 .u037-chip b{font-size:21px}.sky-app-menu.u037 .u037-menu{top:80px}.sky-app-menu.u037 .u037-chip{backdrop-filter:blur(18px)}}
</style>

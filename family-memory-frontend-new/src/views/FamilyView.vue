<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import {
  CalendarDays, ChevronRight, Copy, Gift, Home, LockKeyhole, MapPin, Pencil,
  Plus, Repeat2, Trash2, UserPlus, Users, X
} from 'lucide-vue-next'
import CountUp from '@/components/CountUp.vue'
import SkyDrawer from '@/components/SkyDrawer.vue'
import {memoryApi,capsuleApi,familyApi, milestoneApi} from '@/api'
import {useDialog} from '@/composables/useDialog'
import {notify} from '@/composables/useToast'
import SkyPageHeader from '@/components/SkyPageHeader.vue'
import StarChart from '@/components/StarChart.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import {useAuthStore} from '@/stores/auth'
import {useFamilyStore} from '@/stores/family'
import type {FamilyMilestone, FamilyMilestonePayload, MilestoneVisibility} from '@/types'

const auth = useAuthStore()
const family = useFamilyStore()
const invite = ref<{code: string; expireAt: string} | null>(null)
const inviteError = ref('')
const copied = ref(false)
const milestones = ref<FamilyMilestone[]>([])
const milestoneLoading = ref(false),milestoneError=ref('')
const milestoneModal = ref(false)
const editing = ref<FamilyMilestone | null>(null)
const saving = ref(false)
const inviteBusy = ref(false)
const invitePanel = ref<HTMLElement>()
const milestonePanel = ref<HTMLElement>()
const inviteOpen = computed(()=>Boolean(invite.value))
const deleting = ref<FamilyMilestone|null>(null)
const deleteBusy = ref(false)
useDialog(inviteOpen,invitePanel,()=>invite.value=null)
useDialog(milestoneModal,milestonePanel,()=>{if(!saving.value)milestoneModal.value=false})
const formError = ref('')
const form = reactive<FamilyMilestonePayload>({
  title: '', description: '', eventDate: '', recurringYearly: false,
  visibilityScope: 'PRIVATE', viewerUserIds: []
})

type DataKind='members'|'memories'|'capsules'
const dataKind=ref<DataKind|null>(null),dataLoading=ref(false),dataError=ref(''),dataRows=ref<{id:number;title:string;detail:string;to:string}[]>([])
let dataVersion=0
const dataOpen=computed({get:()=>dataKind.value!==null,set:value=>{if(!value){dataKind.value=null;dataVersion++}}})
const dataTitle=computed(()=>dataKind.value==='members'?'我们的小家':dataKind.value==='memories'?'家人的回忆':'交给时间的珍藏')
async function openFamilyData(kind:DataKind){
 if(!family.current)return
 const version=++dataVersion;dataKind.value=kind;dataError.value='';dataRows.value=[];dataLoading.value=true
 try{
  if(kind==='members')dataRows.value=family.members.map(m=>({id:m.userId,title:m.displayName,detail:`${m.role||'家庭成员'} · 留下 ${m.memoryCount} 段回忆`,to:''}))
  else if(kind==='memories'){const result=await memoryApi.list(family.current.id,{page:1,size:6});if(version!==dataVersion)return;dataRows.value=result.list.map(m=>({id:m.id,title:m.title,detail:`${m.memoryDate} · ${m.authorName}`,to:`/memories/${m.id}?from=family`}))}
  else{const result=await capsuleApi.list(family.current.id);if(version!==dataVersion)return;dataRows.value=result.slice(0,6).map(c=>({id:c.id,title:c.title,detail:`${c.openDate} · ${c.opened?'已可阅读':'封存中'}`,to:`/capsules/${c.id}?from=family`}))}
 }catch(e){if(version===dataVersion)dataError.value=(e as Error).message}finally{if(version===dataVersion)dataLoading.value=false}
}
const memberReplay=ref<Record<number,number>>({}),memberWarm=new Map<number,number>()
function warmMember(id:number){const now=performance.now();if(now-(memberWarm.get(id)??-Infinity)<3000)return;memberWarm.set(id,now);memberReplay.value={...memberReplay.value,[id]:(memberReplay.value[id]||0)+1}}
function milestonePreset(title:string){openCreateMilestone();form.title=title;form.recurringYearly=true}
onBeforeUnmount(()=>{dataVersion++})
const selectableMembers = computed(() => family.members.filter(member => member.userId !== auth.user?.id))

onMounted(async () => {
  if (!auth.user) await auth.loadProfile()
  if (!family.current) await family.load()
  else await family.refresh()
  await loadMilestones()
})

async function loadMilestones() {
  if (!family.current) return
  milestoneLoading.value = true
  milestoneError.value=''
  try { milestones.value = await milestoneApi.list(family.current.id) }catch(e){milestoneError.value=(e as Error).message}
  finally { milestoneLoading.value = false }
}

async function createInvite() {
  if (!family.current) return
  if(inviteBusy.value)return
  inviteBusy.value=true
  inviteError.value = ''
  try { invite.value = await familyApi.invite(family.current.id) }
  catch (e) { inviteError.value = (e as Error).message;notify(inviteError.value,'error') }
  finally {inviteBusy.value=false}
}

async function copyInvite() {
  if (!invite.value) return
  try{await navigator.clipboard.writeText(invite.value.code);copied.value=true;notify('邀请码已复制')}
  catch{notify('浏览器无法自动复制，请长按邀请码手动复制','info')}
}

function todayText() {
  const date = new Date()
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 10)
}

function openCreateMilestone() {
  editing.value = null
  Object.assign(form, {
    title: '', description: '', eventDate: todayText(), recurringYearly: false,
    visibilityScope: 'PRIVATE' as MilestoneVisibility, viewerUserIds: []
  })
  formError.value = ''
  milestoneModal.value = true
}

function openEditMilestone(item: FamilyMilestone) {
  editing.value = item
  Object.assign(form, {
    title: item.title,
    description: item.description || '',
    eventDate: item.eventDate,
    recurringYearly: item.recurringYearly,
    visibilityScope: item.visibilityScope,
    viewerUserIds: item.visibleTo.map(viewer => viewer.userId)
  })
  formError.value = ''
  milestoneModal.value = true
}

function setVisibility(scope: MilestoneVisibility) {
  form.visibilityScope = scope
  if (scope !== 'SPECIFIC') form.viewerUserIds = []
}

function toggleViewer(userId: number) {
  form.viewerUserIds = form.viewerUserIds.includes(userId)
    ? form.viewerUserIds.filter(id => id !== userId)
    : [...form.viewerUserIds, userId]
}

async function saveMilestone() {
  if (!family.current) return
  formError.value = ''
  if (form.visibilityScope === 'SPECIFIC' && form.viewerUserIds.length === 0) {
    formError.value = '请至少选择一位可见家人'
    return
  }
  saving.value = true
  try {
    const payload: FamilyMilestonePayload = {...form, viewerUserIds: [...form.viewerUserIds]}
    if (editing.value) await milestoneApi.update(editing.value.id, payload)
    else await milestoneApi.create(family.current.id, payload)
    milestoneModal.value = false
    notify(editing.value?'提醒已更新':'重要的日子，已经记下')
    await loadMilestones()
  } catch (e) {
    formError.value = (e as Error).message
  } finally {
    saving.value = false
  }
}

async function removeMilestone(item: FamilyMilestone) {
  if(deleteBusy.value)return
  deleteBusy.value=true
  try {
    await milestoneApi.remove(item.id)
    deleting.value=null
    notify('提醒已删除')
    await loadMilestones()
  } catch (e) {
    notify((e as Error).message,'error')
  }finally{deleteBusy.value=false}
}

function dateParts(value: string) {
  const [, month, day] = value.split('-').map(Number)
  return {month, day}
}

function remainingText(item: FamilyMilestone) {
  if (item.expired) return '已过期'
  if (item.daysRemaining === 0) return '就是今天'
  return `还有 ${item.daysRemaining} 天`
}

function visibilityText(item: FamilyMilestone) {
  if (item.visibilityScope === 'PRIVATE') return '仅自己可见'
  if (item.visibilityScope === 'FAMILY') return '全家可见'
  return item.visibleTo.length ? `与 ${item.visibleTo.map(it => it.displayName).join('、')} 共享` : '指定家人可见'
}
</script>

<template>
  <section class="family-page page-reveal space-y-6 pb-8">
    <div class="family-hero" :class="{'family-hero-photo':family.current?.coverUrl}"><img v-if="family.current?.coverUrl" :src="family.current.coverUrl" :alt="`${family.current.name}的家庭封面`" class="family-hero-image"/><StarChart variant="family" class="family-star-chart"/><div class="family-hero-content"><p class="constellation-eyebrow">OUR LITTLE UNIVERSE / 家人的星座</p><SkyPageHeader :title="family.current?.name||'家人'" :description="family.current?.description||'每个人都是一颗星，因为彼此，才有了家的星座。'"><button class="sky-primary" :disabled="inviteBusy" @click="createInvite"><UserPlus :size="17"/>邀请家人</button></SkyPageHeader><div class="family-summary"><button @click="openFamilyData('members')" aria-label="查看家人数据"><strong>{{family.current?.memberCount||family.members.length}}</strong><span>位家人</span><ChevronRight :size="14"/></button><button @click="openFamilyData('memories')" aria-label="查看回忆数据"><strong>{{family.current?.memoryCount||0}}</strong><span>段回忆</span><ChevronRight :size="14"/></button><button @click="openFamilyData('capsules')" aria-label="查看珍藏数据"><strong>{{family.current?.capsuleCount||0}}</strong><span>份珍藏</span><ChevronRight :size="14"/></button><p>已经一起记录 {{family.current?.recordDays||0}} 天</p></div></div></div>
    <div class="family-workspace">
      <section class="space-y-4"><div class="flex items-center justify-between"><h2 class="font-medium text-xl text-night-ink">家人们</h2><span class="text-xs text-night-muted">{{family.members.length}} 位成员</span></div><div class="family-members">
        <article v-for="member in family.members" :key="member.id" class="sky-panel family-member" @pointerenter="warmMember(member.id)" @focusin="warmMember(member.id)"><div class="flex items-center gap-3"><div class="family-avatar"><img v-if="member.avatar" :src="member.avatar" :alt="member.displayName"/><span v-else>{{member.displayName.slice(0,1)}}</span></div><div class="min-w-0"><h3 class="text-sm text-night-ink">{{member.displayName}} <small v-if="member.admin" class="family-admin">管理员</small></h3><p class="mt-1 text-xs text-night-muted">{{member.role||'家庭成员'}}</p></div></div><p class="mt-5 text-xs text-night-secondary">累计留下 <CountUp :value="member.memoryCount" :replay="memberReplay[member.id]||0"/> 段回忆</p><RouterLink :to="{name:'album',query:{view:'timeline'}}" class="mt-4 inline-flex items-center gap-1 text-xs text-night-accent">翻阅家庭记录<ChevronRight :size="14"/></RouterLink></article>
      </div><div class="family-footprint"><MapPin :size="16"/><div><strong>家庭足迹</strong><p>地点地图尚未开放，未来在这里一起回望走过的路。</p></div></div></section>
      <section class="sky-panel family-milestones"><header class="flex items-center justify-between gap-3"><div><h2 class="font-medium text-xl text-night-ink">重要的日子</h2><p class="mt-2 text-xs text-night-muted">记住生日、约定与家人的小事</p></div><button class="sky-secondary" aria-label="添加纪念日提醒" @click="openCreateMilestone"><Plus :size="17"/></button></header>
        <div v-if="milestoneLoading" class="py-10 text-center text-sm text-night-muted">正在整理重要的日子…</div><div v-else-if="milestoneError" class="empty-state" role="alert"><p>{{milestoneError}}</p><button class="sky-secondary" @click="loadMilestones">重试</button></div><div v-else-if="!milestones.length" class="milestone-empty"><CalendarDays :size="21"/><p>还没有纪念日提醒</p><small>从一个生日，或一个共同的约定开始。</small><div><button @click="milestonePreset('生日')"><Plus :size="13"/>添加生日</button><button @click="milestonePreset('纪念日')"><Plus :size="13"/>添加纪念日</button></div></div>
        <div v-else class="milestone-list"><article v-for="item in milestones" :key="item.id" class="milestone-row"><div class="milestone-date"><strong>{{dateParts(item.nextEventDate).day}}</strong><span>{{dateParts(item.nextEventDate).month}}月</span></div><div class="min-w-0 flex-1"><h3 class="text-sm text-night-ink">{{item.title}}</h3><p v-if="item.description" class="mt-1 text-xs text-night-secondary">{{item.description}}</p><p class="mt-2 text-xs text-night-muted">{{item.recurringYearly?'每年提醒 · ':''}}{{visibilityText(item)}}</p><span class="mt-2 inline-block text-xs text-night-accent">{{remainingText(item)}}</span></div><div v-if="item.canManage" class="milestone-actions"><button aria-label="编辑提醒" @click="openEditMilestone(item)"><Pencil :size="15"/></button><button aria-label="删除提醒" @click="deleting=item"><Trash2 :size="15"/></button></div></article></div>
      </section>
    </div>
  </section>

  <SkyDrawer v-model="dataOpen" :title="dataTitle" :description="dataKind==='memories'||dataKind==='capsules'?'先翻看最近的六份，完整内容可继续前往对应页面。':'每一位家人，都在留下自己的故事。'"><p v-if="dataLoading" class="family-data-status">正在整理…</p><div v-else-if="dataError" class="family-data-status" role="alert"><p>{{dataError}}</p><button class="sky-secondary" @click="dataKind&&openFamilyData(dataKind)">重试</button></div><div v-else-if="!dataRows.length" class="family-data-status">这里还在等待第一份记录。</div><template v-else><div v-for="row in dataRows" :key="row.id" class="family-data-row"><RouterLink v-if="row.to" :to="row.to" @click="dataOpen=false"><strong>{{row.title}}</strong><small>{{row.detail}}</small><ChevronRight :size="16"/></RouterLink><div v-else><strong>{{row.title}}</strong><small>{{row.detail}}</small></div></div></template><RouterLink v-if="dataKind==='memories'||dataKind==='capsules'" :to="{name:dataKind==='memories'?'album':'vault'}" class="sky-secondary family-data-more" @click="dataOpen=false">查看全部{{dataKind==='memories'?'回忆':'珍藏'}}<ChevronRight :size="14"/></RouterLink></SkyDrawer>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="invite" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm" @click.self="invite = null">
        <section ref="invitePanel" data-sky-dialog role="dialog" aria-modal="true" aria-label="邀请家人加入" tabindex="-1" class="sky-dialog w-full max-w-md space-y-5 rounded-2xl border border-night-line max-h-[90dvh] overflow-y-auto bg-night-surface p-6 text-center shadow-2xl">
          <div class="flex items-center justify-between border-b border-night-line pb-3"><div class="flex items-center gap-2"><div class="rounded-xl bg-night-accent/10 p-2 text-night-accent"><UserPlus :size="16"> </UserPlus></div><h2 class="text-base font-bold text-night-ink">邀请家人加入</h2></div><button @click="invite = null" class="rounded-lg p-1 text-night-muted hover:text-night-ink"><X :size="20"> </X></button></div>
          <p class="text-xs leading-6 text-night-secondary">复制下方专属邀请码发给家人，即可共同记录与查阅家庭照片。</p>
          <div class="rounded-2xl bg-night-soft p-5"><p class="text-xs font-semibold text-night-muted">家庭专属邀请码</p><div class="mt-2 font-sans text-3xl font-bold tracking-[.18em] text-night-ink">{{ invite.code }}</div><p class="mt-2 text-xs text-night-muted">有效期至 {{ invite.expireAt }}</p></div>
          <button @click="copyInvite" class="sky-primary flex w-full items-center justify-center gap-2 rounded-xl bg-night-action py-3 text-xs font-bold text-night-action-ink transition hover:bg-night-action"><Copy :size="16"> </Copy>{{ copied ? '已复制' : '复制邀请码' }}</button>
          <p v-if="inviteError" class="text-xs text-night-danger">{{ inviteError }}</p>
        </section>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="milestoneModal" class="fixed inset-0 z-[60] flex items-end justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm sm:items-center" @click.self="milestoneModal = false">
        <form ref="milestonePanel" data-sky-dialog role="dialog" aria-modal="true" aria-label="纪念日提醒" tabindex="-1" @submit.prevent="saveMilestone" class="sky-dialog my-auto w-full max-w-lg space-y-5 rounded-t-3xl border border-night-line max-h-[90dvh] overflow-y-auto bg-night-surface p-6 shadow-2xl sm:rounded-2xl">
          <div class="flex items-center justify-between border-b border-night-line pb-3"><div class="flex items-center gap-2"><div class="rounded-xl bg-night-accent/10 p-2 text-night-accent"><Gift :size="16"> </Gift></div><div><h2 class="text-base font-bold text-night-ink">{{ editing ? '编辑大事记提醒' : '添加大事记提醒' }}</h2><p class="mt-0.5 text-xs text-night-muted">默认仅自己可见，可按需分享给家人</p></div></div><button type="button" @click="milestoneModal = false" class="rounded-lg p-1 text-night-muted hover:text-night-ink"><X :size="20"> </X></button></div>

          <div class="space-y-4">
            <label class="block"><span class="mb-1.5 block text-xs font-semibold text-night-secondary">名称</span><input v-model.trim="form.title" required maxlength="120" placeholder="例如：体检、生日、家庭纪念日" class="w-full rounded-xl border border-night-line bg-night-soft/40 px-3.5 py-2.5 text-sm text-night-ink outline-none transition focus:border-night-accent  "></label>
            <div class="grid gap-4 sm:grid-cols-2">
              <label class="block"><span class="mb-1.5 block text-xs font-semibold text-night-secondary">日期</span><input v-model="form.eventDate" type="date" required :min="form.recurringYearly ? undefined : todayText()" class="w-full rounded-xl border border-night-line bg-night-soft/40 px-3.5 py-2.5 text-sm text-night-ink outline-none transition focus:border-night-accent  "></label>
              <label class="flex cursor-pointer items-center gap-3 self-end rounded-xl border border-night-line bg-night-soft/40 px-3.5 py-2.5"><input v-model="form.recurringYearly" type="checkbox" class="h-4 w-4 accent-night-accent"><span><span class="block text-xs font-semibold text-night-secondary">每年重复提醒</span><span class="block text-xs text-night-muted">适合生日与周年纪念</span></span></label>
            </div>
            <label class="block"><span class="mb-1.5 block text-xs font-semibold text-night-secondary">备注（选填）</span><textarea v-model.trim="form.description" maxlength="500" rows="2" placeholder="写下一句提醒" class="w-full resize-none rounded-xl border border-night-line bg-night-soft/40 px-3.5 py-2.5 text-sm text-night-ink outline-none transition focus:border-night-accent  "></textarea></label>

            <fieldset>
              <legend class="mb-2 block text-xs font-semibold text-night-secondary">谁可以看到</legend>
              <div class="grid grid-cols-3 gap-2">
                <button type="button" @click="setVisibility('PRIVATE')" class="rounded-xl border px-2 py-3 text-center transition" :class="form.visibilityScope === 'PRIVATE' ? 'border-night-accent bg-night-accent/10 text-night-accent' : 'border-night-line text-night-secondary hover:bg-night-soft'"><LockKeyhole class="mx-auto mb-1" :size="17"> </LockKeyhole><span class="block text-xs font-bold">仅自己</span><span class="mt-0.5 block text-xs opacity-75">默认</span></button>
                <button type="button" @click="setVisibility('FAMILY')" class="rounded-xl border px-2 py-3 text-center transition" :class="form.visibilityScope === 'FAMILY' ? 'border-night-accent bg-night-accent/10 text-night-accent' : 'border-night-line text-night-secondary hover:bg-night-soft'"><Home class="mx-auto mb-1" :size="17"> </Home><span class="block text-xs font-bold">全家可见</span><span class="mt-0.5 block text-xs opacity-75">所有成员</span></button>
                <button type="button" @click="setVisibility('SPECIFIC')" class="rounded-xl border px-2 py-3 text-center transition" :class="form.visibilityScope === 'SPECIFIC' ? 'border-night-accent bg-night-accent/10 text-night-accent' : 'border-night-line text-night-secondary hover:bg-night-soft'"><Users class="mx-auto mb-1" :size="17"> </Users><span class="block text-xs font-bold">指定家人</span><span class="mt-0.5 block text-xs opacity-75">自由选择</span></button>
              </div>
            </fieldset>

            <div v-if="form.visibilityScope === 'SPECIFIC'" class="rounded-2xl border border-night-line bg-night-soft/60 p-3">
              <p class="mb-2 text-xs text-night-secondary">请选择可以看到这条提醒的家人（你自己始终可见）</p>
              <div v-if="selectableMembers.length" class="flex flex-wrap gap-2">
                <button v-for="member in selectableMembers" :key="member.userId" type="button" @click="toggleViewer(member.userId)" class="flex items-center gap-1.5 rounded-full border px-2.5 py-1.5 text-xs font-semibold transition" :class="form.viewerUserIds.includes(member.userId) ? 'border-night-accent bg-night-action text-night-action-ink' : 'border-night-line bg-night-surface text-night-secondary hover:border-night-accent/50'">
                  <span class="grid h-5 w-5 place-items-center overflow-hidden rounded-full bg-night-accent/10 text-xs text-night-accent"><img v-if="member.avatar" :src="member.avatar" :alt="member.displayName" class="h-full w-full object-cover"><span v-else>{{ member.displayName.slice(0, 1) }}</span></span>{{ member.displayName }}
                </button>
              </div>
              <p v-else class="text-xs text-night-muted">暂无其他家庭成员，请先邀请家人加入。</p>
            </div>
          </div>

          <p v-if="formError" class="rounded-xl bg-night-danger/10 px-3 py-2 text-xs text-night-danger">{{ formError }}</p>
          <div class="flex gap-3"><button type="button" @click="milestoneModal = false" class="flex-1 rounded-xl border border-night-line py-2.5 text-xs font-semibold text-night-secondary hover:bg-night-soft">取消</button><button type="submit" :disabled="saving" class="sky-primary flex-1 rounded-xl bg-night-action py-2.5 text-xs font-bold text-night-action-ink shadow-glow transition hover:bg-night-action disabled:opacity-60">{{ saving ? '保存中…' : (editing ? '保存修改' : '添加提醒') }}</button></div>
        </form>
      </div>
    </Transition>
  </Teleport>
  <ConfirmDialog :open="Boolean(deleting)" :title="`删除${deleting?.title || '提醒'}？`" description="删除后将不再提醒家人，这个操作无法撤销。" :busy="deleteBusy" @update:open="deleting=null" @confirm="deleting && removeMilestone(deleting)"/>
</template>

<style scoped>
.family-cover{width:100%;height:150px;object-fit:cover;border-radius:14px}
.family-summary{display:flex;align-items:center;gap:32px;padding:0 0 23px;border-bottom:1px solid var(--ux-line)}.family-summary>div{display:flex;align-items:baseline;gap:8px}.family-summary strong{font-size:24px;font-weight:500;color:var(--ux-ink)}.family-summary span,.family-summary>p{font-size:12px;color:var(--ux-ink-2)}.family-summary>p{margin-left:auto}.family-workspace{display:grid;grid-template-columns:minmax(0,1.3fr) minmax(330px,1fr);gap:28px;align-items:start}.family-members{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.family-member{padding:22px}.family-avatar{display:grid;place-items:center;width:46px;height:46px;border-radius:13px;background:var(--ux-accent-soft);color:var(--ux-accent);overflow:hidden;flex-shrink:0}.family-avatar img{width:100%;height:100%;object-fit:cover}.family-admin{display:inline-block;margin-left:5px;font-size:10px;padding:2px 5px;border:1px solid var(--ux-line-2);border-radius:5px;color:var(--ux-accent)}.family-footprint{display:flex;align-items:flex-start;gap:12px;padding:20px 0;color:var(--ux-ink-3)}.family-footprint strong{font-size:13px;font-weight:400}.family-footprint p{font-size:12px;line-height:1.8;margin-top:6px}.family-milestones{padding:23px}.milestone-list{margin-top:20px}.milestone-row{display:flex;gap:14px;align-items:flex-start;padding:18px 0;border-top:1px solid var(--ux-line)}.milestone-date{width:42px;flex-shrink:0;text-align:center;color:var(--ux-accent)}.milestone-date strong{display:block;font-size:23px;font-weight:450}.milestone-date span{font-size:11px}.milestone-actions{display:flex;flex-direction:column;gap:3px}.milestone-actions button{display:grid;place-items:center;width:34px;height:34px;color:var(--ux-ink-3);border-radius:8px}.milestone-actions button:hover{color:var(--ux-accent);background:var(--ux-accent-soft)}
@media(max-width:1023px){.family-workspace{grid-template-columns:1fr}}@media(max-width:639px){.family-members{grid-template-columns:1fr}.family-summary{gap:17px;flex-wrap:wrap}.family-summary>p{margin-left:0;width:100%}.family-summary strong{font-size:21px}.family-member,.family-milestones{padding:18px}.milestone-actions button{width:44px;height:44px}}
.family-page{width:100%;max-width:none}.family-hero{position:relative;isolation:isolate;min-height:235px;display:flex;align-items:flex-end;overflow:hidden;border-radius:17px 17px 0 0;background:radial-gradient(ellipse at 80% 0,#a7783a12,transparent 65%)}.family-hero-photo{min-height:330px}.family-hero-image{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center 38%;z-index:-2}.family-hero:after{content:"";position:absolute;inset:0;z-index:-1;background:linear-gradient(180deg,#24232d05 0%,#24232d50 40%,#24232de0 75%,#24232d 100%)}.family-hero-content{width:100%;padding:30px 30px 0}.family-hero :deep(.sky-page-header){align-items:flex-end}.family-hero :deep(.sky-page-header p){color:#d0c4b2;text-shadow:0 1px 5px #0008}.family-summary{padding:13px 0 22px;margin-top:14px;gap:24px;border:0}.family-summary>button{display:flex;align-items:baseline;gap:8px;padding:6px 3px;min-height:44px;color:#c8b38f;border-bottom:1px solid transparent;transition:border-color .2s}.family-summary>button:hover{border-color:#e2b87366}.family-summary>button svg{opacity:.45;align-self:center}.family-workspace{grid-template-columns:minmax(0,1.5fr) minmax(300px,.8fr)}.family-members{grid-template-columns:repeat(2,minmax(0,1fr))}.family-member{position:relative;padding:25px;background:radial-gradient(ellipse at 15% 0%,#d99e5411,transparent 70%),#1a1c20;transition:background .3s,border-color .3s}.family-member:hover,.family-member:focus-within{background:radial-gradient(ellipse at 15% 0%,#d99e5420,transparent 75%),#1c1e22;border-color:#e2b87345}.family-avatar{width:58px;height:58px;font-size:20px}.family-member h3{font-size:17px;font-weight:550}.family-member>p{color:#a59b8c;margin-top:21px}.family-member>a{position:absolute;right:20px;bottom:20px;font-size:0;color:#bba27b}.family-member>a svg{width:17px;height:17px}.family-member>a:focus-visible{font-size:11px}.family-member .count-up{color:#d5b47e;font-variant-numeric:tabular-nums}.family-milestones{min-height:0;align-self:start}.milestone-empty{padding:24px 0 7px;color:#bbaa8f}.milestone-empty>p{font-size:14px;margin:12px 0 7px}.milestone-empty>small{display:block;font-size:12px;color:#9e9689;line-height:1.7}.milestone-empty>div{display:flex;gap:9px;margin-top:18px;flex-wrap:wrap}.milestone-empty button{display:flex;align-items:center;gap:5px;min-height:38px;padding:7px 10px;border:1px dashed #e2b87340;border-radius:8px;font-size:12px;color:#cbb188}.family-data-status{color:#b9ac99;font-size:14px;line-height:1.8;padding:15px 0}.family-data-row{border-bottom:1px solid var(--ux-line);padding:15px 0}.family-data-row>a,.family-data-row>div{display:grid;grid-template-columns:1fr auto;gap:8px}.family-data-row strong{font-size:15px;font-weight:500;color:var(--ux-ink)}.family-data-row small{grid-column:1;font-size:12px;color:var(--ux-ink-3)}.family-data-row svg{grid-column:2;grid-row:1/3;align-self:center;color:var(--ux-accent)}.family-data-more{margin-top:22px;width:100%}
@media(min-width:1500px){.family-members{grid-template-columns:repeat(2,minmax(0,1fr))}.family-workspace{gap:32px}.family-hero-photo{min-height:360px}}@media(max-width:1023px){.family-workspace{grid-template-columns:1fr}.family-hero-photo{min-height:290px}}@media(max-width:639px){.family-hero-content{padding:22px 17px 0}.family-hero-photo{min-height:270px}.family-members{grid-template-columns:1fr}.family-member{padding:20px}.family-summary{gap:12px}.family-summary>button{gap:5px}.family-summary strong{font-size:21px}.family-summary>p{margin:0;font-size:11px}.family-avatar{width:52px;height:52px}}
@media(prefers-reduced-motion:reduce){.family-member,.family-summary>button{transition:none}}
</style>

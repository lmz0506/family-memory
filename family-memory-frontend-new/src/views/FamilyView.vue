<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {
  CalendarDays, ChevronRight, Copy, Gift, Home, LockKeyhole, MapPin, Pencil,
  Plus, Repeat2, Trash2, UserPlus, Users, X
} from 'lucide-vue-next'
import {familyApi, milestoneApi} from '@/api'
import {useDialog} from '@/composables/useDialog'
import {notify} from '@/composables/useToast'
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
const milestoneLoading = ref(false)
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
  try { milestones.value = await milestoneApi.list(family.current.id) }
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
  <section class="page-shell page-reveal space-y-6 pb-8">
    <div class="relative overflow-hidden rounded-3xl bg-gradient-to-r from-warm-900 via-warm-800 to-warm-900 p-8 text-white shadow-soft-lg md:p-10">
      <div class="absolute inset-0 opacity-25 mix-blend-overlay">
        <img :src="family.current?.coverUrl || '/assets/images/family-sunset.jpg'" alt="家庭封面" class="h-full w-full object-cover">
      </div>
      <div class="pointer-events-none absolute -bottom-10 -right-10 h-80 w-80 rounded-full bg-brand-coral/30 blur-3xl"></div>
      <div class="relative z-10 flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
        <div class="max-w-2xl space-y-3">
          <div class="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-1 text-xs font-semibold text-brand-peach backdrop-blur-md">
            <Home :size="14"> </Home> OUR FAMILY · {{ family.current?.name || '温馨小家' }}
          </div>
          <h1 class="font-serif text-3xl font-bold leading-tight md:text-4xl">{{ family.current?.description || '平凡日子里的相亲相爱' }}</h1>
          <p class="text-xs leading-relaxed text-warm-200 opacity-90 md:text-sm">
            在这里，每一位家人的关爱都有迹可循。已共同相伴记录
            <strong class="font-bold text-brand-peach">{{ family.current?.recordDays || 0 }} 天</strong>，凝聚了
            <strong class="font-bold text-brand-peach">{{ family.current?.memoryCount || 0 }} 条</strong>珍贵回忆。
          </p>
        </div>
        <button @click="createInvite" :disabled="inviteBusy" class="flex w-full items-center justify-center gap-2 whitespace-nowrap rounded-2xl bg-brand-coral px-5 py-3 text-xs font-bold text-white shadow-glow transition-all hover:bg-brand-coral-hover active:scale-95 md:w-auto">
          <UserPlus :size="16"> </UserPlus> 邀请新成员
        </button>
      </div>
      <div class="relative z-10 mt-8 grid grid-cols-3 gap-4 border-t border-white/10 pt-6 text-center md:flex md:items-center md:gap-12 md:text-left">
        <div><span class="block text-2xl font-bold text-brand-peach">{{ family.current?.memberCount || family.members.length }} 位</span><span class="text-xs text-warm-300">家庭成员</span></div>
        <div class="border-l border-white/10 pl-4 md:pl-12"><span class="block text-2xl font-bold text-white">{{ family.current?.memoryCount || 0 }} 条</span><span class="text-xs text-warm-300">回忆瞬间</span></div>
        <div class="border-l border-white/10 pl-4 md:pl-12"><span class="block text-2xl font-bold text-white">{{ family.current?.capsuleCount || 0 }} 个</span><span class="text-xs text-warm-300">珍藏胶囊</span></div>
      </div>
    </div>

    <div class="space-y-4">
      <div class="flex items-center justify-between border-b border-warm-200/60 pb-3">
        <div class="flex items-center gap-2"><h2 class="font-serif text-xl font-bold text-warm-900">家人们</h2><span class="rounded-full bg-brand-coral-light px-2 py-0.5 text-xs font-semibold text-brand-coral">{{ family.members.length }} 位成员</span></div>
        <button @click="createInvite" :disabled="inviteBusy" class="flex items-center gap-1 text-xs font-semibold text-brand-coral hover:underline">邀请家人 <ChevronRight :size="14"> </ChevronRight></button>
      </div>
      <div class="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
        <article v-for="member in family.members" :key="member.id" class="group space-y-4 rounded-3xl border border-warm-100 bg-white p-5 shadow-soft transition-all hover:-translate-y-0.5 hover:shadow-soft-lg">
          <div class="flex min-w-0 items-center gap-3">
            <div class="relative h-12 w-12 shrink-0 overflow-hidden rounded-2xl bg-brand-coral-light text-brand-coral ring-2 ring-warm-100">
              <img v-if="member.avatar" :src="member.avatar" :alt="member.displayName" class="h-full w-full object-cover">
              <span v-else class="grid h-full w-full place-items-center text-sm font-bold">{{ member.displayName.slice(0, 1) }}</span>
            </div>
            <div class="min-w-0"><div class="flex flex-wrap items-center gap-1.5"><h3 class="truncate text-sm font-bold text-warm-900">{{ member.displayName }}</h3><span v-if="member.admin" class="rounded-md bg-brand-coral-light px-2 py-0.5 text-xs font-bold text-brand-coral">管理员</span></div><p class="mt-0.5 text-xs text-warm-400">{{ member.role || '家庭成员' }}</p></div>
          </div>
          <div class="flex items-center justify-between rounded-2xl bg-warm-50/60 p-3 text-xs text-warm-700"><span>累计发布 <strong class="text-warm-900">{{ member.memoryCount }}</strong> 条回忆</span></div>
          <RouterLink :to="{name: 'timeline'}" class="block rounded-xl bg-warm-50 py-2 text-center text-xs font-semibold text-warm-800 transition hover:bg-warm-100">查看家庭记录</RouterLink>
        </article>
        <button @click="createInvite" :disabled="inviteBusy" class="group flex min-h-[184px] flex-col items-center justify-center space-y-3 rounded-3xl border-2 border-dashed border-warm-200 bg-warm-50/40 p-6 text-center transition-all hover:border-brand-coral hover:bg-warm-50">
          <div class="grid h-12 w-12 place-items-center rounded-2xl bg-brand-coral-light text-brand-coral transition-transform group-hover:scale-110"><UserPlus :size="24"> </UserPlus></div>
          <div><h3 class="text-sm font-bold text-warm-900 transition-colors group-hover:text-brand-coral">邀请更多家人加入</h3><p class="mt-1 text-xs text-warm-400">共享温馨相册，一起收藏感动</p></div>
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-6 pt-2 lg:grid-cols-3">
      <div class="min-w-0 space-y-4 rounded-3xl border border-warm-100 bg-gradient-to-br from-white to-warm-50/60 p-6 shadow-soft lg:col-span-2">
        <div class="flex items-center gap-2"><div class="rounded-xl bg-teal-100 p-2 text-teal-800"><MapPin :size="20"> </MapPin></div><div><h3 class="text-base font-bold text-warm-900">家庭足迹图谱</h3><p class="text-xs text-warm-400">地点地图将在下一阶段开放</p></div></div>
        <div class="relative flex w-full min-w-0 aspect-[16/7] min-h-52 items-center justify-center overflow-hidden rounded-2xl bg-warm-200">
          <img src="/assets/images/lake-sunset.jpg" alt="家庭足迹预览" class="absolute inset-0 h-full w-full object-cover opacity-70">
          <div class="absolute inset-0 bg-gradient-to-t from-warm-900/75 via-transparent to-black/10"></div>
          <div class="relative rounded-full bg-white/90 px-4 py-2 text-xs font-semibold text-warm-800 shadow-sm backdrop-blur">更多家庭足迹，敬请期待</div>
        </div>
      </div>

      <div class="flex min-h-[360px] flex-col justify-between space-y-4 rounded-3xl border border-warm-100 bg-white p-6 shadow-soft">
        <div class="space-y-4">
          <div class="flex items-center gap-2 border-b border-warm-100 pb-3">
            <div class="rounded-xl bg-brand-coral-light p-2 text-brand-coral"><Gift :size="20"> </Gift></div>
            <div><h3 class="text-base font-bold text-warm-900">家庭大事记提醒</h3><p class="text-xs text-warm-400">贴心记住每一个重要的日子</p></div>
          </div>

          <div v-if="milestoneLoading" class="grid min-h-32 place-items-center text-xs text-warm-400">正在读取提醒…</div>
          <div v-else-if="!milestones.length" class="grid min-h-32 place-items-center rounded-2xl border border-dashed border-warm-200 bg-warm-50/50 px-6 text-center">
            <div><CalendarDays class="mx-auto mb-2 text-warm-300" :size="26"> </CalendarDays><p class="text-xs font-semibold text-warm-600">还没有大事记提醒</p><p class="mt-1 text-xs text-warm-400">添加后会按照你设置的范围展示</p></div>
          </div>
          <div v-else class="max-h-[360px] space-y-3 overflow-y-auto pr-1">
            <article v-for="(item, index) in milestones" :key="item.id" class="group rounded-2xl border p-3.5" :class="index === 0 && !item.expired ? 'border-brand-coral/20 bg-brand-coral-light/50' : 'border-warm-100 bg-warm-50'">
              <div class="flex items-center justify-between gap-2">
                <div class="flex min-w-0 items-center gap-3">
                  <div class="flex h-10 w-10 shrink-0 flex-col items-center justify-center rounded-xl font-serif text-sm font-bold leading-none" :class="index === 0 && !item.expired ? 'bg-brand-coral text-white' : 'bg-warm-200 text-warm-800'">
                    <span>{{ dateParts(item.nextEventDate).day }}</span><span class="mt-0.5 text-xs opacity-80">{{ dateParts(item.nextEventDate).month }}月</span>
                  </div>
                  <div class="min-w-0"><h4 class="truncate text-xs font-bold text-warm-900">{{ item.title }}</h4><p v-if="item.description" class="mt-0.5 truncate text-xs text-warm-500">{{ item.description }}</p></div>
                </div>
                <span class="shrink-0 rounded-lg bg-white px-2 py-1 text-xs font-bold shadow-xs" :class="item.expired ? 'text-warm-400' : 'text-brand-coral'">{{ remainingText(item) }}</span>
              </div>
              <div class="mt-2 flex items-center justify-between border-t border-warm-200/60 pt-2">
                <div class="flex min-w-0 items-center gap-1 text-xs text-warm-400"><Repeat2 v-if="item.recurringYearly" :size="11"> </Repeat2><Users v-else-if="item.visibilityScope !== 'PRIVATE'" :size="11"> </Users><LockKeyhole v-else :size="11"> </LockKeyhole><span class="truncate">{{ item.recurringYearly ? '每年提醒 · ' : '' }}{{ visibilityText(item) }}</span></div>
                <div v-if="item.canManage" class="ml-2 flex shrink-0 items-center gap-1">
                  <button @click="openEditMilestone(item)" aria-label="编辑提醒" class="grid h-9 w-9 place-items-center rounded-md text-warm-500 transition hover:bg-white hover:text-brand-coral"><Pencil :size="12"> </Pencil></button>
                  <button @click="deleting=item" aria-label="删除提醒" class="grid h-9 w-9 place-items-center rounded-md text-warm-500 transition hover:bg-white hover:text-red-500"><Trash2 :size="12"> </Trash2></button>
                </div>
              </div>
            </article>
          </div>
        </div>
        <button @click="openCreateMilestone" class="mt-2 flex w-full items-center justify-center gap-1.5 rounded-xl border border-warm-200 py-2.5 text-center text-xs font-semibold text-warm-700 transition-colors hover:bg-warm-50"><Plus :size="14"> </Plus> 添加纪念日提醒</button>
      </div>
    </div>
  </section>

  <Teleport to="body">
    <Transition name="fade">
      <div v-if="invite" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm" @click.self="invite = null">
        <section ref="invitePanel" role="dialog" aria-modal="true" aria-label="邀请家人加入" tabindex="-1" class="w-full max-w-md space-y-5 rounded-3xl border border-warm-100 max-h-[90dvh] overflow-y-auto bg-white p-6 text-center shadow-2xl">
          <div class="flex items-center justify-between border-b border-warm-100 pb-3"><div class="flex items-center gap-2"><div class="rounded-xl bg-brand-coral-light p-2 text-brand-coral"><UserPlus :size="16"> </UserPlus></div><h2 class="text-base font-bold text-warm-900">邀请家人加入</h2></div><button @click="invite = null" class="rounded-lg p-1 text-warm-400 hover:text-warm-800"><X :size="20"> </X></button></div>
          <p class="text-xs leading-6 text-warm-500">复制下方专属邀请码发给家人，即可共同记录与查阅家庭照片。</p>
          <div class="rounded-2xl bg-warm-50 p-5"><p class="text-xs font-semibold text-warm-400">家庭专属邀请码</p><div class="mt-2 font-serif text-3xl font-bold tracking-[.18em] text-warm-900">{{ invite.code }}</div><p class="mt-2 text-xs text-warm-400">有效期至 {{ invite.expireAt }}</p></div>
          <button @click="copyInvite" class="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-coral py-3 text-xs font-bold text-white transition hover:bg-brand-coral-hover"><Copy :size="16"> </Copy>{{ copied ? '已复制' : '复制邀请码' }}</button>
          <p v-if="inviteError" class="text-xs text-red-600">{{ inviteError }}</p>
        </section>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="milestoneModal" class="fixed inset-0 z-[60] flex items-end justify-center overflow-y-auto bg-black/40 p-4 backdrop-blur-sm sm:items-center" @click.self="milestoneModal = false">
        <form ref="milestonePanel" role="dialog" aria-modal="true" aria-label="纪念日提醒" tabindex="-1" @submit.prevent="saveMilestone" class="my-auto w-full max-w-lg space-y-5 rounded-t-3xl border border-warm-100 max-h-[90dvh] overflow-y-auto bg-white p-6 shadow-2xl sm:rounded-3xl">
          <div class="flex items-center justify-between border-b border-warm-100 pb-3"><div class="flex items-center gap-2"><div class="rounded-xl bg-brand-coral-light p-2 text-brand-coral"><Gift :size="16"> </Gift></div><div><h2 class="text-base font-bold text-warm-900">{{ editing ? '编辑大事记提醒' : '添加大事记提醒' }}</h2><p class="mt-0.5 text-xs text-warm-400">默认仅自己可见，可按需分享给家人</p></div></div><button type="button" @click="milestoneModal = false" class="rounded-lg p-1 text-warm-400 hover:text-warm-800"><X :size="20"> </X></button></div>

          <div class="space-y-4">
            <label class="block"><span class="mb-1.5 block text-xs font-semibold text-warm-700">名称</span><input v-model.trim="form.title" required maxlength="120" placeholder="例如：体检、生日、家庭纪念日" class="w-full rounded-xl border border-warm-200 bg-warm-50/40 px-3.5 py-2.5 text-sm text-warm-900 outline-none transition focus:border-brand-coral focus:ring-2 focus:ring-brand-coral/10"></label>
            <div class="grid gap-4 sm:grid-cols-2">
              <label class="block"><span class="mb-1.5 block text-xs font-semibold text-warm-700">日期</span><input v-model="form.eventDate" type="date" required :min="form.recurringYearly ? undefined : todayText()" class="w-full rounded-xl border border-warm-200 bg-warm-50/40 px-3.5 py-2.5 text-sm text-warm-900 outline-none transition focus:border-brand-coral focus:ring-2 focus:ring-brand-coral/10"></label>
              <label class="flex cursor-pointer items-center gap-3 self-end rounded-xl border border-warm-200 bg-warm-50/40 px-3.5 py-2.5"><input v-model="form.recurringYearly" type="checkbox" class="h-4 w-4 accent-brand-coral"><span><span class="block text-xs font-semibold text-warm-700">每年重复提醒</span><span class="block text-xs text-warm-400">适合生日与周年纪念</span></span></label>
            </div>
            <label class="block"><span class="mb-1.5 block text-xs font-semibold text-warm-700">备注（选填）</span><textarea v-model.trim="form.description" maxlength="500" rows="2" placeholder="写下一句提醒" class="w-full resize-none rounded-xl border border-warm-200 bg-warm-50/40 px-3.5 py-2.5 text-sm text-warm-900 outline-none transition focus:border-brand-coral focus:ring-2 focus:ring-brand-coral/10"></textarea></label>

            <fieldset>
              <legend class="mb-2 block text-xs font-semibold text-warm-700">谁可以看到</legend>
              <div class="grid grid-cols-3 gap-2">
                <button type="button" @click="setVisibility('PRIVATE')" class="rounded-xl border px-2 py-3 text-center transition" :class="form.visibilityScope === 'PRIVATE' ? 'border-brand-coral bg-brand-coral-light text-brand-coral' : 'border-warm-200 text-warm-500 hover:bg-warm-50'"><LockKeyhole class="mx-auto mb-1" :size="17"> </LockKeyhole><span class="block text-xs font-bold">仅自己</span><span class="mt-0.5 block text-xs opacity-75">默认</span></button>
                <button type="button" @click="setVisibility('FAMILY')" class="rounded-xl border px-2 py-3 text-center transition" :class="form.visibilityScope === 'FAMILY' ? 'border-brand-coral bg-brand-coral-light text-brand-coral' : 'border-warm-200 text-warm-500 hover:bg-warm-50'"><Home class="mx-auto mb-1" :size="17"> </Home><span class="block text-xs font-bold">全家可见</span><span class="mt-0.5 block text-xs opacity-75">所有成员</span></button>
                <button type="button" @click="setVisibility('SPECIFIC')" class="rounded-xl border px-2 py-3 text-center transition" :class="form.visibilityScope === 'SPECIFIC' ? 'border-brand-coral bg-brand-coral-light text-brand-coral' : 'border-warm-200 text-warm-500 hover:bg-warm-50'"><Users class="mx-auto mb-1" :size="17"> </Users><span class="block text-xs font-bold">指定家人</span><span class="mt-0.5 block text-xs opacity-75">自由选择</span></button>
              </div>
            </fieldset>

            <div v-if="form.visibilityScope === 'SPECIFIC'" class="rounded-2xl border border-warm-100 bg-warm-50/60 p-3">
              <p class="mb-2 text-xs text-warm-500">请选择可以看到这条提醒的家人（你自己始终可见）</p>
              <div v-if="selectableMembers.length" class="flex flex-wrap gap-2">
                <button v-for="member in selectableMembers" :key="member.userId" type="button" @click="toggleViewer(member.userId)" class="flex items-center gap-1.5 rounded-full border px-2.5 py-1.5 text-xs font-semibold transition" :class="form.viewerUserIds.includes(member.userId) ? 'border-brand-coral bg-brand-coral text-white' : 'border-warm-200 bg-white text-warm-600 hover:border-brand-coral/50'">
                  <span class="grid h-5 w-5 place-items-center overflow-hidden rounded-full bg-brand-coral-light text-xs text-brand-coral"><img v-if="member.avatar" :src="member.avatar" :alt="member.displayName" class="h-full w-full object-cover"><span v-else>{{ member.displayName.slice(0, 1) }}</span></span>{{ member.displayName }}
                </button>
              </div>
              <p v-else class="text-xs text-warm-400">暂无其他家庭成员，请先邀请家人加入。</p>
            </div>
          </div>

          <p v-if="formError" class="rounded-xl bg-red-50 px-3 py-2 text-xs text-red-600">{{ formError }}</p>
          <div class="flex gap-3"><button type="button" @click="milestoneModal = false" class="flex-1 rounded-xl border border-warm-200 py-2.5 text-xs font-semibold text-warm-600 hover:bg-warm-50">取消</button><button type="submit" :disabled="saving" class="flex-1 rounded-xl bg-brand-coral py-2.5 text-xs font-bold text-white shadow-glow transition hover:bg-brand-coral-hover disabled:opacity-60">{{ saving ? '保存中…' : (editing ? '保存修改' : '添加提醒') }}</button></div>
        </form>
      </div>
    </Transition>
  </Teleport>
  <ConfirmDialog :open="Boolean(deleting)" :title="`删除${deleting?.title || '提醒'}？`" description="删除后将不再提醒家人，这个操作无法撤销。" :busy="deleteBusy" @update:open="deleting=null" @confirm="deleting && removeMilestone(deleting)"/>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue';
import {useRouter} from 'vue-router';
import {ArrowRight, Home, KeyRound, LoaderCircle, Sparkles} from 'lucide-vue-next';
import {familyApi} from '@/api';
import {useAuthStore} from '@/stores/auth';
import {useFamilyStore} from '@/stores/family'
import StarField from '@/components/StarField.vue'
import StarChart from '@/components/StarChart.vue'
import '@/styles/constellation-pages.css'

const router = useRouter(), auth = useAuthStore(), family = useFamilyStore();
const mode = ref<'choose' | 'create' | 'join'>('choose');
const form = reactive({name: '', description: '', inviteCode: '', displayName: auth.user?.nickname || ''});
const error = ref('');
const loading = ref(false)

async function submit() {
  loading.value = true;
  error.value = '';
  try {
    if (mode.value === 'create') await familyApi.create({
      name: form.name,
      description: form.description
    }); else await familyApi.join({inviteCode: form.inviteCode, displayName: form.displayName});
    await auth.loadProfile();
    await family.load();
    router.replace('/')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <main class="join-universe">
    <StarField/>
    <div class="join-brand"><Sparkles :size="22"/><span>拾光 <small>记忆星海</small></span></div>
    <aside class="join-story"><p class="constellation-eyebrow">EVERY FAMILY IS A CONSTELLATION</p><h2>一个人是一颗星，<br>一家人是<em>一片星海。</em></h2><p>找到彼此，让散落的回忆有处安放。<br>从这一刻起，一起点亮属于你们的小宇宙。</p><StarChart variant="family"/><span class="join-caption">✦ 因为相连，所以闪耀</span></aside>
    <section
        class="join-panel w-full max-w-2xl rounded-2xl border border-night-line bg-night-surface p-6 shadow-soft sm:p-10">
      <div class="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-night-action text-night-action-ink shadow-float">
        <Sparkles :size="24"/>
      </div>
      <div v-if="mode==='choose'" class="text-center"><h1 class="mt-6 font-display text-3xl font-black">
        让我们的星光相遇</h1>
        <p class="mt-3 text-sm text-night-muted">创建一片星海，或循着家人的邀请加入。</p>
        <div class="mt-8 grid gap-4 sm:grid-cols-2">
          <button @click="mode='create'"
                  class="group rounded-[26px] border border-night-line bg-night-surface p-6 text-left transition  hover:border-night-accent hover:shadow-none">
            <div class="grid h-11 w-11 place-items-center rounded-2xl bg-night-accent/10 text-night-accent">
              <Home :size="20"/>
            </div>
            <h2 class="mt-5 font-display text-xl font-bold">点亮新的星海</h2>
            <p class="mt-2 text-xs leading-5 text-night-muted">成为家庭管理员，邀请其他家人加入。</p></button>
          <button @click="mode='join'"
                  class="group rounded-[26px] border border-night-line bg-night-surface p-6 text-left transition  hover:border-night-accent hover:shadow-none">
            <div class="grid h-11 w-11 place-items-center rounded-2xl bg-night-accent/10 text-night-accent">
              <KeyRound :size="20"/>
            </div>
            <h2 class="mt-5 font-display text-xl font-bold">加入家人的星海</h2>
            <p class="mt-2 text-xs leading-5 text-night-muted">输入家人发给你的 8 位邀请码。</p></button>
        </div>
      </div>
      <form v-else @submit.prevent="submit" class="mx-auto mt-7 max-w-md">
        <button type="button" @click="mode='choose'" class="text-xs text-night-muted">← 返回选择</button>
        <h1 class="mt-5 font-display text-2xl font-black">
          {{ mode === 'create' ? '为你们的星海命名' : '循着邀请，与家人相遇' }}</h1>
        <div class="mt-6 space-y-4">
          <template v-if="mode==='create'"><label class="join-label" for="family-name">家庭名称</label><input id="family-name" v-model="form.name" required
                                                  class="w-full rounded-2xl border border-night-line bg-night-surface px-4 py-3.5 text-sm outline-none focus:border-night-accent"
                                                  placeholder="例如：林家小院"/><label class="join-label" for="family-description">属于你们的一句话（选填）</label><textarea id="family-description" v-model="form.description"
                                                                                         class="h-24 w-full resize-none rounded-2xl border border-night-line bg-night-surface px-4 py-3.5 text-sm outline-none focus:border-night-accent"
                                                                                         placeholder="写一句属于这个家的话"></textarea>
          </template>
          <template v-else><label class="join-label" for="family-invite">8 位家庭邀请码</label><input id="family-invite" v-model="form.inviteCode" required minlength="8" maxlength="8" autocomplete="off"
                                  class="w-full rounded-2xl border border-night-line bg-night-surface px-4 py-3.5 text-center text-xl font-bold uppercase tracking-[.25em] outline-none focus:border-night-accent"
                                  placeholder="XXXXXXXX"/><label class="join-label" for="family-display-name">家人怎么称呼你</label><input id="family-display-name" v-model="form.displayName"
                                                                 class="w-full rounded-2xl border border-night-line bg-night-surface px-4 py-3.5 text-sm outline-none"
                                                                 placeholder="家人怎么称呼你"/></template>
          <p v-if="error" class="rounded-xl bg-night-danger/10 px-3 py-2 text-xs text-night-danger">{{ error }}</p>
          <button :disabled="loading"
                  class="sky-primary flex w-full items-center justify-center gap-2 rounded-2xl bg-night-action py-3.5 text-sm font-bold text-night-action-ink">
            <LoaderCircle v-if="loading" class="animate-spin" :size="17"/>
            {{loading?'正在连接星海…':mode==='create'?'开启我们的星海':'与家人相聚'}}
            <ArrowRight v-if="!loading" :size="17"/>
          </button>
        </div>
      </form>
    </section>
  </main>
</template>

<style scoped>
.join-universe{position:relative;isolation:isolate;min-height:100dvh;display:grid;grid-template-columns:minmax(0,1fr) minmax(360px,480px);align-items:center;gap:5vw;padding:110px max(6vw,24px) 60px;color:#eadfce;background:#0e1422}.join-universe>.reading-starfield{z-index:-1;position:absolute}.join-brand{position:absolute;top:30px;left:6vw;display:flex;align-items:center;gap:10px;color:#e9c79e;font-size:23px;letter-spacing:3px}.join-brand small{font-size:10px;letter-spacing:2px;color:#9ea6ba;margin-left:10px}.join-story{position:relative}.join-story h2{font:400 clamp(28px,3vw,48px)/1.6 'Songti SC','SimSun',serif;letter-spacing:1px}.join-story h2 em{font-style:normal;color:#eac89e}.join-story>p:not(.constellation-eyebrow){font-size:13px;line-height:2;color:#abb2c4;margin-top:20px}.join-story>.star-chart{max-width:500px;margin:-15px auto -10px}.join-caption{display:block;text-align:center;color:#a9a2b3;font-size:11px;letter-spacing:3px}.join-panel{position:relative;background:linear-gradient(145deg,#273044ba,#172133eb);border:1px solid #c6b4d434;border-radius:26px;box-shadow:0 25px 70px #060b2333;backdrop-filter:blur(12px)}.join-panel>div:first-child{border-radius:50%;background:#e9c99f12;border:1px solid #e9c99f45;color:#e9c99f;box-shadow:0 0 0 9px #e9c99f04,0 0 40px #e9c99f0a}.join-panel h1{font-family:'Songti SC',serif;font-weight:400;font-size:27px}.join-panel h2{font-size:18px;font-weight:400}.join-panel .group{border-radius:17px;background:#101c2c66;min-height:190px}.join-panel .group:hover{background:#27354b;border-color:#dfc69e70;transform:translateY(-3px)}.join-panel form input,.join-panel textarea{background:#0f1b2bc9;border-color:#c2bdd82e;color:#eee0cc}.join-label{font-size:12px;color:#c3bcae;display:block}.join-panel .sky-primary{background:#e9c99f;color:#262330;border-radius:24px;min-height:48px}.join-panel form{margin-top:10px}.join-panel input:focus-visible,.join-panel textarea:focus-visible{outline:2px solid #ddc199;outline-offset:3px}.join-panel .group:focus-visible{outline:2px solid #ddc199;outline-offset:4px}
@media(max-width:900px){.join-universe{gap:4vw;padding-inline:28px;grid-template-columns:minmax(0,1fr) minmax(340px,1fr)}.join-story h2{font-size:30px}.join-panel{padding:28px 22px}}
@media(max-width:700px){.join-universe{grid-template-columns:minmax(0,1fr);padding:100px 20px 35px;gap:30px}.join-story>.star-chart,.join-caption{display:none}.join-story h2{font-size:30px}.join-story>p:not(.constellation-eyebrow){font-size:12px;margin-top:12px}.join-story .constellation-eyebrow{font-size:8px;letter-spacing:2px}.join-panel{margin:auto}.join-panel .group{min-height:0;padding:20px}.join-brand{left:24px;top:26px}.join-panel h1{font-size:25px}}
@media(prefers-reduced-motion:reduce){.join-panel .group{transition:none;transform:none}}
</style>

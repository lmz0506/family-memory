<!-- Lucian UX/UI Prompt #075 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#075 -->
<script setup lang="ts">
import {computed,ref} from 'vue'
import {Check,ChevronDown,RotateCcw} from 'lucide-vue-next'
export type UploadItem = {name:string; progress:number; state:'waiting'|'uploading'|'done'|'error'}
const props=defineProps<{items:UploadItem[]; busy:boolean}>()
const emit=defineEmits<{retry:[]}>()
const folded=ref(false)
const done=computed(()=>props.items.filter(item=>item.state==='done').length)
const progress=computed(()=>props.items.length ? props.items.reduce((sum,item)=>sum+item.progress,0)/props.items.length/100 : 0)
</script>
<template>
  <div class="ux u075 is-on" :class="{'is-fold':folded,'is-done':done===items.length}" :style="{'--p':progress}">
    <section class="u075-pan" aria-label="文件上传进度">
      <button class="u075-hd" type="button" :aria-expanded="!folded" @click="folded=!folded"><span class="u075-ring" aria-hidden="true"><svg viewBox="0 0 38 38"><circle class="t" cx="19" cy="19" r="16"/><circle class="p" cx="19" cy="19" r="16" pathLength="100"/></svg><i><Check/></i></span><span class="u075-ht" aria-live="polite"><b>{{done===items.length?'影像上传完成':'正在珍藏影像'}}</b><small>{{done}} / {{items.length}} 个文件已完成</small></span><ChevronDown class="u075-chev"/></button>
      <div class="u075-body"><ul><li v-for="(item,index) in items" :key="index" class="u075-f" :class="{ok:item.state==='done',bad:item.state==='error'}" :style="{'--p':item.progress/100}">
        <span class="u075-ic" style="--c:var(--ux-accent)" aria-hidden="true">{{index+1}}</span><span class="u075-fx"><b><span>{{item.name}}</span><small>{{item.state==='error'?'上传失败':item.state==='waiting'?'等待上传':`${item.progress}%`}}</small></b><span class="u075-bar" role="progressbar" :aria-label="`${item.name}上传进度`" :aria-valuenow="item.progress" aria-valuemin="0" aria-valuemax="100"><i></i></span></span>
        <span class="u075-st"><Check v-if="item.state==='done'"/><button v-if="item.state==='error'" type="button" class="u075-retry" :disabled="busy" aria-label="重试未完成的上传" @click="emit('retry')"><RotateCcw/></button></span>
      </li></ul></div>
    </section>
  </div>
</template>
<style scoped>
.u075{position:relative;width:350px;height:330px;display:flex;flex-direction:column;justify-content:center;gap:12px}
.u075-add{align-self:flex-start;height:46px;padding:0 18px 0 14px;border:0;border-radius:999px;display:inline-flex;align-items:center;gap:8px;background:var(--ux-ink);color:var(--ux-page);font-size:14px;font-weight:750;transition:transform .3s var(--ux-spring),opacity .3s}
.u075-add:active{transform:scale(.95)}
.u075-add:focus-visible{outline:2px solid var(--ux-accent);outline-offset:3px}
.u075-add svg{width:18px;height:18px;fill:none;stroke:currentColor;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round}
.u075-add[aria-disabled="true"]{opacity:.4}
.u075-pan{border-radius:22px;background:var(--ux-surface);box-shadow:0 0 0 1px var(--ux-line),0 24px 44px -24px var(--ux-shadow-strong);overflow:hidden;transform:translateY(30px);opacity:0;transition:transform .5s var(--ux-spring),opacity .3s}
.u075.is-on .u075-pan{transform:none;opacity:1}
.u075-hd{display:flex;align-items:center;gap:12px;width:100%;height:66px;padding:0 14px;border:0;background:none;text-align:left}
.u075-hd:focus-visible{outline:2px solid var(--ux-accent);outline-offset:-3px;border-radius:22px}
.u075-ring{position:relative;width:38px;height:38px;flex:none}
.u075-ring svg{width:100%;height:100%;transform:rotate(-90deg)}
.u075-ring circle{fill:none;stroke-width:3.5}
.u075-ring .t{stroke:var(--ux-surface-3)}
.u075-ring .p{stroke:var(--ux-accent);stroke-linecap:round;stroke-dasharray:100;stroke-dashoffset:calc(100 - var(--p,0) * 100);transition:stroke .3s}
.u075-ring i{position:absolute;inset:0;display:grid;place-items:center;color:var(--ux-mint);opacity:0;transform:scale(.5);transition:opacity .3s,transform .45s var(--ux-spring)}
.u075-ring i svg{width:18px;height:18px;transform:none;fill:none;stroke:currentColor;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}
.u075.is-done .u075-ring .p{stroke:var(--ux-mint)}
.u075.is-done .u075-ring i{opacity:1;transform:none}
.u075-ht{flex:1;min-width:0}
.u075-ht b{display:block;font-size:14.5px;font-weight:800;letter-spacing:-.02em}
.u075-ht small{display:block;margin-top:2px;font-size:12px;font-weight:650;color:var(--ux-ink-3);font-variant-numeric:tabular-nums}
.u075-chev{width:18px;height:18px;fill:none;stroke:var(--ux-ink-3);stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round;transition:transform .4s var(--ux-spring)}
.u075.is-fold .u075-chev{transform:rotate(180deg)}
.u075-body{display:grid;grid-template-rows:1fr;transition:grid-template-rows .45s var(--ux-ease)}
.u075.is-fold .u075-body{grid-template-rows:0fr}
.u075-body>ul{min-height:0;overflow:hidden;margin:0;padding:0 8px;list-style:none}
.u075-f{display:flex;align-items:center;gap:12px;padding:10px 6px;border-top:1px solid var(--ux-line)}
.u075-f:last-child{padding-bottom:14px}
.u075-ic{width:36px;height:36px;flex:none;border-radius:10px;display:grid;place-items:center;background:var(--c);color:#fff;font-size:10px;font-weight:850;letter-spacing:.02em}
.u075-fx{flex:1;min-width:0}
.u075-fx>b{display:flex;justify-content:space-between;gap:8px;font-size:13px;font-weight:720}
.u075-fx b span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.u075-fx b small{flex:none;font-size:11.5px;font-weight:700;color:var(--ux-ink-3);font-variant-numeric:tabular-nums}
.u075-bar{display:block;height:5px;margin-top:7px;border-radius:3px;background:var(--ux-surface-3);overflow:hidden}
.u075-bar i{display:block;height:100%;width:100%;border-radius:inherit;background:var(--ux-accent);transform-origin:0 50%;transform:scaleX(var(--p,0));transition:background-color .3s}
.u075-f.ok .u075-bar i{background:var(--ux-mint)}
.u075-f.bad .u075-bar i{background:var(--ux-danger)}
.u075-f.bad .u075-fx b small{color:var(--ux-danger)}
.u075-st{width:30px;height:30px;flex:none;display:grid;place-items:center}
.u075-st svg{width:18px;height:18px;fill:none;stroke:var(--ux-mint);stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round}
.u075-st path{stroke-dasharray:18;stroke-dashoffset:18;transition:stroke-dashoffset .35s var(--ux-ease)}
.u075-f.ok .u075-st path{stroke-dashoffset:0}
.u075-retry{width:30px;height:30px;border:0;padding:0;border-radius:9px;display:grid;place-items:center;background:color-mix(in srgb,var(--ux-danger) 12%,transparent);color:var(--ux-danger);animation:u075-in .4s var(--ux-spring)}
.u075-retry svg{stroke:currentColor;width:16px;height:16px}
.u075-retry path{stroke-dasharray:none;stroke-dashoffset:0}
.u075-retry:focus-visible{outline:2px solid var(--ux-accent);outline-offset:2px}
@keyframes u075-in{from{transform:scale(0)}}

.u075{width:100%;height:auto}
</style>

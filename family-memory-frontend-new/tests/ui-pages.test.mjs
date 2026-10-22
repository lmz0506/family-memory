import test,{after,before} from 'node:test'
import assert from 'node:assert/strict'
import {createServer} from 'vite'
import {createSSRApp,h} from 'vue'
import {renderToString} from '@vue/server-renderer'
import {createRouter,createMemoryHistory} from 'vue-router'

let server
const memory={id:31,familyId:7,title:'一起看晚霞',authorName:'小林',memoryDate:'2026-08-10',createdAt:'2026-08-11',tags:['周末'],content:'测试故事',mediaUrls:['/test.jpg'],coverUrl:'/test.jpg',likesCount:3,commentsCount:2,liked:false,type:'photo',location:'湖边'}
const fixtures={family:{current:{id:7,name:'测试的小家',description:'一起把日子留住',photoCount:1,memoryCount:1,memberCount:1,capsuleCount:0,recordDays:20},members:[{id:1,userId:1,displayName:'小林',memoryCount:1,admin:true}],load:async()=>{},refresh:async()=>{}},auth:{user:{id:1,nickname:'小林'},logout(){}},memories:{items:[memory],total:1,loading:false,years:[2026],load:async()=>{},loadHighlights:async()=>{}}}
before(async()=>{
 server=await createServer({logLevel:'silent',server:{middlewareMode:true,hmr:false},plugins:[{name:'sky-ui-test-fixtures',enforce:'pre',resolveId(id){
  for(const name of ['family','auth','memories'])if(id===`@/stores/${name}`||[ `/src/stores/${name}`, `/src/stores/${name}.ts`].some(end=>id.replaceAll('\\','/').endsWith(end)))return `\0sky-store-${name}`
  if(id==='@/api'||['/src/api','/src/api/index','/src/api/index.ts'].some(end=>id.replaceAll('\\','/').endsWith(end)))return '\0sky-api'
 },load(id){
  if(id.startsWith('\0sky-store-')){const key=id.slice('\0sky-store-'.length),name={family:'Family',auth:'Auth',memories:'Memory'}[key];return `import {reactive} from 'vue';const store=reactive(globalThis.__skyFixtures[${JSON.stringify(key)}]);export const use${name}Store=()=>store;`}
  if(id==='\0sky-api')return `const unavailable=new Proxy({}, {get:()=>async()=>{throw Error('Network access is forbidden in UI render tests')}});export const memoryApi=unavailable,capsuleApi=unavailable,familyApi=unavailable,milestoneApi=unavailable,uploadApi=unavailable,universeApi=unavailable;`
 }}]})
 globalThis.__skyFixtures=fixtures
 globalThis.localStorage={getItem:()=>null,setItem(){},removeItem(){}}
 globalThis.window={innerWidth:1280,innerHeight:800,history:{state:{}},matchMedia:()=>({matches:false,addEventListener(){},removeEventListener(){}})}
 globalThis.document={hidden:false,activeElement:null,addEventListener(){},removeEventListener(){},querySelector(){return null}}
})
after(async()=>{await server?.close();delete globalThis.__skyFixtures;delete globalThis.window;delete globalThis.document;delete globalThis.localStorage})
async function render(file,props={},name='album',query={}){
 const {default:component}=await server.ssrLoadModule(`/src/${file}`)
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/',name:'home',component:{render:()=>null}},{path:'/memories',name:'album',component:{render:()=>null}},{path:'/timeline',name:'timeline',component:{render:()=>null}},{path:'/vault',name:'vault',component:{render:()=>null}},{path:'/family',name:'family',component:{render:()=>null}},{path:'/memories/:id',name:'memory-detail',component:{render:()=>null}}]})
 await router.push({name,query});await router.isReady()
 const app=createSSRApp({render:()=>h(component,props)});app.use(router)
 const context={};const html=await renderToString(app,context)
 return html+Object.values(context.teleports||{}).join('')
}
test('content shell shares the star menu instead of separate desktop and mobile nav',async()=>{
 const html=await render('layouts/AppLayout.vue')
 assert.match(html,/sky-menu-trigger/);assert.match(html,/fm-shell-content/)
 assert.doesNotMatch(html,/class="fm-header"|class="fm-mobile-nav"/)
 assert.match(html,/测试的小家/)
})
test('wide album adapts without density switches, with scoped search and year controls',async()=>{
 const html=await render('views/HomeView.vue')
 assert.match(html,/一起看晚霞/);assert.doesNotMatch(html,/舒展|标准|紧凑|记忆卡片密度/);assert.match(html,/album-page/);assert.match(html,/memory-wall/)
 assert.match(html,/搜索回忆/);assert.match(html,/album-year/);assert.doesNotMatch(html,/album-tag/);assert.match(html,/网格视图/);assert.match(html,/时间轴/)
 assert.match(html,/搜索记忆标题/);assert.match(html,/album-author/);assert.match(html,/所有作者/)
})

test('author options include family members who have not published memories',async()=>{
 const original=fixtures.family.members
 try{
  fixtures.family.members=[...original,{id:902,userId:47,displayName:'尚未记录的家人',memoryCount:0,admin:false}]
  const html=await render('views/HomeView.vue')
  assert.match(html,/尚未记录的家人/)
 }finally{fixtures.family.members=original}
})
test('memory card retains separate photo, story, like and reply entry points',async()=>{
 const html=await render('components/MemoryCard.vue',{memory})
 assert.match(html,/预览第1张照片：一起看晚霞/);assert.match(html,/喜欢这段记忆/)
 assert.match(html,/href="\/memories\/31"/);assert.match(html,/查看2条回应/)
 assert.doesNotMatch(html,/bg-white|text-warm-/)
})
test('family workspace keeps members, invitation and milestone creation',async()=>{
 const html=await render('views/FamilyView.vue',{},'family')
 assert.match(html,/小林/);assert.match(html,/邀请家人/);assert.match(html,/添加纪念日提醒/)
 assert.match(html,/family-workspace/);assert.match(html,/地点地图尚未开放/)
})
test('custom filter and confirmation render dark shared controls with keyboard semantics',async()=>{
 const select=await render('components/SkySelect.vue',{id:'year',label:'年份',modelValue:'2026',options:[{value:'2026',label:'2026年'}]})
 assert.match(select,/role="listbox"/);assert.match(select,/aria-selected="true"/);assert.doesNotMatch(select,/<select/)
 const confirm=await render('components/ConfirmDialog.vue',{open:true,title:'删除提醒',description:'确认删除这条提醒？'})
 assert.match(confirm,/role="alertdialog"/);assert.match(confirm,/保留/);assert.match(confirm,/确认删除/);assert.match(confirm,/sky-dialog/)
})

test('timeline is an album view sharing its search and year controls',async()=>{
 const timeline=await render('views/HomeView.vue',{},'album',{view:'timeline'})
 assert.match(timeline,/album-timeline/);assert.match(timeline,/album-year/);assert.match(timeline,/一起看晚霞/);assert.match(timeline,/2026/);assert.match(timeline,/段已显示/)
 const vault=await render('views/VaultView.vue',{},'vault')
 assert.match(vault,/封存一封信/);assert.match(vault,/vault-state/);assert.doesNotMatch(vault,/<select/)
})

test('photo memories expose every thumbnail in a grid and text-only stories have no fake media frame',async()=>{
 const gallery=await render('components/MemoryCard.vue',{memory:{...memory,mediaUrls:['/a.jpg','/b.jpg','/c.jpg','/d.mp4'],coverUrl:''}})
 assert.match(gallery,/memory-media-grid/);assert.match(gallery,/--media-columns:2/)
 assert.match(gallery,/预览第1张照片/);assert.match(gallery,/预览第3张照片/);assert.match(gallery,/预览第4个视频/)
 assert.doesNotMatch(gallery,/上一张照片|下一张照片/)
 const note=await render('components/MemoryCard.vue',{memory:{...memory,mediaUrls:[],coverUrl:'',content:memory.title}})
 assert.doesNotMatch(note,/class="memory-media-grid|一段文字记忆/)
 assert.match(note,/is-note/);assert.doesNotMatch(note,/class="memory-excerpt"/)
 assert.doesNotMatch(note,/生活手记|一段留声|memory-note-date/)
 assert.match(note,/datetime="2026-08-10"/);assert.match(note,/小林/)
 assert.match(note,/喜欢这段记忆/);assert.match(note,/查看2条回应/)
})

test('capsule presentation never reveals locked content and remembers a legitimately read letter',async()=>{
 const {capsuleReadPhase,canReadCapsule}=await server.ssrLoadModule('/src/composables/capsulePresentation.ts')
 for(const seen of [false,true])for(const reduced of [false,true]){
  assert.equal(capsuleReadPhase(false,seen,reduced),'locked')
  assert.equal(canReadCapsule(false,'read'),false)
 }
 assert.equal(capsuleReadPhase(true,false,false),'ready')
 assert.equal(capsuleReadPhase(true,false,true),'read')
 const capsule={id:998,title:'写给未来',recipient:'我们',openDate:'2028-01-01',createdAt:'2026-01-01',creatorName:'小林',daysRemaining:300,opened:false,content:'仅开启后可见的正文'}
 const locked=await render('components/CapsuleLetter.vue',{capsule})
 assert.doesNotMatch(locked,/仅开启后可见的正文/)
 assert.match(locked,/查看开启状态/)
 const ready=await render('components/CapsuleLetter.vue',{capsule:{...capsule,opened:true}})
 assert.match(ready,/轻启封印/);assert.doesNotMatch(ready,/仅开启后可见的正文/)
 const {writeViewState,clearViewStates}=await server.ssrLoadModule('/src/composables/viewState.ts')
 try{
  writeViewState('capsule-read:998',true)
  const read=await render('components/CapsuleLetter.vue',{capsule:{...capsule,opened:true}})
  assert.match(read,/仅开启后可见的正文/)
 }finally{clearViewStates()}
})

test('family counts render actual initial values and the shared drawer has dialog semantics',async()=>{
 const count=await render('components/CountUp.vue',{value:8})
 assert.match(count,/aria-hidden="true"[^>]*>8/);assert.match(count,/class="sr-only"[^>]*>8/)
 const drawer=await render('components/SkyDrawer.vue',{modelValue:true,title:'家庭记录'})
 assert.match(drawer,/role="dialog"/);assert.match(drawer,/aria-modal="true"/)
 assert.match(drawer,/aria-label="家庭记录"/);assert.match(drawer,/关闭详情/)
})

test('cards cap thumbnails at nine while the shared viewer exposes the complete gallery',async()=>{
 const media=Array.from({length:12},(_,i)=>`/photo-${i+1}.jpg`)
 const card=await render('components/MemoryCard.vue',{memory:{...memory,title:'一个需要在卡片里保持单行的完整长标题',mediaUrls:media,coverUrl:''}})
 assert.equal((card.match(/class="memory-media-tile"/g)||[]).length,9)
 assert.match(card,/class="memory-media-more"[^>]*>\+3/)
 assert.doesNotMatch(card,/预览第10张照片/)
 assert.match(card,/<h2[^>]*title="一个需要在卡片里保持单行的完整长标题"/)
 const viewer=await render('components/MediaViewer.vue',{open:true,media,index:10,title:'完整相册'})
 assert.match(viewer,/11 \/ 12/)
 assert.equal((viewer.match(/class="viewer-thumbnail"/g)||[]).length,12)
 assert.match(viewer,/查看第12张照片/)
 assert.match(viewer,/aria-current="true"/)
 assert.match(viewer,/全部图片与视频缩略图/)
 assert.match(viewer,/关闭照片浏览/)
})

test('reference card variants keep accurate dates, creators, titles and a shared footer',async()=>{
 const text=await render('components/MemoryCard.vue',{memory:{...memory,mediaUrls:[],coverUrl:''}})
 const photo=await render('components/MemoryCard.vue',{memory})
 assert.match(text,/is-note/);assert.doesNotMatch(text,/随笔与思考/);assert.doesNotMatch(text,/memory-media-frame/)
 assert.match(photo,/memory-photo-date/);assert.match(photo,/memory-media-frame/)
 for(const html of [text,photo]){assert.match(html,/2026-08-10/);assert.match(html,/小林/);assert.match(html,/一起看晚霞/);assert.match(html,/#周末/);assert.match(html,/memory-post-footer/)}
 assert.ok(photo.indexOf('memory-media-frame')<photo.indexOf('memory-card-header'))
 assert.ok(photo.indexOf('memory-media-grid')<photo.indexOf('memory-post-tags'))
 assert.ok(text.indexOf('memory-excerpt')<text.indexOf('memory-post-tags'))
 assert.ok(text.indexOf('memory-post-tags')<text.indexOf('memory-post-actions'))
 assert.doesNotMatch(text,/lucide-quote/)
 const header=photo.slice(photo.indexOf('<header class="memory-card-header"'),photo.indexOf('</header>'))
 assert.doesNotMatch(header,/<time/);assert.match(header,/小林/)
})

test('unavailable media has one central star, a caption and an accessible retry control',async()=>{
 const placeholder=await render('components/MemoryMediaPlaceholder.vue')
 assert.match(placeholder,/memory-placeholder-star/)
 assert.match(placeholder,/memory-placeholder-caption/)
 assert.match(placeholder,/aria-label="重新加载这段记忆的影像"/)
 assert.doesNotMatch(placeholder,/<time/)
})

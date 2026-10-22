import * as THREE from 'three'
import {OrbitControls} from 'three/examples/jsm/controls/OrbitControls.js'
import {EffectComposer} from 'three/examples/jsm/postprocessing/EffectComposer.js'
import {RenderPass} from 'three/examples/jsm/postprocessing/RenderPass.js'
import {UnrealBloomPass} from 'three/examples/jsm/postprocessing/UnrealBloomPass.js'
import {ShaderPass} from 'three/examples/jsm/postprocessing/ShaderPass.js'
import {meteorWakeShader,wakeLifetime,wakeLimit,wakeProjection,type MeteorWake} from './meteorWake'
import {OutputPass} from 'three/examples/jsm/postprocessing/OutputPass.js'
import {randomFromKey, universeColor, type UniverseNode} from './memoryUniverse'
import {ConstellationLinks} from './ConstellationLinks'
import {overviewMagnification,calendarMagnification,contentCameraDistance} from './cameraFraming'
import {seaRippleGLSL,seaRippleOffset} from './seaRipple'
import {NebulaVolume} from './NebulaVolume'

export type ProjectedStar = {node: UniverseNode; x: number; y: number; visible: boolean; ancestor?: boolean; label?:{x:number;y:number}}
type Callbacks = {project: (stars: ProjectedStar[]) => void; hover: (key: string | null) => void; pick: (node: UniverseNode) => void; blank: () => void; failed: () => void}

export class UniverseScene {
  private renderer: THREE.WebGLRenderer
  private scene = new THREE.Scene()
  private camera = new THREE.PerspectiveCamera(48, 1, .1, 2400)
  private controls: OrbitControls
  private composer: EffectComposer
  private wakePass:ShaderPass
  private wakes:MeteorWake[]=[]
  private bloom: UnrealBloomPass
  private group = new THREE.Group()
  private timer = new THREE.Timer()
  private observer: ResizeObserver
  private visibility: IntersectionObserver
  private points: THREE.Points<THREE.BufferGeometry, THREE.ShaderMaterial>
  private backdrop: THREE.Points<THREE.BufferGeometry, THREE.ShaderMaterial>
  private cloud: THREE.Mesh<THREE.PlaneGeometry, THREE.ShaderMaterial>
  private volume!: NebulaVolume
  private dust: THREE.Points<THREE.BufferGeometry, THREE.ShaderMaterial>
  private links: ConstellationLinks
  private parent?: UniverseNode
  private contextNodes: UniverseNode[] = []
  private selected: string|null = null
  private candidates: UniverseNode[] = []
  private projected: ProjectedStar[] = []
  private activeKeys = new Set<string>()
  private expandedNodes: UniverseNode[] = []
  private matches: Set<number> | null = null
  private matchingKeys:Set<string>|null=null
  private targetAlphas:Float32Array
  private sizeVariants:Float32Array
  private reduced = false
  private paused = false
  private reading = false
  private focusMix=0
  private hoverTimer?:ReturnType<typeof setTimeout>
  private pendingHover:string|null|undefined
  private hovering: string | null = null
  private interacting = false
  private userGesture=false
  private overview = true
  private onScreen = true
  private destroyed = false
  private idleUntil = 0
  private time = 0
  private effectsTime = 0
  private rippleAge = 100
  private rippleCenter = new THREE.Vector3()
  private lastFrame = 0
  private lastProjection = 0
  private elapsedMotion = 0
  private width = 1
  private height = 1
  private low = false
  private lowOverride: boolean | undefined
  private fitRadius = 80
  private focusRadius = 80
  private focusPosition = new THREE.Vector3()
  private tween?: {start: number; duration: number; from: THREE.Vector3; to: THREE.Vector3; fromTarget: THREE.Vector3; toTarget: THREE.Vector3; arc: number}
  private point = new THREE.Vector3()
  private down = {x: 0, y: 0, distance: 0}
  private flightWaiters: Array<(arrived:boolean)=>void> = []

  constructor(private host: HTMLElement, private nodes: UniverseNode[], private callbacks: Callbacks) {
    this.targetAlphas=new Float32Array(nodes.length)
    this.sizeVariants=new Float32Array(nodes.map(node=>.92+randomFromKey(`size:${node.key}`)()*.16))
    this.scene.background = new THREE.Color('#030611')
    this.renderer = new THREE.WebGLRenderer({antialias: false, powerPreference: 'default'})
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping
    this.renderer.toneMappingExposure = .95
    this.renderer.domElement.className = 'universe-canvas'
    this.renderer.domElement.setAttribute('aria-label', '可拖动旋转和缩放的记忆星海')
    this.host.appendChild(this.renderer.domElement)
    this.controls = new OrbitControls(this.camera, this.renderer.domElement)
    this.controls.enableDamping = true
    this.controls.dampingFactor = .075
    this.controls.rotateSpeed = .35
    this.controls.zoomSpeed = .65
    this.controls.zoomToCursor = true
    this.controls.minDistance = 10
    this.controls.maxDistance = 1200
    this.controls.enablePan = true
    this.controls.addEventListener('start', this.interactionStart)
    this.controls.addEventListener('end', this.interactionEnd)
    this.scene.add(this.group)
    this.points = this.createPoints()
    this.backdrop = this.createBackground()
    this.dust = this.createDust()
    this.cloud = this.createCloud()
    this.links = new ConstellationLinks(nodes)
    this.group.add(this.points, this.dust, this.backdrop, this.links)
    this.scene.add(this.cloud)
    const target=new THREE.WebGLRenderTarget(1,1,{type:THREE.HalfFloatType,samples:Math.min(4,this.renderer.capabilities.maxSamples)})
    this.composer = new EffectComposer(this.renderer,target)
    this.composer.addPass(new RenderPass(this.scene, this.camera))
    this.bloom = new UnrealBloomPass(new THREE.Vector2(1, 1), .32, .35, 1.1)
    this.composer.addPass(this.bloom)
    this.wakePass=new ShaderPass(meteorWakeShader)
    this.wakePass.uniforms.uWakes.value=Array.from({length:wakeLimit},()=>new THREE.Vector4())
    this.composer.addPass(this.wakePass)
    this.composer.addPass(new OutputPass())
    for (const node of nodes.filter(n => n.count)) this.fitRadius = Math.max(this.fitRadius, new THREE.Vector3(...node.position).length() + 12)
    this.focusRadius = this.fitRadius
    this.observer = new ResizeObserver(this.resize)
    this.observer.observe(host)
    this.visibility = new IntersectionObserver(entries => {this.onScreen = Boolean(entries[0]?.isIntersecting); this.updateVisibility()})
    this.visibility.observe(host)
    this.timer.connect(document)
    this.resize()
    const distance=this.fitDistance(this.fitRadius)
    this.camera.position.set(distance*.46,distance*.18,distance*.87)
    const canvas = this.renderer.domElement
    canvas.addEventListener('pointerdown', this.pointerDown)
    canvas.addEventListener('pointermove', this.pointerMove)
    canvas.addEventListener('pointerleave', this.pointerLeave)
    canvas.addEventListener('click', this.click)
    canvas.addEventListener('webglcontextlost', this.contextLost)
    document.addEventListener('visibilitychange', this.updateVisibility)
    this.renderer.setAnimationLoop(this.render)
  }

  private createPoints(sharedMaterial?:THREE.ShaderMaterial) {
    const positions: number[] = [], colors: number[] = [], phases: number[] = []
    this.nodes.forEach((node, i) => {
      positions.push(...node.position)
      const color = new THREE.Color(node.color)
      colors.push(color.r, color.g, color.b)
      phases.push(randomFromKey(node.key)() * Math.PI * 2)
    })
    const geometry = new THREE.BufferGeometry()
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
    geometry.setAttribute('aPhase', new THREE.Float32BufferAttribute(phases, 1))
    geometry.setAttribute('aKind',new THREE.Float32BufferAttribute(this.nodes.map(node=>node.kind==='capsule'?2:node.kind==='memory'?1:0),1))
    geometry.setAttribute('aAlpha', new THREE.Float32BufferAttribute(new Float32Array(this.nodes.length), 1))
    geometry.setAttribute('aSize', new THREE.Float32BufferAttribute(new Float32Array(this.nodes.length), 1))
    geometry.setAttribute('aEmphasis', new THREE.Float32BufferAttribute(new Float32Array(this.nodes.length), 1))
    const material = sharedMaterial || new THREE.ShaderMaterial({
      vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, toneMapped: false,
      uniforms: {uTime: {value: 0}, uDpr: {value: 1},uDepthCenter:{value:200},uMagnification:{value:1},uRippleCenter:{value:new THREE.Vector3()},uRippleAge:{value:100}},
      vertexShader: `
        attribute float aKind;varying float vKind;attribute float aAlpha; attribute float aSize; attribute float aPhase; attribute float aEmphasis;
        uniform float uTime; uniform float uDpr;uniform float uDepthCenter;uniform float uMagnification;${seaRippleGLSL}
        varying vec3 vColor; varying float vAlpha; varying float vEmphasis; varying float vTwinkle;varying float vDepthFade;
        void main(){
          vKind=aKind;vColor=color;vAlpha=aAlpha;vEmphasis=aEmphasis;
          float leaf=step(.5,aKind);

          vTwinkle=1.0+sin(uTime*(1.15+.3*sin(aPhase))+aPhase)*mix(.07,.25,leaf)+sin(uTime*.63+aPhase*2.7)*.07;
          vec4 mv=modelViewMatrix*vec4(seaRipple(position),1.0);
          vDepthFade=clamp(1.0+(uDepthCenter/max(1.0,-mv.z)-1.0)*2.2,.55,1.5);
          gl_PointSize=min(48.0*uDpr,aSize*uDpr*vDepthFade*uMagnification*(1.0+(vTwinkle-1.0)*.45+aEmphasis*.28));
          gl_Position=projectionMatrix*mv;
        }`,
      fragmentShader: `
        varying float vKind;varying vec3 vColor; varying float vAlpha; varying float vEmphasis; varying float vTwinkle;varying float vDepthFade;
        void main(){
          vec2 p=gl_PointCoord-.5;float r=mix(length(p),(abs(p.x)+abs(p.y))*.82,step(1.5,vKind));if(r>.5||vAlpha<.002)discard;
          float aa=max(fwidth(r),.004);float halo=exp(-r*r*32.0);float core=1.0-smoothstep(.035-aa,.105+aa,r);
          float rays=(exp(-abs(p.x)*100.0-abs(p.y)*14.0)+exp(-abs(p.y)*100.0-abs(p.x)*14.0))*.1;
          vec3 light=vColor*(.85+core*.75+vEmphasis*.85)*vTwinkle;
          gl_FragColor=vec4(light,(halo*(.18+vEmphasis*.55)+core+rays*(.8+vEmphasis))*vAlpha*clamp(vDepthFade,.65,1.1));
        }`
    })
    const points = new THREE.Points(geometry, material)
    points.frustumCulled = false
    points.renderOrder=3
    return points
  }

  private createBackground() {
    const rng = randomFromKey('one-shared-universe')
    const positions: number[] = [], colors: number[] = [], sizes: number[] = [], phases: number[] = []
    for (let i = 0; i < 16000; i++) {
      const theta = rng() * Math.PI * 2, z = rng() * 2 - 1, r = 430 + rng() * 430, planar = Math.sqrt(1 - z * z)
      positions.push(Math.cos(theta) * planar * r, Math.sin(theta) * planar * r, z * r)
      const color = new THREE.Color(universeColor(`sky:${i}`)).multiplyScalar(.5 + rng() * .65)
      colors.push(color.r, color.g, color.b)
      const bright = rng()
      sizes.push(bright > .994 ? 19 + rng()*5 : bright > .9 ? 7 + rng()*5 : 2.6 + rng() * 3.2)
      phases.push(rng() * 6.28)
    }
    return this.decorativePoints(positions, colors, sizes, phases, false)
  }

  private createDust() {
    const rng = randomFromKey('shared-stardust-stream')
    const positions: number[] = [], colors: number[] = [], sizes: number[] = [], phases: number[] = []
    // One loose stream through the whole sea, independent of the number or identity of years.
    for (let i = 0; i < 2200; i++) {
      const x = (rng() - .5) * 300
      const spread = Math.sqrt(-2 * Math.log(Math.max(.0001, rng())))
      const angle = rng() * Math.PI * 2
      positions.push(x, Math.sin(x * .018) * 28 + Math.cos(angle) * spread * 16, Math.sin(angle) * spread * 16 - 28)
      const color = new THREE.Color(universeColor(`dust:${i}`)).multiplyScalar(.3 + rng() * .4)
      colors.push(color.r, color.g, color.b)
      sizes.push(rng() > .98 ? 7 : 1.1 + rng() * 2)
      phases.push(rng() * 6.28)
    }
    return this.decorativePoints(positions, colors, sizes, phases, true)
  }

  private decorativePoints(positions: number[], colors: number[], sizes: number[], phases: number[], nearby: boolean) {
    const geometry = new THREE.BufferGeometry()
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
    geometry.setAttribute('aSize', new THREE.Float32BufferAttribute(sizes, 1))
    geometry.setAttribute('aPhase', new THREE.Float32BufferAttribute(phases, 1))
    const material = new THREE.ShaderMaterial({
      uniforms: {uTime: {value: 0}, uDpr: {value: 1}, uNearby: {value: nearby ? 1 : 0},uFocus:{value:1},uRippleCenter:{value:this.rippleCenter},uRippleAge:{value:100}},
      vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, toneMapped: false,
      vertexShader: `${seaRippleGLSL} attribute float aSize;attribute float aPhase;uniform float uTime;uniform float uDpr;uniform float uNearby;varying vec3 vColor;varying float vAlpha;
        void main(){vColor=color;vAlpha=.62+sin(uTime*(.16+.08*sin(aPhase))+aPhase)*.035;vec3 p=seaRipple(position);p.y+=sin(uTime*.12+aPhase)*uNearby*.8;vec4 mv=modelViewMatrix*vec4(p,1.0);gl_PointSize=clamp(aSize*uDpr*260.0/max(90.0,-mv.z),1.1*uDpr,20.0);gl_Position=projectionMatrix*mv;}`,
      fragmentShader: `uniform float uFocus;varying vec3 vColor;varying float vAlpha;void main(){float r=length(gl_PointCoord-.5);if(r>.5)discard;float core=exp(-r*r*110.0);float halo=exp(-r*r*18.0)*.09;float edge=1.0-smoothstep(.35,.5,r);gl_FragColor=vec4(vColor*.85,(halo+core*.52)*vAlpha*edge*uFocus);}`
    })
    const points = new THREE.Points(geometry, material)
    points.frustumCulled = false
    return points
  }

  private createCloud() {
    this.volume = new NebulaVolume(this.renderer)
    const material = new THREE.ShaderMaterial({
      uniforms: {uVolume: {value: this.volume.texture},uFocus:{value:1},uRippleScreen:{value:new THREE.Vector2(.5,.5)},uRippleAge:{value:100},uAspect:{value:1}},depthTest:false,depthWrite:false,toneMapped:false,
      vertexShader: `varying vec2 vUv;void main(){vUv=uv;gl_Position=vec4(position.xy,1.0,1.0);}`,
      fragmentShader: `uniform sampler2D uVolume;uniform float uFocus;uniform vec2 uRippleScreen;uniform float uRippleAge;uniform float uAspect;varying vec2 vUv;void main(){vec2 uv=vUv;
        if(uRippleAge>0.0&&uRippleAge<4.8){vec2 delta=(uv-uRippleScreen)*vec2(uAspect,1.0);float d=length(delta),front=d-uRippleAge*.19;float envelope=exp(-pow(front/.12,2.0))*pow(1.0-uRippleAge/4.8,2.0)*min(1.0,uRippleAge/.2);uv-=normalize(delta+vec2(.0001))*sin(front*85.0)*envelope*.022/vec2(uAspect,1.0);}
        vec3 sky=vec3(.0007,.0012,.0035);gl_FragColor=vec4(sky+(texture2D(uVolume,uv).rgb-sky)*uFocus,1.0);}`
    })
    const mesh=new THREE.Mesh(new THREE.PlaneGeometry(2,2),material)
    mesh.frustumCulled=false;mesh.renderOrder=-100
    return mesh
  }

  setLevel(candidates: UniverseNode[], parent?: UniverseNode,fly=true) {
    if(!fly){this.tween=undefined;this.finishFlight(false)}
    this.links.showSearchResults(false)
    this.candidates = candidates
    this.activeKeys = new Set(candidates.map(node => node.key))
    this.overview = !parent
    this.parent = parent
    this.contextNodes=[]
    let ancestor=parent
    while(ancestor){this.contextNodes.push(ancestor);ancestor=ancestor.parent?this.nodes.find(node=>node.key===ancestor!.parent):undefined}
    this.holdHover()
    this.selected = null
    this.hovering = null
    this.callbacks.hover(null)
    const framing=[...this.contextNodes,...candidates.flatMap(node=>this.links.subtree(node.key))].map(node=>new THREE.Vector3(...node.position))
    if(parent && framing.length){
      new THREE.Box3().setFromPoints(framing).getCenter(this.focusPosition)
      this.focusRadius=Math.max(9,...framing.map(p=>p.distanceTo(this.focusPosition)+5))
    }else{this.focusPosition.set(0,0,0);this.focusRadius=this.fitRadius}
    this.links.setLevel(candidates,parent)
    this.updateNodes()
    if(fly)this.flyTo(this.focusPosition, this.focusRadius, parent?calendarMagnification[parent.kind]:overviewMagnification)
  }

  /** 增量替换有界节点集合；保留渲染器、镜头和已有节点的亮度。 */
  setNodes(nodes:UniverseNode[]){
    const old=this.points,alpha=old.geometry.getAttribute('aAlpha') as THREE.BufferAttribute
    const previous=new Map(this.nodes.map((node,i)=>[node.key,alpha.getX(i)]))
    this.nodes=nodes;this.targetAlphas=new Float32Array(nodes.length)
    this.sizeVariants=new Float32Array(nodes.map(node=>.92+randomFromKey(`size:${node.key}`)()*.16))
    this.points=this.createPoints(old.material)
    const next=this.points.geometry.getAttribute('aAlpha') as THREE.BufferAttribute
    nodes.forEach((node,i)=>next.setX(i,previous.get(node.key)||0));next.needsUpdate=true
    this.group.remove(old);old.geometry.dispose();this.group.add(this.points)
    this.links.setNodes(nodes)
    const map=new Map(nodes.map(node=>[node.key,node]))
    this.parent=this.parent?map.get(this.parent.key):undefined
    this.contextNodes=this.contextNodes.flatMap(node=>map.get(node.key)?[map.get(node.key)!]:[])
    this.candidates=nodes.filter(node=>this.parent?node.parent===this.parent.key:node.kind==='year')
    this.activeKeys=new Set(this.candidates.map(node=>node.key))
    if(this.hovering&&!map.has(this.hovering)){this.holdHover();this.hovering=null;this.links.hover(null);this.callbacks.hover(null)}
    if(this.selected&&!map.has(this.selected)){this.selected=null;this.links.select(null)}
    this.points.material.uniforms.uDpr.value=this.renderer.getPixelRatio()
    this.updateNodes();this.project()
  }
  /** 用完整概要预先确定全景范围，分批补入节点时不重新挪动镜头。 */
  setOverviewBounds(nodes:UniverseNode[]){this.fitRadius=Math.max(30,...nodes.map(node=>new THREE.Vector3(...node.position).length()+15));if(this.overview){this.focusPosition.set(0,0,0);this.focusRadius=this.fitRadius}}
  hasUserGesture(){return this.userGesture}
  setWarm(warm:boolean){this.volume.setWarm(warm)}
  screenPosition(key:string){this.project();const point=this.projected.find(item=>item.node.key===key);return point?{x:point.x,y:point.y,visible:point.visible}:null}
  private nodeMatches(node:UniverseNode){return this.matches===null||Boolean(this.matchingKeys?.has(node.key))||node.ids.some(id=>this.matches!.has(id))}

  private updateNodes() {
    const revealed=this.links.revealedNodes()
    this.expandedNodes=[...revealed.selected,...revealed.ancestors]
    const pathKeys=new Set(revealed.ancestors.map(node=>node.key))
    const selectedKeys=new Set(revealed.selected.map(node=>node.key)),hoverKeys=new Set(revealed.hovered.map(node=>node.key))
    const size = this.points.geometry.getAttribute('aSize') as THREE.BufferAttribute
    const emphasis = this.points.geometry.getAttribute('aEmphasis') as THREE.BufferAttribute
    this.nodes.forEach((node, i) => {
      const active = this.activeKeys.has(node.key)
      const expanded=selectedKeys.has(node.key),preview=hoverKeys.has(node.key)
      const matched = this.nodeMatches(node)
      const isParent=node.key===this.parent?.key
      const isContext=pathKeys.has(node.key)||this.contextNodes.some(item=>item.key===node.key)
      const dimmed=Boolean(this.hovering)&&selectedKeys.size>0&&!preview&&!isContext
      const ambient=node.count?(matched?node.kind==='year'?.65:.48:.06):0
      this.targetAlphas[i]=(preview ? 1 : expanded ? .9 : pathKeys.has(node.key) ? .72 : active ? node.count ? matched ? 1 : .08 : 0 : isParent ? .65 : isContext?.6:ambient)*(dimmed?.55:1)
      // A narrow seeded variation preserves the year > month > day > memory hierarchy.
      const base={year:30,month:25,day:22,memory:27,capsule:29}[node.kind]
      const richness=1+Math.min(Math.log2(Math.max(1,node.count)),4)*.02
      size.setX(i,(node.count ? base*richness : 0)*(this.sizeVariants[i]||1))
      emphasis.setX(i, node.key === this.hovering ? 1 : node.key===this.selected ? 1.2 : 0)
    })
    size.needsUpdate = emphasis.needsUpdate = true
  }

  setMatches(ids: Set<number> | null,keys:Set<string>|null=null) {this.matches = ids;this.matchingKeys=keys;if(ids===null)this.links.showSearchResults(false);this.links.setMatches(ids,keys);this.updateNodes()}
  focusMatches(ids:Set<number>,keys:Set<string>|null=null){
    this.holdHover();this.highlight(null);this.callbacks.hover(null)
    this.selected=null;this.links.select(null)
    this.matches=ids;this.matchingKeys=keys;this.links.setMatches(ids,keys);this.links.showSearchResults(true)
    this.updateNodes()
    const revealed=this.links.revealedNodes(),points=[...revealed.selected,...revealed.ancestors].map(node=>new THREE.Vector3(...node.position))
    if(!points.length)return
    new THREE.Box3().setFromPoints(points).getCenter(this.focusPosition)
    this.focusRadius=Math.max(12,...points.map(point=>point.distanceTo(this.focusPosition)+5))
    this.overview=false;this.flyTo(this.focusPosition,this.focusRadius)
  }
  selectNode(key:string|null){this.selected=key;this.links.select(key);this.updateNodes()}
  selectMemory(id:number|null){if(id!==null)this.selectNode(`memory:${id}`)}
  clearSelection(){this.holdHover();this.highlight(null);this.selectNode(null);this.callbacks.hover(null)}
  setPaused(paused: boolean) {this.paused = paused}
  setReduced(reduced: boolean) {this.reduced = reduced;if(reduced){this.rippleAge=100;this.wakes=[];} this.controls.enableDamping = !reduced}
  setReading(reading: boolean) {this.reading = reading; this.controls.enabled = !reading}
  setLowQuality(low: boolean) {this.lowOverride = low; this.resize()}
  holdHover(){if(this.hoverTimer)clearTimeout(this.hoverTimer);this.hoverTimer=undefined;this.pendingHover=undefined}
  requestHover(key:string|null){
    if(key===this.hovering){this.holdHover();return}
    if(key===this.pendingHover)return
    this.holdHover();this.pendingHover=key
    this.hoverTimer=setTimeout(()=>{this.hoverTimer=undefined;this.pendingHover=undefined;if(this.destroyed)return;this.highlight(key);this.callbacks.hover(key)},key?120:180)
  }
  highlight(key: string | null) {
    if (key === this.hovering) return
    this.hovering = key
    this.links.hover(key)

    this.idleUntil = performance.now() + 1000
    this.updateNodes()
  }

  private fitDistance(radius: number) {
    const tangent=Math.tan(THREE.MathUtils.degToRad(24))
    const available=this.height*(this.width<768?.68:.82)
    const angle = Math.min(Math.atan(tangent*available/Math.max(1,this.height)), Math.atan(tangent * this.camera.aspect))
    return Math.max(20, radius / Math.sin(angle) * 1.18)
  }
  private flyTo(position: THREE.Vector3, radius: number, magnification=1, distanceOverride?:number) {
    this.finishFlight(false)
    this.group.updateMatrixWorld(true)
    const target = position.clone().applyMatrix4(this.group.matrixWorld)
    const direction=this.camera.position.clone().sub(this.controls.target).normalize()
    if(direction.lengthSq()<.01)direction.set(0,0,1)
    this.controls.enableDamping=false;this.controls.update();this.controls.enableDamping=!this.reduced
    const to=target.clone().add(direction.multiplyScalar(distanceOverride??Math.max(12,this.fitDistance(radius)/magnification)))
    this.tween = {start: performance.now(), duration: this.reduced ? 0 : 1150, from: this.camera.position.clone(), to, fromTarget: this.controls.target.clone(), toTarget: target,arc:Math.min(12,this.camera.position.distanceTo(to)*.045)}
    this.idleUntil = performance.now() + 1250
  }
  captureView(){return {camera:this.camera.position.toArray(),target:this.controls.target.toArray(),rotation:[this.group.rotation.x,this.group.rotation.y,this.group.rotation.z],focus:this.focusPosition.toArray(),radius:this.focusRadius,overview:this.overview}}
  restoreView(view:ReturnType<UniverseScene['captureView']>){
    this.tween=undefined;this.finishFlight(false);this.controls.enableDamping=false;this.controls.update()
    this.camera.position.fromArray(view.camera);this.controls.target.fromArray(view.target);this.group.rotation.set(view.rotation[0]!,view.rotation[1]!,view.rotation[2]!);this.focusPosition.fromArray(view.focus);this.focusRadius=view.radius;this.overview=view.overview;this.userGesture=true
    this.controls.update();this.controls.enableDamping=!this.reduced;this.project()
  }
  recenter() {this.flyTo(this.focusPosition, this.focusRadius,this.overview?overviewMagnification:1.3)}
  settledFlight(){return this.tween?new Promise<boolean>(resolve=>this.flightWaiters.push(resolve)):Promise.resolve(true)}
  private finishFlight(arrived:boolean){const waiters=this.flightWaiters;this.flightWaiters=[];for(const resolve of waiters)resolve(arrived)}
  wakeAt(x:number,y:number){
    if(this.reduced||this.paused||this.reading||document.hidden)return
    this.wakes.push({x:x/Math.max(1,this.width),y:1-y/Math.max(1,this.height),age:0})
    if(this.wakes.length>wakeLimit)this.wakes.shift()
  }
  ripple(key:string){if(this.reduced||document.hidden)return;const node=this.nodes.find(node=>node.key===key);if(!node)return;this.rippleCenter.set(...node.position);this.points.material.uniforms.uRippleCenter.value.copy(this.rippleCenter);this.rippleAge=0}

  preview(node: UniverseNode) {
    this.focusPosition.set(...node.position);this.focusRadius=node.kind==='year'?26:20
    const current=this.camera.position.distanceTo(this.controls.target)
    const distance=contentCameraDistance(current,this.fitDistance(this.focusRadius))
    this.flyTo(this.focusPosition,this.focusRadius,1,distance)
  }
  zoom(factor: number) {
    this.userGesture=true
    this.finishFlight(false)
    const offset=this.camera.position.clone().sub(this.controls.target)
    offset.setLength(THREE.MathUtils.clamp(offset.length()*factor,10,this.controls.maxDistance))
    this.tween={start:performance.now(),duration:this.reduced?0:420,from:this.camera.position.clone(),to:this.controls.target.clone().add(offset),fromTarget:this.controls.target.clone(),toTarget:this.controls.target.clone(),arc:0}
    this.idleUntil=performance.now()+1000
  }

  private resize = () => {
    if (this.destroyed) return
    this.width = this.host.clientWidth
    this.height = this.host.clientHeight
    this.low = this.lowOverride ?? this.width < 640
    this.camera.aspect = this.width / Math.max(this.height, 1)
    this.camera.setViewOffset(this.width,this.height,this.width>=768?Math.min(180,this.width*.18):0,this.width<768?this.height*.25:-20,this.width,this.height)
    this.camera.updateProjectionMatrix()
    const ratio = Math.min(window.devicePixelRatio, this.low ? 1 : 1.5)
    this.renderer.setPixelRatio(ratio)
    this.renderer.setSize(this.width, this.height, false)
    this.composer.setPixelRatio(ratio)
    this.composer.setSize(this.width, this.height)
    this.bloom.strength = this.low ? .22 : .32
    this.links.resize(this.width,this.height,ratio)
    for (const object of [this.points, this.backdrop, this.dust]) object.material.uniforms.uDpr.value = ratio
    this.volume.resize(this.width,this.height,this.low)
    if (this.candidates.length) this.recenter()
  }

  private project() {
    this.group.updateMatrixWorld(true)
    const output: ProjectedStar[] = []
    const targets=this.nodes.filter(node=>node.count)
    for (const node of targets) {
      const offset=seaRippleOffset(node.position,this.rippleCenter.toArray(),this.rippleAge)
      this.point.set(node.position[0]+offset[0],node.position[1]+offset[1],node.position[2]+offset[2]).applyMatrix4(this.group.matrixWorld).project(this.camera)
      const x=(this.point.x*.5+.5)*this.width,y=(this.point.y*.5+.5)*this.height
      const visible=Math.abs(this.point.x)<.95&&Math.abs(this.point.y)<.9&&this.point.z>-1&&this.point.z<1
      let label:ProjectedStar['label']
      const anchor=node.kind==='memory'||node.kind==='capsule'?undefined:this.links.labelAnchor(node.key)
      if(anchor){
        const p=anchor.point,offset=seaRippleOffset(p.toArray(),this.rippleCenter.toArray(),this.rippleAge)
        p.add(new THREE.Vector3(...offset)).applyMatrix4(this.group.matrixWorld).project(this.camera)
        if(p.z>-1&&p.z<1)label={x:(p.x*.5+.5)*this.width,y:(.5-p.y*.5)*this.height}
      }
      const displayed=wakeProjection(x,this.height-y,this.width,this.height,this.wakes)
      if(label)label=wakeProjection(label.x,label.y,this.width,this.height,this.wakes)
      output.push({node,x:displayed.x,y:displayed.y,visible,ancestor:this.contextNodes.includes(node),label})
    }
    this.projected = output
    this.callbacks.project(output)
  }
  nodeAt(event:MouseEvent){return this.pick(event)}
  private pick(event: MouseEvent & {pointerType?:string}) {
    const rect = this.renderer.domElement.getBoundingClientRect()
    const x = event.clientX - rect.left, y = event.clientY - rect.top
    const radius = event.pointerType === 'touch' || this.width < 640 ? 32 : 26
    let nearest: UniverseNode | null = null, distance = radius
    for (const item of this.projected) {
      if (!item.visible || !item.node.count || !this.nodeMatches(item.node)) continue
      const d = Math.hypot(item.x - x, item.y - y)
      if (d < distance) {nearest = item.node; distance = d}
    }
    return nearest
  }
  private pointerDown = (event: PointerEvent) => {this.down = {x: event.clientX, y: event.clientY, distance: 0}}
  private pointerMove = (event: PointerEvent) => {
    if (event.buttons || event.pointerType === 'touch') {this.down.distance = Math.max(this.down.distance, Math.hypot(event.clientX - this.down.x, event.clientY - this.down.y)); return}
    const node = this.pick(event)
    this.requestHover(node?.key || null)
    this.renderer.domElement.style.cursor = node ? 'pointer' : 'grab'
  }
  private pointerLeave = () => {this.requestHover(null)}
  private click = (event: PointerEvent) => {
    if (this.down.distance > 6 || this.reading) return
    const node = this.pick(event)
    if (node) this.callbacks.pick(node)
    else {this.clearSelection();this.callbacks.blank()}
  }
  private interactionStart = () => {this.userGesture=true;this.holdHover();this.interacting = true; this.tween = undefined; this.finishFlight(false);this.highlight(null); this.callbacks.hover(null)}
  private interactionEnd = () => {this.interacting = false; this.idleUntil = performance.now() + 2000}
  private contextLost = (event: Event) => {event.preventDefault(); this.renderer.setAnimationLoop(null);this.finishFlight(false);this.callbacks.failed()}
  private updateVisibility = () => {
    if (this.destroyed) return
    this.renderer.setAnimationLoop(document.hidden || !this.onScreen ? null : this.render)
    this.timer.reset()
    this.lastFrame = 0
  }

  private render = (now: number) => {
    if (this.destroyed || document.hidden || !this.onScreen) return
    if (this.low && now - this.lastFrame < 1000 / 30) return
    this.lastFrame = now
    this.timer.update()
    const delta = Math.min(this.timer.getDelta(), .06)
    const moving = !this.paused && !this.reduced && !this.reading && !this.interacting && !this.hovering && !this.selected
    if (!this.paused && !this.reduced && !this.reading) this.time += delta
    if (!this.paused && !this.reduced && !this.reading) this.effectsTime += delta
    if (moving && this.overview && now > this.idleUntil && !this.tween) {
      this.elapsedMotion += delta
      this.group.rotation.z += delta * .018
      this.group.rotation.y = Math.sin(this.elapsedMotion * .08) * .06
    }
    if (this.tween) {
      const p = this.tween.duration ? Math.min(1, (now - this.tween.start) / this.tween.duration) : 1
      const ease = 1 - Math.pow(1 - p, 3)
      this.camera.position.lerpVectors(this.tween.from, this.tween.to, ease)
      this.camera.position.y+=Math.sin(Math.PI*ease)*this.tween.arc
      this.controls.target.lerpVectors(this.tween.fromTarget, this.tween.toTarget, ease)
      if (p === 1) {this.tween = undefined;this.finishFlight(true)}
    }
    this.controls.update()
    const alpha=this.points.geometry.getAttribute('aAlpha') as THREE.BufferAttribute
    const blend=this.reduced?1:1-Math.exp(-delta*7)
    let changed=false
    for(let i=0;i<this.targetAlphas.length;i++){const value=alpha.getX(i),target=this.targetAlphas[i]!;if(Math.abs(value-target)>.001){alpha.setX(i,value+(target-value)*blend);changed=true}}
    if(changed)alpha.needsUpdate=true
    this.backdrop.rotation.y = this.time * .003
    this.backdrop.rotation.z = -this.time * .002
    for (const object of [this.points, this.backdrop, this.dust]) object.material.uniforms.uTime.value = this.time
    this.group.updateMatrixWorld(true)
    const depthCenter=this.camera.position.distanceTo(this.focusPosition.clone().applyMatrix4(this.group.matrixWorld))
    this.points.material.uniforms.uDepthCenter.value=depthCenter
    this.points.material.uniforms.uMagnification.value=THREE.MathUtils.clamp(Math.sqrt(this.fitDistance(this.fitRadius)/Math.max(12,this.camera.position.distanceTo(this.controls.target))),1,1.3)
    this.links.setDepth(depthCenter)
    this.focusMix=this.reduced?(this.selected?1:0):THREE.MathUtils.damp(this.focusMix,this.selected?1:0,3.5,delta)
    this.backdrop.material.uniforms.uFocus.value=1-this.focusMix*.42
    this.dust.material.uniforms.uFocus.value=1-this.focusMix*.55
    this.cloud.material.uniforms.uFocus.value=1-this.focusMix*.28
    if(!this.paused&&!this.reduced&&!this.reading)this.rippleAge+=delta
    for(const object of [this.points,this.dust,this.backdrop])object.material.uniforms.uRippleAge.value=this.reduced?100:this.rippleAge
    this.links.setRipple(this.rippleCenter,this.reduced?100:this.rippleAge)
    this.point.copy(this.rippleCenter).applyMatrix4(this.group.matrixWorld).project(this.camera)
    this.cloud.material.uniforms.uRippleScreen.value.set(this.point.x*.5+.5,this.point.y*.5+.5)
    this.cloud.material.uniforms.uRippleAge.value=this.reduced?100:this.rippleAge
    this.cloud.material.uniforms.uAspect.value=this.width/Math.max(1,this.height)
    this.points.material.uniforms.uTime.value = this.effectsTime
    this.links.update(this.effectsTime)
    this.group.updateMatrixWorld(true)
    this.volume.render(this.camera,this.group.matrixWorld,this.time,this.interacting||Boolean(this.tween))
    if(!this.paused&&!this.reduced&&!this.reading)for(const wake of this.wakes)wake.age+=delta
    this.wakes=this.wakes.filter(w=>w.age<wakeLifetime)
    this.wakePass.uniforms.uCount.value=this.wakes.length
    this.wakePass.uniforms.uAspect.value=this.width/Math.max(1,this.height)
    this.wakes.forEach((w,i)=>this.wakePass.uniforms.uWakes.value[i].set(w.x,w.y,w.age,1))
    this.composer.render(delta)
    if (now - this.lastProjection > (this.tween||this.rippleAge<4.8||this.wakes.length?32:60)) {this.project(); this.lastProjection = now}
  }

  dispose() {
    this.holdHover()
    this.destroyed = true
    this.finishFlight(false)
    this.renderer.setAnimationLoop(null)
    this.observer.disconnect()
    this.visibility.disconnect()
    this.timer.dispose()
    this.controls.dispose()
    this.controls.removeEventListener('start', this.interactionStart)
    this.controls.removeEventListener('end', this.interactionEnd)
    document.removeEventListener('visibilitychange', this.updateVisibility)
    const canvas = this.renderer.domElement
    canvas.removeEventListener('pointerdown', this.pointerDown)
    canvas.removeEventListener('pointermove', this.pointerMove)
    canvas.removeEventListener('pointerleave', this.pointerLeave)
    canvas.removeEventListener('click', this.click)
    canvas.removeEventListener('webglcontextlost', this.contextLost)
    this.links.dispose()
    this.volume.dispose()
    for (const object of [this.points, this.backdrop, this.dust, this.cloud]) {object.geometry.dispose(); object.material.dispose()}
    this.composer.passes.forEach(pass => pass.dispose())
    this.composer.dispose()
    this.renderer.dispose()
    canvas.remove()
  }
}

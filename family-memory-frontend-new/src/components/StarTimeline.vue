<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue'
import * as THREE from 'three'
import {OrbitControls} from 'three/examples/jsm/controls/OrbitControls.js'
import {ChevronRight, LoaderCircle, MapPin, MousePointer2, RotateCcw, UserRound} from 'lucide-vue-next'
import {memoryApi} from '@/api'
import type {Memory, TimelineMemoryStar, TimelineYearSummary} from '@/types'

const props = defineProps<{familyId: number}>()
type NodeType = 'year' | 'month' | 'day' | 'memory'
type StarNode = {
  key: string; type: NodeType; year: number; month?: number; day?: number; memoryId?: number; count: number;
  parentKey?: string; position: THREE.Vector3; color: THREE.Color; size: number; index: number
}

const host = ref<HTMLElement | null>(null)
const mountPoint = ref<HTMLElement | null>(null)
const loading = ref(true)
const error = ref('')
const overview = ref<TimelineYearSummary[]>([])
const memoryIndex = ref<TimelineMemoryStar[]>([])
const hovered = ref<StarNode | null>(null)
const pinned = ref<StarNode | null>(null)
const dismissedMemoryId = ref<number>()
const detailLoading = ref(false)
const detailCache = reactive(new Map<number, Memory>())
const cardX = ref(0)
const cardY = ref(0)
const viewportWidth = ref(1)
const viewportHeight = ref(1)
const labelPositions = ref<Record<string, {x: number; y: number; visible: boolean}>>({})
let detailTimer: ReturnType<typeof setTimeout> | undefined

let scene: THREE.Scene | undefined
let camera: THREE.PerspectiveCamera | undefined
let renderer: THREE.WebGLRenderer | undefined
let controls: OrbitControls | undefined
let dataGroup: THREE.Group | undefined
let dataPoints: THREE.Points<THREE.BufferGeometry, THREE.ShaderMaterial> | undefined
let connectionLines: THREE.LineSegments<THREE.BufferGeometry, THREE.ShaderMaterial> | undefined
let connectionGeometry: THREE.BufferGeometry | undefined
let clusterDust: THREE.Points<THREE.BufferGeometry, THREE.ShaderMaterial> | undefined
let clusterDustGeometry: THREE.BufferGeometry | undefined
let backgroundPoints: THREE.Points<THREE.BufferGeometry, THREE.ShaderMaterial> | undefined
let backgroundGeometry: THREE.BufferGeometry | undefined
let dataGeometry: THREE.BufferGeometry | undefined
let resizeObserver: ResizeObserver | undefined
let animationFrame = 0
let timer: THREE.Timer | undefined
let frameCount = 0
let pointerDownX = 0
let pointerDownY = 0
let pointerDragDistance = 0
let cameraTween: {
  startedAt: number; duration: number; fromPosition: THREE.Vector3; toPosition: THREE.Vector3;
  fromTarget: THREE.Vector3; toTarget: THREE.Vector3
} | undefined
const pointer = new THREE.Vector2(9, 9)
const pointerTarget = new THREE.Vector2()
const nodes: StarNode[] = []
const nodeByKey = new Map<string, StarNode>()
const yearCenters = new Map<number, THREE.Vector3>()
let baseCameraZ = 78

const effectiveNode = computed(() => hovered.value || pinned.value)
const activeMemoryId = computed(() => effectiveNode.value?.type === 'memory' ? effectiveNode.value.memoryId : undefined)
const cardMemory = computed(() => {
  const id = activeMemoryId.value
  return id && id !== dismissedMemoryId.value ? detailCache.get(id) : undefined
})
const breadcrumb = computed(() => {
  const node = pinned.value
  if (!node) return []
  const result = [`${node.year}`]
  if (node.type !== 'year') result.push(`${node.month}月`)
  if (node.type === 'day' || node.type === 'memory') result.push(`${node.day}日`)
  if (node.type === 'memory') result.push('一段记忆')
  return result
})
const cardStyle = computed(() => {
  const horizontal = cardX.value > viewportWidth.value * .68 ? '-108%' : cardX.value < viewportWidth.value * .32 ? '8%' : '-50%'
  const vertical = cardY.value > viewportHeight.value * .66 ? '-106%' : cardY.value < viewportHeight.value * .34 ? '8%' : '-50%'
  return {left: `${cardX.value}px`, top: `${cardY.value}px`, transform: `translate(${horizontal}, ${vertical})`}
})

function hash(value: string) {
  let result = 2166136261
  for (let index = 0; index < value.length; index++) {
    result ^= value.charCodeAt(index)
    result = Math.imul(result, 16777619)
  }
  return result >>> 0
}
function seeded(seed: number) {
  let value = seed
  return () => {
    value += 0x6D2B79F5
    let next = value
    next = Math.imul(next ^ next >>> 15, next | 1)
    next ^= next + Math.imul(next ^ next >>> 7, next | 61)
    return ((next ^ next >>> 14) >>> 0) / 4294967296
  }
}
function randomDirection(rng: () => number) {
  const theta = rng() * Math.PI * 2
  const z = rng() * 2 - 1
  const radius = Math.sqrt(1 - z * z)
  return new THREE.Vector3(radius * Math.cos(theta), radius * Math.sin(theta), z)
}

const naturalPalette = [
  0xb7cfff, 0xd8e2ff, 0xf4f7ff, 0xfff4dc, 0xffddb0, 0xffbca9, 0xe9c6ff, 0xbfe1dc, 0xffd1c7
]
function starColor(key: string) {
  const rng = seeded(hash(`color-${key}`))
  const first = new THREE.Color(naturalPalette[Math.floor(rng() * naturalPalette.length)])
  const second = new THREE.Color(naturalPalette[Math.floor(rng() * naturalPalette.length)])
  return first.lerp(second, .15 + rng() * .45).multiplyScalar(.9 + rng() * .22)
}

function buildNodes() {
  nodes.length = 0
  nodeByKey.clear()
  yearCenters.clear()
  const yearPositions = new Map<number, THREE.Vector3>()
  const monthPositions = new Map<string, THREE.Vector3>()
  const dayPositions = new Map<string, THREE.Vector3>()
  const yearCount = overview.value.length

  overview.value.forEach((summary, yearOrder) => {
    const rng = seeded(hash(`year-position-${summary.year}`))
    const angle = yearOrder * 2.399963 + rng() * .32
    const radial = yearCount <= 1 || yearOrder === 0 ? 0 : 68 * Math.sqrt(yearOrder)
    const position = new THREE.Vector3(
      Math.cos(angle) * radial,
      Math.sin(angle) * radial * .68,
      yearCount <= 1 ? 0 : (rng() - .5) * 18
    )
    yearPositions.set(summary.year, position)
    yearCenters.set(summary.year, position)
    nodes.push({
      key: `year-${summary.year}`, type: 'year', year: summary.year, count: summary.count, position,
      color: starColor(`year-${summary.year}`), size: Math.min(39, 28 + Math.log2(summary.count + 1) * 2.2), index: -1
    })

    const monthCounts = new Map(summary.months.map(month => [month.month, month.count]))
    const armOffset = rng() * Math.PI * 2
    const diskRatio = .58 + rng() * .12
    for (let month = 1; month <= 12; month++) {
      const monthRng = seeded(hash(`month-position-${summary.year}-${month}`))
      const spiralAngle = armOffset + month * 1.12 + (month % 2) * Math.PI
      const spiralRadius = 10 + month * 1.42 + monthRng() * 2.8
      const offset = new THREE.Vector3(
        Math.cos(spiralAngle) * spiralRadius,
        Math.sin(spiralAngle) * spiralRadius * diskRatio,
        Math.sin(spiralAngle * .55) * 4.5 + (monthRng() - .5) * 5
      )
      const monthPosition = position.clone().add(offset)
      monthPositions.set(`${summary.year}-${month}`, monthPosition)
      const count = monthCounts.get(month) || 0
      nodes.push({
        key: `month-${summary.year}-${month}`, type: 'month', year: summary.year, month, count,
        parentKey: `year-${summary.year}`,
        position: monthPosition, color: starColor(`month-${summary.year}-${month}`),
        size: count ? Math.min(23, 15 + Math.log2(count + 1) * 1.7) : 8.5, index: -1
      })
    }
  })

  const memoriesByDay = new Map<string, TimelineMemoryStar[]>()
  for (const memory of memoryIndex.value) {
    const day = memory.day || 1
    const key = `${memory.year}-${memory.month}-${day}`
    const list = memoriesByDay.get(key) || []
    list.push(memory)
    memoriesByDay.set(key, list)
  }

  for (const [key, memories] of memoriesByDay) {
    const sample = memories[0]
    if (!sample) continue
    const day = sample.day || 1
    const parent = monthPositions.get(`${sample.year}-${sample.month}`)
    if (!parent) continue
    const rng = seeded(hash(`day-position-${key}`))
    const position = parent.clone().add(randomDirection(rng).multiplyScalar(5.5 + rng() * 7.5))
    dayPositions.set(key, position)
    nodes.push({
      key: `day-${key}`, type: 'day', year: sample.year, month: sample.month, day,
      count: memories.length, parentKey: `month-${sample.year}-${sample.month}`, position,
      color: starColor(`day-${key}`), size: Math.min(18, 11 + Math.log2(memories.length + 1) * 1.55), index: -1
    })
  }

  for (const memory of memoryIndex.value) {
    const day = memory.day || 1
    const dayKey = `${memory.year}-${memory.month}-${day}`
    const parent = dayPositions.get(dayKey) || monthPositions.get(`${memory.year}-${memory.month}`) || yearPositions.get(memory.year)
    if (!parent) continue
    const rng = seeded(hash(`memory-position-${memory.id}`))
    const distance = 2.4 + rng() * 5.2
    const position = parent.clone().add(randomDirection(rng).multiplyScalar(distance))
    nodes.push({
      key: `memory-${memory.id}`, type: 'memory', year: memory.year, month: memory.month, day,
      parentKey: `day-${dayKey}`,
      memoryId: memory.id, count: memory.weight, position, color: starColor(`memory-${memory.id}`),
      size: Math.min(13, 7.5 + Math.log2(memory.weight + 2) * .95), index: -1
    })
  }
  nodes.forEach((node, index) => {
    node.index = index
    nodeByKey.set(node.key, node)
  })
}

function createBackground() {
  if (!scene) return
  const rng = seeded(20260811)
  const count = 6800
  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const opacities = new Float32Array(count)
  const phases = new Float32Array(count)
  const speeds = new Float32Array(count)
  for (let index = 0; index < count; index++) {
    const radius = 430 + rng() * 340
    const direction = randomDirection(rng)
    positions.set([direction.x * radius, direction.y * radius, direction.z * radius], index * 3)
    const color = starColor(`background-${index}`).multiplyScalar(.74 + rng() * .4)
    colors.set([color.r, color.g, color.b], index * 3)
    const bright = rng() > .91
    sizes[index] = bright ? 3.3 + rng() * 2.8 : 1.25 + rng() * 2.3
    opacities[index] = bright ? .76 + rng() * .22 : .38 + rng() * .34
    phases[index] = rng() * Math.PI * 2
    speeds[index] = .12 + rng() * .42
  }
  backgroundGeometry = new THREE.BufferGeometry()
  backgroundGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  backgroundGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  backgroundGeometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  backgroundGeometry.setAttribute('aOpacity', new THREE.BufferAttribute(opacities, 1))
  backgroundGeometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  backgroundGeometry.setAttribute('aSpeed', new THREE.BufferAttribute(speeds, 1))
  const material = new THREE.ShaderMaterial({
    uniforms: {uTime: {value: 0}, uPixelRatio: {value: Math.min(window.devicePixelRatio, 2)}},
    vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    vertexShader: `
      attribute float aSize; attribute float aOpacity; attribute float aPhase; attribute float aSpeed;
      varying vec3 vColor; varying float vOpacity;
      uniform float uTime; uniform float uPixelRatio;
      void main(){
        vColor=color;
        vOpacity=aOpacity*(.88+sin(uTime*aSpeed+aPhase)*.12);
        vec4 mvPosition=modelViewMatrix*vec4(position,1.0);
        gl_PointSize=max((.85+aSize*.38)*uPixelRatio,aSize*uPixelRatio*230.0/max(70.0,-mvPosition.z));
        gl_Position=projectionMatrix*mvPosition;
      }`,
    fragmentShader: `
      varying vec3 vColor; varying float vOpacity;
      void main(){
        float d=distance(gl_PointCoord,vec2(.5)); if(d>.5) discard;
        float halo=smoothstep(.5,.08,d); float core=smoothstep(.14,0.0,d);
        float alpha=(halo*.52+core*.96)*vOpacity;
        gl_FragColor=vec4(vColor*(.86+core*.5),alpha);
      }`
  })
  backgroundPoints = new THREE.Points(backgroundGeometry, material)
  backgroundPoints.frustumCulled = false
  scene.add(backgroundPoints)
}

function createClusterDust() {
  if (!dataGroup || !overview.value.length) return
  const perYear = Math.max(150, Math.min(380, Math.floor(4200 / overview.value.length)))
  const count = perYear * overview.value.length
  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const phases = new Float32Array(count)
  let index = 0
  for (const summary of overview.value) {
    const center = yearCenters.get(summary.year)
    if (!center) continue
    const rng = seeded(hash(`galaxy-dust-${summary.year}`))
    const rotation = rng() * Math.PI * 2
    const diskRatio = .52 + rng() * .17
    const baseColor = starColor(`year-${summary.year}`)
    for (let point = 0; point < perYear; point++, index++) {
      const radius = Math.pow(rng(), .58) * 35
      const arm = point % 3
      const angle = rotation + arm * Math.PI * 2 / 3 + radius * .2 + (rng() - .5) * (1.25 - radius / 45)
      const softness = 1.3 + rng() * 2.5
      positions.set([
        center.x + Math.cos(angle) * radius + (rng() - .5) * softness,
        center.y + Math.sin(angle) * radius * diskRatio + (rng() - .5) * softness,
        center.z + (rng() - .5) * (7 - radius * .12)
      ], index * 3)
      const color = baseColor.clone().lerp(starColor(`dust-${summary.year}-${point}`), .36).multiplyScalar(.42 + rng() * .35)
      colors.set([color.r, color.g, color.b], index * 3)
      sizes[index] = .85 + rng() * (rng() > .93 ? 3.5 : 1.9)
      phases[index] = rng() * Math.PI * 2
    }
  }
  clusterDustGeometry = new THREE.BufferGeometry()
  clusterDustGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  clusterDustGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  clusterDustGeometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  clusterDustGeometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  const material = new THREE.ShaderMaterial({
    uniforms: {uTime: {value: 0}, uPixelRatio: {value: Math.min(window.devicePixelRatio, 2)}},
    vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    vertexShader: `
      attribute float aSize; attribute float aPhase; varying vec3 vColor; varying float vAlpha;
      uniform float uTime; uniform float uPixelRatio;
      void main(){
        vColor=color; vAlpha=.38+sin(uTime*.22+aPhase)*.09;
        vec4 mvPosition=modelViewMatrix*vec4(position,1.0);
        gl_PointSize=max(1.0,aSize*uPixelRatio*145.0/max(65.0,-mvPosition.z));
        gl_Position=projectionMatrix*mvPosition;
      }`,
    fragmentShader: `
      varying vec3 vColor; varying float vAlpha;
      void main(){
        float d=distance(gl_PointCoord,vec2(.5)); if(d>.5) discard;
        float alpha=smoothstep(.5,.05,d)*vAlpha;
        gl_FragColor=vec4(vColor,alpha);
      }`
  })
  clusterDust = new THREE.Points(clusterDustGeometry, material)
  clusterDust.renderOrder = -2
  dataGroup.add(clusterDust)
}

function createDataStars() {
  if (!scene) return
  buildNodes()
  updateCameraFit(true)
  const count = nodes.length
  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const opacities = new Float32Array(count)
  const phases = new Float32Array(count)
  const speeds = new Float32Array(count)
  const emphasis = new Float32Array(count)
  nodes.forEach((node, index) => {
    positions.set(node.position.toArray(), index * 3)
    colors.set([node.color.r, node.color.g, node.color.b], index * 3)
    sizes[index] = node.size
    opacities[index] = baselineOpacity(node)
    const rng = seeded(hash(`animation-${node.key}`))
    phases[index] = rng() * Math.PI * 2
    speeds[index] = .32 + rng() * .8
    emphasis[index] = dataEmphasis(node)
  })
  dataGeometry = new THREE.BufferGeometry()
  dataGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  dataGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  dataGeometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  dataGeometry.setAttribute('aOpacity', new THREE.BufferAttribute(opacities, 1))
  dataGeometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  dataGeometry.setAttribute('aSpeed', new THREE.BufferAttribute(speeds, 1))
  dataGeometry.setAttribute('aEmphasis', new THREE.BufferAttribute(emphasis, 1))
  const material = new THREE.ShaderMaterial({
    uniforms: {uTime: {value: 0}, uPixelRatio: {value: Math.min(window.devicePixelRatio, 2)}},
    vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    vertexShader: `
      attribute float aSize; attribute float aOpacity; attribute float aPhase; attribute float aSpeed; attribute float aEmphasis;
      varying vec3 vColor; varying float vOpacity;
      uniform float uTime; uniform float uPixelRatio;
      void main(){
        vColor=color; float twinkle=1.0+sin(uTime*aSpeed+aPhase)*0.075;
        vOpacity=aOpacity; vec4 mvPosition=modelViewMatrix*vec4(position,1.0);
        float perspective=70.0/max(28.0,-mvPosition.z);
        gl_PointSize=max(2.0,aSize*(1.0+aEmphasis*.48)*twinkle*uPixelRatio*perspective);
        gl_Position=projectionMatrix*mvPosition;
      }`,
    fragmentShader: `
      varying vec3 vColor; varying float vOpacity;
      void main(){
        float d=distance(gl_PointCoord,vec2(.5)); if(d>.5) discard;
        float halo=smoothstep(.5,.08,d); float core=smoothstep(.16,0.0,d);
        float alpha=(halo*.42+core*.92)*vOpacity;
        gl_FragColor=vec4(vColor*(1.0+core*.42),alpha);
      }`
  })
  dataPoints = new THREE.Points(dataGeometry, material)
  dataGroup = new THREE.Group()
  createClusterDust()
  dataGroup.add(dataPoints)
  connectionGeometry = new THREE.BufferGeometry()
  connectionGeometry.setAttribute('position', new THREE.BufferAttribute(new Float32Array(0), 3))
  connectionGeometry.setAttribute('color', new THREE.BufferAttribute(new Float32Array(0), 3))
  connectionGeometry.setAttribute('aProgress', new THREE.BufferAttribute(new Float32Array(0), 1))
  connectionGeometry.setAttribute('aPhase', new THREE.BufferAttribute(new Float32Array(0), 1))
  connectionLines = new THREE.LineSegments(connectionGeometry, new THREE.ShaderMaterial({
    uniforms: {uTime: {value: 0}}, vertexColors: true, transparent: true, depthWrite: false,
    blending: THREE.AdditiveBlending,
    vertexShader: `
      attribute float aProgress; attribute float aPhase;
      varying vec3 vColor; varying float vProgress; varying float vPhase;
      void main(){
        vColor=color; vProgress=aProgress; vPhase=aPhase;
        gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0);
      }`,
    fragmentShader: `
      uniform float uTime; varying vec3 vColor; varying float vProgress; varying float vPhase;
      void main(){
        float travel=fract(uTime*.075+vPhase);
        float gap=abs(vProgress-travel); gap=min(gap,1.0-gap);
        float pulse=smoothstep(.2,0.0,gap);
        gl_FragColor=vec4(vColor*(.8+pulse*.7),.12+pulse*.48);
      }`
  }))
  connectionLines.renderOrder = -1
  dataGroup.add(connectionLines)
  scene.add(dataGroup)
  updateVisualState()
}

function baselineOpacity(node: StarNode) {
  if (node.type === 'year') return .98
  if (node.type === 'month') return node.count > 0 ? .82 : .24
  return node.type === 'day' ? .76 : .66
}
function dataEmphasis(node: StarNode) {
  if (!node.count || node.type === 'year') return 0
  return node.type === 'day' ? .2 : .13
}
function ancestorsOf(node: StarNode) {
  const result: StarNode[] = []
  let current = node.parentKey ? nodeByKey.get(node.parentKey) : undefined
  while (current) {
    result.push(current)
    current = current.parentKey ? nodeByKey.get(current.parentKey) : undefined
  }
  return result
}
function relatesTo(active: StarNode, node: StarNode) {
  if (node.key === active.key || ancestorsOf(active).some(item => item.key === node.key)) return true
  return ancestorsOf(node).some(item => item.key === active.key)
}
function updateConnectionLines() {
  if (!connectionGeometry) return
  const active = effectiveNode.value
  const edges = new Map<string, [StarNode, StarNode]>()
  const addEdge = (child: StarNode) => {
    const parent = child.parentKey ? nodeByKey.get(child.parentKey) : undefined
    if (parent) edges.set(`${parent.key}:${child.key}`, [parent, child])
  }
  const childrenByParent = new Map<string, StarNode[]>()
  for (const node of nodes) {
    if (!node.parentKey) continue
    const children = childrenByParent.get(node.parentKey) || []
    children.push(node)
    childrenByParent.set(node.parentKey, children)
  }
  const addDescendantEdges = (parentKey: string) => {
    for (const child of childrenByParent.get(parentKey) || []) {
      addEdge(child)
      addDescendantEdges(child.key)
    }
  }
  if (active) {
    addEdge(active)
    ancestorsOf(active).forEach(addEdge)
    addDescendantEdges(active.key)
  }
  const segments = 20
  const positions = new Float32Array(edges.size * segments * 6)
  const colors = new Float32Array(edges.size * segments * 6)
  const progresses = new Float32Array(edges.size * segments * 2)
  const phases = new Float32Array(edges.size * segments * 2)
  let vertexOffset = 0
  for (const [parent, child] of edges.values()) {
    const rng = seeded(hash(`connection-${parent.key}-${child.key}`))
    const direction = child.position.clone().sub(parent.position)
    const swirlRng = seeded(hash(`swirl-${parent.year}`))
    const swirlSign = swirlRng() > .5 ? 1 : -1
    const bend = new THREE.Vector3(-direction.y, direction.x, 0)
    if (bend.lengthSq() > 0) {
      const ratio = parent.type === 'year' ? .24 + rng() * .1 : .1 + rng() * .1
      bend.normalize().multiplyScalar(Math.min(parent.type === 'year' ? 8 : 4.5, direction.length() * ratio) * swirlSign)
      bend.z = (rng() - .5) * direction.length() * .12
    }
    const control = parent.position.clone().lerp(child.position, .5).add(bend)
    const curve = new THREE.QuadraticBezierCurve3(parent.position, control, child.position)
    const phase = rng()
    for (let segment = 0; segment < segments; segment++) {
      const startT = segment / segments
      const endT = (segment + 1) / segments
      const start = curve.getPoint(startT)
      const end = curve.getPoint(endT)
      const startColor = parent.color.clone().lerp(child.color, startT)
      const endColor = parent.color.clone().lerp(child.color, endT)
      positions.set(start.toArray(), vertexOffset * 3)
      positions.set(end.toArray(), (vertexOffset + 1) * 3)
      colors.set([startColor.r, startColor.g, startColor.b], vertexOffset * 3)
      colors.set([endColor.r, endColor.g, endColor.b], (vertexOffset + 1) * 3)
      progresses.set([startT, endT], vertexOffset)
      phases.set([phase, phase], vertexOffset)
      vertexOffset += 2
    }
  }
  connectionGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  connectionGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  connectionGeometry.setAttribute('aProgress', new THREE.BufferAttribute(progresses, 1))
  connectionGeometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  connectionGeometry.boundingSphere = edges.size ? null : new THREE.Sphere(new THREE.Vector3(), 0)
  if (edges.size) connectionGeometry.computeBoundingSphere()
}
function updateVisualState() {
  if (!dataGeometry) return
  const opacity = dataGeometry.getAttribute('aOpacity') as THREE.BufferAttribute
  const emphasis = dataGeometry.getAttribute('aEmphasis') as THREE.BufferAttribute
  const active = effectiveNode.value
  nodes.forEach((node, index) => {
    let alpha = baselineOpacity(node)
    let glow = dataEmphasis(node)
    if (active) {
      const related = relatesTo(active, node)
      alpha = related ? Math.max(alpha, node.key === active.key ? 1 : .84) : Math.max(.1, alpha * .28)
      glow = node.key === active.key ? 1 : related ? Math.max(glow, .42) : glow * .28
    }
    opacity.setX(index, alpha)
    emphasis.setX(index, glow)
  })
  opacity.needsUpdate = true
  emphasis.needsUpdate = true
  updateConnectionLines()
}

async function loadMemoryDetail(id: number) {
  if (detailCache.has(id)) return
  detailLoading.value = true
  try { detailCache.set(id, await memoryApi.detail(id)) }
  catch (exception) { error.value = (exception as Error).message }
  finally { detailLoading.value = false }
}
function scheduleMemoryDetail(node: StarNode | null, immediate = false) {
  clearTimeout(detailTimer)
  if (!node?.memoryId) return
  detailTimer = setTimeout(() => loadMemoryDetail(node.memoryId!), immediate ? 0 : 150)
}

function pickNode(event: PointerEvent) {
  if (!renderer || !camera || !dataPoints) return null
  const rect = renderer.domElement.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  pointer.x = (x / rect.width) * 2 - 1
  pointer.y = -(y / rect.height) * 2 + 1
  pointerTarget.set(pointer.x, pointer.y)
  let selected: StarNode | null = null
  let bestScore = Number.POSITIVE_INFINITY
  for (const node of nodes) {
    const projected = projectNode(node)
    if (!projected.visible) continue
    const radius = node.type === 'year' ? 30 : node.type === 'month' ? (node.count ? 23 : 17) : node.type === 'day' ? 20 : 18
    const score = Math.hypot(projected.x - x, projected.y - y) / radius
    if (score <= 1 && score < bestScore) {
      bestScore = score
      selected = node
    }
  }
  return selected
}
function handlePointerDown(event: PointerEvent) {
  pointerDownX = event.clientX
  pointerDownY = event.clientY
  pointerDragDistance = 0
}
function handlePointerMove(event: PointerEvent) {
  if (event.buttons) {
    pointerDragDistance = Math.max(pointerDragDistance, Math.hypot(event.clientX - pointerDownX, event.clientY - pointerDownY))
    if (hovered.value) {
      hovered.value = null
      updateVisualState()
    }
    return
  }
  const node = pickNode(event)
  if (hovered.value?.key === node?.key) return
  hovered.value = node
  scheduleMemoryDetail(node)
  updateVisualState()
  if (renderer) renderer.domElement.style.cursor = node ? 'pointer' : 'grab'
}
function handlePointerLeave() {
  hovered.value = null
  pointerTarget.set(0, 0)
  updateVisualState()
}
function handleClick(event: PointerEvent) {
  if (pointerDragDistance > 5) {
    pointerDragDistance = 0
    return
  }
  const node = pickNode(event)
  if (!node) {
    if (pinned.value?.memoryId) dismissedMemoryId.value = pinned.value.memoryId
    return
  }
  const cancelling = pinned.value?.key === node.key
  pinned.value = cancelling ? null : node
  dismissedMemoryId.value = undefined
  scheduleMemoryDetail(pinned.value, true)
  focusCamera(pinned.value)
  updateVisualState()
}
function focusCamera(node: StarNode | null) {
  if (!camera || !controls) return
  const target = node ? node.position.clone().applyMatrix4(dataGroup?.matrixWorld || new THREE.Matrix4()) : new THREE.Vector3()
  const direction = camera.position.clone().sub(controls.target)
  if (direction.lengthSq() < .001) direction.set(0, 0, 1)
  direction.normalize()
  const distance = !node ? baseCameraZ : node.type === 'year' ? 88 : node.type === 'month' ? 72 : node.type === 'day' ? 58 : 52
  cameraTween = {
    startedAt: performance.now(), duration: 850,
    fromPosition: camera.position.clone(), toPosition: target.clone().add(direction.multiplyScalar(distance)),
    fromTarget: controls.target.clone(), toTarget: target
  }
}
function reset() { pinned.value = null; hovered.value = null; dismissedMemoryId.value = undefined; focusCamera(null); updateVisualState() }
function handleKey(event: KeyboardEvent) { if (event.key === 'Escape') reset() }

function projectNode(node: StarNode) {
  if (!camera || !dataGroup) return {x: 0, y: 0, visible: false}
  const point = node.position.clone().applyMatrix4(dataGroup.matrixWorld).project(camera)
  return {
    x: (point.x * .5 + .5) * viewportWidth.value,
    y: (-point.y * .5 + .5) * viewportHeight.value,
    visible: point.z > -1 && point.z < 1 && Math.abs(point.x) < 1.08 && Math.abs(point.y) < 1.08
  }
}
function updateOverlays() {
  const positions: Record<string, {x: number; y: number; visible: boolean}> = {}
  for (const node of nodes) {
    if (node.type !== 'memory') positions[node.key] = projectNode(node)
  }
  const active = effectiveNode.value
  if (active && active.type !== 'year') positions[active.key] = projectNode(active)
  labelPositions.value = positions
  if (active?.type === 'memory') {
    const projected = projectNode(active)
    cardX.value = projected.x
    cardY.value = projected.y
  }
}

function labelOpacity(node: StarNode) {
  const active = effectiveNode.value
  if (!active) return node.type === 'year' ? .9 : 0
  if (node.key === active.key) return 1
  if (relatesTo(active, node)) return node.count > 0 ? .78 : .38
  return node.type === 'year' ? .34 : node.count > 0 ? .2 : .1
}
function showLabel(node: StarNode) {
  if (node.type === 'year') return true
  const active = effectiveNode.value
  if (!active) return false
  if (node.type === 'month') {
    return (active.type === 'year' && active.year === node.year) ||
      (active.year === node.year && active.month === node.month)
  }
  if (node.type === 'day') {
    return active.year === node.year && active.month === node.month &&
      (active.type === 'month' || active.day === node.day)
  }
  return false
}

function updateCameraFit(applyImmediately = false) {
  if (!camera) return
  if (overview.value.length <= 1) {
    baseCameraZ = 78
  } else {
    const rect = host.value?.getBoundingClientRect()
    const aspect = rect ? rect.width / Math.max(rect.height, 1) : 1
    let extent = 0
    for (const center of yearCenters.values()) {
      extent = Math.max(extent, Math.abs(center.x) / Math.max(aspect, 1), Math.abs(center.y))
    }
    baseCameraZ = Math.max(118, (extent + 40) / Math.tan(THREE.MathUtils.degToRad(camera.fov * .5)) * 1.08)
  }
  if (controls) controls.maxDistance = Math.max(260, baseCameraZ * 2.2)
  if (applyImmediately && !pinned.value) camera.position.z = baseCameraZ
}

function setupThree() {
  if (!mountPoint.value || !host.value) return
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(48, 1, .1, 1600)
  camera.position.set(0, 0, 78)
  renderer = new THREE.WebGLRenderer({antialias: true, alpha: true, powerPreference: 'high-performance'})
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.05
  renderer.domElement.className = 'absolute inset-0 h-full w-full touch-none'
  mountPoint.value.appendChild(renderer.domElement)
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = .055
  controls.enablePan = true
  controls.screenSpacePanning = true
  controls.panSpeed = .72
  controls.rotateSpeed = .42
  controls.zoomSpeed = .75
  controls.minDistance = 18
  controls.maxDistance = 420
  controls.target.set(0, 0, 0)
  renderer.domElement.addEventListener('pointerdown', handlePointerDown)
  renderer.domElement.addEventListener('pointermove', handlePointerMove)
  renderer.domElement.addEventListener('pointerleave', handlePointerLeave)
  renderer.domElement.addEventListener('click', handleClick)
  createBackground()
  createDataStars()
  timer = new THREE.Timer()
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(host.value)
  resize()
  render()
}
function resize() {
  if (!host.value || !renderer || !camera) return
  const {width, height} = host.value.getBoundingClientRect()
  viewportWidth.value = width
  viewportHeight.value = height
  camera.aspect = width / Math.max(height, 1)
  camera.updateProjectionMatrix()
  updateCameraFit(false)
  renderer.setSize(width, height, false)
}
function render() {
  if (!renderer || !scene || !camera || !timer) return
  timer.update()
  const elapsed = timer.getElapsed()
  if (cameraTween && controls) {
    const progress = Math.min(1, (performance.now() - cameraTween.startedAt) / cameraTween.duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    camera.position.lerpVectors(cameraTween.fromPosition, cameraTween.toPosition, eased)
    controls.target.lerpVectors(cameraTween.fromTarget, cameraTween.toTarget, eased)
    if (progress >= 1) cameraTween = undefined
  }
  controls?.update()
  if (backgroundPoints) {
    backgroundPoints.rotation.y = elapsed * .0035
    backgroundPoints.rotation.x = Math.sin(elapsed * .05) * .012
    backgroundPoints.position.z = Math.sin(elapsed * .055) * 1.8
    backgroundPoints.material.uniforms.uTime.value = elapsed
  }
  if (dataPoints) dataPoints.material.uniforms.uTime.value = elapsed
  if (clusterDust) clusterDust.material.uniforms.uTime.value = elapsed
  if (connectionLines) connectionLines.material.uniforms.uTime.value = elapsed
  renderer.render(scene, camera)
  if (++frameCount % 3 === 0) updateOverlays()
  animationFrame = requestAnimationFrame(render)
}

function mediaUrl(memory: Memory) { return memory.coverUrl || memory.mediaUrls?.[0] || '' }
function isVideo(url: string) { return /\.(mp4|webm|ogg|mov|m4v)(?:$|[?#])/i.test(url) }

watch(effectiveNode, () => updateVisualState())

onMounted(async () => {
  try {
    [overview.value, memoryIndex.value] = await Promise.all([
      memoryApi.timeline(props.familyId), memoryApi.timelineStars(props.familyId)
    ])
    setupThree()
  } catch (exception) {
    error.value = (exception as Error).message
  } finally {
    loading.value = false
  }
  document.addEventListener('keydown', handleKey)
})

onBeforeUnmount(() => {
  clearTimeout(detailTimer)
  cancelAnimationFrame(animationFrame)
  resizeObserver?.disconnect()
  document.removeEventListener('keydown', handleKey)
  if (renderer) {
    renderer.domElement.removeEventListener('pointerdown', handlePointerDown)
    renderer.domElement.removeEventListener('pointermove', handlePointerMove)
    renderer.domElement.removeEventListener('pointerleave', handlePointerLeave)
    renderer.domElement.removeEventListener('click', handleClick)
    renderer.dispose()
    renderer.domElement.remove()
  }
  controls?.dispose()
  dataGeometry?.dispose()
  dataPoints?.material.dispose()
  connectionGeometry?.dispose()
  connectionLines?.material.dispose()
  clusterDustGeometry?.dispose()
  clusterDust?.material.dispose()
  if (backgroundPoints) {
    backgroundGeometry?.dispose()
    backgroundPoints.material.dispose()
  }
})
</script>

<template>
  <div ref="host" class="cosmos relative min-h-[calc(100vh-72px)] w-full overflow-hidden">
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_34%_28%,#1b2140_0%,#0c1127_42%,#060916_100%)]"></div>
    <div class="pointer-events-none absolute inset-0 opacity-45 bg-[radial-gradient(ellipse_at_70%_70%,rgba(91,60,105,.24),transparent_48%),radial-gradient(ellipse_at_25%_55%,rgba(65,83,120,.18),transparent_42%)]"></div>
    <div class="cosmic-veil"></div>
    <div class="nebula nebula-one"></div>
    <div class="nebula nebula-two"></div>
    <div ref="mountPoint" class="absolute inset-0"></div>

    <div class="pointer-events-none absolute left-5 right-5 top-24 z-20 flex items-start justify-between gap-3 text-white">
      <div class="rounded-2xl border border-white/10 bg-black/25 px-4 py-3 backdrop-blur-md">
        <div class="flex flex-wrap items-center gap-2 text-xs font-semibold text-white/90"><span>记忆星图</span><template v-for="part in breadcrumb" :key="part"><ChevronRight :size="12" class="text-white/30"/><span class="text-brand-peach">{{ part }}</span></template></div>
        <p class="mt-1 flex items-center gap-1.5 text-[10px] text-white/45"><MousePointer2 :size="11"/>左键旋转 · 右键或 Shift+左键平移 · 滚轮缩放</p>
      </div>
      <button v-if="pinned" @click="reset" class="pointer-events-auto flex items-center gap-1.5 rounded-xl border border-white/10 bg-black/25 px-3 py-2 text-[10px] font-semibold text-white/65 backdrop-blur-md transition hover:bg-white/10 hover:text-white"><RotateCcw :size="12"/>取消选择</button>
    </div>

    <div v-if="loading" class="absolute inset-0 z-40 grid place-items-center text-white/60"><span class="flex items-center gap-2 text-xs"><LoaderCircle :size="16" class="animate-spin"/>正在构建3D记忆星海…</span></div>
    <div v-else-if="error" class="absolute inset-0 z-40 grid place-items-center px-6 text-center text-sm text-red-200">{{ error }}</div>
    <div v-else-if="!overview.length" class="absolute inset-0 z-40 grid place-items-center text-sm text-white/50">还没有可以点亮的家庭记忆</div>

    <template v-for="node in nodes.filter(item => item.type === 'year')" :key="node.key">
      <div v-if="labelPositions[node.key]?.visible" class="pointer-events-none absolute z-20 -translate-x-1/2 translate-y-3 text-center text-white drop-shadow-lg transition-opacity duration-300" :style="{left: `${labelPositions[node.key].x}px`, top: `${labelPositions[node.key].y}px`, opacity: labelOpacity(node)}">
        <strong class="font-serif text-sm">{{ node.year }}</strong><small class="block text-[8px] text-white/45">{{ node.count }}条</small>
      </div>
    </template>
    <template v-for="node in nodes.filter(item => item.type === 'month')" :key="node.key">
      <div v-if="showLabel(node) && labelPositions[node.key]?.visible" class="pointer-events-none absolute z-20 -translate-x-1/2 translate-y-2 whitespace-nowrap text-center text-white transition-opacity duration-300" :style="{left: `${labelPositions[node.key].x}px`, top: `${labelPositions[node.key].y}px`, opacity: labelOpacity(node)}">
        <strong class="text-[10px] font-medium">{{ node.month }}月</strong><small v-if="node.count" class="ml-1 text-[8px] text-white/45">{{ node.count }}</small>
      </div>
    </template>
    <template v-for="node in nodes.filter(item => item.type === 'day')" :key="node.key">
      <div v-if="showLabel(node) && labelPositions[node.key]?.visible" class="pointer-events-none absolute z-20 -translate-x-1/2 translate-y-2 whitespace-nowrap text-center text-white transition-opacity duration-300" :style="{left: `${labelPositions[node.key].x}px`, top: `${labelPositions[node.key].y}px`, opacity: labelOpacity(node)}">
        <strong class="text-[9px] font-medium">{{ node.day }}日</strong><small class="ml-1 text-[8px] text-white/40">{{ node.count }}</small>
      </div>
    </template>

    <div v-if="detailLoading && activeMemoryId && activeMemoryId !== dismissedMemoryId && !cardMemory" class="pointer-events-none absolute bottom-5 left-1/2 z-40 -translate-x-1/2 rounded-full border border-white/10 bg-black/40 px-3 py-1.5 text-[10px] text-white/60 backdrop-blur"><LoaderCircle :size="11" class="mr-1 inline animate-spin"/>正在唤醒这段记忆</div>

    <Transition name="card-float">
      <article v-if="cardMemory" class="absolute z-40 w-[270px] overflow-hidden rounded-2xl border border-white/20 bg-[#111523] shadow-[0_24px_80px_rgba(0,0,0,.6)]" :style="cardStyle">
        <div class="relative aspect-[4/3] overflow-hidden bg-warm-900">
          <video v-if="mediaUrl(cardMemory) && isVideo(mediaUrl(cardMemory))" :src="mediaUrl(cardMemory)" muted playsinline preload="metadata" class="h-full w-full object-cover"></video>
          <img v-else-if="mediaUrl(cardMemory)" :src="mediaUrl(cardMemory)" :alt="cardMemory.title" class="h-full w-full object-cover">
          <div v-else class="grid h-full place-items-center bg-gradient-to-br from-[#27243b] to-[#0e1424] px-6 text-center font-serif text-sm text-white/60">一段被星光收藏的文字记忆</div>
          <div class="absolute inset-0 bg-gradient-to-t from-black/90 via-black/25 to-transparent"></div>
          <div class="absolute left-3 top-3 rounded-xl bg-white/90 px-2.5 py-1.5 text-center text-warm-900 shadow"><strong class="block text-sm leading-none">{{ cardMemory.memoryDate.slice(8) }}</strong><span class="mt-0.5 block text-[9px] font-semibold text-warm-500">{{ cardMemory.memoryDate.slice(5, 7) }}月</span></div>
          <div class="absolute bottom-3 left-3 right-3 text-white"><span v-if="cardMemory.tags?.length" class="mb-1 inline-block rounded-full bg-brand-coral/90 px-2 py-0.5 text-[9px] font-semibold">{{ cardMemory.tags[0] }}</span><h3 class="line-clamp-2 text-sm font-bold">{{ cardMemory.title }}</h3><div class="mt-2 space-y-1 border-t border-white/20 pt-2 text-[10px] text-white/75"><div class="flex items-center gap-1"><MapPin :size="11" class="text-brand-peach"/><span class="truncate">{{ cardMemory.location || '未记录地点' }}</span></div><div class="flex items-center gap-1"><UserRound :size="11" class="text-brand-peach"/><span class="truncate">{{ cardMemory.authorName }} 发布</span></div></div></div>
        </div>
        <RouterLink :to="`/memories/${cardMemory.id}`" class="flex items-center justify-center gap-1 bg-white/5 py-2.5 text-[10px] font-semibold text-brand-peach transition hover:bg-white/10">查看这段记忆 <ChevronRight :size="12"/></RouterLink>
      </article>
    </Transition>
  </div>
</template>

<style scoped>
.cosmic-veil{position:absolute;inset:0;pointer-events:none;background:radial-gradient(ellipse at 50% 48%,rgba(75,91,155,.11),transparent 46%),radial-gradient(ellipse at 12% 78%,rgba(96,63,117,.08),transparent 38%),radial-gradient(ellipse at 88% 22%,rgba(46,89,130,.08),transparent 36%);mix-blend-mode:screen}
.nebula{position:absolute;inset:-18%;pointer-events:none;will-change:transform,opacity;filter:blur(44px);mix-blend-mode:screen}
.nebula-one{background:radial-gradient(ellipse at 28% 42%,rgba(80,91,164,.13) 0%,rgba(50,58,112,.055) 28%,transparent 62%);animation:nebula-drift-one 68s ease-in-out infinite alternate}
.nebula-two{background:radial-gradient(ellipse at 72% 58%,rgba(145,81,106,.085) 0%,rgba(102,69,115,.035) 31%,transparent 64%);animation:nebula-drift-two 82s ease-in-out infinite alternate}
@keyframes nebula-drift-one{0%{transform:translate3d(-3%,-1%,0) scale(1) rotate(-2deg);opacity:.65}55%{opacity:.95}100%{transform:translate3d(5%,3%,0) scale(1.08) rotate(3deg);opacity:.72}}
@keyframes nebula-drift-two{0%{transform:translate3d(4%,2%,0) scale(1.06);opacity:.58}100%{transform:translate3d(-5%,-4%,0) scale(.98);opacity:.9}}
.card-float-enter-active,.card-float-leave-active{transition:opacity .22s ease,filter .22s ease}.card-float-enter-from,.card-float-leave-to{opacity:0;filter:blur(8px)}
@media(max-width:767px){.cosmos{min-height:calc(100vh - 64px)}}
@media(prefers-reduced-motion:reduce){*{scroll-behavior:auto!important}.nebula{animation:none}}
</style>

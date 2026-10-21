import * as THREE from 'three'
import {randomFromKey, type UniverseNode} from './memoryUniverse'

type Edge = {from: UniverseNode; to: UniverseNode; control: THREE.Vector3; key: string; preview:boolean; context:boolean}

type RibbonCurve={points:THREE.Vector3[];colors:THREE.Color[];width:number;phase:number}
function ribbon(width:number,opacity:number,glow=false){
  return new THREE.Mesh(new THREE.BufferGeometry(),new THREE.ShaderMaterial({
    uniforms:{uResolution:{value:new THREE.Vector2(1,1)},uWidth:{value:width},uOpacity:{value:opacity},uTime:{value:0},uGlow:{value:glow?1:0},uDepthCenter:{value:200}},
    vertexColors:true,transparent:true,depthWrite:!glow,side:THREE.DoubleSide,toneMapped:false,
    vertexShader:`uniform float uDepthCenter;varying float vDepthFade;attribute vec3 aPrevious;attribute vec3 aNext;attribute float aSide;attribute float aWidth;attribute float aProgress;attribute float aPhase;varying float vProgress;varying float vPhase;varying vec2 vScreen;uniform vec2 uResolution;uniform float uWidth;varying vec3 vColor;varying float vSide;
      void main(){vec4 p=projectionMatrix*modelViewMatrix*vec4(position,1.0);vec4 before=projectionMatrix*modelViewMatrix*vec4(aPrevious,1.0);vec4 after=projectionMatrix*modelViewMatrix*vec4(aNext,1.0);vec2 direction=(after.xy/after.w-before.xy/before.w)*uResolution;float lengthSquared=dot(direction,direction);direction=lengthSquared>.00001?direction*inversesqrt(lengthSquared):vec2(1.0,0.0);vec2 normal=vec2(-direction.y,direction.x);float depthScale=clamp(1.0+(uDepthCenter/max(1.0,-(modelViewMatrix*vec4(position,1.0)).z)-1.0)*2.2,.5,1.5);vDepthFade=depthScale;p.xy+=normal*aSide*(uWidth*aWidth*depthScale*.5+.7)*2.0/uResolution*p.w;gl_Position=p;vColor=color;vSide=aSide;vProgress=aProgress;vPhase=aPhase;vScreen=(p.xy/p.w*.5+.5)*uResolution;}`,
    fragmentShader:`varying float vDepthFade;varying vec2 vScreen;uniform float uGlow;uniform float uTime;varying float vProgress;varying float vPhase;uniform float uOpacity;varying vec3 vColor;varying float vSide;void main(){float edge=max(fwidth(vSide),.02);float coverage=1.0-smoothstep(1.0-edge,1.0,abs(vSide));float distance=abs(fract(vProgress-uTime*.085+vPhase+.5)-.5);float flow=exp(-pow(distance/.17,2.0));coverage*=mix(1.0,exp(-vSide*vSide*4.5),uGlow);if(coverage<.04)discard;gl_FragColor=vec4(vColor*(.85+flow*.65),coverage*uOpacity*(.82+flow*.18)*clamp(vDepthFade*.9,.45,1.1));}`
  }))
}
function fillRibbon(mesh:THREE.Mesh<THREE.BufferGeometry,THREE.ShaderMaterial>,curves:RibbonCurve[]){
  const positions:number[]=[],previous:number[]=[],next:number[]=[],sides:number[]=[],progress:number[]=[],phases:number[]=[],widths:number[]=[],colors:number[]=[],indices:number[]=[]
  for(const curve of curves){
    const offset=positions.length/3
    curve.points.forEach((p,index)=>{
      const before=curve.points[index-1]||p.clone().multiplyScalar(2).sub(curve.points[1]!)
      const after=curve.points[index+1]||p.clone().multiplyScalar(2).sub(curve.points[index-1]!)
      for(const side of [-1,1]){positions.push(p.x,p.y,p.z);previous.push(before.x,before.y,before.z);next.push(after.x,after.y,after.z);sides.push(side);widths.push(curve.width);progress.push(index/(curve.points.length-1));phases.push(curve.phase);const c=curve.colors[index]!;colors.push(c.r,c.g,c.b)}
      if(index<curve.points.length-1){const a=offset+index*2;indices.push(a,a+1,a+2,a+1,a+3,a+2)}
    })
  }
  mesh.geometry.dispose();const geometry=new THREE.BufferGeometry()
  geometry.setAttribute('position',new THREE.Float32BufferAttribute(positions,3));geometry.setAttribute('aPrevious',new THREE.Float32BufferAttribute(previous,3));geometry.setAttribute('aNext',new THREE.Float32BufferAttribute(next,3));geometry.setAttribute('aSide',new THREE.Float32BufferAttribute(sides,1));geometry.setAttribute('color',new THREE.Float32BufferAttribute(colors,3));geometry.setIndex(indices)
  geometry.setAttribute('aWidth',new THREE.Float32BufferAttribute(widths,1));geometry.setAttribute('aProgress',new THREE.Float32BufferAttribute(progress,1));geometry.setAttribute('aPhase',new THREE.Float32BufferAttribute(phases,1))
  mesh.geometry=geometry;mesh.visible=positions.length>0
}

// Continuous, antialiased curves; relationships appear only on hover or selection.
export class ConstellationLinks extends THREE.Group {
  private quiet = ribbon(.85,.35)
  private active = ribbon(1,.65)
  private glowQuiet=ribbon(3.4,.09,true)
  private glowActive=ribbon(3.4,.14,true)
  private edges: Edge[] = []
  private map: Map<string,UniverseNode>
  private childrenByParent=new Map<string,UniverseNode[]>()
  private selected: string|null = null
  private hovered: string|null = null
  private filter: Set<number>|null = null
  private matchingKeys:Set<string>|null=null
  private searchVisible=false
  constructor(nodes:UniverseNode[]) {
    super()
    this.map=new Map(nodes.map(node=>[node.key,node]))
    for(const node of nodes)if(node.parent){const children=this.childrenByParent.get(node.parent)||[];children.push(node);this.childrenByParent.set(node.parent,children)}
    this.quiet.frustumCulled=this.active.frustumCulled=false
    this.quiet.renderOrder=1;this.active.renderOrder=2
    for(const glow of [this.glowQuiet,this.glowActive]){glow.frustumCulled=false;glow.renderOrder=0;glow.geometry.dispose()}
    this.add(this.glowQuiet,this.glowActive,this.quiet,this.active)
  }
  setNodes(nodes:UniverseNode[]){this.map=new Map(nodes.map(node=>[node.key,node]));this.childrenByParent.clear();for(const node of nodes)if(node.parent){const children=this.childrenByParent.get(node.parent)||[];children.push(node);this.childrenByParent.set(node.parent,children)}this.build()}
  setLevel(_candidates:UniverseNode[],_parent?:UniverseNode) {this.selected=null;this.hovered=null;this.build()}
  setMatches(ids:Set<number>|null,keys:Set<string>|null=null) {this.filter=ids;this.matchingKeys=keys;this.build()}
  showSearchResults(visible:boolean){this.searchVisible=visible;this.build()}
  private searchNodes(){return this.searchVisible&&this.filter?[...this.map.values()].filter(node=>(node.kind==='memory'||node.kind==='capsule')&&this.matches(node)):[]}
  select(key:string|null) {this.selected=key;this.build()}
  hover(key:string|null) {if(this.hovered===key)return;this.hovered=key;this.build()}
  resize(width:number,height:number,_dpr:number) {for(const mesh of [this.quiet,this.active,this.glowQuiet,this.glowActive])mesh.material.uniforms.uResolution.value.set(width,height)}
  setDepth(distance:number){for(const mesh of [this.quiet,this.active,this.glowQuiet,this.glowActive])mesh.material.uniforms.uDepthCenter.value=distance}
  private matches(node:UniverseNode){return this.filter===null||Boolean(this.matchingKeys?.has(node.key))||node.ids.some(id=>this.filter!.has(id))}
  update(time:number){for(const mesh of [this.quiet,this.active,this.glowQuiet,this.glowActive])mesh.material.uniforms.uTime.value=time}
  subtree(key:string|null):UniverseNode[]{
    const root=key?this.map.get(key):undefined
    if(!root || !root.count || !this.matches(root))return []
    const found:UniverseNode[]=[],pending=[root],seen=new Set<string>()
    while(pending.length){const node=pending.pop()!;if(seen.has(node.key))continue;seen.add(node.key);found.push(node)
      for(const child of this.childrenByParent.get(node.key)||[])if(child.count && this.matches(child))pending.push(child)
    }
    return found
  }
  ancestors(key:string|null):UniverseNode[]{
    const found:UniverseNode[]=[],seen=new Set<string>()
    let node=key?this.map.get(key):undefined
    while(node?.parent && !seen.has(node.parent)){
      seen.add(node.parent);node=this.map.get(node.parent)
      if(node)found.push(node)
    }
    return found
  }
  revealedNodes(){
    const selected=[...new Map([...this.subtree(this.selected),...this.searchNodes()].map(node=>[node.key,node])).values()],hovered=this.subtree(this.hovered)
    const ancestors=[...new Map([...(selected.length?this.ancestors(this.selected):[]),...this.searchNodes().flatMap(node=>this.ancestors(node.key)),...(hovered.length?this.ancestors(this.hovered):[])].map(node=>[node.key,node])).values()]
    return {selected,hovered,ancestors}
  }
  private build(){
    const pairs=new Map<string,{from:UniverseNode;to:UniverseNode;preview:boolean;context:boolean}>()
    const ancestry=(key:string|null,preview:boolean)=>{
      let child=key?this.map.get(key):undefined
      if(child && this.matches(child))for(const parent of this.ancestors(key)){
        pairs.set(`${parent.key}>${child.key}`,{from:parent,to:child,preview,context:true});child=parent
      }
    }
    ancestry(this.selected,false)
    const tree=(key:string|null,preview:boolean)=>{
      const nodes=this.subtree(key),keys=new Set(nodes.map(node=>node.key))
      for(const node of nodes){
        if(!node.parent || !keys.has(node.parent))continue
        const parent=this.map.get(node.parent)!
        if(new THREE.Vector3(...parent.position).distanceToSquared(new THREE.Vector3(...node.position))<.01)continue
        pairs.set(`${parent.key}>${node.key}`,{from:parent,to:node,preview,context:false})
      }
    }
    tree(this.selected,false);ancestry(this.hovered,true);tree(this.hovered,true)
    for(const node of this.searchNodes()){
      ancestry(node.key,false)
      let child:UniverseNode|undefined=node
      for(const parent of this.ancestors(node.key)){pairs.set(`${parent.key}>${child.key}`,{from:parent,to:child,preview:false,context:false});child=parent}
    }
    this.edges=[...pairs.entries()].map(([key,pair])=>{
      const start=new THREE.Vector3(...pair.from.position),end=new THREE.Vector3(...pair.to.position)
      const direction=end.clone().sub(start),distance=direction.length()
      const bend=new THREE.Vector3(-direction.y,direction.x,0).normalize().multiplyScalar(distance*(pair.from.kind==='year'?.27:pair.from.kind==='month'?.3:.34))
      bend.z=distance*.11
      return {...pair,key,control:start.lerp(end,.5).add(bend)}
    })
    this.paint()
  }
  private paint(){
    const quiet:RibbonCurve[]=[],bright:RibbonCurve[]=[]
    for(const edge of this.edges){
      const primary=this.hovered?edge.preview:!edge.context
      const target=primary?bright:quiet
      const a=new THREE.Vector3(...edge.from.position),b=new THREE.Vector3(...edge.to.position)
      const curve=new THREE.QuadraticBezierCurve3(a,edge.control,b)
      const c1=new THREE.Color(edge.from.color),c2=new THREE.Color(edge.to.color)
      const segments=Math.max(96,Math.min(192,Math.ceil(a.distanceTo(b)*1.5)))
      const path:RibbonCurve={points:[],colors:[],width:edge.from.kind==='year'?1.8:edge.from.kind==='month'?1.15:.7,phase:randomFromKey(`flow:${edge.key}`)()}
      for(let i=0;i<=segments;i++){
        const t=i/segments
        path.points.push(curve.getPoint(t))
        path.colors.push(c1.clone().lerp(c2,t).multiplyScalar((primary?.96:.68)*(edge.from.kind==='year'?1:edge.from.kind==='month'?.86:.74)))
      }
      target.push(path)

    }
    fillRibbon(this.quiet,quiet);fillRibbon(this.active,bright)
    this.glowQuiet.geometry=this.quiet.geometry;this.glowQuiet.visible=this.quiet.visible
    this.glowActive.geometry=this.active.geometry;this.glowActive.visible=this.active.visible
  }
  dispose(){for(const object of [this.quiet,this.active]){object.geometry.dispose();object.material.dispose()}this.glowQuiet.material.dispose();this.glowActive.material.dispose()}
}

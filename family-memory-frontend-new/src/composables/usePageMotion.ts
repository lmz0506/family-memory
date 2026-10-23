import {onBeforeUnmount,onMounted,type Ref} from 'vue'

export function usePageMotion(root:Ref<HTMLElement|undefined>){
  let observer:IntersectionObserver|undefined,changes:MutationObserver|undefined,media:MediaQueryList|undefined
  const seen=new WeakSet<Element>()
  function scan(){
    root.value?.querySelectorAll<HTMLElement>('[data-reveal]').forEach(el=>{
      if(seen.has(el))return
      seen.add(el)
      if(media?.matches||!observer)el.classList.add('is-seen')
      else{el.classList.add('will-reveal');observer.observe(el)}
    })
  }
  function preference(){if(media?.matches){root.value?.querySelectorAll('.will-reveal').forEach(el=>el.classList.add('is-seen'));observer?.disconnect()}}
  onMounted(()=>{
    media=window.matchMedia('(prefers-reduced-motion: reduce)')
    if(typeof IntersectionObserver!=='undefined')observer=new IntersectionObserver(entries=>{for(const e of entries)if(e.isIntersecting){e.target.classList.add('is-seen');observer?.unobserve(e.target)}},{root:document.querySelector('[data-app-scroll]'),rootMargin:'0px 0px 40px',threshold:.04})
    scan();changes=new MutationObserver(scan);if(root.value)changes.observe(root.value,{childList:true,subtree:true});media.addEventListener('change',preference)
  })
  onBeforeUnmount(()=>{observer?.disconnect();changes?.disconnect();media?.removeEventListener('change',preference)})
}

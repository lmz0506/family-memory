import {nextTick, onBeforeUnmount, watch, type Ref} from 'vue'

// Shared keyboard, focus and scroll behavior for modal forms and media viewers.
export function useDialog(open: Ref<boolean>, panel: Ref<HTMLElement | undefined>, close: () => void) {
  let previous: HTMLElement | null = null
  let scrollHost: HTMLElement | null = null
  let previousOverflow = ''
  const selector = 'button:not(:disabled),a[href],input:not(:disabled),textarea:not(:disabled),select:not(:disabled),[tabindex="0"]'
  function keydown(event: KeyboardEvent) {
    if (!open.value) return
    if (event.key === 'Escape') {event.preventDefault(); event.stopPropagation(); close()}
    if (event.key !== 'Tab') return
    const elements = Array.from(panel.value?.querySelectorAll<HTMLElement>(selector) || []).filter(el => el.getClientRects().length)
    const first = elements[0], last = elements[elements.length - 1]
    if (!first) { event.preventDefault(); panel.value?.focus(); return }
    if (!panel.value?.contains(document.activeElement) || (event.shiftKey && document.activeElement === first)) {event.preventDefault(); (event.shiftKey ? last : first)?.focus()}
    else if (!event.shiftKey && document.activeElement === last) {event.preventDefault(); first.focus()}
  }
  function release() {
    document.removeEventListener('keydown', keydown, true)
    if (scrollHost) scrollHost.style.overflow = previousOverflow
    scrollHost = null
    if (previous?.isConnected) previous.focus()
    previous = null
  }
  watch(open, async value => {
    if (!value) {release(); return}
    previous = document.activeElement as HTMLElement
    scrollHost = document.querySelector<HTMLElement>('[data-app-scroll]') || document.body
    previousOverflow = scrollHost.style.overflow
    scrollHost.style.overflow = 'hidden'
    document.addEventListener('keydown', keydown, true)
    await nextTick()
    if (open.value) (panel.value?.querySelector<HTMLElement>('[autofocus]') || panel.value)?.focus()
  }, {immediate:true})
  onBeforeUnmount(release)
}

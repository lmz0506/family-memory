import {shallowRef} from 'vue'
import type {CreationReceipt} from './creationReceipt'

const pending = shallowRef<CreationReceipt|null>(null)
let publishedAt=0
export function useCreationEvents() {
  function publish(receipt:CreationReceipt) {publishedAt=Date.now();pending.value=receipt}
  function take(familyId:number) {
    if(pending.value?.familyId!==familyId)return null
    if(Date.now()-publishedAt>30000){pending.value=null;return null}
    const receipt=pending.value;pending.value=null;return receipt
  }
  return {pending,publish,take}
}

<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import {Image, LoaderCircle, Mic, X} from 'lucide-vue-next'
import {memoryApi, uploadApi} from '@/api'

const props = defineProps<{ modelValue: boolean; familyId: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void; (e: 'created'): void }>()
const files = ref<File[]>([])
const audioFile = ref<File | null>(null)
const saving = ref(false)
const error = ref('')
const form = reactive({
  title: '',
  content: '',
  memoryDate: new Date().toISOString().slice(0, 10),
  location: '',
  tags: ''
})
const previews = computed(() => files.value.map(file => URL.createObjectURL(file)))

function choose(event: Event) {
  files.value = Array.from((event.target as HTMLInputElement).files || []).slice(0, 9)
}

function chooseAudio(event: Event) {
  audioFile.value = (event.target as HTMLInputElement).files?.[0] || null
}

function close() {
  if (!saving.value) emit('update:modelValue', false)
}

async function submit() {
  if (!form.title.trim()) {
    error.value = '给这段记忆起个名字吧';
    return
  }
  saving.value = true
  error.value = ''
  try {
    const mediaUrls: string[] = []
    for (const file of files.value) mediaUrls.push((await uploadApi.file(file)).url)
    const audioUrl = audioFile.value ? (await uploadApi.file(audioFile.value, 'audio')).url : ''
    await memoryApi.create(props.familyId, {
      title: form.title, content: form.content, type: files.value.length ? 'photo' : audioUrl ? 'audio' : 'note',
      memoryDate: form.memoryDate, location: form.location, audioUrl, mediaUrls,
      tags: form.tags.split(/[,，]/).map(value => value.trim()).filter(Boolean),
    })
    Object.assign(form, {
      title: '',
      content: '',
      memoryDate: new Date().toISOString().slice(0, 10),
      location: '',
      tags: ''
    })
    files.value = []
    audioFile.value = null
    emit('created')
    emit('update:modelValue', false)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Transition name="fade">
    <div v-if="modelValue"
         class="fixed inset-0 z-50 flex items-end justify-center bg-ink/45 backdrop-blur-sm sm:items-center sm:p-5"
         @click.self="close">
      <section
          class="max-h-[92vh] w-full max-w-lg overflow-y-auto rounded-t-[34px] bg-[#fffdf9] p-6 shadow-2xl sm:rounded-[34px] sm:p-7">
        <div class="flex items-center justify-between">
          <div><p class="text-[10px] font-bold tracking-[.2em] text-peach-600">NEW MEMORY</p>
            <h2 class="mt-1 font-display text-2xl font-black">记录这一刻</h2></div>
          <button @click="close" class="grid h-10 w-10 place-items-center rounded-full bg-stone-100">
            <X :size="18"/>
          </button>
        </div>
        <p v-if="error" class="mt-4 rounded-xl bg-red-50 px-3 py-2 text-xs text-red-600">{{ error }}</p>
        <input v-model="form.title" maxlength="120"
               class="mt-5 w-full border-0 bg-transparent font-display text-xl font-bold outline-none placeholder:text-stone-300"
               placeholder="给这段记忆起个名字"/>
        <textarea v-model="form.content"
                  class="mt-3 h-24 w-full resize-none border-0 bg-transparent text-sm leading-7 outline-none placeholder:text-stone-300"
                  placeholder="今天发生了什么值得记住的事？"></textarea>
        <div v-if="previews.length" class="no-scrollbar mt-3 flex gap-2 overflow-x-auto"><img v-for="url in previews"
                                                                                              :key="url" :src="url"
                                                                                              class="h-24 w-24 shrink-0 rounded-2xl object-cover"/>
        </div>
        <div class="mt-5 grid grid-cols-2 gap-3">
          <label
              class="flex cursor-pointer flex-col items-center gap-2 rounded-2xl bg-peach-50 py-4 text-[10px] font-bold text-peach-600">
            <Image :size="20"/>
            照片视频<input type="file" multiple accept="image/*,video/*" class="hidden" @change="choose"/></label>
          <label
              class="flex cursor-pointer flex-col items-center gap-2 rounded-2xl bg-sky-50 py-4 text-[10px] font-bold text-sky-600">
            <Mic :size="20"/>
            {{ audioFile ? '已选声音' : '一段声音' }}<input type="file" accept="audio/*" class="hidden"
                                                            @change="chooseAudio"/></label>
        </div>
        <p v-if="audioFile" class="mt-2 truncate text-[10px] text-sky-600">声音：{{ audioFile.name }}</p>
        <div class="mt-5 grid gap-3 sm:grid-cols-2"><label class="text-[10px] font-bold text-stone-400">发生日期<input
            v-model="form.memoryDate" type="date"
            class="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-xs text-ink outline-none focus:border-peach-300"/></label><label
            class="text-[10px] font-bold text-stone-400">地点<input v-model="form.location"
                                                                    class="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-xs text-ink outline-none focus:border-peach-300"
                                                                    placeholder="例如：东湖绿道"/></label></div>
        <input v-model="form.tags"
               class="mt-3 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-xs outline-none focus:border-peach-300"
               placeholder="标签，用逗号分隔"/>
        <button :disabled="saving" @click="submit"
                class="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl bg-peach-500 py-3.5 text-sm font-bold text-white shadow-float disabled:opacity-60">
          <LoaderCircle v-if="saving" :size="17" class="animate-spin"/>
          {{ saving ? '正在珍藏...' : '珍藏这段记忆' }}
        </button>
      </section>
    </div>
  </Transition>
</template>

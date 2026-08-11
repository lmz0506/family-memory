import {defineStore} from 'pinia';
import {ref} from 'vue';
import {memoryApi} from '@/api';
import type {Memory} from '@/types'

export const useMemoryStore = defineStore('memories', () => {
    const items = ref<Memory[]>([]);
    const today = ref<Memory[]>([]);
    const years = ref<number[]>([]);
    const total = ref(0);
    const loading = ref(false);

    async function load(familyId: number, params: Record<string, unknown> = {}, append = false) {
        loading.value = true;
        try {
            const data = await memoryApi.list(familyId, {page: 1, size: 50, ...params});
            items.value = append ? [...items.value, ...data.list] : data.list;
            total.value = data.total;
            return data
        } finally {
            loading.value = false
        }
    }

    async function loadHighlights(familyId: number) {
        [today.value, years.value] = await Promise.all([memoryApi.today(familyId), memoryApi.years(familyId)])
    }

    return {items, today, years, total, loading, load, loadHighlights}
})

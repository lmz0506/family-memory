import {defineStore} from 'pinia';
import {ref} from 'vue';
import {familyApi} from '@/api';
import type {Family, FamilyMember} from '@/types'

export const useFamilyStore = defineStore('family', () => {
    const families = ref<Family[]>([]);
    const current = ref<Family | null>(null);
    const members = ref<FamilyMember[]>([]);
    const loading = ref(false);

    async function load() {
        loading.value = true;
        try {
            families.value = await familyApi.list();
            const saved = Number(localStorage.getItem('family-id'));
            current.value = families.value.find(f => f.id === saved) || families.value[0] || null;
            if (current.value) {
                localStorage.setItem('family-id', String(current.value.id));
                await refresh()
            }
        } finally {
            loading.value = false
        }
    }

    async function refresh() {
        if (!current.value) return;
        const [detail, list] = await Promise.all([familyApi.detail(current.value.id), familyApi.members(current.value.id)]);
        current.value = detail;
        members.value = list
    }

    async function select(family: Family) {
        current.value = family;
        localStorage.setItem('family-id', String(family.id));
        await refresh()
    }

    return {families, current, members, loading, load, refresh, select}
})

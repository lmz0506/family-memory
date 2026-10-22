import {defineStore} from 'pinia';
import {computed, ref} from 'vue';
import {clearViewStates} from '@/composables/viewState'
import {authApi} from '@/api';
import type {User} from '@/types'

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem('family-memory-token') || '');
    const user = ref<User | null>(null);
    const loading = ref(false);
    const loggedIn = computed(() => !!token.value);

    async function login(username: string, password: string) {
        loading.value = true;
        try {
            const data = await authApi.login({username, password});
            clearViewStates();
            token.value = data.token;
            user.value = data.user;
            localStorage.setItem('family-memory-token', data.token);
            await loadProfile()
        } finally {
            loading.value = false
        }
    }

    async function register(username: string, password: string, nickname: string) {
        const data = await authApi.register({username, password, nickname});
        clearViewStates();
        token.value = data.token;
        user.value = data.user;
        localStorage.setItem('family-memory-token', data.token);
        await loadProfile()
    }

    async function loadProfile() {
        if (!token.value) return;
        user.value = await authApi.me()
    }

    function logout() {
        clearViewStates();
        token.value = '';
        user.value = null;
        localStorage.removeItem('family-memory-token')
    }

    return {token, user, loading, loggedIn, login, register, loadProfile, logout}
})

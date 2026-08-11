import {createRouter, createWebHistory} from 'vue-router';
import {useAuthStore} from '@/stores/auth'

const router = createRouter({
    history: createWebHistory(), routes: [
        {path: '/login', name: 'login', component: () => import('@/views/LoginView.vue')},
        {path: '/welcome', name: 'welcome', component: () => import('@/views/WelcomeView.vue'), meta: {auth: true}},
        {
            path: '/', component: () => import('@/layouts/AppLayout.vue'), meta: {auth: true}, children: [
                {path: '', name: 'home', component: () => import('@/views/HomeView.vue')},
                {path: 'timeline', name: 'timeline', component: () => import('@/views/TimelineView.vue')},
                {path: 'vault', name: 'vault', component: () => import('@/views/VaultView.vue')},
                {path: 'family', name: 'family', component: () => import('@/views/FamilyView.vue')},
                {path: 'memories/:id', name: 'memory-detail', component: () => import('@/views/MemoryDetailView.vue')},
                {path: 'capsules/:id', name: 'capsule-detail', component: () => import('@/views/CapsuleDetailView.vue')}
            ]
        },
        {path: '/:pathMatch(.*)*', redirect: '/'}
    ]
})
router.beforeEach(async (to) => {
    const auth = useAuthStore();
    if (to.meta.auth && !auth.loggedIn) return {name: 'login', query: {redirect: to.fullPath}};
    if (auth.loggedIn && !auth.user) {
        try {
            await auth.loadProfile()
        } catch {
            auth.logout();
            return {name: 'login'}
        }
    }
    if (to.name === 'login' && auth.loggedIn) return {name: 'home'};
    return true
})
export default router

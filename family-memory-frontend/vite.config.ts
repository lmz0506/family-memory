import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {fileURLToPath, URL} from 'node:url'

export default defineConfig({
    base: '/memory',
    plugins: [
        vue()
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    server: {
        allowedHosts: ['dc87-35-212-154-86.ngrok-free.app'],
        port: 3000,
        open: true
    }
})

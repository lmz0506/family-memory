import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {fileURLToPath, URL} from 'node:url'

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    server: {
        host: '0.0.0.0', // ✅ 监听所有网络接口（允许局域网/外部访问）
        port: 5210,      // ✅ 指定端口号，按需修改
        proxy: {
            '/api': {
                target: 'http://192.168.23.150:5200',
                changeOrigin: true
            }
        }
    }
})
/** @type {import('tailwindcss').Config} */
export default {
    content: ['./index.html', './src/**/*.{vue,ts}'],
    theme: {
        extend: {
            colors: {
                night: Object.fromEntries(['page','surface','soft','raised','ink','secondary','muted','line','line-strong','action','action-ink','accent','danger','positive'].map(key=>[key,`rgb(var(--sky-${key}) / <alpha-value>)`])),
                ink: '#3f342f',
                cream: '#fffaf5',
                peach: {50: '#fff4f0', 100: '#ffe4dc', 300: '#ffb2a2', 500: '#d85e46', 600: '#bf4c36'},
                warm: {50: '#FDFAF7', 100: '#F7F1E9', 200: '#EFE3D4', 300: '#E0CBB4', 400: '#967960', 500: '#806249', 600: '#70543e', 700: '#5d493a', 800: '#4A3E35', 900: '#2C241E'},
                brand: {coral: '#E76F51', 'coral-hover': '#D85A3C', 'coral-light': '#FDF0EC', peach: '#F4A261', sand: '#264653'}
            },
            fontFamily: {
                display: ['"Noto Serif SC"', '"Songti SC"', 'serif'],
                serif: ['"Noto Serif SC"', 'Georgia', 'serif'],
                sans: ['-apple-system','BlinkMacSystemFont','"PingFang SC"','"Microsoft YaHei"','sans-serif']
            },
            boxShadow: {
                soft: '0 8px 30px rgba(180,150,130,.08)',
                'soft-sm': '0 2px 10px rgba(180,150,130,.06)',
                'soft-lg': '0 15px 45px rgba(160,120,100,.12)',
                float: '0 14px 35px rgba(217,92,72,.28)',
                glow: '0 0 25px rgba(231,111,81,.25)'
            }
        }
    },
    plugins: []
}

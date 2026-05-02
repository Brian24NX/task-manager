import { createApp } from 'vue'
import App from './App.vue'
import './style.css'

// Fire backend warmup as early as possible to mask Render free-tier cold starts.
const apiBase = import.meta.env.VITE_API_URL || ''
if (apiBase) {
  const link = document.createElement('link')
  link.rel = 'preconnect'
  link.href = apiBase
  link.crossOrigin = 'anonymous'
  document.head.appendChild(link)
}
fetch(`${apiBase}/api/health`, { method: 'GET', cache: 'no-store' }).catch(() => {})

createApp(App).mount('#app')

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  })
}

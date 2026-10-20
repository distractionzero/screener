import { ViteSSG } from 'vite-ssg'
import App from './App.vue'
import { routes } from './routes'
import './style.css'

export const createApp = ViteSSG(App, {
  routes,
  scrollBehavior(to, _from, saved) {
    if (saved) return saved
    if (to.hash) return { el: to.hash }
    return { top: 0 }
  },
})

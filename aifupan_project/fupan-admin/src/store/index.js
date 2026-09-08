import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
export default {
    install(app) {
        app.use(pinia)
    }
}

export * from '@/store/modules/user.js'
export * from '@/store/modules/system.js'
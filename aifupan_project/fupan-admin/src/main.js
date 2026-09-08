import { createApp } from 'vue'
import App from './App.vue'
import router from '@/router'
import pinia from '@/store'
import '@/assets/scss/index.scss'
import 'nprogress/nprogress.css'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import NProgress from 'nprogress'
import '@/permission'
import 'virtual:svg-icons-register' // vite-plugin-svg-icons 插件注册，不引入会导致svg图标无法正常显示
// 全局配置 NProgress
NProgress.configure({
    showSpinner: false
})
const app = createApp(App)
app.use(ElementPlus, {
    locale: zhCn
})
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(router)
app.use(pinia)
app.mount('#app')

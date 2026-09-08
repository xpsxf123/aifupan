/**
 * @file main.js
 * @description 应用入口：创建 Vue 实例、注册 Element Plus/全局指令/全局组件、挂载路由与状态
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'
import './auth/guard' // 引入路由权限控制
import store from './store'
import './styles/index.scss'
import * as directives from './auth/directives'
import empty from './directives/empty'
import 'virtual:svg-icons-register' // vite-plugin-svg-icons svg图标插件注册
import globalComponent from '@/components' // 自定义插件对象：注册整个项目全局插件
zhCn.el.pagination.pageClassifier = '页'
zhCn.el.pagination.totalClassifier = '条'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册全局指令
Object.keys(directives).forEach((key) => {
  app.directive(key, directives[key])
})
app.directive('empty', empty)

app.use(ElementPlus, {
  locale: zhCn
})
app.use(globalComponent)
app.use(store)
app.use(router)

app.mount('#app')

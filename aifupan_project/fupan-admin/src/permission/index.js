import router from '@/router'
import { useUserInfoStore } from '@/store'
import NProgress from 'nprogress'
import { anyRoute } from '@/router/route'
const modules = import.meta.glob('@/views/**/*.vue')
const loadComponent = (url) => {
  const path = `/src/views${url}/index.vue`
  if (modules[path]) {
    return modules[path]
  } else {
    // console.error(`未找到${path}组件`)
  }
}
// 路由跳转前的处理方法
router.beforeEach((to, from) => {
  // 开始进度条
  NProgress.start()
  const userInfoStore = useUserInfoStore()
  // 获取加载路由标识
  let loadFlag = userInfoStore.loadRouterFlag
  // 当前是登录路由，或已添加动态菜单路由，跳过
  if (to.name === 'login' || loadFlag) {
    return true
  }
  // 拿到登录返回的菜单列表
  let menuList = userInfoStore.loginResultData.menuList
  // 合并任意路由
  menuList = [...menuList, ...anyRoute]
  // 将后台返回的菜单添加到路由
  fnAddDynamicMenuRoutes(menuList)
  return { ...to, replace: true }
})

const fnAddDynamicMenuRoutes = (menuList) => {
  const userInfoStore = useUserInfoStore()
  menuList.forEach((ele) => {
    if (ele.type === 0) {
      router.addRoute('main', {
        name: ele.url,
        path: ele.url,
        component: loadComponent(ele.url),
        meta: {
          bgColor:
            ele.rName === 'home' || ele.rName === 'visual' ? 'bg-tp' : '',
        },
      })
    } else if (ele.type === 3) {
      // 注册任意路由
      router.addRoute(ele)
    }
  })

  // 已添加动态菜单路由，修改状态
  userInfoStore.setLoadRouterFlag(true)
}

router.afterEach(() => {
  // 结束进度条
  NProgress.done()
})

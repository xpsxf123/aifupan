import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '@/store';


// 页面路由
const pageRoutes = [
  { path: '/versionSelection', name: 'versionSelection', component: () => import('@/views/pages/versionSelection'), meta: { title: '版本选择' } },
  { path: '/login', name: 'login', component: () => import('@/views/pages/login'), meta: { title: '登录' } },
  { path: '/shipinhao-status', name: 'shipinhaoStatus', component: () => import('@/views/pages/shipinhao/status.vue'), meta: { title: '授权结果', notAPPinit: true } },
  { path: '/', name: 'login', component: () => import('@/views/pages/login'), meta: { title: '登录' } },
  { path: '/404', name: '404', component: () => import('@/views/pages/404'), meta: { title: '404' } },
  {
      path:"/restScreen",
      name:"restScreen",
      component: () => import('@/views/modules/system/restScreen/index.vue'),
      // src\views\modules\system\restScreen\index.vue
      meta: {
          title: "屏保"
      }
  },
  {
    path:'', name:'shareLayout', component:()=>import('@/views/pages/common/shareLayout'),
    children: [
      { 
        path: '/onlineAnalysis/:type/:id', 
        name: 'onlineAnalysis', 
        component: () => import('@/views/pages/onlineAnalysis'), 
        meta: {title: '在线智能复盘分析'}
      },
      { 
        path: '/contrastOnlineAnalysis/:id', 
        name: 'contrastOnlineAnalysis', 
        component: () => import('@/views/pages/contrastOnlineAnalysis'), 
        meta: {title: '在线复盘对比分析'}
      },
      { 
        path: '/onlineAiAnalysis/:type/:fileType/:id', name: 'onlineAiAnalysis', 
        component: () => import('@/views/pages/onlineAnalysis/aiAnalysis.vue'), 
        meta: {title: 'ai复盘分析'}
      },
      {
        path: '/onlineAnalysisMonitor/:type/:id',
        name: 'onlineAnalysisMonitor',
        component: () => import('@/views/pages/onlineAnalysis/monitorDetail.vue'),
        meta: { title: '在线监控详情' }
      },
      { 
        path: '/contrastAiAnalysis/:type/:id', name: 'contrastAiAnalysis', 
        component: () => import('@/views/pages/contrastOnlineAnalysis/aiContrast.vue'), 
        meta: {title: 'ai对比分析'}
      },
    ]
  },
  {
    path: '/official-online',
    name: 'official-online',
    component: () => import('@/views/pages/official-online/index.vue'),
  },
  {
    path:'', name:'aiShareLayout', component:()=>import('@/views/pages/aiShare/aiShareLayout'),
    children: [
      {
        path: '/ai-share/:type/:id/:shareId/:aiType',
        name: 'ai-share',
        component: () => import('@/views/pages/aiShare/index.vue'),
      }
    ]
  },
  {
    "path":"/web-anniversary",
    "name":"web-anniversary",
    component: () => import('@/views/modules/anniversary/index.vue'),
    "meta": {
      "title": "周年庆",
    }
  },
]
Vue.use(VueRouter)

const router = new VueRouter({
  routes: pageRoutes
})


function addRoutes() {
  // 拿到json菜单数据
  let menuList = require('@/assets/json/menu.json');
  // 将菜单添加到路由
  fnAddDynamicMenuRoutes(menuList);

  // 表示已添加过路由
  store.commit("setLoadRouterFlag", true);
}


var getComponent = (filePath, pathType, ele) => {
  let url = '';
  let isPath = pathType === 'path'; //判断是否采用path路径，path路径和filePath路径匹配规则不同
  let filePathUrl = filePath;
  // 判断是否是path路径模式，如果是则需要判定是否走根路径。如果是根路径则会出错。
  if (isPath && filePath.indexOf("/") === 0) {
    // 如果path路径带有根匹配则会出错.path路径默认出发点为modules文件。所以需要去除根的/。
    filePathUrl = filePath.split('/')[1];
  }
  // let filePathUrl = pathType ? filePath.indexOf("/") === 0 filePath
  // 根路径 以views根地址
  if (filePath.indexOf('/') === 0) {
    url = `${filePath}`
  } else {
    // 相对路径 以views/modules为根地址
    url = `/modules/${filePath}`
  }

  // 如果结尾不是.vue文件结尾检查是否默认写入index.vue,如果写入index则补全.vue,如果没写index.则不全index.vue，
  // 不能是文件路径。文件路径则匹配错误只能自动匹配文件路径下index.vue。
  // 如需要自定义非index.vue 路径则写全路径以.vue结尾
  if (url.indexOf('.vue') <= 0) {
    // 执行自动index.vue匹配逻辑
    let urls = url.split('/')
    let popStr = urls.pop();
    if (popStr === 'index') {
      // 最后为index则不全index.vue的后缀vue
      url += '.vue'
    } else {
      url += '/index.vue'
    }
  }
  try {
    require(`@/views${url}`)
    return `${url}`;
  } catch (err) {
    console.log(err,'router err')
    return false
  }
}


let LayoutComponent = {
  // name: "Layout", //消除警告
  path: '',
  component: () => import('@/views/layout/index.vue'),
  children: []
}

// 写入模板文件
function layoutModel(ele, comp, type) {
  if(LayoutComponent.children){
    LayoutComponent.children.push(comp);
  }
}

var fnAddDynamicMenuRoutes = (menuList, type) => {
  let l = menuList.map((ele, index) => {
    let compUrl = getComponent(ele.filePath || ele.path, ele.filePath ? 'filePath' : 'path', ele);
    if (compUrl === false) {
      return
    }
    
    // 路由对象
    let routeObj  = null;
    // 二级路由
    if (ele.children) {
      // 二级路由添加
      routeObj = {
        // name: 'routerView',//消除警告
        path: ele.path,
        component:()=> import('@/views/layout/routerView.vue'), // 二级路由模板
        children:[
          {
            // name: ele.name,//消除警告
            path: '/',
            meta: ele.meta,
            component: ()=>import(`@/views${compUrl}`)
          },
          ...fnAddDynamicMenuRoutes(ele.children, 'children')
        ]
      }
    }else{
      routeObj = {
        // name: ele.name,//消除警告
        path: ele.path,
        meta: ele.meta,
        component:() => import(`@/views${compUrl}`) // 直接加入路由
      }
    }
    if (type === 'children') {
      return routeObj;
    }
    if (!ele.notLayout) {
      routeObj = layoutModel(ele, routeObj, type)
    }else{
      router.addRoute('main', routeObj);
    }
  });


  // console.log(LayoutComponent,'---LayoutComponent')
  router.addRoute('main',LayoutComponent)

  if (!type) {
    // console.log(router.getRoutes())
  }
  return l
  // 已添加动态菜单路由，修改状态
  // store.commit("setLoadRouterFlag", true);
}


const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location, onResolve, onReject) {
  if (onResolve || onReject) return originalPush.call(this, location, onResolve, onReject)
  return originalPush.call(this, location).catch(err => err)
}

// 执行路由动态添加
addRoutes();




export default router

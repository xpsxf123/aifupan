import router from './index';
import store from '@/store';
import env from '/src/env';
import httpBack from '@/utils/request-api-back'
import configEnv from "@/config/env";
import { AI_AGENT_ROUTE_PATH, LIVE_ROOM_ROUTE_PATH, shouldRedirectToAiAgent } from '@/utils/aiAgentRoute'
export let whiteList  = [];
export let webUrlList = [
  'login',
  '404',
  'onlineAnalysis',
  // 云空间公开详情页
  'onlineAnalysisMonitor',
  'contrastOnlineAnalysis',
  'onlineAiAnalysis',
  'contrastAiAnalysis',
  'official-online',
  'ai-share',
  'web-anniversary'
];

const resourcesUrl=[
    '/system',
    '/replay',
    '/addCompere',
    '/dataAnalysis',
    '/replay/analysis',
    '/replay/analysisReadonly/video'
]

const defaultTitle = '爱复盘';

function getRouteTitle(route) {
    const matched = route.matched || [];
    const titleRoute = matched.slice().reverse().find(item => item.meta && item.meta.title);
    return titleRoute?.meta?.title || route.meta?.title || defaultTitle;
}

// 路由跳转前的处理方法
router.beforeEach((to, from, next) => {
    const userId = store.state.userInfo?.id || store.state.userInfo?.userId
    const isLoginDefaultEntry = to.path === LIVE_ROOM_ROUTE_PATH && String(to.query?.login || '') === '1'

    if (isLoginDefaultEntry && shouldRedirectToAiAgent(userId)) {
        return next({
            path: AI_AGENT_ROUTE_PATH,
            query: {
                ...to.query
            }
        })
    }

    if (resourcesUrl.includes(to.path) || (configEnv.isWeb && webUrlList.includes(to.name) && !['login', '404'].includes(to.name))) {
        httpBack.user.infoByClient({}).then(res => {
            if (res.code == 0 && res.data) {
                store.commit("saveUserInfo", res.data);
            }
        });
    }
    if(env.dev){
      return next();
    }
    // 白名单 直接进入
    if(whiteList.includes(to.name)){
      return next();
    }
    // web段页面，并且环境不属于爱复盘客户端内
    if(configEnv.isWeb){
      if(webUrlList.includes(to.name)){
        return next();
      }else{
        return next({
          path: '/404'
        });
      }
    }
    // 没有tokon进入404,用于官网访问,如果是客户端则会判断进入login
    // if(!store.state.token){
    //   return next({path: '/404'});
    // }
    // 获取加载路由标识
    let loadFlag = store.state.loadRouterFlag;
    // 当前是登录路由，或已添加动态菜单路由，跳过
    if (to.name == 'login' || loadFlag) {
      return next();
    }
    next()
    // 拿到json菜单数据
    // let menuList = require('@/assets/json/menu.json');
    // // 将菜单添加到路由
    // fnAddDynamicMenuRoutes(menuList);
    // // 表示已添加过路由
    // store.commit("setLoadRouterFlag", true);
    // next({ ...to, replace: true })
  })

router.afterEach((to) => {
    document.title = getRouteTitle(to);
})
  

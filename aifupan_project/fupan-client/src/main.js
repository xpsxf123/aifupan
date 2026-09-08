import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import VideoJsPlayer from '@/components/video/index.vue';
import ElementUI from 'element-ui'
import httpBack from './utils/request-api-back'
import httpBack2 from './utils/request-back2'
import httpClient from './utils/request-api-client'
import { myMixin } from '@/mixins/mixins.js'
import 'video.js/dist/video-js.css';
import 'shepherd.js/dist/css/shepherd.css';
import windLoad from './utils/windLoad.js'
import Contextmunu from 'vue-contextmenujs'
import './assets/fonts/font/iconfont.js';
import '@/icons'
import '@/assets/scss/bh.scss'
import './utils/dialogDrag.js'
import commonMixin from './mixins/common.js';
import echarts from '@/echarts/index.js';
import notifyFromCSharp from '@/utils/notifyFromcsharp.js'
import JsonExcel from 'vue-json-excel';
import auth from './config/permission'
import './router/permission.js';
import env from "@/config/env";
import { Notification, Message } from '@/components/message';
import newHint from './directive/newHint';
import iframeConfig from "@/utils/iframeConfig/index";
import {initApp} from "@/utils/init";
import GlobalConfirm from '@/components/globalConfirm';
import afpUi from '@/components/eleUi/index.js'

import CONFIG from '@/config/common.js';

Vue.use(afpUi);

Vue.directive('new-hint', newHint);

Vue.component(VideoJsPlayer.name,VideoJsPlayer);

Vue.mixin(commonMixin);

Vue.use(auth);

Vue.directive('removeAriaHidden', {
  bind(el, binding) {
    const ariaEls = el.querySelectorAll('.el-radio__original')
    ariaEls.forEach((item) => {
      item.removeAttribute('aria-hidden')
    })
  }
})

sessionStorage.removeItem('iframe');


localStorage.removeItem('notCloseLoading');
iframeConfig?.init();

Vue.prototype.$isMobile = iframeConfig.isMobile();


// 监听父窗口消息
window.addEventListener('message', (event) => {
  if (event.data.type === 'officical-online' || event.data.type === 'officical-login') {
    // 响应父窗口
    iframeConfig.lookIframe();
    window.parent.postMessage({
      type: event.data.type,
      data: {
        heigth: window.innerHeight
      }
    }, '*')
  }
})

// 对象参数获取兼容
Vue.prototype.$fetchValue = function (obj, ...keys) {
  if(keys.length === 1){
    let key = keys[0];
    let test1 = /[A-Z]/; // 大写正则
    let test2 = /[a-b]/; // 小写正则
    let char0 = key?.charAt(0);  // 首字母截取
    let KEY = ''; //储存大驼峰key
    // 大写改小写
    if(test1.test(char0)){
      KEY = key.replace(char0, char0.toLowerCase())
    }
    // 小写改大写
    if(test2.test(char0)){
      KEY = key.replace(char0,char0.toUpperCase());
    }
    return obj?.[key] || obj?.[KEY];
  }else{
    let k = keys.find(d=>typeof obj[d] !=='undefined');
    return obj?.[k];
  }
}


Vue.component('downloadExcel', JsonExcel);

Vue.prototype.$echarts = echarts;
Vue.use(windLoad);
Vue.use(Contextmunu);
Vue.use(ElementUI);
Vue.use(GlobalConfirm);

// 注册消息组件，使用简化名称防止与Element UI冲突
Vue.prototype.$cNotify = Notification;
Vue.prototype.$cMsg = Message;

Vue.prototype.$isWeb = env.isWeb;
Vue.prototype.$isAifupan = env.isAifupan;

Vue.prototype.$httpBack = httpBack;
Vue.prototype.$httpBack2 = httpBack2;
Vue.prototype.$httmlBack2 = httpBack2;
Vue.prototype.$httpClient = httpClient;

Vue.prototype.$CSharpNotify = notifyFromCSharp;

// 全局参数/样式配置
Vue.prototype.$CONFIG = CONFIG;

Vue.config.productionTip = false;

Vue.mixin(myMixin);

initApp().then(()=>{
  new Vue({
    router,
    store,
    render: h => h(App)
  }).$mount('#app');
})


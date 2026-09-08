import axios from "axios";
import { Message } from "element-ui";
import { Loading } from "element-ui";
import router from "../router";
import httpLoading from "./httpLoading";
import { clearLoginInfo } from "./index";
import myUtils from "./utils";
import store from "@/store";
import env from "@/config/env";
import resolveWebVersion from "./webVersion";
let loadingInstance = [];




const setLoading = myUtils.debounce(200,function(text){
  loadingInstance.push(Loading.service({
    lock: true,
    background: "rgba(255,255,255,0)",
    text: text || "正在停止录制和分析"
  }));
});

const delLoading = myUtils.debounce(300,function(){
  if (loadingInstance?.length && !localStorage.getItem("notCloseLoading")) {
    loadingInstance = loadingInstance?.map(d=>{
      d?.close();
      return false
    })?.filter(d=>d);
  }
})

try {
    const userAgent = window.navigator.userAgent;
    const portMatch = userAgent.match(/port=(\d+)/);
    if (portMatch && portMatch[1]) {
        window.SITE_CONFIG.clientApiURL = `http://127.0.0.1:${portMatch[1]}/api`;
    }
} catch (e) {
    console.error('设置动态端口时出错:', e);
}

const http = axios.create({
  baseURL: window.SITE_CONFIG["clientApiURL"],
  // timeout: 3 * 60 * 1000,
  timeout:  60 * 1000,
});

// 请求拦截
http.interceptors.request.use(
  (config) => {
    const webVersion = resolveWebVersion();
    // 设置保存位置路径的接口，调整请求超时时间
    if (config.url == "/config/checkfilebox" || config.url == "/uploadfile/checkfile" || config.url == "/export/alysestxt" || config.url == "/export/wordsexcel"
      || config.url == "/anchorinfo/stopdecector" || config.url == "/anchorinfo/decector" || config.url == "/form/close"
    ) {
      config.timeout = 5 * 60 * 1000;
    }


    // 设置重新选择行业分析接口的超时时间
    if (config.url == "/anchorvideo/reanalysisbytrade" || config.url == "/uploadfile/reanalysisbytrade") {
      config.timeout = 5 * 60 * 1000;
    }

    if (config.url != "/anchorinfo/getpageanchor" && config.url != "/anchorvideo/getpage" && config.url != "/config/getuserinfo" && config.url != "/config/getVersionUpdate"
      && config.url != "/config/getmodel" && config.url != "/config/getdisksize" && config.url != "/anchorvideo/shareanalysis" && config.url != "/contrast/shareanalysis" ) {
      // 检查加载锁，如果状态为true表示需要加载（兼容处理加入锁状态）为了取消全局请求弹窗操作。
      if(httpLoading.get(config.url) && !sessionStorage.getItem('notLoading')){
        if (config.url == "/anchorinfo/stopdecector") {
          setLoading('正在停止录制和分析')
        } else {
          setLoading(' ')
        }
      }
    }

      if(['/shortVideo/captureHotSearch','/shortVideo/updateHotSearchData'].includes(config.url)){
          setLoading('数据整理中，请勿做其它操作...')
          config.timeout = 5 * 60 * 1000;
      }


    // if(httpLoading.get(config.url) && !sessionStorage.getItem('notLoading')){
    //   loadingInstance = Loading.service({
    //     lock: true,
    //     background: "rgba(38,50,56,.7)",
    //   });
    // }



    // 删除加载锁，如果已经生成了加载，后续逻辑不在由锁管理（兼容处理加入锁状态）
    httpLoading.del(config.url);
    config.headers.Token = store.state.token || "";
    if (webVersion) {
      config.headers.webVersion = webVersion;
    }


    return config;
  },
  (error) => {
    delLoading();
    return Promise.reject(error);
  }
);

// 响应拦截
http.interceptors.response.use(
  (res) => {
    delLoading();
    if(res.config.responseType === 'stream'){
      return res;
    }
    if (res.status == 200 && res.config.url === "/anchorvideo/accelerate" && typeof res.data?.accepted === "boolean") {
      return {
        code: 0,
        data: res.data,
      };
    }
    if (res.status == 200 && res.data.code === 0) {
      delLoading()
      return res.data;
    } else {
      let resErr_text = "";
      // 时间校验
      if(res.data.code === 5601){
        store.commit('setTimeAccurate');
      }
      if (res.status != 200) {
        resErr_text = "与服务器网络连接断开，请稍后重试或联系管理员";
      } else {
        resErr_text = res.data.msg || "请求返回信息错误";
      }
      // 不提示
      if(res.data.code === 7001){
        resErr_text = ''
      }
      if (res.data.code === 4001) {
        clearLoginInfo();
        router.replace({ name: "login" });
      }
      delLoading()
      if(resErr_text){
        Message.error(resErr_text);
      }
      return Promise.reject(res.data);
    }
  },
  (error) => {
    // 主播列表，直接跳过，不弹出错误信息
    let notErrorUrl = [
      "/anchorinfo/getpageanchor",
      "/config/getCreateTime",
      "/config/getClientMode"
    ];
    if (notErrorUrl.indexOf(error.config.url) !== -1) {
      return Promise.reject(error);
    }
    if (!localStorage.getItem("notCloseLoading")) {
      // loadingInstance?.close();
      delLoading();
    }
    let err_txt = "";
    if (error.message.indexOf("timeout of") != -1) {
      err_txt = "网络较差,请再试一次";
    } else if (error.message.indexOf("Network Error") != -1) {
      err_txt = "网络较差,请再试一次";
    }
    Message.error(err_txt);
    return Promise.reject(error);
  }
);
// 判断是否用加载页面
const isLoading = function(url,load){
  let l = load === undefined ? true : load;
  httpLoading.edit(url,l)
}

/**
 * get方法，对应get请求
 * @param {String} url [请求的url地址]
 * @param {Object} params [请求时携带的参数]
 */

export const get = (url, params,option={}) => {
  if(env.isWeb){
    return Promise.reject()
  }
  isLoading(url,option.load)
  return http({
    url,
    params,
    ...option?.config || {}
  });
};

/**
 * post方法，对应post请求
 * @param {String} url [请求的url地址]
 * @param {Object} data [请求时携带的参数]
 */
export const post = (url, data, option={}) => {
  if(env.isWeb){return Promise.reject()}
  isLoading(url,option.load);
  return http({
    url,
    data,
    method: "post",
    ...option?.config || {}
  });
};

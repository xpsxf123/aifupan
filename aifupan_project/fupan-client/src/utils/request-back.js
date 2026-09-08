import axios from "axios";
import { Message } from "element-ui";
import { Loading } from "element-ui";
import apiSignature from './signature';
import router from "../router";
import myUtils from "./utils";
import { clearLoginInfo } from "./index";
import httpLoading from "./httpLoading";
import store from "@/store";
import iframeConfig from "@/utils/iframeConfig/index";
import getPlatform from "./platformSource";
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

/**
 * 创建后端请求实例
 * @param {Object} config axios实例配置
 * @returns {import('axios').AxiosInstance}
 */
const createHttpInstance = (config = {}) => {
  return axios.create({
    baseURL: window.SITE_CONFIG["backApiURL"],
    timeout: 60 * 1000,
    ...config
  });
};

/**
 * 绑定请求拦截器
 * @param {import('axios').AxiosInstance} http axios实例
 * @returns {import('axios').AxiosInstance}
 */
const applyInterceptors = (http) => {
// 请求拦截
http.interceptors.request.use(
  async (config) => {
    if (httpLoading.get(config.url) && !sessionStorage.getItem('notLoading')) {
      if (config.url != "/openapi/userproperty/useProperty" && config.url != "/vod/getVodUploa") {
        setLoading(' ');
      }
    }

    const data = config.method?.toLowerCase() === 'post' ? config.data : config.params

    const contentType = config.headers?.common?.Accept
    const params = await apiSignature.createSignatureHeaders(
        data,
        config.method,
        config.url,
        contentType
    )

    const webVersion = resolveWebVersion();
    config.headers = {
      Token: store.state.token || "",
      ...(store.state.token ? { Authorization: `Bearer ${store.state.token}` } : {}),
      source: getPlatform(),
      ...(webVersion ? { webVersion } : {}),
      ...params
    }

    return config;
  },
  (error) => {
    delLoading()
    return Promise.reject(error);
  }
);

  const whiteCodeList = [60002, 70011];
  // 响应拦截
  http.interceptors.response.use(
    (res) => {
      delLoading()
      if(res.config.responseType === 'stream' || res.config.responseType === 'blob'){
        return res;
      }

      if(['/user/login'].includes(res.config.url)){
        return res.data;
      }
      if (res.status == 200 && (res.data.code === 0 || whiteCodeList.includes(res.data.code))) {
        return res.data;
      } else {
        iframeConfig.addPostMessageHttpStatus(res.data.code,res.data);
        if(iframeConfig?.isIframe()){
          iframeConfig.runPostMessage();
          return Promise.reject(res.data);
        }else{
          iframeConfig.isIframeRunPostMessage();
        }

        let resErr_text = "";
        if (res.status != 200) {
          resErr_text = "与服务器网络连接断开，请稍后重试或联系管理员";
        } else {
          resErr_text = res.data.msg || "请求发生异常，请稍后重试或联系管理员";
        }
        if (res.data.code === 4001) {
          const url = window.location?.hash?.split('?')[0]?.split('/')?.[1];
          let whiteList = []
          let webUrlList = []
          try {
            const permissionModule = require('@/router/permission')
            whiteList = permissionModule?.whiteList || []
            webUrlList = permissionModule?.webUrlList || []
          } catch (e) {}
          if([...whiteList,...webUrlList].includes(url)){
            return Promise.reject(res.data);
          }else{
            clearLoginInfo();
            router.replace({ name: "login" });
          }
        }
        Message.error(resErr_text);
        return Promise.reject(res.data);
      }
    },
    (error) => {
      console.log(error);
      delLoading()
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

  return http;
};

const whiteCodeList = [60002];
const http = applyInterceptors(createHttpInstance());
// 判断是否用加载页面
const isLoading = function(url,load){
  let l = load === undefined ? true : load;
  httpLoading.edit(url,l)
}

/**
 * 获取请求扩展配置
 * @param {Object} option 请求选项
 * @returns {Object}
 */
const getRequestConfig = (option = {}) => {
  const config = {
    ...(option?.config || {})
  };
  if (option.baseURL) {
    config.baseURL = option.baseURL;
  }
  return config;
};

/**
 * 发起通用请求
 * @param {import('axios').AxiosInstance} instance axios实例
 * @param {Object} requestConfig 请求配置
 * @param {Object} option 请求选项
 * @returns {Promise<any>}
 */
const requestWithInstance = (instance, requestConfig = {}, option = {}) => {
  const method = (requestConfig.method || 'get').toLowerCase();
  const config = {
    ...requestConfig,
    ...getRequestConfig(option)
  };
  if (config.params && !option.notFormat) {
    config.params = myUtils.httpFormat(config.params);
  }
  if (config.data && !option.notFormat) {
    config.data = myUtils.httpFormat(config.data);
  }
  if (['post', 'put', 'delete'].includes(method)) {
    isLoading(config.url, option.load);
  }
  return instance(config);
};

/**
 * 创建支持自定义baseURL的请求客户端
 * @param {Object} clientConfig 客户端默认配置
 * @returns {{request: Function, get: Function, post: Function, put: Function, del: Function, withBaseURL: Function}}
 */
export const createRequestClient = (clientConfig = {}) => {
  const { baseURL, ...restConfig } = clientConfig || {};
  const instance = applyInterceptors(createHttpInstance({
    ...restConfig,
    ...(baseURL ? { baseURL } : {})
  }));

  return {
    request(config = {}, option = {}) {
      return requestWithInstance(instance, config, option);
    },
    get(url, params, option = {}) {
      return requestWithInstance(instance, {
        url,
        method: 'get',
        params
      }, option);
    },
    post(url, data, option = {}) {
      return requestWithInstance(instance, {
        url,
        method: 'post',
        data
      }, option);
    },
    put(url, data, option = {}) {
      return requestWithInstance(instance, {
        url,
        method: 'put',
        data
      }, option);
    },
    del(url, data, option = {}) {
      return requestWithInstance(instance, {
        url,
        method: 'delete',
        data
      }, option);
    },
    withBaseURL(nextBaseURL) {
      return createRequestClient({
        ...clientConfig,
        baseURL: nextBaseURL
      });
    }
  };
};
/**
 * get方法，对应get请求
 * @param {String} url [请求的url地址]
 * @param {Object} params [请求时携带的参数]
 */

export const get = (url, params, option={}) => {
  return http({
    url,
    params:params && !option.notFormat?myUtils.httpFormat(params):params,
    ...option?.config || {}
  });
};

/**
 * post方法，对应post请求
 * @param {String} url [请求的url地址]
 * @param {Object} data [请求时携带的参数]
 */
export const post = (url, data, option={}) => {
  isLoading(url,option.load)
  return http({
    url,
    data: data && !option.notFormat ? myUtils.httpFormat(data) :data,
    method: "post",
    ...option?.config || {}
  });
};

/**
 * put方法，对应put请求
 * @param {String} url [请求的url地址]
 * @param {Object} data [请求时携带的参数]
 */
export const put = (url, data, option={}) => {
  isLoading(url,option.load)
  return http({
    url,
    data: data && !option.notFormat ? myUtils.httpFormat(data) :data,
    method: "put",
    ...option?.config || {}
  });
};

/**
 * delete方法，对应delete请求
 * @param {String} url [请求的url地址]
 * @param {Object} params [请求时携带的参数]
 */
export const del = (url, params, option={}) => {
  isLoading(url,option.load)
  return http({
    url,
    data: params && !option.notFormat ? myUtils.httpFormat(params) : params,
    method: "delete",
    ...option?.config || {}
  });
};

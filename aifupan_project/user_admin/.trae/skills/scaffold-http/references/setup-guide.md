# HTTP Layer Setup Guide

## 1. Initialize HTTP Layer

**Config**: `src/http/httpConfig/index.js`

```javascript
/**
 * @file http/index.js
 * @description HTTP请求封装，包含httpBack(服务器接口)和httpClient(客户端接口)
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// 创建axios实例
const createService = (baseURL) => {
  const service = axios.create({
    baseURL,
    timeout: 5000 // 请求超时时间
  })

  // 请求拦截器
  service.interceptors.request.use(
    (config) => {
      const userStore = useUserStore()
      if (userStore.token) {
        config.headers['Authorization'] = `Bearer ${userStore.token}`
      }
      return config
    },
    (error) => {
      console.log(error)
      return Promise.reject(error)
    }
  )

  // 响应拦截器
  service.interceptors.response.use(
    (response) => {
      const res = response.data
      // 这里根据实际后端约定进行调整，假设code 200为成功
      if (res.code && res.code !== 200) {
        ElMessage({
          message: res.message || 'Error',
          type: 'error',
          duration: 5 * 1000
        })

        // 401: 未登录或token过期
        if (res.code === 401) {
          const userStore = useUserStore()
          userStore.logout().then(() => {
            router.push(`/login?redirect=${router.currentRoute.value.fullPath}`)
          })
        }
        return Promise.reject(new Error(res.message || 'Error'))
      } else {
        return res
      }
    },
    (error) => {
      console.log('err' + error)
      ElMessage({
        message: error.message,
        type: 'error',
        duration: 5 * 1000
      })
      return Promise.reject(error)
    }
  )

  return service
}

// 假设服务器接口地址
const httpBack = createService(import.meta.env.VITE_APP_BASE_API || '/api')

export { httpBack }
export default httpBack
```

**Main Entry**: `src/http/index.js`

```javascript
import api from './api'
import { httpBack } from './httpConfig'

export { httpBack }
export default api
```

**API Entry**: `src/http/api/index.js`

```javascript
// import user from './user'

export default {
  // user,
}
```

## 2. Add API Module

**Template**: `src/http/api/{name}.js`

```javascript
import http from './../httpConfig'

export default {
  /**
   * @description Example Method
   * @param {Object} params
   * @returns {Promise}
   */
  exampleMethod: (params) => http.get('/url', { params }),

  /**
   * @description Create Method
   * @param {Object} data
   * @returns {Promise}
   */
  create: (data) => http.post('/create', data)
}
```

**Registration**: Update `src/http/api/index.js`

```javascript
import order from './order'
// ...
export default {
  // ...
  order
}
```

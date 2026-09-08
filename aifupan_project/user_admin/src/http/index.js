/**
 * @file http/index.js
 * @description HTTP 模块入口：聚合导出 API 模块与 httpBack 实例
 */
import api from './api'
import { httpBack } from './httpConfig'

export { httpBack }
export default api

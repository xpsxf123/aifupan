/**
 * H5页面在不同环境下的跳转地址配置
 * @description 根据当前环境返回对应的H5页面基础URL
 */

/**
 * 环境配置映射表
 * @type {Object}
 */
const H5_URL_CONFIG = {
  test: '/h5',
  development: 'http://localhost:5173',
  production: '/h5',
  release: '/h5',
  custom: ''
}

export const getH5BaseUrl = (env) => {
  const envName = env || SITE_CONFIG.env;
  return H5_URL_CONFIG[envName] || ''
}



export default {
  getH5BaseUrl,
  H5_URL_CONFIG
}
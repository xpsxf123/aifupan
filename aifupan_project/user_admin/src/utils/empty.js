/**
 * @file empty.js
 * @description 空白页显示辅助：仅在“默认查询条件”下展示引导型空白页
 */

/**
 * @description 判断值是否为“空查询值”
 * @param {*} value - 查询值
 * @returns {boolean}
 */
export const isBlankSearchValue = (value) => {
  if (Array.isArray(value)) return value.length === 0
  return value === undefined || value === null || value === ''
}

/**
 * @description 判断当前查询参数是否仍处于默认态
 * @param {Object} params - 当前查询参数
 * @param {Object} [defaults={}] - 默认查询参数
 * @param {string[]} [ignoreKeys=[]] - 忽略的参数键
 * @returns {boolean}
 */
export const isDefaultSearchParams = (params = {}, defaults = {}, ignoreKeys = []) => {
  const ignored = new Set(ignoreKeys)
  const keys = Array.from(new Set([...Object.keys(params || {}), ...Object.keys(defaults || {})]))

  return keys.every((key) => {
    if (ignored.has(key)) return true
    const current = params?.[key]
    const fallback = defaults?.[key]

    if (fallback !== undefined) {
      if (Array.isArray(fallback) || Array.isArray(current)) {
        return JSON.stringify(current || []) === JSON.stringify(fallback || [])
      }
      return current === fallback
    }

    return isBlankSearchValue(current)
  })
}

/**
 * @description 仅在默认查询且首屏无数据时显示引导型空白页
 * @param {Object} options - 计算选项
 * @param {boolean} options.firstLoaded - 是否完成首次加载
 * @param {Array} options.list - 当前列表
 * @param {Object} options.params - 查询参数
 * @param {Object} [options.defaults={}] - 默认查询参数
 * @param {string[]} [options.ignoreKeys=[]] - 忽略参数键
 * @returns {boolean}
 */
export const shouldShowGuideEmpty = ({ firstLoaded, list, params, defaults = {}, ignoreKeys = [] }) => {
  const isEmpty = Array.isArray(list) && list.length === 0
  if (!firstLoaded || !isEmpty) return false
  return isDefaultSearchParams(params, defaults, ignoreKeys)
}

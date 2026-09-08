/**
 * @file options.js
 * @description 下拉选项数据格式工具（统一使用 { key, label }）
 */
import { unref } from 'vue'

/**
 * @description 归一化下拉选项数据为 { key, label } 结构（兼容后端/历史字段）
 * @param {Array|Object} input - 选项数组或 ref/computed 包裹的数组
 * @returns {Array<{key:any,label:string,raw?:any}>}
 */
export const normalizeKeyLabelOptions = (input) => {
  const resolved = unref(input)
  if (!Array.isArray(resolved)) return []

  return resolved
    .filter((item) => item !== null && item !== undefined)
    .map((item) => {
      if (typeof item !== 'object') {
        return { key: item, label: String(item) }
      }

      const key =
        item.key !== undefined
          ? item.key
          : item.value !== undefined
            ? item.value
            : item.id !== undefined
              ? item.id
              : undefined

      const label =
        item.label !== undefined
          ? String(item.label)
          : item.name !== undefined
            ? String(item.name)
            : key !== undefined
              ? String(key)
              : ''

      if (key === undefined || key === null) return null
      return { ...item, key, label }
    })
    .filter(Boolean)
}

/**
 * @description 根据 key 获取选项 label
 * @param {Array|Object} options - 选项数组或 ref/computed 包裹的数组
 * @param {any} key - 选项 key
 * @returns {string}
 */
export const getOptionLabelByKey = (options, key) => {
  const list = normalizeKeyLabelOptions(options)
  const target = list.find((i) => i.key === key)
  return target?.label || ''
}

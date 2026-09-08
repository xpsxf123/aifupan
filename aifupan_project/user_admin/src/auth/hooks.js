/**
 * @file auth/hooks.js
 * @description 权限组合式函数
 */
import { usePermissionStore } from './store'

export function usePermission() {
  const permissionStore = usePermissionStore()

  /**
   * 检查是否有权限
   * @param {string|string[]} value 权限码或权限码数组
   * @returns {boolean}
   */
  const hasPermission = (value) => {
    return permissionStore.hasPermission(value)
  }

  /**
   * 检查写权限
   * @param {string[]} codes 额外需要的权限码
   * @returns {boolean}
   */
  const canWrite = (codes = []) => {
    // 这里简单实现：检查是否有 codes 中的权限
    // 如果需要检查 'WRITE' 关键字，可以在这里处理
    // 例如：检查 'admin:module:WRITE'，但 hooks 不知道 module
    // 所以假设 codes 包含了完整的权限码
    if (codes.length === 0) return true
    return hasPermission(codes)
  }

  /**
   * 检查读权限
   * @param {string[]} codes
   * @returns {boolean}
   */
  const canRead = (codes = []) => {
    if (codes.length === 0) return true
    return hasPermission(codes)
  }

  return {
    hasPermission,
    canWrite,
    canRead,
    permissions: permissionStore.permissions
  }
}

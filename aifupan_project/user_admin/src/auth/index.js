/**
 * @file auth/index.js
 * @description 权限模块统一出口
 */

export * from './config'
export * from './core'
export * from './store'
export * from './hooks'
export * from './directives'
import './guard' // 执行路由守卫

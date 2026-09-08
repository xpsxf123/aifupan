/**
 * @file auth/core.js
 * @description 权限核心逻辑：转换器与验证器
 */

/**
 * 权限配置转换器
 * 将嵌套的配置对象转换为扁平的权限码数组
 */
export class PermissionConverter {
  /**
   * 转换配置对象为权限码列表
   * @param {Object} config 权限配置
   * @param {string} prefix 前缀
   * @returns {string[]} 权限码数组
   */
  convert(config, prefix = '') {
    let permissions = []

    // 处理数组写法
    if (Array.isArray(config)) {
      config.forEach((item) => {
        permissions.push(prefix ? `${prefix}:${item}` : item)
      })
      return permissions
    }

    // 处理对象写法
    if (typeof config === 'object' && config !== null) {
      for (const key in config) {
        const value = config[key]
        const currentCode = prefix ? `${prefix}:${key}` : key

        if (value === true) {
          // 简写: add: true -> admin:home:add
          permissions.push(currentCode)
        } else if (typeof value === 'string') {
          // 别名: edit: 'userEdit' -> admin:home:userEdit
          permissions.push(prefix ? `${prefix}:${value}` : value)
        } else if (Array.isArray(value)) {
          // 数组: MENU: ['add', 'edit'] -> admin:home:MENU:add (这种一般比较少见，通常是直接作为子级)
          // 根据文档示例: dashboard: ['view', 'export'] -> admin:dashboard:view
          // 这里需要根据 key 是否是特殊字段或者子模块来判断
          // 如果 key 是模块名，value 是数组，则拼接
          value.forEach((item) => {
            permissions.push(`${currentCode}:${item}`)
          })
        } else if (typeof value === 'object') {
          // 递归处理子级
          // 如果是 LABEL 字段，通常是元数据，不生成权限码，跳过
          if (key === 'LABEL') continue

          // 如果是 platform 字段，通常是根配置，不需要拼接 key，直接作为 prefix
          // 但根据文档 config = { platform: 'admin', home: { ... } }
          // platform: 'admin' 只是一个属性，不是子节点
          if (typeof value !== 'object') {
            // 忽略非对象属性，比如 platform: 'admin'
            continue
          }

          // 递归
          permissions = permissions.concat(this.convert(value, currentCode))
        }
      }
    }

    return permissions
  }

  /**
   * 专门处理文档中定义的 Config 结构
   * @param {Object} config
   */
  convertConfig(config) {
    const platform = config.platform || ''
    let permissions = []

    for (const key in config) {
      if (key === 'platform') continue

      const value = config[key]
      // 模块级
      permissions = permissions.concat(this.convert(value, platform ? `${platform}:${key}` : key))
    }

    return permissions
  }
}

/**
 * 权限验证器
 */
export class PermissionValidator {
  /**
   * 验证是否有权限
   * @param {string[]} userPermissions 用户拥有的权限列表
   * @param {string|string[]} requiredPermissions 需要的权限
   * @returns {boolean}
   */
  check(userPermissions, requiredPermissions) {
    if (!userPermissions || !Array.isArray(userPermissions)) return false

    // 超级管理员权限
    if (userPermissions.includes('*') || userPermissions.some((p) => p.endsWith(':*'))) {
      // 简单的 * 检查，更复杂的在 match 中处理
    }

    // 将需要的权限转为数组
    const needed = Array.isArray(requiredPermissions) ? requiredPermissions : [requiredPermissions]

    // 只要满足其中一个需要的权限即可 (OR 逻辑，通常 v-auth="['a','b']" 意味着拥有 a 或 b 即可访问？
    // 或者是 AND 逻辑？通常权限检查是 OR，即 "我有这个权限或者那个权限都能看这个按钮"
    // 但如果是 "需要同时拥有A和B才能操作"，则是 AND。
    // 根据文档 v-auth="'code'" 是单权限。
    // 这里的实现假设 OR 逻辑：只要满足 needed 中的任意一个即可。

    return needed.some((need) => {
      return userPermissions.some((have) => this.match(have, need))
    })
  }

  /**
   * 匹配两个权限码
   * @param {string} have 用户拥有的权限
   * @param {string} need 需要的权限
   * @returns {boolean}
   */
  match(have, need) {
    if (have === need) return true
    if (have === '*') return true

    // 处理通配符
    // admin:* 匹配 admin:home:edit
    // admin:home:* 匹配 admin:home:edit
    // admin:*:MENU 匹配 admin:home:MENU

    const haveParts = have.split(':')
    const needParts = need.split(':')

    // 如果拥有的是 *，直接通过
    if (haveParts.length === 1 && haveParts[0] === '*') return true

    for (let i = 0; i < haveParts.length; i++) {
      const h = haveParts[i]
      // 如果拥有的是 *，则匹配该层级及之后所有
      if (h === '*') {
        // 特殊情况：admin:*:MENU
        // 如果 * 不是最后一位，需要继续匹配后续
        if (i < haveParts.length - 1) {
          // * 在中间，例如 admin:*:MENU
          // 这种匹配比较复杂，简单实现：
          // 暂时只支持 * 在末尾的情况，或者 * 匹配任意单层

          // 如果 * 是最后一位，则匹配成功
          // admin:home:* -> 匹配 admin:home:edit (parts: 3 vs 3) 或 admin:home:edit:view (3 vs 4)
          // 所以如果 h 是 * 且是最后一位，则 match
          if (i === haveParts.length - 1) return true

          // 如果 * 不是最后一位，则跳过这一层比较
          // admin:*:MENU vs admin:home:MENU
          // i=1, h=*, needParts[1]=home -> match
          // continue to i=2
          if (i >= needParts.length) return false // need 更短，不匹配
          continue
        } else {
          // * 是最后一位
          return true
        }
      }

      if (i >= needParts.length) return false // need 比 have 短，且 have 没遇到 *

      if (h !== needParts[i]) return false
    }

    // 如果 have 遍历完了，检查 need 是否也遍历完了
    // 例如 have=admin:home, need=admin:home:edit -> false (除非 have 结尾是 *)
    if (haveParts.length < needParts.length) {
      // have 已经结束了，但 need 还有
      // 除非 have 的最后一位是 * (已经处理过)
      return false
    }

    return true
  }
}

export const permissionConverter = new PermissionConverter()
export const permissionValidator = new PermissionValidator()

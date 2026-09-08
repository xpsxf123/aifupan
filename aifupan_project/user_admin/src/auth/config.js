/**
 * @file auth/config.js
 * @description 权限配置文件
 */

export const permissionConfig = {
  platform: 'admin',

  // 首页模块
  dashboard: {
    view: true, // admin:dashboard:view
    export: true,
    MENU: true, // 菜单权限
    WRITE: true
  },

  // 用户管理模块
  user: {
    list: true,
    add: true,
    edit: true,
    delete: true,
    MENU: true,
    WRITE: true,
    LABEL: {
      list: '查看用户列表',
      add: '添加用户',
      edit: '编辑用户',
      delete: '删除用户'
    }
  },

  // 排班管理模块
  schedule: {
    demo: true,
    MENU: true,
    WRITE: true
  }
}

// 角色定义 (可选，如果前端控制角色)
export const roleConfig = {
  super_admin: ['*'],
  admin: ['*'], // Add admin role
  admin_manager: ['admin:dashboard:*', 'admin:user:*', 'admin:schedule:*'],
  visitor: ['admin:dashboard:view', 'admin:dashboard:MENU']
}

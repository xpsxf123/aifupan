/**
 * @file config/menu.js
 * @description 路由配置和导航菜单配置
 */

import { componentDocsMenu } from './dev/component-menu'

export const menuConfig = [
  // 我的排班和业绩
  {
    path: '/my-schedule-performance',
    name: 'MySchedulePerformance',
    component: 'Layout',
    redirect: '/my-schedule-performance/schedule',
    meta: {
      title: '我的排班和业绩',
      icon: 'mySchedulePerformance',
      isNav: true
    },
    children: [
      {
        path: 'schedule',
        name: 'PersonalSchedule',
        component: 'views/MyScheduleAndPerformance/PersonalSchedule',
        meta: {
          title: '个人排班',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'my:schedule:list'
        }
      },
      {
        path: 'performance',
        name: 'PersonalPerformance',
        component: 'views/MyScheduleAndPerformance/PersonalPerformance',
        meta: {
          title: '个人业绩',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'my:performance:list'
        }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: 'views/MyScheduleAndPerformance/UserProfile',
        meta: {
          title: '个人资料',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'my:profile:page'
        }
      }
    ]
  },
  // 业绩汇总
  {
    path: '/performance-summary',
    name: 'PerformanceSummary',
    component: 'Layout',
    redirect: '/performance-summary/group-compass',
    meta: {
      title: '业绩汇总',
      icon: 'performanceSummary',
      isNav: true
    },
    children: [
      {
        path: 'group-compass',
        name: 'GroupCompass',
        component: 'views/PerformanceSummary/GroupCompass',
        meta: {
          title: '集团业绩罗盘',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'performance:summary:group-compass:page'
        }
      },
      {
        path: 'branch-performance',
        name: 'BranchPerformance',
        component: 'views/PerformanceSummary/BranchPerformance/index',
        redirect: '/performance-summary/branch-performance/data',
        meta: {
          title: '各分公司业绩',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'performance:summary:branch-performance:list'
        },
        children: [
          {
            path: 'data',
            name: 'BranchData',
            component: 'views/PerformanceSummary/BranchPerformance/BranchData/index',
            meta: {
              title: '分公司数据',
              icon: 'mySchedulePerformance',
              isNav: false
            }
          },
          {
            path: 'detail/:id',
            name: 'BranchDetail',
            component: 'views/PerformanceSummary/BranchPerformance/BranchDetail/index',
            meta: {
              title: '分公司详情',
              icon: 'mySchedulePerformance',
              isNav: false,
              activeMenu: '/performance-summary/branch-performance/data'
            }
          }
        ]
      },
      {
        path: 'department-performance',
        name: 'DepartmentPerformance',
        component: 'views/PerformanceSummary/DepartmentPerformance/index',
        redirect: '/performance-summary/department-performance/data',
        meta: {
          title: '各部门业绩',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'performance:summary:department-performance:list'
        },
        children: [
          {
            path: 'data',
            name: 'DepartmentData',
            component: 'views/PerformanceSummary/DepartmentPerformance/BranchData/index',
            meta: {
              title: '部门数据',
              icon: 'mySchedulePerformance',
              isNav: false
            }
          },
          {
            path: 'detail/:id',
            name: 'DepartmentDetail',
            component: 'views/PerformanceSummary/DepartmentPerformance/BranchDetail/index',
            meta: {
              title: '部门详情',
              icon: 'mySchedulePerformance',
              isNav: false,
              activeMenu: '/performance-summary/department-performance/data'
            }
          }
        ]
      },
      {
        path: 'team-performance',
        name: 'TeamPerformance',
        component: 'views/PerformanceSummary/TeamPerformance/index',
        redirect: '/performance-summary/team-performance/data',
        meta: {
          title: '各小组业绩',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'performance:summary:team-performance:list'
        },
        children: [
          {
            path: 'data',
            name: 'TeamData',
            component: 'views/PerformanceSummary/TeamPerformance/TeamData/index',
            meta: {
              title: '各小组数据',
              icon: 'mySchedulePerformance',
              isNav: false
            }
          },
          {
            path: 'detail/:id',
            name: 'TeamDetail',
            component: 'views/PerformanceSummary/TeamPerformance/TeamDetail/index',
            meta: {
              title: '小组详情',
              icon: 'mySchedulePerformance',
              isNav: false,
              activeMenu: '/performance-summary/team-performance/data'
            }
          }
        ]
      },
      {
        path: 'live-room-performance',
        name: 'LiveRoomPerformanceSummary',
        component: 'views/PerformanceSummary/LiveRoomPerformance/index',
        meta: {
          title: '各直播间业绩',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'performance:summary:live-room-performance:list'
        }
      }
    ]
  },
  // 商品排行榜
  {
    path: '/product-ranking',
    name: 'ProductRanking',
    component: 'views/ProductRanking/index',
    meta: {
      title: '商品排行榜',
      icon: 'productRanking',
      isNav: true,
      permissionCode: 'product:ranking:list'
    }
  },
  // 直播间排班
  {
    path: '/live-room-ranking',
    name: 'LiveRoomScheduleParent',
    component: 'Layout',
    redirect: '/live-room-ranking/index',
    meta: {
      title: '直播间排班',
      icon: 'liveRoomScheduleParent',
      isNav: true
    },
    children: [
      {
        path: 'index',
        name: 'LiveRoomSchedule',
        component: 'views/LiveRoomRanking/index',
        meta: {
          title: '直播间排班',
          icon: 'liveRoomScheduleParent',
          isNav: true,
          permissionCode: 'room:schedule:list'
        }
      },
      {
        path: 'detail',
        name: 'LiveRoomScheduleDetail',
        component: 'views/LiveRoomRanking/Detail',
        meta: {
          title: '直播间排班详情',
          icon: 'mySchedulePerformance',
          isNav: false,
          activeMenu: '/live-room-ranking/index'
        }
      }
    ]
  },
  // 直播间业绩
  {
    path: '/live-room-performance',
    name: 'LiveRoomPerformanceParent',
    component: 'Layout',
    redirect: '/live-room-performance/index',
    meta: {
      title: '直播间业绩',
      icon: 'liveRoomPerformanceParent',
      isNav: true
    },
    children: [
      {
        path: 'index',
        name: 'LiveRoomPerformanceList',
        component: 'views/LiveRoomPerformance/List/index',
        meta: {
          title: '直播间业绩',
          icon: 'liveRoomPerformanceParent',
          isNav: true,
          permissionCode: 'room:performance:list'
        }
      },
      {
        path: 'detail/:liveRoomId',
        name: 'LiveRoomPerformanceDetailPage',
        component: 'views/LiveRoomPerformance/index',
        meta: {
          title: '直播间业绩详情',
          icon: 'mySchedulePerformance',
          isNav: false,
          activeMenu: '/live-room-performance/index'
        }
      },
      {
        path: 'history/:liveRoomId',
        name: 'LiveRoomPerformanceHistory',
        component: 'views/LiveRoomPerformance/History/index',
        meta: {
          title: '查看历史数据',
          icon: 'mySchedulePerformance',
          isNav: false,
          activeMenu: '/live-room-performance/index'
        }
      }
    ]
  },
  // 人员业绩和排班
  {
    path: '/staff-management',
    name: 'StaffManagement',
    component: 'Layout',
    redirect: '/staff-management/performance',
    meta: {
      title: '人员业绩和排班',
      icon: 'staffManagement',
      isNav: true
    },
    children: [
      {
        path: 'performance',
        name: 'StaffPerformance',
        component: 'views/StaffPerformanceAndSchedule/StaffPerformance',
        meta: {
          title: '人员业绩',
          icon: 'staffManagement',
          isNav: true,
          permissionCode: 'sys:employee:performance:list'
        }
      },
      {
        path: 'performance/detail/:employeeId',
        name: 'StaffPerformanceDetail',
        component: 'views/StaffPerformanceAndSchedule/StaffPerformance/Detail',
        meta: {
          title: '人员业绩详情',
          icon: 'mySchedulePerformance',
          isNav: false,
          activeMenu: '/staff-management/performance'
        }
      },
      {
        path: 'schedule',
        name: 'StaffSchedule',
        component: 'views/StaffPerformanceAndSchedule/StaffSchedule',
        meta: {
          title: '人员排班',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'sys:employee:schedule:list'
        }
      },
      {
        path: 'schedule-detail',
        name: 'StaffScheduleDetail',
        component: 'views/StaffPerformanceAndSchedule/StaffSchedule/Detail',
        meta: {
          title: '个人排班详情',
          icon: 'mySchedulePerformance',
          isNav: false,
          activeMenu: '/staff-management/schedule'
        }
      }
    ]
  },
  // 部门和人员
  {
    path: '/department-staff',
    name: 'DepartmentStaff',
    component: 'Layout',
    redirect: '/department-staff/staff',
    meta: {
      title: '部门和人员',
      icon: 'departmentStaff',
      isNav: true
    },
    children: [
      {
        path: 'staff',
        name: 'Staff',
        component: 'views/DepartmentAndStaff/Staff/index',
        meta: {
          title: '人员',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'sys:employee:manage:list'
        }
      },
      {
        path: 'live-room',
        name: 'LiveRoomMgmt',
        component: 'views/DepartmentAndStaff/LiveRoom/index',
        meta: {
          title: '直播间',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'room:mgmt:list'
        }
      },
      {
        path: 'team',
        name: 'Team',
        component: 'views/DepartmentAndStaff/Team/index',
        meta: {
          title: '小组',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'org:team:manage:list'
        }
      },
      {
        path: 'department',
        name: 'Department',
        component: 'views/DepartmentAndStaff/Department/index',
        meta: {
          title: '部门',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'org:dept:manage:list'
        }
      },
      {
        path: 'subsidiary',
        name: 'Subsidiary',
        component: 'views/DepartmentAndStaff/Subsidiary/index',
        meta: {
          title: '子公司',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'org:sub-company:manage:list'
        }
      }
    ]
  },
  // 基础设置
  {
    path: '/basic-settings',
    name: 'BasicSettings',
    component: 'Layout',
    redirect: '/basic-settings/position',
    meta: {
      title: '基础设置',
      icon: 'basicSettings',
      isNav: true
    },
    children: [
      {
        path: 'position',
        name: 'Position',
        component: 'views/BasicSettings/Position/index',
        meta: {
          title: '岗位',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'org:position:list'
        }
      },
      {
        path: 'role',
        name: 'RoleManagement',
        component: 'views/BasicSettings/Role/index',
        meta: {
          title: '角色管理',
          icon: 'mySchedulePerformance',
          isNav: true,
          permissionCode: 'sys:role:page'
        }
      }
    ]
  }
]

// 开发环境动态添加组件文档菜单
if (import.meta.env.DEV) {
  menuConfig.push(componentDocsMenu)
}

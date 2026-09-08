/**
 * @file config/dev/component-menu.js
 * @description 开发环境组件测试菜单配置
 */

export const componentDocsMenu = {
  path: '/component-docs',
  name: 'ComponentDocs',
  component: 'Layout',
  redirect: '/component-docs/empty-state',
  meta: { title: '组件文档 (Dev)', icon: 'Document', isNav: true, showTooltip: true },
  children: [
    {
      path: 'empty-state',
      name: 'DocEmptyState',
      component: 'views/ComponentDocs/EmptyState/index',
      meta: { title: 'EmptyState 空白页', isNav: true }
    },
    {
      path: 'icon-image',
      name: 'DocIconImage',
      component: 'views/ComponentDocs/IconImage/index',
      meta: { title: 'IconImage 图标', isNav: true }
    },
    {
      path: 'product-info',
      name: 'DocProductInfo',
      component: 'views/ComponentDocs/ProductInfo/index',
      meta: { title: 'ProductInfo 商品信息', isNav: true }
    },
    {
      path: 'department-card',
      name: 'DocDepartmentCard',
      component: 'views/ComponentDocs/DepartmentCard/index',
      meta: { title: 'DepartmentCard 部门卡片', isNav: true }
    },
    {
      path: 'data-card',
      name: 'DocDataCard',
      component: 'views/ComponentDocs/DataCard/index',
      meta: { title: 'DataCard 数据卡片', isNav: true }
    },
    {
      path: 'data-card-group',
      name: 'DocDataCardGroup',
      component: 'views/ComponentDocs/DataCardGroup/index',
      meta: { title: 'DataCardGroup 卡片组', isNav: true }
    },
    {
      path: 'custom-tabs',
      name: 'DocCustomTabs',
      component: 'views/ComponentDocs/CustomTabs/index',
      meta: { title: 'CustomTabs 标签页', isNav: true }
    },
    {
      path: 'date-quick-picker',
      name: 'DocDateQuickPicker',
      component: 'views/ComponentDocs/DateQuickPicker/index',
      meta: { title: 'DateQuickPicker 日期选择', isNav: true }
    },
    {
      path: 'data-trend-chart',
      name: 'DocDataTrendChart',
      component: 'views/ComponentDocs/DataTrendChart/index',
      meta: { title: 'DataTrendChart 趋势图', isNav: true }
    },
    {
      path: 'data-overview',
      name: 'DocDataOverview',
      component: 'views/ComponentDocs/DataOverview/index',
      meta: { title: 'DataOverview 数据概览', isNav: true }
    },
    {
      path: 'curd',
      name: 'DocCurd',
      component: 'views/ComponentDocs/Curd/index',
      meta: { title: 'Curd 通用列表', isNav: true }
    },
    {
      path: 'data-ranking-list',
      name: 'DocDataRankingList',
      component: 'views/ComponentDocs/DataRankingList/index',
      meta: { title: 'DataRankingList 数据排行', isNav: true }
    },
    {
      path: 'permission-select',
      name: 'DocPermissionSelect',
      component: 'views/ComponentDocs/PermissionSelect/index',
      meta: { title: 'PermissionSelect 权限选择', isNav: true }
    },
    {
      path: 'page-tabs',
      name: 'DocPageTabs',
      component: 'views/ComponentDocs/PageTabs/index',
      meta: { title: 'PageTabs 页面标签页', isNav: true }
    },
    {
      path: 'alert-box',
      name: 'DocAlertBox',
      component: 'views/ComponentDocs/AlertBox/index',
      meta: { title: 'AlertBox 警告框', isNav: true }
    },
    {
      path: 'breadcrumb',
      name: 'DocBreadcrumb',
      component: 'views/ComponentDocs/Breadcrumb/index',
      meta: { title: 'Breadcrumb 面包屑', isNav: true }
    },
    {
      path: 'common-dialog',
      name: 'DocCommonDialog',
      component: 'views/ComponentDocs/CommonDialog/index',
      meta: { title: 'CommonDialog 通用弹窗', isNav: true }
    },
    {
      path: 'data-pie-chart',
      name: 'DocDataPieChart',
      component: 'views/ComponentDocs/DataPieChart/index',
      meta: { title: 'DataPieChart 数据饼图', isNav: true }
    },
    {
      path: 'funnel-chart',
      name: 'DocFunnelChart',
      component: 'views/ComponentDocs/FunnelChart/index',
      meta: { title: 'FunnelChart 漏斗图', isNav: true }
    },
    {
      path: 'global-search',
      name: 'DocGlobalSearch',
      component: 'views/ComponentDocs/GlobalSearch/index',
      meta: { title: 'GlobalSearch 全局搜索', isNav: true }
    },
    {
      path: 'notification-drawer',
      name: 'DocNotificationDrawer',
      component: 'views/ComponentDocs/NotificationDrawer/index',
      meta: { title: 'NotificationDrawer 消息抽屉', isNav: true }
    },
    {
      path: 'page-header-info',
      name: 'DocPageHeaderInfo',
      component: 'views/ComponentDocs/PageHeaderInfo/index',
      meta: { title: 'PageHeaderInfo 页头信息', isNav: true }
    },
    {
      path: 'performance-components',
      name: 'DocPerformanceComponents',
      component: 'views/ComponentDocs/PerformanceComponents/index',
      meta: { title: 'Performance 业绩组件', isNav: true }
    },
    {
      path: 'schedule',
      name: 'DocSchedule',
      component: 'views/ComponentDocs/Schedule/index',
      meta: { title: 'Schedule 排班表', isNav: true }
    },
    {
      path: 'sms-code-input',
      name: 'DocSmsCodeInput',
      component: 'views/ComponentDocs/SmsCodeInput/index',
      meta: { title: 'SmsCodeInput 验证码', isNav: true }
    },
    {
      path: 'user-profile-card',
      name: 'DocUserProfileCard',
      component: 'views/ComponentDocs/UserProfileCard/index',
      meta: { title: 'UserProfileCard 用户卡片', isNav: true }
    }
  ]
}

# 爱复盘（AI Fu Pan）项目详细文档

> 基于对项目源代码的完整阅读和分析，独立生成，不依赖 docs/ 目录。

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈](#2-技术栈)
3. [目录结构](#3-目录结构)
4. [应用入口与初始化流程](#4-应用入口与初始化流程)
5. [环境系统与配置](#5-环境系统与配置)
6. [路由系统](#6-路由系统)
7. [状态管理](#7-状态管理)
8. [HTTP 请求架构](#8-http-请求架构)
9. [API 签名与安全](#9-api-签名与安全)
10. [C# 桌面客户端桥接](#10-c-桌面客户端桥接)
11. [iframe 通信层](#11-iframe-通信层)
12. [双版本体系](#12-双版本体系)
13. [权限系统](#13-权限系统)
14. [组件体系](#14-组件体系)
15. [Mixins 混入系统](#15-mixins-混入系统)
16. [业务模块详解](#16-业务模块详解)
17. [布局系统](#17-布局系统)
18. [App.vue 根组件](#18-appvue-根组件)
19. [工具函数库](#19-工具函数库)
20. [关键用户流程](#20-关键用户流程)
21. [构建与部署](#21-构建与部署)
22. [开发指南](#22-开发指南)
23. [附录：枚举与常量](#23-附录枚举与常量)

---

## 1. 项目概述

### 1.1 项目简介

**爱复盘（AI Fu Pan）** 是一个直播智能复盘分析应用，前端基于 **Vue 2 + Element UI** 构建。支持三种运行模式：

| 运行模式 | 说明 | 判断方式 |
|---------|------|---------|
| 桌面客户端 | WebView 嵌入 C# WPF/WinForms 桌面应用 | `navigator.userAgent.indexOf('aifupan') >= 0` |
| 独立 Web | 在浏览器中作为普通 SPA 运行 | `navigator.userAgent.indexOf('aifupan') < 0` |
| iframe 嵌入 | 嵌入第三方页面通过 iframe | URL 参数 `?iframe` 或 `sessionStorage.iframe` |

### 1.2 核心功能

- **直播录制**：支持抖音、快手、视频号三大平台的自动录制与监控
- **AI 复盘分析**：整场直播的 AI 智能分析（内容诊断 + 数据诊断）
- **视频/文件分析**：上传视频或文案文件进行智能分析
- **切片分析**：对直播片段或短视频进行独立复盘
- **对比分析**：多场直播的智能对比分析
- **词库管理**：敏感词、关键词库的创建与维护
- **短视频功能**：达人搜索与订阅、爆款搜索与订阅、文案提取
- **云空间**：云端存储、云端分析与分享
- **数据看板**：可视化数据展示与截图导出
- **AI 助手**：基于对话的 AI 运营辅助
- **排班业绩**：直播间排班与业绩管理

### 1.3 版本体系

应用存在两种版本模式，通过客户端配置切换：

| 版本 | Vuex 标识 | 说明 |
|------|----------|------|
| AI 全能版（代理版） | `AGENT` | 完整功能，包含 AI 分析、对比等全部模块 |
| 纯录制版 | `PURE` | 精简版，仅保留录制和基础查看功能 |

版本切换通过调用客户端 API `/config/setClientVersionConfig` 实现，切换后重启应用生效。

---

## 2. 技术栈

### 2.1 核心框架

| 技术 | 版本 | 文件来源 |
|------|------|---------|
| Vue.js | 2.6.11 | `package.json` |
| Vue Router | 3.2.0 | `package.json` |
| Vuex | 3.4.0 | `package.json` |
| Element UI | 2.15.10 | `package.json` |
| Axios | 0.27.2 | `package.json` |

### 2.2 重要依赖及用途

| 依赖 | 版本 | 用途 | 关键使用位置 |
|------|------|------|-------------|
| ECharts | 5.4.1 | 图表展示（曲线图、雷达图、柱状图等） | `src/echarts/index.js`，`components/analysis/compass/` |
| Video.js | 8.22.0 | 视频播放器核心 | `components/video/index.vue` |
| WangEditor | 4.7.15 | 富文本编辑器 | `components/editor/` |
| CryptoJS | 4.2.0 | HMAC-SHA256 签名、MD5 摘要 | `utils/signature.js` |
| FingerprintJS | 4.6.2 | 浏览器/设备指纹生成 | `utils/signature.js` |
| html2canvas | 1.4.1 | HTML 截图导出 | `components/analysis/dataScreenshot/` |
| jsPDF | 2.5.1 | PDF 文档生成 | `utils/createPdf.js` |
| docx | 9.5.1 | Word 文档生成 | `utils/downDocx.js` |
| xlsx | 0.18.5 | Excel 处理 | `components/cu-upload-excel/` |
| vuex-persistedstate | 4.1.0 | Vuex 状态持久化到 localStorage | `store/index.js` |
| shepherd.js | 14.3.0 | 新手引导/Tour | `utils/shepherd.js`，`views/layout/common/tour.vue` |
| tesseract.js | 5.0.1 | OCR 文字识别 | 用于图片中文字提取 |
| Swiper | 5.4.5 | 轮播图 | 登录页 banner 等场景 |
| vue-contextmenujs | 1.4.11 | 右键菜单 | 全局注册，`mixins/contextmenu.js` |
| vuedraggable | 2.24.3 | 拖拽排序 | 词库管理等场景 |
| vue-json-excel | - | Excel 导出组件 | 全局注册为 `<downloadExcel>` |
| markdown-it | 14.1.0 | Markdown 渲染 | AI 内容渲染 |

### 2.3 构建工具链

| 工具 | 版本 | 用途 |
|------|------|------|
| @vue/cli-service | ~4.5.15 | Webpack 封装构建工具 |
| @vue/cli-plugin-babel | ~4.5.15 | Babel 转译 |
| Sass/Sass-loader | 1.93.3 / 8.0.2 | SCSS 编译 |
| Less/Less-loader | 4.2.0 / 6.0.0 | Less 编译（Element UI 主题） |
| cross-env | 7.0.3 | 跨平台环境变量设置 |
| clean-webpack-plugin | - | 构建前清理 dist |
| mockjs | 1.1.0 | Mock 数据服务 |

---

## 3. 目录结构

```
D:\Work\fupan-client\
├── public/
│   ├── index.html              # HTML 模板（注入 SITE_CONFIG、百度统计、51.la 统计）
│   └── static/favicon.ico
│
├── src/
│   ├── main.js                 # 应用入口：注册全局插件、挂载实例
│   ├── App.vue                 # 根组件：CSharpNotify 任务注册、版本切换、全局弹窗
│   │
│   ├── assets/
│   │   ├── fonts/font/         # 字体文件（阿里巴巴普惠体、小米 Misans、iconfont）
│   │   ├── imgs/               # 图片资源（按版本分目录：1_9_30, 2_5_8, 2_6_0, 2_6_3）
│   │   ├── json/menu.json      # ★ 动态路由菜单定义（核心配置文件）
│   │   ├── scss/               # 全局样式（bh.scss 入口、Element 主题变量、通用类）
│   │   └── video/test.ts       # 视频测试文件
│   │
│   ├── components/             # 公共组件（~50 个组件目录）
│   │   ├── analysis/           # ★ 核心分析组件组（最大组件群）
│   │   │   ├── index.vue       # 分析主组件
│   │   │   ├── ai/             # AI 复盘子组件
│   │   │   ├── aiElf/          # AI 精灵引导
│   │   │   ├── compass/        # 分析罗盘（barChat, chat, model, radarMap, table）
│   │   │   ├── curveData/      # 曲线数据（含切片滑块）
│   │   │   ├── dataBoard/      # 数据看板
│   │   │   ├── dataScreenshot/ # 数据截图
│   │   │   ├── performance/    # 性能分析
│   │   │   └── scrolling/      # 滚动列表（export, namePop, scrollingList, showOther）
│   │   ├── Annotation/         # 视频标注系统
│   │   ├── buyIn/              # 巨量百应接入
│   │   ├── coreTable/          # 核心表格（带搜索、分页、操作列）
│   │   ├── cu-upload/          # 自定义上传组件
│   │   ├── DiscernSearchContainer/ # 智能搜索容器
│   │   ├── editor/             # 富文本编辑器
│   │   ├── eleUi/afpUi/       # 自定义 UI 组件库（按钮等扩展）
│   │   ├── fileRemark/         # 文件备注
│   │   ├── form/formItemAry/   # 表单组件（DatePicker, Input, Radio, Select）
│   │   ├── forcedUpdate/       # 强制更新弹窗
│   │   ├── globalConfirm/      # 全局确认对话框
│   │   ├── message/            # 自定义消息/通知（Message.js, Notification.js）
│   │   ├── notes/              # 笔记系统（export, textChannel）
│   │   ├── optimizeActions/    # 优化操作（Compare）
│   │   ├── reviewContrast/     # 审查对比
│   │   ├── summary/            # AI 总结
│   │   ├── Table/Tabs/         # 表格和标签页
│   │   ├── TaskQueue/          # 任务队列浮标
│   │   ├── textParagraph/      # 文本段落（含标注标记）
│   │   ├── video/              # ★ Video.js 视频播放器封装
│   │   ├── VideoAndWordContainer/ # 视频+文字双栏容器
│   │   ├── wordClass/          # 词类选择
│   │   └── [20+ 其他组件：anchor, contentSlide, contextmenu, dialog, drawer, empty, fold, letterShake, page, polish, restScreen, scrollbar, search, shareImgModel, shareUpload, tardeDialog, title, tradeId, upgradeTooltip, warmHint]
│   │
│   ├── config/
│   │   ├── common.js           # 全局 UI 默认配置（按钮样式、表格配置）
│   │   ├── env/index.js        # runMode 判断（isAifupan / isWeb）
│   │   ├── h5Url/index.js      # H5 页面 URL 配置
│   │   └── permission/index.js # v-auth 指令 + $auth / $addAuthCode 方法
│   │
│   ├── constants/
│   │   └── webVersion.js       # 版本号常量
│   │
│   ├── directive/
│   │   └── newHint.js          # 新功能提示指令
│   │
│   ├── echarts/
│   │   └── index.js            # ECharts 初始化/主题配置
│   │
│   ├── enum/
│   │   └── index.js            # 枚举定义（平台类型、版本类型、清晰度等级）
│   │
│   ├── env/
│   │   └── index.js            # 运行时环境标志（dev/prod/test/release/custom）
│   │
│   ├── icons/
│   │   ├── iconfont.js         # SVG 图标精灵
│   │   ├── index.js            # 图标注册入口
│   │   └── svg/                # 单独 SVG 文件
│   │
│   ├── mixins/                 # Vue Mixins（27 个文件）
│   │   ├── mixins.js           # 基础全局混入（注入到所有组件）
│   │   ├── common.js           # 通用方法混入
│   │   ├── analysisMixin.js    # 分析功能混入
│   │   ├── contrastMixin.js    # 对比分析混入
│   │   ├── aiContentReport.js  # AI 内容报告混入（App.vue 使用）
│   │   ├── aiDataReport.js     # AI 数据报告混入
│   │   ├── aiContentRenderReport.js # AI 内容渲染报告
│   │   ├── aiCanvasImage.js    # AI Canvas 图像混入
│   │   ├── aiTypeMixin.js      # AI 类型相关
│   │   ├── auth.js             # 权限验证混入
│   │   ├── buyIn.js            # 巨量百应混入
│   │   ├── clickHiden.js       # 点击隐藏面板
│   │   ├── commonHttp.js       # 通用 HTTP 混入
│   │   ├── contextmenu.js      # 右键菜单混入
│   │   ├── dialog.js           # 弹窗管理
│   │   ├── drawer.js           # 抽屉管理
│   │   ├── form.js             # 表单逻辑
│   │   ├── ifDay15.js          # 15 天判断
│   │   ├── resize.js           # 尺寸调整
│   │   ├── routerRepairStop.js # 路由修复
│   │   ├── setTimeOut.js       # 定时器管理
│   │   ├── share.js            # 分享功能
│   │   ├── slot.js             # 插槽混入
│   │   ├── table.js            # 表格逻辑
│   │   ├── tabs.js             # 标签页逻辑
│   │   ├── viewVideo.js        # 视频查看
│   │   └── exampleMixin.js     # 案例混入
│   │
│   ├── router/
│   │   ├── index.js            # 路由实例 + 静态路由 + 动态路由注入
│   │   └── permission.js       # 路由守卫（beforeEach / afterEach）
│   │
│   ├── store/
│   │   └── index.js            # Vuex Store（单一 Store，无模块拆分）
│   │
│   ├── utils/                  # 工具函数（30+ 文件）
│   │   ├── request-back.js     # ★ 云端请求客户端工厂（Axios 实例 + 拦截器）
│   │   ├── request-api-back.js # ★ 云端 API 方法定义（39 个模块，200+ 接口）
│   │   ├── request-client.js   # ★ 客户端请求层（Axios 实例 + 拦截器）
│   │   ├── request-api-client.js # ★ 客户端 API 方法定义（18 个模块，100+ 接口）
│   │   ├── request-back2.js    # ★ 二级后端请求客户端（工厂模式）
│   │   ├── signature.js        # ★ HMAC-SHA256 签名 + FingerprintJS
│   │   ├── iframeConfig/index.js # ★ iframe postMessage 通信
│   │   ├── notifyFromcsharp.js # ★ C# 宿主通知处理（CSharpNotify 机制）
│   │   ├── common.js           # 通用工具（复制分享链接、数据源解析、分享 URL 组装）
│   │   ├── utils.js            # 基础工具函数集
│   │   ├── index.js            # clearLoginInfo 等
│   │   ├── init.js             # 应用初始化（获取机器码等）
│   │   ├── windLoad.js         # 窗口加载指令
│   │   ├── dialogDrag.js       # 弹窗拖拽功能
│   │   ├── createPdf.js        # PDF 生成
│   │   ├── downDocx.js         # Word 文档下载
│   │   ├── downFile.js         # 文件下载
│   │   ├── downloadFile.js     # 通用下载
│   │   ├── downloadHtml.js     # HTML 下载
│   │   ├── copy.js             # 剪贴板操作
│   │   ├── printService.js     # 打印服务
│   │   ├── autoUploadCloud.js  # 自动上传云空间
│   │   ├── platformSource.js   # 平台来源判断
│   │   ├── webVersion.js       # Web 版本解析
│   │   ├── httpLoading.js      # HTTP 请求加载状态锁管理
│   │   ├── aiCreateEle.js      # AI 内容 DOM 元素创建
│   │   ├── mdFormat.js         # Markdown 格式化
│   │   ├── mockUrlConfig.js    # Mock URL 配置
│   │   ├── laTrack.js          # 51.la 统计追踪
│   │   ├── banError.js         # 封禁错误处理
│   │   ├── diskError.js        # 磁盘错误处理
│   │   ├── icon.js / iconImg.js # 图标工具
│   │   ├── tour.js / shepherd.js # 新手引导
│   │   └── ...
│   │
│   └── views/
│       ├── layout/
│       │   ├── index.vue       # ★ 主布局（侧边栏 + 标题栏 + 内容区 + 弹窗）
│       │   ├── routerView.vue  # 二级路由视图
│       │   └── common/         # 布局子组件
│       │       ├── title/      # 标题栏（含标签页切换）
│       │       ├── left/nav.vue # 左侧导航菜单
│       │       ├── left/disk.vue # 磁盘空间显示
│       │       ├── left/version.vue # 版本信息显示
│       │       └── tour.vue    # 新手引导
│       ├── pages/              # 独立页面（不通过布局渲染）
│       │   ├── login/          # 登录页
│       │   ├── 404/            # 404 页
│       │   ├── versionSelection/ # 版本选择
│       │   ├── shipinhao/      # 视频号授权状态
│       │   ├── onlineAnalysis/ # 在线分析分享页
│       │   ├── contrastOnlineAnalysis/ # 在线对比分享页
│       │   ├── aiShare/        # AI 分享页面
│       │   ├── official-online/ # 官方在线页面
│       │   └── common/shareLayout.vue # 分享页布局
│       ├── modules/            # 业务模块页面（通过 menu.json 动态路由加载）
│       │   ├── home/           # 首页/仪表盘
│       │   ├── dataAnalysis/   # 数据分析（主播列表 + hotList 热榜）
│       │   ├── addCompere/     # 添加主播（douyin.vue, kuaishou.vue, shipinhao.vue）
│       │   ├── compere/        # 主播管理
│       │   ├── replay/         # ★ 复盘核心
│       │   │   ├── replayAll/  # 整场复盘列表
│       │   │   ├── replaySection/ # 切片复盘列表
│       │   │   ├── replayShort/ # 短视频复盘列表
│       │   │   ├── analysis/   # 分析详情页
│       │   │   ├── aiAnalysis/ # AI 助手页（only.vue, contrast.vue）
│       │   │   ├── contrast/   # 对比分析
│       │   │   ├── fileUpload/ # 文件上传分析
│       │   │   └── online/     # 云空间分析
│       │   ├── playBckAnalysis/ # 文件分析（uploadVideo, uploadText, uploadSlice, uploadShort）
│       │   ├── shortVideo/     # 短视频模块
│       │   │   ├── extractDoc/ # 提取文案
│       │   │   ├── expert/     # 搜达人
│       │   │   ├── hotItem/    # 搜爆款
│       │   │   ├── subscribeExpert/ # 订阅达人
│       │   │   └── subscribeHotItem/ # 订阅爆款
│       │   ├── aiAssistant/    # AI 运营助手
│       │   ├── aiViolation/    # AI 违规助手
│       │   ├── aiMixin/        # AI 共享混入逻辑
│       │   ├── scheduling/     # 排班业绩
│       │   ├── lexicon/        # 词库管理
│       │   ├── online/         # 云空间列表
│       │   ├── system/         # 系统设置（restScreen 屏保等）
│       │   ├── renewal/        # 续费
│       │   ├── invite/         # 邀请/会员福利
│       │   ├── redirect/       # 中转页
│       │   ├── readonly/       # 只读分析
│       │   ├── example/        # 复盘案例
│       │   └── anniversary/    # 周年庆活动
│       └── commonComponent/    # 跨页面共享的视图级组件
│           ├── analysisLayout/ # 分析页面通用布局
│           ├── aiAnalysis/     # AI 分析视图组件
│           ├── aiReport/       # AI 报告弹窗组件
│           ├── onlineAnalysisDialog.vue # 在线分析上传弹窗
│           ├── onlineContrastAnalysisDialog.vue # 在线对比上传弹窗
│           ├── versionQrCode.vue # 版本二维码
│           ├── updateVersion.vue # 版本更新弹窗
│           ├── sliceAnalysis.vue # 切片分析
│           ├── dayFirstDialog.vue # 每日首次弹窗
│           ├── customerServiceQrCode.vue # 客服二维码
│           ├── markDialog.vue  # 标记弹窗
│           ├── videoMark.vue / videoMarkAdd.vue # 视频标记
│           └── [更多...]
│
├── ipConfig/
│   └── index.js                # ★ 多环境 API 地址 + 平台代码 + 签名密钥
│
├── mockjs/
│   ├── index.js                # Mock 数据定义
│   └── server.js               # Mock 服务器
│
├── scripts/
│   └── updateWebVersion.js     # 构建时自动更新 Web 版本号
│
├── dist/                       # 构建输出
├── node_modules/
├── vue.config.js               # ★ Vue CLI / Webpack 配置
├── babel.config.js             # Babel 配置
├── .browserslistrc             # 浏览器兼容目标
├── package.json
└── README.md
```

---

## 4. 应用入口与初始化流程

### 4.1 入口文件 `src/main.js`

应用的启动分为两个阶段：**同步注册** 和 **异步初始化**。

#### 阶段一：同步注册（67 行）

```javascript
// 1. 注册 Element UI
Vue.use(ElementUI);

// 2. 注册自定义 UI 库 afpUi
Vue.use(afpUi);

// 3. 全局注册 Video.js 播放器组件
Vue.component(VideoJsPlayer.name, VideoJsPlayer);

// 4. 注册全局混入
Vue.mixin(commonMixin);     // 通用方法
Vue.mixin(myMixin);         // 基础混入

// 5. 注册权限系统
Vue.use(auth);              // v-auth 指令 + $auth / $addAuthCode

// 6. 注册其他插件
Vue.use(windLoad);          // 窗口加载指令
Vue.use(Contextmunu);       // 右键菜单
Vue.use(GlobalConfirm);     // 全局确认框

// 7. 注册自定义指令
Vue.directive('new-hint', newHint);
Vue.directive('removeAriaHidden', { ... });

// 8. 挂载全局属性到 Vue.prototype
Vue.prototype.$httpBack = httpBack;         // 云端 API
Vue.prototype.$httpBack2 = httpBack2;       // 二级后端 API
Vue.prototype.$httpClient = httpClient;     // 客户端 API
Vue.prototype.$echarts = echarts;           // ECharts 实例
Vue.prototype.$CONFIG = CONFIG;             // 全局 UI 配置
Vue.prototype.$isWeb = env.isWeb;           // 是否 Web 模式
Vue.prototype.$isAifupan = env.isAifupan;   // 是否桌面客户端
Vue.prototype.$CSharpNotify = notifyFromCsharp; // C# 通知处理
Vue.prototype.$cNotify = Notification;      // 自定义通知（防与 ElementUI 冲突）
Vue.prototype.$cMsg = Message;              // 自定义消息
Vue.prototype.$isMobile = iframeConfig.isMobile(); // 是否移动端
Vue.prototype.$fetchValue = function(obj, ...keys) { ... }; // 大小写兼容取值

// 9. 注册 Excel 导出组件
Vue.component('downloadExcel', JsonExcel);

// 10. iframe 初始化
sessionStorage.removeItem('iframe');
iframeConfig.init();

// 11. 监听父窗口 postMessage（用于 official-online 页面高度通知）
window.addEventListener('message', (event) => { ... });
```

#### 阶段二：异步初始化（130 行）

```javascript
import { initApp } from "@/utils/init";

initApp().then(() => {
    new Vue({
        router,
        store,
        render: h => h(App)
    }).$mount('#app');
});
```

### 4.2 `initApp()` 函数（`src/utils/init.js`）

仅在桌面客户端模式下执行：获取机器码并存入 Vuex。

```javascript
export async function initApp() {
    const isAiFuPan = navigator.userAgent?.indexOf('aifupan') >= 0;
    if (isAiFuPan) {
        try {
            const { data: result, code } = await httpClient.setup.getMachineCode();
            if (code === 0) {
                store.commit("setMachineCode", result);
            }
        } catch (e) {
            console.log(e);
        }
    }
}
```

机器码用于后续 API 签名中的设备指纹生成（格式：`desktop_${machineCode}`）。

### 4.3 HTML 模板注入（`public/index.html`）

构建时通过 `vue.config.js` 的 `chainWebpack` 向 HTML 注入全局变量：

```javascript
window.SITE_CONFIG = {
    env: '<%= RUN_ENV %>',
    backApiURL: '<%= BCAK_API_URL %>',
    clientApiURL: '<%= CLIENT_API_URL %>',
    // ...
};
```

---

## 5. 环境系统与配置

### 5.1 环境标识

通过环境变量 `RUN_ENV` 区分五种运行环境：

| RUN_ENV | 说明 | 构建命令示例 |
|---------|------|-------------|
| `development` | 本地开发 | `npm run serve` |
| `test` | 测试环境 | `npm run serve:test` |
| `release` | 预发布环境 | `npm run build:release` |
| `production` | 生产环境 | `npm run build` |
| `custom` | 自定义环境 | `npm run serve:custom` |

### 5.2 `ipConfig/index.js` —— 多环境配置中心

```javascript
module.exports = {
    // 云端后端 API 地址
    back: {
        test/development: 'https://testapi.aifupan.com.cn/replay',
        production:        'https://api.aifupan.com.cn/replay',
        release:           'https://api-yz.aifupan.com.cn/replay',
    },
    // 二级后端 API 地址
    back2: {
        test/development: 'https://testapi.aifupan.com.cn/api',
        production:        'https://api.aifupan.com.cn/api',
        release:           'https://api-yz.aifupan.com.cn/api',
    },
    // 本地 C# 客户端 API 地址（端口可从 UserAgent 中 port=xxx 动态获取）
    client: {
        default: 'http://127.0.0.1:10086/api',
    },
    // 各环境的平台代码（用于业务区分）
    platform_code: {
        test:       { client: 'aDjPSE8ySm', cloudSpace: 'beNjdKZbks', official: 'EaFeJzBwfI' },
        release:    { client: 'sE3aMz4In7', cloudSpace: 'erl5WDRvFL', official: 'xUyLqPsVPt' },
        production: { client: 'vf406vwooE', cloudSpace: 'm0Wx7Qx35M', official: 'wVkGMiAgl8' },
    },
    // 各环境的签名密钥配置
    signatureConfig: {
        test:        { appId: 'replay_test', signature: { _k: { _a: "...", _b: "...", _c: "..." } } },
        release:     { appId: 'replay_yz',   signature: { _k: { _a: "...", _b: "...", _c: "..." } } },
        production:  { appId: 'replay_prod', signature: { _k: { _a: "...", _b: "...", _c: "..." } } },
    },
};
```

### 5.3 `src/env/index.js` —— 运行时环境判断

```javascript
const ENV_TYPE = {
    dev: process.env.NODE_ENV === 'development',
    prod: process.env.NODE_ENV === 'production',
    test: process.env.RUN_ENV === 'test',
    release: process.env.RUN_ENV === 'release',
    custom: process.env.RUN_ENV === 'custom',
};
```

### 5.4 `src/config/env/index.js` —— 运行模式判断

```javascript
const isAifupan = window.navigator.userAgent?.indexOf('aifupan') >= 0;
const isWeb = window.navigator.userAgent?.indexOf('aifupan') < 0;
```

---

## 6. 路由系统

### 6.1 架构概述

路由系统采用 **静态路由 + JSON 驱动的动态路由** 混合模式：

- **静态路由**：在 `router/index.js` 的 `pageRoutes` 数组中硬编码
- **动态路由**：从 `assets/json/menu.json` 加载，调用 `fnAddDynamicMenuRoutes()` 注入
- 动态路由中有一个特殊的 `LayoutComponent`，其 children 包含了所有通过 layout 渲染的页面

### 6.2 静态路由表

```javascript
const pageRoutes = [
    { path: '/',                  name: 'login',                   component: Login },
    { path: '/login',             name: 'login',                   component: Login },
    { path: '/versionSelection',  name: 'versionSelection',        component: VersionSelection },
    { path: '/404',               name: '404',                     component: NotFound },
    { path: '/shipinhao-status',  name: 'shipinhaoStatus',         component: ShipinhaoStatus, meta: { notAPPinit: true } },
    { path: '/restScreen',        name: 'restScreen',              component: RestScreen },
    // 分享相关路由（使用 shareLayout）
    { path: '', name: 'shareLayout', component: ShareLayout, children: [
        { path: '/onlineAnalysis/:type/:id',         name: 'onlineAnalysis',         component: OnlineAnalysis },
        { path: '/contrastOnlineAnalysis/:id',       name: 'contrastOnlineAnalysis', component: ContrastOnlineAnalysis },
        { path: '/onlineAiAnalysis/:type/:fileType/:id', name: 'onlineAiAnalysis',   component: OnlineAiAnalysis },
        { path: '/contrastAiAnalysis/:type/:id',     name: 'contrastAiAnalysis',     component: ContrastAiAnalysis },
    ]},
    // AI 分享路由（使用 aiShareLayout）
    { path: '', name: 'aiShareLayout', component: AiShareLayout, children: [
        { path: '/ai-share/:type/:id/:shareId/:aiType', name: 'ai-share', component: AiShare },
    ]},
    { path: '/official-online',    name: 'official-online',        component: OfficialOnline },
    { path: '/web-anniversary',    name: 'web-anniversary',        component: Anniversary },
];
```

### 6.3 动态路由（`menu.json` 驱动）

`menu.json` 中的每个菜单项映射为一个路由：

```json
{
    "path": "/replay",
    "name": "replay",
    "filePath": "replay/replayAll/index.vue",
    "notLayout": false,
    "meta": {
        "title": "AI整场复盘",
        "notTabs": false,
        "notKeepAlive": false,
        "returnPath": false,
        "hideTabs": false
    },
    "children": [ ... ]
}
```

**字段说明：**

| 字段 | 说明 |
|------|------|
| `path` | 路由路径。`/` 开头为根路径，否则从 `views/modules/` 解析 |
| `filePath` | 组件文件路径。`/` 开头相对于 `views/`，否则相对于 `views/modules/` |
| `name` | 路由名称 |
| `notLayout` | 为 true 时，路由直接添加到 router 而非 Layout 的 children |
| `meta.title` | 页面标题（用于浏览器 title 和标签页） |
| `meta.notTabs` | 是否不显示标签页 |
| `meta.notKeepAlive` | 是否不使用 `<keep-alive>` 缓存 |
| `meta.returnPath` | 是否显示返回按钮 |
| `meta.hideTabs` | 是否隐藏顶栏 |
| `meta.returnTitle` | 返回按钮的文案 |
| `children` | 子路由数组 |

### 6.4 组件路径解析算法

`getComponent(filePath, pathType, ele)` 的核心逻辑：

1. 如果 `filePath` 以 `/` 开头 → 以 `src/views` 为根路径
2. 否则 → 以 `src/views/modules` 为根路径
3. 如果路径不以 `.vue` 结尾 → 自动补全规则：
    - 路径最后一段是 `index` → 补 `.vue`（`/xxx/index` → `/xxx/index.vue`）
    - 否则 → 补 `/index.vue`（`/xxx` → `/xxx/index.vue`）
4. 使用 `require()` 验证文件存在，不存在则跳过该路由

### 6.5 路由守卫（`router/permission.js`）

```
router.beforeEach((to, from, next) => {
    // 1. 资源页面（system/replay/addCompere/dataAnalysis/replay/analysis）
    //    → 同步调用 infoByClient 更新用户信息
    if (resourcesUrl.includes(to.path)) {
        httpBack.user.infoByClient({});
    }

    // 2. 开发环境 → 全开放
    if (env.dev) return next();

    // 3. 白名单 → 免登录
    if (whiteList.includes(to.name)) return next();

    // 4. Web 模式 → 仅开放 webUrlList
    if (configEnv.isWeb) {
        if (webUrlList.includes(to.name)) return next();
        return next({ path: '/404' });
    }

    // 5. 动态路由已加载或当前是登录页 → 放行
    if (to.name == 'login' || loadFlag) return next();
});

// afterEach: 设置 document.title
router.afterEach((to) => {
    document.title = getRouteTitle(to); // 默认为 '爱复盘'
});
```

**Web 模式可访问路由白名单：**
```javascript
webUrlList = ['login', '404', 'onlineAnalysis', 'contrastOnlineAnalysis',
              'onlineAiAnalysis', 'contrastAiAnalysis', 'official-online',
              'ai-share', 'web-anniversary'];
```

### 6.6 VueRouter push 的 catch 修复

```javascript
const originalPush = VueRouter.prototype.push;
VueRouter.prototype.push = function push(location, onResolve, onReject) {
    if (onResolve || onReject) return originalPush.call(this, location, onResolve, onReject);
    return originalPush.call(this, location).catch(err => err);
};
```

---

## 7. 状态管理

### 7.1 Store 架构

使用单一 Vuex Store（无 modules 拆分），位于 `src/store/index.js`。

### 7.2 State 定义

```javascript
state: {
    versionType: '',          // 版本类型 'AGENT' | 'PURE'
    userInfo: '',             // 用户信息对象
    token: '',                // JWT Token
    replayPageMenu: {},       // 复盘页面菜单状态
    homeMenu: '',             // 首页菜单索引
    detectionStatus: '',      // 录制检测状态
    startDetectionTime: '',   // 检测开始时间
    userproperty: {},         // 用户资产（AI 时长、算力包等）
    tabs: [],                 // 标签页列表
    tabsName: '',             // 当前激活标签名
    tabsClickCallback: null,  // 标签点击回调
    configInfo: {},           // 客户端基本设置信息
    TimeAccurate: 0,          // 时间校验计数器
    titleRightRender: null,   // 标题栏右侧渲染函数
    mode: false,              // 开发者模式
    compereInfo: {},          // 当前主播信息
    domZoom: 1,               // DOM 缩放比例
    routerPathBack: null,     // 路由返回路径
    machineCode: '',          // 机器码（桌面端）
    actionKey: 0,             // 操作锁 key
    versionTypeLoading: false, // 版本类型加载中标志
    pureValue: '',            // 纯录制版套餐等级值
    loadRouterFlag: false,    // 动态路由是否已加载
    stopRecord: '',           // 停止录制标识
    analysisTrade: '',        // 当前分析行业
    videoListQuery: {},       // 视频列表查询参数缓存
    fileListQuery: {},        // 文件列表查询参数缓存
    finishVideoListQuery: {}, // 完成视频列表查询参数缓存
    contrastListQuery: {},    // 对比列表查询参数缓存
    currentUploadVideoId: '', // 当前上传视频 ID
    setupMenu: '',            // 设置页菜单索引
    videoAnalysisId: '',      // 分析的视频 ID
    fileAnalysisId: '',       // 分析的文件 ID
}
```

### 7.3 关键 Getters

```javascript
getters: {
    getToken, getUserInfo, getVersionType, getUserproperty,
    getTabsName, getTabsList, getBiscInfo, getDetectionStatus,
    getUserType, getNickName, getTimeAccurate, getMode,
    getDomZoom, getMachineCode, getActionKey, getCompereInfo,

    // 套餐等级相关
    getPackageLevel:  (state) => state?.userInfo?.packageLevel,
    getPackageLevelName: (state) => state?.userInfo?.packageName,
    isFree:           (state) => state?.userInfo?.packageLevel === 0,
    isPure:           (state) => state?.userInfo?.packageLevel == state.pureValue,
    isActivated:      (state) => state?.userInfo?.packageLevel === -1,
    largeEnterprises: (state) => state?.userInfo?.packageLevel >= 20,
}
```

### 7.4 套餐等级体系

| packageLevel | 类型 | 说明 |
|-------------|------|------|
| -1 | 激活版 | 已激活但未付费 |
| 0 | 免费版 | 免费用户 |
| 10 | 个人版 | 个人付费用户 |
| 20 | 企业版 | 企业用户 |
| 30 | 旗舰版 | 最高等级 |

### 7.5 持久化配置

```javascript
plugins: [createPersistedstate({
    key: 'replayVuex',
    paths: [
        'userInfo', 'token', 'replayPageMenu', 'homeMenu',
        'detectionStatus', 'startDetectionTime', 'versionType'
    ]
})]
```

持久化到 `localStorage['replayVuex']`。

### 7.6 关键 Actions

- **`initVersionType`**：通过客户端 API 获取版本类型（`replay` → AGENT，其他 → PURE），在非 aifupan 环境（Web）中默认为 AGENT
- **`getPurePackageLevel`**：从云端 API 获取纯录制版套餐等级配置值

---

## 8. HTTP 请求架构

### 8.1 三层 API 架构

```
┌──────────────────────────────────────────────────────────────────┐
│                         前端应用层                                 │
│  Vue.prototype.$httpBack     →  request-api-back.js              │
│  Vue.prototype.$httpBack2    →  request-back2.js                 │
│  Vue.prototype.$httpClient   →  request-api-client.js            │
├──────────────────────────────────────────────────────────────────┤
│           │                     │                     │          │
│           ▼                     ▼                     ▼          │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────┐ │
│  │ 云端 API 服务    │ │ 二级 API 服务    │ │ C# 桌面客户端 API    │ │
│  │ /replay 路径     │ │ /api 路径       │ │ 127.0.0.1:10086/api │ │
│  │ Base: back[env]  │ │ Base: back2[env]│ │ UserAgent port 动态  │ │
│  └─────────────────┘ └─────────────────┘ └─────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### 8.2 云端 API 实例（`request-back.js` → `request-api-back.js`）

**Axios 实例创建：**
```javascript
const http = axios.create({
    baseURL: window.SITE_CONFIG["backApiURL"],
    timeout: 60 * 1000,
});
```

**请求拦截器：**
- 显示/隐藏 Loading 动画（通过 `httpLoading` 锁管理）
- 调用 `apiSignature.createSignatureHeaders()` 生成签名头
- 附加 `Token`（JWT）、`source`（平台来源）、`webVersion`

**响应拦截器：**
- 处理 `responseType: 'stream'` / `'blob'` → 返回原始 response
- 处理 `/user/login` → 直接返回 `res.data`
- 成功：`code === 0` 或白名单 `60002` → 返回 `res.data`
- `code === 4001`（Token 过期）→ 清除登录信息，跳转 login
- 其他错误 → Message.error 提示

**API 方法定义（`request-api-back.js`）：**
包含 39 个模块，覆盖：
- 用户认证（user, onlineUser）
- 主播管理（compere）
- 视频管理（video, fileAnalysis）
- 对比分析（contrast）
- 词库管理（lexicon, sensitivewords, cruxtype）
- AI 功能（aiRelated, ai, aiTrain, prompt）
- 短视频（shortVideo）
- 用户资产（userProperty, userPackage）
- 数据查询（v2000, v2100, v2200, v2300, v2400, v2500）
- 笔记（notes）、标记（analysisMark, videomark）
- 文件上传、字典、行业、文章、邀请、子账号、开放 API 等

### 8.3 客户端 API 实例（`request-client.js` → `request-api-client.js`）

**动态端口获取：**
```javascript
// 从 UserAgent 中提取端口号
const portMatch = userAgent.match(/port=(\d+)/);
if (portMatch && portMatch[1]) {
    window.SITE_CONFIG.clientApiURL = `http://127.0.0.1:${portMatch[1]}/api`;
}
```

**Web 模式拦截：**
```javascript
export const get = (url, params, option = {}) => {
    if (env.isWeb) { return Promise.reject(); }  // Web 模式下直接拒绝
    // ...
};
```

**超时策略：**
- 默认 60s
- 文件选择器、检测停止/启动、窗体关闭 → 300s
- 重新选择行业分析 → 300s
- 爆款搜索/更新 → 300s

**不显示 Loading 的白名单 URL：**
```
'/anchorinfo/getpageanchor', '/anchorvideo/getpage', '/config/getuserinfo',
'/config/getVersionUpdate', '/config/getmodel', '/config/getdisksize',
'/anchorvideo/shareanalysis', '/contrast/shareanalysis'
```

**API 方法定义（`request-api-client.js`）：**
包含 18 个模块：
- 窗体控制（form）
- 系统设置（setup）
- 主播管理（compere）
- 视频管理（video）
- 对比分析（contrast）
- 文件上传（uploadFile）
- 导出功能（export）
- 用户资产/信息（userProperty, user）
- AI 相关（aiRelated, system）
- 第三方平台（buyIn, qianchuan, douyin）
- 短视频（shortVideo）
- 数据同步（syncData）
- 测试（test）

### 8.4 二级后端 API（`request-back2.js`）

使用工厂模式 `createRequestClient()` 创建，支持：
- 默认实例（使用默认 baseURL）
- `withBaseURL()` 创建自定义地址实例
- `create()` 别名方法
- 预置的 `liveRoom` 模块（排班相关接口）

### 8.5 请求选项说明

所有 API 方法支持第三个参数 `option`：

```javascript
option = {
    load: true/false,      // 是否显示 Loading 动画
    notFormat: true/false, // 是否跳过参数格式化（myUtils.httpFormat）
    config: {},            // 额外的 axios 配置（如 responseType, timeout 等）
    baseURL: '',           // 覆盖 baseURL（仅 request-back2）
}
```

---

## 9. API 签名与安全

### 9.1 签名机制概述

所有云端 API 请求使用 **HMAC-SHA256** 签名验证，签名模块位于 `utils/signature.js`。

### 9.2 请求头（自动注入）

```javascript
{
    'X-App-Id': '<环境对应的 appId>',
    'X-Timestamp': '<Unix 毫秒时间戳>',
    'X-Nonce': '<16位随机字符串>',
    'X-RequestId': '<UUID v4>',
    'X-Fingerprint': '<设备指纹>',
    'X-Signature': '<HMAC-SHA256 签名>',
    'X-Sign-Type': 'HMAC-SHA256',
    'Token': '<JWT Token>',
    'source': '<平台来源>',
}
```

### 9.3 密钥管理

密钥以 Base64 编码存储在 `ipConfig/index.js` 的 `signatureConfig[env].signature._k` 中：

```javascript
_k: {
    _a: "<Base64 编码的密钥片段A>",
    _b: "<Base64 编码的密钥片段B>",
    _c: "<Base64 编码的密钥片段C>",
}
```

实际密钥 = `atob(_a) + atob(_b) + atob(_c)`。

### 9.4 签名生成流程

```javascript
// 1. 获取待签名内容
function _getContentToSign(method, url, body, contentType) {
    if (method === 'GET') {
        // 将 params 拼接为查询字符串，按 key 排序
        return _objectToQueryString(_parseQueryParams(fullPath));
    }
    if (['POST', 'PUT'].includes(method)) {
        if (contentType.includes('application/json')) {
            return JSON.stringify(body || {});
        }
        if (contentType.includes('x-www-form-urlencoded') || contentType.includes('multipart/form-data')) {
            return _formDataToString(body);
        }
    }
    return '';
}

// 2. 组装签名参数
const params = {
    'X-App-Id': appId,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-RequestId': requestId,
    'X-Fingerprint': fingerprint,
    body: contentToSign,
};

// 3. 参数排序后拼接为 stringToSign
// 4. 使用 HMAC-SHA256 生成签名
const signature = CryptoJS.enc.Base64.stringify(
    CryptoJS.HmacSHA256(stringToSign, appSecret)
);
```

### 9.5 设备指纹

```javascript
async function _getFingerprint() {
    const fp = await FingerprintJS.load();
    const result = await fp.get();
    const visitorId = result.visitorId;

    if (isAiFuPan) {
        // 桌面客户端：使用机器码
        return `desktop_${store.getters.getMachineCode}`;
    }
    // Web 模式：使用浏览器指纹
    return `web_${visitorId}`;
}
```

---

## 10. C# 桌面客户端桥接

### 10.1 概述

桌面客户端模式下，前端以 WebView 形式嵌入 C# 应用。C# 宿主通过调用 JS 函数 `window.notifyFromCsharp(requestData)` 与前端通信。

### 10.2 CSharpNotify 机制（`utils/notifyFromcsharp.js`）

**核心模式：** Task Collection（任务收集器）

```javascript
// 任务容器
const TaskCollection = {};

// 注册任务处理器
const addTask = (nameOrCode, callback, errCallback) => {
    if (typeof callback !== 'function') return;
    TaskCollection[nameOrCode] = (requestData, resFn, rejFn) => {
        let data = typeof requestData?.data === 'string'
            ? JSON.parse(requestData?.data)
            : requestData?.data || {};
        if (requestData.code === 0 && requestData.status === 200) {
            return callback(data, resFn);
        } else {
            if (typeof errCallback === 'function') {
                return errCallback(requestData, rejFn);
            }
        }
    };
};

// C# 调用入口（注册到 window 全局）
window.notifyFromCsharp = function(requestData) {
    return new Promise((resolve, reject) => {
        const task = TaskCollection[requestData?.action];
        if (task) {
            task(requestData, resolve, reject);
        } else {
            console.error('未找到对应任务', requestData?.action);
        }
    });
};
```

**C# 发来的消息格式：**
```json
{
    "code": 0,
    "status": 200,
    "action": "autoUploadCloud",
    "data": {
        "filePath": "D:\\...\\.mp4",
        "fileSize": 21,
        "duration": 83.201,
        "videoId": "ef1f2705-..."
    }
}
```

### 10.3 已注册的 CSharpNotify 任务

| action | 注册位置 | 功能 |
|--------|---------|------|
| `openUpdate` | `App.vue` → `watchForcedUpdate()` | 强制更新弹窗 |
| `generateDiagnosis` | `App.vue` → `aiReportFun()` | 生成 AI 诊断报告 |
| `sliceSuccess` | `App.vue` → `sliceSuccessAction()` | 切片完成通知 |
| `refreshDisk` | `layout/index.vue` → `addRefreshDisk()` | 刷新磁盘空间显示 |
| `notice` | `layout/index.vue` → `addNotice()` | 通用通知推送 |
| `userFrozen` | `layout/index.vue` → `addUserFrozen()` | 用户冻结通知 |
| `autoUploadCloud` | `layout/index.vue` → `autoUploadcloud()` | 自动上传云空间 |

### 10.4 前端返回值给 C#

```javascript
// 成功时 resolve
resolve(JSON.stringify({ code: 0, data: result }));

// 失败时 reject
reject(JSON.stringify({ code: -1, msg: 'error' }));
```

---

## 11. iframe 通信层

### 11.1 概述

当应用以 iframe 形式嵌入第三方页面时，使用 `postMessage` 进行跨窗口通信。配置位于 `utils/iframeConfig/index.js`。

### 11.2 核心机制

**iframe 状态检测：**
```javascript
// 通过 URL 参数 ?iframe 判断
const lookIframe = () => { sessionStorage.setItem('iframe', '1'); };
const isIframe = () => !!sessionStorage.getItem('iframe');
const isMobile = () => getQueryParam('isMobile');
```

**初始化流程：**
```javascript
const init = () => {
    sessionStorage.removeItem('iframe');
    if (getQueryParam('iframe')) {
        lookIframe();  // 有 ?iframe 参数时标记为 iframe 模式
    }
};
```

### 11.3 HTTP 状态转发

当 API 请求返回错误时（非 `code: 0`），通过 postMessage 将错误码和数据转发给父窗口：

```javascript
// 防抖 500ms 批量发送
const postMessageDb = myUtils.debounce(500);

const runPostMessage = () => {
    postMessageDb(() => {
        Object.keys(postMessageHttpStatus).forEach(key => {
            window.parent.postMessage({
                type: 'http',
                data: { ...postMessageHttpStatus[key] }
            }, '*');
            delete postMessageHttpStatus[key];
        });
    });
};
```

### 11.4 official-online 页面通信

`App.vue` 中的 `window.addEventListener('message', ...)` 监听来自父窗口的 `official-online` / `official-login` 消息，并返回页面高度信息。

---

## 12. 双版本体系

### 12.1 版本类型

| 版本 | Vuex 值 | 客户端配置值 | 说明 |
|------|---------|------------|------|
| AI 全能版 | `AGENT` | `replay` | 包含所有功能 |
| 纯录制版 | `PURE` | `record` | 仅录制和基础功能 |

### 12.2 版本判断流程

```javascript
// store/index.js → initVersionType action
async initVersionType({ commit, state }) {
    // 1. Web 模式下默认为 AGENT
    const isAiFuPan = navigator.userAgent?.indexOf('aifupan') >= 0;
    if (!isAiFuPan) {
        commit('setVersionType', VERSION_TYPE.AGENT);
    }

    // 2. 已确认版本则直接返回
    if (state.versionType && [VERSION_TYPE.AGENT, VERSION_TYPE.PURE].includes(state.versionType)) {
        return state.versionType;
    }

    // 3. 调用客户端 API 获取版本
    const response = await httpClient.setup.getVersionType();
    // response.data.clientVersion === 'replay' → AGENT
    // 其他 → PURE
    const versionType = clientVersion === 'replay' ? VERSION_TYPE.AGENT : VERSION_TYPE.PURE;
}
```

### 12.3 版本切换（Layout 中实现）

用户在纯录制版的侧边栏点击"切换成AI全能版"按钮时：

```javascript
async versionChange() {
    // 1. 检测到录制中则拦截
    if (this.$store.state.detectionStatus) return this.$message.info('录制中暂时无法切换版本');

    // 2. 调用客户端 API 切换
    const result = await this.$httpClient.setup.setClientVersionConfig({
        clientVersion: target === VERSION_TYPE.AGENT ? 'replay' : 'record',
        pageType: 0
    });

    // 3. 更新 Vuex 并强制刷新页面
    this.$store.commit("setVersionType", target);
    this.$router.go(0);
}
```

### 12.4 纯录制版路由限制

```javascript
// utils/common.js
const pureRouterList = [
    '/dataAnalysis', '/addCompere/douyin', '/addCompere/kuaishou',
    '/addCompere/shipinhao', '/replay', '/uploadVideo', '/uploadText',
    '/extractDoc', '/expert', '/subscribeExpert', '/subscribeHotItem', '/system'
];
```

---

## 13. 权限系统

### 13.1 权限码机制（`config/permission/index.js`）

```javascript
// 全局权限码列表
let authCodeList = [];

// 添加权限码
Vue.prototype.$addAuthCode = function(list) {
    authCodeList.push(...list);
};

// 检查权限
Vue.prototype.$auth = function(codeOrCodes, type) {
    if (Array.isArray(codeOrCodes)) {
        if (type === 'every') return codeOrCodes.every(c => authCodeList.includes(c));
        return codeOrCodes.some(c => authCodeList.includes(c));
    }
    return authCodeList.includes(codeOrCodes);
};

// 清除权限码
Vue.prototype.$removeAuthIds = function() {
    authCodeList = [];
};
```

### 13.2 v-auth 指令

```html
<!-- 无权限时直接移除 DOM 元素 -->
<el-button v-auth="'someCode'">权限按钮</el-button>
<el-button v-auth="['code1', 'code2']">多码权限</el-button>
```

### 13.3 权限码注入（App.vue）

```javascript
// 监听用户信息变化，重新注入权限码
watchAuthIds() {
    this.$watch(
        () => [this.$store.getters.getUserInfo.id, this.$store.getters.getUserInfo.activeTenantId],
        (newVal) => {
            this.$removeAuthIds();
            const { id, activeTenantId, normalPhone, username } = this.$store.getters.getUserInfo;
            this.$addAuthCode([
                id, activeTenantId, normalPhone, username,
                `${id}_0`, `${activeTenantId}_2`
            ]);
        },
        { deep: true, immediate: true }
    );
}
```

权限码包括：用户 ID、租户 ID、手机号、用户名及其组合衍生码。

### 13.4 套餐等级权限

组件中更常用的权限判断是通过 Vuex Getters：

```javascript
import { mapGetters } from 'vuex';

computed: {
    ...mapGetters(['isFree', 'isActivated', 'isPure', 'largeEnterprises', 'getPackageLevel'])
}
```

---

## 14. 组件体系

### 14.1 全局注册的组件

在 `main.js` 中全局注册：
- **`VideoJsPlayer`**（`components/video/index.vue`）—— Video.js 播放器封装
- **`downloadExcel`**（`vue-json-excel`）—— JSON 转 Excel 导出
- **`GlobalConfirm`**（`components/globalConfirm`）—— 全局确认对话框

### 14.2 核心组件详解

#### VideoJsPlayer（`components/video/index.vue`）
- 基于 Video.js 8.x 的播放器封装
- 支持本地文件和云空间视频播放
- 集成视频标记（videomark）功能
- 支持弹幕显示叠加

#### Analysis（`components/analysis/index.vue`）
- 核心分析容器组件
- 组合 AI 分析、曲线数据、数据看板、罗盘图表等子组件
- 是复盘分析页面的核心渲染组件

#### CoreTable（`components/coreTable/`）
- 通用表格组件
- 支持搜索过滤、分页、操作列
- 可配置搜索删除按钮样式（`searchDelType: 'danger'`）

#### CuUpload（`components/cu-upload/`）
- 文件上传组件
- 支持 Excel 上传（`components/cu-upload-excel/`）
- 集成上传进度和错误处理

#### TaskQueue（`components/TaskQueue/index.vue`）
- 浮标式任务队列指示器
- 显示云空间上传/分析任务的进行状态

#### Annotation（`components/Annotation/`）
- 视频标注系统
- annotationDialog（标注弹窗）+ annotationList（标注列表）
- 支持添加、编辑、删除标注

#### DiscernSearchContainer（`components/DiscernSearchContainer/`）
- 智能搜索容器
- 集成 AI 搜索按钮、可拖拽布局、导出弹窗、工具栏、行业树形列表

#### editor（`components/editor/`）
- 基于 WangEditor 的富文本编辑器封装

#### eleUi/afpUi（`components/eleUi/`）
- 对 Element UI 组件的二次封装
- 提供统一的按钮样式（默认 round + plain）

### 14.3 视图级公共组件（`views/commonComponent/`）

| 组件 | 文件 | 功能 |
|------|------|------|
| analysisLayout | `analysisLayout/` | 分析页面通用布局（含 textMixin, commonMixin, publicMixin, wordsMixin） |
| aiAnalysis | `aiAnalysis/` | AI 分析视图 |
| aiReport | `aiReport/` | AI 报告弹窗 |
| onlineAnalysisDialog | `onlineAnalysisDialog.vue` | 在线分析上传弹窗 |
| onlineContrastAnalysisDialog | `onlineContrastAnalysisDialog.vue` | 在线对比上传弹窗 |
| versionQrCode | `versionQrCode.vue` | 版本二维码/客服弹窗 |
| updateVersion | `updateVersion.vue` | 版本更新提示弹窗 |
| sliceAnalysis | `sliceAnalysis.vue` | 切片分析 |
| customerServiceQrCode | `customerServiceQrCode.vue` | 客服二维码 |
| dayFirstDialog | `dayFirstDialog.vue` | 每日首次打开提醒 |
| markDialog | `markDialog.vue` | 标记管理弹窗 |
| videoMark/videoMarkAdd | `videoMark.vue` | 视频标记管理 |

---

## 15. Mixins 混入系统

### 15.1 全局混入

**`mixins.js`**（`myMixin`）：通过 `Vue.mixin(myMixin)` 全局注册到所有组件实例。

**`common.js`**（`commonMixin`）：通过 `Vue.mixin(commonMixin)` 全局注册，提供通用方法。

### 15.2 功能型 Mixins

| Mixin | 文件 | 使用场景 |
|-------|------|---------|
| `analysisMixin` | `mixins/analysisMixin.js` | 分析页面逻辑复用 |
| `contrastMixin` | `mixins/contrastMixin.js` | 对比分析逻辑复用 |
| `aiContentReport` | `mixins/aiContentReport.js` | AI 内容报告（App.vue 使用） |
| `aiDataReport` | `mixins/aiDataReport.js` | AI 数据报告 |
| `aiContentRenderReport` | `mixins/aiContentRenderReport.js` | AI 内容渲染报告 |
| `aiCanvasImage` | `mixins/aiCanvasImage.js` | AI Canvas 图片处理 |
| `aiTypeMixin` | `mixins/aiTypeMixin.js` | AI 类型判断 |
| `exampleMixin` | `mixins/exampleMixin.js` | 复盘案例功能 |

### 15.3 UI 型 Mixins

| Mixin | 文件 | 功能 |
|-------|------|------|
| `table` | `mixins/table.js` | 表格通用逻辑（搜索、分页、排序） |
| `form` | `mixins/form.js` | 表单验证和提交逻辑 |
| `dialog` | `mixins/dialog.js` | 弹窗显隐管理 |
| `drawer` | `mixins/drawer.js` | 抽屉显隐管理 |
| `tabs` | `mixins/tabs.js` | 标签页切换逻辑 |
| `contextmenu` | `mixins/contextmenu.js` | 右键菜单（配合 vue-contextmenujs） |
| `resize` | `mixins/resize.js` | 窗口尺寸调整响应 |
| `slot` | `mixins/slot.js` | 插槽渲染逻辑 |

### 15.4 业务型 Mixins

| Mixin | 文件 | 功能 |
|-------|------|------|
| `buyIn` | `mixins/buyIn.js` | 巨量百应授权集成 |
| `auth` | `mixins/auth.js` | 权限验证 |
| `share` | `mixins/share.js` | 分享链接生成和复制 |
| `viewVideo` | `mixins/viewVideo.js` | 视频查看 |
| `commonHttp` | `mixins/commonHttp.js` | 通用 HTTP 请求逻辑 |

### 15.5 工具型 Mixins

| Mixin | 文件 | 功能 |
|-------|------|------|
| `clickHiden` | `mixins/clickHiden.js` | 点击隐藏（接受参数用于 localStorage 标记） |
| `ifDay15` | `mixins/ifDay15.js` | 15 天判断逻辑 |
| `routerRepairStop` | `mixins/routerRepairStop.js` | 路由修复/停止 |
| `setTimeOut` | `mixins/setTimeOut.js` | 定时器管理（组件销毁时自动清除） |

---

## 16. 业务模块详解

### 16.1 复盘核心模块（`views/modules/replay/`）

复盘模块是应用的核心，提供了多种复盘能力：

```
replay/
├── replayAll/index.vue       # AI 整场复盘 → 路由 /replay
├── replaySection/index.vue   # AI 切片复盘 → 路由 /section
├── replayShort/index.vue     # 短视频复盘 → 路由 /short
├── analysis/index.vue        # ★ 分析详情页（通用，被多个路由复用）
├── aiAnalysis/
│   ├── only.vue              # ★ 单场 AI 助手
│   └── contrast.vue          # ★ 对比 AI 助手
├── contrast/                 # 对比分析页
├── contrast/contrastReplay/  # 整场对比 → 路由 /contrastReplay
├── contrast/contrastSection/ # 切片对比 → 路由 /contrastSection
├── fileUpload/               # 文件上传分析页
│   ├── file-upload-analysis.vue  # 分析详情
│   └── aiAnalysis.vue            # 文件 AI 助手
└── online/                   # 云空间分析
    ├── analysis.vue          # 云空间分析详情
    ├── contrast.vue          # 云空间对比详情
    ├── aiAnalysis.vue        # 云空间 AI 助手
    └── aiContrast.vue        # 云空间 AI 对比助手
```

### 16.2 文件分析模块（`views/modules/playBckAnalysis/`）

```
playBckAnalysis/
├── index.vue         # 文件分析列表 → 路由 /playBckAnalysis
├── uploadVideo.vue   # 视频分析 → 路由 /uploadVideo
├── uploadText.vue    # 文案预审 → 路由 /uploadText
├── uploadSlice.vue   # 切片复盘 → 路由 /uploadSlice
└── uploadShort.vue   # 短视频切片 → 路由 /uploadShort
```

### 16.3 短视频模块（`views/modules/shortVideo/`）

| 子模块 | 路由 | 功能 |
|--------|------|------|
| extractDoc | `/extractDoc` | 短视频文案提取 |
| expert | `/expert` | 达人搜索 |
| subscribeExpert | `/subscribeExpert` | 订阅达人管理 |
| hotItem | `/hotItem` | 爆款搜索 |
| subscribeHotItem | `/subscribeHotItem` | 订阅爆款管理 |

### 16.4 主播管理（`views/modules/compere/` & `views/modules/addCompere/`）

- **compere/**：主播列表、主播详情、主播管理
- **addCompere/**：添加主播，支持三个平台：
    - `douyin.vue` → 抖音
    - `kuaishou.vue` → 快手
    - `shipinhao.vue` → 视频号

### 16.5 AI 模块

| 模块 | 路由 | 功能 |
|------|------|------|
| aiAssistant | `/aiAssistant` | AI 运营助手（对话式 AI 辅助运营） |
| aiViolation | `/aiViolation` | AI 违规助手（违规内容检测） |
| aiMixin | - | AI 模块共享的混入逻辑 |

### 16.6 其他模块

| 模块 | 路由 | 功能 |
|------|------|------|
| dataAnalysis | `/dataAnalysis` | 数据分析首页（主播直播间列表 + 热榜） |
| scheduling | `/scheduling` | 排班业绩管理 |
| lexicon | 无独立路由 | 词库管理（通过 system 路由子页访问） |
| online | `/online` | 云空间管理（视频/文件/切片/对比） |
| system | `/system` | 系统设置（含词库详情子页） |
| invite | `/invite` | 会员福利/邀请 |
| example | `/example` | 复盘案例 |
| anniversary | `/anniversary` 或 `/web-anniversary` | 周年庆活动 |
| redirect | `/redirect/:path*` | 路由中转 |

---

## 17. 布局系统

### 17.1 主布局（`views/layout/index.vue`）

主布局是一个全功能的桌面应用壳，结构如下：

```
┌──────────────────────────────────────────────────────┐
│ ┌──────────┐ ┌──────────────────────────────────────┐│
│ │          │ │  Title Bar (tabs navigation)         ││
│ │  Logo    │ ├──────────────────────────────────────┤│
│ │          │ │                                      ││
│ │  Nav     │ │                                      ││
│ │  Menu    │ │  <router-view />                     ││
│ │  (左侧   │ │  (with keep-alive)                   ││
│ │   导航)  │ │                                      ││
│ │          │ │                                      ││
│ │  Disk    │ │                                      ││
│ │  Space   │ │                                      ││
│ │          │ │                                      ││
│ │  Version │ │                                      ││
│ └──────────┘ └──────────────────────────────────────┘│
└──────────────────────────────────────────────────────┘
```

### 17.2 布局组件职责

| 组件 | 文件 | 职责 |
|------|------|------|
| Title | `common/title/title.vue` | 标题栏 + 标签页切换（tabs） |
| Nav | `common/left/nav.vue` | 左侧垂直导航菜单 |
| Disk | `common/left/disk.vue` | 磁盘空间使用情况 |
| Version | `common/left/version.vue` | 版本号显示 |
| Tour | `common/tour.vue` | 新手引导（shepherd.js） |

### 17.3 布局核心功能

Layout 组件还承担了以下全局功能：

- **时间校验**：首次进入检查系统时间准确性（`getTimeAccurate()`）
- **自动上传云空间**：`autoUploadcloud(this, this.$CSharpNotify)`
- **封禁/磁盘错误处理**：`banError()`, `diskError()`
- **CSharpNotify 任务注册**：`refreshDisk`, `notice`, `userFrozen`
- **版本切换**：纯录制版 ↔ AI 全能版（`versionChange()`）
- **关闭拦截**：录制或上传中关闭时弹出确认框（`autoCloseDialog()`）
- **资源不足提示**：AI 分析时长 / AI 算力包不足时的 30 天提示机制（`ifRemainingHint()`）
- **窗口控制**：最大化、最小化、关闭（通过客户端 API）

### 17.4 无布局模式

当路由 `meta.layoutModel === false` 时，Layout 不渲染外层壳，直接显示 `<router-view>`。用于登录页、404 页、分享页等。

---

## 18. App.vue 根组件

### 18.1 职责

`App.vue` 是 Vue 应用的根组件，承担以下全局职责：

1. **开发工具面板**：开发环境下按 `h` 键 5 次显示开发面板（URL 跳转、强制更新、F12）
2. **CSharpNotify 任务注册**：
    - `openUpdate` → 强制更新弹窗
    - `generateDiagnosis` → AI 报告生成（区分数据诊断和内容诊断）
    - `sliceSuccess` → 切片完成通知（可点击跳转到分析页）
3. **权限码管理**：watch 用户信息变化，更新 `$auth` 权限码
4. **页面刷新**：客户端首次进入时执行一次刷新
5. **拖拽拦截**：阻止浏览器默认的拖拽/拖放行为
6. **全局弹窗宿主**：客服二维码、强制更新、AI 报告、巨量百应授权

### 18.2 provide/inject

```javascript
provide() {
    return { APP: this };  // 子孙组件可通过 inject: ['APP'] 访问根组件
}
```

### 18.3 关键方法

| 方法 | 功能 |
|------|------|
| `watchForcedUpdate()` | 监听 C# 强制更新通知 |
| `aiReportFun()` | 处理 C# AI 报告生成（数据诊断 `type=1` / 内容诊断） |
| `sliceSuccessAction()` | 处理 C# 切片完成通知（显示 Notification 并可点击跳转） |
| `buyInAccount(secUid)` | 打开巨量百应账号类型选择 |
| `copyShareUrl(shareUrl)` | 复制分享链接到剪贴板 |
| `getMode()` | 获取客户端开发者模式状态 |
| `addKeyDown()` | 注册 `h` 键连按 5 次跳转首页的快捷操作 |
| `F12Click()` | 通过客户端 API 打开开发者工具 |
| `showQrCode()` | 显示客服二维码 |

---

## 19. 工具函数库

### 19.1 `utils/utils.js` —— 基础工具集

提供 `httpFormat`（请求参数格式化去除空值）、`debounce`（防抖）、`deepClone` 等基础工具方法。

### 19.2 `utils/common.js` —— 通用业务工具

```javascript
// 复制分享链接到剪贴板
copyShareUrl(shareUrl, opt)

// 从数据中提取 sourceType 和 sourceId
// sourceType: 2=对比, 1=上传文件, 0=录制视频
getSourceData(data, opt)

// 下载文本为 .txt 文件
downloadBlobTxt(txt, opt)

// 根据环境组装分享 URL
assemblyShareUrl(share)
// prod:    https://ifupan.com/client/#/...
// release: https://yz.ifupan.com/client/#/...
// test:    https://test.ifupan.com/client/#/...

// 纯录制版可用路由列表
pureRouterList = ['/dataAnalysis', '/addCompere/...', '/replay', ...]
```

### 19.3 `utils/httpLoading.js` —— 请求加载锁

管理 HTTP 请求的 Loading 状态，通过 URL 粒度的锁控制哪些请求显示/隐藏 Loading 动画。

```javascript
const httpLoading = {
    get(url),       // 查询 URL 是否需要 Loading
    edit(url, load), // 设置 URL 的 Loading 标记
    del(url),       // 删除 URL 的标记
};
```

### 19.4 `utils/platformSource.js` —— 平台来源

判断当前运行平台（桌面客户端/Web），返回对应的来源标识字符串。

### 19.5 `utils/webVersion.js` —— Web 版本

解析和返回 Web 版本号，用于请求头中的 `webVersion` 字段。

### 19.6 文件导出相关

| 文件 | 功能 |
|------|------|
| `createPdf.js` | 使用 jsPDF 生成 PDF 文档 |
| `downDocx.js` | 使用 docx 库生成 Word 文档 |
| `downFile.js` | 通用文件下载 |
| `downloadFile.js` | 另一个文件下载实现 |
| `downloadHtml.js` | HTML 内容下载 |
| `printService.js` | 打印服务 |

### 19.7 `utils/dialogDrag.js` —— 弹窗拖拽

在 `main.js` 中被直接 import（副作用引入），为 Element UI 的 Dialog 组件添加拖拽功能。

### 19.8 `utils/windLoad.js` —— 窗口加载指令

注册为 Vue 插件，提供窗口加载相关的自定义指令。

### 19.9 `utils/shepherd.js` / `utils/tour.js` —— 新手引导

基于 shepherd.js 的新手引导配置和启动逻辑。

### 19.10 `utils/aiCreateEle.js` —— AI 内容元素创建

将 AI 返回的内容（Markdown/HTML）创建为可渲染的 DOM 元素。

### 19.11 `utils/mdFormat.js` —— Markdown 格式化

使用 markdown-it 将 Markdown 文本格式化为 HTML。

### 19.12 错误处理

| 文件 | 功能 |
|------|------|
| `banError.js` | 账号封禁错误处理 |
| `diskError.js` | 磁盘空间不足错误处理 |

### 19.13 全局配置

| 文件 | 功能 |
|------|------|
| `config/common.js` | `$CONFIG` 全局 UI 配置（按钮样式、表格配置） |
| `config/h5Url/index.js` | H5 页面 URL 配置 |

---

## 20. 关键用户流程

### 20.1 应用启动流程

```
1. HTML 加载 → window.SITE_CONFIG 注入（API 地址、环境等）
2. main.js 同步执行 → 注册 Vue 插件、全局组件、挂载原型属性
3. iframeConfig.init() → 检测是否为 iframe 模式
4. initApp() → 桌面端获取机器码 → 存入 Vuex
5. new Vue() → App.vue 挂载
6. App.mounted() →
   - getMode() → 获取开发者模式状态
   - watchAuthIds() → 注入权限码
   - removeRefresh() → 客户端首次刷新
   - addKeyDown() → 注册快捷键
   - watchForcedUpdate() → 注册 C# 强制更新任务
   - aiReportFun() → 注册 C# AI 报告任务
   - sliceSuccessAction() → 注册 C# 切片通知任务
7. Layout.mounted() →
   - getBasinSetupInfo() → 获取基本设置
   - getUserproperty() → 获取用户资产
   - ifRemainingHint() → 检查资源不足提示
   - getTimeAccurate() → 检查系统时间
   - 注册 C# 任务（refreshDisk, notice, userFrozen）
   - 启动新手引导 Tour
```

### 20.2 登录流程

```
1. 用户打开应用 → 路由守卫检查 Token
   - 无 Token → 跳转 /login
   - 有 Token → 进入应用
2. 用户在登录页填写手机号 → 获取验证码 → 登录
3. api.user.login() → 获取 Token + 用户信息
4. store.commit('saveLoginResultData', res) → 持久化到 localStorage
5. 路由跳转到首页 /dataAnalysis
6. Layout 获取用户资产、基本设置、检查时间等
```

### 20.3 复盘分析流程

```
1. 用户在主播列表选择主播 → 开启录制
2. 客户端自动录制直播 → 生成视频文件
3. 用户在视频列表找到已录制视频 → 点击"生成分析"
4. api.video.createAnalysis(videoId) → 触发后端分析
5. 分析完成后 → 用户进入分析详情页
6. 分析详情页展示：
   - AI 内容分析（段落、关键词、敏感词等）
   - 数据曲线（曲线图、柱状图等）
   - 数据看板（关键指标表格）
7. 用户可导出分析结果（PDF/Word/Excel）
8. 用户可分享分析结果到云空间
```

### 20.4 对比分析流程

```
1. 用户在视频列表选择多个视频 → 创建对比
2. api.contrast.savecontrast() / clientAddContrast()
3. 进入对比分析页 → 查看两个视频的对比结果
4. AI 对比助手可针对对比数据提问
5. 支持导出对比结果
```

### 20.5 云空间分享流程（在线分析弹窗）

```
1. 用户点击"分享" → 弹出 onlineAnalysisDialog
2. 弹窗中上传视频到腾讯云点播（VOD）
3. 上传完成后选择分享设置
4. 生成分享链接 → 复制链接或二维码
5. 其他人通过链接访问 → 加载分享页（onlineAnalysis/contrastOnlineAnalysis）
```

### 20.6 切片分析流程

```
1. 用户在视频播放器中标记起始/结束时间点 → 创建切片
2. 客户端生成切片视频文件
3. C# 发送 sliceSuccess 通知 → App.vue 弹出系统通知
4. 用户点击通知 → 跳转到切片分析页
5. 切片独立进行 AI 分析
```

### 20.7 版本切换流程

```
1. 纯录制版用户看到"切换成 AI 全能版"按钮
2. 点击 → Layout.versionChange()
3. 检查录制状态 → 录制中则阻止
4. 调用 api.setup.setClientVersionConfig({ clientVersion: 'replay' })
5. 更新 Vuex versionType
6. router.go(0) 强制刷新
7. 应用以 AGENT 模式重新初始化，显示全部路由
```

---

## 21. 构建与部署

### 21.1 构建命令

```bash
# 开发启动（development 环境）
npm run serve

# Web/iframe 模式启动
npm run serve:web

# 测试环境构建
npm run build:test

# 预发布环境构建
npm run build:release

# 生产环境构建
npm run build

# Web 版构建（publicPath: '/client'）
npm run build:web

# Mock 服务
npm run mock
```

### 21.2 vue.config.js 关键配置

```javascript
module.exports = {
    // Web 版部署在 /client 路径下，桌面版用相对路径 ./
    publicPath: process.env.BUILD_TARGET === 'website' ? '/client' : './',

    chainWebpack: (config) => {
        // HTML 模板注入：ENV, BUILD_TARGET, API URLs, Baidu 统计, buildInfo
        config.plugin("html").tap((args) => {
            args[0].ENV = RUN_ENV;
            args[0].BCAK_API_URL = back[RUN_ENV];
            args[0].CLIENT_API_URL = client[RUN_ENV];
            args[0].BAIDU_HM_SRC = isProd ? '正式统计ID' : '测试统计ID';
            // ...
        });

        // 构建时添加时间戳到文件名（缓存破坏）
        if (isBuild) {
            config.output.filename(`[name].[contenthash:12].js?v=${timestamp}`);
            config.output.chunkFilename(`[name].[contenthash:12].js?v=${timestamp}`);
        }
    },

    configureWebpack: {
        optimization: {
            runtimeChunk: 'single',    // 单独提取 runtime
            splitChunks: { chunks: 'all' },  // 提取公共 chunk
        },
        plugins: [
            new CleanWebpackPlugin(),  // 构建前清理 dist
            new webpack.DefinePlugin({ // 注入 51.la 统计 ID
                'process.env.VUE_APP_LA_ID': JSON.stringify(...),
                'process.env.VUE_APP_LA_CK': JSON.stringify(...),
            }),
        ],
    },
};
```

### 21.3 构建信息注入

buildInfo 包含：当前环境、版本号、构建时间、git 用户信息、最后提交时间、最后推送时间。

```javascript
const { version, name, email, date, lastCommitTime, lastPushTime } = getGitVersion();
```

---

## 22. 开发指南

### 22.1 新增页面

1. 在 `src/views/modules/` 下创建页面组件
2. 在 `src/assets/json/menu.json` 中添加路由配置
3. 如需 API，在 `src/utils/request-api-back.js` 或 `request-api-client.js` 中添加方法
4. 如需新接口模块，在对应 API 文件中添加 `api.xxx = { ... }`

### 22.2 新增 API

```javascript
// 云端 API → src/utils/request-api-back.js
api.newModule = {
    list: (data) => post('/newModule/list', data),
    get: (data) => get('/newModule/info', data),
    save: (data) => post('/newModule/save', data),
    delete: (data) => get('/newModule/delete', data),
};

// 客户端 API → src/utils/request-api-client.js
api.newModule = {
    list: (data) => get('/newModule/list', data),
};
```

### 22.3 代码规范

- **组件**：PascalCase（`VideoJsPlayer`, `CuUpload`）
- **文件**：camelCase 或 kebab-case
- **Mixin**：按功能域划分，使用 `mixins: [...]` 数组引入
- **API 调用**：`this.$httpBack.module.method(data)` / `this.$httpClient.module.method(data)`
- **Store 访问**：`this.$store.getters.xxx` / `this.$store.commit('xxx', val)`

### 22.4 调试技巧

- **开发环境路由全开放**：`env.dev` 时路由守卫直接放行
- **查看持久化状态**：`localStorage.getItem('replayVuex')`
- **客户端端口**：可从 `navigator.userAgent` 中的 `port=XXXX` 获取
- **Mock 服务**：`npm run mock` 启动本地 Mock 服务器
- **F12 开发者工具**：开发面板中点击 F12 或通过客户端 API `openDevTools`
- **开发面板显示**：开发环境下连按 `h` 键 5 次

---

## 23. 附录：枚举与常量

### 23.1 平台枚举（`src/enum/index.js`）

```javascript
// 添加主播时的平台类型
PLATFORM_ENUM = { douyin: 0, kuaishou: 1, shipinhao: 2 }
PLATFORM_LABEL = { 0: '抖音', 1: '快手', 2: '视频号' }

// 通用平台类型（更多平台）
PLATFORM_TYPE_ENUM = { all: '0', douyin: '1', kuaishou: '2', shipinhao: '3', xiaohongshu: '4' }
PLATFORM_TYPE_LABEL = { '0': '全平台', '1': '抖音', '2': '快手', '3': '视频号', '4': '小红书' }

// 版本类型
VERSION_TYPE = { PURE: 'PURE', AGENT: 'AGENT' }

// 清晰度等级
DEFINITION_LIST = [
    { label: '标清', value: -1, color: '#6B7F06' },
    { label: '标清', value:  0, color: '#6B7F06' },
    { label: '高清', value:  1, color: '#C25705' },
    { label: '超清', value:  2, color: '#047FB0' },
    { label: '蓝光', value:  3, color: '#343ECC' },
]
```

### 23.2 全局 UI 配置（`src/config/common.js`）

```javascript
export default {
    button: {
        type: 'default',
        size: 'medium',
        sizes: ['default', 'medium', 'small'],
        round: true,
        plain: true,
    },
    coreTable: {
        isBtnsCenter: true,
        searchDelType: 'danger',
    },
    table: {},
    form: {},
    formItem: {},
};
```

### 23.3 用户资产字段（数值型）

以下字段在获取用户资产时会被自动转换为 Number 类型：

```
aiAnalysisTime, aiTokenNum, anchorNum, dataBoardNum, imgIdentifyNum,
kuaishouMonitorNum, monitorNum, rpaAmountNum, searchHotVideoNum,
searchInfluencerNum, shortVideoNum, smsMessageNum, subscribeHotVideoNum,
subscribeInfluencerNum, textExtractionNum,
totalAiAnalysisTime, totalAiTokenNum, totalAnchorNum, ...
useAiAnalysisTime, useAiTokenNum, useAnchorNum, ...
```

### 23.4 图标系统

图标系统位于 `src/icons/`，使用 SVG sprite 方式：
- `iconfont.js` → 从 `assets/fonts/font/` 加载 SVG 图标
- `icons/index.js` → 全局注册图标组件
- `icons/svg/` → 独立 SVG 文件

### 23.5 响应码

| Code | 含义 |
|------|------|
| 0 | 成功 |
| 4001 | Token 过期 / 未授权 |
| 5601 | 时间校验失败（客户端） |
| 7001 | 特殊错误，不弹提示 |
| 60002 | 白名单，不视为错误 |

---

*文档基于对项目源代码的完整阅读生成*
*生成日期：2026-05-27*
*项目版本：0.1.0*
*Git 分支：release-2.6.0*

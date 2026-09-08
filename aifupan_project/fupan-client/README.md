# 爱复盘客户端

## 项目介绍
爱复盘是一款基于Vue.js的Web应用，提供在线智能复盘分析功能。该客户端通过WebView嵌套到桌面应用中，实现跨平台的用户体验。

## 技术架构
- **前端框架**: Vue.js 2.x
- **UI组件库**: Element UI
- **状态管理**: Vuex
- **路由管理**: Vue Router
- **数据可视化**: Echarts
- **编辑器**: WangEditor
- **HTTP客户端**: Axios
- **文档处理**: docx, jsPDF
- **视频处理**: Video.js, mux.js
- **OCR识别**: Tesseract.js
- **其他库**: html2canvas, Swiper, Shepherd.js

## 系统要求
- Node.js >= 10.x
- npm >= 6.x

## 安装步骤

1. **克隆仓库**
```bash
git clone [repository-url]
cd fupan-client
```

2. **安装依赖**
```bash
npm install
# 或者
yarn install
```

3. **开发服务器**
```bash
# 开发环境
npm run serve

# 自定义环境
npm run serve:custom

# 测试环境
npm run serve:test

# 发布环境
npm run serve:release

# Web版本
npm run serve:web
```

4. **Mock服务器（用于开发）**
```bash
npm run mock
```

## 构建命令

```bash
# 生产环境构建
npm run build

# Web版本构建
npm run build:web

# 测试环境构建
npm run build:test

# 发布环境构建
npm run build:release
```

## 项目结构
```
爱复盘客户端
├─ .browserslistrc            # 浏览器兼容性配置
├─ .git                       # Git版本控制目录
├─ .gitignore                 # Git忽略文件配置
├─ babel.config.js            # Babel配置文件
├─ dist                       # 构建输出目录
├─ mockjs                     # 模拟数据服务
├─ node_modules               # npm依赖包
├─ package-lock.json          # npm包版本锁定文件
├─ package.json               # 项目配置和依赖管理
├─ public                     # 静态资源目录
│  ├─ favicon.ico             # 网站图标
│  ├─ index.html              # HTML入口文件
│  └─ static                  # 其他静态资源
├─ README.en.md               # 英文说明文档
├─ README.md                  # 项目说明文档(当前文件)
├─ src                        # 源代码目录
│  ├─ App.vue                 # 应用入口组件
│  ├─ assets                  # 资源文件目录
│  │  ├─ fonts                # 字体文件
│  │  ├─ imgs                 # 图片资源
│  │  │  ├─ avatar.png        # 头像图片
│  │  │  ├─ login             # 登录相关图片
│  │  │  └─ ...               # 其他图片资源
│  │  ├─ json                 # JSON配置文件
│  │  └─ scss                 # SCSS样式文件
│  │     ├─ bh.scss           # 主样式文件
│  │     ├─ common.css        # 通用样式
│  │     └─ common.scss       # 通用SCSS样式
│  ├─ components              # 公共组件
│  │  ├─ cu-upload            # 上传组件
│  │  ├─ editor.vue           # 富文本编辑器组件
│  │  └─ video                # 视频播放器组件
│  ├─ config                  # 配置文件目录
│  │  ├─ env.js               # 环境配置
│  │  └─ permission.js        # 权限配置
│  ├─ echarts                 # 图表配置目录
│  ├─ env                     # 环境变量目录
│  ├─ icons                   # 图标资源目录
│  │  ├─ iconfont.js          # IconFont配置
│  │  ├─ index.js             # 图标入口文件
│  │  └─ svg                  # SVG图标文件
│  │     └─ gitee.svg         # Gitee图标
│  ├─ main.js                 # 应用入口JS文件
│  ├─ mixins                  # 混入文件目录
│  │  ├─ common.js            # 通用混入
│  │  └─ mixins.js            # 其他混入
│  ├─ router                  # 路由配置目录
│  │  ├─ index.js             # 路由主配置
│  │  └─ permission.js        # 路由权限控制
│  ├─ store                   # Vuex状态管理
│  │  └─ index.js             # 状态管理入口
│  ├─ utils                   # 工具函数目录
│  │  ├─ dialogDrag.js        # 对话框拖拽功能
│  │  ├─ downloadFile.js      # 文件下载工具
│  │  ├─ notifyFromcsharp.js  # C#通知交互
│  │  ├─ request-api-back.js  # 后端API请求
│  │  ├─ request-api-client.js# 客户端API请求
│  │  ├─ request.js           # 请求工具
│  │  └─ windLoad.js          # 加载工具
│  └─ views                   # 页面视图目录
│     ├─ layout               # 布局组件
│     ├─ modules              # 功能模块
│     │  ├─ home              # 首页/数据大盘
│     │  ├─ compere           # 主播管理
│     │  ├─ addCompere        # 添加主播
│     │  ├─ replay            # 智能复盘
│     │  ├─ playBckAnalysis   # 播放分析
│     │  ├─ dataAnalysis      # 数据分析
│     │  ├─ lexicon           # 本地词库
│     │  ├─ setup             # 系统设置
│     │  ├─ aiAssistant       # AI助手
│     │  ├─ aiMixin           # AI混入组件
│     │  ├─ aiViolation       # AI违规检测
│     │  ├─ system            # 系统模块
│     │  └─ renewal           # 版本续费/升级
│     └─ pages                # 基础页面
│        ├─ 404               # 404页面
│        ├─ common            # 通用页面组件
│        ├─ login             # 登录页面
│        ├─ onlineAnalysis    # 在线分析页面
│        └─ contrastOnlineAnalysis # 对比分析页面
└─ vue.config.js              # Vue项目配置文件
```

## 核心功能

- **智能分析**: AI驱动的复盘和分析能力
- **用户身份验证**: 完整的用户认证与授权系统
- **主播数据管理**: 主播信息管理与数据分析
- **智能复盘分析**: 基于AI的智能复盘功能
- **播前内容分析**: 播放前内容预分析
- **文档导出**: 支持Word和PDF文档生成
- **视频处理**: 视频播放和分析工具
- **数据可视化**: 使用Echarts的交互式图表展示
- **AI助手**: 智能辅助功能
- **本地词库管理**: 自定义词库管理系统
- **云空间存储**: 文件云端存储功能
- **跨平台**: WebView集成桌面应用程序
- **多环境**: 支持不同的部署环境

## 开发规范

1. **代码风格**: 遵循ESLint配置
2. **组件命名**: 使用PascalCase命名组件
3. **文件组织**: 将相关文件分组到适当的目录中
4. **状态管理**: 使用Vuex管理应用程序状态
5. **API集成**: 使用Axios进行HTTP请求
6. **模块化开发**: 使用模块化方式组织代码
7. **客户端交互**: 支持C#客户端与Web应用交互

## 环境配置

应用程序支持多种环境：
- `development`: 本地开发
- `test`: 测试环境
- `release`: 预生产环境
- `production`: 生产环境
- `custom`: 自定义配置

## 贡献

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开 Pull Request

## 许可证

本项目为专有软件。保留所有权利。

## 支持

如需技术支持或有疑问，请联系开发团队。


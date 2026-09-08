# API接口文档 - AI域

> 基础路径: /replay
> 模块: replay-ai/controller

## 1. AiController - AI功能
**路径前缀**: `replay/ai/ai`

AI核心功能接口

## 2. ConversationController - AI对话
**路径前缀**: `replay/ai/conversation`

AI对话管理接口（保存对话、导出、HTML生成、AI纠正等）

## 3. CustPromptController - 自定义提示词
**路径前缀**: `replay/ai/custprompt`

用户/租户自定义提示词CRUD

## 4. DiagnosisController - 诊断
**路径前缀**: `replay/ai/diagnosis`

诊断报告管理接口

## 5. ShareLinkRecordController - 分享链接
**路径前缀**: `replay/ai/sharelinkrecord`

AI对话分享链接记录CRUD

---

# API接口文档 - 视频域

> 模块: replay-video/project/controller

## 1. VideoExtractController - 视频提取
**路径前缀**: `replay/video/extract`

短视频内容提取接口

## 2. VideoGroupManagementController - 视频分组
**路径前缀**: `replay/video/group`

视频分组管理CRUD

## 3. VideoHotSearchController - 视频热搜
**路径前缀**: `replay/video/hotsearch`

热搜数据管理接口

## 4. VideoHotSearchEmailAccountController - 热搜邮件账号
**路径前缀**: `replay/video/hotsearchemailaccount`

热搜邮件推送账号管理CRUD

## 5. VideoInfluencerController - 达人管理
**路径前缀**: `replay/video/influencer`

短视频达人信息管理接口

---

# API接口文档 - 第三方集成域

> 模块: replay-api/controller/third + replay-third/controller

## 1. AiModelController - AI模型管理
**路径前缀**: `replay/aimodel`

AI模型配置CRUD

## 2. AudioDiscernController - 语音识别
**路径前缀**: `replay/audiodiscern`

语音识别相关接口

## 3. CosThumbsFileController - COS缩略图
**路径前缀**: `replay/costhumbsfile`

COS缩略图文件管理CRUD

## 4. ProxyIpController - 代理IP
**路径前缀**: `replay/proxyip`

代理IP管理CRUD

## 5. ProxyIpRecordController - 代理IP记录
**路径前缀**: `replay/proxyiprecord`

代理IP使用记录CRUD

## 6. QiNiuOssController - 七牛云OSS
**路径前缀**: `replay/qiniuoss`

七牛云文件操作接口

## 7. TencentVodController - 腾讯VOD
**路径前缀**: `replay/tencentvod`

腾讯云视频点播接口

## 8. TableStoreController (replay-third)
**路径前缀**: `replay/third/tablestore`

阿里云TableStore操作接口

## 9. ThirdAiModelController (replay-third)
**路径前缀**: `replay/third/aimodel`

第三方AI模型管理接口

---

# API接口文档 - 开放API

## AiRelatedController
**路径前缀**: `replay/openapi`

对外开放的AI相关接口

---

## 接口通用说明

### 统一返回格式
```java
R<T> {
    int code;      // 状态码，0=成功
    String msg;    // 消息
    T data;        // 数据
}
```

### 标准CRUD接口模式
大部分Controller遵循统一的CRUD模式：
- `POST /list` - 分页列表查询（参数: XxxListBo, 返回: PageUtils<XxxListVo>）
- `GET /info` - 详情查询（参数: id, 返回: XxxInfoVo）
- `POST /save` - 新增（参数: XxxBo, 返回: String）
- `POST /update` - 修改（参数: XxxBo, 返回: String）
- `GET /delete` - 删除（参数: id, 返回: String）

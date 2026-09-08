# API接口文档 - 文案分析域

> 基础路径: /replay
> 模块: replay-api/controller/words + replay-words/controller

## replay-api 中的 words Controller

### 1. VideoDataViewingController - 视频观看数据
**路径前缀**: `replay/videodataviewing`

标准CRUD + 视频观看数据查询和分析接口

### 2. AnchorVideoController - 主播视频
**路径前缀**: `replay/anchorvideo`

标准CRUD + 主播视频管理接口

### 3. AiAnalysisController - AI分析
**路径前缀**: `replay/aianalysis`

AI分析相关接口

### 4. AiAnalysisRecordController - AI分析记录
**路径前缀**: `replay/aianalysisrecord`

标准CRUD接口

### 5. AiAnalysisSensitiveRelaController - AI分析敏感词关联
**路径前缀**: `replay/aianalysissensitiverela`

标准CRUD接口

### 6. AiCueButtonController - AI提示按钮
**路径前缀**: `replay/aicuebutton`

标准CRUD接口

### 7. AiTrainController - AI训练
**路径前缀**: `replay/aitrain`

AI训练相关接口

### 8. AnchorCallbackController - 主播回调
**路径前缀**: `replay/anchorcallback`

主播数据回调处理接口

### 9. AnchorUrlController - 主播URL
**路径前缀**: `replay/anchorurl`

标准CRUD + 主播直播间URL管理

### 10. BlessBagController - 福袋
**路径前缀**: `replay/blessbag`

福袋功能接口

### 11. ChanmamaSendRecordController - 蝉妈妈发送记录
**路径前缀**: `replay/chanmamasendrecord`

标准CRUD接口

### 12. ClientAiFavController - AI收藏
**路径前缀**: `replay/clientaifav`

标准CRUD接口

### 13. CruxTypeController - 关键词类型
**路径前缀**: `replay/cruxtype`

标准CRUD接口

### 14. CruxWordsController - 关键词
**路径前缀**: `replay/cruxwords`

标准CRUD接口

### 15. CueWordsController - 提示词
**路径前缀**: `replay/cuewords`

标准CRUD + 提示词管理接口

### 16. DataModelController - 数据模型
**路径前缀**: `replay/datamodel`

标准CRUD接口

### 17. DataScreenshotController - 数据截图
**路径前缀**: `replay/datascreenshot`

标准CRUD接口

### 18. DataScreenshotConfigController - 截图配置
**路径前缀**: `replay/datascreenshotconfig`

标准CRUD接口

### 19. DiagnosisCueController - 诊断提示词
**路径前缀**: `replay/diagnosiscue`

标准CRUD接口

### 20. DiagnosisModelController - 诊断模型
**路径前缀**: `replay/diagnosismodel`

标准CRUD接口

### 21. LexiconController - 词库
**路径前缀**: `replay/lexicon`

标准CRUD接口

### 22. LexiconWordController - 词库词条
**路径前缀**: `replay/lexiconword`

标准CRUD接口

### 23. ModelCruxController - 模型关键词
**路径前缀**: `replay/modelcrux`

标准CRUD接口

### 24. OnlineNumController - 在线人数
**路径前缀**: `replay/onlinenum`

在线人数数据接口

### 25. SensitiveWordsController - 敏感词
**路径前缀**: `replay/sensitivewords`

标准CRUD接口

### 26. SensitiveWordsClientController - 客户端敏感词
**路径前缀**: `replay/sensitivewordsclient`

标准CRUD接口

### 27. SyncContrastController - 同步对比
**路径前缀**: `replay/synccontrast`

数据同步对比接口

### 28. TotalOnlineNumController - 总在线人数
**路径前缀**: `replay/totalonlinenum`

标准CRUD接口

### 29. TradeController - 交易数据
**路径前缀**: `replay/trade`

交易数据管理接口

### 30. UploadFileController - 上传文件
**路径前缀**: `replay/uploadfile`

文件上传管理接口

### 31. UploadFileAnalysisRecordController - 上传文件分析记录
**路径前缀**: `replay/uploadfileanalysisrecord`

标准CRUD接口

### 32. UserAnalysisRollupController - 用户分析汇总
**路径前缀**: `replay/useranalysisrollup`

标准CRUD接口

### 33. UserVideoAppealController - 用户视频申诉
**路径前缀**: `replay/uservideoappeal`

标准CRUD接口

### 34. VideoAnalysisRecordController - 视频分析记录
**路径前缀**: `replay/videoanalysisrecord`

标准CRUD接口

### 35. VideoDataViewingConfuseController - 视频混淆数据
**路径前缀**: `replay/videodataviewingconfuse`

标准CRUD接口

### 36. VideoDataViewingRatioController - 视频观看比例
**路径前缀**: `replay/videodataviewingratio`

标准CRUD接口

### 37. VideoMarkController - 视频标记
**路径前缀**: `replay/videomark`

标准CRUD接口

### 38. WordRuleController - 词规则
**路径前缀**: `replay/wordrule`

标准CRUD接口

### 39. WordRuleRelevanceController - 词规则关联
**路径前缀**: `replay/wordrulerelevance`

标准CRUD接口

---

## replay-words 模块内部 Controller

### 40. AnchorVideoDetailController
**路径前缀**: `replay/words/anchorvideodetail`

主播视频详情管理

### 41. VideoDataViewingController (words)
**路径前缀**: `replay/words/videodataviewing`

视频观看数据（模块内部接口）

### 42. AnchorVideoController (words)
**路径前缀**: `replay/words/anchorvideo`

主播视频管理（模块内部接口）

### 43. AnchorUrlWordsController
**路径前缀**: `replay/words/anchorurl`

主播URL管理

### 44. AnchorCruxWordsController
**路径前缀**: `replay/words/anchorcruxwords`

主播关键词管理

### 45. AnalysisController
**路径前缀**: `replay/words/analysis`

分析功能接口

### 46. AnalysisMarkController
**路径前缀**: `replay/words/analysismark`

分析标记管理

### 47. ContrastController
**路径前缀**: `replay/words/contrast`

对比分析接口

### 48. TradeController (words)
**路径前缀**: `replay/words/trade`

交易数据（模块内部接口）

### 49. TradeRankController
**路径前缀**: `replay/words/traderank`

交易排名接口

### 50. OceanEngineDataController
**路径前缀**: `replay/words/oceanenginedata`

巨量引擎数据接口

### 51. VideoContentController
**路径前缀**: `replay/words/videocontent`

视频内容管理

### 52. VideoSliceController
**路径前缀**: `replay/words/videoslice`

视频切片管理

### 53. UploadFileDetailController
**路径前缀**: `replay/words/uploadfiledetail`

上传文件详情

### 54. WordsCueWordsController
**路径前缀**: `replay/words/cuewords`

提示词管理（模块内部接口）

### 55. BasicSettingsController
**路径前缀**: `replay/words/basicsettings`

基础设置管理

### 56. SourceStarController
**路径前缀**: `replay/words/sourcestar`

来源星级管理

### 57. AiOptimizePurposeController
**路径前缀**: `replay/words/aioptimizepurpose`

AI优化目的管理

### 58. AiWordsController
**路径前缀**: `replay/words/aiwords`

AI文案管理

import { get, post } from "./request-client";
import store from './../store';
import { normalizeAiWorkbenchUrl } from './aiAgentRoute';

const api = {};

//测试
api.test = {
  // 测试方法GET：api/config/tesTdouyinWarnStopRecord
  tesTdouyinWarnStopRecord: (data)=>get("/config/tesTdouyinWarnStopRecord", data),
  // 测试强制更新 api/config/openUpdateVersion
  tesOpenUpdateVersion: (data)=>get("/config/openUpdateVersion", data),

};

// 窗体
api.form = {
  togglemaxsize: (data) => get("/form/togglemaxsize", data), // 最大化/恢复
  minsize: (data) => get("/form/minsize", data), // 最小化
  close: (data) => get("/form/close", data), // 关闭
};

// 系统设置
api.setup = {
  getmodel: (data) => get("/config/getmodel", data).then(res=>{
    if(res?.data?.SerialNumber){
      localStorage.setItem('aifupan_version', res?.data?.SerialNumber);
    }
    return res;
  }), // 获取基本设置信息
  updatemodel: (data) => post("/config/updatemodel", data), // 修改基本设置
  checkfilebox: (data) => get("/config/checkfilebox", data), // 打开文件选择器
  closedialog: (data) => get("/config/closedialog", data), // 关闭文件选择器
  settoken: (data) => get("/config/settoken", data), // 更新token
  getdisksize: (data) => get("/config/getdisksize", data), // 读盘空间
  getmaxdisk: (data) => get("/config/getmaxdisk", data), // 返回剩余空间容量最大的盘符
  getFile: (data) => get("/config/getFile", data), // 读取文件
  getVersionUpdate: (data) => get("/config/getVersionUpdate", data), // 获取版本更新信息
  getCreateTime: (data) => get("/config/getCreateTime", data), // 启动版本更新检查
  updateProgram: (data) => get("/config/updateProgram", data), // 更新的方法
  setClientVersion: (data) => get("/config/setClientVersion", data),
  openDevelopmentMode: (data) => get("/config/openDevelopmentMode", data),
  getClientMode: (data) => get("/config/getClientMode", data),
  openDevTools: (data) => get("/config/openDevTools", data),
  closeDevMode: (data) => get("/config/closeDevMode", data),
  getTimeAccurate: (data) => get("/config/getTimeAccurate", data), // 获取当前系统时间是否准确
  updateVersion: (data)=>get("/config/updateVersion", data),
  getUserObject: (data) => get("/config/getUserObject", data, {load:false}),
  putUserObject: (data) => post("/config/putUserObject", data, {load:false}),
  handUpdateVersion: (data) => get("/config/handUpdateVersion", data, {load:false}),
  getMachineCode: (data) => get('/system/getMachineCode', data, {load: false}),
  getVersionType: (data) => get('/config/getClientVersionConfig', data, {load: false}),
  setClientVersionConfig: (data) => post("/config/setClientVersionConfig", data),
}


// 主播
api.compere = {
  saveanchorinfo: (data) => post("/anchorinfo/saveanchorinfo", data), // 添加主播
  addOrUpdateAnchor: (data) => post("/anchorinfo/addOrUpdateAnchor", data), // 添加或修改主播
  getpageanchor: (data) => post("/anchorinfo/getpageanchor", data), // 主播列表
  openfolder: (data) => get("/anchorinfo/openfolder", data), // 打开目录
  openorcloseautorecord: (data) => get("/anchorinfo/openorcloseautorecord", data), // 开启、关闭自动录制
  stoprecord: (data) => get("/anchorinfo/stoprecord", data), // 停止单个主播录制
  startRecord: (data) => get("/anchorinfo/StartRecord", data), // 开启、关闭自动录制
  decector: (data,...arg) => get("/anchorinfo/decector", data,...arg), // 开始检测
  stopdecector: (data,...arg) => get("/anchorinfo/stopdecector", data,...arg), // 停止检测
  previewvideo: (data) => get("/anchorinfo/previewvideo", data), // 预览视频
  removeanchor: (data) => get("/anchorinfo/removeanchor", data), // 删除主播
  updateanchortrade: (data) => get("/anchorinfo/updateanchortrade", data), // 修改主播行业
  updateBarrageMonitoring: (data) => get("/anchorinfo/updateBarrageMonitoring", data), // 修改弹幕监控位状态
  openOrCloseAutoUploadCloud:(data)=>get("/anchorinfo/openOrCloseAutoUploadCloud",data), // 开启、关闭自动上传云空间
  updatePureRecordOnlineNum:(data)=>get("/anchorinfo/updatePureRecordOnlineNum",data), // 开启、关闭自动上传云空间
  topAnchor:(data)=>get("/anchorinfo/topAnchor",data), // 置顶主播
  openOrCloseDataViewing:(data)=>get("/anchorinfo/openOrCloseDataViewing",data), // 开启/关闭主播的数据看板
  getAiRecommendTrade: (data) => post("/aiRelated/getAiRecommendTrade", data), // 获取ai推荐的行业
  existAnchor:(data)=>get("/anchorinfo/existAnchor",data),//判断是否存在主播

  updateAiPartial: (data) => post("/anchorinfo/updateAiPartial", data), // 更新ai页面的部分主播字段
  reAddAnchor: (data) => get('/anchorinfo/reAddAnchor', data),//从恢复列表添加主播
  fullRemoveAnchor: (data) => get('/anchorinfo/fullRemoveAnchor', data),//从恢复列表删除主播
  authorizeWeChatChannels: (data) => get("/anchorinfo/authorizeWeChatChannels", data), // 授权微信渠道
}

// 视频
api.video = {
  getpage: (data) => get("/anchorvideo/getpage", data), // 视频列表
  createAnalysis: (data) => get("/anchorvideo/createanalysis", data), // 生成视频分析
  lockanalysis: (data) => get("/anchorvideo/lockanalysis", data), // 查看视频分析
  preview: (data) => get("/anchorvideo/preview", data), // 预览
  openFolder: (data) => get("/anchorvideo/openfolder", data), // 打开视频目录
  reAnalysisByTrade: (data) => get("/anchorvideo/reanalysisbytrade", data), // 重新选择行业分析
  reanalysis: (data) => get("/anchorvideo/reanalysis", data), // 重新分析
  deletebyids: (data) => post("/anchorvideo/deletebyids", data), // 根据视频id集合删除视频
  deleteLocalVideoByIds: (data) => post("/anchorvideo/deleteLocalVideoByIds", data), // 根据视频id集合删除视频（只删除视频保留其他）
  deleteVideoByIds: (data) => post("/anchorvideo/deletebyids", data), // 只删除视频文件保留其他
  stopAutoAnalysis: (data) => post("/anchorvideo/stopAutoAnalysis", data), // 停止自动分析
  compress: (data) => get("/anchorvideo/compress", data), // 压缩视频
  shareAnalysis: (data) => get("/anchorvideo/shareanalysis", data), // 分享复盘
  confirmUseMark: (data) => get("/anchorvideo/confirmusemark", data), // 确认消耗标注资源
  getSharePage: (data) => get("/anchorvideo/getsharepage", data), // 已分享的视频列表
  deleteShare: (data) => get("/anchorvideo/deleteShare", data), // 删除分享复盘
  lockCloudAnalysis:(data)=>get("/anchorvideo/lockCloudAnalysis",data), // 查看云空间视频分析详情
  lockCloudContrast:(data)=>get("/contrast/lockCloudContrast",data), // 查看云空间视频分析详情

  reNameVideo: (data) => post('/anchorvideo/renameVideo', data),
  renameFile: (data) => post('/uploadfile/renameFile', data),
  updateDataDiagnosisConfig: (data) => post('/anchorvideo/updateDataDiagnosisConfig', data),//更新数据诊断配置
}
// 对比
api.contrast = {
  savecontrast: (data) => post("/contrast/savecontrast", data), // 创建视频分析对比
  getcontrastpage: (data) => get("/contrast/getcontrastpage", data), // 分页获取对比列表
  lockanalysiscontrast: (data) => get("/contrast/lockanalysiscontrast", data), // 查看视频智能对比分析
  shareanalysis: (data) => get("/contrast/shareanalysis", data), // 分享对比数据
  GetShareContrastPage: (data) => get("/contrast/getsharecontrastpage", data), // 分页获取已分享的对比列表
  deleteShare: (data) => get("/contrast/deleteShare", data), // 删除分享复盘
}


// 文件上传
api.uploadFile = {
  checkfile: (data) => get("/uploadfile/checkfile", data), // 选择文件
  getpage: (data) => get("/uploadfile/getpage", data), // 文件列表
  lockanalysis: (data) => get("/uploadfile/lockanalysis", data), // 查看视频分析
  preview: (data) => get("/uploadfile/preview", data), // 预览
  reAnalysisByTrade: (data) => get("/uploadfile/reanalysisbytrade", data), // 重新选择行业分析
  reanalysis: (data) => get("/uploadfile/reanalysis", data), // 重新分析
  deletebyids: (data) => post("/uploadfile/deletebyids", data), // 根据文件id集合删除文件
  createAnalysis: (data) => get("/uploadfile/createanalysis", data), // 重新分析
  shareAnalysis: (data) => get("/uploadfile/shareanalysis", data), // 分享复盘
  confirmUseMark: (data) => get("/uploadfile/confirmusemark", data), // 确认消耗标注资源
  compress: (data) => get("/uploadfile/compress", data), // 压缩视频
  frontUpload: (data) => post("/upload/frontUpload", data, {load: false}), // AI诊断导出pdf上传
  checkUploadFile: (data) => get("/uploadfile/checkUploadFile", data), // 选择上传的文件
  commitUploadFile: (data) => post("/uploadfile/commitUploadFile", data), // 确认上传文件
  multiImgToGeneratePDF: (...data) => post("/upload/multiImgToGeneratePDF", ...data),

  uploadTxtFileByWord: (data) => post("/uploadfile/uploadTxtFileByWord", data), // 上传文字
}




// 导出
api.export = {
  alysestxt: (data) => get("/export/alysestxt", data), // 导出视频/文件文字内容
  wordsexcel: (data) => get("/export/wordsexcel", data), // 导出敏感词/关键词列表excel
  alysesCloudTxt: (data) => get("/export/alysesCloudTxt", data),// 云空间导出文字
  generateReport: (data) => post('/diagnosis/generateReport', data),
}

api.syncData = {
  syncData: (data) => get("/sync/syncdata", data), // 同步数据
}

// 用户资产 
api.userProperty = {
  info: (data,...arg) => get("/userproperty/info", data,...arg).then(res=>{
    if (res.code == 0 && res.data) {
      store.commit("saveUserproperty", res.data);
    }
    return res;
  }), // 获取用户资产
}

// 用户信息 
api.user = {
  info: (data) => get("/config/getuserinfo", data), // 获取用户信息
}


// 文件视频接口
api.anchorvideo = {
  anchorvideoGetpage: (data)=>get("/uploadfile/checkTxtFileAnalysisProperty" ,data),
  onlineChartData: (data)=>get("/anchorvideo/onlineChartData" ,data, { load: false }),
  repairVideo: (data)=>get("/anchorvideo/repairVideo" ,data, { load: false }),
  cancelRepairVideo: (data)=>get("/anchorvideo/cancelRepairVideo" ,data, { load: false }),
  exportVideoContent:(data)=>post('/anchorvideo/exportVideoContent', data, {load: false}),
  // 生成视频自然、优化原文内容
  generateVideoContent: (data)=>post('/anchorvideo/generateVideoContent', data, {load: false}),
  // 取消视频分析
  cancelVideoAnalysis: (data)=>get('/anchorvideo/cancelVideoAnalysis', data, {load: false}),
  accelerate: (data)=>get('/anchorvideo/accelerate', data, {load: false}),
  addVideoSlice:(data)=>post('/anchorvideo/addVideoSlice', data, {load: false}),
  addFileSlice:(data)=>post('/uploadfile/addFileSlice', data, {load: false}),
  queryDanMuExport:(data)=>post('/anchorvideo/queryDanMuExport', data),
  pullProduct: (data)=>post('/anchorvideo/pullProduct', data, {load: false}),
}

api.aiRelated = {
  // 对话
  sendAsk: (data,...arg)=>post("/aiRelated/ask" ,data,...arg),
  // 历史记录
  getHistoryParagraphList: (data)=>post("/aiRelated/historyParagraphList" ,data, {load:false}),
  addHistoryParagraph: (data)=>post("/aiRelated/addHistoryParagraph" ,data, {load:false}),
  deleteHistoryParagraph: (data)=>post("/aiRelated/deleteHistoryParagraph" ,data, {load:false}),
  // 聊天历史数据结构
  getStructurePage: (data)=>post("/aiRelated/structurePage" ,data, {load:false}),
  addStructure: (data)=>post("/aiRelated/addStructure" ,data, {load:false}),
  //点赞
  likes: (data)=>post("/aiRelated/likes" ,data, {load:false}),
  // 获取使用ai的模型
  getAiModel: (data)=>post("/aiRelated/getAiModel" ,data, {load:false}),
  saveDiagnosis: (data) => post("/diagnosis/saveDiagnosis", data), // 保存诊断信息
  // 新获取历史聊天记录
  conversationPage:(data)=>post(("/aiRelated/conversationPage") ,data, {load:false}),
  // 获取错误记录
  exportAiConfig:(data)=>get("/aiRelated/exportAiConfig" ,data, {load:false}),
  generateHtml:(data)=>get("/aiRelated/generateHtml" ,data, {load:false}),
  getHtmlStatus:(data)=>post("/aiRelated/getHtmlStatus" ,data, {load:false}),
  generateCorrectAiContent:(data)=>get("/aiRelated/generateCorrectAiContent" ,data, {load:false}),
}


api.system ={
  openOfficial: (data)=>get("/system/openOfficial" ,data, {load:false}),
  openUrl: (data)=>post("/system/openUrl" ,data, {load:false}),
  htmlPrintPDF: (data) => post('/system/htmlPrintPDF', data, {load: false}),
  // api/system/openGovernanceWeb?key=governance_record_page
  openGovernanceWeb: () => get('/system/openGovernanceWeb', {key: 'governance_record_page'}, {load: false}),
  /**
   * @description 打开 AI 工作台，并按 `/sso#...` 规则附带板块或直播间定位参数。
   * 约定：只有 `/sso` 入口才校验 token；`panel=influencer` 表示短视频板块，`panel=room` 表示直播间板块。
   * 直播间场景仍使用 `secUid/videoId/liveDate/cue`；达人场景统一使用 `influencerId/clipId`。
   * @param {{ panel?: string, includeToken?: boolean, secUid?: string, videoId?: string, influencerId?: string, clipId?: string, liveDate?: string, cue?: string|number, openInBrowserWindow?: boolean }} options 工作台跳转参数
   * @returns {Promise<void|*>}
   */
  openAIAgentWeb: async (options = {}) => {
    const {
      panel = '',
      includeToken = true,
      secUid: rawSecUid = '',
      SecUid = '',
      videoId: rawVideoId = '',
      VideoId = '',
      influencerId: rawInfluencerId = '',
      InfluencerId = '',
      clipId: rawClipId = '',
      ClipId = '',
      liveDate = '',
      cue = '',
      openInBrowserWindow = false
    } = options
    const secUid = String(rawSecUid || SecUid || '').trim()
    const videoId = String(rawVideoId || VideoId || '').trim()
    const influencerId = String(rawInfluencerId || InfluencerId || '').trim()
    const clipId = String(rawClipId || ClipId || '').trim()
    const httpBack = require('@/utils/request-api-back').default;
    const res = await httpBack.common.getByKey('ai_workbench_page')
    const urlTemplate = res?.data?.kvValue || ''
    const token = includeToken ? (store.getters.getToken || '') : ''
    const url = normalizeAiWorkbenchUrl(urlTemplate, {
      token,
      includeToken,
      panel,
      secUid,
      videoId,
      influencerId,
      clipId,
      liveDate,
      cue
    })
    console.log(url);
    if (!url) return Promise.resolve()
    if (openInBrowserWindow && typeof window !== 'undefined') {
      window.open(url, '_blank')
      return Promise.resolve()
    }
    return api.system.openUrl({url})
  },
}

api.buyIn = {
  authorizedBuyIn: (data) => get('/anchorinfo/authorizeJuliang', data),
  cancelAuthorizeJuliang: (data) => get('/anchorinfo/cancelAuthorizeJuliang', data),
  getAnchorInfo: (data) => get('/anchorinfo/getAnchorInfo', data),//获取单个直播信息
  pullJuliang: (data) => get('/anchorinfo/pullJuliang', data),
  openAnchorJuliang: (data) => get('/anchorinfo/openAnchorJuliang', data),
  pullVideoData: (data) => post('/anchorinfo/pullVideoData', data),
}

api.life = {
  authorizeLife: (data) => get('/anchorinfo/authorizeLife', data),
  cancelAuthorizeLife: (data) => get('/anchorinfo/cancelAuthorizeLife', data),
}

// 千川授权/取消接口
api.qianchuan = {
  // /api/anchorinfo/authorizeQianchuan
  authorizeQianchuan: (data) => get('/anchorinfo/authorizeQianchuan', data),
  // /api/anchorinfo/cancelAuthorizeQianchuan
  cancelAuthorizeQianchuan: (data) => get('/anchorinfo/cancelAuthorizeQianchuan', data),
}

api.douyin = {
  authorizeDouyin: (data) => get('/anchorinfo/authorizeDouyin', data),
}


// 短视频
api.shortVideo = {
    selectLocalVideo: (data) => get('/shortVideo/selectLocalVideo', data),//选择上传的本地视频
    immediatelyLocalVideo: (data) => post("/shortVideo/immediatelyLocalVideo", data, {load: false}),//立即提取文案
    batchCreateExtract: (data) => post("/shortVideo/batchCreateExtract", data),//批量创建提取文案任务
    reExtract: (data) => post("/shortVideo/reExtract", data),//重新提取文案
    captureInfluencerInfo: (data) => post("/shortVideo/captureInfluencerInfo", data),//搜达人
    addInfluencerInfo: (data) => post("/shortVideo/addInfluencerInfo", data),//添加订阅达人
    captureHotSearch: (data) => post("/shortVideo/captureHotSearch", data),//搜爆款
    addHotSearch: (data) => post("/shortVideo/addHotSearch", data),//添加订阅爆款
    syncInfluencerVideo: (data) => post("/shortVideo/syncInfluencerVideo", data),//同步达人的视频信息
    updateHotSearchData: (data) => post("/shortVideo/updateHotSearchData", data),//手动更新爆款
}


export default api;

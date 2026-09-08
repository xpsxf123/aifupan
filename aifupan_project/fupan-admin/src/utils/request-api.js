import { get, post, put, del } from './request'

const api = {}

// 用户
api.user = {
    login: (data) => post('/user/login', data),
    updatePassword: (data) => post('/user/updatePassword', data),
    resetPassword: (data) => get('/user/resetPassword', data),
    list: (data) => post('/user/list', data),
    listManage: (data) => post('/user/listManage', data),
    info: (data) => get('/user/info', data, {showLoading: true}),
    save: (data) => post('/user/save', data),
    update: (data) => post('/user/update', data),
    delete: (data) => get('/user/delete', data),
    logout: (data) => get('/user/logout', data),
    updateUser: (data) => post('/user/updateUser', data),
    userPropertyList: (data) => post('user/userPropertyList', data),
    pageList: (data, options) => post('/user/pageList', data, options),
    deleteByIds: (data) => post('/user/deleteByIds', data),
    updateByUserId: (data) => post('/user/updateByUserId', data),
    selectByuseId: (data) => post('/user/selectByuseId', data),
    updateTrade: (data) => get('/user/updateTrade', data),
    userDetailByUserId: (data) =>
        get('/user/userDetailByUserId', data, {showLoading: true}),
    subAccountList: (data) => post('/user/subAccountList', data),
    exportUserInfoList: (data) => post('/user/exportUserInfoList', data),
    agentSalesList: (data) => post('/agent/list', data),
    getPlatformOperators: (data) => get('/user/platformOperationList', data), // 获取平台运营人员
    addAgent: (data) => post('/agent/save', data), // 新增代理商
    editAgent: (data) => post('/agent/update', data), // 编辑代理商
    getAgentDetail: (data) => get('/agent/info', data, {showLoading: true}), // 获取代理商列表
    platformSaleList: (data) => get('/agentplatformsale/listByAgentId', data), // 查询平台列表
    delFormSaleList: (data) => get('/agentplatformsale/delete', data), // 删除平台销售
    addFormSaleList: (data) => post('/agentplatformsale/save', data), // 新增平台销售
    getFormSaleList: (data) => get('/agentplatformsale/info', data), // 查询平台销售
    updateFormSaleList: (data) => post('/agentplatformsale/update', data), // 编辑平台销售
    promotionChannel: (data) => get('/agentpromotion/listByAgentId', data), // 查询推广渠道列表
    addChannel: (data) => post('/agentpromotion/save', data), // 新增推广渠道
    updateChannel: (data) => post('/agentpromotion/update', data), // 编辑推广渠道
    getChannelDetail: (data) => get('/agentpromotion/info', data), // 代理商推广渠道信息
    updateChannelStatus: (data) => get('/agentpromotion/info', data), // 启用/停用推广渠道
    saleList: (data) => get('/agentsale/listByAgentId', data), // 查询销售列表
    addSale: (data) => post('/agentsale/save', data), // 新增销售
    updateSale: (data) => post('/agentsale/update', data), // 编辑销售
    showSaleDetail: (data) => get('/agentsale/info', data), // 查看销售详情
    getCommission: (data) => get('/agentcommission/commissionRecords', data), // 获取佣金结算记录列表
    sendCustomerAcquisitionMsg: (data) => get('/power/user/sendCustomerAcquisitionMsg', data) // 发送客户获取短信
}

api.userloginlog = {
    list: (data) => post('/userloginlog/list', data)
}

api.userdetails = {
    saveUserDetails: (data) => post('/userdetails/saveUserDetails', data),
    getUserOperations: (data) => post('/common/operationlog/list', data)
}
//用户信息
api.anchorurl = {
    list: (data) => post('/anchorurl/seletByUserId', data),
    AnchorVideo: (data) => post('/AnchorVideo/pageLists', data),
    FileVideo: (data) => post('/UploadFile/queryPagelist', data),
    VideoAnalysis: (data) => post('/AnchorVideo/selectByVideoId', data),
    selectAnchorVideoRecod: (data) =>
        post('/AnchorVideo/selectAnchorVideoRecod', data),
    seletAnchorUrl: (data) => post('/anchorurl/seletAnchorUrl', data),
    seletUidAnchorUrlWhite: (data) =>
        post('/anchorurl/seletUidAnchorUrlWhite', data),

    saveAnchorUrlWhite: (data) => post('/anchorurl/saveAnchorUrlWhite', data),
    removeAnchorUrlWhite: (data) => post('/anchorurl/removeAnchorUrlWhite', data),

    infoBySecUid: (data) =>
        get('/anchorurl/infoBySecUidOne', data, {showLoading: true}),
    selectUserByAnchorWhite: (data) =>
        get('/anchorurl/selectUserByAnchorWhite', data),
    selectAnchorByUserId: (data) => post('/anchorurl/selectAnchorByUserId', data),
    userAddAnchorRecord: (data) => post('/anchorurl/userAddAnchorRecord', data),
    backRePullData: (data) => get('/words/videoDataViewing/backRePullData', data)

    // removeAnchorWhite: (data) => post("/anchorurl/removeAnchorWhite", data),
    // saveAnchorInUserWhite: (data) => post("/anchorurl/saveAnchorInUserWhite", data),
    // whiteCount: (data) => get("/anchorurl/whiteCount", data),

    // Userproperty:(data)=>post("/userproperty/",data)
}
// 菜单
api.menu = {
    listTreeSelf: (data, config = {}) => get('/menu/listTreeSelf', data, config),
    listTreeByRoleId: (data) => get('/menu/listTreeByRoleId', data),
    list: (data) => get('/menu/list', data),
    info: (data) => get('/menu/info', data, {showLoading: true}),
    save: (data) => post('/menu/save', data),
    update: (data) => post('/menu/update', data),
    delete: (data) => get('/menu/delete', data)
}

//录制记录
api.AnchorVideo = {
    listByUserId: (data) => post('/AnchorVideo/listByUserId', data),
    selectVideoRecod: (data) => post('/AnchorVideo/selectVideoRecod', data),
    selectByVideoId: (data) => post('/AnchorVideo/selectByVideoId', data),
    removeByVoidId: (data) => post('/AnchorVideo/removeByVoidId', data),

    selectVideoBySecUid: (data) => post('/AnchorVideo/selectVideoBySecUid', data),
    videoAnalysisByVideoId: (data) =>
        get('/AnchorVideo/videoAnalysisByVideoId', data, {showLoading: true}),
    videoAnalysisByUserId: (data) =>
        post('/AnchorVideo/videoAnalysisByUserId', data),
    selectAnalysisByVideoId: (data) =>
        get('/AnchorVideo/selectAnalysisByVideoId', data),
    selectAnalysisByFileId: (data) =>
        get('/AnchorVideo/selectAnalysisByFileId', data),
    getAnalysisInfo: (data) => get('/AnchorVideo/getAnalysisInfo', data)
}

//用户资产
api.userproperty = {
    seletByUserIdInfo: (data) => get('/userproperty/seletByUserIdInfo', data),
    getPropertyByUserId: (data) => get('/userproperty/getPropertyByUserId', data),
    getPropertyByPropertyId: (data) =>
        get('/userproperty/getPropertyByPropertyId', data),
    list: (data) => post('/userproperty/list', data),
    pagePropertyDetails: (data) =>
        post('/userproperty/pagePropertyDetails', data),
    statisticsUserProperty: (data) =>
        post('/userproperty/statisticsUserProperty', data),
    aiTokenUseRecordByDetailId: (data) =>
        get('/userproperty/aiTokenUseRecordByDetailId', data)
}

// 角色
api.role = {
    power: (data) => get('/role/power', data),
    list: (data) => post('/role/list', data),
    info: (data) => get('/role/info', data),
    save: (data) => post('/role/save', data),
    update: (data) => post('/role/update', data),
    delete: (data) => get('/role/delete', data)
}

// 行业
api.trade = {
    list: (data) => post('/trade/list', data),
    listTree: (data) => get('/trade/listTree', data),
    info: (data) => get('/trade/info', data, {showLoading: true}),
    save: (data) => post('/trade/save', data),
    update: (data) => post('/trade/update', data),
    delete: (data) => get('/trade/delete', data),
    deleteTradeModel: (data) => get('/trade/deleteTradeModel', data)
}

// 关键词
api.cruxwords = {
    list: (data) => post('/cruxwords/list', data),
    info: (data) => get('/cruxwords/info', data),
    save: (data) => post('/cruxwords/save', data),
    saveBatch: (data) => post('/cruxwords/saveBatch', data),
    update: (data) => post('/cruxwords/update', data),
    delete: (data) => get('/cruxwords/delete', data)
}

// 敏感词
api.sensitivewords = {
    list: (data) => post('/sensitivewords/list', data),
    info: (data) => get('/sensitivewords/info', data, {showLoading: true}),
    save: (data) => post('/sensitivewords/save', data),
    saveBatch: (data) => post('/sensitivewords/saveBatch', data),
    update: (data) => post('/sensitivewords/update', data),
    delete: (data) => get('/sensitivewords/delete', data)
}

// AI提示词按钮
api.aiCueButton = {
    list: (data) => post('/aicuebutton/list', data),
    info: (data) => get('/aicuebutton/info', data, {showLoading: true}),
    save: (data) => post('/aicuebutton/save', data),
    saveBatch: (data) => post('/aicuebutton/saveBatch', data),
    update: (data) => post('/aicuebutton/update', data),
    delete: (data) => get('/aicuebutton/delete', data)
}

// 提示词
api.cuewords = {
    list: (data) => post('/cuewords/list', data),
    info: (data) => get('/cuewords/info', data, {showLoading: true}),
    save: (data) => post('/cuewords/save', data),
    saveBatch: (data) => post('/cuewords/saveBatch', data),
    update: (data) => post('/cuewords/update', data),
    delete: (data) => get('/cuewords/delete', data)
}

// 用户自定义词语
api.sensitivewordsClient = {
    list: (data) => post('/sensitivewordsClient/list', data),
    info: (data) => get('/sensitivewordsClient/info', data),
    save: (data) => post('/sensitivewordsClient/save', data),
    update: (data) => post('/sensitivewordsClient/update', data),
    delete: (data) => get('/sensitivewordsClient/delete', data)
}

// 字典类型
api.dicttype = {
    list: (data) => post('/dicttype/list', data),
    info: (data) => get('/dicttype/info', data, {showLoading: true}),
    save: (data) => post('/dicttype/save', data),
    update: (data) => post('/dicttype/update', data),
    delete: (data) => get('/dicttype/delete', data)
}

// 字典
api.dictdata = {
    list: (data) => post('/dictdata/list', data),
    info: (data) => get('/dictdata/info', data, {showLoading: true}),
    save: (data) => post('/dictdata/save', data),
    update: (data) => post('/dictdata/update', data),
    delete: (data) => get('/dictdata/delete', data)
}

// 文章
api.article = {
    list: (data) => post('/article/list', data),
    info: (data, options) => get('/article/info', data, options),
    save: (data) => post('/article/save', data),
    update: (data) => post('/article/update', data),
    delete: (data) => get('/article/delete', data)
}

// 会员等级
api.viplevel = {
    list: (data) => post('/viplevel/list', data),
    info: (data) => get('/viplevel/info', data),
    save: (data) => post('/viplevel/save', data),
    update: (data) => post('/viplevel/update', data),
    delete: (data) => get('/viplevel/delete', data)
}

api.package = {
    list: (data, options) => post('/package/list', data, options),
    info: (data, options) => get('/package/info', data, options),
    saveOrUpdate: (data) => post('/package/saveOrUpdate', data),
    delete: (data) => get('/package/delete', data),
    synchronousPackage: (data) => get('/package/synchronousPackage', data),
    canPurchasePackage: (data) => get('/package/canPurchasePackage', data),
    userRenewal: (data) => get('/package/userRenewal', data),
    incrementByPackageId: (data) =>
        get('/package/incrementByPackageId', data, {showLoading: true}),
    incrementTypeList: (data) => post('/commoditytype/list', data)
}

// 会员等级
api.commodity = {
    list: (data) => post('/commodity/list', data),
    listAll: (data) => post('/commodity/listTypeAll', data),
    info: (data, options) => get('/commodity/info', data, options),
    save: (data) => post('/commodity/save', data),
    update: (data) => post('/commodity/update', data),
    delete: (data) => get('/commodity/delete', data),
    saveOrUpdate: (data) => post('/commodity/saveOrUpdate', data),
    queryPageCommodity: (data) => post('/commodity/queryPageCommodity', data),
    // saveCreate:(data)=>post("/order",data),
    packageSaveOrUpdate: (data) => post('/commodity/packageSaveOrUpdate', data),
    isDeletePriceId: (data) => get('/commodity/isDeletePriceId', data)
}
// 订单
api.order = {
    seletOrderdetail: (data) => post('/orderdetail/seletByOrderId', data),
    saveCreate: (data) => post('/order/saveCreate', data),
    selectUserPropertyDetails: (data) =>
        post('/userproperty/selectUserPropertyDetails', data),
    selectByUserId: (data) => post('/order/selectByUserId', data),
    listOrder: (data) => post('/order/listOrder', data),
    list: (data) => post('/order/list', data),
    info: (data) => get('/order/info', data, {showLoading: true}),
    getOrderByUserId: (data) => get('/order/getOrderByUserId', data),
    userVersionOrder: (data) =>
        get('/order/userVersionOrder', data, {showLoading: true}),
    newPcCreateOrder: (data) => get('/order/newPcCreateOrder', data),
    pcUpgradeOrder: (data) => post('/order/pcUpgradeOrder', data),
    orderEdit: (data) => post('/order/orderEdit', data),
    pcRenewalOrder: (data) => post('/order/pcRenewalOrder', data),
    pcIncrementsOrder: (data) => post('/order/pcIncrementsOrder', data),
    orderStop: (data) => get('/order/orderStop', data),
    getCreateUserList: (data) => post('/user/listAdmin', data) // 获取订单创建人
}

// 商品类型
api.commoditytype = {
    list: (data, options) => post('/commoditytype/list', data, options),
    info: (data, options) => get('/commoditytype/info', data, options),
    save: (data) => post('/commoditytype/save', data),
    update: (data) => post('/commoditytype/update', data),
    delete: (data) => get('/commoditytype/delete', data),
    synchronousUserAssets: (data) =>
        get('/commoditytype/synchronousUserAssets', data)
}

// 邀请码批次
api.invitationcodebatch = {
    list: (data) => post('/invitationcodebatch/list', data),
    info: (data, options) => get('/invitationcodebatch/info', data, options),
    save: (data) => post('/invitationcodebatch/save', data),
    update: (data) => post('/invitationcodebatch/update', data),
    delete: (data) => get('/invitationcodebatch/delete', data),

    getTypeConsumptionById: (data) =>
        get('/invitationcodebatch/getTypeConsumptionById', data),
    invitationByBatchId: (data) =>
        get('/invitationcodebatch/invitationByBatchId', data),
    checkOnlyActivationCode: (data) =>
        get('/invitationcodebatch/checkOnlyActivationCode', data)
}

// 邀请码
api.invitationcode = {
    list: (data) => post('/invitationcode/list', data),
    info: (data) => get('/invitationcode/info', data),
    save: (data) => post('/invitationcode/save', data),
    update: (data) => post('/invitationcode/update', data),
    delete: (data) => get('/invitationcode/delete', data),

    exportInvitation: (data) => post('/invitationcode/exportInvitation', data),
    updateIsLssued: (data) => post('/invitationcode/updateIsLssued', data)
}

// 用户标签
api.tag = {
    list: (data) => post('/tag/list', data),
    info: (data) => get('/tag/info', data),
    save: (data) => post('/tag/save', data),
    update: (data) => post('/tag/update', data),
    delete: (data) => get('/tag/delete', data)
}

//客户端日志
api.clientLog = {
    list: (data, pageIndex, pageSize) =>
        post('/clientlog/pagelist/' + pageIndex + '/' + pageSize, data),
    save: (data) => post('/clientlog/save', data),
    deleteByIds: (data) => post('/clientlog/deleteByIds', data),
    delete: (data) => post('/clientlog/delete', data)
}

//客户端版本更新
api.clientupdate = {
    list: (data) => get('/clientupdate/pagelist', data),
    info: (data, options) => get('/clientupdate/info', data, options),
    save: (data) => post('/clientupdate/save', data),
    update: (data) => post('/clientupdate/update', data),
    delete: (data) => post('/clientupdate/delete', data),
    uploadFile: (data) => post('/clientupdate/uploadFile', data),
    updateStatus: (data) => get('/clientupdate/updateStatus', data)
}
// 客户端轮播图管理
api.clientCarouselManager = {
    list: (data) => post('/loginrotateimage/list', data), // 客户端登录页轮播图列表
    save: (data) => post('/loginrotateimage/save', data), // 新增客户端登录页轮播图
    update: (data) => post('/loginrotateimage/update', data), // 修改客户端登录页轮播图
    info: (data, options) => get('/loginrotateimage/info', data, options), // 查看客户端登录页轮播图信息
    del: (params) => get('/loginrotateimage/delete', params) // 删除客户端登录页轮播图
}

// 邀请活动
api.invitation = {
    list: (data) => post('/activity/clientinviteactivity/list', data), // 邀请活动列表
    save: (data) => post('/activity/clientinviteactivity/save', data), // 新增邀请活动
    info: (data, options) =>
        get('/activity/clientinviteactivity/info', data, options), // 查看邀请活动信息
    update: (data) => post('/activity/clientinviteactivity/update', data), // 查看邀请活动信息
    codeList: (data) => post('/dictdata/list', data), //code列表信息
    listByBack: (data) =>
        post('/reward/clientinviterewardrecord/listByBack', data)
}

// 关键词类型
api.cruxtype = {
    list: (data) => post('/cruxtype/list', data, {showLoading: true}),
    listTree: (data) => get('/cruxtype/listTree', data),
    info: (data) => get('/cruxtype/info', data, {showLoading: true}),
    save: (data) => post('/cruxtype/save', data),
    update: (data) => post('/cruxtype/update', data),
    delete: (data) => get('/cruxtype/delete', data),

    getShowCompassList: (data) => get('/cruxtype/getShowCompassList', data)
}

// 文件
api.file = {
    showOne: (data) => get('/file/showOne', data),
    updateFile: (data) => post('/file/updateFile', data)
}
//复盘文件上传
api.fileUpload = {
    seletByFileId: (data) => get('/UploadFile/seletByFileId', data),
    fileAnalysisByUserId: (data) =>
        post('/UploadFile/fileAnalysisByUserId', data)
}

api.openapi = {
    unbindingSubAccount: (data) =>
        post('/openapi/user/unbindingSubAccount', data)
}
// 公共
api.common = {
    uploadWordExcel:
        import.meta.env.VITE_BASE_URL + '/sensitivewords/importExcel', // 导入词库excel的地址
    pic: import.meta.env.VITE_BASE_URL + '/common/pic/', // 显示图片的地址
    uploadImg: import.meta.env.VITE_BASE_URL + '/common/uploadImg', // 上传图片的地址
    uploadFile: import.meta.env.VITE_BASE_URL + '/common/uploadFile/', // 上传文件的地址
    uploadUpdateFile: import.meta.env.VITE_BASE_URL + '/clientupdate/uploadFile' //上传的软件更新文件包
}

// 用户视频申诉
api.userVideoAppeal = {
    list: (data) => post('/uservideoappeal/list', data),
    info: (data) => get('/uservideoappeal/info', data),
    save: (data) => post('/uservideoappeal/save', data),
    update: (data) => post('/uservideoappeal/update', data),
    delete: (data) => get('/uservideoappeal/delete', data),
    handleAppeal: (data) => post('/uservideoappeal/handleAppeal', data)
}

// 通用模型
api.dataModel = {
    saveDataModel: (data) => post('/datamodel/saveDataModel', data),
    modelCruxTypeList: (data) => post('/datamodel/modelCruxTypeList', data),
    updateDataModel: (data) => post('/datamodel/updateDataModel', data),
    deleteDataModel: (data) => get('/datamodel/deleteDataModel', data),
    infoDataModel: (data) =>
        get('/datamodel/infoDataModel', data, {showLoading: true})
}

// 在线复盘
api.onlineAnalysis = {
    getOnlineAnalysisInfo: (data) => get('/openapi/getOnlineAnalysisInfo', data),
    getOnlineContrastAnalysisInfo: (data) =>
        get('/openapi/getOnlineContrastAnalysisInfo', data)
}

// 用户来源渠道
api.channel = {
    list: (data) => post('/channel/list', data),
    info: (data) => get('/channel/info', data),
    save: (data) => post('/channel/save', data),
    update: (data) => post('/channel/update', data),
    delete: (data) => get('/channel/delete', data),
    listTree: (data) => get('/channel/listTree', data)
}

// 用户来源渠道
api.channel = {
    list: (data) => post('/channel/list', data),
    info: (data) => get('/channel/info', data),
    save: (data) => post('/channel/save', data),
    update: (data) => post('/channel/update', data),
    delete: (data) => get('/channel/delete', data),
    listTree: (data) => get('/channel/listTree', data)
}

// 跟进销售人员姓名
api.sales = {
    list: (data) => post('/sales/list', data),
    info: (data, options) => get('/sales/info', data, options),
    save: (data) => post('/sales/save', data),
    update: (data) => post('/sales/update', data),
    delete: (data) => get('/sales/delete', data),
    agentSaleLists: (data) => get('/agent/agentSale/listByChannelId', data)
}

// ai训练
api.aitrain = {
    list: (data) => post('/aitrain/list', data),
    info: (data) => get('/aitrain/info', data),
    completeTrain: (data) => get('/aitrain/completeTrain', data),
    save: (data) => post('/aitrain/save', data),
    update: (data) => post('/aitrain/update', data),
    delete: (data) => get('/aitrain/delete', data)
}

// ai模型
api.aiModel = {
    list: (data) => post('/aimodel/list', data),
    info: (data) => get('/aimodel/info', data, {showLoading: true}),
    completeTrain: (data) => get('/aimodel/completeTrain', data),
    save: (data) => post('/aimodel/save', data),
    update: (data) => post('/aimodel/update', data),
    delete: (data) => get('/aimodel/delete', data)
}

// AI分析
api.aiModelAnalysis = {
    aiAnalysis: (data) =>
        post('/aianalysis/aiAnalysis', data, {showLoading: true}),
    listAiAnalysisByUuid: (data) => get('/aianalysis/listAiAnalysisByUuid', data),
    analysisStatusByUuid: (data) =>
        get('/aianalysis/analysisStatusByUuid', data, {showLoading: true}),
    getAllSessionId: (data) =>
        get('/aianalysis/getAllSessionId', data, {showLoading: true}),
    listAiButtonByTradeId: (data) =>
        get('/aianalysis/listAiButtonByTradeId', data)
}

api.openapiv2000 = {
    invokeTimingUpdateData: (data) =>
        get('/openapi/v2000/invokeTimingUpdateData', data, {showLoading: true})
}
api.tencentCos = {
    cosPublicReadTempToken: (data) =>
        get('/tencentCos/cosPublicReadTempToken', data)
}

// 用户备注
api.userremark = {
    list: (data) => post('/userremark/list', data),
    info: (data) => get('/userremark/info', data),
    save: (data) => post('/userremark/save', data),
    update: (data) => post('/userremark/update', data),
    delete: (data) => get('/userremark/delete', data),
    listFollowType: (data) => post('/dictdata/list', data)
}

// 对比分析
api.synccontrast = {
    listAllSyncContrast: (data) =>
        post('/synccontrast/listAllSyncContrast', data),
    getContrastAnalysisInfo: (data) =>
        get('/synccontrast/getContrastAnalysisInfo', data)
}

// 对比分析
api.synccontrast = {
    listAllSyncContrast: (data) =>
        post('/synccontrast/listAllSyncContrast', data),
    getContrastAnalysisInfo: (data) =>
        get('/synccontrast/getContrastAnalysisInfo', data)
}
api.dataScreenshotConfig = {
    info: (data, options) => get('/dataScreenshotConfig/info', data, options),
    delete: (data) => get('/dataScreenshotConfig/delete', data),
    list: (data) => post('/dataScreenshotConfig/list', data),
    save: (data) => post('/dataScreenshotConfig/save', data),
    update: (data) => post('/dataScreenshotConfig/update', data)
}
// 系统kv配置
api.systemkv = {
    info: (data) => get('/systemkv/info', data, {showLoading: true}),
    delete: (data) => get('/systemkv/delete', data),
    list: (data) => post('/systemkv/list', data),
    save: (data) => post('/systemkv/save', data),
    update: (data) => post('/systemkv/update', data),
    updateImgConfig: (data) => post('/systemkv/updateImgConfig', data),
    getImgConfig: (data) => get('/systemkv/getImgConfig', data)
}

// 代理ip管理
api.proxyip = {
    list: (data) => post('/proxyip/list', data),
    info: (data) => get('/proxyip/info', data, {showLoading: true}),
    save: (data) => post('/proxyip/save', data),
    update: (data) => post('/proxyip/update', data),
    delete: (data) => get('/proxyip/delete', data)
}

// 代理ip管理
api.proxyiprecord = {
    list: (data) => post('/proxyiprecord/list', data),
    info: (data) => get('/proxyiprecord/info', data, {showLoading: true}),
    save: (data) => post('/proxyiprecord/save', data),
    update: (data) => post('/proxyiprecord/update', data),
    delete: (data) => get('/proxyiprecord/delete', data)
}
export default api

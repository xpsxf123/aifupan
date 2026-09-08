/**
 * @description 后端 API 聚合封装：统一管理 backApi 的接口路径、请求方式与返回数据归一化逻辑。
 * 注意：
 * 1) 本文件包含多个业务模块（如 scriptMonitor），当后端接口字段/方法变更时，需要同步更新这里的封装与解析；
 * 2) 对复杂解析（例如 summary: string -> JSON）会做兼容处理，供页面侧直接使用，避免页面层重复写解析逻辑。
 */
import isMockUrl from './mockUrlConfig'
import {get, post, put, del} from './request-back'
import store from './../store';

const api = {}

api.common = {
    getByKey: (data) => get('/systemkv/getByKey', {key: data}),
}

// 用户
api.user = {
    register: (data) => post('/user/register', data),
    getPhoneCode: (data) => get('/user/getPhoneCode', data),
    login: (data) => post('/user/login', data),
    logout: (data) => get('/user/logout', data),
    updateByClient: (data) => post('/user/updateByClient', data),
    infoByClient: (data) => get('/user/infoByClient', data),
    checkPhoneCode: (data) => get('/user/checkPhoneCode', data),
    checkFreeze: (data) => get('/user/checkFreeze', data),
    updatePassword: (data) => post('/user/updatePasswordByClient', data)
}

// 用户
api.onlineUser = {
    register: (data) => post('/user/register', data),
    getPhoneCode: (data) => get('/user/getPhoneCode', data),
    login: (data) => post('/user/loginOnline', data),
    logout: (data) => get('/user/logout', data),
    updateByClient: (data) => post('/user/updateByClient', data),
    infoByClient: (data) => get('/user/infoByClient', data),
    checkPhoneCode: (data) => get('/user/checkPhoneCode', data),
    checkFreeze: (data) => get('/user/checkFreeze', data),
}

// 在线复盘
api.onlineAnalysis = {
    getOnlineAnalysisInfo: (data) => get('/openapi/getOnlineAnalysisInfo', data),
    getOnlineContrastAnalysisInfo: (data) => get('/openapi/getOnlineContrastAnalysisInfo', data),
}

// 文件
api.file = {
    showOne: (data) => get('/file/showOne', data),
    getCurrentUserSale: (data) => get('/sales/getCurrentUserSale', data, {load: false})
}

/*
云空间行业列表:/replay/trade/listByTenantAnchor
云空间主播列表:/replay/anchorurl/clientTenantAnchorList
*/
// 行业
api.trade = {
    listTree: (data) => get('/trade/listSimpleTree', data),
    list: (data) => post('/trade/list', data),
    listByAnchor: (data) => post('/trade/listByAnchor', data),
    listByTenantAnchor: (data) => post('/trade/listByTenantAnchor', data),
    updateSuggestTrade: (data) => get('/words/aiWords/updateSuggestTrade', data),
}

api.tenant = {
    tenantConfig: (data) => get('/power/tenant/tenantConfig', data, { load: false }),
}
// 字典
api.dictdata = {
    list: (data) => post('/dictdata/list', data),
    uploadFileDictList: (data) => get('/system/dictData/uploadFileDictList', data),
    dictDataListByCode: (data) => get('/system/dictData/dictDataListByCode', data),
    dictDataListByCodes: (data) => get('/system/dictData/dictDataListByCodes', data),
    // replay/system/dictData/dictDataTreeListByCode?code=client_assistant_ai_model_list
    dictDataTreeListByCode:(data) => get('/system/dictData/dictDataTreeListByCode', {code: 'client_assistant_ai_model_list'})    
}
// 文章
api.article = {
    list: (data) => post('/article/list', data),
}

const numberKey = [
    'aiAnalysisTime', 'aiTokenNum', 'anchorNum', 'dataBoardNum', 'imgIdentifyNum', 'kuaishouMonitorNum',
    'monitorNum','rpaAmountNum','searchHotVideoNum','searchInfluencerNum','shortVideoNum','smsMessageNum',
    'subscribeHotVideoNum','subscribeInfluencerNum','textExtractionNum','totalAiAnalysisTime','totalAiTokenNum',
    'totalAnchorNum','totalDataBoardNum','totalImgIdentifyNum','totalKuaishouMonitorNum','totalMonitorNum',
    'totalRpaAmountNum','totalSearchHotVideoNum','totalSearchInfluencerNum','totalShortVideoNum','totalSmsMessageNum',
    'totalSubscribeHotVideoNum','totalSubscribeInfluencerNum','totalTextExtractionNum','totalUserSubscribeHotVideoNum',
    'totalUserSubscribeInfluencerNum','useAiAnalysisTime','useAiTokenNum','useAnchorNum','useDataBoardNum','useImgIdentifyNum',
    'useKuaishouMonitorNum','useMonitorNum','useRpaAmountNum','useSearchHotVideoNum','useSearchInfluencerNum','useShortVideoNum','useSmsMessageNum',
    'useSubscribeHotVideoNum','useSubscribeInfluencerNum','useTextExtractionNum','useUserSubscribeHotVideoNum','useUserSubscribeInfluencerNum',
    'userSubscribeHotVideoNum','userSubscribeInfluencerNum'
]
// 用户资产
api.userProperty = {
    useProperty: (data) => post('/openapi/userproperty/useProperty', data),
    // 获取用户资产(新)
    getUserProperty: () => get('/openapi/userproperty/getUserProperty').then(res=>{
        Object.keys(res.data).forEach(key=>{
            if(numberKey.includes(key)){
                res.data[key] = Number(res.data[key])
            }else{
                res.data[key] = res.data[key] || 0
            }
        })
        store.commit("saveUserproperty", res.data);
        return res.data
    }),
    // 获取用户资产(老)- 老接口范围的是res对象,带有code,新接口直接返回data 数据.
    info: (data,...arg) => get("/openapi/userproperty/getUserProperty", data,...arg).then(res=>{
        Object.keys(res.data).forEach(key=>{
            if(numberKey.includes(key)){
                res.data[key] = Number(res.data[key] || 0)
            }else{
                res.data[key] = res.data[key] || 0
            }
        })
        store.commit("saveUserproperty", res.data);
        return res;
      }),
}
// ai训练
api.aiTrain = {
    aiTrainStatus: (data) => get('/aitrain/infoByVideoId', data),
    initiateTrain: (data) => post('/aitrain/save', data)
}

// 腾讯云点播
api.vod = {
    vodUploadSign: (data) => get('/vod/getVodUploadSign', data), // 获取签名
}

// 用户版本
api.userPackage = {
    packageListAll: (data) => get('/openapi/package/packageListAll', data),
}

// 邀请码
api.invitationCode = {
    exchange: (data) => get('/openapi/invitationcode/exchange', data),
}

// 子账号
api.subAccount = {
    list: (data) => post('/openapi/user/subAccountList', data),
    bind: (data) => post('/openapi/user/setUpASubAccount', data),
    unBind: (data) => post('/openapi/user/unbindingSubAccount', data),
    clientGetSubUserList: (data)=>get("/power/user/clientGetSubUserList" ,data),
    getSubUserAnchorList: (data)=>post("/words/anchorUrl/getSubUserAnchorList" ,data),
    getSubUserYesterdayVideoList: (data)=>post("/anchorVideo/getSubUserYesterdayVideoList" ,data),
    getSubUserYesterdayNotesVideoList: (data)=>post("/anchorVideo/getSubUserYesterdayNotesVideoList" ,data),
    getSubClientVideoList: (data)=>post("/anchorVideo/getSubClientVideoList" ,data)
}

// 词库
api.lexicon = {
    list: (data) => post('/lexicon/list', data),
    info: (data) => get('/lexicon/info', data),
    save: (data) => post('/lexicon/save', data),
    update: (data) => post('/lexicon/update', data),
    delete: (data) => get('/lexicon/delete', data),
    getWordsList: (data) => post('/lexicon/getWordsList', data), // 获取词库的词语列表
}
// 词语
api.sensitivewords = {
    list: (data) => post('/sensitivewordsClient/list', data),
    info: (data) => get('/sensitivewordsClient/info', data),
    save: (data) => post('/sensitivewordsClient/save', data),
    update: (data) => post('/sensitivewordsClient/update', data),
    delete: (data) => get('/sensitivewordsClient/delete', data),
}
// 视频标记
api.videomark = {
    listByUUID: (data) => get('/videomark/listByUUID', data),
    info: (data) => get('/videomark/info', data),
    save: (data) => post('/videomark/save', data),
    update: (data) => post('/videomark/update', data),
    delete: (data) => get('/videomark/delete', data),
}
api.openapi = {
    // 获取云空间视频列表
    listCloudVideo: (data) => post('/openapi/listCloudVideo', data),
    listCloudContrast: (data) => post('/openapi/listCloudContrast', data),
    v2000listTradeModel: (data) => get('/openapi/v2000/listTradeModel', data)
}

// 视频申诉
api.videoAppeal = {
    uploadVideoAppeal: (data) => post('/openapi/uservideoappeal/uploadVideoAppeal', data), //视频申诉
}
// 在线复盘
api.onlineAnalysis = {
    getOnlineAnalysisInfo: (data) => get('/openapi/getOnlineAnalysisInfo', data),
    getOnlineContrastAnalysisInfo: (data) => get('/openapi/getOnlineContrastAnalysisInfo', data),
}
api.uploadUrl = window.SITE_CONFIG['backApiURL'] + '/common/uploadImg' // 头像上传地址

// 词分类接口
api.cruxtype = {
    listTree: (data) => get('/cruxtype/listTree', data),
}

api.v2100 = {
    queryDanMuData: (...data) => post('/openapi/v2100/queryDanMuData', ...data),
    queryOtherDanMuData: (...data) => post('/openapi/v2100/queryOtherDanMuData', ...data),
    onlineChartData: (data) => get('/openapi/v2100/onlineChartData', data),
    // 搜索弹幕数据的总条数
    queryDanMuCount: (...data) => post('/openapi/v2100/queryDanMuCount', ...data),
    // 弹幕数据导出
    queryDanMuExport: (...data) => get('/openapi/v2100/queryDanMuExportGet', ...data),
}

api.scriptMonitor = {
    /**
     * @description 监控点位使用量统计（用于展示 AI 监控配额：质检/还原度/巡检）。
     * @param {Object} data 查询参数
     * @returns {Promise<{code:number,msg:string,data:any}>} 接口响应
     */
    monitorPositionStatistics: (data) => get(('/userproperty/monitorPositionStatistics'), data),
    /**
     * @description 获取主播/直播间基础配置（话术质检/还原度/巡检开关与相关字段）。
     * 注意：后端接口参数名为 anchorUrlUserId，但业务侧实际传入的是主播 secUid。
     * @param {Object} data 入参
     * @param {string} data.secUid 主播 secUid（优先使用）
     * @returns {Promise<{code:number,msg:string,data:any}>} 接口响应
     */
    getAnchorBasicConfig: (data = {}) => {
        const payload = {...(data || {})}
        const secUid = payload.secUid ?? payload.id ?? payload.anchorId ?? payload.anchorUrlUserId
        if (secUid === undefined || secUid === null || secUid === '') {
            return Promise.reject({code: 70013, msg: '缺少 secUid'})
        }
        return get(('/script-monitor/anchorBasicConfig'), { secUid })
    },
    /**
     * @description 获取单场录制的 AI 监控状态（话术质检/还原度/互动巡检）。
     * 兼容点：
     * 1) v1.4 接口由 GET 改为 POST(JSON body)
     * 2) v1.4 summary 字段变为 string(JSON)，需要解析后供页面直接取字段
     * 3) 兼容 data.monitors[] 与旧字段 data.scriptQualityInspection/scriptFidelityMonitor/interactionPatrol
     * @param {Object} data 入参
     * @returns {Promise<{code:number,msg:string,data:any}>} 接口响应（data 已归一化）
     */
    getScriptMonitorStatus: (data = {}) => {
        const safeParseJson = (val) => {
            if (val === null || val === undefined) return null
            if (typeof val === 'object') return val
            if (typeof val !== 'string') return null
            const raw = val.trim()
            if (!raw) return null
            if (!/^[\[{]/.test(raw)) return raw
            try {
                return JSON.parse(raw)
            } catch (e) {
                return raw
            }
        }
        const normalizeMonitor = (m = {}) => {
            const next = { ...(m || {}) }
            // reportId 是 Long，必须以字符串形式在前端流转，避免超出 JS Number 精度导致参数/展示异常
            if (next.reportId !== undefined && next.reportId !== null) next.reportId = String(next.reportId)
            // 兼容 summary: string(JSON) -> object
            if (typeof next.summary === 'string') next.summary = safeParseJson(next.summary)
            return next
        }
        const normalizeStatusData = (d = {}) => {
            const next = { ...(d || {}) }
            const monitors = Array.isArray(next.monitors) ? next.monitors.map(normalizeMonitor) : []
            const hasNamed =
                next.scriptQualityInspection || next.scriptFidelityMonitor || next.interactionPatrol
            if (!monitors.length && hasNamed) {
                // 兼容旧结构：后端未返回 monitors[] 时，把三个命名对象组装成 monitors[] 方便统一渲染
                const qc = next.scriptQualityInspection ? normalizeMonitor({ monitorType: 0, ...next.scriptQualityInspection }) : null
                const fidelity = next.scriptFidelityMonitor ? normalizeMonitor({ monitorType: 1, ...next.scriptFidelityMonitor }) : null
                const inspect = next.interactionPatrol ? normalizeMonitor({ monitorType: 2, ...next.interactionPatrol }) : null
                next.monitors = [qc, fidelity, inspect].filter(Boolean)
            } else if (monitors.length) {
                // 兼容新结构：已返回 monitors[] 时，反向补齐旧字段，避免旧页面取值点出错
                next.monitors = monitors
                if (!next.scriptQualityInspection) next.scriptQualityInspection = monitors.find((it) => Number(it?.monitorType) === 0) || null
                if (!next.scriptFidelityMonitor) next.scriptFidelityMonitor = monitors.find((it) => Number(it?.monitorType) === 1) || null
                if (!next.interactionPatrol) next.interactionPatrol = monitors.find((it) => Number(it?.monitorType) === 2) || null
            }
            if (next.scriptQualityInspection) next.scriptQualityInspection = normalizeMonitor(next.scriptQualityInspection)
            if (next.scriptFidelityMonitor) next.scriptFidelityMonitor = normalizeMonitor(next.scriptFidelityMonitor)
            if (next.interactionPatrol) next.interactionPatrol = normalizeMonitor(next.interactionPatrol)
            return next
        }

        const payload = {...(data || {})}
        if (payload.sourceType === undefined) {
            if (payload.fileId) payload.sourceType = 1
            else payload.sourceType = 0
        }
        if (payload.sceneType === undefined) payload.sceneType = 0
        if (payload.sourceId === undefined) {
            payload.sourceId = payload.videoId || payload.fileId || payload.id
        }
        // v1.4: 方法改为 POST(JSON body)
        return post(('/script-monitor/reportStatus'), payload).then((res) => {
            if (res?.code === 0 && res?.data) {
                res.data = normalizeStatusData(res.data)
            }
            return res
        })
    },
    /**
     * @description 批量查询多场录制的 AI 监控状态（优先用批量接口提升列表加载性能）。
     * 说明：返回 items 内部结构与 reportStatus 一致，这里复用相同的归一化逻辑（含 summary 解析）。
     * 注意：后端要求 sources 单项包含 secUid（用于主播维度的报告定位），调用方需要补齐该字段。
     * @param {Object} data 入参
     * @param {Object[]} data.sources 资源定位列表
     * @param {string} data.sources[].secUid 主播 secUid
     * @returns {Promise<{code:number,msg:string,data:{items:any[]}}>} 接口响应（items 已归一化）
     */
    batchReportStatus: (data) => post(('/script-monitor/batchReportStatus'), data).then((res) => {
        if (res?.code !== 0) return res
        // 兼容返回结构：部分后端会直接返回数组（data: []），这里统一归一化为 { items: [] }
        const rawItems = Array.isArray(res?.data?.items) ? res.data.items : (Array.isArray(res?.data) ? res.data : [])
        const items = rawItems
        if (!items.length) return res
        const safeParseJson = (val) => {
            if (val === null || val === undefined) return null
            if (typeof val === 'object') return val
            if (typeof val !== 'string') return null
            const raw = val.trim()
            if (!raw) return null
            if (!/^[\[{]/.test(raw)) return raw
            try {
                return JSON.parse(raw)
            } catch (e) {
                return raw
            }
        }
        const normalizeMonitor = (m = {}) => {
            const next = { ...(m || {}) }
            // reportId 是 Long，必须以字符串形式在前端流转，避免超出 JS Number 精度导致参数/展示异常
            if (next.reportId !== undefined && next.reportId !== null) next.reportId = String(next.reportId)
            if (typeof next.summary === 'string') next.summary = safeParseJson(next.summary)
            return next
        }
        const normalizeStatusData = (d = {}) => {
            const next = { ...(d || {}) }
            const monitors = Array.isArray(next.monitors) ? next.monitors.map(normalizeMonitor) : []
            const hasNamed =
                next.scriptQualityInspection || next.scriptFidelityMonitor || next.interactionPatrol
            if (!monitors.length && hasNamed) {
                const qc = next.scriptQualityInspection ? normalizeMonitor({ monitorType: 0, ...next.scriptQualityInspection }) : null
                const fidelity = next.scriptFidelityMonitor ? normalizeMonitor({ monitorType: 1, ...next.scriptFidelityMonitor }) : null
                const inspect = next.interactionPatrol ? normalizeMonitor({ monitorType: 2, ...next.interactionPatrol }) : null
                next.monitors = [qc, fidelity, inspect].filter(Boolean)
            } else if (monitors.length) {
                next.monitors = monitors
                if (!next.scriptQualityInspection) next.scriptQualityInspection = monitors.find((it) => Number(it?.monitorType) === 0) || null
                if (!next.scriptFidelityMonitor) next.scriptFidelityMonitor = monitors.find((it) => Number(it?.monitorType) === 1) || null
                if (!next.interactionPatrol) next.interactionPatrol = monitors.find((it) => Number(it?.monitorType) === 2) || null
            }
            if (next.scriptQualityInspection) next.scriptQualityInspection = normalizeMonitor(next.scriptQualityInspection)
            if (next.scriptFidelityMonitor) next.scriptFidelityMonitor = normalizeMonitor(next.scriptFidelityMonitor)
            if (next.interactionPatrol) next.interactionPatrol = normalizeMonitor(next.interactionPatrol)
            return next
        }

        res.data = { items: items.map((it) => normalizeStatusData(it)) }
        return res
    }),
    /**
     * @description 触发生成某场录制的 AI 报告（话术质检/还原度/互动巡检）。
     * 注意：sourceType/sourceId/sceneType 默认会做兜底，调用方可只传 videoId + monitorType。
     * @param {Object} data 入参
     * @returns {Promise<{code:number,msg:string,data:any}>} 接口响应
     */
    triggerScriptMonitorReport: (data = {}) => {
        const payload = {...(data || {})}
        if (payload.sourceType === undefined) payload.sourceType = 0
        if (payload.sceneType === undefined) payload.sceneType = 0
        if (payload.sourceId === undefined) payload.sourceId = payload.videoId || payload.fileId || payload.id
        return post(('/script-monitor/triggerReport'), payload)
    },
    setMonitorEnabled: (data = {}) => {
        const payload = { ...(data || {}) }
        const secUid = payload.secUid ?? payload.id ?? payload.anchorId ?? payload.anchorUrlUserId
        const monitorType = payload.monitorType
        const enabled = payload.enabled
        const query = `secUid=${encodeURIComponent(String(secUid ?? ''))}&monitorType=${encodeURIComponent(String(monitorType ?? ''))}&enabled=${encodeURIComponent(String(enabled ?? ''))}`
        return post(`/script-monitor/setMonitorEnabled?${query}`, {
            secUid: secUid ?? '',
            monitorType,
            enabled
        })
    },
    /**
     * @description 获取话术质检报告详情。
     * 兼容点：v1.4 新增 summaryJson（String），这里解析后挂到 data.summary 供页面侧复用。
     * @param {Object} data 入参
     * @returns {Promise<{code:number,msg:string,data:any}>} 接口响应（可能包含 data.summary）
     */
    getQualityInspectionReport: (data = {}) => {
        const payload = { ...(data || {}) }
        // reportId 是 Long：作为 query 参数传递时必须是字符串，避免 JS Number 精度丢失
        if (payload.reportId !== undefined && payload.reportId !== null) payload.reportId = String(payload.reportId)
        return get(('/script-monitor/qualityReportDetail'), payload).then((res) => {
        if (res?.code !== 0) return res
        const summaryJson = res?.data?.summaryJson
        if (typeof summaryJson === 'string') {
            try {
                // summaryJson 是可复用的统计摘要（字符串 JSON），解析后方便页面侧统一展示/判断
                res.data.summary = JSON.parse(summaryJson)
            } catch (e) {
                res.data.summary = null
            }
        }
        return res
        })
    },
    getInteractionPatrolReport: (data = {}) => {
        const payload = { ...(data || {}) }
        if (payload.reportId !== undefined && payload.reportId !== null) payload.reportId = String(payload.reportId)
        return get(('/script-monitor/patrolReportDetail'), payload)
    },
    getStandardScript: (data = {}) => {
        const payload = {...(data || {})}
        if (payload.secUid !== undefined && payload.secUid !== null && String(payload.secUid).trim()) {
            payload.secUid = String(payload.secUid).trim()
        } else {
            delete payload.secUid
        }
        if (payload.anchorUrlUserId !== undefined && payload.anchorUrlUserId !== null && payload.anchorUrlUserId !== '') {
            payload.anchorUrlUserId = Number(payload.anchorUrlUserId)
        } else {
            delete payload.anchorUrlUserId
        }
        return get(('/script-monitor/standardScriptDetail'), payload)
    },
    generateStandardScript: (data) => post(('/script-monitor/generateStandardScript'), data, { config: { timeout: 2 * 60 * 1000 } }),
    confirmStandardScript: (data) => post(('/script-monitor/confirmStandardScript'), data),
    getFidelityMonitorReport: (data = {}) => {
        const payload = { ...(data || {}) }
        if (payload.reportId !== undefined && payload.reportId !== null) payload.reportId = String(payload.reportId)
        return get(('/script-monitor/fidelityReportDetail'), payload)
    },
}

api.v2000 = {
    getOnlineAnalysis: (data) => get('/openapi/v2000/getOnlineAnalysis', data),
    getOnlineContrastAnalysis: (data) => get('/openapi/v2000/getOnlineContrastAnalysis', data),
    getAnalysis: (data) => get('/openapi/v2000/getAnalysis', data),
    getContrastAnalysis: (data) => get('/openapi/v2000/getContrastAnalysis', data),
    getVideoRoi: (data) => get('/openapi/v2000/getVideoRoi', data),
}

api.v2200 = {
    getPageCueWords: (data) => post('/words/cueWords/pageCueWords', data, {load: false}),
    identityAndAdditionalList: (data) => get('/openapi/v2200/identityAndAdditionalList', data, {load: false}),
    getCustomizeCueWords: (data) => post('/words/cueWords/customize-list', data, {load: false}),
}

api.v2300 = {
    dataScreenshotList: (data) => get('/openapi/v2300/dataScreenshotList', data, {load: false}),
    getDataScreenshotPutUrl: (data) => get('/openapi/v2300/getDataScreenshotPutUrl', data, {load: false}),
    screenshotUpload: (data) => post('/openapi/v2300/screenshotUpload', data),
    screenshotInfo: (data) => get('/openapi/v2300/screenshotInfo', data),
    updateScreenshot: (data) => post('/openapi/v2300/updateScreenshot', data),
    imageSecurity: (data) => get('/openapi/v2300/imageSecurity', data),
    dataScreenshotDelete: (data) => post('/openapi/v2300/dataScreenshotDelete', data),
    aiOptionConfig: (data) => get('/openapi/v2300/aiOptionConfig', data),
    getExistDataScreenshotList: (data) => get('/openapi/v2300/getExistDataScreenshotList', data)
}

api.v2400 = {
    infoByVideoId: (data) => get('/videodataviewing/infoByVideoId', data),
    infoByContrastId: (data) => get('/videodataviewing/infoByContrastId', data),
    createDataViewing: (data) => get('/videodataviewing/createDataViewing', data),
    historyBatchNumberVideoList: (data) => get('/openapi/v2400/historyBatchNumberVideoList', { limit: 20, ...data }),
    currentFreeVersion: (data) => get('/openapi/v2400/currentFreeVersion', data),
}

api.v2500 = {
    listDiagnosisModel: (data) => get('/ai/diagnosis/listDiagnosisModel', data),
    listDiagnosis: (data) => get('/ai/diagnosis/listDiagnosis', data),
    listUnreadDataDiagnosis: (data) => get('/ai/diagnosis/listUnreadDataDiagnosis', data, {load: false}),
    updateReadStatus: (data) => post('/ai/diagnosis/updateReadStatus', data, {load: false}),
    updateDiagnosisModel: (data) => post('/ai/diagnosis/updateDiagnosisModel', data),
    getDiagnosisDownloadUrl: (data) => get('/ai/diagnosis/getDiagnosisDownloadUrl', data),
    conversationByCueWordsIds: (data) => post('/ai/diagnosis/conversationByCueWordsIds', data,{load: false}),
    clientGetUserRewardList: (data) => post('/reward/clientinviterewardrecord/clientGetUserRewardList', data),
    getUserInviteUrl: (data) => get('/agent/inviteurlcode/getUserInviteUrl', data),
    clientGetRewardSummary: (data) => get('/reward/clientinviterewardrecord/clientGetRewardSummary', data),
    clientinviteactivity: (data) => get('/activity/clientinviteactivity/infoByClient', data),
    clintGetData: (data) => get('/userproperty/clintGetData', data),
    conversationPage: (data) => post('/ai/conversation/conversationPage', data),
    updateVideoSlice: (data) => post('/videoSlice/updateVideoSlice', data),
    addStar: (data) => post('/sourceStar/add', data),
    cancelStar: (data) => post('/sourceStar/cancel', data)
}


api.fileAnalysis = {
    clientFileList: (data) => post('/UploadFile/clientFileList', data),
    clientDeleteFile: (data) => post('/UploadFile/clientDeleteFile', data),
}

api.video = {
    clientVideoList: (data) => post('/AnchorVideo/clientVideoList', data),
    clientDeleteVideo: (data) => post('/AnchorVideo/clientDeleteVideo', data),
    clientListCloudVideo: (data) => post('/AnchorVideo/clientListCloudVideo', data),
    clientDeleteCloudVideo: (data) => get('/AnchorVideo/clientDeleteCloudVideo', data),
    clientBatchDeleteCloudVideos: (data, ...arg) => post('/AnchorVideo/clientBatchDeleteCloudVideos', data, ...arg),
    // 分析视频到云空间
    shareVideoToCloud: (data) => post('/anchorVideo/shareVideoToCloud', data),
    clientGetVideoByVideoId: (data) => get('/AnchorVideo/clientGetVideoByVideoId', data),
    aiOptimizePurposeSave: (data) => post('/aiOptimizePurpose/save', data),//新增AI优化目的
    aiOptimizePurposeUpdate: (data) => post('/aiOptimizePurpose/update', data, {notFormat: true}),//新增AI优化目的
    getOptimizePurpose: (data) => get('/aiOptimizePurpose/getBySourceId', data),
    getDataDiagnosisConfig: (data) => get('/AnchorVideo/getDataDiagnosisConfig', data),//获取数据诊断配置
    updateCloudRename: (data) => post('/anchorVideo/updateCloudRename', data),//新增AI优化目的

}

api.contrast = {
    clientContrastList: (data) => post('/synccontrast/clientContrastList', data),
    clientAddContrast: (data) => post('/synccontrast/clientAddContrast', data),
    clientAddCloudContrast: (data) => post('/synccontrast/clientAddCloudContrast', data),
    clientDeleteContrast: (data) => post('/synccontrast/clientDeleteContrast', data),
    clientDeleteCloudContrast: (data) => get('/synccontrast/clientDeleteCloudContrast', data),
    clientListCloudContrast: (data) => post('/synccontrast/clientListCloudContrast', data),
    // 分析对比记录到云空间
    shareContrastToCloud: (data) => post('/synccontrast/shareContrastToCloudPost', data),
    determineVideoContrast: (data) => post('/synccontrast/determineVideoContrastType', data),
    switchContrastPosition: (data) => post('/synccontrast/switchContrastPosition', data),
}


api.clientaifav = {
    getTableList: (data) => post(('/clientaifav/list'), data),
    batchDelete: (data) => post('/clientaifav/batchDelete', data),
    saveOrUpdate: (data) => post('/clientaifav/saveOrUpdate', data),
}

api.aiRelated = {
    // 对话
    sendAsk: (data, ...arg) => post('/aiRelated/ask', data, ...arg),
    // 历史记录
    getHistoryParagraphList: (data) => post('/aiRelated/historyParagraphList', data, {load: false}),
    addHistoryParagraph: (data) => post('/aiRelated/addHistoryParagraph', data, {load: false}),
    deleteHistoryParagraph: (data) => post('/aiRelated/deleteHistoryParagraph', data, {load: false}),
}

api.ai = {
    shareSave: (data) => post(('/ai/share/save'), data, {load: false}),
    shareInfo: (data) => get(('/ai/share/info'), data, {load: false}),
    updateLikesStatus: (data) => post(('/ai/conversation/updateLikesStatus'), data, {load: false}),
    serviceGenerateHtml: (data) => get('/ai/conversation/serviceGenerateHtml' ,data, {load:false}),//服务器生成html
    getHtmlStatus: (data) => post('/ai/conversation/getHtmlStatus' ,data, {load:false}),//获取html生成状态
}


// 主播
api.compere = {
    clientAnchorRecordList: (data) => post('/anchorurl/clientAnchorRecordList', data),
    topAnchor: (data) => post('/anchorurl/topAnchor', data), // 置顶主播
    clientTenantAnchorList: (data) => post('/anchorurl/clientTenantAnchorList', data),
    addOrUpdateAnchor: (data) => post(('/anchorurl/addOrUpdateAnchor'), data),
    updateAiPartial: (data) => post("/words/basicSettings/updateAiPartialNew", data), // 更新ai页面的部分主播字段
}


api.login = {
    getBannerList: (data) => post(('/loginrotateimage/noPage'), data),
}

api.words = {
    getVideoContent: (data) => get(('/words/videoContent/getVideoContent'), data),
    paragraphInfoByVideoId: (data) => get(('/words/videoDataViewing/paragraphInfoByVideoId'), data),
    generateVideoContent: (data) => post(('/words/videoContent/generateVideoContent'), data),
    exportVideoContent: (...data) => get(('/words/videoContent/exportVideoContent'), ...data),
    //  获取重要弹幕的状态
    getImportantBarrageStatus: (data) => get('/words/anchorVideoDetail/getImportantBarrageStatus', data),
    // 开始获取重要弹幕
    startImportantBarrage: (data) => get('/AnchorVideo/startImportantBarrage', data),
    // 获取行业热榜数据
    tradeRankPage: (data) => get('/words/tradeRank/page', data),
    // 统计达人排行数量
    countSimilarAnchors: (data) => get('/words/tradeRank/countSimilarAnchors', data),
    //获取ai页面的部分主播字段
    getAiPartial: (data) => get('/words/basicSettings/getAiPartial', data),
    getByAnchorNumber: (data) => get('/words/anchorUrl/getByAnchorNumber', data),
    getAuthStatus: (data) => get('/words/anchorUrl/getAuthStatus', data),
    getDataDiagnosisStatus: (data) => get('/ai/diagnosis/getDataDiagnosisStatus', data)
}

api.third = {
    getBarrageDataList: (data) => get('/third/tableStore/getBarrageDataList', data)
}


api.analysisMark = {
    getAnnotation: (data) => get('/analysisMark/query', data),
    addAnnotation: (data) => post('/analysisMark/add', data),
    updateAnnotation: (data) => post('/analysisMark/update', data),
    delAnnotation: (data) => post('/analysisMark/delete', data),
}

api.notes = {
    getNotes: (data) => get('/words/videoContent/notes', data),
    saveNotes: (...data) => post('/words/videoContent/notes', ...data),
    getNotesHistory: (data) => get('/words/videoContent/notes-history', data),
    // 同步笔记小结
    lastReview: (data) => get('/words/videoContent/last-review', data),
}

// 短视频
api.shortVideo = {
    // 达人订阅
    subscriptionEdit: (data) => put('/video/influencer/subscription/edit', data),//编辑达人订阅
    subscribe: (data) => post("/video/influencer/subscribe", data),//添加达人订阅
    subscriptions: (data) => get("/video/influencer/subscriptions", data),//查询达人订阅列表
    search: (data) => get("/video/influencer/search", data),//搜索达人数据
    expertHistory: (data) => get("/video/influencer/history", data),//搜索达人历史记录
    expertDetail: (data) => get("/video/influencer/detail", data),//WEB端-获取达人详情
    detailVideos: (data) => get("/video/influencer/detailVideos", data),//WEB端-获取达人详情视频
    subscriptionDel: (data) => del(`/video/influencer/subscription/${data}`, {}),//WEB端-删除达人订阅

    //短视频提取文案
    operationUsers: (data) => get('/video/extract/operationUsers', data),//查询操作用户列表
    history: (data) => post("/video/extract/history", data, {load: false}),//查询历史提取记录
    batch: (data) => del("/video/extract/batch", data),//批量删除视频提取记录
    getContent: (data) => get(`/video/extract/content/${data}`, {}),//查询视频文案内容

    //爆款
    subscriptionHot: (data) => get("/video/hotSearch/subscriptions", data),//查询爆款订阅列表
    subscriptionHotEdit: (data) => put('/video/hotSearch/subscription', data),//WEB端-编辑爆款订阅
    addSubscriptionHot: (data) => post('/video/hotSearch/subscription', data),//WEB端-添加爆款订阅
    hotSearch: (data) => get('/video/hotSearch/search', data),//WEB端-爆款搜索
    hotList: (data) => post('/video/hotSearch/list', data, {load: false}),//WEB端-爆款视频列表
    subscriptionHotList: (data) => post('/video/hotSearch/list/subscription', data, {load: false}),//WEB端-根据订阅条件查询视频列表
    subscriptionHotListExample: (data) => post('/video/hotSearch/list/example', data, {load: false}),//WEB端-根据爆款示例条件查询视频列表
    hotHistory: (data) => get('/video/hotSearch/history', data),//WEB端-爆款搜索历史记录
    delSubscriptionHot: (data) => del(`/video/hotSearch/subscription/${data}`, {}),//WEB端-删除爆款订阅

    //分组管理
    editGroup: (data) => put('/video/group', data),//WEB端-编辑分组
    addGroup: (data) => post('/video/group', data),//WEB端-添加分组
    groupDetail: (data) => get('/video/group/{groupId}', data),//WEB端-获取分组详情
    delGroup: (data) => del(`/video/group/${data}`, {}),//WEB端-删除分组
    options: (data) => get('/video/group/options', data),//WEB端-获取分组选项
    groupList: (data) => post('/video/group/list', data),//WEB端-查询分组列表

}

// 提示词管理
api.prompt = {
    // 添加用户自定义提示词
    list: (data) => post('/ai/custPrompt/privateList', data),
    // 获取用户自定义提示词详情
    info: (data) => get('/ai/custPrompt/info', data),
    // 删除用户自定义提示词
    delete: (data) => get(`/ai/custPrompt/delete`, data),
    // 新增用户自定义提示词
    save: (data) => post('/ai/custPrompt/save', data),
    // 修改用户自定义提示词
    update: (data) => post('/ai/custPrompt/update', data),

    generateCorrectAiContent: (data) => get('/ai/conversation/serviceCorrectAiContent', data),

    getCorrectStatus: (data) => post('/ai/conversation/getCorrectStatus', data,{load: false}),

}

api.placeholder = {
    listForClient: (data) => get('/ai/placeholder/listForClient', data),
}

api.anchorKnowledge = {
    info: (data) => get('/anchorKnowledge/info', data),
    save: (data) => post('/anchorKnowledge/save', data),
    update: (data) => post('/anchorKnowledge/update', data),
}

export default api



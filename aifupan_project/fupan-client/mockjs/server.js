const http = require('http')
const fs = require('fs')
const path = require('path')
const url = require('url')
const mockData = require('./index')

const PORT = Number(process.env.MOCK_PORT || process.env.PORT || 3000)
const DB_PATH = path.join(__dirname, 'db', 'script_monitor_db.json')

const readJsonFile = (filePath, fallback) => {
    try {
        if (!fs.existsSync(filePath)) return fallback
        const text = fs.readFileSync(filePath, 'utf8')
        if (!text) return fallback
        return JSON.parse(text)
    } catch (e) {
        return fallback
    }
}

const writeJsonFile = (filePath, data) => {
    fs.mkdirSync(path.dirname(filePath), { recursive: true })
    fs.writeFileSync(filePath, JSON.stringify(data || {}, null, 2), 'utf8')
}

const loadDb = () => {
    const base = {
        anchors: {},
        videos: {},
        resources: {},
        reports: {},
        pendingJobs: {},
        standardScripts: {},
        standardScriptByAnchorUrlUserId: {},
        anchorMonitorOccupation: {},
        mockConfig: {
            strict: true,
            tokenAmount: 200000,
            packageSupported: true,
            monitorPositions: {
                scriptQualityInspectionNum: { total: 20, use: 0 },
                scriptFidelityMonitorNum: { total: 20, use: 0 },
                interactionPatrolNum: { total: 20, use: 0 }
            }
        }
    }
    const db = readJsonFile(DB_PATH, base)
    return Object.assign(base, db || {})
}

const saveDb = (db) => {
    writeJsonFile(DB_PATH, db || {})
}

const sendJson = (res, payload, statusCode = 200) => {
    const body = JSON.stringify(payload || {})
    res.statusCode = statusCode
    res.setHeader('Content-Type', 'application/json; charset=utf-8')
    res.setHeader('Access-Control-Allow-Origin', '*')
    res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Token, source, webVersion, sign, timestamp, nonce')
    res.end(body)
}

const parseBody = (req) => {
    return new Promise((resolve) => {
        const chunks = []
        req.on('data', (c) => chunks.push(c))
        req.on('end', () => {
            const raw = Buffer.concat(chunks).toString('utf8')
            if (!raw) return resolve({})
            try {
                resolve(JSON.parse(raw))
            } catch (e) {
                resolve({})
            }
        })
    })
}

const hashString = (str = '') => {
    let h = 0
    const s = String(str)
    for (let i = 0; i < s.length; i++) {
        h = ((h << 5) - h) + s.charCodeAt(i)
        h |= 0
    }
    return Math.abs(h)
}

const nowTs = () => Date.now()

const statusText = (status) => {
    const map = {
        0: '未生成',
        1: '生成中',
        2: '已生成',
        3: '生成失败',
        4: '不可生成'
    }
    return map[status] || '未知'
}

const monitorTypeText = (monitorType) => {
    if (monitorType === 0) return '话术质检'
    if (monitorType === 1) return '话术还原度'
    if (monitorType === 2) return '互动巡检'
    return '报告'
}

const ensureMockConfig = (db) => {
    db.mockConfig = db.mockConfig || {}
    const conf = db.mockConfig
    if (conf.strict === undefined) conf.strict = true
    if (conf.tokenAmount === undefined) conf.tokenAmount = 200000
    if (conf.packageSupported === undefined) conf.packageSupported = true
    conf.monitorPositions = conf.monitorPositions || {}
    const mp = conf.monitorPositions
    if (!mp.scriptQualityInspectionNum) mp.scriptQualityInspectionNum = { total: 20, use: 0 }
    if (!mp.scriptFidelityMonitorNum) mp.scriptFidelityMonitorNum = { total: 20, use: 0 }
    if (!mp.interactionPatrolNum) mp.interactionPatrolNum = { total: 20, use: 0 }
    Object.keys(mp).forEach((k) => {
        const item = mp[k] || {}
        item.total = Number(item.total ?? 0)
        item.use = Number(item.use ?? 0)
        if (!Number.isFinite(item.total) || item.total < 0) item.total = 0
        if (!Number.isFinite(item.use) || item.use < 0) item.use = 0
        if (item.use > item.total) item.use = item.total
        mp[k] = item
    })
    return conf
}

const getMonitorPositionItem = (db, code) => {
    const conf = ensureMockConfig(db)
    const item = conf.monitorPositions?.[code] || { total: 0, use: 0 }
    const total = Number(item.total ?? 0)
    const use = Number(item.use ?? 0)
    const remaining = Math.max(total - use, 0)
    return { total, use, remaining }
}

const updateMonitorPositionUse = (db, code, deltaUse) => {
    const conf = ensureMockConfig(db)
    conf.monitorPositions = conf.monitorPositions || {}
    const item = conf.monitorPositions?.[code] || { total: 0, use: 0 }
    const total = Number(item.total ?? 0)
    const nextUse = Math.min(Math.max(Number(item.use ?? 0) + Number(deltaUse ?? 0), 0), total)
    conf.monitorPositions[code] = { total, use: nextUse }
}

const strictError = (code, msg) => ({ code, msg, data: null })

const buildResourceKey = (sourceType, sceneType, sourceId) => `${sourceType}_${sceneType}_${String(sourceId ?? '')}`

const buildStableReportId = (resourceKey, monitorType) => {
    const seed = hashString(`${resourceKey}_${monitorType}`)
    return Number(`${(seed % 900000000) + 100000000}${monitorType + 1}`)
}

const buildDynamicReportId = (resourceKey, monitorType) => {
    const seed = hashString(`${resourceKey}_${monitorType}_${nowTs()}`)
    return Number(`${(seed % 900000000) + 100000000}${monitorType + 1}`)
}

const normalizeConfirmedRecords = (records = []) => {
    const base = [
        { role: 1, label: '运营已知晓', confirmed: false },
        { role: 2, label: '主播已知晓', confirmed: false },
        { role: 3, label: '主管已知晓', confirmed: false }
    ]
    if (!Array.isArray(records) || !records.length) return base
    return base.map((b, idx) => {
        const r = records[idx] || {}
        return {
            role: b.role,
            label: b.label,
            confirmed: !!r.confirmed
        }
    })
}

const initResource = (db, sourceType, sceneType, sourceId) => {
    db.resources = db.resources || {}
    const key = buildResourceKey(sourceType, sceneType, sourceId)
    if (db.resources[key]) return db.resources[key]

    const seed = hashString(key)
    const p = seed % 6
    const interactionSupported = Number(sourceType) === 0 && Number(sceneType) === 0

    const base = {
        sourceType: Number(sourceType) || 0,
        sceneType: Number(sceneType) || 0,
        sourceId: String(sourceId ?? ''),
        scriptQualityInspection: {
            monitorType: 0,
            monitorTypeText: monitorTypeText(0),
            status: 0,
            statusText: statusText(0),
            reportId: null,
            summary: null,
            isRead: false,
            canConfirm: true,
            unavailableReason: null
        },
        scriptFidelityMonitor: {
            monitorType: 1,
            monitorTypeText: monitorTypeText(1),
            status: 0,
            statusText: statusText(0),
            reportId: null,
            summary: null,
            isRead: false,
            canConfirm: true,
            unavailableReason: null
        },
        interactionPatrol: interactionSupported ? {
            monitorType: 2,
            monitorTypeText: monitorTypeText(2),
            status: 0,
            statusText: statusText(0),
            reportId: null,
            summary: null,
            isRead: false,
            canConfirm: true,
            unavailableReason: null
        } : null
    }

    if (p === 1) {
        base.scriptQualityInspection.status = 1
        base.scriptQualityInspection.statusText = statusText(1)
    }
    if (p === 2) {
        base.scriptQualityInspection.status = 2
        base.scriptQualityInspection.statusText = statusText(2)
        base.scriptFidelityMonitor.status = 2
        base.scriptFidelityMonitor.statusText = statusText(2)
        if (interactionSupported) {
            base.interactionPatrol.status = 2
            base.interactionPatrol.statusText = statusText(2)
        }
    }
    if (p === 3) {
        base.scriptQualityInspection.status = 3
        base.scriptQualityInspection.statusText = statusText(3)
        base.scriptQualityInspection.unavailableReason = 'AI 服务暂不可用，请稍后重试'
    }
    if (p === 4 && interactionSupported) {
        base.interactionPatrol.status = 4
        base.interactionPatrol.statusText = statusText(4)
        base.interactionPatrol.unavailableReason = '本场无弹幕数据'
    }
    if (p === 5) {
        base.scriptQualityInspection.status = 2
        base.scriptQualityInspection.statusText = statusText(2)
        base.scriptFidelityMonitor.status = 2
        base.scriptFidelityMonitor.statusText = statusText(2)
        base.scriptQualityInspection.isRead = true
        base.scriptFidelityMonitor.isRead = true
        if (interactionSupported) {
            base.interactionPatrol.status = 2
            base.interactionPatrol.statusText = statusText(2)
            base.interactionPatrol.isRead = true
        }
    }

    const ensureGenerated = (monitorType) => {
        const reportId = buildStableReportId(key, monitorType)
        db.reports = db.reports || {}
        if (monitorType === 0) {
            const seed = hashString(`${key}_qc_${reportId}`)
            const crashCount = seed % 3
            const slackCount = (seed + 2) % 4
            const brandDamageCount = (seed + 1) % 2
            const afterSalesCount = (seed + 3) % 3
            if (!db.reports[String(reportId)] && !db.reports[reportId]) {
                const reportContent = [
                    '<p><strong>结论摘要</strong></p>',
                    `<p>本场共识别到负面话术风险点：${crashCount + slackCount + brandDamageCount + afterSalesCount} 处。</p>`,
                    `<p>崩盘话术：${crashCount}；摸鱼话术：${slackCount}；有损品牌：${brandDamageCount}；增加售后：${afterSalesCount}。</p>`,
                    '<p><strong>建议</strong></p>',
                    '<p>建议在关键卖点环节避免夸大承诺与绝对化表述，及时补充限制条件与售后口径。</p>'
                ].join('')
                db.reports[String(reportId)] = {
                    reportId,
                    sourceType: base.sourceType,
                    sceneType: base.sceneType,
                    sourceId: base.sourceId,
                    monitorType: 0,
                    status: 2,
                    statusText: statusText(2),
                    crashCount,
                    slackCount,
                    brandDamageCount,
                    afterSalesCount,
                    reportContent,
                    anchorName: `主播${(seed % 9) + 1}`,
                    liveTitle: '示例直播',
                    liveTime: '2026-05-19 14:00-16:00',
                    isRead: !!base.scriptQualityInspection.isRead,
                    canConfirm: true,
                    confirmedRecords: normalizeConfirmedRecords([]),
                    createDate: '2026-05-19 17:30:00',
                    __resourceKey: key
                }
            }
            base.scriptQualityInspection.reportId = reportId
            base.scriptQualityInspection.summary = { crashCount, slackCount, brandDamageCount, afterSalesCount }
        }

        if (monitorType === 1) {
            const seed = hashString(`${key}_fidelity_${reportId}`)
            const speechMode = seed % 2
            const speechSpeed = 200 + (seed % 200)
            const score = 70 + (seed % 30)
            const missedSegments = seed % 5
            const outOfOrderSegments = (seed + 2) % 3
            const deviatedSegments = (seed + 3) % 6
            const freePlaySegments = speechMode === 0 ? (seed % 3) : 0
            const reportContent = [
                '<p><strong>偏差摘要</strong></p>',
                `<p>评分：${score}；语速：${speechSpeed} 字/分钟。</p>`,
                '<p><strong>建议</strong></p>',
                '<p>建议在关键卖点段落减少遗漏，并保持节奏一致。</p>'
            ].join('')
            if (!db.reports[String(reportId)] && !db.reports[reportId]) {
                db.reports[String(reportId)] = {
                    reportId,
                    sourceType: base.sourceType,
                    sceneType: base.sceneType,
                    sourceId: base.sourceId,
                    monitorType: 1,
                    status: 2,
                    statusText: statusText(2),
                    speechMode,
                    speechModeText: speechMode === 1 ? '循环模式' : '非循环模式',
                    speechSpeed,
                    score,
                    deviationSummary: {
                        missedSegments,
                        outOfOrderSegments,
                        deviatedSegments,
                        freePlaySegments
                    },
                    reportContent,
                    anchorName: `主播${(seed % 9) + 1}`,
                    liveTitle: '示例直播',
                    liveTime: '2026-05-19 14:00-16:00',
                    isRead: !!base.scriptFidelityMonitor.isRead,
                    canConfirm: true,
                    createDate: '2026-05-19 17:35:00',
                    __resourceKey: key
                }
            }
            base.scriptFidelityMonitor.reportId = reportId
            base.scriptFidelityMonitor.summary = {
                score,
                speechSpeed,
                deviationSummary: '整体节奏接近标准稿，部分段落存在遗漏'
            }
        }

        if (monitorType === 2 && interactionSupported) {
            const seed = hashString(`${key}_inspect_${reportId}`)
            const effectivenessPercentage = 60 + (seed % 40)
            const missed = seed % 8
            if (!db.reports[String(reportId)] && !db.reports[reportId]) {
                const reportContent = [
                    '<p><strong>巡检摘要</strong></p>',
                    `<p>本场互动有效性：${effectivenessPercentage}%；未及时回复：${missed} 条。</p>`,
                    '<p><strong>建议</strong></p>',
                    '<p>建议在高频问题（价格/优惠/尺码/发货）出现后优先回复，并避免“随意发挥”导致信息不一致。</p>'
                ].join('')
                const summaryText = `本场共检测到需回复弹幕 ${Math.max(10, missed + 12)} 条，其中有效回复 ${Math.max(0, 10 - missed)} 条，未及时回复 ${missed} 条。`
                db.reports[String(reportId)] = {
                    reportId,
                    sourceType: base.sourceType,
                    sceneType: base.sceneType,
                    sourceId: base.sourceId,
                    monitorType: 2,
                    status: 2,
                    statusText: statusText(2),
                    effectivenessPercentage,
                    summary: summaryText,
                    reportContent,
                    details: [],
                    anchorName: `主播${(seed % 9) + 1}`,
                    liveTitle: '示例直播',
                    liveTime: '2026-05-19 14:00-16:00',
                    isRead: !!base.interactionPatrol.isRead,
                    canConfirm: true,
                    createDate: '2026-05-19 17:40:00',
                    __resourceKey: key
                }
            }
            base.interactionPatrol.reportId = reportId
            const summaryText = db.reports[String(reportId)]?.summary || db.reports[reportId]?.summary
            base.interactionPatrol.summary = {
                effectivenessPercentage,
                summary: summaryText,
                interactionRate: effectivenessPercentage,
                summaryText,
                missedCount: missed
            }
        }
    }

    if (base.scriptQualityInspection.status === 2) ensureGenerated(0)
    if (base.scriptFidelityMonitor.status === 2) ensureGenerated(1)
    if (interactionSupported && base.interactionPatrol.status === 2) ensureGenerated(2)

    db.resources[key] = base
    return base
}

const initVideo = (db, videoId) => {
    db.videos = db.videos || {}
    if (db.videos[videoId]) return db.videos[videoId]
    const resource = initResource(db, 0, 0, videoId)
    const mapped = {
        videoId: String(videoId ?? ''),
        scriptQualityInspection: resource.scriptQualityInspection,
        scriptFidelityMonitor: resource.scriptFidelityMonitor,
        interactionPatrol: resource.interactionPatrol
    }
    db.videos[videoId] = mapped
    return mapped
}

const patchVideoCache = (db, resource) => {
    const key = buildResourceKey(resource.sourceType, resource.sceneType, resource.sourceId)
    if (resource.sourceType === 0 && resource.sceneType === 0) {
        db.videos = db.videos || {}
        db.videos[resource.sourceId] = {
            videoId: String(resource.sourceId ?? ''),
            scriptQualityInspection: resource.scriptQualityInspection,
            scriptFidelityMonitor: resource.scriptFidelityMonitor,
            interactionPatrol: resource.interactionPatrol
        }
    }
    return key
}

const finishPendingJobs = (db) => {
    const now = nowTs()
    const keys = Object.keys(db.pendingJobs || {})
    keys.forEach((k) => {
        const job = db.pendingJobs[k]
        if (!job) return
        if (now < job.readyAt) return

        const sourceType = Number(job.sourceType ?? 0)
        const sceneType = Number(job.sceneType ?? 0)
        const sourceId = String(job.sourceId ?? job.videoId ?? '')
        const resource = initResource(db, sourceType, sceneType, sourceId)
        const resourceKey = buildResourceKey(sourceType, sceneType, sourceId)
        const reportId = job.reportId
        const monitorType = Number(job.monitorType)

        if (monitorType === 0) {
            resource.scriptQualityInspection.status = 2
            resource.scriptQualityInspection.statusText = statusText(2)
            resource.scriptQualityInspection.reportId = reportId
            const stable = initResource(db, sourceType, sceneType, sourceId)
            const tmpKey = buildResourceKey(sourceType, sceneType, sourceId)
            const seed = hashString(`${tmpKey}_qc_${reportId}`)
            const crashCount = seed % 3
            const slackCount = (seed + 2) % 4
            const brandDamageCount = (seed + 1) % 2
            const afterSalesCount = (seed + 3) % 3
            resource.scriptQualityInspection.summary = { crashCount, slackCount, brandDamageCount, afterSalesCount }
            db.reports[String(reportId)] = {
                reportId,
                sourceType,
                sceneType,
                sourceId,
                monitorType,
                status: 2,
                statusText: statusText(2),
                crashCount,
                slackCount,
                brandDamageCount,
                afterSalesCount,
                reportContent: '<p><strong>结论摘要</strong></p><p>（Mock）报告已生成。</p>',
                anchorName: `主播${(seed % 9) + 1}`,
                liveTitle: '示例直播',
                liveTime: '2026-05-19 14:00-16:00',
                isRead: false,
                canConfirm: true,
                confirmedRecords: normalizeConfirmedRecords([]),
                createDate: '2026-05-19 17:30:00',
                __resourceKey: resourceKey
            }
            void stable
        }

        if (monitorType === 1) {
            resource.scriptFidelityMonitor.status = 2
            resource.scriptFidelityMonitor.statusText = statusText(2)
            resource.scriptFidelityMonitor.reportId = reportId
            resource.scriptFidelityMonitor.summary = {
                score: 86,
                speechSpeed: 280,
                deviationSummary: '整体节奏接近标准稿，部分段落存在遗漏'
            }
            db.reports[String(reportId)] = {
                reportId,
                sourceType,
                sceneType,
                sourceId,
                monitorType,
                status: 2,
                statusText: statusText(2),
                speechMode: 1,
                speechModeText: '循环模式',
                speechSpeed: 280,
                score: 86,
                deviationSummary: {
                    missedSegments: 2,
                    outOfOrderSegments: 1,
                    deviatedSegments: 3,
                    freePlaySegments: 1
                },
                reportContent: '<p><strong>偏差摘要</strong></p><p>（Mock）报告已生成。</p>',
                anchorName: '主播A',
                liveTitle: '示例直播',
                liveTime: '2026-05-19 14:00-16:00',
                isRead: false,
                canConfirm: true,
                createDate: '2026-05-19 17:35:00',
                __resourceKey: resourceKey
            }
        }

        if (monitorType === 2 && resource.interactionPatrol) {
            resource.interactionPatrol.status = 2
            resource.interactionPatrol.statusText = statusText(2)
            resource.interactionPatrol.reportId = reportId
            const summaryText = '本场共检测到需回复弹幕 25 条，其中有效回复 18 条，未及时回复 7 条。'
            resource.interactionPatrol.summary = {
                effectivenessPercentage: 72,
                summary: summaryText,
                interactionRate: 72,
                summaryText,
                missedCount: 7
            }
            db.reports[String(reportId)] = {
                reportId,
                sourceType,
                sceneType,
                sourceId,
                monitorType,
                status: 2,
                statusText: statusText(2),
                effectivenessPercentage: 72,
                summary: summaryText,
                reportContent: '<p><strong>巡检摘要</strong></p><p>（Mock）报告已生成。</p>',
                details: [],
                anchorName: '主播A',
                liveTitle: '示例直播',
                liveTime: '2026-05-19 14:00-16:00',
                isRead: false,
                canConfirm: true,
                createDate: '2026-05-19 17:40:00',
                __resourceKey: resourceKey
            }
        }

        db.resources[resourceKey] = resource
        patchVideoCache(db, resource)
        delete db.pendingJobs[k]
    })
}

const handleReplayApi = async (req, res, pathname, query) => {
    const db = loadDb()
    finishPendingJobs(db)

    const getReportById = (reportId) => db.reports?.[String(reportId)] || db.reports?.[reportId] || null

    const setReportRead = (reportId, confirmRole) => {
        const report = getReportById(reportId)
        if (!report) return { ok: false, code: 70012, msg: '报告不存在' }
        report.isRead = true
        if (Number(confirmRole) && Number(report.monitorType) === 0) {
            report.confirmedRecords = normalizeConfirmedRecords(report.confirmedRecords)
            const idx = Number(confirmRole) === 1 ? 0 : Number(confirmRole) === 2 ? 1 : Number(confirmRole) === 3 ? 2 : -1
            if (idx >= 0 && report.confirmedRecords[idx]) {
                report.confirmedRecords[idx].confirmed = true
            }
        }
        db.reports[String(reportId)] = report

        const resourceKey = report.__resourceKey || buildResourceKey(report.sourceType, report.sceneType, report.sourceId)
        if (resourceKey && db.resources?.[resourceKey]) {
            const resource = db.resources[resourceKey]
            if (resource.scriptQualityInspection?.reportId === reportId) resource.scriptQualityInspection.isRead = true
            if (resource.scriptFidelityMonitor?.reportId === reportId) resource.scriptFidelityMonitor.isRead = true
            if (resource.interactionPatrol?.reportId === reportId) resource.interactionPatrol.isRead = true
            db.resources[resourceKey] = resource
            patchVideoCache(db, resource)
        }
        return { ok: true, code: 0, msg: 'success' }
    }

    const buildAnchorBasicConfig = (anchorUrlUserId) => {
        const id = Number(anchorUrlUserId || 0) || 9876001
        const seed = hashString(String(id))
        const existing = db.anchors[String(id)] || {}
        const standardScriptId = db.standardScriptByAnchorUrlUserId?.[String(id)] || existing.standardScriptId || null
        const resp = {
            anchorUrlUserId: id,
            anchorId: existing.anchorId || Number(existing.anchorId || 1234567890),
            secUid: existing.secUid || 'secUid_mock',
            anchorName: existing.anchorName || `主播${(seed % 9) + 1}`,
            tradeId: existing.tradeId || 1001,
            tradeName: existing.tradeName || '示例行业',
            accountType: existing.accountType ?? 0,
            livingMode: existing.livingMode ?? 1,
            accountWaterLevel: existing.accountWaterLevel ?? 2,
            accountFlow: existing.accountFlow ?? 3,
            recordTime: existing.recordTime || '06:00:00-19:00:00',
            smsTip: existing.smsTip ?? 3,
            engSerViceType: existing.engSerViceType || '16k_zh',
            recordDefinition: existing.recordDefinition ?? 1,
            recordLimitType: existing.recordLimitType ?? 2,
            recordLimitValue: existing.recordLimitValue ?? 30,
            isAutoUploadCloud: existing.isAutoUploadCloud ?? 1,
            isAutoAnalysis: existing.isAutoAnalysis ?? 1,
            isAutoDiagnosis: existing.isAutoDiagnosis ?? 0,
            isDataDiagnosis: existing.isDataDiagnosis ?? 1,
            isScriptQualityInspection: existing.isScriptQualityInspection ?? (seed % 2),
            isScriptFidelityMonitor: existing.isScriptFidelityMonitor ?? 0,
            isInteractionPatrol: existing.isInteractionPatrol ?? ((seed + 1) % 2),
            standardScriptId: standardScriptId ? Number(standardScriptId) : null
        }
        db.anchors[String(id)] = Object.assign(existing, resp)
        return resp
    }

    const handleReportStatus = (sourceType, sceneType, sourceId) => {
        if (sourceId === null || sourceId === undefined || sourceId === '') {
            return sendJson(res, { code: 70013, msg: 'sourceId 不能为空', data: null })
        }
        const conf = ensureMockConfig(db)
        const st = Number(sourceType ?? 0)
        const sc = Number(sceneType ?? 0)
        if (conf.strict) {
            if (st === 0 && sc !== 0) {
                return sendJson(res, { code: 70013, msg: '录制视频仅支持复盘场景', data: null })
            }
            if (st === 1 && ![1, 2].includes(sc)) {
                return sendJson(res, { code: 70013, msg: '上传文件仅支持视频分析/文案预审场景', data: null })
            }
        }
        const resource = initResource(db, Number(sourceType), Number(sceneType), String(sourceId))
        patchVideoCache(db, resource)
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: resource })
    }

    const handleTriggerReport = (payload = {}) => {
        const sourceType = Number(payload.sourceType ?? 0)
        const sceneType = Number(payload.sceneType ?? 0)
        const sourceId = String(payload.sourceId ?? '')
        const monitorType = Number(payload.monitorType)
        if (!sourceId) return sendJson(res, { code: 70013, msg: 'sourceId 不能为空', data: null })
        if (![0, 1, 2].includes(monitorType)) return sendJson(res, { code: 70013, msg: 'monitorType 不合法', data: null })
        const conf = ensureMockConfig(db)
        if (conf.strict) {
            if (sourceType === 0 && sceneType !== 0) {
                return sendJson(res, { code: 70013, msg: '录制视频仅支持复盘场景', data: null })
            }
            if (sourceType === 1 && ![1, 2].includes(sceneType)) {
                return sendJson(res, { code: 70013, msg: '上传文件仅支持视频分析/文案预审场景', data: null })
            }
            if (!conf.packageSupported) {
                return sendJson(res, strictError(70003, '当前套餐不支持，请升级'))
            }
            if (Number(conf.tokenAmount ?? 0) < 100000) {
                return sendJson(res, strictError(70001, '您的算力数量不足，请联系产品顾问购买。'))
            }
            const authCode = monitorType === 0 ? 'scriptQualityInspectionNum' : monitorType === 1 ? 'scriptFidelityMonitorNum' : 'interactionPatrolNum'
            const mp = getMonitorPositionItem(db, authCode)
            if (mp.total <= 0) {
                return sendJson(res, strictError(70002, `${monitorTypeText(monitorType)}授权数量不足，请联系产品顾问购买`))
            }
        }
        if (monitorType === 2 && !(sourceType === 0 && sceneType === 0)) {
            return sendJson(res, { code: 70013, msg: '该场景不支持互动巡检', data: null })
        }

        const resource = initResource(db, sourceType, sceneType, sourceId)
        if (monitorType === 2 && resource.interactionPatrol?.unavailableReason === '本场无弹幕数据') {
            saveDb(db)
            return sendJson(res, { code: 70010, msg: '本场无弹幕数据，无法生成互动巡检报告', data: null })
        }

        const reportField = monitorType === 0 ? 'scriptQualityInspection' : monitorType === 1 ? 'scriptFidelityMonitor' : 'interactionPatrol'
        const current = resource?.[reportField]
        if (current?.status === 1) {
            saveDb(db)
            return sendJson(res, { code: 0, msg: '已触发生成，请刷新查看状态', data: null })
        }

        const resourceKey = buildResourceKey(sourceType, sceneType, sourceId)
        const reportId = buildDynamicReportId(resourceKey, monitorType)
        const jobKey = `${resourceKey}_${monitorType}`
        db.pendingJobs[jobKey] = {
            sourceType,
            sceneType,
            sourceId,
            monitorType,
            reportId,
            readyAt: nowTs() + 1200
        }

        if (current) {
            current.status = 1
            current.statusText = statusText(1)
            resource[reportField] = current
        }
        db.resources[resourceKey] = resource
        patchVideoCache(db, resource)
        saveDb(db)
        return sendJson(res, { code: 0, msg: '已触发生成，请刷新查看状态', data: null })
    }

    const handleMonitorPositionStatistics = () => {
        const qc = getMonitorPositionItem(db, 'scriptQualityInspectionNum')
        const fidelity = getMonitorPositionItem(db, 'scriptFidelityMonitorNum')
        const inspect = getMonitorPositionItem(db, 'interactionPatrolNum')
        return sendJson(res, {
            code: 0,
            msg: 'success',
            data: {
                userId: 10001,
                tenantId: 20001,
                monitorPositions: [
                    {
                        code: 'subAccountCount',
                        name: '子账号数量',
                        scope: 'TENANT_SHARED',
                        scopeName: '租户共享',
                        totalQuantity: 5,
                        useQuantity: 2,
                        remainingQuantity: 3
                    },
                    {
                        code: 'anchorNum',
                        name: '抖音直播间/主播数',
                        scope: 'ACCOUNT_EXCLUSIVE',
                        scopeName: '账号独享',
                        totalQuantity: 10,
                        useQuantity: 4,
                        remainingQuantity: 6
                    },
                    {
                        code: 'scriptQualityInspectionNum',
                        name: '话术质检监控位',
                        scope: 'TENANT_SHARED',
                        scopeName: '租户共享',
                        totalQuantity: qc.total,
                        useQuantity: qc.use,
                        remainingQuantity: qc.remaining
                    },
                    {
                        code: 'scriptFidelityMonitorNum',
                        name: '话术还原度监控位',
                        scope: 'TENANT_SHARED',
                        scopeName: '租户共享',
                        totalQuantity: fidelity.total,
                        useQuantity: fidelity.use,
                        remainingQuantity: fidelity.remaining
                    },
                    {
                        code: 'interactionPatrolNum',
                        name: '互动巡检监控位',
                        scope: 'TENANT_SHARED',
                        scopeName: '租户共享',
                        totalQuantity: inspect.total,
                        useQuantity: inspect.use,
                        remainingQuantity: inspect.remaining
                    }
                ]
            }
        })
    }

    if (req.method === 'GET' && pathname === '/replay/userproperty/monitorPositionStatistics') {
        return handleMonitorPositionStatistics()
    }

    if (req.method === 'GET' && pathname === '/replay/script-monitor/anchorBasicConfig') {
        const secUid = query.secUid || query.anchorId || query.id || query.anchorUrlUserId
        const resp = buildAnchorBasicConfig(secUid)
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: resp })
    }

    if ((req.method === 'GET' || req.method === 'POST') && pathname === '/replay/script-monitor/reportStatus') {
        if (req.method === 'POST') {
            const body = await parseBody(req)
            return handleReportStatus(body?.sourceType, body?.sceneType, body?.sourceId)
        }
        return handleReportStatus(query.sourceType, query.sceneType, query.sourceId)
    }

    if (req.method === 'POST' && pathname === '/replay/script-monitor/batchReportStatus') {
        const body = await parseBody(req)
        const sources = Array.isArray(body.sources) ? body.sources : []
        const conf = ensureMockConfig(db)
        if (conf.strict) {
            for (const s of sources) {
                const st = Number(s?.sourceType ?? 0)
                const sc = Number(s?.sceneType ?? 0)
                if (st === 0 && sc !== 0) {
                    saveDb(db)
                    return sendJson(res, { code: 70013, msg: '录制视频仅支持复盘场景', data: null })
                }
                if (st === 1 && ![1, 2].includes(sc)) {
                    saveDb(db)
                    return sendJson(res, { code: 70013, msg: '上传文件仅支持视频分析/文案预审场景', data: null })
                }
            }
        }
        const items = sources.map((s) => {
            const resource = initResource(db, Number(s?.sourceType), Number(s?.sceneType), String(s?.sourceId ?? ''))
            patchVideoCache(db, resource)
            return resource
        })
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: { items } })
    }

    if (req.method === 'POST' && pathname === '/replay/script-monitor/setMonitorEnabled') {
        const body = await parseBody(req)
        const { secUid, monitorType, enabled } = body || {}
        if (!secUid || monitorType === undefined) {
            return sendJson(res, { code: 70013, msg: 'secUid 和 monitorType 必填', data: null })
        }
        if (![0, 1, 2].includes(Number(monitorType))) {
            return sendJson(res, { code: 70013, msg: 'monitorType 不合法', data: null })
        }
        return sendJson(res, { code: 0, msg: 'success', data: true })
    }

    if (req.method === 'POST' && pathname === '/replay/script-monitor/triggerReport') {
        const body = await parseBody(req)
        return handleTriggerReport(body)
    }

    if (req.method === 'POST' && pathname === '/replay/script-monitor/confirmRead') {
        const body = await parseBody(req)
        const reportId = Number(body.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const result = setReportRead(reportId, body.confirmRole)
        saveDb(db)
        return sendJson(res, { code: result.code, msg: result.msg, data: null })
    }

    if (req.method === 'GET' && pathname === '/replay/script-monitor/qualityReportDetail') {
        const reportId = Number(query.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const report = getReportById(reportId)
        if (!report) return sendJson(res, { code: 70011, msg: '无数据读取权限', data: null })
        report.confirmedRecords = normalizeConfirmedRecords(report.confirmedRecords)
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: report })
    }

    if (req.method === 'GET' && pathname === '/replay/script-monitor/fidelityReportDetail') {
        const reportId = Number(query.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const report = getReportById(reportId)
        if (!report) return sendJson(res, { code: 70011, msg: '无数据读取权限', data: null })
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: report })
    }

    if (req.method === 'GET' && pathname === '/replay/script-monitor/interactionReportDetail') {
        const reportId = Number(query.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const report = getReportById(reportId)
        if (!report) return sendJson(res, { code: 70011, msg: '无数据读取权限', data: null })
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: report })
    }

    if (req.method === 'GET' && pathname === '/replay/script-monitor/standardScriptDetail') {
        const anchorUrlUserId = Number(query.anchorUrlUserId || query.anchorId || 0)
        if (!anchorUrlUserId) return sendJson(res, { code: 70013, msg: 'anchorUrlUserId 不能为空', data: null })
        const standardScriptId = db.standardScriptByAnchorUrlUserId?.[String(anchorUrlUserId)] || null
        const script = standardScriptId ? db.standardScripts?.[String(standardScriptId)] : null
        if (!script) {
            saveDb(db)
            return sendJson(res, {
                code: 0,
                msg: 'success',
                data: {
                    hasScript: false,
                    standardScriptId: null,
                    anchorUrlUserId,
                    secUid: 'secUid_mock',
                    speechMode: null,
                    speechSpeed: 280,
                    cycleDurationMinutes: null,
                    referenceScript: null,
                    timeAxisScript: []
                }
            })
        }
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: Object.assign({ hasScript: true }, script) })
    }

    if (req.method === 'POST' && pathname === '/replay/script-monitor/generateStandardScript') {
        const body = await parseBody(req)
        const speechMode = Number(body.speechMode)
        const speechSpeed = Number(body.speechSpeed)
        const cycleDurationMinutes = Number(body.cycleDurationMinutes ?? 0)
        const referenceScript = String(body.referenceScript ?? '')
        if (!referenceScript) return sendJson(res, { code: 70013, msg: '参考直播脚本不能为空', data: null })
        if (!Number.isFinite(speechSpeed) || speechSpeed < 100 || speechSpeed > 500) {
            return sendJson(res, { code: 70013, msg: '语速范围 100-500 字/分钟', data: null })
        }
        if (speechMode === 1 && (!Number.isFinite(cycleDurationMinutes) || cycleDurationMinutes <= 0)) {
            return sendJson(res, { code: 70013, msg: '循环模式下必须填写循环话术预估时长', data: null })
        }
        const genId = buildDynamicReportId(`standard_${hashString(referenceScript)}`, 0)
        const timeAxisScript = [
            { timeRange: '00:00-05:00', title: '开场暖场', content: referenceScript.slice(0, 80) || '（Mock）开场欢迎词' },
            { timeRange: '05:00-10:00', title: '产品讲解', content: '（Mock）卖点讲解与优惠策略' },
            { timeRange: '10:00-15:00', title: '互动转化', content: '（Mock）答疑互动与转化话术' }
        ]
        return sendJson(res, {
            code: 0,
            msg: 'success',
            data: {
                speechMode,
                speechModeText: speechMode === 1 ? '循环模式' : '非循环模式',
                speechSpeed,
                cycleDurationMinutes: speechMode === 1 ? cycleDurationMinutes : null,
                referenceScript,
                timeAxisScript,
                scriptId: genId
            }
        })
    }

    if (req.method === 'POST' && pathname === '/replay/script-monitor/confirmStandardScript') {
        const body = await parseBody(req)
        const speechMode = Number(body.speechMode)
        const speechSpeed = Number(body.speechSpeed)
        const cycleDurationMinutes = Number(body.cycleDurationMinutes ?? 0)
        const referenceScript = String(body.referenceScript ?? '')
        const timeAxisScript = Array.isArray(body.timeAxisScript) ? body.timeAxisScript : []
        if (![0, 1].includes(speechMode)) {
            return sendJson(res, { code: 70013, msg: 'speechMode 不合法', data: null })
        }
        if (!Number.isFinite(speechSpeed) || speechSpeed < 100 || speechSpeed > 500) {
            return sendJson(res, { code: 70013, msg: '语速范围 100-500 字/分钟', data: null })
        }
        if (speechMode === 1 && (!Number.isFinite(cycleDurationMinutes) || cycleDurationMinutes <= 0)) {
            return sendJson(res, { code: 70013, msg: '循环模式下必须填写循环话术预估时长', data: null })
        }
        if (!referenceScript) {
            return sendJson(res, { code: 70013, msg: '参考直播脚本不能为空', data: null })
        }
        if (!timeAxisScript.length) {
            return sendJson(res, { code: 70013, msg: '标准直播稿内容不能为空', data: null })
        }
        for (const item of timeAxisScript) {
            if (!item?.timeRange || !item?.title || !item?.content) {
                return sendJson(res, { code: 70013, msg: '标准直播稿内容字段不完整', data: null })
            }
        }
        const anchorUrlUserId = body.anchorUrlUserId ? Number(body.anchorUrlUserId) : null
        const existingId = anchorUrlUserId ? db.standardScriptByAnchorUrlUserId?.[String(anchorUrlUserId)] : null
        const standardScriptId = existingId || buildDynamicReportId(`std_${anchorUrlUserId || 'new'}`, 1)
        const now = new Date().toISOString().slice(0, 19).replace('T', ' ')
        const record = {
            standardScriptId: Number(standardScriptId),
            anchorUrlUserId: anchorUrlUserId ? Number(anchorUrlUserId) : null,
            secUid: 'secUid_mock',
            speechMode,
            speechModeText: speechMode === 1 ? '循环模式' : '非循环模式',
            speechSpeed,
            cycleDurationMinutes: speechMode === 1 ? cycleDurationMinutes : null,
            referenceScript,
            timeAxisScript,
            createDate: existingId ? (db.standardScripts?.[String(standardScriptId)]?.createDate || now) : now,
            updateDate: now
        }
        db.standardScripts[String(standardScriptId)] = record
        if (anchorUrlUserId) {
            db.standardScriptByAnchorUrlUserId[String(anchorUrlUserId)] = String(standardScriptId)
        }
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: { standardScriptId: Number(standardScriptId), scriptId: Number(standardScriptId) } })
    }

    if (req.method === 'POST' && pathname === '/replay/anchorurl/addOrUpdateAnchor') {
        const body = await parseBody(req)
        const anchorUrlUserId = Number(body.anchorUrlUserId || body.anchorId || body.id || 0)
        if (!anchorUrlUserId) {
            return sendJson(res, { code: 70013, msg: 'anchorUrlUserId 不能为空', data: null })
        }
        const prev = db.anchors[String(anchorUrlUserId)] || {}
        const conf = ensureMockConfig(db)

        const checkFlag = (val, field) => {
            if (val === undefined || val === null) return null
            const n = Number(val)
            if (![0, 1].includes(n)) {
                return { code: 70013, msg: `${field} 不合法`, data: null }
            }
            return null
        }

        const errQc = checkFlag(body.isScriptQualityInspection, 'isScriptQualityInspection')
        if (errQc) return sendJson(res, errQc)
        const errFidelity = checkFlag(body.isScriptFidelityMonitor, 'isScriptFidelityMonitor')
        if (errFidelity) return sendJson(res, errFidelity)
        const errInspect = checkFlag(body.isInteractionPatrol, 'isInteractionPatrol')
        if (errInspect) return sendJson(res, errInspect)

        const nextAccountType = body.accountType ?? prev.accountType ?? 0
        const prevAiEnabled = Number(prev.isScriptQualityInspection) === 1 || Number(prev.isScriptFidelityMonitor) === 1 || Number(prev.isInteractionPatrol) === 1
        if (Number(prev.accountType ?? 0) === 0 && Number(nextAccountType) !== 0 && prevAiEnabled) {
            saveDb(db)
            return sendJson(res, { code: 70007, msg: '请先关闭AI话术监控功能后，再修改账号归属类型', data: null })
        }
        const nextFidelityFlag = body.isScriptFidelityMonitor ?? prev.isScriptFidelityMonitor ?? 0
        const nextStandardScriptId = body.standardScriptId ?? prev.standardScriptId ?? null
        if (Number(nextFidelityFlag) === 1) {
            if (!nextStandardScriptId) {
                saveDb(db)
                return sendJson(res, { code: 70005, msg: '请先确认标准直播稿', data: null })
            }
            const exists = !!db.standardScripts?.[String(nextStandardScriptId)]
            if (!exists) {
                saveDb(db)
                return sendJson(res, { code: 70006, msg: '标准直播稿无效，请重新确认', data: null })
            }
        }
        const merged = Object.assign({}, prev, body, { anchorUrlUserId })

        const nextQc = Number(merged.isScriptQualityInspection ?? 0)
        const nextFidelity = Number(merged.isScriptFidelityMonitor ?? 0)
        const nextInspect = Number(merged.isInteractionPatrol ?? 0)
        const prevQc = Number(prev.isScriptQualityInspection ?? 0)
        const prevFidelity = Number(prev.isScriptFidelityMonitor ?? 0)
        const prevInspect = Number(prev.isInteractionPatrol ?? 0)

        if (conf.strict) {
            if (Number(merged.accountType ?? nextAccountType) !== 0 && (nextQc === 1 || nextFidelity === 1 || nextInspect === 1)) {
                saveDb(db)
                return sendJson(res, { code: 70004, msg: '仅自有账号支持AI话术监控功能', data: null })
            }

            const enableAny = (prevQc !== 1 && nextQc === 1) || (prevFidelity !== 1 && nextFidelity === 1) || (prevInspect !== 1 && nextInspect === 1)
            if (enableAny) {
                if (!conf.packageSupported) {
                    saveDb(db)
                    return sendJson(res, strictError(70003, '当前套餐不支持，请升级'))
                }
                if (Number(conf.tokenAmount ?? 0) < 100000) {
                    saveDb(db)
                    return sendJson(res, strictError(70001, '您的算力数量不足，请联系产品顾问购买。'))
                }
                const checks = [
                    { prev: prevQc, next: nextQc, code: 'scriptQualityInspectionNum', name: '话术质检' },
                    { prev: prevFidelity, next: nextFidelity, code: 'scriptFidelityMonitorNum', name: '话术还原度' },
                    { prev: prevInspect, next: nextInspect, code: 'interactionPatrolNum', name: '互动巡检' }
                ]
                for (const item of checks) {
                    if (item.prev !== 1 && item.next === 1) {
                        const mp = getMonitorPositionItem(db, item.code)
                        if (mp.remaining <= 0) {
                            saveDb(db)
                            return sendJson(res, strictError(70002, `${item.name}授权数量不足，请联系产品顾问购买`))
                        }
                    }
                }
            }
        }

        db.anchors[String(anchorUrlUserId)] = merged
        if (nextStandardScriptId) {
            db.standardScriptByAnchorUrlUserId[String(anchorUrlUserId)] = String(nextStandardScriptId)
        }

        const applyOccupy = (prevVal, nextVal, code) => {
            if (prevVal !== 1 && nextVal === 1) updateMonitorPositionUse(db, code, 1)
            if (prevVal === 1 && nextVal !== 1) updateMonitorPositionUse(db, code, -1)
        }
        applyOccupy(prevQc, nextQc, 'scriptQualityInspectionNum')
        applyOccupy(prevFidelity, nextFidelity, 'scriptFidelityMonitorNum')
        applyOccupy(prevInspect, nextInspect, 'interactionPatrolNum')

        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: { anchorUrlUserId, anchorId: merged.anchorId || anchorUrlUserId } })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/getAnchorBasicConfig') {
        const anchorId = query.anchorId || query.anchorID || query.id
        const resp = buildAnchorBasicConfig(anchorId)
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: resp })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/getScriptMonitorStatus') {
        const videoId = query.videoId
        if (!videoId) return sendJson(res, { code: 70013, msg: 'videoId 不能为空', data: null })
        const resource = initResource(db, 0, 0, String(videoId))
        patchVideoCache(db, resource)
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: initVideo(db, String(videoId)) })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/triggerScriptMonitorReport') {
        const videoId = query.videoId
        const monitorType = Number(query.monitorType)
        return handleTriggerReport({ sourceType: 0, sceneType: 0, sourceId: String(videoId || ''), monitorType })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/confirmScriptMonitorReportRead') {
        const reportId = Number(query.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const result = setReportRead(reportId)
        saveDb(db)
        return sendJson(res, { code: result.code, msg: result.msg, data: null })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/getQualityInspectionReport') {
        const reportId = Number(query.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const report = getReportById(reportId)
        if (!report) return sendJson(res, { code: 70011, msg: '无数据读取权限', data: null })
        report.confirmedRecords = normalizeConfirmedRecords(report.confirmedRecords)
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: report })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/getInteractionPatrolReport') {
        const reportId = Number(query.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const report = getReportById(reportId)
        if (!report) return sendJson(res, { code: 70011, msg: '无数据读取权限', data: null })
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: report })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/getFidelityMonitorReport') {
        const reportId = Number(query.reportId)
        if (!reportId) return sendJson(res, { code: 70013, msg: 'reportId 不能为空', data: null })
        const report = getReportById(reportId)
        if (!report) return sendJson(res, { code: 70011, msg: '无数据读取权限', data: null })
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: report })
    }

    if (req.method === 'GET' && pathname === '/replay/openapi/v2100/getStandardScript') {
        const anchorUrlUserId = Number(query.anchorUrlUserId || query.anchorId || query.id || 0)
        if (!anchorUrlUserId) return sendJson(res, { code: 70013, msg: 'anchorId 不能为空', data: null })
        const standardScriptId = db.standardScriptByAnchorUrlUserId?.[String(anchorUrlUserId)] || null
        const script = standardScriptId ? db.standardScripts?.[String(standardScriptId)] : null
        if (!script) {
            saveDb(db)
            return sendJson(res, { code: 0, msg: 'success', data: { hasScript: false } })
        }
        saveDb(db)
        return sendJson(res, { code: 0, msg: 'success', data: Object.assign({ hasScript: true, scriptId: Number(standardScriptId) }, script) })
    }

    if (req.method === 'POST' && pathname === '/replay/openapi/v2100/generateStandardScript') {
        const body = await parseBody(req)
        const speechMode = Number(body.speechMode)
        const speechSpeed = Number(body.speechSpeed)
        const cycleDurationMinutes = Number(body.cycleDurationMinutes ?? 0)
        const referenceScript = String(body.referenceScript ?? '')
        if (!referenceScript) return sendJson(res, { code: 70013, msg: '参考直播脚本不能为空', data: null })
        if (!Number.isFinite(speechSpeed) || speechSpeed < 100 || speechSpeed > 500) {
            return sendJson(res, { code: 70013, msg: '语速范围 100-500 字/分钟', data: null })
        }
        if (speechMode === 1 && (!Number.isFinite(cycleDurationMinutes) || cycleDurationMinutes <= 0)) {
            return sendJson(res, { code: 70013, msg: '循环模式下必须填写循环话术预估时长', data: null })
        }
        const genId = buildDynamicReportId(`standard_${hashString(referenceScript)}`, 0)
        const timeAxisScript = [
            { timeRange: '00:00-05:00', title: '开场暖场', content: referenceScript.slice(0, 80) || '（Mock）开场欢迎词' },
            { timeRange: '05:00-10:00', title: '产品讲解', content: '（Mock）卖点讲解与优惠策略' },
            { timeRange: '10:00-15:00', title: '互动转化', content: '（Mock）答疑互动与转化话术' }
        ]
        saveDb(db)
        return sendJson(res, {
            code: 0,
            msg: 'success',
            data: {
                speechMode,
                speechSpeed,
                cycleDurationMinutes: speechMode === 1 ? cycleDurationMinutes : null,
                referenceScript,
                timeAxisScript,
                scriptId: genId
            }
        })
    }

    saveDb(db)
    return sendJson(res, { code: 404, msg: 'not found', data: null }, 404)
}

const server = http.createServer(async (req, res) => {
    const parsed = url.parse(req.url, true)
    const pathname = parsed.pathname || '/'
    if (req.method === 'OPTIONS') {
        res.statusCode = 204
        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Token, source, webVersion, sign, timestamp, nonce')
        return res.end()
    }

    if (req.method === 'GET' && pathname === '/mock/anchorvideo/anchorvideoGetpage') {
        return sendJson(res, mockData.anchorvideoGetpage)
    }

    if (pathname.startsWith('/replay/')) {
        return handleReplayApi(req, res, pathname, parsed.query || {})
    }

    return sendJson(res, { code: 404, msg: 'not found', data: null }, 404)
})

server.listen(PORT, () => {
    console.log(`mock server running at http://localhost:${PORT}`)
})

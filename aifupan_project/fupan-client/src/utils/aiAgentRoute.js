/**
 * @file AI 智能体默认落地页工具。
 * @description 负责按用户维度缓存直播间主播数量，并统一维护 AI 智能体落地页与工作台跳转参数规则。
 * 约定：AI 工作台统一走 `/sso#...` Hash 参数格式；`panel=influencer` 表示短视频板块，`panel=room` 表示直播间板块。
 */

export const AI_AGENT_ROUTE_PATH = '/aiAgent'
export const LIVE_ROOM_ROUTE_PATH = '/dataAnalysis'
export const AI_WORKBENCH_PANEL_QUERY_KEY = 'panel'
export const AI_WORKBENCH_PANELS = {
    INFLUENCER: 'influencer',
    ROOM: 'room'
}

const STORAGE_KEY = 'ai_agent_anchor_count_by_user_v1'

/**
 * @description 读取主播数量缓存映射。
 * @returns {Object<string, number>}
 */
function getAnchorCountCacheMap() {
    try {
        const cache = localStorage.getItem(STORAGE_KEY)
        const parsed = cache ? JSON.parse(cache) : {}
        return parsed && typeof parsed === 'object' ? parsed : {}
    } catch (e) {
        return {}
    }
}

/**
 * @description 持久化主播数量缓存映射。
 * @param {Object<string, number>} cacheMap 用户主播数量映射
 * @returns {void}
 */
function setAnchorCountCacheMap(cacheMap = {}) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cacheMap || {}))
}

/**
 * @description 获取指定用户缓存的主播数量。
 * @param {string|number} userId 用户 ID
 * @returns {number}
 */
export function getUserAnchorCount(userId) {
    if (userId === '' || userId === null || typeof userId === 'undefined') {
        return 0
    }
    const cacheMap = getAnchorCountCacheMap()
    return Number(cacheMap[String(userId)] || 0)
}

/**
 * @description 更新指定用户缓存的主播数量。
 * @param {string|number} userId 用户 ID
 * @param {number} count 主播数量
 * @returns {void}
 */
export function setUserAnchorCount(userId, count) {
    if (userId === '' || userId === null || typeof userId === 'undefined') {
        return
    }
    const normalizedCount = Math.max(Number(count) || 0, 0)
    const cacheMap = getAnchorCountCacheMap()
    cacheMap[String(userId)] = normalizedCount
    setAnchorCountCacheMap(cacheMap)
}

/**
 * @description 判断登录后默认是否应优先进入 AI 智能体介绍页。
 * @param {string|number} userId 用户 ID
 * @returns {boolean}
 */
export function shouldRedirectToAiAgent(userId) {
    return getUserAnchorCount(userId) >= 1
}

/**
 * @description 获取用户登录后的默认落地页。
 * @param {string|number} userId 用户 ID
 * @returns {string}
 */
export function getDefaultLandingPath(userId) {
    return shouldRedirectToAiAgent(userId) ? AI_AGENT_ROUTE_PATH : LIVE_ROOM_ROUTE_PATH
}

/**
 * @description 确保 AI 工作台地址统一落到 `/sso`，只有该入口才会校验 token。
 * @param {string} basePath 原始基础地址
 * @returns {string}
 */
function ensureAiWorkbenchSsoPath(basePath = '') {
    const normalized = String(basePath || '').trim()
    if (!normalized) {
        return '/sso'
    }
    if (/\/sso\/?$/i.test(normalized)) {
        return normalized.replace(/\/+$/, '')
    }
    return `${normalized.replace(/\/+$/, '')}/sso`
}

/**
 * @description 规范化 AI 工作台地址，统一输出 `/sso#...` Hash 参数格式。
 * 约定：
 * 1) 只有 `/sso` 入口才校验 token，因此所有工作台跳转都强制改写到 `/sso`；
 * 2) 短视频板块使用 `panel=influencer`，直播间板块使用 `panel=room`；
 * 3) 直播间场景使用 `secUid/videoId/liveDate/cue`；
 * 4) 达人场景使用 `influencerId/clipId`。
 * @param {string} urlTemplate 后端配置的 AI 工作台 URL 模板
 * @param {{ token?: string, includeToken?: boolean, panel?: string, secUid?: string, videoId?: string, influencerId?: string, clipId?: string, liveDate?: string, cue?: string|number }} options URL 处理选项
 * @returns {string}
 */
export function normalizeAiWorkbenchUrl(urlTemplate, options = {}) {
    const {
        token = '',
        includeToken = true,
        panel = '',
        secUid = '',
        videoId = '',
        influencerId = '',
        clipId = '',
        liveDate = '',
        cue = ''
    } = options
    const rawUrl = String(urlTemplate || '').trim()
    if (!rawUrl) {
        return ''
    }
    const [urlWithoutHash = '', hashFragment = ''] = rawUrl.split('#')
    const [basePath = '', search = ''] = urlWithoutHash.split('?')
    const normalizedBasePath = ensureAiWorkbenchSsoPath(
        basePath
            .replace(/\{token\}/g, '')
            .replace(/\{secUid\}/g, '')
            .replace(/\{videoId\}/g, '')
            .replace(/\{influencerId\}/g, '')
            .replace(/\{clipId\}/g, '')
            .replace(/\{liveDate\}/g, '')
            .replace(/\{cue\}/g, '')
    )
    const hashEntries = []
    const appendedKeys = new Set()

    /**
     * @description 向 Hash 参数列表追加值，并保持业务指定的顺序与去空逻辑。
     * @param {string} key 参数名
     * @param {string|number} value 参数值
     * @param {boolean} force 是否强制覆盖已有值
     * @returns {void}
     */
    const appendHashEntry = (key, value, force = false) => {
        const safeKey = String(key || '').trim()
        const safeValue = value === null || typeof value === 'undefined' ? '' : String(value).trim()
        if (!safeKey || !safeValue) {
            return
        }
        if (appendedKeys.has(safeKey)) {
            if (!force) {
                return
            }
            const targetIndex = hashEntries.findIndex(item => item.key === safeKey)
            if (targetIndex > -1) {
                hashEntries.splice(targetIndex, 1, { key: safeKey, value: safeValue })
                return
            }
        }
        hashEntries.push({ key: safeKey, value: safeValue })
        appendedKeys.add(safeKey)
    }

    // 兼容旧模板仍把参数放在 query/hash 里的情况，统一迁移到新的 `/sso#...` 形式。
    ;[search, hashFragment].forEach((segmentText = '') => {
        segmentText.split('&').filter(Boolean).forEach((segment) => {
            const [rawKey = '', ...restValue] = segment.split('=')
            const key = String(rawKey || '').trim()
            const value = restValue.join('=')
            if (!key) {
                return
            }
            if (value.includes('{token}')) {
                if (!includeToken) {
                    return
                }
                appendHashEntry(key, value.replace(/\{token\}/g, token), true)
                return
            }
            if (['{secUid}', '{videoId}', '{influencerId}', '{clipId}', '{liveDate}', '{cue}'].includes(value)) {
                return
            }
            appendHashEntry(key, value, true)
        })
    })

    if (includeToken) {
        appendHashEntry('token', token, true)
    }
    appendHashEntry('secUid', secUid, true)
    appendHashEntry('videoId', videoId, true)
    appendHashEntry('influencerId', influencerId, true)
    appendHashEntry('clipId', clipId, true)
    appendHashEntry('liveDate', liveDate, true)
    appendHashEntry('cue', cue, true)
    appendHashEntry(AI_WORKBENCH_PANEL_QUERY_KEY, panel, true)

    const hashString = hashEntries
        .map(({ key, value }) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join('&')
    return hashString ? `${normalizedBasePath}#${hashString}` : normalizedBasePath
}

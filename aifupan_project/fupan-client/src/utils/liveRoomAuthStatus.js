import httpBack from '@/utils/request-api-back'

function normalizeStatusValue(value) {
    if (value === undefined || value === null || value === '') return 0
    const numberValue = Number(value)
    return Number.isFinite(numberValue) ? numberValue : 0
}

function normalizeAuthStatusPayload(raw = {}, secUid = '') {
    const anchorUrlSecUid = raw?.anchorUrlSecUid ?? raw?.SecUid ?? raw?.secUid ?? secUid ?? ''
    const authJlbyStatus = normalizeStatusValue(raw?.authJlbyStatus ?? raw?.juliangAuthStatus)
    const authQcStatus = normalizeStatusValue(raw?.authQcStatus ?? raw?.qianchuanAuthStatus)
    const authChannelStatus = normalizeStatusValue(raw?.authChannelStatus)
    const authLifeStatus = normalizeStatusValue(raw?.authLifeStatus ?? raw?.lifeAuthStatus)

    return {
        anchorUrlSecUid,
        authJlbyStatus,
        authQcStatus,
        authChannelStatus,
        authLifeStatus,
        authJlbyStatusTime: raw?.authJlbyStatusTime ?? raw?.juliangAuthStatusTime ?? null,
        authQcStatusTime: raw?.authQcStatusTime ?? raw?.qianchuanAuthStatusTime ?? null,
        authLifeStatusTime: raw?.authLifeStatusTime ?? raw?.lifeAuthStatusTime ?? null,
        juliangAuthStatus: authJlbyStatus,
        qianchuanAuthStatus: authQcStatus,
        lifeAuthStatus: authLifeStatus
    }
}

export async function getLiveRoomAuthStatus(liveRoomId) {
    const secUid = liveRoomId === undefined || liveRoomId === null ? '' : String(liveRoomId).trim()
    if (!secUid) {
        return {
            code: -1,
            msg: '缺少直播间ID（secUid）',
            data: null,
            authStatus: normalizeAuthStatusPayload({}, '')
        }
    }

    if (!httpBack?.words?.getAuthStatus) {
        return {
            code: -1,
            msg: '未配置 getAuthStatus 接口（$httpBack.words.getAuthStatus）',
            data: null,
            authStatus: normalizeAuthStatusPayload({}, secUid)
        }
    }

    try {
        const res = await httpBack.words.getAuthStatus({ secUid })
        if (Number(res?.code) === 0) {
            return {
                code: 0,
                msg: res?.msg || '',
                data: res?.data || null,
                authStatus: normalizeAuthStatusPayload(res?.data || {}, secUid)
            }
        }

        if (Number(res?.code) === 30000) {
            return {
                code: 30000,
                msg: res?.msg || '记录不存在',
                data: null,
                authStatus: normalizeAuthStatusPayload({}, secUid)
            }
        }

        return {
            code: res?.code ?? -1,
            msg: res?.msg || '获取授权状态失败',
            data: res?.data ?? null,
            authStatus: null
        }
    } catch (e) {
        return {
            code: -1,
            msg: '获取授权状态失败',
            data: null,
            authStatus: null
        }
    }
}


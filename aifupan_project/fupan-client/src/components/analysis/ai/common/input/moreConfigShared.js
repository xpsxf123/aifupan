/**
 * @file 更多配置共享数据与工具。
 * @description 统一维护“更多配置”的选项定义、默认值生成与数据归一化逻辑，供页面级配置与自建问题预设配置复用。
 */
import myUtils from "@/utils/utils";
import { PLATFORM_TYPE_ENUM } from '@/enum';

/**
 * @description 额外条件分组，复用段落显示输入条件的字段定义。
 */
export const EXTRA_ITEMS = [
    {label: '开始时间', prop: 'startTime', default: 0},
    {label: '自然时间', prop: 'natureTime', default: 0},
    {label: '在线人数', prop: 'onlineNum', default: 0},
    {label: '语速', prop: 'analysisChar', default: 0},
    {label: '弹幕数量', prop: 'barrageNum', default: 0, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
    {label: '成交数量', prop: 'dealNum', default: 0},
    {label: '互动率', prop: 'interactionRate', default: 0, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
    {label: '成交率', prop: 'dealRate', default: 0, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
    {label: '销售额', prop: 'sales', default: 0, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
    {label: 'UV价值', prop: 'uv', default: 0, hides: [PLATFORM_TYPE_ENUM.kuaishou]},
]

let remoteDynamicConfigItems = null;

export function getBasicItems() {
    if (!Array.isArray(remoteDynamicConfigItems)) {
        return []
    }
    return remoteDynamicConfigItems.filter(item => Number(item?.category ?? 0) === 1)
}

export function getDynamicConfigItems() {
    if (!Array.isArray(remoteDynamicConfigItems)) {
        return []
    }
    return remoteDynamicConfigItems.filter(item => Number(item?.category ?? 0) !== 1)
}

export function setDynamicConfigItems(items = []) {
    remoteDynamicConfigItems = Array.isArray(items) ? items : []
}

export async function ensureDynamicConfigItems(httpBack, options = {}) {
    const { force = false } = options || {}
    if (!force && Array.isArray(remoteDynamicConfigItems)) {
        return remoteDynamicConfigItems
    }
    if (!httpBack?.placeholder?.listForClient) {
        return null
    }
    try {
        const res = await httpBack.placeholder.listForClient({})
        const list = Array.isArray(res?.data) ? res.data : []
        remoteDynamicConfigItems = list
            .filter(item => Number(item?.isFrontendShow ?? 1) === 1 && Number(item?.status ?? 0) === 0)
            .sort((a, b) => Number(a?.sort ?? 0) - Number(b?.sort ?? 0))
            .map(item => ({
                label: item?.name || item?.abbreviation || item?.placeholderKey || '',
                prop: item?.placeholderKey || String(item?.id || ''),
                default: Number(item?.isDefaultChecked ?? 0) ? 1 : 0,
                category: Number(item?.category ?? 0),
                description: item?.description || '',
                raw: item
            }))
        return remoteDynamicConfigItems
    } catch (e) {
        return null
    }
}

/**
 * @description 获取当前资源平台类型。
 * @param {Object} sentenceMarkData 资源上下文
 * @returns {number|string|undefined}
 */
export function getPlatformType(sentenceMarkData = {}) {
    return sentenceMarkData?.videoInfo?.PlatformType || sentenceMarkData?.fileInfo?.platformType
}

/**
 * @description 判断当前资源是否为快手平台。
 * @param {Object} sentenceMarkData 资源上下文
 * @returns {boolean}
 */
export function isKuaishouPlatform(sentenceMarkData = {}) {
    return getPlatformType(sentenceMarkData) == PLATFORM_TYPE_ENUM.kuaishou
}

/**
 * @description 生成基础数据默认值。
 * @returns {Object}
 */
export function getDefaultBasicData() {
    const list = getBasicItems()
    if (!list.length) {
        return {}
    }
    const result = list.reduce((acc, item) => {
        acc[item.prop] = item.default ?? 0
        return acc
    }, {})
    const hasChecked = Object.values(result).some(item => !!item)
    if (hasChecked) {
        return result
    }
    const firstProp = list?.[0]?.prop
    if (firstProp) {
        result[firstProp] = 1
    }
    return result
}

/**
 * @description 生成扩展配置默认值。
 * @returns {Object}
 */
export function getDefaultDynamicConfigs() {
    return getDynamicConfigItems().reduce((result, item) => {
        result[item.prop] = item.default ?? 0
        return result
    }, {})
}

/**
 * @description 计算成交量是否具备展示条件。
 * @param {Object} sentenceMarkData 资源上下文
 * @returns {boolean}
 */
function showTotalDeal(sentenceMarkData = {}) {
    const {purchaseCountStart, purchaseCountEnd} = sentenceMarkData || {}
    return myUtils.isGreaterThanZero(purchaseCountStart) || myUtils.isGreaterThanZero(purchaseCountEnd)
}

/**
 * @description 计算 UV 价值是否具备展示条件。
 * @param {Object} sentenceMarkData 资源上下文
 * @returns {boolean}
 */
function showUV(sentenceMarkData = {}) {
    const {uvValueStart, uvValueEnd} = sentenceMarkData || {}
    return myUtils.isGreaterThanZero(uvValueStart) || myUtils.isGreaterThanZero(uvValueEnd)
}

/**
 * @description 计算销售额是否具备展示条件。
 * @param {Object} sentenceMarkData 资源上下文
 * @returns {boolean}
 */
function showSales(sentenceMarkData = {}) {
    const {volumeStart, volumeEnd} = sentenceMarkData || {}
    return myUtils.isGreaterThanZero(volumeStart) || myUtils.isGreaterThanZero(volumeEnd)
}

/**
 * @description 获取默认勾选规则。
 * @param {Object} options 上下文配置
 * @param {string} options.aiCueType AI 类型
 * @param {boolean} options.isCompare 是否对比场景
 * @returns {string[]}
 */
export function getDefaultRules({aiCueType = '', isCompare = false} = {}) {
    if (isCompare) {
        return ['startTime', 'onlineNum']
    }
    const defaultMap = {
        assistant: ['startTime', 'onlineNum', 'barrageNum', 'dealNum', 'interactionRate', 'dealRate', 'sales', 'uv'],
        violation: ['natureTime'],
        textAssistant: ['startTime', 'interactionRate']
    }
    return defaultMap[aiCueType] || []
}

/**
 * @description 生成可用的额外条件项，处理隐藏与禁用规则。
 * @param {Object} options 上下文配置
 * @param {Object} options.sentenceMarkData 资源上下文
 * @param {Object} options.hide 外部隐藏规则
 * @param {string} options.aiCueType AI 类型
 * @param {boolean} options.isCompare 是否对比场景
 * @returns {Object[]}
 */
export function getAvailableExtraItems({
    sentenceMarkData = {},
    hide = {},
    aiCueType = '',
    isCompare = false
} = {}) {
    const isKuaishou = isKuaishouPlatform(sentenceMarkData)
    const totalBarrageNum = sentenceMarkData?.totalBarrageNum
    const interactionPercent = sentenceMarkData?.interactionPercent
    const isBuyInData = sentenceMarkData?.dataSourceType === 1
    const defaultRules = getDefaultRules({aiCueType, isCompare})
    const disabledRules = {
        barrageNum: () => totalBarrageNum <= 0,
        dealNum: () => !(showTotalDeal(sentenceMarkData) && isBuyInData),
        interactionRate: () => totalBarrageNum <= 0 || !interactionPercent,
        dealRate: () => !(showTotalDeal(sentenceMarkData) && isBuyInData),
        sales: () => !(showSales(sentenceMarkData) && isBuyInData),
        uv: () => !(showUV(sentenceMarkData) && isBuyInData),
    }
    return EXTRA_ITEMS.reduce((result, item) => {
        const nextItem = {
            ...item
        }
        const isPlatformHidden = nextItem.hides?.length ? nextItem.hides.includes(PLATFORM_TYPE_ENUM.kuaishou) && isKuaishou : false
        nextItem.hide = typeof hide[nextItem.prop] !== 'undefined' ? !!hide[nextItem.prop] : isPlatformHidden
        nextItem.disabled = disabledRules[nextItem.prop] ? disabledRules[nextItem.prop]() : !!nextItem.disabled
        nextItem.defaultValue = defaultRules?.includes(nextItem.prop) && !nextItem.disabled
            ? 1
            : (typeof nextItem.default !== 'undefined' ? nextItem.default : (nextItem.disabled ? 0 : 1))
        result.push(nextItem)
        return result
    }, [])
}

/**
 * @description 生成额外条件默认值。
 * @param {Object} options 上下文配置
 * @returns {Object}
 */
export function getDefaultExtraConditions(options = {}) {
    return getAvailableExtraItems(options).reduce((result, item) => {
        result[item.prop] = item.defaultValue ?? 0
        return result
    }, {})
}

/**
 * @description 归一化更多配置值，确保结构完整稳定。
 * @param {Object} value 外部传入配置
 * @param {Object} options 上下文配置
 * @returns {Object}
 */
export function normalizeMoreConfigValue(value = {}, options = {}) {
    const defaultBasicData = getDefaultBasicData()
    const defaultExtraConditions = getDefaultExtraConditions(options)
    const defaultDynamicConfigs = getDefaultDynamicConfigs()
    return {
        basicData: {
            ...defaultBasicData,
            ...(value?.basicData || {})
        },
        extraConditions: {
            ...defaultExtraConditions,
            ...(value?.extraConditions || {})
        },
        dynamicConfigs: {
            ...defaultDynamicConfigs,
            ...(value?.dynamicConfigs || {})
        }
    }
}

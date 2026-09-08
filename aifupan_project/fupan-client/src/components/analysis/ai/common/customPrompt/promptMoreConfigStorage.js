/**
 * @file 自建问题预设配置本地存储工具。
 * @description 在后端未扩展字段前，先用本地缓存为自建问题挂载独立的“更多配置”预设，并在列表与发送阶段回填。
 */
import { normalizeMoreConfigValue } from '../input/moreConfigShared';

const STORAGE_KEY = 'ai_custom_prompt_more_config_v1'

/**
 * @description 读取本地预设配置仓库。
 * @returns {Object}
 */
function loadStore() {
    try {
        const cache = localStorage.getItem(STORAGE_KEY)
        const parsed = cache ? JSON.parse(cache) : {}
        return parsed && typeof parsed === 'object' ? parsed : {}
    } catch (error) {
        return {}
    }
}

/**
 * @description 写回本地预设配置仓库。
 * @param {Object} store 仓库对象
 * @returns {void}
 */
function saveStore(store = {}) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store || {}))
}

/**
 * @description 根据提示词 id 获取本地预设配置。
 * @param {string|number} promptId 提示词 id
 * @param {Object} options 配置归一化上下文
 * @returns {Object}
 */
export function getPromptMoreConfig(promptId, options = {}) {
    if (!promptId && promptId !== 0) {
        return normalizeMoreConfigValue({}, options)
    }
    const store = loadStore()
    return normalizeMoreConfigValue(store[String(promptId)] || {}, options)
}

/**
 * @description 保存提示词预设配置。
 * @param {string|number} promptId 提示词 id
 * @param {Object} config 更多配置
 * @param {Object} options 配置归一化上下文
 * @returns {void}
 */
export function savePromptMoreConfig(promptId, config = {}, options = {}) {
    if (!promptId && promptId !== 0) {
        return
    }
    const store = loadStore()
    store[String(promptId)] = normalizeMoreConfigValue(config, options)
    saveStore(store)
}

/**
 * @description 删除提示词预设配置。
 * @param {string|number} promptId 提示词 id
 * @returns {void}
 */
export function removePromptMoreConfig(promptId) {
    if (!promptId && promptId !== 0) {
        return
    }
    const store = loadStore()
    delete store[String(promptId)]
    saveStore(store)
}

/**
 * @description 将本地预设配置合并到列表数据中，便于编辑和发送时直接读取。
 * @param {Object[]} list 提示词列表
 * @param {Object} options 配置归一化上下文
 * @returns {Object[]}
 */
export function mergePromptMoreConfigList(list = [], options = {}) {
    return (list || []).map(item => {
        return {
            ...item,
            promptMoreConfig: getPromptMoreConfig(item?.id, options)
        }
    })
}

/**
 * @description 尝试从响应或刷新后的列表中解析提示词 id。
 * @param {Object} options 参数集合
 * @param {Object} options.response 保存接口响应
 * @param {Object} options.form 当前表单
 * @param {Object[]} options.list 最新提示词列表
 * @returns {string|number|undefined}
 */
export function resolvePromptId({response, form = {}, list = []} = {}) {
    const responseId = response?.data?.id ?? response?.data?.Id ?? response?.data
    if (responseId || responseId === 0) {
        return responseId
    }
    if (form?.id || form?.id === 0) {
        return form.id
    }
    const matchItem = (list || []).find(item => {
        return item?.promptTitle === form?.promptTitle
            && item?.promptContent === form?.promptContent
            && String(item?.promptSort ?? '') === String(form?.promptSort ?? '')
    })
    return matchItem?.id
}

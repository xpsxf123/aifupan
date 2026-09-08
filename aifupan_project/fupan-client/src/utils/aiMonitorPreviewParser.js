/**
 * @description AI 监控列表预览容错解析工具。
 * 目标：
 * 1. 支持两种容错模式：结构化统计提取 / 文案纠错；
 * 2. 结构化模式用于话术质检等“标签 + 数字”型摘要；
 * 3. 文案模式用于话术还原度、弹幕巡检等一句话摘要，重点是删除/补救坏标签并保留正文；
 * 4. 若解析阶段出现异常，则返回 error 状态，由列表单元格回退为“查看详情”。
 */

const METRIC_LABEL_RE = /([A-Za-z\u4e00-\u9fa5][A-Za-z\u4e00-\u9fa5]{1,29})\s*[:：]?\s*(-?\d+(?:\.\d+)?)/g;
const SUSPICIOUS_MARKUP_RE = /<(?:[^>]*$|\/?[a-z][\w-]*)|(?:font|span|div|strong|em|br|img|p)\s*>?|[\w-]+\s*=\s*['"][^'"]*$/i;

/**
 * @description 将预览内容统一转为字符串，兼容数组/对象场景。
 * @param {any} content 预览内容
 * @returns {string}
 */
export function normalizePreviewRawContent(content) {
    if (content === null || content === undefined) return '';
    if (Array.isArray(content)) {
        return content.map((item) => normalizePreviewRawContent(item)).join('');
    }
    if (typeof content === 'object') {
        return String(
            content?.summaryText
            || content?.summary
            || content?.text
            || content?.content
            || ''
        );
    }
    return String(content);
}

/**
 * @description 清洗残缺标签、裸属性串、标签碎片等噪音。
 * @param {string} text 原始文本
 * @returns {string}
 */
export function denoisePreviewMarkup(text = '') {
    return String(text || '')
        .replace(/[\u2018\u2019]/g, "'")
        .replace(/[\u201C\u201D]/g, '"')
        .replace(/<[^<>]*>/g, ' ')
        .replace(/<\s*\/?\s*[a-z][\w-]*(\s+[\w-]+\s*=\s*("[^"]*"|'[^']*'|[\w#%-]+))*/gi, ' ')
        .replace(/[\w-]+\s*=\s*("[^"]*"|'[^']*'|[\w#%-]+)\s*\/?\s*>?/g, ' ')
        .replace(/\b(font|span|div|strong|em|br|hr|img|b|i|u|p)\b\s*\/?\s*>?/gi, ' ')
        .replace(/[<>\/\\'"“”‘’*`_]/g, ' ');
}

/**
 * @description 针对文案型内容做“坏标签纠错”，目标是去掉错误标签影响，但尽量保住正文可读内容。
 * 处理策略：
 * 1. 脚本/样式块直接删除；
 * 2. 换行类标签转为换行，避免句子硬粘连；
 * 3. 完整/残缺/裸标签与属性串统一移除；
 * 4. 保留普通文本、百分比、数字与中文语义。
 * @param {string} text 原始文本
 * @returns {string}
 */
export function repairPreviewTextMarkup(text = '') {
    return String(text || '')
        .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, ' ')
        .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, ' ')
        .replace(/<\s*br\s*\/?\s*>/gi, '\n')
        .replace(/<\s*\/\s*(p|div|li|h[1-6])\s*>/gi, '\n')
        .replace(/<[^<>]*>/g, '')
        .replace(/<\s*\/?\s*[a-z][\w-]*(\s+[\w-]+\s*=\s*("[^"]*"|'[^']*'|[\w#%-]+))*/gi, '')
        .replace(/[\w-]+\s*=\s*("[^"]*"|'[^']*'|[\w#%-]+)\s*\/?\s*>?/g, '')
        .replace(/\b(font|span|div|strong|em|br|hr|img|b|i|u|p)\b\s*\/?\s*>?/gi, '')
        .replace(/[<>]/g, '');
}

/**
 * @description 将 HTML 文本转为纯文本，并对异常环境做正则回退。
 * @param {string} text 原始文本
 * @returns {string}
 */
export function stripPreviewTags(text = '') {
    const raw = String(text || '');
    if (typeof document !== 'undefined') {
        const div = document.createElement('div');
        div.innerHTML = raw;
        return div.textContent || '';
    }
    return raw.replace(/<[^>]*>/g, ' ');
}

/**
 * @description 统一压缩空白字符，适配列表单元格预览展示。
 * @param {string} text 原始文本
 * @returns {string}
 */
export function normalizePreviewText(text = '') {
    return String(text || '')
        .replace(/\r/g, '\n')
        .replace(/\n[ \t]+/g, '\n')
        .replace(/[ \t]+/g, ' ')
        .replace(/\n{2,}/g, '\n')
        .trim();
}

/**
 * @description 提取“标签 + 数字”结构，适合话术质检/互动巡检类摘要。
 * @param {string} text 原始文本
 * @param {{maxCount?: number}} options 配置项
 * @returns {{label:string,value:string}[]}
 */
export function extractMetricPreviewItems(text = '', options = {}) {
    const maxCount = Number(options?.maxCount) > 0 ? Number(options.maxCount) : 6;
    const clean = denoisePreviewMarkup(text);
    const result = [];
    let matched = null;
    METRIC_LABEL_RE.lastIndex = 0;
    while ((matched = METRIC_LABEL_RE.exec(clean)) !== null) {
        result.push({
            label: matched[1],
            value: matched[2]
        });
        if (result.length >= maxCount) break;
    }
    return result;
}

/**
 * @description 判断原始文本是否仍残留明显标签噪音，用于把“完全坏掉”的内容升级为错误提示。
 * @param {string} text 原始文本
 * @returns {boolean}
 */
export function hasSuspiciousPreviewMarkup(text = '') {
    return SUSPICIOUS_MARKUP_RE.test(String(text || ''));
}

/**
 * @description 结构化模式：对标签残缺文本做统计项提取，适合话术质检类摘要。
 * 说明：该模式只展示提取成功的数据；一旦提取失败，直接回退为“查看详情”，不再展示文本兜底。
 * @param {string} raw 原始文本
 * @param {{maxMetricCount?: number}} options 配置项
 * @returns {{kind:'metrics'|'text'|'empty'|'error', items?:Array, text?:string, message?:string}}
 */
export function buildStructuredPreviewModel(raw = '', options = {}) {
    const items = extractMetricPreviewItems(raw, { maxCount: options?.maxMetricCount || 4 });
    if (items.length) {
        return { kind: 'metrics', items };
    }
    return { kind: 'error', message: '解析出错' };
}

/**
 * @description 文案模式：对残缺标签做纠错后保留原始文案语义，适合还原度/弹幕巡检。
 * @param {string} raw 原始文本
 * @returns {{kind:'text'|'empty'|'error', text?:string, message?:string}}
 */
export function buildTextPreviewModel(raw = '') {
    const repairedText = repairPreviewTextMarkup(raw);
    const plainText = normalizePreviewText(stripPreviewTags(repairedText));
    if (plainText) {
        return { kind: 'text', text: plainText };
    }
    if (hasSuspiciousPreviewMarkup(raw)) {
        return { kind: 'error', message: '解析出错' };
    }
    return { kind: 'empty', text: '-' };
}

/**
 * @description 构建监控列表预览模型。
 * - `structured`：结构化统计提取，适合话术质检；
 * - `text`：文案纠错，适合话术还原度与弹幕巡检；
 * - 真正抛错时返回 `error`，交由列表展示“查看详情”。
 * @param {any} content 预览内容
 * @param {{type?: string,parseMode?: 'structured'|'text',maxMetricCount?: number}} options 解析选项
 * @returns {{kind:'empty'|'metrics'|'text'|'error', items?:Array, text?:string, message?:string}}
 */
export function buildAiMonitorPreviewModel(content, options = {}) {
    try {
        const raw = normalizePreviewRawContent(content);
        const parseMode = String(options?.parseMode || 'text');
        if (!raw.trim()) {
            return { kind: 'empty', text: '-' };
        }
        if (parseMode === 'structured') {
            return buildStructuredPreviewModel(raw, options);
        }
        return buildTextPreviewModel(raw);
    } catch (error) {
        return {
            kind: 'error',
            message: '解析出错'
        };
    }
}

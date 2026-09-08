/**
 * @description 话术质检报告内容解析工具：兼容 reportContent 为 HTML、JSON、或 Markdown ```json 代码块``` 的返回格式。
 * 目标：将 reportContent 解析为结构化对象，供组件渲染为可读的 HTML 视图；同时避免 reportId 等 Long 在前端流转时丢精度。
 */

/**
 * @description 基础 HTML 清洗（用于后端直接返回 HTML 的 reportContent 场景）。
 * @param {string} html 原始 HTML
 * @returns {string} 清洗后的 HTML
 */
export function sanitizeHtml(html) {
    if (!html) return ''
    return String(html)
        .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
        .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, '')
        .replace(/\son\w+="[^"]*"/gi, '')
        .replace(/\son\w+='[^']*'/gi, '')
}

/**
 * @description 从 Markdown 代码块中提取 json 字符串。
 * @param {string} text 原始文本
 * @returns {string} json 字符串（提取失败返回空串）
 */
export function extractJsonFromMarkdown(text) {
    if (!text) return ''
    const raw = String(text)
    const match = raw.match(/```(?:json)?\s*([\s\S]*?)```/i)
    if (match?.[1]) return match[1].trim()
    return ''
}

/**
 * @description 从 HTML 代码块中提取 json 字符串（兼容 Markdown 被渲染成 <pre><code> 的情况）。
 * @param {string} html 原始 HTML
 * @returns {string} json 字符串（提取失败返回空串）
 */
export function extractJsonFromHtmlCodeBlock(html) {
    if (!html) return ''
    const raw = String(html)
    const match = raw.match(/<pre[^>]*>\s*<code[^>]*>([\s\S]*?)<\/code>\s*<\/pre>/i)
    if (!match?.[1]) return ''
    const decoded = String(match[1])
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&nbsp;/g, ' ')
    return decoded.trim()
}

/**
 * @description 解析 reportContent：兼容 HTML、JSON、Markdown(JSON) 三种形态。
 * @param {any} content reportContent 原始值（string/object/array）
 * @param {string} summaryJson summaryJson（可能为空）
 * @returns {{kind:'empty'|'html'|'json'|'text', html?:string, data?:any, summary?:any, rawText?:string}}
 */
export function parseScriptQualityReportContent(content, summaryJson = '') {
    if (content === null || content === undefined || content === '') return { kind: 'empty' }

    const raw =
        Array.isArray(content)
            ? content.map((it) => (it === null || it === undefined ? '' : String(it))).join('')
            : content

    if (typeof raw === 'object') {
        const data = raw
        const summary = data?.summary || safeParseJson(summaryJson) || null
        return { kind: 'json', data, summary }
    }

    const text = String(raw).trim()
    if (!text) return { kind: 'empty' }

    const jsonFromHtmlCode = extractJsonFromHtmlCodeBlock(text)
    const htmlCodeJson = safeParseJson(jsonFromHtmlCode)
    if (htmlCodeJson) {
        const summary = htmlCodeJson?.summary || safeParseJson(summaryJson) || null
        return { kind: 'json', data: htmlCodeJson, summary }
    }

    const asJson = safeParseJson(text)
    if (asJson) {
        const summary = asJson?.summary || safeParseJson(summaryJson) || null
        return { kind: 'json', data: asJson, summary }
    }

    const jsonFromMd = extractJsonFromMarkdown(text)
    const mdJson = safeParseJson(jsonFromMd)
    if (mdJson) {
        const summary = mdJson?.summary || safeParseJson(summaryJson) || null
        return { kind: 'json', data: mdJson, summary }
    }

    if (/^\s*</.test(text) && /<\/[a-z][^>]*>\s*$/i.test(text)) {
        return { kind: 'html', html: sanitizeHtml(text) }
    }

    // 兜底：尝试截取首尾大括号进行解析（兼容返回里混入其它文本）
    const first = text.indexOf('{')
    const last = text.lastIndexOf('}')
    if (first >= 0 && last > first) {
        const sliced = text.slice(first, last + 1)
        const slicedJson = safeParseJson(sliced)
        if (slicedJson) {
            const summary = slicedJson?.summary || safeParseJson(summaryJson) || null
            return { kind: 'json', data: slicedJson, summary }
        }
    }

    return { kind: 'text', rawText: text }
}

/**
 * @description 安全 JSON.parse：失败返回 null。
 * @param {any} val 输入
 * @returns {any|null}
 */
export function safeParseJson(val) {
    if (val === null || val === undefined) return null
    if (typeof val === 'object') return val
    if (typeof val !== 'string') return null
    const raw = val.trim()
    if (!raw) return null
    try {
        return JSON.parse(raw)
    } catch (e) {
        try {
            const normalized = raw
                .replace(/,\s*([}\]])/g, '$1')
                .replace(/^\uFEFF/, '')
            return JSON.parse(normalized)
        } catch (e2) {
            return null
        }
    }
}

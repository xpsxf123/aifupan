const parseHtmlAttrs = (raw = '') => {
    const attrs = {}
    if (!raw) return attrs
    const text = String(raw)
    const reg = /([^\s=]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'<>`]+))/g
    let m = null
    while ((m = reg.exec(text))) {
        const key = m[1]
        const val = m[2] !== undefined ? m[2] : (m[3] !== undefined ? m[3] : m[4])
        attrs[key] = val
    }
    const stripped = text.replace(reg, ' ')
    const bareTokens = stripped
        .split(/\s+/)
        .map((it) => it.trim())
        .filter(Boolean)
        .filter((it) => !it.includes('='))
    bareTokens.forEach((k) => {
        if (!(k in attrs)) attrs[k] = true
    })
    return attrs
}

const normalizeVisible = (v) => {
    if (v === undefined || v === null || v === '') return false
    const s = String(v).trim().toLowerCase()
    if (s === 'true' || s === '1') return true
    if (s === 'false' || s === '0') return false
    return false
}

const normalizeContentType = (v) => {
    const s = String(v ?? '').trim().toLowerCase()
    if (s === 'json' || s === 'html' || s === 'text') return s
    return ''
}

const extractBlockPayload = (inner = '', attrs = {}, fallbackContentType = 'text') => {
    const raw = String(inner)
    const fenced = raw.match(/```(json|html|text)?\s*([\s\S]*?)```/i)
    const fencedLang = fenced?.[1] ? String(fenced[1]).trim().toLowerCase() : ''
    const fencedBody = fenced?.[2] !== undefined ? String(fenced[2]) : ''
    const content = (fenced ? fencedBody : raw).trim()
    const attrType = normalizeContentType(attrs.contentType)
    const contentType = normalizeContentType(fencedLang) || attrType || normalizeContentType(fallbackContentType) || 'text'
    return { contentType, content }
}

const safeParseJson = (text) => {
    if (text === null || text === undefined) return null
    if (typeof text === 'object') return text
    const raw = String(text).trim()
    if (!raw) return null
    try {
        return JSON.parse(raw)
    } catch (e) {
        try {
            const normalized = raw.replace(/,\s*([}\]])/g, '$1').replace(/^\uFEFF/, '')
            return JSON.parse(normalized)
        } catch (e2) {
            return null
        }
    }
}

export const preprocessDataBlocks = (content = '', option = {}) => {
    const rawText = String(content || '')
    const text = rawText.replace(/<aifupan-data-block\b([^>]*)\/>/gi, '<aifupan-data-block$1>')
    const blocks = []
    const tokenPrefix = '@@AIFUPAN_DATA_BLOCK__'
    const jsonRequiredTypes = new Set(['structured', 'scriptQualityReport'])

    let idx = 0
    const replaced = text.replace(
        /<aifupan-data-block\b([^>]*)>([\s\S]*?)<\/aifupan-data-block>/gi,
        (full, attrRaw = '', inner = '') => {
            const attrs = parseHtmlAttrs(attrRaw)
            const visible = normalizeVisible(attrs.visible)
            const cacheKey = attrs.cacheKey ? String(attrs.cacheKey) : `dataBlock_${Date.now()}_${idx++}`
            const type = attrs.type ? String(attrs.type) : (visible ? 'structured' : '')
            const resourceData = !!(attrs['resource-data'] || attrs.resourceData)
            const fallbackContentType = jsonRequiredTypes.has(type) ? 'json' : 'text'
            const payload = extractBlockPayload(inner, attrs, fallbackContentType)
            const resolvedVisible = visible && (!jsonRequiredTypes.has(type) || payload.contentType === 'json')
            const data = payload.contentType === 'json' ? safeParseJson(payload.content) : payload.content

            blocks.push({
                cacheKey,
                type,
                visible: resolvedVisible,
                raw: payload.content,
                data,
                contentType: payload.contentType,
                resourceData
            })

            if (!resolvedVisible) return ''
            return `\n${tokenPrefix}${cacheKey}@@\n`
        }
    )

    return {
        text: replaced,
        blocks
    }
}

export const injectDataBlockPlaceholders = (html = '', blocks = []) => {
    if (!blocks?.length) return html || ''
    const rawHtml = String(html || '')
    const tokenPrefix = '@@AIFUPAN_DATA_BLOCK__'
    let out = rawHtml
    blocks.forEach((b) => {
        if (!b?.visible) return
        const token = `${tokenPrefix}${b.cacheKey}@@`
        out = out.split(token).join(`<div class="aifupan-data-block" data-cache-key="${b.cacheKey}"></div>`)
    })
    return out
}

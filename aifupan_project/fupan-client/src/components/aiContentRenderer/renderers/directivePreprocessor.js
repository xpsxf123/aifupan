const parseDirectiveParams = (raw = '') => {
    const out = {}
    const text = String(raw ?? '').trim()
    if (!text) return out

    const reg = /([^\s=,]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\s]+))/g
    let m = null
    while ((m = reg.exec(text))) {
        const key = m[1]
        const val = m[2] !== undefined ? m[2] : (m[3] !== undefined ? m[3] : m[4])
        out[key] = val
    }

    const stripped = text.replace(reg, ' ')
    const bare = stripped
        .split(',')
        .map((it) => it.trim())
        .filter(Boolean)
        .flatMap((it) => it.split(/\s+/).map((x) => x.trim()).filter(Boolean))
        .filter((it) => !it.includes('='))

    bare.forEach((k) => {
        if (!(k in out)) out[k] = true
    })

    return out
}

const escapeAttrValue = (val) => String(val ?? '').replace(/"/g, '&quot;')

const buildAttrsText = (params = {}, omitKeys = []) => {
    const omit = new Set(omitKeys || [])
    const list = Object.keys(params || {}).filter((k) => !omit.has(k))
    if (!list.length) return ''
    return list
        .map((k) => {
            const v = params[k]
            if (v === true) return ` ${k}`
            if (v === false || v === null || v === undefined || v === '') return ''
            return ` ${k}="${escapeAttrValue(v)}"`
        })
        .join('')
}

export const preprocessDirectiveTags = (content = '') => {
    const text = String(content ?? '')
    if (!text) return text

    const reg = /\[!##!\](?:\{([\s\S]*?)\}|\(([\s\S]*?)\))|\[!##\](?:\{([\s\S]*?)\}|\(([\s\S]*?)\))|\[##!\]/g
    let out = ''
    let lastIndex = 0
    const stack = []
    let m = null

    while ((m = reg.exec(text))) {
        out += text.slice(lastIndex, m.index)
        lastIndex = reg.lastIndex

        const full = m[0]
        const selfParamsRaw = m[1] ?? m[2]
        const openParamsRaw = m[3] ?? m[4]

        if (full === '[##!]') {
            const tagName = stack.pop()
            out += tagName ? `</${tagName}>` : full
            continue
        }

        const paramsRaw = selfParamsRaw ?? openParamsRaw ?? ''
        const params = parseDirectiveParams(paramsRaw)
        const element = params.element ? String(params.element).trim() : ''
        if (!element) {
            out += full
            continue
        }

        const tagName = `aifupan-${element}`
        const attrsText = buildAttrsText(params, ['element'])

        if (full.startsWith('[!##!]')) {
            out += `<${tagName}${attrsText} />`
            continue
        }

        out += `<${tagName}${attrsText}>`
        stack.push(tagName)
    }

    out += text.slice(lastIndex)
    while (stack.length) {
        const tagName = stack.pop()
        out += `</${tagName}>`
    }
    return out
}

export default preprocessDirectiveTags


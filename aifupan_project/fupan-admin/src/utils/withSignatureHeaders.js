import apiSignature from '@/utils/signature.js'

function buildUrlWithParams(url, params) {
    if (!params || Object.keys(params).length === 0) return url
    const query = Object.entries(params)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&')
    return url.includes('?') ? `${url}&${query}` : `${url}?${query}`
}

export async function withSignatureHeaders(config) {
    const method = config.method ? config.method.toUpperCase() : 'GET'
    let url = config.baseURL
        ? config.baseURL.replace(/\/$/, '') + config.url
        : config.url
    url = buildUrlWithParams(url, config.params) // 拼接 params
    const body = config.data || null
    const contentType = config.headers['Content-Type'] || config.headers['content-type'] || 'application/json'

    const signatureHeaders = await apiSignature.createSignatureHeaders(body, method, url, contentType)
    return {
        ...config,
        headers: {
            ...config.headers,
            ...signatureHeaders
        }
    }
}
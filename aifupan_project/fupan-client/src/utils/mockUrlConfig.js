import env from '/src/env'
import resolveWebVersion from './webVersion'

const mockUrlMap = {
    '2.60.3': []
}

const MOCK_BASE_URL = 'http://127.0.0.1:3000/replay'

const normalizeVersionForMock = (version = '') => {
    const text = String(version ?? '')
    const m = text.match(/(\d+(?:\.\d+)*)/)
    const raw = m?.[1] || ''
    if (!raw) return ''
    const parts = raw.split('.').filter(Boolean)

    if (parts.length === 4) {
        const major = Number(parts[0]) || 0
        const minor = (Number(parts[1]) || 0) * 10 + (Number(parts[2]) || 0)
        const patch = Number(parts[3]) || 0
        return `${major}.${minor}.${patch}`
    }

    if (parts.length >= 3) {
        return `${Number(parts[0]) || 0}.${Number(parts[1]) || 0}.${Number(parts[2]) || 0}`
    }

    if (parts.length === 2) {
        return `${Number(parts[0]) || 0}.${Number(parts[1]) || 0}.0`
    }

    return `${Number(parts[0]) || 0}.0.0`
}

const splitPathAndQuery = (value = '') => {
    const text = String(value ?? '')
    if (!text) return {path: '', query: ''}
    if (/^https?:\/\//i.test(text)) {
        const url = new URL(text)
        return {
            path: url.pathname || '',
            query: url.search ? url.search.slice(1) : ''
        }
    }
    const [pathPart, queryPart = ''] = text.split('?')
    const path = pathPart?.startsWith('/') ? pathPart : `/${pathPart || ''}`
    return {path, query: queryPart}
}

const isMockUrl = (httpUrl) => {
    try {
        if (!env.dev) return httpUrl

        const currentVersion = normalizeVersionForMock(resolveWebVersion())
        const allowList = mockUrlMap[currentVersion]
        if (!Array.isArray(allowList) || !allowList.length) return httpUrl

        const {path, query} = splitPathAndQuery(httpUrl)
        if (!path) return httpUrl
        if (!allowList.includes(path)) return httpUrl

        return `${MOCK_BASE_URL}${path}${query ? `?${query}` : ''}`
    } catch (error) {
        return httpUrl
    }
}

export default isMockUrl

const store = new Map()

const getScopeMap = (scope) => {
    const scopeKey = scope || 'global'
    const exists = store.get(scopeKey)
    if (exists) return exists
    const m = new Map()
    store.set(scopeKey, m)
    return m
}

export const setDataBlock = (scope, key, value) => {
    if (!key) return
    getScopeMap(scope).set(String(key), value)
}

export const getDataBlock = (scope, key) => {
    if (!key) return undefined
    return getScopeMap(scope).get(String(key))
}

export const getAllDataBlocks = (scope) => {
    const m = getScopeMap(scope)
    const obj = {}
    m.forEach((v, k) => {
        obj[k] = v
    })
    return obj
}

export const clearDataBlock = (scope, key) => {
    if (!key) return
    getScopeMap(scope).delete(String(key))
}

export const clearDataBlockScope = (scope) => {
    store.delete(scope || 'global')
}


const KEY = 'AI_MONITOR_UNREAD_REPORT_IDS'

const safeRead = () => {
    try {
        const raw = localStorage.getItem(KEY)
        const parsed = raw ? JSON.parse(raw) : []
        return Array.isArray(parsed) ? parsed.map((v) => String(v)).filter(Boolean) : []
    } catch (e) {
        return []
    }
}

const safeWrite = (list) => {
    try {
        localStorage.setItem(KEY, JSON.stringify(list))
    } catch (e) {
    }
}

const toSet = () => new Set(safeRead())

export default {
    has(reportId) {
        if (reportId === null || reportId === undefined || reportId === '') return false
        return toSet().has(String(reportId))
    },
    add(reportId) {
        if (reportId === null || reportId === undefined || reportId === '') return
        const id = String(reportId)
        const set = toSet()
        if (set.has(id)) return
        set.add(id)
        const list = Array.from(set)
        safeWrite(list.slice(-200))
    },
    remove(reportId) {
        if (reportId === null || reportId === undefined || reportId === '') return
        const id = String(reportId)
        const set = toSet()
        if (!set.has(id)) return
        set.delete(id)
        safeWrite(Array.from(set))
    },
    clear() {
        try {
            localStorage.removeItem(KEY)
        } catch (e) {
        }
    }
}


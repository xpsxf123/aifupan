import ScriptQualityReportContent from '@/components/aiMonitor/scriptQualityReportContent/index.vue'
import JsonBlock from './JsonBlock.vue'

export const DATA_BLOCK_REGISTRY = [
    {
        type: 'structured',
        componentName: 'AiDataBlockJson',
        component: JsonBlock,
        mapProps: (block) => ({ value: block?.data })
    },
    {
        type: 'scriptQualityReport',
        componentName: 'ScriptQualityReportContent',
        component: ScriptQualityReportContent,
        mapProps: (block) => ({ content: block?.data })
    }
]

export const getDataBlockRegistryMap = (extra = []) => {
    const all = [...DATA_BLOCK_REGISTRY, ...(Array.isArray(extra) ? extra : [])]
    const m = new Map()
    all.forEach((it) => {
        if (!it?.type) return
        m.set(String(it.type), it)
    })
    return m
}


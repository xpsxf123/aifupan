import { VERSION_TYPE } from '@/enum'

const FEATURE_DESC_MAP = {
    aiSharding: 'AI拆解脚本可以对直播脚本进行结构化拆解。',
    aiOptimal: 'AI仿写本场可以快速仿写和生成直播脚本。',
    aiDiagnose: 'AI数据诊断可以快速诊断本场直播问题。',
    scriptQuality: '话术质检可以生成本场直播的质检报告。',
    interactionInspection: '互动巡检可以生成本场直播的互动巡检报告。',
    scriptRestoration: '话术还原度可以生成本场直播的还原度报告。'
}

export default {
    methods: {
        isPureRecordingVersion() {
            return this.$store.getters.getVersionType === VERSION_TYPE.PURE || this.$store.getters.isPure
        },
        getUpgradeToAgentDesc(feature) {
            const key = String(feature || '')
            return FEATURE_DESC_MAP[key] || '该功能需要升级后使用。'
        },
        showUpgradeToAgentConfirm(desc) {
            const safeDesc = String(desc || '').trim() || '该功能需要升级后使用。'
            return this.$confirm(
                `
                <div style="text-align:center">
                    <div>${safeDesc}</div>
                    <div style="margin-top:12px">
                        需要升级到 <span style="color: var(--color-main)">AI全能版</span> 会员才能使用。
                    </div>
                </div>
            `,
                '温馨提示',
                {
                    showClose: false,
                    customClass: 'format-front',
                    dangerouslyUseHTMLString: true,
                    confirmButtonText: '前往升级',
                    cancelButtonText: '知道了',
                    closeOnClickModal: false,
                    closeOnPressEscape: false
                }
            ).then(() => {
                this.APP?.showQrCode?.()
            })
        }
    }
}


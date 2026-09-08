<!--
/**
 * @description 互动巡检列表单元格：支持只读展示（无按钮）与报告弹窗；内置静态mock用于展示多状态样式。
 */
-->

<template>
    <div class="interactionInspectCell">
        <AiMonitorReportDialog
            :visible.sync="reportVisible"
            type="inspect"
            :row="row"
            :content="reportContent"
            :reportId="reportId"
            :readonly="reportReadonly"
            @opened="handleReportOpened" />

        <div v-if="readOnly" class="cellMain">
            <template v-if="state.reportStatus !== 2">
                <span class="cellText">-</span>
            </template>
            <template v-else>
                <div class="cellStats">
                    <div class="cellRow">
                        <span class="cellLabel">有效性：</span>
                        <span class="cellValue" @click="openReport">{{ formatPercent(state.effectiveness) }}</span>
                    </div>
                </div>
            </template>
        </div>
        <div v-else class="cellMain">
            <template v-if="state.reportStatus !== 2">
                <span class="cellText">-</span>
            </template>
            <template v-else>
                <div class="cellStats">
                    <div class="cellRow">
                        <span class="cellLabel">有效性：</span>
                        <span class="cellValue" @click="openReport">{{ formatPercent(state.effectiveness) }}</span>
                    </div>
                    <div class="cellRow">
                        <span class="cellLabel">未回复：</span>
                        <span class="cellValue" @click="openReport">{{ state.missed || 0 }}条</span>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
import AiMonitorReportDialog from '@/components/aiMonitor/reportDialog/index.vue'

export default {
    components: {
        AiMonitorReportDialog
    },
    props: {
        row: {
            type: Object,
            default: () => ({})
        },
        mockIndex: {
            type: Number,
            default: -1
        },
        useMock: {
            type: Boolean,
            default: true
        },
        readOnly: {
            type: Boolean,
            default: false
        },
        readonlyReport: {
            type: [Boolean, null],
            default: null
        }
    },
    data() {
        return {
            state: {
                reportStatus: 0,
                effectiveness: null,
                missed: 0,
                __mockInitialized: false
            },
            reportVisible: false,
            reportContent: '',
            reportId: null
        }
    },
    computed: {
        reportReadonly() {
            if (this.readonlyReport === true) return true
            if (this.readonlyReport === false) return false
            const accountType = this.row?.basicSettingsVo?.accountType
            if (accountType === undefined || accountType === null) return true
            return accountType !== 0
        }
    },
    watch: {
        row: {
            immediate: true,
            handler() {
                this.initMockIfNeeded()
                this.loadFromApiIfNeeded()
            }
        }
    },
    methods: {
        async loadFromApiIfNeeded() {
            if (this.useMock) return
            const videoId = this.row?.videoId
            if (!videoId) return
            const env = window.SITE_CONFIG?.env
            const enableApi = env === 'custom' || localStorage.getItem('ENABLE_SCRIPT_MONITOR_API') === '1'
            if (!enableApi) return
            if (!this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return
            try {
                const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus({ videoId })
                if (res?.code !== 0) return
                const data = res?.data || {}
                const inspect = data?.interactionPatrol || (Array.isArray(data?.monitors) ? data.monitors.find((it) => Number(it?.monitorType) === 2) : {}) || {}
                const status = Number(inspect?.status ?? 0)
                // UI 规则：仅当 status=0（未生成）且 monitorEnabled 明确为“未开启”时，才按“未开启”处理
                if (status === 0 && inspect?.monitorEnabled !== undefined && inspect?.monitorEnabled !== null && Number(inspect.monitorEnabled) !== 1) {
                    this.state.reportStatus = 0
                    this.state.effectiveness = null
                    this.state.missed = 0
                    this.reportId = null
                    return
                }
                this.state.reportStatus = status
                this.state.effectiveness = inspect?.summary?.effectivenessPercentage ?? null
                const text = String(inspect?.summary?.summary || '')
                const m = text.match(/未(?:及时)?回复\s*(\d+)\s*条/)
                this.state.missed = m ? Number(m[1]) : 0
                this.reportId = inspect.reportId || null
            } catch (e) {
            }
        },
        initMockIfNeeded() {
            if (!this.useMock) return
            if (process?.env?.NODE_ENV === 'production') return
            if (this.state.__mockInitialized) return
            if (this.mockIndex === undefined || this.mockIndex === null || this.mockIndex < 0) return

            this.state.__mockInitialized = true
            const p = this.mockIndex % 3
            if (p === 0) {
                this.state.reportStatus = 0
                return
            }
            if (p === 1) {
                this.state.reportStatus = 2
                this.state.effectiveness = 97.6
                this.state.missed = 0
                this.reportId = 1000000001 + this.mockIndex
                return
            }
            this.state.reportStatus = 2
            this.state.effectiveness = 76.2
            this.state.missed = 5
            this.reportId = 1000000101 + this.mockIndex
        },
        formatPercent(val) {
            if (val === null || val === undefined || val === '') return '--'
            const num = Number(val)
            if (!Number.isFinite(num)) return '--'
            return `${num}%`
        },
        openReport() {
            this.reportVisible = true
        },
        handleReportOpened() {
        }
    }
}
</script>

<style lang="scss" scoped>
@import "./index.scss";
</style>

<!--
/**
 * @description 话术还原度列表单元格：支持复盘列表(开启->生成)与文件分析/上传列表(生成->配置->直接生成)两种流程；内置静态mock用于展示多状态样式。
 */
-->

<template>
    <div class="scriptRestoreCell">
        <!-- 话术还原度弹窗：analysis 场景（已明确直播间/录制资源，可能直接生成报告） -->
        <ScriptRestorationDrawer
            :visible.sync="drawerVisible"
            :scene="drawerScene"
            :form="drawerForm"
            @confirmed="handleStandardScriptConfirmed"
            @generate="generateReport" />

        <AiMonitorReportDialog
            :visible.sync="reportVisible"
            type="restore"
            :row="row"
            :content="reportContent"
            :report-id="state.reportId"
            :readonly="reportReadonly"
            @opened="handleReportOpened"
            @confirmed="handleReportConfirmed" />

        <div v-if="readOnly" class="cellMain">
            <template v-if="state.reportStatus !== 2">
                <span class="cellText">-</span>
            </template>
            <template v-else>
                <AiMonitorPreviewContent
                    :content="getPreviewContent()"
                    preview-type="restore"
                    parse-mode="text"
                    with-stats-style
                    @view="openReport"
                />
            </template>
        </div>
        <div v-else>
            <div v-if="isCompetitor" class="cellText">-</div>
            <div v-else class="cellMain">
                <template v-if="scene === 'replay' && !state.enabled">
                    <el-button type="text" class="cellLink" @click="handleEnable">自动监控</el-button>
                </template>
                <template v-else-if="state.reportStatus !== 2">
                    <el-button type="text" class="cellLink" @click="handleGenerateClick">生成报告</el-button>
                </template>
                <template v-else>
                    <el-badge :is-dot="state.unread" class="cellBadge">
                        <AiMonitorPreviewContent
                            :content="getPreviewContent()"
                            preview-type="restore"
                            parse-mode="text"
                            with-stats-style
                            @view="openReport"
                        />
                    </el-badge>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
import ScriptRestorationDrawer from '@/components/scriptRestoration/ScriptRestorationDrawer.vue'
import AiMonitorReportDialog from '@/components/aiMonitor/reportDialog/index.vue'
import AiMonitorPreviewContent from '@/components/aiMonitor/previewContent/index.vue'

const DEFAULT_CONFIG = {
    scriptRestoreMode: '',
    scriptRestoreLoopDuration: 0,
    scriptRestoreTalkSpeed: 280,
    scriptRestoreReferenceScript: '',
    standardScriptId: null
}

export default {
    components: {
        ScriptRestorationDrawer,
        AiMonitorReportDialog,
        AiMonitorPreviewContent
    },
    props: {
        row: {
            type: Object,
            default: () => ({})
        },
        scene: {
            type: String,
            default: 'replay'
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
                enabled: false,
                reportStatus: 0,
                reportId: null,
                unread: false,
                score: null,
                preview: '',
                config: { ...DEFAULT_CONFIG },
                __mockInitialized: false
            },
            drawerVisible: false,
            drawerScene: 'analysis',
            drawerForm: { ...DEFAULT_CONFIG },
            reportVisible: false,
            reportContent: ''
        }
    },
    computed: {
        resolvedAccountType() {
            const candidates = [
                this.row?.basicSettingsVo?.accountType,
                this.row?.anchorInfo?.accountType,
                this.row?.accountType,
                this.row?.videoInfo?.basicSettingsVo?.accountType,
                this.row?.videoInfo?.accountType
            ]
            for (const item of candidates) {
                if (item === undefined || item === null || item === '') continue
                const num = Number(item)
                if (Number.isFinite(num)) return num
            }
            return null
        },
        isCompetitor() {
            return this.resolvedAccountType !== 0
        },
        reportReadonly() {
            if (this.readonlyReport === true) return true
            if (this.readonlyReport === false) return false
            if (this.resolvedAccountType === null) return false
            return this.resolvedAccountType !== 0
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
        getPreviewContent() {
            if (this.state.preview) return this.state.preview
            if (this.state.score === null || this.state.score === undefined || this.state.score === '') return ''
            const num = Number(this.state.score)
            if (!Number.isFinite(num)) return ''
            return `${num}%`
        },
        getSecUid() {
            const row = this.row || {}
            const candidates = [
                row.secUid,
                row.SecUid,
                row.anchorInfo?.secUid,
                row.anchorInfo?.SecUid,
                row.basicSettingsVo?.secUid,
                row.basicSettingsVo?.SecUid
            ]
            for (const item of candidates) {
                if (item !== undefined && item !== null && String(item).trim()) return String(item).trim()
            }
            return ''
        },
        getSourceParams() {
            const row = this.row || {}
            const fileId = row.fileId || row.FileId || null
            const rawSceneType = row.sceneType ?? row.SceneType
            const rawSourceType = row.sourceType
            if (fileId || Number(rawSourceType) === 1) {
                const fileType = Number(row.fileType ?? row.FileType)
                const sceneType = rawSceneType === undefined || rawSceneType === null
                    ? (fileType === 2 ? 2 : 1)
                    : Number(rawSceneType)
                return {
                    sourceType: 1,
                    sceneType,
                    sourceId: String(fileId || row.sourceId || row.id || '')
                }
            }
            return {
                sourceType: 0,
                sceneType: 0,
                sourceId: String(row.videoId || row.VideoId || row.sourceId || row.id || row.Id || '')
            }
        },
        getAnchorUrlUserId() {
            const row = this.row || {}
            const candidates = [
                row.anchorUrlUserId,
                row.anchor_url_user_id,
                row.anchorInfo?.anchorUrlUserId,
                row.anchorInfo?.anchor_url_user_id,
                row.basicSettingsVo?.anchorUrlUserId,
                row.basicSettingsVo?.anchor_url_user_id,
                row.basicSettingsVo?.anchorId,
                row.anchorId
            ]
            for (const item of candidates) {
                const n = Number(item)
                if (Number.isFinite(n) && n > 0) return n
            }
            return null
        },
        isEnableApi() {
            const env = window.SITE_CONFIG?.env
            return env === 'custom' || localStorage.getItem('ENABLE_SCRIPT_MONITOR_API') === '1'
        },
        async loadFromApiIfNeeded() {
            if (this.useMock) {
                if (process?.env?.NODE_ENV === 'production') return
                if (!this.isEnableApi()) return
            }
            if (!this.isEnableApi()) return
            if (!this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return
            const { sourceType, sceneType, sourceId } = this.getSourceParams()
            if (!sourceId) return
            if (this.isCompetitor) return

            try {
                const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus({ sourceType, sceneType, sourceId })
                if (res?.code !== 0) return
                const data = res?.data || {}
                const fidelity = data?.scriptFidelityMonitor || (Array.isArray(data?.monitors) ? data.monitors.find((it) => Number(it?.monitorType) === 1) : {}) || {}
                const status = Number(fidelity?.status ?? 0)
                // UI 规则：仅当 status=0（未生成）且 monitorEnabled 明确为“未开启”时，才按“未开启”处理
                if (status === 0 && fidelity?.monitorEnabled !== undefined && fidelity?.monitorEnabled !== null && Number(fidelity.monitorEnabled) !== 1) {
                    this.state.enabled = false
                    this.state.reportStatus = 0
                    this.state.reportId = null
                    this.state.unread = false
                    this.state.score = null
                    this.state.preview = ''
                    return
                }
                this.state.reportStatus = status
                this.state.reportId = fidelity.reportId || null
                const summary = fidelity?.summary
                this.state.preview = typeof summary === 'string'
                    ? summary
                    : String(summary?.summaryText || summary?.summary || '')
                const score = summary?.score ?? null
                this.state.score = score === null || score === undefined ? null : Number(score)
                this.state.unread = !fidelity.isRead

                const enabledFlag = this.row?.basicSettingsVo?.isScriptFidelityMonitor
                    ?? this.row?.basicSettingsVo?.scriptRestoreMonitor
                    ?? this.row?.isScriptFidelityMonitor
                    ?? this.row?.scriptRestoreMonitor
                if (enabledFlag !== undefined && enabledFlag !== null) {
                    this.state.enabled = Number(enabledFlag) === 1
                } else if (this.scene !== 'replay') {
                    this.state.enabled = true
                }
            } catch (e) {
            }
        },
        initMockIfNeeded() {
            if (!this.useMock) return
            if (process?.env?.NODE_ENV === 'production') return
            if (this.isEnableApi()) return
            if (this.state.__mockInitialized) return
            if (this.mockIndex === undefined || this.mockIndex === null || this.mockIndex < 0) return

            this.state.__mockInitialized = true
            const p = this.mockIndex % 4

            if (p === 0) {
                this.state.enabled = this.scene === 'replay' ? false : true
                this.state.reportStatus = 0
                return
            }
            if (p === 1) {
                this.state.enabled = true
                this.state.reportStatus = 0
                this.state.config = {
                    scriptRestoreMode: 2,
                    scriptRestoreLoopDuration: 0,
                    scriptRestoreTalkSpeed: 280,
                    scriptRestoreReferenceScript: '（Mock）请粘贴参考直播脚本，用于生成还原度报告'
                }
                return
            }
            if (p === 2) {
                this.state.enabled = true
                this.state.reportStatus = 2
                this.state.reportId = null
                this.state.unread = false
                this.state.score = 98.9
                this.state.preview = '98.9%'
                return
            }
            this.state.enabled = true
            this.state.reportStatus = 2
            this.state.reportId = null
            this.state.unread = true
            this.state.score = 88.1
            this.state.preview = '88.1%'
        },
        formatPercent(val) {
            if (val === null || val === undefined || val === '') return '--'
            const num = Number(val)
            if (!Number.isFinite(num)) return '--'
            return `${num}%`
        },
        handleEnable() {
            const anchorUrlUserId = this.getAnchorUrlUserId()
            const secUid = this.getSecUid()
            this.drawerScene = 'anchorConfig'
            this.drawerForm = {
                ...(this.state.config || DEFAULT_CONFIG),
                ...(anchorUrlUserId ? { anchorUrlUserId } : {}),
                ...(secUid ? { secUid } : {})
            }
            this.drawerVisible = true
        },
        handleGenerateClick() {
            if (this.scene !== 'replay') {
                const anchorUrlUserId = this.getAnchorUrlUserId()
                const secUid = this.getSecUid()
                this.drawerScene = 'analysis'
                this.drawerForm = {
                    ...(this.state.config || DEFAULT_CONFIG),
                    ...(anchorUrlUserId ? { anchorUrlUserId } : {}),
                    ...(secUid ? { secUid } : {})
                }
                this.drawerVisible = true
                return
            }
            if (!this.state.enabled) {
                this.handleEnable()
                return
            }
            this.generateReport()
        },
        async handleStandardScriptConfirmed() {
            if (this.scene === 'replay') {
                const secUid = this.getSecUid()
                if (secUid && this.$httpBack?.scriptMonitor?.setMonitorEnabled) {
                    try {
                        const res = await this.$httpBack.scriptMonitor.setMonitorEnabled({ secUid: String(secUid), monitorType: 1, enabled: 1 })
                        if (res?.code === 0) {
                            this.state.enabled = true
                            this.state.config = { ...(this.drawerForm || DEFAULT_CONFIG) }
                            this.$message?.success?.('已开启')
                            this.loadFromApiIfNeeded()
                            return
                        }
                        this.$message?.warning?.(res?.msg || '开启失败')
                    } catch (e) {
                    }
                }
                return
            }
            this.state.enabled = true
            this.state.config = { ...(this.drawerForm || DEFAULT_CONFIG) }
        },
        async generateReport() {
            const { sourceType, sceneType, sourceId } = this.getSourceParams()
            if (this.isEnableApi() && this.$httpBack?.scriptMonitor?.triggerScriptMonitorReport && sourceId) {
                try {
                    this.state.reportStatus = 1
                    const res = await this.$httpBack.scriptMonitor.triggerScriptMonitorReport({
                        sourceType,
                        sceneType,
                        sourceId,
                        monitorType: 1
                    })
                    if (res?.code !== 0) {
                        const msg = res?.msg || '生成失败'
                        this.$message?.warning?.(msg)
                        if (String(msg).includes('请先生成标准直播稿') || String(msg).includes('请先确认标准直播稿')) {
                            this.handleEnable()
                        }
                        this.state.reportStatus = 0
                        return
                    }
                    setTimeout(() => {
                        this.loadFromApiIfNeeded()
                    }, 1400)
                    return
                } catch (e) {
                    this.state.reportStatus = 0
                    return
                }
            }

            this.state.reportStatus = 2
            if (this.state.score === null || this.state.score === undefined) {
                const base = this.mockIndex >= 0 ? this.mockIndex : 0
                const score = 70 + (base % 30) + ((base % 10) / 10)
                this.state.score = Number(score.toFixed(1))
            }
            this.state.unread = true
        },
        openReport() {
            this.state.unread = false
            this.reportVisible = true
        },
        handleReportOpened() {
        },
        handleReportConfirmed() {
            this.state.unread = false
            this.loadFromApiIfNeeded()
        }
    }
}
</script>

<style lang="scss" scoped>
@import "./index.scss";
</style>

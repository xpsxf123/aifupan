<!--
@description 监控报告面板：独立承载话术质检、互动巡检、话术还原度三类报告的状态查询、空页面、报告渲染与还原度抽屉。
注意：本组件只服务新建的监控详情页，故意不复用 wordDiscern 的整套壳层，避免旧页面的工具栏、弹窗与默认逻辑被带入。
-->
<template>
    <div class="monitorReportPanel">
        <div
            v-if="isScriptRestoration"
            class="wordsBodyContainer ai-diagnose ai-content-box monitorReportPanelBody"
            :style="{ padding: currentStatus === 2 ? '0 10px' : '0' }"
        >
            <template v-if="showMonitorDisabledEmpty">
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">暂未生成话术还原度报告...</div>
                <div v-else-if="currentInlineErrorText" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ currentInlineErrorText }}</div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">请前往客户端获取话术还原度报告...</div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">没有生成话术还原度报告权限...</div>
                <InspectionEmptyPage
                    v-else
                    type="scriptRestoration"
                    :secondary-visible="canEnableAutoMonitor('scriptRestoration', currentMonitorEnabled)"
                    @secondary-click="openScriptRestorationDrawer('enable')"
                    @primary-click="openScriptRestorationDrawer('generate')"
                />
            </template>
            <div v-else-if="currentStatus === 2" ref="scriptRestoration" class="monitorReportPanelBody">
                <div
                    v-for="(item, index) in restorationContentList"
                    :key="`scriptRestoration-${index}`"
                    :class="`scriptRestoration-${index + 1}`"
                    class="pd-t4 pd-b4"
                >
                    <AiContentRenderer
                        :content="item"
                        render-mode="mdTag"
                        :option="{ forceStyle: true, upgradePlainTable: true }"
                        :data-block-scope="`monitor-script-restoration-${index}`"
                    />
                </div>
            </div>
            <div v-else-if="currentStatus === 1" class="pd-t40">
                <letterSpacing text="话术还原度报告正在获取中，请五分钟后再查看..." :position="-3"></letterSpacing>
            </div>
            <div v-else-if="currentStatus === 3">
                <div class="font-s12 text-colorc2 text-center">话术还原度报告生成失败...</div>
                <afp-button
                    size="medium"
                    style="float: right;margin:12px"
                    :disabled="!isAuthenticated"
                    @click="createInspectionReport('scriptRestoration')"
                >重新生成报告</afp-button>
            </div>
            <template v-else>
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">暂未生成话术还原度报告...</div>
                <div v-else-if="currentInlineErrorText" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ currentInlineErrorText }}</div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">请前往客户端获取话术还原度报告...</div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">没有生成话术还原度报告权限...</div>
                <InspectionEmptyPage
                    v-else
                    type="scriptRestoration"
                    :secondary-visible="canEnableAutoMonitor('scriptRestoration', currentMonitorEnabled)"
                    @secondary-click="openScriptRestorationDrawer('enable')"
                    @primary-click="openScriptRestorationDrawer('generate')"
                />
            </template>
        </div>

        <div v-else class="wordsBodyContainer ai-diagnose ai-content-box scriptQualityDetailContainer monitorReportPanelBody">
            <template v-if="showMonitorDisabledEmpty">
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">{{ emptyText }}</div>
                <div v-else-if="currentInlineErrorText" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ currentInlineErrorText }}</div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ webText }}</div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ noAuthText }}</div>
                <InspectionEmptyPage
                    v-else-if="canShowInspectionEmpty"
                    :type="reportType"
                    :secondary-visible="canEnableAutoMonitor(reportType, currentMonitorEnabled)"
                    @secondary-click="handleSecondaryClick"
                    @primary-click="createInspectionReport(reportType)"
                />
                <div v-else class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ pendingAiAnalysisText }}</div>
            </template>
            <div v-else-if="currentStatus === 2" class="monitorReportPanelBody">
                <div :ref="reportType" class="scriptQualityDetailContent">
                    <ScriptQualityReportContent :content="currentContent" />
                </div>
            </div>
            <div v-else-if="currentStatus === 1" class="pd-t40">
                <letterSpacing :text="loadingText" :position="-3"></letterSpacing>
            </div>
            <div v-else-if="currentStatus === 3">
                <div class="font-s12 text-colorc2 text-center">{{ failedText }}</div>
                <afp-button
                    v-if="canRetryGenerate"
                    size="medium"
                    style="float: right;margin:12px"
                    :disabled="!isAuthenticated"
                    @click="createInspectionReport(reportType, { keepOldOn70001: true })"
                >重新生成报告</afp-button>
            </div>
            <template v-else>
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">{{ emptyText }}</div>
                <div v-else-if="currentInlineErrorText" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ currentInlineErrorText }}</div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ webText }}</div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ noAuthText }}</div>
                <InspectionEmptyPage
                    v-else-if="canShowInspectionEmpty"
                    :type="reportType"
                    :secondary-visible="canEnableAutoMonitor(reportType, currentMonitorEnabled)"
                    @secondary-click="handleSecondaryClick"
                    @primary-click="createInspectionReport(reportType)"
                />
                <div v-else class="pd-t20 flex-jc-c font-s12 text-colorc3">{{ pendingAiAnalysisText }}</div>
            </template>
        </div>

        <ScriptRestorationDrawer
            :visible.sync="scriptRestorationDrawerVisible"
            :textData="sentenceMarkData"
            :scene="scriptRestorationDrawerAction === 'enable' ? 'anchorConfig' : 'analysis'"
            :form="scriptRestorationDrawerForm"
            @confirmed="handleScriptRestorationConfirmed"
            @generate="createInspectionReport('scriptRestoration')"
        />
    </div>
</template>

<script>
/**
 * @description 监控报告面板脚本：聚合 reportStatus / detail / triggerReport 三类接口，只保留新监控详情页需要的最小功能集。
 */
import auth from '@/mixins/auth.js'
import { getSourceData } from '@/utils/common'
import InspectionEmptyPage from '@/components/DiscernSearchContainer/InspectionEmptyPage.vue'
import ScriptQualityReportContent from '@/components/aiMonitor/scriptQualityReportContent/index.vue'
import AiContentRenderer from '@/components/aiContentRenderer/index.vue'
import ScriptRestorationDrawer from '@/components/scriptRestoration/ScriptRestorationDrawer.vue'
import letterSpacing from '@/components/letterShake/index.vue'

const MONITOR_TYPE_MAP = {
    scriptQuality: 0,
    scriptRestoration: 1,
    interactionInspection: 2
}

const DETAIL_API_MAP = {
    scriptQuality: 'getQualityInspectionReport',
    interactionInspection: 'getInteractionPatrolReport',
    scriptRestoration: 'getFidelityMonitorReport'
}

export default {
    name: 'MonitorReportPanel',
    components: {
        InspectionEmptyPage,
        ScriptQualityReportContent,
        AiContentRenderer,
        ScriptRestorationDrawer,
        letterSpacing
    },
    mixins: [auth],
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        },
        reportType: {
            type: String,
            default: 'scriptQuality'
        },
        targetType: {
            type: String,
            default: ''
        },
        readonly: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            statusMap: {
                scriptQuality: '',
                interactionInspection: '',
                scriptRestoration: ''
            },
            contentMap: {
                scriptQuality: '',
                interactionInspection: '',
                scriptRestoration: []
            },
            monitorEnabledMap: {
                scriptQuality: '',
                interactionInspection: '',
                scriptRestoration: ''
            },
            reportIdMap: {
                scriptQuality: null,
                interactionInspection: null,
                scriptRestoration: null
            },
            inlineErrorMap: {
                scriptQuality: '',
                interactionInspection: '',
                scriptRestoration: ''
            },
            scriptRestorationDrawerVisible: false,
            scriptRestorationDrawerAction: 'generate',
            scriptRestorationDrawerForm: null,
            refreshTimer: null
        }
    },
    computed: {
        isScriptRestoration() {
            return this.reportType === 'scriptRestoration'
        },
        currentStatus() {
            return Number(this.statusMap[this.reportType] ?? 0)
        },
        currentContent() {
            return this.contentMap[this.reportType]
        },
        currentMonitorEnabled() {
            return this.monitorEnabledMap[this.reportType]
        },
        currentInlineErrorText() {
            return this.inlineErrorMap[this.reportType] || ''
        },
        restorationContentList() {
            const content = this.contentMap.scriptRestoration
            if (Array.isArray(content)) return content
            if (typeof content === 'string' && content.trim()) return [content]
            return []
        },
        emptyText() {
            return this.reportType === 'interactionInspection' ? '暂未生成互动巡检报告...' : '暂未生成话术质检报告...'
        },
        webText() {
            return this.reportType === 'interactionInspection' ? '请前往客户端获取互动巡检报告...' : '请前往客户端获取话术质检报告...'
        },
        noAuthText() {
            return this.reportType === 'interactionInspection' ? '没有生成互动巡检报告权限...' : '没有生成话术质检报告权限...'
        },
        pendingAiAnalysisText() {
            return '智能分析完成后才可生成话术质检报告...'
        },
        loadingText() {
            return this.reportType === 'interactionInspection'
                ? '互动巡检报告正在获取中，请五分钟后再查看...'
                : '话术质检报告正在获取中，请五分钟后再查看...'
        },
        failedText() {
            return this.reportType === 'interactionInspection' ? '互动巡检报告生成失败...' : '话术质检报告生成失败...'
        },
        canRetryGenerate() {
            return this.reportType === 'interactionInspection' || this.isAiAnalysisFinished
        },
        canShowInspectionEmpty() {
            return this.reportType === 'interactionInspection' || this.isAiAnalysisFinished
        },
        showMonitorDisabledEmpty() {
            const enabled = this.currentMonitorEnabled
            return Number(enabled) !== 1 && ![null, undefined, ''].includes(enabled) && this.currentStatus === 0
        },
        isAuthenticated() {
            const { videoInfo = {} } = this.sentenceMarkData
            return videoInfo?.UserId ? this.$auth([videoInfo?.UserId, videoInfo?.TenantId], 'every') : true
        },
        isAiAnalysisFinished() {
            const candidates = [
                this.sentenceMarkData?.analysisStatus,
                this.sentenceMarkData?.AnalysisStatus,
                this.sentenceMarkData?.videoInfo?.analysisStatus,
                this.sentenceMarkData?.videoInfo?.AnalysisStatus,
                this.sentenceMarkData?.fileInfo?.analysisStatus,
                this.sentenceMarkData?.fileInfo?.AnalysisStatus
            ]
            const raw = candidates.find((val) => val !== undefined && val !== null && val !== '')
            if (raw === undefined) return false
            return Number(raw) === 2
        },
        isOnlineAnalysisMonitorPage() {
            return String(this.$route?.path || '').includes('/onlineAnalysisMonitor')
        }
    },
    watch: {
        reportType: {
            immediate: true,
            handler(val) {
                this.refreshScriptMonitorStatus(val)
            }
        },
        sentenceMarkData: {
            deep: false,
            handler() {
                this.refreshScriptMonitorStatus(this.reportType)
            }
        }
    },
    beforeDestroy() {
        clearTimeout(this.refreshTimer)
    },
    methods: {
        /**
         * @description 获取报告状态接口的 sceneType，保持与旧详情页一致。
         * @returns {number}
         */
        getScriptMonitorSceneType() {
            const { sourceType } = getSourceData(this.sentenceMarkData)
            if (Number(sourceType) !== 1) return 0
            const querySceneType = this.$route?.query?.sceneType
            let sceneType = querySceneType !== undefined && querySceneType !== null && querySceneType !== '' ? Number(querySceneType) : NaN
            if (![1, 2].includes(sceneType)) {
                const fromData = this.sentenceMarkData?.sceneType
                sceneType = fromData !== undefined && fromData !== null && fromData !== '' ? Number(fromData) : NaN
            }
            if (![1, 2].includes(sceneType)) {
                const fileType = this.sentenceMarkData?.fileInfo?.fileType ?? this.sentenceMarkData?.fileInfo?.FileType
                if (Number(fileType) === 2) {
                    sceneType = 2
                } else if (fileType !== undefined && fileType !== null && fileType !== '') {
                    sceneType = 1
                }
            }
            if (![1, 2].includes(sceneType)) {
                const path = String(this.$route?.path || '')
                sceneType = path.includes('/uploadText/') ? 2 : 1
            }
            return sceneType
        },
        /**
         * @description 读取主播 secUid，供监控状态/开关接口定位主播维度的报告。
         * @returns {string}
         */
        getSecUid() {
            const anchorInfo = this.sentenceMarkData?.anchorInfo || {}
            return anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId || ''
        },
        setInline70011Error(type, error) {
            if (!this.isOnlineAnalysisMonitorPage || Number(error?.code) !== 70011) return false
            this.inlineErrorMap[type] = error?.msg || '当前报告暂无权限获取'
            this.statusMap[type] = 0
            this.reportIdMap[type] = null
            this.contentMap[type] = type === 'scriptRestoration' ? [] : ''
            return true
        },
        clearInline70011Error(type) {
            this.inlineErrorMap[type] = ''
        },
        /**
         * @description 刷新当前选中报告类型的状态；若已生成，则继续请求详情内容。
         * @param {string} type 报告类型
         * @returns {Promise<void>}
         */
        async refreshScriptMonitorStatus(type) {
            const { sourceType, sourceId } = getSourceData(this.sentenceMarkData)
            if (!type || !sourceId || !this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return
            try {
                this.clearInline70011Error(type)
                const secUid = this.getSecUid()
                const payload = {
                    sourceType,
                    sceneType: this.getScriptMonitorSceneType(),
                    sourceId
                }
                if (secUid) payload.secUid = secUid
                const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus({
                    ...payload
                })
                if (this.setInline70011Error(type, res)) return
                if (res?.code !== 0 || !Array.isArray(res?.data?.monitors)) return
                const item = res.data.monitors.find((it) => Number(it?.monitorType) === MONITOR_TYPE_MAP[type])
                if (!item) return
                const status = Number(item?.status ?? 0)
                const reportId = item?.reportId ? String(item.reportId) : null
                this.statusMap[type] = status
                this.monitorEnabledMap[type] = item?.monitorEnabled
                this.reportIdMap[type] = reportId
                if (status === 2 && reportId) {
                    await this.fetchReportDetail(type, reportId)
                }
            } catch (e) {
            }
        },
        /**
         * @description 获取单个报告详情内容，并写入对应报告容器。
         * @param {string} type 报告类型
         * @param {string} reportId 报告 ID
         * @returns {Promise<void>}
         */
        async fetchReportDetail(type, reportId) {
            const apiName = DETAIL_API_MAP[type]
            if (!apiName || !this.$httpBack?.scriptMonitor?.[apiName]) return
            try {
                const detailRes = await this.$httpBack.scriptMonitor[apiName]({ reportId })
                if (this.setInline70011Error(type, detailRes)) return
                if (detailRes?.code !== 0) return
                this.clearInline70011Error(type)
                if (type === 'scriptRestoration') {
                    const content = detailRes?.data?.reportContent
                    this.contentMap.scriptRestoration = Array.isArray(content) ? content : (content ? [content] : [])
                    return
                }
                this.contentMap[type] = detailRes?.data?.reportContent || ''
            } catch (e) {
            }
        },
        /**
         * @description 触发生成报告，并在短暂延迟后回刷状态。
         * @param {string} type 报告类型
         * @returns {Promise<void>}
         */
        async createInspectionReport(type, option = {}) {
            if (type === 'scriptQuality' && !this.isAiAnalysisFinished) return
            if (this.readonly || this.targetType === 'webOnline') return
            const { sourceType, sourceId } = getSourceData(this.sentenceMarkData)
            if (!sourceId || !this.$httpBack?.scriptMonitor?.triggerScriptMonitorReport) return
            const monitorType = MONITOR_TYPE_MAP[type]
            const labelMap = {
                scriptQuality: '话术质检报告',
                interactionInspection: '互动巡检报告',
                scriptRestoration: '话术还原度报告'
            }
            this.$message.success(`正在生成${labelMap[type] || '报告'}，请稍候...`)
            try {
                const res = await this.$httpBack.scriptMonitor.triggerScriptMonitorReport({
                    sourceType,
                    sceneType: this.getScriptMonitorSceneType(),
                    sourceId,
                    monitorType
                })
                if (res?.code === 0) {
                    this.statusMap[type] = 1
                    clearTimeout(this.refreshTimer)
                    this.refreshTimer = setTimeout(() => {
                        this.refreshScriptMonitorStatus(type)
                    }, 5000)
                    return
                }
                if (Number(res?.code) === 70001 && option?.keepOldOn70001 === true) {
                    if (res?.msg) this.$message.warning(res.msg)
                    return
                }
                if (res?.code === 70005 && type === 'scriptRestoration') {
                    this.$message.warning(res?.msg || '请先确认标准直播稿')
                    this.openScriptRestorationDrawer()
                    return
                }
                this.statusMap[type] = 3
            } catch (e) {
                if (Number(e?.code) === 70001 && option?.keepOldOn70001 === true) {
                    if (e?.msg) this.$message.warning(e.msg)
                    return
                }
                this.statusMap[type] = 3
            }
        },
        /**
         * @description 根据页面权限、平台与当前状态判断是否显示“开启自动监控”入口。
         * @param {string} type 报告类型
         * @param {number|string} monitorEnabled 当前自动监控开关值
         * @returns {boolean}
         */
        canEnableAutoMonitor(type, monitorEnabled) {
            if (Number(monitorEnabled ?? 0) === 1) return false
            if (this.readonly) return false
            if (this.$isWeb) return false
            if (!this.isSlefAuth) return false
            if (this.sentenceMarkData?.fileInfo?.fileId) return false
            if (Number(this.statusMap[type] ?? 0) === 2) return false
            return ['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(type)
        },
        /**
         * @description 处理空页面的次按钮动作：质检/巡检走开启自动监控，还原度直接打开标准稿抽屉。
         * @returns {Promise<void>}
         */
        async handleSecondaryClick() {
            if (this.reportType === 'scriptQuality') {
                await this.handleEnableMonitor({ type: 'scriptQuality', monitorType: 0 })
                return
            }
            if (this.reportType === 'interactionInspection') {
                await this.handleEnableMonitor({ type: 'interactionInspection', monitorType: 2 })
            }
        },
        /**
         * @description 开启自动质检/巡检监控，并在成功后刷新当前报告状态。
         * @param {{type:string, monitorType:number}} payload 动作参数
         * @returns {Promise<void>}
         */
        async handleEnableMonitor({ type, monitorType }) {
            if (this.readonly || this.targetType === 'webOnline') return
            const { sourceType } = getSourceData(this.sentenceMarkData)
            if (Number(sourceType) === 1) return this.$message.warning('文件分析仅支持手动生成报告')
            const anchorInfo = this.sentenceMarkData?.anchorInfo || {}
            const anchorName = anchorInfo?.anchorName || anchorInfo?.AnchorName || ''
            const secUid = this.getSecUid()
            if (!secUid || !this.$httpBack?.scriptMonitor?.setMonitorEnabled) return
            const configMap = {
                scriptQuality: {
                    dialogText: `${anchorName}-是否开启自动话术质检监控？`,
                    confirmText: '开启自动话术质检'
                },
                interactionInspection: {
                    dialogText: `${anchorName}-是否开启自动互动巡检监控？`,
                    confirmText: '开启自动互动巡检'
                }
            }
            const cfg = configMap[type]
            if (!cfg) return
            try {
                await this.$confirm(`
                    <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                        <div>${cfg.dialogText}</div>
                    </div>`, '友情提示', {
                    confirmButtonText: cfg.confirmText,
                    cancelButtonText: '知道了',
                    customClass: 'edit-file-name',
                    showClose: true,
                    showCancelButton: true,
                    closeOnClickModal: false,
                    closeOnPressEscape: false,
                    dangerouslyUseHTMLString: true,
                    center: true
                })
                const res = await this.$httpBack.scriptMonitor.setMonitorEnabled({
                    secUid: String(secUid),
                    monitorType,
                    enabled: 1
                })
                if (res?.code === 0) {
                    this.monitorEnabledMap[type] = 1
                    this.$message.success('已开启')
                    this.refreshScriptMonitorStatus(type)
                }
            } catch (e) {
            }
        },
        /**
         * @description 打开话术还原度标准稿抽屉。
         * @returns {void}
         */
        openScriptRestorationDrawer(action = 'generate') {
            const nextAction = action === 'enable' ? 'enable' : 'generate'
            const secUid = this.getSecUid()
            this.scriptRestorationDrawerAction = nextAction
            this.scriptRestorationDrawerForm = { secUid: String(secUid || '') }
            this.scriptRestorationDrawerVisible = true
        },
        async handleScriptRestorationConfirmed() {
            const secUid = this.getSecUid()
            if (!secUid || !this.$httpBack?.scriptMonitor?.setMonitorEnabled) return
            try {
                const res = await this.$httpBack.scriptMonitor.setMonitorEnabled({
                    secUid: String(secUid),
                    monitorType: 1,
                    enabled: 1
                })
                if (res?.code === 0) {
                    this.monitorEnabledMap.scriptRestoration = 1
                    this.$message.success('已开启')
                    this.refreshScriptMonitorStatus('scriptRestoration')
                    return
                }
                this.$message.warning(res?.msg || '开启失败')
            } catch (e) {
            }
        },
        optimizeReportFormat(type) {
            const currentType = type || this.reportType
            if (currentType !== 'scriptRestoration') {
                this.$message.warning('当前仅支持话术还原度报告优化格式')
                return
            }
            const list = this.restorationContentList
            if (!list.length) {
                this.$message.warning('暂无可优化的报告内容')
                return
            }
            this.$message.success('当前报告已启用智能格式渲染')
        },
        getExportData(type) {
            const t = type || this.reportType
            if (!t) return null
            if (Number(this.statusMap[t] ?? 0) !== 2) return null
            if (t === 'scriptRestoration') {
                const list = this.restorationContentList
                if (!list.length) return null
                return { reportType: t, contentList: list }
            }
            const content = this.contentMap[t]
            if (!content) return null
            return { reportType: t, content }
        }
    }
}
</script>

<style scoped>
.monitorReportPanel {
    height: 100%;
    min-height: 0;
}

.monitorReportPanelBody {
    height: 100%;
    min-height: 0;
    overflow-y: auto;
}
</style>

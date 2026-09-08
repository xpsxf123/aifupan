<!--
/**
 * @description AI监控报告弹窗（复用：话术质检/还原度/互动巡检等）。
 */
-->

<template>
    <el-dialog
        :title="dialogTitle"
        :visible.sync="dialogVisible"
        width="980px"
        top="60px"
        :close-on-click-modal="false"
        :append-to-body="true"
        custom-class="aiMonitorReportDialog">
        <div class="aiMonitorReportHeader">
            <div class="aiMonitorReportHeaderItem">标题：{{ reportHeader.liveTitle || row?.anchorInfo?.remarksName || row?.anchorInfo?.anchorName || '-' }}</div>
            <div class="aiMonitorReportHeaderItem">直播间：{{ reportHeader.anchorName || row?.anchorInfo?.anchorName || '-' }}</div>
            <div class="aiMonitorReportHeaderItem">时间：{{ reportHeader.liveTime || row?.startTime || '-' }}</div>
        </div>
        <div ref="contentBox" class="aiMonitorReportContent" @scroll="onScroll">
            <div class="aiMonitorReportText">
                <div v-if="loading">加载中...</div>
                <ScriptQualityReportContent v-else-if="['qc', 'inspect'].includes(type) && reportData?.reportContent" :report="reportData" />
                <template v-else-if="type === 'restore' && restorationContentList.length">
                    <div
                        v-for="(item, index) in restorationContentList"
                        :key="`restore-report-${index}`"
                        class="aiMonitorReportRestoreItem"
                    >
                        <AiContentRenderer
                            :content="item"
                            render-mode="mdTag"
                            :option="{ forceStyle: true, upgradePlainTable: true }"
                            :data-block-scope="`ai-monitor-report-dialog-${index}`"
                        />
                    </div>
                </template>
                <div v-else-if="renderHtml" v-html="renderHtml"></div>
                <div v-else>{{ renderText || '暂无报告内容' }}</div>
            </div>
        </div>
        <div class="aiMonitorReportFooter" v-if="!readonly">
            <div class="aiMonitorReportFooterActions">
                <afp-button
                    type="primary"
                    :plain="false"
                    size="default"
                    :disabled="!canConfirm"
                    @click="confirmRead">
                    已看完整个报告
                </afp-button>
            </div>
        </div>
    </el-dialog>
</template>

<script>
import ScriptQualityReportContent from '../scriptQualityReportContent/index.vue'
import AiContentRenderer from '@/components/aiContentRenderer/index.vue'
import aiMonitorUnreadStore from '@/utils/aiMonitorUnreadStore'

export default {
    components: { ScriptQualityReportContent, AiContentRenderer },
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        type: {
            type: String,
            default: ''
        },
        row: {
            type: Object,
            default: () => null
        },
        content: {
            type: String,
            default: ''
        },
        reportId: {
            type: [Number, String, null],
            default: null
        },
        readonly: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            reachedBottom: false,
            loading: false,
            reportData: null,
            fetchingReportId: null
        }
    },
    computed: {
        dialogVisible: {
            get() {
                return this.visible
            },
            set(val) {
                this.$emit('update:visible', val)
            }
        },
        dialogTitle() {
            const map = {
                qc: '话术质检报告',
                restore: '话术还原度报告',
                inspect: '互动巡检报告'
            }
            return map[this.type] || '报告'
        },
        currentReportId() {
            const id = this.reportData?.reportId || this.reportId
            if (id === null || id === undefined || id === '') return null
            return String(id)
        },
        canConfirmByPermission() {
            if (this.readonly) return false
            if (this.reportData?.canConfirm === false) return false
            return true
        },
        canConfirm() {
            return this.readonly ? true : (this.reachedBottom && this.canConfirmByPermission)
        },
        reportHeader() {
            return {
                anchorName: this.reportData?.anchorName || '',
                liveTitle: this.reportData?.liveTitle || '',
                liveTime: this.reportData?.liveTime || ''
            }
        },
        restorationContentList() {
            const content = this.reportData?.reportContent
            if (Array.isArray(content)) return content.map((item) => String(item ?? '')).filter(Boolean)
            if (content === null || content === undefined || content === '') return []
            return [String(content)]
        },
        renderHtml() {
            if (this.type === 'restore') return ''
            const html = this.reportData?.reportContent
            if (!html) return ''
            return String(html)
                .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
                .replace(/\son\w+="[^"]*"/gi, '')
                .replace(/\son\w+='[^']*'/gi, '')
        },
        renderText() {
            const t = this.content
            if (!t) return ''
            return String(t)
        }
    },
    watch: {
        visible(val) {
            if (!val) {
                this.setDialogWrapperScroll(false)
                return
            }
            this.reportData = null
            this.reachedBottom = false
            aiMonitorUnreadStore.remove(this.reportId)
            this.$nextTick(() => {
                this.setDialogWrapperScroll(true)
                this.fetchReport()
                this.$emit('opened')
            })
        },
        reportId() {
            if (!this.visible) return
            this.fetchReport()
        }
    },
    methods: {
        setDialogWrapperScroll(enableContentScroll) {
            const box = this.$refs.contentBox
            const wrapper = box?.closest ? box.closest('.el-dialog__wrapper') : null
            if (!wrapper) return
            if (enableContentScroll) {
                wrapper.classList.add('aiMonitorReportDialogWrapper')
            } else {
                wrapper.classList.remove('aiMonitorReportDialogWrapper')
            }
        },
        async fetchReport() {
            if (this.readonly && !this.reportId && !this.reportData?.reportId) return
            const id = this.currentReportId
            if (!id) return
            if (!this.$httpBack?.scriptMonitor) return
            if (this.loading && this.fetchingReportId === id) return
            this.loading = true
            this.fetchingReportId = id
            try {
                let res = null
                if (this.type === 'qc') {
                    res = await this.$httpBack.scriptMonitor.getQualityInspectionReport({ reportId: id })
                } else if (this.type === 'inspect') {
                    res = await this.$httpBack.scriptMonitor.getInteractionPatrolReport({ reportId: id })
                } else if (this.type === 'restore') {
                    res = await this.$httpBack.scriptMonitor.getFidelityMonitorReport({ reportId: id })
                }
                if (res?.code === 0) {
                    this.reportData = res.data || null
                    aiMonitorUnreadStore.remove(this.reportData?.reportId || id)
                    this.$emit('report-read', { reportId: String(this.reportData?.reportId || id), type: this.type })
                }
            } finally {
                this.loading = false
                if (this.fetchingReportId === id) this.fetchingReportId = null
            }
        },
        onScroll() {
            const el = this.$refs.contentBox
            if (!el) return
            const buffer = 8
            this.reachedBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - buffer
        },
        async confirmRead() {
            const id = this.currentReportId
            if (!id) return
            if (this.reportData) {
                this.reportData.isRead = true
            }
            aiMonitorUnreadStore.remove(id)
            this.$emit('confirmed', { reportId: id, type: this.type })
            this.dialogVisible = false
        }
    }
}
</script>

<style lang="scss" scoped>
@import "./index.scss";
</style>

<style lang="scss">
.aiMonitorReportDialog {
    max-height: calc(100vh - 120px);
    display: flex;
    flex-direction: column;
}

.aiMonitorReportDialog .el-dialog__body {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.aiMonitorReportDialogWrapper {
    overflow-y: hidden !important;
    overflow-x: hidden !important;
}
</style>


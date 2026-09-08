<template>
    <el-dialog
        :title="dialogTitle"
        :visible.sync="dialogVisible"
        width="980px"
        top="60px"
        :close-on-click-modal="false"
        :append-to-body="true">
        <div style="background: #F7F7F7;" class="brs-10 pd-b10">
            <div class="aiMonitorReportHeader">
                <div class="aiMonitorReportHeaderItem">标题：{{ row?.anchorInfo?.remarksName || row?.anchorInfo?.anchorName || '-' }}</div>
                <div class="aiMonitorReportHeaderItem">直播间：{{ row?.anchorInfo?.anchorName || '-' }}</div>
                <div class="aiMonitorReportHeaderItem">时间：{{ row?.startTime || '-' }}</div>
            </div>
            <div ref="contentBox" class="aiMonitorReportContent" @scroll="onScroll">
                <div class="aiMonitorReportText">
                    {{ content || '暂无报告内容' }}
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                    <br><br><br><br><br><br><br><br><br><br><br><br>
                </div>
            </div>
        </div>
        <div class="aiMonitorReportFooter">
            <el-checkbox v-model="checkedOps">运营已知晓</el-checkbox>
            <el-checkbox v-model="checkedAnchor">主播已知晓</el-checkbox>
            <el-checkbox v-model="checkedManager">主管已知晓</el-checkbox>
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
export default {
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
        }
    },
    data() {
        return {
            checkedOps: false,
            checkedAnchor: false,
            checkedManager: false,
            reachedBottom: false
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
        canConfirm() {
            return this.reachedBottom
        }
    },
    watch: {
        visible(val) {
            if (!val) return
            this.checkedOps = false
            this.checkedAnchor = false
            this.checkedManager = false
            this.reachedBottom = false
            this.$nextTick(() => {
                this.$emit('opened')
            })
        }
    },
    methods: {
        onScroll() {
            const el = this.$refs.contentBox
            if (!el) return
            const buffer = 8
            this.reachedBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - buffer
        },
        confirmRead() {
            this.$message.success('已确认')
        }
    }
}
</script>

<style lang="scss" scoped>
.aiMonitorReportHeader{
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 16px;
    border-radius: 8px;
    margin-bottom: 12px;
}

.aiMonitorReportHeaderItem{
    font-size: 12px;
    color: #333;
}

.aiMonitorReportContent{
    width: calc(100% - 20px);
    margin: 0 auto;
    height: 500px;
    overflow: auto;
    background: #fff;
    border-radius: 8px;
    padding: 14px 16px;
}

.aiMonitorReportText{
    font-size: 12px;
    line-height: 18px;
    color: #333;
    white-space: pre-wrap;
}

.aiMonitorReportFooter{
    display: flex;
    align-items: center;
    gap: 16px;
    margin-top: 14px;
}

.aiMonitorReportFooterActions{
    margin-left: auto;
}
</style>


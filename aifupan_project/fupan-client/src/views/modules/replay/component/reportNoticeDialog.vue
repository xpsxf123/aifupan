<template>
    <el-dialog
        :visible.sync="dialogVisible"
        class="report-notice-dialog"
        width="620px"
        title="温馨提示"
        :append-to-body="true"
        :destroy-on-close="true"
        :close-on-click-modal="false"
        @close="handleClose">
        <div class="notice-dialog">
            <div class="dialog-banner flex items-center justify-between">
                <div class="banner-left flex items-center">
                    <i class="el-icon-bell banner-icon"></i>
                    <span>{{ currentSummaryText }}</span>
                </div>
                <afp-button type="primary" :plain="false" @click="markAllRead">
                    全部标记为已读
                </afp-button>
            </div>

            <el-table
                v-loading="loading"
                :data="currentRecords"
                class="notice-table"
                max-height="420"
                empty-text="暂无待查看的数据诊断报告">
                <el-table-column label="直播间" min-width="150">
                    <template slot-scope="{ row }">
                        <div class="room-cell flex items-center">
                            <el-avatar :size="36" :src="row.avatar" style="flex-shrink: 0;"/>
                            <span class="room-name">{{ row.roomName || '--' }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="recordTime" label="录制时间" min-width="130" align="center"/>
                <el-table-column label="数据诊断报告" width="120" align="center">
                    <template slot-scope="{ row }">
                        <el-button type="text" class="view-button" @click="handleView(row)">查看</el-button>
                    </template>
                </el-table-column>
                <el-table-column label="阅读状态" width="130" align="center">
                    <template slot-scope="{ row }">
                        <span :class="['read-status', { 'is-read': Number(row.readStatus) === 1 }]">
                            {{ Number(row.readStatus) === 1 ? '已读' : '未读' }}
                        </span>
                    </template>
                </el-table-column>
            </el-table>
            <DialogAiContent
                ref="dialog_ai_report" :tabsList="tabsList"
                :isReport="false"
                :uploadType="2"
                :selectedRow="selectedRow"
                :sponsorship="''"/>
        </div>
    </el-dialog>
</template>

<script>

import DialogAiContent from "@/views/commonComponent/aiReport/dialogAIContent.vue";
import myUtils from "@/utils/utils";

export default {
    name: 'ReportNoticeDialog',
    components: {DialogAiContent},
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        loading: {
            type: Boolean,
            default: false
        },
        summaryText: {
            type: String,
            default: ''
        },
        records: {
            type: Array,
            default: () => ([])
        }
    },
    data() {
        return {
            currentRecords: [],
            tabsList: [],
            selectedRow: {}
        }
    },
    computed: {
        dialogVisible: {
            get() {
                return this.visible
            },
            set(value) {
                this.$emit('update:visible', value)
            }
        },
        unreadCount() {
            return this.currentRecords.filter(item => Number(item.readStatus) !== 1).length
        },
        currentSummaryText() {
            if (this.summaryText) {
                return this.summaryText
            }
            return `您最近有${this.unreadCount}场直播生成了AI数据诊断报告，请及时查看`
        }
    },
    watch: {
        records: {
            handler(value = []) {
                this.currentRecords = value.map(item => ({...item}))
            },
            immediate: true
        }
    },
    methods: {
        handleClose() {
            this.$emit('update:visible', false)
            this.$emit('close')
        },
        async updateReadStatus(ids = []) {
            if (!ids.length) return true
            const result = await this.$httpBack.v2500.updateReadStatus({
                ids,
                isRead: 1
            })
            return result.code === 0
        },
        async markAllRead() {
            const ids = this.currentRecords
                .filter(item => Number(item.readStatus) !== 1 && item.id)
                .map(item => item.id)
            const isSuccess = await this.updateReadStatus(ids)
            if (!isSuccess) return
            this.currentRecords = this.currentRecords.map(item => ({
                ...item,
                readStatus: 1
            }))
        },
        async handleView(record) {
            if (Number(record.readStatus) !== 1 && record.id) {
                await this.updateReadStatus([record.id])
            }
            const currentRecord = this.currentRecords.find(item => item.id === record.id)
            if (currentRecord) {
                currentRecord.readStatus = 1
            }

            this.tabsList = [{
                tagName: "AI数据诊断",
                name: 0,
                ids: [record?.cueWordsId], cueWordsList: [{
                    cueWordsId: record?.cueWordsId,
                    cueWord: "",
                }]
            }]

            this.selectedRow = {
                anchorInfo: {anchorName: record.anchorName},
                startTime: record.startTime,
                durationStr: myUtils.toformatTimeChinse(record.duration * 1000),
                outputName: record.anchorName,
                reportFileName: record.anchorName,
                videoId: record.videoId,
                tradeId: record.tradeId,
            }

            this.$nextTick(() => {
                this.$refs.dialog_ai_report?.changeDialogVisible(this.tabsList, {0: [record?.cueWordsId]}, true)
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.report-notice-dialog {
    z-index: 9999!important;

    ::v-deep .el-dialog {
        border-radius: 16px;
        overflow: hidden;
        box-shadow: 0 24px 60px rgba(37, 52, 104, 0.18);
    }

    ::v-deep .el-dialog__header {
        height: 48px;
        padding: 14px 26px 0 34px;
        background: #f4f9ff;
    }

    ::v-deep .el-dialog__body {
        padding: 0;
    }
}

.notice-dialog {
    background: #fff;
}

.title {
    color: #303133;
    font-size: 16px;
}

.close-button {
    display: inline-flex;
    width: 26px;
    height: 26px;
    padding: 0;
    border: 0;
    background: transparent;
    color: #98a2c3;
    font-size: 24px;
    cursor: pointer;
}

.dialog-banner {
    padding: 12px 24px;
}

.banner-left {
    min-width: 0;
    color: #5a67ff;
    font-size: 14px;
}

.banner-icon {
    margin-right: 10px;
    font-size: 18px;
}

.notice-table {
    padding: 0 2px 22px 22px;
    margin-bottom: 6px;

    ::v-deep .el-table__header th {
        color: #b0b7c3;
        font-weight: 400;
        background: #fff;
    }

    ::v-deep .el-table__cell {
        border-bottom-color: #eef2ff;
    }
}

.room-cell {
    min-width: 0;
}

.room-name {
    margin-left: 10px;
    overflow: hidden;
    color: #303133;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.view-button {
    color: #5a67ff;
}

.read-status {
    color: #c0c4cc;
}

.read-status.is-read {
    color: #909399;
}
</style>

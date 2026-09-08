<template>
    <div class="performance-box" v-loading.lock="dataLoading">
        <el-button type="text" class="refresh" @click="getDataScreenshotList" v-if="performanceGroups.length">刷新
        </el-button>
        <div v-if="!dataLoading && performanceGroups.length" class="performance-panel">
            <div
                v-for="group in performanceGroups"
                class="performance-group"
            >
                <div class="group-title">{{ group.label }}</div>
                <div class="metric-list">
                    <div v-for="item in group.value" :key="item.key" class="metric-item">
                        <div class="metric-label">{{ item.label }}</div>
                        <div class="metric-value">{{ formatDisplayValue(item) }}</div>
                    </div>
                </div>
            </div>
        </div>
        <div v-else-if="!dataLoading" class="empty-box">
            <img src="@/assets/imgs/chartEmpty.png" alt="">
            <div>暂无数据</div>
        </div>
        <div class="flex items-center justify-between btn">
            <span v-if="updateDate" class="text-xs"
                  style="color: var(--color-main);font-weight: 500">*业绩统计时间：{{ updateDate }}</span>
            <afp-button v-if="updateUrl" size="medium" @click="openUrl">编辑</afp-button>
        </div>
    </div>
</template>

<script>


export default {
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        },
        isContrast: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            performanceGroups: [],
            dataLoading: false,
            updateDate: '',
            updateUrl: ''
        }
    },
    watch: {
        sentenceMarkData: {
            handler(newVal) {
                if (!newVal) return
                const sourceKind = String(this.$route?.query?.sourceKind || '')
                const path = String(this.$route?.path || '')
                const hasUploadFileData = !!(newVal?.uploadFile?.fileId || newVal?.fileInfo?.fileId)
                const isUploadSource = sourceKind === 'uploadFile'
                    || hasUploadFileData
                    || path.startsWith('/uploadVideo')
                    || path.startsWith('/uploadText')
                    || path.startsWith('/uploadSlice')
                    || path.startsWith('/uploadShort')
                if (isUploadSource) return
                const {VideoId = ''} = newVal?.videoInfo || {}
                const {fileId = ''} = newVal?.uploadFile || {}
                if (VideoId || fileId) {
                    this.getDataScreenshotList()
                }
            },
            immediate: true,
            deep: true
        }
    },
    computed: {
        getLevel() {
            return this.$store.getters.getPackageLevel
        }
    },
    methods: {
        formatDisplayValue(item) {
            const value = item?.value
            const unit = item?.unit || ''
            if (value === undefined || value === null || value === '') return '-'
            if (unit) return `${value}${unit}`
            if (typeof value === 'number') return this.formatNumberValue(value)
            if (!Number.isNaN(Number(value)) && value !== '') return this.formatNumberValue(Number(value))
            if (Array.isArray(value)) return value.map(entry => this.formatDisplayValue({
                value: entry,
                unit: ''
            })).join(', ')
            if (typeof value === 'object') return JSON.stringify(value)
            return value
        },
        formatNumberValue(value) {
            if (Math.abs(value) > 10000) {
                return `${(value / 10000).toFixed(2)}W`
            }
            return value.toLocaleString('zh-CN', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
            })
        },
        openUrl() {
            if(!this.updateUrl) return
            if (this.getLevel < 20) {
                this.$confirm(`
                    <div class="flex items-center justify-center" style="height: 80px">
                        <div>只有企业版及以上会员才能查看</div>
                    </div>
                `, '友情提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    showCancelButton: false,
                    dangerouslyUseHTMLString: true,
                    customClass: 'edit-file-name',
                    distinguishCancelAndClose: true,
                    closeOnClickModal: false,
                    closeOnPressEscape: false,
                    showClose: false,
                    center:true
                }).then(async () => {

                }).catch(() => {

                });
            } else {
                this.$httpClient.system.openUrl({url: this.updateUrl});
            }
        },
        getDataScreenshotList() {
            const sourceKind = String(this.$route?.query?.sourceKind || '')
            const path = String(this.$route?.path || '')
            const hasUploadFileData = !!(this.sentenceMarkData?.uploadFile?.fileId || this.sentenceMarkData?.fileInfo?.fileId)
            const isUploadSource = sourceKind === 'uploadFile'
                || hasUploadFileData
                || path.startsWith('/uploadVideo')
                || path.startsWith('/uploadText')
                || path.startsWith('/uploadSlice')
                || path.startsWith('/uploadShort')
            if (isUploadSource) {
                this.performanceGroups = []
                this.updateDate = ''
                this.updateUrl = ''
                this.dataLoading = false
                return
            }
            this.dataLoading = true
            const {videoInfo = {}, anchorInfo = {}} = this.sentenceMarkData
            this.$httpBack2?.liveRoom?.querySchedulePerformance({
                secUid: anchorInfo.SecUid,
                startTime: videoInfo.StartTime,
                endTime: videoInfo.EndTime,
            }).then(res => {
                if (res.code === 0) {
                    this.updateDate = res?.data?.updateDate
                    this.updateUrl = res?.data?.updateUrl
                    this.performanceGroups = res?.data?.clientPerformanceResponse || []
                } else {
                    this.performanceGroups = []
                }
            }).catch(() => {
                this.performanceGroups = []
            }).finally(() => {
                this.dataLoading = false
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.performance-box {
    min-height: 180px;
    padding: 6px 20px 14px;
    position: relative;

    .refresh {
        position: absolute;
        right: 20px;
        top: -32px;
    }

    .btn {
        position: absolute;
        right: 0;
        bottom: 4px;
        width: 310px;
    }

    .performance-panel {
        display: flex;
        align-items: stretch;
        gap: 12px;
        width: 100%;
        overflow-x: auto;
        padding: 8px 0 20px;
        flex-wrap: wrap;
    }

    .performance-group {
        display: flex;
        align-items: stretch;
        min-height: 70px;
        min-width: 260px;
        border: 1px solid #DFEAF6;
        border-radius: 4px;
        background: #FFFFFF;
        flex-shrink: 0;
    }

    .group-title {
        width: 86px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        font-size: 14px;
        font-weight: 500;
        color: #151719;
        background: #F4F9FF;
        border-radius: 4px 0 0 4px;
        word-break: break-all;
        text-align: center;
        padding: 8px;
    }

    .metric-list {
        display: flex;
        align-items: center;
        gap: 22px;
        padding: 12px 20px;
        flex-wrap: wrap;
    }

    .metric-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 2px;
        min-width: 56px;
        line-height: 22px;
    }

    .metric-label {
        color: #7A7C80;
        font-size: 14px;
        font-weight: 400;
        word-break: break-all;
        text-align: center;
    }

    .metric-value {
        color: #151719;
        font-size: 14px;
        font-weight: 500;
        word-break: break-all;
        text-align: center;
    }

    .empty-box {
        min-height: 170px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: #7A7C80;
        font-size: 14px;

        img {
            max-height: 160px;
            margin-bottom: 12px;
        }
    }
}
</style>

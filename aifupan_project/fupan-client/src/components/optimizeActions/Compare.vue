<template>
    <div class="contrastLastTime">
        <el-dialog
            title="对比上一场"
            custom-class="contrastLastTimeDialog"
            :visible.sync="dialogVisible"
            :close-on-press-escape="false"
            :close-on-click-modal="false"
            @close="close"
            width="520px">
            <div>
                <div class="first-video">
                    <div>本场次：</div>
                    <div>视频2</div>
                </div>
                <div class="first-video video-info">
                    <div>{{ getVideoName(rowItem?.videoName || '') }}</div>
                    <div>时长：{{ rowItem?.durationStr }}</div>
                </div>

                <div class="last-video" style="margin-top: 20px">
                    <div>上一场：</div>
                    <div>视频1
                        <span v-if="contrastList.length>1">（存在多段，请选择）</span>
                    </div>
                </div>
                <div v-if="!hasHistory" style="margin-top: 8px;font-weight: 500">目前只有一场直播，暂时不能创建对比复盘
                </div>
                <template v-if="hasHistory">
                    <div style="color: red;margin-top: 8px" v-if="contrastList.length===0">无数据</div>
                    <template v-if="contrastList.length===1">
                        <div class="flex" style="margin-top: 10px;font-weight: 500"
                             v-for="(item, index) in contrastList" :key="index">
                            <div>{{ getVideoName(item.videoName || '') }}</div>
                            <div>时长：{{ duration(item) }}</div>
                        </div>
                    </template>
                    <template v-if="contrastList.length>1">
                        <el-radio-group v-model="selectRadio" style="width: 100%;margin-top: 10px">
                            <div class="flex" style="padding: 6px 0"
                                 v-for="(item, index) in contrastList" :key="index">
                                <el-radio :label="item.videoId" class="flex" style="flex: 1">
                                    <div class="flex">
                                        <div>{{ getVideoName(item.videoName || '') }}</div>
                                        <div>时长：{{ duration(item) }}</div>
                                    </div>
                                </el-radio>
                            </div>
                        </el-radio-group>
                    </template>
                </template>
            </div>
            <span slot="footer" class="dialog-footer">
                <afp-button @click="handleOk" type="primary" :plain="false"
                            :disabled="!selectRadio">确认生成对比</afp-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import myUtils from '/src/utils/utils'

export default {
    name: '',
    props: {
        rowItem: {
            type: Object,
            default: () => {
            }
        }
    },
    computed: {
        duration() {
            return (item) => myUtils.toformatTime(item.duration * 1000)
        },
        getVideoName() {
            return (item) => {
                return item ? item.split('_')?.slice(-2)?.join('')?.replace(/\.[^.]+$/, '') : ''
            }
        },
        getReplayType() {
            if (this.rowItem?.videoSliceType === 0) {
                return 'replayAll'
            }
            if (type === 1) {
                return 'replaySection'
            }
        },
    },
    watch: {},
    data() {
        return {
            dialogVisible: false,
            selectRadio: '',
            contrastList: [],
            hasHistory: false
        }
    },
    mounted() {

    },
    created() {

    },
    methods: {
        async getInitData() {
            const result = await this.$httpBack.v2400.historyBatchNumberVideoList({
                videoId: this.rowItem.videoId,
                dataType: 2,
                uploadStatus: 0
            })
            const videoList = result.data || []
            const historyList = videoList?.filter(item => item?.videoId) || []
            this.hasHistory = historyList?.length > 0
            if (historyList.length === 1) {
                this.selectRadio = historyList[0]?.videoId
            }
            this.contrastList = historyList
        },
        close() {
            this.dialogVisible = false
            this.$emit('closeDialog')
        },
        async openDialog() {
            await this.getInitData()
            this.dialogVisible = true
        },
        async handleOk() {
            const selectSentenceMarkData = this.contrastList.find(item => item?.videoId === this.selectRadio)
            const {secUid: anchorTwoId, videoId: videoTwoId} = selectSentenceMarkData
            const requestData = {
                anchorOneId: this.rowItem?.secUid,
                anchorTwoId: anchorTwoId,
                videoOneId: this.rowItem?.videoId,
                videoTwoId: videoTwoId,
                sliceContrastType: 0,
                contrastType: 0,
                syncScene: 1
            }
            if (this.getReplayType === 'replaySection') {
                requestData.contrastType = 1
                requestData.sliceContrastType = 1
            }
            let result = await this.$httpBack.contrast.clientAddContrast(requestData)
            if (result.code !== 0) return this.$message.error('添加对比失败')
            this.callback?.()
            const prentRoute = this.getReplayType === 'replayAll' ? '/contrastReplay' : '/contrastReplay'
            this.$router.push({
                path: `${prentRoute}/analysis`,
                query: {contrastId: result.data}
            })
        }
    }
}
</script>

<style scoped>
.contrastLastTime {
    .first-video {
        display: flex;
        align-items: center;
        margin-bottom: 10px;
    }

    .video-info {
        justify-content: space-between;
        font-weight: 500;
    }

    .flex {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    .last-video {
        display: flex;
        align-items: center;
        margin-top: 10px;
    }

    ::v-deep(.el-radio__label) {
        width: 100%;
    }
}
</style>
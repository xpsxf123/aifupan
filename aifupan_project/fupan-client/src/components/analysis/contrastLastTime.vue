<template>
    <div class="contrastLastTime">
        <el-dialog
            title="对比上一场"
            custom-class="contrastLastTimeDialog"
            :visible.sync="dialogVisible"
            width="520px">
            <div>
                <div class="first-video">
                    <div>本场次：</div>
                    <div>视频2</div>
                </div>
                <div class="first-video video-info">
                    <div>{{ getVideoName(sentenceMarkData?.videoInfo?.VideoName || '') }}</div>
                    <div>时长：{{ sentenceMarkData?.videoInfo?.Duration }}</div>
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
                <afp-button  @click="handleOk" type="primary" :plain="false"
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
        sentenceMarkData: {
            type: Object,
            default: () => {}
        },
        aiType: {
            type: String,
            default: ''
        },
        targetType: {
            type: String,
            default: ''
        },
        isContrast: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        duration () {
            return (item) => myUtils.toformatTime(item.duration * 1000)
        },
        getVideoName () {
            return (item) => {
                return item ? item.split('_')?.slice(-2)?.join('')?.replace(/\.[^.]+$/, '') : ''
            }
        },
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
    },
    watch: {},
    data () {
        return {
            dialogVisible: false,
            selectRadio: '',
            contrastList: [],
            hasHistory: false
        }
    },
    mounted () {

    },
    created () {

    },
    methods: {
        async getInitData () {
            let dataType = null
            if (this.aiType === 'dataCapture') {
                dataType = 0
            } else if (this.aiType === 'dataBoard') {
                dataType = 1
            } else {
                dataType = 2
            }
            const result = await this.$httpBack.v2400.historyBatchNumberVideoList({
                videoId: this.sentenceMarkData.videoInfo.VideoId,
                dataType,
                uploadStatus: this.targetType === 'online' ? 1 : 0
            })
            const videoList = result.data || []
            const historyList = videoList?.filter(item => item?.videoId) || []
            this.hasHistory = historyList?.length > 0
            if (historyList.length === 1) {
                this.selectRadio = historyList[0]?.videoId
            }
            this.contrastList = historyList
        },
        async openDialog () {
            await this.getInitData()
            this.dialogVisible = true
        },
        async handleOk () {
            const { anchorInfo, videoInfo } = this.sentenceMarkData
            const selectSentenceMarkData = this.contrastList.find(item => item?.videoId === this.selectRadio)
            const { secUid: anchorTwoId, videoId: videoTwoId } = selectSentenceMarkData
            const requestData = {
                anchorOneId: anchorInfo?.SecUid,
                anchorTwoId: anchorTwoId,
                videoOneId: videoInfo?.VideoId,
                videoTwoId: videoTwoId,
                sliceContrastType: 0,
                contrastType:0,
                syncScene:1
            }
            if (this.getReplayType === 'replaySection') {
                requestData.contrastType = 1
                requestData.sliceContrastType = 1
            }
            let result = {}
            if (this.targetType === 'online') {
                result = await this.$httpBack.contrast.clientAddCloudContrast(requestData)
            } else {
                result = await this.$httpBack.contrast.clientAddContrast(requestData)
            }
            if (result.code !== 0) return this.$message.error('添加对比失败')

            //2025-12-11 15：53 小胡说的不用了 出问题了 我不承认是我的错
            // if (this.targetType === 'online') {
            //     const resultContrast = await this.$httpBack.contrast.shareContrastToCloud({ contrastId: result.data })
            //     if (resultContrast.code !== 0) return this.$message.error('添加对比失败')
            // }
            if (this.targetType === 'online') {
                const path = this.$route.path.split('/').slice(0, -1).join('/');
                this.$router.push({
                    path: this.isContrast ? `${path}/contrast` : `${path}/aiContrast`,
                    query: { contrastId: result.data, type: this.aiType }
                })
            } else {
                const prentRoute = this.getReplayType === 'replayAll' ? '/contrastReplay' : '/contrastReplay'
                this.$router.push({
                    path: this.isContrast ? `${prentRoute}/analysis` : `${prentRoute}/aiAnalysis`,
                    query: { contrastId: result.data, type: this.aiType }
                })
            }
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
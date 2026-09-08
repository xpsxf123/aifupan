<template>
    <div class="aiTrain">
        <div v-if="showAiButton">
            <afp-button 
                type="primary" 
                class="ai-btn" 
                round 
                size="small"
                @click="handleAiButtonClick"
            >
                {{ aiButtonText }}
            </afp-button>
        </div>

        <el-dialog :visible.sync="aiDialogVisible" :close-on-click-modal="false" :custom-class="getCustomClass"
            :show-close="false">
            <div class="item" v-for="(item, index) in getTrainText" :key="index">{{ item }}</div>
            <div slot="footer" class="dialog-footer">
                <afp-button 
                    round 
                    size="medium" 
                    @click="aiDialogVisible = false" 
                    class="notTrain"
                >
                    {{ cancelButtonText }}
                </afp-button>
                <afp-button 
                    round 
                    size="medium" 
                    type="primary" 
                    class="aiTrainBtn"
                    @click="handleTrainButton"
                >
                    {{ trainButtonText }}
                </afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import { isEmpty } from 'lodash';

export default {
    name: "AiTrain",
    props: {
        videoInfo: {
            type: Object,
            required: true,
            default: {}
        },
        sentenceMarkData: {
            type: Object,
            default: {}
        },
    },
    computed: {
        showAiButton() {
            return this.isTrained !== undefined && this.isTraining !== undefined;
        },
        aiButtonText() {
            if (!this.isTrained && this.isTraining) return 'AI训练中';
            if (this.isTrained && !this.isTraining) return 'AI训练完成';
            return 'AI训练';
        },
        cancelButtonText() {
            return this.isTrained && !this.isTraining ? '取消' : '不训练';
        },
        trainButtonText() {
            return this.isTrained && !this.isTraining ? '再分析' : '马上训练';
        },
        getCustomClass() {
            if (this.isTrained && !this.isTraining) {
                return 'ai-trained-dialog ai-train-dialog'
            }
            return 'ai-training-dialog ai-train-dialog'
        },
        getTrainText() {
            if (this.isTrained && !this.isTraining) {
                return [
                    `本次AI训练已完成，进步 ${((this.aiStatusData.progressRange || 0.011) * 100).toFixed(2)}% `,
                    '您可重新分析查看最新结果',
                    '如果分析结果没有变化，有可能是训练得不够，需要提供更多资源进行训练'
                ]
            }
            return [
                '训练专属于您的AI助手，有助于分析的个性化',
                '本次训练为赠送服务，预计消耗时间24小时到48小时',
                '训练完成后，点击重新分析后即可查最新的分析结果'
            ]

        }
    },
    data() {
        return {
            aiDialogVisible: false,
            isTrained: undefined,
            isTraining: undefined,
            aiStatusData: {}
        };
    },
    mounted() { },
    created() { },
    methods: {
        handleAiButtonClick() {
            this.aiDialogVisible = true;
        },
        handleTrainButton() {
            if (this.isTrained && !this.isTraining) {
                this.treeChange();
            } else {
                this.initiateTrainClick();
            }
        },
        async getAiTrainStatus() {
            try {
                const { VideoId } = this.videoInfo;
                if (!VideoId) return;
                
                const res = await this.$httpBack.aiTrain.aiTrainStatus({ videoId: VideoId });
                if (res.code === 0) {
                    this.updateTrainingStatus(res.data);
                }
            } catch (error) {
                console.error('获取AI训练状态失败:', error);
            }
        },
        updateTrainingStatus(data) {
            if (isEmpty(data)) {
                this.isTraining = false;
                this.isTrained = false;
                return;
            }
            this.aiStatusData = data;
            this.isTraining = data.aiStatus === 0;
            this.isTrained = data.aiStatus !== 0;
        },
        initiateTrainClick() {
            this.$httpBack.aiTrain.initiateTrain({
                videoId: this.videoInfo.VideoId
            }).then((res) => {
                if (res && res.code === 0) {
                    this.aiDialogVisible = false;
                    this.isTrained = false;
                    this.isTraining = true;
                } else {
                    this.$message.error(res.msg);
                }
            });
        },
        treeChange() {
            const { videoInfo = {} } = this.sentenceMarkData
            this.$emit('treeChange', videoInfo?.TradeId)
            this.aiDialogVisible = false
        }
    },
    watch: {
        'videoInfo.VideoId': {
            immediate: true,
            handler(newVal) {
                if (newVal) {
                    this.getAiTrainStatus();
                }
            }
        }
    },
};
</script>

<style lang="scss">
$gradient-primary: linear-gradient(90deg, #01C3CC 0%, #0077FF 53%, #915DD9 100%);
$gradient-primaryed: linear-gradient(0deg, #01C3CC 0%, #0077FF 53%, #915DD9 100%);
$primary-text-color: #151719;
$button-width: 100px;
$dialog-width: 450px;
$dialog-height: 350px;

.aiTrain {
    .ai-btn {
        background: $gradient-primary;
        padding-inline: 20px;
        border: none;

        &:hover,
        &:focus {
            background: $gradient-primary;
            color: #fff;
        }
    }

    .notTrain {
        color: #484A4D;
        border-color: #484A4D;
        width: 100px;

        &:hover,
        &:focus {
            background: #ffffff;
            color: #484A4D;
            border-color: #484A4D;
        }
    }

    .aiTrainBtn {
        background: $gradient-primary;
        margin-left: 30px !important;
        width: 100px;
        border: none;

        &:hover,
        &:focus {
            background: $gradient-primary;
            color: #fff;
        }
    }
}

.ai-training-dialog {
    text-align: center;
    background: url('~@/assets/imgs/aiTrain.png') center/cover no-repeat !important;

    .item {
        color: #151719;
    }

    .item:first-child {
        padding-bottom: 30px;
        background: $gradient-primary;
    }
}

.ai-trained-dialog {
    text-align: left;
    background: url('~@/assets/imgs/aiComplete.png') center/cover no-repeat !important;

    .el-dialog__body {
        padding: 30px 75px 18px 75px;
    }

    .item {
        color: #151719;
    }

    .item:first-child {
        padding-bottom: 15px;
        background: $gradient-primaryed;
    }

    .item:last-child {
        color: var(--color-main);
    }
}

.ai-train-dialog {
    width: 450px !important;
    height: 350px !important;
    border-radius: 15px !important;
    padding-top: 36px;

    .item {
        padding: 8px 0;
        font-weight: 500;
        font-size: 14px;
        line-height: 1.5;
    }

    .item:first-child {
        font-size: 16px;
        -webkit-background-clip: text;
        background-clip: text;
        color: transparent;
        -webkit-text-fill-color: transparent;
    }

    .dialog-footer {
        display: flex;
        align-items: center;
        justify-content: center;
    }
}
</style>

<template>
    <el-dialog
        title="提取文案"
        :visible.sync="visible"
        width="520px"
        :destroy-on-close="true"
        :close-on-click-modal="false"
        :before-close="handleClose"
        class="extraction-dialog"
    >
        <div class="dialog-content" v-if="visible">
            <div class="source-type flex items-center">
                <div class="label">视频来源：</div>
                <el-radio-group v-model="sourceType">
                    <el-radio :label="1">视频链接</el-radio>
                    <el-radio :label="2">本地上传</el-radio>
                </el-radio-group>
            </div>

            <div class="video-url flex items-center">
                <div class="label">{{ sourceType === 1 ? '视频地址' : '上传视频' }}：</div>
                <el-input
                    v-model="localVideoInfo.filePath"
                    size="default"
                    v-if="sourceType === 1"
                    :placeholder="sourceType === 1 ? `请将视频链接粘贴到文本框，点击'立即提取文案'按钮` : ''"
                    :disabled="sourceType !== 1"
                    class="url-input input-gray input-border-none"
                />
                <div class="flex items-end" v-if="sourceType === 2">
                    <div v-if="localVideoInfo.filePath">
                        <div><span class="font-bold">文件名</span>：{{ localVideoInfo.fileName }}</div>
                        <div><span class="font-bold">文件大小</span>：{{
                                myUtils.retainDecimals(localVideoInfo.fileSize / 1024 / 1024) + 'M'
                            }}
                        </div>
                    </div>
                    <div class="flex items-end" v-else>
                        <afp-button size="medium" @click="selectUploadFile">点击上传</afp-button>
                        <div slot="tip" class="el-upload__tip">大小限制：＜500M</div>
                    </div>
                </div>
                <div class="upload-tip flex items-center" v-if="sourceType === 1">
                    <span class="asterisk">*</span>
                    <span class="tip-text">目前支持抖音视频文案提取</span>
                </div>
            </div>

            <div class="extract-btn-container flex justify-center">
                <afp-button type="primary" @click="handleExtract" :loading="loading" size="default">立即提取文案</afp-button>
            </div>
        </div>
    </el-dialog>
</template>

<script>

import myUtils from "@/utils/utils";

export default {
    props: {
        visible: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            sourceType: 1, // 1:视频链接，2:本地上传
            loading: false,
            localVideoInfo: {}
        }
    },
    watch: {
        visible(val) {
            if (!val) {
                this.resetForm()
            }
        },
        sourceType() {
            this.localVideoInfo = {}
        }
    },
    computed: {
        myUtils() {
            return myUtils
        }
    },
    methods: {
        handleClose() {
            this.$emit('update:visible', false)
            this.resetForm()
        },
        async selectUploadFile() {
            try {
                const clientResult = await this.$httpClient.shortVideo.selectLocalVideo()
                if (clientResult.code !== 0) return this.$message.error('选择文件失败')
                this.localVideoInfo = clientResult.data
            } catch (e) {
            }
        },
        async handleExtract() {
            if (!this.localVideoInfo.filePath.trim()) return this.$message.warning('请输入短视频链接地址')
            this.loading = true
            try {
                const clientResult = await this.$httpClient.shortVideo.immediatelyLocalVideo({
                    sourceType: this.sourceType,
                    videoUrl: this.localVideoInfo.filePath,
                })
                if (clientResult.code !== 0) return
                this.loading = false
                this.$message.success('文案提取中，每条耗时1分钟左右')
                this.$emit('extract-success')
                this.handleClose()
            } catch (e) {
                this.loading = false
            }
        },
        resetForm() {
            this.sourceType = 1
            this.localVideoInfo = {}
            this.loading = false
        }
    }
}
</script>

<style lang="scss" scoped>
.extraction-dialog {
    ::v-deep(.el-dialog__header) {
        padding: 15px 20px;
    }

    ::v-deep(.el-dialog__body) {
        padding: 12px 20px 25px 12px;
    }

    ::v-deep(.el-dialog__title) {
        font-size: 16px;
        font-weight: 500;
    }

    .dialog-content {
        .source-type {
            margin-bottom: 20px;
        }

        .video-url {
            margin-bottom: 20px;
            position: relative;

            .url-input {
                width: 100%;

                ::v-deep(.el-input__inner) {
                    height: 36px;
                    font-size: 14px;
                }
            }

            .el-upload__tip {
                padding-left: 20px;
            }

            .upload-tip {
                margin-top: 5px;
                position: absolute;
                right: 0;
                bottom: -20px;

                .asterisk {
                    color: var(--color-main);
                    margin-right: 4px;
                }

                .tip-text {
                    color: var(--color-main);
                    font-size: 12px;
                }
            }
        }

        .extract-btn-container {
            margin-top: 30px;
        }
    }

    .label {
        color: #606266;
        font-size: 14px;
        min-width: 70px;
    }
}
</style>

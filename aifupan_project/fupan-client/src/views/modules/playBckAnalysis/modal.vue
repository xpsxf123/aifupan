<template>
    <div class="play_modal">
        <el-dialog
            :visible.sync="dialogVisible"
            :close-on-press-escape="false"
            :close-on-click-modal="false"
            width="500px">
            <template #title>
                <div>
                    <span v-if="!isCopy" class="text-lg">{{ fileType === 1 ? '上传文本' : '上传视频' }}</span>
                    <span v-else-if="isCopy" class="text-lg">粘贴文本</span>
                    <span v-if="fileType===0 && !isCopy" class="text-xs" style="padding-left: 6px;color: red">上传文件如果过大，卡顿属于正常现象，请耐心等待</span>
                </div>
            </template>
            <UploadContent v-if="dialogVisible" @getUploadData="getUploadData"
                           :fileType="fileType" :isCopy="isCopy"/>
            <span slot="footer" class="dialog-footer flex items-center justify-center">
                <template v-if="isCopy">
                    <afp-button type="" style="padding-inline: 24px" @click="onCancel" size="medium">取消</afp-button>
                    <afp-button type="primary" @click="uploadText" size="medium" :plain="false"
                                :disabled="!uploadData?.content" style="margin-left: 32px">确定</afp-button>
                </template>
                <template v-else>
                    <afp-button style="padding-inline: 18px" size="medium" type="primary" :plain="false"
                                :disabled="!uploadData?.fileData?.filePath"
                                @click="commitUploadFile">点击上传</afp-button>
                </template>
            </span>
        </el-dialog>
    </div>
</template>

<script>

import UploadContent from './uploadContent.vue'

export default {
    components: {
        UploadContent
    },
    props: {},
    data() {
        return {
            uploadData: {},//上传数据
            dialogVisible: false,
            fileType: 1,
            isCopy: false, //是否是文本复制
        }
    },
    watch: {},
    computed: {},
    methods: {
        onCancel() {
            this.uploadData = {};
            this.dialogVisible = false;
            this.fileData = {}
        },
        changeDialogVisible(status, fileType, option = {}) {
            const {isCopy} = option;
            this.dialogVisible = status
            this.fileType = fileType
            this.isCopy = isCopy
        },
        commitUploadFile() {
            this.$emit('uploadFile', {
                platformType: this.uploadData?.platformType,
                fileType: this.fileType,
                filePath: this.uploadData.fileData?.filePath ? this.uploadData.fileData?.filePath : '',
            },this.onCancel)
        },
        uploadText() {
            this.$httpClient.uploadFile.uploadTxtFileByWord({
                platformType: this.platformType,
                content: this.uploadData.content,
            }).then(res => {
                if (res.code === 0) {
                    this.$emit('getTableList'); //更新列表
                    this.$message({
                        message: '上传成功',
                        type: 'success',
                    });
                    this.onCancel();
                } else {
                    this.$message.error(res.msg);
                }
            })
        },
        getUploadData(data) {
            return this.uploadData = data
        }
    }
}
</script>
<style lang='scss' scoped>
.play_modal {
    ::v-deep(.el-dialog__body) {
        padding: 12px 20px;
    }
}
</style>
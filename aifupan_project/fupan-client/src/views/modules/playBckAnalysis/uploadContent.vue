<template>
    <div class="upload-content">
        <div class="flex items-start text-sm">
            <div class="title">选择平台</div>
            <el-radio-group v-model="platformType">
                <el-radio :label="item.value" v-for="item in uploadFileDictList"
                          :key="item.id" style="margin-bottom: 8px">
                    {{ item.label }}
                </el-radio>
            </el-radio-group>
        </div>
        <div class="flex" v-if="fileType===1">
            <div class="title">支持文件类型：TXT、Word</div>
        </div>
        <copyText v-if="isCopy" v-model="content"></copyText>
        <div v-else-if="fileData?.filePath" class="upload-data">
            <div class="flex">
                <div class="title text-sm">文件名称：</div>
                <div class="text-sm">{{ fileData.fileName }}</div>
            </div>
            <div class="flex" style="margin-top: 12px">
                <div class="title text-sm">文件大小：</div>
                <div class="text-sm">{{ getFileSize(fileData?.fileSize) }}</div>
            </div>
            <div class="text-xs cursor-pointer" style="margin-top: 12px;color: var(--color-main)" @click="checkUploadFile">重新上传</div>
        </div>
        <div class="flex" v-else style="position: relative">
            <div class="upload_content" @click="checkUploadFile" style="flex: 1">
                <img src="@/assets/imgs/upload_icon.png" alt="" style="width: 50px"/>
                <div>上传文件</div>
            </div>
            <div class="text-xs pd-l12 tips-error" v-if="fileType!==1&&!isCopy">
                单个文件大小限制在3G以内
            </div>
        </div>
    </div>
</template>

<script>
import copyText from './copyText.vue'

export default {
    components: {
        copyText
    },
    props: {
        fileType: {
            type: [Number, String],
            default: 0
        },
        isCopy: {//是否是文本复制
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            platformType: 1,//平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书
            uploadFileDictList: [],
            fileData: {},
            content: '',
        }
    },
    watch: {
        componentData: {
            handler(newVal) {
                this.$emit('getUploadData', newVal)
            },
            deep: true,
            immediate: true // 立即执行一次
        }
    },
    computed: {
        componentData() {
            return {
                platformType: this.platformType,
                fileData: this.fileData,
                content: this.content,
            }
        },
        getFileSize() {
            return (size) => {
                if (size > 0 && size <= 1024) {
                    return size + ' B'
                } else if (size >= 1024 && size < 1024 ** 2) {
                    return (size / 1024).toFixed(2) + ' KB'
                } else if (size >= 1024 ** 2 && size < 1024 ** 3) {
                    return (size / (1024 ** 2)).toFixed(2) + ' M'
                } else {
                    return (size / (1024 ** 3)).toFixed(2) + ' G'
                }
            }
        }
    },
    methods: {
        onCancel() {
            this.content = '';
            this.fileData = {}
        },
        checkUploadFile() {
            this.$httpClient.uploadFile.checkUploadFile({
                fileType: this.fileType,
            }).then(res => {
                if (res.code === 0) this.fileData = res.data
            })
        },
        //获取平台类型
        getUploadFileDictList() {
            this.$httpBack.dictdata.uploadFileDictList().then(res => {
                if (res.code === 0) {
                    this.uploadFileDictList = res.data
                    this.platformType = res.data[0]?.value
                }
            })
        }
    },
    mounted() {
        this.getUploadFileDictList()
    },
}
</script>
<style lang='scss' scoped>
.upload-content {
    .title {
        width: 70px;
        padding-right: 12px;
        white-space: nowrap;
        color: #606266;
    }

    .tips-error {
        color: red;
        position: absolute;
        bottom: 18px;
        right: -170px;
    }

    .upload-data {
        margin-block: 8px;
        padding: 12px;
        background: #F7F7F7;
        border-radius: 4px;
    }

    .upload_content {
        margin: 8px 0 18px 0;
        border: 1px var(--color-main) dashed;
        border-radius: 20px;
        text-align: center;
        padding: 8px;
        background-color: rgba(68, 77, 255, 0.05);
        cursor: pointer;
        color: var(--color-main);
        font-size: 14px
    }

    .dialog-footer {
        margin-top: 24px;
    }
}
</style>
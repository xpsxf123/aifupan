<template>
    <div style="width: 100%;" class="upload-container">
        <el-upload :class="{'upload-item':true,'upload-item-contrast':isContrast,'upload-item-disabled':isContrast||!item.isAuthenticated}" drag ref="upload" action=""
                   :limit="1" :show-file-list="false" :auto-upload="false"
                   accept=".png,.jpg,.jpeg,.gif,.bmp,.webp"
                   :disabled="isContrast||!item.isAuthenticated"
                   :on-change="onChange">
            <div :style="{fontSize: '14px',color:(isContrast||!item.isAuthenticated)?'#96A1A9':'#000000'}" v-if="placeholder">{{ item.title }}</div>
            <span v-if="placeholder" class="icon font_family icon-shangchuantupian"></span>
            <div :style="{color:(isContrast||!item.isAuthenticated)?'#96A1A9':'var(--color-main)',marginTop: '18px'}">
                <i class="el-icon-plus"></i>
                <span>上传图片</span>
            </div>
            <div class="el-upload__text">{{placeholder}}</div>
        </el-upload>
    </div>
</template>

<script>
export default {
    components: {},
    props: {
        item: {
            type: Object,
            default: {}
        },
        isContrast: {
            type: Boolean,
            default: false
        },
        placeholder: {
            type: String,
            default: ''
        }
    },
    data () {
        return {}
    },
    computed: {},
    watch: {},
    methods: {
        handleClick () {
            this.$refs.upload?.$refs['upload-inner']?.handleClick()
        },
        async customUpload (options) {
            const file = options.file
            const resOss = await this.$httpBack.v2300.getDataScreenshotPutUrl({
                suffix: file.name.split('.').pop()
            })
            console.log('获取上传地址', resOss)
            const { data: { signedUrl, ossKey } } = resOss

            const chunkSize = 10 * 1024 * 1024
            let start = 0
            let end = chunkSize
            while (start < file.size) {
                const chunk = file.slice(start, end)
                const response = await fetch(signedUrl, {
                    method: 'PUT',
                    body: chunk
                })
                if (!response.ok) {
                    options.onError()
                    return
                }
                options.onSuccess(response, ossKey)
                start = end
                end = start + chunkSize
            }
        },
        onChange (file) {
            if (!this.item.isAuthenticated) return
            this.handleImageFile(file)
        },
        getImageSize (file) {
            return new Promise((resolve, reject) => {
                const img = new Image()
                img.onload = () => {
                    resolve({
                        width: img.width,
                        height: img.height
                    })
                }
                img.onerror = () => {
                    this.clearFiles()
                    reject(new Error('图片加载失败'))
                }
                img.src = URL.createObjectURL(file)
            })
        },
        async setErrorFileList (file, loading) {
            this.$emit('changeItem', {
                ...this.item,
                status: 'error',
                sourceImagesAddress: URL.createObjectURL(file),
                screenshotStatus: 0,
            })
            loading?.close()
            this.clearFiles()
            this.$message.error('上传失败')
        },
        screenshotUpload (ossKey, file) {
            return this.$httpBack.v2300.screenshotUpload({
                screenshotCode: this.item.screenshotCode,
                sourceImagesAddress: ossKey,
                sourceType: this.item.sourceType,
                sourceId: this.item.sourceId
            }).then(res => {
                if (res.code === 0) {
                    this.$emit('changeItem', {
                        ...this.item,
                        ...res.data,
                        status: 'success',
                    })
                    this.$message.success('上传成功')
                    this.$emit('singleIdentify', res.data?.id)
                }
            }).catch(err => {
                this.setErrorFileList(file)
            })
        },
        clearFiles () {
            this.$refs.upload?.clearFiles()
        },
        imageSecurity (ossKey, file) {
            return this.$httpBack.v2300.imageSecurity({
                key: ossKey
            }).then(async res => {
                if (res.code === 0) {
                    await this.screenshotUpload(ossKey, file)
                }
            }).catch(err => {
                this.setErrorFileList(file)
            })
        },
        async handleImageFile ({ raw: file }) {
            if (!file) return
            const allowedTypes = ['image/png', 'image/jpg', 'image/jpeg', 'image/gif', 'image/bmp', 'image/webp']
            if (!allowedTypes.includes(file.type)) {
                this.clearFiles()
                return this.$message.error('请上传png、jpg、jpeg、gif、bmp、webp格式的图片')
            }
            if (file?.size / 1024 / 1024 > 10) {
                this.clearFiles()
                return this.$message.error('图片大小不能超过10MB')
            }

            const imageSize = await this.getImageSize(file)
            if (imageSize.width < 10 || imageSize.height < 10) {
                this.clearFiles()
                return this.$message.error('图片宽高不能小于10*10')
            }

            const loading = this.$loading({
                lock: true,
                text: '图片上传中，请稍后...',
                spinner: 'el-icon-loading',
                background: 'rgba(0, 0, 0, 0.7)'
            })

            try {
                await this.customUpload({
                    file,
                    onSuccess: async (res, ossKey) => {
                        await this.imageSecurity(ossKey, file)
                        loading.close()
                    },
                    onError: async () => {
                        await this.setErrorFileList(file, loading)
                    }
                })
            } catch (error) {
                await this.setErrorFileList(file, loading)
            }
        },
    },
    created () {

    },
    mounted () {

    },
    beforeCreate () { }, //生命周期 - 创建之前
    beforeMount () { }, //生命周期 - 挂载之前
    beforeUpdate () { }, //生命周期 - 更新之前
    updated () { }, //生命周期 - 更新之后
    beforeDestroy () { }, //生命周期 - 销毁之前
    destroyed () { }, //生命周期 - 销毁完成
    activated () { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.upload-container {
    .upload-item {
        text-align: center;
        //width: 200px;
        width: 100%;
        aspect-ratio: 11 / 10;
        //height: 180px;
        height: 100%;
        font-size: 12px;

        .el-upload__text {
            font-size: 12px;
            font-weight: 400;
            color: #909499;
            margin-top: 12px;
        }

        ::v-deep(.el-upload) {
            width: 100%;
        }

        ::v-deep(.el-upload-dragger) {
            width: 100%;
            max-width: 220px;
            aspect-ratio: 11 / 10;
            height: auto;
            padding: 12px;
            background: #E8F5FF;
            border-color: #b4cfee;
        }

        ::v-deep(.el-upload-dragger:hover) {
            border-color: var(--color-main);
        }

        .icon {
            margin-top: 14px;
            display: inline-block;
            font-size: 40px;
            font-weight: 400;
            color: #80B7F9;
        }

        .el-upload__text {
            color: #96A1A9;
        }
    }
    .upload-item-contrast{
        width: 220px;
    }
    .upload-item-disabled{
        ::v-deep(.el-upload-dragger:hover) {
            border-color: #B4CFEE;

            cursor: not-allowed;
        }
    }
}
</style>

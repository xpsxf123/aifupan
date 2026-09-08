<template>
    <div class="dataScreenshot-box">
        <div style="width: 100%;" :class="{'upload-box':true,'upload-box-contrast':isContrast}" ref="uploadBox" v-if="!dataLoading&&dataList.length">
            <input ref="hiddenInput" type="text" class="hidden-input" @paste="handlePaste"/>
            <el-button type="text" class="move-right-icon" @click="scrollLeft">
                <span class="icon font_family icon-a-Polygon16" style="font-size: 28px;"></span>
            </el-button>
            <div v-for="(item, index) in dataList" :key="item.screenshotCode" class="upload-space">
                <div v-show="!item.status"
                     @mouseenter="() => activatePasteListener(index)"
                     @mouseleave="() => deactivatePasteListener(index)">
                    <UploadImage :ref="'upload-box-' + index" :item="item" @changeItem="changeItem"
                                 :isContrast="isContrast" @singleIdentify="getTimesRemind" placeholder="复制或截取图片后，回到爱复盘，ctrl+v粘贴"/>
                </div>
                <div v-show="item.status" class="avatar-box" :style="{
                borderColor: statusColor(item),
                background: statusBackgroundColor(item),
            }">
                    <div style="font-size: 14px;">{{ item.title }}</div>
                    <div style="position: relative;height: calc(100% - 45px)">
                        <el-image style="height: 100%;width:100%;"
                                  :ref="'image-ref-' + index"
                                  :src="item.sourceImagesAddress || item.file"
                                  :preview-src-list="[item.sourceImagesAddress || item.file]" fit="contain"></el-image>
                        <i class="el-icon-zoom-in icon" @click="()=>openPreview(index) "></i>
                    </div>

                    <div style="display: flex;align-items: center;justify-content: space-between">
                        <span v-if="!isContrast">
                            <i class="el-icon-delete identify-success"
                               v-if="(identifySuccess(item)||identifyError(item))&&isAuthenticated"
                               @click="()=>delUploadImage(index)"></i>
                        </span>
                        <span/>
                        <div class="upload-status" :class="getStatusClass(item)">
                            {{ getStatusText(item) }}
                        </div>
                    </div>

                </div>
                <div class="upload-btn" v-if="!isContrast">
                    <afp-button plain @click="() => reUpload(index)"
                               :disabled="item.status===''||identifyIng(item)||!isAuthenticated">
                        重新上传
                    </afp-button>
                    <afp-button type="primary" :plain="false" v-if="item.status===''"
                               @click="() => viewExamples(item)">
                        查看示例
                    </afp-button>
                    <afp-button type="danger"
                               :key="new Date().getTime()"
                               v-if="uploadSuccess(item)||uploadError(item)||identifyIng(item)"
                               :disabled="identifyIng(item)||!isAuthenticated"
                               @click="() => delUploadImage(index)">删除图片
                    </afp-button>
                    <afp-button v-if="item.sourceImagesAddress && identifyError(item)"
                               :key="new Date().getTime()"
                               :disabled="!isAuthenticated" @click="()=>getTimesRemind(item.id,4)">
                        重新识别
                    </afp-button>
                    <afp-button type="primary" :plain="false" v-if="item.sourceImagesAddress && identifySuccess(item)"
                               :key="new Date().getTime()"
                               :disabled="!identifySuccess(item)"
                               @click="() => handleChange(item)">查看数据
                    </afp-button>
                </div>
            </div>
            <el-button type="text" class="move-left-icon" @click="scrollRight">
                <span class="icon font_family icon-a-Polygon16" style="font-size: 28px;"></span>
            </el-button>
        </div>
        <div class="nimble-box" v-if="dataList.length">
            <div style="display: flex;align-items: center">
                <!--                <div :class="disabledTenantIdBtn?['nimble-btn-dis','nimble-btn']:['nimble-btn-use','nimble-btn']"-->
                <!--                     @click="()=>aIAnalysisOfData(isContrast?'contrast':'single')">-->
                <!--                    <span :style="{color:disabledTenantIdBtn?'#ABAEB3':'#0077FF'}">{{-->
                <!--                            isContrast ? 'AI对比数据' : 'AI分析数据'-->
                <!--                        }}</span>-->
                <!--                </div>-->
                <div v-if="isContrast"
                     :class="disabledTenantIdBtn?['nimble-btn-dis','nimble-btn']:['nimble-btn-use','nimble-btn']"
                     @click="()=>aIAnalysisOfData('contrast')">
                    <span :style="{color:disabledTenantIdBtn?'#ABAEB3':'#0077FF'}">AI对比数据</span>
                </div>
                <div v-if="!isContrast&&!isFile&&!isWebOnline && !isExample && getReplayType === 'replayAll'"
                     @click="selectVideo"
                     :class="disabledTenantIdBtn?['nimble-btn-dis','nimble-btn-dis-long','nimble-btn']:['radio-nimble-btn-use','nimble-btn']">
                    <span :style="{color:disabledTenantIdBtn?'#ABAEB3':'#05833A'}">对比上一场数据</span>
                </div>
            </div>
            <div/>
            <div class="nimble-text" v-if="!isContrast">*每场上传截图后AI会结合数据进行分析</div>
            <!--            <afp-button type="primary"  class="nimble-btn" @click="()=>getTimesRemind()"-->
            <!--                       :loading="identifyLoading"-->
            <!--                       :disabled="disabledBtn||!isAuthenticated">-->
            <!--                一键识别数据-->
            <!--            </afp-button>-->
        </div>
        <div v-if="!dataLoading&&!dataList.length" style="max-height: 270px;" class="flex-jc-c h100 flex-ai-c">
            <img src="@/assets/imgs/chartEmpty.png" style="max-height: 160px" alt="" srcset="">
        </div>
        <data-edit :currentItem="currentItem" @handleChange="handleChange"/>
        <times-remind ref='timesRemind' @handleChangeDialog="handleChangeDialog" :imageInfo="imageInfo"/>
        <!-- 客服二维码 -->
        <customer-service-qr-code v-if="customerServiceQrCodeVisible"
                                  ref="customerServiceQrCode"></customer-service-qr-code>
        <ContrastLastTime ref="lastTime" :sentenceMarkData="sentenceMarkData" aiType="dataCapture"
                          :targetType="targetType"/>
    </div>
</template>

<script>
import { cloneDeep } from 'lodash'
import UploadImage from './upload.vue'
import DataEdit from './data-edit.vue'
import TimesRemind from './times-remind.vue'
import store from '@/store'
import ContrastLastTime from './../contrastLastTime.vue'
import CustomerServiceQrCode from '@/views/commonComponent/customerServiceQrCode.vue'
import exampleMixin from '@/mixins/exampleMixin'
import myUtils from "@/utils/utils";
export default {
    components: {
        CustomerServiceQrCode,
        UploadImage,
        DataEdit,
        TimesRemind,
        ContrastLastTime
    },
    mixins: [exampleMixin],
    props: {
        sentenceMarkData: {
            type: Object,
            default: {}
        },
        isWebOnline: {
            type: Boolean,
            default: false
        },
        isContrast: {
            type: Boolean,
            default: false
        },
        isFile: {
            type: Boolean,
            default: false
        },
        targetType: {
            type: String,
            default: ''
        }
    },
    data () {
        return {
            dataList: [],
            currentItem: {},
            currentIndex: -1,
            isPasteActive: false,
            dataLoading: false,
            identifyLoading: false,
            customerServiceQrCodeVisible: false,
            imageInfo: {},
            pollingTimeout: null
        }
    },
    computed: {
        uploadSuccess () {
            return (item) => [1].includes(item.screenshotStatus)
        },
        uploadError () {
            return (item) => item.status === 'error'
        },
        identifyIng () {
            return (item) => item.screenshotStatus === 2
        },
        identifySuccess () {
            return (item) => [3].includes(item.screenshotStatus)
        },
        identifyError () {
            return (item) => item.screenshotStatus === 4
        },
        statusColor () {
            return (item) => {
                if (this.uploadSuccess(item) || this.identifySuccess(item)) {
                    return '#33C075'
                }
                if (this.uploadError(item) || this.identifyError(item)) {
                    return '#F56C6C'
                }
                if (this.identifyIng(item)) {
                    return '#1677FF'
                }
            }
        },
        statusBackgroundColor () {
            return (item) => {
                if (this.uploadSuccess(item) || this.identifySuccess(item)) {
                    return '#D5F0F0'
                }
                if (this.uploadError(item) || this.identifyError(item)) {
                    return '#E9E6EE'
                }
                if (this.identifyIng(item)) {
                    return '#C4D8F2'
                }
            }
        },
        disabledBtn () {
            return this.dataList.filter(item => [3].includes(item.screenshotStatus)).length === 0 || !this.isAuthenticated
        },
        disabledTenantIdBtn () {
            return this.dataList.filter(item => [3].includes(item.screenshotStatus)).length === 0 || !this.isTenantIdAuthenticated
        },
        allItemsCompleted () {
            const list = this.dataList.filter(item => item.id)
            return list.every(item => [3, 4].includes(item.screenshotStatus))
        },
        isTenantIdAuthenticated () {
            const { videoInfo = {} } = this.sentenceMarkData
            return videoInfo?.TenantId ? this.$auth([videoInfo?.TenantId], 'every') : true
        },
        isAuthenticated () {
            const { videoInfo = {} } = this.sentenceMarkData
            return videoInfo?.UserId ? this.$auth([videoInfo?.UserId, videoInfo?.TenantId], 'every') : true
        },
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
    },
    watch: {
        sentenceMarkData: {
            handler (newVal) {
                if (newVal) {
                    const { VideoId = '' } = newVal?.videoInfo || {}
                    const { fileId = '' } = newVal?.uploadFile || {}
                    if (VideoId || fileId) {
                        this.getDataScreenshotList()
                    }
                }
            },
            immediate: true
        }
    },
    methods: {
        selectVideo () {
            if (this.disabledTenantIdBtn) return
            this.$refs.lastTime.openDialog()
        },
        scrollLeft(){
            const container = this.$refs.uploadBox
            if (!container) return
            container.scrollLeft -= ((container.offsetWidth / this.dataList.length) || 80)
        },
        scrollRight () {
            const container = this.$refs.uploadBox
            if (!container) return
            container.scrollLeft += ((container.offsetWidth / this.dataList.length) || 80)
        },
        aIAnalysisOfData (type) {
            if (this.disabledTenantIdBtn) return
            if (type === 'contrast') {
                this.$emit('aIContrastData', 'dataCapture')
            } else {
                this.$emit('aiAnalysisData', { type: 'dataCapture' })
            }
        },
        stopPolling () {
            if (this.pollingTimeout) {
                clearTimeout(this.pollingTimeout)
                this.pollingTimeout = null
            }
        },
        scheduleNextPoll () {
            this.stopPolling()
            if (!this.allItemsCompleted) {
                this.pollingTimeout = setTimeout(() => {
                    this.getDataScreenshotList('loading')
                }, 10000)
            }
        },
        activatePasteListener (index) {
            if (!this.isPasteActive) {
                this.currentIndex = index
                this.isPasteActive = true
                this.$refs.hiddenInput.focus()
            }
        },

        deactivatePasteListener () {
            if (this.isPasteActive) {
                this.currentIndex = -1
                this.isPasteActive = false
                this.$refs.hiddenInput.blur()
            }
        },
        handlePaste (event) {
            if (!this.isPasteActive) return
            if (!this.isAuthenticated) return
            if (this.isContrast) return
            event.preventDefault()
            // 获取剪贴板数据
            const clipboardData = event.clipboardData || window.clipboardData
            if (!clipboardData || !clipboardData.items) {
                this.$message.error('无法获取剪贴板内容')
                return
            }

            const items = clipboardData.items
            for (let i = 0; i < items.length; i++) {
                if (items[i].type.indexOf('image') !== -1) {
                    const file = items[i].getAsFile()
                    this.$refs[`upload-box-${this.currentIndex}`][0].handleImageFile({ raw: file }) // 手动触发上传组件的文件添加
                    this.deactivatePasteListener()
                    break // 只处理第一张图片
                }
            }
        },
        showCustomerServiceQrCode () {
            this.customerServiceQrCodeVisible = true
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },
        parseSSE (text) {
            const lines = text.trim().split('\n')
            const result = []
            let currentEvent = {}
            let hasError = false
            try {
                for (const line of lines) {
                    if (!line) continue
                    if (line.startsWith('event: ')) {
                        currentEvent.event = line.replace('event: ', '').trim()
                    } else if (line.startsWith('data: ')) {
                        const dataStr = line.replace('data: ', '').trim()
                        try {
                            currentEvent.data = dataStr === 'stop' ? 'stop' : JSON.parse(dataStr)
                        } catch (jsonError) {
                            if (!hasError) {
                                this.$message.error(dataStr)
                                hasError = true
                            }
                            currentEvent.data = null
                        }
                        result.push({ ...currentEvent })
                        currentEvent = {}
                    }
                }
                return result.filter(item => item.data !== null)
            } catch (error) {
                this.$message.error(error.message)
            }
        },
        async getTimesRemind (id, status) {
            if (!this.isAuthenticated) return
            try {
                const ids = this.dataList.filter(item => item.id && [status ?? 1].includes(item.screenshotStatus)).map(item => item.id)
                if (!ids.length) return
                const response = this.$httpBack.userProperty.getUserProperty()
                //剩余--已使用--总数
                const { imgIdentifyNum, useImgIdentifyNum, totalImgIdentifyNum } = response || {}
                if (ids.length > imgIdentifyNum) {
                    this.imageInfo = { imgIdentifyNum, useImgIdentifyNum, totalImgIdentifyNum }
                    this.$nextTick(() => {
                        this.$refs.timesRemind?.initStatus()
                    })
                } else {
                    await this.identifyImage(id)
                }
            } catch (err) {
                console.log('识别数据错误:', err)
            }
        },
        async identifyImage (id) {
            const { videoInfo = {}, uploadFile = {} } = this.sentenceMarkData
            const ids = id ? [id] : this.dataList.filter(item => item.id && [1].includes(item.screenshotStatus)).map(item => item.id)
            if (!ids.length) return
            this.dataList.map(item => {
                if ((id && item.id === id) || (item.id && [1].includes(item.screenshotStatus))) {
                    item.screenshotStatus = 2
                }
            })
            const response = await fetch(`${window.SITE_CONFIG['backApiURL']}/openapi/v2300/screenshotAnalysis`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Token: store.state.token
                },
                body: JSON.stringify({
                    sourceType: videoInfo?.VideoId ? 0 : 1,
                    sourceId: videoInfo?.VideoId || uploadFile?.fileId,
                    ids
                })
            })
            const reader = response.body.getReader()
            const decoder = new TextDecoder('utf-8')
            let fullData = '';

            while (true) {
                const { done, value } = await reader.read()
                if (done) {
                    break
                }
                const chunk = decoder.decode(value, { stream: true });
                fullData += chunk;
            }
            const result = this.parseSSE(fullData)
            if (result.length) {
                const list = cloneDeep(this.dataList)
                const resultMap = new Map(result.map(item => [item.data.id, {
                    aiContent: item.data.aiContent,
                    status: item.data.status
                }]))
                list.forEach(item => {
                    if (resultMap.has(item.id)) {
                        const resultItem = resultMap.get(item.id)
                        item.aiContent = resultItem.aiContent
                        item.screenshotStatus = resultItem.status
                    }
                })
                this.dataList = list
            }
        },
        handleChange (item) {
            if (!item) {
                this.currentItem = {}
                return
            }
            this.$httpBack.v2300.screenshotInfo({
                id: item.id
            }).then(res => {
                this.currentItem = { ...res.data, isAuthenticated: item.isAuthenticated }
            })
        },
        handleChangeDialog () {
            this.showCustomerServiceQrCode()
        },
        viewExamples (item) {
            this.$alert(`
                <div style="text-align:center">
                    <div style="color: red;font-size: 12px;position: absolute;top: -52px;left: 85px;">以下图片仅作为参考图片，您可以截取任何数据图片，AI都能帮您识别！</div>
                    <div style="margin-top:12px">
                        <img src=${item.exampleImgUrl} alt="" style="width: 100%;height: 260px;object-fit: contain">
                    </div>
                </div>
            `, '图片示例', {
                showClose: false,
                customClass: 'custom-examples-confirm',
                dangerouslyUseHTMLString: true
            })
        },
        openPreview (index) {
            this.$refs[`image-ref-${index}`][0]?.clickHandler()
        },
        delUploadImage (index) {
            this.dataList.map(async (item, _index) => {
                if (_index === index) {
                    this.$refs[`upload-box-${index}`][0]?.clearFiles()
                    let response = { code: 0 }
                    if ([1, 2, 3, 4].includes(item.screenshotStatus)) {
                        response = await this.$httpBack.v2300.dataScreenshotDelete({ id: item.id })
                    }
                    if (response.code === 0) {
                        item.status = ''
                        item.id = null
                        item.file = null
                        item.tenantId = null
                        item.sourceImagesAddress = null
                        item.aiContent = null
                        item.screenshotStatus = 0
                        item.updateDate = null
                        item.createDate = null
                        this.$message.success('删除成功')
                    } else {
                        this.$message.error(response.msg)
                    }
                    // this.getDataScreenshotList('loading')
                }
            })
        },
        reUpload (index) {
            this.$refs[`upload-box-${index}`][0]?.clearFiles()
            this.$refs[`upload-box-${index}`][0].handleClick()
        },
        // 获取状态文本
        getStatusText (item) {
            if (this.identifySuccess(item)) return '识别成功'
            if (this.identifyIng(item)) return '识别中'
            if (this.identifyError(item)) return '识别失败，删除图片'
            if (this.uploadSuccess(item)) return '上传成功'
            if (this.uploadError(item)) return '上传失败'
            return ''
        },
        // 获取状态类名
        getStatusClass (item) {
            return {
                'success-text': this.identifySuccess(item) || this.uploadSuccess(item),
                'error-text': this.identifyError(item) || this.uploadError(item),
                'ing-text': this.identifyIng(item)
            }
        },
        changeItem (item) {
            const list = cloneDeep(this.dataList)
            this.dataList = list.map(_item => (_item.screenshotCode === item.screenshotCode ? item : _item))
        },
        getDataScreenshotList (status) {
            if (!status) this.dataLoading = true
            const { videoInfo = {}, uploadFile = {} } = this.sentenceMarkData
            this.$httpBack.v2300.dataScreenshotList({
                sourceType: videoInfo?.VideoId ? 0 : 1,
                sourceId: videoInfo?.VideoId || uploadFile?.fileId,
            }).then(res => {
                if (!status) this.dataLoading = false
                if (res.code === 0) {
                    const identifyLength = res.data.filter(item => item.screenshotStatus === 2).length
                    this.identifyLoading = identifyLength > 0
                    res.data.map(item => {
                        item.sourceType = videoInfo?.VideoId ? 0 : 1
                        item.sourceId = videoInfo?.VideoId || uploadFile?.fileId
                        item.isAuthenticated = this.isAuthenticated
                        item.status = item.screenshotStatus === 0 ? '' : 'success'
                    })
                    this.dataList = res.data
                    if (identifyLength > 0) this.scheduleNextPoll()
                }
            }).finally(() => {

            })
        }
    },
    created () {

    },
    mounted () {

    },
    beforeCreate () { }, //生命周期 - 创建之前
    beforeMount () { }, //生命周期 - 挂载之前
    beforeUpdate () { }, //生命周期 - 更新之前
    updated () { }, //生命周期 - 更新之后
    beforeDestroy () {
        this.stopPolling()
    }, //生命周期 - 销毁之前
    destroyed () { }, //生命周期 - 销毁完成
    activated () { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.dataScreenshot-box {
    //padding-bottom: 12px;
    padding-inline: 20px;
    min-height: 250px;

    .upload-box {
        display: flex;
        overflow-x: auto;
        overflow-y: hidden;
        margin-bottom: 6px;

        .upload-space {
            padding: 6px 6px 0 6px;
            position: relative;
            //width: 200px;
            min-width: 190px;
            max-width: 220px;
            box-sizing: content-box;

            .upload-btn {
                padding: 6px 12px;
                display: flex;
                align-items: center;
                justify-content: space-between;
            }

            .avatar-box {
                //height: 100%;
                //width: 200px;
                width: 100%;
                max-width: 220px;
                aspect-ratio: 11 / 10;
                max-height: 220px;
                border-radius: 4px;
                text-align: center;
                padding: 12px;
                display: flex;
                border: 1px #fff dashed;
                flex-direction: column;
                justify-content: space-between;

                .identify-success {
                    color: #F56C6C;
                    cursor: pointer;
                }

                .upload-status {
                    font-size: 12px;
                    color: #33C075;
                    font-weight: 500;
                    display: flex;
                    justify-content: end;
                }

                .success-text {
                    color: #74C3A7;
                }

                .error-text {
                    color: #F56C6C;
                }

                .ing-text {
                    color: #1677FF;
                }

                .icon {
                    font-size: 16px;
                    position: absolute;
                    right: 0;
                    bottom: 4px;
                    z-index: 11;
                    color: #9399AC;
                    cursor: pointer;
                }
            }
        }

        .move-right-icon{
            z-index: 9;
            transform: rotate(180deg);
            position: absolute;
            left: 0;
            top: 110px;
        }
        .move-left-icon {
            position: absolute;
            right: -4px;
            top: 110px;
        }
    }

    .upload-box-contrast{
        padding-bottom: 12px;
        .avatar-box {
            width: 220px!important;
        }
    }

    .nimble-box {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-bottom: 8px;

        .nimble-text {
            font-size: 12px;
            color: #484A4D;
            font-weight: 400
        }
    }

    .hidden-input {
        position: absolute;
        width: 1px;
        height: 1px;
        opacity: 0;
        top: 0;
        left: 0;
        pointer-events: none;
    }
}
</style>

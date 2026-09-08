<template>
    <div>
        <el-dialog class="analysisDialog" :visible.sync="visible" width="560px" :close-on-click-modal="false">
            <div class="flex items-center justify-between"
                style="padding: 15px;background: #F4F9FF;">
                <div style="display: flex;align-items: start;flex-direction: column;">
                    <div class="onlineTitle">分享复盘</div>
                    <div class="onlineSubTitle">{{ shareUrl ? '文件上传成功，可以复制分享链接啦' : '将视频上传至云空间以生成分享复盘' }}</div>
                </div>
                <div v-if="!uploading" class="video-mark-close"><img src="../../assets/imgs/close.png"
                        style="width: 20px;height: 20px;" @click="closePop">
                </div>
                <div v-else class="video-mark-close"><img src="../../assets/imgs/min.png"
                        style="width: 20px;height: 20px;" @click="reducePop">
                </div>
            </div>

            <div style="padding-inline: 20px">
                <div class="onlineItemContainer" style="margin-top: 36px;padding: 0;">
                    <div class="onlineItemLabel">云空间容量：</div>
                    <div class="onlineItemText">{{ "已用 " + retainDecimals((userProperty.totalStorageNum -
                        userProperty.storageNum)
                        / 1024 / 1024) + "G / " + retainDecimals(userProperty.totalStorageNum / 1024 / 1024) + "G" }}
                    </div>
                    <div style="margin-left: 34px;color: var(--color-main);font-size: 14px;cursor: pointer;"
                        @click="showCustomerServiceQrCode">
                        点我扩容
                    </div>
                </div>
                <div class="onlineItemContainer" style="padding: 0;">
                    <div class="onlineItemLabel">本地文件名：</div>
                    <div style="display: flex;" class="onlineItemText" v-if="visible">
                        <div>
                            <div style="max-width: 400px;" class="text-clamp1">{{ this.videoInfo.VideoName }}</div>
                            <div style="font-size: 12px;margin-top: 4px">{{ onlineFileInfo.fileSize + "M" }}</div>
                        </div>
                    </div>
                </div>
                <div class="onlineItemContainer" style="padding: 0;display:block">
                    <div class="onlineItemLabel">文件备注：</div>
                    <div class="onlineItemText" v-if="visible" style="margin-top: 6px">
                        <el-input
                            type="textarea"
                            :rows="4"
                            maxlength="100"
                            show-word-limit
                            placeholder="请输入内容"
                            v-model="cloudRemarks">
                        </el-input>
                    </div>
                </div>
                <div class="onlineItemContainer" style="padding: 0px; display: flex; align-items: center;" v-if="false">
                    <div class="onlineItemLabel">
                        <span style="color:#F56C6C;margin-right: 4px;">*</span>
                        查看人群：
                    </div>
                    <div style="display: flex;" class="onlineItemText">
                        <el-checkbox
                            :value="formData.viewCrowdType === 1"
                            :disabled="!isUpload"
                            @change="(val)=>onViewCrowdSelect(1, val)"
                        >本人可见</el-checkbox>
                        <el-checkbox
                            :value="formData.viewCrowdType === 2"
                            style="margin-left: 16px"
                            :disabled="!isUpload"
                            @change="(val)=>onViewCrowdSelect(2, val)"
                        >子公司可见</el-checkbox>
                        <el-checkbox
                            :value="formData.viewCrowdType === 3"
                            style="margin-left: 16px"
                            :disabled="!isUpload"
                            @change="(val)=>onViewCrowdSelect(3, val)"
                        >部门可见</el-checkbox>
                        <el-checkbox
                            :value="formData.viewCrowdType === 4"
                            style="margin-left: 16px"
                            :disabled="!isUpload"
                            @change="(val)=>onViewCrowdSelect(4, val)"
                        >所有人可见</el-checkbox>
                    </div>
                </div>
            </div>

            <div
                style="display: flex; flex-direction: column; align-items: center; padding-bottom: 31px;margin-top: 32px;">
                <!-- <afp-button class="dialog-button" @click="visible = false">取消</afp-button> -->
                <afp-button @click="submit()" type="primary" size="default"
                    v-if="isUpload" :key="'uploadBtn1'">上传</afp-button>
                <afp-button type="info" disabled v-else :key="'uploadBtn2'" size="default">
                    {{ appVnode.isUploading ? "正在上传..." : '上传' }}
                </afp-button>
                <!-- <afp-button style="width: 105px;" type="info"  @click="submit"  v-else :key="'uploadBtn2'">
                    {{appVnode.isUploading?"正在上传...":'上传'}}
                </afp-button> -->

                <div style="display: flex; align-items: center;margin-top: 30px;" v-if="uploading || shareUrl">
                    <div class="progressBarContainer">
                        <div class="progressBar"></div>
                        <div class="progressBarActive" :style="{ width: percent * 300 + 'px' }"></div>
                    </div>
                    <div style="margin-left: 8px;font-size: 12px; color: #4D4D4D;">{{ retainDecimals(percent * 100) }}%
                    </div>
                    <div style="margin-left: 10px;font-size: 12px; color: var(--color-main);cursor: pointer;"
                        @click="cancelUpload" v-if="!shareUrl">取消</div>
                    <div style="margin-left: 10px;font-size: 12px; color: var(--color-main);" v-else>上传成功</div>
                </div>

                <div style="display: flex; align-items: center;margin-top: 20px;" v-if="shareUrl">
                    <div class="shareUrlContainer">
                        {{ shareUrl }}</div>
                    <afp-button style="margin-left: 16px;" type="primary" @click="copyShareAnalysisLink()"
                        :key="'shareBtn1'">复制链接</afp-button>
                </div>
                <div style="display: flex; align-items: center;margin-top: 20px;" v-else>
                    <div class="shareUrlContainer">
                        {{ "http..." }}</div>
                    <afp-button style="margin-left: 16px;" type="info" disabled :key="'shareBtn2'">复制链接</afp-button>
                </div>

                <div style="font-size: 12px; color: #909499;margin-top: 10px;" v-if="uploading">
                    视频上传中，请保持网络稳定，请勿退出当前页面，否则将中断上传
                </div>

            </div>
            <!-- <div style="display: flex;justify-content: center; padding-bottom: 31px;margin-top: 32px;" v-else>
                <div class="shareUrlContainer">
                    {{ shareUrl }}</div>
                <afp-button style="margin-left: 16px;" type="primary" @click="copyShareAnalysisLink()">复制链接</afp-button>
            </div> -->
        </el-dialog>
        <!-- <div class="zhezhao"></div> -->
        <!-- <div class="uploadProgressPopContainer" v-if="reduce" @click="reduce = false; visible = true;">
            <div class="uploadProgressPopContainer-text" style="color: #0077FF;font-size: 10px;">
                <span v-if="uploading">正在上传</span>
                <span v-else style="color: #07AF54;">上传成功</span>
            </div>
        </div> -->

        <!-- 客服二维码 -->
        <customer-service-qr-code v-if="customerServiceQrCodeVisible"
            ref="customerServiceQrCode"></customer-service-qr-code>
    </div>
</template>

<script>
import myUtils from '../../utils/utils';
import axios from 'axios';
import TcVod from 'vod-js-sdk-v6'
import CustomerServiceQrCode from './customerServiceQrCode';

export default {
    components: {
        CustomerServiceQrCode
    },
    data() {
        return {
            customerServiceQrCodeVisible: false, // 显示客服二维码弹窗
            shareUrl: "",
            visible: false,
            userProperty: {},
            onlineFileInfo: {},
            tcVod: null,
            percent: 0,
            uploader: null,
            uploading: false,
            videoInfo: null,
            anchorInfo: null,
            cloudRemarks: '',//文件备注
            formData: {
                shareTypes:[],
                viewCrowdType: 2
            }
        };
    },
    computed:{
        isUpload(){
            return !this.uploading && !this.shareUrl && !this.appVnode.isUploading
        }
    },  
    inject: ['appVnode'],
    // props: {
    //     shareUrl: {
    //         type: String
    //     }
    // },
    beforeDestroy() {
        if (this.uploader && this.uploading) {
            this.cancelUpload();
        }
    },
    watch: {
        uploading(val) {
            this.$emit('updateLoading', val)
        }
    },
    methods: {
        // 显示客服二维码
        showCustomerServiceQrCode() {
            this.customerServiceQrCodeVisible = true;
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init();
            });
        },
        // 缩小上传弹窗
        reducePop() {
            this.visible = false;
            // this.reduce = true;
            this.$emit('reducePop', true)
        },
        // 关闭上传弹窗
        closePop() {
            // let uploadData = {
            //     uploading: this.uploading,
            //     percent: this.percent,
            // };
            // // this.$store.commit("saveTest", uploadData);
            // localStorage.setItem("uploadData", JSON.stringify(uploadData));

            this.visible = false;
            this.$emit('close', 'online')
        },
        // 复制分享链接
        copyShareAnalysisLink() {
            this.appVnode.copyShareUrl(this.shareUrl);
        },

        init(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo) {

            let currentUploadVideoId = this.$store.state.currentUploadVideoId;
            if (currentUploadVideoId && currentUploadVideoId != videoInfo.VideoId) {
                this.$message.warning("请等待正在上传的视频上传完成再继续")
                return;
            }

            this.userProperty = userProperty;
            this.onlineFileInfo = onlineFileInfo;
            this.shareUrl = shareUrl;
            this.videoInfo = videoInfo;
            this.anchorInfo = anchorInfo;
            if (!this.uploading) {
                this.formData.viewCrowdType = 2
            }

            // this.reduce = false;
            // this.$emit('reducePop', false)
            this.visible = true;
            this.$nextTick(() => {
                // let uploadData = JSON.parse(localStorage.getItem("uploadData"));
                // // let uploadData = this.$store.state.test;
                // if (uploadData) {
                //     this.percent = uploadData.percent;
                //     this.uploading = uploadData.uploading;
                //     // this.uploader = uploadData.uploader;

                //     localStorage.setItem("uploadData", "");
                // } else {
                //     this.percent = this.shareUrl ? 1 : 0;
                // }

                this.percent = this.shareUrl ? 1 : 0;

            })

        },
        retainDecimals(val) {
            return myUtils.retainDecimals(val);
        },
        submit() {
            if (!this.formData.viewCrowdType) {
                this.$message.warning('请选择查看人群')
                return
            }
            this.uploadToVod();
        },
        onViewCrowdSelect(type, val) {
            if (val) {
                this.formData.viewCrowdType = type
                return
            }
            this.formData.viewCrowdType = 0
        },
        // 获取签名
        getVodUploadSign() {
            return this.$httpBack.vod.vodUploadSign().then(res => {
                return res.data;
            });
        },
        // 消耗存储资源
        useStoreProperty() {
            this.$httpBack.userProperty.useProperty({ code: "storageNum", num: Math.ceil(this.onlineFileInfo.fileSize * 1024) }).then(res => {

            });
        },
        // 修改文件上传状态
        updateVideoShareStatus(vodUrl) {
            this.$httpBack.video.shareVideoToCloud({ videoId: this.videoInfo.VideoId, onlineFileUrl: vodUrl, cloudRemarks: this.cloudRemarks, viewCrowdType: this.formData.viewCrowdType }).then(res => {
                if (res.code == 0 && res.data) {
                    this.$message.success("文件上传成功，可以复制分享链接啦");
                    this.videoInfo.UploadStatus = 1;
                    this.videoInfo.ShareUrl = res.data;
                    this.shareUrl = res.data;
                }
            });
        },
        cancelUpload() {
            this.$store.commit("saveCurrentUploadVideoId", "");
            this.uploader.cancel();
            this.uploading = false;
        },
        // 上传到腾讯云点播
        async uploadToVod() {

            localStorage.setItem("notCloseLoading", "1");
            const loading = this.$loading({
                lock: true,
                text: '正在构建上传，请稍后...',
                background: 'rgba(0, 0, 0, 0.7)'
            });


            // setTimeout(()=>{
            //     this.uploading = true;
            //     loading.close();
            //     localStorage.setItem("notCloseLoading", "");
            //     setTimeout(()=>{
            //         this.uploading = false;
            //         this.$emit('reducePop', false)
            //     },30000)
            // },1000)
            // return

            // const response = await axios.get('http://127.0.0.1:5001/api/config/getFile?filePath=' + this.onlineFileInfo.filePath, {
            const response = await axios.get(`${window.SITE_CONFIG['clientApiURL']}/config/getFile?filePath=` + encodeURIComponent(this.onlineFileInfo.filePath), {
                responseType: 'blob',
                timeout: 300*1000,
            }).catch(error=>{
                if (error.code === 'ECONNABORTED') {
                    this.$message.error('构建上传失败，请检查一下本地网络，或联系售后技术帮您解决！');
                } else {
                    console.error('其他错误:', error.message);
                }
                return {
                    error: true,
                }
            });
            if(response.error){
                loading.close();
                localStorage.setItem("notCloseLoading", "");
                return;
            }


            const file = new File([response.data], 'temp.mp4', { type: 'video/mp4' });

            this.tcVod = new TcVod({
                getSignature: this.getVodUploadSign
            });

            this.uploader = this.tcVod.upload({
                mediaFile: file, // 媒体文件（视频或音频或图片），类型为 File
            });

            // 保存当前上传的视频id
            this.$store.commit("saveCurrentUploadVideoId", this.videoInfo.VideoId);

            localStorage.setItem("notCloseLoading", "");
            loading.close();

            this.uploading = true;

            let than = this;
            this.uploader.on('media_progress', function (info) {
                than.percent = info.percent;
            })


            this.uploader.done().then(function (doneResult) {
                // deal with doneResult
                // localStorage.setItem("notCloseLoading", "");
                // loading.close();
                than.$store.commit("saveCurrentUploadVideoId", "");
                than.uploading = false;
                // than.$emit('reducePop', false)
                than.useStoreProperty();
                than.updateVideoShareStatus(doneResult.video.url);

                // than.$emit("uploadVodSuccess", doneResult.video.url);

                // than.shareUrl = doneResult.video.url;
                // than.visible = false;

            }).catch(function (err) {
                // deal with error
                than.$store.commit("saveCurrentUploadVideoId", "");
                than.uploading = false;
                // than.$emit('reducePop', false)
                // localStorage.setItem("notCloseLoading", "");
                // loading.close();
                than.$message.error("上传失败");
            });
        },
    }
}
</script>

<style scoped lang="scss">
.analysisDialog {
    ::v-deep(.el-dialog__body) {
        padding: 0;
    }

    ::v-deep(.el-dialog__header) {
        display: none;
    }
}
</style>

<style scoped>
.progressBarActive {
    position: absolute;
    left: 0;
    top: 0;
    height: 6px;
    border-radius: 36px;
    z-index: 2;
    width: 100px;
    background-color: var(--color-main);
}

.progressBar {
    position: absolute;
    left: 0;
    top: 0;
    width: 300px;
    height: 6px;
    border-radius: 36px;
    background: #E6E6E6;
    z-index: 1;
}

.progressBarContainer {
    position: relative;
    width: 300px;
    height: 6px;

}

.shareUrlContainer {
    width: 300px;
    background: rgb(248, 248, 248);
    padding: 10px 16px;
    white-space: nowrap;
    /* 阻止换行 */
    overflow: hidden;
    /* 超出部分隐藏 */
    text-overflow: ellipsis;
    /* 溢出部分用省略号代替 */
}

.zhezhao {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vh;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.8);
    z-index: 99999;
}

.onlineItemText {
    font-size: 14px;
    color: #2E3742;
    margin-left: 8px;
    display: flex;
    align-items: center;
}

.onlineItemLabel {
    text-align: right;
    font-weight: 500;
    font-size: 14px;
    color: #2E3742;
    width: 88px;
}

.onlineItemContainer {
    display: flex;
    align-items: center;
    padding-left: 0px;
    margin-top: 20px;
}

.video-mark-close {
    margin-right: 16px;
    cursor: pointer;
}

.onlineSubTitle {
    display: flex;
    justify-content: center;
    font-size: 14px;
    color: #677583;
    margin-top: 6px;
}

.onlineTitle {
    font-weight: bold;
    display: flex;
    justify-content: center;
    font-size: 16px;
    color: #2E3742;
}

.form-time {
    display: flex;
    align-items: center;
    margin: 19px 0px 0px 39px
}

.form-tiem-item {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 88px;
    height: 32px;
    border: 1px solid #CFD4DB;
    border-radius: 4px;
}

.select-bacc-color {
    display: flex;
    justify-content: center;
    position: absolute;
    color: #2E3742;
    z-index: 2;
    width: 66px;
    border-radius: 4px;
}

.select-color-red {
    background-color: red;
}

.select-color-yellow {
    background-color: yellow;
}

.select-color-blue {
    background-color: var(--color-main);
}

.dialog-button {
    width: 110px;
    height: 40px;
    border-radius: 4px;
    border: 1px solid #B4BCCA;
    color: 14px;
}

.add-yes {
    background-color: var(--color-main);
    color: #FFFFFF;
}
</style>

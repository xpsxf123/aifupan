<template>
    <div>
        <el-dialog class="analysisDialog" :visible.sync="visible" width="520px" :close-on-click-modal="false">
            <div
                style="display: flex;justify-content: space-between;align-items: center;margin-left: 16px;padding-top: 15px;">
                <div style="display: flex;align-items: start;flex-direction: column;">
                    <div class="onlineTitle">分享复盘</div>
                    <div class="onlineSubTitle">{{ shareUrl ? '文件上传成功，可以复制分享链接啦' : '将视频上传至云空间以生成分享复盘' }}</div>
                </div>
                <div class="video-mark-add-close" v-if="!uploading"><img src="../../assets/imgs/close.png"
                        style="width: 16px;height: 16px;" @click="closePop">
                </div>
                <div class="video-mark-add-close" v-else><img src="../../assets/imgs/min.png"
                        style="width: 16px;height: 16px;" @click="reducePop">
                </div>
            </div>

            <div style="width: 100%;height: 1px;background-color: #E3E3E5;margin-top: 15px;"></div>

            <div style="margin-inline: 17px;" v-if="visible">
                <div class="onlineItemContainer" style="margin-top: 36px;">
                    <div class="onlineItemLabel">空间容量：</div>
                    <div class="onlineItemText">{{ "已用 " + retainDecimals((userProperty.totalStorageNum -
                        userProperty.storageNum)
                        / 1024 / 1024) + "G / " + retainDecimals(userProperty.totalStorageNum / 1024 / 1024) + "G" }}
                    </div>
                </div>
                <div class="onlineItemContainer">
                    <div class="onlineItemLabel">大小时长：</div>
                    <div class="onlineItemText">{{ onlineFileInfo.videoList.length + "个视频，时长：共" +
                        onlineFileInfo.duration + "分钟，预计消耗" +
                        onlineFileInfo.fileSize
                        +
                        "M" }}</div>
                </div>
                <div class="onlineItemContainer">
                    <div class="onlineItemLabel">文件备注：</div>
                    <div class="onlineItemText" v-if="visible" style="margin-top: 6px;flex: 1">
                        <el-input
                            type="textarea"
                            style="width: 100%;"
                            :rows="4"
                            maxlength="100"
                            show-word-limit
                            :disabled="!isUpload"
                            placeholder="请输入内容"
                            v-model="cloudRemarks">
                        </el-input>
                    </div>
                </div>
                <div class="onlineItemContainer" style="display: flex; align-items: center;" v-if="false">
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
                <!-- <div class="onlineItemContainer" style="padding: 0px; display: flex; align-items: center;">
                    <div class="onlineItemLabel">查看人群：</div>
                    <div style="display: flex;" class="onlineItemText">
                        <el-radio-group v-model="formData.crowd">
                            <el-radio :label="0" :disabled="!isUpload">任何人可查看</el-radio>
                            <el-radio :label="1" :disabled="!isUpload">仅限机构内账号可查看</el-radio>
                        </el-radio-group>
                        <div style="padding-left: 5px;margin-top: -2px;">
                            <el-tooltip class="item" effect="dark" content="机构内成员指，同属于同一个主账号的成员可查看，其他人不可查看"
                                placement="bottom">
                                <i class="font_family icon-bangzhuzhongxinoff"></i>
                            </el-tooltip>
                        </div>
                    </div>
                </div>
                <div class="onlineItemContainer" style="padding: 0px;">
                    <div class="onlineItemLabel">查看人群：</div>
                    <div style="display: flex;" class="onlineItemText">
                        <div>
                            <el-checkbox-group v-model="formData.shareTypes" >
                                <el-checkbox label="m" :disabled="!isUpload">可查看敏感词</el-checkbox>
                                <el-checkbox label="g" :disabled="!isUpload">可查看关键词</el-checkbox>
                            </el-checkbox-group>
                        </div>
                    </div>
                </div> -->
                <div style="text-align: center; margin-top: 16px;margin-bottom: 16px;">
                    <!-- <afp-button class="dialog-button" @click="visible = false">取消</afp-button> -->
                    <!-- {{ !uploading }}/{{ !shareUrl }}/{{ !appVnode.isUploading }} -->
                    <afp-button style="width: 105px;" @click="submit()" type="primary" v-if="isUpload"
                        :key="'uploadBtn1'">上传</afp-button>
                    <afp-button style="width: 105px;" type="info" disabled v-else-if="!isUploadSuccess" :key="'uploadBtn2'">
                        {{appVnode.isUploading?"正在上传...":'上传'}}
                    </afp-button>
                    <!-- <afp-button style="width: 105px;" type="info" @click="submit" v-else :key="'uploadBtn2'">
                        {{appVnode.isUploading?"正在上传...":'上传'}}
                    </afp-button> -->
                </div>
            </div>

            <hr v-if="(uploading && !shareUrl) || shareUrl">
            <div style="display: flex; flex-direction: column; align-items: center; padding-bottom: 31px;" >
                <!-- 单个视频上传 -->
                <div class="percent-box"  style="margin-top: 10px;" v-if="uploading && !shareUrl">
                    <div class="font-s12">{{ fileName0 }}</div>
                    <div style="display: flex; align-items: center;">
                        <div class="progressBarContainer">
                            <div class="progressBar"></div>
                            <div class="progressBarActive" :style="{ width: (percent0 * 300) + 'px' }"></div>
                        </div>
                        <div style="margin-left: 8px;font-size: 12px; color: #4D4D4D;">{{ parseInt(percent0 * 100) }}%</div>
                        <div style="margin-left: 10px;font-size: 12px; color: var(--color-main);cursor: pointer;"
                            @click="cancelUpload" v-if="!shareUrl">取消</div>
                        <div style="margin-left: 10px;font-size: 12px; color: var(--color-main);" v-else>上传成功</div>
                    </div>
                </div>
                <!-- 2个视频显示2个进度条 -->
                <div  class="percent-box" style="margin-top: 10px;" v-if="(uploading && !shareUrl) && videoListLen>1">
                    <div class="font-s12">{{ fileName1 }}</div>
                    <div style="display: flex; align-items: center;">
                        <div class="progressBarContainer">
                            <div class="progressBar"></div>
                            <div class="progressBarActive" :style="{ width: (percent1 * 300) + 'px' }"></div>
                        </div>
                        <div style="margin-left: 8px;font-size: 12px; color: #4D4D4D;">{{ parseInt(percent1 * 100) }}%</div>
                        <div style="margin-left: 10px;font-size: 12px; color: var(--color-main);cursor: pointer;"
                            @click="cancelUpload" v-if="!shareUrl">取消</div>
                        <div style="margin-left: 10px;font-size: 12px; color: var(--color-main);" v-else>上传成功</div>
                    </div>
                </div>

                <div style="margin-top: 20px;" v-if="shareUrl">
                    <div class="font-s12 text-center mg-b10 text-colorc2">云空间上传成功，点击复制即可分享</div>
                    <div style="display: flex; align-items: center;" v-if="shareUrl">
                        <div class="shareUrlContainer">
                            {{ shareUrl }}</div>
                        <afp-button style="margin-left: 16px;" type="primary" @click="copyShareAnalysisLink()"
                            :key="'shareBtn1'">复制链接</afp-button>
                    </div>
                </div>
                <!-- <div style="display: flex; align-items: center;margin-top: 20px;" v-else>
                    <div class="shareUrlContainer">
                        {{ "http..." }}</div>
                    <afp-button style="margin-left: 16px;" type="info" disabled :key="'shareBtn2'">复制链接</afp-button>
                </div> -->

                <div style="font-size: 12px; color: #909499;margin-top: 10px;" v-if="uploading">
                    视频上传中，请保持网络稳定，请勿退出当前页面，否则将中断上传
                </div>

            </div>
            <!-- <div style="display: flex;justify-content: center; padding-bottom: 31px;margin-top: 32px;" v-if="!shareUrl">
                <afp-button class="dialog-button" @click="visible = false">取消</afp-button>
                <afp-button class="dialog-button add-yes" @click="submit()">上传</afp-button>
            </div>
            <div style="display: flex;justify-content: center; padding-bottom: 31px;margin-top: 32px;" v-else>
                <div class="shareUrlContainer">
                    {{ shareUrl }}</div>
                <afp-button style="margin-left: 16px;" type="primary" @click="copyShareAnalysisLink()">复制链接</afp-button>
            </div> -->
        </el-dialog>
        <!-- <div class="zhezhao"></div> -->
    </div>
</template>

<script>
import myUtils from '../../utils/utils';
import axios from 'axios';
import TcVod from 'vod-js-sdk-v6'

export default {
    data() {
        return {
            visible: false,
            userProperty: {},
            onlineFileInfo: {},
            tcVod: null,
            percent0: 0,
            percent1: 0,
            fileName0: '',
            fileName1: '',
            uploadSuccessNum: 0,
            uploaderArr: [],
            uploading: false,
            cloudRemarks:'',
            formData: {
                shareTypes:[],
                viewCrowdType: 2
            },
            isUploadSuccess: false
        };
    },
    inject: ['appVnode'],
    props: {
        shareUrl: {
            type: String
        },
    },
    watch:{
        uploading(val){
            this.$emit('updateLoading',val)
        }
    },
    beforeDestroy() {
        if (this.uploader && this.uploading) {
            this.cancelUpload();
        }
    },
    computed:{
        isUpload(){
            return !this.uploading && !this.shareUrl && !this.appVnode.isUploading
        },
        videoListLen(){
            return this.onlineFileInfo?.videoList?.length
        }
    },
    methods: {
        
        // 缩小上传弹窗
        reducePop() {
            this.visible = false;
            this.$emit('reducePop', true)
        },
        closePop(){
            this.visible = false;
            this.$emit('close','contrast')
        },
        // 复制分享链接
        copyShareAnalysisLink() {
            this.appVnode.copyShareUrl(this.shareUrl);
        },

        init(userProperty, onlineFileInfo) {

            if (!this.uploading) {
                this.uploadSuccessNum = 0;
                this.userProperty = userProperty;
                this.onlineFileInfo = onlineFileInfo;
                this.cloudRemarks = ''
                this.formData.viewCrowdType = 2
                this.isUploadSuccess = false
            }

            this.visible = true;

            // this.$nextTick(() => {
            //     // this.percent0 = this.shareUrl ? 1 : 0;
            //     // this.percent1 = this.shareUrl ? 1 : 0;
            //     // this.percent = this.shareUrl ? 1 : 0;
            // });
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
            this.$httpBack.userProperty.useProperty({ code: "storageNum", num: this.onlineFileInfo.fileSize * 1024 }).then(res => {

            });
        },
        cancelUpload() {
            this.uploaderArr.forEach(item => {
                if (item) {
                    item.cancel();
                }
            });
            this.uploading = false;
        },
        // 上传到腾讯云点播
        async uploadToVod() {
            let loading = this.$loading({
                lock: true,
                text: '正在上传，请稍后...',
                background: 'rgba(0, 0, 0, 0.7)'
            });
            localStorage.setItem("notCloseLoading", "1");
            // setTimeout(()=>{
            //     this.uploading = true;
            //     loading.close();
            //     localStorage.setItem("notCloseLoading", "");
            //     setTimeout(()=>{
            //         this.uploading = false;
            //         this.$emit("uploadVodSuccess", [1,2,3])
            //         this.$emit('reducePop', false)
            //     },30000)
            // },1000)
            // return
            console.log(this.onlineFileInfo.videoList,'---this.onlineFileInfo.videoList')
            for (const [index, video] of this.onlineFileInfo.videoList.entries()) {
                this[`fileName${index}`] = video.filePath?.split('\\').pop()?.split('.')[0];
                // const response = await axios.get('http://127.0.0.1:5001/api/config/getFile?filePath=' + video.filePath, {
                const response = await axios.get(`${window.SITE_CONFIG['clientApiURL']}/config/getFile?filePath=` + encodeURIComponent(video.filePath), {
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
                    loading?.close();
                    localStorage.setItem("notCloseLoading", "");
                    return;
                }

                const file = new File([response.data], 'temp.mp4', { type: 'video/mp4' });

                this.tcVod = new TcVod({
                    getSignature: this.getVodUploadSign
                });

                const uploader = this.tcVod.upload({
                    mediaFile: file, // 媒体文件（视频或音频或图片），类型为 File
                });

                this.uploaderArr.push(uploader);

                if (index + 1 == this.onlineFileInfo.videoList.length) {
                    this.uploading = true;
                    this.$emit("updateUploadStatus", this.uploading)
                }
                localStorage.setItem("notCloseLoading", "");
                loading?.close();

                let than = this;
                uploader.on('media_progress', function (info) {
                    than.$set(than, `percent${index}`, info.percent);
                    // const sum = than.percentMap.reduce((accumulator, currentValue) => accumulator + currentValue, 0);
                    // than.percent = sum / than.percentMap.length;
                })


                uploader.done().then(function (doneResult) {
                    // deal with doneResult

                    than.uploadSuccessNum++;
                    video.playUrl = doneResult.video.url;

                    if (than.uploadSuccessNum == than.onlineFileInfo.videoList.length) {
                        than.useStoreProperty();
                        loading?.close();
                        localStorage.setItem("notCloseLoading", "");
                        than.isUploadSuccess = true;
                        // than.$emit('reducePop', false)
                        than.$emit("uploadVodSuccess", than.onlineFileInfo.videoList, than.cloudRemarks, than.formData.viewCrowdType);
                        // than.visible = false;
                        setTimeout(()=>{
                            than.uploading = false;
                            than.$emit("updateUploadStatus", than.uploading)
                        },1000)
                    }

                }).catch(function (err) {
                    // deal with error
                    loading?.close();
                    localStorage.setItem("notCloseLoading", "");
                    than.$message.error("上传失败");
                    than.uploading = false;
                    // than.$emit('reducePop', false)
                    than.$emit("updateUploadStatus", than.uploading)
                });
            }
        },
    }
}
</script>
<style scoped lang="scss">
.analysisDialog{
    ::v-deep(.el-dialog__body){
        padding: 0;
    }
    ::v-deep(.el-dialog__header) {
        display: none;
    }
}
</style>

<style>
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
    margin-left: 20px;
}

.onlineItemLabel {
    font-weight: bold;
    font-size: 14px;
    color: #2E3742;
}

.onlineItemContainer {
    display: flex;
    align-items: center;
    padding-left: 0px!important;
    margin-top: 24px;
}

.video-mark-add-close {
    display: flex;
    justify-content: end;
    margin-right: 16px;
    padding-top: 14px;
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

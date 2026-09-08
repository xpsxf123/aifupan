<template>
    <div class="fileColContainer" v-if="anchorVideo && anchorVideo.videoId">
        <img v-if="uploadStatus" src="@/assets/imgs/scdb.png" class="uploadStatus" alt=""/>
        <template v-if="anchorVideo.syncScene===1">
            <img src="@/assets/imgs/ben.png" v-if="anchorVideo?.optimize" class="upload-dui-you" alt=""/>
            <img src="@/assets/imgs/shang.png" v-else class="upload-dui-you" alt=""/>
        </template>
        <template v-if="anchorVideo.syncScene!==1&&anchorVideo?.optimize!==undefined">
            <img src="@/assets/imgs/you.png" v-if="anchorVideo?.optimize" class="upload-dui-you" alt=""/>
            <img src="@/assets/imgs/2_5_8/can.png" v-else class="upload-dui-you" alt=""/>
        </template>
        <img :src="anchorInfo?.anchorAvatar" class="videoImg" alt=""/>
        <div class="fileNameContainer">
            <div class="videoNameText">{{ anchorVideo ? anchorVideo.videoName?.replace('.ts', '')?.replace('.mp4','')?.replace('.flv', '') : '' }}
            </div>
            <div class="compereNameText">{{ anchorInfo ? anchorInfo.anchorName?.replace('.ts', '')?.replace('.mp4','')?.replace('.flv', '') : '' }}
            </div>
        </div>
    </div>
    <div class="fileColContainer" v-else>
        <img  v-if="uploadStatus" src="@/assets/imgs/scdb.png" class="uploadStatus" alt=""/>
        <img src="@/assets/imgs/txt.png" class="videoImg" v-if="fileInfo?.fileType == 2" alt=""/>
        <img src="@/assets/imgs/video.png" class="videoImg" v-else alt=""/>
        <template v-if="anchorVideo.syncScene===1">
            <img src="@/assets/imgs/ben.png" v-if="anchorVideo?.optimize" class="upload-dui-you" alt=""/>
            <img src="@/assets/imgs/shang.png" v-else class="upload-dui-you" alt=""/>
        </template>
        <template v-if="anchorVideo.syncScene!==1&&anchorVideo?.optimize!==undefined">
            <img src="@/assets/imgs/you.png" v-if="anchorVideo?.optimize" class="upload-dui-you" alt=""/>
            <img src="@/assets/imgs/2_5_8/can.png" v-else class="upload-dui-you" alt=""/>
        </template>
        <div class="fileNameContainer">
            <div class="videoNameText">{{ fileInfo ? fileInfo.fileName?.replace('.ts', '')?.replace('.mp4','')?.replace('.flv', '') : '' }}</div>
        </div>
    </div>
</template>

<script>
//syncScene:对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
export default {
    props:{
        anchorVideo: {
            type: Object,
            default: ()=>{
                return {}
            }
        },
        anchorInfo: {
            type: Object,
            default: ()=>{
                return {}
            }
        },
        fileInfo: {
            type: Object,
            default: ()=>{
                return {}
            }
        },
        notUploadStatus:{
            type:Boolean,
            default: false
        }
    },
    data() {
        return {
        
        };
    },
    computed: {
        uploadStatus(){
            return (this.anchorVideo?.uploadStatus || this.fileInfo?.uploadStatus) && !this.notUploadStatus;
        }
    },
    watch: {},
    methods: {
        
    },
    created() {
        
    },
    mounted() {
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.videoImg {
    width: 39px;
    height: 39px;
}
.uploadStatus{
    position: absolute;
    width: 40px;
    height: 40px;
}
.upload-dui-you{
    position: absolute;
    top: 0;
    left: 30px;
}
.fileColContainer {
    display: flex;
    flex: 2;
    align-items: center;
    position: relative;
}
.fileNameContainer {
    display: flex;
    flex-direction: column;
    margin-left: 12px;
}
.videoNameText {
    font-weight: 400;
    font-size: 14px;
    color: #2E3742;
}
.compereNameText {
    font-weight: 400;
    font-size: 13px;
    color: #677583;
    margin-top: 4px;
}
</style>
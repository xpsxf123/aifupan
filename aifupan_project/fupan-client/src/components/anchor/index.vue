<template>
    <div class="anchor-box flex-jc-s flex-ji-c w100">
        <div class="anchor-img-box">
            <div class="uploadStatus" v-if="uploadStatus">
                <img v-if="circle" src="@/assets/imgs/scfx.png" alt=""></img>
                <img v-else src="@/assets/imgs/scdb.png"></img>
            </div>
            <div class="anchor">
                <img :src="displayAvatar" :class="{'anchor-circle':circle}">
            </div>
        </div>
        <div class="pd-l8 slh">
            <div class="text-colorMain w100 slh"><b>{{ getAnchorName }}</b></div>
            <div v-if="!notInfo" class="text-color3 slh">
                <slot name="info">
                    <template v-if="isFile">
                        <b class="slh">{{ getInfoTime }}</b>
                    </template>
                    <template v-else>
                        <span class="slh" v-if="!resolvedInfoTimeMode">{{ getVideoOrFileName }}</span>
                        <b class="slh" v-else>{{ getInfoTime }}</b>
                    </template>
                </slot>
            </div>
        </div>
    </div>
</template>

<script>
import defaultAvatar from '@/assets/imgs/video.png';

export default {
    components: {},
    props: {
        item: {
            type: Object,
            default: () => {
                return {}
            }
        },
        notUploadStatus: {
            type: [Boolean,Number],
            default: false
        },
        circle: {
            type: Boolean,
            default: false
        },
        notInfo: {
            type: Boolean,
            default: false
        },
        infoTime: {
            type: [Boolean, String],
            default: false
        }
    },
    data() {
        return {

        };
    },
    computed: {
        getItem(){
            return this.item
        },
        uploadStatus(){
            return (this.getItem?.uploadStatus) && !this.notUploadStatus;
        },
        isFile(){
            return typeof this.getItem?.fileType !== 'undefined'
                || !!this.getItem?.fileId
                || !!this.getItem?.fileName
        },
        getAnchor(){
            return this.getItem?.anchorInfo || this.getItem
        },
        getAnchorName(){
            if (this.isFile) {
                return this.getItem?.fileName || this.getItem?.FileName || this.getItem?.videoName || '-';
            }
            return this.getAnchor?.anchorName || this.getAnchor?.AnchorName || this.getItem?.roomName || this.getItem?.RoomName || '-';
        },
        resolvedInfoTimeMode() {
            if (!this.infoTime) return '';
            if (this.infoTime === true) return 'upload';
            const mode = String(this.infoTime || '').trim().toLowerCase();
            if (mode.indexOf('analysis') >= 0) return 'analysis';
            if (mode.indexOf('upload') >= 0) return 'upload';
            return '';
        },
        rawAvatar(){
            return this.getAnchor?.anchorAvatar
                || this.getAnchor?.AnchorAvatar
                || this.getItem?.avatar
                || this.getItem?.Avatar
                || '';
        },
        displayAvatar(){
            return this.rawAvatar || defaultAvatar
        },
        getVideoOrFileName(){
            return this.getItem?.videoName || this.getItem?.VideoName || this.getItem?.fileName || this.getItem?.FileName || '-';
        },
        getInfoTime(){
            const uploadTime = this.getItem?.uploadTime || this.getItem?.UploadTime || this.getItem?.createTime || this.getItem?.CreateTime || '';
            const analysisTime = this.getItem?.analysisTime || this.getItem?.AnalysisTime || this.getItem?.finishTime || this.getItem?.FinishTime || '';
            if (this.resolvedInfoTimeMode === 'analysis') {
                return analysisTime || uploadTime || '-';
            }
            return uploadTime || analysisTime || '-';
        }
    },
    watch: {},
    methods: {

    },
    created() {

    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.anchor-img-box{
    position: relative;
}
.anchor {
    width: 39px;
    height: 39px;
    img{
        max-width: 100%;
    }
}
.anchor-circle{
    border-radius: 50%;
}
.uploadStatus{
    width: 40px;
    height: 40px;
    position: absolute;
    img{
        max-width: 100%;
    }
}


</style>

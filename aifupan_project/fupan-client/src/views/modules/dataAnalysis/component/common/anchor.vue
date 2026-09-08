<template>
    <div class="compereColContainer" slot="reference" @click="handleClick" :class="{'cs-p':$listeners.click || (isToHomeUrl && getItem.platform !== 2)}">
        <img v-if="isUploadStatus" src="@/assets/imgs/scfx.png" class="uploadStatus" alt="">
        <template v-else>
            <img src="@/assets/imgs/zy.png" v-if="getItem.accountType === 0" class="avatar-back" alt="">
            <img src="@/assets/imgs/hy.png" v-if="getItem.accountType === 1" class="avatar-back" alt="">
        </template>
        <img :src="getItem.anchorAvatar" class="compereImg" alt="">
        <div class="compereInfoContainer slh">
            <div class="slh text-left cursor-pointer" :class="{compereName:true,anchorName_del_video:!!getItem.localVideoStatus}" @dblclick="$emit('editFileName')">
<!--                <span :style="{color:getItem.isAutoRecord?'':'red'}">-->
<!--                    {{ getItem.remarksName || getItem.anchorName }}-->
<!--                </span>-->
                {{getVideoName}}
            </div>
            <div v-if="anchorPositionVisible" class="anchorPositionLine slh">
                <!-- <span class="anchorPositionLabel">{{ anchorPositionName }}：</span> -->
                <span class="anchorPositionNames">{{ anchorEmployeeNames }}</span>
            </div>
            <div class="compereStatusContainer">
                <slot name="anchorBottom">
                <template v-if="!notLiveStatus">
                    <template v-if="getItem.IsAutoRecord===0">
                        <div class="text-xs" style="color: #28BD6C;padding: 3px 0 4px 0">暂停自动录制</div>
                    </template>
                    <template v-if="getItem.IsAutoRecord===1">
                        <div v-if="getItem.ScheduleStatus === 1" class="text-xs" style="color: #28BD6C;padding: 3px 0 4px 0">未排班不录制</div>
                        <div v-else-if="getItem.liveStatus == 0" class="compereNotPlayStatus">未检测</div>
                        <div v-if="getItem.liveStatus == -1" class="compereNotPlayStatus">检测中</div>
                        <div v-if="getItem.liveStatus == 4" class="compereNotPlayStatus">未开播</div>
                        <div v-if="getItem.liveStatus == 2" class="comperePlayStatus">
                            <img src="@/assets/imgs/play.png" class="comperePlayStatusImg">
                            <div class="comperePlayStatusText">直播中</div>
                        </div>
                    </template>
                </template>
                <template v-if="isUnf(getItem.AnchorPlatform)">
                    <div class="compereResourceType"
                         v-if="getItem.platform == PLATFORM_ENUM.douyin">
                        <img src="@/assets/imgs/icon/douyin.png" class="compereResourceTypeImg">
                    </div>
                    <div class="compereResourceType"
                         v-else-if="getItem.platform == PLATFORM_ENUM.kuaishou">
                        <img src="@/assets/imgs/icon/kuaishou.png" class="compereResourceTypeImg">
                    </div>
                    <div class="compereResourceType"
                         v-else-if="getItem.platform == PLATFORM_ENUM.shipinhao">
                        <img src="@/assets/imgs/icon/shiping.png" class="compereResourceTypeImg">
                    </div>
                    <div class="compereResourceType"
                         v-if="isAiReport">
                        <img src="@/assets/imgs/zhen.png" class="compereResourceTypeImg">
                    </div>
                    <div class="compereResourceType"
                         v-if="isMonitorTime">
                        <img src="@/assets/imgs/monitor.png" class="compereResourceTypeImg">
                    </div>
                    <div class="compereResourceType"
                         v-if="isDefaultRecordSettingChange">
                        <img src="@/assets/imgs/pei.png" class="compereResourceTypeImg">
                    </div>
                    <div class="compereResourceType"
                         v-if="getItem.isScheduleRecord === 1">
                        <img src="@/assets/imgs/icon/ban.png" class="compereResourceTypeImg">
                    </div>
                </template>
                <template v-if="isUnf(getItem.platform)&&!versionTypeIsPure && !notTag">
                    <ActivatedServiceIcons :item="getItem" :replayType="replayType" />
                </template>
                </slot>
            </div>
        </div>
    </div>
</template>

<script>
import {PLATFORM_ENUM, VERSION_TYPE} from "@/enum";
import {isEqual} from "lodash";
import ActivatedServiceIcons from '@/views/commonComponent/activatedServiceIcons/index.vue'
export default {
    components: { ActivatedServiceIcons },
    props: {
        item: {
            type: Object,
            default: () => {
                return {}
            }
        },
        notLiveStatus: {
            type: Boolean,
            default: false
        },
        uploadStatus: {
            type: [Boolean,Number],
            default: false
        },
        isToHomeUrl: {
            type: Boolean,
            default: false
        },
        replayType: {
            type: String,
            default: ''
        },
        notTag:{
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            PLATFORM_ENUM,
        };
    },
    computed: {
        getItem() {
            let o = this.item && Object.fromEntries(Object.keys(this.item).map(k=>{
                return [k.charAt(0).toLowerCase() + k.slice(1),this.item[k]]
            }))
            return {
                ...this.item,
                ...o
            }
        },
        isUploadStatus(){
            return this.uploadStatus === 1
        },
        isAiReport() {
            const {isAutoDiagnosis} = this.getItem
            return !!isAutoDiagnosis
        },
        isMonitorTime(){
            const {RecordTime} = this.getItem
            return !!RecordTime
        },
        isDefaultRecordSettingChange(){
            const defaultSetting = {
                recordDefinition: -1,
                recordLimitValue: -1,
                recordLimitType: -1,
                isAutoAnalysis: 1,
            }
            const {recordDefinition, recordLimitValue, recordLimitType, isAutoAnalysis} = this.getItem

            const currentSetting = {
                recordDefinition,
                recordLimitValue,
                recordLimitType,
                isAutoAnalysis
            }
            return !isEqual(currentSetting, defaultSetting)
        },
        getVideoName() {
            return this.getItem.cloudRename||
                this.getItem.videoRename?.replace(/\.[^.]+$/, '') ||
                this.getItem.sliceVideoName?.replace(/\.[^.]+$/, '') ||
                this.getItem.remarksName ||
                this.getItem.anchorName
        },
        anchorPositionVisible() {
            return !!this.getItem.anchorPosition && !!this.anchorEmployeeNames
        },
        anchorPositionName() {
            return this.getItem.anchorPositionName || '直播间'
        },
        anchorEmployeeNames() {
            return this.getItem.anchorEmployeeNames || ''
        },
        versionTypeIsPure() {
            return this.$store.getters.getVersionType === VERSION_TYPE.PURE
        }
    },
    watch: {},
    methods: {
        openToBrowser (url) {
            window.open(url, '_blank')
        },
        isUnf(val){
            return typeof val !== 'undefined'
        },
        handleClick(){
            if(this.isToHomeUrl && this.getItem.platform !== 2){
                this.openToBrowser(this.getItem.homeUrl);
            }
            this.$emit('click',this.getItem)
        }
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
.compereColContainer {
    flex: 1;
    display: flex;
    align-items: center;
    position: relative;
    width: 100%;
}
.avatar-back{
    width: 41px;
    height: 43px;
    left: -1px;
    position: absolute;
}
.compereImg {
    width: 39px;
    height: 39px;
    border-radius: 50%;
}
.uploadStatus{
    width: 40px;
    height: 40px;
    position: absolute;
    // top: 1px;
}


.compereInfoContainer {
    margin-left: 12px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}
.compereName {
    font-size: 14px;
    color: #2E3742;
}
.anchorName_del_video{
    color: #909499;
}
.compereStatusContainer {
    display: flex;
    align-items: center;
}
.anchorPositionLine{
    font-size: 12px;
    color: #909499;
    line-height: 18px;
}
.anchorPositionLabel{
    color: #909499;
}
.anchorPositionNames{
    color: #909499;
}

.compereNotPlayStatus {
    font-size: 13px;
    color: #95A1AF;
}

.comperePlayStatus {
    display: flex;
    align-items: center;
}
.compereNotPlayStatus,.comperePlayStatus{
    margin-right: 5px;
}

.comperePlayStatusImg {
    width: 18px;
}

.comperePlayStatusText {
    margin-left: 4px;
    font-weight: 500;
    font-size: 13px;
    color: #FF3270;
}

.compereResourceType {
    border-radius: 8px;
    font-size: 10px;
    color: #FFFFFF;
    // margin-left: 5px;
    display: flex;
    align-items: center;
    line-height: 20px;
}

.compereResourceTypeImg {
    width: 16px;
    margin-inline: 1px;
    height: 16px;
}

.activatedService {

}

</style>

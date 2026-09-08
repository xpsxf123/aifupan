<template>
    <div :class="{ 'flex-jc-sb': !isColumn }">
        <Anchor :src="anchorInfo?.AnchorAvatar" :isCompare="isCompare" :fileType="fileInfo?.fileType"
            :name="getVideoName" :videoInfo="videoInfo" :sortIndex="sortIndex" :anchorInfo="anchorInfo" :syncScene="syncScene">
            <template #anchor-left>
                <slot name="anchor-left"></slot>
            </template>
            <template #anchor-right>
                <slot name="anchor-right"></slot>
                <template v-if="timeInAnchorRight">
                    <div class="analysisAnchorTimeRightColumn pd-l16">
                        <div v-if="hasTimeTop" class="analysisAnchorTimeTop">
                            <slot name="time-top"></slot>
                        </div>
                        <div v-if="videoInfo && videoInfo?.VideoId" class="analysisAnchorTimeRightRow text-color3 font-s14">
                            <div class="br-r1-c1" :class="getItemClass">
                                <div>{{ startTitle }}</div>
                                <div>{{ getStartTime }}</div>
                            </div>
                            <div :class="getItemClass">
                                <div>{{ durationTitle }}</div>
                                <div>{{ getDurationStr }}</div>
                            </div>
                        </div>
                        <div v-else class="analysisAnchorTimeRightRow text-color3 font-s14">
                            <div class="br-r1-c1" :class="getItemClass">
                                <div>上传时间：</div>
                                <div>{{ getUpdateTime }}</div>
                            </div>
                            <div v-if="getDurationStr" :class="getItemClass">
                                <div>{{ durationTitle }}</div>
                                <div>{{ getDurationStr }}</div>
                            </div>
                        </div>
                    </div>
                </template>
            </template>
        </Anchor>
        <template v-if="!timeInAnchorRight">
            <div v-if="videoInfo && videoInfo?.VideoId" class="font-s14 font-w400 text-color1" :class="[{ 'flex-ai-c': !isColumn }, { analysisAnchorTimeWrap: hasTimeTop }]">
                <slot name="anchor-before"></slot>
                <div v-if="hasTimeTop" class="analysisAnchorTimeTop">
                    <slot name="time-top"></slot>
                </div>
                <div v-if="['replaySection','replayShort'].includes(getReplayType) && !isCompare && !isWebOnline && !isOnline && !getType">
                    <div class="flex items-center justify-start">
                        <div class="pd-r12 text-right" style="width: 56px">原视频</div>
                        <div class="br-r1-c1" :class="getItemClass">
                            <div>{{ startTitle }}</div>
                            <div>{{  getOriginalStartTime }}</div>
                        </div>
                        <div class="br-r1-c1" :class="getItemClass">
                            <div>{{ endTitle }}</div>
                            <div>{{ getOriginalEndTime }}</div>
                        </div>
                        <div :class="getItemClass">
                            <div>{{ durationTitle }}</div>
                            <div>{{ getOriginalDurationStr }}</div>
                        </div>
                    </div>
                    <div class="flex items-center justify-start">
                        <div class="pd-r12 text-right" style="width: 56px">切片</div>
                        <div class="br-r1-c1" :class="getItemClass">
                            <div>{{ startTitle }}</div>
                            <div>{{ getStartTime }}</div>
                        </div>
                        <div class="br-r1-c1" :class="getItemClass">
                            <div>{{ endTitle }}</div>
                            <div>{{ getEndTime }}</div>
                        </div>
                        <div :class="getItemClass">
                            <div>{{ durationTitle }}</div>
                            <div>{{ getDurationStr }}</div>
                        </div>
                    </div>
                </div>
                <template v-if="isCompare || getReplayType==='replayAll' || isWebOnline || isOnline || getType">
                    <div class="br-r1-c1" :class="getItemClass" v-if="isCompare">
                        <div class="whitespace-nowrap">行业：</div>
                        <div>{{anchorInfo.tradeInfo?.name}}</div>
                    </div>
                    <div class="br-r1-c1" :class="getItemClass">
                        <div>{{ startTitle }}</div>
                        <div>{{ getStartTime }}</div>
                    </div>
                    <div class="br-r1-c1" :class="getItemClass" v-if="!isCompare">
                        <div>{{ endTitle }}</div>
                        <div>{{ getEndTime }}</div>
                    </div>
                    <div :class="getItemClass">
                        <div>{{ durationTitle }}</div>
                        <div>{{ getDurationStr }}</div>
                    </div>
                </template>
                <slot name="anchor-after"></slot>
            </div>
            <div v-else class="font-s14 font-w400 text-color1" :class="[{ 'flex-ai-c': !isColumn }, { analysisAnchorTimeWrap: hasTimeTop }]">
                <slot name="anchor-before"></slot>
                <div v-if="hasTimeTop" class="analysisAnchorTimeTop">
                    <slot name="time-top"></slot>
                </div>
                <div class="br-r1-c1" :class="getItemClass">
                    <div>上传时间：</div>
                    <div>{{ getUpdateTime }}</div>
                </div>
                <div class="br-r1-c1" :class="getItemClass">
                    <div>分析时间：</div>
                    <div>{{ getAnalysisTime }}</div>
                </div>
                <div v-if="getDurationStr" :class="getItemClass">
                    <div>{{ durationTitle }}</div>
                    <div>{{ getDurationStr }}</div>
                </div>
                <slot name="anchor-after"></slot>
            </div>
        </template>
        <div v-else class="font-s14 font-w400 text-color1" :class="{ 'flex-ai-c': !isColumn }">
            <slot name="anchor-before"></slot>
            <slot name="time-right"></slot>
            <slot name="anchor-after"></slot>
        </div>
    </div>
</template>

<script>
import Anchor from './anchor.vue';
import myUtils from '@/utils/utils.js';
export default {
    name: "",
    components: {
        Anchor
    },
    props: {
        itemClass: {
            type: String,
            default: ""
        },
        isColumn: {
            type: Boolean,
            default: false
        },
        isCompare: {
            type: Boolean,
            default: false
        },
        anchorInfo: {
            type: Object,
            default: () => { return {} }
        },
        startTitle: {
            type: String,
            default: '开始：'
        },
        endTitle: {
            type: String,
            default: '结束：'
        },
        durationTitle: {
            type: [Number, String],
            default: '时长：'
        },
        videoInfo: {
            type: Object,
            default: () => { return {} }
        },
        startTime: {
            type: [Number, String],
            default: ''
        },
        endTime: {
            type: [Number, String],
            default: ''
        },
        updateTime: {
            type: String,
            default: ''
        },
        analysisTime: {
            type: String,
            default: ''
        },
        fileInfo: {
            type: Object,
            default: () => { return {} }
        },
        targetType:{
            type: String,
            default: ''
        },
        syncScene: {
            type: [Number, String],
            default: ''
        },
        sortIndex: {
            type: [Number, String],
            default: ''
        },
        timeInAnchorRight: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        hasTimeTop() {
            return !!this.$slots['time-top']
        },
        getItemClass() {
            return [this.itemClass, 'pd-l4', 'pd-r4', 'flex-jc-c', this.isColumn ? 'item-column' : '']
        },
        isWebOnline(){
            return this.targetType === 'webOnline'
        },
        isOnline(){
            return this.targetType === 'online'
        },
        getDurationStr() {
            let durationStr = "";
            let Duration = this.videoInfo?.durationTime || this.fileInfo?.durationTime
            // 计算时长
            if (Duration) {
                durationStr = myUtils.toformatTimeChinse(Duration * 1000);
            }
            return durationStr || Duration
        },
        getVideoName() {
            let videoName = this.videoInfo?.videoRename
            if (['webOnline', 'online'].includes(this.targetType)) {
                if(this.videoInfo?.cloudRename){
                    videoName = `${this.videoInfo?.cloudRename}.mp4`
                }
            }
            return videoName || this.anchorInfo?.AnchorName || this.fileInfo?.fileName
        },
        getStartTime() {
            return this.startTime || this.videoInfo?.StartTime?.substring(0, 16);
        },
        getEndTime() {
            return this.endTime || this.videoInfo?.EndTime?.substring(0, 16);
        },
        getOriginalDurationStr() {
            let durationStr = "";
            let Duration = this.videoInfo?.parentVideoInfo?.duration
            // 计算时长
            if (Duration) {
                durationStr = myUtils.toformatTimeChinse(Duration * 1000);
            }
            return durationStr || Duration
        },
        getOriginalStartTime() {
            return this.videoInfo?.parentVideoInfo?.startTime?.substring(0, 16);
        },
        getOriginalEndTime() {
            return this.videoInfo?.parentVideoInfo?.endTime?.substring(0, 16);
        },
        getUpdateTime() {
            return this.updateTime || this.fileInfo?.uploadTime?.substring(0, 16)
        },
        getAnalysisTime() {
            return this.analysisTime || this.fileInfo?.analysisTime?.substring(0, 16)
        },
        getReplayType() {
            return myUtils.getReplayType({videoInfo:this.videoInfo})
        },
        getType() {
            const {query} = this.$route
            return query?.type
        },
        hasTimeTop() {
            return !!this.$slots['time-top']
        },
    },
    data() {
        return {
        };
    },
    mounted() {

    },
    created() {
    },
    methods: {

    }

};
</script>

<style scoped>
.analysisAnchorTimeWrap {
    flex-wrap: wrap;
    align-content: center;
}

.analysisAnchorTimeTop {
    flex-basis: 100%;
    width: 100%;
    line-height: 18px;
    margin-bottom: 2px;
}

.analysisAnchorTimeRightColumn {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: flex-start;
}

.analysisAnchorTimeRightRow {
    display: flex;
    align-items: center;
    justify-content: flex-start;
}
</style>

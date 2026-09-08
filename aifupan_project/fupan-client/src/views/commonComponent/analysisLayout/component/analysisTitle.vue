<!--
@description 详情页标题区组件：承载直播间信息、顶部功能 tab 与标题区插槽透传。
注意：
1) AI 监控 tab 入口需要与接口契约保持一致，互动巡检仅在录制复盘场景展示；
2) 当前位置已承接换位后的 tab 区，切换事件继续通过 emit 向外透传，避免改动原业务函数。
-->
<template>
    <div>
        <!-- 视频信息/上传的信息 -->
        <AnalysusAnchorTime
            :class="{'analysis-anchor':!isColumn,'analysis-anchor-time':isColumn}"
            :isColumn="isColumn"
            :isCompare="isCompare"
            :syncScene="syncScene"
            :targetType="targetType"
            :timeInAnchorRight="tabsInContent"
            :anchorInfo="{...anchorInfo,tradeInfo:sentenceMarkData.tradeInfo}"
            :sortIndex="sentenceMarkData?.index"
            :videoInfo="videoInfo"
            :fileInfo="fileInfo">
            <template #anchor-left>
                <slot name="anchor-left"></slot>
            </template>
            <template #anchor-right>
                <slot name="anchor-right"></slot>
            </template>
            <template #anchor-before>
                <slot name="anchor-before"></slot>
            </template>
            <template #anchor-after>
                <slot name="anchor-after"></slot>
            </template>
            <template #time-top>
                <slot name="time-top">
                    <div v-if="anchorSchedulePositionName && anchorScheduleEmployeeNames" class="font-s12 text-colorTheme pd-l4">
                        {{ anchorSchedulePositionName }}：{{ anchorScheduleEmployeeNames }}
                    </div>
                </slot>
            </template>
            <template #time-right>
                <slot name="tabs-right"></slot>
            </template>
            <template #title-after>
                <slot name="title-after"></slot>
            </template>
        </AnalysusAnchorTime>
        <slot name="title-center"></slot>
        <!-- 敏感词/关键词/语速汇总 -->
<!--        <div v-if="isWordsContainer && versionType === VERSION_TYPE.AGENT" :class="isColumn?'words-column':'wordsContainer'">-->
<!--                <div class="wordsContainer-left">-->
<!--                    <div id="sensitive-keyword-dom" :class="{'flex-column':isColumn}" class="flex-jc-sb" style="width: 100%">-->
<!--                        <Sensitive :isColumn="isColumn" :readonly="readonly" :ai="ai" :count="wordsInfo.sensitiveWordsNum" v-model="wordsInfo.markSensitive" @markers="markClick">-->
<!--                        </Sensitive>-->
<!--                        <Keyword v-if="isDevMode" :isColumn="isColumn" :readonly="readonly" :ai="ai" :count="wordsInfo.cruxWordsNum" v-model="wordsInfo.markCrux" @markers="markClick"></Keyword>-->
<!--                        <div class="wordsItemContainer flex-ai-c" v-if="!isColumn&&!isFileText">-->
<!--                            <div class="wordsItemColorContainer flex-ai-c">-->
<!--                                <el-dropdown :hide-on-click="false" trigger="click">-->
<!--                                    <afp-button id="pace-dom" style="border: none;padding-inline: 8px" size="small">-->
<!--                                        <div class="wordsItemText wordsItemBox font-s12 text-color2 cursor-pointer"-->
<!--                                             style="color: var(--color-main)">-->
<!--                                            {{ (isKuaishou || isFileId) ? '语速' : '语速/成交/弹幕/销售' }}<i-->
<!--                                            class="el-icon-arrow-down el-icon--right"></i>-->
<!--                                        </div>-->
<!--                                    </afp-button>-->
<!--                                    <el-dropdown-menu slot="dropdown">-->
<!--                                        <el-dropdown-item v-if="(isVideoId ||isFileId) && !notPace && !readonly">-->
<!--                                            <Pace :charConut="getCharCount" :videoTime="getVideoTime" @click="clickPace"></Pace>-->
<!--                                        </el-dropdown-item>-->
<!--                                        <el-dropdown-item v-if="sentenceMarkData?.totalBarrageNum && !isKuaishou">-->
<!--                                            <bulletScreenCount  @click="bulletScreenHandler"  :countNum="sentenceMarkData?.totalBarrageNum"></bulletScreenCount>-->
<!--                                        </el-dropdown-item>-->
<!--                                        <el-dropdown-item v-if="(showDropdown||showTotalDeal) && sentenceMarkData.purchaseCountEnd && !isKuaishou">-->
<!--                                            <TotalDeal  @click="dealHandler" :sentenceMarkData="sentenceMarkData"></TotalDeal>-->
<!--                                        </el-dropdown-item>-->
<!--                                        <el-dropdown-item v-if="(sentenceMarkData.interactionPercent&&!!sentenceMarkData.totalBarrageNum)  && !isKuaishou">-->
<!--                                            <TotalInteract  @click="interactionRateHandler" :sentenceMarkData="sentenceMarkData"></TotalInteract>-->
<!--                                        </el-dropdown-item>-->
<!--                                        <el-dropdown-item v-if="(showDropdown||showTotalDealRate) && sentenceMarkData.purchaseCountEnd && !isKuaishou">-->
<!--                                            <TotalDealRate  @click="dealRateHandler" :sentenceMarkData="sentenceMarkData"></TotalDealRate>-->
<!--                                        </el-dropdown-item>-->
<!--                                        <el-dropdown-item-->
<!--                                            v-if="sentenceMarkData?.volumeEnd&&sentenceMarkData?.volumeEnd>=0 && !isKuaishou">-->
<!--                                            <TotalSales @click="dealSalesHandler"-->
<!--                                                        :sentenceMarkData="sentenceMarkData"></TotalSales>-->
<!--                                        </el-dropdown-item>-->
<!--                                        <el-dropdown-item-->
<!--                                            v-if="sentenceMarkData?.uvValueEnd&&sentenceMarkData?.uvValueEnd>=0 && !isKuaishou">-->
<!--                                            <TotalUV @click="dealUVHandler"-->
<!--                                                     :sentenceMarkData="sentenceMarkData"></TotalUV>-->
<!--                                        </el-dropdown-item>-->
<!--                                    </el-dropdown-menu>-->
<!--                                </el-dropdown>-->
<!--                            </div>-->
<!--                        </div>-->
<!--                    </div>-->
<!--                </div>-->
<!--                <div>-->
<!--                    <slot name="word-control" v-bind="{ videoInfo, fileInfo }"></slot>-->
<!--                </div>-->
<!--        </div>-->

        <div v-if="enableSwappedTabs && !tabsInContent && ((!ai && !readonly)||(readonly && versionType === VERSION_TYPE.PURE))" class="analysis-title-tab-row">
            <el-radio-group class="toolbar-left-tabs analysis-title-tabs" v-model="activeNameProxy" @input="tabsClick" size="medium" @change="handleChange">
                <el-radio-button label="text">{{isRecording?'分钟段落':'话术分析'}}</el-radio-button>
                
                <el-radio-button label="productData" v-if="isRecording && isDouyin && (isWebOnline || isSelfAccount)">商品数据</el-radio-button>

                <el-radio-button label="aiSharding" v-if="getReplayType !== 'replayShort'" style="position: relative">
                    <span>AI脚本拆解</span>
                    <img class="xi-icon" v-if="[1,2].includes(textTypeConfig?.aiShardingStatus)" src="@/assets/imgs/2_5_8/xi.png" alt="">
                    <img class="xi-icon" v-else src="@/assets/imgs/2_5_8/jian.png" alt="">
                </el-radio-button>

                <el-radio-button label="aiOptimal"  v-if="getReplayType !== 'replayShort'">{{ versionType === VERSION_TYPE.PURE?'AI仿写本场':'优化原文' }}</el-radio-button>

                <el-radio-button label="scriptQuality" v-if="getReplayType !== 'replayShort' && (isRecording || isFileAnalysis) && !isSameIndustryRoom && canShowScriptMonitorTabs"
                                 style="position: relative">
                    <span>话术质检</span>
                    <div class="lv-dian" v-if="textTypeConfig?.scriptQualityUnread"></div>
                </el-radio-button>

                <el-radio-button label="interactionInspection" v-if="getReplayType !== 'replayShort' && isRecording && !isSameIndustryRoom && canShowScriptMonitorTabs"
                                 style="position: relative">
                    <span>互动巡检</span>
                    <div class="lv-dian" v-if="textTypeConfig?.interactionInspectionUnread"></div>
                </el-radio-button>

                <el-radio-button label="scriptRestoration" v-if="getReplayType !== 'replayShort' && isRecording && !isSameIndustryRoom && canShowScriptMonitorTabs"
                                 style="position: relative">
                    <span>话术还原度</span>
                    <div class="lv-dian" v-if="textTypeConfig?.scriptRestorationUnread"></div>
                </el-radio-button>
            </el-radio-group>
            <div class="analysis-title-tab-right">
                <slot name="tabs-right"></slot>
            </div>
        </div>

        <div v-if="!enableSwappedTabs && isWordsContainer && versionType === VERSION_TYPE.AGENT" :class="isColumn?'words-column':'wordsContainer'">
            <div class="wordsContainer-left">
                <div id="sensitive-keyword-dom" :class="{'flex-column':isColumn}" class="flex-jc-sb" style="width: 100%">
                    <Sensitive :isColumn="isColumn" :readonly="readonly" :ai="ai" :count="wordsInfo.sensitiveWordsNum" v-model="wordsInfo.markSensitive" @markers="markClick">
                    </Sensitive>
                    <Keyword v-if="isDevMode" :isColumn="isColumn" :readonly="readonly" :ai="ai" :count="wordsInfo.cruxWordsNum" v-model="wordsInfo.markCrux" @markers="markClick"></Keyword>
                    <div class="wordsItemContainer flex-ai-c" v-if="!isColumn&&!isFileText">
                        <div class="wordsItemColorContainer flex-ai-c">
                            <el-dropdown :hide-on-click="false" trigger="click">
                                <afp-button id="pace-dom" style="border: none;padding-inline: 8px" size="small">
                                    <div class="wordsItemText wordsItemBox font-s12 text-color2 cursor-pointer"
                                         style="color: var(--color-main)">
                                        {{ indicatorDropdownLabel }}<i
                                        class="el-icon-arrow-down el-icon--right"></i>
                                    </div>
                                </afp-button>
                                <el-dropdown-menu slot="dropdown">
                                    <el-dropdown-item v-if="(isVideoId ||isFileId) && !notPace && !readonly">
                                        <Pace :charConut="getCharCount" :videoTime="getVideoTime" @click="clickPace"></Pace>
                                    </el-dropdown-item>
                                    <el-dropdown-item v-if="sentenceMarkData?.totalBarrageNum && !isKuaishou">
                                        <bulletScreenCount  @click="bulletScreenHandler"  :countNum="sentenceMarkData?.totalBarrageNum"></bulletScreenCount>
                                    </el-dropdown-item>
                                    <el-dropdown-item v-if="(showDropdown||showTotalDeal) && sentenceMarkData.purchaseCountEnd && !isKuaishou">
                                        <TotalDeal  @click="dealHandler" :sentenceMarkData="sentenceMarkData"></TotalDeal>
                                    </el-dropdown-item>
                                    <el-dropdown-item v-if="(sentenceMarkData.interactionPercent&&!!sentenceMarkData.totalBarrageNum)  && !isKuaishou">
                                        <TotalInteract  @click="interactionRateHandler" :sentenceMarkData="sentenceMarkData"></TotalInteract>
                                    </el-dropdown-item>
                                    <el-dropdown-item v-if="(showDropdown||showTotalDealRate) && sentenceMarkData.purchaseCountEnd && !isKuaishou">
                                        <TotalDealRate  @click="dealRateHandler" :sentenceMarkData="sentenceMarkData"></TotalDealRate>
                                    </el-dropdown-item>
                                    <el-dropdown-item
                                        v-if="sentenceMarkData?.volumeEnd&&sentenceMarkData?.volumeEnd>=0 && !isKuaishou">
                                        <TotalSales @click="dealSalesHandler"
                                                    :sentenceMarkData="sentenceMarkData"></TotalSales>
                                    </el-dropdown-item>
                                    <el-dropdown-item
                                        v-if="sentenceMarkData?.uvValueEnd&&sentenceMarkData?.uvValueEnd>=0 && !isKuaishou">
                                        <TotalUV @click="dealUVHandler"
                                                 :sentenceMarkData="sentenceMarkData"></TotalUV>
                                    </el-dropdown-item>
                                    <el-dropdown-item
                                        v-if="showTotalQianchuanCost && !isKuaishou">
                                        <TotalQianchuanCost
                                            @click="qianchuanCostHandler"
                                            :sentenceMarkData="sentenceMarkData"></TotalQianchuanCost>
                                    </el-dropdown-item>
                                </el-dropdown-menu>
                            </el-dropdown>
                        </div>
                    </div>
                </div>
            </div>
            <div>
                <slot name="word-control" v-bind="{ videoInfo, fileInfo }"></slot>
            </div>
        </div>
    </div>
</template>

<script>
/**
 * @description 详情页标题区脚本：负责顶部 tab 切换、标题区插槽透传与基础展示逻辑。
 * 注意：tab 点击仍然通过事件透传给父层处理，避免在本组件内改动外部状态流转。
 */
import AnalysusAnchorTime from '/src/components/analysis/analysusAnchorTime.vue';
import Sensitive from '/src/components/analysis/sensitive.vue'
import Keyword from '/src/components/analysis/keyword.vue'
import Pace from '/src/components/analysis/pace.vue';
import bulletScreenCount from '/src/components/analysis/bulletScreenCount.vue';
import TotalDeal from '/src/components/analysis/totalDeal.vue';
import TotalInteract from '/src/components/analysis/totalInteract.vue';
import TotalDealRate from '/src/components/analysis/totalDealRate.vue';
import TotalSales from "@/components/analysis/totalSales.vue";
import TotalUV from "@/components/analysis/totalUV.vue";
import TotalQianchuanCost from "@/components/analysis/totalQianchuanCost.vue";
import commonMixin from '../mixin/commonMixin';
import myUtils from "@/utils/utils";
import {VERSION_TYPE} from "@/enum";
import upgradeToAgentTip from '@/mixins/upgradeToAgentTip'
import liveRoomScheduleMixin from '@/mixins/liveRoomScheduleMixin'
export default {
    mixins: [commonMixin, upgradeToAgentTip, liveRoomScheduleMixin],
    components: {
        AnalysusAnchorTime,
        Sensitive,
        Keyword,
        Pace,
        bulletScreenCount,
        TotalInteract,
        TotalDeal,
        TotalDealRate,
        TotalSales,
        TotalUV,
        TotalQianchuanCost
    },
    inject: ['APP'],
    props:{
        wordsInfo:{
            type:Object,
            default:()=>{
                return {}
            }
        },
        sentenceMarkData: {
            type:Object,
            default: ()=>{
                return {}
            }
        },
        charCount: {
            type:[String,Number],
            default: ''
        },
        videoTime: {
            type:[String,Number],
            default: ''
        },
        notPace: {
            type: Boolean,
            default: false
        },
        isColumn: {
            type: Boolean,
            default: false
        },
        // 是否显示敏感词/关键词/语速汇总
        notWords: {
            type: Boolean,
            default: false
        },
        ai: {
            type: Boolean,
            default: false
        },
        targetType:{
            type: String,
            default: ''
        },
        syncScene: {
            type: [String, Number],
            default: ''
        },
        enableSwappedTabs: {
            type: Boolean,
            default: false
        },
        tabsInContent: {
            type: Boolean,
            default: false
        },
        textTypeConfig:{
            type: Object,
            default: () => {
                return {}
            }
        },
        activeName: {
            type: String,
            default: 'text'
        }
    },
    data() {
        return {
            VERSION_TYPE,
            charConutNumber: 0,
            videoTimeNumber: 0
        };
    },
    computed: {
        activeNameProxy: {
            get() {
                return this.activeName
            },
            set(val) {
                this.$emit('update:activeName', val)
            }
        },
        isRecording() {
            return this.isVideoId || !!this.videoInfo?.videoId
        },
        getDuration(){
            return this.sentenceMarkData?.uploadFile?.durationTime || parseInt(this.sentenceMarkData?.videoInfo?.durationTime)
        },
        getCharCount(){
            return this.charConut || this.charConutNumber
        },
        getVideoTime(){
            return this.videoTime || this.videoTimeNumber || this.getDuration
        },
        isWordsContainer(){
            return !this.notWords
        },
        isDevMode(){
            return this.$store.getters.getMode
        },
        showTotalDeal() {
            const {purchaseCountStart, purchaseCountEnd} = this.sentenceMarkData
            return myUtils.isGreaterThanZero(purchaseCountStart) || myUtils.isGreaterThanZero(purchaseCountEnd)
        },
        showDropdown() {
            if (this.targetType === 'webOnline' || this.targetType === 'online') {
                return !!this.showTotalDeal
            } else {
                return this.sentenceMarkData.anchorInfo?.AccountType === 0
            }
        },
        isBuyInData() {
            return this.sentenceMarkData.anchorInfo?.AccountType === 0 && this.sentenceMarkData?.dataSourceType === 1
        },
        showTotalDealRate() {
            const {totalWatchNum} = this.sentenceMarkData
            return this.showTotalDeal && myUtils.isGreaterThanZero(totalWatchNum)
        },
        showTotalQianchuanCost() {
            if (!this.$store.getters.largeEnterprises) return false
            return this.hasMetricValue(this.sentenceMarkData?.totalQianchuanCost)
        },
        indicatorDropdownLabel() {
            if (this.isKuaishou || this.isFileId) {
                return '语速'
            }
            return `语速/成交/弹幕/销售`
        },
        versionType(){
            return this.$store.getters.getVersionType
        },
        /**
         * @description 监控报告 tab 的统一展示条件。
         * 浏览器云空间详情页不会返回客户端版本类型，因此 webOnline/online 场景按 Agent 能力放行。
         * 纯净版账号仍保持隐藏，避免越权展示入口。
         * @returns {boolean}
         */
        canShowScriptMonitorTabs() {
            return (this.versionType === VERSION_TYPE.AGENT || this.isWebOnline || this.isOnline) && !this.$store.getters.isPure
        },
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
        isSameIndustryRoom() {
            const accountType = Number(
                this.sentenceMarkData?.basicSettingsVo?.accountType ??
                this.sentenceMarkData?.videoInfo?.basicSettingsVo?.accountType ??
                this.sentenceMarkData?.videoInfo?.accountType ??
                this.sentenceMarkData?.anchorInfo?.accountType ??
                this.sentenceMarkData?.anchorInfo?.AccountType ??
                0
            )
            return accountType === 1
        },
        isFileAnalysis() {
            return !!this.sentenceMarkData?.fileInfo?.fileId
        }
    },
    watch: {
    },
    methods: {
        handleChange(val) {
            if (this.isPureRecordingVersion()) {
                this.showUpgradeToAgentConfirm(this.getUpgradeToAgentDesc(val))
                this.activeNameProxy = 'text'
            }
        },
        tabsClick(val) {
            const ua = (typeof navigator !== 'undefined' && navigator.userAgent) ? navigator.userAgent : ''
            const isAiFuPan = ua.indexOf('aifupan') >= 0
            if (!isAiFuPan || this.versionType===VERSION_TYPE.AGENT){
                this.$emit('tabsClick', val)
            }
        },
        clickPace(val){
            this.$emit('pace-click',val)
        },
        markClick(){
            this.$emit('markClick',this.wordsInfo)
        },
        // 设置总字数
        setCharAndTime(time,char){
            this.charConutNumber = char;
            this.videoTimeNumber = time;
        },
        bulletScreenHandler(val){
            this.$emit('bulletscreen-click',val)
        },
        dealHandler(val){
            this.$emit('deal-click',val)
        },
        interactionRateHandler(val){
            this.$emit('interactionRate-click',val)
        },
        dealRateHandler(val){
            this.$emit('dealRate-click',val)
        },
        dealSalesHandler(val){
            this.$emit('sales-click',val)
        },
        dealUVHandler(val){
            this.$emit('uv-click',val)
        },
        qianchuanCostHandler(val){
            this.$emit('qianchuanCost-click',val)
        },
        hasMetricValue(value) {
            return value !== undefined && value !== null && value !== ''
        },
    },
    created() {

    },

}
</script>
<style lang='scss' scoped>
.wordsContainer {
    height: 32px;
    //background: #EAEEFC;
    border-radius: 10px;
    margin-top: 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: 12px;
    /* padding: 0 30px; */
}
.wordsContainer-left{
    display: flex;
    justify-content: space-between;
}
.words-column{
    .wordsItemContainer{
        padding: 0;
        margin: 3px 0;
    }
}
.analysis-anchor{
    height: 64px;
    padding-inline: 12px;
    background: #FFFFFF;
    box-shadow: 0px 5px 10px 0px rgba(153,169,216,0.1);
    border-radius: 10px;
}
.analysis-anchor-time{
    ::v-deep(.item-column){
        margin: 3px 0;
        padding: 0;
        justify-content: start;
        font-size: 12px;
        //&::before{
        //    content: ' ';
        //    display: block;
        //    width: 8px;
        //    height: 8px;
        //    border-radius: 50%;
        //    margin: auto 0;
        //    margin-right: 6px;
        //}
        //&:nth-last-child(1)::before{
        //    background: #E41414;
        //}
        //&:nth-last-child(2)::before{
        //    background: #29AD0F;
        //}
        //&:nth-last-child(3)::before{
        //    background: #CD7F11;
        //}
    }

}

.analysis-title-tabs {
    margin-top: 0;
    .el-radio-button {
        &:first-child {
            ::v-deep(.el-radio-button__inner) {
                border-radius: 30px 0 0 30px;
            }
        }

        &:last-child {
            ::v-deep(.el-radio-button__inner) {
                border-radius: 0 30px 30px 0;
            }
        }

        ::v-deep(.el-radio-button__inner) {
            padding: 0 12px;
        }
    }
    .xi-icon{
        position: absolute;
        right: 0;
        top: -15px;
        width: 22px;
        height: 20px;
    }
    .lv-dian{
        position: absolute;
        right: 2px;
        top: 2px;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #13CC63;
    }
}

.analysis-title-tab-row {   
    padding-left: 19%;
    margin-top: 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
}

.analysis-title-tab-right {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex: 1;
    margin-left: 16px;
}
</style>

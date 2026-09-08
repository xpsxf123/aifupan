<!--
@description 详情页搜索与 tab 工具栏：负责顶部 tab、搜索、复制原文及右侧扩展操作区域展示。
注意：
1) 互动巡检入口仅在录制复盘场景显示，不在视频分析/文案预审场景放开；
2) tab 切换继续沿用 emit 方式向外抛出，减少对原有函数的侵入。
-->
<template>
    <div class="discernSearchContainer" :class="{ 'flex-jc-sb': !notes, 'not-ai-tabs': !ai && !notes, 'pd-b10': readonly }">
        <slot name="toolbar-left"></slot>
        <div v-if="!this.isCompare"  :class="{'w100': notes, 'not-ai-tabs': !ai && notes, 'mg-b10': notes}" class="toolbar-left">
<!--            <Title v-if="(readonly || !isRecording)&&versionType === VERSION_TYPE.AGENT">话术分析</Title>-->
<!--            <el-tabs v-if="!ai && !readonly && isRecording" class="toolbar-left-tabs" v-model="activeName" @tab-click="tabsClick">-->
<!--                <el-tab-pane label="分钟段落" name="text"></el-tab-pane>-->
<!--                <el-tab-pane label="AI脚本拆解" name="aiSharding" v-if="getReplayType !== 'replayShort'"></el-tab-pane>-->
<!--                <el-tab-pane label="优化原文" name="aiOptimal" v-if="getReplayType !== 'replayShort'"></el-tab-pane>-->
<!--            </el-tabs>-->
<!--            <el-radio-group v-if="(!ai && !readonly)||(readonly && versionType === VERSION_TYPE.PURE)" class="toolbar-left-tabs" v-model="activeName" @input="tabsClick" size="medium" @change="handleChange">-->
<!--                <el-radio-button label="text">{{isRecording?'分钟段落':'话术分析'}}</el-radio-button>-->
<!---->
<!--                <el-radio-button label="aiSharding" v-if="getReplayType !== 'replayShort'" style="position: relative">-->
<!--                    <span>AI脚本拆解</span>-->
<!--                    <img class="xi-icon" v-if="[1,2].includes(textTypeConfig?.aiShardingStatus)" src="@/assets/imgs/2_5_8/xi.png" alt="">-->
<!--                    <img class="xi-icon" v-else src="@/assets/imgs/2_5_8/jian.png" alt="">-->
<!--                </el-radio-button>-->
<!---->
<!--                <el-radio-button label="aiDiagnose" v-if="getReplayType !== 'replayShort'&&isRecording"-->
<!--                                 style="position: relative">-->
<!--                    <span>AI数据诊断</span>-->
<!--                    <img class="xi-icon" v-if="[2].includes(textTypeConfig?.aiDiagnoseStatus)"-->
<!--                         src="@/assets/imgs/2_5_8/xi.png" alt="">-->
<!--                    <img class="xi-icon" v-else src="@/assets/imgs/2_5_8/jian.png" alt="">-->
<!--                </el-radio-button>-->
<!---->
<!--                <el-radio-button label="scriptQuality" v-if="getReplayType !== 'replayShort' && (isRecording || isFileAnalysis) && !isSameIndustryRoom"-->
<!--                                 style="position: relative">-->
<!--                    <span>话术质检</span>-->
<!--                    <div class="lv-dian" v-if="textTypeConfig?.scriptQualityUnread"></div>-->
<!--                </el-radio-button>-->
<!--                <el-radio-button label="aiOptimal"  v-if="getReplayType !== 'replayShort'">{{ versionType === VERSION_TYPE.PURE?'AI仿写本场':'优化原文' }}</el-radio-button>-->
<!--            </el-radio-group>-->

            <el-radio-group v-if="!notes && !enableSwappedTabs && ((!ai && !readonly)||(readonly && versionType === VERSION_TYPE.PURE))" class="toolbar-left-tabs" v-model="activeName" @input="tabsClick" size="medium" @change="handleChange">
                <el-radio-button label="text">{{isRecording?'分钟段落':'话术分析'}}</el-radio-button>
                <el-radio-button label="productData" v-if="isRecording && isDouyin && (targetType === 'webOnline' || isSelfAccount)">商品数据</el-radio-button>

                <el-radio-button label="aiSharding" v-if="getReplayType !== 'replayShort'" style="position: relative">
                    <span>AI脚本拆解</span>
                    <img class="xi-icon" v-if="[1,2].includes(textTypeConfig?.aiShardingStatus)" src="@/assets/imgs/2_5_8/xi.png" alt="">
                    <img class="xi-icon" v-else src="@/assets/imgs/2_5_8/jian.png" alt="">
                </el-radio-button>

                <el-radio-button label="aiOptimal"  v-if="getReplayType !== 'replayShort'">{{ versionType === VERSION_TYPE.PURE?'AI仿写本场':'优化原文' }}</el-radio-button>

                <el-radio-button label="scriptQuality" v-if="getReplayType !== 'replayShort' && (isRecording || isFileAnalysis) && !isSameIndustryRoom && versionType === VERSION_TYPE.AGENT && !$store.getters.isPure"
                                 style="position: relative">
                    <span>话术质检</span>
                    <div class="lv-dian" v-if="textTypeConfig?.scriptQualityUnread"></div>
                </el-radio-button>

                <el-radio-button label="interactionInspection" v-if="getReplayType !== 'replayShort' && isRecording && !isSameIndustryRoom"
                                 style="position: relative">
                    <span>互动巡检</span>
                    <div class="lv-dian" v-if="textTypeConfig?.interactionInspectionUnread"></div>
                </el-radio-button>
            </el-radio-group>

            <div v-if="enableSwappedTabs && showWordsContainer" :class="isColumn?'words-column':'wordsContainer'">
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
                                        <el-dropdown-item v-if="textData?.totalBarrageNum && !isKuaishou">
                                            <bulletScreenCount  @click="bulletScreenHandler"  :countNum="textData?.totalBarrageNum"></bulletScreenCount>
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="(showDropdown||showTotalDeal) && textData.purchaseCountEnd && !isKuaishou">
                                            <TotalDeal  @click="dealHandler" :sentenceMarkData="textData"></TotalDeal>
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="(textData.interactionPercent&&!!textData.totalBarrageNum)  && !isKuaishou">
                                            <TotalInteract  @click="interactionRateHandler" :sentenceMarkData="textData"></TotalInteract>
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="(showDropdown||showTotalDealRate) && textData.purchaseCountEnd && !isKuaishou">
                                            <TotalDealRate  @click="dealRateHandler" :sentenceMarkData="textData"></TotalDealRate>
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="textData?.volumeEnd&&textData?.volumeEnd>=0 && !isKuaishou">
                                            <TotalSales @click="dealSalesHandler"
                                                        :sentenceMarkData="textData"></TotalSales>
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="textData?.uvValueEnd&&textData?.uvValueEnd>=0 && !isKuaishou">
                                            <TotalUV @click="dealUVHandler"
                                                     :sentenceMarkData="textData"></TotalUV>
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="showTotalQianchuanCost && !isKuaishou">
                                            <TotalQianchuanCost
                                                @click="qianchuanCostHandler"
                                                :sentenceMarkData="textData"></TotalQianchuanCost>
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </el-dropdown>
                            </div>
                        </div>
                    </div>
                </div>
                <div>
                    <slot name="word-control"></slot>
                </div>
            </div>
        </div>
        <div :class="{'w100': notes}" class="toolbar-rigth">
            <slot name="toolbar-right-before"></slot>
<!--            <div class="flex items-center tradeContainer" v-if="!isCompare && isTrade && !notes && versionType === VERSION_TYPE.AGENT">-->
<!--                <span class="text-xs" v-if="isHandCloseStatus??isShowRecommend"-->
<!--                      style="color:var(--color-main)">行业可能不准！</span>-->
<!--                <TradeTreeList ref="trade" v-model="tradeId" :isSelectTrade="isSelectTrade" size="medium"-->
<!--                               :treeList="treeData" @tree-load="treeLoad"-->
<!--                               @tree-list-change="treeChange"></TradeTreeList>-->
<!--                <div class="text-xs tipsContainer" v-if="isHandCloseStatus??isShowRecommend">-->
<!--                    <div>-->
<!--                        AI检测到本直播间行业为<span style="color:var(--color-main);">{{recommendTrade?.tradeName}}</span>更精确，已帮您修改。-->
<!--                    </div>-->
<!--                    <div class="flex items-center justify-around">-->
<!--                        <el-button type="text" class="text-xs" @click="cancelRecommend">改回原行业</el-button>-->
<!--                        <el-button type="text" style="color:red;" class="text-xs" @click="replaceIndustry">-->
<!--                            确认修改-->
<!--                        </el-button>-->
<!--                    </div>-->
<!--                </div>-->
<!--            </div>-->

            <div v-if="isSearch" id="toolbar-search-dom" class="flex-ai-c">
                <!-- 搜索内容标注关键字 -->
                <div class="mg-l10" style="min-width: 140px;">
                    <div style="display: flex; align-items: center;">
                        <span class="font-s14 text-colorMain">数量</span>
                        <div style="display: flex; align-items: center;">
                            <i class="el-icon-caret-left" @click="prevSearchWords"
                                style="font-size: 26px; color: dodgerblue; cursor: pointer;"></i>
                            <div style="font-size: 14px; color: #444;margin: 0 6px;">{{
                                (keyWordCount ? (selectIndex + 1) : 0) + " / " + keyWordCount
                                }}
                            </div>
                            <i class="el-icon-caret-right" @click="nextSearchWords"
                                style="font-size: 26px; color: dodgerblue; cursor: pointer;"></i>
                        </div>
                        <!-- <div v-else style="font-size: 14px; color: #444;margin-right: 10px;">
                            无结果
                        </div> -->
                    </div>
                </div>
                <!-- 非对比分析搜索框在右 -->
                <el-input  placeholder="输入关键字" suffix-icon="el-icon-search" v-model="keyWordText" class="input-gray input-border-none"
                    :style="{ width: isCompare ? '130px' : '200px' }" @input="markKeywords" clearable size="medium">
                </el-input>
            </div>

            <afp-button v-if="!enableSwappedTabs && activeName !=='text'" @click="copyText" style="margin-left: 10px;">复制原文
            </afp-button>
            <!-- 导出文字内容 -->
<!--            <el-button v-if="!isCompare && isExport && !notes" @click="exportTxt" style="margin-left: 10px;" size="mini"-->
<!--                type="primary">导出话术-->
<!--            </el-button>-->

            <slot name="toolbar-right-after"></slot>
        </div>

        <slot name="toolbar-right"></slot>
        <exportDialog ref="exportDialog" :textTypeConfig="textTypeConfig" isWord @exportWord="exportWord" @createText="createText"></exportDialog>
    </div>
</template>

<script>
/**
 * @description 详情页工具栏脚本：承接 tab 切换、搜索关键字、行业选择与导出/复制等行为。
 * 注意：当前文件为多页面复用组件，新增 tab 需同步遵守不同场景下的显示限制。
 */
import TradeTreeList from './tradeTreeList.vue'
import myUtils from '/src/utils/utils';
import Title from './../title/index.vue'
import exportDialog from './exportDialog.vue';
import { getSourceData } from '@/utils/common'
import {trackEvent} from "@/utils/laTrack";
import {isEmpty} from "lodash";
import {VERSION_TYPE} from "@/enum";
import { PLATFORM_TYPE_ENUM } from '@/enum';
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
import upgradeToAgentTip from '@/mixins/upgradeToAgentTip'
export default {
    name: "",
    mixins: [upgradeToAgentTip],
    components: {
        TradeTreeList,
        Title,
        exportDialog,
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
    props: {
        // 是否是对比分析。对比分析采用不同的样式
        isCompare: {
            type: Boolean,
            default: false
        },
        treeData: {
            type: Array,
            default: () => {
                return []
            }
        },
        playUrl: {
            type: String,
            default: ''
        },
        keyWordCount: {
            type: [String, Number],
            default: 0
        },
        selectIndex: {
            type: Number,
            default: -1
        },
        loading: {
            type: Boolean,
            default: false
        },
        isSelectTrade: {
            type: Boolean,
            default: false
        },
        notTarde: {
            type: Boolean,
            default: false
        },
        notSearch: {
            type: Boolean,
            default: false
        },
        notExport: {
            type: Boolean,
            default: false
        },
        ai: {
            type: Boolean,
            default: false
        },
        textData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        notes: {
            type: Boolean,
            default: false
        },
        readonly: {
            type: Boolean,
            default: false
        },
        textTypeConfig:{
            type: Object,
            default: () => {
                return {}
            }
        },
        wordsInfo:{
            type:Object,
            default:()=>{
                return {}
            }
        },
        targetType:{
            type: String,
            default: ''
        },
        enableSwappedTabs: {
            type: Boolean,
            default: false
        },
        notPace: {
            type: Boolean,
            default: false
        },
        isVideo:{
            type: Boolean,
            default: false
        },
        isRecording:{
            type: Boolean,
            default: false
        }
    },

    computed: {
        isVideoId() {
            const videoInfo = this.textData?.videoInfo || {}
            return !!videoInfo?.VideoId
        },
        isFileId(){
            const fileInfo = this.textData?.fileInfo || {}
            return !!fileInfo?.fileId && fileInfo?.fileType !== 2;
        },
        isFileText(){
            const fileInfo = this.textData?.fileInfo || {}
            return fileInfo?.fileType === 2;
        },
        getPlatform(){
            const videoInfo = this.textData?.videoInfo || {}
            const fileInfo = this.textData?.fileInfo || {}
            return videoInfo?.PlatformType || fileInfo?.platformType
        },
        isKuaishou() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.kuaishou
        },
        isShipinhao() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.shipinhao
        },
        isDouyin() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.douyin
        },
        /**
         * @description 是否为自有账号（AccountType === 0 表示自有账号）
         * 兼容云空间场景：云空间API返回的AccountType在basicSettingsVo中，而非anchorInfo中
         */
        isSelfAccount() {
            const accountType = Number(
                this.textData?.basicSettingsVo?.accountType ??
                this.textData?.videoInfo?.basicSettingsVo?.accountType ??
                this.textData?.videoInfo?.accountType ??
                this.textData?.anchorInfo?.accountType ??
                this.textData?.anchorInfo?.AccountType ??
                0
            )
            return accountType === 0
        },
        getDuration(){
            return this.textData?.uploadFile?.durationTime || parseInt(this.textData?.videoInfo?.durationTime)
        },
        getCharCount(){
            return this.charCountNumber
        },
        getVideoTime(){
            return this.videoTimeNumber || this.getDuration
        },
        isDevMode(){
            return this.$store.getters.getMode
        },
        showTotalDeal() {
            const {purchaseCountStart, purchaseCountEnd} = this.textData || {}
            return myUtils.isGreaterThanZero(purchaseCountStart) || myUtils.isGreaterThanZero(purchaseCountEnd)
        },
        showDropdown() {
            if (this.targetType === 'webOnline' || this.targetType === 'online') {
                return !!this.showTotalDeal
            } else {
                return this.textData?.anchorInfo?.AccountType === 0
            }
        },
        showTotalDealRate() {
            const {totalWatchNum} = this.textData || {}
            return this.showTotalDeal && myUtils.isGreaterThanZero(totalWatchNum)
        },
        showTotalQianchuanCost() {
            if (!this.$store.getters.largeEnterprises) return false
            return this.hasMetricValue(this.textData?.totalQianchuanCost)
        },
        indicatorDropdownLabel() {
            if (this.isKuaishou || this.isFileId) {
                return '语速'
            }
            return `语速/成交/弹幕/销售`
        },
        canUseAgentDisplay(){
            return this.versionType === VERSION_TYPE.AGENT || this.targetType === 'webOnline' || this.targetType === 'online'
        },
        isWordsContainer(){
            return this.enableSwappedTabs && this.activeName === 'text' && this.canUseAgentDisplay
        },
        isSameIndustryRoom() {
            const accountType = Number(
                this.textData?.basicSettingsVo?.accountType ??
                this.textData?.videoInfo?.basicSettingsVo?.accountType ??
                this.textData?.videoInfo?.accountType ??
                this.textData?.anchorInfo?.accountType ??
                this.textData?.anchorInfo?.AccountType ??
                0
            )
            return accountType === 1
        },
        isFileAnalysis() {
            return !!this.textData?.fileInfo?.fileId
        },
        isTrade() {
            return !this.notTarde && !this.readonly && (this.enableSwappedTabs || this.activeName === 'text')
        },
        isSearch(){
            return !this.notSearch && this.activeName === 'text'
        },
        isExport() {
            return !this.notExport
        },
        isShowRecommend() {
            if (this.tradeId && this.recommendTrade?.tradeId) {
                return this.textData?.suggestTrade != 1 && this.recommendTrade?.tradeId !== this.tradeId
            }
            return false
        },
        getActionKeyValue(){
            return this.$store.state?.actionKey;
        },
        getReplayType() {
            return myUtils.getReplayType(this.textData)
        },
        versionType(){
            return this.$store.getters.getVersionType
        },
        showWordsContainer(){
            return this.isWordsContainer
        }
    },
    data() {
        return {
            VERSION_TYPE,
            PLATFORM_TYPE_ENUM,
            tradeId: '',
            searchWordIndexInfo: {},
            debounce: null,
            keyWordText: '',
            activeName: 'text',
            recommendTrade: {},
            isHandCloseStatus: undefined,
            charCountNumber: 0,
            videoTimeNumber: 0,
            isColumn: false
        };
    },
    mounted() {
        this.$watch(() => [this.tradeId, this.recommendTrade, this.textData?.suggestTrade], (newVal) => {
                if (newVal[0] === newVal[1]?.tradeId && newVal[2] != 1) {
                    this.changeSuggestTradeStatus()
                }
            },
            {deep: true}
        )
    },
    watch:{
        textData: {
            handler(val) {
                if (this.isSameIndustryRoom && this.activeName === 'scriptQuality') {
                    this.activeName = 'text'
                    this.tabsClick()
                }
                const userId= this.textData?.videoInfo?.UserId
                const {id: currentUserId} = this.$store?.state?.userInfo;
                if (!isEmpty(val) && userId === currentUserId && this.isSelectTrade&&!this.ai&&!this.isCompare) this.getAiRecommendTrade()
            },
            immediate: true,
            deep: true
        },
        getActionKeyValue: {
            handler(val) {
                this.exportTxt()
            },
        }
    },
    created() {
        // 节流储存
        this.debounce = myUtils.debounce(1000, this.keyWords)
    },
    methods: {
        setCharAndTime(time,char){
            this.charCountNumber = char;
            this.videoTimeNumber = time;
        },
        clickPace(val){
            this.$emit('pace-click',val)
        },
        markClick(){
            this.$emit('markClick',this.wordsInfo)
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
        toActiveText(type){
            this.activeName = type || 'text';
            this.tabsClick();
        },
        createText(val){
            this.$emit('createText',val)
        },
        setTradeId(id) {
            this.tradeId = id || '1';
        },
        dropDown() {
            this.$refs?.trade?.dropDown()
        },
        exportWord(data){
            this.$emit('exportWord',data)
        },
        copyText(){
            if(this.activeName === 'aiOptimal' && this.textTypeConfig?.aiOptimalStatus !== 2){
                this.$message.error('请先获取文本，才可复制')
                return
            }else if(this.activeName === 'aiSharding' && this.textTypeConfig?.aiShardingStatus !== 2){
                this.$message.error('请先获取文本，才可复制')
                return
            }
            this.$emit('copyText', this.activeName)
        },
        exportTxt() {
            const { videoInfo, uploadFile } = this.textData;
            const fileName = videoInfo?.VideoName || uploadFile?.fileName;
            const fileSize = videoInfo?.VedioSizie || uploadFile?.fileSize;
            const {sourceId, sourceType} = getSourceData({videoInfo, uploadFile});
            this.$refs.exportDialog.show({
                data: {
                    fileName,
                    fileSize,
                    readonly: this.readonly,
                    sourceId,
                    sourceType,
                    isRecording: this.isRecording
                }
            });
        },
        keyWords(text) {
            this.$emit('keyWord', text)
        },
        // 搜索关键字
        markKeywords(text) {
            this.$emit('update:loading', false)
            this.debounce(text);
        },
        prevSearchWords() {
            this.$emit('prev', this.keyWordText)
        },
        nextSearchWords() {
            this.$emit('next', this.keyWordText)
        },
        treeChange(id) {
            this.$emit('treeChange', id)
        },
        treeLoad(tree) {
            this.$emit('treeLoad', tree)
        },
        handleChange(val) {
            if (this.isPureRecordingVersion()) {
                this.showUpgradeToAgentConfirm(this.getUpgradeToAgentDesc(val))
                this.activeName = 'text'
            }
        },
        tabsClick() {
            const ua = (typeof navigator !== 'undefined' && navigator.userAgent) ? navigator.userAgent : ''
            const isAiFuPan = ua.indexOf('aifupan') >= 0
            if(!isAiFuPan || this.versionType===VERSION_TYPE.AGENT){
                this.keyWordText = '';
                this.markKeywords('');
                this.$emit('tabsClick', this.activeName)
            }
            if (this.activeName === 'aiSharding') {
                trackEvent('P001_A0055')
            } else if (this.activeName === 'aiOptimal') {
                trackEvent('P001_A0056')
            }
        },
        async getAiRecommendTrade() {
            if(this.textData?.suggestTrade) return
            const {VideoId = ''} = this.textData?.videoInfo || {}
            const {fileId = ''} = this.textData?.uploadFile || {}
            const result = await this.$httpClient.compere.getAiRecommendTrade({
                sourceType: VideoId ? 0 : 1,
                sourceId: VideoId || fileId
            })
            if (result.code !== 0) return
            this.recommendTrade = result?.data
        },
        changeSuggestTradeStatus(){
            const {VideoId = ''} = this.textData?.videoInfo || {}
            const {fileId = ''} = this.textData?.uploadFile || {}
            this.$httpBack.trade.updateSuggestTrade({
                sourceType: VideoId ? 0 : 1,
                sourceId: VideoId || fileId
            })
        },
        cancelRecommend() {
            this.changeSuggestTradeStatus()
            this.isHandCloseStatus = false
        },
        changeTrade() {
            let requestData = {
                secUid: this.textData?.anchorInfo?.SecUid,
                tradeId: this.recommendTrade?.tradeId || "1"
            }
            this.$httpClient.compere.updateanchortrade(requestData)
        },
        replaceIndustry() {
            this.$emit('treeChange', this.recommendTrade?.tradeId)
            this.changeTrade()
        }
    }
};
</script>

<style scoped lang="scss">
.discernSearchContainer {
    margin-bottom: 10px;
    margin-top: 10px;

    .tradeContainer {
        position: relative;

        .tipsContainer {
            background: #fff;
            box-shadow: 0px 3px 6px 0px rgba(45, 81, 139, 0.1), 0px 11px 11px 0px rgba(45, 81, 139, 0.09), 0px 25px 15px 0px rgba(45, 81, 139, 0.05), 0px 44px 18px 0px rgba(45, 81, 139, 0.01), 0px 69px 19px 0px rgba(45, 81, 139, 0);
            padding: 12px 12px 0 12px;
            width: 230px;
            border-radius: 6px;
            position: absolute;
            top: 35px;
            left: 15px;
            z-index: 100;
        }
    }
}

.not-ai-tabs {
    //border-bottom: 2px solid #E4E7ED;
}

.toolbar-left {
    display: flex;
    align-items: center;
    //min-width: 0;
}

.toolbar-rigth {
    display: flex;
    align-items: center;
    justify-content: end;
}

.toolbar-left-tabs {
    //max-width: 100%;
    //overflow-x: auto;
    //white-space: nowrap;

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

.wordsContainer {
    height: 32px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: 12px;
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
//.toolbar-left-tabs {
//    ::v-deep(.el-tabs__header) {
//        margin-bottom: 0;
//    }
//
//    ::v-deep(.el-tabs__nav-wrap) {
//        &::after {
//            display: none;
//        }
//    }
//
//    ::v-deep(.el-tabs__active-bar) {
//        height: 2px;
//        background: var(--color-main);
//        border-radius: 29px;
//        margin: 0;
//    }
//
//    .is-scroll {
//        ::v-deep(.el-tabs__content) {
//            height: calc(100vh - 105px);
//            overflow: hidden;
//            overflow-y: auto;
//        }
//    }
//}
</style>

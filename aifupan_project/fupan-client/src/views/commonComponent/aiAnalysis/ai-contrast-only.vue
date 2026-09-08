<template>
    <!-- 页面布局以及功能内聚 -->
    <div  class="contrast-item-box analysis-flex-column">
        <div class="videoAnalysisContainer">
            <el-row :gutter="10" class="h100">
                <el-col v-if="!isFileText" class="h100" :span="getSpan(0)">
                    <div class="h100 w100 brs-8 mg-r12 video-content">
                         <div class="video-box mg-r12 brs-8 overflow_hidden"
                            style="width: 100%;"
                            :style="{height: isDataBoard || isDataCapture ? '100%' : 'calc(70% - 32px)'}">
                            <contrastVideo ref="videoPlayer" notScrolling ai :targetType="targetType" :sentenceMarkData="getSentenceMarkData"
                            @playerTimeupdate="onPlayerTimeupdate"
                            @setDuration="setDuration"
                            @changeParagraphIndex="onPlayerParagraphIndex">
                                <template #video-bottom>
                                    <slot name="video-bottom"></slot>
                                </template>
                            </contrastVideo>
                        </div>
                        <div v-if="isWordDiscern || isScrolling" style="width: 100%;height: 30%;" class="brs-8 mg-t12">
                            <videoSlice
                                ref="videoSlice"
                                :targetType="targetType"
                                :data="sentenceMarkData"
                                :type="type"
                                :dataDisplay="dataDisplay"
                                :otherData="otherData"
                                :readonly="!!shareId"
                                :questionContent="questionContent"
                                :textDataMap="textDataMap"
                                @paragraphData="sliceParagraphData"
                                @curveDataChange="curveDataChange"
                                notAllText
                                notDataCapture
                                notDataBoard
                                paragraphText
                                :maxLength="getMaxLen(model)"
                            ></videoSlice>
                        </div>
                    </div>
                </el-col>
                <el-col class="h100" :span="getSpan(1)">
                    <div v-if="isScrolling" style="width: 100%;overflow: hidden;"  class="h100 pd-8 flex-ai-s flex-column main-bg" >
                        <Title class="pd-b8 flex-jc-sb">
                            <span>公屏弹幕</span>
                            <div v-if="!shareId" class="flex-ji-c">
                                <showOther v-model="dataDisplay"></showOther>
                                <el-radio-group v-model="scrollingType" :disabled="tabLaoding" size="small" @change="scrollingChange">
                                    <el-radio-button label="all">所有弹幕</el-radio-button>
                                    <el-radio-button label="core">重要弹幕</el-radio-button>
                                </el-radio-group>
                            </div>
                        </Title>
                            <scrolling-list ref="scrolling"
                            :targetType="targetType" 
                            class="h100" notScrollingType ai inline 
                            :startTime="getStartTime" 
                            :endTime="getEndTime"
                            scrollAutoLoad
                            :dataDisplay="dataDisplay"
                            :tabScrollingType="scrollingType"
                            @scrollingDatasParams="scrollingDatasParams"
                            @rightContext="rightContextMenuHanlder"
                            @tabLoading="tabLaodingHandle"
                            :sentenceMarkData="sentenceMarkData" :search="true"></scrolling-list>
                    </div>
                    <div v-else-if="isDataBoard || isDataCapture" @contextmenu.prevent="rightContextMenuHanlder" style="width: 100%;overflow: hidden;" class="h100 flex-ai-s flex-column main-bg" >
                        <div v-if="isDataCapture" class="h100 overflow_hidden overflow_auto_y pd-8">
                            <div v-html="aIHtmlContent"></div>
                        </div>
                        <div v-else-if="isDataBoard" class="text-dashboard-box">
                            <TextDashboard :requestId="getId" :span="24" notBtn></TextDashboard>
                        </div>
                    </div>
                    <div v-else class="h100 main-bg brs-8 overflow_hidden aiAssistantTextBox">
                        <div class="aiAssistantHeader pd-l12 pd-r12 pd-t12">
                            <div class="aiAssistantHeaderTop flex-ai-c flex-jc-sb">
                                <div class="aiAssistantAnchorInfo flex-ai-c">
                                    <img class="aiAssistantAnchorAvatar" :src="headerAnchorAvatar" alt="">
                                    <div class="aiAssistantAnchorName">{{ headerAnchorName }}</div>
                                </div>
                                <div class="aiAssistantDuration">时长：{{ headerDurationText }}</div>
                            </div>
                            <div class="aiAssistantHeaderTime flex-ai-c">
                                <div>{{ headerTimeLeftLabel }}：{{ headerStartTime }}</div>
                                <div class="mg-l20">{{ headerTimeRightLabel }}：{{ headerEndTime }}</div>
                            </div>
                            <div class="aiAssistantHeaderControls flex-ai-c flex-jc-sb">
                                <div v-if="!shareId" class="flex-ji-c layout-box">
                                    <div class="aiAssistantLayoutLabel">布局：</div>
                                    <el-checkbox-group v-model="layoutsValue">
                                        <el-checkbox v-for="item in getLayouts" :label="item.value" :key="item.value">展示{{item.label}}</el-checkbox>
                                    </el-checkbox-group>
                                </div>
                                <div v-if="isVideoId" class="aiAssistantHeaderActions flex-ai-c">
                                    <DiscernShowOther
                                        v-model="headerDataDisplay"
                                        class="aiAssistantHeaderShowOther"
                                        :sentenceMarkData="getSentenceMarkData"
                                        :barrageNum="sentenceMarkData.totalBarrageNum"
                                        :isCompare="isCompare"
                                        :hide="{
                                            onlineNum:isViolation,
                                            natureTime:!isViolation&&!isAssistant
                                        }"
                                        :aiCueType="getAiCueTypeKeyValue"
                                        @change="handleHeaderOtherChange"
                                    />
                                    <el-radio-group
                                        v-model="headerTabType"
                                        class="aiAssistantHeaderTabGroup"
                                        size="medium"
                                        @change="handleAiHeaderTextTypeChange"
                                    >
                                        <el-radio-button label="aiOptimal">优化原文</el-radio-button>
                                        <el-radio-button label="text">分钟段落</el-radio-button>
                                    </el-radio-group>
                                </div>
                            </div>
                        </div>
                        <div class="aiAssistantLocateBar pd-l12 pd-r12 pd-t6">
                            <div class="aiAssistantLocateBarInner brs-8 pd-b16 pd-l10 pd-r10">
                                <div class="aiAssistantSliderBox">
                                    <el-slider
                                        v-model="sliderValue"
                                        :marks="sliderMarks"
                                        :max="videoMaxTime"
                                        :format-tooltip="formatTooltip"
                                        @change="onSliderChange"
                                    />
                                </div>
                            </div>
                        </div>
                        <wordDiscern class="h100 overflow_hidden b-t1-c1" ref="textDom" isAi notLocatingBar notTarde notExport
                            :readonly="!!shareId"
                            :targetType="targetType" name="ai" :sentenceMarkData="getSentenceMarkData"
                            :textSliceIndex="textDataMap"
                            :type="type"
                            hideAiBtn
                            @textTypeChange="textTypeChange" @getTextList="getTextList"
                            @selectedText="updateSelectedText" @rightTickContextMenu="rightTickContextMenu"
                            @onParagraph="onParagraph"  @playerReadied="onPlayerReadied" @playerPause="onPlayerPause"
                            @otherChange="otherChange">
                            <template #toolbar-left>
                                <div class="monitorBarrageSwitch flex-ai-c">
                                    <span class="font-s12 text-colorMain mg-r6">弹幕</span>
                                    <el-switch v-model="showBarrage" active-color="#409EFF"></el-switch>
                                </div>
                            </template>
                            <template #textParagraph-after="{ item, index }">
                                <MonitorParagraphBarrage
                                    v-if="shouldRenderBarrage(index)"
                                    :sentenceMarkData="sentenceMarkData"
                                    :paragraph="item"
                                />
                            </template>
                            <template #tabs="item">
                                <slot name="tabs" v-bind="item"></slot>
                            </template>
                        </wordDiscern>
                    </div>
                </el-col>
                <el-col class="h100" :span="getSpan(2)">
                    <div class="ai-box main-bg brs-8 overflow_hidden pd-6 h100">
                        <ai ref="aiDom" class="h100" :targetType="targetType"
                        :askConfig="optionConfig"
                        :type="type"
                        :shareId="shareId"
                        :sentenceMarkData="getSentenceMarkData"
                        :flod="getFlod"
                        :moreConfigValue="moreConfigState"
                        :moreConfigProps="moreConfigProps"
                        @modelChange="modelChange"
                        @more-config-change="handleAiMoreConfigChange"
                        @toggle="onToggle"></ai>
                        <aiFlod
                            v-if="showLayoutFold"
                            class="fold"
                            :max="getMax"
                            :expandMode="isLayoutExpandMode"
                            @change="onLayoutFoldTrigger"
                        />
                    </div>
                </el-col>
            </el-row>
        </div>
    </div>
</template>

<script>
import wordDiscern from '/src/views/commonComponent/analysisLayout/component/wordDiscern.vue';
import contrastVideo from '/src/views/commonComponent/analysisLayout/contrast-video.vue';
import ai from '/src/components/analysis/ai/index.vue';
import commonMixin from './../analysisLayout/mixin/commonMixin'
import wordsMixin from './../analysisLayout/mixin/wordsMixin';
import publicMixin from './../analysisLayout/mixin/publicMixin';
import aiFlod from './aiFold.vue';
import videoSlice from './videoSlice/index.vue';
import Title from '/src/components/title/index.vue';
import scrollingList from '/src/components/analysis/scrolling/scrollingList.vue';
import aiTypeMixin from '@/mixins/aiTypeMixin.js';
import TextDashboard from '@/components/analysis/dataBoard/textDashboard.vue';
import showOther from '@/components/analysis/scrolling/showOther.vue';
import DiscernShowOther from '@/components/DiscernSearchContainer/showOther.vue';
import contextmenu from '/src/mixins/contextmenu.js';
import MonitorParagraphBarrage from '@/views/modules/replay/monitorDetail/components/MonitorParagraphBarrage.vue';
import myUtils from '@/utils/utils';
import defaultAvatar from '@/assets/imgs/video.png';
export default {
    components: {
        wordDiscern,
        contrastVideo,
        ai,
        aiFlod,
        videoSlice,
        Title,
        scrollingList,
        TextDashboard,
        showOther,
        MonitorParagraphBarrage,
        DiscernShowOther
    },
    mixins: [commonMixin,publicMixin,wordsMixin, aiTypeMixin, contextmenu],
    props: {
        targetType: {
            type:String,
            default: ''
        },
        videoWidth: {
            type: String,
            default: '18%'
        },
        shareId: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            flod: 0,
            paragraphData: null,
            scrollingType: 'all',
            oldType: "",
            model: 0,
            aIHtmlContent: '',
            aiConfig: {},
            // AI 问答页默认聚焦文本问答区，首次进入不主动展开视频区域。
            layoutsValue: [2],
            layouts: [
                {label: '视频', value: 1,},
                {label: '文本', value: 2,}
            ],
            dataDisplay: {},
            otherData: {},
            textDataMap:{},
            textHtmlDataMap:{},
            questionContent: 0,
            tabLaoding: true,
            videoMaxTime: 0,
            sliderValue: 0,
            sliderMarks: {},
            videoDurationText: '',
            headerTabType: 'text',
            headerDataDisplay: {},
            moreConfigState: {
                basicData: {},
                extraConditions: {},
                dynamicConfigs: {}
            },
            showBarrage: false,
            barrageRenderCount: 0,
            barrageRenderTimer: null,
            layoutFoldStage: 0,
            syncingLayoutFold: false
        };
    },
    watch: {
        layoutsValue: {
            /**
             * @description 统一处理布局勾选变化，将手动显隐与折叠按钮状态同步到同一套状态机。
             * @param {number[]} val 当前布局值
             * @param {number[]} oldVal 变更前布局值
             * @returns {void}
             */
            handler(val, oldVal) {
                const nextValue = this.normalizeLayoutsValue(val)
                const prevValue = Array.isArray(oldVal) ? this.normalizeLayoutsValue(oldVal) : []
                if (!this.isSameLayoutsValue(nextValue, val)) {
                    this.layoutsValue = nextValue
                    return
                }
                if (this.syncingLayoutFold) {
                    this.syncingLayoutFold = false
                    return
                }
                this.layoutFoldStage = this.resolveLayoutFoldStageByManualChange(nextValue, prevValue)
            },
            deep: true
        },
        getSentenceMarkData: {
            handler() {
                if (!this.showBarrage) return
                this.restartBarrageLazyRender()
            },
            deep: false
        },
        showBarrage(val) {
            if (val) {
                this.restartBarrageLazyRender()
                return
            }
            this.clearBarrageRenderTimer()
            this.barrageRenderCount = 0
        }
    },
    computed: {
        getVideoVnode() {
            return this.$refs.videoPlayer;
        },
        getTextVnode(){
            return this.$refs?.textDom
        }, 
        getAiDom(){
            return this.$refs?.aiDom
        },
        getOtherItems(){
            return this.getAiDom?.getOtherItems
        },
        getSentenceMarkData(){
            return this.paragraphData || this.sentenceMarkData;
        },
        headerAnchorAvatar() {
            return this.anchorInfo?.AnchorAvatar
                || this.anchorInfo?.anchorAvatar
                || defaultAvatar
        },
        headerAnchorName() {
            if (this.isFileId || this.isFileText) {
                return this.fileInfo?.fileName || this.fileInfo?.FileName || this.fileInfo?.videoName || '--'
            }
            return this.anchorInfo?.AnchorName || this.anchorInfo?.anchorName || this.videoInfo?.videoRename || '--'
        },
        headerTimeLeftLabel() {
            return (this.isFileId || this.isFileText) ? '上传时间' : '开始'
        },
        headerTimeRightLabel() {
            return (this.isFileId || this.isFileText) ? '分析时间' : '结束'
        },
        headerStartTime() {
            if (this.isFileId || this.isFileText) {
                const uploadTime = this.fileInfo?.uploadTime || this.fileInfo?.UploadTime || this.fileInfo?.createTime || this.fileInfo?.CreateTime || ''
                return uploadTime ? String(uploadTime).substring(0, 16) : '--'
            }
            const startTime = this.videoInfo?.StartTime || this.videoInfo?.startTime || ''
            return startTime ? String(startTime).substring(0, 16) : '--'
        },
        headerEndTime() {
            if (this.isFileId || this.isFileText) {
                const analysisTime = this.fileInfo?.analysisTime || this.fileInfo?.AnalysisTime || this.fileInfo?.finishTime || this.fileInfo?.FinishTime || ''
                return analysisTime ? String(analysisTime).substring(0, 16) : '--'
            }
            const endTime = this.videoInfo?.EndTime || this.videoInfo?.endTime || ''
            return endTime ? String(endTime).substring(0, 16) : '--'
        },
        headerDurationText() {
            if (this.videoDurationText) return this.videoDurationText
            const durationValue = Number(this.videoInfo?.durationTime || this.fileInfo?.durationTime || 0)
            return durationValue > 0 ? myUtils.toformatTimeChinse(durationValue * 1000) : '--'
        },
        getMax(){
            return  this.isFileText?1:2
        },
        showLayoutFold() {
            return !this.shareId && !this.isScrolling && !this.isDataBoard && !this.isDataCapture && this.getMax > 0
        },
        isLayoutExpandMode() {
            if (this.isFileText) {
                return this.layoutFoldStage === 1
            }
            return [2, 3].includes(this.layoutFoldStage)
        },
        getStartTime(){
            const {sentenceMarkList} = this.getSentenceMarkData;
            return sentenceMarkList[0]?.startTimeSecond
        },
        getEndTime(){
            const {sentenceMarkList} = this.getSentenceMarkData;
            return sentenceMarkList[sentenceMarkList?.length-1]?.endTimeSecond
        },
        // ai其他配置
        optionConfig(){
            const placeholderKeyMap = {
                ...(this.moreConfigState?.basicData || {}),
                ...(this.moreConfigState?.dynamicConfigs || {})
            }
            const placeholderKeys = Object.keys(placeholderKeyMap).filter(key => !!placeholderKeyMap?.[key])
            return {
                questionContent:  this.questionContent,
                ...this.aiConfig,
                dataDisplay: this.dataDisplay,
                aiAssistantDisplay: this.otherData,
                basicDataConfig: this.moreConfigState?.basicData || {},
                moreConfig: this.moreConfigState,
                placeholderKeys,
                showBarrage: this.showBarrage ? 1 : 0
            };
        },
        moreConfigProps(){
            return {
                sentenceMarkData: this.getSentenceMarkData,
                isCompare: this.isCompare,
                hideMoreConfig: this.targetType === 'uploadFile',
                aiCueType: this.getAiCueTypeKeyValue,
                hide: {
                    onlineNum: this.isViolation,
                    natureTime: !this.isViolation && !this.isAssistant
                }
            }
        },
        getLayouts(){
            return this.layouts?.filter(d=>{
                return this.isFileText? d.value === 2 : true
            });
        },
        getFlod(){
            return this.layoutsValue?.reduce((a,b)=>a+b,0)>2? 2: 1
        },
        getLayoutsValue(){
            return this.layoutsValue?.filter(d=>{
                return this.isFileText? d === 2 : true
            })
        },
        barrageBatchSize() {
            return 5
        }
    },
    methods: {
        /**
         * @description 归一化布局勾选值，只保留合法板块并按固定顺序返回。
         * @param {number[]} values 布局勾选数组
         * @returns {number[]}
         */
        normalizeLayoutsValue(values = []) {
            const allowValues = this.isFileText ? [2] : [1, 2]
            return allowValues.filter((item) => Array.isArray(values) && values.includes(item))
        },
        /**
         * @description 比较两组布局勾选值是否一致。
         * @param {number[]} left 左侧布局值
         * @param {number[]} right 右侧布局值
         * @returns {boolean}
         */
        isSameLayoutsValue(left = [], right = []) {
            if (left.length !== right.length) return false
            return left.every((item, index) => item === right[index])
        },
        /**
         * @description 根据当前折叠阶段返回应展示的布局板块，确保折叠/展开顺序固定为“先视频后文本、先文本后视频”。
         * @param {number} stage 折叠阶段
         * @returns {number[]}
         */
        getLayoutsValueByStage(stage = 0) {
            if (this.isFileText) {
                return stage === 1 ? [] : [2]
            }
            const layoutStageMap = {
                0: [1, 2],
                1: [2],
                2: [],
                3: [2],
                4: [1]
            }
            return layoutStageMap[stage] || [1, 2]
        },
        /**
         * @description 应用指定折叠阶段，并同步更新页面的板块显示。
         * @param {number} stage 折叠阶段
         * @returns {void}
         */
        applyLayoutFoldStage(stage = 0) {
            this.layoutFoldStage = stage
            this.syncingLayoutFold = true
            this.layoutsValue = this.getLayoutsValueByStage(stage)
        },
        /**
         * @description 根据手动勾选后的布局，推导折叠按钮当前应处于的阶段。
         * @param {number[]} nextValue 当前布局值
         * @param {number[]} prevValue 变更前布局值
         * @returns {number}
         */
        resolveLayoutFoldStageByManualChange(nextValue = [], prevValue = []) {
            const nextKey = nextValue.join(',')
            const prevKey = prevValue.join(',')
            if (this.isFileText) {
                return nextKey === '2' ? 0 : 1
            }
            if (nextKey === '1,2') return 0
            if (nextKey === '') return 2
            if (nextKey === '1') return 4
            if (nextKey === '2') {
                // 从“全隐藏”恢复文本时，下一步应继续展开视频；其余情况视为已执行过第一次折叠。
                return (prevKey === '' || [2, 3].includes(this.layoutFoldStage)) ? 3 : 1
            }
            return 0
        },
        /**
         * @description 响应折叠把手点击，按约定顺序在视频/文本板块之间执行折叠与展开。
         * @returns {void}
         */
        onLayoutFoldTrigger() {
            if (this.isFileText) {
                this.applyLayoutFoldStage(this.layoutFoldStage === 1 ? 0 : 1)
                return
            }
            const stageMap = {
                0: 1,
                1: 2,
                2: 3,
                3: 0,
                4: 2
            }
            this.applyLayoutFoldStage(stageMap[this.layoutFoldStage] ?? 0)
        },
        /**
         * @description 清理段落弹幕懒渲染定时器，避免切换视图后仍继续追加渲染。
         * @returns {void}
         */
        clearBarrageRenderTimer() {
            if (this.barrageRenderTimer) {
                clearTimeout(this.barrageRenderTimer)
                this.barrageRenderTimer = null
            }
        },
        /**
         * @description 按批次展示段落弹幕，复用详情页的渲染节奏以控制页面压力。
         * @returns {void}
         */
        restartBarrageLazyRender() {
            this.clearBarrageRenderTimer()
            this.barrageRenderCount = 0
            this.scheduleNextBarrageBatch()
        },
        scheduleNextBarrageBatch() {
            if (!this.showBarrage) return
            const total = this.getSentenceMarkData?.sentenceMarkList?.length || 0
            if (!total) return
            this.barrageRenderCount = Math.min(this.barrageRenderCount + this.barrageBatchSize, total)
            if (this.barrageRenderCount >= total) return
            this.barrageRenderTimer = setTimeout(() => {
                this.scheduleNextBarrageBatch()
            }, 300)
        },
        shouldRenderBarrage(index) {
            return this.showBarrage && Number(index) < Number(this.barrageRenderCount || 0)
        },
        /**
         * @description 生成文本区快速定位刻度，沿用原段落定位条的分段策略。
         * @param {number} videoDurationInt 视频总秒数
         * @returns {Object}
         */
        createSliderMarks(videoDurationInt) {
            const time = Number(videoDurationInt || 0)
            const marks = {
                0: '00:00'
            }
            let remainTime = time
            const markTime = parseInt(time) > 18000 ? 3600 : 1800
            while ((remainTime - 300) > markTime) {
                const current = Object.keys(marks).length * markTime
                marks[current] = `${parseInt(current / 60)}min`
                remainTime -= markTime
            }
            return marks
        },
        formatTooltip(value) {
            return myUtils.toformatTimeMM_ssChinse(Number(value || 0) * 1000)
        },
        onSliderChange(value) {
            this.getTextVnode?.sliderChange?.(value)
        },
        mapQuestionContentToHeaderTab(type) {
            const typeMap = {
                0: 'text',
                1: 'text',
                2: 'aiOptimal'
            }
            return typeMap[Number(type)] || 'text'
        },
        handleAiHeaderTextTypeChange(type) {
            this.headerTabType = type
            this.getTextVnode?.textTypeChange?.(type)
        },
        syncMoreConfigExtraConditions(data = {}){
            this.moreConfigState = {
                ...this.moreConfigState,
                extraConditions: {
                    ...data
                }
            }
        },
        applySharedExtraConditions(data = {}, options = {}){
            const { syncText = false, syncMoreConfig = true } = options
            const nextDisplay = { ...data }
            this.headerDataDisplay = nextDisplay
            this.dataDisplay = nextDisplay
            this.otherData = nextDisplay
            if (syncMoreConfig) {
                this.syncMoreConfigExtraConditions(nextDisplay)
            }
            if (syncText) {
                this.getTextVnode?.otherChange?.(nextDisplay)
            }
        },
        handleHeaderOtherChange(data) {
            this.applySharedExtraConditions(data, {
                syncText: true
            })
        },
        handleAiMoreConfigChange(config = {}){
            const nextConfig = {
                basicData: {
                    ...(config?.basicData || {})
                },
                extraConditions: {
                    ...(config?.extraConditions || {})
                },
                dynamicConfigs: {
                    ...(config?.dynamicConfigs || {})
                }
            }
            this.moreConfigState = nextConfig
            this.showBarrage = !!nextConfig?.basicData?.barrage
            this.applySharedExtraConditions(nextConfig.extraConditions, {
                syncText: true,
                syncMoreConfig: false
            })
        },
        getMaxLen(model){
            let maxLenMap = {
                assistant: 80000,
                violation: 60000,
                scrolling: 100000,
                textAssistant: 60000
            }
            return maxLenMap[this.type] || 60000;

            // if(model === 0 || model === 2){
            //     return 30000
            // }else{
            //     return 60000
            // }
        },
        tabLaodingHandle(val){
            this.tabLaoding = val
        },
        getTextList({type,list,contentHtmlList}){
            this.textDataMap[type] = list;
            this.textHtmlDataMap[type] = contentHtmlList;
        },
        textTypeChange(type){
            this.questionContent = type
            this.headerTabType = this.mapQuestionContentToHeaderTab(type)
        },
        rightContextMenuHanlder(event){
            if(this.shareId){return;}
            let selectText = window.getSelection()?.toString()?.trim();

            this.rightContextMenu(event,{
                copyTxt: selectText
            })
        },
        scrollingDatasParams(params){
            this.aiConfig = params;
        },
        otherChange(data){
            this.applySharedExtraConditions(data)
        },
        initGetListData(){
            // if(this.isDataBoard){
            //     // 数据看板
            // }
            // AI数据识图
            if(this.isDataCapture){
                this.aIHtmlContent = '';
                this.$httpBack.v2300.getExistDataScreenshotList({
                    sourceType: this.getSourceType,
                    sourceId: this.getId
                }).then(res=>{
                    this.aIHtmlContent = res.data?.map(d=>{
                        return  `<h3 class="mg-0 pd-b10">${d.title}</h3>
                        <p class="mg-0 pd-b10 font-s14 text-color2" style="white-space: pre-wrap;line-height: 22px;">${d.aiContent}</p>`;
                    }).join('');
                })
            }
        },
        scrollingChange(type){
            this.$refs?.scrolling?.scrollingChange(type);
            this.$refs.videoSlice?.notSlice(type === 'core', type);
        },
        modelChange(val){
            this.model = val
        },
        quitAi(){
            this.$router.go(-1);
        },
        curveDataChange(type){
            this.$nextTick(()=>{
                this.$refs.aiDom?.setScene('paragraph',type);
            })
        },
        setBrush(data){
            this.$nextTick(()=>{
                this.$refs?.videoSlice?.brushChange({data1: data},'set');
                this.curveDataChange('set');
            })
        },
        sliceParagraphData(paragraphData){
            this.paragraphData = paragraphData;
            if(this.questionContent !== 0){
                this.$nextTick(()=>{
                    this.getTextVnode?.setTextData(
                        this.questionContent,
                        this.textHtmlDataMap[this.questionContent]?.slice(...this.paragraphData?.textIndex)
                    );
                })
            }else{
                this.$nextTick(()=>{
                    if(this.isScrolling){
                        this.$refs.scrolling?.initLoadScrolling();
                    }else if(this.isWordDiscern){
                        this.setWindowPayerIndexMap(this.paragraphData.sentenceMarkList);
                        this.getTextVnode?.refreshParagraph(paragraphData.sentenceMarkList, paragraphData.option);
                    }
                })
            }
        },
        getSpan(index){
            let layoutCount = this.getLayoutsValue.reduce((a,b)=>a+b,0);
            switch(index){
                case 0:
                    // 3=4, 2=0, 1=4, 0=0
                    return this.getLayoutsValue?.includes(1) ? 4 : 0;
                case 1: 
                    // 3=8  2=8, 1=0, 0=0
                    return layoutCount <= 1 ? 0 : 8;
                case 2:
                    // 3=12, 2=16, 1=18, 0=24;
                    return 24 - layoutCount * 4;
            }
        },
        selectParagraph(code){
            if(code){
                this.$nextTick(()=>{
                    this.getAiDom?.selectParagraph(code);
                })
            }
        },
        onToggle(val){
            this.$emit('toggle',val)
        },
        // 文本选中段落改变
        onParagraph(index) {
            
            this.getVideoVnode.setVideoParagraphIndex(index);
        },
        // 视频停止
        onPlayerPause() {
            this.getVideoVnode?.playerPause();
        },
        setDuration({ videoDuration, MathDuration }){
            this.$nextTick(() => {
                this.getTextVnode?.marksTitle(MathDuration);
                this.videoDurationText = Number(videoDuration || 0) > 0 ? myUtils.toformatTimeChinse(Number(videoDuration || 0) * 1000) : this.videoDurationText
                this.videoMaxTime = Number(MathDuration || 0)
                this.sliderMarks = this.createSliderMarks(this.videoMaxTime)
            })
        },
        onPlayerReadied(second) {
            this.getVideoVnode.ponlayerReadied(second);
        },
        // 视频改变段落
        onPlayerParagraphIndex(index) {
            this.getTextVnode?.selectDomeScrollIntoView(index,undefined,'video-ai-only');
        },
        // 播放器进度回调
        onPlayerTimeupdate(time) {
            this.sliderValue = Number(time || 0)
            this.getTextVnode?.setVideoCurrentTime(time);
        },
        // 添加AI问题
        addAiTitleText(data){
            this.getAiDom?.addOther(data);
        },
        setWindowPayerIndexMap(list){
            this.$nextTick(async () => {
                this.initPayerIndexMap();
                this.setPayerIndexMap(list);
            });
        }
    },
    created() {
        this.layoutsValue = this.normalizeLayoutsValue(this.layoutsValue)
        this.layoutFoldStage = this.resolveLayoutFoldStageByManualChange(this.layoutsValue, this.layoutsValue)
    },
     mounted() {
        this.$nextTick(async () => {
            this.initPayerIndexMap();
            this.setPayerIndexMap(this.sentenceMarkList);
            this.initGetListData();
            if (Object.keys(this.headerDataDisplay || {}).length) {
                this.getTextVnode?.otherChange?.(this.headerDataDisplay)
            }
            this.setContextMenuData({
                permission: ['copy']
            })
        });
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() {
        this.clearBarrageRenderTimer()
    }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
};
</script>
<style lang="scss" scoped>

.videoAnalysisContainer {
    height: 100%;
}
.aiAssistantTextBox {
    display: flex;
    flex-direction: column;
}

.aiAssistantHeader {
    background: #fff;
    border-bottom: 1px solid #ebeef5;
}

.aiAssistantHeaderTop {
    min-height: 36px;
}

.aiAssistantAnchorInfo {
    min-width: 0;
}

.aiAssistantAnchorAvatar,
.aiAssistantAnchorAvatarPlaceholder {
    width: 40px;
    height: 40px;
    flex: 0 0 40px;
    border-radius: 50%;
    background: #f2f3f5;
}

.aiAssistantAnchorAvatar {
    object-fit: cover;
}

.aiAssistantAnchorName {
    color: #303133;
    font-size: 16px;
    font-weight: 600;
    line-height: 22px;
    min-width: 0;
    margin-left: 8px;
}

.aiAssistantDuration {
    color: #303133;
    font-size: 15px;
    font-weight: 400;
    line-height: 22px;
}

.aiAssistantHeaderTime {
    margin-top: 10px;
    color: #606266;
    font-size: 14px;
    line-height: 20px;
}

.aiAssistantHeaderControls {
    margin-top: 12px;
    gap: 12px;
    align-items: flex-start;
}

.aiAssistantLayoutLabel {
    color: #606266;
}

.aiAssistantHeaderControls .layout-box {
    flex: 0 0 auto;
}

.aiAssistantHeaderActions {
    flex: 1 1 auto;
    justify-content: flex-end;
    gap: 10px;
}

.aiAssistantLocateBarInner {
    background: #fff;
    padding-top: 6px;
    border-bottom: 1px solid #ebeef5;
}

.aiAssistantSliderBox {
    padding-right: 6px;
}

.aiAssistantHeaderShowOther::v-deep {
    .el-tag {
        margin-right: 0;
    }
}

.aiAssistantHeaderTabGroup::v-deep {
    .el-radio-button__inner {
        min-width: 92px;
        padding: 0 3px !important;
        min-width: 70px;
    }
}

.monitorBarrageSwitch {
    flex: 0 0 auto;
}

.ai-box{
    position: relative;
    &:hover .fold{
        opacity: 1;
        left: 0;
    }
}
.text-dashboard-box{
    ::v-deep(.flows){
        height: auto;
        .child-data{
            display: flex;
            flex-wrap: wrap;
            .child{
                width: 50% !important;
            }
        }
    }
}
.layout-box{
    .el-checkbox-group{
        
        .el-checkbox{
            margin-right: 5px;
            display: flex;
            align-items: center;
            .el-checkbox__input{

            }
            .el-checkbox__label{
                padding-left: 3px;
            }
            // display: flex;
            // align-items: center;
            // justify-content: center;
        }
    }
}
</style>

<!--
@description 详情分析底部数据栏：承载收起按钮、tab 切换以及词表/曲线/数据看板等分析内容。
注意：收起提示仅在当前用户未点击过收起按钮时展示，样式需控制在折叠容器内，避免再次出现溢出裁剪问题。
-->
<template>
    <div class="wordsSummaryContainer brs-10" ref="wordsSummaryContainer" v-if="!hideContainerForPure">
        <div class="fold-hint-anchor">
            <div class="fold-trigger-box">
                <div v-if="showFoldHint" class="fold-hint-badge" aria-hidden="true">
                    <span class="fold-hint-badge__text">收起</span>
                    <i class="fold-hint-badge__arrow"></i>
                </div>
            </div>
        </div>
        <div class="fold-box" :class="{ 'fold-move': foldState }">
            <div class="words-export-container" style="">
                <div class="fold-trigger-box">
                    <el-button type="text" style="font-size: 24px;padding: 3px;" circle @click="fold">
                        <svg class="icon" aria-hidden="true">
                            <use :xlink:href="`#icon-${foldState ? 'a-Frame879' : 'a-Frame1164'}`"></use>
                        </svg>
                    </el-button>
                </div>
                <el-tabs v-model="activeName"  @tab-click="handleClick">
                    <el-tab-pane v-for="(item, index) in getTabs" :key="index" :label="item.label" :name="item.name">
                        <div slot="label" :id="item.id" style="position: relative"
                              v-new-hint="{key: item.name, version: item.newVersion}">{{ item.label }}
                            <img v-if="item.hot" class="xi-icon" src="@/assets/imgs/2_5_8/jian.png" alt="">
                        </div>
                    </el-tab-pane>
                </el-tabs>
                <div v-if="showCurveStepSelector" class="curve-step-header-box">
                    <span class="curve-step-header-label">分钟颗粒</span>
                    <el-select v-model="curveStep" size="mini" class="curve-step-header-select">
                        <el-option
                            v-for="item in curveStepOptions"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"></el-option>
                    </el-select>
                </div>
            </div>
            <div v-if="readonly && !notReadonly[activeName]">
                <slot name="readonly"></slot>
            </div>
            <div v-else class="words-container-content">
                <WordTable v-if="activeName === 'a'" :isNewTag="isNewTag" :isContrast="!!contrastId"
                           :cruxTypeList="CruxTypeList" :wordsInfo="wordsInfo" :filterData="filterData" :column="column"
                           @size-change="sizeChange"
                           @table-len="tableLen" :tableList="tableList" :tabsList="tabsList"
                           :sentenceMarkData="sentenceMarkData"
                           :closeParagraph="closeParagraph" @clickWord="clickWord">
                    <template #word-tag="{ item }">
                        <slot name="word-tag" :item="item"></slot>
                    </template>
                    <template #table-column>
                        <slot name="table-column"></slot>
                    </template>
                    <template #table-scene>
                        <slot name="table-scene"></slot>
                    </template>
                    <template #table-view>
                        <slot name="table-view"></slot>
                    </template>
                    <template #page-left>
                        <slot name="page-left"></slot>
                    </template>
                    <template #page-right>
                        <slot name="page-right"></slot>
                    </template>
                </WordTable>
                <compass v-if="activeName === 'b'" :id="getCompassId" :tradeId="getTradeId" :cruxTypeList="CruxTypeList"
                         :type="compassType" :isContrast="!!contrastId"></compass>
                <curveData v-if="activeName === 'c'" @playerReadied="setPlayerReadied" :sentenceMarkData="sentenceMarkData"
                           :targetType="targetType" @brushChange="brushChange" :isContrast="!!contrastId" @selectSliceAnalysis="selectSliceAnalysis"
                           :notBtn="readonly" :curve-step="curveStep" showSlice>
                </curveData>
                <dataScreenshot v-if="['d','e'].includes(activeName)"
                                @aIContrastData="aIContrastData"
                                @aiAnalysisData="aiAnalysisData"
                                :key="activeName" :targetType="targetType"
                                :isContrast="!!contrastId"
                                :isFile="getChartsVideoInfo"
                                :sentenceMarkData="getSentenceMarkData[getDataKey]"
                                :isWebOnline="isWebOnline">
                </dataScreenshot>
                <div v-if="['f','g','h'].includes(activeName)">
                    <TextDashboard v-if="!contrastId" :requestId="getCompassId" :renderType="activeName"
                                   :sentenceMarkData="sentenceMarkData" :isWebOnline="isWebOnline"
                                   :targetType="targetType"
                                   @aiAnalysisData="aiAnalysisData"/>
                    <TableDashboard v-if="!!contrastId" :requestId="getCompassId" :renderType="activeName"
                                    :sentenceMarkData="sentenceMarkData" @aIContrastData="aIContrastData"/>
                </div>
                <performance v-if="['i'].includes(activeName) && !contrastId"
                                :key="activeName"
                                :isContrast="!!contrastId"
                                :sentenceMarkData="getSentenceMarkData[getDataKey]">
                </performance>
                <template v-if="['j'].includes(activeName) && !contrastId">
                    <RoiData v-if="$store.getters.largeEnterprises"
                             :key="activeName"
                             :targetType="targetType"
                             :sentenceMarkData="getSentenceMarkData[getDataKey]"
                             :roi-response="roiResponse"
                             :roi-loading="roiLoading"
                             :is-web-online="isWebOnline">
                    </RoiData>
                    <div v-else class="font-s12 text-colorc3 pd-t20 flex-jc-c">
                        只有企业和企业以上版本才能抓取和分析ROI相关数据
                    </div>
                </template>

                <template v-for="item in tabs">
                    <slot v-if="activeName === item.name" :name="item.name" :id="getCompassId"></slot>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
/**
 * @description 详情分析底部标签栏组件：统一管理收起展开按钮、标签切换与分析内容面板。
 * 说明：收起提示通过 localStorage 按用户记忆，只在未点击过收起按钮时展示一次性的动画提示。
 */

import myUtils from '/src/utils/utils'
import resize from './../../mixins/resize'
import WordTable from './wordTable.vue'
import compass from './compass'
import curveData from './curveData/index.vue'
import dataScreenshot from './dataScreenshot/index.vue'
import performance from './performance'
import RoiData from './roiData/index.vue'
import TextDashboard from '@/components/analysis/dataBoard/textDashboard.vue'
import TableDashboard from '@/components/analysis/dataBoard/tableDashboard.vue'
import { PLATFORM_TYPE_ENUM,VERSION_TYPE } from '@/enum'

export default {
    name: '',
    components: {
        WordTable,
        compass,
        curveData,
        dataScreenshot,
        performance,
        RoiData,
        TextDashboard,
        TableDashboard,
    },
    mixins: [resize],
    props: {
        isNewTag: {
            type: Boolean,
            default: false
        },
        filterData: {
            type: Function,
            default: null
        },
        callbackTags: {
            type: Function,
            default: null
        },
        closeParagraph: {
            type: Function,
            default: () => { }
        },
        tableList: {
            type: Array,
            default: () => {
                return []
            }
        },
        tabsList: {
            type: Array,
            default: () => {
                return []
            }
        },
        pages: {
            type: Array,
            default: () => {
                return []
            }
        },
        column: {
            type: Array,
            default: () => { return [] }
        },
        tabs: {
            type: Array,
            default: () => {
                return []
            }
        },
        contrastId: {
            type: String,
            default: ''
        },
        sentenceMarkData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        isWebOnline: {
            type: Boolean,
            default: false
        },
        wordsInfo: {
            type: Object,
            default: () => {
                return {}
            }
        },
        readonly: {
            type: Boolean,
            default: false
        },
        tabsOrder: {
            type: Object,
            default: () => {
                return {}
            }
        },
        notReadonly: {
            type: Object,
            default: () => {return {}}
        },
        targetType: {
            type: String,
            default: ''
        }
    },
    computed: {
        getCompassId () {
            return this.contrastId || this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.fileInfo?.fileId
        },
        getSentenceMarkData () {
            return this.contrastId ? this.sentenceMarkData : { data1: this.sentenceMarkData }
        },
        getDataKey () {
            const keys = { d: 'data1', e: 'data2' }
            return keys[this.activeName] || 'data1'
        },
        compassType () {
            return this.contrastId ? 1 : this.sentenceMarkData?.videoInfo?.VideoId ? 0 : 2
        },
        getTradeId () {
            const { videoInfo, fileInfo, uploadFile } = this.sentenceMarkData
            return videoInfo?.TradeId || fileInfo?.TradeId || uploadFile?.TradeId
        },
        getVideoId () {
            return this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.videoInfo?.videoId || ''
        },
        isKuaishou(){
            // 对比分析或者单独的分析 只要其中有一个platform是1 就认为是快手
            if(this.sentenceMarkData?.data1){
                const {data1,data2} = this.sentenceMarkData;
                let platform1 = (data1.videoInfo?.PlatformType || data1.fileInfo?.platformType) == PLATFORM_TYPE_ENUM.kuaishou;
                let platform2 = (data2.videoInfo?.PlatformType || data2.fileInfo?.platformType) == PLATFORM_TYPE_ENUM.kuaishou;
                return platform1 || platform2;
            }else{
                return (this.sentenceMarkData?.videoInfo?.PlatformType || this.sentenceMarkData?.fileInfo?.platformType) == PLATFORM_TYPE_ENUM.kuaishou;
            }
        },
        getChartsVideoInfo () {
            if (this.contrastId) {
                return !this.sentenceMarkData?.data1?.videoInfo?.VideoId && !this.sentenceMarkData?.data2?.videoInfo?.VideoId
            } else {
                return !this.sentenceMarkData?.videoInfo?.VideoId
            }
        },
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
        isShort(){
            return this.getReplayType === 'replayShort'
        },
        versionType(){
           return this.$store.getters.getVersionType
        },
        roiAuthEnabled() {
            const qcAuthStatus = this.roiResponse?.qcAuthStatus
            const jlbyAuthStatus = this.roiResponse?.jlbyAuthStatus
            return [qcAuthStatus, jlbyAuthStatus].some((val) => Number(val) === 1)
        },
        canShowRoiTab() {
            return !this.contrastId
                && this.versionType !== VERSION_TYPE.PURE
                && this.$store.getters.largeEnterprises
                && !!this.getVideoId
                && this.roiAuthEnabled
        },
        getTabs () {
            const defaultTabsOrder = {
                f: 1, // 数据看板
                c: 2, // 数据曲线
                a: 3, // 直播高频词
                g: 4, // 人群画像
                h: 5, // 流量结构
                j: 6, // 投放数据
                i: 7, // 业绩统计
                d: 8, // AI数据识图
                b: 9, // 内容罗盘
            }
            let tbsList = [
                {
                    label: '数据看板', name: 'f', newVersion: '2.4.1', hide: () => {
                        return this.getChartsVideoInfo || this.isKuaishou || this.isShort || this.versionType === VERSION_TYPE.PURE
                    }
                },
                {
                    label: '直播高频词', name: 'a', id: 'wordsType-dom', hot: true, hide: () => {
                        return this.versionType === VERSION_TYPE.PURE
                    }
                },
                {
                    label: '数据曲线', name: 'c', hide: () => {
                        //老板说 数据曲线不管有没有数据都将tab展示出来（查看话术脚本的除外）
                        return this.getChartsVideoInfo || this.isKuaishou || this.isShort
                    }
                },
                {
                    label: '投放数据', name: 'j', newVersion: '2.60.4', hide: () => {
                        return !!this.contrastId || this.getChartsVideoInfo || this.isKuaishou || this.isShort || this.versionType === VERSION_TYPE.PURE
                    }
                },
                {
                    label: '人群画像', name: 'g', newVersion: '2.5.0', hide: () => {
                        return this.getChartsVideoInfo || this.isKuaishou || this.isShort || this.versionType === VERSION_TYPE.PURE
                    }
                },
                {
                    label: '业绩统计', name: 'i', newVersion: '2.5.0', hide: () => {
                        return !!this.contrastId || this.getChartsVideoInfo || this.isKuaishou || this.isShort || this.versionType === VERSION_TYPE.PURE || this.$store.getters.getPackageLevel!==30
                    }
                },
                {
                    label: '流量结构', name: 'h', newVersion: '2.5.0', hide: () => {
                        return this.getChartsVideoInfo || this.isKuaishou || this.isShort || this.versionType === VERSION_TYPE.PURE
                    }
                },
                {
                    label: '内容罗盘', name: 'b', id: 'compass-dom', hide: () => {
                        return this.isShort || this.versionType === VERSION_TYPE.PURE
                    }
                },
                ...this.tabs,
            ]
            // 加入排序
            return tbsList.map(d => {
                if (this.tabsOrder[d.name]) {
                    d.index = this.tabsOrder[d.name]
                } else if (defaultTabsOrder[d.name]) {
                    d.index = defaultTabsOrder[d.name]
                } else {
                    d.index = 999
                }
                return d
            }).sort((a, b) => a.index - b.index).filter(item => {
                if (this.readonly && !this.notReadonly[item.name]) {
                    return true
                }
                if (item.hide) {
                    return !item.hide()
                }
                return true
            })
        },
        hideContainerForPure() {//全部隐藏
            return this.versionType === VERSION_TYPE.PURE && this.getReplayType === 'fileAll' || this.getTabs.length === 0
        },
        showCurveStepSelector() {
            return this.activeName === 'c'
        },
    },
    data () {
        return {
            VERSION_TYPE,
            activeName: 'a',
            foldState: false,
            CruxTypeList: [],
            roiLoading: false,
            roiResponse: null,
            showFoldHint: false,
            curveStep: '',
            curveStepOptions: []
        }
    },
    watch: {
        // 监听pages
        // pages: {
        //     handler(val){
        //         if(val.length){
        //             this.setPage(val);
        //         }
        //     },
        //     immediate: true,
        //     deep: true
        // }
        activeName: {
            handler (newVal) {
                if (!['a'].includes(newVal)) {
                    this.$emit('isAiElfChange')
                }
            },
            deep: true
        },
        versionType: {
            handler(newVal) {
                if (newVal === VERSION_TYPE.PURE) {
                    this.activeName = 'c'
                }
            },
            deep: true,
            immediate: true,
        }
        ,
        getVideoId: {
            handler() {
                this.loadVideoRoiMeta()
            },
            immediate: true
        }
    },
    mounted () {
        this.ifSelectActiveName()
        this.initFoldHint()
    },
    created () {
        this.getCruxTypeList();
        this.loadCurveStepOptions();
    },
    methods: {
        /**
         * @description 将字典项 value 尽量转成数字，供分钟颗粒请求参数直接复用。
         * @param {*} value 字典值
         * @returns {*}
         */
        normalizeCurveStepValue(value) {
            const numberValue = Number(value)
            return Number.isFinite(numberValue) ? numberValue : value
        },
        /**
         * @description 拉取在线时间颗粒度字典，并复用到数据曲线分钟颗粒选项中。
         * @returns {Promise<void>}
         */
        async loadCurveStepOptions() {
            if (!this.$httpBack?.dictdata?.dictDataListByCodes) return
            try {
                const res = await this.$httpBack.dictdata.dictDataListByCodes({ codes: 'online_time_scale' })
                if (res?.code !== 0) return
                const list = Array.isArray(res?.data?.online_time_scale) ? res.data.online_time_scale : []
                const nextOptions = list
                    .map(item => ({
                        label: item?.label || `${item?.value}分钟`,
                        value: this.normalizeCurveStepValue(item?.value)
                    }))
                    .filter(item => item.value !== '' && item.value !== null && item.value !== undefined)
                    .sort((a, b) => Number(a.value) - Number(b.value))
                if (!nextOptions.length) return
                this.curveStepOptions = nextOptions
                this.curveStep = nextOptions[0].value
            } catch (e) {
            }
        },
        async loadVideoRoiMeta() {
            if (this.contrastId) {
                this.roiResponse = null
                this.roiLoading = false
                return
            }
            if (this.versionType === VERSION_TYPE.PURE) {
                this.roiResponse = null
                this.roiLoading = false
                return
            }
            if (!this.$store.getters.largeEnterprises) {
                this.roiResponse = null
                this.roiLoading = false
                return
            }
            const videoId = this.getVideoId
            if (!videoId) {
                this.roiResponse = null
                this.roiLoading = false
                return
            }
            if (!this.$httpBack?.v2000?.getVideoRoi) {
                this.roiResponse = null
                this.roiLoading = false
                return
            }
            this.roiLoading = true
            try {
                const res = await this.$httpBack.v2000.getVideoRoi({ videoId })
                if (res?.code === 0) {
                    this.roiResponse = res?.data || {}
                } else {
                    this.roiResponse = null
                }
            } catch (e) {
                this.roiResponse = null
            } finally {
                this.roiLoading = false
            }
        },
        ifSelectActiveName(name) { 
            if(this.sentenceMarkData?.videoInfo){
                this.activeName = 'f'
            }
        },
        handleClick(){
            if(this.foldState === true){
                this.foldState = false;
            }
            this.$emit('tabs-click', this.activeName)
        },
        aIContrastData (type) {
            this.$emit('aIContrastData', type)
        },
        aiAnalysisData (data) {
            this.$emit('aiAnalysisData', data)
        },
        isAiElfChange () {
            this.$emit('isAiElfChange')
        },
        // 设置播放器ready
        setPlayerReadied (time) {
            try {
                let d = new Date(this.sentenceMarkData?.videoInfo?.StartTime + ' UTC')
                let startTime = d.getTime()
                let second = ((time - startTime))
                // this.$emit('playerReadied', second)

                const getAllObjectKeys = () => {
                    const keys = []
                    for (const key in this.sentenceMarkData) {
                        if (this.sentenceMarkData.hasOwnProperty(key)) {
                            keys.push(key)
                        }
                    }
                    return keys
                }
                if (getAllObjectKeys().includes('videoInfo')) {
                    this.$emit('playerReadied', myUtils.toSecond(time) * 1000)
                } else {
                    Object.keys(this.sentenceMarkData).forEach((item, index) => {
                        const { videoInfo = {} } = this.sentenceMarkData[item]
                        if (myUtils.toSecond(videoInfo?.Duration) >= myUtils.toSecond(time)) {
                            this.$emit('playerReadied', index, myUtils.toSecond(time) * 1000)
                        }
                    })
                }
            } catch (e) {
                console.error(e)
            }
        },
        selectActiveName (nameOrIndex) {
            if (typeof nameOrIndex === 'number') {
                let o = this.getTabs.find((item, index) => index === nameOrIndex)
                if (o) {
                    this.activeName = o.name
                }
            } else {
                this.activeName = nameOrIndex
            }
        },
        getCruxTypeList () {
            if (this.contrastId) {
                const { data1, data2 } = this.sentenceMarkData
                this.CruxTypeList = [data1?.cruxTypeList, data2?.cruxTypeList]
            } else {
                this.CruxTypeList = [this.sentenceMarkData?.cruxTypeList] || []
            }
        },
        refreshHandler (type) {
            // 暂时只对选中雷达图tab进行刷新，其他都是通过跟新数据刷新数据，只有雷达图需要重新获取数据。
            if (this.activeName === 'b') {
                this.activeName = ''
                this.$nextTick(() => {
                    this.activeName = 'b'
                })
            }
            this.getCruxTypeList()
        },
        tableLen (len) {
            this.$emit('tableLen', len)
        },
        sizeChange (size) {
            this.$emit('sizeChange', size)
        },
        brushChange (data) {
            this.$emit('brushChange', data)
        },
        selectSliceAnalysis(data){
            this.$emit('selectSliceAnalysis', data)
        },
        // setPage(pages){
        //     this.page_self = pages;
        // },
        /**
         * @description 获取当前用户对应的收起提示缓存 key。
         * @returns {string}
         */
        getFoldHintStorageKey() {
            const userId = this.$store?.state?.userInfo?.id || this.$store?.state?.userInfo?.userId || 'default'
            return `analysis_control_tabs_fold_hint_clicked_${userId}`
        },
        /**
         * @description 初始化收起按钮提示，只对当前未点击过收起的用户展示。
         * @returns {void}
         */
        initFoldHint() {
            const hasClicked = localStorage.getItem(this.getFoldHintStorageKey()) === '1'
            this.showFoldHint = !hasClicked
        },
        /**
         * @description 记录当前用户已点击过收起按钮，并关闭提示展示。
         * @returns {void}
         */
        markFoldHintClicked() {
            localStorage.setItem(this.getFoldHintStorageKey(), '1')
            this.showFoldHint = false
        },
        fold () {
            if (this.showFoldHint) {
                this.markFoldHintClicked()
            }
            this.foldState = !this.foldState
            this.$emit('fold-change', this.foldState)
        },
        setFold  (val) {
            this.foldState = val;
        },
        clickWord (val) {
            this.$emit('clickWord', val)
        },
    }
}
</script>

<style scoped lang="less">
.wordsSummaryContainer {
    // padding-top: 6px;
    position: relative;
    overflow: visible;
    font-weight: 600;
    font-size: 14px;
    color: #2E3742;
    background: #E8F5FF;
    // border-top: 0.5px solid #eee;
    // background: #2E3742;

}

.xi-icon {
    height: 18px;
    width: 18px;
    position: absolute;
    top: 1px;
}
.words-export-container {
    display: flex;
    align-items: center;
    padding: 0 5px;

    ::v-deep(.el-tabs) {
        flex: 1 1 auto;
        min-width: 0;

        .el-tabs__header {
            margin: 0;
        }

        // .el-tabs__active-bar{
        //     display: none;
        // }
        .el-tabs__item {
            padding: 0 10px;
            // height: 30px;
            // line-height: 30px;
            color: #151719;
            font-weight: 500;
            font-size: 14px;
        }

        .is-active {
            font-weight: bold;
        }

        .el-tabs__nav {
            padding-right: 10px;
        }

        .el-tabs__nav-wrap::after {
            display: none;
        }

        .el-tabs__item.is-active {
            color: #151719;
        }
    }


    .curve-step-header-box {
        margin-left: auto;
        display: flex;
        align-items: center;
        gap: 8px;
        padding-right: 10px;
        flex: 0 0 auto;
    }

    .curve-step-header-label {
        color: #606266;
        font-size: 12px;
        font-weight: 500;
        white-space: nowrap;
    }

    .curve-step-header-select {
        width: 86px;
    }

    .curve-step-header-select::v-deep .el-input__inner {
        background: transparent;
        border-color: #dcdfe6;
    }

    .curve-step-header-select::v-deep .el-input__suffix {
        background: transparent;
    }
}

.fold-hint-anchor {
    position: absolute;
    top: 10px;
    left: 0;
    z-index: 999;
    pointer-events: none;
}

.fold-trigger-box {
    position: relative;
    flex: 0 0 auto;
}

.fold-hint-badge {
    position: absolute;
    top: -4px;
    left: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 54px;
    padding: 6px 5px 10px;
    border: 1px solid transparent;
    border-radius: 12px;
    background-image: linear-gradient(90deg, #FF6000 0%, #FF2230 100%), linear-gradient(90deg, #FF934D 0%, #FF555E 100%);
    background-origin: border-box;
    background-clip: padding-box, border-box;
    box-shadow: 0 2px 5px rgba(255, 76, 41, 0.14);
    pointer-events: none;
    transform: translate(-50%, -100%);
    transform-origin: center bottom;
    animation: fold-hint-bounce 0.65s ease-in-out infinite;
    z-index: 4;
}

.fold-hint-badge__text {
    position: relative;
    z-index: 2;
    color: #FFFFFF;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    font-size: 14px;
    font-weight: normal;
    line-height: 1.1;
    white-space: nowrap;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    letter-spacing: 0;
}

.fold-hint-badge__text::before {
    content: '★';
    display: block;
    margin-bottom: 4px;
    color: #FFD24D;
    font-size: 18px;
    line-height: 1;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.22);
}

.fold-hint-badge__arrow {
    position: absolute;
    left: 50%;
    bottom: -5px;
    width: 10px;
    height: 10px;
    background: linear-gradient(180deg, #FF4F1E 0%, #FF2A2D 100%);
    transform: translateX(-50%) rotate(45deg);
    border-bottom-right-radius: 2px;
}

.fold-box {
    position: relative;
    overflow: hidden;
    transition: max-height 1s ease-in;
    height: auto;
}

@keyframes fold-hint-bounce {
    0%, 100% {
        transform: translate(-50%, -100%) translateY(0);
    }
    35% {
        transform: translate(-50%, -100%) translateY(-9px);
    }
    70% {
        transform: translate(-50%, -100%) translateY(-3px);
    }
}

.fold-move {
    max-height: 38px;
    position: relative;
    bottom: 0;

}
</style>

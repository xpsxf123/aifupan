<template>
    <div style="height: 100%;">
        <el-row  style="height:100%" class="analysis-flex-column">
            <!-- 上传文件布局 -->
            <el-col v-if="isFile" :span="24"  class="common-bg analysis-flex-column">
                <div class="analysis-box">
                    <el-row style="height: 100%;">
                        <!-- 直接渲染两个分析列表 不带视频 -->
                        <el-col v-for="(item, index) in getDatas" :span="12" :key="index" style="height: 100%;">
                            <analysis-contrast-item :sentenceMarkData="item" :videoWidth="'24%'"
                               :isNotVideo="true" :ref="`analysis${index}`"
                                :name="`A${index+1}`" :deafultWordsInfo="deafultWordsInfo" isCompare
                                :tabsHeight="getTabHeight"
                                @updateMarkwords="(obj) => { markWords(index, obj, 'update') }"
                                @markwords="(obj) => { markWords(index, obj, 'fileMarkwords') }" 
                                >
                            </analysis-contrast-item>
                        </el-col>
                    </el-row>
                </div>
            </el-col>
            <el-col v-else :span="24" class="common-bg analysis-flex-column">
                <div :style="getWindonHeight" style="height: 100%;overflow: hidden;">
                    <el-row :gutter="8" style="height: 100%;;overflow: hidden;">
                        <!-- 视频分享布局 -->
                        <el-col :span="leftFold == 1?0:8" style="height: 100%;">
                            <el-row class="analysis-flex-column" style="height: 100%;">
                                <el-col :span="24" style="flex: 0;">
                                    <el-row :gutter="8" style="height: 100%;">
                                        <el-col style="height: 100%;" v-for="(item, index) in getDatas" :span="12">
                                            <div style="height: 100%;" :class="`main-bg pd-8 analysis-container-${index}`">
                                                <analysisTitle :class="`analysis-title-${index}`"
                                                    :ref="`title_${index}`" isCompare notPace isColumn :syncScene="syncScene"
                                                    :sentenceMarkData="item" :wordsInfo="index?wordsInfo2:wordsInfo1"
                                                    @markClick="(obj) => { markWords(index, obj,'clickMark') }">
                                                </analysisTitle>
                                            </div>
                                        </el-col>
                                    </el-row>
                                </el-col>
                                <el-col :span="24" style="flex: 1; overflow: hidden;">
                                    <el-row :gutter="8" style="height: 100%;" class="pd-t8 pd-b8">
                                        <el-col v-for="(item, index) in getDatas" :span="12" style="height: 100%;">
                                            <!-- 视频/音频 -->
                                            <contrastVideo  style="height: 100%;" :sentenceMarkData="item" :ref="`video_${index}`"
                                                :name="`A${index + 1}`" isCompare
                                                @setDuration="(...arg) => { setDuration(index, ...arg) }"
                                                @playerTimeupdate="(...arg) => { onPlayerTimeupdate(index, ...arg) }"
                                                @changeParagraphIndex="(...arg) => { onPlayerParagraphIndex(index, ...arg) }">
                                            </contrastVideo>
                                            <!-- <img v-else src="@/assets/imgs/bqfxText.png" style="max-width: 100%;max-height: 100%;opacity: 0;" /> -->
                                        </el-col>
                                    </el-row>
                                </el-col>
                            </el-row>
                        </el-col>
                        <el-col class="flod-content-box overflow_hidden" :span="leftFold == 1?24:16" style="height: 100%;">
                            <el-row :gutter="8" style="height: 100%;">
                                <el-col v-for="(item, index) in getDatas" :span="12" style="height: 100%"
                                    class="contrastText-box">
                                    <wordDiscern :ref="`text_${index}`" class="brs-8" :name="`A${index + 1}`" :style="{height: '100%',border: `1px solid ${index===0?'#79C5FF':'#FF9B70'}`}"
                                        :sentenceMarkData="item" :cruxTypeMap="cruxTypeMap" isCompare
                                        :deafultWordsInfo="deafultWordsInfo"
                                        @onParagraph="(...arg) => { onParagraph(index, ...arg) }"
                                        @playerReadied="(...arg) => { onPlayerReadied(index, ...arg) }"
                                        @playerPause="(...arg) => { onPlayerPause(index, ...arg) }"
                                        @markwords="(obj) => { markWords(index, obj,'loadWrod') }"
                                        @updateMarkwords="(obj) => { markWords(index, obj, 'update') }">
                                        <template #toolbar-left>
                                            <Pace class="pace-box" v-if="item.fileInfo?.fileType!==2" isCompare isColumn :charConut="getCharCount(index)"
                                                :videoTime="getVideoTime(index)"
                                                @click="(val) => { onClickPace(index, val) }"></Pace>
                                        </template>
                                    </wordDiscern>
                                </el-col>
                            </el-row>
                            <aiFold class="fold" :max="1" @change="(val)=>{leftFold = val}"></aiFold>
                        </el-col>
                    </el-row>
                </div>
            </el-col>
            <el-col :span="24">
                <!-- 底部关键词汇总 -->
                <ControlTabs :sentenceMarkData="sentenceMarkData" :contrastId="contrastId"
                             :isWebOnline="isWebOnline"
                :tabsList="getTagsList" :targetType="targetType"
                             :tabs="tabs"
                @brushChange="brushChange" @isAiElfChange="isAiElfChange" @aIContrastData="aIContrastData"
                :tableList="getTableList" @fold-change="foldChange"
                @playerReadied="onPlayerReadied"
                    @tableLen="tableLen">
                    <template #word-tag="{ item }">
                        <span>
                            {{ item.label }}:
                            <span v-if="item.key === 'all'">{{ item.countNum }}</span>
                            <span v-else>
                                <span class="contrast-c1">{{ item.countNum0 || 0 }}</span>|<span class="contrast-c2">{{
                                    item.countNum1 || 0 }}</span>
                            </span>
                        </span>
                    </template>
                    <template #table-column>
                        <el-table-column prop="countNum" label="命中次数-视频1" align="center" width="110">
                            <template slot-scope="{row}">
                                <WordTableCountNum :isShow="`${row.name}${row.wordsType}` === getSelectMark(0)?.name"
                                    :countNum="row.countNum0" :index="getSelectIndex(0)" @selectMark="(type) => {
                                        selectMarkHandler(0, row.name + row.wordsType, type)
                                    }"></WordTableCountNum>
                            </template>
                        </el-table-column>
                        <el-table-column prop="countNum" label="命中次数-视频2" align="center" width="110">
                            <template slot-scope="{row}">
                                <WordTableCountNum :isShow="`${row.name}${row.wordsType}` === getSelectMark(1)?.name"
                                    :countNum="row.countNum1" :index="getSelectIndex(1)" @selectMark="(type) => {
                                        selectMarkHandler(1, row.name + row.wordsType, type)
                                    }"></WordTableCountNum>
                            </template>
                        </el-table-column>
                    </template>
                    <template #page-right>
                        <slot name="page-right"></slot>
                    </template>
                </ControlTabs>
            </el-col>
        </el-row>
        
    </div>
</template>

<script>
import analysisContrastItem from "./contrast-only.vue"
import ControlTabs from '/src/components/analysis/controlTabs.vue';
import WordTableCountNum from '/src/components/analysis/wordTableCountNum.vue';

import analysisTitle from './component/analysisTitle.vue';
import wordDiscern from "./component/wordDiscern.vue";
import contrastVideo from "./contrast-video.vue";

import publicMixin from "./mixin/publicMixin.js";
import myUtils from "../../../utils/utils";
import Pace from '/src/components/analysis/pace.vue';
import aiFold from './../aiAnalysis/aiFold.vue';
export default {
    components: {
        analysisContrastItem,
        ControlTabs,
        WordTableCountNum,
        analysisTitle,
        wordDiscern,
        contrastVideo,
        Pace,
        aiFold
    },
    inject: [],
    mixins: [ publicMixin ],
    provide(){
        return {
            contrastMain: this,
        }
    },
    props: {
        contrastId: {
            type: String,
            default: ''
        },
        isWebOnline: {
            type: Boolean,
            default: false
        },
        sentenceMarkData: {
            type: Object,
            default: ()=>{return {}}
        },
        targetType:{
            type: String,
            default: ''
        },
        syncScene: {
            type: [String, Number],
            default: ''
        }
    },
    data() {
        return {
            // 分析数据
            // sentenceMarkData: {
            //     data1: {},
            //     data2: {}
            // },
            deafultWordsInfo: {
                markCrux: true,
                markSensitive: true,
            },
            // 对比分析1，敏感词开关
            wordsInfo1: {},
            // 对比分析2，敏感词开关
            wordsInfo2: {},
            // 关键词/敏感词数组
            wordsList: [],
            // 分页peages
            pageSize: 2,
            fold: false,
            len: 0,
            // 关键词汇总防抖
            debounceCountWords: null,
            leftFold: 0,
            tabs: [
                {
                    label: 'AI数据识图1', name: 'd', newVersion: '2.4.1',
                }, {
                    label: 'AI数据识图2', name: 'e', newVersion: '2.4.1',
                },
            ]
        };
    },
    computed: {
        // 获取
        getDatas() {
            const { data1, data2 } = this.sentenceMarkData;
            return [data1, data2];
        },
        // 文件对比分析
        isFile() {
            const [data1, data2] = this.getDatas;
            let f1 = data1?.fileInfo?.fileType === 2;
            let f2 = data2?.fileInfo?.fileType === 2;
            return f1 && f2
        },
        getTagsList(){
            const {data1, data2} = this.sentenceMarkData;
            let dataMap = {};
            [data1?.wordsTabList,data2?.wordsTabList]?.forEach((list,index)=>{
                list?.forEach(item=>{
                    let name = item.tabName;
                    if(typeof dataMap[name] === 'undefined'){
                        dataMap[name] = {
                            ...item,
                            tabSort: item.tabSort<0? (item.tabType+1)/10 : item.tabSort,
                            countNum: 0
                        }
                    }
                    dataMap[name].countNum+=item.num;
                    dataMap[name][`countNum${index}`] = item.num;
                })
            });
            let tags = Object.values(dataMap)
            return tags;
        },
        // 获取关键词列表
        getTableList() {
            // 整合关键词数据
            const {data1, data2} = this.sentenceMarkData;
            let dataMap = {};
            [data1?.wordsCollect,data2?.wordsCollect]?.forEach((list,index)=>{
                list?.forEach(item=>{
                    let name = item.name + item.wordsType;
                    if(typeof dataMap[name] === 'undefined'){
                        dataMap[name] = {
                            ...item,
                            totalNum: 0
                        }
                    }
                    dataMap[name].totalNum+=item.totalNum;
                    dataMap[name][`countNum${index}`] = item.countNum;
                })
            })
            return Object.values(dataMap)?.sort(myUtils.wordsSort);
        },
        // 窗口高度
        getWindonHeight() {
            return {
                height: `calc(100vh - ${this.fold ? 100 : this.len * 24 + 200}px)`
            }
        },
        // 设置表格高度
        getTabHeight() {
            return this.fold ? 70 : (this.len * 23) + 180
        }
    },
    watch: {
    },
    methods: {
        brushChange(data){
            this.$emit('brushChange',data)
        },
        aIContrastData(type){
            this.$emit('aIContrastData',type)
        },
        isAiElfChange(){
            this.$emit('isAiElfChange')
        },
        getAnalysisRef(index) {
            if (this.isFile) {
                return this.$refs?.[`analysis${index}`]?.[0];
            } else {
                return this.getTextVnode(index);
            }
        },
        getSelectMark(index) {
            // console.log(this.getAnalysisRef(index),'---this.getAnalysisRef(index)?.selectMark');
            return this.getAnalysisRef(index)?.selectMark
        },
        getSelectIndex(index) {
            return this.getAnalysisRef(index)?.selectIndex
        },
        selectMarkHandler(index, name, type) {
            this.getAnalysisRef(index)?.selectMarkHandler(name, type)
        },
        // 语速功能
        getCharCount(index) {
            let data =  this.sentenceMarkData?.[`data${index+1}`] || {};
            return data.allCharCountNum;
        },
        getVideoTime(index) {
            return this.getTitleVnode(index)?.getVideoTime || 0
        },
        onClickPace(index, value) {
            this.getTextVnode(index).setAnalysisChar(value);
        },
        // 
        foldChange(bl) {
            this.fold = bl;
        },
        tableLen(s) {
            this.len = s;
        },
        
        markWords(index, obj, type) {
            if (index === 0) {
                this.wordsInfo1 = obj;
            } else {
                this.wordsInfo2 = obj;
            }
            if(type === 'update'){
                this.getCruxWordTypeToTidyCountWords(index);
                return;
            }
            this.debounceCountWords(index,type);
        },
        // 获取词类型过后在整理词列表
        getCruxWordTypeToTidyCountWords(index){
            this.getCruxWordType((cruxTypeMap)=>{
                this.$nextTick(()=>{
                    this.getAnalysisRef(index)?.tidyCountWords(cruxTypeMap);
                })
            });
        },
        // 获取关键词汇总表数据
        // type 用于区分对比分析数据。
        getWordsTableListData(item, type, obj) {
            const { wordsList } = item;
            wordsList?.forEach(data => {
                 // 更具关键词类型统计
                let nameType = data.name + data.wordsType;
                // 通过关键词类型进行数据统计
                if (typeof obj[nameType] === 'undefined') {
                    obj[nameType] = {
                        ...JSON.parse(JSON.stringify(data)),
                        countNumA1: 0,
                        countNumA2: 0,
                        count: 0
                    }
                }
                obj[nameType]['countNum' + type] += data['countNum' + type];
                obj[nameType].count += data.count;
            })
        },
        getTitleVnode(index) {
            return this.$refs?.[`title_${index}`]?.[0]
        },
        getTextVnode(index) {
            return this.$refs?.[`text_${index}`]?.[0]
        },
        getVideoVnode(index) {
            return this.$refs?.[`video_${index}`]?.[0]
        },
        // 整理敏感词/关键词列表
        countWords() {
            this.wordsList = [];
            let wordsMap = {};
            // 通过单独的词数据整合为一组词数据
            this.getWordsTableListData(this.wordsInfo1, 'A1', wordsMap);
            this.getWordsTableListData(this.wordsInfo2, 'A2', wordsMap);
            this.$set(this, 'wordsList', Object.values(wordsMap));
        },
        // 视频加载完成回调事件
        onCanplay() {},
        // 预加载
        setDuration(index, { videoDuration, MathDuration }){
            this.$nextTick(() => {
                let data =  this.sentenceMarkData?.[`data${index+1}`] || {};
                this.getTitleVnode(index)?.setCharAndTime(videoDuration, data.allCharCountNum);
                this.getTextVnode(index)?.marksTitle(MathDuration);
            })
        },
        // 视频改变段落
        onPlayerParagraphIndex(index, paragraphIndex) {
            this.getTextVnode(index)?.selectDomeScrollIntoView(paragraphIndex,undefined,'video-layout');
        },
        // 播放器进度回调
        onPlayerTimeupdate(index, time) {
            this.getTextVnode(index)?.setVideoCurrentTime(time);
        },

        // 设置播放器进度，秒
        onPlayerReadied(index, second) {
            this.getVideoVnode(index)?.ponlayerReadied(second);
        },
        // 视频停止
        onPlayerPause(index) {
            this.getVideoVnode(index)?.playerPause();
        },
        // 文本选中段落改变
        onParagraph(index, paragraphIndex) {
            this.getVideoVnode(index)?.setVideoParagraphIndex(paragraphIndex);
        },
    },
    created() {
        this.debounceCountWords = myUtils.debounce(10, (index,type) => {
            this.countWords()
        });
    },
    mounted() {
       
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() {
        
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
@import "~@/assets/scss/var.scss";

.analysis-box {
    height: calc(100%);
}

.analysis-container-0{
    background: #E0EFFF;
    border-radius: 8px 8px 8px 8px;
    border: 1px solid #79C5FF;
}
.analysis-title-0 {
    ::v-deep(.anchor-name) {
        color: $c1;
    }
}
.analysis-container-1{
    background: #FFF3E7;
    border-radius: 8px 8px 8px 8px;
    border: 1px solid #FF9B70;
}
.analysis-title-1 {
    ::v-deep(.anchor-name) {
        color: $c2;
    }
}

.contrastText-box {
    ::v-deep(.discernContainer) {
        height: 100%;
    }
    &:first-child{
        ::v-deep(.discernContainer) {
            .locating-bar-box {
                .video-slider {
                    background: #E0EFFF !important;
                }
            }
        }
    }

    &:nth-child(2) {
        ::v-deep(.discernContainer) {
            .locating-bar-box {
                .video-slider {
                    background: #FFF3E7 !important;
                }
            }
        }
    }
}

.pace-box {
    padding: 0;

    ::v-deep(.wordsItemColorContainer) {
        .wordsItemText {
            min-width: auto;
            text-align: left;
            margin-left: 0;
        }
    }
}
.flod-content-box{
    position: relative;
    padding-bottom: 8px;
    &:hover .fold{
        opacity: 1;
        left: 4px;
    }
}
</style>
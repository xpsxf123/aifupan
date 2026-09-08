<template>
    <div style="height: 100%;">
        <el-row style="height:100%" class="analysis-flex-column">
            <el-col :span="24" class="common-bg analysis-flex-column">
                <div style="height: 100%;overflow: hidden;">
                    <el-row :gutter="8" style="height: 100%;;overflow: hidden;">
                        <!-- 视频分享布局 -->
                        <el-col  :span="getSpan(0)" style="height: 100%;">
                            <el-row class="analysis-flex-column" style="height: 100%;">
                                <el-col :span="24" style="flex: 0;">
                                    <el-row :gutter="8" style="height: 100%;">
                                        <el-col style="height: 100%;" v-for="(item, index) in getDatas" :span="12">
                                            <div style="height: 100%;" :class="`main-bg pd-8 analysis-container-${index}`">
                                                <analysisTitle :class="`analysis-title-${index}`"
                                                    :ref="`title_${index}`" isCompare notPace isColumn
                                                    :sentenceMarkData="{...item,index:index+1}"
                                                    :syncScene="getSyncScene"
                                                    :wordsInfo="index?wordsInfo2:wordsInfo1"
                                                    ai
                                                    @markClick="(obj) => { markWords(index, obj,'clickMark') }">
                                                </analysisTitle>
                                            </div>
                                        </el-col>
                                    </el-row>
                                </el-col>
                                <el-col :span="24" style="height:60%;flex: 1; overflow: hidden;">
                                    <el-row :gutter="8" style="height: 100%;" class="pd-t8 pd-b8">
                                        <el-col v-for="(item, index) in getVideoDatas" :span="12" style="height: 100%;">
                                            <!-- 视频/音频 -->
                                            <contrastVideo ai style="height: calc(100%)" :sentenceMarkData="item" :ref="`video_${index}`"
                                                :name="`A${index + 1}`" isCompare>
                                            </contrastVideo>
                                        </el-col>
                                    </el-row>
                                </el-col>
                                <el-col :span="24" >
                                    <el-row :gutter="8" style="height: 100%;">
                                        <el-col v-for="(item,index) in getDatas" :span="12" style="height: 100%;" >
                                            <videoSlice 
                                                :ref="`videoSlice_${index}`" 
                                                class="main-bg brs-8"
                                                :targetType="targetType"
                                                :data="item"
                                                :type="aiType"
                                                :key="index"
                                                :readonly="!!shareId"
                                                :maxLength="80000"
                                                @paragraphData="(data)=>{onParagraphData(index,data)}"
                                                @otherChange="(data)=>{otherChange(index,data)}"
                                            ></videoSlice>
                                        </el-col>
                                    </el-row>
                                </el-col>
                            </el-row>
                        </el-col>
                        <el-col class="h100" :span="getSpan(1)">
                            <div class="ai-box main-bg brs-8 overflow_hidden pd-6 h100">
                                <ai ref="aiDom" class="h100" :type="aiType"
                                isCompare
                                :aiModel="1"
                                :askConfig="optionConfig"
                                notChangeModel
                                :targetType="targetType"
                                :shareId="shareId"
                                :sentenceMarkData="getAiData"
                                :flod="flod"
                                :moreConfigProps="moreConfigProps"
                                @toggle="onToggle"></ai>
                                <aiFold class="fold" :max="1" @change="val=>{flod = val}"></aiFold>
                            </div>
                        </el-col>
                    </el-row>
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script>
// import WordTableCountNum from '/src/components/analysis/wordTableCountNum.vue';
// import wordDiscern from "/src/views/commonComponent/analysisLayout/component/wordDiscern.vue";
import analysisMixin from '/src/mixins/analysisMixin';
import publicMixin from "/src/views/commonComponent/analysisLayout/mixin/publicMixin";
import analysisTitle from '/src/views/commonComponent/analysisLayout/component/analysisTitle.vue';
import contrastVideo from "/src/views/commonComponent/analysisLayout/contrast-video.vue";
import myUtils from "/src/utils/utils";
// import Pace from '/src/components/analysis/pace.vue';
import ai from '/src/components/analysis/ai/index.vue';
import aiFold from './aiFold.vue';
import videoSlice from './videoSlice/index.vue';
export default {
    components: {
        analysisTitle,
        contrastVideo,
        ai,
        aiFold,
        videoSlice
    },
    inject: [],
    mixins: [analysisMixin, publicMixin],
    provide(){
        return {
            contrastMain: this,
        }
    },
    props: {
        targetType: {
            type: String,
            default: ''
        },
        contrastId: {
            type: String,
            default: ''
        },
        httpRequest: {
            type: Function,
            default: null
        },
        // 运营 assistant
        // 违规 violation
        aiType: {
            type: String,
            default: 'assistant'
        },
        sentenceMarkData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        shareId: {
            type: String,
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
            // 对比分析1，敏感词开关
            wordsInfo1: {},
            // 对比分析2，敏感词开关
            wordsInfo2: {},
            flod:0,
            // 关键词汇总防抖
            debounceCountWords: null,
            dataMap: {},
            data1OtherData: null,
            data2OtherData: null,
        };
    },
    computed: {
        // 获取
        getDatas() {
            const { data1, data2 } = this.sentenceMarkData;
            return [data1,data2];
        },
        getSyncScene() {
            //syncScene:对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
            return this.sentenceMarkData.info?.syncScene
        },
        getVideoDatas(){
            const { data1, data2 } = this.sentenceMarkData;
            const {data1:d1,data2:d2} = this.dataMap;
            return [{
                ...data1,
                ...d1,
            }, {
                ...data2,
                ...d2,
            }];
        },
        getAiData(){
            return {
                ...this.sentenceMarkData,
                ...this.dataMap
            }
        },
        
        // 文件对比分析
        isFile() {
            const [data1, data2] = this.getDatas;
            let f1 = data1?.fileInfo?.fileType === 2;
            let f2 = data2?.fileInfo?.fileType === 2;
            return f1 || f2
        },
        optionConfig(){
            return {
                data1: this.data1OtherData,
                data2: this.data2OtherData,
            }
        },
        /**
         * @description 对比复盘场景隐藏“更多配置/更多选项”入口，避免在对比问答页继续展示。
         * @returns {Object}
         */
        moreConfigProps() {
            return {
                isCompare: true,
                hideMoreConfig: true
            }
        }
    },
    watch: {
    },
    methods: {
        otherChange(index,data){
            this.$set(this,`data${index+1}OtherData`,data);
        },
        setBrush(data,index){
            this.$nextTick(()=>{
                this.$refs[`videoSlice_${index}`]?.[0]?.brushChange({data1: data},'set')
            })
        },
        onParagraphData(index,data){
            this.$set(this.dataMap,`data${index+1}`,data);
        },
        getSpan(index){
            switch(index){
                case 0:
                    return this.flod >= 1 ? 0 :8;
                case 1: 
                    return this.flod === 0 ? 16 :24
            }
        },
        onToggle(val){
            this.$emit('toggle',val)
        },
        getAnalysisRef(index) {
            return this.getTextVnode(index);
        },
        // getVideoTime(index) {
        //     return this.getTitleVnode(index)?.getVideoTime || 0
        // },
        // onClickPace(index, value) {
        //     this.getTextVnode(index).setAnalysisChar(value);
        // },
        // 设置分析数据
        
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
        getTitleVnode(index) {
            return this.$refs?.[`title_${index}`]?.[0]
        },
        getTextVnode(index) {
            return this.$refs?.[`text_${index}`]?.[0]
        },
        getVideoVnode(index) {
            return this.$refs?.[`video_${index}`]?.[0]
        },
        // 视频加载完成回调事件
        onCanplay() {},
        // 获取关键词敏感词数量
        getWordsInfo(){
            const { data1,data2} = this.sentenceMarkData;
            [data1,data2]?.map(d=>d.wordsTabList)?.forEach((item,index)=>{
                let a = item?.find(d=>d.tabType === 1);
                let b = item?.find(d=>d.tabType === 2);
                this.$set(this[`wordsInfo${index+ 1}`],'sensitiveWordsNum',a?.num || 0)
                this.$set(this[`wordsInfo${index+ 1}`],'cruxWordsNum',b?.num || 0)
            })
        }
    },
    created() {
        this.debounceCountWords = myUtils.debounce(10, (index,type) => {
            this.countWords();
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
.ai-box{
    position: relative;
    &:hover .fold{
        opacity: 1;
        left: 0;
    }
}


</style>

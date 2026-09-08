<template>
    <div class="videoSlice-box">
        <Title>{{ getTitle }}</Title>
        <div class="main-bg pd-l10 pd-r10 mg-t6">
            <div class="slider-box" v-if="(isWordDiscern || isScrolling) && !readonly">
                <el-slider v-model="value" :class="{'text-slider': questionContent === 0}" range 
                :max="getMax" 
                @change="onChange" 
                :marks="getMarks"
                :format-tooltip="formatTooltip">
                </el-slider>
            </div>
            <div class="slice-text-list pd-t6">
                <div class="slice-text-item flex-jc-sb font-s12" v-for="(item, index) in getItems">
                    <div class="flex-ai-c">
                        <span class="dot-span" :style="{ background: colors[index] }"></span>
                        <el-tooltip v-if="item.tooltip" class="item" effect="dark" :content="item.tooltip"
                            placement="top">
                            <span>{{ item.label }}</span>
                        </el-tooltip>
                        <span v-else>{{ item.label }}</span>
                        <span class="mg-l4">{{ getItemsMap[item.key] }}</span>
                        <span v-if="item.unit">{{ item.unit }}</span>
                    </div>
                    <div v-if="item.key === 'paragraph' && !paragraphText && !readonly">
                        <el-popover placement="top-start" title="查看段落" width="500" trigger="click"
                            popper-class="slice-paragraph-dialog">
                            <div class="slice-paragraph-dialog-content">
                                <wordDiscern ref="wordDiscern" class="h100 main-bg overflow_hidden" isAi isCompare
                                    :readonly="readonly" notLocatingBar notTarde notSearch notExport name="ai" textShow :type="type"
                                    :sentenceMarkData="getParagraphData" @otherChange="paragraphOtherChange"
>
                                    <template #tabs="item">
                                        <slot name="tabs" v-bind="item"></slot>
                                    </template>
                                </wordDiscern>
                            </div>
                            <el-button slot="reference" type="text" class="text-xs" @click="onViewParagraph"
                                style="padding: 0;color: #FF6839">查看</el-button>
                        </el-popover>
                    </div>
                    <div v-if="item.key === 'allText' && !notAllText">
                        <el-popover placement="top-start" title="查看全文" width="500" trigger="click"
                            popper-class="slice-paragraph-dialog">
                            <div class="slice-paragraph-dialog-content">
                                <wordDiscern class="h100 main-bg brs-8 overflow_hidden" isCompare :readonly="readonly" isAi notLocatingBar notTarde
                                    notSearch notExport name="ai" textShow :sentenceMarkData="data" :type="type" @otherChange="allOtherChange">
                                    <template #tabs="item">
                                        <slot name="tabs" v-bind="item"></slot>
                                    </template>
                                </wordDiscern>
                            </div>
                            <el-button slot="reference" type="text" class="text-xs" style="padding: 0;color: #FF6839">查看</el-button>
                        </el-popover>
                    </div>
                </div>
            </div>
            <div v-if="!readonly && !isKuaishou" class="flex-jc-sb">
                <el-popover v-if="isWordDiscern && !isFile" ref="popover" placement="top-start" title="数据曲线"
                    :width="`${getWidth}`" trigger="click" @show="() => { showCurve(true) }" @hide="() => { showCurve(false) }"
                    popper-class="slice-paragraph-dialog">
                    <div class="slice-paragraph-dialog-curve pd-t28">
                        <curveData ref="curveData" v-if="showPopover" :sentenceMarkData="data" notLoad :isWebOnline="isWebOnline"
                            @brushChange="(data) => { brushChange(data, 'curveData') }">
                        </curveData>
                    </div>
                    <el-button slot="reference" type="text" style="color: #FF6839" class="text-xs">数据曲线</el-button>
                </el-popover>
                <el-popover ref="popoverDataBoard" v-if="isDataBoard && !notDataBoard" placement="top-start"
                    :title="getTitle" :width="`${getWidth}`" trigger="click" popper-class="slice-paragraph-dialog">
                    <div class="slice-paragraph-dialog-content pd-t28">
                        <TextDashboard :requestId="getId" notBtn :sentenceMarkData="data"></TextDashboard>
                    </div>
                    <el-button slot="reference" type="text" >{{ getTitle }}</el-button>
                </el-popover>
                <el-popover v-if="isDataCapture && !notDataCapture" ref="popoverDataCapture" placement="top-start"
                    :title="getTitle" :width="500" trigger="click" popper-class="slice-paragraph-dialog"
                    @show="getExistDataScreenshotList">
                    <div class="slice-paragraph-dialog-content">
                        <!-- <dataScreenshot :sentenceMarkData="data" readonly notBtn></dataScreenshot> -->
                        <div style="height: 300px;" class="overflow_hidden overflow_auto_y pd-8">
                            <div v-if="aIHtmlContent" v-html="aIHtmlContent"></div>
                            <div v-else class="flex-ai-c flex-jc-c h100">
                                <img src="@/assets/imgs/chartEmpty.png" style="width: 160px" alt="" srcset="">
                            </div>
                        </div>
                    </div>
                    <el-button slot="reference" type="text" >{{ getTitle }}</el-button>
                </el-popover>

            </div>
        </div>
    </div>
</template>

<script>
import myUtils from "/src/utils/utils";
import wordDiscern from '/src/views/commonComponent/analysisLayout/component/wordDiscern.vue';
import curveData from '/src/components/analysis/curveData/index.vue';
import Title from '/src/components/title/index.vue';
import aiTypeMixin from '@/mixins/aiTypeMixin';
import TextDashboard from '@/components/analysis/dataBoard/textDashboard.vue';
import dataScreenshot from '@/components/analysis/dataScreenshot/index.vue';
import {PLATFORM_TYPE_ENUM} from '@/enum';
export default {
    components: {
        wordDiscern,
        curveData,
        Title,
        TextDashboard,
        dataScreenshot
    },
    mixins: [aiTypeMixin],
    props: {
        targetType: {
            type: String,
            default: ''
        },
        max: {
            type: [Number, undefined],
            default: undefined
        },
        data: {
            type: Object,
            default: () => { return {} }
        },
        maxLength: {
            type: [Number, undefined],
            default: undefined
        },
        notAllText: {
            type: Boolean,
            default: false
        },
        notDataCapture: {
            type: Boolean,
            default: false
        },
        notDataBoard: {
            type: Boolean,
            default: false
        },
        paragraphText: {
            type: Boolean,
            default: false
        },
        dataDisplay: {
            type: Object,
            default: () => { return {} }
        },
        otherData:{
            type: Object,
            default: () => { return {} }    
        },
        readonly: {
            type: Boolean,
            default: false
        },
        questionContent:{
            type:Number,
            default: 0
        },
        textDataMap:{
            type:Object,
            default: ()=>{
                return {}
            }
        },
        scrollingTotalNum:{
            type: Number,
            default: 0
        }
    },
    data() {
        return {
            value: [],
            items: [
                { label: "段落开始时间", key: 'startTime', type: ['text', 'scrolling'] },
                { label: "段落结束时间", key: 'endTime', type: ['text', 'scrolling'] },
                { label: "全文字数", key: 'allText', type: ['text','textAssistant'] },
                { label: "输入字数", key: 'paragraph', type:  ['text','textAssistant']  },
                { label: "弹幕总条数", key: 'scrollingNum', type: 'scrolling', notMaxLength: true },
                {
                    label: "弹幕总字数", key: 'scrollingCharNum',
                    notMaxLength: true,
                    tooltip: '计算方法按照平均值计算，平均每条约40字: 昵称6个字,等级5个字,粉丝团5个字，新用户1个字,时间8个字,正文15个字',
                    type: 'scrolling'
                },
                // {label: "数据看板总字数",key: 'dataBoardText', type: 'dataBoard'},
                // {label: "AI数据识图总字数",key: 'dataCaptureText', type: 'dataCapture'},
            ],
            colors: ['#FF85C0', '#A0D911', '#1890FF', '#13C2C2'],
            paragraphNum: 0,
            setNumDebounce: myUtils.debounce(1000),
            drawData: {},
            aIHtmlContent: '',
            bulletScreenNum: 0,
            notMaxLength: false,
            sliceType: '',
            showPopover: false,
        };
    },
    computed: {
        getPlatform(){
            return this.data?.videoInfo?.PlatformType || this.data?.fileInfo?.platformType
        },
        isKuaishou() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.kuaishou
        },
        getId() {
            return this.data?.videoInfo?.VideoId || this.data?.fileInfo?.fileId
        },
        getSourceType() {
            return this.data?.videoInfo?.VideoId ? 0 : 1; // 源类型
        },
        isFile() {
            return !!this.data?.fileInfo
        },
        isFileTexT() {
            return this.data?.fileInfo?.fileType === 2;
        },
        isWebOnline() {
            return this.targetType === 'webOnline'
        },
        getWidth() {
            return document.body.clientWidth * 0.7;
        },
        getItems() {
            let type = this.isWordDiscern ? 'text' : this.type;
            let qt = this.questionContent ===  0 ? '' : 'textAssistant';
            return this.items?.filter((d) => {
                // 只读模式，不显示段落切片数据
                if(this.readonly && d.key === 'paragraph'){
                    return false
                }
                if(qt){
                    return d.type.includes(qt)
                }
                if (d.notMaxLength && this.notMaxLength) {
                    return false;
                };
                if (this.isFileTexT) {
                    return d.key === 'allText';
                } else {
                    return d.type.includes(type);
                }
            })
        },
        getList() {
            return this.data?.sentenceMarkList
        },
        getAllChatNum() {
            if(this.questionContent !== 0){
                let list =  this.textDataMap?.[this.questionContent];
                return this.getTextOtherLen( list?.map(text=>text?.length)?.reduce((a,b)=>a+b,0) || 0, list?.length, 'all');
            }
            return this.getTextOtherLen(this.data?.allTextCountNum || 0, this.getList?.length || 0, 'all');
        },

        getItemsMap() {
            const [a, b] = this.value
            return {
                startTime: this.getList[a]?.startHm,
                endTime: this.getList[b - 1]?.endHm,
                paragraph: this.paragraphNum.toLocaleString(),
                allText: this.getAllChatNum.toLocaleString(),
                scrollingNum: this.bulletScreenNum.toLocaleString(),
                scrollingCharNum: this.getScrollingCharNum().toLocaleString(),
            }
        },
        getMax() {
            if(this.questionContent !==  0){
                return this.textDataMap[this.questionContent]?.length || 0
            }else{
                return this.max || this.getList?.length 
            }
        },
        getParagraphData() {
            const [a, b] = this.value;
            if(this.questionContent !== 0){
                return {...this.data,
                    sentenceMarkList: this.getList,
                    startIndex: a,
                    endIndex: b,
                    questionContent: this.questionContent,
                    textList: this.textDataMap[this.questionContent]?.slice(a, b),
                    textIndex: [a,b],
                }
            }else{
                return {
                    ...this.data,
                    sentenceMarkList: this.getList.slice(a, b)
                }
            }
        },
        getMarks() {
            const label_0 =this.questionContent ===  0? this.getList[0]?.startHm : 0;
            const label_1 =this.questionContent ===  0? this.getList[this.getList.length - 1]?.endHm : this.getMax;
            return {
                0: {
                    style: {
                        color: '#1890ff',
                        fontSize: '10px',
                    },
                    label: label_0,
                },
                [this.getMax]: {
                    style: {
                        color: '#1890ff',
                        fontSize: '10px'
                    },
                    label: label_1,
                },
            }
        },
        getTitle() {
            if (this.readonly) {
                return '段落信息';
            }
            /*
                {{isFileTexT?'文件分析':'选择片段'}}
            */
            if (this.isFileTexT) {
                return '文件分析'
            } else if (this.isWordDiscern || this.isScrolling) {
                return '选择片段'
            } else if (this.isDataBoard) {
                return '数据看板'
            } else {
                return 'AI数据识图'
            }
        },
        getOtherLen(){
            /*
            自然时间：20字
            开始时间：38字
            在线人数：23字
            弹幕：12字
            成交人数：11字
            互动率：10字
            成交率：10字 
            {label: '开始时间',prop: 'startTime'},
            {label: '自然时间',prop: 'natureTime'},
            {label: '在线人数',prop: 'onlineNum'},
            {label: '弹幕数量',prop: 'barrageNum' },
            {label: '成交数量',prop: 'dealNum'},
            {label: '互动率',prop: 'interactionRate'},
            {label: '成交率',prop: 'dealRate'},
            */
           let keyNumTextLens = [
                {label: '自然时间',prop: 'natureTime', textNum: 20},
                {label: '开始时间',prop: 'startTime', textNum: 38},
                {label: '在线人数',prop: 'onlineNum', textNum: 23},
                {label: '弹幕数量',prop: 'barrageNum', textNum: 12},
                {label: '成交数量',prop: 'dealNum', textNum: 11},
                {label: '互动率',prop: 'interactionRate', textNum: 10},
                {label: '成交率',prop: 'dealRate', textNum: 10},
           ]
           let nums = keyNumTextLens?.filter(item=>{
            return !!this.otherData[item.prop]
           })?.map(item=>{ 
            return item?.textNum || 0;
           });
           return nums?.reduce((a,b)=>a+b,0) || 0;
        }
    },
    watch: {
        maxLength: {
            handler(val) {
                this.initData('watch');
            }
        },
        dataDisplay: {
            handler() {
                if (this.notMaxLength) { return }
                this.value = [0, this.getMax];
                this.setParagraphNum();
            },
            deep: true
        },
        // 显示条件配置
        otherData: {
            handler() {
                if (this.notMaxLength) { return }
                this.value = [0, this.getMax];
                this.setParagraphNum();
            },
            deep: true
        },
        questionContent: {
            handler(val,old){
                if(val !== old){
                    this.initData('questionContent');
                    this.paragraphNum = 0;
                }
            }
        }
    },
    methods: {
        getTextOtherLen(num,len, type){
            return num + ((this.getOtherLen||0) * (len|| 0));
        },
        allOtherChange(data){
            this.$emit('otherChange', {
                ...data
            })
        },
        paragraphOtherChange(data){
            // this.$emit('otherChange', {

            // })
        },
        notSlice(bl) {
            this.notMaxLength = bl;
            this.value = [0, this.getMax];
            this.setParagraphNum(false);
        },
        getScrollingCharNum(num) {
            /*
                dataDisplay:{}
                弹幕助手中的数据展示
                dateTime
                时间(0不勾，1勾)
                nickName
                昵称(0不勾，1勾)
                level
                用户等级(0不勾，1勾)
                fansLevel
                粉丝团等级(0不勾，1勾)
                isNew
                新icon(0不勾，1勾)
            */
            const { dateTime = 1, nickName = 1, level = 1, fansLevel = 1, isNew = 1 } = this.dataDisplay;
            const countNum = (num || this.bulletScreenNum) * (dateTime * 8 + nickName * 6 + level * 5 + fansLevel * 5 + isNew + 15);
            return countNum;
        },
        getExistDataScreenshotList() {
            this.aIHtmlContent = '';
            this.$httpBack.v2300.getExistDataScreenshotList({
                sourceType: this.getSourceType,
                sourceId: this.getId
            }).then(res => {
                this.aIHtmlContent = res.data?.map(d => {
                    return `<h3 class="mg-0 pd-b10">${d.title}</h3>
                    <p class="mg-0 pd-b10 font-s14 text-color2" style="white-space: pre-wrap;line-height: 22px;">${d.aiContent}</p>`;
                }).join('');
            })
        },
        onViewParagraph() {
            this.$nextTick(() => {
                setTimeout(() => {
                    let vNode = this.$refs?.wordDiscern?.[0] || this.$refs?.wordDiscern;
                    vNode?.refreshParagraph();
                }, 0)
            })
        },
        showCurve(...arg) {
            this.showPopover = arg[0];
            this.$nextTick(() => {
                this.$refs.curveData?.drawData?.(this.drawData, ...arg);
            })
        },
        brushChange(data, type) {
            const { data1 } = data;
            this.getTimeParagraph(...data1?.map(d => d * 1000), 1, type);
            this.$emit('brushChange', data)
            if (type === 'curveData') {
                this.$emit('curveDataChange')
            }
            this.$nextTick(() => {
                this.$refs.popover?.doClose()
            })
        },
        onChange(val) {
            this.setParagraphNum(true);
        },
        initData(type) {
            if (type === 'watch' || type === 'questionContent') {
                let countNum = 0;
                if (this.isWordDiscern) {
                    if(this.questionContent !== 0){
                        countNum = this.textDataMap?.[this.questionContent]?.map(d=>d.length)?.reduce((a,b)=>{
                            return a+b
                        },0)
                    }else{
                        countNum = this.getParagraphData?.sentenceMarkList?.map(d => d.charNum)?.reduce((a, b) => {
                            return a + b
                        }, 0)
                    }
                } else if (this.isScrolling) {
                    countNum = this.getScrollingCharNum(this.getParagraphData?.sentenceMarkList?.map(d => d.bulletScreenNum)?.reduce((a, b) => {
                        return a + b
                    }, 0));
                }
                if(type === 'questionContent'){
                    this.value = [0, this.getMax];
                    this.setParagraphNum(false,{notLoadText: true, defaultHide: true});
                }
                if (countNum <= this.maxLength) { return }
            }
            this.value = [0, this.getMax];
            this.setParagraphNum();
        },
        formatTooltip(value) {
            if(this.questionContent === 0){
                let endIndex = this.value[this.value.length - 1];
                if (value === endIndex) {
                    return this.data?.sentenceMarkList[endIndex - 1]?.endHm;
                } else {
                    return this.data?.sentenceMarkList[value]?.startHm
                }
            }else{
                return value;
            }
        },
        setParagraphNum(notify, opt) {
            this.setNumDebounce(() => {
                let [a, b] = this.value;
                let num = 0;
                if (this.isWordDiscern) {
                    for (let i = a; i < b; i++) {
                        let oldNum = num;
                        if(this.questionContent !== 0){
                            let list = this.textDataMap[this.questionContent] || [];
                            num += list[i]?.length || 0;
                        }else{
                            num += this.getList[i]?.charNum;
                        }
                        if (this.maxLength && num > this.maxLength && !this.isFileTexT && !this.notMaxLength) {
                            this.value = [a, i - 1];
                            num = oldNum;
                            if (notify) {
                                this.$notify.info(`切片输入字数超限。(模型支持${this.maxLength}字)`)
                            }
                            break;
                        }
                    }
                    this.paragraphNum = this.getTextOtherLen(num, (b - a), 'paragraph');
                } else if (this.isScrolling) {
                    for (let i = a; i < b; i++) {
                        let item = this.getList[i];
                        let oldNum = num;
                        num += item.bulletScreenNum;
                        let countCharNum = this.getScrollingCharNum(num);
                        if (this.maxLength && countCharNum > this.maxLength && !this.notMaxLength) {
                            this.value = [a, i - 1];
                            num = oldNum;
                            if (notify) {
                                this.$notify.info(`切片段落弹幕总字数超限。(模型支持${this.maxLength}字)`)
                            }
                            break;
                        }
                    }
                    this.bulletScreenNum = num;
                }
                this.$emit('change', this.value);
                this.$emit('paragraphData', {
                    ...this.getParagraphData,
                    option:opt,
                })
            })
        },
        getTimeParagraph(sTime, eTime, unit = 1) {
            if (typeof sTime === 'undefined' || typeof eTime === 'undefined') {
                this.value = [0, this.getList?.length];
                this.setParagraphNum();
                return
            }
            let list = this.data?.sentenceMarkList?.filter((d) => {
                const { startTime, endTime } = d;
                return (sTime * unit) <= endTime && (eTime * unit) >= startTime;
            });
            this.value = [list[0]?.currentSort - 1, list.pop()?.currentSort - 1];
            this.setParagraphNum();
            this.drawData = {
                data1: [Math.floor(sTime / 1000), Math.floor(eTime / 1000)]
            }
        }
    },
    created() {
        this.initData()
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
        this.initData()
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.slider-box {
    padding: 5px;
    padding-top: 0;
    padding-bottom: 10px;

    ::v-deep(.el-slider__runway) {
        margin: 6px 0;
    }
    .text-slider{
        ::v-deep(.el-slider__marks) {
            .el-slider__marks-text:first-child {
                left: 12px !important;
            }

            .el-slider__marks-text:last-child {
                left: calc(100% - 12px) !important;
            }

        }
    }
}

.videoSlice-box {
    padding: 10px;
    min-height: 180px;
    overflow: hidden;
}

.slice-text-item {
    padding: 5px 0;

    .dot-span {
        width: 8px;
        height: 8px;
    }
}

.slice-paragraph-dialog {

    &-curve{
        padding-right: 5px;
        overflow: hidden;
        overflow-y: auto;
        ::v-deep(.discernSearchContainerContent) {
            padding: 0 !important;
        }
    }
    &-content {
        max-height: 500px;
        padding-right: 5px;
        overflow: hidden;
        overflow-y: auto;
        ::v-deep(.discernSearchContainerContent) {
            padding: 0 !important;
        }
    }
}
</style>
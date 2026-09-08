<template>
    <div class="contrast-item-box analysis-flex-column"> 
        <analysisTitle ref="title" :isCompare="isCompare" :sentenceMarkData="sentenceMarkData" :wordsInfo="wordsInfo"
                       :targetType="targetType"
                       :enableSwappedTabs="enableSwappedTabs"
                       :tabsInContent="tabsInContent"
                       :textTypeConfig="textTypeConfig"
                       :activeName.sync="activeName"
                       @tabsClick="tabsClick"
                       @markClick="markClick" @pace-click="onPaceClick" @bulletscreen-click="onIsBulletScreen"
                       @deal-click="onDealClick" @interactionRate-click="onInteractionRateClick" @dealRate-click="onDealRateClick"
                       @sales-click="onSalesClick" @uv-click="onUVClick"
                       @qianchuanCost-click="onQianchuanCostClick">
            <template #anchor-left>
                <slot name="anchor-left"></slot>
            </template>
            <template #anchor-right>
                <slot name="anchor-right"></slot>
            </template>
            <template #anchor-after>
                <slot name="anchor-after"></slot>
            </template>
            <template #tabs-right>
                <slot name="tabs-right"></slot>
            </template>
            <template #time-top>
                <slot name="time-top"></slot>
            </template>
            <template v-if="!enableSwappedTabs" #word-control="{ videoInfo, fileInfo }">
                <slot name="word-control" v-bind="{ videoInfo, fileInfo }"></slot>
            </template>
        </analysisTitle>

        <!-- 视频/音频分析 -->
        <div class="videoAnalysisContainer pd-t6">
            <div v-if="!isFileText" class="videoAndTradeContainer mg-r12 video-content brs-8 h100 overflow_hidden" :style="'width: ' +  (flodWidth || videoWidth)">
                <div class="video-box brs-8 h100 overflow_hidden">
                    <!-- 视频/音频 -->
                    <contrastVideo ref="videoPlayer" :targetType="targetType" :sentenceMarkData="sentenceMarkData" @canplay="onCanplay"
                        @setDuration="setDuration"
                        @repairVideo="repairVideo"
                        @flodClick="flodClick"
                        @playStaus="playStaus"
                        @playerTimeupdate="onPlayerTimeupdate" @changeParagraphIndex="onPlayerParagraphIndex">
                        <template #video-bottom>
                            <slot name="video-bottom"></slot>
                        </template>
                    </contrastVideo>
                </div>
            </div>
            <wordDiscern ref="textDom" :cruxTypeMap="cruxTypeMap" :targetType="targetType" :name="name" :sentenceMarkData="sentenceMarkData"
                :isCompare="isCompare" @selectedText="updateSelectedText" @rightTickContextMenu="rightTickContextMenu"
                :notes="notes" @getTextList="getTextListHandle" :isAnnotation="isAnnotation" @onTask="taskHandler"
                @onParagraph="onParagraph" @playerReadied="onPlayerReadied" @playerPause="onPlayerPause" @createText="createText"
                @treeChange="treeChange" @textTypeChange="textTypeChange" @treeLoad="tradeTreeLoad" @updateMarkwords="onSetMarkwords" 
                @markwords="onMarkwords" @textLoad="textLoad" @setTextScriptInfo="setTextScriptInfo"
                :enableSwappedTabs="enableSwappedTabs"
                :notLocatingBar="activeName === 'productData'"
                @textTypeNameChange="(val) => { activeName = val }"
                @textTypeConfigChange="(val) => { textTypeConfig = val }"
                @pace-click="onPaceClick"
                @markClick="markClick"
                @bulletscreen-click="onIsBulletScreen"
                @deal-click="onDealClick"
                @interactionRate-click="onInteractionRateClick"
                @dealRate-click="onDealRateClick"
                @sales-click="onSalesClick"
                @uv-click="onUVClick"
                @qianchuanCost-click="onQianchuanCostClick">
                <template #top-tabs>
                    <div v-if="!notes && enableSwappedTabs && ((!ai && !readonly) || (readonly && versionType === VERSION_TYPE.PURE))" class="analysis-title-tab-row analysis-content-tab-row  pd-b6">
                        <el-radio-group class="toolbar-left-tabs analysis-title-tabs" v-model="activeName" @input="tabsClick" size="medium">
                            <el-radio-button label="text">{{isRecording?'分钟段落':'话术分析'}}</el-radio-button>
                            <el-radio-button label="productData" v-if="isRecording && isDouyin && (isWebOnline || isSelfAccount)">商品数据</el-radio-button>

                            <el-radio-button label="aiSharding" v-if="getReplayType !== 'replayShort'" style="position: relative">
                                <span>AI脚本拆解</span>
                                <img class="xi-icon" v-if="[1,2].includes(textTypeConfig?.aiShardingStatus)" src="@/assets/imgs/2_5_8/xi.png" alt="">
                                <img class="xi-icon" v-else src="@/assets/imgs/2_5_8/jian.png" alt="">
                            </el-radio-button>

                            <el-radio-button label="aiOptimal" v-if="getReplayType !== 'replayShort'">{{ versionType === VERSION_TYPE.PURE ? 'AI仿写本场' : '优化原文' }}</el-radio-button>

                            <el-radio-button label="scriptQuality" v-if="getReplayType !== 'replayShort' && (isRecording || isFileAnalysis) && !isSameIndustryRoom && canShowScriptMonitorTabs" style="position: relative">
                                <span>话术质检</span>
                                <div class="lv-dian" v-if="textTypeConfig?.scriptQualityUnread"></div>
                            </el-radio-button>

                            <el-radio-button label="scriptRestoration" v-if="getReplayType !== 'replayShort' && isRecording && !isSameIndustryRoom && canShowScriptMonitorTabs"
                                 style="position: relative">
                                <span>话术还原度</span>
                                <div class="lv-dian" v-if="textTypeConfig?.scriptRestorationUnread"></div>
                            </el-radio-button>

                            <el-radio-button label="interactionInspection" v-if="getReplayType !== 'replayShort' && isRecording && !isSameIndustryRoom && canShowScriptMonitorTabs" style="position: relative">
                                <span>互动巡检</span>
                                <div class="lv-dian" v-if="textTypeConfig?.interactionInspectionUnread"></div>
                            </el-radio-button>

                            
                            
                        </el-radio-group>
                        <div class="analysis-content-tab-right">
                            <slot name="top-tabs-right"></slot>
                        </div>
                    </div>
                </template>
                <template #tabs="item">
                    <slot name="tabs" v-bind="item"></slot>
                </template>
                <template #toolbar-right-after>
                    <slot name="toolbar-right-after"></slot>
                </template>
                <template v-if="enableSwappedTabs" #word-control="{ videoInfo, fileInfo }">
                    <slot name="word-control" v-bind="{ videoInfo, fileInfo }"></slot>
                </template>
            </wordDiscern>
            <div v-if="notes" style="width: 40%;">
                <notes ref="notesDom" 
                :sentenceMarkData="sentenceMarkData" 
                :notesData="notesMapData"
                @tabsClick="notesTabsClick"
                @getText="getText"
                @setTextData="setTextData"
                @quit="quitNotes"
                @notesEditer="notesEditer"
                :addTabs="addNotesTabs">
                    <template #annotation>
                        <div class="common-bg pd-6 h100 overflow_hidden overflow_auto_y">
                            <div style="background: rgba(0,119,255,0.05);" class="pd-16 font-s14 text-color2 brs-4">左键框选原文话术，右键可对框选的话术进行重点批注</div>
                            <annotationList :sentenceMarkData="sentenceMarkData"
                            :isPlayUrl="getPlayUrl"
                            @saveAnnotation="saveAnnotation"
                            :data="annotationList" @getList="getAnnotation"
                            @delAnnotation="delAnnotation" @toTextMark="toTextMark"></annotationList>
                        </div>
                    </template>
                </notes>
            </div>
        </div>
    </div>
</template>

<script>
import analysisTitle from './component/analysisTitle.vue';
import wordDiscern from './component/wordDiscern.vue';
import Sensitive from '/src/components/analysis/sensitive.vue'
import Keyword from '/src/components/analysis/keyword.vue'
import Pace from '/src/components/analysis/pace.vue'
import DiscernSearchContainer from '/src/components/DiscernSearchContainer/index.vue'
import videoPlayer from '/src/components/analysis/videoPlayer.vue';
import contrastVideo from './contrast-video.vue';
import LocatingBar from '/src/components/analysis/locatingBar.vue';
import wordsMixin from './mixin/wordsMixin';
import textMixin from './mixin/textMixin';
import commonMixin from './mixin/commonMixin'
import publicMixin from './mixin/publicMixin';
import notes from '@/components/notes/index.vue';
import annotationList from '@/components/Annotation/annotationList.vue';
import myUtils from '@/utils/utils'
import { VERSION_TYPE } from '@/enum'
export default {
    components: {
        analysisTitle,
        wordDiscern,
        videoPlayer,
        contrastVideo,
        LocatingBar,
        DiscernSearchContainer,
        Sensitive,
        Keyword,
        Pace,
        notes,
        annotationList
    },
    mixins: [commonMixin, publicMixin, wordsMixin, textMixin],
    props: {
        ai: {
            type: Boolean,
            default: false
        },
        // 暂时不用
        type: {
            type: String,
            default: ''
        },
        deafultWordsInfo: {
            type: Object,
            default: null
        },
        // 视频播放器宽度，如：28%
        videoWidth: {
            type: String,
            default: '18%'
        },
        targetType: {
            type:String,
            default: ''
        },
        isCompare: {
            type:Boolean,
            default: false
        },
        notes: {
            type: Boolean,
            default: false
        },
        enableSwappedTabs: {
            type: Boolean,
            default: false
        },
        tabsInContent: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            VERSION_TYPE,
            wordsInfo: {},
            flodWidth: '',
            activeName: 'text',
            textTypeConfig: {},
            addNotesTabs: [
                {label: '纠正和批注汇总',name: 'annotation',dataIndex: 3},
            ],
            notesMapData: {},
            isAnnotation: false,
            annotationList: []
        };
    },
    watch: {
        notes(val){
            this.restoreAnnotationHtmlOnNextTick();
        }
    },
    computed: {
        versionType() {
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
        isRecording() {
            return this.isVideoId
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
        },
        // 视频节点
        getVideoVnode() {
            return this.$refs?.videoPlayer
        },
        // 文本节点
        getTextVnode() {
            return this.$refs?.textDom
        },
        // 视频最大时长
        getVideoMaxTime(){
            return this.getTextVnode?.videoMaxTime
        }
    },
    methods: {
        tabsClick(val) {
            this.activeName = val
            this.toActiveText(val)
            this.$emit('activeNameChange', val)
        },
        getToolbarVnode() {
            return this.$refs?.textDom?.$refs?.DiscernSearchContainer?.$refs?.toolbar
        },
        textLoad(){
            this.getAnnotation();
        },
        playStaus(data){
            this.$nextTick(()=>{
                this.getTextVnode?.playStaus(data);
            })
        },
        // 任务处理
        taskHandler(taskData){
            const {type,data} = taskData || {};
            switch(type){
                case 'play':
                    this.getVideoVnode?.playTask(data);
                    break;
            }
        },
        // 判断笔记是否编辑(主要用于退出提示保存)
        isNotesEditer(callback,next){
            this.$nextTick(()=>{
                if(this.$refs?.notesDom){
                    this.$refs?.notesDom?.isNotesEditer(callback, next);
                }else{
                    callback(false);
                }
            })
        },
        // 笔记编辑
        notesEditer(val){
            this.$emit('notesEditer', val)
        },
        saveAnnotation(data){
            this.getTextVnode?.saveAnnotation(data)
        },
        // 获取批注
        getAnnotation(){
            // if(!this.isVideoId){return};
            this.$nextTick(()=>{
                this.$httpBack.analysisMark.getAnnotation({
                    sourceId: this.getId,
                    sourceType:  this.getSourceType
                }).then(res=>{
                    this.getTextVnode?.initAnnotationText();
                    this.annotationList = res.data?.map(item=>{
                        /*
                            markEndIndex
                            markStartIndex
                            paraphEndNo
                            paraphStartNo
                        */
                        const o = this.getTextVnode?.getAnnotationText({
                            startIndex: item.paraphStartNo,
                            endIndex: item.paraphEndNo,
                            startOffset: item.markStartIndex,
                            endOffset: item.markEndIndex,
                            ...item
                        });
                        item.text = o.text;
                        item.textTime = o.textTime;
                        item.startTime = o.startTime;
                        return item;
                    });
                    this.restoreAnnotationHtmlOnNextTick();
                })
            })
        },
        setTextScriptInfo(data){
            this.$emit('setTextScriptInfo',data)
        },
        // 设置笔记数据
        setTextData(data){
            this.$nextTick(()=>{
                this.getTextVnode?.setTextData(data.type,data.list,data?.status);
            })
        },
        /**
         * @description 延迟到当前文本容器完成刷新后重建批注映射并重新渲染批注样式，避免笔记模式切换时空映射覆盖已存在的批注显示。
         * @returns {void}
         */
        restoreAnnotationHtmlOnNextTick(){
            this.$nextTick(()=>{
                setTimeout(()=>{
                    this.restoreAnnotationHtml();
                }, 0);
            })
        },
        /**
         * @description 获取当前页面真正参与渲染的文本容器实例，避免笔记/批注切换时恢复动作打到旧的 DiscernSearchContainer 上。
         * @returns {Object|null}
         */
        getCurrentDiscernSearchContainer(){
            const wordDiscernVnode = this.getTextVnode;
            const containerVnode = wordDiscernVnode?.$refs?.DiscernSearchContainer;
            if(!containerVnode){
                return null;
            }
            const isNotesMatched = Boolean(containerVnode.notes) === Boolean(this.notes);
            const isAnnotationMatched = Boolean(containerVnode.isAnnotation) === Boolean(this.isAnnotation);
            return isNotesMatched && isAnnotationMatched ? containerVnode : null;
        },
        /**
         * @description 基于当前批注列表重建文本容器内的批注映射，再统一应用红线和“批”字样式。
         * @returns {void}
         */
        restoreAnnotationHtml(retryCount = 0){
            const textVnode = this.getCurrentDiscernSearchContainer();
            if(!textVnode){
                if(retryCount >= 5){
                    return;
                }
                setTimeout(()=>{
                    this.restoreAnnotationHtml(retryCount + 1);
                }, 30);
                return;
            }
            textVnode.initAnnotationText();
            this.annotationList.forEach(item=>{
                textVnode.getAnnotationText({
                    startIndex: item.paraphStartNo,
                    endIndex: item.paraphEndNo,
                    startOffset: item.markStartIndex,
                    endOffset: item.markEndIndex,
                    ...item
                });
            });
            textVnode.setAnnotationHtml();
        },
        // 设置批注html
        setAnnotationHtml(){
            this.restoreAnnotationHtmlOnNextTick();
        },
        // 删除批注
        delAnnotationHtml(data){
            this.$nextTick(()=>{
                this.getTextVnode?.delAnnotationHtml(data);
            })
        },
        // 删除批注
        delAnnotation(id){
            const item = this.annotationList.find(item=>item.id === id);
            // 剔除删除数据
            this.annotationList = this.annotationList.filter(item=>item.id !== id);
            this.delAnnotationHtml(item);
        },
        // 跳转批注原文
        toTextMark(data){
            const {paraphStartNo, markStartIndex, startTime} = data;
            this.$nextTick(()=>{
                this.getTextVnode?.toTextMark(data);
                this.getTextVnode?.selectDomeScrollIntoView(paraphStartNo,markStartIndex,'annotation');
            })
            if(this.getPlayUrl){
                this.getVideoVnode.ponlayerReadied(startTime,'click');
            }
        },
        // 退出笔记
        quitNotes(){
            this.$emit('quitNotes');
            this.notesTabsClick('');
        },
        // 笔记tabs切换
        notesTabsClick(name){
            this.isAnnotation = name === 'annotation';
            this.$emit('annotation', this.isAnnotation);
            if(this.isAnnotation){
                this.toActiveText('text');
            }else if(name === 'aiSharding'||name === 'text'){
                this.toActiveText(name);
            }
            this.restoreAnnotationHtmlOnNextTick();
        },
        // 跳转批注原文
        toActiveText(name){
            this.$nextTick(()=>{
                this.getTextVnode?.toActiveText(name);
            })
        },
        // 文本类型切换
        textTypeChange(val){

            this.$emit('textTypeChange',val)
        },
        // 折叠
        flodClick(flod){
            this.flodWidth = flod ? '33%' : '';
        },
        // 创建文本
        createText(data){
            this.getTextListHandle({
                ...data,
                charType: data.type,
                contentHtmlList: [],
            })
        },
        // 获取文本列表
        getTextListHandle(data={}){
            const {charType, contentHtmlList, type, status} = data;
            this.$nextTick(()=>{
                if(this.$refs?.notesDom){
                    this.$refs.notesDom?.setNotesText(charType,contentHtmlList, {type,status,setText: true});
                }
                this.notesMapData[charType] = {
                    list: contentHtmlList,
                    opt: {type,status}
                }
            })
        },
        // 获取文本
        getText(data){
            this.getTextVnode?.getText(data)
        },
    /**
     * 修复视频并通知父组件
     * @param {boolean} val - 修复视频的状态
     */
    repairVideo(val) {
        this.$emit('repairVideo', val);
    },

    /**
     * 点击语速分析时的处理逻辑
     * @param {number} val - 语速分析的值
     */
    onPaceClick(val) {
        this.getTextVnode.setAnalysisChar(val);
    },
    onIsBulletScreen(val){
        this.getTextVnode.setIsBulletScreen(val);
    },
    onDealClick(val){
        this.getTextVnode.setDeal(val);
    },
    onInteractionRateClick(val){
        this.getTextVnode.setInteractionRate(val);
    },
    onDealRateClick(val){
        this.getTextVnode.setDealRate(val);
    },
    onSalesClick(val) {
        this.getTextVnode?.setSales(val);
    },
    onUVClick(val) {
        this.getTextVnode?.setUV(val);
    },
    onQianchuanCostClick(val) {
        this.getTextVnode?.setQianchuanCost(val);
    },
    /**
     * 标注/取消标注按钮回调
     */
    markClick() {
        // 设置关键词列表
        this.markwords(this.wordsInfo);
    },

    /**
     * 更新 wordsInfo 并调用 markwords 方法
     * @param {Object} obj - 新的 wordsInfo 对象
     */
    onMarkwords(obj) {
        // 使用 Vue 的 $set 方法确保响应式更新
        this.$set(this, 'wordsInfo', obj);
        this.markwords(this.wordsInfo);
    },

    /**
     * 更新 wordsInfo 并调用 setMarkwords 方法
     * @param {Object} obj - 新的 wordsInfo 对象
     */
    onSetMarkwords(obj) {
        // 使用 Vue 的 $set 方法确保响应式更新
        this.$set(this, 'wordsInfo', obj);
        this.setMarkwords(this.wordsInfo);
    },

    /**
     * 设置播放器进度
     * @param {number} second - 播放器进度（秒）
     */
    onPlayerReadied(second, type) {
        this.getVideoVnode.ponlayerReadied(second, type);
    },

    /**
     * 视频停止播放
     */
    onPlayerPause() {
        this.getVideoVnode?.playerPause();
    },

    /**
     * 文本选中段落改变时的处理逻辑
     * @param {number} index - 段落索引
     */
    onParagraph(index) {
        this.getVideoVnode?.setVideoParagraphIndex(index);
    },

    /**
     * 视频加载完成后的回调事件
     */
    onCanplay() {},

    /**
     * 设置视频时长和字符数量
     * @param {Object} param - 包含视频时长和数学时长的对象
     * @param {number} param.videoDuration - 视频时长（秒）
     * @param {number} param.MathDuration - 数学时长（秒）
     */
    setDuration({ videoDuration, MathDuration }) {
        this.$nextTick(() => {
            this.getTextVnode?.marksTitle(MathDuration);
            this.$refs.title.setCharAndTime(videoDuration, this.sentenceMarkData?.allCharCountNum);
            if (this.enableSwappedTabs) {
                this.getTextVnode?.setCharAndTime?.(videoDuration, this.sentenceMarkData?.allCharCountNum)
            }
        });
    },

    /**
     * 视频改变段落时的处理逻辑
     * @param {number} index - 段落索引
     */
    onPlayerParagraphIndex(index) {
        this.getTextVnode?.selectDomeScrollIntoView(index,undefined,'video-only');
    },

    /**
     * 播放器进度回调
     * @param {number} time - 当前播放时间（秒）
     */
    onPlayerTimeupdate(time) {
        this.getTextVnode?.setVideoCurrentTime(time);
    },

    /**
     * 整理关键字
     * @param {Object} cruxTypeMap - 关键字类型映射
     */
    tidyCountWords(cruxTypeMap) {
        this.$nextTick(() => {
            this.getTextVnode?.tidyCountWords(cruxTypeMap);
        });
    },

    /**
     * 添加视频修复成功的事件监听
     */
    addReEncodeSuccess() {
        this.$CSharpNotify.addTask('reEncodeSuccess', (res, resolve) => {
            this.getVideoVnode?.reEncodeSuccess();
            this.$notify({
                title: '修复成功',
                message: '视频修复成功',
                duration: 3000,
                type: 'success'
            });
        });
    },

    /**
     * 添加视频修复失败的事件监听
     */
    addReEncodeError() {
        this.$CSharpNotify.addTask('reEncodeFail', (res, resolve) => {
            this.getVideoVnode?.reEncodeError();
            this.$notify({
                title: '修复失败',
                message: '视频修复失败',
                duration: 3000,
                type: 'error'
            });
        });
    },

    /**
     * 检查视频修复是否正在进行，如果正在进行则提示用户是否中断修复
     * @param {Function} callback - 回调函数，用于处理修复状态
     */
    isRepairStop(callback) {
        if (this.getVideoVnode) {
            this.getVideoVnode?.isRepairStop(callback);
        } else {
            callback(true);
        }
    }
},
    created() {
        this.$nextTick(() => {
            if (this.deafultWordsInfo) {
                this.getTextVnode?.setWordsInfo(this.deafultWordsInfo)
            }
        });
        // 添加频错误修复事件监听
        this.addReEncodeSuccess();
        // 添加视频错误修复事件监听
        this.addReEncodeError()
    },
     mounted() {
        // 对比分析不进行类型加载
        if (this.isCompare) { return }
        this.$nextTick(async () => {
            await this.getCruxWordType((cruxTypeMap)=>{
                this.tidyCountWords(cruxTypeMap)
            });
            this.initPayerIndexMap();
            this.setPayerIndexMap(this.sentenceMarkList);
        });
        
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
};
</script>
<style scoped>
.discernContainer {
    /* margin-left: 12px; */
    box-sizing: border-box;
    flex-grow: 1;
    width: 0;
    position: relative;
    display: flex;
    flex-direction: column;
}

.videoAnalysisContainer {
    display: flex;
    margin-top: 0;
    flex-grow: 1;
    min-height: 0;
}

.contrast-item-box {
    display: flex;
    flex-direction: column;
    height: 100%;
    /* min-height: 400px; */
    /* >div{
        flex: 1;
    } */
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

    .xi-icon {
        position: absolute;
        right: 0;
        top: -15px;
        width: 22px;
        height: 20px;
    }

    .lv-dian {
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
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
}

.analysis-content-tab-right {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-shrink: 0;
    margin-left: auto;
}

.analysis-content-tab-row {
}

.analysis-content-tab-row::-webkit-scrollbar {
    display: none;
}
</style>

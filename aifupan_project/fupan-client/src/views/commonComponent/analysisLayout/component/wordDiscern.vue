<template>
    <div class="discernContainer">
        <AiBtn v-if="isAi && isVideoId && !monitorOnlyMode && !hideAiBtn" :isCompare="isCompare" :notTextType="isCompare || isViolation" :sentenceMarkData="sentenceMarkData" :aiCueType="getAiCueTypeKeyValue"
        :barrageNum="sentenceMarkData.totalBarrageNum" :hide="{
            onlineNum:isViolation,
            natureTime:!isViolation&&!isAssistant
        }" @change="textTypeChange" @other-change="otherChange"></AiBtn>
        <slot name="top-tabs"></slot>
        <div v-if="isPlayUrl && isLoactingBar" :class="{ 'brs-t8': isPlayUrl }" class="pd-l12 pd-r12 pd-t12 main-bg">
            <!-- 定位条 -->
            <LocatingBar v-model="sliderValue" :marks="marksObject" :readonly="readonly" :isCompare="isCompare" :max="videoMaxTime" :tooltip="tooltip"
                :loading="silderActive" @change="sliderChange"></LocatingBar>
        </div>
        <!-- 文案 -->
        <div style="flex:1;overflow: hidden;">
            <div class="analysis-flex-column">
                <slot name="content-before"></slot>
                <slot name="content">
                    <DiscernSearchContainer v-if="!hideContent" class="flex-column main-bg pd-8 h100" ref="DiscernSearchContainer" :class="!(isPlayUrl) ? 'brs-8' : 'brs-b8'"
                        :textData="sentenceMarkData"
                        :wordsInfo="wordsInfo" 
                        :isVideo="isPlayUrl"
                        :isUpload="!isVideoId"
                        :isRecording="isVideoId"
                        :isFileText="isFileText"
                        :name="getName"
                        :notes="notes"
                        :videoCurrentTime="videoCurrentTime"
                        :currentParagraphIndex.sync="currentParagraphIndex"
                        :isCompare="isCompare"
                        :targetType="targetType"
                        :notTarde="notTarde"
                        :notSearch="notSearch"
                        :notExport="notExport"
                        :textShow="textShow"
                        :isAi="isAi"
                        :readonly="readonly"
                        :isAnnotation="isAnnotation"
                        :monitorOnlyMode="monitorOnlyMode"
                        :enableSwappedTabs="enableSwappedTabs"
                        @selectedText="selectedTextHandler"
                        @exportTxt="exportTxt"
                        @setTextScriptInfo="setTextScriptInfo"
                        @playerReadied="playerReadied"
                        @rightTickContextMenu="rightTickContextMenu"
                        @playerPause="playerPause" 
                        @treeChange="treeChange" 
                        @treeLoad="tradeTreeLoad"
                        @changeTextType="changeTextType"
                        @textTypeConfigChange="(val) => $emit('textTypeConfigChange', val)"
                        @pace-click="(val) => $emit('pace-click', val)"
                        @markClick="(val) => $emit('markClick', val)"
                        @bulletscreen-click="(val) => $emit('bulletscreen-click', val)"
                        @deal-click="(val) => $emit('deal-click', val)"
                        @interactionRate-click="(val) => $emit('interactionRate-click', val)"
                        @dealRate-click="(val) => $emit('dealRate-click', val)"
                        @sales-click="(val) => $emit('sales-click', val)"
                        @uv-click="(val) => $emit('uv-click', val)"
                        @qianchuanCost-click="(val) => $emit('qianchuanCost-click', val)"
                        @onTask="taskHandler"
                        @textLoad="textLoad"
                        @getText="getText"
                        @getAiReport="getAiReport"
                        @confirmRead="handleConfirmRead"
                        @enableMonitor="handleEnableMonitor"
                        @scriptRestorationConfirmed="handleScriptRestorationConfirmed">
                        <template #toolbar-left>
                            <slot name="toolbar-left"></slot>
                        </template>
                        <template #toolbar-right-before>
                            <slot name="toolbar-right-before"></slot>
                        </template>
                        <template #toolbar-right-after>
                            <slot name="toolbar-right-after"></slot>
                        </template>
                        <template #word-control>
                            <slot name="word-control" :videoInfo="videoInfo" :fileInfo="fileInfo"></slot>
                        </template>
                        <template #textParagraph-after="slotProps">
                            <slot name="textParagraph-after" v-bind="slotProps"></slot>
                        </template>
                    </DiscernSearchContainer>
                </slot>

                <slot name="content-after"></slot>
                <!-- 是否插入tabs，对比分析需要在外层插入tabs功能 -->
                <slot name="tabs" v-if="!questionContent" v-bind="{ sentenceMarkList, DiscernSearchContainer: $refs.DiscernSearchContainer || {} }"></slot>
            </div>
        </div>
    </div>
</template>

<script>
import DiscernSearchContainer from '/src/components/DiscernSearchContainer/index.vue'
import videoPlayer from '/src/components/analysis/videoPlayer.vue'
import LocatingBar from '/src/components/analysis/locatingBar.vue';
import commonMixin from '../mixin/commonMixin';
import myUtils from '../../../../utils/utils';
import wordsMixin from '../mixin/wordsMixin';
import AiBtn from '/src/components/DiscernSearchContainer/aiBtn.vue';
import {getSourceData} from '@/utils/common';
import aiTypeMixin from '@/mixins/aiTypeMixin.js';
import renderAiResponseContent, { AI_RENDER_MODE_MAP } from "@/components/analysis/ai/common/aiContentRenderParser";
import {VERSION_TYPE} from "@/enum";
export default {
    mixins: [commonMixin, wordsMixin, aiTypeMixin],
    components: {
        videoPlayer,
        LocatingBar,
        DiscernSearchContainer,
        AiBtn
    },
    props: {
        analysisChar:{
            type:Boolean,
            default: false
        },
        cruxTypeMap: {
            type: Object,
            default: null
        },
        deafultWordsInfo: {
            type: Object,
            default: null
        },
        targetType: {
            type:String,
            default: ''
        },
        enableSwappedTabs: {
            type: Boolean,
            default: false
        },
        isCompare: {
            type:Boolean,
            default: false
        },
        notLocatingBar: {
            type: Boolean,
            default: false
        },
        notTarde:{
            type:Boolean,
            default: false
        },
        notSearch:{
            type:Boolean,
            default: false
        },
        notExport:{
            type: Boolean,
            default: false
        },
        textShow: {
            type: Boolean,
            default: false
        },
        isAi: {
            type:Boolean,
            default: false
        },
        notes: {
            type: Boolean,
            default: false,
        },
        hideContent:{
            type: Boolean,
            default: false
        },
        isAnnotation:{
            type: Boolean,
            default: false
        },
        monitorOnlyMode: {
            type: Boolean,
            default: false
        },
        hideAiBtn: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            // 关键词/敏感词数据
            wordsInfo: {
                wordsList: [], // 关键词/敏感词列表
                cruxWordsNum: 0, // 关键词个数
                sensitiveWordsNum: 0, // 敏感词个数
                markCrux: false, // 是否标注关键词
                markSensitive: false, // 是否标注敏感词
            },
            silderActive: false,
            videoMaxTime: 0,
            sliderValue: 0,
            marks: [],
            currentParagraphIndex: -1,
            videoCurrentTime: 0,
            cruxTypeMap_: null,
            // ai话术助手中的提问内容：0分钟段落，1AI脚本拆解，2优化原文 3AI数据诊断,4话术质检报告,5互动巡检报告,6话术还原度报告
            questionContentMap:{
                text: 0,
                aiSharding: 1,
                aiOptimal:2,
                aiDiagnose:3,
                scriptQuality: 4,
                interactionInspection: 5,
                scriptRestoration: 6,
            },
            questionContentNumMap:{
                0:'text',
                1:'aiSharding',
                2:'aiOptimal',
                3:'aiDiagnose',
                4:'scriptQuality',
                5:'interactionInspection',
                6:'scriptRestoration'
            },
            questionContentLabelMap:{
                0:'分钟段落',
                1:'AI脚本拆解',
                2:'优化原文',
                3:'AI数据诊断',
                4: '话术质检报告',
                5: '互动巡检报告',
                6: '话术还原度报告'
            },
            questionContent: 0,
            diagnosisPopVisibleClick: false,
            scriptMonitorPendingUnread: {
                scriptQuality: false,
                interactionInspection: false,
                scriptRestoration: false
            }
        };
    },
    computed: {
        getName() {
            return this.name ? `${this.name}-` : ''
        },
        // 将marks转换为对象
        marksObject() {
            return Object.fromEntries(this.marks);
        },
        // vnode节点
        getTextVnode() {
            return this.$refs?.DiscernSearchContainer
        },
        getCruxTypeMap(){
            return {
                ...this.cruxTypeMap_,
                ...this.cruxTypeMap
            };
        },
        isLoactingBar(){
            return !this.notLocatingBar
        },
        versionType() {
            return this.$store.getters.getVersionType
        }
    },
    watch: {},
    methods: {
        setCharAndTime(time,char){
            this.$refs?.DiscernSearchContainer?.setCharAndTime?.(time, char)
        },
        getScriptMonitorSceneType() {
            const { sourceType } = getSourceData(this.sentenceMarkData)
            if (Number(sourceType) !== 1) return 0
            const querySceneType = this.$route?.query?.sceneType
            let sceneType = querySceneType !== undefined && querySceneType !== null && querySceneType !== '' ? Number(querySceneType) : NaN
            if (![1, 2].includes(sceneType)) {
                const fromData = this.sentenceMarkData?.sceneType
                sceneType = fromData !== undefined && fromData !== null && fromData !== '' ? Number(fromData) : NaN
            }
            if (![1, 2].includes(sceneType)) {
                const fileType = this.sentenceMarkData?.fileInfo?.fileType ?? this.sentenceMarkData?.fileInfo?.FileType
                if (Number(fileType) === 2) {
                    sceneType = 2
                } else if (fileType !== undefined && fileType !== null && fileType !== '') {
                    sceneType = 1
                }
            }
            if (![1, 2].includes(sceneType)) {
                const path = String(this.$route?.path || '')
                if (path.includes('/uploadText/')) {
                    sceneType = 2
                } else {
                    sceneType = 1
                }
            }
            return sceneType
        },
        playStaus(data){
            this.$nextTick(()=>{
                this.getTextVnode?.playStaus(data);
            })
        },
        textLoad(){
            this.$emit('textLoad')
        },
        setTextScriptInfo(data){
            this.$emit('setTextScriptInfo',data)
        },
        // 跳转批注原文
        toActiveText(type){
            this.$nextTick(()=>{
                this.getTextVnode?.toActiveText(type);
            })
        },
        toTextMark(data){
            this.$nextTick(()=>{
                this.getTextVnode?.toTextMark(data);
            })
        },
        // 任务处理
        taskHandler(data){
            this.$emit('onTask',data)
        },
        // 设置批注html
        setAnnotationHtml(){
            this.$nextTick(()=>{
                this.getTextVnode?.setAnnotationHtml();
            })
        },
        saveAnnotation(data){
            this.$nextTick(()=>{
                this.getTextVnode?.saveAnnotation(data)
            })
        },
        // 删除批注
        delAnnotationHtml(data){
            this.$nextTick(()=>{
                this.getTextVnode?.delAnnotationHtml(data);
            })
        },
        // 获取批注
        getAnnotationText(config){
            return this.getTextVnode?.getAnnotationText(config)
        },
        initAnnotationText(data){
            this.getTextVnode?.initAnnotationText();
        },
        // 设置文本数据
        setTextData(type,list, status){
            let t = this.questionContentNumMap[type] || type;
            this.getTextVnode?.setTextData(t,list);
            if(typeof status !== 'undefined'){
                this.getTextVnode?.setTextStatus(t,status);
            }
        },
        // 获取AI脚本拆解
        getText(data){
            const { type, status, event = 'get'} = data;
            const { sourceType, sourceId } = getSourceData(this.sentenceMarkData);
            const typeNum = this.questionContentMap[type] ?? type;
            let  params = { sourceType, sourceId, type: typeNum };

            if ((this.versionType === VERSION_TYPE.PURE || this.$store.getters.isPure) && [4, 5, 6].includes(Number(typeNum))) {
                return
            }

            // 话术质检/互动巡检/话术还原度：点击「生成本场报告」时调 triggerReport 触发生成
            if ([4, 5, 6].includes(Number(typeNum)) && event === 'click') {
                return this.fetchScriptMonitorDetail(data, { sourceType, sourceId, typeNum });
            }

            // 初始状态为'',则不触发生成，直接获取信息，如果状态为0，或者3，表示未生成，或者生成失败。则重新获取
            if(status == 'create' || event ==='click'){
                if(this.readonly){return}
                this.createText(data,params);
            }else{
                this.$httpBack.words.getVideoContent(params).then(res=>{
                    if(res.code == 0 ){
                        const {contentStatus, videoFileContentList} = res.data;
                        this.getTextVnode?.setTextStatus(type,contentStatus);
                        if(contentStatus === 2){
                            const contentList =[].concat(...videoFileContentList.map(item=>{
                                return item.contentList
                            }));
                            const contentMd = contentList.filter(t => !!t).join('\n')
                            const contentHtml = renderAiResponseContent(contentMd || '', {
                                parserMode: AI_RENDER_MODE_MAP.MD_TAG,
                                forceStyle: true,
                                upgradePlainTable: true
                            }).html

                            this.setTextData(type, contentMd);
                            // 生成两份数据，一份纯text一份md文档转换过后的。
                            this.$emit('getTextList',{
                                type: this.questionContentMap[type] ?? type,
                                charType: this.questionContentNumMap[type]??type,
                                list: contentList.map(text=>{
                                    return text.replace(/\*\*/gm,'');
                                }),
                                status: contentStatus,
                                contentHtmlList: [contentHtml]
                            })
                        }else if(contentStatus == 1){
                            setTimeout(()=>{
                                this.getText(data);
                                // 设置为生成中状态
                                this.getTextVnode?.setTextStatus(this.questionContentNumMap[type] ?? type,1);
                            }, 5000)
                        }else if(contentStatus == 0 || contentStatus == 3){
                            if(this.readonly){return}
                            // this.createText(data,params);
                        }
                    }
                }).catch((err) => {
                    if(err.code == 3001){return}
                    setTimeout(()=>{
                        this.getText(data);
                    },5000)
                })
            }
        },
        // 话术质检/互动巡检/话术还原度：点击「生成本场报告」→ 调详情 API
        // 点击「生成本场XX报告」→ 调 triggerReport 触发生成
        async fetchScriptMonitorDetail(data, { sourceType, sourceId, typeNum }) {
            const { type } = data;
            if (this.readonly || this.targetType === 'webOnline') return
            if (this.versionType === VERSION_TYPE.PURE || this.$store.getters.isPure) return
            if (!this.$httpBack?.scriptMonitor?.triggerScriptMonitorReport) return;
            const monitorTypeMap = { 4: 0, 5: 2, 6: 1 };
            const monitorType = monitorTypeMap[typeNum];
            if (monitorType === undefined) return;
            const labelMap = { 4: '话术质检报告', 5: '互动巡检报告', 6: '话术还原度报告' };
            this.$message.success(`正在生成${labelMap[typeNum] || '报告'}，请稍候...`);
            try {
                const sceneType = this.getScriptMonitorSceneType()
                const res = await this.$httpBack.scriptMonitor.triggerScriptMonitorReport({
                    sourceType,
                    sceneType,
                    sourceId,
                    monitorType
                });
                if (res?.code === 0) {
                    // 触发生成成功，状态置为 1（生成中）
                    this.getTextVnode?.setTextStatus(type, 1);
                    if (this.scriptMonitorPendingUnread && Object.prototype.hasOwnProperty.call(this.scriptMonitorPendingUnread, type)) {
                        this.scriptMonitorPendingUnread[type] = true
                    }
                    // 5 秒后刷新状态
                    setTimeout(() => {
                        this.refreshScriptMonitorStatus(type);
                    }, 5000);
                } else if (res?.code === 70005) {
                    this.$message.warning(res?.msg || '请先确认标准直播稿');
                    if (Number(monitorType) === 1) {
                        this.getTextVnode?.openScriptRestorationDrawer?.()
                    }
                } else {
                    this.getTextVnode?.setTextStatus(type, 3);
                    this.$message.warning(res?.msg || '生成失败');
                }
            } catch (e) {
                this.getTextVnode?.setTextStatus(type, 3);
            }
        },
        // 进入详情页时批量查询三种报告状态（不获取详情）
        async loadScriptMonitorStatuses() {
            if (this.versionType === VERSION_TYPE.PURE || this.$store.getters.isPure) return
            if (!this.sentenceMarkData) return;
            const { sourceType, sourceId } = getSourceData(this.sentenceMarkData);
            if (!sourceId) return;
            if (!this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return;
            try {
                const sceneType = this.getScriptMonitorSceneType()
                const anchorInfo = this.sentenceMarkData?.anchorInfo || {}
                const secUid = anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId
                const payload = { sourceType, sceneType, sourceId }
                if (secUid) payload.secUid = secUid
                const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus(payload);
                if (res?.code !== 0 || !Array.isArray(res?.data?.monitors)) return;
                const item = res.data?.monitors;
                if (!item.length) return;
                const qc = item.find(a => a.monitorType === 0) || {};
                const restore = item.find(a => a.monitorType === 1) || {};
                const inspect = item.find(a => a.monitorType === 2) || {};

                this.getTextVnode?.setTextStatus('scriptQuality', Number(qc.status ?? 0));
                this.getTextVnode?.setMonitorEnabled?.('scriptQuality', qc.monitorEnabled);
                const qcReportId = qc.reportId ? String(qc.reportId) : null
                this.getTextVnode?.setReportId('scriptQuality', qcReportId);
                this.getTextVnode?.setUnread?.('scriptQuality', !!(qcReportId && Number(qc?.isRead) === 0))
                this.getTextVnode?.setTextStatus('interactionInspection', Number(inspect.status ?? 0));
                this.getTextVnode?.setMonitorEnabled?.('interactionInspection', inspect.monitorEnabled);
                const inspectReportId = inspect.reportId ? String(inspect.reportId) : null
                this.getTextVnode?.setReportId('interactionInspection', inspectReportId);
                this.getTextVnode?.setUnread?.('interactionInspection', !!(inspectReportId && Number(inspect?.isRead) === 0))
                this.getTextVnode?.setTextStatus('scriptRestoration', Number(restore.status ?? 0));
                this.getTextVnode?.setMonitorEnabled?.('scriptRestoration', restore.monitorEnabled);
                const restoreReportId = restore.reportId ? String(restore.reportId) : null
                this.getTextVnode?.setReportId('scriptRestoration', restoreReportId);
                this.getTextVnode?.setUnread?.('scriptRestoration', !!(restoreReportId && Number(restore?.isRead) === 0))
            } catch (e) {
                // 静默处理
            }
        },
        // 创建文本
        createText(data,params){
            const { type } = data;
            const typeNum = this.questionContentMap[type] ?? type;
            this.$message.success(`获取${this.questionContentLabelMap[typeNum]}中,需排队等待,请稍后...`);
            const httpServer = this.targetType === 'online' ? this.$httpBack.words.generateVideoContent : this.$httpClient.anchorvideo.generateVideoContent;
            httpServer(params).then(res=>{
                if(res.code == 0){
                    // 设置为生成中状态
                    this.getTextVnode?.setTextStatus(this.questionContentNumMap[type] ?? type,'1');
                    this.$emit('createText',{type:this.questionContentNumMap[type] ?? type, status: '1'});
                    // 5秒过后自动获取一次文本
                    setTimeout(()=>{
                        this.getText({type, status: 1});
                    }, 5000)
                }
            }).catch(() => {
                this.getTextVnode?.setTextStatus(this.questionContentNumMap[type] ?? type,'3');
            })
        },
        // ai页面用，多一步设置操作
        textTypeChange(val){
            this.getTextVnode?.setTextType(val);
            this.changeTextType(val);
        },
        getAiReport(data){
            if(this.sentenceMarkData?.fileInfo?.fileType === 0) return; // 文件分析
            if(!["replayAll",'replaySection'].includes(this.getTextVnode?.getReplayType)) return; //ai复盘 非整场和切片
            if (this.versionType === VERSION_TYPE.PURE) return; //纯录制版
            if (this.isCompare) return; //对比
            if (this.isAi) return;
            const {type} = data;
            const {sourceId:videoId } = getSourceData(this.sentenceMarkData);
            if (this.readonly) return
            this.$httpBack.words.getDataDiagnosisStatus({videoId}).then(res => {
                if (res.code === 0) {
                    const {qaStatus, content, isRead, id} = res.data || {};
                    this.getTextVnode?.setTextStatus(type, qaStatus);
                    if (!this.diagnosisPopVisibleClick) {
                        this.getTextVnode?.changeDiagnosisPopVisible((qaStatus === 2 && isRead === 0)||[undefined,null].includes(qaStatus), id)
                        this.diagnosisPopVisibleClick = true
                    }
                    if (qaStatus === 2) {
                        const contentHtml = [renderAiResponseContent(content||'', {
                            parserMode: AI_RENDER_MODE_MAP.MD_TAG,
                            forceStyle: true,
                            upgradePlainTable: true
                        }).html]
                        this.setTextData(type, contentHtml);
                        this.$emit('getTextList',{
                            type: this.questionContentMap[type] ?? type,
                            charType: this.questionContentNumMap[type]??type,
                            list: contentHtml,
                            status: qaStatus,
                            contentHtmlList: contentHtml
                        })
                    }
                }
            }).catch((err) => {

            })
        },
        // 详情页面用
        changeTextType(val){
            if ((this.versionType === VERSION_TYPE.PURE || this.$store.getters.isPure) && ['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(val)) {
                this.getTextVnode?.setTextType?.('text')
                this.questionContent = this.questionContentMap.text
                this.$emit('textTypeChange', this.questionContentMap.text)
                this.$emit('textTypeNameChange', 'text')
                return
            }
            this.questionContent = this.questionContentMap[val];
            this.$emit('textTypeChange', this.questionContentMap[val]);
            this.$emit('textTypeNameChange', val);
            // 切换到话术质检/互动巡检/话术还原度 tab 时刷新单报告状态
            if (['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(val)) {
                this.refreshScriptMonitorStatus(val);
            }
        },
        // 切换 tab 时调 reportStatus 获取报告状态，status=2 时自动获取报告详情
        async refreshScriptMonitorStatus(type) {
            const { sourceType, sourceId } = getSourceData(this.sentenceMarkData);
            if (this.versionType === VERSION_TYPE.PURE || this.$store.getters.isPure) return
            if (!sourceId || !this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return;
            try {
                const sceneType = this.getScriptMonitorSceneType()
                const anchorInfo = this.sentenceMarkData?.anchorInfo || {}
                const secUid = anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId
                const payload = { sourceType, sceneType, sourceId }
                if (secUid) payload.secUid = secUid
                const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus(payload);
                if (res?.code !== 0 || !Array.isArray(res?.data?.monitors)) return;

                const fieldMap = {scriptQuality: 0, scriptRestoration: 1, interactionInspection: 2};
                const data = res?.data?.monitors?.find(a => a.monitorType === fieldMap[type]);

                if (!data) return;
                const status = Number(data.status ?? 0);
                const reportId = data.reportId ? String(data.reportId) : null
                const unread = !!(reportId && Number(data?.isRead) === 0)
                this.getTextVnode?.setUnread?.(type, unread)
                this.getTextVnode?.setMonitorEnabled?.(type, data?.monitorEnabled);
                this.getTextVnode?.setTextStatus(type, status);
                this.getTextVnode?.setReportId(type, reportId || null);
                // status=2 已生成时，自动调用详情接口获取报告内容
                if (status === 2 && reportId) {
                    const apiMap = { scriptQuality: 'getQualityInspectionReport', interactionInspection: 'getInteractionPatrolReport', scriptRestoration: 'getFidelityMonitorReport' };
                    const detailRes = await this.$httpBack.scriptMonitor[apiMap[type]]({ reportId });
                    if (detailRes?.code === 0 && detailRes?.data?.reportContent) {
                        const html = renderAiResponseContent(detailRes?.data?.reportContent || '', {
                            parserMode: AI_RENDER_MODE_MAP.MD_TAG,
                            forceStyle: true,
                            upgradePlainTable: true
                        }).html
                        this.setTextData(type, [html]);
                        this.getTextVnode?.setUnread?.(type, false)

                        // this.$emit('getTextList',{
                        //     type: this.questionContentMap[type] ?? type,
                        //     charType: this.questionContentNumMap[type]??type,
                        //     list: contentHtml,
                        //     status: status,
                        //     contentHtmlList: contentHtml
                        // })

                    }
                }
            } catch (e) {
                // 静默处理
            }
        },
        // 「点我开启XXX」→ setMonitorEnabled → 弹窗 → 生成本场报告
        async handleEnableMonitor({ type, monitorType }) {
            if (this.readonly || this.targetType === 'webOnline') return
            if (this.versionType === VERSION_TYPE.PURE || this.$store.getters.isPure) return
            const { sourceType } = getSourceData(this.sentenceMarkData)
            if (Number(sourceType) === 1) return this.$message.warning('文件分析仅支持手动生成报告')
            if (type === 'scriptRestoration') {
                this.getTextVnode?.openScriptRestorationDrawer?.('enable')
                return
            }
            const anchorInfo = this.sentenceMarkData?.anchorInfo;
            const anchorName = anchorInfo?.anchorName || anchorInfo?.AnchorName || '';
            const secUid = anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId
            if (!secUid) return this.$message.warning('缺少 secUid，无法开启自动监控')
            if (!this.$httpBack?.scriptMonitor?.setMonitorEnabled) return
            const configMap = {
                scriptQuality: {
                    label: '话术质检',
                    dialogText: `${anchorName}-是否开启自动话术质检监控？`,
                    confirmText: '开启自动话术质检'
                },
                interactionInspection: {
                    label: '互动巡检',
                    dialogText: `${anchorName}-是否开启自动互动巡检监控？`,
                    confirmText: '开启自动互动巡检'
                }
            };
            const cfg = configMap[type];
            if (!cfg) return;

            const showCancel = true;
            this.$confirm(`
                    <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                        <div>${cfg.dialogText}</div>
                    </div>`, '友情提示', {
                confirmButtonText: cfg.confirmText,
                cancelButtonText: '知道了',
                customClass: 'edit-file-name',
                showClose: true,
                showCancelButton: showCancel,
                closeOnClickModal: false,
                closeOnPressEscape: false,
                dangerouslyUseHTMLString: true,
                center: true
            }).then(() => {
                this.$httpBack.scriptMonitor.setMonitorEnabled({ secUid: String(secUid), monitorType, enabled: 1 }).then((res) => {
                    if (res?.code === 0) {
                        this.getTextVnode?.setMonitorEnabled?.(type, 1)
                        this.$message.success('已开启')
                        this.refreshScriptMonitorStatus(type)
                        return
                    }
                }).catch((e) => {
                })
            }).catch(() => {});
        },
        async handleScriptRestorationConfirmed() {
            if (this.readonly || this.targetType === 'webOnline') return
            if (!this.$httpBack?.scriptMonitor?.setMonitorEnabled) return
            const anchorInfo = this.sentenceMarkData?.anchorInfo || {}
            const secUid = anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId
            if (!secUid) return this.$message.warning('缺少 secUid，无法开启自动监控')
            try {
                const res = await this.$httpBack.scriptMonitor.setMonitorEnabled({ secUid: String(secUid), monitorType: 1, enabled: 1 })
                if (res?.code === 0) {
                    this.getTextVnode?.setMonitorEnabled?.('scriptRestoration', 1)
                    this.$message.success('已开启')
                    this.refreshScriptMonitorStatus('scriptRestoration')
                    return
                }
                this.$message.warning(res?.msg || '开启失败')
            } catch (e) {
            }
        },
        // 「已看完整个报告」按钮回调：当前以后端详情接口自动置已读为准，前端仅同步样式
        async handleConfirmRead({ type }) {
            const reportIdMap = {
                scriptQuality: 'scriptQualityReportId',
                interactionInspection: 'interactionInspectionReportId',
                scriptRestoration: 'scriptRestorationReportId'
            };
            const reportId = this.getTextVnode?.[reportIdMap[type]];
            try {
                if (!reportId) return
                this.getTextVnode?.setUnread?.(type, false)
                this.$message.success('已确认');
            } catch (e) {
                // 静默处理
            }
        },
        // 其他操作
        otherChange(val){
            this.getTextVnode?.otherChange(val);
            this.$emit('otherChange', val);
        },
        // 根据时长划分
        marksTitle(videoDurationInt) {
            this.videoMaxTime = videoDurationInt;
            let time = videoDurationInt;
            let arr = [];
            // 如果超过5小时按照60分钟拆分节点，如果是对比分析直接按照1小时拆分节点
            // 按照30分钟拆分定位条
            arr.push([0, '00:00']);
            let markTime = (parseInt(time)> 18000 ||this.isCompare) ? 3600 : 1800;
            while ((time - 300) > (markTime)) {
                arr.push([parseInt(arr.length * markTime),parseInt( arr.length * markTime / 60) + 'min']);
                time = time - markTime;
            }
            // arr.push([videoDurationInt, myUtils.toformatTimeMM_ss(videoDurationInt * 1000)])
            this.marks = arr;
            this.silderActive = true
        },
        // 选中节点
        selectMarkHandler(...arg){
            this.getTextVnode?.selectMarkHandler(...arg)
        },
        // 刷新段落
        refreshParagraph(...arg){
            this.getTextVnode?.refreshParagraph(...arg);
        },
        // 设置分析字符
        setAnalysisChar(val){
            this.getTextVnode?.setAnalysisChar(val);
        },
        // 设置是否弹幕
        setIsBulletScreen(val){
            this.getTextVnode?.setIsBulletScreen(val);
        },
        setDeal(val){
            this.getTextVnode?.setDeal(val);
        },
        setInteractionRate(val){
            this.getTextVnode?.setInteractionRate(val);
        },
        setDealRate(val){
            this.getTextVnode?.setDealRate(val);
        },
        setSales(val){
            this.getTextVnode?.setSales(val);
        },
        setUV(val){
            this.getTextVnode?.setUV(val);
        },
        setQianchuanCost(val){
            this.getTextVnode?.setQianchuanCost(val);
        },
        // 滑块值改变的回调
        sliderChange(value) {
            this.playerReadied(value * 1000, 'click');
        },
        // 格式化滑块弹窗信息
        tooltip(value) {
            return myUtils.toformatTimeMM_ssChinse(value * 1000);
        },
        // 刷新段落
        refreshHandler() {
            this.getTextVnode?.refreshHandler()
        },
        // 设置选中行业
        setTrade(id) {
            this.getTextVnode?.setTrade(id)
        },
        // 选中跳转
        selectDomeScrollIntoView(...arg){
            // 如果跳转段落不等于当前段落则重置当前选中段落
            if(arg[0] !== this.currentParagraphIndex){
                this.setCurrentParagraphIndex(arg[0]);
            }
            this.getTextVnode?.selectDomeScrollIntoView(...arg)
        },
        // 关闭行业弹窗
        dropDown() {
            this.getTextVnode?.dropDown();
        },
        // 设置视频时间
        setVideoCurrentTime(time) {
            this.videoCurrentTime = time * 1000;
            this.sliderValue = time
        },
        // 设置选中段落
        setCurrentParagraphIndex(index){
            if(typeof index === 'undefined' && index<0){return;}
            this.currentParagraphIndex = index;
            this.onParagraph(index);
        },
        // 整理关键词、敏感词数据
        tidyCountWords() {
            const { wordsCollect: list, wordsTabList: tabs } = this.sentenceMarkData;
            this.wordsInfo.wordsList = list;
            // 关键词总和
            this.wordsInfo.cruxWordsNum = tabs?.find(d=>d.tabType === 2)?.num || 0;
            // 敏感词总和
            this.wordsInfo.sensitiveWordsNum = tabs?.find(d=>d.tabType === 1)?.num || 0;
            this.markwords(this.wordsInfo)
        },
        // 设置关键词数据
        setWordsInfo(o) {
            this.$set(this,'wordsInfo', {
                ...this.wordsInfo,
                ...o
            })
            this.setMarkwords(this.wordsInfo || {});
        },
        // 导出文字内容
        exportTxt() {
            let fName = this.isVideoId ? this.videoInfo.VideoName : this.fileInfo.fileName;
            // web端导出
            if(this.targetType === 'webOnline'){
                const {sentenceMarkList} = this.sentenceMarkData
                // 要导出的文本内容
                const textContent = sentenceMarkList?.map(d=>d.content)?.join("\n");
                // 创建一个 Blob 对象，指定文本内容和文件类型
                const blob = new Blob([textContent], { type: 'text/plain;charset=utf-8' });
                // 创建一个 URL 对象，将 Blob 对象转换为可下载的 URL
                const url = URL.createObjectURL(blob);
                // 创建一个 <a> 元素
                const a = document.createElement('a');
                a.href = url;
                // 指定下载的文件名
                a.download = `${fName}.txt`;
                // 模拟点击 <a> 元素来触发下载
                a.click();
                // 释放 URL 对象，避免内存泄漏
                URL.revokeObjectURL(url);
                return
            }
            // 客户端导出。
            let requestData = {
                type: this.isVideoId ? 0 : 1,
                id: this.isVideoId ? this.videoInfo.VideoId : this.fileInfo.fileId,
                fileName: encodeURIComponent(fName),
            }
            let http = null;
            if(this.targetType === 'online'){
                // 云空间导出
                http = this.$httpClient.export.alysesCloudTxt;
            }else{
                // 其他导出
                http = this.$httpClient.export.alysestxt;
            }
            http(requestData).then(res => {
                if (res.code == 0 && res.data) {
                    this.$message.success("导出成功");
                }
            });
        },
        // 标注段落的关键词
        getTableListData(list, type, obj) {
            // list?.forEach(item => {
            //     const { wordsList } = item;
            //     // wordsList?.forEach(data => {
            //     //     let name = data.name + data.wordsType;
            //     //     // 关键词类型排序
            //     //     if (data.wordsType == 1) {
            //     //         let sort = this.getCruxTypeMap[data.type]?.sort;
            //     //         data.typeSort = typeof sort !== 'undefined' ? sort : 999;
            //     //     } else {
            //     //         data.typeSort = 0;
            //     //     }
            //     //     if (typeof obj[name] === 'undefined') {
            //     //         obj[name] = {
            //     //             ...JSON.parse(JSON.stringify(data)),
            //     //             count: 0
            //     //         }
            //     //         if (!obj[name].groupStr) {
            //     //             obj[name].groupStr = "";
            //     //         }
            //     //         if (!obj[name].tradeId) {
            //     //             obj[name].tradeId = "1";
            //     //         }
            //     //     }
            //     //     // 判断类型数据总数是否存在
            //     //     if (type !== '') {
            //     //         if (obj[name]?.['countNum' + type]) {
            //     //             obj[name]['countNum' + type] += data.countNum;
            //     //         } else {
            //     //             obj[name]['countNum' + type] = data.countNum;
            //     //         }
            //     //     }
            //     //     obj[name].count += data.countNum;
            //     // })
            // })
        },
        initWordsInfo(){
            if(this.readonly){return}
            this.$nextTick(() => {
                if (this.deafultWordsInfo) {
                    this.setWordsInfo(this.deafultWordsInfo)
                }
            })
        }
    },
    created() {
        this.initWordsInfo()
    },
    mounted() {
        if(this.readonly){return}
        // this.countWords();
        this.setSentenceMarkData(this.sentenceMarkData);
        this.loadScriptMonitorStatuses();
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { 
        this.initWordsInfo();
        this.loadScriptMonitorStatuses();
     }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.discernContainer {
    /* margin-left: 12px; */
    flex-grow: 1;
    // width: 0;
    position: relative;
    display: flex;
    flex-direction: column;
}

.discernContainer {
    /* margin-left: 12px; */
    box-sizing: border-box;
    flex-grow: 1;
    // width: 0;
    position: relative;
    display: flex;
    flex-direction: column;
}
</style>

<template>
    <aiLayout class="p-r" 
    :title="getTitle" 
    :uploadScreenshot.sync="uploadScreenshot" 
    :uploadBoard.sync="uploadBoard" 
    :config="optionConfig" 
    :isCompare="isCompare"
    :sentenceMarkData="sentenceMarkData"
    :type="getAiCueTypeKeyValue" 
    :readonly="isRaedonly"
    :targetType="targetType"
    :shareId="shareId"
    :modeName="modelTypeOption?.name"
    @toggle="onToggle"
    @quit="quitAi"
    @backgroundConfigChange="backgroundConfigChange"
    >
        <template #left-actions>
            <customPrompt v-if="isAssistant" @sendCustomPrompt="sendCustomPrompt" :aiCueType="getCueType" :moreConfigProps="moreConfigProps"></customPrompt>
        </template>
        <aiAssistant v-if="isNotAssistant" @loading="onLoading" @changeParagraph="changeParagraph" ref="aiAssistant"
            :type="getAiCueTypeKeyValue" :sentenceMarkData="sentenceMarkData" :isCompare="isCompare" :flod="getFlod"
            :aiModel="getModelType"
            :targetType="targetType"
            :uploadScreenshot="uploadScreenshot" :uploadBoard="uploadBoard"
            :askConfig="{...askConfig,backgroundConfigOne:backgroundConfig?.one,backgroundConfigTwo:backgroundConfig?.two}"
            :shareId="shareId"
            :readonly="isRaedonly"
            :pdfName="getPdfName"
            :modelOptions="modelTypeOption"
            :showModelType="true"
            :modelTypeProps="modelTypeProps"
            :showKnowledgeBase="true"
            :moreConfigValue="moreConfigValue"
            :moreConfigProps="moreConfigProps"
            @shareIamge="shareImgHandler"
            @model-change="modelChange"
            @model-option-change="changeModel"
            @knowledge-click="openKnowledgeBaseDrawer"
            @more-config-change="$emit('more-config-change',$event)"
            @addHistoryParagraph="addHistoryParagraph"></aiAssistant>
        <aiViolation v-else ref="aiViolation" @loading="onLoading" @changeParagraph="changeParagraph"
            :isCompare="isCompare" :type="getAiCueTypeKeyValue" :sentenceMarkData="sentenceMarkData" :flod="getFlod"
            :aiModel="getModelType"
            :targetType="targetType"
            :uploadScreenshot="uploadScreenshot" :uploadBoard="uploadBoard"
            :askConfig="{...askConfig,backgroundConfigOne:backgroundConfig?.one,backgroundConfigTwo:backgroundConfig?.two}"
            :shareId="shareId"
            :readonly="isRaedonly"
            :pdfName="getPdfName"
            :modelOptions="modelTypeOption"
            :showModelType="true"
            :modelTypeProps="modelTypeProps"
            :showKnowledgeBase="true"
            :moreConfigValue="moreConfigValue"
            :moreConfigProps="moreConfigProps"
            @shareIamge="shareImgHandler"
            @model-change="modelChange"
            @model-option-change="changeModel"
            @knowledge-click="openKnowledgeBaseDrawer"
            @more-config-change="$emit('more-config-change',$event)"
            @addHistoryParagraph="addHistoryParagraph">
        </aiViolation>
        <div class="other-more-box">
            <div class="analysis-multi-room-entry" @click="openAiAssistantWorkbench">
                <div class="analysis-multi-room-entry__img-box">
                    <img class="analysis-multi-room-entry__img" src="@/assets/imgs/agent/fxdc.png" alt="分析多场">
                </div>
            </div>
            <div class="back-to-top-box">
                <el-backtop target=".ai-content-box" class="flex flex-column items-center" style="bottom: 20px;right: 20px;">
                    <div class="text-xs">回到</div>
                    <div class="text-xs">顶部</div>
                </el-backtop>
            </div>
            <div v-if="!isCompare">
            <!-- <div> -->
                <otherDialog ref="otherDialog" :id="getId" :type="type" @click="otherClick"></otherDialog>
            </div>
        </div>
        <WarmHint ref="warmHint" @left-click="warmLeftClick" @right-click="warmRightClick" :rightBt="getBtns.rbt" :leftBt="getBtns.lbt">
            <div class="pd-b36">AI分析算力包不足，请购买算力包</div>
        </WarmHint>

        <KnowledgeBaseConfigDrawer
            :visible.sync="knowledgeBaseDrawerVisible"
            :anchorName="knowledgeBaseAnchorName"
            :value="knowledgeBaseFormData"
            @save="handleKnowledgeBaseSave"
        ></KnowledgeBaseConfigDrawer>

        <shareImg ref="shareImg"></shareImg>
    </aiLayout>
</template>

<script>
import aiLayout from './common/aiLayout.vue';
import aiAssistant from './aiAssistant/index.vue';
import aiViolation from './aiViolation/index.vue';
import otherDialog from './common/otherDialog.vue';
import WarmHint from '@/components/warmHint/index.vue';
import shareImg from './common/shareImg/index.vue';
import aiTypeMixin from '@/mixins/aiTypeMixin'
import customPrompt from './common/customPrompt/index.vue';
import KnowledgeBaseConfigDrawer from '@/components/knowledgeBaseConfigDrawer/index.vue'
export default {
    components: {
        aiLayout,
        aiAssistant,
        aiViolation,
        otherDialog,
        WarmHint,
        shareImg,
        customPrompt,
        KnowledgeBaseConfigDrawer
    },
    mixins: [aiTypeMixin],
    props: {
        // 运营 assistant
        // 违规 violation
        type: {
            type: String,
            default: 'assistant'
        },
        flod: {
            type: Number,
            default: 0
        },
        sentenceMarkData: {
            type: Object,
            default: () => { return {} }
        },
        isCompare: {
            type: Boolean,
            default: false
        },
        notChangeModel: {
            type: Boolean,
            default: false
        },
        aiModel: {
            type: [Number,undefined,String],
            default: undefined
        },
        targetType: {
            type: String,
            default: ''
        },
        askConfig: {
            type: Object,
            default: () => { return {} }
        },
        shareId:{
            type: String,
            default: ''
        },
        raedonly:{
            type: Boolean,
            default: false
        },
        moreConfigValue: {
            type: Object,
            default: () => ({})
        },
        moreConfigProps: {
            type: Object,
            default: () => ({})
        }
    },
    provide() {
        return {
            aiLayout: this
        }
    },
    inject: ['APP'],
    data() {
        return {
            // 0:doubao1.5-pro-32K，1:deepseek-r1
            modelType: '',
            uploadScreenshot: 0, // 设置AI数据识图
            uploadBoard: 0, // 设置数据看板
            optionConfig: {},
            loadingFns: [],
            modelTypeOption: {},
            backgroundConfig: {},//背景配置
            knowledgeBaseDrawerVisible: false,
            knowledgeBaseDraftMap: {},
            currentKnowledgeBaseRow: {},
            placeholderConfigList: []
        };
    },
    computed: {
        modelTypeProps(){
            return {
                value: this.modelTypeOption?.value !== undefined ? Number(this.modelTypeOption.value) : undefined,
                type: this.getAiCueTypeKeyValue,
                showViolationDialogOnEnter: this.showViolationDialogOnEnter,
                notChangeModel: this.notChangeModel,
                isCompare: this.isCompare
            }
        },
        knowledgeBaseAnchorName(){
            return this.currentKnowledgeBaseRow?.AnchorName || this.currentKnowledgeBaseRow?.RemarksName || ''
        },
        knowledgeBaseFormData(){
            return this.getKnowledgeBaseFormData(this.currentKnowledgeBaseRow)
        },
        getModelType(){
            try{
                return Number(this.modelTypeOption.value)
            }catch(e){
                return 0
            }
        },
        getBtns(){
            if(this.targetType === 'webOnline'){
                return {
                    rbt: '前往官网',
                    lbt: '联系客服'
                }
            }else{
                return {
                    rbt: '立即充值',
                    lbt: '知道了'
                }
            }
        },
        isNotAssistant() {
            return this.type !== 'violation'
        },
        isViolation(){
            return this.type === 'violation'
        },
        getTitle() {
            return this.getAiTypeTitle;
            // switch(this.type){
            //     case 'assistant':
            //         return 'AI运营助手'
            //     case 'violation':
            //         return 'AI违规助手'
            //     default:
            //         return ''
            // }
        },
        getFlod(){
            if(this.isCompare){
                return !!this.flod
            }else {
                return this.flod < 2
            }
        },
        getOtherItems() {
            return this.$refs?.otherDialog?.items
        },
        getAiDom() {
            if (this.isNotAssistant) {
                return this.$refs?.aiAssistant
            } else {
                return this.$refs?.aiViolation
            }
        },
        getId() {
            return this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.uploadFile?.fileId
        },
        isVideo() {
            return !!(this?.sentenceMarkData?.videoInfo && this?.sentenceMarkData?.videoInfo?.VideoId)
        },
        getSourceType() {
            return this.isCompare ? 2 : this.isVideo ? 0 : 1; // 源类型
        },
        isRaedonly(){
            return !!this.shareId || this.raedonly
        },
        showViolationDialogOnEnter(){
            return this.isViolation && !this.isCompare && !this.isRaedonly && !this.notChangeModel;
        },
        getPdfName(){
            const {  data1 } = this.sentenceMarkData;
            let d = data1 || this.sentenceMarkData
            const { videoInfo, uploadFile, anchorInfo, } = d;
            let imgName = (videoInfo?.VideoName || uploadFile?.fileName)?.split('.ts')[0]?.split('_');
            if(imgName?.length > 1){
                imgName.pop();
            }
            if(data1){
                imgName.push('对比');
            }
            imgName.push(this.getAiTypeTitle + '分析');
            imgName = imgName.join('_');
            return imgName;
        }
    },
    watch: {
    },
    methods: {
        // 自定义提示词问答。
        sendCustomPrompt({data,next}){
            this.getAiDom?.customPromptClick({
                id:data.id,
                title:data.promptTitle,
                prompt:data.promptContent,
                promptMoreConfig: data.promptMoreConfig,
                placeholderKeys: data.placeholderKeys
            });
            next();
        },
        setScene(scene,type){
            // set 表示连接跳转设置切片
            if(type === 'set'){
                this.loadingFns.push(()=>{
                    this.getAiDom?.setScene(scene);
                })
            }else{
                this.getAiDom?.setScene(scene);
            }
        },
        shareImgHandler(list){
            const {  data1 } = this.sentenceMarkData;
            let d = data1 || this.sentenceMarkData
            const { videoInfo, anchorInfo } = d || {};
            let imgName = this.getPdfName;
            const modelOption = this.modelTypeOption || {}
            this.$refs.shareImg?.show({
                data: {
                    list,
                    imgName,
                    modelType: modelOption?.value,
                    modelName: modelOption?.name || '',
                    anchorName: anchorInfo?.AnchorName,
                    startTime: videoInfo?.StartTime,
                    endTime: videoInfo?.EndTime,
                    duration: videoInfo?.Duration
                }
            });
        },
        changeModel(data){
            this.modelTypeOption = data || {};
        },
        modelChange(value){
            this.$emit('modelChange', value);
        },
        setModelType(value){
            this.modelType = value;
            this.modelChange(value);
        },
        ifViolationModel(){
            return
            if(this.isCompare){
                this.setModelType(1)
                return
            }
            // 使用的ai模型 0:doubai pro 1:deepseek-r1, 2:doubao 思考, 3:doubao 综合
            let defaultModelMap = {
                assistant: 1,
                violation: 1,
                scrolling: 1,
                textAssistant: 1
            };
            let dValue = defaultModelMap[this.type] ??  3;
            this.setModelType(dValue);

            // if(this.isViolation){
            //     this.setModelType(0);
            // }else if(this.isTextAssistant){
            //     this.setModelType(2);
            // }else{
            //     this.setModelType(3);
            // }
        },
        changeParagraph(code) {
            this.$nextTick(() => {
                this.$refs?.otherDialog?.onlySelect(code);
            })
        },
        show() {
            this.$refs.warmHint.show();
        },
        warmLeftClick() {
            if(this.targetType === 'webOnline'){
                this.APP.showQrCode();
                this.$nextTick(()=>{
                    this.$refs.warmHint?.hide();
                })
                return;
            }
            this.$refs.warmHint?.hide();
        },
        warmRightClick() {
            if(this.targetType === 'webOnline'){
                this.APP.toIfupanWebsite();
                return;
            }
            this.warmLeftClick();
            this.APP?.showQrCode();
        },
        onToggle(val) {
            this.$emit('toggle', val)
        },
        quitAi() {
            if (this.$isWeb) {
                const {name, params} = this.$route
                const {id, fileType} = params || {}
                if (name === "onlineAiAnalysis") {
                    this.$router.push({
                        path: `/onlineAnalysis/${fileType || 0}/${id || this.getId}`,
                    })
                } else if (name === "contrastAiAnalysis") {
                    this.$router.push({
                        path: `/contrastOnlineAnalysis/${id || this.getId}`,
                    })
                } else {
                    this.$router.go(-1);
                }
            } else {
                this.$router.go(-1);
            }
            this.$emit('quit')
        },
        otherClick(data) {
            this.$nextTick(() => {
                setTimeout(() => {
                    this.getAiDom?.addTitle(data.value === 'all' ? {
                        label: data.label,
                        value: data.value
                    } : {
                        ...data,
                        value: data.code,
                        html: data.content || data.html,
                        label: data.alias
                    });
                }, 200)
            })
        },
        addOther(data) {
            data.html = data.content;
            delete data.content;
            data.label = data.title;
            delete data.title;
            data.notProblem = data.isToggle;
            delete data.isToggle;
            this.$refs.otherDialog?.addItem(data);
        },
        // 添加历史记录
        addHistoryParagraph({ items, type }) {
            this.$refs?.otherDialog?.setItems(items, type === 0 ? 'assistant' : 'violation');
        },
        selectParagraph(code) {
            this.$nextTick(() => {
                this.$refs?.otherDialog?.selectCode(code);
            })
        },
        onLoading() {
            this.$nextTick(() => {
                setTimeout(() => {
                    this.$refs?.otherDialog?.selectDelayCode();
                    // 释放loading加载队列
                    while(this.loadingFns.length){
                        this.loadingFns.shift()();
                    }
                }, 500)
            })
        },
        // 获取全局配置
        getOptionConfig(){
            if(this.isCompare){
                this.uploadScreenshot = 1;
                this.uploadBoard = 1;
                return
            }
            this.uploadScreenshot = 0;
            this.uploadBoard = 0;
            this.$httpBack.v2300.aiOptionConfig({
                sourceId: this.getId,
                sourceType: this.getSourceType
            }).then(res=>{
                // 设置全局配置是否禁用配置参数。
                this.optionConfig = res.data;
                // 如果有截图数据则默认 勾选
                if(this.optionConfig.hasDataScreenshot){
                    this.uploadScreenshot = 1;
                }
                // 如果有看板数据则默认 勾选
                if(this.optionConfig.hasBoard){
                    this.uploadBoard = 1;
                }
            })
        },
        backgroundConfigChange(compereForm){
            this.backgroundConfig = compereForm
        },
        getKnowledgeBaseStorageKey() {
            return 'dataAnalysisAnchorKnowledgeBaseDraftMap'
        },
        loadKnowledgeBaseDraftMap() {
            try {
                const cache = localStorage.getItem(this.getKnowledgeBaseStorageKey())
                this.knowledgeBaseDraftMap = cache ? JSON.parse(cache) : {}
            } catch (e) {
                this.knowledgeBaseDraftMap = {}
            }
        },
        saveKnowledgeBaseDraftMap() {
            localStorage.setItem(this.getKnowledgeBaseStorageKey(), JSON.stringify(this.knowledgeBaseDraftMap || {}))
        },
        getKnowledgeBaseFormData(row = {}) {
            const secUid = row?.SecUid
            if (!secUid) return {}
            return this.knowledgeBaseDraftMap?.[secUid] || {}
        },
        getCurrentKnowledgeBaseRow() {
            const base = this.sentenceMarkData?.data1 || this.sentenceMarkData || {}
            const anchorInfo = base?.anchorInfo || {}
            return {
                ...anchorInfo
            }
        },
        async loadPlaceholderConfigList() {
            try {
                const res = await this.$httpBack.placeholder?.listForClient({})
                this.placeholderConfigList = res?.data || []
            } catch (e) {
                this.placeholderConfigList = []
            }
        },
        async fetchKnowledgeBaseInfo(secUid) {
            if (!secUid) return
            try {
                const res = await this.$httpBack.anchorKnowledge?.info({secUid})
                if (res?.code !== 0 || !res?.data) return
                const info = res.data || {}
                const current = this.knowledgeBaseDraftMap?.[secUid] || {}
                this.knowledgeBaseDraftMap = {
                    ...this.knowledgeBaseDraftMap,
                    [secUid]: {
                        ...current,
                        id: info?.id ?? current?.id,
                        operationKnowledge: info?.operationContent ?? info?.operationKnowledge ?? current?.operationKnowledge ?? '',
                        sensitiveKnowledge: info?.sensitiveContent ?? info?.sensitiveKnowledge ?? current?.sensitiveKnowledge ?? '',
                        healthKnowledge: info?.healthScore ?? info?.healthKnowledge ?? current?.healthKnowledge ?? '',
                        updatedAt: Date.now()
                    }
                }
                this.saveKnowledgeBaseDraftMap()
            } catch (e) {}
        },
        openKnowledgeBaseDrawer() {
            const row = this.getCurrentKnowledgeBaseRow()
            const secUid = row?.SecUid
            if (!secUid) {
                this.$message.warning('缺少主播标识，暂无法打开知识库')
                return
            }
            this.currentKnowledgeBaseRow = { ...row }
            this.fetchKnowledgeBaseInfo(secUid).finally(() => {
                this.knowledgeBaseDrawerVisible = true
            })
        },
        async handleKnowledgeBaseSave(formData = {}) {
            const secUid = this.currentKnowledgeBaseRow?.SecUid
            if (!secUid) {
                this.$message.warning('缺少主播标识，暂无法保存知识库')
                return
            }
            const current = this.knowledgeBaseDraftMap?.[secUid] || {}
            const payload = {
                secUid,
                operationContent: String(formData?.operationKnowledge || '').trim(),
                sensitiveContent: String(formData?.sensitiveKnowledge || '').trim(),
                healthScore: String(formData?.healthKnowledge || '').trim(),
            }
            try {
                if (current?.id) {
                    await this.$httpBack.anchorKnowledge?.update({
                        ...payload,
                        id: current.id
                    })
                } else {
                    await this.$httpBack.anchorKnowledge?.save(payload)
                }
                await this.fetchKnowledgeBaseInfo(secUid)
                this.$message.success('知识库已保存')
            } catch (e) {}
        },
        /**
         * @description 打开 AI 工作台，与 AI诊断多场 跳转逻辑一致：
         * 携带 secUid、videoId 定位直播间，不传 cue 参数。
         * @returns {void}
         */
        openAiAssistantWorkbench() {
            const secUid = String(this.sentenceMarkData?.anchorInfo?.SecUid || this.sentenceMarkData?.anchorInfo?.secUid || '').trim()
            const videoId = String(this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.videoInfo?.videoId || '').trim()
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb({
                    secUid,
                    videoId,
                    openInBrowserWindow: this.targetType === 'webOnline'
                })
                return
            }
            this.$router.push({
                path: '/aiAssistant'
            })
        }
    },
    created() {
        if(typeof this.aiModel !=='undefined'){
            this.setModelType(this.aiModel);
        }else{
            this.ifViolationModel();
        }
        this.getOptionConfig();
        this.loadKnowledgeBaseDraftMap();
        this.loadPlaceholderConfigList();
        
    },
    mounted() {
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.other-more-box{
    position: absolute;
    bottom: 10px;
    left: 18px;
    z-index: 1000;
}

.analysis-multi-room-entry{
    position: fixed;
    right: 25px;
    bottom: 68px;
    width: 40px;
    height: 40px;
    margin-bottom: 0;
    cursor: pointer;
    z-index: 1001;
}

.analysis-multi-room-entry__img-box{
    width: 48px;
    border-radius: 50%;
    overflow: hidden;
}

.analysis-multi-room-entry__img{
    display: block;
    width: 100%;
    height: 100%;
}


</style>

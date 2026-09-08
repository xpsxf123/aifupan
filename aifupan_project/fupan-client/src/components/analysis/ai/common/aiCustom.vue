<!--
/**
 * @description AI问答回答内容展示组件，负责渲染回答内容及底部工具栏操作。
 */
-->
<template>
    <div :class="{'flex-jc-sb': getIsShare}" class="aiCustom-box">
        <div v-if="getIsShare" class="share-select-box pd-l6 pd-r6 flex-ai-c">
            <el-checkbox v-model="otherOption.selectCode" @change="share"></el-checkbox>
        </div>
        <div v-loading="getLoading" :class="isSelect?'is-select-html':'not-select-html'" style="flex: 1;overflow: hidden;" :ref="`html-content_${id}`">
            <div v-html="html" v-if="identity === 'self'" :class="{'loading-box':getLoading}"></div>
            <template v-if="identity === 'service'" >
                <div v-html="displayHtml" ref="contentHtml" :class="{'loading-box':getLoading}"></div>
            </template>
            <div v-if="identity === 'self' && !getReadonly" class="flex-jc-e">
                <!-- <span v-if="isAssistant" class="font-s12 text-color3 pd-t4">提示：AI擅长拆解和分析；但是优化方案略高于初级运营，仅做参考。</span> -->
                <span v-if="isViolation" class="font-s12 text-color3 pd-t4">提示：为了尽可能降低违规风险，采用了较松的尺度，请注意！</span>
            </div>
            <div v-if="tool && !getReadonly" class="mg-t10">
                <div v-if="showToolLoading" class="aiCustom-tool-loading">
                    <img src="@/assets/imgs/2_6_0/aiLoading.gif" alt="">
                </div>
                <div v-else class="flex-jc-sb">
                    <div class="flex">
                        <div v-if="isWordDiscern">
                            <el-button @click="fullText" type="text" >预设问题列表</el-button>
                            <!--                        <el-button v-if="!isCompare && isParagraph" @click="ohter('paragraph')" type="text">段落问题列表</el-button>-->
                        </div>
                        <div v-else>
                            <el-button @click="fullText" type="text">{{getCueTypeLabel}}问题列表</el-button>
                        </div>
                        <div class="pd-l10 flex items-center">
                            <el-button
                                v-if="!isViolation"
                                @click="togglePlainTextMode"
                                type="text">
                                <span>{{ isPlainTextMode ? '切PPT样式' : '切换纯文本' }}</span>
                            </el-button>
                        </div>

                        <div class="flex items-center mg-l12">
                            <span class="mg-r6" style="color: #DCDCDC">|</span>
                            <img v-if="aiCorrectFormatInfo.aiCorrectStatus!==1" style="max-width: 15px;" src="@/assets/imgs/2_5_8/format.png"  alt="">
                            <el-button type="text" @click="()=>optimizeFormat()"
                                       :loading="aiCorrectFormatInfo.aiCorrectStatus===1" :disabled="!isIdOk">优化格式
                            </el-button>
                            <el-tooltip class="item" effect="dark" v-if="aiCorrectFormatInfo.aiCorrectError" :content="aiCorrectFormatInfo?.aiCorrectError" placement="top">
                                <i class="el-icon-warning mg-l4 color-red"></i>
                            </el-tooltip>
                        </div>
                        <div class="pd-l10 flex items-center">
                            <div class="text-sm">
                                <span style="color: #DCDCDC;padding-inline: 5px">|</span>
                                <span style="color: #484A4D">分析不满意？进一步在输入框持续提问</span>
                            </div>
                        </div>
                    </div>
                    <div class="flex-jc-e flex-ai-c">
                        <div class="other-items pd-l10 pd-r10 flex-ji-c">
                            <el-button class="pd-0" :disabled="otherOption?.isShare" @click.stop="shareHandler" type="text">
                                <span class="flex-ai-c">
                                    <i class="font_family icon-a-fenxiang1"></i>
                                    <span>导出和分享</span>
                                </span>
                            </el-button>
                            <el-button type="text" :disabled="qaCodeLoading" @click="exportErrorQA" v-if="$store?.getters?.getMode">导出错误回答</el-button>
                        </div>
                    </div>
                </div>
                <!-- <div v-if="isWordDiscern" class="flex-jc-sb">
                    <div>
                        <el-button @click="ohter('all')" type="text" >全文问题列表</el-button>
                        <el-button v-if="!isCompare" @click="ohter('paragraph')" type="text" >段落问题列表</el-button>
                    </div>
                    <Commend v-if="!isUseBack" v-model="giveStatuc" @click="commendClick"></Commend>
                </div>
                <div v-else  class="flex-jc-sb">
                    <div>
                        <el-button @click="ohter('all')" type="text" >{{getCueTypeLabel}}问题列表</el-button>
                    </div>
                </div> -->
            </div>
        </div>
    </div>
</template>

<script>
/**
 * @description AI问答回答内容展示逻辑，处理内容渲染、格式切换、导出分享等工具操作。
 */
// import Commend from './commend.vue';
// import { buildNativePrintScript } from './script/nativePrint';
// import { buildChartSlicesScript } from './script/chartSlices';
// import { IFRAME_RESIZE_JS } from './script/iframeResize';
import aiTypeMixin from '@/mixins/aiTypeMixin'
import optimizeFormatM from  './optimizeFormat'
import {createDialogueHtml} from '@/utils/aiCreateEle.js'
// import {cloneDeep} from "lodash";
import {trackEvent} from "@/utils/laTrack";
export default {
    components: {
        // Commend,
    },

    inject: {
        AiVnode: {
            default: () => ({})
        }
    },
    props:{
        otherOption: {
            type:Object,
            default: ()=>{return {}}
        },
        onContentClick(e){
            const root = this.$refs?.contentHtml;
            if (!root) return;
            let node = e.target;
            while (node && node !== root) {
                if (node.tagName === 'A') {
                    const href = node.getAttribute('href');
                    if (href && !href.startsWith('javascript:')) {
                        e.preventDefault();
                        try {
                            window.open(href, '_blank', 'noopener,noreferrer');
                        } catch(_) {}
                    }
                    return;
                }
                node = node.parentNode;
            }
        },
        html: {
            type: String,
            default: ''
        },
        tool: {
            type: Boolean,
            default: false
        },
        identity: {
            type: String,
            default: ''
        },
        code: {
            type: String,
            default: ''
        },
        qaCode:{
            type: String,
            default: ''
        },
        giveStatuc: {
            type: [Number,String],
            default: -1
        },
        aiModel: {
            type: Number,
            default: 0
        },
        generateHtmlInfo: {
            type: Object,
            default: () => {
                return {}
            }
        },
        aiCorrectFormatInfo:{
            type: Object,
            default: () => {
                return {}
            }
        },
        loading: {
            type: Boolean,
            default: false
        },
        isCompare: {
            type: Boolean,
            default: false
        },
        isUseBack: {
            type: Boolean,
            default: false
        },
        addEvent:{
            type: Boolean,
            default: false
        },
        isShare: {
            type: Boolean,
            default: false
        },
        readonly: {
            type: Boolean,
            default: false
        },
        id: {
            type: String,
            default: ''
        },
        item:{
            type: Object,
            default: ()=>{return {}}
        },
        pdfName:{
            type: String,
            default: ''
        },
        rawAnswerContent: {
            type: String,
            default: ''
        },
        rawThinkingContent: {
            type: String,
            default: ''
        }
    },
    mixins: [optimizeFormatM,aiTypeMixin],
    data() {
        return {
            // 深度思考默认折叠（流式阶段一行气泡、输出完成后板块折叠）
            collapseBl:false,
            thinkingInitialized: false,
            QAMode: 'text',
            isPlainTextMode: false,
            currentHtmlInfo: {},//html图表是否生成中以及包含的信息
            currentHtmlStr:'',
            fullscreenLoading: false
        };
    },
    computed: {
        getReadonly(){
            return this.readonly
        },
        getIsShare(){
            return this.otherOption?.isShare && !this.getReadonly
        },
        isSelect(){
            return this.otherOption?.selectCode && this.getIsShare
        },
        isParagraph(){
            return this.type === 'assistant' || this.type === 'violation'
        },
        getLoading(){
            return this.loading
        },
        qaCodeLoading(){
            return !this.qaCode
        },
        showToolLoading() {
            const sending = !!this.AiVnode?.sendAskLoad
            if (!sending) return false
            const currentDomId = this.AiVnode?.sendAskDomId
            if (!currentDomId) return false
            return this.identity === 'service' && this.id === currentDomId
        },
        isIdOk() {
            return this.id?.indexOf('dom_') === -1
        },
        iframeSrc(){
            return `${this.currentHtmlInfo?.htmlDomainName}/${this.currentHtmlInfo?.htmlSavePath}`
        },
        displayHtml() {
            if (this.identity !== 'service') {
                return this.html;
            }
            const answerRaw = this.rawAnswerContent || '';
            const thinkingRaw = this.rawThinkingContent || '';
            if (!answerRaw && !thinkingRaw) {
                return this.html;
            }
            const renderHtml = this.AiVnode?.getMdText?.(answerRaw, {
                thinkingContent: thinkingRaw,
                plainTextMode: this.isPlainTextMode,
                // 流式加载中（showToolLoading=true）：剔除自定义标签仅渲染纯 MD，避免标签未闭合卡顿
                streaming: !!this.showToolLoading
            });
            if (!renderHtml) {
                return this.html;
            }
            return createDialogueHtml(renderHtml, 'left', this.id);
        }
    },
    watch: {
        addEvent(val){
            if(val){
                this.loadingSuccess();
            }
        },
        html(){
            this.loadingSuccess();
        },
        rawAnswerContent(){
            this.loadingSuccess();
        },
        rawThinkingContent(){
            this.loadingSuccess();
        },
        otherOption: {
            handler(val) {
                if (val.selectCode&&this.getIsShare) {
                    this.QAMode = 'text'
                }
            },
            immediate: true,
            deep: true
        },
        generateHtmlInfo:{
            handler(val) {
                this.currentHtmlInfo = val
                // 图表功能已停用，固定纯文本/富文本通道
            },
            immediate: true,
            deep: true
        }
    },
    methods: {
        /**
         * @description 上报复盘列表埋点事件。
         * @param {string} code 埋点编码
         * @returns {void}
         */
        trackReplayListEvent(code) {
            trackEvent(code);
        },
        exportErrorQA(){
            this.$httpClient.aiRelated.exportAiConfig({
                qaCodes: this.qaCode,
            })
        },
        onClick(event){
            this.$emit('click',event);
        },
        commendClick(val){
            this.$emit('commendClick',{
                ...val,
                data: {
                    giveStatuc: val.data,
                    code: this.code,
                    id: this.id
                },
            })
        },
        fullText(){
          this.trackReplayListEvent('P003_A0040');
          this.ohter(this.AiVnode.scene || 'all')
        },
        togglePlainTextMode() {
            this.isPlainTextMode = !this.isPlainTextMode;
            this.loadingSuccess();
        },
        ohter(type){
            this.$parent?.addProblem({},type);
        },
        aiCorrectFormat() {
            this.$emit('aiCorrectFormat', this.id)
        },
        getStrForHtml(){
            return
            /*
            if(!this.currentHtmlInfo?.htmlDomainName||!this.currentHtmlInfo?.htmlSavePath) return
            const iframeSrc = `${this.currentHtmlInfo?.htmlDomainName}/${this.currentHtmlInfo?.htmlSavePath}?t=${Date.now()}`
            const lowerUrlClone = cloneDeep(iframeSrc)
            const lowerUrl = lowerUrlClone.toLowerCase();
            if (lowerUrl.startsWith('http://') || lowerUrl.startsWith('https://')) {
                fetch(iframeSrc)
                    .then(res => res.text())
                    .then(html => {
                        let htmlStr = this.buildHtml(html);
                        // 浏览器模式加入打印机调用。
                        htmlStr = this.$isAifupan ? htmlStr : this.addHtmlModel(htmlStr);
                        this.currentHtmlStr = htmlStr
                    })
            }
            */
        },
        /**
         * 将通信脚本与原生浏览器PDF生成逻辑注入到HTML字符串末尾
         * @param {string} html 原始HTML字符串
         * @returns {string} 注入通信与打印脚本后的HTML字符串
         * @throws {Error} 当传入的html不是字符串时抛出异常
         */
        addHtmlModel(html){
            return html
            /*
            if (typeof html !== 'string') {
                throw new Error('addHtmlModel: html参数必须为字符串');
            }
            const script = buildNativePrintScript();
            var lower = html.toLowerCase();
            var idxBody = lower.lastIndexOf('<\/body>');
            var idxHtml = lower.lastIndexOf('<\/html>');
            if (idxBody !== -1) {
                return html.slice(0, idxBody) + script + html.slice(idxBody);
            } else if (idxHtml !== -1) {
                return html.slice(0, idxHtml) + script + html.slice(idxHtml);
            }
            return html + script;
            */
        },
        exportChart(){
            return
            /*

            // 执行打印判断逻辑，客户端时使用接口打印，浏览器时使用默认的打印机调用功能。
            if(this.$isAifupan){
                // 接口打印pdf
                this.$message({
                    message: 'pdf生成中，请稍后...',
                    type: 'info',
                })
                // 打印时使用的html代码。
                let html = this.createExportPdfHtml(this.currentHtmlStr);
                this.$httpClient.system.htmlPrintPDF({
                    htmlContent: html,
                    fileName: this.pdfName+'-图表'+'.pdf',
                })
            }else{
                // 浏览器打印pdf
                const PRINT_EVENT = 'native-print-pdf';
                const root = this.$refs?.contentHtml;
                const iframe = root?.getElementsByClassName('iframe_content')?.[0] || document.querySelector('.iframe_content');
                if (!iframe || !iframe.contentWindow) {
                    const msg = 'exportChart: 未找到可通信的iframe';
                    if (this.$message) this.$message.error(msg);
                    throw new Error(msg);
                }
                try {
                    iframe.contentWindow.postMessage({ type: PRINT_EVENT }, '*');
                    if (this.$message) this.$message.success('已通知子页面执行PDF打印');
                } catch (e) {
                    if (this.$message) this.$message.error('触发PDF打印失败');
                    console.error('exportChart postMessage error:', e);
                }
            }
            */
        },
        createExportPdfHtml(htmlStr){
            return htmlStr || ''
            /*
            const html = htmlStr || '';
            const injectScript = buildChartSlicesScript();
            let lower = html.toLowerCase();
            let idxBody = lower.lastIndexOf('</body>');
            let idxHtml = lower.lastIndexOf('</html>');
            let nextHtml;
            if (idxBody !== -1) {
                nextHtml = html.slice(0, idxBody) + injectScript + html.slice(idxBody);
            } else if (idxHtml !== -1) {
                nextHtml = html.slice(0, idxHtml) + injectScript + html.slice(idxHtml);
            } else {
                nextHtml = html + injectScript;
            }
            return nextHtml;
            */
        },
        scrollToRef(refName) {
            this.$nextTick(() => {
                const el = this.$refs?.[refName];
                if (!el) return

                el.scrollIntoView({
                    behavior: 'smooth', // 平滑滚动
                    block: 'start'      // 对齐方式
                })
            })
        },
        buildHtml(html) {
            return html
            /*
            const EXTRA_CSS = `
                html, body {
                  margin: 0;
                  padding: 0;
                  height: auto;
                }
                * {
                  box-sizing: border-box;
                }
            `;
            //动态获取iframe高度
            const EXTRA_JS = `${IFRAME_RESIZE_JS};
(function(){
  function ensureThinkingTitle(){
    var thinkings = document.getElementsByClassName('deepThinking');
    for(var i=0;i<thinkings.length;i++){
      var thinking = thinkings[i];
      if(!thinking || !thinking.parentNode){continue;}
      var parent = thinking.parentNode;
      if(parent.classList && !parent.classList.contains('deepThinkingSection')){
        parent.classList.add('deepThinkingSection');
      }
      var title = parent.querySelector ? parent.querySelector('.deepThinkingTitle') : null;
      if(!title){
        title = document.createElement('div');
        title.className = 'deepThinkingTitle';
        title.innerHTML = '深度思考<span class="deepThinkingToggle"></span>';
        parent.insertBefore(title, thinking);
      }
    }
  }
  function ensureThinkingToggle(){
    ensureThinkingTitle();
    var titles = document.getElementsByClassName('deepThinkingTitle');
    for(var i=0;i<titles.length;i++){
      var t = titles[i];
      if(!t.querySelector || t.querySelector('.deepThinkingToggle')){continue;}
      var s = document.createElement('span');
      s.className = 'deepThinkingToggle';
      t.appendChild(s);
    }
  }
  function apply(title, expanded){
    if(!title){return;}
    title.classList.toggle('not-active', !expanded);
    var parent = title.parentNode;
    if(!parent || !parent.getElementsByClassName){return;}
    var nodes = parent.getElementsByClassName('deepThinking');
    for(var i=0;i<nodes.length;i++){
      nodes[i].style.display = expanded ? 'block' : 'none';
    }
  }
  function toggle(title){
    var expanded = !title.classList.contains('not-active');
    apply(title, !expanded);
  }
  document.addEventListener('click', function(e){
    var target = e && e.target;
    if(!target){return;}
    var title = target.closest ? target.closest('.deepThinkingTitle') : null;
    if(!title){return;}
    toggle(title);
  }, true);
  if(document.readyState === 'loading'){
    document.addEventListener('DOMContentLoaded', ensureThinkingToggle);
  }else{
    ensureThinkingToggle();
  }
})();`;
            //将CSS 插入到 </style> 前
            html = html.replace(
                /<\/style>/,
                `${EXTRA_CSS}\n</style>`
            );

            // 将JS 插入到 </body> 前
            html = html.replace(
                /<\/body>/,
                `<script>${EXTRA_JS}<\/script></body>`
            );

            return html;
            */
        },
        async changeMode(mode,id) {
            // 图表模式已停用，保留函数避免外部调用报错
            this.QAMode = 'text'
            this.scrollToRef(`html-content_${id}`)
        },
        loadingSuccess(){
            this.$nextTick(()=>{
                const root = this.$refs.contentHtml;
                if (root && root.dataset?.thinkingDelegated !== '1') {
                    root.dataset.thinkingDelegated = '1';
                    root.addEventListener('click', (e) => {
                        let target = e?.target;
                        if (!target) return;
                        if (target.nodeType === 3) {
                            target = target.parentNode;
                        }
                        if (!target) return;
                        if (target.classList?.contains('deepThinkingToggle')) {
                            target = target.parentNode;
                        }
                        if (target?.className?.indexOf('font_family') >= 0) {
                            target = target.parentNode;
                        }
                        const title = target?.closest
                            ? target.closest('.deepThinkingTitle')
                            : (target?.classList?.contains('deepThinkingTitle') ? target : null);
                        if (!title) return;
                        this.collapseThinking(title);
                    })
                }
                let dom = root?.getElementsByClassName('deepThinkingTitle')?.[0];
                if(!dom){
                    const thinking = root?.getElementsByClassName('deepThinking')?.[0];
                    const section = thinking?.parentNode;
                    if(section){
                        section.classList?.add('deepThinkingSection');
                        dom = document.createElement('div');
                        dom.className = 'deepThinkingTitle';
                        dom.innerHTML = '深度思考<span class="deepThinkingToggle"></span>';
                        section.insertBefore(dom, thinking);
                    }
                }
                if(!dom){return}
                const titleText = (dom.textContent || '').trim();
                if(!titleText){
                    dom.innerHTML = '深度思考<span class="deepThinkingToggle"></span>';
                }
                if(!dom.querySelector('.deepThinkingToggle')){
                    dom.insertAdjacentHTML('beforeend','<span class="deepThinkingToggle"></span>');
                }
                if(this.getReadonly){
                    this.collapseBl = false;
                }
                if (!this.thinkingInitialized) {
                    // 深度思考默认折叠：流式阶段折叠为一行气泡展示，输出完成后板块也保持折叠
                    this.collapseBl = false;
                    this.thinkingInitialized = true;
                }
                this.applyThinkingCollapse(dom, this.collapseBl);

            })
        },
        foldThinking(bl){
            this.$nextTick(()=>{
                let deepThinkingTitle = this.$refs.contentHtml?.getElementsByClassName('deepThinkingTitle')?.[0];
                this.collapseThinking(deepThinkingTitle, bl)
            })
        },
        collapseThinking(self,bl){
            if(!self){return}
            this.collapseBl = typeof bl !== 'undefined'?  bl: !this.collapseBl;
            this.applyThinkingCollapse(self, this.collapseBl);
        },
        /**
         * @description 应用深度思考折叠/展开状态。
         * 气泡样式（deepThinkingBubbleSection）通过 class 切换“一行气泡/完整原文”两种形态；
         * 旧版深度思考区块保持原有 display 切换逻辑。
         * @param {HTMLElement} self 深度思考标题元素。
         * @param {boolean} isExpanded 是否展开。
         * @returns {void}
         */
        applyThinkingCollapse(self, isExpanded){
            if(!self){return}
            const bubbleSection = self.closest ? self.closest('.deepThinkingBubbleSection') : null;
            if(bubbleSection){
                // 气泡模式：折叠时仅显示一行气泡，展开时显示完整原文，不做整块隐藏
                self.classList.toggle('not-active', !isExpanded);
                bubbleSection.classList.toggle('is-collapsed', !isExpanded);
                return;
            }
            const deepThinkings = self?.parentNode?.getElementsByClassName('deepThinking');
            self.classList.toggle('not-active', !isExpanded);
            for(let i = 0; i<deepThinkings.length;i++){
                deepThinkings[i].style.display = isExpanded ? 'block' : 'none';
            }
        },
        shareHandler(){
            this.trackReplayListEvent('P003_A0044');
            this.$emit('shareHandler', this.id);
        },
        share(value){
            this.$emit('share', typeof value === 'boolean' ? value : this.otherOption.selectCode);
        },
        onMessage(e) {
            return
            /*
            if (e.data?.type === 'iframe-resize') {
                const iframes = document.querySelectorAll('.iframe_content')
                for (const iframe of iframes) {
                    if (iframe.contentWindow === e.source) {
                        this.fullscreenLoading = e.data.load
                        iframe.style.height = e.data.height + 60 + 'px'
                        break
                    }
                }
            }
            */
        },
        echartsAgain(id){
            // 图表重新生成功能已停用
            this.QAMode = 'text'
        }
    },
    created() {

    },
    mounted() {
        // window.addEventListener('message', this.onMessage)
        const el = this.$refs?.contentHtml;
        if (el) el.addEventListener('click', this.onContentClick, true);
        this.loadingSuccess();
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {
        this.loadingSuccess();
    }, //生命周期 - 更新之后
    beforeDestroy() {
        // window.removeEventListener('message', this.onMessage)
        const el = this.$refs?.contentHtml;
        if (el) el.removeEventListener('click', this.onContentClick, true);
    }, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
        // window.addEventListener('message', this.onMessage)
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.aiCustom-box{
    margin-top: 20px;
}
.loading-box{
    .ai-content-box{
        min-height: 50px;
        min-width: 60%;
    }
}
.is-select-html{
    width: 100%;
    border: 1px var(--color-main) solid;
    border-radius: 6px;
}
.not-select-html{
    border: 1px transparent solid;
}
.share-select-box{
    ::v-deep(.el-checkbox__inner){
        border-width: 2px;
        width: 16px;
        height: 16px;
        border-color: var(--color-main);
    }
}
.iframe_content{
    border: none;
    padding: 0;
    margin: 0;
    display: block;
    width: 100%
}
.has-click{
    cursor: pointer;
    color: var(--color-main);
}
.not-click{
    cursor: not-allowed;
    color: #C0C4CC;
}
.aiCustom-tool-loading{
    width: 100%;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    padding: 8px 0;
}
.aiCustom-tool-loading img{
    height: 22px;
    width: auto;
    display: block;
}
::v-deep .ai-plain-md-output{
    background: transparent !important;
    color: #303133;
    font-size: 14px;
    line-height: 1.6;
    white-space: normal;
    word-break: break-word;
}
::v-deep .ai-plain-md-output .ai-plain-md-block{
    margin: 0;
    padding: 0;
    background: transparent;
    color: inherit;
    font-family: inherit;
    font-size: inherit;
    line-height: inherit;
    white-space: pre-wrap;
    word-break: break-word;
}
::v-deep .ai-plain-md-output .aifupan-block,
::v-deep .ai-plain-md-output .aifupan-card,
::v-deep .ai-plain-md-output .aifupan-chart,
::v-deep .ai-plain-md-output .aifupan-columns,
::v-deep .ai-plain-md-output .aifupan-column,
::v-deep .ai-plain-md-output .aifupan-alert,
::v-deep .ai-plain-md-output .aifupan-statistic{
    all: unset;
    display: block;
}
</style>

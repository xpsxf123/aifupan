/**
 * @file AI 通用混入。
 * @description 负责问答流式请求、内容解析、历史记录同步及交互行为封装。
 */
import elsConfig from './elsConfig.js';
import myUtils from '@/utils/utils.js';
import aiTypeMixin from '@/mixins/aiTypeMixin';
import shareMixin from './shareMixin';
import optimizeFormat from "./optimizeFormat";
import renderAiResponseContent, {AI_RENDER_MODE_MAP} from './aiContentRenderParser';
import mdFormat from '@/utils/mdFormat.js';
import {omit} from 'lodash';
import {copyShareUrl} from '@/utils/common';
import {createCustom, createTip, createTitle, listChannel, createDialogueHtml} from '@/utils/aiCreateEle.js';
import resolveWebVersion from "/src/utils/webVersion.js";
import { options } from 'less';
export default {
    props: {
        flod: {
            type: Boolean,
            default: false
        },
        sentenceMarkData: {
            type: Object,
            default: () => { return {} }
        },
        isCompare: {
            type: Boolean,
            default: false
        },
        aiModel: {
            type: [Number,String,undefined],
            default: undefined
        },
        targetType: {
            type: String,
            default: ''
        },
        uploadScreenshot: {
            type: Number,
            default: 0
        },
        uploadBoard: {
            type: Number,
            default: 0
        },
        askConfig: {
            type: Object,
            default: () => { return {} }
        },
        shareId:{
            type: String,
            default: ''
        },
        readonly:{
            type: Boolean,
            default: false
        },
        pdfName:{
            type: String,
            default: ''
        },
        modelOptions:{
            type: Object,
            default: () => { return {} }
        }
    },
    data() {
        return {
            // 运营 assistant
            // 违规 violation
            elsMap: elsConfig.elsMap,
            historyParagraphMap: {
                assistant: [],
                violation: [],
                scrolling: [],
                dataBoard: [],
                dataCapture: []
            },
            scene: 'all',
            problem: {
                all: null,
                paragraph: null,
                custom: null
            },
            // 延迟载入队列
            delayEls: [],
            // 预加载队伍
            preloadEls: [],
            // 段落数据
            paragraphCode: 0,
            // 添加问题
            addProblem: null,
            pageIndex: 1,
            isMore: false,
            myThrottle: myUtils.throttle(1000),
            sorollTime: null,
            sendAskLoad: false,
            sendAskDomId: '',
            scrollToBottomStop: false,
            pollingHtmlTimeout: null, //生成图表时 的轮询定时器
            pollingCorrectTimeout: null, //ai纠正内容 的轮询定时器
            distillActiveKey: '',
            distillActiveQaCode: '',
        }
    },
    provide() {
        return {
            AiVnode: this
        }
    },
    mixins: [aiTypeMixin, shareMixin, optimizeFormat],
    inject: ['aiLayout'],
    computed: {
        getQuestionContent(){
            return this.sentenceMarkData?.questionContent;
        },
        // 是否使用服务器逻辑
        isUseBack(){
            return this.targetType === 'online' || this.targetType === 'webOnline'
        },
        isShowBtn() {
            return this.aiModel === 0 || this.$store.getters.getMode === 0;
        },
        getFlod() {
            return this.flod;
        },
        getEls() {
            return this.elsMap[this.type]
        },
        getId() {
            if(this.isCompare){
                return this.sentenceMarkData?.info?.ContrastId;
            }
            return this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.uploadFile?.fileId
        },
        isVideo() {
            return !!(this?.sentenceMarkData?.videoInfo && this?.sentenceMarkData?.videoInfo?.VideoId)
        },
        getSourceType() {
            return this.isCompare ? 2 : this.isVideo ? 0 : 1; // 源类型
        },
        getType() {
            return this.getCueType
        },
        getAskUrl(){
            // reviewApiType 为 1 时，使用 backApiURL
            if(this.isUseBack || this.modelOptions?.reviewApiType === 1){
                return `${window.SITE_CONFIG['backApiURL']}/aiRelated/ask`;
            }
            return `${window.SITE_CONFIG["clientApiURL"]}/aiRelated/ask`;
        },
        getBindConfig(){
            return {
                // *当前关联：直播AI数据识图、直播数据看板
                uploadScreenshot: {
                    value: this.uploadScreenshot,
                    label: '【AI数据识图】'
                },
                uploadBoard: {
                    value:this.uploadBoard,
                    label: '【数据看板】'
                },
            }
        },
        getBindConfigLabel(){
            if(this.isViolation){return `${this.scene==='custom'?'以上为定制分析智能体提示词，':''}*违规助手仅用于查违规，改写话术请使用运营助手`};
            if(!this.isAssistant){return''}
            let config = Object.values(this.getBindConfig);
            if(config?.map(d=>d.value)?.reduce((a,b)=>{return a+b},0)>0){
                return [`${this.scene==='custom'?'以上为定制分析智能体提示词，':''}*当前关联：功能模块`,config?.map(d=>{
                    return d?.value ? d.label: ''
                }).filter(d=>d).join('、'), '，点击上方【背景配置】按钮修改'].join('')
            }else{
                return ''
            }
        },
        // 绑定的配置
        bindConfig(){
            return {
                // *当前关联：直播AI数据识图、直播数据看板
                ...this.getBindConfig,
                bindConfigLabel: this.getBindConfigLabel
            }
        },
        httpGetHistoryParagraphList(){
            if(this.isUseBack){
                return this.$httpBack.aiRelated?.getHistoryParagraphList
            }else{
                return this.$httpClient.aiRelated?.getHistoryParagraphList
            }
        }
    },
    watch: {
        flod(val) {
            this.setElsKeyValue('flod', val);
        },
        // elsMap:{
        //     handler(val) {
        //         console.log(val,3333333333)
        //     },
        //     deep: true
        // }
    },
    methods: {
        getDistillStorageKey() {
            return 'ai_distill_record_v1'
        },
        loadDistillStore() {
            try {
                const cache = localStorage.getItem(this.getDistillStorageKey())
                const parsed = cache ? JSON.parse(cache) : {}
                if (!parsed || typeof parsed !== 'object') {
                    return { issues: {} }
                }
                if (!parsed.issues || typeof parsed.issues !== 'object') {
                    parsed.issues = {}
                }
                return parsed
            } catch (e) {
                return { issues: {} }
            }
        },
        saveDistillStore(store) {
            try {
                localStorage.setItem(this.getDistillStorageKey(), JSON.stringify(store || {}))
            } catch (e) {}
        },
        distillHash(text = '') {
            const str = String(text || '')
            let h = 0
            for (let i = 0; i < str.length; i++) {
                h = (h << 5) - h + str.charCodeAt(i)
                h |= 0
            }
            return String(h)
        },
        buildDistillKey(content = '') {
            const normalized = String(content || '').trim().replace(/\s+/g, ' ')
            if (!normalized) return ''
            const head = normalized.slice(0, 120)
            return `${this.getId || ''}_${this.getType || ''}_${this.distillHash(normalized)}_${this.distillHash(head)}`
        },
        ensureDistillIssue(store, key, base = {}) {
            if (!key) return null
            if (!store.issues[key]) {
                store.issues[key] = {
                    key,
                    failCount: 0,
                    cycles: [],
                    createdAt: Date.now(),
                    updatedAt: Date.now(),
                    ...base
                }
            }
            store.issues[key].updatedAt = Date.now()
            return store.issues[key]
        },
        finalizeDistillIssue(store, key) {
            const issue = store?.issues?.[key]
            if (!issue) return
            if (issue.finalizedAt) return
            issue.finalizedAt = Date.now()
            issue.updatedAt = Date.now()
        },
        markDistillNewQuestion(content = '') {
            const key = this.buildDistillKey(content)
            const store = this.loadDistillStore()
            if (this.distillActiveKey && this.distillActiveKey !== key) {
                this.finalizeDistillIssue(store, this.distillActiveKey)
            }
            this.distillActiveKey = key
            const issue = this.ensureDistillIssue(store, key, {
                contentPreview: String(content || '').trim().slice(0, 600)
            })
            if (issue) {
                issue.lastAskAt = Date.now()
            }
            this.saveDistillStore(store)
        },
        recordDistillAnswer({ qaCode, answerId, answerContent }) {
            const store = this.loadDistillStore()
            const key = this.distillActiveKey || ''
            const issue = this.ensureDistillIssue(store, key)
            if (!issue) return
            const item = {
                t: Date.now(),
                qaCode: qaCode || '',
                answerId: answerId || '',
                model: this.modelOptions?.modelName || this.modelOptions?.modelCode || this.modelOptions?.name || '',
                answerPreview: String(answerContent || '').replace(/<[^>]*>/g, '').trim().slice(0, 900)
            }
            issue.cycles.push({
                type: 'answer',
                ...item
            })
            issue.lastAnswerAt = item.t
            if (qaCode) {
                this.distillActiveQaCode = qaCode
            }
            this.saveDistillStore(store)
        },
        recordDistillModify({ key, actionType, detail }) {
            const store = this.loadDistillStore()
            const issue = this.ensureDistillIssue(store, key)
            if (!issue) return
            const nextFailCount = Number(issue.failCount || 0) + 1
            const record = {
                t: Date.now(),
                type: actionType,
                detail: detail || {}
            }
            if (nextFailCount > 4) {
                issue.cycles.push({
                    t: Date.now(),
                    type: 'reset',
                    reason: 'over_4_fail',
                    lastRecord: record
                })
                issue.failCount = 1
            } else {
                issue.failCount = nextFailCount
            }
            issue.cycles.push(record)
            issue.lastModifyAt = record.t
            this.saveDistillStore(store)
        },
        // scrollToBottomStop
        handleScrollToTop(event) {
            // 检查滚轮方向
            if (event.deltaY < 0 && !this.scrollToBottomStop && this.sendAskLoad) {
                this.scrollToBottomStop = true;
            }
        },
        /**
         * 设置els的key值
         * 此函数用于在一组元素中设置特定键的值这些元素是通过`getEls`方法获取的
         * 它会遍历每个元素，并根据提供的`optionKey`和`key`参数设置相应的值
         *
         * @param {string} key - 要设置的键名
         * @param {*} value - 要设置的值
         * @param {string} [optionKey='bind'] - 元素上包含键值对的属性名默认为'bind'
         */
        setElsKeyValue(key, value, optionKey = 'bind') {
            // 遍历获取的元素集合
            this.getEls.forEach((item) => {
                // 检查元素的optionKey属性中是否存在指定的键
                if (typeof item?.[optionKey]?.[key] !== 'undefined') {
                    // 如果键存在，则设置其值为新的值
                    this.$set(item[optionKey], key, value);
                }
            });
        },
        addTip(option) {
            let o = createTip({type: this.type});
            if (option?.output) {
                return o;
            }
            this.getEls.push(o);
        },
        getSceneProblem(scene) {
            return this.problem[scene || this.scene];
        },
        getParagraphItem(code) {
            code = code === 0 ? 'all' : code;
            return this.historyParagraphMap[this.type]?.find(d => {
                return code === d.value
            })
        },
        /**
         * 添加问题Dom配置
         * @param {Object} option - 配置选项
         * @param {Function} callback - 点击问题时的回调函数
         */
        addProblemEls(option, callback) {
            // 创建问题元素配置对象
            let p = {
                el: 'problem', // 元素标识为问题
                on: {
                    click: (data) => {
                        if (this.sendAskLoad) { return }
                        // 当数据类型为违规时，如果回调函数存在，则执行回调函数
                        if (data?.data?.type === 'violation') {
                            if (typeof callback === 'function') {
                                callback({
                                    ...data,
                                    data: {
                                        ...data.data,
                                        paragraphCode: this.paragraphCode,
                                        scene: this.scene,
                                        item: this.getParagraphItem(this.paragraphCode)
                                    }
                                })
                            }
                        } else {
                            // 否则执行问题点击方法
                            this.problemClick(data);
                        }
                    }
                },
                bind: {
                    // 绑定折叠状态方法
                    flod: this.getFlod,
                    // 展开绑定的其他配置选项
                    ...option || {}
                }
            };
            // 如果配置选项中包含output属性，则返回问题元素配置对象
            if (option?.output) {
                return p;
            }
            // 如果最后一个问答模块为问题模块，则不会在执行添加，只会刷新。因为不会存在渲染两次的问答模块
            if (this.getEls[this.getEls?.length - 1]?.el === 'problem') {
                this.$set(this.getEls, this.getEls?.length - 1, p);
            } else {
                // 否则添加新的问题元素配置到元素配置列表中
                this.addEls(p);
            }
        },
        // 初始化添加问答
        /**
         * 初始化添加问题功能
         *
         * 此函数旨在配置一个用于添加问题的函数，通过合并初始配置和场景特定配置，并绑定回调函数
         * 它允许在特定场景下灵活地添加问题
         *
         * @param {Object} option - 初始配置对象，包含添加问题所需的基本配置
         * @param {Function} callback - 回调函数，问题添加后将被调用
         */
        initAddProblem(option, callback) {
            // 配置添加问题函数
            this.addProblem = (options, scene) => {
                this.scene = scene;
                // 合并配置项，包括初始配置、场景问题配置和新传入的配置，然后调用addProblemEls函数添加问题
                // 这种方式允许在不同场景下灵活地添加问题，并且可以重用代码
                return this.addProblemEls({
                    items: this.getSceneProblem(scene),
                    ...option,
                    ...options
                }, callback);
            };
        },

        /**
         * 处理问题点击事件
         * 本函数使用了节流技术来限制发送询问的频率，以避免短时间内多次发送相同或不同的问题
         * @param {Object} data - 包含问题信息的数据对象，通过可选链操作安全地访问属性
         */
        problemClick(data) {
            // 执行我的节流函数，用于控制发送问题的频率
            this.myThrottle(() => {
                // 调用发送询问的方法，传递关键词和实际内容
                this.sendAsk(data?.data?.cueWord, {
                    realContent: data?.data?.problem,
                    cueWordsId: data?.data?.id,
                    cueWordsType: 0
                });
            });
        },
        customPromptClick(data){
            // 执行我的节流函数，用于控制发送问题的频率
            this.myThrottle(() => {
                const placeholderKeys = Array.isArray(data?.placeholderKeys) && data.placeholderKeys.length
                    ? data.placeholderKeys
                    : Object.keys(data?.promptMoreConfig?.dynamicConfigs || {}).filter((key) => {
                        return !!data?.promptMoreConfig?.dynamicConfigs?.[key]
                    })
                // 调用发送询问的方法，传递关键词和实际内容
                this.sendAsk(data?.title, {
                    realContent: data?.prompt,
                    cueWordsId: data?.id,
                    cueWordsType: 1,
                    promptMoreConfig: data?.promptMoreConfig,
                    placeholderKeys
                });
            });
        },
        /**
         * 滚动到页面底部
         * 此函数用于在不同情况下将滚动条移动到页面的底部
         * 它可以根据传入的类型参数，执行不同的滚动操作
         *
         * @param {String} type - 滚动的类型，可选值为'more'、'init'或其它
         */
        scrollToBottom(type,dom) {
            if(this.scrollToBottomStop){return};
            // 确保DOM更新后执行滚动操作
            this.$nextTick(() => {
                // 清除上一次的定时器，避免内存泄漏
                clearTimeout(this.sorollTime);
                // 设置一个新的定时器来执行滚动操作
                this.sorollTime = setTimeout(() => {
                    if (type === 'more') {
                        // 对于'more'类型，目前不需要执行滚动操作
                        dom?.scrollIntoView({
                            // behavior: 'smooth' // 平滑滚动效果
                        });
                        return
                    }
                    // 获取需要滚动的DOM元素
                    let scrollDiv = document.getElementById(`${this.type}_dom`);
                    // 根据传入的type参数执行不同的滚动操作
                    if (type === 'init') {
                        // 对于'init'类型，将滚动条设置到最底部
                        scrollDiv.scrollTop = scrollDiv?.scrollHeight;
                        return;
                    }
                    // 对于其他情况，将滚动条平滑地设置到最底部
                    scrollDiv.scrollTo({
                        top: scrollDiv.scrollHeight,
                        behavior: 'smooth'
                    });
                }, 10)
            });
        },
        /**
         * 将混合文本转换为 HTML 文本
         *
         * 此函数会优先解析文本中的 JSON 结构片段，将其转换为 HTML 后，
         * 再与 Markdown 正文一起交给统一渲染器处理，兼容混合输出场景。
         *
         * @param {string} mdText - 输入的混合文本
         * @returns {string} 转换后的 HTML 文本
         */
        stripAifupanTagFragments(text = '') {
            return String(text || '')
                .replace(/<\/?aifupan-[^>]*>/gi, '')
                .replace(/<\/?aifupan-[^\n]*$/gi, '');
        },
        /**
         * @description Markdown 渲染前清洗深度思考内容：
         * 去除 aifupan 标签片段与 HTML 标签，<br>/<p> 转换为换行，仅保留文本，避免标签被当作正文结构渲染。
         * @param {string} text 深度思考原始文本。
         * @returns {string} 清洗后可用于 Markdown 渲染的文本。
         */
        sanitizeDeepThinkingBeforeMarkdown(text = '') {
            let source = String(text ?? '');
            if (!source) return '';
            source = this.stripAifupanTagFragments(source);
            source = source
                .replace(/<\s*br\s*\/?\s*>/gi, '\n')
                .replace(/<\s*\/\s*p\s*>/gi, '\n\n')
                .replace(/<\s*p\b[^>]*>/gi, '\n\n');
            source = source.replace(/<\s*(script|style|iframe|object|embed|link|meta)\b[\s\S]*?>[\s\S]*?<\s*\/\s*\1\s*>/gi, '');
            source = source.replace(/<\s*(script|style|iframe|object|embed|link|meta)\b[\s\S]*?\/\s*>/gi, '');
            source = source.replace(/<\/?[^>\n]+>/g, '');
            source = source.replace(/<[^>\n]*$/g, '');
            return source;
        },
        /**
         * @description 判断当前流式文本块是否为后端返回的完整内容块。
         * 最后一次流式输出会一次性下发完整问答内容（正文内嵌 deepThinking 块），
         * 识别该块后改用整体替换累加内容，避免追加导致深度思考与正文临时重复。
         * @param {string} text 文本块内容（type=text）。
         * @returns {boolean} 是完整内容块返回 true。
         */
        isFullAnswerChunk(text = '') {
            return /<div\b[^>]*class\s*=\s*(['"])[^'"]*\bdeepThinking\b/i.test(String(text ?? ''));
        },
        /**
         * @description 提取正文中内嵌的 deepThinking 块。
         * 后端完整内容会把深度思考包裹进正文区域，需将其从正文中分离为独立深度思考内容，
         * 避免深度思考与正文重复展示。
         * @param {string} text 正文内容。
         * @returns {null|{answer:string,thinking:string,complete:boolean}} 提取结果，无内嵌块时返回 null。
         */
        extractInlineDeepThinkingBlock(text = '') {
            const source = String(text ?? '');
            if (!source) return null;
            const startMatch = source.match(/<div\b[^>]*class\s*=\s*(['"])[^'"]*\bdeepThinking\b[^'"]*\1[^>]*>/i);
            if (!startMatch || startMatch.index === undefined) {
                return null;
            }
            const startIndex = startMatch.index;
            const startEndIndex = startIndex + startMatch[0].length;
            const endIndex = source.indexOf('</div>', startEndIndex);
            if (endIndex < 0) {
                return {
                    answer: source.replace(startMatch[0], ''),
                    thinking: '',
                    complete: false
                };
            }
            return {
                answer: `${source.slice(0, startIndex)}${source.slice(endIndex + 6)}`,
                thinking: source.slice(startEndIndex, endIndex),
                complete: true
            };
        },
        /**
         * @description 标准化正文与深度思考内容。
         * 深度思考通过流式 type（thinking/text）区分；当后端完整内容把深度思考包裹进正文时，
         * 从正文中提取内联 deepThinking 块单独渲染，并对深度思考做渲染前清洗，避免与正文重复展示。
         * @param {string} answerContent 正文内容。
         * @param {string} thinkingContent 深度思考内容（type=thinking 累积结果）。
         * @returns {{answerText: string, thinkingText: string}} 标准化后的正文与深度思考文本。
         */
        normalizeDeepThinkingForRender(answerContent = '', thinkingContent = '') {
            let answerText = String(answerContent ?? '');
            let thinkingText = String(thinkingContent ?? '');
            const extracted = this.extractInlineDeepThinkingBlock(answerText);
            if (extracted) {
                answerText = extracted.answer || '';
                if (!thinkingText.trim()) {
                    thinkingText = extracted.thinking || '';
                }
                if (!extracted.complete) {
                    thinkingText = '';
                }
            }
            const safeThinking = this.sanitizeDeepThinkingBeforeMarkdown(thinkingText);
            return {
                answerText,
                thinkingText: safeThinking
            };
        },
        normalizePlainMarkdownText(text = '') {
            const sourceText = String(text ?? '');
            const trimText = sourceText.trim();
            if (!trimText) {
                return '';
            }
            if (/^```(?:markdown)?\s*\n/i.test(trimText) && /\n```\s*$/i.test(trimText)) {
                return trimText
                    .replace(/^```(?:markdown)?\s*\n/i, '')
                    .replace(/\n```\s*$/i, '');
            }
            return sourceText;
        },
        escapeHtmlText(text = '') {
            return String(text ?? '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        },
        /**
         * @description 流式输出阶段剔除所有自定义标签与不完整 HTML 片段。
         * 流式期间仅保留纯 Markdown 文本渲染（表格/图表等块级元素延迟到完成后再统一渲染），
         * 剔除以下四类内容：
         * 1. 完整 <aifupan-*> 自定义标签
         * 2. 末尾未闭合的 <aifupan- 片段
         * 3. 不完整的 <a ... 开头片段（AI 流式输出中偶尔出现的 HTML 锚点残片）
         * 4. 行末不完整的 </ 或 < 起始字符
         * @param {string} text 流式输出中的正文内容。
         * @returns {string} 剔除后仅含纯 Markdown 的文本。
         */
        stripCustomTagsForStreaming(text = '') {
            return String(text ?? '')
                .replace(/<\/?aifupan-[^>]*>/gi, '')
                .replace(/<\/?aifupan-[^\n]*$/gi, '')
                .replace(/<a\s+[^>]*$/gi, '')
                .replace(/<\/?\s*$/g, '');
        },
        renderPlainMarkdownContent(mdText = '', option = {}) {
            const answerText = this.normalizePlainMarkdownText(this.stripAifupanTagFragments(mdText || ''));
            const thinkingText = this.normalizePlainMarkdownText(this.stripAifupanTagFragments(option?.thinkingContent || ''));
            const answerBlock = `<pre class="ai-plain-md-block">${this.escapeHtmlText(answerText || '')}</pre>`;
            const thinkingSection = thinkingText
                ? `<div class="deepThinkingSection"><div class="deepThinkingTitle">深度思考<span class="deepThinkingToggle"></span></div><div class="deepThinking"><pre class="ai-plain-md-block">${this.escapeHtmlText(thinkingText)}</pre></div></div>`
                : '';
            return `<div class="ai-plain-md-output">${thinkingSection}${answerBlock}</div>`;
        },
        /**
         * @description 构建深度思考流式气泡 HTML（仅用于深度思考流式输出阶段）。
         * 深度思考内容不做任何渲染处理（md/html/json 均不解析），
         * 仅做 HTML 转义后按接收到的流式原文直接输出；
         * 「深度思考」按钮与一行流式容器同行展示，下方不渲染任何内容。
         * 深度思考流式结束（正文开始返回）后，本气泡不再渲染，
         * 改回原有深度思考板块的展示与交互逻辑。
         * @param {string} thinkingText 深度思考原始内容（type=thinking 累积结果）。
         * @returns {string} 气泡 HTML 字符串，无内容时返回空字符串。
         */
        buildThinkingBubbleHtml(thinkingText = '') {
            const rawText = String(thinkingText ?? '');
            if (!rawText.trim()) {
                return '';
            }
            // 只做 HTML 转义防止标签被真实解析，内容本身保持原样输出
            const escapedText = this.escapeHtmlText(rawText);
            return `<div class="deepThinkingBubbleSection is-collapsed is-streaming">
                <div class="deepThinkingBubbleHeader">
                    <div class="deepThinkingTitle">深度思考<span class="deepThinkingToggle"></span></div>
                    <div class="deepThinkingBubbleLine">
                        <div class="deepThinkingBubbleClip"><span>${escapedText}</span></div>
                    </div>
                </div>
                <div class="deepThinkingBubbleFull">${escapedText}</div>
            </div>`;
        },
        getMdText(mdText, option = {}) {
            const rawThinking = String(option?.thinkingContent ?? '');
            // 深度思考流式输出阶段（正文还没开始返回）：
            // 仅展示一行流式气泡，内容不做任何渲染处理，直接按接收原文输出；
            // 此时不走渲染器，避免输出空的渲染根节点形成空白内容块
            if (!String(mdText || '').trim() && rawThinking.trim()) {
                return this.buildThinkingBubbleHtml(rawThinking);
            }
            // 正文开始返回（深度思考流式结束）或无思考内容：
            // 流式阶段：剔除所有自定义标签与不完整 HTML 片段，仅渲染纯 Markdown，
            // 表格/图表等块级元素延迟到流式完成后再统一渲染，避免标签未闭合时的卡顿
            // 非流式阶段（历史记录 / 流式完成后的最终渲染）：走完整渲染器
            const effectiveAnswer = option?.streaming
                ? this.stripCustomTagsForStreaming(mdText || '')
                : (mdText || '');
            const normalized = this.normalizeDeepThinkingForRender(effectiveAnswer, rawThinking);
            const normalizedOption = {
                ...(option || {}),
                thinkingContent: normalized.thinkingText || ''
            };
            if (option?.plainTextMode) {
                return renderAiResponseContent(normalized.answerText, {
                    ...normalizedOption,
                    parserMode: AI_RENDER_MODE_MAP.PURE_MD
                })?.html || '';
            }
            return renderAiResponseContent(normalized.answerText, normalizedOption)?.html || '';
        },
        /**
         * 标准化历史会话项，确保历史记录也走统一渲染器。
         *
         * @param {Array} items 历史会话列表。
         * @returns {Array} 处理后的历史会话列表。
         */
        normalizeHistoryEls(items = []) {
            return (items || []).map(item => {
                if (!item || item === 'tip') {
                    return item;
                }
                if (item.el === 'aiCustom') {
                    return this.normalizeHistoryCustomItem(item);
                }
                if (['A', 'Q'].includes(item.type)) {
                    return item;
                }
                if (item.content && !item.el) {
                    return {
                        ...item,
                        type: item.type || 'Q'
                    };
                }
                return item;
            });
        },
        /**
         * 标准化单个历史回答组件。
         *
         * @param {Object} item 历史项。
         * @returns {Object} 处理后的历史项。
         */
        normalizeHistoryCustomItem(item = {}) {
            const identity = item?.bind?.identity || item?.identity || '';
            if (identity === 'self') {
                return item;
            }
            const rawContent = this.getHistoryRawContent(item);
            if (!rawContent) {
                return item;
            }
            return {
                ...item,
                bind: {
                    ...item.bind,
                    html: createDialogueHtml(this.getMdText(rawContent), 'left', item.id),
                    rawContent,
                    rawAnswerContent: rawContent
                }
            };
        },
        /**
         * 获取历史记录中的原始文本。
         *
         * @param {Object} item 历史项。
         * @returns {string} 原始文本。
         */
        getHistoryRawContent(item = {}) {
            const candidateList = [
                this.normalizeHistoryText(item?.content),
                this.normalizeHistoryText(item?.bind?.rawContent),
                this.normalizeHistoryText(item?.bind?.content)
            ];
            return candidateList.find(text => typeof text === 'string' && text.trim()) || '';
        },
        /**
         * 标准化历史文本内容。
         *
         * @param {string|Object} value 历史文本值。
         * @returns {string} 标准化后的文本。
         */
        normalizeHistoryText(value) {
            if (typeof value === 'string') {
                return value;
            }
            if (!value || typeof value !== 'object') {
                return '';
            }
            const candidateList = [
                value?.content,
                value?.rawContent,
                value?.text,
                value?.value,
                value?.html
            ];
            return candidateList.find(text => typeof text === 'string' && text.trim()) || '';
        },
        applyServiceAiRender(targetItem, {
            answerContent = '',
            thinkingContent = '',
            id = '',
            mdOption = {}
        } = {}) {
            const item = targetItem || {};
            const bind = item.bind || item;
            if (!bind) return;
            const targetId = id || item.id || bind.id;
            this.$set(bind, 'rawAnswerContent', answerContent || '');
            this.$set(bind, 'rawThinkingContent', thinkingContent || '');
            const renderHtml = this.getMdText(answerContent || '', {
                ...(mdOption || {}),
                thinkingContent: thinkingContent || ''
            });
            this.$set(bind, 'html', createDialogueHtml(renderHtml || '', 'left', targetId));
        },
        // 直接载入
        addEls(item) {
            while (this.preloadEls?.length) {
                let shift = this.preloadEls.shift();
                if (typeof shift === 'function') {
                    shift();
                } else if (typeof shift === 'object') {
                    this.getEls.push(item);
                }
            }
            if(item.el === 'aiCustom'){
                item.otherOption = {};
            }
            this.getEls.push(item);
            // 如果载入节点时，检查是否又延迟载入节点，如果有则持续延迟载入所有节点
            if (this.delayEls?.length) {
                let shift = this.delayEls?.shift();
                if (typeof shift === 'function') {
                    shift();
                } else if (typeof shift === 'object') {
                    this.addEls(shift);
                }
                return;
            }
            this.scrollToBottom();
        },
        // 延迟载入
        delayAddEls(item, index) {
            if (typeof index !== 'undefined') {
                this.delayEls[index] = item;
                return
            }
            this.delayEls.push(item);
        },
        // 预加载，在下次加载elDom之前执行或加载组件
        /**
         * 将元素添加到预加载数组中
         *
         * 此函数的作用是将传入的元素添加到一个预定义的预加载数组（this.preloadEls）中
         * 如果提供了索引参数，并且该索引参数不是一个未定义的值，则将元素插入到指定索引位置
         * 否则，将元素添加到数组的末尾
         *
         * @param {any} item 要添加到预加载数组中的元素，可以是任何类型的值
         * @param {number} [index] 可选参数，指定将元素插入到数组中的特定索引位置如果未提供，则将元素添加到数组末尾
         */
        preloadAddEls(item, index) {
            // 检查index参数是否已定义，如果已定义，则将item插入到指定索引位置
            if (typeof index !== 'undefined') {
                this.preloadEls[index] = item;
                return
            }
            // 如果index未定义，则将item添加到数组末尾
            this.preloadEls.push(item)
        },
        /**
         * @param {Object|string} data - 包含对话框基本数据的对象，如html内容、身份标识和对齐方式如果为字符串，则直接用作html内容
         * @param {Object} option - 可选配置对象，包含额外的配置信息如配置ID、输出配置等
         */
        createCustom(data, option = {}) {
            const o = createCustom(data, {
                ...option,
                type: this.type
            });
            // 如果用户指定了输出配置，则返回对话框配置对象，否则直接将对话框添加到元素中
            if (option?.output) {
                return o;
            }
            this.addEls(o);
        },

        toTop(dom) {
            if(this.scrollToBottomStop){return};
            // 对于其他情况，将滚动条平滑地设置到最底部
            dom.scrollTo({
                top: dom.scrollHeight,
                behavior: 'smooth'
            });
        },
        getVideoTime(data){
            if(!data || data?.fileInfo?.fileType === 2){ return null };
            const {sentenceMarkList, startIndex, endIndex} = data;
            if(this.getQuestionContent){
                return [ startIndex, endIndex ];
            }else{
                let s = sentenceMarkList[0],e = sentenceMarkList[sentenceMarkList.length - 1];
                if(!s || !e){return 'overlap'}
                return [ s?.startTime, e?.endTime ];
            }
        },
        getVideoTimeList(){
            let videoTimeOneList = this.getVideoTime((this.sentenceMarkData?.data1 || this.sentenceMarkData)),
                videoTimeTwoList = this.getVideoTime(this.sentenceMarkData?.data2);
            if(this.isCompare){
                return { videoTimeOneList, videoTimeTwoList }
            }else{
                return { videoTimeOneList }
            }
        },
        chunkStringSplit(str){
            return str?.replace(/\n+/g, '\n')?.replace(/(event:|data:)/g, '\n$1')?.trim()?.split('\n');
        },
        async getAiOptimizePurpose() {
            if(!this.sentenceMarkData.data1?.videoInfo?.VideoId) return
            const result = await this.$httpBack.video.getOptimizePurpose({
                sourceId: this.sentenceMarkData.data1?.videoInfo?.VideoId
            })
            if (result.code !== 0) return
            return result?.data
        },
        async sendAsk(content, option) {
            if (!this.isCompare) {
                await this.sendAfter(content, option);
                return;
            }

            const { syncScene } = this.sentenceMarkData?.info || {};
            if (syncScene !== 1) {
                await this.sendAfter(content, option);
                return;
            }

            const res = await this.getAiOptimizePurpose();
            if (!res?.id) {
                await this.sendAfter(content, option);
                return;
            }

            this.$confirm('检测到您记录了本场优化动作，请确认AI分析是否带上优化动作和目的进行分析？', '温馨提示', {
                confirmButtonText: '带上分析',
                cancelButtonText: '不带分析',
                customClass: 'edit-file-name',
                showClose: false,
                closeOnClickModal: false,
                closeOnPressEscape: false,
                center: true
            }).then(async () => {
                await this.sendAfter(content, {...option, optimizeActions: 1});
            }).catch(async () => {
                await this.sendAfter(content, option);
            });
        },
        // 发送问答
        async sendAfter(content, option) {
            if (this.sendAskLoad) { return }
            this.sendAskLoad = true;
            this.markDistillNewQuestion(option?.realContent || content);
            const optimize = option.optimizeActions ? {optimizeActions: option.optimizeActions} : {}
            const promptMoreConfig = option?.promptMoreConfig || {}
            const baseMoreConfig = this.askConfig?.moreConfig || {}
            const sanitizedBaseMoreConfig = {
                basicData: baseMoreConfig?.basicData || {},
                dynamicConfigs: baseMoreConfig?.dynamicConfigs || {}
            }
            const mergedPromptMoreConfig = {
                basicData: {
                    ...(sanitizedBaseMoreConfig?.basicData || {}),
                    ...(promptMoreConfig?.basicData || {})
                },
                dynamicConfigs: {
                    ...(sanitizedBaseMoreConfig?.dynamicConfigs || {}),
                    ...(promptMoreConfig?.dynamicConfigs || {})
                }
            }
            const placeholderKeys = Array.isArray(this.askConfig?.placeholderKeys)
                ? this.askConfig.placeholderKeys
                : []
            // 深拷贝数据
            let o = JSON.parse(JSON.stringify({
                aiModel: option?.modelType !== undefined ? option?.modelType : this.aiModel,
                content: content,
                realContent: option.realContent || content,
                type: this.getType, // 问答类型
                paragraphCode: this.paragraphCode || 0, // 段落code
                sourceId: this.getId, // 源id
                sourceType: this.getSourceType, // 源类型
                ...this.getVideoTimeList(),
                uploadScreenshot: this.uploadScreenshot,
                uploadBoard: this.uploadBoard,
                placeholderKeys,
                ...omit(option, ['optimizeActions', 'modelType', 'optimizeText', 'extraRequire', 'promptMoreConfig']),
                otherObj: {
                    ...this.askConfig,
                    basicDataConfig: Object.keys(promptMoreConfig || {}).length ? mergedPromptMoreConfig?.basicData : this.askConfig?.basicDataConfig,
                    moreConfig: Object.keys(promptMoreConfig || {}).length ? mergedPromptMoreConfig : sanitizedBaseMoreConfig,
                    optimizeText: option.optimizeText,
                    extraRequire: option.extraRequire,
                    backgroundConfigOne:{
                        ...this.askConfig?.backgroundConfigOne,
                        ...optimize
                    }
                }
            }));

            if(o.videoTimeOneList === 'overlap' || o.videoTimeTwoList === 'overlap'){
                this.$message.error('视频切片时间重叠，请重新选择')
                this.sendAskLoad = false;
                return;
            }
            // return;
            this.scrollToBottomStop = false;
            let selfContent = content;
            // 创建对话
            this.createCustom({
                html: selfContent,
                identity: 'self'
            });

            let domId = `dom_${(new Date()).getTime()}`;
            this.sendAskDomId = domId;
            // 创建加载状态的服务返回对话框
            this.createCustom({
                html: ' ',
                identity: 'service'
            }, {
                config: {
                    loading: true
                },
                id: domId
            });
            try {
                // 将数据转换为查询字符串
                // 发起 fetch 请求
                 const webVersion = resolveWebVersion();
                const response = await fetch(this.getAskUrl, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Token: this.$store.state.token || "",
                        ...(webVersion ? { webVersion } : {}),
                    },
                    body: JSON.stringify(o)
                });
                // 获取最后一个元素
                let Q = this.getEls[this.getEls?.length - 1];
                // 设置加载状态为false
                // this.$set(Q.bind, 'loading', false);

                // 获取需要滚动的DOM元素
                let scrollDiv = document.getElementById(`${this.type}_dom`);
                // 准备读取响应体
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let answerRenderContent = '';
                let thinkingRenderContent = '';
                let res = null;
                let lock = true;
                let streamRenderTimer = null;
                // 记录错误次数
                let errNum = 0;
                let errorArray = [];
                const getCurrentRenderOption = () => ({
                    thinkingContent: thinkingRenderContent,
                    // 流式阶段剔除自定义标签，仅渲染纯 Markdown；表格/图表等块级元素在流式完成后统一渲染
                    streaming: true
                });
                const clearStreamRenderTimer = () => {
                    if (streamRenderTimer) {
                        clearTimeout(streamRenderTimer);
                        streamRenderTimer = null;
                    }
                };
                const flushStreamRender = (force = false) => {
                    clearStreamRenderTimer();
                    // 流式阶段已通过 stripCustomTagsForStreaming 剔除自定义标签，
                    // 不再需要等待 aifupan 标签闭合，直接渲染纯 MD 内容
                    const renderOption = getCurrentRenderOption();
                    this.applyServiceAiRender(Q, {
                        answerContent: answerRenderContent,
                        thinkingContent: thinkingRenderContent,
                        id: Q.id || domId,
                        mdOption: renderOption
                    });
                    this.toTop(scrollDiv);
                };
                const scheduleStreamRender = () => {
                    // 取消流速限制：每个流式分片到达后立即渲染，不再做 220ms 防抖合并
                    flushStreamRender(false);
                };
                // 循环读取数据
                while (true) {
                    // 读取数据块
                    const { done, value } = await reader.read();
                    if (done) break;

                    // 解码数据块
                    const chunk = decoder.decode(value, { stream: true });
                    // 处理数据块，分割成行
                    let lines = this.chunkStringSplit(chunk);
                    let currentEvent = '';
                    let joinError = false;
                    // 遍历每一行数据
                    while ((lines?.length && errNum<10)) {
                        let line = lines.shift();
                        if(line.startsWith('join:')){
                            joinError = true;
                        }
                        if (line.startsWith('event:')) {
                            // 记录新的事件类型
                            currentEvent = line.slice('event:'.length).trim();
                        } else if (line.startsWith('data:')) {
                            if(currentEvent === 'stop'){
                                continue;
                            }
                            // 如果当前行以 data: 开头
                            let data = null
                            try{
                                data = JSON.parse((line.slice('data:'.length).trim()));
                                if(joinError){
                                    errorArray = [];
                                }
                            }catch(e){
                                errNum++;
                                if(!joinError){
                                    errorArray.push(`event:${currentEvent}\n`+line);
                                }
                                continue;
                            }

                            //第一次返回数据在取消加载
                            if( data.content && lock){
                                lock = false; // 刷新锁状态
                                // 更新UI，显示加载完成
                                this.$set(Q.bind, 'loading', false);
                            }
                            // 根据事件类型处理数据
                            if(currentEvent === 'warning'){
                                clearStreamRenderTimer();
                                answerRenderContent = data.msg || '';
                                thinkingRenderContent = '';
                                this.applyServiceAiRender(Q, {
                                    answerContent: answerRenderContent,
                                    thinkingContent: thinkingRenderContent,
                                    id: Q.id || domId,
                                    mdOption: getCurrentRenderOption()
                                });
                                this.toTop(scrollDiv);
                            }if (currentEvent === 'error') {
                                clearStreamRenderTimer();
                                answerRenderContent = data.msg || '';
                                thinkingRenderContent = '';
                                this.applyServiceAiRender(Q, {
                                    answerContent: answerRenderContent,
                                    thinkingContent: thinkingRenderContent,
                                    id: Q.id || domId,
                                    mdOption: getCurrentRenderOption()
                                });
                                this.toTop(scrollDiv);
                                throw new Error(JSON.stringify(data));
                            } else if (currentEvent === 'returnData') {
                                res = data;
                                //慢总新需求 如果AI回答格式有问题，直接弹出一个窗
                                if(data?.checkAiContent){
                                    const id = data?.answer?.id;
                                    this.optimizeFormat(() => this.aiCorrectFormat(id))
                                }
                            } else {
                                // 更新文本内容
                                if(data.type === "thinking"){
                                    thinkingRenderContent += data.content || '';
                                }else if(this.isFullAnswerChunk(data.content || '')){
                                    // 最后一次流式输出下发完整内容（正文内嵌深度思考块）：
                                    // 整体替换正文并清空流式累积的临时思考内容，
                                    // 渲染时由提取逻辑从完整内容中还原深度思考，保证只有一份
                                    answerRenderContent = data.content || '';
                                    thinkingRenderContent = '';
                                }else{
                                    answerRenderContent += data.content || '';
                                }
                                if(data.content){
                                    scheduleStreamRender();
                                }
                            }
                        }else if(line){
                            // 如果没有加入错误
                            if(!joinError){
                                // 将当前行添加到错误数组中
                                errorArray.push(line);
                                // 如果错误数组中有有效元素且错误数量小于10
                                if(errorArray?.filter(d=>!!d)?.length>0 && errNum<10 ){
                                    // 将错误信息合并为字符串并拆分成块，然后添加到行的开头
                                    let joinLs = this.chunkStringSplit(`join: error\n ${errorArray.join('')}`);
                                    lines.unshift(...joinLs);
                                }
                            }
                        }
                    }
                }

                if((answerRenderContent || thinkingRenderContent) && errNum<=0){
                    flushStreamRender(true);
                }

                // 如果没有结果数据，则返回
                if (!res) {
                    this.scrollToBottomStop = false;
                    return
                }


                const {code: problemCode, id: problemId, content, cueWordsId, cueWordsType} = res?.problem;
                const { code: answerCode, id: answerId, qaCode } = res?.answer;
                const finalAnswerContent = res?.answer?.content || answerRenderContent;
                // 后端完整内容中已内嵌深度思考（deepThinking 标记）时，清空流式累积的思考内容，避免重复展示
                const finalHasThinking = finalAnswerContent?.indexOf('deepThinking') >= 0;
                if(finalAnswerContent){
                    answerRenderContent = finalAnswerContent;
                    thinkingRenderContent = finalHasThinking ? '' : thinkingRenderContent;
                    this.applyServiceAiRender(Q, {
                        answerContent: answerRenderContent,
                        thinkingContent: thinkingRenderContent,
                        id: Q.id || domId,
                        // 流式完成后的最终渲染：不传 streaming，走完整渲染器（表格/图表/标签全部格式化）
                        mdOption: { thinkingContent: thinkingRenderContent }
                    });
                    this.toTop(scrollDiv);
                }

                this.$set(this.getEls[this.getEls?.length - 2].bind, 'code', problemCode);
                this.$set(this.getEls[this.getEls?.length - 2].bind, 'qaCode', qaCode);
                this.$set(this.getEls[this.getEls?.length - 2].bind, 'originalCueWordsInfo', { //原始cueWords信息  用于重新提问
                    content: content,
                    cueWordsId: cueWordsId,
                    cueWordsType: cueWordsType
                });
                this.$set(this.getEls[this.getEls?.length - 2], 'id', problemId);
                this.$set(this.getEls[this.getEls?.length - 2], 'qaCode', qaCode);

                this.$set(Q, 'id', answerId);
                this.$set(Q.bind, 'code', answerCode);
                this.$set(Q.bind, 'qaCode', qaCode);
                this.$set(Q.bind, 'addEvent', true);
                this.applyServiceAiRender(Q, {
                    answerContent: answerRenderContent,
                    thinkingContent: thinkingRenderContent,
                    id: answerId,
                    mdOption: {thinkingContent: thinkingRenderContent}
                });
                this.$set(Q.bind, 'originalCueWordsInfo', { //原始cueWords信息  用于重新提问 此处可以删除，但防止意外 先留着
                    content: content,
                    cueWordsId: cueWordsId,
                    cueWordsType: cueWordsType
                });
                this.$set(Q.bind, 'generateHtmlInfo', {
                    htmlStatus: 0,
                    htmlDomainName:'',
                    htmlSavePath:'',
                });
                this.$set(Q.bind, 'aiCorrectFormatInfo', {
                    aiCorrectStatus: 0,
                    aiCorrectType: '',
                    aiCorrectError: '',
                });
                //  储存提问结构
                // 储存ai回答问题结构
                // this.addStructure([
                //     {
                //         code: problemCode,
                //         content: {
                //             el: 'aiCustom',
                //             identity: 'self',
                //             code: problemCode
                //         }
                //     }, {
                //         code: answerCode,
                //         content: {
                //             el: 'aiCustom',
                //             identity: 'service',
                //             code: answerCode
                //         }
                //     }
                // ]);
                this.recordDistillAnswer({
                    qaCode,
                    answerId,
                    answerContent: finalAnswerContent || answerRenderContent
                })
                this.sendAskLoad = false;
                this.sendAskDomId = '';
                // 更新UI，显示加载完成
                this.$set(Q.bind, 'loading', false);
                if (res?.property?.surplusNum <= 0) {
                    this.aiLayout.show();
                }
                this.scrollToBottomStop = false;
            } catch (err) {
                let errObj = {}
                console.log({err},'-----err')
                try{
                    errObj = JSON.parse(err.message);
                }catch(err){
                    console.error(err);
                }
                if (errObj.code === 7001) {
                    this.aiLayout.show();
                }
                this.sendAskLoad = false;
                this.sendAskDomId = '';
                let Q = this.getEls[this.getEls?.length - 1];
                this.$set(Q.bind, 'loading', false);
                this.$set(Q.bind, 'addEvent', true);
                this.scrollToBottomStop = false;
            }
        },
        // 添加历史记录数据
        addStructure(datas) {
            // if(this.isUseBack){return}
            // let list = (Array.isArray(datas) ? datas : [datas])?.map(d => {
            //     return {
            //         code: d.code,
            //         content: JSON.stringify(d.content)
            //     }
            // });

            // this.$httpClient.aiRelated?.addStructure({
            //     type: this.getType,
            //     sourceId: this.getId,
            //     sourceType: this.getSourceType,
            //     contentList: list,
            // });
        },
        // 创建一个新标题
        addTitle(data, option) {
            // 修改场景类型
            this.scene = data?.value === 'all' ? 'all' : 'paragraph';
            // 段落code
            this.paragraphCode = data?.value === 'all' ? 0 : data?.value;
            // 创建标题对象
            let o = createTitle(data,{isTooltip: !!this.paragraphCode});

            this.$emit('changeParagraph', this.paragraphCode);
            // 如果需要输出标题对象，则直接返回
            if (option?.output) {
                return o;
            }
            // 判断倒数第二位是否是title标签，如果是则替换，并删除掉最后一个问题组件。以方便修改title后重新添加。
            if (this.getEls[this.getEls?.length - 2]?.el === 'aiTitle') {
                this.$set(this.getEls, this.getEls?.length - 2, o);
            } else {
                this.addEls(o);
            }
            // 添加问题组件
            this.addProblem({}, this.scene);
        },
        setScene(scene){
            this.$nextTick(()=>{
                this.addProblem({}, scene);
            })
        },
        // 获取自定义问题列表
        async getCustomizeCueWords(type, scope) {
            return this.$httpBack.v2200.getCustomizeCueWords({
                sourceId: this.getId, // 获取统一的id,对比id,或单个id(视频,文件)
                cueType: type, //运营还是违规类型
                sourceType: this.getSourceType, // 对比还是单个
                scope, // 全文本还是段落 范围 0:全文，1:段落
                applyTo: this.isCompare?1:0,
                page: 1,
                limit: 15
            }).then(res => {
                let {tenantCustomize, universal} = res?.data || {}
                const all = universal?.map(d => {
                    return {
                        ...d,
                        content: d.cueWord,
                        value: d.id,
                        type: d.scene === 1 ? 'violation' : 'assistant'
                    }
                })
                const custom = tenantCustomize?.map(d => {
                    return {
                        ...d,
                        content: d.cueWord,
                        value: d.id,
                        type: d.scene === 1 ? 'violation' : 'assistant'
                    }
                })
                return {
                    all,
                    custom
                }
            });
        },
        // 获取问题列表
        async getCuewordsList(type, scope) {
            return this.$httpBack.v2200.getPageCueWords({
                sourceId: this.getId, // 获取统一的id,对比id,或单个id(视频,文件)
                cueType: type, //运营还是违规类型
                sourceType: this.getSourceType, // 对比还是单个
                scope, // 全文本还是段落 范围 0:全文，1:段落
                applyTo: this.isCompare?1:0,
                page: 1,
                limit: 15
            }).then(res => {
                return res?.data?.list?.map(d => {
                    return {
                        ...d,
                        content: d.cueWord,
                        value: d.id,
                        type: d.scene === 1 ? 'violation' : 'assistant'
                    }
                })
            });
        },
        // 获取全部问题分类
        /**
         * 异步获取问题数据
         *
         * 本函数通过异步调用获取不同类型的问题数据，并将其组织成所需格式返回
         * 主要目的是为了分别获取某种类型下的所有关键词和段落关键词，并包装成一个问题对象
         *
         * @param {string} type - 问题的类型，用于区分不同的问题类别
         * @returns {Promise} 返回一个Promise对象，该对象在数据成功获取并处理后解析，若发生错误则拒绝
         */
        async getProblem(type) {
            return new Promise(async (resolve, reject) => {
                try {
                    // if(this.isWordDiscern){
                    //     // 异步获取指定类型下的所有关键词
                    //     let all = await this.getCuewordsList(type, 0);
                    //     // 异步获取指定类型下的段落关键词
                    //     let paragraph = await this.getCuewordsList(type, 1);
                    //
                    //     // 将获取的数据组织成一个问题对象
                    //     this.problem = {
                    //         all,
                    //         paragraph
                    //     };
                    // }else {
                    //     // 异步获取指定类型下的所有关键词
                    //     let all = await this.getCuewordsList(this.getCueType,0);
                    //     // 将获取的数据组织成一个问题对象
                    //     this.problem = {
                    //         all,
                    //     };
                    // }
                    // let all = await this.getCuewordsList(this.getCueType,0);
                    const cueWords = await this.getCustomizeCueWords(this.getCueType,0);
                    this.problem = {
                        all: cueWords?.all || [],
                        custom: cueWords?.custom || []
                        // custom:cueWords?.all||[]
                    };
                    // 成功时，解析Promise，返回问题对象
                    if(cueWords?.custom?.length) this.scene = 'custom'
                    resolve(this.problem);
                } catch (err) {
                    // 失败时，拒绝Promise，返回错误信息
                    reject(err)
                }
            })
        },
        // 获取全部历史分类
        async getHistoryParagraphList(type) {
            return new Promise((resolve, reject) => {
                if(type !== 0 && type !== 1){
                    return resolve();
                }
                this.httpGetHistoryParagraphList({
                    sourceId: this.getId,
                    sourceType: this.getSourceType, // 对比还是单个
                    type,
                }).then(res => {
                    // 获取全部历史段落
                    let historyParagraphItems = res?.data?.map(d => {
                        return {
                            ...d,
                            value: d.code,
                            label: d.alias,
                            html: d.content
                        }
                    });
                    // 加入默认全文本
                    historyParagraphItems.unshift({ label: '全文本', value: "all" });
                    // 设置数据
                    this.historyParagraphMap[this.type] = historyParagraphItems;
                    // 刷新历史会话记录
                    this.$emit('addHistoryParagraph', {
                        items: historyParagraphItems,
                        type
                    });
                    resolve()
                }).catch(err => {
                    reject(err);
                })
            })
        },
        async getElsList(type) {
            // 判断如果已经拥有了顶部提示则表示不需要从新加载数据
            if (this.getEls?.length && this.getEls[0]?.el === 'aiTip') {
                return;
            }

            // 根据类型重置或增加页码
            if (type === 'init') {
                this.pageIndex = 1;
            } else if (type === 'more') {
                this.pageIndex += 1;
            }
            let httpFn = this.isUseBack? this.$httpBack.v2500.conversationPage : this.$httpClient.aiRelated.conversationPage;

            let param = {
                type: this.getType,
                sourceId: this.getId,
                sourceType: this.getSourceType,
                page: this.pageIndex,
                limit: 10,
            };
            if(this.shareId){
                httpFn = this.$httpBack.ai.shareInfo;
                param = {
                    id: this.shareId
                }
            }
            let els =[];
            try{

                // 获取结构化数据列表
                els = await httpFn(param).then(res => {
                    if(this.shareId){
                        const { conversationVoList } = res.data;
                        const normalizedList = this.normalizeHistoryEls(conversationVoList);
                        return listChannel(normalizedList,{
                            type: this.type,
                            renderContent: (text, renderOption = {}) => this.getMdText(text, renderOption)
                        });
                    }else{
                        this.isMore = res?.data?.existPreviousPage;
                        if(res.data?.list?.length === 0){return []}
                        const originalList = [this.isMore?'':'tip',...res.data?.list];
                        const normalizedList = this.normalizeHistoryEls(originalList);
                        return listChannel(normalizedList,{
                            type: this.type,
                            renderContent: (text, renderOption = {}) => this.getMdText(text, renderOption)
                        });
                    }
                }) || [];
            }catch(err){
                console.error(err);
                els = [];
            }
            // 如果获取到元素，则合并到现有元素地图中
            if (els?.length) {
                let oldlen = this.getEls?.length;
                this.elsMap[this.type] = [].concat(els, this.getEls);
                if(type === 'more'){
                    let len = (this.elsMap[this.type]?.length - oldlen) + 1;
                    // 跳转到加载更多下一个回答
                    this.scrollToBottom(type,document.getElementsByClassName(`component_${len}`)?.[0])
                    return Promise.resolve();
                }

            } else {
                if(this.raedonly){return}
                // 如果没有更多数据，添加提示和问题反馈元素
                this.addTip();
                this.addProblem({}, this.scene);
            }


            // 滚动到页面底部
            this.scrollToBottom(type);
            await this.getHtmlStatus()
            await this.getAiCorrectFormatRealTimeStatus()
            return Promise.resolve();
        },

        //点赞
        commendClick({ data, callback }) {
            // this.$httpClient.aiRelated.likes({
            this.$httpBack.ai.updateLikesStatus({
                type: this.getType,
                sourceId: this.getId,
                sourceType: this.getSourceType,
                ...data
            }).then(() => {
                this.$message.success("点赞成功");
                typeof callback === 'function' && callback();
            })
        },
        stopPolling(name) {
            if (this[name]) {
                clearTimeout(this[name])
                this[name] = null
            }
        },
        scheduleNextPoll(name,callBack) {
            this.stopPolling(name)
            this[name] = setTimeout(() => {
                callBack?.()
            }, 20000)
        },
        //再问一次
        async answerAgain(values) {
            const {qaCode, feedbackValue, modelType, extraRequire} = values;
            const currentObj = this.elsMap[this.type].filter(v => v.bind?.qaCode === qaCode)?.find(s => s.bind?.identity === "self")
            const {originalCueWordsInfo} = currentObj?.bind;
            // const problems = Object.values(this.problem).reduce((acc, curr) => {
            //     return Array.isArray(curr) ? acc.concat(curr) : acc;
            // }, []);
            // const promptInfo = problems?.find(v => v.id === originalCueWordsInfo?.cueWordsId)
            // if(!Object.keys(promptInfo)?.length) return this.$message('智能体提示词以变更，无法再问一次')
            if (Object.keys(originalCueWordsInfo)?.length) {
                const key = this.buildDistillKey(originalCueWordsInfo.content)
                this.recordDistillModify({
                    key,
                    actionType: 'answerAgain',
                    detail: {
                        qaCode,
                        feedbackValue,
                        extraRequire,
                        modelType
                    }
                })
                await this.sendAfter(originalCueWordsInfo.content, {
                    realContent: originalCueWordsInfo?.content,
                    cueWordsId: originalCueWordsInfo?.cueWordsId,
                    cueWordsType: originalCueWordsInfo?.cueWordsType,
                    optimizeText: feedbackValue,
                    extraRequire: extraRequire,
                    lastConversationId: currentObj?.id,
                    modelType
                });
            }
        },
        updateAiCorrectInfo(currentItems) {
            const items = this.elsMap[this.type]
            const bMap = new Map(currentItems.map(item => [item.id, item]))
            items.forEach(item => {
                const match = bMap.get(item.id)
                if (!match) return

                // 确保 aiCorrectFormatInfo 存在
                if (!item.bind.aiCorrectFormatInfo) {
                    this.$set(item.bind, 'aiCorrectFormatInfo', {})
                }

                const info = item.bind.aiCorrectFormatInfo

                //获取目标元素
                let Q = this.getEls?.find(v => v.id === match.id)

                if (match.aiCorrectStatus === 2) {
                    const correctedContent = match.content || '';
                    if (Q?.bind) {
                        this.applyServiceAiRender(Q, {
                            answerContent: correctedContent,
                            thinkingContent: '',
                            id: Q.id
                        });
                    }
                }
                this.$set(info, 'aiCorrectStatus', match.aiCorrectStatus)
                this.$set(info, 'aiCorrectType', match.aiCorrectType)
                this.$set(info, 'aiCorrectError', match.aiCorrectError)
            })
        },
        //服务器获取AI纠正状态
        async getAiCorrectFormatRealTimeStatus() {
            try {
                const ids = this.getEls?.filter(item => item.bind?.aiCorrectFormatInfo?.aiCorrectStatus === 1)
                    .map(item => item.id)
                if (ids?.length) {
                    const result = await this.$httpBack.prompt.getCorrectStatus(ids);
                    if (result.code === 0) {
                        this.updateAiCorrectInfo(result.data)
                        this.scheduleNextPoll('pollingCorrectTimeout', this.getAiCorrectFormatRealTimeStatus)
                    }
                }else {
                    this.stopPolling('pollingCorrectTimeout');
                }
            } catch (e) {
                this.stopPolling('pollingCorrectTimeout');
            }
        },
        //生成ai纠正内容  优化格式
        async aiCorrectFormat(id) {
            try{
                const serviceItem = this.elsMap[this.type].find(v => v.id === id)
                const qaCode = serviceItem?.bind?.qaCode
                const selfItem = this.elsMap[this.type].filter(v => v.bind?.qaCode === qaCode)?.find(s => s.bind?.identity === "self")
                const content = selfItem?.bind?.originalCueWordsInfo?.content
                const key = this.buildDistillKey(content || '')
                this.recordDistillModify({
                    key,
                    actionType: 'aiCorrectFormat',
                    detail: {
                        id,
                        qaCode
                    }
                })
            }catch(e){}
            const httpServer = this.$isWeb ? this.$httpBack.prompt.generateCorrectAiContent : this.$httpClient.aiRelated.generateCorrectAiContent;
            const result = await httpServer({id});
            if(result.code===0){
                const item = this.elsMap[this.type].find(v => v.id === id)
                if (!item) return
                this.$set(item.bind, 'aiCorrectFormatInfo', {
                    ...item.bind?.aiCorrectFormatInfo,
                    aiCorrectStatus: 1
                })

                await this.getAiCorrectFormatRealTimeStatus()
            }
        },
        updateGenerateHtmlInfo(currentItems) {
            const items = this.elsMap[this.type]
            const bMap = new Map(currentItems.map(item => [item.id, item]))
            items.forEach(item => {
                const match = bMap.get(item.id)
                if (!match) return

                // 确保 generateHtmlInfo 存在
                if (!item.bind.generateHtmlInfo) {
                    this.$set(item.bind, 'generateHtmlInfo', {})
                }

                const info = item.bind.generateHtmlInfo

                this.$set(info, 'htmlStatus', match.htmlStatus)
                this.$set(info, 'htmlDomainName', match.htmlDomainName)
                this.$set(info, 'htmlSavePath', match.htmlSavePath)
            })
        },
        async getHtmlStatus() {
            try {
                const ids = this.getEls?.filter(item => item.bind?.generateHtmlInfo?.htmlStatus === 1)
                    .map(item => item.id)
                if (ids?.length) {
                    const httpServer = this.$isWeb ? this.$httpBack.ai.getHtmlStatus : this.$httpClient.aiRelated.getHtmlStatus
                    const result = await httpServer(ids);
                    if (result.code === 0) {
                        this.updateGenerateHtmlInfo(result.data)
                        this.scheduleNextPoll('pollingHtmlTimeout',this.getHtmlStatus)
                    }
                }else {
                    this.stopPolling('pollingHtmlTimeout');
                }
            } catch (e) {
                this.stopPolling('pollingHtmlTimeout');
            }
        },
        async generateChartsHandler(id) {
            const httpServer = this.$isWeb ? this.$httpBack.ai.serviceGenerateHtml : this.$httpClient.aiRelated.generateHtml;
            const result = await httpServer({id})
            if (result.code === 0) {
                const item = this.elsMap[this.type].find(v => v.id === id)
                if (!item) return
                this.$set(item.bind, 'generateHtmlInfo', {
                    ...item.bind?.generateHtmlInfo,
                    htmlStatus: 1
                })
                await this.getHtmlStatus()
            }
        },
        getAiModel(){
            this.$httpClient.aiRelated.getAiModel({
                sourceId: this.getId,
                sourceType: this.getSourceType
            }).then(res=>{
                if(res.code === 0){
                    // 0:doubao1.5-pro-32K，1:deepseek-r1
                    // this.aiModel = res.data;
                    this.$emit('changeAiModel',res.data);
                }
            })
        },
        // 分享链接保存回调。
        shareUrlCallback(ids){
            this.$httpBack.ai.shareSave({
                ids,
                sourceId: this.getId,
                sourceType: this.getSourceType,
                askType: this.getType
            }).then(res=>{
                if(res.code === 0){
                    const {host,sourceId,sourceType,id, askType}  = res.data;
                    let sT = sourceType||this.getSourceType;
                    let sI = sourceId||this.getId;
                    let aT = askType||this.getType;
                    copyShareUrl(`${host || localStorage.host}/#/ai-share/${sT}/${sI}/${id}/${aT}`,{
                        message: this.$message.success('分享链接复制成功！'),
                        notPrefix: true,
                    });
                }
            })
        },
        clearInfo() {
            elsConfig.clear();
            this.historyParagraphMap = {
                assistant: [],
                violation: [],
                scrolling: [],
                dataBoard: [],
                dataCapture: []
            }
        },
    },
    beforeDestroy(){
        this.stopPolling('pollingHtmlTimeout')
        this.stopPolling('pollingCorrectTimeout')
    }
}



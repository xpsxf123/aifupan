/**
 * @file AI 对话元素工厂。
 * @description 负责将问答数据转换为对话组件配置对象。
 */
import renderAiResponseContent from '@/components/analysis/ai/common/aiContentRenderParser.js';
const createTip = (opt={})=>{
    const {type} = opt;
    return {
        el: 'aiTip',
        bind: {
            type: type
        }
    }
}
const createTitle = (data, option = {}) => {
    let o = {
        el: 'aiTitle',
        bind: {
            isTooltip: !!option.isTooltip,
            title: data?.label,
            html: data.html
        }
    }
    return o;
}
const createCustom = (data, option = {}) => {
    // 从data中解构出html、identity和align属性，如果data是字符串，则直接使用该字符串作为html内容
    const { html, identity, align, id } = data || {};
    let _html = typeof data === 'string' ? data : html;
    // 如果没有html内容，则不进行任何操作并退出函数
    if (!_html) { return }
    // 初始化对齐方式，如果没有指定对齐方式则默认为空字符串
    let _align = align || '';
    // 根据身份标识设置对齐方式：'self'标识对话框内容对齐到右侧，'service'标识对话框内容对齐到左侧
    if (identity === 'self') {
        _align = 'right'
    } else if (identity === 'service') {
        _align = 'left'
    }
    // 创建对话框配置对象，包括元素标识、绑定的HTML内容和工具提示显示逻辑，同时合并用户自定义配置
    let o = {
        el: 'aiCustom',
        id: id || option.id,
        bind: {
            html: createDialogueHtml(_html, _align, option.id),
            tool: _align === 'left',
            identity: identity,
            type: option?.type,
            ...option.config,
        }
    }
    // 如果用户指定了输出配置，则返回对话框配置对象，否则直接将对话框添加到元素中
    return o;
}
/**
 * 根据对齐方式生成包含HTML内容的div元素
 * 
 * 该函数的作用是根据提供的HTML内容和对齐方式，生成一个包含该内容的div元素字符串
 * 主要用于动态生成具有特定对齐方式的HTML内容
 * 
 * @param {string} html - 要插入的HTML内容
 * @param {string} align - 对齐方式，可以是'right'、'left'或其它值，决定div的对齐方式
 * @returns {string} 返回一个包含HTML内容的div元素字符串，根据align参数决定对齐方式
 */
const createHtmlBox = (html, align) => {
    // 根据对齐方式选择对应的class后缀
    let unit = align === 'right' ? 'e' : align === 'left' ? 's' : 'c';
    // 构造包含HTML内容和对齐方式的div元素字符串
    let box = `<div class="flex-jc-${unit}">${html}</div>`;
    return box;
}
/**
 * 创建内容气泡窗
 * 
 * 该函数用于生成一个带有特定内容和对齐方式的气泡窗口HTML元素
 * 它根据提供的内容、对齐方式和可选的ID来构建一个对话框盒子的HTML字符串
 * 
 * @param {string} content - 气泡窗内显示的内容
 * @param {string} align - 气泡窗的对齐方式，用于添加特定的CSS类
 * @param {string} [id] - 可选参数，如果提供，将为气泡窗添加相应的ID属性
 * @returns {string} 生成的HTML字符串，包含气泡窗的结构
 */
const createDialogueHtml = (content, align, id) => {
    // 根据提供的参数构建气泡窗的HTML字符串
    // 使用模板字符串和条件表达式来处理可选的ID属性
    let html = `<div class="dialogue-box ai-content-box pd-10 brs-8 font-s14 text-colorMain ai-content-box-${align}" ${id ? `id="${id}"` : ''}>
                ${content}
            </div>`;
    // 调用另一个函数clearTimeout来处理生成的HTML和对齐方式
    // 这里假设clearTimeout的目的是进一步处理或优化HTML，例如清除某些不必要的元素或属性
    return createHtmlBox(html, align);
} 

/**
 * 规范化已结构化的历史会话项。
 *
 * @param {Object} item 历史项对象。
 * @param {Object} opt 额外配置。
 * @returns {Object} 规范化后的对象。
 */
const normalizeExistsItem = (item, opt = {}) => {
    if (item?.el !== 'aiCustom' || item?.bind?.identity !== 'service') {
        return item;
    }
    const renderContent = typeof opt?.renderContent === 'function'
        ? opt.renderContent
        : (text, renderOption = {}) => renderAiResponseContent(text || '', renderOption)?.html || '';
    const rawContent = getHistoryRawContent(item);
    if (!rawContent) {
        return item;
    }
    return {
        ...item,
        bind: {
            ...item.bind,
            html: createDialogueHtml(renderContent(rawContent), 'left', item.id),
            rawContent,
            rawAnswerContent: rawContent
        }
    };
}

/**
 * 获取历史记录中的原始内容。
 *
 * @param {Object} item 历史项对象。
 * @returns {string} 原始文本。
 */
const getHistoryRawContent = (item = {}) => {
    const candidateList = [
        normalizeHistoryText(item?.content),
        normalizeHistoryText(item?.bind?.rawContent),
        normalizeHistoryText(item?.bind?.content)
    ];
    return candidateList.find(text => typeof text === 'string' && text.trim()) || '';
}

/**
 * 标准化历史文本内容。
 *
 * @param {string|Object} value 历史值。
 * @returns {string} 标准化后的文本。
 */
const normalizeHistoryText = (value) => {
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
}

// 数据通道，将特定格式数据清洗为我们需要的数据
const dataChannel = (item, opt={})=>{
    if(item === 'tip'){
        return createTip({type: opt.type})
    }else if(item === 'title'){
        return createTitle(item)
    }else if(['A','Q'].includes(item.type)){
        try {
            // 检查JSON对象中的el属性是否为'aiCustom'
            if (item.type) {
                // 将输入的jsonStr解析为JSON对象，如果jsonStr已经是对象则直接使用
                const {content,code,giveStatuc, qaCode} = item;
                const identity  = item.type === 'A' ? 'self': 'service';
                const { type } = opt;
                let cHtml = '';
                let c = normalizeHistoryText(content);
                const renderContent = typeof opt?.renderContent === 'function'
                    ? opt.renderContent
                    : (text, renderOption = {}) => renderAiResponseContent(text || '', renderOption)?.html || '';
                // 根据identity属性判断是否需要解析Markdown文本为HTML
                if (identity === 'service') {
                    cHtml = renderContent(c || '');
                } else {
                    cHtml = c;
                }
                // 调用createCustom方法创建自定义元素并返回
                return createCustom(
                    {
                        html: cHtml,
                        identity: identity,
                        id: item.id,
                    },
                    {
                        type,
                        config: {
                            code: code,
                            qaCode: qaCode,
                            rawAnswerContent: identity === 'service' ? c : '',
                            originalCueWordsInfo:{//原始cueWords信息  用于重新提问
                                content: cHtml,
                                cueWordsId: item.cueWordsId,
                                cueWordsType: item.cueWordsType
                            },
                            giveStatuc: giveStatuc,
                            generateHtmlInfo: {
                                htmlStatus: item.htmlStatus,
                                htmlDomainName: item.htmlDomainName,
                                htmlSavePath: item.htmlSavePath
                            },
                            aiCorrectFormatInfo:{
                                aiCorrectStatus: item.aiCorrectStatus,
                                aiCorrectType: item.aiCorrectType,
                                aiCorrectError: item.aiCorrectError,
                            }
                        }
                    })
            }
         } catch (err) {
            // 捕获并输出错误信息
            console.error(err);
            return false
        }
    }
}


const listChannel = (list,opt={})=>{
    return list.map(item=>{
        if(item.el){return normalizeExistsItem(item, opt)};
        // 空数据退出逻辑
        if(!item){return false}
        return dataChannel(item, opt);
    })?.filter(d=>d)
}





export {
    listChannel,
    dataChannel,
    createTip,
    createTitle,
    createCustom,
    createDialogueHtml
}

export default {
    listChannel,
    dataChannel,
    createTip,
    createTitle,
    createCustom,
    createDialogueHtml
}

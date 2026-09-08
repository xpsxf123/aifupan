/**
 * @file AI 内容统一渲染入口。
 * @description 根据核心渲染模式配置分发到 JSON 渲染器或 aifupan 自定义标签渲染器。
 */
import renderJsonResponseContent from './jsonRenderParser';
import renderMdTagResponseContent from './mdTagRenderParser';
import renderPureMdResponseContent from './pureMdRenderParser';
import {AI_RENDER_MODE, AI_RENDER_MODE_MAP} from './renderModeConfig';
import { injectDataBlockPlaceholders, preprocessDataBlocks } from './dataBlockPreprocessor';
import preprocessDirectiveTags from './directivePreprocessor';

/**
 * 统一渲染 AI 内容。
 *
 * @param {string} content 主回答内容。
 * @param {Object} option 渲染配置。
 * @param {string} option.parserMode 临时指定的渲染模式。
 * @returns {Object} 渲染结果。
 */
const renderAiResponseContent = (content = '', option = {}) => {
    const directiveText = preprocessDirectiveTags(content);
    const { text, blocks } = preprocessDataBlocks(directiveText, option);
    const parserMode = option?.parserMode || AI_RENDER_MODE;
    if (parserMode === AI_RENDER_MODE_MAP.PURE_MD) {
        const res = renderPureMdResponseContent(text, option);
        return {
            ...(res || {}),
            html: injectDataBlockPlaceholders(res?.html || '', blocks),
            dataBlocks: blocks
        };
    }
    if (parserMode === AI_RENDER_MODE_MAP.MD_TAG) {
        const res = renderMdTagResponseContent(text, option);
        return {
            ...(res || {}),
            html: injectDataBlockPlaceholders(res?.html || '', blocks),
            dataBlocks: blocks
        };
    }
    
    const res = renderJsonResponseContent(text, option);
    return {
        ...(res || {}),
        html: injectDataBlockPlaceholders(res?.html || '', blocks),
        dataBlocks: blocks
    };
};

export {
    AI_RENDER_MODE,
    AI_RENDER_MODE_MAP,
    renderAiResponseContent
};

export default renderAiResponseContent;

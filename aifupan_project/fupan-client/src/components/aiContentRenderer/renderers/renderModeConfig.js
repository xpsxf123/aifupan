/**
 * @file AI 渲染模式配置。
 * @description 统一控制 AI 内容采用 JSON 结构化渲染或 aifupan 自定义标签渲染。
 */

const AI_RENDER_MODE_MAP = {
    JSON: 'json',
    MD_TAG: 'mdTag',
    PURE_MD: 'pureMd'
};

/**
 * AI 内容渲染模式核心开关。
 *
 * 可选值：
 * - AI_RENDER_MODE_MAP.JSON
 * - AI_RENDER_MODE_MAP.MD_TAG
 */
const AI_RENDER_MODE = AI_RENDER_MODE_MAP.MD_TAG;

export {
    AI_RENDER_MODE,
    AI_RENDER_MODE_MAP
};

export default AI_RENDER_MODE;

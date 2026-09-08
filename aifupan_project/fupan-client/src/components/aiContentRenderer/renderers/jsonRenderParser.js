/**
 * @file AI 结构化内容渲染解析器。
 * @description 以 Markdown 为主通道，按行识别 JSON 片段并先转成 HTML，再交给 mdFormat 做二次过滤。
 */
import MarkdownIt from 'markdown-it';
import mdFormat from '@/utils/mdFormat.js';

const TEXT_FIELD_LIST = ['value', 'content', 'title', 'text', 'alt', 'label', 'timestamp', 'name', 'description'];
const inlineMd = new MarkdownIt({
    html: false,
    breaks: true,
    linkify: true,
    typographer: true
});
const STYLE_TEXT = `
<style>
    .ai-json-render-root{
        --ai-primary:#6d5dfc;
        --ai-primary-light:#8b7fff;
        --ai-secondary:#2ec5ff;
        --ai-success:#22c55e;
        --ai-warning:#f59e0b;
        --ai-danger:#ef4444;
        --ai-text:#1f2937;
        --ai-text-light:#667085;
        --ai-border:rgba(109,93,252,.16);
        --ai-surface:#ffffff;
        --ai-surface-soft:linear-gradient(180deg,rgba(248,250,255,.96) 0%,rgba(240,244,255,.92) 100%);
        line-height:1.85;
        color:var(--ai-text);
        word-break:break-word;
    }
    .ai-json-render-root > *{position:relative;}
    .ai-json-render-root h1,
    .ai-json-render-root h2,
    .ai-json-render-root h3,
    .ai-json-render-root h4,
    .ai-json-render-root h5,
    .ai-json-render-root h6{
        margin:18px 0 12px;
        color:#312e81;
        font-weight:700;
        line-height:1.35;
        letter-spacing:.02em;
    }
    .ai-json-render-root h1{
        padding:14px 18px;
        border-radius:16px;
        background:linear-gradient(135deg,rgba(109,93,252,.14),rgba(46,197,255,.12));
        box-shadow:0 12px 32px rgba(109,93,252,.12);
    }
    .ai-json-render-root h2,
    .ai-json-render-root h3{
        padding-left:12px;
        border-left:4px solid var(--ai-primary);
    }
    .ai-json-render-root p{margin:10px 0;color:var(--ai-text);}
    .ai-json-render-root ul,
    .ai-json-render-root ol{padding-left:24px;margin:12px 0;}
    .ai-json-render-root li{margin:6px 0;}
    .ai-json-render-root strong{
        color:var(--ai-primary);
        font-weight:700;
        padding:0 2px;
    }
    .ai-json-render-root em{
        color:#0f766e;
        font-style:italic;
    }
    .ai-json-render-root mark{
        padding:2px 6px;
        border-radius:6px;
        background:linear-gradient(120deg,rgba(250,204,21,.32),rgba(250,204,21,.14));
        color:#92400e;
    }
    .ai-json-render-root a{
        color:var(--ai-secondary);
        text-decoration:none;
        border-bottom:1px dashed rgba(46,197,255,.45);
    }
    .ai-json-render-root a:hover{
        color:var(--ai-primary);
        border-bottom-color:rgba(109,93,252,.45);
    }
    .ai-json-render-root blockquote{
        margin:14px 0;
        padding:14px 18px;
        border-left:4px solid var(--ai-secondary);
        border-radius:0 14px 14px 0;
        background:linear-gradient(90deg,rgba(46,197,255,.12),rgba(46,197,255,.04));
        color:#155e75;
    }
    .ai-json-render-root hr{
        height:1px;
        border:none;
        margin:18px 0;
        background:linear-gradient(90deg,transparent,rgba(109,93,252,.4),transparent);
    }
    .ai-json-render-root .ai-json-card,
    .ai-json-render-root .ai-json-alert,
    .ai-json-render-root .ai-json-statistic,
    .ai-json-render-root .ai-json-chart,
    .ai-json-render-root .ai-json-collapse,
    .ai-json-render-root .ai-json-timeline{margin:12px 0;}
    .ai-json-render-root .ai-json-card{
        padding:18px 20px;
        border:1px solid var(--ai-border);
        border-radius:18px;
        background:var(--ai-surface-soft);
        box-shadow:0 12px 30px rgba(99,102,241,.08);
        overflow:hidden;
    }
    .ai-json-render-root .ai-json-card::before{
        content:'';
        position:absolute;
        inset:0 auto 0 0;
        width:4px;
        background:linear-gradient(180deg,var(--ai-primary),var(--ai-secondary));
    }
    .ai-json-render-root .ai-json-card-title{
        margin-bottom:12px;
        font-size:16px;
        font-weight:700;
        color:#312e81;
    }
    .ai-json-render-root .ai-json-alert{
        padding:14px 16px;
        border-radius:16px;
        border:1px solid rgba(46,197,255,.16);
        border-left:4px solid #409eff;
        background:linear-gradient(135deg,rgba(64,158,255,.12),rgba(64,158,255,.04));
        box-shadow:0 10px 24px rgba(64,158,255,.08);
    }
    .ai-json-render-root .ai-json-alert-warning{
        border-left-color:var(--ai-warning);
        border-color:rgba(245,158,11,.16);
        background:linear-gradient(135deg,rgba(245,158,11,.14),rgba(245,158,11,.04));
    }
    .ai-json-render-root .ai-json-alert-error{
        border-left-color:var(--ai-danger);
        border-color:rgba(239,68,68,.14);
        background:linear-gradient(135deg,rgba(239,68,68,.14),rgba(239,68,68,.04));
    }
    .ai-json-render-root .ai-json-alert-success{
        border-left-color:var(--ai-success);
        border-color:rgba(34,197,94,.14);
        background:linear-gradient(135deg,rgba(34,197,94,.14),rgba(34,197,94,.04));
    }
    .ai-json-render-root .ai-json-alert-title{
        margin-bottom:8px;
        font-weight:700;
        color:#111827;
    }
    .ai-json-render-root .ai-json-progress{
        display:flex;
        align-items:center;
        gap:12px;
        margin:12px 0;
    }
    .ai-json-render-root .ai-json-progress-track{
        flex:1;
        height:12px;
        border-radius:999px;
        background:rgba(109,93,252,.12);
        overflow:hidden;
        box-shadow:inset 0 1px 4px rgba(15,23,42,.08);
    }
    .ai-json-render-root .ai-json-progress-bar{
        height:100%;
        background:linear-gradient(90deg,var(--ai-primary),var(--ai-secondary));
        border-radius:999px;
        box-shadow:0 0 18px rgba(109,93,252,.32);
    }
    .ai-json-render-root .ai-json-progress-success{background:linear-gradient(90deg,#22c55e,#4ade80);}
    .ai-json-render-root .ai-json-progress-warning{background:linear-gradient(90deg,#f59e0b,#fbbf24);}
    .ai-json-render-root .ai-json-progress-exception{background:linear-gradient(90deg,#ef4444,#f87171);}
    .ai-json-render-root .ai-json-progress span{
        min-width:52px;
        font-weight:700;
        color:var(--ai-primary);
    }
    .ai-json-render-root .ai-json-statistic{
        padding:16px 18px;
        border-radius:18px;
        background:linear-gradient(135deg,rgba(109,93,252,.1),rgba(46,197,255,.08));
        border:1px solid var(--ai-border);
        box-shadow:0 14px 28px rgba(109,93,252,.08);
    }
    .ai-json-render-root .ai-json-statistic-title{
        font-size:13px;
        color:var(--ai-text-light);
        letter-spacing:.04em;
    }
    .ai-json-render-root .ai-json-statistic-value{
        margin-top:8px;
        font-size:30px;
        font-weight:800;
        line-height:1.2;
        background:linear-gradient(90deg,var(--ai-primary),var(--ai-secondary));
        -webkit-background-clip:text;
        background-clip:text;
        color:transparent;
    }
    .ai-json-render-root .ai-json-tag{
        display:inline-flex;
        align-items:center;
        padding:4px 12px;
        margin:0 6px 6px 0;
        border-radius:999px;
        border:1px solid rgba(109,93,252,.12);
        background:rgba(109,93,252,.08);
        color:var(--ai-primary);
        font-size:12px;
        font-weight:700;
        box-shadow:0 6px 14px rgba(109,93,252,.08);
    }
    .ai-json-render-root .ai-json-tag-success{background:rgba(34,197,94,.12);border-color:rgba(34,197,94,.16);color:var(--ai-success);}
    .ai-json-render-root .ai-json-tag-info{background:rgba(46,197,255,.12);border-color:rgba(46,197,255,.16);color:#0284c7;}
    .ai-json-render-root .ai-json-tag-warning{background:rgba(245,158,11,.14);border-color:rgba(245,158,11,.2);color:#b45309;}
    .ai-json-render-root .ai-json-tag-danger,
    .ai-json-render-root .ai-json-tag-error{background:rgba(239,68,68,.12);border-color:rgba(239,68,68,.16);color:var(--ai-danger);}
    .ai-json-render-root .ai-json-table{
        width:100%;
        border-collapse:separate;
        border-spacing:0;
        margin:16px 0;
        font-size:13px;
        overflow:hidden;
        border-radius:16px;
        border:1px solid rgba(109,93,252,.12);
        box-shadow:0 14px 28px rgba(15,23,42,.06);
        background:#fff;
    }
    .ai-json-render-root .ai-json-table th,
    .ai-json-render-root .ai-json-table td{
        padding:10px 12px;
        text-align:left;
        vertical-align:top;
        border-bottom:1px solid rgba(109,93,252,.08);
        border-right:1px solid rgba(109,93,252,.08);
    }
    .ai-json-render-root .ai-json-table th{
        padding:12px 14px;
        background:linear-gradient(90deg,var(--ai-primary),var(--ai-primary-light));
        color:#fff;
        font-weight:700;
        letter-spacing:.04em;
        border-bottom:1px solid rgba(109,93,252,.12);
        border-right:1px solid rgba(255,255,255,.12);
    }
    .ai-json-render-root .ai-json-table th:last-child,
    .ai-json-render-root .ai-json-table td:last-child{border-right:none;}
    .ai-json-render-root .ai-json-table tbody tr:last-child td{border-bottom:none;}
    .ai-json-render-root .ai-json-table thead th:first-child{border-top-left-radius:16px;}
    .ai-json-render-root .ai-json-table thead th:last-child{border-top-right-radius:16px;}
    .ai-json-render-root .ai-json-table tbody tr:last-child td:first-child{border-bottom-left-radius:16px;}
    .ai-json-render-root .ai-json-table tbody tr:last-child td:last-child{border-bottom-right-radius:16px;}
    .ai-json-render-root .ai-json-table tbody tr:nth-child(odd){background:rgba(109,93,252,.03);}
    .ai-json-render-root .ai-json-table tbody tr:hover{background:rgba(46,197,255,.06);}
    .ai-json-render-root .ai-json-table td:first-child{
        font-weight:700;
        color:#312e81;
    }
    .ai-json-render-root .ai-json-chart{
        padding:16px;
        border:1px dashed rgba(109,93,252,.24);
        border-radius:18px;
        background:linear-gradient(135deg,rgba(109,93,252,.06),rgba(46,197,255,.04));
        box-shadow:0 12px 28px rgba(15,23,42,.05);
    }
    .ai-json-render-root .ai-json-chart-title{
        margin-bottom:8px;
        font-weight:700;
        color:#312e81;
    }
    .ai-json-render-root .ai-json-chart-meta{
        color:var(--ai-text-light);
        font-size:12px;
    }
    .ai-json-render-root .ai-json-chart-pre{
        margin:12px 0 0;
        padding:12px;
        border-radius:12px;
        background:rgba(255,255,255,.82);
        border:1px solid rgba(109,93,252,.08);
        white-space:pre-wrap;
        overflow:auto;
    }
    .ai-json-render-root .ai-json-timeline{position:relative;padding-left:20px;}
    .ai-json-render-root .ai-json-timeline-item{position:relative;padding:0 0 16px 18px;border-left:1px solid rgba(109,93,252,.18);}
    .ai-json-render-root .ai-json-timeline-item:last-child{padding-bottom:0;}
    .ai-json-render-root .ai-json-timeline-item::before{
        content:'';
        position:absolute;
        left:-7px;
        top:6px;
        width:11px;
        height:11px;
        border-radius:50%;
        background:linear-gradient(180deg,var(--ai-primary),var(--ai-secondary));
        box-shadow:0 0 0 3px rgba(109,93,252,.12);
    }
    .ai-json-render-root .ai-json-timeline-time{
        font-size:12px;
        color:var(--ai-primary);
        font-weight:700;
        margin-bottom:4px;
    }
    .ai-json-render-root .ai-json-collapse details{
        margin:10px 0;
        padding:12px 14px;
        border:1px solid rgba(109,93,252,.12);
        border-radius:14px;
        background:var(--ai-surface);
        box-shadow:0 10px 20px rgba(15,23,42,.04);
    }
    .ai-json-render-root .ai-json-collapse summary{
        cursor:pointer;
        font-weight:700;
        color:#312e81;
        outline:none;
    }
    .ai-json-render-root pre{
        padding:14px 16px;
        border-radius:14px;
        background:linear-gradient(180deg,#0f172a 0%,#111827 100%);
        color:#e5e7eb;
        overflow:auto;
        box-shadow:0 18px 28px rgba(15,23,42,.16);
    }
    .ai-json-render-root code{
        padding:2px 6px;
        border-radius:6px;
        background:rgba(109,93,252,.08);
        color:var(--ai-primary);
        font-weight:600;
    }
    .ai-json-render-root pre code{padding:0;background:transparent;}
    .deepThinkingSection{
        margin-bottom:18px;
        padding:14px 16px;
        border-radius:18px;
        background:linear-gradient(135deg,rgba(245,158,11,.1),rgba(251,191,36,.06));
        border:1px solid rgba(245,158,11,.16);
        box-shadow:0 12px 26px rgba(245,158,11,.08);
    }
    .deepThinkingTitle{
        margin-bottom:10px;
        font-weight:800;
        color:#b45309;
        letter-spacing:.04em;
        display:flex;
        align-items:center;
        justify-content:space-between;
        gap:10px;
        cursor:pointer;
        user-select:none;
    }
    .deepThinkingToggle{
        display:inline-flex;
        align-items:center;
        justify-content:center;
        width:16px;
        height:16px;
        flex:0 0 16px;
        transition:transform .2s ease;
        color:#b45309;
        font-size:14px;
        line-height:1;
    }
    .deepThinkingToggle::before{
        content:'▾';
    }
    .deepThinkingTitle.not-active .deepThinkingToggle{
        transform:rotate(-90deg);
    }
    .deepThinking{
        color:#78350f;
    }
</style>`;

/**
 * 统一入口，返回 AI 内容的 HTML 渲染结果。
 *
 * @param {string} content 主回答内容。
 * @param {Object} option 渲染配置。
 * @param {string} option.thinkingContent 深度思考内容。
 * @returns {Object} 渲染结果。
 */
const renderAiResponseContent = (content = '', option = {}) => {
    const answerResult = renderMixedContent(content);
    const thinkingResult = option?.thinkingContent ? renderMixedContent(option.thinkingContent) : null;
    const thinkingHtml = thinkingResult?.html ? `<div class="deepThinkingSection"><div class="deepThinkingTitle">深度思考<span class="deepThinkingToggle"></span></div><div class="deepThinking">${thinkingResult.html}</div></div>` : '';
    const needStyle = answerResult.hasJson || thinkingResult?.hasJson;
    return {
        html: `${needStyle ? STYLE_TEXT : ''}${thinkingHtml}${answerResult.html}`,
        answerHtml: answerResult.html,
        thinkingHtml,
        hasJson: !!needStyle
    };
};

/**
 * 渲染混合内容。
 *
 * @param {string} content 原始内容。
 * @returns {Object} 渲染结果。
 */
const renderMixedContent = (content = '') => {
    const sourceText = normalizeLineBreaks(content || '');
    if (!sourceText.trim()) {
        return { html: '', hasJson: false };
    }
    const directParseResult = tryParseWholeStructuredJson(sourceText);
    if (directParseResult.ok) {
        return {
            html: directParseResult.html,
            hasJson: !!directParseResult.hasJson
        };
    }
    const mixedResult = buildMixedMarkdownSource(sourceText);
    return {
        html: mdFormat(mixedResult.source),
        hasJson: mixedResult.hasJson
    };
};

/**
 * 将混合文本转换为 Markdown 源串。
 *
 * @param {string} text 原始文本。
 * @returns {Object} 转换结果。
 */
const buildMixedMarkdownSource = (text = '') => {
    const lineList = normalizeLineBreaks(text).split('\n');
    const sourceList = [];
    const bufferList = [];
    let hasJson = false;
    let index = 0;

    while (index < lineList.length) {
        const line = lineList[index];
        const trimLine = line.trim();

        if (!bufferList.length) {
            const lineParseResult = tryParseStructuredJsonCandidate(trimLine);
            if (lineParseResult.ok) {
                if (lineParseResult.html) {
                    sourceList.push(lineParseResult.html);
                }
                hasJson = hasJson || lineParseResult.hasJson;
                index += 1;
                continue;
            }
            if (looksLikeJsonStart(trimLine)) {
                bufferList.push(line);
                index += 1;
                continue;
            }
            sourceList.push(line);
            index += 1;
            continue;
        }

        const candidate = `${bufferList.join('\n')}\n${line}`.trim();
        const candidateParseResult = tryParseStructuredJsonCandidate(candidate);
        if (candidateParseResult.ok) {
            if (candidateParseResult.html) {
                sourceList.push(candidateParseResult.html);
            }
            hasJson = hasJson || candidateParseResult.hasJson;
            bufferList.length = 0;
            index += 1;
            continue;
        }

        if (looksLikeJsonContinuation(trimLine)) {
            bufferList.push(line);
            index += 1;
            continue;
        }

        flushJsonPreview(bufferList, sourceList);
    }

    flushJsonPreview(bufferList, sourceList);

    return {
        source: normalizeMixedSource(sourceList.join('\n')),
        hasJson
    };
};

/**
 * 刷新 JSON 预览文本。
 *
 * @param {string[]} bufferList JSON 缓冲数组。
 * @param {string[]} sourceList 输出数组。
 */
const flushJsonPreview = (bufferList = [], sourceList = []) => {
    if (!bufferList.length) {
        return;
    }
    const previewText = extractJsonPreviewText(bufferList.join('\n'));
    if (previewText) {
        sourceList.push(previewText);
    }
    bufferList.length = 0;
};

/**
 * 标准化混合源文本。
 *
 * @param {string} text 原始文本。
 * @returns {string} 处理后的文本。
 */
const normalizeMixedSource = (text = '') => text
    .replace(/\n{4,}/g, '\n\n\n')
    .trim();

/**
 * 尝试将文本解析为结构化 JSON。
 *
 * @param {string} text 原始文本。
 * @returns {Object} 解析结果。
 */
const tryParseStructuredJsonCandidate = (text = '') => {
    const candidateList = getJsonParseCandidates(text);
    for (let i = 0; i < candidateList.length; i++) {
        const candidate = candidateList[i];
        const parseResult = safeJsonParse(candidate);
        if (!parseResult.ok) {
            continue;
        }
        const normalizedResult = normalizeStructuredPayload(parseResult.value);
        if (!normalizedResult.isStructured) {
            continue;
        }
        return {
            ok: true,
            html: normalizedResult.nodes.length ? wrapJsonHtml(renderBlocks(normalizedResult.nodes)) : '',
            hasJson: true,
            meta: normalizedResult.meta
        };
    }
    return { ok: false, html: '', hasJson: false };
};

/**
 * 尝试将整段文本作为完整 JSON 解析。
 *
 * @param {string} text 原始文本。
 * @returns {Object} 解析结果。
 */
const tryParseWholeStructuredJson = (text = '') => {
    const trimText = normalizeLineBreaks(text).trim();
    const candidateList = [trimText];
    if (trimText.startsWith('data:')) {
        candidateList.push(trimText.slice('data:'.length).trim());
    }
    for (let i = 0; i < candidateList.length; i++) {
        const candidate = candidateList[i];
        const parseResult = safeJsonParse(candidate);
        if (!parseResult.ok) {
            continue;
        }
        const normalizedResult = normalizeStructuredPayload(parseResult.value);
        if (!normalizedResult.isStructured) {
            continue;
        }
        return {
            ok: true,
            html: normalizedResult.nodes.length ? wrapJsonHtml(renderBlocks(normalizedResult.nodes)) : '',
            hasJson: true,
            meta: normalizedResult.meta
        };
    }
    return { ok: false, html: '', hasJson: false };
};

/**
 * 获取可能的 JSON 字符串候选。
 *
 * @param {string} text 原始文本。
 * @returns {string[]} 候选数组。
 */
const getJsonParseCandidates = (text = '') => {
    const trimText = normalizeLineBreaks(text).trim();
    if (!trimText) {
        return [];
    }
    const candidateSet = new Set([trimText]);
    const objectMatch = trimText.match(/\{[\s\S]*\}/);
    const arrayMatch = trimText.match(/\[[\s\S]*\]/);
    if (objectMatch?.[0]) {
        candidateSet.add(objectMatch[0].trim());
    }
    if (arrayMatch?.[0]) {
        candidateSet.add(arrayMatch[0].trim());
    }
    if (trimText.startsWith('data:')) {
        candidateSet.add(trimText.slice('data:'.length).trim());
    }
    return [...candidateSet].filter(Boolean);
};

/**
 * 归一化结构化 JSON 数据。
 *
 * @param {Object|Array} value 解析后的 JSON 对象。
 * @returns {Object} 归一化结果。
 */
const normalizeStructuredPayload = (value) => {
    if (Array.isArray(value)) {
        return {
            isStructured: value.some(isBlockLike),
            nodes: value.filter(isBlockLike),
            meta: {}
        };
    }
    if (!value || typeof value !== 'object') {
        return {
            isStructured: false,
            nodes: [],
            meta: {}
        };
    }
    if (Array.isArray(value.blocks)) {
        return {
            isStructured: true,
            nodes: value.blocks.filter(isBlockLike),
            meta: value.meta || {}
        };
    }
    if (value.type === 'fragment' && isBlockLike(value.block)) {
        return {
            isStructured: true,
            nodes: [value.block],
            meta: {}
        };
    }
    if (value.type === 'done') {
        return {
            isStructured: true,
            nodes: [],
            meta: value.meta || {}
        };
    }
    if (isBlockLike(value)) {
        return {
            isStructured: true,
            nodes: [value],
            meta: {}
        };
    }
    return {
        isStructured: false,
        nodes: [],
        meta: {}
    };
};

/**
 * 标准化换行符。
 *
 * @param {string} text 文本内容。
 * @returns {string} 标准化后的文本。
 */
const normalizeLineBreaks = (text = '') => text.replace(/\r\n/g, '\n');

/**
 * 判断当前文本是否像 JSON 开头。
 *
 * @param {string} text 文本内容。
 * @returns {boolean} 判断结果。
 */
const looksLikeJsonStart = (text = '') => {
    const trimText = (text || '').trim();
    return !!trimText && (
        trimText.startsWith('{') ||
        trimText.startsWith('[') ||
        trimText.startsWith('data: {') ||
        trimText.startsWith('data:{') ||
        trimText.includes('"type"') ||
        trimText.includes('"component"') ||
        trimText.includes('"blocks"')
    );
};

/**
 * 判断当前文本是否像 JSON 延续行。
 *
 * @param {string} text 文本内容。
 * @returns {boolean} 判断结果。
 */
const looksLikeJsonContinuation = (text = '') => {
    const trimText = (text || '').trim();
    if (!trimText) {
        return true;
    }
    return (
        trimText.startsWith('{') ||
        trimText.startsWith('[') ||
        trimText.startsWith('}') ||
        trimText.startsWith(']') ||
        trimText.startsWith('"') ||
        trimText.startsWith(',') ||
        trimText.startsWith('data:') ||
        trimText.endsWith(',') ||
        trimText.includes('":') ||
        trimText.includes('"},') ||
        trimText.includes('}]')
    );
};

/**
 * 安全解析 JSON。
 *
 * @param {string} text JSON 文本。
 * @returns {Object} 解析结果。
 */
const safeJsonParse = (text = '') => {
    try {
        return { ok: true, value: JSON.parse(text) };
    } catch (error) {
        return { ok: false, error };
    }
};

/**
 * 提取 JSON 预览文本。
 *
 * @param {string} text 原始文本。
 * @returns {string} 预览文本。
 */
const extractJsonPreviewText = (text = '') => {
    const cleanText = normalizeLineBreaks(text);
    const previewList = TEXT_FIELD_LIST
        .flatMap(field => extractFieldText(cleanText, field))
        .map(item => decodeJsonString(item))
        .map(item => compactPreviewText(item))
        .filter(Boolean);

    return [...new Set(previewList)].join('\n');
};

/**
 * 提取指定字段的文本值，支持不完整字符串。
 *
 * @param {string} text 原始文本。
 * @param {string} field 字段名称。
 * @returns {string[]} 文本数组。
 */
const extractFieldText = (text = '', field = '') => {
    const pattern = new RegExp(`"${field}"\\s*:\\s*"`, 'g');
    const valueList = [];
    let match = null;
    while ((match = pattern.exec(text))) {
        let cursor = match.index + match[0].length;
        let value = '';
        let escaped = false;
        while (cursor < text.length) {
            const char = text[cursor];
            if (!escaped && char === '"') {
                break;
            }
            if (!escaped && char === '\\') {
                escaped = true;
                cursor += 1;
                if (cursor < text.length) {
                    value += `\\${text[cursor]}`;
                    escaped = false;
                    cursor += 1;
                }
                continue;
            }
            escaped = false;
            value += char;
            cursor += 1;
        }
        if (value.trim()) {
            valueList.push(value);
        }
    }
    return valueList;
};

/**
 * 压缩预览文本中的无效空白。
 *
 * @param {string} text 原始文本。
 * @returns {string} 清洗后的文本。
 */
const compactPreviewText = (text = '') => String(text)
    .replace(/\n{3,}/g, '\n\n')
    .replace(/[ \t]{2,}/g, ' ')
    .trim();

/**
 * 反转义 JSON 字符串。
 *
 * @param {string} text 文本内容。
 * @returns {string} 解码结果。
 */
const decodeJsonString = (text = '') => String(text)
    .replace(/\\\\/g, '__AI_JSON_SLASH__')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\r')
    .replace(/\\t/g, '\t')
    .replace(/\\"/g, '"')
    .replace(/\\\//g, '/')
    .replace(/__AI_JSON_SLASH__/g, '\\');

/**
 * 包装 JSON 渲染结果。
 *
 * @param {string} html 内容 HTML。
 * @returns {string} 包装后的 HTML。
 */
const wrapJsonHtml = (html = '') => html ? `<div class="ai-json-render-root">\n${html}\n</div>` : '';

/**
 * 渲染块级节点。
 *
 * @param {Object[]} blockList 块列表。
 * @returns {string} HTML 字符串。
 */
const renderBlocks = (blockList = []) => blockList.map(renderBlock).join('\n');

/**
 * 渲染单个块节点。
 *
 * @param {Object} block 块节点。
 * @returns {string} HTML 字符串。
 */
const renderBlock = (block = {}) => {
    const component = block?.component;
    const props = block?.props || {};
    const childrenHtml = renderChildren(block?.children);
    switch (component) {
    case 'div':
    case 'blockquote':
    case 'ul':
    case 'ol':
    case 'li':
    case 'p':
    case 'h1':
    case 'h2':
    case 'h3':
    case 'h4':
    case 'h5':
    case 'h6':
        return `<${component}${renderCommonAttr(props)}>${childrenHtml}</${component}>`;
    case 'pre':
        return `<pre><code${props?.language ? ` class="language-${escapeAttr(props.language)}"` : ''}>${escapeHtml(props?.content || '')}</code></pre>`;
    case 'table':
        return renderTable(props);
    case 'hr':
        return '<hr />';
    case 'Progress':
        return renderProgress(props);
    case 'Card':
        return `<div class="ai-json-card"><div class="ai-json-card-title">${renderInlineMarkdown(props?.title || '')}</div>${childrenHtml}</div>`;
    case 'Alert':
        return renderAlert(props, childrenHtml);
    case 'Statistic':
        return renderStatistic(props);
    case 'Tag':
        return renderTag(props, childrenHtml);
    case 'Timeline':
        return renderTimeline(props);
    case 'Collapse':
        return renderCollapse(props);
    case 'Chart':
        return renderChart(props);
    case 'img':
        return renderInline(block);
    default:
        return childrenHtml || '';
    }
};

/**
 * 渲染子节点。
 *
 * @param {Array} children 子节点列表。
 * @returns {string} HTML 字符串。
 */
const renderChildren = (children = []) => (children || []).map(child => {
    if (child?.type === 'text') {
        return renderTextNode(child?.value || '');
    }
    if (child?.component) {
        return isInlineComponent(child?.component) ? renderInline(child) : renderBlock(child);
    }
    return '';
}).join('');

/**
 * 渲染行内节点。
 *
 * @param {Object} node 行内节点。
 * @returns {string} HTML 字符串。
 */
const renderInline = (node = {}) => {
    const component = node?.component;
    const props = node?.props || {};
    const childrenHtml = renderChildren(node?.children);
    switch (component) {
    case 'strong':
    case 'em':
    case 'del':
    case 'code':
        return `<${component}>${childrenHtml}</${component}>`;
    case 'a':
        return `<a href="${escapeAttr(safeHref(props?.href))}" target="${escapeAttr(props?.target || '_blank')}" rel="noopener noreferrer">${childrenHtml}</a>`;
    case 'img':
        return `<img src="${escapeAttr(props?.src || '')}" alt="${escapeAttr(props?.alt || '')}" title="${escapeAttr(props?.title || '')}" />`;
    case 'span':
        return `<span${props?.style ? ` style="${escapeAttr(props.style)}"` : ''}${props?.class ? ` class="${escapeAttr(props.class)}"` : ''}>${childrenHtml}</span>`;
    case 'Tag':
        return renderTag(props, childrenHtml);
    case 'br':
        return '<br />';
    default:
        return childrenHtml;
    }
};

/**
 * 渲染表格。
 *
 * @param {Object} props 表格属性。
 * @returns {string} HTML 字符串。
 */
const renderTable = (props = {}) => {
    const headers = Array.isArray(props?.headers) ? props.headers : [];
    const rows = Array.isArray(props?.rows) ? props.rows : [];
    const theadHtml = headers.length ? `<thead><tr>${headers.map(item => `<th>${renderInlineMarkdown(item)}</th>`).join('')}</tr></thead>` : '';
    const tbodyHtml = `<tbody>${rows.map(row => `<tr>${(row || []).map(cell => `<td>${renderInlineMarkdown(cell)}</td>`).join('')}</tr>`).join('')}</tbody>`;
    return `<table class="ai-json-table">${theadHtml}${tbodyHtml}</table>`;
};

/**
 * 渲染进度条。
 *
 * @param {Object} props 进度条属性。
 * @returns {string} HTML 字符串。
 */
const renderProgress = (props = {}) => {
    const percentage = Math.max(0, Math.min(100, Number(props?.percentage) || 0));
    const statusClass = props?.status ? ` ai-json-progress-${escapeAttr(props.status)}` : '';
    const infoHtml = props?.showInfo === false ? '' : `<span>${percentage}%</span>`;
    return `<div class="ai-json-progress"><div class="ai-json-progress-track"><div class="ai-json-progress-bar${statusClass}" style="width:${percentage}%"></div></div>${infoHtml}</div>`;
};

/**
 * 渲染提示框。
 *
 * @param {Object} props 属性。
 * @param {string} childrenHtml 子内容。
 * @returns {string} HTML 字符串。
 */
const renderAlert = (props = {}, childrenHtml = '') => {
    const type = props?.type === 'error' ? 'error' : props?.type || 'info';
    const titleHtml = props?.title ? `<div class="ai-json-alert-title">${renderInlineMarkdown(props.title)}</div>` : '';
    return `<div class="ai-json-alert ai-json-alert-${escapeAttr(type)}">${titleHtml}${childrenHtml}</div>`;
};

/**
 * 渲染统计块。
 *
 * @param {Object} props 属性。
 * @returns {string} HTML 字符串。
 */
const renderStatistic = (props = {}) => `<div class="ai-json-statistic"><div class="ai-json-statistic-title">${renderInlineMarkdown(props?.title || '')}</div><div class="ai-json-statistic-value">${escapeHtml(`${props?.prefix || ''}${props?.value ?? ''}${props?.suffix || ''}`)}</div></div>`;

/**
 * 渲染标签。
 *
 * @param {Object} props 属性。
 * @param {string} childrenHtml 子内容。
 * @returns {string} HTML 字符串。
 */
const renderTag = (props = {}, childrenHtml = '') => `<span class="ai-json-tag${props?.type ? ` ai-json-tag-${escapeAttr(props.type)}` : ''}">${childrenHtml || renderInlineMarkdown(props?.text || props?.title || '')}</span>`;

/**
 * 渲染时间线。
 *
 * @param {Object} props 属性。
 * @returns {string} HTML 字符串。
 */
const renderTimeline = (props = {}) => {
    const items = Array.isArray(props?.items) ? props.items : [];
    return `<div class="ai-json-timeline">${items.map(item => `<div class="ai-json-timeline-item"><div class="ai-json-timeline-time">${renderInlineMarkdown(item?.timestamp || '')}</div><div>${renderInlineMarkdown(item?.content || '')}</div></div>`).join('')}</div>`;
};

/**
 * 渲染折叠面板。
 *
 * @param {Object} props 属性。
 * @returns {string} HTML 字符串。
 */
const renderCollapse = (props = {}) => {
    const items = Array.isArray(props?.items) ? props.items : [];
    return `<div class="ai-json-collapse">${items.map(item => `<details><summary>${renderInlineMarkdown(item?.title || '')}</summary>${mdFormat(item?.content || '')}</details>`).join('')}</div>`;
};

/**
 * 渲染图表占位块。
 *
 * @param {Object} props 属性。
 * @returns {string} HTML 字符串。
 */
const renderChart = (props = {}) => {
    const chartData = props?.data ? JSON.stringify(props.data, null, 2) : '';
    return `<div class="ai-json-chart"><div class="ai-json-chart-title">${renderInlineMarkdown(props?.title || '图表')}</div><div class="ai-json-chart-meta">图表类型：${escapeHtml(props?.type || 'line')}</div>${chartData ? `<pre class="ai-json-chart-pre">${escapeHtml(chartData)}</pre>` : ''}</div>`;
};

/**
 * 渲染文本节点。
 *
 * @param {string} text 文本内容。
 * @returns {string} HTML 字符串。
 */
const renderTextNode = (text = '') => renderInlineMarkdown(text);

/**
 * 将 Markdown 文本按行内语法渲染成 HTML。
 *
 * @param {string|number} text 文本内容。
 * @returns {string} HTML 字符串。
 */
const renderInlineMarkdown = (text = '') => {
    const sourceText = String(text ?? '');
    if (!sourceText) {
        return '';
    }
    const renderedHtml = inlineMd.renderInline(sourceText);
    return renderedHtml.replace(/\n/g, '<br />');
};

/**
 * 过滤通用属性。
 *
 * @param {Object} props 属性。
 * @returns {string} 属性文本。
 */
const renderCommonAttr = (props = {}) => {
    const attrList = [];
    if (props?.id) {
        attrList.push(` id="${escapeAttr(props.id)}"`);
    }
    if (props?.class) {
        attrList.push(` class="${escapeAttr(props.class)}"`);
    }
    return attrList.join('');
};

/**
 * 判断是否为块节点。
 *
 * @param {Object} value 待判断对象。
 * @returns {boolean} 判断结果。
 */
const isBlockLike = (value = {}) => !!value?.component;

/**
 * 判断是否为行内组件。
 *
 * @param {string} component 组件名。
 * @returns {boolean} 判断结果。
 */
const isInlineComponent = (component = '') => ['strong', 'em', 'del', 'a', 'img', 'span', 'code', 'Tag', 'br'].includes(component);

/**
 * 转义 HTML 内容。
 *
 * @param {string|number} text 文本内容。
 * @returns {string} 转义结果。
 */
const escapeHtml = (text = '') => String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

/**
 * 转义属性内容。
 *
 * @param {string|number} text 文本内容。
 * @returns {string} 转义结果。
 */
const escapeAttr = (text = '') => escapeHtml(text).replace(/`/g, '&#96;');

/**
 * 过滤链接地址。
 *
 * @param {string} href 链接地址。
 * @returns {string} 安全链接。
 */
const safeHref = (href = '') => /^(https?:|mailto:)/i.test(href || '') ? href : '#';

export {
    renderAiResponseContent
};

export default renderAiResponseContent;

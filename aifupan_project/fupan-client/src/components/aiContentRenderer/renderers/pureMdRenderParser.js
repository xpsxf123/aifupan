import MarkdownIt from 'markdown-it';
import preprocessDirectiveTags from './directivePreprocessor';

const md = new MarkdownIt({
    html: true,
    breaks: true,
    linkify: true,
    typographer: true
});

const normalizeLineBreaks = (text = '') => String(text ?? '').replace(/\r\n?/g, '\n');

const decodeSerializedText = (text = '') => {
    const sourceText = String(text ?? '');
    const trimText = sourceText.trim();
    if (!trimText) {
        return '';
    }
    if (trimText.startsWith('"') && trimText.endsWith('"')) {
        try {
            const parsedText = JSON.parse(trimText);
            if (typeof parsedText === 'string') {
                return parsedText;
            }
        } catch (_) {
            return trimText
                .slice(1, -1)
                .replace(/\\r\\n/g, '\n')
                .replace(/\\n/g, '\n')
                .replace(/\\r/g, '\r')
                .replace(/\\t/g, '\t')
                .replace(/\\"/g, '"')
                .replace(/\\\\/g, '\\');
        }
    }
    return sourceText;
};

const protectQuotedPipes = (text = '') => {
    const lineList = String(text ?? '').split('\n');
    let inCodeFence = false;
    return lineList.map(line => {
        if (/^\s*```/.test(line)) {
            inCodeFence = !inCodeFence;
            return line;
        }
        if (inCodeFence) {
            return line;
        }
        let result = '';
        let inStraightQuote = false;
        let inCnQuote = false;
        let escaped = false;
        for (let i = 0; i < line.length; i++) {
            const char = line[i];
            if (char === '"' && !escaped) {
                inStraightQuote = !inStraightQuote;
                result += char;
                escaped = false;
                continue;
            }
            if (char === '“') {
                inCnQuote = true;
                result += char;
                escaped = false;
                continue;
            }
            if (char === '”') {
                inCnQuote = false;
                result += char;
                escaped = false;
                continue;
            }
            if (char === '|' && (inStraightQuote || inCnQuote)) {
                result += '&#124;';
                escaped = false;
                continue;
            }
            result += char;
            escaped = char === '\\' && !escaped;
            if (char !== '\\') {
                escaped = false;
            }
        }
        return result;
    }).join('\n');
};

const normalizeMarkdownFence = (text = '') => {
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
};

const stripAifupanTagContent = (text = '') => {
    let source = String(text ?? '');
    if (!source || source.indexOf('<aifupan-') < 0) {
        return source;
    }
    source = source.replace(/<aifupan-[^>\s/]+[^>]*\/\s*>/gi, '');
    return source
        .replace(/<\/?aifupan-[^>]*>/gi, '')
        .replace(/<\/?aifupan-[^\n]*$/gi, '');
};

const sanitizeHtml = (html = '') => {
    let result = String(html ?? '');
    result = result.replace(/<\s*(script|style|iframe|object|embed|link|meta)\b[\s\S]*?>[\s\S]*?<\s*\/\s*\1\s*>/gi, '');
    result = result.replace(/<\s*(script|style|iframe|object|embed|link|meta)\b[\s\S]*?\/\s*>/gi, '');
    result = result.replace(/\s+on[a-z]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, '');
    result = result.replace(/\s+style\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)/gi, '');
    result = result.replace(/\s+(href|src)\s*=\s*(['"])\s*javascript:[\s\S]*?\2/gi, ' $1="#"');
    return result;
};

const renderPureMarkdown = (content = '') => {
    const source = preprocessDirectiveTags(protectQuotedPipes(normalizeLineBreaks(decodeSerializedText(content || ''))));
    const normalized = normalizeMarkdownFence(stripAifupanTagContent(source));
    if (!String(normalized ?? '').trim()) {
        return '';
    }
    return sanitizeHtml(md.render(normalized));
};

const renderAiResponseContent = (content = '', option = {}) => {
    const answerHtml = renderPureMarkdown(content);
    const thinkingHtmlBody = option?.thinkingContent ? renderPureMarkdown(option.thinkingContent) : '';
    const thinkingHtml = thinkingHtmlBody
        ? `<div class="deepThinkingSection"><div class="deepThinkingTitle">深度思考<span class="deepThinkingToggle"></span></div><div class="deepThinking">${thinkingHtmlBody}</div></div>`
        : '';
    return {
        html: `${thinkingHtml}${answerHtml}`,
        answerHtml,
        thinkingHtml,
        hasCustomTag: false
    };
};

export {
    renderAiResponseContent
};

export default renderAiResponseContent;

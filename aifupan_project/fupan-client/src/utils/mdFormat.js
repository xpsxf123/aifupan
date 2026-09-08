import MarkdownIt from 'markdown-it';

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

const init = (aiContent, opt = {}) => {
    // const { } = opt;
    // 初始化htmlText变量，用于存储处理后的HTML文本,去除代码输出格式
    let htmlText = decodeSerializedText(aiContent).replace('```markdown', '').replace('```', '');
    if(htmlText.indexOf('div>|')>=0){
        htmlText = htmlText.replace('div>|','div>\n\n|');
    }
    htmlText = protectQuotedPipes(htmlText);
    // 创建MarkdownIt实例，并允许解析HTML标签
    const md = new MarkdownIt({
        html: true,
        xhtmlOut: true,
        breaks: true,
        langPrefix: 'language-',
        linkify: true,
        typographer: true,
        // quotes: '“”‘’',
        // highlight: function (str, lang) {
        //     console.log(lang,'---')
        //     if (lang && hljs.getLanguage(lang)) {
        //         try {
        //             console.log(hljs.highlight(str, { language: lang, ignoreIllegals: true }).value,'---')
        //             return hljs.highlight(str, { language: lang, ignoreIllegals: true }).value;
        //         } catch (__) {}
        //     }
        //     return '';
        // }
    });

    // 错误的自定义渲染规则，将所有HTML标签转义
    // 这个规则的目的是确保在输出HTML时，所有的HTML标签都被正确地转义，防止XSS攻击等安全问题
    // 通过重写html_block规则，确保每个HTML标签都被作为普通文本处理，而不是被解析为实际的HTML元素
    md.renderer.rules.html_block = function (tokens, idx, options, env, self) {
        // console.log(tokens, idx, options, env, self,'-------')
        if (tokens[idx].content.indexOf('deepThinking') >= 0 || tokens[idx].content.indexOf('</div>') >= 0) {
            return tokens[idx].content;
        } else if (tokens[idx].type === 'html_block') {
            return tokens[idx].content;
        }
        return self.renderToken(tokens, idx, options);
    };

    // 使用MarkdownIt实例将Markdown文本转换为HTML文本
    htmlText = md.render(htmlText);

    // 返回转换后的HTML文本
    return htmlText;
}

export default init;

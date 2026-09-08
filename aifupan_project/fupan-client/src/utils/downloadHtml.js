/**
 * @description 生成矢量 PDF/原生打印使用的完整 HTML 文档，统一补齐页尺寸、页边距与打印态宽度约束。
 * 说明：导出时需要按“纸张宽度 - 左右页边距”收敛内容宽度，避免内容按整张 A4 宽度排版后被打印页边距再次裁切。
 */

/**
 * @description 常见纸张尺寸宽度映射，供打印容器计算可用内容宽度。
 * @type {Record<string, string>}
 */
const PAGE_WIDTH_MAP = {
    A3: '297mm',
    A4: '210mm',
    A5: '148mm',
    LETTER: '8.5in',
    LEGAL: '8.5in'
};

/**
 * @description 归一化页边距配置，兼容 CSS margin 的 1~4 值写法。
 * @param {string} margin 页边距字符串
 * @returns {{top: string, right: string, bottom: string, left: string}}
 */
function normalizeMarginValues(margin = '12mm 10mm 14mm') {
    const values = String(margin)
        .trim()
        .split(/\s+/)
        .filter(Boolean);

    if (!values.length) {
        return {
            top: '12mm',
            right: '10mm',
            bottom: '14mm',
            left: '10mm'
        };
    }

    if (values.length === 1) {
        const [all] = values;
        return { top: all, right: all, bottom: all, left: all };
    }

    if (values.length === 2) {
        const [vertical, horizontal] = values;
        return { top: vertical, right: horizontal, bottom: vertical, left: horizontal };
    }

    if (values.length === 3) {
        const [top, horizontal, bottom] = values;
        return { top, right: horizontal, bottom, left: horizontal };
    }

    const [top, right, bottom, left] = values;
    return { top, right, bottom, left };
}

/**
 * @description 解析纸张宽度，优先使用命名纸张宽度，无法识别时回退到传入值的首个尺寸。
 * @param {string} pageSize 纸张尺寸配置
 * @returns {string}
 */
function resolvePageWidth(pageSize = 'A4') {
    const normalizedPageSize = String(pageSize).trim();
    const upperPageSize = normalizedPageSize.toUpperCase();
    if (PAGE_WIDTH_MAP[upperPageSize]) {
        return PAGE_WIDTH_MAP[upperPageSize];
    }

    // 自定义尺寸通常为 "210mm 297mm" 或 "8.5in 11in"，只取宽度部分参与布局。
    const customWidth = normalizedPageSize.split(/\s+/)[0];
    return customWidth || PAGE_WIDTH_MAP.A4;
}

/**
 * @description 构建打印布局宽度变量，确保导出内容宽度与实际可打印区域一致。
 * @param {{pageSize?: string, margin?: string}} options 打印配置
 * @returns {{pageWidth: string, contentWidth: string, marginValues: {top: string, right: string, bottom: string, left: string}}}
 */
function buildPrintLayoutVars(options = {}) {
    const { pageSize = 'A4', margin = '12mm 10mm 14mm' } = options || {};
    const marginValues = normalizeMarginValues(margin);
    const pageWidth = resolvePageWidth(pageSize);
    const contentWidth = `calc(${pageWidth} - ${marginValues.left} - ${marginValues.right})`;

    return {
        pageWidth,
        contentWidth,
        marginValues
    };
}

export default (html, options = {}) => {
    html = html.replace(/data-v-[0-9]+=\"\"/g, '');
    const { pageSize = 'A4', margin = '12mm 10mm 14mm' } = options || {};
    const { pageWidth, contentWidth } = buildPrintLayoutVars({ pageSize, margin });
    return `<!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title></title>
        <style>
            :root{
                --print-page-width: ${pageWidth};
                --print-content-width: ${contentWidth};
                --a4-width: ${contentWidth};
            }
            html, body {
                margin: 0;
                padding: 0;
                background: #f4f5f8;
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
            }

            @page { 
                size: ${pageSize}; 
                margin: ${margin}; 
            }

            .aifupan-card,
            .aifupan-step,
            .aifupan-statistic,
            .aifupan-alert,
            .aifupan-note,
            .aifupan-chart,
            .aifupan-columns,
            .aifupan-mermaid,
            .aifupan-flowchart,
            .aifupan-process-tree-wrap,
            .aifupan-process-svg-wrap,
            figure,
            .ai-custom-box { 
                break-inside: avoid !important; 
            }

            table, .aifupan-table { 
                overflow: visible !important; 
                break-inside: auto !important;
            }
            thead { display: table-header-group !important; }
            tr { break-inside: avoid !important; }
            h1, h2, h3 { break-after: avoid !important; }

            .aifupan-process-tree-svg,
            .aifupan-process-svg,
            .aifupan-chart-svg,
            svg {
                max-height: 208mm !important;
                width: auto !important;
                max-width: 100% !important;
                height: auto !important;
                margin: 0 auto !important;
                min-width: 0 !important;
            }

            .aifupan-statistic-value,
            .aifupan-gradient-text {
                background: none !important;
                -webkit-background-clip: border-box !important;
                background-clip: border-box !important;
                -webkit-text-fill-color: currentColor !important;
                color: #1d4ed8 !important;
            }

            table {
                border-collapse: collapse;
                width: 100%;
            }
            td, th {
                border: 1px solid black;
            }
            tr {
                
            }
            .dialogue-box {
                max-width: 100%;
                background-color: #F5F5F5;
            }
            .ai-box {
                height: auto;
            }
            
            .ai-box .deepThinkingTitle {
                display: none;
                padding: 0;
            }

            .ai-content-box-left {
                background: #fff;
            }
            .flex-jcai-sb {
                display: flex;
                align-items: center;
                justify-content: space-between;
            }

            .flex-jc-sb {
                display: flex;
                justify-content: space-between;
            }

            .flex-jcai-c,
            .flex-ji-c {
                text-align: center;
                vertical-align: middle;
                >*{
                    vertical-align: middle;
                }
            }

            .flex-jc-c {
                text-align: center;
            }

            .flex-jc-s {
                float: left;
            }

            .flex-jc-e {
                float: right;
            }

            .flex-column {
                flex-direction: column;
            }
            .pd-t40{
                padding-top: 40px;
            }
            .pd-b40{
                padding-bottom: 40px;
            }
            .pd-6{
                padding: 6px;
            }
            .mg-t10 {
                margin-top: 10px;
            }
            
            .h100 {
                height: 100%;
            }

            .w100 {
                width: 100%;
            }
            .vh100{
                height: 100vh;
            }
            .vw100{
                width: 100vw;
            }

            .flex-row {
                display: flex;
                flex-direction: row;
            }

            .flex-ai-s {
                align-items: stretch;
                display: flex;
            }

            .flex-ai-c {
                display: flex;
                align-items: center;
            }

            .flex-wp-w{
                flex-wrap: wrap;
            }

            .flex-wp-wr{
                flex-wrap: wrap-reverse;
            }


            .flex-1 {
                flex: 1;
            }

            .flex-2 {
                flex: 2;
            }

            .flex-05 {
                flex: 0.5;
            }

            .main-padding {
                padding: 16px;
            }

            .text-center {
                text-align: center;
            }

            .text-right {
                text-align: right;
            }

            .text-left {
                text-align: left;
            }

            .single-line {
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
            }

            .shardText {
                color: var(--color-main);
                cursor: pointer;
            }

            .cs-p {
                cursor: pointer;
            }

            .emptyTipText {
                font-weight: 400;
                font-size: 14px;
                color: #677583;
                line-height: 24px;
            }

            .emptyContainer {
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
            }


            .overflow_hidden {
                overflow: hidden;
            }

            .overflow_auto {
                overflow: auto;
            }

            .overflow_auto_x {
                overflow-x: auto;
            }

            .overflow_auto_y {
                overflow-y: auto;
            }

            .p-r {
                position: relative;
            }

            .p-a {
                position: absolute;
            }


            .slh {
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
            }
            .text-colorc1 {
                color: #0D6986;
            }

            .text-colorc2 {
                color: #B94E3B;
            }
            .text-colorc3 {
                color: #004793;
            }
            .text-color3 {
                color: #95A1AF;
            }
            .img-window-box {
                height: 100%;
                position: relative;
                background-color: rgb(238, 240, 246);
                width: 100%;
                max-width: var(--print-content-width);
                min-width: 0;
                margin: 0 auto;
                overflow: visible !important;
                box-sizing: border-box;
            }
            .img-window-box > div,
            .img-window-box .ai-share-watermark-box,
            .img-window-box .ai-share-watermark + div,
            .img-window-box .ai-content-box,
            .img-window-box .dialogue-box,
            .img-window-box .aifupan-card,
            .img-window-box .aifupan-note,
            .img-window-box .aifupan-alert,
            .img-window-box .aifupan-columns,
            .img-window-box .aifupan-chart,
            .img-window-box .aifupan-process-tree-wrap,
            .img-window-box .aifupan-process-svg-wrap {
                width: 100%;
                max-width: 100%;
                min-width: 0;
                box-sizing: border-box;
            }
            
            .ai-share-watermark-box {
                position: relative;
                background-color: rgb(255, 255, 255);
            }
            .ai-share-watermark {
                position: absolute;
                width: 100%;
                z-index: 0;
                left: 0;
                top: 0;
                height: 100% !important;
                background-size: 50%;
                background-repeat: repeat;
                background-position: 0 0;
                pointer-events: none;
                background-color: transparent;
            }
            .ai-share-watermark + div {
                position: relative;
                z-index: 1;
            }
            .ai-content-box table {
                overflow-x: auto;
                table-layout: fixed;
                border-collapse: collapse;
                width: 100% !important;
                max-width: 100% !important;
            }
            
            .ai-content-box table tbody th {
                white-space: pre-wrap;
                padding: 10px 5px;
                word-break: break-word;
                overflow-wrap: anywhere;
            }
            
            .ai-content-box table td {
                text-align: left;
                padding: 10px 5px;
                word-break: break-word;
                overflow-wrap: anywhere;
                white-space: pre-wrap;
            }

            /*
            .ai-content-box table tr>td:last-child{
                border-right: 1px solid #ccc;
            }

            .ai-content-box table tr {
                border-bottom: 1px solid #ccc;
            }
            */
            
            .ai-content-box table tbody tr:hover {
                background: #fff;
            }


            .ai-content-box p,
            .ai-content-box h1,
            .ai-content-box h3,
            .ai-content-box h4,
            .ai-content-box h5,
            .ai-content-box ol {
                margin: 7px 0;
                max-width: 100%;
                word-break: break-word;
                overflow-wrap: anywhere;
            }

            .ai-content-box p {
                white-space: pre-wrap;
            }

            .ai-content-box pre {
                background: #000;
                line-height: 20px;
                /* 行距 */
                white-space: pre-wrap;
                display: block;
                /*设置布局流，避免换行导致的错误布局*/
                font-size: 12px;
                /*设置字号*/
                tab-width: 4;
                color: #fff;
                border-radius: 5px;
                padding: 10px;
            }

            .ai-content-box ul {
                list-style-type: disc;
            }

            .ai-content-box ol {
                padding-left: 20px;
            }
            
            .ai-content-box ol li {
                list-style-type: decimal !important;
                list-style-type:decimal !important;
            }
            .ai-content-box ol ol {
                list-style-type: lower-roman !important;
            }
            .ai-content-box ol ol ol {
                list-style-type: lower-latin !important;
            }
            
            .ai-content-box li {
                display: list-item !important;
                text-align: -webkit-match-parent !important;
            }
            .pd-8 {
                padding: 8px;
            }
            .main-bg {
                    background-color: #FFFFFF;
            }
            .deepThinkingTitle,.deepThinking {
                display: none;
            }
                .pd-b12 {
                    padding-bottom: 12px;
                }

                .pd-t12 {
                    padding-top: 12px;
                }
                .font-s14 {
                    font-size: 14px;
                }
                .text-colorMain {
                    color: #151719;
                }
                .pd-10 {
                    padding: 10px;
                }
                .brs-8 {
                    border-radius: 8px;
                }
                aiCustom-box {
                    margin-top: 20px;
                }
        </style>
    </head>
    <body>
     <div ref="imgWindowBox" class="img-window-box overflow_hidden overflow_auto_y" style="background-color:  #EEF0F6;">
        ${html}
    </div>
    </body>
    </html>`
}

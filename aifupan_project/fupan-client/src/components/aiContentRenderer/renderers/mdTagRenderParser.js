/**
 * @file AI 自定义标签渲染解析器。
 * @description 通过“自定义标签占位 -> Markdown 转 HTML -> 回填自定义标签 HTML”的方式统一处理 aifupan 标签与普通 Markdown 混排渲染。
 */
import MarkdownIt from 'markdown-it';
import preprocessDirectiveTags from './directivePreprocessor';
import mdFormat from '@/utils/mdFormat.js';
import {AIFUPAN_SKIN_MAP, AIFUPAN_THEME_MAP} from './mdTagStyleConfig';
import {renderMermaidTreeChart} from './svgFlowTreeRenderer';

const inlineMd = new MarkdownIt({
    html: false,
    breaks: true,
    linkify: true,
    typographer: true
});
const inlineRichMd = new MarkdownIt({
    html: true,
    breaks: true,
    linkify: true,
    typographer: true
});
const INLINE_TAG_TYPE_SET = new Set([
    'progress',
    'font',
    'tag',
    'badge',
    'tooltip',
    'emoji',
    'highlight',
    'mark-red',
    'text-red',
    'spoiler',
    'kbd'
]);

/**
 * @description aifupan-tag 预设色板：保留历史语义色，并扩展 1-30 常用色编号标签。
 * 使用方式：
 * 1) 语义标签：`aifupan-tag-success / warning / error / danger / info / default`
 * 2) 编号标签：`aifupan-tag-1 ... aifupan-tag-30`
 */
const AIFUPAN_TAG_PRESET_COLOR_MAP = {
    default: '#2ec5ff',
    info: '#2ec5ff',
    success: '#22c55e',
    warning: '#f59e0b',
    danger: '#ef4444',
    error: '#ef4444',
    1: '#ef4444',
    2: '#f97316',
    3: '#f59e0b',
    4: '#eab308',
    5: '#84cc16',
    6: '#22c55e',
    7: '#10b981',
    8: '#14b8a6',
    9: '#06b6d4',
    10: '#0ea5e9',
    11: '#3b82f6',
    12: '#2563eb',
    13: '#4f46e5',
    14: '#6366f1',
    15: '#8b5cf6',
    16: '#a855f7',
    17: '#c026d3',
    18: '#d946ef',
    19: '#ec4899',
    20: '#f43f5e',
    21: '#be123c',
    22: '#b45309',
    23: '#92400e',
    24: '#65a30d',
    25: '#15803d',
    26: '#0f766e',
    27: '#0d9488',
    28: '#0369a1',
    29: '#1d4ed8',
    30: '#334155'
};

/**
 * @description 将 16 进制颜色转为 rgb 通道。
 * @param {string} color 颜色值
 * @returns {{r:number,g:number,b:number}|null}
 */
const parseHexColorToRgb = (color = '') => {
    const colorText = String(color || '').trim();
    const matched = colorText.match(/^#([0-9a-f]{3}|[0-9a-f]{6})$/i);
    if (!matched) {
        return null;
    }
    let hex = matched[1];
    if (hex.length === 3) {
        hex = hex.split('').map((char) => `${char}${char}`).join('');
    }
    return {
        r: parseInt(hex.slice(0, 2), 16),
        g: parseInt(hex.slice(2, 4), 16),
        b: parseInt(hex.slice(4, 6), 16)
    };
};

/**
 * @description 将 rgb / rgba 颜色字符串转为 rgb 通道。
 * @param {string} color 颜色值
 * @returns {{r:number,g:number,b:number}|null}
 */
const parseRgbColorToRgb = (color = '') => {
    const colorText = String(color || '').trim();
    const matched = colorText.match(/^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*(?:0|1|0?\.\d+))?\s*\)$/i);
    if (!matched) {
        return null;
    }
    return {
        r: Math.max(0, Math.min(255, Number(matched[1]))),
        g: Math.max(0, Math.min(255, Number(matched[2]))),
        b: Math.max(0, Math.min(255, Number(matched[3])))
    };
};

/**
 * @description 统一把合法颜色转为带透明度的 rgba 字符串，供 tag 背景/边框复用。
 * @param {string} color 颜色值
 * @param {number} alpha 透明度
 * @returns {string}
 */
const colorToRgba = (color = '', alpha = 1) => {
    const safeAlpha = Math.max(0, Math.min(1, Number(alpha) || 0));
    const normalizedColor = normalizeThemeColor(color);
    if (!normalizedColor) {
        return '';
    }
    const rgb = parseHexColorToRgb(normalizedColor) || parseRgbColorToRgb(normalizedColor);
    if (!rgb) {
        return '';
    }
    return `rgba(${rgb.r},${rgb.g},${rgb.b},${safeAlpha})`;
};

const AIFUPAN_TAG_PRESET_STYLE_TEXT = Object.entries(AIFUPAN_TAG_PRESET_COLOR_MAP)
    .map(([key, color]) => `.ai-mdtag-render-root .aifupan-tag-${key}{background:${colorToRgba(color, 0.12)};border-color:${colorToRgba(color, 0.18)};color:${color};}`)
    .join('\n');

const STYLE_TEXT = `
<style>
    .ai-mdtag-render-root{
        --ai-tag-primary:#6d5dfc;
        --ai-tag-secondary:#2ec5ff;
        --ai-tag-success:#22c55e;
        --ai-tag-warning:#f59e0b;
        --ai-tag-danger:#ef4444;
        --ai-tag-text:#1f2937;
        --ai-tag-muted:#667085;
        line-height:1.85;
        color:var(--ai-tag-text);
        word-break:break-word;
    }
    .ai-mdtag-render-root.ai-mdtag-theme-aurora{
        --ai-tag-primary:#6d5dfc;
        --ai-tag-secondary:#2ec5ff;
    }
    .ai-mdtag-render-root.ai-mdtag-theme-business{
        --ai-tag-primary:#1d4ed8;
        --ai-tag-secondary:#0891b2;
        --ai-tag-text:#0f172a;
        --ai-tag-muted:#475569;
    }
    .ai-mdtag-render-root.ai-mdtag-theme-warm{
        --ai-tag-primary:#c2410c;
        --ai-tag-secondary:#f59e0b;
        --ai-tag-text:#431407;
        --ai-tag-muted:#9a3412;
    }
    .ai-mdtag-render-root.ai-mdtag-skin-glass .aifupan-card,
    .ai-mdtag-render-root.ai-mdtag-skin-glass .aifupan-chart,
    .ai-mdtag-render-root.ai-mdtag-skin-glass .aifupan-statistic,
    .ai-mdtag-render-root.ai-mdtag-skin-glass .aifupan-alert,
    .ai-mdtag-render-root.ai-mdtag-skin-glass .aifupan-note,
    .ai-mdtag-render-root.ai-mdtag-skin-glass .aifupan-collapse details{
        backdrop-filter:blur(10px);
        background:linear-gradient(180deg,rgba(255,255,255,.72),rgba(247,249,255,.62));
    }
    .ai-mdtag-render-root.ai-mdtag-skin-paper{
        background:linear-gradient(180deg,#fffdf8 0%,#fffaf0 100%);
        padding:18px;
        border-radius:18px;
        box-shadow:0 14px 34px rgba(120,53,15,.06);
    }
    .ai-mdtag-render-root.ai-mdtag-skin-minimal .aifupan-card,
    .ai-mdtag-render-root.ai-mdtag-skin-minimal .aifupan-chart,
    .ai-mdtag-render-root.ai-mdtag-skin-minimal .aifupan-statistic,
    .ai-mdtag-render-root.ai-mdtag-skin-minimal .aifupan-alert,
    .ai-mdtag-render-root.ai-mdtag-skin-minimal .aifupan-note,
    .ai-mdtag-render-root.ai-mdtag-skin-minimal .aifupan-collapse details{
        box-shadow:none;
        border-radius:12px;
    }
    .ai-mdtag-render-root .aifupan-block{margin:14px 0;}
    .ai-mdtag-render-root .aifupan-title{
        margin:18px 0 12px;
        color:#312e81;
        font-weight:800;
        letter-spacing:.02em;
    }
    .ai-mdtag-render-root .aifupan-title-1{
        padding:16px 18px;
        border-radius:18px;
        background:linear-gradient(135deg,rgba(109,93,252,.16),rgba(46,197,255,.12));
        box-shadow:0 14px 30px rgba(109,93,252,.12);
    }
    .ai-mdtag-render-root .aifupan-title-2,
    .ai-mdtag-render-root .aifupan-title-3{
        padding-left:12px;
        border-left:4px solid var(--ai-tag-primary);
    }
    .ai-mdtag-render-root .aifupan-title-filled{
        padding:14px 16px;
        border-radius:16px;
        background:linear-gradient(135deg,rgba(109,93,252,.14),rgba(46,197,255,.10));
        box-shadow:0 12px 26px rgba(109,93,252,.10);
    }
    .ai-mdtag-render-root .aifupan-title-bordered{
        padding:12px 14px;
        border-radius:16px;
        border:1px solid rgba(109,93,252,.22);
        background:rgba(255,255,255,.55);
    }
    .ai-mdtag-render-root .aifupan-title-leftbar{
        padding-left:12px;
        border-left:6px solid var(--ai-tag-primary);
    }
    .ai-mdtag-render-root .aifupan-title-underline{
        padding-bottom:8px;
        border-bottom:3px double rgba(109,93,252,.35);
    }
    .ai-mdtag-render-root .aifupan-card,
    .ai-mdtag-render-root .aifupan-chart,
    .ai-mdtag-render-root .aifupan-statistic,
    .ai-mdtag-render-root .aifupan-alert,
    .ai-mdtag-render-root .aifupan-collapse details{
        border:1px solid rgba(109,93,252,.14);
        border-radius:18px;
        background:linear-gradient(180deg,rgba(248,250,255,.96),rgba(240,244,255,.9));
        box-shadow:0 12px 28px rgba(15,23,42,.06);
    }
    .ai-mdtag-render-root .aifupan-card{
        overflow:hidden;
        padding:0;
    }
    .ai-mdtag-render-root .aifupan-card-head{
        padding:16px 20px 14px;
        border-bottom:1px solid rgba(109,93,252,.1);
        background:linear-gradient(180deg,rgba(109,93,252,.08),rgba(109,93,252,.02));
    }
    .ai-mdtag-render-root .aifupan-card-head > :first-child,
    .ai-mdtag-render-root .aifupan-card-body > :first-child,
    .ai-mdtag-render-root .aifupan-column > :first-child{
        margin-top:0;
    }
    .ai-mdtag-render-root .aifupan-card-head > :last-child,
    .ai-mdtag-render-root .aifupan-card-body > :last-child,
    .ai-mdtag-render-root .aifupan-column > :last-child{
        margin-bottom:0;
    }
    .ai-mdtag-render-root .aifupan-card-title{
        margin:0;
        font-size:16px;
        font-weight:800;
        color:#312e81;
    }
    .ai-mdtag-render-root .aifupan-card-body{
        padding:8px 10px 9px;
    }
    .ai-mdtag-render-root .aifupan-alert{
        padding:14px 16px;
        border-left:4px solid var(--ai-tag-secondary);
        background:linear-gradient(135deg,rgba(46,197,255,.12),rgba(46,197,255,.04));
    }
    .ai-mdtag-render-root .aifupan-alert-success{
        border-left-color:var(--ai-tag-success);
        background:linear-gradient(135deg,rgba(34,197,94,.12),rgba(34,197,94,.04));
    }
    .ai-mdtag-render-root .aifupan-alert-warning{
        border-left-color:var(--ai-tag-warning);
        background:linear-gradient(135deg,rgba(245,158,11,.14),rgba(245,158,11,.04));
    }
    .ai-mdtag-render-root .aifupan-alert-error{
        border-left-color:var(--ai-tag-danger);
        background:linear-gradient(135deg,rgba(239,68,68,.14),rgba(239,68,68,.04));
    }
    .ai-mdtag-render-root .aifupan-note{
        padding:14px 16px;
        border-radius:18px;
        border:1px dashed rgba(168,85,247,.32);
        background:linear-gradient(135deg,rgba(168,85,247,.10),rgba(236,72,153,.06));
        box-shadow:0 14px 34px rgba(168,85,247,.08);
    }
    .ai-mdtag-render-root .aifupan-note-head{
        display:flex;
        align-items:center;
        justify-content:space-between;
        gap:10px;
        margin-bottom:10px;
    }
    .ai-mdtag-render-root .aifupan-note-badge{
        display:inline-flex;
        align-items:center;
        gap:8px;
        padding:6px 10px;
        border-radius:999px;
        font-weight:800;
        font-size:12px;
        letter-spacing:.06em;
        color:#6b21a8;
        background:rgba(168,85,247,.14);
        border:1px solid rgba(168,85,247,.22);
    }
    .ai-mdtag-render-root .aifupan-note-body{color:var(--ai-tag-text);}
    .ai-mdtag-render-root .aifupan-progress-inline{
        display:inline-flex;
        align-items:center;
        gap:10px;
        min-width:220px;
        margin:0 6px;
        vertical-align:middle;
    }
    .ai-mdtag-render-root .aifupan-progress-track{
        display:block;
        flex:1;
        height:10px;
        border-radius:999px;
        background:rgba(109,93,252,.12);
        overflow:hidden;
    }
    .ai-mdtag-render-root .aifupan-progress-bar{
        display:block;
        height:100%;
        border-radius:999px;
        background:linear-gradient(90deg,var(--ai-tag-primary),var(--ai-tag-secondary));
        box-shadow:0 0 16px rgba(109,93,252,.28);
        min-width:0;
        width:0;
    }
    .ai-mdtag-render-root .aifupan-progress-success{background:linear-gradient(90deg,#22c55e,#4ade80);}
    .ai-mdtag-render-root .aifupan-progress-warning{background:linear-gradient(90deg,#f59e0b,#fbbf24);}
    .ai-mdtag-render-root .aifupan-progress-exception,
    .ai-mdtag-render-root .aifupan-progress-error{background:linear-gradient(90deg,#ef4444,#f87171);}
    .ai-mdtag-render-root .aifupan-progress-value{
        min-width:50px;
        font-size:12px;
        font-weight:800;
        color:var(--ai-tag-primary);
    }
    .ai-mdtag-render-root .aifupan-font{
        display:inline;
    }
    .ai-mdtag-render-root .aifupan-statistic-grid{
        display:grid;
        grid-template-columns:repeat(4,minmax(0,1fr));
        gap:10px;
        margin:14px 0;
        padding:0;
        background:none;
        border:none;
        box-shadow:none;
    }
    .ai-mdtag-render-root .aifupan-statistic{
        display:flex;
        flex-direction:column;
        width:100%;
        min-width:0;
        min-height:96px;
        padding:12px 12px 10px;
        margin:0 0 10px;
        box-sizing:border-box;
    }
    .ai-mdtag-render-root .aifupan-statistic-col-1{
        grid-column:span 1;
    }
    .ai-mdtag-render-root .aifupan-statistic-col-2{
        grid-column:span 2;
    }
    .ai-mdtag-render-root .aifupan-statistic-col-3{
        grid-column:span 3;
    }
    .ai-mdtag-render-root .aifupan-statistic-col-4{
        grid-column:span 4;
    }
    .ai-mdtag-render-root .aifupan-statistic-col-5{
        grid-column:span 5;
    }
    .ai-mdtag-render-root .aifupan-statistic-col-6{
        grid-column:1 / -1;
    }
    .ai-mdtag-render-root .aifupan-statistic-grid .aifupan-statistic{
        margin:0;
    }
    @media (max-width: 1280px){
        .ai-mdtag-render-root .aifupan-statistic-grid{
            grid-template-columns:repeat(2,minmax(0,1fr));
        }
        .ai-mdtag-render-root .aifupan-statistic-col-4,
        .ai-mdtag-render-root .aifupan-statistic-col-5,
        .ai-mdtag-render-root .aifupan-statistic-col-6{
            grid-column:1 / -1;
        }
    }
    @media (max-width: 760px){
        .ai-mdtag-render-root .aifupan-statistic-grid{
            grid-template-columns:minmax(0,1fr);
        }
        .ai-mdtag-render-root .aifupan-statistic-col-1,
        .ai-mdtag-render-root .aifupan-statistic-col-2,
        .ai-mdtag-render-root .aifupan-statistic-col-3,
        .ai-mdtag-render-root .aifupan-statistic-col-4,
        .ai-mdtag-render-root .aifupan-statistic-col-5,
        .ai-mdtag-render-root .aifupan-statistic-col-6{
            grid-column:1 / -1;
        }
    }
    .ai-mdtag-render-root .aifupan-statistic-title{
        color:var(--ai-tag-muted);
        font-size:11px;
        letter-spacing:.02em;
        line-height:1.4;
    }
    .ai-mdtag-render-root .aifupan-statistic-value{
        margin-top:6px;
        font-size:22px;
        line-height:1.2;
        font-weight:800;
        background:linear-gradient(90deg,var(--ai-tag-primary),var(--ai-tag-secondary));
        -webkit-background-clip:text;
        background-clip:text;
        color:transparent;
    }
    .ai-mdtag-render-root .aifupan-statistic-value.is-plain{
        background:none;
        -webkit-background-clip:initial;
        background-clip:initial;
        color:var(--ai-tag-primary);
        font-size:14px;
        line-height:1.45;
    }
    .ai-mdtag-render-root .aifupan-statistic-meta{
        margin-top:6px;
        color:var(--ai-tag-muted);
        font-size:11px;
        line-height:1.45;
    }
    .ai-mdtag-render-root .aifupan-statistic-trend{
        display:flex;
        align-items:flex-start;
        justify-content:space-between;
        gap:8px;
    }
    .ai-mdtag-render-root .aifupan-statistic-trend-main{
        flex:1;
        min-width:0;
    }
    .ai-mdtag-render-root .aifupan-statistic-trend-side{
        display:inline-flex;
        align-items:center;
        gap:4px;
        padding:4px 8px;
        border-radius:999px;
        background:rgba(109,93,252,.08);
        color:var(--ai-tag-primary);
        font-size:11px;
        font-weight:800;
        white-space:nowrap;
    }
    .ai-mdtag-render-root .aifupan-statistic-trend-side.is-up{
        background:rgba(34,197,94,.12);
        color:var(--ai-tag-success);
    }
    .ai-mdtag-render-root .aifupan-statistic-trend-side.is-down{
        background:rgba(239,68,68,.12);
        color:var(--ai-tag-danger);
    }
    .ai-mdtag-render-root .aifupan-statistic-trend-label{
        margin-top:6px;
        font-size:12px;
        line-height:1.45;
        color:var(--ai-tag-text);
        font-weight:700;
    }
    .ai-mdtag-render-root .aifupan-tag{
        display:inline-flex;
        align-items:center;
        padding:2px 6px;
        margin:0 6px 6px 0;
        border-radius:6px;
        font-size:12px;
        font-weight:800;
        border:1px solid rgba(109,93,252,.14);
        background:rgba(109,93,252,.08);
        color:var(--ai-tag-primary);
    }
    ${AIFUPAN_TAG_PRESET_STYLE_TEXT}
    .ai-mdtag-render-root .aifupan-highlight{
        padding:2px 6px;
        border-radius:8px;
        background:linear-gradient(120deg,rgba(250,204,21,.34),rgba(250,204,21,.14));
        color:#92400e;
        font-weight:700;
    }
    .ai-mdtag-render-root .aifupan-text-red{
        color:var(--ai-tag-danger);
        font-weight:700;
    }
    .ai-mdtag-render-root .aifupan-mark-red{
        display:inline-block;
        padding:2px 6px;
        border-radius:8px;
        background:rgba(239,68,68,.14);
        color:var(--ai-tag-danger);
        font-weight:700;
    }
    .ai-mdtag-render-root .aifupan-badge{
        display:inline-flex;
        align-items:center;
        gap:4px;
        padding:2px 10px;
        border-radius:999px;
        background:linear-gradient(90deg,var(--ai-tag-primary),#8b7fff);
        color:#fff;
        font-size:11px;
        font-weight:800;
        letter-spacing:.05em;
        text-transform:uppercase;
    }
    .ai-mdtag-render-root .aifupan-spoiler{
        display:inline-block;
        padding:2px 8px;
        border-radius:8px;
        background:rgba(15,23,42,.08);
        color:transparent;
        text-shadow:0 0 10px rgba(15,23,42,.45);
        transition:all .2s ease;
    }
    .ai-mdtag-render-root .aifupan-spoiler:hover{
        color:var(--ai-tag-text);
        text-shadow:none;
    }
    .ai-mdtag-render-root .aifupan-kbd{
        display:inline-flex;
        align-items:center;
        padding:2px 8px;
        border-radius:8px;
        border:1px solid rgba(109,93,252,.18);
        background:#fff;
        box-shadow:inset 0 -2px 0 rgba(15,23,42,.08);
        font-size:12px;
        font-weight:700;
    }
    .ai-mdtag-render-root .aifupan-chart{padding:16px 18px;}
    .ai-mdtag-render-root .aifupan-chart.aifupan-chart-flat{
        padding:16px 18px 18px;
    }
    .ai-mdtag-render-root .aifupan-chart-title{
        font-size:16px;
        font-weight:800;
        color:#312e81;
        margin-bottom:8px;
    }
    .ai-mdtag-render-root .aifupan-chart-title:empty{
        display:none;
    }
    .ai-mdtag-render-root .aifupan-chart-visual{
        margin-bottom:14px;
        padding:14px 14px 10px;
        border-radius:16px;
        background:linear-gradient(180deg,rgba(255,255,255,.92),rgba(244,247,255,.92));
        border:1px solid rgba(109,93,252,.1);
    }
    .ai-mdtag-render-root .aifupan-chart-flat .aifupan-chart-visual{
        margin:8px 0 12px;
        padding:0;
        border:none;
        border-radius:0;
        background:none;
    }
    .ai-mdtag-render-root .aifupan-chart-svg{
        width:100%;
        height:auto;
        display:block;
    }
    .ai-mdtag-render-root .aifupan-chart-legend{
        display:flex;
        flex-wrap:wrap;
        gap:8px 12px;
        margin-top:12px;
    }
    .ai-mdtag-render-root .aifupan-chart-flat > .aifupan-table{
        margin-top:12px;
    }
    .ai-mdtag-render-root .aifupan-chart-legend-item{
        display:inline-flex;
        align-items:center;
        gap:6px;
        font-size:12px;
        color:var(--ai-tag-muted);
    }
    .ai-mdtag-render-root .aifupan-chart-legend-color{
        width:10px;
        height:10px;
        border-radius:50%;
        flex:0 0 auto;
    }
    .ai-mdtag-render-root .aifupan-chart-empty{
        padding:18px 12px;
        border-radius:14px;
        text-align:center;
        color:var(--ai-tag-muted);
        background:rgba(109,93,252,.04);
        font-size:13px;
    }
    .ai-mdtag-render-root .aifupan-process-svg-wrap{
        padding:14px 12px 16px;
        border-radius:18px;
        background:linear-gradient(180deg,rgba(246,249,255,.96),rgba(238,244,255,.92));
        border:1px solid rgba(109,93,252,.08);
    }
    .ai-mdtag-render-root .aifupan-process-svg{
        display:block;
        width:100%;
        height:auto;
        overflow:visible;
    }
    .ai-mdtag-render-root .aifupan-process-tree-wrap{
        padding:16px 14px 18px;
        background:linear-gradient(180deg,rgba(246,249,255,.98),rgba(237,243,255,.94));
        overflow-x:auto;
    }
    .ai-mdtag-render-root .aifupan-process-tree-svg{
        display:block;
        width:100%;
        height:auto;
        overflow:visible;
        min-width:720px;
    }
    .ai-mdtag-render-root .aifupan-chart-table table{
        width:100%;
        border-collapse:separate;
        border-spacing:0;
        overflow:hidden;
        border-radius:14px;
        border:1px solid rgba(109,93,252,.12);
        background:#fff;
    }
    .ai-mdtag-render-root .aifupan-chart-table thead th{
        padding:10px 12px;
        background:linear-gradient(90deg,var(--ai-tag-primary),#8b7fff);
        color:#fff;
        border-bottom:1px solid rgba(109,93,252,.12);
    }
    .ai-mdtag-render-root .aifupan-chart-table td{
        padding:9px 12px;
        border-bottom:1px solid rgba(109,93,252,.08);
        border-right:1px solid rgba(109,93,252,.08);
    }
    .ai-mdtag-render-root .aifupan-chart-table th,
    .ai-mdtag-render-root .aifupan-chart-table td:last-child{border-right:none;}
    .ai-mdtag-render-root .aifupan-chart-table tbody tr:last-child td{border-bottom:none;}
    .ai-mdtag-render-root .aifupan-chart-table thead th:first-child{border-top-left-radius:14px;}
    .ai-mdtag-render-root .aifupan-chart-table thead th:last-child{border-top-right-radius:14px;}
    .ai-mdtag-render-root .aifupan-chart-table tbody tr:last-child td:first-child{border-bottom-left-radius:14px;}
    .ai-mdtag-render-root .aifupan-chart-table tbody tr:last-child td:last-child{border-bottom-right-radius:14px;}
    .ai-mdtag-render-root .aifupan-chart-table tr:nth-child(odd) td{background:rgba(109,93,252,.03);}
    .ai-mdtag-render-root .aifupan-table{
        width:100%;
        border-collapse:separate;
        border-spacing:0;
        overflow:hidden;
        border-radius:14px;
        border:1px solid rgba(109,93,252,.12);
        background:#fff;
    }
    .ai-mdtag-render-root .aifupan-table thead th{
        padding:10px 12px;
        background:linear-gradient(90deg,var(--ai-tag-primary),#8b7fff);
        color:#fff;
        font-weight:800;
        border-bottom:1px solid rgba(109,93,252,.12);
        border-right:1px solid rgba(255,255,255,.12);
    }
    .ai-mdtag-render-root .aifupan-table td{
        padding:9px 12px;
        border-bottom:1px solid rgba(109,93,252,.08);
        border-right:1px solid rgba(109,93,252,.08);
    }
    .ai-mdtag-render-root .aifupan-table th:last-child,
    .ai-mdtag-render-root .aifupan-table td:last-child{border-right:none;}
    .ai-mdtag-render-root .aifupan-table tbody tr:last-child td{border-bottom:none;}
    .ai-mdtag-render-root .aifupan-table thead th:first-child{border-top-left-radius:14px;}
    .ai-mdtag-render-root .aifupan-table thead th:last-child{border-top-right-radius:14px;}
    .ai-mdtag-render-root .aifupan-table tbody tr:last-child td:first-child{border-bottom-left-radius:14px;}
    .ai-mdtag-render-root .aifupan-table tbody tr:last-child td:last-child{border-bottom-right-radius:14px;}
    .ai-mdtag-render-root .aifupan-table-striped tbody tr:nth-child(odd) td{background:rgba(109,93,252,.04);}
    .ai-mdtag-render-root .aifupan-table-hover tbody tr:hover td{background:rgba(46,197,255,.08);}
    .ai-mdtag-render-root .aifupan-table-bordered td,
    .ai-mdtag-render-root .aifupan-table-bordered th{border-right:1px solid rgba(109,93,252,.12);}
    .ai-mdtag-render-root .aifupan-table-bordered td:last-child,
    .ai-mdtag-render-root .aifupan-table-bordered th:last-child{border-right:none;}
    .ai-mdtag-render-root .aifupan-table-compact td,
    .ai-mdtag-render-root .aifupan-table-compact th{padding:6px 8px;}
    .ai-mdtag-render-root .aifupan-quote-modern{
        padding:14px 18px;
        border-left:4px solid var(--ai-tag-secondary);
        border-radius:0 16px 16px 0;
        background:linear-gradient(90deg,rgba(46,197,255,.12),rgba(46,197,255,.04));
        color:#155e75;
    }
    .ai-mdtag-render-root .aifupan-columns{
        display:grid;
        gap:16px;
        margin:14px 0;
        align-items:stretch;
    }
    .ai-mdtag-render-root .aifupan-columns-2{grid-template-columns:repeat(2,minmax(0,1fr));}
    .ai-mdtag-render-root .aifupan-column{
        min-width:0;
        height:100%;
        display:flex;
        flex-direction:column;
        padding:14px 16px;
        border-radius:16px;
        background:linear-gradient(180deg,rgba(255,255,255,.82),rgba(244,247,255,.88));
        border:1px solid rgba(109,93,252,.12);
        box-shadow:0 10px 20px rgba(15,23,42,.04);
    }
    .ai-mdtag-render-root .aifupan-card-body .aifupan-columns{
        margin:0;
        padding:0;
        border:none;
        background:none;
        box-shadow:none;
    }
    .ai-mdtag-render-root .aifupan-card-body .aifupan-column{
        padding:0;
        border:none;
        background:none;
        box-shadow:none;
        border-radius:0;
    }
    @media screen and (max-width: 900px){
        .ai-mdtag-render-root .aifupan-columns-2{
            grid-template-columns:minmax(0,1fr);
        }
    }
    @media print{
        .ai-mdtag-render-root .aifupan-columns{
            display:flex !important;
            flex-wrap:wrap;
            gap:16px;
        }
        .ai-mdtag-render-root .aifupan-columns-2 .aifupan-column{
            flex:0 0 calc(50% - 8px);
            max-width:calc(50% - 8px);
        }
        .ai-mdtag-render-root .aifupan-column{
            box-sizing:border-box;
            break-inside:auto;
            page-break-inside:auto;
        }
        .ai-mdtag-render-root .aifupan-chart-visual{
            break-inside:avoid;
            page-break-inside:avoid;
        }
    }
    .ai-mdtag-render-root .aifupan-list{
        margin:14px 0;
        padding:0;
        list-style:none;
    }
    .ai-mdtag-render-root .aifupan-list li{
        display:flex;
        gap:10px;
        align-items:flex-start;
        padding:8px 0;
        margin:0;
    }
    .ai-mdtag-render-root .aifupan-list-marker{
        flex:0 0 auto;
        display:inline-flex;
        align-items:center;
        justify-content:center;
        min-width:24px;
        height:24px;
        border-radius:999px;
        background:rgba(109,93,252,.1);
        color:var(--ai-tag-primary);
        font-size:12px;
        font-weight:800;
    }
    .ai-mdtag-render-root .aifupan-steps{
        display:grid;
        gap:10px;
        margin:14px 0;
    }
    .ai-mdtag-render-root .aifupan-step{
        display:flex;
        gap:12px;
        align-items:flex-start;
        padding:12px 14px;
        border-radius:14px;
        border:1px solid rgba(109,93,252,.12);
        background:linear-gradient(180deg,rgba(255,255,255,.86),rgba(246,248,255,.88));
    }
    .ai-mdtag-render-root .aifupan-step-index{
        display:inline-flex;
        align-items:center;
        justify-content:center;
        width:28px;
        height:28px;
        border-radius:50%;
        background:linear-gradient(180deg,var(--ai-tag-primary),#8b7fff);
        color:#fff;
        font-weight:800;
    }
    .ai-mdtag-render-root .aifupan-step-main{
        flex:1;
        min-width:0;
    }
    .ai-mdtag-render-root .aifupan-step-title{
        font-size:16px;
        font-weight:800;
        line-height:1.6;
        color:#0f172a;
    }
    .ai-mdtag-render-root .aifupan-step-content{
        margin-top:8px;
        color:#334155;
        font-size:14px;
        line-height:1.8;
    }
    .ai-mdtag-render-root .aifupan-step-content > :first-child{
        margin-top:0;
    }
    .ai-mdtag-render-root .aifupan-step-content > :last-child{
        margin-bottom:0;
    }
    .ai-mdtag-render-root .aifupan-meter{
        margin:14px 0;
        padding:14px 16px;
        border-radius:18px;
        border:1px solid rgba(109,93,252,.12);
        background:linear-gradient(180deg,rgba(255,255,255,.82),rgba(246,248,255,.92));
    }
    .ai-mdtag-render-root .aifupan-meter-track{
        position:relative;
        height:12px;
        border-radius:999px;
        background:rgba(109,93,252,.12);
        overflow:hidden;
    }
    .ai-mdtag-render-root .aifupan-meter-bar{
        height:100%;
        border-radius:999px;
        background:linear-gradient(90deg,var(--ai-tag-primary),var(--ai-tag-secondary));
    }
    .ai-mdtag-render-root .aifupan-meter-meta{
        margin-top:8px;
        color:var(--ai-tag-muted);
        font-size:12px;
        font-weight:700;
    }
    .ai-mdtag-render-root .aifupan-mermaid,
    .ai-mdtag-render-root .aifupan-flowchart,
    .ai-mdtag-render-root .aifupan-code{
        margin:14px 0;
        border-radius:18px;
        overflow:hidden;
        border:1px solid rgba(109,93,252,.12);
        box-shadow:0 12px 24px rgba(15,23,42,.06);
    }
    .ai-mdtag-render-root .aifupan-code-head{
        display:flex;
        align-items:center;
        justify-content:space-between;
        padding:10px 14px;
        background:linear-gradient(90deg,#111827,#1f2937);
        color:#e5eefb;
        font-size:12px;
        font-weight:700;
    }
    .ai-mdtag-render-root .aifupan-code pre{
        margin:0;
        border-radius:0;
        box-shadow:none;
    }
    .ai-mdtag-render-root .aifupan-funnel{
        padding:18px 16px 16px;
        background:linear-gradient(180deg,rgba(255,255,255,.94),rgba(241,245,255,.92));
    }
    .ai-mdtag-render-root .aifupan-funnel-list{
        display:flex;
        flex-direction:column;
        gap:10px;
    }
    .ai-mdtag-render-root .aifupan-funnel-step{
        position:relative;
        margin:0 auto;
        min-height:68px;
        padding:12px 18px;
        border-radius:16px;
        background:linear-gradient(90deg,rgba(29,78,216,.12),rgba(8,145,178,.1));
        border:1px solid rgba(29,78,216,.12);
        display:flex;
        align-items:center;
        justify-content:space-between;
        gap:14px;
        width:var(--funnel-width,100%);
    }
    .ai-mdtag-render-root .aifupan-funnel-step.is-highlight{
        background:linear-gradient(90deg,rgba(239,68,68,.16),rgba(248,113,113,.12));
        border-color:rgba(239,68,68,.18);
    }
    .ai-mdtag-render-root .aifupan-funnel-step-main{
        flex:1;
        min-width:0;
    }
    .ai-mdtag-render-root .aifupan-funnel-title{
        font-size:15px;
        font-weight:800;
        color:#0f172a;
    }
    .ai-mdtag-render-root .aifupan-funnel-desc{
        margin-top:4px;
        font-size:13px;
        line-height:1.6;
        color:#475569;
    }
    .ai-mdtag-render-root .aifupan-funnel-order{
        display:inline-flex;
        align-items:center;
        justify-content:center;
        width:28px;
        height:28px;
        border-radius:50%;
        background:#fff;
        color:var(--ai-tag-primary);
        font-size:12px;
        font-weight:800;
        box-shadow:0 4px 12px rgba(15,23,42,.08);
    }
    .ai-mdtag-render-root .aifupan-funnel-note{
        margin-top:12px;
        color:var(--ai-tag-muted);
        font-size:12px;
        line-height:1.7;
    }
    .ai-mdtag-render-root .aifupan-tabs{
        margin:14px 0;
        border:1px solid rgba(109,93,252,.12);
        border-radius:18px;
        overflow:hidden;
        box-shadow:0 12px 24px rgba(15,23,42,.05);
    }
    .ai-mdtag-render-root .aifupan-tabs-nav{
        display:flex;
        flex-wrap:wrap;
        gap:8px;
        padding:12px;
        background:rgba(109,93,252,.04);
    }
    .ai-mdtag-render-root .aifupan-tab-chip{
        padding:6px 12px;
        border-radius:999px;
        background:#fff;
        border:1px solid rgba(109,93,252,.12);
        color:var(--ai-tag-primary);
        font-size:12px;
        font-weight:800;
    }
    .ai-mdtag-render-root .aifupan-tab-panel{
        padding:14px 16px;
        border-top:1px solid rgba(109,93,252,.08);
    }
    .ai-mdtag-render-root .aifupan-divider{
        display:flex;
        align-items:center;
        gap:12px;
        margin:18px 0;
        color:var(--ai-tag-muted);
        font-size:12px;
        font-weight:700;
        letter-spacing:.08em;
        text-transform:uppercase;
    }
    .ai-mdtag-render-root .aifupan-divider::before,
    .ai-mdtag-render-root .aifupan-divider::after{
        content:'';
        flex:1;
        height:1px;
        background:linear-gradient(90deg,transparent,rgba(109,93,252,.34),transparent);
    }
    .ai-mdtag-render-root .aifupan-tooltip{
        border-bottom:1px dashed rgba(46,197,255,.5);
        cursor:help;
    }
    .ai-mdtag-render-root .aifupan-emoji{
        display:inline-block;
        line-height:1;
    }
    .ai-mdtag-render-root .aifupan-emoji-large{font-size:28px;}
    .ai-mdtag-render-root .aifupan-emoji-xl{font-size:36px;}
    .ai-mdtag-render-root .aifupan-timeline{
        position:relative;
        padding-left:18px;
        margin:14px 0;
    }
    .ai-mdtag-render-root .aifupan-timeline-item{
        position:relative;
        padding:0 0 14px 16px;
        border-left:1px solid rgba(109,93,252,.18);
    }
    .ai-mdtag-render-root .aifupan-timeline-item:last-child{padding-bottom:0;}
    .ai-mdtag-render-root .aifupan-timeline-item::before{
        content:'';
        position:absolute;
        left:-6px;
        top:6px;
        width:10px;
        height:10px;
        border-radius:50%;
        background:linear-gradient(180deg,var(--ai-tag-primary),var(--ai-tag-secondary));
        box-shadow:0 0 0 3px rgba(109,93,252,.12);
    }
    .ai-mdtag-render-root .aifupan-timeline-time{
        color:var(--ai-tag-primary);
        font-weight:800;
        font-size:12px;
        margin-bottom:4px;
    }
    .ai-mdtag-render-root .aifupan-collapse details{padding:12px 14px;}
    .ai-mdtag-render-root .aifupan-collapse summary{
        cursor:pointer;
        color:#312e81;
        font-weight:800;
    }
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
    .deepThinking{color:#78350f;}
</style>`;

const renderAiResponseContent = (content = '', option = {}) => {
    const answerResult = renderMdTagContent(content, option);
    const thinkingResult = option?.thinkingContent ? renderMdTagContent(option.thinkingContent, option) : null;
    const thinkingHtml = thinkingResult?.html ? `<div class="deepThinkingSection"><div class="deepThinkingTitle">深度思考<span class="deepThinkingToggle"></span></div><div class="deepThinking">${thinkingResult.html}</div></div>` : '';
    const needStyle = answerResult.hasCustomTag || thinkingResult?.hasCustomTag || !!option?.forceStyle;
    const themeName = answerResult.themeName || thinkingResult?.themeName || AIFUPAN_THEME_MAP.BUSINESS;
    const skinName = answerResult.skinName || thinkingResult?.skinName || AIFUPAN_SKIN_MAP.PAPER;
    return {
        html: `${needStyle ? STYLE_TEXT : ''}${wrapRootHtml(`${thinkingHtml}${answerResult.html}`, {
            themeName,
            skinName
        })}`,
        answerHtml: answerResult.html,
        thinkingHtml,
        hasCustomTag: !!needStyle,
        themeName,
        skinName
    };
};

/**
 * @description mdTag 内容渲染结果缓存。
 * 深度思考内容在流式结束后不再变化，但正文流式输出期间每 220ms 会触发一次整体渲染，
 * 缓存相同内容的渲染结果可避免深度思考被重复执行 markdown 解析，消除渲染卡顿。
 * @type {Map<string, Object>}
 */
const renderMdTagCache = new Map();
// 缓存条目上限，超出后淘汰最早写入的条目，避免长对话内存膨胀
const RENDER_MD_TAG_CACHE_LIMIT = 12;

/**
 * @description 构建渲染缓存 key（内容 + 影响输出的 option 字段）。
 * @param {string} content 待渲染内容。
 * @param {Object} option 渲染配置。
 * @returns {string} 缓存 key。
 */
const buildRenderMdTagCacheKey = (content = '', option = {}) => `${content}\u0001${option?.upgradePlainTable ? 1 : 0}\u0001${option?.tableVariant || ''}`;

const renderMdTagContentCore = (content = '', option = {}) => {
    const sourceText = normalizeLineBreaks(decodeSerializedText(content || ''));
    if (!sourceText.trim()) {
        return {
            html: '',
            hasCustomTag: false
        };
    }
    const metaInfo = parseMetaHeader(sourceText);
    const renderSource = metaInfo ? metaInfo.content : sourceText;
    const format = metaInfo?.format?.toLowerCase();

    if (format === 'html') {
        return {
            html: renderSource,
            hasCustomTag: false
        };
    }
    if (format === 'text') {
        return {
            html: `<pre>${escapeHtml(renderSource)}</pre>`,
            hasCustomTag: false
        };
    }

    const directiveSource = preprocessDirectiveTags(renderSource);
    const normalizedSource = normalizeFontTagColorQuotes(directiveSource);
    const parsedResult = buildTagPlaceholderSource(normalizedSource);
    const resolvedThemeConfig = resolveThemeSkin(parsedResult, normalizedSource);
    let renderedHtml = renderMarkdownWithPlaceholders(parsedResult.source, parsedResult.placeholderList);
    let hasCustomTag = parsedResult.hasCustomTag;
    if (!hasCustomTag && option?.upgradePlainTable) {
        const upgraded = upgradePlainTableHtml(renderedHtml, option?.tableVariant || 'striped');
        if (upgraded !== renderedHtml) {
            renderedHtml = upgraded;
            hasCustomTag = true;
        }
    }
    return {
        html: renderedHtml,
        hasCustomTag,
        themeName: resolvedThemeConfig.themeName,
        skinName: resolvedThemeConfig.skinName
    };
};

/**
 * @description 渲染 mdTag 内容（带缓存入口）。
 * 相同内容与配置的渲染结果直接命中缓存，避免流式期间对不变的深度思考内容重复解析。
 * @param {string} content 待渲染内容。
 * @param {Object} option 渲染配置。
 * @returns {{html: string, hasCustomTag: boolean, themeName?: string, skinName?: string}} 渲染结果。
 */
const renderMdTagContent = (content = '', option = {}) => {
    const cacheKey = buildRenderMdTagCacheKey(content, option);
    const cachedResult = renderMdTagCache.get(cacheKey);
    if (cachedResult) {
        return cachedResult;
    }
    const result = renderMdTagContentCore(content, option);
    // 超出上限时淘汰最早写入的条目（Map 首个 key）
    if (renderMdTagCache.size >= RENDER_MD_TAG_CACHE_LIMIT) {
        const oldestKey = renderMdTagCache.keys().next().value;
        renderMdTagCache.delete(oldestKey);
    }
    renderMdTagCache.set(cacheKey, result);
    return result;
};

const upgradePlainTableHtml = (html = '', variant = 'striped') => {
    const sourceHtml = String(html || '');
    if (!sourceHtml || sourceHtml.indexOf('<table') < 0) {
        return sourceHtml;
    }
    const safeVariant = normalizeInlineText(variant).toLowerCase() === 'dark' ? 'striped' : (variant || 'striped');
    return sourceHtml.replace(/<table\b([^>]*)>/gi, (matchText = '', attrsText = '') => {
        if (/\baifupan-table\b/i.test(matchText)) {
            return matchText;
        }
        if (/\bclass\s*=/.test(attrsText)) {
            const nextAttrs = attrsText.replace(/\bclass\s*=\s*("([^"]*)"|'([^']*)')/i, (_m, quoted, d1, d2) => {
                const current = d1 ?? d2 ?? '';
                const next = `${current} aifupan-table aifupan-table-${safeVariant}`.trim().replace(/\s+/g, ' ');
                return `class="${next}"`;
            });
            return `<table${nextAttrs}>`;
        }
        return `<table class="aifupan-table aifupan-table-${safeVariant}"${attrsText}>`;
    });
};

const normalizeFontColorValue = (value = '') => {
    let text = String(value ?? '').trim();
    text = text.replace(/[“”]/g, '"').replace(/[‘’]/g, '\'');
    if ((text.startsWith('"') && text.endsWith('"')) || (text.startsWith('\'') && text.endsWith('\''))) {
        text = text.slice(1, -1).trim();
    }
    text = text.replace(/[“”]/g, '"').replace(/[‘’]/g, '\'');
    if ((text.startsWith('"') && text.endsWith('"')) || (text.startsWith('\'') && text.endsWith('\''))) {
        text = text.slice(1, -1).trim();
    }
    return text;
};

const normalizeFontTagColorQuotes = (text = '') => String(text ?? '').replace(/<font\b[^>]*>/gi, (tagText) => {
    const colorAttrPattern = /\bcolor\s*=\s*(?:(["'“”‘’])([\s\S]*?)\1|([^\s>]+))/i;
    if (!colorAttrPattern.test(tagText)) {
        return tagText;
    }
    return tagText.replace(colorAttrPattern, (_, _quote, quotedValue, plainValue) => {
        const nextValue = normalizeFontColorValue(quotedValue ?? plainValue ?? '');
        return `color="${nextValue}"`;
    });
});

const buildTagPlaceholderSource = (text = '', context = {placeholderIndex: 0}) => {
    const sourceList = [];
    const placeholderList = [];
    let cursor = 0;
    let hasCustomTag = false;
    let themeName = '';
    let skinName = '';
    const sourceText = normalizeLineBreaks(text);

    while (cursor < sourceText.length) {
        const tagStart = sourceText.indexOf('<aifupan-', cursor);
        if (tagStart < 0) {
            sourceList.push(stripAifupanFragments(sourceText.slice(cursor)));
            break;
        }
        sourceList.push(sourceText.slice(cursor, tagStart));
        const openEnd = sourceText.indexOf('>', tagStart);
        if (openEnd < 0) {
            break;
        }
        const openTagText = sourceText.slice(tagStart, openEnd + 1);
        const tagMeta = parseAifupanOpenTag(openTagText);
        if (!tagMeta) {
            cursor = openEnd + 1;
            continue;
        }

        if (tagMeta.selfClosing) {
            if (tagMeta.type === 'theme') {
                themeName = tagMeta.themeName || themeName;
                hasCustomTag = true;
                cursor = openEnd + 1;
                continue;
            }
            if (tagMeta.type === 'skin') {
                skinName = tagMeta.skinName || skinName;
                hasCustomTag = true;
                cursor = openEnd + 1;
                continue;
            }
            const lineTitleResult = tryRenderSelfClosingTitleLine(sourceText, tagStart, openEnd, tagMeta);
            if (lineTitleResult) {
                sourceList.push(createTagPlaceholder(lineTitleResult.html, tagMeta, placeholderList, context));
                hasCustomTag = true;
                cursor = lineTitleResult.nextIndex;
                continue;
            }
            const lineBindingResult = tryRenderSelfClosingBoundLine(sourceText, openEnd, tagMeta);
            if (lineBindingResult) {
                sourceList.push(createTagPlaceholder(lineBindingResult.html, tagMeta, placeholderList, context));
                hasCustomTag = true;
                cursor = lineBindingResult.nextIndex;
                continue;
            }
            const dataBlockResult = tryRenderSelfClosingDataBlock(sourceText, openEnd, tagMeta);
            if (dataBlockResult) {
                sourceList.push(createTagPlaceholder(dataBlockResult.html, tagMeta, placeholderList, context));
                hasCustomTag = true;
                cursor = dataBlockResult.nextIndex;
                continue;
            }
            sourceList.push(createTagPlaceholder(renderAifupanTag(tagMeta, ''), tagMeta, placeholderList, context));
            hasCustomTag = true;
            cursor = openEnd + 1;
            continue;
        }

        const closeTagText = `</${tagMeta.rawName}>`;
        const closeStart = sourceText.indexOf(closeTagText, openEnd + 1);
        if (closeStart < 0) {
            const dataBlockResult = tryRenderSelfClosingDataBlock(sourceText, openEnd, tagMeta);
            if (dataBlockResult) {
                sourceList.push(createTagPlaceholder(dataBlockResult.html, tagMeta, placeholderList, context));
                hasCustomTag = true;
                cursor = dataBlockResult.nextIndex;
                continue;
            }
            sourceList.push(stripAifupanFragments(sourceText.slice(openEnd + 1)));
            break;
        }
        const innerContent = sourceText.slice(openEnd + 1, closeStart);
        if (tagMeta.type === 'theme') {
            themeName = tagMeta.themeName || normalizeInlineText(tagMeta.attrs?.name) || themeName;
            const nestedResult = buildTagPlaceholderSource(innerContent, context);
            placeholderList.push(...nestedResult.placeholderList);
            sourceList.push(nestedResult.source);
            themeName = themeName || nestedResult.themeName;
            skinName = skinName || nestedResult.skinName;
            hasCustomTag = true;
            cursor = closeStart + closeTagText.length;
            continue;
        }
        if (tagMeta.type === 'skin') {
            skinName = tagMeta.skinName || normalizeInlineText(tagMeta.attrs?.name) || skinName;
            const nestedResult = buildTagPlaceholderSource(innerContent, context);
            placeholderList.push(...nestedResult.placeholderList);
            sourceList.push(nestedResult.source);
            themeName = themeName || nestedResult.themeName;
            skinName = skinName || nestedResult.skinName;
            hasCustomTag = true;
            cursor = closeStart + closeTagText.length;
            continue;
        }
        sourceList.push(createTagPlaceholder(renderAifupanTag(tagMeta, innerContent), tagMeta, placeholderList, context));
        hasCustomTag = true;
        cursor = closeStart + closeTagText.length;
    }

    return {
        source: normalizeMixedSource(sourceList.join('')),
        placeholderList,
        hasCustomTag,
        themeName,
        skinName
    };
};

const tryRenderSelfClosingTitleLine = (text = '', tagStart = 0, openEnd = 0, tagMeta = {}) => {
    if (tagMeta.type !== 'title' || !tagMeta.level) {
        return null;
    }
    const lineEnd = getLineEndIndex(text, openEnd + 1);
    const trailingText = text.slice(openEnd + 1, lineEnd).trim();
    if (trailingText) {
        return {
            html: renderTitleTag(tagMeta, trailingText),
            nextIndex: lineEnd
        };
    }
    let cursor = lineEnd;
    while (cursor < text.length) {
        const nextLineEnd = getLineEndIndex(text, cursor + 1);
        const lineText = text.slice(cursor + 1, nextLineEnd).trim();
        if (!lineText) {
            cursor = nextLineEnd;
            continue;
        }
        if (/^#{1,6}\s+/.test(lineText)) {
            return {
                html: renderTitleTag(tagMeta, lineText),
                nextIndex: nextLineEnd
            };
        }
        break;
    }
    return null;
};

const tryRenderSelfClosingBoundLine = (text = '', openEnd = 0, tagMeta = {}) => {
    if (!['progress', 'statistic', 'statistic-trend', 'title'].includes(tagMeta.type)) {
        return null;
    }
    const lineEnd = getLineEndIndex(text, openEnd + 1);
    const trailingText = text.slice(openEnd + 1, lineEnd).trim();
    if (!trailingText) {
        return null;
    }
    return {
        html: renderAifupanTag(tagMeta, trailingText),
        nextIndex: lineEnd
    };
};

const tryRenderSelfClosingDataBlock = (text = '', openEnd = 0, tagMeta = {}) => {
    if (!['echarts', 'table'].includes(tagMeta.type)) {
        return null;
    }
    const nextBlock = extractNextMarkdownTableBlock(text, openEnd + 1);
    if (!nextBlock.content) {
        return null;
    }
    return {
        html: tagMeta.type === 'table' ? renderTableTag(tagMeta, nextBlock.content) : renderChartTag(tagMeta, nextBlock.content),
        nextIndex: nextBlock.endIndex
    };
};

const createTagPlaceholder = (html = '', tagMeta = {}, placeholderList = [], context = {placeholderIndex: 0}) => {
    if (!html) {
        return '';
    }
    const placeholderId = `aifupan-placeholder-${context.placeholderIndex}`;
    context.placeholderIndex += 1;
    const tagName = isInlineTag(tagMeta) ? 'span' : 'div';
    placeholderList.push({
        id: placeholderId,
        html,
        tagName
    });
    if (tagName === 'span') {
        return `<span data-aifupan-placeholder="${placeholderId}"></span>`;
    }
    return `\n\n<div data-aifupan-placeholder="${placeholderId}"></div>\n\n`;
};

const isInlineTag = (tagMeta = {}) => INLINE_TAG_TYPE_SET.has(tagMeta.type);

const renderMarkdownWithPlaceholders = (source = '', placeholderList = []) => {
    const html = renderMarkdownHtml(source);
    if (!placeholderList.length) {
        return wrapStatisticGroupHtml(html);
    }
    const renderedHtml = placeholderList.reduce((resultHtml, item) => replacePlaceholderHtml(resultHtml, item), html);
    return wrapStatisticGroupHtml(renderedHtml);
};

const wrapStatisticGroupHtml = (html = '') => {
    const sourceHtml = String(html || '');
    if (!sourceHtml || sourceHtml.indexOf('data-aifupan-statistic="1"') < 0) {
        return sourceHtml;
    }
    const groupPattern = /((?:\s*<section\b[^>]*data-aifupan-statistic="1"[\s\S]*?<\/section>\s*){1,})/g;
    return sourceHtml.replace(groupPattern, (matchText = '', _groupText = '', offset = 0, fullText = '') => {
        const nearPrefix = String(fullText || '').slice(Math.max(0, offset - 140), offset);
        if (/<div class="aifupan-statistic-grid">\s*$/.test(nearPrefix)) {
            return matchText;
        }
        const cardCount = (matchText.match(/data-aifupan-statistic="1"/g) || []).length;
        if (!cardCount || cardCount < 2) {
            return matchText;
        }
        return `<div class="aifupan-statistic-grid">${matchText}</div>`;
    });
};

const replacePlaceholderHtml = (html = '', placeholder = {}) => {
    const placeholderId = escapeRegExp(placeholder.id || '');
    const tagName = escapeRegExp(placeholder.tagName || 'div');
    if (!placeholderId) {
        return html;
    }
    const wrappedPattern = new RegExp(`<p>\\s*<${tagName}\\s+data-aifupan-placeholder="${placeholderId}"><\\/${tagName}>\\s*<\\/p>`, 'g');
    const plainPattern = new RegExp(`<${tagName}\\s+data-aifupan-placeholder="${placeholderId}"><\\/${tagName}>`, 'g');
    return html
        .replace(wrappedPattern, placeholder.html || '')
        .replace(plainPattern, placeholder.html || '');
};

const renderAifupanTag = (tagMeta = {}, innerContent = '') => {
    switch (tagMeta.type) {
    case 'title':
        return renderTitleTag(tagMeta, innerContent);
    case 'echarts':
        return renderChartTag(tagMeta, innerContent);
    case 'progress':
        return renderProgressTag(tagMeta, innerContent);
    case 'font':
        return renderFontTag(tagMeta, innerContent);
    case 'card':
        return renderCardTag(tagMeta, innerContent);
    case 'card-head':
        return renderCardHeadTag(innerContent);
    case 'card-body':
        return renderCardBodyTag(innerContent);
    case 'alert':
        return renderAlertTag(tagMeta, innerContent);
    case 'callout':
        return renderCalloutTag(tagMeta, innerContent);
    case 'note':
        return renderNoteTag(tagMeta, innerContent);
    case 'statistic':
        return renderStatisticTag(tagMeta, innerContent);
    case 'statistic-trend':
        return renderStatisticTrendTag(tagMeta, innerContent);
    case 'tag':
        return renderTagTag(tagMeta, innerContent);
    case 'badge':
        return renderBadgeTag(tagMeta, innerContent);
    case 'timeline':
        return renderTimelineTag(tagMeta, innerContent);
    case 'steps':
        return renderStepsTag(tagMeta, innerContent);
    case 'collapse':
        return renderCollapseTag(tagMeta, innerContent);
    case 'quote-modern':
        return renderQuoteModernTag(innerContent);
    case 'columns':
        return renderColumnsTag(tagMeta, innerContent);
    case 'column':
        return renderColumnTag(innerContent);
    case 'list':
        return renderListTag(tagMeta, innerContent);
    case 'table':
        return renderTableTag(tagMeta, innerContent);
    case 'meter':
        return renderMeterTag(tagMeta, innerContent);
    case 'mermaid':
        return renderMermaidTag(tagMeta, innerContent);
    case 'flowchart':
        return renderFlowchartTag(innerContent);
    case 'code':
    case 'code-diff':
        return renderCodeTag(tagMeta, innerContent);
    case 'tabs':
        return renderTabsTag(innerContent);
    case 'accordion':
        return renderAccordionTag(innerContent);
    case 'divider':
        return renderDividerTag(tagMeta, innerContent);
    case 'tooltip':
        return renderTooltipTag(tagMeta, innerContent);
    case 'emoji':
        return renderEmojiTag(tagMeta, innerContent);
    case 'highlight':
        return `<mark class="aifupan-highlight">${renderInlineRichContent(innerContent)}</mark>`;
    case 'mark-red':
        return `<span class="aifupan-mark-red">${renderInlineRichContent(innerContent)}</span>`;
    case 'text-red':
        return `<span class="aifupan-text-red">${renderInlineRichContent(innerContent)}</span>`;
    case 'spoiler':
        return `<span class="aifupan-spoiler">${renderInlineRichContent(innerContent)}</span>`;
    case 'kbd':
        return `<kbd class="aifupan-kbd">${renderInlineRichContent(innerContent)}</kbd>`;
    default:
        return stripAifupanFragments(innerContent);
    }
};

const renderTitleTag = (tagMeta = {}, innerContent = '') => {
    const titleSource = resolveTitleSource(tagMeta, innerContent);
    const level = titleSource.level;
    const titleText = titleSource.title;
    const variantClass = tagMeta.titleVariant ? ` aifupan-title-${escapeAttr(tagMeta.titleVariant)}` : '';
    return `<div class="aifupan-block"><h${level} class="aifupan-title aifupan-title-${level}${variantClass}">${renderInlineRichContent(titleText)}</h${level}></div>`;
};

const renderChartTag = (tagMeta = {}, innerContent = '') => {
    const chartType = tagMeta.chartType || 'line';
    const normalizedChartType = normalizeInlineText(chartType).toLowerCase();
    const tableData = resolveChartTableData(chartType, innerContent);
    const titleText = normalizeInlineText(tagMeta.attrs?.title) || '';
    const tableHtml = tableData.headers.length ? renderMarkdownTable(tableData) : renderBlockRichContent(innerContent);
    const visualHtml = tableData.headers.length ? renderChartVisual(chartType, tableData, titleText) : '';
    const titleHtml = titleText ? `<div class="aifupan-chart-title">${renderInlineRichContent(titleText)}</div>` : '';
    if (normalizedChartType === 'pie') {
        return `<div class="aifupan-block aifupan-chart aifupan-chart-flat">${titleHtml}${visualHtml}${tableHtml}</div>`;
    }
    return `<div class="aifupan-block aifupan-chart">${titleHtml}${visualHtml}<div class="aifupan-chart-table">${tableHtml}</div></div>`;
};

const renderProgressTag = (tagMeta = {}, innerContent = '') => {
    const progressSource = parseProgressSource(innerContent);
    const rawValue = normalizeInlineText(tagMeta.attrs?.value)
        || normalizeInlineText(tagMeta.attrs?.percentage)
        || progressSource.valueText
        || normalizeInlineText(innerContent)
        || tagMeta.percentage
        || '0';
    const percentage = normalizePercentValue(rawValue);
    const status = normalizeProgressStatus(tagMeta.attrs?.status || tagMeta.status || progressSource.status || '');
    return `<span class="aifupan-progress-inline"><span class="aifupan-progress-track"><span class="aifupan-progress-bar ${status ? `aifupan-progress-${escapeAttr(status)}` : ''}" style="width:${percentage}%"></span></span><span class="aifupan-progress-value">${percentage}%</span></span>`;
};

const renderFontTag = (tagMeta = {}, innerContent = '') => {
    const textHtml = renderInlineRichContent(innerContent);
    if (!textHtml) {
        return '';
    }
    const styleList = [];
    const colorText = normalizeThemeColor(tagMeta.attrs?.color || tagMeta.attrs?.fontColor);
    const backgroundColorText = normalizeThemeColor(tagMeta.attrs?.bgColor || tagMeta.attrs?.backgroundColor);
    const fontWeight = normalizeInlineText(tagMeta.attrs?.weight || tagMeta.attrs?.fontWeight);
    const fontSize = normalizeFontSize(tagMeta.attrs?.size || tagMeta.attrs?.fontSize);
    if (colorText) {
        styleList.push(`color:${colorText}`);
    }
    if (backgroundColorText) {
        styleList.push(`background:${backgroundColorText}`);
        styleList.push('padding:2px 6px');
        styleList.push('border-radius:8px');
    }
    if (fontWeight) {
        styleList.push(`font-weight:${fontWeight}`);
    }
    if (fontSize) {
        styleList.push(`font-size:${fontSize}`);
    }
    if (parseBooleanAttr(tagMeta.attrs?.italic)) {
        styleList.push('font-style:italic');
    }
    if (parseBooleanAttr(tagMeta.attrs?.underline)) {
        styleList.push('text-decoration:underline');
    }
    return `<span class="aifupan-font" ${styleList.length ? `style="${styleList.join(';')}"` : ''}>${textHtml}</span>`;
};

const renderCardTag = (tagMeta = {}, innerContent = '') => {
    const cardSectionResult = parseNamedTagBlocks(innerContent, ['aifupan-card-head', 'aifupan-card-body']);
    const headContent = cardSectionResult.blockMap['aifupan-card-head'] || '';
    const bodyContent = cardSectionResult.blockMap['aifupan-card-body'] || cardSectionResult.restContent;
    const compatibleTitle = normalizeInlineText(tagMeta.attrs?.title);
    const headHtml = headContent
        ? renderCardHeadTag(headContent)
        : compatibleTitle ? renderCardHeadTag(compatibleTitle) : '';
    const bodyHtml = bodyContent ? renderCardBodyTag(bodyContent) : '';
    return `<div class="aifupan-block aifupan-card">${headHtml}${bodyHtml}</div>`;
};

const renderCardHeadTag = (innerContent = '') => {
    const titleHtml = renderBlockRichContent(innerContent);
    if (!titleHtml) {
        return '';
    }
    return `<div class="aifupan-card-head"><div class="aifupan-card-title">${titleHtml}</div></div>`;
};

const renderCardBodyTag = (innerContent = '') => {
    const bodyHtml = renderInnerMarkdown(innerContent);
    if (!bodyHtml) {
        return '';
    }
    return `<div class="aifupan-card-body">${bodyHtml}</div>`;
};

const renderAlertTag = (tagMeta = {}, innerContent = '') => {
    const alertType = tagMeta.alertType || 'info';
    return `<div class="aifupan-block aifupan-alert aifupan-alert-${escapeAttr(alertType)}">${renderInnerMarkdown(innerContent)}</div>`;
};

const renderCalloutTag = (tagMeta = {}, innerContent = '') => {
    const alertType = normalizeInlineText(tagMeta.attrs?.type) || 'info';
    return `<div class="aifupan-block aifupan-alert aifupan-alert-${escapeAttr(alertType)}">${renderInnerMarkdown(innerContent)}</div>`;
};

/**
 * @description 备注标签：仅用于样式渲染（不做内容语义判断），适合作为“非正文但需要醒目提示”的独立卡片
 * @param {Object} tagMeta 标签元信息
 * @param {string} innerContent 双标签内部内容（支持 Markdown 与 aifupan 嵌套）
 * @returns {string} HTML 字符串
 */
const renderNoteTag = (tagMeta = {}, innerContent = '') => {
    const title = normalizeInlineText(tagMeta.attrs?.title) || '备注';
    const bodyHtml = renderInnerMarkdown(innerContent);
    if (!bodyHtml) {
        return '';
    }
    return `<aside class="aifupan-block aifupan-note" role="note"><div class="aifupan-note-head"><span class="aifupan-note-badge">${renderInlineRichContent(title)}</span></div><div class="aifupan-note-body">${bodyHtml}</div></aside>`;
};

const renderStatisticTag = (tagMeta = {}, innerContent = '') => {
    const labelValueSource = parseLabelValueText(innerContent);
    const valueText = normalizeInlineText(tagMeta.attrs?.value) || labelValueSource.value || normalizeInlineText(innerContent) || '0';
    const titleText = normalizeInlineText(tagMeta.attrs?.title) || labelValueSource.label || '统计值';
    const prefix = normalizeInlineText(tagMeta.attrs?.prefix);
    const suffix = normalizeInlineText(tagMeta.attrs?.suffix);
    const description = normalizeInlineText(tagMeta.attrs?.description);
    const displayValue = `${prefix}${valueText}${suffix}`;
    const isNumericStatistic = isMeaningfulStatisticValue(valueText, prefix, suffix);
    const valueClassName = isNumericStatistic ? 'aifupan-statistic-value' : 'aifupan-statistic-value is-plain';
    const descriptionHtml = description ? `<div class="aifupan-statistic-meta">${renderInlineRichContent(description)}</div>` : '';
    const col = normalizeStatisticCol(tagMeta.attrs?.col);
    return `<section class="aifupan-block aifupan-statistic aifupan-statistic-col-${col}" data-aifupan-statistic="1"><div class="aifupan-statistic-title">${renderInlineRichContent(titleText)}</div><div class="${valueClassName}">${renderInlineRichContent(displayValue)}</div>${descriptionHtml}</section>`;
};

const renderStatisticTrendTag = (tagMeta = {}, innerContent = '') => {
    const labelValueSource = parseLabelValueText(innerContent);
    const valueText = normalizeInlineText(tagMeta.attrs?.value) || labelValueSource.value || normalizeInlineText(innerContent) || '0';
    const titleText = normalizeInlineText(tagMeta.attrs?.title) || labelValueSource.label || '趋势变化';
    const description = normalizeInlineText(tagMeta.attrs?.description);
    const positive = parseBooleanAttr(tagMeta.attrs?.positive);
    const negative = parseBooleanAttr(tagMeta.attrs?.negative);
    const direction = normalizeTrendDirection(tagMeta.attrs?.trend || tagMeta.attrs?.direction || '', positive, negative);
    const directionArrow = direction === 'up' ? '↑' : direction === 'down' ? '↓' : '→';
    const directionClassName = direction === 'up' ? 'is-up' : direction === 'down' ? 'is-down' : '';
    const isNumericStatistic = isMeaningfulStatisticValue(valueText);
    const valueClassName = isNumericStatistic ? 'aifupan-statistic-value' : 'aifupan-statistic-value is-plain';
    const descriptionHtml = description ? `<div class="aifupan-statistic-meta">${renderInlineRichContent(description)}</div>` : '';
    const col = normalizeStatisticCol(tagMeta.attrs?.col);
    return `<section class="aifupan-block aifupan-statistic aifupan-statistic-col-${col} aifupan-statistic-trend" data-aifupan-statistic="1"><div class="aifupan-statistic-trend-main"><div class="aifupan-statistic-title">${renderInlineRichContent(titleText)}</div><div class="${valueClassName}">${renderInlineRichContent(valueText)}</div>${descriptionHtml}</div><div class="aifupan-statistic-trend-side ${directionClassName}"><span>${directionArrow}</span><span>${renderInlineRichContent(getTrendDirectionLabel(direction))}</span></div></section>`;
};

const renderTagTag = (tagMeta = {}, innerContent = '') => {
    const tagType = tagMeta.tagType || 'default';
    const tagText = normalizeInlineText(innerContent) || normalizeInlineText(tagMeta.attrs?.text) || '标签';
    const textColor = normalizeThemeColor(tagMeta.attrs?.color || tagMeta.attrs?.fontColor);
    const backgroundSourceColor = normalizeThemeColor(tagMeta.attrs?.bgColor || tagMeta.attrs?.backgroundColor || '') || textColor;
    const presetColor = AIFUPAN_TAG_PRESET_COLOR_MAP[tagType] || '';
    const styleList = [];
    // 自定义色逻辑：
    // 1) 只传 color：文字用 color，背景/边框也基于 color 计算透明度。
    // 2) 同时传 color + bgColor：文字用 color，背景/边框基于 bgColor 计算透明度。
    if (textColor) {
        styleList.push(`color:${textColor}`);
    } else if (!backgroundSourceColor && presetColor) {
        styleList.push(`color:${presetColor}`);
    }
    if (backgroundSourceColor) {
        const backgroundColor = colorToRgba(backgroundSourceColor, 0.12);
        const borderColor = colorToRgba(backgroundSourceColor, 0.18);
        if (backgroundColor) {
            styleList.push(`background:${backgroundColor}`);
        }
        if (borderColor) {
            styleList.push(`border-color:${borderColor}`);
        }
    }
    return `<span class="aifupan-tag aifupan-tag-${escapeAttr(tagType)}" ${styleList.length ? `style="${styleList.join(';')}"` : ''}>${renderInlineRichContent(tagText)}</span>`;
};

const renderBadgeTag = (tagMeta = {}, innerContent = '') => {
    const badgeText = normalizeInlineText(innerContent) || normalizeInlineText(tagMeta.attrs?.text) || '标签';
    return `<span class="aifupan-badge">${renderInlineRichContent(badgeText)}</span>`;
};

const renderTimelineTag = (tagMeta = {}, innerContent = '') => {
    const itemList = parseTimelineItems(innerContent);
    if (!itemList.length) {
        return renderInnerMarkdown(innerContent);
    }
    return `<div class="aifupan-block aifupan-timeline">${itemList.map(item => `<div class="aifupan-timeline-item"><div class="aifupan-timeline-time">${renderInlineRichContent(item.time)}</div><div>${renderInlineRichContent(item.content)}</div></div>`).join('')}</div>`;
};

const renderStepsTag = (tagMeta = {}, innerContent = '') => {
    const stepList = parseStructuredSteps(innerContent);
    if (!stepList.length) {
        const itemList = parseSimpleListItems(innerContent);
        if (!itemList.length) {
            return renderInnerMarkdown(innerContent);
        }
        return `<div class="aifupan-block aifupan-steps">${itemList.map((item, index) => `<div class="aifupan-step"><span class="aifupan-step-index">${index + 1}</span><div class="aifupan-step-main"><div class="aifupan-step-title">${renderInlineRichContent(item)}</div></div></div>`).join('')}</div>`;
    }
    return `<div class="aifupan-block aifupan-steps">${stepList.map((item, index) => {
        const contentHtml = item.content ? `<div class="aifupan-step-content">${renderBlockRichContent(item.content)}</div>` : '';
        return `<div class="aifupan-step"><span class="aifupan-step-index">${index + 1}</span><div class="aifupan-step-main"><div class="aifupan-step-title">${renderInlineRichContent(item.title || `步骤${index + 1}`)}</div>${contentHtml}</div></div>`;
    }).join('')}</div>`;
};

const parseStructuredSteps = (content = '') => {
    const lineList = normalizeLineBreaks(content).split('\n');
    const stepList = [];
    let currentStep = null;
    lineList.forEach(line => {
        const stepMarker = parseStepMarkerLine(line);
        if (stepMarker) {
            if (currentStep) {
                stepList.push({
                    ...currentStep,
                    content: normalizeMixedSource(currentStep.content)
                });
            }
            currentStep = {
                title: stepMarker,
                content: ''
            };
            return;
        }
        const stepTitle = extractStepTitle(line);
        if (stepTitle) {
            if (currentStep) {
                stepList.push({
                    ...currentStep,
                    content: normalizeMixedSource(currentStep.content)
                });
            }
            currentStep = {
                title: stepTitle,
                content: ''
            };
            return;
        }
        if (!currentStep) {
            return;
        }
        currentStep.content += `${line}\n`;
    });
    if (currentStep) {
        stepList.push({
            ...currentStep,
            content: normalizeMixedSource(currentStep.content)
        });
    }
    return stepList.filter(item => item.title);
};

const extractStepTitle = (line = '') => {
    const sourceLine = normalizeLineBreaks(line).trim();
    if (!sourceLine) {
        return '';
    }
    const strippedLine = sourceLine
        .replace(/^\s*(?:[-*+]\s+|\d+[.)、]\s+)/, '')
        .replace(/^>\s*/, '')
        .trim();
    const plainLine = strippedLine
        .replace(/^\*\*(.+)\*\*$/,'$1')
        .replace(/^__(.+)__$/,'$1')
        .trim();
    if (/^步骤\s*\d+\s*[:：]/.test(plainLine) || /^步骤\s*\d+\b/.test(plainLine)) {
        return plainLine;
    }
    return '';
};

const parseStepMarkerLine = (line = '') => {
    const sourceLine = normalizeTagQuotes(normalizeLineBreaks(line).trim());
    if (!sourceLine) {
        return '';
    }
    const itemMatch = sourceLine.match(/^<aifupan-steps-item(?:\s+[^>]*)?name="([^"]+)"(?:\s+[^>]*)?\/>$/i);
    if (itemMatch) {
        return normalizeInlineText(itemMatch[1]) || '';
    }
    const shortMatch = sourceLine.match(/^<aifupan-steps-(\d+)\s*\/>$/i);
    if (shortMatch) {
        return `步骤${shortMatch[1]}`;
    }
    return '';
};

const renderCollapseTag = (tagMeta = {}, innerContent = '') => {
    const titleText = normalizeInlineText(tagMeta.attrs?.title) || '展开查看';
    return `<div class="aifupan-block aifupan-collapse"><details><summary>${renderInlineRichContent(titleText)}</summary>${renderInnerMarkdown(innerContent)}</details></div>`;
};

const renderQuoteModernTag = (innerContent = '') => `<div class="aifupan-block aifupan-quote-modern">${renderInnerMarkdown(innerContent)}</div>`;

const renderColumnsTag = (tagMeta = {}, innerContent = '') => {
    const columnCount = Number(tagMeta.columnCount) || 2;
    const explicitColumnResult = parseRepeatingTagBlocks(innerContent, 'aifupan-column');
    const columnList = explicitColumnResult.blockList.length
        ? explicitColumnResult.blockList
        : normalizeLineBreaks(innerContent)
            .split(/\n-{3,}\n/)
            .map(item => item.trim())
            .filter(Boolean);
    if (!columnList.length) {
        return renderInnerMarkdown(innerContent);
    }
    return `<div class="aifupan-block aifupan-columns aifupan-columns-${columnCount}">${columnList.map(item => renderColumnTag(item)).join('')}</div>`;
};

const renderColumnTag = (innerContent = '') => {
    const columnHtml = renderBlockRichContent(innerContent);
    if (!columnHtml) {
        return '';
    }
    return `<div class="aifupan-column">${columnHtml}</div>`;
};

const renderListTag = (tagMeta = {}, innerContent = '') => {
    const itemList = parseSimpleListItems(innerContent);
    if (!itemList.length) {
        return renderInnerMarkdown(innerContent);
    }
    return `<ul class="aifupan-list aifupan-list-${escapeAttr(tagMeta.listVariant || 'check')}">${itemList.map((item, index) => `<li><span class="aifupan-list-marker">${getListMarker(tagMeta, index)}</span><span>${renderInlineRichContent(item)}</span></li>`).join('')}</ul>`;
};

const renderTableTag = (tagMeta = {}, innerContent = '') => {
    const tableData = parseMarkdownTable(innerContent);
    if (!tableData.headers.length) {
        return renderInnerMarkdown(innerContent);
    }
    return `<div class="aifupan-block">${renderMarkdownTable(tableData, tagMeta.tableVariant)}</div>`;
};

const renderMeterTag = (tagMeta = {}, innerContent = '') => {
    const value = Number(normalizeInlineText(tagMeta.attrs?.value) || normalizeInlineText(innerContent) || 0);
    const min = Number(tagMeta.attrs?.min ?? 0);
    const max = Number(tagMeta.attrs?.max ?? 100);
    const safeMax = max <= min ? min + 1 : max;
    const percentage = Math.max(0, Math.min(100, ((value - min) / (safeMax - min)) * 100));
    return `<div class="aifupan-block aifupan-meter"><div class="aifupan-meter-track"><div class="aifupan-meter-bar" style="width:${percentage}%"></div></div><div class="aifupan-meter-meta">当前值：${escapeHtml(value)} / ${escapeHtml(safeMax)}</div></div>`;
};

const renderMermaidTag = (tagMeta = {}, innerContent = '') => {
    const treeChartHtml = renderMermaidTreeChart(tagMeta, innerContent);
    if (treeChartHtml) {
        return treeChartHtml;
    }
    const titleHtml = `<div class="aifupan-code-head"><span>${renderInlineRichContent(normalizeInlineText(tagMeta.attrs?.title) || '流程图')}</span></div>`;
    return `<div class="aifupan-block aifupan-mermaid">${titleHtml}<pre><code>${escapeHtml(normalizeLineBreaks(innerContent).trim())}</code></pre></div>`;
};

const renderFlowchartTag = (innerContent = '') => {
    const itemList = normalizeLineBreaks(innerContent)
        .split('\n')
        .map(line => line.replace(/^\s*[-*]\s*/, '').trim())
        .filter(Boolean);
    if (!itemList.length) {
        return renderInnerMarkdown(innerContent);
    }
    return `<div class="aifupan-block aifupan-flowchart">${itemList.map((item, index) => `<div class="aifupan-step"><span class="aifupan-step-index">${index + 1}</span><div class="aifupan-step-main"><div class="aifupan-step-title">${renderInlineRichContent(item)}</div></div></div>`).join('')}</div>`;
};

const renderCodeTag = (tagMeta = {}, innerContent = '') => {
    const codeResult = parseFencedCode(innerContent);
    const titleText = normalizeInlineText(tagMeta.attrs?.title) || codeResult.title || '代码片段';
    const languageText = codeResult.language || (tagMeta.type === 'code-diff' ? 'diff' : 'text');
    const codeBody = addLineNumbers(codeResult.code, hasBooleanLikeAttr(tagMeta, 'line-numbers'));
    return `<div class="aifupan-block aifupan-code"><div class="aifupan-code-head"><span>${renderInlineRichContent(titleText)}</span><span>${escapeHtml(languageText)}</span></div><pre><code class="language-${escapeAttr(languageText)}">${codeBody}</code></pre></div>`;
};

const renderTabsTag = (innerContent = '') => {
    const sectionList = parseSectionBlocks(innerContent);
    if (!sectionList.length) {
        return renderInnerMarkdown(innerContent);
    }
    return `<div class="aifupan-block aifupan-tabs"><div class="aifupan-tabs-nav">${sectionList.map(item => `<span class="aifupan-tab-chip">${renderInlineRichContent(item.title)}</span>`).join('')}</div>${sectionList.map(item => `<div class="aifupan-tab-panel">${renderBlockRichContent(item.content)}</div>`).join('')}</div>`;
};

const renderAccordionTag = (innerContent = '') => {
    const sectionList = parseSectionBlocks(innerContent);
    if (!sectionList.length) {
        return renderInnerMarkdown(innerContent);
    }
    return `<div class="aifupan-block aifupan-collapse">${sectionList.map(item => `<details><summary>${renderInlineRichContent(item.title)}</summary>${renderBlockRichContent(item.content)}</details>`).join('')}</div>`;
};

const renderDividerTag = (tagMeta = {}, innerContent = '') => {
    const labelText = normalizeInlineText(innerContent) || normalizeInlineText(tagMeta.attrs?.text) || 'Section';
    return `<div class="aifupan-divider">${renderInlineRichContent(labelText)}</div>`;
};

const renderTooltipTag = (tagMeta = {}, innerContent = '') => {
    const tooltipText = normalizeInlineText(tagMeta.attrs?.text) || '提示';
    return `<span class="aifupan-tooltip" title="${escapeAttr(tooltipText)}">${renderInlineRichContent(innerContent)}</span>`;
};

const renderEmojiTag = (tagMeta = {}, innerContent = '') => {
    const size = normalizeInlineText(tagMeta.attrs?.size) || 'normal';
    const sizeClass = size === 'large' ? 'aifupan-emoji-large' : size === 'xl' ? 'aifupan-emoji-xl' : '';
    return `<span class="aifupan-emoji ${sizeClass}">${escapeHtml(normalizeInlineText(innerContent) || '✨')}</span>`;
};

const renderInnerMarkdown = (content = '') => renderBlockRichContent(content || '');

const renderBlockRichContent = (content = '') => {
    const cleanContent = preprocessRichContent(content, {
        keepAifupanTags: true
    });
    if (!cleanContent) {
        return '';
    }
    const parsedResult = buildTagPlaceholderSource(cleanContent);
    return renderMarkdownWithPlaceholders(parsedResult.source, parsedResult.placeholderList);
};

const renderInlineRichContent = (text = '') => {
    const cleanText = preprocessRichContent(text);
    if (!cleanText) {
        return '';
    }
    return inlineRichMd.renderInline(cleanText).replace(/\n/g, '<br />');
};

const renderInlineMarkdown = (text = '') => {
    const sourceText = String(text ?? '');
    if (!sourceText) {
        return '';
    }
    return inlineMd.renderInline(sourceText).replace(/\n/g, '<br />');
};

const resolveTitleSource = (tagMeta = {}, innerContent = '') => {
    const headingSource = parseMarkdownHeading(innerContent);
    const level = headingSource.level || Math.max(1, Math.min(6, Number(tagMeta.level) || 1));
    const title = headingSource.title || normalizeInlineText(innerContent) || normalizeInlineText(tagMeta.attrs?.title) || '标题';
    return {
        level,
        title
    };
};

const parseMarkdownHeading = (text = '') => {
    const sourceText = normalizeLineBreaks(text).trim();
    const match = sourceText.match(/^(#{1,6})\s+(.+)$/);
    if (!match) {
        return {
            level: 0,
            title: ''
        };
    }
    return {
        level: match[1].length,
        title: normalizeInlineText(match[2])
    };
};

const parseProgressSource = (text = '') => {
    const sourceText = normalizeInlineText(text);
    const valueMatch = sourceText.match(/(\d+(?:\.\d+)?)\s*%/);
    const statusMap = [
        {pattern: /(警告|报警|warning)/i, status: 'warning'},
        {pattern: /(异常|错误|error|exception)/i, status: 'error'},
        {pattern: /(成功|正常|success)/i, status: 'success'}
    ];
    const matchedStatus = statusMap.find(item => item.pattern.test(sourceText));
    return {
        valueText: valueMatch ? valueMatch[1] : '',
        status: matchedStatus?.status || ''
    };
};

const parseLabelValueText = (text = '') => {
    const sourceText = normalizeInlineText(text);
    const matched = sourceText.match(/^(.+?)\s*[:：]\s*(.+)$/);
    if (!matched) {
        return {
            label: '',
            value: ''
        };
    }
    return {
        label: normalizeInlineText(matched[1]),
        value: normalizeInlineText(matched[2])
    };
};

const renderMarkdownHtml = (source = '') => {
    let html = mdFormat(source);
    let renderCount = 0;
    while (renderCount < 2 && hasResidualMarkdownSyntax(html)) {
        const nextHtml = mdFormat(html);
        if (nextHtml === html) {
            break;
        }
        html = nextHtml;
        renderCount += 1;
    }
    return html;
};

const hasResidualMarkdownSyntax = (html = '') => {
    const cleanHtml = String(html || '')
        .replace(/<pre[\s\S]*?<\/pre>/gi, '')
        .replace(/<code[\s\S]*?<\/code>/gi, '')
        .replace(/<svg[\s\S]*?<\/svg>/gi, '');
    return /(^|>|\n)\s*(#{1,6}\s+.+|\d+\.\s+\*\*.+\*\*|[-*]\s+\*\*.+\*\*|\*\*[^*]+\*\*)/m.test(cleanHtml);
};

const renderChartVisual = (chartType = '', tableData = {}, titleText = '') => {
    const normalizedType = normalizeInlineText(chartType).toLowerCase();
    if (normalizedType === 'pie') {
        return renderPieChartVisual(tableData, titleText);
    }
    if (normalizedType === 'bar') {
        return renderBarChartVisual(tableData, titleText);
    }
    if (normalizedType === 'line' || normalizedType === 'area') {
        return renderLineChartVisual(tableData, titleText, normalizedType === 'area');
    }
    return '';
};

const resolveChartTableData = (chartType = '', content = '') => {
    const normalizedType = normalizeInlineText(chartType).toLowerCase();
    if (normalizedType === 'pie') {
        return parsePieChartTableData(content);
    }
    return parseMarkdownTable(content);
};

const roundNumber = (value = 0, decimals = 6) => {
    const num = Number(value);
    if (!Number.isFinite(num)) {
        return 0;
    }
    const d = Math.max(0, Math.min(12, Math.floor(Number(decimals) || 0)));
    const factor = 10 ** d;
    return Math.round((num + Number.EPSILON) * factor) / factor;
};

const formatChartNumber = (value = 0, options = {}) => {
    const num = Number(value);
    if (!Number.isFinite(num)) {
        return '0';
    }
    const maxDecimals = Math.max(0, Math.min(12, Math.floor(Number(options?.maxDecimals ?? 6) || 0)));
    const fixed = roundNumber(num, maxDecimals);
    const snapEps = 10 ** (-maxDecimals);
    const nearestInt = Math.round(fixed);
    if (Math.abs(fixed - nearestInt) <= snapEps) {
        return String(nearestInt);
    }
    let text = fixed.toFixed(maxDecimals);
    if (maxDecimals > 0) {
        text = text.replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '');
    }
    if (text === '-0') {
        return '0';
    }
    return text;
};

const kahanSum = (valueList = []) => {
    const list = Array.isArray(valueList) ? valueList : [];
    let sum = 0;
    let c = 0;
    for (let i = 0; i < list.length; i += 1) {
        const v = Number(list[i]);
        if (!Number.isFinite(v)) {
            continue;
        }
        const y = v - c;
        const t = sum + y;
        c = (t - sum) - y;
        sum = t;
    }
    return sum;
};

const normalizePieChartValuesForRender = (rowList = []) => {
    const rows = Array.isArray(rowList) ? rowList : [];
    const totalRaw = kahanSum(rows.map(item => item.value));
    const percentSignalCount = rows.filter(item => /%/.test(String(item?.rawValue || ''))).length;
    const looksLikePercent = (rows.length > 0 && percentSignalCount >= Math.ceil(rows.length * 0.6))
        || (totalRaw > 0 && Math.abs(totalRaw - 100) <= 0.05);
    if (!looksLikePercent || totalRaw <= 0) {
        return {rowList: rows, totalValue: totalRaw, totalText: formatChartNumber(totalRaw, {maxDecimals: 6})};
    }
    const targetTotal = 100;
    const factor = totalRaw ? targetTotal / totalRaw : 1;
    const normalized = rows.map(item => ({...item, value: Number(item.value) * factor}));
    const fixedRows = [];
    let sumFixed = 0;
    for (let i = 0; i < normalized.length; i += 1) {
        const item = normalized[i];
        const fixedValue = i === normalized.length - 1
            ? Math.max(0, targetTotal - sumFixed)
            : Math.max(0, roundNumber(item.value, 6));
        sumFixed += fixedValue;
        fixedRows.push({...item, value: fixedValue});
    }
    return {rowList: fixedRows, totalValue: targetTotal, totalText: '100'};
};

const renderPieChartVisual = (tableData = {}, titleText = '') => {
    const rowList = normalizePieChartPairs(tableData);
    const normalized = normalizePieChartValuesForRender(rowList);
    const renderRowList = normalized.rowList;
    const totalValue = normalized.totalValue;
    if (!rowList.length || totalValue <= 0) {
        return `<div class="aifupan-chart-visual"><div class="aifupan-chart-empty">当前饼图数据不足，暂时仅展示表格数据。</div></div>`;
    }
    const colorList = getChartColorPalette();
    let currentAngle = -Math.PI / 2;
    const sliceHtml = renderRowList.map((item, index) => {
        const angle = (item.value / totalValue) * Math.PI * 2;
        const nextAngle = currentAngle + angle;
        const path = describeArcPath(110, 110, 76, currentAngle, nextAngle);
        currentAngle = nextAngle;
        return `<path d="${path}" fill="${colorList[index % colorList.length]}"></path>`;
    }).join('');
    const legendHtml = renderRowList.map((item, index) => `<span class="aifupan-chart-legend-item"><i class="aifupan-chart-legend-color" style="background:${colorList[index % colorList.length]}"></i><span>${renderInlineRichContent(`${item.name} ${formatChartPercent(item.value, totalValue)}`)}</span></span>`).join('');
    return `<div class="aifupan-chart-visual"><svg class="aifupan-chart-svg" viewBox="0 0 220 220" role="img" aria-label="${escapeAttr(titleText || '饼图')}">${sliceHtml}<circle cx="110" cy="110" r="40" fill="#ffffff"></circle><text x="110" y="105" text-anchor="middle" font-size="13" font-weight="800" fill="#1d4ed8">总计</text><text x="110" y="126" text-anchor="middle" font-size="15" font-weight="800" fill="#0f172a">${escapeHtml(String(normalized.totalText))}</text></svg><div class="aifupan-chart-legend">${legendHtml}</div></div>`;
};

/**
 * @description 统一处理 x 轴类目文字（用于解决横向文字重叠）
 * @param {string} label x 轴类目名称
 * @returns {string} 文本内容（已 escape）
 */
const renderAxisLabelText = (label = '') => escapeHtml(String(label || ''));

/**
 * @description x 轴文字旋转配置（按“度数 + 方向”可配置）
 * @type {{bar: {angle: number, direction: 'left'|'right'}, line: {angle: number, direction: 'left'|'right'}}}
 */
const AXIS_LABEL_ROTATE_CONFIG = {
    bar: {angle: 45, direction: 'left'},
    line: {angle: 45, direction: 'right'}
};

/**
 * @description 生成 x 轴类目文字的 SVG 属性（按“度数 + 方向”实现可配置化）
 * @param {number} x 文字锚点 x
 * @param {number} y 文字锚点 y
 * @param {{angle?: number, direction?: 'left'|'right'}} rotateConfig 旋转配置
 * @returns {{x: number, y: number, transform: string, textAnchor: string}} SVG 属性
 */
const buildAxisLabelRotateAttr = (x = 0, y = 0, rotateConfig = {}) => {
    const angle = Math.max(0, Number(rotateConfig?.angle) || 0);
    const direction = rotateConfig?.direction === 'right' ? 'right' : 'left';
    const rotateDeg = direction === 'right' ? angle : -angle;
    const textAnchor = direction === 'right' ? 'start' : 'end';
    const offsetX = direction === 'right' ? -2 : 2;
    const nextX = x + offsetX;
    return {
        x: nextX,
        y,
        transform: `rotate(${rotateDeg} ${nextX} ${y})`,
        textAnchor
    };
};

const renderBarChartVisual = (tableData = {}, titleText = '') => {
    const rowList = normalizeChartPairs(tableData);
    if (rowList.some(item => !item.valid)) {
        return '';
    }
    const maxValue = Math.max(...rowList.map(item => item.value), 0);
    if (!rowList.length || maxValue <= 0) {
        return `<div class="aifupan-chart-visual"><div class="aifupan-chart-empty">当前柱状图数据不足，暂时仅展示表格数据。</div></div>`;
    }
    const chartWidth = 420;
    const baseChartHeight = 220;
    const top = 18;
    const left = 48;
    const labelRotateConfig = AXIS_LABEL_ROTATE_CONFIG.bar;
    const maxLabelLength = Math.max(...rowList.map(item => Array.from(String(item.name || '')).length), 1);
    const labelFontSize = 11;
    const labelOffset = 18;
    const labelEstimatedWidth = maxLabelLength * 9;
    const estimatedVertical = Math.ceil(labelEstimatedWidth * Math.sin((Math.max(0, Number(labelRotateConfig?.angle) || 0) * Math.PI) / 180));
    const requiredBottom = Math.max(44, 14 + labelOffset + labelFontSize + estimatedVertical);
    const bottomBaseline = 92;
    const bottom = requiredBottom;
    const chartHeight = baseChartHeight + Math.max(0, bottom - bottomBaseline);
    const availableWidth = chartWidth - left - 18;
    const stepWidth = availableWidth / rowList.length;
    const barWidth = Math.min(36, stepWidth * 0.56);
    const barHtml = rowList.map((item, index) => {
        const x = left + index * stepWidth + (stepWidth - barWidth) / 2;
        const maxBarHeight = Math.max(60, chartHeight - bottom - top - 18);
        const height = (item.value / maxValue) * maxBarHeight;
        const y = chartHeight - bottom - height;
        const labelCenterX = x + barWidth / 2;
        const labelY = chartHeight - bottom + labelOffset;
        const labelAttr = buildAxisLabelRotateAttr(labelCenterX, labelY, labelRotateConfig);
        return `<g><rect x="${x}" y="${y}" width="${barWidth}" height="${height}" rx="10" fill="url(#aifupanBarGradient)"></rect><text x="${labelAttr.x}" y="${labelAttr.y}" transform="${labelAttr.transform}" text-anchor="${labelAttr.textAnchor}" font-size="${labelFontSize}" fill="#64748b">${renderAxisLabelText(item.name)}</text><text x="${labelCenterX}" y="${y - 6}" text-anchor="middle" font-size="11" font-weight="700" fill="#1d4ed8">${escapeHtml(formatChartNumber(item.value, {maxDecimals: 6}))}</text></g>`;
    }).join('');
    return `<div class="aifupan-chart-visual"><svg class="aifupan-chart-svg" style="overflow: visible" viewBox="0 0 ${chartWidth} ${chartHeight}" role="img" aria-label="${escapeAttr(titleText || '柱状图')}"><defs><linearGradient id="aifupanBarGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#6d5dfc"></stop><stop offset="100%" stop-color="#2ec5ff"></stop></linearGradient></defs><line x1="${left}" y1="${top}" x2="${left}" y2="${chartHeight - bottom}" stroke="#cbd5e1"></line><line x1="${left}" y1="${chartHeight - bottom}" x2="${chartWidth - 10}" y2="${chartHeight - bottom}" stroke="#cbd5e1"></line>${barHtml}</svg></div>`;
};

const renderLineChartVisual = (tableData = {}, titleText = '', fillArea = false) => {
    const rowList = normalizeChartPairs(tableData);
    if (rowList.some(item => !item.valid)) {
        return '';
    }
    const maxValue = Math.max(...rowList.map(item => item.value), 0);
    if (!rowList.length || maxValue <= 0) {
        return `<div class="aifupan-chart-visual"><div class="aifupan-chart-empty">当前折线图数据不足，暂时仅展示表格数据。</div></div>`;
    }
    const chartWidth = 420;
    const baseChartHeight = 220;
    const left = 30;
    const top = 20;
    const labelRotateConfig = AXIS_LABEL_ROTATE_CONFIG.line;
    const labelAngleRad = (Math.max(0, Number(labelRotateConfig?.angle) || 0) * Math.PI) / 180;
    const maxLabelLength = Math.max(...rowList.map(item => Array.from(String(item.name || '')).length), 1);
    const labelFontSize = 11;
    const labelOffset = 20;
    const labelEstimatedWidth = maxLabelLength * 9;
    const estimatedVertical = Math.ceil(labelEstimatedWidth * Math.sin(labelAngleRad));
    // x 轴 label 以最右侧数据点为锚点旋转 45° 后向右下延伸，水平投影会超出画布导致容器外被裁剪；
    // 按最长 label 的水平投影预留右侧边距，保证最右侧 label 完整展示在容器内
    const labelHorizontalProjection = Math.ceil(labelEstimatedWidth * Math.cos(labelAngleRad));
    const right = Math.max(20, labelHorizontalProjection + 6);
    const requiredBottom = Math.max(48, 16 + labelOffset + labelFontSize + estimatedVertical);
    const bottomBaseline = 92;
    const bottom = requiredBottom;
    const chartHeight = baseChartHeight + Math.max(0, bottom - bottomBaseline);
    const plotWidth = chartWidth - left - right;
    const plotHeight = chartHeight - top - bottom;
    const pointList = rowList.map((item, index) => {
        const x = left + (plotWidth / Math.max(rowList.length - 1, 1)) * index;
        const y = top + plotHeight - (item.value / maxValue) * plotHeight;
        return {x, y, ...item};
    });
    const polylinePoints = pointList.map(item => `${item.x},${item.y}`).join(' ');
    const areaPoints = `${left},${chartHeight - bottom} ${polylinePoints} ${left + plotWidth},${chartHeight - bottom}`;
    const pointHtml = pointList.map(item => {
        const labelY = chartHeight - bottom + labelOffset;
        const labelAttr = buildAxisLabelRotateAttr(item.x, labelY, labelRotateConfig);
        return `<g><circle cx="${item.x}" cy="${item.y}" r="4.5" fill="#ffffff" stroke="#1d4ed8" stroke-width="2"></circle><text x="${item.x}" y="${item.y - 10}" text-anchor="middle" font-size="11" font-weight="700" fill="#1d4ed8">${escapeHtml(formatChartNumber(item.value, {maxDecimals: 6}))}</text><text x="${labelAttr.x}" y="${labelAttr.y}" transform="${labelAttr.transform}" text-anchor="${labelAttr.textAnchor}" font-size="${labelFontSize}" fill="#64748b">${renderAxisLabelText(item.name)}</text></g>`;
    }).join('');
    return `<div class="aifupan-chart-visual"><svg class="aifupan-chart-svg" style="overflow: visible" viewBox="0 0 ${chartWidth} ${chartHeight}" role="img" aria-label="${escapeAttr(titleText || '折线图')}"><defs><linearGradient id="aifupanLineAreaGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#6d5dfc" stop-opacity="0.28"></stop><stop offset="100%" stop-color="#6d5dfc" stop-opacity="0.02"></stop></linearGradient></defs><line x1="${left}" y1="${top}" x2="${left}" y2="${chartHeight - bottom}" stroke="#cbd5e1"></line><line x1="${left}" y1="${chartHeight - bottom}" x2="${chartWidth - right}" y2="${chartHeight - bottom}" stroke="#cbd5e1"></line>${fillArea ? `<polygon points="${areaPoints}" fill="url(#aifupanLineAreaGradient)"></polygon>` : ''}<polyline points="${polylinePoints}" fill="none" stroke="#1d4ed8" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"></polyline>${pointHtml}</svg></div>`;
};

const normalizeChartPairs = (tableData = {}) => {
    const headerList = tableData?.headers || [];
    const rowList = tableData?.rows || [];
    if (headerList.length < 2) {
        return [];
    }
    return rowList.map(row => {
        const rawValue = String(row?.[1] ?? '');
        const parsedValue = parseChartSingleValue(rawValue);
        return {
            name: normalizeInlineText(row?.[0]) || '未命名',
            value: parsedValue.value,
            valid: parsedValue.valid,
            rawValue: rawValue.trim()
        };
    }).filter(item => item.name);
};

const normalizePieChartPairs = (tableData = {}) => normalizeChartPairs(tableData)
    .filter(item => item.valid && item.value > 0);

const parseChartSingleValue = (value = '') => {
    const sourceText = normalizeInlineText(value);
    if (!sourceText) {
        return {value: 0, valid: false};
    }
    if (/(->|→|<-|←|~|～|至|到)/.test(sourceText)) {
        return {value: 0, valid: false};
    }
    const numberList = sourceText.match(/-?\d+(?:\.\d+)?/g) || [];
    if (numberList.length !== 1) {
        return {value: 0, valid: false};
    }
    return {
        value: Number(numberList[0]) || 0,
        valid: true
    };
};

const parsePieChartTableData = (content = '') => {
    const markdownTable = parseMarkdownTable(content);
    if (markdownTable.headers.length >= 2) {
        return markdownTable;
    }
    const tableBlock = extractStandaloneMarkdownTable(content);
    if (tableBlock) {
        return parseMarkdownTable(tableBlock);
    }
    return markdownTable;
};

const getChartColorPalette = () => ['#6d5dfc', '#2ec5ff', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316'];

const polarToCartesian = (cx = 0, cy = 0, radius = 0, angle = 0) => ({
    x: cx + radius * Math.cos(angle),
    y: cy + radius * Math.sin(angle)
});

const describeArcPath = (cx = 0, cy = 0, radius = 0, startAngle = 0, endAngle = 0) => {
    const start = polarToCartesian(cx, cy, radius, startAngle);
    const end = polarToCartesian(cx, cy, radius, endAngle);
    const largeArcFlag = endAngle - startAngle > Math.PI ? 1 : 0;
    return [`M ${cx} ${cy}`, `L ${start.x} ${start.y}`, `A ${radius} ${radius} 0 ${largeArcFlag} 1 ${end.x} ${end.y}`, 'Z'].join(' ');
};

const formatChartPercent = (value = 0, total = 0) => {
    if (!total) {
        return '0%';
    }
    const percent = (Number(value) / Number(total)) * 100;
    const fixed = roundNumber(percent, 2);
    const normalized = Object.is(fixed, -0) ? 0 : fixed;
    return `${Number(normalized).toFixed(2).replace(/\.00$/, '')}%`;
};

/**
 * @description 归一化主题颜色输入，兼容命名色、十六进制与 rgb/rgba 写法。
 * @param {string} color 原始颜色值
 * @returns {string}
 */
function normalizeThemeColor(color = '') {
    const colorText = String(color ?? '').trim().toLowerCase();
    if (!colorText) {
        return '';
    }
    const colorMap = {
        red: '#ef4444',
        blue: '#2563eb',
        green: '#16a34a',
        yellow: '#ca8a04',
        orange: '#ea580c',
        purple: '#7c3aed',
        pink: '#db2777',
        black: '#111827',
        white: '#ffffff',
        gray: '#6b7280',
        grey: '#6b7280',
        cyan: '#0891b2',
        teal: '#0f766e'
    };
    if (colorMap[colorText]) {
        return colorMap[colorText];
    }
    if (/^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(colorText)) {
        return colorText;
    }
    if (/^rgb(a)?\(/i.test(colorText)) {
        return colorText;
    }
    return '';
}

const normalizeFontSize = (size = '') => {
    const sizeText = normalizeInlineText(size);
    if (!sizeText) {
        return '';
    }
    if (/^\d+(px|em|rem|%)$/i.test(sizeText)) {
        return sizeText;
    }
    if (/^\d+$/.test(sizeText)) {
        return `${sizeText}px`;
    }
    return '';
};

const normalizePercentValue = (value = '') => Math.max(0, Math.min(100, Number(String(value).replace(/[^\d.]/g, '')) || 0));

const normalizeProgressStatus = (status = '') => {
    const normalizedStatus = normalizeInlineText(status).toLowerCase();
    if (!normalizedStatus) {
        return '';
    }
    if (normalizedStatus === 'exception') {
        return 'error';
    }
    return ['success', 'warning', 'error'].includes(normalizedStatus) ? normalizedStatus : '';
};

const parseBooleanAttr = (value) => {
    if (typeof value === 'boolean') {
        return value;
    }
    const normalizedValue = normalizeInlineText(value).toLowerCase();
    if (!normalizedValue) {
        return false;
    }
    if (['false', '0', 'no', 'off'].includes(normalizedValue)) {
        return false;
    }
    return ['true', '1', 'yes', 'on'].includes(normalizedValue);
};

const normalizeTrendDirection = (value = '', positive = false, negative = false) => {
    const normalizedValue = normalizeInlineText(value).toLowerCase();
    if (['up', 'increase', 'positive', 'rise', 'higher', 'asc'].includes(normalizedValue)) {
        return 'up';
    }
    if (['down', 'decrease', 'negative', 'decline', 'lower', 'desc'].includes(normalizedValue)) {
        return 'down';
    }
    if (positive) {
        return 'up';
    }
    if (negative) {
        return 'down';
    }
    return 'flat';
};

const getTrendDirectionLabel = (direction = '') => {
    if (direction === 'up') {
        return '上升';
    }
    if (direction === 'down') {
        return '下降';
    }
    return '持平';
};

const isMeaningfulStatisticValue = (value = '', prefix = '', suffix = '') => {
    const text = `${prefix || ''}${value || ''}${suffix || ''}`.trim();
    if (!text) {
        return false;
    }
    return /[\d]/.test(text) || /[%¥$]/.test(text);
};

const normalizeStatisticCol = (value = '') => {
    const col = Number(normalizeInlineText(value) || 1);
    if (!col || Number.isNaN(col)) {
        return 1;
    }
    return Math.max(1, Math.min(6, col));
};

const parseAifupanOpenTag = (tagText = '') => {
    const normalizedTagText = normalizeTagQuotes(tagText);
    const match = normalizedTagText.match(/^<\s*(aifupan-[^\s/>]+)([\s\S]*?)(\/?)>$/);
    if (!match) {
        return null;
    }
    const rawName = match[1];
    const attrsText = match[2] || '';
    const selfClosing = match[3] === '/';
    const rawType = rawName.replace(/^aifupan-/, '');
    const segmentList = rawType.split('-').filter(Boolean);
    const attrs = parseTagAttributes(attrsText);
    return {
        rawName,
        rawType,
        selfClosing,
        attrs,
        type: getTagType(segmentList),
        level: segmentList[0] === 'title' ? segmentList[1] : '',
        titleVariant: segmentList[0] === 'title' ? (segmentList[2] || normalizeInlineText(attrs?.variant)) : '',
        chartType: segmentList[0] === 'echarts' ? (segmentList[1] || 'line') : '',
        percentage: segmentList[0] === 'progress' ? (segmentList.find(item => /^\d+$/.test(item)) || '') : '',
        status: segmentList[0] === 'progress' ? (segmentList.find(item => !/^\d+$/.test(item) && item !== 'progress') || '') : '',
        alertType: segmentList[0] === 'alert' ? (segmentList[1] || 'info') : '',
        tagType: segmentList[0] === 'tag' ? (segmentList[1] || 'default') : '',
        listVariant: segmentList[0] === 'list' ? (segmentList[1] || 'check') : '',
        tableVariant: segmentList[0] === 'table' ? (segmentList[1] || 'striped') : '',
        columnCount: segmentList[0] === 'columns' ? (segmentList[1] || '2') : '',
        themeName: segmentList[0] === 'theme' ? (segmentList[1] || normalizeInlineText(attrs?.name)) : '',
        skinName: segmentList[0] === 'skin' ? (segmentList[1] || normalizeInlineText(attrs?.name)) : ''
    };
};

const getTagType = (segmentList = []) => {
    if (!segmentList.length) {
        return '';
    }
    if (segmentList[0] === 'mark' && segmentList[1] === 'red') {
        return 'mark-red';
    }
    if (segmentList[0] === 'text' && segmentList[1] === 'red') {
        return 'text-red';
    }
    if (segmentList[0] === 'quote' && segmentList[1] === 'modern') {
        return 'quote-modern';
    }
    if (segmentList[0] === 'card' && segmentList[1] === 'head') {
        return 'card-head';
    }
    if (segmentList[0] === 'card' && segmentList[1] === 'body') {
        return 'card-body';
    }
    if (segmentList[0] === 'card' && segmentList[1] === 'hover') {
        return 'card';
    }
    if (segmentList[0] === 'list') {
        return 'list';
    }
    if (segmentList[0] === 'table') {
        return 'table';
    }
    if (segmentList[0] === 'statistic' && segmentList[1] === 'trend') {
        return 'statistic-trend';
    }
    if (segmentList[0] === 'code' && segmentList[1] === 'diff') {
        return 'code-diff';
    }
    if (segmentList[0] === 'columns') {
        return 'columns';
    }
    if (segmentList[0] === 'theme') {
        return 'theme';
    }
    if (segmentList[0] === 'skin') {
        return 'skin';
    }
    return segmentList[0];
};

const parseTagAttributes = (attrsText = '') => {
    const normalizedAttrsText = normalizeTagQuotes(attrsText);
    const attrs = {};
    const pattern = /([:@\w-]+)\s*=\s*"([^"]*)"/g;
    let match = null;
    while ((match = pattern.exec(normalizedAttrsText))) {
        attrs[match[1]] = match[2];
    }
    const boolPattern = /\s([:@\w-]+)(?=\s|$)/g;
    let boolMatch = null;
    while ((boolMatch = boolPattern.exec(normalizedAttrsText))) {
        if (!attrs[boolMatch[1]]) {
            attrs[boolMatch[1]] = 'true';
        }
    }
    return attrs;
};

const normalizeTagQuotes = (text = '') => String(text || '')
    .replace(/[“”]/g, '"')
    .replace(/[‘’]/g, '\'');

const parseMarkdownTable = (tableText = '') => {
    const lineList = normalizeLineBreaks(tableText)
        .split('\n')
        .map(line => line.trim())
        .filter(Boolean)
        .filter(line => line.startsWith('|') && line.endsWith('|'));
    if (lineList.length < 2) {
        return {
            headers: [],
            rows: []
        };
    }
    const headers = splitTableRow(lineList[0]);
    const rows = lineList.slice(2)
        .map(splitTableRow)
        .filter(row => row.length);
    return {
        headers,
        rows
    };
};

const extractNextMarkdownTableBlock = (text = '', startIndex = 0) => {
    const sourceText = normalizeLineBreaks(text || '');
    let cursor = startIndex;
    while (cursor < sourceText.length && /\s/.test(sourceText[cursor])) {
        cursor += 1;
    }
    const sliceText = sourceText.slice(cursor);
    const matched = sliceText.match(/^((?:\|[^\n]*\|\n?){2,})/);
    if (!matched) {
        return {
            content: '',
            endIndex: startIndex
        };
    }
    return {
        content: matched[1].trim(),
        endIndex: cursor + matched[1].length
    };
};

const extractStandaloneMarkdownTable = (text = '') => {
    const matched = normalizeLineBreaks(text || '').match(/((?:^\|[^\n]*\|\n?){2,})/m);
    return matched ? matched[1].trim() : '';
};

const splitTableRow = (line = '') => line
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map(cell => cell.trim());

const renderMarkdownTable = (tableData = {}, variant = 'striped') => {
    const headers = tableData?.headers || [];
    const rows = tableData?.rows || [];
    if (!headers.length) {
        return '';
    }
    const safeVariant = normalizeInlineText(variant).toLowerCase() === 'dark' ? 'striped' : (variant || 'striped');
    const theadHtml = `<thead><tr>${headers.map(item => `<th>${renderInlineRichContent(item)}</th>`).join('')}</tr></thead>`;
    const tbodyHtml = `<tbody>${rows.map(row => `<tr>${row.map(cell => `<td>${renderInlineRichContent(cell)}</td>`).join('')}</tr>`).join('')}</tbody>`;
    return `<table class="aifupan-table aifupan-table-${escapeAttr(safeVariant)}">${theadHtml}${tbodyHtml}</table>`;
};

const parseTimelineItems = (content = '') => normalizeLineBreaks(content)
    .split('\n')
    .map(line => line.replace(/^\s*[-*]\s*/, '').trim())
    .filter(Boolean)
    .map(item => {
        const matched = item.match(/^(\S+)\s+(.+)$/);
        if (!matched) {
            return {
                time: '时间节点',
                content: item
            };
        }
        return {
            time: matched[1],
            content: matched[2]
        };
    });

const parseSimpleListItems = (content = '') => normalizeLineBreaks(content)
    .split('\n')
    .map(line => line.replace(/^\s*[-*]\s*/, '').trim())
    .filter(Boolean);

const getListMarker = (tagMeta = {}, index = 0) => {
    const variant = tagMeta.listVariant || 'check';
    if (variant === 'steps') {
        return String(index + 1);
    }
    if (variant === 'icon') {
        return getIconMarker(tagMeta.attrs?.icon);
    }
    return '✓';
};

const getIconMarker = (icon = '') => {
    const iconText = normalizeInlineText(icon).toLowerCase();
    const iconMap = {
        check: '✅',
        'check-circle': '✅',
        checked: '✅',
        success: '✅',
        done: '✅',
        uncheck: '⬜',
        unchecked: '⬜',
        empty: '⬜',
        close: '❌',
        'x-circle': '❌',
        cross: '❌',
        error: '❌',
        warn: '⚠️',
        warning: '⚠️',
        info: 'ℹ️',
        star: '⭐',
        arrow: '➡️',
        right: '➡️',
        fire: '🔥',
        idea: '💡'
    };
    if (iconMap[iconText]) {
        return iconMap[iconText];
    }
    return '*';
};

const parseFencedCode = (content = '') => {
    const matched = normalizeLineBreaks(content).match(/```([\w-]*)\n([\s\S]*?)```/);
    if (!matched) {
        return {
            language: '',
            code: normalizeLineBreaks(content).trim(),
            title: ''
        };
    }
    return {
        language: matched[1] || '',
        code: matched[2] || '',
        title: ''
    };
};

const addLineNumbers = (code = '', needLineNumbers = false) => {
    const safeCode = escapeHtml(code || '');
    if (!needLineNumbers) {
        return safeCode;
    }
    return safeCode
        .split('\n')
        .map((line, index) => `${String(index + 1).padStart(2, '0')}  ${line}`)
        .join('\n');
};

const hasBooleanLikeAttr = (tagMeta = {}, attrName = '') => Object.prototype.hasOwnProperty.call(tagMeta?.attrs || {}, attrName);

const parseSectionBlocks = (content = '') => {
    const lineList = normalizeLineBreaks(content).split('\n');
    const sectionList = [];
    let currentSection = null;
    lineList.forEach(line => {
        const matched = line.match(/^##\s+(.+)$/);
        if (matched) {
            if (currentSection) {
                sectionList.push(currentSection);
            }
            currentSection = {
                title: matched[1].trim(),
                content: ''
            };
            return;
        }
        if (!currentSection) {
            currentSection = {
                title: '内容',
                content: ''
            };
        }
        currentSection.content += `${line}\n`;
    });
    if (currentSection) {
        sectionList.push({
            ...currentSection,
            content: currentSection.content.trim()
        });
    }
    return sectionList.filter(item => item.title || item.content);
};

const parseNamedTagBlocks = (content = '', rawNameList = []) => {
    let restContent = normalizeLineBreaks(content);
    const blockMap = {};
    rawNameList.forEach(rawName => {
        const blockList = [];
        const pattern = new RegExp(`<${rawName}(?:\\s+[^>]*)?>([\\s\\S]*?)<\\/${rawName}>`, 'gi');
        restContent = restContent.replace(pattern, (_, blockContent = '') => {
            const normalizedBlock = normalizeMixedSource(blockContent);
            if (normalizedBlock) {
                blockList.push(normalizedBlock);
            }
            return '\n';
        });
        if (blockList.length) {
            blockMap[rawName] = blockList.join('\n\n');
        }
    });
    return {
        blockMap,
        restContent: normalizeMixedSource(restContent)
    };
};

const parseRepeatingTagBlocks = (content = '', rawName = '') => {
    const blockList = [];
    const pattern = new RegExp(`<${rawName}(?:\\s+[^>]*)?>([\\s\\S]*?)<\\/${rawName}>`, 'gi');
    normalizeLineBreaks(content).replace(pattern, (_, blockContent = '') => {
        const normalizedBlock = normalizeMixedSource(blockContent);
        if (normalizedBlock) {
            blockList.push(normalizedBlock);
        }
        return '';
    });
    return {
        blockList
    };
};

const stripAifupanFragments = (text = '') => normalizeLineBreaks(text)
    .replace(/<\/?aifupan-[^>]*>/g, '')
    .replace(/<\/?aifupan-[^\n]*$/g, '');

const preprocessRichContent = (content = '', option = {}) => {
    const sourceText = normalizeLineBreaks(decodeSerializedText(content ?? ''));
    if (!sourceText) {
        return '';
    }
    const normalizedText = option?.keepAifupanTags ? sourceText : stripAifupanFragments(sourceText);
    return normalizedText
        .replace(/^\s+|\s+$/g, '')
        .replace(/<br\s*\/?>/gi, '<br />');
};

const normalizeMixedSource = (text = '') => text
    .replace(/\n{4,}/g, '\n\n\n')
    .trim();

const normalizeLineBreaks = (text = '') => text.replace(/\r\n/g, '\n');

const normalizeInlineText = (text = '') => String(text ?? '').trim();

const normalizeThemeName = (value = '') => {
    const themeName = normalizeInlineText(value);
    const validThemeList = Object.values(AIFUPAN_THEME_MAP);
    if (themeName && validThemeList.includes(themeName)) {
        return themeName;
    }
    return AIFUPAN_THEME_MAP.BUSINESS;
};

const normalizeSkinName = (value = '') => {
    const skinName = normalizeInlineText(value);
    const validSkinList = Object.values(AIFUPAN_SKIN_MAP);
    if (skinName && validSkinList.includes(skinName)) {
        return skinName;
    }
    return AIFUPAN_SKIN_MAP.PAPER;
};

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

const wrapRootHtml = (html = '', option = {}) => {
    const themeName = normalizeThemeName(option?.themeName);
    const skinName = normalizeSkinName(option?.skinName);
    return `<div class="ai-mdtag-render-root ai-mdtag-theme-${escapeAttr(themeName)} ai-mdtag-skin-${escapeAttr(skinName)}">${html}</div>`;
};

const resolveThemeSkin = (parsedResult = {}, sourceText = '') => {
    const explicitThemeName = normalizeInlineText(parsedResult?.themeName);
    const explicitSkinName = normalizeInlineText(parsedResult?.skinName);
    if (explicitThemeName || explicitSkinName) {
        return {
            themeName: normalizeThemeName(explicitThemeName || getDefaultThemeBySkin(explicitSkinName)),
            skinName: normalizeSkinName(explicitSkinName || getDefaultSkinByTheme(explicitThemeName))
        };
    }
    return autoMatchThemeSkin(sourceText);
};

const autoMatchThemeSkin = (sourceText = '') => {
    const text = normalizeLineBreaks(sourceText).toLowerCase();
    const tagText = (text.match(/aifupan-[a-z0-9-]+/g) || []).join(' ');

    const isTechTheme = hasMatchedKeyword(text, [
        '技术方案', '系统架构', '架构设计', '监控看板', '日志分析', '接口文档', '代码', 'sql', 'mermaid', 'echarts', 'dashboard', 'api'
    ]) || hasMatchedKeyword(tagText, [
        'aifupan-mermaid', 'aifupan-code', 'aifupan-code-diff', 'aifupan-echarts', 'aifupan-flowchart'
    ]);

    if (isTechTheme) {
        return {
            themeName: AIFUPAN_THEME_MAP.AURORA,
            skinName: AIFUPAN_SKIN_MAP.GLASS
        };
    }

    const isWarmTheme = hasMatchedKeyword(text, [
        '活动总结', '庆典', '海报', '亮点', '故事', '温馨', '暖场', '节日', '回顾', '品牌故事', '视觉展示'
    ]) || hasMatchedKeyword(tagText, [
        'aifupan-emoji', 'aifupan-badge', 'aifupan-highlight'
    ]);

    if (isWarmTheme) {
        return {
            themeName: AIFUPAN_THEME_MAP.WARM,
            skinName: AIFUPAN_SKIN_MAP.GLASS
        };
    }

    const isBusinessTheme = hasMatchedKeyword(text, [
        '复盘', '分析', '报告', '周报', '月报', '经营', '指标', '数据', '项目进度', '汇报', '总结'
    ]) || hasMatchedKeyword(tagText, [
        'aifupan-statistic', 'aifupan-progress', 'aifupan-table', 'aifupan-callout', 'aifupan-note', 'aifupan-card'
    ]);

    if (isBusinessTheme) {
        return {
            themeName: AIFUPAN_THEME_MAP.BUSINESS,
            skinName: AIFUPAN_SKIN_MAP.PAPER
        };
    }

    const needMinimalSkin = !/aifupan-/.test(text) || hasMatchedKeyword(text, [
        '说明文档', '接口清单', '规则说明', '规范', '配置说明'
    ]);

    return {
        themeName: AIFUPAN_THEME_MAP.BUSINESS,
        skinName: needMinimalSkin ? AIFUPAN_SKIN_MAP.MINIMAL : AIFUPAN_SKIN_MAP.PAPER
    };
};

const hasMatchedKeyword = (text = '', keywordList = []) => keywordList.some(keyword => text.includes(String(keyword).toLowerCase()));

const getDefaultThemeBySkin = (skinName = '') => {
    if (skinName === AIFUPAN_SKIN_MAP.PAPER) {
        return AIFUPAN_THEME_MAP.BUSINESS;
    }
    return AIFUPAN_THEME_MAP.BUSINESS;
};

const getDefaultSkinByTheme = (themeName = '') => {
    if (themeName === AIFUPAN_THEME_MAP.BUSINESS) {
        return AIFUPAN_SKIN_MAP.PAPER;
    }
    return AIFUPAN_SKIN_MAP.GLASS;
};

const getLineEndIndex = (text = '', start = 0) => {
    const lineEnd = text.indexOf('\n', start);
    return lineEnd < 0 ? text.length : lineEnd;
};

const parseMetaHeader = (text = '') => {
    const lineList = normalizeLineBreaks(text).split('\n');
    const firstLine = lineList[0]?.trim();
    if (!firstLine?.startsWith('---format=')) {
        return null;
    }
    const format = firstLine.slice('---format='.length).trim();
    let contentStartIndex = 1;
    if (lineList[1]?.trim()?.startsWith('---version=')) {
        contentStartIndex = 2;
    }
    return {
        format,
        content: lineList.slice(contentStartIndex).join('\n')
    };
};

const escapeHtml = (text = '') => String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const escapeAttr = (text = '') => escapeHtml(text).replace(/`/g, '&#96;');

const escapeRegExp = (text = '') => String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

export {
    renderAiResponseContent
};

export default renderAiResponseContent;

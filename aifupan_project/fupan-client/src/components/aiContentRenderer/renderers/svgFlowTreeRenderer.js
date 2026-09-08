/**
 * @file Mermaid 树形 SVG 渲染器。
 * @description 将 Mermaid 流程文本解析为纵向 PRD 风格的树形分支 SVG。
 */

const FLOW_LAYOUT_CONFIG = {
    canvasMinWidth: 760,
    paddingX: 34,
    paddingY: 28,
    paddingBottom: 40,
    rootGap: 72,
    childGap: 34,
    levelGap: 120,
    regularWidth: 200,
    regularHeight: 56,
    decisionWidth: 160,
    decisionHeight: 92
};
let mermaidTreeRenderSeed = 0;

/**
 * @description 渲染 Mermaid 流程图为纵向树形 SVG。
 * @param {object} tagMeta 自定义标签元信息。
 * @param {string} innerContent Mermaid 原始内容。
 * @returns {string} Mermaid 树形图 HTML。
 */
const renderMermaidTreeChart = (tagMeta = {}, innerContent = '') => {
    const mermaidSource = extractMermaidSource(innerContent);
    const graphData = parseMermaidTreeSource(mermaidSource);
    if (graphData.nodeList.length < 2 || graphData.edgeList.length < 1) {
        return '';
    }
    mermaidTreeRenderSeed += 1;
    const renderId = `aifupanTree${mermaidTreeRenderSeed}`;
    const layout = buildMermaidTreeLayout(graphData);
    const titleText = normalizeText(tagMeta?.attrs?.title) || '流程图';
    const svgHtml = renderMermaidTreeSvg(layout, titleText, renderId);
    return `<div class="aifupan-block aifupan-mermaid aifupan-mermaid-tree"><div class="aifupan-code-head"><span>${escapeHtml(titleText)}</span></div><div class="aifupan-process-tree-wrap">${svgHtml}</div></div>`;
};

/**
 * @description 提取 Mermaid 源码，兼容标准 fenced code block 与纯 Mermaid 文本。
 * @param {string} content 原始内容。
 * @returns {string} Mermaid 源码。
 */
const extractMermaidSource = (content = '') => {
    const sourceText = normalizeLineBreaks(content).trim();
    if (!sourceText) {
        return '';
    }
    const fencedMatch = sourceText.match(/```mermaid\s*\n([\s\S]*?)```/i);
    if (fencedMatch?.[1]) {
        return fencedMatch[1].trim();
    }
    return sourceText;
};

/**
 * @description 解析 Mermaid 文本为节点和边数据。
 * @param {string} source Mermaid 源码。
 * @returns {object} 图结构数据。
 */
const parseMermaidTreeSource = (source = '') => {
    const nodeMap = {};
    const nodeOrderList = [];
    const edgeList = [];
    const highlightSet = new Set();
    const lineList = normalizeLineBreaks(source)
        .split('\n')
        .map(line => line.trim())
        .filter(Boolean);

    lineList.forEach(line => {
        if (isMermaidMetaLine(line)) {
            return;
        }
        const styleMatch = line.match(/^style\s+([A-Za-z0-9_]+)\s+(.+)$/i);
        if (styleMatch) {
            if (/(fill:\s*#?(?:f99|ffb4b4|fecdd3|ffe4e6)|stroke:\s*#?(?:ef4444|fb7185))/i.test(styleMatch[2])) {
                highlightSet.add(styleMatch[1]);
            }
            return;
        }
        const edgeSequence = parseMermaidEdgeSequence(line);
        if (!edgeSequence.length) {
            const standaloneNode = parseStandaloneNode(line);
            if (standaloneNode) {
                mergeNode(nodeMap, nodeOrderList, standaloneNode);
            }
            return;
        }
        edgeSequence.forEach(edge => {
            mergeNode(nodeMap, nodeOrderList, edge.fromNode);
            mergeNode(nodeMap, nodeOrderList, edge.toNode);
            edgeList.push({
                from: edge.fromNode.id,
                to: edge.toNode.id,
                label: edge.label
            });
        });
    });

    return {
        nodeList: nodeOrderList.map(nodeId => ({
            ...nodeMap[nodeId],
            isHighlight: highlightSet.has(nodeId),
            width: nodeMap[nodeId].shape === 'decision' ? FLOW_LAYOUT_CONFIG.decisionWidth : FLOW_LAYOUT_CONFIG.regularWidth,
            height: nodeMap[nodeId].shape === 'decision' ? FLOW_LAYOUT_CONFIG.decisionHeight : FLOW_LAYOUT_CONFIG.regularHeight
        })),
        edgeList
    };
};

/**
 * @description 判断 Mermaid 行是否为结构声明或注释行。
 * @param {string} line Mermaid 单行内容。
 * @returns {boolean} 是否为元数据行。
 */
const isMermaidMetaLine = (line = '') => /^(graph|flowchart|classDef|class|linkStyle|%%)\b/i.test(normalizeText(line));

/**
 * @description 解析 Mermaid 单行边序列。
 * @param {string} line Mermaid 单行内容。
 * @returns {Array<object>} 边列表。
 */
const parseMermaidEdgeSequence = (line = '') => {
    const edgeList = [];
    let cursor = 0;
    let currentNodeResult = readNodeAt(line, cursor);
    if (!currentNodeResult) {
        return edgeList;
    }
    cursor = currentNodeResult.endIndex;
    while (cursor < line.length) {
        const edgeMatch = line.slice(cursor).match(/^\s*-->\s*(?:\|([^|]+)\|\s*)?/);
        if (!edgeMatch) {
            break;
        }
        cursor += edgeMatch[0].length;
        const nextNodeResult = readNodeAt(line, cursor);
        if (!nextNodeResult) {
            break;
        }
        edgeList.push({
            fromNode: currentNodeResult.node,
            toNode: nextNodeResult.node,
            label: normalizeText(edgeMatch[1])
        });
        currentNodeResult = nextNodeResult;
        cursor = nextNodeResult.endIndex;
    }
    return edgeList;
};

/**
 * @description 从文本指定位置读取 Mermaid 节点。
 * @param {string} line Mermaid 单行内容。
 * @param {number} startIndex 开始索引。
 * @returns {object|null} 节点读取结果。
 */
const readNodeAt = (line = '', startIndex = 0) => {
    const restText = line.slice(startIndex);
    const matched = restText.match(/^\s*([A-Za-z0-9_]+)(?:\[([\s\S]*?)\]|\{([\s\S]*?)\})?/);
    if (!matched) {
        return null;
    }
    const rawLabel = matched[2] ?? matched[3] ?? matched[1];
    return {
        node: {
            id: matched[1],
            label: normalizeNodeLabel(rawLabel) || matched[1],
            shape: matched[3] !== undefined ? 'decision' : 'regular'
        },
        endIndex: startIndex + matched[0].length
    };
};

/**
 * @description 解析独立节点。
 * @param {string} line Mermaid 单行内容。
 * @returns {object|null} 节点数据。
 */
const parseStandaloneNode = (line = '') => readNodeAt(normalizeText(line), 0)?.node || null;

/**
 * @description 合并节点并保持首次出现顺序。
 * @param {object} nodeMap 节点映射。
 * @param {Array<string>} nodeOrderList 节点顺序列表。
 * @param {object} node 节点数据。
 */
const mergeNode = (nodeMap = {}, nodeOrderList = [], node = {}) => {
    if (!node?.id) {
        return;
    }
    if (!nodeMap[node.id]) {
        nodeMap[node.id] = {...node};
        nodeOrderList.push(node.id);
        return;
    }
    if (node.shape === 'decision') {
        nodeMap[node.id].shape = 'decision';
    }
    if (node.label && nodeMap[node.id].label === node.id) {
        nodeMap[node.id].label = node.label;
    }
};

/**
 * @description 构建树形布局坐标。
 * @param {object} graphData 图结构数据。
 * @returns {object} 布局结果。
 */
const buildMermaidTreeLayout = (graphData = {}) => {
    const nodeList = graphData.nodeList || [];
    const edgeList = graphData.edgeList || [];
    const nodeMap = nodeList.reduce((result, item, index) => {
        result[item.id] = {
            ...item,
            order: index
        };
        return result;
    }, {});
    const childMap = {};
    const parentMap = {};
    nodeList.forEach(item => {
        childMap[item.id] = [];
    });
    edgeList.forEach(edge => {
        if (!nodeMap[edge.from] || !nodeMap[edge.to]) {
            return;
        }
        if (!parentMap[edge.to]) {
            parentMap[edge.to] = edge.from;
            childMap[edge.from].push(edge.to);
        }
    });
    Object.keys(childMap).forEach(nodeId => {
        childMap[nodeId].sort((prevId, nextId) => nodeMap[prevId].order - nodeMap[nextId].order);
    });

    const rootIdList = nodeList
        .filter(item => !parentMap[item.id])
        .sort((prev, next) => nodeMap[prev.id].order - nodeMap[next.id].order)
        .map(item => item.id);
    const safeRootIdList = rootIdList.length ? rootIdList : (nodeList[0] ? [nodeList[0].id] : []);
    const subtreeWidthMap = {};
    const layoutNodeMap = {};
    const levelStep = FLOW_LAYOUT_CONFIG.levelGap + FLOW_LAYOUT_CONFIG.decisionHeight;

    const getSubtreeWidth = (nodeId = '') => {
        if (subtreeWidthMap[nodeId]) {
            return subtreeWidthMap[nodeId];
        }
        const currentNode = nodeMap[nodeId];
        const childIdList = childMap[nodeId] || [];
        if (!currentNode) {
            return 0;
        }
        if (!childIdList.length) {
            subtreeWidthMap[nodeId] = currentNode.width;
            return currentNode.width;
        }
        const childWidth = childIdList.reduce((sum, childId, index) => {
            return sum + getSubtreeWidth(childId) + (index > 0 ? FLOW_LAYOUT_CONFIG.childGap : 0);
        }, 0);
        subtreeWidthMap[nodeId] = Math.max(currentNode.width, childWidth);
        return subtreeWidthMap[nodeId];
    };
    safeRootIdList.forEach(getSubtreeWidth);

    const placeNode = (nodeId = '', leftX = 0, level = 0) => {
        const currentNode = nodeMap[nodeId];
        if (!currentNode) {
            return;
        }
        const subtreeWidth = subtreeWidthMap[nodeId] || currentNode.width;
        const nodeX = leftX + ((subtreeWidth - currentNode.width) / 2);
        const nodeY = FLOW_LAYOUT_CONFIG.paddingY + (level * levelStep);
        layoutNodeMap[nodeId] = {
            ...currentNode,
            x: nodeX,
            y: nodeY,
            centerX: nodeX + (currentNode.width / 2),
            centerY: nodeY + (currentNode.height / 2)
        };
        const childIdList = childMap[nodeId] || [];
        if (!childIdList.length) {
            return;
        }
        const childWidth = childIdList.reduce((sum, childId, index) => {
            return sum + (subtreeWidthMap[childId] || 0) + (index > 0 ? FLOW_LAYOUT_CONFIG.childGap : 0);
        }, 0);
        let childLeftX = leftX + ((subtreeWidth - childWidth) / 2);
        childIdList.forEach(childId => {
            placeNode(childId, childLeftX, level + 1);
            childLeftX += (subtreeWidthMap[childId] || 0) + FLOW_LAYOUT_CONFIG.childGap;
        });
    };

    let rootLeftX = FLOW_LAYOUT_CONFIG.paddingX;
    safeRootIdList.forEach((rootId, index) => {
        if (index > 0) {
            rootLeftX += FLOW_LAYOUT_CONFIG.rootGap;
        }
        placeNode(rootId, rootLeftX, 0);
        rootLeftX += subtreeWidthMap[rootId] || 0;
    });

    const layoutNodeList = nodeList.map(item => layoutNodeMap[item.id]).filter(Boolean);
    const maxRight = layoutNodeList.reduce((result, item) => Math.max(result, item.x + item.width), FLOW_LAYOUT_CONFIG.canvasMinWidth - FLOW_LAYOUT_CONFIG.paddingX);
    const maxBottom = layoutNodeList.reduce((result, item) => Math.max(result, item.y + item.height), 0);
    return {
        width: Math.max(FLOW_LAYOUT_CONFIG.canvasMinWidth, Math.ceil(maxRight + FLOW_LAYOUT_CONFIG.paddingX)),
        height: Math.max(280, Math.ceil(maxBottom + FLOW_LAYOUT_CONFIG.paddingBottom)),
        nodeList: layoutNodeList,
        nodeMap: layoutNodeMap,
        edgeList
    };
};

/**
 * @description 渲染 Mermaid 树形图 SVG。
 * @param {object} layout 布局结果。
 * @param {string} titleText 标题文案。
 * @param {string} renderId 当前渲染实例 ID。
 * @returns {string} SVG 字符串。
 */
const renderMermaidTreeSvg = (layout = {}, titleText = '', renderId = '') => {
    const markerId = `${renderId}-arrow`;
    const edgeHtml = (layout.edgeList || []).map(edge => renderTreeEdge(edge, layout.nodeMap || {}, markerId)).join('');
    const nodeHtml = (layout.nodeList || []).map(renderTreeNode).join('');
    return `<svg class="aifupan-process-tree-svg" viewBox="0 0 ${layout.width} ${layout.height}" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="${escapeAttr(titleText || '流程图')}">${renderSvgDefs(markerId)}${edgeHtml}${nodeHtml}</svg>`;
};

/**
 * @description 渲染 SVG 定义。
 * @param {string} markerId 箭头标识。
 * @returns {string} defs 字符串。
 */
const renderSvgDefs = (markerId = '') => `<defs><marker id="${markerId}" markerWidth="7" markerHeight="7" refX="6.2" refY="3.5" orient="auto" markerUnits="userSpaceOnUse"><path d="M0 0 L7 3.5 L0 7 Z" fill="#5b88ff"></path></marker><filter id="${markerId}-shadow" x="-20%" y="-20%" width="140%" height="160%"><feDropShadow dx="0" dy="6" stdDeviation="6" flood-color="#1d4ed8" flood-opacity="0.12"></feDropShadow></filter></defs>`;

/**
 * @description 渲染单条连线。
 * @param {object} edge 边数据。
 * @param {object} nodeMap 节点映射。
 * @param {string} markerId 箭头标识。
 * @returns {string} 连线 SVG。
 */
const renderTreeEdge = (edge = {}, nodeMap = {}, markerId = '') => {
    const fromNode = nodeMap[edge.from];
    const toNode = nodeMap[edge.to];
    if (!fromNode || !toNode) {
        return '';
    }
    const startPoint = getNodeBottomAnchor(fromNode);
    const endPoint = getNodeTopAnchor(toNode);
    const middleY = Number((((startPoint.y + endPoint.y) / 2)).toFixed(2));
    const pathText = `M ${startPoint.x} ${startPoint.y} L ${startPoint.x} ${middleY} L ${endPoint.x} ${middleY} L ${endPoint.x} ${endPoint.y}`;
    const labelText = normalizeText(edge.label);
    const labelHtml = labelText ? renderEdgeLabel(startPoint, endPoint, middleY, labelText) : '';
    return `<g><path d="${pathText}" fill="none" stroke="#5b88ff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" marker-end="url(#${markerId})"></path>${labelHtml}</g>`;
};

/**
 * @description 渲染边标签。
 * @param {{x:number,y:number}} startPoint 起点。
 * @param {{x:number,y:number}} endPoint 终点。
 * @param {number} middleY 中间高度。
 * @param {string} labelText 标签文案。
 * @returns {string} 标签 SVG。
 */
const renderEdgeLabel = (startPoint = {}, endPoint = {}, middleY = 0, labelText = '') => {
    const labelWidth = Math.max(48, Math.min(132, estimateTextWidth(labelText, 10) + 20));
    const centerX = Number((((startPoint.x + endPoint.x) / 2)).toFixed(2));
    const labelX = Number((centerX - (labelWidth / 2)).toFixed(2));
    const labelY = Number((middleY - 20).toFixed(2));
    return `<g><rect x="${labelX}" y="${labelY}" width="${labelWidth}" height="22" rx="11" fill="#ffffff" stroke="#dce7ff"></rect><text x="${centerX}" y="${labelY + 14}" text-anchor="middle" font-size="10" font-weight="700" fill="#4b74e6">${escapeHtml(labelText)}</text></g>`;
};

/**
 * @description 渲染单个节点。
 * @param {object} node 节点数据。
 * @returns {string} 节点 SVG。
 */
const renderTreeNode = (node = {}) => {
    const lineList = buildNodeTextLines(node.label, node.shape === 'decision' ? 7 : 10, node.shape === 'decision' ? 3 : 2);
    const baseY = node.centerY - (((lineList.length - 1) * (node.shape === 'decision' ? 15 : 16)) / 2);
    const textHtml = lineList.map((line, index) => {
        const offsetY = baseY + (index * (node.shape === 'decision' ? 15 : 16));
        return `<text x="${node.centerX}" y="${Number(offsetY.toFixed(2))}" text-anchor="middle" font-size="${node.shape === 'decision' ? 13 : 14}" font-weight="700" fill="#1f2a44">${escapeHtml(line)}</text>`;
    }).join('');
    if (node.shape === 'decision') {
        const diamondPath = `M ${node.centerX} ${node.y} L ${node.x + node.width} ${node.centerY} L ${node.centerX} ${node.y + node.height} L ${node.x} ${node.centerY} Z`;
        const fillColor = node.isHighlight ? '#fff3f4' : '#f7faff';
        const strokeColor = node.isHighlight ? '#f39aaa' : '#9bbcff';
        return `<g><path d="${diamondPath}" fill="${fillColor}" stroke="${strokeColor}" stroke-width="2"></path></g><g>${textHtml}</g>`;
    }
    const fillColor = node.isHighlight ? '#fff5f5' : '#f9fbff';
    const strokeColor = node.isHighlight ? '#f39aaa' : '#cfdcff';
    return `<g><rect x="${node.x}" y="${node.y}" width="${node.width}" height="${node.height}" rx="16" fill="${fillColor}" stroke="${strokeColor}" stroke-width="2"></rect></g><g>${textHtml}</g>`;
};

/**
 * @description 获取顶部连接点。
 * @param {object} node 节点数据。
 * @returns {{x:number,y:number}} 顶部连接点。
 */
const getNodeTopAnchor = (node = {}) => ({
    x: node.centerX,
    y: node.shape === 'decision' ? node.y + 4 : node.y
});

/**
 * @description 获取底部连接点。
 * @param {object} node 节点数据。
 * @returns {{x:number,y:number}} 底部连接点。
 */
const getNodeBottomAnchor = (node = {}) => ({
    x: node.centerX,
    y: node.shape === 'decision' ? node.y + node.height - 4 : node.y + node.height
});

/**
 * @description 将节点文案拆分为多行。
 * @param {string} text 节点文案。
 * @param {number} maxUnitsPerLine 每行最大字符权重。
 * @param {number} maxLines 最大行数。
 * @returns {Array<string>} 文案行数组。
 */
const buildNodeTextLines = (text = '', maxUnitsPerLine = 10, maxLines = 2) => {
    const sourceText = normalizeNodeLabel(text);
    if (!sourceText) {
        return [];
    }
    const lineList = [];
    sourceText.split('\n').forEach(segment => {
        let currentLine = '';
        let currentUnits = 0;
        Array.from(segment).forEach(char => {
            const currentUnit = /[a-zA-Z0-9]/.test(char) ? 0.62 : 1;
            if (currentLine && currentUnits + currentUnit > maxUnitsPerLine) {
                lineList.push(currentLine);
                currentLine = char;
                currentUnits = currentUnit;
                return;
            }
            currentLine += char;
            currentUnits += currentUnit;
        });
        if (currentLine) {
            lineList.push(currentLine);
        }
    });
    if (lineList.length <= maxLines) {
        return lineList;
    }
    const slicedList = lineList.slice(0, maxLines);
    slicedList[maxLines - 1] = `${slicedList[maxLines - 1].slice(0, Math.max(1, slicedList[maxLines - 1].length - 1))}…`;
    return slicedList;
};

/**
 * @description 规范化 Mermaid 节点文案。
 * @param {string} text 原始文案。
 * @returns {string} 规范化后的文案。
 */
const normalizeNodeLabel = (text = '') => normalizeText(text)
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/\s*\n\s*/g, '\n');

/**
 * @description 粗略估算文本宽度。
 * @param {string} text 文案内容。
 * @param {number} fontSize 字号。
 * @returns {number} 估算宽度。
 */
const estimateTextWidth = (text = '', fontSize = 12) => Array.from(normalizeText(text))
    .reduce((sum, char) => sum + (/[a-zA-Z0-9]/.test(char) ? fontSize * 0.6 : fontSize), 0);

/**
 * @description 规范化换行。
 * @param {string} text 原始文本。
 * @returns {string} 规范化结果。
 */
const normalizeLineBreaks = (text = '') => String(text || '').replace(/\r\n/g, '\n');

/**
 * @description 规范化普通文本。
 * @param {string} text 原始文本。
 * @returns {string} 规范化结果。
 */
const normalizeText = (text = '') => String(text ?? '').trim();

/**
 * @description 转义 HTML 文本。
 * @param {string} text 原始文本。
 * @returns {string} 转义结果。
 */
const escapeHtml = (text = '') => String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

/**
 * @description 转义 HTML 属性文本。
 * @param {string} text 原始文本。
 * @returns {string} 转义结果。
 */
const escapeAttr = (text = '') => escapeHtml(text).replace(/`/g, '&#96;');

export {
    renderMermaidTreeChart
};

export default renderMermaidTreeChart;

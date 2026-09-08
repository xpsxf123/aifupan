/**
 * 从类名中提取段落ID
 * @param {string} className - HTML元素的类名
 * @returns {string|undefined} 提取出的段落ID
 */
function getClassParagraph(className){
    if(className.indexOf('textParagraph-') >=0){
        return className.split('textParagraph-')?.[1];
    }else if(className.indexOf('char_') >=0){
        return className.split('char_')?.[1]?.split('_')?.[0];
    }else if(className.indexOf('otherMark_') >=0){
        return className.split('otherMark_')?.[1]?.split('_')?.[0];
    }
}

/**
 * 从类名中提取偏移量
 * @param {string} className - HTML元素的类名
 * @returns {string|boolean} 提取出的偏移量或false
 */
function getOffset(className){
    if(className.indexOf('textParagraph-') >=0){
        return false
    }else if(className.indexOf('char_') >=0){
        return className.split('char_')?.[1]?.split('_')?.[1]?.split(' ')?.[0];
    }else if(className.indexOf('otherMark_') >=0){
        return className.split('otherMark_')?.[1]?.split('_')?.[1]?.split(' ')?.[0];
    }
}
function getTextPositionWithoutSpans(event) {
    const target = event.target;
    
    // 如果是span元素，则使用其父元素
    const textContainer = target.tagName === 'SPAN' ? target.parentNode : target;
    
    // 获取容器内的所有文本节点（包括嵌套的）
    const textNodes = [];
    const walker = document.createTreeWalker(
      textContainer,
      NodeFilter.SHOW_TEXT,
      null,
      false
    );
    
    let node;
    while (node = walker.nextNode()) {
        if(node.parentNode?.className?.indexOf('annotation-item-mark')>=0){
           continue;
        }
        textNodes.push(node);
    }
    
    // 计算点击位置前的所有文本长度
    const clickX = event.clientX;
    const clickY = event.clientY;
    
    const range = document.createRange();
    let charCount = 0;
    let found = false;
    
    
    for (const textNode of textNodes) {
      const textLength = textNode.nodeValue.length;
      for (let i = 0; i < textLength; i++) {
        range.setStart(textNode, i);
        range.setEnd(textNode, i + 1);
        const rect = range.getBoundingClientRect();
        charCount++;
        // 检查点击位置是否在字符范围内
        if (
          clickX >= rect.left && 
          clickX <= rect.right &&
          clickY >= rect.top && 
          clickY <= rect.bottom
        ) {
          found = true;
          break;
        }
      }
      
      if (found) break;
    }
    
    return found ? charCount : -1;
  }
  


/**
 * 获取用户当前选中文本的范围信息
 * 处理正常文本和隐藏内容的选择情况
 * @returns {Object|undefined} 包含选择范围信息的对象
 */
export default function (event) {
    const clickIndex = getTextPositionWithoutSpans(event);

    const selection = window.getSelection();
    // 如果没有选择内容或选择已折叠则退出
    if (selection.rangeCount === 0 || selection.isCollapsed) return {clickIndex};

    // 获取所有隐藏内容元素
    const hiddenElements = document.querySelectorAll('.content-text');
    if (hiddenElements.length === 0) return {clickIndex};

    // 遍历当前所有选区范围
    const originalRange = selection.getRangeAt(0);
    // 检查当前范围是否包含隐藏内容
    let containsHidden = false;
    hiddenElements.forEach(el => {
        if (originalRange.intersectsNode(el)) {
            containsHidden = true;
        }
    });

    if (!containsHidden) {
        // 处理不包含隐藏内容的情况
        const {startContainer,endContainer} = originalRange
        let startIndex = getClassParagraph(startContainer?.parentNode?.className);
        let endIndex = getClassParagraph(endContainer?.parentNode?.className);
        let sO = getOffset(startContainer?.parentNode?.className)||originalRange.startOffset;
        let eO = getOffset(endContainer?.parentNode?.className)||originalRange.endOffset;
        return {
            range: originalRange,
            startOffset: Number(sO),
            endOffset: Number(eO),
            startIndex: Number(startIndex),
            endIndex: Number(endIndex),
            clickIndex
        }
    } else {
        // 如果包含隐藏内容，需要拆分选区
        const range = originalRange.cloneRange();
        const documentFragment = range.cloneContents();
        // 创建TreeWalker用于遍历文档片段中的文本节点
        const walker = document.createTreeWalker(
            documentFragment,
            NodeFilter.SHOW_TEXT,
            null,
            false
        );

        let currentNode;
        let textNodes = [];
        // 收集所有文本节点
        while (currentNode = walker.nextNode()) {
            if(currentNode?.parentNode?.className?.indexOf('not-select-text')>=0 || currentNode?.parentNode?.parentNode?.className?.indexOf('not-select-text')>=0){
                continue;
            }
            textNodes.push(currentNode);
        }
        let isHasContentText = false;
        // 过滤隐藏内容的文本节点
        textNodes = textNodes.map(d => {
            if (d?.parentElement?.closest('.content-text')) {
                isHasContentText = true;
                return true
            } else {
                return d.parentNode?.className?.indexOf('annotation-item-mark')>=0 || d
            }
        }).filter(d => d !== true);


        let startIndex = getClassParagraph(range?.startContainer?.parentNode?.className)
        let endIndex = getClassParagraph(range?.endContainer?.parentNode?.className)
        // 如果没有隐藏内容，直接返回范围信息
        if(!isHasContentText){
            return {
                range,
                startOffset: range.startOffset,
                endOffset: range.endOffset,
                startIndex,endIndex,
                clickIndex
            };
        }
        // 处理包含隐藏内容的情况
        let txts = [];
        let nodes = [];
        let isChar = -1;
        // 分析文本节点的类型和内容
        textNodes.forEach((d,index)=>{
            if(d.parentNode.className.indexOf('char_')>=0 || d.parentNode.className.indexOf('mark')>=0){
                txts.push(d.data);
                nodes.push('char_');
                if(isChar === -1){
                    isChar = index;
                }
            }else{
                nodes.push(d.data);
            }
        });
        // 处理特殊字符节点
        if(isChar !== -1){
            let charIndex = nodes.findIndex(d=>d === 'char_');
            nodes = nodes.filter((d,index)=>{
                if(d !== 'char_'){
                    return true
                }else{
                    return charIndex === index;
                }
            }).map(d=>{
                if(d === 'char_'){
                    return txts.join('')
                }else{
                    return d
                }
            });
        }

        // 返回包含文本内容和范围信息的对象
        let rangeData = {
            txt: nodes.join('\n'),
            range,
            startOffset: getOffset(range?.startContainer?.parentNode?.className) || range.startOffset,
            endOffset: getOffset(range?.endContainer?.parentNode?.className) || range.endOffset,
            startIndex,endIndex,
            clickIndex
        }

        return rangeData;
    }

}

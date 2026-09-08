/**
 * @description Word 导出工具：负责清洗 HTML 内容并转换为 docx，避免样式代码、自定义标签残片等脏数据进入正文。
 */
import {
  Document,
  Packer,
  Paragraph,
  TextRun,
  Table,
  TableRow,
  TableCell,
  HeadingLevel,
  AlignmentType,
  WidthType,
  BorderStyle,
  UnderlineType,
  ShadingType
} from 'docx';

/**
 * 使用docx.js将HTML内容转换为Word文档并下载
 * @param {string} html - 要转换的HTML内容
 * @param {string} filename - 文件名，默认为'document'
 * @param {string} extension - 文件扩展名，默认为'.docx'
 * @param {Object} opt - 可选参数
 * @param {boolean} opt.output - 是否返回blob对象而不是直接下载
 * @param {Function} opt.message - 消息提示函数
 * @returns {Promise<Blob|void>} 如果output为true则返回blob对象
 */
export async function exportHtmlToWord(html, filename = 'document', extension = '.docx', opt = {}) {
  const { output, message } = opt;
  try {
    const normalizeTextWithLineBreaks = (rawText = '') => {
      const normalized = String(rawText ?? '')
        .replace(/\r\n/g, '\n')
        .replace(/\r/g, '\n');
      return normalized
        .split('\n')
        .map(line => String(line || '').replace(/[ \t\f\v]+/g, ' ').trim())
        .join('\n')
        .replace(/\n{3,}/g, '\n\n')
        .trim();
    };

    const pushTextRunsWithLineBreaks = (textRuns, runOptions, text, option = {}) => {
      const normalizedText = normalizeTextWithLineBreaks(text);
      if (!normalizedText) return;
      const { appendSpace } = option;
      const parts = normalizedText.split('\n');
      parts.forEach((part, idx) => {
        if (idx > 0) {
          textRuns.push(new TextRun({ break: 1 }));
        }
        if (!part) return;
        textRuns.push(createSafeTextRun({
          ...runOptions,
          text: appendSpace ? `${part} ` : part
        }));
      });
    };

    /**
     * 清理HTML内容，移除不需要在Word中显示的元素
     * @param {string} htmlText - 原始HTML文本
     * @returns {string} 清理后的HTML文本
     */
    function getHtml(htmlText) {
      /**
       * @description 判断一行文本是否属于误导出的 CSS/自定义标签残片。
       * @param {string} lineText - 单行文本内容。
       * @returns {boolean}
       */
      function isArtifactLine(lineText = '') {
        const safeText = String(lineText || '').trim();
        if (!safeText) {
          return false;
        }
        return (
          /\.ai-mdtag-render-root\b|--ai-tag-primary:|--ai-tag-secondary:|--ai-tag-success:|--ai-tag-warning:|--ai-tag-danger:|--ai-tag-text:|--ai-tag-muted:/i.test(safeText)
          || /\.aifupan-(?:card|title|tag|grid|alert|note|table|chart|collapse|statistic)\b/i.test(safeText)
          || /^<\/?aifupan-[^>\s]*/i.test(safeText)
          || /^[.#][^{]+(?:\{|\s*,\s*[.#][^{]+)+(?:[\s\S]*)$/i.test(safeText)
        );
      }

      /**
       * @description 清除被当作普通文本导出的 CSS 行和自定义标签残片。
       * @param {string} sourceText - 原始文本。
       * @returns {string}
       */
      function stripArtifactText(sourceText = '') {
        return String(sourceText || '')
          .replace(/&lt;style[\s\S]*?&lt;\/style&gt;/gi, '')
          .replace(/<\/?aifupan-[^>\s]*/gi, '')
          .split(/\r?\n/)
          .filter(line => !isArtifactLine(line))
          .join('\n')
          .trim();
      }

      /**
       * 对原始 HTML 字符串做一次预清洗，兜底移除无法被 DOM 正常解析的样式串和标签碎片。
       * 某些 AI 报告导出内容会把 style/custom tag 片段当普通文本写入 innerHTML，仅靠 DOM 删除不够。
       * @param {string} sourceHtml 原始 HTML 字符串
       * @returns {string} 预处理后的 HTML 字符串
       */
      function normalizeRawHtml(sourceHtml) {
        return stripArtifactText(String(sourceHtml || '')
          .replace(/<style[\s\S]*?<\/style>/gi, '')
          .replace(/<script[\s\S]*?<\/script>/gi, '')
          .replace(/&lt;script[\s\S]*?&lt;\/script&gt;/gi, '')
          .replace(/<\/?aifupan-[a-z0-9-]+\b[^>]*>/gi, '')
          .replace(/<\/?aifupan-[^\n\r>]*$/gim, ''))
      }

      const divContent = document.createElement('div');
      divContent.innerHTML = normalizeRawHtml(htmlText);

      /**
       * 将自定义标签展开为普通子节点，避免标签壳和附带样式干扰 Word 导出。
       * @param {Element} element 需要展开的元素
       * @returns {void}
       */
      function unwrapElement(element) {
        if (!element || !element.parentNode) {
          return;
        }
        const parent = element.parentNode;
        while (element.firstChild) {
          parent.insertBefore(element.firstChild, element);
        }
        parent.removeChild(element);
      }
      
      // 需要移除的选择器列表，包括CSS中设置为display: none的class
      const selectorsToRemove = [
        '.not-word',
        '.deepThinkingTitle', 
        '.deepThinking',
        '.avatar-img-box',
        '.wordsBodyAvatar',
        '.online-num-img',
        '.onlineNumContainer',
        '.share-title-img'
      ];
      
      // 统一处理所有需要移除的元素
      selectorsToRemove.forEach(selector => {
        const elements = divContent.querySelectorAll(selector);
        elements.forEach(el => {
          // 使用更安全的remove方法，如果不支持则回退到removeChild
          if (el.remove) {
            el.remove();
          } else if (el.parentNode) {
            el.parentNode.removeChild(el);
          }
        });
      });

      // 彻底移除样式、脚本和元信息节点，避免其文本被当作正文写入 Word。
      const tagSelectorsToRemove = ['style', 'script', 'noscript', 'template', 'link', 'meta', 'title'];
      tagSelectorsToRemove.forEach(selector => {
        const elements = divContent.querySelectorAll(selector);
        elements.forEach(el => {
          if (el.remove) {
            el.remove();
          } else if (el.parentNode) {
            el.parentNode.removeChild(el);
          }
        });
      });

      // 将 aifupan 自定义标签展开，只保留内部真实内容。
      const customTagElements = divContent.querySelectorAll('*');
      Array.from(customTagElements).forEach(el => {
        const tagName = el.tagName?.toLowerCase?.() || '';
        if (tagName.startsWith('aifupan-')) {
          unwrapElement(el);
          return;
        }
        // 移除 Vue 作用域属性，减少无关导出内容。
        Array.from(el.attributes || []).forEach(attr => {
          if (/^data-v-[\w-]+$/i.test(attr.name)) {
            el.removeAttribute(attr.name);
          }
        });
      });
      
      // 特殊处理：移除.share-title内的img元素
      const shareImages = divContent.querySelectorAll('.share-title img');
      shareImages.forEach(img => {
        if (img.remove) {
          img.remove();
        } else if (img.parentNode) {
          img.parentNode.removeChild(img);
        }
      });

      // 移除注释节点，避免少数场景下注释文本串入正文。
      const walker = document.createTreeWalker(divContent, NodeFilter.SHOW_COMMENT, null, false);
      const commentNodes = [];
      while (walker.nextNode()) {
        commentNodes.push(walker.currentNode);
      }
      commentNodes.forEach(node => node.parentNode?.removeChild(node));

      // 兜底移除被浏览器解析成普通文本的样式块或自定义标签碎片，避免其混入导出正文。
      const textWalker = document.createTreeWalker(divContent, NodeFilter.SHOW_TEXT, null, false);
      const invalidTextNodes = [];
      while (textWalker.nextNode()) {
        const currentNode = textWalker.currentNode;
        const rawText = String(currentNode?.textContent || '');
        const cleanText = stripArtifactText(rawText);
        if (!cleanText.trim()) {
          invalidTextNodes.push(currentNode);
          continue;
        }
        if (cleanText !== rawText.trim()) {
          currentNode.textContent = cleanText;
        }
      }
      invalidTextNodes.forEach(node => node.parentNode?.removeChild(node));
      
      return divContent.innerHTML;
    }
    
    /**
     * 解析HTML元素为docx元素
     * @param {Element} element - HTML元素
     * @returns {Array} docx元素数组
     */
    function parseHtmlToDocxElements(element) {
      const elements = [];
      const allTextRuns = [];
      
      if (!element || !element.childNodes) {
        return elements;
      }
      
      // 收集所有文本内容到一个数组中
      function collectTextRuns(node) {
        if (node.nodeType === Node.TEXT_NODE) {
          pushTextRunsWithLineBreaks(allTextRuns, {}, node.textContent, { appendSpace: true });
          return true; // 文本节点处理完成
        } else if (node.nodeType === Node.ELEMENT_NODE) {
          const tagName = node.tagName.toLowerCase();
          const className = node.className || '';
          

          // 对于特殊标签，仍然单独处理
          if (['table', 'ul', 'ol'].includes(tagName)) {
            return false; // 返回false表示需要单独处理
          }

          if (tagName === 'br') {
            allTextRuns.push(new TextRun({ break: 1 }));
            return true;
          }
          
          // 处理 h1-h6 标题标签
          if (['h1', 'h2', 'h3', 'h4', 'h5', 'h6'].includes(tagName)) {
            const titleText = normalizeTextWithLineBreaks(node.textContent);
            if (titleText) {
              // 标题前添加换行
              allTextRuns.push(new TextRun({ break: 1 }));
              
              const runOptions = {
                text: titleText,
                bold: true // 加粗
              };
              
              // 根据标题级别设置字体大小
              switch (tagName) {
                case 'h1':
                  runOptions.size = 32; // 16pt = 32 half-points
                  break;
                case 'h2':
                  runOptions.size = 28; // 14pt = 28 half-points
                  break;
                case 'h3':
                  runOptions.size = 26; // 13pt = 26 half-points
                  break;
                case 'h4':
                  runOptions.size = 24; // 12pt = 24 half-points
                  break;
                case 'h5':
                  runOptions.size = 22; // 11pt = 22 half-points
                  break;
                case 'h6':
                  runOptions.size = 20; // 10pt = 20 half-points
                  break;
              }
              
              allTextRuns.push(createSafeTextRun(runOptions));
              // 标题后添加换行
              allTextRuns.push(new TextRun({ break: 1 }));
            }
            return true; // h1-h6标签处理完成
          }

          if (['p', 'div', 'li', 'section', 'article', 'blockquote'].includes(tagName)) {
            for (const child of node.childNodes) {
              const result = collectTextRuns(child);
              if (result === false) {
                break;
              }
            }
            allTextRuns.push(new TextRun({ break: 1 }));
            return true;
          }
          
          // 检查是否有 wordsBodyContentText2 类
          const hasWordsBodyContentText2 = className.includes('wordsBodyContentText2');
          // 检查是否需要换行的特殊class
          const shouldAddLineBreak = className.includes('words-other-info');
          const hasTextParagraphBox = className.includes('text-paragraph-box');
          
          // 如果需要换行，递归处理子节点以检查 wordsBodyContentText2 类
          if (shouldAddLineBreak) {
            // 递归处理 words-other-info 的子节点
            for (const child of node.childNodes) {
              if (child.nodeType === Node.TEXT_NODE) {
                pushTextRunsWithLineBreaks(allTextRuns, {}, child.textContent, { appendSpace: true });
              } else if (child.nodeType === Node.ELEMENT_NODE) {
                const childClassName = child.className || '';
                // 检查子元素是否有 wordsBodyContentText2 类
                if (childClassName.includes('wordsBodyContentText2')) {
                  const childText = normalizeTextWithLineBreaks(child.textContent);
                  if (childText) {
                    const runOptions = {
                      size: 24, // 12pt = 24 half-points
                      color: '95A1AF'
                    };
                    pushTextRunsWithLineBreaks(allTextRuns, runOptions, childText, { appendSpace: true });
                  }
                } else {
                  // 继续递归处理其他子节点
                  collectTextRuns(child);
                }
              }
            }
            allTextRuns.push(new TextRun({ break: 1 })); // 添加换行
            return true; // words-other-info节点处理完成
          } else if (hasTextParagraphBox) {
            // 对于text-paragraph-box class的节点，需要递归处理其子节点
            for (const child of node.childNodes) {
              collectTextRuns(child);
            }
            // 处理完子节点后添加换行
            allTextRuns.push(new TextRun({ break: 1 }));
            return true; // text-paragraph-box节点处理完成
          } else if (['span', 'font', 'u', 's', 'strike', 'del', 'b', 'strong', 'i', 'em'].includes(tagName)) {
            // 处理各种样式标签，应用样式并递归处理子节点
            
            // 递归处理标签的所有子节点
            for (const child of node.childNodes) {
              if (child.nodeType === Node.TEXT_NODE) {
                const childText = normalizeTextWithLineBreaks(child.textContent);
                if (childText) {
                  let runOptions = {};
                  
                  // 检查是否有 wordsBodyContentText2 类，设置字体大小和颜色
                  if (className.includes('wordsBodyContentText2')) {
                    runOptions.size = 24; // 12pt = 24 half-points
                    runOptions.color = '95A1AF';
                  }
                  
                  // 应用样式解析
                  runOptions = parseElementStyles(node, runOptions);
                  
                  // 根据标签类型设置基础样式
                  switch (tagName) {
                    case 'b':
                    case 'strong':
                      runOptions.bold = true;
                      break;
                    case 'i':
                    case 'em':
                      runOptions.italics = true;
                      break;
                    case 'u':
                      runOptions.underline = { type: UnderlineType.SINGLE };
                      break;
                    case 's':
                    case 'strike':
                    case 'del':
                      runOptions.strike = true;
                      break;
                  }
                  
                  pushTextRunsWithLineBreaks(allTextRuns, runOptions, childText, { appendSpace: true });
                }
              } else if (child.nodeType === Node.ELEMENT_NODE) {
                // 递归处理嵌套的元素节点
                collectTextRuns(child);
              }
            }
            return true; // 样式标签处理完成
          } else {
            // 对于其他标签，检查是否有 wordsBodyContentText2 类
            if (hasWordsBodyContentText2) {
              // 处理带有 wordsBodyContentText2 类的元素
              for (const child of node.childNodes) {
                if (child.nodeType === Node.TEXT_NODE) {
                  const childText = normalizeTextWithLineBreaks(child.textContent);
                  if (childText) {
                    const runOptions = {
                      size: 24, // 12pt = 24 half-points
                      color: '95A1AF'
                    };
                    pushTextRunsWithLineBreaks(allTextRuns, runOptions, childText, { appendSpace: true });
                  }
                } else if (child.nodeType === Node.ELEMENT_NODE) {
                  collectTextRuns(child);
                }
              }
              allTextRuns.push(new TextRun({ break: 1 })); // 为 wordsBodyContentText2 类添加换行
            } else {
              // 对于其他标签，递归收集文本
              for (const child of node.childNodes) {
                const result = collectTextRuns(child);
                // 如果子节点返回false，说明需要单独处理，跳出递归
                if (result === false) {
                  break;
                }
              }
            }
            return true; // 其他标签处理完成
          }
        }
        return true; // 默认返回true
      }
      
      for (const node of element.childNodes) {
        if (node.nodeType === Node.ELEMENT_NODE) {
          const tagName = node.tagName.toLowerCase();
          
          switch (tagName) {
            case 'table':
              const table = parseTable(node);
              if (table) {
                elements.push(table);
              }
              break;
              
            case 'ul':
            case 'ol':
              const listItems = parseList(node);
              elements.push(...listItems);
              break;
              
            default:
              // 对于其他所有内容，收集到文本运行中
              collectTextRuns(node);
              break;
          }
        } else if (node.nodeType === Node.TEXT_NODE) {
          collectTextRuns(node);
        }
      }
      
      // 如果有收集到的文本，创建一个段落
      if (allTextRuns.length > 0) {
        elements.unshift(new Paragraph({
          children: allTextRuns,
          alignment: AlignmentType.LEFT
        }));
      }
      
      return elements;
    }
    
    /**
     * 解析HTML元素的样式属性
     * @param {Element} node - HTML元素节点
     * @param {Object} runOptions - TextRun选项对象
     * @returns {Object} 更新后的runOptions
     */
    function parseElementStyles(node, runOptions) {
      const style = node.getAttribute('style') || '';
      const tagName = node.tagName.toLowerCase();
      
      // 定义统一的颜色映射表
      const colorMap = {
        'red': 'FF0000',
        'blue': '0000FF',
        'green': '008000',
        'yellow': 'FFFF00',
        'orange': 'FFA500',
        'purple': '800080',
        'pink': 'FFC0CB',
        'black': '000000',
        'white': 'FFFFFF',
        'gray': '808080',
        'grey': '808080',
        // 扩展更多常见颜色
        'paleturquoise': 'AFEEEE',
        'lightblue': 'ADD8E6',
        'lightgreen': '90EE90',
        'darkgreen': '006400',
        'navy': '000080',
        'maroon': '800000',
        'olive': '808000',
        'lime': '00FF00',
        'aqua': '00FFFF',
        'teal': '008080',
        'silver': 'C0C0C0',
        'fuchsia': 'FF00FF',
        'cyan': '00FFFF',
        'magenta': 'FF00FF',
        'brown': 'A52A2A',
        'gold': 'FFD700',
        'violet': 'EE82EE',
        'indigo': '4B0082',
        'turquoise': '40E0D0'
      };

      /**
       * @description 将颜色值统一转换为 6 位十六进制颜色，供 docx 使用。
       * @param {string} rawColor - 原始颜色值。
       * @param {Object} option - 转换选项。
       * @returns {string} 标准化后的 6 位十六进制颜色。
       */
      const normalizeDocxColor = (rawColor = '', option = {}) => {
        const fallbackColor = option?.fallbackColor || '';
        const colorText = String(rawColor || '')
          .trim()
          .replace(/^['"]|['"]$/g, '')
          .replace(/\s*!important\s*$/i, '');
        if (!colorText) {
          return fallbackColor;
        }

        const rgbMatch = colorText.match(/rgba?\s*\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*[\d.]+\s*)?\)/i);
        if (rgbMatch) {
          const r = Math.max(0, Math.min(255, parseInt(rgbMatch[1], 10)));
          const g = Math.max(0, Math.min(255, parseInt(rgbMatch[2], 10)));
          const b = Math.max(0, Math.min(255, parseInt(rgbMatch[3], 10)));
          return ((r << 16) | (g << 8) | b).toString(16).padStart(6, '0').toUpperCase();
        }

        const hexText = colorText.replace(/^#/, '').toUpperCase();
        if (/^[0-9A-F]{3}$/.test(hexText)) {
          return hexText.split('').map(char => char + char).join('');
        }
        if (/^[0-9A-F]{6}$/.test(hexText)) {
          return hexText;
        }

        const mappedColor = colorMap[colorText.toLowerCase()];
        if (mappedColor) {
          return mappedColor;
        }

        return fallbackColor;
      };
      
      // 解析style属性中的颜色和背景色
      if (style) {
        // 解析文字颜色
        const colorMatch = style.match(/color\s*:\s*([^;]+)/i);
        if (colorMatch) {
          const color = normalizeDocxColor(colorMatch[1], {
            fallbackColor: '000000'
          });
          if (!color && colorMatch[1]) {
            console.warn(`未支持的文字颜色名称: ${colorMatch[1].trim()}，将使用默认黑色`);
          }
          runOptions.color = color;
        }
        
        // 解析背景色
        const backgroundMatch = style.match(/background(?:-color)?\s*:\s*([^;]+)/i);
        if (backgroundMatch) {
          const backgroundColor = normalizeDocxColor(backgroundMatch[1]);
          if (!backgroundColor) {
            console.warn(`未支持的背景色名称: ${backgroundMatch[1].trim()}，将跳过背景色设置`);
          } else {
            // 使用shading设置背景色，支持16进制颜色值
            // 添加color属性以确保在不同版本的Word中都能正确显示
            runOptions.shading = {
              type: 'clear' || ShadingType.SOLID,
              fill: backgroundColor,
              color: 'auto'
            };
          }
        }
        
        // 注释掉颜色冲突检测，保持原有的文字颜色
        // 避免自动调整导致突出颜色丢失
        /*
        if (runOptions.color && runOptions.shading && runOptions.shading.fill) {
          const textColor = runOptions.color.toUpperCase();
          const bgColor = runOptions.shading.fill.toUpperCase();
          
          if (textColor === bgColor) {
            const r = parseInt(bgColor.substring(0, 2), 16);
            const g = parseInt(bgColor.substring(2, 4), 16);
            const b = parseInt(bgColor.substring(4, 6), 16);
            
            const brightness = (r * 0.299 + g * 0.587 + b * 0.114);
            runOptions.color = brightness > 128 ? '000000' : 'FFFFFF';
          }
        }
        */
        
        // 解析字体样式
        if (style.includes('font-weight:bold') || style.includes('font-weight: bold')) {
          runOptions.bold = true;
        }
        if (style.includes('font-style:italic') || style.includes('font-style: italic')) {
          runOptions.italics = true;
        }
        if (style.includes('text-decoration:underline') || style.includes('text-decoration: underline')) {
          runOptions.underline = { type: UnderlineType.SINGLE };
        }
        if (style.includes('text-decoration:line-through') || style.includes('text-decoration: line-through')) {
          runOptions.strike = true;
        }
        
        // 解析字体大小
        const fontSizeMatch = style.match(/font-size\s*:\s*([\d.]+)(px|pt|em)?/i);
        if (fontSizeMatch) {
          let fontSize = parseFloat(fontSizeMatch[1]);
          const unit = fontSizeMatch[2] || 'px';
          
          // 转换为half-points (docx使用half-points作为单位)
          if (unit === 'pt') {
            fontSize = fontSize * 2; // pt转half-points
          } else if (unit === 'px') {
            fontSize = fontSize * 1.5; // px转half-points (近似转换)
          } else if (unit === 'em') {
            fontSize = fontSize * 24; // em转half-points (假设基础字体12pt)
          }
          
          runOptions.size = Math.round(fontSize);
        }
      }
      
      // 处理font标签的特殊属性
      if (tagName === 'font') {
        const color = node.getAttribute('color');
        if (color) {
          const fontColor = normalizeDocxColor(color, {
            fallbackColor: '000000'
          });
          if (!fontColor) {
            console.warn(`未支持的 font 颜色名称: ${color.trim()}，将使用默认黑色`);
          }
          runOptions.color = fontColor;
        }
        
        const face = node.getAttribute('face');
        if (face) {
          runOptions.font = face;
        }
        
        const size = node.getAttribute('size');
        if (size) {
          // HTML font size转换为pt (1=8pt, 2=10pt, 3=12pt, 4=14pt, 5=18pt, 6=24pt, 7=36pt)
          const fontSizeMap = {
            '1': 16, '2': 20, '3': 24, '4': 28, '5': 36, '6': 48, '7': 72
          };
          if (fontSizeMap[size]) {
            runOptions.size = fontSizeMap[size];
          }
        }
      }
      
      // 最终颜色冲突检测（处理所有情况，包括font标签）
      if (runOptions.color && runOptions.shading && runOptions.shading.fill) {
        const textColor = runOptions.color.toUpperCase();
        const bgColor = runOptions.shading.fill.toUpperCase();
        
        // 如果文字颜色和背景色相同，自动调整文字颜色
        if (textColor === bgColor) {
          // 根据背景色的亮度选择合适的文字颜色
          const r = parseInt(bgColor.substring(0, 2), 16);
          const g = parseInt(bgColor.substring(2, 4), 16);
          const b = parseInt(bgColor.substring(4, 6), 16);
          
          // 计算亮度 (使用相对亮度公式)
          const brightness = (r * 0.299 + g * 0.587 + b * 0.114);
          
          // 如果背景较亮，使用黑色文字；如果背景较暗，使用白色文字
          runOptions.color = brightness > 128 ? '000000' : 'FFFFFF';
        }
      }
      
      return runOptions;
    }

    /**
     * @description 统一标准化 docx 文本颜色，若无法识别则回退为黑色或空值。
     * @param {string} rawColor - 原始颜色值。
     * @param {string} fallbackColor - 默认回退颜色。
     * @returns {string} 标准化后的 6 位十六进制颜色。
     */
    function normalizeDocxTextColor(rawColor = '', fallbackColor = '') {
      const colorMap = {
        'red': 'FF0000',
        'blue': '0000FF',
        'green': '008000',
        'yellow': 'FFFF00',
        'orange': 'FFA500',
        'purple': '800080',
        'pink': 'FFC0CB',
        'black': '000000',
        'white': 'FFFFFF',
        'gray': '808080',
        'grey': '808080',
        'paleturquoise': 'AFEEEE',
        'lightblue': 'ADD8E6',
        'lightgreen': '90EE90',
        'darkgreen': '006400',
        'navy': '000080',
        'maroon': '800000',
        'olive': '808000',
        'lime': '00FF00',
        'aqua': '00FFFF',
        'teal': '008080',
        'silver': 'C0C0C0',
        'fuchsia': 'FF00FF',
        'cyan': '00FFFF',
        'magenta': 'FF00FF',
        'brown': 'A52A2A',
        'gold': 'FFD700',
        'violet': 'EE82EE',
        'indigo': '4B0082',
        'turquoise': '40E0D0'
      };
      const colorText = String(rawColor || '')
        .trim()
        .replace(/^['"]|['"]$/g, '')
        .replace(/\s*!important\s*$/i, '');
      if (!colorText) {
        return fallbackColor;
      }
      const rgbMatch = colorText.match(/rgba?\s*\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*[\d.]+\s*)?\)/i);
      if (rgbMatch) {
        const r = Math.max(0, Math.min(255, parseInt(rgbMatch[1], 10)));
        const g = Math.max(0, Math.min(255, parseInt(rgbMatch[2], 10)));
        const b = Math.max(0, Math.min(255, parseInt(rgbMatch[3], 10)));
        return ((r << 16) | (g << 8) | b).toString(16).padStart(6, '0').toUpperCase();
      }
      const hexText = colorText.replace(/^#/, '').toUpperCase();
      if (/^[0-9A-F]{3}$/.test(hexText)) {
        return hexText.split('').map(char => char + char).join('');
      }
      if (/^[0-9A-F]{6}$/.test(hexText)) {
        return hexText;
      }
      return colorMap[colorText.toLowerCase()] || fallbackColor;
    }

    /**
     * @description 在创建 TextRun 前兜底清洗颜色相关字段，避免非法颜色导致导出中断。
     * @param {Object|string} input - TextRun 入参。
     * @returns {TextRun} 安全的 TextRun 实例。
     */
    function createSafeTextRun(input) {
      if (!input || typeof input !== 'object' || Array.isArray(input)) {
        return new TextRun(input);
      }
      const safeInput = {...input};
      if (Object.prototype.hasOwnProperty.call(safeInput, 'color')) {
        safeInput.color = normalizeDocxTextColor(safeInput.color, '000000') || '000000';
      }
      if (safeInput.shading && typeof safeInput.shading === 'object') {
        const fillColor = normalizeDocxTextColor(safeInput.shading.fill);
        if (fillColor) {
          safeInput.shading = {
            ...safeInput.shading,
            fill: fillColor
          };
          if (safeInput.shading.color && safeInput.shading.color !== 'auto') {
            safeInput.shading.color = normalizeDocxTextColor(safeInput.shading.color, '000000') || '000000';
          }
        } else {
          delete safeInput.shading;
        }
      }
      if (safeInput.underline && typeof safeInput.underline === 'object' && safeInput.underline.color) {
        safeInput.underline = {
          ...safeInput.underline,
          color: normalizeDocxTextColor(safeInput.underline.color, '000000') || '000000'
        };
      }
      try {
        return new TextRun(safeInput);
      } catch (error) {
        const fallbackInput = {...safeInput};
        fallbackInput.color = '000000';
        delete fallbackInput.shading;
        if (fallbackInput.underline && typeof fallbackInput.underline === 'object' && fallbackInput.underline.color) {
          delete fallbackInput.underline.color;
        }
        return new TextRun(fallbackInput);
      }
    }
    
    /**
     * 解析文本运行元素
     * @param {Element} element - HTML元素
     * @returns {Array} TextRun数组
     */
    function parseTextRuns(element) {
      const textRuns = [];
      
      for (const node of element.childNodes) {
        if (node.nodeType === Node.TEXT_NODE) {
          pushTextRunsWithLineBreaks(textRuns, {}, node.textContent);
        } else if (node.nodeType === Node.ELEMENT_NODE) {
          const tagName = node.tagName.toLowerCase();
          const nodeClassName = node.className || '';
          const rawText = node.textContent;
          const normalizedText = normalizeTextWithLineBreaks(rawText);
          
          if (!normalizedText) continue;
          
          let runOptions = {};
          
          // 检查是否有 wordsBodyContentText2 类，设置字体大小和颜色
          if (nodeClassName.includes('wordsBodyContentText2')) {
            runOptions.size = 24; // 12pt = 24 half-points
            runOptions.color = '95A1AF';
          }
          
          // 应用通用样式解析
          runOptions = parseElementStyles(node, runOptions);
          
          switch (tagName) {
            case 'strong':
            case 'b':
              runOptions.bold = true;
              break;
            case 'em':
            case 'i':
              runOptions.italics = true;
              break;
            case 'u':
              runOptions.underline = { type: UnderlineType.SINGLE };
              break;
            case 's':
            case 'strike':
            case 'del':
              runOptions.strike = true;
              break;
            case 'code':
              runOptions.font = 'Courier New';
              break;
            case 'font':
              // font标签的样式已在parseElementStyles中处理
              break;
            case 'span':
              // 检查是否有 wordsBodyContentText2 类，设置字体大小和颜色
              const spanClassName = node.className || '';
              if (spanClassName.includes('wordsBodyContentText2')) {
                runOptions.size = 24; // 12pt = 24 half-points
                runOptions.color = '95A1AF';
              }
              // span标签的其他样式已在parseElementStyles中处理
              break;
            case 'h1':
            case 'h2':
            case 'h3':
            case 'h4':
            case 'h5':
            case 'h6':
              // 标题前添加换行
              textRuns.push(new TextRun({ break: 1 }));
              
              runOptions.bold = true; // 加粗
              // 根据标题级别设置字体大小
              switch (tagName) {
                case 'h1':
                  runOptions.size = 32; // 16pt = 32 half-points
                  break;
                case 'h2':
                  runOptions.size = 28; // 14pt = 28 half-points
                  break;
                case 'h3':
                  runOptions.size = 26; // 13pt = 26 half-points
                  break;
                case 'h4':
                  runOptions.size = 24; // 12pt = 24 half-points
                  break;
                case 'h5':
                  runOptions.size = 22; // 11pt = 22 half-points
                  break;
                case 'h6':
                  runOptions.size = 20; // 10pt = 20 half-points
                  break;
              }
              break;
            case 'p':
              // p标签前添加换行
              textRuns.push(new TextRun({ break: 1 }));
              break;
            case 'div':
              // div标签前添加换行
              textRuns.push(new TextRun({ break: 1 }));
              break;
            case 'br':
              textRuns.push(new TextRun({ break: 1 }));
              continue;
          }
          
          pushTextRunsWithLineBreaks(textRuns, runOptions, normalizedText);
          
          // 为 h1-h6 标签添加后面的换行
          if (['h1', 'h2', 'h3', 'h4', 'h5', 'h6'].includes(tagName)) {
            textRuns.push(new TextRun({ break: 1 }));
          }
          
          // 为 p 标签添加后面的换行
          if (tagName === 'p') {
            textRuns.push(new TextRun({ break: 1 }));
          }
          
          // 为 div 标签添加后面的换行
          if (tagName === 'div') {
            textRuns.push(new TextRun({ break: 1 }));
          }
          
          // 为 wordsBodyContentText2 类添加换行
          if (nodeClassName.includes('wordsBodyContentText2')) {
            textRuns.push(new TextRun({ break: 1 }));
          }
        }
      }
      
      return textRuns;
    }
    
    /**
     * 解析表格
     * @param {Element} tableElement - 表格HTML元素
     * @returns {Table|null} docx表格对象
     */
    function parseTable(tableElement) {
      const rows = [];
      const tableRows = tableElement.querySelectorAll('tr');
      
      if (tableRows.length === 0) return null;
      
      tableRows.forEach(tr => {
        const cells = [];
        const tableCells = tr.querySelectorAll('td, th');
        
        tableCells.forEach(cell => {
          const textRuns = parseTextRuns(cell);
          let cellText = cell.textContent || '';
          cellText = normalizeTextWithLineBreaks(cellText);
          
          let cellChildren;
          if (textRuns.length > 0) {
            cellChildren = textRuns;
          } else {
            const plainRuns = [];
            pushTextRunsWithLineBreaks(plainRuns, {}, cellText);
            cellChildren = plainRuns.length ? plainRuns : [new TextRun(cellText)];
          }
          
          cells.push(new TableCell({
            children: [new Paragraph({
              children: cellChildren,
              alignment: AlignmentType.LEFT
            })],
            width: {
              size: 2000,
              type: WidthType.DXA
            }
          }));
        });
        
        if (cells.length > 0) {
          rows.push(new TableRow({ children: cells }));
        }
      });
      
      if (rows.length === 0) return null;
      
      return new Table({
        rows,
        width: {
          size: 100,
          type: WidthType.PERCENTAGE
        }
      });
    }
    
    /**
     * 解析列表
     * @param {Element} listElement - 列表HTML元素
     * @returns {Array} 段落数组
     */
    function parseList(listElement) {
      const paragraphs = [];
      const listItems = listElement.querySelectorAll('li');
      
      listItems.forEach((li, index) => {
        const textRuns = parseTextRuns(li);
        const isOrdered = listElement.tagName.toLowerCase() === 'ol';
        const prefix = isOrdered ? `${index + 1}. ` : '• ';
        
        const children = [new TextRun(prefix)];
        if (textRuns.length > 0) {
          children.push(...textRuns);
        } else {
          let listText = li.textContent || '';
          // 处理列表项中的换行符：将换行符替换为空格，确保文字连续显示
          listText = listText.replace(/\s+/g, ' ').trim();
          children.push(new TextRun(listText));
        }
        
        paragraphs.push(new Paragraph({
          children,
          alignment: AlignmentType.LEFT
        }));
      });
      
      return paragraphs;
    }
    
    // 清理HTML内容
    const cleanHtml = getHtml(html);
    
    // 创建临时DOM元素来解析HTML
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = cleanHtml;
    
    // 解析HTML为docx元素
    const docxElements = parseHtmlToDocxElements(tempDiv);
    
    // 如果没有解析到任何元素，添加一个空段落
    if (docxElements.length === 0) {
      docxElements.push(new Paragraph({
        children: [new TextRun('文档内容为空')],
        alignment: AlignmentType.LEFT
      }));
    }
    
    // 创建Word文档
    const doc = new Document({
      sections: [{
        properties: {},
        children: docxElements
      }]
    });
    
    // 生成文档buffer
    const buffer = await Packer.toBuffer(doc);
    
    // 创建Blob对象
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    });
    
    if (output) {
      return blob;
    }
    
    // 创建下载链接
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename}${extension}`;
    document.body.appendChild(a);
    a.click();
    
    // 清理
    setTimeout(() => {
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      message && message({
        message: '下载中，内容过多需要等待，请勿重复点击，请稍后...',
        type: 'success'
      });
    }, 100);
    
  } catch (error) {
    console.error('导出Word文档失败:', error);
    if (message) {
      message({
        message: '导出Word文档失败，请重试',
        type: 'error'
      });
    }
    throw error;
  }
}


export default { exportHtmlToWord };

/**
 * 文件功能：封装原生浏览器打印为PDF的服务
 * 依赖 src/utils/downloadHtml.js 生成完整HTML（含样式），在新窗口或隐藏iframe中渲染并调用原生打印
 */
import downloadHtml from '@/utils/downloadHtml';
export default class PrintService {
  /**
   * 构造函数
   * @returns {void}
   */
  constructor() {
    this.printWindow = null;
  }

  /**
   * 打印指定元素为PDF（依赖浏览器原生打印，用户选择“保存为PDF”）
   * @param {string|HTMLElement} selector 选择器或元素或HTML字符串
   * @param {Object} options 配置项
   * @param {string} [options.title='打印文档'] 文档标题（用于窗口标题）
   * @param {string} [options.pageSize='A4'] 页面尺寸（如：A4、Letter）
   * @param {string} [options.margin='0.75in'] 页面边距（保留接口，不在此处注入样式，downloadHtml负责样式）
   * @returns {void}
   * @throws {Error} 当未找到打印元素且传入不是HTML字符串时抛出异常
   */
  printElement(selector, options = {}) {
    const {
      title = '打印文档',
      pageSize = 'A4',
      margin = '0.75in'
    } = options;

    const element = typeof selector === 'string'
      ? document.querySelector(selector)
      : selector;
    if (!element) {
      // 若传入的是HTML字符串，走HTML直接打印
      if (typeof selector === 'string' && selector.indexOf('<') >= 0) {
        return this.printHtml(selector, { title, pageSize, margin });
      }
      throw new Error('打印元素未找到');
    }

    // A4宽度约 794px (8.27in * 96dpi)
    const a4WidthPx = 794;
    const heightParam = element?.scrollHeight && Number.isFinite(element.scrollHeight)
      ? Math.min(element.scrollHeight, 2000)
      : 600;
    this.printWindow = window.open('', '_blank', `width=${a4WidthPx},height=${heightParam}`);
    if (!this.printWindow) {
      // 弹窗被拦截，使用隐藏iframe回退
      return this.printWithIframe(element, { title, pageSize, margin });
    }

    const html = downloadHtml(element.outerHTML, { pageSize, margin });

    this.printWindow.document.write(html);
    this.printWindow.document.close();
    this.printWindow.onload = () => {
      try {
        this.printWindow.focus();
        this.printWindow.print();
      } catch (e) {
        console.error('打印调用失败:', e);
      }
    };
  }

  /**
   * 使用隐藏iframe打印（回退方案）
   * @param {HTMLElement} element 打印元素
   * @param {Object} options 配置项
   * @returns {void}
   */
  printWithIframe(element, options) {
    const { title = '打印文档', pageSize = 'A4', margin = '0.75in' } = options || {};
    const iframe = document.createElement('iframe');
    iframe.style.position = 'fixed';
    iframe.style.left = '-99999px';
    iframe.style.top = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.visibility = 'hidden';
    document.body.appendChild(iframe);
    this._iframeNode = iframe;

    const html = downloadHtml(element.outerHTML, { pageSize, margin });

    const doc = iframe.contentDocument || iframe.contentWindow.document;
    doc.open();
    doc.write(html);
    doc.close();
    iframe.onload = () => {
      try {
        iframe.contentWindow.focus();
        iframe.contentWindow.print();
      } catch (e) {
        console.error('iframe打印失败:', e);
      }
    };
  }

  /**
   * 直接打印HTML字符串（不依赖原文档元素）
   * @param {string} htmlString HTML字符串
   * @param {Object} options 配置项
   * @returns {void}
   */
  printHtml(htmlString, options) {
    const { title = '打印文档', pageSize = 'A4', margin = '0.75in' } = options || {};
    const iframe = document.createElement('iframe');
    iframe.style.position = 'fixed';
    iframe.style.left = '-99999px';
    iframe.style.top = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.visibility = 'hidden';
    document.body.appendChild(iframe);
    this._iframeNode = iframe;
    const doc = iframe.contentDocument || iframe.contentWindow.document;
    const html = downloadHtml(htmlString, { pageSize, margin });
    doc.open();
    doc.write(html);
    doc.close();
    iframe.onload = () => {
      try {
        iframe.contentWindow.focus();
        iframe.contentWindow.print();
      } catch (e) {
        console.error('printHtml 调用失败:', e);
      }
    };
  }

  /**
   * 生成可打印的HTML字符串
   * @param {string|HTMLElement} selectorOrHtml
   * @returns {string}
   */
  getPrintableHtml(selectorOrHtml, options = {}) {
    const { pageSize = 'A4', margin = '0.75in' } = options || {};
    const el = typeof selectorOrHtml === 'string'
      ? document.querySelector(selectorOrHtml)
      : selectorOrHtml;
    if (el && el.outerHTML) {
      return downloadHtml(el.outerHTML, { pageSize, margin });
    }
    if (typeof selectorOrHtml === 'string') {
      return downloadHtml(selectorOrHtml, { pageSize, margin });
    }
    throw new Error('无法生成HTML字符串');
  }

  /**
   * 关闭打印窗口
   * @returns {void}
   */
  close() {
    if (this.printWindow) {
      try {
        this.printWindow.close();
      } catch (_) {}
    }
    if (this._iframeNode && this._iframeNode.parentNode) {
      try {
        this._iframeNode.parentNode.removeChild(this._iframeNode);
      } catch (_) {}
      this._iframeNode = null;
    }
  }
}

import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import createPdf from '@/utils/createPdf';
import PrintService from '@/utils/printService';
import { buildChartSlicesScript } from '@/components/analysis/ai/common/script/chartSlices';
export default {
    methods: {
        async copyCanvasToClipboard(canvas, imageType = 'image/png') {
            try {
                // 检查API可用性
                if (!navigator.clipboard || !navigator.clipboard.write) {
                    throw new Error('复制API不可用');
                }

                // 检查canvas有效性
                if (!canvas || !(canvas instanceof HTMLCanvasElement)) {
                    throw new Error('canvas无效');
                }

                // 创建Blob
                const blob = await new Promise((resolve, reject) => {
                    canvas.toBlob((blob) => {
                        if (!blob) {
                            reject(new Error('blob转换失败'));
                            return;
                        }
                        resolve(blob);
                    }, imageType);
                });

                // 写入剪贴板
                await navigator.clipboard.write([
                    new ClipboardItem({
                        [blob.type]: blob
                    })
                ]);
                this.$message.success("已复制图片到剪贴板");
                return true;
            } catch (error) {
                // 回退方案：使用execCommand作为备选
                try {
                    canvas.toBlob((blob) => {
                        const item = new ClipboardItem({ [blob.type]: blob });
                        navigator.clipboard.write([item]);
                    }, imageType);
                    this.$message.success("已复制图片到剪贴板");
                    return true;
                } catch (fallbackError) {
                    console.error('复制Canvas图片失败:', err);
                    this.$message.error("复制图片失败，请重新尝试，或更换其他分享方式");
                    return false;
                }
            }
        },
        async copyCanvasToClipboard1(canvas, imageType = 'image/png') {
            try {
                // 检查Clipboard API是否可用
                if (!navigator.clipboard || !navigator.clipboard.write) {
                    throw new Error('Clipboard API不可用');
                }

                // 将Canvas转换为Blob
                const blob = await new Promise((resolve) => {
                    canvas.toBlob(resolve, imageType);
                });

                // 创建ClipboardItem并写入剪贴板
                await navigator.clipboard.write([
                    new ClipboardItem({
                        [imageType]: blob
                    })
                ]);
                this.$message.success("已复制图片到剪贴板");
                return true;
            } catch (err) {
                console.error('复制Canvas图片失败:', err);
                this.$message.error("复制图片失败，请重新尝试，或更换其他分享方式");
            }
        },
        // 预处理函数
        preprocessForHtml2canvas(element) {
            const walker = document.createTreeWalker(
                element,
                NodeFilter.SHOW_TEXT,
                null,
                false
              );
              let node;
              const textNodes = [];
              while (node = walker.nextNode()) {
                if (node.nodeValue.trim() !== '') {
                    textNodes.push(node);
                }
              }
              textNodes.forEach(textNode => {
                let el = textNode.parentNode;
                if(el?.style?.backgroundColor){
                    el.innerHTML = textNode?.data?.split('')?.map(d=>{
                        return `<${el.localName} style="${el.style.cssText};padding-right:1px;padding-bottom:3px;margin-left:-1px;margin-top:-1px;">${d}</${el.localName}>`
                    })?.join('');
                    el.style.backgroundColor = 'transparent';
                    el.style.letterSpacing = '0';
                    el.style.display = 'inline';
                    el.style.margin = '0';
                    el.style.padding = '0';
                }
            });
            return element;
        },
        sanitizeGradientStylesForCanvas(clonedDoc){
            if(!clonedDoc?.querySelectorAll){return;}
            const allNodes = clonedDoc.querySelectorAll('*');
            allNodes.forEach(node => {
                const view = clonedDoc.defaultView || window;
                const style = view?.getComputedStyle?.(node);
                if(!style){return;}
                const backgroundClip = style.backgroundClip || style.webkitBackgroundClip || '';
                const textColor = style.color || '';
                const webkitTextFillColor = style.webkitTextFillColor || style.getPropertyValue?.('-webkit-text-fill-color') || '';
                const isTransparentColor = (v) => {
                    if(!v){return false;}
                    if(v === 'transparent'){return true;}
                    const m = String(v).match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([0-9.]+))?\)/i);
                    if(!m){return false;}
                    if(typeof m[4] === 'undefined'){return false;}
                    return Number(m[4]) === 0;
                };
                const isTransparentText = backgroundClip === 'text'
                    || isTransparentColor(textColor)
                    || webkitTextFillColor === 'transparent'
                    || isTransparentColor(webkitTextFillColor);
                if(isTransparentText){
                    node.style.backgroundImage = 'none';
                    node.style.background = 'none';
                    node.style.webkitTextFillColor = '';
                    node.style.color = isTransparentColor(textColor) ? '#1d4ed8' : textColor || '#1d4ed8';
                }
            });
            const progressBars = clonedDoc.querySelectorAll('.aifupan-progress-bar,.aifupan-meter-bar');
            progressBars.forEach(node => {
                const className = String(node.className || '');
                if(className.indexOf('warning') >= 0){
                    node.style.background = '#f59e0b';
                    return;
                }
                if(className.indexOf('error') >= 0 || className.indexOf('exception') >= 0){
                    node.style.background = '#ef4444';
                    return;
                }
                if(className.indexOf('success') >= 0){
                    node.style.background = '#22c55e';
                    return;
                }
                node.style.background = '#4f46e5';
            });
        },
        createExportPdfHtml(htmlStr){
            const html = htmlStr || '';
            const injectScript = buildChartSlicesScript();
            let lower = html.toLowerCase();
            let idxBody = lower.lastIndexOf('</body>');
            let idxHtml = lower.lastIndexOf('</html>');
            let nextHtml;
            if (idxBody !== -1) {
                nextHtml = html.slice(0, idxBody) + injectScript + html.slice(idxBody);
            } else if (idxHtml !== -1) {
                nextHtml = html.slice(0, idxHtml) + injectScript + html.slice(idxHtml);
            } else {
                nextHtml = html + injectScript;
            }
            return nextHtml;
        },
        async createImg(el, type, opt = {}) {
            const { fileName, output = false, html, params, typeName} = opt;

            // htmlPDF拦截：走原生打印逻辑
            if (type === 'htmlPDF') {
                try {
                    const svc = new PrintService();
                    if(this.$isAifupan){
                        const pageSize = opt?.htmlPDFOptions?.pageSize || 'A4';
                        // 统一与 downloadHtml 的默认打印边距，避免预览宽度与实际打印内容宽度不一致。
                        const margin = opt?.htmlPDFOptions?.margin || '12mm 10mm 14mm';
                        const htmlStr = svc.getPrintableHtml(el, { pageSize, margin });
                        // 加入客户端调用打印机能力(封装好得能力,直接用打印机生成pdf.无感生成,效果最好)
                        const exportHtml = this.createExportPdfHtml(htmlStr);
                        this.$httpClient.system.htmlPrintPDF({
                            htmlContent:exportHtml,
                            fileName: `${fileName}.pdf` || '打印文档.pdf',
                            uploadType:params?.uploadType,
                            ...params?.otherObj
                        });
                    }else{
                        const title = fileName ? String(fileName) : '打印文档';
                        const pageSize = opt?.htmlPDFOptions?.pageSize || 'A4';
                        // 统一与 downloadHtml 的默认打印边距，避免预览宽度与实际打印内容宽度不一致。
                        const margin = opt?.htmlPDFOptions?.margin || '12mm 10mm 14mm';
                        const targetEl = typeof el === 'function' ? el() : el;
                        svc.printElement(targetEl, {
                            title,
                            styles: true,
                            pageSize,
                            margin
                        });
                    }
                } catch (e) {
                    this.$message?.error('原生PDF打印失败');
                    console.error('htmlPDF打印失败:', e);
                    return;
                }
            }

            let element = el;
            let imgW = Math.ceil(element?.scrollWidth || element?.offsetWidth || 794);
            let scale = 2.5;
            /*
                pdf小于等于8页的情况将会使用2.5倍，超过则1.5，如果在1.5还有超过的情况使用下面的分辨率
                浏览器下大概43页到35页为1倍分辨率，小于35页为1.5倍分辨率.超过43页则报错。
                客户端下19页一下1.5倍分辨率，21页以上29页以下1倍分辨率。超过29页则报错。
            */
            if(el.scrollHeight > 8700){
                scale = 1.5;
            };
            if(type === 'pdf' || type === 'download'){
                if(element.scrollHeight > 64000){
                    this.$message.error(`导出${type === 'pdf' ? 'pdf页数':'图片长度'}超出系统限制，无法生成！可导出txt或word文档。`);
                    return
                }
                if(element.scrollHeight > 51000 ){
                    scale = 1;
                }
            }
            // 1.5倍下大概3.1w。
            if(this.$isAifupan && type === 'pdf'){
                if(element.scrollHeight > 31000){
                    this.$message.error('客户端导出页数超出系统限制（29页），无法生成！');
                    return;
                }
                if(element.scrollHeight * scale>31000){
                    scale = 1;
                }
            }
            this.$message('正在生成中,请稍后...');
            try {
                if (document?.fonts?.ready) {
                    await document.fonts.ready;
                }
            } catch (_) {}
            const bg = window.getComputedStyle?.(element)?.backgroundColor;
            const backgroundColor = bg && bg !== 'rgba(0, 0, 0, 0)' ? bg : '#ffffff';
            return html2canvas(element, {
                width: imgW,
                scale, // 缩放比例，提高分辨率
                logging: false, // 关闭日志
                useCORS: true, // 允许跨域图片
                allowTaint: true, // 允许污染画布
                windowWidth: imgW, // 设置窗口宽度
                windowHeight: element.scrollHeight, // 设置窗口高度
                backgroundColor,
                letterRendering: true,
                onclone:(clonedDoc)=>{
                    const view = clonedDoc.defaultView || window;
                    // 在克隆的文档上修改样式
                    const notesBox = clonedDoc.querySelectorAll('.notes-export-html-box')?.[0];
                    if(notesBox){
                        this.preprocessForHtml2canvas(notesBox);
                    }
                    this.sanitizeGradientStylesForCanvas(clonedDoc);
                    const tables = clonedDoc.querySelectorAll('table');
                    tables.forEach(table => {
                        table.style.overflow = 'visible';
                        const thead = table.querySelector('thead');
                        if (thead) {
                            const theadStyle = view?.getComputedStyle?.(thead);
                            if (theadStyle?.position === 'sticky') {
                                thead.style.position = 'static';
                            }
                            thead.style.position = 'static';
                            thead.style.display = 'table-header-group';
                        }
                         const ths = table.querySelectorAll('th');
                         ths.forEach(th => {
                             const thStyle = view?.getComputedStyle?.(th);
                             if (thStyle?.position === 'sticky') {
                                 th.style.position = 'static';
                             }
                             th.style.position = 'static';
                         });
                    });
                    const stickyEls = clonedDoc.querySelectorAll('[style*="sticky"]');
                    stickyEls.forEach(el => {
                        el.style.position = 'static';
                    });
                    const hiddenEls = clonedDoc.querySelectorAll('.overflow_hidden,[style*="overflow: hidden"]');
                    hiddenEls.forEach(el => {
                        el.style.overflow = 'visible';
                    });
                }
            }).then(async canvas => {
                if (type === 'download' || type === 'pdf') {
                    let name = fileName + (typeName ?? '分享');
                    // 将canvas转换为图片
                    const image = canvas.toDataURL('image/png');
                    if (type === 'pdf') {
                        return this.createPDF({ canvas, w: imgW, h: element.scrollHeight, image }, name, output, opt);
                    } else {
                        this.downloadFile(image, name);
                    }
                } else if (type === 'copy') {
                    await this.copyCanvasToClipboard(canvas)
                }
                // 将canvas转换为图片
                const image = canvas.toDataURL('image/png');
                return {
                    image,
                    canvas,
                    w: imgW,
                    h: element.scrollHeight
                };
            });
        },
        frontUpload(file, name, opt = {}) {
            const { notFolder, callback, otherObj, generationType, uploadType, finallyFn, extension = '.pdf' } = opt;
            let formData = new FormData();
            formData.append("file", file, `${name}${extension}`);
            formData.append("uploadType", uploadType);//1诊断报告-pdf
            formData.append("otherObj", JSON.stringify({
                notFolder,
                ...otherObj
            }));
            formData.append("generationType", generationType);
            return this.$httpClient.uploadFile.frontUpload(formData).then(res => {
                if (res.code === 0 && notFolder === 0) {
                    this.$message.success('文件下载成功');
                }
                callback && callback(res);
                return res.code === 0;
            }).catch(err => {
                return false;
            }).finally(() => {
                finallyFn && finallyFn();
            })
        },
        base64ToBlob(base64Data, contentType = '', sliceSize = 512) {
            // 去除Base64前缀（如"data:image/png;base64,"）
            const byteCharacters = atob(base64Data.split(',')[1]);
            const byteArrays = [];
            for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
              const slice = byteCharacters.slice(offset, offset + sliceSize);
          
              const byteNumbers = new Array(slice.length);
              for (let i = 0; i < slice.length; i++) {
                byteNumbers[i] = slice.charCodeAt(i);
              }
          
              const byteArray = new Uint8Array(byteNumbers);
              byteArrays.push(byteArray);
            }
            return new Blob(byteArrays, { type: contentType });
        },
        async createPDF(data, pdfName, output, opt = {}) {
            const pdf = new jsPDF('p', 'mm', 'a4');
            const { canvas, image, w, h } = data;
                // 使用示例
            let imgs = await createPdf(pdf, canvas, { image, w, h });
            if (this.$isAifupan) {
                this.frontUpload(pdf.output('blob'), pdfName, { notFolder: 0, uploadType: 0, generationType: 1, ...opt.params });
            } else {
                pdf.save(pdfName + '.pdf');
            }
        },
        // 通过图片生成pdf
        multiImgToGeneratePDF(files, pdfName, opt = {}){
            const { notFolder, callback, otherObj, generationType, uploadType, finallyFn, extension = '.pdf' } = opt;
            let formData = new FormData();
            files?.forEach((file,index)=>{
                formData.append("file", file, `${index + 1}.png`);
            })
            formData.append("fileName", `${pdfName}${extension}`);
            formData.append("uploadType", uploadType);//1诊断报告-pdf
            formData.append("otherObj", JSON.stringify({
                notFolder,
                ...otherObj
            }));
            formData.append("generationType", generationType);
            this.$httpClient?.uploadFile?.multiImgToGeneratePDF(formData).then(res => {
                if (res.code === 0 && notFolder === 0) {
                    this.$message.success('文件下载成功');
                }
                callback && callback(res);
                return res.code === 0;
            }).catch(err => {
                return false;
            }).finally(() => {
                finallyFn && finallyFn();
            });
        },
        async downloadFile(url, filename, opt = {}) {
            try {
                if (this.$isAifupan) {
                    // 1. 使用fetch获取文件
                    const response = await fetch(url);
                    const blob = await response.blob();
                    this.frontUpload(blob, filename, { notFolder: 0, uploadType: 0, generationType: 1, extension: '.png', ...opt.params });
                } else {
                    // 1. 使用fetch获取文件
                    const response = await fetch(url);
                    const blob = await response.blob();

                    // 2. 创建下载链接
                    const a = document.createElement('a');
                    const blobUrl = URL.createObjectURL(blob);

                    a.href = blobUrl;
                    a.download = `${filename}.png` || url.split('/').pop();
                    document.body.appendChild(a);
                    // 3. 触发下载
                    a.click();
                    // 4. 清理和回调
                    setTimeout(() => {
                        document.body.removeChild(a);
                        URL.revokeObjectURL(blobUrl);
                        // 这里可以执行下载成功后的回调
                        this.$message({
                            message: '下载中，内容过多需要等待，请勿重复点击，请稍后...',
                            type: 'success'
                          });
                    }, 100);
                }
            } catch (error) {
                console.error('下载失败:', error);
                // 这里可以执行下载失败的回调
                this.$message({
                    message: '下载图片失败',
                    type: 'error'
                });
            }
        },
    }
}

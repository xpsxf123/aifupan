import { exportHtmlToWord as exportHtmlToWordDocx } from './downDocx.js';
function exportHtmlToWord(html, filename = 'document', extension = '.docx', opt={}) {
    return exportHtmlToWordDocx(html,filename,extension,opt);
    // const {output, message} = opt;
    
    // /**
    //  * 清理HTML内容，移除不需要在Word中显示的元素
    //  * @param {string} htmlText - 原始HTML文本
    //  * @returns {string} 清理后的HTML文本
    //  */
    // function getHtml(htmlText){
    //   const divContent = document.createElement('div');
    //   divContent.innerHTML = htmlText;
      
    //   // 需要移除的选择器列表，包括CSS中设置为display: none的class
    //   const selectorsToRemove = [
    //     '.not-word',
    //     '.deepThinkingTitle', 
    //     '.deepThinking',
    //     '.avatar-img-box',
    //     '.wordsBodyAvatar',
    //     '.online-num-img',
    //     '.onlineNumContainer',
    //     '.share-title-img'
    //   ];
      
    //   // 统一处理所有需要移除的元素
    //   selectorsToRemove.forEach(selector => {
    //     const elements = divContent.querySelectorAll(selector);
    //     elements.forEach(el => {
    //       // 使用更安全的remove方法，如果不支持则回退到removeChild
    //       if (el.remove) {
    //         el.remove();
    //       } else if (el.parentNode) {
    //         el.parentNode.removeChild(el);
    //       }
    //     });
    //   });
      
    //   // 特殊处理：移除.share-title内的img元素
    //   const shareImages = divContent.querySelectorAll('.share-title img');
    //   shareImages.forEach(img => {
    //     if (img.remove) {
    //       img.remove();
    //     } else if (img.parentNode) {
    //       img.parentNode.removeChild(img);
    //     }
    //   });
      
    //   return divContent.innerHTML;
    // }
    
    // // 创建Word文档的HTML模板
    // const template = `
    //   <html xmlns:o="urn:schemas-microsoft-com:office:office" 
    //         xmlns:w="urn:schemas-microsoft-com:office:word"
    //         xmlns="http://www.w3.org/TR/REC-html40">
    //     <head>
    //       <meta charset="UTF-8">
    //       <title>${filename}</title>
    //       <!-- 保留原始样式 -->
    //       ${[...document.querySelectorAll('style')].map(s =>{
    //         if(s.outerHTML.indexOf('[data-')>=0 || s.outerHTML.indexOf('vjs')>=0){
    //           return ''
    //         }
    //         return s.outerHTML;
    //       }).join('\n')}
    //       <style>
    //         .deepThinkingTitle{
    //           disaplay: none;
    //         }
    //         .word-body .avatar-img-box{
    //           display: none;
    //         }
    //         .word-body .wordsBodyAvatar{
    //           display: none;
    //         }
    //         .word-body .online-num-img{
    //           display: none;
    //         }
    //         .share-title{
    //           margin: 0 40pt;
    //         }
    //         .share-title img, .share-title-img{
    //           display: none;
    //         }
    //         .share-title-info span{
    //           margin: 0 10pt;
    //         }
    //         .not-word, .word-body .not-word, .word-body .onlineNumContainer{
    //           display: none !important;
    //         }
    //         .words-other-info, .onlineNumContainer{
    //           display: none;
    //         }
    //       </style>
          
    //     </head>
    //     <body class="word-body">
    //       ${getHtml(html)}
    //     </body>
    //   </html>
    // `;


    

    // // 创建Blob对象
    // const blob = new Blob(['\ufeff', template], {
    //   type: 'application/msword'
    // });
    // // 创建Blob对象
    // // const blob1 = new Blob(['\ufeff', template], {
    // //   type: 'application/html'
    // // });
    // // // 创建下载链接
    // // const url1 = URL.createObjectURL(blob1);
    // // const a1 = document.createElement('a');
    // // a1.href = url1;
    // // a1.download = `${filename}.html`;
    // // document.body.appendChild(a1);
    // // a1.click();
    // // // 清理
    // // setTimeout(() => {
    // //   document.body.removeChild(a1);
    // //   URL.revokeObjectURL(url1);
    // //   message && message({
    // //     message: '下载中，内容过多需要等待，请勿重复点击，请稍后...',
    // //     type: 'success'
    // //   });
    // // }, 100);

    // if(output){
    //   return blob;
    // }
    // // 创建下载链接
    // const url = URL.createObjectURL(blob);
    // const a = document.createElement('a');
    // a.href = url;
    // a.download = `${filename}${extension}`;
    // document.body.appendChild(a);
    // a.click();
    // // 清理
    // setTimeout(() => {
    //   document.body.removeChild(a);
    //   URL.revokeObjectURL(url);
    //   message && message({
    //     message: '下载中，内容过多需要等待，请勿重复点击，请稍后...',
    //     type: 'success'
    //   });
    // }, 100);
  }
export {exportHtmlToWord};
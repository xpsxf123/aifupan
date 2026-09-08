import { Message } from "element-ui";
export default function(txt,opt){
    return new Promise((resolve, reject) => {
        try{
            const textarea = document.createElement('textarea');
            const { msg,notMsg } = opt || {};
            textarea.value = txt;
            // 将 textarea 添加到文档中
            document.body.appendChild(textarea);
            // 选中 textarea 中的文本
            textarea.select();
            textarea.setSelectionRange(0, 99999); // 对于移动设备
            // 复制文本
            document.execCommand('copy');
            // 移除 textarea 元素
            document.body.removeChild(textarea);
            if(!notMsg){
                Message.success(msg||'复制成功');
            }
            resolve();
        }catch (error) {
            reject(error);
        }
    })
}
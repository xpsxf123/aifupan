import { MessageBox } from 'element-ui';
import myUtils from './utils';
export default function(notify){
    notify.addTask('douyinWarnStopRecord',(res,resolve)=>{
        MessageBox.confirm(`
            <div>您好！<b class="text-colorErr">您现在的网络不适合录制直播间</b>，请重启网络或换个网络，再开启录制。您也可以联系我们的产品顾问帮您诊断。</div>
            <div>当前录制已停止，<span class="text-colorTheme">停止时间：${myUtils.toFormatDate(new Date())}</span></div>
            `, '提示', {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '网络已重启',
            cancelButtonText: '我知道了',
        })
        .then(() => {
            // 用户点击确定按钮后的操作
        })
        .catch(() => {
            // 用户点击取消按钮后的操作
        });
        // resolve();
    })
}
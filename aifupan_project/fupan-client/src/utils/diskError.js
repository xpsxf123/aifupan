import { Notification } from 'element-ui';
export default function(notify){
    notify.addTask('diskWarnStopRecord',(res,resolve)=>{
        Notification({
            title: '磁盘空间不足',
            message: '磁盘可用空间已不足10G，为保证系统正常运行，将停止录制',
            duration: 0,
            type: 'error'
        });
        res();
    })
}
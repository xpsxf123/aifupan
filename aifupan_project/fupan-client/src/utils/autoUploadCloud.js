import { Notification } from 'element-ui';
import axios from 'axios';
import TcVod from 'vod-js-sdk-v6'
export default function(vue,notify){
    // 获取签名
    const getVodUploadSign = ()=> {
        return vue.$httpBack.vod.vodUploadSign().then(res => {
            return res.data;
        });
    }
    notify.addTask('cloudPropertyInsufficient',(res,resolve)=>{
        Notification({
            title: '失败',
            message: '自动上传云空间失败，云空间资源不足',
            duration: 2000,
            type: 'error'
        });
        resolve();
    })
    // 自动上传功能。
    notify.addTask('autoUploadCloud',async (res,resolve)=>{
        const { filePath } = res;
       // const response = await axios.get('http://127.0.0.1:5001/api/config/getFile?filePath=' + filePath, { responseType: 'blob' });
        const response = await axios.get(`${window.SITE_CONFIG['clientApiURL']}/config/getFile?filePath=` + encodeURIComponent(filePath), { responseType: 'blob' });
        const file = new File([response.data], 'temp.mp4', { type: 'video/mp4' });
        const tcVod = new TcVod({
            getSignature: getVodUploadSign
        });
        const uploader = tcVod.upload({
            mediaFile: file, // 媒体文件（视频或音频或图片），类型为 File
        });
        return await uploader.done().then(function (doneResult) {
            Notification({
                title: '成功',
                message: '视频自动上传云空间成功',
                duration: 2000,
                type: 'success'
            });
            return resolve(doneResult.video.url);
        }).catch(function (err) {
            Notification({
                title: '失败',
                message: '视频自动上传失败',
                duration: 2000,
                type: 'error'
            });
        });
    },(rej)=>{
        console.error('分析失败',rej);
    })
}
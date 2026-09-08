import env from '@/env';

const copyShareUrl = (shareUrl, opt) => {
    try {
        let messageFun = null;
        const { message, copyPrefix, notPrefix } = opt || {};
        if (typeof opt === 'function') {
            messageFun = opt;
        } else {
            messageFun = message;
        }
        // 开发模式
        let dev = env.dev;
        const isIfarame = sessionStorage.getItem('iframe');
        if (dev) {
            let url = shareUrl?.split('#')?.[1];
            shareUrl = `${location.origin}${isIfarame ? '/client' : ''}/#${url}`;
        }
        // 获取复制链接前缀
        let copyPrefixText = '';
        if (isIfarame || notPrefix) {
            copyPrefixText = ''
        } else {
            // 默认前缀
            copyPrefixText = copyPrefix || '复制以下链接在电脑浏览器打开：'
        }
        shareUrl = copyPrefixText + shareUrl;
        // navigator.clipboard.writeText(shareUrl);
        // 创建一个临时 textarea 元素
        const textarea = document.createElement('textarea');
        textarea.value = shareUrl;
        // 将 textarea 添加到文档中
        document.body.appendChild(textarea);
        // 选中 textarea 中的文本
        textarea.select();
        textarea.setSelectionRange(0, 99999); // 对于移动设备
        // 复制文本
        document.execCommand('copy');
        // 移除 textarea 元素
        document.body.removeChild(textarea);
        if (typeof messageFun === 'function') {
            messageFun("已复制分享链接到剪贴板");
        }
        return shareUrl;
    } catch (e) {
        console.error(e);
    }
}

// 获取文件类型和文件数据
const getSourceData = (data = {}, opt = {}) => {
    const {
        videoInfo,
        uploadFile,
        fileInfo,
        info,
        contrastInfo
    } = data;
    const { isCompare } = opt;
    const upload = uploadFile || fileInfo
    let isVideo = videoInfo && videoInfo.VideoId;
    let isContrast = isCompare || (info && info.ContrastId) || (contrastInfo && contrastInfo.ContrastId);
    //sourceType -  2: 对比,1: 上传文件,0: 录制视频
    const sourceType = isContrast ? 2 : isVideo ? 0 : 1;
    // sourceId -  视频id,文件id,对比id(根据sourceType判断id类型)
    const sourceId = isContrast
        ? info?.ContrastId || contrastInfo?.ContrastId
        : isVideo
            ? videoInfo?.VideoId
            : (upload?.fileId || upload?.FileId);
    return {
        sourceType,
        sourceId
    }
}
const downloadBlobTxt = (txt,opt={})=>{
    const { blobOpt = {}, fileName } = opt;
    const url = window.URL.createObjectURL(new Blob([txt]),blobOpt);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `${fileName}.txt`); // 设置下载文件名
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
const toOfficialWebsite = (httpClient)=>{
    httpClient?.system?.openOfficial();
}
const assemblyShareUrl = (share)=>{
    let shareUrl = '';
    if (env.prod) {
        shareUrl = `https://ifupan.com/client/#/${share}`
    } else if (env.release) {
        shareUrl = `https://yz.ifupan.com/client/#/${share}`
    } else {
        shareUrl = `https://test.ifupan.com/client/#/${share}`
    }
    return shareUrl
}

const pureRouterList = [
    '/dataAnalysis',
    '/addCompere/douyin',
    '/addCompere/kuaishou',
    '/addCompere/shipinhao',
    '/replay',
    '/uploadVideo',
    '/uploadText',
    '/extractDoc',
    '/expert',
    '/subscribeExpert',
    '/subscribeHotItem',
    '/system'
]

export { copyShareUrl, getSourceData, toOfficialWebsite, downloadBlobTxt,assemblyShareUrl,pureRouterList }
export default {
    downloadBlobTxt,
    toOfficialWebsite,
    copyShareUrl,
    getSourceData,
    assemblyShareUrl,
    pureRouterList
}

import { isWeb } from '@/config/env/index';
import myUtils from '@/utils/utils';

const postMessageDb = myUtils.debounce(500);
const postMessageHttpStatus = {};
let timeOutNum = 0;


const addPostMessageHttpStatus = (code, data) => {
    postMessageHttpStatus[code] = {
        ...data
    }
}
const runPostMessage = () => {
    postMessageDb(() => {
        Object.keys(postMessageHttpStatus).forEach(key => {
            window.parent.postMessage({
                type: 'http',
                data: {
                    ...postMessageHttpStatus[key]
                }
            }, '*');
            delete postMessageHttpStatus[key];
        })
    })
}

const isIframeRunPostMessage = (type) => {
    let isTimeOut = type || 'isTimeOut';
    let isReturn = sessionStorage.getItem('iframe') || isWeb;
    if ((isReturn && !isTimeOut) || timeOutNum > 3) {
        return;
    }
    if (isTimeOut) {
        timeOutNum++;
        setTimeOutNum();
    }
    if (!sessionStorage.getItem('iframe')) {
        setTimeout(() => {
            isIframeRunPostMessage('isTimeOut');
        }, 500)
    } else {
        timeOutNum = 0;
        setTimeOutNum();
        runPostMessage();
    }
}

const setTimeOutNum = ()=>{
    sessionStorage.setItem('ifPostMessage',timeOutNum);
}

function getQueryParam(name) {
    const queryString = window.location.search.substring(1);
    const params = queryString.split('&');
    for (const param of params) {
      const [key, value] = param.split('=');
      if (key === name) {
        return decodeURIComponent(value);
      }
    }
    return null;
}

const init = ()=>{
    sessionStorage.removeItem('iframe');
    timeOutNum = sessionStorage.getItem('ifPostMessage') || 0;
    if(getQueryParam('iframe')){
        lookIframe();
    }
}

const isMobile=()=>{
    return getQueryParam('isMobile')
}

const lookIframe = ()=>{
    sessionStorage.setItem('iframe','1');
}

const isIframe = ()=>{
    return !!sessionStorage.getItem('iframe');
}

export {
    init,
    lookIframe,
    isIframe,
    isMobile,
    runPostMessage,
    isIframeRunPostMessage,
    addPostMessageHttpStatus
}

export default{
    init,
    lookIframe,
    isIframe,
    isMobile,
    runPostMessage,
    isIframeRunPostMessage,
    addPostMessageHttpStatus
};
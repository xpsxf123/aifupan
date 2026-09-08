import CryptoJS from 'crypto-js';
import FingerprintJS from '@fingerprintjs/fingerprintjs';
import ipConfig from "./../../ipConfig";
import store from './../store'
import {isEmpty} from "lodash";

const {signatureConfig} = ipConfig;
const _c = signatureConfig[window.SITE_CONFIG.env].signature
const _cBackup = JSON.parse(JSON.stringify(_c));

// 签名类型常量
const SIGN_TYPE = {
    HMAC_SHA256: 'HMAC-SHA256',
    MD5: 'MD5'
};

// ROT13算法，用于混淆返回错误密钥
function _rot13(str) {
    return str.replace(/[a-zA-Z]/g, c =>
        String.fromCharCode((c <= 'Z' ? 90 : 122) >= (c = c.charCodeAt(0) + 13) ? c : c - 26)
    );
}

// 获取解码后的密钥
function _getSecret(appId, hostname = globalThis.location?.hostname || '') {
    // if (!_c._k) {
    //     _c._k = JSON.parse(JSON.stringify(_cBackup._k));
    // }
    //
    // if (hostname !== "" && !_c._d.includes(hostname) && hostname !== "localhost" && hostname !== "127.0.0.1" && !globalThis.location?.protocol?.includes('file:')) {
    //     return _rot13(atob(_c._k._a) + atob(_c._k._b) + atob(_c._k._c));
    // }

    return atob(_c._k._a) + atob(_c._k._b) + atob(_c._k._c);
}

// 生成随机字符串
function _generateNonce(length = 16) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    return Array.from({length}, () => chars.charAt(Math.floor(Math.random() * chars.length))).join('');
}

// 生成UUID
function _generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
}
// 生成指纹
async function getVisitorId() {
    const fp = await FingerprintJS.load()
    const result = await fp.get()
    return result.visitorId
}

// 获取设备指纹
async function _getFingerprint() {
    const isAiFuPan = navigator.userAgent?.indexOf('aifupan') >= 0;
    const fingerprint = await getVisitorId();
    const web_fingerprint = `web_${fingerprint}`
    const default_client_fingerprint = store.getters.getMachineCode ? `desktop_${store.getters.getMachineCode}` : web_fingerprint;
    // console.log("default_client_fingerprint:", default_client_fingerprint)
    return isAiFuPan ? default_client_fingerprint : web_fingerprint
}

// FNV-1a哈希算法实现
function _hashFnv32a(str) {
    let hash = 0x811c9dc5;
    for (let i = 0; i < str.length; i++) {
        hash ^= str.charCodeAt(i);
        hash += (hash << 1) + (hash << 4) + (hash << 7) + (hash << 8) + (hash << 24);
    }
    return (hash >>> 0).toString(16);
}

// 生成当前时间戳
function _getTimestamp() {
    return Date.now().toString();
}

// 生成请求ID - 优化版本
function _generateRequestId(appId, timestamp) {
    // const randomPart = _generateUUID();
    return _generateUUID()
    // const timePart = timestamp || _getTimestamp();
    // const appIdPrefix = appId ? appId.substring(0, Math.min(8, appId.length)) : '';
    // const requestIdBase = `${appIdPrefix}-${timePart.slice(-6)}-${randomPart}`;
    // return _hashFnv32a(requestIdBase);
}

// 解析URL查询参数
function _parseQueryParams(url) {
    const queryString = url?.split('?')[1];
    if (!queryString) return {};
    return Object.fromEntries(queryString.split('&').map(pair => {
        const [k, v] = pair.split('=');
        return [decodeURIComponent(k), decodeURIComponent(v || '')];
    }));
}

// 将表单数据转换为字符串
function _formDataToString(formData) {
    if (!formData) return '';

    if (formData instanceof FormData) {
        const params = {};
        for (const [key, value] of formData.entries()) {
            params[key] = value;
        }
        return _objectToQueryString(params);
    }

    if (formData instanceof URLSearchParams) {
        return formData.toString();
    }

    if (typeof formData === 'object') {
        return _objectToQueryString(formData);
    }

    return String(formData);
}

// 将对象转换为查询字符串
function _objectToQueryString(obj) {
    return Object.keys(obj)
        .sort()
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(obj[key])}`)
        .join('&');
}

// 获取需要签名的内容
function _getContentToSign(method, url, body, contentType) {
    method = (method || 'GET').toUpperCase();
    if (method === 'GET') {
        const params = {}
        if (!isEmpty(body)) {
            for (let key in body) {
                const result = typeof body[key] === 'string' ? body[key].trim() : body[key]
                if (result) params[key] = result
            }
        }
        const query = !isEmpty(params) ? new URLSearchParams(params).toString() : null;
        const fullPath = query ? `${url}?${query}` : url;
        return _objectToQueryString(_parseQueryParams(fullPath));
    }

    if (['POST', 'PUT'].includes(method)) {
        if (contentType?.includes('application/json')) {
            return typeof body === 'string' ? body : JSON.stringify(body || {});
        }

        if (contentType?.includes('application/x-www-form-urlencoded') || contentType?.includes('multipart/form-data')) {
            return _formDataToString(body);
        }
    }

    return '';
}

// 生成签名
function _generateSignature(params, appSecret, signType) {
    const sortedParams = Object.keys(params).sort().reduce((acc, key) => {
        acc[key] = params[key];
        return acc;
    }, {});

    const stringToSign = Object.entries(sortedParams)
        .map(([key, value]) => `${key}=${value}`)
        .join('&');

    if (signType === SIGN_TYPE.MD5) {
        return CryptoJS.MD5(appSecret + stringToSign + appSecret).toString();
    } else {
        return CryptoJS.enc.Base64.stringify(CryptoJS.HmacSHA256(stringToSign, appSecret));
    }
}

// 主模块对象
const apiSignature = {
    SIGN_TYPE,

    async createSignatureHeaders(body, method, url, contentType, signType = SIGN_TYPE.HMAC_SHA256) {
        method = method || 'GET';
        body = body || null;
        contentType = contentType ? contentType : 'application/json';
        const isAiFuPan = navigator.userAgent?.indexOf('aifupan') >= 0;

        const appId = signatureConfig[window.SITE_CONFIG.env].appId
        const timestamp = _getTimestamp();
        const nonce = _generateNonce();
        const fingerprint = await _getFingerprint()
        const requestId = _generateRequestId(appId, timestamp);
        const appSecret = _getSecret(appId);
        const contentToSign = _getContentToSign(method, url, body, contentType);

        const params = {
            'X-App-Id': appId,
            'X-Timestamp': timestamp,
            'X-Nonce': nonce,
            'X-RequestId': requestId,
            'X-Fingerprint': fingerprint
        };

        if (contentToSign) {
            try {
                params.body = decodeURIComponent(contentToSign);
            } catch (e) {
                params.body = contentToSign;
            }
        }

        const signature = _generateSignature(params, appSecret, signType);

        return {
            'X-App-Id': appId,
            'X-Timestamp': timestamp,
            'X-Nonce': nonce,
            'X-RequestId': requestId,
            'X-Fingerprint': fingerprint,
            'X-Signature': signature,
            'X-Sign-Type': signType,

        };
    },
};

export default apiSignature;

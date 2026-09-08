
const ipConfig = require('./../../ipConfig');


const { platform_code } = ipConfig;
/**
 * 判断当前运行平台的类型
 * @param {Object} [options] - 可选的直接配置参数
 * @param {boolean} [options.forcePlatform] - 直接指定平台类型: 'client' | 'browser' | 'website'
 * @returns {h5|client|cloudSpace|official} 返回平台类型
 *  移动端：h5
    客户端：client
    云空间：cloudSpace
    官网：official 
 */
const PLATFORM_CODE = platform_code[window.SITE_CONFIG.env];
function getPlatform(options = {}) {
    const { isCode } = options;
    // 如果有直接配置参数，优先使用
    let platform = '';
    if (options.forcePlatform) {
      platform = options.forcePlatform;
    }else{
        const userAgent = window?.navigator?.userAgent || '';
        const isAifupan = userAgent?.indexOf('aifupan') >= 0;
        // 如果是网站构建目标
        if (window.SITE_CONFIG?.website === 'website') {
            platform = 'official';
        }else if (isAifupan) {
            platform = 'client'; // 客户端内嵌网站
        } else {
            platform = 'cloudSpace'; // 普通浏览器访问
        }
    }
    if(isCode){
        return PLATFORM_CODE[platform] || platform;
    }else{
        return platform;
    }
}

export default getPlatform
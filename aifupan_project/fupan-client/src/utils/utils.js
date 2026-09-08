import {isNumber} from 'lodash'

const myUtils = {};

/**
 * 将时间戳转换为本地时区的年月日格式
 * @param {number|string} timestamp - 时间戳（毫秒或秒）
 * @param {string} [format='YYYY-MM-DD'] - 输出格式，默认为'YYYY-MM-DD'
 * @param {boolean} [isSeconds=false] - 是否为秒级时间戳，默认为false（毫秒级）
 * @returns {string} 格式化后的日期字符串
 */
myUtils.formatLocalDate = function(timestamp, format = 'YYYY-MM-DD HH:mm:ss', opt={}) {
    const {isSeconds=false} = opt;
    // 处理时间戳
    let ts = Number(timestamp);
    if (isSeconds) {
      ts *= 1000; // 如果是秒级时间戳，转换为毫秒
    }
    // 创建Date对象（自动使用本地时区）
    const date = new Date(ts);
    // 验证Date对象是否有效
    if (isNaN(date.getTime())) {
      throw new Error('Invalid timestamp');
    }
    // 提取日期组件
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    // 根据格式替换
    return format
      .replace('YYYY', year)
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hours)
      .replace('mm', minutes)
      .replace('ss', seconds);
  }

myUtils.toTimeFormatDate = function(time,option={}){
    const { format='yyyy-MM-dd HH:mm:ss', type= 'all' } = option
    let date = new Date(Number(time));
    let dates = date.toJSON()?.split('T')?.map(d=>{
        return d.indexOf('.')>=0?d?.split('.')[0]:d;
    });
    if(format === 'yyyy-MM-dd' || type==='date'){
        return datas[0]
    }else if(format === 'HH:mm:ss' || type==='dayTime'){
        return dates[1]
    }
    return dates.join(' ');
}

/**
 * 手动格式化时间
 * @param {Number} timestamp 时间戳
 * @param {Number} offsetHours 时区偏移
 * @returns {String} 格式化后的时间
 */
myUtils.formatDateManual = function(timestamp, offsetHours = 8) {
  timestamp = timestamp.toString().length === 10 ? timestamp * 1000 : timestamp;
  // 计算UTC时间
  const utcTime = timestamp + (new Date().getTimezoneOffset() * 60000);
  // 应用目标时区偏移
  const targetTime = utcTime + (offsetHours * 3600000);
  const date = new Date(targetTime);
  const pad = num => num.toString().padStart(2, '0');
  return [
    date.getUTCFullYear(),
    pad(date.getUTCMonth() + 1),
    pad(date.getUTCDate())
  ].join('-') + ' ' + [
    pad(date.getUTCHours()),
    pad(date.getUTCMinutes()),
    pad(date.getUTCSeconds())
  ].join(':');
}

/**
 * 将时间戳转换为中文格式
 * @param {Number} timestamp 时间戳
 * @returns {String} 中文格式的时间
 */
myUtils.timestampToChinese = function(timestamp, option={}) {
    const { format = 'yyyy-MM-dd HH:mm:ss' } = option;
    // 自动处理秒级和毫秒级时间戳
    timestamp = timestamp.toString().length === 10 ? timestamp * 1000 : timestamp;
    const date = new Date(typeof timestamp === 'string' ? timestamp * 1 : timestamp);
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hours = date.getHours();
    const minutes = date.getMinutes();
    const seconds = date.getSeconds();
    if(format === 'yyyy-MM-dd HH:mm:ss'){
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }else if(format === 'yyyy-MM-dd'){
        return `${year}-${month}-${day}`;
    }else if(format === 'HH:mm:ss'){
        return `${hours}:${minutes}:${seconds}`;
    }
  }


/**
 * 加两位小数
 * @param {Object} val
 */
myUtils.addXiaoshu = function (val) {
    let result = val + "";
    if (result.indexOf(".") == -1) {
        return result + ".00";
    } else if (result.length - result.indexOf(".") == 2) {
        return result + "0";
    } else {
        return result;
    }
}

/**
 * 保留两位小数
 * @param {Object} val
 */
myUtils.retainDecimals = function (val) {
    let result = val + "";
    let index = result.indexOf(".");
    if (index != -1) {
        if (result.length > index + 2) {
            result = result.substring(0, index + 3);
        } else if (result.length > index + 1) {
            result = result.substring(0, index + 2);
        }
    }
    return result;
}

/**
 * 补零
 * @param {Object} val
 */
myUtils.zeroFill = function (val) {

    if (0 <= val && val < 10) {
        return "0" + val;
    }

    return val;
}

/**
 * 将毫秒转成时分秒
 * @param {Object} val
 */
myUtils.toformatTime = function (val) {
    val = parseInt(val / 1000);
    let hour = parseInt(val / 3600);
    let minute = parseInt(val % 3600 / 60);
    let second = parseInt(val % 3600 % 60);
    return myUtils.zeroFill(hour) + ":" + myUtils.zeroFill(minute) + ":" + myUtils.zeroFill(second);
}
myUtils.toformatSeconds = function (timeStr) {
    const [h, m, s] = timeStr.split(':').map(Number)
    return h * 3600 + m * 60 + s
}

/**
 * 将文件大小从字节转换为最合适的单位(GB或MB)
 * @param {number} bytes - 文件大小，单位为字节(B)
 * @param {number} [gbThreshold=0.1] - GB单位阈值，默认0.1GB
 * @param {number} [decimalPlaces=2] - 保留的小数位数，默认2位
 * @returns {string} 转换后的大小字符串，包含单位
 */
myUtils.formatFileSize= function(bytes, opt={}) {
    const {separa = false, unit = '', gbThreshold = 0.1, decimalPlaces = 2, passUnit = 'B'} = opt;
    if (isNaN(bytes) || bytes < 0) {
        if(separa){
            return {
                size: '0',
                unit: unit || 'MB'
            }
        }else{
            return '0 MB';
        }
    }
    const unitList = ['B','KB','MB','GB','TB','PB','EB','ZB','YB'];
    const startIndex = unitList.findIndex(d=>passUnit === d);
    const endIndex = unit ? unitList.findIndex(d=>unit === d): 3;
    const len = endIndex - startIndex;
    let o = {
        size: bytes,
        unit: passUnit
    };
    if(bytes <=0){
        return {
            size: '0',
            unit: unit || 'MB'
        };
    }
    for(let i = 1; i<=len;i++){
        const unitSize = 1024 ** (i);
        if(unitSize === 1){ continue; }
        const size = bytes / unitSize;
        o.size = size.toFixed(decimalPlaces);
        o.unit = unitList[startIndex + i];
        if(unit === '' && size < gbThreshold){
            break;
        }
    }
    if(separa){
        return o
    }else{
        return `${o.size} ${o.unit}`
    }
}


/**
 * Convert minutes to hours with configurable decimal places and units
 * @param {number} minutes - The minutes to convert
 * @param {Object} [options] - Configuration options
 * @param {number} [options.decimalPlaces=1] - Number of decimal places to keep
 * @param {boolean} [options.showUnits=true] - Whether to include units in the output
 * @param {string} [options.hourUnit='h'] - Unit for hours
 * @param {string} [options.minuteUnit='m'] - Unit for minutes
 * @returns {string|number} The converted value with or without units
 */
myUtils.minutesToHours=function(minutes, options = {}) {
    // Set default options
    const {
        decimalPlaces = 1,
        showUnits = true,
        unit = 'h'
    } = options;
    // Convert minutes to hours
    const hours = minutes / 60;
    // Round to specified decimal places
    const roundedHours = Number(hours.toFixed(decimalPlaces));
    // Return with or without units
    if (!showUnits) {
        return roundedHours;
    }
    return `${roundedHours}${unit}`;
}

/**
 * 将毫秒转成分秒
 * @param {Object} val
 */
myUtils.toformatTimeMM_ss = function (val) {
    val = parseInt(val / 1000);
    let minute = parseInt(val / 60);
    let second = parseInt(val % 60);

    return myUtils.zeroFill(minute) + ":" + myUtils.zeroFill(second);
}

/**
 * 将毫秒转成分秒 --中文
 * @param {Object} val
 */
myUtils.toformatTimeMM_ssChinse = function (val) {
    val = parseInt(val / 1000);
    let minute = parseInt(val / 60);
    let second = parseInt(val % 60);

    return myUtils.zeroFill(minute) + "分" + myUtils.zeroFill(second) + "秒";
}

/**
 * 将毫秒转成时分秒-中文
 * @param {Object} val
 */
myUtils.toformatTimeChinse = function (val) {
    val = parseInt(val / 1000);
    let hour = parseInt(val / 3600);
    let minute = parseInt(val % 3600 / 60);
    let second = parseInt(val % 3600 % 60);
    let result = "";
    if (hour) {
        result += hour + "时";
    }
    if (minute) {
        result += minute + "分";
    }else if(second && hour){
        // 如果秒数存在，且小时数存在，则补0分
        result += "0分";
    }
    if (second) {
        result += second + "秒";
    }
    return result;
}

myUtils.formatSeconds = function (seconds) {
    if (!seconds) return '00:00:00'
    const h = String(Math.floor(seconds / 3600)).padStart(2, '0')
    const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0')
    const s = String(seconds % 60).padStart(2, '0')
    return `${h}:${m}:${s}`
}

/**
 * 格式化HTTP请求或响应中的数据
 * 该函数主要用于将给定的数据对象中的一些特定值转换为null，或删除一些未定义的属性
 * @param {Object} data - 需要格式化的数据对象
 * @returns {Object} - 格式化后的数据对象
 */
myUtils.httpFormat = function(data) {
    // 深拷贝传入的数据对象，以避免直接修改原始数据
    let d = JSON.parse(JSON.stringify(data));
    // 遍历深拷贝后的数据对象的所有键
    Object.keys(d).map((key) => {
        let val = d[key];
        // 将空字符串、空数组或-1的值转换为null
        if(val === '' || val === -1 || (Array.isArray(val) && val.length ===0)){
            d[key] = null;
        // 删除值为undefined的属性
        } else if(typeof val === 'undefined'){
            delete d[key];
        }
    })
    // 返回格式化后的数据对象
    return d;
}

/**
 * 将时分秒转成秒
 * @param {Object} val
 */
myUtils.toSecond = function (val) {
    let second = 0;

    if (val) {
        let arr = val.split(":");
        if (arr[0]) {
            // 将小时转成秒
            let hour = parseInt(arr[0])
            second += hour * 3600
        }
        if (arr[1]) {
            // 将分转成秒
            let minute = parseInt(arr[1])
            second += minute * 60
        }
        if (arr[2]) {
            second += parseInt(arr[2]);
        }
    }

    return second;
}

/**
 * 将Date转成yyyy-MM-dd HH:mm:ss格式
 * @param {Object} val
 */
myUtils.toFormatDate = function (date) {
    var year = date.getFullYear();
    var month = ("0" + (date.getMonth() + 1)).slice(-2);
    var day = ("0" + date.getDate()).slice(-2);
    var hour = ("0" + date.getHours()).slice(-2);
    var minute = ("0" + date.getMinutes()).slice(-2);
    var second = ("0" + date.getSeconds()).slice(-2);

    return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
}

/**
 * 将yyyy-MM-dd HH:mm:ss格式秒
 * @param {Object} val
 */
myUtils.toSecondByDate = function (timeString) {
    const date = new Date(Date.parse(timeString));
    return Math.floor(date.getTime() / 1000);
}

/**
 * @method numberToSting 数字转文字
 * @param {Number} number
 * @returns String
 */
myUtils.numberToSting = function (number,option={}) {
    const {unit = '万', isFormat = false} = option;
    if (number >= 10000) {
        let num = (number / 10000).toFixed(1);
        return `${isFormat? parseFloat(num).toLocaleString() :parseFloat(num)}${unit}`;
    } else {
        return number;
    }
}

/**
 * 数字转千分位或万单位格式化
 * @param {number} number - 需要格式化的数字
 * @param {object} option - 配置选项
 * @param {string} option.unit - 单位，默认为'万'
 * @param {boolean} option.isFormat - 是否格式化千分位，默认为false
 * @param {boolean} option.autoFormat - 是否自动格式化（1万以上显示w单位，1万以下显示千分位），默认为false
 * @returns {string} 格式化后的字符串
 * @description 支持千分位格式化和万单位转换，新增autoFormat选项用于观看热度和粉丝量格式化
 */
myUtils.toLocale = function (number, option = {}) {
    if (typeof number === 'undefined' || number === null) {
        return '';
    }
    
    const { unit = '万', isFormat = false, autoFormat = false } = option;
    const num = Number(number);
    
    // 处理无效数字
    if (isNaN(num)) {
        return '';
    }
    
    // 自动格式化模式：用于观看热度和粉丝量
    if (autoFormat) {
        if (num >= 10000) {
            const wanValue = (num / 10000).toFixed(1);
            const formattedValue = parseFloat(wanValue); // 去除末尾的0
            return `${formattedValue}w`;
        } else {
            return num.toLocaleString();
        }
    }
    
    // 原有逻辑：兼容现有用法
    if (unit && unit !== '万') {
        return myUtils.numberToSting(number, { unit, isFormat });
    } else {
        return num.toLocaleString();
    }
}


/**
 * @method debounce 节流防抖
 * @returns Function
 */
myUtils.debounce = function (delay, fn, immediately) {
    let immediate = typeof fn === 'boolean' ? fn : immediately;
    var timer; // 维护同一个timer
    return function (runFn, ...arg) {
        // 为真立即触发
        if (immediate) {
            immediate = false;
            if (typeof runFn === 'function') {
                runFn(...arg)
            } else if (typeof fn === 'function') {
                fn(runFn, ...arg);
            }
        }
        clearTimeout(timer);
        timer = setTimeout(function () {
            // 如果首次执行参数有值则不会执行后续函数逻辑
            if(typeof immediate !=='undefined'){
                immediate = true;
                return;
            }
            if (typeof runFn === 'function') {
                runFn(...arg)
            } else if (typeof fn === 'function') {
                fn(runFn, ...arg);
            }
        }, delay);
    }
}


myUtils.throttle = (delay, func, type)=>{
    // first 为真定时器之前执行，假在之后执行
    let first = typeof func !=='function'? func !==1 : type !== 1;
    let timer = null;
    return function(fn,...arg) {
        if (!timer) {
            // 定时器之前执行
            if(first){
                if(typeof fn === 'function'){
                    fn(...arg)
                }else if(typeof func === 'function'){
                    func(fn,...arg);
                }
            }
            timer = setTimeout(() => {
                // 定时器执行时在执行
                if(!first){
                    if(typeof fn === 'function'){
                        fn(...arg)
                    }else if(typeof func === 'function'){
                        func(fn,...arg);
                    }
                }
                timer = null;
            }, delay);
        }
    };
}



// 运行屏幕监听
myUtils.monitorScreen = function (fn, name, sort) {
    // 首次运行
    typeof fn === 'function' && fn();
    // 持续化监听可添加任务队列，屏幕变化将会重新运行
    if (name && typeof fn === 'function') {
        // 储存运行函数
        runMonitorScreen.funs[name] = fn;
        // 手动加入权重设置执行顺序
        runMonitorScreen.funs[name].sort = sort || Object.keys(runMonitorScreen.funs).length + 999;
        // 运行程序。
        runMonitorScreen();
    }
}

// 删除监听函数
myUtils.delMonitorScreen = function (name) {
    delete runMonitorScreen.funs[name]
}

// 执行监听
runMonitorScreen.lock = false;
runMonitorScreen.funs = {};
// 执行监听函数防抖
const _debounceMonitorScreen = myUtils.debounce(500)
function runMonitorScreen() {
    if (runMonitorScreen.lock) { return }
    // 监听屏幕变化（事件持续存在不会销毁）
    window.addEventListener('resize', ($event) => {
        _debounceMonitorScreen(() => {
            // 运行屏幕监听队列，优先排序在执行队列
            const fns = Object.values(runMonitorScreen.funs).sort((a, b) => a.sort - b.sort)
            fns.forEach(fn => {
                typeof fn === 'function' && fn();
            })
        })
    }, true)
    // 运行第一次枷锁。
    runMonitorScreen.lock = true;
}

myUtils.indexOfAll = function (text, word) {
    let all = [];
    let i = text.indexOf(word);
    while (i !== -1) {
        all.push(i)
        i = text.indexOf(word, i + 1);
    }
    return all;
}



// 关键词,铭感词排序.
myUtils.wordsSort = function (a, b, countKey) {
    // 先按词语类型升序排序
    if (a.wordsType !== b.wordsType) {
        return a.wordsType - b.wordsType;
    }

    // 再按词语分类升序排序
    if (a.typeSort !== b.typeSort) {
        return a.typeSort - b.typeSort;
    }
    if(a?.cruxTypeInfo?.sort !== b?.cruxTypeInfo?.sort){
        return a?.cruxTypeInfo?.sort - b?.cruxTypeInfo?.sort
    }

    // 再按词语分组降序排序
    if (a.groupStr !== b.groupStr) {
        return b.groupStr.localeCompare(a.groupStr);
    }

    // 最后按出现的次数降序排序
    if (typeof countKey === 'string' && typeof a[countKey] !== 'undefined' && typeof b[countKey] !== 'undefined') {
        return b[countKey] - a[countKey]
    } else {
        // 关键词更具数量排序
        return b.count - a.count
    }

    // 类型排序完成之后,敏感词更具词组排序,关键词 优先根据type的sort排序,最后在更具数量排序.
    // if(a.wordsType === b.wordsType){
    //     if(a.wordsType == 0){
    //         // 敏感词根据分组排序
    //         return a.groupStr.localeCompare(b.groupStr)
    //     }else{
    //         if(a.typeSort != b.typeSort){
    //             // tyoeSort来自于后台分类排序
    //             return a.typeSort - b.typeSort;
    //         }else{
    //             // if(a.typeSort === 0){
    //             // }
    //             if(typeof countKey === 'string' && typeof a[countKey] !== 'undefined' && typeof b[countKey] !== 'undefined'){
    //                 return b[countKey] - a[countKey]
    //             }else{
    //                 // 关键词更具数量排序
    //                 return b.count - a.count
    //             }
    //         }
    //     }
    // }
}

myUtils.isGreaterThanZero = function (val) {
    if ([null, undefined, NaN].includes(val)) return false
    const num = Number(val);
    return !isNaN(num) && num > 0;
}

myUtils.fnw = function (num, length = 2, minus = false) {
    // 尝试转换为数字
    num = parseFloat(num)

    // 非有限数返回 null
    if (!Number.isFinite(num)) return null

    // minus = false 时，不处理负数
    if (!minus && num < 0) return null

    // 处理符号与绝对值
    const sign = num < 0 ? '-' : ''
    const absNum = Math.abs(num)

    // 把 -0 当作 0
    if (absNum === 0) return '0'

    const formatWithThousands = (number) => {
        const [intPart, decimalPart = ''] = Number(number).toFixed(length).split('.')
        const formattedInt = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
        if (Number(decimalPart) === 0) {
            return formattedInt
        }
        return `${formattedInt}.${decimalPart}`
    }

    if (absNum < 10000) {
        return sign + formatWithThousands(absNum)
    } else if (absNum >= 10000 && absNum < 100000000) {
        const wNum = absNum / 10000
        return sign + formatWithThousands(wNum) + 'w'
    } else {
        const billions = Math.floor(absNum / 100000000)
        return sign + `${billions}亿+`
    }
}

myUtils.dataView = function (resData, keys, length) {
    const [start, end] = keys
    const act = isNumber(resData[start]) && isNumber(resData[end]) && resData[start] >= 0 && resData[end] >= 0
    if (act) {
        if (resData[start] === resData[end]) {
            return `${myUtils.fnw(resData[start], length)}`
        } else {
            return `${myUtils.fnw(resData[start], length)} ~ ${myUtils.fnw(resData[end], length)}`
        }
    } else {
        return '-'
    }
}


myUtils.addSeconds = function (date, seconds) {
    const d = new Date(date?.replace(/-/g, '/'));
    if (isNaN(d)) throw new Error('Invalid date format');
    const newDate = new Date(d.getTime() + seconds * 1000);
    const pad = n => String(n).padStart(2, '0');

    const Y = newDate.getFullYear();
    const M = pad(newDate.getMonth() + 1);
    const D = pad(newDate.getDate());
    const h = pad(newDate.getHours());
    const m = pad(newDate.getMinutes());
    const s = pad(newDate.getSeconds());

    return `${Y}-${M}-${D} ${h}:${m}:${s}`;
}

// 对象去空
myUtils.objectFommatEmpty = function (obj,opt={}) {
    const {keyEmpty=true,valueEmpty=true, arrayEmpty=true,objectEmpty=true} = opt;
    return Object.fromEntries(
        Object.entries(obj).filter(([_, value]) => {
            if(_ === '' && keyEmpty){return false};
            if (value === null || value === undefined || value === '' && valueEmpty) return false;
            if (typeof value === 'object' && Object.keys(value).length === 0 && objectEmpty) return false;
            if (Array.isArray(value) && value.length === 0 && arrayEmpty) return false;
            return true;
        })
    );
}

myUtils.copyToClipboard = async function (text) {
    const input = document.createElement('input')
    input.setAttribute('value', text)
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
}

myUtils.dailySession = {
    key: 'daily_session_data',
    timeKey: 'daily_session_expire',

    set(data) {
        // 设置过期时间为次日凌晨 00:00
        const expire = new Date();
        expire.setHours(24, 0, 0, 0);

        localStorage.setItem(this.key, JSON.stringify(data));
        localStorage.setItem(this.timeKey, expire.getTime());
    },

    get() {
        const expireTime = parseInt(localStorage.getItem(this.timeKey), 10);
        const now = Date.now();
        if (!expireTime || now > expireTime) {
            this.clear(); // 自动清除过期数据
            return null;
        }

        const data = localStorage.getItem(this.key);
        return data ? JSON.parse(data) : null;
    },

    clear() {
        localStorage.removeItem(this.key);
        localStorage.removeItem(this.timeKey);
    }
};



myUtils.mergeArrays = function (arr1, arr2, label = "label", value = 'value') {
    const map = {};

    // 遍历第一个数组 -> 填充 value1
    arr1.forEach(item => {
        map[item[label]] = {[label]: item[label], value1: item[value], value2: ""};
    });

    // 遍历第二个数组 -> 填充 value2 或新增
    arr2.forEach(item => {
        if (map[item[label]]) {
            map[item[label]].value2 = item[value];
        } else {
            map[item[label]] = {[label]: item[label], value1: "", value2: item[value]};
        }
    });
    return Object.values(map);
}


myUtils.isNumberOrNumericString = function (val) {
    // 判断 number 类型且不是 NaN
    if (typeof val === "number" && !isNaN(val)) {
        return true;
    }
    if (typeof val === "string" && val.trim() !== "" && !isNaN(Number(val))) {
        return true;
    }
    return false;
}


myUtils.createEnumHelper = function (enumObj) {
    const reverse = Object.freeze(
        Object.fromEntries(
            Object.entries(enumObj).map(([k, v]) => [v, k])
        )
    )

    return Object.freeze({
        raw: enumObj,
        getValue: key => enumObj[key],
        getKey: value => reverse[value],
        hasKey: key => key in enumObj,
        hasValue: value => value in reverse,
        entries: () => Object.entries(enumObj),
        keys: () => Object.keys(enumObj),
        values: () => Object.values(enumObj)
    })
}

myUtils.isMoreThanOtherDays = function (day,date) {
    const time = day || 7
    if (!date) return false;
    const now = new Date();
    const target = new Date(date.replace(/-/g, '/'));

    const diffMs = Math.abs(now - target); // 毫秒差
    const otherDaysMs = time * 24 * 60 * 60 * 1000; // x天毫秒数
    return diffMs > otherDaysMs;
}

myUtils.normalizeVal = function (val) {
    if (val === null || val === undefined || val === '') {
        return []
    }
    if (Array.isArray(val)) {
        return val
    }
    if (typeof val === 'string') {
        return val.split(',').map(Number)
    }
    return []
}

myUtils.getReplayType = function (sentenceMarkData) {
    const {videoInfo, uploadFile} = sentenceMarkData
    const sliceType = [null, '', undefined].includes(videoInfo?.videoSliceType) ? uploadFile?.fileSliceType : videoInfo?.videoSliceType

    switch (sliceType) {
        case 0:
            return videoInfo ? 'replayAll' : 'fileAll'
        case 1:
            return videoInfo ? 'replaySection' : 'fileSection'
        case 2:
            return videoInfo ? 'replayShort' : 'fileShort'
    }
}

export default myUtils

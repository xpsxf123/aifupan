const myUtils = {}

/**
 * 加两位小数
 * @param {Object} val
 */
myUtils.addXiaoshu = function (val) {
    let result = val + ''
    if (result.indexOf('.') === -1) {
        return result + '.00'
    } else if (result.length - result.indexOf('.') === 2) {
        return result + '0'
    } else {
        return result
    }
}

/**
 * 保留两位小数
 * @param {Object} val
 */
myUtils.retainDecimals = function (val) {
    let result = val + ''
    let index = result.indexOf('.')
    if (index !== -1) {
        if (result.length > index + 2) {
            result = result.substring(0, index + 3)
        } else if (result.length > index + 1) {
            result = result.substring(0, index + 2)
        }
    }
    return result
}

/**
 * 补零
 * @param {Object} val
 */
myUtils.zeroFill = function (val) {

    if (0 <= val && val < 10) {
        return '0' + val
    }

    return val
}

/**
 * 将毫秒转成时分秒
 * @param {Object} val
 */
myUtils.toformatTime = function (val) {
    val = parseInt(val / 1000)
    let hour = parseInt(val / 3600)
    let minute = parseInt(val % 3600 / 60)
    let second = parseInt(val % 3600 % 60)

    return myUtils.zeroFill(hour) + ':' + myUtils.zeroFill(minute) + ':' + myUtils.zeroFill(second)
}

/**
 * 将毫秒转成分秒
 * @param {Object} val
 */
myUtils.toformatTimeMM_ss = function (val) {
    val = parseInt(val / 1000)
    let minute = parseInt(val / 60)
    let second = parseInt(val % 60)

    return myUtils.zeroFill(minute) + ':' + myUtils.zeroFill(second)
}

/**
 * 将毫秒转成分秒 --中文
 * @param {Object} val
 */
myUtils.toformatTimeMM_ssChinse = function (val) {
    val = parseInt(val / 1000)
    let minute = parseInt(val / 60)
    let second = parseInt(val % 60)

    return myUtils.zeroFill(minute) + '分' + myUtils.zeroFill(second) + '秒'
}

/**
 * 将毫秒转成时分秒-中文
 * @param {Object} val
 */
myUtils.toformatTimeChinse = function (val) {
    val = parseInt(val / 1000)
    let hour = parseInt(val / 3600)
    let minute = parseInt(val % 3600 / 60)
    let second = parseInt(val % 3600 % 60)
    let result = ''
    if (hour) {
        result += hour + '时'
    }
    if (minute) {
        result += minute + '分'
    }
    if (second) {
        result += second + '秒'
    }

    return result
}

/**
 * 将毫秒转成时分-中文
 * @param {Object} val
 */
myUtils.toformatHourChinse = function (val) {
    val = parseInt(val / 1000)
    let hour = parseInt(val / 3600)
    let minute = parseInt(val % 3600 / 60)
    let result = ''
    if (hour) {
        result += hour + '时'
    }
    if (minute) {
        result += minute + '分'
    } else if (minute <= 0) {
        result = '1分'
    }

    return result
}

/**
 * 将时分秒转成秒
 * @param {Object} val
 */
myUtils.toSecond = function (val) {
    let second = 0

    if (val) {
        let arr = val.split(':')
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
            second += parseInt(arr[2])
        }
    }

    return second
}

/**
 * 将Date转成yyyy-MM-dd HH:mm:ss格式
 * @param {Object} val
 */
myUtils.toFormatDate = function (date) {
    const year = date.getFullYear()
    const month = ('0' + (date.getMonth() + 1)).slice(-2)
    const day = ('0' + date.getDate()).slice(-2)
    const hour = ('0' + date.getHours()).slice(-2)
    const minute = ('0' + date.getMinutes()).slice(-2)
    const second = ('0' + date.getSeconds()).slice(-2)

    return year + '-' + month + '-' + day + ' ' + hour + ':' + minute + ':' + second
}

/**
 * 将yyyy-MM-dd HH:mm:ss格式秒
 * @param {Object} val
 */
myUtils.toSecondByDate = function (timeString) {
    const date = new Date(Date.parse(timeString))

    return Math.floor(date.getTime() / 1000)
}


export default myUtils

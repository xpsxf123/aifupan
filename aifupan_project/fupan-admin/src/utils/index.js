import { useUserInfoStore } from '@/store/modules/user.js'

// 手机号正则var
export const phoneReg = (val) => {
    let reg =
        /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/
    return reg.test(val)
}

// 获取当前日期
export const getDate = ({stamp = '', symbol = '-'}) => {
    let date = stamp ? new Date(stamp) : new Date()

    let year = date.getFullYear()
    let month = date.getMonth() + 1
    let day = date.getDate()

    let result = ''

    if (Array.isArray(symbol)) {
        result = `${year}${symbol[0]}${month > 9 ? month : '0' + month}${symbol[1]
        }${day > 9 ? day : '0' + day}${symbol[2]}`
    } else {
        result = `${year}${symbol}${month > 9 ? month : '0' + month}${symbol}${day > 9 ? day : '0' + day
        }`
    }

    return result
}

/**
 * 获取svg图标(id)列表
 */
export function getIconList() {
    const res = []
  const list = document.querySelectorAll('svg symbol')
  for (let i = 0; i < list.length; i++) {
        res.push(list[i].id)
    }

    return res
}

// 中文正则
export const reg_cn = (val) => {
    let reg = new RegExp('[\u4E00-\u9FA5]+')
    return reg.test(val)
}
// 英文正则
export const reg_en = (val) => {
    let reg = new RegExp('[A-Za-z]+')
    return reg.test(val)
}

/**
 * 清除登录信息
 */
export function clearLoginInfo() {
    const userInfoStore = useUserInfoStore()
    userInfoStore.saveLoginResultData()
    userInfoStore.setLoadRouterFlag(false)

}
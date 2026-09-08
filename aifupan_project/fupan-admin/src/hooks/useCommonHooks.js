import { ref } from 'vue'

export function useCommonHooks() {
  // 知识类型选项
  const knowledgeTypeOpt = ref([
    { name: '安全培训', val: '0' },
    { name: '岗前培训', val: '1' },
    { name: '违章培训', val: '2' },
    { name: '法律法规', val: '3' },
    { name: '岗位职责', val: '4' },
    { name: '其它', val: '5' },
  ])

  // 备注删除
  const remarksDelete = () => {
    return new Promise((resolve, reject) => {
      ElMessageBox.prompt('请输入备注', '删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S/,
        inputErrorMessage: '内容不能为空',
      })
        .then(({ value }) => resolve(value))
        .catch(() => {
          ElMessage({
            type: 'info',
            message: '取消输入',
          })
          reject('取消输入')
        })
    })
  }

  // 日期格式化
  const formatDate = (input, format) => {
    let date
    if (typeof input === 'string') {
      date = new Date(input)
    } else if (input instanceof Date) {
      date = input
    } else {
      throw new Error('Invalid input: must be a string or Date object')
    }

    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')

    return format
      .replace(/yyyy/g, year)
      .replace(/MM/g, month)
      .replace(/dd/g, day)
      .replace(/HH/g, hours)
      .replace(/mm/g, minutes)
      .replace(/ss/g, seconds)
  }

  // 转换单位-分钟/KB → 小时/G
  const setConvertUnitValue = (value, code) => {
    if (isNaN(value) || !value) return value
    if (code === 'aiAnalysisTime' || code === 'textExtractionNum') {
      return parseFloat((value / 60).toFixed(2))
    } else if (code === 'storageNum') {
      return parseFloat((value / 1024 / 1024).toFixed(2))
    }
    return value
  }

  // 转换单位-小时/G → 分钟/KB
  const getConvertUnitValue = (value, code) => {
    if (isNaN(value) || !value) return value
    if (code === 'aiAnalysisTime' || code === 'textExtractionNum') {
      return parseFloat((value * 60).toFixed(2))
    } else if (code === 'storageNum') {
      return parseFloat((value * 1024 * 1024).toFixed(2))
    }
    return value
  }

  // 转换单位显示
  const convertUnit = (value, code, unit) => {
    if (isNaN(value) || !value) return `${value}${unit}`
    if (code === 'aiAnalysisTime' || code === 'textExtractionNum') {
      return convertMinutesToHoursAndMinutes(value)
    } else if (code === 'storageNum') {
      return `${parseFloat((value / 1024 / 1024).toFixed(2))}${unit}`
    }
    return `${formatNumber(value)}${unit}`
  }

  // 分钟转小时+分钟
  const convertMinutesToHoursAndMinutes = (minutes) => {
    const hours = Math.floor(minutes / 60)
    const remainingMinutes = minutes % 60
    let result = ''
    if (hours > 0) result += `${hours}小时`
    if (remainingMinutes > 0) result += `${remainingMinutes}分钟`
    return result || '0分钟'
  }

  // 格式化数字，大于 10000 显示万
  const formatNumber = (num) => {
    if (num > 10000) {
      const wan = Math.floor(num / 10000)
      const remainder = num % 10000
      return remainder > 0 ? `${wan}万${remainder}` : `${wan}万`
    }
    return `${num}`
  }

  return {
    knowledgeTypeOpt,
    remarksDelete,
    formatDate,
    setConvertUnitValue,
    getConvertUnitValue,
    convertUnit,
    convertMinutesToHoursAndMinutes,
    formatNumber,
  }
}

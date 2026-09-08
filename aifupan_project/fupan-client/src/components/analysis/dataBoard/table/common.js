import myUtils from '@/utils/utils'
import {isEmpty, isNumber} from 'lodash'
import up from '@/assets/imgs/up.png'

export default {
    components: {},
    props: {},
    data() {
        return {}
    },
    computed: {
        minutes() {
            return (value) => {
                return myUtils.toformatTimeMM_ssChinse(value * 1000)
            }
        },
        isNotNilNumber() {
            return (item, keys) => {
                const [a, b] = keys
                return this.isStrNum(item[a]) && this.isStrNum(item[b]) && item[a] >= 0 && item[b] >= 0
            }
        },
        fnw() {
            return (num, length) => {
                return myUtils.fnw(num, length)
            }
        },
        dataView() {
            return (resData, keys, length) => {
                return myUtils.dataView(resData, keys, length)
            }
        },
        isStrNum() {
            return (value) => {
                return myUtils.isNumberOrNumericString(value)
            }
        },
        goodsDataView() {
            return (resData, keys) => {
                const [start, end] = keys
                const act = this.isStrNum(resData[start]) && this.isStrNum(resData[end])
                if (act) {
                    if (resData[start] === resData[end]) {
                        return `${myUtils.fnw(resData[start] * 100, 2)} %`
                    } else {
                        return `${myUtils.fnw(resData[start] * 100, 2)} % ~ ${myUtils.fnw(resData[end] * 100, 2)} %`
                    }
                } else {
                    return '-'
                }
            }
        },
        isAuthenticated() {
            const videoInfo1 = this.sentenceMarkData?.data1?.videoInfo || {}
            const videoInfo2 = this.sentenceMarkData?.data2?.videoInfo || {}
            return (videoInfo1?.UserId && videoInfo2?.UserId) ? this.$auth(videoInfo1?.UserId) && this.$auth(videoInfo2?.UserId) : true
        }
    },
    watch: {},
    methods: {
        tableRowClassName({row, rowIndex}) {
            if (rowIndex % 2 === 0) {
                return 'warning-row'
            } else if (rowIndex % 2 === 1) {
                return 'success-row'
            }
            return ''
        },
        isMax(key, value) {
            const values = this.tableData.map(item => Number(item[key]))
            const allEqual = values.every(v => v === values[0])
            if (allEqual) return null
            const maxVal = Math.max(...values)
            if (Number(value) === maxVal) {
                return `<img src="${up}" style="width: 12px" alt=""/>`
            } else {
                return null
            }
        },
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
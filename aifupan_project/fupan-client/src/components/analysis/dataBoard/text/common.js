import myUtils from '@/utils/utils'
import {PLATFORM_TYPE_ENUM} from "@/enum";

export default {
    components: {},
    props: {},
    data() {
        return {
            isEmptyType: ''
        }
    },
    computed: {
        isTakeProductData() {
            return (item, key) => {
                // const emptyStatus = this.isEmptyType === 8 ? false : key === 'salesCount' && item.isTakeProduct
                const emptyStatus = key === 'salesCount' && item.isTakeProduct
                return ['watchCount', 'popularityCount'].includes(key) || emptyStatus
            }
        },
        spanStyle() {
            return `flex: 0 0 ${this.span / 24 * 100}%`
            // return (res)=>{
            //     if(res?.length){
            //         return `flex: 0 0 calc(100% - 525px)`
            //     }
            //     return `flex: 0 0 ${this.span / 24 * 100}%`
            // }
        },
        isAuthenticated() {
            const {videoInfo = {}} = this.sentenceMarkData
            return videoInfo?.UserId ? this.$auth([videoInfo?.UserId, videoInfo?.TenantId], 'every') : true
        },
        isStrNum() {
            return (value) => {
                return myUtils.isNumberOrNumericString(value)
            }
        },
        numberData() {
            return (num) => {
                if (this.isStrNum(num)) {
                    return `${(num * 100).toFixed(2)} %`
                }
                return '-'
            }
        },
        getPlatform(){
            return this.sentenceMarkData?.videoInfo?.PlatformType || this.sentenceMarkData?.fileInfo?.platformType
        },
        isDouYin() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.douyin
        },
        isShipinhao(){
            return this.getPlatform == PLATFORM_TYPE_ENUM.shipinhao
        },
    },
    watch: {},
    methods: {
        isNotNilNumber(item, keys) {
            const [a, b] = keys
            return this.isStrNum(item[a]) && this.isStrNum(item[b]) && item[a] >= 0 && item[b] >= 0
        },
        minutes(value) {
            if (value < 0) return '-'
            return myUtils.toformatTimeMM_ssChinse(value * 1000)
        },
        dataView(resData, keys, length) {
            return myUtils.dataView(resData, keys, length)
        },
        singleMergeArrays(arr1, arr2, label, value) {
            return myUtils.mergeArrays(arr1, arr2, label, value)
        },
        computedInteractionPercent(resData) {
            const {dataSourceType} = resData
            const {totalBarrageNum, totalWatchNum} = this.sentenceMarkData || {}
            const p = totalWatchNum > 0 ? (totalBarrageNum / totalWatchNum) * 100 : 0
            const isNum = this.isStrNum(resData.interactionPercent)
            const m = isNum ? resData.interactionPercent : 0
            const result = isNum ? (resData.interactionPercent?.toFixed(2) === '0.00' ? '-' : `${resData.interactionPercent?.toFixed(2)} %`) : '-'
            if ([1].includes(dataSourceType)) {//巨量
                if (isNum && resData.interactionPercent > 0) {
                    return result
                } else {
                    return '-'
                }
            } else {
                if (p > m) {
                    return `${p.toFixed(2)} %`
                } else {
                    return result
                }
            }
        },
        goodsConvert(resData) {
            const isEffective = this.isNotNilNumber(resData, ['goodsConvertRateStart', 'goodsConvertRateEnd'])
            return isEffective ? (resData.goodsConvertRateStart === resData.goodsConvertRateEnd ?
                    `${(resData.goodsConvertRateEnd * 100).toFixed(2)}%` :
                    `${(resData.goodsConvertRateStart * 100).toFixed(2)}% ~ ${(resData.goodsConvertRateEnd * 100).toFixed(2)}%`)
                : '-'
        },
        fansConvert(resData) {
            const isNum = this.isStrNum(resData.convertFanRate)
            if (isNum && resData.convertFanRate > 0) {
                return `${(resData.convertFanRate * 100)?.toFixed(2)} %`
            } else {
                return '-'
            }
        },
        exposureWatchConvert(value) {
            if (!this.isStrNum(value) || value < 0) {
                return '-'
            }
            return `${(Number(value) * 100).toFixed(2)} %`
        },
        assemblyData(resData) {
            this.isEmptyType = resData.dataStatus
            const {videoInfo} = this.sentenceMarkData || {}
            const {dataSourceType} = resData//数据类型 0：第三方，1:巨量百应
            const sliceText = videoInfo?.videoSliceType === 1 && dataSourceType === 0 ? '(原视频)' : ''
            const exposure = dataSourceType === 1 ? [{
                label: '曝光观看率',
                value: this.exposureWatchConvert(resData.showWatchCntRatio)
            }] : []
            this.liveData = {
                dashboard: {
                    watchCount: {
                        title: `流量数据${sliceText}`,
                        childData: [
                            {
                                label: '观看人次',
                                value: myUtils.fnw(resData.totalWatchNum) || '-'
                            }, {
                                label: this.isShipinhao ? '最高在线' :'平均在线',
                                value: myUtils.fnw(resData.averageOnlineNum) || '-'
                            }, {
                                label: '平均停留',
                                value: this.isStrNum(resData.averageResidenceTime) ? this.minutes(resData.averageResidenceTime) : '-'
                            }, {
                                label: '涨粉人数',
                                value: myUtils.fnw(resData.incrementFollowerCount) || '-'
                            },
                            ...exposure
                        ]
                    },
                    salesCount: {
                        title: `销售数据${sliceText}`,
                        isTakeProduct: resData?.isTakeProduct,
                        childData: [
                            {
                                label: '销售额',
                                value: this.dataView(resData, ['volumeStart', 'volumeEnd'])
                            }, {
                                label: '销量',
                                value: this.dataView(resData, ['purchaseCountStart', 'purchaseCountEnd'])
                            }, {
                                label: '客单价',
                                value: this.dataView(resData, ['customerUnitPriceStart', 'customerUnitPriceEnd'], 1)
                            }, {
                                label: 'uv价值',
                                value: this.dataView(resData, ['uvValueStart', 'uvValueEnd'])
                            }, {
                                label: '带货转化率',
                                value: this.goodsConvert(resData)
                            }, {
                                label: '千次成交',
                                value: this.dataView(resData, ['gpmStart', 'gpmEnd'])
                            }
                        ]
                    },
                    popularityCount: {
                        title: `人气数据${sliceText}`,
                        childData: [
                            {
                                label: '转粉率',
                                value: this.fansConvert(resData)
                            }, {
                                label: '整场互动率',
                                value: this.computedInteractionPercent(resData)
                            }
                        ]
                    },
                },
                crowd: {
                    genderPortrait: {
                        title: '性别分布',
                        childData: this.singleMergeArrays(JSON.parse(resData?.watchUserPortrait || '{}')?.genderPortrait || [], JSON.parse(resData?.payUserPortrait || '{}')?.genderPortrait || [], 'label', 'value')
                    },
                    agePortrait: {
                        title: '年龄分布',
                        childData: this.singleMergeArrays(JSON.parse(resData?.watchUserPortrait || '{}')?.agePortrait || [], JSON.parse(resData?.payUserPortrait || '{}')?.agePortrait || [], 'label', 'value')
                    },
                },
                flow: {
                    watchFlowList: {
                        title: '流量结构',
                        childData: this.singleMergeArrays(JSON.parse(resData?.watchFlowList || '[]') || [], JSON.parse(resData?.payFlowList || '[]') || [], 'channelName', 'ratio')
                    },
                }
            }
            return {
                data: this.liveData,
                labels: [
                    {key: 'dashboard', label: '数据看板'},
                    {key: 'crowd', label: '人群结构'},
                    {key: 'flow', label: '数据流量'},
                ]
            }
        }
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

/**
 * @description 在线曲线配置 mixin：负责把在线曲线接口数据转换为 ECharts legend/series 配置，并处理默认显隐策略。
 */
import myUtils from "@/utils/utils";
import {VERSION_TYPE} from "@/enum";

export default {
    data() {
        return {
            legendsList: []
        }
    },
    computed: {
        isShowBtn() {
            return this.liveRoomLegend.every(item => !item)
        },
        isSliceView() {//是否是切片详情页
            return false
        },
        playUrl() {
            return this.sentenceMarkData?.playUrl
        },
        stylesData() {
            return (index) => {
                const startPixel = this.progressData['custom-brush-' + index]?.startPixel || 0
                const endPixel = this.progressData['custom-brush-' + index]?.endPixel || 0
                return {
                    left: startPixel,
                    width: endPixel - startPixel
                }
            }
        },
        getWidth() {
            return this.stylesData(this.currentBrushIndex).width
        },
        getLeft() {
            return this.stylesData(this.currentBrushIndex).left
        },
        secondLeftToTime() {
            const time = Math.round(this.getLeft * this.secondsPerPixel * 1000)
            return myUtils.toformatTime(time)
        },
        secondWidthToTime() {
            const time = Math.round((this.getWidth + this.getLeft) * this.secondsPerPixel * 1000)
            return myUtils.toformatTime(time)
        },
        tipsTime() {
            return this.handIndex === 0 ? this.getLeft : this.getLeft + this.getWidth
        },
        isEmptyChartData() {
            return Object.values(this.chartData).every(this.chartIsEmpty)
        },
        getMaxOnlineLive() {
            return (key) => {
                const lengthChart = Object.keys(this.chartData).length
                return `人气峰值${lengthChart > 1 ? '-' + this.chartData[key]?.anchorInfo?.AnchorName : ''}`
            }
        },
        getBarrageNum() {
            return (key) => {
                const lengthChart = Object.keys(this.chartData).length
                return `弹幕总数${lengthChart > 1 ? '-' + this.chartData[key]?.anchorInfo?.AnchorName : ''}`
            }
        },
        getMaxOnlineNum() {
            return (key) => {
                return myUtils.numberToSting(this.chartData[key]?.maxOnlineNum)
            }
        },
        isShow() {
            if (this.notLoad) {
                return this.showCs
            } else {
                return true
            }
        },
        isWebOnline() {
            return this.targetType === 'webOnline'
        },
        isOnline() {
            return this.targetType === 'online'
        },
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
        isReplay() {
            return this.getReplayType === 'replayAll'
            // && !this.isWebOnline && !this.isOnline
        },
        isSliceAnalysis() {//是否是切片分析详情页
            return this.getReplayType === 'replaySection'
            // && !this.isWebOnline && !this.isOnline
        },
        isSection() {
            return this.getReplayType === 'replaySection'
        },
    },
    methods: {
        /**
         * @description 将曲线 tooltip 中的数值统一格式化为保留 1 位小数，避免浮点精度直接透出。
         * @param {*} value tooltip 当前展示的原始值
         * @returns {string|*} 格式化后的展示值；非数值原样返回
         */
        formatCurveTooltipValue(value) {
            if (value === null || value === undefined || value === '') return '-'
            if (Array.isArray(value)) {
                return this.formatCurveTooltipValue(value[value.length - 1])
            }
            const numberValue = Number(value)
            if (!Number.isFinite(numberValue)) return value
            return numberValue.toFixed(1)
        },
        /**
         * @description 生成数据曲线 tooltip 展示内容，统一处理浮点精度与空值展示。
         * @param {Array} params ECharts tooltip 回调参数
         * @returns {string} tooltip HTML 字符串
         */
        getCurveTooltipHtml(params = []) {
            if (!params.length) return ''
            const title = params[0]?.axisValueLabel || params[0]?.name || ''
            const lines = params
                .filter(item => item && item.seriesName)
                .map(item => {
                    const value = this.formatCurveTooltipValue(item.value)
                    return `${item.marker}${item.seriesName} ${value}`
                })
            return [title, ...lines].join('<br/>')
        },
        /**
         * @description 设置图表配置项。
         * @returns {void}
         */
        setChartOptions() {
            const _this = this
            Object.keys(_this.chartData)
                .sort((a, b) => _this.chartData[a]?.sort - _this.chartData[b]?.sort)
                .forEach(key => {
                    if (_this.chartData[key]) {
                        const onlineDataList = _this.chartData[key]?.onlineDataList?.map(d => d.valueNum)
                        const approachDataList = _this.chartData[key]?.approachDataList?.map(d => d.valueNum)
                        const barrageDataList = _this.chartData[key]?.barrageDataList?.map(d => d.valueNum)
                        const payComboCntDataList = _this.chartData[key]?.payComboCntDataList?.map(d => {
                            // 案例数据
                            if (this.isExample) {
                                let data = this.exampleCurveData.payComboCntDataList;
                                if (data[d.dateTimeNew]) {
                                    return data[d.dateTimeNew]
                                }
                                return d.valueNum;
                            }
                            // 正常数据
                            return d.valueNum;
                        })
                        const exitPeopleDataList = _this.chartData[key]?.exitPeopleDataList?.map(d => d.valueNum) //离场人数折线数据
                        const languageDataList = _this.chartData[key]?.languageDataList?.map(d => d.valueNum) //语数的折线数据
                        const payAmtDataList = _this.chartData[key]?.payAmtDataList?.map(d =>{
                            // 案例数据
                            if (this.isExample) {
                                let data = this.exampleCurveData.payComboCntDataList;
                                if (data[d.dateTimeNew]) {
                                    return data[d.dateTimeNew] * 40
                                }
                                return d.valueNum;
                            }
                            return  d.valueNum
                        }) //销售额的折线数据
                        const canViewRoi = !!this.$store?.getters?.largeEnterprises
                        const qianchuanCostDataList = canViewRoi ? _this.chartData[key]?.qianchuanCostDataList?.map(d => d.valueNum) : [] //投放消耗
                        const netTransactionRoiDataList = canViewRoi ? _this.chartData[key]?.netTransactionRoiDataList?.map(d => d.valueNum) : [] //净成交ROI/净成交额（以后端该字段实际语义为准）
                        const followAnchorUcntDataList = _this.chartData[key]?.followAnchorUcntDataList?.map(d => d.valueNum)
                        const onlineSecond = _this.chartData[key]?.onlineDataList?.map(d => d.dateTimeNew / 1000)
                        const videoDuration = onlineSecond[onlineSecond.length - 1]
                        if (videoDuration > 0) {
                            if (onlineDataList && onlineDataList.length) {
                                const anchorName = _this.chartData[key]?.anchorInfo?.AnchorName
                                // const time = _this.chartData[key]?.videoInfo?.Duration
                                const startTime = _this.chartData[key]?.videoInfo?.StartTime?.substring(0, 16)
                                const liveName = `${anchorName}-(${startTime})`
                                _this.liveRoom.push({
                                    key: key,
                                    videoInfo: _this.chartData[key]?.videoInfo,
                                    anchorInfo: _this.chartData[key]?.anchorInfo,
                                    liveName: liveName,
                                    list: onlineDataList,//在线人数
                                    approachList: approachDataList,//进场人数
                                    barrageDataList: barrageDataList,//互动量
                                    payComboCntDataList: payComboCntDataList,//成交量
                                    followAnchorUcntDataList: followAnchorUcntDataList,//涨粉数
                                    exitPeopleDataList: exitPeopleDataList,//离场人数
                                    languageDataList: languageDataList,//语数
                                    payAmtDataList: payAmtDataList,//销售额
                                    qianchuanCostDataList: qianchuanCostDataList,//投放消耗
                                    netTransactionRoiDataList: netTransactionRoiDataList,//净成交ROI
                                    blessBagList: _this.chartData[key]?.blessBagList,//福袋
                                    duration: videoDuration,
                                    initSecondsRange: this.initSecondsRange[key]
                                })
                            }
                        }
                    }
                })
            const canViewRoi = !!this.$store?.getters?.largeEnterprises
            const legendYList = [{
                name: '语速',
                value: 'languageDataList'
            }, {name: '互动量', value: 'barrageDataList'}, {
                name: '成交量',
                value: 'payComboCntDataList'
            }, {name: '销售额', value: 'payAmtDataList'}, ...(canViewRoi ? [{
                name: '投放消耗',
                value: 'qianchuanCostDataList'
            }, {
                name: '净成交ROI',
                value: 'netTransactionRoiDataList'
            }] : []), {name: '涨粉数', value: 'followAnchorUcntDataList'}]
            const legendList = ['在线人数', '进场人数', '离场人数']
            const yAxisList = [{
                name: '在线人数',
                type: 'line',
                areaStyle: {opacity: 0.1},
                showSymbol: false,
                itemStyle: {color: this.chartColor[0]},
                lineStyle: {width: 1},
                data: this.liveRoom[0]?.list
            }, {
                name: '进场人数',
                type: 'line',
                areaStyle: {opacity: 0.1},
                lineStyle: {width: 1},
                itemStyle: {color: this.chartColor[1]},
                showSymbol: false,
                data: this.liveRoom[0]?.approachList
            }, {
                name: '离场人数',
                type: 'line',
                areaStyle: {opacity: 0.1},
                lineStyle: {width: 1},
                itemStyle: {color: this.chartColor[2]},
                showSymbol: false,
                data: this.liveRoom[0]?.exitPeopleDataList
            }]
            legendYList.forEach((item, index) => {
                if (this.liveRoom[0]?.[item.value]?.length) {
                    legendList.push(item.name)
                    yAxisList.push({
                        name: item.name,
                        type: 'line',
                        yAxisIndex: 1,
                        areaStyle: {opacity: 0.1},
                        lineStyle: {width: 1},
                        itemStyle: {color: this.chartColor[index + 3]},
                        showSymbol: false,
                        data: this.liveRoom[0]?.[item.value]
                    })
                }
            })
            const legendContrastList = []
            const yAxisContrastList = []
            if (Object.keys(this.chartData).length > 1) {
                this.liveRoom.forEach((item, index) => {
                    legendContrastList.push((item.liveName ?? `直播间${index + 1}`) + '-在线')
                    yAxisContrastList.push({
                        name: (item.liveName ?? `直播间${index + 1}`) + '-在线',
                        type: 'line',
                        data: item.list,
                        areaStyle: {opacity: 0.1},
                        itemStyle: {color: this.chartColor[2 * index]},
                        symbolSize: 0,
                        lineStyle: {
                            width: 1
                        }
                    })
                    if (item.payComboCntDataList) {
                        legendContrastList.push((item.liveName ?? `直播间${index + 1}`) + '-成交量')
                        yAxisContrastList.push({
                            name: (item.liveName ?? `直播间${index + 1}`) + '-成交量',
                            type: 'line',
                            data: item.payComboCntDataList,
                            areaStyle: {opacity: 0.1},
                            itemStyle: {color: this.chartColor[2 * index + 1]},
                            symbolSize: 0,
                            yAxisIndex: 1,
                            lineStyle: {
                                width: 1
                            }
                        })
                    }
                })
            }
            const legendsList = Object.keys(this.chartData).length > 1 ? legendContrastList : legendList
            this.legendsList = legendsList
            const legends = [{
                data: legendsList.slice(0, 4),
                left: "4%",
                top: 0, // 每行间隔30px
            }, {
                data: legendsList.slice(4, legendsList.length),
                left: "4%",
                top: 30, // 每行间隔30px
            }];
            const option = {
                legend: legends,
                tooltip: {
                    trigger: 'axis',
                    formatter: function (params) {
                        return _this.getCurveTooltipHtml(params)
                    }
                },
                xAxis: {
                    type: 'category',
                    axisLabel: {interval: 'auto'},
                    boundaryGap: false,
                    data: this.xAxisData,
                },
                yAxis: [{
                    type: 'value',
                    // name: '人数',
                    position: 'left',
                    boundaryGap: [0, 0.1],
                    axisLabel: {
                        formatter: function (value) {
                            let v = parseInt(value)
                            return myUtils.numberToSting(v)
                        }
                    }
                }, {
                    type: 'value',
                    // name: '互动',
                    position: 'right',
                    alignTicks: true,
                    nameTextStyle: {
                        padding: [0, 0, 0, 30],
                        align: 'center'
                    },
                    axisLabel: {
                        formatter: function (value) {
                            const num = Number(value)
                            if (!Number.isFinite(num)) return value
                            if (!Number.isInteger(num) && Math.abs(num) < 100) {
                                return num.toFixed(2)
                            }
                            return myUtils.numberToSting(num)
                        }
                    }
                }],
                grid: {
                    bottom: 12,
                    left: '5%',
                    right: 65,
                    top: legendsList.length > 4 ? 100 : 40 + this.liveRoom.length * 20,
                    containLabel: true
                },
                series: Object.keys(this.chartData).length > 1 ? yAxisContrastList : yAxisList
            }

            this.myChart.setOption(option)

            // 默认不显示
            this.myChart?.dispatchAction({
                type: 'legendUnSelect',
                name: '进场人数',
            })
            this.myChart?.dispatchAction({
                type: 'legendUnSelect',
                name: '涨粉数',
            })
            this.myChart?.dispatchAction({
                type: 'legendUnSelect',
                name: '离场人数',
            })
            this.myChart?.dispatchAction({
                type: 'legendUnSelect',
                name: '语速',
            })

            this.myChart.on('legendselectchanged', function (params) {
                const liveRoomLegend = []
                if (Object.keys(_this.chartData)?.length > 1) {
                    for (let key in params.selected) {
                        liveRoomLegend.push(params.selected[key])
                    }
                } else {
                    liveRoomLegend.push(params.selected['在线人数'])
                }
                _this.liveRoomLegend = liveRoomLegend
                if (this.versionType === VERSION_TYPE.AGENT) _this.setCustomPositions(_this.liveRoom[0])

            })
			if (this.versionType === VERSION_TYPE.AGENT)  this.setCustomPositions(_this.liveRoom[0])

            this.myChart.getZr().on('click', function (params) {
                if (params.target?.type === 'rect') return

                const pointInPixel = [params.offsetX, params.offsetY]
                let pointInGrid = _this.myChart.convertFromPixel({seriesIndex: 0}, pointInPixel)
                let xIndex = pointInGrid[0]
                const time = _this.xAxisData[xIndex]
                if (time && _this.isReplay) {
                    _this.$emit('playerReadied', time)
                }
            })
        },
        showBrush() {
            this.$nextTick(() => {
                this.setInitSecondsRange(this.initSecondsRange || {})
                this.$nextTick(() => {
                    const hasVideoInfo = Object.keys(this.sentenceMarkData).includes('videoInfo')
                    if (!hasVideoInfo) {
                        ['进场人数', '互动量', '成交量', '涨粉数'].forEach(_item => {
                            this.myChart?.dispatchAction({
                                type: this.aIAnalysis ? 'legendUnSelect' : 'legendSelect',
                                name: _item,
                            })
                        })
                    }
					if (this.versionType === VERSION_TYPE.AGENT) this.setCustomPositions(this.liveRoom[0])

                    this.$nextTick(() => {
                        if (this.aIAnalysis || this.sliceAnalysisMode) this.setupBrushes()
                    })
                })
            })
        },
        chartIsEmpty(value) {
            if (value === null || value === undefined) return true
            if (typeof value === 'object') {
                if (Array.isArray(value)) return value.length === 0
                return Object.keys(value).length === 0
            }
            return false
        },
    },
}



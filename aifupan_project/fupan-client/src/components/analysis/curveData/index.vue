<!--
@description 在线曲线组件：负责单场/对比场景的在线曲线拉取、绘制、切片框选与福袋/切片辅助展示。
-->
<template>
    <div class="chart-wrapper" v-if="!isEmptyChartData" ref="resizeTarget"
         :style="{height:isReplay&&!isContrast&&!isOnline&&!isWebOnline&&versionType === VERSION_TYPE.AGENT?'350px':'auto'}">
        <!--切片模式-->
        <template v-if="isReplay&&!isContrast&&!isWebOnline && !isOnline && versionType === VERSION_TYPE.AGENT">
            <div class="curve-data-item flex-ai-c text-center" style="right: 170px" v-show="!sliceAnalysisMode">
                <afp-button class="ai-Analysis-mode" size="small" @click="sliceAnalysisClick"
                            v-if="!isShowBtn && !notBtn && !isExample && showSlice && !aIAnalysis && playUrl">
                    进入切片
                </afp-button>
            </div>
            <div class="exit-action" v-if="!isShowBtn" style="right: 95px;margin-top:0">
                <afp-button class="ai-Analysis-mode" size="small" @click="selectSliceAnalysisClick"
                            v-if="sliceAnalysisMode">
                    选择切片
                </afp-button>
                <afp-button v-if="sliceAnalysisMode" class="ai-Analysis-mode" size="small"
                            @click="sliceAnalysisClick">
                    退出
                </afp-button>
            </div>
        </template>
        <!--AI分析模式-->
        <div class="curve-data-item flex-ai-c text-center" v-show="!aIAnalysis">
            <afp-button class="ai-Analysis-mode" size="small" @click="aiAnalysisClick"
                        v-if="!isShowBtn && !notBtn && !isExample && !sliceAnalysisMode && !isSliceAnalysis && versionType === VERSION_TYPE.AGENT">
                进入AI分析
            </afp-button>
            <div v-for="item in liveRoom" :key="item.key">
                <div class="font-s12 text-color4"> {{ getMaxOnlineLive(item.key) }}</div>
                <div class="text-colorMain">{{ getMaxOnlineNum(item.key) || 0 }}</div>
            </div>
            <div v-if="chartData?.data1?.totalBarrageNum>0&&!isContrast">
                <div class="font-s12 text-color4">{{ getBarrageNum('data1') }}</div>
                <div class="text-colorMain">{{ chartData?.data1?.totalBarrageNum || 0 }}</div>
            </div>
        </div>
        <div class="exit-action" v-if="!isShowBtn">
            <el-button v-if="aIAnalysis" class="ai-Analysis-btn shaking-element" type="primary" size="small"
                       @click="aiAnalysisParagraph"/>
            <afp-button v-if="aIAnalysis" class="ai-Analysis-mode" size="small"
                        @click="aiAnalysisClick">
                退出
            </afp-button>
        </div>
        <div v-if="isShow" class="chart-container" ref="chartContainer"></div>
        <div v-if="aIAnalysis||sliceAnalysisMode" class="aIAnalysis-action">
            <div v-if="showTips" class="tips-time"
                 :style="{ bottom: currentBrushIndex * -16 - 20 + 'px', left: tipsTime + 'px' }">
                <div>段落开始时间 {{ secondLeftToTime }}</div>
                <div>段落结束时间 {{ secondWidthToTime }}</div>
            </div>
            <div v-for="(item, index) in liveRoom" :key="item.key" :id="'custom-brush-' + index" :class="{
                'custom-brush':true,
                'custom-brush-slice-view':isSliceView,
                'custom-brush-0':index===0&&legendsList.length>4,
                'custom-brush-1':index===1&&legendsList.length>4,
            }"
                 :style="{ background: sliceAnalysisMode?'rgba(244,157,44,0.3)':backgroundColor[index], zIndex: 11 - index, marginTop: (2 * index + 1) * 8 + (legendsList.length>4?48:20) + 'px' }"
                 v-show="liveRoomLegend[index]">
                <div
                    :class="{'brush-line':true,'brush-line-slice-mode':sliceAnalysisMode,'brush-line-slice':isSliceAnalysis, 'brush-line-slice-view':isSliceView}"
                    :style="{ '--after-width': `${stylesData(index).left}px` }"
                    :id="'brush-start-' + index">
                    <div class="brush-handle">
                        <i class="el-icon-arrow-left icon"></i>
                    </div>
                </div>
                <div
                    :class="{'brush-line':true,'brush-line-slice-mode':sliceAnalysisMode,'brush-line-slice':isSliceAnalysis,'brush-line-slice-view':isSliceView}"
                    :style="{ '--after-width': `${chartWidth - stylesData(index).width - stylesData(index).left}px` }"
                    :id="'brush-end-' + index">
                    <div class="brush-handle">
                        <i class="el-icon-arrow-right icon"></i>
                    </div>
                </div>
                <div :class="{progress:true,'progress-view' :isSliceView}"
                     :style="{ background: sliceAnalysisMode?'#F49D2C':chartColor[index], left: stylesData(index).left + 'px', width: stylesData(index).width + 'px' }">
                </div>
            </div>
        </div>
        <div class="flex items-center" v-if="!isContrast&&!hideLucky && versionType === VERSION_TYPE.AGENT">
            <div class="flex-jcai-sb flex-jc-s" style="text-align: right;margin-left: 10px;font-weight: 500">
                <img src="@/assets/imgs/lucky_bag.png" alt="" style="height:15px;">
                <span>福袋</span>
            </div>
            <div id="lucky-bag" class="lucky-bag">
                <div v-for="(item,index) in blessBagList" :key="item.relativeTime">
                    <div class="tool-com lucky-bag-item"
                         :style="{left:item.relativeTime/(secondsPerPixel*1000) + 'px'}"
                         @mouseleave="()=>currentLuckyBagIndex=-1"
                         @mousemove="()=>setCurrentToolPosition(item.relativeTime/(secondsPerPixel*1000),index,'lucky_bag','currentLuckyBagIndex')"></div>
                    <LuckyBag :style="{
                            position:'absolute',
                            bottom: '30px',
                            left: computePopoverPosition.left
                        }" ref="lucky_bag"
                              :luckyBagData="blessBagList[currentLuckyBagIndex]"
                              v-if="currentLuckyBagIndex===index"/>
                </div>
            </div>
        </div>
        <div class="flex items-center"
             :style="{marginTop: '6px',
                 visibility:(isOnline||isWebOnline||isSection)?'hidden':'visible',
                 height:(isOnline||isWebOnline||isSection)?0:'auto'}"
             v-if="!isContrast&&!hideLucky&& versionType === VERSION_TYPE.AGENT">
            <div style="text-align: right;margin-left: 25px;font-weight: 500">
                <span>切片</span>
            </div>
            <div id="slice-analysis" class="slice-analysis flex">
                <div v-for="(item,index) in sliceVideos" :key="index">
                    <div class="tool-com slice-analysis-item"
                         :style="{ background:item.sliceType==0?'#F49D2C':'#636CBD',
                          width:(item.endMillisecond-item.startMillisecond)/(secondsPerPixel*1000)+'px',
                          left:item.startMillisecond/(secondsPerPixel*1000) + 'px'}"
                         @mouseleave="()=>leaveSliceAnalysis()"
                         @mousemove="()=>setCurrentSliceAnalysisPosition(item.startMillisecond/(secondsPerPixel*1000),index,'slice_analysis','currentSliceIndex')"></div>

                    <div @mouseenter="enterPopover" @mouseleave="leavePopover"
                         v-if="currentSliceIndex===index">
                        <SliceAnalysisSlider :style="{
                            position:'absolute',
                            bottom: '20px',
                            left: computePopoverPosition.left
                        }" ref="slice_analysis" :sentenceMarkData="sentenceMarkData"
                                             :sliceVideo="sliceVideos[currentSliceIndex]"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div v-else style="min-height: 295px;" class="flex-jc-c h100 flex-ai-c">
        <div v-if="loadingImg" style="text-align: center">
            <img src="@/assets/imgs/chartEmpty.png" style="max-height: 160px" alt="" srcset="">
            <div style="text-align: center" v-if="Object.keys(this.chartData)?.length">
                {{ versionType === VERSION_TYPE.PURE ? '需要在直播间列表开启【在线人数】监控开关按钮后才能录制在线曲线' : '暂无数据' }}
            </div>
            <div style="text-align: center" v-else>网络问题导致数据抓取错误，请重启网络后，重新录制</div>
        </div>
    </div>
</template>

<script>
/**
 * @description 在线曲线组件：根据详情来源切换 Java/C# 曲线接口，并承接图表、切片与 AI 分析联动。
 */
import * as echarts from 'echarts'
import myUtils from '/src/utils/utils'
import {cloneDeep, isEmpty, isEqual, sumBy} from 'lodash'
import LuckyBag from './luckyBag.vue'
import SliceAnalysisSlider from './sliceAnalysisSlider.vue'
import sliceAnalysisSlider from './sliceAnalysisSlider.js'
import exampleMixin from '@/mixins/exampleMixin.js'
import indexMixin from './index'
import {VERSION_TYPE} from "@/enum";

export default {
    components: {LuckyBag, SliceAnalysisSlider},
    props: {
        ai: {
            type: Boolean,
            default: false
        },
        sentenceMarkData: {
            type: Object,
            default: {}
        },
        targetType: {
            type: String,
            default: ''
        },
        isContrast: {
            type: Boolean,
            default: false
        },
        hideLucky: {
            type: Boolean,
            default: false
        },
        notLoad: {
            type: Boolean,
            default: false
        },
        notBtn: {
            type: Boolean,
            default: false
        },
        showSlice: { //是否显示切片
            type: Boolean,
            default: false
        },
        curveStep: {
            type: Number,
            default: 1
        }
    },
    mixins: [sliceAnalysisSlider, exampleMixin, indexMixin],
    data() {
        return {
            VERSION_TYPE,
            myChart: null,//echarts实例
            sliceAnalysisMode: false,//切片模式
            aIAnalysis: false,//AI分析模式
            chartColor: ['#16CC96', '#FFD480', '#84BFFA', '#D684FD', '#EE7332', '#FFD10A', '#785DB0', '#4F7CFF', '#46C2A9', '#FF8D6B'],//如果有多个对比，直接添加色值，与backgroundColor数量保持一致
            backgroundColor: ['#94E5D5', '#F4EDC4', '#C2DAF3', '#E6E7FF', '#ECD0C0', '#F1E4AA', '#EEEBF5', '#DDE6FF', '#D7F3EC', '#FFE2D8'],//框选的背景颜色，如果有多个对比，直接添加色值，与chartColor
            initSecondsRange: {},
            chartData: {},//数据
            xAxisData: [], // X轴数据
            liveRoom: [], // 存储多个直播间的数据
            brushConfigs: {}, // 存储刷选控件的配置信息
            liveRoomLegend: [true, true],
            selectedRangeSecond: [],
            // selectedRangeDate: [],
            progressData: {},
            showCs: false,
            loadingImg: false,
            showTips: false,//是否显示开始/结束时间
            handIndex: -1,//当前选中的是开始还是结束 0开始 1结束
            currentBrushIndex: -1,//当前选中的直播间小标
            secondsPerPixel: 0.0000001,// 计算每一像素的秒数 不能为0
            observer: null,
            isFirst: true,
            currentLuckyBagIndex: -1,//福袋位置
            blessBagList: [],
            chartWidth: 0,//图表的宽度
            computePopoverPosition: {
                left: 0
            },
        }
    },
    watch: {
        sentenceMarkData: {
            handler(newVal, oldVal) {
                if (this.notLoad) return
                if (!isEqual(newVal, oldVal) && !isEmpty(newVal)) {
                    this.liveRoom = []
                    this.getDataAndDraw(newVal)
                }
            },
            immediate: true
        },
        isEmptyChartData: {
            handler(newVal, oldVal) {
                if (!newVal) {
                    this.$nextTick(() => {
                        this.observerUpdate()
                    })
                }
            }
        },
        curveStep(newVal, oldVal) {
            if (Number(newVal) === Number(oldVal)) return
            if (isEmpty(this.sentenceMarkData)) return
            this.reloadCurveData()
        }
    },
    computed: {
        sliceVideos() {
            return this.sentenceMarkData?.videoInfo?.sliceList || []
        },
        versionType() {
            return this.$store.getters.getVersionType
        }
    },
    mounted() {
    },
    methods: {
        /**
         * @description 清理当前图表实例与缓存数据，供分钟颗粒切换后重绘使用。
         * @returns {void}
         */
        resetCurveChartState() {
            window.removeEventListener('resize', this.handleResize)
            if (this.myChart) {
                this.myChart.dispose()
                this.myChart = null
            }
            this.chartData = {}
            this.xAxisData = []
            this.liveRoom = []
            this.blessBagList = []
            this.progressData = {}
            this.brushConfigs = {}
            this.chartWidth = 0
            this.currentLuckyBagIndex = -1
            this.currentBrushIndex = -1
            this.showTips = false
        },
        /**
         * @description 依据当前分钟颗粒重新拉取并绘制数据曲线。
         * @returns {void}
         */
        reloadCurveData() {
            if (isEmpty(this.sentenceMarkData)) return
            this.aIAnalysis = false
            this.sliceAnalysisMode = false
            this.resetCurveChartState()
            this.getDataAndDraw(this.sentenceMarkData)
        },
        // 绘制图表 rangeList = {data1: [0, 600], data2: [0, 600]}
        drawData(rangeList, showCs) {
            this.showCs = typeof showCs !== 'undefined' ? showCs : !this.showCs
            this.aIAnalysis = false
            this.resetCurveChartState()
            this.initSecondsRange = rangeList || {}
            this.$nextTick(() => {
                if (this.showCs) {
                    this.getDataAndDraw(this.sentenceMarkData || {})
                }
            })
        },
        observerUpdate() {
            const el = this.$refs.resizeTarget
            if (el) {
                this.observer = new ResizeObserver((entries) => {
                    if (!this.isFirst) {
                        for (let entry of entries) {
                            this.handleResize()
                        }
                    }
                    this.isFirst = false
                })
                this.observer.observe(el)
            }
        },
        setCurrentToolPosition(left, index, ref, key) {
            this[key] = index
            this.$nextTick(() => {
                const rect = this.$refs[ref]?.[0]?.$el?.getBoundingClientRect()
                const viewportWidth = this.$refs.chartContainer?.offsetWidth

                let newLeft = left + 15
                const popupWidth = rect.width
                // 如果右边超出窗口
                if (newLeft + popupWidth + 100 > viewportWidth) {
                    newLeft = left - popupWidth
                }

                this.computePopoverPosition = {
                    left: newLeft + 'px'
                }
            })
        },
        //设置默认选中范围
        setInitSecondsRange(rangeList = {}) {
            const rang = {}
            const sliceVideoInfo = this.chartData.data1?.videoInfo?.videoSliceInfo || {};
            const {startMillisecond, endMillisecond} = sliceVideoInfo
            const sliceStartTime = (startMillisecond / 1000).toFixed(0)
            const sliceEndTime = (endMillisecond / 1000).toFixed(0)
            for (const key in this.chartData) {
                if (this.chartData[key]) {

                    const isEmptyRange = isEmpty(rangeList);
                    const isReplay = this.getReplayType === 'replaySection';

                    const propMaxSecond = isEmptyRange
                        ? (isReplay ? (sliceEndTime || 600) : 600)
                        : (rangeList[key][1] || 0);

                    const startSecond = isEmptyRange
                        ? (this.isSliceAnalysis && isReplay ? (sliceStartTime || 0) : 0)
                        : (rangeList[key][0] || 0);

                    let onlineSecond = this.chartData[key]?.onlineDataList?.map(d => d.dateTimeNew / 1000)
                    const maxSecond = onlineSecond[onlineSecond.length - 1] || 0
                    const endSecond = isReplay ? propMaxSecond : (propMaxSecond - startSecond > 3600 ? 3600 : propMaxSecond)
                    if (isReplay) {
                        // 如果是复盘，并且有切片信息，则默认选中切片范围
                        rang[key] = [startSecond, endSecond]
                    } else {
                        rang[key] = [startSecond, maxSecond > endSecond ? endSecond : maxSecond]
                    }
                }
            }
            this.initSecondsRange = rang
        },
        //进入/退出AI切片
        sliceAnalysisClick() {
            this.sliceAnalysisMode = !this.sliceAnalysisMode
            this.showBrush()
        },
        //进入/退出AI分析模式
        aiAnalysisClick() {
            this.aIAnalysis = !this.aIAnalysis
            this.showBrush()
        },
        //分析该段落
        aiAnalysisParagraph() {
            const rangeSecond = {}
            this.selectedRangeSecond?.forEach((item) => {
                rangeSecond[item.key] = item.secondsRange
            })
            this.$emit('brushChange', rangeSecond)
        },
        //选择切片
        selectSliceAnalysisClick() {
            const rangeSecond = {}
            this.selectedRangeSecond?.forEach((item) => {
                rangeSecond[item.key] = item.secondsRange
            })
            this.$emit('selectSliceAnalysis', rangeSecond)
        },
        // 初始化ECharts实例
        initChart() {
            const dom = this.$refs.chartContainer
            this.myChart = echarts.init(dom, null, {
                renderer: 'canvas',
                useDirtyRect: false,
            })
        },
        // 获取图表中的X坐标位置
        getChartXPosition(index) {
            return this.myChart.convertToPixel({gridIndex: 0}, [index, 0])[0]
        },
        // 计算对应曲线的长度
        getBrushWidth(list) {
            let startPos = this.getChartXPosition(0)
            let endPos = this.getChartXPosition(list.length - 1)
            this.chartWidth = endPos - startPos
            return [startPos, endPos]
        },
        // 计算每一像素的秒数
        computeSecondsPerPixel(liveRoomInfo) {
            const {list, duration} = liveRoomInfo
            const [startPos, endPos] = this.getBrushWidth(list)
            let brushWidth = endPos - startPos
            this.secondsPerPixel = duration / brushWidth
        },
        //福袋位置设置/切片位置设置
        setCustomPositions(liveRoomInfo) {
            if (!liveRoomInfo) return;
            this.computeSecondsPerPixel(liveRoomInfo)
            if (this.isContrast || this.hideLucky) return
            const {list, blessBagList, duration} = liveRoomInfo
            this.blessBagList = blessBagList
            if (!list.length) return
            // 获取图表中的X坐标位置
            const customBrushLuckyBag = document.getElementById('lucky-bag')
            const customSliceAnalysis = document.getElementById('slice-analysis')
            const [startPos, endPos] = this.getBrushWidth(list)
            let brushWidth = endPos - startPos
            const customStyles = {
                position: 'absolute',
                left: `${startPos}px`,
                width: `${brushWidth}px`,
                height: '1px',
                background: 'repeating-linear-gradient( 90deg,#A9B5BF, #A9B5BF 5px,transparent 5px,transparent 8px)'
            }
            Object.assign(customBrushLuckyBag.style, customStyles)
            Object.assign(customSliceAnalysis.style, customStyles)
        },
        // 初始化所有刷选控件
        setupBrushes() {
            this.brushConfigs = {}
            const deepLiveRoom = cloneDeep(this.liveRoom)
            deepLiveRoom.forEach(item => {
                item['initSecondsRange'] = this.initSecondsRange[item.key]
            })
            this.liveRoom = deepLiveRoom
            this.$nextTick(() => {
                deepLiveRoom.forEach((item, index) => {
                    this.setupBrush({
                        brushId: `custom-brush-${index}`,
                        ...item
                    })
                })
            })
        },
        // 设置单个筛选控件
        setupBrush(liveRoomInfo) {
            const {brushId, list, videoInfo, anchorInfo, initSecondsRange, duration} = liveRoomInfo
            const initialStartSecond = initSecondsRange[0], initialEndSecond = initSecondsRange[1]
            const customBrush = document.getElementById(brushId)
            const brushStart = document.getElementById(`brush-start-${brushId.split('-')[2]}`)

            const brushEnd = document.getElementById(`brush-end-${brushId.split('-')[2]}`)
            const handleStart = brushStart.querySelector('.brush-handle')
            const handleEnd = brushEnd.querySelector('.brush-handle')

            let isDragging = false
            let activeHandle = null
            let startX = 0
            // 存储刷选控件的相对比例（0到1之间）
            this.brushConfigs[brushId] = {
                startRatio: initialStartSecond / duration, // 初始起点比例
                endRatio: initialEndSecond / duration,     // 初始终点比例
            }
            // 获取日期范围的功能先留着
            // // 从像素坐标转换为图表索引
            // const getChartIndexFromPixel = (pixelX) => {
            //     const coord = this.myChart.convertFromPixel({ gridIndex: 0 }, [pixelX, 0]);
            //     return Math.round(Math.max(0, Math.min(coord[0], list.length - 1)));
            // };

            // // 获取当前选中的数据范围(日期)
            // const getSelectedData = () => {
            //     const startPos = parseInt(brushStart.style.left || 0) + parseInt(customBrush.style.left || 0);
            //     const endPos = parseInt(brushEnd.style.left || 0) + parseInt(customBrush.style.left || 0);
            //     const newStartIndex = getChartIndexFromPixel(startPos);
            //     const newEndIndex = getChartIndexFromPixel(endPos);

            //     const selectedData = list.slice(newStartIndex, newEndIndex + 1);
            //     const selectedXAxis = this.xAxisData.slice(newStartIndex, newEndIndex + 1);

            //     const newItem = {
            //         xAxis: selectedXAxis,
            //         videoInfo,
            //         secondsRange: selectedData,
            //     }
            //     const deepData = cloneDeep(this.selectedRangeDate);

            //     const index = deepData.findIndex(item => item?.videoInfo?.VideoId === newItem.videoInfo?.VideoId)
            //     if (index !== -1) {
            //         deepData[index] = newItem;
            //     } else {
            //         deepData.push(newItem);
            //     }
            //     this.selectedRangeDate = deepData
            //     // return { xAxis: selectedXAxis, data: selectedData };
            // };

            // 获取当前选中的数据范围(时长)
            const getSelectedSeconds = () => {
                const [startPos, endPos] = this.getBrushWidth(list)
                let brushWidth = endPos - startPos

                // 根据相对比例计算新地像素位置
                const startPixel = this.brushConfigs[brushId].startRatio * brushWidth
                const endPixel = this.brushConfigs[brushId].endRatio * brushWidth

                const newItem = {
                    key: liveRoomInfo.key,
                    videoInfo,
                    anchorInfo,
                    secondsRange: [Math.round(startPixel * this.secondsPerPixel), Math.round(endPixel * this.secondsPerPixel)],
                }
                const deepData = cloneDeep(this.selectedRangeSecond)

                const index = deepData.findIndex(item => item?.videoInfo?.VideoId === newItem.videoInfo?.VideoId)
                if (index !== -1) {
                    deepData[index] = newItem
                } else {
                    deepData.push(newItem)
                }
                this.selectedRangeSecond = deepData
            }

            // 更新筛选控件的位置
            const updateBrush = () => {
                const [startPos, endPos] = this.getBrushWidth(list)
                let brushWidth = endPos - startPos
                //ai分析模式
                Object.assign(customBrush.style, {
                    position: 'absolute',
                    left: `${startPos}px`,
                    width: `${brushWidth}px`,
                    height: '5px',
                })

                // 根据相对比例计算新地像素位置
                const startPixel = this.brushConfigs[brushId].startRatio * brushWidth
                const endPixel = this.brushConfigs[brushId].endRatio * brushWidth
                brushStart.style.left = `${startPixel}px`
                brushEnd.style.left = `${endPixel}px`

                // 更新进度条
                const progressDataObj = cloneDeep(this.progressData)
                if (!progressDataObj[brushId]) progressDataObj[brushId] = {}
                progressDataObj[brushId]['startPixel'] = startPixel
                progressDataObj[brushId]['endPixel'] = endPixel
                this.progressData = {...progressDataObj}

                //初始化/窗口变化的时候获取一次范围
                getSelectedSeconds()
                // getSelectedData()
            };
            if (this.getReplayType === 'replayAll' || this.isContrast) {//只有AI复盘才能拖拽时间选择
                // 添加拖动开始事件监听
                [handleStart, handleEnd].forEach((handle, index) => {
                    handle.addEventListener('mousedown', (e) => {
                        this.currentBrushIndex = liveRoomInfo.brushId.split('-')[2]
                        this.handIndex = index
                        this.showTips = true
                        isDragging = true
                        activeHandle = handle.parentElement
                        startX = e.clientX
                        e.preventDefault()
                    })
                })
            }
            // 鼠标移动事件处理
            const handleMouseMove = (e) => {
                if (!isDragging || !activeHandle) return

                const dx = e.clientX - startX
                let currentPos = parseFloat(activeHandle.style.left || 0) + dx
                const minPos = 0
                const maxPos = parseFloat(customBrush.style.width)

                const otherPos = activeHandle === brushStart
                    ? parseFloat(brushEnd.style.left)
                    : parseFloat(brushStart.style.left)

                let newPos = activeHandle === brushStart
                    ? Math.max(minPos, Math.min(currentPos, otherPos - 15))
                    : Math.min(maxPos, Math.max(currentPos, otherPos + 15))

                // 计算当前宽度
                const currentWidth = activeHandle === brushStart
                    ? otherPos - newPos
                    : newPos - otherPos
                const timeLimit = this.sliceAnalysisMode ? this.sentenceMarkData?.videoInfo?.durationTime : 3600
                const secondsToPerPixel = parseFloat(timeLimit / this.secondsPerPixel)
                if (currentWidth > secondsToPerPixel) {
                    if (activeHandle === brushStart) {// 如果是起始手柄拖动，且宽度超限，则限制在最大允许位置
                        newPos = otherPos - secondsToPerPixel
                    } else { // 如果是结束手柄拖动，且宽度超限，则限制在最大允许位置
                        newPos = otherPos + secondsToPerPixel
                    }
                }
                activeHandle.style.left = `${newPos}px`

                // 更新进度条
                const progressDataObj = cloneDeep(this.progressData)
                progressDataObj[brushId][activeHandle === brushStart ? 'startPixel' : 'endPixel'] = newPos
                this.progressData = {...progressDataObj}

                // 更新相对比例
                const brushWidth = parseFloat(customBrush.style.width)
                this.brushConfigs[brushId][activeHandle === brushStart ? 'startRatio' : 'endRatio'] = newPos / brushWidth

                startX = e.clientX
                e.preventDefault()
            }
            // 添加鼠标移动和松开事件监听
            document.addEventListener('mousemove', handleMouseMove)
            document.addEventListener('mouseup', () => {
                if (isDragging) {
                    isDragging = false
                    this.currentBrushIndex = -1
                    this.handIndex = -1
                    this.showTips = false
                    activeHandle = null
                    getSelectedSeconds()
                    // getSelectedData();
                }
            })

            // 初次渲染时更新位置
            updateBrush()
            this.brushConfigs[brushId].update = updateBrush
        },
        // 处理窗口大小调整
        handleResize() {
            if (this.myChart) {
                this.myChart.resize()
                // 在图表调整大小后重新计算所有刷选控件的位置
                Object.keys(this.brushConfigs).forEach((brushId) => {
                    this.brushConfigs[brushId].update()
                })
                if (this.versionType === VERSION_TYPE.AGENT) this.setCustomPositions(this.liveRoom[0])
            }
        },
        //数据
        getData(videoInfo, anchorInfo, key, index) {
            const {VideoId: videoId} = videoInfo || {}
            if (!videoId) return null
            return this.$httpBack.v2100.onlineChartData({videoId, step: Number(this.curveStep) || 1}).then(res => {
                if (res.code === 0) {
                    const deepChartData = cloneDeep(this.chartData)
                    if (!(res.data?.onlineDataList?.length > 1)) {
                        deepChartData[key] = null
                        this.chartData = deepChartData
                        return null
                    }
                    const hasVideoInfo = Object.keys(this.sentenceMarkData).includes('videoInfo')
                    const sumOnlineCount = sumBy(res.data?.onlineDataList, (o) => o?.valueNum || 0) || 0
                    const sumApproachCount = sumBy(res.data?.approachDataList, (o) => o?.valueNum || 0) || 0
                    const isShowCharts = hasVideoInfo
                        ? sumOnlineCount > 0 || sumApproachCount > 0
                        : sumOnlineCount > 0

                    deepChartData[key] = isShowCharts ? {...res.data, videoInfo, anchorInfo, sort: index} : null
                    this.chartData = deepChartData
                    return isShowCharts ? res : null
                }
                return null
            });
        },
        async getDataAndDraw(sentenceMarks) {
            const _this = this
            const requestEvent = []
            const hasVideoInfo = Object.keys(sentenceMarks).includes('videoInfo')
            if (hasVideoInfo) {
                const {videoInfo = {}, anchorInfo = {}} = sentenceMarks
                requestEvent.push(_this.getData(videoInfo, anchorInfo, 'data1', 0))
            } else {
                Object.entries(sentenceMarks).forEach(([key, value], index) => {
                    const {videoInfo = {}, anchorInfo = {}} = sentenceMarks[key]
                    requestEvent.push(_this.getData(videoInfo, anchorInfo, key, index))
                })
            }
            this.loadingImg = false
            Promise.all(requestEvent)
                .then((results) => {
                    console.log('所有数据加载完成:', results)
                    this.loadingImg = true
                    if (this.isEmptyChartData) return

                    const longestList = results?.reduce((max, item) =>
                        item?.data?.onlineDataList?.length > max.length ? item.data?.onlineDataList : max, [])
                    const xAxisData = []

                    longestList.forEach(item => {
                        xAxisData.push(myUtils.toformatTime(item.dateTimeNew))
                    })
                    _this.xAxisData = xAxisData
                    if (_this.$refs.chartContainer) {
                        _this.initChart() // 初始化图表
                        _this.setChartOptions()// 设置图表选项
                        if (_this.aIAnalysis) _this.setupBrushes() // 设置刷选控件
                        window.addEventListener('resize', _this.handleResize) // 添加窗口大小调整监听
                        if (this.isSliceAnalysis) this.sliceAnalysisClick()
                    } else {
                        console.log('图表容器引用不可用')
                    }
                })
                .catch((error) => {
                    console.error('promise有任务失败:', error)
                }).finally(() => {
                this.loadingImg = true
            })
        },
    },
    beforeDestroy() {
        window.removeEventListener('resize', this.handleResize) // 移除窗口大小调整监听
        if (this.myChart) {
            this.myChart.dispose() // 销毁图表实例
        }
        if (this.observer) {
            this.observer.disconnect() // 组件卸载时取消监听
        }
    },
}
</script>

<style lang='scss' scoped>
$green-color: #16CC96;
$yellow-color: #FFD480;

.chart-wrapper {
    position: relative;
    width: 100%;
    min-height: 295px;

    .curve-data-item {
        align-items: flex-start;
        position: absolute;
        right: 20px;
        top: 0;
        z-index: 20;

        > div {
            padding: 0 8px;
        }
    }

    .curve-data-item > div {
        display: flex;
        flex-direction: column;
        justify-content: flex-start;
    }

    .ai-Analysis-mode {
        background: transparent;
    }

    .ai-Analysis-btn {
        background-image: url('~@/assets/imgs/to_ai.png');
        background-size: 100% 100%;
        background-repeat: no-repeat;
        width: 104px;
        margin-right: 20px;
        background-color: transparent;
        height: 27px;
        border: none;
    }

    .exit-action {
        position: absolute;
        right: 45px;
        top: 0;
        margin-top: 0;
        z-index: 20;
        display: flex;
        align-items: flex-start;
    }

    .chart-container {
        position: relative;
        height: 295px;
        overflow: hidden;
        // padding-top: 30px;
    }

    .aIAnalysis-action {
        position: absolute;
        top: 0;
        width: 100%;
    }

    .tips-time {
        position: absolute;
        background: #fff;
        right: 0;
        z-index: 50;
        background: rgba(0, 0, 0, 0.7);
        width: 145px;
        font-size: 12px;
        line-height: 20px;
        font-weight: 500;
        border-radius: 4px;
        color: #ffff;
        padding: 4px;
    }

    .custom-brush {
        position: absolute;
        display: block;
        z-index: 10;
        top: 0;
        left: 0;
        height: 5px;
        pointer-events: none;
        border-radius: 5px;
    }

    .custom-brush-slice-view {
        background: transparent !important;
    }

    .tool-com {
        position: absolute;
        height: 12px;
        cursor: pointer;

        &:hover {
            &::before {
                content: '';
                position: absolute;
                width: 1px;
                height: 220px;
                border-left: 1px #C2CFD9 dashed;
                bottom: 14px;
                left: 5px;
            }
        }
    }

    .lucky-bag-item {
        width: 12px;
        border-radius: 50%;
        background: #F8937F;
        top: -6px;

        &:hover {
            border: 3px #6287BA solid;
            top: -8px;
            margin-left: -3px;
            box-sizing: content-box;
            z-index: 22222;

            &::before {
                content: '';
                position: absolute;
                width: 1px;
                height: 220px;
                border-left: 1px #C2CFD9 dashed;
                bottom: 14px;
                left: 5px;
            }
        }
    }

    .slice-analysis-item {
        border-radius: 12px;
        top: -6px;

        &:hover {
            background: #636CBD;
            box-sizing: content-box;
            z-index: 22222;
        }
    }


    .brush-line {
        position: absolute;
        width: 2px;
        height: calc(100% - 15px);
        transform: translateX(0px);
        top: 18px;
    }

    .brush-handle {
        position: absolute;
        width: 15px;
        height: 15px;
        border-radius: 50%;
        top: -23px;
        transform: translateX(-50%);
        cursor: move;
        z-index: 11;
        pointer-events: auto;
        background: #fff;
        display: flex;
        align-items: center;
        justify-content: center;

        .icon {
            font-weight: 600;
            font-size: 12px;
        }
    }

    #custom-brush-0 {
        .brush-line {
            background: $green-color;

            &::after {
                content: '';
                display: block;
                height: 234px;
                position: absolute;
                right: 1px;
                top: -11px;
                border-left: 1px $green-color dashed;
            }
        }

        .brush-handle {
            border: 1px $green-color solid;

            .icon {
                color: $green-color;
            }
        }

        .brush-line-slice-mode {
            .brush-handle {
                border: 1px #F49D2C solid;

                .icon {
                    color: #F49D2C
                }
            }

            &::after {
                border-left: 1px #F49D2C dashed;
            }
        }

        .brush-line-slice {
            &:first-child {
                &::after {
                    width: var(--after-width, 0); /* 使用CSS变量 */
                    height: 230px;
                    background: rgba(0, 0, 0, 0.1);
                    right: 0;
                    top: -13px;
                    transform: scaleX(-1)
                }
            }

            &:nth-child(2) {
                &::after {
                    width: var(--after-width, 0); /* 使用CSS变量 */
                    height: 230px;
                    background: rgba(0, 0, 0, 0.1);
                    left: 0;
                    top: -13px;
                }
            }
        }

        .brush-line-slice-view {
            .brush-handle {
                display: none;
            }
        }
    }

    .custom-brush-0 {
        .brush-line {
            &::after {
                height: 204px !important;
            }
        }
    }

    #custom-brush-1 {
        .brush-line {
            background: $yellow-color;

            &::after {
                content: '';
                display: block;
                height: 218px;
                position: absolute;
                right: 1px;
                top: -10px;
                border-left: 1px $yellow-color dashed;
            }
        }

        .brush-handle {
            border: 1px $yellow-color solid;

            .icon {
                color: $yellow-color;
            }
        }
    }

    .custom-brush-1 {
        .brush-line {
            &::after {
                height: 188px !important;
            }
        }
    }

    .progress {
        position: absolute;
        height: 5px;
        z-index: -2;
        top: 0;
    }

    .progress-view {
        background: transparent !important;
    }
}
</style>

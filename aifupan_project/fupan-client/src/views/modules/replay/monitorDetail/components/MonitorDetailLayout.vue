<!--
@description 监控详情页布局：左侧承载分钟段落与音频联动，右侧承载独立的监控报告面板，并提供折叠能力。
注意：右侧报告区不再复用 wordDiscern 整体壳层，只保留新页面所需的三类监控报告能力。
-->
<template>
    <div class="monitorDetailLayout contrast-item-box analysis-flex-column">
        <analysisTitle ref="title" notWords :sentenceMarkData="sentenceMarkData" :targetType="targetType">
            <template #time-top>
                <div v-if="anchorSchedulePositionName && anchorScheduleEmployeeNames" class="monitorScheduleInfo pd-l4">
                    <span class="monitorScheduleInfoValue text-colorTheme">{{ anchorSchedulePositionName }}：{{ anchorScheduleEmployeeNames }}</span>
                </div>
            </template>
            <template #anchor-right>
                <div></div>
            </template>
        </analysisTitle>
        <div class="videoAnalysisContainer pd-t10">
            <el-row :gutter="16" class="h100">
                <el-col v-if="leftSpan > 0" class="h100 monitorLeftCol" :span="leftSpan">
                    <div class="h100 main-bg brs-8 overflow_hidden monitorLeftBox">
                        <div class="monitorHiddenVideo">
                            <contrastVideo
                                ref="videoPlayer"
                                name="monitor"
                                notScrolling
                                :targetType="targetType"
                                :sentenceMarkData="sentenceMarkData"
                                @setDuration="setDuration"
                                @playerTimeupdate="onPlayerTimeupdate"
                                @changeParagraphIndex="onPlayerParagraphIndex"
                                @playStaus="playStaus"
                            />
                        </div>

                        <div class="monitorLocateBar pd-l12 pd-r12 pd-t12">
                            <div class="monitorLocateBarInner brs-8 pd-b16 pd-l10">
                                <div class="monitorSliderBox">
                                    <el-slider
                                        v-model="sliderValue"
                                        :marks="sliderMarks"
                                        :max="videoMaxTime"
                                        :format-tooltip="formatTooltip"
                                        @change="onSliderChange"
                                    />
                                </div>
                                <el-button type="text" class="pd-0 monitorPlayBtn" :disabled="!canPlay" @click="togglePlay">
                                    <img style="width: 30px;" v-if="isPlaying" src="@/assets/imgs/pStop.png" alt="">
                                    <img style="width: 28px;" v-else src="@/assets/imgs/play1.png" alt="">
                                </el-button>
                            </div>
                        </div>

                        <wordDiscern
                            ref="leftTextDom"
                            class="h100 monitorWordDiscern b-t1-c1"
                            name="monitor"
                            isAi
                            type="assistant"
                            monitorOnlyMode
                            :targetType="targetType"
                            :sentenceMarkData="sentenceMarkData"
                            :enableSwappedTabs="true"
                            :tabsInContent="true"
                            notLocatingBar
                            notTarde
                            notExport
                            @playerReadied="onPlayerReadied"
                            @playerPause="onPlayerPause"
                            @onParagraph="onParagraph"
                        >
                            <template #toolbar-left>
                                <div class="monitorBarrageSwitch flex-ai-c">
                                    <span class="font-s12 text-colorMain mg-r6">弹幕</span>
                                    <el-switch v-model="showBarrage" active-color="#409EFF"></el-switch>
                                </div>
                            </template>
                            <template #textParagraph-after="{ item, index }">
                                <MonitorParagraphBarrage v-if="shouldRenderBarrage(index)" :sentenceMarkData="sentenceMarkData" :paragraph="item" />
                            </template>
                        </wordDiscern>

                    </div>
                </el-col>

                <el-col class="h100 monitorRightCol" :span="rightSpan">
                    <div class="h100 main-bg brs-8 overflow_hidden monitorRightBox">
                        <div class="monitorRightHeader pd-8 flex-ai-c flex-jc-sb">
                            <el-radio-group
                                class="monitorRightTabs"
                                v-model="reportType"
                                size="medium"
                                @input="onReportTypeInput"
                            >
                                <el-radio-button label="scriptQuality">话术质检</el-radio-button>
                                <el-radio-button v-if="showInteractionInspection" label="interactionInspection">互动巡检</el-radio-button>
                                <el-radio-button v-if="showScriptRestoration" label="scriptRestoration">话术还原度</el-radio-button>
                            </el-radio-group>
                            <div class="monitorRightActions">
                                <el-button v-if="showRegenerateAction" type="text" class="monitorRightActionBtn" @click="onRegenerateReport">重新生成</el-button>
                                <!-- <el-button type="text" class="monitorRightActionBtn" @click="onOptimizeFormat">优化格式</el-button> -->
                                <el-button type="text" class="monitorRightActionBtn" @click="onExportReport">导出报告</el-button>
                            </div>
                        </div>

                        <MonitorReportPanel
                            ref="reportPanel"
                            class="h100 monitorReportDiscern"
                            :reportType="reportType"
                            :targetType="targetType"
                            :sentenceMarkData="sentenceMarkData"
                        />

                        <aiFlod class="fold" :max="1" @change="onFoldChange" />
                    </div>
                </el-col>
            </el-row>
        </div>

        <MonitorReportExportDialog
            :visible.sync="exportDialogVisible"
            :reportType="exportDialogReportType"
            :sentenceMarkData="sentenceMarkData"
            :content="exportDialogContent"
            :contentList="exportDialogContentList"
            :fileName="exportDialogFileName"
        />
    </div>
</template>

<script>
/**
 * @description 监控详情页布局脚本：负责音频播放、段落联动、顶部报告切换与左右布局折叠，不处理右侧报告具体渲染。
 */
import analysisTitle from '@/views/commonComponent/analysisLayout/component/analysisTitle.vue'
import contrastVideo from '@/views/commonComponent/analysisLayout/contrast-video.vue'
import wordDiscern from '@/views/commonComponent/analysisLayout/component/wordDiscern.vue'
import aiFlod from '@/views/commonComponent/aiAnalysis/aiFold.vue'
import MonitorReportPanel from './MonitorReportPanel.vue'
import MonitorParagraphBarrage from './MonitorParagraphBarrage.vue'
import MonitorReportExportDialog from './MonitorReportExportDialog.vue'
import myUtils from '@/utils/utils'
import publicMixin from '@/views/commonComponent/analysisLayout/mixin/publicMixin'
import aiCanvasImage from '@/mixins/aiCanvasImage'

export default {
    mixins: [publicMixin, aiCanvasImage],
    components: {
        analysisTitle,
        contrastVideo,
        wordDiscern,
        aiFlod,
        MonitorReportPanel,
        MonitorReportExportDialog,
        MonitorParagraphBarrage
    },
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        },
        targetType: {
            type: String,
            default: ''
        },
        defaultReportType: {
            type: String,
            default: 'scriptQuality'
        }
    },
    data() {
        return {
            flod: 0,
            reportType: this.defaultReportType || 'scriptQuality',
            videoMaxTime: 0,
            sliderValue: 0,
            sliderMarks: {},
            isPlaying: false,
            playType: '',
            paragraphDisplayConfig: {},
            showBarrage: false,
            barrageRenderCount: 0,
            barrageRenderTimer: null,
            anchorSchedulePositionName: '',
            anchorScheduleEmployeeNames: '',
            anchorScheduleRequestKey: '',
            exportDialogVisible: false,
            exportDialogReportType: 'scriptQuality',
            exportDialogContent: '',
            exportDialogContentList: [],
            exportDialogFileName: ''
        }
    },
    computed: {
        leftSpan() {
            return this.flod === 1 ? 0 : 9
        },
        rightSpan() {
            return this.flod === 1 ? 24 : 15
        },
        getVideoVnode() {
            return this.$refs?.videoPlayer
        },
        getLeftTextVnode() {
            return this.$refs?.leftTextDom
        },
        canPlay() {
            return !!this.sentenceMarkData?.playUrl
        },
        currentParagraphIndex() {
            const currentMs = Number(this.sliderValue || 0) * 1000
            const sentenceMarkList = this.sentenceMarkData?.sentenceMarkList || []
            return sentenceMarkList.findIndex((item) => {
                const startTime = Number(item?.startTime || 0)
                const endTime = Number(item?.endTime || 0)
                return currentMs >= startTime && currentMs <= endTime
            })
        },
        currentParagraph() {
            return this.sentenceMarkData?.sentenceMarkList?.[this.currentParagraphIndex] || null
        },
        barrageBatchSize() {
            return 5
        },
        isUploadFileSource() {
            return String(this.$route?.query?.sourceKind || '') === 'uploadFile'
        },
        showInteractionInspection() {
            return !this.isUploadFileSource
        },
        showScriptRestoration() {
            return !this.isUploadFileSource
        },
        /**
         * @description 云空间监控详情页不展示“重新生成”操作，仅保留导出报告。
         * 客户端原监控详情页维持现有按钮行为不变。
         * @returns {boolean}
         */
        showRegenerateAction() {
            return this.targetType !== 'webOnline'
        }
    },
    watch: {
        defaultReportType: {
            handler(val) {
                if (['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(val)) {
                    this.reportType = val
                }
            },
            immediate: true
        },
        sentenceMarkData: {
            handler() {
                this.applyParagraphDisplayConfig()
                this.loadAnchorSchedulePlan()
                if (this.showBarrage) {
                    this.restartBarrageLazyRender()
                }
            },
            deep: false
        },
        showBarrage(val) {
            if (val) {
                this.restartBarrageLazyRender()
                return
            }
            this.clearBarrageRenderTimer()
            this.barrageRenderCount = 0
        },
        isUploadFileSource: {
            handler(val) {
                if (!val) return
                if (this.reportType !== 'scriptQuality') {
                    this.reportType = 'scriptQuality'
                }
            },
            immediate: true
        }
    },
    mounted() {
        this.$nextTick(() => {
            this.initPayerIndexMap()
            this.setPayerIndexMap(this.sentenceMarkData?.sentenceMarkList || [], 'monitor')
            this.applyParagraphDisplayConfig()
            this.loadAnchorSchedulePlan()
        })
    },
    beforeDestroy() {
        this.clearBarrageRenderTimer()
    },
    methods: {
        getAnchorScheduleParams() {
            if (!this.$store.getters.largeEnterprises) return null
            if (this.isUploadFileSource) return null

            const videoInfo = this.sentenceMarkData?.videoInfo || {}
            const anchorInfo = this.sentenceMarkData?.anchorInfo || {}

            const livePlatformType = videoInfo?.PlatformType || anchorInfo?.platform || anchorInfo?.PlatformType
            const videoId = videoInfo?.VideoId || videoInfo?.videoId || this.$route?.query?.id
            const secUid = anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId
            const startTime = videoInfo?.StartTime || videoInfo?.startTime || ''
            const endTime = videoInfo?.EndTime || videoInfo?.endTime || ''

            if (!videoId) return null

            return {
                livePlatformType,
                videoId,
                secUid,
                startTime,
                endTime
            }
        },
        extractScheduleEmployeeNames(position) {
            const direct = position?.employeeNames || position?.employeeName
            if (direct) return String(direct)
            const employees = Array.isArray(position?.employees) ? position.employees : []
            return employees
                .map(employee => employee?.employeeName || employee?.name || employee?.nickName || employee?.employeeNickName)
                .filter(Boolean)
                .join('、')
        },
        normalizeSchedulePlanResponse(data) {
            if (!data) return []
            if (Array.isArray(data)) return data
            if (Array.isArray(data?.list)) return data.list
            if (Array.isArray(data?.records)) return data.records
            if (Array.isArray(data?.rows)) return data.rows
            return [data]
        },
        async loadAnchorSchedulePlan() {
            const params = this.getAnchorScheduleParams()
            if (!params) return

            const requestKey = JSON.stringify(params)
            if (requestKey === this.anchorScheduleRequestKey) return
            this.anchorScheduleRequestKey = requestKey

            const request = this.$httpBack2?.liveRoom?.plan
            if (typeof request !== 'function') return

            try {
                const res = await request(params)
                if (res?.code !== 0) return

                const list = this.normalizeSchedulePlanResponse(res?.data)
                const schedules = list.filter(Boolean)
                const schedulePositions = schedules
                    .map(item => (Array.isArray(item?.positions) ? item.positions : []))
                    .flat()

                const anchorPosition = schedulePositions.find(p => p?.anchorPosition === true)
                const positionName = anchorPosition?.positionName || ''
                const employeeNames = this.extractScheduleEmployeeNames(anchorPosition) || ''

                if (positionName && employeeNames) {
                    this.anchorSchedulePositionName = positionName
                    this.anchorScheduleEmployeeNames = employeeNames
                } else {
                    this.anchorSchedulePositionName = ''
                    this.anchorScheduleEmployeeNames = ''
                }
            } catch (e) {
            }
        },
        normalizeReportType(type) {
            const t = String(type || '')
            if (!['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(t)) return 'scriptQuality'
            if (this.isUploadFileSource && t !== 'scriptQuality') return 'scriptQuality'
            return t
        },
        clearBarrageRenderTimer() {
            if (this.barrageRenderTimer) {
                clearTimeout(this.barrageRenderTimer)
                this.barrageRenderTimer = null
            }
        },
        restartBarrageLazyRender() {
            this.clearBarrageRenderTimer()
            this.barrageRenderCount = 0
            this.scheduleNextBarrageBatch()
        },
        scheduleNextBarrageBatch() {
            if (!this.showBarrage) return
            const total = this.sentenceMarkData?.sentenceMarkList?.length || 0
            if (!total) return
            this.barrageRenderCount = Math.min(this.barrageRenderCount + this.barrageBatchSize, total)
            if (this.barrageRenderCount >= total) return
            this.barrageRenderTimer = setTimeout(() => {
                this.scheduleNextBarrageBatch()
            }, 300)
        },
        shouldRenderBarrage(index) {
            return this.showBarrage && Number(index) < Number(this.barrageRenderCount || 0)
        },
        syncPlayButtonState(isPlaying, playType = '') {
            this.isPlaying = !!isPlaying
            this.playType = isPlaying ? (playType || this.playType || 'play') : ''
        },
        applyParagraphDisplayConfig() {
            const config = this.buildParagraphDisplayConfig()
            this.paragraphDisplayConfig = config
            this.$nextTick(() => {
                this.getLeftTextVnode?.otherChange?.(config)
                setTimeout(() => {
                    this.getLeftTextVnode?.otherChange?.(config)
                }, 0)
            })
        },
        onFoldChange(val) {
            this.flod = val
        },
        onReportTypeInput(val) {
            this.reportType = this.normalizeReportType(val)
        },
        setDuration({ videoDuration, MathDuration }) {
            this.$nextTick(() => {
                this.getLeftTextVnode?.marksTitle?.(MathDuration)
                this.$refs?.title?.setCharAndTime?.(videoDuration, this.sentenceMarkData?.allCharCountNum)
                this.videoMaxTime = Number(MathDuration || 0)
                this.sliderMarks = this.createSliderMarks(this.videoMaxTime)
            })
        },
        onPlayerTimeupdate(time) {
            this.sliderValue = Number(time || 0)
            this.getLeftTextVnode?.setVideoCurrentTime?.(time)
        },
        onPlayerParagraphIndex(index) {
            this.getLeftTextVnode?.selectDomeScrollIntoView?.(index, undefined, 'video-only')
        },
        onPlayerReadied(second, type) {
            this.syncPlayButtonState(true, type || 'play')
            this.getVideoVnode?.ponlayerReadied?.(second, type)
        },
        onPlayerPause() {
            const currentPlayType = this.playType || 'play'
            this.syncPlayButtonState(false)
            this.getLeftTextVnode?.playStaus?.({
                type: 'stop',
                playType: currentPlayType
            })
            this.getVideoVnode?.playerPause?.()
        },
        onParagraph(index) {
            this.getVideoVnode?.setVideoParagraphIndex?.(index)
        },
        playStaus(data) {
            this.syncPlayButtonState(data?.type === 'play', data?.playType)
            this.getLeftTextVnode?.playStaus?.(data)
        },
        formatTooltip(value) {
            return myUtils.toformatTimeMM_ssChinse(Number(value || 0) * 1000)
        },
        buildParagraphDisplayConfig() {
            const sentenceMarkData = this.sentenceMarkData || {}
            const totalBarrageNum = Number(sentenceMarkData?.totalBarrageNum || 0)
            const interactionPercent = sentenceMarkData?.interactionPercent
            const isBuyInData = sentenceMarkData?.dataSourceType === 1
            const purchaseCountStart = sentenceMarkData?.purchaseCountStart
            const purchaseCountEnd = sentenceMarkData?.purchaseCountEnd
            const volumeStart = sentenceMarkData?.volumeStart
            const volumeEnd = sentenceMarkData?.volumeEnd
            const uvValueStart = sentenceMarkData?.uvValueStart
            const uvValueEnd = sentenceMarkData?.uvValueEnd
            const platformType = sentenceMarkData?.videoInfo?.PlatformType || sentenceMarkData?.fileInfo?.platformType
            const isKuaishou = Number(platformType) === 3
            const showTotalDeal = myUtils.isGreaterThanZero(purchaseCountStart) || myUtils.isGreaterThanZero(purchaseCountEnd)
            const showSales = myUtils.isGreaterThanZero(volumeStart) || myUtils.isGreaterThanZero(volumeEnd)
            const showUV = myUtils.isGreaterThanZero(uvValueStart) || myUtils.isGreaterThanZero(uvValueEnd)

            return {
                startTime: 1,
                natureTime: 0,
                onlineNum: 1,
                analysisChar: 0,
                barrageNum: isKuaishou || totalBarrageNum <= 0 ? 0 : 1,
                dealNum: showTotalDeal && isBuyInData ? 1 : 0,
                interactionRate: isKuaishou || totalBarrageNum <= 0 || !interactionPercent ? 0 : 1,
                dealRate: showTotalDeal && isBuyInData && !isKuaishou ? 1 : 0,
                sales: showSales && isBuyInData && !isKuaishou ? 1 : 0,
                uv: showUV && isBuyInData && !isKuaishou ? 1 : 0
            }
        },
        createSliderMarks(videoDurationInt) {
            const marks = { 0: '00:00' }
            let remain = Number(videoDurationInt || 0)
            const markTime = parseInt(remain) > 18000 ? 3600 : 1800
            let index = 1
            while ((remain - 300) > markTime) {
                marks[index * markTime] = parseInt(index * markTime / 60) + 'min'
                remain -= markTime
                index += 1
            }
            return marks
        },
        onSliderChange(value) {
            this.onPlayerReadied(Number(value || 0) * 1000, 'click')
        },
        togglePlay() {
            if (!this.canPlay) return
            if (this.isPlaying) {
                this.onPlayerPause()
                return
            }
            this.onPlayerReadied(Number(this.sliderValue || 0) * 1000, 'play')
        },
        sanitizeExportFileName(name) {
            return String(name || '')
                .replace(/[\\/:*?"<>|]/g, '-')
                .replace(/\s+/g, ' ')
                .trim()
        },
        getExportFileName() {
            const anchorInfo = this.sentenceMarkData?.anchorInfo || {}
            const anchorName = anchorInfo?.anchorName || anchorInfo?.AnchorName || ''
            const startTime = this.sentenceMarkData?.startTime
                || this.sentenceMarkData?.StartTime
                || this.sentenceMarkData?.videoInfo?.startTime
                || this.sentenceMarkData?.videoInfo?.StartTime
                || this.sentenceMarkData?.fileInfo?.UploadTime
                || ''
            const labelMap = {
                scriptQuality: '话术质检',
                interactionInspection: '互动巡检',
                scriptRestoration: '话术还原度'
            }
            const name = [anchorName, startTime, labelMap[this.reportType] || '报告'].filter(Boolean).join('-')
            return this.sanitizeExportFileName(name || '报告')
        },
        getReportExportDom() {
            const panel = this.$refs?.reportPanel
            if (!panel?.$refs) return null
            if (this.reportType === 'scriptRestoration') return panel.$refs.scriptRestoration || null
            return panel.$refs?.[this.reportType] || null
        },
        onRegenerateReport() {
            this.$refs?.reportPanel?.createInspectionReport?.(this.reportType, { keepOldOn70001: true })
        },
        onOptimizeFormat() {
            this.$refs?.reportPanel?.optimizeReportFormat?.(this.reportType)
        },
        onExportReport() {
            const panel = this.$refs?.reportPanel
            const data = panel?.getExportData?.(this.reportType)
            if (!data) {
                this.$message.warning('暂无可导出的报告内容')
                return
            }
            this.exportDialogReportType = data?.reportType || this.reportType
            this.exportDialogContent = data?.content || ''
            this.exportDialogContentList = Array.isArray(data?.contentList) ? data.contentList : []
            this.exportDialogFileName = this.getExportFileName()
            this.exportDialogVisible = true
        }
    }
}
</script>

<style scoped>
.monitorDetailLayout {
    height: 100%;
    min-height: 0;
    overflow: hidden;
}

.videoAnalysisContainer {
    height: calc(100% - 64px);
    min-height: 0;
    overflow: hidden;
}

.videoAnalysisContainer > .el-row {
    height: 100%;
    min-height: 0;
}

.monitorLeftBox {
    position: relative;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
}

.monitorHiddenVideo {
    position: absolute;
    left: -99999px;
    top: -99999px;
    width: 1px;
    height: 1px;
    opacity: 0;
    pointer-events: none;
}

.monitorLocateBar {
    flex-shrink: 0;
}

.monitorLocateBarInner {
    display: flex;
    align-items: center;
    width: 100%;
    background: #fff;
}

.monitorSliderBox {
    flex: 1;
    min-width: 0;
    order: 1;
}

.monitorPlayBtn {
    margin-left: 10px;
    flex-shrink: 0;
    order: 2;
}

.monitorRightBox {
    display: flex;
    flex-direction: column;
    position: relative;
    min-height: 0;
    overflow: hidden;
}

.monitorRightHeader {
    flex-shrink: 0;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.monitorRightTabs {
    flex: 1;
    min-width: 0;
    width: auto;
}

.monitorRightActions {
    flex-shrink: 0;
    display: flex;
    align-items: center;
}

.monitorRightActionBtn {
    padding: 0 6px !important;
}

.monitorRightActionBtn + .monitorRightActionBtn {
    margin-left: 4px !important;
}

.monitorRightBox:hover .fold {
    opacity: 1;
    left: 0;
}

.monitorLeftCol {
    padding-right: 8px !important;
}

.monitorRightCol {
    padding-left: 8px !important;
}

.monitorLeftBox ::v-deep(.wordsContainer) {
    display: none;
}

.monitorLeftBox ::v-deep(.discernContainer) {
    height: 100%;
    min-height: 0;
}

.monitorWordDiscern ::v-deep(.discernSearchContainerContent),
.monitorWordDiscern ::v-deep(.analysis-flex-column) {
    height: 100%;
    min-height: 0;
}

.monitorWordDiscern ::v-deep(.discernContainer),
.monitorWordDiscern ::v-deep(.discernSearchContainerContent) {
    overflow: hidden;
    padding-top: 0 !important;
}

.monitorWordDiscern ::v-deep(.wordsBodyContainer) {
    padding: 0 10px !important;
    overflow-y: auto !important;
    min-height: 0;
}

.monitorWordDiscern ::v-deep(.discernContainer > .flex-jc-e),
.monitorReportDiscern ::v-deep(.scriptQualityDetailContainer),
.monitorReportDiscern ::v-deep(.monitorReportPanelBody),
.monitorReportDiscern ::v-deep(.scriptQualityDetailContent) {
    height: 100%;
    min-height: 0;
}

.monitorReportDiscern ::v-deep(.monitorReportPanelBody) {
    padding: 0 10px !important;
}

.monitorReportDiscern ::v-deep(.ai-mdtag-render-root.ai-mdtag-skin-paper) {
    padding: 0 10px !important;
}

.monitorWordDiscern ::v-deep(.discernContainer > .flex-jc-e) {
    display: none;
}

.monitorLocateBar ::v-deep(.el-slider__marks-text) {
    font-size: 12px;
}

.monitorLocateBar ::v-deep(.el-slider__bar) {
    background: linear-gradient(90deg, #83E5FC 0%, #8F8EFF 100%);
    box-shadow: inset 0px -1px 1px 0px rgba(1, 89, 174, 0.25);
}

.monitorScheduleInfo {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.65);
}
</style>

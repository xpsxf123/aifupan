<!--
@description 复盘详情容器：承载详情页分析布局、顶部操作区、AI/监控入口联动与部分跨页面跳转逻辑。
-->
<template>
    <div style="display: flex; flex-direction: column;height: 100%;">
        <!-- 新版 -->
        <analysisContrastItem ref='analysis' :sentenceMarkData="sentenceMarkData" :selectedText.sync="selectedText"
                              :targetType="targetType" @markwords="markWords" @treeChange="treeChange"
                              @treeLoad="tradeTreeLoad"
                              @repairVideo="repairVideo"
                              @notesEditer="notesEditer"
                              @textTypeChange="textTypeChange"
                              @activeNameChange="handleActiveNameChange"
                              :notes="isNotes"
                              :enableSwappedTabs="true"
                              :tabsInContent="true"
                              @quitNotes="quitNotes"
                              @annotation="annotationHandler"
                              @setTextScriptInfo="setTextScriptInfo"
                              @rightTickContextMenu="rightTickContextMenu">
            <template #anchor-right>
                <div></div>
            </template>

            <template #tabs-right>
                <div class="analysis-item-tabs-actions flex-ai-c pd-r10 action-btns">
                    <Share v-if="isVideoId && !isShare && !isWebOnline && !isRepair && versionType === VERSION_TYPE.AGENT" @analysisClick="shareAnalysis"
                           @shareClick="handleCopyShareAnalysisLink"
                           :isShareUrl="((videoInfo && videoInfo.UploadStatus === 1) || (fileInfo && fileInfo.uploadStatus === 1))">
                    </Share>
                    <afp-button v-if="showCopyTextBtn" @click="handleCopyText" size="medium" style="margin-left: 10px;">复制原文</afp-button>
                    <template
                        v-if="isReplay || isFile || isFileSection || isSection || isOnline || isWebOnline">
                        <afp-button v-if="!isCompare && !isNotes" @click="handleExportScript" size="medium">导出话术</afp-button>
                    </template>
                    <template v-if="versionType === VERSION_TYPE.AGENT">
                        <template v-if="isReplay || (isOnline && isReplay)">
                            <afp-button @click="handleContrastData" size="medium"
                                        v-if="isVideoId && !isShare && !isWebOnline && !isExample">对比上一场
                            </afp-button>
                        </template>
                    </template>
                    <template v-if="(isReplay || isFile)&&!isWebOnline&&!isOnline&&playUrl&&!isShare">
                        <afp-button @click="()=>handleSelectSliceAnalysis(undefined)" size="medium">切片</afp-button>
                        <span @click="howToSlice" class="icon font_family icon-a-Frame530 cursor-pointer" style="color: green;font-size:18px;margin-left: 10px;"></span>
                    </template>
                    <template v-if="versionType === VERSION_TYPE.PURE">
                        <afp-button @click="checkViolation" size="medium" style="margin-left: 12px">AI查违规</afp-button>
                    </template>
                    <template v-if="versionType === VERSION_TYPE.AGENT">
                        <template v-if="(isSection ||isShort || isFileSection || isFileShort)&&!isWebOnline&&!isOnline">
                            <afp-button size="medium" @click="viewAnalysis">查看整场复盘</afp-button>
                            <afp-button size="medium" @click="editSliceAnalysis">编辑</afp-button>
                        </template>
                    </template>
                </div>
            </template>
            <template #top-tabs-right>
                <div v-if="toolbarRef && toolbarRef.isSearch && !isCompare && toolbarRef.isTrade && !isNotes && versionType === VERSION_TYPE.AGENT" class="analysis-item-trade flex items-center">
                    <span class="text-xs" v-if="tradeTipVisible" style="color:var(--color-main)">行业可能不准！</span>
                    <TradeTreeList v-model="tradeIdProxy" :isSelectTrade="toolbarRef.isSelectTrade" size="medium"
                                   :treeList="toolbarRef.treeData || []" @tree-load="handleTradeTreeLoad"
                                   @tree-list-change="handleTradeListChange"></TradeTreeList>
                    <div class="text-xs tipsContainer" v-if="tradeTipVisible">
                        <div>
                            AI检测到本直播间行业为<span style="color:var(--color-main);">{{toolbarRef.recommendTrade?.tradeName}}</span>更精确，已帮您修改。
                        </div>
                        <div class="flex items-center justify-around">
                            <el-button type="text" class="text-xs" @click="handleCancelRecommend">改回原行业</el-button>
                            <el-button type="text" style="color:red;" class="text-xs" @click="handleReplaceIndustry">
                                确认修改
                            </el-button>
                        </div>
                    </div>
                </div>
            </template>
            <template #video-bottom>
                <slot name="video-bottom"></slot>
            </template>
            <template #toolbar-right-after v-if="!isShort && versionType === VERSION_TYPE.AGENT">
                <afp-button v-if="!isNotes && activeNameLocal === 'text'" plain size="medium" style="margin-left: 10px;" @click="handleStartNotes">笔记+批注</afp-button>
            </template>
            <template #word-control="{ videoInfo, fileInfo }" v-if="versionType === VERSION_TYPE.AGENT">
<!--                <el-button type="text" @click="handleStartTour"><i class="font_family icon-bangzhuzhongxinoff"></i>新手引导-->
<!--                </el-button>-->
<!--                <div class="flex-ai-c pd-r10">-->
<!--                    &lt;!&ndash; <AiTrain v-if="isVideoId && !isShare && !isWebOnline" :videoInfo="videoInfo"-->
<!--                             @treeChange="treeChange" :sentenceMarkData="sentenceMarkData"/> &ndash;&gt;-->
<!--                    <afp-button v-if="!isCompare && !isNotes" @click="exportScript" size="medium">导出话术</afp-button>-->
<!--                    <afp-button @click="contrastData" size="medium"-->
<!--                        v-if="isVideoId && !isShare && !isWebOnline && !isExample">对比上一场-->
<!--                    </afp-button>-->
<!--                    <Share v-if="isVideoId && !isShare && !isWebOnline && !isRepair" @analysisClick="shareAnalysis"-->
<!--                           @shareClick="copyShareAnalysisLink"-->
<!--                           :isShareUrl="((videoInfo && videoInfo.UploadStatus === 1) || (fileInfo && fileInfo.uploadStatus === 1))">-->
<!--                    </Share>-->
<!--                    <el-button type="text" @click="startTour"><i class="font_family icon-bangzhuzhongxinoff"></i>新手引导-->
<!--                    </el-button>-->
<!--                </div>-->
            </template>
            <template #tabs="{ DiscernSearchContainer }">
                <div v-if="!isNotes && activeNameLocal !== 'productData'" class="analysis-tabs-box">
                    <slot name="analysis-tabs">
                        <ControlTabs ref="ControlTabs" isNewTag :tableList="sentenceMarkData.wordsCollect"
                            :tabsList="sentenceMarkData.wordsTabList" :filterData="filterData" :tabs="getTabs"
                            :targetType="targetType" :sentenceMarkData="sentenceMarkData" :isWebOnline="isWebOnline"
                            :wordsInfo="wordsInfo" @brushChange="brushChange" @isAiElfChange="isAiElfChange"
                            @tabs-click="handleTabsClick"
                            @aiAnalysisData="aiAnalysisData" @clickWord="clickWord" :closeParagraph="closeParagraph"
                            @playerReadied="setPlayerReadied" @selectSliceAnalysis="selectSliceAnalysis">
                            <template #table-column>
                                <el-table-column prop="countNum" label="命中次数" align="center" width="110">
                                    <template slot-scope="{row}">
                                        <WordTableCountNum
                                            :isShow="(row.name + row.wordsType) === DiscernSearchContainer?.selectMark?.name"
                                            :countNum="row.countNum" :index="DiscernSearchContainer?.selectIndex"
                                            @selectMark="(type) => { DiscernSearchContainer?.selectMarkHandler(row.name + row.wordsType, type) }" />
                                    </template>
                                </el-table-column>
                            </template>

                            <template #table-scene>
                                <el-table-column prop="remarks" label="场景描述" align="center" min-width="150">
                                    <template slot-scope="scope">
                                        <div style="display: flex;align-items: center;justify-content: center;">
                                            <span>{{ sceneText(scope.row) }}</span>
                                            <span v-if="showMore(scope.row)">...</span>
                                            <el-popover placement="top-end" width="300" trigger="hover"
                                                :close-delay="30" popper-class="myPopover" :content="scope.row.remarks">
                                                <div slot="reference" style="width: 100%;">
                                                    <span v-if="showMore(scope.row)"
                                                        style="padding-left: 8px;color: #2988F5;"
                                                        @click.stop="handleSceneDetailClick">详情</span>
                                                </div>
                                            </el-popover>
                                        </div>
                                    </template>
                                </el-table-column>
                            </template>

                            <template #table-view>
                                <el-table-column prop="paragraph" label="查看句子" align="center" min-width="80">
                                    <template slot-scope="scope">
                                        <el-popover placement="top-end" width="420" trigger="manual"
                                                    v-model="popoverVisible[scope.$index]"
                                                    popper-class="check-paragraph"
                                                    :open-delay="30" ref="popoverRef"
                                                    v-if="popoverVisible[scope.$index]">
                                            <div class="tips_action">
                                                <span style="cursor: pointer;color: #0B7CFF" v-if="isVideoId"
                                                      @click="reportParagraph">导出</span>
                                                <i class="el-icon-circle-close"
                                                   style="font-size: 18px;cursor: pointer;padding-inline: 6px"
                                                   @click="() => exchangeParagraph(scope, false)"></i>
                                            </div>

                                            <Appear ref="appear_ref"
                                                    :sentenceMarkData="sentenceMarkData" :row="scope.row"/>
                                            <div slot="reference" style="width: 100%;">
                                                <span
                                                    style="color: #2988F5">点我查看</span>
                                            </div>
                                        </el-popover>
                                        <div v-else style="width: 100%;">
                                                <span
                                                    style="cursor: pointer;color: #2988F5"
                                                    @click="() => exchangeParagraph(scope)">点我查看</span>
                                        </div>
                                    </template>
                                </el-table-column>
                            </template>
                            <template #page-right>
                                <slot name="page-right">
                                    <div class="contact-kefu mg-r10">
                                        <div style="color: #909499; font-size: 12px; margin-right: 15px;"
                                            class="font-w100">
                                            添加客服微信，领取更多权益
                                        </div>
                                        <el-button size="small" type="text" icon="el-icon-right"
                                            @click="showCustomerServiceQrCode">人工咨询
                                        </el-button>
                                    </div>
                                </slot>
                            </template>
                        </ControlTabs>
                    </slot>
                </div>
            </template>
        </analysisContrastItem>

        <!-- Ai精灵 -->
        <aiElf v-if="isAiElf && !questionContent && !isNotes && (versionType === VERSION_TYPE.AGENT || isWebOnline || isOnline)" @click="elfClick"
               :sentenceMarkData="sentenceMarkData" :type="isVideoId ? 'video' : 'file'" ref="aiElfRef"
               :isScrolling="!!sentenceMarkData.totalBarrageNum"></aiElf>
        <!--对比上一场-->
        <ContrastLastTime ref="lastTime" :sentenceMarkData="sentenceMarkData" :targetType="targetType"
            :isContrast="true" />
        <!-- web端ai精灵提示 -->
        <aiDialog ref="aiDialog" @left-click="$refs.aiDialog?.hide()" @right-click="aiDialogSubmit"
            :rightBt="isWebOnline ? '前往官网' : '前往复盘'">
            <div v-if="isWebOnline">
                目前云空间分享暂未开放AI分析助手，<br><span class="text-colorTheme">请前往爱复盘官网下载爱复盘软件使用AI功能</span><br>
                后续AI助手就和大家见面，敬请期待～
            </div>
            <div v-else>
                亲爱的用户，云空间AI助手暂未开放，<span class="text-colorTheme">我们的团队正以超高速全力开发中！</span><br>
                <br>
                后续AI助手就和大家见面，敬请期待～
            </div>
        </aiDialog>

        <!-- 资源弹窗 -->
        <mark-dialog ref="markDialog" @cancal="cancalMark" @useMark="confirmUseMark"
            @qrCode="showCustomerServiceQrCode"></mark-dialog>

        <!-- 客服二维码 -->
        <customer-service-qr-code v-if="customerServiceQrCodeVisible"
            ref="customerServiceQrCode"></customer-service-qr-code>

        <!-- 自定义菜单 -->
        <contextmenu v-if="contextmenuVisible" :selected-text="selectedText" :targetType="targetType" ref="contextmenu"
            @task="onTask" @annotation="annotationContextmenu" :sentenceMarkData="sentenceMarkData" @taskSlice="taskSlice">
        </contextmenu>

        <!-- 批注弹窗 -->
        <annotationDialog ref="annotationDialog" @getList="getAnnotationList"></annotationDialog>

        <ContextmenuDialog v-if="keywordDialogVisible" :keywordSelectedText="selectedText" ref="contextmenuDialog">
        </ContextmenuDialog>

        <online-analysis-dialog v-if="onlineAnalysisDialogVisible" ref="onlineAnalysisDialog"
            @uploadVodSuccess="confirmShareAnalysis" :shareUrl="shareUrl"></online-analysis-dialog>

        <online-not-analysis-dialog v-if="onlineNotAnalysisDialogVisible"
            ref="onlineNotAnalysisDialog"></online-not-analysis-dialog>
        <complaintDialog v-if="complaintDialogVisible" ref="complaint"></complaintDialog>

        <SliceAnalysis ref="slice-analysis" :sentenceMarkData="sentenceMarkData" :textScriptInfo="textScriptInfo"/>

        <SliceTutorial ref="slice_tutorial" :isVideo="!isFile"/>

        <AiAgentWorkbenchGuide
            :visible.sync="aiAgentGuideVisible"
            @close="handleCloseAiAgentGuide"
            @experience="handleExperienceAiAgentGuide" />
    </div>
</template>

<script>
import AnalysusTime from '../../components/analysis/analysusTime.vue'
import MarkDialog from './markDialog.vue'
import AnalysusUpdateTime from '../../components/analysis/analysusUpdateTime.vue'
import CustomerServiceQrCode from './customerServiceQrCode'
import VideoMark from './videoMark.vue'
import contextmenu from './contextmenu.vue'
import ContextmenuDialog from './contextmenuDialog.vue'
import OnlineAnalysisDialog from './onlineAnalysisDialog.vue'
import OnlineNotAnalysisDialog from './onlineNotAnalysisDialog.vue'
// import AiTrain from './aiTrain.vue'
import Share from '/src/components/analysis/share.vue'
import complaintDialog from './complaintDialog.vue'
import ControlTabs from '/src/components/analysis/controlTabs.vue'
import WordTableCountNum from '/src/components/analysis/wordTableCountNum.vue'
import analysisContrastItem from './analysisLayout/contrast-only.vue'
import polish from '@/components/polish/index.vue'
import analysisMixin from '/src/mixins/analysisMixin.js'
import commonMixin from './analysisLayout/mixin/commonMixin'
import analysisTourMixin from './analysisLayout/tourConfig/analysisTourMixin'
import aiElf from '/src/components/analysis/aiElf/index.vue'
import Appear from '/src/components/analysis/appear.vue'
import aiDialog from '/src/components/analysis/ai/common/aiDialog.vue'
import ContrastLastTime from '@/components/analysis/contrastLastTime.vue'
import NotesMixin from '@/components/notes/mixins.js'
import clickHiden from '@/mixins/clickHiden';
import annotationDialog from '@/components/Annotation/annotationDialog.vue';
import auth from '@/mixins/auth';
import buyIn from "@/mixins/buyIn";
import exampleMixin from "@/mixins/exampleMixin";
import SliceAnalysis from "@/views/commonComponent/sliceAnalysis.vue";
import myUtils from "@/utils/utils";
import SliceTutorial from "@/views/modules/replay/component/sliceTutorial.vue";
import commonUtils from "@/utils/common";
import { trackEvent } from '@/utils/laTrack';
import {VERSION_TYPE} from "@/enum";
import { AI_WORKBENCH_PANELS } from '@/utils/aiAgentRoute'
import TradeTreeList from '@/components/DiscernSearchContainer/tradeTreeList.vue'
import upgradeToAgentTip from '@/mixins/upgradeToAgentTip'
import AiAgentWorkbenchGuide from '@/components/aiAgentWorkbenchGuide/index.vue'
export default {
    components: {
        SliceTutorial,
        SliceAnalysis,
        ContrastLastTime,
        analysisContrastItem,
        AnalysusTime,
        AnalysusUpdateTime,
        CustomerServiceQrCode,
        VideoMark,
        contextmenu,
        ContextmenuDialog,
        OnlineAnalysisDialog,
        OnlineNotAnalysisDialog,
        // AiTrain,
        Share,
        Appear,
        complaintDialog,
        ControlTabs,
        WordTableCountNum,
        polish,
        MarkDialog,
        aiElf,
        aiDialog,
        annotationDialog,
        TradeTreeList,
        AiAgentWorkbenchGuide
    },
    mixins: [analysisMixin, commonMixin, buyIn, analysisTourMixin, NotesMixin, clickHiden('analysis-item'), auth, exampleMixin, upgradeToAgentTip],
    props: {
        isShare: {
            type: Boolean,
            default: false
        },
        targetType: {
            type: String,
            default: ''
        },
        /**
         * @description 是否禁用监控 tab 的独立详情页跳转。
         * 用于云空间风格监控详情页，点击监控 tab 时仅做页内切换。
         */
        disableMonitorDetailJump: {
            type: Boolean,
            default: false
        }
    },
    provide() {
        return {
            analysisMain: this
        }
    },
    inject: ['appVnode', 'APP'],
    computed: {
        sceneText() {
            return (row) => (row.overView || row.remarks)?.slice(0, 6)
        },
        showMore() {
            return row => [row.remarks, row.overView].some(str => str?.length > 6)
        },
        isAiUrl() {
            if (this.targetType === 'webOnline' || this.targetType === 'online') {
                const path = this.$route.path.split('/').slice(0, -1).join('/');
                return `${path}/aiAnalysis`
            } else {
                if (this.fileInfo) {
                    const path = '/' + this.$route.path.split('/')[1]
                    return `${path}/aiAnalysis`
                }
                const path = '/' + this.$route.path.split('/')[1]
                return `${path}/aiAnalysis`
            }
        },
        getSourceId() {
            return this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.fileInfo?.fileId
        },
        getMonitorDetailSourceKind() {
            if (this.isWebOnline || this.isOnline) return 'online'
            if (this.sentenceMarkData?.fileInfo?.fileId) return 'uploadFile'
            if (this.sentenceMarkData?.uploadFile?.fileId) return 'uploadFile'
            const path = String(this.$route?.path || '')
            if (path.startsWith('/uploadVideo') || path.startsWith('/uploadText') || path.startsWith('/uploadSlice') || path.startsWith('/uploadShort')) {
                return 'uploadFile'
            }
            return 'analysis'
        },
        getReplayType() {
            return myUtils.getReplayType(this.sentenceMarkData)
        },
        isReplay(){
            return this.getReplayType === 'replayAll'
        },
        isFile(){
            return this.getReplayType === 'fileAll'
        },
        isSection(){
            return this.getReplayType === 'replaySection'
        },
        isShort(){
            return this.getReplayType === 'replayShort'
        },
        isFileSection(){
            return this.getReplayType === 'fileSection'
        },
        isFileShort(){
            return this.getReplayType === 'fileShort'
        },
        playUrl() {
            return this.sentenceMarkData?.playUrl
        },
        getTabs() {
            return [
                {
                    label: 'AI数据识图', name: 'd', hide: () => {
                        return !this.getSourceId || this.isShort || this.versionType === VERSION_TYPE.PURE
                    }
                },
            ]
        },
        versionType(){
            return this.$store.getters.getVersionType
        },
        tradeTipVisible() {
            if (!this.toolbarRef) return false
            return this.toolbarRef.isHandCloseStatus !== undefined ? this.toolbarRef.isHandCloseStatus : this.toolbarRef.isShowRecommend
        },
        showCopyTextBtn() {
            return this.versionType === VERSION_TYPE.AGENT && !this.isCompare && !this.isNotes && this.activeNameLocal !== 'text'
        },
        /**
         * @description 监控详情页跳转统一开关。
         * 浏览器云空间详情页不会返回客户端版本类型，因此 webOnline/online 场景按 Agent 能力放行。
         * 纯净版、对比态、分享态及短视频/切片场景仍保持原有限制。
         * @returns {boolean}
         */
        canOpenMonitorDetail() {
            return (this.versionType === VERSION_TYPE.AGENT || this.isWebOnline || this.isOnline)
                && !this.$store.getters.isPure
                && !this.isCompare
                && !this.isShare
                && !this.isShort
                && !this.isSection
                && !!this.getSourceId
        },
        aiAgentGuideUserId() {
            return String(this.$store?.state?.userInfo?.id || '')
        },
        canShowAiAgentGuide() {
            return this.versionType === VERSION_TYPE.AGENT
                && !this.isShare
                && !this.isWebOnline
                && !this.isCompare
        }
    },
    data() {
        return {
            VERSION_TYPE,
            tradeId: '1',
            toolbarRef: null,
            tradeIdProxy: '',
            unwatchToolbarTradeId: null,
            activeNameLocal: 'text',

            shareUrl: '',

            onlineFileInfo: {},
            onlineNotAnalysisDialogVisible: false,
            onlineAnalysisDialogVisible: false,
            keywordDialogVisible: false,
            complaintDialogVisible: false,

            // 存放选中的文本
            selectedText: '',

            // 自定义菜单
            contextmenuVisible: false,

            customerServiceQrCodeVisible: false, // 显示客服二维码弹窗
            markDialogVisible: true, // 标注资源确定弹窗
            // 播放器参数
            // 关键词/敏感词数据
            wordsInfo: {},

            sufficient: false, // 资源是否足够
            duration: '', // 视频时长，单位：分钟
            wordNum: '', // 文字总数量
            userProperty: {}, // 用户资产信息
            markResourceType: '', // 标注的来源类型 0：初次标注 1：换行业标注
            popoverVisible: [],
            toAi: false,
            isRepair: false,
            annotation: false,
            questionContent: 0,
            textScriptInfo:{},
            aiAgentGuideVisible: false
        }
    },
    watch: {
        '$route.query.activeName':{
            handler (newVal) {
               if(newVal){
                   this.$nextTick(() => {
                       this.$refs?.analysis?.toActiveText?.('text')
                       setTimeout(()=>{
                           this.$refs.ControlTabs?.selectActiveName?.(newVal)
                           this.$nextTick(() => {
                               this.clearActiveNameFromUrl()
                           })
                       },500)
                   })
               }
            },
            deep: true,
            immediate: true,
        }
    },
    mounted() {
        // 初始化视频标记状态
        this.initMarkStatus()
        this.buyInExpired()
        this.resolveToolbarRef()
        this.handleActiveNameChange(this.$refs?.analysis?.activeName || 'text')
        this.checkAndShowAiAgentGuide()
    },
    activated() {
        this.initMarkStatus()
        this.resolveToolbarRef()
        this.handleActiveNameChange(this.$refs?.analysis?.activeName || 'text')
        this.checkAndShowAiAgentGuide()
    },
    beforeDestroy() {
        if (this.toAi) { return }
        this.APP.refresh()
    },
    destroyed() { },
    methods: {
        handleActiveNameChange(val) {
            this.activeNameLocal = val || 'text'
            if (this.activeNameLocal !== 'text' && this.isNotes) {
                this.quitNotes()
            }
            if (this.activeNameLocal === 'productData') {
                this.$nextTick(() => {
                    this.$refs?.aiElfRef?.toBorder?.()
                })
            }
            if (
                this.enableSwappedMonitorJump?.(this.activeNameLocal)
            ) {
                this.$nextTick(() => {
                    this.$refs?.analysis?.toActiveText?.('text')
                    this.activeNameLocal = 'text'
                })
                this.$emit('monitorDetailJump', {
                    id: this.getSourceId,
                    targetType: this.targetType,
                    reportType: this.activeNameLocal,
                    sourceKind: this.getMonitorDetailSourceKind,
                    sceneType: this.sentenceMarkData?.sceneType
                })
            }
        },
        enableSwappedMonitorJump(activeName) {
            return ['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(activeName)
                && this.canOpenMonitorDetail
                && !this.disableMonitorDetailJump
        },
        handleCopyText() {
            this.toolbarRef?.copyText?.()
        },
        resolveToolbarRef() {
            this.$nextTick(() => {
                const next = this.$refs?.analysis?.getToolbarVnode?.()
                if (!next) return
                if (next === this.toolbarRef) return
                this.toolbarRef = next
                if (typeof this.unwatchToolbarTradeId === 'function') this.unwatchToolbarTradeId()
                this.unwatchToolbarTradeId = this.$watch(() => this.toolbarRef?.tradeId, (val) => {
                    if (val !== undefined) this.tradeIdProxy = val
                }, { immediate: true })
            })
        },
        handleTradeTreeLoad(tree) {
            this.toolbarRef?.treeLoad?.(tree)
        },
        handleTradeListChange(id) {
            this.toolbarRef?.setTradeId?.(id)
            this.toolbarRef?.treeChange?.(id)
        },
        handleCancelRecommend() {
            this.toolbarRef?.cancelRecommend?.()
        },
        handleReplaceIndustry() {
            this.toolbarRef?.replaceIndustry?.()
        },
        clearActiveNameFromUrl() {
            const newQuery = { ...this.$route.query };
            delete newQuery.activeName;
            this.$router.replace({ query: newQuery });
        },
        isReplayDetailTrackPage() {
            return this.isReplay && !this.isCompare && !this.isShare && !this.isWebOnline && !this.isOnline && !this.isShort && !this.isSection;
        },
        trackReplayDetailEvent(code) {
            if (this.isReplayDetailTrackPage()) {
                trackEvent(code);
            }
        },
        handleShareAnalysis() {
            this.trackReplayDetailEvent('P001_A001');
            this.shareAnalysis();
        },
        handleCopyShareAnalysisLink() {
            this.trackReplayDetailEvent('P001_A001');
            this.copyShareAnalysisLink();
        },
        handleExportScript() {
            this.trackReplayDetailEvent('P001_A002');
            this.exportScript();
        },
        handleContrastData() {
            this.trackReplayDetailEvent('P001_A003');
            this.contrastData();
        },
        handleSelectSliceAnalysis(rangeSecond) {
            this.trackReplayDetailEvent('P001_A004');
            this.selectSliceAnalysis(rangeSecond);
        },
        handleStartTour() {
            this.trackReplayDetailEvent('P001_A005');
            this.startTour();
        },
        handleStartNotes() {
            this.trackReplayDetailEvent('P001_A006');
            this.startNotes();
        },
        /**
         * @description 获取 AI 智能体工作台引导弹窗的本地存储 key。
         * @returns {string}
         */
        getAiAgentGuideStorageKey() {
            return 'aiAgentWorkbenchGuideState'
        },
        /**
         * @description 获取下一次本地零点重置时间戳。
         * @param {Date} now 当前时间
         * @returns {number}
         */
        getAiAgentGuideNextResetAt(now = new Date()) {
            const next = new Date(now)
            next.setHours(24, 0, 0, 0)
            return next.getTime()
        },
        /**
         * @description 读取引导弹窗存储。
         * @returns {Object}
         */
        getAiAgentGuideStorage() {
            try {
                return JSON.parse(localStorage.getItem(this.getAiAgentGuideStorageKey()) || '{}')
            } catch (e) {
                return {}
            }
        },
        /**
         * @description 写入引导弹窗存储。
         * @param {Object} storage 存储对象
         * @returns {void}
         */
        setAiAgentGuideStorage(storage = {}) {
            localStorage.setItem(this.getAiAgentGuideStorageKey(), JSON.stringify(storage || {}))
        },
        /**
         * @description 归一化当前用户的每日弹窗状态。
         * 过了本地零点后会自动重置展示次数。
         * @returns {{shownCount: number, resetAt: number}}
         */
        getAiAgentGuideUserState() {
            const userId = this.aiAgentGuideUserId
            const now = Date.now()
            const storage = this.getAiAgentGuideStorage()
            const current = storage?.[userId] || {}
            const resetAt = Number(current.resetAt || 0)
            if (!resetAt || now >= resetAt) {
                return {
                    shownCount: 0,
                    resetAt: this.getAiAgentGuideNextResetAt(new Date(now))
                }
            }
            return {
                shownCount: Number(current.shownCount || 0),
                resetAt
            }
        },
        /**
         * @description 记录当前用户今日已弹出过引导弹窗。
         * @returns {void}
         */
        markAiAgentGuideShown() {
            const userId = this.aiAgentGuideUserId
            if (!userId) return
            const storage = this.getAiAgentGuideStorage()
            const current = this.getAiAgentGuideUserState()
            storage[userId] = {
                shownCount: 1,
                resetAt: current.resetAt || this.getAiAgentGuideNextResetAt()
            }
            this.setAiAgentGuideStorage(storage)
        },
        /**
         * @description 检查并展示 AI 智能体工作台引导弹窗。
         * 满足“分析详情页每日首次进入展示一次”的规则时才打开。
         * @returns {void}
         */
        checkAndShowAiAgentGuide() {
            if (!this.canShowAiAgentGuide) return
            if (!this.aiAgentGuideUserId) return
            const current = this.getAiAgentGuideUserState()
            if (current.shownCount >= 1) return
            this.aiAgentGuideVisible = true
        },
        /**
         * @description 打开 AI 智能体工作台，优先复用客户端原生能力。
         * @returns {void}
         */
        openAiAgentWorkbench() {
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb()
                return
            }
            this.$router.push({
                path: '/aiAssistant'
            })
        },
        /**
         * @description 关闭 AI 智能体工作台引导弹窗。
         * @returns {void}
         */
        handleCloseAiAgentGuide() {
            this.markAiAgentGuideShown()
            this.aiAgentGuideVisible = false
        },
        /**
         * @description 点击立即体验后关闭引导并打开 AI 智能体工作台。
         * @returns {void}
         */
        handleExperienceAiAgentGuide() {
            this.markAiAgentGuideShown()
            this.aiAgentGuideVisible = false
            this.openAiAgentWorkbench()
        },
        handleTabsClick(name) {
            const tabCodeMap = {
                a: 'P001_A007',
                b: 'P001_A008',
                c: 'P001_A009',
                d: 'P001_A010',
                f: 'P001_A011',
                g: 'P001_A012',
                h: 'P001_A013',
            };
            const code = tabCodeMap[name];
            if (code) {
                this.trackReplayDetailEvent(code);
            }
        },
        trackAiEntryEvent(type) {
            const typeCodeMap = {
                assistant: 'P001_A014',
                currentDiagnoseNew: 'P001_A014',
                violation: 'P001_A015',
                textAssistant: 'P001_A016',
                dataBoard: 'P001_A014',
            };
            const code = typeCodeMap[type];
            if (code) {
                this.trackReplayDetailEvent(code);
            }
        },
        handleSceneDetailClick() {
            this.trackReplayDetailEvent('P001_A018');
        },
        setTextScriptInfo(data){
            this.textScriptInfo = data
        },
        howToSlice(){
            this.$refs.slice_tutorial?.open()
        },
        toAnniversary(){
            if(this.isWebOnline){
                this.$router.push({
                    path: '/web-anniversary'
                })
            }else{
                this.$router.push({
                    path: '/anniversary'
                })
            }
        },
        buyInExpired(){
            if(this.targetType !== 'online' && this.targetType !== 'webOnline'){
                this.watchAuthorizedBuyInExpired(()=>{
                    this.$nextTick(()=>{
                        this.APP.$refs?.buy_in?.openDimension(this.sentenceMarkData?.anchorInfo?.SecUid)
                    })
                })
            }
        },
        isNotesEditer(next){
            return new Promise((res)=>{
                this.$refs?.analysis?.isNotesEditer((status)=>{
                    res(status);
                }, next)
            })
        },
        getAnnotationList(){
            this.$nextTick(()=>{
                this.$refs.analysis.getAnnotation();
            })
        },
        annotationContextmenu(val){
            this.$nextTick(()=>{
                this.$refs.annotationDialog.show({
                    data: {
                        ...val,
                        sentenceMarkData: this.sentenceMarkData
                    }
                })
            })
        },
        annotationHandler(val){
            this.annotation = val;
        },
        textTypeChange(val){
            // this.questionContent = val;// 原来的值是undefined
            this.complete();
            if ([1,3].includes(val)) this.isAiElfChange()
            this.resolveToolbarRef()
        },
        contrastData () {
            this.$refs.lastTime?.openDialog()
        },
        exportScript() {
            this.$store.commit("setActionKey", new Date().getTime());
        },
        repairVideo(val) {
            this.isRepair = val
            this.$emit('repairVideo', val)
        },
        notesEditer(val){
            this.$emit('notesEditer', val)
        },
        isRepairStop(callback) {
            this.$nextTick(() => {
                this.$refs?.analysis?.isRepairStop(callback)
            })
        },
        brushChange(data) {
            if(this.ifExample('assistant')){return;}
            if (this.targetType === 'webOnline') {
                this.$emit('webElfClick', 'assistant', { data1: data.data1?.join(','), data2: data.data2?.join(',') })
                return
            }
            
            this.onTask({}, { data1: data.data1?.join(','), data2: data.data2?.join(',') })
        },
        taskSlice({pureText,contenxtData}){
            this.$refs['slice-analysis'].open({
                pureText,
                contenxtData
            })
        },
        checkViolation(){
            this.showUpgradeToAgentConfirm('AI查违规功能可以排查直播间违禁词和违禁语句。')
        },
        selectSliceAnalysis(rangeSecond){
            if(this.isPureRecordingVersion()){
                this.showUpgradeToAgentConfirm('切片功能可以对直播间的高光片段进行切片。')
            }else {
                this.$refs['slice-analysis'].open({
                    text: '',
                    rangeSecond
                })
            }
        },
        editSliceAnalysis(){
            const {videoSliceInfo} = this.sentenceMarkData.videoInfo || this.sentenceMarkData.uploadFile || {}
            const startSecond = videoSliceInfo?.startMillisecond || 0
            const endSecond = videoSliceInfo?.endMillisecond || 0
            this.selectSliceAnalysis({data1: [(startSecond / 1000).toFixed(0), (endSecond / 1000).toFixed(0)]})
        },
        viewAnalysis() {
            const parentInfo = this.sentenceMarkData?.videoInfo?.parentVideoInfo || this.sentenceMarkData?.uploadFile?.parentFileInfo
            const id = parentInfo?.videoId || parentInfo?.fileId

            if (!id) return this.$message.error('未找到原视频信息')
            if (this.isFileSection || this.isFileShort) {//文件
                this.$router.replace({
                    path: '/uploadVideo/fileUploadAnalysis',
                    query: {id}
                })
            } else {
                this.$router.replace({
                    path: '/replay/analysis',
                    query: {id}
                })
            }
        },
        aiAnalysisData(data) {
            this.trackAiEntryEvent(data.type);
            if(this.ifExample( data.type)){return;}
            if (this.targetType === 'webOnline') {
                this.$emit('webElfClick', data.type)
                return
            }
            this.onTask({}, data)
        },
        isAiElfChange() {
            this.$refs.aiElfRef?.toBorder();
        },
        onTask(data, o = {}) {
            this.toAi = true
            this.$router.push({
                path: this.isAiUrl,
                query: {
                    id: this.getAnalysisId,
                    type: data.type,
                    value: data.value,
                    fileType: this.isVideoId ? 'video' : 'file',
                    ...o
                }
            })
        },
        aiDialogSubmit() {
            if (this.isWebOnline) {
                this.APP?.toIfupanWebsite()
            } else {
                this.$router.push({
                    path: '/replay'
                })
            }
        },
        ifExample(type){
            if(this.$route.query.type === 'example'){
                setTimeout(()=>{
                    this.toExampleAi(type, this.$route.query.id)
                }, 100)
                return true;
            }
        },
        elfClick(type) {
            this.trackAiEntryEvent(type);
            if (type === 'currentDiagnoseNew') {
                this.openCurrentAiAgentWorkbench()
                return
            }
            if (type === 'multiDiagnose') {
                this.openAiAgentWorkbench()
                return
            }
            if(this.ifExample(type)){return;}
            if (this.targetType === 'webOnline') {
                this.$emit('webElfClick', type)
                return
            }
            this.toAi = true
            this.$router.push({
                path: this.isAiUrl,
                query: {
                    id: this.getAnalysisId,
                    type: type,
                    fileType: this.isVideoId ? 'video' : 'file'
                }
            })
        },
        /**
         * @description 打开 AI 智能体工作台，详情页“诊断多场”场景只锁定直播间，不锁定具体场次。
         * @returns {void}
         */
        openAiAgentWorkbench() {
            const secUid = String(this.sentenceMarkData?.anchorInfo?.SecUid || this.sentenceMarkData?.anchorInfo?.secUid || '').trim()
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb({
                    secUid,
                    openInBrowserWindow: this.targetType === 'webOnline'
                })
                return
            }
            if (this.versionType === VERSION_TYPE.PURE) {
                this.$router.push({ path: '/dataAnalysis' })
                return
            }
            this.$router.push({ path: '/aiAssistant' })
        },
        /**
         * @description 获取详情页“AI 诊断本场（新）”所需的工作台跳转参数。
         * @returns {{ secUid: string, videoId: string, cue: string }}
         */
        getCurrentAgentWorkbenchParams() {
            const secUid = String(
                this.sentenceMarkData?.anchorInfo?.SecUid
                || this.sentenceMarkData?.anchorInfo?.secUid
                || ''
            ).trim()
            const videoId = String(
                this.sentenceMarkData?.videoInfo?.VideoId
                || this.sentenceMarkData?.videoInfo?.videoId
                || ''
            ).trim()
            return {
                secUid,
                videoId,
                cue: '0'
            }
        },
        /**
         * @description 打开 AI 智能体工作台，详情页“AI 诊断本场（新）”场景需要同时锁定直播间与当前场次，并默认切到运营助手。
         * @returns {void}
         */
        openCurrentAiAgentWorkbench() {
            const { secUid, videoId, cue } = this.getCurrentAgentWorkbenchParams()
            if (!(secUid && videoId)) {
                this.$message.warning('当前直播暂未生成 AI 诊断入口')
                return
            }
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb({
                    panel: AI_WORKBENCH_PANELS.ROOM,
                    secUid,
                    videoId,
                    cue,
                    openInBrowserWindow: this.targetType === 'webOnline'
                })
                return
            }
            if (this.versionType === VERSION_TYPE.PURE) {
                this.$router.push({ path: '/dataAnalysis' })
                return
            }
            this.$router.push({
                path: '/aiAssistant',
                query: {
                    panel: AI_WORKBENCH_PANELS.ROOM,
                    secUid,
                    videoId,
                    cue
                }
            })
        },
        setWordsInfo(o) {
            this.$nextTick(() => {
                this.$refs.analysis.setWordsInfo({
                    markCrux: true,
                    markSensitive: false,//this.isVideoId ? false : true,
                    ...o
                })
            })
        },
        markWords(markWord) {
            this.wordsInfo = markWord
        },

        // 关键词/敏感词table显示过滤
        filterData(item) {
            // return true;
            let t = item.wordsType;
            if(t === 0) {
                return this.wordsInfo.markSensitive;
            }else{
                // 关键词不在过滤/永久显示
                return true;
            }
            // 2.5.1版本修改显示逻辑
            // let markCrux = this.wordsInfo.markCrux && t == 1
            // let markSensitive = this.wordsInfo.markSensitive && t == 0
            // return (markCrux || markSensitive)
        },
        setMarkState() {
            this.setWordsInfo()
            this.$nextTick(() => {
                // 检查是否已经上传云空间
                if (this.videoInfo?.ShareUrl || this.fileInfo?.shareUrl) {
                    this.shareUrl = this.videoInfo?.ShareUrl || this.fileInfo?.shareUrl
                }
            })
        },
        // 初始化视频标记状态
        initMarkStatus() {
            // 如果是视频或音频，默认显示词语列表
            if ((this.isVideoId) || (this.fileInfo?.fileType == 0 || this.fileInfo?.fileType == 1)) {
                this.setMarkState()
            } else {
                // 是文本文件，已经标注过的，才显示词语列表
                this.setMarkState()
            }
            if(this.versionType === VERSION_TYPE.PURE){
                this.$refs.ControlTabs?.setFold?.(true)
            }
        },
        //右键打开自定义菜单
        rightTickContextMenu({event,data}) {
            this.contextmenuVisible = true
            let notModel = []
            if (this.isAiElf && !this.isWebOnline) {
                if (this.playUrl && this.isReplay && !this.isNotes&& !this.isOnline) {
                    notModel = []
                } else {
                    notModel = ['slice']
                }
            } else {
                notModel = ['ai1', 'ai2', 'slice']
            }
            this.$nextTick(() => {
                this.$refs.contextmenu.rightContextMenu(event, {
                    id: this.getAnalysisId,
                    sourceType: this.isVideoId ? 0 : 1,
                    notModel: notModel,
                    annotation: this.annotation && this.isSlefAuth,
                    data
                })
            })
        },
        // 取消消耗资源标注
        cancalMark() {
            this.$refs.markDialog?.hide()
            if (this.markResourceType == 1) {
                this.setTadeId(this.oldTradeId)
                this.$refs.analysis.dropDown()
            }
        },
        // 使用资源
        confirmUseMark() {
            if (this.markResourceType == 1) {
                // 二次选择行业标注
                this.tradeChange()
                this.$refs.markDialog?.hide()
            } else {
                // 首次标注
                if (this.videoInfo && this.videoInfo.VideoId) {
                    // 视频
                    let requestData = {
                        videoId: this.videoInfo.VideoId,
                        duration: this.duration
                    }
                    this.$httpClient.video.confirmUseMark(requestData).then(res => {
                        if (res.code == 0) {
                            this.$message.success('分析成功')
                            this.$refs.markDialog?.hide()
                            this.setMarkState()
                            this.countWords()
                        }
                    })
                } else {
                    // 文件
                    let requestData = {
                        fileId: this.fileInfo.fileId,
                        duration: this.duration ? this.duration : 0,
                        wordNum: this.wordNum ? this.wordNum : 0
                    }
                    this.$httpClient.uploadFile.confirmUseMark(requestData).then(res => {
                        if (res.code == 0) {
                            this.$message.success('分析成功')
                            this.$refs.markDialog?.hide()
                            this.setMarkState()
                            this.countWords()
                        }
                    })
                }
            }
        },
        // 显示客服二维码
        showCustomerServiceQrCode() {
            this.$refs.markDialog?.hide()
            this.customerServiceQrCodeVisible = true
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },

        // 复制分享链接
        copyShareAnalysisLink() {
            if (this.videoInfo?.UploadStatus === 1) {
                let shareUrl = this.shareUrl
                if (!this.shareUrl) {
                    shareUrl = commonUtils.assemblyShareUrl(`onlineAnalysis/0/${this.videoInfo?.VideoId}`)
                }
                this.appVnode.copyShareUrl(shareUrl)
                return
            }
            // this.onlineAnalysisDialogVisible = true;
            let vInt = this.$refs.analysis.getVideoMaxTime
            this.$nextTick(() => {
                this.$httpBack.userProperty.info({}).then(userPropertyRes => {
                    if (userPropertyRes.code == 0 && userPropertyRes.data) {
                        this.onlineFileInfo.duration = Math.floor(vInt / 60) <= 0 ? 1 : Math.floor(vInt / 60)
                        this.onlineFileInfo.fileSize = this.videoInfo.CloudStore
                        this.shareUrl = this.videoInfo.ShareUrl
                        // this.$refs.onlineAnalysisDialog.init(userPropertyRes.data, this.onlineFileInfo);
                        this.$emit('showUploadPop', userPropertyRes.data, this.onlineFileInfo, this.shareUrl, this.videoInfo, this.anchorInfo)
                    }
                })
            })
        },
        // 修改分享状态
        confirmShareAnalysis(vodUrl) {
            if (this.videoInfo && this.videoInfo.VideoId) {
                // 视频
                this.$httpBack.video.shareVideoToCloud({
                    videoId: this.videoInfo.VideoId,
                    onlineFileUrl: vodUrl
                }).then(res => {
                    if (res.code == 0 && res.data) {
                        this.$message.success('文件上传成功，可以复制分享链接啦')
                        this.videoInfo.UploadStatus = 1
                        this.videoInfo.ShareUrl = res.data
                        this.shareUrl = res.data
                    }
                })
            } else {
                // 文件
                this.$httpClient.uploadFile.shareAnalysis({
                    fileId: this.fileInfo.fileId,
                    onlineFileUrl: vodUrl
                }).then(res => {
                    if (res.code == 0) {
                        this.$message.success('文件上传成功，可以复制分享链接啦')
                        this.fileInfo.uploadStatus = 1
                        this.fileInfo.shareUrl = res.data
                        this.shareUrl = res.data
                    }
                })
            }
        },
        // 判断分享复盘资源是否足够
        checkOnlineProperty() {
            // 获取用户资产，判断资源是否足够
            this.$httpBack.userProperty.info({}).then(userPropertyRes => {
                this.userProperty = userPropertyRes.data
                if (this.userProperty.storageNum / 1024 >= this.onlineFileInfo.fileSize) {
                    // 存储空间足够
                    // this.onlineAnalysisDialogVisible = true;
                    this.$nextTick(() => {
                        // this.$refs.onlineAnalysisDialog.init(this.userProperty, this.onlineFileInfo);
                        this.$emit('showUploadPop', userPropertyRes.data, this.onlineFileInfo, this.shareUrl, this.videoInfo, this.anchorInfo)
                    })
                } else {
                    // 存储空间不足
                    this.onlineNotAnalysisDialogVisible = true
                    this.$nextTick(() => {
                        this.$refs.onlineNotAnalysisDialog.init(this.userProperty, this.onlineFileInfo)
                    })
                }
            })
        },
        // 分享复盘
        shareAnalysis() {
            // 获取视频时长
            let vInt = this.$refs.analysis.getVideoMaxTime
            this.onlineFileInfo = {};
            if (this.videoInfo && this.videoInfo.VideoId) {
                // 视频
                const loading = this.$loading({
                    lock: true,
                    text: '正在构建上传，请稍后...',
                    spinner: 'el-icon-loading',
                    background: 'rgba(0, 0, 0, 0.7)'
                })

                this.$httpClient.video.compress({ videoId: this.videoInfo.VideoId }).then(res => {
                    loading.close()
                    if (res.code == 0) {
                        vInt = res.data.duration
                        this.onlineFileInfo = res.data
                        this.onlineFileInfo.duration = Math.floor(vInt / 60) <= 0 ? 1 : Math.floor(vInt / 60)
                        // 获取用户资产，判断资源是否足够
                        this.checkOnlineProperty()
                    }
                }).catch(err => {
                    loading.close()
                })
            } else if (this.fileInfo.fileType == 0) {
                // 视频文件
                const loading = this.$loading({
                    lock: true,
                    text: '正在构建上传，请稍后...',
                    spinner: 'el-icon-loading',
                    background: 'rgba(0, 0, 0, 0.7)'
                })
                this.$httpClient.uploadFile.compress({ fileId: this.fileInfo.fileId }).then(res => {
                    loading.close()
                    if (res.code == 0) {
                        vInt = res.data.duration
                        this.onlineFileInfo = res.data
                        this.onlineFileInfo.duration = Math.floor(vInt / 60) <= 0 ? 1 : Math.floor(vInt / 60)
                        // 获取用户资产，判断资源是否足够
                        this.checkOnlineProperty()
                    }
                }).catch(err => {
                    loading.close()
                })
            } else if (this.fileInfo.fileType == 1) {
                // 音频文件
                this.onlineFileInfo.duration = Math.floor(vInt / 60) <= 0 ? 1 : Math.floor(vInt / 60)
                this.onlineFileInfo.fileSize = Math.floor(this.fileInfo.fileSize / 1024 / 1024)
                this.onlineFileInfo.filePath = this.fileInfo.nowPath
                this.checkOnlineProperty()
            } else {
                // 文本文件
                this.confirmShareAnalysis('')
            }
        },
        // 点击词语
        clickWord(word) {
            this.selectedText = word
            this.keywordDialogVisible = true
            this.$nextTick(() => {
                this.$refs.contextmenuDialog.init()
            })
        },

        setTadeId(id) {
            this.tradeId = id || '1'
            this.$refs.analysis.setTrade(id)
        },
        // 获取行业列表树形
        tradeTreeLoad() {
            this.setTadeId(this.videoInfo && this.videoInfo.VideoId ? this.videoInfo.TradeId : this.fileInfo.tradeId)
            this.oldTradeId = this.tradeId
        },
        treeChange(id) {
            this.setTadeId(id)
            this.enoughMarkProperty(1)
        },
        // 修改行业二次分析
        tradeChange() {
            this.oldTradeId = this.tradeId
            // 关闭级联列表下拉
            // this.$refs.tradeCascader.dropDownVisible = false;
            let analysisByTrade = null
            this.$refs.analysis.dropDown()
            if (this.videoInfo && this.videoInfo.VideoId) {
                // 录制的视频
                let requestData = {
                    videoId: this.videoInfo.VideoId,
                    tradeId: this.tradeId,
                    platformType: this.videoInfo.PlatformType
                }
                analysisByTrade = this.$httpClient.video.reAnalysisByTrade(requestData).then(res => {
                    if (res.code == 0 && res.data) {
                        this.$message.success('分析成功')
                        // 标注词语
                        this.setMarkState()
                        this.initAudioaAlyses(res.data)
                        this.countWords()
                    }
                })
            } else {
                // 上传的文件
                let requestData = {
                    fileId: this.fileInfo.fileId,
                    tradeId: this.tradeId,
                    platformType: this.fileInfo.platformType,
                    duration: this.duration ? this.duration : 0,
                    wordNum: this.wordNum ? this.wordNum : 0
                }
                analysisByTrade = this.$httpClient.uploadFile.reAnalysisByTrade(requestData).then(res => {
                    if (res.code == 0 && res.data) {
                        this.$message.success('分析成功')
                        // 标注词语
                        this.setMarkState()
                        this.initAudioaAlyses(res.data)
                        this.countWords()

                    }
                })
            }

            analysisByTrade.then(() => {

                this.$nextTick(() => {
                    this.$refs.analysis.refreshHandler('init')
                    this.$refs.ControlTabs?.refreshHandler('init')
                })
            })

        },

        // 判断是否有足够的标注资源，resourceType 来源类型 0：初次标注 1：换行业标注
        enoughMarkProperty(markResourceType) {
            this.markResourceType = markResourceType
            // 只有文本类型，才需要判断标注资源
            if (this.fileInfo && this.fileInfo?.fileType == 2) {
                this.$httpBack.userProperty.info({}).then(res => {
                    this.userProperty = res.data
                    if ((this.isVideoId) || (this.fileInfo && (this.fileInfo?.fileType == 0 || this.fileInfo?.fileType == 1))) {
                        // 按时长计费
                        let duration = Math.floor(this.$refs.analysis.getVideoMaxTime / 60) // 时长，分钟
                        duration = duration < 1 ? 1 : duration
                        this.duration = duration
                    } else {
                        // 按文本字数计费
                        let wordNum = this.fileInfo.fileWordNum
                        this.wordNum = wordNum
                    }
                    let sufficient = false
                    if (this.duration && this.duration > 0) {
                        if (this.userProperty.videoTaggingTime >= this.duration) {
                            sufficient = true
                        } else {
                            sufficient = false
                        }
                    } else {
                        if (this.userProperty.textTaggingWordCount >= this.wordNum) {
                            sufficient = true
                        } else {
                            sufficient = false
                        }
                    }
                    this.$refs.markDialog.show({
                        data: {
                            sufficient,
                            duration: this.duration,
                            wordNum: this.wordNum,
                            userProperty: this.userProperty
                        }
                    })
                })
            } else {
                this.tradeChange()
            }
        },
        // 标注/取消标注按钮回调
        notMark() {
            // 不允许标注，判断资源是否足够
            // this.enoughMarkProperty(0);
        },

        // 整理关键词、敏感词数据
        countWords() {
            this.$refs.analysis.countWords()
        },

        setPlayerReadied(second) {
            this.$refs.analysis.onPlayerReadied(second)
        },

        // 申诉弹窗
        Complaint() {
            const secUid = this.sentenceMarkData.anchorInfo.SecUid
            const videoId = this.sentenceMarkData.videoInfo.VideoId
            this.complaintDialogVisible = true
            this.$nextTick(() => {
                this.$refs.complaint.init(secUid, videoId)
            })
        },
        closeParagraph() {
            this.popoverVisible = []
        },
        exchangeParagraph(scope, close) {
            const index = scope.$index
            const nextVisible = close ?? !this.popoverVisible[index]
            if (nextVisible) {
                this.trackReplayDetailEvent('P001_A017');
            }
            this.popoverVisible.forEach((item, i) => {
                if (i !== index) {
                    this.$set(this.popoverVisible, i, false)
                }
            })

            this.$set(this.popoverVisible, index, nextVisible)

            this.$nextTick(() => {
                requestAnimationFrame(() => {
                    this.$nextTick(() => {
                        const popover = this.$refs.popoverRef
                        if (popover?.updatePopper) {
                            popover.updatePopper()
                        }
                    })
                })
            })
        },
        reportParagraph(){
            this.$refs.appear_ref?.reportParagraph()
        }
    },
}
</script>
<style scoped lang="less">
/* 联系客服 */
.contact-kefu {
    display: flex;
    align-items: center;
    cursor: pointer;
}

.action-btns {
    height: 28px;
    position: relative;
    padding-left: 36px;
    &::before {
        content: '';
        position: absolute;
        left: 24px;
        top: 7px;
        height: 16px;
        border-left: 1px #DCDCDC solid;
    }
}

.analysis-item-action-wrapper {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
}

.analysis-item-action-wrapper {
    .action-btns {
        padding-left: 0;
        &::before {
            display: none;
        }
    }
}

.analysis-item-tabs-actions {
    padding-left: 0;
    &::before {
        display: none;
    }
}

.analysis-item-anchor-right {
    display: flex;
    align-items: center;
}

.analysis-item-anchor-divider {
    height: 16px;
    border-left: 1px #DCDCDC solid;
    margin: 0 12px;
}

.analysis-item-trade {
    position: relative;
    margin-right: 0;

    ::v-deep .tradeCascader {
        .el-input__inner {
            height: 28px;
            line-height: 28px;
            font-size: 12px;
            background: #F5F5F5;
            border-color: transparent;
        }

        .el-input__icon {
            line-height: 28px;
        }
    }

    .tipsContainer {
        background: #fff;
        box-shadow: 0px 3px 6px 0px rgba(45, 81, 139, 0.1), 0px 11px 11px 0px rgba(45, 81, 139, 0.09), 0px 25px 15px 0px rgba(45, 81, 139, 0.05), 0px 44px 18px 0px rgba(45, 81, 139, 0.01), 0px 69px 19px 0px rgba(45, 81, 139, 0);
        padding: 12px 12px 0 12px;
        width: 230px;
        border-radius: 6px;
        position: absolute;
        top: 35px;
        left: 15px;
        z-index: 100;
    }
}
.txyAi {
    // margin-top: -3px;
    display: flex;
    // flex-direction: column;
    flex-direction: row;
    // justify-content: flex-end;
    align-items: center;
    vertical-align: bottom;
}

.tips_action {
    position: absolute;
    top: 10px;
    right: 4px;
    display: flex;
    z-index: 1;
    align-items: center;
}
</style>

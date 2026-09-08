<!--
@description 录制列表页：包含录制列表检索、对比、AI 监控报告（话术质检/互动巡检/还原度）入口与状态展示等核心能力。
注意：
1) AI 监控相关字段来自 scriptMonitor 接口封装（request-api-back.js），接口字段变更需同步调整本页面取值；
2) 本页面逻辑较重，新增/修改复杂逻辑时请补充必要的单行注释，便于后续 AI/人类维护与升级理解。
3) 【废弃】2026-08-03 已移除"生成内容诊断/生成数据诊断"列表入口（含dropdown和更多菜单），aiContentReport/aiDataReport 抽屉组件保留但不再从列表唤起。
-->
<template>
    <div class="recordListContainer">
        <GuardAction ref="aiMonitorGuard" style="display:none">
            <template #default>
                <span></span>
            </template>
        </GuardAction>
        <el-row type="flex">
            <el-col :style="{width: toggleMenu ? '70px' : '184px',transition: 'width 0.3s ease'}" v-if="!aIFinish">
                <!-- 左边 -->
                <compere @click="compereClick" ref="compere" :replayType="replayType" :toggleMenu="toggleMenu">
                    <div class="flex items-center" :style="{color: '#515C73','padding-bottom': toggleMenu?'0':'8px'}">
                        <i :class="{'cursor-pointer':true,'text-xl':true,'el-icon-s-fold':!toggleMenu,'el-icon-s-unfold':toggleMenu }"
                           @click="toggleCompere"></i>
                        <span class="text-xs cursor-pointer pd-l6 whitespace-nowrap" @click="toggleCompere">{{toggleMenu?'展开':'收起菜单'}}</span>
                    </div>
                </compere>
            </el-col>
            <el-col style="flex: 1;min-width: 0;">
                <!-- 右边 -->
                <div class="recordListRightContainer h100" :style="{marginLeft:aIFinish? 0: '10px'}">
                    <CoreTable v-if="isExampleList" isFlex :searchConfig="computeFormConfig"
                               notFirstGet :buffer="aIFinish?'aIFinish':'recordList'" :table-size="'mini'"
                               :menuConfig="computeMenuConfig"
                               :getDataApi="getTableList" ref="table" :column="computeColumn" :table-select="true"
                               row-key="videoId" @visibleChange="visibleChange"
                               @search="handleSearch" @table-select="tableSelect" @deletes="delConfirm"
                               :dataCallback="dataCallback" :resData="resData"
                               :table-height="`calc(100vh - ${contrastList.length ? 262 : 210}px)`">
                        <template #tableTop>
                            <div v-if="contrastList.length" class="contrast-box main-bg">
                                <el-tag v-for="item in contrastList" :key="item.videoId || item.videoName" closable @close="cancelContrast(item)" class="brs-40">
                                    {{ item.videoName }}
                                </el-tag>
                                <afp-button v-show="contrastList.length>=2"  type="primary"
                                           @click="modalContrast">开始对比
                                </afp-button>
                            </div>
                        </template>
                        <template #ref-btn>
                            <el-button type="text" @click="handleRefresh" style="padding: 0;height: 22px">刷新</el-button>
                            <el-button type="text" @click="howToSlice" style="padding: 0;height: 22px" v-if="['replaySection','replayShort'].includes(replayType)">如何生成切片</el-button>
                        </template>
                        <template #viewersNum="{row}">
                            <div class="font-s14">
<!--                                <div>场观: {{ conversion(row?.observationNum) }}</div>-->
<!--                                <div>销售: {{ dataView(row, ['volumeStart', 'volumeEnd']) }}</div>-->
                                <Popover :item="row"/>
                            </div>
                        </template>
                        <template #definition="{row}">
                            <span
                                v-for="item in definitionList">
                                <span v-if="row.definition===item.value" :style="{ color: item.color }">{{ item.label }}</span>
                            </span>
                        </template>
                        <template #speechQc="{row}">
                            <div class="aiMonitorCell">
                                <template v-if="getAiMonitorState(row).qcReportStatus === 2">
                                    <el-badge :is-dot="getAiMonitorState(row).qcUnread" class="aiMonitorBadge">
                                        <AiMonitorPreviewContent
                                            :content="getAiMonitorState(row).qcPreview"
                                            preview-type="qc"
                                            parse-mode="structured"
                                            with-stats-style
                                            @view="openAiReport('qc', row)"
                                        />
                                    </el-badge>
                                </template>
                                <template v-else>
                                    <div class="aiMonitorActions">
                                        <template v-if="canShowEnableAiMonitor('qc', row)">
                                            <el-button type="text" class="aiMonitorLink" @click="enableAiMonitor('qc', row)">自动质检</el-button>
                                        </template>
                                        <template v-if="Number(row.analysisStatus) !== 2">
                                            <span class="aiMonitorLinkDisabled">-</span>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).qcReportStatus === 1">
                                            <span class="aiMonitorLinkDisabled">生成中</span>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).qcReportStatus === 4">
                                            <el-button type="text" class="aiMonitorLink" :disabled="true">{{ getAiMonitorState(row).qcUnavailableReason || '不可生成' }}</el-button>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).qcReportStatus === 3">
                                            <el-button type="text" class="aiMonitorLink" @click="generateAiReport('qc', row)">重试生成</el-button>
                                        </template>
                                        <template v-else>
                                            <el-button type="text" class="aiMonitorLink" @click="generateAiReport('qc', row)">生成报告</el-button>
                                        </template>
                                    </div>
                                </template>
                            </div>
                        </template>

                        <template #scriptRestore="{row}">
                            <div class="aiMonitorCell">
                                <template v-if="getAiMonitorState(row).restoreReportStatus === 2">
                                    <el-badge :is-dot="getAiMonitorState(row).restoreUnread" class="aiMonitorBadge">
                                        <AiMonitorPreviewContent
                                            :content="getAiMonitorState(row).restorePreview"
                                            preview-type="restore"
                                            parse-mode="text"
                                            with-stats-style
                                            @view="openAiReport('restore', row)"
                                        />
                                    </el-badge>
                                </template>
                                <template v-else>
                                    <div class="aiMonitorActions">
                                        <template v-if="canShowEnableAiMonitor('restore', row)">
                                            <el-button type="text" class="aiMonitorLink" @click="enableAiMonitor('restore', row)">自动监控</el-button>
                                        </template>
                                        <template v-if="Number(row.analysisStatus) !== 2">
                                            <span class="aiMonitorLinkDisabled">-</span>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).restoreReportStatus === 1">
                                            <span class="aiMonitorLinkDisabled">生成中</span>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).restoreReportStatus === 4">
                                            <el-button type="text" class="aiMonitorLink" :disabled="true">{{ getAiMonitorState(row).restoreUnavailableReason || '不可生成' }}</el-button>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).restoreReportStatus === 3">
                                            <el-button type="text" class="aiMonitorLink" @click="generateAiReport('restore', row)">重试生成</el-button>
                                        </template>
                                        <template v-else>
                                            <el-button type="text" class="aiMonitorLink" @click="generateAiReport('restore', row)">生成报告</el-button>
                                        </template>
                                    </div>
                                </template>
                            </div>
                        </template>

                        <template #interactionInspect="{row}">
                            <div class="aiMonitorCell">
                                <template v-if="getAiMonitorState(row).inspectReportStatus === 2">
                                    <el-badge :is-dot="getAiMonitorState(row).inspectUnread" class="aiMonitorBadge">
                                        <AiMonitorPreviewContent
                                            :content="getAiMonitorState(row).inspectPreview"
                                            preview-type="inspect"
                                            parse-mode="text"
                                            with-stats-style
                                            @view="openAiReport('inspect', row)"
                                        />
                                    </el-badge>
                                </template>
                                <template v-else>
                                    <div class="aiMonitorActions">
                                        <template v-if="canShowEnableAiMonitor('inspect', row)">
                                            <el-button type="text" class="aiMonitorLink" @click="enableAiMonitor('inspect', row)">自动巡检</el-button>
                                        </template>
                                        <template v-if="Number(row.analysisStatus) !== 2">
                                            <span class="aiMonitorLinkDisabled">-</span>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).inspectReportStatus === 1">
                                            <span class="aiMonitorLinkDisabled">生成中</span>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).inspectReportStatus === 4">
                                            <el-button type="text" class="aiMonitorLink" :disabled="true">{{ getAiMonitorState(row).inspectUnavailableReason || '不可生成' }}</el-button>
                                        </template>
                                        <template v-else-if="getAiMonitorState(row).inspectReportStatus === 3">
                                            <el-button type="text" class="aiMonitorLink" @click="generateAiReport('inspect', row)">重试生成</el-button>
                                        </template>
                                        <template v-else>
                                            <el-button type="text" class="aiMonitorLink" @click="generateAiReport('inspect', row)">生成报告</el-button>
                                        </template>
                                    </div>
                                </template>
                            </div>
                        </template>
                        <template #aiReport="{row}">
                            <div style="position: relative; display: inline-block;">
                                <el-badge :is-dot="row.isDataDiagnosisRead===0" class="report-badge">
                                    <el-dropdown @command="(command)=>viewAiReport(command,row)" trigger="click" :disabled="!isShowViewAnalysis(row)" @visible-change="(visible)=>visibleReportChange(visible,row)">
                                        <span class="el-dropdown-link cursor-pointer">
                                            <span v-if="[row.hasDataDiagnosisReport,row.hasDiagnosisReport].includes(1)"
                                                  :style="{color:isShowViewAnalysis(row)?'#15ACFE':'#95A1AF'}">
                                                 {{ row?.basicSettingsVo?.accountType === 0 ? '查看诊断' : '查看分析' }}
                                            </span>
                                             <span v-else :style="{color:isShowViewAnalysis(row)?'#444DFF':'#95A1AF'}">
                                                 {{row?.basicSettingsVo?.accountType===0?'生成诊断':'竞品分析'}}
                                            </span>
                                        </span>
                                        <el-dropdown-menu slot="dropdown">
                                            <template v-if="row?.basicSettingsVo?.accountType===0">
                                                <el-dropdown-item command="data" v-if="row.hasDataDiagnosisReport===1">查看数据诊断</el-dropdown-item>
                                                <el-dropdown-item command="content" v-if="row.hasDiagnosisReport===1">查看内容诊断</el-dropdown-item>
                                            </template>
                                            <template v-else>
                                                <el-dropdown-item command="data" v-if="row.hasDataDiagnosisReport===1">查看数据分析</el-dropdown-item>
                                                <el-dropdown-item command="data" v-else>竞品数据分析</el-dropdown-item>
                                                <el-dropdown-item command="content" v-if="row.hasDiagnosisReport===1">查看内容分析</el-dropdown-item>
                                                <el-dropdown-item command="content" v-else>竞品内容分析</el-dropdown-item>
                                            </template>
                                        </el-dropdown-menu>
                                    </el-dropdown>
                                </el-badge>
                            </div>
                        </template>
                        <template #anchorName="{ row:item, pVisible, pIndex }">
                            <div class="fileColContainer">
                            <el-popover
                                v-model="pVisible"
                                placement="right"
                                trigger="hover"
                                :visible-arrow="false"
                                popper-class="schedule-popover-popper"
                                :disabled="!hasSchedulePopover(item)"
                                >
                                <div class="schedule-popover-content">
                                    <div
                                        v-for="(scheduleItem, scheduleIndex) in getSchedulePopoverRows(item)"
                                        :key="`${item.videoId || item.secUid || 'schedule'}-${scheduleIndex}`"
                                        class="schedule-popover-row">
                                        <i
                                            class="schedule-popover-dot"
                                            :style="{
                                                display: 'inline-block',
                                                width: '6px',
                                                height: '6px',
                                                borderRadius: '50%',
                                                marginRight: '8px',
                                                flexShrink: 0,
                                                backgroundColor: getScheduleDotColor(scheduleIndex)
                                            }"></i>
                                        <span class="schedule-popover-position" style="color: #FFFFFF;">{{ scheduleItem.positionName }}：</span>
                                        <span class="schedule-popover-employees" style="color: #FFFFFF;">{{ scheduleItem.employeeNames }}</span>
                                    </div>
                                </div>
                                <div slot="reference">
                                    <Anchor
                                        :item="{...item.anchorInfo,sliceVideoName:item.videoSliceInfo?.sliceVideoName,...getMark(item),...getAnchorPositionInject(item)}"
                                        :uploadStatus="item.uploadStatus"
                                        @editFileName="()=>editFileName(item,getRecordList)"
                                        :replayType="replayType"
                                        notTag
                                        :notLiveStatus="true"></Anchor>
                                    <!-- <img :src="row?.AnchorInfo?.AnchorAvatar" class="videoImg">
                                    <div class="fileNameContainer">
                                        <div class="videoNameText slh" style="width: 120px">{{ row?.AnchorInfo?.AnchorName }}</div>
                                        <div class="compereNameText slh" style="width: 120px">{{ row?.VideoName }}</div>
                                    </div> -->
                                </div>
                            </el-popover>
                            </div>
                        </template>
                        <template #startTime="{ row}"> 
                            <div class="startTimeBox">
                                <div class="text-xs" style="line-height: 20px">{{row?.startTime?.substring(5, 16)}}</div>
                                <!-- <div style="line-height: 0.6;">~</div>
                                <div class="gray-9 text-xs" style="line-height: 20px">{{row?.endTime?.substring(5, 16)}}</div> -->
                                <div class="text-xs" style="line-height: 20px">{{myUtils.toformatTimeChinse(row.duration * 1000)}}</div>
                                <ActivatedServiceIcons v-if="!versionTypeIsPure" :item="getMark(row)" :replayType="replayType" />
                            </div>
                        </template>
                        <template #menu="{ row,menu }">
                            <template v-for="option in menu.options">
                                <Operation :options="option" :data="row" @visibleChange="visibleChange"></Operation>
                            </template>
                        </template>
                        <template #empty>
                            <div class="emptyContainer">
                                <div class="emptyTipText" v-if="replayType==='replayAll'&&aIFinish">当前主播还没有已完成的AI诊断</div>
                                <div class="emptyTipText" v-if="replayType==='replayShort'">当前主播暂无短视频切片</div>
                                <div class="emptyTipText" v-if="replayType==='replaySection'">当前主播暂无AI切片复盘</div>
                                <div class="emptyTipText" v-if="replayType==='replayAll'&&!aIFinish">当前主播还没有录制的视频</div>
                            </div>
                        </template>
                    </CoreTable>
                    <exampleList v-else></exampleList>
                </div>
            </el-col>
        </el-row>


        <!-- 行业弹窗 -->
        <TardeDialog ref="tardeDialog" :treeList="tradeTreeList" @confirm="createAnalysisConfirm"></TardeDialog>

        <AiDataReport ref="ai_data_report" reportType="video"/>

        <AiContentReport ref="ai_content_report" :isReplay="true"/>

        <exportDialog ref="export_dialog" @createText="createText"/>

        <Summary ref="summary"></Summary>

        <DelVideoConfirm :visible.sync="delVideoVisible" @delFileType="delFileType"/>

        <SliceTutorial ref="slice_tutorial"/>

        <OptimizeActions ref="optimize_actions"/>

        <ReviewContrast ref="review_contrast" @contrastSubmit="contrastSubmit" @swapObj="swapObj"/>

        <el-dialog
            title="编辑人员"
            :visible.sync="editPersonDialogVisible"
            width="388px"
            class="edit-person-dialog"
            :close-on-click-modal="false"
            :close-on-press-escape="false"
            :destroy-on-close="true">
            <div class="edit-person-dialog-content">
                <div class="edit-person-dialog-text">
                    排班人员需要前往
                    <span class="edit-person-dialog-link" @click="goScheduleManage">
                        企业管理后台
                    </span>
                    去编辑
                </div>
                <afp-button type="primary" size="default" :plain="false" @click="goScheduleManage">
                    点我前往后台
                </afp-button>
            </div>
        </el-dialog>
        <ReportNoticeDialog
            :visible.sync="reportNoticeVisible"
            :records="reportNoticeRecords"
            :loading="reportNoticeLoading"
            @view-report="handleViewReportNotice" />

        <AiMonitorReportDialog
            v-if="!versionTypeIsPure"
            :visible.sync="aiMonitorReportVisible"
            :type="aiMonitorReportType"
            :row="aiMonitorReportRow"
            :reportId="aiMonitorReportId"
            @opened="handleAiMonitorReportOpened"
            @report-read="handleAiMonitorReportRead"
            @confirmed="handleAiMonitorConfirmed" />

        <ScriptRestorationDrawer
            :visible.sync="scriptRestorationDrawerVisible"
            :scene="scriptRestorationDrawerScene"
            :form="scriptRestorationDrawerForm"
            @confirmed="handleScriptRestorationConfirmed"
            @generate="handleScriptRestorationGenerate"
        />

    </div>
</template>

<script>
/**
 * @description 录制列表页脚本逻辑：负责列表查询/对比、AI 监控状态批量拉取与映射、报告弹窗交互等。
 * 注意：AI 监控状态字段需要与 scriptMonitor/reportStatus 的返回结构保持一致（接口封装层已做归一化）。
 */
import compere from './component/compere.vue'
import CoreTable from '@/components/coreTable/index.vue'
import TardeDialog from '@/components/tardeDialog/index.vue'
import contrastMixin from '../../../mixins/contrastMixin'
import AiDataReport from '../../commonComponent/aiReport/aiDataReport.vue'
import AiContentReport from '../../commonComponent/aiReport/aiContentReport.vue'
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue'
import Popover from '@/views/modules/dataAnalysis/component/common/popover.vue'
import table from '@/mixins/table'
import viewVideo from '@/mixins/viewVideo'
import exportDialog from '@/components/DiscernSearchContainer/exportDialog.vue';
import summaryMixin from '@/components/summary/mixin';
import DelVideoConfirm from './component/delVideoConfirm.vue';
import exampleMixin from '@/mixins/exampleMixin';
import exampleList from '@/views/modules/example/index.vue'
import SliceTutorial from './component/sliceTutorial.vue'
import OptimizeActions from '@/components/optimizeActions'
import Operation from '@/components/Table/operation.vue'
import ReportNoticeDialog from './component/reportNoticeDialog.vue'
import AiMonitorReportDialog from '@/components/aiMonitor/reportDialog/index.vue'
// import ScriptRestoreCell from '@/components/aiMonitor/scriptRestoreCell/index.vue'
import ScriptRestorationDrawer from '@/components/scriptRestoration/ScriptRestorationDrawer.vue'
import GuardAction from '@/components/qrCodeGuard/GuardAction/index.vue'
import { QR_CODE_DIALOG_TYPE } from '@/enum/qrCodeDialogEnum'
import AiMonitorPreviewContent from '@/components/aiMonitor/previewContent/index.vue'

import commonHttp from '@/mixins/commonHttp';
import {cloneDeep} from "lodash";

import ifDay15 from '@/mixins/ifDay15.js'
import myUtils from "@/utils/utils";
import { trackEvent } from '@/utils/laTrack';
import { AI_WORKBENCH_PANELS } from '@/utils/aiAgentRoute'
import {DEFINITION_LIST, VERSION_TYPE} from "@/enum";
import ActivatedServiceIcons from '@/views/commonComponent/activatedServiceIcons/index.vue'

export default {
    components: {
        compere,
        CoreTable,
        TardeDialog,
        Anchor,
        Popover,
        ActivatedServiceIcons,
        AiDataReport,
        AiContentReport,
        DelVideoConfirm,
        exportDialog,
        exampleList,
        SliceTutorial,
        OptimizeActions,
        Operation,
        ReportNoticeDialog,
        AiMonitorReportDialog,
        ScriptRestorationDrawer,
        GuardAction,
        AiMonitorPreviewContent,
        // ScriptRestoreCell
    },
    mixins: [contrastMixin, table, viewVideo, summaryMixin, exampleMixin,commonHttp,ifDay15],
    data () {
        return {
            aiMonitorQuota: {
                qc: { used: 0, total: 0 },
                fidelity: { used: 0, total: 0 },
                inspect: { used: 0, total: 0 }
            },
            aiMonitorStateMap: {},
            speedUpAnalysisMap: {},
            aiMonitorReportVisible: false,
            aiMonitorReportType: '',
            aiMonitorReportRow: null,
            aiMonitorReportId: null,
            _openAiReportDebounced: null,
            scriptRestorationDrawerVisible: false,
            scriptRestorationDrawerScene: 'analysis',
            scriptRestorationDrawerForm: null,
            scriptRestorationDrawerRow: null,
            scriptRestorationDrawerAction: '',
            formConfig: {
                items: [
                    {
                        label: '分析状态', prop: 'analysisStatus',
                        temp: 'Select',
                        showItems: ['replayAll'],
                        config: {
                            default: '',
                            options: [
                                {
                                    value: '',
                                    label: '全部'
                                },
                                {
                                    value: 0,
                                    label: '未分析'
                                },
                                {
                                    value: 1,
                                    label: '分析中'
                                },
                                {
                                    value: 2,
                                    label: '分析完成'
                                },
                                {
                                    value: 3,
                                    label: '分析失败'
                                }
                            ],
                            style: {
                                width: '170px'
                            }
                        },
                        on: {
                            input: true
                        }
                    },
                    {
                        label: '直播间搜索', prop: 'anchorName',
                        temp: 'Input',
                        showItems: ['replayShort'],
                        config: {
                            default: '',
                            placeholder: '请输入直播间名称',
                        }
                    },
                    {
                        label: '行业筛选', prop: 'tradeId',
                        temp: 'Select',
                        showItems: ['replayShort'],
                        config: {
                            filterable: true,
                            default: '',
                            options: [{
                                id: '',
                                name: '全部'
                            }],
                            prop: {
                                label: 'name',
                                value: 'id'
                            },
                            style: {
                                width: '200px'
                            }
                        },
                    },
                    {
                        label: '切片类型', prop: 'sliceClass',
                        temp: 'Select',
                        showItems: ['replaySection', 'replayShort'],
                        config: {
                            filterable: true,
                            default: '',
                            options: [{
                                value: '',
                                label: '全部'
                            }],
                            prop: {
                                label: 'label',
                                value: 'value'
                            },
                            style: {
                                width: '200px'
                            }
                        },
                        on: {
                            input: true
                        }
                    },
                    {
                        label: '录制时间', prop: 'searchDate',
                        temp: 'DatePicker',
                        showItems: ['replayAll', 'replaySection'],
                        config: {
                            type: 'daterange',
                            valueFormat: 'yyyy-MM-dd',
                            style: {
                                width: '240px'
                            }
                        }
                    }
                ],
            },
            //
            column: [
                {
                    label: '本地文件名（双击修改）',
                    prop: 'anchorName',
                    hidden: ()=>{
                        return this.columnHidden([])
                    },
                    option: {
                        minWidth: 120,
                        'show-overflow-tooltip': true
                    }
                },
                {
                    label: '切片分类',
                    prop: 'sliceClass',
                    hidden: ()=>{
                        return this.columnHidden(['replayAll'])
                    },
                    option: {
                        width: '100px',
                        render: (row) => {
                            return (`<div class="recordColContainer" style="padding:15px 0 16px 0">
                                <div>${this.sliceOptions.find(item=>item.value==row.videoSliceInfo?.sliceClass)?.label||''}</div>
                            </div>`)
                        },
                    },
                },
                {
                    label: '原视频时间',
                    prop: 'startTime',
                    hidden: ()=>{
                        return this.columnHidden(['replayAll','replaySection'])
                    },
                    option: {
                        width: '150px',
                        render: (row) => {
                            return (`<div class="recordColContainer" style="padding:15px 0 16px 0">
                                 <div>${row?.startTime?.substring(5, 16)}</div>
                                 <div>${myUtils.toformatTimeChinse(row.duration * 1000)}</div>
                            </div>`)
                        },
                    },
                },
                {
                    label: () => {
                        if (this.columnHidden(['replaySection'])) {
                            return '切片时间'
                        }
                        if (this.columnHidden(['replayAll'])) {
                            return '录制时间'
                        }
                    },
                    prop: 'startTime',
                    hidden: ()=>{
                        return this.columnHidden(['replayShort'])
                    },
                    option: {
                        width: '120px',
                        // render: (row) => {
                        //     return (`<div class="recordColContainer">
                        //         <div class="text-xs" style="line-height: 20px">${row?.startTime?.substring(5, 16)}</div>
                        //         <div style="line-height: 0.6;">~</div>
                        //         <div class="gray-9 text-xs" style="line-height: 20px">${row?.endTime?.substring(5, 16)}</div>
                        //         <div class="text-xs" style="line-height: 20px">${myUtils.toformatTimeChinse(row.duration * 1000)}</div>
                        //     </div>`)
                        // },
                    },
                },
                {
                    label: '核心数据',
                    prop: 'viewersNum',
                    option: {
                        minWidth: 120,
                        align: 'left',
                    },
                    hidden: ()=>{
                        return this.columnHidden(['replayShort']) || this.versionTypeIsPure
                    },
                },
                {
                    label: '话术质检',
                    prop: 'speechQc',
                    hidden: ()=>{
                        return this.columnHidden(['replaySection','replayShort']) || this.versionTypeIsPure
                    },
                    option: {
                        width: '135px',
                    }
                },
                {
                    label: '话术还原度',
                    prop: 'scriptRestore',
                    hidden: ()=>{
                        return this.columnHidden(['replaySection','replayShort']) || this.versionTypeIsPure
                    },
                    option: {
                        width: '135px',
                    }
                },
                {
                    label: '互动巡检',
                    prop: 'interactionInspect',
                    hidden: ()=>{
                        return true
                    },
                    option: {
                        width: '150px',
                    }
                },
                {
                    label: '录制时长',
                    prop: 'durationStr',
                    hidden: ()=>{
                        return !this.versionTypeIsPure
                    },
                    option: {
                        width: '120px',
                        render: (row) => {
                            if(row.recordErrorStatus === 7){
                                return (`<div>
                                    <span>${row.durationStr}</span><br>
                                    <span class="text-colorErr font-s14">网络不稳定<span class="el-icon-warning-outline cs-p"></span></span>
                                </div>`)
                            }else{
                                 return myUtils.toformatTimeChinse(row.duration * 1000) //row.durationStr
                            }
                        },
                        on:{
                            click: (e, row) => {
                                if(e.target.className.includes('cs-p')){
                                    this.$cMsg.custom({
                                        title: '友情提示',
                                        closeOnClickModal: false,
                                        closeOnPressEscape: false,
                                        dangerouslyUseHTMLString: true, // 允许使用 HTML
                                        message: `您的录制网络或者直播间的直播网络不稳定，会导致出现多个分段或只录制到音频的情况发生。<br>
                                        可考虑重启录制网络或检查直播网络或设备散热情况。<br>
                                        <span class="text-colorErr">特别建议</span>：光猫等设备长时间不重启，会导致网络出现不稳定的情况，因此建议每日重启。`
                                    })
                                }
                            }
                        }
                    },
                },
                {
                    label: '分析状态',
                    prop: 'analysisStatus',
                    hidden: ()=>{
                        return this.columnHidden(['replaySection','replayShort'])
                    },
                    dicProp: 'analysisStatus',
                    option: {
                        width:'100px',
                        renderFormatter: true,
                        render: (row, option) => {
                            if(row.analysisStatus === 0){
                                return `<div>排队分析中</div>`
                            }
                            const {useAnalysisPropertyType} = row
                            let renderText = null
                            if(useAnalysisPropertyType===1){
                                renderText=option.formatterText?.replace("分析", "提取")
                            }else {
                                renderText=option.formatterText
                            }
                            if(row.analysisStatus === 4){
                                renderText = '未分析'
                            }
                            let className = {
                                3: 'text-colorErr',
                                4: 'text-color3',
                            }
                            return `<div class="${className[row.analysisStatus] || ''}">${renderText}</div>
                                    ${row.errorReason ? '<div class="text-colorErr text-xs">(' + row.errorReason + ')</div>' : ''}`
                        }
                    }
                },
 
                {
                    label: '优化计划',
                    prop: 'optimizeActions',
                    formatter: (row) => {
                        return this.isShowScript(row) ? '-' : (row.hasAiOptimizePurpose ? '查看计划' : '记录计划');
                    },
                    option: {
                        width: '80px',
                        classNameFn: (row) => {
                            if (row.analysisStatus === 2 && !this.isShowScript(row)) {
                                return row.hasAiOptimizePurpose ? 'color-main -view cursor-pointer' : 'color-main cursor-pointer';
                            } else {
                                return 'text-color3'
                            }
                        },
                        on: {
                            click: (e, row) => {
                                if (this.replayType === 'replayAll' && !row.hasAiOptimizePurpose) {
                                    this.trackReplayListEvent('P002_A0022')
                                }
                                if (row.analysisStatus === 2 && !this.isShowScript(row)) {
                                    this.$refs?.optimize_actions?.change(true, row, this.getRecordList);
                                }
                            }
                        }
                    },
                    hidden: ()=>{
                        return this.columnHidden(['replayShort']) || this.versionTypeIsPure
                    },
                },
                {
                    label: '清晰度',
                    prop: 'definition',
                    hidden: ()=>{
                        return this.columnHidden(['replayAll', 'replaySection']) && !this.versionTypeIsPure
                    },
                    option: {
                        render: (row) => {
                            const definitionList = ["标清", "高清", "超清", "蓝光"]
                            return (`<div class="recordColContainer" style="padding-block:15px">
                                <div>${definitionList[row?.definition]}</div>
                            </div>`)
                        },
                    },
                },
                {
                    label: '视频文件夹',
                    prop: 'open',
                    hidden: ()=>{
                        return this.columnHidden(['replayAll','replaySection'])
                    },
                    option: {
                        render: () => {
                            return (`<div class="cursor-pointer" style="color: var(--color-main)">打开文件夹</div>`)
                        },
                        on:{
                            click: (e,item) => {
                                this.openFolder(item.videoId)
                            },
                        }
                    }
                },
                {
                    label: '播放切片',
                    prop: 'play',
                    hidden: ()=>{
                        return this.columnHidden(['replayAll','replaySection'])
                    },
                    option: {
                        render: () => {
                            return (`<div class="cursor-pointer">查看</div>`)
                        },
                        on:{
                            click: (e,item) => {
                                this.preview(item.videoId)
                            },
                        }
                    }
                },
                // {
                //     label: '本段进场人数',
                //     prop: 'viewersNum'
                // },
                {
                    label: '文件大小',
                    prop: 'vedioSizie',
                    hidden: ()=>{
                        return this.columnHidden(['replaySection', 'replayShort'])
                    },
                    option: {
                        suffix: 'M'
                    },
                },
                {
                    label: '话术',
                    prop: 'script',
                    hidden: ()=>{
                        return this.columnHidden(['replayShort'])
                    },
                    formatter: (row) => row.analysisStatus===2?'导出':'-',
                    option: {
                        width: '50px',
                        className: 'export_script',
                        on: {
                            click: (e, row) => {
                                if (row.analysisStatus !== 2) return
                                this.trackReplayListEvent('P002_A0023')
                                Promise.all([this.$httpBack.words.getVideoContent({
                                    sourceId: row.videoId,
                                    sourceType: 0,
                                    type: 1,
                                }), this.$httpBack.words.getVideoContent({
                                    sourceId: row.videoId,
                                    sourceType: 0,
                                    type: 2,
                                })]).then(results => {
                                    const [result1, result2] = results
                                    if (result1?.code !== 0 || result2?.code !== 0) return this.$message.error('获取视频内容失败')
                                    this.$refs.export_dialog?.show({
                                        data: {
                                            fileName: `${row.reportFileName}_`,
                                            fileSize: Number(row.vedioSizie) * 1024 * 1024,
                                            sourceId: row.videoId,
                                            sourceType: 0,
                                            isRecording: true,
                                            isReplay:true,
                                            //contentStatus: result.contentStatus,
                                            aiShardingStatus: result1.data?.contentStatus,//AI脚本拆解
                                            aiOptimalStatus: result2.data?.contentStatus,//优化原文
                                            readonly: this.isShowScript(row),
                                        }
                                    })
                                })
                            }
                        }
                    }
                },
                // {
                //     label: '小结',
                //     prop: 'summary',
                //     hidden: ()=>{
                //         return this.columnHidden(['replayShort']) || this.versionTypeIsPure
                //     },
                //     formatter: (row) => {
                //         return '查看';
                //     },
                //     option: {
                //         width: '50px',
                //         classNameFn: (row)=>{
                //             return row.notesSummary?'color-main cursor-pointer': 'text-color3';
                //         },
                //         on: {
                //             click: (e, row) => {
                //                 if(row.notesSummary){
                //                     this.trackReplayListEvent('P002_A0028')
                //                     this.onClickSummary(row);
                //                 }
                //             }
                //         }
                //     }
                // },
                {
                    label: 'AI诊断报告',
                    prop: 'aiReport',
                    hidden: ()=>{
                        return true
                    },
                    option: {
                        width: '110px',
                    }
                }
            ],
            menuConfig: {
                // width: "150px",
                width: '200px',
                options: [
                    [
                        {
                            label: '切片中',
                            type: 'warning',
                            show: (row) => {
                                return row.analysisStatus === 5
                            },
                        }, {
                        label: '看视频',
                        click: (item) => {
                            this.viewVideo(item, 'video')
                        },
                        show: (row) => {
                            return this.isShowViewVideo(row) || row.analysisStatus === 4
                        },
                    }, {
                        label: '点我停止分析',
                        // disabled: true,
                        type: 'danger',
                        popconfirm: '请确认是否需要停止分析？',
                        plain: true,
                        show: (row) => {
                            return row.analysisStatus === 0
                        },
                        disabled: (row) => {
                            return this.isSpeedingUpAnalysis(row)
                        },
                        click: (row) => {
                            this.cancelVideoAnalysis(row)
                        }
                    },
                        {
                            label: '智能分析中',
                            disabled: true,
                            type: 'info',
                            plain: true,
                            show: (row) => {
                                return row.analysisStatus === 1
                            },
                        },
                        {
                            label: () => {
                                if (this.versionTypeIsPure) {
                                    return '查看文案'
                                } else {
                                    return '查看话术脚本'
                                }
                            },
                            type: 'success',
                            plain: () => this.versionTypeIsPure,
                            click: (item) => {
                                this.viewVideo(item, 'video')
                            },
                            show: (row) => {
                                return this.isShowScript(row)
                            },
                        },
                        {
                            label: () => {
                                if (this.versionTypeIsPure) {
                                    return '查看文案'
                                } else {
                                    if (this.replayType === 'replayAll') {
                                        return '查看整场分析'
                                    } else if (this.replayType === 'replaySection') {
                                        return '查看切片分析'
                                    } else {
                                        return '查看分析'
                                    }
                                }
                            },
                            type: () => this.versionTypeIsPure?'success':'primary',
                            plain: () => this.versionTypeIsPure,
                            click: (item) => {
                                this.trackReplayListEvent('P002_A0025')
                                this.lockAnalysis(item.videoId, item)
                            },
                            show: (row) => {
                                return this.isShowViewAnalysis(row)
                            },
                        },
                        {
                            label: '再分析',//重新智能分析
                            click: (row) => {
                                this.afreshAnalysis(row)
                            },
                            type: 'warning',
                            show: (row) => {
                                return row.analysisStatus === 3 || row.analysisStatus === 4
                            },
                        },
                        {
                            label: '加对比',
                            click: (item) => {
                                this.trackReplayListEvent('P002_A0026')
                                this.addContrast(item)
                            },
                            type: 'text',
                            show: (row) => {
                                return this.isShowViewAnalysis(row) && !this.versionTypeIsPure
                            },
                            disabled: (item) => {
                                return this.contrastList.some(_item => _item.videoId === item.videoId)
                            },
                            iconSize: 13,
                            icon: 'icon-a-Frame762',
                        }
                    ],
                    [
                        this.columnHidden(['replayAll']) ? {
                            label: (row) => {
                                if (Number(row?.analysisStatus) === 1) {
                                    if (Number(row?.isDownloaded) !== 1) {
                                        return '<span class="speedUpLabel speedUpLabel--disabled" title="视频下载中">加速分析</span>'
                                    }
                                    if (this.isSpeedingUpAnalysis(row)) {
                                        return '<span class="speedUpLabel speedUpLabel--active">加速中<i class="el-icon-caret-right speedUpCaret speedUpCaret--1"></i><i class="el-icon-caret-right speedUpCaret speedUpCaret--2"></i><i class="el-icon-caret-right speedUpCaret speedUpCaret--3"></i></span>'
                                    }
                                    return '<span style="color:#2FD16C;">加速分析<i class="el-icon-caret-right" style="font-size:12px;margin-left:2px;"></i><i class="el-icon-caret-right" style="font-size:12px;margin-left:-3px;"></i></span>'
                                }
                                return 'AI诊断直播'
                            },
                            dangerouslyUseHTMLString: true,
                            click: (row) => {
                                if (Number(row?.analysisStatus) === 1) {
                                    if (Number(row?.isDownloaded) !== 1) return
                                    this.speedUpAnalysis(row)
                                    return
                                }
                                this.trackReplayListEvent('P002_A0033')
                                this.openAiAgentWorkbench(row)
                            },
                            disabled: (row) => {
                                if (Number(row?.analysisStatus) === 1 && Number(row?.isDownloaded) !== 1) return true
                                return Number(row?.analysisStatus) === 1 && (this.isSpeedingUpAnalysis(row) || this.isSpeedingUpAnalysisLoading(row))
                            },
                            type: 'text',
                            show: (row) => {
                                const status = Number(row?.analysisStatus)
                                return status === 1 || status === 2
                            },
                        } : {
                            label: () => {
                                if (this.versionTypeIsPure) {
                                    return '文件夹'
                                } else {
                                    return '视频文件夹'
                                }
                            },
                            click: (item) => {
                                this.trackReplayListEvent('P002_A0027')
                                this.openFolder(item.videoId)
                            },
                            icon: 'icon-a-Frame1219',
                            type: 'text',
                            show: (row) => {
                                return row.analysisStatus !== 5 && !this.getRowSchedulesData(row)
                            }
                        },{
                        label: '编辑人员',
                        click: (item) => {
                            this.openEditPersonDialog(item)
                        },
                        type: 'text',
                        show: (row) => {
                            return !!this.getRowSchedulesData(row)
                        }
                    }, [
                        this.columnHidden(['replayAll']) ? {
                            label: () => {
                                if (this.versionTypeIsPure) {
                                    return '文件夹'
                                } else {
                                    return '视频文件夹'
                                }
                            },
                            click: (item) => {
                                this.trackReplayListEvent('P002_A0027')
                                this.openFolder(item.videoId)
                            },
                            icon: 'icon-a-Frame1219',
                            type: 'text',
                            show: (row) => {
                                return row.analysisStatus !== 5 && !this.getRowSchedulesData(row)
                            }
                        } : {
                            label: 'AI诊断直播',
                            click: (row) => {
                                this.trackReplayListEvent('P002_A0033')
                                this.openAiAgentWorkbench(row)
                            },
                            show: (row) => {
                                return row.analysisStatus === 2
                            },
                        }, {
                            label: '原视频',
                            click: (item) => {
                                this.trackReplayListEvent('P002_A0034')
                                this.preview(item.videoId)
                            },
                            icon: 'icon-a-Frame981',
                            show: (row) => {
                                return row.analysisStatus !== 5
                            },
                        }, {
                            label: '再分析',
                            click: (row) => {
                                this.trackReplayListEvent('P002_A0035')
                                // this.reAnalysis(row.videoId, row.secUid)
                                this.afreshAnalysis(row)
                            },
                            show: () => {
                                return this.replayType === 'replayAll'
                            },
                            iconSize: 14,
                            icon: 'icon-tianjiawenben',
                        }, {
                            label: '修改文件名',
                            icon: 'icon-a-bukechakan2',
                            show: (row) => {
                                return row.analysisStatus === 2
                            },
                            click: (row) => {
                                this.trackReplayListEvent('P002_A0038')
                                this.editFileName(row, this.getRecordList)
                            }
                        }, {
                            label: '删除',
                            type: 'danger',
                            click: (item) => {
                                this.trackReplayListEvent('P002_A0039')
                                this.delVideoVisible = true;
                                this.selectDelList = [item]
                                // this.deleteFile({
                                //     list: [item],
                                //     callback: () => {
                                //         this.$refs.compere?.getCompereList()
                                //     }
                                // })
                            },
                            show: (row) => {
                                return row.analysisStatus !== 5
                            },
                        }]
                    ]
                ]
            },
            tradeTreeList: [],
            listByAnchor: [],
            fileDataForm: {
                secUid: -1
            },
            // searchData: {
            //     searchDate: []
            // },
            tradeId: '1',
            selectList: [],
            listTimer: null,
            // 记录已加速成功的行，列表刷新后会根据最新分析状态自动清理。
            speedUpAnalysisMap: {},
            // 记录加速请求中的行，避免用户重复点击。
            speedUpAnalysisLoadingMap: {},
            selectDelList: [],
            delVideoVisible: false,
            delConfirmCallback: null,
            isExampleList: true,
            sliceOptions: [],
            toggleMenu: false,
            definitionList: DEFINITION_LIST,
            schedulesData: [], // 直播间排班数据
            editPersonDialogVisible: false,
            currentEditPersonItem: {},
            reportNoticeVisible: false,
            reportNoticeRecords: [],
            reportNoticeLoading: false,
            reportNoticePending: false,
            hasShownInitReportNotice: false,
            myUtils,
        }
    },
    inject: ['appVnode'],
    computed: {
        dataView () {
            return (resData, keys, length) => {
                return myUtils.dataView(resData, keys, length)
            }
        },
        workshopOrAbove() {
            const level = Number(this.$store?.getters?.getPackageLevel ?? 0)
            if (level === -1) return true
            return level >= 15
        },
        conversion () {
            return (value) => {
                return myUtils.fnw(value)
            }
        },
        versionTypeIsPure() {
            return this.$store.getters.getVersionType === VERSION_TYPE.PURE || this.$store.getters.isPure
        },
        computeFormConfig () {
            const items = this.formConfig.items.filter(item => item.prop !== (this.aIFinish ? 'analysisStatus' : ''))
            const currentItems = items.filter(item => {
                return item.showItems?.includes(this.replayType)
            })
            return {
                ...this.formConfig,
                items: currentItems
            }
        },
        computeColumn () {
            return this.column.filter(item => {
                return this.aIFinish ? !['script', 'analysisStatus'].includes(item.prop) : (this.versionTypeIsPure ? true : item.prop !== 'vedioSizie')
            })
        },
        computeMenuConfig() {
            if (this.versionTypeIsPure) {
                const menuConfig = cloneDeep(this.menuConfig)
                menuConfig.options[this.menuConfig.options.length - 1] = [...menuConfig.options[this.menuConfig.options.length - 1].slice(0, -1), {
                    label: '删除',
                    type: 'danger',
                    click: (item) => {
                        this.delVideoVisible = true;
                        this.selectDelList = [item]
                    },
                    show: (row) => {
                        return row.analysisStatus !== 5
                    },
                }]
                return menuConfig
            } else {
                if (this.replayType === 'replayShort') {
                    const menuConfig = cloneDeep(this.menuConfig)
                    menuConfig.options[this.menuConfig.options.length - 1] = []
                    menuConfig.options.forEach((item, index) => {
                        if (index === 0) {
                            menuConfig.options[index] = [...menuConfig.options[index].slice(0, -1), {
                                label: '删除',
                                type: 'danger',
                                click: (item) => {
                                    this.delVideoVisible = true;
                                    this.selectDelList = [item]
                                },
                                show: (row) => {
                                    return row.analysisStatus !== 5
                                },
                            }]
                        } else {
                            menuConfig.options[index] = []
                        }
                    })
                    return menuConfig
                }
                return this.menuConfig
            }
        },
        getMark() {
            return (item) => {
                return {
                    videoRename: item.videoRename,
                    existBarrage: item.existBarrage,
                    existDataBoard: item.existDataBoard,
                    hasDiagnosisReport: item.hasDiagnosisReport || item.hasDataDiagnosisReport,
                    existsMark: item.existsMark,
                    existsNotes: item.existsNotes,
                    notesSummary: item.notesSummary,
                    localVideoStatus: item.localVideoStatus
                }
            }
        },
    },
    mounted () {
        if (!this._openAiReportDebounced && typeof myUtils?.debounce === 'function') {
            this._openAiReportDebounced = myUtils.debounce(260, (type, row) => {
                this.openAiReportCore(type, row)
            }, true)
        }
        this.restoreSpeedUpAnalysisState()
        this.initData()
    },
    activated () {
        if (!this._openAiReportDebounced && typeof myUtils?.debounce === 'function') {
            this._openAiReportDebounced = myUtils.debounce(260, (type, row) => {
                this.openAiReportCore(type, row)
            }, true)
        }
        this.restoreSpeedUpAnalysisState()
        this.initData()
    },
    beforeDestroy () {
        this.clearInit()
    },
    beforeRouteLeave (to, from, next) {
        this.clearInit()
        next()
    },
    props: {
        // isSelectTabs:{
        //     type: Boolean,
        //     default: false
        // }
        aIFinish: {// 是否是已完成的AI诊断
            type: Boolean,
            default: false
        },
        replayType:{
            type:String,
            default: ''
        }
    },
    watch: {
        // isSelectTabs: {
        //     handler(v){
        //         if(!v){return};
        //         this.getRecordList('watch');
        //     }
        // }
        aiMonitorReportVisible(val) {
            if (val) return
            this.aiMonitorReportId = null
            this.aiMonitorReportRow = null
            this.aiMonitorReportType = ''
        }
    },
    methods: {
        /**
         * @description 获取某行录制数据的 AI 监控状态对象（按 videoId 维护本地 stateMap，保证响应式更新）。
         * 说明：这里统一输出固定结构，避免模板层大量判空；未命中 videoId 时返回默认结构。
         * @param {Object} row 录制行数据
         * @returns {Object} state（含质检/还原度/巡检状态、报告ID、未读状态、统计字段等）
         */
        getAiMonitorState(row) {
            const id = row?.videoId
            if (!id) {
                return {
                    qcEnabled: false,
                    qcMonitorEnabled: false,
                    restoreEnabled: false,
                    restoreMonitorEnabled: false,
                    inspectEnabled: false,
                    inspectMonitorEnabled: false,
                    qcReportStatus: 0,
                    restoreReportStatus: 0,
                    inspectReportStatus: 0,
                    qcReportId: null,
                    restoreReportId: null,
                    inspectReportId: null,
                    qcUnread: false,
                    restoreUnread: false,
                    inspectUnread: false,
                    qcReadOverrideReportId: null,
                    inspectReadOverrideReportId: null,
                    qcPendingUnread: false,
                    restorePendingUnread: false,
                    inspectPendingUnread: false,
                    qcUnavailableReason: '',
                    restoreUnavailableReason: '',
                    inspectUnavailableReason: '',
                    qcPreview: '',
                    restorePreview: '',
                    inspectPreview: '',
                    restoreScore: null,
                    restoreDeviations: 0,
                    inspectEffectiveness: null,
                    inspectMissed: 0,
                    restoreConfig: {}
                }
            }
            if (!this.aiMonitorStateMap[id]) {
                // 初始化 stateMap：保证后续对字段的赋值是响应式的（避免直接新增字段不触发更新）
                this.$set(this.aiMonitorStateMap, id, {
                    qcEnabled: false,
                    qcMonitorEnabled: false,
                    restoreEnabled: false,
                    restoreMonitorEnabled: false,
                    inspectEnabled: false,
                    inspectMonitorEnabled: false,
                    qcReportStatus: 0,
                    restoreReportStatus: 0,
                    inspectReportStatus: 0,
                    qcReportId: null,
                    restoreReportId: null,
                    inspectReportId: null,
                    qcUnread: false,
                    restoreUnread: false,
                    inspectUnread: false,
                    qcReadOverrideReportId: null,
                    inspectReadOverrideReportId: null,
                    qcPendingUnread: false,
                    restorePendingUnread: false,
                    inspectPendingUnread: false,
                    qcUnavailableReason: '',
                    restoreUnavailableReason: '',
                    inspectUnavailableReason: '',
                    qcPreview: '',
                    restorePreview: '',
                    inspectPreview: '',
                    restoreScore: null,
                    restoreDeviations: 0,
                    inspectEffectiveness: null,
                    inspectMissed: 0,
                    restoreConfig: {
                        scriptRestoreMode: '',
                        scriptRestoreLoopDuration: 0,
                        scriptRestoreTalkSpeed: 280,
                        scriptRestoreReferenceScript: ''
                    }
                })
            }
            return this.aiMonitorStateMap[id]
        },
        /**
         * @description 批量加载 AI 监控状态并写入 stateMap（列表渲染依赖）。
         * 策略：
         * 1) 优先调用 batchReportStatus（性能更好）
         * 2) 批量接口不可用/失败时，降级为逐条 reportStatus
         * @param {Object[]} list 录制列表
         * @returns {Promise<void>}
         */
        async loadAiMonitorStatus(list = []) {
            if (!Array.isArray(list) || !list.length) return
            const processRow = (row, data) => {
                if (!row.basicSettingsVo) {
                    this.$set(row, 'basicSettingsVo', {})
                }
                const state = this.getAiMonitorState(row)
                const qc = data?.scriptQualityInspection || {}
                const restore = data?.scriptFidelityMonitor || {}
                const inspect = data?.interactionPatrol || {}
                const prevQcReportId = state.qcReportId
                const prevInspectReportId = state.inspectReportId

                const isOwn = this.isOwnAiMonitorAccount(row)
                const qcStatus = Number(qc?.status ?? 0)
                const restoreStatus = Number(restore?.status ?? 0)
                const inspectStatus = Number(inspect?.status ?? 0)
                state.qcMonitorEnabled = isOwn && Number(qc?.monitorEnabled ?? 0) === 1
                state.inspectMonitorEnabled = isOwn && Number(inspect?.monitorEnabled ?? 0) === 1
                state.restoreMonitorEnabled = isOwn && Number(restore?.monitorEnabled ?? 0) === 1
                state.qcEnabled = state.qcMonitorEnabled
                state.inspectEnabled = state.inspectMonitorEnabled
                state.restoreEnabled = state.restoreMonitorEnabled

                state.qcReportStatus = qcStatus
                state.qcReportId = qc.reportId || null
                state.qcUnavailableReason = qc.unavailableReason || ''
                state.qcPreview = (qc?.summary ?? '') || ''
                state.qcUnread = !!(state.qcReportId && Number(qc?.isRead) === 0)

                state.restoreReportStatus = restoreStatus
                state.restoreReportId = restore.reportId || null
                state.restoreUnavailableReason = restore.unavailableReason || ''
                const restoreSummary = restore?.summary
                state.restorePreview = typeof restoreSummary === 'string'
                    ? restoreSummary
                    : String(restoreSummary?.summaryText || restoreSummary?.summary || '')
                const restoreScore =
                    (typeof restoreSummary === 'object' && restoreSummary !== null ? restoreSummary?.score : null)
                    ?? restore?.score
                    ?? null
                state.restoreScore = restoreScore === null || restoreScore === undefined ? null : Number(restoreScore)
                const deviationObj = restore?.deviationSummary
                const deviationCounts = typeof deviationObj === 'object' && deviationObj !== null
                    ? Object.values(deviationObj).reduce((acc, v) => acc + (Number(v) || 0), 0)
                    : 0
                state.restoreDeviations = Number.isFinite(deviationCounts) ? deviationCounts : 0
                state.restoreUnread = !!(state.restoreReportId && Number(restore?.isRead) === 0)

                state.inspectReportStatus = inspectStatus
                state.inspectReportId = inspect.reportId || null
                state.inspectUnavailableReason = inspect.unavailableReason || ''
                const inspectSummary = inspect?.summary
                state.inspectPreview = typeof inspectSummary === 'string'
                    ? inspectSummary
                    : String(inspectSummary?.summaryText || inspectSummary?.summary || '')
                state.inspectEffectiveness = inspect?.summary?.interactionRate ?? inspect?.summary?.effectivenessPercentage ?? null
                const missedDirect = Number(inspect?.summary?.missed || inspect?.summary?.missedCount || inspect?.summary?.missedBarrageCount || 0)
                if (Number.isFinite(missedDirect) && missedDirect > 0) {
                    state.inspectMissed = missedDirect
                } else {
                    const text = String(inspect?.summary?.summaryText || inspect?.summary?.summary || '')
                    const m = text.match(/未(?:及时)?回复\s*(\d+)\s*条/)
                    state.inspectMissed = m ? Number(m[1]) : 0
                }
                state.inspectUnread = !!(state.inspectReportId && Number(inspect?.isRead) === 0)
            }

            const rows = list.filter(row => !!row?.videoId)
            if (!rows.length) return

            if (this.$httpBack?.scriptMonitor?.batchReportStatus) {
                try {
                    // 批量接口：一次请求拉取多个 sourceId 的状态
                    const sources = rows.map((row) => ({
                        sourceType: 0,
                        sceneType: 0,
                        sourceId: String(row.videoId),
                        // 后端批量状态接口需要定位主播维度数据：优先取行数据 secUid
                        secUid: String(row?.secUid || row?.anchorInfo?.secUid || '')
                    }))
                    const res = await this.$httpBack.scriptMonitor.batchReportStatus({ sources })
                    if (res?.code === 0) {
                        const items = Array.isArray(res?.data?.items) ? res.data.items : (Array.isArray(res?.data) ? res.data : [])
                        const map = new Map(items.map((it) => [String(it?.sourceId ?? ''), it]))
                        rows.forEach((row) => {
                            const data = map.get(String(row.videoId))
                            if (data) processRow(row, data)
                        })
                        return
                    }
                } catch (e) {}
            }

            if (!this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return
            // 降级为逐条接口：并发请求，全部完成后再返回
            const tasks = rows.map(async (row) => {
                try {
                    const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus({ videoId: row.videoId })
                    if (res?.code !== 0) return
                    processRow(row, res.data || {})
                } catch (e) {}
            })
            await Promise.all(tasks)
        },
        restoreHasIssue(row) {
            const s = this.getAiMonitorState(row)
            return Number(s.restoreDeviations || 0) >= 1
        },
        inspectHasIssue(row) {
            const s = this.getAiMonitorState(row)
            return Number(s.inspectMissed || 0) >= 1
        },
        formatPercent(val) {
            if (val === null || val === undefined || val === '') return '--'
            const num = Number(val)
            if (!Number.isFinite(num)) return '--'
            return `${num}%`
        },
        async loadAiMonitorQuota() {
            if (!this.$httpBack?.scriptMonitor?.monitorPositionStatistics) return
            try {
                const res = await this.$httpBack.scriptMonitor.monitorPositionStatistics({})
                if (res?.code !== 0) return
                const list = Array.isArray(res?.data?.monitorPositions)
                    ? res.data.monitorPositions
                    : (Array.isArray(res?.data) ? res.data : [])
                const getItem = (codes = []) => list.find((it) => codes.some((code) => String(it?.code) === String(code)))
                const qc = getItem(['scriptQualityInspectionNum', 'scriptQualityNum'])
                const fidelity = getItem(['scriptFidelityMonitorNum', 'scriptFidelityNum'])
                const inspect = getItem(['interactionPatrolNum'])
                this.aiMonitorQuota = {
                    qc: { used: Number(qc?.useQuantity || 0), total: Number(qc?.totalQuantity || 0) },
                    fidelity: { used: Number(fidelity?.useQuantity || 0), total: Number(fidelity?.totalQuantity || 0) },
                    inspect: { used: Number(inspect?.useQuantity || 0), total: Number(inspect?.totalQuantity || 0) }
                }
            } catch (e) {}
        },
        resolveAiMonitorGuardType(kind, requireRemain = true, requireVersion = true) {
            if (requireVersion && !this.workshopOrAbove) return QR_CODE_DIALOG_TYPE.NO_PERMISSION_FREE_VERSION

            const map = {
                qc: { key: 'qc', type: QR_CODE_DIALOG_TYPE.AI_SPEECH_QC_QUOTA },
                fidelity: { key: 'fidelity', type: QR_CODE_DIALOG_TYPE.AI_SCRIPT_RESTORE_QUOTA },
                inspect: { key: 'inspect', type: QR_CODE_DIALOG_TYPE.AI_INTERACTION_INSPECT_QUOTA }
            }
            const item = map[kind]
            if (!item) return false
            const { used = 0, total = 0 } = this.aiMonitorQuota?.[item.key] || {}
            const totalNum = Number(total || 0)
            const usedNum = Number(used || 0)
            if (requireRemain && totalNum <= 0) return item.type
            if (requireRemain && (totalNum - usedNum) <= 0) return item.type
            return false
        },
        runAiMonitorGuard(kind, options, onPass) {
            const guard = this.$refs.aiMonitorGuard
            const { requireRemain = true, requireVersion = true } = typeof options === 'object'
                ? options
                : { requireRemain: options !== false, requireVersion: true }
            if (!guard?.run) {
                if (typeof onPass === 'function') onPass()
                return
            }
            guard.run(() => this.resolveAiMonitorGuardType(kind, requireRemain, requireVersion), onPass)
        },
        /**
         * @description 解析录制列表当前行的账号归属；仅明确为 0 时视为自有账号，其余一律按非自有处理，避免把未知数据误放开自动监控入口。
         * @param {Object} row 当前行数据
         * @returns {number|null}
         */
        resolveRowAccountType(row) {
            const r = row || {}
            const candidates = [
                r?.basicSettingsVo?.accountType,
                r?.anchorInfo?.accountType,
                r?.accountType,
                r?.videoInfo?.basicSettingsVo?.accountType,
                r?.videoInfo?.accountType
            ]
            for (const item of candidates) {
                if (item === undefined || item === null || item === '') continue
                const num = Number(item)
                if (Number.isFinite(num)) return num
            }
            return null
        },
        /**
         * @description AI 自动监控功能仅允许自有账号开启；账号归属缺失时按不可开启处理。
         * @param {Object} row 当前行数据
         * @returns {boolean}
         */
        isOwnAiMonitorAccount(row) {
            return this.resolveRowAccountType(row) === 0
        },
        canShowEnableAiMonitor(type, row) {
            const state = this.getAiMonitorState(row)
            const map = {
                qc: { enabledKey: 'qcMonitorEnabled', statusKey: 'qcReportStatus', guardKind: 'qc' },
                restore: { enabledKey: 'restoreMonitorEnabled', statusKey: 'restoreReportStatus', guardKind: 'fidelity' },
                inspect: { enabledKey: 'inspectMonitorEnabled', statusKey: 'inspectReportStatus', guardKind: 'inspect' }
            }
            const item = map[type]
            if (!item) return false
            if (Number(state?.[item.statusKey] ?? 0) === 2) return false
            if (state?.[item.enabledKey]) return false
            if (!this.isOwnAiMonitorAccount(row)) return false
            if (!this.workshopOrAbove) return false
            return this.resolveAiMonitorGuardType(item.guardKind, true, false) === false
        },
        getAnchorUrlUserId(row) {
            const r = row || {}
            const candidates = [
                r.anchorUrlUserId,
                r.anchor_url_user_id,
                r.anchorInfo?.anchorUrlUserId,
                r.anchorInfo?.anchor_url_user_id,
                r.basicSettingsVo?.anchorUrlUserId,
                r.basicSettingsVo?.anchor_url_user_id,
                r.basicSettingsVo?.anchorId,
                r.anchorId
            ]
            for (const item of candidates) {
                const n = Number(item)
                if (Number.isFinite(n) && n > 0) return n
            }
            return null
        },
        openScriptRestorationDrawer(row, action) {
            const state = this.getAiMonitorState(row)
            const anchorUrlUserId = this.getAnchorUrlUserId(row)
            const secUid = row?.secUid || row?.anchorInfo?.secUid || ''
            this.scriptRestorationDrawerRow = row
            this.scriptRestorationDrawerAction = action || 'generate'
            this.scriptRestorationDrawerScene = action === 'enable' ? 'anchorConfig' : 'analysis'
            this.scriptRestorationDrawerForm = {
                ...(state?.restoreConfig || {}),
                ...(anchorUrlUserId ? { anchorUrlUserId } : {}),
                ...(secUid ? { secUid } : {})
            }
            this.scriptRestorationDrawerVisible = true
        },
        async handleScriptRestorationConfirmed() {
            const row = this.scriptRestorationDrawerRow
            if (!row) return
            const secUid = row?.secUid || row?.anchorInfo?.secUid
            if (!secUid || !this.$httpBack?.scriptMonitor?.setMonitorEnabled) return
            try {
                const res = await this.$httpBack.scriptMonitor.setMonitorEnabled({ secUid: String(secUid), monitorType: 1, enabled: 1 })
                if (res?.code === 0) {
                    this.$message.success('已开启')
                    this.loadAiMonitorStatus([row])
                    return
                }
                this.$message.warning(res?.msg || '开启失败')
            } catch (e) {
            }
        },
        async handleScriptRestorationGenerate() {
            const row = this.scriptRestorationDrawerRow
            if (!row?.videoId) return
            if (!this.$httpBack?.scriptMonitor?.triggerScriptMonitorReport) return
            const state = this.getAiMonitorState(row)
            try {
                state.restoreReportStatus = 1
                const res = await this.$httpBack.scriptMonitor.triggerScriptMonitorReport({ videoId: row.videoId, monitorType: 1 })
                if (res?.code !== 0) {
                    state.restoreReportStatus = 0
                    this.$message.warning(res?.msg || '生成失败')
                    return
                }
                setTimeout(() => {
                    this.loadAiMonitorStatus([row])
                }, 1400)
            } catch (e) {
                state.restoreReportStatus = 3
            }
        },
        async enableAiMonitor(type, row) {
            if (!this.$httpBack?.scriptMonitor?.setMonitorEnabled) return
            if (!this.isOwnAiMonitorAccount(row)) return this.$message.warning('仅自有账号支持开启自动监控')
            const secUid = row?.secUid || row?.anchorInfo?.secUid
            if (!secUid) return this.$message.warning('缺少 secUid，无法开启监控')
            const map = { qc: 0, restore: 1, inspect: 2 }
            const monitorType = map[type]
            if (monitorType === undefined) return
            const guardKind = type === 'qc' ? 'qc' : (type === 'inspect' ? 'inspect' : (type === 'restore' ? 'fidelity' : ''))

            const labelMap = {
                qc: '话术质检',
                restore: '话术还原度',
                inspect: '互动巡检'
            }
            const anchorName = row?.anchorInfo?.anchorName || row?.anchorInfo?.AnchorName || ''
            const dialogText = anchorName ? `${anchorName}-是否开启自动${labelMap[type] || '监控'}？` : `是否开启自动${labelMap[type] || '监控'}？`
            const confirmText = `开启自动${labelMap[type] || '监控'}`

            if (type === 'restore') {
                this.runAiMonitorGuard(guardKind, { requireRemain: true, requireVersion: true }, async () => {
                    this.openScriptRestorationDrawer(row, 'enable')
                })
                return
            }

            this.$confirm(`
                    <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                        <div>${dialogText}</div>
                    </div>`, '友情提示', {
                confirmButtonText: confirmText,
                cancelButtonText: '知道了',
                customClass: 'edit-file-name',
                showClose: true,
                showCancelButton: true,
                closeOnClickModal: false,
                closeOnPressEscape: false,
                dangerouslyUseHTMLString: true,
                center: true
            }).then(() => {
                this.runAiMonitorGuard(guardKind, { requireRemain: true, requireVersion: true }, async () => {
                    try {
                        const res = await this.$httpBack.scriptMonitor.setMonitorEnabled({ secUid: String(secUid), monitorType, enabled: 1 })
                        if (res?.code === 0) {
                            this.$message.success('已开启')
                            this.loadAiMonitorStatus([row])
                            return
                        }
                        this.$message.warning(res?.msg || '开启失败')
                    } catch (e) {
                    }
                })
            }).catch(() => {})
        },
        async generateAiReport(type, row) {
            if (!row?.videoId) return
            if (type === 'qc' && Number(row.analysisStatus) !== 2) return
            if (!this.$httpBack?.scriptMonitor?.triggerScriptMonitorReport) return
            const state = this.getAiMonitorState(row)
            const map = { qc: 0, restore: 1, inspect: 2 }
            const monitorType = map[type]
            if (monitorType === undefined) return
            try {
                // 生成报告直接走 triggerReport，由后端统一校验是否可生成，不再以前端监控位/版本 Guard 做前置拦截。
                if (type === 'qc') {
                    state.qcReportStatus = 1
                }
                if (type === 'restore') {
                    state.restoreReportStatus = 1
                }
                if (type === 'inspect') {
                    state.inspectReportStatus = 1
                }
                const res = await this.$httpBack.scriptMonitor.triggerScriptMonitorReport({ videoId: row.videoId, monitorType })
                if (res?.code === 0) {
                    this.$message.success(res?.msg || '已触发生成，请稍后刷新查看状态')
                }
                if (monitorType === 1 && res?.code === 70005) {
                    this.$message.warning(res?.msg || '请先确认标准直播稿')
                    this.openScriptRestorationDrawer(row, 'generate')
                    state.restoreReportStatus = 0
                    return
                }
                // 延迟刷新：给后端落库与状态更新留一点时间
                setTimeout(() => {
                    this.loadAiMonitorStatus([row])
                }, 1400)
            } catch (e) {
                if (type === 'qc') state.qcReportStatus = 3
                if (type === 'restore') state.restoreReportStatus = 3
                if (type === 'inspect') state.inspectReportStatus = 3
                setTimeout(() => {
                    this.loadAiMonitorStatus([row])
                }, 200)
            }
        },
        openAiReport(type, row) {
            if (typeof this._openAiReportDebounced === 'function') {
                this._openAiReportDebounced(type, row)
                return
            }
            this.openAiReportCore(type, row)
        },
        openAiReportCore(type, row) {
            const state = this.getAiMonitorState(row)
            this.aiMonitorReportType = type
            this.aiMonitorReportRow = row
            if (type === 'qc') {
                this.aiMonitorReportId = state.qcReportId
            }
            if (type === 'restore') {
                this.aiMonitorReportId = state.restoreReportId
            }
            if (type === 'inspect') {
                this.aiMonitorReportId = state.inspectReportId
            }
            this.aiMonitorReportVisible = true
        },
        handleAiMonitorReportRead({ reportId, type }) {
            const row = this.aiMonitorReportRow
            if (!row) return
            const state = this.getAiMonitorState(row)
            if (type === 'qc' && String(state.qcReportId || '') === String(reportId || '')) {
                state.qcUnread = false
            }
            if (type === 'inspect' && String(state.inspectReportId || '') === String(reportId || '')) {
                state.inspectUnread = false
            }
            if (type === 'restore' && String(state.restoreReportId || '') === String(reportId || '')) {
                state.restoreUnread = false
            }
        },
        handleAiMonitorConfirmed({ reportId, type }) {
            const row = this.aiMonitorReportRow
            if (!row) return
            const state = this.getAiMonitorState(row)
            if (type === 'qc' && state.qcReportId === reportId) {
                state.qcUnread = false
            }
            if (type === 'inspect' && state.inspectReportId === reportId) {
                state.inspectUnread = false
            }
            if (type === 'restore' && state.restoreReportId === reportId) {
                state.restoreUnread = false
            }
            this.loadAiMonitorStatus([row])
        },
        handleAiMonitorReportOpened() {
        },
        trackReplayListEvent(code) {
            trackEvent(code);
        },
        handleSearch() {
            if (this.replayType === 'replayAll') {
                this.trackReplayListEvent('P002_A0019');
            }
        },
        handleRefresh() {
            this.$refs.table?.refresh();
            if (this.replayType === 'replayAll') {
                this.trackReplayListEvent('P002_A0021');
            }
        },
        /**
         * @description 归一化排班数据的 videoId 字段为数组形式（兼容旧字段为 string 的情况）
         * @param {Object} schedule 单条排班数据
         * @returns {string[]} videoId 数组（去空、转字符串）
         */
        getScheduleVideoIds(schedule) {
            const raw = schedule?.videoId
            const list = Array.isArray(raw) ? raw : (raw ? [raw] : [])
            return list.map(v => String(v)).filter(Boolean)
        },
        /**
         * @description 获取当前录制行命中的排班数据（按 secUid + videoId 匹配；排班 videoId 支持数组）
         * @param {Object} row 当前行数据（录制数据）
         * @returns {Object[]} 命中的排班数据列表
         */
        getRowMatchedSchedules(row) {
            const rowSecUid = row?.secUid || row?.anchorInfo?.secUid
            const rowVideoId = row?.videoId
            if (!rowSecUid || !rowVideoId) return []

            const list = Array.isArray(this.schedulesData) ? this.schedulesData : []
            return list.filter(schedule => {
                if ((schedule?.secUid || '') !== rowSecUid) return false
                const videoIds = this.getScheduleVideoIds(schedule)
                return videoIds.includes(String(rowVideoId))
            })
        },
        getRowSchedulesData(row) {
            const matched = this.getRowMatchedSchedules(row)
            if (!matched.length) return null
            return matched[0] || null
        },
        getRowSchedulesPopoverData(row) {
            const matched = this.getRowMatchedSchedules(row)
            if (!matched.length) return null

            const positionMap = new Map()
            const mergedPositions = []

            for (const schedule of matched) {
                const positions = Array.isArray(schedule?.positions) ? schedule.positions : []
                for (const p of positions) {
                    const positionName = this.getScheduleDisplayPositionName(p)
                    if (!positionMap.has(positionName)) {
                        const next = {
                            positionName,
                            anchorPosition: p?.anchorPosition === true,
                            employees: []
                        }
                        positionMap.set(positionName, next)
                        mergedPositions.push(next)
                    }

                    const target = positionMap.get(positionName)
                    if (p?.anchorPosition === true) target.anchorPosition = true

                    const employees = Array.isArray(p?.employees) ? p.employees : []
                    for (const e of employees) {
                        const employeeName = e?.employeeName || e?.name || e?.nickName || e?.employeeNickName
                        const employeeId = e?.employeeId || e?.id
                        if (!employeeName && !employeeId) continue

                        const has = (target.employees || []).some(x => {
                            const xName = x?.employeeName || x?.name || x?.nickName || x?.employeeNickName
                            const xId = x?.employeeId || x?.id
                            if (employeeId && xId) return String(employeeId) === String(xId)
                            return String(employeeName) === String(xName)
                        })
                        if (!has) target.employees.push(e)
                    }
                }
            }

            return {
                positions: mergedPositions
            }
        },
        getScheduleDisplayPositionName(position) {
            const rawName = String(position?.positionName || '').trim()
            if (position?.anchorPosition === true || rawName.includes('主播')) {
                return '主播'
            }
            return rawName || '未命名岗位'
        },
        getPositionEmployeeNames(position) {
            const employeeNames = []
            const employees = Array.isArray(position?.employees) ? position.employees : []
            for (const employee of employees) {
                const employeeName = employee?.employeeName || employee?.name || employee?.nickName || employee?.employeeNickName
                if (!employeeName || employeeNames.includes(employeeName)) continue
                employeeNames.push(employeeName)
            }
            return employeeNames
        },
        getSchedulePopoverRows(row) {
            const schedulesData = this.getRowSchedulesPopoverData(row)
            const positions = Array.isArray(schedulesData?.positions) ? schedulesData.positions : []
            return positions
            .map(position => {
                const rawPositionName = position?.positionName || ''
                const employeeNames = this.getPositionEmployeeNames(position).join('、')
                return {
                    positionName: rawPositionName || '未命名岗位',
                    employeeNames: employeeNames || '暂无人员'
                }
            })
            .filter(item => !!item?.positionName)
        },
        getAnchorPositionInject(row) {
            const schedulesData = this.getRowSchedulesData(row)
            const positions = Array.isArray(schedulesData?.positions) ? schedulesData.positions : []
            const anchorPositions = positions.filter(p => p?.anchorPosition === true || String(p?.positionName || '').includes('主播'))
            if (anchorPositions.length) {
                const employeeNames = []
                anchorPositions.forEach(position => {
                    this.getPositionEmployeeNames(position).forEach(name => {
                        if (!employeeNames.includes(name)) {
                            employeeNames.push(name)
                        }
                    })
                })
                if (employeeNames.length) {
                    return {
                        anchorPosition: true,
                        anchorPositionName: '主播',
                        anchorEmployeeNames: employeeNames.join('、')
                    }
                }
            }
            return {}
        },
        hasSchedulePopover(row) {
            return this.getSchedulePopoverRows(row).length > 0
        },
        getScheduleDotColor(index) {
            const colors = ['#2FD16C', '#45A7FF', '#9A6BFF', '#F6B24A', '#FF7E98']
            return colors[index % colors.length]
        },
        /**
         * 打开编辑人员弹窗
         * @param {Object} item 当前行数据
         */
        openEditPersonDialog(item = {}) {
            this.currentEditPersonItem = item
            this.editPersonDialogVisible = true
        },
        /**
         * 获取企业管理后台地址
         * 优先读取全局配置，未配置时从 backApiURL 推导根地址。
         * @returns {string}
         */
        getScheduleManageUrl() {
            const config = window.SITE_CONFIG || {}
            const configUrl = config.scheduleManageUrl || config.manageURL || config.enterpriseManageURL || localStorage.getItem('scheduleManageUrl') || ''
            if (configUrl) {
                return configUrl
            }
            const backApiURL = config.backApiURL || ''
            if (!backApiURL) {
                return ''
            }
            return backApiURL.replace(/\/(?:api|replay)(?:\/.*)?$/i, '')
        },
        /**
         * 跳转企业管理后台
         */
        goScheduleManage() {
            const targetUrl = this.getScheduleManageUrl()
            if (!targetUrl) {
                this.$message.warning('未配置企业管理后台地址')
                return
            }
            this.$httpClient.system.openGovernanceWeb()
        },
        columnHidden(items){
            return items.includes(this.replayType)
        },
        cancelVideoAnalysis(row){
            this.$httpClient.anchorvideo.cancelVideoAnalysis({videoId: row.videoId}).then(res=>{
                if(res.code === 0){
                    this.$message.success("停止分析成功");
                    this.getRecordList('cancel')
                }else{
                    this.$message.error(res.msg)
                }
            })
        },
        /**
         * @description 判断当前行是否已进入“加速分析”本地态，避免重复点击。
         * @param {Object} row 当前表格行数据
         * @returns {boolean}
         */
        isSpeedingUpAnalysis(row) {
            const id = row?.videoId
            if (!id) return false
            return !!this.speedUpAnalysisMap[String(id)]
        },
        /**
         * @description 判断当前行是否正在请求加速接口，避免请求未返回前重复点击。
         * @param {Object} row 当前表格行数据
         * @returns {boolean}
         */
        isSpeedingUpAnalysisLoading(row) {
            const id = row?.videoId
            if (!id) return false
            return !!this.speedUpAnalysisLoadingMap[String(id)]
        },
        getSpeedUpStorageKey() {
            const userInfo = this.$store?.getters?.getUserInfo || {}
            const userId = userInfo?.userId || userInfo?.id || ''
            return `speedUpAnalysisIds_${userId || 'default'}`
        },
        restoreSpeedUpAnalysisState() {
            if (this.replayType !== 'replayAll') return
            const key = this.getSpeedUpStorageKey()
            const raw = localStorage.getItem(key)
            if (!raw) {
                this.speedUpAnalysisMap = {}
                this.startListTimer()
                return
            }
            try {
                const ids = (JSON.parse(raw) || []).map(String).filter(Boolean)
                const map = {}
                ids.forEach((id) => {
                    map[id] = true
                })
                this.speedUpAnalysisMap = map
            } catch (e) {
                this.speedUpAnalysisMap = {}
            }
            this.startListTimer()
        },
        persistSpeedUpAnalysisState() {
            if (this.replayType !== 'replayAll') return
            const key = this.getSpeedUpStorageKey()
            try {
                const ids = Object.keys(this.speedUpAnalysisMap || {}).map(String).filter(Boolean)
                localStorage.setItem(key, JSON.stringify(ids))
            } catch (e) {
            }
        },
        /**
         * @description 根据最新列表状态同步本地加速态，分析状态流转后自动移除“正在加速中”标记。
         * @param {Array} list 当前列表数据
         * @returns {void}
         */
        syncSpeedUpAnalysisState(list = []) {
            const active = new Set(
                (Array.isArray(list) ? list : [])
                    .filter(r => Number(r?.analysisStatus) === 1 && r?.videoId)
                    .map(r => String(r.videoId))
            )
            Object.keys(this.speedUpAnalysisMap || {}).forEach((id) => {
                if (!active.has(String(id))) {
                    this.$delete(this.speedUpAnalysisMap, id)
                }
            })
            this.persistSpeedUpAnalysisState()
        },
        /**
         * @description 点击“加速分析”后的本地交互入口，先将按钮置为进行中，后续由列表刷新结果接管状态。
         * @param {Object} row 当前表格行数据
         * @returns {void}
         */
        speedUpAnalysis(row) {
            const id = row?.videoId
            if (!id) return
            if (this.isSpeedingUpAnalysis(row) || this.isSpeedingUpAnalysisLoading(row)) return
            const key = String(id)
            this.$set(this.speedUpAnalysisLoadingMap, key, true)
            this.$httpClient.anchorvideo.accelerate({ videoId: id }).then((res) => {
                if (res?.data?.accepted) {
                    this.$set(this.speedUpAnalysisMap, key, true)
                    this.persistSpeedUpAnalysisState()
                    this.$message.success('已加速，剩余内容将由云端快速识别')
                    this.startListTimer()
                    return
                }
                this.$message.warning('请稍候，分析开始后再试')
            }).catch(() => {
            }).finally(() => {
                this.$delete(this.speedUpAnalysisLoadingMap, key)
            })
        },
        toggleCompere(){
            this.toggleMenu = !this.toggleMenu
        },
        setClass (row = {}) {
            if (!row.aiReportStaus) {
                return 'grey-text'
            } else if (row.aiReportStaus === 1) {
                return 'blue-text'
            } else if (row.aiReportStaus === 2) {
                return 'red-text'
            } else {
                return ''
            }
        },
        createText(val, sourceId) {
            this.$httpClient.anchorvideo.generateVideoContent({
                sourceId: sourceId,
                sourceType: 0,
                type: val
            }).then(res => {
                if (res.code === 0){
                    const mapTips = new Map([[1, 'AI脚本拆解'], [2, '优化原文']])
                    this.$message.success(`正在生成${mapTips.get(val)}，请稍后...`)
                    this.$refs.export_dialog?.hide()
                }else {
                    this.$message.error('生成优化原文失败')
                }
            })
        },
        async getDictDataListByCode(index) {
            const result = await this.$httpBack.dictdata.dictDataListByCode({
                code: 'video_slice_type'
            })
            const list = result.data || []
            list.map((item, index) => {
                const value = cloneDeep(item.value)
                item.children = JSON.parse(value)
                item.value = index
            })
            const resultList = list[index]?.children || []
            this.setFormConfigDic({3: resultList}, this.formConfig, {
                sliceClass: (opts, form) => {
                    return [].concat(form.config?.options?.shift(), opts);
                }
            })
            this.sliceOptions = list[index]?.children
        },
        async initData () {
            this.reportNoticePending = !this.hasShownInitReportNotice
            this.loadAiMonitorQuota()
            this.fileDataForm = this.aIFinish ? {
                secUid: "",
            } :this.getRouteQuery(this.fileDataForm)
            if (this.fileDataForm.time) {
                // this.searchData.searchDate = [this.fileDataForm.time,this.fileDataForm.time];
                this.$refs.table?.updatedSearch('searchDate', [this.fileDataForm.time, this.fileDataForm.time])
                delete this.fileDataForm.time
            }
            await this.getTradeTreeList()

            if (this.replayType === 'replayShort') {
                let anchor = await this.getListByAnchor()
                this.setFormConfigDic({2: anchor}, this.formConfig, {
                    tradeId: (opts, form) => {
                        return [].concat(form.config?.options?.shift(), opts);
                    }
                })
            }
            if(this.replayType !== 'replayAll'){
                await this.getDictDataListByCode(this.replayType === 'replaySection' ? 0 : 1)
            }

            // 列表定时器
            this.clearInit()
            // 开启列表定时器
            // this.startListTimer();
            this.$nextTick(() => {
                this.$refs.table?.getList();
                if(this.$store.getters.largeEnterprises){
                }
            })
        },
        resData(data){
            return data
        },
        dataCallback(list){
            this.syncSpeedUpAnalysisState(list)
            this.loadAiMonitorStatus(list)
            this.$nextTick(async()=>{
                if(this.$store.getters.largeEnterprises){
                    await this.$httpBack2?.liveRoom?.batchPlan(list.map(d=>{
                        return {
                            livePlatformType: d?.anchorInfo?.platform,
                            videoId: d.videoId,
                            secUid: d.secUid,
                            startTime: d.startTime,
                            endTime: d.endTime
                        }
                    })).then(res=>{
                        if(res.code === 0){
                            this.schedulesData = res.data || [];
                        }
                    })
                }
            })
        },
        // 获取智能复盘列表
        getRecordList (type) {
            this.$nextTick(() => {
                this.$refs.table?.getList(type || 'init')
            })
        },
        // 清除列表定时器
        stopListTimer () {
            if (this.listTimer) {
                clearTimeout(this.listTimer)
                this.listTimer = null
            }
        },
        startListTimer () {
            if (this.replayType !== 'replayAll') return
            this.stopListTimer()
            const speedingUpIds = Object.keys(this.speedUpAnalysisMap || {})
            if (!speedingUpIds.length) return
            this.listTimer = setTimeout(() => {
                this.getRecordList('notClearSelection')
            }, 60000)
        },
        clearInit () {
            // 列表定时器
            this.stopListTimer()
        },
        compereClick (item) {
            this.fileDataForm.secUid = item.secUid
            this.setRouteQuerys(this.fileDataForm)
            // 判断是否有路径参数
            if (this.$route.query?.secUid) {
                // 清空参数
                this.$router.push({
                    path: this.$route.path,
                })
            }
            this.$refs.table.updatedSearch('searchDate', [])
            this.$refs.table.currentChange(1)
        },
        tableSelect ({ list }) {
            this.selectList = list
        },
        delConfirm({ list, callback }){
            if (list?.length > 1 && this.replayType === 'replayAll') {
                this.trackReplayListEvent('P002_A0020')
            }
            this.delVideoVisible = true;
            this.selectDelList = list
            this.delConfirmCallback = callback

            // this.$confirm('将永久删除选中的文件, 是否继续?', '提示', {
            //     confirmButtonText: '确定',
            //     cancelButtonText: '取消',
            //     type: 'warning'
            // }).then(() => {
            //     this.deleteFile({list, callback })
            // })
        },
        delFileType(type){
            this.deleteFile({
                list: this.selectDelList,
                type,
                callback: this.delConfirmCallback ? this.delConfirmCallback : () => {
                    this.$refs.compere?.getCompereList()
                }
            })
            this.delVideoVisible = false
            this.selectDelList = []
        },
        deleteFile({list, callback, type}) {
            let ids = list.map(d => d.videoId)
            const httpServer = type === 0 ? this.$httpClient.video.deletebyids : this.$httpClient.video.deleteLocalVideoByIds
            httpServer(ids).then(res => {
                // this.$httpBack.video.clientDeleteVideo(ids).then(res=>{
                if (res.code === 0) {
                    this.$message.success('删除成功')
                    this.getRecordList('del')
                    // 执行数据重置
                    typeof callback === 'function' ? callback() : null
                    this.delConfirmCallback = null
                    this.$refs.table.getList()
                    // this.$httpClient.video.deletebyids(ids).then(res => {})
                } else {
                    this.$message.warning('请等待分析结束后再删除')
                }
                this.appVnode?.getDisk()
            })
        },
        // 查看分析结果
        lockAnalysis (id, item) {
            this.hintDay15(item.createDate);
            const {path} = this.$route;
            this.$router.push({
                path: `${path}/analysis`,
                query: {
                    id: id
                }
            })
        },
        toAiAnalysis (row) {
            const {path} = this.$route
            this.$router.push({
                path: `${path}/aiAnalysis`,
                query: {
                    id: row.videoId
                }
            })
        },
        /**
         * @description 组装复盘列表“AI诊断直播”跳转工作台所需的定位参数。
         * @param {Object} row 当前行数据
         * @returns {{ secUid: string, videoId: string, cue: string }}
         */
        getAiAgentWorkbenchParams (row = {}) {
            const secUid = String(row?.secUid || row?.SecUid || row?.anchorInfo?.secUid || row?.anchorInfo?.SecUid || '').trim()
            const videoId = String(row?.videoId || row?.VideoId || row?.id || '').trim()
            return {
                secUid,
                videoId,
                cue: '0'
            }
        },
        openAiAgentWorkbench (row = {}) {
            const { secUid, videoId, cue } = this.getAiAgentWorkbenchParams(row)
            if (!(secUid && videoId)) {
                this.$message.warning('当前直播暂未生成 AI 诊断入口')
                return
            }
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb({
                    secUid,
                    videoId,
                    cue,
                    panel: AI_WORKBENCH_PANELS.ROOM
                })
                return
            }
            if (this.$store.getters.getVersionType !== VERSION_TYPE.AGENT) {
                this.$router.push({
                    path: '/dataAnalysis'
                })
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
        // 获取行业列表树形
        getTradeTreeList () {
            this.tradeTreeList = []
            return  this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                    return res.data
                }
            })
        },
        getListByAnchor () {
            this.listByAnchor = []
            return  this.$httpBack.trade.listByAnchor({}).then((res) => {
                if (res && res.code === 0) {
                    this.listByAnchor = res.data
                    return res.data
                }
            })
        },
        // 打开目录
        openFolder (videoId) {
            this.$httpClient.video.openFolder({ videoId }).then((res) => {
                if (res.code == 0) { }
            })
        },
        // 预览
        preview (videoId) {
            this.$httpClient.video.preview({ videoId }).then((res) => {
                if (res.code == 0) {
                    window.open(res.data, '_blank')
                }
            })
        },
        // 确认生成直播分析
        createAnalysisConfirm ({ id, data, callback }) {
            let token = this.$store.state.token
            // 储存数据
            this.tradeId = id || '1'
            localStorage.setItem('saveAnalysisTrade' + this.currentSecUid, id)
            if (data.isReAnalysis) {
                // 重新分析
                this.$httpClient.video.reanalysis({ videoId: data.videoId, token, tradeId: id }).then((res) => {
                    if (res.code == 0 && res.data) {
                        // this.$refs.table.getList();
                        this.getRecordList('anew')
                        callback?.()
                        this.$message.success('已重新加入分析排队中等待分析')
                    } else {
                        this.$message.warning('请等待上一个分析完再继续')
                    }
                })
            } else {
                // 创建分析
                this.$httpClient.video.createAnalysis({ videoId: data.videoId, token, tradeId: id }).then((res) => {
                    if (res.code == 0 && res.data) {
                        // this.$refs.table.getList()
                        this.getRecordList('create')
                        callback()
                    } else {
                        this.$message.warning('请等待上一个分析完再继续')
                    }
                })
            }
        },

        // 生成直播分析弹窗
        createAnalysis (videoId, SecUid) {
            // 设置默认行业
            this.tradeId = localStorage.getItem('saveAnalysisTrade' + SecUid) || '1'
            this.$refs.tardeDialog.show({
                data: {
                    isReAnalysis: false,
                    videoId
                },
                id: this.tradeId || '1'
            })
        },
        afreshAnalysis (item) {
            const { videoId, tradeId} = item;
            let data = {
                id: tradeId,
                isReAnalysis: true,
                videoId:videoId
            }
            this.createAnalysisConfirm({
                id: tradeId,
                data
            })
        },
        // 重新生成分析
        reAnalysis (videoId, SecUid) {
            // 设置默认行业
            this.tradeId = localStorage.getItem('saveAnalysisTrade' + SecUid) || '1'
            // 
            this.$refs.tardeDialog.show({
                data: {
                    isReAnalysis: true,
                    videoId
                },
                id: this.tradeId || '1'
            })
        },
        async generateReport(item) {
            try {
                const {data: result, code} = await this.$httpClient.export.generateReport({
                    videoId: item.videoId,
                    fileName: item.fileName,
                    uploadType: item.uploadType
                })
                if (code !== 0) return this.$message.error(result.msg)
            } catch (e) {
            }
        },
        visibleReportChange(visible,row){
            if (visible) {
                if ([row.hasDataDiagnosisReport, row.hasDiagnosisReport].includes(1)) {
                    if (row?.basicSettingsVo?.accountType === 0) {
                        //列表-查看诊断
                        this.trackReplayListEvent('P002_A0030')
                    } else {
                        //列表-查看竞品分析
                        this.trackReplayListEvent('P002_A0024')
                    }
                }
            }
        },
        async viewAiReport(command, item) {
            if (command === 'content') {//内容诊断
                if (item.hasDiagnosisReport === 1) {//有内容诊断
                    if (item?.basicSettingsVo?.accountType === 0) {
                        //列表-XX-查看内容诊断
                        this.trackReplayListEvent('P002_A0054')
                    } else {
                        //列表-XX-查看内容分析
                        this.trackReplayListEvent('P002_A0051')
                    }
                }
                await this.$refs.ai_content_report?.changeDrawerStatus(true, item)
            } else {//数据诊断
                if (item.hasDataDiagnosisReport === 1) {//有数据诊断
                    if (item?.basicSettingsVo?.accountType === 0) {
                        //列表-XX-查看数据诊断
                        this.trackReplayListEvent('P002_A0053')
                    } else {
                        //列表-XX-查看数据分析
                        this.trackReplayListEvent('P002_A0050')
                    }
                }
                await this.$refs.ai_data_report?.changeDrawerStatus(true, item)
            }
        },
        tableHttp (param) {
            const resultParams = {
                page: param.pageIndex,
                limit: param.pageSize,
                secUid: this.fileDataForm.secUid,
                tradeId: '',
                analysisStatus: param.analysisStatus,
                recordStartDate: param?.searchDate?.[0] || '',
                recordEndDate: param?.searchDate?.[1] || '',
                analysisStartDate: '',
                analysisEndDate: '',
            }
            if (this.replayType !== 'replayShort') {
                resultParams.hasDiagnosis = this.aIFinish ? 1 : ''
                // resultParams.hasDiagnosisReport = this.aIFinish ? 1 : ''
            }
            if (this.replayType === 'replayAll') {
                this.fileDataForm = {
                    ...resultParams,
                    videoSliceType: 0
                }
            } else if (this.replayType === 'replaySection') {
                this.fileDataForm = {
                    ...resultParams,
                    videoSliceType: 1,
                    sliceClass: param.sliceClass
                }
            } else {
                this.fileDataForm = {
                    ...resultParams,
                    videoSliceType: 2,
                    sliceClass: param.sliceClass,
                    tradeId: param.tradeId,
                    anchorName:param.anchorName
                }
            }
            this.setRouteQuerys(this.fileDataForm)
            return {
                http: this.$httpBack.video.clientVideoList,
                param: this.fileDataForm,
                isVideoFormat: true,
                callback:(res)=>{
                    if (this.reportNoticePending && !this.hasShownInitReportNotice && this.replayType === 'replayAll' && !this.versionTypeIsPure) {
                        this.showInitReportNotice()
                    }
                    if (this.replayType === 'replayAll') {
                        this.syncSpeedUpAnalysisState(res?.data?.list || [])
                        if (!localStorage.getItem('isExampleList') || isNaN(parseInt(localStorage.getItem('isExampleList')))) {
                            localStorage.setItem('isExampleList', res?.data?.list?.length || 0);
                        }
                        this.isExampleList = res?.data?.list?.length || parseInt(localStorage.getItem('isExampleList') || '0');
                        this.startListTimer();
                    }
                }
            }
        },
        async showInitReportNotice() {
            this.reportNoticeLoading = true
            try {
                const result = await this.$httpBack.v2500.listUnreadDataDiagnosis()
                const list = result?.data || []
                this.reportNoticeRecords = list.map(item => ({
                    ...item,
                    avatar: item.anchorAvatar,
                    roomName: item.anchorName || item.videoName,
                    recordTime: this.formatReportNoticeTime(item.startTime, item.endTime),
                    readStatus: item.isRead,
                    hasDataDiagnosisReport: 1
                }))
                this.reportNoticeVisible = this.reportNoticeRecords.length > 0
            } catch (e) {
                this.reportNoticeVisible = false
            } finally {
                this.reportNoticeLoading = false
                this.reportNoticePending = false
                this.hasShownInitReportNotice = true
            }
        },
        formatReportNoticeTime(startTime, endTime) {
            const startText = startTime ? startTime.substring(5, 16) : ''
            const endText = endTime ? endTime.substring(11, 16) : ''
            return endText ? `${startText}-${endText}` : startText
        },
        handleViewReportNotice(record) {
            this.viewAiReport('data', record)
        },
        howToSlice(){
            this.$refs.slice_tutorial?.open()
        },
        visibleChange(visible){
            if (this.replayType === 'replayAll' && visible) {
                this.trackReplayListEvent('P002_A0032');
            }
        }
    },
}
</script>


<style lang="scss">

.schedule-popover-popper{
    min-width: 132px;
    max-width: 220px;
    padding: 8px 10px !important;
    border: none !important;
    border-radius: 8px !important;
    background: rgba(0, 0, 0, 0.86) !important;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.28);
}

::v-deep(.schedule-popover-popper .schedule-popover-content) {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

::v-deep(.schedule-popover-popper .schedule-popover-row) {
    display: flex;
    align-items: center;
    font-size: 12px;
    line-height: 18px;
    color: #FFFFFF;
}

::v-deep(.schedule-popover-popper .schedule-popover-dot) {
    display: inline-block;
    width: 6px;
    height: 6px;
    margin-right: 8px;
    border-radius: 50%;
    flex-shrink: 0;
}

::v-deep(.schedule-popover-popper .schedule-popover-position) {
    color: #FFFFFF;
    flex-shrink: 0;
}

::v-deep(.schedule-popover-popper .schedule-popover-employees) {
    color: #FFFFFF;
    word-break: break-all;
}

.edit-person-dialog {
    .edit-person-dialog-content {
        min-height: 180px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 22px 0 8px;
    }

    .edit-person-dialog-text {
        margin-bottom: 56px;
        font-size: 16px;
        line-height: 22px;
        color: #303133;
        text-align: center;
    }

    .edit-person-dialog-link {
        color: #444DFF;
        cursor: pointer;
    }

    ::v-deep(.el-dialog) {
        border-radius: 8px;
        overflow: hidden;
    }

    ::v-deep(.el-dialog__header) {
        padding: 16px 20px;
        border-bottom: 1px solid #EEF1F5;
    }

    ::v-deep(.el-dialog__title) {
        font-size: 20px;
        font-weight: 500;
        color: #303133;
    }

    ::v-deep(.el-dialog__body) {
        padding: 0 20px 24px;
    }

    ::v-deep(.el-dialog__headerbtn) {
        top: 18px;
        right: 20px;
    }

    ::v-deep(.el-dialog__close) {
        font-size: 18px;
        color: #8F9BB3;
    }
}

.speedUpLabel {
    color: #2FD16C;
    display: inline-flex;
    align-items: center;

    &--disabled {
        color: #C0C4CC;
        cursor: not-allowed;
    }
}

.speedUpCaret {
    display: inline-block;
    margin-left: 2px;
    font-size: 12px;
    transform-origin: center center;
    animation: speedUpCaretPulse 0.9s infinite ease-in-out;
    color: rgba(47, 209, 108, 0.35);
}

.speedUpCaret--1 {
    margin-left: 2px;
}

.speedUpCaret--2 {
    margin-left: -4px;
    animation-delay: 0.12s;
}

.speedUpCaret--3 {
    margin-left: -4px;
    animation-delay: 0.24s;
}

@keyframes speedUpCaretPulse {
    0% {
        transform: translateX(0) scale(0.85);
        opacity: 0.3;
        color: rgba(47, 209, 108, 0.3);
    }
    50% {
        transform: translateX(2px) scale(1.05);
        opacity: 1;
        color: rgba(47, 209, 108, 1);
    }
    100% {
        transform: translateX(4px) scale(0.85);
        opacity: 0.3;
        color: rgba(47, 209, 108, 0.3);
    }
}


</style>

<style scoped lang="less">
.emptyTipText {
    font-weight: 400;
    font-size: 14px;
    color: #677583;
}

.emptyContainer {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    margin-top: 100px;
}

.startTimeBox{
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: flex-start;
}

.recordColContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}

.compereNameText {
    font-weight: 400;
    font-size: 13px;
    color: #677583;
    margin-top: 4px;
    text-align: start;
    // white-space: nowrap;
    // overflow: hidden;
    // text-overflow: ellipsis;
    // max-width: 260px;
}

.videoNameText {
    font-weight: 400;
    font-size: 14px;
    color: #2E3742;
    text-align: start;
    // white-space: nowrap;
    // overflow: hidden;
    // text-overflow: ellipsis;
    // max-width: 260px;
}

.fileNameContainer {
    display: flex;
    flex-direction: column;
    align-items: start;
    margin-left: 12px;
}

.videoImg {
    width: 40px;
    height: 40px;
    border-radius: 50%;
}

.fileColContainer {
    //display: flex;
    align-items: center;
}



.recordListRightContainer {
    margin-left: 10px;
    position: relative;
    flex: 1;
    // height: ;
}

.recordListContainer {
    box-sizing: content-box;
    // padding-bottom: 20px;
}

.contrast-box {
    padding: 10px;
    margin: 0;

    > * {
        margin: 0 5px;
    }
}

.report-badge ::v-deep .el-badge__content.is-fixed {
    top: 8px;
    right: -5px;
}

.aiMonitorCell{
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px 0;
    font-size: 12px;
    color: #2E3742;
}

.aiMonitorActions{
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    line-height: 1;
    width: 100%;
    .el-button{
        margin-left: 0 !important;
    }
}

.aiMonitorLink{
    padding: 0;
    height: 18px;
    line-height: 18px;
    width: 100%;
    display: flex;
    justify-content: center;
}

.aiMonitorLinkDisabled{
    height: 18px;
    line-height: 18px;
    color: #95A1AF;
    cursor: default;
    user-select: none;
    width: 100%;
    display: flex;
    justify-content: center;
}

.aiMonitorBadge{
    line-height: 1;
}

.aiMonitorBadge ::v-deep .el-badge__content.is-fixed{
    top: 4px;
    right: -6px;
}

.aiMonitorStats{
    display: flex;
    flex-direction: column;
    gap: 2px;
    align-items: flex-start;
}

.aiMonitorPreview{
    cursor: pointer;
    width: 100%;
}

.aiMonitorStatRow{
    display: flex;
    align-items: center;
    gap: 4px;
    line-height: 16px;
}

.aiMonitorStatLabel{
    color: #677583;
}

.aiMonitorStatValue{
    color: #2E3742;
    cursor: pointer;
}
</style>

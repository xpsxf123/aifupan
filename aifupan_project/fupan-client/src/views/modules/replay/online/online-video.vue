<template>
    <div>
        <CoreTable
            :searchConfig="formConfig"
            :menuConfig="menuConfig"
            :getDataApi="getVideoList"
            ref="table"
            :column="column"
            :buffer="isIframe?'online-offcial-video-list':'online-video-list'"
            :table-height="tableH || tableHeight"
            :notSearch="iframeNotLogin"
            :notPage="iframeNotLogin"
            @deletes="deletes"
            :table-select="enableBatchDelete"
            row-key="videoId"
            :table-config="{
                'show-overflow-tooltip': false
            }"
        >
            <template #searchRight>
                <div v-if="space">容量：{{ space }}</div>
            </template>
            <template #viewersNum="{row}">
                <div class="font-s14">
                    <div>场观: {{ conversion(row?.observationNum) }}</div>
                    <div>销售: {{ dataView(row, ['volumeStart', 'volumeEnd']) }}</div>
                </div>
            </template>
            <template #speechQc="{row}">
                <div v-if="Number(row?.basicSettingsVo?.accountType ?? 0) !== 0">-</div>
                <div v-else>
                    <template v-if="getAiMonitorState(row).qcReportStatus === 2">
                        <AiMonitorPreviewContent
                            :content="getAiMonitorState(row).qcPreview"
                            preview-type="qc"
                            parse-mode="structured"
                            with-stats-style
                            @view="openAiReport('qc', row)"
                        />
                    </template>
                    <template v-else>
                        -
                    </template>
                </div>
            </template>
            <template #scriptRestore="{row}">
                <div v-if="Number(row?.basicSettingsVo?.accountType ?? 0) !== 0">-</div>
                <div v-else>
                    <template v-if="getAiMonitorState(row).restoreReportStatus === 2">
                        <AiMonitorPreviewContent
                            :content="getAiMonitorState(row).restorePreview"
                            preview-type="restore"
                            parse-mode="text"
                            with-stats-style
                            @view="openAiReport('restore', row)"
                        />
                    </template>
                    <template v-else>
                        -
                    </template>
                </div>
            </template>
            <template #interactionInspect="{row}">
                <div v-if="Number(row?.basicSettingsVo?.accountType ?? 0) !== 0">-</div>
                <div v-else>
                    <template v-if="getAiMonitorState(row).inspectReportStatus === 2">
                        <AiMonitorPreviewContent
                            :content="getAiMonitorState(row).inspectPreview"
                            preview-type="inspect"
                            parse-mode="text"
                            with-stats-style
                            @view="openAiReport('inspect', row)"
                        />
                    </template>
                    <template v-else>
                        -
                    </template>
                </div>
            </template>
            <template #table="param">
                <slot name="table" v-bind="{...param, reportEd}"></slot>
            </template>
            <template #anchorName="{row:item}">
                <Anchor
                    @editFileName="()=>editFileName(item,getList,'online')"
                    :item="{...item.anchorInfo,...getMark(item)}"
                    :notLiveStatus="true"></Anchor>
            </template>
            <template #videoName="{row:item}">
                <div class="fileNameContainer">
                    <div class="videoNameText slh">{{ item.videoName }}</div>
                    <div class="compereNameText slh">{{ item.liveTitle }}</div>
                </div>
            </template>
            <!--
            <template #scriptRestore="{row, $index}">
                <ScriptRestoreCell :row="row" :mock-index="$index" :read-only="true" :readonly-report="true" />
            </template>
            <template #interactionInspect="{row, $index}">
                <InteractionInspectCell :row="row" :use-mock="false" :read-only="true" :readonly-report="true" />
            </template>
            -->
            <template #aIReport="{row:item}">
                <el-dropdown @command="(command)=>viewAiReport(command,item)" trigger="click"
                             :disabled="!item.hasDataDiagnosisReport && !item.dataDiagnosisOssName && !item.diagnosisOssName && !item.hasDiagnosisReport">
                    <span class="el-dropdown-link cursor-pointer">
                        <span :style="{color: !item.hasDataDiagnosisReport && !item.hasDiagnosisReport?'#ABAEB3':'#15ACFE'}">
                           {{ item?.basicSettingsVo?.accountType === 0 ? '查看诊断分析' : '查看竞品分析' }}
                        </span>
                    </span>
                    <el-dropdown-menu slot="dropdown">
                        <template v-if="item?.basicSettingsVo?.accountType===0">
                            <el-dropdown-item command="data" v-if="item.hasDataDiagnosisReport===1&&item.dataDiagnosisOssName">查看数据诊断
                            </el-dropdown-item>
                            <el-dropdown-item command="content" v-if="item.hasDiagnosisReport===1&&item.diagnosisOssName">查看内容诊断
                            </el-dropdown-item>
                        </template>
                        <template v-else>
                            <el-dropdown-item command="data" v-if="item.hasDataDiagnosisReport===1&&item.dataDiagnosisOssName">查看数据分析
                            </el-dropdown-item>
                            <el-dropdown-item command="content" v-if="item.hasDiagnosisReport===1&&item.diagnosisOssName">查看内容分析
                            </el-dropdown-item>
                        </template>
                    </el-dropdown-menu>
                </el-dropdown>
            </template>
            <template #startTime="{row:item}">
                <div class="fileSizeColContainer" style="flex-direction: column;">
                    <div>{{ item.startTime.substring(0, 16) }}</div>
                    <div>录制时长：{{ item.durationStr }}</div>
                    <div class="file-size-text">文件大小：{{ item.vedioSizie ? `${item.vedioSizie}M` : '-' }}</div>
                </div>
            </template>
            <template #analysisTime="{row:item}">
                <div class="analysisDateColContainer">
                    <div>{{ item.analysisTime.substring(0, 10) }}</div>
                    <div>{{ item.analysisTime.substring(10) }}</div>
                </div>
            </template>
            <template #menu="{ row,menu }">
                <template v-for="option in menu.options">
                    <Operation :options="option" :data="row"></Operation>
                </template>
            </template>
            <template #empty>
                <slot name="empty"></slot>
            </template>
            <template #page-before>
                <div v-if="$isAifupan" class="font-s12 flex-ji-c text-colorTheme cs-p" @click="toOfficialWebsite">
                    <img src="@/assets/imgs/rightgif.gif" style="max-width: 24px;" alt="" srcset="">
                    <span style="width: 200px;">前往官网，可进行网页查看云空间</span>
                </div>
            </template>
            <template #page-after>
                <div style="width: 230px;"></div>
            </template>
            <template #tableTop>
                <div v-if="contrastList.length" class="contrast-box main-bg">
                    <el-tag v-for="item in contrastList" closable @close="cancelContrast(item)" class="brs-40">
                        {{ item.videoName }}
                    </el-tag>
                    <afp-button v-show="contrastList.length>=2"  type="primary"
                                @click="modalContrast">开始对比
                    </afp-button>
                </div>
            </template>
        </CoreTable>
        <Summary ref="summary"></Summary>
        <FileRemark ref="fileRemark"/>
        <ReviewContrast ref="review_contrast" @contrastSubmit="contrastSubmit" @swapObj="swapObj"/>
        <AiMonitorReportDialog
            v-if="!versionTypeIsPure"
            :visible.sync="aiMonitorReportVisible"
            :type="aiMonitorReportType"
            :row="aiMonitorReportRow"
            :reportId="aiMonitorReportId"
            :readonly="true"
        />
    </div>
</template>

<script>
import myUtils from '@/utils/utils.js';
import CoreTable from '@/components/coreTable/index.vue'
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue';
import commonHttp from '@/mixins/commonHttp';
import commonUtils from '@/utils/common.js'
import {toOfficialWebsite} from '@/utils/common';
import summaryMixin from '@/components/summary/mixin';
import contrastMixin from '@/mixins/contrastMixin';
import fileRemarkMixin from '@/components/fileRemark/mixin';
import tabsMixin from '@/mixins/tabs';
import {cloneDeep} from "lodash";
import viewVideo from '@/mixins/viewVideo'
import Operation from "@/components/Table/operation.vue";
// import ScriptRestoreCell from '@/components/aiMonitor/scriptRestoreCell/index.vue'
// import InteractionInspectCell from '@/components/aiMonitor/interactionInspectCell/index.vue'
import AiMonitorReportDialog from '@/components/aiMonitor/reportDialog/index.vue'
import table from '@/mixins/table'
import AiMonitorPreviewContent from '@/components/aiMonitor/previewContent/index.vue'
import { VERSION_TYPE } from '@/enum'

export default {
    name: 'ReplayClientAnalysisFinishList',
    components: {
        Operation,
        CoreTable,
        Anchor,
        AiMonitorReportDialog,
        AiMonitorPreviewContent,
        // ScriptRestoreCell,
        // InteractionInspectCell
    },
    inject: ['appVnode'],
    props: {
        tableH: {
            type: String,
            default: ''
        },
        isIframe: {
            type: Boolean,
            default: false
        },
        type: {
            type: String,
            default: ''
        },
    },
    computed: {
        // contrastList() {
        //     return Object.values(this.contrastMap)
        // },
        iframeNotLogin(){
            return this.isIframe && !this.$store?.state?.userInfo?.id;
        },
        getMark() {
            return (item) => {
                return {
                    cloudRename: item.cloudRename,
                    videoRename: item.videoRename,
                    existBarrage: item.existBarrage,
                    existDataBoard: item.existDataBoard,
                    hasDiagnosisReport: item.hasDiagnosisReport || item.hasDataDiagnosisReport,
                    existsMark: item.existsMark,
                    existsNotes: item.existsNotes,
                    notesSummary: item.notesSummary
                }
            }
        },
        userProperty(){
            return this.$store.getters.getUserproperty
        },
        space(){
            if(this.userProperty?.totalStorageNum === undefined || this.userProperty?.storageNum === undefined){
                return '0/0 G';
            }
            let a = (this.userProperty?.totalStorageNum - this.userProperty?.storageNum) / 1024 / 1024;
            let b = this.userProperty?.totalStorageNum / 1024 / 1024;
            return `${this.retainDecimals(a)} / ${this.retainDecimals(b)} G`
        },
        videoSliceType(){
            // if(this.isIframe){return {videoSliceType:0};};
            return {videoSliceType: this.type === 'slice'?1:0}
        },
        dataView () {
            return (resData, keys, length) => {
                return myUtils.dataView(resData, keys, length)
            }
        },
        conversion () {
            return (value) => {
                return myUtils.fnw(value)
            }
        },
        enableBatchDelete() {
            if (this.isIframe) return false
            return !!this.$store.getters.getMode
        },
        versionTypeIsPure() {
            return this.$store.getters.getVersionType === VERSION_TYPE.PURE || this.$store.getters.isPure
        }
    },
    mixins: [commonHttp, summaryMixin, fileRemarkMixin, tabsMixin, contrastMixin, viewVideo, table],
    data() {
        return {
            aiMonitorStateMap: {},
            aiMonitorReportVisible: false,
            aiMonitorReportType: '',
            aiMonitorReportId: null,
            aiMonitorReportRow: null,
            formConfig: {
                items: [
                    {
                        label: '行业筛选', prop: 'tradeId',
                        temp: 'Select',
                        config: {
                            filterable: true,
                            clearable: true,
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
                                width: '150px'
                            }
                        }
                        // hidden: ()=>{
                        //     return !this.isIframe
                        // }
                    },
                    {
                        label: '直播间搜索', prop: 'anchorName',
                        // temp: 'Select',
                        placeholder: "输入直播间名称",
                        hide: ()=>{
                            return this.type === 'file' || this.type === 'slice'
                        },
                        config: {
                            options: [],
                            style: {
                                width: '150px'
                            }
                        },
                    },
                    {
                        label: '上传账号', prop: 'userKeyword',
                        placeholder: "输入上传账号",
                        config: {
                            style: {
                                width: '150px'
                            }
                        },
                    },
                    {
                        label: '切片类型', prop: 'sliceClass',
                        temp: 'Select',
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
                        // on: {
                        //     input: true
                        // },
                        hidden: ()=>{
                            return this.type === 'slice'
                        }
                    },
                    {
                        label: this.isIframe?'时间':'录制时间', prop: 'recordDate',
                        temp: 'DatePicker',
                        config: {
                            type: "daterange",
                            valueFormat: "yyyy-MM-dd",
                            style: {
                                width: '260px'
                            }
                        }
                    },
                    {
                        label: '账号筛选',
                        prop: 'accountType',
                        hide: ()=>{
                            return this.type === 'file' || this.type === 'slice'
                        },
                        temp: 'Radio',
                        config: {
                            default: '',
                            options: [{label:'全部账号', value: ''}, {label:'自有账号', value: 0},{label:'同行业账号', value: 1}],
                        }
                    },
                    // {
                    //     label: '分析时间', prop: 'analysisDate',
                    //     temp: 'DatePicker',
                    //     config: {
                    //         type:"daterange", 
                    //         valueFormat:"yyyy-MM-dd",
                    //         style: {
                    //             width: '170px'
                    //         }
                    //     }
                    // }
                ],
            },
            // 
            column: [
                {
                    label: '文件名（双击修改）',
                    prop: 'anchorName',
                    option: {
                        minWidth: 180
                    },
                },
                // {
                //     label: '视频文件',
                //     prop: 'videoName',
                //     hidden:()=>{
                //         return this.type === 'slice'|| this.isIframe
                //     }
                // },
                {
                    label: '核心数据',
                    prop: 'viewersNum',
                    option: {
                        minWidth: 100
                    },
                    hidden:()=>{
                        return this.type === 'slice'
                    }
                },
                {
                    label: '话术质检',
                    prop: 'speechQc',
                    hidden:()=>{
                        return this.type === 'slice' || this.versionTypeIsPure
                    },
                    option: {
                        width: '135px',
                    }
                },
                {
                    label: '话术还原度',
                    prop: 'scriptRestore',
                    hidden:()=>{
                        return this.type === 'slice' || this.versionTypeIsPure
                    },
                    option: {
                        width: '135px',
                    },
                },
                {
                    label: '互动巡检',
                    prop: 'interactionInspect',
                    hidden:()=>{
                        return true
                    },
                    option: {
                        width: '150px',
                    },
                },
                {
                    label: '录制时间/文件大小',
                    prop: 'startTime',
                    option: {
                        minWidth: 120
                    },
                },
                {
                    label: '文件大小',
                    prop: 'vedioSizie',
                    option: {
                        suffix: 'M'
                    },
                    hidden:()=>{
                        return true
                    }
                },
                {
                    label: '小结',
                    prop: 'summary',
                    hidden:()=>{
                        return true
                    },
                    formatter: (row) => {
                        return '查看';
                    },
                    option: {
                        classNameFn: (row)=>{
                            return  row.notesSummary?'color-main cursor-pointer': 'text-color3';
                        },
                        on: {
                            click: (e, row) => {
                                if(row.notesSummary){
                                    this.onClickSummary(row);
                                }
                            }
                        }
                    }
                },
                {
                    label: 'AI诊断报告',
                    prop: 'aIReport',
                    hidden: ()=>{
                        return true
                    },
                    option: this.isIframe?{}:{
                        width: '120px'
                    }
                },
                {
                    label: '文件备注',
                    prop: 'fileRemark',
                    hidden:()=>{
                        return true
                    },
                    formatter: (row) => {
                        return '查看';
                    },
                    option: {
                        classNameFn: (row) => {
                            return row.cloudRemarks ? 'color-main cursor-pointer' : 'text-color3';
                        },
                        on: {
                            click: (e, row) => {
                                if (row.cloudRemarks) {
                                    this.onClickFileRemark(row);
                                }
                            }
                        }
                    }
                },
                {
                    label: '上传账号',
                    prop: 'userNickName'
                }
            ],
            // 
            menuConfig: {
                width: '205px',
                options: [
                    [{
                        label: "查看分析",
                        click: (item) => {
                            this.toInfo(item)
                        },
                    }, {
                        label: "复制分享链接",
                        hidden: (item) => {
                            return item.uploadStatus !== 1
                        },
                        click: (item) => {
                            if (item.uploadStatus === 1) {
                                let shareUrl = item.shareUrl
                                if (!item.shareUrl) {
                                    shareUrl = commonUtils.assemblyShareUrl(`onlineAnalysis/0/${item.videoId}`)
                                }
                                this.copyShareAnalysisLink(shareUrl)
                            }
                        }
                    }],
                    [{
                        label: "加对比",
                        type: 'text',
                        click: (item) => {
                            this.addContrast(item)
                            this.getTableHeight()
                        },
                        hidden: () => {
                            return this.isIframe;
                        },
                        disabled: (item) => {
                            return this.contrastList.some(_item => _item.videoId === item.videoId)
                        },
                    },{
                        label: (item) => {
                            if (item?.hasStar === 0) {
                                const unStartImg = require('@/assets/imgs/unStart.png')
                                return `<img src="${unStartImg}" style="width: 25px;height: 25px" alt="">`
                            } else {
                                const startImg = require('@/assets/imgs/start.png')
                                return `<img src="${startImg}" style="width: 25px;height: 25px" alt="">`
                            }
                        },
                        dangerouslyUseHTMLString:true,
                        type: 'text',
                        click: (item) => {
                            this.changeStart(item)
                        },
                        hidden: () => {
                            return this.isIframe;
                        },
                        disabled: (item) => {
                            if (this.isIframe) return true
                            if (item.hasStar) {
                                const {userType, id: currentUserId} = this.$store?.state?.userInfo;
                                if (userType === 0) {
                                    return false
                                } else {
                                    return item?.sourceStarInfo?.userId !== currentUserId
                                }
                            }else {
                                return false
                            }
                        },
                    },{
                        label: "删除",
                        type: 'danger',
                        hidden: (item) => {
                            if (this.isIframe) return true
                            const {userType, id: currentUserId} = this.$store?.state?.userInfo;
                            if (userType === 0) {
                                return false
                            } else {
                                return item?.userId !== currentUserId
                            }
                        },
                        disabled: (item) => {
                            return item.hasStar === 1
                        },
                        click: (item) => {
                            this.deleteShare(item.videoId)
                        }
                    }]
                ]
            },
            // contrastMap: {},
            tabs: [],
            // contrastList: [], // 对比列表
            fileList: [], // 视频列表
            // compereList: [],
            fileDataForm: {},
            tableHeight: `calc(100vh - 210px)`
        };
    },
    watch: {
        aiMonitorReportVisible(val) {
            if (val) return
            this.aiMonitorReportId = null
            this.aiMonitorReportRow = null
            this.aiMonitorReportType = ''
        }
    },
    created() {
        this.getTableHeight();
    },
    mounted() {
        window.addEventListener('resize', this.getTableHeight);
        this.initVideoList();
        this.initTabs();
    },
    methods: {
        getDefaultAiMonitorState() {
            return {
                qcReportStatus: 0,
                restoreReportStatus: 0,
                inspectReportStatus: 0,
                qcReportId: null,
                restoreReportId: null,
                inspectReportId: null,
                qcPreview: '',
                restorePreview: '',
                inspectPreview: '',
                restoreScore: null
            }
        },
        getAiMonitorState(row) {
            const id = row?.videoId
            if (!id) {
                return this.getDefaultAiMonitorState()
            }
            if (!this.aiMonitorStateMap[id]) {
                this.$set(this.aiMonitorStateMap, id, this.getDefaultAiMonitorState())
            }
            return this.aiMonitorStateMap[id]
        },
        async loadAiMonitorStatus(list = []) {
            const rows = Array.isArray(list) ? list.filter(r => !!r?.videoId) : []
            if (!rows.length) return
            if (!this.$httpBack?.scriptMonitor?.batchReportStatus && !this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return
            const processRow = (row, data) => {
                const state = this.getAiMonitorState(row)
                const qc = data?.scriptQualityInspection || {}
                const restore = data?.scriptFidelityMonitor || {}
                const inspect = data?.interactionPatrol || {}
                state.qcReportStatus = Number(qc?.status ?? 0)
                state.qcReportId = qc.reportId || null
                state.qcPreview = (qc?.summary ?? '') || ''
                state.restoreReportStatus = Number(restore?.status ?? 0)
                state.restoreReportId = restore.reportId || null
                const restoreSummary = restore?.summary
                state.restorePreview = typeof restoreSummary === 'string'
                    ? restoreSummary
                    : String(restoreSummary?.summaryText || restoreSummary?.summary || '')
                const restoreScore =
                    (typeof restoreSummary === 'object' && restoreSummary !== null ? restoreSummary?.score : null)
                    ?? restore?.score
                    ?? null
                state.restoreScore = restoreScore === null || restoreScore === undefined ? null : Number(restoreScore)
                state.inspectReportStatus = Number(inspect?.status ?? 0)
                state.inspectReportId = inspect.reportId || null
                const inspectSummary = inspect?.summary
                state.inspectPreview = typeof inspectSummary === 'string'
                    ? inspectSummary
                    : String(inspectSummary?.summaryText || inspectSummary?.summary || '')
            }
            if (this.$httpBack?.scriptMonitor?.batchReportStatus) {
                try {
                    const sources = rows.map((row) => ({
                        sourceType: 0,
                        sceneType: 0,
                        sourceId: String(row.videoId),
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
            const tasks = rows.map(async (row) => {
                try {
                    const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus({ sourceType: 0, sceneType: 0, sourceId: String(row.videoId), secUid: String(row?.secUid || row?.anchorInfo?.secUid || '') })
                    if (res?.code !== 0) return
                    const monitors = Array.isArray(res?.data?.monitors) ? res.data.monitors : []
                    const qc = monitors.find(a => a.monitorType === 0) || {}
                    const restore = monitors.find(a => a.monitorType === 1) || {}
                    const inspect = monitors.find(a => a.monitorType === 2) || {}
                    processRow(row, {
                        scriptQualityInspection: qc,
                        scriptFidelityMonitor: restore,
                        interactionPatrol: inspect
                    })
                } catch (e) {}
            })
            await Promise.all(tasks)
        },
        formatPercent(val, emptyText = '--') {
            if (val === null || val === undefined || val === '') return emptyText
            const num = Number(val)
            if (!Number.isFinite(num)) return emptyText
            return `${num}%`
        },
        openAiReport(type, row) {
            if (this.versionTypeIsPure) return
            const state = this.getAiMonitorState(row)
            if (type === 'qc') {
                if (!state.qcReportId) return
                this.aiMonitorReportId = state.qcReportId
            } else if (type === 'restore') {
                if (!state.restoreReportId) return
                this.aiMonitorReportId = state.restoreReportId
            } else if (type === 'inspect') {
                if (!state.inspectReportId) return
                this.aiMonitorReportId = state.inspectReportId
            } else {
                return
            }
            this.aiMonitorReportType = type
            this.aiMonitorReportRow = row
            this.aiMonitorReportVisible = true
        },
        async initTabs() {
            if (this.type === 'file') {
                this.tabs = [
                    {
                        label: '文件复盘',
                        name: 'b'
                    }
                ]
            } else if (this.type === 'slice') {
                this.tabs = [
                    {
                        label: '切片复盘',
                        name: 'c'
                    }
                ]
                await this.getDictDataListByCode(0)
            } else {
                this.tabs = [
                    {
                        label: '整场复盘',
                        name: 'a'
                    }
                ]
            }
            this.setTabs(this.tabs);
        },
        async changeStart(item) {
            try {
                const httpServer = item?.hasStar === 1 ? this.$httpBack.v2500.cancelStar : this.$httpBack.v2500.addStar
                const result = await httpServer({
                    sourceId: item.videoId,
                    sourceType: 0
                })
                if (result.code === 0) {
                    this.getList()
                    return this.$message.success(`${item.hasStar === 1 ? '取消' : '添加'}星标成功`)
                }
            } catch (e) {
            }
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
        toOfficialWebsite(){
            toOfficialWebsite(this.$httpClient);
        },
        contrastSubmitHttp(param){
            return this.$httpBack.contrast.clientAddCloudContrast(param);
        },
        contrastSubmitCallback(){
            this.$router.push({
                path: '/online/onlineContrast',
                // query:{type: this.type==='video'?'replayAll':'replaySection'}
            })
        },
        getTableHeight() {
            const isContrast = !!this.contrastList.length
            if(this.type === 'slice'){
                if (window.innerWidth > 1568) {
                    return this.tableHeight = `calc(100vh - ${isContrast ? '260px' : '208px'})`
                } else {
                    return this.tableHeight = `calc(100vh - ${isContrast ? '305px' : '253px'})`
                }
            }
            if(this.type === 'video'){
                if (window.innerWidth > 1886) {
                    return this.tableHeight = `calc(100vh - ${isContrast ? '260px' : '208px'})`
                } else {
                    return this.tableHeight = `calc(100vh - ${isContrast ? '305px' : '253px'})`
                }
            }
        },
        toInfo(item){
            if(this.isIframe){
                window.open(commonUtils.copyShareUrl(item.shareUrl), "_blank");
                return
            }
            this.lockAnalysis(item.videoId)
        },
        async initVideoList(){
            let anchor = await this.listByTenantAnchor();
            this.setFormConfigDic({0: anchor}, this.formConfig, {
                tradeId: (opts, form) => {
                    return [].concat(form.config?.options?.shift(), opts);
                }
            })

        },
        // 查看分析结果
        lockAnalysis(id) {
            this.$router.push({
                path: this.$route.path + '/analysis',
                query: {
                    id: id
                }
            })
        },
        // 删除分析
        deleteShare(videoId) {
            this.$confirm('将删除分享复盘并将视频从云空间清除，相关的在线对比复盘也将一并删除，是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.$httpBack.video.clientDeleteCloudVideo({videoId}).then(res => {
                    if (res.code == 0) {
                        this.$message.success("删除成功");
                        this.$refs.table.getList();
                        // this.$emit("updateProperty");
                        this.getUserProperty()
                    }
                })
            });
        },
        deleteOpt (list = []) {
            const rows = Array.isArray(list) ? list : []
            const { userType, id: currentUserId } = this.$store?.state?.userInfo || {}
            const canDelete = (item) => {
                if (!item?.videoId) return false
                if (item.hasStar === 1) return false
                if (userType === 0) return true
                return item?.userId === currentUserId
            }
            const deleteList = rows.filter(canDelete)
            const ids = deleteList.map(d => d?.videoId).filter(Boolean)
            if (Array.isArray(list)) {
                list.splice(0, list.length, ...deleteList)
            }
            if (!ids.length) {
                this.$message.warning('暂无可删除的数据（星标/权限限制）')
                return {
                    http: null
                }
            }
            return {
                title: `将批量删除 ${ids.length} 条云空间复盘记录，是否继续？`,
                http: (deleteIds) => this.$httpBack.video.clientBatchDeleteCloudVideos(deleteIds, { load: false }),
                idKey: 'videoId',
                callback: () => {
                    this.getUserProperty()
                }
            }
        },
        getUserProperty(){
            this.appVnode?.getUserproperty?.();
        },
        // 复制分享链接
        copyShareAnalysisLink(shareUrl) {
            commonUtils.copyShareUrl(shareUrl, this.$message.success)
        },
        // 预览
        preview(videoId) {
            this.$httpClient.video.preview({videoId}).then((res) => {
                if (res.code == 0) {
                    window.open(res.data, "_blank");
                }
            });
        },
        retainDecimals(val) {
            return myUtils.retainDecimals(val);
        },

        // 获取主播视频列表
        getVideoList(param) {
            let fileDataForm = {
                page: param.pageIndex,
                limit: param.pageSize,
                recordStartDate: param.recordDate?.[0] || '',
                recordEndDate: param.recordDate?.[1] || '',
                analysisStartDate: param.analysisDate?.[0] || '',
                analysisEndDate: param.analysisDate?.[1] || '',
                ...this.videoSliceType,
                ...param,
            };

            // const name = fileDataForm?.secUidName;
            // const secUidArrConfigOpt = this.formConfig?.items?.find(d=>d.prop==='secUidName')?.config?.options || [];
            // const ls = secUidArrConfigOpt?.filter(d=>{
            //     return d.anchorName.indexOf(name) >= 0
            // }).map(d=>d.secUid);
            // fileDataForm.secUidArr =name ? ls?.length ? ls : [999] : '';

            return this.$httpBack.video.clientListCloudVideo(fileDataForm, {load: false}).then((res) => {
                if (res.code == 0) {
                    this.totalCount = res.data.totalCount;
                    let fileList = res.data.list || [];
                    fileList?.forEach(item => {
                        item.duration = myUtils.toformatTime(item.duration * 1000);
                        item.vedioSizie = myUtils.retainDecimals(item.vedioSizie / 1024 / 1024);
                        // 计算时长
                        let second = myUtils.toSecondByDate(item.endTime) - myUtils.toSecondByDate(item.startTime);
                        item.durationStr = myUtils.toformatTimeChinse(second * 1000);
                        item.reportFileName = item.videoName.replace(/_[^_]+\.ts$/, '')
                        item.videoName = item.videoName.replaceAll("AF_", "");
                        item.videoName = item.videoName.replaceAll(".ts", "");
                        item.videoName = item.videoName.substring(item.videoName.indexOf("_") + 1);
                    })
                    this.fileList = fileList;
                    this.$nextTick(() => {
                        this.loadAiMonitorStatus(fileList)
                    })
                }
                return res;
            })
        },
        reportEd(){
            console.log(22222222)
        },
        async viewAiReport(command, item) {
            if (this.$isWeb) {
                const {data: result, code} = await this.$httpBack.v2500.getDiagnosisDownloadUrl({
                    videoId: item.videoId,
                    sourceType:0,
                    uploadType: command === 'content' ? 1 : 2
                })
                if (code !== 0) return this.$message.error(result.msg)
                if (this.$isWeb) {
                    const a = document.createElement('a');
                    a.href = result;
                    a.download = `${item.reportFileName}.pdf`;
                    a.target = '_blank';
                    a.click();
                }
            } else {
                const {data: result, code} = await this.$httpClient.export.generateReport({
                    videoId: item.videoId,
                    fileName: item.diagnosisOssName,
                    uploadType: item.hasDiagnosisReport === 1?1:2
                })
                if (code !== 0) return this.$message.error(result.msg)
            }
        },
        toPage(url) {
            this.$emit("toPage", url);
        },
        toMenu(menu) {
            this.$emit("toMenu", menu);
        },
        getList() {
            this.$refs.table.getList()
        }
    },
    activated() {

    },
    beforeDestroy() {
        window.removeEventListener('resize', this.getTableHeight);
    }, //生命周期 - 销毁之前
};
</script>

<style scoped lang="less">

.fileSizeColContainer {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}

.file-size-text {
    color: #7A7C80;
}

.analysisDateColContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 0.8;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}

.compereNameText {
    font-weight: 400;
    font-size: 13px;
    color: #677583;
    margin-top: 4px;

}

.videoNameText {
    font-weight: 400;
    font-size: 14px;
    color: #2E3742;
}

.fileNameContainer {
    display: flex;
    flex-direction: column;
    text-align: left;
    // white-space: pre-wrap;
}
.contrast-box {
    padding: 10px;
    margin: 0;

    > * {
        margin: 0 5px;
    }
}

.aiMonitorPreview{
    cursor: pointer;
}
</style>

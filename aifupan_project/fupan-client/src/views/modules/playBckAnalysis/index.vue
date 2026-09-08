<template>
    <div>
        <core-table ref="coreTable" :searchConfig="formConfig" :menuConfig="computeMenuConfig" :getDataApi="getFileList"
                    :column="column" :table-select="true" @deletes="deletes"
                    @api-succees="onFileListSuccess"
                    :table-height="`calc(100vh - ${getContrast.length ? 265 : 212}px)`">
            <template #searchRight>
                <div>
                    <afp-button v-if="isText && versionType === VERSION_TYPE.AGENT" size="default" type="primary" @click="copyText" class="uploadFileBtn">
                        粘贴文本
                    </afp-button>
                    <afp-button v-if="isText" size="default" type="primary" @click="selectUploadFile(1)"
                                class="uploadFileBtn">上传文本
                    </afp-button>
                    <afp-button v-if="isVideo" size="default" type="primary" @click="selectUploadFile(0)"
                                class="uploadFileBtn">上传视频
                    </afp-button>
                </div>
            </template>
            <template #tableTop>
                <div v-if="getContrast.length" class="contrast-box main-bg">
                    <el-tag v-for="item in getContrast" closable @close="cancelContrast(item)" class="brs-40">
                        {{ item.fileName }}
                    </el-tag>
                    <afp-button v-show="getContrast.length>=2" size="default" type="primary" @click="modalContrast">
                        开始对比
                    </afp-button>
                </div>
            </template>
            <template #analysisStatus="{ row }">
                <div class="fileSizeColContainer">
                    <div>{{ ['未分析', '分析中', '分析完成', '分析失败', '', '切片中'][row.analysisStatus] }}</div>
                    <div style="color: red;" v-if="row.errorReason">{{ '(' + row.errorReason + ')' }}</div>
                </div>
            </template>
            <template #fileName="{ row }">
                <div class="fileColContainer">
                    <img src="@/assets/imgs/txt.png" class="videoImg" v-if="row.fileType == 2">
                    <img src="@/assets/imgs/video.png" class="videoImg" v-else>
                    <div class="fileNameContainer" @dblclick="() => editFileName(row,getTableList)">
                        <div class="videoNameText cursor-pointer">{{ row.fileName }}</div>
                    </div>
                </div>
            </template>
            <template #speechQc="{ row }">
                <div class="aiMonitorCell">
                    <template v-if="getAiMonitorState(row).qcReportStatus === 2">
                        <el-badge :is-dot="getAiMonitorState(row).qcUnread" class="aiMonitorBadge">
                            <AiMonitorPreviewContent
                                :content="getAiMonitorState(row).qcPreview"
                                preview-type="qc"
                                parse-mode="structured"
                                @view="openAiReport(row)"
                            />
                        </el-badge>
                    </template>
                    <template v-else-if="Number(row.analysisStatus) !== 2">
                        <span class="aiMonitorLinkDisabled">-</span>
                    </template>
                    <template v-else-if="getAiMonitorState(row).qcReportStatus === 1">
                        <span class="aiMonitorLinkDisabled">生成中</span>
                    </template>
                    <template v-else-if="getAiMonitorState(row).qcReportStatus === 4">
                        <el-button type="text" class="aiMonitorLink" :disabled="true">{{ getAiMonitorState(row).qcUnavailableReason || '不可生成' }}</el-button>
                    </template>
                    <template v-else-if="getAiMonitorState(row).qcReportStatus === 3">
                        <el-button type="text" class="aiMonitorLink" @click="generateAiReport(row)">重试生成</el-button>
                    </template>
                    <template v-else>
                        <el-button type="text" class="aiMonitorLink" @click="generateAiReport(row)">生成报告</el-button>
                    </template>
                </div>
            </template>
            <!--
            <template #scriptRestore="{ row, $index }">
                <ScriptRestoreCell :row="row" scene="upload" :mock-index="$index" />
            </template>
            -->

            <template #empty>
                <div class="emptyContainer text-left">
                    <div class="emptyTipText">
                        文件分析，可以分析直播录屏和短视频文件，
                        可以使用<span class="danger-color">AI运营助手</span>、<span
                        class="danger-color">AI违规助手</span>、<span class="danger-color">AI话术助手</span>等相关AI功能
                    </div>
                    <div class="emptyTipText" style="margin-top: 10px;">
                        <span>支持平台: <b>抖音、视频号、小红书、快手、B站</b>等平台<br>  </span>
                        <span v-if="isVideo">支持文件: 视频、音频<b>文案提取并智能分析</b><br></br></span>
                        <span v-if="isText">支持文件: txt文本文件<b>智能分析</b></span>
                    </div>
                    <img style="max-width: 267px;margin-top: 30px;" src="@/assets/imgs/1_9_30/bqEmpty.png" alt=""
                         srcset="">
                </div>
            </template>
        </core-table>

        <!-- 行业弹窗 -->
        <TardeDialog ref="tardeDialog" :treeList="tradeTreeList" @confirm="createAnalysisConfirm"></TardeDialog>

        <Modal ref="play_modal" @uploadFile="uploadFile" @getTableList="getTableList"/>
        <!-- emitPath: false -->
        <!-- 播前分析原来页面参考 -->
        <!-- <file-upload></file-upload> -->
        <MarkDialog ref="markDialog" @cancal="cancalMark" @useMark="confirmUseMark"
                    @qrCode="showCustomerServiceQrCode"></MarkDialog>

        <AiDataReport ref="ai_data_report" :reportType="reportType" @uploadFile="uploadFile"/>

        <exportDialog ref="export_dialog"/>

        <ReviewContrast ref="review_contrast" @contrastSubmit="contrastSubmit" @swapObj="swapObj"/>

        <AiMonitorReportDialog
            :visible.sync="aiMonitorReportVisible"
            type="qc"
            :row="aiMonitorReportRow"
            :reportId="aiMonitorReportId"
            @report-read="handleAiMonitorReportRead"
        />
    </div>
</template>

<script>
import CoreTable from '/src/components/coreTable/index.vue'
import FileUpload from '../replay/fileUpload/index.vue'
import myUtils from '@/utils/utils'
import TardeDialog from '@/components/tardeDialog/index.vue'
import MarkDialog from '@/views/commonComponent/markDialog.vue'
import Modal from './modal.vue'
import viewVideo from '@/mixins/viewVideo'
import AiDataReport from '@/views/commonComponent/aiReport/aiDataReport.vue';
import UploadContent from "@/views/modules/playBckAnalysis/uploadContent.vue";
import ReviewContrast from '@/components/reviewContrast'
import {cloneDeep} from "lodash";
import exportDialog from "@/components/DiscernSearchContainer/exportDialog.vue";
import {VERSION_TYPE} from "@/enum";
import AiMonitorReportDialog from '@/components/aiMonitor/reportDialog/index.vue'
import AiMonitorPreviewContent from '@/components/aiMonitor/previewContent/index.vue'
// import ScriptRestoreCell from '@/components/aiMonitor/scriptRestoreCell/index.vue'

export default {
    mixins: [viewVideo],
    components: {
        exportDialog,
        UploadContent,
        CoreTable,
        FileUpload,
        TardeDialog,
        MarkDialog,
        Modal,
        AiDataReport,
        ReviewContrast,
        AiMonitorReportDialog,
        AiMonitorPreviewContent,
        // ScriptRestoreCell
    },
    inject: ['appVnode'],
    props: {
        type: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            VERSION_TYPE,
            aiMonitorStateMap: {},
            aiMonitorReportVisible: false,
            aiMonitorReportId: null,
            aiMonitorReportRow: null,
            formConfig: {
                items: [{label: '文件名', prop: 'fileName'}]
            },
            column: [
                {
                    label: '文件名称',
                    prop: 'fileName'
                },
                {
                    label: '文件大小',
                    prop: 'fileSize',
                    option: {
                        numberText: true,
                    },
                    formatter(row) {
                        return myUtils.retainDecimals(row.fileSize / 1024 / 1024) + 'M'
                    }
                },
                {
                    label: '视频时长',
                    prop: 'fileDuration',
                    hidden: () => {
                        return this.type === 'text'
                    },
                    formatter(row) {
                        if (!row.fileDuration) return '-'
                        return myUtils.toformatTimeChinse(row.fileDuration * 1000)
                    }
                },
                {
                    label: '话术',
                    prop: 'script',
                    hidden: () => {
                        return !['slice', 'video'].includes(this.type) || this.versionTypeIsPure
                    },
                    formatter: (row) => row.analysisStatus===2?'导出':'-',
                    option: {
                        width: '50px',
                        className: 'export_script',
                        on: {
                            click: (e, row) => {
                                this.exportScript(row)
                            }
                        }
                    }
                },
                /*
                {
                    label: '话术还原度',
                    prop: 'scriptRestore',
                    hidden: () => {
                        return this.versionType === VERSION_TYPE.PURE
                    },
                    option: {
                        width: '150px',
                    }
                },
                */
                {
                    label: '话术质检',
                    prop: 'speechQc',
                    hidden: () => {
                        return this.versionTypeIsPure
                    },
                    option: {
                        width: '105px',
                    }
                },
                {
                    label: '上传时间',
                    prop: 'uploadTime',
                },
                {
                    label: '分析时间',
                    prop: 'analysisTime',
                },
                {
                    label: '分析状态',
                    prop: 'analysisStatus'
                }
            ],
            menuConfig: {
                width: '255px',
                options: [
                    {
                        label: '生成分析',
                        show(row) {
                            return row.analysisStatus == 0
                        },
                        click: (item) => {
                            this.createAnalysis(item.fileId, item)
                        }
                    },
                    {
                        label: '查看分析',
                        show(row) {
                            return row.analysisStatus == 2
                        },
                        click: (item) => {
                            this.lockAnalysis(item.fileId)
                        }
                    },
                    {
                        label: '导出文案',
                        show: (row) => {
                            return row.analysisStatus == 2 && this.versionType === VERSION_TYPE.PURE
                        },
                        click: (item) => {
                          this.exportScript(item)
                        }
                    },
                    {
                        label: '重新分析',
                        type: 'danger',
                        show(row) {
                            return row.analysisStatus == 3
                        },
                        click: (row) => {
                            this.reAnalysis(row.fileId, row)
                        }
                    },
                    {
                        label: '切片中',
                        type: 'warning',
                        show(row) {
                            return row.analysisStatus == 5
                        },
                    },
                    {
                        label: '加入对比',
                        show: (row) => {
                            return !this.contrast[row.fileId] && row.analysisStatus === 2 && !this.isShort && this.versionType === VERSION_TYPE.AGENT
                        },
                        click: (row) => {
                            this.addContrast(row)
                        }
                    },

                    {
                        label: '取消对比',
                        type: 'danger',
                        show: (row) => {
                            return this.contrast[row.fileId] && row.analysisStatus === 2 && !this.isShort
                        },
                        click: (row) => {
                            this.cancelContrast(row)
                        }
                    },
                    [
                        {
                            label: '查看',
                            icon: 'icon-a-bukechakan2',
                            show(row) {
                                return row.analysisStatus == 2
                            },
                            click: (row) => {
                                this.preview(row.fileId)
                            }
                        },
                        {
                            label: '基础设置',
                            icon: 'icon-a-bukechakan2',
                            show: (row) => {
                                return ['video'].includes(this.type) && row.analysisStatus == 2
                            },
                            click: (row) => {
                                this.reportType = 'fileVideo'
                                this.$refs.ai_data_report.changeDrawerStatus(true,row)
                            }
                        },
                        {
                            label: '修改文件名',
                            icon: 'icon-a-bukechakan2',
                            show: (row) => {
                                return ['video'].includes(this.type) && row.analysisStatus == 2
                            },
                            click: (row) => {
                                this.editFileName(row,this.getTableList)
                            }
                        }
                    ]
                ]
            },
            tradeTreeList: [],
            fileList: [],
            tradeId: '',
            contrast: {},
            timeOut: null,
            reportType:'fileVideo'
        }
    },
    computed: {
        getContrast() {
            return Object.values(this.contrast) || []
        },
        isText() {
            return this.type === 'text'
        },
        isVideo() {
            return this.type === 'video'
        },
        getFileTypeArr() {
            if (!this.isText && !this.isVideo) return []
            return this.isVideo ? [0, 1] : [2]
        },
        getFileSliceType() {
            switch (this.type) {
                case 'text':
                    return 0
                case 'video':
                    return 0
                case 'slice':
                    return 1
                case 'short':
                    return 2
            }
        },
        isShort() {
            return this.type === 'short'
        },
        computeMenuConfig(){
            if (this.versionTypeIsPure) {
                const menuConfig = cloneDeep(this.menuConfig)
                menuConfig.options[this.menuConfig.options.length - 1] = []
                return menuConfig
            }
            return this.menuConfig
        },
        versionType() {
            return this.$store.getters.getVersionType
        },
        versionTypeIsPure() {
            return this.$store.getters.getVersionType === VERSION_TYPE.PURE || this.$store.getters.isPure
        },
    },
    watch: {
        aiMonitorReportVisible(val) {
            if (val) return
            this.aiMonitorReportId = null
            this.aiMonitorReportRow = null
        }
    },
    methods: {
        // 2025/9/5日加入，粘贴文本功能
        copyText() {
            this.$refs.play_modal.changeDialogVisible(true, 1, {isCopy: true})
        },
        getAiMonitorSceneType() {
            return this.type === 'text' ? 2 : 1
        },
        getTableList() {
            this.$refs.coreTable.getList()
        },
        isAnalysisStatus() {
            // 重新获取数据
            clearTimeout(this.timeOut)
            this.timeOut = setTimeout(() => {
                let list = this.$refs?.coreTable?.tableData || []
                if (!(list?.length)) return
                // 如果有分析中的数据则5s后再次检查数据
                if (list.some(d => d.analysisStatus === 1)) {
                    this.$refs?.coreTable.getList('notClearSelection')
                }
            }, 5000)
        },
        createAnalysisConfirm({id, data, callback}) {
            let token = this.$store.state.token
            // 储存数据
            this.tradeId = id || '1'
            localStorage.setItem('saveAnalysisTradeFile', id)

            if (data.isReAnalysis) {
                // 重新分析
                this.$httpClient.uploadFile.reanalysis({
                    fileId: data.fileId,
                    platformType: 1,
                    token,
                    tradeId: this.tradeId
                }).then((res) => {
                    if (res.code == 0 && res.data) {
                        callback()
                        this.$refs.coreTable.getList()
                        // this.isAnalysisStatus();
                    } else {
                        this.$message.warning('请等待上一个分析完再继续')
                    }
                })
            } else {
                // 创建分析
                this.$httpClient.uploadFile.createAnalysis({
                    fileId: data.fileId,
                    token,
                    tradeId: this.tradeId
                }).then((res) => {
                    if (res.code == 0 && res.data) {
                        callback()
                        this.$refs.coreTable.getList()
                        // this.isAnalysisStatus();
                    } else {
                        this.$message.warning('请等待上一个分析完再继续')
                    }
                })
            }
        },
        // 提交对比
        contrastSubmit() {
            let cMap = Object.keys(this.contrast)
            if (!cMap || cMap.length != 2) {
                this.$message.error('请选择两个进行对比')
                return
            }
            let requestData = {
                fileOneId: cMap[0],
                fileTwoId: cMap[1],
                contrastType: 1,
                sliceContrastType: this.getFileSliceType,
            }
            this.$httpBack.contrast.clientAddContrast(requestData).then(res => {
                if (res.code === 0) {
                    this.contrast = {}
                    this.$nextTick(() => {
                        this.clearPathBufferData('/contrastReplay')
                        this.clearPathBufferData('/contrastSection')
                        this.$router.push({
                            path: ['text', 'video'].includes(this.type) ? '/contrastReplay' : '/contrastSection',
                        })
                    })
                    // // 设置直播复盘页的菜单和tabs
                    // this.$store.commit("saveReplayPageMenu", { page: 'tabs', menu: 'compare' })
                    // // 跳转到直播复盘页
                    // this.$emit("updateMenuIndex", 2);
                    // this.toMenu("compare");
                }
            })

        },
        showCustomerServiceQrCode() {
            this.appVnode?.showQrCode()
        },
        cancalMark() {
            this.$refs.markDialog?.hide()
        },
        confirmUseMark(data) {
            this.$refs.markDialog?.hide()
            const {fileId} = data
            this.showTardeDialog(fileId)
        },
        showTardeDialog(fileId) {
            // 设置默认行业
            this.tradeId = localStorage.getItem('saveAnalysisTradeFile') || '1'
            this.$refs.tardeDialog.show({
                data: {
                    isReAnalysis: true,
                    fileId: fileId
                },
                id: this.tradeId
            })
        },
        ifAnchorvideo(fileId, row) {
            if (row.fileType === 2) {
                this.$httpClient.anchorvideo.anchorvideoGetpage({fileId}).then(res => {
                    if (res.code === 0) {
                        const {data} = res
                        if (data?.fileWordNum) {
                            this.$refs?.markDialog.show({
                                data: {
                                    sufficient: data.isSufficient,
                                    wordNum: data.fileWordNum,
                                    propertyWordNum: data.propertyWordNum,
                                    fileId: fileId
                                }
                            })
                        } else {
                            this.$message.error('该文件内容为空，不能进行分析')
                        }
                    }
                }).catch(err => {
                })
            } else {
                this.showTardeDialog(fileId)
            }
        },
        // 重新生成分析
        async reAnalysis(fileId, row) {
            await this.ifAnchorvideo(fileId, row)
        },
        // 生成直播分析弹窗
        async createAnalysis(fileId, row) {
            await this.ifAnchorvideo(fileId, row)
        },
        // 查看分析
        lockAnalysis(id) {
            if (this.versionType === VERSION_TYPE.PURE && this.isText) {
                this.pureTips()
                return
            }
            const {path} = this.$route
            this.$router.push({
                path: path + '/fileUploadAnalysis',
                query: {
                    id: id,
                    sceneType: this.getAiMonitorSceneType()
                }
            })
        },
        // 获取行业列表树形
        getTradeTreeList() {
            this.tradeTreeList = []
            this.$httpBack.trade.listTree({}, {load: false}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
            })
        },
        // 预览
        preview(fileId) {
            this.$httpClient.uploadFile.preview({fileId}).then(res => {
                if (res.code == 0 && res.data) {
                    window.open(res.data, '_blank')
                }
            })
        },
        // 取消对比
        cancelContrast(data) {
            if (this.contrast[data.fileId]) {
                this.$delete(this.contrast, data.fileId)
            }
        },
        // 加入对比
        addContrast(data) {
            if (Object.keys(this.contrast)?.length >= 2) {
                this.$message.error('仅支持两个对比')
                return
            }
            this.$set(this.contrast, data.fileId, data)

            this.$nextTick(() => {
                if (Object.keys(this.contrast)?.length === 2) {
                    const list = Object.values(this.contrast)
                    this.$refs.review_contrast?.open?.(list,'file');
                }
            })
        },
        deletes({list, callback}) {
            // 删除文件
            this.$confirm('将永久删除选中的文件, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                let ids = []
                list.forEach(item => {
                    ids.push(item.fileId)
                })

                // this.$httpBack.fileAnalysis.clientDeleteFile(ids).then(async res => {
                //     if (res.code === 0) {
                //         this.$message.success('删除成功')
                //         await this.$httpClient.uploadFile.deletebyids(ids)
                //         callback() // 执行数据重置
                //         this.appVnode?.getDisk()
                //     } else {
                //         this.$message.warning('请等待分析结束后再删除')
                //     }
                // })

                this.$httpClient.uploadFile.deletebyids(ids).then(res => {
                    if (res.code === 0) {
                        this.$message.success('删除成功')
                        callback() // 执行数据重置
                    } else {
                        this.$message.warning('请等待分析结束后再删除')
                    }
                    this.appVnode?.getDisk()
                })
            })
        },
        pureTips(){
            this.$confirm(`
                <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                    <div>
                        <div>文案预审功能，可以排查直播间违禁词和违规语句。</div>
                        <div>您目前使用的是<span style="color:#444DFF;">纯录制版会员</span> ，无法使用该功能。</div>
                    </div>
                </div>`, '温馨提示', {
                showConfirmButton: false,
                showCancelButton: false,
                customClass: 'edit-file-name',
                showClose: true,
                closeOnClickModal: false,
                closeOnPressEscape: false,
                dangerouslyUseHTMLString: true,
                center: true
            }).then(async () => {
            }).catch(async () => {
            });
        },
        selectUploadFile(fileType) {
            this.reportType = 'addFileVideo'
            if (fileType === 0) {
                this.$refs.ai_data_report.changeDrawerStatus(true,{})
            } else {
                if (this.versionType === VERSION_TYPE.PURE) {
                    this.pureTips()
                } else {
                    this.$refs.play_modal.changeDialogVisible(true, fileType)
                }
            }
        },
        swapObj() {
            const obj = cloneDeep(this.contrast)
            this.contrast = Object.fromEntries(
                Object.entries(obj).reverse()
            );
        },
        //弹窗对比提示
        modalContrast(){
            const list = Object.values(this.contrast)
            this.$refs.review_contrast?.open?.(list,'file');
        },
        // 选择文件
        uploadFile(fileData,callback) {
            if (!fileData?.filePath) return this.$message.error('文件不能为空')
            localStorage.setItem('notCloseLoading', '1')
            const loading = this.$loading({
                lock: true,
                background: 'rgba(0, 0, 0, 0.7)'
            })
            this.$httpClient.uploadFile.commitUploadFile({...fileData}).then(res => {
                if (res.code === 0) {
                    this.$message.success('上传成功')
                    callback?.()
                    this.$refs.ai_data_report?.changeDrawerStatus?.(false, {})
                    this.$refs.coreTable?.getList?.()
                }
                localStorage.setItem('notCloseLoading', '')
                loading.close()
            }).catch(err => {
                localStorage.setItem('notCloseLoading', '')
                loading.close()
            })
        },
        // 获取文件列表
        getFileList(params) {
            this.fileDataForm = {
                page: params.pageIndex,
                limit: params.pageSize,
                analysisStatus: null,
                // fileType: this.isVideo ? 0 : this.isText? 2 : 1,
                fileName: params.fileName || '',
                uploadStartDate: '',
                uploadEndDate: '',
                analysisStartDate: '',
                analysisEndDate: '',
                fileTypeArr: this.getFileTypeArr,
                fileSliceType: this.getFileSliceType,
            }

            return this.$httpBack.fileAnalysis.clientFileList(myUtils.httpFormat(this.fileDataForm), {load: false}).then((res) => {
                return res
            })

            // return this.$httpClient.uploadFile.getpage(this.fileDataForm, { load: false }).then((res) => {
            //     if (res.code === 0) {
            //         this.$nextTick(() => {
            //             this.isAnalysisStatus()
            //         })
            //     }
            //     return res
            // })

        },
        onFileListSuccess(res) {
            const data = res?.data || {}
            const list =
                (Array.isArray(data?.list) ? data.list : null) ||
                (Array.isArray(data?.DataList) ? data.DataList : null) ||
                (Array.isArray(data?.dataList) ? data.dataList : null) ||
                []

            this.loadAiMonitorStatus(list)
            this.$nextTick(() => {
                if (this.$refs?.coreTable) this.isAnalysisStatus()
            })
        },
        exportScript(row){
            if (row.analysisStatus !== 2) return
            Promise.all([this.$httpBack.words.getVideoContent({
                sourceId: row.fileId,
                sourceType: 1,
                type: 1,
            }), this.$httpBack.words.getVideoContent({
                sourceId: row.fileId,
                sourceType: 1,
                type: 2,
            })]).then(results => {
                const [result1, result2] = results
                if (result1?.code !== 0 || result2?.code !== 0) return this.$message.error('获取视频文件内容失败')
                this.$refs.export_dialog?.show({
                    data: {
                        fileName: `${row.fileName}_`,
                        fileSize: Number(row.fileSize),
                        sourceId: row.fileId,
                        sourceType: 1,
                        isRecording: true,
                        isReplay:true,
                        //contentStatus: result.contentStatus,
                        aiShardingStatus: result1.data?.contentStatus,//AI脚本拆解
                        aiOptimalStatus: result2.data?.contentStatus,//优化原文
                        readonly: true,
                    }
                })
            })
        }
        ,
        getAiMonitorState(row) {
            const id = row?.fileId
            if (!id) return { qcReportStatus: 0, qcReportId: null, qcUnavailableReason: '', qcUnread: false, qcReadOverrideReportId: null, qcPendingUnread: false, qcPreview: '' }
            if (!this.aiMonitorStateMap[id]) {
                this.$set(this.aiMonitorStateMap, id, { qcReportStatus: 0, qcReportId: null, qcUnavailableReason: '', qcUnread: false, qcReadOverrideReportId: null, qcPendingUnread: false, qcPreview: '' })
            }
            return this.aiMonitorStateMap[id]
        },
        async loadAiMonitorStatus(list = []) {
            const rows = Array.isArray(list) ? list.filter(r => !!r?.fileId) : []
            if (!rows.length) return
            if (!this.$httpBack?.scriptMonitor?.batchReportStatus && !this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return

            const processRow = (row, data) => {
                const state = this.getAiMonitorState(row)
                const qc = data?.scriptQualityInspection || {}
                const status = Number(qc?.status ?? 0)
                state.qcReportStatus = status
                state.qcReportId = qc.reportId || null
                state.qcUnavailableReason = qc.unavailableReason || ''
                const summary =
                    typeof qc?.summary === 'string'
                        ? qc.summary.replace(/\n[ \t]+/g, '\n')
                        : (qc?.summary?.summary ?? qc?.summary?.text ?? qc?.summary?.content ?? '')
                state.qcPreview = summary || ''
                state.qcUnread = !!(state.qcReportId && Number(qc?.isRead) === 0)
            }

            if (this.$httpBack?.scriptMonitor?.batchReportStatus) {
                try {
                    const sceneType = this.getAiMonitorSceneType()
                    const sources = rows.map((row) => ({
                        sourceType: 1,
                        sceneType,
                        sourceId: String(row.fileId)
                    }))
                    const res = await this.$httpBack.scriptMonitor.batchReportStatus({ sources })
                    if (res?.code === 0) {
                        const items = Array.isArray(res?.data?.items) ? res.data.items : (Array.isArray(res?.data) ? res.data : [])
                        const map = new Map(items.map((it) => [String(it?.sourceId ?? ''), it]))
                        rows.forEach((row) => {
                            const data = map.get(String(row.fileId))
                            if (data) processRow(row, data)
                        })
                        return
                    }
                } catch (e) {}
            }

            const tasks = rows.map(async (row) => {
                try {
                    const sceneType = this.getAiMonitorSceneType()
                    const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus({ sourceType: 1, sceneType, sourceId: String(row.fileId) })
                    if (res?.code !== 0) return
                    const qc = (res?.data?.monitors || []).find(a => a.monitorType === 0) || {}
                    processRow(row, { scriptQualityInspection: qc })
                } catch (e) {}
            })
            await Promise.all(tasks)
        },
        async generateAiReport(row) {
            if (!row?.fileId) return
            if (Number(row.analysisStatus) !== 2) return
            if (!this.$httpBack?.scriptMonitor?.triggerScriptMonitorReport) return
            const state = this.getAiMonitorState(row)
            if (state.qcReportStatus === 1) return
            state.qcReportStatus = 1
            try {
                const sceneType = this.getAiMonitorSceneType()
                const res = await this.$httpBack.scriptMonitor.triggerScriptMonitorReport({
                    sourceType: 1,
                    sceneType,
                    sourceId: String(row.fileId),
                    monitorType: 0
                })
                setTimeout(() => {
                    this.loadAiMonitorStatus([row])
                }, 1400)
            } catch (e) {
                state.qcReportStatus = 3
            }
        },
        openAiReport(row) {
            const state = this.getAiMonitorState(row)
            if (!state.qcReportId) return
            this.aiMonitorReportId = state.qcReportId
            this.aiMonitorReportRow = row
            this.aiMonitorReportVisible = true
        },
        handleAiMonitorReportRead({ reportId }) {
            const row = this.aiMonitorReportRow
            if (!row) return
            const state = this.getAiMonitorState(row)
            if (String(state.qcReportId || '') === String(reportId || '')) {
                state.qcUnread = false
            }
        }
    },
    created() {

    },
    mounted() {
        this.getTradeTreeList()
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
</script>
<style lang='scss' scoped>
.fileColContainer {
    display: flex;
}

.fileNameContainer {
    display: flex;
    flex-direction: column;
    justify-content: center;
    margin-left: 12px;
}

.videoNameText {
    font-weight: 400;
    font-size: 14px;
    color: #2E3742;
}

.videoImg {
    width: 40px;
    height: 40px;
}

.contrast-box {
    padding: 10px;
    margin: 0;

    > * {
        margin: 0 5px;
    }
}

.aiMonitorCell{
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px 0;
    font-size: 12px;
    color: #2E3742;
}

.aiMonitorPreview{
    cursor: pointer;
    width: 100%;
    text-align: left;
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

.aiMonitorBadge ::v-deep .el-badge__content.is-fixed{
    top: 4px;
    right: -6px;
}
</style>

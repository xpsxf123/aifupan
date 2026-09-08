<template>
    <div class="windon-box" style="padding: 20px;">
        <!-- 搜索栏 -->
        <div v-if="currentPage == 'fileUploadList'">
            <div class="searchContainer">
                <div class="searchLeftContainer">
                    <el-form :inline="true" :model="fileDataForm">
                        <el-form-item>
                            <el-input v-model="fileName" placeholder="输入文件名搜索"  clearable></el-input>
                        </el-form-item>
                    </el-form>
                    <afp-button  type="primary" plain @click="search">查找</afp-button>
                    <afp-button  type="danger" plain @click="deleteFile"
                        v-if="selectFileList && selectFileList.length > 0">批量删除</afp-button>
                </div>
                <div style="display: flex; align-items: center;">
                    <!-- <el-select v-model="uploadFileDataForm.platformType" placeholder="请选择平台" 
                    style="width: 100px;margin-right: 10px">
                    <el-option v-for="item in platformList" :key="item.id" :label="item.label" :value="item.value">
                    </el-option>
                </el-select>
                <el-cascader v-model="tradeIdArr" :options="tradeTreeList" style="width: 260px;margin-right: 10px;"
                    :props="{ checkStrictly: true, value: 'id', label: 'name' }" filterable 
                    placeholder="选择一个行业，提高关键词匹配准确性">
                </el-cascader> -->
                    <afp-button  type="primary" @click="uploadFile" class="uploadFileBtn">上传文件</afp-button>
                </div>
            </div>
            <!-- 对比 -->
            <div v-if="contrastList && contrastList.length > 0" style="display: flex; align-items: center;">
                <el-tag v-for="tag in contrastList" :key="tag.FileId" closable style="margin-right: 10px;"
                    @close="cancelContrast(tag)">
                    <!-- {{ tag.AnchorInfo.AnchorName + "(" + tag.VideoName + ")" }} -->
                    {{ tag.FileName }}
                </el-tag>
                <afp-button size="small" type="primary" @click="contrastSubmit">开始对比</afp-button>
            </div>
            <!-- 视频列表 -->
            <el-table :data="fileList" :key="tableKey"  ref="fileTable" :max-height="tableMaxHeight"
                @selection-change="selectFileTable" row-key="Id">
                <el-table-column type="selection" reserve-selection width="55" align="center">
                </el-table-column>
                <el-table-column label="文件名称" align="left" show-overflow-tooltip>
                    <template slot-scope="scope">
                        <div class="fileColContainer">
                            <img src="@/assets/imgs/txt.png" class="videoImg" v-if="scope.row.FileType == 2">
                            <img src="@/assets/imgs/video.png" class="videoImg" v-else>
                            <div class="fileNameContainer">
                                <div class="videoNameText">{{ scope.row.FileName }}</div>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="文件大小" align="center" width="130">
                    <template slot-scope="scope">
                        <div>{{ scope.row.FileSize + "M" }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="话术质检" align="center" width="145">
                    <template slot-scope="scope">
                        <div v-if="getAiMonitorState(scope.row).qcReportStatus === 1" class="text-color3">生成中</div>
                        <AiMonitorPreviewContent
                            v-else-if="getAiMonitorState(scope.row).qcReportStatus === 2"
                            :content="getAiMonitorState(scope.row).qcPreview"
                            preview-type="qc"
                            parse-mode="structured"
                            @view="openAiReport(scope.row)"
                        />
                        <div v-else-if="Number(scope.row.AnalysisStatus) !== 2">-</div>
                        <div v-else-if="getAiMonitorState(scope.row).qcReportStatus === 3" class="color-main cursor-pointer"
                             @click="generateAiReport(scope.row)">重试生成</div>
                        <div v-else class="color-main cursor-pointer" @click="generateAiReport(scope.row)">生成报告</div>
                    </template>
                </el-table-column>
                <!--
                <el-table-column label="话术还原度" align="center" width="135">
                    <template slot-scope="scope">
                        <ScriptRestoreCell :row="scope.row" scene="upload" :mock-index="scope.$index" />
                    </template>
                </el-table-column>
                -->
                <el-table-column label="上传时间" align="center" width="200">
                    <template slot-scope="scope">
                        <div>{{ scope.row.UploadTime }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="分析时间" align="center" width="200">
                    <template slot-scope="scope">
                        <div>{{ scope.row.AnalysisTime }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="分析状态" align="center" width="130">
                    <template slot-scope="scope">
                        <div class="fileSizeColContainer">
                            <div>{{ ["未分析", "分析中", "分析完成", "分析失败"][scope.row.AnalysisStatus] }}</div>
                            <div style="color: red;" v-if="scope.row.ErrorReason">{{ "(" + scope.row.ErrorReason + ")"
                                }}
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="130">
                    <template slot-scope="scope">
                        <div class="operateColContainer">
                            <div class="analysisContainer">
                                <div class="createAnalysis" @click="createAnalysis(scope.row.FileId)"
                                    v-if="scope.row.AnalysisStatus == 0">
                                    生成分析</div>
                                <div class="lookAnalysis" @click="lockAnalysis(scope.row.FileId)"
                                    v-if="scope.row.AnalysisStatus == 2">
                                    查看分析</div>
                                <div v-if="scope.row.AnalysisStatus == 3" class="lookAnalysis"
                                    @click="reAnalysis(scope.row.FileId)">
                                    重新分析</div>
                            </div>
                            <div class="operateBtnContainer">
                                <div class="operateBtn" @click="preview(scope.row.FileId)"
                                    v-if="scope.row.AnalysisStatus == 2">
                                    预览
                                </div>
                                <div class="operateBtn" @click="addContrast(scope.row)"
                                    v-if="!scope.row.isContrast && scope.row.AnalysisStatus == 2">加入对比</div>
                                <div class="contrastBtn" @click="cancelContrast(scope.row)"
                                    v-if="scope.row.isContrast && scope.row.AnalysisStatus == 2">取消对比</div>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <template slot="empty">
                    <div style="display: flex;flex-direction: column;align-items: center;">
                        <span style="font-size: 14px">支持本地上传视频/音频/txt文件进行话术分析</span>
                        <afp-button  type="primary" @click="uploadFile" class="uploadFileBtn"
                            style="width: 100px;margin-bottom: 10px;">立即上传</afp-button>
                    </div>
                </template>
            </el-table>

            <!-- 分页 -->
            <div style="position: absolute; bottom: 16px;left: 0;right: 0;">
                <el-pagination style="text-align: center; margin-top: 10px" @size-change="sizeChangeHandle"
                    :key="paginationKey" @current-change="currentChangeHandle" :current-page="pageIndex"
                    :page-sizes="[10, 20, 50, 100]" :page-size="pageSize" :total="totalCount"
                    layout="total, sizes, prev, pager, next, jumper">
                </el-pagination>
            </div>

            <!-- 行业弹窗 -->
            <el-dialog title="行业选择" :visible.sync="tardeDialogVisible" width="600px" :close-on-click-modal="false">
                <el-cascader v-model="tradeIdArr" :options="tradeTreeList" style="width: 100%;"
                    :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name' }" filterable
                    placeholder="选择或者搜索输入一个行业，以提高分析准确性" @change="changeTradeHandle" ref="tradeCascader">
                </el-cascader>
                <span slot="footer" class="dialog-footer">
                    <afp-button @click="tardeDialogVisible = false">取消</afp-button>
                    <afp-button type="primary" @click="createAnalysisConfirm()">确定</afp-button>
                </span>
            </el-dialog>
        </div>

        <upload-file-analysis v-if="currentPage == 'fileUploadAnalysis'"
            @switchList="switchList"></upload-file-analysis>

        <AiMonitorReportDialog
            :visible.sync="aiMonitorReportVisible"
            type="qc"
            :row="aiMonitorReportRow"
            :reportId="aiMonitorReportId"
        />
    </div>
</template>

<script>
import myUtils from '@/utils/utils.js';
import uploadFileAnalysis from "./file-upload-analysis.vue";
// import ScriptRestoreCell from '@/components/aiMonitor/scriptRestoreCell/index.vue'
import AiMonitorReportDialog from '@/components/aiMonitor/reportDialog/index.vue'
import AiMonitorPreviewContent from '@/components/aiMonitor/previewContent/index.vue'
import { VERSION_TYPE } from '@/enum'

export default {
    name: 'ReplayClientAnalysisFinishList',
    components: {
        uploadFileAnalysis,
        AiMonitorReportDialog,
        AiMonitorPreviewContent,
        // ScriptRestoreCell
    },
    computed: {
        versionTypeIsPure() {
            return this.$store.getters.getVersionType === VERSION_TYPE.PURE || this.$store.getters.isPure
        }
    },
    data() {
        return {
            currentPage: 'fileUploadList',
            aiMonitorStateMap: {},
            aiMonitorReportVisible: false,
            aiMonitorReportId: null,
            aiMonitorReportRow: null,
            paginationKey: 0,
            tardeDialogVisible: false,
            tableKey: false,
            selectFileList: [],
            tradeTreeList: [], // 行业列表
            platformList: [], // 平台列表
            tradeIdArr: [],
            tableMaxHeight: 0,
            uploadFileDataForm: {
                token: "",
                tradeId: "",
                platformType: 1,
            },
            pageIndex: 1,
            pageSize: 10,
            totalCount: 0,
            fileName: "",
            uploadDate: [],
            analysisDate: [],
            resourceTypeList: [
                { value: 0, label: "抖音" },
                { value: 1, label: "快手" },
            ],
            contrastList: [], // 对比列表
            fileList: [], // 视频列表
            fileDataForm: {
                pageIndex: 1,
                pageSize: 10,
                anchorId: -1,
                analysisStatus: 2,
                recordStartDate: "",
                recordEndDate: "",
                analysisStartDate: "",
                analysisEndDate: "",
            },
            listTimer: null,
            currentFileId: "",
            isReAnalysis: false,
        };
    },
    watch: {
        aiMonitorReportVisible(val) {
            if (val) return
            this.aiMonitorReportId = null
            this.aiMonitorReportRow = null
        },
        contrastList(newVal, oldVal) {
            if (newVal && newVal.length > 0) {
                this.tableMaxHeight = (document.documentElement.clientHeight || document.body.clientHeight) - 210;
            } else {
                this.tableMaxHeight = (document.documentElement.clientHeight || document.body.clientHeight) - 180;
            }
        }
    },
    inject: ['appVnode'],
    mounted() {

        // 查询参数
        let queryParam = this.$store.state.fileListQuery;
        if (queryParam) {
            this.pageIndex = queryParam.pageIndex;
            this.pageSize = queryParam.pageSize;
            this.fileName = queryParam.fileName;
            this.$store.commit("saveFileListQuery", null);
            this.paginationKey++;
        }

        this.updateViewportHeight();
        window.addEventListener('resize', this.updateViewportHeight);

        this.getPlatformList();
        this.getFileList();
        this.getTradeTreeList();

        if (this.listTimer) {
            clearInterval(this.listTimer);
            this.listTimer = null;
        }
        this.listTimer = setInterval(() => {
            this.getFileList(true);
        }, 10000);
    },
    beforeDestroy() {
        if (this.listTimer) {
            clearInterval(this.listTimer);
            this.listTimer = null;
        }
    },
    methods: {
        getAiMonitorState(row) {
            const id = row?.FileId
            if (!id) return { qcReportStatus: 0, qcReportId: null, qcPreview: '' }
            if (!this.aiMonitorStateMap[id]) {
                this.$set(this.aiMonitorStateMap, id, { qcReportStatus: 0, qcReportId: null, qcPreview: '' })
            }
            return this.aiMonitorStateMap[id]
        },
        async loadAiMonitorStatus(list = []) {
            const rows = Array.isArray(list) ? list.filter(r => !!r?.FileId) : []
            if (!rows.length) return
            if (!this.$httpBack?.scriptMonitor?.batchReportStatus && !this.$httpBack?.scriptMonitor?.getScriptMonitorStatus) return
            const processRow = (row, data) => {
                const state = this.getAiMonitorState(row)
                const qc = data?.scriptQualityInspection || {}
                state.qcReportStatus = Number(qc?.status ?? 0)
                state.qcReportId = qc.reportId || null
                state.qcPreview = (qc?.summary ?? '') || ''
            }
            if (this.$httpBack?.scriptMonitor?.batchReportStatus) {
                try {
                    const sources = rows.map((row) => ({
                        sourceType: 1,
                        sceneType: 0,
                        sourceId: String(row.FileId)
                    }))
                    const res = await this.$httpBack.scriptMonitor.batchReportStatus({ sources })
                    if (res?.code === 0) {
                        const items = Array.isArray(res?.data?.items) ? res.data.items : (Array.isArray(res?.data) ? res.data : [])
                        const map = new Map(items.map((it) => [String(it?.sourceId ?? ''), it]))
                        rows.forEach((row) => {
                            const data = map.get(String(row.FileId))
                            if (data) processRow(row, data)
                        })
                        return
                    }
                } catch (e) {}
            }
            const tasks = rows.map(async (row) => {
                try {
                    const res = await this.$httpBack.scriptMonitor.getScriptMonitorStatus({ sourceType: 1, sceneType: 0, sourceId: String(row.FileId) })
                    if (res?.code !== 0) return
                    const qc = (res?.data?.monitors || []).find(a => a.monitorType === 0) || {}
                    processRow(row, { scriptQualityInspection: qc })
                } catch (e) {}
            })
            await Promise.all(tasks)
        },
        async generateAiReport(row) {
            if (!row?.FileId) return
            if (Number(row.AnalysisStatus) !== 2) return
            if (!this.$httpBack?.scriptMonitor?.triggerScriptMonitorReport) return
            const state = this.getAiMonitorState(row)
            if (state.qcReportStatus === 1) return
            state.qcReportStatus = 1
            try {
                const res = await this.$httpBack.scriptMonitor.triggerScriptMonitorReport({
                    sourceType: 1,
                    sceneType: 0,
                    sourceId: String(row.FileId),
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
        switchList() {
            this.currentPage = "fileUploadList"
        },
        // 选择行业回调
        changeTradeHandle() {
            // 关闭级联列表下拉
            this.$refs.tradeCascader.dropDownVisible = false;
        },
        // 删除文件
        deleteFile() {
            this.$confirm('将永久删除选中的文件, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                let ids = [];
                this.selectFileList.forEach(item => {
                    ids.push(item.FileId);
                });
                this.$httpClient.uploadFile.deletebyids(ids).then(res => {
                    if (res.code == 0 && res.data) {
                        this.selectFileList = [];
                        this.$refs['fileTable'].clearSelection();
                        this.$message.success("删除成功");
                        this.getFileList();
                    } else {
                        this.$message.warning("请等待分析结束后再删除");
                    }
                    this.appVnode?.getDisk();
                });
            })
        },
        selectFileTable(value) {
            this.selectFileList = value;
        },
        // 计算表格高度
        updateViewportHeight() {
            this.tableMaxHeight = window.innerHeight - 160;
            this.tableKey = !this.tableKey;
        },
        // 获取行业列表树形
        getTradeTreeList() {
            this.tradeTreeList = [];
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data;
                }
            });
        },
        // 获取平台列表
        getPlatformList() {
            this.platformList = [];
            this.$httpBack.dictdata.list({ limit: -1, typeLogo: "words_platform_type" }).then((res) => {
                if (res.code == 0) {
                    this.platformList = res.data.list;
                    this.platformList.forEach(item => {
                        item.value = parseInt(item.value)
                    });
                    this.uploadFileDataForm.platformType = this.platformList[0].value;
                }
            })
        },
        // 预览
        preview(fileId) {
            this.$httpClient.uploadFile.preview({ fileId }).then(res => {
                if (res.code == 0 && res.data) {
                    window.open(res.data, "_blank");
                }

            })
        },
        // 选择文件
        uploadFile() {
            let requestData = {
                token: this.$store.state.token,
                platformType: this.uploadFileDataForm.platformType,
                tradeId: "1"
            }
            if (this.tradeIdArr && this.tradeIdArr.length > 0) {
                requestData.tradeId = this.tradeIdArr[this.tradeIdArr.length - 1];
            }

            localStorage.setItem("notCloseLoading", "1");
            const loading = this.$loading({
                lock: true,
                background: 'rgba(0, 0, 0, 0.7)'
            });

            this.$httpClient.uploadFile.checkfile(requestData).then(res => {
                if (res.code == 0 && res.data) {

                    this.$message.success("上传成功");
                    this.getFileList();

                }
                localStorage.setItem("notCloseLoading", "");
                loading.close();

            }).catch(err => {
                localStorage.setItem("notCloseLoading", "");
                loading.close();
            })
        },
        // 重新生成分析
        reAnalysis(fileId) {
            // 设置默认行业
            if (localStorage.getItem("saveAnalysisTradeFile")) {
                this.tradeIdArr = JSON.parse(localStorage.getItem("saveAnalysisTradeFile"));
            } else {
                this.tradeIdArr = null;
            }
            this.currentFileId = fileId;
            this.isReAnalysis = true;
            this.tardeDialogVisible = true;

        },
        // 确认生成直播分析
        createAnalysisConfirm() {
            let token = this.$store.state.token;
            let tradeId = "1";
            if (this.tradeIdArr && this.tradeIdArr.length > 0) {
                tradeId = this.tradeIdArr[this.tradeIdArr.length - 1];
                localStorage.setItem('saveAnalysisTradeFile', JSON.stringify(this.tradeIdArr));
            } else {
                localStorage.setItem('saveAnalysisTradeFile', JSON.stringify(["1"]));
            }

            if (this.isReAnalysis) {
                // 重新分析
                this.$httpClient.uploadFile.reanalysis({ fileId: this.currentFileId, platformType: 1, token, tradeId }).then((res) => {
                    if (res.code == 0 && res.data) {
                        this.tardeDialogVisible = false;
                        this.getFileList();
                    } else {
                        this.$message.warning("请等待上一个分析完再继续");
                    }
                });
            } else {
                // 创建分析
                this.$httpClient.uploadFile.createAnalysis({ fileId: this.currentFileId, token, tradeId }).then((res) => {
                    if (res.code == 0 && res.data) {
                        this.tardeDialogVisible = false;
                        this.getFileList();
                    } else {
                        this.$message.warning("请等待上一个分析完再继续");
                    }
                });
            }

        },
        // 生成直播分析弹窗
        createAnalysis(fileId) {
            // 设置默认行业
            if (localStorage.getItem("saveAnalysisTradeFile")) {
                this.tradeIdArr = JSON.parse(localStorage.getItem("saveAnalysisTradeFile"));
            } else {
                this.tradeIdArr = null;
            }
            this.currentFileId = fileId;
            this.isReAnalysis = false;
            this.tardeDialogVisible = true;
        },
        // 查看分析
        lockAnalysis(id) {
            this.$store.commit("saveFileAnalysisId", id);
            this.$store.commit("saveFileListQuery", {
                fileName: this.fileName,
                pageIndex: this.pageIndex,
                pageSize: this.pageSize
            });

            this.currentPage = "fileUploadAnalysis";

            // // 设置智能复盘页的菜单和tabs
            // this.$store.commit("saveReplayPageMenu", { page: 'fileUploadAnalysis' })
            // // 跳转到智能复盘页
            // this.$emit("updateMenuIndex", 2);
        },
        // 提交对比
        contrastSubmit() {
            if (!this.contrastList || this.contrastList.length != 2) {
                this.$message.error("请选择两个进行对比");
                return;
            }

            let requestData = {
                FileOneId: this.contrastList[0].FileId,
                FileTwoId: this.contrastList[1].FileId,
            }

            // let requestData = {
            //     fileOneId: this.contrastList[0].FileId,
            //     fileTwoId: this.contrastList[1].FileId,
            // }

            this.$httpClient.contrast.savecontrast(requestData).then(res => {
                if (res.code == 0) {
                    this.fileList.forEach(item => {
                        item.isContrast = false;
                    });
                    this.contrastList = [];
                    // // 设置智能复盘页的菜单和tabs
                    this.$store.commit("saveReplayPageMenu", { page: 'tabs', menu: 'compare' })
                    // // 跳转到智能复盘页
                    this.$emit("updateMenuIndex", 2);
                    this.toMenu("compare");
                }
            })

            // this.$httpBack.contrast.clientAddContrast(requestData).then(res => {
            //     if (res.code == 0) {
            //         this.fileList.forEach(item => {
            //             item.isContrast = false;
            //         });
            //         this.contrastList = [];
            //         // // 设置智能复盘页的菜单和tabs
            //         this.$store.commit("saveReplayPageMenu", { page: 'tabs', menu: 'compare' })
            //         // // 跳转到智能复盘页
            //         this.$emit("updateMenuIndex", 2);
            //         this.toMenu("compare");
            //     }
            // })

        },
        // 取消对比
        cancelContrast(data) {
            this.contrastList = this.contrastList.filter(item => item.FileId != data.FileId);

            this.fileList.forEach(item => {
                if (item.FileId == data.FileId) {
                    item.isContrast = false;
                }
            });
            this.tableKey = !this.tableKey;
        },
        // 加入对比
        addContrast(data) {
            if (this.contrastList && this.contrastList.length >= 2) {
                this.$message.error("仅支持两个对比");
                return;
            }
            this.fileList.forEach(item => {
                if (item.FileId == data.FileId) {
                    item.isContrast = true;
                }
            });
            this.tableKey = !this.tableKey;
            this.contrastList.push(data);
        },
        search() {
            this.pageIndex = 1;
            this.getFileList();
        },
        // 获取文件列表
        getFileList(notReset) {
            if (!notReset) {
                this.fileList = [];
            }
            this.fileDataForm = {
                pageIndex: 1,
                pageSize: 10,
                analysisStatus: -1,
                fileName: this.fileName ? encodeURIComponent(this.fileName) : "",
                uploadStartDate: "",
                uploadEndDate: "",
                analysisStartDate: "",
                analysisEndDate: "",
            };
            this.fileDataForm.pageIndex = this.pageIndex;
            this.fileDataForm.pageSize = this.pageSize;

            if (this.uploadDate && this.uploadDate.length > 0) {
                this.fileDataForm.uploadStartDate = this.uploadDate[0];
                this.fileDataForm.uploadEndDate = this.uploadDate[1];
            }
            if (this.analysisDate && this.analysisDate.length > 0) {
                this.fileDataForm.analysisStartDate = this.analysisDate[0];
                this.fileDataForm.analysisEndDate = this.analysisDate[1];
            }

            this.$httpClient.uploadFile.getpage(this.fileDataForm).then((res) => {
                if (res.code == 0) {
                    this.totalCount = res.data.Total;
                    this.fileList = res.data.DataList;
                    if (this.fileList && this.fileList.length > 0) {
                        this.fileList.forEach(item => {
                            if (item.Duration) {
                                item.Duration = myUtils.toformatTime(item.Duration * 1000);
                            }
                            if (item.FileSize) {
                                item.FileSize = myUtils.retainDecimals(item.FileSize / 1024 / 1024);
                            }

                            if (this.contrastList && this.contrastList.length > 0) {
                                this.contrastList.forEach(contrastItem => {
                                    if (contrastItem.FileId == item.FileId) {
                                        item.isContrast = true;
                                    }
                                })
                            }

                        })
                    }
                    this.$nextTick(() => {
                        this.loadAiMonitorStatus(this.fileList)
                    })
                }
            })
        },
        toPage(url) {
            this.$emit("toPage", url);
        },
        toMenu(menu) {
            this.$emit("toMenu", menu);
        },
        // 每页数
        sizeChangeHandle(val) {
            this.pageSize = val;
            this.pageIndex = 1;
            this.getFileList();
            this.selectFileList = [];
        },
        // 当前页
        currentChangeHandle(val) {
            this.selectFileList = [];
            this.$refs['fileTable'].clearSelection();
            this.pageIndex = val;
            this.getFileList();
            this.selectFileList = [];
        },
    },
};
</script>

<style scoped lang="less">
/deep/ .el-table__body-wrapper::-webkit-scrollbar {
    width: 17px; // 横向滚动条
}

/*滚动条凹槽的颜色，还可以设置边框属性 */
/deep/::-webkit-scrollbar-track-piece {
    width: 17px;
}

// 滚动条的滑块
/deep/ .el-table__body-wrapper::-webkit-scrollbar-thumb {
    background: #ddd;
    border-radius: 8px;
}

.uploadFileBtn {
    margin-right: 10px;
}

.aiMonitorPreview{
    cursor: pointer;
    text-align: left;
}

.contrastBtn {
    height: 20px;
    border-radius: 4px;
    border: 1px solid var(--color-main);
    font-size: 12px;
    color: var(--color-main);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    padding: 0 4px;
}

.operateBtn {
    height: 20px;
    border-radius: 4px;
    border: 1px solid #B4BCCA;
    font-size: 12px;
    color: #2E3742;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    padding: 0 4px;
}

.operateBtnContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 5px;
}

.createAnalysis {
    width: 98px;
    height: 20px;
    border-radius: 4px;
    border: 1px solid var(--color-main);
    font-size: 12px;
    color: #fff;
    background-color: var(--color-main);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
}

.lookAnalysis {
    width: 98px;
    height: 20px;
    border-radius: 4px;
    border: 1px solid var(--color-main);
    font-size: 12px;
    color: var(--color-main);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
}

.analysisContainer {
    display: flex;
    align-items: center;
    justify-content: center;
}

.operateText {
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}

.operateColContainer {
    display: flex;
    flex-direction: column;
    width: 100px;
    justify-content: center;
}

.fileSizeColContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
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

.recordColContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
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
    justify-content: center;
    margin-left: 12px;
}

.videoImg {
    width: 40px;
    height: 40px;
}

.fileColContainer {
    display: flex;
}

.comperePlayStatusText {
    margin-left: 4px;
    font-weight: 500;
    font-size: 13px;
    color: #FF3270;
}

.comperePlayStatusImg {
    width: 18px;
}

.comperePlayStatus {
    display: flex;
    align-items: center;
}

.compereResourceType {
    background: #200B1C;
    border-radius: 8px;
    font-size: 10px;
    color: #FFFFFF;
    text-align: center;
    height: 15px;
    line-height: 15px;
    padding: 0 8px;
    margin-left: 5px;
}

.compereNotPlayStatus {
    font-size: 13px;
    color: #95A1AF;
}

.compereStatusContainer {
    display: flex;
    align-items: center;
}

.compereName {
    font-size: 14px;
    color: #2E3742;
}

.compereInfoContainer {
    margin-left: 12px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.compereImg {
    width: 40px;
    height: 40px;
    border-radius: 50%;
}

.compereColContainer {
    flex: 1;
    display: flex;
    align-items: center;
}

.listRowContainer {
    display: flex;
    align-items: center;
    padding: 10px 13px;
}

.tableContainer::-webkit-scrollbar {
    display: none;
}

.tableContainer {
    max-height: calc(100vh - 210px);
    overflow-y: auto;
}

.listTitleContainer {
    font-weight: 500;
    font-size: 12px;
    color: #2E3742;
    height: 28px;
    background: #F5F7F9;
    border-radius: 4px;
    display: flex;
    align-items: center;
    padding: 0 13px;
    margin-top: 4px;
}

.searchLeftContainer {
    display: flex;
    align-items: center;
}

.searchContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 10px;
}

/deep/ .el-form-item {
    margin-bottom: 0px;
}

/deep/ .el-range-separator {
    width: 10%;
}
</style>

<!--
@description AI内容诊断报告抽屉组件。【废弃】2026-08-03 列表入口已移除，组件保留备用但不从列表唤起。
-->
<template>
    <div class="aiReport">
        <el-drawer
            :destroy-on-close="true"
            :visible.sync="drawerStatus"
            :size="800"
            @close="closeDrawer"
            :wrapperClosable="false"
            :close-on-press-escape="false"
            :with-header="false">
            <div class="question-selector">
                <div class="qs-title">
                    <i class="el-icon-close close-icon" @click="closeAiReport"></i>
                    <span>生成内容诊断</span>
                </div>
                <div class="qs-desc" v-if="!isReplay">
                    *请选择AI诊断报告涵盖的问题，直播间录制结束后会自动生成报告
                </div>
                <div class="qs-desc" style="color:#FC6467;" v-if="!isReplay">
                    *选择的问题越多，消耗的AI算力包越多，请按需求选择
                </div>
                <div class="qs-desc" style="color:#FC6467;" v-if="!isReplay">
                    *开启自动诊断报告后，每一场都会自动生成诊断报告！！！
                </div>
                <div v-if="isReplay">
                    <div class="qs-desc qs-desc-title">
                        <span>*若干个AI分析问题组成一份AI诊断报告，请按照以下步骤生成AI诊断报告：</span>
                        <!--                        <span style="color: #0077FF">新手引导 <i class="el-icon-question"-->
                        <!--                                                                 style="color: #0077FF"></i></span>-->
                    </div>
                    <div class="qs-desc">步骤一：先生成单个问题的AI分析</div>
                    <div class="qs-desc">步骤二：勾选需要涵盖的AI分析问题</div>
                    <div class="qs-desc">步骤三：点击“合成AI诊断报告”</div>
                    <div class="qs-desc" style="margin-bottom: 0;color: #FC6467">友情提醒:
                        请按分析需要选择，选择的问题越多，消耗的AI算力包越多。
                    </div>
                </div>
                <div class="aiReport_content">
                    <el-tabs v-model="activeTab" class="qs-tabs">
                        <el-tab-pane
                            v-for="(tab,index) in tabsList"
                            :key="index"
                            :label="tab.tagName"
                            :name="tab.name">
                            <div class="all">
                                <el-checkbox
                                    :indeterminate="tab.isIndeterminate"
                                    v-model="tab.checkAll"
                                    :disabled="allDisabled(tab)"
                                    @change="handleCheckAllChange(tab.name, $event)">
                                    全选
                                </el-checkbox>
                            </div>
                            <el-checkbox-group v-model="selectedCueWordsList[tab.name]"
                                               :class="{'qs-checkbox-group':true,'qs-checkbox-group-init':!isReplay}"
                                               @change="handleCheckedChange(tab.name, $event)">
                                <div v-for="item in tab.cueWordsList"
                                     :key="item.cueWordsId"
                                     class="question-item">
                                    <el-checkbox
                                        :disabled="item.qaStatus!==2"
                                        :label="item.cueWordsId"
                                        class="qs-checkbox">
                                        <div class="cueword">
                                            {{ item.cueWord }}
                                        </div>
                                    </el-checkbox>
                                    <div class="action-buttons" v-if="isReplay">
                                        <template v-if="[0,1].includes(item.qaStatus)">
                                            <afp-button size="small" class="processing-btn" disabled>分析中
                                            </afp-button>
                                        </template>
                                        <template v-else-if="item.qaStatus === 2">
                                            <el-button type="text" class="view-btn"
                                                       @click="viewAnalysis(tab,item)">
                                                查看AI分析
                                            </el-button>
                                            <afp-button class="reanalyze-btn" size="small"
                                                        @click="generateAnalysis(tab.name,item)">重新分析
                                            </afp-button>
                                        </template>
                                        <template v-else-if="item.qaStatus === 3">
                                            <afp-button size="small" class="reanalyze-btn"
                                                        @click="generateAnalysis(tab.name,item)">重新分析
                                            </afp-button>
                                            <afp-button size="small" type="danger" disabled>分析失败
                                            </afp-button>
                                        </template>
                                        <template v-else>
                                            <afp-button size="small" @click="generateAnalysis(tab.name,item)">生成AI分析
                                            </afp-button>
                                        </template>
                                    </div>
                                </div>
                            </el-checkbox-group>
                        </el-tab-pane>
                    </el-tabs>
                    <div class="model-content">
                        <div class="model-content-title">选择模型：</div>
                        <el-select v-model="aiModel" placeholder="请选择" @change="updateDiagnosisModel" size="medium"
                                   style="width: 185px">
                            <el-option
                                v-for="item in modelOptions"
                                :key="item.id"
                                :label="item.modelName"
                                :value="item.id">
                            </el-option>
                        </el-select>
                    </div>
                </div>

                <div class="qs-footer">
                    <afp-button size="medium" :disabled="confirmDisabled"
                                v-if="!isReplay"
                                @click="handleConfirm">确 定
                    </afp-button>
                    <afp-button size="medium" v-if="isReplay"
                                :disabled="aIDiagnosticReport"
                                @click="handleConfirm">合成AI诊断报告
                    </afp-button>
                    <afp-button type="primary" :plain="false" size="medium" v-if="isReplay"
                                :disabled="aIDiagnosticReport"
                                @click="handleView">预览合并报告
                    </afp-button>
                </div>
            </div>
        </el-drawer>
        <MergeReport v-if="mergeReportVisible" ref="merge_report" :currentSelectedObj="currentSelectedObj"
                     @mergeReportStatus="mergeReportStatus"/>
        <DialogAiContent ref="dialog_ai_report" :tabsList="tabsList" :isReport="isReport"  :uploadType="1"
                         :selectedRow="{...selectedRow,outputName}" :sponsorship="getModelZh"
                         :hideReportModal="hideReportModal"/>
    </div>
</template>

<script>
import {cloneDeep} from 'lodash'
import aiContentReport from '@/mixins/aiContentReport'
import MergeReport from './mergeReport.vue'
import DialogAiContent from './dialogAIContent.vue'

export default {
    name: 'AiReport',
    components: {
        MergeReport,
        DialogAiContent
    },
    mixins: [aiContentReport],
    props: {
        isReplay: {
            type: Boolean,
            default: false
        },
        diagnosisParams: {
            type: Object,
            default: () => {
                return {}
            }
        },
    },
    data() {
        return {
            modelOptions: [],
            currentSelectedObj: {},
            aiModel: '',
            mergeReportVisible: false,
            listDiagnosisTimer: null,
            outputName: ''
        }
    },
    created() {
    },
    mounted() {
    },
    watch: {
        drawerStatus: {
            handler(val) {
                if (!val) {
                    this.stopPolling()
                }
            },
            immediate: true
        },
    },
    computed: {
        getModelZh() {
            return this.sponsorship || this.modelOptions.find(a => a.id === this.aiModel)?.modelName
        },
        aIDiagnosticReport() {
            return Object.values(this.selectedCueWordsList).every(arr => arr.length === 0)
        },
        allDisabled() {
            return (item) => {
                const cueWordsList = item.cueWordsList?.filter(a => a.qaStatus === 2)
                return cueWordsList?.length === 0
            }
        },
        allItemsCompleting() {
            return this.tabsList.some(item =>
                item.cueWordsList.some(cueWord => [0, 1].includes(cueWord.qaStatus))
            );
        },
        confirmDisabled() {
            const selectedCueWordsList = Object.values(this.selectedCueWordsList)?.flat()
            return selectedCueWordsList.length === 0 || !this.aiModel
        }
    },
    methods: {
        async changeDrawerStatus(status, row) {
            this.selectedCueWordsList = {}
            this.selectedRow = row
            if (status) {
                await this.getModelList()
                this.getListDiagnosis(row?.isAddCompere).then(() => {
                    this.initCheckedStatus()
                    this.drawerStatus = status
                })
                return
            }
            this.drawerStatus = false
            this.selectedCueWordsList = {}
        },
        getModelList() {
            this.$httpBack.v2500.listDiagnosisModel().then(result => {
                if (result.code === 0) {
                    this.modelOptions = result.data
                    this.aiModel = this.diagnosisParams?.modelId
                }
            })
        },
        getUnSelectIds() {
            const deepList = cloneDeep(this.tabsList)
            const allCanCueWordsIds = deepList
                .flatMap(item => item.cueWordsList || [])
                .filter(cue => cue.qaStatus === 2)
                .map(cue => cue.cueWordsId);
            const allSelectedIds = Object.values(this.selectedCueWordsList)?.flat()
            const allUnSelectIds = allCanCueWordsIds.filter(item => !allSelectedIds.includes(item));
            this.changeUnSelectIds = [...new Set(allUnSelectIds)]
        },
        handleCheckAllChange(tabName, checked) {
            const deepList = cloneDeep(this.tabsList)
            const tab = deepList.find(t => t.name === tabName)
            const selectIds = tab.cueWordsList.filter(a => a.qaStatus === 2).map(b => b.cueWordsId)
            this.selectedCueWordsList[tabName] = checked
                ? selectIds
                : []
            this.getUnSelectIds()
            deepList.map(item => {
                if (item.name === tabName) item.isIndeterminate = false
            })
            this.tabsList = deepList
        },
        handleCheckedChange(tabName, values) {
            const deepList = cloneDeep(this.tabsList)
            this.getUnSelectIds()
            this.selectedCueWordsList[tabName] = values
            const checkedCount = values.length
            const tab = deepList.find(t => t.name === tabName)
            const selectableQuestions = tab.cueWordsList.filter(a => a.qaStatus === 2) || []
            deepList.map(item => {
                if (item.name === tabName) {
                    item.checkAll = checkedCount === 0 ? false : checkedCount === selectableQuestions.length
                    item.isIndeterminate = checkedCount === 0 ? false : checkedCount > 0 && checkedCount < selectableQuestions.length
                }
            })
            this.tabsList = deepList
        },
        closeDrawer() {
            this.selectedCueWordsList = {}
        },
        closeAiReport() {
            if (!this.selectedRow?.action) this.$emit('initAiReport')
            this.stopPolling()
            this.drawerStatus = false
            this.selectedCueWordsList = {}
            this.activeTab = ''
            this.aiModel = ''
            this.selectedRow = {}
        },
        updateDiagnosisModel() {
            if (this.isReplay) {
                this.$httpBack.v2500.updateDiagnosisModel({
                    sourceId: this.selectedRow?.videoId,
                    sourceType: 1,
                    modelId: this.aiModel
                })
            }
        },
        getSelectedQuestions() {
            return Object.keys(this.selectedCueWordsList).reduce((result, key) => {
                // 找到对应的诊断项
                const section = this.tabsList.find(item => item.name === key)
                if (section) {
                    result[key] = {
                        tagName: section.tagName,
                        cueWordsList: section.cueWordsList.filter(_item => this.selectedCueWordsList[key]?.includes(_item.cueWordsId))
                    }
                }
                return result
            }, {})
        },
        handleConfirm() {
            const allSelectedIds = Object.values(this.selectedCueWordsList)?.flat()
            this.$confirm(`您目前选择了${allSelectedIds.length} 个问题，${this.isReplay ? '' : `预计每场直播消耗${allSelectedIds.length}*2.5万个AI算力，`}您确定${this.isReplay?'':'自动'}生成诊断报告吗？`, '友情提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.handleConfirmDialog()
            }).catch(() => {
            });
        },
        handleView() {
            this.hideReportModal(false)
            this.$nextTick(() => {
                this.$refs.dialog_ai_report?.changeDialogVisible(this.tabsList, this.selectedCueWordsList, true)
            })
        },
        handleConfirmDialog() {
            if (this.isReplay) {
                this.currentSelectedObj = this.getSelectedQuestions()
                this.mergeReportVisible = true
                this.$nextTick(() => {
                    this.$refs.merge_report.changeMergeReportStatus(true, this.selectedRow.reportFileName)
                })
            } else {
                this.$emit('setDiagnosisParams', {
                    modelId: this.aiModel,
                    cueWordsIds: Object.values(this.selectedCueWordsList)?.flat()
                })
                this.changeDrawerStatus(false, {})
                this.$message.success('自动生成AI报告设置成功')
            }
        },
        updateStatus(tabName, key, qaStatus) {
            const targetGroup = this.tabsList.find(group => group.name === tabName)
            if (targetGroup) {
                const targetCueWord = targetGroup.cueWordsList.find(q => q.cueWordsId === key)
                if (targetCueWord) {
                    targetCueWord.qaStatus = qaStatus
                }
            }
        },

        stopPolling() {
            if (this.listDiagnosisTimer) {
                clearTimeout(this.listDiagnosisTimer)
                this.listDiagnosisTimer = null
            }
        },
        scheduleNextPoll() {
            this.stopPolling()
            if (this.allItemsCompleting) {
                this.listDiagnosisTimer = setTimeout(async () => {
                    await this.getListDiagnosis()
                    this.initCheckedStatus()
                }, 12000)
            }
        },
        // 生成AI分析
        async generateAnalysis(tabName, item) {
            const {data: result, code} = await this.$httpBack.userProperty.info()
            if (code !== 0) return
            if (result?.aiTokenNum <= 0) return this.$message.error('AI助手分析余量不足，请联系产品顾问进行套餐外购买')

            const selectedQs = this.selectedCueWordsList[tabName]?.filter(_item => _item && (_item !== item.cueWordsId))
            this.selectedCueWordsList = {
                ...this.selectedCueWordsList,
                [tabName]: selectedQs
            }
            this.$nextTick(() => {
                this.$httpClient.aiRelated.saveDiagnosis({
                    videoId: this.selectedRow?.videoId,
                    cueWordsId: item.cueWordsId
                }).then(async res => {
                    if (res.code === 0) {
                        this.updateStatus(tabName, item.cueWordsId, 1)
                        await this.getListDiagnosis()
                        this.initCheckedStatus()
                    } else {
                        this.$message.error(res.msg)
                    }
                })
            })
        },
        // 查看AI分析
        viewAnalysis(tab, item) {
            this.hideReportModal(false)
            this.$nextTick(() => {
                this.$refs.dialog_ai_report?.changeDialogVisible([tab], {
                    [tab.name]: [item.cueWordsId]
                }, true)
            })
        },
        initCheckedStatus() {
            const deepList = cloneDeep(this.tabsList)
            if (!this.isReplay) {
                // 更新数组中的 qaStatus  非AI复盘 都可以选择
                deepList.map(item => {
                    item.cueWordsList.map(cueWord => {
                        if (cueWord.cueWordsId) {
                            cueWord.qaStatus = 2
                        }
                    })
                })
            }
            deepList.map(tab => {
                const cueWordsList = tab.cueWordsList.filter(a => a.qaStatus === 2)
                const checkAll = cueWordsList.length > 0 ? cueWordsList.every(q => this.selectedCueWordsList[tab.name]?.includes(q.cueWordsId)) : false
                tab.checkAll = checkAll
                tab.isIndeterminate = cueWordsList.length > 0 ? (cueWordsList.some(q => this.selectedCueWordsList[tab.name]?.includes(q.cueWordsId)) && !checkAll) : false
            })
            this.tabsList = deepList
        },
        mergeReportStatus(outputName) {
            this.outputName = outputName
            this.hideReportModal(true)
            this.$nextTick(() => {
                this.$refs.dialog_ai_report?.changeDialogVisible(this.tabsList, this.selectedCueWordsList, true)
            })
        }
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
        this.stopPolling()
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>

<style lang="scss" scoped>
.aiReport {
    .question-selector {
        background: #fff;
        border-radius: 8px;
        padding: 12px 12px 6px 12px;

        .qs-title {
            font-size: 16px;
            font-weight: 600;
            color: #151719;
            padding: 10px 0;

            .close-icon {
                cursor: pointer;
                padding-right: 12px;
            }
        }

        .qs-desc {
            font-size: 14px;
            margin: 10px 0;
            color: #000;
        }

        .qs-desc-title {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .aiReport_content {
            position: relative;

            .model-content {
                display: flex;
                align-items: center;
                position: absolute;
                top: 6px;
                right: 0;

                .model-content-title {
                    font-size: 13px;
                    padding: 0 5px;
                }
            }

            .qs-tabs {
                ::v-deep(.el-tabs__header) {
                    margin-bottom: 16px;

                    .el-tabs__nav-wrap:after {
                        display: none;
                    }

                    .el-tabs__item {
                        font-size: 15px;
                        color: #959799;
                        font-weight: 600;

                        &.is-active {
                            color: #000;
                        }
                    }

                    .el-tabs__active-bar {
                        background: var(--color-main);
                        height: 2px;
                        border-radius: 2px;
                    }
                }

                .all {
                    font-weight: 600;
                    height: 42px;
                    line-height: 42px;
                    width: 100%;
                    padding: 0 12px;
                    color: #151719;
                    border: 1px #EBEBEB solid;
                    border-bottom: none;
                }

                .qs-checkbox-group {
                    display: flex;
                    flex-direction: column;
                    max-height: calc(100vh - 360px);
                    border: 1px #EBEBEB solid;
                    overflow: auto;

                    .qs-checkbox {
                        font-size: 14px;
                        padding: 12px;
                        color: #151719;
                        margin-right: 0;
                        position: relative;
                        display: flex;
                        align-items: center;

                        .cueword {
                            word-break: break-all;
                            white-space: normal;
                            line-height: 1.5
                        }

                        &:not(:first-child) {
                            &::before {
                                content: '';
                                position: absolute;
                                height: 1px;
                                top: 0;
                                left: 0;
                                width: 100%;
                                background: #EBEBEB;
                            }
                        }
                    }

                    .question-item {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        position: relative;

                        &:nth-child(2n-1) {
                            background-color: #F5F7FB;
                        }

                        &:not(:first-child) {
                            &::before {
                                content: '';
                                position: absolute;
                                height: 1px;
                                top: 0;
                                left: 0;
                                width: 100%;
                                background: #EBEBEB;
                            }
                        }

                        .action-buttons {
                            padding-right: 12px;
                            display: flex;
                            align-items: center;

                            .view-btn {
                                color: var(--color-main);
                                font-size: 13px;
                                margin-right: 10px;
                            }

                            .reanalyze-btn {
                                color: #009CB8;
                                border-color: #009CB8
                            }

                            .error-btn {
                                color: red;
                                border-color: red;
                            }

                            .processing-btn {
                                color: #909499;
                                border-color: #909499
                            }
                        }
                    }
                }

                .qs-checkbox-group-init {
                    max-height: calc(100vh - 300px);
                }
            }
        }

        .qs-footer {
            display: flex;
            justify-content: flex-start;
            margin-top: 12px;
        }
    }
}
</style>

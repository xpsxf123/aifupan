<template>
    <div class="text-extraction-container flex flex-col">
        <el-card class="filter-container">
            <el-form :inline="true" :model="searchValues" size="default">
                <el-form-item label="提取日期:">
                    <el-date-picker
                        clearable
                        v-model="searchValues.dateRange"
                        type="daterange"
                        size="default"
                        range-separator="→"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        class="date-picker"
                        value-format="yyyy-MM-dd"
                    />
                </el-form-item>
                <el-form-item label="短视频名称:">
                    <el-input
                        clearable
                        size="default"
                        v-model="searchValues.videoTitle"
                        placeholder="请输入"
                        class="video-name-input input-gray input-border-none"
                    />
                </el-form-item>

                <el-form-item label="类型:">
                    <el-select clearable v-model="searchValues.sourceType" placeholder="全部" class="operator-select"
                               size="default">
                        <el-option label="短视频URL" :value="1"></el-option>
                        <el-option label="本地上传" :value="2"></el-option>
                        <el-option label="达人" :value="3"></el-option>
                        <el-option label="爆款" :value="4"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="操作人:">
                    <el-select clearable v-model="searchValues.operatorUserId" placeholder="全部" size="default"
                               class="operator-select">
                        <el-option v-for="item in operationUsers" :key="item.userId" :label="item.nickName"
                                   :value="item.userId"/>
                    </el-select>
                </el-form-item>

                <!-- <el-form-item label="提取状态:">
                    <el-select clearable v-model="searchValues.extractStatus" placeholder="全部" size="default"
                               class="operator-select">
                        <el-option label="待处理" :value="1"></el-option>
                        <el-option label="处理中" :value="2"></el-option>
                        <el-option label="提取完成" :value="3"></el-option>
                        <el-option label="提取失败" :value="4"></el-option>
                    </el-select>
                </el-form-item> -->
                <br>
                <el-form-item style="margin-top:12px">
                    <afp-button type="primary" size="default" plain @click="getPaperList">查找
                    </afp-button>
                    <afp-button type="primary" size="default" @click="handleExtractFromVideo">从视频提取文案
                    </afp-button>
                </el-form-item>
            </el-form>

            <div class="tips-row flex items-center">
                <span>本日剩余<span class="highlight font-bold">{{
                        userProperty.shortVideoNum
                    }}</span>次。我要</span>
                <el-link type="primary" @click="handleUpgrade">升级会员</el-link>
                <span>或</span>
                <el-link type="primary" @click="handleUpgrade">充值</el-link>
                增加文案提取次数
            </div>
        </el-card>

        <el-card class="history-section">
            <div class="flex items-center justify-between">
                <div class="section-title font-bold">提取历史</div>
                <el-button :disabled="selectionList.length===0" type="text" style="margin-right: 32px;color: red"
                           class="extract-btn-empty"
                           @click="delAction">批量删除
                </el-button>
            </div>

            <div class="history-list">
                <TableList :tableConfig="tableConfig" @paginationChange="paginationChange"
                           @selectionChange="selectionChange">
                    <template #videoTitle="{ row: item }">
                        <div class="video-content-cell cursor-pointer" @click="()=>openUrl(item)">
                            <div class="video-coverUrl">
                                <!--                                <ImageValidator :urls="[item.coverUrl]"/>-->
                                <el-image
                                    fit="contain"
                                    :src="item.coverUrl"
                                    class="video-thumbnail">
                                </el-image>
                            </div>
                            <div class="video-info flex justify-center flex-col">
                                <div class="video-title text-clamp2">{{ item.videoTitle }}</div>
                            </div>
                        </div>
                    </template>
                    <template #extractStatus="{ row: item }">
                        <div class="extract" v-if="item.extractStatus!==undefined">
                            <i v-if="item.extractStatus === 2" class="icon el-icon-loading"></i>
                            <i v-else-if="item.extractStatus === 3" class="icon el-icon-success"></i>
                            <i v-else-if="item.extractStatus === 4" class="icon el-icon-error"></i>
                            <i v-else class="icon el-icon-remove"></i>
                            <span>{{ extractStatus(item.extractStatus) }}</span>
                        </div>
                        <div class="extract" v-else>
                            <span>-</span>
                        </div>
                    </template>
                    <template #emptyText>
                        <afp-button type="primary" size="default" :plain="false" class="extract-btn-empty" @click="handleExtractFromVideo">点我提取文案
                        </afp-button>
                    </template>
                </TableList>
            </div>
        </el-card>

        <ExtractionDialog
            :visible.sync="dialogVisible"
            @extract-success="handleExtractSuccess"
        />
        <TextDialog :visible.sync="textVisible" :currentVideoItem="currentVideoItem" :isHasLike="false"/>
        <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>
    </div>
</template>

<script>
import tabs from '@/mixins/tabs'
import setTimeOut from '@/mixins/setTimeOut'
import ExtractionDialog from './extractionDialog.vue'
import TextDialog from './../component/textDialog.vue'
import TableList from "@/views/modules/shortVideo/component/tableList.vue"
import extract from './../mixins/index'
import userAssets from '../mixins/userAssets'
// import ImageValidator from './../component/imageValidator.vue'
import customerServiceQrCode from "@/views/commonComponent/customerServiceQrCode.vue";

export default {
    mixins: [tabs, setTimeOut, extract(), userAssets],
    components: {
        customerServiceQrCode,
        // ImageValidator,
        TableList,
        TextDialog,
        ExtractionDialog
    },
    data() {
        return {
            tabs: [{label: '提取文案', name: 'first'}],
            searchValues: {
                dateRange: [],
                videoTitle: '',
                sourceType: '',
                operatorUserId: '',
                extractStatus: ''
            },
            operationUsers: [],
            remainingTimes: 30, // 剩余次数
            dialogVisible: false, // 弹窗显示状态
            selectionList: [],
            kefuDialogVisible: false,
            tableConfig: {
                eHeight: 319,
                selection: true,
                column: [{
                    label: "视频",
                    prop: 'videoTitle',
                    option: {
                        width: '300',
                        align: 'left'
                    }
                }, {
                    label: "类型",
                    prop: 'sourceType',
                    option: {
                        render: (row) => {
                            const map = {
                                1: '视频链接',
                                2: '本地视频',
                                3: '达人视频',
                                4: '爆款视频'
                            }
                            return `<div>${map[row.sourceType] || '未知'}</div>`
                        }
                    }
                },
                    // {
                    //     label: "短视频链接",
                    //     prop: 'videoUrl',
                    //     option: {
                    //         align: 'left',
                    //         tooltip: true
                    //     }
                    // },
                    {
                        label: "操作人",
                        prop: 'nickName'
                    }, {
                        label: "提取时间",
                        prop: 'extractTime',
                        option: {
                            width: '175',
                        }
                    }, {
                        label: '提取状态',
                        prop: 'extractStatus',
                    }],
                pagination: {},
                tableData: []
            }
        }
    },
    computed: {
        extractStatus() {
            const STATUS_TYPE = new Map([
                [0, '未提取'],//未提取
                [1, '待处理'],//待处理
                [2, 'AI处理中'],
                [3, '提取成功'],
                [4, '提取失败'],
            ])
            return (status) => {
                return STATUS_TYPE.get(status) || '-'
            }
        },
    },
    mounted() {
        this.$nextTick(() => {
            this.getActionUsers()
            this.getPaperList()
        })
    },
    methods: {
        async paginationChange(pagination) {
            await this.getPaperList(pagination)
        },
        selectionChange(values) {
            this.selectionList = values
        },
        async getPaperList(pagination) {
            const [startTime, endTime] = this.searchValues?.dateRange || []
            try {
                const serverResult = await this.$httpBack.shortVideo.history({
                    page: pagination?.currentPage || 1,
                    limit: pagination?.pageSize || 10,
                    startTime: startTime ? `${startTime} 00:00:00` : '',
                    endTime: endTime ? `${endTime} 23:59:59` : '',
                    videoTitle: this.searchValues.videoTitle,
                    sourceType: this.searchValues.sourceType,
                    operatorUserId: this.searchValues.operatorUserId,
                    extractStatus: this.searchValues.extractStatus
                })
                if (serverResult.code !== 0) return
                await this.getUserProperty()
                const list = serverResult.data?.list || []
                list.forEach(_item => {
                    _item.selectable = !this.isAuthenticated(_item)
                })
                this.tableConfig = {
                    ...this.tableConfig,
                    tableData: list,
                    pagination: {
                        currentPage: serverResult.data?.currPage || 0,
                        total: serverResult.data?.totalCount || 0,
                        pageSize: serverResult.data?.pageSize || 10
                    }
                }
                this.$nextTick(() => {
                    this.scheduleNextPoll(10000, () => {
                        this.getPaperList(this.tableConfig?.pagination)
                    })
                })
            } catch (e) {
            }
        },
        extractAgain(item) {
            this.reExtract({
                id: item.extractId,
                sourceType: item.sourceType
            }, () => {
                this.getPaperList(this.tableConfig?.pagination)
            })
        },
        async getActionUsers() {
            try {
                const serverResult = await this.$httpBack.shortVideo.operationUsers()
                if (serverResult.code !== 0) return
                this.operationUsers = serverResult.data || []
            } catch (e) {
            }
        },
        async delAction() {
            try {
                const ids = this.selectionList.map(item => item.id)
                const serverResult = await this.$httpBack.shortVideo.batch({ids})
                if (serverResult.code !== 0) return this.$message.error(serverResult.msg)
                await this.getPaperList()
                this.$message.success('删除成功')
                await this.getActionUsers()
            } catch (e) {
            }
        },
        handleExtractFromVideo() {
            this.dialogVisible = true
        },
        handleExtractSuccess() {
            this.getPaperList()
            this.getActionUsers()
            this.remainingTimes -= 1
        },
        handleUpgrade() {
            this.kefuDialogVisible = true
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },
        openUrl(row) {
            if (row.sourceType === 2) return
            window.open(row.videoUrl, '_blank')
            // this.$httpClient.system.openUrl({url: row.videoUrl});
        },
    }
}
</script>

<style lang="scss" scoped>
.text-extraction-container {
    height: 100%;

    ::v-deep(.el-card__body) {
        padding: 0;
    }

    ::v-deep(.el-form-item) {
        margin-bottom: 0px;
    }

    .filter-container {
        padding: 16px 12px;

        .date-picker {
            width: 260px;
        }

        .video-name-input {
            width: 200px;
        }

        .operator-select {
            width: 150px;
        }

        .tips-row {
            padding: 10px 0 0 0;
            color: #606266;
            font-size: 14px;

            .highlight {
                color: #409EFF;
                margin: 0 3px;
            }

            .el-link {
                margin: 0 5px;
                font-size: 14px;
            }
        }
    }

    .history-section {
        flex: 1;
        margin-top: 10px;

        .section-title {
            position: relative;
            padding-left: 10px;
            font-size: 14px;
            color: #303133;
            margin: 12px;

            &:before {
                content: '';
                position: absolute;
                left: 0;
                top: 2px;
                width: 4px;
                height: 14px;
                background-color: #409EFF;
                border-radius: 2px;
            }
        }

        .history-list {
            .extract {
                .icon {
                    margin-inline: 3px;
                    font-size: 16px;
                }

                .el-icon-loading {
                    color: var(--color-main);
                }

                .el-icon-success {
                    color: #28BD6C;
                }

                .el-icon-error {
                    color: #FC4F52;
                }

                .el-icon-minus {
                    color: #4D4D4D;
                }
            }
        }
    }

    .video-coverUrl {
        flex-shrink: 0;
        width: 56px;
        text-align: center;
        height: 56px;

        .video-thumbnail {
            background: #DCDCDC;
            border-radius: 6px;
            overflow: hidden;
        }
    }
}
</style>

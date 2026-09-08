<template>
    <div style="height: 100%">
        <div class="user-video-management flex flex-col">
            <!-- 用户信息区域 -->
            <el-card>
                <div class="user-info-card flex items-center justify-between">
                    <div class="user-basic-info flex items-center user-border">
                        <div class="user-avatar flex items-center">
                            <el-avatar :size="64" :src="userInfo.avatar"></el-avatar>
                        </div>
                        <div class="user-details">
                            <h2 class="username">{{ userInfo.nickname }}</h2>
                            <p class="user-id">抖音号：{{ userInfo.platformAccount }}</p>
                        </div>
                    </div>
                    <div class="user-stats flex flex-wrap justify-center user-border">
                        <div class="stat-item">
                            <p class="stat-title">近期新增粉丝</p>
                            <p class="stat-value">
                                {{ fnw(userInfo.yesterdayFansCount, 2) }}
                            </p>
                        </div>
                        <div class="stat-item">
                            <p class="stat-title">3日发布</p>
                            <p class="stat-value">{{ fnw(userInfo.threeDaysVideoCount, 2) }}</p>
                        </div>
                        <div class="stat-item">
                            <p class="stat-title">粉丝数</p>
                            <p class="stat-value">{{ fnw(userInfo.followersCount, 2) }}</p>
                        </div>
                        <div class="stat-item">
                            <p class="stat-title">作品数</p>
                            <p class="stat-value">{{ fnw(userInfo.videoCount, 2) }}</p>
                        </div>
                        <div class="stat-item">
                            <p class="stat-title">获赞数</p>
                            <p class="stat-value">{{ fnw(userInfo.likeCount, 2) }}</p>
                        </div>
                    </div>
                    <div class="user-profile">
                        <div class="font-bold">达人简介</div>
                        <p class="user-intro text-clamp2">
                            {{ userInfo.description ? userInfo.description : '这个人很懒，暂时没有简介' }}
                        </p>
                    </div>
                </div>
            </el-card>

            <!-- 视频列表区域 -->
            <el-card class="video-list-section">
                <div class="section-header flex items-center justify-between">
                    <div class="section-title flex items-center">
                        <div class="section-header-title">达人视频
                            <el-tooltip class="item" effect="dark" content="采集视频数据为该达人近半年发布的视频"
                                        placement="top">
                                <i class="el-icon-warning-outline"/>
                            </el-tooltip>
                        </div>
                        <span class="section-subtitle">
                            提取文案剩余条数：{{ userProperty?.shortVideoNum }}/{{ userProperty?.totalShortVideoNum }}
                        </span>
                        <afp-button type="primary" :plain="false" size="medium" v-if="isIgnore"
                                    style="margin-left: 12px"
                                    @click="updateExpert">手动更新
                        </afp-button>
                    </div>
                    <afp-button type="primary" :plain="false" :disabled="!selectionChangeList.length" @click="batchCreateExtract">批量提取文案</afp-button>
                </div>
                <TableList :tableConfig="tableConfig" @sortChange="sortChange" @selectionChange="selectionChange"
                           @paginationChange="paginationChange">
                    <template #title="{ row: item }">
                        <div class="video-content-cell cursor-pointer"
                             @click="()=>toVideoUrl(item)"
                             @mouseenter="showPopover($event, item)"
                             @mouseleave="hidePopover">
                            <div class="video-coverUrl">
                                <el-image
                                    :src="item.coverUrl"
                                    fit="contain"
                                    class="video-thumbnail">
                                </el-image>
                            </div>
                            <div class="video-info flex justify-between flex-col">
                                <div class="video-title text-clamp2 line-clamp2">{{ item.title }}</div>
                                <div class="video-date">
                                    <!--                                    <i class="el-icon-date"></i>-->
                                    {{ item.publishTime }}
                                    <i class="el-icon-time" style="margin-left: 10px;"></i> {{ minutes(item.duration) }}
                                </div>
                            </div>
                        </div>
                    </template>
                    <template
                        v-for="key in ['likeCountIncrements','commentCountIncrements','commentLikeRatios','shareCountIncrements','collectCountIncrements']"
                        v-slot:[key]="{ row: item }">
                        <div v-for="(_item,index) in item[key]" :class="{'font-bold':true,'text-xs':index !== 0}"
                             :style="{color:index === 0?'#000':'#909399'}">
                            <span v-if="item[key].length>1">{{ index === 0 ? '今日' : '上次' }} ：</span>
                            <span v-if="key !== 'commentLikeRatios'">
                                {{ _item.number !== null ? fnw(_item.number, 2) : '-' }}
                            </span>
                            <span v-else>
                                {{ _item.ratio !== null ? _item.ratio : '-' }}
                            </span>
                        </div>
                    </template>
                </TableList>
            </el-card>
            <TextDialog :visible.sync="textVisible" :currentVideoItem="currentVideoItem" :isHasLike="true"/>
            <el-popover
                v-model="popoverVisible"
                trigger="click"
                append-to-body
                popper-class="table-popover"
                :style="popoverStyle"
            >
                <div v-if="currentPopoverItem" @mouseenter="enterPopover" @mouseleave="leavePopover">
                    <PopoverInfo :currentPopoverItem="currentPopoverItem"/>
                </div>
            </el-popover>
        </div>
    </div>
</template>

<script>
import TableList from '../../component/tableList.vue'
import TextDialog from '../../component/textDialog.vue'
import PopoverInfo from '../../component/popoverInfo.vue'
import setTimeOut from "@/mixins/setTimeOut";
import extract from './../../mixins/index'
import myUtils from "@/utils/utils";
import {pick} from 'lodash'
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";
import popover from "@/views/modules/shortVideo/mixins/popover";
import {AI_WORKBENCH_PANELS} from '@/utils/aiAgentRoute'

export default {
    components: {TableList, TextDialog, PopoverInfo},
    mixins: [setTimeOut, extract('notCopy'), userAssets, popover],
    data() {
        return {
            userInfo: {},
            sortSequence: 0,//排序顺序 0：降序 1：升序
            sortCode: 1,//排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序 6评赞比
            selectionChangeList: [],
            tableConfig: {
                eHeight: 285,
                selection: true,
                column: [{
                    label: "视频内容",
                    prop: 'title',
                    option: {
                        align: 'left'
                    }
                }, {
                    label: "点赞数",
                    prop: 'likeCountIncrements',
                    sortable: 'custom',
                    option: {
                        width: 150,
                        align: 'left'
                    }
                }, {
                    label: "评论数",
                    prop: 'commentCountIncrements',
                    sortable: 'custom',
                    option: {
                        width: 150,
                        align: 'left'
                    }
                }, {
                    label: "评赞比",
                    prop: 'commentLikeRatios',
                    sortable: 'custom',
                    option: {
                        width: 150,
                        align: 'left'
                    }
                }, {
                    label: '转发数',
                    prop: 'shareCountIncrements',
                    sortable: 'custom',
                    option: {
                        width: 150,
                        align: 'left'
                    }
                }, {
                    label: "收藏数",
                    prop: 'collectCountIncrements',
                    sortable: 'custom',
                    option: {
                        width: 150,
                        align: 'left'
                    }
                }],
                options: [{
                    label: 'AI拆解分析',
                    type: 'text',
                    plain: false,
                    className: 'agent-analysis-action',
                    hidden: () => {
                        return false
                    },
                    click: (item) => {
                        this.openAgentAnalysis(item)
                    }
                }],
                pagination: {},
                tableData: []
            },
        };
    },
    computed: {
        fnw() {
            return (num, length) => {
                return myUtils.fnw(num, length, true)
            }
        },
        minutes() {
            return (value) => {
                return myUtils.toformatTimeMM_ssChinse(value * 1000)
            }
        },
        isIgnore() {
            const {query: param} = this.$route
            return param.isIgnore == 1
        },
    },
    mounted() {
        this.$nextTick(() => {
            this.initData()
        })
    },
    methods: {
        async initData() {
            await this.getExpertDetail()
            await this.getExpertVideoList({})
        },
        toVideoUrl(item) {
            if (!item.videoUrl) return this.$message.error('视频地址异常')
            window.open(item.videoUrl, '_blank')
            // this.$httpClient.system.openUrl({url: item.videoUrl});
        },
        extractAgain(item) {
            this.reExtract({
                id: item.extractId,
                sourceType: item.sourceType || 3
            }, () => {
                this.getExpertVideoList({...this.tableConfig?.pagination})
            })
        },
        async paginationChange(pagination) {
            await this.getExpertVideoList(pagination)
        },
        async getExpertDetail() {
            const {query: param} = this.$route
            const serverResult = await this.$httpBack.shortVideo.expertDetail({...pick(param, ['platformType', 'platformUserId']),})
            if (serverResult.code !== 0) return this.$message.error(serverResult.msg)
            this.userInfo = serverResult.data || {}
        },
        async getExpertVideoList({currentPage = 1, pageSize = 10}) {
            const {query: param} = this.$route
            const serverResult = await this.$httpBack.shortVideo.detailVideos({
                page: currentPage,
                limit: pageSize,
                ...pick(param, ['platformType', 'platformUserId']),
                videoPublishTimeType: param.time,
                sortCode: this.sortCode,
                sortSequence: this.sortSequence
            })
            if (serverResult.code !== 0) return this.$message.error(serverResult.msg)
            const list = serverResult?.data?.list || []
            list.forEach(_item => {
                _item.selectable = _item.extractStatus === 0
            })
            this.tableConfig = {
                ...this.tableConfig,
                tableData: list,
                pagination: {
                    currentPage: serverResult?.data?.currPage || 0,
                    total: serverResult?.data?.totalCount || 0,
                    pageSize: serverResult?.data?.pageSize || 10
                }
            }
            this.$nextTick(() => {
                this.scheduleNextPoll(10000, () => {
                    this.getExpertVideoList({...this.tableConfig?.pagination})
                })
            })
        },
        selectionChange(list) {
            this.selectionChangeList = list
        },
        sortChange({column, prop, order}) {
            const sortSequence = order === 'ascending' ? 1 : 0
            let sortCode = 1
            if (prop === 'likeCountIncrements') {
                sortCode = 2
            } else if (prop === 'commentCountIncrements') {
                sortCode = 3
            } else if (prop === 'shareCountIncrements') {
                sortCode = 4
            } else if (prop === 'collectCountIncrements') {
                sortCode = 5
            } else if (prop === 'commentLikeRatios') {
                sortCode = 6
            }
            if (!order) sortCode = 1
            this.sortCode = sortCode
            this.sortSequence = sortSequence
            this.$nextTick(() => {
                this.getExpertVideoList({})
            })
        },
        async updateExpert() {
            const clientResult = await this.$httpClient.shortVideo.syncInfluencerVideo({
                platformUserId: this.userInfo.platformUserId,
                platformAccount: this.userInfo.platformAccount,
                platformType: this.userInfo.platformType,
                influencerId: this.userInfo.id,
                actionType: 2
            })
            if (clientResult.code !== 0) return this.$message.error(clientResult.msg)
            await this.getExpertVideoList({})
            this.$message.success('达人数据更新成功')
        },
        async batchCreateExtract() {
            await this.createExtract({
                sourceType: 3,
                sourceId: this.userInfo.id,
            })
            await this.getExpertVideoList({...this.tableConfig?.pagination})
        },
        openAgentAnalysis(item) {
            const influencerId = String(this.userInfo.id || this.userInfo.influencerId || '').trim()
            const clipId = String(item.clipId || item.id || item.videoId || '').trim()
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb({
                    panel: AI_WORKBENCH_PANELS.INFLUENCER,
                    influencerId,
                    clipId
                })
                return
            }
            this.$router.push({
                path: '/aiAgent',
                query: {
                    panel: AI_WORKBENCH_PANELS.INFLUENCER,
                    influencerId,
                    clipId
                }
            })
        },
    }
};
</script>

<style lang="scss" scoped>
.user-video-management {
    height: 100%;

    ::v-deep(.el-card__body) {
        padding: 0;
    }

    ::v-deep(.table-popover) {
        position: fixed !important;
        pointer-events: auto;
        border-radius: 10px;
    }

    .video-content-cell {
        display: flex;
        cursor: pointer;
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

    .user-info-card {
        padding: 18px 12px;
    }

    .user-basic-info {
        width: 300px;

        .user-avatar {
            margin-right: 15px;
        }

        .user-details {
            flex: 1;

            .username {
                font-size: 18px;
                font-weight: bold;
                margin: 0;
                padding: 4px 0;
            }

            .user-id {
                font-size: 14px;
                color: #606266;
                margin: 0;
                padding: 4px 0;
            }
        }
    }

    .user-stats {
        width: 550px;

        .stat-item {
            padding: 0px 20px;
            text-align: center;

            .stat-title {
                font-size: 14px;
                color: #909399;
                margin: 0 0 5px;
            }

            .stat-value {
                font-size: 16px;
                font-weight: bold;
                color: #303133;
                margin: 0;
            }
        }
    }

    .user-profile {
        flex: 1;
        padding-left: 30px;

        .user-intro {
            font-size: 14px;
            color: #606266;
            margin: 0;
            line-height: 1.5;
        }
    }

    .user-border {
        position: relative;

        &::after {
            content: ' ';
            position: absolute;
            right: 0;
            top: 0;
            width: 1px;
            height: 100%;
            background: #DCDCDC;
        }
    }

    .video-list-section {
        flex: 1;
        margin-top: 12px;

        .section-header {
            padding: 12px;

            .section-title {
                font-size: 16px;
                font-weight: bold;

                .section-header-title {
                    position: relative;
                    padding-left: 10px;

                    &:before {
                        content: ' ';
                        position: absolute;
                        left: 0;
                        top: 2px;
                        width: 4px;
                        height: 16px;
                        background: #05D0FF;
                        border-radius: 42px;
                    }
                }

                i {
                    color: #409EFF;
                    margin-right: 5px;
                }

                .section-subtitle {
                    font-size: 14px;
                    font-weight: normal;
                    color: #151719;
                    margin-left: 15px;
                }
            }
        }
    }


    .extract-text-btn {
        padding: 5px 10px;
    }

    ::v-deep .el-table {
        border-radius: 4px;

        .cell {
            font-size: 14px;
        }

        .el-table-column--selection .cell {
            padding-right: 0;
            padding-left: 10px;
        }

        .el-table__header-wrapper {
            th {
                background-color: #F8FCFF;
                color: #606266;
            }
        }
    }

    ::v-deep(.agent-analysis-action) {
        padding: 0;
        font-size: 14px;
        font-weight: 500;
    }
}
</style>

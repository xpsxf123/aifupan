<template>
    <div class="subscribe-expert-list-container">
        <div class="subscribe-expert-list flex flex-col">
            <!-- 筛选条件区域 -->
            <el-card class="filter-container">
                <div class="filter-area flex items-center flex-wrap">
                    <div class="filter-item flex items-center" style="padding-right: 20px">
                        <div style="padding-right: 20px">列表达人数：{{ getAddedExpertCount }}</div>
                        <afp-button type="primary" plain class="extract-btn" size="default" @click="addExpert">
                            添加达人
                        </afp-button>
                    </div>

                    <el-form :inline="true" :model="searchData" class="demo-form-inline" size="default">
                        <el-form-item label="搜索达人:">
                            <el-input
                                size="default"
                                v-model="searchData.nickname"
                                placeholder="请输入"
                                class="video-name-input input-gray input-border-none"
                            ></el-input>
                        </el-form-item>
                        <el-form-item label="行业筛选:">
                            <tradeId v-model="searchData.industryId" v-removeAriaHidden :clearable="true"
                                     :options="tradeTreeList"></tradeId>
                        </el-form-item>
                        <el-form-item>
                            <afp-button type="primary" plain size="default" class="search-btn"
                                        @click="getSubscriptionsExpertList">
                                查 找
                            </afp-button>
                        </el-form-item>
                    </el-form>
                </div>

                <div class="tips-row flex items-center justify-start">
                    <span>本账号订阅数(剩余/总)：{{
                            userProperty.userSubscribeInfluencerNum || 0
                        }}/{{ userProperty?.totalUserSubscribeInfluencerNum || 0 }}个<span
                            style="padding-inline:6px"/>共享订阅数：{{
                            userProperty.subscribeInfluencerNum || 0
                        }}/{{ userProperty.totalSubscribeInfluencerNum || 0 }}个</span>
                </div>
            </el-card>
            <!-- 提取历史 -->
            <div class="history-section">
                <el-card class="history-card"
                         :style="{minHeight:coreHeight}"
                         v-for="item in subscriptionsExpertList" :key="item.groupId">
                    <div class="section-title">{{ item.groupName }}</div>
                    <!-- 历史列表 -->
                    <div class="history-list">
                        <TableList :tableConfig="{...tableConfig,tableData:item.influencers||[]}">
                            <template #expert="{ row: _item }">
                                <div class="expert-content-cell flex items-center cursor-pointer"
                                     @click="()=>toExpertHomeForThird(_item)">
                                    <el-avatar :src="_item.avatar"/>
                                    <div style="flex: 1">
                                        <div class="name">{{ _item.nickname }}</div>
                                    </div>
                                </div>
                            </template>
                            <template #videoCount="{ row: _item }">
                                <div style="color:#0077FF;cursor: pointer" @click="()=>toExpertHome(_item)">
                                    {{ _item.videoCount }}
                                </div>
                            </template>
                            <template #autoSyncEnabled="{ row: _item }">
                                <el-switch
                                    :disabled="isAuthenticated(_item)"
                                    v-model="_item.autoSyncEnabled"
                                    @change="(status)=>switchChange(_item,status)"
                                    active-color="#13ce66">
                                </el-switch>
                            </template>
                            <template #todayIncrement="{ row: _item }">
                                <div class="cursor-pointer" style="color:#0077FF;" @click="()=>toExpertHome(_item,1)">
                                    {{ _item.todayIncrement }}
                                </div>
                                <div>
                                    <span class="text-xs">{{ _item.todaySyncTime?.split(' ')?.[1] }}</span>
                                    <span class="text-xs" v-if="_item.todaySyncTime">更新</span>
                                </div>
                            </template>
                            <template #threeDayIncrement="{ row: _item }">
                                <div class="cursor-pointer" style="color:#0077FF;" @click="()=>toExpertHome(_item,2)">
                                    {{ _item.threeDayIncrement }}
                                </div>
                            </template>
                            <template #emptyText>
                                <div class="empty-text">暂无达人</div>
                                <afp-button type="primary" size="default" :plain="false" @click="addExpert">点我添加达人
                                </afp-button>

                                <div class="text-sm flex items-center" style="margin-top: 12px"
                                     @click="checkExampleExpertVideoList">
                                    <img src="@/assets/imgs/rightgif.gif" style="max-width: 24px;" alt="" srcset="">
                                    <span class="cursor-pointer" style="color: var(--color-main)">点我查看</span>
                                    <span style="color: #000">订阅达人示例</span>
                                </div>
                            </template>
                        </TableList>
                    </div>
                </el-card>
            </div>
        </div>
        <EditExpert
            :autoSync="autoSync"
            :visible.sync="editExpertVisible"
            @changeExpert="changeExpert"
            @changeRules="getSubscriptionsExpertList"
            :editExpertDetail="editExpertDetail"/>
    </div>
</template>

<script>
import TableList from "./../../component/tableList.vue";
import EditExpert from './editExpert.vue'
import tradeId from "@/components/tradeId/index.vue";
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";
import {AI_WORKBENCH_PANELS} from '@/utils/aiAgentRoute'

export default {
    mixins: [userAssets],
    props: {
        selectGroupId: {
            type: String,
            default: ''
        },
        pendingAutoSyncExpert: {
            type: Object,
            default: null
        }
    },
    watch: {
        selectGroupId: {
            handler() {
                this.getSubscriptionsExpertList()
            }
        },
        pendingAutoSyncExpert: {
            handler(val) {
                if (!val) return
                this.$nextTick(() => {
                    this.openAutoSyncForNewExpert(val)
                })
            },
            deep: true
        },
        editExpertVisible: {
            handler(val) {
                if (!val) this.editExpertDetail = {}
            },
            deep: true
        }
    },
    components: {
        tradeId,
        TableList,
        EditExpert
    },
    data() {
        return {
            searchData: {
                nickname: '',
                industryId: '',
            },
            autoSync: false,
            tradeTreeList: [],
            editExpertVisible: false,
            editExpertDetail: {},
            subscriptionsExpertList: [],
            tableConfig: {
                column: [{
                    label: "达人",
                    prop: 'expert',
                    option: {
                        width: '200',
                        align: 'left'
                    }
                }, {
                    label: "行业",
                    prop: 'industry'
                }, {
                    label: "自动提取文案",
                    prop: 'autoSyncEnabled',
                    option: {
                        width: '120',
                        align: 'center'
                    }
                }, {
                    label: "获取视频数",
                    prop: 'videoCount',
                }, {
                    label: '今日发布',
                    prop: 'todayIncrement',
                    option: {
                        width: '160',
                        align: 'center'
                    }
                }, {
                    label: "3日发布",
                    prop: 'threeDayIncrement',
                    option: {
                        width: '160',
                        align: 'center'
                    }
                }, {
                    label: '添加账号',
                    prop: 'operator',
                }],
                options: [{
                    label: '查看列表',
                    hidden: () => {
                        return false
                    },
                    click: (item) => {
                        this.toExpertHome(item)
                    }
                }, {
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
                }, [{
                    label: '手动更新',
                    hidden: () => {
                        return false
                    },
                    disabled: (item) => {
                        const {id} = this.$store.getters.getUserInfo;
                        return id !== item?.userId
                    },
                    click: (item) => {
                        this.updateExpert(item)
                    }
                }, {
                    label: '编辑',
                    hidden: (item) => {
                        return this.isAuthenticated(item)
                    },
                    click: (item) => {
                        this.autoSync = false
                        this.editExpertDetail = item
                        this.editExpertVisible = true
                    }
                }, {
                    label: '删除',
                    hidden: (item) => {
                        return this.isAuthenticated(item)
                    },
                    click: (item) => {
                        this.$confirm('确认删除后将无法监控达人视频更新情况，确认要删除吗？', '友情提示', {
                            confirmButtonText: '确定删除',
                            cancelButtonText: '再想想',
                            type: 'warning'
                        }).then(() => {
                            this.delExpert(item.subscriptionId)
                        }).catch(() => {
                        })
                    }
                }]],
                tableData: []
            }
        }
    },
    computed: {
        getAddedExpertCount() {
            return this.subscriptionsExpertList.length ? this.subscriptionsExpertList?.reduce((acc, cur) => acc + cur?.influencerCount || 0, 0) : 0
        },
        coreHeight() {
            return this.subscriptionsExpertList?.filter(item => item.groupId)?.length ? 'auto' : '100%'
        }
    },
    mounted() {
        this.getTradeListTree()
        this.getSubscriptionsExpertList()
    },
    methods: {
        toExpertHome(item, time) {
            const {id} = this.$store.getters.getUserInfo;
            const isIgnore = id !== item?.userId ? 0 : 1
            this.$router.push({
                path: '/subscribeExpert/expertDetail',
                query: {
                    time,
                    platformType: item.platformType,
                    platformUserId: item.platformUserId,
                    isIgnore
                }
            })
        },
        checkExampleExpertVideoList() {
            this.$router.push({
                path: '/subscribeExpert/expertDetail',
                query: {
                    platformType: 1,
                    platformUserId: 'MS4wLjABAAAAW6_zMAHHOk5M73ZWwg3oHRmIQ5QNLxahZ42soKDjN-4',
                    isIgnore: 0
                }
            })
        },
        toExpertHomeForThird(item) {
            if (!item.platformUserId) return this.$message.error('达人主页地址异常')
            window.open(`https://www.douyin.com/user/${item.platformUserId}`, '_blank')
            // this.$httpClient.system.openUrl({url: `https://www.douyin.com/user/${item.platformUserId}`});
        },
        getTradeListTree() {
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
            })
        },
        async getSubscriptionsExpertList() {
            const result = await this.$httpBack.shortVideo.subscriptions(this.searchData)
            if (result.code !== 0) return
            const list = result.data || []
            if (list.length === 0) {
                list.push({
                    groupName: '默认分组',
                    groupId: '',
                    influencers: []
                })
            }
            this.subscriptionsExpertList = !!this.selectGroupId ? list.filter(_item => _item.groupId === this.selectGroupId) : list
            await this.getUserProperty()
        },
        addExpert() {
            this.$emit('addExpert', true, this.getSubscriptionsExpertList)
        },
        /**
         * @description 添加达人成功后自动弹出"开启自动提取文案"配置弹窗，与手动点击开关逻辑一致。
         * @param {Object} expertData 添加达人接口返回的订阅数据
         * @returns {void}
         */
        openAutoSyncForNewExpert(expertData) {
            const resultData = expertData?.resultData || {}
            const keyword = expertData?.searchKeyword || ''
            let subscriptionId = resultData?.subscriptionId || resultData?.id || ''
            let expertNickname = ''
            if (!subscriptionId) {
                const list = this.subscriptionsExpertList || []
                const lowerKeyword = keyword.toLowerCase()
                for (const group of list) {
                    const influencers = group?.influencers || []
                    const found = influencers.find(item =>
                        (lowerKeyword && (
                            String(item?.platformAccount || '').toLowerCase() === lowerKeyword ||
                            String(item?.nickname || '') === keyword
                        )) ||
                        (
                            String(item?.groupId || '') === String(expertData?.groupId || '') &&
                            String(item?.industryId || '') === String(expertData?.industryId || '')
                        )
                    )
                    if (found?.subscriptionId) {
                        subscriptionId = found.subscriptionId
                        expertNickname = found.nickname || found.platformAccount || ''
                        break
                    }
                }
            }
            this.autoSync = true
            this.editExpertDetail = {
                subscriptionId: subscriptionId,
                groupId: expertData?.groupId || '',
                industryId: expertData?.industryId || '',
                autoSyncEnabled: 0,
                likeCountThreshold: undefined,
                updateTimeCondition: '',
                nickname: expertNickname
            }
            this.editExpertVisible = true
        },
        async switchChange(item, status) {
            if (status) {
                this.autoSync = true
                this.editExpertDetail = item
                this.editExpertVisible = true
            } else {
                await this.changeExpert({
                    subscriptionId: item.subscriptionId,
                    groupId: item.groupId,
                    industryId: item.industryId,
                    autoSyncEnabled: 0,
                    updateTimeCondition: '',
                    likeCountThreshold: ''
                })
            }
        },
        async changeExpert(editForm) {
            try {
                const submitData = {
                    subscriptionId: this.editExpertDetail?.subscriptionId,
                    ...editForm
                }
                if (this.autoSync) {
                    submitData.autoSyncEnabled = 1
                }
                const result = await this.$httpBack.shortVideo.subscriptionEdit(submitData)
                if (result.code !== 0) return
                this.$message.success('达人编辑成功');
            } catch (e) {
            }
            await this.getSubscriptionsExpertList()
        },
        delExpert(subscriptionId) {
            this.$httpBack.shortVideo.subscriptionDel(subscriptionId).then(res => {
                if (res.code !== 0) return
                this.getSubscriptionsExpertList()
                this.$message({
                    type: 'success',
                    message: '删除成功!'
                });
            })
        },
        async updateExpert(items) {
            const clientResult = await this.$httpClient.shortVideo.syncInfluencerVideo({
                platformUserId: items.platformUserId,
                platformAccount: items.platformAccount,
                platformType: items.platformType,
                influencerId: items.influencerId,
                actionType: 2
            })
            if (clientResult.code !== 0) return this.$message.error(clientResult.msg)
            await this.getSubscriptionsExpertList()
            this.$message.success('达人数据更新成功')
        },
        openAgentAnalysis(item) {
            const influencerId = String(item.influencerId || item.id || '').trim()
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb({
                    panel: AI_WORKBENCH_PANELS.INFLUENCER,
                    influencerId
                })
                return
            }
            this.$router.push({
                path: '/aiAgent',
                query: {
                    panel: AI_WORKBENCH_PANELS.INFLUENCER,
                    influencerId
                }
            })
        },
    }
}
</script>

<style lang="scss" scoped>
.subscribe-expert-list-container {
    height: 100%;

    .subscribe-expert-list {
        height: 100%;

        ::v-deep(.el-form-item) {
            margin-bottom: 0;
        }

        ::v-deep(.el-card__body) {
            padding: 0;
        }

        .filter-container {
            padding: 6px 12px;
            flex-shrink: 0;

            .filter-area {
                padding: 12px 0;

                .filter-item {
                    margin-right: 20px;

                    .filter-label {
                        color: #606266;
                        font-size: 14px;
                        white-space: nowrap;
                    }

                    .date-picker {
                        width: 280px;
                    }

                    .video-name-input {
                        width: 220px;
                    }

                    .industry-select {
                        width: 150px;
                    }
                }

                .search-btn {
                    margin-right: 10px;
                }
            }

            .tips-row {
                padding: 12px 0;
                color: #606266;
                font-size: 14px;

                .highlight {
                    color: #409EFF;
                    font-weight: bold;
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
            overflow: auto;
            margin-top: 10px;

            .history-card {
                margin-top: 10px;

                .section-title {
                    position: relative;
                    padding-left: 10px;
                    font-size: 14px;
                    font-weight: bold;
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

                        .el-icon-success {
                            color: #28BD6C;
                        }

                        .el-icon-error {
                            color: #FC4F52;
                        }
                    }

                    .empty-text {
                        color: #484A4D;
                    }
                }

                &:first-child {
                    margin-top: 0;
                }
            }
        }

        .expert-content-cell {

            .name {
                padding-left: 8px;
            }
        }
    }
}

::v-deep(.agent-analysis-action) {
    padding: 0;
    font-size: 14px;
    font-weight: 500;
}
</style>

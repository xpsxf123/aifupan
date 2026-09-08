<template>
    <div style="height: 100%">
        <div class="subscribeHotItem flex flex-col">
            <el-card class="subscribe-container">
                <div class="subscribe-area flex flex-wrap items-center justify-between">
                    <div class="flex items-center">
                        <afp-button type="primary" plain class="extract-btn" size="default" @click="addSubscribe">
                            添加订阅
                        </afp-button>
                        <div class="text-xs cursor-pointer" style="margin-left: 12px;color: var(--color-main)"
                             @click="openUrlToDouYin">打开抖音
                        </div>
                        <div class="text-xs cursor-pointer" style="margin-left: 12px;color: var(--color-main)"
                             @click="openUrlToDouBao">打开豆包
                        </div>
                    </div>

                    <span>
                    <i class="el-icon-warning-outline cursor-pointer" @click="tipsIcon" style="color: red"></i>
                    本账号订阅数(剩余/总)：{{ userProperty?.userSubscribeHotVideoNum || 0 }}/{{
                            userProperty?.totalUserSubscribeHotVideoNum || 0
                        }}个
                    <span style="padding-inline:6px"/>
                    共享订阅数{{
                            userProperty?.subscribeHotVideoNum || 0
                        }}/{{ userProperty?.totalSubscribeHotVideoNum || 0 }}个
                </span>
                </div>
            </el-card>
            <div class="history-section-container">
                <el-card class="history-section"
                         :style="{minHeight:coreHeight}"
                         v-for="item in subscribeHotItemList" :key="item.groupId">
                    <div class="section-title flex">
                        <span class="font-bold">{{ item.groupName }}</span>
                        <div class="flex items-center text-sm" style="margin-left: 24px;color: var(--color-main)">
                            <span>哪些关键词搜索量大？</span>
                            <div class="flex items-center cursor-pointer" @click="openUrl">
                                <span>点我了解</span>
                                <img class="arrow" src="@/assets/imgs/rightgif.gif" alt="">
                            </div>
                        </div>
                    </div>
                    <!-- 历史列表 -->
                    <div class="history-list">
                        <TableList :tableConfig="{...tableConfig,tableData:item.hotSubscriptionItemVos||[]}">
                            <template #keyword="{ row: item }">
                                <el-tag type="success" style="font-size: 15px;max-width: 158px">
                                    <div class="text-clamp1">{{ item.keyword }}</div>
                                </el-tag>
                            </template>
                            <template #videoCount="{ row: item }">
                                <div style="color:var(--color-main);cursor: pointer" @click="()=>toExpertHome(item)">
                                    {{ item.videoCount }}
                                </div>
                            </template>
                            <template #autoSyncEnabled="{ row: item }">
                                <el-switch
                                    :active-value="1" :inactive-value="0"
                                    :disabled="isAuthenticated(item)"
                                    v-model="item.autoSyncEnabled"
                                    @change="(status)=>switchChange(item,status)"
                                    active-color="#13ce66">
                                </el-switch>
                            </template>
                            <template #todayIncrement="{ row: item }">
                                <div class="cursor-pointer" style="color:#0077FF;" @click="()=>toExpertHome(item,1)">
                                    {{ item.todayIncrement }}
                                </div>
                                <div class="text-xs" v-if="item.todaySyncTime">
                                    {{ item.todaySyncTime?.split(' ')?.[1] }} 更新
                                </div>
                            </template>
                            <template #threeDayIncrement="{ row: item }">
                                <div class="cursor-pointer" style="color:#0077FF;" @click="()=>toExpertHome(item,2)">
                                    {{ item.threeDayIncrement }}
                                </div>
                            </template>
                            <template #emptyText>
                                <div class="empty-text">
                                    <div>24小时自动帮您在全平台搜索与关键词有关的视频</div>
                                    <div>添加关键词注意以下要点</div>
                                    <div>1、用户搜索什么您就输入什么</div>
                                    <div>2、尽量搜索简短一些的词</div>
                                    <div>3、爱复盘开启才能自动搜索</div>
                                </div>
                                <div class="text-sm flex items-center" style="margin-top: 32px" @click="checkExample">
                                    <img src="@/assets/imgs/rightgif.gif" style="max-width: 24px;" alt="" srcset="">
                                    <span class="cursor-pointer" style="color: var(--color-main)">点我查看</span>
                                    <span style="color: #000">订阅爆款示例</span>
                                </div>
                            </template>
                        </TableList>
                    </div>
                </el-card>
            </div>
        </div>
        <TipMessageBox ref="tipMessage" @confirmEvent="confirmEvent" @cancelEvent="cancelEvent"/>
    </div>
</template>

<script>
import TableList from "./../../component/tableList.vue";
import TipMessageBox from '../../component/tipMessageBox.vue'
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";
import {isEmpty, pick} from "lodash";

export default {
    mixins: [userAssets],
    props: {
        selectGroupId: {
            type: String,
            default: ''
        }
    },
    components: {
        TableList, TipMessageBox
    },
    data() {
        return {
            subscribeHotItemList: [],
            currentItem: {},
            tableConfig: {
                pagination: false,
                column: [{
                    label: "关键字",
                    prop: 'keyword',
                    option: {
                        width: '180',
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
                    label: "满足要求条数",
                    prop: 'videoCount',
                }, {
                    label: '今日总更新',
                    prop: 'todayIncrement',
                    option: {
                        width: '160',
                        align: 'center'
                    }
                }, {
                    label: "3日总更新",
                    prop: 'threeDayIncrement',
                    option: {
                        width: '160',
                        align: 'center'
                    }
                }, {
                    label: '创建人',
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
                }, [{
                    label: '手动更新',
                    hidden: () => {
                        return false
                    },
                    disabled: (item) => {
                        const {id} = this.$store.getters.getUserInfo;
                        return id !== item?.userId || item.subscriptionId === this.currentItem.subscriptionId
                    },
                    click: (item) => {
                        // this.toExpertHome(item)
                        this.manualUpdate(item)
                    }
                }, {
                    label: '编辑',
                    hidden: (item) => {
                        return this.isAuthenticated(item)
                    },
                    click: (item) => {
                        this.$emit('addSubscribe', item, this.getSubscriptionHotList)
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
                            this.delSubscribe(item.subscriptionId)
                        }).catch(() => {
                        })
                    }
                }]],
                tableData: [],
            }
        }
    },
    watch: {},
    computed: {
        coreHeight() {
            return this.subscribeHotItemList?.filter(item => item.groupId)?.length ? 'auto' : '100%'
        }
    },
    created() {
    },
    methods: {
        toExpertHome(item, time) {
            const {id} = this.$store.getters.getUserInfo;
            const isIgnore = id !== item?.userId ? 0 : 1
            this.$router.push({
                path: '/subscribeHotItem/hotItemList',
                query: {
                    subscriptionId: item.subscriptionId,
                    subscriptionLikeCountThreshold: time ? undefined : item.subscriptionLikeCountThreshold,
                    keyWords: encodeURIComponent(item.keyword || ''),
                    time: time,
                    isIgnore
                }
            })
        },
        checkExample() {
            this.$router.push({
                path: '/subscribeHotItem/hotItemList',
                query: {
                    keyWords: encodeURIComponent('减肥'),
                    isIgnore: 0,
                    isExample: 1
                }
            })
        },
        async getSubscriptionHotList() {
            const result = await this.$httpBack.shortVideo.subscriptionHot()
            if (result.code !== 0) return
            const list = result.data || []
            if (list.length === 0) {
                list.push({
                    groupName: '默认分组',
                    groupId: '',
                    influencers: []
                })
            }

            this.subscribeHotItemList = !!this.selectGroupId ? list.filter(_item => _item.groupId === this.selectGroupId) : list
            await this.getUserProperty()
        },
        refreshData() {
            this.getSubscriptionHotList()
        },
        addSubscribe() {
            this.$emit('addSubscribe', {}, this.getSubscriptionHotList)
        },
        async delSubscribe(subscriptionId) {
            const result = await this.$httpBack.shortVideo.delSubscriptionHot(subscriptionId)
            if (result.code !== 0) return
            await this.getSubscriptionHotList()
            this.$message.success('删除成功')
        },
        async switchChange(item, status) {
            if (status) {
                this.$emit('getSubscribeData', item, this.getSubscriptionHotList)
            } else {
                this.$emit('handleSubscribe', true, {
                    ...pick(item, ['keyword', 'industryId', 'groupId', 'subscriptionId', 'likeCountMin']),
                    autoSyncEnabled: 0,
                    updateTimeCondition: '',
                    likeCountThreshold: ''
                }, this.getSubscriptionHotList)
            }
        },
        openUrlToDouYin() {
            window.open('https://www.douyin.com', '_blank')
            // this.$httpClient.system.openUrl({url: 'https://www.douyin.com'});
        },
        openUrlToDouBao() {
            this.$httpClient.system.openUrl({url: 'https://www.doubao.com/chat/'});
        },
        openUrl() {
            this.$httpClient.system.openUrl({url: 'https://aidso.douchacha.com/?dso_from=aifupan'});
        },
        tipsIcon() {
            this.$refs.tipMessage?.open({
                content: ` <div>
                    多个子账号在<span style="color: red">同一个网络下</span>订阅较多关键词，会有 <span style="color: red">一定的风控问题</span>，导致数据获取不顺，请分开网络订阅！
                </div>`,
                showCancelButton: false,
                confirmButtonText: '知道了',
            })
        }
    },
    mounted() {
        this.getSubscriptionHotList()
    }
}
</script>

<style lang="scss" scoped>
.subscribeHotItem {
    height: 100%;

    ::v-deep(.el-card__body) {
        padding: 0;
    }

    .subscribe-container {
        padding: 12px 24px;
        flex-shrink: 0;

        .subscribe-area {
            padding: 12px 0;

            .search-btn {
                margin-right: 10px;
            }
        }
    }

    .history-section-container {
        overflow: auto;
        margin-top: 10px;
        flex: 1;

        .history-section {
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

                .arrow {
                    max-width: 24px;
                    transform: rotate(180deg);
                    margin-top: -2px
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
                    line-height: 1.8;
                    color: #484A4D;
                    font-size: 16px;
                    text-align: left;
                }
            }

            &:first-child {
                margin-top: 0;
            }
        }
    }
}

</style>
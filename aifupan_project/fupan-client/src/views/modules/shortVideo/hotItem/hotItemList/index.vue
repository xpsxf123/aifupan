<template>
    <div class="common-hotItem-list">
        <CommonHotItem
            :tableConfig="tableConfig"
            @updateData="updateData"
            @sortChange="sortChange"
            @onSubmitSearch="onSubmitSearch"
            @paginationChange="paginationChange"/>
        <TextDialog :visible.sync="textVisible" :currentVideoItem="currentVideoItem" :isHasLike="true"/>
        <TipMessageBox ref="tipMessage" @confirmEvent="confirmEvent" @cancelEvent="cancelEvent"/>
    </div>
</template>

<script>
import CommonHotItem from '../../component/searchDetail/commonHotItem.vue'
import myUtils from "@/utils/utils";
import setTimeOut from '@/mixins/setTimeOut'
import {isEmpty, omit, pick} from 'lodash'
import extract from './../../mixins/index'
import TipMessageBox from "@/views/modules/shortVideo/component/tipMessageBox.vue";
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";

export default {
    components: {TipMessageBox, CommonHotItem},
    mixins: [setTimeOut, userAssets, extract()],
    data() {
        return {
            searchForm: {},
            sortSequence: 0,//排序顺序 0：降序 1：升序
            sortCode: 2,//排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序
            subscribeHotItemList: [],
            currentItem: {},
            tableConfig: {
                column: [{
                    label: "视频",
                    prop: 'videoTitle',
                    option: {
                        align: 'left'
                    }
                }, {
                    label: '达人(粉丝量)',
                    prop: 'influencerNickname',
                    sortable: 'custom',
                    option: {
                        width: 200,
                        align: 'left',
                        tooltip: true
                    }
                }, {
                    label: "点赞数",
                    prop: 'likeCount',
                    sortable: 'custom',
                    option: {
                        width: 110,
                        render: (item) => this.fnw(item.likeCount)
                    }
                }, {
                    label: "评论数",
                    prop: 'commentCount',
                    sortable: 'custom',
                    option: {
                        width: 110,
                        render: (item) => this.fnw(item.commentCount)
                    }
                }, {
                    label: "评赞比",
                    prop: 'commentLikeRatio',
                    sortable: 'custom',
                    option: {
                        width: 110,
                        render: (item) => this.fnw(item.commentLikeRatio)
                    }
                }, {
                    label: '转发数',
                    prop: 'shareCount',
                    sortable: 'custom',
                    option: {
                        width: 110,
                        render: (item) => this.fnw(item.shareCount)
                    }
                }, {
                    label: "收藏数",
                    prop: 'collectCount',
                    sortable: 'custom',
                    option: {
                        width: 110,
                        render: (item) => this.fnw(item.collectCount)
                    }
                }],
                options: [[{
                    label: '查看视频',
                    show: () => {
                        return true
                    },
                    click: (item) => {
                        if (!item.videoUrl) return this.$message.error('视频地址异常')
                        window.open(item.videoUrl, '_blank')
                        // this.$httpClient.system.openUrl({url: item.videoUrl});
                    }
                }, {
                    label: '复制原链接',
                    show: () => {
                        return true
                    },
                    click: async (item) => {
                        await myUtils.copyToClipboard(item.videoUrl)
                        this.$message.success('链接复制成功')
                    }
                }]],
                defaultSort: {prop: 'likeCount', order: 'descending'},
                eHeight: 305,
                tableData: []
            }
        }
    },
    computed: {
        fnw() {
            return (num, length) => {
                return myUtils.fnw(num, length)
            }
        },
    },
    created() {

    },
    mounted() {
        this.$nextTick(() => {
            this.getHotItemList({})
        })
    },
    methods: {
        async paginationChange(pagination) {
            await this.getHotItemList({pagination})
        },
        onSubmitSearch(searchForm) {
            this.searchForm = searchForm
            this.$nextTick(async () => {
                await this.getHotItemList({})
            })
        },
        async getHotItemList({pagination}) {
            const {query: param, path} = this.$route
            // const [publishStartTime, publishEndTime] = this.searchForm?.releaseTime || []
            const [durationStartNumber, durationEndNumber] = this.searchForm?.videoDuration || []
            let httpServer = ''
            if (path === '/subscribeHotItem/hotItemList') {//订阅爆款进入列表页
                if (param.isExample == 1) {//是否是示例数据
                    httpServer = this.$httpBack.shortVideo.subscriptionHotListExample
                } else {
                    httpServer = this.$httpBack.shortVideo.subscriptionHotList
                }
            } else {//搜爆款进入列表页
                httpServer = this.$httpBack.shortVideo.hotList
            }
            let paramRequest = {
                page: pagination?.currentPage || 1,
                limit: pagination?.pageSize || 10,
                sortCode: this.sortCode,
                sortSequence: this.sortSequence,
                ...pick(this.searchForm, ['commentCount', 'shareCount', 'collectCount', 'commentLikeRatioFilter', 'followersCountFilter']),
                durationStartNumber: durationStartNumber ?? '',
                durationEndNumber: durationEndNumber ?? '',
                publishTimeValue: [undefined, '', null].includes(this.searchForm.publishTimeValue) ? '' : JSON.stringify(this.searchForm.publishTimeValue),
                videoUpdateTimeType: param.time,
                likeCount: isEmpty(this.searchForm) ? param?.subscriptionLikeCountThreshold : this.searchForm.likeCount,
                // publishStartTime: publishStartTime ? `${publishStartTime} 00:00:00` : '',
                // publishEndTime: publishEndTime ? `${publishEndTime} 23:59:59` : '',
            }
            if (param.isExample == 1) {
                paramRequest = {
                    platformType: 1,//平台类型: 1-抖音, 2-快手, 3-视频号
                    searchKeyword: '江南第一深情',
                    ...paramRequest
                }
            } else {
                paramRequest = {
                    ...omit(param, ['keyWords', 'time', 'subscriptionLikeCountThreshold', 'isIgnore']),
                    ...paramRequest,
                    likeCount: isEmpty(this.searchForm) ? param?.subscriptionLikeCountThreshold : this.searchForm.likeCount,
                    videoUpdateTimeType: param.time,
                }
            }
            const serverResult = await httpServer({
                ...paramRequest
            })
            if (serverResult.code !== 0) return this.$message.error(serverResult.msg)
            this.tableConfig = {
                ...this.tableConfig,
                tableData: serverResult.data?.list || [],
                pagination: {
                    currentPage: serverResult.data?.currPage || 0,
                    total: serverResult.data?.totalCount || 0,
                    pageSize: serverResult.data?.pageSize || 10
                }
            }
            this.$nextTick(() => {
                this.scheduleNextPoll(10000, () => {
                    this.getHotItemList({pagination: this.tableConfig?.pagination})
                })
            })
        },
        sortChange({column, prop, order}) {
            const sortSequence = order === 'ascending' ? 1 : 0
            let sortCode = 1
            if (prop === 'likeCount') {
                sortCode = 2
            } else if (prop === 'commentCount') {
                sortCode = 3
            } else if (prop === 'shareCount') {
                sortCode = 4
            } else if (prop === 'collectCount') {
                sortCode = 5
            } else if (prop === 'influencerNickname') {
                sortCode = 6
            } else if (prop === 'commentLikeRatio') {
                sortCode = 7
            }
            if (!order) sortCode = 1
            this.sortCode = sortCode
            this.sortSequence = sortSequence
            this.$nextTick(() => {
                this.getHotItemList({})
            })
        },
        extractAgain(item) {
            this.reExtract({
                id: item.extractId,
                sourceType: item.sourceType || 4
            }, () => {
                this.getHotItemList({pagination: this.tableConfig?.pagination})
            })
        },
        async batchCreateExtract() {
            const {query: param} = this.$route
            await this.createExtract({
                sourceType: 4,
                sourceId: param?.snapshotId || param?.subscriptionId,
            })
            await this.getHotItemList({pagination: this.tableConfig?.pagination})
        },
        async getSubscriptionGroupHotList() {
            const result = await this.$httpBack.shortVideo.subscriptionHot()
            if (result.code !== 0) return
            this.subscribeHotItemList = result.data?.flatMap(item => item.hotSubscriptionItemVos);
        },
        refreshData() {
            this.getHotItemList({})
        },
        async updateData() {
            if (!this.subscribeHotItemList.length) {
                await this.getSubscriptionGroupHotList()
            }
            this.$nextTick(() => {
                const currentItem = this.subscribeHotItemList.find(i => i.subscriptionId === this.$route.query?.subscriptionId)
                this.manualUpdate(currentItem)
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.common-hotItem-list {
    height: 100%;
}
</style>
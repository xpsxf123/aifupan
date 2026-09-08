<template>
    <div>
        <hotItemSearchList :tableConfig="tableConfig" @sortChange="sortChange" @paginationChange="paginationChange">
            <template #emptyText>
                <div class="empty-text">如果暂无数据，请多尝试几次，每次间隔5秒钟以上</div>
            </template>
        </hotItemSearchList>
        <TextDialog :visible.sync="textVisible" :currentVideoItem="currentVideoItem" :isHasLike="true"/>
    </div>
</template>

<script>
import myUtils from "@/utils/utils";
import hotItemSearchList from "../../component/searchDetail/searchResultList.vue";
import extract from "./../../mixins/index";
import setTimeOut from "@/mixins/setTimeOut";
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";

export default {
    components: {hotItemSearchList},
    mixins: [setTimeOut, extract(), userAssets],
    props: {
        searchValue: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            snapshotId: '',
            sortSequence: 0,//排序顺序 0：降序 1：升序
            sortCode: 1,//排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序
            tableConfig: {
                eHeight: 307,
                column: [{
                    label: "视频",
                    prop: 'videoTitle',
                    option: {
                        align: 'left'
                    },
                }, {
                    label: '达人',
                    prop: 'influencerNickname',
                    option: {
                        width: 200,
                        align: 'left'
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
                pagination: {},
            }
        }
    },
    watch: {
        searchValue: {
            handler(val) {
                if (val) this.getSearchList()
            },
            immediate: true,
            deep: true
        }
    },
    computed: {
        fnw() {
            return (num) => {
                return myUtils.fnw(num, 2)
            }
        },
    },
    created() {

    },
    mounted() {

    },
    methods: {
        async paginationChange(values) {
            await this.getSearchList(values)
        },
        extractAgain(item) {
            this.reExtract({
                id: item.extractId,
                sourceType: item.sourceType || 4
            }, () => {
                this.getSearchList(this.tableConfig?.pagination)
            })
        },
        async getSearchList(pagination) {
            try {
                if (!pagination) this.snapshotId = ''
                const page = pagination?.currentPage || 1
                const limit = pagination?.pageSize || 10
                if (!pagination) {
                    const clientResult = await this.$httpClient.shortVideo.captureHotSearch({searchKeyword: this.searchValue})
                    if (!clientResult.data) {
                        this.$emit('loadingEnd')
                        return this.$message.error('获取达人快照失败，请稍后再试！')
                    }
                    this.snapshotId = clientResult.data
                }
                let serverResult = await this.$httpBack.shortVideo.hotSearch({
                    snapshotId: this.snapshotId,
                    page,
                    limit,
                    sortCode: this.sortCode,
                    sortSequence: this.sortSequence,
                })
                this.$emit('loadingEnd')
                if (serverResult.code !== 0) return
                await this.getUserProperty()
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
                        this.getSearchList(this.tableConfig?.pagination)
                    })
                })
            } catch (e) {
                this.$emit('loadingEnd')
            }
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
            }
            if (!order) sortCode = 1
            this.sortCode = sortCode
            this.sortSequence = sortSequence
            this.$nextTick(() => {
                this.getSearchList({})
            })
        },
        async batchCreateExtract() {
            await this.createExtract({
                sourceType: 4,
                sourceId: this.snapshotId,
            })
            await this.getSearchList(this.tableConfig?.pagination)
        }
    }
}
</script>

<style lang="scss" scoped>
.empty-text {
    color: #484A4D;
}
</style>
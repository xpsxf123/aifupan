<template>
    <div class="expert">
        <Search :config="config" @getAction="getAction" :loading="loading">
            <SearchList
                v-if="!!actionType"
                :actionType="actionType"
                :tableConfig="getTableConfig"
                @paginationChange="paginationChange"/>
        </Search>
    </div>
</template>

<script>
import myUtils from "@/utils/utils";
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";
import Search from './../component/search.vue'
import SearchList from './component/searchList.vue'

export default {
    mixins: [userAssets],
    components: {Search, SearchList},
    props: {},
    data() {
        return {
            loading: false,
            actionType: '',
            searchValue: '',
            snapshotId: '',
            config: {
                searchResource: '今日达人剩余搜索量',
                type: 'expert',
                searchTitle: '搜达人',
                pattern: /^[a-zA-Z0-9_.]{3,23}$/,
                searchSubtitle: '智能搜索分析 深度学习亮点',
                searchPlaceholder: '请输入抖音号'
            },
            tableConfig: {
                selection: false,
                eHeight: 322,
                column: [{
                    label: "达人昵称",
                    prop: 'name',
                    option: {
                        align: 'left',
                    }
                }, {
                    label: "达人ID",
                    prop: 'platformAccount'
                }, {
                    label: "粉丝量",
                    prop: 'followersCount',
                    hidden: this.searchValue === 'history',
                    option: {
                        render: (item) => {
                            return myUtils.fnw(item?.followersCount)
                        }
                    }
                }, {
                    label: '总视频数',
                    prop: 'videoCount',
                }],
                options: [{
                    label: '查看列表',
                    hidden: () => {
                        return false
                    },
                    click: async (item) => {
                        await this.jumpBefore(item)
                    }
                }],
                tableData: []
            }
        };
    },
    computed: {
        getTableConfig() {
            return {
                ...this.tableConfig,
                eHeight: this.tableConfig.eHeight + (this.actionType === 'history' ? 0 : -28),
                column: this.actionType === 'history' ? this.tableConfig.column.filter(item => item.prop !== 'followersCount') : this.tableConfig.column
            }
        },
    },
    watch: {},
    methods: {
        async paginationChange(values = {}) {
            await this.getAction({action: this.actionType, searchValue: this.searchValue}, values)
        },
        initTable() {
            this.tableConfig = {
                ...this.tableConfig,
                tableData: [],
                pagination: {
                    currentPage: 0,
                    total: 0,
                    pageSize: 10
                }
            }
        },
        updateTableConfig(data) {
            this.tableConfig = {
                ...this.tableConfig,
                tableData: data?.list || [],
                pagination: {
                    currentPage: data?.currPage || 0,
                    total: data?.totalCount || 0,
                    pageSize: data?.pageSize || 10
                }
            }
        },
        async getAction({action, searchValue}, pagination) {
            this.loading = true
            try {
                if (!pagination) {
                    this.initTable()
                    this.snapshotId = ''
                }
                this.actionType = action
                this.searchValue = searchValue
                const page = pagination?.currentPage || 1
                const limit = pagination?.pageSize || 10
                let serverResult = null
                if (action === 'search') {
                    if (!pagination) {
                        const clientResult = await this.$httpClient.shortVideo.captureInfluencerInfo({searchKeyword: searchValue})
                        if (!clientResult.data) {
                            this.$message.error('获取达人快照失败，请稍后再试！')
                            return
                        }
                        this.snapshotId = clientResult.data
                    }
                    serverResult = await this.$httpBack.shortVideo.search({
                        snapshotId: this.snapshotId,
                        page,
                        limit
                    })
                    await this.getUserProperty()
                } else if (action === 'history') {
                    serverResult = await this.$httpBack.shortVideo.expertHistory({page, limit})
                }
                this.loading = false
                if (!serverResult) return
                if (serverResult.code !== 0) return
                this.updateTableConfig(serverResult.data)
            } catch (e) {
                this.loading = false
            }
        },
        async jumpBefore(item) {
            const clientResult = await this.$httpClient.shortVideo.syncInfluencerVideo({
                platformUserId: item.platformUserId,
                platformAccount: item.platformAccount,
                platformType: item.platformType,
                actionType: this.actionType === 'search' ? 0 : 1
            })
            if (clientResult.code !== 0) return this.$message.success(clientResult.msg)
            await this.$router.push({
                path: '/expert/expertDetail',
                query: {
                    platformType: item.platformType,
                    platformUserId: item.platformUserId
                }
            })
        }
    },
    created() {

    },
    mounted() {

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
</style>
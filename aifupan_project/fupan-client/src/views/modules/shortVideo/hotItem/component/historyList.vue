<template>
    <div class="history-list">
        <TableList :tableConfig="tableConfig" @paginationChange="paginationChange">
            <template #keyword="{ row: item }">
                <el-tag type="success" style="max-width: 385px;font-size: 15px;">
                    <div class="text-clamp1">{{ item.keyword }}</div>
                </el-tag>
            </template>
        </TableList>
    </div>
</template>

<script>
import TableList from "./../../component/tableList.vue";

export default {
    components: {TableList},
    props: {},
    data() {
        return {
            tableConfig: {
                eHeight: 350,
                column: [{
                    label: "关键字",
                    prop: 'keyword',
                    option: {
                        width: '420',
                        align: 'left'
                    }
                }, {
                    label: '短视频条数',
                    prop: 'resultCount',
                }, {
                    label: '搜索时间',
                    prop: 'searchTime',
                }],
                options: [{
                    label: '查看列表',
                    hidden: () => {
                        return false
                    },
                    click: (item) => {
                        this.$router.push({
                            path: '/hotItem/hotItemList',
                            query: {snapshotId: item.historyId, keyWords: encodeURIComponent(item.keyword || '')}
                        })
                    }
                }],
            }
        }
    },
    computed: {},
    created() {

    },
    mounted() {
        this.getHistoryList()
    },
    methods: {
        async paginationChange(values) {
            await this.getHistoryList(values)
        },
        async getHistoryList(pagination) {
            const page = pagination?.currentPage || 1
            const limit = pagination?.pageSize || 10
            const serverResult = await this.$httpBack.shortVideo.hotHistory({page, limit})
            if (serverResult.code !== 0) return
            this.tableConfig = {
                ...this.tableConfig,
                tableData: serverResult.data?.list || [],
                pagination: {
                    currentPage: serverResult.data?.currPage || 0,
                    total: serverResult.data?.totalCount || 0,
                    pageSize: serverResult.data?.pageSize || 10
                }
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.history-list {
    height: 100%;
}
</style>
<template>
    <el-dialog
        title="录制历史"
        class="record-history"
        :visible.sync="visible"
        width="720px"
        :close-on-click-modal="false"
        :before-close="closeRecordHistoryDialog"
        :destroy-on-close="true">
        <template v-if="visible">
            <el-table
                :data="compereList"
                max-height="420px"
                class="record-history-list"
                style="width: 100%">
                <el-table-column
                    prop="name"
                    label="直播间">
                    <template slot-scope="{row}">
                        <div class="flex items-center">
                            <el-avatar :src="row.AnchorAvatar" :size="36" style="flex-shrink: 0"></el-avatar>
                            <span class="text-clamp1" style="padding-left: 6px">{{ row.AnchorName }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column
                    prop="HomeUrl"
                    label="主页"
                    width="100">
                    <template slot-scope="{row}">
                        <span v-if="row.platform!==2" style="color: var(--color-main)" class="cursor-pointer"
                              @click="()=>openUrl(row)">查看</span>
                    </template>
                </el-table-column>
                <el-table-column
                    prop="deleteDate"
                    label="删除时间"
                    width="170">
                </el-table-column>
                <el-table-column
                    prop="action"
                    align="right"
                    label="操作">
                    <template slot-scope="{row}">
                        <afp-button size="small" @click="()=>reAddAnchor(row)">添加账号</afp-button>
                        <afp-button type="danger" size="small" @click="()=>comDelete(row)">彻底删除</afp-button>
                    </template>
                </el-table-column>
            </el-table>
        </template>
        <div style="margin-top: 12px" class="text-center">
            <el-pagination
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="pagination.currentPage"
                :page-sizes="[10, 20, 50, 100]"
                :page-size="pagination.pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="pagination.total">
            </el-pagination>
        </div>
    </el-dialog>
</template>

<script>
import DialogBox from "@/components/dialog/index.vue";

export default {
    components: {DialogBox},
    mixins: [],
    props: {
        visible: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            pagination: {
                pageSize: 10,
                total: 0,
                currentPage: 1,
            },
            compereList: []
        }
    },
    computed: {},
    watch: {
        visible: {
            handler(val) {
                if (val) {
                    this.getCompereList()
                } else {
                    this.pagination = {
                        pageSize: 10,
                        total: 0,
                        currentPage: 1,
                    }
                    this.compereList = []
                }
            },
            deep: true
        }
    },
    methods: {
        closeRecordHistoryDialog() {
            this.$emit('update:visible', false);
        },
        reAddAnchor(item) {
            this.$httpClient.compere.reAddAnchor({
                secUid: item.SecUid
            }).then(res => {
                if (res.code === 0) {
                    this.getCompereList()
                    this.$message.success('恢复成功')
                }
            });
        },
        fullRemoveAnchor(item){
            this.$httpClient.compere.fullRemoveAnchor({
                secUid: item.SecUid
            }).then(res => {
                if (res.code === 0) {
                    this.getCompereList()
                    this.$message({
                        type: 'success',
                        message: '删除成功!'
                    });
                }
            });
        },
        comDelete(item) {
            this.$confirm('彻底删除后直播间信息无法恢复，请确认操作', '友情提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
                showClose: false
            }).then(() => {
                this.fullRemoveAnchor(item)
            }).catch(() => {
            });
        },
        openUrl(row) {
            this.$httpClient.system.openUrl({url: row.HomeUrl});
        },
        handleSizeChange(val) {
            this.pagination = {
                ...this.pagination,
                currentPage: 1,
                pageSize: val
            }
            this.getCompereList()
        },
        handleCurrentChange(val) {
            this.pagination = {
                ...this.pagination,
                currentPage: val
            }
            this.getCompereList()
        },
        getCompereList() {
            let requestData = {
                pageIndex: this.pagination.currentPage,
                pageSize: this.pagination.pageSize,
                anchorName: "",
                recordStatus: null,
                isRemoveRecord: 1,
                tradeId: ''
            }
            this.$httpClient.compere.getpageanchor(requestData).then((res) => {
                if (res.code === 0) {
                    this.compereList = res.data?.DataList || []
                    this.pagination = {
                        ...this.pagination,
                        total: res.data?.Total || 0
                    }
                }
            })
        },
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
    beforeRouteLeave() {

    }
}
</script>
<style lang='scss' scoped>
.record-history {
    ::v-deep(.el-dialog__body) {
        padding: 6px 6px 6px 12px !important;
    }
}
::v-deep(.el-table td.el-table__cell){
    border: none;
}
::v-deep(.el-table th.el-table__cell.is-leaf, .el-table td.el-table__cell){
    background: #FBFBFB;
    border: none;
}
::v-deep(.el-table){
    &:before{
        height: 0;
    }
}

</style>
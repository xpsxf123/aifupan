<template>
    <div class="step-anchor">
        <el-table
            :data="tableData"
            height="calc(100vh - 190px)"
            style="width: 100%">
            <el-table-column
                prop="anchorInfo"
                label="主播"
                width="200">
                <template slot-scope="{row}">
                    <div class="flex items-center">
                        <el-avatar :size="42" :src="row?.anchorInfo?.anchorAvatar" style="flex-shrink: 0"/>
                        <div style="margin-left: 6px">{{ row?.anchorInfo?.anchorName }}</div>
                    </div>
                </template>
            </el-table-column>
            <el-table-column
                align="center"
                prop="videoCount"
                label="录制场次">
            </el-table-column>
            <el-table-column
                prop="lastRecordTime"
                align="center"
                label="最近录制">
            </el-table-column>
            <el-table-column
                label="操作"
                align="right"
                width="128">
                <template slot-scope="{row}">
                    <afp-button size="small" @click="()=>viewAnalysis(row)">查看列表</afp-button>
                </template>
            </el-table-column>
            <div slot="empty">
                <img src="@/assets/imgs/chartEmpty.png" style="max-height: 160px" alt="">
            </div>
        </el-table>
        <el-pagination
            class="text-center"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="dataForm.page"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="dataForm.limit"
            layout="total, sizes, prev, pager, next, jumper"
            :total="dataForm.totalCount">
        </el-pagination>
        <AnchorRecordList :currentItemAnchor="currentItemAnchor" ref="currentItemAnchor"
                          @changeCurrentItemAnchor="changeCurrentItemAnchor"/>
    </div>
</template>

<script>
import AnchorRecordList from "@/views/modules/setup/component/anchorRecordList.vue";

export default {
    components: {AnchorRecordList},
    props: {
        currentItem: {
            type: Object,
            default: () => {
                return {}
            }
        },
        accountType: {
            type: [Number, String],
            default: ''
        }
    },
    data() {
        return {
            tableData: [],
            currentItemAnchor: {},
            dataForm: {
                page: 1,
                limit: 10,
                totalCount: 0
            }
        };
    },
    computed: {},
    watch: {
        "currentItem.id": {
            handler(value) {
                if (value) this.getSubList()
            },
            deep: true,
            immediate: true
        },
        accountType: {
            handler(value) {
                if (value !== undefined) {
                    this.dataForm = {
                        page: 1,
                        limit: 10,
                        totalCount: 0
                    }
                    this.$nextTick(() => {
                        this.getSubList()
                    })
                }
            },
            deep: true
        }
    },
    methods: {
        changeCurrentItemAnchor() {
            this.currentItemAnchor = {}
        },
        getSubList() {
            this.$httpBack.subAccount.getSubUserAnchorList({
                page: this.dataForm?.page,
                limit: this.dataForm?.limit,
                userId: this.currentItem?.id,
                accountType: this.accountType
            }).then(res => {
                if (res.code === 0) {
                    this.tableData = res.data?.list ? res.data?.list : [];
                    this.dataForm = {
                        page: res.data?.currPage,
                        limit: res.data?.pageSize,
                        totalCount: res.data?.totalCount,
                    }
                } else {
                    this.tableData = [];
                }
            })
        },
        handleSizeChange(val) {
            this.dataForm = {
                ...this.dataForm,
                limit: val
            }
            this.$nextTick(() => {
                this.getSubList()
            })
        },
        handleCurrentChange(val) {
            this.dataForm = {
                ...this.dataForm,
                page: val
            }
            this.$nextTick(() => {
                this.getSubList()
            })
        },
        viewAnalysis(row) {
            this.currentItemAnchor = row
            this.$refs.currentItemAnchor?.open()
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
.step-anchor {
    ::v-deep(.el-table) {
        .el-table__cell {
            border: none;
        }

        &:before {
            height: 0;
        }
    }

    .text-center {
        margin-top: 12px;
    }
}
</style>
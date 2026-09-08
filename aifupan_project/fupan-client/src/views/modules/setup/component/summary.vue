<template>
    <div class="step-summary">
        <el-table
            :data="tableData"
            height="calc(100vh - 190px)"
            style="width: 100%">
            <el-table-column
                prop="liveTitle"
                label="本地文件">
                <template slot-scope="{row}">
                    <div class="flex items-center" style="width: 220px;position: relative">
                        <el-avatar :size="42" :src="row?.anchorInfo?.anchorAvatar" style="flex-shrink: 0"/>
                        <div style="margin-left: 6px">
                            <div class="text-clamp1">{{ row?.videoName }}</div>
                            <div class="text-clamp1">{{ row?.startTime }}</div>
                        </div>
                        <img style="position: absolute;width: 42px" v-if="row.uploadStatus" src="@/assets/imgs/scfx.png" alt=""/>
                    </div>
                </template>
            </el-table-column>
            <el-table-column
                prop="startTime"
                label="录制时间"
                width="200">
            </el-table-column>
            <el-table-column
                prop="session"
                label="查看小结"
                width="100">
                <template slot-scope="{row}">
                    <span style="color:var(--color-main);cursor: pointer" @click="()=>viewNotes(row)">查看</span>
                </template>
            </el-table-column>
            <el-table-column
                label="操作"
                align="right"
                width="100">
                <template slot-scope="{row}">
                    <afp-button
                        :type="row.uploadStatus?'primary':'warning'"
                        round size="small"
                        @click="()=>assistant(row)">查看分析
                    </afp-button>
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

        <SummaryNote ref="summary"></SummaryNote>
    </div>
</template>

<script>
import myUtils from "@/utils/utils";
import SummaryNote from "@/components/summary/index.vue";
import summaryMixin from "@/components/summary/mixin";

export default {
    components: {SummaryNote},
    mixins: [summaryMixin],
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
                if (value) this.getSubUserYesterdayNotesVideoList()
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
                    this.$nextTick(()=>{
                        this.getSubUserYesterdayNotesVideoList()
                    })
                }
            },
            deep: true
        }
    },
    methods: {
        getSubUserYesterdayNotesVideoList() {
            this.$httpBack.subAccount.getSubUserYesterdayNotesVideoList({
                page: this.dataForm?.page,
                limit: this.dataForm?.limit,
                userId: this.currentItem?.id,
                accountType: this.accountType
            }).then(res => {
                if (res.code === 0) {
                    const list = res.data?.list || []
                    list.forEach(_item => {
                        _item.duration = myUtils.toformatTimeChinse(_item.duration * 1000);
                    })
                    this.tableData = list
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
                this.getSubUserYesterdayNotesVideoList()
            })
        },
        handleCurrentChange(val) {
            this.dataForm = {
                ...this.dataForm,
                page: val
            }
            this.$nextTick(() => {
                this.getSubUserYesterdayNotesVideoList()
            })
        },
        assistant(item) {
            if (!item.uploadStatus) return this.$message.warning('该场直播未上传云空间，无法查看')
            const {videoSliceType} = item;
            this.$router.push({
                path: videoSliceType === 1 ? '/online/sliceOnline/analysis' : '/online/onlineVideo/analysis',
                query: {
                    id: item.videoId
                }
            })
        },
        viewNotes(row) {
            this.onClickSummary(row);
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
.step-summary {
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
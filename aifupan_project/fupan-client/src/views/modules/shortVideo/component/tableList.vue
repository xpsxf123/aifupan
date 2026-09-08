<template>
    <div class="table-list flex flex-col">
        <div style="flex: 1">
            <el-table
                :data="tableConfig.tableData"
                v-bind="coreHeight"
                size="medium"
                @sort-change="sortChange"
                :header-cell-class-name="'custom-header'"
                :default-sort="tableConfig.defaultSort||{}"
                @selection-change="handleSelectionChange"
                style="width: 100%">
                <el-table-column
                    v-if="tableConfig.selection"
                    type="selection"
                    :selectable="checkSelectable"
                    width="35">
                </el-table-column>
                <template
                    v-for="item in tableConfig.column">
                    <el-table-column
                        v-if="!item.show"
                        :key="item.prop"
                        :prop="item.prop"
                        :sortable="item.sortable||false"
                        :label="item.label"
                        :show-overflow-tooltip="item.option?.tooltip"
                        :align="item.option?.align||'center'"
                        :width="item.option?.width"
                    >
                        <template slot-scope="{ row, column, $index }">
                            <template v-if="$scopedSlots[item.prop]">
                                <slot :name="item.prop" :row="row"/>
                            </template>
                            <template v-else>
                                <div v-if="isRender(item)"
                                     v-html="handlerRender(row, item.option, column, $index)"></div>
                                <div v-else style="padding-inline: 6px">{{ row[item.prop] }}</div>
                            </template>
                        </template>
                    </el-table-column>
                </template>

                <el-table-column
                    label="操作"
                    width="250"
                    align='center'
                >
                    <template slot-scope="{row}">
                        <Operation :options="tableConfig.options" :data="row"></Operation>
                    </template>
                </el-table-column>
                <template #empty>
                    <div class="empty-state flex flex-col items-center justify-center"
                         v-if="!tableConfig.tableData?.length">
                        <div class="empty-image">
                            <img style="filter: grayscale(100%);" src="@/assets/imgs/none.png" alt="暂无数据"/>
                        </div>
                        <slot name="emptyText"></slot>
                    </div>
                </template>
            </el-table>
        </div>
        <div class="table-pagination" v-if="tableConfig.pagination??false">
            <el-pagination
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="pagination.currentPage"
                :page-sizes="[10, 20, 50, 100]"
                :page-size="pagination.pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="pagination.total||0">
            </el-pagination>
        </div>
    </div>
</template>

<script>
import {isEmpty, isEqual} from "lodash";
import Operation from "@/components/Table/operation.vue";

export default {
    components: {Operation},
    props: {
        tableConfig: {
            type: Object,
            default: () => {
                return {}
            }
        },
        optional: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            pagination: {
                currentPage: 1,
                total: 0,
                pageSize: 10
            }
        }
    },
    computed: {
        tableMaxHeight() {
            return this.tableConfig?.eHeight ? `calc(100vh - ${this.tableConfig?.eHeight || 0}px)` : '100%'
        },
        isRender() {
            return (item) => {
                return typeof item.option?.render === 'function'
            }
        },
        coreHeight() {
            return this.tableConfig.pagination ? {height: this.tableMaxHeight} : {}
        }
    },
    watch: {
        'tableConfig.pagination': {
            handler(newVal) {
                if (!isEmpty(newVal)) {
                    this.pagination = {
                        ...this.pagination,
                        ...newVal
                    }
                }
            },
            immediate: true,
            deep: true
        },
    },
    methods: {
        handlerRender(row, option, column, index) {
            return option.render(row, option, column)
        },
        handleSelectionChange(val) {
            this.$emit('selectionChange', val)
        },
        sortChange({column, prop, order}) {
            this.$emit('sortChange', {column, prop, order})
        },
        checkSelectable(row, index) {
            return row.selectable
        },
        handleSizeChange(val) {
            this.pagination = {
                ...this.pagination,
                currentPage: 1,
                pageSize: val
            }
            this.$emit('paginationChange', this.pagination)
        },
        handleCurrentChange(val) {
            this.pagination = {
                ...this.pagination,
                currentPage: val
            }
            this.$emit('paginationChange', this.pagination)
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
<style lang='scss'>

.table-list {
    height: 100%;

    .el-table {
        .custom-header {
            background: #FBFBFB;
        }
    }

    .el-table td.el-table__cell {
        border: none;
        padding: 14px 0;
    }

    .el-table th.el-table__cell.is-leaf, .el-table td.el-table__cell {
        border: none;
    }

    .el-table-column--selection {
        .cell {
            padding-left: 10px;
            padding-right: 0;
        }
    }

    .el-table::before {
        background: none;
    }

    .table-pagination {
        text-align: center;
        padding-block: 12px;

        .el-pagination {
            padding-block: 0;
        }
    }

    .empty-state {
        padding: 60px 0;

        .empty-image {
            width: 240px;
            height: 180px;
            margin-bottom: 20px;

            img {
                width: 100%;
                height: 100%;
                object-fit: contain;
            }
        }
    }
}
</style>
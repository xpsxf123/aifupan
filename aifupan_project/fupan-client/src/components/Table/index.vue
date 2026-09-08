<template>
    <div>
        <div class="table-common table-content" style="height: 100%; width: 100%;">
            <slot name="table-before"></slot>
            <slot>
                <el-table
                    ref="table"
                    :data="data"
                    :row-key="getRowKey"
                    :key="tableKey"
                    @selection-change="selectTableList"
                    @select-all="selectAll"
                    @cell-mouse-enter="cellMouseEnter"
                    @cell-mouse-leave="cellMouseLeave"
                    v-bind="tableBind"
                >
                    <slot name="column-before"></slot>
                    <!-- 暂无数据提醒 -->
                    <template slot="empty">
                        <div>
                            <slot name="empty">
                                <Empty size="80" :desc="emptyText || '暂无数据'"></Empty>
                            </slot>
                        </div>
                    </template>
                    <!-- 占位符号解决ele问题 -->

                    
                    <el-table-column v-if="select" :key="`__selection_${select}`" type="selection" reserve-selection width="55" align="center">
                    </el-table-column>
                    <el-table-column v-else :key="`__selection_placeholder_${select}`" width="1"></el-table-column>
                    <Column
                        v-for="(col,index) in getColumn"
                        v-if="colHide(col)"
                        :key="col.prop"
                        :prop="col.prop"
                        :label="col.label"
                        v-bind="{
                        align: index === 0 ? 'left': 'center',
                        ...col,
                        defaultText,
                        dicData: getDicData[(col.dicProp || col.prop)]
                    }"
                    >
                        <template v-for="slot in getSlotList(col)" slot-scope="scope" :slot="slot">
                            <slot v-bind="{...scope, pVisible: popoverIndex === scope.$index, pIndex: popoverIndex}" :name="slot"></slot>
                        </template>
                    </Column>
                    <slot name="column-after"></slot>
                    <template v-if="slotList.append" slot="append">
                        <slot name="append"></slot>
                    </template>
                    <el-table-column
                        label="操作"
                        v-if="getMenu !== false || $scopedSlots['menu']"
                        :width="`${getMenu.width}`"
                        :align="`right`"
                    >
                        <template slot-scope="scope">
                            <slot name="menu" v-bind="{...scope,menu:getMenu}">
                                <Operation :options="getMenu.options" :data="scope.row"></Operation>
                            </slot>
                        </template>
                    </el-table-column>
                </el-table>
            </slot>
            <slot name="after"></slot>
        </div>
    </div>
</template>

<script>
import Column from './column.vue'
import slot from './../../mixins/slot'
import Empty from './../empty/index.vue'
import Operation from './operation.vue'

export default {
    components: { Column, Empty, Operation },
    mixins: [slot],
    props: {
        data: {
            type: Array,
            default: () => {
                return []
            }
        },
        select: {
            type: Boolean,
            default: false
        },
        column: {
            type: Array,
            default: () => {
                return []
            }
        },
        defaultText: {
            type: String,
            default: ''
        },
        menuConfig: {
            type: [Boolean, Object],
            default: false
        },
        emptyText: {
            type: String,
            default: ''
        },
        menu: {
            type: [Boolean, Object],
            default: false
        },
        selectId: {
            type: String,
            default: ''
        },
        height: {
            type: [Number, String],
            default: ''
        },
        rowKey: {
            type: String,
            default: ''
        },
        optionsMap: {
            type: Object,
            default: () => {return {}}
        },
        config: {
            type: Object,
            default: () => {
                return {}
            }
        },
        isFlex: {
            type: Boolean,
            default: false
        },
    },
    data () {
        return {
            columnMap: {},
            dicDataMap: {},
            popoverVisible: false,
            popoverIndex: -1,
            tableKey:1,
        }
    },
    computed: {
        versionType(){
            return this.$store.getters.getVersionType
        },
        slotList () {
            return {
                ...this.$slots,
                ...this.$scopedSlots
            }
        },
        getColumn () {
            return this.column
        },
        getMenu () {
            return this.menuConfig || this.menu
        },
        // 获取table的dom
        getTableRef () {
            return this.$refs?.table
        },
        getRowKey () {
            return this.rowKey || 'id'
        },
        tableBind () {
            let o = {
                ...this.config || {},
                ...this.$CONFIG?.table || {}
            }
            if (this.height) {
                o.height = this.height
            }
            return o
        },
        getDicData () {
            let o = { ...this.optionsMap }
            this.column.forEach(d => {
                if (d.dicData) {
                    o[d.prop] = d.dicData
                }
            })
            return o
        }
    },
    watch: {
        versionType() {
            this.updateTable()
        },
        select() {
            this.updateTable()
        }
    },
    methods: {
        // 鼠标移入
        cellMouseEnter (row, column, cell, event) {
            const id= row[this.getRowKey];
            this.popoverIndex = this.data.findIndex((d,index) => {
                return  row[this.getRowKey] ===  d[this.getRowKey]
            });
            this.popoverVisible = true;
        },
        // 鼠标移出
        cellMouseLeave (row, column, cell, event) {
            this.popoverVisible = false;
            this.popoverIndex = -1;
        },
        colHide (col) {
            if (typeof col.hidden === 'function') {
                return !col.hidden(col)
            }
            if (typeof col.show === 'function') {
                return col.show(col)
            }
            if (col.hidden) return false
            return true
        },
        initColumn () {
            this.column.forEach((d) => {

                this.columnMap[d.prop] = d
            })
        },
        setHidden (prop, bl) {
            this.$set(this.columnMap[prop], 'hidden', bl)
        },
        selectAll (list) {
            this.$emit('select-all', list)
        },
        selectTableList (list) {
            // 储存特殊id数据
            let selectIds = []
            let key = this.selectId || this.rowKey
            // 过滤固定id数据
            if (key) {
                selectIds = list?.map(item => {
                    return item?.[key] || false
                })?.filter(d => !!d)
            }
            // 返回选择数据
            this.$emit('select-list', {
                selectIds,
                list
            })
        },
        // 选中
        toggleRowSelection (tableList) {
            this.clearSelection()
            this.$nextTick(() => {
                let list = this.data
                tableList?.forEach(row => {
                    let key = this.selectId || this.rowKey
                    if (key) {
                        let sRow = list.find(d => d[key] === row[key])
                        if (sRow) {
                            this.$refs.table.toggleRowSelection(sRow, true)
                        }
                    } else {
                        this.$refs.table.toggleRowSelection(row, true)
                    }
                })
            })
        },
        clearSelection () {
            this.$nextTick(() => {
                this.$refs.table.clearSelection()
            })
        },
        // toggleRowSelection(...arg){
        //     this.$refs.table.toggleRowSelection(...arg)
        // }
        updateTable() {
            this.tableKey += 1
        }
    },
    created () {

    },
    mounted () {
        this.$nextTick(() => {
            this.initColumn()
        })
    },
    beforeCreate () {}, //生命周期 - 创建之前
    beforeMount () {}, //生命周期 - 挂载之前
    beforeUpdate () {}, //生命周期 - 更新之前
    updated () {}, //生命周期 - 更新之后
    beforeDestroy () {}, //生命周期 - 销毁之前
    destroyed () {}, //生命周期 - 销毁完成
    activated () {

    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.table-common {
    ::v-deep(.el-table__header-wrapper) {
        tr, th {
            background: #FBFBFB;
            font-size: 14px;
            color: #909499;
        }
    }
}

// .wrapper{
//     flex:1;
//     width:100%;
//     position: relative;
//     .table-content{
//         position: absolute; 
//         width:100%;
//     }
// }


</style>

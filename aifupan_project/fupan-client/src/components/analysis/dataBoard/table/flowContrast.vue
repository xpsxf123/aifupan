<template>
    <div class="flow-contrast">
        <el-table
            :data="tableData"
            :span-method="arraySpanMethod"
            max-height="300"
            border
            :header-cell-style="{ padding: '8px 0', fontWeight: 500 }"
            :row-class-name="tableRowClassName"
            style="width: 100%">
            <el-table-column
                align="center"
                fixed="left"
                label="直播间"
                width="120"
                show-overflow-tooltip>
                <template slot-scope="scope">
                    <div style="display: flex;align-items: center;flex-direction:column;padding: 8px 0">
                        <el-avatar :size="32" :src="scope.row.AnchorAvatar"></el-avatar>
                        
                        <div>{{ scope.row.AnchorName }}</div>
                    </div>
                </template>
            </el-table-column>
            <el-table-column align="center" label="流量结构">
                <template slot-scope="{row}">
                    <div v-if="![1,8].includes(row.dataStatus)||flowLabels.length===0">
                        <span v-if="flowLabels.length===0">暂无数据</span>
                        <span v-else-if="row.dataStatus===0">数据正在拉取中，请稍后</span>
                        <span v-else-if="row.dataStatus===2">数据获取失败，请重试</span>
                        <span v-else-if="row.dataStatus===3">账号流量较低，暂未收录</span>
                        <span v-else-if="row.dataStatus===4">低于50分钟的录屏，需要手动刷新才会获取数据</span>
                        <span v-else-if="row.dataStatus===5">资源不足</span>
                        <span v-else-if="row.dataStatus===6">直播间还没下播，暂时无法提供数据</span>
                        <span v-else-if="row.dataStatus===7">数据整理中，请稍后获取</span>
                        <span v-else>未开启数据面板</span>
                    </div>
                </template>
                <el-table-column v-for="(flow, index) in flowLabels" :key="index" :label="flow" :prop="'flow_' + index"
                                 align="center">
                    <template slot-scope="{row}">
                        <div v-if="[1,8].includes(row.dataStatus)">
                            {{ getFlowValue(row.watchFlowList, flow) || '-' }}
                        </div>
                        <div v-else>-</div>
                    </template>
                </el-table-column>
            </el-table-column>
        </el-table>
    </div>
</template>

<script>
import contrastCommon from './common'
import {isNumber} from "lodash";

export default {
    mixins: [contrastCommon],
    props: {
        tableData: {
            type: Array,
            default: () => {
                return []
            }
        }
    },
    data() {
        return {}
    },
    computed: {
        formatRatio() {
            return (value) => {
                return isNumber(value) ? (value * 100).toFixed(2) + '%' : '-';
            }
        },
        flowLabels() {
            const labelList = []
            this.tableData.forEach(item => {
                item?.watchFlowList?.forEach(_item => {
                    if (_item?.channelName) {
                        labelList.push(_item.channelName)
                    }
                })
            })
            return [...new Set(labelList)]
        }
    },
    watch: {},
    methods: {
        arraySpanMethod({row, column, rowIndex, columnIndex}) {
            const NO_SALES_START_INDEX = 1
            const SALES_END_INDEX = this.flowLabels?.length || 0 // 销售数据结束列索引
            if (![1, 8].includes(row.dataStatus)) {// 没有正确的数据返回
                if (columnIndex >= NO_SALES_START_INDEX && columnIndex <= SALES_END_INDEX) {
                    if (columnIndex === NO_SALES_START_INDEX) {
                        return [1, SALES_END_INDEX]
                    } else {
                        return [0, 0]
                    }
                }
            }
            return [1, 1] // 默认不合并
        },
        getFlowValue(flowPortrait = [], label) {
            const item = flowPortrait?.find(p => {
                return p?.channelName === label
            });
            return item ? this.formatRatio(item.ratio) : '-';
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
.flow-contrast {
    padding-bottom: 12px;

    ::v-deep(.el-table) {
        .warning-row {
            color: #0D6986;
        }

        .success-row {
            color: #B94E3B;
        }
    }

}
</style>

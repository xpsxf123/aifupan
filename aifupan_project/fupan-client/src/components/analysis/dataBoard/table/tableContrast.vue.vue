<template>
    <div class="table-dashboard">
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
                min-width="200"
                label="直播间">
                <template slot-scope="scope">
                    <div style="display: flex;align-items: center;flex-direction:column;padding: 8px 0">
                        <el-avatar :size="32" :src="scope.row.AnchorAvatar"></el-avatar>
                        <div>{{ scope.row.AnchorName }}</div>
                    </div>
                </template>
            </el-table-column>
            <el-table-column align="center" label="流量数据">
                <el-table-column
                    align="center"
                    label="观看人次"
                    width="100">
                    <template slot-scope="{row}">
                        <div v-if="[1,8].includes(row.dataStatus)">
                            {{ fnw(row.totalWatchNum) || '-' }}
                            <span v-html="isMax('totalWatchNum', row.totalWatchNum)"></span>
                        </div>
                        <div v-else>
                            <span v-if="row.dataStatus===0">数据正在拉取中，请稍后</span>
                            <span v-else-if="row.dataStatus===2">数据获取失败，请重试</span>
                            <span v-else-if="row.dataStatus===3">账号流量较低，暂未收录</span>
                            <span v-else-if="row.dataStatus===4">低于50分钟的录屏，需要手动刷新才会获取数据</span>
                            <span v-else-if="row.dataStatus===5">资源不足</span>
                            <span v-else-if="row.dataStatus===6">直播间还没下播，暂时无法提供数据</span>
                            <span v-else-if="row.dataStatus===7">数据整理中，请稍后获取</span>
                            <span v-else>未开启数据面板</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="平均在线"
                    width="100">
                    <template slot-scope="{row}">
                            <span v-if="row.averageOnlineNum>=0">
                                {{ fnw(row.averageOnlineNum) }}
                                <span v-html="isMax('averageOnlineNum', row.averageOnlineNum)"></span>
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="平均停留"
                    width="100">
                    <template slot-scope="{row}">
                            <span v-if="row.averageResidenceTime>=0">
                                {{ minutes(row.averageResidenceTime) }}
                                <span v-html="isMax('averageResidenceTime', row.averageResidenceTime)"></span>
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="涨粉人数"
                    width="100">
                    <template slot-scope="{row}">
                            <span v-if="row.incrementFollowerCount>=0">
                                {{ fnw(row.incrementFollowerCount) }}
                                <span v-html="isMax('incrementFollowerCount', row.incrementFollowerCount)"></span>
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
            </el-table-column>

            <el-table-column align="center" label="人气数据">
                <el-table-column
                    align="center"
                    label="转粉率"
                    width="100">
                    <template slot-scope="{row}">
                        <span v-if="row.interactionPercent>0&&row.interactionPercent.toFixed(2)!=='0.00'">
                            {{ (row.convertFanRate * 100).toFixed(2) }} %
                            <span v-html="isMax('convertFanRate', row.convertFanRate)"></span>
                        </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="整场互动率"
                    width="100">
                    <template slot-scope="{row}">
                            <span v-if="row.interactionPercent>=0">
                                {{ fnw(row.interactionPercent) }} %
                                <span v-html="isMax('interactionPercent', row.interactionPercent)"></span>
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
            </el-table-column>

            <el-table-column align="center" label="销售数据">
                <el-table-column
                    align="center"
                    label="销售额"
                    width="150">
                    <template slot-scope="{row}">
                        <div v-if="[1,8].includes(row.dataStatus)&&row.isTakeProduct">
                                <span v-if="isNotNilNumber(row,['volumeStart','volumeEnd'])">
                                    {{ dataView(row, ['volumeStart', 'volumeEnd']) }}
                            </span>
                            <span v-else>-</span>
                        </div>
                        <span v-if="row.dataStatus===1&&!row.isTakeProduct">本场没有进行电商商品销售</span>
                        <!--                        <span v-if="row.dataStatus===8">销售数据校验中，下播后6到8个小时再看</span>-->
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="销量"
                    width="120">
                    <template slot-scope="{row}">
                            <span v-if="isNotNilNumber(row,['purchaseCountStart','purchaseCountEnd'])">
<!--                                  {{ fnw(row.purchaseCountStart) }} ~ {{ fnw(row.purchaseCountEnd) }}-->
                                   {{ dataView(row, ['purchaseCountStart', 'purchaseCountEnd'], 2) }}
                                </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="客单价"
                    width="120">
                    <template slot-scope="{row}">
                            <span v-if="isNotNilNumber(row,['customerUnitPriceStart','customerUnitPriceEnd'])">
<!--                                  {{ fnw(row.customerUnitPriceStart, 0) }} ~ {{ fnw(row.customerUnitPriceEnd, 0) }}-->
                                  {{ dataView(row, ['customerUnitPriceStart', 'customerUnitPriceEnd'], 1) }}
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="uv价值"
                    width="100">
                    <template slot-scope="{row}">
                            <span v-if="isNotNilNumber(row,['uvValueStart','uvValueEnd'])">
                                 {{ dataView(row, ['uvValueStart', 'uvValueEnd'], 1) }}
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="带货转化率"
                    width="130">
                    <template slot-scope="{row}">
                            <span v-if="isNotNilNumber(row,['goodsConvertRateStart','goodsConvertRateEnd'])">
                                  {{ goodsDataView(row, ['goodsConvertRateStart', 'goodsConvertRateEnd'], 2) }}
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    label="千次成交"
                    width="120">
                    <template slot-scope="{row}">
                            <span v-if="isNotNilNumber(row,['goodsConvertRateStart','goodsConvertRateEnd'])">
<!--                                  {{ fnw(row.goodsConvertRateStart) }} ~ {{ fnw(row.goodsConvertRateEnd) }}-->
                                  {{ dataView(row, ['gpmStart', 'gpmEnd'], 2) }}
                            </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
            </el-table-column>
        </el-table>
    </div>
</template>

<script>
import contrastCommon from './common'

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
    computed: {},
    watch: {},
    methods: {
        arraySpanMethod({row, column, rowIndex, columnIndex}) {
            const NO_SALES_START_INDEX = 1
            const SALES_START_INDEX = 7 // 你的销售数据开始列索引（根据实际表格结构可能要调整）
            const SALES_END_INDEX = 12 // 销售数据结束列索引
            const SALES_LENGTH = 6 //销售数据长度
            if (![1, 8].includes(row.dataStatus)) {// 没有正确的数据返回
                if (columnIndex >= NO_SALES_START_INDEX && columnIndex <= SALES_END_INDEX) {
                    if (columnIndex === NO_SALES_START_INDEX) {
                        return [1, SALES_END_INDEX]
                    } else {
                        return [0, 0]
                    }
                }
            } else {
                if (!row.isTakeProduct) {
                    if (columnIndex >= SALES_START_INDEX && columnIndex <= SALES_END_INDEX) {
                        if (columnIndex === SALES_START_INDEX) {
                            // 第一列显示，跨 SALES_LENGTH 列
                            return [1, SALES_LENGTH]
                        } else {
                            // 其他列隐藏
                            return [0, 0]
                        }
                    }
                }
            }
            return [1, 1] // 默认不合并
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
}
</script>
<style lang='scss' scoped>
.table-dashboard {
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

<template>
    <div class="crowd-contrast">
        <el-table
            :data="tableData"
            max-height="300"
            border
            :span-method="arraySpanMethod"
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

            <el-table-column align="center" label="性别分布 (整场)">
                <template slot-scope="{row}">
                    <div v-if="![1,8].includes(row.dataStatus)||sexLabels.length===0">
                        <span v-if="sexLabels.length===0">暂无数据</span>
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
                <el-table-column v-for="(sex, index) in sexLabels" :key="index" :label="sex" :prop="'sex_' + index"
                                 align="center">
                    <template slot-scope="{row}">
                        <div v-if="[1,8].includes(row.dataStatus)">
                            {{ getAgeValue(row.watchUserPortrait?.genderPortrait, sex) || '-' }}
                        </div>
                        <div v-else>-</div>
                    </template>
                </el-table-column>
            </el-table-column>

            <el-table-column align="center" label="年龄分布 (整场)">
                <template slot-scope="{row}">
                    <div v-if="![1,8].includes(row.dataStatus)||ageLabels.length===0">
                        <span v-if="ageLabels.length===0">暂无数据</span>
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
                <el-table-column v-for="(age, index) in ageLabels" :key="index" :label="age" :prop="'age_' + index"
                                 align="center">
                    <template slot-scope="{row}">
                        <div v-if="[1,8].includes(row.dataStatus)">
                            {{ getAgeValue(row.watchUserPortrait?.agePortrait, age) || '-' }}
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
                return value >= 0 ? (value * 100).toFixed(2) + '%' : '-';
            }
        },
        sexLabels() {
            const labelList = []
            this.tableData.forEach(item => {
                item?.watchUserPortrait?.genderPortrait?.forEach(_item => {
                    if (_item?.label) {
                        labelList.push(_item.label)
                    }
                })
            })
            return [...new Set(labelList)]
        },
        ageLabels() {
            const labelList = []
            this.tableData.forEach(item => {
                item?.watchUserPortrait?.agePortrait?.forEach(_item => {
                    if (_item?.label) {
                        labelList.push(_item.label)
                    }
                })
            })
            return this.sortList([...new Set(labelList)])
        }
    },
    watch: {},
    methods: {
        sortList(list) {
            list.sort((a, b) => {
                // 提取开头的数字，如果是 > 开头的，就给它一个很大的值
                let numA = a.startsWith('>') ? Infinity : parseInt(a.split('-')[0], 10);
                let numB = b.startsWith('>') ? Infinity : parseInt(b.split('-')[0], 10);
                return numA - numB;
            });
            return list
        },
        arraySpanMethod({row, column, rowIndex, columnIndex}) {
            const NO_SALES_START_INDEX = 1
            const SALES_END_INDEX = 2 + (this.ageLabels?.length || 0) // 销售数据结束列索引
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
        getAgeValue(agePortrait = [], label) {
            const item = agePortrait?.find(p => {
                return p?.label === label
            });
            return item ? this.formatRatio(item.value) : '-';
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
.crowd-contrast {
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

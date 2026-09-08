<template>
    <div class="average">
        <div>
            <span>场观</span>
            <el-popover v-if="row.YesterdayAverageObservationNum > 0" style="padding: 0" trigger="hover">
                <el-table class="averageTable" size="mini" :data="dataList"
                          style="width: 100%;max-height: 200px;font-size: 12px">
                    <el-table-column prop="type">
                    </el-table-column>
                    <el-table-column width="150" prop="yesterday" label="昨日平均">
                    </el-table-column>
                    <el-table-column width="150" prop="before" label="前日平均">
                    </el-table-column>
                </el-table>
                <span class="text-colorTheme" slot="reference">
                    {{ conversion(row.YesterdayAverageObservationNum) }}
                    <i class="el-icon-caret-top" style="color: red"
                       v-if="row?.YesterdayAverageObservationNum>row?.DayBeforeAverageObservationNum"/>
                    <i class="el-icon-caret-bottom" style="color: green"
                       v-if="row?.YesterdayAverageObservationNum<row?.DayBeforeAverageObservationNum"/>
                </span>
            </el-popover>
            <span v-else class="font-s12">无数据</span>
        </div>
        <div>
            <span>销售额</span>
            <span class="text-colorTheme" v-if="row.YesterdayAverageVolumeStart > 0">
                {{ dataView(row, ['YesterdayAverageVolumeStart', 'YesterdayAverageVolumeStart']) }}
                 <i class="el-icon-caret-top" style="color: red"
                    v-if="row?.YesterdayAverageVolumeStart>(row?.DayBeforeAverageVolumeStart||0)"/>
                    <i class="el-icon-caret-bottom" style="color: green"
                       v-if="row?.YesterdayAverageVolumeStart<(row?.DayBeforeAverageVolumeStart||0)"/>
            </span>
            <span v-else class="font-s12">无数据</span>
        </div>
    </div>
</template>
<script>
import myUtils from "@/utils/utils";

export default {
    components: {},
    mixins: [],
    props: {
        row: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {}
    },
    computed: {
        conversion() {
            return (value) => {
                return myUtils.fnw(value)
            }
        },
        dataView() {
            return (resData, keys) => {
                return myUtils.dataView(resData, keys)
            }
        },
        dataList() {
            return [{
                type: '场观',
                yesterday: this.conversion(this.row?.YesterdayAverageObservationNum),
                before: this.conversion(this.row?.DayBeforeAverageObservationNum)
            }, {
                type: '销售额',
                yesterday: `${this.conversion(this.row?.YesterdayAverageVolumeStart)} - ${this.conversion(this.row?.YesterdayAverageVolumeEnd)}`,
                before: `${this.conversion(this.row?.DayBeforeAverageVolumeStart)} - ${this.conversion(this.row?.DayBeforeAverageVolumeEnd)}`,
            }]
        }
    },
    watch: {},
    methods: {},
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
.averageTable {
    .el-table__cell {
        border: none !important;
    }

    &::before {
        height: 0 !important;
    }
}
</style>
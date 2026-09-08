<template>
    <div class="averageLive">
        <template>
            <el-table
                :data="record"
                
                cell-class-name="table-cell-custom"
                header-cell-class-name="table-header-cell-custom"
                :border="false"
                max-height="200"
                style="width: 100%">
                <el-table-column
                    label=""
                    width="200">
                    <template slot-scope="{row}">
                        <div style="margin-left: 5px">{{ row.recordDate }}</div>
                    </template>
                </el-table-column>
                <el-table-column
                    label="昨日平均"
                    width="80">
                    <template slot-scope="{row}">
                        {{ conversion(row.observationNum) || '-' }}
                    </template>
                </el-table-column>
                <el-table-column
                    label="前日平均"
                    width="80">
                    <template slot-scope="{row}">
                        <span v-if="isNotNilNumber(row,['volumeStart', 'volumeEnd'])">
                            {{ dataView(row, ['volumeStart', 'volumeEnd']) }}
                        </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
            </el-table>
        </template>
    </div>
</template>
<script>
import myUtils from '@/utils/utils'
import {isNumber} from 'lodash'

export default {
    components: {},
    props: {
        record: {
            type: Array,
            default: () => {
                return []
            }
        }
    },
    data () {
        return {}
    },
    computed: {
        conversion () {
            return (value) => {
                return myUtils.fnw(value)
            }
        },
        isNotNilNumber () {
            return (item, keys) => {
                const [a, b] = keys
                return isNumber(item[a]) && isNumber(item[b]) && item[a] >= 0 && item[b] >= 0
            }
        },
        dataView () {
            return (resData, keys) => {
                return myUtils.dataView(resData, keys)
            }
        },
    },
    watch: {},
    methods: {

    },
    created () {

    },
    mounted () {

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
.averageLive {
    ::v-deep(.table-cell-custom) {
        border: none !important;
    }

    ::v-deep(.table-header-cell-custom) {
        border: none !important;
    }

    ::v-deep(.el-table::before) {
        width: 0;
    }
}
</style>
<template>
    <div class="slice-analysis-tip">
        <div class="content">
            <el-table
                :show-header="false"
                :data="tableData"
                style="width: 100%;font-weight: 500">
                <el-table-column
                    prop="name"
                    width="70"
                    align="right"
                    label="名称">
                </el-table-column>
                <el-table-column
                    prop="info"
                    align="left"
                    label="信息">
                    <template slot-scope="{row,$index}">
                        <div class="flex items-center justify-between">
                            <div v-html="row.info"></div>
                            <div v-if="$index===0" class="cursor-pointer" @click="viewSliceVideo"
                                 style="color: var(--color-main)">查看切片
                            </div>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
        </div>
    </div>
</template>

<script>

import myUtils from "@/utils/utils";

export default {
    components: {},
    props: {
        sliceVideo: {
            type: Object,
            default: () => {
                return {}
            }
        },
        sentenceMarkData: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            tableData: []
        }
    },
    computed: {},
    watch: {
        sliceVideo: {
            immediate: true,
            deep: true,
            handler(newVal) {
                const {videoInfo} = this.sentenceMarkData
                const dif = (newVal.endMillisecond - newVal.startMillisecond) / 1000
                if (newVal) {
                    this.tableData = [{
                        name: '类型：',
                        info: [0].includes(newVal.sliceType) ? '切片复盘' : '短视频切片'
                    }, {
                        name: '切片名称：',
                        info: newVal.sliceVideoName
                    }, {
                        name: '切片时间：',
                        info: `<div class="flex">
                                    <div>${myUtils.toformatTimeChinse(newVal.startMillisecond)} - ${myUtils.toformatTimeChinse(newVal.endMillisecond)}</div>
                                    <div style="width: 30px"></div>
                                    <div>${myUtils.toformatTimeChinse(dif * 1000)}</div>
                             </div>`
                    }]
                }
            }
        }
    },
    methods: {
        viewSliceVideo() {
            const {sourceId, sliceType} = this.sliceVideo
            this.$router.push({
                path: [0].includes(sliceType) ? '/section/analysis' : '/short/analysis',
                query: {id: sourceId}
            })
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
.slice-analysis-tip {
    width: 320px;
    font-family: Arial, sans-serif;
    padding: 12px;
    background: #fff;
    border-radius: 4px;

    .content {

        ::v-deep(.el-table td.el-table__cell) {
            border: none;
            padding: 2px 0;
            vertical-align: top;
        }

        ::v-deep(.el-table::before) {
            height: 0
        }

        ::v-deep(.el-table .cell) {
            padding: 0;
            font-size: 14px;
        }
    }
}
</style>

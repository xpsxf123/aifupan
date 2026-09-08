<template>
    <div class="table-dashboard">
        <div :style="{minHeight: tableDataLoading?'180px':''}">
            <template v-if="renderType==='f'">
                <TableContrast v-if="tableData.length&&!tableDataLoading" :tableData="tableData"/>
            </template>

            <template v-if="renderType==='g'">
                <CrowdContrast v-if="tableData.length&&!tableDataLoading" :tableData="tableData"/>
            </template>

            <template v-if="renderType==='h'">
                <FlowContrast v-if="tableData.length&&!tableDataLoading" :tableData="tableData"/>
            </template>
        </div>
        <Empty v-if="tableData.length === 0&&!tableDataLoading&&isEmptyType===-1" :isEmptyType="isEmptyType"/>
        <div v-if="isAuthenticated"
             :class="disabledBtn?['nimble-btn-dis','nimble-btn']:['nimble-btn-use','nimble-btn']">
            <span :style="{color:disabledBtn?'#ABAEB3':'#0077FF'}" @click="toAiContrast">AI对比数据</span>
        </div>
        <div/>
    </div>
</template>

<script>
import Empty from './empty.vue'
import FlowContrast from './table/flowContrast.vue'
import CrowdContrast from './table/crowdContrast.vue'
import TableContrast from './table/tableContrast.vue'

export default {
    components: {
        FlowContrast, CrowdContrast, TableContrast,
        Empty
    },
    props: {
        requestId: {
            type: String,
            default: ''
        },
        renderType: {
            type: String,
            default: 'f'
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
            tableData: [],
            isEmptyType: '',
            tableDataLoading: false,
        }
    },
    computed: {
        disabledBtn() {
            return this.tableDataLoading || this.tableData.length === 0 || this.tableData?.some(item => item.dataStatus !== 1)
        },
        isAuthenticated() {
            const videoInfo1 = this.sentenceMarkData?.data1?.videoInfo || {}
            const videoInfo2 = this.sentenceMarkData?.data2?.videoInfo || {}
            return (videoInfo1?.UserId && videoInfo2?.UserId) ? this.$auth(videoInfo1?.UserId) && this.$auth(videoInfo2?.UserId) : true
        }
    },
    watch: {
        requestId: {
            handler(newVal) {
                if (newVal) {
                    this.getInfoByContrastId(newVal)
                }
            },
            immediate: true
        }
    },
    methods: {
        getInfoByContrastId(id) {
            this.tableDataLoading = true
            this.$httpBack.v2400.infoByContrastId({
                contrastId: id
            }).then(res => {
                if (res.code === 0) {
                    const {videoDataViewing1, videoDataViewing2} = res.data || {}
                    const [dataStatus1, dataStatus2] = [videoDataViewing1?.dataStatus, videoDataViewing2?.dataStatus]
                    if (!videoDataViewing1 && !videoDataViewing2) {
                        this.isEmptyType = -1
                        return
                    }
                    const data1 = [1, 8].includes(dataStatus1) ? videoDataViewing1 : {}
                    const data2 = [1, 8].includes(dataStatus2) ? videoDataViewing2 : {}
                    this.tableData = [{
                        ...data1,
                        isTakeProduct: videoDataViewing1?.isTakeProduct ?? 1,
                        dataStatus: videoDataViewing1?.dataStatus ?? -1,
                        AnchorAvatar: this.sentenceMarkData?.data1?.anchorInfo?.AnchorAvatar ?? '',
                        AnchorName: this.sentenceMarkData?.data1?.anchorInfo?.AnchorName ?? '',
                        watchUserPortrait: JSON.parse(videoDataViewing1?.watchUserPortrait || '{}'),
                        watchFlowList: JSON.parse(videoDataViewing1?.watchFlowList || '[]')
                    }, {
                        ...data2,
                        isTakeProduct: videoDataViewing2?.isTakeProduct ?? 1,
                        dataStatus: videoDataViewing2?.dataStatus ?? -1,
                        AnchorAvatar: this.sentenceMarkData?.data2?.anchorInfo?.AnchorAvatar ?? '',
                        AnchorName: this.sentenceMarkData?.data2?.anchorInfo?.AnchorName ?? '',
                        watchUserPortrait: JSON.parse(videoDataViewing2?.watchUserPortrait || '{}'),
                        watchFlowList: JSON.parse(videoDataViewing2?.watchFlowList || '[]')
                    }]
                }
            }).finally(() => {
                this.tableDataLoading = false
            })
        },
        toAiContrast() {
            if (this.disabledBtn) return
            this.$emit('aIContrastData', 'dataBoard')
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
.table-dashboard {
    font-weight: 500;
    padding-bottom: 12px;
    min-height: 230px;

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

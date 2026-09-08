<template>
    <div class="yesterdayRecordList">
        <template>
            <el-table
                :data="record"
                
                cell-class-name="table-cell-custom"
                header-cell-class-name="table-header-cell-custom"
                :border="false"
                max-height="200"
                style="width: 100%">
                <el-table-column
                    label="昨日场次及时间"
                    width="180">
                    <template slot-scope="{row}">
                        <div style="display: flex;align-items: center;">
                            <div
                                :style="{height: '6px',width: '6px',borderRadius: '50%',background: getRandomRgbColor()}"></div>
                            <span style="margin-left: 5px">{{ row.recordDate }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column
                    label="场观"
                    width="80">
                    <template slot-scope="{row}">
                        {{ conversion(row.observationNum) || '-' }}
                    </template>
                </el-table-column>
                <el-table-column
                    label="销售额"
                    width="160">
                    <template slot-scope="{row}">
                        <span v-if="isNotNilNumber(row,['volumeStart', 'volumeEnd'])">
                            {{ dataView(row, ['volumeStart', 'volumeEnd']) }}
                        </span>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column
                    label="操作"
                    width="80">
                    <template slot-scope="{row}">
                        <el-button @click="viewDetail(row)" type="text" size="small">查看</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </template>
    </div>
</template>
<script>
import myUtils from '@/utils/utils'
import {isNumber} from 'lodash'
import viewVideo from '@/mixins/viewVideo'

export default {
    components: {},
    mixins: [viewVideo],
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
        getRandomRgbColor () {
            const r = Math.floor(Math.random() * 256)
            const g = Math.floor(Math.random() * 256)
            const b = Math.floor(Math.random() * 256)
            return `rgb(${r}, ${g}, ${b})`
        },
        viewDetail(item) {
            if (!item.videoId) return this.$message.error('获取视频信息失败')
            this.$httpBack.video.clientGetVideoByVideoId({
                videoId: item.videoId
            }).then(res => {
                if (res.code !== 0) return this.$message.error('获取视频信息失败')
                const result = res.data || {}

                if (this.isShowViewVideo(result) || this.isShowScript(result) || this.isAnalysisFail(result)) {
                    this.$router.push({
                        path: '/replay/analysisReadonly/video',
                        query: {id: result.videoId, analysisStatus: result.analysisStatus}
                    })
                }

                if (this.isShowViewAnalysis(result)) {
                    this.$router.push({
                        path: '/replay/analysis',
                        query: {id: result.videoId}
                    })
                }
            })
        }
    },
    created () {

    },
    mounted () {

    },
    beforeCreate () {}, //生命周期 - 创建之前
    beforeMount () {}, //生命周期 - 挂载之前
    beforeUpdate () {}, //生命周期 - 更新之前
    updated () {}, //生命周期 - 更新之后
    beforeDestroy () {}, //生命周期 - 销毁之前
    destroyed () {}, //生命周期 - 销毁完成
    activated () {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.yesterdayRecordList {
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
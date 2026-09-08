<template>
    <div class="h100 main-bg">
        <CoreTable
            :menuConfig="menuConfig"
            :tableList="exampleAnalysisList"
            notPage
            notSearch
            ref="table"
            :column="column"
            :table-config="{
                'show-overflow-tooltip': false
            }"
        >
            <template #anchorName="{row:item}">
                <Anchor
                    :item="{...item.anchorInfo, ...getMark(item)}"
                    :notLiveStatus="true"></Anchor>
            </template>
            <template #VideoName="{row:item}">
                <div class="fileNameContainer">
                    <div class="videoNameText slh">{{ item.videoName }}</div>
                    <div class="compereNameText slh">{{ item.vedioSizie + 'M' }} | {{ item.durationStr }}</div>
                </div>
            </template>
            <template #startTime="{row:item}">
                <div class="fileSizeColContainer" style="flex-direction: column;">
                    <div>{{ item.startTime.substring(0, 16) }}</div>
                    <div>{{ item.endTime.substring(0, 16) }}</div>
                </div>
            </template>
            <template #analysisTime="{row:item}">
                <div class="analysisDateColContainer">
                    <div>{{ item.analysisTime.substring(0, 10) }}</div>
                    <div>{{ item.analysisTime.substring(10) }}</div>
                </div>
            </template>
        </CoreTable>
        <Summary ref="summary"></Summary>
        <div class="font-s14 text-center text-colorErr">暂无录制直播，点击可查看案例详情。</div>
    </div>
</template>

<script>
import CoreTable from '@/components/coreTable/index.vue'
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue'
import table from '@/mixins/table'
import commonHttp from '@/mixins/commonHttp'
import viewVideo from '@/mixins/viewVideo';
import exampleMixin from '@/mixins/exampleMixin';
import summaryMixin from '@/components/summary/mixin';
export default {
    name: 'ReplayClientAnalysisFinishList',
    components: {
        CoreTable,
        Anchor
    },
    mixins: [table,commonHttp,viewVideo,summaryMixin,exampleMixin],
    props: {
        space: {
            type: String,
            default: ''
        }
    },
    data () {
        return {
            //
            column: [
                {
                    label: '主播',
                    prop: 'anchorName',
                    option: {
                        width: '160px'
                    }
                },
                {
                    label: '本地视频文件名',
                    prop: 'videoName',
                },
                {
                    label: '录制时间',
                    prop: 'startTime'
                },
                {
                    label: '话术',
                    option: {
                        render:(row) => {
                            return '<span class="text-color3">导出</span>'
                        },
                        on: {
                            click: (e, row) => {
                                this.$message({
                                    message: '案例不可导出话术，请录制直播后才可导出',
                                    type: 'success'
                                })
                            }
                        }
                    }
                },
                {
                    label: '小结',
                    formatter: (row) => {
                        return '查看';
                    },
                    option: {
                        classNameFn: (row)=>{
                            return  row.notesSummary?'cs-p': 'text-color3';
                        },
                        on: {
                            click: (e, row) => {
                                if(row.notesSummary){
                                    this.onClickSummary(row);
                                }
                            }
                        }
                    }
                },
                {
                    label: 'AI证断报告',
                    option: {
                        render:(row) => {
                            return '<span class="text-color3">生成诊断</span>'
                        },
                        on: {
                            click: (e, row) => {
                                this.$message({
                                    message: '案例不可生成证断，请录制直播后可自行生成证断报告',
                                    type: 'success'
                                })
                            }
                        }
                    }
                }
            ],
            //
            menuConfig: {
                width: '105px',
                options: [
                    {
                        label: '查看案例',
                        prop: 'viewExample',
                        type: 'primary',
                        click: (row) => {
                            this.viewExample(row)
                        }
                    }
                ]
            },
        }
    },

    mounted () {
    },
    computed: {
         getMark() {
            return (item) => {
                return {
                    videoRename: item.videoRename,
                    existBarrage: item.existBarrage,
                    existDataBoard: item.existDataBoard,
                    hasDiagnosisReport: item.hasDiagnosisReport || item.hasDataDiagnosisReport,
                    existsMark: item.existsMark,
                    existsNotes: item.existsNotes,
                    notesSummary: item.notesSummary
                }
            }
        }
    },
    methods: {
        // 查看分析结果
        viewExample(row) {
            const {videoSliceType} = row
            this.$router.push({
                path: videoSliceType === 1 ? '/online/sliceOnline/analysis' : '/online/onlineVideo/analysis',
                query: {
                    id: row.videoId,
                    type: 'example'
                }
            })
        },
    },
    activated () {

    },
}
</script>

<style scoped lang="less">

</style>
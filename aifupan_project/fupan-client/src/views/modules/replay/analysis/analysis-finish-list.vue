<template>
    <div>
        <CoreTable
            :searchConfig="formConfig"
            :menuConfig="menuConfig"
            :getDataApi="getTableList"
            ref="table"
            :column="column"
            buffer="analysisFinishList"
            :table-height="`calc(100vh - ${contrastList.length ? 262 : 210}px)`"
            :table-config="{
                'show-overflow-tooltip': false
            }"
        >
            <template #tableTop>
                <div v-if="contrastList.length" class="contrast-box main-bg">
                    <el-tag v-for="item in contrastList" closable @close="cancelContrast(item)" class="brs-40">
                        {{ item.videoName }}
                    </el-tag>
                    <afp-button v-show="contrastList.length>=2"  type="primary" @click="contrastSubmit">
                        开始对比
                    </afp-button>
                </div>
            </template>
            <template #anchorName="{row:item}">
                <Anchor
                    :item="{...item.anchorInfo,...getMark(item)}"
                    @editFileName="()=>editFileName(item,getList)"
                    :uploadStatus="item.uploadStatus" :notLiveStatus="true"></Anchor>
            </template>
            <template #VideoName="{row:item}">
                <div class="fileNameContainer">
                    <div class="videoNameText slh">{{ item.videoName }}</div>
                    <!-- <div class="compereNameText slh">{{ item.LiveTitle }}</div> -->
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
            <template #viewersNum="{row}">
                <div class="font-s14">
<!--                    <div>场观: {{ conversion(row?.observationNum) }}</div>-->
<!--                    <div>销售: {{ dataView(row, ['volumeStart', 'volumeEnd']) }}</div>-->
                    <Popover :item="row"/>
                </div>
            </template>
        </CoreTable>
        <DelVideoConfirm :visible.sync="delVideoVisible" @delFileType="delFileType"/>
    </div>
</template>

<script>
import CoreTable from '@/components/coreTable/index.vue'
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue'
import contrastMixin from '../../../../mixins/contrastMixin'
import table from '@/mixins/table'
import commonHttp from '@/mixins/commonHttp'
import viewVideo from '@/mixins/viewVideo'
import DelVideoConfirm from "@/views/modules/replay/component/delVideoConfirm.vue";
import myUtils from '@/utils/utils'
import Popover from "@/views/modules/dataAnalysis/component/common/popover.vue";
export default {
    name: 'ReplayClientAnalysisFinishList',
    components: {
        Popover,
        DelVideoConfirm,
        CoreTable,
        Anchor
    },
    mixins: [contrastMixin, table,commonHttp,viewVideo],
    props: {
        space: {
            type: String,
            default: ''
        },
        replayType:{
            type:String,
            default: ''
        }
    },
    data () {
        return {
            formConfig: {
                items: [
                    {
                        label: '', prop: 'secUid',
                        temp: 'Select',
                        config: {
                            filterable: true,
                            default: '',
                            options: [{
                                secUid: '',
                                anchorName: '全部'
                            }],
                            prop: {
                                label: 'anchorName',
                                value: 'secUid'
                            },
                            style: {
                                width: '170px'
                            }
                        },
                        on: {
                            change: true
                        }
                    },
                    {
                        label: '录制时间', prop: 'recordDate',
                        temp: 'DatePicker',
                        config: {
                            type: 'daterange',
                            valueFormat: 'yyyy-MM-dd',
                            style: {
                                width: '220px'
                            }
                        }
                    },
                    {
                        label: '分析时间', prop: 'analysisDate',
                        temp: 'DatePicker',
                        config: {
                            type: 'daterange',
                            valueFormat: 'yyyy-MM-dd',
                            style: {
                                width: '220px'
                            }
                        }
                    }
                ],
            },
            //
            column: [
                {
                    label: '主播',
                    prop: 'anchorName'
                },
                // {
                //     label: '本地视频文件名',
                //     prop: 'videoName',
                // },
                {
                    label: '录制时间',
                    prop: 'startTime'
                },
                {
                    label: '时长',
                    prop: 'durationStr',
                    option: {
                        render: (row) => {
                            return (`<div style="line-height: 20px">${myUtils.toformatTimeChinse(row.duration * 1000)}</div>`)
                        },
                    },
                },
                {
                    label: '文件大小',
                    prop: 'vedioSizie',
                    option: {
                        suffix: 'M'
                    }
                },
                {
                    label: '核心数据',
                    prop: 'viewersNum',
                    option: {
                        minWidth: 120
                    }
                },
                {
                    label: '最高在线',
                    prop: 'cruxNum',
                    formatter:(row)=>{
                        return this.formatTableValue(row?.onlineMaxNum)
                    }
                }
            ],
            //
            menuConfig: {
                width: '305px',
                options: [
                    {
                        label: '查看话术脚本',
                        type: 'success',
                        click: (item) => {
                            this.viewVideo(item, 'video')
                        },
                        show: (row) => {
                            return this.isShowScript(row)
                        },
                    },
                    {
                        label: '查看分析',
                        show: (row) => {
                            return this.isShowViewAnalysis(row)
                        },
                        click: (item) => {
                            this.lockAnalysis(item.videoId, item)
                        }
                    },
                    {
                        label: '加入对比',
                        show: (row) => {
                            return !this.contrastMap[row.videoId] && row.analysisStatus === 2
                        },
                        click: (row) => {
                            this.addContrast(row)
                        }
                    },
                    {
                        label: '取消对比',
                        type: 'danger',
                        show: (row) => {
                            return this.contrastMap[row.videoId] && row.analysisStatus === 2
                        },
                        click: (row) => {
                            this.cancelContrast(row)
                        }
                    },
                    [
                        {
                            label: '查看',
                            click: (item) => {
                                this.preview(item.videoId)
                            },
                            icon: 'icon-a-bukechakan2'
                        },
                        {
                            label: '删除',
                            type: 'danger',
                            click: (item) => {
                                // this.deleteFile([item])
                                this.delVideoVisible = true;
                                this.selectDelList = [item]
                            }
                        }
                    ]
                ]
            },
            contrastMap: {},

            // contrastList: [], // 对比列表
            fileList: [], // 视频列表
            selectDelList: [],
            delVideoVisible: false,
        }
    },

    mounted () {
        // this.getVideoList();
        this.getCList()
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
                    notesSummary: item.notesSummary,
                    localVideoStatus: item.localVideoStatus
                }
            }
        },
        dataView () {
            return (resData, keys, length) => {
                return myUtils.dataView(resData, keys, length)
            }
        },
        conversion () {
            return (value) => {
                return myUtils.fnw(value)
            }
        },
    },
    methods: {
        getList () {
            this.$refs.table.getList()
        },
        // 查看分析
        lockAnalysis (id, item) {
            this.$router.push({
                path: '/replay/analysis',
                query: {
                    id: id
                }
            })
        },
        // 预览
        preview (videoId) {
            this.$httpClient.video.preview({ videoId }).then((res) => {
                if (res.code == 0) {
                    window.open(res.data, '_blank')
                }
            })
        },
        delFileType(type){
            this.deleteFile({
                list: this.selectDelList,
                type,
            })
            this.delVideoVisible = false
            this.selectDelList = []
        },
        deleteFile({list, type}) {
            let ids = list.map(d => d.videoId)
            const httpServer = type === 0 ? this.$httpClient.video.deletebyids : this.$httpClient.video.deleteLocalVideoByIds
            httpServer(ids).then(res => {
                // this.$httpBack.video.clientDeleteVideo(ids).then(res=>{
                if (res.code == 0) {
                    this.$message.success('删除成功')
                    this.getList();
                    // this.$httpClient.video.deletebyids(ids).then(res => {})
                }
                this.appVnode?.getDisk()
            })
        },
        // 删除文件
        // deleteFile (list) {
        //     this.$confirm('将永久删除选中的视频, 是否继续?', '提示', {
        //         confirmButtonText: '确定',
        //         cancelButtonText: '取消',
        //         type: 'warning'
        //     }).then(() => {
        //         let ids = list.map(d => d.videoId);
        //         // this.$httpBack.video.clientDeleteVideo(ids).then(res=>{
        //             this.$httpClient.video.deletebyids(ids).then(res=>{
        //             if (res.code == 0) {
        //                 this.$message.success('删除成功')
        //                 this.getList();
        //                 // this.$httpClient.video.deletebyids(ids).then(res => {})
        //             }
        //             this.appVnode?.getDisk()
        //         })
        //     })
        // },
        // 获取主播列表
        async getCList () {
            let cList = await this.getCompereBackList();
            this.setFormConfigDic({0:cList.map(d=>{
                return d?.anchorInfo;
            })},this.formConfig,{
                secUid:(opts,form)=>{
                    return [].concat(form.config?.options?.shift(),opts);
                }
            })
        },
        tableHttp(param){
            let fileDataForm = {
                ...param,
                page: param.pageIndex,
                limit: param.pageSize,
                secUid: param.secUid || '',
                analysisStatus: 2,
                recordStartDate: param.recordDate?.[0] || '',
                recordEndDate: param.recordDate?.[1] || '',
                analysisStartDate: param.analysisDate?.[0] || '',
                analysisEndDate: param.analysisDate?.[1] || ''
            }
            delete fileDataForm.recordDate
            delete fileDataForm.analysisDate
            return {
                http: this.$httpBack.video.clientVideoList,
                param: fileDataForm,
                isVideoFormat: true
            }
        },
    },
    activated () {

    },
}
</script>

<style scoped lang="less">

.fileSizeColContainer {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}

.analysisDateColContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 0.8;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}


.compereNameText {
    font-weight: 400;
    font-size: 13px;
    color: #677583;
    margin-top: 4px;

}

.videoNameText {
    font-weight: 400;
    font-size: 14px;
    color: #2E3742;
}

.fileNameContainer {
    display: flex;
    flex-direction: column;
    text-align: left;
    // white-space: pre-wrap;
}

.contrast-box {
    padding: 10px;
    margin: 0;

    > * {
        margin: 0 5px;
    }
}
</style>
<template>
    <div>
        <CoreTable v-if="loadingTable" :table-height="`calc(100vh - 210px)`" :buffer="isFullAnalysis?'analysisContrastList':'sliceContrastList'" :searchConfig="formConfig"
                   :menuConfig="menuConfig" :getDataApi="getContrastList" ref="table" :column="column"
                   :table-select="true" @api-finally="apiFinally"
                   @deletes="deletes" :table-config="{
                'show-overflow-tooltip': false
            }">
            <template #btns>
                <afp-button type="primary" plain @click="contrastDialog" size="default">新增对比分析</afp-button>
            </template>
            <template #videoOneInfo="{ row: item }">
                <AnchorName :anchorVideo="{...item.videoOneInfo,optimize:true,syncScene:item.syncScene}" :anchorInfo="item.anchorOneInfo"
                            :fileInfo="{...item.fileOneInfo,optimize:false,syncScene:item.syncScene}"></AnchorName>
            </template>
            <template #videoTwoInfo="{ row: item }">
                <AnchorName :anchorVideo="{...item.videoTwoInfo,optimize:false,syncScene:item.syncScene}" :anchorInfo="item.anchorTwoInfo"
                            :fileInfo="{...item.fileTwoInfo,optimize:false,syncScene:item.syncScene}"></AnchorName>
            </template>
            <template #updateDate="{ row: item }">
                <div class="analysisDateColContainer">
                    <div>{{ item.updateDate?.substring(0, 10) }}</div>
                    <div>{{ item.updateDate?.substring(10) }}</div>
                </div>
            </template>
            <template #empty="{ row: item }">
                <div class="analysisDateColContainer" v-if="!contrastListLoading">
                    <img src="@/assets/imgs/2_5_8/none-contrast.png" alt="">
                    <div>
                        <afp-button type="primary" :plain="false" size="default" @click="viewDemo">查看演示案例 <i class="el-icon-right"></i> </afp-button>
                        <afp-button type="primary" @click="contrastDialog" size="default">马上新增对比复盘</afp-button>
                    </div>
                </div>
                <div class="analysisDateColContainer" v-else>
                    <span>数据加载中，请稍等...</span>
                </div>
            </template>
        </CoreTable>

        <ContrastDialog ref="ContrastDialog" :compereList="compereList" :replayType="replayType" @contrastSubmit="getList"></ContrastDialog>
    </div>
</template>

<script>
import CoreTable from '@/components/coreTable/index.vue'
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue'
import AnchorName from '@/views/modules/replay/online/anchorName.vue'
import ContrastDialog from './contrastDialog.vue'
import tableMixin from '@/mixins/table.js'
import { omit } from 'lodash'
import myUtils from '@/utils/utils'
import commonHttp from '@/mixins/commonHttp'
import tabs from '@/mixins/tabs.js'
import AfpButton from "@/components/eleUi/button.vue";
export default {
    name: 'ReplayClientAnalysisFinishList',
    components: {
        AfpButton,
        CoreTable,
        Anchor,
        AnchorName,
        ContrastDialog
    },
    mixins: [tabs,tableMixin, commonHttp],
    props: {
        space: {
            type: String,
            default: ''
        },
        replayType:{
            type: String,
            default: ''
        }
    },
    data () {
        return {
            tabs: [],
            formConfig: {
                items: [
                    {
                        label: '', prop: 'secUid',
                        temp: 'Select',
                        config: {
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
                        }
                    },
                    {
                        label: '对比时间', prop: 'searchDate',
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
                    label: '对比1',
                    prop: 'videoOneInfo',
                    align: 'left',
                    // option: {
                    //     align: 'left'
                    // }
                },
                {
                    label: '对比2',
                    prop: 'videoTwoInfo',
                    align: 'left',
                },
                {
                    label: '对比时间',
                    prop: 'updateDate',
                },
            ],
            menuConfig: {
                width: '270px',
                options: [
                    {
                        label: '查看智能对比',
                        click: (item) => {
                            this.toContrast(item.contrastId)
                        }
                    },
                    {
                        label: 'AI助手',
                        click: (item) => {
                            this.toAIContrast(item.contrastId)
                        }
                    },
                    {
                        label: '变换定位',
                        click: (item) => {
                            this.toSwitchContrast(item.contrastId)
                        }
                    }
                ]
            },
            compereList: [],
            loadingTable: true,
            contrastListLoading:true,
        }
    },
    computed: {
        isFullAnalysis () {
            return this.getTabsName === 'fullAnalysis'
        },
    },
    watch:{
        getTabsName (newVal, oldVal) {
            if(newVal !== oldVal){
                this.loadingTable = false;
                this.$nextTick(() => {
                    this.loadingTable = true
                })
            }
        },
        '$route.path': {
            handler(val) {
                if (this.replayType === 'replayAll') {
                    this.tabs = [{
                        label: '整场复盘对比',
                        name: 'fullAnalysis'
                    }]
                } else {
                    this.tabs = [{
                        label: '切片复盘对比',
                        name: 'sliceAnalysis'
                    }]
                }
                this.initTabs('init')
            },
            immediate: true,
            deep: true
        }
    },
    mounted () {
        this.getCList()
    },

    methods: {
        deleteOpt () {
            return {
                http: this.isFullAnalysis? this.$httpBack.contrast.clientDeleteContrast : this.$httpBack.contrast.clientDeleteContrast,
                idKey: 'contrastId',
            }
        },
        contrastDialog () {
            this.$refs.ContrastDialog?.show()
        },
        viewDemo(){
            this.$httpClient.system.openUrl({url: ' https://ifupan.com/client/#/ai-share/2/a1b03478-13f9-46ba-8815-150a2e5186bd/4532727204709253120/0'});
        },
        clearBufferData () {
            this.$refs.table?.clearBufferData()
        },
        getList () {
            this.$refs.table.getList()
        },
        preview (videoId) {
            this.$httpClient.video.preview({ videoId }).then((res) => {
                if (res.code == 0) {
                    window.open(res.data, '_blank')
                }
            })
        },
        // 跳转到对比页
        toContrast (contrastId) {
            const levelPath = this.replayType === 'replayAll' ? '/contrastReplay' : '/contrastSection';
            if(this.isFullAnalysis){
                this.$router.push({ path: `${levelPath}/analysis`, query: { 'contrastId': contrastId } })
            }else{
                this.$router.push({ path: `${levelPath}/analysis`, query: { 'contrastId': contrastId } })
            }
        },
        toAIContrast (contrastId) {
            const levelPath = this.replayType === 'replayAll' ? '/contrastReplay' : '/contrastSection';
            if(this.isFullAnalysis){
                this.$router.push({ path: `${levelPath}/aiAnalysis`, query: { 'contrastId': contrastId } })
            }else{
                this.$router.push({ path: `${levelPath}/aiAnalysis`, query: { 'contrastId': contrastId } })
            }
        },
        toSwitchContrast(contrastId) {
            this.$httpBack.contrast.switchContrastPosition({contrastId}).then((res) => {
                if (res.code == 0) this.getList()
            })
        },
        // 获取主播列表
        async getCList () {
            let cList = await this.getCompereBackList();
            this.setFormConfigDic({0:cList.map(d=>{
                return d?.anchorInfo;
            })},this.formConfig,{
                secUid:(opts,form)=>{
                    let a = [].concat(form.config?.options?.shift(),opts);
                    this.compereList = a;
                    return a;
                }
            })
        },
        apiFinally(){
            this.contrastListLoading = false
        },
        // 获取对比页列表数据
        getContrastList (param) {
            this.contrastListLoading = true
            let requestData = {
                ...omit(param, ['pageIndex', 'pageSize']),
                page: param.pageIndex,
                limit: param.pageSize,
                anchorId: param.anchorId || '',
                contrastStartDate: param.searchDate?.[0] || undefined,
                contrastEndDate: param.searchDate?.[1] || undefined,
                sliceContrastType: 0
            }
            const {path} = this.$route
            if (path === '/contrastSection') {
                requestData.sliceContrastType = 1
            }
            delete requestData.searchDate
            let http = this.isFullAnalysis? this.$httpBack.contrast.clientContrastList : this.$httpBack.contrast.clientContrastList;
            return http(myUtils.httpFormat(requestData)).then(res => {
                if (res.code === 0) {
                    this.totalCount = res.data.totalPage
                    let contrastList = res.data.list
                    contrastList?.forEach(item => {
                        if (item.videoOneId&& item.videoTwoId) {
                            item.videoOneInfo.videoName = item.videoOneInfo.videoName.replaceAll('AF_', '')
                            item.videoOneInfo.videoName = item.videoOneInfo.videoName.replaceAll('.ts', '')
                            item.videoOneInfo.videoName = item.videoOneInfo.videoName.substring(item.videoOneInfo.videoName.indexOf('_') + 1)
                            item.videoTwoInfo.videoName = item.videoTwoInfo.videoName.replaceAll('AF_', '')
                            item.videoTwoInfo.videoName = item.videoTwoInfo.videoName.replaceAll('.ts', '')
                            item.videoTwoInfo.videoName = item.videoTwoInfo.videoName.substring(item.videoTwoInfo.videoName.indexOf('_') + 1)
                        }
                    })
                    // this.contrastList = contrastList;
                }
                return res
            })

            // return this.$httpClient.contrast.getcontrastpage(requestData).then(res => {
            //     if (res.code == 0) {
            //         this.totalCount = res.data.Total
            //         let contrastList = res.data.DataList
            //         contrastList?.forEach(item => {
            //             if (item.VideoOneId && item.VideoTwoId) {
            //                 item.anchorVideoOne.VideoName = item.anchorVideoOne.VideoName.replaceAll('AF_', '')
            //                 item.anchorVideoOne.VideoName = item.anchorVideoOne.VideoName.replaceAll('.ts', '')
            //                 item.anchorVideoOne.VideoName = item.anchorVideoOne.VideoName.substring(item.anchorVideoOne.VideoName.indexOf('_') + 1)
            //                 item.anchorVideoTwo.VideoName = item.anchorVideoTwo.VideoName.replaceAll('AF_', '')
            //                 item.anchorVideoTwo.VideoName = item.anchorVideoTwo.VideoName.replaceAll('.ts', '')
            //                 item.anchorVideoTwo.VideoName = item.anchorVideoTwo.VideoName.substring(item.anchorVideoTwo.VideoName.indexOf('_') + 1)
            //             }
            //         })
            //         // this.contrastList = contrastList;
            //     }
            //     return res
            // })
        },
    },
}
</script>

<style scoped lang="less">
.analysisDateColContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 2;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}
</style>
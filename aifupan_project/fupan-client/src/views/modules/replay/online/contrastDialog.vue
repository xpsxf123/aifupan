<template>
    <div>
        <drawer ref="drawer" class="drawer-table-box" title="新增对比分析" :visible.sync="drawerVisible">
            <CoreTable  :table-height="tableHeight" :searchConfig="formConfig"
                        :menuConfig="menuConfig" :getDataApi="getVideoList" ref="tableDialog" :column="column" :table-config="{
                'show-overflow-tooltip': false
            }">
                <template #tableTop>
                    <div v-if="contrastList.length" class="contrast-box main-bg mg-b6">
                        <el-tag v-for="item in contrastList" closable @close="cancelContrast(item)" class="mg-r6 brs-40">
                            {{ item.videoName }}
                        </el-tag>
                        <afp-button v-show="contrastList.length>=2"  type="primary" @click="modalContrast">开始对比</afp-button>
                    </div>
                </template>
                <template #AnchorName="{ row: item }">
                    <AnchorName :anchorVideo="item" :anchorInfo="item.anchorInfo"
                                :fileInfo="item"></AnchorName>
                </template>
            </CoreTable>
        </drawer>
        <ReviewContrast ref="review_contrast" @contrastSubmit="contrastSubmit" @swapObj="swapObj"/>
    </div>
</template>

<script>
import drawerMixin from '@/mixins/drawer.js'
import drawer from '@/components/drawer/index.vue'
import CoreTable from '@/components/coreTable/index.vue'
import contrastMixin from '@/mixins/contrastMixin'
import myUtils from '@/utils/utils'
import AnchorName from '@/views/modules/replay/online/anchorName.vue';
import commonHttp from '@/mixins/commonHttp';
// import { omit } from 'lodash'
export default {
    components: { CoreTable, AnchorName, drawer},
    mixins: [drawerMixin, contrastMixin, commonHttp],
    props:{
        replayType:{
            type: String,
            default: ''
        }
    },
    data() {
        return {
            formConfig: {
                items: [
                    {
                        label: '', prop: 'secUid',
                        temp: 'Select',
                        config: {
                            default: '',
                            filterable: true,
                            clearable: true,
                            options: [{
                                secUid: '',
                                anchorName: "全部"
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
                            change:true
                        }
                    },
                    {
                        label: '录制时间', prop: 'recordDate',
                        temp: 'DatePicker',
                        config: {
                            type:"daterange",
                            valueFormat:"yyyy-MM-dd",
                            style: {
                                width: '220px'
                            }
                        }
                    },
                ],
            },
            //
            column: [
                {
                    label: '主播',
                    prop: 'AnchorName'
                },
                {
                    label: '录制时间',
                    prop: 'startTime'
                },
                {
                    label: '视频时长',
                    prop: 'duration'
                },
            ],
            //
            menuConfig: {
                width: '110px',
                options: [
                    {
                        label: "加入对比",
                        show: (row) => {
                            return !this.contrastMap[row.videoId] && row.analysisStatus === 2
                        },
                        click: (row) => {
                            this.addContrast(row)
                        }
                    },
                    {
                        label: "取消对比",
                        type: 'danger',
                        show: (row) => {
                            return this.contrastMap[row.videoId] && row.analysisStatus === 2
                        },
                        click: (row) => {
                            this.cancelContrast(row)
                        }
                    },
                ]
            },
            fileDataForm: {},
            contrastMap: {},
            compereList: []
        };
    },
    computed: {
        tableHeight() {
            return this.contrastList.length ? `calc(100vh - 228px)` : `calc(100vh - 190px)`
        }
    },
    watch: {
        drawerVisible:{
            handler(val) {
                if(!val){
                    this.contrastMap = {};
                }
            },
            immediate: true
        }
    },
    methods: {
        async getCList () {
            let cList = await this.clientTenantAnchorList();
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
        contrastSubmitHttp(param){
            return this.$httpBack.contrast.clientAddCloudContrast(param);
        },
        contrastSubmitCallback(){
            this.hide();
            this.$emit('contrastSubmit')
        },
        async showCallback(){
            await this.getCList();
            this.$nextTick(()=>{
                this.$refs.tableDialog.getList();
            })
        },
        // 获取主播视频列表
        getVideoList(param) {
            let fileDataForm = {
                page: param.pageIndex,
                limit: param.pageSize,
                recordStartDate: param.recordDate?.[0] || '',
                recordEndDate: param.recordDate?.[1] || '',
                analysisStartDate: param.analysisDate?.[0] || '',
                analysisEndDate: param.analysisDate?.[1] || '',
                ...this.drawerData,
                ...param
            };

            // const name = fileDataForm?.secUidName;
            // const secUidArrConfigOpt = this.formConfig?.items?.find(d=>d.prop==='secUidName')?.config?.options || [];
            // const ls = secUidArrConfigOpt?.filter(d=>{
            //     return d.anchorName.indexOf(name) >= 0
            // }).map(d=>d.secUid);
            // fileDataForm.secUidArr =name ? ls?.length ? ls : [999] : '';

            return this.$httpBack.video.clientListCloudVideo(fileDataForm, {load: false}).then((res) => {
                if (res.code == 0) {
                    this.totalCount = res.data.totalCount;
                    let fileList = res.data.list || [];
                    fileList?.forEach(item => {
                        item.duration = myUtils.toformatTime(item.duration * 1000);
                        item.vedioSizie = myUtils.retainDecimals(item.vedioSizie / 1024 / 1024);
                        // 计算时长
                        let second = myUtils.toSecondByDate(item.endTime) - myUtils.toSecondByDate(item.startTime);
                        item.durationStr = myUtils.toformatTimeChinse(second * 1000);
                        item.reportFileName = item.videoName.replace(/_[^_]+\.ts$/, '')
                        item.videoName = item.videoName.replaceAll("AF_", "");
                        item.videoName = item.videoName.replaceAll(".ts", "");
                        item.videoName = item.videoName.substring(item.videoName.indexOf("_") + 1);
                    })
                    this.fileList = fileList;
                }
                return res;
            })
            // let fileDataForm = {
            //     ...omit(param, ['pageIndex', 'pageSize']),
            //     page: param.pageIndex,
            //     limit: param.pageSize,
            //     secUid: param.secUid === -1 ? '' : param.secUid,
            //     analysisStatus: 2,
            //     recordStartDate:param.recordDate?.[0] || '',
            //     recordEndDate:param.recordDate?.[1] || '',
            //     analysisStartDate:param.analysisDate?.[0] || '',
            //     analysisEndDate:param.analysisDate?.[1] || '',
            //     tradeId: ''
            // };
            // delete fileDataForm.recordDate;
            // delete fileDataForm.analysisDate;

            // return this.$httpBack.video.clientVideoList(fileDataForm).then((res) => {
            //     if (res.code === 0) {
            //         res.data.list?.forEach(item => {
            //             item.duration = myUtils.toformatTime(item.duration * 1000);
            //             item.vedioSizie = myUtils.retainDecimals(item.vedioSizie / 1024 / 1024);
            //             // 计算时长
            //             let second = myUtils.toSecondByDate(item.endTime) - myUtils.toSecondByDate(item.StartTime);
            //             item.DurationStr = myUtils.toformatTimeChinse(second * 1000);

            //             item.videoName = item.videoName.replaceAll("AF_", "");
            //             item.videoName = item.videoName.replaceAll(".ts", "");
            //             item.videoName = item.videoName.substring(item.videoName.indexOf("_") + 1);
            //         })
            //     }
            //     return res;
            // })
        },
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
</style>
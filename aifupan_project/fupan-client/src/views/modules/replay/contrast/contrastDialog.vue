<template>
    <div>
        <drawer ref="drawer" class="drawer-table-box" title="新增对比分析" :visible.sync="drawerVisible">
            <CoreTable :table-height="tableHeight" :searchConfig="formConfig"
                       :menuConfig="menuConfig" :getDataApi="getVideoList" ref="tableDialog" :column="column"
                       :table-config="{
                'show-overflow-tooltip': false
            }">
                <template #tableTop>
                    <div v-if="contrastList.length" class="contrast-box main-bg mg-b6">
                        <el-tag v-for="item in contrastList" closable @close="cancelContrast(item)"
                                class="mg-r6 brs-40">
                            {{ item.videoName }}
                        </el-tag>
                        <afp-button v-show="contrastList.length>=2" type="primary" @click="modalContrast">开始对比
                        </afp-button>
                    </div>
                </template>
                <template #AnchorName="{ row: item }">
                    <AnchorName :anchorVideo="item" :anchorInfo="item.anchorInfo"
                                :fileInfo="item"></AnchorName>
                </template>
                <template #viewersNum="{row}">
                    <div class="font-s14">
                        <Popover :item="row"/>
                    </div>
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
import { omit } from 'lodash'
import Popover from "@/views/modules/dataAnalysis/component/common/popover.vue";
export default {
    components: {Popover, CoreTable, AnchorName, drawer},
    mixins: [drawerMixin, contrastMixin],
    props:{
        compereList: {
            type:Array,
            default:()=>{[]}
        },
        replayType:{
            type:String,
            default:''
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
                            options: [{
                                SecUid: '',
                                AnchorName: "全部"
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
                    prop: 'startTime',
                    hidden: ()=>{
                        return this.columnHidden(['/contrastSection'])
                    },
                    option: {
                        render: (row) => {
                            return (`<div class="recordColContainer" style="padding:15px 0 16px 0">
                                <div>${row?.startTime}</div>
                                <div>${row?.duration}</div>
                            </div>`)
                        },
                    },
                },
                {
                    label: '切片时间',
                    prop: 'analysisTime',
                    hidden: ()=>{
                        return this.columnHidden(['/contrastReplay'])
                    },
                    option: {
                        render: (row) => {
                            return (`<div class="recordColContainer" style="padding:15px 0 16px 0">
                                <div>${row?.analysisTime?.substring(0, 16)}</div>
                                <div>${row?.duration}</div>
                            </div>`)
                        },
                    },
                },
                // {
                //     label: '视频时长',
                //     prop: 'duration',
                //     hidden: ()=>{
                //         return this.columnHidden(['/contrastSection'])
                //     },
                // },
                // {
                //     label: '切片时长',
                //     prop: 'duration',
                //     hidden: ()=>{
                //         return this.columnHidden(['/contrastReplay'])
                //     },
                // },
                {
                    label: '核心数据',
                    prop: 'viewersNum'
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
        };
    },
    computed: {
        tableHeight() {
            return this.contrastList.length ? `calc(100vh - 228px)` : `calc(100vh - 190px)`
        }
    },
    watch: {},
    methods: {
        columnHidden(items){
            const { path } = this.$route
            return items.includes(path)
        },
        contrastSubmitCallback(){
            this.hide();
            this.$emit('contrastSubmit')
        },
        showCallback(){
            this.$set(this.formConfig?.items[0]?.config, 'options', [...this.compereList]);
            this.$nextTick(()=>{
                this.$refs.tableDialog.resetList();
            })
        },
        // 获取主播视频列表
        getVideoList(param) {
            const {path} = this.$route
            let fileDataForm = {
                ...omit(param, ['pageIndex', 'pageSize']),
                page: param.pageIndex,
                limit: param.pageSize,
                secUid: param.secUid === -1 ? '' : param.secUid,
                analysisStatus: 2,
                recordStartDate:param.recordDate?.[0] || '',
                recordEndDate:param.recordDate?.[1] || '',
                analysisStartDate:param.analysisDate?.[0] || '',
                analysisEndDate:param.analysisDate?.[1] || '',
                tradeId: '',
                videoSliceType: path === '/contrastReplay' ? 0 : 1
            };
            delete fileDataForm.recordDate;
            delete fileDataForm.analysisDate;

            return this.$httpBack.video.clientVideoList(fileDataForm).then((res) => {
                if (res.code === 0) {
                    res.data.list?.forEach(item => {
                        item.duration = myUtils.toformatTime(item.duration * 1000);
                        item.vedioSizie = myUtils.retainDecimals(item.vedioSizie / 1024 / 1024);
                        // 计算时长
                        let second = myUtils.toSecondByDate(item.endTime) - myUtils.toSecondByDate(item.StartTime);
                        item.DurationStr = myUtils.toformatTimeChinse(second * 1000);

                        item.videoName = item.videoName.replaceAll("AF_", "");
                        item.videoName = item.videoName.replaceAll(".ts", "");
                        item.videoName = item.videoName.substring(item.videoName.indexOf("_") + 1);
                    })
                }
                return res;
            })
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
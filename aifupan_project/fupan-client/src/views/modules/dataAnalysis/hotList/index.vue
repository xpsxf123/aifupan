<template>
    <div class="main-bg h100">
        <div class="font-s16 text-colorMain font-w500 pd-l16 pd-r16 pd-t16">优秀账号热度榜: <span class="text-colorTheme">{{ getLabel }}</span></div>
        <CoreTable v-if="!isHintMsg" notBuffer 
        :searchConfig="formConfig"
        :menuConfig="menuConfig" 
        :getDataApi="getTableList" 
        :resData="tableResData" 
        ref="tableDialog" 
        :column="column"
         @api-finally="apiFinally"
          table-height="calc(100vh - 250px)">
           <template #rank="{ row }">
                <div v-if="row.rank <= 3" class="rank-icon flex-ji-c">
                    <img :src="require(`@/assets/imgs/hotList/${row.rank}.png`)" :alt="`第${row.rank}名`" style="width: 24px; height: 24px;" />
                </div>
                <div v-else class="rank-text">
                    {{ row.rank }}
                </div>
           </template>
           <template #anchorName="{ row }">
                <div class="pd-t4 pd-b4 slh">
                    <Anchor :item="{
                        anchorAvatar: row.anchorAvatar,
                        anchorName: row.anchorName,
                        remarksName: row.remarksName,
                        liveStatus: row.liveStatus || 4
                    }" :notLiveStatus="true" @click="()=>{handleClickAnchor(row)}">
                        <template #anchorBottom>
                            <span class="font-s12 text-color3" style="display: block;">账号：{{ row.anchorPlatformAccount }}</span>
                        </template>
                    </Anchor>
                </div>
           </template>
        </CoreTable>
        <div v-else class="pd-16 font-s16 text-color3 flex-ji-c" style="height: calc(100vh - 190px);">
            <div>
                <div class="text-center">
                    <img src="@/assets/imgs/notRoot.png" alt=""></img>
                </div>
                 <div>《优秀账号热度榜》只有企业版及以上会员才能查看, 请<el-button type="text" class="font-s20" @click="toUpgrade">咨询产品顾问</el-button>后查看。</div>
            </div>
        </div>
        <!-- 监控抽屉 -->
        <drawer ref="monitorDrawer" title="加入监控" :visible.sync="drawerVisible" width="60%">
            <div class="monitor-content">
                <CompereForm 
                    ref="compereFormRef"
                    :tradeTreeList="tradeTreeList"
                    :tabName="'douyin'"
                    labelPosition="left"
                    :notUrls="true"
                    :defaultTredeId="$route.query.id"
                    @addCompere="handleAddCompere"
                    @onIsAiReportChange="handleIsAiReportChange"
                ></CompereForm>
            </div>
        </drawer>
        
        <!-- AI报告组件 -->
        <AiDataReport ref="ai_data_report" @initAiReport="initAiReport"
                  @setDiagnosisParams="setDiagnosisParams"
                  :diagnosisParams="diagnosisParams" reportType="anchor"/>


        <AddCompereStatusDialog ref="statusDialog" @toCompereList="toHome" @cleseStatus="closeStatusDialog"></AddCompereStatusDialog>

    </div>
</template>

<script>
import table from '@/mixins/table.js';
import utils from '@/utils/utils.js';
import drawer from '@/components/drawer/index.vue';
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue';
import CompereForm from '@/views/modules/addCompere/common/compereForm.vue';
import AiDataReport from '@/views/commonComponent/aiReport/aiDataReport.vue';
import { cloneDeep, omit } from 'lodash';
import { PLATFORM_ENUM } from '@/enum/index.js';
import AddCompereStatusDialog from '@/views/modules/addCompere/common/addCompereStatusDialog.vue'
export default {
    components: {
        drawer,
        Anchor,
        CompereForm,
        AiDataReport,
        AddCompereStatusDialog
    },
    props:{
        
    },
    mixins: [table],
    data() {
        return {
            formConfig: {
                    items:[
                    {
                        label: '直播间名称', prop: 'anchorName',
                        temp: 'Input', config: {placeholder: '请输入直播间名称'}
                    },    
                    {
                        label: '关键词', prop: 'liveKeyword',
                        temp: 'Input', config: {placeholder: '请输入关键词'}
                    },
                    ]
                },
            isHintMsg: false,
            // 抽屉显示状态
            drawerVisible: false,
            // 当前选中的监控项目
            currentMonitorItem: null,
            // 行业树形数据
            tradeTreeList: [],
            // AI诊断参数
            diagnosisParams: {},
            // 表格列配置
            column: [
                {
                    label: '排名',
                    prop: 'rank',
                    option: {
                        align: 'center'
                    }
                },
                {
                    label: '直播间',
                    prop: 'anchorName',
                    option: {
                        tooltip: true,
                        minWidth: 100
                    }
                },
                {
                    label: '账号热度',
                    prop: 'accountHeat',
                    option: {
                        align: 'center'
                    },
                    formatter: (row) => {
                        if (!row || typeof row.accountHeat === 'undefined') {
                            return '-';
                        }
                        return utils.toLocale(row.accountHeat, { autoFormat: true });
                    }
                },
                {
                    label: '粉丝量',
                    prop: 'followerCount',
                    option: {
                        align: 'center'
                    },
                    formatter: (row) => {
                        if (!row || typeof row.followerCount === 'undefined') {
                            return '-';
                        }
                        return utils.toLocale(row.followerCount, { autoFormat: true });
                    }
                },
                {
                    label: '平均场观',
                    prop: 'liveAverageUser',
                    option: {
                        align: 'center'
                    },
                    formatter: (row) => {
                        if (!row || typeof row.liveAverageUser === 'undefined') {
                            return '-';
                        }
                        return utils.toLocale(row.liveAverageUser>0?row.liveAverageUser:0, { autoFormat: true });
                    }
                },
                {
                    label: '销售额',
                    prop: 'totalAmount',
                    option: {
                        align: 'center'
                    },
                    formatter: (row) => {
                        if (!row || typeof row.totalAmount === 'undefined') {
                            return '-';
                        }
                        return row.totalAmount
                    }
                },
                {
                    label: '平台',
                    prop: 'anchorPlatformType',
                    option: {
                        align: 'center'
                    },
                    formatter: (row) => {
                        if (!row || !row.anchorPlatformType) {
                            return '-';
                        }
                        const platformMap = {
                            '1': '抖音',
                            '2': '快手', 
                            '3': '视频号'
                        };
                        return platformMap[row.anchorPlatformType] || row.anchorPlatformType;
                    }
                },
                {
                    label: '数据更新时间',
                    prop: 'updateTime',
                    option: {
                        align: 'center'
                    },
                    formatter: (row) => {
                        if (!row || !row.updateTime) {
                            return '-';
                        }
                        // 格式化时间显示
                        return row.updateTime.split(' ')[0];
                    }
                }
            ],
            // 菜单配置
            menuConfig: {
                options: [
                    {
                        label: '加入监控',
                        click: (item) => {
                            this.addToMonitor(item)
                        }
                    }
                ]
            },
            pageIndex: 0,
        };
    },
    computed: {
        getAnchorTotalNum() {
            return this.$store?.getters?.getUserproperty?.totalAnchorNum || 0
        },
        addAnhorNumber() {
            return this.$store?.getters?.getUserproperty?.anchorNum < 0 ? 0 : this.$store?.getters?.getUserproperty?.anchorNum
        },
        addCompereNumber() {
            return this.$store?.getters?.getUserproperty?.useAnchorNum
        },
        getLabel(){
            return this.$route.query?.label || ''
        }
    },
    inject: ['appVnode'],
    watch: {},
    methods: {
        handleClickAnchor(item){
            window.open('https://live.douyin.com/'+item.anchorPlatformAccount)
        },
        toUpgrade(){
            this.appVnode.versionQrCodeShow()
        },
        /**
         * 获取平台代码
         * @param {string|number} platform 平台类型 (1:抖音 2:快手 3:视频号)
         * @returns {string} 平台代码
         */
        // getPlatformCode(platform) {
        //     // 处理数字类型的平台类型
        //     const platformTypeMap = {
        //         '1': 'DouYinLive',
        //         '2': 'KuaiShouLive', 
        //         '3': 'WeChatChannels',
        //         1: 'DouYinLive',
        //         2: 'KuaiShouLive',
        //         3: 'WeChatChannels'
        //     };
            
        //     // 处理字符串类型的平台名称（兼容旧数据）
        //     const platformNameMap = {
        //         '抖音': 'DouYinLive',
        //         '小红书': 'XiaoHongShu',
        //         '快手': 'KuaiShouLive',
        //         'TikTok': 'TiktokLive',
        //         '视频号': 'WeChatChannels'
        //     };
        //     return platformTypeMap[platform] || platformNameMap[platform] || 'DouYinLive';
        // },
        apiFinally(res){
            this.isHintMsg = res?.code === 60002
        },
        /**
         * 获取热门榜单数据
         * @returns {Promise} 返回包含榜单数据的Promise
         */
        tableHttp(param){
            this.pageIndex = param.pageIndex
            return {
                http:this.$httpBack.words.tradeRankPage,
                param:{
                    tradeId: this.$route.query.id || 0,
                    page: param.pageIndex,
                    limit: param.pageSize,
                    anchorName: param.anchorName || '',
                    liveKeyword: param.liveKeyword || ''
                }
            }
            
            // return new Promise((resolve, reject) => { 
            //     // 模拟数据
            //     const mockData = [
            //         {
            //             rank: 1,
            //             account: '美食达人小王',
            //             accountId: 'foodking001',
            //             avatar: 'https://via.placeholder.com/40x40/FF6B6B/FFFFFF?text=美',
            //             viewHeat: 156800,
            //             fansCount: 2350000,
            //             mainField: '美食制作',
            //             platform: '抖音',
            //             updateTime: '2024-01-15 14:30',
            //             operation: ''
            //         },
            //     ];
            // })
        },
        tableResData(data){
            data.totalCount = data.totalCount>300 ? 300 : data.totalCount;
            data.list = data.list.map((item,index)=>{
                item.rank = (index + 1) + (data.pageSize * (this.pageIndex - 1))
                return item
            })
            return data
        },
        /**
         * 加入监控操作
         * @param {Object} item 选中的数据项
         */
        addToMonitor(item) {
            // 显示抽屉
            this.drawerVisible = true;
            // 可以在这里保存当前选中的项目信息
            this.currentMonitorItem = item;
        },
        getUserproperty() {
            // 获取资产
            this.appVnode.getUserproperty((data => {
                this.userProperty = data
            }))
        },
        
        /**
         * 处理添加主播事件
         * @param {Object} formData 表单数据
         */
        async handleAddCompere(formData) {
            const requestData = cloneDeep(formData)
            this.$nextTick(() => {

                this.$refs.compereFormRef.loadingStatus(true)
                const rowItem = this.currentMonitorItem;
                const recordTime = requestData.recordTime;
                if (recordTime?.length > 0) {
                    requestData.recordTime = recordTime[0] && recordTime[1] ? `${recordTime[0]}-${recordTime[1]}` : ''
                } else {
                    requestData.recordTime = ''
                }
                if (this.addAnhorNumber <= 0) {
                    this.$refs.compereFormRef.loadingStatus(false)
                    return this.$refs.statusDialog.openStatusDialog({
                        broadcastUrls: rowItem.anchorPlatformAccount,
                        addCompereNumber: this.addCompereNumber,
                        status: 'warning'
                    })
                    // this.$confirmWarning({
                    //     title: '添加上限',
                    //     message: `您已添加${this.addCompereNumber}个主播，请删除部分主播后，再进行添加。`,
                    //     confirmButtonText: '去录制',
                    //     cancelButtonText: '前去删除',
                    // }).then(res=>{ 
                    //     this.initDrawer();
                    //     this.toHome();
                    // }).catch(err=>{
                    //     this.initDrawer();
                    //     this.toHome();
                    // });
                    // this.$refs.compereFormRef.loadingStatus(false)
                    return
                }
                // 执行版本判断。
                this.appVnode.isVersionQrCode('init').then(() => {
                    this.$httpClient.compere.addOrUpdateAnchor({
                        ...omit(requestData, ['broadcastUrls', 'ksUrls','recordDuration','isAutoDiagnosis']),
                        recordLimitValue: requestData.recordLimitValue === 0 ? -1 : formData.recordDuration,
                        liveUrl: 'https://live.douyin.com/' + rowItem.anchorPlatformAccount,
                        platform: 0,
                        ...omit(this.diagnosisParams,['modelId']),
                        isDataDiagnosis:requestData?.isAutoDiagnosis,
                        dataDiagnosisParams: {
                            modelId: this.diagnosisParams?.modelId
                        },
                        isAutoDiagnosis: 0
                    }).then((res) => {
                        if (res.code === 0) {
                            this.diagnosisParams = {};
                        }
                        this.initDrawer();
                        /*
                            this.$confirmSuccess({
                                title: '添加成功',
                                message: `<div class="flex items-center justify-center text-left" style="margin-top: 10px;">
                                        <img src="${rowItem.anchorAvatar}" alt="头像"
                                            style="min-width: 40px;height:40px;border-radius: 50%"/>
                                        <div style="margin-left: 12px;">
                                            <div>${rowItem.anchorName}</div>
                                            <div>${rowItem.accountId}</div>
                                        </div>
                                    </div>`,
                                confirmButtonText: '去录制',
                                cancelButtonText: '继续添加',
                            }).then(res=>{
                                this.initDrawer();
                                this.toHome();
                            }).catch(err=>{
                                this.initDrawer();
                            });
                        */
                    }).catch(() => {
                        this.$refs.compereFormRef.loadingStatus(false);
                        this.initDrawer();
                    })
                })
            })
        },
        _cNotification() {
            this.$CSharpNotify.addTask('addAnchorEnd', (res, resolve) => {
                // 成功回调：res 为 data（AnchorInfo），仅 code=0&&status=200 时才触发
                this.$nextTick(() => {
                    this.$refs.compereFormRef.loadingStatus(false, 'reset')
                    const rowItem = this.currentMonitorItem;
                    this.$refs.statusDialog.openStatusDialog({
                        ...res,
                        broadcastUrls: rowItem.anchorPlatformAccount,
                        urlType: 0,
                        status: res ? (res.Id === -999 ? 'info' : 'success') : 'error',
                        platform: PLATFORM_ENUM[0]
                    })
                })
            }, (resultData = {}) => {
                const rowItem = this.currentMonitorItem;
                if (resultData.status === 500) {
                    const codeMsgMap = {
                        70007: '请先关闭AI话术监控功能后，再修改账号归属类型',
                        70002: '授权量不足，请联系产品顾问',
                        70005: '请先确认标准直播稿'
                    }
                    const message = codeMsgMap[resultData.code] || resultData.msg || '保存失败'
                    this.$refs.statusDialog.openStatusDialog({
                        broadcastUrls: rowItem?.anchorPlatformAccount,
                        urlType: 0,
                        status: 'error',
                        message,
                        platform: PLATFORM_ENUM[0]
                    })
                }
                this.$refs.compereFormRef.loadingStatus(false)
            })
        },
        initDrawer(){
            this.drawerVisible = false;
            this.$refs?.compereFormRef?.resetFields();
        },
        closeStatusDialog({status,type}){
            if(status === 'warning'){
                this.toHome();
                return;
            }
            if(type ==='right'){
                this.toAdd();
            }
        },
        toHome(){
            this.$router.push({
                path: '/dataAnalysis',
            })
        },
        toAdd(){
            this.initDrawer();
        },
        /**
         * 处理AI报告变化事件
         * @param {Object} obj AI报告配置对象
         */
        handleIsAiReportChange(obj) {
            if (obj.isAutoDiagnosis === 1) {
                this.$refs.ai_data_report.changeDrawerStatus(true, {
                    ...obj,
                    isAddCompere: true
                });
            } else {
                this.diagnosisParams = {};
            }
        },
         
         /**
          * 初始化AI报告
          */
         initAiReport() {
             // 重置CompereForm中的AI诊断配置
             if (this.$refs.compereFormRef) {
                 this.$refs.compereFormRef.setFormData({isAutoDiagnosis: 0});
             }
         },
         
         /**
          * 设置诊断参数
          * @param {Object} data 诊断参数数据
          */
         setDiagnosisParams(data) {
             this.diagnosisParams = data;
         },
         
         /**
          * 初始化行业树形数据
          */
         initTradeTreeList() {
            // 模拟行业数据，实际项目中应该从API获取
            this.tradeTreeList = []
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
            })
         }
    },
    created() {
        this.initTradeTreeList();
        this._cNotification();
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
.monitor-content{
    height: 90vh;
    .compereForm{
        height: 100%;
    }
    :deep(.bodyContainer){
        height: 100%;
        padding: 0 !important;
    }
}
</style>

<template>
    <div class="addContainer"
         v-if="versionType === VERSION_TYPE.AGENT||(versionType===VERSION_TYPE.PURE&&!isShipinhao)">
        <div class="main-bg pd-16 brs-10">
            <!-- 各平台提示 -->
            <component :is="domName[getTabsName]" class="tipContainer" @showDyHelp="showDyHelp"></component>
            <!-- 添加提示 -->
            <div class="tipContainer">
                <div>小Tips：{{isShipinhao?'授权后':'添加直播间后'}}，如直播间开启直播，将会自动录制并分析</div>
                <div v-if="activateVersion&&isDouyin">您的会员套餐版本是：<span class="text-colorErr">{{ $store.getters.getPackageLevelName }}</span>，需要激活软件后才能正常使用，<span style="color: var(--color-main);cursor: pointer" @click="isVersionQrCode">点我激活</span></div>
                <div v-else>您的会员套餐版本是：
                    <span class="text-colorErr">{{ $store.getters.getPackageLevelName }}</span>，{{isShipinhao ? '还可以授权账号数量':'还可以添加直播间数'}}：
                    <span class="text-colorErr">{{addAnhorNumber}}<span  style="font-size: 12px !important;" class="text-color3">(剩)</span></span>
                    <span>/{{ getAnchorTotalNum }}<span style="font-size: 12px !important;" class="text-color3">(总)</span></span>
                    <!-- <span class="text-color3">（剩余/总数）</span> -->
                    <el-button type="text" style="margin-left: 12px" @click="showQr">立即升级</el-button>
                </div>
                <div v-if="activateVersion&&isDouyin">
                    目前已经添加直播间数量：<span class="text-colorErr">{{ addCompereNumber }}</span>个，目前还可以添加：<span class="text-colorErr">{{ addAnhorNumber }}</span>个
                    <el-button type="text" @click="isVersionQrCode">点我免费扩容</el-button>
                </div>
                <div v-if="anchorUsedNum>15" class="text-colorErr" style="margin-bottom: 8px">注意：录制直播间较多时候，如果您的下行带宽不够，有可能出现录制问题，请按需录制！</div>
                <div v-if="isKuaishou" class="text-colorErr">
                   快手暂无数据看板功能！！！
                </div>
            </div>
            <div v-if="isShipinhao">
                <span class="text-colorErr font-s14">重要提示：视频号录制数量是主账号和子账号一起共享的!</span>
            </div>
        </div>
        <div class="bodyContainer pd-16 main-bg brs-10 flex-1">
            <CompereForm @addCompere="addCompere" :tabName="target" @onIsAiReportChange="onIsAiReportChange"
                :tradeTreeList="tradeTreeList" ref="compereFormRef">
                <template v-if="$slots.sphAuthorization" #sphAuthorization>
                    <slot name="sphAuthorization"></slot>
                </template>
            </CompereForm>
            <!-- <el-cascader class="cascaderId" popper-class="select-trade-id" v-model="tradeId" v-removeAriaHidden :options="tradeTreeList" style="width: 440px;"
                :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name', emitPath: false }" filterable
                placeholder="为主播选择一个行业，以提高敏感词匹配分析准确性" @expand-change="expandChange" @change="changeTradeHandle" ref="tradeCascader">
                <template slot-scope="{ node, data }">
                    <span v-if="data.children?.length">{{ data.name }}</span>
                    <span v-else @click="expandChange(data.id)">{{ data.name }}</span>
                </template>
</el-cascader> -->
        </div>
        <AddCompereStatusDialog ref="statusDialog" @toCompereList="toCompereList" @cleseStatus="closeStatusDialog" />
        <el-dialog title="帮助" top="10px" :visible.sync="dialogVisible" width="40%" :close-on-click-modal="true">
            <div style="max-height: 600px;overflow: hidden;overflow-y: auto;">
                <div v-html="helpHtml"></div>
            </div>
        </el-dialog>
        <AiDataReport ref="ai_content_report" @initAiReport="initAiReport" @setDiagnosisParams="setDiagnosisParams"
                  :diagnosisParams="diagnosisParams" reportType="anchor"/>
        <!-- 客服弹窗 -->
        <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>

        <versionQrCode ref="versionQrCode"></versionQrCode>
    </div>
    <div class="addContainer" v-else-if="versionType===VERSION_TYPE.PURE&&isShipinhao">
        <el-empty :image-size="120" style="margin-top: 10%">
            <div slot="description" style="font-weight: 500;font-size: 14px;">
               视频号录制 <span style="color: #444DFF;">仅限 &lt;AI全能版&gt; </span> 使用
            </div>
        </el-empty>
    </div>
</template>

<script>
import { cloneDeep, omit } from 'lodash'
import customerServiceQrCode from '../../commonComponent/customerServiceQrCode.vue'
import tabs from './../../../mixins/tabs'
import Douyin from './common/douyin.vue'
import Kuaishou from './common/kuaishou.vue'
import Shipinhao from './common/shipinhao.vue'
import tradeId from '@/components/tradeId/index.vue'
import CompereForm from './common/compereForm.vue'
import AddCompereStatusDialog from './common/addCompereStatusDialog.vue'
import AiDataReport from '@/views/commonComponent/aiReport/aiDataReport.vue'
import versionQrCode from '@/views/commonComponent/versionQrCode.vue'
import {PLATFORM_ENUM,VERSION_TYPE} from '@/enum/index.js'
export default {
    components: { customerServiceQrCode, Douyin, Kuaishou,Shipinhao, tradeId, CompereForm, AddCompereStatusDialog, AiDataReport,versionQrCode },
    mixins: [tabs],
    props:{
        target:{
            type: String,
            default: 'douyin'
        },
        weChatInfo:{
            type: Object,
            default: null
        },
    },
    data() {
        return {
            VERSION_TYPE,
            // tabs配置
            tabs: [],
            domName: {
                douyin: 'Douyin',
                kuaishou: 'Kuaishou',
                shipinhao: 'Shipinhao'
            },
            kefuDialogVisible: false,
            tradeId: '',
            tradeTreeList: [],
            dialogVisible: false,
            helpHtml: '',
            broadcastUrls: '',
            urlType: null,
            userProperty: {},
            diagnosisParams: {},
            ksUrls: ''
        }
    },
    comments:{},
    mounted() {
        this.init()
        this._cNotification()
    },
    activated() {
        this.init()
    },
    inject: ['appVnode'],
    computed: {
        activateVersion(){
            return this.$store.getters.getPackageLevel === -1
        },
        getAnchorTotalNum() {
            // 视频号总数
            if(this.isShipinhao){
                return this.userProperty?.totalChannelMonitorNum || 0
            }
            // 快手总数
            if(this.isKuaishou){
                return this.userProperty?.totalKuaishouMonitorNum || 0
            } 
            // 抖音总数
            return this.userProperty?.totalAnchorNum || 0
        },
        anchorUsedNum() {
            // 抖音已使用
            const d = this.isStrictNumeric(this.userProperty?.useAnchorNum || 0)
            const k = this.isStrictNumeric(this.userProperty?.useKuaishouMonitorNum || 0)
            const s = this.isStrictNumeric(this.userProperty?.useChannelMonitorNum || 0)
            return d + k + s
        },
        addAnhorNumber() {
            // 视频号剩余资源
            if(this.isShipinhao){
                return this.userProperty?.channelMonitorNum< 0 ? 0 : this.userProperty?.channelMonitorNum || 0;
            }
            // 快手剩余资源
            if(this.isKuaishou){
                return this.userProperty?.kuaishouMonitorNum< 0 ? 0 : this.userProperty?.kuaishouMonitorNum || 0;
            }
            // 抖音剩余资源
            return this.userProperty?.anchorNum < 0 ? 0 : this.userProperty?.anchorNum
        },
        addCompereNumber() {
            // 视频号已使用
            if(this.isShipinhao){ 
                return this.userProperty?.useChannelMonitorNum || 0
            }
            // 快手已使用
            if(this.isKuaishou){
                return this.userProperty?.useKuaishouMonitorNum || 0
            }
            // 抖音已使用
            return this.userProperty?.useAnchorNum || 0
        },
        isKuaishou() {
            return this.target === 'kuaishou'
        },
        isDouyin() {
            return this.target === 'douyin'
        },
        isShipinhao() {
            return this.target === 'shipinhao'
        },
        versionType(){
            return this.$store.getters.getVersionType
        }
    },
    watch: {
        '$route'(to, from) {
            this.$nextTick(() => {
                this.$refs.ruleForm?.resetFields()
            })
        }
    },
    methods: {
        setAuthorizerInfoId(id){
            this.$refs.compereFormRef.setAuthorizerInfoId(id)
        },
        isStrictNumeric(value) {
            // 匹配：可选的正负号 + 数字 + 可选的小数部分
            const reg = /^-?\d+(\.\d+)?$/;
            if (reg.test(value)) {
                return Number(value)
            }else {
                return 0
            }
        },
        getTabs(){
            let tabs = [
                { label: '抖音', name: 'douyin' },
                { label: "快手", name: 'kuaishou' },
                { label: "视频号", name: 'shipinhao' }
            ]
            this.tabs = tabs.filter(item=>item.name === this.target)
        },
        getUserproperty() {
            // 获取资产
            this.appVnode.getUserproperty((data => {
                this.userProperty = data
            }))
        },
        init() {
            this.getUserproperty()
            this.getTradeTreeList()
            this.tradeId = ''
            this.tradeTreeList = []
            this.broadcastUrls = ''
            this.urlType = null;
            this.ksUrls = '';
            this.getTabs();
        },
        _cNotification() {
            this.$CSharpNotify.addTask('addAnchorEnd', (res, resolve) => {
                // 成功回调：res 为 data（AnchorInfo），仅 code=0&&status=200 时才触发
                this.$nextTick(() => {
                    this.$refs.compereFormRef.loadingStatus(false, 'reset');
                    if(this.isShipinhao){
                        this.$emit('delInfo');
                    }
                    this.$refs.statusDialog.openStatusDialog({
                        ...res,
                        broadcastUrls: this.broadcastUrls ||  this.ksUrls,
                        urlType: this.urlType,
                        status: res ? (res.Id === -999 ? 'info' : 'success') : 'error',
                        platform: PLATFORM_ENUM[this.getTabsName]
                    })
                    setTimeout(() => {
                        this.getUserproperty();// 重新刷新数据
                        this.broadcastUrls = '';
                    }, 1000)
                })
            }, (resultData={}) => {
                if(resultData.status === 500){
                    const codeMsgMap = {
                        70007: '请先关闭AI话术监控功能后，再修改账号归属类型',
                        70002: '授权量不足，请联系产品顾问',
                        70005: '请先确认标准直播稿'
                    }
                    const message = codeMsgMap[resultData.code] || resultData.msg || '保存失败'
                    this.$refs.statusDialog.openStatusDialog({
                        broadcastUrls: this.broadcastUrls ||  this.ksUrls,
                        urlType: this.urlType,
                        status:'error',
                        message,
                        platform: PLATFORM_ENUM[this.getTabsName]
                    })
                }
                this.$refs.compereFormRef.loadingStatus(false)
            })
        },
        showQr() {
            this.kefuDialogVisible = true
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },
        isVersionQrCode(){
            return this.$refs.versionQrCode.autoShow('init')
        },
        expandChange(data) {
            this.tradeId = data
            if (Array.isArray(data)) {
                setTimeout(() => {
                    this.tradeId = data[data.length - 1]
                }, 50)
                return
            }
        },
        // 获取用户资产
        // getUserProperty() {
        //     this.$httpBack.userProperty.info({}).then((res) => {
        //         if (res && res.code === 0) {
        //             this.userProperty = res.data;
        //         }
        //     });
        // },

        // 选择行业回调
        changeTradeHandle() {
            // 关闭级联列表下拉
            this.$refs.tradeCascader.dropDownVisible = false
        },

        // 获取行业列表树形
        getTradeTreeList() {
            this.tradeTreeList = []
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
            })
        },
        // 显示抖音帮助页
        showDyHelp(type) {
            this.$httpBack.article.list({ limit: -1, type: type || 0 }).then((res) => {
                if (res.code == 0 && res.data) {
                    this.helpHtml = res.data.list[0].content
                    this.helpHtml = this.helpHtml.replaceAll('<img', '<img style=\'width: 100%\'')
                    this.dialogVisible = true
                } else {
                    this.$message.error('暂未支持')
                }
            })
        },
        // 跳转到添加主播页面
        toCompereList() {
            this.$router.push({
                path: '/dataAnalysis'
            })
        },
        closeStatusDialog({data,type,status}){
            if(status === 'error' && type === 'right'){
                this.initCompereForm();
            }
        },
        initCompereForm() {
            this.$nextTick(()=>{
                this.$refs.compereFormRef.initForm();
                this.$emit('delInfo');
            })
        },
        initAiReport() {
            this.$refs.compereFormRef.setFormData({ isAutoDiagnosis: 0 })
        },
        onIsAiReportChange(obj) {
            if (obj.isAutoDiagnosis === 1) {
                this.$refs.ai_content_report.changeDrawerStatus(true, {
                    ...obj,
                    isAddCompere: true
                })
            } else {
                this.diagnosisParams = {}
            }
        },
        setDiagnosisParams(data) {
            this.diagnosisParams = data
        },
        getPlatform(){
            return PLATFORM_ENUM[this.getTabsName]
        },
        async addCompere(data) {

            const requestData = cloneDeep(data)
            this.$nextTick(() => {

                this.$refs.compereFormRef.loadingStatus(true)
                // if (!this.tradeId) {
                //     this.$message.error('请为主播选择一个行业')
                //     return
                // }
                // if (!this.broadcastUrls) {
                //     this.$message.error('地址不能为空')
                //     return
                // }
                // 去空格
                const recordTime = requestData.recordTime
                if (recordTime?.length > 0) {
                    requestData.recordTime = recordTime[0] && recordTime[1] ? `${recordTime[0]}-${recordTime[1]}` : ''
                } else {
                    requestData.recordTime = ''
                }


                let arr = []
                if (this.isDouyin) {
                    requestData.broadcastUrls = requestData.broadcastUrls.replaceAll(' ', '')?.replace(/^[^a-zA-Z0-9_.]+/, '')?.replace(/[^a-zA-Z0-9_.]+$/, '')
                    if (!requestData.broadcastUrls) {
                        this.$message.error('地址不能为空')
                        this.$refs.compereFormRef.loadingStatus(false)
                        return
                    }
                    this.broadcastUrls = requestData.broadcastUrls
                    this.urlType = requestData.urlType
                    if (requestData.urlType === 0) {
                        let tempArr = [requestData.broadcastUrls]
                        tempArr = tempArr.filter(item => item)

                        // 校验只能输入字母、数字、下划线和点
                        const regex = /^[a-zA-Z0-9_.]+$/
                        let regexFlag = true
                        tempArr.forEach(item => {
                            if (!regex.test(item)) {
                                regexFlag = false
                            }
                        })

                        if (!regexFlag) {
                            this.$message.error('请检查抖音号是否输入正确，抖音号只支持字母、数字、下划线和点')
                            this.$refs.compereFormRef.loadingStatus(false)
                            return
                        }

                        tempArr.forEach(item => {
                            if (item.indexOf('douyin.com') === -1) {
                                item = 'https://live.douyin.com/' + item
                            }
                            arr.push(item)
                        })
                    } else {
                        const match = requestData.broadcastUrls?.match(/\/(\d+)(?:\?|$)/);
                        if (!match) {
                            this.$refs.compereFormRef.loadingStatus(false)
                            this.$message.error('请检查直播间地址是否输入正确')
                            return
                        }
                        arr.push('https://live.douyin.com/' + (match ? match[1] : null))
                    }
                }
                this.ksUrls = requestData.ksUrls;

                if (this.userProperty.anchorNum !== -1 && arr.length > this.userProperty.anchorNum) {
                    this.$confirm('添加直播间授权数量不足，是否立即扩容？', '提示', {
                        confirmButtonText: '确定',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }).then(() => {
                        this.showQr()
                    })
                    this.$refs.compereFormRef.loadingStatus(false)
                    return
                }

                if (this.addAnhorNumber <= 0 && this.isDouyin) {
                    this.$refs.compereFormRef.loadingStatus(false)
                    return this.$refs.statusDialog.openStatusDialog({
                        broadcastUrls: requestData.broadcastUrls,
                        addCompereNumber: this.addCompereNumber,
                        status: 'warning'
                    })
                }

               if(this.isShipinhao){
                    requestData.recordTime = requestData.recordTimes?.map(d=>{
                        return d.join('-')
                    })?.join(',');
                    delete requestData.recordTimes;
                    requestData.ksUrls = requestData.authorizerInfoId;
                    requestData.secUid = requestData.setAuthorizerInfoId;
                    delete requestData.authorizerInfoId;
               }

                // let requestData = {
                //     TradeId: this.tradeId,
                //     Urls: arr
                // }
                // 执行版本判断。
                this.appVnode.isVersionQrCode('init').then(() => {
                    this.$httpClient.compere.addOrUpdateAnchor({
                        ...omit(requestData, ['broadcastUrls', 'ksUrls','recordDuration','isAutoDiagnosis']),
                        recordLimitValue: requestData.recordLimitValue === 0 ? -1 : data.recordDuration,
                        liveUrl: this.isDouyin ? arr[0] : requestData.ksUrls,
                        platform: this.getPlatform(),
                        ...omit(this.diagnosisParams,['modelId']),
                        isDataDiagnosis:requestData?.isAutoDiagnosis,
                        dataDiagnosisParams: {
                            modelId: this.diagnosisParams?.modelId
                        },
                        pureRecordOnlineNum: 0,
                        isAutoDiagnosis: 0
                    }).then((res) => {
                        if (res.code === 0) {
                            this.diagnosisParams = {}
                        }
                    }).catch(() => {
                        this.$refs.compereFormRef.loadingStatus(false)
                    })
                })
            })
        }
    },
    beforeDestroy() {
    }, //生命周期 - 销毁之前
}
</script>
<style lang="scss" scoped>
.addContainer {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 100%;
}

.bodyContainer {
    display: flex;
    margin-top: 10px;
    flex-direction: column;
    align-items: flex-start;
}

.tipContainer {
    font-size: 14px;
    color: #2E3742;
}

.cascaderId {
    ::v-deep(.el-radio__original) {
        display: none !important;
        /* 隐藏原生 radio 输入，但仍然允许交互 */
    }

    ::v-deep(.el-radio:focus:not(.is-focus):not(:active):not(.is-disabled) .el-radio__inner) {
        box-shadow: none !important;
    }
}
</style>

<template>
    <div class="data-analysis-tab-box">
        <Title @leftClick="rechargeTime" :detection="detection" @start="startHanlder" @stop="stopHandler"
               :recordNum="compereInfo.CurrentRecordNum" type="all" :compereMapList="compereMapList" :detectionTime="detectionTime" ref="allTitle"
               style="margin-bottom: 12px;">
            <template #titleLeft>
<!--                <div class="common-card anchorNum flex flex-col justify-around items-center">-->
<!--                    <div>主播数量</div>-->
<!--                    <span class="text-2xl font-bold">{{ compereInfo.AllTotal || 0 }}</span>-->
<!--                </div>-->
<!--                <div class="common-card flex flex-col justify-around items-center">-->
<!--                    <div>直播中</div>-->
<!--                    <span class="text-2xl font-bold">{{ compereInfo.CurrentLiveNum || 0 }}</span>-->
<!--                </div>-->
<!--                <div class="common-card recording flex flex-col justify-around items-center">-->
<!--                    <div>录制中</div>-->
<!--                    <span class="text-2xl font-bold">{{ compereInfo.CurrentRecordNum || 0 }}</span>-->
<!--                </div>-->
            </template>

            <template #search>
                <slot name="search"></slot>
            </template>
            <template #titleLeftBottom>
                <slot name="search-header"></slot>
                <div class="flex-ai-c">
                    <div class="account-type">
                        <span class="text-title">账号筛选：</span>
                        <el-radio-group v-model="accountType">
                            <el-radio :label="''" class="radio-as-checkbox">全部账号</el-radio>
                            <el-radio :label="0" class="radio-as-checkbox">自有账号</el-radio>
                            <el-radio :label="1" class="radio-as-checkbox">同行业账号</el-radio>
                        </el-radio-group>
                    </div>
                    <div class="mg-l10 text-colorTheme font-s14">
                        <el-button @click="toDouyin" type="text">
                            <div class="flex items-center justify-between">
                                <img src="@/assets/imgs/tiktok.png" alt="" style="width: 11px">
                                <span style="margin-left: 4px">打开抖音</span>
                            </div>
                        </el-button>
                    </div>
                </div>
            </template>
        </Title>
        <div v-if="localCompereMapList && localCompereMapList.length > 0" style="position: relative;">
            <template v-for="(item, index) in localCompereMapList">
                <Card style="margin-bottom: 10px;" :title="item.trade?.name" class="dataAnalysis-card">
                    <template #title>
                        <div class="flex-ai-c">
                            <span class="font-s16 text-colorMain">{{item.trade?.name}}</span>
                            <span class="font-s14 text-color2 font-w100 mg-l8" v-if="item.hotListCount >= 10">
                                想学习更多优秀同行？点我查看《行业优秀账号TOP热度榜！》
                            </span>
                            <el-button type="text  test-icon" style="margin-left: 12px" @click="toHotList(item)" v-if="item.hotListCount >= 10">点击查看</el-button>
                        </div>
                    </template>
                    <Table class="home-table" :data="item.list" :column="columnConfig"
                           :menuConfig="{ options: options, width: '220px' }">
                        <template #anchor="{ row }">
                            <el-popover placement="top" trigger="hover" v-if="row.RecordTime">
                                <template slot="reference">
                                    <Anchor :item="row" isToHomeUrl></Anchor>
                                </template>
                                <div>
                                    <div>监控时间段</div>
                                    <div style="padding: 8px 0">{{ row.RecordTime || '-' }}</div>
                                    <div class="text-colorErr font-s12" v-if="!row.InRecordTime">当前不在监控时间段内（不会监控是否开播）</div>
                                </div>
                            </el-popover>
                            <Anchor :item="row" isToHomeUrl v-else></Anchor>
                        </template>
                        <template #knowledgeBase="{ row }">
                            <span class="cursor-pointer" style="color:var(--color-main)"
                                  @click="parent.openKnowledgeBaseDrawer(row)">
                                {{ parent.getKnowledgeBaseButtonText(row) }}
                            </span>
                        </template>
                        <template #buyIn="{ row }">
                            <!-- 视频号授权 -->
                            <div v-if="row.platform === PLATFORM_ENUM.shipinhao">
                                <div class="buyIn-success" v-if="row.authChannelStatus === 1">
                                    {{ authChannelStatus.get(row.authChannelStatus) }}
                                </div>
                                <div v-else-if="row.authChannelStatus === 0" class="buyInIng">
                                    {{ authChannelStatus.get(row.authChannelStatus) }}
                                </div>
                                <div v-else class="text-color3">
                                    {{ authChannelStatus.get(row.authChannelStatus) || '-' }}
                                </div>
                            </div>
                            <!-- 巨量/来客授权 -->
                            <div v-else-if="row.AccountType===0 && row.platform === PLATFORM_ENUM.douyin">
                                <div v-if="row.juliangAuthStatus !== 1 && Number(row.lifeAuthStatus) !== 1">
                                    <div class="buyIn-default pd-b4" @click="()=>openAuthorizeDialog(row)">
                                        点我授权
                                    </div>
                                </div>
                                <div class="flex justify-center items-center">
                                    <div class="buyIn-success mg-l8" v-if="[1].includes(row.juliangAuthStatus)">
                                        巨量:{{ buyInStatus.get(row.juliangAuthStatus) }}
                                    </div>
                                    <div :class="buyInClass(row.juliangAuthStatus)" class="text-xs"
                                        style="cursor: text" v-if="[2,3,4,5,6].includes(row.juliangAuthStatus)">
                                        {{ buyInStatus.get(row.juliangAuthStatus) }}
                                    </div>
                                    <span v-if="[1].includes(row.juliangAuthStatus)" class="text-xs cursor-pointer text-color3 mg-l8" @click="()=>openBuyIn(row.SecUid)">
                                        打开巨量
                                    </span>
                                </div>
                                <div v-if="(row.juliangAuthStatus === 1) || (row.juliangAuthStatus === 0 && row.qianchuanAuthStatus === 1)">
                                    <div class="flex justify-center items-center">
                                        <el-button
                                            v-if="getQianchuanAuthStatus(row) === 0"
                                            type="text"
                                            @click="authorizeQianchuanHandler(row)">千川授权</el-button>
                                        <div
                                            class="buyIn-default"
                                            v-if="[2,3,5].includes(getQianchuanAuthStatus(row))"
                                            @click="()=>authorizeQianchuanHandler(row)">
                                            重新千川授权
                                        </div>
                                        <div
                                            v-if="[1].includes(getQianchuanAuthStatus(row))"
                                            class="buyIn-success"
                                            style="cursor: text">
                                            千川:{{ qianchuanStatus.get(getQianchuanAuthStatus(row)) }}
                                        </div>
                                        <!-- TODO: 已迁移至统一取消授权入口 cancelAuthDialog -->
                                        <!-- <span
                                            v-if="[1].includes(getQianchuanAuthStatus(row))"
                                            class="qianchuan-cancel-btn"
                                            @click="cancelAuthorizeQianchuanHandler(row)">
                                            取消授权
                                        </span> -->
                                        <div
                                            v-if="[2,3,4,5].includes(getQianchuanAuthStatus(row))"
                                            :class="buyInClass(getQianchuanAuthStatus(row))"
                                            class="text-xs"
                                            style="cursor: text">
                                            {{ qianchuanStatus.get(getQianchuanAuthStatus(row)) }}
                                        </div>
                                    </div>
                                </div>
                                <div v-if="Number(row.lifeAuthStatus) === 1 && row.juliangAuthStatus !== 1" style="margin-top: 4px;">
                                    <div class="flex justify-center items-center">
                                        <div class="buyIn-success" style="cursor: text">来客:已授权</div>
                                    </div>
                                </div>
                            </div>
                            <span v-else>-</span>
                        </template>

                        <template #Folder="{ row }">
                            <span class="cursor-pointer" style="color:var(--color-main)"
                                  @click="()=>openDirectory(row.SecUid)">查看</span>
                        </template>
                        <template #recordList="{ row }">
                            <span class="cursor-pointer" style="color:var(--color-main)"
                                  @click="()=>openRecordList(row)">查看</span>
                        </template>
                        <template #home="{ row }">
                            <span class="cursor-pointer" style="color:var(--color-main)"
                                  @click="()=> openToBrowser(row.HomeUrl)">查看</span>
                        </template>
                        <template #recordDefinition="{ row }">
                            <span
                                v-for="item in definitionList">
                                <span v-if="row.recordDefinition===item.value" :style="{ color: item.color }">{{ item.label }}</span>
                            </span>
                        </template>
                        <template #PureRecordOnlineNum="{row}">
                            <el-switch :disabled="row.LiveStatus==2||notSwitch(row)" :value="row.pureRecordOnlineNum"
                                       active-color="#28BD6C" inactive-color="#DCE0E7"
                                       :active-value="1" :inactive-value="0"
                                       @change="(value) => autoOnlineNumberPureChange(value, row)">
                            </el-switch>
                        </template>

                        <template #IsAutoRecord="{ row }">
                            <el-tag effect="dark" :type="row.IsAutoRecord?'success': 'danger'">
                                {{ row.IsAutoRecord ? '是' : '否' }}
                            </el-tag>
                            <!-- <el-switch v-model="row.IsAutoRecord" active-color="#0077FF" inactive-color="#DCE0E7"
                                :active-value="1" :inactive-value="0"
                                @change="(value) => autoRecordChange(value, row.SecUid)">
                            </el-switch> -->
                        </template>

                        <template #IsDataViewing-header="{column}">
                            <div>
                                <!-- <el-tooltip class="item" effect="dark" content="Top Center 提示文字" placement="top">
                                    <i class="el-icon-warning"></i>
                                </el-tooltip> -->
                                <span>{{ column.label }}</span>
                            </div>
                        </template>
                        <template #IsDataViewing="{ row }">
                            <!-- <el-switch v-if="row.platform === PLATFORM_ENUM.kuaishou" :value="0" active-color="#444DFF" inactive-color="#DCE0E7" :active-value="1" :inactive-value="0" :disabled="true"></el-switch> -->
                            <el-switch :value="row.platform === PLATFORM_ENUM.kuaishou?0:row.IsDataViewing" active-color="#28BD6C" inactive-color="#DCE0E7"
                                        :disabled="row.platform === PLATFORM_ENUM.kuaishou"
                                       :active-value="1" :inactive-value="0"
                                       @change="(value) => autoDataBoardChange(value, row.SecUid)">
                            </el-switch>
                        </template>

                        <template #IsBarrageMonitoring="{ row }">
                            <!-- <el-switch v-if="row.platform === PLATFORM_ENUM.kuaishou" :value="0" active-color="#444DFF" inactive-color="#DCE0E7" :active-value="1" :inactive-value="0" :disabled="true"></el-switch> -->
                            <el-switch :value="notSwitch(row)?0:row.IsBarrageMonitoring" active-color="#444DFF" inactive-color="#DCE0E7"
                                        :disabled="notSwitch(row)"
                                       :active-value="1" :inactive-value="0"
                                       @change="(value) => autoBarrageMonitoringChange(value, row.SecUid)">
                            </el-switch>
                        </template>

                        <template #IsAutoUploadCloud="{ row }">
                            <el-switch :value="row.IsAutoUploadCloud" active-color="#0077FF" inactive-color="#DCE0E7"
                                       :active-value="1" :inactive-value="0"
                                       @change="(value) => autoUploadcloudChange(value, row.SecUid)">
                            </el-switch>
                        </template>

                        <template #YesterdayAverage="{ row }">
                            <Average :row="row"/>
                            <!--                            <el-popover placement="bottom" trigger="hover" v-if="row.YesterdayAverageObservationNum > 0||row.YesterdayAverageVolumeStart > 0">-->
                            <!--                                <template slot="reference">-->
                            <!--                                    <Average :row="row"/>-->
                            <!--                                </template>-->
                            <!--                                <AverageLive :record="row.YesterdayRecordList"/>-->
                            <!--                            </el-popover>-->
                            <!--                            <Average :row="row" v-else/>-->
                        </template>
                        <template #RecordTime="{ row }">
                            <div>{{ row.RecordTime || '-' }}</div>
                            <div v-if="!row.InRecordTime">不在时间段内</div>
                        </template>
                        <template #YesterdayRecordList="{ row }">
                            <el-popover placement="bottom" trigger="hover" v-if="row.YesterdayRecordNum">
                                <el-button slot="reference" class="cs-p" @click="toAnalysis(row)" type="text">
                                    {{ row.YesterdayRecordList?.length || 0 }}
                                </el-button>
                                <!--                                <div class="time-show-box">-->
                                <!--                                    <div class="time-show-title">昨日录制时间:</div>-->
                                <!--                                    <p v-for="(item, index) in row.YesterdayRecordList">-->
                                <!--                                        <span class="dot-span" :class="`s-${index % 3}`"></span>-->
                                <!--                                        {{ item.recordDate }}-->
                                <!--                                    </p>-->
                                <!--                                </div>-->
                                <YesterdayRecord :record="row.YesterdayRecordList"/>
                            </el-popover>
                            <span v-else>{{ 0 }}</span>
                        </template>
                        <template #RecordStatus="{ row, $index }">
                            <div v-if="row.RecordStatus == 0">未录制</div>
                            <div v-if="row.RecordStatus == 1">
                                <div style="color: #FC4F52;">
                                    <span class="dot-span s-0"></span>录制中
                                </div>
                                <div style="margin-top: 4px;" v-html="row.StartTime?.split(' ')?.join('<br />')"></div>
                            </div>
                            <div v-if="row.RecordStatus == 2">手动停止</div>
                            <div v-if="row.RecordStatus == 3">录制完成</div>
                            <div v-if="row.RecordStatus == 4">手动开启中</div>
                        </template>
                        <template #OnlineNumber="{row}">
                            <span v-if="detection && row?.LiveStatus == 2 && configInfo?.OnlineNumber == 1">
                                {{ row.OnlineNumber ? row.OnlineNumber : 0 }}</span>
                            <span v-else>-</span>
                        </template>
                        <template #rate="{ row }">
                            <div class="flex-jc-c text-center" style="width: 100%;">
                                <span v-if="!row.YesterdaySessionRatio">{{ row.YesterdaySessionRatio || 0 }}%</span>
                                <span v-else class="compere-table-rate"
                                      :class="row.YesterdaySessionRatio < 0 ? 'donw-color' : 'up-color'">
                                    {{ ((row.YesterdaySessionRatio || 0) * 100).toFixed(1) }}%<i
                                    :class="row.YesterdaySessionRatio < 0 ? 'el-icon-bottom' : 'el-icon-top'"
                                    style="font-size: 17px;"></i>
                                </span>
                            </div>
                        </template>
                    </Table>
                </Card>
            </template>
        </div>
        <!-- 列表没有数据 -->
        <Table v-else class="home-table" :data="[]" :column="columnConfig" :menuConfig="{ width: '120px' }">
            <template #empty="{ row }">
                <div class="emptyContainer">
                    <img style="max-width: 300px;margin-bottom: 40px;" src="@/assets/imgs/1_9_30/hEmpty.png" alt=""
                         srcset="">
                    <div class="emptyTipText" style="margin-bottom: 10px;">暂无直播间</div>
                    <afp-button type="primary" :plain="false" size="default" id="to-add-compere" @click="toAddCompere">点我添加直播间</afp-button>
                </div>
            </template>
        </Table>
        <scrollingHint ref="scrollingHint"></scrollingHint>

        <scrollingWarmHint ref="scrollingWarmHint"></scrollingWarmHint>

        <IsDataViewingHint ref="IsDataViewingHint"></IsDataViewingHint>
        <IsDataViewingWarmHint ref="IsDataViewingWarmHint"></IsDataViewingWarmHint>
        <LiveRoomAuthorizeDialog ref="liveRoomAuthorizeDialog" @authorized="handleLifeAuthorized" />
        <CancelAuthDialog ref="cancelAuthDialog" @canceled="handleCanceled" />
        <LifeAuthDialog v-if="false" ref="lifeAuthDialog" @authorized="onLifeAuthorized"/>
    </div>
</template>

<script>
import Mixin from './mixin'
import buyIn from '@/mixins/buyIn'
import LiveRoomAuthorizeDialog from '@/components/liveRoomAuthorizeDialog.vue'
import CancelAuthDialog from '@/components/cancelAuthDialog.vue'
import scrollingHint from './../dialog/scrollingHint.vue'
import scrollingWarmHint from './../dialog/scrollingWarmHint.vue'
import YesterdayRecord from './common/yesterdayRecord.vue'
import AverageLive from './common/averageLive.vue'
import IsDataViewingHint from '../dialog/IsDataViewingHint.vue'
import IsDataViewingWarmHint from '../dialog/IsDataViewingWarmHint.vue'
import Average from './common/average.vue'
import {DEFINITION_LIST, PLATFORM_ENUM} from '@/enum'

export default {
    components: {
        LiveRoomAuthorizeDialog,
        CancelAuthDialog,
        scrollingHint,
        scrollingWarmHint,
        YesterdayRecord,
        IsDataViewingHint,
        IsDataViewingWarmHint,
        Average,
        AverageLive
    },
    mixins: [Mixin, buyIn],
    props: {
    },
    data() {
        return {
            PLATFORM_ENUM,
            accountType: '',
            definitionList: DEFINITION_LIST,
            buyInStatus: new Map([
                [0, '点我授权'],
                [1, '已授权'],
                [2, '授权过期'],
                [3, '授权失败'],
                [4, '授权中'],
                [5, '授权账号不匹配'],
                [6, '子账号无权限']
            ]),
            qianchuanStatus: new Map([
                [0, '未授权'],
                [1, '已授权'],
                [2, '授权过期'],
                [3, '授权失败'],
                [4, '授权中'],
                [5, '授权抖音号不匹配']
            ]),
            // 状态 0：未授权 1：已授权 2：微信后台取消授权, 3：用户取消授权
            authChannelStatus: new Map([
                [0, '未授权'],
                [1, '已授权'],
                [2, '微信后台取消授权'],
                [3, '用户取消授权'],
            ]),
        }
    },
    inject: ['appVnode'],
    computed: {
        // -1 激活版 0免费版 10个人 20企业 30旗舰
        getPackageLevel() {
            return this.$store.state?.userInfo?.packageLevel
        },
        getTotalAnchorBarrageNum() {
            return this.$store?.getters?.getUserproperty?.totalAnchorBarrageNum || 0
        },
        getAnchorBarrageNum() {
            return (this.getTotalAnchorBarrageNum - this.$store?.getters?.getUserproperty?.anchorBarrageNum) || 0
        },
        isToUpgrades() {
            // 如果可激活弹幕监听位和已激活监听位相减之后小于等于0，并且当前套餐等级不是旗舰版，则返回true用于提示用户去升级
            return (this.getTotalAnchorBarrageNum - this.getAnchorBarrageNum) <= 0 && this.getPackageLevel !== 30
        },
        isDataBoard() {
            return this.$store.getters.getUserproperty.dataBoardNum <= 0
        },
        localCompereMapList() {
            if (this.accountType !== '') {
                return this.filterListByAccountType(this.getCompereMapList, this.accountType);
            }
            return this.getCompereMapList
        },
        buyInClass() {
            return (buyIn) => {
                if (buyIn === 0) {
                    return 'buyIn-default'
                } else if (buyIn === 1) {
                    return 'buyIn-success'
                } else if (buyIn === 2) {
                    return 'buyIn-time'
                } else if (buyIn === 3) {
                    return 'buyIn-error'
                } else if (buyIn === 4) {
                    return 'buyInIng'
                } else if (buyIn === 5) {
                    return 'buyIn-error'
                }
            }
        }
    },
    watch: {},
    methods: {
        getQianchuanAuthStatus(row) {
            const value = row?.qianchuanAuthStatus
            const status = value === undefined || value === null || value === '' ? 0 : Number(value)
            return Number.isNaN(status) ? 0 : status
        },
        openAuthorizeDialog(row) {
            this.$refs.liveRoomAuthorizeDialog?.open?.(row?.SecUid)
        },
        handleLifeAuthorized() {
            this.parent.getDataList()
        },
        openCancelAuthDialog(row) {
            this.$refs.cancelAuthDialog?.open?.(row?.SecUid)
        },
        handleCanceled() {
            this.parent.getDataList()
        },
        toDouyin(){
            window.open('https://www.douyin.com/jingxuan/');
        },
        notSwitch(row){
            return row.platform === PLATFORM_ENUM.kuaishou || row.platform === PLATFORM_ENUM.shipinhao
        },
        openBuyIn(secUid){
            this.$httpClient.system.openUrl({url:'https://buyin.jinritemai.com/dashboard'})
            // this.$confirm(`
            // <div>使用爱复盘打开巨量，能有效降低授权失效时间。但是请使用授权的同一个主账号或者子账号来登录，否则数据会出现错乱。</div>
            // `, '打开巨量', {
            //     confirmButtonText: '打开巨量',
            //     cancelButtonText: '取消',
            //     dangerouslyUseHTMLString: true,
            //     customClass: 'buy-in-front',
            //     distinguishCancelAndClose: true,
            //     closeOnClickModal: false,
            //     closeOnPressEscape: false,
            //     showClose: false
            // }).then(async () => {
            //     this.$httpClient.system.openUrl({url:'https://buyin.jinritemai.com/dashboard'})
            //     // this.$httpClient.buyIn.openAnchorJuliang({secUid})
            // }).catch(() => {
            //
            // });
        },
        authorizeQianchuanHandler(row){
            this.$httpClient.qianchuan.authorizeQianchuan({secUid: row.SecUid})
        },
        // TODO: 已迁移至统一取消授权入口 cancelAuthDialog
        // /**
        //  * @description 取消主播千川授权，确认后调用客户端接口并刷新当前列表。
        //  * @param {Object} row 主播行数据
        //  * @returns {Promise<void>}
        //  */
        // async cancelAuthorizeQianchuanHandler(row) {
        //     const secUid = row?.SecUid
        //     if (!secUid) {
        //         this.$message.error('获取主播信息失败，请稍后重试')
        //         return
        //     }
        //     try {
        //         await this.$confirm('取消授权后将停止该主播的千川授权状态与相关数据采集，是否继续？', '取消千川授权', {
        //             confirmButtonText: '确定取消',
        //             cancelButtonText: '暂不取消',
        //             type: 'warning'
        //         })
        //         const res = await this.$httpClient.qianchuan.cancelAuthorizeQianchuan({ secUid })
        //         if (res?.code === 0) {
        //             this.$message.success('取消千川授权成功')
        //             this.parent.getDataList()
        //             return
        //         }
        //         this.$message.error(res?.msg || '取消千川授权失败')
        //     } catch (error) {
        //         if (error !== 'cancel') {
        //             this.$message.error('取消千川授权失败')
        //         }
        //     }
        // },
        toHotList(item){
            // if(this.$store.getters.isFree || this.$store.getters.isActivated){
            //     this.appVnode.versionQrCodeShow()
            //     return;
            // }
            this.$router.push({
                path: this.$route.path + '/hotList',
                query: {
                    id: item?.trade?.id,
                    label: item.trade?.name || ''
                }
            })
        },
        filterListByAccountType(arr, targetType) {
            return arr
                .map(item => {
                    const filteredList = item.list.filter(i => i.AccountType === targetType);
                    if (filteredList.length > 0) {
                        return {...item, list: filteredList};
                    }
                    return null;
                })
                .filter(Boolean); // 移除空项
        },
        // 打开目录
        openDirectory(secUid) {
            this.$httpClient.compere.openfolder({secUid}).then((res) => {
            })
        },
        toAnalysis(row) {
            let item = row?.YesterdayRecordList?.[0]
            this.$router.push({
                path: '/replay',
                query: {
                    secUid: row.SecUid,
                    tabsName: 'all',
                    time: item.recordDate?.split(' ')?.[0]
                }
            })
        },
        //开启数据看板
        autoDataBoardChange(value, SecUid) {
            this.appVnode.getUserproperty(() => {
                let requestData = {
                    SecUid,
                    value: value ? 1 : 0
                }
                // 打开将会进入判断，关闭将会直接发起请求
                if (requestData?.value) {
                    if (this.isDataBoard) {
                        this.$refs.IsDataViewingWarmHint.show()
                        return
                    } else {
                        this.$refs.IsDataViewingHint.show({
                            data: {
                                countNum: this.$store.getters.getUserproperty?.totalDataBoardNum,
                                useNum: this.$store.getters.getUserproperty?.useDataBoardNum,
                                callback: () => {
                                    this.openOrCloseDataViewing(value, SecUid)
                                }
                            }
                        })
                    }
                } else {
                    this.openOrCloseDataViewing(value, SecUid)
                }
            })
        },
        //开启弹幕监控
        openOrCloseDataViewing(value, SecUid) {
            this.$httpClient.compere.openOrCloseDataViewing({
                secUid: SecUid,
                isDataViewing: value ? 1 : 0
            }).then((res) => {
                if (res.code === 0) {
                    this.$message.success('修改成功')
                    this.parent.getDataList()
                    this.$refs?.IsDataViewingHint?.hide()
                }
            })
        },
        // 开启弹幕监控
        autoBarrageMonitoringChange(value, SecUid) {
            this.appVnode.getUserproperty(() => {
                let requestData = {
                    SecUid,
                    IsBarrageMonitoring: value ? 1 : 0
                }

                // 打开将会进入判断，关闭将会直接发起请求
                if (requestData?.IsBarrageMonitoring) {
                    // 如果可激活监控位为0，或者当前套餐等级不是旗舰版，并且已经使用完资源，则提示用户去升级
                    if (this.getTotalAnchorBarrageNum <= 0 || this.isToUpgrades) {
                        this.$refs.scrollingWarmHint.show()
                        return
                    } else {
                        this.$refs.scrollingHint.show({
                            data: {
                                countNum: this.getTotalAnchorBarrageNum,
                                useNum: this.getAnchorBarrageNum,
                                callback: () => {
                                    this.barrageMonitoringChange(requestData)
                                }
                            }
                        })
                    }
                } else {
                    this.barrageMonitoringChange(requestData)
                }
            })
        },

        /**
         * @description 自动上传云空间开关变更（2.6.2.1 简易版，无权限弹窗）
         * @param {number} value - 开关值 0/1
         * @param {string} SecUid - 主播id
         */
        autoUploadcloudChange(value, SecUid) {
            this.$httpClient.compere.openOrCloseAutoUploadCloud({
                secUid: SecUid,
                isAutoUploadCloud: value ? 1 : 0
            }).then((res) => {
                if (res.code == 0) {
                    this.$message.success('修改成功')
                    this.parent.getDataList()
                }
            })
        },

        barrageMonitoringChange(requestData) {
            /*
                secUid:主播id
                IsBarrageMonitoring:弹幕监控 0否 1是
            */
            this.$httpClient.compere.updateBarrageMonitoring(requestData).then((res) => {
                if (res.code == 0) {
                    this.$refs.scrollingHint.hide()
                    this.parent.appVnode.getUserproperty()
                    this.$message.success('修改弹幕监控成功')
                    this.parent.getDataList()
                }
            })

        },

        /**
         * @description 自动上传云空间开关变更（2.6.2.1 简易版，无权限弹窗）
         * @param {number} value - 开关值 0/1
         * @param {string} SecUid - 主播id
         */
        autoUploadcloudChange(value, SecUid) {
            this.$httpClient.compere.openOrCloseAutoUploadCloud({
                secUid: SecUid,
                isAutoUploadCloud: value ? 1 : 0
            }).then((res) => {
                if (res.code == 0) {
                    this.$message.success('修改成功')
                    this.parent.getDataList()
                }
            })
        },

        // 删除主播
        removeCompere(item) {
            if (item.RecordStatus == 1) {
                this.$message.error('主播正在录制中，请停止录制后再删除')
                return
            }
            this.$confirm('将删除直播间, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.$httpClient.compere.removeanchor({secUid: item.SecUid}).then((res) => {
                    if (res.code == 0) {
                        this.$message.success('删除成功')
                        this.parent.getDataList();
                    }
                })
            })
        },
        startClick() {
            this.$nextTick(() => {
                this.$refs.allTitle.startClick()
            })
        },
        // 开启主播录制
        startRecord(secUid) {
            if (this.getClickTimerLock(secUid)) {
                this.$message.warning('请等待倒计时结束后再操作')
                return
            }
            // 启动限制点击定时器
            this.clickTimerLock(secUid)
            sessionStorage.setItem('notLoading', '')
            this.$httpClient.compere.startRecord({secUid}).then((res) => {
                if (res.code == 0) {
                    this.parent.getDataList()
                    this.$message.success('操作成功')
                    sessionStorage.setItem('notLoading', '1')
                }
            })
        },
        getCompereList(param) {
            return this.getDataList(0, param)
        }
    },
    created() {

    },
    mounted() {
        this.watchAuthorizedBuyInSuccess(() => {
            this.parent.getDataList()
        })
        this.watchAuthorizedBuyInError()
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
.compere-table-rate {
    font-weight: 600;
    font-size: 14px;
    line-height: 16px;
    display: flex;
    align-items: center;

    > i {
        font-weight: 600;
        font-size: 15px !important;
        margin-bottom: 1px;
    }
}

.dataAnalysis-card{
    ::v-deep(.el-card__header){
        padding: 10px 12px 12px 12px;
    }
}

.common-card {
    height: 99px;
    width: 176px;
    border-radius: 10px;
    box-shadow: 0 5px 10px 0 rgba(153, 169, 216, 0.10);
    background: radial-gradient(107.81% 407.56% at 108.54% 107.5%, rgba(138, 204, 255, 0.5) 0%, rgba(189, 216, 255, 0.5) 100%);
}

.anchorNum {
    background: radial-gradient(355.86% 142.06% at 79.46% 107.5%, rgba(177, 205, 255, 0.50) 0%, rgba(227, 225, 255, 0.50) 100%);
}

.recording {
    background: radial-gradient(355.86% 142.06% at 79.46% 107.5%, rgba(208, 177, 255, 0.50) 0%, rgba(227, 225, 255, 0.50) 100%);
}

.account-type {
    //padding-top: 12px;

    .text-title {
        font-size: 13px;
    }

    .radio-as-checkbox {
        ::v-deep(.el-radio__label) {
            font-size: 13px;
        }
    }
}

.time-show-box {
    .time-show-title {
        font-weight: 400;
        font-size: 14px;
        color: #151917;
        line-height: 22px;
    }

    p {
        font-size: 12px;
        color: #151917;
        margin: 0;
        padding: 3px 0;
        vertical-align: middle;
        text-align: left;
    }
}

.teble-Paragraph {
    font-weight: 400;
    font-size: 14px;
    color: #4D4D4D;
    line-height: 22px;
}

.teble-VedioSizie {
    font-weight: normal;
    font-size: 14px;
    color: #4D4D4D;
    line-height: 20px;
}

.buyIn-default {
    color: var(--color-main);
    cursor: pointer;
}

.buyIn-success {
    color: #61B593;
}

/* TODO: 已迁移至统一取消授权入口 cancelAuthDialog */
/* .qianchuan-cancel-btn {
    margin-left: 8px;
    color: #909399;
    font-size: 12px;
    line-height: 18px;
    cursor: pointer;
}

.qianchuan-cancel-btn:hover {
    color: #606266;
} */

.buyIn-error {
    color: #f60808;
    cursor: pointer;
}
.buyInIng{
    color: #F4D05A;
}
.buyIn-time {
    cursor: pointer;
    color: #f60808;
}
</style>

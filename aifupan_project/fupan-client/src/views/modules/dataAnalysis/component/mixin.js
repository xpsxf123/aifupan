import Title from './common/title.vue'
import Anchor from './common/anchor.vue'
import Card from './common/homeCard.vue'
import Table from '/src/components/Table/index.vue'
import Operation from '/src/components/Table/operation.vue'
import {VERSION_TYPE} from "@/enum";
import { setUserAnchorCount } from '@/utils/aiAgentRoute'

export default {
    inject: ['parent'],
    components: {
        Title,
        Anchor,
        Card,
        Table,
        Operation
    },
    props: {},
    data () {
        return {
            columnConfig: [
                {
                    label: '直播间',
                    prop: 'anchor',
                    option: {
                        width: '200'
                    }
                },
                
                {
                    label: '平台授权',
                    prop: 'buyIn',
                    option: {
                        width: '160'
                    },
					hidden: () => {
						return this.versionTypeIsPure
					}
                },
                // {
                //     label: '自动检测录制',
                //     prop: 'IsAutoRecord',
                //     option: {
                //         width: '105'
                //     }
                // },
                {
                    label: '数据看板',
                    prop: 'IsDataViewing',
                    hidden: () => {
                        return !this.isAll || this.versionTypeIsPure
                    }
                },
                {
                    label: '弹幕监控',
                    prop: 'IsBarrageMonitoring',
                    option: {
                        width: '105'
                    },
                    hidden: () => {
                        return !this.isAll || this.versionTypeIsPure
                    }
                },
                {
                    label: '自动上传云空间',
                    prop: 'IsAutoUploadCloud',
                    option: {
                        width: '120'
                    },
                    hidden: () => {
                        return !this.isAll || this.versionTypeIsPure
                    }
                },
                 {
                    label: '昨日平均',
                    prop: 'YesterdayAverage',
                    option: {
                        width: '160'
                    },
                    hidden: () => {
                        return !this.isAll || this.versionTypeIsPure
                    }
                },
                {
                    label: '录制状态',
                    prop: 'RecordStatus',
                    hidden: () => {
						return this.isAll && !this.versionTypeIsPure;
                    }
                },
				{
					label: '清晰度',
					prop: 'recordDefinition',
					hidden: () => {
						return !this.isAll || !this.versionTypeIsPure;
					}
				},
				{
					label: '分段时长',
					prop: 'recordLimitValue',
                    formatter: (row) => {
                        return `${row.recordLimitValue > 0 ? row.recordLimitValue : this.configInfo?.LimitValue}分钟`
                    },
					hidden: () => {
						return !this.isAll || !this.versionTypeIsPure;
					}
				},
				{
					label: '在线人数',
					prop: 'PureRecordOnlineNum',
					hidden: () => {
						return !this.isAll || !this.versionTypeIsPure;
					},
					// formatter:(row)=>{
					//     return row.LiveStatus == 2 && configInfo.OnlineNumber == 1
					// }
				},
                {
                    label: '在线人数',
                    prop: 'OnlineNumber',
                    hidden: () => {
						return this.isAll
                    },
                    // formatter:(row)=>{
                    //     return row.LiveStatus == 2 && configInfo.OnlineNumber == 1
                    // }
                },
                {
                    label: '昨日录制',
                    prop: 'YesterdayRecordList',
                    option: {
                        width: '80'
                    },
                    hidden: () => {
                        return !this.isAll
                    },
                },
                {
                    label: '视频文件夹',
                    prop: 'Folder',
                    hidden: () => {
                        return !this.isAll
                    },
                    option: {
                        width: '90'
                    }
                },
                {
                    label: '知识库',
                    prop: 'knowledgeBase',
                    hidden: () => {
                        return !this.isAll
                    },
                    option: {
                        width: '90'
                    }
                },
				{
					label: '录制表',
					prop: 'recordList',
					hidden: () => {
						return !this.isAll || !this.versionTypeIsPure;
					},
					formatter(row) {
						return '查看'
					},
					option: {
						width: '90'
					}
				},
				{
					label: '主页',
					prop: 'home',
					hidden: () => {
						return !this.isAll || !this.versionTypeIsPure;
					},
					formatter(row) {
						return '查看'
					},
					option: {
						width: '90'
					}
				},
                // {
                //     label: '监控时间段',
                //     prop: 'RecordTime',
                //     hidden: () => {
                //         return !this.isAll
                //     },
                //     option: {
                //         width: '200'
                //     }
                // },
                // {
                //     label: '昨日平均人数',
                //     prop: 'YesterdaySessionAverageNum',
                //     hidden: () => {
                //         return !this.isAll
                //     },
                //     formatter(row) {
                //         return myUtils.numberToSting(row.YesterdaySessionAverageNum || 0)
                //     }
                // },
                // {
                //     label: '人数环比',
                //     prop: 'rate',
                //     hidden: () => {
                //         return !this.isAll
                //     },
                // },

                {
                    label: '大小时长',
                    prop: 'VedioSizie',
                    hidden: () => {
                        return this.isAll
                    },
                }
            ],
            options: [
                {
                    label: '复盘表',
                    hidden: () => {
                        return !this.isAll || this.versionTypeIsPure
                    },
                    click: (item) => {
						this.openRecordList(item)
                    },
                    icon: 'icon-a-Frame978'
                },
                {
                    label: '看直播',
                    hidden: (row) => {
                        return this.isAll || row.platform === 2
                    },
                    click: (item) => {
                        this.openToBrowser(item.LiveUrl)
                    }
                },
                {
                    label: '开始录制',
                    hidden: (item) => {
                        // 非列表隐藏按钮，非录制中隐藏按钮，非直播中隐藏按钮，不是未录制或者
                        return (!this.isAll) || !this.detection || ![2,3].includes(item.RecordStatus)
                    },
                    click: (item) => {
                        this.startRecord(item.SecUid)
                    }
                },
                {
                    label: '停录',
                    type: 'danger',
                    hidden: () => {
                        return this.isAll
                    },
                    disabled: (item) => {
                        return !!this.itemBtnClickData[item.SecUid]
                    },
                    click: (item) => {
                        this.stopRecord(item.SecUid);
                    }
                },
                [
                    {
                        label: '主页',
                        hidden: (row) => {
                            return !this.isAll || row.platform === 2 || this.versionTypeIsPure
                        },
                        click: (item) => {
                            this.openToBrowser(item.HomeUrl)
                        }
                    },
                    {
                        label: '开启自动录制',
                        hidden: (item) => {
                            return (!this.isAll) || item?.IsAutoRecord === 1
                        },
                        click: (item) => {
                            this.autoRecordChange(1, item.SecUid)
                        },
                    },
                    {
                        label: '暂停自动录制',
                        hidden: (item) => {
                            return (!this.isAll) || item?.IsAutoRecord === 0
                        },
                        click: (item) => {
                            this.autoRecordChange(0, item.SecUid)
                        },
                    },
                    {
                        label: '主页',
                        hidden: (row) => {
                            return this.isAll || row.platform === 2
                        },
                        click: (item) => {
                            this.openToBrowser(item.HomeUrl)
                        },
                        icon: 'icon-a-zhuye1'
                    },
                    // {
                    //   label: '预览',
                    //   hidden:()=>{
                    //     return this.isAll
                    //   },
                    //   click:(item)=>{
                    //     this.previewVideo(item.SecUid, item.LiveStatus)
                    //   },
                    //   icon: 'icon-a-bukechakan2'
                    // },
                    // {
                    //     label: '视频文件夹',
                    //     click: (item) => {
                    //         this.openDirectory(item.SecUid)
                    //     },
                    //     icon: 'icon-a-Frame1220'
                    // },
                    // {
                    //     label: '行业',
                    //     click: (data) => {
                    //         this.parent.showTrade(data.SecUid, data.TradeId)
                    //     },
                    //     icon: 'icon-xingye',
                    //     iconSize: '16'
                    // },
                    {
                        label: '拉取数据',
                        hidden: (item) => {
                            return !this.$store.getters.getMode || Number(item?.juliangAuthStatus) !== 1
                        },
                        click: (item) => {
                            this.pullVideoDataForRow(item)
                        },
                    },
                    {
                        label: '基础设置',
                        click: (data) => {
                            // this.parent.showTrade(data.SecUid, data.TradeId)
                            this.parent.showTrade(data)
                        },
                        icon: 'icon-xingye',
                        iconSize: '16'
                    },
                    {
                        label: '取消授权',
                        hidden: (item) => {
                            return !this.isAll || !(item.platform === 0 && item.AccountType === 0)
                        },
                        click: (item) => {
                            this.openCancelAuthDialog(item)
                        }
                    },
                    {
                        label: '删除',
                        type: 'danger',
                        // url: del,
                        // hoverUrl: hoverDel,
                        hidden: () => {
                            return !this.isAll
                        },
                        click: (item) => {
                            return this.removeCompere(item)
                        },
                        icon: 'icon-shanchu',
                        iconSize: '16'
                    }
                ]
            ],
            itemBtnClickData: {}
        }
    },
    computed: {
        isAll () {
            return this.parent.isAll
        },
        configInfo () {
            return this.parent.configInfo
        },
        detection () {
            return this.parent.detection
        },
        compereList () {
            return this.parent.compereList
        },
        compereMapList () {
            return this.parent.compereMapList
        },
        compereInfo () {
            return this.parent.compereInfo
        },
        detectionTime () {
            return this.parent.detectionTime
        },
        getTotalMonitorNum () {
            return this.$store.getters?.getUserproperty?.totalMonitorNum || 0
        },
        getCompereMapList () {
            // 数据排序，按照大行业，在按照行业主播数
            return Object.values(this.parent.compereMapList)
                .sort((a, b) => {
                    return a.trade?.sort - b.trade?.sort
                }).sort((a, b) => {
                    return b.list?.length - a.list?.length
                }) || []
        },
        localCompereMapList() {
            if (this.accountType !== 2) {
                return this.filterListByAccountType(this.getCompereMapList, this.accountType);
            }
            return this.getCompereMapList
        },
		versionType(){
			return this.$store.getters.getVersionType
		},
		versionTypeIsPure(){
			return this.versionType === VERSION_TYPE.PURE
		},
    },
    watch: {},
    methods: {

        // 开启、关闭自动录制
        autoRecordChange (value, SecUid) {

            let requestData = {
                SecUid,
                isAuto: value ? 1 : 0
            }
            this.$httpClient.compere.openorcloseautorecord(requestData).then((res) => {
                if (res.code == 0) {
                    this.$message.success('修改成功')
                    this.parent.getDataList()
                }
            })
        },

        // 获取主播列表
        getDataList (type, param) {
            let requestData = {
                pageIndex: 1,
                pageSize: 9999999,
                anchorName: param.anchorName ? param.anchorName : '',
                recordStatus: type === 0 ? null : 1,
                isRemoveRecord: 0,
                tradeId: ''
            }
            return this.$httpClient.compere.getpageanchor(requestData).then( async (res) => {
                if (res.code == 0) {
                    try {
                        // 储存主播信息
                        const compereInfo = {
                            ...this.parent.compereInfo,
                            Total: type === 0 ? res.data.Total : this.parent.compereInfo.Total,
                            AllTotal: type === 0 ? res.data.Total : this.parent.compereInfo.AllTotal,
                            CurrentRecordNum: res.data.CurrentRecordNum,
                            CurrentLiveNum: res.data.CurrentLiveNum,
                        }
                        this.$set(this.parent, 'compereInfo', compereInfo)
                        this.$store.commit("setCompereInfo", compereInfo);
                        if (type === 0) {
                            const userId = this.$store.state.userInfo?.id
                            setUserAnchorCount(userId, res?.data?.Total || res?.data?.DataList?.length || 0)
                            let compereMapList = {}
                            res?.data?.DataList?.forEach(item => {
                                let trade = this.parent.tradeMap[item.TradeId]
                                if (!trade?.id) {
                                    return
                                }
                                if (item.SessionList?.length > 1) {
                                    item.SessionList = this.getSessionList(item.SessionList)
                                }
                                if (!Array.isArray(compereMapList[trade?.id]?.list)) {
                                    compereMapList[trade?.id] = {
                                        trade,
                                        hotListCount: this.parent.compereMapList[trade?.id]?.hotListCount || 0,
                                        list: []
                                    }
                                }
                                compereMapList[trade?.id]?.list?.push(item)
                            })
                            this.parent.compereMapList = compereMapList
                            // 记录主播列表总数
                            this.parent.compereListLen = res.data.DataList.length;
                            // if(Object.keys(compereMapList).every(d=>))
                            this.countSimilarAnchors();
                        } else {
                            this.parent.compereList = res.data.DataList
                        }
                    } catch (err) {
                        console.error(err)
                    }
                }
            }).catch(err => {
                console.error(err)
            })
        },
        countSimilarAnchors(){
            let tIds = Object.keys(this.parent.compereMapList) || []
            if(tIds.length === 0){
                return
            }
            this.$httpBack.words.countSimilarAnchors({
                tradeIds: tIds.join(',')
            }).then((res) => {
               if(res.code === 0){
                    res.data.forEach(item => {
                        let old = this.parent.compereMapList[item.tradeId];
                        if(old !== item.count){
                            this.$set(this.parent.compereMapList[item.tradeId], 'hotListCount', item.count)
                        }
                    })
               }
            })
        },
        // 在线复盘时长充值
        rechargeTime () {
            this.kefuDialogVisible = true
            this.$nextTick(() => {
                this.parent.$refs.customerServiceQrCode.init()
            })
        },
        // 跳转到添加主播页面
        toAddCompere () {
            // this.$emit('updateMenuIndex', 1);
            this.$router.push({
                path: '/addCompere'
            })
        },
        startHanlder () {
            this.parent.startHanlder()
        },
        stopHandler () {
            this.parent.stopHandler()
        },
        // 打开主播主页
        openToBrowser (url) {
            window.open(url, '_blank')
        },
        updatePureRecordOnlineNum(value,item){
            this.$httpClient.compere.updatePureRecordOnlineNum({
                secUid: item.SecUid,
                pureRecordOnlineNum: value
            }).then((res) => {
                if (res.code === 0) {
                    this.$message.success('修改成功')
                    this.parent.getDataList()
                }
            })
        },
        autoOnlineNumberPureChange(value, item) {
            if (value == 1) {
                this.$confirm(`
                    <div style="text-align: center;height: 120px;" class="flex items-center justify-center">
                        <div>
                         越多直播间开启在线人数开关，<span style="color: red">越容易引发风控问题</span> ，有可能无法录制在线，纯录制版客户端不提供防风控网络IP服务，如果录制不到在线，请重启光猫
                        </div>
                    </div>`, '温馨提示', {
                    confirmButtonText: '确定开启',
                    cancelButtonText: '不开启',
                    customClass: 'edit-file-name',
                    showClose: true,
                    closeOnClickModal: false,
                    closeOnPressEscape: false,
                    dangerouslyUseHTMLString: true,
                    center: true
                }).then(async () => {
                    await this.updatePureRecordOnlineNum(value, item)
                }).catch(async () => {
                });
            } else {
                this.updatePureRecordOnlineNum(value, item)
            }
        },
		openRecordList(item){
			this.$router.push({
				path: '/replay',
				query: {
					secUid: item.SecUid,
					tabsName: 'all'
				}
			})
		},
        // 预览视频
        previewVideo (secUid, status) {
            if (status != 2) {
                this.$message.error('未开始检测或主播未开播')
                return
            }
            this.$httpClient.compere.previewvideo({ secUid }).then((res) => { })
        },
        // 打开目录
        openDirectory (secUid) {
            this.$httpClient.compere.openfolder({ secUid }).then((res) => {
            })
        },
        // 设置定时锁
        clickTimerLock (secUid) {
            this.itemBtnClickData[secUid] = 30;
        },
        // 获取定时锁
        getClickTimerLock (secUid) {
            return this.itemBtnClickData[secUid] > 0
        },
        // 手动拉取视频数据
        pullVideoDataForRow (row) {
            const secUid = row?.SecUid
            if (!secUid) return this.$message.warning('缺少主播信息')
            this.$httpClient.buyIn.pullVideoData({ secUid, platform: 'juliang' }).then(res => {
                if (res?.code === '0' || res?.code === 0) {
                    this.$message.success('已触发拉取，请稍后刷新列表查看')
                } else {
                    this.$message.warning(res?.msg || '拉取失败')
                }
            }).catch(() => {
                this.$message.error('拉取请求失败')
            })
        },
    },
    created () {

    },
    mounted () {

    },
    beforeCreate () { }, //生命周期 - 创建之前
    beforeMount () { }, //生命周期 - 挂载之前
    beforeUpdate () { }, //生命周期 - 更新之前
    updated () { }, //生命周期 - 更新之后
    beforeDestroy () { }, //生命周期 - 销毁之前
    destroyed () { }, //生命周期 - 销毁完成
    activated () { }, //如果页面有keep-alive缓存功能，这个函数会触发
}

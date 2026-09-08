<template>
  <div class="compereContainer common-bg">
    <div>
      <Tabs v-model="activeName" :is-scroll="true" :tabs="tabs" :reload="true" class="home-tabs" content-padding="20px">
        <!-- 公共模块 -->
        <template #common>
          <!-- 搜索栏 -->
          <div>
            <el-form :inline="true" :model="dataForm">
              <el-form-item>
                <el-input placeholder="请输入直播间名称" size="small" v-model="dataForm.anchorName" clearable></el-input>
              </el-form-item>
              <el-form-item>
                <afp-button @click="keywordChange" type="primary" plain size="small">查询</afp-button>
              </el-form-item>
            </el-form>
          </div>
        </template>
        <template v-slot:first>
          <div v-loading="tradeLoading">
            <Title @leftClick="rechargeTime" :detection="detection" @start="startHanlder" @stop="stopHandler"
              :recordNum="compereInfo.CurrentRecordNum" type="all" :detectionTime="detectionTime"
              style="margin-bottom: 20px;" ref="allTitle">
              <template #titleLeft>
                <div>主播数量：<span>{{ compereInfo.Total || 0 }}</span></div>
                <div>直播中：<span>{{ compereInfo.CurrentLiveNum || 0 }}</span></div>
                <div>录制中：<span>{{ compereInfo.CurrentRecordNum || 0 }}</span></div>
              </template>
            </Title>
            <!-- 主播列表 -->
            <div v-if="getCompereMapList && getCompereMapList.length > 0" style="position: relative; " class="">
              <template v-for="(item, index) in getCompereMapList">
                <HomeCard style="margin-bottom: 20px;" :title="item.trade?.name">
                  <Table class="home-table" :data="item.list" :column="columnConfig" :menuConfig="{ width: '180px' }">
                    <template #anchor="{ row }">
                      <Anchor :item="row"></Anchor>
                    </template>
                    <template #IsAutoRecord="{ row }">
                      <el-switch v-model="row.IsAutoRecord" active-color="#0077FF" inactive-color="#DCE0E7"
                        :active-value="1" :inactive-value="0" @change="(value) => autoRecordChange(value, row.SecUid)">
                      </el-switch>
                    </template>
                    <template #SessionList="{ row }">
                      <el-popover placement="bottom" trigger="hover" v-if="row.SessionList?.length > 0">
                        <el-button slot="reference" type="text">{{ row.SessionList?.length || 0 }}</el-button>
                        <div class="time-show-box">
                          <div class="time-show-title">昨日场次及时间:</div>
                          <p v-for="(item, index) in row.SessionList">
                            <span class="dot-span" :class="`s-${index % 3}`"></span>{{ item.RecordDate }}
                          </p>
                        </div>
                      </el-popover>
                      <span v-else>{{ 0 }}</span>
                    </template>
                    <template #RecordStatus="{ row, $index }">
                      <div v-if="row.RecordStatus == 0">未录制</div>
                      <div v-if="row.RecordStatus == 1">
                        <div style="color: #FC4F52;">
                          <span class="dot-span s-0"></span>录制中
                        </div>
                        <div style="margin-top: 4px;">{{ row.StartTime }}</div>
                      </div>
                      <div v-if="row.RecordStatus == 2">手动停止</div>
                      <div v-if="row.RecordStatus == 3">录制完成</div>
                      <div v-if="row.RecordStatus == 4">手动开启中</div>
                    </template>
                    <template #rate="{ row }">
                      <span v-if="!row.YesterdaySessionRatio">{{ row.YesterdaySessionRatio || 0 }}%</span>
                      <span v-else class="compere-table-rate"
                        :class="row.YesterdaySessionRatio < 0 ? 'donw-color' : 'up-color'">
                        {{ getRatio(row.YesterdaySessionRatio) }}%<i
                          :class="row.YesterdaySessionRatio < 0 ? 'el-icon-bottom' : 'el-icon-top'"
                          style="font-size: 17px;"></i>
                      </span>
                    </template>
                    <template #menu="{ row }">
                      <Operation :options="options" :data="row"></Operation>
                    </template>
                  </Table>
                </HomeCard>
              </template>
            </div>
            <!-- 列表没有数据 -->
            <Table v-else class="home-table" :data="[]" :column="columnConfig" :menuConfig="{ width: '120px' }">
              <template #empty="{ row }">
                <div class="emptyContainer">
                  <img style="max-width: 300px;margin-bottom: 40px;" src="@/assets/imgs/1_9_30/hEmpty.png" alt=""
                    srcset="">
                  <div class="emptyTipText" style="margin-bottom: 10px;">暂无直播间</div>
                  <afp-button type="primary" @click="toAddCompere">点我添加直播间</afp-button>
                </div>
              </template>
            </Table>
          </div>
        </template>
        <template v-slot:second>
          <Title @leftClick="rechargeTime" :detection="detection" @stop="stopHandler" @start="startHanlder"
            :detectionTime="detectionTime" type="detection">
            <template #titleLeft>
              <div>录制中：<span>{{ compereInfo.CurrentRecordNum || 0 }}</span></div>
              <div>还可以录制：<span>{{ (getTotalMonitorNum || 0) - (compereInfo.CurrentRecordNum || 0) }}</span></div>
            </template>
          </Title>
          <Table style="padding-top: 12px;" class="home-table" :data="compereList" :column="columnConfig"
            :menuConfig="{ width: '170px' }">
            <template #anchor="{ row }">
              <Anchor :item="row"></Anchor>
            </template>
            <template #IsAutoRecord="{ row }">
              <el-switch v-model="row.IsAutoRecord" active-color="#0077FF" inactive-color="#DCE0E7" :active-value="1"
                :inactive-value="0" @change="(value) => autoRecordChange(value, row.SecUid)">
              </el-switch>
            </template>
            <template #RecordStatus="{ row, $index }">
              <div v-if="row.RecordStatus == 0">未录制</div>
              <div v-if="row.RecordStatus == 1">
                <div style="color: #FC4F52;">
                  <span class="dot-span s-0"></span>录制中
                </div>
                <div style="margin-top: 4px;">{{ row.StartTime }}</div>
              </div>
              <div v-if="row.RecordStatus == 2">手动停止</div>
              <div v-if="row.RecordStatus == 3">录制完成</div>
              <div v-if="row.RecordStatus == 4">手动开启中</div>
            </template>
            <template #VedioSizie="{ row }">
              <div v-if="row.RecordStatus === 1">
                <!-- 第几段 -->
                <div class="teble-Paragraph">{{ '第' + (row.Paragraph + 1) + '段' }}</div>                                                                                                                                                      
                <div class="teble-VedioSizie" style="margin-top: 1px;">
                  <span v-if="configInfo.HideSize != 1">
                    {{ row.VedioSizie ? row.VedioSizie + "M" : "" }}
                  </span>
                  <span v-if="configInfo.HideDuration != 1 && configInfo.HideSize != 1"
                    style="font-size: 14px;color: #909499;">|</span>
                  <span v-if="configInfo.HideDuration != 1">
                    {{ row.Duration }}
                  </span>
                </div>
              </div>
              <div v-else>-</div>
            </template>
            <template #menu="{ row }">
              <Operation :options="options" :data="row"></Operation>
            </template>
            <template #empty>
              <div class="emptyContainer">
                <img style="max-width: 350px;margin-bottom: 40px;" src="@/assets/imgs/1_9_30/hlz.png" alt="" srcset="">
                <div class="emptyTipText">暂无录制</div>
                <el-button type="text" @click="activeName = 'first'">返回录制</el-button>
              </div>
              <!-- <div class="emptyTipText">当前没有添加录制，请返回列表进行录制</div>
                <div class="emptyTipBtn" @click="activeName = 'first'">返回录制</div> -->
            </template>
          </Table>
        </template>
      </Tabs>
    </div>

    <!-- 行业弹窗 -->
    <el-dialog title="行业选择" :visible.sync="tardeDialogVisible" width="600px" :close-on-click-modal="false">
      <el-cascader v-model="selectTradeId" :options="tradeTreeList" style="width: 100%;" node-key="id"
        :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name', emitPath: false }" filterable
        placeholder="选择或者搜索输入一个行业，以提高分析准确性" @change="changeTradeHandle" ref="tradeCascader">
      </el-cascader>
      <span slot="footer" class="dialog-footer">
        <afp-button @click="tardeDialogVisible = false">取消</afp-button>
        <afp-button type="primary" @click="tradeConfirm()">确定</afp-button>
      </span>
    </el-dialog>

    <!-- 客服弹窗 -->
    <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>
    <!-- 自定义加载转圈 -->
    <loading :dialogText="loadingText" ref="loadingStop"></loading>

    <!-- 每天第一次打开提醒弹窗 -->
    <dayFirstDialog v-if="dayFirstVisible" ref="dayFirst" @beginRecord="startDetection"></dayFirstDialog>

    <!-- 校验时间弹窗 -->
    <!-- <timeDialog v-if="timeDialogVisible" ref="timeD" @closeClient="closeClient"></timeDialog> -->
  </div>
</template>

<script>
import myUtils from './../../../utils/utils';
import customerServiceQrCode from '../../commonComponent/customerServiceQrCode.vue';
import Tabs from '/src/components/Tabs/index.vue';
import Title from './../home/component/title.vue';
import Anchor from './../home/component/anchor.vue';
import HomeCard from '../home/component/homeCard.vue';
import Table from './../../../components/Table/index.vue';
import Operation from '/src/components/Table/operation.vue';
import loading from './loading.vue';
import dayFirstDialog from '../../commonComponent/dayFirstDialog.vue';
// import timeDialog from './timeDialog.vue';
export default {
  components: {
    customerServiceQrCode, Tabs, Title, Anchor, HomeCard, Table, Operation, loading, dayFirstDialog,
    // timeDialog 
  },
  data() {
    return {
      activeName: 'first',
      tabs: [{ label: '所有直播间', name: 'first' }, { label: '录制中', name: 'second' }],
      compereList: [], // 主播列表
      compereMapList: {},
      compereInfo: {},
      tradeList: [],
      tradeMap: {},
      tradeTreeList: [],
      kefuDialogVisible: false,
      loadingText: '正在停止录制和分析',
      loadingVisible: true,
      dayFirstVisible: false,
      // timeDialogVisible: false,

      // 智能复盘时长
      fupanTime: 216,
      fupanTimeAll: 2000,

      dataForm: {
        anchorName: "",
        recordStatus: ""
      },
      pageIndex: 1,
      detection: false, // 是否已开启检测
      pageSize: 999999,
      totalCount: 0,
      recordStatusList: [
        { value: -1, label: "全部" },
        { value: 0, label: "未录制" },
        { value: 1, label: "录制中" },
        { value: 2, label: "手动停止录制" },
        { value: 3, label: "录制完成" },
      ],

      recordNum: 0, // 正在录制的数量
      startDetectionTime: 0, // 开始检测的时间
      detectionTime: "0秒", // 已检测时间
      detectionTimeTimer: null, // 检测时间定时器
      listTimer: null, // 列表定时器
      // definitionList: ["标清", "高清", "超清", "蓝光"],
      // definition: 0,
      // recordModeList: ["ts", "flv"],
      // recordMode: 0,
      configInfo: {}, // 系统设置信息
      detectionBtnClickTime: 0, // 检测按钮点击的时间，用于限制30秒后才能再次点击
      itemBtnClickData: {}, // 主播开启/结束录制按钮点击的时间，用于限制30秒后才能再次点击
      itemBtnClickTimerData: {}, // 主播开启/结束录制按钮限制时间的定时器

      tardeDialogVisible: false,
      selectTradeId: '1',
      currentAnchorSecUid: "",
      userProperty: {
        monitorNum: 0,
        aiAnalysisTime: "加载中...",
        anchorNum: 0,
        totalMonitorNum: "加载中...",
        totalAiAnalysisTime: 0,
        totalAnchorNum: 0,
        videoTaggingTime: 0,
        totalVideoTaggingTime: 0,
        textTaggingWordCount: 0,
        totalTextTaggingWordCount: 0,
        storageNum: 0,
        totalStorageNum: 0
      },
      isAllTime: 10,
      loading: null,
      columnConfig: [
        {
          label: "主播",
          prop: 'anchor',
          option: {
            width: '200'
          }
        },
        {
          label: "是否自动录制",
          prop: 'IsAutoRecord',
        },
        {
          label: '录制状态',
          prop: 'RecordStatus',
          // hidden:()=>{
          //   return this.isAll
          // },
        },
        {
          label: '昨日场次',
          prop: 'SessionList',
          hidden: () => {
            return !this.isAll
          },
        },
        {
          label: '昨日平均人次',
          prop: 'YesterdaySessionAverageNum',
          hidden: () => {
            return !this.isAll
          },
          formatter(row) {
            return myUtils.numberToSting(row.YesterdaySessionAverageNum || 0)
          }
        },
        {
          label: '人次环比',
          prop: 'rate',
          hidden: () => {
            return !this.isAll
          },
        },

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
          label: '主页',
          hidden: () => {
            return !this.isAll
          },
          click: (item) => {
            this.openToBrowser(item.HomeUrl)
          }
        },
        {
          label: '看直播',
          hidden: () => {
            return this.isAll
          },
          click: (item) => {
            this.openToBrowser(item.LiveUrl)
          }
        },
        {
          label: '停录',
          type: 'danger',
          hidden: () => {
            return this.isAll
          },
          click: (item) => {
            this.stopRecord(item.SecUid)
          }
        },
        {
          label: '开始录制',
          hidden: (item) => {
            // 非列表隐藏按钮，非录制中隐藏按钮，非直播中隐藏按钮，不是未录制或者
            return (!this.isAll) || !this.detection || !(item.LiveStatus === 2 && ((item.RecordStatus == 0 || item.RecordStatus == 2 || item.RecordStatus == 3)))
          },
          click: (item) => {
            this.startRecord(item.SecUid);
          }
        },
        [
          {
            label: '复盘表',
            hidden: () => {
              return !this.isAll
            },
            click: (item) => {
              this.$emit('updateMenuIndex', 2);
              this.$store.commit("saveVideoListQuery", {
                anchorName: item.AnchorName,
                secUid: item.SecUid
              });
            },
            icon: 'icon-a-Frame978'
          },
          {
            label: '主页',

            hidden: () => {
              return this.isAll
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
          {
            label: '视频文件夹',
            click: (item) => {
              this.openDirectory(item.SecUid)
            },
            icon: 'icon-a-Frame1220'
          },

          {
            label: '行业',
            click: (data) => {
              this.showTrade(data.SecUid, data.TradeId)
            },
            icon: 'icon-xingye',
            iconSize: '16'
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
      ]
    };
  },
  computed: {
    isAll() {
      return this.activeName === 'first'
    },
    getCompereMapList() {
      // 数据排序，按照大行业，在按照行业主播数
      return Object.values(this.compereMapList).sort((a, b) => {
        return a.trade?.sort - b.trade?.sort
      }).sort((a, b) => {
        return b.list?.length - a.list?.length
      }).sort((a, b) => {
        return a?.YesterdaySessionAverageNum - b?.YesterdaySessionAverageNum
      }) || []
    },
    getTotalMonitorNum() {
      return this.$store.getters?.getUserproperty?.totalMonitorNum || 0
    }
  },
  created() {
    sessionStorage.setItem("notLoading", "");
  },
  activated() {
  },
  async mounted() {
    // 检测是否需要调用检测时间弹窗
    // this.getTimeAccurate();

    await this.getTradeList();
    // this.getUserProperty();
    // 获取配置信息
    this.getBasinSetupInfo();
    // 开启列表定时器
    this.startListTimer();

    // 每天第一次打开弹窗(登录成功后,设为true;当点击关闭后设为false)
    if (localStorage.getItem("currentPrompt") === 'true') {
      this.dayFirstOpen()
    }

    // 停止录制
    if (this.$store.state.stopRecord) {
      this.$store.commit("saveStopRecord", false);
      this.$httpClient.compere.stopdecector({}).then((res) => {
        if (res.code == 0) {
          this.$store.commit("saveDetectionStatus", false);
          this.$store.commit("saveDetectionTime", null);
          this.detection = this.$store.getters.getDetectionStatus;
          // 清除检测时间定时器
          // clearInterval(this.detectionTimeTimer);
          this.detectionTimeTimer = false;
          this.$message.success("已停止录制");

        }
      });
    }

    this.$nextTick(() => {
      // 获取列表数据
      this.getDataList();
      // this.$refs.loadingStop.show()
    })
  },
  beforeDestroy() {
    // 清除列表定时器
    this.stopListTimer();
    // 清除检测时间定时器
    // clearInterval(this.detectionTimeTimer);
    this.detectionTimeTimer = false;
    sessionStorage.setItem('notLoading', '')
  },

  methods: {
    // closeClient() {
    //   this.$emit("closeClient");
    // },

    startDetection() {
      // TODO...
      this.$nextTick(() => {
        this.$refs.allTitle.startClick()
      })
    },
    getSessionList(list = []) {
      if (list.length > 1) {
        return list.sort((a, b) => {
          let aTime = new Date(a.RecordDate || 0).getTime()
          let bTime = new Date(b.RecordDate || 0).getTime()
          return aTime - bTime
        })
      } else {
        return list
      }
    },
    getRatio(rote) {
      return (parseInt((rote || 0) * 1000) / 10).toFixed(1)
    },

    // 每天第一次打开弹窗
    dayFirstOpen() {
      this.appVnode.openDayFirst()
    },

    // 分享复盘时长充值
    rechargeTime() {
      this.kefuDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.customerServiceQrCode.init()
      })
    },

    // 跳转到设置页面
    toSetup() {
      this.$store.commit("saveSetupMenu", "customerService");
      this.$emit('updateMenuIndex', 4);
    },
    // 确定选择行业
    tradeConfirm() {
      let requestData = {
        secUid: this.currentAnchorSecUid,
        tradeId: this.selectTradeId || "1"
      }

      this.$httpClient.compere.updateanchortrade(requestData).then((res) => {
        if (res && res.code === 0) {
          this.tardeDialogVisible = false;
        }
      });

    },
    // 选择行业回调
    changeTradeHandle() {
      // 关闭级联列表下拉
      this.$refs.tradeCascader.dropDownVisible = false;
    },
    // 显示行业弹窗
    showTrade(secUid, TradeId) {
      this.currentAnchorSecUid = secUid;
      this.selectTradeId = TradeId || '1';

      if (!this.tradeTreeList || this.tradeTreeList.length < 1) {
        this.$httpBack.trade.listTree({}).then((res) => {
          if (res && res.code === 0) {
            this.tradeTreeList = res.data;
            this.tardeDialogVisible = true;
          }
        });
      } else {
        this.tardeDialogVisible = true;
      }

    },
    // 获取行业列表
    async getTradeList() {
      this.tradeLoading = true
      await this.$httpBack.trade.list({ limit: -1 }).then((res) => {
        if (res && res.code == 0) {
          this.tradeList = res.data.list;
          this.tradeList.forEach(item => {
            this.tradeMap[item.id] = item;
          })
        }
        this.tradeLoading = false
      }).finally(() => {
        this.tradeLoading = false
      })
    },
    // 清除列表定时器
    stopListTimer() {
      if (this.listTimer) {
        clearInterval(this.listTimer);
        this.listTimer = null;
      }
    },
    // 开启列表定时器
    startListTimer() {
      if (!this.listTimer) {
        clearInterval(this.listTimer);
        this.listTimer = setInterval(() => {
          if (this.detectionTimeTimer) {
            this.detectionTime = myUtils.toformatTime(Date.now() - this.startDetectionTime);
          } else {
            this.detectionTime = 0;
          }
          //持续检查点击时间锁
          this.runClickTimerLock();
          if (this.isAll && this.isAllTime > 0 && !this.detectionTimeTimer) {
            this.isAllTime -= 1;
            return
          }
          this.isAllTime = 3;
          this.getDataList();
        }, 1000);
      }
    },
    // 删除主播
    removeCompere(item) {
      if (item.RecordStatus == 1) {
        this.$message.error("主播正在录制中，请停止录制后再删除");
        return;
      }
      this.$confirm('将删除直播间, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$httpClient.compere.removeanchor({ secUid: item.SecUid }).then((res) => {
          if (res.code == 0) {
            this.$message.success("删除成功");
          }
        });
      });
    },
    // 获取基本设置信息
    getBasinSetupInfo() {
      this.$httpClient.setup.getmodel({}).then((res) => {
        if (res.code == 0) {
          this.configInfo = res.data;
          if (this.configInfo.IsRocord == 1) {
            // 正在检测中
            this.$store.commit("saveDetectionStatus", true);
            if (!this.$store.state.startDetectionTime) {
              this.$store.commit("saveDetectionTime", Date.now());
            }

          } else {
            // 没有在检测
            this.$store.commit("saveDetectionStatus", false);
            this.$store.commit("saveDetectionTime", null);

          }
          // 设置检测状态
          this.detection = this.$store.getters.getDetectionStatus;
          this.startDetectionTime = this.$store.state.startDetectionTime;
          if (this.detection && !this.detectionTimeTimer) {
            this.detectionTimeTimer = true;
          }
        }
      });
    },
    // 打开主播主页
    openToBrowser(url) {
      window.open(url, "_blank");
    },
    // 预览视频
    previewVideo(secUid, status) {
      if (status != 2) {
        this.$message.error("未开始检测或主播未开播");
        return;
      }
      this.$httpClient.compere.previewvideo({ secUid }).then((res) => { });
    },
    stopHandler(bl) {
      if (typeof bl !== 'undefined') {
        this.detection = bl;
        this.$refs.loadingStop.show()
        return
      }
      this.$refs.loadingStop.hide()
      this.detection = this.$store.getters.getDetectionStatus;
      // 清除检测时间定时器
      // clearInterval(this.detectionTimeTimer);
      this.detectionTimeTimer = false;
    },
    startHanlder(bl) {
      // 特殊情况执行特殊处理
      // if(type){
      //   this.detection = bl;
      //   return
      // }
      if (typeof bl !== 'undefined') {
        this.detection = bl;
        return
      }
      this.detection = this.$store.getters.getDetectionStatus;
      this.startDetectionTime = this.$store.state.startDetectionTime;
      // 创建检测时间定时器
      this.detectionTimeTimer = true
    },

    // 开启、关闭自动录制
    autoRecordChange(value, SecUid) {
      let requestData = {
        SecUid,
        isAuto: value ? 1 : 0
      }
      this.$httpClient.compere.openorcloseautorecord(requestData).then((res) => {
        if (res.code == 0) {
          this.$message.success("修改成功");
        }
      });
    },

    // 运行单个主播录制定时锁
    runClickTimerLock() {
      let timeKeys = Object.keys(this.itemBtnClickData);
      if (timeKeys.length <= 0) { return }
      timeKeys.forEach(key => {
        this.itemBtnClickData[key] -= 1; //减少1s时间锁
        if (this.itemBtnClickData[key] <= 0) {
          // 小于等于0删除时间锁
          delete this.itemBtnClickData[key];
        }
      })
    },
    // 设置定时锁
    clickTimerLock(secUid) {
      this.itemBtnClickData[secUid] = 30;
    },
    // 获取定时锁
    getClickTimerLock(secUid) {
      return this.itemBtnClickData[secUid] > 0
    },
    // 停止主播录制
    stopRecord(secUid) {
      if (this.getClickTimerLock(secUid)) {
        this.$message.warning("请等待倒计时结束后再操作");
        return;
      };

      this.clickTimerLock(secUid)

      this.$httpClient.compere.stoprecord({ secUid }).then((res) => {
        if (res.code == 0) {
          this.$message.success("操作成功");
        }
      });
    },



    // 开启主播录制
    startRecord(secUid) {

      if (this.getClickTimerLock(secUid)) {
        this.$message.warning("请等待倒计时结束后再操作");
        return;
      }

      // 启动限制点击定时器
      this.clickTimerLock(secUid)
      this.$httpClient.compere.startRecord({ secUid }).then((res) => {
        if (res.code == 0) {
          this.$message.success("操作成功");
        }
      });
    },
    // 打开目录
    openDirectory(secUid) {
      this.$httpClient.compere.openfolder({ secUid }).then((res) => {
      });
    },
    // 跳转到添加主播页面
    toAddCompere() {
      this.$emit('updateMenuIndex', 1);
    },
    // 查询主播列表
    keywordChange() {
      // 判断是全部主播还是录制主播
      this.getDataList();
    },
    // 获取主播列表
    getDataList() {
      let requestData = {
        pageIndex: this.pageIndex,
        pageSize: this.pageSize,
        anchorName: this.dataForm.anchorName ? this.dataForm.anchorName : "",
        recordStatus: this.isAll ? null : 1,
        isRemoveRecord: 0,
        tradeId: ''
      }
      // this.compereList = [];
      this.$httpClient.compere.getpageanchor(requestData, {
        load: false
      }).then((res) => {
        if (res.code == 0) {
          try {
            // 储存主播信息
            this.$set(this, 'compereInfo', {
              Total: res.data.Total,
              CurrentRecordNum: res.data.CurrentRecordNum,
              CurrentLiveNum: res.data.CurrentLiveNum,
            })
            // let num = 10
            // res.data.DataList.forEach((a,index)=>{
            //   a.StartTime = new Date(new Date(a.StartTime).getTime() + (num + index) * 1000).toLocaleString ()
            // })
            if (this.isAll) {
              let compereMapList = {};
              res?.data?.DataList?.forEach(item => {
                if (item.SessionList?.length > 1) {
                  item.SessionList = this.getSessionList(item.SessionList)
                }
                // item.YesterdaySessionRatio = Math.random()*2-1;
                let trade = this.tradeMap[item.TradeId];
                if (!Array.isArray(compereMapList[trade.id]?.list)) {
                  compereMapList[trade.id] = {
                    trade,
                    list: []
                  }
                }
                compereMapList[trade.id]?.list?.push(item)
              })
              this.compereMapList = compereMapList;
            } else {
              this.compereList = res.data.DataList.sort((a, b) => {
                return (new Date(b.StartTime)).getTime() - (new Date(a.StartTime)).getTime()
              });
            }
          } catch (err) {
          }
        }
      }).catch(err => {
      });
    },


    retainDecimals(val) {
      return myUtils.retainDecimals(val);
    },
  },
};
</script>
<style scoped lang="less">
.emptyTipText {
  font-weight: 400;
  font-size: 14px;
  color: #677583;
}

.emptyContainer {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 300px);
}


.compereContainer {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}


.home-tabs ::v-deep(.el-tabs__header) {
  background-color: #FFFFFF;
  padding: 20px;
  padding-bottom: 0px;
}

.home-table ::v-deep(.el-table__header-wrapper) {
  th.el-table__cell {
    background: #F8FCFF;
    color: #909499;
    font-size: 14px;
  }
}

.compere-table-rate {
  font-weight: 600;
  font-size: 14px;
  line-height: 16px;
  display: flex;
  justify-items: center;
  justify-content: center;
  align-items: center;

  >i {
    font-weight: 600;
    font-size: 15px !important;
    margin-bottom: 1px;
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
</style>

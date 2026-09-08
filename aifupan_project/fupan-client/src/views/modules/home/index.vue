<template>
  <div class="container" @contextmenu="preventRightClick">
    <!-- 左边内容 -->
    <div class="leftContainer">
      <!-- 左边顶部 -->
      <div class="leftTopContainer">
        <!-- logo -->
        <img src="@/assets/imgs/1_9_30/logo.png" class="logoImg">
        <!-- 显示用户当前版本 -->
        <div>
          <img v-if="userInfo.logoImgAddress" :src="userInfo.logoImgAddress" style="width: 59px">
        </div>
        <!-- 名字/头像 -->
        <!-- <img :src="userInfo.avatar" class="avatarImg" v-if="userInfo.avatar">
        <img src="@/assets/imgs/avatar.png" class="avatarImg" v-else>
        <div class="nameText">{{ userInfo.nickName }}</div> -->
        <!-- 菜单 -->
        <div class="menu-box">
          <div v-for="(item, index) in menuList" :key="item.label" class="menuItemContainer"
            @click="menuClick(item.index)">
            <img v-if="item.type === 'img'" :src="item.index == currentMenuIndex ? item.iconSelect : item.icon" class="img-menuImg">
            <svg v-else class="icon menuImg" aria-hidden="true">
              <use :xlink:href="`#icon-${item.index == currentMenuIndex ? item.iconSelect : item.icon}`"></use>
            </svg>
            <div class="menuText" :style="item.index == currentMenuIndex ? 'color: #00AAFF' : ''">{{ item.label }}</div>
          </div>
        </div>
      </div>
      <!-- 左边底部 -->
      <div class="leftBottomContainer">
        <div class="diskContainer">
          <div>本地磁盘空间</div>
          <div>
            <span>{{ diskInfo.driveUsedSpace + "G / " }}</span>
            <span>{{ diskInfo.driveTotalSize + "G" }}</span>
          </div>
          <div style="width: 100%;padding-top: 13px;">
            <el-progress :percentage="percentage" :text-inside="true" :show-text="false"
              :stroke-width="6"></el-progress>
          </div>
        </div>
        <div class="systemSetupContainer" @click="currentMenuIndex = 4">
          <img :src="currentMenuIndex == 4 ? systemSetupMenu.iconSelect : systemSetupMenu.icon" class="systemSetupImg">
          <div :style="currentMenuIndex == 4 ? 'color: #00AAFF' : ''">{{ systemSetupMenu.label }}</div>
        </div>
        <div style="margin-top: 8px; display: flex;flex-direction: column; align-items: center; cursor: pointer;"
          @click="showUpdateDialog">
          <div style="font-size: 12px; color: #aaa;">版本号：{{ configInfo.SerialNumber }}</div>
          <div style="color: red;font-size: 12px;" v-if="versionInfo && versionInfo.VersionNum">(可更新)</div>
        </div>
      </div>
    </div>
    <!-- 右边区域 -->
    <div class="rightContainer home-bg">
      <!-- 窗体控制栏 -->
      <div class="windowCtrlContainer">
        <img class="windowCtrlImg" src="@/assets/imgs/min.png" @click="minsize" />
        <img class="windowCtrlImg" v-if="togglemaxsizeFlag == 'normal'" src="@/assets/imgs/max.png"
          @click="togglemaxsize" />
        <img class="windowCtrlImg" v-else src="@/assets/imgs/normal.png" @click="togglemaxsize" />
        <img class="windowCtrlImg" src="@/assets/imgs/close.png" @click="close" />
      </div>
      <!-- 内容区域 -->
      <!-- 主播列表 -->
      <compere-list v-if="currentMenuIndex == 0" @updateMenuIndex="updateMenuIndex" :key="compereListKey"></compere-list>
      <!-- 添加主播 -->
      <compere-add v-if="currentMenuIndex == 1" @updateMenuIndex="updateMenuIndex"></compere-add>
      <!-- 智能复盘 -->
      <replay v-if="currentMenuIndex == 2" :key="refresh" @showUploadPop="showUploadPop"></replay>
      <!-- 系统设置 -->
      <setup v-if="currentMenuIndex == 4" @updateUserInfo="updateUserInfo" @updateMenuIndex="updateMenuIndex"></setup>
      <!-- 本地词库 -->
      <lexicon v-if="currentMenuIndex == 5"></lexicon>
      <!-- 版本续费/升级 -->
      <renewal v-if="currentMenuIndex == 6" @updateMenuIndex="updateMenuIndex"></renewal>
      <!-- 播前分析 -->
      <file-upload v-if="currentMenuIndex == 7" @updateMenuIndex="updateMenuIndex"></file-upload>
      <!-- 云空间 -->
      <online v-if="currentMenuIndex == 8"></online>

    </div>

    <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>
    <update-version v-if="updateVersionVisible" ref="updateVersion"></update-version>

    <!-- 分享复盘弹窗 -->
    <!-- <online-analysis-dialog v-if="onlineAnalysisDialogVisible" ref="onlineAnalysisDialog"></online-analysis-dialog> -->




  </div>
</template>

<script>
import myUtils from '../../../utils/utils';
import compereList from '../compere/compere-list.vue';
import compereAdd from '../compere/compere-add.vue'
import setup from '../setup/index.vue';
import replay from '../replay/replay.vue';
import lexicon from '../lexicon/lexicon.vue'
import customerServiceQrCode from '../../commonComponent/customerServiceQrCode.vue';
import renewal from '../renewal/renewal.vue';
import UpdateVersion from '../../commonComponent/updateVersion.vue';
import FileUpload from "../replay/fileUpload/index.vue";
import online from "../replay/online/index.vue"
// import OnlineAnalysisDialog from '../../commonComponent/onlineAnalysisDialog.vue';

export default {
  components: {
    compereList,
    setup,
    compereAdd,
    replay,
    lexicon,
    customerServiceQrCode,
    renewal,
    UpdateVersion,
    FileUpload,
    online,
    // OnlineAnalysisDialog,
  },
  data() {
    return {
      onlineAnalysisDialogVisible: false,
      updateVersionVisible: false,
      compereListKey: 0,
      // 点击充值打开客服弹窗
      kefuDialogVisible: false,

      // 智能复盘时长
      fupanTime: 216,
      fupanTimeAll: 2000,

      userInfo: {},
      togglemaxsizeFlag: 'normal',
      currentMenuIndex: 0,
      menuList: [
        {
          index: 0,
          label: "数据大盘",
          // icon: require("@/assets/imgs/zhubo_list.png"),
          // iconSelect: require("@/assets/imgs/zhubo_list_select.png"),
          icon: 'daohangtubiao',
          iconSelect: 'daohangtubiao-1'
        },
        {
          index: 1,
          label: "添加主播",
          // icon: require("@/assets/imgs/zhubo_add.png"),
          // iconSelect: require("@/assets/imgs/zhubo_add_select.png"),
          icon: 'a-Property1Default-4',
          iconSelect: 'a-Property1Default-5'
        },
        {
          index: 7,
          label: "播前分析",
          // icon: require("@/assets/imgs/replay_local.png"),
          // iconSelect: require("@/assets/imgs/replay_local_select.png"),
          icon: 'a-122',
          iconSelect: 'a-Property1Default-3'
        },
        {
          index: 2,
          label: "智能复盘",
          // icon: require("@/assets/imgs/zhibo_replay.png"),
          // iconSelect: require("@/assets/imgs/zhibo_replay_select.png"),
          icon: 'a-Property1Default-6',
          iconSelect: 'a-11111111'
        },
        {
          index: 8,
          label: "云空间",
          // icon: require("@/assets/imgs/replay_online.png"),
          // iconSelect: require("@/assets/imgs/replay_online_select.png"),
          icon: 'zaixianfupan',
          iconSelect: 'zaixianfupan-1'
        },
        {
          index: 5,
          label: "本地词库",
          // icon: require("@/assets/imgs/bendi_ciku.png"),
          // iconSelect: require("@/assets/imgs/bendi_ciku_select.png"),
          icon: 'a-Property1Default-1',
          iconSelect: 'a-Property1Default-2'
        },
        {
          index: 999,
          label: '帮助中心',
          icon: 'bangzhuzhongxinoff',
          iconSelect:'as',
        }
      ],
      systemSetupMenu: {
        label: "系统设置",
        icon: require("@/assets/imgs/system_setup.png"),
        iconSelect: require("@/assets/imgs/system_setup_select.png"),
      },
      diskInfo: {
        driveTotalSize: 0,
        driveUsedSpace: 0,
        driveAvailablepace: 0
      },
      refresh: false,
      diskTimer: null,
      userTimer: null,
      configInfo: {},
      userProperty: {
        monitorNum: 0,
        aiAnalysisTime: 0,
        anchorNum: 0,
        totalMonitorNum: 0,
        totalAiAnalysisTime: 0,
        totalAnchorNum: 0,
        videoTaggingTime: 0,
        totalVideoTaggingTime: 0,
        textTaggingWordCount: 0,
        totalTextTaggingWordCount: 0,
        storageNum: 0,
        totalStorageNum: 0
      },
      versionInfo: {
        VersionNum: "",
      },
      
    }
  },
  created() {

    this.getUserInfo();
    this.getVersionUpdate();

    // 禁止用户按F5和F12
    // window.addEventListener("keydown", (event) => {
    //   let code = event.keyCode || event.which;
    //   if (event.key === 'F12' || event.key === 'F5' || (event.ctrlKey && (code === 82))) {
    //     event.preventDefault()
    //   }
    // });

    let homeMenu = this.$store.state.homeMenu;
    this.userInfo = this.$store.state.userInfo;

    if (homeMenu) {
      this.currentMenuIndex = homeMenu;
      this.$store.commit("saveHomeMenu", null);
    }
   
    // 开启读盘空间定时器
    if (this.diskTimer) {
      clearInterval(this.diskTimer)
    }
    this.diskTimer = null;
    this.startTimer();
    // 开启读用户信息定时器
    if (this.userTimer) {
      clearInterval(this.userTimer)
    }
    this.userTimer = null;
    this.startUserTimer();

    this.getUserproperty();
  },
  beforeDestroy() {
    // 清空定时器
    if (this.diskTimer) {
      clearInterval(this.diskTimer)
    }
    this.diskTimer = null;
    if (this.userTimer) {
      clearInterval(this.userTimer)
    }
    this.userTimer = null;
  },
  computed: {
    percentage() {

      return (parseInt(parseFloat(this.diskInfo.driveUsedSpace / this.diskInfo.driveTotalSize) * 10000) / 100) || 0;
    }
  },
  inject: ['appVnode'],
  methods: {
    close(){
      // 调用关闭检查
      this.appVnode.close()
    },
    showUploadPop(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo) {
      this.appVnode.showUploadPop(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo)
      // this.onlineAnalysisDialogVisible = true;
      // this.$nextTick(() => {
      //   this.$refs.onlineAnalysisDialog.init(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo);
      // })
      // this.$emit('showUpload',{
      //   userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo
      // })
    },
    // 获取版本更新信息
    getVersionUpdate() {
      this.$httpClient.setup.getVersionUpdate({}).then(res => {
        this.versionInfo = res.data;
        if (this.versionInfo && this.versionInfo.VersionNum) {
          if (!localStorage.getItem(this.versionInfo.VersionNum)) {
            this.showUpdateDialog();
          }
        }
      });
    },
    // 显示更新代码弹窗
    showUpdateDialog() {
      if (this.versionInfo && this.versionInfo.VersionNum) {
        this.updateVersionVisible = true;
        this.$nextTick(() => {
          this.$refs['updateVersion'].init();
        })
      }
    },
    // 获取用户信息
    getUserInfo() {
      this.$httpBack.user.infoByClient({}).then(res => {
        if (res.code == 0 && res.data) {
          this.userInfo = res.data;
          this.$store.commit("saveUserInfo", res.data);
        }
      });
    },
    getUserproperty() {
      this.$httpBack.userProperty.info().then(res => {
        if (res.code == 0 && res.data) {
          this.$store.commit("saveUserproperty", res.data);
        }
      });
    },
    retainDecimals(val) {
      return myUtils.retainDecimals(val);
    },
    // 打开客服弹窗
    rechargeTime() {
      this.kefuDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.customerServiceQrCode.init()
      })
    },

    menuClick(menuIndex) {
      if(menuIndex === 999){
        window.open('https://ucnus90885lw.feishu.cn/wiki/EvgqwyoL5ikDjAksEmvccFVbnDg?from=from_copylink', "_blank");
        return
      }
      this.currentMenuIndex = menuIndex;
      this.$nextTick(() => {
        if (menuIndex == 2) {
          this.refresh = !this.refresh;
        }
      })
    },
    // 获取基本设置信息
    getBasinSetupInfo() {
      this.$httpClient.setup.getmodel({}).then((res) => {
        if (res.code == 0) {
          this.configInfo = res.data;
        }
      });
    },
    // 读盘空间
    getDisk() {
      this.$httpClient.setup.getdisksize({}).then(res => {
        if (res.code == 0 && res.data) {
          this.diskInfo = res.data;
          if (this.diskInfo.driveAvailablepace < 10) {
            // 磁盘空间不足10G
            if (this.configInfo && this.configInfo.IsRocord == 1) {
              // 当前正在录制
              this.$message({
                showClose: true,
                message: '磁盘可用空间已不足10G，为保证系统正常运行，将停止录制',
                type: 'error',
                duration: 0
              });
              // 去列表页停止检测
              this.$store.commit("saveStopRecord", true);
              if (this.currentMenuIndex == 0) {
                this.compereListKey++;
              } else {
                this.currentMenuIndex = 0;
              }


              // // 当前正在录制
              // this.$confirm('磁盘可用空间已不足10G，是否停止录制？', '提示', {
              //   confirmButtonText: '确定',
              //   cancelButtonText: '取消',
              //   type: 'warning'
              // }).then(() => {
              //   // 去列表页停止检测
              //   this.$store.commit("saveStopRecord", true);
              //   this.currentMenuIndex = 0;
              // })
            }
          }
        }
      })
    },
    // 开启读盘空间定时器
    startTimer() {
      // this.getUserProperty();
      this.getBasinSetupInfo();
      this.getDisk();
      this.diskTimer = setInterval(() => {
        this.getBasinSetupInfo();
        this.getDisk();
        // this.getUserProperty();
      }, 30 * 1000);
    },
    // 开启用户信息定时器，如果被冻结，跳转到登录页
    startUserTimer() {
      this.userTimer = setInterval(() => {
        this.$httpClient.user.info({}).then((res) => {
          if (!res.data) {
            // 没有获取到用户信息，跳回登录页
            this.$message.error("登录失效");
            this.$router.push({ path: "login" });
          } else if (res.data.Status == 1) {
            // 账号已被冻结，跳回登录页
            this.$message.error("账号已被冻结，将停止录制和分析。");
            this.$router.push({ path: "login" });
          }
        });

        // 读取版本更新信息
        this.getVersionUpdate();
      }, 10000);
    },
    // 禁止鼠标右键
    preventRightClick(event) {
      event.preventDefault();
    },
    // 更新用户信息
    updateUserInfo() {
      this.userInfo = this.$store.state.userInfo;
    },
    // 获取用户资产信息
    getUserProperty() {
      this.$httpBack.userProperty.info({}).then((res) => {
        if (res && res.code === 0 && res.data) {
          this.userProperty = res.data;
        }
      });
    },
    // 更新菜单索引
    updateMenuIndex(menuIndex) {
      this.currentMenuIndex = menuIndex;
    },
    // 最大化/恢复正常
    togglemaxsize() {
      this.$httpClient.form.togglemaxsize().then((res) => {
        if (res.code == 0) {
          if (this.togglemaxsizeFlag == 'normal') {
            this.togglemaxsizeFlag = 'max';
          } else {
            this.togglemaxsizeFlag = 'normal';
          }
        }
      })
    },
    // 最小化
    minsize() {
      this.$httpClient.form.minsize().then((res) => {
      })
    },
  },
};
</script>

<style scoped>
.cancelButtonClass {
  color: red;
}

.windowCtrlImg {
  width: 16px;
  height: 16px;
  margin-left: 21px;
  cursor: pointer;
}

.windowCtrlContainer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 10px 20px 0 0;
}

.rightContainer {
  /* padding: 18px; */
  box-sizing: border-box;
  width: 0;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.systemSetupContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
  color: #2E3742;
  margin-top: 16px;
  cursor: pointer;
}

.systemSetupImg {
  width: 28px;
  height: 28px;
}

.diskContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
  color: #2E3742;
}

.leftBottomContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}

.menuText {
  font-size: 12px !important;
  color: #2E3742;
}
.img-menuImg{
  width: 20px;
  margin-right: 8px;
}
.menuImg {
  /* width: 24px;
  height: 24px; */
  font-size: 20px;
  margin-right: 8px;
}

.menu-box {
  flex-direction: initial;
}

.menuItemContainer {
  display: flex;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  margin: 14px 0;
  padding: 14px 0;
  cursor: pointer;
}

.nameText {
  font-size: 14px;
  color: #2E3742;
  margin-top: 4px;
  cursor: pointer;
  margin-bottom: 20px;
}

.avatarImg {
  width: 40px;
  height: 40px;
  margin-top: 20px;
  border-radius: 50%;
  cursor: pointer;
}

.logoImg {
  max-width: 95%;
  /* height: 56px; */
  margin-top: 24px;
}

.leftTopContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.leftContainer {
  width: 120px;
  height: 100%;
  background: #F4F9FF;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
}

.container {
  display: flex;
  border: 1px solid #ccc;
  height: calc(100vh - 2px);
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}
</style>
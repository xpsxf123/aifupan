<template>
  <div>
    <!-- 开发版代码 -->
    <div v-if="isDev && !isClick3Hiden" @mouseenter="onMouseenter" @mouseleave="onMouseleave" class="flex-url mg-t30" :class="{'hide-url':!showUrl}" >
      <div class="flex-jc-sb">
        <el-button type="text" @click="showUrl = !showUrl">{{ showUrl?'收起功能栏':'展开功能栏' }}</el-button>
        <el-button type="text" @click="clickHiden_3_forever"><span class="text-colorErr">永久隐藏功能栏</span></el-button>
      </div>
      <div v-if="showUrl" class="w100">
        <div class="flex-jc-sb">
          <span>url：</span>
          <el-input
          type="textarea"
          placeholder="请输入内容"
          v-model="textarea"
          show-word-limit
          :autosize="{minRows: 2, maxRows: 2 }"
          @blur="onToUrl"
        ></el-input>
        </div>
        <div class="pd-t6 pd-l6">
          <afp-button type="primary" @click="$router.back()">上一页</afp-button>
          <afp-button type="primary" @click="$router.push({path: '/dataAnalysis'})">回到首页</afp-button>
          <afp-button type="primary" @click="$httpClient.test.tesOpenUpdateVersion()">强制跟新</afp-button>
          <afp-button type="primary" @click="F12Click">F12</afp-button>
        </div>
      </div>
    </div>
    <!-- 页面主体 -->
    <router-view />
    <!-- 客服二维码 -->
    <customer-service-qr-code ref="customerServiceQrCode"></customer-service-qr-code>
    <!-- 强制更新 -->
    <forcedUpdate ref="forcedUpdate"></forcedUpdate>
    <!-- 生成AI报告 -->
      <DialogAiContent
          ref="dialog_ai_report"
          :tabsList="tabsList"
          :isReport="isReport"
          :uploadType="uploadType"
          :selectedRow="selectedRow"
          :hideReportModal="hideReportModal"
          :sponsorship="sponsorship"/>
    <!-- 巨量百应 -->
    <buyIn ref="buy_in"/>
    <BuyInAccountType ref="buyIn_account"/>

  </div>
</template>


<script>
import env from '/src/env';
import customerServiceQrCode from '@/views/commonComponent/customerServiceQrCode.vue';
import forcedUpdate from '@/components/forcedUpdate/index.vue';
import commonUtils from '@/utils/common.js'
import aiReport from '@/mixins/aiContentReport'
import DialogAiContent from "@/views/commonComponent/aiReport/dialogAIContent.vue";
import clickHiden from '@/mixins/clickHiden'
import buyIn from '@/components/buyIn/index.vue';
import BuyInAccountType from '@/components/buyIn/components/buyInAccountType.vue';
import {Notification} from "element-ui";
import {VERSION_TYPE} from "@/enum";
export default {
  components: {
    buyIn,
    DialogAiContent,
    customerServiceQrCode,
    forcedUpdate,
    BuyInAccountType
  },
  provide(){
    return {
        APP: this
    }
  },
  // mixins: [onlineContrastAnalysisDialogMixin],
  mixins: [aiReport, clickHiden('app-dev-content')],
  data() {
    return {
      showUrl: false,
      textarea: '',
      isRefresh: false
    }
  },
  computed: {
    isDev(){
      return env.dev || env.test;
    }
  },
  watch: {
      '$route.fullPath'(val){
          this.$set(this,'textarea',val);
      }
    // '$store.getters.getUserInfo.id':{
    //   handler(val){
    //     // 获取用户账号权限数据。
    //     const {id, activeTenantId, normalPhone, username} = this.$store.getters.getUserInfo;
    //     // 加入权限数据
    //     this.$addAuthCode([id, activeTenantId, normalPhone, username, `${id}_0`, `${activeTenantId}_2`]);
    //   },
    //   immediate: true
    // }
  },
  methods: {
      watchAuthIds() {
          this.$watch(() => [this.$store.getters.getUserInfo.id, this.$store.getters.getUserInfo.activeTenantId], (newVal) => {
                  this.$removeAuthIds()
                  // 获取用户账号权限数据。
                  const {id, activeTenantId, normalPhone, username} = this.$store.getters.getUserInfo;
                  // 加入权限数据
                  this.$addAuthCode([id, activeTenantId, normalPhone, username, `${id}_0`, `${activeTenantId}_2`]);
              },
              {deep: true, immediate: true}
          )
      },
    onToUrl(){
      if(this.textarea.indexOf('http')>=0 || this.textarea.indexOf('https')>=0){
        location.href = this.textarea;
        return;
      }
      this.$router.push({ path:  this.textarea})
    },
    onMouseenter(){
        // 放开ctry+c的限制，c表示按键c，99 表示是否按下ctry健
        window.notKeyDown.c = 99;
    },
    onMouseleave(){
        window.notKeyDown.c = 0;
    },
    showQrCode(){
      this.$refs.customerServiceQrCode.init();
    },
    refresh(){
      // 只会在客户端执行视频刷新。web段不会执行页面刷新
      if(!sessionStorage.getItem('isRefresh') && !this.$isWeb){
        sessionStorage.setItem('isRefresh','1');
        this.isRefresh = true;
        setTimeout(()=>{
          window.location.reload();
        },0)
      }
    },
    removeRefresh(){
      if(sessionStorage.getItem('isRefresh')){
        this.isRefresh = true;
        this.$nextTick(()=>{
          sessionStorage.removeItem('isRefresh');
          this.isRefresh = false;
        })
      }
    },
    getMode() {
        if(this.$isWeb){return}
        this.$httpClient.setup.getClientMode({}).then(res => {
          this.$store.commit('setMode',res.data)
        })
    },
    toIfupanWebsite(){
      window.open('https://www.ifupan.com/', '_blank');
    },
    // 复制分享链接
    copyShareUrl(shareUrl) {
      commonUtils.copyShareUrl(shareUrl, this.$message.success);
    },
    addKeyDown(){
      let hNum = 0;
      let timeOut = null;
      document.addEventListener('keydown',(event)=>{
        if(this.$store.getters.getMode === 0){
          if(event.key === 'h'){
            console.log(event.key);
            hNum++
            if(hNum === 5){
              this.$router.push({
                path:'/dataAnalysis'
              })
            }
            clearTimeout(timeOut);
            timeOut = setTimeout(()=>{
              hNum = 0
            },1000)
          }else{
            hNum = 0
          }
        }
      });
    },
    watchForcedUpdate(){
      this.$CSharpNotify.addTask('openUpdate',(res,resolve)=>{
        this.$nextTick(()=>{
          this.$refs.forcedUpdate.show({data:res});
        })
      });
    },
    F12Click(){
      this.$httpClient.setup.openDevelopmentMode({}).then(res => {
        this.$httpClient.setup.openDevTools({})
      })
    },
    computedIdsObj(res){
        return new Promise((resolve, reject) => {
            this.$nextTick(async () => {
                this.getListDiagnosis().then(list=>{
                    const selectedCueWordsList = {};
                    list.forEach((group) => {
                        let name = group.name;
                        selectedCueWordsList[name] = []; // 初始化为数组
                        group.cueWordsList.forEach((word) => {
                            if (res?.cueWordsIds?.includes(word.cueWordsId)) {
                                selectedCueWordsList[name].push(word.cueWordsId);
                            }
                        });
                    });
                    resolve(selectedCueWordsList);
                })
            })
        })
    },
      async aiReportFun() {
          this.$CSharpNotify.addTask('generateDiagnosis', async (res, resolve) => {
              this.selectedRow = {
                  videoId: res?.videoId,
                  reportFileName: res?.diagnosisOssName?.replace(/_[^_]+\.ts$/, ''),
              }
              if (res?.diagnosisType === 1) {//数据诊断
                  const list = [{
                      tagName: "AI数据诊断",
                      name: 0,
                      ids: res.cueWordsIds,
                      cueWordsList: [{
                          cueWordsId: res.cueWordsIds,
                          cueWord: "",
                      }]
                  }]
                  this.uploadType = 2
                  this.hideReportModal(true)
                  this.$nextTick(() => {
                      this.$refs.dialog_ai_report?.changeDialogVisible(list, {0: res.cueWordsIds}, true, 1)
                  })
              } else {//内容诊断
                  const selectedCueWordsList = await this.computedIdsObj()
                  this.hideReportModal(true)
                  this.uploadType = 1
                  this.$nextTick(() => {
                      this.$refs.dialog_ai_report?.changeDialogVisible(this.tabsList, selectedCueWordsList, true, 1)
                  })
              }
          });
      },
      buyInAccount(secUid){
          this.$refs.buyIn_account?.open(secUid)
      },
      sliceSuccessAction() {
          this.$CSharpNotify.addTask('sliceSuccess', async (res, resolve) => {
              Notification({
                  title: '友情提示',
                  type: 'success',
                  dangerouslyUseHTMLString: true,
                  duration: 60000,
                  message: `<div>${res.sliceName}切片已生成，<span style="color: var(--color-main); cursor: pointer">点我查看</span></div>`,
                  onClick: () => {
                      const {fileId, videoId, sliceType} = res;
                      const map = {
                          0: {
                              file: '/uploadSlice/fileUploadAnalysis',
                              noFile: '/section/analysis'
                          },
                          1: {
                              file: '/uploadShort/fileUploadAnalysis',
                              noFile: '/short/analysis'
                          }
                      };
                      const path = fileId ? map[sliceType].file : map[sliceType].noFile;
                      this.$router.replace({
                          path: path,
                          query: {id: fileId || videoId}
                      })
                  }
              });
          });
      },
      buyIn(list){
        this.$refs.buy_in?.open(list)
      }
  },
  async mounted() {
      if(this.$route?.meta?.notAPPinit){return}
      this.getMode();
      this.watchAuthIds()
      this.removeRefresh();
      this.addKeyDown();
      this.watchForcedUpdate();
      this.aiReportFun();
      this.sliceSuccessAction()
      window.addEventListener('dragover', function (e) {
          e.preventDefault(); // 阻止默认行为（比如打开文件）
      }, false);
      window.addEventListener('drop', function (e) {
          e.preventDefault(); // 阻止默认行为
      }, false);
  }
}
</script>

<style lang="scss">
.flex-url{
  position: fixed;
  right: 0;
  top: 0;
  background: #fff;
  z-index: 99999999999;
  width: 400px;
  background: rgba(#fff, 1);
  border: 1px solid #000;
  overflow-y: auto;
  .el-textarea{
    height: 100%;
  }
}
.hide-url{
  right: -320px;
}


// @font-face {
//   font-family: 'Alibaba-PuHuiTi-R';
//   src: url('./assets/fonts/Alibaba-PuHuiTi-R.woff2') format('woff2');
//   font-weight: normal;
//   font-style: normal;
// }

html,
body {
  padding: 0;
  margin: 0;
  // font-family: Alibaba-PuHuiTi-R;
}

* {
  box-sizing: border-box;
}

ul,
li {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebarFold {
  width: 65px !important;
}

.sidebarOpen {
  width: 230px !important;
}

.icon-popper {
  width: 450px;
  overflow: hidden;
}

.toolbar {
  margin-bottom: 10px;
}

.el-menu--popup {
  .is-active {
    background-color: rgba(17, 187, 141, 0.8);
  }

  .is-active:hover {
    background-color: rgba(17, 187, 141, 0.8);
  }
}

</style>

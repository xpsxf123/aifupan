<template>
  <el-dialog :visible.sync="dialogVisible" width="300px" top="50px" :close-on-click-modal="false" :show-close="false"
    style="padding: 0">
    <div slot="title"></div>

    <div class="updateVersionContainer">
      <img src="@/assets/imgs/version_update.png" class="updateVersionImg">
      <div class="updateContentContainer">
        <div class="updateContentTitle">
          {{ "发现新版本 V" + versionInfo.VersionNum }}
        </div>
        <div class="updateContentBody">
          <div v-html="versionInfo.UpdateInfo"></div>
        </div>
      </div>

      <afp-button class="updateBtn" type="primary" @click="submit">立即更新</afp-button>

      <img src="@/assets/imgs/version_update_close.png" class="updateVersionCloseImg" @click="dialogVisible = false">

    </div>
  </el-dialog>
</template>
<script>
export default {

  data() {
    return {
      dialogVisible: false,
      versionInfo: {
        VersionNum: "",
        UpdateInfo: ""
      },
    };
  },

  mounted() {

  },

  methods: {
    init() {
      this.getVersionUpdate();
      this.dialogVisible = true;
    },
    // 获取版本更新信息
    getVersionUpdate() {
      this.$httpClient.setup.getVersionUpdate({}).then(res => {
        this.versionInfo = res.data;
        localStorage.setItem(this.versionInfo.VersionNum, this.versionInfo.VersionNum);
      })
    },
    submit() {
      this.$httpClient.setup.getmodel({}).then((res) => {
        if (res.data.IsRocord == 1) {
          this.$confirm('目前正在录制中，将停止录制进行更新，是否继续？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            this.$httpClient.compere.stopdecector().then((res) => {
              // 更新
              setTimeout(()=>{
                this.$httpBack.user.logout({}).then(res => {
                this.$httpClient.setup.updateProgram({});
                this.dialogVisible = false;
              });
              },8000)
            });

          });
        } else {
          // 更新
          let path = this.$route.path
          if (path === '/' || path === '/login'){
            this.$httpClient.setup.updateProgram({});
            this.dialogVisible = false;
          }else {
            this.$httpBack.user.logout({}).then(res => {
              this.$httpClient.setup.updateProgram({});
              this.dialogVisible = false;
            });
          }
        }
      });
    },
  },
};
</script>

<style scoped lang="less">
.updateBtn {
  position: absolute;
  margin-top: 500px;
  width: 250px;
}

.updateContentBody {
  font-size: 14px;
  color: #4D4D4D;
  display: flex;
  flex-direction: column;
  margin-top: 8px;
  margin-left: 6px;
}

.updateContentTitle {
  font-weight: 700;
  font-size: 16px;
  color: #151917;
}

.updateContentContainer {
  position: absolute;
  margin-top: 340px;
}

.updateVersionCloseImg {
  width: 40px;
  height: 40px;
  cursor: pointer;
  margin-top: 30px;
}

.updateVersionImg {
  width: 387px;
  height: 576px;
}

.updateVersionContainer {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/deep/ .el-dialog {
  background: transparent;
  box-shadow: none;
}

/deep/ .el-dialog__header {
  display: none;
}

/deep/ .el-dialog__body {
  padding: 0 0 10px 0;
}
</style>
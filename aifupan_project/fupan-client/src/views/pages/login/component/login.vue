<template>
  <!-- 登录 -->
  <div class="bodyContainer">
    <!-- 登录界面宣传语 -->
    <div class="logintext">
      <div>
        <span>数据只是</span><span style="color: #5E81F4;">结果</span>
      </div>
      <div style="margin-top: 8px;">
        <span>内容才是</span><span style="color: #5E81F4;margin-right:14px;">核心</span>
        <span>复盘解决</span><span style="color: #5E81F4;">根本</span>
      </div>
    </div>
    <!-- 登录类型 -->
    <div class="loginTypeContainer pd-b14">
      <div :class="loginType == 'password' ? 'loginTypeItemSelect' : 'loginTypeItem'"
        @click="loginTypeChange('password')">账号密码登录</div>
      <div :class="loginType == 'code' ? 'loginTypeItemSelect' : 'loginTypeItem'" @click="loginTypeChange('code')">
        验证码登录</div>
    </div>
    <!-- 账号密码登录 -->
    <div class="formContainer" v-if="loginType == 'password'">
      <el-form :model="dataForm" :rules="ruleConfig" ref="dataForm" size="default">
        <el-form-item prop="username">
          <el-input v-model="dataForm.username" placeholder="手机号码"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input  v-model="dataForm.password" placeholder="密码" show-password></el-input>
        </el-form-item>
        <afp-button  type="primary" size="default" :plain="false" style="width: 100%;" @click="login">登录</afp-button>
        <div class="flex-jc-sb mg-t14  flex-ai-c">
          <el-checkbox v-model="rememberPwd">记住账号密码</el-checkbox>
          <el-button type="text" @click="toRegister">立即注册</el-button>
          <!-- <div class="registerText" @click="toRegister">立即注册</div> -->
        </div>
      </el-form>
    </div>
    <!-- 验证码登录 -->
    <div class="formContainer" v-if="loginType == 'code'">
      <el-form :model="dataForm" :rules="ruleConfig" ref="dataForm" size="default">
        <el-form-item prop="phone">
          <el-input  v-model="dataForm.phone" placeholder="手机号码"></el-input>
        </el-form-item>
        <el-form-item prop="code">
          <el-input style="width: 165px;" v-model="dataForm.code" placeholder="验证码"></el-input>
          <GetCode size="medium" width="100px" :phone="dataForm.phone"></GetCode>
          <!-- <el-input style="width: 165px;" v-model="dataForm.code" placeholder="验证码"></el-input>
          <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
          <span class="getCodeText" v-else style="cursor:not-allowed;">{{ this.getCodeCountdown }}s</span> -->
        </el-form-item>
        <afp-button  type="primary" size="default" :plain="false" style="width: 100%;" @click="login">登录</afp-button>
        <!-- <div class="loginBtn" @click="login">
          登录
        </div> -->
        <div class="flex-jc-sb mg-t14 flex-ai-c">
          <div></div>
          <el-button @click="toRegister" type="text">立即注册</el-button>
          <!-- <div class="registerText" @click="toRegister">立即注册</div> -->
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import loginMixin from './mixin'
export default {
  components: {},
  props: {

  },
  mixins: [loginMixin],
  data() {
    return {
      rememberPwd: false,
      loginType: 'password'
    };
  },
  computed: {},
  watch: {},
  methods: {
    toRegister() {
      this.$emit('loginOrRegisterChange', 'register');
    },
    // 登录
    login() {
      this.$refs["dataForm"].validate((valid) => {
        if (valid) {
          if (this.rememberPwd) {
            localStorage.setItem("username", this.dataForm.username);
            localStorage.setItem("password", this.dataForm.password);
          } else {
            localStorage.setItem("username", "");
            localStorage.setItem("password", "");
          }
          
          this.$httpBack.onlineUser.login(this.dataForm).then(res => {
            if (res.code == 0) {
              this.$message.success(res.msg);
              this.$store.commit("saveLoginResultData", res.data);
              this.$emit('changeType','read')
            } else {
              this.$message.error(res.msg);
            }
          });
          //   this.$httpBack.user.login(this.dataForm).then(res => {
          //     if (res.code == 0) {
          //       this.$message.success(res.msg);
          //       this.$store.commit("saveLoginResultData", res.data);
          //       this.$httpClient.setup.settoken({ token: res.data.token, userId: res.data.id }).then((res) => {
          //         if (res.code == 0) {
          //           this.$router.push({ path: "dataAnalysis",query: {
          //             login: '1'
          //           } });
          //         }
          //       })
          //       // 每次登录成功将该值设为ture，打开每天首次弹窗的组件
          //       localStorage.setItem("currentPrompt",String(true))
          //     } else {
          //       this.$message.error(res.msg);
          //     }
          //   });
        }
      });
    },
  },
  created() {

  },
  mounted() {
    this.getStorageData();
  },
  beforeCreate() { }, //生命周期 - 创建之前
  beforeMount() { }, //生命周期 - 挂载之前
  beforeUpdate() { }, //生命周期 - 更新之前
  updated() { }, //生命周期 - 更新之后
  beforeDestroy() { }, //生命周期 - 销毁之前
  destroyed() { }, //生命周期 - 销毁完成
  activated() { 
      this.getStorageData();
   }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.logintext {
  display: flex;
  flex-direction: column;
  font-size: 22px;
  color: #4D4D4D;
  margin-bottom: 40px;
}
.loginTypeContainer {
  width: 222px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.loginTypeItemSelect {
  font-weight: 500;
  font-size: 16px;
  color: rgb(0, 119, 255);
  padding: 7px 0;
  height: 36px;
  border-bottom: 3px solid rgb(0, 119, 255);
  ;
  box-sizing: border-box;
  cursor: pointer;
}

.loginTypeItem {
  font-weight: 500;
  font-size: 16px;
  height: 36px;
  color: #2E3742;
  padding: 7px 0;
  box-sizing: border-box;
  cursor: pointer;
}
</style>
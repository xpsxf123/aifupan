<template>
  <div class="bodyContainer">
    <div>
      <div class="registerTitle">
        欢迎注册爱复盘
      </div>
      <div class="registerSubTitleContainer">
        <span>已有账号？</span>
        <span style="color: var(--color-main); cursor: pointer;" @click="toLogin">登录</span>
      </div>
      <el-form :model="dataForm" :rules="ruleConfig" ref="dataForm" style="margin-top: 24px;">
        <el-form-item prop="nickName">
          <el-input  v-model="dataForm.nickName" placeholder="昵称"></el-input>
        </el-form-item>
        <el-form-item prop="phone">
          <el-input  v-model="dataForm.phone" placeholder="手机号"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input  v-model="dataForm.password" placeholder="密码" show-password></el-input>
        </el-form-item>
        <el-form-item prop="invitationCode">
          <el-input  v-model="dataForm.invitationCode" placeholder="邀请码（非必填）"></el-input>
        </el-form-item>
        <el-form-item prop="code">
          <el-input style="width: 165px;" v-model="dataForm.code" placeholder="验证码"></el-input>
          <GetCode size="medium" width="100px" :phone="dataForm.phone"></GetCode>
          <!-- <el-input style="width: 165px;" v-model="dataForm.code" placeholder="验证码"></el-input>
          <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
          <span class="getCodeText" v-else style="cursor:not-allowed;">{{ this.getCodeCountdown }}s</span> -->
        </el-form-item>
        <afp-button  type="primary" style="width: 100%;" @click="register">注册</afp-button>
        <!-- <div class="loginBtn" @click="register">
          注册
        </div> -->
      </el-form>
    </div>
  </div>
</template>

<script>
import loginMixin from './mixin';
import getPlatform from '@/utils/platformSource';
export default {
  components: {},
  props: {

  },
  mixins: [loginMixin],
  data() {
    return {
      inviteUrlCode: ''
    };
  },
  computed: {
    getInviteUrlCode(){
      // 防止删除掉code，导致注册失败。
      return this.$route.query.code || this.inviteUrlCode
    }
  },
  watch: {},
  methods: {
    toLogin() {
      this.$emit('loginOrRegisterChange', 'login')
    },
    // 注册
    register() {
      this.$refs["dataForm"].validate((valid) => {
        if (valid) {
          this.dataForm.inviteUrlCode = this.getInviteUrlCode || getPlatform({isCode: true, forcePlatform:'cloudSpace'});
          this.$httpBack.user.register(this.dataForm).then(res => {
            if (res.code == 0) {
              this.dataForm.username = this.dataForm.phone
              this.$message.success(res.msg);
              this.toLogin();
            } else {
              this.$message.error(res.msg);
            }
          });
        }
      });
    },
  },
  created() {

  },
  mounted() {
    this.inviteUrlCode = this.getInviteUrlCode;
  },
  beforeCreate() { }, //生命周期 - 创建之前
  beforeMount() { }, //生命周期 - 挂载之前
  beforeUpdate() { }, //生命周期 - 更新之前
  updated() { }, //生命周期 - 更新之后
  beforeDestroy() { }, //生命周期 - 销毁之前
  destroyed() { }, //生命周期 - 销毁完成
  activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.registerTitle {
  font-weight: 600;
  font-size: 20px;
  color: #2E3742;
}

</style>
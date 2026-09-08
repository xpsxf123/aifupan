<template>
  <div class="msgPushSetupContainer">
    <el-form :model="dataForm" :rules="dataRule" ref="dataForm" label-width="120px" label-position="right">
      <!-- 设置是否推送消息到微信 -->
      <div class="pushWechatContainer">
        <div>推送消息到微信</div>
        <el-switch v-model="dataForm.startPush" active-color="#13ce66" inactive-color="#DCE0E7"
          style="margin-left: 12px;">
        </el-switch>
      </div>
      <!-- 推送事件 -->
      <div>
        <div class="pushEventText">推送事件类型</div>
        <div class="pushEventContainer">
          <el-checkbox-group v-model="dataForm.checkList">
            <el-checkbox label="startAndEndPlay">开播下播</el-checkbox>
            <el-checkbox label="recordStart">录制开始</el-checkbox>
            <el-checkbox label="recordFinish">录制完成</el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
      <!-- 微信群配置 -->
      <div>
        <div class="pushSetupTextContainer">
          <div class="pushSetupText">企业微信群机器人</div>
          <div class="pushSetupText">查看教程</div>
        </div>
        <div style="margin-top: 32px;">
          <el-form-item label="WebHook地址" prop="webHookUrl">
            <el-input v-model.number="dataForm.webHookUrl" placeholder="请输入WebHook地址"></el-input>
          </el-form-item>
          <el-form-item label="消息接收者" prop="msgRecipient">
            <el-input v-model.number="dataForm.msgRecipient" placeholder="请输入消息接收者"></el-input>
          </el-form-item>
        </div>
        <div style="text-align: right;">
          <afp-button type="primary" plain size="small">测试消息推送</afp-button>
          <afp-button type="success" size="small">应用</afp-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'ReplayClientMsgPushSetup',

  data() {
    return {

      dataForm: {
        startPush: false,
        checkList: [],
        webHookUrl: "",
        msgRecipient: "",
      },
      dataRule: {},
    };
  },

  mounted() {

  },

  methods: {

  },
};
</script>
<style scoped>
.pushSetupText {

  padding: 0 16px;
  height: 30px;
  background: #F5F7F9;
  border-radius: 15px;
  font-weight: 400;
  font-size: 14px;
  color: #677583;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pushSetupTextContainer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.pushEventContainer {
  margin-top: 16px;
}

.pushEventText {
  margin-top: 16px;
  width: 116px;
  height: 30px;
  background: #F5F7F9;
  border-radius: 15px;
  font-weight: 400;
  font-size: 14px;
  color: #677583;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pushWechatContainer {
  display: flex;
  align-items: center
}

.msgPushSetupContainer {
  margin-top: 20px;
  width: 500px;
}
</style>
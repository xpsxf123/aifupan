<template>
  <div class="component-doc">
    <h2>SmsCodeInput 验证码输入</h2>
    <p>带倒计时和 API 调用的验证码输入框。</p>

    <div class="demo-block">
      <div style="width: 300px">
        <SmsCodeInput v-model="code" phone="13800138000" :api="mockSendApi" smsKey="doc-test" />
      </div>
      <p style="margin-top: 10px">输入值: {{ code }}</p>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import SmsCodeInput from '@/components/SmsCodeInput/index.vue'

  const code = ref('')

  const mockSendApi = async (phone) => {
    return new Promise((resolve) => {
      console.log('Sending SMS to', phone)
      setTimeout(() => {
        resolve(true)
        alert('验证码已发送 (模拟)')
      }, 1000)
    })
  }
</script>

<style scoped>
  .component-doc {
    padding: 20px;
    background: #fff;
  }
  .demo-block {
    padding: 20px;
    border: 1px solid #ebeef5;
    margin: 20px 0;
  }
</style>

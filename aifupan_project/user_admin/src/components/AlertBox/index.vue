<template>
  <div class="alert-box">
    <div class="alert-content flex items-center">
      <div class="svg-container">
        <SvgIcon name="notice" :iconStyle="iconStyle"></SvgIcon>
      </div>
      <span class="alert-message ml-10"
        >当前为<span class="focus">子公司管理员</span>，只能看到子公司<span class="focus">管辖直播间</span
        >的商品数据</span
      >
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue'

  const iconStyle = computed(() => {
    return {
      width: '14px',
      height: '14px'
    }
  })
</script>

<style scoped lang="scss">
  .svg-container {
    display: flex;
    align-items: center;
  }
  .alert-box {
    padding: 15px 20px;
    background: #ffffff;
    border-radius: 10px;
    border: 1px solid #f4be34;
    font-size: 14px;
    letter-spacing: 1px;
    .focus {
      color: #444dff;
    }
    .alert-message {
      line-height: 1;
    }
  }
</style>

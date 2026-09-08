<template>
  <div class="funnel-box">
    <div class="text-main fw-500 mb-20 convert">转化漏斗</div>
    <div class="funnel-content-main">
      <div class="rate-content">
        <img src="@/assets/images/funnel/line.png" />
        <div class="rate-text">
          <div class="text-main fw-500 font-s12">{{ getRateLable }}</div>
          <div class="text-regular font-s14">{{ getRateText }}</div>
        </div>
      </div>
      <div class="datas-content">
        <img src="@/assets/images/funnel/main.png" />
        <div class="datas-top">
          <span class="font-s14 text-main">{{ getTop.label }}</span>
          <span class="font-s18 text-white">{{ formatValue(getTop) }}</span>
        </div>
        <div class="datas-bottom">
          <span class="font-s14 text-main">{{ getBottom.label }}</span>
          <span class="font-s18 text-white">{{ formatValue(getBottom) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { computed, defineProps } from 'vue'
  const props = defineProps({
    data: {
      type: Array,
      default: () => []
    }
  })

  const getRateText = computed(() => {
    return `${props.data?.rate?.join('-')}${props.data?.unit || ''}`
  })
  const getRateLable = computed(() => {
    return `${props.data?.label || ''}`
  })

  const getDatas = computed(() => {
    return props.data?.datas || []
  })

  const getTop = computed(() => {
    return getDatas.value?.top || {}
  })
  const getBottom = computed(() => {
    return getDatas.value?.bottom || {}
  })

  const formatValue = (data) => {
    if (Array.isArray(data.datas)) {
      const [a, b] = data?.datas || []
      if (a !== undefined && b !== undefined && String(a) === String(b)) {
        return `${a}${data?.unit || ''}`
      }
      return `${data?.datas?.join('-')}${data?.unit || ''}`
    } else {
      return `${data?.data}${data?.unit || ''}`
    }
  }
</script>

<style lang="scss" scoped>
  .convert {
    font-weight: bold;
  }

  .funnel-box {
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  .funnel-content-main {
    // min-height: 340px;
    // min-width: 400px;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    overflow: hidden;
    // padding: 30px;
    img {
      width: 100%;
    }
  }
  .rate-content {
    width: 20%;
    position: relative;
    text-align: right;
    .rate-text {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      display: flex;
      align-items: center;
      flex-direction: column;
      justify-content: flex-start;
      width: 100%;
      white-space: nowrap;
    }
    img {
      max-width: 60%;
    }
  }
  .datas-content {
    display: flex;
    gap: 20px;
    width: 87%;
    position: relative;
    .datas-top,
    .datas-bottom {
      position: absolute;
      width: 100%;
      height: calc(50% - 10px);
      display: flex;
      align-items: center;
      justify-content: space-between;
      span {
        width: 50%;
        text-align: center;
      }
    }
    .datas-top {
      top: 0;
    }
    .datas-bottom {
      bottom: 0;
    }
  }
</style>

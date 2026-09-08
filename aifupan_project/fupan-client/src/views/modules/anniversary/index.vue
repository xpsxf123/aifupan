<template>
  <div class="anniversary w100 main-bg">
    <div class="anniversary-title pd-b40">
        <img src="@/assets/imgs/activity/1znq/1.png" alt="">
    </div>
    <div class="anniversary-content">
        <div class="anniversary-content-img">
            <img src="@/assets/imgs/activity/1znq/2.png" alt="">
            <img src="@/assets/imgs/activity/1znq/3.png" alt="">
            <img src="@/assets/imgs/activity/1znq/4.png" alt="">
        </div>
        <p class="text-center font-s14 text-color2">*活动最终解释权归爱复盘团队所有</p>
    </div>
  </div>
</template>

<script setup>

</script>

<style lang="scss" scoped>
.anniversary-title,.anniversary-content{
    img{
        max-width: 100%;
    }
}
.anniversary-content{
    padding: 0 68px;
    min-height: 800px;
    img{
        margin-bottom: 60px;
    }
    .anniversary-content-img{
        margin-bottom: -60px;
    }
    p{
        padding: 40px 0;
    }
}
</style>
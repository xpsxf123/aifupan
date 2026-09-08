<template>
    <div class="flex-jc-e flex-ai-c">
      <img src="@/assets/imgs/new_logoT.png" style="width: 20px;height: 20px;margin-right: 4px;">
      <!-- <span style="color: #4D4D4D;font-size: 12px;">没流量、没转化、怕违规？</span> -->
      <img src="@/assets/imgs/textgif.gif" style="width: 144px;">
      <div style="display: flex;align-items: center;margin-left: 8px;justify-content: center;">
          <img src="@/assets/imgs/rightgif.gif" style="width: 18px;margin-right: 2px;">
          <span class="url-style" @click="ifupUrl">点击进入爱复盘官网</span>
      </div>
    </div>
</template>

<script>
    export default{
        data(){
            return{

            }
        },
        methods:{
            // 跳转到官网
            ifupUrl(){
              window.open('https://www.ifupan.com/', '_blank')
            },
        }
    }
</script>

<style>
  .url-style{
    font-size: 12px;
    color: var(--color-main);
    border-bottom: 1px solid var(--color-main);
    cursor: pointer;
  }
  .url-style:hover{
    color: red;
    border-bottom: 1px solid red;
  }
</style>
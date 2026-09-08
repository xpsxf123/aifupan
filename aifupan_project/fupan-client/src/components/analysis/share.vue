<template>
     <div class="wordsItemContainer" >
        <!-- 123123 -->
        <!-- <div style="color: #0077FF; cursor: pointer;" @click="shareAnalysis" v-if="!isShareUrl">分享复盘</div>
        <div style="color: #0077FF; cursor: pointer;" v-else @click="copyShareAnalysisLink">复制分享链接</div> -->
        <!-- {{ isShareUrl }} -->
        <afp-button id="share-dom" type="primary" :plain="false"  @click="btClick">{{ isShareUrl?'复制分享链接':'分享复盘' }}</afp-button>
    </div>
 </template>
 
 <script>
 export default {
   name: "",
   props: {
    isShareUrl:{
        type: Boolean,
        default: false
    }
   },
   computed:{
 
   },
   data() {
     return {
     };
   },
   mounted(){
 
   },
   created(){
    
   },
   methods: {
    shareAnalysis(){
        this.$emit('analysisClick')
    },
    copyShareAnalysisLink(){
        this.$emit("shareClick")
    },
    btClick(){
        if(this.isShareUrl){
            this.copyShareAnalysisLink()
        }else{
            this.shareAnalysis()
        }
    }
   }
   
 };
 </script>
 
 <style scoped>
 .wordsItemContainer {
    display: flex;
    align-items: center;
    padding: 0 12px;
}
 </style>
 
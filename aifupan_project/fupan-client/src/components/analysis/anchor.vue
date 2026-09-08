<template>
    <div class="flex-jcai-sb">
        <slot name="anchor-left"></slot>
        <div  class="anchor-box">
            <div v-if="src" class="anchor-c">
                <img :src="src" class="anchor-img">
                <template v-if="isCompare">
                    <img src="@/assets/imgs/zy.png" v-if="anchorInfo.AccountType===0" class="avatar-back" alt="">
                    <img src="@/assets/imgs/hy.png" v-if="anchorInfo.AccountType===1" class="avatar-back" alt="">
                </template>
            </div>
            <img v-else-if="isTextImg" src="@/assets/imgs/txt.png" class="default-anchor-img">
            <img v-else src="@/assets/imgs/1_9_30/video.png" class="default-anchor-img">
            <div>
                <div :style="isCompare?{maxWidth:'150px'}:{}">
                    <div class="anchor-name slh">{{ getVideoName(name) }}</div>
                    <div class="anchor-name slh" v-if="anchorInfo">
                        <span class="text-xs gray-9" v-if="anchorInfo?.platform == PLATFORM_ENUM.douyin">抖音号：</span>
                        <span class="text-xs gray-9" v-if="anchorInfo?.platform == PLATFORM_ENUM.kuaishou">快手号：</span>
                        <span class="text-xs gray-9" v-if="anchorInfo?.platform == PLATFORM_ENUM.shipinhao">视频号</span>
                        <span class="text-xs gray-9">{{anchorInfo?.anchorNumber}}</span>
                    </div>
                </div>
                <div class="anchor-name slh" v-if="isCompare">
                    <template v-if="syncScene===1">
                        <img src="@/assets/imgs/ben.png" alt="" v-if="sortIndex===1">
                        <img src="@/assets/imgs/shang.png" alt="" v-if="sortIndex===2">
                    </template>
                    <template v-else>
                        <img src="@/assets/imgs/you.png" alt="" v-if="sortIndex===1">
                        <img src="@/assets/imgs/2_5_8/can.png" alt="" v-if="sortIndex===2">
                    </template>
                </div>
            </div>
        </div>
        <slot name="anchor-right"></slot>
    </div>
 </template>
 
 <script>
 import { PLATFORM_ENUM } from '@/enum'
 export default {
   name: "anchor",
   props: {
    src: {
        type: String,
        default: ''
    },
    name: {
        type: String,
        default: ''
    },
    isCompare: {
        type: Boolean,
        default: false
    },
    fileType: {
        type: [String,Number],
        default: ''
    },
       videoInfo:{
           type: Object,
           default: ()=>{
               return {}
           }
       },
       anchorInfo:{
           type: Object,
           default: ()=>{
               return {}
           }
       },
       syncScene: {  //syncScene:对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
           type: [String, Number],
           default: ''
       },
       sortIndex:{
           type: [String, Number],
           default: ''
       }
   },
   computed:{
    isTextImg(){
        return this.fileType === 2
    },
       getVideoName() {
           return (item) => {
               return item ? item.split('_')?.slice(-2)?.join('')?.replace(/\.[^.]+$/, '') : ''
           }
       },
   },
   data() {
     return {
         PLATFORM_ENUM
     };
   },
   mounted(){
 
   },
   created(){
    
   },
   methods: {
   }
   
 };
 </script>
 
 <style scoped>
.anchor-box {
    display: flex;
    align-items: center;
}
.anchor-img {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    border: 0.5px #ccc solid;
}
.default-anchor-img{
    width: 40px;
    height: 40px;
    /* border-radius: 50%; */
}
.anchor-c{
    position: relative;
}
.avatar-back{
    position: absolute;
    left: -1px;
    top: -2px;
    width: 42px;
    height: 46px
}
.anchor-name {
    color: #2E3742;
    font-size: 14px;
    margin-left: 10px;
    max-width: 300px;
}
 </style>
 
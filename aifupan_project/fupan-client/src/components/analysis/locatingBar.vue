<template>
    <div :class="{'locating-bar-box':true,'locating-bar-box-pure':versionType===VERSION_TYPE.PURE}">
        <div v-if="!isCompare" class="video-slider-text">
            <h4 class="header-title" v-if="versionType===VERSION_TYPE.PURE">快速定位</h4>
            <Title v-else>快速定位</Title>
        </div>
        <div class="video-slider brs-4">
            <el-slider v-model="sliderValue" :marks="marks" :max=max
                @change="sliderChange" :format-tooltip="tooltip">
            </el-slider>
        </div>
    </div>
 </template>
 
 <script>
 import Title from './../title/index.vue'
 import {VERSION_TYPE} from "@/enum";
 export default {
    components:{
        Title
    },
   name: "",
   props: {
    value: {
        type: Number,
        default: 0
    },
    marks:{
        type: Object,
        default:()=>{return {}}
    },
    max: {
        type: Number,
        default: 0
    },
    tooltip: {
        type:Function,
        default: (msg)=>msg
    },
    loading: {
        type: Boolean,
        default: false
    },
    isCompare: {
        type:Boolean,
        default: false
    }
   },
   computed:{
        versionType(){
            return this.$store.getters.getVersionType
        }
   },
   watch: {
    value: {
        handler(val){
            this.sliderValue = val;
        },
        immediate: true
    }
   },
    data() {
        return {
            VERSION_TYPE,
            sliderValue: 0, // 进度条绑定值
        };
    },
    mounted(){
        // this.sliderValue = this.value;
    },
    created(){
        
    },
    methods: {
        sliderChange(){
            this.$emit('change',  this.sliderValue)
        }
    }
   
 };
 </script>
 
 <style scoped>
 .locating-bar-box{
    display: flex;
    height: 58px;
    justify-items: center;
    align-items: center;
 }
 .locating-bar-box-pure{
     background: linear-gradient( 90deg, #E9F4FF 0%, #E0EFFF 100%);
     border-radius: 8px 8px 8px 8px;
 }
 .header-title{
     font-size: 14px;
     font-weight: 500;
     padding-left: 8px;
 }
 /* 快速定位文本 */
.video-slider-text {
    padding-right: 14px;
    z-index: 1;
}
/* 视频定位条 */
.video-slider {
    padding: 11px 22px;
    border-radius: 8px 8px 0px 0px;
    flex: 1;
    ::v-deep(.el-slider__marks-text){
        font-size: 12px;
    }
    ::v-deep(.el-slider__bar){
        background: linear-gradient( 90deg, #83E5FC 0%, #8F8EFF 100%);
        box-shadow: inset 0px -1px 1px 0px rgba(1,89,174,0.25);
    }
}
 </style>
 
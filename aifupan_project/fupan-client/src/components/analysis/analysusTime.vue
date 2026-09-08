<template>
    <div class="flex-jc-sb">
        <Anchor :src="anchorInfo?.AnchorAvatar" :isCompare="isCompare" :name="anchorInfo.AnchorName">
            <template #anchor-left><slot name="anchor-left"></slot></template>
            <template #anchor-right><slot name="anchor-right"></slot></template>
        </Anchor>
        <div class="flex-ai-c font-s14 font-w400 text-color1">
            <div class="flex-jc-c pd-l10 pd-r10 br-r1-c1" :class="itemClass">
                <div>{{startTitle}}</div>
                <div>{{ getStartTime }}</div>
            </div>
            <div class="flex-jc-c pd-l10 pd-r10 br-r1-c1" :class="itemClass">
                <div>{{endTitle}}</div>
                <div>{{ getStartTime }}</div>
            </div>
            <div class="flex-jc-c pd-l10 pd-r10" :class="itemClass">
                <div>{{durationTitle}}</div>
                <div>{{ getDurationStr }}</div>
            </div>
        </div>
    </div>
 </template>
 
 <script>
  import Anchor from './anchor.vue';
 export default {
   name: "",
   components: {
    Anchor
   },
   props: {
    itemClass:{
        type: String,
        default:""
    }, 
    isCompare: {
        type: Boolean,
        default: false
    },
    anchorInfo: {
        type: Object,
        default: ()=>{return {}}
    },
    startTitle:{
        type: String,
        default: '开始：'
    },
    endTitle:{
        type: String,
        default: '结束：'
    },
    durationTitle: {
        type: [Number,String],
        default: '时长：'
    },
    videoInfo: {
        type: Object,
        default: ()=>{return {}}
    },
    startTime: {
        type: [Number,String],
        default: ''
    },
    endTime: {
        type: [Number,String],
        default: ''
    },
   },
   computed:{
    getDurationStr(){
        let durationStr = "";
        // 计算时长
        if (this.videoInfo && this.videoInfo.Duration) {
            let arr = this.videoInfo.Duration.split(":");
            if (arr[0] && parseInt(arr[0])) {
                 durationStr += arr[0] + "时";
            }
            if (arr[1] && parseInt(arr[1])) {
                 durationStr += arr[1] + "分";
            } else {
                 durationStr = "01分";
            }
        }
        return durationStr
    },
    getStartTime(){
        return this.startTime || this.videoInfo?.StartTime?.substring(0, 16);
    },
    getEndTime(){
        return this.endTime || this.videoInfo?.EndTime?.substring(0, 16);
    }
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
    
   }
   
 };
 </script>
 
 <style scoped>
 </style>
 
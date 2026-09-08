<template>
    <div class="flex-jc-sb" >
      <Anchor :name="fileInfo.fileName" :isCompare="isCompare">
        <template #anchor-left><slot name="anchor-left"></slot></template>
        <template #anchor-right><slot name="anchor-right"></slot></template>
      </Anchor>
      
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
    updateTime: {
      type:String,
      default: ''
    },
    analysisTime: {
      type:String,
      default: ''
    },
    fileInfo: {
        type: Object,
        default: ()=>{return {}}
    },
   },
   computed:{
    getUpdateTime(){
      return this.updateTime || this.fileInfo?.uploadTime?.substring(0, 16)
    },
    getAnalysisTime(){
      return this.analysisTime || this.fileInfo?.analysisTime?.substring(0, 16)
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
 
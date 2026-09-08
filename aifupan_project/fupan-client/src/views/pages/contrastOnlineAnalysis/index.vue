<template>
    <div  style="height: 100vh;">
        <contrast targetType="webOnline" @webElfClick="elfClick" :id="getId" :httpRequest="getHttp" @request="requestHandle">
            <template #page-right>
                <officialBottom class="pd-r8"></officialBottom>
            </template>
        </contrast>
    </div>
  </template>
  
  <script>
  import officialBottom from "../common/officialBottom.vue";
  import contrast from '/src/views/commonComponent/analysis-contrast.vue';
  export default {
    components: {
      contrast,
      officialBottom
    },
    provide() {
        return {
            appVnode: this
        }
    },
    props:{
      
    },
    data() {
      return {
      
      };
    },
    computed: {
        getHttp(){
            return this.$httpBack.v2000.getOnlineContrastAnalysis
        },
        getId(){
            return this.$route.params.id;
        }
    },
    watch: {},
    methods: {
        elfClick(type,o){
            this.$router.push({
                path: `/contrastAiAnalysis/${type}/${this.$route.params.id}`,
                query: {
                    ...o
                }
            })
        },
        requestHandle({type,data}){
            if(type === 'error'){
                if(data.code === 4001){
                    this.$emit('changeType','login')
                }
            }else if(type === 'lose'){
                this.$emit('changeType','lose');
            }
        }
    },
    created() {
      
    },
    mounted() {
      
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
  }
  </script>
  <style lang='scss' scoped>
  
  </style>
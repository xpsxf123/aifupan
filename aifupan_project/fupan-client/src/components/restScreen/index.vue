<template>
    <div class="vh100 pd-40 bg-c rest-screen-box" >
        <div class="flex-jc-e pd-t40 pd-r40 mg-t20">
            <span class="font-s20 text-colorMain">爱复盘已工作：15小时</span>
        </div>
        <div class="">
            <h1 class="flex-ji-c">
                <img style="width: 46px;" src="~@/assets/imgs/restScreenLogo.png" alt="">
                <span class="mg-l10 font-s40 text-colorc1">爱复盘提醒您</span>
            </h1>
            <p class="flex-ji-c">
                <span class="font-s34" style="color: #389681;">正在录制中</span>
            </p>
            <div class="text-colorMain pd-t40">
                <div class="font-s36 pd-t40 flex-ji-c">
                    <span>电脑</span>
                    <span class="text-colorErr">请勿断网、休眠！</span>
                    <span>以防断录或漏录，导致视频分段</span>
                </div>
                <div class="flex-ji-c pd-t18">
                    <span class="font-s26">（注意：部分电脑熄屏也有可能断网、休眠）</span>
                </div>
            </div>
            <div class="flex-jc-sb pd-t40 mg-t40" style="padding:40px 235px 0 235px;">
                <div class="flex-ai-c">
                    <afp-button class="font-s24 pd-16" type="primary" @click="goHome">进入爱复盘</afp-button>
                </div>
                <div class="btns-box">
                    <el-button class="font-s20" type="text" @click="setNotRest">如何设置不休眠 <i class="el-icon-right"></i></el-button>
                    <el-button class="font-s20" type="text" @click="showQrCode">咨询技术客服 <i class="el-icon-right"></i></el-button>
                    <el-button class="font-s20" type="text" @click="toSystem">设置屏幕保护 <i class="el-icon-right"></i></el-button>
                </div>
            </div>
        </div>
    </div>
  </template>
  <script>
  export default {
    components: {},
    props:{
        
    },
    inject: ['APP'],
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        goHome(){
            this.$router.push({
                path: '/dataAnalysis'
            })
        },
        showQrCode(){
            this.APP.showQrCode();
        },
        toSystem(){
            this.$router.push({
                path: '/system',
                query: {
                    type: 'restScreen'
                }
            })
        },
        setNotRest(){
            this.$cMsg.custom({
                dangerouslyUseHTMLString: true,
                title: '设置不休眠',
                customClass: 'message-box-500',
                message: `
                <div class="overflow_hidden overflow_auto_y" style="height: 500px;">
                    <div>
                        123123
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        12312321
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        <br>
                        123123
                    </div>
                </div>
                `
            })   
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
};
  </script>

<style lang="scss" scoped>
.rest-screen-box {
    background-image: url('~@/assets/imgs/restScreenBg.png');
}
.btns-box{
    .el-button{
        display: block;
        margin:0;
        color: var(--text-1); 
        &:hover{
            color: var(--color-main);  
        }
    }
}


</style>
<template>
    <div>
        <addDom target="shipinhao" ref="shipinhaoAddDom" :weChatInfo="weChatInfo" @delInfo="delInfo">
            <template #sphAuthorization>
                <div class="flex-ai-c flex-jc-s">
                    <div v-if="weChatInfo" class="flex-ai-c">
                        <img style="max-width: 40px;" :src="weChatInfo?.AccountBody?.HeadImg || ''" alt="">
                        <span class="mg-l6">{{weChatInfo?.AccountBody?.NickName || ''}}</span>
                    </div>
                    <afp-button :type="weChatInfo?'text':'primary'" size="medium" :plain="false" style="width: 120px;" @click="toIfupanWebsite">{{weChatInfo?'重新授权':'去授权'}}</afp-button>
                </div>
            </template>
        </addDom>
        <shipinhaoDialog ref="shipinhaoDialog"></shipinhaoDialog>
    </div>
</template>

<script>
import shipinhaoDialog from './shipinhao-dialog.vue'
import addDom from './index.vue'
export default {
    components: {
        addDom,
        shipinhaoDialog
    },
    props:{
        
    },
    data() {
        return {
            weChatInfo: null,
        };
    },
    computed: {},
    watch: {},
    methods: {
        getWeChatInfo(){
            this.$CSharpNotify.addTask('weChatChannelsAuthorizationResult', (res, resolve) => {
                // this.setInfo(res);
                this.successInfo(res);
            }, () => {
                this.delInfo();
            })
        },
        toIfupanWebsite(){
            this.$refs.shipinhaoDialog.show();
            // this.getWeChatData(); 
            if(this.weChatInfo){
                this.delInfo();
            }
        },
        getInfo(){
            return JSON.parse(localStorage.getItem('shipinhaoInfo') || '{}');
        },
        setInfo(data = {}){
            localStorage.setItem('shipinhaoInfo', JSON.stringify(data) || "{}")
        },
        delInfo(){
            localStorage.removeItem('shipinhaoInfo');
            this.weChatInfo = null;
        },
        // getWeChatData(){
        //     // setTimeout(()=>{
        //     //     let info = this.getInfo();               
        //     //     if(info.userChannelId){
        //     //         this.successInfo(info);
        //     //     }else{
        //     //         // 轮旋查询数据
        //     //         this.getWeChatData();
        //     //     }
        //     // }, 5000)
        // },
        successInfo(info){
            this.$refs.shipinhaoDialog.hide();
            if(info.duplicated === 'false'){
                this.$message.error('重复授权: 该用户已绑定账号, 请先解绑账号后重试');
                this.delInfo();
                this.toIfupanWebsite();
                return
            }
            const accountBody = JSON.parse(info.accountBody || '{}');
            info.accountBody = accountBody;
            this.weChatInfo = info;
            this.$refs.shipinhaoAddDom.setAuthorizerInfoId(info.AuthorizerInfoId);
        }
    },
    created() {
        this.getWeChatInfo()
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {
        this.delInfo();
    }, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>
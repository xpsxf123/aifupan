<template>
    <customerServiceQrCode ref="qrCode" width="414px" custom-class="version-qr-code" >
        <div v-if="dialogData.html" v-html="dialogData.html"></div>
        <template v-else> 
            <div v-if="$store.state?.userInfo?.packageLevel === -1" class="font-s16 text-center">
                您当前的版本是<span class="font-s20 text-colorErr">激活</span>版本，<span class="font-s20 text-colorErr">需要激活后</span>才能正常使用。请添加产品顾问，帮您<span class="font-s20 text-colorErr">免费激活软件</span>并<span class="font-s20 text-colorErr">领取</span>更高的版本的试用套餐。
                <!-- 您当前版本为<span class="font-s20 text-colorErr">未激活</span>版本，请联系产品顾问，<br>领取<span class="font-s20 text-colorErr">免费试用权限</span> -->
            </div>
            <div v-else-if="$store.state?.userInfo?.packageLevel === 0" class="font-s16 text-center">
                您当前版本是<span class="font-s20 text-colorErr">免费</span>版本，请联系产品顾问，升级您的套餐版本！
            </div>
        </template>
        <template #footer>
            <div class="text-color1 font-s12 mg-t8">微信扫码添加</div>
        </template>
    </customerServiceQrCode>
</template>

<script>
import customerServiceQrCode from './customerServiceQrCode.vue';
export default {
    components: {
        customerServiceQrCode
    },
    props:{
    },
    data() {
        return {
            dialogData:{}
        };
    },
    computed: {},
    watch: {},
    methods: {
        show(data){
            this.dialogData = {}; //重置数据
            this.$refs.qrCode.init();
            this.$nextTick(() => {
                document.querySelectorAll('.version-qr-code')[0].parentNode.classList.add('zIndex999999')
            })
            if(data){
                this.dialogData = data;
            }
        },
        // 获取用户信息
        getUserInfo() {
            return this.$httpBack.user.infoByClient({}).then(res => {
                if (res.code == 0 && res.data) {
                    this.$store.commit("saveUserInfo", res.data);
                }
            });
        },
        qrTime(bl){
            if(bl){
                TimeStorage.setTimeItem('qrTime',true, 24*60*60)
            }else{
                return TimeStorage.getTimeItem('qrTime')
            }
        },
        autoShow(type){
            // 检查用户信息是否退回免费版。无论成功还是失败都会提示。
            return this.getUserInfo().finally(()=>{
                if(this.$store.state?.userInfo?.packageLevel !== 0 && this.$store.state?.userInfo?.packageLevel !== -1){
                    return Promise.resolve();
                };
                if(type === 'init'){
                    this.show();
                    this.qrTime(true)
                }else if(!this.qrTime()){
                    this.show();
                    this.qrTime(true)
                }
                return Promise.resolve()
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
}
</script>
<style lang='scss'>
.zIndex999999{
    z-index: 99999999 !important;
}
</style>
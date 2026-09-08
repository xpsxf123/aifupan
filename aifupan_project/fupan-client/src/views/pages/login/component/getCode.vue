<template>
    <div class="get-code-box mg-l8">
        <afp-button :disabled="getCodeCountdown>0" @click="getCode" :style="{width:width, whiteSpace: 'nowrap'}"  :size="size">{{ getCodeCountdown <= 0 ? '获取验证码': `${getCodeCountdown}s` }}</afp-button>
        <!-- <span class="getCodeText" v-if="getCodeCountdown <= 0" @click="getCode">获取验证码</span>
        <span class="getCodeText" v-else style="cursor:not-allowed;">{{ this.getCodeCountdown }}s</span> -->
    </div>
</template>

<script>

export default {
    components: {},
    props: {
        phone: {
            type:String,
            default: ''
        },
        isGetCode: {
            type: Function,
            default: null
        },
        size: {
            type: String,
            default: 'medium'
        },
        width: {
            type: String,
            default: '90px'
        }
    },
    data() {
        return {
            getCodeCountdown: 0,
            getCodeInterval: null
        };
    },
    computed: {},
    watch: {},
    methods: {
        // 获取验证码
        async getCode() {
            if (this.getCodeCountdown > 0) {
                return;
            }


            var reg_tel = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/;
            if (!this.phone || !reg_tel.test(this.phone)) {
                this.$emit('error')
                // this.$message.error("请填写正确的手机号");
                return;
            }
            // 判断是否执行获取验证码逻辑
            if(typeof this.isGetCode === 'function'){
                let o = await this.isGetCode();
                if(o === false){
                    return;
                }
            }
            this.$httpBack.user.getPhoneCode({ phone: this.phone }).then(res => {
                if (res.code == 0) {
                    this.$emit('success');
                    this.$message.success("获取成功，请留意短信");
                }
            });
            this.getCodeCountdown = 60;
            // 启动定时器
            this.getCodeInterval = setInterval(() => {
                // 创建定时器，每1秒执行一次
                this.getCodeCountdown -= 1;
                if (this.getCodeCountdown <= 0) {
                    clearInterval(this.getCodeInterval);
                    this.getCodeInterval = null;
                    this.$emit('refresh')
                }
            }, 1000);
        },
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.get-code-box{
    display: inline-block;
    white-space: nowrap;
}
.getCodeText {
    margin-left: 10px;
    width: 90px;
    font-size: 14px;
    color: #2E3742;
    cursor: pointer;
    height: 40px;
    border-radius: 4px;
    border: 1px solid #DCE0E7;
    text-align: center;
    line-height: 40px;
    display: inline-block;
    box-sizing: border-box;
}
</style>

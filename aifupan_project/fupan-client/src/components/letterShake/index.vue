<template>
    <div class="letter-shake flex-ji-c">
        <b class="font-s12 text-colorc2">{{ getText }}</b>
        <span v-for="(text,index) in getLetterShakeText" :key="index" class="font-s12 text-colorc2">{{ text }}</span>
    </div>
</template>

<script>
export default {
    components: {},
    props:{
        text: {
            type:String,
            default:''
        },
        position: {
            type: Number,
            default: 0
        }
    },
    data() {
        return {
        
        };
    },
    computed: {
        getText(){
            return this.text?.slice(0,this.position);
        },
        getLetterShakeText(){
            return this.text?.slice(this.position)?.split('');
        }
    },
    watch: {},
    methods: {
        
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
.letter-shake span {
  display: inline-block;
  animation: letterShake 1s infinite;
}

.letter-shake span:nth-child(5n+1) { animation-delay: 0.2s; }
.letter-shake span:nth-child(5n+2) { animation-delay: 0.4s; }
.letter-shake span:nth-child(5n+3) { animation-delay: 0.6s; }
.letter-shake span:nth-child(5n+4) { animation-delay: 0.8s; }
.letter-shake span:nth-child(5n+5) { animation-delay: 1s; }
/* 继续为其他字母设置延迟 */

@keyframes letterShake {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}
</style>
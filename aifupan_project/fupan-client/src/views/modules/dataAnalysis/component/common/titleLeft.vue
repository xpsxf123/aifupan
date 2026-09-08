<template>
    <div class="statusLeftContainer">
        <slot>
            <slot name="titleLeft"></slot>
            <div class="aiTime flex flex-col justify-around items-start" v-if="versionType === VERSION_TYPE.AGENT">
                <div>AI智能分析时长</div>
                <div class="flex justify-between items-center" style="width: 100%">
                    <div style="padding-right: 12px"><span class="text-2xl font-bold"> {{ retainDecimals(getAiAnalvsisTime / 60) }}</span> 小时</div>
                    <afp-button plain round size="small" @click="$emit('leftClick')" style="background: transparent">立即扩容</afp-button>
                </div>
            </div>
            <!-- 小于三小时显示文案提取 -->
            <div v-if="getAiAnalvsisTime<480" class="official flex flex-col justify-around items-start">
                <img class="img_way" src="@/assets/imgs/dailyGiveaway.png" alt="">
                <div>文案提取剩余</div>
                <div><span class="text-2xl font-bold">{{ getTextExtractionNum }}</span> 分钟</div>
            </div>
            <div class="detected flex flex-col justify-around items-start">
                <div>已检测</div>
                <span class="text-2xl font-bold">{{ detectionTime }}</span>
            </div>
            <!-- <div v-if="isFree&&versionType === VERSION_TYPE.AGENT"
                 class="free-icon flex flex-col justify-around items-start"></div> -->
            <div v-if="isDiffSevenDays" class="expirationDate flex flex-col justify-around items-start">
                <div style="color: #E43B32">会员版本即将到期</div>
                <div style="font-size: 13px">
                    <div>会员到期时间</div>
                    <div>{{getUserInfo?.expirationDate?.substring(0, 10) }}</div>
                </div>
            </div>
        </slot>
    </div>
</template>

<script>
import myUtils from '/src/utils/utils';
import {VERSION_TYPE} from '@/enum'
export default {
components: {},
props:{
    detectionTime: {
        type: [Number,String],
        default: ''
    },
    aiAnalysisTime: {
        type:Number,
        default: 0
    }
},
data() {
    return {
        VERSION_TYPE,
    };
},
computed: {
    getAiAnalvsisTime(){
        return this.$store.getters?.getUserproperty?.aiAnalysisTime || 0
    },
    getTextExtractionNum() {
        return this.$store.getters?.getUserproperty?.textExtractionNum || 0
    },
    isFree(){
        return this.$store.getters.isFree
    },
    getUserInfo() {
        return this.$store.state.userInfo;
    },
    isDiffSevenDays() {
        const now = new Date()
        const target = new Date(this.getUserInfo?.expirationDate)

        if (target instanceof Date && !isNaN(target.getTime())) {
            now.setHours(0, 0, 0, 0)
            target.setHours(0, 0, 0, 0)

            const diffTime = target - now // 毫秒差
            const diffDays = diffTime / (1000 * 60 * 60 * 24)

            return Math.abs(diffDays) <= 7
        } else {
            return false
        }
    },
    versionType(){
        return this.$store.getters.getVersionType
    }
},
watch: {},
methods: {
    retainDecimals: myUtils.retainDecimals,
    // getUserProperty
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
.statusLeftContainer {
    font-size: 14px;
    color: #2E3742;
    display: flex;
    // align-items: center;
    // justify-items: center;
    margin: 0 -8px;
    >div{
        padding: 12px 16px;
        margin: 0 8px;
        >span{
            padding: 0 3px;
        }
    }
    .aiTime{
        height: 99px;
        min-width: 268px;
        border-radius: 10px;
        background: url("~@/assets/imgs/theme/ai_time.png") no-repeat center center;
        background-size: cover;
        box-shadow: 0 2px 16px 0 rgba(143, 161, 226, 0.15);
    }
    .common-bg{
        border-radius: 10px;
        background: radial-gradient(318.59% 138.89% at 78.4% 108.5%, rgba(159, 196, 255, 0.50) 0%, rgba(179, 232, 255, 0.50) 100%);
        box-shadow: 0 2px 16px 0 rgba(143, 161, 226, 0.15);
    }
    .official{
        height: 99px;
        width: 176px;
        position: relative;
        background: url("~@/assets/imgs/theme/doc.png") no-repeat center center;
        background-size: cover;
        .img_way{
            height: 15px;
            position: absolute;
            top: 10px;
            right: 24px;
        }
    }
    .detected{
        height: 99px;
        width: 176px;
        background: url("~@/assets/imgs/theme/check.png") no-repeat center center;
        background-size: cover;
    }
    .free-icon{
        height: 99px;
        width: 176px;
        background: url("~@/assets/imgs/theme/free-icon.png") no-repeat center center;
        background-size: cover;
    }
    .expirationDate{
        height: 99px;
        width: 176px;
        background: url("~@/assets/imgs/theme/expirationDate.png") no-repeat center center;
        background-size: cover;
    }
}
</style>
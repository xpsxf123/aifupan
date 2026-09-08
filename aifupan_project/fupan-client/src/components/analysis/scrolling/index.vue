<template>
    <div class="scrolling-item">
        <div v-if="item.showUpgradeTips" class="upgrade-hint pd-t6 pd-b6">
            <h3 class="mg-0 font-s16" style="color: #7D3D16">温馨提示：</h3>
            <p class="font-s14 mg-0" :class="{'color-white': !ai}" style="line-height: 18px;">{{$store.getters?.getPackageLevelName}}单场直播仅展示<span class="text-colorErr">{{item.sort-1}}条</span>弹幕，
                想查看更多弹幕，请<el-button type="text" class="pd-0" @click="upgrade">立即升级</el-button>到更高版本</p>
        </div>
        <div class="pd-2 flex-jc-sb">
           <div>
                <span>
                    <span class="scrolling-item-dydj" v-if="isLevel && Number(item.level) !== -1">{{ item.level }}</span>
                    <span class="scrolling-item-fstdj" v-if="Number(item.fansLevelMin) + Number(item.fansLevelMax) > 0 && isFansLevel && Number(item.level) !== -1">{{ `${item.fansLevelMin}-${item.fansLevelMax}` }}</span>
                    <span class="scrolling-item-new" v-if="item.isNew && isNew">新</span>
                    <span class="scrolling-item-name font-s12 text-color3" v-if="isNickName"><slot name="name" v-bind="item">{{ item.nickName }}</slot>：</span>
                </span>
                <span class="scrolling-item-text font-s12 text-color1">{{ item.content }}</span>
           </div>
           <div v-if="isShowTime && isTime">
                <span class="scrolling-item-time font-s12" style="color: #636CBD;">{{ formatTime(item.recordDate)}}</span>
           </div>
        </div>
    </div>
</template>

<script>
import myUtils from '@/utils/utils'
export default {
    components: {},
    props:{
        item: {
            type: Object,
            default: ()=>{}
        },
        isShowTime: {
            type: Boolean,
            default: false
        },
        ai: {
            type: Boolean,
            default: false
        },
        /**
         * 扣除时间
         */
         deductionTime: {
            type: Number,
            default: 0
        },
        // 数据展示
        /*
        dataDisplay:{}
        弹幕助手中的数据展示
            dateTime
            时间(0不勾，1勾)
            nickName
            昵称(0不勾，1勾)
            level
            用户等级(0不勾，1勾)
            fansLevel
            粉丝团等级(0不勾，1勾)
            isNew
            新icon(0不勾，1勾)
        */
        dataDisplay: {
            type: Object,
            default: () => {return{}}
        }
    },
    inject: ['appVnode'],
    data() {
        return {
            
        };
    },
    computed: {
        isTime(){
            return this.dataDisplay?.dateTime ?? true
        },
        isLevel(){
            return this.dataDisplay?.level?? true
        },
        isNew(){
            return this.dataDisplay?.isNew?? true
        },
        isFansLevel(){
            return this.dataDisplay?.fansLevel?? true
        },
        isNickName(){
            return this.dataDisplay?.nickName?? true
        },
    },
    watch: {},
    methods: {
        upgrade(){
            this.appVnode?.showQrCode()
        },
        formatTime(time){
            if(this.deductionTime){
                return myUtils.toformatTime((time - this.deductionTime));
            }else{
                return myUtils.timestampToChinese(time, { format: 'HH:mm:ss' });
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
.scrolling-item{
    >div{
        flex-direction: row;
    }
    &-dydj,&-fstdj{
        display: inline-block;
        font-size: 10px;
        height: 14px;
        line-height: 14px;
        text-align: right;
        background-size: cover;
        padding-right: 4px;
        padding-top: 1px;
        margin-right: 5px;
    }
    &-dydj{
        width: 30px;
        background-image: url('~@/assets/imgs/dydj.png');  
    }
    &-fstdj{
        width: 37px;
        background-image: url('~@/assets/imgs/fstdj.png');
    }
    &-new{
        display: inline-block;
        vertical-align: middle;
        height: 14px;
        width: 14px;
        font-size: 10px;
        color: #09297D;
        text-align: center;
        background-size: cover;
        margin-right: 6px;
        background-image: url('~@/assets/imgs/xyh.png');
    }
    &-text{
        word-break: break-word;
    }
    &-time{
        white-space: nowrap;
    }
}
</style>
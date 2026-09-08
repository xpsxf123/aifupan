<template>
    <div class="commend-content-box">
        <div class="commend-box commend-1 flex-ji-c h100 cs-p" :class="{select1: getValue == 0}" @click="onClick(0)">
            <div class="flex-ji-c h100"><i class="font_family icon-dianzan mg-r4"></i><span class="font-s12">分析的好</span></div>
        </div>
        <div class="commend-box commend-2 flex-ji-c h100 cs-p" :class="{select2: getValue == 1}" @click="onClick(1)">
            <div class="flex-ji-c  h100"><i class="font_family icon-dianzan inversion mg-r4"></i><span class="font-s12">分析的烂</span></div>
        </div>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        value: {
            type: [Number, String],
            default: -1
        }
    },
    data() {
        return {
            select: false
        };
    },
    computed: {
        getValue: {
            get(){
                if(this.select !== false){
                    return this.select
                }
                return this.value
            },
            set(v){
                this.select = v;
            }
        }
    },
    watch: {},
    methods: {
        onClick(selectType){
            // 如果大于等于0则取消点赞
            if(this.value>=0){
                selectType = -1;
            }
            this.$emit('click',{
                data: selectType,
                callback:()=>{
                    this.getValue = selectType;
                }
            });
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
.inversion{
    display: inline-block;
    -moz-transform: scaleY(-1);
    -webkit-transform: scaleY(-1);
    -o-transform: scaleY(-1);
    transform: scaleY(-1);
    filter: FlipV;
}
.commend-content-box{
    margin:0 -5px;
}
.commend-box{
    margin: 0 5px;
    border-radius: 14px;
    border: 1px solid #ABAEB3;
    padding:0 6px;
    height: 28px;
    // line-height: 28px;
    display: inline-block;
    transition: all 0.3s;
}
.commend-1{
    &:hover{
        color: var(--color-success);
        border-color: var(--color-success);
    }
}
.commend-2{
    &:hover{
        color: var(--color-err);
        border-color: var(--color-err);
    }
}
.select1{
    color: var(--color-success);
    border-color: var(--color-success);
}
.select2{
    color: var(--color-err);
    border-color: var(--color-err);
}
</style>
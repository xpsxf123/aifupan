<template>
    <div>
        <afp-button slot="reference" 
        class="pd-0 font-s12"
        style="width: 42px;height: 42px;padding: 0;"
        type="primary"
        circle
        @click="onClick">
        <span style="line-height: 16px;">自建<br/>问题</span>
        </afp-button>
        <!-- <el-button type="text" @click="onClick" class="pd-0 mg-b10">
            <img style="max-width: 42px;" src="~@/assets/imgs/new/dzwt.png" alt=""></img>
        </el-button> -->
        <dialogList ref="dialogList" @select="onSelect" :aiCueType="aiCueType" :moreConfigProps="moreConfigProps"></dialogList>
    </div>
</template>

<script>
/**
 * @file 自建问题入口按钮。
 * @description 负责打开自建问题列表抽屉，并把选中的预设问题发送给 AI 问答区。
 */
import dialogList from './list.vue'
import {trackEvent} from "@/utils/laTrack";
export default {
    components: {
        dialogList
    },
    props:{
        aiCueType:{
            type: [String,Number],
            default: ''
        },
        moreConfigProps: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            hintTime: null
        };
    },
    computed: {
        getLevel (){
            return this.$store.getters.getPackageLevel
        }
    },
    watch: {
    },
    methods: {
        onClick(){
            trackEvent('P003_A0061')
            if(this.getLevel<20){
                if(this.hintTime){return}
                clearTimeout(this.hintTime);
                this.hintTime = setTimeout(()=>{;
                    this.hintTime = null
                }, 3000);
                this.$message.success('自建问题仅限企业版及以上会员版本使用');
                return
            }
            this.$refs.dialogList.show()
        },
        onSelect(data){
            this.$emit('sendCustomPrompt',{
                data,
                next:()=>{
                    this.$refs.dialogList.hide()
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
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
</style>

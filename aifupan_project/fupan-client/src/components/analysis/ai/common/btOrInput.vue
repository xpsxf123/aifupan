<template>
    <div class="w100">
        <div v-if="shareStatus" class="w100 pd-6">
                <el-row class="share-btn-box">
                    <el-col :span="8" class="pd-l30 flex-ai-c">
                        <el-checkbox class="share-checkbox-all" v-model="isAllShare" @change="allChange">
                            <span>全选</span>
                        </el-checkbox>
                    </el-col>
                    <el-col :span="8" class="flex-ai-c">
                        <afp-button  @click="imgShare">导出分享</afp-button>
                        <afp-button type="primary"  @click="copyUrlShare">复制链接 </afp-button>
                    </el-col>
                    <el-col :span="8" class="pd-r30 flex-jc-e flex-ai-c">
                        <el-button type="text" @click="exitHandler">取消</el-button>
                    </el-col>
                </el-row>
            </div>
        <div v-else class="w100 text-center flex-ji-c" style="min-height:38px">
            <mainBt class="mg-t12 mg-b12 q-icon" :isAnimation="isAnimation" :isType="isInput" v-show="!getShowInput" @click="isInput = true"></mainBt>
            <inputField
                ref="inputField"
                v-show="getShowInput"
                @hide="isInput = false"
                :load="load"
                :identity="identityList"
                :extra="additionalList"
                :notIdentity="notIdentity"
                :notExtra="notExtra"
                :showModelType="showModelType"
                :modelTypeProps="modelTypeProps"
                :showKnowledgeBase="showKnowledgeBase"
                :moreConfigValue="moreConfigValue"
                :moreConfigProps="moreConfigProps"
                @model-change="$emit('model-change',$event)"
                @model-option-change="$emit('model-option-change',$event)"
                @knowledge-click="$emit('knowledge-click')"
                @more-config-change="$emit('more-config-change',$event)"
                @submit="onSubmit"
            ></inputField>
        </div>
    </div>
</template>

<script>
import myUtils from '/src/utils/utils';
import mainBt from './btn/mainBt.vue';
import inputField from './input/inputField.vue';
export default {
    components: {
        mainBt,
        inputField
    },
    props:{
        notIdentity: {
            type: Boolean,
            default: false
        },
        notExtra: {
            type: Boolean,
            default: false
        },
        load: {
            type: Boolean,
            default: false
        },
        shareStatus: {
            type: Boolean,
            default: false
        },
        els:{
            type: Array,
            default: () => []
        },
        showModelType: {
            type: Boolean,
            default: false
        },
        modelTypeProps: {
            type: Object,
            default: () => ({})
        },
        showKnowledgeBase: {
            type: Boolean,
            default: false
        },
        moreConfigValue: {
            type: Object,
            default: () => ({})
        },
        moreConfigProps: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            showInput: true,
            isInput: true,
            isAnimation: false,
            throttle: myUtils.throttle(1000,1),
            identityList: [],
            additionalList: [],
        };
    },
    computed: {
        getShowInput(){
            return this.showInput
        },
        isAllShare:{
            get(){
                return this.els?.filter(d=>{
                    return d.el==='aiCustom'
                })?.every(d=>{
                    return d.otherOption.selectCode
                }) || false
            },
            set(){
                
            }
        }
    },
    watch: {
        isInput:{
            handler(v){
                if(!v){
                    this.showInput = false;
                    return;
                }
                this.isAnimation = true
                this.throttle(()=>{
                    this.showInput = v;
                    this.isAnimation = false;
                });
            }
        }
    },
    methods: {
        allChange(v){
            this.$nextTick(()=>{
                if(v){
                    this.$emit('share','all');
                }else{
                    this.$emit('share','notAll');
                }
            })
        },
        exitHandler(){
            this.$nextTick(()=>{
                this.$emit('share','exit')
            })
        },
        imgShare(){
            this.$emit('share','img')
        },
        copyUrlShare(){
            this.$emit('share','url');
        }, 
        onSubmit(val){
            this.$emit('submit',val);
        },
        clear(){
            this.$refs.inputField.clear()
        },
        getOptions(){
            this.$httpBack.v2200?.identityAndAdditionalList().then(res=>{
                const {identityList,additionalList} = res?.data;
                this.identityList = identityList
                this.additionalList = additionalList;
            })
        }
    },
    created() {
        
    },
    mounted() {
        this.$nextTick(()=>{
            this.getOptions();
        })
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
.share-btn-box{
    .el-col{
        height: 40px;
    }
}
.q-icon{
    position: absolute;
   bottom: 0;
}
.share-checkbox-all{
    ::v-deep(.el-checkbox__input){
        margin-top: -1.5px;
    }
    ::v-deep(.el-checkbox__label){
        padding-left: 2px;
    }
}
</style>

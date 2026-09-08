<template>
    <div  v-if="selectedModel !== undefined" class="model-type-box flex-ji-c" :class="{'model-type-box--inline': isInlineVariant}">
        <el-popover
            placement="bottom"
            width="300"
            :trigger="getTrigger"
            popper-class="model-type-popper"
            >
            <div  slot="reference" class="model-type-content flex-ji-c font-s14">
                <template v-if="isInlineVariant">
                    <span>{{ getModelObjcet.name }} &gt;</span>
                </template>
                <template v-else>
                    <span>切换AI模型：</span>
                    <img :src="getModelObjcet.img" style="max-width: 20px;" class="mg-l4 mg-r4">
                    <span>{{ getModelObjcet.name }}</span>
                    <span class="mg-l4">
                        <i class="el-icon-arrow-down"></i>
                    </span>
                </template>
            </div>
            <div class="model-type-item-box">
                <div v-for="(item,index) in getModelItems" @click="changeModel(item.value)" :key="index" class="cs-p model-type-item  brs-4" :class="{'is-select': selectedModel == item.value}">
                    <div class="font-s14 text-colorMain">{{ item.name }}</div>
                    <div class="font-s12 pd-t4" v-html="item.content"></div>
                </div>
            </div>
        </el-popover>

        <dialog-box
            :visible.sync="violationDialogVisible"
            class="violation-model-dialog"
            :close-on-click-modal="false"
            width="700px"
            title=""
            @close="closeViolationModelDialog"
        >
            <div class="violation-model-dialog__content">
                <div class="violation-model-dialog__title">AI违规助手按照您近期的风控强度情况分配模型</div>
                <div class="violation-model-dialog__list">
                    <div
                        v-for="item in getViolationDialogItems"
                        :key="item.value"
                        class="violation-model-dialog__item cs-p"
                        :class="{ 'is-active': pendingViolationModel === item.value }"
                        @click="selectViolationModel(item.value)"
                    >
                        <div class="violation-model-dialog__tag">{{ item.tag }}</div>
                        <div class="violation-model-dialog__desc font-s14">
                            <span v-html="item.highlight"></span><br>
                            <span v-html="item.desc"></span>
                        </div>
                        <div class="violation-model-dialog__footer  font-s14">
                            <span v-html="item.actionText"></span>
                            <span class="violation-model-dialog__checkbox">
                                <i v-if="pendingViolationModel === item.value" class="el-icon-check"></i>
                            </span>
                        </div>
                    </div>
                </div>
                <div class="violation-model-dialog__btn">
                    <afp-button
                        round
                        type="primary"
                        :disabled="!canSubmitViolationModel"
                        @click="confirmViolationModel"
                    >
                        点我开始分析
                    </afp-button>
                </div>
            </div>
        </dialog-box>
    </div>
</template>

<script>
import doubao from '@/assets/imgs/dbAI.png';
import deepseek from '@/assets/imgs/deepseek.png';
import aiTypeMixin from '@/mixins/aiTypeMixin';
import DialogBox from '/src/components/dialog';
export default {
    components: {
        DialogBox
    },
    props:{
        value: {
            type: Number,
        },
        variant: {
            type: String,
            default: 'pill'
        },
        notChangeModel: {
            type: Boolean,
            default: false
        },
        type:{
            type: String,
            default: ''
        },
        isCompare:{
            type: Boolean,
            default: false
        },
        showViolationDialogOnEnter: {
            type: Boolean,
            default: false
        }
    },
    mixins:[aiTypeMixin],
    data() {
        return {
            violationDialogVisible: false,
            pendingViolationModel: undefined,
            // 使用的ai模型 0:doubai pro 1:deepseek-r1, 2:doubao 思考, 3:doubao 综合
            // modelMap: {
            //     0: {
            //         name: '豆包 PRO',
            //         img: doubao
            //     },
            //     1: {
            //         name: 'deepseek R1',
            //         img: deepseek
            //     },
            //     2: {
            //         name: '豆包 思考',
            //         img: doubao
            //     },
            //     3: {
            //         name: '豆包 综合',
            //         img: doubao
            //     },
            //     4: {
            //         name: 'Doubao 综合（新）',
            //         img: doubao
            //     },
            //     5: {
            //         name: '运营高手2.0（内测）',
            //         img: doubao
            //     },
            //     6: {
            //         name: '运营专家2.0（内测）',
            //         img: doubao
            //     }
            // },
            modelMap:{},
            modelItems:[],
            // modelItems: [
            //     {
            //         name: 'Deepseek R1',
            //         content: `<div>本模型在<span class="text-colorTheme">归纳总结、推理思考</span>上有突出表现</div>`,
            //         value: 1,
            //     },
            //     {
            //         name: 'Doubao Pro',
            //         content: `<div>本模型在<span class="text-colorTheme">总结归纳、话术脚本</span>上有突出表现</div>`,
            //         value: 0,
            //     },
            //     {
            //         name: 'Doubao 综合',
            //         content: `<div>Dou bao综合 <span class="text-colorTheme">综合分析、话术能力较强</span></div>`,
            //         value: 3,
            //     },
            //     {
            //         name: 'Doubao 思考',
            //         content: `<div>本模型擅长 <span class="text-colorTheme">归纳总结</span>、<span class="text-colorTheme">话术脚本</span> 和 <span class="text-colorTheme">直播运营</span></div>`,
            //         value: 2,
            //     },
            //     {
            //         name: 'Doubao 综合（新）',
            //         content: `<div>Dou bao综合（新） <span class="text-colorTheme">综合分析、话术能力较强</span></div>`,
            //         value: 4,
            //     },
            //     {
            //         name: '运营高手2.0（内测）',
            //         content: `<div>运营高手2.0（内测）<span class="text-colorTheme">擅长复杂运营分析、数据分析，分析速度较快</span></div>`,
            //         value: 5,
            //     },
            //     {
            //         name: '运营专家2.0（内测）',
            //         content: `<div>运营专家2.0（内测）<span class="text-colorTheme">擅长复杂运营体系搭建，适合顶尖运营使用，消耗算力是普通模型三倍</span></div>`,
            //         value: 6,
            //     }
            // ],
            modelitemsMap:{},
            modelObjcet: {},
            selectedModel: typeof this.value === 'number' ? this.value : undefined,
        };
    },
    computed: {
        isInlineVariant(){
            return this.variant === 'inline'
        },
        getTrigger(){
            return this.isInlineVariant ? 'click' : 'hover'
        },
        getAiTypeMap(){
            if(this.isCompare){
                return [1,3,4,5,6]
            }
            let aiTypeMap = {
                0:[1,2,3,4,5,6],
                1:[ 
                    1,
                    {value: 0, name: '豆包宽松', content: `<div>本模型在<span class="text-colorTheme">风控不严</span>的时候使用</div>`},
                    {value: 5, name: '豆包严格', content: `<div>本模型在<span class="text-colorTheme">风控较严</span>的时候使用</div>`},
                    // {value: 5, name: '豆包综合2.0-lite', content: `<div>豆包综合2.0-lite <span class="text-colorTheme">综合分析、话术能力较强</span></div>`},
                    // {value: 6, name: '豆包综合2.0-pro', content: `<div>豆包综合2.0-pro <span class="text-colorTheme">综合分析、话术能力较强</span></div>`},
                ],
                2:[1,3,4],
                6:[2,3,4]
            }
            return aiTypeMap[this.getCueType] || [0,1,2,3,4];
        },
        getModelMap(){
            // let objAry = Object.keys(this.modelMap)?.map(key=>{
            //     let data = this.getAiTypeMap?.find(aiData=> key == aiData?.value) || {};
            //     return [ key, {
            //         ...this.modelMap[key],
            //         ...data
            //     }]
            // });
            let objAry = this.modelItems.map(d=>{
                return [ d.value, {
                    ...d
                }]
            });
            return Object.fromEntries(objAry);
        },
         getModelObjcet(){
            return this.getModelMap[this.selectedModel] || {};
        },
        getModelItems(){
            return this.modelItems;
            // let models = this.getAiTypeMap?.map(d=>d?.value ?? d) || [];
            // return this.modelItems.map(d=>{
            //     let data = this.getAiTypeMap?.find(aiData=>d?.value === aiData?.value) || {};
            //     return {
            //         ...d,
            //         ...data
            //     }
            // }).filter(d=>{
            //     return models.includes(d.value);
            // });
        },
        getViolationDialogItems() {
            let violationDialogMap = {
                'strict': {
                    tag: '近期',
                    highlight: '<span class="text-colorErr">频繁违规</span>、多个直播间违规、平台<span class="text-colorErr">风控严格</span>!',
                    desc: '甚至有封禁直播间的情况。',
                    actionText: '选择<span class="text-colorTheme">豆包严格</span>模型'
                },
                'loose': {
                    tag: '近期',
                    highlight: '<span class="text-colorErr">偶尔违规</span>、个别直播间违规、平台<span class="text-colorErr">风控宽松</span>。',
                    desc: '没有封禁直播间的情况。',
                    actionText: '选择<span class="text-colorTheme">豆包宽松</span>模型'
                }
            };
            return this.modelItems?.filter(d=>!!d.standard)?.map(obj=>{
                let value = obj.value;
                let currentModel = this.getModelMap[value] || this.modelMap[value] || {};
                return {
                    value,
                    name: currentModel.name,
                    ...violationDialogMap[obj?.standard]
                };
            });
        },
        canSubmitViolationModel() {
            return typeof this.pendingViolationModel === 'number';
        }
    },
    watch: {
        value(val){
            if(typeof val !== 'number'){
                return
            }
            if(val === this.selectedModel){
                return
            }
            if(this.modelItems?.some(d=>d.value === val)){
                this.selectedModel = val
            }
        }
    },
    methods: {
        changeModel(val){
            this.selectedModel = val;
            this.$emit('input',val);
            this.$emit('change',val);
            this.getChangeModelData();
        },
        setSelectedModel(val){
            this.selectedModel = val;
        },
        selectViolationModel(val){
            this.pendingViolationModel = val;
        },
        openViolationModelDialog(){
            // this.pendingViolationModel = this.getViolationDialogItems.some(d=>d.value === this.selectedModel) ? this.selectedModel : this.getModelItems[0]?.value;
            this.violationDialogVisible = true;
        },
        closeViolationModelDialog(){
            this.violationDialogVisible = false;
        },
        confirmViolationModel(){
            if(!this.canSubmitViolationModel){
                return;
            }
            this.changeModel(this.pendingViolationModel);
            this.closeViolationModelDialog();
        },
        getChangeModelData(){
            this.$nextTick(()=>{
                this.$emit('change-model', this.modelItems?.find(d=>d.value === this.selectedModel));
            })
        },
        getModelItemsDatas(){
            this.$httpBack.dictdata.dictDataTreeListByCode().then(res=>{
                this.modelitemsMap = res?.data || {};
                if(this.isCompare){
                    this.modelObjcet = res?.data.find(d=>d?.value === '0-1') || {}
                }else{
                    this.modelObjcet = res?.data.find(d=>d?.value == this.getCueType) || {};
                }
                
                let defaultSelectedModel = undefined
                this.modelItems = this.modelObjcet?.children?.map(d=>{
                    const val = JSON.parse(d.value);
                    if(val.defaultSelect == 1 && defaultSelectedModel === undefined){
                        defaultSelectedModel = val.aiModel
                    }
                    return {
                        ...val,
                        img: val?.modelAvatarCode === 'doubao'? doubao : deepseek,
                        name: val.modelName || val.modelCode || val.title,
                        content: val.subtitle,
                        value: val.aiModel
                    }
                }) || [];

                const externalValue = typeof this.value === 'number' ? this.value : undefined
                const hasExternalValue = externalValue !== undefined && this.modelItems.some(d=>d.value === externalValue)
                const hasCurrentValue = typeof this.selectedModel === 'number' && this.modelItems.some(d=>d.value === this.selectedModel)

                if(!hasCurrentValue){
                    if(hasExternalValue){
                        this.selectedModel = externalValue
                    }else if(defaultSelectedModel !== undefined){
                        this.selectedModel = defaultSelectedModel
                    }else{
                        this.selectedModel = this.modelItems[0]?.value || undefined
                    }
                }
                this.$nextTick(()=>{
                    this.getChangeModelData();
                })
            })
        },
        isShowViolationDialogOnEnter(){
            this.$nextTick(()=>{
                if(this.showViolationDialogOnEnter){
                    if(typeof this.value === 'number' && this.modelItems?.some(d=>d.value === this.value)){
                        return
                    }
                    this.openViolationModelDialog();
                }
            })
        }
        // dialogSubmit(){
        //     this.$emit('input',this.selectedModel === 0 ? 1 : 0);
        //     this.$emit('change', this.selectedModel === 0 ? 1 : 0)
        //     this.$refs.aiDialog.hide();
        // },
    },
    created() {
        this.getModelItemsDatas();
    },
    mounted() {
        this.isShowViolationDialogOnEnter();
        // this.getChangeModelData();
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
        this.isShowViolationDialogOnEnter()
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.model-type-box{
    background: linear-gradient( 107deg, #E5EFFF 0%, #F4D3FF 100%);
    box-shadow: 1px 2px 6px 0px rgba(26,25,25,0.09);
    border-radius: 44px;
    padding: 4px 12px;
    
}
.model-type-box--inline{
    background: transparent;
    box-shadow: none;
    border-radius: 0;
    padding: 0;
    color: var(--color-main);
}
.model-type-content{
    cursor: pointer;
}
.model-type-box--inline{
    .model-type-content,
    .model-type-content *{
        cursor: pointer;
    }
}
.model-type-item-box{
    .model-type-item{
        padding: 5px;
    }
}
.is-select{
    background: #F4F1FF;
}
.violation-model-dialog{
    ::v-deep(.el-dialog){
        border-radius: 8px;
        overflow: hidden;
        .el-dialog__header{
            padding: 20px;
            background: #EEF2F7;
        }
        .el-dialog__body{
            padding: 20px 24px 28px;
        }
    }
}
.violation-model-dialog__content{
    text-align: center;
}
.violation-model-dialog__title{
    padding-bottom: 20px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
}
.violation-model-dialog__list{
    display: flex;
    justify-content: space-between;
    gap: 12px;
}
.violation-model-dialog__item{
    flex: 1;
    min-height: 122px;
    padding: 16px;
    border: 1px solid #DFE6F0;
    border-radius: 4px;
    text-align: left;
    transition: all .2s ease;
}
.violation-model-dialog__item.is-active{
    border-color: var(--color-main);
    box-shadow: 0px 4px 12px 0px rgba(127,86,217,0.12);
}
.violation-model-dialog__tag{
    font-size: 14px;
    color: #606266;
}
.violation-model-dialog__desc{
    min-height: 52px;
    padding-top: 8px;
    line-height: 22px;
}
.violation-model-dialog__footer{
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 16px;
    font-size: 14px;
    color: #606266;
}
.violation-model-dialog__checkbox{
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 14px;
    height: 14px;
    border: 1px solid #C0C4CC;
    border-radius: 2px;
    color: var(--color-main);
    font-size: 12px;
}
.violation-model-dialog__item.is-active .violation-model-dialog__checkbox{
    border-color: var(--color-main);
    background: #F4F1FF;
}
.violation-model-dialog__btn{
    padding-top: 28px;
}

</style>
<style>
.model-type-popper{
    padding: 8px !important;
    border-radius: 8px;
}
</style>

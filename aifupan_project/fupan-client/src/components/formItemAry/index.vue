<template>
    <div class="form-item-ary">
        <div v-for="(item, index) in arrayData" :key="index" class="flex-jc-sb mg-b10">
            <div class="flex-ji-c pd-r10">
                <slot :data="item" :index="index">
                    <component :is="getModelType" :key="index"></component>
                </slot>
            </div>
            <div class="flex-ji-c form-item-ary-btn">
                <afp-button circle :plain="true" v-if="arrayData.length < max" type="primary" size="small" icon="el-icon-plus" @click="addClick(index)"></afp-button>
                <afp-button circle :plain="true" v-if="arrayData.length > min" type="danger" size="small" icon="el-icon-minus" @click="delClick(index)"></afp-button>
            </div>
        </div>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        value: {
            type: Array,
            default: ()=>{return []}
        },
        min: {
            type: Number,
            default: 0
        },
        max: {
            type: Number,
            default: 0
        },
        dataType: {
            type: String,
            default: 'string', //string, number,boolean,object,array
        },
        formType: {
            type: String,
            default: '',
        }
    },
    data() {
        return {
            arrayData: []   
        };
    },
    computed: {
        getModelType(){
            switch(this.formType){}
            return 'el-input'
        }
    },
    watch: {
        value: {
            handler(newVal, oldVal){
                if(newVal.length !== this.arrayData.length){
                    this.initData();
                }
            },
            deep: true
        }
    },
    methods: {
        addClick(index){
            this.addData(index)
        },
        delClick(index){
            this.delData(index)
        },
        addData(index){
            console.log(this.arrayData.length, this.max)
            if(this.arrayData.length >= this.max){
                this.delData();
                return;
            }
            let dataModel = null;
            switch(this.dataType){
                case 'string':
                    dataModel = '';
                    break;
                case 'number':
                    dataModel = 0;
                    break;
                case 'boolean':
                    dataModel = false;
                    break;
                case 'object':
                    dataModel = {};
                    break;
                case 'array':
                    dataModel = [];
                    break;
            }
            
             if(typeof index === 'undefined'){
                this.arrayData.splice(index, 0, dataModel);
            }else{
                this.arrayData.push(dataModel);
            }
            this.$emit('input', this.arrayData);
        },
        delData(index){
            
            if(this.arrayData.length <= this.min){
                return;
            }
            if(typeof index === 'undefined'){
                this.arrayData.pop();
            }else{
                this.arrayData.splice(index, 1);
            }
            console.log(JSON.parse(JSON.stringify(this.arrayData)))
            this.$emit('input', this.arrayData);
        },
        initData() {
            this.$set(this,'arrayData', this.value);
            
            for(let i = this.arrayData.length; i < this.min; i++){
               this.addData();
            }
        },
        
    },
    created() {
        
    },
    mounted() {
        this.initData();
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
.form-item-ary-btn{
    ::v-deep(.el-button.el-button--primary){
        background: #fff !important;
        color: var(--color-main) !important;
    }
}
</style>
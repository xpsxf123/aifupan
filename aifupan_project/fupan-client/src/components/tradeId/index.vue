<template>
    <div style="display: inline-block;">
        <el-cascader popper-class="select-trade-id" v-model="$attrs.value" :show-all-levels="showAllLevels" :disabled="disabled" :options="options" style="width: 100%;"
            :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name', emitPath: false }" filterable :size="size" :clearable="clearable"
            :placeholder="placeholder"  @change="changeTradeHandle" ref="tradeCascader">
            <template slot-scope="{ node, data }">
                <span @click="expandChange(node)">{{ data.name }}</span>
            </template>
        </el-cascader>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        placeholder: {
            type: String,
            default: '选择或者搜索输入一个行业，以提高分析准确性'
        },
        options: {
            type: Array,
            default: ()=>{
                return [];
            }
        },
        disabled: {
            type:Boolean,
            default: false
        },
        notInit: {
            type: Boolean,
            default: false
        },
        emitPath: {
            type: Boolean,
            default: true
        },
        notExpandChange: {
            type: Boolean,
            default: false
        },
        showAllLevels: {
            type: Boolean,
            default: true
        },
        clearable: {
            type: Boolean,
            default: false
        },
        size: {
            type: String,
            default: 'default' // medium / small / mini
        }
    },
    data() {
        return {
            // tradeId: []
        };
    },
    computed: {
        // getValue:{
        //     get(){
        //         if(this.tradeId?.length){
        //             return this.tradeId
        //         }else if(this.$attrs.value){
        //             this.$set(this,'tradeId',[this.$attrs.value]);
        //         }
        //         return this.tradeId
        //     },
        //     set(v){
        //         this.tradeId = v;
        //         if(v.length){
        //             this.$emit('input',v[v.length-1]);
        //         }
        //     }
        // }
    },
    watch: {},
    methods: {
        expandChange(data){
            let d = data.path || data;
            if(this.notExpandChange){
                if(!data?.children?.length){
                    this.changeTradeHandle(d[d.length-1], true);
                }
                return
            }
            if(d.length){
                this.changeTradeHandle(d[d.length-1], true);
            }
            if(!data.children.length){
                this.dropDownVisible();
            }
        },
        changeTradeHandle(val, notDrop) {
            if (this.clearable || val) {
                this.$emit('input', val)
            }
            this.$emit('change', val)
            if(notDrop){return};
            this.dropDownVisible();
        },
        dropDownVisible(){
            this.$nextTick(()=>{
                // 关闭级联列表下拉
                this.$refs.tradeCascader.dropDownVisible = false;
            });
        },
        initData(){
            this.$nextTick(()=>{
                // if(!this.notInit){
                //     this.getValue = [];
                //     return
                // }
                // if(this.tradeId?.length){
                //     this.$emit('input',this.tradeId[this.tradeId.length-1]);
                // }
            })
        }
    },
    created() {
        this.initData();
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
        this.initData();
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>
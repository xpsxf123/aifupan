<template>
    <div>
        <el-cascader ref="cascader" popper-class="word-class-cascader" v-model="$attrs.value" 
            @input="onInput"
            style="width: 100%;"
            :options="options"
            :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name', emitPath: false }">
        </el-cascader>
    </div>
</template>

<script>

export default {
    components: {},
    model: {
        prop: 'value',
        event: 'input'
    },
    props:{
    },
    data() {
        return {
            options: []
        };
    },
    computed: {},
    watch: {},
    methods: {
        getTreeList(){
            this.$httpBack.cruxtype.listTree().then(res=>{
                if(res?.code === 0){
                    res.data?.forEach(d1=>{
                        d1.disabled = true;
                        d1.children?.forEach(d2=>{
                            d2.disabled = true;
                        })
                    })
                    this.options = res.data
                }
            })
        },
        onInput(val){
            this.$emit('input', val);
            this.$nextTick(()=>{
                this.$refs.cascader.dropDownVisible = false
            })
        }
    },
    created() {
        
    },
    mounted() {
        this.$nextTick(()=>{
            this.getTreeList()
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
<style lang='scss'>
.word-class-cascader{
    .el-radio.is-disabled{
        display: none !important;
    }
}
</style>
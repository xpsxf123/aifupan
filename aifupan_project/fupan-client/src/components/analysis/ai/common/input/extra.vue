<template>
    <popoverBt text="输出格式" :select="!!checkedCities?.length">
        <template #title>
            <div class="font-s16 pd-b6 text-colorMain">输出格式:<span class="font-s12 mg-l4 text-color3">(可多选)</span></div>
        </template>
        <div class="extra-checkbox-box">
            <el-checkbox-group v-model="checkedCities" @change="onChange">
                <el-checkbox v-for="item in extras" :label="item.value" :key="item.value">{{item.label}}</el-checkbox>
            </el-checkbox-group>
        </div>
    </popoverBt>
</template>

<script>
import popoverBt from './popoverBt.vue';
export default {
    components: {
        popoverBt
    },
    props:{
        list: {
            type:Array,
            default: () => []
        }
    },
    data() {
        return {
            checkedCities: [],
            extras: []
        };
    },
    computed: {},
    watch: {
        list: {
            handler(val) {
                if(val.length>0){
                    this.setItems(val)
                }
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        onChange(){
            this.$emit('input', this.checkedCities?.map(d=>{
                return this.extras[d]?.label
            }));
        },
        setItems(items){
            this.extras = items?.map((d,i)=>{
                return {
                    label: d,
                    value: `${i}`,
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
.extra-checkbox-box{
    max-height: 300px;
    overflow: hidden;
    overflow-y: auto;
    ::v-deep(.el-checkbox){
        display: block;
        margin: 5px 0;
        font-size: 12px;
    }
}
</style>

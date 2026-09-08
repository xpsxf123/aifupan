<template>
    <popoverBt text="身份设定" :select="!!select" :width="500">
        <template #title>
            <div class="font-s16 pd-b6 text-colorMain">身份设定：</div>
        </template>
        <div class="radio-group-box">
            <el-radio-group v-model="select" @change="onChange">
                <el-radio v-for="item in items" :label="item.value" :key="item.value">{{item.label}}</el-radio>
            </el-radio-group>
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
            select: '',
            items: []
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
            this.$emit('input', this.items?.find(d=>d.value===this.select)?.label)
        },
        setItems(items){
            this.items = items?.map((d,i)=>{
                return {
                    label: d,
                    value: `${i}`,
                }
            });
            this.select = this.items[0].value
            this.onChange();
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
.radio-group-box{
    max-height: 300px;
    overflow: hidden;
    overflow-y: auto;
    ::v-deep(.el-radio-group){
        width: 100%;
        .el-radio{
            display: block;
            margin: 5px 0;
            font-size: 12px;
            padding: 5px;
            display: flex;
            align-items: center;
            .el-radio__input{
                visibility: hidden;
                opacity: 0;
                width: 0;
            }
            .el-radio__label{
                padding: 0;
            }
        }
        .el-radio.is-checked{
            background: #EFF0F4;
            
            border-radius: 5px;
            .el-radio__label{
                color: #484A4D;
                font-size: 14px;
            }
        }
    }
}
</style>
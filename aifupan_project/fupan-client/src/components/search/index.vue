<template>
    <div class="flex-jc-sb flex-ai-c w100 items-center">
        <div class="flex-ai-c flex-wp-w w100 flex-1">
            <el-form class="search-form-box flex-ai-c flex-wp-w w100" 
            @keyup.enter.native="handleSubmit"
            size="default"
            :inline="true" 
            :model="searchData">
                <slot name="form">
                    <slot name="form-before"></slot>
                    <el-form-item v-for="(formItem, index) in searchConfig?.items || []" :key="formItem.prop"
                        :label="formItem.label" :prop="formItem.prop">
                        <el-input v-if="!formItem.temp" v-model="searchData[formItem.prop]"
                            size="default"
                            class="input-gray input-border-none"
                            :placeholder="formItem.placeholder || `输入${formItem.label || '内容'}搜索`"
                            @change="onChange(formItem.on)"
                            @input="onInput(formItem.on)"
                            :style="formItem?.config?.style || {}"
                            clearable></el-input>
                        <component v-else :is="formItem.temp" v-model="searchData[formItem.prop]"
                            v-bind="{...formItem.config}" :style="formItem?.config?.style || {}" @change="onChange(formItem.on)" @input="onInput(formItem.on)"></component>
                    </el-form-item>
                    <slot name="form-after"></slot>
                </slot>
                <el-form-item>
                    <div class="flex-jc-c flex-ai-c search-btn-box">
                        <afp-button @click="search" size="default">查找</afp-button>
                        <slot name="btns"></slot>
                    </div>
                </el-form-item>
            </el-form>
        </div>
        <div class="flex items-center">
            <div v-if="$slots.searchRight" class="search-right-box">
                <slot name="searchRight"></slot>
            </div>
            <div class="pd-l12">
                <slot name="ref-btn"></slot>
            </div>
        </div>
    </div>
</template>

<script>
import Select from './../form/Select.vue'
import DatePicker from './../form/DatePicker.vue'
import Radio from '../form/Radio.vue'
import RadioGroup from '../form/RadioGroup.vue'
import Input from '../form/Input.vue'
export default {
    components: {Select,DatePicker,Radio,RadioGroup,Input},
    props: {
        searchConfig: {
            type: Object,
            default: () => { return {} }
        },
        searchData: {
            type: Object,
            default: () => { return {} }
        },
        size: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
        };
    },
    computed: {},
    watch: {},
    methods: {
        getSearchData(){
            return JSON.parse(JSON.stringify({
                ...this.searchData,
            }))
        },
        search() {
            this.$emit('search',this.getSearchData())
        },
        updatedData(data){
            this.$set(this,'formData',data);
        },
        init(){
            this.$nextTick(()=>{
                this.searchConfig?.items.forEach(d=>{
                    if(typeof d.config?.default !=='undefined'){
                        this.$set(this.searchData,d.prop,d?.config?.default);
                    }
                })
                this.$emit('updatedSearch', this.searchData);
            })
        },
        onChange(on){
            if(typeof on?.change === 'function'){
                on?.change(this.getSearchData())
            }else if(on?.change){
                this.search()
            }
        },
        onInput(on){
            if(typeof on?.input === 'function'){
                on?.input(this.getSearchData())
            }else if(on?.input){
                this.search()
            }
        },
        handleSubmit(event){
            if (event.key === 'Enter') {
                this.search();
            }
        }
    },
    created() {
        this.init()
    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { 
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.search-right-box {
    display: flex;
    flex-direction: row-reverse;
}
.search-form-box{
    margin: -5px;
    .el-form-item{
        margin: 5px;
    }
}
</style>
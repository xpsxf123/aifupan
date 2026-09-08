<template>
         <!--v-if="getItems?.length > 1"  -->
    <el-popover
        v-if="getItems?.length > 1"
        placement="top-start"
        title="历史分析"
        trigger="hover">
        <div slot="title"></div>
        <div style="margin-top: -7px;">
            <div class="radio-group-box">
                <el-radio-group v-model="select" @change="onChange">
                    <el-radio v-for="item in getItems" :label="item.value" :key="item.value">
                        <div style="max-width: 200px;" class="slh">{{item.label}}</div>
                    </el-radio>
                </el-radio-group>
            </div>
        </div>
        <afp-button slot="reference" 
        class="pd-0 font-s12"
         style="width: 42px;height: 42px;padding: 0;"
         type="primary"
         circle>
         历史
        </afp-button>
    </el-popover>
</template>

<script>

export default {
    components: {},
    props:{
        type: {
            type:String,
            default: ''
        },
        id: {
            type:String,
            default: ''
        }
    },
    data() {
        return {
            select: 'all',
            itemsMap:{
                assistant: [],
                violation: []
            },
            // 延迟选中code
            delayCode: ''
        };
    },
    computed: {
        getItems(){
            return this.itemsMap[this.type]
        }
    },
    watch: {
        type: {
            handler(val) {
                this.select = 'all';
            },
            immediate: true
        }
    },
    methods: {
        // 只做选中数据变更
        onlySelect(code){
            if(code === 0) {
                code = 'all'
            }
            this.select = code;
        },
        onChange(){
            this.onSelect();
        },
        setItems(items,type){
            this.itemsMap[type || this.type] = items;
            this.saveOtherSessionStorage();
        },
        addItems(items,type){
            this.itemsMap[type || this.type]?.push(...items);
            this.saveOtherSessionStorage();
        },
        addItem(item,type){
            this.itemsMap[type || this.type]?.push(item);
            this.select = item.value;
            this.onSelect(item);
            this.saveOtherSessionStorage();
        },
        // 选中code
        selectCode(code){
            if(!this.itemsMap[this.type]?.length){
                this.delayCode = code;
                return
            }
            if(!(code)){return}
            // 加载code
            this.select = code;
            this.onSelect();
            this.saveOtherSessionStorage();
        },
        // 延迟加载code
        selectDelayCode(){
            if(!this.delayCode){return}
            // 加载code
            this.select = this.delayCode;
            this.delayCode = '';
            this.onSelect();
            this.saveOtherSessionStorage();
        },
        onSelect(item){
            this.$nextTick(()=>{
                this.$emit('click', item || this.getItems?.find(item=>item.value === this.select));
            });
        },
        saveOtherSessionStorage(){
            localStorage.setItem(`${this.id}_${this.type}_itemsLen`, this.itemsMap[this.type]?.length);
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
        this.itemsMap={
            assistant:[],
            violation: []
        };
        this.select = 'all';
    }, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.radio-group-box{
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
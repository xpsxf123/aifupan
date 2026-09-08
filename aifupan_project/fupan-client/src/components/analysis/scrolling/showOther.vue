<template>
    <el-popover
        placement="bottom-start"
        width="200"
        trigger="hover">
        <div>
            <div v-for="item in items">
                <el-checkbox v-model="getData[item.prop]" :true-label="1" :false-label="0">{{item.label}}</el-checkbox>
            </div>
        </div>
        <el-tag  slot="reference" class="mg-r10" style="border-radius: 28px" size="medium" :effect="isAll?'dark':'plain'">
            输入条件
        </el-tag>
        <!-- <afp-button slot="reference" class="mg-r10" :plain="!isAll"  type="primary" ></afp-button> -->
    </el-popover>
</template>

<script>

export default {
    components: {},
    props:{
        value: {
            type: [Object,undefined],
            default: undefined
        }
    },
    data() {
        return {
            /*
                dataDisplay:{}
                弹幕助手中的数据展示
                dateTime
                时间(0不勾，1勾)
                nickName
                昵称(0不勾，1勾)
                level
                用户等级(0不勾，1勾)
                fansLevel
                粉丝团等级(0不勾，1勾)
                isNew
                新icon(0不勾，1勾)
            */
            dataDisplay: {},
            items:[
                {label: '昵称',prop: 'nickName'},
                {label: '时间',prop: 'dateTime', default: 0},
                {label: '用户等级',prop: 'level', default: 0},
                {label: '粉丝团等级',prop: 'fansLevel'},
                {label: '新用户',prop: 'isNew'},
            ]
        };
    },
    computed: {
        getData:{
            get(){
                return this.value || this.dataDisplay
            },
            set(val){
                this.$set(this,'dataDisplay',val);
                this.$emit('input',val);
                this.$emit('change',val);
            }
        },
        isAll(){
            return Object.values(this.dataDisplay)?.every(item=>!!item);
        }
    },
    watch: {},
    methods: {
        initData(){
            this.$nextTick(()=>{
                let o = {};
                this.items?.forEach(item => {
                    o[item.prop] = item.disabled?0:item.default ?? 1;
                });
                this.getData = o;
            })
        }
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

</style>
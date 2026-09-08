<template>
    <div>
        <tradeId v-model="$attrs.value" ref="trade" :size="size" :options="getTree" :showAllLevels="false" class="tradeCascader" notExpandChange :disabled="!isSelectTrade" placeholder=" " @change="onChange"></tradeId>
    </div>
    
</template>

<script>
import tradeId from './../tradeId/index.vue';
export default {
    components: { tradeId },
    name: "",
    props: {
        treeList: {
            type: Array,
            default: () => {
                return []
            }
        },
        notLoad:{
            type:Boolean,
            default: false
        },
        size: {
            type: String,
            default: 'default' // medium / small / mini
        },
        isSelectTrade: {
            type:Boolean,
            default: false
        }
    },
    computed: {
        getTree(){
            return this.treeList.length ? this.treeList : this.tradeTreeList
        }
    },
    data() {
        return {
            // treeIds: [],
            tradeTreeList: []
        };
    },
    mounted() {
        this.$nextTick(()=>{
            if(this.notLoad || this.treeList.length){return}
            this.getTradeTreeList();
        })
    },
    created() {
        
    },
    methods: {
        // 获取行业列表树形
        getTradeTreeList() {
            this.tradeTreeList = [];
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data;
                    this.$emit('tree-load');
                }
            });
        },
        dropDown() {
            // this.$refs.trade.dropDownVisible();
        },
        onChange(val) {
            // this.$emit('input', this.$attrs.value);
            this.$emit('input',val);
            this.$emit('tree-list-change', val)
        }
    }

};
</script>

<style lang="scss" scoped>
.tradeCascader{
    ::v-deep(.el-input){
        width: 100%;
    }
}
</style>
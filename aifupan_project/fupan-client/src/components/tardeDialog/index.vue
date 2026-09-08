<template>
    <!-- 行业弹窗 -->
    <el-dialog title="行业选择" :visible.sync="dialogVisible" width="600px" :close-on-click-modal="false">
        <tradeId v-model="tradeId" :options="treeList" style="width: 100%;" @chage="changeTradeHandle"></tradeId>
        <span slot="footer" class="dialog-footer">
            <afp-button @click="hide" size="medium" style="padding-inline: 18px">取消</afp-button>
            <afp-button size="medium" type="primary" :plain="false" @click="confirm" style="padding-inline: 18px">确定</afp-button>
        </span>
    </el-dialog>
</template>

<script>
import DialogMixin from '@/mixins/dialog';
import tradeId from '@/components/tradeId/index.vue'
export default {
    components: {
        tradeId
    },
    mixins: [DialogMixin],
    props:{
        treeList: {
            type: Array,
            default: ()=>{return []}
        }
    },
    data() {
        return {
            tradeId: '1'
        };
    },
    computed: {},
    watch: {},
    methods: {
        changeTradeHandle(val){
            this.$emit('change',val)
        },
        confirm(){
            this.$emit('confirm',{
                id: this.tradeId || '1',
                data: this.dialogData,
                callback:()=>{
                    this.hide()
                }
            })
        },
        // 显示回调（默认dialog的mixin会在show函数内部调用）
        showCallback(option){
            const {id} = option;
            this.tradeId = id;
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

</style>
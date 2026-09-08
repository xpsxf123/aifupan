<template>
    <!-- 分页 -->
    <div class="flex-ji-c flex-wp-w w100">
        <el-pagination  class="flex-ji-c flex-wp-w w100" style="text-align: center;" 
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle" 
        :current-page="getPageIndex"
        :page-sizes="pageSizes" 
        :page-size="getPageSize"
        :total="total"
        :layout="$isMobile?'sizes, jumper, ->, total':layout">
        </el-pagination>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        layout: {
            type:String,
            default: 'total, sizes, prev, pager, next, jumper'
        },
        pageSizes: {
            type:Array,
            default: ()=>{
                return [10, 20, 50, 100]
            }
        },
        total: {
            type:Number,
            default: 0
        },
        pageSize:{
            type:Number,
            default: -1
        },
        pageIndex:{
            type:Number,
            default: -1
        }
    },
    data() {
        return {
            pIndex: 1,
            pSize: 0
        };
    },
    computed: {
        getPageSize:{
            get(){
                if(this.pageSize>=0){
                    return this.pageSize;
                }else{
                    return this.pSize;
                }
            },
            set(val){
                this.pSize = val;
                this.$emit('size-input', val)
            }
        },
        getPageIndex:{
            get(){
                if(this.pageIndex>=0){
                    return this.pageIndex;
                }else{
                    return this.pIndex;
                }
            },
            set(val){
                this.pIndex= val;
                this.$emit('current-input',val)
            }
        }
    },
    watch: {},
    methods: {
        currentChangeHandle(pageIndex){
            this.getPageIndex = pageIndex;
            this.$emit('current-change',pageIndex)
        },
        sizeChangeHandle(pageSize){
            this.getPageSize = pageSize;
            this.$emit('size-change', pageSize)
        }
    },
    created() {
        
    },
    mounted() {
        this.$nextTick(()=>{
            if(this.getPageSize<=0){
                this.getPageSize = this.pageSizes?.[0] || 10;
            }
            if(this.getPageIndex<=0){
                this.getPageIndex = 1;
            }
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
<style lang='scss' scoped>

</style>
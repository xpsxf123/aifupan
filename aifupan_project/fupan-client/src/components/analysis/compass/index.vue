<template>
    <el-row style="align-items: stretch;display: flex">
        <el-col :span="isTable?isContrast?18:16:24" class="pd-l16 pd-t16 pd-r16 main-bg">
            <div class="text-right pd-b16">
                <el-radio-group v-model="radio" size="small">
                    <el-radio-button label="1" id="compass-chat-dom">柱状图</el-radio-button>
                    <el-radio-button label="2">表格</el-radio-button>
                </el-radio-group>
            </div>
            <CompassChat class="main-bg" v-if="!isTable" :datas="datas" :compareData="compareData"></CompassChat>
            <CompassTable v-if="isTable" :isContrast="isContrast" type="vertical" :compareData="compareData" :tableDatas="tableDatas"></CompassTable>
        </el-col>
        <el-col v-show="isTable" :span="isContrast?6:8" class="pd-l16">
            <CompassModel :id="id" :type="type" :tradeId="tradeId" :datas="datas" @compareData="changeCompareData"></CompassModel>
        </el-col>
    </el-row>   
</template>
<script>
import CompassTable from './table.vue'
import CompassChat from './chat.vue'
import CompassModel from './model.vue'
export default {
    components: {
        CompassTable,
        CompassChat,
        CompassModel
    },
    props:{
        // 判断是否是对比分析
        isContrast: {
            type: Boolean,
            default: false
        },
        // 雷达图参数
        type: {
            type: [String,Number],
            default: ''
        },
        // 雷达图id
        id: {
            type: String,
            default: ''
        },
        cruxTypeList: {
            type:Array,
            default: () => {
                return []
            }
        },
        tradeId: {
            type: [Number,String],
            default: ''
        }
    },
    data() {
        return {
            radio: '2',
            compareData: {},
            compareMap: {}
        };
    },
    computed: {
        isTable(){
            return this.radio === '2'
        },
        isOnly(){
            return this.cruxTypeList?.length === 1;
        },
        datas(){
            let ids = null;
            let ary = this.cruxTypeList?.map((item,i)=>{
                let ds = item.filter(d=>{
                    return d?.cruxTypeInfoVo?.isShowCompass
                });
                if(!ids){
                    ids = ds?.map(d=>d?.cruxTypeInfoVo?.id);
                }
                return {
                    name: this.isOnly?'我的直播间':`直播间${i+1}`,
                    ids,
                    value: ids.map(id=>{
                        let o = ds?.find(d=>d?.cruxTypeInfoVo?.id === id);
                        return parseFloat((o?.scale * 100).toFixed(1));
                    }),
                    type: `${i}`
                }
            });
            if(this.compareData?.name){
                ary.push({
                    name: this.compareData?.name,
                    ids,
                    value: ids.map(id=>{
                        let o = this.compareData?.modelItemList?.find(d=>d?.cruxTypeId === id);
                        return parseFloat((o?.cruxTypeScale * 100).toFixed(1));
                    }),
                    type: 'compare'
                });
                // 调整顺序
                this.compareData.modelItemList = ids?.map(id=>{
                    return this.compareData?.modelItemList?.find(d=>d?.cruxTypeId === id);
                })
            };
            return ary;
        },
        tableDatas(){
            let treeData = {};
            this.cruxTypeList?.forEach(list=>{
                list?.forEach(item=>{
                    let d = item.cruxTypeInfoVo;
                    if(typeof treeData[d.id] === 'undefined'){
                        treeData[d.id] = {
                            ...item,
                            ...d,
                            label: d.name,
                            datas: []
                        }
                    }
                    if(typeof treeData[d.id]?.data1 === 'undefined'){
                        treeData[d.id].data1 = parseFloat((item?.scale * 100).toFixed(1));
                    }else if(typeof treeData[d.id]?.data2 === 'undefined'){
                        treeData[d.id].data2 = parseFloat((item?.scale * 100).toFixed(1));
                    }
                    if(d.parentId != '0'){
                        // 阻止重复设置数据
                        if(treeData[d.parentId].datas?.some(o=>o.id === d.id)){return}
                        treeData[d.parentId].datas.push(treeData[d.id]);
                    }
                })
            })
            return Object.values(treeData)?.filter(d=>d.parentId == '0');
        }
    },
    watch: {},
    methods: {
        changeCompareData(data){
            this.compareData = data;
            let compareMap = {};
            data?.modelItemList?.forEach(d=>{
                compareMap[d.cruxTypeId] = d;
            });
            this.compareData.compareMap = compareMap;
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
<template>
    <div style="height: 100%;">
        <div class="text-center pd-t16 pd-b16">
            <el-radio-group id="compass-model-dom" class="model-radio-group-box" v-model="radio" size="small" @change="onChange">
                <el-radio-button v-for="(item, index) in modelDatas" :key="index" :label="item.value">
                    <span style="display: block;max-width: 60px;" class="slh">{{ item.label }}</span>
                </el-radio-button>
            </el-radio-group>
        </div>
        <div style="height: 100%;" v-if="radarShow">
            <!-- 雷达图 -->
            <radarMap ref="radarMap" :datas="datas" :compareData="compareData"></radarMap>
        </div>
    </div>
</template>

<script>
import radarMap from './radarMap.vue'
export default {
    components: { radarMap },
    props: {
        id: {
            type: String,
            default: ''
        },
        type: {
            type: [String, Number],
            default: ''
        },
        datas: {
            type: Array,
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
            radio: '',
            modelDatas: [],
            compareData: {},
            radarShow: false,
            oldTradeId: ''
        };
    },
    computed: {},
    watch: {},
    methods: {
        getModelData() {
            this.radarShow = false;
            // oldTradeId 不等于 tradeId，则表示刷新了行业。需要重新请求模型。
            // this.radarShow = this.tradeId === this.oldTradeId
            // if(this.radarShow){
            //     return
            // };
            // // 储存老模型数据
            // this.oldTradeId = this.tradeId;
            setTimeout(() => {
                this.$httpBack.openapi.v2000listTradeModel({ uuid: this.id, type: parseInt(this.type) }).then((res) => {
                    if (res.code === 0) {
                        this.radarShow = true
                        this.modelDatas = res.data?.filter(d => !!d?.modelItemList?.length).map((item, index) => {
                            return {
                                ...item,
                                label: item.alias || item.name,
                                name: item.alias || item.name,
                                value: item.id
                            }
                        })
                        let find = this.modelDatas?.find(d => d?.defaultShow === 1) || this.modelDatas[0];
                        this.radio = find.value;
                        this.selectCompareData(find);
                    }
                })
            }, 200)
        },
        onChange(val) {
            this.selectCompareData(this.modelDatas.find(item => item.value === val))
            this.$nextTick(() => {
                this.$refs?.radarMap?.setOption();
            });
        },
        selectCompareData(data) {
            this.compareData = data;
            this.$emit('compareData', data)
        }
    },
    created() {
    },
    mounted() {
        this.$nextTick(() => {
            this.getModelData();
        })
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() {
        this.$nextTick(()=>{
            this.getModelData();
        })
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.model-radio-group-box {
    ::v-deep(.el-radio-button__inner) {
        padding: 7px;}
}
</style>
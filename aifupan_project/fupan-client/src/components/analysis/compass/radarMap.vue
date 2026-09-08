<template>
    <div class="main" ref="main"></div>
</template>

<script>
import * as echarts from 'echarts';

export default {
    components: {},
    props: {
        datas: {
            type: Array,
            default: () => {
                return []
            }
        },
        compareData: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {

        return {
            option: {
                tooltip: {},
                title: {},
                legend: {
                    orient: 'vertical',
                    left: 'left',
                    top: 0
                },
                radar: {
                    radius: '70%', // 半径为容器的60%
                    center: ['60%', '50%'], // 中心位置在容器的正中央
                    splitNumber: 4, // 分成5个圈
                    indicator: [],
                },
                series: []
            },
            color: ['#0052D9', '#F5BA18'],
            compareColor: '#F88262',
            myChart: null
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        init() {
            this.$nextTick(() => {
                if(!this.$refs?.main){return};
                this.myChart = echarts?.init(this.$refs?.main);
                this.setOption()
            })
        },
        // 处理数据展示tooltip信息
        buildSeries(dataIndex, datas, indicator) {
            const data = datas[dataIndex];
            const item = data?.value;
            const hepler = item?.map((vItem, vIndex) => {
                const arr = new Array(item?.length);
                arr.splice(vIndex, 1, vItem);
                return arr;
            })
            return [item, ...hepler].map((o, index) => {
                return {
                    type: 'radar',
                    symbolSize: 5,
                    name: data?.name,
                    symbol: index === 0 ? 'circle' : 'none',
                    itemStyle: {
                        color: index === 0 ? data.color : 'transparent'
                    },
                    lineStyle: {
                        color: index === 0 ? data.color : 'transparent',
                        width: 1
                    },
                    areaStyle: {
                        color: index === 0 ? data.color : 'transparent',
                        //设置区域背景颜色透明度
                        opacity: 0.1,
                    },
                    tooltip: {
                        show: index === 0 ? false : true,
                        formatter: function () {
                            let res = indicator[index - 1].name + '：<br>';
                            for (let x of datas) {
                                res += '<span class="radar-map-label mg-r6" style="background: ' + x.color + ';"></span>' + x.name + '：' + x.value[index - 1] + '%<br>';
                            }
                            return res;
                        }
                    },
                    z: index === 0 ? 1 : 2,
                    data: [o]
                }
            })
        },
        // 设置雷达配置数据
        setOption() {
            
            let maxs = new Array(this.compareData?.modelItemList?.length);
            this.datas?.forEach((d)=>{
                d?.value?.forEach((v,i)=>{
                    if(!Array.isArray(maxs[i])){
                        maxs[i] = []
                    }
                    maxs[i].push(v);
                })
            })
            maxs = maxs?.map(d=>{
                let max = Math.max(...d);
                return max+(max*0.1);
            })
            // 过滤获取雷达各点名称
            let indicator = this.compareData?.modelItemList?.map((d,index)=>{
                return {
                    name: d.cruxTypeName,
                    max: maxs[index]>100?100:Math.ceil(maxs[index])
                }
            });
            // 默认雷达图数据
            let datas= this.datas?.map((d,i)=>{
                return {
                    ...d,
                    color: d.type === 'compare' ? this.compareColor : this.color[d.type],
                }
            });

            const series = [];
            for (let i in datas) {
                series.push(...this.buildSeries(i, datas, indicator))
            }

            this.option.radar.indicator = indicator;
            this.option.series = series;
            this.option.legend.data = datas.map(o => o.name);
            this.myChart.setOption(this.option);
        }
    },
    created() {

    },
    mounted() {
        setTimeout(() => {
            this.init();
        }, 1000)
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.main {
    min-height: 300px;
    width: 100%;
}

::v-deep(.radar-map-label) {
    width: 8px;
    height: 3px;
    display: inline-block;
    vertical-align: middle;
    margin-top: -2px;
}
</style>
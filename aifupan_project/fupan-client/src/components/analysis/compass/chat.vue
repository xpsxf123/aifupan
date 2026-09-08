<template>
    <div ref="main" style="height: 240px;width: 100%;"></div>
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
        },
        isPopover: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            load:false,
            myChart: null,
            color: ['#6881AB', '#549AB4'],
            compareColor: '#EC8775'
        };
    },
    computed: {
        getxAxisNams(){
            if(this.isPopover){
                return this.datas.map(item=>item.name);
            }else{
                return this.compareData?.modelItemList?.map(item => {
                    return item.cruxTypeName
                })
            }
        },
        
    },
    watch: {},
    methods: {
        init() {
            this.$nextTick(()=>{
                this.drawerChat();
            })
        },
        drawerChat(){
            this.myChart = echarts.init(this.$refs.main);
            this.setOption(this.datas);
        },
        getServerData(datas){
            if(this.isPopover){
                let {data1,data2} = this.datas[0];


                let dataAry = [data1,data2];
                let names = dataAry?.filter(d=>typeof d !=='undefined')?.map((d,i)=>{
                    return {
                        name: `直播间${i+1}`,
                        color: this.color[i],
                        data: this.datas?.map(d=>{
                            return parseFloat(d?.[`data${i +1}`])
                        })
                    };
                })
                names.push({
                    name: this.compareData?.name,
                    color:this.compareColor,
                    data: this.datas?.map(d=>{
                        let n = this.compareData?.compareMap[d.id]?.cruxTypeScale * 100;
                        return parseFloat(n.toFixed(1))
                    })
                });
                return names?.map(nameItem=>{
                    return {
                        type: 'bar',
                        barGap: 0,
                        emphasis: {
                            focus: 'series'
                        },
                        ...nameItem,
                    }
                });
            }else{
                return datas.map((item,index) => {
                    return {
                        name: item.name,
                        type: 'bar',
                        barGap: 0,
                        color: item.type === "compare"? this.compareColor : this.color[index],
                        emphasis: {
                            focus: 'series'
                        },
                        data: item.value?.map(d=>parseFloat(d))
                    }
                })
            }
        },
        setOption(datas) {
            var option;
            let seriesData = this.getServerData(datas);
            option = {
                tooltip: {
                    trigger: 'axis',
                    formatter:function(params) {
                        let itemStr = params?.map(item=>{
                            return `<div class="flex-jc-sb"><span>${item.marker}<b>${item.seriesName}</b>：</span><span><b>${item.value}%</b></span></div>`
                        })
                        return `
                            <b>${params[0].name}</b><br>
                            ${itemStr.join('')}
                        `
                    }
                },
                legend: {
                    bottom: '5',
                    itemWidth: 10,
                    itemHeight: 10,
                    textStyle: {
                        fontSize: 12
                    },
                },
                xAxis: [
                    {
                        type: 'category',
                        // axisTick: { show: false },
                        boundaryGap: [0, 0.01],
                        data: this.getxAxisNams,
                    }
                ],
                yAxis: [{
                    type: 'value',
                    name: '百分比',
                    max: 100,
                    series: [{
                        data: [0, 20, 40, 60, 80, 100],
                        type: 'line'
                    }],
                    nameTextStyle: {
                        padding: [0, 0, 0, -50],
                        color: "#909499",
                        align: 'center',
                        fontSize:12
                    }, 
                    axisLabel: {
                        formatter: '{value}%', // 格式化为百分比
                        textStyle: {
                            show:true,
                            color: "#909499",
                            fontSize:12
                        },                           
                    },
                }],
                grid: {
                    top:'15%',
                    left: '5%',
                    right: '5%',
                    bottom: '15%',
                    containLabel: true
                },
                series: seriesData
            };
            
            option && this.myChart.setOption(option);
        }
    },
    created() {
        setTimeout(()=>{
            this.init();
        },300)
    },
    mounted() {

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
// div{
//     display: flex;

// }
</style>
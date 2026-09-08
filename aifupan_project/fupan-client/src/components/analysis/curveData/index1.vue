<template>
    <div style="height: 300px;width: 100%;" class="curve-box main-bg">
        <div v-if="chartData" class="curve-data-item flex-ai-c text-center">
            <!-- <div>
                <div class="curve-data-item-title text-color4 font-s12">本段进场人数</div>
                <div class="text-colorMain">{{ getTotalViewersNum || 0 }}</div>
            </div> -->
            <div>
                <div class="font-s12 text-color4">峰值在线</div>
                <div class="text-colorMain">{{ getMaxOnlineNum || 0 }}</div>
            </div>
            <!-- <div>
                <div class="font-s12 text-color4">弹幕总数</div>
                <div class="text-colorMain">{{ chartData.totalBarrageNum || 0 }}</div>
            </div> -->
        </div>
        <div v-if="chartData" ref="main" style="height: 300px;width: 100%;"></div>
        <div v-else class="flex-jc-c h100 flex-ai-c">
            <!-- <p class="mg-0 h100 ">未采集到数据...</p> -->
            <img src="@/assets/imgs/chartEmpty.png" style="max-height: 160px" alt="" srcset="">
        </div>
    </div>
</template>

<script>
import * as echarts from 'echarts';
import myUtils from '/src/utils/utils';
export default {
    components: {},
    props: {
        videoId: {
            type: String,
            default: ''
        },
        isWebOnline: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            myChart: null,
            chartData: {}
        };
    },
    computed: {
        getTotalViewersNum(){
            return myUtils.numberToSting(this.chartData.totalViewersNum);
        },
        getMaxOnlineNum(){
            return myUtils.numberToSting(this.chartData.maxOnlineNum);
        }
       
    },
    watch: {
        videoId: {
            handler(val, oldVal) {
                if(val !== oldVal && val){
                    this.getDataAndDraw(val);
                }
            },
            immediate: true
        }
    },
    methods: {
        initChart() {
            var myChart = echarts.init(this.$refs.main);
            this.myChart = myChart;
            const {approachDataList} = this.chartData || {};
            const then = this;
            // 监听点击事件
            myChart.getZr().on('click', function (params) {
                // 获取像素坐标点
                const pointInPixel = [params.offsetX, params.offsetY]
                const { target, topTarget } = params
               // 使用 convertFromPixel方法 转换像素坐标值到逻辑坐标系上的点。获取点击位置对应的x轴数据的索引         值，借助于索引值的获取到其它的信息
                // 转换X轴坐标
                let pointInGrid = myChart.convertFromPixel({ seriesIndex: 0 }, pointInPixel);
                // 转换Y轴坐标
                // let pointInGrid2 = myChart.convertFromPixel({ seriesIndex: 1 }, pointInPixel);
                // x轴数据的索引值
                // 所点击点的X轴坐标点所在X轴data的下标
                let xIndex = pointInGrid[0];
                // 所点击点的Y轴坐标点数值
                // let yIndex = pointInGrid2[1];
                // 使用getOption() 获取图表的option
                let op = myChart.getOption();
                //获取到x轴的索引值和option之后，我们就可以获取我们需要的任意数据。
                // 点击点的X轴对应坐标的名称
                var time = approachDataList[xIndex]?.dateTime;
                // 点击点的series -- data对应的值
                // var value = op.series?.map(d=>{
                //     return d.data[xIndex]
                // });
                if(time){
                    then.$emit('playerReadied',time)
                }
            });
        },
        setOption() {
            var option;
            // 进场人数折线数据 approachDataList
            // 在线人数折线数据 onlineDataList
            const {approachDataList, onlineDataList} = this.chartData || {};
            const xData = onlineDataList?.map(d=>myUtils.toTimeFormatDate(d.dateTime));
            // const approachDatas = approachDataList?.map(d=>d.valueNum);
            const onlineDatas = onlineDataList?.map(d=>d.valueNum);

            option = {
                color: ['#16CC96', '#FFD480'],
                title: {
                },
                tooltip: {
                    trigger: 'axis'
                },
                legend: {
                    top: 25,
                    left: '4%',
                    itemWidth: 10,
                    itemHeight: 4,
                    textStyle: {
                        fontSize: 12
                    },
                    icon:'rect',
                    data: [
                        '在线人数', 
                        // '进场人数'
                    ]
                },
                grid: {
                    top:"60px",
                    left: '4%',
                    right: '4%',
                    bottom: '5%',
                    containLabel: true
                },
                toolbox: {
                },
                xAxis: {
                    type: 'category',
                    axisLabel: {
                        interval: 'auto',
                        // formatter: function (value) {
                        //     // 自定义日期格式化
                        //     return ;
                        // }
                    },
                    boundaryGap: false,
                    data: xData
                },
                yAxis: {
                    type: 'value',
                    boundaryGap: [0,0.1],
                    axisLabel: {
                        formatter: function (value) {
                            let v = parseInt(value);
                            return myUtils.numberToSting(v);
                        }
                    }
                },
                series: [
                    {
                        name: '在线人数',
                        type: 'line',
                        areaStyle: {
                            opacity: 0.1,
                        },
                        showSymbol: false,
                        lineStyle: {
                            width: 1
                        },
                        // stack: 'Total',
                        data: onlineDatas
                    },
                    // {
                    //     name: '进场人数',
                    //     type: 'line',
                    //     areaStyle: {
                    //         opacity: 0.1,
                    //     },
                    //     lineStyle: {
                    //         width: 1
                    //     },
                    //     showSymbol: false,
                    //     // stack: 'Total',
                    //     data: approachDatas
                    // },
                ]
            };
            option && this.myChart.setOption(option);
        },
        getData(videoId) {
            let httpRequest = this.isWebOnline ? this.$httpBack.v2100.onlineChartData : this.$httpClient.anchorvideo.onlineChartData;
            return httpRequest({videoId}).then(res=>{
                if(res.code === 0){
                    this.chartData = res.data;
                }
                return res
            })
        },
        getDataAndDraw(videoId) {
            this.$nextTick(()=>{
                // this.initChart();
                // return
                this.getData(videoId).then((res)=>{
                    if(res.code === 0){
                        if(!this.chartData){return}
                        this.initChart();
                        this.setOption();
                    }
                })
            })
        },
    },
    created() {
        
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
.curve-box{
    position: relative;
}
.curve-data-item{
    position: absolute;
    right: 20px;
    top: 10px;
    >div{
        padding: 0 6px;
        .text-color4{
            font-weight: normal;
        }
    }
}
</style>

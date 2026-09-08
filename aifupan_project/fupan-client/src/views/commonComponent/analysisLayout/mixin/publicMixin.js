export default {
    data(){
        return {
            cruxTypeMap: null
        }
    },
    methods: {
        // 获取关键词类型列表
        async getCruxWordType(callback,type) {
            let isInit = callback === 'init' || type === 'init';
            // 如果已经存在类型，并且类型并不需要初始化的情况，不需要请求接口
            if(this.cruxTypeMap && !isInit){
                if(typeof callback === 'function'){
                    callback(this.cruxTypeMap);
                }
                return
            };
            await this.$httpBack.dictdata.list({ limit: -1, typeLogo: "crux_words_type" }).then((res)=>{
                if (res.code == 0) {
                    let cruxTypeMap = Object.fromEntries(res.data.list?.map(d => {
                        return [d.value, d]
                    }));
                    this.cruxTypeMap = cruxTypeMap;
                    if(typeof callback === 'function'){
                        callback(cruxTypeMap);
                    }
                }
            });
        },
        initPayerIndexMap(){
            window.payerAllIndexMap = {};
        },
        // 设置视频分段index索引
        setPayerIndexMap(list = [],name) {
            if(list.length === 0){return}
            // 取所有结束时间
            let endTimes = list?.map(i => Math.ceil(i.endTime / 1000));
            // 取所有时间高度生成对应数组
            let timeLength = Array.from({ length: Math.ceil(list[list?.length - 1]?.endTime / 1000) }, (x, i) => i);
            let Index = 0;
            // 生成三维数组
            let timeArrs = timeLength.map((i) => {
                // 获取当前结束时间判断段落
                let time = endTimes[Index];
                // 如果当前结束时间小于当前时间则跳入下一个段落
                if (i > time) {
                    Index = Index + 1;
                }
                return [i, Index];
            })
            let allMap = Object.fromEntries(timeArrs);
            // 数据不需要双向绑定，不进行vue数据劫持处理，过于庞大劫持处理困难
            window.payerAllIndexMap[name||'default'] = allMap;
            // 创建对象索引
            // this.$set(this,'payerAllIndexMap', allMap)
        }
    }
}
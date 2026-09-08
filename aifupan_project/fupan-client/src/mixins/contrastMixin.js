import ReviewContrast from '@/components/reviewContrast'
import {cloneDeep} from "lodash";

export default {
    components: {ReviewContrast},
    props:{
    },
    data() {
        return {
            contrastMap: {}
        };
    },
    computed: {
        contrastList(){
            return Object.values(this.contrastMap)
        }
    },
    watch: {
    },
    methods: {
         // 加入对比
         addContrast(data) {
            if (Object.keys(this.contrastMap)?.length >= 2) {
                this.$message.error("仅支持两个对比");
                return;
            }
            this.$set(this.contrastMap, data.videoId, data);

             this.$nextTick(() => {
                 if (Object.keys(this.contrastMap)?.length === 2) {
                     this.determineVideoContrast().then(r => {
                         this.$refs.review_contrast?.open?.(this.contrastList);
                     })
                 }
             })
        },
        // 取消对比
        cancelContrast(data) {
            if (this.contrastMap[data.videoId]) {
                this.$delete(this.contrastMap, data.videoId);
            }
            this.getTableHeight?.()
        },
        //弹窗对比提示
        modalContrast(contrastParams){
            this.determineVideoContrast().then(r => {
                this.$refs.review_contrast?.open?.(this.contrastList);
            })
        },
        swapObj() {
            const obj = cloneDeep(this.contrastMap)
            this.contrastMap = Object.fromEntries(
                Object.entries(obj).reverse()
            );
        },
        async determineVideoContrast() {
            const [videoOneId, videoTwoId] = Object.keys(this.contrastMap)
            try {
                const result = await this.$httpBack.contrast.determineVideoContrast({
                    videoOneId, videoTwoId
                })
                if (result.code !== 0) return
                const {optimizeVideoId, benchmarkVideoId} = result.data;//优化场次/参考场次

                this.contrastMap = Object.fromEntries([
                    ...[optimizeVideoId, benchmarkVideoId].filter(k => k in this.contrastMap),
                    ...Object.keys(this.contrastMap).filter(k => ![optimizeVideoId, benchmarkVideoId].includes(k))
                ].map(k => [k, this.contrastMap[k]]))

            } catch (e) {

            }
        },
        // 提交对比
        contrastSubmit(contrastParams) {
            let cMap = this.contrastList;
            if (!cMap || cMap.length != 2) {
                this.$message.error("请选择两个进行对比");
                return;
            }
            let requestData = {
                videoOneId: cMap[0].videoId,
                videoTwoId: cMap[1].videoId,
                anchorOneId: cMap[0].secUid,
                anchorTwoId: cMap[1].secUid,
                sliceContrastType: 0,
                contrastType:0,
                syncScene: cMap[0].secUid === cMap[1].secUid ? 3 : 2
            }
            if (this.replayType === 'replaySection' || this.type === 'slice') {
                requestData.contrastType = 1
                requestData.sliceContrastType = 1
            }

            if(typeof this.contrastParams === 'function' || typeof contrastParams === 'function'){
                requestData = contrastParams?contrastParams(requestData, cMap) : this.contrastParams(requestData, cMap);
            }
            let http =  null;
            if(typeof this.contrastSubmitHttp === 'function'){
                http = this.contrastSubmitHttp(requestData, cMap);
            }else{
                http = this.$httpBack.contrast.clientAddContrast(requestData);
            }
            return http.then(res => {
                if (res.code == 0) {
                    this.getTableHeight?.()
                    this.contrastMap = {};
                    if(typeof this.contrastSubmitCallback === 'function'){
                        let o = this.contrastSubmitCallback();
                        // 如果回调函数返回真，则阻止emit事件
                        if(o){
                            return res;
                        }
                    }
                    this.$emit('contrast');
                }
                return res
            })

            // return this.$httpBack.contrast.clientAddContrast(requestData).then(res => {
            //     if (res.code === 0) {
            //         this.contrastMap = {};
            //         if(typeof this.contrastSubmitCallback === 'function'){
            //             let o = this.contrastSubmitCallback();
            //             // 如果回调函数返回真，则阻止emit事件
            //             if(o){
            //                 return res;
            //             }
            //         }
            //         this.$emit('contrast');
            //     }
            //     return res
            // })
        },
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
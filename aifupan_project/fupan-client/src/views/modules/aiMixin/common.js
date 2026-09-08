import tableMixin from '@/mixins/table.js'
import Anchor from '@/components/anchor/index.vue';
import drawerList from './drawerList.vue';
import commonHttp from '@/mixins/commonHttp';
import myUtils from '@/utils/utils.js';
export default (type)=>{
    return {
        mixins: [tableMixin,commonHttp],
        components: {
            Anchor,
            drawerList
        },
        inject: ['appVnode'],
        props:{
            
        },
        data() {
            return {
                httpList: this.$httpBack.clientaifav.getTableList,
                httpDel: this.$httpBack.clientaifav.batchDelete,
                httpSave: this.$httpBack.clientaifav.saveOrUpdate,
                formConfig: {
                    items: type !== 'file' ? [{
                        label: '行业筛选', prop: 'tradeId',
                        temp: 'Select',
                        config: {
                            default: '',
                            options: [{
                                id: '',
                                name: '全部'
                            }],
                            prop: {
                                label: 'name',
                                value: 'id'
                            },
                            formmater (item) {
                                return typeof item.anchorNum === 'undefined' ? item.name : `${item.name}(${item.anchorNum})`
                            },
                            style: {
                                width: '170px'
                            }
                        }
                    }, {
                        label: '直播间搜索', prop: 'secUidArr',
                        temp: 'Select',
                        config: {
                            default: [],
                            multiple: true,
                            filterable: true,
                            collapseTags: true,
                            options: [],
                            prop: {
                                label: 'anchorName',
                                value: 'secUid'
                            },
                            style: {
                                width: '260px'
                            }
                        },
                    }] : [{
                        label: '文件搜索', prop: 'fileName',
                    }],
                },
                menuConfig: {
                    width: '305px',
                    options: [
                        {
                            label: '查看AI分析',
                            click:(item)=>{
                                this.toAiInfo(item)
                            },
                            icon: 'icon-a-bukechakan2'
                        },
                        {
                            label:"删除",
                            type: 'danger',
                            click:(item)=>{
                                this.deletes({
                                    list: [item]
                                })
                            }
                        }
                    ]
                },
                type: type || 'video',
                emptyMsg: '暂无数据，请点击上方“添加AI分析场次”，选择您需要通过AI分析的直播场次'
            };
        },
        computed: {
            AiThokenNum(){
                const { totalAiTokenNum = 0, useAiTokenNum = 0} = this.$store.getters.getUserproperty;
                return `AI分析量: ${myUtils.numberToSting(useAiTokenNum)}/${myUtils.numberToSting(totalAiTokenNum)}字`;
            }
        },
        watch: {},
        methods: {
            getList(){
                this.$refs.table.getList()
            },
            // 添加ai分析抽屉
            addAI(){
                this.$refs.drawerList.show()
            },
            toAiInfo(row){
                // console.log(this.typeConfig,'-----',row, this.$route)
                /*
                favType 收藏类型 0：运营助手 1：违规助手
                dataResourceType 数据来源类型 0：录制视频 1：文件上传 2：对比
                contrastType 对比类型 0：视频对比 1：文件对比
                */
                const {contrastType, dataResourceType, favType } = this.typeConfig;
                let url = {
                    0: 'aiVideo',
                    1: 'aiFile',
                    2: 'aiContrast'
                };
                let id = {
                    0: row?.videoInfo?.videoId,
                    1: row?.fileInfo?.fileId,
                    2: row?.contrastInfo?.contrastId,
                };
                let o = {};
                if(dataResourceType !== 2){
                    o = {
                        fileType: dataResourceType
                    }
                }
                this.$router.push({
                    path: this.$route.fullPath + '/' + url[dataResourceType],
                    query: {
                        [dataResourceType === 2?'contrastId':'id']: id[dataResourceType],
                        type: favType === 0 ? 'assistant' : 'violation',
                        ...o
                    }
                })
                
            },
            initList(){
                if(this.type === 'file'){
                    this.initFileList();
                }else{
                    this.initVideoList();
                }
                this.appVnode?.getUserproperty();
            },
            async initFileList(){
                // let anchor = await this.getListByAnchor();
                // this.setFormConfigDic({0:anchor},this.formConfig,{
                //     tradeId:(opts,form)=>{
                //         return [].concat(form.config?.options?.shift(),opts);
                //     }
                // })
            },
            async initVideoList(){
                let cList = await this.getCompereBackList();
                let anchor = await this.getListByAnchor();
                this.setFormConfigDic({0:anchor,1:cList?.map(d=>d.anchorInfo)},this.formConfig,{
                    tradeId:(opts,form)=>{
                        return [].concat(form.config?.options?.shift(),opts);
                    }
                })
            },
            deleteOpt(list){
                return {
                    http: this.httpDel,
                    idKey: 'id',
                    callback:()=>{
                        this.getList();
                    }
                }
            }
        },
        created() {
        
        },
        mounted() {
            this.initList();
        },
        beforeCreate() {}, //生命周期 - 创建之前
        beforeMount() {}, //生命周期 - 挂载之前
        beforeUpdate() {}, //生命周期 - 更新之前
        updated() {}, //生命周期 - 更新之后
        beforeDestroy() {}, //生命周期 - 销毁之前
        destroyed() {}, //生命周期 - 销毁完成
        activated() {
            this.initList();
        }, 
    }
}
<template>
    <div class="common-bg" style="margin: -16px;height: calc(100% + 32px);">
        <div class="version-items main-bg flex-jc-s flex-ai-c pd-16">
            <div class="flex-ai-c">
                <span>套餐版本：</span>
                <img v-if="getUserInfo.logoImgAddress" :src="getUserInfo.logoImgAddress"
                style="width: 59px">
            </div>
            <div>
                <span>有限期至：</span>
                <span>{{ getUserInfo?.expirationDate?.substring(0, 10) }}</span>
            </div>
            <div>
                <span>下次资源更新时间：</span>
                <span>{{ resourceUpdateTime }}</span> 
            </div>
            <div>
                <afp-button @click="updateVersion">
                    {{ getUserInfo.packageLevel == 0?'立即升级': '立即续费' }}
                </afp-button>
            </div>
        </div>

        <div class="pd-t16">
            <Table class="home-table" :data="getTableData" :column="packageConfig">
                <template #setMenuTotalQuantity="{row, column}">
                    <span slot="reference">
                        <span v-if="row.commodityTypeReset" class="text-colorTheme">{{ row.setMenuRemUseQuantity }}{{ row.commodityTypeUnit }}</span>
                        <span v-if="row.commodityTypeReset">/</span>
                        <span >{{ row.setMenuTotalQuantity }}{{ row.commodityTypeUnit }}</span>
                    </span>
                    <!-- <el-popover
                        placement="top"
                        width="500"
                        :disabled="!row?.setMenuList?.length"
                        trigger="hover">
                        <span slot="reference">
                            <span v-if="row.commodityTypeReset" class="text-colorTheme">{{ row.setMenuUseQuantity }}{{ row.commodityTypeUnit }}</span>
                            <span v-if="row.commodityTypeReset">/</span>
                            <span >{{ row.setMenuTotalQuantity }}{{ row.commodityTypeUnit }}</span>
                        </span>
                        <div>
                            <div class="pd-b6 font-s12">{{ row.commodityTypeName }}-{{ column.label }}</div>
                            <div v-for="(item,index) in row?.setMenuList" :key="index" class="flex-ai-c pd-b6 font-s12">
                                <span class="pd-r10">
                                    <span v-if="row.commodityTypeReset" class="text-colorTheme">{{ item.useNumber }}{{item.commodityTypeUnit}}</span>
                                    <span v-if="row.commodityTypeReset">/</span>
                                    {{ item.totalNumber }}{{item.commodityTypeUnit}}</span>
                                <span class="pd-r10">有效期至：{{ item.endTime }}</span>
                            </div>
                        </div>
                    </el-popover> -->
                </template>
                <template #incTotalQuantity="{row, column}">
                    <el-popover
                        placement="top"
                        width="400"
                        :disabled="!row?.incPackageList?.length"
                        trigger="hover">
                        <span slot="reference">
                            <span v-if="row.commodityTypeReset" class="text-colorTheme">{{ row.incRemUseQuantity }}{{ row.commodityTypeUnit }}</span>
                            <span v-if="row.commodityTypeReset">/</span>
                            <span >{{ row.incTotalQuantity }}{{ row.commodityTypeUnit }}</span>
                        </span>
                        <div>
                            <div class="pd-b6 font-s12">{{ row.commodityTypeName }}-{{ column.label }}</div>
                            <div v-for="(item,index) in row?.incPackageList" :key="index" class="flex-ai-c pd-b6 font-s12">
                                <span class="pd-r10">
                                    <span v-if="row.commodityTypeReset" class="text-colorTheme">{{ item.remUseNumber }}{{item.commodityTypeUnit}}</span>
                                    <span v-if="row.commodityTypeReset">/</span>
                                    {{ item.totalNumber }}{{item.commodityTypeUnit}}</span>
                                <span class="pd-r10">有效期至：{{ item.endTime }}</span>
                            </div>
                        </div>
                    </el-popover>
                </template>
                <template #totalQuantity="{row, column}">
                    <span slot="reference">
                        <span  class="text-colorTheme">{{ row.remUseQuantity }}{{ row.commodityTypeUnit }}</span>
                        <span >/</span>
                        <span >{{ row.totalQuantity }}{{ row.commodityTypeUnit }}</span>
                    </span>
                    <!-- <el-popover
                        placement="top"
                        width="500"
                        :disabled="!row?.allInnerList?.length || row.commodityTypeReset===0"
                        trigger="hover">
                        <span slot="reference">
                            <span  class="text-colorTheme">{{ row.useQuantity }}{{ row.commodityTypeUnit }}</span>
                            <span >/</span>
                            <span >{{ row.totalQuantity }}{{ row.commodityTypeUnit }}</span>
                        </span>
                        <div>
                            <div class="pd-b6 font-s12">{{ row.commodityTypeName }}-{{ column.label }}</div>
                            <div v-for="(item,index) in row?.allInnerList" :key="index" class="flex-ai-c pd-b6 font-s12">
                                <span class="pd-r10">
                                    <span class="text-colorTheme">{{ item.useNumber }}{{item.commodityTypeUnit}}</span>
                                    <span >/</span>
                                    {{ item.totalNumber }}{{item.commodityTypeUnit}}</span>
                                <span class="pd-r10">有效期至：{{ item.endTime }}</span>
                            </div>
                        </div>
                    </el-popover> -->
                </template>
            </Table> 
        </div>
    </div>
</template>

<script>
import myUtils from '../../../utils/utils';
import Table from '/src/components/Table/index.vue'
export default {
    components: {Table},
    props:{
        
    },
    data() {
        return {
            packageData: {},
            packageConfig: [
                // 会员资源（剩余/总）   增量包（剩余/总）  总资源（剩余/总）
                {  label: '会员权益', prop: 'commodityTypeName', option: {width: '320px', align: 'left'} },
                {  label: '会员资源（剩余/总）', prop: 'setMenuTotalQuantity',  option: { align: 'left'} },
                {  label: '增量包（剩余/总）', prop: 'incTotalQuantity' },
                {  label: '总资源（剩余/总）', prop: 'totalQuantity'},
            ],
        };
    },
    inject: ['APP'],
    computed: {
        getUserInfo() {
            return this.$store.state.userInfo;
        },
        getTableData(){
            // myUtils.numberToSting
            return this.packageData.dataList;
        },
        resourceUpdateTime(){
            if(this.getUserInfo.resourceUpdateTime){
                return this.getUserInfo.resourceUpdateTime?.split(' ')[0];
            }else{
                return '-'
            }
        }
    },
    watch: {},
    methods: {
        updateVersion(){
            this.APP.showQrCode()
        },
        getPackageData(){
            this.$httpBack.v2500.clintGetData({}).then(res=>{
                if(res.code == 0){
                    this.fromatData(res.data?.dataList);
                    this.packageData = res.data;
                }
            })
        },
        fromatData(data){
            data.forEach(item=>{
                let keys = ['setMenuRemUseQuantity','setMenuTotalQuantity','incRemUseQuantity','incTotalQuantity','remUseQuantity','totalQuantity'];
                if(['textTaggingWordCount','aiTokenNum'].includes(item.commodityTypeCode)){
                    keys.forEach(key=>{
                        item[key] = myUtils.numberToSting(item[key]);
                    })
                    this.formatListData(item,0)
                }else if(['aiAnalysisTime'].includes(item.commodityTypeCode)){
                    keys.forEach(key=>{
                        item[key] = myUtils.toLocale(item[key]);
                    })
                    this.formatListData(item,1)
                }else if(['storageNum'].includes(item.commodityTypeCode)){
                    item.commodityTypeUnit = 'GB';
                    keys.forEach(key=>{
                        let o = myUtils.formatFileSize(item[key],{separa: true,passUnit: 'KB', unit: item.commodityTypeUnit});
                        item[key] = o.size;
                    });
                    this.formatListData(item,2);
                }
            })
        },
        formatListData(data,type){
            let itemKeys = ['allInnerList','setMenuList','incPackageList'];
            itemKeys.forEach(k=>{
                data[k]?.forEach(item=>{ 
                    if(type === 0){
                        item.totalNumber = myUtils.numberToSting(item.totalNumber)
                        item.remUseNumber = myUtils.numberToSting(item.remUseNumber)
                    }else if(type === 1){ 
                        item.totalNumber = myUtils.toLocale(item.totalNumber)
                        item.remUseNumber = myUtils.toLocale(item.remUseNumber)
                    }else if(type === 2){
                        item.commodityTypeUnit = 'GB';
                        item.totalNumber = myUtils.formatFileSize(item.totalNumber,{separa: true,passUnit: 'KB', unit: item.commodityTypeUnit})?.size;
                        item.remUseNumber = myUtils.formatFileSize(item.remUseNumber,{separa: true,passUnit: 'KB', unit: item.commodityTypeUnit})?.size
                    }
                })
            })
            
        }
    },
    created() {
        this.getPackageData();
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
.version-items{
    >div{
        padding-right: 40px;
    }
}
</style>
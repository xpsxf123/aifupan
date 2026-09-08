<template>
    <!-- 左边 -->
    <div class="recordListLeftContainer main-bg brs-10">
        <!-- 搜索栏 -->
        <div class="pd-10">
            <slot></slot>
            <el-input placeholder="输入直播间名称搜索" v-model="anchorName" clearable size="default" v-if="!toggleMenu"
                style="width: 100%;" @input="searchCompere" class="input-gray input-border-none">
            </el-input>
            <el-select v-model="tradeId" class="mg-t10" size="default" clearable filterable placeholder="请选择" style="width: 100%;" @change="tardeChange" v-if="!toggleMenu">
                <el-option
                v-for="(item,index) in options"
                :key="index"
                :label="item.name"
                :value="item.id">
                    {{ item.name }}
                    <span v-if="item.id !==''">
                        ({{ item.anchorNum }})
                    </span>
                </el-option>
            </el-select>
        </div>
        <!-- 主播列表 -->
        <div :class="{'compereListContainer':true,'compereListContainer-toggleMenu':toggleMenu}">
            <template v-for="(item,index) in compereList">
                <div :class="item?.anchorInfo?.secUid == currentSecUid ? 'compereActiveContainer' : ''"
                    class="compereContainer"
                    @click="compereClickHandle(item)">
                    <template v-if="item?.anchorInfo?.secUid == -1">
                        <div class="nowrap">
                            <div :class="{'compereName':true,'pd-b10':!toggleMenu}">{{ item.anchorName }}</div>
                            <div class="flex-jc-sb" style="width: 100%;;font-size: 14px;color: #666;display: flex;justify-content: space-between;" v-if="!toggleMenu">
                                <span class="compere-record compere-record-all" style="background: rgba(68,77,255,0.4);color: #484A4D">总：{{ item.recordTotal}}</span>
                                <span class="compere-record compere-record-day">今：{{ item.recordTodayTotal }}</span>
                            </div>
                        </div>
                    </template>
                    <template  v-if="item?.anchorInfo?.secUid != -1">
                        <div class="flex-ai-c compere-item-box">
                            <img src="@/assets/imgs/zy.png" v-if="item.accountType==0" class="avatar-back" alt="">
                            <img src="@/assets/imgs/hy.png" v-if="item.accountType==1" class="avatar-back" alt="">
                            <img :src="item?.anchorInfo?.anchorAvatar" class="compereAvatar" />
                            <div class="compereInfoContainer" v-if="!toggleMenu">
                                <div class="compereName flex-jc-sb nowrap">
                                    <span class="compereName-text slh">{{ item.anchorInfo?.anchorName }}</span>
                                </div>
                                <div class="flex items-end justify-between" style="height: 30px">
                                    <div class="nowrap" style="margin-top: 8px">
                                        <span class="compere-record compere-record-all compere-right-record">总/今：{{ item.recordTotal }}/{{ item.recordTodayTotal }}</span>
                                    </div>
                                    <div class="up-btn-box" :class="{'is-up-btn-show': item.isTop}" @click.stop="onUp(item)" v-if="!toggleMenu">
                                        <el-button class="up-btn pd-0" type="text" icon="el-icon-download"></el-button>
                                        <div class="isTop-text" v-if="!item.isTop">置顶</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </template>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
import myUtils from '../../../../utils/utils';
import commonHttp from '../../../../mixins/commonHttp';
export default {
    components: {},
    props: {
        replayType: {
            type: String,
            default: ''
        },
        toggleMenu:{
            type: Boolean,
            default: false
        }
    },
    mixins: [commonHttp],
    data() {
        return {
            anchorName: '',
            currentSecUid: -1,
            tradeId: '',
            options: [],
            compereList: [
                {
                    anchorInfo: {
                        secUid: -1,
                    },
                    anchorName: "全部",
                    recordTotal: 0,
                    recordTodayTotal: 0
                }
            ],
            deb:myUtils.debounce(500)
        };
    },
    computed: {},
    watch: {},
    methods: {
        searchCompere() {
            this.deb(()=>{
                this.getCompereList();
                this.setRouteQuery('anchorName', this.anchorName)
                this.$emit('search', this.anchorName);
            })
        },
        tardeChange(){
            this.setRouteQuery('tradeId', this.tradeId);
            this.getCompereList();
        },
        compereClickHandle(item) {
            this.currentSecUid = item?.anchorInfo?.secUid || item?.secUid;
            this.$emit('click', item?.anchorInfo)
        },
        onUp(item){
            this.$httpBack.compere.topAnchor({
                secUid: item?.anchorInfo?.secUid,
                action: item?.isTop? 0 : 1
            }).then(()=>{
                this.getCompereList();
            });
        },
        // 获取主播列表
        getCompereList() {
            const {replayType} = this
            let requestData = {
                page: 1,
                limit: 999999,
                anchorName: this.anchorName || "",
                tradeId: this.tradeId,
            }
            if (replayType === 'replayAll') {
                requestData.videoSliceType = 0
            } else if (replayType === 'replaySection') {
                requestData.videoSliceType = 1
            } else {
                requestData.videoSliceType = 2
            }
            this.$httpBack.compere.clientAnchorRecordList(requestData).then((res) => {
                if (res.code == 0) {
                    let aryData = [];
                    let o = {
                        recordTotal: 0,
                        recordTodayTotal: 0
                    }
                    res.data?.list?.forEach(item => {
                        aryData.push(item);
                        o.recordTotal += item.recordTotal;
                        o.recordTodayTotal += item.recordTodayTotal;
                    });
                    this.compereList = [].concat({
                        ...this.compereList.shift(),
                        ...o
                    }, aryData);
                    // 加载列表完成
                    this.$emit('load-list');

                }
            })
        },
        init(){
            this.$nextTick(() => {
                this.anchorName = this.getRouteQuery('anchorName','');
                this.tradeId = this.getRouteQuery('tradeId','');
                this.currentSecUid = this.getRouteQuery()?.secUid || -1;
                this.getListByAnchor((list)=>{
                    this.options = [
                        {
                            id: "",
                            name: "行业筛选（全部）"
                        },
                        ...list
                    ]
                })
                this.getCompereList();
            })
        }
    },
    created() {

    },
    mounted() {
        this.init();
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() {

    }, //生命周期 - 销毁完成
    activated() {
        this.init();
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.recordListLeftContainer {
    // min-width: 255px;
    width: 100%;
    // padding: 10px;
    box-sizing: content-box;
    // border-right: 0.5px #DCE0E7 solid;
}

.compereListContainer::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 4px;
}

.compereListContainer::-webkit-scrollbar {
    width: 4px;
}

.compereListContainer {
    height: calc(100vh - 213px);
    overflow-y: auto;
    margin-top: 10px;
}

.compereListContainer-toggleMenu{
    display: flex;
    flex-direction: column;
    align-items: center;
    height: calc(100vh - 128px);
}

.compereActiveContainer {
    // display: flex;
    padding: 10px;
    cursor: pointer;
    align-items: center;
    // margin-right: 10px;
    // background:  var(--color-main);
    background: linear-gradient( 90deg, #e8e2fa 0%, #d1eaf8 100%);
    border-radius: 10px;
}

.compereContainer {
    // display: flex;
    padding: 10px;
    cursor: pointer;
    // align-items: center;
    // margin-right: 10px;
}

.compereAvatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
}

.compereInfoContainer {
    // display: flex;
    // flex-direction: column;
    margin-left: 10px;
    width: 100%;
}

.compereName {
    font-weight: 400;
    font-size: 14px;
    color: #2E3742;
}

.compereStatusContainer {
    display: flex;
    align-items: center;
    margin-top: 4px;
}

.compereNotPlayStatus {
    font-size: 13px;
    color: #95A1AF;
}

.comperePlayStatus {
    display: flex;
    align-items: center;
}

.comperePlayStatusImg {
    width: 18px;
}

.comperePlayStatusText {
    margin-left: 4px;
    font-weight: 500;
    font-size: 13px;
    color: #FF3270;
}

.compereResourceType {
    border-radius: 8px;
    font-size: 10px;
    color: #FFFFFF;
    margin-left: 5px;
    display: flex;
    align-items: center;
}

.compereResourceTypeImg {
    width: 16px;
    height: 16px;
}
.compere-record{
    padding: 6px 12px;
    border-radius: 30px;
    color: #666;
    font-size: 14px;
    &-all{
        background: #F1F4F8;
    }
    &-day{
        background: #F1F4F8;
    }
}
.compere-right-record{
    padding: 2px 4px;
    font-size: 12px;
    border-radius: 0;
}
.compereName-text{
    width: 110px;
    display: inline-block;
}

.compere-item-box{
    position: relative;
    .up-btn-box{
        text-align: center;
        display: none;
        .up-btn{
            color: var(--text-3);
        }
    }
    .avatar-back{
        width: 42px;
        height: 43px;
        left: -1px;
        position: absolute;
    }

    &:hover .up-btn-box{
        display: block;
    }
    .is-up-btn-show{
        display: block;
        .up-btn{
            color: var(--color-main);
        }
    }
}

.up-btn{
    transform: rotate(180deg);
}
.isTop-text {
    font-size: 10px;
    color: #7A7C80;
    width: 22px
}
.nowrap{
    white-space: nowrap;
}
</style>
<template>
    <el-popover
    placement="right-end"
    width="230"
    popper-class="scrolling-name-pop"
    v-model="getPopValue"
    trigger="manual">
        <div ref="popContent">
            <div v-if="getItem?.isDesensitization" class="scrolling-pop-content-mask flex-jc-c flex-ai-c" style="color: #fff;">
                <div class="scrolling-pop-content-mask-text">
                    想查看更多个人资料<br>
                    请<el-button type="text" class="pd-0" @click="upgrade">立即升级</el-button>到更高版本
                </div>
            </div>
            <div class="scrolling-pop-title text-fff font-s14 b-b1-c1 flex-jc-sb flex-ai-c">{{ getItem.nickName }}<img style="max-height: 38px;" src="@/assets/imgs/namePopIcon.png" alt=""></div>
            <div :class="{'scrolling-pop-filter':getItem?.isDesensitization}" class="scrolling-pop-content">
                <el-tabs class="scrolling-pop-tabs" v-model="activeName" @tab-click="handleClick">
                    <el-tab-pane label="基本资料" name="first">
                        <div style="height: 122px;">
                            <div class="scrolling-pop-item">
                                抖音等级：<span class="icon-bg icon-dydj">{{ getItem.level }}</span>
                            </div>
                            <div class="scrolling-pop-item">
                                粉丝牌：
                                <div style="display: inline-block;vertical-align: top;">
                                    <span>初始等级：<span class="icon-bg icon-min">{{ getItem.fansLevelMin }}</span></span><br>
                                    <span>最终等级：<span class="icon-bg icon-max">{{ getItem.fansLevelMax }}</span></span>
                                </div>
                            </div>
                            <div class="scrolling-pop-item">
                                发言数：{{ getItem.countSendNum }}
                            </div>
                            <div v-if="getItem.isNew" class="scrolling-pop-item">
                                直播间第一次发言
                            </div>
                        </div>
                    </el-tab-pane>
                    <el-tab-pane label="发言内容" name="second">
                        <div class="content-list">
                            <div v-for="(item,index) in allList" :key="index" class="scrolling-pop-item">{{ item.content }}</div>
                        </div>
                    </el-tab-pane>
                    <el-tab-pane label="其他场次" name="third">
                        <div class="other-content-list">
                            <el-tree class="other-content-tree" :data="otherList" :props="defaultProps">
                                <span class="custom-tree-node" node-key="id" slot-scope="{ node, data }">
                                    <span v-if="node.level === 1" class="flex-jc-sb">
                                        <span class="font-s12 tree-main-left">{{ data.batchNumberDate || '-----' }}</span>
                                        <span class="font-s12 tree-main-right">共{{ data?.list?.length }}条</span>
                                    </span>
                                    <span v-else class="flex-jc-sb">
                                        <span class="font-s12 tree-main-left">{{ data.content||'-'}}</span>
                                        <span class="font-s12 tree-main-right">{{ getStringDate(data.recordDate) }}</span>
                                    </span>
                                </span>
                            </el-tree>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </div>
        </div>
        <div slot="reference" style="position: absolute;bottom: 15px;right: 0;">
            <slot></slot>
        </div>
    </el-popover>
</template>

<script>
import myUtils from '../../../utils/utils';
export default {
    components: {},
    props:{
        startTime: {
            type: [String,Number],
            default: 0
        },
        isUpgrade: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            showState: false,
            defaultProps: {
                children: 'list',
                id: 'id',
                value: 'content',
            },
            activeName: 'first',
            allList: [],//本场所有发言
            otherList: [],//其他场次发言,
            popItem: null
        };
    },
    inject: ['appVnode'],
    computed: {
        getPopValue:{
            get(){
                return this.$attrs.value
            },
            set(val){
                // this.$emit('input',val)
            }
        },
        getItem(){
            return {
                ...this.item,
                ...this.popItem
            }
        }
    },
    watch: {},
    methods: {
        setTiem(item){
            if(item.nickName === this.getItem.nickName && this.getPopValue){
                this.hide();
            }else{
                this.activeName = 'first';
                this.allList = [];
                this.otherList = [];
                this.popItem = item;
            }
        }, 
        hide(){
            // this.showState = false;
            this.$emit('hide');
            this.$nextTick(()=>{
                this.$emit('hide');
            })
        },
        upgrade(){
            this.appVnode?.showQrCode()
        },
        handleClick(){
            // 判断升级,如果需要升级则不请求数据
            if(this.getIsUpgrade){return};
            if(this.activeName === 'second'){
                this.$emit('getAll',{nickName: this.getItem.nickName, level: this.getItem.level},(data)=>{
                    this.allList = data;
                })
            }else if(this.activeName === 'third'){
                this.$emit('getOther',{nickName: this.getItem.nickName, level: this.getItem.level},(data)=>{
                    this.otherList = data;
                })
            }
        },
        getStringDate(time){
            return myUtils.timestampToChinese(time,{format: 'HH:mm:ss'})
        },
        clickHandler(){
            this.$emit('input',!this.$attrs.value);
            this.$emit('update:selectIndex', this.index);
        },
        // 设置点击document时取消弹窗
        setDocumentClick(){
            document.addEventListener('click',(e)=>{
                if(this.$refs?.popContent && this.getPopValue){
                    let isSelf = this.$refs?.popContent?.contains(e.target);
                    if(!isSelf){
                        this.hide();
                    }
                }
            })
        }
    },
    created() {
        this.$nextTick(()=>{
            this.setDocumentClick();
        })
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
<style lang='scss'>
.scrolling-name-pop{
    background: #3B3D3F !important;
    border: none !important;
    box-shadow: 1px 1px 8px 0px rgba(59,61,63,0.46) !important;
    color: #fff;
    .popper__arrow{
        &::after{
            border-right-color: #3B3D3F !important;
            display: none !important;
        }
    }
    .scrolling-pop-title{
        padding: 0 10px;
        padding-top: 0;
        margin: 0 -10px;
        margin-bottom: 8px;
    }
    .scrolling-pop-content{
        position: relative;
        .scrolling-pop-content-mask{
            width: 100%;
            height: 100%;
            left: 0;
            top: 0;
            position: absolute;
            background: rgba(0,0,0,0.4);
            text-align: center;
            z-index: 99;
            &-text{
                padding: 4px;
                // background: rgba(#000, 0.3);
                border-radius: 5px;
            }
        }
    }
    .scrolling-pop-filter{
        filter: blur(1px);
    }
    // .scrolling-pop-content-upgrade{
    //     margin: -9px -12px -12px -12px;
    // }
    .scrolling-pop-tabs{
        .el-tabs__header{
            margin-bottom: 5px;
        }
        .el-tabs__nav{
            width: 100%;
            display: flex;
            justify-content: space-between;
        }
        .el-tabs__item{
            padding: 0 3px;
            color: #fff;
            line-height: 24px;
            height: 24px;
        }
        .is-active{
            color: #C2DBFF;
        }
        .el-tabs__active-bar{
            display: none;
        }
        .el-tabs__nav-wrap{
            &::after{display: none;}
        }
    }
    .icon-bg{
        display: inline-block;
        font-size: 10px;
        height: 14px;
        line-height: 14px;
        text-align: right;
        background-size: cover;
        padding-right: 4px;
        padding-top: 1px;
        margin-right: 5px;
        width: 30px;
    }
    .icon-dydj{
        width: 30px;
        color: #09297D;
        background-image: url('~@/assets/imgs/dydj.png');  
    }
    .icon-min{
        width: 27px;
        color: #09297D;
        background-image: url('~@/assets/imgs/icon-max.png');
    }
    .icon-max{
        width: 27px;
        color: #7D090B;
        background-image: url('~@/assets/imgs/icon-max.png'); 
    }
    .scrolling-pop-item{
        color: #fff;
        padding: 3px 0;
        &::before{
            content: ' ';
            display: inline-block;
            width: 4px;
            height: 4px;
            background: #BBE5FF;
            border-radius: 50%;
            vertical-align: middle;
            margin-right: 2px;
        }
    }
    .content-list,.other-content-list{
        height: 122px;
        overflow: hidden;
        overflow-y: auto;
    }
    .other-content-tree{
        background: transparent;
        color: #fff;
        .el-tree-node{
            white-space: normal;
            outline: 0;
        }
        .el-tree-node__content{
            padding: 0 !important;
            background: transparent !important;
            display: flex;
            text-align: left;
            align-items: center;
            height: 100% !important;
        }
        .custom-tree-node{
            width: 100%;
            // padding: 0 5px;
            // display: block;
        }
        .is-leaf{
            display: none;
        }
        .tree-main-left{
            max-width: calc(100% - 55px);
            white-space: normal;
        }
        .tree-main-right{
            text-align: right;
            max-width: 55px;
        }
        .is-focusable{

        }
    }
}
</style>
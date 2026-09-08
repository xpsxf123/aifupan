<template>
    <div class="operation-button flex items-center justify-end">
        <template v-for="(item, index) in options">
<!--            <div class="cursor-pointer" style="height: 28px" @click="clickHandler(item)">-->
<!--                <img src="@/assets/imgs/unStart.png" alt="">-->
<!--            </div>-->
            <el-popconfirm v-if="item.popconfirm && itemHide[index]" @confirm="clickHandler(item)" :title="item.popconfirm" >
                <afp-button  slot="reference" class="operation-button-single" :class="getClass(item)" :disabled="isDisabled(item)" :type="getType(item) || ''" size="medium"
                    :plain="getPlain(item)??true">
                    <span v-if="item.dangerouslyUseHTMLString" v-html="getLabel(item)"></span>
                    <span v-else>{{ getLabel(item) }}</span>
                </afp-button>
            </el-popconfirm>
            <afp-button v-else-if="itemHide[index]" class="operation-button-single" :class="getClass(item)" :disabled="isDisabled(item)" :type="getType(item) || ''" size="medium"
                :plain="getPlain(item)??true" @click="clickHandler(item)">
                <span v-if="item.dangerouslyUseHTMLString" v-html="getLabel(item)"></span>
                <span v-else>{{ getLabel(item) }}</span>
            </afp-button>
            <el-dropdown v-else-if="isSHowItems && item.length" class="operation-button-btns" @command="handleCommand" @visible-change="visibleChange"
                trigger="click">
                <span class="el-dropdown-link cursor-pointer">
                    更多<i :class="{'arrow-icon':true,'el-icon-arrow-down':true, active: iconStatus }"></i>
                </span>
                <el-dropdown-menu slot="dropdown" class="operation-drop">
                    <el-dropdown-item class="operation-button" v-for="(sItem, sIndex) in item" :key="sIndex"
                        v-if="itemsHide[sIndex]" :command="sIndex"  :disabled="isDisabled(sItem)">
                        <div class="bt-hover">
                            <!-- <img v-if="sItem.url" class="bt-img default-img" :src="sItem.url">
                            <img v-if="sItem.hoverUrl" class="bt-img hover-img" :src="sItem.hoverUrl">
                            <span class="icon-box">
                                <i v-if="sItem.icon" class="font_family"
                                    :style="{ 'font-size': `${sItem.iconSize || 18}px` }"
                                    :class="`${sItem.icon} color-${sItem.type || 'primary'}`">
                                </i>
                            </span> -->
                            <span v-if="sItem.dangerouslyUseHTMLString" v-html="getLabel(sItem)" class="bt-text" :class="`color-${sItem.type || 'primary'}`"></span>
                            <span v-else class="bt-text" :class="`color-${sItem.type || 'primary'}`">{{ getLabel(sItem) }}</span>
                        </div>
                    </el-dropdown-item>
                </el-dropdown-menu>
            </el-dropdown>
            <div style="font-size: 12px;color: red" v-if="itemHide[index] && item.subError">{{item.subError}}</div>
        </template>
    </div>
</template>

<script>

// import 
export default {
    components: {},
    props: {
        options: {
            type: Array,
            default: () => { }
        },
        data: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            hiddenMap: [],
            iconStatus:false
        };
    },
    computed: {
        getItems(){
            let items = this.options[this.options.length - 1];
            return Array.isArray(items) ? items : [];
        },
        itemHide() {
            return this.options?.filter(o=>!Array.isArray(o))?.map(o=>{
                return this.hidden?.(o)
            })
        },
        itemsHide() {
            // getItems
            return this.getItems.map(o=>{
                return this.hidden?.(o) ? 1 : 0
            })
        },
        isSHowItems(){
            if(this.getItems?.length<=0){return false}
            // 如果固定存在一个更多按钮就不进行隐藏计算
            if (this.getItems.some(item => {
                return typeof item.hidden === 'undefined' && typeof item.show === 'undefined'
            })) { return true };
            return !!this.itemsHide.reduce((a,b)=>a+b,0)
        },
    },
    watch: {},
    methods: {
        isDisabled(item){
            if(typeof item.disabled ==='undefined'){return false}
            if(typeof item.disabled === 'function'){
                return item.disabled(this.data,item);
            }else{
                return item.disabled;
            }
        },
        getLabel(item) {
            if (typeof item.label === 'undefined') {
                return ''
            }
            if (typeof item.label === 'function') {
                return item.label(this.data);
            } else {
                return item.label
            }
        },
        getType(item) {
            if (typeof item.type === 'undefined') {
                return ''
            }
            if (typeof item.type === 'function') {
                return item.type(this.data);
            } else {
                return item.type
            }
        },
        getPlain(item) {
            if (typeof item.plain === 'undefined') {
                return undefined
            }
            if (typeof item.plain === 'function') {
                return item.plain(this.data);
            } else {
                return item.plain
            }
        },
        visibleChange(visible) {
            this.iconStatus = visible;
            this.$emit('visibleChange', visible);
        },
        // isShowItems(items) {
        //     if (typeof items.length === 'undefined') {
        //         return false
        //     }
        //     // 如果固定存在一个更多按钮就不进行隐藏计算
        //     if (items.some(item => {
        //         return typeof item.hidden === 'undefined' && typeof item.show === 'undefined'
        //     })) { return true };
        //     // 如果多个按钮全是动态隐藏 则计算如果全部隐藏则取消更多按钮
        //     this.hiddenMap = [];
        //     this.hiddenMap = items.map((o) => {
        //         return this.hidden(o) ? 1 : 0
        //     })
        //     return !!(this.hiddenMap.reduce((a, b) => a + b, 0));
        // },
        hidden(item) {
            if (item.length >= 0) { return false }
            if (typeof item.show === 'function') {
                return item.show(this.data, item)
            } else if (typeof item.hidden === 'function') {
                return !item.hidden(this.data, item);
            } else if (typeof item.hidden === 'boolean') {
                return !item.hidden
            } else {
                return !item.hidden
            }
        },
        handleCommand(index) {
            this.clickHandler(this.options[this.options.length - 1]?.[index])
        },
        clickHandler(item) {
            if (item.click && typeof item.click === 'function') {
                item.click(this.data, item)
            }
            this.$emit('click', {
                data: this.data,
                item
            })
        },
        getClass(option){
            if(option.classNameFn){
                return option.classNameFn(this.data)
            }else if(option.className){
                return option.className
            }else{
                return ''
            }
        }
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
.operation-button {
    .el-button {
        margin: 5px;
        height:26px;
    }
}
.el-dropdown-link{
    white-space: nowrap;
    font-size: 12px;
    padding-left: 5px;
    margin: 2px;
    color: var(--color-main)
}
.bt-img {
    max-width: 18px;
    margin-top: -3px;
}

.bt-text {
    font-weight: 400;
    font-size: 14px;
    color: #151917;
    line-height: 22px;
    padding-block: 1px;
}

.bt-hover {
    >* {
        display: inline-block;
        vertical-align: middle;
    }

    .hover-img {
        display: none;
    }

    .default-img {
        display: inline-block;
    }

    &:hover .hover-img {
        display: inline-block;
    }

    &:hover .default-img {
        display: none;
    }

    &:hover .color-primary {
        color: var(--color-main);
    }

    &:hover .color-danger {
        color: #FC4F52;
    }
}
.arrow-icon {
    display: inline-block;
    transition: transform 0.25s ease;
}

/* 向上 */
.arrow-icon.active {
    transform: rotate(180deg);
}
.operation-drop {
    background: #D4E6FD;

    ::v-deep(.popper__arrow::after) {
        border-bottom-color: #D4E6FD !important;
    }

    ::v-deep(.el-dropdown-menu__item i) {
        margin-right: 0px;
    }

    ::v-deep(.el-dropdown-menu__item.is-disabled .bt-text) {
        color: #949494;
    }
}
.dropdown-more-button{
    padding:0 !important;
    width:26px;
    height:26px;
}
.icon-box {
    width: 20px;
    text-align: center;
    align-items: center;
    vertical-align: middle;
}
</style>
<template>
    <el-row class="nav-el-row">
        <el-col>
            <el-menu :default-active="selectData" @select="onSelect">
                <template v-for="(item, index) in menuList">
                    <!-- 有子菜单的项 -->
                    <el-submenu  v-if="item.children && item.children.length" :key="`submenu-${index}`" :index="`${index}`">
                        <template slot="title">
                            <div class="icon-container flex items-center justify-center"
                                 :style="{backgroundPosition: index == (selectData.split('-')[0]) ? item.selectPosition : item.unSelectPosition}">
<!--                                <svg class="icon menuImg" aria-hidden="true" :key="`icon-${index}`">-->
<!--                                    <use :xlink:href="`#icon-${`${index}` == (selectData.split('-')[0]) ? item.iconSelect : item.icon}`"></use>-->
<!--                                </svg>-->
                            </div>
                            <span class="menuText font-color2 suffix-icon" :class="{'other-icon':item.otherIcon}" v-new-hint="getNewHintData(item)">{{ getMenuLabel(item) }}</span>
                        </template>
                        <el-menu-item v-for="(subItem, subIndex) in item.children"
                                      :key="`${index}-${subIndex}`"
                                      :index="`${index}-${subIndex}`"
                                      :class="{'sub-menu-item': true}">
                            <span :class="{'menuText':true,'font-color2':true,'suffix-icon':true,'upgrade-icon':subItem.upgrade,'other-icon':subItem.otherIcon}" v-new-hint="getNewHintData(subItem)">{{ getMenuLabel(subItem) }}</span>
                        </el-menu-item>
                    </el-submenu>
                    <!-- 没有子菜单的普通项 -->
                    <el-menu-item v-else :class="{'meun-item':true,'meun-item-other':item.index===9999 || item.type === 'other'}" :id="item.addId || undefined" :key="`item-${index}`" :index="`${index}`">
                        <div class="icon-container flex items-center justify-center" v-if="item.index !== 9999&&item.type !== 'other'"
                             :style="{background:item.index == 1 && isDetectionStatus?'#fff':'',backgroundPosition:item.index == 1 && isDetectionStatus?'-9999px -9999px':(index == (selectData.split('-')[0]) ? item.selectPosition : item.unSelectPosition)}">
                            <div v-if="item.index == 1 && isDetectionStatus" class="loaderRectangle" style="margin-top:12px" :key="`loader-${index}`">
                                <div></div>
                                <div></div>
                                <div></div>
                                <div></div>
                            </div>
<!--                            <svg v-else class="icon menuImg" aria-hidden="true" :key="`icon-${index}`">-->
<!--                                <use :xlink:href="`#icon-${`${index}` == (selectData) ? item.iconSelect : item.icon}`"></use>-->
<!--                            </svg>-->
                        </div>
                        <div v-else-if="item.index === 9999" class="invite" :key="`invite-${index}`">
                           <span class="menuText font-color2 " v-new-hint="getNewHintData(item)">{{ getMenuLabel(item) }}</span>
                        </div>
                        <!-- <div v-else-if="item.type === 'other'" :class="item.className" :key="`${item.pathName}-${index}`">
                           <span class="menuText font-color2">{{ item.label }}</span>
                        </div> -->
                        <span
                            v-if="item.index !== 9999"
                            :class="{'menuText': true, 'font-color2': true, 'suffix-icon': true, 'other-icon': item.otherIcon}"
                            v-new-hint="getNewHintData(item)"
                            :key="`text-${index}`">{{ getMenuLabel(item) }}</span>
                    </el-menu-item>
                </template>
            </el-menu>
        </el-col>
    </el-row>
</template>

<script>
import menuJson from '@/assets/json/menu.json';
// require('@/assets/json/menu.json');
import {VERSION_TYPE} from "@/enum";
import {cloneDeep} from "lodash";
export default {
    components: {},
    props:{
    },
    data() {
        return {
            selectData: '',
            originalMenuList: [
                {
                    label: "AI智能体",
                    icon: 'a-123456',
                    iconSelect: 'daohangtubiao-11',
                    pathName: 'aiAgent',
                    matchPaths: ['/aiAgent', '/aiAssistant', '/aiViolation'],
                    unSelectPosition: '-208px 0px',
                    selectPosition: '-208px -42px',
                    otherIcon: true,
                    show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                },
                {
                    index: 1,
                    label: "直播间列表",
                    icon: 'daohangtubiao',
                    iconSelect: 'daohangtubiao-1',
                    pathName: 'dataAnalysis',
                    unSelectPosition: '0px 0px',
                    selectPosition: '0px -42px',
                    show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                },
                {
                    label: "添加直播间",
                    icon: 'a-Property1Default-4',
                    iconSelect: 'a-Property1Default-5',
                    pathName: 'addCompere',
                    unSelectPosition: '-104px 0px',
                    selectPosition: '-104px -42px',
                    show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE],
                    children: [{
                        label: "添加抖音",
                        pathName: 'douyin',
                        path: '/addCompere/douyin',
                        show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                    }, {
                        label: "添加视频号",
                        pathName: 'shipinhao',
                        path: '/addCompere/shipinhao',
                        otherIcon: true,
                        show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                    }, {
                        label: "添加快手",
                        pathName: 'kuaishou',
                        path: '/addCompere/kuaishou',
                        show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                    }]
                },
                {
                    label: "复盘直播",
                    icon: 'a-Property1Default-6',
                    iconSelect: 'a-11111111',
                    pathName: 'replay',
                    addId: 'replay-nav',
                    unSelectPosition: '-364px 0px',
                    selectPosition: '-364px -42px',
                    otherIcon: true,
                    show: [VERSION_TYPE.AGENT],
                    children: [
                        {
                            label: "AI整场复盘",
                            pathName: 'replay',
                            show: [VERSION_TYPE.AGENT]
                        },
                        {
                            label: "AI切片复盘",
                            pathName: 'section',
                            show: [VERSION_TYPE.AGENT]
                        },
                        {
                            label: "短视频切片",
                            pathName: 'short',
                            show: [VERSION_TYPE.AGENT]
                        }
                    ]
                },
                {
                    label: "短视频创作",
                    icon: 'a-Property1Default-6',
                    iconSelect: 'a-11111111',
                    pathName: 'shortVideo',
                    unSelectPosition: '0px 0px',
                    selectPosition: '0 -42px',
                    show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE],
                    children: [{
                        label: "提取文案",
                        icon: 'a-Property1Default-6',
                        iconSelect: 'a-11111111',
                        pathName: 'extractDoc',
                        show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                    }, {
                        label: "搜达人",
                        icon: 'a-Property1Default-6',
                        iconSelect: 'a-11111111',
                        pathName: 'expert',
                        show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                    }, {
                        label: "订阅达人",
                        icon: 'a-Property1Default-6',
                        iconSelect: 'a-11111111',
                        pathName: 'subscribeExpert',
                        show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                    },
                        {
                            label: "订阅爆款",
                            icon: 'a-Property1Default-6',
                            iconSelect: 'a-11111111',
                            pathName: 'subscribeHotItem',
                            show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE],
                        }]
                },
                {
                    label: "录屏列表",
                    icon: 'a-Property1Default-6',
                    iconSelect: 'a-11111111',
                    pathName: 'replay',
                    addId: 'replay-nav',
                    unSelectPosition: '-208px 0px',
                    selectPosition: '-208px -42px',
                    otherIcon: true,
                    show: [VERSION_TYPE.PURE]
                },
                {
                    label: "AI对比复盘",
                    icon: 'a-123456',
                    iconSelect: 'daohangtubiao-11',
                    pathName: 'contrast',
                    unSelectPosition: '-260px 0px',
                    selectPosition: '-260px -42px',
                    show: [VERSION_TYPE.AGENT],
                    children: [
                        {
                            label: "整场对比",
                            pathName: 'contrastReplay',
                            show: [VERSION_TYPE.AGENT]
                        },
                        {
                            label: "切片对比",
                            pathName: 'contrastSection',
                            show: [VERSION_TYPE.AGENT]
                        },
                    ]
                },
                {
                    label: "排班业绩",
                    icon: 'a-Property1Default-6',
                    iconSelect: 'a-11111111',
                    pathName: 'scheduling',
                    unSelectPosition: '-468px 0px',
                    selectPosition: '-468px -42px',
                    otherIcon: true,
                    show: [VERSION_TYPE.AGENT]
                },
                {
                    label: "文件分析",
                    icon: 'a-122',
                    iconSelect: 'a-Property1Default-3',
                    pathName: 'playBckAnalysis',
                    unSelectPosition: '-156px 0px',
                    selectPosition: '-156px -42px',
                    show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE],
                    children: [
                        {
                            label: "视频分析",
                            pathName: 'uploadVideo',
                            show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                        },
                        {
                            label: "文案预审",
                            pathName: 'uploadText',
                            show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                        },
                        {
                            label: "切片复盘",
                            pathName: 'uploadSlice',
                            show: [VERSION_TYPE.AGENT]
                        },
                        {
                            label: "短视频切片",
                            pathName: 'uploadShort',
                            show: [VERSION_TYPE.AGENT]
                        }
                    ]
                },
                {
                    label: "云空间",
                    icon: 'zaixianfupan',
                    iconSelect: 'zaixianfupan-1',
                    show: [VERSION_TYPE.AGENT],
                    pathName: 'online',
                    children: [
                        {
                            label: "整场复盘",
                            pathName: 'onlineVideo',
                            path: '/online/onlineVideo',
                            show: [VERSION_TYPE.AGENT]
                        },
                        // {
                        //     label: "文件复盘",
                        //     pathName:'fileOnline',
                        //     path: '/online/fileOnline'
                        // },
                        {
                            label: "切片复盘",
                            pathName: 'sliceOnline',
                            path: '/online/sliceOnline',
                            show: [VERSION_TYPE.AGENT]
                        },
                        {
                            label: "对比复盘",
                            pathName: 'onlineContrast',
                            path: '/online/onlineContrast',
                            show: [VERSION_TYPE.AGENT]
                        }
                    ]
                },
                // {
                //     label: "本地词库",
                //     icon: 'a-Property1Default-1',
                //     iconSelect: 'a-Property1Default-2',
                //     pathName:'lexicon'
                // },
                {
                    label: "系统设置",
                    icon: 'a-Property1Default-9',
                    iconSelect: 'a-Property1Default-10',
                    pathName: 'system',
                    unSelectPosition: '-468px 0px',
                    selectPosition: '-468px -42px',
                    show: [VERSION_TYPE.AGENT, VERSION_TYPE.PURE]
                },
                // {
                //     index: 999,
                //     label: '帮助中心',
                //     icon: 'bangzhuzhongxinoff',
                //     iconSelect:'as',
                // },
                // {
                //     type: 'other',
                //     label: "",
                //     icon: '',
                //     iconSelect: '',
                //     pathName: 'anniversary',
                //     className: 'anniversary'
                // },
                {
                    index: 9999,
                    label: "",
                    icon: '',
                    iconSelect: '',
                    pathName: 'invite',
                    show: [VERSION_TYPE.AGENT]
                }
            ],
        };
    },
    computed: {
        isDetectionStatus() {
            return this.$store.state?.detectionStatus;
        },
        versionType(){
            return this.$store.getters.getVersionType
        },
        menuList() {
            const list = cloneDeep(this.originalMenuList)
            return this.filterByVersionType(list, this.versionType);
        },
    },
    watch: {
        '$route.path'() {
            this.selectNav();
        },
        menuList() {
            this.pathNormal();
            this.$nextTick(() => {
                this.selectNav()
            })
        }
        // '$store.state.versionType': {
        //     handler(val) {
        //         this.$nextTick(()=>{
        //             this.selectNav();
        //         })
        //     },
        //     deep: true,
        //     immediate: true
        // }

        // activeNav: {
        //     handler(val){
        //         this.$emit('nav-change', this.menuList[val]);
        //     },
        //     immediate: true
        // }
    },
    methods: {
        filterByVersionType(list, versionType) {
            return list.reduce((acc, item) => {
                if (item.show && item.show?.includes(versionType)) {
                    const newItem = {...item}
                    if (newItem.children && Array.isArray(newItem.children)) {
                        newItem.children = this.filterByVersionType(newItem.children, versionType)
                    }
                    acc.push(newItem)
                }
                return acc
            }, [])
        },
        /**
         * @description 获取菜单项显示文字。
         * 优先使用当前页面的版本标题；若菜单项显式声明了 `label`，则直接使用该值，哪怕它是空字符串；
         * 只有在未声明 `label` 时，才回退到 `menuConfig.meta.title`。
         * @param {Object} item 菜单项配置
         * @returns {string}
         */
        getMenuLabel(item) {
            if (item.menuConfig?.meta?.versionTitles?.[this.versionType]) {
                return item.menuConfig.meta.versionTitles[this.versionType];
            }
            // 显式传入空字符串也视为有效配置，避免误回退到路由 title。
            if (Object.prototype.hasOwnProperty.call(item || {}, 'label')) {
                return item.label;
            }
            if (item.menuConfig?.meta?.title) {
                return item.menuConfig.meta.title;
            }
            return '';
        },
        getNewHintData(item) {
            return item.new ? {
                version: '2.4.1',
                className: 'new-hint',
                type: 'always'
            } : undefined
        },
        getHeight() {
            return document.querySelector('.nav-el-row')?.offsetHeight;
        },
        onSelect(index) {
            // 处理子菜单项的选择
            const indexArr = index.split('-');
            const parentIndex = indexArr[0];
            const childIndex = indexArr[1];

            let selectedItem;
            if (childIndex !== undefined) {
                // 选择的是子菜单项
                selectedItem = this.menuList[parentIndex].children[childIndex];
                // 合并父菜单的pathName和子菜单的pathName
                const parentPathName = this.menuList[parentIndex].pathName;
                const childPathName = selectedItem.pathName;
                this.$router.push({
                    path: selectedItem.path || this.getPathByName(childPathName),
                    query: selectedItem.query
                });
            } else {
                // 处理普通菜单项
                selectedItem = this.menuList[index];
                if (selectedItem.index === 999) {
                    let url = localStorage.getItem('helpUrl') || '';
                    if (!url) {
                        url = this.getHelpUrl('helpUrl');
                    }
                    window.open(url, "_blank");
                    return;
                }
                this.$router.push({
                    path: selectedItem.path
                });
            }

            this.selectData = `${index}`;
            // this.activeNav = `${index}`;
        },

        getHelpUrl(key) {
            localStorage.setItem(key, 'https://ucnus90885lw.feishu.cn/wiki/EvgqwyoL5ikDjAksEmvccFVbnDg?from=from_copylink', {
                expires: Date.now() + (1000 * 60 * 60 * 1) // 过期时间为 1 天
            });
            return localStorage.getItem(key);
        },

        // 匹配path路径
        pathNormal() {
            // 循环添加路径
            this.menuList.forEach(item => {
                let menuConfig = menuJson.find(i => i.name === item.pathName);
                if (menuConfig) {
                    item.path = menuConfig.path;
                    item.menuConfig = menuConfig;
                }
                // 处理子菜单
                if (item.children && item.children.length) {
                    item.children.forEach((child) => {
                        let childMenuConfig = menuJson.find((i) => i.name === child.pathName);
                        if (childMenuConfig) {
                            child.path = childMenuConfig.path;
                            child.menuConfig = childMenuConfig;
                        }
                    });
                }
            })
        },

        // 根据pathName获取path
        getPathByName(pathName) {
            const menuConfig = menuJson.find(i => i.name === pathName);
            return menuConfig ? menuConfig.path : '/';
        },
        isMatch(menuPath, currentPath) {
            if (!menuPath) return false;
            if (Array.isArray(menuPath)) {
                return menuPath.some(item => this.isMatch(item, currentPath));
            }
            // 处理相对路径
            const full = menuPath.startsWith('/') ? menuPath : '/' + menuPath;
            const regex = new RegExp(
                '^' + full
                    .replace(/\/:\w+/g, '/[^/]+')
                    .replace(/\*/g, '[^/]*') +   // 关键：* 只匹配当前层级，不吞子路径！
                '(/|$)'
            );
            return regex.test(currentPath);
        },
        // 选中nav
        selectNav() {
            const path = this.$route.path;
            // 优先匹配最深的菜单项（关键：从子菜单开始倒序遍历！）
            for (let i = this.menuList.length - 1; i >= 0; i--) {
                const main = this.menuList[i];
                // 先检查所有子菜单（深度优先）
                if (main.children && main.children.length) {
                    for (let j = main.children.length - 1; j >= 0; j--) {
                        const child = main.children[j];
                        if (this.isMatch(child.path, path)) {
                            this.selectData = `${i}-${j}`;
                            return;
                        }
                    }
                }
                // 再检查主菜单自己
                if (this.isMatch(main.matchPaths || main.path, path)) {
                    this.selectData = `${i}`;
                    return;
                }
            }
            // 如果没有匹配子菜单，则匹配主菜单
            this.selectData = `${this.menuList.findIndex(item => this.$route.matched.some(d => d.path === item.path))}`;
        }
    },
    created() {
        this.pathNormal();
        this.$nextTick(() => {
            this.selectNav()
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
<style lang='scss' scoped>
.el-menu {
    background: transparent;
    border-right: none;

    .meun-item {
        display: flex;
        align-items: center;
        width: 90%;
        margin: 8px auto;
        height: auto;
        padding: 0 0 0 6px!important;
        line-height: 40px;
        text-align: center;

        &:hover {
            @extend .is-active;
        }
    }

    .meun-item.is-active {
        background: none;

        .menuText {
            color: #151719 !important;
        }
    }

    .el-menu-item {
        min-width: 0;

        &:hover {
            //@extend .is-active;
            background: none;

            .menuText {
                color: #151719;
            }
        }
    }

    .icon-container {
        height: 32px;
        width: 32px;
        border-radius: 50%;
        margin-right: 6px;
        display: inline-block;
        background-image: url("~@/assets/imgs/theme/icon.png");
        background-repeat: no-repeat;
    }

    .el-submenu.is-active {
        .menuText {
            color: #151719 !important;
        }
    }

    .el-submenu {
        margin: 8px auto;
        width: 90%;


        .sub-menu-item {
            display: flex;
            align-items: center;
            width: 90%;
            position: relative;
            margin: 8px auto;
            height: auto;
            line-height: 32px;
            text-align: center;
            padding-right: 0;
            padding-left: 10px !important;
        }


        .el-menu .sub-menu-item:hover {
            .menuText {
                color: #151719 !important;
            }
        }

        .el-menu .sub-menu-item .menuText {
            color: #515C73 !important;
        }

        .el-submenu.is-active .el-submenu__title {
            .menuText {
                color: #151719 !important;
            }
        }

        .el-menu .sub-menu-item.is-active {
            background: #fff;
            border-radius: 10px;

            .menuText {
                color: var(--color-main) !important;
            }
        }

        ::v-deep(.el-submenu__title) {
            margin: 0 auto;
            height: auto;
            //width: 90%;
            padding: 0 0 0 6px!important;
            line-height: 40px;
            display: flex;
            align-items: center;

            &:hover {
                background: none;

                .menuText {
                    color: #151719 !important;
                }
            }

            .el-submenu__icon-arrow {
                right: 0px;
                font-weight: bold;
                margin-top: -6px;
            }
        }

        ::v-deep(.el-menu--inline) {
            background: transparent;
            padding-left: 30px;
        }
    }
    //.is-active {
    //    background: #FFFFFF;
    //    color: #1787f0 !important;
    //    border-radius: 8px;
    //    box-shadow: 0px 1px 1px 0px rgba(116, 181, 251, 0.1), 0px 2px 2px 0px rgba(116, 181, 251, 0.09), 0px 5px 3px 0px rgba(116, 181, 251, 0.05), 0px 9px 4px 0px rgba(116, 181, 251, 0.01), 0px 15px 4px 0px rgba(116, 181, 251, 0);
    //}
    .meun-item-other {
        width: 100%;
        height: auto;
        padding-inline: 8px !important;
        background: transparent !important;
        box-shadow: none !important;
        border-radius: 0 !important;
        justify-content: center;

        &:hover {
            background: transparent !important;
            box-shadow: none !important;
            border-radius: 0 !important;
        }
    }

    .is-active .invite {
        color: #F8463D;
    }
}

.menuText {
    font-size: 14px !important;
    font-weight: 900;
    color: #515C73;
}

.suffix-icon {
    position: relative;
    overflow: initial;

    &::after {
        content: ' ';
        display: block;
        background-size: cover;
        background-position: center;
        position: absolute;
    }
}
.test-icon{
    &::after {
        height: 20px;
        width: 22px;
        right: -18px;
        top: -7px;
        background-image: url('~@/assets/imgs/test.png');
    }
}
.upgrade-icon {
    &::after {
        height: 20px;
        width: 32px;
        right: -29px;
        top: -7px;
        background-image: url('~@/assets/imgs/upgrade.png');
    }
}

.other-icon{
    &::after {
        height: 20px;
        width: 22px;
        right: -18px;
        top: -3px;
        background-image: url('~@/assets/imgs/2_5_8/jian.png');
    }
}

.menuImg {
    width: 22px;
    height: 22px;
    //font-size: 20px;
    //margin-right: 8px;
}


/* From Uiverse.io by abrahamcalsin */
.loaderRectangle {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 0 3px;
}

.invite {
    width: 116px;
    height: 42px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-image: url("~@/assets/imgs/activity/hyfl.png");
    background-size: 100% 100%;
    background-repeat: no-repeat;
}

.anniversary {
    width: 116px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-image: url("~@/assets/imgs/activity/znq.png");
    background-size: 100% 100%;
    background-repeat: no-repeat;
}

.loaderRectangle div {
    width: 2px;
    height: 10px;
    animation: .9s ease-in-out infinite;
    background: #FF5E7C;
    box-shadow: 0 0 20px rgba(18, 31, 53, 0.3);
}

.loaderRectangle div:nth-child(1) {
    animation-name: rectangleOneAnim;
    animation-delay: 1s;
}

@keyframes rectangleOneAnim {
    0% {
        height: 5px;
    }

    40% {
        height: 10px;
    }

    100% {
        height: 5px;
    }
}

.loaderRectangle div:nth-child(2) {
    animation-name: rectangleTwoAnim;
    animation-delay: 1.1s;
}

@keyframes rectangleTwoAnim {
    0% {
        height: 5px;
    }

    40% {
        height: 13px;
    }

    100% {
        height: 5px;
    }
}

.loaderRectangle div:nth-child(3) {
    animation-name: rectangleThreeAnim;
    animation-delay: 1.2s;
}

@keyframes rectangleThreeAnim {
    0% {
        height: 5px;
    }

    40% {
        height: 13px;
    }

    100% {
        height: 5px;
    }
}

.loaderRectangle div:nth-child(4) {
    animation-name: rectangleFourAnim;
    animation-delay: 1.3s;
}

@keyframes rectangleFourAnim {
    0% {
        height: 5px;
    }

    40% {
        height: 10px;
    }

    100% {
        height: 5px;
    }
}

.loaderRectangle div:nth-child(5) {
    animation-name: rectangleFiveAnim;
    animation-delay: 1.4s;
}

@keyframes rectangleFiveAnim {
    0% {
        height: 5px;
    }

    40% {
        height: 10px;
    }

    100% {
        height: 5px;
    }
}
</style>

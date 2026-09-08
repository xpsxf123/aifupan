<template>
    <div class="addContainer ">
        <div class="main-bg pd-16 flex-1">
            <!-- 各平台提示 -->
            <component :is="domName[getTabsName]" class="tipContainer" @showDyHelp="showDyHelp"></component>
            <!-- 添加提示 -->
            <div class="tipContainer">
                <div>小Tips：添加主播后，如主播开启直播，将会自动录制并分析</div>
                <div>您的会员套餐版本是:<span class="text-colorErr">{{ $store.getters.getPackageLevelName }}</span>，可添加主播数量:<span
                    class="text-colorErr">{{ getAnchorTotalNum }}</span></div>
                <div>
                    目前已经添加主播数量:<span class="text-colorErr">{{ addCompereNumber }}</span>个，目前还可以添加:<span
                    class="text-colorErr">{{ addAnhorNumber }}</span>个
                    <!-- <span>总添加主播数：</span>
                    <span style="color: red;">{{ addCompereNumber }}</span>
                    <span>，您还可以添加</span>
                    <span style="color: red;">{{(addAnhorNumber||0) }}</span>
                    <span>个主播</span> -->
                    <el-button type="text" @click="showQr">立即扩容</el-button>
                    <!-- <span style="margin-left: 15px;color: blue;cursor: pointer;" @click="showQr">立即扩容</span> -->
                </div>
            </div>
        </div>
        <div class="bodyContainer pd-16 main-bg flex-1">
            <tradeId v-model="tradeId" v-removeAriaHidden :options="tradeTreeList" style="width: 440px;"></tradeId>
            <!-- <el-cascader class="cascaderId" popper-class="select-trade-id" v-model="tradeId" v-removeAriaHidden :options="tradeTreeList" style="width: 440px;"
                :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name', emitPath: false }" filterable
                placeholder="为主播选择一个行业，以提高敏感词匹配分析准确性" @expand-change="expandChange" @change="changeTradeHandle" ref="tradeCascader">
                <template slot-scope="{ node, data }">
                    <span v-if="data.children?.length">{{ data.name }}</span>
                    <span v-else @click="expandChange(data.id)">{{ data.name }}</span>
                </template>
</el-cascader> -->
            <el-input type="textarea" :rows="15" resize="none" :placeholder="`ctr1+v粘贴直播间账号,为保证网络安全，一次仅能添加一个直播间
如果复制粘贴无法添加成功，请尝试手动输入`" v-model="broadcastUrls" style="margin-top: 10px;width: 540px;">
            </el-input>
            <afp-button type="primary" size="medium" style="margin-top: 16px;" @click="addCompere">添加到主播列表</afp-button>
        </div>

        <el-dialog title="帮助" top="10px" :visible.sync="dialogVisible" width="40%" :close-on-click-modal="true">
            <div style="max-height: 600px;overflow: hidden;overflow-y: auto;">
                <div v-html="helpHtml"></div>
            </div>
        </el-dialog>

        <!-- 客服弹窗 -->
        <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>
    </div>
</template>

<script>
import customerServiceQrCode from '../../commonComponent/customerServiceQrCode.vue';
import tabs from './../../../mixins/tabs';
import Douyin from './common/douyin.vue';
import Kuaishou from './common/kuaishou.vue';
import tradeId from '@/components/tradeId/index.vue'
export default {
    components: { customerServiceQrCode, Douyin, Kuaishou, tradeId },
    mixins: [tabs],
    data() {
        return {
            // tabs配置
            tabs: [
                { label: '抖音', name: 'douyin' },
                // { label: "快手", name: 'kuaishou' },
                // { label: "视频号", name: 'shipinhao' }
            ],
            domName: {
                douyin: 'Douyin',
                kuaishou: 'Kuaishou'
            },

            kefuDialogVisible: false,
            tradeId: '',
            tradeTreeList: [],
            dialogVisible: false,
            helpHtml: "",
            broadcastUrls: "",
            userProperty: {},
        };
    },

    mounted() {
        this.init()
    },
    activated() {
        this.init()
    },
    inject: ['appVnode'],
    computed: {
        getAnchorTotalNum() {
            return this.userProperty?.totalAnchorNum || 0;
        },
        addAnhorNumber() {
            return this.userProperty?.anchorNum < 0 ? 0 : this.userProperty?.anchorNum;
        },
        addCompereNumber() {
            return this.userProperty?.useAnchorNum;
            // let num = (this.getAnchorTotalNum ||0) - (this.addAnhorNumber||0);
            // return num < 0 ? 0 : num;
        }
    },
    methods: {
        init() {
            // 获取资产
            this.appVnode.getUserproperty((data => {
                this.userProperty = data;
            }));
            this.getTradeTreeList();
            this.tradeId = '';
            this.tradeTreeList = [];
            this.broadcastUrls = '';
        },
        showQr() {
            this.kefuDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },
        expandChange(data) {
            this.tradeId = data;
            if (Array.isArray(data)) {
                setTimeout(() => {
                    this.tradeId = data[data.length - 1];
                }, 50)
                return
            }
        },
        // 获取用户资产
        // getUserProperty() {
        //     this.$httpBack.userProperty.info({}).then((res) => {
        //         if (res && res.code === 0) {
        //             this.userProperty = res.data;
        //         }
        //     });
        // },

        // 选择行业回调
        changeTradeHandle() {
            // 关闭级联列表下拉
            this.$refs.tradeCascader.dropDownVisible = false;
        },

        // 获取行业列表树形
        getTradeTreeList() {
            this.tradeTreeList = [];
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data;
                }
            });
        },
        // 显示抖音帮助页
        showDyHelp() {
            this.$httpBack.article.list({ limit: -1, type: 0 }).then((res) => {
                if (res.code == 0 && res.data) {
                    this.helpHtml = res.data.list[0].content;
                    this.helpHtml = this.helpHtml.replaceAll("<img", "<img style='width: 100%'");
                    this.dialogVisible = true;
                } else {
                    this.$message.error("暂未支持");
                }
            })
        },
        // 跳转到添加主播页面
        toCompereList() {
            this.$router.push({
                path: '/dataAnalysis'
            })
        },
        addCompere() {
            this.$nextTick(() => {
                if (!this.tradeId) {
                    this.$message.error("请为主播选择一个行业");
                    return;
                }
                if (!this.broadcastUrls) {
                    this.$message.error("地址不能为空");
                    return;
                }
                // 去空格
                this.broadcastUrls = this.broadcastUrls.replaceAll(" ", "");
                if (!this.broadcastUrls) {
                    this.$message.error("地址不能为空");
                    return;
                }

                // let tempArr = this.broadcastUrls.split("\n");
                let tempArr = [this.broadcastUrls];
                tempArr = tempArr.filter(item => item);

                // 校验只能输入字母、数字、下划线和点
                const regex = /^[a-zA-Z0-9_.]+$/;
                let regexFlag = true;
                tempArr.forEach(item => {
                    if (!regex.test(item)) {
                        regexFlag = false;
                    }
                });

                if (!regexFlag) {
                    this.$message.error("请检查抖音号是否输入正确，抖音号只支持字母、数字、下划线和点");
                    return;
                }

                let arr = [];
                tempArr.forEach(item => {
                    if (item.indexOf("douyin.com") == -1) {
                        item = "https://live.douyin.com/" + item;
                    }
                    arr.push(item);
                });

                if (this.userProperty.anchorNum != -1 && arr.length > this.userProperty.anchorNum) {
                    this.$confirm('添加主播授权数量不足，是否立即扩容？', '提示', {
                        confirmButtonText: '确定',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }).then(() => {
                        this.showQr();
                    });
                    return;
                }

                let requestData = {
                    TradeId: this.tradeId,
                    Urls: arr
                }
                // 执行版本判断。
                this.appVnode.isVersionQrCode('init').then(() => {
                    this.$httpClient.compere.saveanchorinfo(requestData).then((res) => {
                        if (res.code == 0) {
                            this.toCompereList()
                        }
                    });
                })
            })
        }
    },
};
</script>
<style lang="scss" scoped>
.addContainer {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.bodyContainer {
    display: flex;
    margin-top: 16px;
    flex-direction: column;
    align-items: flex-start;
}

.tipContainer {
    font-size: 14px;
    color: #2E3742;
}

.cascaderId {
    ::v-deep(.el-radio__original) {
        display: none !important;
        /* 隐藏原生 radio 输入，但仍然允许交互 */
    }

    ::v-deep(.el-radio:focus:not(.is-focus):not(:active):not(.is-disabled) .el-radio__inner) {
        box-shadow: none !important;
    }
}
</style>
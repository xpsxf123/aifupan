<template>
    <div class="addContainer windon-box" style="padding: 20px;">
        <el-tabs v-model="currentMenuValue">
            <el-tab-pane v-for="item in menuList" :key="item.value" :label="item.label" :name="item.name"></el-tab-pane>
        </el-tabs>
        <div v-if="currentMenuValue == 'douyin'" class="tipContainer">
            <div>在下方粘贴要检测录制的主播账号，一行一个，支持以下格式：</div>
            <br />

            <!-- <div>1、直播账号的电脑网页直播页链接：https://live.douyin.com/xxxxxxxxxxxx</div> -->
            <div>
                <span>直播账号的抖音号：xxxxx(一般是一串数字或者英文字母,注意不是抖音昵称)</span>
                <span style="color: blue;cursor: pointer;margin-left: 10px;" @click="showDyHelp">(查看如何获取抖音号)</span>
            </div>
            <!-- <div>3、APP分享链接：https://v.douyin.com/xxxxxx</div> -->
        </div>
        <div v-if="currentMenuValue == 'kuaishou'" class="tipContainer">
            <div>在下方粘贴要检测录制的主播主页链接，一行一个，支持以下格式：</div>
            <br />
            <div>1、APP分享链接：https://v.kuaishou.com/xxxxxx</div>
            <div>2、直播账号的电脑网页直播页链接：https://live.kuaishou.com/u/xxxxxxxxx</div>
            <div>3、直播账号的电脑网页主页连接：https://live,kuaishou.com/profile/xxxxxxxxx</div>
        </div>
        <div class="tipContainer" style="margin-top: 30px;">
            <div>小Tips：添加主播后，如主播开启直播，将会自动录制并分析</div>
            <div>
                <span>总添加主播数：</span>
                <span style="color: red;">{{ userProperty?.anchorNum == -1 ? '无限制' : userProperty?.totalAnchorNum -
                    userProperty?.anchorNum }}</span>
                <span>，您还可以添加</span>
                <span style="color: red;">{{ userProperty?.anchorNum == -1 ? '无限制' : userProperty?.anchorNum }}</span>
                <span>个主播</span>
                <span style="margin-left: 15px;color: blue;cursor: pointer;" @click="showQr">立即扩容</span>
            </div>
        </div>
        <div class="bodyContainer">

            <el-cascader v-model="tradeIdArr" :options="tradeTreeList" style="width: 440px;"
                :props="{ checkStrictly: true, expandTrigger: 'click', value: 'id', label: 'name' }" filterable
                placeholder="为主播选择一个行业，以提高敏感词匹配分析准确性" @change="changeTradeHandle" ref="tradeCascader">
            </el-cascader>

            <el-input type="textarea" :rows="15" placeholder="ctr1+v粘贴直播间账号，一行一个" v-model="broadcastUrls"
                style="margin-top: 10px;">
            </el-input>
            <afp-button type="primary" size="medium" style="margin-top: 16px;" @click="addCompere">添加到主播列表</afp-button>
        </div>

        <el-dialog title="帮助" :visible.sync="dialogVisible" width="40%" :close-on-click-modal="false">
            <div v-html="helpHtml"></div>
        </el-dialog>

        <!-- 客服弹窗 -->
        <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>
    </div>
</template>

<script>
import customerServiceQrCode from '../../commonComponent/customerServiceQrCode.vue';
export default {
    components: { customerServiceQrCode },
    data() {
        return {
            kefuDialogVisible: false,
            tradeIdArr: [],
            tradeTreeList: [],
            dialogVisible: false,
            helpHtml: "",
            broadcastUrls: "",
            currentMenuValue: 'douyin',
            menuList: [
                { label: "抖音", name: 'douyin' },
                // { label: "快手", name: 'kuaishou' },
                // { label: "视频号", name: 'shipinhao' }
            ],
            userProperty: {
                monitorNum: 0,
                aiAnalysisTime: 0,
                anchorNum: 0,
                totalMonitorNum: 0,
                totalAiAnalysisTime: 0,
                totalAnchorNum: 0
            },
        };
    },

    mounted() {
        this.tradeIdArr = [];
        this.tradeTreeList = [];
        this.getUserProperty();
        this.getTradeTreeList();
    },

    methods: {
        showQr() {
            this.kefuDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },
        // 获取用户资产
        getUserProperty() {
            this.$httpBack.userProperty.info({}).then((res) => {
                if (res && res.code === 0) {
                    this.userProperty = res.data;
                }
            });
        },
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
            this.$emit('updateMenuIndex', 0);
        },
        // 跳转到设置页面
        toSetup() {
            this.$store.commit("saveSetupMenu", "customerService");
            this.$emit('updateMenuIndex', 4);
        },
        addCompere() {
            if (!this.tradeIdArr || this.tradeIdArr.length < 1) {
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

            let tempArr = this.broadcastUrls.split("\n");
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
                    // this.toSetup();
                    this.showQr();
                });
                return;
            }

            let requestData = {
                TradeId: this.tradeIdArr[this.tradeIdArr.length - 1],
                Urls: arr
            }

            this.$httpClient.compere.saveanchorinfo(requestData).then((res) => {
                if (res.code == 0) {
                    this.toCompereList();
                }
            });

        }
    },
};
</script>
<style scoped>
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
</style>

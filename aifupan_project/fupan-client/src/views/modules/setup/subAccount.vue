<template>
    <div style="height: 100%">
        <div class="sub-account-header">
            <div>绑定子账号数：{{ userProperty.useSubAccountCount + '/' + userProperty.totalSubAccountCount }}</div>
            <div style="margin-left: 30px;">
                <afp-button size="medium" @click="addOrupdateSubAccount(0)">添加子账号</afp-button>
            </div>
        </div>

        <!-- <CoreTable></CoreTable> -->

        <el-table height="calc(100% - 20px)" :data="subList" stripe>
            <!--            <el-table-column prop="phone" label="手机号码" align="center">-->
            <!--            </el-table-column>-->
            <!--            <el-table-column prop="username" label="用户名" align="center">-->
            <!--            </el-table-column>-->
            <el-table-column prop="nickName" label="昵称" align="center">
            </el-table-column>
            <el-table-column prop="anchorCount" label="录制主播数" align="center" width="100">
                <template slot-scope="{row:item}">
                    <span class="cursor-pointer" style="color: var(--color-main)"
                          @click="()=>openSubView(item,'first')"
                    >{{ item.anchorCount }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="yesterdayVideoCount" label="昨日录制场次" align="center" width="110">
                <template slot-scope="{row:item}">
                    <span class="cursor-pointer" style="color: var(--color-main)"
                          @click="()=>openSubView(item,'second')"
                    >{{ item.yesterdayVideoCount }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="yesterdayNotesCount" label="昨日小结" align="center" width="90">
                <template slot-scope="{row:item}">
                    <span class="cursor-pointer" style="color: var(--color-main)"
                          @click="()=>openSubView(item,'third')"
                    >{{ item.yesterdayNotesCount }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="yesterdayResourceConsumption" label="昨日消耗" align="center">
                <template slot-scope="{row:item}">
                    <div v-if="item.yesterdayResourceConsumption?.length">
                        <div v-for="_item in item.yesterdayResourceConsumption" :key="_item.commodityTypeId">
                            <span>{{ _item.commodityTypeName }}
                                {{ _item.consumptionQuantity }}
                                {{ _item.commodityTypeUnit }}
                            </span>
                        </div>
                    </div>
                    <div v-else>-</div>
                </template>
            </el-table-column>
            <el-table-column prop="videoCount" label="本月录制场次" align="center"  width="110">
            </el-table-column>
            <el-table-column prop="monthlyResourceConsumption" label="本月消耗" align="center">
                <template slot-scope="{row:item}">
                    <div v-if="item.monthlyResourceConsumption?.length">
                        <div v-for="_item in item.monthlyResourceConsumption" :key="_item.commodityTypeId">
                            <span>
                                {{ _item.commodityTypeName }}
                                {{ _item.consumptionQuantity }}
                                {{ _item.commodityTypeUnit }}
                            </span>
                        </div>
                    </div>
                    <div v-else>-</div>
                </template>
            </el-table-column>
            <!--            <el-table-column prop="createDate" label="添加时间" align="center">-->
            <!--            </el-table-column>-->
            <el-table-column prop="operate" label="操作" align="center">
                <template slot-scope="scope">
                    <afp-button type="danger" @click="unbinding(scope.row.id)"
                                :disabled="scope.row?.userType === 0||(scope.row?.userType === 2&&getUserInfo.id==scope.row.id)">
                        解绑
                    </afp-button>
                    <el-dropdown style="margin-left: 12px" @command="(command)=>handleDropdown(command,scope.row)">
                        <span class="el-dropdown-link">
                            <el-button type="text" size="mini">更多</el-button>
                        </span>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item command="view">查看分析</el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                </template>
            </el-table-column>
        </el-table>
        <!--        <el-pagination style="text-align: center; margin-top: 10px" @size-change="sizeChangeHandle"-->
        <!--                       @current-change="currentChangeHandle" :current-page="pageIndex" :page-sizes="[10, 20, 50, 100]"-->
        <!--                       :page-size="pageSize" :total="totalCount" layout="total, sizes, prev, pager, next, jumper">-->
        <!--        </el-pagination>-->

        <subAcountDialog v-if="subAcountDialogVisible" ref="subAcountDialog" @refreshDataList="getSubList">
        </subAcountDialog>
        <SubVideoInfo ref="sub_video_info"/>
    </div>
</template>

<script>
import subAcountDialog from './subAcountDialog.vue';
import CoreTable from '@/components/coreTable'
import SubVideoInfo from './component/subVideoInfo.vue'

export default {
    components: {
        subAcountDialog,
        CoreTable,
        SubVideoInfo
    },
    data() {
        return {
            subAcountDialogVisible: false,
            subList: [],
            pageIndex: 1,
            pageSize: 10,
            totalCount: 0,
            dataForm: {},
            currentTab: 'first',
            userProperty: {
                totalSubAccountCount: 0
            }
        }
    },
    computed:{
        getUserInfo(){
            return this.$store.getters.getUserInfo;
        },
    },
    created() {
        this.getUserProperty();
        this.getSubList();
    },
    methods: {
        // 获取子账号列表
        getSubList() {
            this.dataForm.page = this.pageIndex;
            this.dataForm.limit = this.pageSize;
            this.$httpBack.subAccount.clientGetSubUserList().then(res => {
                if (res.code === 0) {
                    this.subList = res.data ? res.data : [];
                    // this.totalCount = res.data?.totalCount;
                } else {
                    this.dataList = [];
                    // this.totalCount = 0;
                }
            })
        },
        // 获取用户资产信息
        getUserProperty() {
            this.$httpBack.userProperty.info({}).then((res) => {
                if (res && res.code === 0 && res.data) {
                    this.userProperty = res.data;
                }
            });
        },
        addOrupdateSubAccount(id) {
          this.subAcountDialogVisible = true;
          if (Number(this.userProperty.useSubAccountCount||'0') >= Number(this.userProperty.totalSubAccountCount||'0')) {
                // this.$message.error("子账号授权数量不足");
                // return;
                this.$nextTick(() => {
                    this.$refs.subAcountDialog.lack()
                })
            } else {
                this.$nextTick(() => {
                    this.$cMsg.customConfirm({
                        message: '绑定成子账号后，子账号原有的数据将无法查看，请做好子账号原数据的保存，本操作不影响主账号数据。',
                        confirmButtonText: '确认',
                        cancelButtonText: '取消',
                        customClass: 'sub-account-confirm confirm-btns-center',
                    }).then(() => {
                        this.$refs.subAcountDialog.init(id);
                    })
                })
            }

        },
        unbinding(id) {
            this.$confirm('将与该账号解除绑定关系, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.$httpBack.subAccount.unBind({subUserId: id}).then(res => {
                    if (res.code === 0) {
                        this.$message.success("解绑成功");
                        this.getSubList();
                    }
                })
            })
        },
        // 每页数
        sizeChangeHandle(val) {
            this.pageSize = val
            this.pageIndex = 1
            this.getSubList()
        },
        // 当前页
        currentChangeHandle(val) {
            this.pageIndex = val
            this.getSubList()
        },
        openSubView(item,tab){
            this.$refs.sub_video_info?.changeStatus(true, item,tab)
        },
        handleDropdown(command,item) {
            if (command === 'view') {
                this.$refs.sub_video_info?.changeStatus(true,item)
            }
        }
    }
}
</script>

<style>
.sub-account-header {
    display: flex;
    align-items: center;
}

.sub-account-confirm {
    width: 420px !important;
}

</style>
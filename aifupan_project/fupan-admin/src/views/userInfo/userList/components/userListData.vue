<template>
  <div class="user-list-container">
    <div>
      <div class="search-form">
        <el-form :inline="true" :model="form">
          <el-form-item label="用户：">
            <el-input
                v-model="form.keyword"
                clearable
                placeholder="昵称/手机号搜索"
                style="width: 140px"
            ></el-input>
          </el-form-item>
          <el-form-item label="微信昵称：">
            <el-input
                v-model="form.userWxName"
                clearable
                placeholder="微信昵称搜索"
                style="width: 140px"
            ></el-input>
          </el-form-item>
          <el-form-item label="公司：">
            <el-input
                v-model="form.companyName"
                clearable
                placeholder="输入公司名称"
                style="width: 140px"
            ></el-input>
          </el-form-item>
          <el-form-item label="归属行业：">
            <!--        <el-input v-model="form.tradeName" placeholder="输入行业名称" style="width: 170px;" clearable></el-input>-->
            <el-cascader
                :key="cascaderNum"
                v-model="form.tradeId"
                :options="tradeTreeList"
                :props="{
                checkStrictly: true,
                value: 'id',
                label: 'name',
                emitPath: false,
              }"
                clearable
                filterable
                placeholder="选择归属行业"
                style="width: 140px"
                @change="tradeChange"
            >
            </el-cascader>
          </el-form-item>
          <el-form-item label="版本：">
            <el-select
                v-model="form.packageId"
                clearable
                placeholder="选择版本"
                style="width: 140px"
            >
              <el-option
                  v-for="item in packageIdS"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
              >
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="是否登录过：">
            <el-select
                v-model="form.isLoggedIn"
                clearable
                placeholder="是否已登录过"
                style="width: 140px"
            >
              <el-option :value="1" label="已登录过"/>
              <el-option :value="0" label="未登录过"/>
            </el-select>
          </el-form-item>
          <el-form-item label="跟进销售：">
            <el-select
                v-model="form.salesId"
                clearable
                placeholder="选择跟进销售"
                style="width: 140px"
            >
              <el-option
                  v-for="item in salesList"
                  :key="item.id"
                  :label="item.salesName"
                  :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="付费到期时间(≤)：">
            <el-select
                v-model="form.expireTime"
                clearable
                placeholder="到期时间"
                style="width: 140px"
            >
              <el-option
                  v-for="item in expireTimeList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="日平均分析条数：">
            <div style="width: 240px; display: flex; align-items: center">
              <div style="flex: 1">
                <el-input
                    v-model="form.startDayAnalysis"
                    :min="1"
                    clearable
                    placeholder="开始值"
                    type="number"
                />
              </div>
              <div style="text-align: center; width: 25px">至</div>
              <div style="flex: 1">
                <el-input
                    v-model="form.endDayAnalysis"
                    :min="1"
                    clearable
                    placeholder="结束值"
                    type="number"
                />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="多久未分析：">
            <div style="width: 240px; display: flex; align-items: center">
              <div style="flex: 1">
                <el-input
                    v-model="form.startLongNotAnalysis"
                    :min="1"
                    clearable
                    placeholder="开始值"
                    type="number"
                />
              </div>
              <div style="text-align: center; width: 25px">至</div>
              <div style="flex: 1">
                <el-input
                    v-model="form.endLongNotAnalysis"
                    :min="1"
                    clearable
                    placeholder="结束值"
                    type="number"
                />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="注册时间：">
            <el-date-picker
                v-model="form.startEndDate"
                :default-time="[
                new Date(0, 0, 0, 0, 0, 0),
                new Date(0, 0, 0, 23, 59, 59),
              ]"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                range-separator="至"
                start-placeholder="开始日期"
                style="width: 260px"
                type="daterange"
                value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="来源渠道：">
            <el-cascader
                :key="channelKey"
                v-model="form.channelId"
                :options="channelOptions"
                :props="{
                checkStrictly: true,
                value: 'id',
                label: 'channelName',
                emitPath: false,
              }"
                class="channel-class"
                clearable
                filterable
                placeholder="选择渠道筛选"
                style="width: 140px"
                @change="channelChange"
            ></el-cascader>
          </el-form-item>
          <el-form-item label="客户类型：">
            <el-select
                v-model="form.userBelongType"
                clearable
                placeholder="选择客户类型"
                style="width: 140px"
            >
              <el-option
                  v-for="item in customerTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="客户意向：">
            <el-select
                v-model="form.userAmbition"
                class="custom-select"
                clearable
                placeholder="选择客户意向"
                style="width: 140px"
            >
              <el-option
                  v-for="item in intentionOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="自用抖音号数量(≥)：">
            <el-input-number
                v-model="form.ownCount"
                label="自用抖音号数量"
                placeholder="自用抖音号数量"
                style="width: 190px"
            ></el-input-number>
          </el-form-item>
          <el-form-item label="是否为子账号：">
            <el-select
                v-model="form.userType"
                clearable
                placeholder="是否为子账号"
                style="width: 140px"
            >
              <el-option :value="2" label="是"></el-option>
              <el-option :value="0" label="否"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="是否为付费用户：">
            <el-select
                v-model="form.trialOrder"
                clearable
                placeholder="是否为付费用户"
                style="width: 145px"
            >
              <el-option :value="0" label="正式版"></el-option>
              <el-option :value="1" label="试用版"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="渠道明细：">
            <el-input
                v-model="form.promotionName"
                clearable
                placeholder="渠道明细"
                style="width: 140px"
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button
                icon="Search"
                plain
                style="margin-right: 15px"
                type="primary"
                @click="search()"
            >查询
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button
                v-if="admin"
                icon="el-icon-delete"
                type="danger"
                @click="deleteHandle()"
            >删除
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-table
          v-loading="dataListLoading"
          :data="dataList"
          :element-loading-spinner="customSvg"
          border
          header-row-class-name="my-header-row"
          size="default"
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
      >
        <el-table-column
            v-if="admin"
            align="center"
            header-align="center"
            label="选择"
            type="selection"
            width="40"
        ></el-table-column>
        <el-table-column
            :fixed="!isMobile"
            :width="100"
            align="center"
            header-align="center"
            label="微信昵称"
            prop="wxName"
            show-overflow-tooltip
        />
        <el-table-column
            :fixed="!isMobile"
            :width="120"
            align="center"
            header-align="center"
            label="手机号"
            prop="phone"
            show-overflow-tooltip
        />
        <el-table-column
            :fixed="!isMobile"
            :width="110"
            align="center"
            header-align="center"
            label="来源渠道"
            prop="channelName"
            show-overflow-tooltip
        />
        <el-table-column
            :width="110"
            align="center"
            header-align="center"
            label="渠道明细"
            prop="promotionName"
            show-overflow-tooltip
        />
        <el-table-column
            :width="100"
            align="center"
            header-align="center"
            label="版本"
            prop="packageName"
            show-overflow-tooltip
        >
          <template #default="scope">
            <span v-if="scope.row.packageId">{{ scope.row.packageName }}</span>
            <span v-else>
              <el-button type="text" @click="addNowPackage(scope.row)"
              >添加激活版</el-button
              >
            </span>
          </template>
        </el-table-column>
        <el-table-column
            :width="100"
            align="center"
            header-align="center"
            label="账号类型"
            prop="userType"
            show-overflow-tooltip
        >
          <template #default="scope">
            {{ getLabel(userDict.userType, scope.row.userType) }}
          </template>
        </el-table-column>
        <el-table-column
            :formatter="formatterIsLoggedIn"
            :width="100"
            align="center"
            header-align="center"
            label="是否登录过"
            prop="isLoggedIn"
            show-overflow-tooltip
        >
          <template #default="{ row }">
            <span v-if="row.isLoggedIn === 1" style="color: #67c23a"
            >已登录过</span
            >
            <span v-else style="color: #f56c6c">未登录过</span>
          </template>
        </el-table-column>
        <el-table-column
            :width="100"
            align="center"
            header-align="center"
            label="跟进销售"
            prop="salesName"
            show-overflow-tooltip
        />
        <el-table-column
            :width="100"
            align="center"
            header-align="center"
            label="归属行业"
            prop="tradeName"
            show-overflow-tooltip
        />
        <el-table-column
            :width="170"
            align="center"
            header-align="center"
            label="自用抖音号数量"
            prop="ownCount"
            show-overflow-tooltip
        >
          <template #header="">
            <div class="header-with-icon">
              <span>自用抖音号数量</span>
              <div
                  style="display: flex; flex-direction: column; margin-left: 10px"
              >
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="升序排列"
                    effect="dark"
                    placement="top-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_up.png"
                      style="width: 50%"
                      @click="handleSort(10)"
                  />
                </el-tooltip>
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="降序排列"
                    effect="dark"
                    placement="bottom-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_down.png"
                      style="width: 50%"
                      @click="handleSort(11)"
                  />
                </el-tooltip>
              </div>
            </div>
          </template>
          <template #default="{ row }">
            <span>{{ row.ownCount ? row.ownCount + '个' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column
            :width="100"
            align="center"
            header-align="center"
            label="客户意向"
            prop="userAmbition"
            show-overflow-tooltip
        >
          <template #default="{ row }">
            <span>{{ row.userAmbition ? row.userAmbition + '级' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column
            :formatter="formatterUserBelongType"
            :width="100"
            align="center"
            header-align="center"
            label="客户类型"
            prop="userBelongType"
            show-overflow-tooltip
        />
        <el-table-column
            :formatter="formatterTrialOrder"
            :width="120"
            align="center"
            header-align="center"
            label="是否付费用户"
            prop="trialOrder"
            show-overflow-tooltip
        />
        <el-table-column
            :width="180"
            align="center"
            header-align="center"
            label="注册时间"
            prop="createDate"
            show-overflow-tooltip
        />
        <el-table-column
            :width="210"
            align="center"
            header-align="center"
            label="最高版本付费到期时间"
            prop="expireTime"
            show-overflow-tooltip
        >
          <template #header="">
            <div class="header-with-icon">
              <span>最高版本付费到期时间</span>
              <div
                  style="display: flex; flex-direction: column; margin-left: 10px"
              >
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="升序排列"
                    effect="dark"
                    placement="top-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_up.png"
                      style="width: 50%"
                      @click="handleSort(0)"
                  />
                </el-tooltip>
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="降序排列"
                    effect="dark"
                    placement="bottom-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_down.png"
                      style="width: 50%"
                      @click="handleSort(1)"
                  />
                </el-tooltip>
              </div>
            </div>
          </template>
          <template #default="scope">
            <span v-if="scope.row.expireTime"
            >{{ scope.row.expireTime }}天</span
            >
            <span v-else>暂无付费套餐</span>
          </template>
        </el-table-column>
        <el-table-column
            :width="140"
            align="center"
            header-align="center"
            label="总分析条数"
            prop="sumAnalysis"
        >
          <template #header="">
            <div class="header-with-icon">
              <span>总分析条数</span>
              <div
                  style="display: flex; flex-direction: column; margin-left: 10px"
              >
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="升序排列"
                    effect="dark"
                    placement="top-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_up.png"
                      style="width: 50%"
                      @click="handleSort(2)"
                  />
                </el-tooltip>
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="降序排列"
                    effect="dark"
                    placement="bottom-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_down.png"
                      style="width: 50%"
                      @click="handleSort(3)"
                  />
                </el-tooltip>
              </div>
            </div>
          </template>
          <template #default="scope"> {{ scope.row.sumAnalysis }}条</template>
        </el-table-column>
        <el-table-column
            :width="170"
            align="center"
            header-align="center"
            label="日平均分析条数"
            prop="dayAnalysis"
        >
          <template #header="">
            <div class="header-with-icon">
              <span>日平均分析条数</span>
              <div
                  style="display: flex; flex-direction: column; margin-left: 10px"
              >
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="升序排列"
                    effect="dark"
                    placement="top-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_up.png"
                      style="width: 50%"
                      @click="handleSort(4)"
                  />
                </el-tooltip>
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="降序排列"
                    effect="dark"
                    placement="bottom-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_down.png"
                      style="width: 50%"
                      @click="handleSort(5)"
                  />
                </el-tooltip>
              </div>
            </div>
          </template>
          <template #default="scope"> {{ scope.row.dayAnalysis }}条</template>
        </el-table-column>
        <el-table-column
            :width="160"
            align="center"
            header-align="center"
            label="对比分析条数"
            prop="contrastAnalysis"
        >
          <template #header="">
            <div class="header-with-icon">
              <span>对比分析条数</span>
              <div
                  style="display: flex; flex-direction: column; margin-left: 10px"
              >
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="升序排列"
                    effect="dark"
                    placement="top-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_up.png"
                      style="width: 50%"
                      @click="handleSort(8)"
                  />
                </el-tooltip>
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="降序排列"
                    effect="dark"
                    placement="bottom-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_down.png"
                      style="width: 50%"
                      @click="handleSort(9)"
                  />
                </el-tooltip>
              </div>
            </div>
          </template>
          <template #default="scope">
            {{ scope.row.contrastAnalysis }}条
          </template>
        </el-table-column>
        <el-table-column
            :width="140"
            align="center"
            header-align="center"
            label="多久未分析"
            prop="longNotAnalysis"
        >
          <template #header="">
            <div class="header-with-icon">
              <span>多久未分析</span>
              <div
                  style="display: flex; flex-direction: column; margin-left: 10px"
              >
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="升序排列"
                    effect="dark"
                    placement="top-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_up.png"
                      style="width: 50%"
                      @click="handleSort(6)"
                  />
                </el-tooltip>
                <el-tooltip
                    :enterable="false"
                    class="item"
                    content="降序排列"
                    effect="dark"
                    placement="bottom-start"
                >
                  <img
                      class="hover-style"
                      src="@/assets/images/word_list_down.png"
                      style="width: 50%"
                      @click="handleSort(7)"
                  />
                </el-tooltip>
              </div>
            </div>
          </template>
          <template #default="scope">
            <span v-if="scope.row.longNotAnalysis" style="color: #67c23a"
            >{{ scope.row.longNotAnalysis }}天</span
            >
            <span v-else style="color: #f56c6c">从未分析</span>
          </template>
        </el-table-column>
        <el-table-column
            :width="150"
            align="center"
            header-align="center"
            label="状态"
            prop="status"
        >
          <template #default="scope">
            <el-switch
                v-model="scope.row.status"
                :active-value="0"
                :inactive-value="1"
                active-text="正常"
                inactive-text="冻结"
                @change="handleSwitchChange(scope.row)"
            >
            </el-switch>
          </template>
        </el-table-column>
        <el-table-column
            :width="150"
            align="center"
            header-align="center"
            label="昵称"
            prop="nickName"
            show-overflow-tooltip
        />
        <el-table-column
            :width="280"
            align="center"
            class-name="operate-column"
            fixed="right"
            header-align="center"
            label="操作"
        >
          <template #default="scope">
            <el-button
                class="operate-btn"
                size="small"
                type="text"
                @click="addOrUpdateHandle(scope.row.id)"
            >详情
            </el-button>
            <el-button
                :disabled="scope.row.packageLevel <= 0 || scope.row.userType == 2"
                class="operate-btn"
                size="small"
                type="text"
                @click="handleUserRenewal(scope.row)"
            >续费
            </el-button>
            <el-button
                :disabled="scope.row.userType == 2"
                class="operate-btn"
                size="small"
                type="text"
                @click="packageUpgrade(scope.row)"
            >版本升级
            </el-button>
            <el-button
                :disabled="scope.row.userType == 2"
                class="operate-btn"
                size="small"
                type="text"
                @click="handleIncrementBuy(scope.row)"
            >购买增量包
            </el-button>
            <el-button
                class="operate-btn"
                size="small"
                style="color: #f56c6c"
                type="text"
                @click="resetPassword(scope.row.id)"
            >重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <!--密码提示弹窗-->
      <el-dialog
          v-model="passWordDialogVisible"
          title="密码提示"
          width="500px"
      >
        <p>密码已成功重置为 <span class="rest-password-class">{{ restPassWord }}</span> ，请及时修改密码！</p>
        <template #footer>
          <div class="dialog-footer">
            <el-button type="primary" @click="handleKnow">
              我已知晓
            </el-button>
          </div>
        </template>
      </el-dialog>
      <pagination
          v-model:limit="form.limit"
          v-model:page="form.page"
          :background="true"
          :small="isMobile"
          :total="totalCount"
          layout="total, sizes, prev, pager,next,->, jumper"
          @change="getDataList"
      />
      <!--版本升级-->
      <purchase-package
          v-if="purchasePackageShow"
          ref="purchasePackageRef"
          :isMobile="isMobile"
          @close="purchasePackageShow = false"
          @is-ok="getDataList"
      />
      <!--续费-->
      <userRenewal
          v-if="userRenewalShow"
          ref="userRenewalRef"
          :isMobile="isMobile"
          @close="userRenewalShow = false"
          @is-ok="getDataList"
      />
      <!--购买增量包-->
      <incrementBuy
          v-if="incrementBuyShow"
          ref="incrementBuyRef"
          :isMobile="isMobile"
          @close="incrementBuyShow = false"
          @is-ok="getDataList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PurchasePackage from '@/views/userInfo/userList/indexCopy/purchasePackage.vue'
import userRenewal from '@/views/userInfo/userList/indexCopy/userRenewal.vue'
import incrementBuy from '@/views/userInfo/userList/indexCopy/incrementBuy.vue'
import pagination from '@/components/commonComponent/pagination.vue'
import emitter from '@/utils/emitter.js'
import { customSvg } from '@/utils/icon.js'
import { useDict } from '@/hooks/useDict.js'
import api from '@/utils/request-api'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
import { ElMessageBox } from 'element-plus'
// 响应式变量定义
const route = useRoute()
const router = useRouter()
const {getLabel} = useDict()
const {isMobile} = storeToRefs(useSystemInfoStore())
const form = reactive({
  userType: null,
  trialOrder: null
})

const channelKey = ref(1)
const channelOptions = ref([])
const purchasePackageShow = ref(false)
const userRenewalShow = ref(false)
const incrementBuyShow = ref(false)
const selectedRows = ref([])
const dataList = ref([])
const totalCount = ref(0)
const dataListLoading = ref(false)
const tradeTreeList = ref([])
const cascaderNum = ref(0)
const admin = ref(0)
const packageIdS = ref([])
const salesList = ref([])
const searchUpOrDown = ref(0)
let passWordDialogVisible = ref(false)
let restPassWord = ref('')
const expireTimeList = ref([
  {id: 1, name: '30天', value: 30},
  {id: 2, name: '15天', value: 15},
  {id: 3, name: '7天', value: 7},
  {id: 4, name: '3天', value: 3}
])

const customerTypeOptions = ref([
  {value: 0, label: '个人'},
  {value: 1, label: '工作室'},
  {value: 2, label: '企业'}
])

const intentionOptions = ref([
  {value: 'S', label: 'S 级'},
  {value: 'A', label: 'A 级'},
  {value: 'B', label: 'B 级'},
  {value: 'C', label: 'C 级'}
])

const userDict = ref({
  userType: []
})

const userRenewalRef = ref(null)
const incrementBuyRef = ref(null)
const purchasePackageRef = ref(null)

// 函数
const tradeChange = (val) => {
  if (!val) {
    cascaderNum.value++
  }
}

const channelChange = (val) => {
  if (!val) {
    channelKey.value++
  }
}

const init = () => {
  admin.value = route?.query?.admin ?? 0
  getDataList()
  getTradeTreeList()
  getPackageList()
  getSalesList()
  getChannelTreeList()
}

const getChannelTreeList = async () => {
  const res = await api.channel.listTree({childrenNotNull: 1})
  if (res.code === 0 && Array.isArray(res.data)) {
    channelOptions.value = cleanEmptyChildren(res.data)
  }
}

const getPackageList = async () => {
  packageIdS.value = []
  const res = await api.package.list({packageType: 1, limit: -1})
  if (res.data && res.code == 0) {
    packageIdS.value = res.data.list
  }
}

const getSalesList = async () => {
  salesList.value = []
  const res = await api.sales.list({limit: -1})
  if (res && res.code === 0) {
    salesList.value = res.data.list
  }
}

const getTradeTreeList = async () => {
  tradeTreeList.value = []
  const res = await api.trade.listTree({})
  if (res && res.code === 0) {
    tradeTreeList.value = cleanEmptyChildren(res.data)
    cascaderNum.value++
  }
}

const addNowPackage = async (row) => {
  const res = await api.order.newPcCreateOrder({userId: row.id})
  if (res?.code == 0) {
    ElMessage.success('处理成功')
    await getDataList()
  }
}

const changeSubAccount = (item) => {
  if (item.userType == 2) {
    ElMessage.error('子账号不能购买版本或者增量包')
    throw new Error('子账号不能购买版本或者增量包')
  }
}

const handleIncrementBuy = (item) => {
  changeSubAccount(item)
  incrementBuyShow.value = true
  nextTick(() => {
    incrementBuyRef.value.init(item.id)
  })
}

const handleUserRenewal = (item) => {
  changeSubAccount(item)
  userRenewalShow.value = true
  nextTick(() => {
    userRenewalRef.value.packageUpgrade(item.id)
  })
}

const packageUpgrade = (item) => {
  changeSubAccount(item)
  purchasePackageShow.value = true
  nextTick(() => {
    purchasePackageRef.value.packageUpgrade(item.id)
  })
}

const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const handleSwitchChange = async (form) => {
  try {
    await ElMessageBox.confirm('确定要修改该用户状态吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    // 取消后回滚开关状态
    form.status = form.status === 0 ? 1 : 0
    ElMessage.info('已取消操作')
    return
  }

  const data = {userId: form.id, status: form.status}
  const res = await api.user.updateUser(data)
  if (res && res.code === 0) {
    ElMessage.success('操作成功')
    await getDataList()
  }
}

const resetPassword = async (id) => {
  await ElMessageBox.confirm('是否重置密码?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.user.resetPassword({id})
  if (res && res.code === 0) {
    restPassWord.value = res.data
    passWordDialogVisible.value = true

  }
}
const handleKnow = () => {
  passWordDialogVisible.value = false
  ElMessage.success('密码重置成功')
}
const search = () => {
  form.page = 1
  form.specialSorting = 0
  getDataList()
}

const expireTimeSearch = () => {
  form.specialSorting = 1
  form.searchUpOrDown = searchUpOrDown.value
  getDataList()
}

const handleSort = (sortType) => {
  searchUpOrDown.value = sortType
  expireTimeSearch()
}

const getDataList = async () => {
  dataListLoading.value = true
  if (form.startEndDate?.length > 1) {
    form.startTime = form.startEndDate[0]
    form.endTime = form.startEndDate[1]
  } else {
    form.endTime = null
    form.startTime = null
  }
  try {
    const res = await api.user.pageList(form)
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
  } catch (error) {
    dataList.value = []
    totalCount.value = 0
  }
  dataListLoading.value = false
}

const addOrUpdateHandle = (userId) => {
  const resolved = router.resolve({
    query: {
      userId,
      componentName: 'userDetail'
    }
  })
  const url = window.location.origin + resolved.href
  window.open(url, '_blank')
  if (isMobile.value) {
    setTimeout(() => {
      window.open(url, '_blank')
    }, 100)
  }
}

const deleteHandle = async () => {
  if (selectedRows.value?.length <= 0) {
    ElMessage.error('请选择用户')
    return
  }
  const ids = selectedRows.value.map((row) => row.id)
  await ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.user.deleteByIds(ids)
  if (res && res.code === 0) {
    ElMessage.success(res.msg)
    await getDataList()
  }
}

const formatterUserBelongType = (row, column, cellValue, index) => {
  const map = {
    0: '个人',
    1: '工作室',
    2: '企业'
  }
  return map[cellValue] ?? ''
}

const formatterTrialOrder = (row, column, cellValue, index) => {
  const map = {
    1: '试用',
    0: '正式版'
  }
  return map[cellValue] ?? ''
}

const formatterIsLoggedIn = (row, column, cellValue, index) => {
  const map = {
    0: '未登录过',
    1: '已登录过'
  }
  return map[cellValue] ?? ''
}

const cleanEmptyChildren = (data) => {
  if (!Array.isArray(data)) return data
  return data.map((item) => {
    if (item.children && Array.isArray(item.children)) {
      item.children = cleanEmptyChildren(item.children)
      if (item.children.length === 0) {
        delete item.children
      }
    }
    return {...item}
  })
}

// 生命周期
onMounted(() => {
  init()
  emitter.on('refreshData', getDataList)
})

onBeforeUnmount(() => {
  emitter.off('refreshData', getDataList)
})

// Expose (defineExpose)
// 当前组件没有需要暴露的方法或属性
</script>
<style lang="scss" scoped>
.user-list-container {
  padding: 15px;
}

.search-form {
  margin-bottom: 15px;

  .el-form {
    display: flex;
    flex-wrap: wrap;

    .el-form-item {
      margin-bottom: 15px;
    }
  }
}

.header-with-icon {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mod-config {
  padding: 15px;
}

.hover-style {
  cursor: pointer;
}

.operate-btn {
  padding: 0;
  margin-left: 0;
}

.rest-password-class {
  color: rgb(245, 108, 108);
}

:deep(.el-table__body .operate-column .cell) {
  display: flex;
  justify-content: space-between;
}

:deep(.el-form-item .el-form-item__label) {
  padding: 0 5px 0 0;
}
</style>

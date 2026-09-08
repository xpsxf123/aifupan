<template>
  <div>
    <el-table
        :data="propertyList"
        :element-loading-spinner="customSvg"
        :header-cell-style="headerCellStyle"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%;padding: 0;"
    >
      <el-table-column v-if="index" :key="'index'" align="center" label="序" type="index"/>
      <el-table-column :key="'commodityTypeName'" :min-width="100" align="center" header-align="center"
                       label="资产名称" prop="commodityTypeName" show-overflow-tooltip/>
      <el-table-column :key="'totalQuantity'" align="center" header-align="center" label="总数" prop="totalQuantity">
        <template #default="{ row }">
          <span>{{ convertUnit(row.totalQuantity, row.commodityTypeCode, row.commodityTypeUnit) }}</span>
        </template>
      </el-table-column>
      <el-table-column :key="'useQuantity'" align="center" header-align="center" label="使用数" prop="useQuantity">
        <template #default="{ row }">
          <span v-if="row.commodityTypeCode.startsWith('child_')">-</span>
          <span v-else>{{ convertUnit(row.useQuantity, row.commodityTypeCode, row.commodityTypeUnit) }}</span>
        </template>
      </el-table-column>
      <el-table-column :key="'quantity'" align="center" header-align="center" label="剩余数" prop="quantity">
        <template #default="{ row }">
          <span v-if="row.commodityTypeCode.startsWith('child_')">-</span>
          <span v-else>{{
              convertUnit((row.totalQuantity - row.useQuantity), row.commodityTypeCode, row.commodityTypeUnit)
            }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const props = defineProps({
  userId: {
    type: String,
    default: ''
  },
  index: {
    type: Boolean,
    default: true
  },
  headerCellStyle: {
    type: Object,
    default: () => ({})
  }
})

const propertyList = ref([])

const convertUnit = (value, commodityTypeCode, commodityTypeUnit) => {
  if (value === null || value === undefined) return '0'
  return `${value}${commodityTypeUnit || ''}`
}

const getPropertyList = async () => {
  if (props.userId) {
    const res = await api.userproperty.getPropertyByUserId({userId: props.userId})
    if (res?.code == 0) {
      propertyList.value = res.data
    }
  }
}

watch(
    () => props.userId,
    () => {
      getPropertyList()
    }
)

onMounted(() => {
  getPropertyList()
})
</script>

<style lang="less" scoped>

</style>
<template>
  <div class="component-doc">
    <h2>数据排行列表 (DataRankingList)</h2>

    <div class="doc-section">
      <h3>组件说明</h3>
      <p>基于 Curd 二次封装，固定展示排行、场观、销售额等字段，支持小组列自定义。</p>
    </div>

    <div class="doc-section">
      <h3>基础用法</h3>
      <div class="demo-box">
        <DataRankingList :api="mockApi" @detail="handleDetail" @export="handleExport" />
      </div>
    </div>

    <div class="doc-section">
      <h3>自定义小组列名与内容</h3>
      <p>通过 group-title 修改列名，通过 slot 修改内容。</p>
      <div class="demo-box">
        <DataRankingList :api="mockApi" group-title="直播间信息" group-slot-name="liveInfo">
          <template #liveInfo="{ row }">
            <div style="display: flex; align-items: center">
              <div class="avtar-container">
                <el-avatar :size="40" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              </div>
              <div style="margin-left: 10px">
                <div style="font-weight: bold">{{ row.name }}</div>
                <div style="font-size: 12px; color: #999">ID: {{ row.id }}</div>
              </div>
            </div>
          </template>
        </DataRankingList>
      </div>
    </div>

    <div class="doc-section">
      <h3>配置文档</h3>
      <table class="doc-table">
        <thead
          ><tr><th>参数名</th><th>说明</th><th>类型</th><th>默认值</th></tr></thead
        >
        <tbody>
          <tr><td>api</td><td>列表 API 对象 (包含 list 方法)</td><td>Object</td><td>Required</td></tr>
          <tr><td>groupTitle</td><td>小组列标题</td><td>String</td><td>'小组'</td></tr>
          <tr><td>groupSlotName</td><td>小组列插槽名</td><td>String</td><td>'group'</td></tr>
        </tbody>
      </table>
      <h4>Events</h4>
      <table class="doc-table">
        <thead
          ><tr><th>事件名</th><th>说明</th><th>回调参数</th></tr></thead
        >
        <tbody>
          <tr><td>detail</td><td>点击查看详情</td><td>row</td></tr>
          <tr><td>export</td><td>点击导出</td><td>-</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import { ElMessage } from 'element-plus'
  import DataRankingList from '@/components/DataRankingList/index.vue'

  // Mock API
  const mockApi = {
    list: (params) => {
      console.log('Query Params:', params)
      return new Promise((resolve) => {
        setTimeout(() => {
          const list = Array.from({ length: 10 }, (_, i) => ({
            id: i + 1,
            name: i < 3 ? `优品严选组 ${i + 1}` : `内容创作组 ${i + 1}`,
            subName: '金桔公司-星选好物事业部',
            description: '创造想科技公司',
            view: Math.floor(Math.random() * 500),
            sales: `${Math.floor(Math.random() * 1000)}-${Math.floor(Math.random() * 2000)}w`,
            refund: '1,562',
            netSales: '755w',
            ads: '6316',
            roi: (Math.random() * 10).toFixed(2)
          }))

          resolve({
            code: 200,
            data: {
              list,
              total: 50
            }
          })
        }, 500)
      })
    }
  }

  const handleDetail = (row) => {
    ElMessage.info(`查看详情: ${row.name}`)
  }

  const handleExport = () => {
    ElMessage.success('触发导出')
  }
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .component-doc {
    padding: 20px;
  }
  .doc-section {
    margin-bottom: 30px;
  }
  .demo-box {
    padding: 20px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    background: #f5f7fa;
  }
  .doc-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
  }
  .doc-table th,
  .doc-table td {
    border: 1px solid #dcdfe6;
    padding: 10px;
    text-align: left;
  }
  .doc-table th {
    background-color: #f5f7fa;
  }
</style>

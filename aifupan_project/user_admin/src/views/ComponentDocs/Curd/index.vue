<template>
  <div class="component-doc">
    <h2>通用增删改查组件 (Curd)</h2>

    <div class="doc-section">
      <h3>组件说明</h3>
      <p>封装了 Table + Pagination + Search + Tools 的通用业务组件。</p>
      <p>支持“列表配置”按页面路径自动持久化：在同一路由下刷新/下次进入仍保留上次勾选的列显示状态。</p>
    </div>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box">
        <Curd
          :api="api"
          :table-columns="[
            { label: 'ID', prop: 'id' },
            { label: 'Name', prop: 'name' }
          ]"
          :search-config="[{ type: 'input', prop: 'name', placeholder: 'Search Name' }]"
        />
      </div>
      <div class="doc-tip">
        <div>操作说明：</div>
        <div>1. 点击右上角“列表配置”取消勾选某一列；</div>
        <div>2. 刷新页面或重新进入该页面；</div>
        <div>3. 列显示状态会自动从本地恢复。</div>
      </div>
    </div>

    <div class="doc-section">
      <h3>配置文档</h3>
      <table class="doc-table">
        <thead
          ><tr><th>参数名</th><th>说明</th><th>类型</th><th>默认值</th></tr></thead
        >
        <tbody>
          <tr><td>api</td><td>包含 list(params) 方法的API对象</td><td>Object</td><td>Required</td></tr>
          <tr><td>tableColumns</td><td>表格列配置数组</td><td>Array</td><td>[]</td></tr>
          <tr><td>searchConfig</td><td>搜索栏配置数组</td><td>Array</td><td>[]</td></tr>
          <tr><td>showAdd</td><td>显示新增按钮</td><td>Boolean</td><td>true</td></tr>
          <tr><td>showOperation</td><td>显示操作列</td><td>Boolean</td><td>true</td></tr>
          <tr><td>pagination</td><td>启用分页</td><td>Boolean</td><td>true</td></tr>
          <tr>
            <td>tableCacheByRoute</td>
            <td>是否按当前路由 path 自动持久化列表配置（列显示状态）</td>
            <td>Boolean</td>
            <td>true</td>
          </tr>
          <tr>
            <td>tableCacheKey</td>
            <td>自定义列表配置缓存索引（不传则默认使用当前路由 path）</td>
            <td>String</td>
            <td>''</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description Curd 组件文档与示例页
   */
  import Curd from '@/components/Curd/index.vue'

  const api = {
    list: () =>
      Promise.resolve({
        code: 200,
        data: {
          list: [{ id: 1, name: 'Test User' }],
          total: 1
        }
      })
  }
</script>

<style scoped>
  .component-doc {
    padding: 20px;
  }
  .doc-section {
    margin-bottom: 30px;
  }
  .demo-box {
    padding: 20px;
    border: 1px solid #eee;
  }
  .doc-tip {
    margin-top: 10px;
    color: #606266;
    font-size: 13px;
    line-height: 20px;
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

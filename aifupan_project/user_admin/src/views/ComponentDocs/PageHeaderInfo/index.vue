<template>
  <div class="component-doc">
    <h2>PageHeaderInfo 页头信息</h2>
    <p>标准页头组件，包含标题、描述、图标/头像以及右侧统计信息。</p>

    <div class="demo-block">
      <h3>基础用法</h3>
      <PageHeaderInfo
        title="某某部门"
        description="负责全公司销售业务"
        icon="DataAnalysis"
        :stats="[
          { label: '总销售额', value: '¥1,000,000' },
          { label: '完成率', value: '98%' }
        ]"
      />
    </div>

    <div class="demo-block">
      <h3>使用头像与Slot</h3>
      <PageHeaderInfo
        title="张三"
        description="高级销售经理"
        avatar="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
      >
        <template #stats>
          <el-button type="primary">编辑资料</el-button>
        </template>
      </PageHeaderInfo>
    </div>

    <div class="doc-section">
      <h3>Props</h3>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="150" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
  import PageHeaderInfo from '@/components/PageHeaderInfo/index.vue'

  const propsData = [
    { name: 'title', desc: '标题', type: 'String' },
    { name: 'description', desc: '描述文本', type: 'String' },
    { name: 'icon', desc: '图标名称 (Element Plus Icon)', type: 'String' },
    { name: 'avatar', desc: '头像图片URL', type: 'String' },
    { name: 'stats', desc: '统计数据 [{label, value, unit}]', type: 'Array' }
  ]
</script>

<style scoped>
  .component-doc {
    padding: 20px;
    background: #fff;
  }
  .demo-block {
    padding: 20px;
    border: 1px solid #ebeef5;
    margin: 20px 0;
  }
</style>

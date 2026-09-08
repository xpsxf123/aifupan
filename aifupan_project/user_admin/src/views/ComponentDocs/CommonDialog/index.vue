<template>
  <div class="component-doc">
    <h2>CommonDialog 通用弹窗</h2>
    <p>基于 el-dialog 封装的统一风格弹窗。</p>

    <div class="demo-block">
      <el-button @click="visible = true">打开弹窗</el-button>
      <CommonDialog v-model="visible" title="测试弹窗" width="500px">
        <p>这是弹窗内容区域。</p>
        <template #footer>
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="visible = false">确定</el-button>
        </template>
      </CommonDialog>
    </div>

    <div class="doc-section">
      <h3>Props</h3>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="default" label="默认值" width="150" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import CommonDialog from '@/components/CommonDialog/index.vue'

  const visible = ref(false)

  const propsData = [
    { name: 'modelValue', desc: '是否显示', type: 'Boolean', default: 'false' },
    { name: 'title', desc: '标题', type: 'String', default: "''" },
    { name: 'width', desc: '宽度', type: 'String | Number', default: "'440px'" },
    { name: 'minHeight', desc: '最小高度', type: 'String', default: "'420px'" }
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

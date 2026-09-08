<template>
  <div class="component-doc">
    <div class="doc-header">
      <h2>PermissionSelect 权限选择组件</h2>
      <p>基于 Element Plus 的 el-tree 封装的权限选择组件，支持树形数据展示、复选框选择以及自定义高度。</p>
    </div>

    <div class="doc-section">
      <h3>基础用法</h3>
      <p>默认高度为 600px，超出部分滚动显示。</p>
      <div class="demo-block">
        <PermissionSelect v-model="selectedKeys" :options="permissionTreeData" />
        <div class="demo-value"> Selected IDs: {{ selectedKeys }} </div>
      </div>
    </div>

    <div class="doc-section">
      <h3>自定义高度</h3>
      <p>通过 height 属性自定义组件高度，例如 300px。</p>
      <div class="demo-block">
        <PermissionSelect v-model="selectedKeys2" :options="permissionTreeData" height="300px" />
        <div class="demo-value"> Selected IDs: {{ selectedKeys2 }} </div>
      </div>
    </div>

    <div class="doc-section">
      <h3>Props</h3>
      <el-table :data="propsData" border style="width: 100%">
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="default" label="默认值" width="150" />
      </el-table>
    </div>

    <div class="doc-section">
      <h3>Events</h3>
      <el-table :data="eventsData" border style="width: 100%">
        <el-table-column prop="name" label="事件名称" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="params" label="回调参数" width="250" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import PermissionSelect from '@/views/BasicSettings/Role/components/PermissionSelect.vue'

  // Mock Data
  const permissionTreeData = [
    {
      id: 1,
      label: '首页',
      children: []
    },
    {
      id: 2,
      label: '我的排班和业绩',
      children: [
        { id: 21, label: '个人排班' },
        { id: 22, label: '个人业绩' },
        { id: 23, label: '个人资料' }
      ]
    },
    {
      id: 3,
      label: '业绩汇总',
      children: [
        { id: 31, label: '集团业绩罗盘' },
        { id: 32, label: '各分公司业绩' },
        { id: 33, label: '各部门业绩' },
        { id: 34, label: '各小组业绩' },
        { id: 35, label: '各直播间业绩' }
      ]
    },
    {
      id: 4,
      label: '基础设置',
      children: [
        { id: 41, label: '岗位' },
        { id: 42, label: '角色管理' }
      ]
    },
    // Adding more data to test scroll
    {
      id: 5,
      label: '测试模块 A',
      children: Array.from({ length: 5 }, (_, i) => ({ id: 50 + i, label: `测试子菜单 A-${i + 1}` }))
    },
    {
      id: 6,
      label: '测试模块 B',
      children: Array.from({ length: 5 }, (_, i) => ({ id: 60 + i, label: `测试子菜单 B-${i + 1}` }))
    },
    {
      id: 7,
      label: '测试模块 C',
      children: Array.from({ length: 5 }, (_, i) => ({ id: 70 + i, label: `测试子菜单 C-${i + 1}` }))
    }
  ]

  const selectedKeys = ref([1, 2, 21])
  const selectedKeys2 = ref([3, 31])

  const propsData = [
    { name: 'modelValue', desc: '选中项的 ID 数组 (v-model)', type: 'Array', default: '[]' },
    { name: 'options', desc: '树形数据源', type: 'Array', default: '[]' },
    { name: 'height', desc: '组件最大高度，支持数字(px)或字符串', type: 'String | Number', default: "'600px'" }
  ]

  const eventsData = [{ name: 'update:modelValue', desc: '选中项发生变化时触发', params: 'checkedKeys: Array' }]
</script>

<style scoped>
  .component-doc {
    padding: 20px;
    background: #fff;
  }
  .doc-header {
    margin-bottom: 30px;
  }
  .doc-section {
    margin-bottom: 40px;
  }
  .demo-block {
    border: 1px solid #ebeef5;
    padding: 20px;
    border-radius: 4px;
    margin: 10px 0;
  }
  .demo-value {
    margin-top: 10px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
    font-family: monospace;
  }
</style>

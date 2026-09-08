<template>
  <div class="component-doc">
    <h2>Breadcrumb 面包屑导航</h2>
    <p>基于 Vue Router 路由配置自动生成的面包屑导航。</p>

    <div class="demo-block">
      <Breadcrumb />
    </div>

    <div class="doc-section">
      <h3>使用说明</h3>
      <p>组件会自动读取当前路由的 matched 数组。</p>
      <ul>
        <li>路由 meta.title 用于显示名称。</li>
        <li>路由 meta.breadcrumb: false 可隐藏该级面包屑。</li>
        <li>路由 redirect: 'noRedirect' 可使该级面包屑不可点击。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
  import Breadcrumb from '@/components/Breadcrumb/index.vue'
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

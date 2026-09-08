<template>
  <div class="component-doc">
    <h2>商品信息组件 (ProductInfo)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box" style="width: 400px">
        <ProductInfo
          image="https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg"
          name="测试商品名称非常长测试商品名称非常长测试商品名称非常长"
          price="99.9"
          shop-name="测试店铺名称也比较长测试店铺名称也比较长"
          shop-rating="4.8"
        />
      </div>
    </div>

    <div class="doc-section">
      <h3>配置文档</h3>
      <table class="doc-table">
        <thead
          ><tr><th>参数名</th><th>说明</th><th>类型</th><th>默认值</th></tr></thead
        >
        <tbody>
          <tr><td>image</td><td>商品图片URL</td><td>String</td><td>''</td></tr>
          <tr><td>name</td><td>商品名称</td><td>String</td><td>''</td></tr>
          <tr><td>price</td><td>价格</td><td>String | Number</td><td>0</td></tr>
          <tr><td>shopName</td><td>店铺名称</td><td>String</td><td>''</td></tr>
          <tr><td>shopRating</td><td>店铺评分</td><td>String | Number</td><td>0</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import ProductInfo from '@/components/ProductInfo/index.vue'
</script>

<style scoped>
  .component-doc {
    padding: 20px;
  }
  .doc-section {
    margin-bottom: 30px;
  }
  .demo-box {
    border: 1px solid #eee;
    padding: 10px;
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

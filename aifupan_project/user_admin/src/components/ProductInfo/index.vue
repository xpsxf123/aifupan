<template>
  <div class="product-info">
    <!-- 左侧图片 -->
    <div class="product-image">
      <el-image :src="image" :preview-src-list="[image]" fit="cover" class="image-el">
        <template #error>
          <div class="image-slot">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>
    </div>

    <!-- 右侧信息 -->
    <div class="product-details">
      <!-- 第一行：商品名称 -->
      <el-tooltip :content="name" placement="top" :show-after="500" :disabled="!isNameOverflow">
        <div class="product-name" @mouseenter="checkNameOverflow">
          <span ref="nameRef">{{ name }}</span>
        </div>
      </el-tooltip>

      <!-- 第二行：价格 | 店铺 | 评分 -->
      <div class="product-meta">
        <span class="product-price">¥{{ price }}</span>

        <div class="shop-info-wrapper">
          <el-icon class="shop-icon"><Shop /></el-icon>

          <el-tooltip :content="shopName" placement="top" :show-after="500" :disabled="!isShopNameOverflow">
            <span class="shop-name" ref="shopNameRef" @mouseenter="checkShopNameOverflow">
              {{ shopName }}
            </span>
          </el-tooltip>

          <span class="shop-rating">({{ shopRating }})</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, defineProps } from 'vue'
  import { Picture, Shop } from '@element-plus/icons-vue'

  const props = defineProps({
    image: {
      type: String,
      default: ''
    },
    name: {
      type: String,
      default: ''
    },
    price: {
      type: [String, Number],
      default: 0
    },
    shopName: {
      type: String,
      default: ''
    },
    shopRating: {
      type: [String, Number],
      default: 0
    }
  })

  // 文本溢出检测
  const nameRef = ref(null)
  const shopNameRef = ref(null)
  const isNameOverflow = ref(false)
  const isShopNameOverflow = ref(false)

  const checkNameOverflow = () => {
    if (nameRef.value) {
      const parent = nameRef.value.parentElement
      // 简单的判断：如果 scrollWidth > clientWidth
      // 但由于我们使用了 text-overflow: ellipsis，通常需要判断 scrollWidth > clientWidth
      // 这里我们简单比较 parent 的宽度和 span 的宽度，或者直接检查 scrollWidth
      isNameOverflow.value = parent.scrollWidth > parent.clientWidth
    }
  }

  const checkShopNameOverflow = () => {
    if (shopNameRef.value) {
      // 这里的 shop-name 是 flex item，如果有 ellipsis，scrollWidth 会大于 clientWidth
      isShopNameOverflow.value = shopNameRef.value.scrollWidth > shopNameRef.value.clientWidth
    }
  }
</script>

<style scoped lang="scss">
  .product-info {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
  }

  .product-image {
    flex-shrink: 0;
    width: 60px;
    height: 60px;
    border-radius: 4px;
    overflow: hidden;
    border: 1px solid #ebeef5;

    .image-el {
      width: 100%;
      height: 100%;
    }

    .image-slot {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      height: 100%;
      background: #f5f7fa;
      color: #909399;
    }
  }

  .product-details {
    flex: 1;
    min-width: 0; // 关键：允许 flex 子项缩小到小于内容宽度
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
  }

  .product-name {
    font-size: 14px;
    color: #303133;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    width: 100%;
    cursor: default;
  }

  .product-meta {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #606266;
    gap: 8px;
  }

  .product-price {
    color: #f56c6c;
    font-weight: 600;
    flex-shrink: 0;
  }

  .shop-info-wrapper {
    display: flex;
    align-items: center;
    gap: 4px;
    flex: 1;
    min-width: 0; // 允许子元素压缩

    .shop-icon {
      flex-shrink: 0;
      color: #409eff; // 抖音/Element 蓝色
    }

    .shop-name {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      // flex: 1; // 让它占据剩余空间
      // 不需要 flex: 1，默认 flex-shrink: 1 即可
    }

    .shop-rating {
      flex-shrink: 0; // 评分不压缩，永远显示
      color: #909399;
    }
  }
</style>

<template>
  <div :class="{ hidden: hidden }" class="curd-pagination-container">
    <!-- Left Slot -->
    <div v-if="$slots.left" class="pagination-left">
      <slot name="left" />
    </div>

    <!-- Pagination (Center if no slots or both slots, Right if only left slot) -->
    <!-- 
      Layout Logic:
      1. Default (No slots): Center
      2. Only Left or Only Right: Space Between (Content | Pagination) or (Pagination | Content) -> But user asked:
         "If only one content (front/back), content and pagination left/right"
         "If both, content left/right, pagination center"
         "If none, pagination center"
    -->
    <div class="pagination-wrapper" :class="paginationClass">
      <el-pagination
        :background="background"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :layout="layout"
        :page-sizes="pageSizes"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- Right Slot -->
    <div v-if="$slots.right" class="pagination-right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup>
  /**
   * @file Pagination Component for Curd Module
   * @description Provides a configurable pagination component with flexible layout support.
   */
  import { computed, useSlots } from 'vue'

  const props = defineProps({
    total: {
      required: true,
      type: Number
    },
    page: {
      type: Number,
      default: 1
    },
    limit: {
      type: Number,
      default: 10
    },
    pageSizes: {
      type: Array,
      default() {
        return [10, 20, 30, 50]
      }
    },
    layout: {
      type: String,
      default: 'total, sizes, prev, pager, next, jumper'
    },
    background: {
      type: Boolean,
      default: true
    },
    hidden: {
      type: Boolean,
      default: false
    }
  })

  const emit = defineEmits(['update:page', 'update:limit', 'pagination'])
  const slots = useSlots()

  const currentPage = computed({
    get() {
      return props.page
    },
    set(val) {
      emit('update:page', val)
    }
  })

  const pageSize = computed({
    get() {
      return props.limit
    },
    set(val) {
      emit('update:limit', val)
    }
  })

  // Determine pagination alignment class based on slots
  const paginationClass = computed(() => {
    const hasLeft = !!slots.left
    const hasRight = !!slots.right

    if (hasLeft && hasRight) {
      // Case 2: Both slots present -> Center pagination
      return 'is-center'
    } else if (hasLeft || hasRight) {
      // Case 1: Only one slot -> Pagination takes remaining space (flex-end or flex-start depending on slot)
      // Actually, container is flex space-between.
      // If left exists: Left | Pagination (Right)
      // If right exists: Pagination (Left) | Right
      // So pagination itself doesn't need special class if container is space-between,
      // BUT we need to ensure pagination is not centered in this case.
      return ''
    } else {
      // Case 3: No slots -> Center pagination
      return 'is-center'
    }
  })

  const handleSizeChange = (val) => {
    emit('pagination', { page: currentPage.value, limit: val })
  }

  const handleCurrentChange = (val) => {
    emit('pagination', { page: val, limit: pageSize.value })
  }
</script>

<style scoped lang="scss">
  .curd-pagination-container {
    background: #fff;
    display: flex;
    align-items: center;
    /* Default: space-between handles the "Only one slot" case naturally:
     - Left + Pagination: Left is left, Pagination is right
     - Pagination + Right: Pagination is left, Right is right
  */
    justify-content: space-between;
  }

  .curd-pagination-container.hidden {
    display: none;
  }

  .pagination-wrapper {
    display: flex;
    /* Default allow flex grow/shrink */
  }

  /* 
   Case 3 & Case 2: Center Pagination 
   If is-center, we want the pagination to be absolutely centered 
   OR we use flex tricks.
   
   If container is space-between:
   - No slots: Pagination is the only child. space-between doesn't center single child unless we use margin: auto or justify-content: center.
   - Both slots: Left | Pagination | Right. space-between puts pagination in middle if widths are equal, but they aren't.
   
   Better approach for centering:
   Use flex: 1 on the wrapper and justify-content: center
*/

  .pagination-wrapper.is-center {
    flex: 1;
    justify-content: center;
  }
</style>

<template>
  <div class="staff-list">
    <template v-if="staffList.length">
      <div v-for="item in staffList" :key="item.role" class="staff-item">
        <span class="dot" :style="{ 'background-color': getRoleColor(item.positionName) }"></span>
        <span class="role">{{ item.positionName }}:</span>
        <span class="name">{{ item.employeeName }}</span>
      </div>
    </template>
    <template v-else>
      <div class="staff-item"> </div>
    </template>
  </div>
</template>

<script setup>
  import useGetRoleColor from '@/hooks/roleColor.js'
  const props = defineProps({
    staffList: {
      type: Array,
      default: () => []
    }
  })
  const { getRoleColor } = useGetRoleColor()
</script>

<style scoped>
  .staff-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 13px;
  }

  .staff-item {
    display: flex;
    align-items: center;
  }

  .staff-item .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    margin-right: 8px;
  }

  .staff-item .role {
    margin-right: 4px;
  }

  .staff-item .name {
    margin-right: 8px;
    min-width: 40px;
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>

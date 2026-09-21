<script setup lang="ts">
import { Columns3 } from '@lucide/vue'
defineProps<{ modelValue: boolean; label: string }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>
<template>
  <button
    class="table-detail-toggle"
    type="button"
    :aria-pressed="modelValue"
    @click="$emit('update:modelValue', !modelValue)"
  >
    <Columns3 :size="16" aria-hidden="true" />{{ modelValue ? '收起' : '显示' }}{{ label }}
  </button>
</template>
<style scoped>
.table-detail-toggle {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 7px 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary-strong);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
  transition: background-color var(--motion-fast);
}
.table-detail-toggle[aria-pressed='true'] {
  background: var(--color-surface);
}
@media (prefers-reduced-motion: reduce) {
  .table-detail-toggle {
    transition: none;
  }
}
</style>

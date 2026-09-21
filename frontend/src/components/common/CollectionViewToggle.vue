<script setup lang="ts">
import { LayoutGrid, List } from '@lucide/vue'
defineProps<{ modelValue: 'table' | 'cards' }>()
defineEmits<{ 'update:modelValue': [value: 'table' | 'cards'] }>()
</script>
<template>
  <fieldset class="collection-toggle" aria-label="结果显示方式">
    <button
      type="button"
      :aria-pressed="modelValue === 'table'"
      @click="$emit('update:modelValue', 'table')"
    >
      <List :size="16" aria-hidden="true" />表格
    </button>
    <button
      type="button"
      :aria-pressed="modelValue === 'cards'"
      @click="$emit('update:modelValue', 'cards')"
    >
      <LayoutGrid :size="16" aria-hidden="true" />摘要
    </button>
  </fieldset>
</template>
<style scoped>
.collection-toggle {
  min-width: 0;
  margin: 0;
  display: inline-flex;
  gap: 3px;
  padding: 3px;
  background: var(--color-surface-secondary);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
}
button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 36px;
  padding: 6px 12px;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  background: transparent;
  font-size: 13px;
  transition:
    background-color var(--motion-fast),
    color var(--motion-fast);
}
button[aria-pressed='true'] {
  background: var(--color-surface);
  color: var(--color-primary-strong);
  box-shadow: var(--shadow-sm);
}
@media (max-width: 700px) {
  .collection-toggle {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  button {
    transition: none;
  }
}
</style>

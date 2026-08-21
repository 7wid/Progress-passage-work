<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    icon: Component
    tone?: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'slate'
  }>(),
  { description: '', tone: 'blue' },
)
</script>

<template>
  <div class="app-section-header" :class="`app-section-header--${tone}`">
    <span class="app-section-header__icon" aria-hidden="true">
      <component :is="icon" :size="18" :stroke-width="1.9" />
    </span>
    <div class="app-section-header__copy">
      <strong>{{ title }}</strong>
      <small v-if="description">{{ description }}</small>
    </div>
    <div v-if="$slots.actions" class="app-section-header__actions"><slot name="actions" /></div>
  </div>
</template>

<style scoped>
.app-section-header {
  --section-color: var(--color-primary);
  --section-soft: var(--color-primary-soft);
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.app-section-header--green {
  --section-color: var(--color-success);
  --section-soft: #ecfdf5;
}

.app-section-header--orange {
  --section-color: var(--color-warning);
  --section-soft: #fff7ed;
}

.app-section-header--red {
  --section-color: var(--color-danger);
  --section-soft: #fef2f2;
}

.app-section-header--purple {
  --section-color: var(--color-purple);
  --section-soft: #f5f3ff;
}

.app-section-header--slate {
  --section-color: #475569;
  --section-soft: #f1f5f9;
}

.app-section-header__icon {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  color: var(--section-color);
  background: var(--section-soft);
  border-radius: var(--radius-md);
}

.app-section-header__copy {
  display: grid;
  min-width: 0;
  gap: 1px;
}

.app-section-header strong {
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 650;
}

.app-section-header small {
  color: var(--color-text-tertiary);
  font-size: 12px;
  font-weight: 500;
}

.app-section-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>

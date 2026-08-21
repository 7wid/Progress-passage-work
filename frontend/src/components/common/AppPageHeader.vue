<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    eyebrow?: string
    icon: Component
    tone?: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'slate'
  }>(),
  {
    description: '',
    eyebrow: '',
    tone: 'blue',
  },
)
</script>

<template>
  <header class="app-page-header" :class="`app-page-header--${tone}`">
    <div class="app-page-header__identity">
      <span class="app-page-header__icon" aria-hidden="true">
        <component :is="icon" :size="22" :stroke-width="1.9" />
      </span>
      <div class="app-page-header__copy">
        <span v-if="eyebrow" class="app-page-header__eyebrow">{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p v-if="description">{{ description }}</p>
        <div v-if="$slots.meta" class="app-page-header__meta">
          <slot name="meta" />
        </div>
      </div>
    </div>

    <div v-if="$slots.actions" class="app-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.app-page-header {
  --header-color: var(--color-primary);
  --header-soft: var(--color-primary-soft);
  --header-border: var(--color-primary-border);
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 2px;
}

.app-page-header--green {
  --header-color: var(--color-success);
  --header-soft: #ecfdf5;
  --header-border: #a7f3d0;
}

.app-page-header--orange {
  --header-color: var(--color-warning);
  --header-soft: #fff7ed;
  --header-border: #fed7aa;
}

.app-page-header--red {
  --header-color: var(--color-danger);
  --header-soft: #fef2f2;
  --header-border: #fecaca;
}

.app-page-header--purple {
  --header-color: var(--color-purple);
  --header-soft: #f5f3ff;
  --header-border: #ddd6fe;
}

.app-page-header--slate {
  --header-color: #475569;
  --header-soft: #f1f5f9;
  --header-border: #cbd5e1;
}

.app-page-header__identity {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 14px;
}

.app-page-header__icon {
  position: relative;
  display: inline-grid;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  place-items: center;
  color: var(--header-color);
  background: var(--header-soft);
  border: 1px solid var(--header-border);
  border-radius: var(--radius-md);
  box-shadow: 0 5px 14px rgb(15 23 42 / 5%);
}

.app-page-header__icon::after {
  position: absolute;
  right: 5px;
  bottom: 5px;
  width: 4px;
  height: 4px;
  background: var(--header-color);
  border-radius: 1px;
  content: '';
}

.app-page-header__copy {
  min-width: 0;
}

.app-page-header__eyebrow {
  display: block;
  margin-bottom: 2px;
  color: var(--header-color);
  font-size: 11px;
  font-weight: 700;
  line-height: 1.4;
}

.app-page-header h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 28px;
  font-weight: 680;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.app-page-header p {
  max-width: 720px;
  margin: 5px 0 0;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.55;
}

.app-page-header__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.app-page-header__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.app-page-header__actions :deep(.el-button) {
  margin: 0;
}

.app-page-header__actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

@media (max-width: 760px) {
  .app-page-header {
    align-items: stretch;
    flex-direction: column;
    gap: 14px;
  }

  .app-page-header h1 {
    font-size: 25px;
  }

  .app-page-header__icon {
    width: 42px;
    height: 42px;
    flex-basis: 42px;
  }

  .app-page-header__actions {
    justify-content: flex-start;
  }
}
</style>

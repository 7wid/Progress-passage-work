<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { Component } from 'vue'
import { ArrowUpRight, Plus, Search, X } from '@lucide/vue'
import { useRouter } from 'vue-router'

export interface CommandNavigationItem {
  label: string
  group: string
  to: string
  icon: Component
}

interface CommandItem extends CommandNavigationItem {
  id: string
  type: 'navigation' | 'create' | 'search'
}

const props = defineProps<{
  modelValue: boolean
  navigationItems: CommandNavigationItem[]
  canCreateRequest: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const router = useRouter()
const query = ref('')
const activeIndex = ref(0)
const searchInput = ref<HTMLInputElement | null>(null)

const normalizedQuery = computed(() => query.value.trim().toLocaleLowerCase('zh-CN'))

const commands = computed<CommandItem[]>(() => {
  const keyword = normalizedQuery.value
  const navigation = props.navigationItems
    .filter(
      (item) =>
        !keyword || `${item.label} ${item.group}`.toLocaleLowerCase('zh-CN').includes(keyword),
    )
    .map((item) => ({ ...item, id: `nav-${item.to}`, type: 'navigation' as const }))

  const quickActions: CommandItem[] = []
  if (props.canCreateRequest && (!keyword || '发起需求 新建需求 创建申请'.includes(keyword))) {
    quickActions.push({
      id: 'create-request',
      label: '发起新需求',
      group: '快捷操作',
      to: '/requests/new',
      icon: Plus,
      type: 'create',
    })
  }

  if (keyword) {
    quickActions.push({
      id: 'search-requests',
      label: `搜索“${query.value.trim()}”`,
      group: '需求检索',
      to: '/requests',
      icon: Search,
      type: 'search',
    })
  }

  return [...quickActions, ...navigation]
})
const activeCommandId = computed(() => {
  const command = commands.value[activeIndex.value]
  return command ? `command-${command.id}` : undefined
})

watch(commands, () => {
  activeIndex.value = 0
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      query.value = ''
      activeIndex.value = 0
    }
  },
)

function close() {
  emit('update:modelValue', false)
}

async function focusSearch() {
  await nextTick()
  searchInput.value?.focus()
}

function moveActive(offset: number) {
  if (!commands.value.length) return
  activeIndex.value = (activeIndex.value + offset + commands.value.length) % commands.value.length
}

async function runCommand(command: CommandItem | undefined) {
  if (!command) return
  close()
  if (command.type === 'search') {
    await router.push({ name: 'request-list', query: { keyword: query.value.trim() } })
    return
  }
  await router.push(command.to)
}

function handleSearchKeydown(event: KeyboardEvent) {
  if (event.isComposing) return
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveActive(1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveActive(-1)
  } else if (event.key === 'Enter') {
    event.preventDefault()
    void runCommand(commands.value[activeIndex.value])
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    class="command-dialog"
    width="min(620px, calc(100vw - 32px))"
    :show-close="false"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @opened="focusSearch"
  >
    <template #header>
      <div class="command-dialog__header">
        <div>
          <span class="command-dialog__mark" aria-hidden="true"><Search :size="17" /></span>
          <strong>快捷访问</strong>
        </div>
        <button
          type="button"
          class="command-dialog__close"
          aria-label="关闭"
          title="关闭"
          @click="close"
        >
          <X :size="18" aria-hidden="true" />
        </button>
      </div>
    </template>

    <div class="command-search">
      <Search :size="19" aria-hidden="true" />
      <label class="sr-only" for="command-search-input">搜索页面或需求</label>
      <input
        id="command-search-input"
        ref="searchInput"
        v-model="query"
        type="search"
        role="combobox"
        aria-autocomplete="list"
        aria-controls="command-results"
        :aria-activedescendant="activeCommandId"
        :aria-expanded="modelValue"
        autocomplete="off"
        placeholder="搜索页面、操作或需求关键词"
        @keydown="handleSearchKeydown"
      />
    </div>

    <div id="command-results" class="command-results" role="listbox" aria-label="可用操作">
      <button
        v-for="(command, index) in commands"
        :id="`command-${command.id}`"
        :key="command.id"
        type="button"
        role="option"
        class="command-item"
        :class="{ 'command-item--active': index === activeIndex }"
        :aria-selected="index === activeIndex"
        @mouseenter="activeIndex = index"
        @focus="activeIndex = index"
        @click="runCommand(command)"
      >
        <span class="command-item__icon" aria-hidden="true">
          <component :is="command.icon" :size="18" :stroke-width="1.8" />
        </span>
        <span class="command-item__copy">
          <strong>{{ command.label }}</strong>
          <small>{{ command.group }}</small>
        </span>
        <ArrowUpRight :size="17" class="command-item__arrow" aria-hidden="true" />
      </button>

      <div v-if="!commands.length" class="command-empty">
        <Search :size="20" aria-hidden="true" />
        <span>没有匹配的页面</span>
      </div>
    </div>
  </el-dialog>
</template>

<style>
.command-dialog {
  margin-top: min(14vh, 120px) !important;
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.command-dialog .el-dialog__header {
  padding: 15px 16px;
  margin: 0;
  border-bottom: 1px solid var(--color-border-subtle);
}

.command-dialog .el-dialog__body {
  padding: 0;
}

.command-dialog__header,
.command-dialog__header > div {
  display: flex;
  align-items: center;
}

.command-dialog__header {
  justify-content: space-between;
  gap: 16px;
}

.command-dialog__header > div {
  gap: 9px;
}

.command-dialog__header strong {
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.command-dialog__mark {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: var(--color-primary-strong);
  background: var(--color-primary-soft);
  border-radius: var(--radius-sm);
}

.command-dialog__close {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  padding: 0;
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--radius-md);
}

.command-dialog__close:hover {
  color: var(--color-text-primary);
  background: var(--color-surface-hover);
}

.command-search {
  display: flex;
  height: 58px;
  align-items: center;
  gap: 11px;
  padding: 0 20px;
  color: var(--color-text-tertiary);
  border-bottom: 1px solid var(--color-border-subtle);
}

.command-search:focus-within {
  color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-focus-ring) inset;
}

.command-search input {
  min-width: 0;
  height: 100%;
  flex: 1;
  color: var(--color-text-primary);
  background: transparent;
  border: 0;
  outline: 0;
  font: inherit;
  font-size: 15px;
}

.command-search input::placeholder {
  color: var(--color-text-tertiary);
}

.command-results {
  display: grid;
  max-height: min(440px, 55vh);
  gap: 4px;
  padding: 10px;
  overflow-y: auto;
}

.command-item {
  display: grid;
  min-height: 58px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 9px 11px;
  color: var(--color-text-primary);
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  transition:
    color var(--motion-fast) ease,
    background-color var(--motion-fast) ease,
    border-color var(--motion-fast) ease;
}

.command-item--active {
  background: var(--color-primary-soft);
  border-color: var(--color-primary-border);
}

.command-item__icon {
  display: inline-grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: var(--color-text-secondary);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-sm);
}

.command-item--active .command-item__icon,
.command-item--active .command-item__arrow {
  color: var(--color-primary-strong);
}

.command-item__copy {
  display: grid;
  min-width: 0;
  gap: 1px;
}

.command-item__copy strong,
.command-item__copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.command-item__copy strong {
  font-size: 14px;
  font-weight: 600;
}

.command-item__copy small {
  color: var(--color-text-tertiary);
  font-size: 11px;
}

.command-item__arrow {
  color: var(--color-text-tertiary);
}

.command-empty {
  display: grid;
  min-height: 150px;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

@media (max-width: 600px) {
  .command-dialog {
    margin-top: 72px !important;
  }
}
</style>

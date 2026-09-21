<script setup lang="ts">
import { businessDateRange, type BusinessRangePreset } from '@/utils/businessDateRange'
const props = defineProps<{ modelValue: [string, string] }>()
const emit = defineEmits<{ 'update:modelValue': [value: [string, string]]; change: [] }>()
const options: Array<{ label: string; value: BusinessRangePreset }> = [
  { label: '本月', value: 'month' },
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
]
function selected(preset: BusinessRangePreset) {
  return props.modelValue?.join() === businessDateRange(preset).join()
}
function choose(preset: BusinessRangePreset) {
  emit('update:modelValue', businessDateRange(preset))
  emit('change')
}
</script>
<template>
  <div class="range-presets" role="group" aria-label="快捷日期范围">
    <span>时间范围</span
    ><button
      v-for="option in options"
      :key="option.value"
      type="button"
      :aria-pressed="selected(option.value)"
      @click="choose(option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DailyRequestCount } from '@/types/statistics'

const props = defineProps<{ data: DailyRequestCount[] }>()
const width = 760
const height = 260
const left = 42
const right = 18
const top = 18
const bottom = 38
const plotWidth = width - left - right
const plotHeight = height - top - bottom

const hasData = computed(() => props.data.some((item) => item.count > 0))
const maximum = computed(() => Math.max(1, ...props.data.map((item) => item.count)))

function xAt(index: number): number {
  if (props.data.length <= 1) return left + plotWidth / 2
  return left + (index / (props.data.length - 1)) * plotWidth
}

function yAt(count: number): number {
  return top + plotHeight - (count / maximum.value) * plotHeight
}

const points = computed(() =>
  props.data.map((item, index) => `${xAt(index)},${yAt(item.count)}`).join(' '),
)
const labelIndexes = computed(() => {
  const last = props.data.length - 1
  return last < 0 ? [] : [...new Set([0, Math.round(last / 2), last])]
})

function shortDate(value: string): string {
  const parts = value.split('-')
  return parts.length === 3 ? `${parts[1]}-${parts[2]}` : value
}
</script>

<template>
  <div v-if="!hasData" class="chart-empty">所选范围内暂无新增需求</div>
  <div v-else class="chart-scroll">
    <svg
      class="trend-chart"
      :viewBox="`0 0 ${width} ${height}`"
      role="img"
      aria-label="需求新增趋势折线图"
    >
      <line :x1="left" :y1="top" :x2="left" :y2="top + plotHeight" class="axis" />
      <line
        :x1="left"
        :y1="top + plotHeight"
        :x2="left + plotWidth"
        :y2="top + plotHeight"
        class="axis"
      />
      <line
        :x1="left"
        :y1="top + plotHeight / 2"
        :x2="left + plotWidth"
        :y2="top + plotHeight / 2"
        class="grid-line"
      />
      <text :x="left - 8" :y="top + 5" text-anchor="end" class="axis-label">
        {{ maximum }}
      </text>
      <text :x="left - 8" :y="top + plotHeight + 5" text-anchor="end" class="axis-label">0</text>
      <polyline :points="points" class="trend-line" />
      <circle
        v-for="(item, index) in data"
        :key="item.date"
        :cx="xAt(index)"
        :cy="yAt(item.count)"
        r="3"
        class="trend-point"
      >
        <title>{{ item.date }}：{{ item.count }} 条</title>
      </circle>
      <text
        v-for="index in labelIndexes"
        :key="data[index]?.date ?? index"
        :x="xAt(index)"
        :y="height - 10"
        text-anchor="middle"
        class="axis-label"
      >
        {{ shortDate(data[index]?.date ?? '') }}
      </text>
    </svg>
  </div>
</template>

<style scoped>
.chart-empty {
  display: grid;
  min-height: 260px;
  place-items: center;
  color: #909399;
}
.chart-scroll {
  overflow-x: auto;
}
.trend-chart {
  display: block;
  min-width: 620px;
  width: 100%;
}
.axis {
  stroke: #cbd5e1;
  stroke-width: 1;
}
.grid-line {
  stroke: #e5e7eb;
  stroke-dasharray: 5 5;
}
.axis-label {
  fill: #6b7280;
  font-size: 12px;
}
.trend-line {
  fill: none;
  stroke: #409eff;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}
.trend-point {
  fill: #fff;
  stroke: #409eff;
  stroke-width: 2;
}
</style>

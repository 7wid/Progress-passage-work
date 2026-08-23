<script setup lang="ts">
import { ClipboardCheck } from '@lucide/vue'

withDefaults(
  defineProps<{
    size?: number
    strokeWidth?: number
  }>(),
  {
    size: 22,
    strokeWidth: 2,
  },
)
</script>

<template>
  <ClipboardCheck :size="size" :stroke-width="strokeWidth" aria-hidden="true" />
</template>

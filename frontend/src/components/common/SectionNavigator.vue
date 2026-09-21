<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Check } from '@lucide/vue'

const props = defineProps<{
  label: string
  items: Array<{ id: string; label: string; complete?: boolean }>
}>()
const activeId = ref('')
let frame: number | undefined
function updateActive() {
  frame = undefined
  const available = props.items.filter((item) => document.getElementById(item.id))
  activeId.value =
    available
      .filter((item) => document.getElementById(item.id)!.getBoundingClientRect().top <= 210)
      .slice(-1)[0]?.id ??
    available[0]?.id ??
    ''
}
function scheduleUpdate() {
  if (frame === undefined) frame = requestAnimationFrame(updateActive)
}
function goTo(id: string) {
  const target = document.getElementById(id)
  if (!target) return
  target.focus({ preventScroll: true })
  target.scrollIntoView({
    block: 'start',
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
  })
  activeId.value = id
}
onMounted(() => {
  updateActive()
  window.addEventListener('scroll', scheduleUpdate, { passive: true })
  window.addEventListener('resize', scheduleUpdate)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', scheduleUpdate)
  window.removeEventListener('resize', scheduleUpdate)
  if (frame !== undefined) cancelAnimationFrame(frame)
})
</script>
<template>
  <nav class="section-navigator" :aria-label="label">
    <span class="section-navigator__label">{{ label }}</span>
    <div>
      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        :aria-current="activeId === item.id ? 'location' : undefined"
        @click="goTo(item.id)"
      >
        <Check v-if="item.complete" :size="15" aria-hidden="true" />
        {{ item.label }}<span v-if="item.complete" class="sr-only">，已填写</span>
      </button>
    </div>
    <slot />
  </nav>
</template>
<style scoped>
.section-navigator {
  position: sticky;
  z-index: 20;
  top: 80px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  padding: 10px 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.section-navigator__label {
  color: var(--color-text-tertiary);
  font-size: 12px;
}
.section-navigator > div {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 40px;
  padding: 8px 12px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  transition:
    color var(--motion-fast),
    background-color var(--motion-fast);
}
button:hover,
button[aria-current] {
  background: var(--color-primary-soft);
  color: var(--color-primary-strong);
}
button > svg {
  color: var(--color-success-strong);
}
@media (max-width: 700px) {
  .section-navigator {
    top: 68px;
    gap: 4px;
    padding: 8px;
  }
  .section-navigator__label {
    display: none;
  }
  button {
    padding-inline: 9px;
  }
}
@media (prefers-reduced-motion: reduce) {
  button {
    transition: none;
  }
}
</style>

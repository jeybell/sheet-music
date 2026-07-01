<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Calendar, ChevronLeft, ChevronRight } from '@lucide/vue'

// v-model 값은 'yyyy-mm-dd' 문자열 (네이티브 date input 과 호환)
const model = defineModel<string>()

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

const open = ref(false)
const root = ref<HTMLElement | null>(null)

// 달력이 보여줄 기준 연/월 (1~12)
const viewYear = ref(0)
const viewMonth = ref(0)

const pad = (n: number) => String(n).padStart(2, '0')

const selected = computed(() => {
  const v = model.value
  if (v && /^\d{4}-\d{2}-\d{2}$/.test(v)) {
    const [y, m, d] = v.split('-').map(Number)
    return { y, m, d }
  }
  return null
})

const label = computed(() => {
  const s = selected.value
  return s ? `${s.y}.${pad(s.m)}.${pad(s.d)}` : ''
})

const monthLabel = computed(() => `${viewYear.value}년 ${viewMonth.value}월`)

const todayIso = (() => {
  const n = new Date()
  return `${n.getFullYear()}-${pad(n.getMonth() + 1)}-${pad(n.getDate())}`
})()

// 날짜 그리드: 앞쪽 빈칸(null) + 1일~말일
const days = computed(() => {
  const startDow = new Date(viewYear.value, viewMonth.value - 1, 1).getDay()
  const daysInMonth = new Date(viewYear.value, viewMonth.value, 0).getDate()
  const cells: ({ day: number; iso: string } | null)[] = []
  for (let i = 0; i < startDow; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push({ day: d, iso: `${viewYear.value}-${pad(viewMonth.value)}-${pad(d)}` })
  }
  return cells
})

function initView() {
  if (selected.value) {
    viewYear.value = selected.value.y
    viewMonth.value = selected.value.m
  } else {
    const now = new Date()
    viewYear.value = now.getFullYear()
    viewMonth.value = now.getMonth() + 1
  }
}

function toggle() {
  if (!open.value) initView()
  open.value = !open.value
}

function prevMonth() {
  if (viewMonth.value === 1) {
    viewMonth.value = 12
    viewYear.value--
  } else {
    viewMonth.value--
  }
}

function nextMonth() {
  if (viewMonth.value === 12) {
    viewMonth.value = 1
    viewYear.value++
  } else {
    viewMonth.value++
  }
}

function select(iso: string) {
  model.value = iso
  open.value = false
}

function onClickOutside(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}

onMounted(() => document.addEventListener('mousedown', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('mousedown', onClickOutside))
</script>

<template>
  <div ref="root" class="relative">
    <button
      type="button"
      class="flex h-9 w-full items-center gap-2 rounded-md border border-input bg-card px-3 py-1 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent"
      @click="toggle"
    >
      <Calendar class="w-4 h-4 text-muted-foreground shrink-0" />
      <span v-if="label">{{ label }}</span>
      <span v-else class="text-muted-foreground">날짜 선택</span>
    </button>

    <div
      v-if="open"
      class="absolute left-0 z-50 mt-1 w-64 rounded-md border border-border bg-card p-3 shadow-lg"
    >
      <div class="flex items-center justify-between mb-2">
        <button type="button" class="p-1 rounded hover:bg-muted text-muted-foreground" @click="prevMonth">
          <ChevronLeft class="w-4 h-4" />
        </button>
        <span class="text-sm font-medium text-foreground">{{ monthLabel }}</span>
        <button type="button" class="p-1 rounded hover:bg-muted text-muted-foreground" @click="nextMonth">
          <ChevronRight class="w-4 h-4" />
        </button>
      </div>

      <div class="grid grid-cols-7 gap-0.5 mb-1">
        <span
          v-for="(w, i) in WEEKDAYS"
          :key="w"
          class="text-center text-xs py-1"
          :class="i === 0 ? 'text-destructive' : i === 6 ? 'text-primary' : 'text-muted-foreground'"
        >{{ w }}</span>
      </div>

      <div class="grid grid-cols-7 gap-0.5">
        <template v-for="(cell, i) in days" :key="i">
          <span v-if="!cell" />
          <button
            v-else
            type="button"
            class="h-8 rounded text-sm transition-colors"
            :class="cell.iso === model
              ? 'bg-primary text-primary-foreground font-medium'
              : cell.iso === todayIso
                ? 'bg-muted text-foreground'
                : 'text-foreground hover:bg-muted'"
            @click="select(cell.iso)"
          >{{ cell.day }}</button>
        </template>
      </div>
    </div>
  </div>
</template>

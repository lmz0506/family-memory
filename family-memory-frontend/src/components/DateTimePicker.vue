<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { IconChevronRight, IconCalendar, IconX } from '@/components/icons'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  /** 模式：datetime-日期时间，date-仅日期 */
  mode?: 'datetime' | 'date'
  /** 主题色：coral-珊瑚红，amber-琥珀黄 */
  theme?: 'coral' | 'amber'
  /** 左侧标签文字 */
  label?: string
  /** 最小日期限制（YYYY-MM-DD格式） */
  minDate?: string
}>(), {
  mode: 'datetime',
  theme: 'coral',
  label: '记忆时间'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// 主题色配置
const themeColors = computed(() => {
  if (props.theme === 'amber') {
    return {
      primary: '#FFD54F',
      secondary: '#FFE082',
      gradient: 'from-[#FFD54F] to-[#FFE082]',
      shadow: 'shadow-yellow-200/50',
      bgLight: '#FFFDE7',
      bgHover: '#FFF9C4',
      ring: 'ring-[#FFD54F]/30',
      text: 'text-[#FFD54F]'
    }
  }
  return {
    primary: '#FF8A80',
    secondary: '#FFAB91',
    gradient: 'from-[#FF8A80] to-[#FFAB91]',
    shadow: 'shadow-[#FF8A80]/30',
    bgLight: '#FFF9F8',
    bgHover: '#FFF0EE',
    ring: 'ring-[#FF8A80]/30',
    text: 'text-[#FF8A80]'
  }
})

// 控制弹窗显示
const showPicker = ref(false)

// 当前视图：date | time
const currentView = ref<'date' | 'time'>('date')

// 解析初始值
const parseDateTime = (value: string) => {
  if (!value) {
    const now = new Date()
    return {
      year: now.getFullYear(),
      month: now.getMonth(),
      day: now.getDate(),
      hour: now.getHours(),
      minute: now.getMinutes()
    }
  }
  const date = new Date(value)
  return {
    year: date.getFullYear(),
    month: date.getMonth(),
    day: date.getDate(),
    hour: date.getHours(),
    minute: date.getMinutes()
  }
}

// 临时选择的日期时间
const tempDate = ref(parseDateTime(props.modelValue))

// 当前显示的年月（用于日历翻页）
const displayYear = ref(tempDate.value.year)
const displayMonth = ref(tempDate.value.month)

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  tempDate.value = parseDateTime(newVal)
  displayYear.value = tempDate.value.year
  displayMonth.value = tempDate.value.month
})

// 月份名称
const monthNames = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// 计算当月日历数据
const calendarDays = computed(() => {
  const year = displayYear.value
  const month = displayMonth.value

  // 当月第一天是星期几
  const firstDay = new Date(year, month, 1).getDay()
  // 当月天数
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  // 上月天数
  const daysInPrevMonth = new Date(year, month, 0).getDate()

  const days: { day: number; type: 'prev' | 'current' | 'next'; isToday: boolean }[] = []

  // 上月的日期
  for (let i = firstDay - 1; i >= 0; i--) {
    days.push({
      day: daysInPrevMonth - i,
      type: 'prev',
      isToday: false
    })
  }

  // 当月的日期
  const today = new Date()
  for (let i = 1; i <= daysInMonth; i++) {
    days.push({
      day: i,
      type: 'current',
      isToday: today.getFullYear() === year && today.getMonth() === month && today.getDate() === i
    })
  }

  // 下月的日期（补齐6行）
  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    days.push({
      day: i,
      type: 'next',
      isToday: false
    })
  }

  return days
})

// 生成小时数组
const hours = Array.from({ length: 24 }, (_, i) => i)
// 生成分钟数组
const minutes = Array.from({ length: 60 }, (_, i) => i)

// 上一月
const prevMonth = () => {
  if (displayMonth.value === 0) {
    displayMonth.value = 11
    displayYear.value--
  } else {
    displayMonth.value--
  }
}

// 下一月
const nextMonth = () => {
  if (displayMonth.value === 11) {
    displayMonth.value = 0
    displayYear.value++
  } else {
    displayMonth.value++
  }
}

// 选择日期
const selectDay = (day: { day: number; type: 'prev' | 'current' | 'next' }) => {
  // 如果日期被禁用，不允许选择
  if (isDisabled(day)) return

  if (day.type === 'prev') {
    prevMonth()
    tempDate.value.day = day.day
  } else if (day.type === 'next') {
    nextMonth()
    tempDate.value.day = day.day
  } else {
    tempDate.value.day = day.day
  }
  tempDate.value.year = displayYear.value
  tempDate.value.month = displayMonth.value
}

// 判断是否选中
const isSelected = (day: { day: number; type: 'prev' | 'current' | 'next' }) => {
  return day.type === 'current' &&
    day.day === tempDate.value.day &&
    displayMonth.value === tempDate.value.month &&
    displayYear.value === tempDate.value.year
}

// 判断日期是否被禁用（早于最小日期）
const isDisabled = (day: { day: number; type: 'prev' | 'current' | 'next' }) => {
  if (!props.minDate) return false

  let year = displayYear.value
  let month = displayMonth.value

  if (day.type === 'prev') {
    month = month === 0 ? 11 : month - 1
    year = month === 11 ? year - 1 : year
  } else if (day.type === 'next') {
    month = month === 11 ? 0 : month + 1
    year = month === 0 ? year + 1 : year
  }

  const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day.day).padStart(2, '0')}`
  return dateStr < props.minDate
}

// 选择小时
const selectHour = (hour: number) => {
  tempDate.value.hour = hour
}

// 选择分钟
const selectMinute = (minute: number) => {
  tempDate.value.minute = minute
}

// 格式化显示
const formatDisplay = computed(() => {
  if (!props.modelValue) return props.placeholder || '选择时间'
  const { year, month, day, hour, minute } = parseDateTime(props.modelValue)
  if (props.mode === 'date') {
    return `${year}年${month + 1}月${day}日`
  }
  return `${year}年${month + 1}月${day}日 ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
})

// 打开选择器
const openPicker = () => {
  tempDate.value = parseDateTime(props.modelValue)
  displayYear.value = tempDate.value.year
  displayMonth.value = tempDate.value.month
  currentView.value = 'date'
  showPicker.value = true
}

// 关闭选择器
const closePicker = () => {
  showPicker.value = false
}

// 确认选择
const confirmSelect = () => {
  const { year, month, day, hour, minute } = tempDate.value
  let dateStr: string
  if (props.mode === 'date') {
    dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  } else {
    dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
  }
  emit('update:modelValue', dateStr)
  closePicker()
}

// 选择今天
const selectToday = () => {
  const now = new Date()
  tempDate.value = {
    year: now.getFullYear(),
    month: now.getMonth(),
    day: now.getDate(),
    hour: now.getHours(),
    minute: now.getMinutes()
  }
  displayYear.value = tempDate.value.year
  displayMonth.value = tempDate.value.month
}

// 切换到时间选择
const goToTimeView = () => {
  currentView.value = 'time'
}

// 切换到日期选择
const goToDateView = () => {
  currentView.value = 'date'
}

// 小时/分钟滚动容器引用
const hourScrollRef = ref<HTMLElement | null>(null)
const minuteScrollRef = ref<HTMLElement | null>(null)

// 滚动到选中的时间
const scrollToSelectedTime = () => {
  setTimeout(() => {
    if (hourScrollRef.value) {
      const selectedHour = hourScrollRef.value.querySelector('.hour-selected')
      if (selectedHour) {
        selectedHour.scrollIntoView({ block: 'center', behavior: 'smooth' })
      }
    }
    if (minuteScrollRef.value) {
      const selectedMinute = minuteScrollRef.value.querySelector('.minute-selected')
      if (selectedMinute) {
        selectedMinute.scrollIntoView({ block: 'center', behavior: 'smooth' })
      }
    }
  }, 100)
}

// 切换到时间视图时滚动到选中项
watch(currentView, (view) => {
  if (view === 'time') {
    scrollToSelectedTime()
  }
})
</script>

<template>
  <!-- 触发按钮 -->
  <div
    @click="openPicker"
    class="flex items-center justify-between p-3.5 bg-stone-50 rounded-xl cursor-pointer hover:bg-stone-100 transition-colors border border-stone-100 active:scale-[0.98]"
  >
    <div class="flex items-center text-stone-600 text-sm">
      <IconCalendar :size="18" class="mr-3" :class="themeColors.text" />
      <span>{{ label }}</span>
    </div>
    <span class="text-sm font-medium" :class="themeColors.text">{{ formatDisplay }}</span>
  </div>

  <!-- 选择器弹窗 -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="showPicker"
        class="fixed inset-0 bg-black/40 z-[100] flex items-end"
        @click="closePicker"
      >
        <Transition name="slide-up">
          <div
            v-if="showPicker"
            class="w-full rounded-t-[2rem] overflow-hidden"
            :style="{ backgroundColor: themeColors.bgLight }"
            @click.stop
          >
            <!-- 拖拽指示器 -->
            <div class="w-10 h-1 bg-stone-300 rounded-full mx-auto mt-3"></div>

            <!-- 头部 -->
            <div class="flex items-center justify-between px-5 py-4">
              <button
                @click="closePicker"
                class="w-9 h-9 rounded-full bg-stone-100 flex items-center justify-center text-stone-400 hover:bg-stone-200 transition-colors"
              >
                <IconX :size="18" />
              </button>
              <h3 class="text-base font-bold text-stone-700 font-serif-title">
                {{ mode === 'date' ? '选择日期' : (currentView === 'date' ? '选择日期' : '选择时间') }}
              </h3>
              <button
                @click="confirmSelect"
                class="px-4 py-2 text-white text-sm font-bold rounded-full active:scale-95 transition-transform"
                :class="[`bg-gradient-to-r ${themeColors.gradient}`, `shadow-lg ${themeColors.shadow}`]"
              >
                确定
              </button>
            </div>

            <!-- 日期/时间切换标签 (仅datetime模式显示) -->
            <div v-if="mode === 'datetime'" class="flex mx-5 mb-4 p-1 bg-white rounded-2xl shadow-inner">
              <button
                @click="goToDateView"
                class="flex-1 py-2.5 text-sm font-medium rounded-xl transition-all"
                :class="currentView === 'date'
                  ? `bg-gradient-to-r ${themeColors.gradient} text-white shadow-md`
                  : 'text-stone-500'"
              >
                📅 日期
              </button>
              <button
                @click="goToTimeView"
                class="flex-1 py-2.5 text-sm font-medium rounded-xl transition-all"
                :class="currentView === 'time'
                  ? `bg-gradient-to-r ${themeColors.gradient} text-white shadow-md`
                  : 'text-stone-500'"
              >
                ⏰ 时间
              </button>
            </div>

            <!-- 当前选择预览 -->
            <div class="mx-5 mb-4 p-4 bg-white rounded-2xl shadow-sm" :style="{ borderColor: themeColors.primary + '20', borderWidth: '1px' }">
              <div class="text-center">
                <span class="text-2xl font-bold text-stone-800">
                  {{ tempDate.year }}年{{ tempDate.month + 1 }}月{{ tempDate.day }}日
                </span>
                <template v-if="mode === 'datetime'">
                  <span class="mx-2 text-stone-300">|</span>
                  <span class="text-2xl font-bold" :class="themeColors.text">
                    {{ String(tempDate.hour).padStart(2, '0') }}:{{ String(tempDate.minute).padStart(2, '0') }}
                  </span>
                </template>
              </div>
            </div>

            <!-- 日期选择视图 -->
            <div v-show="currentView === 'date' || mode === 'date'" class="px-5 pb-8">
              <!-- 月份导航 -->
              <div class="flex items-center justify-between mb-4">
                <button
                  @click="prevMonth"
                  class="w-10 h-10 rounded-full bg-white shadow-sm flex items-center justify-center text-stone-500 transition-colors active:scale-95"
                  :class="`hover:bg-[${themeColors.bgHover}] hover:${themeColors.text}`"
                >
                  <IconChevronRight :size="20" class="rotate-180" />
                </button>
                <div class="text-center">
                  <span class="text-lg font-bold text-stone-700">
                    {{ displayYear }}年 {{ monthNames[displayMonth] }}
                  </span>
                </div>
                <button
                  @click="nextMonth"
                  class="w-10 h-10 rounded-full bg-white shadow-sm flex items-center justify-center text-stone-500 transition-colors active:scale-95"
                  :class="`hover:bg-[${themeColors.bgHover}] hover:${themeColors.text}`"
                >
                  <IconChevronRight :size="20" />
                </button>
              </div>

              <!-- 星期标题 -->
              <div class="grid grid-cols-7 mb-2">
                <div
                  v-for="day in weekDays"
                  :key="day"
                  class="text-center text-xs font-medium py-2"
                  :class="day === '日' || day === '六' ? themeColors.text : 'text-stone-400'"
                >
                  {{ day }}
                </div>
              </div>

              <!-- 日期网格 -->
              <div class="grid grid-cols-7 gap-1">
                <button
                  v-for="(day, index) in calendarDays"
                  :key="index"
                  @click="selectDay(day)"
                  :disabled="isDisabled(day)"
                  class="aspect-square rounded-xl flex items-center justify-center text-sm font-medium transition-all"
                  :class="[
                    isDisabled(day) ? 'text-stone-200 cursor-not-allowed' :
                      day.type !== 'current' ? 'text-stone-300' : 'text-stone-700',
                    day.isToday && !isSelected(day) ? `ring-2 ${themeColors.ring}` : '',
                    isSelected(day)
                      ? `bg-gradient-to-br ${themeColors.gradient} text-white shadow-lg ${themeColors.shadow} scale-110`
                      : !isDisabled(day) ? `hover:bg-[${themeColors.bgHover}]` : ''
                  ]"
                >
                  {{ day.day }}
                </button>
              </div>

              <!-- 快捷按钮 -->
              <div class="flex gap-3 mt-5">
                <button
                  @click="selectToday"
                  class="flex-1 py-3 bg-white rounded-2xl text-sm font-medium text-stone-600 shadow-sm hover:shadow-md transition-all active:scale-95"
                >
                  📍 今天
                </button>
                <button
                  v-if="mode === 'datetime'"
                  @click="goToTimeView"
                  class="flex-1 py-3 rounded-2xl text-sm font-medium shadow-sm hover:shadow-md transition-all active:scale-95"
                  :class="themeColors.text"
                  :style="{ background: `linear-gradient(to right, ${themeColors.bgHover}, white)` }"
                >
                  下一步：选择时间 →
                </button>
              </div>
            </div>

            <!-- 时间选择视图 (仅datetime模式) -->
            <div v-if="mode === 'datetime'" v-show="currentView === 'time'" class="px-5 pb-8">
              <div class="flex gap-4">
                <!-- 小时选择 -->
                <div class="flex-1">
                  <div class="text-center text-xs font-medium text-stone-400 mb-2 uppercase tracking-widest">小时</div>
                  <div
                    ref="hourScrollRef"
                    class="h-48 overflow-y-auto no-scrollbar bg-white rounded-2xl shadow-inner p-2"
                  >
                    <button
                      v-for="hour in hours"
                      :key="hour"
                      @click="selectHour(hour)"
                      class="w-full py-3 rounded-xl text-center font-medium transition-all"
                      :class="[
                        tempDate.hour === hour
                          ? `bg-gradient-to-r ${themeColors.gradient} text-white shadow-md hour-selected`
                          : `text-stone-600 hover:bg-[${themeColors.bgHover}]`
                      ]"
                    >
                      {{ String(hour).padStart(2, '0') }}
                    </button>
                  </div>
                </div>

                <!-- 分隔符 -->
                <div class="flex items-center text-3xl font-bold" :class="themeColors.text">:</div>

                <!-- 分钟选择 -->
                <div class="flex-1">
                  <div class="text-center text-xs font-medium text-stone-400 mb-2 uppercase tracking-widest">分钟</div>
                  <div
                    ref="minuteScrollRef"
                    class="h-48 overflow-y-auto no-scrollbar bg-white rounded-2xl shadow-inner p-2"
                  >
                    <button
                      v-for="minute in minutes"
                      :key="minute"
                      @click="selectMinute(minute)"
                      class="w-full py-3 rounded-xl text-center font-medium transition-all"
                      :class="[
                        tempDate.minute === minute
                          ? `bg-gradient-to-r ${themeColors.gradient} text-white shadow-md minute-selected`
                          : `text-stone-600 hover:bg-[${themeColors.bgHover}]`
                      ]"
                    >
                      {{ String(minute).padStart(2, '0') }}
                    </button>
                  </div>
                </div>
              </div>

              <!-- 快捷时间按钮 -->
              <div class="grid grid-cols-4 gap-2 mt-5">
                <button
                  v-for="time in [
                    { label: '现在', h: new Date().getHours(), m: new Date().getMinutes() },
                    { label: '早上', h: 8, m: 0 },
                    { label: '中午', h: 12, m: 0 },
                    { label: '晚上', h: 20, m: 0 }
                  ]"
                  :key="time.label"
                  @click="tempDate.hour = time.h; tempDate.minute = time.m"
                  class="py-2.5 bg-white rounded-xl text-xs font-medium text-stone-600 shadow-sm transition-all active:scale-95"
                  :class="`hover:bg-[${themeColors.bgHover}] hover:${themeColors.text}`"
                >
                  {{ time.label }}
                </button>
              </div>

              <!-- 返回日期 -->
              <button
                @click="goToDateView"
                class="w-full mt-4 py-3 bg-white rounded-2xl text-sm font-medium text-stone-500 shadow-sm hover:shadow-md transition-all active:scale-95"
              >
                ← 返回修改日期
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 淡入动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 底部滑入动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}
</style>

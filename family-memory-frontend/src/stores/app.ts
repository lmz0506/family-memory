import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TabType } from '@/types'

export const useAppStore = defineStore('app', () => {
  // State
  const isLocked = ref(true)
  const activeTab = ref<TabType>('home')
  const showAddModal = ref(false)
  const showYearPicker = ref(false)
  const showCreateCapsuleModal = ref(false)
  const showInviteFamilyModal = ref(false)
  const selectedYear = ref(new Date().getFullYear().toString())

  // Actions
  const unlock = () => {
    isLocked.value = false
  }

  const setActiveTab = (tab: TabType) => {
    activeTab.value = tab
  }

  const openAddModal = () => {
    showAddModal.value = true
  }

  const closeAddModal = () => {
    showAddModal.value = false
  }

  const openYearPicker = () => {
    showYearPicker.value = true
  }

  const closeYearPicker = () => {
    showYearPicker.value = false
  }

  const setSelectedYear = (year: string) => {
    selectedYear.value = year
  }

  const openCreateCapsuleModal = () => {
    showCreateCapsuleModal.value = true
  }

  const closeCreateCapsuleModal = () => {
    showCreateCapsuleModal.value = false
  }

  const openInviteFamilyModal = () => {
    showInviteFamilyModal.value = true
  }

  const closeInviteFamilyModal = () => {
    showInviteFamilyModal.value = false
  }

  return {
    isLocked,
    activeTab,
    showAddModal,
    showYearPicker,
    showCreateCapsuleModal,
    showInviteFamilyModal,
    selectedYear,
    unlock,
    setActiveTab,
    openAddModal,
    closeAddModal,
    openYearPicker,
    closeYearPicker,
    setSelectedYear,
    openCreateCapsuleModal,
    closeCreateCapsuleModal,
    openInviteFamilyModal,
    closeInviteFamilyModal
  }
})

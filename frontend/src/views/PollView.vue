<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPoll, getResults, submitVote, type Poll, type Results } from '../services/api'
import { getVisitorId } from '../utils/visitor-id'

const route = useRoute()
const pollId = computed(() => String(route.params.id))
const poll = ref<Poll>()
const results = ref<Results>()
const selectedOptionId = ref('')
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const submissionFailed = ref(false)
const connectionState = ref<'connected' | 'reconnecting' | 'offline'>('reconnecting')
let visitorId = ''
let timer: number | undefined
let eventSource: EventSource | undefined
const hasVoted = computed(() => !!results.value?.selectedOptionId)
const isSelectionSynced = computed(() => !!results.value?.selectedOptionId && results.value.selectedOptionId === selectedOptionId.value)

async function refresh() {
  const previousServerChoice = results.value?.selectedOptionId
  const nextResults = await getResults(pollId.value, visitorId)
  results.value = nextResults
  if (nextResults.selectedOptionId && (!selectedOptionId.value || selectedOptionId.value === previousServerChoice)) {
    selectedOptionId.value = nextResults.selectedOptionId
  }
}
async function load() {
  loading.value = true; error.value = ''
  try { visitorId = getVisitorId(); poll.value = await getPoll(pollId.value); await refresh() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '加载失败，请重试。' }
  finally { loading.value = false }
}
async function vote() {
  if (!selectedOptionId.value) { error.value = '请选择一个选项。'; return }
  submitting.value = true; error.value = ''; submissionFailed.value = false
  try { results.value = await submitVote(pollId.value, selectedOptionId.value, visitorId); selectedOptionId.value = results.value.selectedOptionId ?? selectedOptionId.value }
  catch {
    submissionFailed.value = true
    error.value = navigator.onLine
      ? '暂时无法保存你的选择，请稍后重试。'
      : '网络已断开。你的选择尚未保存，恢复网络后请重试。'
    await refresh().catch(() => undefined)
  }
  finally { submitting.value = false }
}

function handleSelectionChange() {
  error.value = ''
  submissionFailed.value = false
}

function connectEvents() {
  eventSource?.close()
  eventSource = new EventSource(`/api/polls/${pollId.value}/events`)
  eventSource.onopen = () => { connectionState.value = 'connected' }
  eventSource.addEventListener('results-updated', () => { refresh().catch(() => undefined) })
  eventSource.onerror = () => { if (navigator.onLine) connectionState.value = 'reconnecting' }
}
function handleOnline() {
  connectionState.value = 'reconnecting'
  if (submissionFailed.value) error.value = '网络已恢复，请点击重试保存。'
  refresh().catch(() => undefined)
  connectEvents()
}
function handleOffline() { connectionState.value = 'offline' }
onMounted(async () => {
  await load()
  if (poll.value) connectEvents()
  connectionState.value = navigator.onLine ? connectionState.value : 'offline'
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  timer = window.setInterval(() => { if (document.visibilityState === 'visible' && navigator.onLine) refresh().catch(() => undefined) }, 15000)
})
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  eventSource?.close()
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
})
</script>

<template>
  <section class="card" aria-live="polite">
    <template v-if="loading"><p>正在加载投票…</p></template>
    <template v-else-if="error && !poll"><p class="error" role="alert">{{ error }}</p><button @click="load">重试</button></template>
    <template v-else-if="poll && results">
      <p class="eyebrow">匿名投票</p><h1>{{ poll.title }}</h1><p class="muted">共 {{ results.totalVotes }} 票</p>
      <form @submit.prevent="vote">
        <fieldset :disabled="submitting"><legend class="sr-only">请选择一个选项</legend>
          <label v-for="option in poll.options" :key="option.id" class="option">
            <input v-model="selectedOptionId" type="radio" name="option" :value="option.id" @change="handleSelectionChange" />{{ option.label }}
          </label>
        </fieldset>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '正在提交…' : (submissionFailed ? '重试保存' : (hasVoted ? '更新投票' : '提交投票')) }}</button>
        <p v-if="isSelectionSynced" class="success">已记录你的投票；你可以随时改投其他选项。</p>
      </form>
      <section class="results" aria-label="投票结果"><h2>投票结果</h2>
        <div v-for="option in results.options" :key="option.id" class="result-row">
          <div><span>{{ option.label }}</span><strong>{{ option.votes }} 票（{{ option.percentage }}%）</strong></div>
          <div class="track" role="progressbar" :aria-valuenow="option.percentage" aria-valuemin="0" aria-valuemax="100" :aria-label="`${option.label}：${option.votes} 票，占 ${option.percentage}%`"><span :style="{ width: `${option.percentage}%` }" /></div>
        </div>
      </section>
    </template>
  </section>
</template>

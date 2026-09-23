<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createPoll } from '../services/api'

const router = useRouter()
const title = ref('')
const options = ref(['', '', ''])
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  const cleanedTitle = title.value.trim()
  const cleanedOptions = options.value.map((option) => option.trim())
  if (!cleanedTitle || cleanedOptions.some((option) => !option) || new Set(cleanedOptions).size !== 3) {
    error.value = '请填写标题和三个互不重复的选项。'
    return
  }
  submitting.value = true
  try {
    const poll = await createPoll(cleanedTitle, cleanedOptions)
    await router.push(poll.url)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '创建失败，请重试。'
  } finally { submitting.value = false }
}
</script>

<template>
  <section class="card">
    <p class="eyebrow">创建投票</p><h1>发起一个三选一投票</h1>
    <form @submit.prevent="submit" novalidate>
      <label>投票标题<input v-model="title" maxlength="200" required placeholder="例如：本周团建活动" /></label>
      <label v-for="(_, index) in options" :key="index">选项 {{ index + 1 }}<input v-model="options[index]" maxlength="100" required :placeholder="`填写选项 ${index + 1}`" /></label>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" :disabled="submitting">{{ submitting ? '正在创建…' : '创建投票' }}</button>
    </form>
  </section>
</template>

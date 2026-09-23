import { createRouter, createWebHistory } from 'vue-router'
import CreatePollView from '../views/CreatePollView.vue'
import PollView from '../views/PollView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/polls/new' },
    { path: '/polls/new', component: CreatePollView },
    { path: '/polls/:id', component: PollView, props: true }
  ]
})

import { BootstrapVue } from 'bootstrap-vue'
import Vue from 'vue'
import '@/styles/bootstrap.scss'

Vue.use(BootstrapVue, {
  breakpoints: [`xs`, 'sm', 'md', 'lg', 'xl', 'xxl']
})

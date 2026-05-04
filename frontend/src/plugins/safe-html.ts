import DOMPurify from 'dompurify'
import Vue from 'vue'

Vue.directive('safe-html', {
  bind(el, binding) {
    el.innerHTML = DOMPurify.sanitize(binding.value ?? '')
  },
  update(el, binding) {
    el.innerHTML = DOMPurify.sanitize(binding.value ?? '')
  }
})

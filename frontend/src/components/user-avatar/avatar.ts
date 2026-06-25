/**
 * Simple Vue 3 avatar component replacement for vue-avatar.
 * Displays initials or an image as a circular avatar.
 */
/* eslint-disable vue/multi-word-component-names */
import { defineComponent, computed, h } from 'vue'

const Avatar = defineComponent({
  name: 'Avatar',
  props: {
    username: {
      type: String,
      default: ''
    },
    src: {
      type: String,
      default: ''
    },
    size: {
      type: Number,
      default: 50
    },
    backgroundColor: {
      type: String,
      default: '#097BB9'
    },
    color: {
      type: String,
      default: '#ffffff'
    },
    rounded: {
      type: Boolean,
      default: true
    }
  },
  setup(props) {
    const initials = computed(() => {
      if (!props.username) return '?'
      const parts = props.username.trim().split(/\s+/)
      if (parts.length >= 2) {
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
      }
      return props.username.substring(0, 2).toUpperCase()
    })

    return () => {
      const style = {
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: `${props.size}px`,
        height: `${props.size}px`,
        borderRadius: props.rounded ? '50%' : '0',
        backgroundColor: props.backgroundColor,
        color: props.color,
        fontSize: `${props.size / 2.5}px`,
        fontWeight: '500',
        overflow: 'hidden' as const
      }

      if (props.src) {
        return h('div', { style }, [
          h('img', {
            src: props.src,
            style: { width: '100%', height: '100%', objectFit: 'cover' },
            alt: props.username
          })
        ])
      }

      return h('div', { style }, initials.value)
    }
  }
})

export default Avatar

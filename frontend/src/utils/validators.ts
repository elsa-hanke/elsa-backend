/**
 * Simple validation utility functions.
 * These are lightweight replacements for vuelidate validators,
 * used with vue-class-component's `validations` option.
 */

export function required(value: any): boolean {
  if (Array.isArray(value)) return value.length > 0
  if (value === null || value === undefined) return false
  return String(value).trim().length > 0
}

export function email(value: string): boolean {
  if (!value) return true
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

export function sameAs(field: string) {
  return function (this: any, value: any): boolean {
    return value === this[field]
  }
}

export function integer(value: any): boolean {
  if (!value && value !== 0) return true
  return Number.isInteger(Number(value))
}

export function between(min: number, max: number) {
  return function (value: number): boolean {
    if (!value && value !== 0) return true
    return Number(value) >= min && Number(value) <= max
  }
}

export function maxValue(max: number) {
  return function (value: number): boolean {
    if (!value && value !== 0) return true
    return Number(value) <= max
  }
}

export function minValue(min: number) {
  return function (value: number): boolean {
    if (!value && value !== 0) return true
    return Number(value) >= min
  }
}

export function requiredIf(condition: any) {
  return function (value: any): boolean {
    const isRequired = typeof condition === 'function' ? condition() : condition
    if (!isRequired) return true
    return required(value)
  }
}

export function minLength(min: number) {
  return function (value: string): boolean {
    if (!value) return true
    return value.length >= min
  }
}

export function maxLength(max: number) {
  return function (value: string): boolean {
    if (!value) return true
    return value.length <= max
  }
}

// Validation type for TypeScript (replaces vuelidate's Validation type)
export interface Validation {
  $invalid: boolean
  $dirty: boolean
  $error: boolean
  $pending: boolean
  $anyDirty: boolean
  $anyError: boolean
  $touch(): void
  $reset(): void
  [key: string]: any
}

// vuelidate-compatible helpers object
export const helpers = {
  regex: (_name: string, pattern: RegExp) => {
    return (value: string): boolean => {
      if (!value) return true
      return pattern.test(value)
    }
  }
}


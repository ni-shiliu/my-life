import { reactive, computed, type Ref } from 'vue'

export function usePasswordRule(password: Ref<string>) {
  const rules = reactive([
    { label: '大写字母', met: computed(() => /[A-Z]/.test(password.value)) },
    { label: '小写字母', met: computed(() => /[a-z]/.test(password.value)) },
    { label: '数字', met: computed(() => /\d/.test(password.value)) },
    { label: '特殊字符', met: computed(() => /[!@#$%^&*()_+\-=]/.test(password.value)) },
    { label: '8-20位', met: computed(() => /^.{8,20}$/.test(password.value)) }
  ])

  const allMet = computed(() => rules.every((r) => r.met))

  return { rules, allMet }
}

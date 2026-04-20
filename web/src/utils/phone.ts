const PHONE_REGEX = /^1[3-9]\d{9}$/

export function validatePhone(phone: string): boolean {
  return PHONE_REGEX.test(phone)
}

export function desensitizePhone(phone: string): string {
  if (!phone || phone.length !== 11) return phone
  return phone.substring(0, 3) + '****' + phone.substring(7)
}

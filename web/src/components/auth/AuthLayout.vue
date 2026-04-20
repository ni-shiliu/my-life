<template>
  <div class="auth-layout">
    <div class="auth-brand">
      <div class="brand-content">
        <h1 class="brand-title">my-life</h1>
        <p class="brand-slogan">让 AI 赋能每一天</p>
      </div>
    </div>
    <div class="auth-form-area">
      <div class="auth-card">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.auth-layout {
  display: flex;
  min-height: 100vh;
}

.auth-brand {
  width: 45%;
  background: $brand-gradient;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.brand-content {
  text-align: center;
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
}

.brand-slogan {
  font-size: 18px;
  opacity: 0.9;
}

.auth-form-area {
  width: 55%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.auth-card {
  background: #fff;
  border-radius: $radius-lg;
  box-shadow: $card-shadow;
  padding: 40px;
  width: 100%;
  max-width: 440px;
}

@media (max-width: 768px) {
  .auth-layout {
    flex-direction: column;
  }

  .auth-brand {
    width: 100%;
    height: 120px;
  }

  .brand-title {
    font-size: 24px;
  }

  .brand-slogan {
    font-size: 14px;
  }

  .auth-form-area {
    width: 100%;
  }

  .auth-card {
    box-shadow: none;
    padding: 24px;
  }
}
</style>

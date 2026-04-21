<template>
  <div class="auth-layout">
    <div class="auth-brand">
      <div class="particle-layer"></div>
      <div class="brand-shimmer"></div>
      <div class="illustration-container">
        <svg class="geometric-illustration" viewBox="0 0 400 400" xmlns="http://www.w3.org/2000/svg">
          <!-- Floating circles -->
          <circle class="floating-circle circle-1" cx="80" cy="80" r="20" />
          <circle class="floating-circle circle-2" cx="320" cy="120" r="35" />
          <circle class="floating-circle circle-3" cx="100" cy="300" r="25" />
          <circle class="floating-circle circle-4" cx="300" cy="320" r="15" />

          <!-- Connection lines -->
          <line class="connection-line line-1" x1="80" y1="80" x2="320" y2="120" />
          <line class="connection-line line-2" x1="320" y1="120" x2="300" y2="320" />
          <line class="connection-line line-3" x1="300" y1="320" x2="100" y2="300" />
          <line class="connection-line line-4" x1="100" y1="300" x2="80" y2="80" />

          <!-- Wave shapes -->
          <path class="wave-shape wave-1" d="M 50 200 Q 100 180 150 200 T 250 200" />
          <path class="wave-shape wave-2" d="M 200 50 Q 220 100 200 150 T 200 250" />

          <!-- Diamond shapes -->
          <polygon class="diamond-shape diamond-1" points="200,60 220,80 200,100 180,80" />
          <polygon class="diamond-shape diamond-2" points="150,280 170,300 150,320 130,300" />

          <!-- Central hub -->
          <circle class="central-hub" cx="200" cy="200" r="40" />
          <circle class="central-hub-inner" cx="200" cy="200" r="25" />
        </svg>
      </div>

      <div class="brand-content">
        <h1 class="brand-title">My-Life</h1>
        <div class="brand-divider"></div>
        <p class="brand-slogan">让 AI 赋能每一天<br/>开启智能学习之旅</p>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
    animation: float 6s ease-in-out infinite;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -30%;
    left: -30%;
    width: 150%;
    height: 150%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 70%);
    animation: float 8s ease-in-out infinite reverse;
  }
}

.illustration-container {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 0;
}

.geometric-illustration {
  width: 100%;
  height: 100%;
  max-width: 500px;
  max-height: 500px;
  opacity: 0.3;
}

.brand-content {
  text-align: center;
  position: relative;
  z-index: 2;
}

// Floating circles
.floating-circle {
  fill: rgba(255, 255, 255, 0.2);
  stroke: rgba(255, 255, 255, 0.4);
  stroke-width: 2;

  &.circle-1 {
    animation: floatCircle1 8s ease-in-out infinite;
  }

  &.circle-2 {
    animation: floatCircle2 10s ease-in-out infinite;
  }

  &.circle-3 {
    animation: floatCircle3 9s ease-in-out infinite;
  }

  &.circle-4 {
    animation: floatCircle4 7s ease-in-out infinite;
  }
}

// Connection lines
.connection-line {
  stroke: rgba(255, 255, 255, 0.15);
  stroke-width: 2;
  stroke-dasharray: 5, 5;

  &.line-1 {
    animation: dashLine 20s linear infinite;
  }

  &.line-2 {
    animation: dashLine 25s linear infinite reverse;
  }

  &.line-3 {
    animation: dashLine 22s linear infinite;
  }

  &.line-4 {
    animation: dashLine 18s linear infinite reverse;
  }
}

// Wave shapes
.wave-shape {
  fill: none;
  stroke: rgba(255, 255, 255, 0.25);
  stroke-width: 3;
  stroke-linecap: round;

  &.wave-1 {
    animation: waveMove 6s ease-in-out infinite;
  }

  &.wave-2 {
    animation: waveMove 7s ease-in-out infinite reverse;
  }
}

// Diamond shapes
.diamond-shape {
  fill: rgba(255, 255, 255, 0.15);
  stroke: rgba(255, 255, 255, 0.3);
  stroke-width: 2;

  &.diamond-1 {
    animation: rotateDiamond 12s linear infinite;
    transform-origin: 200px 80px;
  }

  &.diamond-2 {
    animation: rotateDiamond 15s linear infinite reverse;
    transform-origin: 150px 300px;
  }
}

// Central hub
.central-hub {
  fill: rgba(255, 255, 255, 0.1);
  stroke: rgba(255, 255, 255, 0.3);
  stroke-width: 2;
  animation: pulseHub 4s ease-in-out infinite;
}

.central-hub-inner {
  fill: rgba(255, 255, 255, 0.2);
  stroke: rgba(255, 255, 255, 0.5);
  stroke-width: 2;
  animation: pulseHubInner 3s ease-in-out infinite;
}

.brand-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInScale 600ms ease-out forwards;
  transition: all 300ms cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;

  &:hover {
    transform: scale(1.05);
    box-shadow: 0 12px 40px rgba(255, 255, 255, 0.3);
  }
}

.icon-text {
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -1px;
}

.brand-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  opacity: 0;
  animation: slideUpFade 600ms ease-out 150ms forwards;
  transition: letter-spacing 300ms ease;
  cursor: default;

  &:hover {
    letter-spacing: 0.5px;
  }
}

.brand-divider {
  width: 60px;
  height: 3px;
  background: rgba(255, 255, 255, 0.5);
  margin: 0 auto 20px;
  border-radius: 2px;
  transform: scaleX(0);
  animation: expandWidth 400ms ease-out 300ms forwards;
}

.brand-slogan {
  font-size: 18px;
  opacity: 0;
  font-weight: 300;
  line-height: 1.6;
  max-width: 320px;
  margin: 0 auto;
  animation: fadeIn 600ms ease-out 450ms forwards;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, -30px) scale(1.1);
  }
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes slideUpFade {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes expandWidth {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes particleFloat {
  from {
    background-position: 0 0;
  }
  to {
    background-position: 0 -100px;
  }
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(200%);
  }
}

.particle-layer {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at center, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: particleFloat 20s linear infinite;
  pointer-events: none;
}

.brand-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.1),
    transparent
  );
  animation: shimmer 3s infinite;
  pointer-events: none;
}

// Circle floating animations
@keyframes floatCircle1 {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(10px, -15px);
  }
}

@keyframes floatCircle2 {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(-15px, 10px);
  }
}

@keyframes floatCircle3 {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(15px, 15px);
  }
}

@keyframes floatCircle4 {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(-10px, -10px);
  }
}

// Line dashing animation
@keyframes dashLine {
  to {
    stroke-dashoffset: 1000;
  }
}

// Wave movement
@keyframes waveMove {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

// Diamond rotation
@keyframes rotateDiamond {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// Hub pulse
@keyframes pulseHub {
  0%, 100% {
    r: 40;
    opacity: 0.1;
  }
  50% {
    r: 45;
    opacity: 0.15;
  }
}

@keyframes pulseHubInner {
  0%, 100% {
    r: 25;
    opacity: 0.2;
  }
  50% {
    r: 28;
    opacity: 0.25;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-brand *,
  .auth-brand *::before,
  .auth-brand *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

.auth-form-area {
  width: 55%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(to bottom, #fafbfc 0%, #ffffff 100%);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(to right, transparent, rgba(102, 126, 234, 0.1), transparent);
  }
}

.auth-card {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 48px;
  width: 100%;
  max-width: 460px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 8px 32px rgba(102, 126, 234, 0.12), 0 2px 4px rgba(0, 0, 0, 0.04);
  }
}

@media (max-width: 768px) {
  .auth-layout {
    flex-direction: column;
  }

  .auth-brand {
    width: 100%;
    min-height: 200px;
    padding: 40px 24px;
  }

  .geometric-illustration {
    max-width: 300px;
    max-height: 300px;
    opacity: 0.2;
  }

  .brand-title {
    font-size: 32px;
    margin-bottom: 12px;
  }

  .brand-divider {
    width: 40px;
    height: 2px;
    margin-bottom: 12px;
  }

  .brand-slogan {
    font-size: 15px;
  }

  .auth-form-area {
    width: 100%;
    padding: 32px 24px;
  }

  .auth-card {
    box-shadow: none;
    padding: 32px 24px;
    border-radius: 20px;
  }
}
</style>

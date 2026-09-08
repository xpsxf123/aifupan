<template>
  <div class="login-page">
    <div class="login-page__main">
      <section class="login-page__left">
        <div class="login-page__brand">
          <img class="login-page__logo" :src="logoImg" alt="logo" />
        </div>

        <div class="login-page__hero">
          <img class="login-page__hero-img" :src="aiImg" alt="ai" />
        </div>
      </section>

      <section class="login-page__right">
        <div class="login-box">
          <div class="login-box__slogan">
            <div class="login-box__title">
              <span class="login-box__title-ai">AI</span>
              <span>运营复盘专家</span>
            </div>
            <div class="login-box__sub">
              <span class="onePlus">2万</span>

              <span class="end-title">头部直播间在用</span>
            </div>
          </div>

          <LoginForm v-if="currentView === 'login'" @switch-view="switchView" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file views/Login.vue
   * @description 登录页面
   */
  import { ref } from 'vue'
  import LoginForm from './components/LoginForm.vue'
  import logoImg from '@/assets/images/theme/logo.png'
  import aiImg from '@/assets/images/theme/aiImg.png'

  // 视图状态: 'login'
  const currentView = ref('login')

  /**
   * @description 切换视图
   * @param {string} view 视图名称
   * @param {string} type 登录类型 (可选)
   */
  const switchView = (view) => {
    currentView.value = view
  }
</script>

<style scoped lang="scss">
  .login-page {
    height: 100vh;
    box-sizing: border-box;
    background:
      radial-gradient(1100px 500px at 15% 35%, rgba(94, 129, 244, 0.16) 0%, rgba(94, 129, 244, 0) 60%),
      radial-gradient(900px 460px at 75% 40%, rgba(64, 169, 255, 0.12) 0%, rgba(64, 169, 255, 0) 65%),
      linear-gradient(180deg, #f5fbff 0%, #ffffff 70%);
    overflow: scroll;

    .login-page__main {
      height: 100%;
      display: grid;
      grid-template-columns: minmax(0, 1fr) 460px;
      gap: clamp(20px, 4vw, 56px);
      align-items: center;
      box-sizing: border-box;
      padding: clamp(24px, 4vw, 48px);
      max-width: 1500px;
      margin: 0 auto;
      .login-page__left {
        position: relative;
        min-height: 640px;
        display: flex;
        flex-direction: column;
        justify-content: center;

        .login-page__brand {
          position: absolute;
          top: 0;
          left: 0;
          display: flex;
          align-items: center;
          gap: 10px;

          .login-page__logo {
            height: 60px;
            width: auto;
            user-select: none;
            -webkit-user-drag: none;
          }
        }

        .login-page__hero {
          display: flex;
          justify-content: center;
          align-items: center;
          padding: 40px 0;

          .login-page__hero-img {
            width: min(760px, 100%);
            height: auto;
            user-select: none;
            -webkit-user-drag: none;
          }
        }
      }

      .login-page__right {
        display: flex;
        justify-content: center;

        .login-box {
          width: 350px;
          background: rgba(255, 255, 255, 0.92);
          border: 1px solid rgba(0, 0, 0, 0.06);
          border-radius: 14px;
          box-shadow: 0 14px 30px rgba(20, 60, 160, 0.12);
          padding: clamp(24px, 3.2vw, 40px) clamp(18px, 3vw, 40px) clamp(22px, 3.2vw, 36px);
          backdrop-filter: blur(10px);

          .login-box__slogan {
            text-align: center;
            margin-bottom: 16px;

            .login-box__title {
              font-size: 22px;
              line-height: 30px;
              font-weight: 600;
              color: #2b2f3a;
              letter-spacing: 0.2px;

              .login-box__title-ai {
                color: #ff4d4f;
                font-size: 26px;
                margin-right: 6px;
                font-weight: 700;
              }
            }

            .login-box__sub {
              margin-top: 16px;
              font-size: 14px;
              color: #5e81f4;
              font-weight: 600;
              .onePlus {
                position: relative;

                &::after {
                  content: '+';
                  position: absolute;
                  top: -10px;
                }
              }

              .end-title {
                padding-left: 10px;
              }
            }
          }
        }
      }
    }
  }

  @media (max-width: 1080px) {
    .login-page {
      .login-page__main {
        grid-template-columns: 1fr;
        justify-items: center;
        padding: 28px 16px;
        .login-page__left {
          display: none;
        }
      }
    }
  }

  @media (max-width: 420px) {
    .login-page {
      .login-page__main {
        .login-page__right {
          .login-box {
            .login-box__slogan {
              .login-box__title {
                font-size: 22px;

                .login-box__title-ai {
                  font-size: 26px;
                }
              }
            }
          }
        }
      }
    }
  }
</style>

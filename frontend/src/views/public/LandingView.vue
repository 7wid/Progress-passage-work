<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import type { Component } from 'vue'
import {
  ArrowRight,
  BadgeCheck,
  ClipboardList,
  Code2,
  Database,
  FileCheck2,
  Menu,
  MessageSquare,
  Monitor,
  ShieldCheck,
  Users,
  Workflow,
  Wrench,
  X,
} from '@lucide/vue'
import { PRODUCT_NAME, PRODUCT_NAME_EN } from '@/config/product'

interface FeatureItem {
  icon: Component
  title: string
  description: string
}

const mobileNavOpen = ref(false)

const serviceAreas: FeatureItem[] = [
  {
    icon: Monitor,
    title: '网站与系统建设',
    description: '从业务梳理、界面实现到系统交付，让想法形成可使用的数字化工具。',
  },
  {
    icon: Code2,
    title: '程序调试与自动化',
    description: '协助定位程序问题、优化处理流程，并为重复工作设计自动化方案。',
  },
  {
    icon: Database,
    title: '数据整理与处理',
    description: '面向数据清洗、统计分析和结果呈现，形成清晰、可复用的处理成果。',
  },
  {
    icon: Wrench,
    title: '设备与技术支持',
    description: '对常见设备、软件环境与技术实施问题进行评估，提供可执行的处理建议。',
  },
]

const platformFeatures: FeatureItem[] = [
  {
    icon: ClipboardList,
    title: '标准化需求提交',
    description: '按背景、目标、成果和约束完整描述需求，减少反复确认。',
  },
  {
    icon: ShieldCheck,
    title: '专业可行性评估',
    description: '技术组围绕范围、工作量、风险和实施条件形成明确结论。',
  },
  {
    icon: Users,
    title: '负责人清晰可见',
    description: '管理员统一安排负责人和参与成员，让每项任务都有归属。',
  },
  {
    icon: MessageSquare,
    title: '进度持续同步',
    description: '处理节点、公开进展和状态变化集中记录，需求方随时可查。',
  },
  {
    icon: FileCheck2,
    title: '成果集中交付',
    description: '交付说明与附件统一沉淀，不再散落在群聊和临时链接中。',
  },
  {
    icon: BadgeCheck,
    title: '验收闭环留档',
    description: '由需求方确认成果，完整保留过程与验收结果，方便后续回看。',
  },
]

const workflowSteps = [
  { number: '01', title: '发起需求', description: '说明背景、目标与期望成果' },
  { number: '02', title: '技术评估', description: '确认范围、风险与承接条件' },
  { number: '03', title: '任务分配', description: '明确负责人和参与成员' },
  { number: '04', title: '协作处理', description: '持续同步进展与关键记录' },
  { number: '05', title: '交付验收', description: '提交成果并完成需求确认' },
]

const faqs = [
  {
    question: '哪些需求适合通过系统发起？',
    answer:
      '网站与系统建设、程序调试、数据处理、设备维护等技术需求都可以先提交。技术组会结合范围、资源和风险进行评估，再给出是否承接及后续安排。',
  },
  {
    question: '提交需求后，会立即进入开发吗？',
    answer:
      '不会跳过评估直接实施。需求会先进入技术可行性评估，必要时请你补充资料；评估通过并完成成员分配后，才进入正式处理阶段。',
  },
  {
    question: '我如何知道当前处理到了哪一步？',
    answer:
      '登录后可查看需求状态、负责人、公开进度、交付记录与站内通知。关键状态变化会形成时间线，避免信息只停留在群聊或私信中。',
  },
  {
    question: '怎样才算一项需求真正完成？',
    answer:
      '服务团队提交成果和交付说明后，需求进入待验收阶段。需求方检查成果并确认通过，系统才会将其标记为已完成并归档。',
  },
]

function closeMobileNav(): void {
  mobileNavOpen.value = false
}
</script>

<template>
  <div class="landing-page">
    <a class="skip-link" href="#landing-main">跳到主要内容</a>

    <header class="landing-header">
      <div class="landing-container landing-header__inner">
        <RouterLink class="landing-brand" to="/" aria-label="需求协作中心首页">
          <span class="landing-brand__mark" aria-hidden="true">
            <Workflow :size="21" :stroke-width="2" />
          </span>
          <span class="landing-brand__copy">
            <strong>计算机技术组</strong>
            <small>{{ PRODUCT_NAME_EN }}</small>
          </span>
        </RouterLink>

        <nav class="landing-nav" aria-label="页面导航">
          <a href="#services">服务范围</a>
          <a href="#features">系统功能</a>
          <a href="#workflow">协作流程</a>
          <a href="#team">关于技术组</a>
        </nav>

        <div class="landing-header__actions">
          <RouterLink class="landing-login-link" to="/login">登录</RouterLink>
          <RouterLink
            class="landing-button landing-button--primary landing-register-link"
            to="/register"
          >
            注册账号
            <ArrowRight :size="16" aria-hidden="true" />
          </RouterLink>
          <button
            class="landing-menu-button"
            type="button"
            aria-label="打开页面导航"
            :aria-expanded="mobileNavOpen"
            aria-controls="landing-mobile-nav"
            @click="mobileNavOpen = !mobileNavOpen"
          >
            <X v-if="mobileNavOpen" :size="22" aria-hidden="true" />
            <Menu v-else :size="22" aria-hidden="true" />
          </button>
        </div>
      </div>

      <nav
        v-if="mobileNavOpen"
        id="landing-mobile-nav"
        class="landing-mobile-nav"
        aria-label="移动端页面导航"
      >
        <a href="#services" @click="closeMobileNav">服务范围</a>
        <a href="#features" @click="closeMobileNav">系统功能</a>
        <a href="#workflow" @click="closeMobileNav">协作流程</a>
        <a href="#team" @click="closeMobileNav">关于技术组</a>
        <RouterLink to="/login" @click="closeMobileNav">登录系统</RouterLink>
        <RouterLink to="/register" @click="closeMobileNav">注册需求方账号</RouterLink>
      </nav>
    </header>

    <main id="landing-main">
      <section class="landing-hero" aria-labelledby="landing-title">
        <figure class="hero-scene" aria-label="需求协作中心的需求处理界面示意">
          <div class="hero-scene__window">
            <div class="hero-scene__toolbar">
              <span><Workflow :size="16" aria-hidden="true" />{{ PRODUCT_NAME }}</span>
              <span class="hero-scene__status"><i />服务运行中</span>
            </div>

            <div class="hero-scene__body">
              <aside class="hero-scene__queue" aria-label="最近需求">
                <div class="hero-scene__queue-heading">
                  <strong>我的需求</strong>
                  <span>3</span>
                </div>
                <div class="hero-scene__queue-item is-active">
                  <small>REQ-2026-0087</small>
                  <strong>校园活动报名系统</strong>
                  <span>处理中</span>
                </div>
                <div class="hero-scene__queue-item">
                  <small>REQ-2026-0081</small>
                  <strong>实验数据整理工具</strong>
                  <span>待分配</span>
                </div>
                <div class="hero-scene__queue-item">
                  <small>REQ-2026-0076</small>
                  <strong>设备报修记录优化</strong>
                  <span>已完成</span>
                </div>
              </aside>

              <div class="hero-scene__detail">
                <div class="hero-scene__request-head">
                  <div>
                    <span>当前需求</span>
                    <strong>校园活动报名系统</strong>
                  </div>
                  <span class="hero-scene__tag">处理中</span>
                </div>

                <div class="hero-scene__progress-head">
                  <span>整体进度</span>
                  <strong>68%</strong>
                </div>
                <div class="hero-scene__progress" aria-hidden="true"><span /></div>

                <ol class="hero-scene__stages" aria-label="需求处理进度">
                  <li class="is-completed"><span>1</span><strong>需求评估</strong></li>
                  <li class="is-completed"><span>2</span><strong>成员分配</strong></li>
                  <li class="is-current"><span>3</span><strong>协作处理</strong></li>
                  <li><span>4</span><strong>交付验收</strong></li>
                </ol>

                <div class="hero-scene__activity">
                  <div class="hero-scene__activity-heading">
                    <strong>最新进展</strong>
                    <span>今天 16:30</span>
                  </div>
                  <p>核心报名流程已完成，正在进行移动端适配与提交校验。</p>
                  <div class="hero-scene__activity-meta">
                    <span><Users :size="14" aria-hidden="true" />负责人已确认</span>
                    <span><FileCheck2 :size="14" aria-hidden="true" />交付物持续归档</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <figcaption class="sr-only">
            示例界面展示需求列表、当前处理进度、阶段节点和最新协作记录。
          </figcaption>
        </figure>

        <div class="landing-container landing-hero__inner">
          <div class="landing-hero__copy">
            <span class="landing-eyebrow">计算机技术组 · 统一服务入口</span>
            <h1 id="landing-title">计算机技术组<br />需求协作中心</h1>
            <p>
              把零散的技术需求变成清晰、可评估、可跟踪的协作任务。从提出想法到成果验收，每一步都有明确记录。
            </p>
            <div class="landing-hero__actions">
              <RouterLink class="landing-button landing-button--primary" to="/register">
                发起第一项需求
                <ArrowRight :size="17" aria-hidden="true" />
              </RouterLink>
              <RouterLink class="landing-button landing-button--light" to="/login">
                登录查看进度
              </RouterLink>
            </div>
            <p class="landing-hero__note">
              <BadgeCheck :size="16" aria-hidden="true" />
              面向校内需求方、技术组成员与管理员的统一协作平台
            </p>
          </div>
        </div>
      </section>

      <section class="landing-proof" aria-label="平台价值">
        <div class="landing-container landing-proof__grid">
          <div><strong>标准提交</strong><span>需求信息一次说清</span></div>
          <div><strong>专业评估</strong><span>范围与风险先确认</span></div>
          <div><strong>过程透明</strong><span>状态与进展随时可查</span></div>
          <div><strong>交付闭环</strong><span>成果验收完整留档</span></div>
        </div>
      </section>

      <section id="services" class="landing-section landing-services">
        <div class="landing-container">
          <header class="landing-section-heading">
            <span>技术服务范围</span>
            <h2>让技术问题，进入合适的解决路径</h2>
            <p>
              计算机技术组面向校内常见技术场景提供支持。提交需求后，团队会先判断可行性、工作量与实施条件。
            </p>
          </header>

          <div class="landing-service-grid">
            <article v-for="(service, index) in serviceAreas" :key="service.title">
              <span class="landing-service-grid__number">0{{ index + 1 }}</span>
              <span class="landing-service-grid__icon" aria-hidden="true">
                <component :is="service.icon" :size="23" :stroke-width="1.8" />
              </span>
              <h3>{{ service.title }}</h3>
              <p>{{ service.description }}</p>
            </article>
          </div>
        </div>
      </section>

      <section id="features" class="landing-section landing-features">
        <div class="landing-container">
          <header class="landing-section-heading landing-section-heading--centered">
            <span>系统功能</span>
            <h2>不是另一张在线表格，而是一套完整协作流程</h2>
            <p>需求方清楚掌握进展，技术组集中处理任务，管理员获得统一的流程与数据视图。</p>
          </header>

          <div class="landing-feature-grid">
            <article v-for="feature in platformFeatures" :key="feature.title">
              <span aria-hidden="true">
                <component :is="feature.icon" :size="22" :stroke-width="1.8" />
              </span>
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.description }}</p>
            </article>
          </div>
        </div>
      </section>

      <section id="workflow" class="landing-section landing-workflow">
        <div class="landing-container">
          <header class="landing-section-heading">
            <span>协作流程</span>
            <h2>一项需求，五个明确阶段</h2>
            <p>所有角色围绕同一条流程协作，不再依赖聊天记录推测“现在做到哪了”。</p>
          </header>

          <ol class="landing-workflow__steps">
            <li v-for="step in workflowSteps" :key="step.number">
              <span>{{ step.number }}</span>
              <div>
                <h3>{{ step.title }}</h3>
                <p>{{ step.description }}</p>
              </div>
            </li>
          </ol>

          <div class="landing-workflow__handoff">
            <div>
              <span>需求方视角</span>
              <strong>知道当前阶段，也知道下一步该做什么</strong>
            </div>
            <ArrowRight :size="24" aria-hidden="true" />
            <div>
              <span>技术组视角</span>
              <strong>任务、成员、进展和交付集中在一个工作台</strong>
            </div>
          </div>
        </div>
      </section>

      <section id="team" class="landing-team">
        <div class="landing-container landing-team__grid">
          <div class="landing-team__copy">
            <span>关于技术组</span>
            <h2>把技术能力，变成可持续的服务流程</h2>
            <p>
              计算机技术组承担网站开发、程序调试、数据处理、设备维护等技术支持。过去依赖群聊、私信或在线表格时，需求信息、负责人和处理记录容易分散。
            </p>
            <p>
              需求协作中心把团队的评估、分工、进度与交付沉淀为统一流程，让服务更清楚，也让每一次协作都可追溯。
            </p>
          </div>

          <dl class="landing-team__principles">
            <div>
              <dt>先评估，再承接</dt>
              <dd>不对范围和条件不清的需求作轻率承诺。</dd>
            </div>
            <div>
              <dt>有负责人，有记录</dt>
              <dd>成员分配、公开进展和关键节点都有明确归属。</dd>
            </div>
            <div>
              <dt>权限分层，信息适度</dt>
              <dd>需求方看到必要进展，团队内部信息按权限隔离。</dd>
            </div>
            <div>
              <dt>以成果完成闭环</dt>
              <dd>交付物集中提交，由需求方确认后完成归档。</dd>
            </div>
          </dl>
        </div>
      </section>

      <section class="landing-section landing-faq">
        <div class="landing-container landing-faq__grid">
          <header class="landing-section-heading">
            <span>常见问题</span>
            <h2>发起需求前，你可能想知道</h2>
            <p>先了解协作规则，能帮助技术组更快完成评估，也减少后续反复沟通。</p>
          </header>

          <div class="landing-faq__list">
            <details v-for="(faq, index) in faqs" :key="faq.question" :open="index === 0">
              <summary>
                <span>{{ faq.question }}</span>
                <span aria-hidden="true">+</span>
              </summary>
              <p>{{ faq.answer }}</p>
            </details>
          </div>
        </div>
      </section>

      <section class="landing-cta" aria-labelledby="landing-cta-title">
        <div class="landing-container landing-cta__inner">
          <div>
            <span>从一项清楚的需求开始</span>
            <h2 id="landing-cta-title">准备好把想法交给技术组了吗？</h2>
            <p>注册需求方账号，完整描述背景、目标和期望成果；已有账号可直接登录查看进展。</p>
          </div>
          <div class="landing-cta__actions">
            <RouterLink class="landing-button landing-button--white" to="/register">
              注册并发起需求
              <ArrowRight :size="17" aria-hidden="true" />
            </RouterLink>
            <RouterLink class="landing-button landing-button--outline-white" to="/login">
              登录系统
            </RouterLink>
          </div>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <div class="landing-container landing-footer__inner">
        <div class="landing-footer__brand">
          <span aria-hidden="true"><Workflow :size="19" /></span>
          <div>
            <strong>{{ PRODUCT_NAME }}</strong
            ><small>计算机技术组需求服务平台</small>
          </div>
        </div>
        <nav aria-label="页脚导航">
          <RouterLink to="/login">登录</RouterLink>
          <RouterLink to="/register">注册</RouterLink>
          <a href="#services">服务范围</a>
          <a href="#workflow">协作流程</a>
        </nav>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.landing-page {
  --landing-ink: #102a43;
  --landing-ink-strong: #071b2d;
  --landing-blue: #2563eb;
  --landing-blue-strong: #1d4ed8;
  --landing-cyan: #0e7490;
  --landing-orange: #ea580c;
  --landing-paper: #ffffff;
  --landing-soft: #f4f7fb;
  --landing-line: #dce5f0;
  min-width: 320px;
  color: var(--color-text-primary);
  background: var(--landing-paper);
}

.landing-container {
  width: min(1200px, calc(100% - 64px));
  margin-inline: auto;
}

.landing-header {
  position: sticky;
  z-index: 50;
  top: 0;
  background: rgb(255 255 255 / 96%);
  border-bottom: 1px solid rgb(220 229 240 / 88%);
}

.landing-header__inner {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
}

.landing-brand {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 11px;
}

.landing-brand__mark {
  display: inline-grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  color: #ffffff;
  background: var(--landing-blue);
  border-radius: var(--radius-md);
}

.landing-brand__copy {
  display: grid;
  line-height: 1.25;
}

.landing-brand__copy strong {
  color: var(--landing-ink-strong);
  font-size: 15px;
  font-weight: 700;
}

.landing-brand__copy small {
  color: var(--color-text-tertiary);
  font-size: 9px;
  font-weight: 650;
}

.landing-nav {
  display: flex;
  align-items: center;
  gap: 30px;
  margin-left: auto;
}

.landing-nav a,
.landing-login-link {
  position: relative;
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 600;
  transition: color var(--motion-fast) var(--ease-standard);
}

.landing-nav a::after {
  position: absolute;
  right: 0;
  bottom: 6px;
  left: 0;
  height: 2px;
  background: var(--landing-blue);
  content: '';
  opacity: 0;
  transform: scaleX(0.45);
  transition:
    opacity var(--motion-fast) var(--ease-standard),
    transform var(--motion-fast) var(--ease-standard);
}

.landing-nav a:hover,
.landing-login-link:hover {
  color: var(--landing-blue-strong);
}

.landing-nav a:hover::after,
.landing-nav a:focus-visible::after {
  opacity: 1;
  transform: scaleX(1);
}

.landing-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.landing-button {
  display: inline-flex;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 18px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 680;
  line-height: 1.3;
  transition:
    color var(--motion-fast) var(--ease-standard),
    background-color var(--motion-fast) var(--ease-standard),
    border-color var(--motion-fast) var(--ease-standard),
    box-shadow var(--motion-fast) var(--ease-standard);
}

.landing-button--primary {
  color: #ffffff;
  background: var(--landing-blue);
  box-shadow: 0 6px 18px rgb(37 99 235 / 22%);
}

.landing-button--primary:hover {
  background: var(--landing-blue-strong);
  box-shadow: 0 8px 22px rgb(37 99 235 / 28%);
}

.landing-menu-button {
  display: none;
  width: 44px;
  height: 44px;
  place-items: center;
  padding: 0;
  color: var(--landing-ink);
  background: transparent;
  border: 1px solid var(--landing-line);
  border-radius: var(--radius-md);
}

.landing-mobile-nav {
  position: absolute;
  top: 100%;
  right: 0;
  left: 0;
  display: grid;
  padding: 12px 24px 20px;
  background: #ffffff;
  border-bottom: 1px solid var(--landing-line);
  box-shadow: 0 18px 30px rgb(15 23 42 / 10%);
}

.landing-mobile-nav a {
  display: flex;
  min-height: 48px;
  align-items: center;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border-subtle);
  font-size: 15px;
  font-weight: 600;
}

.landing-hero {
  position: relative;
  height: calc(100dvh - 112px);
  min-height: 520px;
  max-height: 760px;
  overflow: hidden;
  color: #ffffff;
  background: var(--landing-ink);
}

.landing-hero__inner {
  position: relative;
  z-index: 2;
  display: flex;
  height: 100%;
  align-items: center;
}

.landing-hero__copy {
  width: min(570px, 50%);
  animation: landing-hero-enter 420ms var(--ease-standard) both;
}

.landing-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: #9bd8ea;
  font-size: 13px;
  font-weight: 700;
}

.landing-eyebrow::before {
  width: 20px;
  height: 2px;
  background: var(--landing-orange);
  content: '';
}

.landing-hero h1 {
  margin: 0;
  color: #ffffff;
  font-size: 58px;
  font-weight: 720;
  line-height: 1.18;
  text-wrap: balance;
}

.landing-hero__copy > p:not(.landing-hero__note) {
  max-width: 35em;
  margin: 22px 0 0;
  color: #d5e5f2;
  font-size: 17px;
  line-height: 1.8;
}

.landing-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.landing-button--light {
  color: #f7fbff;
  background: rgb(255 255 255 / 8%);
  border-color: rgb(255 255 255 / 35%);
}

.landing-button--light:hover {
  background: rgb(255 255 255 / 14%);
  border-color: rgb(255 255 255 / 58%);
}

.landing-hero__note {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 20px 0 0;
  color: #a9c3d8;
  font-size: 13px;
}

.hero-scene {
  position: absolute;
  z-index: 1;
  top: 50%;
  right: max(24px, calc((100vw - 1200px) / 2));
  width: 620px;
  height: 420px;
  margin: 0;
  transform: translateY(-50%);
  animation: landing-scene-enter 520ms var(--ease-standard) 80ms both;
}

.hero-scene__window {
  height: 100%;
  overflow: hidden;
  color: var(--color-text-primary);
  background: #f8fafc;
  border: 1px solid rgb(255 255 255 / 35%);
  border-radius: var(--radius-md);
  box-shadow: 0 28px 70px rgb(0 0 0 / 28%);
}

.hero-scene__toolbar {
  display: flex;
  height: 52px;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid var(--color-border-subtle);
}

.hero-scene__toolbar > span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--landing-ink);
  font-size: 13px;
  font-weight: 700;
}

.hero-scene__toolbar .hero-scene__status {
  color: var(--color-text-tertiary);
  font-size: 11px;
  font-weight: 600;
}

.hero-scene__status i {
  width: 7px;
  height: 7px;
  background: var(--color-success);
  border-radius: 50%;
}

.hero-scene__body {
  display: grid;
  height: calc(100% - 52px);
  grid-template-columns: 190px 1fr;
}

.hero-scene__queue {
  min-width: 0;
  padding: 18px 14px;
  background: #edf3f9;
  border-right: 1px solid var(--color-border-subtle);
}

.hero-scene__queue-heading,
.hero-scene__request-head,
.hero-scene__progress-head,
.hero-scene__activity-heading,
.hero-scene__activity-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-scene__queue-heading {
  padding: 0 5px 12px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.hero-scene__queue-heading span {
  display: inline-grid;
  min-width: 22px;
  height: 22px;
  place-items: center;
  color: var(--landing-blue-strong);
  background: #dbeafe;
  border-radius: 50%;
  font-size: 10px;
  font-weight: 700;
}

.hero-scene__queue-item {
  display: grid;
  gap: 5px;
  padding: 12px 9px;
  border-top: 1px solid #dce5ef;
}

.hero-scene__queue-item.is-active {
  background: #ffffff;
  border: 1px solid #bfdbfe;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
}

.hero-scene__queue-item small {
  color: var(--color-text-tertiary);
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 9px;
}

.hero-scene__queue-item strong {
  overflow: hidden;
  color: var(--landing-ink-strong);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-scene__queue-item > span {
  color: var(--landing-blue-strong);
  font-size: 10px;
  font-weight: 650;
}

.hero-scene__detail {
  min-width: 0;
  padding: 24px 26px;
}

.hero-scene__request-head > div {
  display: grid;
  gap: 3px;
}

.hero-scene__request-head span,
.hero-scene__progress-head span {
  color: var(--color-text-tertiary);
  font-size: 10px;
}

.hero-scene__request-head strong {
  color: var(--landing-ink-strong);
  font-size: 18px;
}

.hero-scene__request-head .hero-scene__tag {
  padding: 4px 8px;
  color: var(--landing-blue-strong);
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: var(--radius-sm);
  font-size: 10px;
  font-weight: 700;
}

.hero-scene__progress-head {
  margin-top: 23px;
}

.hero-scene__progress-head strong {
  color: var(--landing-blue-strong);
  font-size: 13px;
}

.hero-scene__progress {
  height: 6px;
  margin-top: 8px;
  overflow: hidden;
  background: #e2e8f0;
  border-radius: 3px;
}

.hero-scene__progress span {
  display: block;
  width: 68%;
  height: 100%;
  background: var(--landing-blue);
}

.hero-scene__stages {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
}

.hero-scene__stages li {
  position: relative;
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 7px;
  color: var(--color-text-tertiary);
}

.hero-scene__stages li:not(:last-child)::after {
  position: absolute;
  z-index: 0;
  top: 13px;
  left: calc(50% + 16px);
  width: calc(100% - 32px);
  height: 1px;
  background: #d6e0eb;
  content: '';
}

.hero-scene__stages li > span {
  position: relative;
  z-index: 1;
  display: inline-grid;
  width: 27px;
  height: 27px;
  place-items: center;
  background: #ffffff;
  border: 1px solid #d6e0eb;
  border-radius: 50%;
  font-size: 9px;
  font-weight: 700;
}

.hero-scene__stages li strong {
  overflow: hidden;
  font-size: 10px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-scene__stages li.is-completed > span {
  color: var(--color-success);
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.hero-scene__stages li.is-current {
  color: var(--landing-blue-strong);
}

.hero-scene__stages li.is-current > span {
  color: #ffffff;
  background: var(--landing-blue);
  border-color: var(--landing-blue);
}

.hero-scene__activity {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--color-border-subtle);
}

.hero-scene__activity-heading strong {
  color: var(--landing-ink-strong);
  font-size: 12px;
}

.hero-scene__activity-heading span {
  color: var(--color-text-tertiary);
  font-size: 9px;
}

.hero-scene__activity p {
  margin: 9px 0 0;
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 1.65;
}

.hero-scene__activity-meta {
  justify-content: flex-start;
  gap: 16px;
  margin-top: 13px;
}

.hero-scene__activity-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--color-text-tertiary);
  font-size: 9px;
}

.landing-proof {
  background: #ffffff;
  border-bottom: 1px solid var(--landing-line);
}

.landing-proof__grid {
  display: grid;
  min-height: 92px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: center;
}

.landing-proof__grid > div {
  display: grid;
  gap: 2px;
  padding: 4px 24px;
  border-left: 1px solid var(--landing-line);
}

.landing-proof__grid > div:last-child {
  border-right: 1px solid var(--landing-line);
}

.landing-proof__grid strong {
  color: var(--landing-ink-strong);
  font-size: 14px;
}

.landing-proof__grid span {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.landing-section {
  padding: 104px 0;
}

.landing-section-heading {
  max-width: 720px;
}

.landing-section-heading > span,
.landing-team__copy > span,
.landing-cta__inner > div:first-child > span {
  display: block;
  margin-bottom: 10px;
  color: var(--landing-blue-strong);
  font-size: 12px;
  font-weight: 750;
}

.landing-section-heading h2,
.landing-team h2,
.landing-cta h2 {
  margin: 0;
  color: var(--landing-ink-strong);
  font-size: 38px;
  font-weight: 700;
  line-height: 1.35;
  text-wrap: balance;
}

.landing-section-heading p {
  margin: 16px 0 0;
  color: var(--color-text-secondary);
  font-size: 16px;
  line-height: 1.8;
}

.landing-section-heading--centered {
  margin-inline: auto;
  text-align: center;
}

.landing-service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px;
  margin-top: 58px;
}

.landing-service-grid article {
  position: relative;
  min-width: 0;
  padding-top: 24px;
  border-top: 2px solid var(--landing-line);
}

.landing-service-grid article:nth-child(2) {
  border-top-color: #67a5ef;
}

.landing-service-grid article:nth-child(3) {
  border-top-color: var(--landing-cyan);
}

.landing-service-grid article:nth-child(4) {
  border-top-color: var(--landing-orange);
}

.landing-service-grid__number {
  position: absolute;
  top: 18px;
  right: 0;
  color: #a6b4c4;
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 11px;
}

.landing-service-grid__icon,
.landing-feature-grid article > span {
  display: inline-grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--landing-blue-strong);
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: var(--radius-md);
}

.landing-service-grid h3,
.landing-feature-grid h3,
.landing-workflow h3 {
  margin: 18px 0 0;
  color: var(--landing-ink-strong);
  font-size: 18px;
  font-weight: 680;
}

.landing-service-grid p,
.landing-feature-grid p,
.landing-workflow__steps p {
  margin: 10px 0 0;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.75;
}

.landing-features,
.landing-faq {
  background: var(--landing-soft);
}

.landing-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 52px;
}

.landing-feature-grid article {
  min-width: 0;
  padding: 26px;
  background: #ffffff;
  border: 1px solid var(--landing-line);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition:
    border-color var(--motion-fast) var(--ease-standard),
    box-shadow var(--motion-fast) var(--ease-standard);
}

.landing-feature-grid article:hover {
  border-color: #bfdbfe;
  box-shadow: var(--shadow-raised);
}

.landing-feature-grid article:nth-child(3n + 2) > span {
  color: var(--landing-cyan);
  background: #ecfeff;
  border-color: #a5f3fc;
}

.landing-feature-grid article:nth-child(3n) > span {
  color: var(--landing-orange);
  background: #fff7ed;
  border-color: #fed7aa;
}

.landing-workflow__steps {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin: 56px 0 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--landing-line);
}

.landing-workflow__steps li {
  position: relative;
  min-width: 0;
  padding: 26px 20px 0 0;
}

.landing-workflow__steps li > span {
  position: absolute;
  top: -14px;
  left: 0;
  display: inline-grid;
  width: 29px;
  height: 29px;
  place-items: center;
  color: #ffffff;
  background: var(--landing-blue);
  border: 4px solid #ffffff;
  border-radius: 50%;
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 9px;
  font-weight: 700;
}

.landing-workflow__steps h3 {
  margin-top: 0;
  font-size: 16px;
}

.landing-workflow__handoff {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 30px;
  margin-top: 64px;
  padding: 28px 32px;
  background: #f8fafc;
  border: 1px solid var(--landing-line);
  border-radius: var(--radius-md);
}

.landing-workflow__handoff > div {
  display: grid;
  gap: 5px;
}

.landing-workflow__handoff span {
  color: var(--landing-blue-strong);
  font-size: 11px;
  font-weight: 700;
}

.landing-workflow__handoff strong {
  color: var(--landing-ink-strong);
  font-size: 15px;
  line-height: 1.55;
}

.landing-workflow__handoff > svg {
  color: var(--landing-orange);
}

.landing-team {
  padding: 104px 0;
  color: #ffffff;
  background: var(--landing-ink);
}

.landing-team__grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(420px, 1.1fr);
  gap: 88px;
  align-items: start;
}

.landing-team__copy > span {
  color: #8ed2e6;
}

.landing-team h2 {
  color: #ffffff;
}

.landing-team__copy p {
  margin: 22px 0 0;
  color: #cfdeeb;
  font-size: 15px;
  line-height: 1.85;
}

.landing-team__principles {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  border-top: 1px solid rgb(255 255 255 / 18%);
  border-left: 1px solid rgb(255 255 255 / 18%);
}

.landing-team__principles > div {
  min-width: 0;
  padding: 24px;
  border-right: 1px solid rgb(255 255 255 / 18%);
  border-bottom: 1px solid rgb(255 255 255 / 18%);
}

.landing-team__principles dt {
  color: #ffffff;
  font-size: 15px;
  font-weight: 680;
}

.landing-team__principles dd {
  margin: 9px 0 0;
  color: #abc2d5;
  font-size: 13px;
  line-height: 1.7;
}

.landing-faq__grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.75fr) minmax(480px, 1.25fr);
  gap: 80px;
  align-items: start;
}

.landing-faq__list {
  border-top: 1px solid #cfd9e5;
}

.landing-faq details {
  border-bottom: 1px solid #cfd9e5;
}

.landing-faq summary {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  color: var(--landing-ink-strong);
  cursor: pointer;
  font-size: 16px;
  font-weight: 650;
  list-style: none;
}

.landing-faq summary::-webkit-details-marker {
  display: none;
}

.landing-faq summary > span:last-child {
  display: inline-grid;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  place-items: center;
  color: var(--landing-blue-strong);
  border: 1px solid #b9c9db;
  border-radius: 50%;
  font-size: 18px;
  font-weight: 400;
  transition: transform var(--motion-fast) var(--ease-standard);
}

.landing-faq details[open] summary > span:last-child {
  transform: rotate(45deg);
}

.landing-faq details p {
  margin: -4px 48px 22px 0;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.landing-cta {
  padding: 72px 0;
  color: #ffffff;
  background: var(--landing-blue-strong);
}

.landing-cta__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 64px;
}

.landing-cta__inner > div:first-child {
  max-width: 720px;
}

.landing-cta__inner > div:first-child > span {
  color: #bfdbfe;
}

.landing-cta h2 {
  color: #ffffff;
  font-size: 34px;
}

.landing-cta p {
  margin: 12px 0 0;
  color: #dbeafe;
  font-size: 15px;
  line-height: 1.75;
}

.landing-cta__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.landing-button--white {
  color: var(--landing-blue-strong);
  background: #ffffff;
  border-color: #ffffff;
}

.landing-button--white:hover {
  background: #eff6ff;
}

.landing-button--outline-white {
  color: #ffffff;
  background: transparent;
  border-color: rgb(255 255 255 / 62%);
}

.landing-button--outline-white:hover {
  background: rgb(255 255 255 / 10%);
  border-color: #ffffff;
}

.landing-footer {
  padding: 28px 0;
  background: var(--landing-ink-strong);
  border-top: 1px solid rgb(255 255 255 / 10%);
}

.landing-footer__inner,
.landing-footer__brand,
.landing-footer nav {
  display: flex;
  align-items: center;
}

.landing-footer__inner {
  justify-content: space-between;
  gap: 32px;
}

.landing-footer__brand {
  gap: 10px;
}

.landing-footer__brand > span {
  display: inline-grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #ffffff;
  background: var(--landing-blue);
  border-radius: var(--radius-md);
}

.landing-footer__brand > div {
  display: grid;
}

.landing-footer__brand strong {
  color: #ffffff;
  font-size: 13px;
}

.landing-footer__brand small {
  color: #8ca7bd;
  font-size: 10px;
}

.landing-footer nav {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 24px;
}

.landing-footer nav a {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  color: #afc3d4;
  font-size: 12px;
  transition: color var(--motion-fast) var(--ease-standard);
}

.landing-footer nav a:hover {
  color: #ffffff;
}

@keyframes landing-hero-enter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
}

@keyframes landing-scene-enter {
  from {
    opacity: 0;
    transform: translateY(calc(-50% + 12px));
  }
}

@media (max-width: 1180px) {
  .landing-container {
    width: min(100% - 48px, 1200px);
  }

  .landing-nav {
    gap: 20px;
  }

  .hero-scene {
    right: 24px;
    width: calc(50vw - 40px);
    height: 400px;
  }

  .hero-scene__body {
    grid-template-columns: 160px 1fr;
  }

  .landing-hero h1 {
    font-size: 50px;
  }

  .landing-team__grid {
    gap: 56px;
  }
}

@media (max-width: 900px) {
  .landing-nav,
  .landing-login-link,
  .landing-register-link {
    display: none;
  }

  .landing-menu-button {
    display: grid;
  }

  .landing-hero__copy {
    width: 48%;
  }

  .landing-hero h1 {
    font-size: 44px;
  }

  .hero-scene {
    right: 24px;
    width: clamp(300px, calc(52vw - 60px), 400px);
  }

  .hero-scene__body {
    grid-template-columns: 1fr;
  }

  .hero-scene__queue {
    display: none;
  }

  .landing-service-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .landing-feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .landing-workflow__steps {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 36px 0;
    border-top: 0;
  }

  .landing-workflow__steps li {
    padding-top: 22px;
    border-top: 1px solid var(--landing-line);
  }

  .landing-team__grid,
  .landing-faq__grid {
    grid-template-columns: 1fr;
  }

  .landing-team__grid,
  .landing-faq__grid {
    gap: 52px;
  }

  .landing-cta__inner {
    align-items: flex-start;
    flex-direction: column;
    gap: 30px;
  }

  .landing-cta__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .landing-container {
    width: min(100% - 32px, 1200px);
  }

  .landing-header__inner {
    min-height: 64px;
  }

  .landing-brand__mark {
    width: 36px;
    height: 36px;
    flex-basis: 36px;
  }

  .landing-hero {
    height: calc(100svh - 96px);
    min-height: 580px;
    max-height: 680px;
  }

  .landing-hero__inner {
    align-items: flex-start;
    padding-top: 44px;
  }

  .landing-hero__copy {
    width: 100%;
  }

  .landing-eyebrow {
    margin-bottom: 12px;
    font-size: 12px;
  }

  .landing-hero h1 {
    font-size: 38px;
    line-height: 1.2;
  }

  .landing-hero__copy > p:not(.landing-hero__note) {
    max-width: 31em;
    margin-top: 14px;
    font-size: 16px;
    line-height: 1.7;
  }

  .landing-hero__actions {
    margin-top: 20px;
  }

  .landing-hero__note {
    max-width: 330px;
    margin-top: 14px;
    font-size: 12px;
    line-height: 1.5;
  }

  .hero-scene {
    top: auto;
    right: 16px;
    bottom: -22px;
    left: 16px;
    width: auto;
    height: 228px;
    transform: none;
  }

  .hero-scene__body {
    grid-template-columns: 1fr;
  }

  .hero-scene__queue {
    display: none;
  }

  .hero-scene__detail {
    padding: 15px 18px;
  }

  .hero-scene__request-head strong {
    font-size: 14px;
  }

  .hero-scene__progress-head {
    margin-top: 12px;
  }

  .hero-scene__stages {
    margin-top: 14px;
  }

  .hero-scene__activity {
    display: none;
  }

  .hero-scene__stages li strong {
    font-size: 9px;
  }

  .landing-proof__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 12px 0;
  }

  .landing-proof__grid > div,
  .landing-proof__grid > div:last-child {
    min-height: 64px;
    justify-content: center;
    padding: 8px 14px;
    border-right: 0;
  }

  .landing-proof__grid > div:nth-child(n + 3) {
    border-top: 1px solid var(--landing-line);
  }

  .landing-section,
  .landing-team {
    padding: 72px 0;
  }

  .landing-section-heading h2,
  .landing-team h2,
  .landing-cta h2 {
    font-size: 30px;
  }

  .landing-service-grid,
  .landing-feature-grid {
    grid-template-columns: 1fr;
  }

  .landing-service-grid {
    gap: 30px;
    margin-top: 42px;
  }

  .landing-feature-grid {
    margin-top: 38px;
  }

  .landing-feature-grid article {
    padding: 22px;
  }

  .landing-workflow__steps {
    grid-template-columns: 1fr;
    gap: 30px;
    margin-top: 44px;
  }

  .landing-workflow__steps li {
    display: grid;
    grid-template-columns: 42px 1fr;
    gap: 12px;
    padding: 14px 0 0;
  }

  .landing-workflow__steps li > span {
    position: static;
    grid-row: span 2;
    border-width: 0;
  }

  .landing-workflow__steps h3 {
    margin-top: 0;
  }

  .landing-workflow__steps p {
    margin-top: 4px;
  }

  .landing-workflow__handoff {
    grid-template-columns: 1fr;
    gap: 18px;
    margin-top: 46px;
    padding: 24px;
  }

  .landing-workflow__handoff > svg {
    transform: rotate(90deg);
  }

  .landing-team__principles {
    grid-template-columns: 1fr;
  }

  .landing-faq__grid {
    gap: 38px;
  }

  .landing-faq summary {
    min-height: 68px;
    font-size: 15px;
  }

  .landing-cta {
    padding: 58px 0;
  }

  .landing-cta__actions {
    display: grid;
    width: 100%;
    grid-template-columns: 1fr;
  }

  .landing-footer__inner {
    align-items: flex-start;
    flex-direction: column;
  }

  .landing-footer nav {
    justify-content: flex-start;
    gap: 18px;
  }
}

@media (max-width: 390px) {
  .landing-hero h1 {
    font-size: 34px;
  }

  .landing-button {
    padding-inline: 14px;
  }
}

@media (min-width: 641px) and (max-height: 500px) {
  .landing-hero {
    height: calc(100svh - 96px);
    min-height: 280px;
    max-height: none;
  }

  .landing-hero__copy {
    width: 48%;
  }

  .landing-eyebrow {
    margin-bottom: 8px;
    font-size: 11px;
  }

  .landing-hero h1 {
    font-size: 36px;
    line-height: 1.1;
  }

  .landing-hero__copy > p:not(.landing-hero__note) {
    margin-top: 10px;
    font-size: 14px;
    line-height: 1.55;
  }

  .landing-hero__actions {
    margin-top: 14px;
  }

  .landing-hero__note {
    display: none;
  }

  .hero-scene {
    right: 16px;
    width: 380px;
    height: 260px;
  }

  .hero-scene__toolbar {
    height: 40px;
    padding: 0 14px;
  }

  .hero-scene__body {
    height: calc(100% - 40px);
    grid-template-columns: 1fr;
  }

  .hero-scene__queue {
    display: none;
  }

  .hero-scene__detail {
    padding: 14px;
  }

  .hero-scene__activity {
    display: none;
  }

  .hero-scene__progress-head {
    margin-top: 12px;
  }

  .hero-scene__stages {
    margin-top: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .landing-hero__copy,
  .hero-scene {
    animation: none;
  }
}
</style>

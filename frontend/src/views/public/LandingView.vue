<script setup lang="ts">
import {
  ArrowRight,
  BadgeCheck,
  BarChart3,
  Check,
  CheckCircle2,
  ChevronDown,
  CircleHelp,
  Code2,
  Database,
  FileCheck2,
  FileText,
  Laptop,
  Menu,
  MessageCircleMore,
  MonitorSmartphone,
  Paperclip,
  Send,
  ShieldCheck,
  Sparkles,
  Users,
  Wrench,
  X,
} from '@lucide/vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import ProductLogo from '@/components/common/ProductLogo.vue'
import LandingIllustration from '@/components/public/LandingIllustration.vue'
import { PRODUCT_NAME } from '@/config/product'

const mobileMenuOpen = ref(false)

const serviceAreas = [
  {
    icon: MonitorSmartphone,
    title: '网站与系统建设',
    description: '活动报名、信息展示、管理工具等，把使用场景和期望结果告诉我们即可。',
    example: '例如：社团报名页、学院信息系统',
  },
  {
    icon: Code2,
    title: '程序问题与自动化',
    description: '定位程序故障，或把高频、重复的工作整理为更省时的自动化流程。',
    example: '例如：程序修复、批量处理脚本',
  },
  {
    icon: Database,
    title: '数据整理与分析',
    description: '协助清洗、汇总和呈现数据，让散乱信息变成可理解、可使用的结果。',
    example: '例如：数据清洗、统计看板',
  },
  {
    icon: Wrench,
    title: '设备与技术支持',
    description: '针对设备、网络、软件环境等问题提供诊断建议和可执行的处理方案。',
    example: '例如：环境配置、故障排查',
  },
]

const requesterPromises = [
  {
    icon: FileText,
    title: '不用写“技术需求书”',
    description: '说清背景、使用人和期待结果，技术方案由我们评估。',
  },
  {
    icon: MessageCircleMore,
    title: '每个问题都有回应',
    description: '资料不足会明确列出待补内容，不让你猜下一步。',
  },
  {
    icon: BarChart3,
    title: '进展始终看得见',
    description: '受理、处理、交付和验收记录集中在同一条时间线上。',
  },
  {
    icon: ShieldCheck,
    title: '交付由你来确认',
    description: '成果、说明和反馈有据可查，确认符合预期后再完成归档。',
  },
  {
    icon: Users,
    title: '由合适的成员承接',
    description: '团队结合能力与当前任务情况安排负责人，不做黑箱分配。',
  },
  {
    icon: Paperclip,
    title: '资料不再散落',
    description: '参考文件、补充说明和交付链接随需求保存，方便持续协作。',
  },
]

const workflow = [
  { number: '01', title: '描述问题', description: '从“为什么需要”开始，不必先确定技术方案。' },
  { number: '02', title: '补充资料', description: '上传参考文件、期望时间和你心中的理想结果。' },
  {
    number: '03',
    title: '团队评估',
    description: '技术组给出专业可行性评估、承接结论和下一步安排。',
  },
  { number: '04', title: '查看进度', description: '在时间线上了解负责人、当前阶段和最新说明。' },
  { number: '05', title: '确认成果', description: '集中查看交付内容，反馈问题或完成验收。' },
]

const faqs = [
  {
    question: '我不懂编程，也可以提交需求吗？',
    answer:
      '可以。平台首先需要的是问题背景、实际使用场景、希望达到的结果和时间要求。技术组会在评估阶段把这些信息转化为实施方案；如果资料不足，也会给出具体的补充清单。',
  },
  {
    question: '提交后就代表技术组一定会承接吗？',
    answer:
      '不代表。团队会综合需求范围、时间、资源和合规性进行专业可行性评估。无论可以承接、需要补充资料或暂时无法承接，都会在需求详情中说明原因。',
  },
  {
    question: '在哪里查看负责人和最新进度？',
    answer:
      '登录后进入“我的需求”，打开对应需求即可查看当前状态、负责人公开信息、预计时间、进度记录和交付内容；关键变化也会进入通知中心。',
  },
  {
    question: '成果与预期不一致怎么办？',
    answer:
      '在待验收阶段选择“需要调整”并说明具体问题，需求会回到处理中。建议按使用场景逐项反馈，便于负责人快速定位和修正。',
  },
]

let observer: IntersectionObserver | undefined

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape') closeMobileMenu()
}

onMounted(() => {
  document.addEventListener('keydown', handleEscape)
  if (!('IntersectionObserver' in window)) {
    document.querySelectorAll<HTMLElement>('[data-reveal]').forEach((element) => {
      element.dataset.visible = 'true'
    })
    return
  }
  observer = new IntersectionObserver(
    (entries) =>
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        ;(entry.target as HTMLElement).dataset.visible = 'true'
        observer?.unobserve(entry.target)
      }),
    { threshold: 0.14, rootMargin: '0px 0px -48px' },
  )
  document
    .querySelectorAll<HTMLElement>('[data-reveal]')
    .forEach((element) => observer?.observe(element))
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleEscape)
  observer?.disconnect()
})
</script>

<template>
  <div class="landing-page">
    <a class="skip-link" href="#landing-main">跳到主要内容</a>
    <header class="landing-header">
      <div class="landing-shell landing-header__inner">
        <RouterLink class="landing-brand" to="/" aria-label="需求协作中心首页">
          <span class="landing-brand__mark" aria-hidden="true"><ProductLogo :size="22" /></span>
          <span
            ><strong>{{ PRODUCT_NAME }}</strong
            ><small>计算机技术组服务入口</small></span
          >
        </RouterLink>
        <nav class="landing-nav" aria-label="宣传页导航">
          <a href="#services">可以帮什么</a><a href="#journey">如何协作</a
          ><a href="#assurance">服务方式</a><a href="#faq">常见问题</a>
        </nav>
        <div class="landing-header__actions">
          <RouterLink class="landing-link-button" to="/login">登录查看进度</RouterLink>
          <RouterLink class="landing-button landing-button--compact" to="/register"
            >提交新需求<ArrowRight :size="16" aria-hidden="true"
          /></RouterLink>
        </div>
        <button
          class="landing-menu-button"
          type="button"
          :aria-expanded="mobileMenuOpen"
          aria-controls="landing-mobile-nav"
          :aria-label="mobileMenuOpen ? '关闭导航菜单' : '打开导航菜单'"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <X v-if="mobileMenuOpen" :size="22" aria-hidden="true" /><Menu
            v-else
            :size="22"
            aria-hidden="true"
          />
        </button>
      </div>
      <nav
        v-if="mobileMenuOpen"
        id="landing-mobile-nav"
        class="landing-mobile-nav"
        aria-label="移动端导航"
      >
        <a href="#services" @click="closeMobileMenu">可以帮什么</a
        ><a href="#journey" @click="closeMobileMenu">如何协作</a
        ><a href="#assurance" @click="closeMobileMenu">服务方式</a
        ><a href="#faq" @click="closeMobileMenu">常见问题</a>
        <RouterLink to="/login" @click="closeMobileMenu">登录查看进度</RouterLink
        ><RouterLink class="landing-button" to="/register" @click="closeMobileMenu"
          >提交新需求</RouterLink
        >
      </nav>
    </header>

    <main id="landing-main">
      <section class="landing-hero">
        <div class="landing-shell landing-hero__grid">
          <div class="landing-hero__copy" data-reveal data-visible="true">
            <p class="landing-eyebrow">
              <Sparkles :size="16" aria-hidden="true" /> 从问题出发，不从术语开始
            </p>
            <h1>
              <span>把想解决的问题</span>
              <span>交给 <em>计算机技术组</em></span>
            </h1>
            <p class="landing-hero__lead">
              你只需要说清楚“现在遇到了什么”和“希望变成什么样”。从评估、处理到成果确认，所有沟通和进展都在一个地方看得见。
            </p>
            <div class="landing-hero__actions">
              <RouterLink class="landing-button landing-button--hero" to="/register"
                >开始描述我的需求<ArrowRight :size="18" aria-hidden="true"
              /></RouterLink>
              <a class="landing-secondary-button" href="#journey">先看看怎么进行</a>
            </div>
            <ul class="landing-hero__checks" aria-label="服务特点">
              <li><Check :size="16" aria-hidden="true" /> 无需预先懂技术</li>
              <li><Check :size="16" aria-hidden="true" /> 进度与责任人透明</li>
              <li><Check :size="16" aria-hidden="true" /> 交付结果由你确认</li>
            </ul>
          </div>
          <div class="landing-hero__visual" data-reveal data-visible="true">
            <figure class="landing-hero__photo">
              <LandingIllustration variant="hero" />
              <figcaption>从一个问题开始，把每一步整理清楚</figcaption>
            </figure>
            <div class="request-preview" aria-label="需求进度示例">
              <div class="request-preview__header">
                <span class="request-preview__icon"><Laptop :size="18" aria-hidden="true" /></span
                ><span><small>我的需求 · REQ-0823</small><strong>活动报名信息收集页面</strong></span
                ><span class="request-preview__status">处理中</span>
              </div>
              <div class="request-preview__progress" aria-label="当前进度 60%">
                <span style="width: 60%"></span>
              </div>
              <div class="request-preview__timeline">
                <span class="is-complete"
                  ><CheckCircle2 :size="17" aria-hidden="true" /> 已完成专业可行性评估</span
                ><span class="is-current"
                  ><span class="request-preview__dot"></span> 正在搭建可预览版本</span
                ><span><span class="request-preview__dot"></span> 下一步：邀请你确认页面内容</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="landing-proof" aria-label="协作承诺">
        <div class="landing-shell landing-proof__grid">
          <div>
            <BadgeCheck :size="21" aria-hidden="true" /><strong>专业评估</strong
            ><span>不让需求停在“看不懂”</span>
          </div>
          <div>
            <MessageCircleMore :size="21" aria-hidden="true" /><strong>明确反馈</strong
            ><span>每一步都说明原因与下一步</span>
          </div>
          <div>
            <FileCheck2 :size="21" aria-hidden="true" /><strong>可验收交付</strong
            ><span>成果、说明与反馈集中留存</span>
          </div>
        </div>
      </section>

      <section id="services" class="landing-section">
        <div class="landing-shell">
          <div class="landing-section__heading landing-section__heading--split" data-reveal>
            <div>
              <p class="landing-kicker">可以帮你做什么</p>
              <h2 class="landing-title-lines">
                <span>先选择最接近的方向</span>
                <span>不必纠结技术分类</span>
              </h2>
            </div>
            <p>分类只是为了把问题更快送到合适的人手中。即使选不准，也不会影响你提交需求。</p>
          </div>
          <div class="landing-service-grid">
            <article
              v-for="(service, index) in serviceAreas"
              :key="service.title"
              class="landing-service-card"
              data-reveal
              :style="{ '--delay': `${index * 55}ms` }"
            >
              <span class="landing-service-card__number">0{{ index + 1 }}</span
              ><span class="landing-service-card__icon"
                ><component :is="service.icon" :size="24" aria-hidden="true"
              /></span>
              <h3>{{ service.title }}</h3>
              <p>{{ service.description }}</p>
              <small>{{ service.example }}</small>
            </article>
          </div>
        </div>
      </section>

      <section id="journey" class="landing-section landing-section--tint">
        <div class="landing-shell landing-story">
          <div class="landing-story__media" data-reveal>
            <LandingIllustration variant="collaboration" />
            <div class="landing-story__note">
              <MessageCircleMore :size="20" aria-hidden="true" /><span
                ><strong>资料需要补充</strong><small>请提供活动流程和预计参与人数</small></span
              >
            </div>
          </div>
          <div class="landing-story__copy" data-reveal>
            <p class="landing-kicker">需求方的协作体验</p>
            <h2 class="landing-title-lines">
              <span>你不用追着问</span>
              <span>下一步会清楚呈现</span>
            </h2>
            <p>
              群聊里的一句“做到哪了”，往往要翻找很多上下文。平台把关键结论、补充问题、负责人和交付内容放进同一条需求时间线，让沟通更轻、更准确。
            </p>
            <ul>
              <li>
                <CheckCircle2 :size="19" aria-hidden="true" /><span
                  ><strong>看得懂</strong>状态使用日常语言，并说明它意味着什么</span
                >
              </li>
              <li>
                <CheckCircle2 :size="19" aria-hidden="true" /><span
                  ><strong>找得到</strong>资料、回复和成果始终跟随同一个需求</span
                >
              </li>
              <li>
                <CheckCircle2 :size="19" aria-hidden="true" /><span
                  ><strong>有选择</strong>交付后可以通过验收，也可以明确提出调整</span
                >
              </li>
            </ul>
          </div>
        </div>
        <div class="landing-shell landing-workflow" data-reveal>
          <div class="landing-section__heading">
            <p class="landing-kicker">一次完整协作</p>
            <h2 class="landing-title-lines">
              <span>从一个想法出发</span>
              <span>得到可以确认的结果</span>
            </h2>
          </div>
          <ol class="landing-workflow__steps">
            <li v-for="step in workflow" :key="step.number">
              <span>{{ step.number }}</span>
              <div>
                <h3>{{ step.title }}</h3>
                <p>{{ step.description }}</p>
              </div>
            </li>
          </ol>
        </div>
      </section>

      <section id="assurance" class="landing-section">
        <div class="landing-shell">
          <div class="landing-section__heading" data-reveal>
            <p class="landing-kicker">平台如何减少沟通成本</p>
            <h2 class="landing-title-lines">
              <span>把专业能力转化为</span>
              <span>看得见的确定性</span>
            </h2>
          </div>
          <div class="landing-feature-grid">
            <article
              v-for="(promise, index) in requesterPromises"
              :key="promise.title"
              data-reveal
              :style="{ '--delay': `${(index % 3) * 55}ms` }"
            >
              <span><component :is="promise.icon" :size="22" aria-hidden="true" /></span>
              <h3>{{ promise.title }}</h3>
              <p>{{ promise.description }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="landing-section landing-editorial">
        <div class="landing-shell landing-editorial__grid">
          <div class="landing-editorial__copy" data-reveal>
            <p class="landing-kicker">人与流程同样重要</p>
            <h2 class="landing-title-lines">
              <span>技术组负责</span>
              <span>把复杂问题讲清楚</span>
              <span>把每次交付做扎实</span>
            </h2>
            <p>
              平台不会代替真实沟通，它负责保存共识、提醒关键节点，并让每一次讨论都能沉淀为明确行动。
            </p>
            <RouterLink class="landing-inline-link" to="/register"
              >把你的问题告诉我们 <ArrowRight :size="17" aria-hidden="true"
            /></RouterLink>
          </div>
          <figure class="landing-editorial__photo" data-reveal>
            <LandingIllustration variant="delivery" />
            <figcaption><span>统一协作视图</span>先理解目标，再给出方案</figcaption>
          </figure>
        </div>
      </section>

      <section id="faq" class="landing-section landing-section--faq">
        <div class="landing-shell landing-faq">
          <div class="landing-faq__intro" data-reveal>
            <span class="landing-faq__icon"><CircleHelp :size="26" aria-hidden="true" /></span>
            <p class="landing-kicker">常见问题</p>
            <h2 class="landing-title-lines">
              <span>提交之前</span>
              <span>先看看这些常见问题</span>
            </h2>
            <p>仍不确定是否属于技术需求？先提交问题背景，团队会在评估阶段给出明确反馈。</p>
          </div>
          <div class="landing-faq__list" data-reveal>
            <details v-for="(faq, index) in faqs" :key="faq.question" :open="index === 0">
              <summary>{{ faq.question }}<ChevronDown :size="19" aria-hidden="true" /></summary>
              <p>{{ faq.answer }}</p>
            </details>
          </div>
        </div>
      </section>

      <section class="landing-cta">
        <div class="landing-shell landing-cta__inner" data-reveal>
          <div>
            <span class="landing-cta__icon"><Send :size="24" aria-hidden="true" /></span>
            <p class="landing-kicker">准备好开始了吗</p>
            <h2 class="landing-title-lines">
              <span>先把问题说出来</span>
              <span>我们一起梳理清楚</span>
            </h2>
            <p>一个清楚的背景，往往就是解决问题最重要的第一步。</p>
          </div>
          <div class="landing-cta__actions">
            <RouterLink class="landing-button landing-button--light" to="/register"
              >提交我的需求 <ArrowRight :size="18" aria-hidden="true" /></RouterLink
            ><RouterLink class="landing-cta__login" to="/login">已有账号，查看进度</RouterLink>
          </div>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <div class="landing-shell landing-footer__inner">
        <div class="landing-brand landing-brand--footer">
          <span class="landing-brand__mark" aria-hidden="true"><ProductLogo :size="21" /></span
          ><span
            ><strong>{{ PRODUCT_NAME }}</strong
            ><small>计算机技术组服务入口</small></span
          >
        </div>
        <p>让需求被认真理解，让过程清楚可见，让成果可以确认。</p>
        <div class="landing-footer__links">
          <a href="#services">服务范围</a><a href="#journey">协作流程</a><a href="#faq">常见问题</a
          ><RouterLink to="/login">登录</RouterLink>
        </div>
      </div>
      <div class="landing-shell landing-footer__meta">
        <span>© 2026 计算机技术组</span><span>页面插画由项目代码原生绘制</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.landing-page {
  --navy: #102a43;
  --blue: #1f5fd1;
  --ink: #10243e;
  --muted: #516176;
  --line: #dce5ef;
  --paper: #f7f9fc;
  --font-display:
    'MiSans', 'HarmonyOS Sans SC', 'Alibaba PuHuiTi 3', 'PingFang SC', 'Microsoft YaHei UI',
    'Microsoft YaHei', sans-serif;
  --font-body:
    Inter, 'SF Pro Text', 'MiSans', 'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei UI',
    'Microsoft YaHei', system-ui, sans-serif;
  min-height: 100vh;
  color: var(--ink);
  background: #fff;
  font-family: var(--font-body);
  font-feature-settings: 'kern';
}
.landing-shell {
  width: min(1180px, calc(100% - 48px));
  margin-inline: auto;
}
.landing-header {
  position: sticky;
  z-index: 50;
  top: 0;
  background: rgb(255 255 255/96%);
  border-bottom: 1px solid rgb(220 229 239/86%);
  backdrop-filter: blur(12px);
}
.landing-header__inner {
  display: flex;
  min-height: 74px;
  align-items: center;
  gap: 32px;
}
.landing-brand {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  line-height: 1.15;
}
.landing-brand__mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: #fff;
  background: var(--navy);
  border-radius: 10px;
}
.landing-brand span:last-child {
  display: grid;
  gap: 4px;
}
.landing-brand strong {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 680;
  letter-spacing: -0.015em;
}
.landing-brand small {
  color: #6b7b90;
  font-size: 10px;
  letter-spacing: 0.11em;
}
.landing-nav {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-left: auto;
}
.landing-nav a,
.landing-link-button {
  color: #435269;
  font-size: 13px;
  font-weight: 600;
  transition: color 160ms ease;
}
.landing-nav a:hover,
.landing-link-button:hover {
  color: var(--blue);
}
.landing-header__actions {
  display: flex;
  align-items: center;
  gap: 18px;
}
.landing-button,
.landing-secondary-button {
  display: inline-flex;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 20px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-weight: 650;
  transition:
    transform 160ms ease,
    box-shadow 180ms ease,
    background-color 160ms ease,
    border-color 160ms ease;
}
.landing-button {
  color: #fff;
  background: var(--blue);
  box-shadow: 0 8px 20px rgb(31 95 209/20%);
}
.landing-button:hover {
  background: #174fad;
  box-shadow: 0 12px 28px rgb(31 95 209/26%);
  transform: translateY(-2px);
}
.landing-button:active,
.landing-secondary-button:active {
  transform: scale(0.98);
}
.landing-button--compact {
  min-height: 40px;
  padding-inline: 15px;
  font-size: 13px;
  box-shadow: none;
}
.landing-menu-button {
  display: none;
  width: 44px;
  height: 44px;
  place-items: center;
  padding: 0;
  color: var(--ink);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
}
.landing-mobile-nav {
  display: none;
}
.landing-hero {
  position: relative;
  overflow: hidden;
  padding: 78px 0 84px;
  background: var(--paper);
  border-bottom: 1px solid var(--line);
}
.landing-hero::after {
  position: absolute;
  right: -90px;
  bottom: -160px;
  width: 410px;
  height: 410px;
  background: #e8eef8;
  border-radius: 50%;
  content: '';
}
.landing-hero__grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 0.94fr) minmax(520px, 1.06fr);
  align-items: center;
  gap: 68px;
}
.landing-eyebrow,
.landing-kicker {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  color: var(--blue);
  font-size: 12px;
  font-weight: 680;
  letter-spacing: 0.07em;
}
.landing-eyebrow {
  padding: 7px 11px;
  margin-bottom: 22px;
  background: #e8f0fc;
  border: 1px solid #d3e2f7;
  border-radius: 999px;
}
.landing-hero h1 {
  margin: 0;
  color: var(--navy);
  font-family: var(--font-display);
  font-size: clamp(46px, 5vw, 70px);
  font-weight: 660;
  letter-spacing: -0.035em;
  line-height: 1.18;
  text-wrap: balance;
}
.landing-hero h1 span {
  display: block;
  white-space: nowrap;
}
.landing-hero h1 span + span {
  margin-top: 4px;
}
.landing-hero h1 em {
  position: relative;
  color: var(--blue);
  font-style: normal;
  white-space: nowrap;
}
.landing-hero h1 em::after {
  position: absolute;
  right: 0;
  bottom: -5px;
  left: 0;
  height: 4px;
  background: #a8c7f4;
  border-radius: 99px;
  content: '';
}
.landing-hero__lead {
  max-width: 32em;
  margin: 26px 0 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.78;
  letter-spacing: 0.01em;
}
.landing-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 34px;
}
.landing-button--hero {
  min-height: 52px;
  padding-inline: 24px;
}
.landing-secondary-button {
  color: #243b53;
  background: #fff;
  border-color: #cfdbe9;
}
.landing-secondary-button:hover {
  border-color: #9fb6d5;
  box-shadow: 0 8px 22px rgb(16 42 67/8%);
  transform: translateY(-2px);
}
.landing-hero__checks {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
  padding: 0;
  margin: 27px 0 0;
  color: #50647a;
  font-size: 12px;
  list-style: none;
}
.landing-hero__checks li {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.landing-hero__checks svg {
  color: #16805a;
}
.landing-hero__visual {
  position: relative;
  min-height: 570px;
}
.landing-hero__photo {
  position: absolute;
  inset: 0 0 80px 64px;
  margin: 0;
  overflow: hidden;
  background: #dfe7ef;
  border: 1px solid #cbd7e5;
  border-radius: 14px;
  box-shadow: 0 28px 70px rgb(16 42 67/16%);
}
.landing-hero__photo :deep(.landing-illustration) {
  width: 100%;
  height: 100%;
}
.landing-hero__photo figcaption {
  position: absolute;
  z-index: 1;
  top: 20px;
  left: 20px;
  padding: 8px 11px;
  color: #fff;
  background: var(--navy);
  border-radius: 6px;
  font-size: 11px;
  font-weight: 620;
}
.request-preview {
  position: absolute;
  right: 30px;
  bottom: 0;
  width: min(430px, calc(100% - 20px));
  padding: 20px;
  background: #fff;
  border: 1px solid #cfdae7;
  border-radius: 12px;
  box-shadow: 0 24px 56px rgb(16 42 67/22%);
}
.request-preview__header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 11px;
}
.request-preview__icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--blue);
  background: #eaf1fd;
  border-radius: 8px;
}
.request-preview__header span:nth-child(2) {
  display: grid;
  min-width: 0;
  gap: 3px;
}
.request-preview__header small {
  color: #718096;
  font-size: 10px;
}
.request-preview__header strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.request-preview__status {
  padding: 4px 8px;
  color: #1551a5;
  background: #e9f2ff;
  border: 1px solid #c7dcfb;
  border-radius: 99px;
  font-size: 10px;
  font-weight: 700;
}
.request-preview__progress {
  height: 5px;
  margin: 17px 0;
  overflow: hidden;
  background: #e6edf5;
  border-radius: 99px;
}
.request-preview__progress span {
  display: block;
  height: 100%;
  background: var(--blue);
}
.request-preview__timeline {
  display: grid;
  gap: 10px;
  color: #6a788b;
  font-size: 11px;
}
.request-preview__timeline > span {
  display: flex;
  align-items: center;
  gap: 8px;
}
.request-preview__timeline .is-complete {
  color: #187254;
}
.request-preview__timeline .is-current {
  color: #1f4e8c;
  font-weight: 650;
}
.request-preview__dot {
  width: 8px;
  height: 8px;
  margin: 4px;
  background: #c4cfdb;
  border-radius: 50%;
}
.is-current .request-preview__dot {
  background: var(--blue);
  box-shadow: 0 0 0 4px #e8f0fc;
}
.landing-proof {
  background: #fff;
  border-bottom: 1px solid var(--line);
}
.landing-proof__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}
.landing-proof__grid > div {
  display: grid;
  grid-template-columns: auto auto;
  justify-content: center;
  gap: 0 9px;
  padding: 24px;
  border-right: 1px solid var(--line);
}
.landing-proof__grid > div:last-child {
  border-right: 0;
}
.landing-proof svg {
  grid-row: 1 / span 2;
  color: var(--blue);
}
.landing-proof strong {
  font-size: 14px;
  font-weight: 650;
}
.landing-proof span {
  color: #718096;
  font-size: 12px;
}
.landing-section {
  padding: 108px 0;
  scroll-margin-top: 74px;
}
.landing-section--tint {
  background: var(--paper);
  border-block: 1px solid var(--line);
}
.landing-section__heading {
  max-width: 780px;
  margin-bottom: 48px;
}
.landing-section__heading--split {
  display: grid;
  max-width: none;
  grid-template-columns: 1fr 360px;
  align-items: end;
  gap: 60px;
}
.landing-section__heading h2,
.landing-story__copy h2,
.landing-editorial h2,
.landing-faq h2,
.landing-cta h2 {
  margin: 0;
  color: var(--navy);
  font-family: var(--font-display);
  font-size: clamp(32px, 3.6vw, 48px);
  font-weight: 650;
  letter-spacing: -0.025em;
  line-height: 1.28;
  text-wrap: balance;
}
.landing-section__heading--split > p {
  max-width: 29em;
  margin: 0 0 6px;
  color: var(--muted);
  font-size: 15px;
  line-height: 1.75;
}
.landing-title-lines > span {
  display: block;
  width: max-content;
  max-width: 100%;
  white-space: nowrap;
}
.landing-service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid var(--line);
  border-radius: 12px;
}
.landing-service-card {
  position: relative;
  min-width: 0;
  min-height: 330px;
  padding: 28px;
  background: #fff;
  border-right: 1px solid var(--line);
  transition:
    transform 200ms ease,
    box-shadow 200ms ease;
}
.landing-service-card:first-child {
  border-radius: 11px 0 0 11px;
}
.landing-service-card:last-child {
  border-right: 0;
  border-radius: 0 11px 11px 0;
}
.landing-service-card:hover {
  z-index: 1;
  box-shadow: 0 18px 38px rgb(16 42 67/11%);
  transform: translateY(-5px);
}
.landing-service-card__number {
  position: absolute;
  top: 25px;
  right: 25px;
  color: #a7b5c5;
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
.landing-service-card__icon {
  display: grid;
  width: 48px;
  height: 48px;
  margin-bottom: 50px;
  place-items: center;
  color: var(--blue);
  background: #edf3fd;
  border-radius: 10px;
}
.landing-service-card h3,
.landing-feature-grid h3,
.landing-workflow h3 {
  margin: 0;
  color: #17324d;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 650;
  letter-spacing: -0.01em;
}
.landing-service-card p {
  margin: 13px 0 18px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.72;
}
.landing-service-card small {
  color: #78879a;
  font-size: 12px;
  line-height: 1.55;
}
.landing-story {
  display: grid;
  grid-template-columns: 1.03fr 0.97fr;
  align-items: center;
  gap: 88px;
}
.landing-story__media {
  position: relative;
  min-height: 520px;
}
.landing-story__media > :deep(.landing-illustration) {
  width: calc(100% - 52px);
  height: 520px;
  border-radius: 12px;
  box-shadow: 0 24px 58px rgb(16 42 67/14%);
}
.landing-story__note {
  position: absolute;
  right: 0;
  bottom: 38px;
  display: flex;
  width: 300px;
  align-items: flex-start;
  gap: 11px;
  padding: 17px;
  color: #243b53;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 9px;
  box-shadow: 0 18px 36px rgb(16 42 67/18%);
}
.landing-story__note svg {
  flex: 0 0 auto;
  color: #c56a12;
}
.landing-story__note span {
  display: grid;
  gap: 4px;
}
.landing-story__note strong {
  font-size: 12px;
}
.landing-story__note small {
  color: #6f7f92;
  font-size: 11px;
  line-height: 1.5;
}
.landing-story__copy > p:not(.landing-kicker) {
  max-width: 34em;
  margin: 24px 0;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.78;
}
.landing-story__copy ul {
  display: grid;
  gap: 17px;
  padding: 0;
  margin: 30px 0 0;
  list-style: none;
}
.landing-story__copy li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #56677a;
  font-size: 14px;
  line-height: 1.65;
}
.landing-story__copy li svg {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #13805a;
}
.landing-story__copy li strong {
  margin-right: 8px;
  color: #203850;
}
.landing-workflow {
  padding-top: 112px;
}
.landing-workflow__steps {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  padding: 0;
  margin: 0;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 12px;
  list-style: none;
}
.landing-workflow__steps li {
  min-height: 200px;
  padding: 25px 22px;
  border-right: 1px solid var(--line);
}
.landing-workflow__steps li:last-child {
  border-right: 0;
}
.landing-workflow__steps > li > span {
  display: block;
  margin-bottom: 43px;
  color: var(--blue);
  font-family: ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
}
.landing-workflow p {
  margin: 10px 0 0;
  color: #6b7a8d;
  font-size: 13px;
  line-height: 1.68;
}
.landing-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.landing-feature-grid article {
  min-height: 220px;
  padding: 27px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}
.landing-feature-grid article:hover {
  border-color: #b4c8e2;
  box-shadow: 0 14px 32px rgb(16 42 67/8%);
  transform: translateY(-3px);
}
.landing-feature-grid article > span {
  display: grid;
  width: 42px;
  height: 42px;
  margin-bottom: 32px;
  place-items: center;
  color: var(--blue);
  background: #edf3fd;
  border-radius: 8px;
}
.landing-feature-grid p {
  margin: 13px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.72;
}
.landing-editorial {
  padding-top: 36px;
}
.landing-editorial__grid {
  display: grid;
  overflow: hidden;
  grid-template-columns: 0.82fr 1.18fr;
  align-items: center;
  background: var(--navy);
  border-radius: 14px;
}
.landing-editorial__copy {
  padding: 58px;
  color: #fff;
}
.landing-editorial__copy .landing-kicker {
  color: #8fb7ff;
}
.landing-editorial h2 {
  color: #fff;
  font-size: clamp(30px, 3.1vw, 42px);
}
.landing-editorial__copy > p:not(.landing-kicker) {
  max-width: 32em;
  margin: 24px 0;
  color: #c9d6e4;
  font-size: 15px;
  line-height: 1.78;
}
.landing-inline-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #fff;
  font-size: 14px;
  font-weight: 680;
}
.landing-inline-link:hover {
  text-decoration: underline;
  text-underline-offset: 5px;
}
.landing-editorial__photo {
  position: relative;
  height: 520px;
  margin: 0;
}
.landing-editorial__photo :deep(.landing-illustration) {
  width: 100%;
  height: 100%;
}
.landing-editorial__photo figcaption {
  position: absolute;
  z-index: 1;
  right: 22px;
  bottom: 22px;
  display: grid;
  padding: 12px 14px;
  color: #fff;
  background: var(--navy);
  border-radius: 7px;
  font-size: 12px;
}
.landing-editorial__photo figcaption span {
  color: #a8c7ff;
  font-size: 9px;
  letter-spacing: 0.1em;
}
.landing-section--faq {
  background: #fff;
}
.landing-faq {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 100px;
}
.landing-faq__icon {
  display: grid;
  width: 54px;
  height: 54px;
  margin-bottom: 26px;
  place-items: center;
  color: var(--blue);
  background: #edf3fd;
  border-radius: 11px;
}
.landing-faq__intro > p:last-child {
  max-width: 30em;
  margin: 22px 0 0;
  color: var(--muted);
  font-size: 15px;
  line-height: 1.72;
}
.landing-faq h2 {
  font-size: clamp(32px, 3.2vw, 40px);
}
.landing-faq__list {
  border-top: 1px solid var(--line);
}
.landing-faq details {
  border-bottom: 1px solid var(--line);
}
.landing-faq summary {
  display: flex;
  min-height: 76px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  color: #203850;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 620;
  list-style: none;
}
.landing-faq summary::-webkit-details-marker {
  display: none;
}
.landing-faq summary svg {
  flex: 0 0 auto;
  color: #718096;
  transition: transform 180ms ease;
}
.landing-faq details[open] summary svg {
  transform: rotate(180deg);
}
.landing-faq details p {
  max-width: 720px;
  padding: 0 36px 24px 0;
  margin: -5px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.76;
}
.landing-cta {
  padding: 84px 0;
  color: #fff;
  background: var(--blue);
}
.landing-cta__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 60px;
}
.landing-cta__inner > div:first-child {
  max-width: 720px;
}
.landing-cta__icon {
  display: grid;
  width: 48px;
  height: 48px;
  margin-bottom: 25px;
  place-items: center;
  color: var(--blue);
  background: #fff;
  border-radius: 10px;
}
.landing-cta .landing-kicker {
  color: #d5e5ff;
}
.landing-cta h2 {
  color: #fff;
}
.landing-cta p:last-child {
  margin: 18px 0 0;
  color: #dbe8ff;
  font-size: 15px;
  line-height: 1.7;
}
.landing-cta__actions {
  display: grid;
  flex: 0 0 230px;
  gap: 13px;
  text-align: center;
}
.landing-button--light {
  color: #164eaf;
  background: #fff;
}
.landing-button--light:hover {
  background: #f4f8ff;
}
.landing-cta__login {
  color: #eef5ff;
  font-size: 12px;
  font-weight: 620;
}
.landing-footer {
  padding: 48px 0 22px;
  color: #b8c7d8;
  background: #0c2136;
}
.landing-footer__inner {
  display: grid;
  grid-template-columns: 1fr 1.4fr auto;
  align-items: center;
  gap: 40px;
  padding-bottom: 36px;
  border-bottom: 1px solid #233a51;
}
.landing-brand--footer strong {
  color: #fff;
}
.landing-brand--footer small {
  color: #8fa3b8;
}
.landing-brand--footer .landing-brand__mark {
  color: #d8e7ff;
  background: #173a5b;
}
.landing-footer__inner > p {
  margin: 0;
  color: #99acbf;
  font-size: 12px;
}
.landing-footer__links {
  display: flex;
  gap: 20px;
  font-size: 12px;
}
.landing-footer__links a:hover {
  color: #fff;
}
.landing-footer__meta {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  color: #71869b;
  font-size: 10px;
}
[data-reveal] {
  opacity: 0;
  transform: translateY(22px);
  transition:
    opacity 620ms cubic-bezier(0.2, 0, 0, 1) var(--delay, 0ms),
    transform 620ms cubic-bezier(0.2, 0, 0, 1) var(--delay, 0ms);
}
[data-reveal][data-visible='true'] {
  opacity: 1;
  transform: none;
}
@media (max-width: 1050px) {
  .landing-nav {
    display: none;
  }
  .landing-header__actions {
    margin-left: auto;
  }
  .landing-hero__grid {
    grid-template-columns: 1fr 1fr;
    gap: 38px;
  }
  .landing-hero__visual {
    min-height: 520px;
  }
  .landing-hero__photo {
    left: 20px;
  }
  .landing-service-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .landing-service-card:nth-child(2) {
    border-right: 0;
  }
  .landing-service-card:nth-child(-n + 2) {
    border-bottom: 1px solid var(--line);
  }
  .landing-story {
    gap: 48px;
  }
  .landing-workflow__steps {
    grid-template-columns: repeat(3, 1fr);
  }
  .landing-workflow__steps li {
    border-bottom: 1px solid var(--line);
  }
  .landing-workflow__steps li:nth-child(3) {
    border-right: 0;
  }
  .landing-workflow__steps li:nth-child(n + 4) {
    border-bottom: 0;
  }
  .landing-faq {
    gap: 60px;
  }
  .landing-editorial__copy {
    padding: 42px;
  }
}
@media (max-width: 820px) {
  .landing-shell {
    width: min(100% - 36px, 680px);
  }
  .landing-header__actions {
    display: none;
  }
  .landing-menu-button {
    display: grid;
    margin-left: auto;
  }
  .landing-mobile-nav {
    display: grid;
    gap: 4px;
    padding: 12px 18px 18px;
    background: #fff;
    border-top: 1px solid var(--line);
  }
  .landing-mobile-nav a {
    display: flex;
    min-height: 44px;
    align-items: center;
    padding: 0 12px;
    color: #435269;
    border-radius: 7px;
    font-size: 14px;
    font-weight: 620;
  }
  .landing-mobile-nav .landing-button {
    justify-content: center;
    color: #fff;
  }
  .landing-hero {
    padding: 58px 0 68px;
  }
  .landing-hero__grid {
    grid-template-columns: 1fr;
  }
  .landing-hero__visual {
    min-height: 510px;
    margin-top: 14px;
  }
  .landing-hero__photo {
    inset: 0 16px 64px 0;
  }
  .request-preview {
    right: 0;
  }
  .landing-proof__grid {
    grid-template-columns: 1fr;
    padding-block: 4px;
  }
  .landing-proof__grid > div {
    justify-content: start;
    padding: 17px 4px;
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }
  .landing-proof__grid > div:last-child {
    border-bottom: 0;
  }
  .landing-section {
    padding: 84px 0;
  }
  .landing-section__heading--split {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .landing-story {
    grid-template-columns: 1fr;
  }
  .landing-story__media {
    min-height: 480px;
  }
  .landing-story__media > :deep(.landing-illustration) {
    height: 480px;
  }
  .landing-workflow {
    padding-top: 82px;
  }
  .landing-feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .landing-editorial__grid {
    grid-template-columns: 1fr;
  }
  .landing-editorial__photo {
    height: 420px;
  }
  .landing-faq {
    grid-template-columns: 1fr;
    gap: 42px;
  }
  .landing-cta__inner {
    align-items: flex-start;
    flex-direction: column;
  }
  .landing-cta__actions {
    width: min(100%, 280px);
    flex-basis: auto;
  }
  .landing-footer__inner {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
@media (max-width: 560px) {
  .landing-shell {
    width: calc(100% - 28px);
  }
  .landing-header__inner {
    min-height: 66px;
  }
  .landing-brand__mark {
    width: 36px;
    height: 36px;
  }
  .landing-brand strong {
    font-size: 14px;
  }
  .landing-hero h1 {
    font-size: clamp(34px, 11vw, 48px);
  }
  .landing-hero__lead {
    font-size: 16px;
  }
  .landing-hero__actions {
    align-items: stretch;
    flex-direction: column;
  }
  .landing-button--hero,
  .landing-secondary-button {
    width: 100%;
  }
  .landing-hero__checks {
    display: grid;
  }
  .landing-hero__visual {
    min-height: 420px;
  }
  .landing-hero__photo {
    inset: 0 0 76px;
  }
  .request-preview {
    right: 8px;
    width: calc(100% - 16px);
    padding: 16px;
  }
  .request-preview__header {
    grid-template-columns: auto 1fr;
  }
  .request-preview__status {
    display: none;
  }
  .landing-section {
    padding: 68px 0;
  }
  .landing-section__heading {
    margin-bottom: 32px;
  }
  .landing-section__heading h2,
  .landing-story__copy h2,
  .landing-editorial h2,
  .landing-faq h2,
  .landing-cta h2 {
    font-size: 32px;
  }
  .landing-service-grid,
  .landing-feature-grid {
    grid-template-columns: 1fr;
  }
  .landing-service-card,
  .landing-service-card:nth-child(n) {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--line);
    border-radius: 0;
  }
  .landing-service-card:first-child {
    border-radius: 11px 11px 0 0;
  }
  .landing-service-card:last-child {
    border-bottom: 0;
    border-radius: 0 0 11px 11px;
  }
  .landing-service-card__icon {
    margin-bottom: 30px;
  }
  .landing-story__media {
    min-height: 380px;
  }
  .landing-story__media > :deep(.landing-illustration) {
    width: 100%;
    height: 360px;
  }
  .landing-story__note {
    right: 10px;
    bottom: 0;
    width: calc(100% - 20px);
  }
  .landing-workflow__steps {
    grid-template-columns: 1fr;
  }
  .landing-workflow__steps li,
  .landing-workflow__steps li:nth-child(n) {
    display: grid;
    min-height: auto;
    grid-template-columns: 42px 1fr;
    gap: 8px;
    padding: 22px;
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }
  .landing-workflow__steps li:last-child {
    border-bottom: 0;
  }
  .landing-workflow__steps > li > span {
    margin: 3px 0 0;
  }
  .landing-feature-grid article {
    min-height: auto;
  }
  .landing-editorial__copy {
    padding: 34px 24px;
  }
  .landing-editorial__photo {
    height: 330px;
  }
  .landing-faq summary {
    min-height: 70px;
  }
  .landing-cta {
    padding: 68px 0;
  }
  .landing-footer__links {
    flex-wrap: wrap;
  }
  .landing-footer__meta {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
@media (prefers-reduced-motion: reduce) {
  [data-reveal] {
    opacity: 1;
    transform: none;
  }
  .landing-button:hover,
  .landing-secondary-button:hover,
  .landing-service-card:hover,
  .landing-feature-grid article:hover {
    transform: none;
  }
}
</style>

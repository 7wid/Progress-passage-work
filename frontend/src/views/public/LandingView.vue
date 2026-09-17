<script setup lang="ts">
import {
  ArrowDown,
  ArrowRight,
  BadgeCheck,
  Check,
  CheckCircle2,
  ChevronDown,
  Code2,
  Database,
  FileCheck2,
  FileText,
  Menu,
  MessageCircleMore,
  MonitorSmartphone,
  Paperclip,
  ShieldCheck,
  Wrench,
  X,
} from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getRegistrationStatus } from '@/api/auth'
import ProductLogo from '@/components/common/ProductLogo.vue'
import LandingJourney from '@/components/public/LandingJourney.vue'
import LandingOutcomePreview from '@/components/public/LandingOutcomePreview.vue'
import LandingRequestDemo from '@/components/public/LandingRequestDemo.vue'
import {
  getLandingScenario,
  landingScenarios,
  type LandingScenarioId,
} from '@/components/public/landingScenarios'
import { PRODUCT_NAME } from '@/config/product'

const page = ref<HTMLElement>()
const menuButton = ref<HTMLButtonElement>()
const mobileMenuOpen = ref(false)
const registrationEnabled = ref<boolean | null>(null)
const selectedScenario = ref<LandingScenarioId>('website')
const scenario = computed(() => getLandingScenario(selectedScenario.value))
const activeSection = ref('')
const serviceIcons = {
  website: MonitorSmartphone,
  automation: Code2,
  data: Database,
  support: Wrench,
}
const navigation = [
  { id: 'services', label: '可以帮什么' },
  { id: 'journey', label: '如何协作' },
  { id: 'assurance', label: '服务方式' },
  { id: 'faq', label: '常见问题' },
]
const compactCtaLabel = computed(() =>
  registrationEnabled.value === true
    ? '提交新需求'
    : registrationEnabled.value === false
      ? '获取使用账号'
      : '开始使用',
)
const heroCtaLabel = computed(() =>
  registrationEnabled.value === true
    ? '开始描述我的需求'
    : registrationEnabled.value === false
      ? '先获取需求方账号'
      : '了解如何开始',
)
const finalCtaLabel = computed(() =>
  registrationEnabled.value === true
    ? '提交我的需求'
    : registrationEnabled.value === false
      ? '查看账号获取方式'
      : '了解如何开始',
)
const promises = [
  {
    icon: MessageCircleMore,
    title: '说人话，也认真听你说',
    description: '不需要先学会技术术语。说清背景和期待，技术组负责评估方案。',
  },
  {
    icon: Paperclip,
    title: '资料、讨论，都找得到',
    description: '补充说明、参考文件、处理记录与交付内容跟随同一个需求保存。',
  },
  {
    icon: ShieldCheck,
    title: '交付完成，由你来确认',
    description: '符合预期就确认成果，需要调整就说明问题，每次反馈都有记录。',
  },
]
const faqs = [
  {
    question: '我不懂编程，也可以提交需求吗？',
    answer:
      '可以。说明问题背景、实际使用场景、期待结果和时间要求即可。技术组会在评估阶段把这些信息转化为实施方案；资料不足时，也会给出具体的补充清单。',
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

let revealObserver: IntersectionObserver | undefined
let sections: HTMLElement[] = []
let frame: number | undefined
let disposed = false

function closeMobileMenu(restoreFocus = false) {
  mobileMenuOpen.value = false
  if (restoreFocus) menuButton.value?.focus()
}
function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && mobileMenuOpen.value) closeMobileMenu(true)
}
function updateScrollPosition() {
  frame = undefined
  const readingLine = Math.min(window.innerHeight * 0.3, 240)
  activeSection.value =
    sections.filter((section) => section.getBoundingClientRect().top <= readingLine).slice(-1)[0]
      ?.id ?? ''
  const height = document.documentElement.scrollHeight - window.innerHeight
  const progress = height > 0 ? Math.max(0, Math.min(1, window.scrollY / height)) : 0
  page.value?.style.setProperty('--reading-progress', String(progress))
}
function scheduleScrollUpdate() {
  if (frame === undefined) frame = window.requestAnimationFrame(updateScrollPosition)
}
async function loadRegistrationAvailability() {
  try {
    const status = await getRegistrationStatus()
    if (!disposed) registrationEnabled.value = status.enabled
  } catch {
    // Account guidance remains safe when the public status endpoint is unavailable.
    if (!disposed) registrationEnabled.value = false
  }
}

onMounted(() => {
  void loadRegistrationAvailability()
  document.addEventListener('keydown', handleEscape)
  window.addEventListener('scroll', scheduleScrollUpdate, { passive: true })
  window.addEventListener('resize', scheduleScrollUpdate)
  sections = Array.from(page.value?.querySelectorAll<HTMLElement>('[data-section]') ?? [])
  scheduleScrollUpdate()
  if (!('IntersectionObserver' in window)) return
  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-visible')
        revealObserver?.unobserve(entry.target)
      })
    },
    { threshold: 0.08, rootMargin: '0px 0px -30px' },
  )
  page.value?.classList.add('motion-ready')
  page.value
    ?.querySelectorAll<HTMLElement>('[data-reveal]')
    .forEach((element) => revealObserver?.observe(element))
})

onBeforeUnmount(() => {
  disposed = true
  document.removeEventListener('keydown', handleEscape)
  window.removeEventListener('scroll', scheduleScrollUpdate)
  window.removeEventListener('resize', scheduleScrollUpdate)
  revealObserver?.disconnect()
  if (frame !== undefined) window.cancelAnimationFrame(frame)
})
</script>

<template>
  <div ref="page" class="landing-page">
    <a class="skip-link" href="#landing-main">跳到主要内容</a>
    <header class="landing-header">
      <div class="landing-shell landing-header__inner">
        <RouterLink class="landing-brand" to="/" aria-label="需求协作中心首页"
          ><span class="landing-brand__mark"><ProductLogo :size="23" /></span
          ><span
            ><strong>{{ PRODUCT_NAME }}</strong
            ><small>计算机技术组</small></span
          ></RouterLink
        >
        <nav class="landing-nav" aria-label="宣传页导航">
          <a
            v-for="item in navigation"
            :key="item.id"
            :href="`#${item.id}`"
            :aria-current="activeSection === item.id ? 'location' : undefined"
            >{{ item.label }}</a
          >
        </nav>
        <div class="landing-header__actions">
          <RouterLink class="landing-login" to="/login">登录查看进度</RouterLink
          ><RouterLink class="landing-button landing-button--small" to="/register"
            >{{ compactCtaLabel }}<ArrowRight :size="15" aria-hidden="true"
          /></RouterLink>
        </div>
        <button
          ref="menuButton"
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
      <Transition name="mobile-navigation"
        ><nav
          v-if="mobileMenuOpen"
          id="landing-mobile-nav"
          class="landing-mobile-nav"
          aria-label="移动端宣传页导航"
        >
          <a
            v-for="item in navigation"
            :key="item.id"
            :href="`#${item.id}`"
            @click="closeMobileMenu()"
            >{{ item.label }}</a
          ><RouterLink to="/login" @click="closeMobileMenu()">登录查看进度</RouterLink
          ><RouterLink class="landing-button" to="/register" @click="closeMobileMenu()">{{
            compactCtaLabel
          }}</RouterLink>
        </nav></Transition
      >
      <div class="landing-reading-progress" aria-hidden="true"></div>
    </header>

    <main id="landing-main" tabindex="-1">
      <section class="landing-hero" aria-labelledby="landing-title">
        <div class="landing-shell landing-hero__grid">
          <div class="landing-hero__copy">
            <p class="landing-eyebrow"><span aria-hidden="true"></span>计算机技术组服务入口</p>
            <h1 id="landing-title"><span>把想解决的问题</span><span>交给计算机技术组</span></h1>
            <p class="landing-hero__lead">
              活动要报名，表格要整理，程序出了点问题。<br
                class="landing-desktop-break"
              />从你熟悉的场景说起，我们一起把下一步理清楚。
            </p>
            <div class="landing-hero__actions">
              <RouterLink class="landing-button" to="/register"
                >{{ heroCtaLabel }}<ArrowRight :size="18" aria-hidden="true" /></RouterLink
              ><a class="landing-text-link" href="#journey"
                >先看看如何协作<ArrowDown :size="16" aria-hidden="true"
              /></a>
            </div>
            <p v-if="registrationEnabled === false" class="landing-access-note">
              当前采用受控开通方式，请先查看如何获取需求方账号。
            </p>
            <ul class="landing-hero__checks" aria-label="服务特点">
              <li><Check :size="15" aria-hidden="true" />无需预先懂技术</li>
              <li><Check :size="15" aria-hidden="true" />进展看得见</li>
              <li><Check :size="15" aria-hidden="true" />成果由你确认</li>
            </ul>
            <div class="landing-hero__note">
              <MessageCircleMore :size="20" aria-hidden="true" /><span>一个问题，就能开始。</span>
            </div>
          </div>
          <div class="landing-hero__demo">
            <LandingRequestDemo v-model:scenario-id="selectedScenario" />
          </div>
        </div>
        <div class="landing-shell landing-hero__baseline">
          <span>从一个问题出发</span
          ><span class="landing-hero__baseline-line" aria-hidden="true"></span
          ><span>到可以确认的结果</span><ArrowDown :size="16" aria-hidden="true" />
        </div>
      </section>

      <section
        id="services"
        class="landing-section landing-services"
        data-section
        aria-labelledby="services-title"
      >
        <div class="landing-shell">
          <div class="landing-section-heading" data-reveal>
            <div>
              <p class="landing-eyebrow">可以帮什么</p>
              <h2 id="services-title">这些问题，<br />你可能也遇到过。</h2>
            </div>
            <p>
              选择一个接近的场景，看看问题可以怎样被梳理。<br />分类不用选得很准，把期待说清楚就好。
            </p>
          </div>
          <div class="landing-services__layout" data-reveal>
            <div class="landing-services__choices" aria-label="选择服务场景">
              <button
                v-for="item in landingScenarios"
                :key="item.id"
                type="button"
                :aria-pressed="selectedScenario === item.id"
                @click="selectedScenario = item.id"
              >
                <span class="landing-services__icon"
                  ><component :is="serviceIcons[item.id]" :size="23" aria-hidden="true" /></span
                ><span
                  ><strong>{{ item.service }}</strong
                  ><small>{{ item.question }}</small></span
                ><ArrowRight :size="18" aria-hidden="true" />
              </button>
            </div>
            <div class="landing-services__example">
              <Transition name="service-example"
                ><div :key="selectedScenario" class="landing-services__example-content">
                  <span class="landing-sample-label">场景示例</span>
                  <div class="landing-services__problem">
                    <MessageCircleMore :size="23" aria-hidden="true" />
                    <h3>“{{ scenario.question }}”</h3>
                  </div>
                  <p>{{ scenario.description }}</p>
                  <div class="landing-services__connection" aria-hidden="true">
                    <span></span><ArrowDown :size="19" />
                  </div>
                  <div class="landing-services__outcome">
                    <span><FileCheck2 :size="20" aria-hidden="true" />期待的交付</span
                    ><strong>{{ scenario.outcome }}</strong>
                    <LandingOutcomePreview :scenario-id="selectedScenario" />
                    <ul>
                      <li v-for="item in scenario.deliverables" :key="item">
                        <Check :size="14" aria-hidden="true" />{{ item }}
                      </li>
                    </ul>
                  </div>
                </div></Transition
              >
              <p class="landing-services__note">具体范围与交付内容，以团队评估后确认的方案为准。</p>
            </div>
          </div>
        </div>
      </section>

      <section
        id="journey"
        class="landing-section landing-process"
        data-section
        aria-labelledby="journey-title"
      >
        <div class="landing-shell">
          <div class="landing-section-heading" data-reveal>
            <div>
              <p class="landing-eyebrow">如何协作</p>
              <h2 id="journey-title">把每一步，<br />都放在明处。</h2>
            </div>
            <p>你不用追着问“做到哪了”。<br />从描述问题到确认成果，始终有迹可循。</p>
          </div>
          <LandingJourney :scenario-id="selectedScenario" />
        </div>
      </section>

      <section
        id="assurance"
        class="landing-section landing-assurance"
        data-section
        aria-labelledby="assurance-title"
      >
        <div class="landing-shell landing-assurance__layout">
          <div class="landing-assurance__intro" data-reveal>
            <p class="landing-eyebrow">服务方式</p>
            <h2 id="assurance-title">技术交给我们，<br />决定始终有你。</h2>
            <p>
              平台连接的是具体的问题与认真做事的人。它保存共识、记录进展，也把成果的确认权交还给你。
            </p>
            <div class="landing-assurance__seal">
              <BadgeCheck :size="25" aria-hidden="true" /><span
                >先理解目标<br /><strong>再给出方案</strong></span
              >
            </div>
          </div>
          <div class="landing-assurance__list" data-reveal>
            <article v-for="promise in promises" :key="promise.title">
              <span class="landing-assurance__icon"
                ><component :is="promise.icon" :size="23" aria-hidden="true"
              /></span>
              <div>
                <h3>{{ promise.title }}</h3>
                <p>{{ promise.description }}</p>
              </div>
            </article>
            <div class="landing-assurance__receipt">
              <FileText :size="20" aria-hidden="true" /><span
                >每一份资料，每一次反馈，都留在同一条需求里。</span
              ><CheckCircle2 :size="20" aria-hidden="true" />
            </div>
          </div>
        </div>
      </section>

      <section
        id="faq"
        class="landing-section landing-faq-section"
        data-section
        aria-labelledby="faq-title"
      >
        <div class="landing-shell landing-faq">
          <div data-reveal>
            <p class="landing-eyebrow">常见问题</p>
            <h2 id="faq-title">开始之前，<br />你可能想知道。</h2>
            <p class="landing-faq__intro">关于提交、协作和验收，<br />这里有一些具体的回答。</p>
          </div>
          <div class="landing-faq__list" data-reveal>
            <details v-for="(faq, index) in faqs" :key="faq.question" :open="index === 0">
              <summary>{{ faq.question }}<ChevronDown :size="18" aria-hidden="true" /></summary>
              <div>
                <p>{{ faq.answer }}</p>
              </div>
            </details>
          </div>
        </div>
      </section>

      <section class="landing-cta" aria-labelledby="cta-title">
        <div class="landing-shell landing-cta__inner">
          <div>
            <p class="landing-eyebrow">从你想解决的事开始</p>
            <h2 id="cta-title">先把问题说出来，<br />我们一起往下走。</h2>
          </div>
          <div class="landing-cta__actions">
            <RouterLink class="landing-button" to="/register"
              >{{ finalCtaLabel }}<ArrowRight :size="18" aria-hidden="true" /></RouterLink
            ><RouterLink class="landing-text-link" to="/login">已有账号，登录查看进度</RouterLink>
          </div>
        </div>
      </section>
    </main>
    <footer class="landing-footer">
      <div class="landing-shell landing-footer__inner">
        <RouterLink class="landing-brand" to="/"
          ><span class="landing-brand__mark"><ProductLogo :size="21" /></span
          ><span
            ><strong>{{ PRODUCT_NAME }}</strong
            ><small>先理解目标，再给出方案</small></span
          ></RouterLink
        >
        <p>计算机技术组 · 校园技术协作</p>
        <a href="#landing-main">回到顶部<ArrowRight :size="14" aria-hidden="true" /></a>
      </div>
    </footer>
  </div>
</template>

<style scoped src="./landing.css"></style>

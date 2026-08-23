<script setup lang="ts">
defineProps<{
  variant: 'hero' | 'collaboration' | 'delivery'
}>()
</script>

<template>
  <svg
    class="landing-illustration"
    :class="`landing-illustration--${variant}`"
    viewBox="0 0 720 520"
    role="img"
    focusable="false"
  >
    <title v-if="variant === 'hero'">需求从问题描述到成果确认的产品流程插画</title>
    <title v-else-if="variant === 'collaboration'">资料、沟通与处理结论集中协作的插画</title>
    <title v-else>在需求协作中心查看交付进度和确认成果的插画</title>

    <g v-if="variant === 'hero'">
      <rect class="canvas" x="24" y="28" width="672" height="448" rx="40" />
      <circle class="accent-orb" cx="615" cy="104" r="63" />
      <circle class="dot-grid" cx="88" cy="408" r="42" />

      <g class="window">
        <rect class="surface" x="111" y="82" width="498" height="336" rx="24" />
        <path class="window-bar" d="M135 82h450a24 24 0 0 1 24 24v31H111v-31a24 24 0 0 1 24-24Z" />
        <circle class="window-dot" cx="140" cy="110" r="5" />
        <circle class="window-dot" cx="158" cy="110" r="5" />
        <circle class="window-dot" cx="176" cy="110" r="5" />
        <rect class="brand-soft" x="139" y="162" width="52" height="52" rx="14" />
        <path class="brand-stroke" d="m153 188 10 10 17-23" />
        <rect class="ink" x="212" y="166" width="151" height="13" rx="6.5" />
        <rect class="muted" x="212" y="190" width="235" height="8" rx="4" />

        <rect class="panel" x="139" y="244" width="196" height="133" rx="16" />
        <rect class="muted-light" x="158" y="265" width="88" height="8" rx="4" />
        <rect class="ink" x="158" y="286" width="138" height="10" rx="5" />
        <rect class="muted" x="158" y="311" width="148" height="7" rx="3.5" />
        <rect class="muted" x="158" y="330" width="118" height="7" rx="3.5" />
        <rect class="brand" x="158" y="351" width="72" height="12" rx="6" />

        <rect class="panel" x="355" y="244" width="226" height="133" rx="16" />
        <circle class="success" cx="384" cy="274" r="12" />
        <path class="white-stroke" d="m378 274 4 4 8-10" />
        <rect class="ink" x="410" y="268" width="111" height="9" rx="4.5" />
        <path class="timeline-line" d="M384 294v58" />
        <circle class="brand" cx="384" cy="315" r="8" />
        <rect class="muted" x="410" y="311" width="135" height="7" rx="3.5" />
        <circle class="muted-light" cx="384" cy="352" r="8" />
        <rect class="muted" x="410" y="348" width="98" height="7" rx="3.5" />
      </g>

      <g class="floating-card floating-card--left">
        <rect class="surface" x="56" y="171" width="145" height="89" rx="16" />
        <rect class="warning-soft" x="73" y="188" width="34" height="34" rx="10" />
        <path class="warning-stroke" d="M90 197v13M90 217h.1" />
        <rect class="ink" x="119" y="191" width="61" height="8" rx="4" />
        <rect class="muted" x="119" y="208" width="49" height="6" rx="3" />
        <rect class="warning-soft" x="73" y="232" width="76" height="12" rx="6" />
      </g>

      <g class="floating-card floating-card--right">
        <rect class="surface" x="524" y="336" width="145" height="91" rx="16" />
        <circle class="success-soft" cx="553" cy="366" r="15" />
        <path class="success-stroke" d="m546 366 5 5 10-12" />
        <rect class="ink" x="578" y="357" width="68" height="8" rx="4" />
        <rect class="muted" x="578" y="375" width="51" height="6" rx="3" />
        <rect class="success-soft" x="541" y="397" width="93" height="14" rx="7" />
      </g>
    </g>

    <g v-else-if="variant === 'collaboration'">
      <rect class="canvas" x="28" y="30" width="664" height="450" rx="38" />
      <circle class="accent-orb" cx="622" cy="110" r="52" />
      <circle class="brand-orb" cx="91" cy="401" r="28" />

      <g class="document-stack">
        <rect
          class="paper-back"
          x="151"
          y="101"
          width="348"
          height="286"
          rx="22"
          transform="rotate(-6 151 101)"
        />
        <rect class="surface" x="177" y="85" width="368" height="306" rx="22" />
        <rect class="brand-soft" x="209" y="119" width="82" height="30" rx="15" />
        <rect class="ink" x="310" y="127" width="153" height="11" rx="5.5" />
        <rect class="muted" x="209" y="178" width="271" height="9" rx="4.5" />
        <rect class="muted" x="209" y="203" width="236" height="9" rx="4.5" />

        <rect class="panel" x="209" y="242" width="304" height="112" rx="16" />
        <circle class="brand" cx="238" cy="270" r="11" />
        <rect class="ink" x="262" y="264" width="109" height="9" rx="4.5" />
        <rect class="muted" x="262" y="284" width="187" height="7" rx="3.5" />
        <circle class="warning" cx="238" cy="321" r="11" />
        <rect class="ink" x="262" y="315" width="136" height="9" rx="4.5" />
        <rect class="muted" x="262" y="335" width="155" height="7" rx="3.5" />
      </g>

      <g class="floating-card floating-card--left">
        <rect class="surface" x="74" y="160" width="157" height="105" rx="17" />
        <path
          class="attachment"
          d="M104 198v-10a18 18 0 0 1 36 0v31a13 13 0 0 1-26 0v-28a8 8 0 0 1 16 0v24"
        />
        <rect class="ink" x="153" y="183" width="52" height="8" rx="4" />
        <rect class="muted" x="153" y="201" width="41" height="6" rx="3" />
        <rect class="brand-soft" x="96" y="238" width="99" height="12" rx="6" />
      </g>

      <g class="floating-card floating-card--right">
        <path
          class="chat"
          d="M503 170h137a18 18 0 0 1 18 18v72a18 18 0 0 1-18 18h-69l-31 24 6-24h-43a18 18 0 0 1-18-18v-72a18 18 0 0 1 18-18Z"
        />
        <circle class="brand" cx="521" cy="207" r="9" />
        <rect class="ink" x="541" y="201" width="83" height="9" rx="4.5" />
        <rect class="muted" x="515" y="231" width="110" height="7" rx="3.5" />
        <rect class="muted" x="515" y="250" width="79" height="7" rx="3.5" />
      </g>

      <path class="flow-line" d="M137 405c76 34 156 30 233-11s155-45 232-9" />
      <circle class="success" cx="205" cy="421" r="13" />
      <circle class="brand" cx="370" cy="394" r="13" />
      <circle class="warning" cx="544" cy="398" r="13" />
    </g>

    <g v-else>
      <rect class="dark-canvas" x="24" y="28" width="672" height="448" rx="38" />
      <circle class="dark-orb" cx="604" cy="112" r="66" />
      <rect class="surface" x="103" y="82" width="514" height="326" rx="24" />
      <path class="sidebar" d="M127 82h92v326h-92a24 24 0 0 1-24-24V106a24 24 0 0 1 24-24Z" />
      <rect class="brand-soft" x="127" y="112" width="40" height="40" rx="11" />
      <path class="brand-stroke" d="m138 132 8 8 12-17" />
      <rect class="dark-muted" x="127" y="180" width="68" height="8" rx="4" />
      <rect class="brand" x="127" y="214" width="68" height="28" rx="8" />
      <rect class="dark-muted" x="127" y="266" width="52" height="8" rx="4" />
      <rect class="dark-muted" x="127" y="298" width="62" height="8" rx="4" />

      <rect class="ink" x="255" y="119" width="162" height="13" rx="6.5" />
      <rect class="muted" x="255" y="148" width="224" height="8" rx="4" />
      <rect class="brand-soft" x="255" y="190" width="326" height="72" rx="14" />
      <circle class="brand" cx="286" cy="226" r="14" />
      <path class="white-stroke" d="m279 226 5 5 10-12" />
      <rect class="ink" x="317" y="211" width="118" height="9" rx="4.5" />
      <rect class="muted" x="317" y="231" width="172" height="7" rx="3.5" />
      <circle class="success" cx="276" cy="309" r="12" />
      <path class="success-line" d="M288 309h44" />
      <circle class="brand" cx="366" cy="309" r="12" />
      <path class="brand-line" d="M378 309h44" />
      <circle class="muted-light" cx="456" cy="309" r="12" />
      <rect class="ink" x="255" y="348" width="104" height="9" rx="4.5" />
      <rect class="muted" x="255" y="369" width="186" height="7" rx="3.5" />

      <g class="floating-card floating-card--right">
        <rect class="surface" x="490" y="341" width="148" height="100" rx="18" />
        <circle class="success-soft" cx="526" cy="377" r="18" />
        <path class="success-stroke" d="m518 377 6 6 11-13" />
        <rect class="ink" x="554" y="364" width="58" height="8" rx="4" />
        <rect class="muted" x="554" y="383" width="42" height="6" rx="3" />
        <rect class="success-soft" x="512" y="408" width="92" height="17" rx="8.5" />
      </g>
    </g>
  </svg>
</template>

<style scoped>
.landing-illustration {
  display: block;
  width: 100%;
  height: 100%;
}
.canvas {
  fill: #edf3fb;
}
.dark-canvas {
  fill: #102a43;
}
.accent-orb {
  fill: #d9e8fb;
}
.brand-orb {
  fill: #c5d9f6;
}
.dark-orb {
  fill: #1e4264;
}
.dot-grid {
  fill: #d6e4f4;
  stroke: #c6d8ec;
  stroke-width: 2;
  stroke-dasharray: 4 8;
}
.surface {
  fill: #fff;
}
.window-bar {
  fill: #f6f8fb;
}
.window-dot {
  fill: #c3cfdd;
}
.panel {
  fill: #f3f6fa;
}
.paper-back {
  fill: #d7e4f3;
}
.brand {
  fill: #2563eb;
}
.brand-soft {
  fill: #dbeafe;
}
.ink {
  fill: #17324d;
}
.muted {
  fill: #9aabc0;
}
.muted-light {
  fill: #d7e0eb;
}
.success {
  fill: #059669;
}
.success-soft {
  fill: #d1fae5;
}
.warning {
  fill: #ea8b2d;
}
.warning-soft {
  fill: #ffedd5;
}
.sidebar {
  fill: #163d68;
}
.dark-muted {
  fill: #56748f;
}
.chat {
  fill: #fff;
  stroke: #d9e3ee;
  stroke-width: 2;
}
.attachment {
  fill: none;
  stroke: #2563eb;
  stroke-width: 6;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.flow-line {
  fill: none;
  stroke: #b4c8df;
  stroke-width: 5;
  stroke-linecap: round;
  stroke-dasharray: 10 12;
}
.timeline-line {
  fill: none;
  stroke: #c6d4e4;
  stroke-width: 4;
  stroke-linecap: round;
}
.brand-stroke,
.success-stroke,
.white-stroke,
.warning-stroke {
  fill: none;
  stroke: #2563eb;
  stroke-width: 5;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.success-stroke {
  stroke: #059669;
}
.white-stroke {
  stroke: #fff;
  stroke-width: 3;
}
.warning-stroke {
  stroke: #c56a12;
  stroke-width: 4;
}
.success-line,
.brand-line {
  stroke: #8bbca9;
  stroke-width: 4;
  stroke-dasharray: 7 7;
}
.brand-line {
  stroke: #8eb0e8;
}
.window,
.document-stack {
  filter: drop-shadow(0 22px 32px rgb(16 42 67 / 12%));
}
.floating-card {
  filter: drop-shadow(0 16px 22px rgb(16 42 67 / 14%));
  animation: card-float 5.4s ease-in-out infinite;
}
.floating-card--right {
  animation-delay: -1.8s;
}
@keyframes card-float {
  50% {
    transform: translateY(-7px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .floating-card {
    animation: none;
  }
}
</style>

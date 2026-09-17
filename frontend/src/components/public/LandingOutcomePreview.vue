<script setup lang="ts">
import { ArrowRight, Check, CheckCircle2, FileText, FolderInput, FolderOutput } from '@lucide/vue'
import type { LandingScenarioId } from './landingScenarios'

defineProps<{ scenarioId: LandingScenarioId }>()
</script>

<template>
  <figure class="outcome-preview">
    <figcaption>交付效果示意</figcaption>
    <div v-if="scenarioId === 'website'" class="outcome-preview__website">
      <div class="outcome-preview__website-title">
        <FileText :size="17" aria-hidden="true" /><strong>活动报名</strong><span>在线收集</span>
      </div>
      <dl>
        <div>
          <dt>参与者信息</dt>
          <dd>姓名、联系方式</dd>
        </div>
        <div>
          <dt>报名选项</dt>
          <dd>按活动需要设置</dd>
        </div>
      </dl>
      <span class="outcome-preview__foot"
        ><CheckCircle2 :size="14" aria-hidden="true" />报名信息集中汇总</span
      >
    </div>
    <div v-else-if="scenarioId === 'data'" class="outcome-preview__data">
      <table>
        <caption class="sr-only">
          数据处理前后的示意对照
        </caption>
        <thead>
          <tr>
            <th scope="col">整理项目</th>
            <th scope="col">原始资料</th>
            <th scope="col">整理结果</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th scope="row">日期格式</th>
            <td>格式不一致</td>
            <td><Check :size="12" aria-hidden="true" />统一格式</td>
          </tr>
          <tr>
            <th scope="row">重复记录</th>
            <td>需要核对</td>
            <td><Check :size="12" aria-hidden="true" />标记与整理</td>
          </tr>
        </tbody>
      </table>
    </div>
    <ol v-else-if="scenarioId === 'automation'" class="outcome-preview__automation">
      <li>
        <FolderInput :size="26" aria-hidden="true" /><strong>原始文件</strong
        ><span>放入待处理资料</span>
      </li>
      <li class="outcome-preview__rule">
        <ArrowRight :size="20" aria-hidden="true" /><strong>按规则处理</strong
        ><span>重复流程自动执行</span>
      </li>
      <li>
        <FolderOutput :size="26" aria-hidden="true" /><strong>整理结果</strong
        ><span>集中检查输出</span>
      </li>
    </ol>
    <div v-else class="outcome-preview__support">
      <div><strong>排查与处理指引</strong><span>可以照着操作的步骤</span></div>
      <ol>
        <li><span>1</span>确认环境与报错信息</li>
        <li><span>2</span>按步骤修复与配置</li>
        <li><span>3</span>验证运行结果</li>
      </ol>
    </div>
  </figure>
</template>

<style scoped>
.outcome-preview {
  margin: 18px 0 0;
  padding: 15px 16px;
  border: 1px solid #d6e1ed;
  border-radius: 9px;
  background: #fff;
  color: #15314e;
  box-shadow: 0 3px 0 #e1eaf4;
}
.outcome-preview figcaption {
  font-size: 10px;
  color: #53677e;
  margin-bottom: 12px;
}
.outcome-preview__website-title {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #245ed0;
}
.outcome-preview__website-title strong {
  font-size: 13px;
  font-weight: 600;
  color: #15314e;
}
.outcome-preview__website-title > span {
  margin-left: auto;
  font-size: 10px;
  color: #53677e;
}
.outcome-preview dl {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 13px 0;
}
.outcome-preview dl > div {
  padding: 9px 11px;
  background: #f2f6fb;
  border-radius: 4px;
}
.outcome-preview dt {
  font-size: 10px;
  color: #53677e;
}
.outcome-preview dd {
  font-size: 11px;
  line-height: 1.7;
  margin: 4px 0 0;
}
.outcome-preview__foot {
  display: flex;
  gap: 6px;
  align-items: center;
  color: #167053;
  font-size: 10px;
}
.outcome-preview table {
  border-collapse: collapse;
  width: 100%;
  text-align: left;
  font-size: 11px;
  line-height: 1.6;
}
.outcome-preview th {
  font-weight: 500;
}
.outcome-preview thead {
  background: #f0f5fc;
  color: #53677e;
  font-size: 10px;
}
.outcome-preview th,
.outcome-preview td {
  padding: 9px 7px;
  border-bottom: 1px solid #e3eaf1;
}
.outcome-preview tr:last-child > th,
.outcome-preview tr:last-child > td {
  border-bottom: 0;
}
.outcome-preview td:last-child {
  color: #167053;
}
.outcome-preview td svg {
  display: inline;
  vertical-align: middle;
  margin-right: 3px;
}
.outcome-preview__automation {
  list-style: none;
  margin: 0;
  padding: 5px 0;
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  gap: 8px;
}
.outcome-preview__automation li {
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 9px;
  text-align: center;
}
.outcome-preview__automation svg {
  color: #245ed0;
  height: 26px;
}
.outcome-preview__automation strong {
  font-size: 12px;
  font-weight: 550;
}
.outcome-preview__automation li > span {
  font-size: 10px;
  color: #53677e;
  line-height: 1.6;
}
.outcome-preview__support {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  align-items: center;
}
.outcome-preview__support > div {
  display: grid;
  gap: 8px;
}
.outcome-preview__support strong {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.7;
}
.outcome-preview__support > div > span {
  font-size: 10px;
  color: #53677e;
  line-height: 1.7;
}
.outcome-preview__support ol {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 9px;
}
.outcome-preview__support li {
  display: flex;
  gap: 7px;
  align-items: center;
  font-size: 10px;
  line-height: 1.7;
}
.outcome-preview__support li > span {
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  background: #eaf1ff;
  color: #245ed0;
  font-size: 9px;
}
@media (max-width: 400px) {
  .outcome-preview {
    padding: 13px 11px;
  }
  .outcome-preview dl {
    gap: 7px;
  }
  .outcome-preview dl > div {
    padding: 8px;
  }
  .outcome-preview th,
  .outcome-preview td {
    padding: 8px 4px;
    font-size: 10px;
  }
  .outcome-preview__support {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>

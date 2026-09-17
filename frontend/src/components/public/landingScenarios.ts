export type LandingScenarioId = 'website' | 'automation' | 'data' | 'support'

export interface LandingScenario {
  id: LandingScenarioId
  label: string
  service: string
  title: string
  question: string
  description: string
  attachment: string
  processing: string
  outcome: string
  deliverables: readonly string[]
}

// Public examples are local presentation data, never records from the request API.
export const landingScenarios: readonly LandingScenario[] = [
  {
    id: 'website',
    label: '报名页面',
    service: '网站与系统建设',
    title: '活动报名信息收集页面',
    question: '活动快开始了，还缺一个报名页面。',
    description: '希望参与者可以在线报名，组织者能集中查看和整理报名信息。',
    attachment: '活动流程与报名字段.pdf',
    processing: '正在搭建可预览版本',
    outcome: '一份可以确认的报名页面',
    deliverables: ['报名页面预览', '信息汇总说明', '使用与维护指引'],
  },
  {
    id: 'automation',
    label: '批量处理',
    service: '程序问题与自动化',
    title: '重复文件整理与批量处理',
    question: '同样的操作，每次都要重复做。',
    description: '希望按照统一规则整理文件，把重复的手工步骤变成可以复用的流程。',
    attachment: '文件样例与处理规则.zip',
    processing: '正在验证处理规则与脚本',
    outcome: '一套可以重复使用的处理流程',
    deliverables: ['处理脚本与示例', '运行步骤说明', '异常情况处理指引'],
  },
  {
    id: 'data',
    label: '数据整理',
    service: '数据整理与分析',
    title: '多份表格的数据整理与汇总',
    question: '表格有很多，信息却总对不上。',
    description: '希望统一不同表格的格式，整理重复信息，并得到便于查看的汇总结果。',
    attachment: '原始表格与字段说明.xlsx',
    processing: '正在清洗数据与核对汇总',
    outcome: '一份清楚、可核对的数据结果',
    deliverables: ['整理后的数据表', '字段与处理说明', '统计结果预览'],
  },
  {
    id: 'support',
    label: '技术排障',
    service: '设备与技术支持',
    title: '软件环境与运行故障排查',
    question: '程序运行不起来，不知道从哪查。',
    description: '希望定位设备、网络或软件环境中的问题，并拿到可以执行的处理步骤。',
    attachment: '报错截图与环境说明.pdf',
    processing: '正在复现问题与验证方案',
    outcome: '一份可以照着操作的解决方案',
    deliverables: ['问题定位结论', '环境配置步骤', '验证与后续维护建议'],
  },
]

export const landingSteps = [
  {
    title: '描述问题',
    status: '整理需求',
    short: '描述',
    description: '说清背景、使用人和期待结果，不必先写技术方案。',
  },
  {
    title: '补充资料',
    status: '补充资料',
    short: '资料',
    description: '参考文件、期望时间和补充说明，始终跟随同一个需求。',
  },
  {
    title: '团队评估',
    status: '评估示例',
    short: '评估',
    description: '技术组给出专业可行性评估，说明承接结论和下一步安排。',
  },
  {
    title: '查看进度',
    status: '处理中',
    short: '处理',
    description: '负责人、最新进展与关键结论集中呈现，不用反复追问。',
  },
  {
    title: '确认成果',
    status: '待验收',
    short: '验收',
    description: '查看交付内容，确认成果，或提出具体调整意见。',
  },
] as const

export type LandingStep = 0 | 1 | 2 | 3 | 4

export function getLandingScenario(id: LandingScenarioId): LandingScenario {
  return landingScenarios.find((scenario) => scenario.id === id) ?? landingScenarios[0]!
}

# GHCR 拉取式 CD：第二阶段（服务器执行端）

## 当前交付边界

本分支提供拉取程序、摘要部署入口、测试和 systemd 模板。
**代码在仓库里不等于服务器已安装，更不等于自动部署已启用。**
默认命令和默认 service 都只执行 dry-run；本说明中的生产操作须在 PR/CI 通过、
管理员确认安装路径和维护窗口后，逐步执行。不要把整篇文档当成一个脚本运行。

已完成的现场前置检查：部署用户可从 GHCR 读取发布记录，以及记录指定的两个应用清单。
这不替代镜像层拉取、平台检查、数据库备份、健康检查、外网业务验收。

## 文件与权限

| 文件 | 安装位置 / 权限 |
| --- | --- |
| deploy.sh、pull-agent.py | /home/Ted_Kasane/tech-request-prod-deploy/；Ted_Kasane 所有，750 |
| 原 docker-compose.yml | 保持原路径和640权限；本分支没有修改 Compose |
| 原 .env.prod、release.env | 保持原路径和600权限；不上传、不打印 .env.prod |
| pull-agent-state.json | 程序首次初始化生成，Ted_Kasane所有，600；不可删除来绕过保护 |
| pull-agent.lock、deploy.lock | Ted_Kasane所有，600；不删除正在使用的锁文件 |
| pull-agent.enabled | 真实部署的独立开关，600；内容必须严格是 enabled 加一个 LF 换行 |
| tech-request-cd.service、tech-request-cd.timer | /etc/systemd/system/；root所有，644；管理员安装 |
| history/pull-deploy-*.log、pull-state-before-resume-*.json | 部署目录内审计文件，600 |
| MySQL、上传、业务日志、数据库备份 | 继续使用 /data/volumes/tech-request-prod 下原有目录 |

运行要求：Linux、Python 3.9+、本机 Docker socket、linux/amd64、Docker Compose、
timeout、flock、gzip、sha256sum。程序必须作为 Ted_Kasane 运行，不能以 root 运行。
wid7 仅通过已获准的 sudo 完成安装/管理，不改跳板机账户和防火墙。

程序用 Docker 用户现有的 GHCR 读取凭据；不要给服务器 Actions 写令牌。
显式指定本机 Docker socket，避免误用用户配置中的远程 Docker context。
Docker socket 本身具有很高权限；systemd 的沙箱选项不是针对恶意代码的安全隔离。
只运行经过审核的本地程序，发布记录只是 JSON 数据，不会被 source/eval/执行。

## 运行模式

- 不带参数或 --dry-run：读取 production 指针，解析单平台记录摘要，按摘要下载小记录镜像，
  校验记录、当前容器及候选平台。可能写 Docker 缓存、创建锁文件；不是零写入。
  不拉取应用镜像层，不备份数据库，不修改 release.env，不更新容器，不初始化状态。
- --initialize sha256:...：以人工核对的记录摘要建立最低版本序号和当前生产环境基线。
  写一次状态文件，不部署、不启用定时任务。已有状态时拒绝覆盖。
- --status：只显示状态记录；可能创建/使用锁文件，不访问 GHCR。
- --apply：必须已有状态且存在合法 enabled 文件；否则停止。
  成功版本重复出现时跳过；新版本调用本地 deploy.sh 的摘要接口。
- --resume：只用于故障检查后的人工恢复。要求 release.env、应用镜像内容及健康状态
  已恢复到故障前基线，MySQL容器和镜像身份未变；允许回退过程重建前后端容器。
  先归档旧状态，再解除暂停，但故障提交加入阻止列表，不会再次自动部署该提交。

真实部署调用形状（由程序生成，不要从截图手抄摘要）：

~~~text
deploy.sh --digests COMMIT BACKEND_AT_DIGEST FRONTEND_AT_DIGEST CURRENT_RELEASE_ENV_SHA256
~~~

旧的 deploy.sh COMMIT 接口仍可用。人工部署/回退前先停 timer；
人工改版之后，拉取端会检测到基线变化并停止，需要维护人员审核后重新协调状态。

## 安全流程

1. 用独立 pull-agent.lock 阻止两次拉取任务重叠。
2. 校验已部署 release.env 与运行容器的引用、镜像ID、容器ID、健康状态。
3. 查询 GHCR production 指针，再按解析出的固定摘要获取记录，不按标签直接部署应用。
4. 校验项目名、结构、UTC时间、序号、提交号、成对摘要和目标平台。
5. 拒绝低于人工基线或已部署序号的版本；同序号不同内容停止；
   同一成功记录仅元数据重新发布时也不重复部署。
6. 持久化 in_progress，再调用 deploy.sh。脚本在共用 deploy.lock 内重新核对
   release.env 的文件 SHA256，防止检查后发生的人工版本切换被覆盖。
7. 检查至少5GiB空间；拉取应用后再次检查备份空间；备份、校验gzip并记录旧版本；
   仅更新 backend/frontend，保留 MySQL。健康失败时尝试应用回退。
8. 检查实际镜像引用和 MySQL身份后才记为成功。失败/中断留下标记，后续自动运行停止。

状态不是数据库事务：如果进程在容器更新后、写入成功状态前中断，
必须人工对照日志和实际版本处理，不能假设已回退，也不能删除状态重试。
数据库迁移不自动回退；应用回退之前必须确认新旧版本数据库兼容。

## 分阶段安装与验收

### A. 先完成本地审核和 CI

在 Windows 的本地工作区提交、推送并通过 PR 审核。
Configuration / validate 必须通过新增的 Linux 假 Docker 测试与 systemd 单元检查。
Windows 本地测试会跳过 Linux锁/权限及 Bash集成测试，不把跳过当通过。
只有 CI 和发布任务都通过后，才准备服务器安装包。

安装只取同一个已审核提交中的4个文件：
deploy/server/deploy.sh、deploy/server/pull-agent.py、
deploy/server/systemd/tech-request-cd.service、deploy/server/systemd/tech-request-cd.timer。
通过既有获准通道传输，记录文件SHA256，服务器端逐个核对。
先检查目标、备份旧脚本/同名单元文件，再从同目录临时文件原子替换。
保留所有 .env.prod、release.env、状态文件与业务数据，不覆盖整个部署目录。
不在服务器脏源码工作区执行 git pull、checkout、reset 或 clean。
本项目不通过 GHCR 下载并执行新的部署脚本；控制代码更新始终单独审核安装。

### B. 只运行一次 dry-run

安装脚本后，仍不要创建 enabled 文件或启动 timer。
作为 wid7 在服务器执行：

~~~bash
sudo -H -u Ted_Kasane python3 -B /home/Ted_Kasane/tech-request-prod-deploy/pull-agent.py --dry-run
~~~

检查输出的 sequence、commit、metadata 与最新批准记录对应，两个应用平台都通过。
首次应报告 uninitialized。这是预检，不是部署完成。
如网络、权限、磁盘、健康检查失败，保持当前服务，先解决具体问题。

### C. 建立人工批准基线，再通过 systemd 试运行

从已验证的 GitHub 发布摘要取完整记录 digest，用 --initialize 后接该值。
不要输入标签 production，也不要从未经验证的来源复制摘要。
已有状态的机器不执行初始化；先 --status 并检查现有管理关系。

管理员安装原始 service/timer，执行 systemd-analyze verify，再 daemon-reload。
暂不 enable timer；单独启动一次 service。原始 service 使用 --dry-run。
读取 journal，确认服务沙箱内也能访问 GHCR、Docker凭据、部署目录和备份路径。

### D. 显式批准一次真实部署

这一阶段会备份数据库并更新应用，必须另行确认维护窗口、磁盘空间、
数据库兼容性、回退版本、可恢复备份以及外网验收方式。

管理员以 Ted_Kasane 所有、600权限创建 pull-agent.enabled，内容严格为 enabled 加 LF换行。
用 systemctl edit tech-request-cd.service 建立仅针对该服务的 drop-in：

~~~ini
[Service]
ExecStart=
ExecStart=/usr/bin/python3 -B /home/Ted_Kasane/tech-request-prod-deploy/pull-agent.py --apply
~~~

确认 timer 仍未启动，然后 daemon-reload，再单独启动一次 service。
检查 journal、history/pull-deploy-*.log、备份及 --status：
last应记录本次序号，status应为ready，attempt应为空；三个容器healthy，MySQL ID未变。
再从外网检查网站、登录和关键业务流程；不要只以容器healthy代替业务验收。
只有验收通过，才批准周期运行。

### E. 最后开启周期运行

在D阶段已经通过、告警与保留策略已经确认后，由管理员启用 tech-request-cd.timer。
模板在开机后约2分钟检查，之后在上次运行结束约5分钟后再次检查，附加最多30秒随机延迟。
它不是秒级实时发布。不要设置 RemainAfterExit=yes，否则周期行为会受到影响。
不同时设置 cron、另一个 timer 或后台循环执行同一脚本。

本实现输出 journal 错误和失败状态，**尚未接入邮件/企业微信等外部通知**。
正式启用前需由团队确定失败告警的接收人、通知渠道和 journal/备份保留策略；
不要把无人查看的本地日志称为已完成故障告警。

## 暂停、故障与回退

停止后续调度（不会主动停止已经执行中的 service）：

~~~bash
sudo systemctl disable --now tech-request-cd.timer
sudo systemctl status tech-request-cd.service --no-pager
sudo journalctl -u tech-request-cd.service -n 100 --no-pager
~~~

正常维护先停timer并等当前执行结束，再操作版本。不要随意中断备份/切换过程。
如必须紧急停止正在运行的 service，systemd 会终止其进程组，但 Docker
已经完成的更新不会被撤销；state可能保留in_progress，必须人工检查现场。
不要执行 compose down、down -v、docker system prune，也不要清空状态/锁/备份来解卡。

失败时先看日志、状态、release.env和容器，不打印 .env.prod。
如果应用回退已完成、MySQL未变化、健康正常，人工审查后可 --resume；
它会归档状态并阻止故障提交。发布一个更高序号的新修复提交后才能继续自动部署。
若现场与旧基线不一致，--resume会拒绝；应按经批准的回退/状态协调步骤处理。
停止定时器不等于停网站；删除 enabled 文件也不能撤销已经开始的部署。

旧版本备份与控制脚本备份应按团队保留策略管理；本改动不自动删除镜像、数据或备份。
存储不足时停止自动更新，请管理员精确评估清理对象，不做全局 prune。

## 官方接口依据

- [Docker manifest](https://docs.docker.com/reference/cli/docker/manifest/)：
  从 registry 查询清单，兼容旧 Platform/Digest 与新 Descriptor 输出结构。
- [systemd timer 官方源文档](https://github.com/systemd/systemd/blob/main/man/systemd.timer.xml)：
  OnUnitInactiveSec 从上次服务停止计时；正在运行的服务不会因计时器重复启动。
- [systemd service 官方源文档](https://github.com/systemd/systemd/blob/main/man/systemd.service.xml)：
  oneshot 明确设置 TimeoutStartSec，避免依赖默认无限启动等待。

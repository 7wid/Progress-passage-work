# GHCR 拉取式 CD：第一阶段（发布端）

## 当前边界

本改动只准备 GitHub Actions 发布版本记录，**不安装服务器定时任务，不连接 SSH，不更新任何生产容器**。
合并本改动后仍不能称为“合并即自动上线”；第二阶段的服务器消费者尚未实现。

生产服务器已验证可以读取 GHCR 镜像清单，但连接 GitHub API 超时。
因此采用“GitHub 发布记录 → 服务器主动读取 GHCR → 验证后部署”的方式。
GitHub API 查询仅在 GitHub 托管 runner 上执行，不在生产服务器执行。

## 发布条件与数据格式

仅 main 分支的 push CI 成功、前后端两个镜像都推送成功，才允许更新发布记录。
手动运行 Publish container images 只构建应用镜像，不更新 production 记录。
PR、失败的 CI、失败的任一镜像构建都不触发发布记录。

应用镜像保留 sha-提交号 标签，同时增加 build-工作流ID-尝试次数 标签。
记录从同一次发布运行的两个 build 标签读取顶层 SHA256 digest，
最终记录的是 ghcr.io/7wid/progress-passage-work-backend@sha256:... 和对应 frontend 引用，
不依赖可能移动的应用标签。摘要是内容标识，不等于数字签名。

发布记录使用新的 GHCR 包 ghcr.io/7wid/progress-passage-work-release：

- production：供后续服务器发现新版本的可变指针。
- run-工作流ID-尝试次数：供审核追溯的标签；标签本身不保证不可变。
- 记录镜像的 digest：应在服务器审计日志中保存的准确内容标识。

镜像基于 scratch，只包含 release.json 及标签
xyz.gxutech.progress-passage-work.release.v1，没有 CMD、ENTRYPOINT 或 RUN。
后续消费者只拉取并 inspect 镜像，不启动此镜像的容器。

JSON 包含 schema_version、repository、commit、sequence（CI run_number）、
ci_run_id、publish_run_id、publish_run_attempt、created_at（UTC）、images。
生成器拒绝非法提交号、缺失摘要、错误清单格式及非法运行编号。

发布记录任务串行执行，并在锁内核对该提交仍是 main 当前提交。
较旧构建晚完成时会跳过记录发布；保留已有生产指针。
若 main 已推进而新构建失败，指针保持之前成功发布的记录，不会选择未经检查的 main。
发现较旧候选被跳过时，应等最新 main 的 CI 和发布流程完成。

## 本地和 CI 验证

在仓库根目录执行：

~~~text
python -B -m unittest discover -s deploy/ci/tests -v
git diff --check
~~~

本地没有 Docker 时，Docker 集成用例默认跳过；这不代表已经完成真实镜像验收。
CI 的 Configuration / validate 设置 TEST_RELEASE_DOCKER=1，
会实际构建 scratch 记录镜像，并 inspect 校验标签 JSON，整个测试不运行容器。

如发布工作流失败，排除原因后选择 **Re-run all jobs**，不要只重跑失败任务。
这是因为记录要求同一 run_attempt 的前后端 build 标签成对存在；
只重跑部分矩阵任务可能缺少另一组件的对应标签，届时安全失败，不拼接不同次构建。

## 第一阶段合并后的人工验收（尚未执行）

1. 通过 PR 审核并合并，确认 main 的 CI 全部成功。
2. 确认 Publish container images 的两项构建和 Publish production release record 都成功。
3. 查看任务摘要中的提交号、记录镜像完整 digest，确认对应此次合并。
4. 检查新 release 包的可见性和权限：新包不保证自动公开。
   公开记录需获得项目许可；否则为部署用户配置仅需的 GHCR 读取凭据。
   不把 PAT、SSH 私钥、生产 env 或 Docker 凭据提交到代码仓库。
5. 服务器先只读检查能否获取该记录及其引用的两个应用 digest，核对实际平台。
   检查成功不等于部署完成，此时不启用轮询或更新容器。

## 第二阶段必须补齐的工程约束

- 部署用户 Ted_Kasane 执行；wid7 仅通过已有 sudo 权限安装和管理。
  不改跳板机权限，不开放新入站端口，不给予 runner 生产服务器任意命令权限。
- 只部署固定仓库、完整摘要引用，校验记录结构、提交、平台及版本顺序；
  不能 source、eval 或执行从镜像获取的记录内容，也不下载执行部署脚本。
- 扩展现有 deploy.sh 的接口，使其支持经验证的成对 digest 引用。
  不能直接把本记录传给当前仅接受 SHA 的脚本并宣称已经按摘要部署。
- 与手动部署共用锁；同一记录幂等；失败版本暂停重试并报警；
  防止旧记录重放导致自动回退，人工回退需显式审批。
- 保留现有数据库备份、磁盘空间检查、健康检查、应用回退、审计日志。
  不重建 MySQL，不动服务器脏源码工作区；业务数据仍放 /data/volumes。
- 经管理员确认后再安装 systemd service/timer，先 dry-run，再受控部署一次，
  最后开启定时任务，并给出停用、回滚、备份保留和故障处理操作。
- 审核仓库分支保护及工作流/包写权限。此方案信任受控发布者和 TLS，
  尚未实现独立签名校验；若工程要求签名，必须另行补齐。
- GHCR/API 暂时不可用时保留当前服务；不得关闭 TLS 校验。
- 新通路验收后，单独评估撤销不再使用的自动化 SSH 公钥，
  不删除人工登录凭据或更改共享跳板账户。

## 官方接口依据

按照 [Docker Buildx imagetools inspect 文档](https://docs.docker.com/reference/cli/docker/buildx/imagetools/inspect/)
读取格式化 .Manifest 的顶层 digest；不能用原始清单的子项摘要替代根摘要。

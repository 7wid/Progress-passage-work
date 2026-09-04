# CD 邮件告警与本地保留策略

本文说明 `tech-request-cd` 自动轮询启用前必须完成的邮件告警和本地容量保护。
代码合并不等于服务器已经安装；所有生产操作仍须经过 PR、CI、维护窗口和管理员审批。

## 已批准策略

- SMTP：QQ 邮箱 `smtp.qq.com:465`，TLS证书必须验证，使用独立授权码而不是登录密码。
- 收件人：由 `/etc/tech-request-cd/alert-recipients` 配置，不提交仓库。
- 重复告警：相同故障一小时内最多发送一次；不同 sequence/commit 立即发送。
- 数据库备份：本机保留30天，并始终保护最近14份。
- 部署审计：匹配已知安全文件名的记录保留180天。
- journal：保留30天，最大512MiB，并为文件系统至少保留2GiB。
- 异机备份：不属于本实现；数据库和附件仍须生成时间对应的异机副本并定期恢复演练。

## 安全边界

`notify-failure.py` 只发送主机、UTC时间、失败来源、状态、sequence、commit、
systemd结果和退出码，不发送 journal 正文、环境变量或生产配置。
credential 只由 systemd `LoadCredential=` 提供。

`retention.py` 默认是 dry-run。真实清理还要求 `retention.enabled` 为
Ted_Kasane所有、600权限，且内容严格为 `enabled` 加一个换行。程序与部署脚本共用
`deploy.lock`，只删除精确匹配的普通单链接文件；删除数据库旧备份前完整校验受保护的
14份 gzip。未知文件、符号链接、硬链接、状态文件、锁文件和 `.partial` 文件均不删除。

## 经审核安装的文件

从同一个已通过 PR/CI 的提交取下列文件，生成安装清单并核对 SHA256：

~~~text
deploy/server/notify-failure.py
deploy/server/retention.py
deploy/server/systemd/tech-request-cd.service
deploy/server/systemd/tech-request-cd-alert@.service
deploy/server/systemd/tech-request-cd-alert-test.service
deploy/server/systemd/tech-request-retention.service
deploy/server/systemd/tech-request-retention.timer
deploy/server/systemd/journald/60-tech-request-retention.conf
~~~

脚本安装到 `/home/Ted_Kasane/tech-request-prod-deploy/`，所有者
Ted_Kasane:Ted_Kasane、权限750。unit 安装到 `/etc/systemd/system/`，
所有者root:root、权限644。journald drop-in影响整台服务器，必须单独记录管理员审批。
安装时备份同名旧文件，通过目标目录内临时文件原子替换，不覆盖 `.env.prod`、
`release.env`、状态、锁和业务数据。

## 安装 SMTP credential

在生产服务器 `wid7@debian` 上进入 `/tmp` 后执行。输入过程不会回显授权码：

~~~bash
cd /tmp
umask 077
read -r -p 'SMTP发件邮箱: ' cd_smtp_username
read -r -s -p 'SMTP授权码（不回显）: ' cd_smtp_password
printf '\n'
read -r -p '告警收件邮箱（多个用逗号分隔）: ' cd_alert_recipients

sudo install -d -o root -g root -m 700 /etc/tech-request-cd
for cd_credential in smtp-username smtp-password alert-recipients; do
  sudo install -o root -g root -m 600 /dev/null \
    "/etc/tech-request-cd/$cd_credential"
done
printf '%s\n' "$cd_smtp_username" \
  | sudo tee /etc/tech-request-cd/smtp-username >/dev/null
printf '%s\n' "$cd_smtp_password" \
  | sudo tee /etc/tech-request-cd/smtp-password >/dev/null
printf '%s\n' "$cd_alert_recipients" \
  | sudo tee /etc/tech-request-cd/alert-recipients >/dev/null
unset cd_smtp_username cd_smtp_password cd_alert_recipients

sudo chown root:root \
  /etc/tech-request-cd/smtp-username \
  /etc/tech-request-cd/smtp-password \
  /etc/tech-request-cd/alert-recipients
sudo chmod 600 \
  /etc/tech-request-cd/smtp-username \
  /etc/tech-request-cd/smtp-password \
  /etc/tech-request-cd/alert-recipients
sudo stat -c '权限=%a 所有者=%U:%G 路径=%n' \
  /etc/tech-request-cd/smtp-username \
  /etc/tech-request-cd/smtp-password \
  /etc/tech-request-cd/alert-recipients
~~~

不得打印或传输 credential 内容。

## 分阶段验证

保持 `tech-request-cd.timer` 和 `tech-request-retention.timer` 关闭，先验证单元：

~~~bash
sudo systemd-analyze verify \
  /etc/systemd/system/tech-request-cd.service \
  /etc/systemd/system/tech-request-cd.timer \
  /etc/systemd/system/tech-request-cd-alert@.service \
  /etc/systemd/system/tech-request-cd-alert-test.service \
  /etc/systemd/system/tech-request-retention.service \
  /etc/systemd/system/tech-request-retention.timer
sudo systemctl daemon-reload
~~~

发送测试邮件，并由收件人确认送达；测试失败时不得启用任何 timer：

~~~bash
sudo systemctl reset-failed tech-request-cd-alert-test.service
sudo systemctl start tech-request-cd-alert-test.service
sudo systemctl status tech-request-cd-alert-test.service --no-pager
sudo journalctl -u tech-request-cd-alert-test.service -n 30 --no-pager
~~~

执行保留 dry-run。首次只有4份数据库备份时，数据库删除候选必须为0：

~~~bash
cd /tmp
sudo -H -u Ted_Kasane python3 -B \
  /home/Ted_Kasane/tech-request-prod-deploy/retention.py
~~~

审核输出后创建独立开关；创建开关本身不执行删除：

~~~bash
cd /tmp
sudo -H -u Ted_Kasane sh -c \
  'umask 077; printf "enabled\n" > /home/Ted_Kasane/tech-request-prod-deploy/retention.enabled'
sudo -H -u Ted_Kasane stat \
  -c '权限=%a 所有者=%U:%G 路径=%n' \
  /home/Ted_Kasane/tech-request-prod-deploy/retention.enabled
~~~

## 应用 journal 策略

先安装 `/etc/systemd/journald.conf.d/60-tech-request-retention.conf` 并记录旧占用。
首次 vacuum 会删除超出已批准策略的归档 journal，须写入维护记录：

~~~bash
sudo journalctl --disk-usage
sudo systemctl restart systemd-journald
sudo journalctl --rotate --vacuum-time=30d --vacuum-size=512M
sudo journalctl --disk-usage
~~~

## 最终启用顺序

先单次执行保留服务，确认输出和邮件告警均正常：

~~~bash
sudo systemctl start tech-request-retention.service
sudo systemctl status tech-request-retention.service --no-pager
sudo journalctl -u tech-request-retention.service -n 100 --no-pager
~~~

只有测试邮件已送达、保留计划符合预期、数据库恢复演练和生产验收均通过后执行：

~~~bash
sudo systemctl enable --now tech-request-retention.timer
sudo systemctl enable --now tech-request-cd.timer
systemctl is-enabled tech-request-retention.timer tech-request-cd.timer
systemctl is-active tech-request-retention.timer tech-request-cd.timer
systemctl list-timers tech-request-retention.timer tech-request-cd.timer --no-pager
~~~

CD timer 启用后，合并到 main 且发布记录成功即进入生产自动部署链路；
未完成、未验收或仅供测试的代码不得合入 main。

## 故障处理

邮件告警到达后先停后续调度并保留现场：

~~~bash
sudo systemctl disable --now tech-request-cd.timer
sudo systemctl status tech-request-cd.service --no-pager
sudo journalctl -u tech-request-cd.service -n 200 --no-pager
sudo -H -u Ted_Kasane python3 -B \
  /home/Ted_Kasane/tech-request-prod-deploy/pull-agent.py --status
~~~

不要删除状态/锁文件，不执行 `docker compose down -v` 或全局 prune，
也不要在未检查数据库兼容性和发布后业务写入前恢复旧数据库。

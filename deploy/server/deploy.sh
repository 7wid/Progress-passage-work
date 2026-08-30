#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'
umask 077

readonly PROJECT_NAME="tech-request-prod"
readonly DEPLOY_DIR="/data/services/tech-request-prod"
readonly COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
readonly SECRETS_FILE="${DEPLOY_DIR}/.env.prod"
readonly RELEASE_FILE="${DEPLOY_DIR}/release.env"
readonly HISTORY_DIR="${DEPLOY_DIR}/history"
readonly BACKUP_DIR="/data/volumes/tech-request-prod/backups"
readonly LOCK_FILE="${DEPLOY_DIR}/deploy.lock"
readonly MIN_FREE_BYTES="5368709120"
readonly GHCR_PREFIX="ghcr.io/7wid"

usage() {
  echo "用法: $0 <main 分支的 40 位 Git 提交 SHA>" >&2
}

fail() {
  echo "部署终止: $*" >&2
  exit 1
}

compose() {
  local release_file="$1"
  shift
  docker compose \
    --project-name "${PROJECT_NAME}" \
    --env-file "${SECRETS_FILE}" \
    --env-file "${release_file}" \
    --file "${COMPOSE_FILE}" \
    "$@"
}

[[ $# -eq 1 ]] || { usage; exit 64; }

commit_sha="${1#sha-}"
[[ "${commit_sha}" =~ ^[0-9a-f]{40}$ ]] || fail "提交 SHA 必须是 40 位小写十六进制字符串。"

command -v docker >/dev/null 2>&1 || fail "找不到 docker。"
command -v flock >/dev/null 2>&1 || fail "找不到 flock（Debian 通常由 util-linux 提供）。"
command -v gzip >/dev/null 2>&1 || fail "找不到 gzip。"

[[ -f "${COMPOSE_FILE}" ]] || fail "缺少 ${COMPOSE_FILE}。"
[[ -f "${SECRETS_FILE}" ]] || fail "缺少 ${SECRETS_FILE}。"
[[ -f "${RELEASE_FILE}" ]] || fail "缺少 ${RELEASE_FILE}，首次切换前必须先记录当前镜像。"
[[ -d "${HISTORY_DIR}" && -w "${HISTORY_DIR}" ]] || fail "${HISTORY_DIR} 不存在或不可写。"
[[ -d "${BACKUP_DIR}" && -w "${BACKUP_DIR}" ]] || fail "${BACKUP_DIR} 不存在或不可写。"

secrets_mode="$(stat -c '%a' "${SECRETS_FILE}")"
[[ "${secrets_mode}" == "600" ]] || fail "${SECRETS_FILE} 权限必须为 600，当前为 ${secrets_mode}。"

available_bytes="$(df --output=avail -B1 "${BACKUP_DIR}" | tail -n 1 | tr -d ' ')"
[[ "${available_bytes}" =~ ^[0-9]+$ ]] || fail "无法读取备份目录剩余空间。"
(( available_bytes >= MIN_FREE_BYTES )) || fail "备份磁盘剩余空间不足 5 GiB，不执行部署。"

exec 9>"${LOCK_FILE}"
flock -n 9 || fail "已有另一个部署任务正在运行。"

candidate_file="$(mktemp "${DEPLOY_DIR}/.release.candidate.XXXXXX")"
database_tmp=""
rollback_tmp=""
cleanup() {
  [[ -z "${candidate_file}" || ! -e "${candidate_file}" ]] || rm -f -- "${candidate_file}"
  [[ -z "${database_tmp}" || ! -e "${database_tmp}" ]] || rm -f -- "${database_tmp}"
  [[ -z "${rollback_tmp}" || ! -e "${rollback_tmp}" ]] || rm -f -- "${rollback_tmp}"
}
trap cleanup EXIT

printf 'BACKEND_IMAGE=%s/progress-passage-work-backend:sha-%s\n' "${GHCR_PREFIX}" "${commit_sha}" >"${candidate_file}"
printf 'FRONTEND_IMAGE=%s/progress-passage-work-frontend:sha-%s\n' "${GHCR_PREFIX}" "${commit_sha}" >>"${candidate_file}"
chmod 600 "${candidate_file}"

echo "[1/6] 校验部署配置（不会显示环境变量值）"
compose "${candidate_file}" config --quiet

echo "[2/6] 拉取候选镜像（不会启动或重建容器）"
compose "${candidate_file}" pull backend frontend

timestamp="$(date -u '+%Y%m%dT%H%M%SZ')"
history_file="${HISTORY_DIR}/release-${timestamp}.env"
database_file="${BACKUP_DIR}/mysql-${timestamp}.sql.gz"
database_tmp="${database_file}.tmp"

echo "[3/6] 备份当前数据库"
compose "${RELEASE_FILE}" exec -T mysql \
  sh -eu -c 'exec env MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --host=127.0.0.1 --user=root --single-transaction --quick --routines --triggers --events --set-gtid-purged=OFF tech_request' \
  | gzip -1 >"${database_tmp}"
[[ -s "${database_tmp}" ]] || fail "数据库备份为空。"
chmod 600 "${database_tmp}"
mv -- "${database_tmp}" "${database_file}"
database_tmp=""

echo "[4/6] 保存上一版镜像清单并切换候选版本"
cp -- "${RELEASE_FILE}" "${history_file}"
chmod 600 "${history_file}"
mv -- "${candidate_file}" "${RELEASE_FILE}"
candidate_file=""

echo "[5/6] 仅更新后端和前端；不会重建 MySQL"
if ! compose "${RELEASE_FILE}" up --detach --no-deps --wait --wait-timeout 180 backend frontend; then
  echo "候选版本未通过健康检查，开始恢复上一版应用镜像。" >&2
  rollback_tmp="$(mktemp "${DEPLOY_DIR}/.release.rollback.XXXXXX")"
  cp -- "${history_file}" "${rollback_tmp}"
  chmod 600 "${rollback_tmp}"
  mv -- "${rollback_tmp}" "${RELEASE_FILE}"
  rollback_tmp=""
  compose "${RELEASE_FILE}" up --detach --no-deps --wait --wait-timeout 180 backend frontend \
    || fail "自动恢复也失败，请保持现场并联系维护人员；不要执行 compose down。"
  fail "候选版本部署失败，应用容器已恢复到上一版。数据库迁移不会自动回退。"
fi

echo "[6/6] 输出本项目应用容器状态"
compose "${RELEASE_FILE}" ps backend frontend
echo "部署完成：sha-${commit_sha}"
echo "数据库备份：${database_file}"

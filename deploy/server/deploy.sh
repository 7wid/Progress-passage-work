#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'
umask 077
unset DOCKER_CONTEXT
export DOCKER_HOST=unix:///var/run/docker.sock

readonly PROJECT_NAME="tech-request-prod"
readonly DEPLOY_DIR="/home/Ted_Kasane/tech-request-prod-deploy"
readonly COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
readonly SECRETS_FILE="${DEPLOY_DIR}/.env.prod"
readonly RELEASE_FILE="${DEPLOY_DIR}/release.env"
readonly HISTORY_DIR="${DEPLOY_DIR}/history"
readonly BACKUP_DIR="/data/volumes/tech-request-prod/backups"
readonly LOCK_FILE="${DEPLOY_DIR}/deploy.lock"
readonly IMAGE_STORE_PATH="/var/lib/containerd"
readonly MIN_BACKUP_FREE_BYTES="5368709120"
readonly MIN_IMAGE_STORE_FREE_BYTES="5368709120"
readonly GHCR_PREFIX="ghcr.io/7wid"

usage() {
  echo "用法: $0 <main 分支的 40 位 Git 提交 SHA>" >&2
  echo "摘要部署: $0 --digests <SHA> <backend@sha256:...> <frontend@sha256:...> <当前 release.env 的 SHA256>" >&2
}

fail() {
  echo "部署终止: $*" >&2
  exit 1
}

compose() {
  local release_file="$1"
  shift
  (
    cd -- "${DEPLOY_DIR}"
    timeout --kill-after=10s 600s docker compose \
      --project-directory "${DEPLOY_DIR}" \
      --project-name "${PROJECT_NAME}" \
      --env-file "${SECRETS_FILE}" \
      --env-file "${release_file}" \
      --file "${COMPOSE_FILE}" \
      "$@"
  )
}

expected_release_hash=""
if [[ $# -eq 1 ]]; then
  commit_sha="${1#sha-}"
  backend_image="$GHCR_PREFIX/progress-passage-work-backend:sha-$commit_sha"
  frontend_image="$GHCR_PREFIX/progress-passage-work-frontend:sha-$commit_sha"
elif [[ $# -eq 5 && "$1" == "--digests" ]]; then
  commit_sha="$2"
  backend_image="$3"
  frontend_image="$4"
  expected_release_hash="$5"
  [[ "$backend_image" =~ ^ghcr\.io/7wid/progress-passage-work-backend@sha256:[0-9a-f]{64}$ ]] || fail "后端摘要引用非法。"
  [[ "$frontend_image" =~ ^ghcr\.io/7wid/progress-passage-work-frontend@sha256:[0-9a-f]{64}$ ]] || fail "前端摘要引用非法。"
  [[ "$expected_release_hash" =~ ^[0-9a-f]{64}$ ]] || fail "release.env 校验值非法。"
else
  usage
  exit 64
fi
[[ "${commit_sha}" =~ ^[0-9a-f]{40}$ ]] || fail "提交 SHA 必须是 40 位小写十六进制字符串。"

command -v docker >/dev/null 2>&1 || fail "找不到 docker。"
command -v flock >/dev/null 2>&1 || fail "找不到 flock（Debian 通常由 util-linux 提供）。"
command -v gzip >/dev/null 2>&1 || fail "找不到 gzip。"
command -v timeout >/dev/null 2>&1 || fail "找不到 timeout。"
command -v sha256sum >/dev/null 2>&1 || fail "找不到 sha256sum。"

[[ -f "${COMPOSE_FILE}" ]] || fail "缺少 ${COMPOSE_FILE}。"
[[ -f "${SECRETS_FILE}" ]] || fail "缺少 ${SECRETS_FILE}。"
[[ -f "${RELEASE_FILE}" ]] || fail "缺少 ${RELEASE_FILE}，首次切换前必须先记录当前镜像。"
[[ -d "${HISTORY_DIR}" && -w "${HISTORY_DIR}" ]] || fail "${HISTORY_DIR} 不存在或不可写。"
[[ -d "${BACKUP_DIR}" && -w "${BACKUP_DIR}" ]] || fail "${BACKUP_DIR} 不存在或不可写。"
[[ -d "${IMAGE_STORE_PATH}" ]] || fail "缺少 Docker containerd 镜像存储目录 ${IMAGE_STORE_PATH}。"

secrets_mode="$(stat -c '%a' "${SECRETS_FILE}")"
[[ "${secrets_mode}" == "600" ]] || fail "${SECRETS_FILE} 权限必须为 600，当前为 ${secrets_mode}。"

backup_available_bytes="$(df --output=avail -B1 "${BACKUP_DIR}" | tail -n 1 | tr -d ' ')"
[[ "${backup_available_bytes}" =~ ^[0-9]+$ ]] || fail "无法读取备份目录剩余空间。"
(( backup_available_bytes >= MIN_BACKUP_FREE_BYTES )) \
  || fail "备份目录所在分区剩余空间不足 5 GiB，不执行部署。"

image_store_available_bytes="$(df --output=avail -B1 "${IMAGE_STORE_PATH}" | tail -n 1 | tr -d ' ')"
[[ "${image_store_available_bytes}" =~ ^[0-9]+$ ]] || fail "无法读取 Docker 镜像存储分区剩余空间。"
(( image_store_available_bytes >= MIN_IMAGE_STORE_FREE_BYTES )) \
  || fail "Docker containerd 镜像存储分区剩余空间不足 5 GiB，不拉取镜像。"

exec 9>"${LOCK_FILE}"
flock -n 9 || { echo "已有另一个部署任务正在运行。" >&2; exit 75; }

if [[ -n "$expected_release_hash" ]]; then
  current_release_hash="$(sha256sum "$RELEASE_FILE")"
  current_release_hash="${current_release_hash%% *}"
  [[ "$current_release_hash" == "$expected_release_hash" ]] \
    || fail "release.env 已变化；拒绝覆盖人工部署，请先检查。"
fi

mysql_before="$(timeout --kill-after=5s 20s docker inspect --format '{{.Id}}' "$PROJECT_NAME-mysql-1")"
[[ "$mysql_before" =~ ^[0-9a-f]{64}$ ]] || fail "无法确认当前 MySQL 容器 ID。"

candidate_file="$(mktemp "${DEPLOY_DIR}/.release.candidate.XXXXXX")"
database_tmp=""
rollback_tmp=""
cleanup() {
  [[ -z "${candidate_file}" || ! -e "${candidate_file}" ]] || rm -f -- "${candidate_file}"
  [[ -z "${database_tmp}" || ! -e "${database_tmp}" ]] || rm -f -- "${database_tmp}"
  [[ -z "${rollback_tmp}" || ! -e "${rollback_tmp}" ]] || rm -f -- "${rollback_tmp}"
}
trap cleanup EXIT

printf 'BACKEND_IMAGE=%s\n' "$backend_image" >"$candidate_file"
printf 'FRONTEND_IMAGE=%s\n' "$frontend_image" >>"$candidate_file"
chmod 600 "${candidate_file}"

echo "[1/6] 校验部署配置（不会显示环境变量值）"
compose "${candidate_file}" config --quiet

echo "[2/6] 拉取候选镜像（不会启动或重建容器）"
compose "${candidate_file}" pull backend frontend

# Pulling layers may consume the same filesystem as the backup directory.
backup_available_bytes="$(df --output=avail -B1 "$BACKUP_DIR" | tail -n 1 | tr -d ' ')"
[[ "$backup_available_bytes" =~ ^[0-9]+$ ]] || fail "无法重新读取备份目录空间。"
(( backup_available_bytes >= MIN_BACKUP_FREE_BYTES )) || fail "拉取后备份空间不足 5 GiB，未更新容器。"

timestamp="$(date -u '+%Y%m%dT%H%M%S.%NZ')"
history_file="${HISTORY_DIR}/release-${timestamp}.env"
database_file="${BACKUP_DIR}/mysql-${timestamp}.sql.gz"
database_tmp="${database_file}.tmp"

echo "[3/6] 备份当前数据库"
compose "${RELEASE_FILE}" exec -T mysql \
  sh -eu -c 'exec env MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --host=127.0.0.1 --user=root --single-transaction --quick --routines --triggers --events --set-gtid-purged=OFF tech_request' \
  | gzip -1 >"${database_tmp}"
[[ -s "${database_tmp}" ]] || fail "数据库备份为空。"
gzip -t "$database_tmp" || fail "数据库备份压缩校验失败。"
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
mysql_after="$(timeout --kill-after=5s 20s docker inspect --format '{{.Id}}' "$PROJECT_NAME-mysql-1")"
[[ "$mysql_after" == "$mysql_before" ]] || fail "MySQL 容器 ID 发生变化，请检查现场。"
for component in backend frontend; do
  expected_image="$backend_image"
  [[ "$component" != "frontend" ]] || expected_image="$frontend_image"
  actual_image="$(timeout --kill-after=5s 20s docker inspect --format '{{.Config.Image}}' "$PROJECT_NAME-$component-1")"
  [[ "$actual_image" == "$expected_image" ]] || fail "$component 实际镜像与候选摘要/标签不一致。"
done
compose "${RELEASE_FILE}" ps backend frontend
echo "部署完成：sha-${commit_sha}"
echo "数据库备份：${database_file}"

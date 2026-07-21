#!/usr/bin/env bash
# Linux 运维入口：启停 / 状态 / 打包 / 清日志
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS="$ROOT/scripts"
BUILD="$ROOT/build"
PATH_FILE="$SCRIPTS/web-path.txt"
DOMAIN="${SERVER_DOMAIN:-www.cloudhui.cc.cd}"

ORDER=(center gate lobby game web)

declare -A JAR_NAME=(
  [center]=Center.jar
  [gate]=Gate.jar
  [lobby]=Lobby.jar
  [game]=Game.jar
  [web]=Web.jar
)

declare -A HEAP=(
  [center]=64m
  [gate]=96m
  [lobby]=128m
  [game]=128m
  [web]=192m
)

usage() {
  cat <<EOF
用法: $0 {start|stop|restart|status|build|clean-logs} [服务|all]

服务: center | gate | lobby | game | web | all
默认服务: all（start/stop/restart/status）

示例:
  $0 start
  $0 restart web
  $0 build
  $0 clean-logs
EOF
}

need_java() {
  command -v java >/dev/null 2>&1 || { echo "未找到 java"; exit 1; }
}

web_path() {
  if [[ ! -f "$PATH_FILE" ]]; then
    local p
    p="$(tr -dc 'A-Za-z0-9' </dev/urandom | head -c 18)"
    echo "$p" >"$PATH_FILE"
  fi
  tr -d '[:space:]' <"$PATH_FILE"
}

resolve_targets() {
  local arg="${1:-all}"
  case "$arg" in
    all) echo "${ORDER[*]}" ;;
    center|gate|lobby|game|web) echo "$arg" ;;
    *) echo "未知服务: $arg" >&2; usage; exit 1 ;;
  esac
}

svc_dir() {
  echo "$BUILD/$1"
}

svc_jar() {
  echo "$(svc_dir "$1")/${JAR_NAME[$1]}"
}

pids_of() {
  local svc="$1"
  local jar
  jar="$(svc_jar "$svc")"
  pgrep -f "$jar" 2>/dev/null || true
}

stop_one() {
  local svc="$1"
  local pids
  pids="$(pids_of "$svc")"
  if [[ -z "$pids" ]]; then
    echo "[$svc] 未在运行"
    return 0
  fi
  echo "[$svc] 停止: $pids"
  # shellcheck disable=SC2086
  kill $pids 2>/dev/null || true
  local i=0
  while [[ -n "$(pids_of "$svc")" && $i -lt 15 ]]; do
    sleep 1
    i=$((i + 1))
  done
  pids="$(pids_of "$svc")"
  if [[ -n "$pids" ]]; then
    echo "[$svc] 强制结束: $pids"
    # shellcheck disable=SC2086
    kill -9 $pids 2>/dev/null || true
  fi
  echo "[$svc] 已停止"
}

start_one() {
  local svc="$1"
  local dir jar heap logctx
  dir="$(svc_dir "$svc")"
  jar="$(svc_jar "$svc")"
  heap="${HEAP[$svc]}"

  if [[ ! -f "$jar" ]]; then
    echo "[$svc] 找不到 $jar，请先执行: $0 build"
    return 1
  fi
  if [[ -n "$(pids_of "$svc")" ]]; then
    echo "[$svc] 已在运行 (PID $(pids_of "$svc" | tr '\n' ' '))"
    return 0
  fi

  mkdir -p "$dir"
  mkdir -p "$ROOT/build/logs/$svc" 2>/dev/null || true

  local jvm=(java -Dfile.encoding=UTF-8 "-Xms${heap}" "-Xmx${heap}" -XX:+UseG1GC)
  local workdir="$dir"
  if [[ "$svc" == "web" ]]; then
    local ctx
    ctx="/$(web_path)"
    jvm+=("-Dserver.servlet.context-path=${ctx}")
    # 保持仓库根为工作目录，使 application.yml 中 build/game/replay 路径有效
    workdir="$ROOT"
    echo "[$svc] 启动 context-path=${ctx} heap=${heap}"
  else
    echo "[$svc] 启动 heap=${heap}"
  fi

  (
    cd "$workdir"
    nohup "${jvm[@]}" -jar "$jar" >/dev/null 2>&1 &
  )
  sleep 1
  if [[ -n "$(pids_of "$svc")" ]]; then
    echo "[$svc] 已启动 PID $(pids_of "$svc" | tr '\n' ' ')"
  else
    echo "[$svc] 启动失败，请检查 jar / 依赖"
    return 1
  fi
}

cmd_start() {
  need_java
  local targets
  # shellcheck disable=SC2207
  targets=($(resolve_targets "${1:-all}"))
  local svc
  for svc in "${targets[@]}"; do
    start_one "$svc" || true
    sleep 1
  done
  if [[ "${1:-all}" == "all" || "${1:-}" == "web" ]]; then
    echo "外网入口: https://${DOMAIN}/$(web_path)/"
  fi
}

cmd_stop() {
  local targets
  # shellcheck disable=SC2207
  targets=($(resolve_targets "${1:-all}"))
  # 反向停止
  local reversed=()
  local i
  for ((i=${#targets[@]}-1; i>=0; i--)); do
    reversed+=("${targets[i]}")
  done
  local svc
  for svc in "${reversed[@]}"; do
    stop_one "$svc"
  done
}

cmd_restart() {
  local arg="${1:-all}"
  cmd_stop "$arg"
  sleep 1
  cmd_start "$arg"
}

cmd_status() {
  local targets
  # shellcheck disable=SC2207
  targets=($(resolve_targets "${1:-all}"))
  local svc pids
  printf "%-8s %-10s %s\n" "SERVICE" "STATE" "PID"
  for svc in "${targets[@]}"; do
    pids="$(pids_of "$svc" | tr '\n' ' ')"
    if [[ -n "${pids// /}" ]]; then
      printf "%-8s %-10s %s\n" "$svc" "running" "$pids"
    else
      printf "%-8s %-10s %s\n" "$svc" "stopped" "-"
    fi
  done
  local wp
  wp="$(web_path)"
  echo "外网入口: https://${DOMAIN}/${wp}/"
  echo "本机 web:  http://127.0.0.1:8081/${wp}/"
  if [[ -f /etc/nginx/conf.d/www.cloudhui.cc.cd.conf ]]; then
    if grep -q "/${wp}/" /etc/nginx/conf.d/www.cloudhui.cc.cd.conf 2>/dev/null; then
      echo "Nginx 反代: 已配置随机路径"
    else
      echo "警告: Nginx 配置中未找到路径 /${wp}/ ，请同步 conf 后 reload"
    fi
  fi
}

cmd_build() {
  command -v mvn >/dev/null 2>&1 || { echo "未找到 mvn"; exit 1; }
  cd "$ROOT"
  echo "打包中（跳过 mcp/sp，跳过测试）..."
  mvn -q install -DskipTests -pl '!mcp,!sp'
  if [[ -f "$ROOT/web/target/Web.jar" && ! -f "$BUILD/web/Web.jar" ]]; then
    mkdir -p "$BUILD/web"
    cp -f "$ROOT/web/target/Web.jar" "$BUILD/web/Web.jar"
  fi
  echo "打包完成。产物目录: $BUILD"
  for svc in "${ORDER[@]}"; do
    if [[ -f "$(svc_jar "$svc")" ]]; then
      echo "  OK $(svc_jar "$svc")"
    else
      echo "  MISSING $(svc_jar "$svc")"
    fi
  done
}

cmd_clean_logs() {
  local days=7
  local removed=0
  local dirs=(
    "$ROOT/logs"
    "$ROOT/build/logs"
    "$BUILD/web/logs"
    "$ROOT/web/logs"
  )
  local d
  for d in "${dirs[@]}"; do
    [[ -d "$d" ]] || continue
    while IFS= read -r -d '' f; do
      rm -f "$f"
      removed=$((removed + 1))
    done < <(find "$d" -type f \( -name '*.log' -o -name '*.log.gz' -o -name '*.zip' \) -mtime +$((days - 1)) -print0 2>/dev/null)
  done
  echo "已清理超过 ${days} 天的日志文件，删除 ${removed} 个"
}

main() {
  local cmd="${1:-}"
  local arg="${2:-all}"
  case "$cmd" in
    start) cmd_start "$arg" ;;
    stop) cmd_stop "$arg" ;;
    restart) cmd_restart "$arg" ;;
    status) cmd_status "$arg" ;;
    build) cmd_build ;;
    clean-logs) cmd_clean_logs ;;
    -h|--help|help|"") usage; [[ -n "$cmd" ]] || exit 1 ;;
    *) echo "未知命令: $cmd"; usage; exit 1 ;;
  esac
}

main "$@"

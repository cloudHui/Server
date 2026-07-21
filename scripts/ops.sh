#!/usr/bin/env bash
# Linux 运维入口：启停 / 状态 / 打包 / 清日志
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS="$ROOT/scripts"
NGINX_DIR="$SCRIPTS/nginx"
BUILD="$ROOT/build"
LOG_HOME="$ROOT/logs"
PATH_FILE="$SCRIPTS/web-path.txt"
# 仅作 status 展示；nginx-apply 必须显式传入域名
DOMAIN="${SERVER_DOMAIN:-}"

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
用法:
  $0 {start|stop|restart|status} [服务|all]
  $0 build
  $0 clean-logs
  $0 nginx-apply <域名>

服务: center | gate | lobby | game | web | all

示例:
  $0 start
  $0 restart web
  $0 build
  $0 clean-logs
  $0 nginx-apply www.example.com
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
  mkdir -p "$LOG_HOME/$svc"

  local jvm=(
    java
    -Dfile.encoding=UTF-8
    "-DLOG_HOME=${LOG_HOME}"
    "-Xms${heap}"
    "-Xmx${heap}"
    -XX:+UseG1GC
  )
  local workdir="$dir"
  if [[ "$svc" == "web" ]]; then
    local ctx
    ctx="/$(web_path)"
    jvm+=("-Dserver.servlet.context-path=${ctx}")
    # 保持仓库根为工作目录，使 application.yml 中 build/game/replay 路径有效
    workdir="$ROOT"
    echo "[$svc] 启动 context-path=${ctx} heap=${heap} log=${LOG_HOME}/${svc}"
  else
    echo "[$svc] 启动 heap=${heap} log=${LOG_HOME}/${svc}"
  fi

  local console_out="$LOG_HOME/$svc/console.out"
  (
    cd "$workdir"
    nohup "${jvm[@]}" -jar "$jar" >>"$console_out" 2>&1 &
  )
  sleep 1
  if [[ -n "$(pids_of "$svc")" ]]; then
    echo "[$svc] 已启动 PID $(pids_of "$svc" | tr '\n' ' ')"
  else
    echo "[$svc] 启动失败，请检查 $console_out 与 jar / 依赖"
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
    local wp
    wp="$(web_path)"
    if [[ -n "$DOMAIN" ]]; then
      echo "外网入口: https://${DOMAIN}/${wp}/"
    else
      echo "Web 路径: /${wp}/ （外网域名请用 nginx-apply <域名> 或 SERVER_DOMAIN）"
    fi
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
  echo "本机 web:  http://127.0.0.1:8081/${wp}/"
  if [[ -n "$DOMAIN" ]]; then
    echo "外网入口: https://${DOMAIN}/${wp}/"
  else
    echo "外网入口: 设置 SERVER_DOMAIN 或使用 nginx-apply 时的域名 + /${wp}/"
  fi
  echo "日志目录: $LOG_HOME/<服务>/{日期日志, error-*.log, console.out}"
  if [[ -f /etc/nginx/snippets/game-web.conf ]] && grep -q "/${wp}/" /etc/nginx/snippets/game-web.conf 2>/dev/null; then
    echo "Nginx 反代: snippets/game-web.conf 已是当前路径"
  elif [[ -n "$DOMAIN" && -f "/etc/nginx/conf.d/${DOMAIN}.conf" ]] && grep -q "/${wp}/" "/etc/nginx/conf.d/${DOMAIN}.conf" 2>/dev/null; then
    echo "Nginx 反代: 域名 conf 内已含当前路径（建议改用 nginx-apply）"
  else
    echo "Nginx 反代: 未检测到当前路径，可执行: $0 nginx-apply <域名>"
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
    "$LOG_HOME"
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
    done < <(find "$d" -type f \( -name '*.log' -o -name '*.log.gz' -o -name '*.zip' -o -name 'console.out' \) -mtime +$((days - 1)) -print0 2>/dev/null)
  done
  echo "已清理超过 ${days} 天的日志文件，删除 ${removed} 个（统一目录: $LOG_HOME）"
}

find_domain_conf() {
  local domain="$1"
  local candidate="/etc/nginx/conf.d/${domain}.conf"
  if [[ -f "$candidate" ]]; then
    echo "$candidate"
    return 0
  fi
  local f
  for f in /etc/nginx/conf.d/*.conf; do
    [[ -f "$f" ]] || continue
    if grep -qE "server_name[[:space:]]+.*${domain}" "$f" 2>/dev/null; then
      echo "$f"
      return 0
    fi
  done
  return 1
}

cmd_nginx_apply() {
  local domain="${1:-}"
  if [[ -z "$domain" ]]; then
    echo "请传入域名，例如: $0 nginx-apply www.example.com" >&2
    exit 1
  fi
  command -v sudo >/dev/null 2>&1 || { echo "需要 sudo"; exit 1; }
  command -v python3 >/dev/null 2>&1 || { echo "需要 python3"; exit 1; }

  local wp conf map_src snippet_in snippet_out
  wp="$(web_path)"
  map_src="$NGINX_DIR/00-websocket-map.conf"
  snippet_in="$NGINX_DIR/game-web.snippet.conf.in"
  snippet_out="/etc/nginx/snippets/game-web.conf"

  [[ -f "$map_src" && -f "$snippet_in" ]] || { echo "缺少 nginx 模板，请确认 $NGINX_DIR"; exit 1; }

  if ! conf="$(find_domain_conf "$domain")"; then
    echo "未找到域名 $domain 的 Nginx conf（期望 /etc/nginx/conf.d/${domain}.conf）" >&2
    exit 1
  fi
  echo "域名 conf: $conf"
  echo "随机路径: /$wp/"

  sudo mkdir -p /etc/nginx/snippets
  local tmp
  tmp="$(mktemp)"
  sed "s/@WEB_PATH@/${wp}/g" "$snippet_in" >"$tmp"
  sudo install -m 644 "$tmp" "$snippet_out"
  rm -f "$tmp"

  sudo install -m 644 "$map_src" /etc/nginx/conf.d/00-websocket-map.conf

  # 域名 conf：去掉旧 8081 location，写入 include（幂等替换）
  sudo cp -a "$conf" "${conf}.bak.$(date +%Y%m%d%H%M%S)"
  tmp="$(mktemp)"
  cp -a "$conf" "$tmp" 2>/dev/null || sudo cat "$conf" >"$tmp"
  chmod u+w "$tmp"
  python3 "$NGINX_DIR/apply_game_web.py" "$tmp"
  sudo install -m 644 "$tmp" "$conf"
  rm -f "$tmp"

  if sudo nginx -t; then
    sudo systemctl reload nginx
    echo "Nginx 已应用并 reload"
    echo "外网入口: https://${domain}/${wp}/"
  else
    echo "nginx -t 失败，已保留 .bak；请检查 $conf" >&2
    exit 1
  fi
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
    nginx-apply) cmd_nginx_apply "${2:-}" ;;
    -h|--help|help|"") usage; [[ -n "$cmd" ]] || exit 1 ;;
    *) echo "未知命令: $cmd"; usage; exit 1 ;;
  esac
}

main "$@"

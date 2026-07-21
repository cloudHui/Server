#!/usr/bin/env python3
"""将 game-web snippet include 写入指定域名的 HTTPS server，并去掉旧的 8081 反代 location。"""
from __future__ import annotations

import re
import sys
from pathlib import Path

INCLUDE_LINE = "    include /etc/nginx/snippets/game-web.conf;"


def iter_location_spans(text: str):
    """Yield (start, end) of each top-level-ish location { ... } block found left-to-right."""
    i = 0
    while True:
        m = re.search(r"(?m)^[ \t]*location[ \t][^\n]*\{", text[i:])
        if not m:
            return
        start = i + m.start()
        brace = i + m.end() - 1
        depth = 0
        j = brace
        while j < len(text):
            c = text[j]
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    j += 1
                    yield start, j
                    i = j
                    break
            j += 1
        else:
            return


def strip_game_locations(text: str) -> str:
    text = re.sub(
        r"(?m)^[ \t]*include[ \t]+/etc/nginx/snippets/game-web\.conf;[ \t]*\n?",
        "",
        text,
    )
    # 删掉「棋牌」注释行
    text = re.sub(r"(?m)^[ \t]*#[^\n]*棋牌[^\n]*\n", "", text)

    spans = list(iter_location_spans(text))
    drop = set()
    redirect_paths = {}

    for idx, (a, b) in enumerate(spans):
        block = text[a:b]
        if "127.0.0.1:8081" in block:
            drop.add(idx)
            m = re.search(r"location\s+\^~\s+/([^/\s]+)/", block)
            if m:
                redirect_paths[m.group(1)] = idx

    for idx, (a, b) in enumerate(spans):
        if idx in drop:
            continue
        block = text[a:b]
        m = re.match(
            r"[ \t]*location\s*=\s*/([A-Za-z0-9_-]+)\s*\{[^{}]*return\s+302\s+/([A-Za-z0-9_-]+)/",
            block,
            re.S,
        )
        if m and m.group(1) == m.group(2) and m.group(1) in redirect_paths:
            drop.add(idx)

    if not drop:
        # 仍清理历史残留：纯 302 自跳转且无其它指令（仅在已无 8081 时，靠 path 与 snippet 无关则保留）
        # 此处不额外删除，避免误伤
        pass

    keep_parts = []
    last = 0
    for idx, (a, b) in enumerate(spans):
        if idx in drop:
            keep_parts.append(text[last:a])
            last = b
            # 吃掉块后多余空行一个
            while last < len(text) and text[last] == "\n":
                last += 1
                break
    keep_parts.append(text[last:])
    text = "".join(keep_parts)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text


def inject_include(text: str) -> str:
    if re.search(r"(?m)^[ \t]*include[ \t]+/etc/nginx/snippets/game-web\.conf;", text):
        return text

    servers = list(re.finditer(r"(?m)^server\s*\{", text))
    if not servers:
        raise SystemExit("未找到 server 块")

    def server_span(start: int) -> tuple[int, int]:
        depth = 0
        j = start
        while j < len(text):
            if text[j] == "{":
                depth += 1
            elif text[j] == "}":
                depth -= 1
                if depth == 0:
                    return start, j + 1
            j += 1
        raise SystemExit("server 块未闭合")

    target = None
    for m in servers:
        a, b = server_span(m.start())
        block = text[a:b]
        if re.search(r"listen[^\n]*8443", block) or re.search(r"ssl_certificate", block):
            target = (a, b, block)
            break
    if target is None:
        m = servers[-1]
        a, b = server_span(m.start())
        target = (a, b, text[a:b])

    a, b, block = target
    loc = re.search(r"(?m)^[ \t]*location[ \t]+/[ \t]*\{", block)
    if loc:
        insert_at = a + loc.start()
        return text[:insert_at] + INCLUDE_LINE + "\n\n" + text[insert_at:]
    insert_at = b - 1
    return text[:insert_at] + INCLUDE_LINE + "\n" + text[insert_at:]


def main() -> None:
    if len(sys.argv) != 2:
        print("usage: apply_game_web.py <domain-conf-path>", file=sys.stderr)
        sys.exit(2)
    path = Path(sys.argv[1])
    raw = path.read_text(encoding="utf-8")
    cleaned = strip_game_locations(raw)
    updated = inject_include(cleaned)
    path.write_text(updated, encoding="utf-8")
    print(f"updated {path}")


if __name__ == "__main__":
    main()

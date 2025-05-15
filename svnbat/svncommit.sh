#!/bin/bash

# 定义变量
REPO_PATH="/path/to/your/svn/working/copy"
COMMIT_MSG="Automated commit: Modified, Deleted, and Added files"

# 切换到SVN工作副本目录
cd "$REPO_PATH" || exit

# 自动化处理所有新增文件
# 找出所有未被版本控制的新文件，并将其添加到版本控制中
for file in $(svn status | grep '^?' | awk '{print $2}'); do
    svn add "$file"
done

# 自动化处理所有已删除文件
# 找出所有已被删除但未标记为删除的文件，并将其标记为删除
for file in $(svn status | grep '^!' | awk '{print $2}'); do
    svn delete "$file"
done

# 提交所有更改
svn commit -m "$COMMIT_MSG"

echo "所有操作完成并已提交到SVN仓库。"
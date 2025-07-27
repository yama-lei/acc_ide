# ACC IDE 版本发布记录

本文档记录ACC IDE的所有正式版本发布信息。

## 版本 1.2.1 (2025-07-28)

- **下载链接**: [ACC_IDE_v1.2.1.apk](release/1.2.1/ACC_IDE_v1.2.1.apk)
- **Git标签**: `v1.2.1`

### 主要更新

- 添加了textmate的支持，对语法高亮进行了扩展
- 加入了贴合ui配色的dark/light主题
- 删除之前用Java解释器进行的语法高亮
- 加入了编辑页面对AgaveNerdFont字体的支持
- 加入自动补全的开关

### TODO

- 修复语言切换、主题切换的界面相应bug
- 加入语法树
- 加入treesitter和lsp
- 修复在GitHub Action不能执行java和py

## 版本 1.2.0 (2025-07-15)

- **下载链接**: [ACC_IDE_v1.2.0.apk](release/1.2.0/ACC_IDE_v1.2.0.apk)
- **Git标签**: `v1.2.0`

### 主要更新
- 添加了撤回和重做功能
- 添加了符号面板，便于安卓快速输入符号
- 使用`Github Action`真正实现了C++的IO功能

### 修复
- 修复了临时目录下文件关闭后无法正确删除的bug
- 修复了新建文件名称的bug
- 修复了启动界面卡白屏的bug

## 版本 1.1.1 (2025-06-20)

- **下载链接**: [acc_ide-1.1.1-unsigned.apk](release/1.1.1/acc_ide-1.1.1-unsigned.apk)
- **Git标签**: `v1.1.1`

### 主要更新
- 添加了全新图标，提升了整体视觉体验
- 优化了符号面板的布局，减小了面板高度
- 修复了多个UI渲染问题
- 提升了应用整体性能

## 版本 1.1.0 (2025-06-20)

- **下载链接**: [acc_ide-1.1.0-unsigned.apk](release/1.1.0/acc_ide-1.1.0-unsigned.apk) 
- **Git标签**: `v1.1.0`

### 主要更新
- 添加了光标宽度自定义设置（2dp-14dp）
- 改进了符号面板布局
- 优化了编辑器性能
- 改进了自动完成功能的稳定性

## 版本 1.0.0 (2025-06-01)

本版由flutter版本转化过来，使用[sora-editor](https://github.com/Rosemoe/sora-editor)库进行模板化构建

- **下载链接**: [acc_ide-1.0.0.apk](release/1.0.0/acc_ide-1.0.0.apk)
- **Git标签**: `v1.0.0`

### 首次发布功能
- 文件管理系统
- 明/暗主题支持
- 输入/输出测试面板 
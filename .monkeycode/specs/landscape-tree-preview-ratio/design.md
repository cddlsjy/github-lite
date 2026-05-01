# 技术设计：横屏模式目录树与文件预览比例调整

## 概述

将 GitHub Explorer Android 应用在横屏模式下，目录树（FileTree）与文件预览（File Content）窗口的显示比例从当前的 1:1（各占 50%）调整为 1:2（目录树占 33.33%，文件预览占 66.66%）。

## 需求分析

### 当前状态
- **横屏模式**：目录树和文件预览左右并排，比例为 1:1（`weight = 1f : 1f`）
- **竖屏模式**：目录树和文件预览上下排列，比例为 1:1（`weight = 1f : 1f`）

### 目标状态
- **横屏模式**：目录树占 33.33%，文件预览占 66.66%（`weight = 0.3333f : 0.6666f`）
- **竖屏模式**：保持 1:1 比例不变

### 影响范围
- 仅影响 `FileTreeFragment` 的布局逻辑
- 仅修改横屏模式下的比例，竖屏模式不受影响

## 技术方案

### 方案一：修改 Kotlin 代码中的 weight 值（推荐）

#### 修改文件
`app/src/main/java/com/example/githubexplorer/ui/repodetail/FileTreeFragment.kt`

#### 修改内容
在 `updateLayoutOrientation()` 方法中，修改横屏分支的 weight 赋值：

**修改前**（第 76-83 行）：
```kotlin
if (isLandscape) {
    treeParams.width = 0
    treeParams.height = LinearLayout.LayoutParams.MATCH_PARENT
    treeParams.weight = 1f

    scrollParams.width = 0
    scrollParams.height = LinearLayout.LayoutParams.MATCH_PARENT
    scrollParams.weight = 1f
}
```

**修改后**：
```kotlin
if (isLandscape) {
    treeParams.width = 0
    treeParams.height = LinearLayout.LayoutParams.MATCH_PARENT
    treeParams.weight = 0.3333f

    scrollParams.width = 0
    scrollParams.height = LinearLayout.LayoutParams.MATCH_PARENT
    scrollParams.weight = 0.6666f
}
```

#### 优点
- 改动最小，仅修改 2 行代码
- 逻辑清晰，易于维护
- 不影响 XML 布局文件

#### 缺点
- XML 中的初始 `layout_weight="1"` 与代码中的值不一致，可能造成理解困惑
- 但 XML 值仅作为初始值，实际会被代码覆盖，不影响运行

### 方案二：同时修改 XML 和 Kotlin 代码（备选）

#### 修改文件 1
`app/src/main/res/layout/fragment_file_tree.xml`

#### 修改内容
修改目录树容器和文件预览容器的初始 weight 值：

**第 49 行**：`android:layout_weight="1"` → `android:layout_weight="0.3333"`
**第 72 行**：`android:layout_weight="1"` → `android:layout_weight="0.6666"`

#### 修改文件 2
`app/src/main/java/com/example/githubexplorer/ui/repodetail/FileTreeFragment.kt`

修改内容同方案一。

#### 优点
- XML 与代码保持一致，可读性更好

#### 缺点
- 需要修改两个文件
- XML 中的竖屏初始比例也会被改变（虽然竖屏时会被代码覆盖回 1:1）

## 推荐方案

**采用方案一**：仅修改 Kotlin 代码中的 weight 值。

理由：
1. 改动最小，风险最低
2. XML 布局的 weight 值在运行时会被 `updateLayoutOrientation()` 方法完全覆盖，实际不影响显示
3. 横竖屏切换时会重新计算并应用正确的 weight 值

## 测试计划

### 功能测试
1. **横屏模式测试**
   - 启动应用，进入仓库详情页
   - 旋转设备至横屏
   - 验证目录树约占屏幕宽度的 1/3
   - 验证文件预览约占屏幕宽度的 2/3

2. **竖屏模式测试**
   - 旋转设备至竖屏
   - 验证目录树和文件预览仍为 1:1 比例（上下排列）

3. **横竖屏切换测试**
   - 在横屏和竖屏之间多次切换
   - 验证每次切换后比例正确应用
   - 验证已选择的文件内容保持显示

### 边界测试
1. 平板设备横屏显示
2. 折叠屏设备展开/折叠状态
3. 分屏模式下的横屏显示

## 风险评估

| 风险项 | 可能性 | 影响 | 缓解措施 |
|--------|--------|------|----------|
| weight 值计算精度问题 | 低 | 低 | Android LinearLayout 的 weight 机制支持浮点数，0.3333 + 0.6666 ≈ 1，不会导致布局溢出 |
| 竖屏比例被意外修改 | 低 | 中 | 代码中竖屏分支保持不变，明确只修改横屏分支 |
| 旧版本 Android 兼容性 | 低 | 低 | weight 机制从 API 1 开始支持，最低 SDK 24 无兼容问题 |

## 实施步骤

1. 修改 `FileTreeFragment.kt` 第 79 行和第 83 行的 weight 值
2. 编译项目，确认无编译错误
3. 在模拟器或真机上进行功能测试
4. 提交代码并创建 Pull Request

## 后续优化建议（可选）

如果未来需要支持可调节比例，可以考虑：
1. 添加 `SplitLayout` 或拖拽分割线组件
2. 将比例值存入用户偏好设置（`PreferenceHelper`）
3. 支持用户自定义横屏/竖屏比例

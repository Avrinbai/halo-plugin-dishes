<div align="center">
    <img alt="logo" width="106px" src="./web-frontend/src/assets/logo.png" style="border-radius: 16px;">
    <h1>Halo - Dishes（家庭私厨）</h1>
    <p>一个面向家庭场景的 Halo 点菜插件</p>
    <p align="center">
    <a href="https://www.halo.run/store/apps/app-espsmupa"><img alt="Halo App Store" src="https://img.shields.io/badge/Halo-%E5%BA%94%E7%94%A8%E5%B8%82%E5%9C%BA-%230A81F5?style=flat-square&logo=appstore&logoColor=%23fff" /></a>
        <a href="//github.com/Avrinbai/halo-plugin-dishes/releases"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/Avrinbai/halo-plugin-dishes?style=flat-square&logo=github" /></a>
        <a href="./LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/Avrinbai/halo-plugin-dishes?style=flat-square" /></a>
    </p>

</div>

## 概览

`Dishes` 是一个基于 Halo 2 的家庭私厨点菜插件，包含后台管理端与前台点菜页：

- 后台：维护分类、菜品、点菜记录、访问与通知设置。
- 前台：按餐段点菜，支持密码访问、推荐与预约场景。

## 目录

| 章节 | 说明 |
|------|------|
| [功能亮点](#功能亮点) | 能力列表 |
| [预览图](#预览图) | 界面截图 |
| [快速开始](#快速开始) | 安装、配置、初始化、使用 |
| [部署指南](#部署指南) | 插件内嵌 / 独立域名 / 子路径 |
| [编译与构建](#编译与构建) | Gradle 插件包、单独构建前台 |
| [Nginx 参考配置](#nginx-参考配置) | 独立前台时 API 反代示例 |
| [验证与排错](#验证与排错) | 上线自检与常见问题 |
| [许可证](#许可证) | 协议 |

## 功能亮点

- 菜品分类管理（新增、编辑、排序、删除）
- 菜品管理（上下架、推荐等级、餐段配置、批量删除）
- 点菜记录管理（按日期查看、明细查看）
- 前台点菜页（早餐/午餐/晚餐、提交备注、预约点餐）
- 访问控制（公开/密码）
- 消息通知（支持企业微信 webhook 推送）

##管理端预览

<div align="center">

<img src="./images/admin-preview.png" alt="菜品管理预览" title="菜品管理" width="31%" />
<img src="./images/admin-preview-2.png" alt="点菜记录预览" title="点菜记录" width="31%" />
<img src="./images/admin-preview-3.png" alt="插件设置预览" title="插件设置" width="31%" />


</div>

## 前台预览

<div align="center">

<img src="./images/public-preview.png" alt="前台首页预览" title="前台首页" width="40%" />
<img src="./images/public-preview-2.png" alt="前台点菜页预览" title="前台点菜页" width="40%" />

</div>

## 快速开始

### 1) 安装插件

在 Halo 应用市场安装，或手动上传本项目构建后的插件包。

### 2) 基础配置

进入 Halo 后台插件设置页，按需配置：

- 访问模式（公开/密码）
- 前台访问路径
- 前台 Logo
- 前台域名白名单（建议配置，限制可调用前台 API 的来源域名）
- 通知开关与 企业微信机器人 webhook 地址

### 3) 初始化菜单与菜品

依次创建分类与菜品，配置推荐等级、可用餐段与上下架状态。

### 4) 开始使用

打开前台页面进行点菜，可查看历史与预约记录。

---

## 部署指南

按前台托管方式选择下面一种即可。

### 场景 A：作为 Halo 插件前台（同域）

- **适用**：前台静态资源由 Halo 插件托管，访问与 Halo 同域。
- **配置**：`web-frontend/.env.production` 保持默认（如下）。

```env
VITE_ASSET_BASE=
```

### 场景 B：独立域名部署（前台在 A，Halo 在 B）

#### 前提（API）

- 浏览器只访问**前台域名**。
- 路径 **`/plugins/dishes/public/`**（前台默认）必须由 **Nginx 反代到 Halo**。

#### 域名示例

- 前台：`https://menu.example.com`
- Halo：`https://blog.example.com`

#### 前端环境变量

构建打包时请把示例中的 `blog.example.com` 换成你的 Halo 主站根地址（无尾部斜杠）。

```env
VITE_ASSET_BASE=/

# 前台挂在域名根目录（打开即首页）时填 /
VITE_PUBLIC_BASE=/

# 独立站必填：Halo 主站根地址（无尾部斜杠），将接口返回的 /upload/... 解析到主站，避免菜品图片资源 404
VITE_MEDIA_ORIGIN=https://blog.example.com
```

#### 附件图片 `/upload/`

接口返回的菜品图等常为站点相对路径 `/upload/...`。独立部署时若仍用当前前台域名请求，会落在无文件的站点上。**请统一在 `web-frontend/.env.production` 配置 `VITE_MEDIA_ORIGIN` 为主站根地址**（无尾部斜杠），构建后前端会把 `/upload/` 资源指向 Halo。

#### 其它说明

- Nginx 反代示例见下文「Nginx 参考配置」（API **`/plugins/dishes/public/`**；附件通过 `VITE_MEDIA_ORIGIN` 指到主站）。
- 插件后台「前台域名白名单」仍用于服务端校验请求来源。

### 场景 C：子路径部署（如 `https://menu.example.com/dishes/`）

`web-frontend/.env.production`：

```env
VITE_ASSET_BASE=/dishes/
VITE_PUBLIC_BASE=/dishes/
```

- `VITE_ASSET_BASE` 与 `VITE_PUBLIC_BASE` 必须以 `/` 开头并以 `/` 结尾，并与 Nginx 中前台子路径一致。
- API 仍须在同一前台域名下反代 `/plugins/dishes/public/` 到 Halo。

---

## 编译与构建

### 插件整体编译（推荐）

在项目根目录执行：

```bash
./gradlew clean build
```

Windows：

```powershell
.\gradlew.bat clean build
```

- 会统一构建后端与前端子模块，产出可安装插件包。
- 插件包目录：`build/libs`

### 何时单独构建前台

- **插件内嵌、同域使用**：不必单独跑 `web-frontend` / `ui` 的构建命令，用上面的 Gradle 即可。
- **独立域名或子路径单独托管前台**：在 `web-frontend` 目录额外执行：

```bash
pnpm install
pnpm build
```

产物目录：`web-frontend/build/dist`

---

## Nginx 参考配置

独立前台部署**必须**通过反向代理：浏览器只访问前台域名，由 Nginx 将 **`/plugins/dishes/public/`** 转发到 Halo，避免跨域与 Cookie 问题。

默认走 **`/plugins/dishes/public/...`**（与匿名 RBAC 一致）。独立静态页无 Thymeleaf CSRF 时，代码会在 POST 前先 GET **`/access/status`** 以触发 `XSRF-TOKEN`；若仍 403，请确认反代未丢弃 `Set-Cookie`。

### 独立域名（根路径）示例

```nginx
server {
    listen 80;
    server_name menu.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name menu.example.com;

    # 前台静态文件目录（替换为你的实际路径）
    root /var/www/dishes-frontend;
    index index.html;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 代理插件公共 API 到 Halo 主站
    location /plugins/dishes/public/ {
        proxy_pass https://blog.example.com/plugins/dishes/public/;
        proxy_http_version 1.1;
        proxy_set_header Host blog.example.com;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 验证与排错

- 页面静态资源可正常加载（无 404）。
- 浏览器网络面板中，API 请求路径为 `/plugins/dishes/public/...`（默认）。
- API 请求返回 JSON（不是 HTML 登录页）。若出现 **`/login?authentication_required` 且 404**，多为误将 API 指到 **`/apis/plugins/...`** 且被 Halo 要求登录；请改回默认前缀并反代 **`/plugins/dishes/public/`**。若 POST **403**，检查 CSRF Cookie 是否被反代/浏览器拦截。
- 若出现跨域报错，说明 API 仍指向了 Halo 绝对地址或未配置反代；应确保请求 URL 为前台域名下的上述路径。
- 若你希望 API 走自定义前缀（如 `/dishes-api/`），可设置 `VITE_API_PREFIX` 并同步修改 Nginx `location`。

## 许可证

[GPL-3.0](./LICENSE)

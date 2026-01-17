# My Little App 全栈项目

## 项目简介

My Little App 是一个基于前后端分离架构的企业级全栈应用，支持多端部署（移动端 H5/苹果/安卓/鸿蒙、微信小程序、管理后台 Web），提供博客发布、AI 对话、工具合集等丰富功能，并采用现代化的技术栈进行开发。

## [项目介绍 →](about.md)

## 项目演示（微信小程序 + Web 后台）

https://github.com/user-attachments/assets/ca3cd36f-22e5-430e-800b-ec32f96b2335

https://github.com/user-attachments/assets/62a9a035-eaa6-4a96-93bc-ca8516489a0a

---

## Docker 部署

### 1. 准备 SSL 证书

将域名证书文件放到项目根目录的 `ssl` 文件夹中：

```bash
mkdir -p ssl
# 复制证书文件
# ssl/your-domain.com.crt    (域名证书)
# ssl/your-domain.com.key    (私钥)
```

> **没有 SSL 证书？** 可使用 Let's Encrypt 免费获取，或使用 IP 地址:80 端口访问（仅限测试环境）。

### 2. 配置环境变量

```bash
# 复制环境变量
cp .env.example .env
vim .env
```

### 3. 部署

```bash
./deploy.sh build
```

### 服务地址

| 服务 | 端口 | 访问地址 |
|------|------|----------|
| 后端 API | 8080 | https://your-domain.com:8080 |
| H5 前端 | 80/443 | https://your-domain.com |
| 管理后台 | 8081 | https://your-domain.com:8081 |
| MySQL | 3306 | - |
| Redis | 6379 | - |

> **微信小程序**：需将 `https://your-domain.com:8080` 配置为微信后台的 request 合法域名（需 ICP 备案）。

### 管理命令

```bash
./deploy.sh build    # 构建并启动
./deploy.sh update   # 更新代码并重新部署
./deploy.sh restart  # 重启服务
./deploy.sh stop     # 停止服务
./deploy.sh logs     # 查看日志 (可指定服务名)
./deploy.sh clean    # 清理所有资源（包括数据卷，谨慎使用）
```

### 数据库备份与恢复

```bash
./deploy.sh backup           # 备份数据库到 backups/ 目录
./deploy.sh list-backups     # 列出所有可用的备份
./deploy.sh restore <file>   # 从备份文件恢复数据库
```

示例：
```bash
# 查看可用备份
./deploy.sh list-backups

# 恢复数据库（需要输入确认）
./deploy.sh restore backup_20240115_120000.sql.gz
```

### 服务器迁移

```bash
./deploy.sh migrate    # 显示迁移步骤说明
./deploy.sh export     # 导出完整迁移包（数据库+配置）
./deploy.sh import <file>  # 从迁移包导入数据
```

**迁移步骤**：
1. **旧服务器**：导出迁移包
   ```bash
   ./deploy.sh export
   # 生成: migration_20240115_120000.tar.gz
   ```

2. **复制到新服务器**
   ```bash
   scp migration_xxx.tar.gz user@new-server:/path/to/project/
   ```

3. **新服务器**：准备环境并导入
   ```bash
   git clone <your-repo-url>
   cp .env.example .env  # 编辑 .env 配置
   ./deploy.sh build
   ./deploy.sh import migration_xxx.tar.gz
   ```

**重要迁移文件**：
- `.env` - 敏感配置（必须保持一致）
- `ssl/` - SSL 证书（如果有）
- `backups/` - 数据库备份

> **提示**：首次迁移建议先在测试环境验证，确保数据完整。

### 配置说明

所有敏感配置通过 `.env` 文件管理：

| 变量 | 说明 |
|------|------|
| `DATABASE_URL` | MySQL 连接地址 |
| `DATABASE_USERNAME` | 数据库用户名 |
| `DATABASE_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 密钥 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `SILICONFLOW_API_KEY` | SiliconFlow API Key |
| `BOCHA_API_KEY` | Bocha API Key |
| `MAIL_USERNAME` | 邮件账号 |
| `MAIL_PASSWORD` | 邮件授权码 |
| `VITE_API_BASE_URL` | 前端 API 地址 |
| `VITE_WEIXIN_APPID` | 微信小程序 AppID |
| `CORS_ALLOWED_ORIGINS` | CORS 白名单 |
| `SSL_CERT` | SSL 证书路径（容器内） |
| `SSL_KEY` | SSL 私钥路径（容器内） |

### 初始化管理员

首次部署完成后，访问管理后台 `/setup` 页面设置管理员账号：

1. 访问 https://your-domain.com:8081/setup
2. 设置用户名、邮箱和密码
3. 点击"创建管理员"完成初始化

> **注意**：首次部署后必须通过此页面创建管理员，否则无法登录系统。

### 重新初始化

如果忘记邮箱或密码，可通过命令行删除所有管理员后重新初始化：

```bash
# 进入 MySQL 容器
docker exec -it my-little-app-mysql mysql -uroot -proot123

# 删除所有管理员（会清空登录信息，但保留用户数据）
DELETE FROM t_admin;

# 退出容器
exit

# 重启后端服务使更改生效
./deploy.sh restart backend
```

执行后重新访问 https://your-domain.com:8081/setup 即可重新设置管理员。

### 安全建议

- **部署前修改默认密钥**：JWT_SECRET、数据库密码、API Keys 等
- **生产环境使用 SSL**：启用 HTTPS 加密传输
- **配置 HTTPS**：生产环境必须启用 HTTPS
- **定期轮换密钥**：API Key、数据库密码等定期更换
- **CORS 白名单**：生产环境只保留必要的域名

---

> 详细开发指南请参阅 [项目介绍 →](about.md)

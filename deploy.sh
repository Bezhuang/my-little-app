#!/bin/bash

# ==================== 部署脚本 ====================
# 用法: ./deploy.sh [build|update|restart|logs|interactive]
# 直接执行进入交互模式

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 默认端口配置
PORT_FRONTEND=${PORT_FRONTEND:-80}
PORT_ADMIN=${PORT_ADMIN:-8081}
PORT_API=${PORT_API:-8443}

# 加载环境变量
load_env() {
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    if [ -f "$SCRIPT_DIR/.env" ]; then
        export $(cat "$SCRIPT_DIR/.env" | grep -v '^#' | xargs)
    fi
}

# 函数：检查端口是否被占用
check_port() {
    local port=$1
    if lsof -i:$port &> /dev/null || netstat -tuln 2>/dev/null | grep -q ":$port " || ss -tuln 2>/dev/null | grep -q ":$port "; then
        return 1  # 端口被占用
    fi
    return 0  # 端口可用
}

# 函数：配置端口（交互式，仅端口）
configure_ports() {
    load_env

    echo -e "${GREEN}🔧 端口配置${NC}"
    echo ""

    # H5 前端端口
    echo -e "${BLUE}📦 H5 前端端口: ${PORT_FRONTEND}${NC}"
    if ! check_port $PORT_FRONTEND 2>/dev/null; then
        echo -e "${YELLOW}  端口 $PORT_FRONTEND 已被占用${NC}"
    fi
    read -p "  请输入 H5 前端端口 (直接回车使用默认: ${PORT_FRONTEND}): " input
    [ -n "$input" ] && PORT_FRONTEND=$input
    echo ""

    # 管理后台端口
    echo -e "${BLUE}📦 管理后台端口: ${PORT_ADMIN}${NC}"
    if ! check_port $PORT_ADMIN 2>/dev/null; then
        echo -e "${YELLOW}  端口 $PORT_ADMIN 已被占用${NC}"
    fi
    read -p "  请输入管理后台端口 (直接回车使用默认: ${PORT_ADMIN}): " input
    [ -n "$input" ] && PORT_ADMIN=$input
    echo ""

    # API 端口
    echo -e "${BLUE}📦 API 服务端口: ${PORT_API}${NC}"
    if ! check_port $PORT_API 2>/dev/null; then
        echo -e "${YELLOW}  端口 $PORT_API 已被占用${NC}"
    fi
    read -p "  请输入 API 服务端口 (直接回车使用默认: ${PORT_API}): " input
    [ -n "$input" ] && PORT_API=$input
    echo ""

    echo -e "${GREEN}✅ 端口配置完成${NC}"
    echo ""
    echo "📋 当前配置:"
    echo "  主域名:      ${DOMAIN_NAME:-http://localhost}"
    echo "  H5 前端:     $PORT_FRONTEND"
    echo "  管理后台:    $PORT_ADMIN"
    echo "  API 服务:    $PORT_API"
    echo ""
    echo -e "${YELLOW}💡 这些设置仅对本次运行生效，如需永久保存请写入 .env 文件${NC}"
    echo ""

    # 导出供后续使用
    export PORT_FRONTEND PORT_ADMIN PORT_API
}

# 函数：检查所有端口是否可用
check_all_ports() {
    local all_ok=true
    load_env
    echo -e "${YELLOW}🔍 检查端口占用情况...${NC}"
    echo ""

    local ports=(
        "$PORT_FRONTEND:H5 前端"
        "$PORT_ADMIN:管理后台"
        "$PORT_API:API 服务"
    )

    for p in "${ports[@]}"; do
        IFS=':' read -r port name <<< "$p"
        if ! check_port $port 2>/dev/null; then
            echo -e "  ${RED}❌ 端口 $port ($name) 已被占用${NC}"
            all_ok=false
        else
            echo -e "  ${GREEN}✅ 端口 $port ($name) 可用${NC}"
        fi
    done

    if [ "$all_ok" = false ]; then
        echo ""
        echo -e "${RED}❌ 部分端口被占用，无法启动服务${NC}"
        echo -e "${YELLOW}💡 请使用 ./deploy.sh 进入交互模式重新配置${NC}"
        return 1
    fi

    echo ""
    echo -e "${GREEN}✅ 所有端口可用${NC}"
    return 0
}

# 解析域名获取主机名
get_hostname() {
    local domain="$1"
    domain="${domain#http://}"
    domain="${domain#https://}"
    domain="${domain%%/*}"
    echo "$domain"
}

echo -e "${GREEN}🚀 My Little App 部署脚本${NC}"

# 检查 docker 命令
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ 未找到 docker 命令${NC}"
    echo ""
    echo "请安装 Docker："
    echo "  官方文档: https://docs.docker.com/engine/install/"
    exit 1
fi

# 检查 docker compose 版本
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif docker-compose version &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    echo -e "${RED}❌ 未找到 docker compose${NC}"
    echo ""
    echo "请安装 Docker Compose："
    echo "  官方文档: https://docs.docker.com/compose/install/"
    exit 1
fi

# 函数：构建并启动
build() {
    load_env

    # 先配置端口
    configure_ports

    # 检查端口
    if ! check_all_ports; then
        exit 1
    fi

    echo -e "${YELLOW}📦 构建镜像...${NC}"
    PORT_FRONTEND=$PORT_FRONTEND PORT_ADMIN=$PORT_ADMIN PORT_API=$PORT_API \
        DOMAIN_NAME="$DOMAIN_NAME" \
        $COMPOSE_CMD -f docker-compose.yml build --no-cache
    echo -e "${GREEN}✅ 构建完成${NC}"

    start
}

# 函数：启动服务
start() {
    load_env

    echo -e "${YELLOW}🚀 启动服务...${NC}"
    PORT_FRONTEND=$PORT_FRONTEND PORT_ADMIN=$PORT_ADMIN PORT_API=$PORT_API \
        DOMAIN_NAME="$DOMAIN_NAME" \
        $COMPOSE_CMD -f docker-compose.yml up -d
    echo -e "${GREEN}✅ 服务已启动${NC}"
    echo -e "${YELLOW}📋 服务状态:${NC}"
    PORT_FRONTEND=$PORT_FRONTEND PORT_ADMIN=$PORT_ADMIN PORT_API=$PORT_API \
        DOMAIN_NAME="$DOMAIN_NAME" \
        $COMPOSE_CMD -f docker-compose.yml ps
    echo ""

    # 显示访问地址
    DOMAIN_HOSTNAME=$(get_hostname "$DOMAIN_NAME")
    if [[ "$DOMAIN_NAME" == https://* ]]; then
        # HTTPS 模式
        echo -e "${GREEN}🌐 访问地址 (HTTPS):${NC}"
        echo "  H5:        https://${DOMAIN_HOSTNAME}"
        echo "  管理后台:  https://admin.${DOMAIN_HOSTNAME}:${PORT_ADMIN}"
        echo "  后端 API:  https://api.${DOMAIN_HOSTNAME}:${PORT_API}"
    else
        # HTTP 模式
        echo -e "${GREEN}🌐 访问地址 (HTTP):${NC}"
        echo "  H5:        http://localhost:${PORT_FRONTEND:-80}"
        echo "  管理后台:  http://localhost:${PORT_ADMIN}"
        echo "  后端 API:  http://localhost:${PORT_API}"
    fi
    echo ""
}

# 函数：更新代码并重新部署
update() {
    echo -e "${YELLOW}📥 更新代码...${NC}"
    git pull
    echo -e "${GREEN}✅ 代码已更新${NC}"
    build
}

# 函数：重启服务
restart() {
    load_env
    echo -e "${YELLOW}🔄 重启服务...${NC}"
    PORT_FRONTEND=$PORT_FRONTEND PORT_ADMIN=$PORT_ADMIN PORT_API=$PORT_API \
        DOMAIN_NAME="$DOMAIN_NAME" \
        $COMPOSE_CMD -f docker-compose.yml restart
    echo -e "${GREEN}✅ 服务已重启${NC}"
}

# 函数：停止服务
stop() {
    echo -e "${YELLOW}🛑 停止服务...${NC}"
    $COMPOSE_CMD -f docker-compose.yml down
    echo -e "${GREEN}✅ 服务已停止${NC}"
}

# 函数：查看日志
logs() {
    load_env
    PORT_FRONTEND=$PORT_FRONTEND PORT_ADMIN=$PORT_ADMIN PORT_API=$PORT_API \
        DOMAIN_NAME="$DOMAIN_NAME" \
        $COMPOSE_CMD -f docker-compose.yml logs -f "${2:-}"
}

# 函数：清理
clean() {
    echo -e "${YELLOW}🧹 清理资源...${NC}"
    $COMPOSE_CMD -f docker-compose.yml down -v
    echo -e "${GREEN}✅ 已清理所有资源（包括数据卷）${NC}"
}

# 函数：备份数据库
backup() {
    load_env
    BACKUP_DIR="backups"
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="${BACKUP_DIR}/backup_${TIMESTAMP}.sql.gz"

    mkdir -p "${BACKUP_DIR}"

    echo -e "${YELLOW}💾 正在备份数据库...${NC}"

    if [ ! -w "${BACKUP_DIR}" ]; then
        echo -e "${RED}❌ 备份目录 ${BACKUP_DIR} 没有写权限${NC}"
        exit 1
    fi

    docker exec my-little-app-mysql mysqldump -u root -p"${DATABASE_PASSWORD}" \
        --databases my_little_app | gzip > "${BACKUP_FILE}"

    if [ $? -eq 0 ]; then
        FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
        echo -e "${GREEN}✅ 数据库备份成功: ${BACKUP_FILE} (${FILE_SIZE})${NC}"
        echo ""
        echo "📋 最近的备份文件:"
        ls -lh "${BACKUP_DIR}"/*.sql.gz 2>/dev/null | tail -5
    else
        echo -e "${RED}❌ 数据库备份失败${NC}"
        exit 1
    fi
}

# 函数：列出备份
list_backups() {
    BACKUP_DIR="backups"
    if [ -d "${BACKUP_DIR}" ]; then
        echo -e "${YELLOW}📋 可用的备份文件:${NC}"
        ls -lh "${BACKUP_DIR}"/*.sql.gz 2>/dev/null || echo "没有找到备份文件"
    else
        echo "没有备份目录，请先执行 ./deploy.sh backup"
    fi
}

# 函数：恢复数据库
restore() {
    if [ -z "$2" ]; then
        echo -e "${RED}❌ 请指定备份文件名${NC}"
        echo "用法: $0 restore <backup_file>"
        echo "示例: $0 restore backup_20240115_120000.sql.gz"
        exit 1
    fi

    load_env
    BACKUP_FILE="$2"
    BACKUP_DIR="backups"

    if [ ! -f "${BACKUP_DIR}/${BACKUP_FILE}" ]; then
        echo -e "${RED}❌ 备份文件不存在: ${BACKUP_DIR}/${BACKUP_FILE}${NC}"
        exit 1
    fi

    echo -e "${YELLOW}⚠️  即将恢复数据库...${NC}"
    echo "备份文件: ${BACKUP_DIR}/${BACKUP_FILE}"
    echo ""
    read -p "确认恢复? 这将覆盖现有数据 (y/N): " confirm
    if [ "${confirm}" != "y" ] && [ "${confirm}" != "Y" ]; then
        echo "已取消"
        exit 0
    fi

    echo -e "${YELLOW}🔄 正在恢复数据库...${NC}"

    $COMPOSE_CMD stop backend
    gunzip -c "${BACKUP_DIR}/${BACKUP_FILE}" | docker exec -i my-little-app-mysql mysql -u root -p"${DATABASE_PASSWORD}"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 数据库恢复成功${NC}"
        $COMPOSE_CMD start backend
        echo -e "${YELLOW}🚀 已重启后端服务${NC}"
    else
        echo -e "${RED}❌ 数据库恢复失败${NC}"
        $COMPOSE_CMD start backend
        exit 1
    fi
}

# ==================== 本地调试功能（来自 debug.sh） ====================

# 检查依赖
check_dependencies() {
    echo -e "${YELLOW}检查本地开发依赖...${NC}"

    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ 未找到 Java，请安装 JDK 17+${NC}"
        exit 1
    fi

    if ! command -v node &> /dev/null; then
        echo -e "${RED}❌ 未找到 Node.js，请安装 Node.js 18+${NC}"
        exit 1
    fi

    if ! command -v pnpm &> /dev/null; then
        echo -e "${YELLOW}⚠️ 未找到 pnpm，尝试安装...${NC}"
        npm install -g pnpm
    fi

    echo -e "${GREEN}✅ 依赖检查完成${NC}"
}

# 安装依赖
install_deps() {
    load_env
    PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

    echo -e "${YELLOW}安装依赖...${NC}"

    # 后端依赖
    cd "$PROJECT_ROOT/my-little-app-backend"
    if [ ! -d ".mvn/wrapper" ]; then
        ./mvnw wrapper:wrapper -Dmaven=3.9.9
    fi
    ./mvnw install -DskipTests

    # H5 前端依赖
    cd "$PROJECT_ROOT/my-little-app-frontend"
    pnpm install --frozen-lockfile

    # 管理后台依赖
    cd "$PROJECT_ROOT/my-little-app-admin"
    pnpm install --frozen-lockfile

    echo -e "${GREEN}✅ 依赖安装完成${NC}"
}

# 启动后端
start_backend() {
    load_env
    cd "$(dirname "${BASH_SOURCE[0]}")/my-little-app-backend"
    echo -e "${GREEN}🚀 启动后端服务...${NC}"
    ./mvnw spring-boot:run
}

# 启动管理后台
start_admin() {
    cd "$(dirname "${BASH_SOURCE[0]}")/my-little-app-admin"
    echo -e "${GREEN}🚀 启动管理后台...${NC}"
    pnpm run dev
}

# 启动 H5 前端
start_frontend() {
    load_env
    cd "$(dirname "${BASH_SOURCE[0]}")/my-little-app-frontend"
    echo -e "${GREEN}🚀 启动 H5 前端...${NC}"
    pnpm run dev:h5
}

# 停止所有本地服务
stop_local() {
    echo -e "${YELLOW}🛑 停止所有本地服务...${NC}"
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "pnpm run dev" 2>/dev/null || true
    pkill -f "pnpm run dev:h5" 2>/dev/null || true
    echo -e "${GREEN}✅ 所有本地服务已停止${NC}"
}

# 检查数据库表结构
check_db_schema() {
    load_env
    PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

    echo -e "${YELLOW}检查数据库表结构...${NC}"

    # 从项目 .env 文件读取数据库配置
    DB_HOST="mysql"
    DB_PORT="3306"
    DB_NAME="my_little_app"
    DB_USER="root"
    DB_PASSWORD="${DATABASE_PASSWORD:-root123}"

    # 构建 MySQL 连接命令
    MYSQL_CMD="mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASSWORD $DB_NAME"

    # 检查表是否存在
    TABLES=("t_admin" "t_user" "t_thought" "t_image" "api_usage" "ai_config" "system_config")
    MISSING_TABLES=()

    for table in "${TABLES[@]}"; do
        if ! $MYSQL_CMD -e "SHOW TABLES LIKE '$table'" 2>/dev/null | grep -q "$table"; then
            MISSING_TABLES+=("$table")
        fi
    done

    if [ ${#MISSING_TABLES[@]} -gt 0 ]; then
        echo -e "${RED}❌ 缺少表: ${MISSING_TABLES[*]}${NC}"
        echo -e "${YELLOW}请确保 Docker 服务已运行，数据库已初始化${NC}"
        return 1
    fi

    echo -e "${GREEN}✅ 数据库表结构检查通过${NC}"
}

# 启动所有本地服务
start_all_local() {
    load_env
    check_dependencies
    check_db_schema

    PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

    echo -e "${GREEN}🚀 启动所有本地服务...${NC}"

    # 启动后端（后台）
    cd "$PROJECT_ROOT/my-little-app-backend"
    ./mvnw spring-boot:run &
    BACKEND_PID=$!

    echo -e "${YELLOW}等待后端启动...${NC}"
    sleep 10

    # 启动管理后台（后台）
    cd "$PROJECT_ROOT/my-little-app-admin"
    pnpm run dev &
    ADMIN_PID=$!

    # 启动 H5 前端（后台）
    cd "$PROJECT_ROOT/my-little-app-frontend"
    pnpm run dev:h5 &
    FRONTEND_PID=$!

    echo -e "${GREEN}✅ 所有服务已启动${NC}"
    echo ""
    echo "📋 服务访问地址:"
    echo "  H5 前端:    http://localhost:5173"
    echo "  管理后台:   http://localhost:5174"
    echo "  后端 API:   http://localhost:${PORT_BACKEND:-8080}"
    echo ""
    echo "📋 进程 PID:"
    echo "  后端:       $BACKEND_PID"
    echo "  管理后台:   $ADMIN_PID"
    echo "  H5 前端:    $FRONTEND_PID"
    echo ""
    echo -e "按 ${CYAN}Ctrl+C${NC} 停止所有服务"

    wait
}

# 函数：交互式菜单
interactive_menu() {
    clear
    echo -e "${GREEN}====================================${NC}"
    echo -e "${GREEN}      🚀 My Little App 部署菜单      ${NC}"
    echo -e "${GREEN}====================================${NC}"
    echo ""

    echo -e "${BLUE}📦 Docker 部署:${NC}"
    echo -e "  ${CYAN}1)${NC} 构建并启动服务 (build)"
    echo -e "  ${CYAN}2)${NC} 更新代码并重新部署 (update)"
    echo -e "  ${CYAN}3)${NC} 重启服务 (restart)"
    echo -e "  ${CYAN}4)${NC} 停止服务 (stop)"
    echo -e "  ${CYAN}5)${NC} 查看服务日志 (logs)"
    echo -e "  ${CYAN}6)${NC} 清理所有资源 (clean)"
    echo ""

    echo -e "${BLUE}💾 数据库操作:${NC}"
    echo -e "  ${CYAN}7)${NC} 备份数据库 (backup)"
    echo -e "  ${CYAN}8)${NC} 恢复数据库 (restore)"
    echo -e "  ${CYAN}9)${NC} 列出备份文件 (list-backups)"
    echo ""

    echo -e "${BLUE}🔧 本地开发 (调试模式):${NC}"
    echo -e "  ${CYAN}a)${NC} 启动所有本地服务 (start-local)"
    echo -e "  ${CYAN}b)${NC} 只启动后端 (start-backend)"
    echo -e "  ${CYAN}c)${NC} 只启动管理后台 (start-admin)"
    echo -e "  ${CYAN}d)${NC} 只启动 H5 前端 (start-frontend)"
    echo -e "  ${CYAN}e)${NC} 停止所有本地服务 (stop-local)"
    echo -e "  ${CYAN}f)${NC} 检查数据库表结构 (check-db)"
    echo -e "  ${CYAN}g)${NC} 安装本地依赖 (install-deps)"
    echo ""

    echo -e "${BLUE}🔧 配置:${NC}"
    echo -e "  ${CYAN}p)${NC} 配置端口 (ports)"
    echo -e "  ${CYAN}t)${NC} 检查端口占用 (check-ports)"
    echo ""

    echo -e "${GREEN}====================================${NC}"
    echo -e "请选择操作: ${NC}"
    read -r choice

    case "$choice" in
        1) build ;;
        2) update ;;
        3) restart ;;
        4) stop ;;
        5)
            echo -e "${YELLOW}请指定服务名 (直接回车查看所有): ${NC}"
            read -r service
            logs "" "${service}"
            ;;
        6)
            echo -e "${RED}⚠️  警告：此操作将删除所有数据卷！${NC}"
            read -p "确认清理? (y/N): " confirm
            if [ "${confirm}" = "y" ] || [ "${confirm}" = "Y" ]; then
                clean
            else
                echo "已取消"
            fi
            ;;
        7) backup ;;
        8)
            echo -e "${YELLOW}请输入备份文件名: ${NC}"
            read -r backup_file
            restore "" "${backup_file}"
            ;;
        9) list_backups ;;
        a|A) start_all_local ;;
        b|B) start_backend ;;
        c|C) start_admin ;;
        d|D) start_frontend ;;
        e|E) stop_local ;;
        f|F) check_db_schema ;;
        g|G) install_deps ;;
        p|P) configure_ports ;;
        t|T) check_all_ports ;;
        q|Q)
            echo -e "${GREEN}👋 再见！${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}无效选择: $choice${NC}"
            sleep 1
            interactive_menu
            ;;
    esac

    echo ""
    echo -e "${YELLOW}按回车键返回菜单...${NC}"
    read -r
    interactive_menu
}

# 主逻辑
case "${1:-interactive}" in
    interactive|i)
        interactive_menu
        ;;
    build)
        build
        ;;
    update)
        update
        ;;
    restart)
        restart
        ;;
    stop)
        stop
        ;;
    logs)
        logs "$@"
        ;;
    clean)
        clean
        ;;
    backup)
        backup
        ;;
    restore)
        restore "$@"
        ;;
    list-backups|list)
        list_backups
        ;;
    start)
        start
        ;;
    ports|port|p)
        configure_ports
        ;;
    check-ports|check|t)
        check_all_ports
        ;;
    # 本地调试命令
    start-local|a)
        start_all_local
        ;;
    start-backend|b)
        start_backend
        ;;
    start-admin|c)
        start_admin
        ;;
    start-frontend|d)
        start_frontend
        ;;
    stop-local|e)
        stop_local
        ;;
    check-db|f)
        check_db_schema
        ;;
    install-deps|g)
        install_deps
        ;;
    *)
        echo -e "${YELLOW}用法: $0 [命令]${NC}"
        echo ""
        echo "📋 直接执行 ./deploy.sh 进入交互式菜单"
        echo ""
        echo "📦 Docker 部署命令:"
        echo "  build          - 构建镜像并启动"
        echo "  update         - 更新代码并重新部署"
        echo "  restart        - 重启服务"
        echo "  stop           - 停止服务"
        echo "  logs           - 查看日志 (可指定服务名)"
        echo "  clean          - 清理所有资源（包括数据卷）"
        echo ""
        echo "💾 数据库备份/恢复:"
        echo "  backup         - 备份数据库到 backups/ 目录"
        echo "  restore        - 从备份恢复数据库"
        echo "  list-backups   - 列出可用的备份文件"
        echo ""
        echo "🔧 本地开发命令:"
        echo "  start-local    - 启动所有本地服务（后端+前端）"
        echo "  start-backend  - 只启动后端服务"
        echo "  start-admin    - 只启动管理后台"
        echo "  start-frontend - 只启动 H5 前端"
        echo "  stop-local     - 停止所有本地服务"
        echo "  check-db       - 检查数据库表结构"
        echo "  install-deps   - 安装本地依赖"
        echo ""
        echo "🔧 配置命令:"
        echo "  ports|port|p    - 交互式配置端口"
        echo "  check-ports|t   - 检查端口占用情况"
        echo ""
        echo "💡 示例:"
        echo "  ./deploy.sh                    # 进入交互式菜单"
        echo "  ./deploy.sh build              # 构建并启动"
        echo "  ./deploy.sh p                  # 配置端口"
        echo "  ./deploy.sh start-local        # 启动本地开发环境"
        echo "  ./deploy.sh backup             # 备份数据库"
        exit 1
        ;;
esac

@echo off
chcp 65001 >nul
echo ==========================================
echo    秒杀系统本地开发环境启动脚本
echo ==========================================
echo.

REM 检查Java环境
echo [1/3] 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Java环境，请先安装JDK 17或更高版本
    pause
    exit /b 1
)
echo [OK] Java环境正常
echo.

REM 检查Maven环境
echo [2/3] 检查Maven环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Maven环境，请先安装Maven
    pause
    exit /b 1
)
echo [OK] Maven环境正常
echo.

REM 编译项目
echo [3/3] 编译并启动后端服务...
echo 使用本地配置启动（application-local.properties）
echo.

REM 使用本地配置启动
call mvn clean spring-boot:run -Dspring-boot.run.profiles=local

if errorlevel 1 (
    echo.
    echo [错误] 启动失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo 服务已停止
pause

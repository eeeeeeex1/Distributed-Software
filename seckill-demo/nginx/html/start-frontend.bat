@echo off
chcp 65001 >nul
echo ==========================================
echo    前端本地测试服务器启动脚本
echo ==========================================
echo.

cd /d "%~dp0"

echo 正在启动前端本地服务器...
echo 访问地址: http://localhost:3000
echo.
echo 按Ctrl+C停止服务器
echo.

REM 使用Python启动简易HTTP服务器
python -m http.server 3000

if errorlevel 1 (
    echo.
    echo [错误] 启动失败，请确保已安装Python
    echo 或者您可以手动使用其他方式启动静态文件服务器
    pause
)

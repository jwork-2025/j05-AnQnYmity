@echo off
setlocal enabledelayedexpansion

rem 创建编译目录
if not exist "build\classes" mkdir "build\classes"

rem 设置类路径
set "LWJGL_CP=."
if exist "lib\lwjgl" (
  set "LWJGL_CP=.;lib\lwjgl\*"
)

rem 查找所有 Java 源文件
set "SOURCES="
for /r "src\main\java" %%f in (*.java) do (
  set "SOURCES=!SOURCES! %%f"
)

rem 编译
echo Compiling Java sources...
javac -d build\classes -cp "%LWJGL_CP%" %SOURCES%
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    exit /b %ERRORLEVEL%
)

rem 设置运行类路径
set "CLASSPATH=build\classes"
if exist "lib\lwjgl" (
  set "CLASSPATH=!CLASSPATH!;lib\lwjgl\*"
)

rem 尝试定位 natives 目录
set "NATIVES_PATH=lib\lwjgl\native"
if not exist "!NATIVES_PATH!" (
    rem 尝试 run.bat 中使用的结构
    set "NATIVES_PATH=lib\lwjgl\natives\windows-x86_64"
)

rem 运行 HuluGame
echo Running HuluGame...
echo Natives path: !NATIVES_PATH!
java -cp "%CLASSPATH%" -Djava.library.path="!NATIVES_PATH!" com.gameengine.hulu.HuluGame

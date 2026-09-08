@echo off 
set "currentTime=%date:~0,4%-%date:~5,2%-%date:~8,2% %time:~0,2%:%time:~3,2%:%time:~6,2%" 
echo [%currentTime%] start run_end.bat
:: 检查是否以管理员身份运行 
:: net session >nul 2>&1 
:: if %errorLevel% == 0 ( 
::     echo [%currentTime%] start install VC_redist.x64... 
:: ) else ( 
::     echo [%currentTime%] Please run this batch file as an administrator. 
::     exit /b 
:: ) 

:: 这里需要替换为你实际的VC_redist.x64.exe 文件路径 
set "installerPath=%1\tools\VC_redist.x64.exe"  
 
:: 检查安装包是否存在 
if exist "%installerPath%" ( 
    echo [%currentTime%] has package start install... 
    "%installerPath%" /install /quiet /norestart 
    if %errorLevel% == 0 ( 
        echo [%currentTime%] VC_dedist.x64 installation successful.
    ) else ( 
        echo [%currentTime%] VC_dedist.x64 installation failed, error code: %errorLevel%.
    ) 
) else ( 
    echo [%currentTime%] Installation package not found, please check the path:%installerPath%.
) 

:: 删除 initconfig.json 文件
set "initConfigPath=%1\initconfig.json"
if exist "%initConfigPath%" (
    echo [%currentTime%] Found initconfig.json, deleting...
    del /f /q "%initConfigPath%"
    if %errorLevel% == 0 (
        echo [%currentTime%] initconfig.json deleted successfully.
    ) else (
        echo [%currentTime%] initconfig.json deletion failed, error code: %errorLevel%.
    )
) else (
    echo [%currentTime%] initconfig.json not found, skip deletion.
)

echo --------------------------------------------
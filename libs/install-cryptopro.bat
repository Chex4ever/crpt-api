@echo off
setlocal enabledelayedexpansion
echo ON
echo ========================================
echo CryptoPro Libraries Installer
echo ========================================

echo WARNING: This script uses hardcoded versions
echo.

set "success_count=0"
set "total_count=0"

REM Установка CryptoPro библиотек с версией 2.0.48378_A
for %%f in (AdES-core.jar ASN1P.jar asn1rt.jar CAdES.jar cpSSL.jar JCP.jar JCPRevCheck.jar JCPRevTools.jar sspiSSL.jar) do (
    if exist "%%f" (
		call :install_library "%%f" "ru.CryptoPro" "%%~nf" "2.0.48378_A"
    )
)
REM Установка asn1rt.jar с версией 1.4.2_19
for %%f in (asn1rt.jar) do (
    if exist "%%f" (
		call :install_library "%%f" "com.objsys" "%%~nf" "1.4.2_19"
    )
)

REM Установка JCSP.jar с версией 5.0.48378_A
for %%f in (JCSP.jar) do (
    if exist "%%f" (
		call :install_library "%%f" "ru.CryptoPro" "%%~nf" "5.0.48378_A"
    )
)

echo.
echo ========================================
echo Installation complete!
echo Total processed: !total_count! files
echo Successfully installed: !success_count! files
echo ========================================
pause
exit /b

:install_library
set "jar_file=%~1"
set "group_id=%~2"
set "artifact_id=%~3"
set "version=%~4"

set /a "total_count+=1"
echo [!total_count!] Processing: %jar_file%
echo   File: "%jar_file%"
echo   ArtifactId: %artifact_id%
echo   GroupId: %group_id%
echo   Version: %version%

call mvn install:install-file ^
    -Dfile="%jar_file%" ^
    -DgroupId="%group_id%" ^
    -DartifactId="%artifact_id%" ^
    -Dversion="%version%" ^
    -Dpackaging=jar

if !errorlevel! equ 0 (
    echo   SUCCESS: Installed
    set /a "success_count+=1"
) else (
    echo   ERROR: Failed
)
echo.
exit /b
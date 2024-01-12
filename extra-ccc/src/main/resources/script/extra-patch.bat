@echo off
pushd "%~dp0"
if "%~1"=="" goto :NOISO

set FLAGS=
echo Press Y (Yes) or N (No) to confirm patch options
CHOICE /C YN /M "Change Nero's Praetor to Maestro"
if %ERRORLEVEL% == 1 set "FLAGS=%FLAGS% --useMaestro"
CHOICE /C YN /M "Use low-res ruby font (use when playing on a PSP)"
if %ERRORLEVEL% == 1 set "FLAGS=%FLAGS% --useLowResRuby"
echo.

echo Patching ISO, please wait...
fine.exe extra.patchfs "%~1" extra-patched.iso %FLAGS% > NUL 2>&1
if errorlevel 1 goto :FINEERR

echo.
echo Success!
echo Patched ISO was saved as extra-patched.iso next to the bat file.
goto :FIN

:NOISO
echo To patch ISO don't run this bat file.
echo Simply drag and drop ISO on it and the patch process will start.
goto :FIN

:FINEERR
echo.
echo Something went wrong while patching files.
echo You might be trying to patch wrong ISO, try using a different one.
echo Look at patcher-logs.txt file for more details.
goto :FIN

:FIN
popd
echo You can now close this window.
pause >nul
exit

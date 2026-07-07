@echo off
setlocal EnableDelayedExpansion

set "PROJECT_ROOT=%~dp0"
set "SOURCE_DIR=%PROJECT_ROOT%java\src"
set "OUTPUT_DIR=%PROJECT_ROOT%java\out"
set "CORRETTO_BIN=C:\Program Files\Amazon Corretto\jdk21.0.6_7\bin"

echo Blockforge Java
echo Cartella progetto: %PROJECT_ROOT%
echo.

where javac >nul 2>nul
if %errorlevel%==0 (
  set "JAVAC=javac"
) else (
  if exist "%CORRETTO_BIN%\javac.exe" (
    set "JAVAC=%CORRETTO_BIN%\javac.exe"
  ) else (
    echo javac non trovato.
    pause
    exit /b 1
  )
)

where java >nul 2>nul
if %errorlevel%==0 (
  set "JAVA=java"
) else (
  if exist "%CORRETTO_BIN%\java.exe" (
    set "JAVA=%CORRETTO_BIN%\java.exe"
  ) else (
    echo java non trovato.
    pause
    exit /b 1
  )
)

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

set "SOURCES="
for /r "%SOURCE_DIR%" %%F in (*.java) do (
  set "SOURCES=!SOURCES! "%%~fF""
)

if "!SOURCES!"=="" (
  echo Nessun file Java trovato in %SOURCE_DIR%
  pause
  exit /b 1
)

echo Compilo i sorgenti Java...
call "%JAVAC%" -d "%OUTPUT_DIR%" !SOURCES!
if errorlevel 1 (
  echo.
  echo Compilazione fallita.
  pause
  exit /b 1
)

echo Compilazione completata.
echo Avvio la finestra del gioco...
echo.
"%JAVA%" -cp "%OUTPUT_DIR%" blockforge.Main

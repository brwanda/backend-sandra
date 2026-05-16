@echo off
REM =====================================================================
REM Batch Script to Run HOD Migration
REM =====================================================================
REM This script runs the PowerShell migration script
REM =====================================================================

echo ========================================
echo HOD MIGRATION SCRIPT
echo ========================================
echo.
echo This will run the HOD migration to move Head Of Delegation
echo from sub_committee table to committee table.
echo.
echo Press any key to continue or Ctrl+C to cancel...
pause >nul

echo.
echo Running PowerShell migration script...
echo.

powershell.exe -ExecutionPolicy Bypass -File "%~dp0run_hod_migration.ps1"

echo.
echo ========================================
echo Script execution completed
echo ========================================
echo.
pause

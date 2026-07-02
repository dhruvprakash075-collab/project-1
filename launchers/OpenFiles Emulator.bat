@echo off
set ANDROID_AVD_HOME=D:\android-avd
set ANDROID_USER_HOME=D:\android-user-home
set ANDROID_HOME=D:\android-sdk
set ANDROID_SDK_ROOT=D:\android-sdk
start "OpenFiles Emulator" "D:\android-sdk\emulator\emulator.exe" -avd OpenFilesTest -no-snapshot-load -no-snapshot-save -scale 0.35 -no-boot-anim

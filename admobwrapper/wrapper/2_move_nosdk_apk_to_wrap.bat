d:
cd D:\work\wrapper\SampleProjectForWrap
call gradlew.bat clean build

move app\build\outputs\apk\app-debug.apk D:\work\wrapper\wrapper\wrapper\app-debug.apk
start D:\work\wrapper\wrapper\wrapper

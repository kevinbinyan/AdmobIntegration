cd C:\workspace\proxy
call gradlew.bat clean build

cd C:\workspace\proxy\app\build\outputs\aar
7z e app-debug.aar -aoa
move /y C:\workspace\proxy\app\build\outputs\aar\classes.jar C:\workspace\wrapper\wrapper\resources\android\wrapper.mg.jar
start C:\workspace\wrapper\wrapper\resources\android\
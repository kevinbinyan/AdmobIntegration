rem 请核对你的texturepacker安装路径
@echo off
 
path %path%;"D:\CodeAndWeb\TexturePacker\bin"
 
for /f "usebackq tokens=*" %%d in (`dir /s /b *.png`) do (
TexturePacker.exe "%%d" --sheet "%%~dpnd.pvr" --data "%%~dpnd.plist" --opt PVRTC4 --allow-free-size --algorithm Basic --no-trim --dither-fs
)
 
pause
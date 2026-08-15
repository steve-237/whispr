Add-Type -AssemblyName System.Drawing
$bitmap = New-Object System.Drawing.Bitmap(512, 512)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::White)
$font = New-Object System.Drawing.Font("Segoe UI Emoji", 250)
$brush = [System.Drawing.Brushes]::Black
$graphics.DrawString("??", $font, $brush, 50, 50)
$bitmap.Save("frontend\public\icons\shush.png", [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()

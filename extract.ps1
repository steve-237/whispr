$text = Get-Content -Path "cahier_des_charges.txt" -Raw -Encoding UTF8
if ($text -match '(?s)(11\.\s+Mod.*?)(12\.\s+API)') {
    Set-Content -Path "section11.txt" -Value $matches[1] -Encoding UTF8
}

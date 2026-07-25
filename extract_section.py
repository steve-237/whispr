import re

with open("cahier_des_charges.txt", "r", encoding="utf-8", errors="ignore") as f:
    text = f.read()

match = re.search(r'(?s)11\.\s+Mod.*?12\.\s+API', text)
if match:
    print(match.group(0))
else:
    print("Section not found.")

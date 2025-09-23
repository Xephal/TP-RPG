Minimal Java project for the Design Patterns TP

Build & run (PowerShell / javac + java):

From project root:

```powershell
javac -d out $(Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
java -cp out rpg.main.Main
```

or

```powershell
Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object { $_.FullName } | %{ javac -d out $_ }
```

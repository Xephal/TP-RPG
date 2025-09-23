Minimal Java project for the Design Patterns TP

Build & run (PowerShell / javac + java):

From project root:

Compile:

```powershell
$files = Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }; javac -d out $files; if ($LASTEXITCODE -eq 0) { echo 'COMPILE_OK' } else { echo 'COMPILE_FAIL' }
```

```powershell
# Recommended: compile first, then run.
javac -d out $(Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })

# Run the demo / CLI. The program will display the demo and then ask whether to launch the GUI.
java -cp out rpg.main.Main
```

Custom creation note:

Custom character creation is now GUI-only (WIP). To create, decorate and save custom characters, launch the GUI:

```powershell
java -cp out rpg.main.Main gui
```

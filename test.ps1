$ErrorActionPreference = 'Stop'
python (Join-Path $PSScriptRoot 'tools/test.py')
if ($LASTEXITCODE -ne 0) { throw 'JVM checks failed' }

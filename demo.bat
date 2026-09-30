@echo off
REM ---------------------------------------------------------------------
REM demo.bat -- demonstracao OFFLINE no Windows (sem make, sem internet).
REM Usa o scanner ja gerado em gen\. Requer JDK 17+ no PATH.
REM ---------------------------------------------------------------------
chcp 65001 > nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\main\java\br\edu\cesupa\tucupi\*.java src\test\java\br\edu\cesupa\tucupi\*.java gen\br\edu\cesupa\tucupi\*.java || goto :erro
echo.
echo ############ CASO ACEITO: exemplos\04_sintatico_valido.tcp ############
java -cp out br.edu.cesupa.tucupi.MainSintatico --tokens exemplos\04_sintatico_valido.tcp
echo.
echo ############ CASO REJEITADO: exemplos\05_sintatico_erro.tcp ############
java -cp out br.edu.cesupa.tucupi.MainSintatico --tokens exemplos\05_sintatico_erro.tcp
echo.
echo ############ TESTES ############
java -cp out br.edu.cesupa.tucupi.TestesSintaticos testes\sintatico
goto :fim
:erro
echo Falha na compilacao. Verifique se o JDK 17+ esta instalado.
:fim
pause

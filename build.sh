#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Build sem make.
#
#   ./build.sh             OFFLINE: compila usando o scanner ja gerado em gen/,
#                          roda os testes do parser e a demonstracao
#                          (caso aceito + caso rejeitado)
#   ./build.sh --regerar   baixa o JFlex (internet), regenera o scanner a
#                          partir de tucupi.flex e depois faz o mesmo que acima
#
# Requisitos: JDK 17+ (testado com OpenJDK 21). curl e tar so para --regerar.
# ---------------------------------------------------------------------------
set -euo pipefail

JFLEX_VERSAO="1.9.1"
JFLEX_JAR="ferramentas/jflex-full-${JFLEX_VERSAO}.jar"
JFLEX_URL="https://github.com/jflex-de/jflex/releases/download/v${JFLEX_VERSAO}/jflex-${JFLEX_VERSAO}.tar.gz"
PACOTE_DIR="br/edu/cesupa/tucupi"
PKG="br.edu.cesupa.tucupi"

cd "$(dirname "$0")"

if [ "${1:-}" = "--regerar" ]; then
  if [ ! -f "$JFLEX_JAR" ]; then
    echo ">> baixando JFlex ${JFLEX_VERSAO}"
    mkdir -p ferramentas
    curl -sSL -o ferramentas/jflex.tar.gz "$JFLEX_URL"
    tar xzf ferramentas/jflex.tar.gz -C ferramentas \
        "jflex-${JFLEX_VERSAO}/lib/jflex-full-${JFLEX_VERSAO}.jar" --strip-components=2
    rm -f ferramentas/jflex.tar.gz
  fi
  echo ">> gerando o analisador lexico a partir de src/main/jflex/${PACOTE_DIR}/tucupi.flex"
  mkdir -p "gen/${PACOTE_DIR}"
  java -jar "$JFLEX_JAR" --nobak -d "gen/${PACOTE_DIR}" "src/main/jflex/${PACOTE_DIR}/tucupi.flex"
fi

if [ ! -f "gen/${PACOTE_DIR}/TucupiLexer.java" ]; then
  echo "!! gen/${PACOTE_DIR}/TucupiLexer.java ausente. Rode: ./build.sh --regerar" >&2
  exit 1
fi

echo ">> compilando"
mkdir -p out
# shellcheck disable=SC2046
javac -encoding UTF-8 -d out $(find src/main/java src/test/java gen -name '*.java')

echo ">> testes do analisador sintatico"
java -cp out "${PKG}.TestesSintaticos" testes/sintatico || true

for arquivo in exemplos/04_sintatico_valido.tcp exemplos/05_sintatico_erro.tcp; do
  echo ""
  echo "############ ${arquivo} ############"
  java -cp out "${PKG}.MainSintatico" --tokens "$arquivo" || true
done

#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Build sem make: baixa o JFlex, gera o scanner, compila e roda os exemplos.
# Uso: ./build.sh
# Requisitos: JDK 8+ (testado com JDK 21), curl e tar.
# ---------------------------------------------------------------------------
set -euo pipefail

JFLEX_VERSAO="1.9.1"
JFLEX_JAR="ferramentas/jflex-full-${JFLEX_VERSAO}.jar"
JFLEX_URL="https://github.com/jflex-de/jflex/releases/download/v${JFLEX_VERSAO}/jflex-${JFLEX_VERSAO}.tar.gz"
PACOTE_DIR="br/edu/cesupa/tucupi"

cd "$(dirname "$0")"

if [ ! -f "$JFLEX_JAR" ]; then
  echo ">> baixando JFlex ${JFLEX_VERSAO}"
  mkdir -p ferramentas
  curl -sSL -o ferramentas/jflex.tar.gz "$JFLEX_URL"
  tar xzf ferramentas/jflex.tar.gz -C ferramentas \
      "jflex-${JFLEX_VERSAO}/lib/jflex-full-${JFLEX_VERSAO}.jar" --strip-components=2
  rm -f ferramentas/jflex.tar.gz
fi

echo ">> gerando o analisador a partir de src/main/jflex/br/edu/cesupa/tucupi/tucupi.flex"
mkdir -p "gen/${PACOTE_DIR}"
java -jar "$JFLEX_JAR" --nobak -d "gen/${PACOTE_DIR}" src/main/jflex/br/edu/cesupa/tucupi/tucupi.flex

echo ">> compilando"
mkdir -p out
# shellcheck disable=SC2046
javac -encoding UTF-8 -d out $(find src/main/java gen -name '*.java')

echo ">> executando os exemplos"
for arquivo in exemplos/*.tcp; do
  echo ""
  echo "############ ${arquivo} ############"
  java -cp out br.edu.cesupa.tucupi.Main "$arquivo" || true
done

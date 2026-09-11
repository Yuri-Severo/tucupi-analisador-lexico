# ---------------------------------------------------------------------------
# Analisador lexico Tucupi -- geracao com JFlex + compilacao com javac
#
#   make            gera o scanner, compila e roda os tres exemplos
#   make gerar      roda apenas o JFlex sobre src/main/jflex/br/edu/cesupa/tucupi/tucupi.flex
#   make compilar   compila o codigo gerado + o codigo de apoio
#   make exemplos   executa o analisador sobre os arquivos de exemplos/
#   make saidas     regrava docs/saidas/*.txt com as saidas atuais
#   make bench      mede a vazao do analisador
#   make limpar     remove artefatos gerados
# ---------------------------------------------------------------------------

JFLEX_VERSAO = 1.9.1
JFLEX_JAR    = ferramentas/jflex-full-$(JFLEX_VERSAO).jar
JFLEX_URL    = https://github.com/jflex-de/jflex/releases/download/v$(JFLEX_VERSAO)/jflex-$(JFLEX_VERSAO).tar.gz

PACOTE_DIR   = br/edu/cesupa/tucupi
FONTES_JAVA  = $(shell find src/main/java gen -name '*.java' 2>/dev/null)

.PHONY: all gerar compilar exemplos saidas bench limpar

all: exemplos

# --- 1. obtem o JFlex (jar "full", ja com as dependencias embutidas) --------
$(JFLEX_JAR):
	@mkdir -p ferramentas
	@echo ">> baixando JFlex $(JFLEX_VERSAO)"
	@curl -sSL -o ferramentas/jflex.tar.gz "$(JFLEX_URL)"
	@tar xzf ferramentas/jflex.tar.gz -C ferramentas \
		jflex-$(JFLEX_VERSAO)/lib/jflex-full-$(JFLEX_VERSAO).jar --strip-components=2
	@rm -f ferramentas/jflex.tar.gz
	@echo ">> JFlex pronto em $(JFLEX_JAR)"

# --- 2. gera TucupiLexer.java a partir da especificacao ---------------------
gerar: $(JFLEX_JAR)
	@mkdir -p gen/$(PACOTE_DIR)
	java -jar $(JFLEX_JAR) --nobak -d gen/$(PACOTE_DIR) src/main/jflex/br/edu/cesupa/tucupi/tucupi.flex

# --- 3. compila -------------------------------------------------------------
compilar: gerar
	@mkdir -p out
	javac -encoding UTF-8 -d out $(shell find src/main/java gen -name '*.java')

# --- 4. executa os exemplos -------------------------------------------------
exemplos: compilar
	@echo ""
	@echo "############ 01_valido.tcp ############"
	@java -cp out br.edu.cesupa.tucupi.Main exemplos/01_valido.tcp || true
	@echo ""
	@echo "############ 02_erros.tcp ############"
	@java -cp out br.edu.cesupa.tucupi.Main --sem-tabela exemplos/02_erros.tcp || true
	@echo ""
	@echo "############ 03_unicode.tcp ############"
	@java -cp out br.edu.cesupa.tucupi.Main exemplos/03_unicode.tcp || true

saidas: compilar
	@mkdir -p docs/saidas
	@java -cp out br.edu.cesupa.tucupi.Main exemplos/01_valido.tcp  > docs/saidas/01_valido.txt  || true
	@java -cp out br.edu.cesupa.tucupi.Main exemplos/02_erros.tcp   > docs/saidas/02_erros.txt   || true
	@java -cp out br.edu.cesupa.tucupi.Main exemplos/03_unicode.tcp > docs/saidas/03_unicode.txt || true
	@java -cp out br.edu.cesupa.tucupi.Main --json exemplos/03_unicode.tcp > docs/saidas/03_unicode.json || true
	@java -cp out br.edu.cesupa.tucupi.Main --bench 20000 > docs/saidas/benchmark.txt || true
	@echo ">> saidas regravadas em docs/saidas/"

bench: compilar
	@java -cp out br.edu.cesupa.tucupi.Main --bench 20000

limpar:
	rm -rf out gen

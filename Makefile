# ---------------------------------------------------------------------------
# Tucupi -- analisador lexico (JFlex) + analisador sintatico (descida recursiva)
#
# Alvos que funcionam OFFLINE (usam o TucupiLexer.java ja versionado em gen/):
#   make            = make demo
#   make demo       compila e roda o parser no caso aceito e no rejeitado
#   make compilar   compila src/main/java + gen/ (+ testes) em out/
#   make tabela     tabela de lexemas e tokens (etapa lexica) do exemplo da demo
#   make sintatico  roda o parser sobre todos os exemplos/*.tcp
#   make exemplos   roda o analisador LEXICO sobre os exemplos 01..03
#   make testes     executa os testes do parser (testes/sintatico/)
#   make saidas     regrava docs/saidas/*.txt com as saidas atuais
#   make bench      mede a vazao do analisador lexico
#   make limpar     remove apenas out/ (NAO apaga o scanner gerado)
#
# Alvos que precisam de internet na primeira vez (baixam o JFlex):
#   make gerar      regenera gen/.../TucupiLexer.java a partir do tucupi.flex
#   make completo   gerar + compilar + exemplos + demo
#   make limpar-tudo  remove out/, gen/ e ferramentas/ (exige "make gerar" depois)
# ---------------------------------------------------------------------------

JFLEX_VERSAO = 1.9.1
JFLEX_JAR    = ferramentas/jflex-full-$(JFLEX_VERSAO).jar
JFLEX_URL    = https://github.com/jflex-de/jflex/releases/download/v$(JFLEX_VERSAO)/jflex-$(JFLEX_VERSAO).tar.gz

PACOTE_DIR   = br/edu/cesupa/tucupi
LEXER_GERADO = gen/$(PACOTE_DIR)/TucupiLexer.java
PKG          = br.edu.cesupa.tucupi

.PHONY: all demo tabela compilar sintatico exemplos testes saidas bench gerar completo limpar limpar-tudo

all: demo

# --- JFlex (somente para regenerar o scanner) ------------------------------
$(JFLEX_JAR):
	@mkdir -p ferramentas
	@echo ">> baixando JFlex $(JFLEX_VERSAO)"
	@curl -sSL -o ferramentas/jflex.tar.gz "$(JFLEX_URL)"
	@tar xzf ferramentas/jflex.tar.gz -C ferramentas \
		jflex-$(JFLEX_VERSAO)/lib/jflex-full-$(JFLEX_VERSAO).jar --strip-components=2
	@rm -f ferramentas/jflex.tar.gz
	@echo ">> JFlex pronto em $(JFLEX_JAR)"

gerar: $(JFLEX_JAR)
	@mkdir -p gen/$(PACOTE_DIR)
	java -jar $(JFLEX_JAR) --nobak -d gen/$(PACOTE_DIR) src/main/jflex/$(PACOTE_DIR)/tucupi.flex

# --- compilacao (offline) --------------------------------------------------
compilar:
	@test -f $(LEXER_GERADO) || { echo "!! $(LEXER_GERADO) ausente: rode 'make gerar' (precisa de internet)"; exit 1; }
	@mkdir -p out
	javac -encoding UTF-8 -d out $(shell find src/main/java src/test/java gen -name '*.java')

# --- demonstracao da etapa sintatica ---------------------------------------
demo: compilar
	@echo ""
	@echo "############ CASO ACEITO: exemplos/04_sintatico_valido.tcp ############"
	@java -cp out $(PKG).MainSintatico --tokens exemplos/04_sintatico_valido.tcp || true
	@echo ""
	@echo "############ CASO REJEITADO: exemplos/05_sintatico_erro.tcp ############"
	@java -cp out $(PKG).MainSintatico --tokens exemplos/05_sintatico_erro.tcp || true

tabela: compilar
	@java -cp out $(PKG).Main exemplos/04_sintatico_valido.tcp || true

sintatico: compilar
	@for f in exemplos/*.tcp; do \
		echo ""; echo "############ $$f ############"; \
		java -cp out $(PKG).MainSintatico "$$f" || true; \
	done

# --- etapa lexica (preservada) ---------------------------------------------
exemplos: compilar
	@echo ""
	@echo "############ 01_valido.tcp ############"
	@java -cp out $(PKG).Main exemplos/01_valido.tcp || true
	@echo ""
	@echo "############ 02_erros.tcp ############"
	@java -cp out $(PKG).Main --sem-tabela exemplos/02_erros.tcp || true
	@echo ""
	@echo "############ 03_unicode.tcp ############"
	@java -cp out $(PKG).Main exemplos/03_unicode.tcp || true

testes: compilar
	@java -cp out $(PKG).TestesSintaticos testes/sintatico

saidas: compilar
	@mkdir -p docs/saidas
	@java -cp out $(PKG).Main exemplos/01_valido.tcp  > docs/saidas/01_valido.txt  || true
	@java -cp out $(PKG).Main exemplos/02_erros.tcp   > docs/saidas/02_erros.txt   || true
	@java -cp out $(PKG).Main exemplos/03_unicode.tcp > docs/saidas/03_unicode.txt || true
	@java -cp out $(PKG).Main --json exemplos/03_unicode.tcp > docs/saidas/03_unicode.json || true
	@java -cp out $(PKG).Main --bench 20000 > docs/saidas/benchmark.txt || true
	@java -cp out $(PKG).Main exemplos/04_sintatico_valido.tcp > docs/saidas/lexico_demo.txt || true
	@java -cp out $(PKG).MainSintatico --tokens exemplos/04_sintatico_valido.tcp > docs/saidas/sintatico_valido.txt || true
	@java -cp out $(PKG).MainSintatico --tokens exemplos/05_sintatico_erro.tcp   > docs/saidas/sintatico_erro.txt   || true
	@java -cp out $(PKG).TestesSintaticos testes/sintatico > docs/saidas/testes_sintaticos.txt || true
	@echo ">> saidas regravadas em docs/saidas/"

bench: compilar
	@java -cp out $(PKG).Main --bench 20000

completo: gerar compilar exemplos demo

limpar:
	rm -rf out

limpar-tudo:
	rm -rf out gen ferramentas

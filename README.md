# Analisador Léxico **Tucupi** — JFlex

> Trabalho 1 da disciplina **Autômatos e Compiladores** — Tópico 2: *Soluções de Lexers*
> Ferramenta escolhida: **JFlex 1.9.1** · Opção de apresentação: **(A) artigo + (B) implementação**
> Centro Universitário do Pará (CESUPA)

**Equipe:** _preencher_ · _preencher_ · _preencher_ · _preencher_

---

## 1. O que este repositório contém

Um analisador léxico completo para a **Tucupi**, uma mini-linguagem imperativa
com palavras reservadas em português, escrita como especificação `.flex` e
gerada automaticamente pelo **JFlex**.

O analisador não se limita a reconhecer palavras reservadas e identificadores.
Cada recurso implementado existe para demonstrar um mecanismo específico do
JFlex que uma implementação trivial não exercitaria:

| Recurso da linguagem | Mecanismo do JFlex demonstrado |
|---|---|
| Comentários de bloco **aninhados** `/* /* */ */` | Estado léxico exclusivo `%xstate` + contador de profundidade (aninhamento não é linguagem regular) |
| Literais de texto com escapes `\n \t \uXXXX` | Estado exclusivo `TEXTO` que acumula o valor já decodificado em um buffer |
| Operador de intervalo `1..10` | **`yypushback(2)`** para desfazer o casamento mais longo, que leria `1.` como literal real |
| Família `<` `<=` `<<` `<<=` | Princípio do **casamento mais longo** (*longest match*) |
| Palavras reservadas × identificadores | Desempate por **ordem das regras** na especificação |
| Identificadores acentuados (`vazão_média`, `σ_amostral`) | `%unicode` + classes `[:jletter:]` / `[:jletterdigit:]` |
| Bases numéricas `0xFF00`, `0b1010_1100`, `1_000_000` | Macros compostas e conversão no código de ação |
| Estouro de faixa em `99999999999999` | Validação semântica dentro da ação, usando `BigInteger` |
| **Recuperação de erros** (o analisador não aborta) | Regra coringa `[^]` + regras `<<EOF>>` por estado |
| Rastreio de linha e coluna | Diretivas `%line` e `%column` |
| Tabela de símbolos e estatísticas | Código de usuário embutido com `%{ ... %}` |

O arquivo de exemplo com erros dispara **10 erros léxicos distintos** e mesmo
assim a varredura chega até o fim do arquivo.

---

## 2. Estrutura do projeto

```
jflex-tucupi/
├── src/main/jflex/br/edu/cesupa/tucupi/
│   └── tucupi.flex            ← A ESPECIFICAÇÃO (o coração do trabalho)
├── src/main/java/br/edu/cesupa/tucupi/
│   ├── Token.java             ← tipo, lexema, valor semântico, linha, coluna
│   ├── TokenType.java         ← enumeração dos tokens, agrupados por categoria
│   ├── ErroLexico.java        ← erro registrado sem interromper a varredura
│   ├── TabelaDeSimbolos.java  ← identificadores, 1ª ocorrência e nº de usos
│   ├── Relatorio.java         ← tabelas de saída, estatísticas e JSON
│   └── Main.java              ← CLI e laço "pede um token por vez"
├── gen/br/edu/cesupa/tucupi/
│   └── TucupiLexer.java       ← GERADO pelo JFlex (versionado de propósito,
│                                 para rodar sem baixar a ferramenta)
├── exemplos/
│   ├── 01_valido.tcp          ← exercita todas as classes de token
│   ├── 02_erros.tcp           ← 10 erros léxicos diferentes
│   └── 03_unicode.tcp         ← identificadores e literais fora do ASCII
├── docs/saidas/               ← saídas reais das execuções (usadas nos slides)
├── Makefile · build.sh · pom.xml
└── README.md
```

---

## 3. Como executar

Pré-requisito: **JDK 8 ou superior** (testado com OpenJDK 21).

### Opção A — Makefile (recomendada)

```bash
make            # baixa o JFlex, gera, compila e roda os 3 exemplos
make gerar      # só gera TucupiLexer.java a partir de tucupi.flex
make bench      # mede a vazão do analisador
make limpar
```

### Opção B — script único

```bash
./build.sh
```

### Opção C — Maven

```bash
mvn -q compile
mvn -q exec:java -Dexec.mainClass=br.edu.cesupa.tucupi.Main \
                 -Dexec.args="exemplos/01_valido.tcp"
```

### Opção D — sem instalar o JFlex

O código gerado está versionado em `gen/`, então basta:

```bash
javac -encoding UTF-8 -d out $(find src/main/java gen -name '*.java')
java  -cp out br.edu.cesupa.tucupi.Main exemplos/01_valido.tcp
```

### Uso da linha de comando

```
java -cp out br.edu.cesupa.tucupi.Main [opções] <arquivo.tcp>

  --json           imprime tokens e erros em JSON
  --sem-tabela     omite a tabela de símbolos
  --bench <n>      benchmark com <n> blocos sintéticos
  -h, --ajuda      ajuda

Código de saída: 0 = sem erros léxicos · 1 = com erros léxicos
```

---

## 4. A linguagem Tucupi

**Palavras reservadas** — `programa` `var` `constante` `inteiro` `real`
`texto` `caractere` `logico` `se` `senao` `enquanto` `para` `ate` `passo`
`funcao` `retorne` `leia` `escreva` `nulo` · literais `verdadeiro` `falso`

**Literais**

| Forma | Exemplo | Token |
|---|---|---|
| Decimal (com separador) | `1_000_000` | `LIT_INTEIRO` |
| Hexadecimal | `0xFF00` | `LIT_INTEIRO` |
| Binário | `0b1010_1100` | `LIT_INTEIRO` |
| Real, com expoente | `9.80665`, `1.0e-9`, `2.` | `LIT_REAL` |
| Texto | `"sensor \"P-01\"\tlinha 3\n"` | `LIT_TEXTO` |
| Caractere | `'a'`, `'\n'`, `'µ'`, `'Ω'` | `LIT_CARACTERE` |
| Lógico | `verdadeiro`, `falso` | `LIT_LOGICO` |

**Operadores** — `+ - * / % **` · `= += -= *= /= %=` · `== != < <= > >=` ·
`<< >> <<= >>=` · `&& || !` · `->` · `..`

**Delimitadores** — `( ) [ ] { } , . ; :`

**Comentários** — `// até o fim da linha` e `/* de bloco, aninháveis */`

---

## 5. Erros léxicos detectados

O arquivo `exemplos/02_erros.tcp` demonstra os dez casos abaixo. Cada um é
registrado com linha e coluna e **a varredura continua**:

1. caractere fora do alfabeto da linguagem (`@`)
2. identificador começando com dígito (`2vias`)
3. literal inteiro fora da faixa de 32 bits (`99999999999999`)
4. separador `_` em posição inválida (`1_000_`)
5. prefixo de base sem dígitos (`0x`)
6. sequência de escape desconhecida (`\q`)
7. escape unicode incompleto (`\u12`)
8. literal de texto não terminado antes do fim da linha
9. literal de caractere inválido (`'abc'`)
10. `*/` sem `/*` correspondente

Há ainda duas regras `<<EOF>>` específicas que detectam **texto** ou
**comentário de bloco** abertos e não fechados até o fim do arquivo.

---

## 6. Resultados medidos

Números obtidos nesta máquina (OpenJDK 21, Linux x86-64) — reproduza com
`make bench` e com `make saidas`. Os números de estados do autômato são
determinísticos; os de desempenho variaram entre rodadas na mesma máquina
(de 140 ms a 240 ms para o mesmo arquivo), por isso são apresentados como faixa.

| Métrica | Valor |
|---|---|
| Estados do NFA construído | 480 |
| Estados do DFA antes da minimização | 236 |
| Estados do DFA minimizado | **212** |
| Vazão (arquivo sintético de 5,06 MB, 1.240.000 tokens) | **≈ 5 a 9 milhões de tokens/s · ≈ 20 a 35 MB/s** |
| `01_valido.tcp` | 335 tokens, 30 identificadores distintos, 0 erros |
| `02_erros.tcp` | 80 tokens, **10 erros** reportados em uma única passagem |
| `03_unicode.tcp` | 159 tokens, 16 identificadores acentuados, 0 erros |

As saídas completas estão em [`docs/saidas/`](docs/saidas/).

---

## 7. Artigo de apoio (opção A)

> BARBOSA, Cinthyan; BONIDIA, Robson; NETO, João.
> **Flex, JFlex e GALS: Ferramentas de Apoio ao Ensino de Compiladores.**
> In: WORKSHOP SOBRE EDUCAÇÃO EM COMPUTAÇÃO (WEI), 27., 2019, Belém.
> *Anais [...]*. Porto Alegre: Sociedade Brasileira de Computação, 2019.
> p. 176–187. ISSN 2595-6175.
> DOI: <https://doi.org/10.5753/wei.2019.6628>

O artigo compara Flex, JFlex e GALS sob quatro critérios (ambiente de
trabalho, robustez, usabilidade e aplicação do embasamento teórico) e conclui
que o GALS é o mais adequado **como ferramenta didática**, por gerar também
analisadores sintático e semântico e exibir as tabelas geradas — enquanto
Flex e JFlex, restritos à fase léxica e operados por linha de comando, se
destacam pela documentação e pela robustez. Vale registrar que o WEI 2019, em
que o artigo foi publicado, aconteceu em Belém.

---

## 8. Referências verificadas

- JFlex — página oficial: <https://jflex.de/>
- JFlex — manual do usuário: <https://jflex.de/manual.html>
- JFlex — código-fonte e releases: <https://github.com/jflex-de/jflex>
- JFlex Maven Plugin — uso: <https://jflex-de.github.io/jflex-web/jflex-maven-plugin/usage.html>
- Artigo do WEI 2019 (item 7): <https://doi.org/10.5753/wei.2019.6628>

---

## 9. Licença

Código deste trabalho: uso acadêmico livre.
O JFlex é distribuído sob licença BSD (baixado pelo build, não versionado aqui).

# Tucupi — Analisador Léxico e Sintático

> Disciplina **Autômatos e Compiladores** — Centro Universitário do Pará (CESUPA)
> Etapa 1: analisador léxico gerado com **JFlex 1.9.1** ·
> Etapa 2: **gramática livre de contexto** + **analisador sintático** por descida recursiva, escrito à mão em Java

**Equipe:** Jorge Vasconcelos · Lucas Salviano · Renan Abreu · Yuri Severo

---

## 1. O que este repositório contém

Um *front-end* de compilador para a **Tucupi**, uma mini-linguagem imperativa
com palavras reservadas em português:

```text
arquivo .tcp
    ↓
TucupiLexer (gerado pelo JFlex a partir de tucupi.flex)
    ↓  um Token por vez — proximoToken()
AnalisadorSintatico (Java, descida recursiva LL(1))
    ↓
Árvore sintática
    ↓
ACEITO  ou  ERRO SINTÁTICO (linha:coluna)
```

- **Etapa léxica (preservada sem alterações):** `tucupi.flex`, tokens, tabela de
  símbolos, recuperação de erros léxicos, Unicode, benchmark. O
  `TucupiLexer.java` versionado em `gen/` foi regenerado com o JFlex 1.9.1 e
  conferido: é idêntico ao que está no repositório.
- **Etapa sintática (nova):** GLC em [`docs/gramatica.ebnf`](docs/gramatica.ebnf),
  parser que consome diretamente os tokens do lexer, árvore sintática impressa
  no terminal e mensagens de erro com linha, coluna, token esperado, token
  encontrado e a produção em que o erro ocorreu.

Estratégia: **JFlex para a análise léxica + parser manual em Java para a
análise sintática** (sem ANTLR, CUP ou outra ferramenta). Esta etapa **não**
faz análise semântica, geração de código nem interpretação.

---

## 2. Estrutura do projeto

```
tucupi-analisador-lexico/
├── src/main/jflex/br/edu/cesupa/tucupi/
│   └── tucupi.flex              ← especificação léxica (JFlex)
├── src/main/java/br/edu/cesupa/tucupi/
│   ├── Token.java · TokenType.java · ErroLexico.java
│   ├── TabelaDeSimbolos.java · Relatorio.java
│   ├── Main.java                ← CLI da etapa léxica
│   ├── AnalisadorSintatico.java ← NOVO: parser (um método por não-terminal)
│   ├── ErroSintatico.java       ← NOVO: erro com linha, coluna, esperado/encontrado
│   ├── NoSintatico.java         ← NOVO: nó genérico da árvore (tipo, valor, filhos)
│   ├── ImpressoraArvore.java    ← NOVO: imprime a árvore no estilo "tree"
│   └── MainSintatico.java       ← NOVO: CLI da etapa sintática
├── src/test/java/br/edu/cesupa/tucupi/
│   └── TestesSintaticos.java    ← NOVO: testes sem dependências (roda offline)
├── gen/br/edu/cesupa/tucupi/
│   └── TucupiLexer.java         ← GERADO pelo JFlex (versionado para rodar offline)
├── docs/
│   ├── gramatica.ebnf           ← NOVO: a GLC
│   └── saidas/                  ← saídas reais (usadas no artigo e nos slides)
├── exemplos/
│   ├── 01_valido.tcp · 02_erros.tcp · 03_unicode.tcp   (etapa léxica)
│   ├── 04_sintatico_valido.tcp  ← NOVO: caso ACEITO
│   └── 05_sintatico_erro.tcp    ← NOVO: caso REJEITADO (";" ausente)
├── testes/sintatico/
│   ├── validos/*.tcp            ← 9 programas que devem ser aceitos
│   └── invalidos/*.tcp          ← 10 programas com o erro esperado em "// espera-erro: L:C"
├── Makefile · build.sh · demo.bat · pom.xml
└── README.md
```

---

## 3. Como executar

Pré-requisito: **JDK 17 ou superior** (o código usa *text blocks*; testado com
OpenJDK 21). **Nenhum comando abaixo precisa de internet**, exceto os marcados.

### Opção A — Makefile (Linux/macOS/WSL)

```bash
make              # = make demo: compila e roda o caso aceito e o rejeitado
make tabela       # tabela de lexemas e tokens do exemplo da demo (etapa léxica)
make testes       # 19 testes do parser (9 válidos + 10 inválidos)
make sintatico    # parser sobre todos os exemplos/*.tcp
make exemplos     # etapa léxica sobre os exemplos 01..03
make saidas       # regrava docs/saidas/
make limpar       # remove só out/ (NÃO apaga o scanner gerado)

make gerar        # [internet] baixa o JFlex e regenera gen/.../TucupiLexer.java
make completo     # [internet] gerar + compilar + exemplos + demo
```

### Opção B — script único

```bash
./build.sh             # offline: compila, roda os testes e a demonstração
./build.sh --regerar   # [internet] regenera o scanner antes
```

### Opção C — Windows sem make

Rode `demo.bat` (duplo clique ou pelo `cmd`). Ele compila, mostra os dois
casos e roda os testes.

### Opção D — comandos diretos

```bash
javac -encoding UTF-8 -d out $(find src/main/java src/test/java gen -name '*.java')
java  -cp out br.edu.cesupa.tucupi.MainSintatico --tokens exemplos/04_sintatico_valido.tcp
java  -cp out br.edu.cesupa.tucupi.MainSintatico exemplos/05_sintatico_erro.tcp
java  -cp out br.edu.cesupa.tucupi.Main          exemplos/01_valido.tcp
```

### Linha de comando

```
java -cp out br.edu.cesupa.tucupi.MainSintatico [--tokens] [--ascii] <arquivo.tcp>
  --tokens     mostra antes os tokens do lexer, agrupados por linha
  --ascii      desenha a árvore com |-- e `-- (terminal sem UTF-8)
  Código de saída: 0 = ACEITO · 1 = REJEITADO · 2 = uso incorreto

java -cp out br.edu.cesupa.tucupi.Main [--json] [--sem-tabela] [--bench n] <arquivo.tcp>
  Código de saída: 0 = sem erros léxicos · 1 = com erros léxicos
```

---

## 4. Gramática livre de contexto (GLC)

Versão completa e comentada em [`docs/gramatica.ebnf`](docs/gramatica.ebnf).
Os terminais são exatamente os tokens de `TokenType.java` — a gramática não usa
nenhum símbolo que o lexer não produza.

```ebnf
programa      = "programa", IDENTIFICADOR, ";",
                { declaracao },
                { funcao },
                EOF ;

declaracao    = ( "var" | "constante" ), IDENTIFICADOR,
                [ ":", tipo ],
                [ "=", expressao ],
                ";" ;

tipo          = "inteiro" | "real" | "texto" | "caractere" | "logico" ;

funcao        = "funcao", IDENTIFICADOR,
                "(", [ parametros ], ")",
                "->", tipo,
                bloco ;
                (* a funcao de nome "principal" e o ponto de entrada;
                   nao ha palavra reservada "main" *)

parametros    = parametro, { ",", parametro } ;

parametro     = IDENTIFICADOR, ":", tipo ;

bloco         = "{", { comando }, "}" ;


(* -------------------------------- comandos ------------------------------ *)

comando       = declaracao
              | condicional
              | enquanto
              | para
              | leitura, ";"
              | escrita, ";"
              | retorno, ";"
              | comandoIdent, ";" ;

(* Atribuicao e chamada comecam ambas com IDENTIFICADOR. A regra abaixo e a
   forma FATORADA A ESQUERDA das duas, o que mantem a gramatica LL(1):
     atribuicao = alvo, opAtribuicao, expressao ;
     chamada    = IDENTIFICADOR, "(", [ argumentos ], ")" ;               *)
comandoIdent  = IDENTIFICADOR,
                ( "(", [ argumentos ], ")"
                | { sufixo }, opAtribuicao, expressao ) ;

opAtribuicao  = "=" | "+=" | "-=" | "*=" | "/=" | "%=" | "<<=" | ">>=" ;

condicional   = "se", "(", expressao, ")", bloco,
                [ "senao", ( condicional | bloco ) ] ;

enquanto      = "enquanto", "(", expressao, ")", bloco ;

para          = "para", IDENTIFICADOR, "=", expressao,
                "..", expressao,
                [ "passo", expressao ],
                bloco ;

leitura       = "leia", "(", alvo, { ",", alvo }, ")" ;

escrita       = "escreva", "(", [ argumentos ], ")" ;

retorno       = "retorne", [ expressao ] ;

alvo          = IDENTIFICADOR, { sufixo } ;

sufixo        = "[", expressao, "]"
              | ".", IDENTIFICADOR ;

argumentos    = expressao, { ",", expressao } ;


(* ------------------------------ expressoes ------------------------------
   Um nao-terminal por nivel de precedencia, do MENOS para o MAIS
   prioritario. Quanto mais fundo o nivel, mais cedo o operador e agrupado. *)

expressao     = expressaoOu ;

expressaoOu   = expressaoE, { "||", expressaoE } ;

expressaoE    = igualdade, { "&&", igualdade } ;

igualdade     = relacional, { ( "==" | "!=" ), relacional } ;

relacional    = deslocamento, { ( "<" | "<=" | ">" | ">=" ), deslocamento } ;

deslocamento  = soma, { ( "<<" | ">>" ), soma } ;

soma          = produto, { ( "+" | "-" ), produto } ;

produto       = unario, { ( "*" | "/" | "%" ), unario } ;

unario        = ( "!" | "+" | "-" ), unario
              | potencia ;

potencia      = posfixo, [ "**", unario ] ;
                (* 2 ** 3 ** 2 = 2 ** (3 ** 2);  -2 ** 2 = -(2 ** 2) *)

posfixo       = primario, { sufixo } ;

primario      = literal
              | "nulo"
              | IDENTIFICADOR, [ "(", [ argumentos ], ")" ]   (* variavel ou chamada *)
              | "(", expressao, ")" ;

literal       = LIT_INTEIRO | LIT_REAL | LIT_TEXTO | LIT_CARACTERE | LIT_LOGICO ;
```

**Propriedades usadas pelo parser**

- **LL(1):** em todo ponto de decisão, o token atual basta para escolher a
  alternativa — por isso não há retrocesso.
- **Sem recursão à esquerda:** operadores binários usam repetição `{ }`, e o
  laço do parser monta a árvore da esquerda para a direita
  (`a - b - c` = `(a - b) - c`).
- **Fatoração à esquerda:** atribuição e chamada começam com `IDENTIFICADOR`;
  a regra `comandoIdent` decide pelo token seguinte (`(` ou operador de
  atribuição).
- **Sem “senão pendente”:** o corpo de `se` é sempre um bloco com chaves.
- **`principal`:** não há palavra reservada `main`; a função chamada
  `principal` é marcada como ponto de entrada na árvore.

**Precedência** (da menor para a maior): `||` → `&&` → `== !=` →
`< <= > >=` → `<< >>` → `+ -` → `* / %` → unário `! + -` → `**`
(associa à direita) → sufixos `[ ]` e `.` → primário.

---

## 5. Analisador sintático

`AnalisadorSintatico` recebe um `TucupiLexer` e mantém **um token de
lookahead**. Os métodos auxiliares traduzem a EBNF diretamente:

| EBNF | Código |
|---|---|
| terminal `";"` | `esperar(TokenType.DEL_PONTO_VIRGULA)` |
| opcional `[ X ]` | `if (verificar(...)) { X }` |
| repetição `{ X }` | `while (verificar(...)) { X }` |
| alternativa `A` ou `B` | `switch` sobre o token atual |
| não-terminal `bloco` | chamada ao método `bloco()` |

Métodos de produção: `programa`, `declaracao`, `tipo`, `funcao`,
`parametros`, `bloco`, `comando`, `comandoIdent`, `atribuicao`,
`condicional`, `enquanto`, `para`, `leitura`, `escrita`, `retorno`,
`chamada`, `sufixos`, `expressao`, `expressaoOu`, `expressaoE`, `igualdade`,
`relacional`, `deslocamento`, `soma`, `produto`, `unario`, `potencia`,
`posfixo`, `primario`.

Uso programático:

```java
TucupiLexer lexer = new TucupiLexer(reader);
AnalisadorSintatico parser = new AnalisadorSintatico(lexer);
NoSintatico raiz = parser.programa();   // lança ErroSintatico se a entrada for inválida
```

O parser **para no primeiro erro sintático** (não há recuperação nesta etapa).
Se o lexer entregar um token `ERRO` (símbolo inválido), o parser também para
nesse ponto e a lista de erros léxicos é exibida.

---

## 6. Caso aceito — `exemplos/04_sintatico_valido.tcp`

```tucupi
programa Demo;

var limite : inteiro = 10;

funcao principal() -> inteiro {
    var x : inteiro = 5 + 2 * 3;

    se (x > limite && x != 0) {
        escreva("maior");
    } senao {
        escreva("menor");
    }

    retorne 0;
}
```

Árvore produzida (trecho de [`docs/saidas/sintatico_valido.txt`](docs/saidas/sintatico_valido.txt)):

```text
PROGRAMA Demo
├── DECL_VAR limite : inteiro
│   └── LITERAL 10
└── FUNCAO principal -> inteiro  [ponto de entrada]
    └── BLOCO
        ├── DECL_VAR x : inteiro
        │   └── SOMA +
        │       ├── LITERAL 5
        │       └── MULTIPLICACAO *
        │           ├── LITERAL 2
        │           └── LITERAL 3
        ├── SE
        │   ├── CONDICAO
        │   │   └── E &&
        │   │       ├── MAIOR >
        │   │       │   ├── IDENT x
        │   │       │   └── IDENT limite
        │   │       └── DIFERENTE !=
        │   │           ├── IDENT x
        │   │           └── LITERAL 0
        │   ├── BLOCO
        │   │   └── ESCREVA
        │   │       └── LITERAL "maior"
        │   └── SENAO
        │       └── BLOCO
        │           └── ESCREVA
        │               └── LITERAL "menor"
        └── RETORNE
            └── LITERAL 0
```

Os exemplos 01 e 03 da etapa léxica também são **aceitos** pelo parser
(`make sintatico`), o que exercita `para`, `enquanto`, `senao se`, `**`,
`<<`, atribuições compostas, índice `[ ]`, campo `.`, `leia` e chamadas
aninhadas.

---

## 7. Caso rejeitado — `exemplos/05_sintatico_erro.tcp`

O `;` ausente na linha 4 só é percebido quando o parser encontra o `se` da
linha 6. Por isso a mensagem mostra também o **último token aceito** e a
**pilha de produções** ativas:

```text
Erro sintático em 6:5
Mensagem..: falta ";" no fim da declaração
Esperado..: DEL_PONTO_VIRGULA (";")
Encontrado: PR_SE ("se")
Após......: LIT_INTEIRO ("10") em 4:23
Produções.: programa > funcao > bloco > comando > declaracao

   4 |     var x : inteiro = 10
     |                         ^ após este ponto
     :
   6 |     se (x > 2) {
     |     ^ erro detectado aqui
```

---

## 8. Testes

`make testes` executa `TestesSintaticos` sobre `testes/sintatico/`:

| Válidos (devem ser aceitos) | Inválidos (devem falhar na posição indicada) |
|---|---|
| declaração simples | `;` ausente |
| precedência (árvore conferida com `02_precedencia.arvore`) | `)` ausente |
| `se` | `}` ausente |
| `se / senao se / senao` | expressão incompleta (`1 + * 2`) |
| `enquanto` | token inesperado (`retorne 0 0;`) |
| funções com e sem parâmetros | `senao` sem `se` |
| chamadas (inclusive como comando) | declaração global depois das funções |
| só a função `principal` | função sem `-> tipo` |
| `para`, `+=`, `<<`, índice e campo | comparação usada como comando (`x == 1;`) |
| | arquivo sem `programa Nome;` |

Resultado atual: [`docs/saidas/testes_sintaticos.txt`](docs/saidas/testes_sintaticos.txt)
(19 passaram, 0 falharam).

---

## 9. A linguagem Tucupi — tokens

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

## 10. Erros léxicos detectados

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

## 11. Resultados medidos (etapa léxica)

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

## 12. Artigo de apoio (opção A)

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

## 13. Referências verificadas

- JFlex — página oficial: <https://jflex.de/>
- JFlex — manual do usuário: <https://jflex.de/manual.html>
- JFlex — código-fonte e releases: <https://github.com/jflex-de/jflex>
- JFlex Maven Plugin — uso: <https://jflex-de.github.io/jflex-web/jflex-maven-plugin/usage.html>
- Artigo do WEI 2019 (item 12): <https://doi.org/10.5753/wei.2019.6628>

---

## 14. Próximos passos (fora do escopo desta etapa)

- recuperação de erros sintáticos (modo pânico por `;` e `}`);
- análise semântica: tipos, declaração antes do uso, retorno compatível,
  obrigatoriedade de `principal`;
- uso de `ate` no `para` (a palavra é reservada pelo lexer, mas a gramática
  atual usa `..`, como nos exemplos).

---

## 15. Licença

Código deste trabalho: uso acadêmico livre.
O JFlex é distribuído sob licença BSD (baixado pelo build, não versionado aqui).

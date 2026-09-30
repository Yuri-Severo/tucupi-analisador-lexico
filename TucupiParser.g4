// =====================================================================
// TucupiParser.g4 -- analisador sintatico da linguagem Tucupi (ANTLR 4)
//
// Versao simplificada para visualizacao no lab.antlr.org. Reconhece a mesma
// linguagem de docs/gramatica.ebnf, mas escreve as expressoes numa regra so
// (recurso do ANTLR). Regra inicial: programa
// =====================================================================
parser grammar TucupiParser;

options { tokenVocab = TucupiLexer; }

// --------------------------- estrutura geral ----------------------------
programa
    : PR_PROGRAMA IDENTIFICADOR DEL_PONTO_VIRGULA
      declaracao*
      funcao*
      EOF
    ;

declaracao
    : (PR_VAR | PR_CONSTANTE) IDENTIFICADOR
      (DEL_DOIS_PONTOS tipo)?
      (OP_ATRIBUI expressao)?
      DEL_PONTO_VIRGULA
    ;

tipo
    : PR_INTEIRO | PR_REAL | PR_TEXTO | PR_CARACTERE | PR_LOGICO
    ;

// A funcao chamada "principal" e o bloco de execucao principal ("main").
funcao
    : PR_FUNCAO IDENTIFICADOR DEL_ABRE_PAR parametros? DEL_FECHA_PAR
      OP_SETA tipo bloco
    ;

parametros
    : parametro (DEL_VIRGULA parametro)*
    ;

parametro
    : IDENTIFICADOR DEL_DOIS_PONTOS tipo
    ;

bloco
    : DEL_ABRE_CHA comando* DEL_FECHA_CHA
    ;

// ------------------------------- comandos -------------------------------
comando
    : declaracao
    | condicional
    | enquanto
    | para
    | leitura DEL_PONTO_VIRGULA
    | escrita DEL_PONTO_VIRGULA
    | retorno DEL_PONTO_VIRGULA
    | comandoIdent DEL_PONTO_VIRGULA
    ;

// Atribuicao e chamada comecam com IDENTIFICADOR: regra fatorada a esquerda.
comandoIdent
    : IDENTIFICADOR
      ( DEL_ABRE_PAR argumentos? DEL_FECHA_PAR
      | sufixo* opAtribuicao expressao
      )
    ;

opAtribuicao
    : OP_ATRIBUI | OP_MAIS_ATRIBUI | OP_MENOS_ATRIBUI | OP_VEZES_ATRIBUI
    | OP_DIVIDE_ATRIBUI | OP_RESTO_ATRIBUI
    | OP_DESLOCA_ESQ_ATRIBUI | OP_DESLOCA_DIR_ATRIBUI
    ;

condicional
    : PR_SE DEL_ABRE_PAR expressao DEL_FECHA_PAR bloco
      (PR_SENAO (condicional | bloco))?
    ;

enquanto
    : PR_ENQUANTO DEL_ABRE_PAR expressao DEL_FECHA_PAR bloco
    ;

para
    : PR_PARA IDENTIFICADOR OP_ATRIBUI expressao
      OP_INTERVALO expressao
      (PR_PASSO expressao)?
      bloco
    ;

leitura
    : PR_LEIA DEL_ABRE_PAR alvo (DEL_VIRGULA alvo)* DEL_FECHA_PAR
    ;

escrita
    : PR_ESCREVA DEL_ABRE_PAR argumentos? DEL_FECHA_PAR
    ;

retorno
    : PR_RETORNE expressao?
    ;

alvo
    : IDENTIFICADOR sufixo*
    ;

sufixo
    : DEL_ABRE_COL expressao DEL_FECHA_COL
    | DEL_PONTO IDENTIFICADOR
    ;

argumentos
    : expressao (DEL_VIRGULA expressao)*
    ;

// ------------------------------ expressoes ------------------------------
// Versao SIMPLIFICADA para o ANTLR: uma unica regra recursiva a esquerda.
// A precedencia vem da ORDEM das alternativas (a primeira liga mais forte).
// O ANTLR reescreve internamente essa recursao; a arvore fica bem mais baixa.
expressao
    : expressao sufixo                                              # acesso
    | <assoc=right> expressao OP_POTENCIA expressao                 # potencia
    | (OP_NAO_LOGICO | OP_MAIS | OP_MENOS) expressao                # unario
    | expressao (OP_VEZES | OP_DIVIDE | OP_RESTO) expressao         # multiplicacao
    | expressao (OP_MAIS | OP_MENOS) expressao                      # soma
    | expressao (OP_DESLOCA_ESQ | OP_DESLOCA_DIR) expressao         # deslocamento
    | expressao (OP_MENOR | OP_MENOR_IGUAL
                | OP_MAIOR | OP_MAIOR_IGUAL) expressao              # comparacao
    | expressao (OP_IGUAL | OP_DIFERENTE) expressao                 # igualdade
    | expressao OP_E_LOGICO expressao                               # eLogico
    | expressao OP_OU_LOGICO expressao                              # ouLogico
    | IDENTIFICADOR DEL_ABRE_PAR argumentos? DEL_FECHA_PAR          # chamada
    | DEL_ABRE_PAR expressao DEL_FECHA_PAR                          # parenteses
    | IDENTIFICADOR                                                 # variavel
    | (LIT_INTEIRO | LIT_REAL | LIT_TEXTO | LIT_CARACTERE
      | LIT_LOGICO | PR_NULO)                                       # literal
    ;

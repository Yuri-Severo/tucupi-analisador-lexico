package br.edu.cesupa.tucupi;

/**
 * Conjunto de tokens da mini-linguagem Tucupi.
 *
 * <p>Cada token carrega uma {@link Categoria}, usada pelo relatorio de saida e
 * pelas estatisticas. A separacao entre "categoria" e "tipo" e a mesma que um
 * analisador sintatico usaria: o parser olha o tipo; ferramentas de
 * diagnostico (realce de sintaxe, metricas) olham a categoria.</p>
 */
public enum TokenType {

    // ------------------------------------------------------------------
    // Palavras reservadas
    // ------------------------------------------------------------------
    PR_PROGRAMA(Categoria.PALAVRA_RESERVADA),
    PR_VAR(Categoria.PALAVRA_RESERVADA),
    PR_CONSTANTE(Categoria.PALAVRA_RESERVADA),
    PR_INTEIRO(Categoria.PALAVRA_RESERVADA),
    PR_REAL(Categoria.PALAVRA_RESERVADA),
    PR_TEXTO(Categoria.PALAVRA_RESERVADA),
    PR_CARACTERE(Categoria.PALAVRA_RESERVADA),
    PR_LOGICO(Categoria.PALAVRA_RESERVADA),
    PR_SE(Categoria.PALAVRA_RESERVADA),
    PR_SENAO(Categoria.PALAVRA_RESERVADA),
    PR_ENQUANTO(Categoria.PALAVRA_RESERVADA),
    PR_PARA(Categoria.PALAVRA_RESERVADA),
    PR_ATE(Categoria.PALAVRA_RESERVADA),
    PR_PASSO(Categoria.PALAVRA_RESERVADA),
    PR_FUNCAO(Categoria.PALAVRA_RESERVADA),
    PR_RETORNE(Categoria.PALAVRA_RESERVADA),
    PR_LEIA(Categoria.PALAVRA_RESERVADA),
    PR_ESCREVA(Categoria.PALAVRA_RESERVADA),
    PR_NULO(Categoria.PALAVRA_RESERVADA),

    // ------------------------------------------------------------------
    // Identificadores e literais
    // ------------------------------------------------------------------
    IDENTIFICADOR(Categoria.IDENTIFICADOR),
    LIT_INTEIRO(Categoria.LITERAL),
    LIT_REAL(Categoria.LITERAL),
    LIT_TEXTO(Categoria.LITERAL),
    LIT_CARACTERE(Categoria.LITERAL),
    LIT_LOGICO(Categoria.LITERAL),

    // ------------------------------------------------------------------
    // Operadores
    // ------------------------------------------------------------------
    OP_MAIS(Categoria.OPERADOR),
    OP_MENOS(Categoria.OPERADOR),
    OP_VEZES(Categoria.OPERADOR),
    OP_DIVIDE(Categoria.OPERADOR),
    OP_RESTO(Categoria.OPERADOR),
    OP_POTENCIA(Categoria.OPERADOR),

    OP_ATRIBUI(Categoria.OPERADOR),
    OP_MAIS_ATRIBUI(Categoria.OPERADOR),
    OP_MENOS_ATRIBUI(Categoria.OPERADOR),
    OP_VEZES_ATRIBUI(Categoria.OPERADOR),
    OP_DIVIDE_ATRIBUI(Categoria.OPERADOR),
    OP_RESTO_ATRIBUI(Categoria.OPERADOR),

    OP_IGUAL(Categoria.OPERADOR),
    OP_DIFERENTE(Categoria.OPERADOR),
    OP_MENOR(Categoria.OPERADOR),
    OP_MENOR_IGUAL(Categoria.OPERADOR),
    OP_MAIOR(Categoria.OPERADOR),
    OP_MAIOR_IGUAL(Categoria.OPERADOR),

    OP_DESLOCA_ESQ(Categoria.OPERADOR),
    OP_DESLOCA_DIR(Categoria.OPERADOR),
    OP_DESLOCA_ESQ_ATRIBUI(Categoria.OPERADOR),
    OP_DESLOCA_DIR_ATRIBUI(Categoria.OPERADOR),

    OP_E_LOGICO(Categoria.OPERADOR),
    OP_OU_LOGICO(Categoria.OPERADOR),
    OP_NAO_LOGICO(Categoria.OPERADOR),

    OP_SETA(Categoria.OPERADOR),
    OP_INTERVALO(Categoria.OPERADOR),

    // ------------------------------------------------------------------
    // Delimitadores
    // ------------------------------------------------------------------
    DEL_ABRE_PAR(Categoria.DELIMITADOR),
    DEL_FECHA_PAR(Categoria.DELIMITADOR),
    DEL_ABRE_COL(Categoria.DELIMITADOR),
    DEL_FECHA_COL(Categoria.DELIMITADOR),
    DEL_ABRE_CHA(Categoria.DELIMITADOR),
    DEL_FECHA_CHA(Categoria.DELIMITADOR),
    DEL_VIRGULA(Categoria.DELIMITADOR),
    DEL_PONTO(Categoria.DELIMITADOR),
    DEL_PONTO_VIRGULA(Categoria.DELIMITADOR),
    DEL_DOIS_PONTOS(Categoria.DELIMITADOR),

    // ------------------------------------------------------------------
    // Especiais
    // ------------------------------------------------------------------
    EOF(Categoria.ESPECIAL),
    ERRO(Categoria.ESPECIAL);

    /** Grupos usados no relatorio e nas estatisticas. */
    public enum Categoria {
        PALAVRA_RESERVADA,
        IDENTIFICADOR,
        LITERAL,
        OPERADOR,
        DELIMITADOR,
        ESPECIAL
    }

    private final Categoria categoria;

    TokenType(Categoria categoria) {
        this.categoria = categoria;
    }

    public Categoria categoria() {
        return categoria;
    }
}

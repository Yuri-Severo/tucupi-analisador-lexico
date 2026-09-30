package br.edu.cesupa.tucupi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Analisador sintatico da linguagem Tucupi -- descida recursiva, LL(1).
 *
 * <p>Cada nao-terminal da gramatica ({@code docs/gramatica.ebnf}) vira um
 * metodo. O analisador pede um token por vez ao {@link TucupiLexer} (gerado
 * pelo JFlex) e decide o que fazer olhando apenas o token atual
 * (<em>lookahead</em> de 1). Nao ha retrocesso: se o token atual nao serve
 * para nenhuma alternativa, a entrada e rejeitada com um
 * {@link ErroSintatico}.</p>
 *
 * <p>Correspondencia entre EBNF e codigo:</p>
 * <pre>
 *   [ X ]      -&gt;  if (verificar(...)) { X }
 *   { X }      -&gt;  while (verificar(...)) { X }
 *   A | B      -&gt;  switch / if sobre o token atual
 *   "terminal" -&gt;  esperar(TIPO)
 * </pre>
 *
 * <p>Uso:</p>
 * <pre>
 *   TucupiLexer lexer = new TucupiLexer(reader);
 *   AnalisadorSintatico parser = new AnalisadorSintatico(lexer);
 *   NoSintatico raiz = parser.programa();
 * </pre>
 */
public final class AnalisadorSintatico {

    private final TucupiLexer lexer;

    /** Token de lookahead: o proximo token ainda nao consumido. */
    private Token atual;

    /** Ultimo token consumido (usado nas mensagens de erro). */
    private Token anterior;

    private int consumidos = 0;

    /** Token do nome da funcao "principal", se existir. */
    private Token pontoDeEntrada;

    /** Producoes ativas, para mostrar ONDE na gramatica o erro ocorreu. */
    private final Deque<String> producoes = new ArrayDeque<>();

    public AnalisadorSintatico(TucupiLexer lexer) {
        this.lexer = lexer;
        avancar();
    }

    // =====================================================================
    // Metodos auxiliares
    // =====================================================================

    /** Consome o token atual e le o proximo do analisador lexico. */
    private void avancar() {
        if (atual != null) {
            anterior = atual;
            consumidos++;
        }
        try {
            atual = lexer.proximoToken();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (atual.tipo() == TokenType.ERRO) {
            throw erro("símbolo inválido: o analisador léxico não reconheceu este trecho",
                    "um token válido da linguagem");
        }
    }

    /** O token atual e do tipo informado? (nao consome) */
    private boolean verificar(TokenType tipo) {
        return atual.tipo() == tipo;
    }

    private boolean verificarAlgum(TokenType... tipos) {
        for (TokenType t : tipos) {
            if (atual.tipo() == t) {
                return true;
            }
        }
        return false;
    }

    /** Se o token atual for do tipo informado, consome e devolve true. */
    private boolean aceitar(TokenType tipo) {
        if (verificar(tipo)) {
            avancar();
            return true;
        }
        return false;
    }

    /** Exige um token do tipo informado; caso contrario, erro sintatico. */
    private Token esperar(TokenType tipo) {
        return esperar(tipo, "token inesperado");
    }

    private Token esperar(TokenType tipo, String mensagem) {
        if (!verificar(tipo)) {
            throw erro(mensagem, descrever(tipo));
        }
        Token t = atual;
        if (tipo != TokenType.EOF) {
            avancar();
        }
        return t;
    }

    private ErroSintatico erro(String mensagem, String esperado) {
        return new ErroSintatico(mensagem, atual, esperado, anterior, caminho());
    }

    private void entrar(String producao) {
        producoes.push(producao);
    }

    /** Desempilha a producao corrente e devolve o no construido. */
    private NoSintatico sair(NoSintatico no) {
        producoes.pop();
        return no;
    }

    private String caminho() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = producoes.descendingIterator();
        while (it.hasNext()) {
            if (sb.length() > 0) {
                sb.append(" > ");
            }
            sb.append(it.next());
        }
        return sb.length() == 0 ? "(nenhuma)" : sb.toString();
    }

    public int tokensConsumidos() {
        return consumidos;
    }

    /** Token do nome da funcao {@code principal}, ou {@code null} se ela nao existir. */
    public Token pontoDeEntrada() {
        return pontoDeEntrada;
    }

    // =====================================================================
    // Estrutura geral
    // =====================================================================

    /** programa = "programa" IDENTIFICADOR ";" { declaracao } { funcao } EOF */
    public NoSintatico programa() {
        entrar("programa");
        Token inicio = esperar(TokenType.PR_PROGRAMA,
                "todo arquivo Tucupi deve começar com \"programa <Nome>;\"");
        Token nome = esperar(TokenType.IDENTIFICADOR, "nome do programa ausente");
        esperar(TokenType.DEL_PONTO_VIRGULA, "falta \";\" após o nome do programa");

        NoSintatico raiz = new NoSintatico("PROGRAMA", nome.lexema(), inicio);

        while (verificarAlgum(TokenType.PR_VAR, TokenType.PR_CONSTANTE)) {
            raiz.adicionar(declaracao());
        }
        while (verificar(TokenType.PR_FUNCAO)) {
            raiz.adicionar(funcao());
        }

        if (verificarAlgum(TokenType.PR_VAR, TokenType.PR_CONSTANTE)) {
            throw erro("declarações globais devem vir antes de todas as funções",
                    "PR_FUNCAO (\"funcao\") ou EOF");
        }
        esperar(TokenType.EOF, "conteúdo inesperado fora de uma função");
        return sair(raiz);
    }

    /** declaracao = ("var" | "constante") IDENTIFICADOR [":" tipo] ["=" expressao] ";" */
    private NoSintatico declaracao() {
        entrar("declaracao");
        Token inicio = atual;
        boolean constante = verificar(TokenType.PR_CONSTANTE);
        if (!constante) {
            esperar(TokenType.PR_VAR);
        } else {
            avancar();
        }
        Token nome = esperar(TokenType.IDENTIFICADOR, "nome da variável ausente");

        String tipo = null;
        if (aceitar(TokenType.DEL_DOIS_PONTOS)) {
            tipo = tipo();
        }
        String rotulo = nome.lexema() + (tipo != null ? " : " + tipo : "");
        NoSintatico no = new NoSintatico(constante ? "DECL_CONST" : "DECL_VAR", rotulo, inicio);

        if (aceitar(TokenType.OP_ATRIBUI)) {
            no.adicionar(expressao());
        }
        esperar(TokenType.DEL_PONTO_VIRGULA, "falta \";\" no fim da declaração");
        return sair(no);
    }

    /** tipo = "inteiro" | "real" | "texto" | "caractere" | "logico" */
    private String tipo() {
        if (verificarAlgum(TokenType.PR_INTEIRO, TokenType.PR_REAL, TokenType.PR_TEXTO,
                TokenType.PR_CARACTERE, TokenType.PR_LOGICO)) {
            String nome = atual.lexema();
            avancar();
            return nome;
        }
        throw erro("tipo inválido", "tipo (inteiro, real, texto, caractere ou logico)");
    }

    /** funcao = "funcao" IDENTIFICADOR "(" [parametros] ")" "->" tipo bloco */
    private NoSintatico funcao() {
        entrar("funcao");
        Token inicio = esperar(TokenType.PR_FUNCAO);
        Token nome = esperar(TokenType.IDENTIFICADOR, "nome da função ausente");
        esperar(TokenType.DEL_ABRE_PAR, "falta \"(\" após o nome da função");

        NoSintatico params = null;
        if (!verificar(TokenType.DEL_FECHA_PAR)) {
            params = parametros();
        }
        esperar(TokenType.DEL_FECHA_PAR, "lista de parâmetros não foi fechada");
        esperar(TokenType.OP_SETA, "falta \"->\" antes do tipo de retorno");
        String retorno = tipo();

        boolean principal = "principal".equals(nome.lexema());
        if (principal) {
            pontoDeEntrada = nome;
        }
        NoSintatico no = new NoSintatico("FUNCAO",
                nome.lexema() + " -> " + retorno + (principal ? "  [ponto de entrada]" : ""),
                inicio);
        no.adicionar(params);
        no.adicionar(bloco());
        return sair(no);
    }

    /** parametros = parametro { "," parametro } ;  parametro = IDENTIFICADOR ":" tipo */
    private NoSintatico parametros() {
        entrar("parametros");
        NoSintatico no = new NoSintatico("PARAMETROS", atual);
        do {
            Token nome = esperar(TokenType.IDENTIFICADOR, "nome de parâmetro esperado");
            esperar(TokenType.DEL_DOIS_PONTOS, "parâmetro sem tipo");
            no.adicionar(new NoSintatico("PARAM", nome.lexema() + " : " + tipo(), nome));
        } while (aceitar(TokenType.DEL_VIRGULA));
        return sair(no);
    }

    /** bloco = "{" { comando } "}" */
    private NoSintatico bloco() {
        entrar("bloco");
        Token abre = esperar(TokenType.DEL_ABRE_CHA, "bloco deve começar com \"{\"");
        NoSintatico no = new NoSintatico("BLOCO", abre);
        while (!verificar(TokenType.DEL_FECHA_CHA) && !verificar(TokenType.EOF)) {
            no.adicionar(comando());
        }
        esperar(TokenType.DEL_FECHA_CHA,
                "bloco aberto em " + abre.linha() + ":" + abre.coluna() + " não foi fechado");
        return sair(no);
    }

    // =====================================================================
    // Comandos
    // =====================================================================

    private NoSintatico comando() {
        entrar("comando");
        NoSintatico no;
        switch (atual.tipo()) {
            case PR_VAR:
            case PR_CONSTANTE:
                no = declaracao();
                break;
            case PR_SE:
                no = condicional();
                break;
            case PR_ENQUANTO:
                no = enquanto();
                break;
            case PR_PARA:
                no = para();
                break;
            case PR_LEIA:
                no = leitura();
                esperar(TokenType.DEL_PONTO_VIRGULA, "falta \";\" após o comando leia");
                break;
            case PR_ESCREVA:
                no = escrita();
                esperar(TokenType.DEL_PONTO_VIRGULA, "falta \";\" após o comando escreva");
                break;
            case PR_RETORNE:
                no = retorno();
                esperar(TokenType.DEL_PONTO_VIRGULA, "falta \";\" após o comando retorne");
                break;
            case IDENTIFICADOR:
                no = comandoIdent();
                esperar(TokenType.DEL_PONTO_VIRGULA, "falta \";\" no fim do comando");
                break;
            case PR_SENAO:
                throw erro("\"senao\" sem \"se\" correspondente", "comando");
            case PR_FUNCAO:
                throw erro("funções não podem ser declaradas dentro de um bloco", "comando");
            default:
                throw erro("início de comando inválido",
                        "comando (var, constante, se, enquanto, para, leia, escreva, "
                                + "retorne ou identificador)");
        }
        return sair(no);
    }

    /**
     * comandoIdent = IDENTIFICADOR ( "(" [argumentos] ")" | {sufixo} opAtribuicao expressao )
     *
     * <p>Atribuicao e chamada comecam com o mesmo token; a gramatica foi
     * fatorada a esquerda e o token SEGUINTE ao identificador decide.</p>
     */
    private NoSintatico comandoIdent() {
        Token id = esperar(TokenType.IDENTIFICADOR);
        if (verificar(TokenType.DEL_ABRE_PAR)) {
            return chamada(id);
        }
        return atribuicao(id);
    }

    /** atribuicao = alvo opAtribuicao expressao */
    private NoSintatico atribuicao(Token id) {
        entrar("atribuicao");
        NoSintatico alvo = sufixos(new NoSintatico("IDENT", id.lexema(), id));
        if (!verificarAlgum(TokenType.OP_ATRIBUI, TokenType.OP_MAIS_ATRIBUI,
                TokenType.OP_MENOS_ATRIBUI, TokenType.OP_VEZES_ATRIBUI,
                TokenType.OP_DIVIDE_ATRIBUI, TokenType.OP_RESTO_ATRIBUI,
                TokenType.OP_DESLOCA_ESQ_ATRIBUI, TokenType.OP_DESLOCA_DIR_ATRIBUI)) {
            throw erro("comando incompleto após identificador",
                    "operador de atribuição (=, +=, -=, *=, /=, %=, <<=, >>=) ou \"(\" de chamada");
        }
        Token op = atual;
        avancar();
        NoSintatico no = new NoSintatico("ATRIBUICAO", op.lexema(), op);
        no.adicionar(alvo);
        no.adicionar(expressao());
        return sair(no);
    }

    /** condicional = "se" "(" expressao ")" bloco [ "senao" (condicional | bloco) ] */
    private NoSintatico condicional() {
        entrar("condicional");
        Token se = esperar(TokenType.PR_SE);
        esperar(TokenType.DEL_ABRE_PAR, "a condição do \"se\" deve estar entre parênteses");
        NoSintatico condicao = new NoSintatico("CONDICAO", atual).adicionar(expressao());
        esperar(TokenType.DEL_FECHA_PAR, "parêntese da condição não foi fechado");

        NoSintatico no = new NoSintatico("SE", se);
        no.adicionar(condicao);
        no.adicionar(bloco());

        if (verificar(TokenType.PR_SENAO)) {
            Token senao = atual;
            avancar();
            NoSintatico ramo = new NoSintatico("SENAO", senao);
            if (verificar(TokenType.PR_SE)) {
                ramo.adicionar(condicional());
            } else if (verificar(TokenType.DEL_ABRE_CHA)) {
                ramo.adicionar(bloco());
            } else {
                throw erro("\"senao\" deve ser seguido de \"se\" ou de um bloco",
                        "PR_SE (\"se\") ou DEL_ABRE_CHA (\"{\")");
            }
            no.adicionar(ramo);
        }
        return sair(no);
    }

    /** enquanto = "enquanto" "(" expressao ")" bloco */
    private NoSintatico enquanto() {
        entrar("enquanto");
        Token inicio = esperar(TokenType.PR_ENQUANTO);
        esperar(TokenType.DEL_ABRE_PAR, "a condição do \"enquanto\" deve estar entre parênteses");
        NoSintatico condicao = new NoSintatico("CONDICAO", atual).adicionar(expressao());
        esperar(TokenType.DEL_FECHA_PAR, "parêntese da condição não foi fechado");

        NoSintatico no = new NoSintatico("ENQUANTO", inicio);
        no.adicionar(condicao);
        no.adicionar(bloco());
        return sair(no);
    }

    /** para = "para" IDENTIFICADOR "=" expressao ".." expressao [ "passo" expressao ] bloco */
    private NoSintatico para() {
        entrar("para");
        Token inicio = esperar(TokenType.PR_PARA);
        Token var = esperar(TokenType.IDENTIFICADOR, "variável de controle do \"para\" ausente");
        esperar(TokenType.OP_ATRIBUI, "falta \"=\" após a variável de controle");
        NoSintatico de = new NoSintatico("INICIO", atual).adicionar(expressao());
        if (verificar(TokenType.PR_ATE)) {
            throw erro("nesta versão da gramática o intervalo do \"para\" usa \"..\"",
                    descrever(TokenType.OP_INTERVALO));
        }
        esperar(TokenType.OP_INTERVALO, "falta o operador de intervalo \"..\"");
        NoSintatico ate = new NoSintatico("FIM", atual).adicionar(expressao());

        NoSintatico no = new NoSintatico("PARA", var.lexema(), inicio);
        no.adicionar(de);
        no.adicionar(ate);
        if (verificar(TokenType.PR_PASSO)) {
            Token passo = atual;
            avancar();
            no.adicionar(new NoSintatico("PASSO", passo).adicionar(expressao()));
        }
        no.adicionar(bloco());
        return sair(no);
    }

    /** leitura = "leia" "(" alvo { "," alvo } ")" */
    private NoSintatico leitura() {
        entrar("leitura");
        Token inicio = esperar(TokenType.PR_LEIA);
        esperar(TokenType.DEL_ABRE_PAR, "falta \"(\" após \"leia\"");
        NoSintatico no = new NoSintatico("LEIA", inicio);
        do {
            Token id = esperar(TokenType.IDENTIFICADOR, "\"leia\" exige uma variável");
            no.adicionar(sufixos(new NoSintatico("IDENT", id.lexema(), id)));
        } while (aceitar(TokenType.DEL_VIRGULA));
        esperar(TokenType.DEL_FECHA_PAR, "parêntese do \"leia\" não foi fechado");
        return sair(no);
    }

    /** escrita = "escreva" "(" [ argumentos ] ")" */
    private NoSintatico escrita() {
        entrar("escrita");
        Token inicio = esperar(TokenType.PR_ESCREVA);
        esperar(TokenType.DEL_ABRE_PAR, "falta \"(\" após \"escreva\"");
        NoSintatico no = new NoSintatico("ESCREVA", inicio);
        if (!verificar(TokenType.DEL_FECHA_PAR)) {
            argumentos(no);
        }
        esperar(TokenType.DEL_FECHA_PAR, "parêntese do \"escreva\" não foi fechado");
        return sair(no);
    }

    /** retorno = "retorne" [ expressao ] */
    private NoSintatico retorno() {
        entrar("retorno");
        Token inicio = esperar(TokenType.PR_RETORNE);
        NoSintatico no = new NoSintatico("RETORNE", inicio);
        if (!verificar(TokenType.DEL_PONTO_VIRGULA)) {
            no.adicionar(expressao());
        }
        return sair(no);
    }

    /** argumentos = expressao { "," expressao } -- acrescenta cada argumento ao no pai. */
    private void argumentos(NoSintatico pai) {
        do {
            pai.adicionar(expressao());
        } while (aceitar(TokenType.DEL_VIRGULA));
    }

    /** chamada = IDENTIFICADOR "(" [ argumentos ] ")" -- o identificador ja foi consumido. */
    private NoSintatico chamada(Token nome) {
        entrar("chamada");
        esperar(TokenType.DEL_ABRE_PAR);
        NoSintatico no = new NoSintatico("CHAMADA", nome.lexema(), nome);
        if (!verificar(TokenType.DEL_FECHA_PAR)) {
            argumentos(no);
        }
        esperar(TokenType.DEL_FECHA_PAR,
                "argumentos de \"" + nome.lexema() + "\" não foram fechados com \")\"");
        return sair(no);
    }

    /** { sufixo } ;  sufixo = "[" expressao "]" | "." IDENTIFICADOR */
    private NoSintatico sufixos(NoSintatico base) {
        NoSintatico no = base;
        while (true) {
            if (verificar(TokenType.DEL_ABRE_COL)) {
                Token abre = atual;
                avancar();
                NoSintatico indice = expressao();
                esperar(TokenType.DEL_FECHA_COL, "colchete não foi fechado");
                no = new NoSintatico("INDICE", "[]", abre).adicionar(no).adicionar(indice);
            } else if (verificar(TokenType.DEL_PONTO)) {
                avancar();
                Token campo = esperar(TokenType.IDENTIFICADOR, "nome do campo esperado após \".\"");
                no = new NoSintatico("CAMPO", campo.lexema(), campo).adicionar(no);
            } else {
                return no;
            }
        }
    }

    // =====================================================================
    // Expressoes -- um metodo por nivel de precedencia (menor -> maior)
    // =====================================================================

    /** expressao = expressaoOu */
    private NoSintatico expressao() {
        entrar("expressao");
        return sair(expressaoOu());
    }

    /** expressaoOu = expressaoE { "||" expressaoE } */
    private NoSintatico expressaoOu() {
        return binario(this::expressaoE, TokenType.OP_OU_LOGICO);
    }

    /** expressaoE = igualdade { "&&" igualdade } */
    private NoSintatico expressaoE() {
        return binario(this::igualdade, TokenType.OP_E_LOGICO);
    }

    /** igualdade = relacional { ("==" | "!=") relacional } */
    private NoSintatico igualdade() {
        return binario(this::relacional, TokenType.OP_IGUAL, TokenType.OP_DIFERENTE);
    }

    /** relacional = deslocamento { ("<" | "<=" | ">" | ">=") deslocamento } */
    private NoSintatico relacional() {
        return binario(this::deslocamento, TokenType.OP_MENOR, TokenType.OP_MENOR_IGUAL,
                TokenType.OP_MAIOR, TokenType.OP_MAIOR_IGUAL);
    }

    /** deslocamento = soma { ("<<" | ">>") soma } */
    private NoSintatico deslocamento() {
        return binario(this::soma, TokenType.OP_DESLOCA_ESQ, TokenType.OP_DESLOCA_DIR);
    }

    /** soma = produto { ("+" | "-") produto } */
    private NoSintatico soma() {
        return binario(this::produto, TokenType.OP_MAIS, TokenType.OP_MENOS);
    }

    /** produto = unario { ("*" | "/" | "%") unario } */
    private NoSintatico produto() {
        return binario(this::unario, TokenType.OP_VEZES, TokenType.OP_DIVIDE, TokenType.OP_RESTO);
    }

    /**
     * Esquema comum aos niveis binarios: {@code X = Y { op Y }}.
     * O laco constroi a arvore da esquerda para a direita, o que da
     * associatividade a esquerda: {@code a - b - c} vira {@code (a - b) - c}.
     */
    private NoSintatico binario(Supplier<NoSintatico> proximoNivel, TokenType... operadores) {
        NoSintatico esquerda = proximoNivel.get();
        while (verificarAlgum(operadores)) {
            Token op = atual;
            avancar();
            NoSintatico direita = proximoNivel.get();
            esquerda = new NoSintatico(nomeOperador(op.tipo()), op.lexema(), op)
                    .adicionar(esquerda)
                    .adicionar(direita);
        }
        return esquerda;
    }

    /** unario = ("!" | "+" | "-") unario | potencia */
    private NoSintatico unario() {
        if (verificarAlgum(TokenType.OP_NAO_LOGICO, TokenType.OP_MENOS, TokenType.OP_MAIS)) {
            Token op = atual;
            avancar();
            String nome = op.tipo() == TokenType.OP_NAO_LOGICO ? "NAO"
                    : op.tipo() == TokenType.OP_MENOS ? "NEGATIVO" : "POSITIVO";
            return new NoSintatico(nome, op.lexema(), op).adicionar(unario());
        }
        return potencia();
    }

    /** potencia = posfixo [ "**" unario ]  -- recursao a direita: associa a direita. */
    private NoSintatico potencia() {
        NoSintatico base = posfixo();
        if (verificar(TokenType.OP_POTENCIA)) {
            Token op = atual;
            avancar();
            return new NoSintatico("POTENCIA", op.lexema(), op).adicionar(base).adicionar(unario());
        }
        return base;
    }

    /** posfixo = primario { sufixo } */
    private NoSintatico posfixo() {
        return sufixos(primario());
    }

    /** primario = literal | "nulo" | IDENTIFICADOR [ "(" [argumentos] ")" ] | "(" expressao ")" */
    private NoSintatico primario() {
        Token t = atual;
        switch (t.tipo()) {
            case LIT_INTEIRO:
            case LIT_REAL:
            case LIT_TEXTO:
            case LIT_CARACTERE:
            case LIT_LOGICO:
                avancar();
                return new NoSintatico("LITERAL", t.lexemaVisivel(), t);
            case PR_NULO:
                avancar();
                return new NoSintatico("NULO", t);
            case IDENTIFICADOR:
                avancar();
                if (verificar(TokenType.DEL_ABRE_PAR)) {
                    return chamada(t);
                }
                return new NoSintatico("IDENT", t.lexema(), t);
            case DEL_ABRE_PAR:
                avancar();
                NoSintatico interna = expressao();
                esperar(TokenType.DEL_FECHA_PAR,
                        "parêntese aberto em " + t.linha() + ":" + t.coluna() + " não foi fechado");
                return interna;
            default:
                throw erro("expressão incompleta ou mal formada",
                        "expressão (literal, identificador, \"(\", \"!\", \"-\" ou \"+\")");
        }
    }

    // =====================================================================
    // Tabelas de nomes usadas na arvore e nas mensagens
    // =====================================================================

    private static String nomeOperador(TokenType tipo) {
        switch (tipo) {
            case OP_OU_LOGICO:   return "OU";
            case OP_E_LOGICO:    return "E";
            case OP_IGUAL:       return "IGUAL";
            case OP_DIFERENTE:   return "DIFERENTE";
            case OP_MENOR:       return "MENOR";
            case OP_MENOR_IGUAL: return "MENOR_IGUAL";
            case OP_MAIOR:       return "MAIOR";
            case OP_MAIOR_IGUAL: return "MAIOR_IGUAL";
            case OP_DESLOCA_ESQ: return "DESLOCA_ESQ";
            case OP_DESLOCA_DIR: return "DESLOCA_DIR";
            case OP_MAIS:        return "SOMA";
            case OP_MENOS:       return "SUBTRACAO";
            case OP_VEZES:       return "MULTIPLICACAO";
            case OP_DIVIDE:      return "DIVISAO";
            case OP_RESTO:       return "RESTO";
            default:             return tipo.name();
        }
    }

    /** "DEL_PONTO_VIRGULA (";")" -- nome do token mais o simbolo, quando ele e fixo. */
    static String descrever(TokenType tipo) {
        String s = simbolo(tipo);
        return s == null ? tipo.name() : tipo.name() + " (\"" + s + "\")";
    }

    private static String simbolo(TokenType tipo) {
        switch (tipo) {
            case PR_PROGRAMA:       return "programa";
            case PR_VAR:            return "var";
            case PR_CONSTANTE:      return "constante";
            case PR_SE:             return "se";
            case PR_SENAO:          return "senao";
            case PR_ENQUANTO:       return "enquanto";
            case PR_PARA:           return "para";
            case PR_PASSO:          return "passo";
            case PR_FUNCAO:         return "funcao";
            case PR_RETORNE:        return "retorne";
            case PR_LEIA:           return "leia";
            case PR_ESCREVA:        return "escreva";
            case OP_ATRIBUI:        return "=";
            case OP_SETA:           return "->";
            case OP_INTERVALO:      return "..";
            case DEL_ABRE_PAR:      return "(";
            case DEL_FECHA_PAR:     return ")";
            case DEL_ABRE_COL:      return "[";
            case DEL_FECHA_COL:     return "]";
            case DEL_ABRE_CHA:      return "{";
            case DEL_FECHA_CHA:     return "}";
            case DEL_VIRGULA:       return ",";
            case DEL_PONTO:         return ".";
            case DEL_PONTO_VIRGULA: return ";";
            case DEL_DOIS_PONTOS:   return ":";
            default:                return null;
        }
    }
}

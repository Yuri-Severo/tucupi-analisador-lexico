/* ------------------------------------------------------------------------
 * tucupi.flex -- Especificacao JFlex do analisador lexico da linguagem Tucupi
 *
 * Disciplina: Automatos e Compiladores -- CESUPA
 * Gerador...: JFlex 1.9.1 (https://jflex.de)
 *
 * ESTRUTURA DE UM ARQUIVO .flex (tres secoes separadas por %%):
 *   1) codigo de usuario  -> copiado literalmente antes da classe gerada
 *   2) opcoes e declaracoes -> diretivas %, macros e estados lexicos
 *   3) regras lexicas     -> expressao regular + acao Java
 * ------------------------------------------------------------------------ */

/* =========================== 1. CODIGO DE USUARIO ======================== */

package br.edu.cesupa.tucupi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

%%

/* ==================== 2. OPCOES E DECLARACOES ============================ */

%public
%final
%class TucupiLexer
%unicode
%line
%column
%type Token
%function proximoToken

/* Estados exclusivos: dentro deles, SO valem as regras marcadas com o estado.
   Sao a forma de o JFlex tratar construcoes que uma unica expressao regular
   nao descreve bem -- literais de texto com escapes e comentarios aninhados. */
%xstate TEXTO, COMENTARIO

%{
  /* ---- estado auxiliar mantido entre chamadas de proximoToken() ---- */

  private final List<ErroLexico> erros = new ArrayList<>();
  private final TabelaDeSimbolos tabela = new TabelaDeSimbolos();

  /** Acumula o conteudo ja "desescapado" de um literal de texto. */
  private final StringBuilder buffer = new StringBuilder();

  /** Posicao de abertura do texto/comentario em curso (para mensagens de erro). */
  private int inicioLinha;
  private int inicioColuna;

  /** Profundidade de aninhamento de comentarios de bloco.
      Expressoes regulares nao contam aninhamento (nao sao livres de contexto);
      um contador inteiro resolve o caso sem sair do analisador lexico. */
  private int profundidade = 0;

  /* ---- contadores para o relatorio de estatisticas ---- */
  private int comentariosLinha = 0;
  private int comentariosBloco = 0;

  public List<ErroLexico> erros()        { return erros; }
  public TabelaDeSimbolos tabela()       { return tabela; }
  public int comentariosDeLinha()        { return comentariosLinha; }
  public int comentariosDeBloco()        { return comentariosBloco; }

  /* ---- fabricas de token ---- */

  /** Token cujo lexema e o proprio texto casado, sem valor semantico. */
  private Token tk(TokenType tipo) {
    return new Token(tipo, yytext(), null, yyline + 1, yycolumn + 1);
  }

  /** Token com valor semantico ja convertido. */
  private Token tk(TokenType tipo, Object valor) {
    return new Token(tipo, yytext(), valor, yyline + 1, yycolumn + 1);
  }

  private void erro(String mensagem, String lexema, int linha, int coluna) {
    erros.add(new ErroLexico(mensagem, lexema, linha, coluna));
  }

  private void erroAqui(String mensagem) {
    erro(mensagem, yytext(), yyline + 1, yycolumn + 1);
  }

  /* ---- conversao de literais numericos ---- */

  /**
   * Converte um literal inteiro em qualquer base aceita pela linguagem.
   * Usa BigInteger para detectar estouro da faixa de 32 bits -- um erro que o
   * analisador lexico consegue reportar com precisao, pois ja tem o lexema
   * completo e a posicao exata.
   */
  private Token inteiro(String lexema, String digitos, int base) {
    if (digitos.isEmpty()) {
      erroAqui("literal inteiro sem digitos apos o prefixo de base");
      return new Token(TokenType.LIT_INTEIRO, lexema, 0, yyline + 1, yycolumn + 1);
    }
    BigInteger valor = new BigInteger(digitos, base);
    if (valor.bitLength() > 31) {
      erro("literal inteiro fora da faixa de 32 bits com sinal", lexema,
           yyline + 1, yycolumn + 1);
      return new Token(TokenType.LIT_INTEIRO, lexema, 0, yyline + 1, yycolumn + 1);
    }
    return new Token(TokenType.LIT_INTEIRO, lexema, valor.intValue(),
                     yyline + 1, yycolumn + 1);
  }

  /** Remove os separadores "_" e recusa separador em posicao invalida. */
  private String digitosDe(String bruto) {
    if (bruto.endsWith("_") || bruto.startsWith("_") || bruto.contains("__")) {
      erroAqui("separador \"_\" em posicao invalida no literal numerico");
    }
    return bruto.replace("_", "");
  }
%}

/* ============================== MACROS =================================== */

QuebraLinha   = \r|\n|\r\n
Espaco        = [ \t\f]
Branco        = {QuebraLinha} | {Espaco}

Digito        = [0-9]
DigitosDec    = {Digito} ({Digito} | _)*
HexDig        = [0-9a-fA-F]
DigitosHex    = {HexDig} ({HexDig} | _)*
DigitosBin    = [01] ([01] | _)*
Hex4          = {HexDig} {HexDig} {HexDig} {HexDig}

Expoente      = [eE] [+\-]? {Digito}+
Real          = {DigitosDec} "." {DigitosDec}? {Expoente}?
              | {DigitosDec} {Expoente}

/* [:jletter:] e [:jletterdigit:] usam Character.isJavaIdentifierStart/Part,
   entao "vazao", "preco_medio" e "temperatura_C" sao identificadores validos
   -- inclusive com acentuacao, gracas a %unicode. */
Identificador = [:jletter:] [:jletterdigit:]*

ComentarioLinha = "//" [^\r\n]*

%%

/* ========================= 3. REGRAS LEXICAS ============================= */

<YYINITIAL> {

  /* ------------------- palavras reservadas -------------------------------
     Uma regra por palavra. Quando duas regras casam o MESMO numero de
     caracteres, o JFlex escolhe a que aparece PRIMEIRO na especificacao --
     por isso as palavras reservadas vem antes da regra de identificador. */

  "programa"     { return tk(TokenType.PR_PROGRAMA); }
  "var"          { return tk(TokenType.PR_VAR); }
  "constante"    { return tk(TokenType.PR_CONSTANTE); }
  "inteiro"      { return tk(TokenType.PR_INTEIRO); }
  "real"         { return tk(TokenType.PR_REAL); }
  "texto"        { return tk(TokenType.PR_TEXTO); }
  "caractere"    { return tk(TokenType.PR_CARACTERE); }
  "logico"       { return tk(TokenType.PR_LOGICO); }
  "se"           { return tk(TokenType.PR_SE); }
  "senao"        { return tk(TokenType.PR_SENAO); }
  "enquanto"     { return tk(TokenType.PR_ENQUANTO); }
  "para"         { return tk(TokenType.PR_PARA); }
  "ate"          { return tk(TokenType.PR_ATE); }
  "passo"        { return tk(TokenType.PR_PASSO); }
  "funcao"       { return tk(TokenType.PR_FUNCAO); }
  "retorne"      { return tk(TokenType.PR_RETORNE); }
  "leia"         { return tk(TokenType.PR_LEIA); }
  "escreva"      { return tk(TokenType.PR_ESCREVA); }
  "nulo"         { return tk(TokenType.PR_NULO); }

  "verdadeiro"   { return tk(TokenType.LIT_LOGICO, Boolean.TRUE); }
  "falso"        { return tk(TokenType.LIT_LOGICO, Boolean.FALSE); }

  /* ------------------- literais numericos --------------------------------

     O caso "1..10": pelo principio do casamento mais longo, o JFlex preferiria
     ler "1." como literal real e sobraria ".10". A regra abaixo casa
     "1.." (3 caracteres, mais longo ainda) e devolve os dois pontos ao fluxo
     de entrada com yypushback(2). O resultado e a sequencia correta:
     LIT_INTEIRO(1) OP_INTERVALO LIT_INTEIRO(10). */

  {DigitosDec} ".."   { yypushback(2);
                        String lex = yytext();
                        return inteiro(lex, digitosDe(lex), 10); }

  0 [xX] {DigitosHex} { String lex = yytext();
                        return inteiro(lex, digitosDe(lex.substring(2)), 16); }

  0 [bB] {DigitosBin} { String lex = yytext();
                        return inteiro(lex, digitosDe(lex.substring(2)), 2); }

  /* Prefixo de base sem nenhum digito. Casa 2 caracteres, o mesmo que a regra
     de "identificador comecando com digito" logo abaixo; como as duas empatam
     no comprimento, vale a que aparece PRIMEIRO -- e a mensagem de erro fica
     especifica em vez de generica. */
  0 [xX]              { erroAqui("literal inteiro sem digitos apos o prefixo hexadecimal");
                        return tk(TokenType.ERRO); }
  0 [bB]              { erroAqui("literal inteiro sem digitos apos o prefixo binario");
                        return tk(TokenType.ERRO); }

  {Real}              { String lex = yytext();
                        return tk(TokenType.LIT_REAL,
                                  Double.valueOf(digitosDe(lex))); }

  {DigitosDec}        { String lex = yytext();
                        return inteiro(lex, digitosDe(lex), 10); }

  /* Erro classico: identificador comecando com digito ("2vias").
     Sem esta regra, o analisador emitiria LIT_INTEIRO seguido de
     IDENTIFICADOR e o erro so apareceria na analise sintatica. */
  {Digito} ({Digito} | _)* {Identificador}
                      { erroAqui("identificador nao pode comecar com digito");
                        return tk(TokenType.ERRO); }

  /* ------------------- literal de caractere ------------------------------ */

  ' [^\\'\r\n] '      { return tk(TokenType.LIT_CARACTERE,
                                  Character.valueOf(yytext().charAt(1))); }
  ' \\ [ntr0\\'\"] '  { char c = yytext().charAt(2);
                        char v;
                        switch (c) {
                          case 'n': v = '\n'; break;
                          case 't': v = '\t'; break;
                          case 'r': v = '\r'; break;
                          case '0': v = '\0'; break;
                          default:  v = c;
                        }
                        return tk(TokenType.LIT_CARACTERE, Character.valueOf(v)); }
  ' \\ u {Hex4} '     { char v = (char) Integer.parseInt(yytext().substring(3, 7), 16);
                        return tk(TokenType.LIT_CARACTERE, Character.valueOf(v)); }
  ' [^\r\n']* '       { erroAqui("literal de caractere invalido");
                        return tk(TokenType.ERRO); }

  /* ------------------- inicio de literal de texto ------------------------ */

  \"                  { buffer.setLength(0);
                        inicioLinha  = yyline + 1;
                        inicioColuna = yycolumn + 1;
                        yybegin(TEXTO); }

  /* ------------------- comentarios --------------------------------------- */

  {ComentarioLinha}   { comentariosLinha++; }

  "/*"                { profundidade   = 1;
                        inicioLinha    = yyline + 1;
                        inicioColuna   = yycolumn + 1;
                        comentariosBloco++;
                        yybegin(COMENTARIO); }

  "*/"                { erroAqui("fechamento de comentario sem abertura correspondente");
                        return tk(TokenType.ERRO); }

  /* ------------------- operadores ----------------------------------------

     A familia "<", "<=", "<<", "<<=" existe para demonstrar o casamento mais
     longo: o automato finito nao para no primeiro casamento possivel; ele
     avanca ate o maior prefixo aceito e so entao dispara a acao. */

  "<<="  { return tk(TokenType.OP_DESLOCA_ESQ_ATRIBUI); }
  ">>="  { return tk(TokenType.OP_DESLOCA_DIR_ATRIBUI); }
  "<<"   { return tk(TokenType.OP_DESLOCA_ESQ); }
  ">>"   { return tk(TokenType.OP_DESLOCA_DIR); }
  "<="   { return tk(TokenType.OP_MENOR_IGUAL); }
  ">="   { return tk(TokenType.OP_MAIOR_IGUAL); }
  "<"    { return tk(TokenType.OP_MENOR); }
  ">"    { return tk(TokenType.OP_MAIOR); }

  "**"   { return tk(TokenType.OP_POTENCIA); }
  "+="   { return tk(TokenType.OP_MAIS_ATRIBUI); }
  "-="   { return tk(TokenType.OP_MENOS_ATRIBUI); }
  "*="   { return tk(TokenType.OP_VEZES_ATRIBUI); }
  "/="   { return tk(TokenType.OP_DIVIDE_ATRIBUI); }
  "%="   { return tk(TokenType.OP_RESTO_ATRIBUI); }
  "=="   { return tk(TokenType.OP_IGUAL); }
  "!="   { return tk(TokenType.OP_DIFERENTE); }
  "&&"   { return tk(TokenType.OP_E_LOGICO); }
  "||"   { return tk(TokenType.OP_OU_LOGICO); }
  "->"   { return tk(TokenType.OP_SETA); }
  ".."   { return tk(TokenType.OP_INTERVALO); }

  "+"    { return tk(TokenType.OP_MAIS); }
  "-"    { return tk(TokenType.OP_MENOS); }
  "*"    { return tk(TokenType.OP_VEZES); }
  "/"    { return tk(TokenType.OP_DIVIDE); }
  "%"    { return tk(TokenType.OP_RESTO); }
  "="    { return tk(TokenType.OP_ATRIBUI); }
  "!"    { return tk(TokenType.OP_NAO_LOGICO); }

  /* ------------------- delimitadores -------------------------------------- */

  "("    { return tk(TokenType.DEL_ABRE_PAR); }
  ")"    { return tk(TokenType.DEL_FECHA_PAR); }
  "["    { return tk(TokenType.DEL_ABRE_COL); }
  "]"    { return tk(TokenType.DEL_FECHA_COL); }
  "{"    { return tk(TokenType.DEL_ABRE_CHA); }
  "}"    { return tk(TokenType.DEL_FECHA_CHA); }
  ","    { return tk(TokenType.DEL_VIRGULA); }
  "."    { return tk(TokenType.DEL_PONTO); }
  ";"    { return tk(TokenType.DEL_PONTO_VIRGULA); }
  ":"    { return tk(TokenType.DEL_DOIS_PONTOS); }

  /* ------------------- identificadores ------------------------------------ */

  {Identificador} { tabela.inserir(yytext(), yyline + 1, yycolumn + 1);
                    return tk(TokenType.IDENTIFICADOR, yytext()); }

  /* ------------------- espacos em branco ---------------------------------- */

  {Branco}+ { /* descartados: nao geram token nesta linguagem */ }

  <<EOF>>   { return new Token(TokenType.EOF, "<EOF>", null,
                               yyline + 1, yycolumn + 1); }
}

/* ===================== ESTADO: LITERAL DE TEXTO ==========================
   Estado exclusivo. Cada fragmento reconhecido e acumulado no buffer ja
   convertido, de modo que o token final carrega o VALOR do texto, e nao o
   texto-fonte com as barras invertidas.                                    */

<TEXTO> {

  \"                { yybegin(YYINITIAL);
                      return new Token(TokenType.LIT_TEXTO,
                                       "\"" + buffer + "\"",
                                       buffer.toString(), inicioLinha, inicioColuna); }

  [^\r\n\"\\]+      { buffer.append(yytext()); }

  \\n               { buffer.append('\n'); }
  \\t               { buffer.append('\t'); }
  \\r               { buffer.append('\r'); }
  \\0               { buffer.append('\0'); }
  \\\\              { buffer.append('\\'); }
  \\\"              { buffer.append('"');  }
  \\'               { buffer.append('\''); }
  \\u {Hex4}        { buffer.append((char) Integer.parseInt(yytext().substring(2), 16)); }

  \\u               { erroAqui("escape unicode incompleto (esperados 4 digitos hexadecimais)");
                      buffer.append("\\u"); }

  \\ [^]            { erroAqui("sequencia de escape desconhecida");
                      buffer.append(yytext().charAt(1)); }

  /* Quebra de linha dentro de texto: erro recuperavel. O analisador fecha o
     literal, reporta a posicao de ABERTURA e volta ao estado inicial, para
     que o resto do arquivo continue sendo analisado. */
  {QuebraLinha}     { erro("literal de texto nao terminado antes do fim da linha",
                           "\"" + buffer, inicioLinha, inicioColuna);
                      yybegin(YYINITIAL);
                      return new Token(TokenType.ERRO, buffer.toString(), null,
                                       inicioLinha, inicioColuna); }

  <<EOF>>           { erro("literal de texto nao terminado ate o fim do arquivo",
                           "\"" + buffer, inicioLinha, inicioColuna);
                      yybegin(YYINITIAL);
                      return new Token(TokenType.EOF, "<EOF>", null,
                                       yyline + 1, yycolumn + 1); }
}

/* ==================== ESTADO: COMENTARIO DE BLOCO ========================
   Comentarios aninhados nao sao uma linguagem regular. A saida e manter um
   contador de profundidade: o estado lexico cuida do "onde estou" e o inteiro
   cuida do "quantos niveis abaixo".                                        */

<COMENTARIO> {

  "/*"          { profundidade++; comentariosBloco++; }

  "*/"          { profundidade--;
                  if (profundidade == 0) { yybegin(YYINITIAL); } }

  [^]           { /* qualquer outro caractere e ignorado */ }

  <<EOF>>       { erro("comentario de bloco aberto e nao fechado ate o fim do arquivo",
                       "/*", inicioLinha, inicioColuna);
                  yybegin(YYINITIAL);
                  return new Token(TokenType.EOF, "<EOF>", null,
                                   yyline + 1, yycolumn + 1); }
}

/* ===================== REGRA CORINGA (RECUPERACAO) =======================
   Ultima regra da especificacao: casa qualquer caractere que nenhuma outra
   regra reconheceu. Sem ela, o JFlex encerraria a execucao no primeiro
   caractere invalido; com ela, o analisador reporta o erro e continua,
   listando todos os problemas do arquivo em uma unica passagem.            */

[^] { erroAqui("caractere invalido na linguagem");
      return tk(TokenType.ERRO); }

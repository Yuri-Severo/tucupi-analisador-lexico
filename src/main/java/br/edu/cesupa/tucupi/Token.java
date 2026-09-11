package br.edu.cesupa.tucupi;

/**
 * Unidade lexica reconhecida pelo analisador.
 *
 * <p>Alem do par classico (tipo, lexema), o token guarda o <em>valor
 * semantico</em> ja convertido (por exemplo, o {@code Integer} 255 para o
 * lexema {@code 0xFF}) e a posicao de inicio no arquivo-fonte. Guardar o valor
 * convertido no proprio token e uma decisao de projeto: evita que o parser
 * precise reinterpretar o texto e permite que o analisador lexico ja reporte
 * erros como estouro de faixa numerica.</p>
 */
public final class Token {

    private final TokenType tipo;
    private final String lexema;
    private final Object valor;
    private final int linha;
    private final int coluna;

    public Token(TokenType tipo, String lexema, Object valor, int linha, int coluna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.valor = valor;
        this.linha = linha;
        this.coluna = coluna;
    }

    public TokenType tipo() {
        return tipo;
    }

    public String lexema() {
        return lexema;
    }

    /** Valor ja convertido (Integer, Double, String, Character, Boolean) ou {@code null}. */
    public Object valor() {
        return valor;
    }

    /** Linha de inicio do token, contada a partir de 1. */
    public int linha() {
        return linha;
    }

    /** Coluna de inicio do token, contada a partir de 1. */
    public int coluna() {
        return coluna;
    }

    /** Lexema com caracteres de controle visiveis, para exibicao em tabela. */
    public String lexemaVisivel() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lexema.length(); i++) {
            char c = lexema.charAt(i);
            switch (c) {
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("<%s, \"%s\", %d:%d>", tipo, lexemaVisivel(), linha, coluna);
    }
}

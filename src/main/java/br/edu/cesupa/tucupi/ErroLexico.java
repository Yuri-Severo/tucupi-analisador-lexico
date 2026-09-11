package br.edu.cesupa.tucupi;

/**
 * Erro lexico registrado sem interromper a varredura.
 *
 * <p>O comportamento padrao do JFlex quando nenhuma regra casa e abortar. Aqui
 * usamos a regra coringa {@code [^]} para capturar o caractere invalido,
 * registrar o erro e continuar &mdash; o que permite reportar todos os
 * problemas do arquivo em uma unica execucao (recuperacao em modo panico no
 * nivel lexico).</p>
 */
public final class ErroLexico {

    private final String mensagem;
    private final String lexema;
    private final int linha;
    private final int coluna;

    public ErroLexico(String mensagem, String lexema, int linha, int coluna) {
        this.mensagem = mensagem;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
    }

    public String mensagem() {
        return mensagem;
    }

    public String lexema() {
        return lexema;
    }

    public int linha() {
        return linha;
    }

    public int coluna() {
        return coluna;
    }

    @Override
    public String toString() {
        return String.format("linha %d, coluna %d: %s (encontrado: \"%s\")",
                linha, coluna, mensagem, lexema);
    }
}

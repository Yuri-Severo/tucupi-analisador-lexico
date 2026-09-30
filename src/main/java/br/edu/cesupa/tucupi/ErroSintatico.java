package br.edu.cesupa.tucupi;

/**
 * Erro sintatico: a sequencia de tokens nao pertence a linguagem gerada pela
 * gramatica ({@code docs/gramatica.ebnf}).
 *
 * <p>Nesta etapa o analisador para no primeiro erro (sem recuperacao), por
 * isso o erro e uma excecao: ela desfaz toda a pilha de chamadas da descida
 * recursiva de uma vez so.</p>
 *
 * <p>Alem do token encontrado e do que era esperado, o erro guarda:</p>
 * <ul>
 *   <li>o token <em>anterior</em>, porque num {@code ;} ausente o culpado
 *       esta no fim da linha anterior, e nao no token onde o erro e
 *       percebido;</li>
 *   <li>a pilha de producoes ativas no momento do erro (por exemplo
 *       {@code programa > funcao > bloco > comando > declaracao}).</li>
 * </ul>
 */
public final class ErroSintatico extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Token encontrado;
    private final String esperado;
    private final transient Token anterior;
    private final String producoes;

    public ErroSintatico(String mensagem, Token encontrado, String esperado,
                         Token anterior, String producoes) {
        super(mensagem);
        this.encontrado = encontrado;
        this.esperado = esperado;
        this.anterior = anterior;
        this.producoes = producoes;
    }

    public Token encontrado() {
        return encontrado;
    }

    public String esperado() {
        return esperado;
    }

    /** Ultimo token aceito antes do erro, ou {@code null} se o erro ocorreu no inicio. */
    public Token anterior() {
        return anterior;
    }

    public String producoes() {
        return producoes;
    }

    public int linha() {
        return encontrado.linha();
    }

    public int coluna() {
        return encontrado.coluna();
    }

    /** Descricao "TIPO ("lexema")" de um token, usada nas mensagens. */
    static String descrever(Token t) {
        if (t == null) {
            return "(nenhum)";
        }
        if (t.tipo() == TokenType.EOF) {
            return "EOF (fim do arquivo)";
        }
        return t.tipo() + " (\"" + t.lexemaVisivel() + "\")";
    }

    /** Relatorio de varias linhas, no formato exibido pelo {@link MainSintatico}. */
    public String relatorio() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Erro sintático em %d:%d%n", linha(), coluna()));
        sb.append("Mensagem..: ").append(getMessage()).append(System.lineSeparator());
        sb.append("Esperado..: ").append(esperado).append(System.lineSeparator());
        sb.append("Encontrado: ").append(descrever(encontrado)).append(System.lineSeparator());
        if (anterior != null) {
            sb.append(String.format("Após......: %s em %d:%d%n",
                    descrever(anterior), anterior.linha(), anterior.coluna()));
        }
        sb.append("Produções.: ").append(producoes);
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Erro sintático em %d:%d -- esperado %s, encontrado %s",
                linha(), coluna(), esperado, descrever(encontrado));
    }
}

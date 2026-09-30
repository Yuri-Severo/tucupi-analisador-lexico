package br.edu.cesupa.tucupi;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface de linha de comando da etapa sintatica.
 *
 * <pre>
 *   arquivo .tcp -&gt; TucupiLexer (JFlex) -&gt; AnalisadorSintatico -&gt; arvore -&gt; ACEITO / REJEITADO
 *
 *   java -cp out br.edu.cesupa.tucupi.MainSintatico exemplos/04_sintatico_valido.tcp
 *   java -cp out br.edu.cesupa.tucupi.MainSintatico --tokens exemplos/04_sintatico_valido.tcp
 *   java -cp out br.edu.cesupa.tucupi.MainSintatico --ascii exemplos/05_sintatico_erro.tcp
 * </pre>
 *
 * <p>Codigo de saida: 0 = aceito; 1 = rejeitado (erro lexico ou sintatico);
 * 2 = uso incorreto.</p>
 */
public final class MainSintatico {

    private static final String LINHA =
            "==========================================================================";
    private static final String TRACO =
            "--------------------------------------------------------------------------";

    public static void main(String[] args) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, StandardCharsets.UTF_8);
        System.setOut(out);

        boolean ascii = false;
        boolean tokens = false;
        String arquivo = null;
        for (String a : args) {
            switch (a) {
                case "--ascii":
                    ascii = true;
                    break;
                case "--tokens":
                    tokens = true;
                    break;
                case "-h":
                case "--ajuda":
                    uso(out);
                    return;
                default:
                    arquivo = a;
            }
        }
        if (arquivo == null) {
            uso(out);
            System.exit(2);
        }

        Path caminho = Path.of(arquivo);
        String fonte = Files.readString(caminho, StandardCharsets.UTF_8);
        System.exit(analisar(caminho.toString().replace('\\', '/'), fonte, ascii, tokens, out) ? 0 : 1);
    }

    /**
     * Executa lexer + parser sobre o fonte e imprime o relatorio.
     *
     * @return {@code true} se o programa foi aceito (sem erros lexicos nem sintaticos)
     */
    public static boolean analisar(String nome, String fonte, boolean ascii, PrintStream out) {
        return analisar(nome, fonte, ascii, false, out);
    }

    public static boolean analisar(String nome, String fonte, boolean ascii,
                                   boolean mostrarTokens, PrintStream out) {
        out.println(LINHA);
        out.println(" ANALISADOR SINTÁTICO TUCUPI");
        out.println(" Léxico: TucupiLexer (JFlex 1.9.1) | Sintático: descida recursiva LL(1)");
        out.println(" Arquivo: " + nome);
        out.println(LINHA);

        if (mostrarTokens) {
            imprimirTokens(fonte, out);
        }

        TucupiLexer lexer = new TucupiLexer(new StringReader(fonte));
        NoSintatico raiz = null;
        ErroSintatico erro = null;
        AnalisadorSintatico parser = null;

        try {
            parser = new AnalisadorSintatico(lexer);
            raiz = parser.programa();
        } catch (ErroSintatico e) {
            erro = e;
        }

        List<ErroLexico> errosLexicos = lexer.erros();

        if (raiz != null) {
            out.println();
            out.println("ÁRVORE SINTÁTICA");
            out.println(TRACO);
            new ImpressoraArvore(ascii).imprimir(raiz, out);
        }

        if (!errosLexicos.isEmpty()) {
            out.println();
            out.println("ERROS LÉXICOS (" + errosLexicos.size() + ")");
            out.println(TRACO);
            for (ErroLexico e : errosLexicos) {
                out.println("  " + e);
            }
        }

        if (erro != null) {
            out.println();
            out.println(erro.encontrado().tipo() == TokenType.ERRO
                    ? "ANÁLISE INTERROMPIDA POR SÍMBOLO INVÁLIDO (ERRO LÉXICO)"
                    : "ERRO SINTÁTICO");
            out.println(TRACO);
            out.println(erro.relatorio());
            out.println();
            mostrarTrecho(fonte, erro, out);
        }

        boolean aceito = erro == null && errosLexicos.isEmpty();

        out.println();
        out.println(TRACO);
        if (parser != null) {
            out.println("Tokens consumidos pelo parser: " + parser.tokensConsumidos());
        }
        if (raiz != null) {
            out.println("Nós na árvore sintática.....: " + raiz.tamanho());
            Token principal = parser.pontoDeEntrada();
            out.println("Ponto de entrada............: " + (principal == null
                    ? "(aviso) função \"principal\" não encontrada"
                    : "função principal, linha " + principal.linha()));
        }
        if (aceito) {
            out.println("Resultado: ACEITO — a entrada pertence à linguagem gerada pela GLC.");
        } else if (erro != null && erro.encontrado().tipo() == TokenType.ERRO) {
            out.println("Resultado: REJEITADO — símbolo inválido (erro léxico) em "
                    + erro.linha() + ":" + erro.coluna() + "; o parser parou nesse ponto.");
        } else if (erro != null) {
            out.println("Resultado: REJEITADO — erro sintático em "
                    + erro.linha() + ":" + erro.coluna() + ".");
        } else {
            out.println("Resultado: REJEITADO — estrutura sintática válida, mas há "
                    + errosLexicos.size() + " erro(s) léxico(s).");
        }
        out.println(LINHA);
        return aceito;
    }

    /**
     * Etapa 1 isolada: roda o lexer sobre o fonte inteiro e mostra os tokens
     * agrupados pela linha em que comecam. Usa um lexer proprio, separado do
     * que alimenta o parser, para que a listagem apareca completa mesmo quando
     * a analise sintatica para no meio do arquivo.
     */
    private static void imprimirTokens(String fonte, PrintStream out) {
        out.println();
        out.println("TOKENS (etapa léxica — TucupiLexer)");
        out.println(TRACO);
        TucupiLexer lexer = new TucupiLexer(new StringReader(fonte));
        int linhaAtual = -1;
        int total = 0;
        StringBuilder linha = new StringBuilder();
        try {
            Token t;
            while ((t = lexer.proximoToken()).tipo() != TokenType.EOF) {
                if (t.linha() != linhaAtual) {
                    if (linha.length() > 0) {
                        out.println(linha);
                    }
                    linha.setLength(0);
                    linha.append(String.format("%4d | ", t.linha()));
                    linhaAtual = t.linha();
                } else {
                    linha.append(' ');
                }
                linha.append(t.tipo());
                if (t.tipo() == TokenType.IDENTIFICADOR
                        || t.tipo().categoria() == TokenType.Categoria.LITERAL
                        || t.tipo() == TokenType.ERRO) {
                    linha.append('(').append(t.lexemaVisivel()).append(')');
                }
                total++;
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        if (linha.length() > 0) {
            out.println(linha);
        }
        out.println("     | EOF   (" + total + " tokens, "
                + lexer.erros().size() + " erro(s) léxico(s))");
    }

    /**
     * Mostra a linha do erro com um marcador na coluna. Se o ultimo token
     * aceito estiver em outra linha (caso tipico do ";" esquecido), mostra
     * tambem essa linha, marcando o ponto logo apos o token.
     */
    private static void mostrarTrecho(String fonte, ErroSintatico erro, PrintStream out) {
        String[] linhas = fonte.split("\r\n|\r|\n", -1);
        Token anterior = erro.anterior();
        if (anterior != null && anterior.linha() != erro.linha()) {
            int fim = anterior.coluna() + anterior.lexema().length();
            imprimirLinha(linhas, anterior.linha(), fim, "após este ponto", out);
            if (erro.linha() - anterior.linha() > 1) {
                out.println("     :");
            }
        }
        imprimirLinha(linhas, erro.linha(), erro.coluna(), "erro detectado aqui", out);
    }

    private static void imprimirLinha(String[] linhas, int numero, int coluna,
                                      String legenda, PrintStream out) {
        if (numero < 1 || numero > linhas.length) {
            return;
        }
        String texto = linhas[numero - 1];
        out.printf("%4d | %s%n", numero, texto);
        StringBuilder marcador = new StringBuilder();
        for (int i = 0; i < coluna - 1; i++) {
            // preserva tabulacoes para que o ^ fique alinhado
            marcador.append(i < texto.length() && texto.charAt(i) == '\t' ? '\t' : ' ');
        }
        out.printf("     | %s^ %s%n", marcador, legenda);
    }

    private static void uso(PrintStream out) {
        out.println("""
                Uso: java -cp out br.edu.cesupa.tucupi.MainSintatico [opcoes] <arquivo.tcp>

                Opcoes:
                  --tokens      mostra antes os tokens produzidos pelo lexer, por linha
                  --ascii       desenha a arvore com |-- e `-- (terminais sem UTF-8)
                  -h, --ajuda   mostra esta ajuda

                Codigo de saida: 0 = ACEITO, 1 = REJEITADO, 2 = uso incorreto.""");
    }
}

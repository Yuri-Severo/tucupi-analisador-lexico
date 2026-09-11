package br.edu.cesupa.tucupi;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface de linha de comando do analisador lexico Tucupi.
 *
 * <pre>
 *   java -cp out br.edu.cesupa.tucupi.Main exemplos/valido.tcp
 *   java -cp out br.edu.cesupa.tucupi.Main --json exemplos/valido.tcp
 *   java -cp out br.edu.cesupa.tucupi.Main --bench 200000
 * </pre>
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        // Garante que a saida saia em UTF-8 mesmo em terminais cuja
        // codificacao padrao seja outra -- os exemplos usam acentuacao e
        // simbolos fora do ASCII de proposito.
        System.setOut(new java.io.PrintStream(
                new java.io.FileOutputStream(java.io.FileDescriptor.out),
                true, StandardCharsets.UTF_8));

        if (args.length == 0) {
            uso();
            System.exit(2);
        }

        boolean json = false;
        boolean semTabela = false;
        String arquivo = null;
        int repeticoesBench = -1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--json":
                    json = true;
                    break;
                case "--sem-tabela":
                    semTabela = true;
                    break;
                case "--bench":
                    repeticoesBench = Integer.parseInt(args[++i]);
                    break;
                case "-h":
                case "--ajuda":
                    uso();
                    return;
                default:
                    arquivo = args[i];
            }
        }

        if (repeticoesBench > 0) {
            benchmark(repeticoesBench);
            return;
        }

        if (arquivo == null) {
            uso();
            System.exit(2);
        }

        Path caminho = Path.of(arquivo);
        String fonte = Files.readString(caminho, StandardCharsets.UTF_8);

        System.out.println("==========================================================================");
        System.out.println(" ANALISADOR LEXICO TUCUPI -- gerado com JFlex 1.9.1");
        System.out.println(" Arquivo: " + caminho);
        System.out.println("==========================================================================");

        long inicio = System.nanoTime();
        Resultado r = analisar(new StringReader(fonte));
        long nanos = System.nanoTime() - inicio;

        if (json) {
            Relatorio.json(System.out, r.tokens, r.lexer.erros());
        } else {
            Relatorio.tabelaDeTokens(System.out, r.tokens);
            Relatorio.erros(System.out, r.lexer.erros());
            if (!semTabela) {
                Relatorio.tabelaDeSimbolos(System.out, r.lexer.tabela());
            }
            Relatorio.estatisticas(System.out, r.tokens, r.lexer, nanos, fonte.length());
        }

        System.exit(r.lexer.erros().isEmpty() ? 0 : 1);
    }

    /** Resultado de uma varredura completa. */
    private static final class Resultado {
        final List<Token> tokens;
        final TucupiLexer lexer;

        Resultado(List<Token> tokens, TucupiLexer lexer) {
            this.tokens = tokens;
            this.lexer = lexer;
        }
    }

    /**
     * Laco principal: chama {@code proximoToken()} ate receber EOF.
     * E exatamente esse laco que um analisador sintatico executaria, pedindo
     * um token por vez em vez de guardar todos em memoria.
     */
    private static Resultado analisar(Reader entrada) throws IOException {
        TucupiLexer lexer = new TucupiLexer(entrada);
        List<Token> tokens = new ArrayList<>();
        Token t;
        do {
            t = lexer.proximoToken();
            tokens.add(t);
        } while (t.tipo() != TokenType.EOF);
        return new Resultado(tokens, lexer);
    }

    /**
     * Mede a vazao do analisador sobre um fonte sintetico repetido N vezes.
     * Inclui um aquecimento para que a JIT ja tenha compilado o automato
     * antes da medicao.
     */
    private static void benchmark(int repeticoes) throws IOException {
        String bloco = """
                funcao area(base: real, altura: real) -> real {
                  var resultado: real = base * altura / 2.0;
                  se (resultado > 1_000.0 && base != 0) { escreva("grande"); }
                  para i = 1..10 passo 1 { resultado += i << 2; }
                  retorne resultado;  // comentario de linha
                }
                """;
        StringBuilder sb = new StringBuilder(bloco.length() * repeticoes);
        for (int i = 0; i < repeticoes; i++) {
            sb.append(bloco);
        }
        String fonte = sb.toString();

        System.out.println("Benchmark: " + repeticoes + " blocos, "
                + fonte.length() + " caracteres.");

        for (int i = 0; i < 3; i++) {
            analisar(new StringReader(fonte));
        }

        long melhor = Long.MAX_VALUE;
        int tokens = 0;
        for (int i = 0; i < 5; i++) {
            long inicio = System.nanoTime();
            Resultado r = analisar(new StringReader(fonte));
            long dt = System.nanoTime() - inicio;
            melhor = Math.min(melhor, dt);
            tokens = r.tokens.size() - 1;
        }

        double segundos = melhor / 1_000_000_000.0;
        System.out.printf("Tokens.......: %d%n", tokens);
        System.out.printf("Melhor tempo.: %.1f ms%n", melhor / 1_000_000.0);
        System.out.printf("Vazao........: %.0f tokens/s | %.1f MB/s%n",
                tokens / segundos,
                (fonte.length() / (1024.0 * 1024.0)) / segundos);
    }

    private static void uso() {
        System.out.println("""
                Uso: java -cp out br.edu.cesupa.tucupi.Main [opcoes] <arquivo.tcp>

                Opcoes:
                  --json           imprime tokens e erros em JSON
                  --sem-tabela     omite a tabela de simbolos
                  --bench <n>      executa o benchmark com <n> blocos sinteticos
                  -h, --ajuda      mostra esta ajuda

                Codigo de saida: 0 = sem erros lexicos, 1 = com erros lexicos.""");
    }
}

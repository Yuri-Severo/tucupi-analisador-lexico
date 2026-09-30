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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Testes automatizados do analisador sintatico, sem dependencias externas
 * (roda offline, sem JUnit).
 *
 * <ul>
 *   <li>{@code testes/sintatico/validos/*.tcp} devem ser ACEITOS. Se existir um
 *       arquivo {@code .arvore} com o mesmo nome, a arvore impressa (modo
 *       ASCII) deve ser identica a ele -- isso verifica precedencia e
 *       associatividade, nao so a aceitacao.</li>
 *   <li>{@code testes/sintatico/invalidos/*.tcp} devem ser REJEITADOS com erro
 *       sintatico na posicao indicada pelo comentario
 *       {@code // espera-erro: LINHA:COLUNA} na primeira linha.</li>
 * </ul>
 *
 * <pre>java -cp out br.edu.cesupa.tucupi.TestesSintaticos [diretorio-base]</pre>
 */
public final class TestesSintaticos {

    private static final Pattern ESPERA = Pattern.compile("espera-erro:\\s*(\\d+):(\\d+)");

    private static int passou = 0;
    private static int falhou = 0;

    public static void main(String[] args) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, StandardCharsets.UTF_8);
        Path base = Path.of(args.length > 0 ? args[0] : "testes/sintatico");

        out.println("== Casos válidos (devem ser ACEITOS) ==");
        for (Path p : listar(base.resolve("validos"))) {
            testarValido(p, out);
        }
        out.println();
        out.println("== Casos inválidos (devem ser REJEITADOS na posição indicada) ==");
        for (Path p : listar(base.resolve("invalidos"))) {
            testarInvalido(p, out);
        }
        out.println();
        out.printf("Resumo: %d passaram, %d falharam.%n", passou, falhou);
        System.exit(falhou == 0 ? 0 : 1);
    }

    private static List<Path> listar(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".tcp")).sorted().collect(Collectors.toList());
        }
    }

    private static void testarValido(Path p, PrintStream out) throws IOException {
        String fonte = Files.readString(p, StandardCharsets.UTF_8);
        TucupiLexer lexer = new TucupiLexer(new StringReader(fonte));
        try {
            NoSintatico raiz = new AnalisadorSintatico(lexer).programa();
            if (!lexer.erros().isEmpty()) {
                falha(out, p, "erro léxico inesperado: " + lexer.erros().get(0));
                return;
            }
            Path arvore = Path.of(p.toString().replaceAll("\\.tcp$", ".arvore"));
            if (Files.exists(arvore)) {
                String esperada = Files.readString(arvore, StandardCharsets.UTF_8).replace("\r\n", "\n");
                String obtida = new ImpressoraArvore(true).comoTexto(raiz);
                if (!esperada.equals(obtida)) {
                    falha(out, p, "árvore diferente da esperada em " + arvore.getFileName()
                            + "\n--- obtida ---\n" + obtida);
                    return;
                }
                ok(out, p, "aceito, árvore confere");
                return;
            }
            ok(out, p, "aceito");
        } catch (ErroSintatico e) {
            falha(out, p, "rejeitado: " + e);
        }
    }

    private static void testarInvalido(Path p, PrintStream out) throws IOException {
        String fonte = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = ESPERA.matcher(fonte);
        if (!m.find()) {
            falha(out, p, "arquivo sem o comentário \"// espera-erro: L:C\"");
            return;
        }
        int linha = Integer.parseInt(m.group(1));
        int coluna = Integer.parseInt(m.group(2));
        try {
            new AnalisadorSintatico(new TucupiLexer(new StringReader(fonte))).programa();
            falha(out, p, "foi ACEITO, mas deveria ser rejeitado");
        } catch (ErroSintatico e) {
            if (e.linha() == linha && e.coluna() == coluna) {
                ok(out, p, "rejeitado em " + linha + ":" + coluna
                        + " -- esperado " + e.esperado());
            } else {
                falha(out, p, "erro em " + e.linha() + ":" + e.coluna()
                        + ", esperado em " + linha + ":" + coluna + " (" + e + ")");
            }
        }
    }

    private static void ok(PrintStream out, Path p, String detalhe) {
        passou++;
        out.printf("  [PASSOU] %-34s %s%n", p.getFileName(), detalhe);
    }

    private static void falha(PrintStream out, Path p, String detalhe) {
        falhou++;
        out.printf("  [FALHOU] %-34s %s%n", p.getFileName(), detalhe);
    }
}

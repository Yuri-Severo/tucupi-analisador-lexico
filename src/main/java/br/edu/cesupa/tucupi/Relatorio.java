package br.edu.cesupa.tucupi;

import java.io.PrintStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Formatacao das saidas do analisador: tabela de tokens, erros, tabela de simbolos e estatisticas. */
public final class Relatorio {

    private Relatorio() { }

    private static final String LINHA =
            "+--------+------+------+----------------------+----------------------------+----------------------+";

    public static void tabelaDeTokens(PrintStream out, List<Token> tokens) {
        out.println();
        out.println("TABELA DE TOKENS");
        out.println(LINHA);
        out.printf("| %-6s | %-4s | %-4s | %-20s | %-26s | %-20s |%n",
                "#", "LIN", "COL", "LEXEMA", "TOKEN", "VALOR");
        out.println(LINHA);
        int i = 0;
        for (Token t : tokens) {
            if (t.tipo() == TokenType.EOF) {
                continue;
            }
            out.printf("| %-6d | %4d | %4d | %-20s | %-26s | %-20s |%n",
                    ++i,
                    t.linha(),
                    t.coluna(),
                    corta(t.lexemaVisivel(), 20),
                    t.tipo(),
                    corta(t.valor() == null ? "-" : visivel(String.valueOf(t.valor())), 20));
        }
        out.println(LINHA);
    }

    public static void erros(PrintStream out, List<ErroLexico> erros) {
        out.println();
        if (erros.isEmpty()) {
            out.println("ERROS LEXICOS: nenhum.");
            return;
        }
        out.println("ERROS LEXICOS (" + erros.size() + ")");
        out.println("--------------------------------------------------------------------------");
        for (ErroLexico e : erros) {
            out.printf("  [linha %d, coluna %d] %s%n", e.linha(), e.coluna(), e.mensagem());
            out.printf("      lexema: %s%n", e.lexema());
        }
        out.println("--------------------------------------------------------------------------");
    }

    public static void tabelaDeSimbolos(PrintStream out, TabelaDeSimbolos tabela) {
        out.println();
        out.println("TABELA DE SIMBOLOS (" + tabela.tamanho() + " identificadores distintos)");
        out.println("+--------+----------------------------+--------------------+------------+");
        out.printf("| %-6s | %-26s | %-18s | %-10s |%n",
                "ID", "NOME", "1a OCORRENCIA", "USOS");
        out.println("+--------+----------------------------+--------------------+------------+");
        for (TabelaDeSimbolos.Entrada e : tabela.entradas()) {
            out.printf("| %-6d | %-26s | %-18s | %-10d |%n",
                    e.indice(),
                    corta(e.nome(), 26),
                    "linha " + e.primeiraLinha() + ", col " + e.primeiraColuna(),
                    e.ocorrencias());
        }
        out.println("+--------+----------------------------+--------------------+------------+");
    }

    public static void estatisticas(PrintStream out,
                                    List<Token> tokens,
                                    TucupiLexer lexer,
                                    long nanos,
                                    long caracteres) {
        Map<TokenType.Categoria, Integer> porCategoria = new EnumMap<>(TokenType.Categoria.class);
        int total = 0;
        for (Token t : tokens) {
            if (t.tipo() == TokenType.EOF) {
                continue;
            }
            total++;
            porCategoria.merge(t.tipo().categoria(), 1, Integer::sum);
        }

        out.println();
        out.println("ESTATISTICAS");
        out.println("--------------------------------------------------------------------------");
        out.printf("  Caracteres lidos.............: %d%n", caracteres);
        out.printf("  Tokens reconhecidos..........: %d%n", total);
        for (TokenType.Categoria c : TokenType.Categoria.values()) {
            int q = porCategoria.getOrDefault(c, 0);
            if (q > 0) {
                out.printf("    - %-24s: %4d (%5.1f%%)%n", c, q, 100.0 * q / total);
            }
        }
        out.printf("  Identificadores distintos....: %d%n", lexer.tabela().tamanho());
        out.printf("  Comentarios de linha.........: %d%n", lexer.comentariosDeLinha());
        out.printf("  Comentarios de bloco.........: %d%n", lexer.comentariosDeBloco());
        out.printf("  Erros lexicos................: %d%n", lexer.erros().size());
        out.printf("  Tempo de varredura...........: %.3f ms%n", nanos / 1_000_000.0);
        if (nanos > 0) {
            out.printf("  Vazao........................: %.0f tokens/s%n",
                    total / (nanos / 1_000_000_000.0));
        }
        out.println("--------------------------------------------------------------------------");
    }

    /** Saida em JSON, util para integrar o analisador a outras ferramentas. */
    public static void json(PrintStream out, List<Token> tokens, List<ErroLexico> erros) {
        out.println("{");
        out.println("  \"tokens\": [");
        int n = 0;
        for (Token t : tokens) {
            if (t.tipo() == TokenType.EOF) {
                continue;
            }
            if (n++ > 0) {
                out.println(",");
            }
            out.printf("    {\"tipo\": \"%s\", \"categoria\": \"%s\", \"lexema\": \"%s\", "
                            + "\"linha\": %d, \"coluna\": %d, \"valor\": %s}",
                    t.tipo(), t.tipo().categoria(), escapar(t.lexema()),
                    t.linha(), t.coluna(), valorJson(t.valor()));
        }
        out.println();
        out.println("  ],");
        out.println("  \"erros\": [");
        int m = 0;
        for (ErroLexico e : erros) {
            if (m++ > 0) {
                out.println(",");
            }
            out.printf("    {\"mensagem\": \"%s\", \"lexema\": \"%s\", \"linha\": %d, \"coluna\": %d}",
                    escapar(e.mensagem()), escapar(e.lexema()), e.linha(), e.coluna());
        }
        out.println();
        out.println("  ]");
        out.println("}");
    }

    private static String valorJson(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        return "\"" + escapar(String.valueOf(v)) + "\"";
    }

    private static String escapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Torna visiveis os caracteres de controle, para nao quebrar as tabelas. */
    private static String visivel(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\0': sb.append("\\0"); break;
                default:
                    sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : c);
            }
        }
        return sb.toString();
    }

    private static String corta(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}

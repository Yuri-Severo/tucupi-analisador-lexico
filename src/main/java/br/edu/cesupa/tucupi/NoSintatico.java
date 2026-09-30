package br.edu.cesupa.tucupi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * No generico da arvore sintatica.
 *
 * <p>Em vez de uma classe por construcao da linguagem (o que daria dezenas de
 * classes nesta etapa), cada no guarda apenas:</p>
 * <ul>
 *   <li>{@code tipo}  -- rotulo da construcao ({@code PROGRAMA}, {@code SE},
 *       {@code SOMA}, {@code LITERAL}...);</li>
 *   <li>{@code valor} -- informacao complementar opcional (nome do
 *       identificador, operador, lexema do literal);</li>
 *   <li>{@code filhos} -- subarvores, na ordem em que aparecem no fonte.</li>
 * </ul>
 * <p>Linha e coluna do token que originou o no sao guardadas para uso futuro
 * (mensagens da analise semantica).</p>
 */
public final class NoSintatico {

    private final String tipo;
    private final String valor;
    private final int linha;
    private final int coluna;
    private final List<NoSintatico> filhos = new ArrayList<>();

    public NoSintatico(String tipo, String valor, int linha, int coluna) {
        this.tipo = tipo;
        this.valor = valor;
        this.linha = linha;
        this.coluna = coluna;
    }

    public NoSintatico(String tipo, String valor, Token origem) {
        this(tipo, valor, origem.linha(), origem.coluna());
    }

    public NoSintatico(String tipo, Token origem) {
        this(tipo, null, origem.linha(), origem.coluna());
    }

    /** Acrescenta um filho (ignora {@code null}) e devolve o proprio no, para encadear. */
    public NoSintatico adicionar(NoSintatico filho) {
        if (filho != null) {
            filhos.add(filho);
        }
        return this;
    }

    public String tipo() {
        return tipo;
    }

    public String valor() {
        return valor;
    }

    public int linha() {
        return linha;
    }

    public int coluna() {
        return coluna;
    }

    public List<NoSintatico> filhos() {
        return Collections.unmodifiableList(filhos);
    }

    /** Numero total de nos da subarvore (inclui este). */
    public int tamanho() {
        int n = 1;
        for (NoSintatico f : filhos) {
            n += f.tamanho();
        }
        return n;
    }

    /** Rotulo exibido na impressao: "TIPO valor". */
    public String rotulo() {
        return valor == null || valor.isEmpty() ? tipo : tipo + " " + valor;
    }

    @Override
    public String toString() {
        return rotulo();
    }
}

package br.edu.cesupa.tucupi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabela de simbolos preenchida durante a analise lexica.
 *
 * <p>Na arquitetura classica de um compilador a tabela de simbolos e
 * compartilhada entre as fases. O analisador lexico e quem faz a primeira
 * insercao: ao reconhecer um identificador, ele registra o nome e devolve ao
 * parser um token que aponta para essa entrada, em vez de repetir a string.
 * Aqui guardamos tambem a primeira ocorrencia e a contagem de usos, o que
 * torna a tabela util para diagnostico mesmo sem as fases seguintes.</p>
 */
public final class TabelaDeSimbolos {

    /** Entrada da tabela: nome, primeira ocorrencia e numero de ocorrencias. */
    public static final class Entrada {
        private final String nome;
        private final int primeiraLinha;
        private final int primeiraColuna;
        private final int indice;
        private int ocorrencias;

        Entrada(String nome, int linha, int coluna, int indice) {
            this.nome = nome;
            this.primeiraLinha = linha;
            this.primeiraColuna = coluna;
            this.indice = indice;
            this.ocorrencias = 1;
        }

        public String nome() { return nome; }
        public int primeiraLinha() { return primeiraLinha; }
        public int primeiraColuna() { return primeiraColuna; }
        public int indice() { return indice; }
        public int ocorrencias() { return ocorrencias; }
    }

    private final Map<String, Entrada> entradas = new LinkedHashMap<>();

    /**
     * Insere o identificador se ainda nao existir; caso contrario incrementa a
     * contagem. Devolve a entrada correspondente.
     */
    public Entrada inserir(String nome, int linha, int coluna) {
        Entrada existente = entradas.get(nome);
        if (existente == null) {
            Entrada nova = new Entrada(nome, linha, coluna, entradas.size());
            entradas.put(nome, nova);
            return nova;
        }
        existente.ocorrencias++;
        return existente;
    }

    public Collection<Entrada> entradas() {
        return entradas.values();
    }

    public int tamanho() {
        return entradas.size();
    }
}

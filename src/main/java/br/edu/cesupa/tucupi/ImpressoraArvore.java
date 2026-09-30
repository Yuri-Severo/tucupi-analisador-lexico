package br.edu.cesupa.tucupi;

import java.io.PrintStream;
import java.util.List;

/**
 * Imprime a arvore sintatica no terminal, no estilo do comando {@code tree}.
 *
 * <pre>
 * PROGRAMA Exemplo
 * ├── DECL_VAR x : inteiro
 * │   └── LITERAL 10
 * └── FUNCAO principal -&gt; inteiro
 *     └── BLOCO
 * </pre>
 *
 * <p>O modo ASCII ({@code |--}, {@code `--}) existe para terminais que nao
 * exibem os caracteres de desenho de caixa (por exemplo, o prompt antigo do
 * Windows sem {@code chcp 65001}).</p>
 */
public final class ImpressoraArvore {

    private final String ramo;
    private final String ultimo;
    private final String vertical;
    private final String vazio;

    public ImpressoraArvore(boolean ascii) {
        if (ascii) {
            ramo = "|-- ";
            ultimo = "`-- ";
            vertical = "|   ";
        } else {
            ramo = "├── ";
            ultimo = "└── ";
            vertical = "│   ";
        }
        vazio = "    ";
    }

    public ImpressoraArvore() {
        this(false);
    }

    public void imprimir(NoSintatico raiz, PrintStream saida) {
        saida.print(comoTexto(raiz));
    }

    public String comoTexto(NoSintatico raiz) {
        StringBuilder sb = new StringBuilder();
        sb.append(raiz.rotulo()).append('\n');
        acumular(raiz, "", sb);
        return sb.toString();
    }

    private void acumular(NoSintatico no, String prefixo, StringBuilder sb) {
        List<NoSintatico> filhos = no.filhos();
        for (int i = 0; i < filhos.size(); i++) {
            boolean eUltimo = i == filhos.size() - 1;
            NoSintatico filho = filhos.get(i);
            sb.append(prefixo).append(eUltimo ? ultimo : ramo).append(filho.rotulo()).append('\n');
            acumular(filho, prefixo + (eUltimo ? vazio : vertical), sb);
        }
    }
}

package zemberek.tokenization.antlr;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.dfa.DFAState;

// For speeding up lexer, we had to make this hack.
// Refer to: https://github.com/antlr/antlr4/issues/1613#issuecomment-273514372

public class CustomLexerATNSimulator extends LexerATNSimulator {

    public static final int MAX_DFA_EDGE = 368;

    public CustomLexerATNSimulator(ATN atn, DFA[] decisionToDFA, PredictionContextCache sharedContextCache) {
        super(atn, decisionToDFA, sharedContextCache);
    }

    public CustomLexerATNSimulator(Lexer recog, ATN atn, DFA[] decisionToDFA, PredictionContextCache sharedContextCache) {
        super(recog, atn, decisionToDFA, sharedContextCache);
    }

    @Override
    protected DFAState getExistingTargetState(DFAState s, int t) {
        if (s.edges == null || t < MIN_DFA_EDGE || t > MAX_DFA_EDGE) {
            return null;
        }

        DFAState target = s.edges[t - MIN_DFA_EDGE];
        if (debug && target != null) {
            System.out.println("reuse state "+s.stateNumber+
                    " edge to "+target.stateNumber);
        }

        return target;
    }

    @Override
    protected void addDFAEdge(DFAState p, int t, DFAState q) {
        if (t < MIN_DFA_EDGE || t > MAX_DFA_EDGE) {
            // Only track edges within the DFA bounds
            return;
        }

        if ( debug ) {
            System.out.println("EDGE "+p+" -> "+q+" upon "+((char)t));
        }

        synchronized (p) {
            if ( p.edges==null ) {
                //  make room for tokens 1..n and -1 masquerading as index 0
                p.edges = new DFAState[MAX_DFA_EDGE-MIN_DFA_EDGE+1];
            }
            p.edges[t - MIN_DFA_EDGE] = q; // connect
        }
    }
}
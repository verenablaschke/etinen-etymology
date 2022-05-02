module de.tuebingen.sfs.eie.components.etymology {
    exports de.tuebingen.sfs.eie.components.etymology.eval;
    exports de.tuebingen.sfs.eie.components.etymology.filter;
    exports de.tuebingen.sfs.eie.components.etymology.ideas;
    exports de.tuebingen.sfs.eie.components.etymology.problems;
    exports de.tuebingen.sfs.eie.components.etymology.talk.pred;
    exports de.tuebingen.sfs.eie.components.etymology.talk.rule;
    exports de.tuebingen.sfs.eie.components.etymology.util;
    requires com.fasterxml.jackson.core;
    requires de.tuebingen.sfs.psl;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires de.jdellert.iwsa;
    requires de.tuebingen.sfs.cldfjava;
    requires psl.core;
    requires de.tuebingen.sfs.eie.shared;
}
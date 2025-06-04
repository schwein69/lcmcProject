package compiler.lib;

import compiler.AST.*;

public class FOOLlib {

    public static String extractNodeName(String s) { // s is in the form compiler.AST$NameNode
        return s.substring(s.lastIndexOf('$') + 1, s.length() - 4);
    }

    public static String extractCtxName(String s) { // s is in the form compiler.FOOLParser$NameContext
        return s.substring(s.lastIndexOf('$') + 1, s.length() - 7);
    }

    public static String lowerizeFirstChar(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1, s.length());
    }

    public static int typeErrors = 0;

    // valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode/ EmptyTypeNode
    public static boolean isSubtype(TypeNode a, TypeNode b) {
        if (a == null || b == null) return false;

        // Same class and check contents
        if (a.getClass().equals(b.getClass())) {
            if (a instanceof ArrowTypeNode aa && b instanceof ArrowTypeNode bb) {
                // Check parameter list sizes
                if (aa.parlist.size() != bb.parlist.size()) return false;

                // Parameters: contravariant
                for (int i = 0; i < aa.parlist.size(); i++) {
                    if (!isSubtype(bb.parlist.get(i), aa.parlist.get(i))) {
                        return false;
                    }
                }

                // Return type: covariant
                return isSubtype(aa.ret, bb.ret);
            }
            // For non-arrow types, same class = subtype (can refine more if needed)
            return true;
        }

        // Allow bool <: int
        if (a instanceof BoolTypeNode && b instanceof IntTypeNode) return true;

        // Allow empty <: reference
        if (a instanceof EmptyTypeNode && b instanceof RefTypeNode || b instanceof EmptyTypeNode && a instanceof RefTypeNode) return true;


        // Otherwise, not a subtype
        return false;
    }


    // crea un'unica stringa a partire da un insieme di stringhe concatenadole e
    // introducendo, all'interno, dei newline "\n" come separatore tra le stringhe
    public static String nlJoin(String... lines) { //argomenti null ignorati , ... è uguale ad args**, cioè numero variabile di argomenti
        String code = null;
        for (int i = 0; i < lines.length; i++)
            if (lines[i] != null) code = (code == null ? "" : code + "\n") + lines[i];
        return code;
    }

    private static int labCount = 0;

    public static String freshLabel() {
        return "label" + (labCount++);
    }

    private static int funlabCount = 0;

    public static String freshFunLabel() {
        return "function" + (funlabCount++);
    }

    private static String funCode = "";//deposito del corpo delle funzioni

    public static void putCode(String c) {
        funCode = nlJoin(funCode, "", c); //linea vuota di separazione prima di codice funzione
    }

    public static String getCode() {
        return funCode;
    }
}

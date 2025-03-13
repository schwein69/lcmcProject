package svm;

public class ExecuteVM {

    public static final int CODESIZE = 10000;
    public static final int MEMSIZE = 10000;

    private int[] code;
    private int[] memory = new int[MEMSIZE];// memoria stack e heap

    private int tm;
    private int ip = 0;/* Program counter*/ /*o meglio instruction counter, cioè a quale riga sono arrivato a leggere/eseguire*/
    private int sp = MEMSIZE; //punta al top dello stack, stack funziona da 10000 a 0, quindi scende


    //stack sp sta in top, hp heap sta in fondo, se si incontrano vuol dire che è finita la memoria
    private int hp = 0; //
    private int fp = sp; // frame pointer
    private int ra;
    public ExecuteVM(int[] code) {
        this.code = code;
    }

    public void cpu() {
        while (true) {
            int bytecode = code[ip++]; // fetch dell'istruzione che deve eseguire, bytecode rappresenta il codice dell'istruzione
            int v1, v2;
            int address;
            switch (bytecode) {
                case SVMParser.PUSH:
                    push(code[ip++]);//push e poi salto all'istruzione prossimo (praticamente nel code[] ci sono
                    // in modo alternato istruzione output, istruzione output... ex: 1(che equivale a push) 0(input di push),
                    // poi 1 1, 1 2 .... 22 (print), 1(push) di 0 ecc... Appunto ip serve per fare questo(leggere istruzione))
                    break;
                case SVMParser.POP:
                    pop();
                    break;
                case SVMParser.ADD:
                    v1 = pop();
                    v2 = pop();
                    push(v2 + v1);//invertire l'ordine
                    break;
                case SVMParser.SUB:
                    v1 = pop();// 5 - 3,  stack 5 3 -> pop 3 in v1 , pop 5 in v2, quindi invertire l'ordine
                    v2 = pop();
                    push(v2 - v1);//invertire l'ordine
                    break;
                case SVMParser.MULT:
                    v1 = pop();
                    v2 = pop();
                    push(v2 * v1);//invertire l'ordine
                    break;
                case SVMParser.DIV:
                    v1 = pop();
                    v2 = pop();
                    push(v2 / v1);//invertire l'ordine
                    break;
                case SVMParser.STOREW:
                    address = pop();//pop valore
                    memory[address] = pop();// indirizzo del valore e ci metto il valore che poppo la seconda volta
                    break;
                case SVMParser.LOADW:
                    push(memory[pop()]);//pop il valore dello stack, cerco la memoria di indirizzo di quel valore e poi lo push sullo stack
                    break;
                case SVMParser.BRANCH:
                    address = code[ip];//leggo indirizzo
                    ip = address;// va a prendere in code[] la posizione della label r faccio il loop
                    break;
                case SVMParser.BRANCHEQ:
                    address = code[ip++];//leggo il codice che devo eseguire, in questo caso in code[ip=27] = 29, e tornando sopra (prima dello switch)
                    // in code[29] = 23(Codice di Halt)
                    v1 = pop();
                    v2 = pop();
                    if(v2 == v1) ip = address;
                    break;
                case SVMParser.BRANCHLESSEQ:
                    address = code[ip++];//leggo indirizzo successivo e poi salto
                    v1 = pop();
                    v2 = pop();
                    if(v2 <= v1) ip = address;
                    break;
                case SVMParser.JS://jump all'indirizzo deciso al runtime
                    v1 = pop();
                    ra = ip;//istruzione successiva al salto
                    ip = v1;
                    break;
                case SVMParser.LOADRA:
                    push(ra);
                    break;
                case SVMParser.STORERA:
                    ra = pop();
                    break;
                case SVMParser.LOADTM:
                    push(tm);
                    break;
                case SVMParser.STORETM:
                    tm = pop();
                    break;
                case SVMParser.LOADFP:
                    push(fp);
                    break;
                case SVMParser.STOREFP:
                    fp = pop();
                    break;
                case SVMParser.COPYFP:
                    fp = sp;// frame pointer fa riferimento all'activation record
                    break;
                case SVMParser.LOADHP:
                    push(hp);
                    break;
                case SVMParser.STOREHP:
                    hp = pop();
                    break;
                case SVMParser.PRINT:
                    System.out.println( (sp<MEMSIZE) ? memory[sp] : "EMPTY STACK");
                    break;
                case SVMParser.HALT:
                    return;
            }
        }
    }

    private int pop() {
        return memory[sp++];
    }

    private void push(int v) {
        memory[--sp] = v;
    }

}
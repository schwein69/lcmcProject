    push 0
    push 1
    push 2
    push 3
    push 4
    push 5
    push 6
    push 7
    push 8
    push 9
    push 10

l1: print  /* stampa senza fare pop, label def*/ /*Per le istruzioni in pratica: mette in labelDef (l1,riga13) */
    push 0
    bleq l2 /* fa due pop dallo stack(rimuove)(ricorda che pop è bottom up, cioè stsack 10 9 8 ... pop 10  10  stack 9 8 7 ...  pop 9  9 10  stack 8 7 6...) e guarda se vale <=, se true salta*/
    b l1  /*b = salto*/
    /* all'istruzione branch fa: il contatore code[16] = b (che deve eseguire l'istruzione b),*/
    /* in labelRef mette(riga17,l1), i diventa 18, poi scatta istruzione di EOF: in code[17] dato che è vuoto e i è passato ad 18, devo riempire il buco, quindi osserva istruzione di EOF, va a riempire code[17]= riga 13 (prima va a prendere da labelRef.get(17)=l1 e poi labelDef.get(l1)=13), quindi diventa code[16] fai jump e code[17] a l1.*/
l2: halt/* exit */

push 0
push 6
push 5
push function0
lfp
push -2
add
lw
lfp
push -3
add
lw
sub
push 0
bleq label8
push 0
b label9
label8:
push 1
label9:
push 1
beq label6
lfp
push 0
lfp
stm
ltm
ltm
push -4
add
lw
js
b label7
label6:
lfp
push -2
add
lw
label7:
print
halt

function0:
cfp
lra
lfp
push 1
add
lw
push 1
beq label0
push 1
lfp
lw
push -3
add
lw
lfp
lw
push -2
add
lw
sub
push 0
bleq label2
push 0
b label3
label2:
push 1
label3:
sub
b label1
label0:
lfp
lw
push -2
add
lw
lfp
lw
push -3
add
lw
beq label4
push 0
b label5
label4:
push 1
label5:
label1:
stm

sra
pop
pop

sfp
ltm
lra
js
push 0
push 6
push 7
lfp
push function0
push -2
lfp
add
lw
push -3
lfp
add
lw
bleq label11
push 0
b label12
label11:
push 1
label12:
push 1
beq label9
lfp
push 0
push -4
lfp
add
lw
push -5
lfp
add
lw
js
b label10
label9:
push -2
lfp
add
lw
label10:
print
halt


function0:
cfp
lra

push 1
lfp
add
lw
push 1
beq label0
push -2
lfp
lw
add
lw
push -3
lfp
lw
add
lw
bleq label4
push 1
b label6
label4:
push -2
lfp
lw
add
lw
push -3
lfp
lw
add
lw
beq label5
push 0
b label6
label5:
push 1
label6:
push 1
beq
label2
push 1
b
label3
label2:
push 0
label3:
b label1
label0:
push -2
lfp
lw
add
lw
push -3
lfp
lw
add
lw
beq label7
push 0
b label8
label7:
push 1
label8:
label1:
stm

sra
pop
pop

sfp
ltm
lra
js
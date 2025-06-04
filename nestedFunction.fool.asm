push 0
lfp
push function2
lfp
push -2
lfp
add
lw
push -3
lfp
add
lw
js
print
halt


function0:
cfp
lra

push 42
stm

sra
pop

sfp
ltm
lra
js

function1:
cfp
lra

lfp
push function0
lfp
push -2
lfp
add
lw
push -3
lfp
add
lw
js
stm
pop
pop

sra
pop

sfp
ltm
lra
js

function2:
cfp
lra

lfp
push function1
lfp
push -2
lfp
add
lw
push -3
lfp
add
lw
js
stm
pop
pop

sra
pop

sfp
ltm
lra
js
grammar Expression;

expr
    : expr op=(MUL|DIV) expr  # BinaryOp
    | expr op=(ADD|SUB) expr  # BinaryOp
    | op=(ADD|SUB) factor  # UnaryOp
    | factor  # ToFactor
    ;

factor
    : power factor  # ImplicitMultiply
    | power  # ToPower
    ;

power
    : <assoc=right> atom op=(EXP|EXPN) power  # PowerOp
    | atom # ToAtom
    ;

atom
    : func=(F_ABS|F_SQRT|F_SIN|F_COS|F_TAN|F_SINH|F_COSH|F_TANH|F_LOG|F_LN|F_EXP) '(' expr ')'  # FunctionCall
    | '(' expr ')'  # Brackets
    | ID  # Variable
    | NUMBER  # Literal
    ;

ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/';
EXP : '^';
EXPN : '^-';

F_ABS: 'abs';
F_SQRT: 'sqrt';
F_SIN: 'sin';
F_COS: 'cos';
F_TAN: 'tan';
F_SINH: 'sinh';
F_COSH: 'cosh';
F_TANH: 'tanh';
F_LOG: 'log';
F_LN: 'ln';
F_EXP: 'exp';

ID  : [a-zA-Z];
NUMBER
    : ([0-9]+|[0-9]*'.'[0-9]+)([eE][-+]?[0-9]+)?;
WS : [ \t\r\n]+ -> skip ;
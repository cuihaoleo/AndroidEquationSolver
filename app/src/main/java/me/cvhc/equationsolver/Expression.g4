grammar Expression;

expr
    : <assoc=right> expr op=EXP expr  # BinaryOp
    | expr op=(MUL|DIV) expr  # BinaryOp
    | expr op=(ADD|SUB) expr  # BinaryOp
    | op=(ADD|SUB) factor  # UnaryOp
    | factor  # ToFactor
    ;

factor
    : atom factor  # ImplicitMultiply
    | atom  # ToAtom
    ;

atom
    : FUNC '(' expr ')'  # FunctionCall
    | '(' expr ')'  # Brackets
    | ID  # Variable
    | NUMBER  # Literal
    ;

ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/';
EXP : '^';

FUNC
    : [a-zA-Z][a-zA-Z]+;
ID  : [a-zA-Z];
NUMBER
    : ([0-9]+|[0-9]*'.'[0-9]+)([eE][-+]?[0-9]+)?;
WS : [ \t\r\n]+ -> skip ;
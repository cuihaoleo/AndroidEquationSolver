grammar Expression;

expression
    : binaryOperatorL2;

binaryOperatorL2
    : binaryOperatorL2 op=(ADD|SUB) binaryOperatorL1
    | binaryOperatorL1;

binaryOperatorL1
    : binaryOperatorL1 op=(MUL|DIV) unaryOperator
    | unaryOperator;

unaryOperator
    : op=(ADD|SUB) implicitMultiply
    | implicitMultiply;

implicitMultiply
    : implicitMultiply implicitMultiplyComponent
    | number
    | number implicitMultiplyComponent
    | implicitMultiplyComponent;

implicitMultiplyComponent
    : identifier
    | bracketExpression
    | functionCall
    | powerOperator;

powerOperator
    : <assoc=right> powerComponent op=(EXP|EXPN) powerOperator
    | <assoc=right> powerComponent op=(EXP|EXPN) powerComponent;

powerComponent
    : number
    | identifier
    | bracketExpression
    | functionCall;

functionCall
    : func=(F_ABS|F_SQRT|F_SIN|F_COS|F_TAN|F_SINH|F_COSH|F_TANH|F_LOG|F_LN|F_EXP) bracketExpression;

bracketExpression
    : '(' expression ')';

number
    : NUMBER;

identifier
    : ID;

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

ID  : [a-z];
NUMBER
    : ([0-9]+|[0-9]*'.'[0-9]+)([Ee][-+]?[0-9]+)?;
WS : [ \t\r\n]+ -> skip ;
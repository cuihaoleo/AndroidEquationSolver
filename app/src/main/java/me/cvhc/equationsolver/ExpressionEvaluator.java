package me.cvhc.equationsolver;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class ExpressionEvaluator {
    public static class PropertyStruct {
        public HashSet<Character> Variables = new HashSet<>();
        public double Value = Double.NaN;
        public boolean Determined = false;
    }

    private String Expression;
    private ExpressionVisitorImpl Evaluator;
    private ParseTree ASTree;
    private boolean ErrorFlag = false;
    private PropertyStruct Property;

    private HashMap<Character, Double> VariableList = new HashMap<>();
    private HashSet<Character> CachedVariables = new HashSet<>();

    public ExpressionEvaluator(String exp) {
        ExpressionLexer lexer = new ExpressionLexer(new ANTLRInputStream(exp));

        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                ErrorFlag = true;
            }
        });

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExpressionParser parser = new ExpressionParser(tokens);
        Expression = exp;

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                ErrorFlag = true;
            }
        });

        ASTree = parser.expr();
        if (!ErrorFlag) {
            Evaluator = new ExpressionVisitorImpl();
            Property = Evaluator.visit(ASTree);
        }
    }

    @Override
    public String toString() {
        return Expression;
    }

    public boolean isError() {
        return ErrorFlag;
    }

    public PropertyStruct getProperty() {
        return Property;
    }

    public Double getValue() {
        return Property.Determined ? Property.Value : null;
    }

    public void setVariable(Character id, Double val) {
        CachedVariables.remove(id);
        VariableList.put(id, val);
        Property = Evaluator.visit(ASTree);
    }

    public void updateVariables(Map<Character, Double> vars) {
        for (Character key: CachedVariables) {
            if (vars.containsKey(key) && VariableList.containsKey(key) &&
                    !VariableList.get(key).equals(vars.get(key))) {
                CachedVariables.remove(key);
            }
        }

        VariableList = new HashMap<>(vars);
        Property = Evaluator.visit(ASTree);
    }

    public void resetVariables() {
        CachedVariables.clear();
        VariableList.clear();
        Property = Evaluator.visit(ASTree);
    }

    private class ExpressionVisitorImpl extends ExpressionBaseVisitor<PropertyStruct> {
        private ParseTreeProperty<PropertyStruct> NodeProperty = new ParseTreeProperty<>();

        @Override
        protected PropertyStruct aggregateResult(PropertyStruct aggregate, PropertyStruct nextResult) {
            return nextResult != null ? nextResult : aggregate;
        }

        @Override
        public PropertyStruct visitBinaryOp(ExpressionParser.BinaryOpContext ctx) {
            PropertyStruct prop = NodeProperty.get(ctx);
            if (prop != null && CachedVariables.containsAll(prop.Variables))
                return prop;

            PropertyStruct left = visit(ctx.expr(0));
            PropertyStruct right = visit(ctx.expr(1));
            PropertyStruct current = new PropertyStruct();

            current.Determined = left.Determined && right.Determined;
            current.Variables = new HashSet<>(left.Variables);
            current.Variables.addAll(right.Variables);

            if (current.Determined) {
                switch (ctx.op.getType()) {
                    case ExpressionParser.ADD:
                        current.Value = left.Value + right.Value;
                        break;
                    case ExpressionParser.SUB:
                        current.Value = left.Value - right.Value;
                        break;
                    case ExpressionParser.MUL:
                        current.Value = left.Value * right.Value;
                        break;
                    case ExpressionParser.DIV:
                        current.Value = left.Value / right.Value;
                        break;
                    case ExpressionParser.EXP:
                        current.Value = Math.pow(left.Value, right.Value);
                        break;
                    default:
                        break;
                }
            }

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public PropertyStruct visitUnaryOp(ExpressionParser.UnaryOpContext ctx) {
            PropertyStruct prop = NodeProperty.get(ctx);
            if (prop != null && CachedVariables.containsAll(prop.Variables))
                return prop;

            PropertyStruct child = visit(ctx.factor());
            PropertyStruct current = new PropertyStruct();

            current.Determined = child.Determined;
            current.Variables = new HashSet<>(child.Variables);

            if (child.Determined) {
                switch (ctx.op.getType()) {
                    case ExpressionParser.ADD:
                        break;
                    case ExpressionParser.SUB:
                        current.Value = -child.Value;
                        break;
                    default:
                        break;
                }
            }

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public PropertyStruct visitPowerOp(ExpressionParser.PowerOpContext ctx) {
            PropertyStruct prop = NodeProperty.get(ctx);
            if (prop != null && CachedVariables.containsAll(prop.Variables))
                return prop;

            PropertyStruct left = visit(ctx.atom());
            PropertyStruct right = visit(ctx.power());
            PropertyStruct current = new PropertyStruct();

            current.Determined = left.Determined && right.Determined;
            current.Variables = new HashSet<>(left.Variables);
            current.Variables.addAll(right.Variables);

            if (ctx.op.getType() == ExpressionParser.EXP) {
                current.Value = current.Determined ? Math.pow(left.Value, right.Value) : 0.0;
            } else {
                current.Value = current.Determined ? Math.pow(left.Value, -right.Value) : 0.0;
            }

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public PropertyStruct visitImplicitMultiply(ExpressionParser.ImplicitMultiplyContext ctx) {
            PropertyStruct prop = NodeProperty.get(ctx);
            if (prop != null && CachedVariables.containsAll(prop.Variables))
                return prop;

            PropertyStruct left = visit(ctx.power());
            PropertyStruct right = visit(ctx.factor());
            PropertyStruct current = new PropertyStruct();

            current.Determined = left.Determined && right.Determined;
            current.Variables = new HashSet<>(left.Variables);
            current.Variables.addAll(right.Variables);
            current.Value = current.Determined ? left.Value * right.Value : 0.0;

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public PropertyStruct visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
            PropertyStruct prop = NodeProperty.get(ctx);
            if (prop != null && CachedVariables.containsAll(prop.Variables))
                return prop;

            PropertyStruct child = visit(ctx.expr());
            PropertyStruct current = new PropertyStruct();

            current.Determined = child.Determined;
            current.Variables = new HashSet<>(child.Variables);

            Double v = child.Value;
            switch (ctx.func.getType()) {
                case ExpressionParser.F_ABS:
                    current.Value = Math.abs(v); break;
                case ExpressionParser.F_SQRT:
                    current.Value = Math.sqrt(v); break;
                case ExpressionParser.F_SIN:
                    current.Value = Math.sin(v); break;
                case ExpressionParser.F_COS:
                    current.Value = Math.cos(v); break;
                case ExpressionParser.F_TAN:
                    current.Value = Math.tan(v); break;
                case ExpressionParser.F_SINH:
                    current.Value = Math.sinh(v); break;
                case ExpressionParser.F_COSH:
                    current.Value = Math.cosh(v); break;
                case ExpressionParser.F_TANH:
                    current.Value = Math.tanh(v); break;
                case ExpressionParser.F_LOG:
                    current.Value = Math.log10(v); break;
                case ExpressionParser.F_LN:
                    current.Value = Math.log(v); break;
                case ExpressionParser.F_EXP:
                    current.Value = Math.exp(v); break;
                default:
                    // todo: deal with undefined function
                    break;
            }

            return current;
        }

        @Override
        public PropertyStruct visitVariable(ExpressionParser.VariableContext ctx) {
            char id = ctx.ID().getText().charAt(0);
            PropertyStruct current = new PropertyStruct();

            current.Variables = new HashSet<>();
            current.Variables.add(id);

            if (current.Determined = VariableList.containsKey(id)) {
                current.Value = VariableList.get(id);
            }

            return current;
        }

        @Override
        public PropertyStruct visitLiteral(ExpressionParser.LiteralContext ctx) {
            PropertyStruct current = new PropertyStruct();
            current.Variables = new HashSet<>();
            current.Determined = true;
            current.Value = Double.valueOf(ctx.NUMBER().getText());

            return current;
        }
    }

    public static Double eval(String exp) {
        ExpressionEvaluator e = new ExpressionEvaluator(exp);
        return e.isError() ? null : e.getValue();
    }
}
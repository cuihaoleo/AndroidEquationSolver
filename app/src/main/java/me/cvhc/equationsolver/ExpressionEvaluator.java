package me.cvhc.equationsolver;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ExpressionEvaluator {
    public static class PropertyStruct {
        public HashSet<Character> Variables = new HashSet<>();
        public double Value = Double.NaN;
        public boolean Determined = false;
    }

    private ExpressionVisitorImpl Evaluator;
    private ParseTree ASTree;
    private boolean ErrorFlag = false;
    private PropertyStruct Property;

    private HashMap<Character, Double> VariableList = new HashMap<>();
    private HashSet<Character> CachedVariables = new HashSet<>();

    public ExpressionEvaluator(String exp) {
        ExpressionLexer lexer = new ExpressionLexer(new ANTLRInputStream(exp));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExpressionParser parser = new ExpressionParser(tokens);

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

    public boolean isError() {
        return ErrorFlag;
    }

    public PropertyStruct getProperty() {
        return Property;
    }

    public Double getValue() {
        return Property.Determined ? Property.Value : null;
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
        public PropertyStruct visitImplicitMultiply(ExpressionParser.ImplicitMultiplyContext ctx) {
            PropertyStruct prop = NodeProperty.get(ctx);
            if (prop != null && CachedVariables.containsAll(prop.Variables))
                return prop;

            PropertyStruct left = visit(ctx.atom());
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
            // do nothing yet~
            return visit(ctx.expr());
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
}
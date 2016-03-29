package me.cvhc.equationsolver;


import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class ExpressionCalculator {
    private HashMap<Character, ExpressionRenderer> mIDList = new HashMap<>();
    private HashMap<Character, OptionUnion> mValueCache = new HashMap<>();
    private ArrayList<Character> mEvalOrder = new ArrayList<>();

    public static ExpressionCalculator copy(ExpressionCalculator eval) {
        ExpressionCalculator n = new ExpressionCalculator();
        n.mIDList.putAll(eval.mIDList);
        n.mValueCache.putAll(eval.mValueCache);
        n.mEvalOrder.addAll(eval.mEvalOrder);
        return n;
    }

    private void removeCache(Character id) {
        if (mValueCache.containsKey(id)) {
            mValueCache.remove(id);
        }

        for (Character c: mIDList.keySet()) {
            ExpressionRenderer expr = mIDList.get(c);
            if (expr.getDependency().contains(id)) {
                removeCache(c);
            }
        }
    }

    public boolean isSet(Character c) {
        return mIDList.keySet().contains(c);
    }

    public boolean setVariable(Character c, Double val) {
        // todo: this will lose precision
        return setVariable(c, val.toString());
    }

    public boolean setVariable(Character c, String exp) {
        ExpressionRenderer expr;
        try {
            expr = new ExpressionRenderer(exp);
        } catch (Exception e) {
            return false;
        }

        return setVariable(c, expr);
    }

    public boolean setVariable(Character c, ExpressionRenderer exp) {
        ExpressionRenderer prev = mIDList.get(c);

        mIDList.put(c, exp);
        mEvalOrder = topologicalSort();

        if (mEvalOrder == null) {
            if (prev != null) { mIDList.put(c, prev); }
            return false;
        }

        removeCache(c);
        return true;
    }

    // topological sort (DFS) to determine the order of evaluating
    private boolean _topologicalSort(ExpressionRenderer expr,
                                     ArrayList<Character> result,
                                     HashSet<Character> marked) {
        for (Character c: expr.getDependency()) {
            // detect loop dependency
            if (marked.contains(c)) {
                return false;
            }

            ExpressionRenderer next = mIDList.get(c);
            if (next != null) {
                marked.add(c);
                if (!_topologicalSort(next, result, marked)) {
                    return false;
                }
                marked.remove(c);

                if (!result.contains(c)) {
                    result.add(c);
                }
            }
        }

        return true;
    }

    private ArrayList<Character> topologicalSort() {
        ArrayList<Character> result = new ArrayList<>();
        HashSet<Character> marked = new HashSet<>();

        for (Character c: mIDList.keySet()) {
            if (!result.contains(c)) {
                if (!_topologicalSort(mIDList.get(c), result, marked)) {
                    return null;
                } else {
                    result.add(c);
                }
            }
        }

        return result;
    }

    public OptionUnion evaluate(Character id) {
        if (!mIDList.containsKey(id)) {
            return null;
        }

        if (mEvalOrder == null) {
            mEvalOrder = topologicalSort();
        }

        if (!mValueCache.containsKey(id)) {
            for (Character c: mEvalOrder) {
                if (!mValueCache.containsKey(c)) {
                    ExpressionRenderer expr = mIDList.get(c);
                    ExpressionVisitorImpl visitor = new ExpressionVisitorImpl();

                    OptionUnion prop = visitor.visit(expr.getASTree());
                    mValueCache.put(c, prop);
                }

                if (c == id) {
                    break;
                }
            }
        }

        return mValueCache.get(id);
    }

    public static class OptionUnion {
        private HashSet<Character> mVariable = null;
        private Double mValue = null;

        public HashSet<Character> getVariable() {
            return mVariable;
        }

        public Double getValue() {
            return mValue;
        }
    }


    private class ExpressionVisitorImpl extends ExpressionBaseVisitor<OptionUnion> {
        private ParseTreeProperty<OptionUnion> NodeProperty = new ParseTreeProperty<>();

        @Override
        protected OptionUnion aggregateResult(OptionUnion aggregate, OptionUnion nextResult) {
            return nextResult != null ? nextResult : aggregate;
        }

        @Override
        public OptionUnion visitBinaryOp(ExpressionParser.BinaryOpContext ctx) {
            OptionUnion prop = NodeProperty.get(ctx);
            if (prop != null && mValueCache.keySet().containsAll(prop.mVariable)) {
                return prop;
            }

            OptionUnion left = visit(ctx.expr(0));
            OptionUnion right = visit(ctx.expr(1));
            OptionUnion current = new OptionUnion();

            if (left.mVariable != null || right.mVariable != null) {
                current.mVariable = new HashSet<>();
                if (left.mVariable != null) {
                    current.mVariable.addAll(left.mVariable);
                }
                if (right.mVariable != null) {
                    current.mVariable.addAll(right.mVariable);
                }
            } else {
                switch (ctx.op.getType()) {
                    case ExpressionParser.ADD:
                        current.mValue = left.mValue + right.mValue;
                        break;
                    case ExpressionParser.SUB:
                        current.mValue = left.mValue - right.mValue;
                        break;
                    case ExpressionParser.MUL:
                        current.mValue = left.mValue * right.mValue;
                        break;
                    case ExpressionParser.DIV:
                        current.mValue = left.mValue / right.mValue;
                        break;
                    default:
                        break;
                }
            }

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public OptionUnion visitUnaryOp(ExpressionParser.UnaryOpContext ctx) {
            OptionUnion prop = NodeProperty.get(ctx);
            if (prop != null && mValueCache.keySet().containsAll(prop.mVariable)) {
                return prop;
            }

            OptionUnion child = visit(ctx.factor());
            OptionUnion current = new OptionUnion();

            if (child.mVariable != null) {
                current.mVariable = new HashSet<>(child.mVariable);

            } else {
                switch (ctx.op.getType()) {
                    case ExpressionParser.ADD:
                        break;
                    case ExpressionParser.SUB:
                        current.mValue = -child.mValue;
                        break;
                    default:
                        break;
                }
            }

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public OptionUnion visitPowerOp(ExpressionParser.PowerOpContext ctx) {
            OptionUnion prop = NodeProperty.get(ctx);
            if (prop != null && mValueCache.keySet().containsAll(prop.mVariable)) {
                return prop;
            }

            OptionUnion left = visit(ctx.atom());
            OptionUnion right = visit(ctx.power());
            OptionUnion current = new OptionUnion();

            if (left.mVariable != null || right.mVariable != null) {
                current.mVariable = new HashSet<>();
                if (left.mVariable != null) {
                    current.mVariable.addAll(left.mVariable);
                }
                if (right.mVariable != null) {
                    current.mVariable.addAll(right.mVariable);
                }
            } else {
                if (ctx.op.getType() == ExpressionParser.EXP) {
                    current.mValue = Math.pow(left.mValue, right.mValue);
                } else {
                    current.mValue = Math.pow(left.mValue, -right.mValue);
                }
            }

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public OptionUnion visitImplicitMultiply(ExpressionParser.ImplicitMultiplyContext ctx) {
            OptionUnion prop = NodeProperty.get(ctx);
            if (prop != null && mValueCache.keySet().containsAll(prop.mVariable)) {
                return prop;
            }

            OptionUnion left = visit(ctx.power());
            OptionUnion right = visit(ctx.factor());
            OptionUnion current = new OptionUnion();

            if (left.mVariable != null || right.mVariable != null) {
                current.mVariable = new HashSet<>();
                if (left.mVariable != null) { current.mVariable.addAll(left.mVariable); }
                if (right.mVariable != null) { current.mVariable.addAll(right.mVariable); }
            } else {
                current.mValue = left.mValue * right.mValue;
            }

            NodeProperty.put(ctx, current);
            return current;
        }

        @Override
        public OptionUnion visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
            OptionUnion prop = NodeProperty.get(ctx);
            if (prop != null && mValueCache.keySet().containsAll(prop.mVariable)) {
                return prop;
            }

            OptionUnion child = visit(ctx.expr());
            OptionUnion current = new OptionUnion();

            if (child.mVariable != null) {
                current.mVariable = new HashSet<>(child.mVariable);
            } else {
                Double v = child.mValue;
                switch (ctx.func.getType()) {
                    case ExpressionParser.F_ABS:
                        current.mValue = Math.abs(v); break;
                    case ExpressionParser.F_SQRT:
                        current.mValue = Math.sqrt(v); break;
                    case ExpressionParser.F_SIN:
                        current.mValue = Math.sin(v); break;
                    case ExpressionParser.F_COS:
                        current.mValue = Math.cos(v); break;
                    case ExpressionParser.F_TAN:
                        current.mValue = Math.tan(v); break;
                    case ExpressionParser.F_SINH:
                        current.mValue = Math.sinh(v); break;
                    case ExpressionParser.F_COSH:
                        current.mValue = Math.cosh(v); break;
                    case ExpressionParser.F_TANH:
                        current.mValue = Math.tanh(v); break;
                    case ExpressionParser.F_LOG:
                        current.mValue = Math.log10(v); break;
                    case ExpressionParser.F_LN:
                        current.mValue = Math.log(v); break;
                    case ExpressionParser.F_EXP:
                        current.mValue = Math.exp(v); break;
                    default:
                        throw new RuntimeException();
                }
            }

            return current;
        }

        @Override
        public OptionUnion visitVariable(ExpressionParser.VariableContext ctx) {
            char id = ctx.ID().getText().charAt(0);

            if (mValueCache.containsKey(id)) {
                return mValueCache.get(id);
            } else {
                OptionUnion current = new OptionUnion();
                current.mVariable = new HashSet<>();
                current.mVariable.add(id);
                return current;
            }
        }

        @Override
        public OptionUnion visitLiteral(ExpressionParser.LiteralContext ctx) {
            OptionUnion current = new OptionUnion();
            current.mValue = Double.valueOf(ctx.NUMBER().getText());
            return current;
        }
    }

}
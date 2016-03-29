package me.cvhc.equationsolver;


import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ErrorNode;

import java.util.HashSet;

public class ExpressionRenderer {
    private HashSet<Character> mDepends;
    private boolean mErrorFlag = false;
    private String mString;
    private String mColoredHtml;
    private ExpressionParser.ExpressionContext mASTree;

    public ExpressionRenderer(String exp) throws Exception {
        ExpressionLexer lexer = new ExpressionLexer(new ANTLRInputStream(exp));
        mString = exp;

        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                mErrorFlag = true;
            }
        });

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
                mErrorFlag = true;
            }
        });

        if (mErrorFlag) { throw new Exception(); }

        mASTree = parser.expression();
        mDepends = new HashSet<>();
        ColorfulVisitor visitor = new ColorfulVisitor();
        mColoredHtml = visitor.visit(mASTree);

        if (mErrorFlag) { throw new Exception(); }
    }

    public final HashSet<Character> getDependency() {
        return mDepends;
    }

    public final ExpressionParser.ExpressionContext getASTree() {
        return mASTree;
    }

    @Override
    public String toString() {
        return mString;
    }

    public String toHTML() {
        return mColoredHtml;
    }

    private class ColorfulVisitor extends ExpressionBaseVisitor<String> {
        @Override
        public String visitErrorNode(ErrorNode node) {
            mErrorFlag = true;
            return "";
        }

        private String parseBinaryOperator(String left, String op, String right) {
            switch (op) {
                case "+":
                case "-":
                    return left + " " + ops(op) + " " + right;
                case "*":
                case "/":
                    return left + ops(op) + right;
                case "^":
                    return left + sup(right);
                case "^-":
                    return left + sup(ops("-") + right);
                case " ":
                    return left + right;
                default:
                    throw new RuntimeException();
            }
        }

        private String parseUnaryOperator(String op, String right) {
            return ops(op) + right;
        }

        @Override
        public String visitBinaryOperatorL2(ExpressionParser.BinaryOperatorL2Context ctx) {
            if (ctx.getChildCount() == 3) {
                return parseBinaryOperator(
                        visit(ctx.getChild(0)),
                        ctx.getChild(1).getText(),
                        visit(ctx.getChild(2)));
            } else {
                return super.visitBinaryOperatorL2(ctx);
            }
        }

        @Override
        public String visitBinaryOperatorL1(ExpressionParser.BinaryOperatorL1Context ctx) {
            if (ctx.getChildCount() == 3) {
                return parseBinaryOperator(
                        visit(ctx.getChild(0)),
                        ctx.getChild(1).getText(),
                        visit(ctx.getChild(2)));
            } else {
                return super.visitBinaryOperatorL1(ctx);
            }
        }

        @Override
        public String visitUnaryOperator(ExpressionParser.UnaryOperatorContext ctx) {
            if (ctx.getChildCount() == 2) {
                return parseUnaryOperator(
                        ctx.getChild(0).getText(),
                        visit(ctx.getChild(1)));
            } else {
                return super.visitUnaryOperator(ctx);
            }
        }

        @Override
        public String visitImplicitMultiply(ExpressionParser.ImplicitMultiplyContext ctx) {
            if (ctx.getChildCount() == 2) {
                return parseBinaryOperator(
                        visit(ctx.getChild(0)),
                        " ",
                        visit(ctx.getChild(1)));
            } else {
                return super.visitImplicitMultiply(ctx);
            }
        }

        @Override
        public String visitPowerOperator(ExpressionParser.PowerOperatorContext ctx) {
            if (ctx.getChildCount() == 3) {
                String left = ctx.getChild(0).getText();

                if (left.matches("([0-9]+|[0-9]*'.'[0-9]+)[eE][-+]?[0-9]+")) {
                    return parseBinaryOperator(
                            ops("(") + visit(ctx.getChild(0)) + ops(")"),
                            ctx.getChild(1).getText(),
                            visit(ctx.getChild(2)));
                } else {
                    return parseBinaryOperator(
                            visit(ctx.getChild(0)),
                            ctx.getChild(1).getText(),
                            visit(ctx.getChild(2)));
                }
            } else {
                return super.visitPowerOperator(ctx);
            }
        }

        @Override
        public String visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
            return String.format(
                    "<i>%s</i>%s",
                    colorize(ctx.func.getText(), "#445588"), visit(ctx.bracketExpression()));
        }

        @Override
        public String visitBracketExpression(ExpressionParser.BracketExpressionContext ctx) {
            return ops("(") + visit(ctx.expression()) + ops(")");
        }

        @Override
        public String visitNumber(ExpressionParser.NumberContext ctx) {
            String text = ctx.getText();
            String[] parts = text.split("[eE]");

            StringBuilder sb = new StringBuilder(parts[0]);
            if (parts.length > 1) {
                sb.append("&times;10").append(sup(parts[1]));
            }

            return colorize(sb.toString(), "#0074D9");
        }

        @Override
        public String visitIdentifier(ExpressionParser.IdentifierContext ctx) {
            char id = ctx.ID().getText().charAt(0);
            mDepends.add(id);

            String color = ctx.getText().equals("x") ? "#DD1144" : "#008080";
            return bold(colorize("" + id, color));
        }

        private String colorize(String s, String color) {
            return String.format("<font color=\"%s\">%s</font>", color, s);
        }

        private String bold(String s) {
            return "<b>" + s + "</b>";
        }

        private String ops(String s) {
            return colorize(s, "#666666");
        }

        private String sup(String s) {
            return "<sup><small>" + s + "</small></sup>";
        }
    }
}

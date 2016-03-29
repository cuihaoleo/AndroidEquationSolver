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
    private ExpressionParser.ExprContext mASTree;

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

        mASTree = parser.expr();
        mDepends = new HashSet<>();
        ColorfulVisitor visitor = new ColorfulVisitor();
        mColoredHtml = visitor.visit(mASTree);

        if (mErrorFlag) { throw new Exception(); }
    }

    public final HashSet<Character> getDependency() {
        return mDepends;
    }

    public final ExpressionParser.ExprContext getASTree() {
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

        @Override
        public String visitUnaryOp(ExpressionParser.UnaryOpContext ctx) {
            return ops(ctx.op.getText()) + visit(ctx.factor());
        }

        @Override
        public String visitBinaryOp(ExpressionParser.BinaryOpContext ctx) {
            String op = ctx.op.getText();

            if (op.equals("*")) {
                op = "&times;";
            }

            return visit(ctx.expr(0))
                    + ops(ctx.op.getText())
                    + visit(ctx.expr(1));
        }

        @Override
        public String visitImplicitMultiply(ExpressionParser.ImplicitMultiplyContext ctx) {
            ExpressionParser.PowerContext power = ctx.power();
            ExpressionParser.FactorContext factor = ctx.factor();

            if (!Character.isDigit(factor.getText().charAt(0))) {
                return visit(power) + visit(factor);
            } else {
                return visit(power) + "&times;" + visit(factor);
            }
        }

        @Override
        public String visitPowerOp(ExpressionParser.PowerOpContext ctx) {
            ExpressionParser.AtomContext atom = ctx.atom();
            String prefix = ops(ctx.op.getType() == ExpressionParser.EXP ? "" : "-");

            if (atom.getText().matches("([0-9]+|[0-9]*'.'[0-9]+)[eE][-+]?[0-9]+")) {
                return ops("(") + visit(ctx.atom()) + ops(")")
                        + sup(prefix + visit(ctx.power()));
            } else {
                return visit(ctx.atom()) + sup(prefix + visit(ctx.power()));
            }
        }

        @Override
        public String visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
            return String.format(
                    "<i>%s</i>%s",
                    colorize(ctx.func.getText(), "#445588"), ops("(") + visit(ctx.expr()) + ops(")"));
        }

        @Override
        public String visitBrackets(ExpressionParser.BracketsContext ctx) {
            return ops("(") + visit(ctx.expr()) + ops(")");
        }

        @Override
        public String visitVariable(ExpressionParser.VariableContext ctx) {
            char id = ctx.ID().getText().charAt(0);
            mDepends.add(id);

            String color = ctx.getText().equals("x") ? "#DD1144" : "#008080";
            return bold(colorize("" + id, color));
        }

        @Override
        public String visitLiteral(ExpressionParser.LiteralContext ctx) {
            String text = ctx.getText();
            String[] parts = text.split("e");

            StringBuilder sb = new StringBuilder(parts[0]);
            if (parts.length > 1) {
                sb.append("&times;10").append(sup(parts[1]));
            }

            return colorize(sb.toString(), "#222222");
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

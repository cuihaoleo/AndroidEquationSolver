package me.cvhc.equationsolver;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExpressionEvaluatorTest {
    @Test
    public void testGetValue() throws Exception {
        ExpressionEvaluator eval = new ExpressionEvaluator("1.5+2^2*3 + 4(2+ -2)");
        Double result = eval.getValue();
        assertEquals(Double.valueOf(13.5), result);
    }

    @Test
    public void testGetValue2() throws Exception {
        ExpressionEvaluator eval = new ExpressionEvaluator("2x^2(0.5)");
        eval.setVariable('x', 2.0);
        Double result = eval.getValue();
        assertEquals(Double.valueOf(4.0), result);
    }

    @Test
    public void testGetValue3() throws Exception {
        ExpressionEvaluator eval = new ExpressionEvaluator("-1--1.0e-1");
        Double result = eval.getValue();
        assertEquals(Double.valueOf(-0.9), result);
    }

    @Test
    public void testError() throws Exception {
        ExpressionEvaluator eval = new ExpressionEvaluator("1.2+");
        assertTrue(eval.isError());
    }

    @Test
    public void testError2() throws Exception {
        ExpressionEvaluator eval = new ExpressionEvaluator("1.2=3");
        assertTrue(eval.isError());
    }
}

package me.cvhc.equationsolver;

import org.junit.Test;

import static org.junit.Assert.*;

public class CalculatorTest {
    @Test
    public void testCalculate() throws Exception {
        ExpressionCalculator eval = new ExpressionCalculator();

        eval.setVariable(' ', "2 + c");
        eval.setVariable('c', "1.5");

        ExpressionCalculator.OptionUnion result = eval.evaluate(' ');
        assertEquals(result.getValue(), 3.5, 1e-10);
    }

    @Test
    public void testCalculate2() throws Exception {
        final int ROUND = 100;
        ExpressionCalculator eval = new ExpressionCalculator();
        double[] expected = new double[ROUND];
        double[] real = new double[ROUND];

        eval.setVariable(' ', "-1.0 + c");
        eval.setVariable('c', "x^2");

        for (int i=0; i<ROUND; i++) {
            expected[i] = -1.0 + i*i;
            eval.setVariable('x', (double)i);
            real[i] = eval.evaluate(' ').getValue();
        }

        assertArrayEquals(expected, real, 1e-10);
    }

    @Test
    public void testLoopDependency() throws Exception {
        final int ROUND = 100;
        ExpressionCalculator eval = new ExpressionCalculator();
        double[] expected = new double[ROUND];
        double[] real = new double[ROUND];

        eval.setVariable(' ', "-1.0 + a + b");
        eval.setVariable('c', "a^2 + b");
        assertFalse(eval.setVariable('b', "c^2"));
    }
}

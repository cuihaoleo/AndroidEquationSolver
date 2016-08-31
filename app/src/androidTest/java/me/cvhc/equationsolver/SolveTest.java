package me.cvhc.equationsolver;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class SolveTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void testResultOk01() {
        setMode("Bisection");
        assignId('a', "3e-16");
        assignId('b', "10^4.4");
        assignId('c', "10^10.1");
        assignId('d', "10^14.2");
        assignId('e', "10^15.5");
        addEquation("2a/x/x + 1e-4 + 1e-14/x + ba/x - adx - 2ae x^2 - x");
        runCheck(9.5582635337302632e-05);
    }

    @Test
    public void testResultOk02() {
        setMode("Bingo");
        assignId('a', "1.2e-10");
        assignId('b', "0.18");
        assignId('c', "3.2e-7");
        assignId('h', "10^-4.2");
        assignId('e', "(h^2 + b*h + bc)/b/c");
        assignId('f', "86*h*h/c/c");
        addEquation("a / x - e*x - f*x*x");
        runCheck(7.7299861993189826e-07);
    }

    @Test
    public void testResultOk03() {
        setMode("Bisection");
        assignId('z', "4.5e-9");
        assignId('i', "4.2e-7");
        assignId('j', "5.6e-11");
        addEquation("(2*x^2 + i*x) / sqrt(x^2 + i*x + i*j) - sqrt(i*j/z) * (1e-14/x - x)");
        runCheck(1.2196078049504092e-10);
    }

    @Test
    public void testResultOk04() {
        setMode("Bingo");
        assignId('z', "2.3e-9");
        assignId('i', "5.9e-2");
        assignId('j', "6.4e-5");
        addEquation("(2*x^2 + i*x) / sqrt(x^2 + i*x + i*j) - sqrt(i*j/z) * (1e-3 + 1e-14/x - x)");
        runCheck(8.3001160619836957e-04);
    }

    @Test
    public void testResultOk05() {
        setMode("Bisection");
        assignId('a', "1.74e3");
        assignId('b', "1.12e7");
        addEquation("x + 1.34e-4*(a*x + 2*b*x^2) / (1 + a*x + b*x^2) - 0.0030");
        runCheck(2.7420833649623305e-03);
    }


    @Test
    public void testResultOk06() {
        setMode("Bingo");
        assignId('z', "1.8e-10");
        assignId('a', "1.1e3");
        assignId('b', "1.1e5");
        assignId('c', "1.1e5");
        assignId('d', "2e5");
        assignId('e', "1e-3");
        addEquation("z / x - x - z / x * (b*x^2 + 2*c*x^3 + 3*d*x^4) + e");
        runCheck(1.0001601282902475e-03);
    }

    @Test
    public void testResultOk07() {
        setMode("Bisection");
        assignId('z', "1.8e-10");
        assignId('b', "1.1e5");
        assignId('c', "1.1e5");
        assignId('d', "2e5");
        addEquation("z / x - x - z / x * (b*x^2 + 2*c*x^3 + 3*d*x^4)");
        runCheck(1.3416275040969297e-05);
    }

    @Test
    public void testResultOk08() {
        setMode("Bingo");
        assignId('k', "4.9e10 / 2.82");
        assignId('z', "2.3e-9");
        addEquation("x - z / x - k*z / (x + k*z) * 0.05");
        runCheck(4.9937646582598201e-02);
    }

    // todo: this equation has two solution
    public void testResultOk09() {
        setMode("Bisection");
        assignId('z', "1.8e-10");
        assignId('a', "1.74e3");
        assignId('b', "1.12e7");
        assignId('a', "10^3.24");
        assignId('b', "10^7.05");
        addEquation("sqrt(z / (1 + a*x + b*x*x)) * (a*x + 2*b*x*x) - 0.1 + x");
        runCheck(9.9993897037046503e-02);
    }

    @Test
    public void testResultOk10() {
        setMode("Bingo");
        assignId('z', "2.3e-9");
        assignId('i', "5.9e-2");
        assignId('j', "6.4e-5");
        assignId('r', "x ^ 2 + i*x + i*j");
        assignId('s', "(i*x + 2*i*j)*0.043 / r + 0.014 + 1e-14 / x - x");
        assignId('t', "(2*z - z*(i*x + 2*i*j) / r) / s");
        addEquation("t - i*j / r * (0.043 + z / t)");
        runCheck(1.0036734890426360e-01);
    }

    @Test
    public void testResultOk11() {
        setMode("Bisection");
        assignId('i', "1.3e-7");
        assignId('j', "7.1e-15");
        addEquation("3.2e15 * x^2 + x - (2 + x / j) * 2e-26 / x^2 - 1e-14 / x");
        runCheck(9.5952346125561045e-10);
    }

    @Test
    public void testResultOk12() {
        setMode("Bingo");
        assignId('z', "3.2e-11");
        assignId('i', "1.3e-7");
        assignId('j', "7.1e-15");
        addEquation("(2*x*x + i*x) / sqrt(x*x + i*x + i*j) - sqrt(i*j/z) * (1e-14 / x - x)");
        runCheck(2.8096063063349570e-11);
    }

    @Test
    public void testResultOk13() {
        setMode("Bisection");
        addEquation("2*x / (x + 6.6e-4) ^ (1/3) - (4*6.6e-4*6.6e-4 / 3.9e-11) ^ (1/3) * (1e-14 / x - x + 0.01)");
        runCheck(7.8355764730846586e-03);
    }

    @Test
    public void testResultOk14() {
        setMode("Bingo");
        assignId('a', "5.6e-10");
        assignId('b', "1.74e3");
        assignId('c', "1.12e7");
        assignId('d', "1e-14");
        assignId('e', "0.01515*a / x + d*a / x / x - a");
        assignId('r', "(b*e + 2*c*e*e)");
        assignId('s', "(1 + b*e + c*e*e)");
        addEquation("r / s - 0.1 + e * (x + a) / a");
        runCheck(2.3631751773518458e-07);
    }

    @Test
    public void testResultOk15() {
        setMode("Bisection");
        assignId('i', "4.9e10 / 2.82");
        assignId('j', "7.24e7 / 2.82");
        addEquation("(i * x / (1 + i * x)*0.5 + j * x / (1 + j * x)*0.5 + 0.7) * 10^-3.8 * (1 + i * x) / 0.5 + x - 0.02");
        runCheck(2.9066790682251168e-09);
    }

    @Test
    public void testResultOk16() {
        setMode("Bingo");
        assignId('k', "2.09e7 / 2.82");
        assignId('z', "1.8e-10");
        addEquation("x ^ 2 * (1 + k*0.05 / (1 + k*x)) - z");
        runCheck(2.3912887876071924e-08);
    }

    @Test
    public void testResultOk17() {
        setMode("Bisection");
        assignId('i', "7.6e-3");
        assignId('j', "6.3e-8");
        assignId('m', "4.4e-13");
        assignId('k', "10^9.35");
        assignId('d', "i*j*1e-2 / (1e-6 + i*1e-4 + i*j*1e-2 + i*j*m)");
        addEquation("x - (0.15 - k*0.1*x / (1 + k*x)) * d");
        runCheck(1.3690706493800081e-07);
    }

    @Test
    public void testResultOk18() {
        setMode("Bingo");
        assignId('i', "10^-0.9");
        assignId('j', "10^-1.6");
        assignId('m', "10^-2");
        assignId('n', "10^-2.67");
        assignId('o', "10^-6.16");
        assignId('a', "10^-10.26");
        assignId('c', "0.01");
        assignId('u', "2*x^6 + i*x^5 - i*j*m*x^3 - 2*i*j*m*n*x^2 - 3*i*j*m*n*o*x - 4*i*j*m*n*o*a");
        assignId('v', "x^6 + i*x^5 + i*j*x^4 + i*j*m*x^3 + i*j*m*n*x^2 + i*j*m*n*o*x + i*j*m*n*o*a");
        addEquation("2*c + x - 1e-14/x + u / v * c");
        runCheck(3.4698788830594682e-05);
    }

    @Test
    public void testResultOk19() {
        setMode("Bisection");
        assignId('i', "3.16e16 / 3.24e5");
        assignId('j', "5.01e8/3.24e5");
        addEquation("i*x / (1 + i*x) * 0.01 + j*x /(1 + j*x)*0.1 + x  - 0.01");
        runCheck(2.5662835666153488e-08);
    }

    @Test
    public void testResultOk20() {
        setMode("Bingo");
        assignId('i', "1.1e18 / 2.8e6");
        assignId('j', "4.9e10 / 2.8e6");
        assignId('a', "1e-7");
        addEquation("(i * x / (1 + i*x) + j*x / (1 + j*x) + 1) * a * (1 + i *x ) + x - 0.02");
        runCheck(2.5397901952704268e-07);
    }

    @Test
    public void testResultOk21() {
        setMode("Bisection");
        assignId('i', "4.2e-7");
        assignId('j', "5.6e-11");
        addEquation("0.095 + x - (i*x + 2*i*j) / (x^2 + i*x + i*j) * 0.07 - 1e-14 / x");
        runCheck(1.0132831925019216e-10);
    }

    @Test
    public void testResultOk22() {
        setMode("Bingo");
        assignId('c', "1.23 / 82.03 / 0.1");
        addEquation("c + x - 1.8e-5 / (x + 1.8e-5) * c - 0.1 - 1e-14 / x");
        runCheck(3.6000617610636496e-05);
    }

    @Test
    public void testResultOk23() {
        setMode("Bisection");
        assignId('i', "5.0e-5");
        assignId('j', "1.5e-10");
        addEquation("0.05 + x - (i*x + 2*i*j) / (x^2 + i*x + i*j) * 0.05 - 1e-14/x");
        runCheck(8.6617008610254896e-08);
    }

    @Test
    public void testResultOk24() {
        setMode("Bingo");
        assignId('i', "5.9e-2");
        assignId('j', "6.4e-5");
        assignId('a', "4.2e-7");
        assignId('b', "5.6e-11");
        addEquation("0.2 + x - (i*x + 2*i*j) / (x^2 + i*x + i*j) * 0.1 - (a*x + 2*a*b) / (x^2 + a*x + a*b)*0.1 - 1e-14/x");
        runCheck(5.1822406168267872e-06);
    }

    @Test
    public void testResultOk25() {
        setMode("Bisection");
        assignId('i', "1.3e-7");
        assignId('j', "7.1e-15");
        addEquation("0.02 + x - (i*x + 2*i*j) / (x^2 + i*x + i*j) * 0.01 - 1e-14/x");
        runCheck(1.0071386965363334e-12);
    }

    @Test
    public void testResultOk26() {
        setMode("Bingo");
        assignId('i', "7.6e-3");
        assignId('j', "6.3e-8");
        assignId('m', "4.4e-13");
        addEquation("0.9 + x - (i*x^2 + 2*i*j*x + 3*i*j*m) / (x^3 + i*x^2 + i*j*x + i*j*m) * 0.3 - 1e-14/x");
        runCheck(1.3871452615832681e-13);
    }

    private void runCheck(double actual) {
        onView(allOf(withId(R.id.fab), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withId(R.id.buttonApply))
                .perform(click());

        double d = getDouble(withId(R.id.textResult));
        assertDoubleEquals(d, actual, 0.02);
    }

    private static class DecimalInputViewSettingAction implements ViewAction {
        private double mNumber;

        public DecimalInputViewSettingAction(double num) {
            mNumber = num;
        }

        @Override
        public Matcher<View> getConstraints() {
            return isAssignableFrom(DecimalInputView.class);
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void perform(UiController uiController, View view) {
            DecimalInputView dsView = (DecimalInputView) view;
            dsView.setValue(mNumber);
        }
    }

    private void setMode(String mode, final double... thresh) {
        onView(withText(mode)).perform(click());

        if (thresh.length >= 1) {
            onView(allOf(withId(R.id.threshold1), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(new DecimalInputViewSettingAction(thresh[0]));
        } else if (thresh.length >= 2) {
            onView(allOf(withId(R.id.threshold2), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                    .perform(new DecimalInputViewSettingAction(thresh[1]));
        }
    }

    private void assignId(char c, String exp) {
        if (isToggleButtonChecked(withId(R.id.toggleInputType))) {
            onView(withId(R.id.toggleInputType)).perform(click());
        }

        onView(withId(R.id.editInputNewExpression))
                .perform(typeText(c + ""))
                .perform(typeText(exp + "\n"));
    }

    private void addEquation(String exp) {
        if (!isToggleButtonChecked(withId(R.id.toggleInputType))) {
            onView(withId(R.id.toggleInputType)).perform(click());
        }

        onView(withId(R.id.editInputNewExpression))
                .perform(typeText(exp + "\n"));
    }

    static public void assertDoubleEquals(double expected, double actual, double deltaRatio) {
        assertEquals(null, expected, actual, Math.abs(actual * deltaRatio));
    }

    private double getDouble(final Matcher<View> matcher) {
        final Number[] holder = { null };

        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "Read number from TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView textView = (TextView)view;
                String str = textView.getText().toString();

                holder[0] = Double.parseDouble((String)textView.getTag());
            }
        });

        return holder[0].doubleValue();
    }

    private boolean isToggleButtonChecked(final Matcher<View> matcher) {
        final Boolean[] holder = { null };

        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(ToggleButton.class);
            }

            @Override
            public String getDescription() {
                return "Read toggle state of a ToggleButton";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ToggleButton dsView = (ToggleButton)view;
                holder[0] = dsView.isChecked();
            }
        });

        return holder[0];
    }
}

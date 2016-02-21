package me.cvhc.equationsolver;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class CustomTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void testIndeterminateConstants() {
        onView(withId(R.id.textEquation))
                .perform(typeText("a=b"), closeSoftKeyboard());
        onView(withId(R.id.buttonSolve))
                .perform(click());

        onView(withText(R.string.indeterminate_constants)).check(matches(isDisplayed()));
    }

    @Test
    public void testSettingID() {
        onView(withId(R.id.textEquation))
                .perform(typeText("2x=a"), closeSoftKeyboard());
        onView(allOf(withId(R.id.textViewIDCharacter), withText("a")))
                .perform(click());

        onView(withId(R.id.editExpression))
                .perform(typeText("1.0"), closeSoftKeyboard());
        onView(withId(R.id.buttonPositive))
                .perform(click());
        onView(withId(R.id.buttonSolve))
                .perform(click());
        onView(withId(R.id.editCoefficient))
                .check(matches(withText("0.5")));
    }
}
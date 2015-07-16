package ac.at.tuwien.inso.ble.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by manu on 16.07.2015.
 */
public class BaseCalculatorTest {

    private BaseCalculator calc;

    @Before
    public void setUp() {

        LimitedList<Double> heartRate = new LimitedList<Double>(10);
        heartRate.add(60.0);
        heartRate.add(62.0);
        heartRate.add(66.0);
        heartRate.add(68.0);
        heartRate.add(73.0);
        heartRate.add(65.0);
        heartRate.add(60.0);
        heartRate.add(58.0);
        heartRate.add(58.0);
        heartRate.add(60.0);
        calc = new BaseCalculator(heartRate);
    }

    @Test
    public void testGetSumHr() {
        Assert.assertEquals(630.0, calc.getSumHr(), 0.0);
    }

    @Test
    public void testGetMeanHr() {
        Assert.assertEquals(63.0, calc.getMeanHr(), 0.0);
    }

    @Test
    public void testGetSumRr() {
        Assert.assertEquals(9573.146, calc.getSumRr(), 0.0001);
    }

    @Test
    public void testGetMeanRr() {
        Assert.assertEquals(957.3146, calc.getMeanRr(), 0.0001);
    }

    @Test
    public void testGetSdnn() {
        Assert.assertEquals(70.6588, calc.getSdnn(), 0.0001);
    }

    @Test
    public void testGetRmssd() {
        Assert.assertEquals(58.4982, calc.getRmssd(), 0.0001);
    }

    @Test
    public void testGetPnn50() {
        Assert.assertEquals(0.4444, calc.getPnn50(), 0.0001);
    }
}

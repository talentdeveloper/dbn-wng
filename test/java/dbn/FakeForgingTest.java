/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package dbn;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

public class FakeForgingTest extends AbstractForgingTest {

    @Before
    public void init() {
        Properties properties = AbstractForgingTest.newTestProperties();
        properties.setProperty("dbn.disableGenerateBlocksThread", "false");
        properties.setProperty("dbn.enableFakeForging", "true");
        properties.setProperty("dbn.timeMultiplier", "1");
        AbstractForgingTest.init(properties);
        Assert.assertTrue("dbn.fakeForgingAccount must be defined in dbn.properties", Dbn.getStringProperty("dbn.fakeForgingAccount") != null);
    }

    @Test
    public void fakeForgingTest() {
        forgeTo(startHeight + 10, testForgingSecretPhrase);
    }

    @After
    public void destroy() {
        AbstractForgingTest.shutdown();
    }

}

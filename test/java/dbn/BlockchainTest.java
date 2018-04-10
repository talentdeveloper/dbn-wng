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

import dbn.util.Logger;
import dbn.util.Time;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.util.Properties;

public abstract class BlockchainTest extends AbstractBlockchainTest {

    protected static Tester FORGY;
    protected static Tester ALICE;
    protected static Tester BOB;
    protected static Tester CHUCK;
    protected static Tester DAVE;

    protected static int baseHeight;

    protected static String forgerSecretPhrase = "aSykrgKGZNlSVOMDxkZZgbTvQqJPGtsBggb";
    protected static final String forgerAccountId = "DBN-9KZM-KNYY-QBXZ-5TD8V";
    protected static String secretPhrase1 = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
    protected static String secretPhrase2 = "rshw9abtpsa2";
    protected static String secretPhrase3 = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    protected static String secretPhrase4 = "t9G2ymCmDsQij7VtYinqrbGCOAtDDA3WiNr";

    private static final String aliceSecretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
    private static final String bobSecretPhrase2 = "rshw9abtpsa2";
    private static final String chuckSecretPhrase = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    private static final String daveSecretPhrase = "t9G2ymCmDsQij7VtYinqrbGCOAtDDA3WiNr";

    protected static boolean isDbnInitted = false;
    protected static boolean needShutdownAfterClass = false;

    public static void initDbn() {
        if (!isDbnInitted) {
            Properties properties = ManualForgingTest.newTestProperties();
            properties.setProperty("dbn.isTestnet", "true");
            properties.setProperty("dbn.isOffline", "true");
            properties.setProperty("dbn.enableFakeForging", "true");
            properties.setProperty("dbn.fakeForgingAccount", forgerAccountId);
            properties.setProperty("dbn.timeMultiplier", "1");
            properties.setProperty("dbn.testnetGuaranteedBalanceConfirmations", "1");
            properties.setProperty("dbn.testnetLeasingDelay", "1");
            properties.setProperty("dbn.disableProcessTransactionsThread", "true");
            properties.setProperty("dbn.deleteFinishedShufflings", "false");
            AbstractForgingTest.init(properties);
            isDbnInitted = true;
        }
    }
    
    @BeforeClass
    public static void init() {
        needShutdownAfterClass = !isDbnInitted;
        initDbn();
        
        Dbn.setTime(new Time.CounterTime(Dbn.getEpochTime()));
        baseHeight = blockchain.getHeight();
        Logger.logMessage("baseHeight: " + baseHeight);
        FORGY = new Tester(forgerSecretPhrase);
        ALICE = new Tester(aliceSecretPhrase);
        BOB = new Tester(bobSecretPhrase2);
        CHUCK = new Tester(chuckSecretPhrase);
        DAVE = new Tester(daveSecretPhrase);
    }

    @AfterClass
    public static void shutdown() {
        if (needShutdownAfterClass) {
            Dbn.shutdown();
        }
    }

    @After
    public void destroy() {
        TransactionProcessorImpl.getInstance().clearUnconfirmedTransactions();
        blockchainProcessor.popOffTo(baseHeight);
        shutdown();
    }

    public static void generateBlock() {
        try {
            blockchainProcessor.generateBlock(forgerSecretPhrase, Dbn.getEpochTime());
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public static void generateBlocks(int howMany) {
        for (int i = 0; i < howMany; i++) {
            generateBlock();
        }
    }
}

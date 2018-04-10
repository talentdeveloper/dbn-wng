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

package dbn.http.monetarysystem;

import dbn.Account;
import dbn.BlockchainTest;
import dbn.Constants;
import dbn.CurrencyType;
import dbn.crypto.Crypto;
import dbn.http.APICall;
import dbn.util.Convert;
import dbn.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyReserveAndClaim extends BlockchainTest {

    @Test
    public void reserveIncrease() {
        APICall apiCall = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                issuanceHeight(baseHeight + 5).
                minReservePerUnitDQT((long) 1).
                initialSupply((long)0).
                reserveSupply((long)100000).
                build();
        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall);
        reserveIncreaseImpl(currencyId, ALICE.getSecretPhrase(), BOB.getSecretPhrase());
    }

    @Test
    public void cancelCrowdFunding() {
        APICall apiCall1 = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                issuanceHeight(baseHeight + 4).
                minReservePerUnitDQT((long) 11).
                initialSupply((long)0).
                reserveSupply((long)100000).
                build();
        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall1);
        long balanceDQT1 = ALICE.getBalance();
        long balanceDQT2 = BOB.getBalance();
        reserveIncreaseImpl(currencyId, ALICE.getSecretPhrase(), BOB.getSecretPhrase());
        generateBlock(); // cancellation of crowd funding because of insufficient funds
        APICall apiCall = new APICall.Builder("getCurrencyFounders").
                feeDQT(Constants.ONE_DBN).
                param("currency", currencyId).
                build();
        JSONObject getFoundersResponse = apiCall.invoke();
        Logger.logMessage("getFoundersResponse: " + getFoundersResponse);
        Assert.assertTrue(((JSONArray)getFoundersResponse.get("founders")).size() == 0);
        Assert.assertEquals(balanceDQT1 - Constants.ONE_DBN, ALICE.getBalance());
        Assert.assertEquals(balanceDQT2 - 2*Constants.ONE_DBN, BOB.getBalance());
    }

    @Test
    public void crowdFundingDistribution() {
        APICall apiCall = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                initialSupply((long) 0).
                reserveSupply((long) 100000).
                issuanceHeight(baseHeight + 4).
                minReservePerUnitDQT((long) 10).
                build();

        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall);
        long balanceDQT1 = ALICE.getBalance();
        long balanceDQT2 = BOB.getBalance();
        reserveIncreaseImpl(currencyId, ALICE.getSecretPhrase(), BOB.getSecretPhrase());
        generateBlock(); // distribution of currency to founders
        Assert.assertEquals(20000, ALICE.getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(80000, BOB.getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(balanceDQT1 - Constants.ONE_DBN - 200000 + (100000*10), ALICE.getBalance());
        Assert.assertEquals(balanceDQT2 - 2*Constants.ONE_DBN - 800000, BOB.getBalance());
    }

    @Test
    public void crowdFundingDistributionRounding() {
        APICall apiCall = new TestCurrencyIssuance.Builder().
                type(CurrencyType.RESERVABLE.getCode() | CurrencyType.EXCHANGEABLE.getCode()).
                initialSupply((long)0).
                reserveSupply((long)24).
                maxSupply((long) 24).
                issuanceHeight(baseHeight + 4).
                minReservePerUnitDQT((long) 10).
                build();

        String currencyId = TestCurrencyIssuance.issueCurrencyApi(apiCall);
        long balanceDQT1 = ALICE.getBalance();
        long balanceDQT2 = BOB.getBalance();
        long balanceDQT3 = CHUCK.getBalance();
        reserveIncreaseImpl(currencyId, BOB.getSecretPhrase(), CHUCK.getSecretPhrase());
        generateBlock(); // distribution of currency to founders

        // account 2 balance round(24 * 0.2) = round(4.8) = 4
        // account 3 balance round(24 * 0.8) = round(19.2) = 19
        // issuer receives the leftover of 1
        Assert.assertEquals(4, BOB.getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(19, CHUCK.getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(1, ALICE.getCurrencyUnits(Convert.parseAccountId(currencyId)));
        Assert.assertEquals(balanceDQT1 + 24 * 10, ALICE.getBalance());
        Assert.assertEquals(balanceDQT2 - Constants.ONE_DBN - 24 * 2, BOB.getBalance());
        Assert.assertEquals(balanceDQT3 - 2 * Constants.ONE_DBN - 24 * 8, CHUCK.getBalance());

        apiCall = new APICall.Builder("getCurrency").
                param("currency", currencyId).
                build();
        JSONObject response = apiCall.invoke();
        Assert.assertEquals("24", response.get("currentSupply"));
    }

    public void reserveIncreaseImpl(String currencyId, String secret1, String secret2) {
        APICall apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secret1).
                feeDQT(Constants.ONE_DBN).
                param("currency", currencyId).
                param("amountPerUnitDQT", "" + 2).
                build();
        JSONObject reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);
        generateBlock();

        // Two increase reserve transactions in the same block
        apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secret2).
                feeDQT(Constants.ONE_DBN).
                param("currency", currencyId).
                param("amountPerUnitDQT", "" + 3).
                build();
        reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);

        apiCall = new APICall.Builder("currencyReserveIncrease").
                secretPhrase(secret2).
                feeDQT(Constants.ONE_DBN).
                param("currency", currencyId).
                param("amountPerUnitDQT", "" + 5).
                build();
        reserveIncreaseResponse = apiCall.invoke();
        Logger.logMessage("reserveIncreaseResponse: " + reserveIncreaseResponse);

        generateBlock();

        apiCall = new APICall.Builder("getCurrencyFounders").
                feeDQT(Constants.ONE_DBN).
                param("currency", currencyId).
                build();
        JSONObject getFoundersResponse = apiCall.invoke();
        Logger.logMessage("getFoundersResponse: " + getFoundersResponse);

        JSONArray founders = (JSONArray)getFoundersResponse.get("founders");
        JSONObject founder1 = (JSONObject)founders.get(0);
        Assert.assertTrue(Long.toUnsignedString(Account.getId(Crypto.getPublicKey(secret1))).equals(founder1.get("account")) ||
                Long.toUnsignedString(Account.getId(Crypto.getPublicKey(secret2))).equals(founder1.get("account")));
        Assert.assertTrue(String.valueOf(3L + 5L).equals(founder1.get("amountPerUnitDQT")) || String.valueOf(2L).equals(founder1.get("amountPerUnitDQT")));

        JSONObject founder2 = (JSONObject)founders.get(1);
        Assert.assertTrue(Long.toUnsignedString(Account.getId(Crypto.getPublicKey(secret1))).equals(founder2.get("account")) ||
                Long.toUnsignedString(Account.getId(Crypto.getPublicKey(secret2))).equals(founder2.get("account")));
        Assert.assertTrue(String.valueOf(3L + 5L).equals(founder2.get("amountPerUnitDQT")) || String.valueOf(2L).equals(founder2.get("amountPerUnitDQT")));
    }

}

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

package dbn.http;

import dbn.Account;
import dbn.Attachment;
import dbn.Constants;
import dbn.Currency;
import dbn.DbnException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Increase the value of currency units by paying DBN
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>amountPerUnitDQT - the DQT amount invested into increasing the value of a single currency unit.<br>
 * This value is multiplied by the currency total supply and the result is deducted from the sender's account balance.
 * </ul>
 * <p>
 * Constraints
 * <p>
 * This API is allowed only when the currency is {@link dbn.CurrencyType#RESERVABLE} and is not yet active.
 * <p>
 * The sender account is registered as a founder. Once the currency becomes active
 * the total supply is distributed between the founders based on their proportional investment<br>
 * The list of founders and their DQT investment can be obtained using the {@link dbn.http.GetCurrencyFounders} API.
 */

public final class CurrencyReserveIncrease extends CreateTransaction {

    static final CurrencyReserveIncrease instance = new CurrencyReserveIncrease();

    private CurrencyReserveIncrease() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "amountPerUnitDQT");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws DbnException {
        Currency currency = ParameterParser.getCurrency(req);
        long amountPerUnitDQT = ParameterParser.getLong(req, "amountPerUnitDQT", 1L, Constants.MAX_BALANCE_DQT, true);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemReserveIncrease(currency.getId(), amountPerUnitDQT);
        return createTransaction(req, account, attachment);

    }

}

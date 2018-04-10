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
import dbn.Asset;
import dbn.Attachment;
import dbn.DbnException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static dbn.http.JSONResponses.NOT_ENOUGH_ASSETS;

public final class PlaceAskOrder extends CreateTransaction {

    static final PlaceAskOrder instance = new PlaceAskOrder();

    private PlaceAskOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityQNT", "priceDQT");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws DbnException {

        Asset asset = ParameterParser.getAsset(req);
        long priceDQT = ParameterParser.getPriceDQT(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset.getId(), quantityQNT, priceDQT);
        try {
            return createTransaction(req, account, attachment);
        } catch (DbnException.InsufficientBalanceException e) {
            return NOT_ENOUGH_ASSETS;
        }
    }

}

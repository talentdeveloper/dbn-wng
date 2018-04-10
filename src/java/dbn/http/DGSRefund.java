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
import dbn.DigitalGoodsStore;
import dbn.DbnException;
import dbn.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static dbn.http.JSONResponses.DUPLICATE_REFUND;
import static dbn.http.JSONResponses.GOODS_NOT_DELIVERED;
import static dbn.http.JSONResponses.INCORRECT_DGS_REFUND;
import static dbn.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSRefund extends CreateTransaction {

    static final DGSRefund instance = new DGSRefund();

    private DGSRefund() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "purchase", "refundDQT");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws DbnException {

        Account sellerAccount = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (sellerAccount.getId() != purchase.getSellerId()) {
            return INCORRECT_PURCHASE;
        }
        if (purchase.getRefundNote() != null) {
            return DUPLICATE_REFUND;
        }
        if (purchase.getEncryptedGoods() == null) {
            return GOODS_NOT_DELIVERED;
        }

        String refundValueDQT = Convert.emptyToNull(req.getParameter("refundDQT"));
        long refundDQT = 0;
        try {
            if (refundValueDQT != null) {
                refundDQT = Long.parseLong(refundValueDQT);
            }
        } catch (RuntimeException e) {
            return INCORRECT_DGS_REFUND;
        }
        if (refundDQT < 0 || refundDQT > Constants.MAX_BALANCE_DQT) {
            return INCORRECT_DGS_REFUND;
        }

        Account buyerAccount = Account.getAccount(purchase.getBuyerId());

        Attachment attachment = new Attachment.DigitalGoodsRefund(purchase.getId(), refundDQT);
        return createTransaction(req, sellerAccount, buyerAccount.getId(), 0, attachment);

    }

}

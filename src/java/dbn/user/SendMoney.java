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

package dbn.user;

import dbn.Account;
import dbn.Attachment;
import dbn.Constants;
import dbn.Dbn;
import dbn.DbnException;
import dbn.Transaction;
import dbn.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static dbn.user.JSONResponses.NOTIFY_OF_ACCEPTED_TRANSACTION;

public final class SendMoney extends UserServlet.UserRequestHandler {

    static final SendMoney instance = new SendMoney();

    private SendMoney() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws DbnException.ValidationException, IOException {
        if (user.getSecretPhrase() == null) {
            return null;
        }

        String recipientValue = req.getParameter("recipient");
        String amountValue = req.getParameter("amountDBN");
        String feeValue = req.getParameter("feeDBN");
        String deadlineValue = req.getParameter("deadline");
        String secretPhrase = req.getParameter("secretPhrase");

        long recipient;
        long amountDQT = 0;
        long feeDQT = 0;
        short deadline = 0;

        try {

            recipient = Convert.parseUnsignedLong(recipientValue);
            if (recipient == 0) throw new IllegalArgumentException("invalid recipient");
            amountDQT = Convert.parseDBN(amountValue.trim());
            feeDQT = Convert.parseDBN(feeValue.trim());
            deadline = (short)(Double.parseDouble(deadlineValue) * 60);

        } catch (RuntimeException e) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "One of the fields is filled incorrectly!");
            response.put("recipient", recipientValue);
            response.put("amountDBN", amountValue);
            response.put("feeDBN", feeValue);
            response.put("deadline", deadlineValue);

            return response;
        }

        if (! user.getSecretPhrase().equals(secretPhrase)) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "Wrong secret phrase!");
            response.put("recipient", recipientValue);
            response.put("amountDBN", amountValue);
            response.put("feeDBN", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        } else if (amountDQT <= 0 || amountDQT > Constants.MAX_BALANCE_DQT) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Amount\" must be greater than 0!");
            response.put("recipient", recipientValue);
            response.put("amountDBN", amountValue);
            response.put("feeDBN", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        } else if (feeDQT < Constants.ONE_DBN || feeDQT > Constants.MAX_BALANCE_DQT) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Fee\" must be at least 1 DBN!");
            response.put("recipient", recipientValue);
            response.put("amountDBN", amountValue);
            response.put("feeDBN", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        } else if (deadline < 1 || deadline > 1440) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Deadline\" must be greater or equal to 1 minute and less than 24 hours!");
            response.put("recipient", recipientValue);
            response.put("amountDBN", amountValue);
            response.put("feeDBN", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        }

        Account account = Account.getAccount(user.getPublicKey());
        if (account == null || Math.addExact(amountDQT, feeDQT) > account.getUnconfirmedBalanceDQT()) {

            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "Not enough funds!");
            response.put("recipient", recipientValue);
            response.put("amountDBN", amountValue);
            response.put("feeDBN", feeValue);
            response.put("deadline", deadlineValue);

            return response;

        } else {

            final Transaction transaction = Dbn.newTransactionBuilder(user.getPublicKey(),
                    amountDQT, feeDQT, deadline, Attachment.ORDINARY_PAYMENT).recipientId(recipient).build(secretPhrase);

            Dbn.getTransactionProcessor().broadcast(transaction);

            return NOTIFY_OF_ACCEPTED_TRANSACTION;

        }
    }

    @Override
    boolean requirePost() {
        return true;
    }

}

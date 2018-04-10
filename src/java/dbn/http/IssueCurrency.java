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
import dbn.CurrencyType;
import dbn.DbnException;
import dbn.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Issue a currency on the DBN blockchain
 * <p>
 * A currency is the basic block of the DBN Monetary System it can be exchanged with DBN, transferred between accounts,
 * minted using proof of work methods, reserved and claimed as a crowd funding tool.
 * <p>
 * Pass the following parameters in order to issue a currency
 * <ul>
 * <li>name - unique identifier of the currency composed of between 3 to 10 latin alphabetic symbols and numbers, name must be
 * no shorter than code. name and code are mutually unique.
 * <li>code - unique 3 to 5 letter currency trading symbol composed of upper case latin letters
 * <li>description - free text description of the currency limited to 1000 characters
 * <li>type - a numeric value representing a bit vector modeling the currency capabilities (see below)
 * <li>ruleset - for future use, always set to 0
 * <li>maxSupply - the total number of currency units which can be created
 * <li>initialSupply - the number of currency units created when the currency is issued (pre-mine)
 * <li>decimals - currency units are divisible to this number of decimals
 * <li>issuanceHeight - the blockchain height at which the currency would become active
 * For {@link dbn.CurrencyType#RESERVABLE} currency
 * <li>minReservePerUnitDQT - the minimum DBN value per unit to allow the currency to become active
 * For {@link dbn.CurrencyType#RESERVABLE} currency
 * <li>reserveSupply - the number of units that will be distributed to founders when currency becomes active (less initialSupply)
 * For {@link dbn.CurrencyType#RESERVABLE} currency
 * <li>minDifficulty - for mint-able currency, the exponent of the initial difficulty.
 * For {@link dbn.CurrencyType#MINTABLE} currency
 * <li>maxDifficulty - for mint-able currency, the exponent of the final difficulty.
 * For {@link dbn.CurrencyType#MINTABLE} currency
 * <li>algorithm - the hashing {@link dbn.crypto.HashFunction algorithm} used for minting.
 * For {@link dbn.CurrencyType#MINTABLE} currency
 * </ul>
 * <p>
 * Constraints
 * <ul>
 * <li>A given currency can not be neither {@link dbn.CurrencyType#EXCHANGEABLE} nor {@link dbn.CurrencyType#CLAIMABLE}.<br>
 * <li>A {@link dbn.CurrencyType#RESERVABLE} currency becomes active once the blockchain height reaches the currency issuance height.<br>
 * At this time, if the minReservePerUnitDQT has not been reached the currency issuance is cancelled and
 * funds are returned to the founders.<br>
 * Otherwise the currency becomes active and remains active until deleted, provided deletion is possible.
 * When a {@link dbn.CurrencyType#RESERVABLE} becomes active, in case it is {@link dbn.CurrencyType#CLAIMABLE} the DBN used for
 * reserving the currency are locked until they are claimed back.
 * When a {@link dbn.CurrencyType#RESERVABLE} becomes active, in case it is non {@link dbn.CurrencyType#CLAIMABLE} the DBN used for
 * reserving the currency are sent to the issuer account as crowd funding.
 * <li>When issuing a {@link dbn.CurrencyType#MINTABLE} currency, the number of units per {@link dbn.http.CurrencyMint} cannot exceed 0.01% of the
 * total supply. Therefore make sure totalSupply &gt; 10000 or otherwise the currency cannot be minted
 * <li>difficulty is calculated as follows<br>
 * difficulty of minting the first unit is based on 2^minDifficulty<br>
 * difficulty of minting the last unit is based on 2^maxDifficulty<br>
 * difficulty increases linearly from min to max based on the ratio between the current number of units and the total supply<br>
 * difficulty increases linearly with the number units minted per {@link dbn.http.CurrencyMint}<br>
 * </ul>
 *
 * @see dbn.CurrencyType
 * @see dbn.crypto.HashFunction
 */
public final class IssueCurrency extends CreateTransaction {

    static final IssueCurrency instance = new IssueCurrency();

    private IssueCurrency() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION},
                "name", "code", "description", "type", "initialSupply", "reserveSupply", "maxSupply", "issuanceHeight", "minReservePerUnitDQT",
                "minDifficulty", "maxDifficulty", "ruleset", "algorithm", "decimals");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws DbnException {
        String name = Convert.nullToEmpty(req.getParameter("name"));
        String code = Convert.nullToEmpty(req.getParameter("code"));
        String description = Convert.nullToEmpty(req.getParameter("description"));

        if (name.length() < Constants.MIN_CURRENCY_NAME_LENGTH || name.length() > Constants.MAX_CURRENCY_NAME_LENGTH) {
            return JSONResponses.INCORRECT_CURRENCY_NAME_LENGTH;
        }
        if (code.length() < Constants.MIN_CURRENCY_CODE_LENGTH || code.length() > Constants.MAX_CURRENCY_CODE_LENGTH) {
            return JSONResponses.INCORRECT_CURRENCY_CODE_LENGTH;
        }
        if (description.length() > Constants.MAX_CURRENCY_DESCRIPTION_LENGTH) {
            return JSONResponses.INCORRECT_CURRENCY_DESCRIPTION_LENGTH;
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return JSONResponses.INCORRECT_CURRENCY_NAME;
            }
        }
        for (int i = 0; i < code.length(); i++) {
            if (Constants.ALLOWED_CURRENCY_CODE_LETTERS.indexOf(code.charAt(i)) < 0) {
                return JSONResponses.INCORRECT_CURRENCY_CODE;
            }
        }

        int type = 0;
        if (Convert.emptyToNull(req.getParameter("type")) == null) {
            for (CurrencyType currencyType : CurrencyType.values()) {
                if ("1".equals(req.getParameter(currencyType.toString().toLowerCase()))) {
                    type = type | currencyType.getCode();
                }
            }
        } else {
            type = ParameterParser.getInt(req, "type", 0, Integer.MAX_VALUE, false);
        }

        long maxSupply = ParameterParser.getLong(req, "maxSupply", 1, Constants.MAX_CURRENCY_TOTAL_SUPPLY, false);
        long reserveSupply = ParameterParser.getLong(req, "reserveSupply", 0, maxSupply, false);
        long initialSupply = ParameterParser.getLong(req, "initialSupply", 0, maxSupply, false);
        int issuanceHeight = ParameterParser.getInt(req, "issuanceHeight", 0, Integer.MAX_VALUE, false);
        long minReservePerUnit = ParameterParser.getLong(req, "minReservePerUnitDQT", 1, Constants.MAX_BALANCE_DQT, false);
        int minDifficulty = ParameterParser.getInt(req, "minDifficulty", 1, 255, false);
        int maxDifficulty = ParameterParser.getInt(req, "maxDifficulty", 1, 255, false);
        byte ruleset = ParameterParser.getByte(req, "ruleset", (byte)0, Byte.MAX_VALUE, false);
        byte algorithm = ParameterParser.getByte(req, "algorithm", (byte)0, Byte.MAX_VALUE, false);
        byte decimals = ParameterParser.getByte(req, "decimals", (byte)0, Byte.MAX_VALUE, false);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemCurrencyIssuance(name, code, description, (byte)type, initialSupply,
                reserveSupply, maxSupply, issuanceHeight, minReservePerUnit, minDifficulty, maxDifficulty, ruleset, algorithm, decimals);

        return createTransaction(req, account, attachment);
    }
}

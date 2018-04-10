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

import dbn.AccountLedger.LedgerEvent;
import dbn.db.DbClause;
import dbn.db.DbIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class CurrencyExchangeOffer {

    public static final class AvailableOffers {

        private final long rateDQT;
        private final long units;
        private final long amountDQT;

        private AvailableOffers(long rateDQT, long units, long amountDQT) {
            this.rateDQT = rateDQT;
            this.units = units;
            this.amountDQT = amountDQT;
        }

        public long getRateDQT() {
            return rateDQT;
        }

        public long getUnits() {
            return units;
        }

        public long getAmountDQT() {
            return amountDQT;
        }

    }

    static {

        Dbn.getBlockchainProcessor().addListener(block -> {
            if (block.getHeight() <= Constants.MONETARY_SYSTEM_BLOCK) {
                return;
            }
            List<CurrencyBuyOffer> expired = new ArrayList<>();
            try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(new DbClause.IntClause("expiration_height", block.getHeight()), 0, -1)) {
                for (CurrencyBuyOffer offer : offers) {
                    expired.add(offer);
                }
            }
            expired.forEach((offer) -> CurrencyExchangeOffer.removeOffer(LedgerEvent.CURRENCY_OFFER_EXPIRED, offer));
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

    }

    static void publishOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        CurrencyBuyOffer previousOffer = CurrencyBuyOffer.getOffer(attachment.getCurrencyId(), transaction.getSenderId());
        if (previousOffer != null) {
            CurrencyExchangeOffer.removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, previousOffer);
        }
        CurrencyBuyOffer.addOffer(transaction, attachment);
        CurrencySellOffer.addOffer(transaction, attachment);
    }

    private static AvailableOffers calculateTotal(List<CurrencyExchangeOffer> offers, final long units) {
        long totalAmountDQT = 0;
        long remainingUnits = units;
        long rateDQT = 0;
        for (CurrencyExchangeOffer offer : offers) {
            if (remainingUnits == 0) {
                break;
            }
            rateDQT = offer.getRateDQT();
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountDQT = Math.multiplyExact(curUnits, offer.getRateDQT());
            totalAmountDQT = Math.addExact(totalAmountDQT, curAmountDQT);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);
        }
        return new AvailableOffers(rateDQT, Math.subtractExact(units, remainingUnits), totalAmountDQT);
    }

    static final DbClause availableOnlyDbClause = new DbClause.LongClause("unit_limit", DbClause.Op.NE, 0)
            .and(new DbClause.LongClause("supply", DbClause.Op.NE, 0));

    public static AvailableOffers getAvailableToSell(final long currencyId, final long units) {
        return calculateTotal(getAvailableBuyOffers(currencyId, 0L), units);
    }

    private static List<CurrencyExchangeOffer> getAvailableBuyOffers(long currencyId, long minRateDQT) {
        List<CurrencyExchangeOffer> currencyExchangeOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        if (minRateDQT > 0) {
            dbClause = dbClause.and(new DbClause.LongClause("rate", DbClause.Op.GTE, minRateDQT));
        }
        try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(dbClause, 0, -1,
                " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencyBuyOffer offer : offers) {
                currencyExchangeOffers.add(offer);
            }
        }
        return currencyExchangeOffers;
    }

    static void exchangeCurrencyForDBN(Transaction transaction, Account account, final long currencyId, final long rateDQT, final long units) {
        List<CurrencyExchangeOffer> currencyBuyOffers = getAvailableBuyOffers(currencyId, rateDQT);

        long totalAmountDQT = 0;
        long remainingUnits = units;
        for (CurrencyExchangeOffer offer : currencyBuyOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountDQT = Math.multiplyExact(curUnits, offer.getRateDQT());

            totalAmountDQT = Math.addExact(totalAmountDQT, curAmountDQT);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);

            offer.decreaseLimitAndSupply(curUnits);
            long excess = offer.getCounterOffer().increaseSupply(curUnits);

            Account counterAccount = Account.getAccount(offer.getAccountId());
            counterAccount.addToBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), -curAmountDQT);
            counterAccount.addToCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, curUnits);
            counterAccount.addToUnconfirmedCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, excess);
            Exchange.addExchange(transaction, currencyId, offer, account.getId(), offer.getAccountId(), curUnits);
        }
        long transactionId = transaction.getId();
        account.addToBalanceAndUnconfirmedBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId, totalAmountDQT);
        account.addToCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, -(units - remainingUnits));
        account.addToUnconfirmedCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, remainingUnits);
    }

    public static AvailableOffers getAvailableToBuy(final long currencyId, final long units) {
        return calculateTotal(getAvailableSellOffers(currencyId, 0L), units);
    }

    private static List<CurrencyExchangeOffer> getAvailableSellOffers(long currencyId, long maxRateDQT) {
        List<CurrencyExchangeOffer> currencySellOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        if (maxRateDQT > 0) {
            dbClause = dbClause.and(new DbClause.LongClause("rate", DbClause.Op.LTE, maxRateDQT));
        }
        try (DbIterator<CurrencySellOffer> offers = CurrencySellOffer.getOffers(dbClause, 0, -1,
                " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencySellOffer offer : offers) {
                currencySellOffers.add(offer);
            }
        }
        return currencySellOffers;
    }

    static void exchangeDBNForCurrency(Transaction transaction, Account account, final long currencyId, final long rateDQT, final long units) {
        List<CurrencyExchangeOffer> currencySellOffers = getAvailableSellOffers(currencyId, rateDQT);

        if (Dbn.getBlockchain().getHeight() < Constants.SHUFFLING_BLOCK) {
            long totalUnits = 0;
            long totalAmountDQT = Math.multiplyExact(units, rateDQT);
            long remainingAmountDQT = totalAmountDQT;

            for (CurrencyExchangeOffer offer : currencySellOffers) {
                if (remainingAmountDQT == 0) {
                    break;
                }
                long curUnits = Math.min(Math.min(remainingAmountDQT / offer.getRateDQT(), offer.getSupply()), offer.getLimit());
                if (curUnits == 0) {
                    continue;
                }
                long curAmountDQT = Math.multiplyExact(curUnits, offer.getRateDQT());

                totalUnits = Math.addExact(totalUnits, curUnits);
                remainingAmountDQT = Math.subtractExact(remainingAmountDQT, curAmountDQT);

                offer.decreaseLimitAndSupply(curUnits);
                long excess = offer.getCounterOffer().increaseSupply(curUnits);

                Account counterAccount = Account.getAccount(offer.getAccountId());
                counterAccount.addToBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), curAmountDQT);
                counterAccount.addToUnconfirmedBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(),
                        Math.addExact(
                                Math.multiplyExact(curUnits - excess, offer.getRateDQT() - offer.getCounterOffer().getRateDQT()),
                                Math.multiplyExact(excess, offer.getRateDQT())
                        )
                );
                counterAccount.addToCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, -curUnits);
                Exchange.addExchange(transaction, currencyId, offer, offer.getAccountId(), account.getId(), curUnits);
            }
            long transactionId = transaction.getId();
            account.addToCurrencyAndUnconfirmedCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, transactionId,
                    currencyId, totalUnits);
            account.addToBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId, -(totalAmountDQT - remainingAmountDQT));
            account.addToUnconfirmedBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId, remainingAmountDQT);
        } else {
            long totalAmountDQT = 0;
            long remainingUnits = units;

            for (CurrencyExchangeOffer offer : currencySellOffers) {
                if (remainingUnits == 0) {
                    break;
                }
                long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
                long curAmountDQT = Math.multiplyExact(curUnits, offer.getRateDQT());

                totalAmountDQT = Math.addExact(totalAmountDQT, curAmountDQT);
                remainingUnits = Math.subtractExact(remainingUnits, curUnits);

                offer.decreaseLimitAndSupply(curUnits);
                long excess = offer.getCounterOffer().increaseSupply(curUnits);

                Account counterAccount = Account.getAccount(offer.getAccountId());
                counterAccount.addToBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), curAmountDQT);
                counterAccount.addToUnconfirmedBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(),
                        Math.addExact(
                                Math.multiplyExact(curUnits - excess, offer.getRateDQT() - offer.getCounterOffer().getRateDQT()),
                                Math.multiplyExact(excess, offer.getRateDQT())
                        )
                );
                counterAccount.addToCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, -curUnits);
                Exchange.addExchange(transaction, currencyId, offer, offer.getAccountId(), account.getId(), curUnits);
            }
            long transactionId = transaction.getId();
            account.addToCurrencyAndUnconfirmedCurrencyUnits(LedgerEvent.CURRENCY_EXCHANGE, transactionId,
                    currencyId, Math.subtractExact(units, remainingUnits));
            account.addToBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId, -totalAmountDQT);
            account.addToUnconfirmedBalanceDQT(LedgerEvent.CURRENCY_EXCHANGE, transactionId, Math.multiplyExact(units, rateDQT) - totalAmountDQT);
        }
    }

    static void removeOffer(LedgerEvent event, CurrencyBuyOffer buyOffer) {
        CurrencySellOffer sellOffer = buyOffer.getCounterOffer();

        CurrencyBuyOffer.remove(buyOffer);
        CurrencySellOffer.remove(sellOffer);

        Account account = Account.getAccount(buyOffer.getAccountId());
        account.addToUnconfirmedBalanceDQT(event, buyOffer.getId(), Math.multiplyExact(buyOffer.getSupply(), buyOffer.getRateDQT()));
        account.addToUnconfirmedCurrencyUnits(event, buyOffer.getId(), buyOffer.getCurrencyId(), sellOffer.getSupply());
    }


    final long id;
    private final long currencyId;
    private final long accountId;
    private final long rateDQT;
    private long limit; // limit on the total sum of units for this offer across transactions
    private long supply; // total units supply for the offer
    private final int expirationHeight;
    private final int creationHeight;
    private final short transactionIndex;
    private final int transactionHeight;

    CurrencyExchangeOffer(long id, long currencyId, long accountId, long rateDQT, long limit, long supply,
                          int expirationHeight, int transactionHeight, short transactionIndex) {
        this.id = id;
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.rateDQT = rateDQT;
        this.limit = limit;
        this.supply = supply;
        this.expirationHeight = expirationHeight;
        this.creationHeight = Dbn.getBlockchain().getHeight();
        this.transactionIndex = transactionIndex;
        this.transactionHeight = transactionHeight;
    }

    CurrencyExchangeOffer(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.rateDQT = rs.getLong("rate");
        this.limit = rs.getLong("unit_limit");
        this.supply = rs.getLong("supply");
        this.expirationHeight = rs.getInt("expiration_height");
        this.creationHeight = rs.getInt("creation_height");
        this.transactionIndex = rs.getShort("transaction_index");
        this.transactionHeight = rs.getInt("transaction_height");
    }

    void save(Connection con, String table) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, transaction_index, transaction_height, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.currencyId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setLong(++i, this.rateDQT);
            pstmt.setLong(++i, this.limit);
            pstmt.setLong(++i, this.supply);
            pstmt.setInt(++i, this.expirationHeight);
            pstmt.setInt(++i, this.creationHeight);
            pstmt.setShort(++i, this.transactionIndex);
            pstmt.setInt(++i, this.transactionHeight);
            pstmt.setInt(++i, Dbn.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getRateDQT() {
        return rateDQT;
    }

    public long getLimit() {
        return limit;
    }

    public long getSupply() {
        return supply;
    }

    public int getExpirationHeight() {
        return expirationHeight;
    }

    public int getHeight() {
        return creationHeight;
    }

    public abstract CurrencyExchangeOffer getCounterOffer();

    long increaseSupply(long delta) {
        long excess = Math.max(Math.addExact(supply, Math.subtractExact(delta, limit)), 0);
        supply += delta - excess;
        return excess;
    }

    void decreaseLimitAndSupply(long delta) {
        limit -= delta;
        supply -= delta;
    }
}

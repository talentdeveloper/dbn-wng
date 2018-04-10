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

import dbn.db.BasicDb;
import dbn.db.TransactionalDb;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "dbn.testDb" : "dbn.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Dbn.getIntProperty("dbn.dbCacheKB"))
            .dbUrl(Dbn.getStringProperty(PREFIX + "Url"))
            .dbType(Dbn.getStringProperty(PREFIX + "Type"))
            .dbDir(Dbn.getStringProperty(PREFIX + "Dir"))
            .dbParams(Dbn.getStringProperty(PREFIX + "Params"))
            .dbUsername(Dbn.getStringProperty(PREFIX + "Username"))
            .dbPassword(Dbn.getStringProperty(PREFIX + "Password", null, true))
            .maxConnections(Dbn.getIntProperty("dbn.maxDbConnections"))
            .loginTimeout(Dbn.getIntProperty("dbn.dbLoginTimeout"))
            .defaultLockTimeout(Dbn.getIntProperty("dbn.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(Dbn.getIntProperty("dbn.dbMaxMemoryRows"))
    );

    static void init() {
        db.init(new DbnDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}

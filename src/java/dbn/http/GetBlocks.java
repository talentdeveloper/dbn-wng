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

import dbn.Block;
import dbn.Dbn;
import dbn.DbnException;
import dbn.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBlocks extends APIServlet.APIRequestHandler {

    static final GetBlocks instance = new GetBlocks();

    private GetBlocks() {
        super(new APITag[] {APITag.BLOCKS}, "firstIndex", "lastIndex", "timestamp", "includeTransactions", "includeExecutedPhased");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws DbnException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final int timestamp = ParameterParser.getTimestamp(req);
        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));
        boolean includeExecutedPhased = "true".equalsIgnoreCase(req.getParameter("includeExecutedPhased"));

        JSONArray blocks = new JSONArray();
        try (DbIterator<? extends Block> iterator = Dbn.getBlockchain().getBlocks(firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getTimestamp() < timestamp) {
                    break;
                }
                blocks.add(JSONData.block(block, includeTransactions, includeExecutedPhased));
            }
        }

        JSONObject response = new JSONObject();
        response.put("blocks", blocks);

        return response;
    }

}

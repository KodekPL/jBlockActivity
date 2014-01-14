package jcraft.jblockactivity.session;

import java.sql.ResultSet;
import java.sql.SQLException;

import jcraft.jblockactivity.actionlogs.BlockActionLog;
import jcraft.jblockactivity.actionlogs.SummedActionLogs;
import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.QueryParams.SummarizationMode;

public class LookupCacheFactory {
    private final QueryParams params;
    private final float spaceFactor;

    public LookupCacheFactory(QueryParams params, float spaceFactor) {
        this.params = params;
        this.spaceFactor = spaceFactor;
    }

    public LookupCache getLookupCache(ResultSet result) throws SQLException {
        if (params.mode == SummarizationMode.NONE) {
            return BlockActionLog.getBlockActionLog(result, params);
        } else {
            return new SummedActionLogs(result, params, spaceFactor);
        }
    }
}

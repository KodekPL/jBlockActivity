package jcraft.jblockactivity.session;

import java.sql.ResultSet;
import java.sql.SQLException;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlogs.BlockActionLog;
import jcraft.jblockactivity.actionlogs.EntityActionLog;
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
            final LoggingType logType = params.needLogType ? LoggingType.getTypeById(result.getInt("type")) : null;
            switch (logType) {
            case blockbreak:
            case blockinteract:
            case blockplace:
            case inventoryaccess:
                return BlockActionLog.getBlockActionLog(result, params);
            case hangingbreak:
            case hangingplace:
            case hanginginteract:
                return EntityActionLog.getEntityActionLog(result, params);
            default:
                return null;
            }
        } else {
            return new SummedActionLogs(result, params, spaceFactor);
        }
    }
}

package jcraft.jblockactivity.session;

import java.sql.ResultSet;
import java.sql.SQLException;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.actionlog.EntityActionLog;
import jcraft.jblockactivity.actionlog.SummedActionLogs;
import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.QueryParams.ParamType;
import jcraft.jblockactivity.utils.QueryParams.SummarizationMode;

public class LookupCacheFactory {

    private final QueryParams params;
    private final float spaceFactor;

    public LookupCacheFactory(QueryParams params, float spaceFactor) {
        this.params = params;
        this.spaceFactor = spaceFactor;
    }

    public LookupCache getLookupCache(ResultSet result) throws SQLException {
        if (params.getSumMode() == SummarizationMode.NONE) {
            final LoggingType logType = params.getBoolean(ParamType.NEED_LOG_TYPE) ? LoggingType.getTypeById(result.getInt("type")) : null;

            switch (logType) {
            case blockbreak:
            case blockinteract:
            case blockplace:
            case inventoryaccess:
            case tramplefarmland:
                return BlockActionLog.getBlockActionLog(result, params);
            case hangingbreak:
            case hangingplace:
            case hanginginteract:
            case creaturekill:
                return EntityActionLog.getEntityActionLog(result, params);
            default:
                return null;
            }
        } else if (params.getSumMode() == SummarizationMode.BLOCKS) {
            return new SummedActionLogs.SummedBlockActionLogs(result, params, spaceFactor);
        } else {
            return new SummedActionLogs.SummedEntityActionLogs(result, params, spaceFactor);
        }
    }

}

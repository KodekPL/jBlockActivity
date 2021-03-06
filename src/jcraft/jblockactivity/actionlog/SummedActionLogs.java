package jcraft.jblockactivity.actionlog;

import java.sql.ResultSet;
import java.sql.SQLException;

import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.utils.ActivityUtil;
import jcraft.jblockactivity.utils.MaterialNames;
import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.QueryParams.SummarizationMode;

import org.bukkit.Location;

public class SummedActionLogs {

    public static class SummedBlockActionLogs implements LookupCache {

        private final String group;
        private final int created, destroyed;
        private final float spaceFactor;

        public SummedBlockActionLogs(ResultSet result, QueryParams params, float spaceFactor) throws SQLException {
            group = (params.getSumMode() == SummarizationMode.PLAYERS) ? result.getString(1) : MaterialNames.materialName(result.getInt(1));
            created = result.getInt(2);
            destroyed = result.getInt(3);
            this.spaceFactor = spaceFactor;
        }

        @Override
        public String getMessage() {
            return created + ActivityUtil.makeSpaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)) + destroyed
                    + ActivityUtil.makeSpaces((int) ((10 - String.valueOf(destroyed).length()) / spaceFactor)) + group;
        }

        @Override
        public Location getLocation() {
            return null;
        }

        @Override
        public ActionLog getActionLog() {
            return null;
        }

    }

    public static class SummedEntityActionLogs implements LookupCache {

        private final String group;
        private final int kills;
        private final float spaceFactor;

        public SummedEntityActionLogs(ResultSet result, QueryParams params, float spaceFactor) throws SQLException {
            final Object object = result.getObject(1);

            group = (object instanceof Integer) ? MaterialNames.entityName((int) object) : (String) object;
            kills = result.getInt(2);
            this.spaceFactor = spaceFactor;
        }

        @Override
        public String getMessage() {
            return kills + ActivityUtil.makeSpaces((int) ((10 - String.valueOf(kills).length()) / spaceFactor)) + group;
        }

        @Override
        public Location getLocation() {
            return null;
        }

        @Override
        public ActionLog getActionLog() {
            return null;
        }

    }

}

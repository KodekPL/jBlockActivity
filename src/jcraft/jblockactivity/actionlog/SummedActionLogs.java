package jcraft.jblockactivity.actionlog;

import static jcraft.jblockactivity.utils.ActivityUtil.makeSpaces;

import java.sql.ResultSet;
import java.sql.SQLException;

import jcraft.jblockactivity.session.LookupCache;
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
            group = (params.mode == SummarizationMode.PLAYERS) ? result.getString(1) : MaterialNames.materialName(result.getInt(1));
            created = result.getInt(2);
            destroyed = result.getInt(3);
            this.spaceFactor = spaceFactor;
        }

        @Override
        public String getMessage() {
            return created + makeSpaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)) + destroyed
                    + makeSpaces((int) ((10 - String.valueOf(destroyed).length()) / spaceFactor)) + group;
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
            group = MaterialNames.entityName(result.getInt(1));
            kills = result.getInt(2);
            this.spaceFactor = spaceFactor;
        }

        @Override
        public String getMessage() {
            return kills + makeSpaces((int) ((10 - String.valueOf(kills).length()) / spaceFactor)) + group;
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

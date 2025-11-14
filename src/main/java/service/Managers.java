package service;

import db.DbManager;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public class Managers {
    public static DbManager getDefaultDatabase() {
        return new DbManager();
    }

    public static JdbcTaskManager getDefault(DbManager db) throws SQLException, IOException {
        return new JdbcTaskManager(db);
    }


}

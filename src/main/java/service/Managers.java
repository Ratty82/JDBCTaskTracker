package service;

import db.DbManager;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class Managers {
    public static DbManager getDefaultDatabase() {
        return new DbManager();
    }

    public static JdbcHistoryManager getDefaultHistory(DbManager db) throws SQLException {
        return new JdbcHistoryManager(db);
    }

    public static JdbcTaskManager getDefault(DbManager db,JdbcHistoryManager hm) throws SQLException, IOException {
        return new JdbcTaskManager(db,hm);
    }


}

package db;

import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.io.*;
import java.nio.file.*;

public class DbManager {

    public static Properties getPropertiesFromFile(Path path) throws IOException {
        try (var in = java.nio.file.Files.newInputStream(path)) {
            Properties p = new Properties();
            p.load(in);
            return p;
        }
    }

    public static Connection getConnection(Properties props) throws SQLException {
        Connection conn = DriverManager.getConnection(props.getProperty("jdbc.url"),props.getProperty("jdbc.user"),props.getProperty("jdbc.password"));
        return conn;
    }

    public static ResultSet executeSql(Connection conn, Path path, Integer paramsQty, List<Object> params) throws SQLException, IOException {
        String sql = Files.readString(path);
        PreparedStatement statement = conn.prepareStatement(sql);
        if (paramsQty != null && paramsQty > 0) {
            for (int i = 0; i < paramsQty; i++) {
                statement.setObject(i + 1, params.get(i));
            }
        }
        return statement.executeQuery();
    }


}

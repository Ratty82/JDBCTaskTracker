package db;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.io.*;
import java.nio.file.*;

public class DbManager {

    public static Properties getPropertiesFromFile(String path) {
        InputStream in = DbManager.class.getClassLoader().getResourceAsStream(path);
        try{
            Properties prop = new Properties();
            prop.load(in);
            return prop;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static Connection getConnection(Properties props) throws SQLException {
        Connection conn = DriverManager.getConnection(props.getProperty("jdbc.url"),props.getProperty("jdbc.user"),props.getProperty("jdbc.password"));
        return conn;
    }


    public static String loadSQL(String path) {
        InputStream in = DbManager.class.getClassLoader().getResourceAsStream(path);
        try{
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return sql;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    //TODO проверить при апдейте
    public static ResultSet executeSql(Connection conn, String path, Integer paramsQty, List<Object> params) throws SQLException, IOException {
        String sql = loadSQL(path);
        PreparedStatement statement = conn.prepareStatement(sql);
        if (paramsQty != null && paramsQty > 0) {
            for (int i = 0; i < paramsQty; i++) {
                statement.setObject(i + 1, params.get(i));
            }
        }
        boolean hasResultSet = statement.execute();
        return hasResultSet ? statement.getResultSet() : null;
    }
}

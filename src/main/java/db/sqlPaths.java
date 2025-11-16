package db;

import java.nio.file.Path;
import java.util.Properties;

public class sqlPaths {
    public static final Path sqlInsert = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\insertTask.sql");
    public static final Path sqlUpdate = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\updateTask.sql");
    public static final Path sqlSelectById = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\selectTaskById.sql");
    public static final Path sqlSelectAll = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\selectAllTasks.sql");
    public static final Path truncateTasks = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\truncateTasks.sql");
    public static final Path deleteSql = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\deleteById.sql");
    public static final Path deleteAllSubtasks = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\deleteAllSubtasks.sql");
    public static final Path selectAllSubtasks = Path.of("D:\\JAVA_Projects\\java_course\\JDBCTaskTracker\\src\\main\\java\\db\\selectAllSubtasks.sql");
    public static final String dbProperties = "db\\db.properties";

}

package service;

import db.DbManager;
import exceptions.TaskNotFoundException;
import model.Task;
import util.TaskStatus;
import util.TaskType;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JdbcHistoryManager implements HistoryManager {
    protected DbManager db;
    protected Properties prop;
    protected Connection conn;

    public JdbcHistoryManager(DbManager db) throws SQLException {
        this.db = db;
        this.prop = db.getPropertiesFromFile("db\\db.properties");
        this.conn = db.getConnection(prop);

    }

    @Override
    public List<Integer> getHistory() {
        List<Integer> taskHistory = null;
        try {
            conn.setAutoCommit(false);
            ResultSet result = db.executeSql(conn, "sql\\selectAllHistory.sql", 0, null);
            while (result.next()) {
                taskHistory.add(result.getInt("task_id"));
            }
            conn.commit();
        } catch (SQLException | IOException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.out.println("Ошибка закрытия транзакции:" + ex.getMessage());
            }
            System.out.println("Ошибка получения списка задач:" + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println("Ошибка изменения режима соединения к базе данных:" + ex.getMessage());
            }
        }
        return taskHistory;
    }

    @Override
    public void addTaskToHistory(Integer taskId) {
        try {
            List<Object> params = List.of(taskId);
            db.executeSql(conn, "sql\\upsertIntoHistory.sql", params.size(), params);
        } catch (SQLException | IOException e) {
            System.out.println("Ошибка вставки в историю:" + e.getMessage());
        }

    }

    public void deleteFromHistoryById(Integer taskId) {
        try {
            List<Object> params = List.of(taskId);
            db.executeSql(conn, "sql\\deleteFromHistoryById.sql", params.size(), params);
        } catch (SQLException | IOException e) {
            System.out.println("Ошибка удаления из истории:" + e.getMessage());
        }
    }

    public void deleteAllSubtasksFromHistory(Integer epicId) {
        try {
            List<Object> params = List.of(epicId);
            db.executeSql(conn, "sql\\deleteSubTasksFromHistory.sql", params.size(), params);
        } catch (SQLException | IOException e) {
            System.out.println("Ошибка удаления из истории:" + e.getMessage());
        }
    }

}

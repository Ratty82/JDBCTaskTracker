package service;

import db.DbManager;
import exceptions.TaskNotFoundException;
import model.Epic;
import model.SubTask;
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

public class JdbcTaskManager implements TaskManager, AutoCloseable {
    protected DbManager db;
    protected Properties prop;
    protected Connection conn;


    public JdbcTaskManager(DbManager db) throws SQLException {
        this.db = db;
        this.prop = db.getPropertiesFromFile("db\\db.properties");
        this.conn = db.getConnection(prop);
    }

    public static Task create(Integer id,String name,String details,TaskStatus status,TaskType type,Integer epicId) {
        return switch (type) {
            case TASK    -> new Task(id, name, details, status, type);
            case EPIC    -> new Epic(id, name, details, status, type);
            case SUBTASK -> new SubTask(id,name,details,status,type,epicId);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    @Override
    public List<Task> getAllTasks() {
        List<Task> taskList = null;
        try {
            ResultSet result = db.executeSql(conn, "sql\\selectAllTasks.sql", 0, null);
            taskList = new ArrayList<>();
            while (result.next()) {
                taskList.add(create(result.getInt("id"),
                                                result.getString("name"),
                                                result.getString("details"),
                                                TaskStatus.valueOf(result.getString("status")),
                                                TaskType.valueOf(result.getString("type")),
                                                result.getInt("epic_id")));
            }
        } catch (SQLException | IOException e) {
            System.out.println("Ошибка получения списка задач:" + e.getMessage());
        }
        return taskList;
    }

    @Override
    public void removeAllTasks() {
        try {
            db.executeSql(conn, "sql\\truncateTasks.sql", 0, null);
        } catch (SQLException | IOException e) {
            System.out.println("Ошибка удаления всех задач:" + e.getMessage());
        }
    }

    @Override
    public <T extends Task> T findTaskByID(Integer taskId, Class<T> type) throws TaskNotFoundException, IllegalArgumentException {
        if (taskId == null || taskId < 0) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        }
        Task out = null;
        try {
            List<Object> params = List.of(taskId);
            ResultSet result = db.executeSql(conn, "sql\\selectTaskById.sql", params.size(), params);
            if (!result.next()) {
                throw new TaskNotFoundException(taskId);
            }
            out = create(
                    result.getInt("id"),
                    result.getString("name"),
                    result.getString("details"),
                    TaskStatus.valueOf(result.getString("status")),
                    TaskType.valueOf(result.getString("type")),
                    result.getInt("epic_id")
            );
        } catch (IOException | SQLException e) {
            System.out.println("Ошибка поиска задачи по Id:" + e.getMessage());
        }
        return type.cast(out);
    }

    @Override
    public void createTask(Task task) throws IllegalArgumentException {
        if (task == null || task.getTaskType() == TaskType.SUBTASK) {
            throw new IllegalArgumentException(("Задача не должен быть null или подзадачей"));
        }
        try {
            List<Object> params = List.of(
                    task.getTaskId(),
                    task.getTaskName(),
                    task.getTaskDetails(),
                    task.getTaskStatus().toString(),
                    task.getTaskType().toString(),
                    0
            );
            db.executeSql(conn, "sql\\insertTask.sql", params.size(), params);
        } catch (SQLException | IOException e) {
            System.out.println("Ошибка создания задачи:" + e.getMessage());
        }
    }

    @Override
    public <T extends Task> T updateTask(T newTask, Class<T> type) throws IllegalArgumentException, TaskNotFoundException {
        if (newTask == null) {
            throw new IllegalArgumentException("Задача не должна быть null");
        }
        Task toUpdate = findTaskByID(newTask.getTaskId(), type);
        if (toUpdate == null) {
            throw new TaskNotFoundException(newTask.getTaskId());
        }

               List<Object> taskParams = new ArrayList<>(
                List.of(
                        newTask.getTaskName(),
                        newTask.getTaskDetails(),
                        newTask.getTaskStatus().toString(),
                        newTask.getTaskType().toString(),
                        (newTask instanceof SubTask subTask) ? subTask.getTaskParentId() : 0,
                        newTask.getTaskId()
        ));

                try {
            db.executeSql(conn, "sql\\updateTask.sql", taskParams.size(), taskParams);
        } catch (SQLException | IOException e) {
            System.out.println("Ошибка обновления задачи':" + e.getMessage());
        }

        Epic linkedEpic;
        if (newTask instanceof SubTask subTask) {
            linkedEpic = findTaskByID(subTask.getTaskParentId(),Epic.class);
            System.out.println("Linked epic:" + linkedEpic.toString());
        } else {
            linkedEpic = null;
        }

        List<Object> epicParams = (linkedEpic != null) ? new ArrayList<>(
                List.of(
                        linkedEpic.getTaskName(),
                        linkedEpic.getTaskDetails(),
                        setEpicStatus(linkedEpic).getTaskStatus().toString(),
                        linkedEpic.getTaskType().toString(),
                        0,
                        linkedEpic.getTaskId()
                )) : null;

        if (linkedEpic != null) {
            try {
            db.executeSql(conn, "sql\\updateTask.sql", epicParams.size(), taskParams);
            } catch (SQLException | IOException e) {
            System.out.println("Ошибка обновления связанного эпика':" + e.getMessage());
            }
        }
        return findTaskByID(newTask.getTaskId(), type);
    }

    @Override
    public void removeTaskById(Integer taskId) throws IllegalArgumentException, TaskNotFoundException {
        if (taskId == null || taskId < 0) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        }
        try {
           List<Object> params = List.of(taskId);
           ResultSet result = db.executeSql(conn, "sql\\selectTaskById.sql", params.size(), params);
           if (!result.next()) {
               throw new TaskNotFoundException(taskId);
           }
           TaskType ttype = TaskType.valueOf(result.getString("type"));
           Integer epicId = result.getInt("epic_id");
           if (ttype == TaskType.TASK || ttype == TaskType.SUBTASK) {
                db.executeSql(conn, "sql\\deleteById.sql", 1, params);
           } else {
                db.executeSql(conn, "sql\\deleteById.sql", 1, params);
                db.executeSql(conn, "sql\\deleteAllSubtasks.sql", 1, List.of(epicId));
           }
        } catch (SQLException | IOException e) {
                System.out.println("Deleting task Exception:" + e.getMessage());
        }
    }

    @Override
    public List<SubTask> getAllSubTasks(Epic epic) {
        List subTaskList = new ArrayList<>();
        if (epic == null) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        }
        try {
            List<Object> params = List.of(epic.getTaskId());
            ResultSet result = db.executeSql(conn, "sql\\selectAllSubtasks.sql", params.size(), params);
            if (!result.next()) {
                throw new TaskNotFoundException(epic.getTaskId());
            }
            do {
                subTaskList.add( new SubTask(
                        result.getInt("id"),
                        result.getString("name"),
                        result.getString("details"),
                        TaskStatus.valueOf(result.getString("status")),
                        TaskType.valueOf(result.getString("type")),
                        result.getInt("epic_id"))
                );
            }
            while (result.next());


            } catch (SQLException | IOException | TaskNotFoundException e) {
                System.out.println("Selecting subtasks Exception:" + e.getMessage());
            }
        return subTaskList;
        }


    @Override
    public Epic setEpicStatus(Epic epic) {
        System.out.println("Subtasks" + getAllSubTasks(epic).toString());
        if ( getAllSubTasks(epic).isEmpty()  || getAllSubTasks(epic).stream().allMatch(t -> t.getTaskStatus() == TaskStatus.NEW)) {
            return new Epic(epic.getTaskId(), epic.getTaskName(), epic.getTaskDetails(), TaskStatus.NEW, TaskType.EPIC, epic.getAllSubtaskIds());
        } else if (getAllSubTasks(epic).stream().allMatch(t -> t.getTaskStatus() == TaskStatus.DONE)) {
            return new Epic(epic.getTaskId(), epic.getTaskName(), epic.getTaskDetails(), TaskStatus.DONE, TaskType.EPIC, epic.getAllSubtaskIds());
        } else {
            return new Epic(epic.getTaskId(), epic.getTaskName(), epic.getTaskDetails(), TaskStatus.IN_PROGRESS, TaskType.EPIC, epic.getAllSubtaskIds());
        }
    }

    @Override
    public void includeTaskToEpic(Task task, Epic epic) throws IllegalArgumentException {
        if (epic == null || task == null) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        }
        try {
            Task toInclude = findTaskByID(task.getTaskId(), Task.class);
            Epic parentTask = findTaskByID(epic.getTaskId(), Epic.class);
            List<Object> params = List.of(parentTask.getTaskId(), TaskType.SUBTASK.toString(), toInclude.getTaskId());
            db.executeSql(conn, "sql\\includeTaskToEpic.sql", params.size(), params);
        } catch (SQLException | IOException | TaskNotFoundException e) {
            System.out.println("Ошибка добавления задачи в эпик:" + e.getMessage());
        }
    }

    @Override
    public TaskType getTaskType(Integer taskId) throws TaskNotFoundException {
        TaskType taskType = null;
        if (taskId == null || taskId < 0) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        }
        try {
            List<Object> params = List.of(taskId);
            ResultSet result = db.executeSql(conn, "sql\\selectTaskById.sql", params.size(), params);
            if (!result.next()) {
                throw new TaskNotFoundException(taskId);
            }
            taskType =  TaskType.valueOf(result.getString("type"));
        } catch (SQLException | IOException | TaskNotFoundException e) {
                System.out.println("Selecting subtasks Exception:" + e.getMessage());
        }
        return taskType;
    }

    @Override
    public void close() throws Exception {

    }
}

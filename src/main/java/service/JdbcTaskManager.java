package service;

import db.DbManager;
import db.sqlPaths;
import exceptions.TaskAlreadyExistException;
import exceptions.TaskNotFoundException;
import model.Epic;
import model.SubTask;
import model.Task;
import util.TaskStatus;
import util.TaskType;
import util.TaskFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

//throws SQLException, IOException

public class JdbcTaskManager implements TaskManager{
    protected DbManager db;
    protected Properties prop;
    protected Connection conn;
    protected TaskType ttype;
    protected Integer epicId;
    protected ArrayList<SubTask> subTaskList;

    public JdbcTaskManager(DbManager db) throws IOException, SQLException {
        this.db = db;
        this.prop = db.getPropertiesFromFile(sqlPaths.dbProperties);
        this.conn = db.getConnection(prop);
    }

    @Override
    public List<Task> getAllTasks() {
        List<Task> taskList = null;
        try {
            ResultSet result = db.executeSql(conn, sqlPaths.sqlSelectAll, 0, null);
            taskList = new ArrayList<>();
            while (result.next()) {
                int id = result.getInt("id");
                String name = result.getString("name");
                String details = result.getString("details");
                TaskStatus status = TaskStatus.valueOf(result.getString("status"));
                TaskType type = TaskType.valueOf(result.getString("type"));
                Integer parentId = result.getInt("epic_id");
                taskList.add(TaskFactory.create(id, name, details, status, type, parentId));
            }
        } catch (SQLException | IOException e) {
            System.out.println("Getting tasks Exception:" + e.getMessage());
        }
        return taskList;
    }

    @Override
    public void removeAllTasks() {
        try {
            db.executeSql(conn, sqlPaths.truncateTasks, 0, null);
        } catch (SQLException | IOException e) {
            System.out.println("Deleting tasks Exception:" + e.getMessage());
        }
    }


    @Override
    public <T extends Task> T findTaskByID(Integer taskId, Class<T> type ) throws TaskNotFoundException, IllegalArgumentException {
        Task out = null;
        if (taskId == null || taskId < 0) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        } else {
            try {
                List<Object> params = List.of(taskId);
                ResultSet result = db.executeSql(conn, sqlPaths.sqlSelectById, 1,params );
                if (!result.next()) {
                    throw new TaskNotFoundException(taskId);
                } else {
                    while (result.next()) {
                        int id = result.getInt("id");
                        String name = result.getString("name");
                        String details = result.getString("details");
                        TaskStatus status = TaskStatus.valueOf(result.getString("status"));
                        TaskType taskType = TaskType.valueOf(result.getString("type"));
                        Integer parentId = result.getInt("epic_id");
                        out = TaskFactory.create(id, name, details, status, taskType, parentId);
                    }
                }
            } catch (IOException e) {
                System.out.println("Finding tasks IOException:" + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Finding tasks SQLException:" + e.getMessage());
            }
        }
        if (out == null) {
            throw new TaskNotFoundException(taskId);
        }
        else {
            return type.cast(out);
        }
    }

    @Override
    public void createTask(Task task) throws IllegalArgumentException {
        if (task == null || task.getTaskType() == TaskType.SUBTASK ) {
            throw new IllegalArgumentException(("Задача не должен быть null или подзадачей"));
        } else {
            try {
                List<Object> params = List.of(task.getTaskId(), task.getTaskName(), task.getTaskDetails(), task.getTaskStatus().toString(), task.getTaskType().toString(),0);
                db.executeSql(conn, sqlPaths.sqlInsert, 6,params );
            } catch (SQLException | IOException e) {
                System.out.println("Creating task Exception:" + e.getMessage());
            }
        }
    }

    @Override
    public <T extends Task> T updateTask(T task, Class<T> type) throws IllegalArgumentException, TaskNotFoundException {
        if (task == null) {
            throw new IllegalArgumentException("Задача не должна быть null");
        } else {
            Task old = findTaskByID(task.getTaskId(), type);
            if (old == null) {
                throw new TaskNotFoundException(task.getTaskId());
            } else {
                if (task instanceof Epic epic) {
                    T updated = null;
                    try {
                        List<Object> params = List.of(epic.getTaskId(), epic.getTaskName(), epic.getTaskDetails(), epic.getTaskStatus().toString(), epic.getTaskType().toString(),0);
                        db.executeSql(conn, sqlPaths.sqlUpdate, 6,params );
                        updated = findTaskByID(epic.getTaskId(),type);
                    } catch (SQLException | IOException e) {
                        System.out.println("Updating task Exception:" + e.getMessage());
                    }
                    return updated;
                } else if (task instanceof SubTask subTask) {
                    T updated = null;
                    try {
                        List<Object> params = List.of(subTask.getTaskId(), subTask.getTaskName(), subTask.getTaskDetails(), subTask.getTaskStatus().toString(), subTask.getTaskType().toString(),subTask.getTaskParentId());
                        db.executeSql(conn, sqlPaths.sqlUpdate, 6,params);
                        updated = findTaskByID(subTask.getTaskId(),type);
                    } catch (SQLException | IOException e) {
                        System.out.println("Updating task Exception:" + e.getMessage());
                    }
                    return updated;
                } else {
                    T updated = null;
                    try {
                        List<Object> params = List.of(task.getTaskId(), task.getTaskName(), task.getTaskDetails(), task.getTaskStatus().toString(), task.getTaskType().toString(),0);
                        db.executeSql(conn, sqlPaths.sqlUpdate, 6,params );
                        updated = findTaskByID(task.getTaskId(),type);
                    } catch (SQLException | IOException e) {
                        System.out.println("Updating task Exception:" + e.getMessage());
                    }
                    return updated;
                }
            }
        }
    }

    @Override
    public void removeTaskById(Integer taskId) throws IllegalArgumentException, TaskNotFoundException {
        if (taskId == null || taskId < 0) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        } else {
            try {
                List<Object> params = List.of(taskId);
                ResultSet result = db.executeSql(conn, sqlPaths.sqlSelectById, 1,params );
                if (!result.next()) {
                    throw new TaskNotFoundException(taskId);
                } else {
                    while (result.next()) {
                       ttype = TaskType.valueOf(result.getString("type"));
                       epicId = result.getInt("epic_id");
                    }
                    if (ttype == TaskType.TASK || ttype == TaskType.SUBTASK) {
                        db.executeSql(conn, sqlPaths.deleteSql, 1,params );
                    } else {
                        db.executeSql(conn, sqlPaths.deleteSql, 1,params );
                        db.executeSql(conn, sqlPaths.deleteAllSubtasks, 1,List.of(epicId) );
                    }
                }
            } catch (SQLException | IOException e) {
                System.out.println("Deleting task Exception:" + e.getMessage());
            }
        }
    }


    @Override
    public List<SubTask> getAllSubTasks(Epic epic) {
        subTaskList = new ArrayList<>();
        if (epic == null) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        } else {
            try {
                List<Object> params = List.of(epic.getTaskId());
                ResultSet result = db.executeSql(conn, sqlPaths.sqlSelectById, 1, params);
                if (!result.next()) {
                    throw new TaskNotFoundException(epic.getTaskId());
                } else {
                    while (result.next()) {
                        int id = result.getInt("id");
                        String name = result.getString("name");
                        String details = result.getString("details");
                        TaskStatus status = TaskStatus.valueOf(result.getString("status"));
                        TaskType type = TaskType.valueOf(result.getString("type"));
                        Integer parentId = result.getInt("epic_id");
                        subTaskList.add((SubTask) TaskFactory.create(id, name, details, status, type, parentId));
                    }
                }
            } catch (SQLException | IOException | TaskNotFoundException e) {
                System.out.println("Selecting subtasks Exception:" + e.getMessage());
            }
        }
        return subTaskList;
    }

    @Override
    public Epic setEpicStatus(Epic epic){
        if (epic.getAllSubtaskIds().isEmpty() || getAllSubTasks(epic).stream().allMatch(t -> t.getTaskStatus() == TaskStatus.NEW)) {
            return new Epic(epic.getTaskId(),epic.getTaskName(),epic.getTaskDetails(),TaskStatus.NEW, TaskType.EPIC,epic.getAllSubtaskIds());
        }
        if (getAllSubTasks(epic).stream().allMatch(t -> t.getTaskStatus() == TaskStatus.DONE)) {
            return new Epic(epic.getTaskId(),epic.getTaskName(),epic.getTaskDetails(),TaskStatus.DONE,TaskType.EPIC,epic.getAllSubtaskIds());
        }
        else {
            return new Epic(epic.getTaskId(),epic.getTaskName(),epic.getTaskDetails(),TaskStatus.IN_PROGRESS,TaskType.EPIC,epic.getAllSubtaskIds());
        }
    }

    @Override
    public void includeTaskToEpic(Task task, Epic epic) throws IllegalArgumentException, TaskAlreadyExistException {
        if (epic == null || task == null) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        } else {
            try {
                Task toInclude = findTaskByID(task.getTaskId(), Task.class);
            } catch (TaskNotFoundException e) {
                System.out.println("Finding task Exception:" + e.getMessage());
            }
            try {
                Epic parentTask = findTaskByID(task.getTaskId(), Epic.class);
            } catch (TaskNotFoundException e) {
                System.out.println("Finding epic Exception:" + e.getMessage());
            }
            try {
                List<Object> params = List.of(epic.getTaskId(), TaskType.SUBTASK, task.getTaskId());
                db.executeSql(conn, sqlPaths.sqlSelectById, 3, params);
            } catch (SQLException | IOException e) {
                System.out.println("Selecting subtasks Exception:" + e.getMessage());
            }
        }
    }

    @Override
    public TaskType getTaskType(Integer taskId) throws TaskNotFoundException {
        if (taskId == null || taskId < 0) {
            throw new IllegalArgumentException("ID не должен быть null или отрицательным");
        } else {
            try {
                List<Object> params = List.of(taskId);
                ResultSet result = db.executeSql(conn, sqlPaths.sqlSelectById, 1, params);
                if (!result.next()) {
                    throw new TaskNotFoundException(taskId);
                } else {
                    while (result.next()) {
                        ttype = TaskType.valueOf(result.getString("type"));
                    }
                }
            } catch (SQLException | IOException | TaskNotFoundException e) {
                System.out.println("Selecting subtasks Exception:" + e.getMessage());
            }
        }
        return ttype;
    }
}

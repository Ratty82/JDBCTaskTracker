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

public class JdbcTaskManager implements TaskManager{
    protected DbManager tasks = new DbManager();
    protected Properties prop = tasks.getPropertiesFromFile(sqlPaths.dbProperties);
    Connection conn = tasks.getConnection(prop);

    public JdbcTaskManager() throws IOException, SQLException {
    }

    @Override
    public List<Task> getAllTasks() {
        List<Task> taskList = null;
        try {
            ResultSet result = tasks.executeSql(conn, sqlPaths.sqlSelectAll, 0, null);
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
            tasks.executeSql(conn, sqlPaths.truncateTasks, 0, null);
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
                ResultSet result = tasks.executeSql(conn, sqlPaths.sqlSelectById, 1,params );
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
                tasks.executeSql(conn, sqlPaths.sqlInsert, 6,params );
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
                    T updated = type.cast(setEpicStatus(epic));
                    try {
                        List<Object> params = List.of(epic.getTaskId(), epic.getTaskName(), epic.getTaskDetails(), epic.getTaskStatus().toString(), epic.getTaskType().toString(),0);
                        tasks.executeSql(conn, sqlPaths.sqlUpdate, 6,params );
                    } catch (SQLException | IOException e) {
                        System.out.println("Updating task Exception:" + e.getMessage());
                    }
                    return updated;
                } else if (task instanceof SubTask subTask) {
                    T updated = type.cast(subTask);
                    try {
                        List<Object> params = List.of(subTask.getTaskId(), subTask.getTaskName(), subTask.getTaskDetails(), subTask.getTaskStatus().toString(), subTask.getTaskType().toString(),0);
                        tasks.executeSql(conn, sqlPaths.sqlUpdate, 6,params );
                        Integer parentId = subTask.getTaskParentId();
                        Epic epicToUpdate = findTaskByID(parentId, Epic.class);
                        List<Object> epicParams = List.of(epicToUpdate.getTaskId(), epicToUpdate.getTaskName(), epicToUpdate.getTaskDetails(), epicToUpdate.getTaskStatus().toString(), epicToUpdate.getTaskType().toString(),subTask.getTaskParentId());
                        tasks.executeSql(conn, sqlPaths.sqlUpdate, 6,epicParams );
                    } catch (SQLException | IOException e) {
                        System.out.println("Updating task Exception:" + e.getMessage());
                    }
                    return updated;
                } else {
                    T updated = type.cast(task);
                    try {
                        List<Object> params = List.of(task.getTaskId(), task.getTaskName(), task.getTaskDetails(), task.getTaskStatus().toString(), task.getTaskType().toString(),0);
                        tasks.executeSql(conn, sqlPaths.sqlUpdate, 6,params );
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
                ResultSet result = tasks.executeSql(conn, sqlPaths.sqlSelectById, 1,params );
                if (!result.next()) {
                    throw new TaskNotFoundException(taskId);
                } else {
                    tasks.executeSql(conn, sqlPaths.deleteSql, 1,params );
                }
            } catch (SQLException | IOException e) {
                System.out.println("Deleting task Exception:" + e.getMessage());
                }
        }
    }


    @Override
    public List<SubTask> getAllSubTasks(Epic epic) {
        return List.of();
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

    }

    @Override
    public TaskType getTaskType(Integer taskId) throws TaskNotFoundException {
        return null;
    }
}

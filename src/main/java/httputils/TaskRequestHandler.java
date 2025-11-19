package httputils;

import com.sun.net.httpserver.HttpExchange;
import db.DbManager;
import dto.TaskDto;
import exceptions.TaskNotFoundException;
import model.Epic;
import model.SubTask;
import model.Task;

import service.HistoryManager;
import service.JdbcHistoryManager;
import service.JdbcTaskManager;
import service.Managers;
import util.DtoVars;
import util.GenId;
import util.TaskType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class TaskRequestHandler  {
    static DbManager dbman = Managers.getDefaultDatabase();
    static JdbcHistoryManager hm;
    static JdbcTaskManager ftm;

    static {
        try {
            hm =  Managers.getDefaultHistory(dbman);
            ftm = Managers.getDefault(dbman,hm);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getTaskHandler(HttpExchange he) throws IOException {
        //отсылаю запросы http://localhost:8080/tasks?id=1 - получить задачу по Id; http://localhost:8080/tasks - получить все задачи
        String query = he.getRequestURI().getQuery();
        if ( query == null ) {
            System.out.println("Request: " + he.getRequestURI().getPath());
            try {
                Responses.sendJson(he, 200,ftm.getAllTasks());
            } catch (Exception e) {
                Responses.sendError(he, 404, "Tasks not found");
            }
        } else {
            System.out.println("Requested task: " + query);
            try {
                Integer taskId = Integer.parseInt(query.split("=")[1]);
                TaskType taskType = ftm.getTaskType(taskId);
                if (taskType == TaskType.TASK) {
                    Responses.sendJson(he, 200,ftm.findTaskByID(taskId, Task.class));
                } else if (taskType == TaskType.EPIC) {
                    Responses.sendJson(he, 200,ftm.findTaskByID(taskId, Epic.class));
                } else if (taskType == TaskType.SUBTASK) {
                    Responses.sendJson(he, 200, ftm.findTaskByID(taskId, SubTask.class));
                }
            } catch (IOException|TaskNotFoundException e) {
                Responses.sendError(he, 404, "Task not found" );
            }
        }
    }

    public static void postTaskHandler(HttpExchange he) throws IOException {
        // отсылаю body вида { "taskName": "Тестовый task 1", "taskDetails": "Проверка метода createTask", "taskType": "TASK" }
        String body = new String(he.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        TaskDto dto = Responses.gson.fromJson(body, TaskDto.class);
        Integer newId = GenId.setTaskId();
        switch (dto.getTaskType()) {
            case TASK -> {
                try {
                    ftm.createTask(new Task(newId, dto.getTaskName(), dto.getTaskDetails(), dto.getTaskStatus(), TaskType.TASK));
                    Responses.sendJson(he, 201, ftm.findTaskByID(newId, Task.class));
                } catch (Exception e) {
                    Responses.sendError(he, 404, "Problem creating task :" + e.getMessage());
                }
            }
            case EPIC -> {
                try {
                    ftm.createTask(new Epic(newId, dto.getTaskName(), dto.getTaskDetails(), dto.getTaskStatus(), TaskType.EPIC));
                    Responses.sendJson(he, 201, ftm.findTaskByID(newId, Epic.class));
                } catch (Exception e) {
                    Responses.sendError(he, 404, "Problem creating epic :" + e.getMessage());
                }
            }
            case SUBTASK -> {
                Responses.sendError(he, 400, "Subtask is not allowed");
            }
        }
    }

    public static void putTaskHandler(HttpExchange he) throws IOException {
        // для обновления отсылаю body вида { "taskId": 1, "taskName" "Тестовый task 1", "taskDetails": "Проверка метода createTask" , "taskStatus": "NEW"}
        // для включения в эпик отсылаю query вида /tasks?id=1&id=4
        String body = new String(he.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String query = he.getRequestURI().getQuery();
        System.out.println(query);
        if ( query == null ) {
            try {
                TaskDto dto = Responses.gson.fromJson(body, TaskDto.class);
                switch (DtoVars.getVariant(dto.getTaskType(),ftm.getTaskType(dto.getTaskId()))) {
                    case TASK -> {
                        Task taskToUpdate = ftm.findTaskByID(dto.getTaskId(), Task.class);
                        Task updated = ftm.updateTask(new Task(taskToUpdate.getTaskId(),
                                        DtoVars.getVariant(dto.getTaskName(),taskToUpdate.getTaskName()),
                                        DtoVars.getVariant(dto.getTaskDetails(),taskToUpdate.getTaskDetails()),
                                        DtoVars.getVariant(dto.getTaskStatus(),taskToUpdate.getTaskStatus()),
                                        TaskType.TASK),
                                Task.class);
                        Responses.sendJson(he, 202, updated);
                    }
                    case EPIC -> {
                        Epic epicToUpdate = ftm.findTaskByID(dto.getTaskId(), Epic.class);
                        Epic updated = ftm.updateTask(new Epic(epicToUpdate.getTaskId(),
                                        DtoVars.getVariant(dto.getTaskName(),epicToUpdate.getTaskName()),
                                        DtoVars.getVariant(dto.getTaskDetails(),epicToUpdate.getTaskDetails()),
                                        epicToUpdate.getTaskStatus(),
                                        TaskType.EPIC,
                                        epicToUpdate.getAllSubtaskIds()),
                                Epic.class);
                        Responses.sendJson(he, 202, updated);
                    }
                    case SUBTASK -> {
                        SubTask subTaskToUpdate = ftm.findTaskByID(dto.getTaskId(), SubTask.class);
                        SubTask updated = ftm.updateTask(new SubTask(subTaskToUpdate.getTaskId(),
                                        DtoVars.getVariant(dto.getTaskName(),subTaskToUpdate.getTaskName()),
                                        DtoVars.getVariant(dto.getTaskDetails(),subTaskToUpdate.getTaskDetails()),
                                        DtoVars.getVariant(dto.getTaskStatus(),subTaskToUpdate.getTaskStatus()),
                                        TaskType.SUBTASK ,
                                        subTaskToUpdate.getTaskParentId()),
                                SubTask.class);
                        Responses.sendJson(he, 202, updated);
                    }
                }
            } catch (Exception e) {
                Responses.sendError(he, 404, "Problem updating task :" + e.getMessage());
            }
        } else {
            try {
                List<Integer> idsEpicTask = Arrays.stream(query.split("&"))
                        .filter(s -> s.startsWith("id="))
                        .map(s -> s.substring(3))
                        .map(Integer::parseInt)
                        .toList();
                Task taskToInclude = ftm.findTaskByID(idsEpicTask.get(0), Task.class);
                Epic epicToIncludeIn = ftm.findTaskByID(idsEpicTask.get(1), Epic.class);
                ftm.includeTaskToEpic(taskToInclude,epicToIncludeIn);
                List<Task> result = List.of(ftm.findTaskByID(idsEpicTask.get(0), SubTask.class),ftm.findTaskByID(idsEpicTask.get(1), Epic.class));
                Responses.sendJson(he, 202, result);
            } catch (Exception e) {
                Responses.sendError(he, 404, "Problem including task to epic :" + e.getMessage());
            }
        }
    }

    public static void deleteTaskHandler(HttpExchange he) throws IOException {
        // для удаления задач отсылаю query вида /tasks?id=1
        String query = he.getRequestURI().getQuery();
        if ( query != null ) {
            try {
                Integer taskId = Integer.parseInt(query.split("=")[1]);
                ftm.removeTaskById(taskId);
                Responses.sendJson(he, 204, null );
            } catch (Exception e) {
                Responses.sendError(he, 404, "Problem with deletion :" + e.getMessage());
            }
        } else {
            Responses.sendError(he, 400, "Request query is null - nothing to delete" );
        }
    }



}

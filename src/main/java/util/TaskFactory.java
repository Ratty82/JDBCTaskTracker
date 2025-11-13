package util;

import model.Epic;
import model.SubTask;
import model.Task;


public final class TaskFactory {
    private TaskFactory() {}

    public static Task create(Integer id,String name,String details,TaskStatus status,TaskType type,Integer epicId) {
        return switch (type) {
            case TASK    -> new Task(id, name, details, status, type);
            case EPIC    -> new Epic(id, name, details, status, type);
            case SUBTASK -> new SubTask(id,name,details,status,type,epicId);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }



}

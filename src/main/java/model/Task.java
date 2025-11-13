package model;
import util.TaskStatus;
import util.TaskType;
import util.GenId;

public class Task {
    private Integer taskId;
    private String taskName;
    private String taskDetails;
    private TaskStatus taskStatus;
    private TaskType taskType;

    public Task(Integer taskId, String taskName, String taskDetails, TaskStatus taskStatus, TaskType taskType) {
        this.taskId = (taskId == null) ? GenId.setTaskId() : taskId;
        this.taskName = validateText(taskName, "Название не может быть пустым или null");
        this.taskDetails = validateText(taskDetails, "Описание не может быть пустым или null");
        this.taskStatus = checkForNulls(taskStatus, "Статус не может быть null");
        this.taskType = checkForNulls(taskType, "Тип не может быть null");
    }

    private static String validateText(String string, String message) {
        if (string == null || string.trim().isEmpty()) throw new IllegalArgumentException(message);
        return string;
    }

    private static <T> T checkForNulls(T v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }


    public Integer getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskDetails() {
        return taskDetails;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public String getTaskStatusString() {
        return taskStatus.getDesc();
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public String getTaskTypeString() {
        return taskType.getDesc();
    }

    @Override
    public String toString() {
        return "[ ID = " + getTaskId() + ", Название = " + getTaskName() + ", Описание = " + getTaskDetails() + ", Статус = " + getTaskStatusString() + ", Тип = " + getTaskTypeString() +"]";
    }

    public String toCSV(){
        return getTaskId() + "," + getTaskName() + "," + getTaskDetails() + "," + getTaskStatusString() + "," + getTaskTypeString();
    }






}


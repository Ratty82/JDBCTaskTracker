package dto;

import util.TaskStatus;
import util.TaskType;

public class TaskDto {
    private Integer taskId;
    private String taskName;
    private String taskDetails;
    private TaskStatus taskStatus;
    private TaskType taskType;

    public TaskDto(Integer taskId, String taskName, String taskDetails, TaskStatus taskStatus, TaskType taskType) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskDetails = taskDetails;
        this.taskStatus = taskStatus;
        this.taskType = taskType;
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

    public TaskType getTaskType() {
        return taskType;
    }
}

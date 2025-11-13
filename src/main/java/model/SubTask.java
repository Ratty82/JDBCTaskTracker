package model;

import util.TaskStatus;
import util.TaskType;

public class SubTask extends Task {
    private Integer parentTaskId;

    public SubTask(Integer taskId, String taskName, String taskDetails, TaskStatus taskStatus, TaskType taskType, Integer parentTaskId) {
        super(taskId, taskName, taskDetails, taskStatus,taskType);
        this.parentTaskId =  validateInteger(parentTaskId,"ID родительской задачи не может быть null");
    }

    private static Integer validateInteger(Integer intv, String message) {
        if (intv == null || intv < 0) throw new IllegalArgumentException(message);
        return intv;
    }

    public Integer getTaskParentId(){
        return parentTaskId;
    }

    @Override
    public String toString() {
        return super.toString() + "[Род.задача: " + getTaskParentId() +  "]";
    }

    @Override
    public String toCSV() {return super.toCSV() + "," + getTaskParentId();}

    @Override
    public TaskType getTaskType() {
        return super.getTaskType();
    }



}

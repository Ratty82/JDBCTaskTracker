package model;
import util.TaskStatus;
import util.TaskType;

import java.util.*;

public class Epic extends Task {

    private HashSet<Integer> subTasks;

    public Epic(Integer taskId, String taskName, String taskDetails, TaskStatus taskStatus, TaskType taskType) {
        super(taskId,taskName, taskDetails, taskStatus,taskType);
        this.subTasks = new HashSet<>();
    }

    public Epic(Integer taskId, String taskName, String taskDetails, TaskStatus taskStatus, TaskType taskType, HashSet<Integer> subTasks) {
        super(taskId,taskName, taskDetails, taskStatus,taskType);
        this.subTasks = subTasks;
    }

    //добавить ID задачи в подзадачи
    public void addSubtask(Integer subTaskId){
        if (subTaskId == null || subTaskId < 0) {throw new IllegalArgumentException("ID не должен быть Null или отрицательным");}
        else {
            subTasks.add(subTaskId);
        }
    }

    //Проверить есть ли ID в списке подзадач
    public boolean checkSubTaskById(Integer subTaskId){
        return subTasks.stream()
                .anyMatch(n -> n.equals(subTaskId));
    }

    //Достать все Id подзадач
    public HashSet<Integer> getAllSubtaskIds(){
        return subTasks;
    }

    @Override
    public String toString() {
        return super.toString() + "[Подзадачи: " + getAllSubtaskIds() +  "]";
    }

    @Override
    public String toCSV() {return super.toCSV();}

    @Override
    public TaskType getTaskType() {
        return super.getTaskType();
    }




}

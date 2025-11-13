package service;

import exceptions.TaskAlreadyExistException;
import exceptions.TaskNotFoundException;
import model.Epic;
import model.SubTask;
import model.Task;
import util.TaskStatus;
import util.TaskType;

import java.util.List;


public interface TaskManager {

    //a. Получение списка всех задач.
    List<Task> getAllTasks();


    //b. Удаление всех задач.
    void removeAllTasks();

    //c. Получение по идентификатору.
    <T extends Task> T findTaskByID(Integer taskId ,Class<T> type ) throws TaskNotFoundException, IllegalArgumentException;


    //d. Создание. Сам объект должен передаваться в качестве параметра.
    void createTask(Task task) throws IllegalArgumentException, TaskAlreadyExistException;

    //- e. Обновление. Новая версия объекта с верным идентификатором передаётся в виде параметра.
    <T extends Task> T updateTask(T task, Class<T> type) throws IllegalArgumentException, TaskNotFoundException;

    //f. Удаление по идентификатору.
    void removeTaskById(Integer taskId) throws IllegalArgumentException, TaskNotFoundException;

    //- a. Получение списка всех подзадач определённого эпика.
    List<SubTask> getAllSubTasks(Epic epic);

    /*- b. Для эпиков:
    - если у эпика нет подзадач или все они имеют статус NEW, то статус должен быть NEW.
    - если все подзадачи имеют статус DONE, то и эпик считается завершённым — со статусом DONE.
    - во всех остальных случаях статус должен быть IN_PROGRESS.*/

    Epic setEpicStatus(Epic epic);

    //добавить задачу в эпик
    void includeTaskToEpic(Task task, Epic epic) throws IllegalArgumentException, TaskAlreadyExistException;

    //вернуть тип задачи по ID
    TaskType getTaskType(Integer taskId) throws TaskNotFoundException;
}
import db.DbManager;
import exceptions.TaskNotFoundException;
import model.Epic;
import model.SubTask;
import model.Task;
import service.JdbcHistoryManager;
import service.JdbcTaskManager;
import service.Managers;
import util.TaskStatus;
import util.TaskType;

import java.io.IOException;
import java.sql.SQLException;

public class testApp {
    public static void main(String[] args) throws SQLException, IOException, TaskNotFoundException {
        DbManager dbman = Managers.getDefaultDatabase();
        JdbcHistoryManager hm = new JdbcHistoryManager(dbman);
        JdbcTaskManager db =  Managers.getDefault(dbman,hm);

        db.removeAllTasks();

        db.createTask(new Task(null,"Task1","MyTask1", TaskStatus.NEW, TaskType.TASK ));
        db.createTask(new Task(null,"Task1","MyTask2", TaskStatus.NEW, TaskType.TASK ));
        db.createTask(new Epic(null,"TaskEpic","MyEpic", TaskStatus.NEW, TaskType.EPIC));

        System.out.println(db.getAllTasks());
        System.out.println(db.findTaskByID(1, Task.class));
        System.out.println(db.findTaskByID(3, Epic.class));

        db.includeTaskToEpic(db.findTaskByID(1,Task.class),db.findTaskByID(3,Epic.class));
        db.updateTask(new SubTask(1,"SubTask1","SubTaskUpdated",TaskStatus.IN_PROGRESS, TaskType.SUBTASK,3), SubTask.class);
        System.out.println(db.getAllSubTasks(db.findTaskByID(3,Epic.class)));

        System.out.println(db.getAllTasks());
        System.out.println(db.getTaskType(1));



    }
}

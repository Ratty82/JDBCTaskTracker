import exceptions.TaskNotFoundException;
import model.Task;
import service.JdbcTaskManager;
import util.TaskStatus;
import util.TaskType;

import java.io.IOException;
import java.sql.SQLException;

public class testApp {
    public static void main(String[] args) throws SQLException, IOException, TaskNotFoundException {
        JdbcTaskManager db = new JdbcTaskManager();

        System.out.println(db.getAllTasks());
        System.out.println(db.findTaskByID(1, Task.class));

        db.createTask(new Task(2,"Task2","gfgfggf", TaskStatus.NEW, TaskType.TASK ));



    }
}

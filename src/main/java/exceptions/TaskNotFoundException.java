package exceptions;

public class TaskNotFoundException extends Exception{
    public TaskNotFoundException(int itemId){
        super("Задача с ID: " + itemId + " не найдена");
    }
}

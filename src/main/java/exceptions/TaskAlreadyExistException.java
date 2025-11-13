package exceptions;

public class TaskAlreadyExistException extends Exception{
    public TaskAlreadyExistException(int itemId){
        super("Задача с ID: " + itemId + " уже существует");
    }
}


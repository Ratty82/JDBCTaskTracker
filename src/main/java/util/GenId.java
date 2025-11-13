package util;

import java.util.Optional;

public class GenId {
    private static int counterTask;

    public static int setCounterTask(int id) {
        if (id > 0){
            counterTask = id;
        }
        return counterTask;
    }

    public static int getCounterTask() {
        return counterTask;
    }

    public static int setTaskId(){
        return ++counterTask;
    }

}

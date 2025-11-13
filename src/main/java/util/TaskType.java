package util;

public enum TaskType {
    TASK("Задача"),
    EPIC("Эпик"),
    SUBTASK("Подзадача");

    private final String desc;

    TaskType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}

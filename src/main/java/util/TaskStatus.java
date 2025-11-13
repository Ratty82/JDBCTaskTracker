package util;

public enum TaskStatus {
    NEW("Новая"),
    IN_PROGRESS("В работе"),
    DONE("Готова");

    private final String desc;

    TaskStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}


## 1. Изменения в Maven (`pom.xml`)

Добавьте зависимость драйвера PostgreSQL: -- done

```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.7.3</version>
</dependency>
```


Версию примерную указал, нужно в репе посмотреть
---

## 2. Настройки подключения

Создайте `db/db.properties` (не коммить реальные пароли):

```properties
jdbc.url=jdbc:postgresql://localhost:5432/db
jdbc.user=db
jdbc.password=postgres
jdbc.schema=public
```

Пример Docker для локальной БД:

```bash
docker run --name sjtt-postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_USER=db -e POSTGRES_DB=db -p 5432:5432 -d postgres:16
```

---

## 3. Шаблоны JDBC

- Получение соединения (DriverManager):
```java
try (Connection conn = DriverManager.getConnection(url, user, pass)) {
    // работа с БД
}
```

- INSERT с параметрами:
```java
String sql = "INSERT INTO tasks(id,name,details,status,type,epic_id) VALUES (?,?,?,?,?,?)";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, task.getTaskId());
    ps.setString(2, task.getTaskName());
    ps.setString(3, task.getTaskDetails());
    ps.setString(4, task.getTaskStatus().name());
    ps.setString(5, task.getTaskType().name());
    if (task instanceof SubTask s) ps.setInt(6, s.getTaskParentId()); else ps.setNull(6, Types.INTEGER);
    ps.executeUpdate();
}
```

- SELECT и маппинг:
```java
String sql = "SELECT id,name,details,status,type,epic_id FROM tasks WHERE id=?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, id);
    try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new TaskNotFoundException(id);
        int tid = rs.getInt("id");
        String name = rs.getString("name");
        String details = rs.getString("details");
        TaskStatus st = TaskStatus.valueOf(rs.getString("status"));
        TaskType tt = TaskType.valueOf(rs.getString("type"));
        Integer epicId = (Integer) rs.getObject("epic_id");
        // создать Task/Epic/SubTask в зависимости от tt
    }
}
```

- Транзакция:
```java
conn.setAutoCommit(false);
try {
    // несколько связанных операций
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(true);
}
```

- Обновление истории (UPSERT):
```sql
INSERT INTO task_history(task_id, last_viewed)
VALUES (?, now())
ON CONFLICT (task_id) DO UPDATE SET last_viewed = EXCLUDED.last_viewed;
```

---

## 4. План интеграции в проект

Минимальная интеграция предполагает новую реализацию `TaskManager` на JDBC и (опционально) реализацию `HistoryManager` на JDBC.

- **Новые классы (пакет `service/`):**
  - `JdbcTaskManager implements TaskManager`
    - Хранение и чтение задач из таблицы `tasks`.
    - Пересчёт статуса эпика при CRUD сабтасков — аналогично `InMemoryTaskManager.setEpicStatus(...)` (можно вычитывать сабтаски эпика SQL‑запросом и считать статус на лету).
    - `includeTaskToEpic(Task, Epic)` — транзакция: обновить эпик (его набор сабтасков фактически хранится как `epic_id` в строке сабтаска) и вставить/обновить `SUBTASK`.
  - `JdbcHistoryManager implements HistoryManager` (опция A — хранить историю в БД)
    - `addTaskToHistory(id)` — upsert в `task_history`.
    - `getHistory()` — `SELECT task_id FROM v_task_history_last10`.
    - `remove(id)` — `DELETE FROM task_history WHERE task_id=?`.

- **Фабрика (`service/Managers.java`):**
  - Добавить `getDefaultJdbc(Properties props)` для создания `JdbcTaskManager` и (опционально) `JdbcHistoryManager`.

- **Идентификаторы:**
  - Вариант 1 (проще): оставить генерацию через `GenId` (как сейчас) и вставлять `id` в БД.
  - Вариант 2 (правильно для БД): `id` генерируется Postgres (IDENTITY), после INSERT считывать сгенерированный id и синхронизировать `GenId.setCounterTask(max(id))`. Для совместимости с другими менеджерами начните с варианта 1.

- **История:**
  - Вариант А: перенести историю в БД (рекомендовано).
  - Вариант Б: сохранить in‑memory историю и просто синхронизировать при удалениях.

---

## 5. Чёткое ТЗ

1. Добавить зависимость `org.postgresql:postgresql` в `pom.xml`.
2. Создать SQL‑миграцию `db/migrations/V1__init.sql` с таблицами `tasks`, `task_history` и представлением `v_task_history_last10` (DDL выше).
3. Создать `db/db.properties` (образец выше). Учесть, что реальные креды не коммитятся.
4. Реализовать `service/JdbcTaskManager implements TaskManager`:
   - `getAllTasks()` — чтение всех строк `tasks`, сортировка по `id`.
   - `removeAllTasks()` — транзакция: `DELETE FROM tasks; DELETE FROM task_history;`.
   - `findTaskByID(id, type)` — SELECT по `id`, добавление в историю через `HistoryManager.addTaskToHistory(id)`.
   - `createTask(task)` — INSERT в `tasks`.
   - `updateTask(task)` — UPDATE по `id`.
   - `removeTaskById(id)` — транзакция: если `Epic` — удалить эпик (каскад удалит его `SubTask` по FK), удалить записи из истории; если `SubTask` — удалить строку и пересчитать эпик; если `Task` — удалить строку.
   - `getAllSubTasks(epic)` — SELECT всех `SUBTASK` с `epic_id = epic.id`.
   - `setEpicStatus(epic)` — вычислить по сабтаскам через SQL (если нет сабтасков или все NEW → NEW; если все DONE → DONE; иначе IN_PROGRESS) и вернуть новый объект `Epic`.
   - `includeTaskToEpic(task, epic)` — транзакция: превратить `task` в `SUBTASK` (или вставить новый) с `epic_id` и пересчитать эпик.
   - `getTaskType(id)` — SELECT type.
5. Реализовать `service/JdbcHistoryManager implements HistoryManager` (опция А):
   - `getHistory()` — вернуть список ID из `v_task_history_last10` (по убыванию `last_viewed`).
   - `addTaskToHistory(id)` — UPSERT в `task_history`.
   - `remove(id)` — удалить из `task_history`.
6. Обновить `service/Managers.java`:
   - `getDefaultJdbc(Properties props)` — создать объекты JDBC менеджеров.
7. Не менять HTTP API. Хэндлеры должны опираться на абстракции интерфейсов `TaskManager` и `HistoryManager`.
8. Критерии приёмки:
   - Все операции CRUD задач работают против PostgreSQL.
   - История отображает последние 10 уникальных ID корректно, очищается при удалениях.
   - Инварианты эпика соблюдаются.
   - Приложение запускается и переживает перезапуск без потери данных.

---


## 6. Мини‑задачи для закрепления

1. Напишите метод JDBC, который возвращает все `SubTask` заданного `Epic` (SQL + маппинг в `SubTask`).
2. Реализуйте транзакционно `includeTaskToEpic(task, epic)` — если `task` был `TASK`, превратите в `SUBTASK` с `epic_id`, пересчитайте эпик. Если уже был `SUBTASK`, только поменяйте `epic_id`.
3. Сделайте метод `getTaskType(id)` максимально эффективным — выбирайте только колонку `type` одним запросом и маппьте в `TaskType`.
4. Напишите интеграционный тест: создайте `EPIC` + 2 `SUBTASK`, поменяйте статус сабтасков, убедитесь, что `EPIC` пересчитался. Перезапустите приложение и проверьте, что данные восстановились из БД.
5. Реализуйте очистку истории при удалении `EPIC` (проверьте, что каскадно удаляются и сабтаски; история очищается для всех связанных ID).

---

## 7. Подводные камни и лучшие практики

- **Всегда** закрывай ресурсы (`Connection/PreparedStatement/ResultSet`) через try-with-resources.
- Не используй `Statement` со строковой конкатенацией — только `PreparedStatement`.
- Контролируй `autoCommit`: для нескольких операций — явная транзакция.
- Храни `status`/`type` как текстовые значения `enum.name()` — проще, читаемо, совместимо с Java‑перечислениями.
- Не храни креды в git. Используйте переменные окружения/секреты.

---

## 8. Чек‑лист проверки

- Подключение к БД успешно, таблицы созданы.
- CRUD для `TASK/EPIC/SUBTASK` работает, `getTaskType(id)` корректен.
- История возвращает последние 10 уникальных ID; после удаления задач запись из `task_history` пропадает.
- Перезапуск процесса сохраняет данные.
- HTTP‑эндпоинты без изменений и работают поверх JDBC‑менеджера.

---

## 9. Дальнейшее развитие (опционально посмотрим что можно добавить в курс)

- **Flyway** для версионирования схемы (`db/migrations`). 
- **Log SQL**: SLF4J/Logback, логирование длительных запросов.
- **Метрики**: время запросов, пул соединений.

---

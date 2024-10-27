package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private boolean isAvailableCreateTask = false;
    private Long chatId;

    @Autowired
    private NotificationTaskRepository repository;

    @Autowired
    private TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
        checkTasks();
    }

    @Override
    public int process(List<Update> updates) {
        String regex = "(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)";
        Pattern pattern = Pattern.compile(regex);
        this.chatId = updates.get(0).message().chat().id();

        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            logger.info("Text of message : {}", update.message().text());

            if (update.message().text().equals("/start")) {
                sendMessage("Привет! Используй /createTask для создания задачи. \uD83E\uDD13");
            }

            if (isAvailableCreateTask) {
                Matcher matcher = pattern.matcher(update.message().text());

                if (matcher.matches()) {
                    LocalDateTime dateTime = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                    String message = matcher.group(3);

                    NotificationTask nt = new NotificationTask(dateTime, message);
                    repository.save(nt);
                    sendMessage("Задача успешно сохранена! \uD83E\uDD73");
                    isAvailableCreateTask = false;
                } else {
                    sendMessage("Задача должна быть в формате dd:mm:yy 00:00 text.");
                    logger.info("Строка не соответствует паттерну.");
                }
            }

            if (update.message().text().equals("/createTask")) {
                sendMessage("Отправьте свою задачу следующим сообщением! \uD83D\uDE0E");
                isAvailableCreateTask = true;
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendMessage(String text) {
        var sendMessage = new SendMessage(chatId, text);

        try {
            telegramBot.execute(sendMessage);
        } catch (Exception e) {
            logger.info("Problem with send message: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void checkTasks() {
        List<NotificationTask> notificationTaskList = repository.findAll();

        for (NotificationTask n : notificationTaskList) {
            if (LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).equals(n.getDate())) {
                sendMessage("Задача: " + "'" + n.getText() + "'\n" + "Необходимо выполнить сегодня!");
            }
        }
    }
}

package pro.sky.telegrambot.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationTaskScheduler {

    private final NotificationTaskRepository repository;
    private final TelegramBotUpdatesListener telegramBotUpdatesListener;


    public NotificationTaskScheduler(NotificationTaskRepository repository, TelegramBotUpdatesListener telegramBotUpdatesListener) {
        this.repository = repository;
        this.telegramBotUpdatesListener = telegramBotUpdatesListener;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void checkTasks() {
        List<NotificationTask> notificationTaskList = repository.findAll();

        for (NotificationTask n : notificationTaskList) {
            if (LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).equals(n.getDate())) {
                telegramBotUpdatesListener.sendMessage("Задача: " + "'" + n.getText() + "'\n" + "Необходимо выполнить сегодня!");
            }
        }
    }
}

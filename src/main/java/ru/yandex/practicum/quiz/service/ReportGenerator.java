package ru.yandex.practicum.quiz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.quiz.config.AppConfig;
import ru.yandex.practicum.quiz.model.QuizLog;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.practicum.quiz.config.AppConfig.ReportMode.VERBOSE;
import static ru.yandex.practicum.quiz.config.AppConfig.ReportOutputMode.CONSOLE;
import static ru.yandex.practicum.quiz.config.AppConfig.ReportOutputSettings;
import static ru.yandex.practicum.quiz.config.AppConfig.ReportSettings;

@Slf4j
@Component
public class ReportGenerator {
    private final String reportTitle;
    private final ReportSettings reportSettings;

    public ReportGenerator(AppConfig appConfig) {
        this.reportTitle = appConfig.getTitle();
        this.reportSettings = appConfig.getReport();
    }
    public void generate(QuizLog quizLog) {
        // Если генерация отключена, завершаем метод
        if (!reportSettings.isEnabled()) {
            log.debug("Вывод отчёта отключён, генерация отчёта прекращена");
            return;
        }

        ReportOutputSettings outputSettings = reportSettings.getOutput();
        log.trace("Отчёт будет выведен: {}", outputSettings.getMode());
        try {
            // создаём объект PrintWriter, выводящий отчёт в файл или консоль в зависимости от настроек
            boolean isConsole = outputSettings.getMode().equals(CONSOLE);
            try (PrintWriter writer = (isConsole ?
                    new PrintWriter(System.out) :
                    new PrintWriter(outputSettings.getPath()))
            ) {
                write(quizLog, writer);
            }
        } catch (Exception exception) {
            log.warn("При генерации отчёта произошла ошибка: ", exception);
        }
    }

    private void write(QuizLog quizLog, PrintWriter writer) {
        writer.println("Отчет о прохождении теста " + reportTitle + "\n");
        for (QuizLog.Entry entry : quizLog) {
            if (reportSettings.getMode().equals(VERBOSE)) {
                writeVerbose(writer, entry);
            } else {
                writeConcise(writer, entry);
            }
        }
        writer.printf("Всего вопросов: %d\nОтвечено правильно: %d\n", quizLog.total(), quizLog.successful());
    }

    private void writeVerbose(PrintWriter writer, QuizLog.Entry entry) {
        // записываем номер и текст вопроса
        writer.println("Вопрос " + entry.getNumber() + ": " + entry.getQuestion().getText());

        // записываем варианты ответов
        List<String> options = entry.getQuestion().getOptions();
        for (int i = 0; i < options.size(); i++) {
            writer.println((i + 1) + ") " + options.get(i));
        }

        // записываем ответы пользователя
        writer.print("Ответы пользователя: ");
        List<Integer> answers = entry.getAnswers();
        for (Integer answer : answers) {
            writer.print(answer + " ");
        }
        writer.println();

        // записываем флаг успешности ответа
        String successFlag = entry.isSuccessful() ? "да" : "нет";
        writer.println("Содержит правильный ответ: " + successFlag);
        // добавляем пустую строку между записями
        writer.println();
    }

    private void writeConcise(PrintWriter writer, QuizLog.Entry entry) {
        // записываем номер и текст вопроса
        char successSign = entry.isSuccessful() ? '+' : '-';
        String answers = entry.getAnswers().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        writer.printf("%d(%s): %s\n", entry.getNumber(), successSign, answers);
    }
}


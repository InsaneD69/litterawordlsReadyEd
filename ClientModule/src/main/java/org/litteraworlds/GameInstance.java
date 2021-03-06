package org.litteraworlds;

import org.litteraworlds.game.WorldGenerator;
import org.litteraworlds.input.Command;
import org.litteraworlds.net.Requests;
import org.litteraworlds.view.GameScreen;
import org.litteraworlds.view.MessageType;
import org.litteraworlds.view.TextLines;
import org.litteraworlds.view.colors.TextColors;
import org.litteraworlds.workers.ConnectionWorker;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.litteraworlds.input.PlayerInput.inputCommand;

public class GameInstance {

    private ExecutorService workers;

    private final ConnectionWorker connectionWorker;

    public GameInstance() {
        workers = Executors.newCachedThreadPool();

        connectionWorker = new ConnectionWorker();

        workers.execute(connectionWorker);

        Requests.setConnectionWorker(connectionWorker);

        initSequence();
    }

    private void initSequence() {
        GameScreen.init();
        GameScreen.putString(MessageType.SYSTEM, "Запуск игрового клиента");
        GameScreen.putString(MessageType.SYSTEM, "Оконная форма инциализирована");

        TextLines.init();
        GameScreen.putString(MessageType.SYSTEM, "Текстовые данные загружены");

        Command.init();
        GameScreen.putString(MessageType.SYSTEM, "Игровые команды инициализированы");

        if(!WorldGenerator.checkWorldExists()) {
            GameScreen.putString(MessageType.SYSTEM, "Получение хэша с сервера и генерация мира");
            WorldGenerator.generateWorld(Requests.getHash());
            GameScreen.putString(MessageType.SYSTEM, "Игровой мир сгенерирован");
        } else {
            if(WorldGenerator.loadWorldFromFile()) {
                GameScreen.putString(MessageType.SYSTEM, "Игровой мир загружен");
            } else {
                GameScreen.putString(MessageType.ERROR, "Произошла ошибка при загрузке игрового мира!");
            }
        }
    }

    public void startSequence() {
        GameScreen.putString(TextColors.HELP_MESSAGE, "Для начала игры, введите команду /начать");
        GameScreen.putString(TextColors.HELP_MESSAGE, "Для продолжения игры, введите команду /войти");
        while (!GameScreen.isClose()) {
            try {
                String command = inputCommand();
                switch (command) {

                    case "/начать" -> {

                        for (String line : TextLines.getIntrolines()) {
                            GameScreen.putString(line);
                        }

                        inputCommand();

                        GameLoop gameLoop = new GameLoop(connectionWorker);

                        gameLoop.start();

                    }
                    case "/войти" -> {


                        GameLoop gameLoop = new GameLoop(connectionWorker);

                       String userName = gameLoop.resume();
                        GameScreen.putString(MessageType.SYSTEM, "Вход в аккаунт с никнеймом "+userName+"был успешно выполнен");


                    }

                    case "/выход" -> {
                        workers.shutdownNow();
                        workers.awaitTermination(5, TimeUnit.SECONDS);
                    }

                    default -> { continue;}
                }
                if (GameScreen.isClose()) {
                    break;
                }
            } catch (InterruptedException | IOException | NoSuchAlgorithmException | ClassNotFoundException e){
                e.printStackTrace();
            } finally {
                GameScreen.dispose();
                connectionWorker.close();
                workers.shutdown();
            }
        }
    }
}

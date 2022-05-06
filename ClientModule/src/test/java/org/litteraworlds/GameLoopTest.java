package org.litteraworlds;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.litteraworlds.dto.PlayerDTO;
import org.litteraworlds.game.WorldGenerator;
import org.litteraworlds.input.Command;
import org.litteraworlds.net.Requests;
import org.litteraworlds.utils.HashGen;
import org.litteraworlds.utils.HashToString;
import org.litteraworlds.view.GameScreen;
import org.litteraworlds.view.MessageType;
import org.litteraworlds.view.TextLines;
import org.litteraworlds.workers.ConnectionWorker;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameLoopTest {


    static ConnectionWorker connectionWorker;
    static ExecutorService workers;
    static String userName;
    static GameLoop gameLoop;
    static PlayerDTO playerDTO;

    // Запускать только во время запущенного сервера!

    @BeforeAll
    static void  init() throws Exception {


        workers = Executors.newCachedThreadPool();

        connectionWorker = new ConnectionWorker();

        workers.execute(connectionWorker);

        Requests.setConnectionWorker(connectionWorker);

        GameScreen.init();
        TextLines.init();
        Command.init();
        if(!WorldGenerator.checkWorldExists()) {

            WorldGenerator.generateWorld(Requests.getHash()); ;
        } else {
            if(WorldGenerator.loadWorldFromFile()) {

            } else {
               throw  new Exception("Ошибка при загрузке мира");
            }
        }

        gameLoop = new GameLoop(connectionWorker);
        userName      =   gameLoop.resume();
    }

    @Test
    @DisplayName("Объект PlayerDTO дожен быть получен после регистрации ")
    void checkTakenPlayerDtoObject() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

       playerDTO=gameLoop.getPlayerDTO();

       Assertions.assertNotNull(playerDTO,"объект PlayerDTO не был получен");

    }


    @Test
    @DisplayName("Введенный никнейм должен совпадать с никнеймом в playerDTO")
    void checkUserNameInPlayerDtoObject() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {


        Assertions.assertTrue(HashToString.convert(HashGen.getHash(userName)).equals(HashToString.convert(HashGen.getHash(playerDTO.getPlayerName()))),"хэши никнеймов не совпадают");


    }

}

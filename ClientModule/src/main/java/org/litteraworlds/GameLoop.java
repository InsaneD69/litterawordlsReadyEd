package org.litteraworlds;

import org.litteraworlds.dto.PlayerDTO;
import org.litteraworlds.game.GameLogic;
import org.litteraworlds.game.PlayerCreation;
import org.litteraworlds.game.WorldGenerator;
import org.litteraworlds.input.Command;
import org.litteraworlds.map.Region;
import org.litteraworlds.net.Requests;
import org.litteraworlds.objects.Player;
import org.litteraworlds.utils.HashGen;
import org.litteraworlds.utils.HashToString;
import org.litteraworlds.view.Debug;
import org.litteraworlds.view.colors.TextColors;
import org.litteraworlds.view.GameScreen;
import org.litteraworlds.view.MessageType;
import org.litteraworlds.workers.ConnectionWorker;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import static org.litteraworlds.input.PlayerInput.inputCommand;

/**
 * <h2>[CLIENT-SIDE]</h2>
 * <h3>GameLoop</h3>
 * Класс, описывающий поведение игрового цикла<br>
 * Содержит функции для создания игрока и его регистрации на сервере.
 */
public class GameLoop {

    private static Player player;

    private ConnectionWorker connectionWorker;

    private Region region;
    private PlayerDTO playerDTO;

    public PlayerDTO getPlayerDTO() {
        return playerDTO;
    }

    public GameLoop(ConnectionWorker connectionWorker){
        this.connectionWorker = connectionWorker;

        GameScreen.putString(TextColors.GAME_MESSAGE,"Посреди осколков старого мира появляется таинственный герой");

    }

    public static Player getPlayer(){
        Debug.toLog("Getting player "+player);
        return player;
    }


    public void start() throws IOException, NoSuchAlgorithmException {
        //Вызов функции создания персонажа
        player = PlayerCreation.newCreationSequence().createNewCharacter();

        //генерация и хэширование нового пароля
        String password = HashGen.randomPasswordGenerator(player.getTokenIDBytes());
        byte[] hashPassword= getHashPassword(password);

        GameScreen.putString(MessageType.SYSTEM,"Регистрация игрока на сервере");
        GameScreen.putString(MessageType.SYSTEM,"Отправка данных...");

        //передача на сервер никнейма и хеш пароля
        if(Requests.registerAuthDataPlayer(player.getName(),HashGen.encode(hashPassword))){

            GameScreen.putString(MessageType.SYSTEM,"Регистрация прошла успешно!");


        } else {
            GameScreen.putString(MessageType.ERROR,"Проблемы с регистрацией");
            GameScreen.putString(MessageType.INFO,"Нажмите любую кнопку для выхода");
            inputCommand();
        }

        GameScreen.putString(MessageType.SYSTEM, "Запуск игрового цикла");

        //Отравляем данные об игроке на сервер для получения хэша генерации мира
        byte[] hash = Requests.getHash();
        String hashLine = HashToString.convert(hash);
        GameScreen.putString(MessageType.SYSTEM,"Ожидание хэша для генерации");
        Requests.setConnectionWorker(connectionWorker);



        Debug.toLog("h length "+hashLine.length());
        Debug.toLog("hash:"+hashLine);

        GameScreen.putString(MessageType.INFO, hashLine);

        region = WorldGenerator.getWorld().getRegions().get(0);

        Debug.toLog("Помещаем игрока с случайную зону");
        region.putPlayerIntoRandomZone(player);

        /* Отправляем данные персонажа на сервер */
        GameScreen.putString(MessageType.SYSTEM,"Регистрация мира игрока на сервере");
        GameScreen.putString(MessageType.SYSTEM,"Отправка данных...");

        if(sendPlayerDataToServer()){

            GameScreen.putString(MessageType.SYSTEM,"Регистрация мира прошла успешно!");
            GameScreen.putString(MessageType.SYSTEM,"Ваш пароль для входа в игру");
            GameScreen.putString(MessageType.SYSTEM, password);

            //сохраняем пользователю пароль в txt файлик
            savePasswordIntoFile(password);

            return;

        } else {
            GameScreen.putString(MessageType.ERROR,"Проблемы с регистрацией");
            GameScreen.putString(MessageType.INFO,"Нажмите любую кнопку для выхода");
            inputCommand();
        }
    }

    public String resume() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

        while (true) {

            GameScreen.putString(TextColors.GAME_MESSAGE, "Введите свой никнейм:");

            String userName = inputCommand();


            //получение байтов tokenID игрока с сервера,
            // 'O' - get "Old" tokenID, 'N' =  get "new" tokenID
            byte[] byteTokenID=Requests.getTokenIDFromServer(userName,'O');




            // проверка пришедшего токена:
            if (new String(byteTokenID).equals("null")){

                GameScreen.putString(TextColors.ERROR_MESSAGE, "Игрока с  никнеймом "+userName+" не существует");
                continue;

            }


            String tokenID = new String(
                    (new SecretKeySpec(byteTokenID,"AES"))
                            .getEncoded()
            );

            Debug.toLog("Gameloop: cont: Полученный токен ид : "+tokenID);

            GameScreen.putString(TextColors.GAME_MESSAGE, "Введите свой пароль:");

            String enteredPassword = inputCommand();

            //получение хэша пароля
            String enteredHashPassword = HashToString.convert(
                    HashGen.hashPassword(
                            tokenID.getBytes(StandardCharsets.UTF_8)
                            ,enteredPassword.getBytes(StandardCharsets.UTF_8))
            );

            Debug.toLog("Gameloop: cont: сгенерированный хэш пароля : "+enteredHashPassword);

            playerDTO = Requests.logIn(userName,enteredHashPassword);

            if (playerDTO!=null) { //объект будет null в случае неверного пароля

                GameScreen.putString(MessageType.SYSTEM, "Вы вошли в аккаунт под именем: " + userName);

               return userName;

            } else {
                GameScreen.putString(MessageType.SYSTEM, "Неверный  пароль" );
                continue;
            }


        }

        //запуск игры с последнего места игрока на карте

    }

    private void savePasswordIntoFile(String password){

        try{
            PrintWriter writer = new PrintWriter(player.getName()+".txt", "UTF-8");
            writer.println(password);
            writer.close();

        }
        catch (FileNotFoundException |UnsupportedEncodingException e){
            e.printStackTrace();
            GameScreen.putString(MessageType.ERROR,"Проблемы с сохранением пароля!");
        }
    }


    private byte[] getHashPassword(String password) throws NoSuchAlgorithmException {

        //хэширование   пароля
        return  HashGen.hashPassword(player.getTokenID().getBytes(StandardCharsets.UTF_8),password.getBytes(StandardCharsets.UTF_8));

    }

    private boolean sendPlayerDataToServer(){
        return Requests.registerPlayer(player);
    }

    /**
     * Запуск игрового процесса
     */
    private void gameStart(){
        GameScreen.putString(TextColors.PLAYER_COLOR, "Вы оказываетесь в регионе "+region+" в зоне "+player.getObjectPlace());

        GameLogic.lookAround();

        GameScreen.putString(TextColors.GAME_MESSAGE,"Что будете делать дальше?");
        GameScreen.putString(TextColors.HELP_MESSAGE,"Наберите /помощь для списка команд");

        String commandLine;
        while (!(commandLine = inputCommand()).equals("/выход")){
            if(commandLine.startsWith("/")){
                Command.parse(commandLine);
            }
        }

    }
}

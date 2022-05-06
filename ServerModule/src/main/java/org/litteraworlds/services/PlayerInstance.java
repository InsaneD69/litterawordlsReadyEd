package org.litteraworlds.services;

import org.litteraworlds.database.DBService;
import org.litteraworlds.dto.DataStorage;
import org.litteraworlds.dto.PlayerDTO;
import org.litteraworlds.security.HashGen;
import org.litteraworlds.security.HashToString;
import org.litteraworlds.services.annotations.Mapping;
import org.litteraworlds.services.annotations.Params;

import javax.crypto.SecretKey;
import javax.tools.FileObject;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * <h2>[SERVER-SIDE]</h2>
 * <h3>PlayerInstance</h3>
 * Экземпляр сессии пользователя.
 * Содержит методы, которые выполняют различные запросы от клиента. Методы аннотируются интерфейсом {@link Mapping} и вызываются через рефлексию в классе-обработчике
 * {@link PlayerInstanceHandler}.
 */
public class PlayerInstance {

    public static final String WAIT_RESPONSE = "WAIT_FOR_DTO";
    public static final String WAIT_EXPECT = "EXPECT_DTO";

    private Socket client;

    private final Logger logClient = Logger.getGlobal();

    private InputStream in;

    private OutputStream out;

    private String clIP;

    private static PlayerDTO lastTakenPlayerDTO;

    public Socket getClient() {
        return client;
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }

    public PlayerInstance(Socket client) throws IOException {
        this.client = client;
        this.in = client.getInputStream();
        this.out = client.getOutputStream();
        logClient.info("Client " + client + " has connected");

    }

    private byte[] trimBuffer(byte[] buffer){

        int size = 0;

        for(int i = 0; i < buffer.length; i++){
            if(buffer[i] == 0){
                break;
            }
            size++;
        }
        return Arrays.copyOfRange(buffer, 0, size);
    }

    //осуществляется магия, не смотреть
    private String[] separator(byte[] data){

        String params = new String(data);

        int numOfLengthFirstWord = Integer.parseInt(String.valueOf(params.charAt(0)));
        params=params.substring(1);

        int lengthFirstWord;

        if(numOfLengthFirstWord>1){

            char[] qwerty= {params.charAt(0),params.charAt(1)};
            lengthFirstWord= Integer.parseInt(String.valueOf(qwerty[0])+String.valueOf(qwerty[1]));
            params=params.substring(2);

        }
        else{
            lengthFirstWord= Integer.parseInt(String.valueOf(params.charAt(0)));
            params=params.substring(1);

        }

        return new String[]{params.substring(0,lengthFirstWord)
                            , params.substring(lengthFirstWord)};


    }



    public byte[] getFromClient(byte[] buffer, BufferedInputStream bIn) throws IOException {
        if(bIn.read(buffer) > 0){
            return trimBuffer(buffer);
        } else {
            return new byte[0];
        }
    }

    public void sendToClient(byte[] reply) {
        logClient.info(clIP+"|>Reply: " + Arrays.toString(reply));
        try {
            out.write(reply);
            out.flush();
        } catch (IOException e) {
            logClient.info("|>Error");
        }
    }

    @Mapping("HASH")
    private void sendHash(@Params byte[] data){

        String stringData = new String(data);

        logClient.info(clIP+"|> get data: "+stringData);

        byte[] hashResponse = HashGen.getHash("Genesis");

        logClient.info(clIP+"|> send hash: "+stringData);

        sendToClient(hashResponse);
    }

    //TODO: при регистрации игрока, помимо ответа "OK" должна возвращаться
    // сгенерированная строка для поля tokenID. Необходимо добавить локальное данных о нём на диск
    // и в файловую "базу данных" типа csv, в формате:
    // tokenID, playerName, playerPlaceHashID (это хэш местоположения)
    /**
     * Обработчик регистрации нового игрока. Готовность о приёме данных сообщается ответным запросом:
     * {@value #WAIT_RESPONSE}, после чего входной поток оборачивается классом {@link ObjectInputStream} и готовится принимать
     * сериализованные данные класса {@link PlayerDTO}, см.@see <a href="https://ru.wikipedia.org/wiki/DTO">Data Transfer Object</a>
     */

    @Mapping("PREG")
    private void registerNewPlayer(){
        try {

            sendToClient(WAIT_RESPONSE.getBytes(StandardCharsets.UTF_8));

            ObjectInputStream inputStream = new ObjectInputStream(in);

            PlayerDTO playerDTO = (PlayerDTO) inputStream.readObject();

            lastTakenPlayerDTO = playerDTO;

            logClient.info(clIP + "|> take something");
            logClient.info(clIP + "|> player data: "+playerDTO.toString());
            logClient.info(clIP + "|> send response OK");

            DataStorage.saveData(playerDTO);
            DBService.getInstance()
                    .insertNewPlayer(playerDTO);

            sendToClient("OK".getBytes(StandardCharsets.UTF_8));

        } catch (IOException e){
            logClient.info(clIP + "|> send response NOT OK");
            sendToClient("NOT OK".getBytes(StandardCharsets.UTF_8));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Mapping("CHNM")
    private void checkNickName(@Params byte[] nickname){

        String stringData = new String(nickname);
        logClient.info(clIP+"|> get data: "+stringData);

        if(DBService.getInstance().checkUniquenessNickName(stringData)){

            sendToClient("OK".getBytes(StandardCharsets.UTF_8));

        }
        else {
            sendToClient("NOT OK".getBytes(StandardCharsets.UTF_8));

        }

    }

    @Mapping("GTKN")
    private void getTokenID(@Params byte[] data) throws InvalidKeySpecException, NoSuchAlgorithmException, SQLException {

        String userData= new String(data);


        logClient.info(clIP+"|> get data username: "+userData.substring(1));

        if(userData.charAt(0)=='N'){

            SecretKey tokenID = HashGen.generateKey(userData.substring(1));
            logClient.info(clIP+"|> generate tokenId: "+tokenID);
            logClient.info(clIP+"|> generate tokenId: "+tokenID.getEncoded());
            sendToClient(tokenID.getEncoded());

        }
        else if(userData.charAt(0)=='O'){

           String tokenID=DBService.getInstance().getOldTokenByUserName(userData.substring(1));

           if (tokenID==null){

               sendToClient("null".getBytes(StandardCharsets.UTF_8));
               return;

           }

           logClient.info(clIP+"|> sending to user tokenId: "+tokenID);
           sendToClient(tokenID.getBytes(StandardCharsets.UTF_8));
        }
        else {
            logClient.info(clIP+"|> Bad request to getTokenID, required 'N' or 'O', given: "+userData.charAt(0));
            sendToClient("NOT OK".getBytes(StandardCharsets.UTF_8));
        }




    }

    @Mapping("AUTH")
    private void userAuthentication(@Params byte[] data) throws SQLException {

        String[] authData= separator(data);//0: username, 1: hashPassword

        String userRealHashPassword=DBService.getInstance().authentication(authData[0]);
        logClient.info(clIP+"|> user with nickname "+authData[0]+" enter hashPassword: "+authData[1]);


        if(userRealHashPassword.equals(authData[1])){
            logClient.info(clIP+"|> user with nickname "+authData[0]+" have successful  login attempt");

            String path=HashToString.convert(HashGen.getHash(authData[0]))+".pdto";
            File playerDtoFile = new File(path);


            if(playerDtoFile.exists()){


                sendToClient(WAIT_EXPECT.getBytes(StandardCharsets.UTF_8));


                try{
                    //получаем объект playerDTO из файла
                    FileInputStream playerDtoFileStream = new FileInputStream(path);
                    ObjectInputStream playerDtoObjectStream = new ObjectInputStream(playerDtoFileStream);
                    PlayerDTO playerDTO = (PlayerDTO)playerDtoObjectStream.readObject();


                    //отправляем объект playerDTO клиенту
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
                    objectOutputStream.writeObject(playerDTO);
                    objectOutputStream.flush();


                } catch (IOException | ClassNotFoundException e){

                    sendToClient("SERVER_ERROR_FIND_DTO".getBytes(StandardCharsets.UTF_8));
                    e.printStackTrace();

                }


            }
            else{
                logClient.info(clIP+"|>ERROR the   "+authData[0]+" DTO file has been lost");

            }


        }
        else {
            logClient.info(clIP+"|> user with nickname "+authData[0]+" have unsuccessful login attempt");
            sendToClient("NOT OK".getBytes(StandardCharsets.UTF_8));
        }

    }


    @Mapping("VLWD")
    private void validateWorld(@Params byte[] worldHash){
        logClient.info("Get world hash bytes for validate");
        logClient.info(Arrays.toString(worldHash));

        String worldHashLine = "";

        for(byte b : worldHash){
            worldHashLine = worldHashLine.concat(String.format("%02x",b));
        }

        logClient.info("World hash line ");
        logClient.info(worldHashLine);

        if(Arrays.equals(HashGen.getHash("Genesis"), worldHash)){
            logClient.info("World hash id is valid");
            sendToClient("VALID".getBytes(StandardCharsets.UTF_8));
        } else {
            logClient.info("World hash id is not valid");
            sendToClient("NOT_VALID".getBytes(StandardCharsets.UTF_8));
        }
    }


    @Mapping("GUAD")

    private void getUserPassword(@Params byte[] data){


        String[] userNameAndHashPass = separator(data);

        if(DBService
                .getInstance()
                .insertAuthDataNewPlayer(userNameAndHashPass[0],userNameAndHashPass[1])
        )
        {

            sendToClient("OK".getBytes(StandardCharsets.UTF_8));

        }
        else {

            sendToClient("NOT OK".getBytes(StandardCharsets.UTF_8));

        }

    }


    public static  PlayerDTO waitNewRegistration() {

        while(lastTakenPlayerDTO==null) {

            System.out.println("Wait new registration...");
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }


        }

        return  lastTakenPlayerDTO;

    }
}

package org.litteraworlds.dto;

import org.litteraworlds.security.HashGen;
import org.litteraworlds.security.HashToString;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class DataStorage {

    private DataStorage(){}

    private static final ArrayList<PlayerDTO> dtoCache = new ArrayList<>();

    private static Semaphore semaphore = new Semaphore(1);

    public static void saveData(PlayerDTO playerDTO){
        try {
            semaphore.acquire();

            dtoCache.add(playerDTO);

            // сохраняем данные DTO в файл
            savePlayerDTOInFile(playerDTO);
////////

        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    public static void flushDataToFile(){
        FileOutputStream outputStream;
        ObjectOutputStream stream;

        for(PlayerDTO playerDTO : dtoCache){
            //outputStream = new FileOutputStream(new File(""));
        }
    }
////////////////
    private static void savePlayerDTOInFile(PlayerDTO playerDTO){


        byte[] hashName = HashGen.getHash(playerDTO.getPlayerName());

        try {

            //PrintWriter writer = new PrintWriter(HashToString.convert(hashName)+ ".pdto", "UTF-8");
           // writer.println(playerDTO);
           // writer.close();

            FileOutputStream fileOutputStream = new FileOutputStream(HashToString.convert(hashName)+ ".pdto");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(playerDTO);


            objectOutputStream.close();
            fileOutputStream.close();

        } catch(IOException e ){
            e.printStackTrace();
        }
    }
//////////////
}

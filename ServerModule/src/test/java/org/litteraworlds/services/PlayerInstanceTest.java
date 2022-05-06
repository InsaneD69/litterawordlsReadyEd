package org.litteraworlds.services;

import org.junit.jupiter.api.*;
import org.litteraworlds.ServerStart;
import org.litteraworlds.database.DBService;
import org.litteraworlds.dto.PlayerDTO;

import java.io.IOException;





public class PlayerInstanceTest {


    PlayerDTO playerDTO;

    @BeforeAll
    static void init() throws IOException {

        ServerStart.main(new String[]{});

    }

    @Test
    @DisplayName("проверяет, что полученный в конце регистрации объект PlayerDTO не равен null")
    void  checkTakenPlayerDTO(){


        System.out.println("checkTakenPlayerDTO is work");

        playerDTO=PlayerInstance.waitNewRegistration();

        Assertions.assertNotNull(playerDTO,"объект PlayerDTO  равен null");


    }

    @Test
    @DisplayName("проверяет, что  текущей локации игрока совпадает с тем, что есть на сервере")
    void  checkPlaceHashUser(){

        String placeHashUserFromBD=DBService.getInstance().selectPlayerRowByName(playerDTO.getPlayerName())[3];

        Assertions.assertTrue(placeHashUserFromBD.equals(playerDTO.getPlayerPlaceHashID()),
                "хэш текущей локации игрока не  совпадает с тем, " +
                "что есть на сервере");
    }
}

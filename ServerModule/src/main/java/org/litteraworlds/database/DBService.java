package org.litteraworlds.database;

import org.litteraworlds.dto.PlayerDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

public class DBService {
    private String hostName="ec2-52-48-159-67.eu-west-1.compute.amazonaws.com";
    private String dBName ="dfblnf1s7191i8";
    private String port="5432";
    private String user = "esukhkdkydaenr";
    private String password ="74c49179e980b54db7205c8b3379df4ab7e326a55f741bd8d6d81234295f609a";





    private final String SELECT_FROM_USERS =
            "SELECT * " +
            "FROM users "+
            "WHERE name = ?";



    private final String INSERT_INTO_USERS =
            "INSERT INTO users (name, password)" +
                    "VALUES (?,?)";

    private final String UPDATE_USER =
            "UPDATE users " +
                    "SET ";
    private final String UPDATE_USER_WHERE =
            "WHERE name = ?";

    private static DBService instance;

    private Connection myPostgresqlConnection;

    private DBService(){
        try {
            Properties dbConnectionProp = new Properties();
            dbConnectionProp.load(DBService.class.getResource("/DataBaseConnection.properties").openStream());
            Logger.getGlobal().info(dbConnectionProp.toString());

            Class.forName("org.postgresql.Driver");
            myPostgresqlConnection= DriverManager
                    .getConnection("jdbc:postgresql://"+hostName+":"+port+"/"+ dBName +"?user="+user+"&password="+password);

        } catch (IOException | SQLException e){
            Logger.getGlobal().info("Connection error, caused by "+e.getCause().toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();

        }
    }

    static {
        instance = new DBService();
    }

    /**
     * Осуществляет поиск в БД по имени игрока.
     * Возвращает массив строк, сформированный по полученным полям, если пользователь найден
     * Если нет, то вернёт пустой массив.
     * @param playerName имя игрока
     * @return массив полей пользователя, либо пустой массив
     */
    public String[] selectPlayerRowByName(String playerName) {
        String[] outArray = new String[0];
        try {
            ResultSet set = getResultSetFromPrepStatement(makePreparedStatementByData(SELECT_FROM_USERS, playerName));

            if(set != null) {
                if(set.next()) {

                    outArray = new String[]{
                            set.getString(1),//TokenID
                            set.getString(2),//name
                            set.getString(3),//password
                            set.getString(4)};//place_hash

                    System.out.println(Arrays.toString(outArray));

                    set.close();
                } else {
                    return outArray;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return outArray;
    }

    public boolean checkUniquenessNickName(String comparingNickName)  {

        try {

            ResultSet resultSet = getResultSetFromPrepStatement(makePreparedStatementByData(SELECT_FROM_USERS
                                                                                            ,comparingNickName));

            if (resultSet!=null){
                while (resultSet.next()) {
                    if (resultSet.getString("name").equals(comparingNickName)) {

                        Logger.getGlobal().info("[DATABASE] Failed nickname change: nickname " + comparingNickName + " already busy");
                        return false;

                    }
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }


        return true;

    }


    /**
     * Производит вставку нового игрока в таблицу users
     * Принимает на вход контейнер с данными.
     * @param playerDTO контейнер с данными игрока
     */

    public void insertNewPlayer(PlayerDTO playerDTO)  {

        updatePlayerRow(playerDTO.getPlayerName(),
                new String[]{"\"tokenID\"","place_hash"},
                new String[]{playerDTO.getTokenID(),playerDTO.getPlayerPlaceHashID()}
        );


    }
////
    /**
     * Обновляет данные об игроке, используя в качестве ключа поиска - имя игрока
     * Принимает два массива - название полей и значений
     * @param playerName имя игрока
     * @param fields массив полей
     * @param data массив значений
     */
    public void updatePlayerRow(String playerName, String[] fields, String[] data){
        String fieldAndValue = "";

        for(int i = 0; i < fields.length;i++){
            fieldAndValue = fieldAndValue.concat(fields[i]).concat(" = ").concat("?");

            if(i+1 < fields.length){
                fieldAndValue = fieldAndValue.concat(",");
            }
        }

        String updateQuery = UPDATE_USER.concat(fieldAndValue).concat(UPDATE_USER_WHERE);

        String[] finalData = new String[data.length+1];
        System.arraycopy(data, 0, finalData, 0, data.length);
        finalData[finalData.length-1] = playerName;

        makePreparedStatementByData(updateQuery, finalData);
    }
//////////////////
    public boolean insertAuthDataNewPlayer(String userName, String hashUserPassword){

        PreparedStatement preparedStatement=makePreparedStatementByData(INSERT_INTO_USERS, userName, hashUserPassword);
        if (preparedStatement!=null){
            return true;
        }
        else {return false;}


    }

    public  void deleteUser(String parameter) throws SQLException {//пример аргументов: (login, nickname)

       String query="DELETE FROM users WHERE name = ?";
       PreparedStatement preparedStatement=myPostgresqlConnection.prepareStatement(query);
       preparedStatement.setString(1,parameter);
         int i= preparedStatement.executeUpdate();

        if (i!=1){System.out.println("[DATABASE] ERROR delete user "+ parameter);}
        else {

            System.out.println("[DATABASE]user with nickname "+parameter+" was deleted from users");
        }




    }

    public  String authentication(String userName) throws SQLException {


       ResultSet resultSet = getResultSetFromPrepStatement( makePreparedStatementByData(SELECT_FROM_USERS, userName));

        if(!resultSet.next()){return null;}

        return  resultSet.getString("password");

    }

    public String getOldTokenByUserName(String userName) throws SQLException {

        ResultSet resultSet=getResultSetFromPrepStatement(makePreparedStatementByData(SELECT_FROM_USERS,userName));

        if(!resultSet.next()){return null;}

        return  resultSet.getString("tokenID");

    }
////////////////////////
    /**
     * Осуществляет магию, не трогать.
     * @param SQLQuery
     * @param values
     * @return
     */
    private PreparedStatement makePreparedStatementByData(String SQLQuery, String... values){
        PreparedStatement prepState = null;

        System.out.println(SQLQuery);
        try {
            prepState = myPostgresqlConnection.prepareStatement(SQLQuery);
            int indexColumn = 1;

            for(String value : values){
                System.out.println(value);
                prepState.setString(indexColumn++, value);
            }

            if(SQLQuery.startsWith("INSERT") || SQLQuery.startsWith("UPDATE")){
                System.out.println(SQLQuery);
                prepState.execute();
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return prepState;
    }

    /**
     * Возвращает результаты магии, не трогать
     * @param preparedStatement
     * @return
     */
    private ResultSet getResultSetFromPrepStatement(PreparedStatement preparedStatement){
        try {
            return preparedStatement.executeQuery();
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public static DBService getInstance() {
        return instance;
    }




}

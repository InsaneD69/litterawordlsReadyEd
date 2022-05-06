package org.litteraworlds.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Collectors;

public class HashGen {
    private HashGen() {}

    private static MessageDigest hashGen;

    private static String encodeToString(byte[] rawData){
        StringBuilder sb = new StringBuilder();
        for (byte rawDatum : rawData) {
            if ((0xff & rawDatum) < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(0xff & rawDatum));
        }
        return sb.toString();
    }

    public static byte[] getHash(byte[] data){
        try {
            hashGen = MessageDigest.getInstance("SHA-256");

            byte[] rawHash = hashGen.digest(data);

            String encodedHash = HashToString.convert(rawHash);

            System.out.println("Generate new hash: "+encodedHash);
            return rawHash;

        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            return data;
        }
    }

    public static byte[] getHash(String data){
        if(data.equals("")) {
            data = new Random().ints().mapToObj(Character::toString).collect(Collectors.joining());
        }
        try {
            hashGen = MessageDigest.getInstance("SHA-256");

            byte[] rawHash = hashGen.digest(data.getBytes(StandardCharsets.UTF_8));

            String encodedHash = HashToString.convert(rawHash);

            System.out.println("Generate new hash: "+encodedHash);
            return rawHash;

        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            return data.getBytes();
        }
    }

    public static String randomPasswordGenerator(byte[] tokenIDBytes) {
        SecureRandom secureRandom = new SecureRandom(tokenIDBytes);
        String password = secureRandom.ints(5,'a', 'z')
                .mapToObj(Character::toString)
                .collect(Collectors.joining());
        return password;
    }

    static public byte[] hashPassword(byte[] tokenIDBytes, byte[] passwordBytes) throws NoSuchAlgorithmException {

        MessageDigest hasher = MessageDigest.getInstance("SHA-256");
        byte[] tokenIDAndPassword = new byte[tokenIDBytes.length+passwordBytes.length];

        System.arraycopy(tokenIDBytes,0, tokenIDAndPassword,0, tokenIDBytes.length);
        System.arraycopy(passwordBytes,0, tokenIDAndPassword, tokenIDBytes.length, passwordBytes.length);

        byte[] passwordHash = hasher.digest(tokenIDAndPassword);
        return passwordHash;
    }

    public static String encode(byte[] hash){

        return encodeToString(hash);
    }
}

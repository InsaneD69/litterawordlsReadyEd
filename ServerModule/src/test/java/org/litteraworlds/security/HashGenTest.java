package org.litteraworlds.security;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class HashGenTest {

    @Test
    void testing() throws InvalidKeySpecException, NoSuchAlgorithmException {



       SecretKey token1= HashGen.generateKey("West");
       System.out.println("token1: "+token1);
        System.out.println("token1 string: "+token1.toString());
       byte[] tk1s=token1.getEncoded();
        System.out.println("token1 encoded: "+tk1s);
        String tk1sS = new String(tk1s);
        System.out.println("token1 encoded string: "+tk1sS);
        System.out.println("token1 encoded string byte: "+HashToString.convert(tk1sS.getBytes(StandardCharsets.UTF_8)));


        SecretKey token2= new SecretKeySpec(tk1s,"AES");
        System.out.println("token2: "+token2);
        System.out.println("token2 string: "+token2.toString());
        byte[] tk2s=token1.getEncoded();
        System.out.println("token2 encoded: "+tk2s);
        String tk2sS = new String(tk2s);
        System.out.println("token2 encoded string: "+tk2sS);
        System.out.println("token2 encoded string byte: "+HashToString.convert(tk2sS.getBytes(StandardCharsets.UTF_8)));
    }
}

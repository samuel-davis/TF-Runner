package com.davis.ml.image;

import org.junit.Test;

import javax.net.ssl.TrustManagerFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Set;

/**
 * This software was created for
 * rights to this software belong to
 * appropriate licenses and restrictions apply.
 *
 * @author Samuel Davis created on 8/9/17.
 */
public class SSLTest {
    @Test
    public void test() throws NoSuchAlgorithmException {
        StringBuffer sb = new StringBuffer();
        Provider[] p = Security.getProviders();
        for (int i = 0; i < p.length; i++) {
            sb.append("\nProvider : " + p[i].toString() + "\n");
            Set s = p[i].keySet();
            Object[] o = s.toArray();
            Arrays.sort(o);
            for (int j = 1; j < o.length; j++) {
                sb.append(o[j].toString() + ", ");
            }
        }
        System.out.println(sb.toString());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());


        System.out.println(tmf.getAlgorithm());




    }
}

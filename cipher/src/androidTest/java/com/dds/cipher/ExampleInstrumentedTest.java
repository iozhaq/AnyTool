package com.dds.cipher;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.dds.cipher.aes.AESCrypt;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private final static String TAG = "dds_test";

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.dds.cipher.test", appContext.getPackageName());
    }


    @Test
    public void testAES() throws Exception {
        String content = "欢迎来到chacuo.net";
        String key = "123456";


        //========================NoPadding test ==================================
        // 1. NoPadding 加密的内容必须是16的倍数

        String encrypt = AESCrypt.encrypt(key, content,
                true, "SHA-256",
                "AES/ECB/NoPadding", null);


        Log.d(TAG, "加密出内容：" + encrypt);

        String result = AESCrypt.decrypt(key, encrypt,
                true, "SHA-256",
                "AES/ECB/NoPadding", null);

        Log.d(TAG, "解密出内容：" + result);

        assertEquals(result.trim(), content);

        //========================PKCS5Padding test ==================================

        String encrypt1 = AESCrypt.encrypt(key, content,
                true, "SHA-256",
                "AES/ECB/PKCS5Padding", null);


        Log.d(TAG, "加密出内容：" + encrypt1);

        String result1 = AESCrypt.decrypt(key, encrypt1,
                true, "SHA-256",
                "AES/ECB/PKCS5Padding", null);

        Log.d(TAG, "解密出内容：" + result1);

        assertEquals(result1.trim(), content);
        //========================PKCS7Padding test ==================================

        // 1. pkcs7padding和pkcs5padding的区别在于pkcs7padding的块大小不固定

        String encrypt2 = AESCrypt.encrypt(key, content,
                true, "SHA-256",
                "AES/ECB/PKCS7Padding", null);


        Log.d(TAG, "加密出内容：" + encrypt2);

        String result2 = AESCrypt.decrypt(key, encrypt2,
                true, "SHA-256",
                "AES/ECB/PKCS7Padding", null);

        Log.d(TAG, "解密出内容：" + result2);

        assertEquals(result2.trim(), content);

    }

    @Test
    public void testAES2() throws Exception {
        String content = "123456";
        String key = "GAOQXQQ99QPKOMTZE9YF96OLTD8EU6T9";


        String encrypt = AESCrypt.encrypt(key, content,
                false, null,
                "AES/ECB/PKCS5Padding", null);
        Log.d(TAG, "加密出内容：" + encrypt);

        String decrypt = AESCrypt.decrypt(key, content,
                false, null,
                "AES/ECB/PKCS5Padding", null);
        Log.d(TAG, "解密出内容：" + decrypt);

        assertEquals("123456", decrypt.trim());


    }
}

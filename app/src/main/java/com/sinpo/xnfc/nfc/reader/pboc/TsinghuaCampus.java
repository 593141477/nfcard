package com.sinpo.xnfc.nfc.reader.pboc;

import android.annotation.SuppressLint;
import android.util.Log;

import com.sinpo.xnfc.SPEC;
import com.sinpo.xnfc.nfc.Util;
import com.sinpo.xnfc.nfc.bean.Application;
import com.sinpo.xnfc.nfc.bean.Card;
import com.sinpo.xnfc.nfc.tech.Iso7816;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Zhang Yuxiang on 2016/10/16.
 */
public class TsinghuaCampus extends StandardPboc {

    private final static int SFI_CARD_INFO = 21;
    private final static int SFI_PERSON = 22;

    @Override
    protected Object getApplicationId() {
        return SPEC.APP.TSINGHUA;
    }

    @Override
    protected byte[] getMainApplicationId() {
        return new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03,
                (byte) 0x86, (byte) 0x98, (byte) 0x07, (byte) 0x01, };
    }

    @Override
    protected HINT readCard(Iso7816.StdTag tag, Card card) throws IOException {

        Iso7816.Response CARD_INFO;

        if (!(CARD_INFO = tag.readBinary(SFI_CARD_INFO)).isOkey())
            return HINT.GONEXT;

		/*--------------------------------------------------------------*/
        // select Main Application
		/*--------------------------------------------------------------*/
        if (!selectMainApplication(tag))
            return HINT.GONEXT;

        Iso7816.Response INFO, BALANCE;

        INFO = tag.readBinary(SFI_PERSON);
        if(!INFO.isOkey())
            return HINT.RESETANDGONEXT;

		/*--------------------------------------------------------------*/
        // read balance
		/*--------------------------------------------------------------*/
        BALANCE = tag.getBalance(0, false);

		/*--------------------------------------------------------------*/
        // read log file, record (24)
		/*--------------------------------------------------------------*/
        ArrayList<byte[]> LOG = readLog24(tag, SFI_LOG);

		/*--------------------------------------------------------------*/
        // build result
		/*--------------------------------------------------------------*/
        final Application app = createApplication();

        parseBalance(app, BALANCE);

        parseCardInfo(app, CARD_INFO);

        parseInfo22(app, INFO);

        parseLog24(app, LOG);

        configApplication(app);

        card.addApplication(app);

        return HINT.STOP;
    }

    protected void parseCardInfo(Application app, Iso7816.Response data) {
        if (!data.isOkey() || data.size() < 18) {
            return;
        }

        final byte[] d = data.getBytes();
        app.setProperty(SPEC.PROP.SERIAL, String.format("%010d", Util.toInt(d, 6, 4)));

        app.setProperty(SPEC.PROP.DATE, String.format("20%02X.%02X.%02X", d[15], d[16], d[17]));
    }

    protected void parseInfo22(Application app, Iso7816.Response data) {
        if (!data.isOkey() || data.size() < 40) {
            return;
        }

        final byte[] d = data.getBytes();
        app.setProperty(SPEC.PROP.NUMBER, new String(d, 28, 10));
        try {
            app.setProperty(SPEC.PROP.NAME, new String(Arrays.copyOfRange(d,0,20), "gbk"));
        } catch (UnsupportedEncodingException e) {
            Log.e("parseInfo22", ""+e.getMessage());
        }
    }
}

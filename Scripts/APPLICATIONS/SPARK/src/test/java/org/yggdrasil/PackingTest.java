package org.yggdrasil;

import org.junit.jupiter.api.Test;
import org.yggdrasil.messages.YggdrasilApplicationMessage;
import org.yggdrasil.messages.YggdrasilCTRLMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PackingTest {
    protected YggdrasilCTRLMessage        origin_ctrlMessage;
    protected YggdrasilApplicationMessage origin_applicationMessage;

    public PackingTest() {
        origin_ctrlMessage        = new YggdrasilCTRLMessage("ctrl1", "0", "1", "3");
        origin_applicationMessage = new YggdrasilApplicationMessage("apo1", "5", "6", "7", "8");
    }

    @Test
    public void test_packing() throws Exception {
        byte[] pocm = origin_ctrlMessage.pack();
        byte[] poam = origin_applicationMessage.pack();
        YggdrasilCTRLMessage result_ctrlMessage = new
            YggdrasilCTRLMessage(pocm);
        YggdrasilApplicationMessage result_applicationMessage = new
            YggdrasilApplicationMessage(poam);
        assertEquals(result_ctrlMessage.toString(),
                     origin_ctrlMessage.toString());
        assertEquals(result_applicationMessage.toString(),
                     origin_applicationMessage.toString());
    }
}

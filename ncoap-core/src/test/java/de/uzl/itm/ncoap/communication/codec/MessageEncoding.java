/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.communication.codec;
//
//import com.google.common.collect.Lists;
//import AbstractCoapTest;
//import CoapTestEncoder;
//import de.uniluebeck.itm.ncoap.message.*;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//
//import java.net.InetAddress;
//import java.net.URI;
//import java.util.Collection;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 09.11.13
// * Time: 19:55
// * To change this template use File | Settings | File Templates.
// */
//@RunWith(Parameterized.class)
//public class MessageEncoding extends AbstractCoapTest{
//
//    @Parameterized.Parameters(name = "{index}: Test: {0}")
//    public static Collection<Object[]> data() throws Exception {
//        return Lists.newArrayList(
//                new Object[]{MessageType.Name.CON,
//                             MessageCode.Name.GET,
//                             new URI("coap", null, "www.example.org", -1, "/path/to/service", null, null),
//                             null,
//                             12345,
//                             0xFFFFFFFFFFFFL},
//
//                new Object[]{MessageType.Name.CON,
//                             MessageCode.Name.GET,
//                             new URI("coap", null, "www.example.org", -1, "/path/to/service", null, null),
//                             InetAddress.getByName("2001:1:2:3:4:5:6:7"),
//                             65535,
//                             0L}
//        );
//    }
//
//
//
//    private CoapTestEncoder encoder;
//    private CoapMessage coapMessage;
//
//
//    public MessageEncoding(MessageType.Name messageType, MessageCode.Name messageCode, URI targetUri, InetAddress proxy, int messageID,
//                           long token) throws Exception{
//
//        this.encoder = new CoapTestEncoder();
//        if (MessageCode.isRequest(messageCode.getNumber())) {
//            if (proxy != null)
//                this.coapMessage = new CoapRequest(messageType, messageCode, targetUri, proxy);
//            else
//                this.coapMessage = new CoapRequest(messageType, messageCode, targetUri);
//        }
//
//        else if (MessageCode.Name.isResponse(messageCode)) {
//            this.coapMessage = new CoapResponse(messageCode);
//        }
//
//        else{
//            this.coapMessage = CoapMessage.createCoapMessage(messageType.getNumber(), messageCode.getNumber(),
//                    messageID, token);
//        }
//
//        coapMessage.setMessageID(messageID);
//        coapMessage.setToken(token);
//    }
//
//    @Test
//    public void testEncoding() throws Exception {
//        encoder.encode(this.coapMessage);
//    }
//
//    @Override
//    public void setupLogging() throws Exception {
//        Logger.getRootLogger().setLevel(Level.DEBUG);
//    }
//}

/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
* - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
* disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
* following disclaimer in the documentation and/or other materials provided with the distribution.
* - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
* products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package de.uniluebeck.itm.spitfire.nCoap.core;

import de.uniluebeck.itm.spitfire.nCoap.application.SimpleApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.CoapPipelineFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;


/**
 * @author Oliver Kleine
 */
public class CoapChannel {

    private static Logger log = Logger.getLogger("nCoap");
    static{
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.DEBUG);
        Logger.getLogger(MessageIDFactory.class).addAppender(new ConsoleAppender(new SimpleLayout()));
        Logger.getLogger(MessageIDFactory.class).setLevel(Level.DEBUG);

        //SimpleApplication
        Logger.getLogger(SimpleApplication.class).addAppender(new ConsoleAppender(new SimpleLayout()));
        Logger.getLogger(SimpleApplication.class).setLevel(Level.DEBUG);
    }

    public DatagramChannel channel;

    private static CoapChannel instance = new CoapChannel();

    public static CoapChannel getInstance(){
        return instance;
    }

    private CoapChannel(){
        //Create Datagram Channel
        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(Executors.newCachedThreadPool());

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new CoapPipelineFactory());
        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(5683));
    }

    private static String getHexString(byte[] b){
      String result = "";
        for (byte curByte : b) {
            result +=
                    Integer.toString((curByte & 0xff) + 0x100, 16).substring(1) + "-";
        }
      return (String)result.subSequence(0, Math.max(result.length() - 1, 0));
    }
}
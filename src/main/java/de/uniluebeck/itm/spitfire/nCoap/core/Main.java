/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.spitfire.nCoap.core;

import de.uniluebeck.itm.spitfire.nCoap.communication.CoapPipelineFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.Message;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;


/**
 *
 * @author Oliver Kleine
 */
public class Main {

    private static Logger log = Logger.getLogger("nCoap");
    static{
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.DEBUG);
    }

    public static DatagramChannel channel;

    public static void main(String[] args) throws Exception{

        //Create Datagram Channel
        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(Executors.newCachedThreadPool());

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new CoapPipelineFactory());
        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(5683));

        new MessageSender().start();
    }

    private static void writeMessage() throws Exception{
        //Create Message
        URI uri = new URI("coap://[2001:638:70a:b157:215:7ff:fe20:1002]:5683/.well-known/core");
        Message message = Message.createRequest(MsgType.CON, Code.GET, uri);

        //Send Message
        InetSocketAddress remoteAddress = new InetSocketAddress(message.getTargetUri().getHost(),
                message.getTargetUri().getPort());
        channel.write(message, remoteAddress);
    }

    private static String getHexString(byte[] b){
      String result = "";
      for (int i=0; i < b.length; i++) {
        result +=
              Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 ) + "-";
      }
      return (String)result.subSequence(0, Math.max(result.length() - 1, 0));
    }

    private static class MessageSender extends Thread{
        @Override
        public void run(){
            try {
                for(int i = 0; i < 100; i++){
                    writeMessage();
                    sleep(10);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

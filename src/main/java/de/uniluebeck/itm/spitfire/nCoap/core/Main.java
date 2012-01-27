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

        //Create Message
        URI uri = new URI("coap://[2001:638:70a:b157:215:7ff:fe20:1002]:5683/.well-known/core");
        Message message = Message.createRequest(MsgType.CON, Code.GET, uri);
        Message message2 = Message.createRequest(MsgType.CON, Code.PUT, uri);
        Message message3 = Message.createRequest(MsgType.CON, Code.POST, uri);

        //Send Message
        InetSocketAddress remoteAddress = new InetSocketAddress(message.getTargetUri().getHost(),
                message.getTargetUri().getPort());
        channel.write(message, remoteAddress);
        channel.write(message2, remoteAddress);
        channel.write(message3, remoteAddress);



        //bootstrap.bind(new InetSocketAddress(5683));

//        URI uri = new URI("coap://[2001:638:746::1]:5386/test%7E1/test2?q1=1&q2=2");
//        Message message = Message.createRequest(MsgType.CON, Code.GET, uri);
//
////        URI proxy = new URI("coap://proxy.itm.uni-luebeck.de/path1/path2");
////        message.setProxyURI(proxy);
//
//        URI location = new URI("/path/to/new/resource?value=1");
//        //message.setLocationURI(location);
//
//        log.debug("Target URI: " + message.getTargetUri());
//        log.debug("Proxy URI: " + message.getProxyURI());
//        log.debug("Location URI: " + message.getLocationURI());


//        String uriString = "/test%7E1/test2";
//        String path = "";
//        for(int i = 0; i < 50; i++){
//           path += uriString;
//        }
//        URI uri = new URI("coap://[2a02:2e0:3fe:100::6]:5388" + path + "?q1=1&q2=2");
//        Message msg = new Message(MsgType.CON, Code.GET, uri);
//
//        //System.out.println("Receipient InetSocketAddress: " + msg.rcptSocketAddress);
//
//        System.out.println("Target URI: " + msg.getTargetUri());
//        System.out.println("Option Count: " + msg.getHeader().getOptionCount());
//
//        msg.setTargetURI(new URI("coap://www.six.heise.de:5386/test1/test2?q1=1&q2=2"));
//        System.out.println("Target URI: " + msg.getTargetUri());
//        System.out.println("Option Count: " + msg.getHeader().getOptionCount());
//
//        try{
//            msg.setTargetURI(new URI("/test1/test2?q1=1&q2=2"));
//        }
//        catch(Exception e){
//           log.debug("Exception gefangen!", e);
//        }
//
//        System.out.println("Target URI: " + msg.getTargetUri());
//        System.out.println("Option Count: " + msg.getHeader().getOptionCount());

        /*
        uri.resolve(uri);

        System.out.println("ASCII: " + msg.getTargetUri().toASCIIString());

        URI uri2 = new URI(msg.getTargetUri().toASCIIString());

        URI uri3 = new URI("coap", "[2a02:2e0:3fe:100::6]", URLDecoder.decode(uri2.getPath(), "UTF-8"), uri2.getQuery());

        System.out.println("Target URI 3: " + uri2.toString());

        /*
        boolean[] test = new boolean[1];
        System.out.println("Test: " + test[0]);

        URI uri = new URI("coap://www.six.heise.de:5386/test1/test2?q1=1&q2=2");

        String host = uri.getHost();
        if(host.startsWith("[") && host.endsWith("]")){
            host = host.substring(1, host.length() - 1);
        }
        if(IPAddressUtil.isIPv6LiteralAddress(host)){
            System.out.println("Jawoll!!");
        }
        else{
            System.out.println("No!");
        }
        System.out.println("Host: " +  host);
        InetAddress host_addr = InetAddress.getByName(host);


        String authority = uri.getAuthority();
        System.out.println("Authority: " + authority);

        InetAddress addr = InetAddress.getByName("www.six.heise.de");
        System.out.println("Heise: " + addr.getHostAddress());

        System.out.println("Gleich?: " +  host_addr.equals(addr));

        int port = uri.getPort();
        System.out.println("Port: "+ port);

        String path = uri.getRawPath();
        System.out.println("Path: " + path);

        String query = uri.getRawQuery();
        System.out.println("Query: " + query);
        */
    }

    private static String getHexString(byte[] b){
      String result = "";
      for (int i=0; i < b.length; i++) {
        result +=
              Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 ) + "-";
      }
      return (String)result.subSequence(0, Math.max(result.length() - 1, 0));
    }
}

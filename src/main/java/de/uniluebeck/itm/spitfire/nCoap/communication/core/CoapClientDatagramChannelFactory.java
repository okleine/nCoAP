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

package de.uniluebeck.itm.spitfire.nCoap.communication.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * @author Oliver Kleine
 */
public class CoapClientDatagramChannelFactory {

    //public final int COAP_CLIENT_PORT = 5682;
    public static final int RECEIVE_BUFFER_SIZE = 65536;
    private static final int NO_OF_THREADS = 20;
    
    private DatagramChannel channel;

    private static CoapClientDatagramChannelFactory instance = new CoapClientDatagramChannelFactory();

    private CoapClientDatagramChannelFactory(){
        //Create Datagram Channel
//        ChannelFactory channelFactory =
//                new NioDatagramChannelFactory(CoapExecutorService.getExecutorService());

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(NO_OF_THREADS,
                new ThreadFactoryBuilder().setNameFormat("nCoap-Client-Thread %d")
                        .build());

        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(executorService);

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new CoapClientPipelineFactory(executorService));

        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(0));

        FixedReceiveBufferSizePredictor predictor = new FixedReceiveBufferSizePredictor(RECEIVE_BUFFER_SIZE);
        channel.getConfig().setReceiveBufferSizePredictor(predictor);
    }
    
//    public static void resetInstance() {
//        instance = new CoapClientDatagramChannelFactory();
//        initInstance();
//    }

    public static CoapClientDatagramChannelFactory getInstance(){
        return instance;
    }

    public DatagramChannel getChannel(){
        return channel;
    }
}

package de.uniluebeck.itm.spitfire.nCoap.communication.reliability;

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalErrorMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.09.12
 * Time: 12:33
 * To change this template use File | Settings | File Templates.
 */
//Private class to handle the retransmission of confirmable message using a thread scheduler


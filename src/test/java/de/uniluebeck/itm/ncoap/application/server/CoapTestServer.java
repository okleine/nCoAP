/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * Simple extension to {@link CoapServerApplication} that apart from the inherited functionality does nothing
 * but store the reception times of {@link CoapRequest}s.
 *
 * @author Oliver Kleine
 */
public class CoapTestServer extends CoapServerApplication {

    private Map<Integer, Long> requestReceptionTimes = Collections.synchronizedMap(new TreeMap<Integer, Long>());

    /**
     * @param serverPort the port the server is supposed to listen at
     */
    public CoapTestServer(int serverPort){
        super(serverPort);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent me){
        if(me.getMessage() instanceof CoapRequest){
           requestReceptionTimes.put(((CoapRequest) me.getMessage()).getMessageID(), System.currentTimeMillis());
        }

        super.messageReceived(ctx, me);

    }

    /**
     * Returns a {@link Map} containing all received {@link CoapRequest}s, i.e. their message IDs as keys and the
     * reception time as values.
     *
     * @return  a {@link Map} containing all received {@link CoapRequest}s, i.e. their message IDs as keys and the
     * reception time as values.
     */
    public Map<Integer, Long> getRequestReceptionTimes(){
        return this.requestReceptionTimes;
    }
}

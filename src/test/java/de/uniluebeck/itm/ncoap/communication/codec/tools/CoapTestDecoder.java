package de.uniluebeck.itm.ncoap.communication.codec.tools;

import de.uniluebeck.itm.ncoap.communication.codec.CoapMessageDecoder;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 12.11.13
 * Time: 23:54
 * To change this template use File | Settings | File Templates.
 */
public class CoapTestDecoder extends CoapMessageDecoder{

    public Object decode(ChannelBuffer buffer) throws Exception {
        return super.decode(null, null, buffer);
    }

}

//package de.uniluebeck.itm.spitfire.nCoap.communication.internal;
//
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
//import org.jboss.netty.channel.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.nio.charset.Charset;
//
///**
// * This is the topmost handler in the pipeline to deal with exceptions thrown during the internal message processing.
// * If there was an {@link InvalidOptionException} caused by a critical option during the decoding process of an incoming
// * message M it is handled as follows:
// * <ul>
// *     <li>M was anything but a {@link MsgType#CON} message: Ignore siltently.</li>
// *     <li>M was a {@link MsgType#CON} request: Send {@link MsgType#NON} response with {@link Code#BAD_OPTION_402}</li>
// *     <li>M was a {@link MsgType#CON} response: Send {@link MsgType#RST}</li>
// * </ul>
// * The channel to process the incoming message M is closed afterwards.
// *
// * {@link InvalidHeaderException} and all others are ignored silently. *
// */
//public class ExceptionHandler extends SimpleChannelHandler{
//
//    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
//        Throwable cause = e.getCause();
//
//        //handle InvalidHeaderExceptions
//        if(cause instanceof InvalidHeaderException){
//            log.error("Message that caused the exception has invalid header.", e);
//            ctx.getChannel().close();
//        }
//        //handle InvalidOptionException
//        else if(cause instanceof InvalidOptionException){
//            InvalidOptionException ex = (InvalidOptionException) e;
//            Header header = ex.getMessageHeader();
//
//            if(header == null){
//                log.error("This should never happen. There is no header information contained in the exception. " +
//                          "Ignore silently.", ex);
//                return;
//            }
//
//            if(MsgType.CON != header.getMsgType()){
//                log.info("Message that caused the exception is not confirmable ({}). Ignore silently.", header, ex);
//                return;
//            }
//
//            //Create response
//            CoapResponse response;
//            if(header.getCode().isRequest()){
//                response = createErrorResponse(MsgType.CON, Code.BAD_OPTION_402, header.getMsgID(), ex.getMessage());
//                log.info("Message that caused the exception is a confirmable request ({}). Send 402 response.",
//                         header, ex);
//            }
//            else{
//                response = createErrorResponse(MsgType.RST, Code.EMPTY, header.getMsgID());
//                log.info("Message that caused the exception is a confirmable response ({}). Send RST message.",
//                         header, ex);
//            }
//
//            //Send response and close channel afterwards
//            ctx.getChannel().write(response);
//        }
//        //handle other exceptions
//        else{
//            log.error("There was an unexpected exception!", e);
//        }
//    }
//
//    private CoapResponse createErrorResponse(MsgType msgType, Code code, int messageID){
//        CoapResponse response = new CoapResponse(MsgType.NON, code);
//        try {
//            response.setMessageID(messageID);
//        } catch (InvalidHeaderException e) {
//            log.error("This should never happen.", e);
//        }
//        return response;
//    }
//
//    private CoapResponse createErrorResponse(MsgType msgType, Code code, int messageID, String payload){
//        CoapResponse response = createErrorResponse(msgType, code, messageID);
//        try {
//            response.setPayload(payload.getBytes(Charset.forName("UTF-8")));
//            response.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
//        } catch (MessageDoesNotAllowPayloadException e) {
//            log.error("This should never happen.", e);
//        } catch (InvalidOptionException e) {
//            log.error("This should never happen.", e);
//        } catch (ToManyOptionsException e) {
//            log.error("This should never happen.", e);
//        }
//        return response;
//    }
//}

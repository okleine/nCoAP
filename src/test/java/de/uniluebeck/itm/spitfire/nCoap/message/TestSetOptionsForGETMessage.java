//package de.uniluebeck.itm.spitfire.coap7.message;
//
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
//import junit.framework.TestCase;
//
//import java.net.InetAddress;
//import java.net.URI;
//
///**
// * Created by IntelliJ IDEA.
// * User: olli
// * Date: 10.01.12
// * Time: 08:38
// * To change this template use File | Settings | File Templates.
// */
//public class TestSetOptionsForGETMessage extends TestCase {
//
//    private Message message;
//    private URI targetURI;
//    private URI proxyURI;
//    private URI locationURI;
//    private OptionRegistry.MediaType contentType;
//    private int maxAge;
//    private byte[][] etags;
//    private byte[] token;
//    private OptionRegistry.MediaType[] accept;
//    private byte[][] ifMatch;
//    private boolean ifNonMatch;
//    private boolean valid;
//
//    public TestSetOptionsForGETMessage(Message message, URI proxyURI, URI locationURI,
//                                       OptionRegistry.MediaType contentType, int maxAge, byte[][] etags, byte[] token,
//                                       OptionRegistry.MediaType[] accept, byte[][] ifMatch, boolean ifNonMatch,
//                                       boolean valid){
//
//        this.message = message;
//
//        if(!(proxyURI == null)){
//            this.proxyURI = proxyURI;
//        }
//        if(!(locationURI == null)){
//            this.locationURI = locationURI;
//        }
//        if(!(contentType == null)){
//            this.contentType = contentType;
//        }
//        if(maxAge != 0){
//            this.maxAge = maxAge;
//        }
//        if(!(etags == null)){
//            this.etags = etags;
//        }
//        if(!(token == null)){
//            this.token = token;
//        }
//        if(!(accept == null)){
//            this.accept = accept;
//        }
//        if(!(ifMatch == null)){
//            this.ifMatch = ifMatch;
//        }
//        this.ifNonMatch = ifNonMatch;
//        this.valid = valid;
//    }
//
//    public void testSetProxyURI() throws Exception{
//        message.setProxyURI(proxyURI);
//
//        assertEquals(proxyURI, message.getProxyURI());
//    }
//
//    private void assertEquals(URI expected, URI actual) throws Exception{
//
//        //Compare schemes
//        assertEquals(expected.getScheme(), actual.getScheme());
//
//        //Compare host
//        InetAddress actualHost = InetAddress.getByName(actual.getHost());
//        InetAddress expectedHost = InetAddress.getByName(expected.getHost());
//
//        assertEquals(expectedHost, actualHost);
//
//        //Compare port
//        int targetUriPort = expected.getPort();
//        if(targetUriPort == -1){
//            targetUriPort = OptionRegistry.COAP_PORT_DEFAULT;
//        }
//        assertEquals(targetUriPort, actual.getPort());
//
//        //Compare paths
//        assertEquals(expected.getRawPath(), actual.getRawPath());
//
//        //Compare Query
//        assertEquals(expected.getRawQuery(), actual.getRawQuery());
//    }
//
//
//    private URI locationURI;
//    private OptionRegistry.MediaType contentType;
//    private int maxAge;
//    private byte[][] etags;
//    private byte[] token;
//    private OptionRegistry.MediaType[] accept;
//    private byte[][] ifMatch;
//    private boolean ifNonMatch;
//
//    public void TestValidOptionsForGetMessage() throws Exception{
//        Message message = new Message(MsgType.CON, Code.GET, targetURI);
//
//        message.setToken(token);
//        message.setProxyURI(proxyURI);
//        message.setAccept(accept);
//        message.setETAG(etags);
//    }
//
//    public void TestInvalidOptionsForGetMessage() throws Exception{
//        Message message = new Message(MsgType.CON, Code.GET, targetURI);
//
////        private URI locationURI;
////        private OptionRegistry.MediaType contentType;
////        private int maxAge;
////        private byte[][] ifMatch;
////        private boolean ifNonMatch;
//
//        message.setLocationURI(locationURI);
//
//
//
//
//    }
//
//    public void testSetOption() throws Exception{
//        if(proxyURI != null){
//            message.setProxyURI(proxyURI);
//        }
//
//        if(locationURI != null){
//            message.setLocationURI(locationURI);
//        }
//
//        if(contentType != null){
//            message.setContentType(contentType);
//        }
//
//        if(maxAge != 0){
//            message.setMaxAge(maxAge);
//        }
//
//        if(etags != null){
//            message.setETAG(etags);
//        }
//
//        if(token != null){
//            message.setToken(token);
//        }
//
//        if(accept != null){
//           message.setAccept(accept);
//        }
//
//        if(ifMatch != null){
//            message.setIfMatch(ifMatch);
//        }
//
//        if(ifNonMatch){
//            message.setIfNoneMatch();
//        }
//
//    }
//
//
//
//
//}

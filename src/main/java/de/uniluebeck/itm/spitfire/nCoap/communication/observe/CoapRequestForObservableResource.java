//package de.uniluebeck.itm.spitfire.nCoap.communication.observe;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 31.01.13
// * Time: 16:43
// * To change this template use File | Settings | File Templates.
// */
//
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
//
//import java.net.InetSocketAddress;
//import java.net.SocketAddress;
//
///**
// * Represents a observer.
// * Holds the original request, remote address and a notification counter.
// */
//class CoapRequestForObservableResource extends CoapRequest{
//    private InetSocketAddress remoteAddress;
//    private int responseCount = 1;
//
//    /**
//     * Creates a new instance.
//     * @param request observable request
//     * @param remoteAddress observer remote address
//     */
//    public CoapRequestForObservableResource(CoapRequest request, InetSocketAddress remoteAddress) throws InvalidHeaderException {
//        super(request);
//        this.remoteAddress = remoteAddress;
//    }
//
//    public int updateResponseCount() {
//        return responseCount++;
//    }
//
//    public int getResponseCount() {
//        return responseCount;
//    }
//
//    public InetSocketAddress getRemoteAddress() {
//        return remoteAddress;
//    }
//
//    public void setResponseCount(long observeOptionValue) {
//        responseCount = (int)observeOptionValue;
//    }
//}

///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.communication.observe;
//
//import java.net.InetSocketAddress;
//import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
//import de.uniluebeck.itm.ncoap.message.MessageType;
//
///**
// * This internal message is created and send upstream when an observer of an {@link ObservableWebService} instance
// * rejects the reception of an update notification with a {@link MessageType#RST}.
// *
// * @author Oliver Kleine
// */
//public class InternalUpdateNotificationRejectedMessage {
//
//    private InetSocketAddress observerAddress;
//    private String servicePath;
//
//    /**
//     * @param observerAddress the address of the rejecting observer
//     * @param servicePath the path of the {@link ObservableWebService} instance whose update notification was rejected
//     */
//    public InternalUpdateNotificationRejectedMessage(InetSocketAddress observerAddress, String servicePath){
//        this.observerAddress = observerAddress;
//        this.servicePath = servicePath;
//    }
//
//    /**
//     * Returns the address of the observer that rejected the update notification
//     * @return the address of the observer that rejected the update notification
//     */
//    public InetSocketAddress getObserverAddress() {
//        return observerAddress;
//    }
//
//    /**
//     * Returns the path of the service whose update notification was rejected
//     * @return the path of the service whose update notification was rejected
//     */
//    public String getServicePath() {
//        return servicePath;
//    }
//
//    @Override
//    public String toString(){
//        return "[InternalUpdateNotificationRejectedMessage] Remote address " + observerAddress +
//                ", observable web service " + servicePath + ".";
//    }
//
//}

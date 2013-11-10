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
//package de.uniluebeck.itm.ncoap.communication.observe;
//
//import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.ContentFormat;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Instances of {@link ObservationParameter} contain meta-information about a running observation of
// * a local resource by a (remote) observer.
// *
// * @author Oliver Kleine
// */
//class ObservationParameter {
//
//    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//    private byte[] token;
//    private ContentFormat acceptedMediaType;
//    private int notificationCount = 0;
//
//    /**
//     * @param token The token to be included in every update notification for the observer
//     */
//    public ObservationParameter(byte[] token){
//        this.token = token;
//    }
//
//    /**
//     * Returns the {@link ContentFormat} for the observation. The payload of all update notifications for the
//     * observer must have this {@link ContentFormat}.
//     *
//     * @return the {@link ContentFormat} for the observation
//     */
//    public ContentFormat getAcceptedMediaType() {
//        if(acceptedMediaType != null){
//            return acceptedMediaType;
//        }
//        return null;
//    }
//
//    /**
//     * Set the {@link ContentFormat} for the observation. The payload of all update notifications for the
//     * observer must have this {@link ContentFormat}.
//     *
//     * @param acceptedMediaType the {@link ContentFormat} for the observation
//     */
//    public void setAcceptedMediaType(ContentFormat acceptedMediaType){
//        this.acceptedMediaType = acceptedMediaType;
//    }
//
//    /**
//     * Returns the number of update notifications already sent to the observer.
//     * @return the number of update notifications already sent to the observer.
//     */
//    public int getNotificationCount() {
//        return notificationCount;
//    }
//
//    /**
//     * Increases the notification count for this observation by 1.
//     */
//    public void increaseNotificationCount() {
//        this.notificationCount++;
//        log.debug("Notificaton count set to {}.", notificationCount);
//    }
//
//    /**
//     * Returns the token to be included in every update notification for the observer
//     * @return the token to be included in every update notification for the observer
//     */
//    public byte[] getToken() {
//        return token;
//    }
//}

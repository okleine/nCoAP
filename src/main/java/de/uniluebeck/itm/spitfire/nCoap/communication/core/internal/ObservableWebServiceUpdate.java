//package de.uniluebeck.itm.spitfire.nCoap.communication.core.internal;
//
//import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * This message will be passed down the pipeline if a registered service updates.
// *
// * @author Stefan Hueske, Oliver Kleine
// */
//public class ObservableWebServiceUpdate{
//
//    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//    private ObservableWebService service;
//
//    public ObservableWebServiceUpdate(ObservableWebService service) {
//        this.service = service;
//        log.info("Internal observable service update created for service " + service.getPath());
//    }
//
//    /**
//     * Returns the content of the internal ObservableWebServiceUpdate message which is the updated WebService instance
//     * itself.
//     * @return the updated {@link ObservableWebService} instance (may be cast from Object)
//     */
//    public ObservableWebService getWebService() {
//        return service;
//    }
//
//    @Override
//    public String toString(){
//        return "Observable status update: New status of " + service.getPath() + ": " + service.getResourceStatus();
//    }
//}

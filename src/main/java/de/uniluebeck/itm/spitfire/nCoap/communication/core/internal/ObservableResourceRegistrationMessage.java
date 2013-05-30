package de.uniluebeck.itm.spitfire.nCoap.communication.core.internal;

import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 28.05.13
 * Time: 15:32
 * To change this template use File | Settings | File Templates.
 */
public class ObservableResourceRegistrationMessage {

    private ObservableWebService webService;

    public ObservableResourceRegistrationMessage(ObservableWebService webService){

        this.webService = webService;
    }

    public ObservableWebService getWebService() {
        return webService;
    }

    @Override
    public String toString(){
        return "[ObservableResourceRegistration] Path: " + webService.getPath();
    }
}

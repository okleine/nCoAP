package de.uniluebeck.itm.spitfire.nCoap.communication.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.*;

/**
* Created with IntelliJ IDEA.
* User: olli
* Date: 15.11.12
* Time: 16:32
* To change this template use File | Settings | File Templates.
*/
public abstract class CoapExecutorService {

    private static final int NO_OF_THREADS = 100;

    private static ScheduledExecutorService executorService =
            Executors.newScheduledThreadPool(NO_OF_THREADS,
                                             new ThreadFactoryBuilder().setNameFormat("nCoap-Thread %d")
                                                                       .build()
    );

    public static ScheduledFuture schedule(Runnable runnable, int delay, TimeUnit timeUnit){
        return executorService.schedule(runnable, delay, timeUnit);
    }

    public static ExecutorService getExecutorService(){
        return executorService;
    }
    
    public static void cancelAll() {
        executorService.shutdownNow();
        executorService = Executors.newScheduledThreadPool(NO_OF_THREADS,
                                             new ThreadFactoryBuilder().setNameFormat("nCoap-Thread %d")
                                                                       .build());
    }
}

package org.littleneko;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        ScheduledFuture future = executor.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("ssss");
            }
        }, 5, TimeUnit.SECONDS);

        future.cancel(true);
        executor.shutdown();
    }
}

package com.example.main;

import com.amazonaws.services.sqs.model.Message;
import com.example.InMemoryQueueService;
import com.example.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
public class QueueApp implements CommandLineRunner {

    @Autowired
    private ServerProperties serverProperties;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(serverProperties);

        QueueService queue;

        {//InMemoryQueue
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);

            queue = new InMemoryQueueService(scheduler, serverProperties.getQueueSize());

            queue.push("Test Message 1");
            queue.push("Test Message 2");
            queue.push("Test Message 3");
            Message m = queue.pull().get();
            queue.pull();
            queue.pull();

            //Delete Test Message 1
            queue.delete(m.getReceiptHandle());
        }

        {//FileQueue
//            queue = new FileQueueService(scheduler, "/tmp/messages", serverProperties.getQueueSize());
//            queue.push("Message1");
//            queue.push("Message2");
//            queue.push("Message3");
//
//            queue.pull();
//            queue.pull();
//
//            queue.delete("");
        }


    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(QueueApp.class, args);
    }

}

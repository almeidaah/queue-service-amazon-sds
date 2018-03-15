package com.example.main;

import com.amazonaws.services.sqs.model.Message;
import com.example.FileQueueService;
import com.example.InMemoryQueueService;
import com.example.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    final Logger logger = LoggerFactory.getLogger(QueueApp.class);

    @Override
    public void run(String... args) throws Exception {

        logger.info(serverProperties.toString());

        QueueService queue;
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);

        {//InMemoryQueue
//
            queue = new InMemoryQueueService(scheduler, serverProperties.getQueueSize(), serverProperties.getVisibilityTimeout());

            queue.push("Test Message 1");
            queue.push("Test Message 2");
            queue.push("Test Message 3");
            Message m = queue.pull().get();
            queue.pull();
            queue.pull();

//            //Delete Test Message 1
            queue.delete(m.getReceiptHandle());
        }

        {//FileQueue
            queue = new FileQueueService(scheduler, "/tmp/messages", serverProperties.getQueueSize(), serverProperties.getVisibilityTimeout());
//            queue.push("Message1");
//            queue.push("Message2");
//            queue.push("Message3");

//            Message m = queue.pull().get();
//            queue.pull();
//            queue.pull();

//            queue.delete(m.getReceiptHandle());
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(QueueApp.class, args);
    }

}

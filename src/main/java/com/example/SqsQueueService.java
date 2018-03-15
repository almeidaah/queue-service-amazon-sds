package com.example;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.example.main.QueueApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * I should include queueUrl but don't know where.
 */
public class SqsQueueService implements QueueService {
    //
    // Task 4: Optionally implement parts of me.
    //
    // This file is a placeholder for an AWS-backed implementation of QueueService.  It is included
    // primarily so you can quickly assess your choices for method signatures in QueueService in
    // terms of how well they map to the implementation intended for a production environment.
    //

    final Logger logger = LoggerFactory.getLogger(QueueApp.class);

    private AmazonSQSClient sqsClient;

    public SqsQueueService(AmazonSQSClient sqsClient) {
        this.sqsClient = sqsClient;
        sqsClient.createQueue(new CreateQueueRequest());

    }

    @Override
    public void push(String message) {
        try {
            sqsClient.sendMessage("queueUrl", message);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public Optional<Message> pull() {
        try {
            return sqsClient.receiveMessage("queueUrl").getMessages().stream().findFirst();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return Optional.empty();
    }

    public void delete(String receiptHandle) {
        sqsClient.deleteMessage("queueUrl", receiptHandle);
    }
}

package com.example;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

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

    private AmazonSQSClient sqsClient;

    public SqsQueueService(AmazonSQSClient sqsClient) {
        this.sqsClient = sqsClient;
        createQueue();

    }

    private void createQueue() {
        CreateQueueResult queue = sqsClient.createQueue(new CreateQueueRequest());
    }


    @Override
    public void push(String message) {
        try {
            sqsClient.sendMessage("queueUrl", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Message> pull() {
        try {
            ReceiveMessageResult result = sqsClient.receiveMessage("queueUrl");
            if (result.getMessages().size() > 0) {
                return Optional.of(result.getMessages().get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void delete(String receiptHandle) {
        sqsClient.deleteMessage("queueUrl", receiptHandle);

    }
}

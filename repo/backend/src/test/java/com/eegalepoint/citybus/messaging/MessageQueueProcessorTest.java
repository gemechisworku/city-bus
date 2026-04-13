package com.eegalepoint.citybus.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eegalepoint.citybus.domain.messaging.MessageEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueAttemptEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueAttemptRepository;
import com.eegalepoint.citybus.domain.messaging.MessageQueueEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageQueueProcessorTest {

  @Mock private MessageQueueRepository queueRepository;
  @Mock private MessageQueueAttemptRepository attemptRepository;
  @InjectMocks private MessageQueueProcessor processor;

  @Test
  void processQueue_marksQueuedEntriesSent() {
    MessageEntity message = org.mockito.Mockito.mock(MessageEntity.class);
    MessageQueueEntity entry = new MessageQueueEntity(message);
    when(queueRepository.findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(eq("QUEUED"), any(Instant.class)))
        .thenReturn(List.of(entry));

    processor.processQueue();

    ArgumentCaptor<MessageQueueEntity> cap = ArgumentCaptor.forClass(MessageQueueEntity.class);
    verify(queueRepository).save(cap.capture());
    assertThat(cap.getValue().getStatus()).isEqualTo("SENT");
    assertThat(cap.getValue().getSentAt()).isNotNull();
    verify(attemptRepository).save(any(MessageQueueAttemptEntity.class));
  }
}

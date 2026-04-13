package com.eegalepoint.citybus.messaging;

import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.messaging.MessageEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueRepository;
import com.eegalepoint.citybus.domain.messaging.MessageRedactionRuleEntity;
import com.eegalepoint.citybus.domain.messaging.MessageRedactionRuleRepository;
import com.eegalepoint.citybus.domain.messaging.MessageRepository;
import com.eegalepoint.citybus.messaging.dto.CreateMessageRequest;
import com.eegalepoint.citybus.messaging.dto.MessageResponse;
import com.eegalepoint.citybus.repo.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {

  private final MessageRepository messageRepository;
  private final MessageQueueRepository messageQueueRepository;
  private final MessageRedactionRuleRepository redactionRuleRepository;
  private final UserRepository userRepository;

  public MessageService(
      MessageRepository messageRepository,
      MessageQueueRepository messageQueueRepository,
      MessageRedactionRuleRepository redactionRuleRepository,
      UserRepository userRepository) {
    this.messageRepository = messageRepository;
    this.messageQueueRepository = messageQueueRepository;
    this.redactionRuleRepository = redactionRuleRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> listMessages() {
    UserEntity user = currentUser();
    return messageRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public MessageResponse getMessage(long id) {
    UserEntity user = currentUser();
    MessageEntity msg = messageRepository.findByIdAndUser_Id(id, user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    return toResponse(msg);
  }

  @Transactional
  public MessageResponse markRead(long id) {
    UserEntity user = currentUser();
    MessageEntity msg = messageRepository.findByIdAndUser_Id(id, user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    msg.markRead();
    messageRepository.save(msg);
    return toResponse(msg);
  }

  @Transactional
  public MessageResponse createMessage(CreateMessageRequest req) {
    UserEntity user = currentUser();
    String body = applyRedaction(req.body());
    MessageEntity msg = messageRepository.save(new MessageEntity(user, req.subject(), body));
    messageQueueRepository.save(new MessageQueueEntity(msg));
    return toResponse(msg);
  }

  private String applyRedaction(String text) {
    List<MessageRedactionRuleEntity> rules = redactionRuleRepository.findByEnabledTrue();
    String result = text;
    for (MessageRedactionRuleEntity rule : rules) {
      result = result.replaceAll(rule.getPattern(), rule.getReplacement());
    }
    return result;
  }

  private UserEntity currentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
  }

  private MessageResponse toResponse(MessageEntity msg) {
    return new MessageResponse(
        msg.getId(), msg.getSubject(), msg.getBody(), msg.isRead(), msg.getCreatedAt());
  }
}

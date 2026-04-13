package com.eegalepoint.citybus.messaging;

import com.eegalepoint.citybus.messaging.dto.CreateMessageRequest;
import com.eegalepoint.citybus.messaging.dto.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/messages", produces = MediaType.APPLICATION_JSON_VALUE)
public class MessageController {

  private final MessageService messageService;

  public MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<MessageResponse> list() {
    return messageService.listMessages();
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public MessageResponse get(@PathVariable("id") long id) {
    return messageService.getMessage(id);
  }

  @PostMapping(path = "/{id}/read")
  @PreAuthorize("isAuthenticated()")
  public MessageResponse markRead(@PathVariable("id") long id) {
    return messageService.markRead(id);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  @ResponseStatus(HttpStatus.CREATED)
  public MessageResponse create(@Valid @RequestBody CreateMessageRequest req) {
    return messageService.createMessage(req);
  }
}

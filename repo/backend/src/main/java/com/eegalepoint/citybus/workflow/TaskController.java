package com.eegalepoint.citybus.workflow;

import com.eegalepoint.citybus.workflow.dto.BatchResultResponse;
import com.eegalepoint.citybus.workflow.dto.BatchTaskDecisionRequest;
import com.eegalepoint.citybus.workflow.dto.CreateTaskRequest;
import com.eegalepoint.citybus.workflow.dto.TaskDecisionRequest;
import com.eegalepoint.citybus.workflow.dto.TaskResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
public class TaskController {

  private final WorkflowService workflowService;

  public TaskController(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse create(@Valid @RequestBody CreateTaskRequest req) {
    return workflowService.createTask(req);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public List<TaskResponse> list(
      @RequestParam(value = "status", required = false) String status) {
    return workflowService.listTasks(status);
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public TaskResponse approve(
      @PathVariable("id") long id,
      @RequestBody(required = false) TaskDecisionRequest req) {
    return workflowService.approveTask(id, req != null ? req.note() : null);
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public TaskResponse reject(
      @PathVariable("id") long id,
      @RequestBody(required = false) TaskDecisionRequest req) {
    return workflowService.rejectTask(id, req != null ? req.note() : null);
  }

  @PostMapping("/{id}/return")
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public TaskResponse returnTask(
      @PathVariable("id") long id,
      @RequestBody(required = false) TaskDecisionRequest req) {
    return workflowService.returnTask(id, req != null ? req.note() : null);
  }

  @PostMapping(path = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public BatchResultResponse batch(@Valid @RequestBody BatchTaskDecisionRequest req) {
    return workflowService.batchDecide(req);
  }
}

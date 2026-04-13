package com.eegalepoint.citybus.workflow;

import com.eegalepoint.citybus.workflow.dto.CreateWorkflowRequest;
import com.eegalepoint.citybus.workflow.dto.WorkflowInstanceResponse;
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
@RequestMapping(path = "/api/v1/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
public class WorkflowController {

  private final WorkflowService workflowService;

  public WorkflowController(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  @ResponseStatus(HttpStatus.CREATED)
  public WorkflowInstanceResponse create(@Valid @RequestBody CreateWorkflowRequest req) {
    return workflowService.createWorkflow(req);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public List<WorkflowInstanceResponse> list(
      @RequestParam(value = "status", required = false) String status) {
    return workflowService.listWorkflows(status);
  }

  @GetMapping("/{instanceId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public WorkflowInstanceResponse get(@PathVariable("instanceId") long instanceId) {
    return workflowService.getWorkflow(instanceId);
  }
}
